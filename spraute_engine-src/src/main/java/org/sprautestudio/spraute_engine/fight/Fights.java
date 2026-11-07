package org.sprautestudio.spraute_engine.fight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow.Pickup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

public final class Fights {
    private static final Map<UUID, Fights.Fighter> FIGHTERS = new LinkedHashMap<>();
    private static final Set<UUID> GONE = Collections.newSetFromMap(new LinkedHashMap<UUID, Boolean>() {
        @Override
        protected boolean removeEldestEntry(Entry<UUID, Boolean> eldest) {
            return this.size() > 4096;
        }
    });
    private static final Map<UUID, String> DOWN = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final Map<Entity, Integer> VANISH = new HashMap<>();
    private static final String SHOT_DAMAGE = "SprauteFightDamage";
    private static final String SHOT_PUSH = "SprauteFightPush";

    public static void registerHooks() {
        Hooks.ENTITY_LEAVE.add(Fights::onLeave);
        Hooks.ENTITY_JOIN.add(Fights::onJoin);
        Hooks.SERVER_TICK.add(Fights::onTick);
        Hooks.LIVING_ATTACK.add(Priority.HIGH, Fights::onAttack);
    }

    private Fights() {
    }

    public static boolean gone(UUID id) {
        return GONE.contains(id);
    }

    public static void onLeave(Hooks.EntityLeave ev) {
        Entity e = ev.getEntity();
        if (!Mc.level(e).isClientSide) {
            RemovalReason why = e.getRemovalReason();
            if (why != null && why.shouldDestroy()) {
                GONE.add(e.getUUID());
            }
        }
    }

    public static void onJoin(Hooks.EntityJoin ev) {
        if (!ev.getLevel().isClientSide) {
            GONE.remove(ev.getEntity().getUUID());
        }
    }

    public static FightDef style(String project, String name) {
        if (name != null && !name.isBlank()) {
            String json = SprauteRuntime.get().gui(project, "fight:" + name);
            return json != null ? FightDef.fromJson(json) : FightDef.preset(name);
        } else {
            return FightDef.preset("fists");
        }
    }

    public static void start(Entity e, String name, int scriptId, FightDef def, Entity target) {
        if (!(e instanceof PathfinderMob)) {
            throw new IllegalArgumentException(name + ": эта сущность не умеет драться (нужен живой персонаж)");
        } else {
            Fights.Fighter old = FIGHTERS.get(e.getUUID());
            Fights.Fighter f = new Fights.Fighter(e.getUUID(), name, scriptId, def);
            if (old != null && old.def == def) {
                f.hp = old.hp;
            }

            if (old != null && old.bar != null) {
                old.bar.removeAllPlayers();
            }

            f.target = target != null ? target.getUUID() : null;
            if (old != null) {
                f.heldBefore = old.heldBefore;
            }

            if (!def.weapon.isBlank() && e instanceof LivingEntity le) {
                if (f.heldBefore == null) {
                    f.heldBefore = le.getMainHandItem().copy();
                }

                le.setItemInHand(InteractionHand.MAIN_HAND, ItemSpec.stack(def.weapon, 1));
            }

            DOWN.remove(e.getUUID());
            FIGHTERS.put(e.getUUID(), f);
            if (def.bossBar) {
                f.bar = new ServerBossEvent(Component.literal(def.bossName.isEmpty() ? name : def.bossName), BossBarColor.RED, BossBarOverlay.PROGRESS);

                for (ServerPlayer p : SprauteRuntime.audience(scriptId)) {
                    f.bar.addPlayer(p);
                }
            }

            stance(e, f);
            post("fightstart", f, Map.of());
        }
    }

    public static void stop(Entity e) {
        Fights.Fighter f = FIGHTERS.remove(e.getUUID());
        if (f != null) {
            end(e, f);
        }
    }

    private static void end(Entity e, Fights.Fighter f) {
        if (f.bar != null) {
            f.bar.removeAllPlayers();
        }

        if (e instanceof LivingEntity le) {
            if (le.isUsingItem()) {
                le.stopUsingItem();
            }

            if (f.heldBefore != null) {
                le.setItemInHand(InteractionHand.MAIN_HAND, f.heldBefore);
            }
        }

        if (e instanceof Mob m) {
            m.getNavigation().stop();
        }

        if (e instanceof AnimatedEntity ae && f.state != Fights.State.DOWN) {
            play(ae, "", false, false);
        }
    }

    public static void stopScript(int scriptId) {
        MinecraftServer s = Platform.get().server();
        Iterator<Fights.Fighter> it = FIGHTERS.values().iterator();

        while (it.hasNext()) {
            Fights.Fighter f = it.next();
            if (f.scriptId == scriptId) {
                it.remove();
                Entity e = s != null ? find(s, f.id) : null;
                if (e != null) {
                    end(e, f);
                } else if (f.bar != null) {
                    f.bar.removeAllPlayers();
                }
            }
        }
    }

    public static void heal(Entity e, float amount) {
        Fights.Fighter f = FIGHTERS.get(e.getUUID());
        if (f != null) {
            f.hp = amount < 0.0F ? f.def.health : Math.min(f.def.health, f.hp + amount);
            if (f.state == Fights.State.DOWN) {
                f.state = Fights.State.CHASE;
                DOWN.remove(e.getUUID());
                if (e instanceof AnimatedEntity ae) {
                    stance(ae, f);
                }
            }

            if (f.bar != null) {
                f.bar.setProgress(f.hp / f.def.health);
            }
        }
    }

    public static boolean fighting(Entity e) {
        Fights.Fighter f = FIGHTERS.get(e.getUUID());
        return f != null && f.state != Fights.State.DOWN;
    }

    public static boolean defeated(Entity e) {
        return DOWN.containsKey(e.getUUID());
    }

    public static double hp(Entity e) {
        Fights.Fighter f = FIGHTERS.get(e.getUUID());
        return f == null ? 0.0 : Math.max(0.0F, f.hp);
    }

    public static double maxHp(Entity e) {
        Fights.Fighter f = FIGHTERS.get(e.getUUID());
        return f == null ? 0.0 : f.def.health;
    }

    public static void hold(Entity e, int ticks) {
        Fights.Fighter f = FIGHTERS.get(e.getUUID());
        if (f != null) {
            MinecraftServer s = Platform.get().server();
            f.heldUntil = ticks > 0 && s != null ? s.getTickCount() + ticks : 0;
            if (ticks > 0 && e instanceof Mob m) {
                m.getNavigation().stop();
            }
        }
    }

    public static boolean tune(Entity e, Double armor, Double speed, Double power, Double stagger, Double block, Double dodge) {
        Fights.Fighter f = FIGHTERS.get(e.getUUID());
        if (f == null) {
            return false;
        } else {
            if (armor != null) {
                f.def.armor = (float)Math.max(0.0, Math.min(1.0, armor));
            }

            if (speed != null) {
                f.def.speed = (float)Math.max(0.0, speed);
            }

            if (power != null) {
                f.power = (float)Math.max(0.0, power);
            }

            if (stagger != null) {
                f.def.staggerChance = (float)Math.max(0.0, Math.min(1.0, stagger));
            }

            if (block != null) {
                f.def.block.chance = (float)Math.max(0.0, Math.min(1.0, block));
            }

            if (dodge != null) {
                f.def.dodge.chance = (float)Math.max(0.0, Math.min(1.0, dodge));
            }

            return true;
        }
    }

    public static void unload() {
        for (Fights.Fighter f : FIGHTERS.values()) {
            if (f.bar != null) {
                f.bar.removeAllPlayers();
            }
        }

        FIGHTERS.clear();
        DOWN.clear();
        VANISH.clear();
    }

    private static Optional<Animation> anim(Entity e, String id) {
        if (id != null && !id.isBlank()) {
            try {
                if (e instanceof SprauteModelEntity me) {
                    EntityLibrary.Look look = EntityLibrary.look(me.model());
                    if (look != null) {
                        int bang = id.indexOf(33);
                        Optional<Animation> a = bang >= 0 ? look.animation(id.substring(bang + 1)) : Optional.empty();
                        if (a.isEmpty()) {
                            a = look.animation(id);
                        }

                        if (a.isPresent()) {
                            return a;
                        }
                    }
                }

                return Anims.resolve(id);
            } catch (RuntimeException var6) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public static int play(Entity e, String id, boolean loop, boolean hold) {
        if (e instanceof AnimatedEntity ae) {
            try {
                if (id != null && !id.isBlank()) {
                    Optional<Animation> a = anim(e, id);
                    if (a.isEmpty()) {
                        return 0;
                    } else {
                        ConfiguredAnimation ca = a.get().configure().transitionTime(0.12F);
                        if (loop) {
                            ca.loopMode(LoopMode.LOOP);
                        } else if (hold) {
                            ca.loopMode(LoopMode.HOLD_ON_LAST_FRAME);
                        }

                        ae.startAnimation("action", ca);
                        return Math.max(1, Anims.lengthTicks(a.get(), 1.0));
                    }
                } else {
                    ae.startAnimation("action", Animation.NULL.configure().transitionTime(0.2F));
                    return 0;
                }
            } catch (RuntimeException var7) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static void stance(Entity e, Fights.Fighter f) {
        f.stanceOn = !f.def.idle.isBlank() && play(e, f.def.idle, true, false) > 0;
    }

    private static void moving(Entity e, Fights.Fighter f) {
        if (f.stanceOn && !f.def.stanceMoving) {
            play(e, "", false, false);
            f.stanceOn = false;
        }
    }

    private static void sound(Entity e, String id, float pitch) {
        if (id != null && !id.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            SoundEvent s = rl == null ? null : Regs.SOUND_EVENT.get(rl);
            if (s != null) {
                Mc.level(e).playSound(null, e.getX(), e.getY(), e.getZ(), s, SoundSource.HOSTILE, 1.0F, pitch);
            }
        }
    }

    private static Entity find(MinecraftServer s, UUID id) {
        for (ServerLevel l : s.getAllLevels()) {
            Entity e = l.getEntity(id);
            if (e != null) {
                return e;
            }
        }

        return null;
    }

    public static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END) {
            if (!VANISH.isEmpty()) {
                Iterator<Entry<Entity, Integer>> it = VANISH.entrySet().iterator();

                while (it.hasNext()) {
                    Entry<Entity, Integer> v = it.next();
                    v.setValue(v.getValue() - 1);
                    if (v.getValue() <= 0) {
                        it.remove();
                        Entity e = v.getKey();
                        if (e.isAlive() && Mc.level(e) instanceof ServerLevel sl) {
                            sl.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.8, e.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
                            e.discard();
                        }
                    }
                }
            }

            if (!FIGHTERS.isEmpty()) {
                MinecraftServer s = Platform.get().server();
                if (s != null) {
                    for (Fights.Fighter f : new ArrayList<>(FIGHTERS.values())) {
                        Entity e = find(s, f.id);
                        if (e != null || GONE.contains(f.id)) {
                            if (e != null && e.isAlive()) {
                                if (f.heldUntil <= s.getTickCount()) {
                                    try {
                                        tick(s, (PathfinderMob)e, f);
                                    } catch (RuntimeException var6) {
                                        SprauteEngine.LOGGER.warn("Spraute fight of {}", f.name, var6);
                                        FIGHTERS.remove(f.id);
                                        end(e, f);
                                    }
                                }
                            } else {
                                FIGHTERS.remove(f.id);
                                if (f.bar != null) {
                                    f.bar.removeAllPlayers();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static LivingEntity target(MinecraftServer s, Fights.Fighter f) {
        if (f.target != null) {
            return find(s, f.target) instanceof LivingEntity l ? l : null;
        } else {
            ServerPlayer best = null;
            Entity self = find(s, f.id);

            for (ServerPlayer p : SprauteRuntime.audience(f.scriptId)) {
                if (!p.isSpectator()
                    && p.isAlive()
                    && (self == null || Mc.level(p) == Mc.level(self))
                    && (best == null || self != null && p.distanceToSqr(self) < best.distanceToSqr(self))) {
                    best = p;
                }
            }

            return best;
        }
    }

    private static void face(Mob e, Entity t) {
        double dx = t.getX() - e.getX();
        double dz = t.getZ() - e.getZ();
        float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        e.setYRot(yaw);
        e.setYBodyRot(yaw);
        e.setYHeadRot(yaw);
        e.getLookControl().setLookAt(t, 30.0F, 30.0F);
    }

    private static void walk(PathfinderMob e, Fights.Fighter f, Vec3 to, Entity chase) {
        double speed = f.def.speed;
        if (e.isNoAi()) {
            Vec3 d = to.subtract(e.position());
            Vec3 flat = new Vec3(d.x, 0.0, d.z);
            if (!(flat.lengthSqr() < 0.04)) {
                Vec3 v = flat.normalize().scale(0.13 * speed);
                double vy = e.getDeltaMovement().y;
                if (e.horizontalCollision && Mc.onGround(e)) {
                    vy = 0.42;
                }

                e.setDeltaMovement(v.x, vy, v.z);
                float yaw = (float)(Math.toDegrees(Math.atan2(d.z, d.x)) - 90.0);
                e.setYRot(yaw);
                e.setYBodyRot(yaw);
            }
        } else {
            if (f.pathAt-- <= 0 || e.getNavigation().isDone()) {
                f.pathAt = 8;
                if (chase != null) {
                    e.getNavigation().moveTo(chase, speed);
                } else {
                    e.getNavigation().moveTo(to.x, to.y, to.z, speed);
                }
            }
        }
    }

    private static void stopWalking(PathfinderMob e) {
        if (!e.isNoAi()) {
            e.getNavigation().stop();
        } else {
            e.setDeltaMovement(0.0, e.getDeltaMovement().y, 0.0);
        }
    }

    private static void tick(MinecraftServer s, PathfinderMob e, Fights.Fighter f) {
        if (f.bar != null) {
            f.bar.setProgress(Math.max(0.0F, f.hp / f.def.health));

            for (ServerPlayer p : SprauteRuntime.audience(f.scriptId)) {
                if (!f.bar.getPlayers().contains(p)) {
                    f.bar.addPlayer(p);
                }
            }
        }

        if (f.state != Fights.State.DOWN) {
            LivingEntity t = target(s, f);
            if (t instanceof ServerPlayer sp
                && !sp.isSpectator()
                && (!sp.isAlive() || sp.isRemoved() || Mc.level(sp) != Mc.level(e) || sp.distanceTo(e) > f.def.giveUp)) {
                stopWalking(e);
            } else if (t == null && f.target != null && !GONE.contains(f.target)) {
                stopWalking(e);
            } else {
                boolean targetGone = t == null
                    || !t.isAlive()
                    || t.isRemoved()
                    || t instanceof ServerPlayer sp && sp.isSpectator()
                    || defeated(t)
                    || t != null && Mc.level(t) != Mc.level(e)
                    || t != null && t.distanceTo(e) > f.def.giveUp;
                if (targetGone) {
                    post("fightwin", f, t != null ? Map.of("target", nameOf(t)) : Map.of());
                    FIGHTERS.remove(f.id);
                    end(e, f);
                } else {
                    if (f.cooldown > 0) {
                        f.cooldown--;
                    }

                    if (f.coverCooldown > 0) {
                        f.coverCooldown--;
                    }

                    if (f.healCooldown > 0) {
                        f.healCooldown--;
                    }

                    double dist = e.distanceTo(t);
                    f.timer++;
                    switch (f.state) {
                        case ATTACK:
                            f.stanceOn = false;
                            stopWalking(e);
                            if (!f.hitDone) {
                                face(e, t);
                            }

                            if (!f.hitDone && f.timer >= f.windup) {
                                f.hitDone = true;
                                if (f.attack.ranged()) {
                                    if (e.isUsingItem()) {
                                        e.stopUsingItem();
                                    }

                                    shoot(e, f, t);
                                    int rel = f.attack.release.isBlank() ? 0 : play(e, f.attack.release, false, false);
                                    if (rel > 0) {
                                        f.length = Math.max(f.length, f.timer + rel);
                                    }

                                    if (f.timer >= f.length) {
                                        back(e, f);
                                    }

                                    return;
                                }

                                if (f.attack.lunge > 0.0F) {
                                    Vec3 dir = t.position().subtract(e.position()).multiply(1.0, 0.0, 1.0).normalize().scale(f.attack.lunge * 0.35);
                                    e.setDeltaMovement(dir.x, e.getDeltaMovement().y, dir.z);
                                }

                                hit(e, f, t);
                            }

                            if (f.timer >= f.length) {
                                back(e, f);
                            }
                            break;
                        case COVER:
                            cover(e, f, t);
                            break;
                        case HEAL:
                            stopWalking(e);
                            if (f.timer >= f.length) {
                                healed(e, f);
                            }
                            break;
                        case BLOCK:
                        case DODGE:
                        case HURT:
                            if (f.state != Fights.State.DODGE) {
                                stopWalking(e);
                            }

                            face(e, t);
                            if (f.timer >= f.length) {
                                back(e, f);
                            }
                            break;
                        case FLEE:
                            Vec3 away = e.position().subtract(t.position()).multiply(1.0, 0.0, 1.0);
                            away = away.lengthSqr() < 0.01 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                            if (dist < 14.0) {
                                walk(e, f, e.position().add(away.scale(10.0)), null);
                            } else {
                                stopWalking(e);
                            }
                            break;
                        default:
                            if (f.def.fleeAt > 0.0F && f.hp < f.def.health * f.def.fleeAt) {
                                f.state = Fights.State.FLEE;
                                play(e, "", false, false);
                                post("fightflee", f, Map.of());
                                return;
                            }

                            if (care(e, f, t)) {
                                return;
                            }

                            double tw = t.getBbWidth() / 2.0F;
                            boolean see = e.hasLineOfSight(t);
                            if (f.cooldown <= 0) {
                                List<FightDef.Attack> ready = new ArrayList<>();

                                for (FightDef.Attack a : f.def.attacks) {
                                    if (a.ranged() ? see && dist <= a.range + tw : dist <= (a.range + tw) * 0.9) {
                                        ready.add(a);
                                    }
                                }

                                if (!ready.isEmpty()) {
                                    stopWalking(e);
                                    face(e, t);
                                    attack(e, f, pick(ready));
                                    return;
                                }
                            }

                            double melee = range(f.def, false);
                            double ranged = range(f.def, true);
                            double want = f.def.distance > 0.0F ? f.def.distance : (melee > 0.0 ? (melee + tw) * 0.9 : Math.max(2.0, ranged * 0.7));
                            if (f.def.tooClose > 0.0F && dist < f.def.tooClose) {
                                Vec3 awayx = e.position().subtract(t.position()).multiply(1.0, 0.0, 1.0);
                                awayx = awayx.lengthSqr() < 0.01 ? Vec3.directionFromRotation(0.0F, e.getYRot() + 180.0F) : awayx.normalize();
                                moving(e, f);
                                walk(e, f, e.position().add(awayx.scale(4.0)), null);
                            } else if (!(dist > want + (f.def.distance > 0.0F ? 1.5 : 0.0)) && (melee != 0.0 || see)) {
                                stopWalking(e);
                                face(e, t);
                                if (!f.stanceOn) {
                                    stance(e, f);
                                }
                            } else {
                                moving(e, f);
                                walk(e, f, t.position(), t);
                            }
                    }
                }
            }
        }
    }

    private static double range(FightDef d, boolean shots) {
        double m = shots ? 0.0 : 99.0;

        for (FightDef.Attack a : d.attacks) {
            if (a.ranged() == shots) {
                m = shots ? Math.max(m, (double)a.range) : Math.min(m, (double)a.range);
            }
        }

        return m == 99.0 ? 0.0 : m;
    }

    private static FightDef.Attack pick(List<FightDef.Attack> from) {
        int total = 0;

        for (FightDef.Attack a : from) {
            total += a.weight;
        }

        int r = RANDOM.nextInt(Math.max(1, total));

        for (FightDef.Attack a : from) {
            r -= a.weight;
            if (r < 0) {
                return a;
            }
        }

        return from.get(0);
    }

    private static void attack(PathfinderMob e, Fights.Fighter f, FightDef.Attack a) {
        f.attack = a;
        f.state = Fights.State.ATTACK;
        f.timer = 0;
        f.hitDone = false;
        int len = play(e, a.anim, false, false);
        f.length = a.duration > 0 ? a.duration : Math.max(8, len > 0 ? len : 12);
        f.windup = a.windup > 0 ? Math.min(a.windup, f.length) : Math.max(3, Math.round(f.length * 0.45F));
        if (len == 0 && a.anim.isBlank() && !a.ranged()) {
            e.swing(InteractionHand.MAIN_HAND);
        }

        if (a.ranged() && e.getMainHandItem().getUseAnimation() == UseAnim.BOW) {
            e.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    private static void back(PathfinderMob e, Fights.Fighter f) {
        if (e.isUsingItem()) {
            e.stopUsingItem();
        }

        if (f.state == Fights.State.ATTACK && f.attack != null) {
            f.cooldown = f.attack.cooldown;
        }

        f.state = Fights.State.CHASE;
        f.timer = 0;
        stance(e, f);
    }

    private static void hit(PathfinderMob e, Fights.Fighter f, LivingEntity t) {
        FightDef.Attack a = f.attack;
        double reach = a.range + t.getBbWidth() / 2.0F + 0.3;
        if (e.distanceTo(t) > reach) {
            sound(e, "minecraft:entity.player.attack.nodamage", 1.0F);
        } else {
            Vec3 look = Vec3.directionFromRotation(0.0F, e.getYRot());
            Vec3 to = t.position().subtract(e.position()).multiply(1.0, 0.0, 1.0);
            if (to.lengthSqr() > 0.01 && look.dot(to.normalize()) < 0.3) {
                sound(e, "minecraft:entity.player.attack.nodamage", 1.0F);
            } else {
                sound(e, a.sound.isBlank() ? f.def.hitSound : a.sound, 0.9F + RANDOM.nextFloat() * 0.2F);
                t.hurt(Mc.mobAttack(Mc.level(e), e), a.damage * f.power);
                if (a.knockback > 0.0F) {
                    t.knockback(a.knockback, e.getX() - t.getX(), e.getZ() - t.getZ());
                    t.hurtMarked = true;
                }
            }
        }
    }

    private static void shoot(PathfinderMob e, Fights.Fighter f, LivingEntity t) {
        FightDef.Attack a = f.attack;
        face(e, t);
        ServerLevel level = (ServerLevel)Mc.level(e);
        ResourceLocation id = ResourceLocation.tryParse(a.projectile);
        EntityType<?> type = id == null ? null : Regs.ENTITY_TYPE.getOptional(id).orElse(null);
        boolean any = false;

        for (int i = 0; i < a.shots; i++) {
            Entity made = type == null ? null : type.create(level);
            if (!(made instanceof Projectile)) {
                if (made != null) {
                    made.discard();
                }

                made = EntityType.ARROW.create(level);
            }

            if (!(made instanceof Projectile p)) {
                return;
            }

            Vec3 look = Vec3.directionFromRotation(0.0F, e.getYRot());
            Vec3 from = e.getEyePosition().add(look.scale(0.6)).add(0.0, -0.15, 0.0);
            p.setOwner(e);
            p.moveTo(from.x, from.y, from.z, e.getYRot(), 0.0F);
            double dx = t.getX() - from.x;
            double dy = t.getY(0.5) - from.y;
            double dz = t.getZ() - from.z;
            double flat = Math.sqrt(dx * dx + dz * dz);
            if (p instanceof AbstractArrow arrow) {
                arrow.pickup = Pickup.DISALLOWED;
                dy += flat * 0.2;
            } else if (!p.isNoGravity() && !(p instanceof AbstractHurtingProjectile)) {
                dy += flat * 0.12;
            }

            if (p instanceof AbstractHurtingProjectile h) {
                Vec3 dir = new Vec3(dx, dy, dz)
                    .normalize()
                    .yRot((float)Math.toRadians((RANDOM.nextFloat() - 0.5F) * a.spread))
                    .xRot((float)Math.toRadians((RANDOM.nextFloat() - 0.5F) * a.spread));
                h.xPower = dir.x * 0.1 * a.shotSpeed;
                h.yPower = dir.y * 0.1 * a.shotSpeed;
                h.zPower = dir.z * 0.1 * a.shotSpeed;
                h.setDeltaMovement(Vec3.ZERO);
            } else {
                p.shoot(dx, dy, dz, a.shotSpeed, a.spread);
            }

            CompoundTag tag = Platform.get().persistentData(p);
            tag.putFloat("SprauteFightDamage", a.damage * f.power);
            tag.putFloat("SprauteFightPush", a.knockback);
            level.addFreshEntity(p);
            any = true;
        }

        if (any) {
            sound(e, a.sound.isBlank() ? "minecraft:entity.arrow.shoot" : a.sound, 0.9F + RANDOM.nextFloat() * 0.2F);
        }
    }

    private static boolean shotHit(Hooks.LivingAttack ev, LivingEntity e) {
        Entity shot = ev.getSource().getDirectEntity();
        if (shot != null && !(shot instanceof LivingEntity)) {
            CompoundTag tag = Platform.get().persistentData(shot);
            if (!tag.contains("SprauteFightDamage")) {
                return false;
            } else {
                ev.setCanceled(true);
                float dmg = tag.getFloat("SprauteFightDamage");
                float push = tag.getFloat("SprauteFightPush");
                tag.remove("SprauteFightDamage");
                shot.discard();
                Entity owner = ev.getSource().getEntity();
                if (owner == e) {
                    return true;
                } else {
                    Fights.Fighter by = owner != null ? FIGHTERS.get(owner.getUUID()) : null;
                    Fights.Fighter to = FIGHTERS.get(e.getUUID());
                    if (by != null && to != null && !e.getUUID().equals(by.target) && !owner.getUUID().equals(to.target)) {
                        return true;
                    } else {
                        e.hurt(owner instanceof LivingEntity lo ? Mc.mobAttack(Mc.level(e), lo) : ev.getSource(), dmg);
                        if (push > 0.0F && owner != null) {
                            e.knockback(push, owner.getX() - e.getX(), owner.getZ() - e.getZ());
                            e.hurtMarked = true;
                        }

                        return true;
                    }
                }
            }
        } else {
            return false;
        }
    }

    private static boolean care(PathfinderMob e, Fights.Fighter f, LivingEntity t) {
        FightDef.Heal h = f.def.heal;
        if (h.at > 0.0F && f.healsLeft > 0 && f.healCooldown <= 0 && f.hp < f.def.health * h.at) {
            f.healCooldown = 60;
            if (h.hide && hide(e, f, t, "heal")) {
                f.wantHeal = true;
                return true;
            } else {
                heal(e, f);
                return true;
            }
        } else {
            FightDef.Cover c = f.def.cover;
            return c.at > 0.0F && f.coverCooldown <= 0 && f.hp < f.def.health * c.at ? hide(e, f, t, "low") : false;
        }
    }

    private static boolean hide(PathfinderMob e, Fights.Fighter f, LivingEntity t, String why) {
        f.coverCooldown = f.def.cover.time * 2 + 40;
        Vec3 at = coverPlace(e, t, f.def.cover.radius);
        if (at == null) {
            return false;
        } else {
            f.state = Fights.State.COVER;
            f.timer = 0;
            f.coverPos = at;
            f.coverArrived = false;
            f.wantHeal = false;
            play(e, "", false, false);
            f.stanceOn = false;
            Map<String, Object> d = new HashMap<>();
            d.put("why", why);
            post("fightcover", f, d);
            return true;
        }
    }

    private static void cover(PathfinderMob e, Fights.Fighter f, LivingEntity t) {
        if (f.coverArrived) {
            stopWalking(e);
            if (f.timer >= f.def.cover.time) {
                back(e, f);
            }
        } else {
            if (!(e.position().distanceToSqr(f.coverPos) < 1.5) && f.timer <= 200) {
                walk(e, f, f.coverPos, null);
            } else {
                f.coverArrived = true;
                f.timer = 0;
                stopWalking(e);
                if (f.wantHeal) {
                    f.wantHeal = false;
                    heal(e, f);
                    return;
                }

                if (!f.def.cover.anim.isBlank()) {
                    play(e, f.def.cover.anim, true, false);
                }
            }
        }
    }

    private static void heal(PathfinderMob e, Fights.Fighter f) {
        stopWalking(e);
        f.state = Fights.State.HEAL;
        f.timer = 0;
        f.stanceOn = false;
        int len = play(e, f.def.heal.anim, false, true);
        f.length = f.def.heal.time > 0 ? f.def.heal.time : Math.max(20, len > 0 ? len : 30);
        sound(e, f.def.heal.sound, 1.0F);
    }

    private static void healed(PathfinderMob e, Fights.Fighter f) {
        float before = f.hp;
        f.hp = Math.min(f.def.health, f.hp + f.def.heal.amount);
        f.healsLeft--;
        if (Mc.level(e) instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 5, 0.4, 0.2, 0.4, 0.02);
        }

        Map<String, Object> d = new HashMap<>();
        d.put("amount", Math.round((f.hp - before) * 10.0F) / 10.0);
        post("fightheal", f, d);
        back(e, f);
    }

    private static Vec3 coverPlace(PathfinderMob e, LivingEntity t, float radius) {
        Level level = Mc.level(e);
        Vec3 eyes = t.getEyePosition();
        List<Vec3> found = new ArrayList<>();

        for (double r = 3.0; r <= radius; r += 2.0) {
            int steps = (int)Math.max(8.0, r * 3.0);

            for (int i = 0; i < steps; i++) {
                double ang = (Math.PI * 2) * i / steps;
                Vec3 ground = ground(level, e.getX() + Math.cos(ang) * r, e.getY(), e.getZ() + Math.sin(ang) * r);
                if (ground != null && !(ground.distanceTo(t.position()) < 4.0)) {
                    Vec3 head = ground.add(0.0, e.getEyeHeight() * 0.9, 0.0);
                    BlockHitResult hit = level.clip(new ClipContext(eyes, head, Block.COLLIDER, Fluid.NONE, t));
                    if (hit.getType() == Type.BLOCK) {
                        found.add(ground);
                    }
                }
            }
        }

        found.sort(Comparator.comparingDouble(vx -> vx.distanceToSqr(e.position())));
        int tried = 0;

        for (Vec3 v : found) {
            if (tried++ >= 6) {
                break;
            }

            Path path = e.getNavigation().createPath(Mc.blockPos(v.x, v.y, v.z), 0);
            if (path != null && path.canReach()) {
                return v;
            }
        }

        return null;
    }

    private static Vec3 ground(Level level, double x, double y, double z) {
        BlockPos base = Mc.blockPos(x, y, z);

        for (int dy = 2; dy >= -3; dy--) {
            BlockPos feet = base.offset(0, dy, 0);
            if (level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
                BlockPos below = feet.below();
                if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                    return Vec3.atBottomCenterOf(feet);
                }
            }
        }

        return null;
    }

    public static void onAttack(Hooks.LivingAttack ev) {
        LivingEntity e = ev.getEntity();
        if (!Mc.level(e).isClientSide) {
            if (!shotHit(ev, e)) {
                Fights.Fighter f = FIGHTERS.get(e.getUUID());
                if (f != null) {
                    ev.setCanceled(true);
                    if ((Hitboxes.flags(e) & 32) == 0) {
                        if (f.state != Fights.State.DOWN && e instanceof PathfinderMob m) {
                            Entity by = ev.getSource().getEntity();
                            if (by instanceof LivingEntity lb && lb != e && f.target != null && !lb.getUUID().equals(f.target) && by instanceof ServerPlayer) {
                                f.target = lb.getUUID();
                            }

                            f.lastBy = by != null ? nameOf(by) : "";
                            float dmg = ev.getAmount();
                            boolean busy = f.state == Fights.State.ATTACK && !f.hitDone && f.timer > 2;
                            if (!busy && f.def.dodge.chance > 0.0F && RANDOM.nextFloat() < f.def.dodge.chance) {
                                f.state = Fights.State.DODGE;
                                f.timer = 0;
                                int len = play(m, f.def.dodge.anim, false, false);
                                f.length = Math.max(6, len > 0 ? len : 8);
                                Vec3 side = by != null ? e.position().subtract(by.position()).multiply(1.0, 0.0, 1.0) : Vec3.ZERO;
                                if (side.lengthSqr() < 0.01) {
                                    side = Vec3.directionFromRotation(0.0F, e.getYRot() + 90.0F);
                                }

                                side = side.normalize().yRot((RANDOM.nextBoolean() ? 1 : -1) * 1.2F).scale(f.def.dodge.power * 0.3);
                                e.setDeltaMovement(side.x, 0.2, side.z);
                                sound(e, "minecraft:entity.player.attack.nodamage", 1.3F);
                                post("fightdodge", f, Map.of("by", f.lastBy));
                            } else {
                                boolean blocked = false;
                                if (!busy && f.def.block.chance > 0.0F && RANDOM.nextFloat() < f.def.block.chance) {
                                    blocked = true;
                                    dmg *= 1.0F - Math.max(0.0F, Math.min(1.0F, f.def.block.power));
                                    f.state = Fights.State.BLOCK;
                                    f.timer = 0;
                                    int len = play(m, f.def.block.anim, false, false);
                                    f.length = Math.max(6, Math.min(20, len > 0 ? len : 10));
                                    sound(e, "minecraft:item.shield.block", 0.9F + RANDOM.nextFloat() * 0.2F);
                                }

                                dmg *= 1.0F - f.def.armor;
                                f.hp -= dmg;
                                Mc.showHurt(e, ev.getSource());
                                if (by != null) {
                                    e.knockback(blocked ? 0.15 : 0.35, by.getX() - e.getX(), by.getZ() - e.getZ());
                                }

                                Map<String, Object> d = new HashMap<>();
                                d.put("by", f.lastBy);
                                d.put("damage", Math.round(dmg * 10.0F) / 10.0);
                                d.put("hp", Math.round(Math.max(0.0F, f.hp) * 10.0F) / 10.0);
                                d.put("blocked", blocked);
                                post("fighthit", f, d);
                                if (f.hp <= 0.0F) {
                                    defeat(m, f);
                                } else {
                                    if (f.state != Fights.State.COVER
                                        && f.state != Fights.State.HEAL
                                        && f.def.cover.chance > 0.0F
                                        && f.coverCooldown <= 0
                                        && RANDOM.nextFloat() < f.def.cover.chance) {
                                        LivingEntity from = by instanceof LivingEntity lb ? lb : null;
                                        if (from != null && hide(m, f, from, "hurt")) {
                                            return;
                                        }
                                    }

                                    if (!blocked && RANDOM.nextFloat() < f.def.staggerChance) {
                                        int len = play(m, f.def.hurt, false, false);
                                        if (len > 0 || f.state == Fights.State.ATTACK || f.state == Fights.State.HEAL) {
                                            f.state = Fights.State.HURT;
                                            f.timer = 0;
                                            f.length = Math.max(5, Math.min(25, len > 0 ? len : 6));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void defeat(PathfinderMob e, Fights.Fighter f) {
        f.state = Fights.State.DOWN;
        f.hp = 0.0F;
        stopWalking(e);
        DOWN.put(f.id, f.name);
        if (f.bar != null) {
            f.bar.removeAllPlayers();
        }

        int len = play(e, f.def.defeat, false, true);
        sound(e, "minecraft:entity.player.hurt", 0.7F);
        post("fightdown", f, Map.of("by", f.lastBy));
        String var3 = f.def.defeatMode;
        switch (var3) {
            case "die":
                FIGHTERS.remove(f.id);
                Map<String, Object> d = new HashMap<>();
                d.put("__entity", e);
                d.put("entity", String.valueOf(Regs.ENTITY_TYPE.getKey(e.getType())));
                d.put("name", e.getName().getString());
                d.put("by", f.lastBy);
                d.put("killer", f.lastBy);
                d.put("source", "fight");
                d.put("x", e.getX());
                d.put("y", e.getY());
                d.put("z", e.getZ());
                GameEvents.post("npcdeath", SprauteRuntime.playerOf(f.scriptId), d);
                VANISH.put(e, Math.max(10, len) + 10);
                break;
            case "stand":
                FIGHTERS.remove(f.id);
                end(e, f);
        }
    }

    private static String nameOf(Entity e) {
        if (e instanceof ServerPlayer p) {
            return "player";
        } else {
            Fights.Fighter f = FIGHTERS.get(e.getUUID());
            if (f != null) {
                return f.name;
            } else {
                String dn = DOWN.get(e.getUUID());
                return dn != null ? dn : String.valueOf(Regs.ENTITY_TYPE.getKey(e.getType()));
            }
        }
    }

    private static void post(String type, Fights.Fighter f, Map<String, Object> extra) {
        Map<String, Object> d = new HashMap<>(extra);
        d.put("who", f.name);
        d.putIfAbsent("hp", Math.round(Math.max(0.0F, f.hp) * 10.0F) / 10.0);
        GameEvents.post(type, SprauteRuntime.playerOf(f.scriptId), d);
    }

    static final class Fighter {
        final UUID id;
        final String name;
        final int scriptId;
        final FightDef def;
        UUID target;
        float hp;
        Fights.State state = Fights.State.CHASE;
        int timer;
        int length;
        int windup;
        int cooldown;
        int pathAt;
        FightDef.Attack attack;
        boolean hitDone;
        boolean stanceOn;
        String lastBy = "";
        ServerBossEvent bar;
        float power = 1.0F;
        int heldUntil;
        int healsLeft;
        Vec3 coverPos;
        boolean coverArrived;
        boolean wantHeal;
        int coverCooldown;
        int healCooldown;
        ItemStack heldBefore;

        Fighter(UUID id, String name, int scriptId, FightDef def) {
            this.id = id;
            this.name = name;
            this.scriptId = scriptId;
            this.def = def;
            this.hp = def.health;
            this.healsLeft = def.heal.charges;
        }
    }

    static enum State {
        CHASE,
        ATTACK,
        BLOCK,
        DODGE,
        HURT,
        FLEE,
        DOWN,
        COVER,
        HEAL;
    }
}
