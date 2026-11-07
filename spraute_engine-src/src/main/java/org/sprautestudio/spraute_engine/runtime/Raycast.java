package org.sprautestudio.spraute_engine.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.runtime.models.BoneWorld;

public final class Raycast {
    private Raycast() {
    }

    private static Entity entity(Object v) {
        return v instanceof EntityRef r ? r.require() : null;
    }

    private static Vec3 eyes(Object v) {
        Entity e = entity(v);
        return e != null ? e.getEyePosition() : WorldApi.vec(v);
    }

    private static Vec3 aimAt(Object v) {
        Entity e = entity(v);
        return e != null ? e.getBoundingBox().getCenter() : WorldApi.vec(v);
    }

    public static Raycast.Hit trace(Vec3 from, Vec3 to, Entity ignore, Entity ignore2, boolean entities) {
        ServerLevel level = ignore != null && Mc.level(ignore) instanceof ServerLevel sl ? sl : WorldApi.level();
        Entity ctx = (Entity)(ignore != null ? ignore : SprauteRuntime.player());
        BlockHitResult bh = ctx == null ? null : level.clip(new ClipContext(from, to, Block.OUTLINE, Fluid.NONE, ctx));
        Vec3 end = bh != null && bh.getType() == Type.BLOCK ? bh.getLocation() : to;
        Raycast.Hit best = null;
        if (entities) {
            double bestD = from.distanceTo(end);
            AABB box = new AABB(from, end).inflate(1.0);

            for (Entity e : level.getEntities(
                (Entity)null, box, ex -> ex != ignore && ex != ignore2 && !ex.isSpectator() && ex.isAlive() && (ex.isPickable() || ex instanceof LivingEntity)
            )) {
                AABB bb = e.getBoundingBox().inflate(Math.max(0.1, (double)e.getPickRadius()));
                Optional<Vec3> p = bb.contains(from) ? Optional.of(from) : bb.clip(from, end);
                if (!p.isEmpty()) {
                    double d = from.distanceTo(p.get());
                    if (d < bestD) {
                        bestD = d;
                        best = new Raycast.Hit("entity", p.get(), d, null, "", e);
                    }
                }
            }
        }

        if (best != null) {
            return best;
        } else {
            return bh != null && bh.getType() == Type.BLOCK
                ? new Raycast.Hit("block", bh.getLocation(), from.distanceTo(bh.getLocation()), bh.getBlockPos(), bh.getDirection().getName(), null)
                : new Raycast.Hit("none", to, from.distanceTo(to), null, "", null);
        }
    }

    public static Raycast.Hit look(Object who, double dist) {
        Entity e = entity(who);
        if (e == null) {
            throw new SprauteRuntimeException("Смотреть может только сущность: игрок или персонаж");
        } else {
            Vec3 from = e.getEyePosition();
            return trace(from, from.add(e.getViewVector(1.0F).scale(dist)), e, null, true);
        }
    }

    public static Object raycast(Object a, Object b, double dist) {
        Raycast.Hit h;
        Vec3 from;
        Vec3 to;
        if (b != null && !(b instanceof Double)) {
            from = eyes(a);
            to = aimAt(b);
            h = trace(from, to, entity(a), entity(b), true);
        } else {
            double far = b instanceof Double d ? d : dist;
            Entity e = entity(a);
            h = look(a, far);
            from = e.getEyePosition();
            to = from.add(e.getViewVector(1.0F).scale(far));
        }

        Map<String, Object> m = map(h);
        String bone = h.entity() != null ? BoneWorld.onRay(h.entity(), from, to) : null;
        m.put("bone", bone != null ? bone : "");
        m.put("bonepos", bone != null ? WorldApi.list(BoneWorld.position(h.entity(), bone, 0.0, 0.0, 0.0)) : "");
        return m;
    }

    public static Object lookBone(Object who, double dist) {
        Raycast.Hit h = look(who, dist);
        if (h.entity() == null) {
            return "";
        } else {
            Entity e = entity(who);
            Vec3 from = e.getEyePosition();
            String bone = BoneWorld.onRay(h.entity(), from, from.add(e.getViewVector(1.0F).scale(dist)));
            return bone != null ? bone : "";
        }
    }

    public static Object lookBlock(Object who, double dist) {
        Raycast.Hit h = look(who, dist);
        Entity e = entity(who);
        if (h.kind().equals("entity")) {
            Vec3 from = e.getEyePosition();
            h = trace(from, from.add(e.getViewVector(1.0F).scale(dist)), e, null, false);
        }

        return h.block() == null ? "" : WorldApi.list(Vec3.atLowerCornerOf(h.block()));
    }

    public static Object lookEntity(Object who, double dist) {
        Raycast.Hit h = look(who, dist);
        return h.entity() == null ? "" : new EntityRef(h.entity());
    }

    public static boolean canSee(Object a, Object b) {
        Raycast.Hit h = trace(eyes(a), aimAt(b), entity(a), entity(b), false);
        return !h.kind().equals("block");
    }

    public static boolean looksAt(Object who, Object target, double dist) {
        Entity t = entity(target);
        if (t != null) {
            Raycast.Hit h = look(who, dist);
            return h.entity() == t;
        } else {
            Entity e = entity(who);
            if (e == null) {
                return false;
            } else {
                Vec3 p = WorldApi.vec(target);
                Vec3 from = e.getEyePosition();
                Vec3 dir = e.getViewVector(1.0F);
                Vec3 to = p.subtract(from);
                double along = to.dot(dir);
                if (!(along <= 0.0) && !(along > dist)) {
                    double off = to.subtract(dir.scale(along)).length();
                    return off <= Math.max(1.5, along * 0.05) && canSee(new EntityRef(e), target);
                } else {
                    return false;
                }
            }
        }
    }

    private static Map<String, Object> map(Raycast.Hit h) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hit", h.kind());
        m.put("pos", WorldApi.list(h.at()));
        m.put("dist", Math.round(h.dist() * 100.0) / 100.0);
        if (h.block() != null) {
            m.put("block", String.valueOf(Regs.BLOCK.getKey(WorldApi.level().getBlockState(h.block()).getBlock())));
            m.put("blockpos", WorldApi.list(Vec3.atLowerCornerOf(h.block())));
            m.put("face", h.face());
        } else {
            m.put("block", "");
            m.put("blockpos", "");
            m.put("face", "");
        }

        m.put("entity", h.entity() != null ? new EntityRef(h.entity()) : "");
        return m;
    }

    public record Hit(String kind, Vec3 at, double dist, BlockPos block, String face, Entity entity) {
    }
}
