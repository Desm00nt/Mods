package org.sprautestudio.spraute_engine.core.model;

import java.util.Map;
import org.sprautestudio.spraute_engine.core.math.SpQuaternion;
import org.sprautestudio.spraute_engine.core.math.SpVec3;

public final class SpCube {
    public final SpVec3 origin;
    public final SpVec3 size;
    public final float inflate;
    public final SpVec3 pivot;
    public final SpVec3 rotation;
    public final boolean mirror;
    public final boolean boxUv;
    public final Map<SpCube.SpFace, SpFaceUV> faceUVs;
    private final boolean hasRotation;
    private final SpQuaternion rotationQuat;

    public SpCube(SpVec3 origin, SpVec3 size, float inflate, Map<SpCube.SpFace, SpFaceUV> faceUVs) {
        this(origin, size, inflate, new SpVec3(), new SpVec3(), false, false, faceUVs);
    }

    public SpCube(SpVec3 origin, SpVec3 size, float inflate, SpVec3 pivot, SpVec3 rotation, Map<SpCube.SpFace, SpFaceUV> faceUVs) {
        this(origin, size, inflate, pivot, rotation, false, false, faceUVs);
    }

    public SpCube(SpVec3 origin, SpVec3 size, float inflate, SpVec3 pivot, SpVec3 rotation, boolean mirror, boolean boxUv, Map<SpCube.SpFace, SpFaceUV> faceUVs) {
        this.origin = origin;
        this.size = size;
        this.inflate = inflate;
        this.pivot = pivot != null ? pivot : new SpVec3();
        this.rotation = rotation != null ? rotation : new SpVec3();
        this.mirror = mirror;
        this.boxUv = boxUv;
        this.faceUVs = faceUVs;
        this.hasRotation = Math.abs(this.rotation.x) > 1.0E-5F || Math.abs(this.rotation.y) > 1.0E-5F || Math.abs(this.rotation.z) > 1.0E-5F;
        if (this.hasRotation) {
            this.rotationQuat = new SpQuaternion().setEulerDeg(-this.rotation.x, this.rotation.y, -this.rotation.z);
        } else {
            this.rotationQuat = null;
        }
    }

    public boolean hasRotation() {
        return this.hasRotation;
    }

    public void modelPointToBoneLocal(SpBone bone, SpVec3 p) {
        if (this.hasRotation && this.rotationQuat != null) {
            p.x = p.x - this.pivot.x;
            p.y = p.y - this.pivot.y;
            p.z = p.z - this.pivot.z;
            this.rotationQuat.rotate(p);
            p.x = p.x + this.pivot.x;
            p.y = p.y + this.pivot.y;
            p.z = p.z + this.pivot.z;
        }

        p.x = p.x - bone.pivot.x;
        p.y = p.y - bone.pivot.y;
        p.z = p.z - bone.pivot.z;
    }

    public void rotateDirection(SpVec3 dir) {
        if (this.hasRotation && this.rotationQuat != null) {
            this.rotationQuat.rotate(dir);
        }
    }

    public SpCube.SpFace geometryFace(SpCube.SpFace face, boolean boxUv) {
        return switch (face) {
            case EAST -> this.mirror ? SpCube.SpFace.EAST : SpCube.SpFace.WEST;
            case WEST -> this.mirror ? SpCube.SpFace.WEST : SpCube.SpFace.EAST;
            case UP -> this.mirror && !boxUv ? SpCube.SpFace.DOWN : SpCube.SpFace.UP;
            case DOWN -> this.mirror && !boxUv ? SpCube.SpFace.UP : SpCube.SpFace.DOWN;
            default -> face;
        };
    }

    public static enum SpFace {
        NORTH,
        EAST,
        SOUTH,
        WEST,
        UP,
        DOWN;

        public static SpCube.SpFace fromString(String s) {
            String var1 = s.toLowerCase();

            return switch (var1) {
                case "north" -> NORTH;
                case "east" -> EAST;
                case "south" -> SOUTH;
                case "west" -> WEST;
                case "up" -> UP;
                case "down" -> DOWN;
                default -> null;
            };
        }
    }
}
