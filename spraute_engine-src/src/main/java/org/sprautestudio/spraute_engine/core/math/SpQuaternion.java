package org.sprautestudio.spraute_engine.core.math;

public final class SpQuaternion {
    public float x;
    public float y;
    public float z;
    public float w;

    public SpQuaternion() {
        this.w = 1.0F;
    }

    public SpQuaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public SpQuaternion identity() {
        this.x = 0.0F;
        this.y = 0.0F;
        this.z = 0.0F;
        this.w = 1.0F;
        return this;
    }

    public SpQuaternion setEulerDeg(float degX, float degY, float degZ) {
        float rx = (float)Math.toRadians(degX) * 0.5F;
        float ry = (float)Math.toRadians(degY) * 0.5F;
        float rz = (float)Math.toRadians(degZ) * 0.5F;
        float sx = (float)Math.sin(rx);
        float cx = (float)Math.cos(rx);
        float sy = (float)Math.sin(ry);
        float cy = (float)Math.cos(ry);
        float sz = (float)Math.sin(rz);
        float cz = (float)Math.cos(rz);
        this.w = cx * cy * cz + sx * sy * sz;
        this.x = sx * cy * cz - cx * sy * sz;
        this.y = cx * sy * cz + sx * cy * sz;
        this.z = cx * cy * sz - sx * sy * cz;
        return this;
    }

    public SpQuaternion setAxisAngle(float ax, float ay, float az, float angleDeg) {
        float halfRad = (float)Math.toRadians(angleDeg) * 0.5F;
        float s = (float)Math.sin(halfRad);
        this.x = ax * s;
        this.y = ay * s;
        this.z = az * s;
        this.w = (float)Math.cos(halfRad);
        return this;
    }

    public SpQuaternion mul(SpQuaternion o) {
        float nw = this.w * o.w - this.x * o.x - this.y * o.y - this.z * o.z;
        float nx = this.w * o.x + this.x * o.w + this.y * o.z - this.z * o.y;
        float ny = this.w * o.y - this.x * o.z + this.y * o.w + this.z * o.x;
        float nz = this.w * o.z + this.x * o.y - this.y * o.x + this.z * o.w;
        this.x = nx;
        this.y = ny;
        this.z = nz;
        this.w = nw;
        return this;
    }

    public SpVec3 rotate(SpVec3 v) {
        float vx = v.x;
        float vy = v.y;
        float vz = v.z;
        float tx = 2.0F * (this.y * vz - this.z * vy);
        float ty = 2.0F * (this.z * vx - this.x * vz);
        float tz = 2.0F * (this.x * vy - this.y * vx);
        v.x = vx + this.w * tx + (this.y * tz - this.z * ty);
        v.y = vy + this.w * ty + (this.z * tx - this.x * tz);
        v.z = vz + this.w * tz + (this.x * ty - this.y * tx);
        return v;
    }

    public SpQuaternion normalize() {
        float len = (float)Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w);
        if (len > 1.0E-8F) {
            float inv = 1.0F / len;
            this.x *= inv;
            this.y *= inv;
            this.z *= inv;
            this.w *= inv;
        }

        return this;
    }

    public SpQuaternion copy() {
        return new SpQuaternion(this.x, this.y, this.z, this.w);
    }

    public SpQuaternion set(SpQuaternion o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
        this.w = o.w;
        return this;
    }

    @Override
    public String toString() {
        return String.format("SpQuat(%.4f, %.4f, %.4f, %.4f)", this.x, this.y, this.z, this.w);
    }
}
