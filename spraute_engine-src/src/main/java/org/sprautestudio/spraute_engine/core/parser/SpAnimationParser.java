package org.sprautestudio.spraute_engine.core.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.sprautestudio.spraute_engine.core.math.SpVec3;
import org.sprautestudio.spraute_engine.core.model.SpModelInstance;

public final class SpAnimationParser {
    private SpAnimationParser() {
    }

    public static SpAnimationParser.AnimationSet parse(InputStream inputStream) {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)).getAsJsonObject();
        return parseRoot(root);
    }

    public static SpAnimationParser.AnimationSet parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return parseRoot(root);
    }

    private static SpAnimationParser.AnimationSet parseRoot(JsonObject root) {
        JsonObject animationsObj = root.getAsJsonObject("animations");
        if (animationsObj != null && animationsObj.size() != 0) {
            Map<String, SpAnimationParser.AnimationClip> byName = new LinkedHashMap<>();

            for (Entry<String, JsonElement> entry : animationsObj.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    SpAnimationParser.AnimationClip clip = parseClip(entry.getValue().getAsJsonObject());
                    byName.put(entry.getKey(), clip);
                }
            }

            return new SpAnimationParser.AnimationSet(byName);
        } else {
            return new SpAnimationParser.AnimationSet(Map.of());
        }
    }

    private static SpAnimationParser.AnimationClip parseClip(JsonObject obj) {
        float lengthSec = obj.has("animation_length") ? parseFloat(obj.get("animation_length")) : 0.0F;
        boolean loop = parseLoopFlag(obj.get("loop"));
        Map<String, SpAnimationParser.BoneTrack> boneTracks = new LinkedHashMap<>();
        JsonObject bonesObj = obj.getAsJsonObject("bones");
        if (bonesObj != null) {
            for (Entry<String, JsonElement> boneEntry : bonesObj.entrySet()) {
                if (boneEntry.getValue().isJsonObject()) {
                    JsonObject boneObj = boneEntry.getValue().getAsJsonObject();
                    SpAnimationParser.KeyframeTrack rot = parseChannel(boneObj.get("rotation"));
                    SpAnimationParser.KeyframeTrack pos = parseChannel(boneObj.get("position"));
                    SpAnimationParser.KeyframeTrack scale = parseChannel(boneObj.get("scale"));
                    if (!rot.isEmpty() || !pos.isEmpty() || !scale.isEmpty()) {
                        boneTracks.put(boneEntry.getKey(), new SpAnimationParser.BoneTrack(rot, pos, scale));
                    }
                }
            }
        }

        Map<String, Map<String, SpAnimationParser.PropertyTrack>> propertyTracks = new LinkedHashMap<>();
        JsonObject targetsObj = obj.getAsJsonObject("spraute_properties");
        if (targetsObj != null) {
            for (Entry<String, JsonElement> targetEntry : targetsObj.entrySet()) {
                if (targetEntry.getValue().isJsonObject()) {
                    Map<String, SpAnimationParser.PropertyTrack> target = new LinkedHashMap<>();

                    for (Entry<String, JsonElement> propertyEntry : targetEntry.getValue().getAsJsonObject().entrySet()) {
                        if (propertyEntry.getValue().isJsonObject()) {
                            JsonObject property = propertyEntry.getValue().getAsJsonObject();
                            String type = property.has("type") ? property.get("type").getAsString() : "number";
                            JsonObject keys = property.getAsJsonObject("keys");
                            if (keys != null) {
                                List<SpAnimationParser.PropertyKeyframe> frames = new ArrayList<>();

                                for (Entry<String, JsonElement> keyEntry : keys.entrySet()) {
                                    Float time = parseTime(keyEntry.getKey());
                                    if (time != null) {
                                        JsonElement value = keyEntry.getValue();
                                        int lerp = 0;
                                        if (value.isJsonObject() && value.getAsJsonObject().has("value")) {
                                            lerp = parseLerpMode(value);
                                            value = value.getAsJsonObject().get("value");
                                        }

                                        frames.add(new SpAnimationParser.PropertyKeyframe(time, value.deepCopy(), lerp));
                                    }
                                }

                                frames.sort(Comparator.comparingDouble(SpAnimationParser.PropertyKeyframe::time));
                                if (!frames.isEmpty()) {
                                    target.put(propertyEntry.getKey(), new SpAnimationParser.PropertyTrack(type, frames));
                                }
                            }
                        }
                    }

                    if (!target.isEmpty()) {
                        propertyTracks.put(targetEntry.getKey(), target);
                    }
                }
            }
        }

        SpAnimationParser.AnimationClip made = new SpAnimationParser.AnimationClip(lengthSec, loop, boneTracks, propertyTracks);
        JsonElement loopEl = obj.get("loop");
        if (loop && lengthSec > 0.0F) {
            for (SpAnimationParser.BoneTrack b : boneTracks.values()) {
                b.rotation().loopLength = lengthSec;
                b.position().loopLength = lengthSec;
                b.scale().loopLength = lengthSec;
            }
        }

        made.holdsLast = loopEl != null && loopEl.isJsonPrimitive() && "hold_on_last_frame".equalsIgnoreCase(loopEl.getAsString());
        return made;
    }

    private static SpAnimationParser.KeyframeTrack parseChannel(JsonElement channelEl) {
        if (channelEl != null && !channelEl.isJsonNull()) {
            List<SpAnimationParser.Keyframe> keyframes = new ArrayList<>();
            if (channelEl.isJsonArray()) {
                SpVec3 vec = parseVector(channelEl);
                if (vec != null) {
                    keyframes.add(new SpAnimationParser.Keyframe(0.0F, vec));
                }

                return new SpAnimationParser.KeyframeTrack(keyframes);
            } else if (channelEl.isJsonObject()) {
                JsonObject obj = channelEl.getAsJsonObject();
                SpVec3 single = parseVector(channelEl);
                if (single != null && hasVectorLikeField(obj)) {
                    keyframes.add(new SpAnimationParser.Keyframe(0.0F, single));
                    return new SpAnimationParser.KeyframeTrack(keyframes);
                } else {
                    for (Entry<String, JsonElement> entry : obj.entrySet()) {
                        Float time = parseTime(entry.getKey());
                        if (time != null) {
                            SpVec3 vec = parseVector(entry.getValue());
                            if (vec != null) {
                                keyframes.add(new SpAnimationParser.Keyframe(time, vec, parseLerpMode(entry.getValue())));
                            }
                        }
                    }

                    keyframes.sort(Comparator.comparingDouble(k -> k.time));
                    return new SpAnimationParser.KeyframeTrack(keyframes);
                }
            } else {
                return SpAnimationParser.KeyframeTrack.EMPTY;
            }
        } else {
            return SpAnimationParser.KeyframeTrack.EMPTY;
        }
    }

    private static int parseLerpMode(JsonElement el) {
        if (el != null && el.isJsonObject()) {
            JsonElement mode = el.getAsJsonObject().get("lerp_mode");
            if (mode != null && !mode.isJsonNull()) {
                try {
                    String s = mode.getAsString().toLowerCase(Locale.ROOT);
                    if (s.contains("catmull") || s.contains("smooth")) {
                        return 1;
                    }

                    if (s.contains("step")) {
                        return 2;
                    }
                } catch (Exception var3) {
                }

                return 0;
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static boolean hasVectorLikeField(JsonObject obj) {
        return obj.has("vector") || obj.has("post") || obj.has("pre");
    }

    private static Float parseTime(String raw) {
        try {
            return Float.parseFloat(raw);
        } catch (Exception var2) {
            return null;
        }
    }

    private static SpVec3 parseVector(JsonElement el) {
        if (el == null || el.isJsonNull()) {
            return null;
        } else if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            return arr.size() < 3 ? null : new SpVec3(parseFloat(arr.get(0)), parseFloat(arr.get(1)), parseFloat(arr.get(2)));
        } else {
            if (el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("vector")) {
                    return parseVector(obj.get("vector"));
                }

                if (obj.has("post")) {
                    return parseVector(obj.get("post"));
                }

                if (obj.has("pre")) {
                    return parseVector(obj.get("pre"));
                }
            }

            return null;
        }
    }

    private static float parseFloat(JsonElement el) {
        try {
            if (el == null || el.isJsonNull()) {
                return 0.0F;
            } else if (!el.isJsonPrimitive()) {
                return 0.0F;
            } else {
                return el.getAsJsonPrimitive().isNumber() ? el.getAsFloat() : Float.parseFloat(el.getAsString());
            }
        } catch (Exception var2) {
            return 0.0F;
        }
    }

    public static boolean isMetaClipName(String name) {
        if (name != null && !name.isBlank()) {
            String t = name.trim();
            char c = t.charAt(0);
            return c == '[' || c == '#' || t.startsWith("---") || t.startsWith("//");
        } else {
            return true;
        }
    }

    private static boolean parseLoopFlag(JsonElement el) {
        if (el != null && !el.isJsonNull()) {
            try {
                if (el.getAsJsonPrimitive().isBoolean()) {
                    return el.getAsBoolean();
                } else {
                    String s = el.getAsString().toLowerCase(Locale.ROOT);
                    return "true".equals(s) || "loop".equals(s);
                }
            } catch (Exception var2) {
                return false;
            }
        } else {
            return false;
        }
    }

    public static final class AnimationClip {
        private final float lengthSec;
        private final boolean loop;
        public boolean holdsLast;
        private final Map<String, SpAnimationParser.BoneTrack> bones;
        private final Map<String, Map<String, SpAnimationParser.PropertyTrack>> properties;

        public AnimationClip(float lengthSec, boolean loop, Map<String, SpAnimationParser.BoneTrack> bones) {
            this(lengthSec, loop, bones, Map.of());
        }

        public AnimationClip(
            float lengthSec, boolean loop, Map<String, SpAnimationParser.BoneTrack> bones, Map<String, Map<String, SpAnimationParser.PropertyTrack>> properties
        ) {
            this.lengthSec = lengthSec;
            this.loop = loop;
            this.bones = bones;
            this.properties = properties == null ? Map.of() : properties;
        }

        public float getLengthSec() {
            return this.lengthSec;
        }

        public boolean isLoop() {
            return this.loop;
        }

        public boolean sampleBone(String bone, float time, SpVec3 rot, SpVec3 pos) {
            SpAnimationParser.BoneTrack tr = this.bones.get(bone);
            if (tr == null) {
                return false;
            } else {
                boolean any = false;
                if (tr.rotation != null && !tr.rotation.isEmpty()) {
                    tr.rotation.sample(time, rot);
                    any = true;
                } else {
                    rot.set(0.0F, 0.0F, 0.0F);
                }

                if (tr.position != null && !tr.position.isEmpty()) {
                    tr.position.sample(time, pos);
                    any = true;
                } else {
                    pos.set(0.0F, 0.0F, 0.0F);
                }

                return any;
            }
        }

        public Map<String, SpAnimationParser.BoneTrack> bones() {
            return this.bones;
        }

        public Map<String, Map<String, SpAnimationParser.PropertyTrack>> properties() {
            return this.properties;
        }

        public void apply(SpModelInstance instance, float elapsedSec, float weight) {
            this.apply(instance, elapsedSec, weight, this.loop, true);
        }

        public void apply(SpModelInstance instance, float elapsedSec, float weight, boolean playbackLoop) {
            this.apply(instance, elapsedSec, weight, playbackLoop, true);
        }

        public void apply(SpModelInstance instance, float elapsedSec, float weight, boolean playbackLoop, boolean additive) {
            if (!this.bones.isEmpty() && !(weight <= 0.0F)) {
                float t = this.resolveTime(elapsedSec, playbackLoop);
                SpVec3 sampled = new SpVec3();

                for (Entry<String, SpAnimationParser.BoneTrack> boneEntry : this.bones.entrySet()) {
                    SpAnimationParser.BoneTrack track = boneEntry.getValue();
                    if (!track.rotation.isEmpty()) {
                        SpVec3 dst = instance.animRotation(boneEntry.getKey());
                        if (dst != null) {
                            track.rotation.sample(t, sampled);
                            if (additive) {
                                dst.add(sampled.x * weight, sampled.y * weight, sampled.z * weight);
                            } else {
                                dst.x = lerp(dst.x, sampled.x, weight);
                                dst.y = lerp(dst.y, sampled.y, weight);
                                dst.z = lerp(dst.z, sampled.z, weight);
                            }
                        }
                    }

                    if (!track.position.isEmpty()) {
                        SpVec3 dst = instance.animPosition(boneEntry.getKey());
                        if (dst != null) {
                            track.position.sample(t, sampled);
                            if (additive) {
                                dst.add(sampled.x * weight, sampled.y * weight, sampled.z * weight);
                            } else {
                                dst.x = lerp(dst.x, sampled.x, weight);
                                dst.y = lerp(dst.y, sampled.y, weight);
                                dst.z = lerp(dst.z, sampled.z, weight);
                            }
                        }
                    }

                    if (!track.scale.isEmpty()) {
                        SpVec3 dst = instance.animScale(boneEntry.getKey());
                        if (dst != null) {
                            track.scale.sample(t, sampled);
                            if (additive) {
                                dst.x = dst.x + (sampled.x - 1.0F) * weight;
                                dst.y = dst.y + (sampled.y - 1.0F) * weight;
                                dst.z = dst.z + (sampled.z - 1.0F) * weight;
                            } else {
                                dst.x = lerp(dst.x, sampled.x, weight);
                                dst.y = lerp(dst.y, sampled.y, weight);
                                dst.z = lerp(dst.z, sampled.z, weight);
                            }

                            if (dst.x < 0.0F) {
                                dst.x = 0.0F;
                            }

                            if (dst.y < 0.0F) {
                                dst.y = 0.0F;
                            }

                            if (dst.z < 0.0F) {
                                dst.z = 0.0F;
                            }
                        }
                    }
                }
            }
        }

        private static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }

        private float resolveTime(float elapsedSec, boolean playbackLoop) {
            if (this.lengthSec <= 0.0F) {
                return Math.max(0.0F, elapsedSec);
            } else if (playbackLoop) {
                float mod = elapsedSec % this.lengthSec;
                if (mod < 0.0F) {
                    mod += this.lengthSec;
                }

                return mod;
            } else {
                return elapsedSec < 0.0F ? 0.0F : Math.min(elapsedSec, this.lengthSec);
            }
        }
    }

    public static final class AnimationSet {
        private final Map<String, SpAnimationParser.AnimationClip> byName;
        private final Map<String, SpAnimationParser.AnimationClip> byLowerName;

        public AnimationSet(Map<String, SpAnimationParser.AnimationClip> byName) {
            this.byName = byName;
            this.byLowerName = new LinkedHashMap<>();

            for (Entry<String, SpAnimationParser.AnimationClip> entry : byName.entrySet()) {
                this.byLowerName.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }

        public SpAnimationParser.AnimationClip get(String name) {
            if (name != null && !name.isEmpty()) {
                SpAnimationParser.AnimationClip exact = this.byName.get(name);
                if (exact != null) {
                    return exact;
                } else {
                    String lower = name.toLowerCase(Locale.ROOT);
                    SpAnimationParser.AnimationClip byLower = this.byLowerName.get(lower);
                    if (byLower != null) {
                        return byLower;
                    } else {
                        SpAnimationParser.AnimationClip unique = null;
                        int matches = 0;

                        for (Entry<String, SpAnimationParser.AnimationClip> e : this.byLowerName.entrySet()) {
                            String key = e.getKey();
                            if (!SpAnimationParser.isMetaClipName(key)) {
                                boolean hit = key.endsWith("." + lower) || key.endsWith("/" + lower);
                                if (!hit) {
                                    int dot = key.lastIndexOf(46);
                                    hit = dot >= 0 && key.substring(dot + 1).equals(lower);
                                }

                                if (hit) {
                                    matches++;
                                    unique = e.getValue();
                                }
                            }
                        }

                        return matches == 1 ? unique : null;
                    }
                }
            } else {
                return null;
            }
        }

        public SpAnimationParser.AnimationClip playable(String preferred) {
            if (preferred != null && !preferred.isBlank() && !SpAnimationParser.isMetaClipName(preferred)) {
                SpAnimationParser.AnimationClip clip = this.get(preferred);
                if (clip != null && !clip.bones.isEmpty()) {
                    return clip;
                }
            }

            SpAnimationParser.AnimationClip idle = this.get("idle");
            if (idle != null && !idle.bones.isEmpty()) {
                return idle;
            } else {
                for (Entry<String, SpAnimationParser.AnimationClip> e : this.byName.entrySet()) {
                    if (!SpAnimationParser.isMetaClipName(e.getKey()) && e.getValue() != null && !e.getValue().bones.isEmpty()) {
                        return e.getValue();
                    }
                }

                return null;
            }
        }

        public String firstPlayableName(String preferred) {
            if (preferred != null && !preferred.isBlank() && this.get(preferred) != null && !SpAnimationParser.isMetaClipName(preferred)) {
                return preferred;
            } else {
                if (this.get("idle") != null) {
                    for (String key : this.byName.keySet()) {
                        if (key.equalsIgnoreCase("idle")) {
                            return key;
                        }
                    }
                }

                for (String keyx : this.byName.keySet()) {
                    if (!SpAnimationParser.isMetaClipName(keyx)) {
                        SpAnimationParser.AnimationClip clip = this.byName.get(keyx);
                        if (clip != null && !clip.bones.isEmpty()) {
                            return keyx;
                        }
                    }
                }

                return "";
            }
        }

        public int size() {
            return this.byName.size();
        }

        public Set<String> names() {
            return this.byName.keySet();
        }
    }

    public static final class BoneTrack {
        private final SpAnimationParser.KeyframeTrack rotation;
        private final SpAnimationParser.KeyframeTrack position;
        private final SpAnimationParser.KeyframeTrack scale;

        public BoneTrack(SpAnimationParser.KeyframeTrack rotation, SpAnimationParser.KeyframeTrack position, SpAnimationParser.KeyframeTrack scale) {
            this.rotation = rotation;
            this.position = position;
            this.scale = scale != null ? scale : SpAnimationParser.KeyframeTrack.EMPTY;
        }

        public SpAnimationParser.KeyframeTrack rotation() {
            return this.rotation;
        }

        public SpAnimationParser.KeyframeTrack position() {
            return this.position;
        }

        public SpAnimationParser.KeyframeTrack scale() {
            return this.scale;
        }
    }

    public static final class Keyframe {
        public static final int LERP_LINEAR = 0;
        public static final int LERP_CATMULLROM = 1;
        public static final int LERP_STEP = 2;
        private final float time;
        private final SpVec3 value;
        private final int lerpMode;

        public Keyframe(float time, SpVec3 value) {
            this(time, value, 0);
        }

        public Keyframe(float time, SpVec3 value, int lerpMode) {
            this.time = time;
            this.value = value;
            this.lerpMode = lerpMode;
        }
    }

    public static final class KeyframeTrack {
        private static final SpAnimationParser.KeyframeTrack EMPTY = new SpAnimationParser.KeyframeTrack(List.of());
        private final float[] times;
        private final SpVec3[] values;
        private final int[] lerpModes;
        float loopLength;

        public KeyframeTrack(List<SpAnimationParser.Keyframe> keyframes) {
            if (keyframes != null && !keyframes.isEmpty()) {
                this.times = new float[keyframes.size()];
                this.values = new SpVec3[keyframes.size()];
                this.lerpModes = new int[keyframes.size()];

                for (int i = 0; i < keyframes.size(); i++) {
                    SpAnimationParser.Keyframe kf = keyframes.get(i);
                    this.times[i] = kf.time;
                    this.values[i] = kf.value;
                    this.lerpModes[i] = kf.lerpMode;
                }
            } else {
                this.times = new float[0];
                this.values = new SpVec3[0];
                this.lerpModes = new int[0];
            }
        }

        public boolean isEmpty() {
            return this.times.length == 0;
        }

        private float keyTime(int i) {
            int n = this.times.length;
            return this.loopLength > 0.0F
                ? this.times[Math.floorMod(i, n)] + Math.floorDiv(i, n) * this.loopLength
                : this.times[Math.max(0, Math.min(n - 1, i))];
        }

        private SpVec3 keyValue(int i) {
            int n = this.times.length;
            return this.values[this.loopLength > 0.0F ? Math.floorMod(i, n) : Math.max(0, Math.min(n - 1, i))];
        }

        public void sample(float t, SpVec3 out) {
            int n = this.times.length;
            if (n == 0) {
                out.set(0.0F, 0.0F, 0.0F);
            } else if (n == 1) {
                out.set(this.values[0]);
            } else {
                boolean wrap = this.loopLength > 0.0F;
                if (!wrap && t <= this.times[0]) {
                    out.set(this.values[0]);
                } else if (!wrap && t >= this.times[n - 1]) {
                    out.set(this.values[n - 1]);
                } else {
                    int idx;
                    if (wrap && t < this.times[0]) {
                        idx = -1;
                    } else {
                        idx = 0;

                        while (idx + 1 < n && t >= this.times[idx + 1]) {
                            idx++;
                        }
                    }

                    float t0 = this.keyTime(idx);
                    float t1 = this.keyTime(idx + 1);
                    SpVec3 v0 = this.keyValue(idx);
                    SpVec3 v1 = this.keyValue(idx + 1);
                    float span = t1 - t0;
                    if (span <= 1.0E-5F) {
                        out.set(v1);
                    } else {
                        float alpha = (t - t0) / span;
                        int mode = this.lerpModes[Math.floorMod(idx, n)];
                        if (mode == 2) {
                            out.set(v0);
                        } else if (mode != 1 && this.lerpModes[Math.floorMod(idx + 1, n)] != 1) {
                            out.set(v0.x + (v1.x - v0.x) * alpha, v0.y + (v1.y - v0.y) * alpha, v0.z + (v1.z - v0.z) * alpha);
                        } else {
                            float ta = this.keyTime(idx - 1);
                            float tb = this.keyTime(idx + 2);
                            SpVec3 pa = this.keyValue(idx - 1);
                            SpVec3 pb = this.keyValue(idx + 2);
                            out.set(
                                hermite(alpha, span, pa.x, v0.x, v1.x, pb.x, t0 - ta, tb - t1),
                                hermite(alpha, span, pa.y, v0.y, v1.y, pb.y, t0 - ta, tb - t1),
                                hermite(alpha, span, pa.z, v0.z, v1.z, pb.z, t0 - ta, tb - t1)
                            );
                        }
                    }
                }
            }
        }

        private static float hermite(float a, float span, float p0, float p1, float p2, float p3, float before, float after) {
            float m1 = before > 1.0E-5F ? (p2 - p0) / (before + span) : (p2 - p1) / span;
            float m2 = after > 1.0E-5F ? (p3 - p1) / (span + after) : (p2 - p1) / span;
            float a2 = a * a;
            float a3 = a2 * a;
            return (2.0F * a3 - 3.0F * a2 + 1.0F) * p1 + (a3 - 2.0F * a2 + a) * span * m1 + (-2.0F * a3 + 3.0F * a2) * p2 + (a3 - a2) * span * m2;
        }
    }

    public record PropertyKeyframe(float time, JsonElement value, int lerpMode) {
    }

    public static final class PropertyTrack {
        private final String type;
        private final List<SpAnimationParser.PropertyKeyframe> frames;

        public PropertyTrack(String type, List<SpAnimationParser.PropertyKeyframe> frames) {
            this.type = type == null ? "number" : type;
            this.frames = frames == null ? List.of() : List.copyOf(frames);
        }

        public String type() {
            return this.type;
        }

        public boolean isEmpty() {
            return this.frames.isEmpty();
        }

        public JsonElement sample(float time) {
            if (this.frames.isEmpty()) {
                return null;
            } else {
                SpAnimationParser.PropertyKeyframe floor = this.frames.get(0);
                SpAnimationParser.PropertyKeyframe ceil = floor;

                for (SpAnimationParser.PropertyKeyframe frame : this.frames) {
                    if (frame.time() <= time) {
                        floor = frame;
                    }

                    if (frame.time() >= time) {
                        ceil = frame;
                        break;
                    }

                    ceil = frame;
                }

                if (floor != ceil && floor.lerpMode() != 2 && floor.value().isJsonPrimitive() && ceil.value().isJsonPrimitive()) {
                    float span = ceil.time() - floor.time();
                    if (span <= 1.0E-6F) {
                        return floor.value().deepCopy();
                    } else {
                        double alpha = (time - floor.time()) / span;
                        if ("color".equalsIgnoreCase(this.type)
                            && floor.value().getAsJsonPrimitive().isString()
                            && ceil.value().getAsJsonPrimitive().isString()) {
                            String mixed = interpolateColor(floor.value().getAsString(), ceil.value().getAsString(), alpha);
                            if (mixed != null) {
                                return new JsonPrimitive(mixed);
                            }
                        }

                        if (floor.value().getAsJsonPrimitive().isNumber() && ceil.value().getAsJsonPrimitive().isNumber()) {
                            double a = floor.value().getAsDouble();
                            double b = ceil.value().getAsDouble();
                            return new JsonPrimitive(a + (b - a) * alpha);
                        } else {
                            return floor.value().deepCopy();
                        }
                    }
                } else {
                    return floor.value().deepCopy();
                }
            }
        }

        private static String interpolateColor(String a, String b, double alpha) {
            try {
                String ah = a.replace("#", "");
                String bh = b.replace("#", "");
                long ca = Long.parseLong(ah, 16);
                long cb = Long.parseLong(bh, 16);
                if (ah.length() <= 6) {
                    ca |= 4278190080L;
                }

                if (bh.length() <= 6) {
                    cb |= 4278190080L;
                }

                int out = 0;

                for (int shift = 0; shift <= 24; shift += 8) {
                    int av = (int)(ca >> shift & 255L);
                    int bv = (int)(cb >> shift & 255L);
                    out |= (int)Math.round(av + (bv - av) * alpha) << shift;
                }

                return String.format(Locale.ROOT, "#%08X", out);
            } catch (Exception var14) {
                return null;
            }
        }
    }
}
