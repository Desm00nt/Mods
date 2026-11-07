package org.sprautestudio.spraute_engine.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import org.sprautestudio.spraute_engine.core.math.SpMatrix4;
import org.sprautestudio.spraute_engine.core.math.SpVec3;

public final class SpGeoMeshBaker {
    static final float MIN_EXTENT = 0.025F;

    private SpGeoMeshBaker() {
    }

    public static SpGeoBakedModel bake(SpGeoModel model) {
        if (model != null && !model.boneMap.isEmpty()) {
            SpModelInstance bind = new SpModelInstance(model);
            bind.computeTransforms();
            SpGeoMeshBaker.Bounds global = new SpGeoMeshBaker.Bounds();
            List<SpGeoBakedModel.Page> pages = new ArrayList<>();
            SpGeoMeshBaker.PageBuilder page = new SpGeoMeshBaker.PageBuilder();
            int faces = 0;
            int vertices = 0;

            for (SpBone bone : model.boneMap.values()) {
                if (page.bones.size() >= 48 && !page.hasBone(bone.name)) {
                    pages.add(page.finish());
                    page = new SpGeoMeshBaker.PageBuilder();
                }

                int paletteIndex = page.bone(bone.name);
                SpMatrix4 bindMatrix = bind.getBoneMatrix(bone.name);

                for (SpCube cube : bone.cubes) {
                    int before = page.faceCount();
                    bakeCube(model, bone, cube, paletteIndex, page, bindMatrix, global);
                    int added = page.faceCount() - before;
                    faces += added;
                    vertices += added * 4;
                }
            }

            if (page.faceCount() > 0) {
                pages.add(page.finish());
            }

            return new SpGeoBakedModel(pages, global.finish(), faces, vertices);
        } else {
            return new SpGeoBakedModel(List.of(), SpGeoBounds.EMPTY, 0, 0);
        }
    }

    private static void bakeCube(
        SpGeoModel model, SpBone bone, SpCube cube, int boneIndex, SpGeoMeshBaker.PageBuilder out, SpMatrix4 bind, SpGeoMeshBaker.Bounds global
    ) {
        float x0 = cube.origin.x - cube.inflate;
        float y0 = cube.origin.y - cube.inflate;
        float z0 = cube.origin.z - cube.inflate;
        float x1 = x0 + cube.size.x + cube.inflate * 2.0F;
        float y1 = y0 + cube.size.y + cube.inflate * 2.0F;
        float z1 = z0 + cube.size.z + cube.inflate * 2.0F;
        if (x1 - x0 < 0.025F) {
            float m = (x0 + x1) * 0.5F;
            x0 = m - 0.0125F;
            x1 = m + 0.0125F;
        }

        if (y1 - y0 < 0.025F) {
            float m = (y0 + y1) * 0.5F;
            y0 = m - 0.0125F;
            y1 = m + 0.0125F;
        }

        if (z1 - z0 < 0.025F) {
            float m = (z0 + z1) * 0.5F;
            z0 = m - 0.0125F;
            z1 = m + 0.0125F;
        }

        boolean cullSafe = cube.faceUVs.size() == 6 && cube.size.x > 2.0F && cube.size.y > 2.0F && cube.size.z > 2.0F;
        float faceSize = Math.max(cube.size.x, Math.max(cube.size.y, cube.size.z)) / 16.0F;

        for (Entry<SpCube.SpFace, SpFaceUV> entry : cube.faceUVs.entrySet()) {
            SpFaceUV uv = entry.getValue();
            if (!(Math.abs(uv.uSize) < 0.001F) || !(Math.abs(uv.vSize) < 0.001F)) {
                SpCube.SpFace face = cube.geometryFace(entry.getKey(), cube.boxUv);
                float[] verts = faceVertices(face, x0, y0, z0, x1, y1, z1);
                float[] normal = faceNormal(face);
                SpVec3 n = new SpVec3(normal[0], normal[1], normal[2]);
                cube.rotateDirection(n);
                float[] rotatedUv = faceUvs(uv, model.textureWidth, model.textureHeight, cube.mirror);
                float cx = 0.0F;
                float cy = 0.0F;
                float cz = 0.0F;
                int first = out.vertexCount();

                for (int i = 0; i < 4; i++) {
                    SpVec3 p = new SpVec3(verts[i * 3], verts[i * 3 + 1], verts[i * 3 + 2]);
                    cube.modelPointToBoneLocal(bone, p);
                    out.vertex(p.x, p.y, p.z, n.x, n.y, n.z, rotatedUv[i * 2], rotatedUv[i * 2 + 1], boneIndex);
                    cx += p.x;
                    cy += p.y;
                    cz += p.z;
                    SpVec3 world = new SpVec3(p);
                    bind.transformPoint(world);
                    global.add(-world.x, world.y, world.z);
                }

                out.face(first, cx * 0.25F, cy * 0.25F, cz * 0.25F, faceSize, cullSafe);
            }
        }
    }

    static float[] faceUvs(SpFaceUV uv, int texW, int texH, boolean mirror) {
        float u = uv.u / texW;
        float v = uv.v / texH;
        float u1 = (uv.u + uv.uSize) / texW;
        float v1 = (uv.v + uv.vSize) / texH;
        if (!mirror) {
            float t = u1;
            u1 = u;
            u = t;
        }

        int steps = Math.floorMod(uv.uvRotation, 360) / 90;

        return switch (steps) {
            case 1 -> new float[]{u1, v, u1, v1, u, v1, u, v};
            case 2 -> new float[]{u1, v1, u, v1, u, v, u1, v};
            case 3 -> new float[]{u, v1, u, v, u1, v, u1, v1};
            default -> new float[]{u, v, u1, v, u1, v1, u, v1};
        };
    }

    static float[] faceVertices(SpCube.SpFace face, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case NORTH -> new float[]{x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0};
            case SOUTH -> new float[]{x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1};
            case EAST -> new float[]{x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1};
            case WEST -> new float[]{x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0};
            case UP -> new float[]{x1, y1, z1, x0, y1, z1, x0, y1, z0, x1, y1, z0};
            case DOWN -> new float[]{x1, y0, z0, x0, y0, z0, x0, y0, z1, x1, y0, z1};
        };
    }

    private static float[] faceNormal(SpCube.SpFace face) {
        return switch (face) {
            case NORTH -> new float[]{0.0F, 0.0F, -1.0F};
            case SOUTH -> new float[]{0.0F, 0.0F, 1.0F};
            case EAST -> new float[]{1.0F, 0.0F, 0.0F};
            case WEST -> new float[]{-1.0F, 0.0F, 0.0F};
            case UP -> new float[]{0.0F, 1.0F, 0.0F};
            case DOWN -> new float[]{0.0F, -1.0F, 0.0F};
        };
    }

    private static final class Bools {
        boolean[] a = new boolean[16];
        int size;

        void add(boolean v) {
            if (this.size == this.a.length) {
                this.a = Arrays.copyOf(this.a, this.size * 2);
            }

            this.a[this.size++] = v;
        }

        boolean[] array() {
            return Arrays.copyOf(this.a, this.size);
        }
    }

    private static final class Bounds {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        float radius;

        void add(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
            this.radius = Math.max(this.radius, (float)Math.sqrt(x * x + y * y + z * z));
        }

        SpGeoBounds finish() {
            return this.minX == Float.POSITIVE_INFINITY
                ? SpGeoBounds.EMPTY
                : new SpGeoBounds(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ, this.radius);
        }
    }

    private static final class Bytes {
        byte[] a = new byte[32];
        int size;

        void add(byte v) {
            if (this.size == this.a.length) {
                this.a = Arrays.copyOf(this.a, this.size * 2);
            }

            this.a[this.size++] = v;
        }

        byte[] array() {
            return Arrays.copyOf(this.a, this.size);
        }
    }

    private static final class Floats {
        float[] a = new float[64];
        int size;

        void add(float... v) {
            for (float x : v) {
                if (this.size == this.a.length) {
                    this.a = Arrays.copyOf(this.a, this.size * 2);
                }

                this.a[this.size++] = x;
            }
        }

        float[] array() {
            return Arrays.copyOf(this.a, this.size);
        }
    }

    private static final class Ints {
        int[] a = new int[16];
        int size;

        void add(int v) {
            if (this.size == this.a.length) {
                this.a = Arrays.copyOf(this.a, this.size * 2);
            }

            this.a[this.size++] = v;
        }

        int[] array() {
            return Arrays.copyOf(this.a, this.size);
        }
    }

    private static final class PageBuilder {
        final List<String> bones = new ArrayList<>();
        final SpGeoMeshBaker.Floats pos = new SpGeoMeshBaker.Floats();
        final SpGeoMeshBaker.Floats norm = new SpGeoMeshBaker.Floats();
        final SpGeoMeshBaker.Floats uv = new SpGeoMeshBaker.Floats();
        final SpGeoMeshBaker.Floats centers = new SpGeoMeshBaker.Floats();
        final SpGeoMeshBaker.Floats sizes = new SpGeoMeshBaker.Floats();
        final SpGeoMeshBaker.Bytes boneIds = new SpGeoMeshBaker.Bytes();
        final SpGeoMeshBaker.Ints first = new SpGeoMeshBaker.Ints();
        final SpGeoMeshBaker.Bools cull = new SpGeoMeshBaker.Bools();

        boolean hasBone(String name) {
            return this.bones.contains(name);
        }

        int bone(String name) {
            int i = this.bones.indexOf(name);
            if (i >= 0) {
                return i;
            } else {
                this.bones.add(name);
                return this.bones.size() - 1;
            }
        }

        int vertexCount() {
            return this.boneIds.size;
        }

        int faceCount() {
            return this.first.size;
        }

        void vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v, int bone) {
            this.pos.add(x, y, z);
            this.norm.add(nx, ny, nz);
            this.uv.add(u, v);
            this.boneIds.add((byte)bone);
        }

        void face(int f, float x, float y, float z, float size, boolean safe) {
            this.first.add(f);
            this.centers.add(x, y, z);
            this.sizes.add(size);
            this.cull.add(safe);
        }

        SpGeoBakedModel.Page finish() {
            return new SpGeoBakedModel.Page(
                this.bones,
                this.pos.array(),
                this.norm.array(),
                this.uv.array(),
                this.boneIds.array(),
                this.first.array(),
                this.centers.array(),
                this.sizes.array(),
                this.cull.array()
            );
        }
    }
}
