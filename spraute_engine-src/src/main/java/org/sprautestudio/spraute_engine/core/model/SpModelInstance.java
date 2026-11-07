package org.sprautestudio.spraute_engine.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import org.sprautestudio.spraute_engine.core.math.SpMatrix4;
import org.sprautestudio.spraute_engine.core.math.SpQuaternion;
import org.sprautestudio.spraute_engine.core.math.SpVec3;

public final class SpModelInstance {
    private final SpGeoModel model;
    public final Map<String, SpMatrix4> boneWorldMatrices = new LinkedHashMap<>();
    public final Map<String, SpVec3> boneAnimRotation = new LinkedHashMap<>();
    public final Map<String, SpVec3> boneAnimPosition = new LinkedHashMap<>();
    public final Map<String, SpVec3> boneAnimScale = new LinkedHashMap<>();
    private final Map<String, SpMatrix4> interpolationFrom = new LinkedHashMap<>();
    private final Map<String, SpMatrix4> interpolationTo = new LinkedHashMap<>();
    private final Map<String, String> boneByLower = new HashMap<>();
    private final SpMatrix4 identity = new SpMatrix4();
    private final SpMatrix4 local = new SpMatrix4();
    private final SpQuaternion rotation = new SpQuaternion();
    private SpModelInstance.Node[] nodes;
    private SpVec3[] allRotations;
    private SpVec3[] allPositions;
    private SpVec3[] allScales;
    private final Map<String, String> resolvedNames = new HashMap<>();
    private SpMatrix4[][] palettes;

    public SpModelInstance(SpGeoModel model) {
        this.model = model;

        for (String name : model.boneMap.keySet()) {
            this.boneWorldMatrices.put(name, new SpMatrix4());
            this.boneAnimRotation.put(name, new SpVec3());
            this.boneAnimPosition.put(name, new SpVec3());
            this.boneAnimScale.put(name, new SpVec3(1.0F, 1.0F, 1.0F));
            this.interpolationFrom.put(name, new SpMatrix4());
            this.interpolationTo.put(name, new SpMatrix4());
            this.boneByLower.putIfAbsent(name.toLowerCase(Locale.ROOT), name);
        }

        this.allRotations = this.boneAnimRotation.values().toArray(new SpVec3[0]);
        this.allPositions = this.boneAnimPosition.values().toArray(new SpVec3[0]);
        this.allScales = this.boneAnimScale.values().toArray(new SpVec3[0]);
        List<SpModelInstance.Node> order = new ArrayList<>();

        for (SpBone root : model.rootBones) {
            this.collect(root, this.identity, order);
        }

        this.nodes = order.toArray(new SpModelInstance.Node[0]);
    }

    private void collect(SpBone bone, SpMatrix4 parentWorld, List<SpModelInstance.Node> out) {
        SpMatrix4 world = this.boneWorldMatrices.computeIfAbsent(bone.name, n -> new SpMatrix4());
        out.add(
            new SpModelInstance.Node(
                bone, parentWorld, world, this.boneAnimRotation.get(bone.name), this.boneAnimPosition.get(bone.name), this.boneAnimScale.get(bone.name)
            )
        );

        for (SpBone child : bone.children) {
            this.collect(child, world, out);
        }
    }

    public SpMatrix4[] palette(int pageIndex) {
        if (this.palettes == null) {
            this.palettes = new SpMatrix4[this.model.baked.pages.size()][];
        }

        SpMatrix4[] p = this.palettes[pageIndex];
        if (p == null) {
            List<String> bones = this.model.baked.pages.get(pageIndex).bones();
            p = new SpMatrix4[bones.size()];

            for (int i = 0; i < p.length; i++) {
                p[i] = this.boneWorldMatrices.get(bones.get(i));
            }

            this.palettes[pageIndex] = p;
        }

        return p;
    }

    public SpVec3 animRotation(String boneName) {
        return this.lookupAnim(this.boneAnimRotation, boneName);
    }

    public SpVec3 animPosition(String boneName) {
        return this.lookupAnim(this.boneAnimPosition, boneName);
    }

    public SpVec3 animScale(String boneName) {
        return this.lookupAnim(this.boneAnimScale, boneName);
    }

    private SpVec3 lookupAnim(Map<String, SpVec3> map, String boneName) {
        if (boneName != null && !boneName.isEmpty()) {
            String resolved = this.resolvedNames.get(boneName);
            if (resolved == null) {
                String canonical = map.containsKey(boneName) ? boneName : this.boneByLower.get(boneName.toLowerCase(Locale.ROOT));
                resolved = canonical == null ? "" : canonical;
                this.resolvedNames.put(boneName, resolved);
            }

            return resolved.isEmpty() ? null : map.get(resolved);
        } else {
            return null;
        }
    }

    public SpGeoModel getModel() {
        return this.model;
    }

    public void resetAnims() {
        for (SpVec3 rot : this.allRotations) {
            rot.set(0.0F, 0.0F, 0.0F);
        }

        for (SpVec3 pos : this.allPositions) {
            pos.set(0.0F, 0.0F, 0.0F);
        }

        for (SpVec3 scale : this.allScales) {
            scale.set(1.0F, 1.0F, 1.0F);
        }
    }

    public void computeTransforms() {
        this.identity.identity();

        for (SpModelInstance.Node node : this.nodes) {
            this.solve(node);
        }
    }

    private void solve(SpModelInstance.Node node) {
        SpBone bone = node.bone;
        SpVec3 animRot = node.rot;
        SpVec3 animPos = node.pos;
        SpVec3 animScale = node.scale;
        float rx = bone.rotation.x + (animRot != null ? animRot.x : 0.0F);
        float ry = bone.rotation.y + (animRot != null ? animRot.y : 0.0F);
        float rz = bone.rotation.z + (animRot != null ? animRot.z : 0.0F);
        float rotX = -rx;
        float rotZ = -rz;
        float offX = animPos != null ? animPos.x : 0.0F;
        float offY = animPos != null ? animPos.y : 0.0F;
        float offZ = animPos != null ? animPos.z : 0.0F;
        float parentPivotX = bone.parent != null ? bone.parent.pivot.x : 0.0F;
        float parentPivotY = bone.parent != null ? bone.parent.pivot.y : 0.0F;
        float parentPivotZ = bone.parent != null ? bone.parent.pivot.z : 0.0F;
        float localX = bone.pivot.x - parentPivotX + offX;
        float localY = bone.pivot.y - parentPivotY + offY;
        float localZ = bone.pivot.z - parentPivotZ + offZ;
        float scaleX = animScale != null ? animScale.x : 1.0F;
        float scaleY = animScale != null ? animScale.y : 1.0F;
        float scaleZ = animScale != null ? animScale.z : 1.0F;
        if (rotX == 0.0F && ry == 0.0F && rotZ == 0.0F && scaleX == 1.0F && scaleY == 1.0F && scaleZ == 1.0F) {
            node.world.setTranslated(node.parentWorld, localX, localY, localZ);
        } else {
            this.rotation.setEulerDeg(rotX, ry, rotZ);
            this.local.compose(localX, localY, localZ, this.rotation, scaleX, scaleY, scaleZ);
            node.world.set(node.parentWorld).mul(this.local);
        }
    }

    public SpMatrix4 getBoneMatrix(String boneName) {
        return this.boneWorldMatrices.get(boneName);
    }

    public void captureInterpolationFrom() {
        for (Entry<String, SpMatrix4> e : this.boneWorldMatrices.entrySet()) {
            this.interpolationFrom.get(e.getKey()).set(e.getValue());
        }
    }

    public void captureInterpolationTarget() {
        for (Entry<String, SpMatrix4> e : this.boneWorldMatrices.entrySet()) {
            this.interpolationTo.get(e.getKey()).set(e.getValue());
        }
    }

    public void applyInterpolation(float alpha) {
        float t = Math.max(0.0F, Math.min(1.0F, alpha));
        float inv = 1.0F - t;

        for (Entry<String, SpMatrix4> e : this.boneWorldMatrices.entrySet()) {
            SpMatrix4 from = this.interpolationFrom.get(e.getKey());
            SpMatrix4 to = this.interpolationTo.get(e.getKey());
            if (from != null && to != null) {
                float[] out = e.getValue().m;
                float[] a = from.m;
                float[] b = to.m;

                for (int i = 0; i < 16; i++) {
                    out[i] = a[i] * inv + b[i] * t;
                }
            }
        }
    }

    public SpVec3 getLocatorModelPosition(String locatorName) {
        SpGeoModel.LocatorRef ref = this.model.findLocator(locatorName);
        if (ref == null) {
            return null;
        } else {
            SpBone bone = ref.bone();
            SpLocator locator = ref.locator();
            SpMatrix4 world = this.boneWorldMatrices.get(bone.name);
            if (world == null) {
                return null;
            } else {
                SpVec3 p = new SpVec3(locator.offset);
                p.x = p.x - bone.pivot.x;
                p.y = p.y - bone.pivot.y;
                p.z = p.z - bone.pivot.z;
                if (locator.hasRotation()) {
                    new SpQuaternion().setEulerDeg(-locator.rotation.x, locator.rotation.y, -locator.rotation.z).rotate(p);
                }

                world.transformPoint(p);
                return p;
            }
        }
    }

    private static final class Node {
        final SpBone bone;
        final SpMatrix4 parentWorld;
        final SpMatrix4 world;
        final SpVec3 rot;
        final SpVec3 pos;
        final SpVec3 scale;

        Node(SpBone bone, SpMatrix4 parentWorld, SpMatrix4 world, SpVec3 rot, SpVec3 pos, SpVec3 scale) {
            this.bone = bone;
            this.parentWorld = parentWorld;
            this.world = world;
            this.rot = rot;
            this.pos = pos;
            this.scale = scale;
        }
    }
}
