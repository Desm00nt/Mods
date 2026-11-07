package org.sprautestudio.spraute_engine.core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import org.sprautestudio.spraute_engine.core.math.SpMatrix4;
import org.sprautestudio.spraute_engine.core.math.SpVec3;

public final class SpJavaModel {
    public static final int BOX_MIN = -48;
    public static final int BOX_MAX = 64;
    public static final float MIN = -16.0F;
    public static final float MAX = 32.0F;
    private static final float[] ANGLES = new float[]{-45.0F, -22.5F, 0.0F, 22.5F, 45.0F};
    private static final String[] DIRS = new String[]{"west", "east", "down", "up", "north", "south"};

    private SpJavaModel() {
    }

    public static SpJavaModel.Result convert(SpGeoModel model, float scale, int turn, float[] offset, boolean fit) {
        List<SpJavaModel.Raw> raws = new ArrayList<>();
        SpModelInstance bind = new SpModelInstance(model);
        bind.computeTransforms();
        int q = Math.floorMod(turn, 4);

        for (SpBone bone : model.boneMap.values()) {
            SpMatrix4 m = bind.getBoneMatrix(bone.name);

            for (SpCube cube : bone.cubes) {
                raws.add(raw(model, bone, cube, m, q));
            }
        }

        float s = scale <= 0.0F ? 1.0F : scale;
        float fitScale = 1.0F;
        if (fit) {
            float need = 1.0F;

            for (SpJavaModel.Raw r : raws) {
                for (float[] p : r.corners) {
                    float[] t = place(p, s, offset);

                    for (int a = 0; a < 3; a++) {
                        float c = a == 1 ? 0.0F : 8.0F;
                        float o = offset[a] + c;
                        float d = t[a] - o;
                        if (t[a] > 32.0F && d > 0.0F) {
                            need = Math.max(need, d / (32.0F - o));
                        }

                        if (t[a] < -16.0F && d < 0.0F) {
                            need = Math.max(need, d / (-16.0F - o));
                        }
                    }
                }
            }

            fitScale = 1.0F / need;
        }

        float total = s * fitScale;
        List<SpJavaModel.Element> out = new ArrayList<>();
        int approx = 0;

        for (SpJavaModel.Raw r : raws) {
            float[][] corners = new float[8][];

            for (int i = 0; i < 8; i++) {
                corners[i] = place(r.corners[i], total, offset);
            }

            List<float[][]> faces = new ArrayList<>();

            for (float[][] f : r.faces) {
                float[][] pf = new float[4][];

                for (int i = 0; i < 4; i++) {
                    pf[i] = place(f[i], total, offset);
                }

                faces.add(pf);
            }

            SpJavaModel.Built b = element(corners, faces, r.uvs);
            if (b != null) {
                if (!b.exact) {
                    approx++;
                }

                out.add(b.element);
            }
        }

        return new SpJavaModel.Result(out, approx, fitScale);
    }

    private static SpJavaModel.Raw raw(SpGeoModel model, SpBone bone, SpCube cube, SpMatrix4 bind, int turn) {
        float x0 = cube.origin.x - cube.inflate;
        float y0 = cube.origin.y - cube.inflate;
        float z0 = cube.origin.z - cube.inflate;
        float x1 = x0 + cube.size.x + cube.inflate * 2.0F;
        float y1 = y0 + cube.size.y + cube.inflate * 2.0F;
        float z1 = z0 + cube.size.z + cube.inflate * 2.0F;
        float h = 0.0125F;
        if (x1 - x0 < 0.025F) {
            float m = (x0 + x1) * 0.5F;
            x0 = m - h;
            x1 = m + h;
        }

        if (y1 - y0 < 0.025F) {
            float m = (y0 + y1) * 0.5F;
            y0 = m - h;
            y1 = m + h;
        }

        if (z1 - z0 < 0.025F) {
            float m = (z0 + z1) * 0.5F;
            z0 = m - h;
            z1 = m + h;
        }

        float[][] corners = new float[8][];

        for (int i = 0; i < 8; i++) {
            corners[i] = world(bone, cube, bind, (i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1, turn);
        }

        List<float[][]> faces = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();

        for (Entry<SpCube.SpFace, SpFaceUV> entry : cube.faceUVs.entrySet()) {
            SpFaceUV uv = entry.getValue();
            if (!(Math.abs(uv.uSize) < 0.001F) || !(Math.abs(uv.vSize) < 0.001F)) {
                SpCube.SpFace face = cube.geometryFace(entry.getKey(), cube.boxUv);
                float[] v = SpGeoMeshBaker.faceVertices(face, x0, y0, z0, x1, y1, z1);
                float[][] quad = new float[4][];

                for (int i = 0; i < 4; i++) {
                    quad[i] = world(bone, cube, bind, v[i * 3], v[i * 3 + 1], v[i * 3 + 2], turn);
                }

                faces.add(quad);
                uvs.add(SpGeoMeshBaker.faceUvs(uv, Math.max(1, model.textureWidth), Math.max(1, model.textureHeight), cube.mirror));
            }
        }

        return new SpJavaModel.Raw(corners, faces, uvs);
    }

    private static float[] world(SpBone bone, SpCube cube, SpMatrix4 bind, float x, float y, float z, int turn) {
        SpVec3 p = new SpVec3(x, y, z);
        cube.modelPointToBoneLocal(bone, p);
        bind.transformPoint(p);
        float wx = -p.x;
        float wz = p.z;

        for (int i = 0; i < turn; i++) {
            float t = wx;
            wx = -wz;
            wz = t;
        }

        return new float[]{wx, p.y, wz};
    }

    private static float[] place(float[] p, float scale, float[] offset) {
        return new float[]{p[0] * scale + 8.0F + offset[0], p[1] * scale + offset[1], p[2] * scale + 8.0F + offset[2]};
    }

    private static SpJavaModel.Built element(float[][] corners, List<float[][]> faces, List<float[]> uvs) {
        float[] c = new float[3];

        for (float[] p : corners) {
            for (int a = 0; a < 3; a++) {
                c[a] += p[a] / 8.0F;
            }
        }

        float[][] edges = new float[][]{sub(corners[1], corners[0]), sub(corners[2], corners[0]), sub(corners[4], corners[0])};
        String axis = "";
        float angle = 0.0F;
        boolean exact = true;
        if (!aligned(edges)) {
            axis = "?";

            for (int k = 0; k < 3 && axis.equals("?"); k++) {
                for (float[] e : edges) {
                    float len = len(e);
                    if (!(len < 1.0E-4) && !(Math.abs(e[k]) < len * 0.9995F)) {
                        float[] other = null;

                        for (float[] o : edges) {
                            if (o != e && len(o) > 1.0E-4 && (other == null || len(o) > len(other))) {
                                other = o;
                            }
                        }

                        if (other != null) {
                            float raw = switch (k) {
                                case 0 -> (float)Math.toDegrees(Math.atan2(other[2], other[1]));
                                case 1 -> (float)Math.toDegrees(Math.atan2(-other[2], other[0]));
                                default -> (float)Math.toDegrees(Math.atan2(other[1], other[0]));
                            };
                            float r = raw - 90 * Math.round(raw / 90.0F);
                            float snapped = nearest(r);
                            exact = Math.abs(snapped - r) < 0.5F;
                            axis = k == 0 ? "x" : (k == 1 ? "y" : "z");
                            angle = snapped;
                            break;
                        }
                    }
                }
            }

            if (axis.equals("?")) {
                exact = false;
            }
        }

        String ax = axis.equals("?") ? "" : axis;
        float[] bMin = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] bMax = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        float[] from = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] to = new float[]{-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};

        for (float[] p : corners) {
            float[] l = unturn(p, c, ax, angle);

            for (int a = 0; a < 3; a++) {
                from[a] = Math.min(from[a], l[a]);
                to[a] = Math.max(to[a], l[a]);
                bMin[a] = Math.min(bMin[a], p[a]);
                bMax[a] = Math.max(bMax[a], p[a]);
            }
        }

        List<SpJavaModel.Face> out = new ArrayList<>();

        for (int i = 0; i < faces.size(); i++) {
            float[][] local = new float[4][];

            for (int j = 0; j < 4; j++) {
                local[j] = unturn(faces.get(i)[j], c, ax, angle);
            }

            SpJavaModel.Face f = face(local, uvs.get(i), from, to);
            if (f != null) {
                out.add(f);
            }
        }

        if (out.isEmpty()) {
            return null;
        } else {
            for (int a = 0; a < 3; a++) {
                from[a] = clampTo(from[a]);
                to[a] = clampTo(to[a]);
            }

            return new SpJavaModel.Built(new SpJavaModel.Element(from, to, ax, ax.isEmpty() ? 0.0F : angle, c, out, bMin, bMax), exact);
        }
    }

    private static boolean aligned(float[][] edges) {
        for (float[] e : edges) {
            float len = len(e);
            if (!(len < 1.0E-4)) {
                boolean one = false;

                for (int a = 0; a < 3; a++) {
                    if (Math.abs(e[a]) >= len * 0.9995F) {
                        one = true;
                    }
                }

                if (!one) {
                    return false;
                }
            }
        }

        return true;
    }

    private static float nearest(float r) {
        float best = 0.0F;

        for (float a : ANGLES) {
            if (Math.abs(a - r) < Math.abs(best - r)) {
                best = a;
            }
        }

        return best;
    }

    private static float[] unturn(float[] p, float[] c, String axis, float angle) {
        if (!axis.isEmpty() && angle != 0.0F) {
            double t = Math.toRadians(-angle);
            double cs = Math.cos(t);
            double sn = Math.sin(t);
            float x = p[0] - c[0];
            float y = p[1] - c[1];
            float z = p[2] - c[2];
            float nx = x;
            float ny = y;
            float nz = z;
            switch (axis) {
                case "x":
                    ny = (float)(y * cs - z * sn);
                    nz = (float)(y * sn + z * cs);
                    break;
                case "y":
                    nx = (float)(x * cs + z * sn);
                    nz = (float)(-x * sn + z * cs);
                    break;
                default:
                    nx = (float)(x * cs - y * sn);
                    ny = (float)(x * sn + y * cs);
            }

            return new float[]{nx + c[0], ny + c[1], nz + c[2]};
        } else {
            return (float[])p.clone();
        }
    }

    private static SpJavaModel.Face face(float[][] quad, float[] uv, float[] from, float[] to) {
        int axis = -1;
        float best = Float.MAX_VALUE;

        for (int a = 0; a < 3; a++) {
            float lo = Float.MAX_VALUE;
            float hi = -Float.MAX_VALUE;

            for (float[] p : quad) {
                lo = Math.min(lo, p[a]);
                hi = Math.max(hi, p[a]);
            }

            if (hi - lo < best) {
                best = hi - lo;
                axis = a;
            }
        }

        float at = 0.0F;

        for (float[] p : quad) {
            at += p[axis] / 4.0F;
        }

        boolean positive = Math.abs(at - to[axis]) < Math.abs(at - from[axis]);
        String dir = DIRS[axis * 2 + (positive ? 1 : 0)];
        float[][] corners = faceInfo(dir, from, to);
        float[][] got = new float[4][];

        for (int i = 0; i < 4; i++) {
            int nearest = 0;
            float d = Float.MAX_VALUE;

            for (int j = 0; j < 4; j++) {
                float dd = dist2(corners[i], quad[j]);
                if (dd < d) {
                    d = dd;
                    nearest = j;
                }
            }

            got[i] = new float[]{uv[nearest * 2] * 16.0F, uv[nearest * 2 + 1] * 16.0F};
        }

        for (int k = 0; k < 4; k++) {
            float[] c0 = got[(4 - k) % 4];
            float[] c1 = got[(5 - k) % 4];
            float[] c2 = got[(6 - k) % 4];
            float[] c3 = got[(7 - k) % 4];
            if (near(c1[0], c0[0]) && near(c1[1], c2[1]) && near(c3[0], c2[0]) && near(c3[1], c0[1])) {
                return new SpJavaModel.Face(dir, c0[0], c0[1], c2[0], c2[1], k * 90);
            }
        }

        return new SpJavaModel.Face(dir, got[0][0], got[0][1], got[2][0], got[2][1], 0);
    }

    private static float[][] faceInfo(String dir, float[] f, float[] t) {
        return switch (dir) {
            case "down" -> new float[][]{{f[0], f[1], t[2]}, {f[0], f[1], f[2]}, {t[0], f[1], f[2]}, {t[0], f[1], t[2]}};
            case "up" -> new float[][]{{f[0], t[1], f[2]}, {f[0], t[1], t[2]}, {t[0], t[1], t[2]}, {t[0], t[1], f[2]}};
            case "north" -> new float[][]{{t[0], t[1], f[2]}, {t[0], f[1], f[2]}, {f[0], f[1], f[2]}, {f[0], t[1], f[2]}};
            case "south" -> new float[][]{{f[0], t[1], t[2]}, {f[0], f[1], t[2]}, {t[0], f[1], t[2]}, {t[0], t[1], t[2]}};
            case "west" -> new float[][]{{f[0], t[1], f[2]}, {f[0], f[1], f[2]}, {f[0], f[1], t[2]}, {f[0], t[1], t[2]}};
            default -> new float[][]{{t[0], t[1], t[2]}, {t[0], f[1], t[2]}, {t[0], f[1], f[2]}, {t[0], t[1], f[2]}};
        };
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float len(float[] v) {
        return (float)Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    private static float dist2(float[] a, float[] b) {
        float x = a[0] - b[0];
        float y = a[1] - b[1];
        float z = a[2] - b[2];
        return x * x + y * y + z * z;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.01F;
    }

    private static float clampTo(float v) {
        return Math.max(-16.0F, Math.min(32.0F, v));
    }

    private static float round(float v) {
        return Math.round(v * 1000.0F) / 1000.0F;
    }

    private static JsonArray vec(float[] v) {
        JsonArray a = new JsonArray();

        for (int i = 0; i < 3; i++) {
            a.add(round(v[i]));
        }

        return a;
    }

    private record Built(SpJavaModel.Element element, boolean exact) {
    }

    public record Element(
        float[] from, float[] to, String axis, float angle, float[] origin, List<SpJavaModel.Face> faces, float[] boundsMin, float[] boundsMax
    ) {
    }

    public record Face(String dir, float u1, float v1, float u2, float v2, int rotation) {
    }

    private record Raw(float[][] corners, List<float[][]> faces, List<float[]> uvs) {
    }

    public record Result(List<SpJavaModel.Element> elements, int approximated, float fitScale) {
        public JsonArray json(String texture) {
            JsonArray out = new JsonArray();

            for (SpJavaModel.Element e : this.elements) {
                JsonObject o = new JsonObject();
                o.add("from", SpJavaModel.vec(e.from));
                o.add("to", SpJavaModel.vec(e.to));
                if (!e.axis.isEmpty()) {
                    JsonObject r = new JsonObject();
                    r.addProperty("angle", e.angle);
                    r.addProperty("axis", e.axis);
                    r.add("origin", SpJavaModel.vec(e.origin));
                    o.add("rotation", r);
                }

                JsonObject faces = new JsonObject();

                for (SpJavaModel.Face f : e.faces) {
                    JsonObject fo = new JsonObject();
                    JsonArray uv = new JsonArray();

                    for (float v : new float[]{f.u1, f.v1, f.u2, f.v2}) {
                        uv.add(SpJavaModel.round(v));
                    }

                    fo.add("uv", uv);
                    if (f.rotation != 0) {
                        fo.addProperty("rotation", f.rotation);
                    }

                    fo.addProperty("texture", texture);
                    faces.add(f.dir, fo);
                }

                o.add("faces", faces);
                out.add(o);
            }

            return out;
        }

        public List<int[]> boxes() {
            Set<String> seen = new LinkedHashSet<>();
            List<int[]> out = new ArrayList<>();

            for (SpJavaModel.Element e : this.elements) {
                int[] b = new int[6];

                for (int a = 0; a < 3; a++) {
                    float lo = Math.max(-48.0F, Math.min(64.0F, e.boundsMin[a]));
                    float hi = Math.max(-48.0F, Math.min(64.0F, e.boundsMax[a]));
                    int l = (int)Math.floor(lo + 0.001);
                    int h = (int)Math.ceil(hi - 0.001);
                    if (h <= l) {
                        if (l >= 64) {
                            l = 63;
                        }

                        h = l + 1;
                    }

                    b[a] = l;
                    b[a + 3] = h;
                }

                if (seen.add(Arrays.toString(b))) {
                    out.add(b);
                }
            }

            return out;
        }
    }
}
