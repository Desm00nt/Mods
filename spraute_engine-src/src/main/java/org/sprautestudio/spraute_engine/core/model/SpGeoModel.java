package org.sprautestudio.spraute_engine.core.model;

import java.util.List;
import java.util.Map;

public final class SpGeoModel {
    public final String identifier;
    public final int textureWidth;
    public final int textureHeight;
    public final List<SpBone> rootBones;
    public final Map<String, SpBone> boneMap;
    public final SpGeoBakedModel baked;

    public SpGeoModel(String identifier, int textureWidth, int textureHeight, List<SpBone> rootBones, Map<String, SpBone> boneMap) {
        this.identifier = identifier;
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.rootBones = rootBones;
        this.boneMap = boneMap;
        this.baked = SpGeoMeshBaker.bake(this);
    }

    public SpBone getBone(String name) {
        return this.boneMap.get(name);
    }

    public SpGeoModel.LocatorRef findLocator(String name) {
        if (name != null && !name.isBlank()) {
            for (SpBone bone : this.boneMap.values()) {
                SpLocator loc = bone.locators.get(name);
                if (loc != null) {
                    return new SpGeoModel.LocatorRef(bone, loc);
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public record LocatorRef(SpBone bone, SpLocator locator) {
    }
}
