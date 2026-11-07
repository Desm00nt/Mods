package org.sprautestudio.spraute_engine.core.math;

public final class SpVec3 {
    public float x;
    public float y;
    public float z;

    public SpVec3() {
    }

    public SpVec3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public SpVec3(SpVec3 other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }

    public SpVec3 set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public SpVec3 set(SpVec3 o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
        return this;
    }

    public SpVec3 add(SpVec3 o) {
        this.x = this.x + o.x;
        this.y = this.y + o.y;
        this.z = this.z + o.z;
        return this;
    }

    public SpVec3 add(float ax, float ay, float az) {
        this.x += ax;
        this.y += ay;
        this.z += az;
        return this;
    }

    public SpVec3 sub(SpVec3 o) {
        this.x = this.x - o.x;
        this.y = this.y - o.y;
        this.z = this.z - o.z;
        return this;
    }

    public SpVec3 scale(float s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
        return this;
    }

    public SpVec3 negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public float length() {
        return (float)Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public float lengthSq() {
        return this.x * this.x + this.y * this.y + this.z * this.z;
    }

    public SpVec3 normalize() {
        float len = this.length();
        if (len > 1.0E-8F) {
            float inv = 1.0F / len;
            this.x *= inv;
            this.y *= inv;
            this.z *= inv;
        }

        return this;
    }

    public float dot(SpVec3 o) {
        return this.x * o.x + this.y * o.y + this.z * o.z;
    }

    public SpVec3 cross(SpVec3 o) {
        float cx = this.y * o.z - this.z * o.y;
        float cy = this.z * o.x - this.x * o.z;
        float cz = this.x * o.y - this.y * o.x;
        this.x = cx;
        this.y = cy;
        this.z = cz;
        return this;
    }

    public SpVec3 lerp(SpVec3 target, float t) {
        this.x = this.x + (target.x - this.x) * t;
        this.y = this.y + (target.y - this.y) * t;
        this.z = this.z + (target.z - this.z) * t;
        return this;
    }

    public SpVec3 copy() {
        return new SpVec3(this.x, this.y, this.z);
    }

    @Override
    public String toString() {
        return String.format("SpVec3(%.4f, %.4f, %.4f)", this.x, this.y, this.z);
    }
}
