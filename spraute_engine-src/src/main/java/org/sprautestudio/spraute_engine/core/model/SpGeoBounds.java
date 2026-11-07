package org.sprautestudio.spraute_engine.core.model;

public record SpGeoBounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float radius) {
    public static final SpGeoBounds EMPTY = new SpGeoBounds(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

    public boolean isEmpty() {
        return this.maxX <= this.minX && this.maxY <= this.minY && this.maxZ <= this.minZ;
    }

    public float width() {
        return this.maxX - this.minX;
    }

    public float height() {
        return this.maxY - this.minY;
    }

    public float depth() {
        return this.maxZ - this.minZ;
    }
}
