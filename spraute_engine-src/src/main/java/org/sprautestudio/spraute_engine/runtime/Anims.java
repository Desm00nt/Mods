package org.sprautestudio.spraute_engine.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.Emotions;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

public final class Anims {
    public static final String DEFAULT_NS = "spraute_engine";
    private static final Map<String, String> GESTURES = Map.of(
        "nod", "nodsYes", "nope", "nodsNo", "laugh", "laughter", "sigh", "idle_sigh", "shake", "shiver", "refuse", "refuse"
    );
    private static Map<ResourceLocation, SpAnimationParser.AnimationSet> files;
    private static List<Anims.Entry> cache;

    private Anims() {
    }

    private static Map<ResourceLocation, SpAnimationParser.AnimationSet> files() {
        if (files != null) {
            return files;
        } else {
            Map<ResourceLocation, SpAnimationParser.AnimationSet> out = new LinkedHashMap<>();

            try {
                Path root = Platform.get().modResource("assets", "spraute_engine", "animations");
                if (Files.isDirectory(root)) {
                    try (Stream<Path> walk = Files.walk(root)) {
                        for (Path f : (Iterable<Path>) walk.filter(p -> p.getFileName().toString().endsWith(".animation.json")).sorted()::iterator) {
                            String rel = root.relativize(f).toString().replace('\\', '/').replace(".animation.json", "");

                            try (InputStream in = Files.newInputStream(f)) {
                                out.put(new ResourceLocation("spraute_engine", rel), SpAnimationParser.parse(in));
                            }
                        }
                    }
                }
            } catch (RuntimeException | IOException var13) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read the built-in animations", (Throwable)var13);
            }

            files = out;
            return out;
        }
    }

    private static Animation clip(ResourceLocation file, String key, SpAnimationParser.AnimationClip c) {
        return new Animation(file + "!" + key, c);
    }

    public static Optional<Animation> resolve(String id) {
        if (id != null && !id.isBlank()) {
            String s = id.trim();
            if (s.startsWith("spraute:")) {
                return ModelLibrary.resolve(s);
            } else if (s.startsWith("project:")) {
                return ModelLibrary.projectAnimation(null, s);
            } else {
                Optional<Animation> shipped = shipped(s);
                return shipped.isPresent() ? shipped : ModelLibrary.projectAnimation(null, s);
            }
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Animation> shipped(String id) {
        String s = id;
        String key = null;
        int bang = id.indexOf(33);
        if (bang >= 0) {
            key = id.substring(bang + 1);
            s = id.substring(0, bang);
        }

        ResourceLocation rl = ResourceLocation.tryParse(s.contains(":") ? s : "spraute_engine:" + s);
        if (rl == null) {
            return Optional.empty();
        } else {
            if (rl.getNamespace().equals("spraute_engine") && rl.getPath().equals("entity/humanoid") && ("turn_left".equals(key) || "turn_right".equals(key))) {
                rl = new ResourceLocation("spraute_engine", "entity/classic");
            }

            SpAnimationParser.AnimationSet set = files().get(rl);
            if (set == null) {
                return Optional.empty();
            } else if (key != null) {
                SpAnimationParser.AnimationClip c = set.get(key);
                return c == null ? Optional.empty() : Optional.of(clip(rl, key, c));
            } else {
                List<String> keys = new ArrayList<>(set.names());
                keys.removeIf(SpAnimationParser::isMetaClipName);
                if (keys.isEmpty()) {
                    return Optional.empty();
                } else {
                    Collections.sort(keys);
                    String last = rl.getPath().substring(rl.getPath().lastIndexOf(47) + 1);
                    String found = keys.get(0);

                    for (String k : keys) {
                        if (k.equals(last) || k.endsWith("." + last) || k.endsWith("_" + last)) {
                            found = k;
                        }
                    }

                    return Optional.of(clip(rl, found, set.get(found)));
                }
            }
        }
    }

    public static Optional<Animation> humanoid(String name) {
        return resolve("spraute_engine:entity/classic!" + name);
    }

    public static Animation turn(AnimatedEntity e, boolean left) {
        String name = left ? "turn_left" : "turn_right";
        if (e instanceof SprauteModelEntity me) {
            EntityLibrary.Look look = me.look();
            if (look != null) {
                Optional<Animation> own = look.animation(name);
                if (own.isPresent()) {
                    return own.get();
                }
            }
        }

        return humanoid(name).orElse(null);
    }

    public static Animation gesture(AnimatedEntity e, String name) {
        if (e instanceof SprauteModelEntity me && me.look() != null) {
            Optional<Animation> own = me.look().animation(name);
            if (own.isPresent()) {
                return own.get();
            }
        }

        return humanoid(GESTURES.getOrDefault(name, name)).orElse(null);
    }

    public static void showEmotion(AnimatedEntity e, String name) {
        String clip = Emotions.clip(name);
        if (clip != null) {
            Optional<Animation> a = Optional.empty();
            if (e instanceof SprauteModelEntity me && me.look() != null) {
                a = me.look().animation("emotion_" + name.toLowerCase(Locale.ROOT));
                if (a.isEmpty() && !clip.isEmpty()) {
                    a = me.look().animation(clip);
                }
            }

            if (a.isEmpty() && !clip.isEmpty()) {
                a = humanoid(clip);
            }

            if (a.isPresent()) {
                e.startAnimation("emotion", a.get().configure().transitionTime(0.25F).loopMode(LoopMode.HOLD_ON_LAST_FRAME));
            } else {
                e.getAnimationSystem().stopAnimation("emotion", 0.25F);
            }
        }
    }

    public static boolean exists(String id) {
        try {
            return resolve(id).isPresent();
        } catch (RuntimeException var2) {
            return false;
        }
    }

    public static int lengthTicks(Animation a, double speed) {
        if (speed <= 0.0) {
            speed = 1.0;
        }

        return (int)Math.ceil(a.lengthSeconds() * 20.0 / speed);
    }

    public static synchronized List<Anims.Entry> all() {
        if (cache != null) {
            return cache;
        } else {
            List<Anims.Entry> list = new ArrayList<>();
            files().forEach((rl, set) -> {
                TreeSet<String> keys = new TreeSet<>(set.names());
                keys.removeIf(SpAnimationParser::isMetaClipName);

                for (String k : keys) {
                    SpAnimationParser.AnimationClip c = set.get(k);
                    if (c != null) {
                        list.add(new Anims.Entry(rl, k, idFor(rl, k, keys.size()), c.getLengthSec(), c.isLoop()));
                    }
                }
            });
            list.sort((x, y) -> x.id().compareTo(y.id()));
            cache = Collections.unmodifiableList(list);
            return cache;
        }
    }

    public static String idFor(ResourceLocation container, String key, int keysInContainer) {
        String base = container.getNamespace().equals("spraute_engine") ? container.getPath() : container.toString();
        return keysInContainer == 1 ? base : base + "!" + key;
    }

    public record Entry(ResourceLocation container, String key, String id, double seconds, boolean loops) {
        public String folder() {
            String p = this.container.getPath();
            int i = p.lastIndexOf(47);
            return this.container.getNamespace() + ":" + (i < 0 ? "" : p.substring(0, i));
        }

        public String fileName() {
            String p = this.container.getPath();
            return p.substring(p.lastIndexOf(47) + 1);
        }
    }
}
