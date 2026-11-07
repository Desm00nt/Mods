package org.sprautestudio.spraute_engine.core.anim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.sprautestudio.spraute_engine.core.model.SpModelInstance;
import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;

public final class AnimationSystem {
    private static Function<String, Optional<Animation>> library = id -> Optional.empty();
    private static final String REPLACING = "action";
    private static final List<String> SERVICE = List.of("movement", "turn", "EYES", "LOOK_LEFT", "LOOK_RIGHT");
    private static final float END_FADE = 0.3F;
    private final Map<String, AnimationSystem.Layer> layers = new LinkedHashMap<>();
    private long serial;
    private boolean dirty;
    private boolean firstNews = true;

    public static void setLibrary(Function<String, Optional<Animation>> found) {
        library = found;
    }

    public AnimationSystem() {
        for (String n : List.of("movement", "ambient")) {
            this.layers.put(n, new AnimationSystem.Layer(n));
        }

        for (int i = 0; i <= 20; i++) {
            this.layers.put("ANIMATION_" + i, new AnimationSystem.Layer("ANIMATION_" + i));
        }

        for (String n : List.of("action", "turn", "EYES", "EMOTION_COMBINE", "LOOK_LEFT", "LOOK_RIGHT", "emotion")) {
            this.layers.put(n, new AnimationSystem.Layer(n));
        }
    }

    public AnimationSystem.Layer getLayer(String name) {
        return this.layers.get(name);
    }

    public List<String> getLayerNames() {
        return new ArrayList<>(this.layers.keySet());
    }

    public void startAnimationAt(String layer, ConfiguredAnimation ca) {
        AnimationSystem.Layer l = this.layers.computeIfAbsent(layer, x$0 -> new AnimationSystem.Layer(x$0));
        l.startSerial = ++this.serial;
        this.start(l, ca);
        this.dirty = true;
    }

    private void start(AnimationSystem.Layer l, ConfiguredAnimation ca) {
        long now = System.nanoTime();
        release(l, Math.max(ca.transitionTime(), 0.001F), now);
        l.last = ca.animation();
        l.current = ca.animation().isNull() ? null : new AnimationSystem.Playing(ca);
    }

    private static void release(AnimationSystem.Layer l, float seconds, long now) {
        if (l.current != null) {
            AnimationSystem.Playing p = l.current;
            l.current = null;
            if (seconds <= 0.0F) {
                return;
            }

            p.fadeFrom = now;
            p.fadeSeconds = seconds;
            l.fading.add(p);
        }
    }

    public void setRate(String layer, float rate) {
        AnimationSystem.Layer l = this.layers.get(layer);
        if (l != null && l.current != null) {
            l.current.rate = rate;
        }
    }

    private static float smooth(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    public void stopAnimation(String layer, float fade) {
        this.startAnimationAt(layer, Animation.NULL.configure().transitionTime(fade));
    }

    public String encode() {
        StringBuilder sb = new StringBuilder();

        for (AnimationSystem.Layer l : this.layers.values()) {
            if (l.startSerial != 0L) {
                AnimationSystem.Playing p = l.current;
                sb.append(l.name).append('\t').append(l.startSerial).append('\t');
                if (p != null) {
                    sb.append(p.ca.animation().id())
                        .append('\t')
                        .append(p.ca.loopMode().ordinal())
                        .append('\t')
                        .append(p.ca.speed())
                        .append('\t')
                        .append(p.ca.transitionTime())
                        .append('\t')
                        .append(p.ca.startTime());
                } else {
                    sb.append("\t0\t1\t").append(0.2F).append("\t0");
                }

                sb.append('\n');
            }
        }

        return sb.toString();
    }

    public boolean takeDirty() {
        boolean d = this.dirty;
        this.dirty = false;
        return d;
    }

    public String encodeSaved() {
        StringBuilder sb = new StringBuilder();

        for (AnimationSystem.Layer l : this.layers.values()) {
            AnimationSystem.Playing p = l.current;
            if (p != null && !SERVICE.contains(l.name) && p.ca.loopMode() != LoopMode.ONCE) {
                sb.append(l.name)
                    .append('\t')
                    .append(p.ca.animation().id())
                    .append('\t')
                    .append(p.ca.loopMode().ordinal())
                    .append('\t')
                    .append(p.ca.speed())
                    .append('\n');
            }
        }

        return sb.toString();
    }

    public void restore(String saved) {
        for (String row : saved.split("\n")) {
            String[] f = row.split("\t", -1);
            if (f.length >= 4) {
                try {
                    Optional<Animation> a = library.apply(f[1]);
                    if (!a.isEmpty()) {
                        ConfiguredAnimation ca = a.get().configure().transitionTime(0.0F);
                        ca.loopMode(LoopMode.values()[Integer.parseInt(f[2])]).speed(Float.parseFloat(f[3]));
                        AnimationSystem.Layer l = this.layers.computeIfAbsent(f[0], x$0 -> new AnimationSystem.Layer(x$0));
                        l.startSerial = ++this.serial;
                        this.start(l, ca);
                        if (ca.loopMode() == LoopMode.HOLD_ON_LAST_FRAME) {
                            l.current.settled = true;
                        }

                        this.dirty = true;
                    }
                } catch (RuntimeException var10) {
                }
            }
        }
    }

    public void decode(String data) {
        boolean instant = this.firstNews;
        this.firstNews = false;

        for (String row : data.split("\n")) {
            String[] f = row.split("\t", -1);
            if (f.length >= 6) {
                try {
                    long s = Long.parseLong(f[1]);
                    AnimationSystem.Layer l = this.layers.computeIfAbsent(f[0], x$0 -> new AnimationSystem.Layer(x$0));
                    if (l.startSerial != s) {
                        l.startSerial = s;
                        ConfiguredAnimation ca = (f[2].isEmpty() ? Animation.NULL : library.apply(f[2]).orElse(Animation.NULL)).configure();
                        ca.loopMode(LoopMode.values()[Integer.parseInt(f[3])]).speed(Float.parseFloat(f[4])).transitionTime(Float.parseFloat(f[5]));
                        if (f.length > 6) {
                            ca.startTime(Float.parseFloat(f[6]));
                        }

                        if (instant) {
                            if (ca.loopMode() == LoopMode.ONCE) {
                                continue;
                            }

                            ca.transitionTime(0.0F);
                        }

                        this.start(l, ca);
                        if (instant && ca.loopMode() == LoopMode.HOLD_ON_LAST_FRAME && l.current != null) {
                            l.current.settled = true;
                        }
                    }
                } catch (RuntimeException var12) {
                }
            }
        }
    }

    public void apply(SpModelInstance instance) {
        long now = System.nanoTime();

        for (AnimationSystem.Layer l : this.layers.values()) {
            if (l.current != null || !l.fading.isEmpty()) {
                boolean additive = !l.name.equals("action");
                float fadeIn = 1.0F;
                if (l.current != null) {
                    AnimationSystem.Playing p = l.current;
                    float t = p.ca.transitionTime();
                    fadeIn = t <= 0.0F ? 1.0F : smooth((float)(now - p.start) / 1.0E9F / t);
                }

                Iterator<AnimationSystem.Playing> it = l.fading.iterator();

                while (it.hasNext()) {
                    AnimationSystem.Playing p = it.next();
                    float t = (float)(now - p.fadeFrom) / 1.0E9F / p.fadeSeconds;
                    if (t >= 1.0F) {
                        it.remove();
                    } else {
                        pose(instance, p, now, !additive && l.current != null ? 1.0F : 1.0F - smooth(t), additive);
                    }
                }

                if (l.current != null) {
                    AnimationSystem.Playing p = l.current;
                    pose(instance, p, now, fadeIn, additive);
                    if (p.ca.loopMode() == LoopMode.ONCE && p.clockNow() >= p.ca.animation().lengthSeconds()) {
                        release(l, 0.3F, now);
                    }
                }
            }
        }
    }

    private static void pose(SpModelInstance instance, AnimationSystem.Playing p, long now, float weight, boolean additive) {
        SpAnimationParser.AnimationClip clip = p.ca.animation().clip();
        if (clip != null && !(weight <= 0.0F)) {
            float time = p.settled ? clip.getLengthSec() : p.elapsed(now);
            boolean loop = p.ca.loopMode() == LoopMode.LOOP;
            if (!loop) {
                time = Math.min(time, clip.getLengthSec());
            }

            clip.apply(instance, time, weight, loop, additive);
        }
    }

    public final class Layer {
        final String name;
        private AnimationSystem.Playing current;
        private final List<AnimationSystem.Playing> fading = new ArrayList<>();
        private Animation last = Animation.NULL;
        private long startSerial;

        Layer(String name) {
            this.name = name;
        }

        public Animation activeAnimation() {
            return this.last;
        }
    }

    private static final class Playing {
        final ConfiguredAnimation ca;
        private double clock;
        private long last;
        float rate = 1.0F;
        final long start;
        long fadeFrom;
        float fadeSeconds;
        boolean settled;

        Playing(ConfiguredAnimation ca) {
            this.ca = ca;
            this.start = System.nanoTime();
            this.last = this.start;
            this.clock = ca.startTime();
        }

        double clockNow() {
            return this.clock;
        }

        float elapsed(long now) {
            this.clock = this.clock + (now - this.last) / 1.0E9 * this.ca.speed() * this.rate;
            this.last = now;
            return (float)this.clock;
        }
    }
}
