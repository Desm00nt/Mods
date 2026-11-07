package org.sprautestudio.spraute_engine.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.custom.ItemApi;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.Priority;

public final class Mechanics {
    private static final Map<UUID, Integer> FALL_SAFE = new HashMap<>();
    private static final Set<UUID> UNDYING = new HashSet<>();
    private static final Map<String, ServerBossEvent> BARS = new LinkedHashMap<>();

    public static void registerHooks() {
        Hooks.LIVING_DEATH.add(Priority.HIGHEST, Mechanics::onDeathUndying);
        Hooks.LIVING_FALL.add(Mechanics::onFall);
        Hooks.PLAYER_LOGGED_OUT.add(Mechanics::onLogout);
    }

    private Mechanics() {
    }

    public static int now() {
        MinecraftServer s = Platform.get().server();
        return s == null ? 0 : s.getTickCount();
    }

    public static void fallSafe(Entity e, double seconds) {
        if (seconds <= 0.0) {
            FALL_SAFE.remove(e.getUUID());
        } else {
            FALL_SAFE.put(e.getUUID(), now() + (int)Math.round(seconds * 20.0));
        }

        e.fallDistance = 0.0F;
    }

    public static void setUndying(Entity e, boolean on) {
        if (on) {
            UNDYING.add(e.getUUID());
        } else {
            UNDYING.remove(e.getUUID());
        }
    }

    public static boolean undying(Entity e) {
        return UNDYING.contains(e.getUUID());
    }

    public static void onDeathUndying(Hooks.LivingDeath e) {
        LivingEntity le = e.getEntity();
        if (!Mc.level(le).isClientSide && UNDYING.contains(le.getUUID())) {
            e.setCanceled(true);
            le.setHealth(Math.max(1.0F, le.getMaxHealth() / 2.0F));
            le.clearFire();
            le.fallDistance = 0.0F;
            if (le instanceof Player p) {
                Map<String, Object> d = new LinkedHashMap<>();
                Entity killer = e.getSource().getEntity();
                if (killer != null) {
                    d.put("entity", killer instanceof Player ? "player" : String.valueOf(Regs.ENTITY_TYPE.getKey(killer.getType())));
                }

                if (killer != null) {
                    d.put("__entity", killer);
                }

                d.put("source", e.getSource().getMsgId());
                d.put("x", Math.floor(p.getX()));
                d.put("y", Math.floor(p.getY()));
                d.put("z", Math.floor(p.getZ()));
                GameEvents.post("death", p, d);
            }
        }
    }

    public static double fallSafeLeft(Entity e) {
        Integer until = FALL_SAFE.get(e.getUUID());
        return until == null ? 0.0 : Math.max(0.0, (until - now()) / 20.0);
    }

    public static void onFall(Hooks.LivingFall e) {
        if (!FALL_SAFE.isEmpty()) {
            Integer until = FALL_SAFE.get(e.getEntity().getUUID());
            if (until != null) {
                if (until < now()) {
                    FALL_SAFE.remove(e.getEntity().getUUID());
                } else {
                    e.setDistance(0.0F);
                    e.setDamageMultiplier(0.0F);
                }
            }
        }
    }

    public static int shockwave(ServerLevel level, Vec3 c, double radius, double power, float damage, Entity except, Entity by) {
        int n = 0;
        AABB box = new AABB(c, c).inflate(radius);

        for (Entity en : level.getEntities((Entity)null, box, x -> x instanceof LivingEntity && x.isAlive() && !x.isSpectator())) {
            if (en != except) {
                Vec3 d = en.position().subtract(c);
                double dist = d.length();
                if (!(dist > radius)) {
                    double k = power * (1.0 - dist / radius * 0.7);
                    Vec3 flat = new Vec3(d.x, 0.0, d.z);
                    flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 0.0) : flat.normalize();
                    en.push(flat.x * k, 0.35 * k + 0.1, flat.z * k);
                    en.hurtMarked = true;
                    if (damage > 0.0F) {
                        DamageSource src = by != null ? Mc.indirectMagic(level, by, by) : Mc.magic(level);
                        en.hurt(src, damage);
                    }

                    n++;
                }
            }
        }

        return n;
    }

    public static int aoe(ServerLevel level, Vec3 at, Mechanics.Area a) {
        int n = 0;
        Vec3 look = a.by != null ? a.by.getLookAngle() : Vec3.ZERO;
        double lookFlat = Math.hypot(look.x, look.z);

        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(a.radius + 1.0))) {
            if (e.isAlive()
                && !e.isSpectator()
                && !(e instanceof ArmorStand)
                && !(e.position().distanceTo(at) > a.radius + e.getBbWidth() / 2.0F)
                && e != a.except
                && areaTakes(e, a)) {
                Vec3 away = e.position().subtract(at);
                double dist = Math.max(0.01, Math.hypot(away.x, away.z));
                if (a.cone > 0.0 && lookFlat > 0.01 && e != a.by) {
                    double dot = (away.x * look.x + away.z * look.z) / (dist * lookFlat);
                    if (Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot)))) > a.cone / 2.0) {
                        continue;
                    }
                }

                if (a.damage > 0.0F) {
                    e.invulnerableTime = 0;
                    e.hurt(a.by != null ? Mc.indirectMagic(level, a.by, a.by) : Mc.magic(level), a.damage);
                }

                if (a.heal > 0.0F) {
                    e.heal(a.heal);
                }

                if (a.fireSeconds > 0) {
                    e.setSecondsOnFire(a.fireSeconds);
                }

                if (a.effect != null) {
                    e.addEffect(new MobEffectInstance(a.effect, a.effectTicks, Math.max(0, a.level - 1)));
                }

                double shove = a.push - a.pull;
                if (shove != 0.0) {
                    motion(e, e.getDeltaMovement().add(away.x / dist * shove, Math.abs(shove) * 0.3, away.z / dist * shove), false);
                }

                n++;
            }
        }

        return n;
    }

    private static boolean areaTakes(LivingEntity e, Mechanics.Area a) {
        if (e == a.by) {
            return a.self || a.allies;
        } else {
            if (!a.only.isBlank()) {
                ResourceLocation key = Regs.ENTITY_TYPE.getKey(e.getType());
                if (key == null || !key.toString().equals(a.only)) {
                    return false;
                }
            }

            if (!a.everyone && a.by != null) {
                boolean own = a.by instanceof Player
                    ? e instanceof Player || e instanceof OwnableEntity pet && pet.getOwner() == a.by
                    : e.getType() == a.by.getType();
                return own == a.allies;
            } else {
                return true;
            }
        }
    }

    public static Vec3 flat(Vec3 v) {
        Vec3 f = new Vec3(v.x, 0.0, v.z);
        return f.lengthSqr() < 1.0E-6 ? Vec3.ZERO : f.normalize();
    }

    public static void motion(Entity e, Vec3 v, boolean safe) {
        e.setDeltaMovement(v);
        e.hasImpulse = true;
        e.hurtMarked = true;
        if (safe) {
            e.fallDistance = 0.0F;
        }
    }

    public static void breakBlock(ServerPlayer p, Vec3 at, boolean drops, boolean wear, boolean force) {
        ServerLevel level = Mc.serverLevel(p);
        BlockPos pos = new BlockPos((int)Math.floor(at.x), (int)Math.floor(at.y), (int)Math.floor(at.z));
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !(state.getDestroySpeed(level, pos) < 0.0F)) {
            boolean fits = p.hasCorrectToolForDrops(state);
            if (fits || force) {
                if (drops && fits && !p.isCreative()) {
                    Block.dropResources(state, level, pos, level.getBlockEntity(pos), p, p.getMainHandItem());
                }

                level.destroyBlock(pos, false, p);
                if (wear && !p.isCreative()) {
                    ItemApi.damage(p, 1);
                }
            }
        }
    }

    public static void explode(ServerLevel level, Vec3 at, float power, boolean fire, boolean breakBlocks, Entity by) {
        Mc.explode(level, by, at, power, fire, breakBlocks);
    }

    public static void lightning(ServerLevel level, Vec3 at, boolean harmless) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(harmless);
            level.addFreshEntity(bolt);
        }
    }

    public static void bar(String id, double value, double max, String title, String color) {
        ServerBossEvent b = BARS.get(id);
        if (b == null) {
            b = new ServerBossEvent(Component.literal(title == null ? id : title), BossBarColor.PURPLE, BossBarOverlay.PROGRESS);
            BARS.put(id, b);
        }

        if (title != null) {
            b.setName(Component.literal(title));
        }

        if (color != null && !color.isBlank()) {
            String p = color.toLowerCase(Locale.ROOT);

            BossBarColor c = switch (p) {
                case "red", "красный" -> BossBarColor.RED;
                case "blue", "синий" -> BossBarColor.BLUE;
                case "green", "зелёный", "зеленый" -> BossBarColor.GREEN;
                case "yellow", "жёлтый", "желтый" -> BossBarColor.YELLOW;
                case "white", "белый" -> BossBarColor.WHITE;
                case "pink", "розовый" -> BossBarColor.PINK;
                default -> BossBarColor.PURPLE;
            };
            b.setColor(c);
        }

        b.setProgress((float)Math.max(0.0, Math.min(1.0, max <= 0.0 ? 0.0 : value / max)));

        for (ServerPlayer p : SprauteRuntime.audience()) {
            if (!b.getPlayers().contains(p)) {
                b.addPlayer(p);
            }
        }
    }

    public static void hideBar(String id) {
        ServerBossEvent b = BARS.remove(id);
        if (b != null) {
            b.removeAllPlayers();
        }
    }

    public static void actionbar(String text) {
        for (ServerPlayer p : SprauteRuntime.audience()) {
            p.displayClientMessage(Component.literal(text), true);
        }
    }

    public static void keyPressed(ServerPlayer p, String key) {
        SprauteRuntime.env().ifPresent(env -> env.keyPressed(p.getUUID(), key));
        if (key.equals("air.left")) {
            GameEvents.post("leftair", p, new HashMap<>());
        } else {
            Map<String, Object> d = new HashMap<>();
            d.put("key", key);
            d.put("onground", Mc.onGround(p));
            d.put("x", p.getX());
            d.put("y", p.getY());
            d.put("z", p.getZ());
            GameEvents.post("keypress", p, d);
        }
    }

    public static void onLogout(Hooks.PlayerLoggedOut e) {
        for (ServerBossEvent b : BARS.values()) {
            if (e.getEntity() instanceof ServerPlayer sp) {
                b.removePlayer(sp);
            }
        }
    }

    public static void unload() {
        for (ServerBossEvent b : BARS.values()) {
            b.removeAllPlayers();
        }

        BARS.clear();
        FALL_SAFE.clear();
    }

    public static Vec3 aimPoint(Entity e, double dist) {
        Raycast.Hit h = Raycast.look(new EntityRef(e), dist);
        return h.at();
    }

    static boolean isPlayer(Entity e) {
        return e instanceof Player;
    }

    public record Area(
        double radius,
        double cone,
        float damage,
        double push,
        double pull,
        int fireSeconds,
        MobEffect effect,
        int effectTicks,
        int level,
        float heal,
        Entity by,
        Entity except,
        String only,
        boolean allies,
        boolean everyone,
        boolean self
    ) {
    }
}
