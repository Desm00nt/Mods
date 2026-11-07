package org.sprautestudio.spraute_engine.runtime.models;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public record PartShape(Vector3f center, Quaternionf rot, Vector3f half) {
    public static PartShape of(EntityLibrary.Def.Part p, float scale, float bodyYaw, PartShape.BonePose bone) {
        Quaternionf base = bone != null ? new Quaternionf(bone.rot()) : new Quaternionf().rotationY((float)(-Math.toRadians(bodyYaw)));
        Vector3f origin = bone != null ? new Vector3f(bone.pos()) : new Vector3f();
        Vector3f anchor = base.transform(new Vector3f(p.x, p.y, p.z).mul(scale)).add(origin);
        Quaternionf rot = new Quaternionf(base).rotateY((float)Math.toRadians(p.ry)).rotateX((float)Math.toRadians(p.rx)).rotateZ((float)Math.toRadians(p.rz));
        float depth = p.depth > 0.0F ? p.depth : p.width;
        Vector3f half = new Vector3f(p.width / 2.0F, p.height / 2.0F, depth / 2.0F).mul(scale);
        Vector3f center = rot.transform(new Vector3f(0.0F, half.y, 0.0F)).add(anchor);
        return new PartShape(center, rot, half);
    }

    public Vector3f[] corners() {
        Vector3f[] out = new Vector3f[8];
        int i = 0;

        for (int dy = -1; dy <= 1; dy += 2) {
            for (int dx = -1; dx <= 1; dx += 2) {
                for (int dz = -1; dz <= 1; dz += 2) {
                    out[i++] = this.rot.transform(new Vector3f(dx * this.half.x, dy * this.half.y, dz * this.half.z)).add(this.center);
                }
            }
        }

        return out;
    }

    public AABB bounds(double x, double y, double z) {
        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float z0 = Float.MAX_VALUE;
        float x1 = -Float.MAX_VALUE;
        float y1 = -Float.MAX_VALUE;
        float z1 = -Float.MAX_VALUE;

        for (Vector3f c : this.corners()) {
            x0 = Math.min(x0, c.x);
            y0 = Math.min(y0, c.y);
            z0 = Math.min(z0, c.z);
            x1 = Math.max(x1, c.x);
            y1 = Math.max(y1, c.y);
            z1 = Math.max(z1, c.z);
        }

        return new AABB(x + x0, y + y0, z + z0, x + x1, y + y1, z + z1);
    }

    public double hit(Vec3 from, Vec3 to) {
        Quaternionf inv = new Quaternionf(this.rot).conjugate();
        Vector3f a = inv.transform(new Vector3f((float)from.x, (float)from.y, (float)from.z).sub(this.center));
        Vector3f b = inv.transform(new Vector3f((float)to.x, (float)to.y, (float)to.z).sub(this.center));
        float[] o = new float[]{a.x, a.y, a.z};
        float[] d = new float[]{b.x - a.x, b.y - a.y, b.z - a.z};
        float[] h = new float[]{this.half.x, this.half.y, this.half.z};
        double t0 = 0.0;
        double t1 = 1.0;

        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0E-7) {
                if (o[i] < -h[i] || o[i] > h[i]) {
                    return -1.0;
                }
            } else {
                double ta = (-h[i] - o[i]) / d[i];
                double tb = (h[i] - o[i]) / d[i];
                if (ta > tb) {
                    double t = ta;
                    ta = tb;
                    tb = t;
                }

                t0 = Math.max(t0, ta);
                t1 = Math.min(t1, tb);
                if (t0 > t1) {
                    return -1.0;
                }
            }
        }

        return t0;
    }

    public record BonePose(Vector3f pos, Quaternionf rot, long at) {
    }
}
