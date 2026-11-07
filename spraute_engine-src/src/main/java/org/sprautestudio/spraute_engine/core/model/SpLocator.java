package org.sprautestudio.spraute_engine.core.model;

import org.sprautestudio.spraute_engine.core.math.SpVec3;

public final class SpLocator {
    public final String name;
    public final SpVec3 offset;
    public final SpVec3 rotation;
    public final boolean ignoreInheritedScale;

    public SpLocator(String name, SpVec3 offset) {
        this(name, offset, new SpVec3(), false);
    }

    public SpLocator(String name, SpVec3 offset, SpVec3 rotation, boolean ignoreInheritedScale) {
        this.name = name;
        this.offset = offset != null ? offset : new SpVec3();
        this.rotation = rotation != null ? rotation : new SpVec3();
        this.ignoreInheritedScale = ignoreInheritedScale;
    }

    public boolean hasRotation() {
        return Math.abs(this.rotation.x) > 1.0E-5F || Math.abs(this.rotation.y) > 1.0E-5F || Math.abs(this.rotation.z) > 1.0E-5F;
    }
}
