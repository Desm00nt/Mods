package org.sprautestudio.spraute_engine.core.math;

public final class SpMatrix4 {
    public final float[] m = new float[16];

    public SpMatrix4() {
        this.identity();
    }

    public SpMatrix4 identity() {
        for (int i = 0; i < 16; i++) {
            this.m[i] = 0.0F;
        }

        this.m[0] = this.m[5] = this.m[10] = this.m[15] = 1.0F;
        return this;
    }

    public SpMatrix4 set(SpMatrix4 o) {
        System.arraycopy(o.m, 0, this.m, 0, 16);
        return this;
    }

    public SpMatrix4 compose(SpVec3 translate, SpQuaternion rot, float scale) {
        return this.compose(translate.x, translate.y, translate.z, rot, scale, scale, scale);
    }

    public SpMatrix4 compose(SpVec3 translate, SpQuaternion rot, SpVec3 scale) {
        return this.compose(translate.x, translate.y, translate.z, rot, scale.x, scale.y, scale.z);
    }

    public SpMatrix4 compose(float tx, float ty, float tz, SpQuaternion rot, float sx, float sy, float sz) {
        float xx = rot.x * rot.x;
        float xy = rot.x * rot.y;
        float xz = rot.x * rot.z;
        float xw = rot.x * rot.w;
        float yy = rot.y * rot.y;
        float yz = rot.y * rot.z;
        float yw = rot.y * rot.w;
        float zz = rot.z * rot.z;
        float zw = rot.z * rot.w;
        this.m[0] = (1.0F - 2.0F * (yy + zz)) * sx;
        this.m[1] = 2.0F * (xy + zw) * sx;
        this.m[2] = 2.0F * (xz - yw) * sx;
        this.m[3] = 0.0F;
        this.m[4] = 2.0F * (xy - zw) * sy;
        this.m[5] = (1.0F - 2.0F * (xx + zz)) * sy;
        this.m[6] = 2.0F * (yz + xw) * sy;
        this.m[7] = 0.0F;
        this.m[8] = 2.0F * (xz + yw) * sz;
        this.m[9] = 2.0F * (yz - xw) * sz;
        this.m[10] = (1.0F - 2.0F * (xx + yy)) * sz;
        this.m[11] = 0.0F;
        this.m[12] = tx;
        this.m[13] = ty;
        this.m[14] = tz;
        this.m[15] = 1.0F;
        return this;
    }

    public SpMatrix4 setTranslated(SpMatrix4 parent, float tx, float ty, float tz) {
        float[] p = parent.m;
        System.arraycopy(p, 0, this.m, 0, 12);
        this.m[12] = p[0] * tx + p[4] * ty + p[8] * tz + p[12];
        this.m[13] = p[1] * tx + p[5] * ty + p[9] * tz + p[13];
        this.m[14] = p[2] * tx + p[6] * ty + p[10] * tz + p[14];
        this.m[15] = p[3] * tx + p[7] * ty + p[11] * tz + p[15];
        return this;
    }

    public SpMatrix4 mul(SpMatrix4 o) {
        float[] b = o.m;
        float a0 = this.m[0];
        float a1 = this.m[1];
        float a2 = this.m[2];
        float a3 = this.m[3];
        float a4 = this.m[4];
        float a5 = this.m[5];
        float a6 = this.m[6];
        float a7 = this.m[7];
        float a8 = this.m[8];
        float a9 = this.m[9];
        float a10 = this.m[10];
        float a11 = this.m[11];
        float a12 = this.m[12];
        float a13 = this.m[13];
        float a14 = this.m[14];
        float a15 = this.m[15];
        this.m[0] = a0 * b[0] + a4 * b[1] + a8 * b[2] + a12 * b[3];
        this.m[1] = a1 * b[0] + a5 * b[1] + a9 * b[2] + a13 * b[3];
        this.m[2] = a2 * b[0] + a6 * b[1] + a10 * b[2] + a14 * b[3];
        this.m[3] = a3 * b[0] + a7 * b[1] + a11 * b[2] + a15 * b[3];
        this.m[4] = a0 * b[4] + a4 * b[5] + a8 * b[6] + a12 * b[7];
        this.m[5] = a1 * b[4] + a5 * b[5] + a9 * b[6] + a13 * b[7];
        this.m[6] = a2 * b[4] + a6 * b[5] + a10 * b[6] + a14 * b[7];
        this.m[7] = a3 * b[4] + a7 * b[5] + a11 * b[6] + a15 * b[7];
        this.m[8] = a0 * b[8] + a4 * b[9] + a8 * b[10] + a12 * b[11];
        this.m[9] = a1 * b[8] + a5 * b[9] + a9 * b[10] + a13 * b[11];
        this.m[10] = a2 * b[8] + a6 * b[9] + a10 * b[10] + a14 * b[11];
        this.m[11] = a3 * b[8] + a7 * b[9] + a11 * b[10] + a15 * b[11];
        this.m[12] = a0 * b[12] + a4 * b[13] + a8 * b[14] + a12 * b[15];
        this.m[13] = a1 * b[12] + a5 * b[13] + a9 * b[14] + a13 * b[15];
        this.m[14] = a2 * b[12] + a6 * b[13] + a10 * b[14] + a14 * b[15];
        this.m[15] = a3 * b[12] + a7 * b[13] + a11 * b[14] + a15 * b[15];
        return this;
    }

    public SpVec3 transformPoint(SpVec3 v) {
        float rx = this.m[0] * v.x + this.m[4] * v.y + this.m[8] * v.z + this.m[12];
        float ry = this.m[1] * v.x + this.m[5] * v.y + this.m[9] * v.z + this.m[13];
        float rz = this.m[2] * v.x + this.m[6] * v.y + this.m[10] * v.z + this.m[14];
        v.x = rx;
        v.y = ry;
        v.z = rz;
        return v;
    }

    public SpVec3 transformDirection(SpVec3 v) {
        float rx = this.m[0] * v.x + this.m[4] * v.y + this.m[8] * v.z;
        float ry = this.m[1] * v.x + this.m[5] * v.y + this.m[9] * v.z;
        float rz = this.m[2] * v.x + this.m[6] * v.y + this.m[10] * v.z;
        v.x = rx;
        v.y = ry;
        v.z = rz;
        return v;
    }

    public SpVec3 getTranslation() {
        return new SpVec3(this.m[12], this.m[13], this.m[14]);
    }

    public SpMatrix4 copy() {
        SpMatrix4 c = new SpMatrix4();
        System.arraycopy(this.m, 0, c.m, 0, 16);
        return c;
    }
}
