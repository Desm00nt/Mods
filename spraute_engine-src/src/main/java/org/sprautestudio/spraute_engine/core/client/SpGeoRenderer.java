package org.sprautestudio.spraute_engine.core.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Map.Entry;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.math.SpMatrix4;
import org.sprautestudio.spraute_engine.core.math.SpVec3;
import org.sprautestudio.spraute_engine.core.model.SpBone;
import org.sprautestudio.spraute_engine.core.model.SpCube;
import org.sprautestudio.spraute_engine.core.model.SpFaceUV;
import org.sprautestudio.spraute_engine.core.model.SpGeoBakedModel;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.core.model.SpModelInstance;

public final class SpGeoRenderer {
    private static final ThreadLocal<int[]> FACE_ORDER = ThreadLocal.withInitial(() -> new int[64]);
    private static final ThreadLocal<float[]> FACE_KEYS = ThreadLocal.withInitial(() -> new float[64]);
    private static final Vector4f POSE_SCRATCH = new Vector4f();

    private SpGeoRenderer() {
    }

    public static void render(
        SpModelInstance instance,
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        float r,
        float g,
        float b,
        float a,
        String[] renderBones
    ) {
        render(instance, poseStack, consumer, consumer, packedLight, packedOverlay, r, g, b, a, renderBones, 0);
    }

    public static void render(
        SpModelInstance instance,
        PoseStack poseStack,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        float r,
        float g,
        float b,
        float a,
        String[] renderBones,
        int lod
    ) {
        render(instance, poseStack, consumer, consumer, packedLight, packedOverlay, r, g, b, a, renderBones, lod);
    }

    public static void render(
        SpModelInstance instance,
        PoseStack poseStack,
        VertexConsumer solid,
        VertexConsumer thin,
        int packedLight,
        int packedOverlay,
        float r,
        float g,
        float b,
        float a,
        String[] renderBones,
        int lod
    ) {
        Matrix4f pose = Mc.pose(poseStack.last());
        Matrix3f normalMat = Mc.normal(poseStack.last());
        SpGeoModel model = instance.getModel();
        if (model.baked != null && !model.baked.pages.isEmpty()) {
            renderBaked(instance, model.baked, pose, normalMat, solid, thin, packedLight, packedOverlay, r, g, b, a, renderBones, lod);
        } else {
            for (SpBone bone : model.boneMap.values()) {
                if ((renderBones == null || renderBones.length <= 0 || isBoneOrChildOf(bone, renderBones)) && !bone.cubes.isEmpty()) {
                    SpMatrix4 world = instance.getBoneMatrix(bone.name);
                    if (world != null) {
                        for (SpCube cube : bone.cubes) {
                            VertexConsumer dest = cube.faceUVs.size() == 6 && cube.size.x > 2.0F && cube.size.y > 2.0F && cube.size.z > 2.0F ? solid : thin;
                            renderCube(cube, bone, world, model, pose, normalMat, dest, packedLight, packedOverlay, r, g, b, a);
                        }
                    }
                }
            }
        }
    }

    private static void renderBaked(
        SpModelInstance instance,
        SpGeoBakedModel baked,
        Matrix4f pose,
        Matrix3f normalMat,
        VertexConsumer solid,
        VertexConsumer thin,
        int packedLight,
        int packedOverlay,
        float r,
        float g,
        float b,
        float a,
        String[] renderBones,
        int lod
    ) {
        float cutoff = 0.0F;
        if (lod >= 2) {
            float radius = Math.max(0.01F, baked.bounds.radius() / 16.0F);
            cutoff = lod == 2 ? radius * 0.025F : radius * 0.06F;
        }

        boolean cullBack = lod >= 1;

        for (int i = 0; i < baked.pages.size(); i++) {
            SpGeoBakedModel.Page page = baked.pages.get(i);
            if (solid == thin) {
                emitPageFaces(
                    instance, i, page, pose, normalMat, thin, packedLight, packedOverlay, r, g, b, a, renderBones, cutoff, cullBack, false, false, true
                );
            } else {
                emitPageFaces(
                    instance, i, page, pose, normalMat, solid, packedLight, packedOverlay, r, g, b, a, renderBones, cutoff, cullBack, true, false, false
                );
                emitPageFaces(
                    instance, i, page, pose, normalMat, thin, packedLight, packedOverlay, r, g, b, a, renderBones, cutoff, cullBack, false, true, false
                );
            }
        }
    }

    private static void emitPageFaces(
        SpModelInstance instance,
        int pageIndex,
        SpGeoBakedModel.Page page,
        Matrix4f pose,
        Matrix3f normalMat,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        float r,
        float g,
        float b,
        float a,
        String[] renderBones,
        float cutoff,
        boolean cullBack,
        boolean solidPass,
        boolean sortFarToNear,
        boolean allFaces
    ) {
        int n = 0;
        int faces = page.faceCount();
        int[] order = null;
        float[] keys = null;
        if (sortFarToNear) {
            order = FACE_ORDER.get();
            keys = FACE_KEYS.get();
            if (order.length < faces) {
                order = new int[faces];
                keys = new float[faces];
                FACE_ORDER.set(order);
                FACE_KEYS.set(keys);
            }
        }

        byte[] boneIndices = page.boneIndices();
        boolean[] cullSafe = page.faceCullSafe();
        int[] firsts = page.faceFirstVertices();
        float[] sizes = page.faceSizes();
        float[] centers = page.faceCenters();
        float[] positions = page.positions();
        float[] normals = page.normals();
        float[] uvs = page.uvs();
        SpMatrix4[] palette = instance.palette(pageIndex);
        boolean filtered = renderBones != null && renderBones.length > 0;
        float p00 = pose.m00();
        float p10 = pose.m10();
        float p20 = pose.m20();
        float p30 = pose.m30();
        float p01 = pose.m01();
        float p11 = pose.m11();
        float p21 = pose.m21();
        float p31 = pose.m31();
        float p02 = pose.m02();
        float p12 = pose.m12();
        float p22 = pose.m22();
        float p32 = pose.m32();
        float n00 = normalMat.m00();
        float n10 = normalMat.m10();
        float n20 = normalMat.m20();
        float n01 = normalMat.m01();
        float n11 = normalMat.m11();
        float n21 = normalMat.m21();
        float n02 = normalMat.m02();
        float n12 = normalMat.m12();
        float n22 = normalMat.m22();

        for (int face = 0; face < faces; face++) {
            if ((allFaces || cullSafe[face] == solidPass) && (!(cutoff > 0.0F) || !(sizes[face] < cutoff))) {
                int first = firsts[face];
                int boneIndex = boneIndices[first] & 255;
                if (filtered) {
                    SpBone bone = instance.getModel().boneMap.get(page.bones().get(boneIndex));
                    if (bone == null || !isBoneOrChildOf(bone, renderBones)) {
                        continue;
                    }
                }

                SpMatrix4 world = palette[boneIndex];
                if (world != null) {
                    float[] m = world.m;
                    if (sortFarToNear) {
                        int ci = face * 3;
                        float x = centers[ci];
                        float y = centers[ci + 1];
                        float z = centers[ci + 2];
                        float px = m[0] * x + m[4] * y + m[8] * z + m[12];
                        float py = m[1] * x + m[5] * y + m[9] * z + m[13];
                        float pz = m[2] * x + m[6] * y + m[10] * z + m[14];
                        keys[n] = poseDistSq(pose, -px / 16.0F, py / 16.0F, pz / 16.0F);
                        order[n++] = face;
                    } else {
                        emitFace(
                            face,
                            first,
                            m,
                            positions,
                            normals,
                            uvs,
                            centers,
                            cullBack && cullSafe[face],
                            consumer,
                            p00,
                            p10,
                            p20,
                            p30,
                            p01,
                            p11,
                            p21,
                            p31,
                            p02,
                            p12,
                            p22,
                            p32,
                            n00,
                            n10,
                            n20,
                            n01,
                            n11,
                            n21,
                            n02,
                            n12,
                            n22,
                            r,
                            g,
                            b,
                            a,
                            packedOverlay,
                            packedLight
                        );
                    }
                }
            }
        }

        if (n > 1) {
            sortByKeyDesc(order, keys, n);
        }

        for (int i = 0; i < n; i++) {
            int facex = order[i];
            int firstx = firsts[facex];
            SpMatrix4 world = palette[boneIndices[firstx] & 255];
            emitFace(
                facex,
                firstx,
                world.m,
                positions,
                normals,
                uvs,
                centers,
                cullBack && cullSafe[facex],
                consumer,
                p00,
                p10,
                p20,
                p30,
                p01,
                p11,
                p21,
                p31,
                p02,
                p12,
                p22,
                p32,
                n00,
                n10,
                n20,
                n01,
                n11,
                n21,
                n02,
                n12,
                n22,
                r,
                g,
                b,
                a,
                packedOverlay,
                packedLight
            );
        }
    }

    private static void emitFace(
        int face,
        int first,
        float[] m,
        float[] positions,
        float[] normals,
        float[] uvs,
        float[] centers,
        boolean mayCull,
        VertexConsumer consumer,
        float p00,
        float p10,
        float p20,
        float p30,
        float p01,
        float p11,
        float p21,
        float p31,
        float p02,
        float p12,
        float p22,
        float p32,
        float n00,
        float n10,
        float n20,
        float n01,
        float n11,
        float n21,
        float n02,
        float n12,
        float n22,
        float r,
        float g,
        float b,
        float a,
        int overlay,
        int light
    ) {
        int fp = first * 3;
        float nx = normals[fp];
        float ny = normals[fp + 1];
        float nz = normals[fp + 2];
        float tx = -(m[0] * nx + m[4] * ny + m[8] * nz);
        float ty = m[1] * nx + m[5] * ny + m[9] * nz;
        float tz = m[2] * nx + m[6] * ny + m[10] * nz;
        float len2 = tx * tx + ty * ty + tz * tz;
        float inv = (float)(1.0 / Math.sqrt(Math.max(1.0E-12F, len2)));
        tx *= inv;
        ty *= inv;
        tz *= inv;
        float vnx = n00 * tx + n10 * ty + n20 * tz;
        float vny = n01 * tx + n11 * ty + n21 * tz;
        float vnz = n02 * tx + n12 * ty + n22 * tz;
        if (mayCull && len2 > 1.0E-6F) {
            int ci = face * 3;
            float x = centers[ci];
            float y = centers[ci + 1];
            float z = centers[ci + 2];
            float wx = -(m[0] * x + m[4] * y + m[8] * z + m[12]) / 16.0F;
            float wy = (m[1] * x + m[5] * y + m[9] * z + m[13]) / 16.0F;
            float wz = (m[2] * x + m[6] * y + m[10] * z + m[14]) / 16.0F;
            float cx = p00 * wx + p10 * wy + p20 * wz + p30;
            float cy = p01 * wx + p11 * wy + p21 * wz + p31;
            float cz = p02 * wx + p12 * wy + p22 * wz + p32;
            if (cx * vnx + cy * vny + cz * vnz > 0.0F) {
                return;
            }
        }

        for (int v = first; v < first + 4; v++) {
            int p = v * 3;
            int t = v * 2;
            float x = positions[p];
            float y = positions[p + 1];
            float z = positions[p + 2];
            float wx = -(m[0] * x + m[4] * y + m[8] * z + m[12]) / 16.0F;
            float wy = (m[1] * x + m[5] * y + m[9] * z + m[13]) / 16.0F;
            float wz = (m[2] * x + m[6] * y + m[10] * z + m[14]) / 16.0F;
            consumer.vertex(
                p00 * wx + p10 * wy + p20 * wz + p30,
                p01 * wx + p11 * wy + p21 * wz + p31,
                p02 * wx + p12 * wy + p22 * wz + p32,
                r,
                g,
                b,
                a,
                uvs[t],
                uvs[t + 1],
                overlay,
                light,
                vnx,
                vny,
                vnz
            );
        }
    }

    private static float poseDistSq(Matrix4f pose, float x, float y, float z) {
        Vector4f v = POSE_SCRATCH.set(x, y, z, 1.0F);
        pose.transform(v);
        return v.x * v.x + v.y * v.y + v.z * v.z;
    }

    private static void sortByKeyDesc(int[] order, float[] keys, int n) {
        for (int i = 1; i < n; i++) {
            int idx = order[i];
            float key = keys[i];

            int j;
            for (j = i - 1; j >= 0 && keys[j] < key; j--) {
                order[j + 1] = order[j];
                keys[j + 1] = keys[j];
            }

            order[j + 1] = idx;
            keys[j + 1] = key;
        }
    }

    private static boolean isBoneOrChildOf(SpBone bone, String[] renderBones) {
        for (SpBone current = bone; current != null; current = current.parent) {
            for (String rb : renderBones) {
                if (current.name.equalsIgnoreCase(rb)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void renderCube(
        SpCube cube,
        SpBone bone,
        SpMatrix4 boneWorld,
        SpGeoModel model,
        Matrix4f pose,
        Matrix3f normalMat,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay,
        float r,
        float g,
        float b,
        float a
    ) {
        float inf = cube.inflate;
        float x0 = cube.origin.x - inf;
        float y0 = cube.origin.y - inf;
        float z0 = cube.origin.z - inf;
        float x1 = x0 + cube.size.x + inf * 2.0F;
        float y1 = y0 + cube.size.y + inf * 2.0F;
        float z1 = z0 + cube.size.z + inf * 2.0F;
        float minExtent = 0.025F;
        if (x1 - x0 < 0.025F) {
            float mid = (x0 + x1) * 0.5F;
            x0 = mid - 0.0125F;
            x1 = mid + 0.0125F;
        }

        if (y1 - y0 < 0.025F) {
            float mid = (y0 + y1) * 0.5F;
            y0 = mid - 0.0125F;
            y1 = mid + 0.0125F;
        }

        if (z1 - z0 < 0.025F) {
            float mid = (z0 + z1) * 0.5F;
            z0 = mid - 0.0125F;
            z1 = mid + 0.0125F;
        }

        float invTexW = 1.0F / model.textureWidth;
        float invTexH = 1.0F / model.textureHeight;

        for (Entry<SpCube.SpFace, SpFaceUV> entry : cube.faceUVs.entrySet()) {
            SpCube.SpFace face = entry.getKey();
            SpFaceUV uv = entry.getValue();
            if (!(Math.abs(uv.uSize) < 0.001F) || !(Math.abs(uv.vSize) < 0.001F)) {
                SpCube.SpFace geomFace = cube.geometryFace(face, cube.boxUv);
                float[][] verts = getFaceVertices(geomFace, x0, y0, z0, x1, y1, z1);
                float[] fn = getFaceNormal(geomFace);
                float u = uv.u * invTexW;
                float v = uv.v * invTexH;
                float uWidth = (uv.u + uv.uSize) * invTexW;
                float vHeight = (uv.v + uv.vSize) * invTexH;
                if (!cube.mirror) {
                    float tmp = uWidth;
                    uWidth = u;
                    u = tmp;
                }

                float[][] uvs = rotateUvs(u, v, uWidth, vHeight, uv.uvRotation);
                SpVec3 nv = new SpVec3(fn[0], fn[1], fn[2]);
                cube.rotateDirection(nv);
                boneWorld.transformDirection(nv);
                nv.x = -nv.x;
                nv.normalize();
                float toBlocks = 0.0625F;

                for (int i = 0; i < 4; i++) {
                    SpVec3 p = new SpVec3(verts[i][0], verts[i][1], verts[i][2]);
                    cube.modelPointToBoneLocal(bone, p);
                    boneWorld.transformPoint(p);
                    float mx = -p.x * toBlocks;
                    float my = p.y * toBlocks;
                    float mz = p.z * toBlocks;
                    Mc.normal(
                            Mc.vertex(consumer, pose, mx, my, mz).color(r, g, b, a).uv(uvs[i][0], uvs[i][1]).overlayCoords(packedOverlay).uv2(packedLight),
                            normalMat,
                            nv.x,
                            nv.y,
                            nv.z
                        )
                        .endVertex();
                }
            }
        }
    }

    private static float[][] rotateUvs(float u, float v, float uWidth, float vHeight, int rotationDeg) {
        int steps = (rotationDeg % 360 + 360) % 360 / 90;

        float[] flat = switch (steps) {
            case 1 -> new float[]{uWidth, v, uWidth, vHeight, u, vHeight, u, v};
            case 2 -> new float[]{uWidth, vHeight, u, vHeight, u, v, uWidth, v};
            case 3 -> new float[]{u, vHeight, u, v, uWidth, v, uWidth, vHeight};
            default -> new float[]{u, v, uWidth, v, uWidth, vHeight, u, vHeight};
        };
        return new float[][]{{flat[0], flat[1]}, {flat[2], flat[3]}, {flat[4], flat[5]}, {flat[6], flat[7]}};
    }

    private static float[][] getFaceVertices(SpCube.SpFace face, float x0, float y0, float z0, float x1, float y1, float z1) {
        return switch (face) {
            case NORTH -> new float[][]{{x1, y1, z0}, {x0, y1, z0}, {x0, y0, z0}, {x1, y0, z0}};
            case SOUTH -> new float[][]{{x0, y1, z1}, {x1, y1, z1}, {x1, y0, z1}, {x0, y0, z1}};
            case EAST -> new float[][]{{x1, y1, z1}, {x1, y1, z0}, {x1, y0, z0}, {x1, y0, z1}};
            case WEST -> new float[][]{{x0, y1, z0}, {x0, y1, z1}, {x0, y0, z1}, {x0, y0, z0}};
            case UP -> new float[][]{{x1, y1, z1}, {x0, y1, z1}, {x0, y1, z0}, {x1, y1, z0}};
            case DOWN -> new float[][]{{x1, y0, z0}, {x0, y0, z0}, {x0, y0, z1}, {x1, y0, z1}};
        };
    }

    private static float[] getFaceNormal(SpCube.SpFace face) {
        return switch (face) {
            case NORTH -> new float[]{0.0F, 0.0F, -1.0F};
            case SOUTH -> new float[]{0.0F, 0.0F, 1.0F};
            case EAST -> new float[]{1.0F, 0.0F, 0.0F};
            case WEST -> new float[]{-1.0F, 0.0F, 0.0F};
            case UP -> new float[]{0.0F, 1.0F, 0.0F};
            case DOWN -> new float[]{0.0F, -1.0F, 0.0F};
        };
    }
}
