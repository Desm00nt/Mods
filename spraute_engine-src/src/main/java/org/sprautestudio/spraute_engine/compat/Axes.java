package org.sprautestudio.spraute_engine.compat;

import org.joml.Quaternionf;

public enum Axes {
    XP(1.0F, 0.0F, 0.0F),
    XN(-1.0F, 0.0F, 0.0F),
    YP(0.0F, 1.0F, 0.0F),
    YN(0.0F, -1.0F, 0.0F),
    ZP(0.0F, 0.0F, 1.0F),
    ZN(0.0F, 0.0F, -1.0F);

    private final float x;
    private final float y;
    private final float z;

    private Axes(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Quaternionf rotation(float radians) {
        return new Quaternionf().setAngleAxis(radians, this.x, this.y, this.z);
    }

    public Quaternionf rotationDegrees(float degrees) {
        return this.rotation((float)Math.toRadians(degrees));
    }
}
