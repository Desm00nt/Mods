package org.sprautestudio.spraute_engine.runtime.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.math.SpMatrix4;
import org.sprautestudio.spraute_engine.core.model.BoneRay;
import org.sprautestudio.spraute_engine.core.model.SpBone;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.core.model.SpModelInstance;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class BoneWorld {
    public static final long WATCH_MS = 3000L;
    private static final long ASK_EVERY_MS = 1000L;
    private static final double ASK_WITHIN = 96.0;
    private static final float PIXELS = 16.0F;
    private static final Map<SpGeoModel, SpModelInstance> REST = Collections.synchronizedMap(new WeakHashMap<>());

    private BoneWorld() {
    }

    private static SprauteModelEntity body(Entity e) {
        if (e instanceof SprauteModelEntity me && geometry(me) != null) {
            return me;
        } else {
            throw new SprauteRuntimeException(
                "У " + (e == null ? "этой цели" : e.getName().getString()) + " нет модели с костями: кости есть только у NPC с моделью"
            );
        }
    }

    private static SpGeoModel geometry(SprauteModelEntity me) {
        EntityLibrary.Look l = me.look();
        return l != null ? l.geometry() : null;
    }

    private static float scale(SprauteModelEntity me) {
        EntityLibrary.Look l = me.look();
        return l.scale() * me.bodyScale();
    }

    private static void watch(SprauteModelEntity me) {
        long now = System.currentTimeMillis();
        if (Mc.level(me) instanceof ServerLevel level) {
            if (now - me.boneAskedAt >= 1000L) {
                me.boneAskedAt = now;
                Packets.BoneWatch ask = new Packets.BoneWatch(me.getId());

                for (ServerPlayer p : level.players()) {
                    if (p.distanceToSqr(me) < 9216.0) {
                        SprauteNetwork.sendTo(p, ask);
                    }
                }
            }
        } else {
            me.boneWatchUntil = now + 3000L;
        }
    }

    private static SpModelInstance rest(SpGeoModel geo) {
        return REST.computeIfAbsent(geo, g -> {
            SpModelInstance inst = new SpModelInstance(g);
            inst.computeTransforms();
            return inst;
        });
    }

    private static PartShape.BonePose restPose(SprauteModelEntity me, SpGeoModel geo, String bone) {
        SpMatrix4 b = rest(geo).getBoneMatrix(bone);
        if (b == null) {
            return null;
        } else {
            float[] m = b.m;
            EntityLibrary.Look l = me.look();
            float lift = l.def() != null ? l.def().modelY : 0.0F;
            float size = scale(me);
            Matrix4f body = new Matrix4f().translate(0.0F, lift, 0.0F).rotateY((float)Math.toRadians(180.0F - me.yBodyRot)).scale(size);
            body.translate(-m[12] / 16.0F, m[13] / 16.0F, m[14] / 16.0F);
            body.rotate(new Quaternionf().setFromNormalized(new Matrix3f(m[0], -m[1], -m[2], -m[4], m[5], m[6], -m[8], m[9], m[10])));
            return new PartShape.BonePose(body.transformPosition(new Vector3f()), body.getNormalizedRotation(new Quaternionf()), System.currentTimeMillis());
        }
    }

    private static BoneRay.Pose pose(SprauteModelEntity me, SpGeoModel geo, String bone) {
        PartShape.BonePose p = me.freshPose(bone);
        if (p == null) {
            p = restPose(me, geo, bone);
        }

        return p == null ? null : new BoneRay.Pose(p.pos().x, p.pos().y, p.pos().z, p.rot().x, p.rot().y, p.rot().z, p.rot().w);
    }

    private static Map<String, BoneRay.Pose> poses(SprauteModelEntity me, SpGeoModel geo) {
        Map<String, BoneRay.Pose> out = new HashMap<>();

        for (String name : geo.boneMap.keySet()) {
            BoneRay.Pose p = pose(me, geo, name);
            if (p != null) {
                out.put(name, p);
            }
        }

        return out;
    }

    public static Vec3 position(Entity e, String name, Vec3 base, double dx, double dy, double dz) {
        SprauteModelEntity me = body(e);
        SpGeoModel geo = geometry(me);
        watch(me);
        SpBone bone = geo.boneMap.get(name);
        if (bone == null) {
            SpGeoModel.LocatorRef ref = geo.findLocator(name);
            if (ref == null) {
                throw new SprauteRuntimeException("У модели нет кости «" + name + "». Есть: " + String.join(", ", geo.boneMap.keySet()));
            }

            bone = ref.bone();
            dx += ref.locator().offset.x - bone.pivot.x;
            dy += ref.locator().offset.y - bone.pivot.y;
            dz += ref.locator().offset.z - bone.pivot.z;
        }

        BoneRay.Pose pose = pose(me, geo, bone.name);
        if (pose == null) {
            return base;
        } else {
            double[] p = BoneRay.point(pose, scale(me), dx, dy, dz);
            return base.add(p[0], p[1], p[2]);
        }
    }

    public static Vec3 position(Entity e, String name, double dx, double dy, double dz) {
        return position(e, name, e.position(), dx, dy, dz);
    }

    public static Vec3 forward(Entity e, String name) {
        SprauteModelEntity me = body(e);
        SpGeoModel geo = geometry(me);
        watch(me);
        if (!geo.boneMap.containsKey(name)) {
            throw new SprauteRuntimeException("У модели нет кости «" + name + "». Есть: " + String.join(", ", geo.boneMap.keySet()));
        } else {
            double[] f = BoneRay.forward(pose(me, geo, name));
            return new Vec3(f[0], f[1], f[2]);
        }
    }

    public static List<String> names(Entity e) {
        return new ArrayList<>(geometry(body(e)).boneMap.keySet());
    }

    public static String onRay(Entity e, Vec3 from, Vec3 to) {
        if (e instanceof SprauteModelEntity me) {
            SpGeoModel geo = geometry(me);
            if (geo == null) {
                return null;
            } else {
                watch(me);
                Vec3 at = e.position();
                double[] a = new double[]{from.x - at.x, from.y - at.y, from.z - at.z};
                double[] b = new double[]{to.x - at.x, to.y - at.y, to.z - at.z};
                Map<String, BoneRay.Pose> poses = poses(me, geo);
                String hit = BoneRay.hit(geo, poses, scale(me), a, b);
                return hit != null ? hit : BoneRay.nearest(geo, poses, a, b);
            }
        } else {
            return null;
        }
    }
}
