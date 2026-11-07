package org.sprautestudio.spraute_engine.core.model;

public final class SpFaceUV {
    public final float u;
    public final float v;
    public final float uSize;
    public final float vSize;
    public final int uvRotation;

    public SpFaceUV(float u, float v, float uSize, float vSize) {
        this(u, v, uSize, vSize, 0);
    }

    public SpFaceUV(float u, float v, float uSize, float vSize, int uvRotation) {
        this.u = u;
        this.v = v;
        this.uSize = uSize;
        this.vSize = vSize;
        int r = uvRotation % 360;
        if (r < 0) {
            r += 360;
        }

        this.uvRotation = r / 90 * 90;
    }
}
