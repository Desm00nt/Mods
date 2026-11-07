package org.sprautestudio.spraute_engine.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sprautestudio.spraute_engine.core.math.SpVec3;

public final class SpBone {
    public final String name;
    public final SpVec3 pivot;
    public final SpVec3 rotation;
    public final float inflate;
    public final boolean mirror;
    public final List<SpCube> cubes;
    public final Map<String, SpLocator> locators;
    public final List<SpBone> children = new ArrayList<>();
    public SpBone parent;

    public SpBone(String name, SpVec3 pivot, SpVec3 rotation, List<SpCube> cubes) {
        this(name, pivot, rotation, 0.0F, false, cubes, Collections.emptyMap());
    }

    public SpBone(String name, SpVec3 pivot, SpVec3 rotation, float inflate, boolean mirror, List<SpCube> cubes, Map<String, SpLocator> locators) {
        this.name = name;
        this.pivot = pivot;
        this.rotation = rotation;
        this.inflate = inflate;
        this.mirror = mirror;
        this.cubes = cubes;
        this.locators = locators != null && !locators.isEmpty() ? Collections.unmodifiableMap(new LinkedHashMap<>(locators)) : Collections.emptyMap();
    }
}
