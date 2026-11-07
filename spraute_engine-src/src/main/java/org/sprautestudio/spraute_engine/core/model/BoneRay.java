package org.sprautestudio.spraute_engine.core.model;

import java.util.Map;

public final class BoneRay {
    private static final double PIXELS = 16.0;
    private static final double NEAR_ZERO = 1.0E-9;

    private BoneRay() {
    }

    private static double[] rotate(double qx, double qy, double qz, double qw, double vx, double vy, double vz) {
        double cx = qy * vz - qz * vy + qw * vx;
        double cy = qz * vx - qx * vz + qw * vy;
        double cz = qx * vy - qy * vx + qw * vz;
        return new double[]{vx + 2.0 * (qy * cz - qz * cy), vy + 2.0 * (qz * cx - qx * cz), vz + 2.0 * (qx * cy - qy * cx)};
    }

    public static double[] point(BoneRay.Pose pose, float scale, double dx, double dy, double dz) {
        double[] v = pose.turn(-dx / 16.0 * scale, dy / 16.0 * scale, dz / 16.0 * scale);
        return new double[]{pose.x + v[0], pose.y + v[1], pose.z + v[2]};
    }

    public static double[] forward(BoneRay.Pose pose) {
        return pose.turn(0.0, 0.0, -1.0);
    }

    public static String hit(SpGeoModel model, Map<String, BoneRay.Pose> poses, float scale, double[] from, double[] to) {
        String best = null;
        double bestAt = Double.MAX_VALUE;

        for (SpBone bone : model.boneMap.values()) {
            BoneRay.Pose pose = poses.get(bone.name);
            if (pose != null && !bone.cubes.isEmpty()) {
                double[] a = toModel(bone, pose, scale, from);
                double[] b = toModel(bone, pose, scale, to);

                for (SpCube cube : bone.cubes) {
                    double at = slabs(cube, a, b);
                    if (at >= 0.0 && at < bestAt) {
                        bestAt = at;
                        best = bone.name;
                    }
                }
            }
        }

        return best;
    }

    public static String nearest(SpGeoModel model, Map<String, BoneRay.Pose> poses, double[] from, double[] to) {
        String best = null;
        double bestDist = Double.MAX_VALUE;
        double dx = to[0] - from[0];
        double dy = to[1] - from[1];
        double dz = to[2] - from[2];
        double len = dx * dx + dy * dy + dz * dz;

        for (SpBone bone : model.boneMap.values()) {
            BoneRay.Pose p = poses.get(bone.name);
            if (p != null) {
                double px = p.x - from[0];
                double py = p.y - from[1];
                double pz = p.z - from[2];
                double t = len < 1.0E-9 ? 0.0 : Math.max(0.0, Math.min(1.0, (px * dx + py * dy + pz * dz) / len));
                double ox = px - dx * t;
                double oy = py - dy * t;
                double oz = pz - dz * t;
                double d = ox * ox + oy * oy + oz * oz;
                if (d < bestDist) {
                    bestDist = d;
                    best = bone.name;
                }
            }
        }

        return best;
    }

    private static double[] toModel(SpBone bone, BoneRay.Pose pose, float scale, double[] at) {
        double[] v = pose.unturn(at[0] - pose.x, at[1] - pose.y, at[2] - pose.z);
        double k = 16.0 / Math.max(0.001, (double)scale);
        return new double[]{-v[0] * k + bone.pivot.x, v[1] * k + bone.pivot.y, v[2] * k + bone.pivot.z};
    }

    private static double slabs(SpCube cube, double[] a, double[] b) {
        double enter = 0.0;
        double leave = 1.0;
        double[] min = new double[]{cube.origin.x - cube.inflate, cube.origin.y - cube.inflate, cube.origin.z - cube.inflate};
        double[] max = new double[]{
            cube.origin.x + cube.size.x + cube.inflate, cube.origin.y + cube.size.y + cube.inflate, cube.origin.z + cube.size.z + cube.inflate
        };

        for (int i = 0; i < 3; i++) {
            double d = b[i] - a[i];
            if (Math.abs(d) < 1.0E-9) {
                if (a[i] < min[i] || a[i] > max[i]) {
                    return -1.0;
                }
            } else {
                double t0 = (min[i] - a[i]) / d;
                double t1 = (max[i] - a[i]) / d;
                enter = Math.max(enter, Math.min(t0, t1));
                leave = Math.min(leave, Math.max(t0, t1));
                if (enter > leave) {
                    return -1.0;
                }
            }
        }

        return enter;
    }

    public record Pose(float x, float y, float z, float qx, float qy, float qz, float qw) {
        double[] turn(double vx, double vy, double vz) {
            return BoneRay.rotate((double)this.qx, (double)this.qy, (double)this.qz, (double)this.qw, vx, vy, vz);
        }

        double[] unturn(double vx, double vy, double vz) {
            return BoneRay.rotate((double)(-this.qx), (double)(-this.qy), (double)(-this.qz), (double)this.qw, vx, vy, vz);
        }
    }
}
