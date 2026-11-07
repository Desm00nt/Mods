package org.sprautestudio.spraute_engine.core.model;

import java.util.List;

public final class SpGeoBakedModel {
    public static final int PALETTE_BONES = 48;
    public final List<SpGeoBakedModel.Page> pages;
    public final SpGeoBounds bounds;
    public final int faceCount;
    public final int vertexCount;

    public SpGeoBakedModel(List<SpGeoBakedModel.Page> pages, SpGeoBounds bounds, int faceCount, int vertexCount) {
        this.pages = List.copyOf(pages);
        this.bounds = bounds;
        this.faceCount = faceCount;
        this.vertexCount = vertexCount;
    }

    public record Page(
        List<String> bones,
        float[] positions,
        float[] normals,
        float[] uvs,
        byte[] boneIndices,
        int[] faceFirstVertices,
        float[] faceCenters,
        float[] faceSizes,
        boolean[] faceCullSafe
    ) {
        public Page(
            List<String> bones,
            float[] positions,
            float[] normals,
            float[] uvs,
            byte[] boneIndices,
            int[] faceFirstVertices,
            float[] faceCenters,
            float[] faceSizes,
            boolean[] faceCullSafe
        ) {
            bones = List.copyOf(bones);
            this.bones = bones;
            this.positions = positions;
            this.normals = normals;
            this.uvs = uvs;
            this.boneIndices = boneIndices;
            this.faceFirstVertices = faceFirstVertices;
            this.faceCenters = faceCenters;
            this.faceSizes = faceSizes;
            this.faceCullSafe = faceCullSafe;
        }

        public int vertexCount() {
            return this.boneIndices.length;
        }

        public int faceCount() {
            return this.faceFirstVertices.length;
        }
    }
}
