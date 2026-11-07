package org.sprautestudio.spraute_engine.path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.entity.NpcChunks;
import org.sprautestudio.spraute_engine.core.script.Behaviours;
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;

public final class Walkers {
    public static final double WALK_BPS = 2.5902;
    private static final Map<UUID, Walkers.Walk> WALKS = new LinkedHashMap<>();
    private static final Map<UUID, Supplier<Vec3>> LOOKS = new HashMap<>();
    private static final double ARRIVE = 0.08;
    private static final double NEAR = 2.0;
    public static final double FAST_WALK = 1.6;
    public static final double RUN = 2.0;

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(Walkers::onTick);
    }

    private Walkers() {
    }

    public static void lookAt(Entity e, Supplier<Vec3> at) {
        if (e != null) {
            if (at == null) {
                LOOKS.remove(e.getUUID());
            } else {
                LOOKS.put(e.getUUID(), at);
            }
        }
    }

    public static void start(Entity e, String name, PathDef d, double speed, String mode, int from) {
        if (e != null && !d.points.isEmpty()) {
            startWalk(e, name, d, speed, mode, from, false);
            Behaviours.walking(e);
        }
    }

    public static void go(Entity e, Vec3 to, double speed, boolean straight) {
        PathDef d = new PathDef();
        d.points.add(new PathDef.Point(to.x, to.y, to.z));
        d.speed = speed;
        startWalk(e, "", d, speed, "once", 0, straight);
    }

    public static void fly(Entity e, Vec3 to, double speed) {
        go(e, to, speed, true);
        Behaviours.walking(e);
    }

    private static void startWalk(Entity e, String name, PathDef d, double speed, String mode, int from, boolean straight) {
        stop(e);
        if (from < 0) {
            double best = Double.MAX_VALUE;

            for (int i = 0; i < d.points.size(); i++) {
                double dist = d.points.get(i).vec().distanceToSqr(e.position());
                if (dist < best) {
                    best = dist;
                    from = i;
                }
            }
        }

        Walkers.Walk w = new Walkers.Walk(e, name, d, mode != null && !mode.isEmpty() ? mode : d.mode, speed > 0.0 ? speed : d.speed, from);
        Mc.setMaxUpStep(e, Math.max(w.oldStep, 1.05F));
        w.straight = straight;
        WALKS.put(e.getUUID(), w);
    }

    public static void stop(Entity e) {
        if (e != null) {
            Walkers.Walk w = WALKS.remove(e.getUUID());
            if (w != null) {
                end(w);
            }
        }
    }

    private static void end(Walkers.Walk w) {
        Mc.setMaxUpStep(w.e, w.oldStep);
        Vec3 v = w.e.getDeltaMovement();
        w.e.setDeltaMovement(0.0, v.y, 0.0);
        if (w.e instanceof Mob m) {
            m.getNavigation().stop();
        }
    }

    public static boolean walking(Entity e) {
        return e != null && WALKS.containsKey(e.getUUID());
    }

    public static double speedOf(Entity e) {
        Walkers.Walk w = e == null ? null : WALKS.get(e.getUUID());
        if (w != null && !w.waiting) {
            PathDef.Point p = w.d.points.get(w.index);
            return p.speed > 0.0 ? p.speed : w.speed;
        } else {
            return -1.0;
        }
    }

    public static int point(Entity e) {
        Walkers.Walk w = e == null ? null : WALKS.get(e.getUUID());
        return w == null ? -1 : w.index;
    }

    public static void unload() {
        WALKS.clear();
        LOOKS.clear();
    }

    public static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END && !WALKS.isEmpty()) {
            for (Walkers.Walk w : new ArrayList<>(WALKS.values())) {
                if (!w.e.isRemoved() && w.e.isAlive()) {
                    try {
                        step(w);
                    } catch (RuntimeException var4) {
                        SprauteEngine.LOGGER.warn("Spraute: path walk", (Throwable)var4);
                        WALKS.remove(w.e.getUUID());
                        end(w);
                    }
                } else {
                    WALKS.remove(w.e.getUUID());
                }
            }
        }
    }

    private static boolean stopsAt(Walkers.Walk w, int i) {
        if (w.d.points.get(i).wait > 0.0) {
            return true;
        } else {
            int n = w.d.points.size();
            return w.mode.equals("once") && i == n - 1 || w.mode.equals("pingpong") && (i == n - 1 && w.dir > 0 || i == 0 && w.dir < 0);
        }
    }

    private static void step(Walkers.Walk w) {
        Entity e = w.e;
        PathDef.Point p = w.d.points.get(w.index);
        if (w.waiting) {
            float yaw = p.yaw != null ? p.yaw.floatValue() : w.facing;
            if (!Float.isNaN(yaw)) {
                face(e, yaw);
            } else if (LOOKS.containsKey(e.getUUID())) {
                face(e, e.getYRot());
            }

            if (--w.waitLeft > 0) {
                return;
            }

            w.waiting = false;
            if (!p.anim.isBlank()) {
                Fights.play(e, "", false, false);
            }

            if (!next(w)) {
                return;
            }

            p = w.d.points.get(w.index);
        }

        Vec3 to = p.vec();
        if (--w.chunks <= 0 && Mc.level(e) instanceof ServerLevel sl) {
            w.chunks = 40;
            NpcChunks.around(sl, e.chunkPosition());
            NpcChunks.along(sl, e.position(), to);
        }

        double dx = to.x - e.getX();
        double dz = to.z - e.getZ();
        double dy = Math.abs(e.getY() - to.y);
        double flat = Math.sqrt(dx * dx + dz * dz);
        boolean stops = stopsAt(w, w.index);
        double reach = stops ? 0.08 : 1.3;
        boolean walksOnGround = e instanceof Mob m0 && !m0.isNoAi() && !w.straight;
        double reachY = !walksOnGround && stops ? 0.08 : 1.6;
        boolean noWayFurther = w.route != null && w.node >= w.route.getNodeCount() && w.repath < 35 && !w.route.canReach();
        double sp = p.speed > 0.0 ? p.speed : w.speed;
        double want = sp * 2.5902 / 20.0;
        if (flat < w.closest - 0.05) {
            w.closest = flat;
            w.noCloser = 0;
        } else {
            w.noCloser++;
        }

        boolean blockedNear = flat <= 2.0 && dy <= 2.5 && w.noCloser > Math.max(15, (int)(0.1 / Math.max(0.001, want)));
        if ((!(flat <= reach) || !(dy <= reachY)) && (!noWayFurther || !(flat <= 2.0) || !(dy <= 2.5)) && !blockedNear) {
            if (e instanceof Mob m && !m.isNoAi() && !w.straight) {
                PathNavigation nav = m.getNavigation();
                if (w.route == null || --w.repath <= 0) {
                    w.route = nav.createPath(to.x, to.y, to.z, 0);
                    w.node = 1;
                    w.repath = w.route != null && w.route.canReach() ? 40 : 10;
                }

                if (!nav.isDone()) {
                    nav.stop();
                }

                m.setZza(0.0F);
                m.setXxa(0.0F);
                Vec3 aim = to;
                if (w.route != null) {
                    while (w.node < w.route.getNodeCount()) {
                        Node nd = w.route.getNode(w.node);
                        double nx = nd.x + 0.5 - e.getX();
                        double nz = nd.z + 0.5 - e.getZ();
                        if (!(nx * nx + nz * nz < 0.09) || !(Math.abs(nd.y - e.getY()) < 1.2)) {
                            break;
                        }

                        w.node++;
                    }

                    if (w.node < w.route.getNodeCount() - 1) {
                        Node nd = w.route.getNode(w.node);
                        aim = new Vec3(nd.x + 0.5, nd.y, nd.z + 0.5);
                    }
                }

                double ax = aim.x - e.getX();
                double az = aim.z - e.getZ();
                double len = Math.sqrt(ax * ax + az * az);
                double stepLen = Math.min(want, len);
                Vec3 v = e.getDeltaMovement();
                if (len > 1.0E-4) {
                    e.setDeltaMovement(ax / len * stepLen, v.y, az / len * stepLen);
                } else {
                    e.setDeltaMovement(0.0, v.y, 0.0);
                }

                float yawx = len > 1.0E-4 ? (float)Math.toDegrees(Math.atan2(-ax, az)) : w.facing;
                if (Float.isNaN(yawx)) {
                    yawx = e.getYRot();
                }

                w.facing = yawx;
                float body = Mth.approachDegrees(e instanceof LivingEntity le ? le.yBodyRot : e.getYRot(), yawx, 30.0F);
                e.setYRot(body);
                turn(e, body);
            } else {
                float yawx = (float)Math.toDegrees(Math.atan2(-dx, dz));
                Vec3 d = to.subtract(e.position());
                double lenx = d.length();
                double stepLenx = Math.min(lenx, want);
                Vec3 n = e.position().add(d.scale(stepLenx / Math.max(1.0E-6, lenx)));
                e.moveTo(n.x, n.y, n.z, yawx, e.getXRot());
                w.facing = yawx;
                face(e, yawx);
            }

            if (w.last == null || w.last.distanceToSqr(e.position()) > 0.04) {
                w.last = e.position();
                w.stuck = 0;
            } else if (++w.stuck > 80) {
                e.teleportTo(to.x, to.y, to.z);
                w.stuck = 0;
            }
        } else {
            if (flat > 0.5) {
                w.facing = (float)Math.toDegrees(Math.atan2(-dx, dz));
            }

            arrived(w, p, stops);
        }
    }

    private static void turn(Entity e, float yaw) {
        float head = yaw;
        Supplier<Vec3> look = LOOKS.get(e.getUUID());
        Vec3 at = null;
        if (look != null) {
            try {
                at = look.get();
            } catch (RuntimeException var12) {
            }
        }

        if (at != null) {
            double dx = at.x - e.getX();
            double dy = at.y - e.getEyeY();
            double dz = at.z - e.getZ();
            float wantYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            head = yaw + Math.max(-75.0F, Math.min(75.0F, Mth.wrapDegrees(wantYaw - yaw)));
            e.setXRot((float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)))));
        } else {
            e.setXRot(0.0F);
        }

        e.setYHeadRot(head);
        if (e instanceof LivingEntity le) {
            le.setYBodyRot(yaw);
        }

        if (e instanceof Mob m) {
            double hr = Math.toRadians(head);
            m.getLookControl().setLookAt(e.getX() - Math.sin(hr) * 4.0, at != null ? at.y : e.getEyeY(), e.getZ() + Math.cos(hr) * 4.0, 360.0F, 90.0F);
        }
    }

    private static void face(Entity e, float yaw) {
        float head = yaw;
        float pitch = 0.0F;
        Supplier<Vec3> look = LOOKS.get(e.getUUID());
        Vec3 at = null;
        if (look != null) {
            try {
                at = look.get();
            } catch (RuntimeException var14) {
            }
        }

        if (at != null) {
            double dx = at.x - e.getX();
            double dy = at.y - e.getEyeY();
            double dz = at.z - e.getZ();
            float want = (float)Math.toDegrees(Math.atan2(-dx, dz));
            float diff = Mth.wrapDegrees(want - yaw);
            head = yaw + Math.max(-75.0F, Math.min(75.0F, diff));
            pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        }

        e.setYRot(yaw);
        e.setXRot(pitch);
        e.setYHeadRot(head);
        if (e instanceof LivingEntity le) {
            le.setYBodyRot(yaw);
        }

        if (e instanceof Mob m) {
            double hr = Math.toRadians(head);
            m.getLookControl().setLookAt(e.getX() - Math.sin(hr) * 4.0, at != null ? at.y : e.getEyeY(), e.getZ() + Math.cos(hr) * 4.0, 360.0F, 90.0F);
        }
    }

    private static void arrived(Walkers.Walk w, PathDef.Point p, boolean stops) {
        Entity e = w.e;
        w.stuck = 0;
        w.last = null;
        w.closest = Double.MAX_VALUE;
        w.noCloser = 0;
        if (stops) {
            Vec3 v = e.getDeltaMovement();
            e.setDeltaMovement(0.0, v.y, 0.0);
        }

        if (p.wait > 0.0) {
            w.waiting = true;
            w.waitLeft = (int)Math.round(p.wait * 20.0);
            if (!p.anim.isBlank()) {
                Fights.play(e, p.anim, true, false);
            }
        } else {
            next(w);
        }
    }

    private static boolean next(Walkers.Walk w) {
        int n = w.d.points.size();
        int i = w.index + w.dir;
        if (i >= n || i < 0) {
            String var3 = w.mode;
            switch (var3) {
                case "loop":
                    i = 0;
                    break;
                case "pingpong":
                    w.dir = -w.dir;
                    i = n == 1 ? 0 : w.index + w.dir;
                    break;
                default:
                    finish(w);
                    return false;
            }
        }

        w.index = Math.max(0, Math.min(n - 1, i));
        w.route = null;
        w.closest = Double.MAX_VALUE;
        w.noCloser = 0;
        return true;
    }

    private static void finish(Walkers.Walk w) {
        WALKS.remove(w.e.getUUID());
        end(w);
        if (!Float.isNaN(w.facing)) {
            face(w.e, w.facing);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("path", w.name);
        data.put("entity", Regs.ENTITY_TYPE.getKey(w.e.getType()).toString());
        data.put("name", w.e.getName().getString());
        data.put("__entity", w.e);
        data.put("x", Math.floor(w.e.getX()));
        data.put("y", Math.floor(w.e.getY()));
        data.put("z", Math.floor(w.e.getZ()));
        if (!w.name.isEmpty()) {
            GameEvents.post("pathend", null, data);
        }
    }

    private static final class Walk {
        final Entity e;
        final String name;
        final PathDef d;
        final String mode;
        final double speed;
        final float oldStep;
        int index;
        int dir = 1;
        int waitLeft;
        int stuck;
        int repath;
        int chunks;
        boolean waiting;
        boolean straight;
        Vec3 last;
        double closest = Double.MAX_VALUE;
        int noCloser;
        float facing = Float.NaN;
        Path route;
        int node;

        Walk(Entity e, String name, PathDef d, String mode, double speed, int from) {
            this.e = e;
            this.name = name;
            this.d = d;
            this.mode = mode;
            this.speed = speed;
            this.index = Math.max(0, Math.min(from, d.points.size() - 1));
            this.oldStep = Mc.maxUpStep(e);
        }
    }
}
