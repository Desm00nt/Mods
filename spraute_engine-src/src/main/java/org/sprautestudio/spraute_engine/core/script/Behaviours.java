package org.sprautestudio.spraute_engine.core.script;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Behaviours {
    private static final Map<UUID, Map<Behaviours.Type, Behaviours.Running>> RUNNING = new HashMap<>();
    private static final Map<UUID, Long> ENTITY_CLICKS = new HashMap<>();
    private static final Map<BlockPos, Long> BLOCK_CLICKS = new HashMap<>();
    private static final List<Behaviours.Later> LATER = new ArrayList<>();

    public static void registerHooks() {
        Hooks.ENTITY_INTERACT.add(Behaviours::onEntityClick);
        Hooks.RIGHT_CLICK_BLOCK.add(Behaviours::onBlockClick);
        Hooks.ENTITY_JOIN.add(Behaviours::onJoin);
        Hooks.LIVING_DEATH.add(Behaviours::onDeath);
        Hooks.SERVER_TICK.add(Behaviours::onTick);
    }

    private Behaviours() {
    }

    private static void event(String name, Behaviours.Running r) {
        Map<String, Object> d = new HashMap<>();
        d.put("type", r.type.name().toLowerCase(Locale.ROOT));
        d.put("name", r.body.getName().getString());
        d.put("__entity", r.body);
        GameEvents.fire(name, null, d);
    }

    public static void start(Entity e, Behaviours.Type type, BooleanSupplier tick, Runnable cancel) {
        stop(e, type);
        Behaviours.Running r = new Behaviours.Running(e, type, tick, cancel);
        RUNNING.computeIfAbsent(e.getUUID(), k -> new EnumMap<>(Behaviours.Type.class)).put(type, r);
        event("behaviorstart", r);
    }

    public static void stop(Entity e, Behaviours.Type type) {
        if (e != null) {
            Map<Behaviours.Type, Behaviours.Running> m = RUNNING.get(e.getUUID());
            Behaviours.Running r = m != null ? m.remove(type) : null;
            if (r != null) {
                if (r.cancel != null) {
                    r.cancel.run();
                }

                event("behaviorend", r);
            }
        }
    }

    public static void clear(Entity e) {
        if (e != null) {
            for (Behaviours.Type t : Behaviours.Type.values()) {
                stop(e, t);
            }

            Walkers.stop(e);
        }
    }

    public static void setPaused(Entity e, Behaviours.Type type, boolean paused) {
        Map<Behaviours.Type, Behaviours.Running> m = e == null ? null : RUNNING.get(e.getUUID());
        Behaviours.Running r = m != null ? m.get(type) : null;
        if (r != null) {
            r.paused = paused;
        }
    }

    public static void walking(Entity e) {
        start(e, Behaviours.Type.MOVEMENT, () -> Walkers.walking(e), () -> Walkers.stop(e));
    }

    public static void follow(Entity e, Supplier<? extends Entity> target, double speed, double min, double max) {
        Vec3[] going = new Vec3[]{null};
        Entity first = target.get();
        if (first != null) {
            lookOnce(e, first.getEyePosition());
        }

        start(e, Behaviours.Type.MOVEMENT, () -> {
            Entity t = target.get();
            if (t != null && Mc.level(t) == Mc.level(e)) {
                double d = e.distanceTo(t);
                if (max >= 0.0 && d > max) {
                    Walkers.stop(e);
                    going[0] = null;
                    e.teleportTo(t.getX(), t.getY(), t.getZ());
                } else if (d <= min) {
                    Walkers.stop(e);
                    going[0] = null;
                } else if (going[0] == null || going[0].distanceToSqr(t.position()) > 1.0 || !Walkers.walking(e)) {
                    going[0] = t.position();
                    Walkers.go(e, going[0], speed, false);
                }

                return true;
            } else {
                return true;
            }
        }, () -> Walkers.stop(e));
    }

    public static void wander(Entity e, double speed, double radius) {
        Vec3 home = e.position();
        int[] pause = new int[]{0};
        start(e, Behaviours.Type.MOVEMENT, () -> {
            if (!Walkers.walking(e) && --pause[0] <= 0) {
                pause[0] = 20 + Mc.level(e).random.nextInt(100);
                if (e instanceof Mob m) {
                    double angle = Mc.level(e).random.nextDouble() * Math.PI * 2.0;
                    double r = Mc.level(e).random.nextDouble() * radius;
                    double x = home.x + Math.cos(angle) * r;
                    double z = home.z + Math.sin(angle) * r;
                    Path path = m.getNavigation().createPath(x, home.y, z, 0);
                    if (path != null && path.canReach()) {
                        Node end = path.getEndNode();
                        if (end != null) {
                            Walkers.go(e, new Vec3(end.x + 0.5, end.y, end.z + 0.5), speed, false);
                        }
                    }

                    return true;
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }, () -> Walkers.stop(e));
    }

    public static void lookOnce(Entity e, Vec3 at) {
        Vec3 d = at.subtract(e.getEyePosition());
        float yaw = (float)(Mth.atan2(d.z, d.x) * 180.0F / (float)Math.PI) - 90.0F;
        float pitch = (float)(-(Mth.atan2(d.y, d.horizontalDistance()) * 180.0F / (float)Math.PI));
        e.setYRot(yaw);
        e.setXRot(pitch);
        e.setYHeadRot(yaw);
        if (e instanceof LivingEntity le) {
            le.yBodyRot = yaw;
        }
    }

    public static void afterTicks(int ticks, Runnable r) {
        LATER.add(new Behaviours.Later(new int[]{ticks}, r));
    }

    public static long clicks(Entity e) {
        return e == null ? 0L : ENTITY_CLICKS.getOrDefault(e.getUUID(), 0L);
    }

    public static long clicks(BlockPos pos) {
        return BLOCK_CLICKS.getOrDefault(pos, 0L);
    }

    public static void onEntityClick(Hooks.EntityInteract e) {
        if (!e.getLevel().isClientSide && e.getHand() == InteractionHand.MAIN_HAND) {
            ENTITY_CLICKS.merge(e.getTarget().getUUID(), 1L, Long::sum);
        }
    }

    public static void onBlockClick(Hooks.RightClickBlock e) {
        if (!e.getLevel().isClientSide && e.getHand() == InteractionHand.MAIN_HAND) {
            BLOCK_CLICKS.merge(e.getPos(), 1L, Long::sum);
        }
    }

    public static void onJoin(Hooks.EntityJoin e) {
        if (!e.getLevel().isClientSide && SprauteRuntime.env().isPresent() && !Characters.belongs(e.getEntity())) {
            e.setCanceled(true);
        }
    }

    public static void onDeath(Hooks.LivingDeath e) {
        if (!Mc.level(e.getEntity()).isClientSide) {
            Characters.died(e.getEntity());
        }
    }

    public static void onTick(Hooks.ServerTick e) {
        if (e.phase == TickPhase.END) {
            Characters.tick(e.getServer());

            for (Map<Behaviours.Type, Behaviours.Running> m : new ArrayList<>(RUNNING.values())) {
                for (Behaviours.Running r : new ArrayList<>(m.values())) {
                    if (!r.paused && (!r.body.isAlive() || !r.tick.getAsBoolean())) {
                        m.remove(r.type);
                        event("behaviorend", r);
                    }
                }
            }

            RUNNING.values().removeIf(Map::isEmpty);
            if (!LATER.isEmpty()) {
                for (Behaviours.Later l : new ArrayList<>(LATER)) {
                    if (--l.left()[0] <= 0) {
                        LATER.remove(l);
                        l.r().run();
                    }
                }
            }
        }
    }

    public static void unload() {
        RUNNING.clear();
        ENTITY_CLICKS.clear();
        BLOCK_CLICKS.clear();
        LATER.clear();
    }

    private record Later(int[] left, Runnable r) {
    }

    private static final class Running {
        final Entity body;
        final Behaviours.Type type;
        final BooleanSupplier tick;
        final Runnable cancel;
        boolean paused;

        Running(Entity body, Behaviours.Type type, BooleanSupplier tick, Runnable cancel) {
            this.body = body;
            this.type = type;
            this.tick = tick;
            this.cancel = cancel;
        }
    }

    public static enum Type {
        MOVEMENT,
        LOOK,
        COMBAT,
        SPEECH,
        ACTION,
        INTERACTION,
        EMOTION,
        COMPLEX;
    }
}
