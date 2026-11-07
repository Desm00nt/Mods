package org.sprautestudio.spraute_engine.runtime.models;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;
import org.sprautestudio.spraute_engine.core.parser.SpGeoParser;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class ModelLibrary {
    private static final Map<String, ModelLibrary.Model> MODELS = new LinkedHashMap<>();
    private static final Map<Path, ModelLibrary.Stamped<SpGeoModel>> GEOS = new HashMap<>();
    private static final Map<Path, ModelLibrary.Stamped<SpAnimationParser.AnimationSet>> ANIMS = new HashMap<>();
    private static final Map<String, SpGeoModel> BUILTINS = new HashMap<>();
    private static final List<ModelLibrary.AnimFile> ANIM_FILES = new ArrayList<>();
    private static final Map<Path, Long> CHECKED = new HashMap<>();
    private static final long CHECK_EVERY_MS = 1000L;
    public static final String PROJECT_ID = "project:";

    private ModelLibrary() {
    }

    private static long stamp(Path f) {
        try {
            return Files.getLastModifiedTime(f).toMillis() ^ Files.size(f);
        } catch (IOException var2) {
            return -1L;
        }
    }

    private static boolean checkedLately(Path f) {
        long now = System.currentTimeMillis();
        Long at = CHECKED.get(f);
        if (at != null && now - at < 1000L) {
            return true;
        } else {
            CHECKED.put(f, now);
            return false;
        }
    }

    private static synchronized SpGeoModel parsedGeo(Path f) {
        ModelLibrary.Stamped<SpGeoModel> old = GEOS.get(f);
        if (old != null && checkedLately(f)) {
            return old.value;
        } else {
            long s = stamp(f);
            if (old != null && old.stamp == s) {
                return old.value;
            } else {
                SpGeoModel m = null;

                try (InputStream in = Files.newInputStream(f)) {
                    m = SpGeoParser.parse(in);
                } catch (RuntimeException | IOException var10) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot load {}", f, var10);
                }

                GEOS.put(f, new ModelLibrary.Stamped<>(s, m));
                return m;
            }
        }
    }

    private static synchronized SpAnimationParser.AnimationSet parsedAnims(Path f) {
        long s = stamp(f);
        ModelLibrary.Stamped<SpAnimationParser.AnimationSet> old = ANIMS.get(f);
        if (old != null && old.stamp == s) {
            return old.value;
        } else {
            SpAnimationParser.AnimationSet set = null;

            try (InputStream in = Files.newInputStream(f)) {
                set = SpAnimationParser.parse(in);
            } catch (RuntimeException | IOException var10) {
                SprauteEngine.LOGGER.warn("Spraute: cannot load {}", f, var10);
            }

            ANIMS.put(f, new ModelLibrary.Stamped<>(s, set));
            return set;
        }
    }

    public static synchronized SpGeoModel builtinGeometry(String name) {
        SpGeoModel have = BUILTINS.get(name);
        if (have != null) {
            return have;
        } else {
            try {
                Path f = Platform.get().modResource("assets", "spraute_engine", "geo", "entity", name + ".geo.json");

                try (InputStream in = Files.newInputStream(f)) {
                    have = SpGeoParser.parse(in);
                    BUILTINS.put(name, have);
                }
            } catch (RuntimeException | IOException var8) {
                SprauteEngine.LOGGER.warn("Spraute: cannot load the model {}", name, var8);
            }

            return have;
        }
    }

    public static ResourceLocation builtinTexture() {
        return new ResourceLocation("spraute_engine", "textures/entity/classic.png");
    }

    private static List<Path> projectDirs() {
        List<Path> out = new ArrayList<>();
        Path root = null;
        if (Platform.get().isClient()) {
            try {
                root = Workspace.projectsDir();
            } catch (Throwable var6) {
                root = null;
            }
        } else {
            root = ProjectContent.root();
        }

        if (root != null && Files.isDirectory(root)) {
            try (Stream<Path> projects = Files.list(root)) {
                projects.filter(x$0 -> Files.isDirectory(x$0)).sorted().forEach(out::add);
            } catch (IOException var8) {
                SprauteEngine.LOGGER.warn("Spraute: cannot scan models", (Throwable)var8);
            }
        }

        if (Platform.get().isClient()) {
            out.addAll(ClientModels.serverProjects());
        }

        return out;
    }

    public static synchronized void scan() {
        CHECKED.clear();
        List<Path> dirs = projectDirs();
        Map<String, ModelLibrary.Model> found = new LinkedHashMap<>();
        List<ModelLibrary.AnimFile> animFiles = new ArrayList<>();
        Iterator var3 = dirs.iterator();
        if (var3.hasNext()) {
            Path pd = (Path)var3.next();
        }

        MODELS.clear();
        MODELS.putAll(found);
        ANIM_FILES.clear();
        ANIM_FILES.addAll(animFiles);
        EntityLibrary.scan(dirs);
        if (Platform.get().isClient()) {
            ClientModels.registerTextures(found.values());
        }
    }

    private static void scanAnimFiles(String project, Path dir, List<ModelLibrary.AnimFile> out) {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : (Iterable<Path>) files.filter(x$0 -> Files.isRegularFile(x$0)).sorted()::iterator) {
                    String n = f.getFileName().toString();
                    String low = n.toLowerCase();
                    int cut = low.endsWith(".animation.json")
                        ? ".animation.json".length()
                        : (low.endsWith(".animations.json") ? ".animations.json".length() : 0);
                    if (cut > 0) {
                        out.add(new ModelLibrary.AnimFile(project, n.substring(0, n.length() - cut), f));
                    }
                }
            } catch (IOException var11) {
                SprauteEngine.LOGGER.warn("Spraute: cannot scan animations", (Throwable)var11);
            }
        }
    }

    public static String projectNameOf(Path dir) {
        return projectName(dir);
    }

    private static String projectName(Path dir) {
        try {
            JsonObject json = new Gson().fromJson(Files.readString(dir.resolve("project.json")), JsonObject.class);
            if (json != null && json.has("name")) {
                return json.get("name").getAsString();
            }
        } catch (Exception var2) {
        }

        return dir.getFileName().toString();
    }

    private static ModelLibrary.Model model(String project, Path d) throws IOException {
        Path geo = null;
        Path tex = null;
        List<Path> anims = new ArrayList<>();

        try (Stream<Path> files = Files.list(d)) {
            for (Path f : (Iterable<Path>) files.sorted()::iterator) {
                String n = f.getFileName().toString().toLowerCase();
                if (n.endsWith(".geo.json")) {
                    if (geo == null) {
                        geo = f;
                    }
                } else if (n.endsWith(".animation.json") || n.endsWith(".animations.json")) {
                    anims.add(f);
                } else if (n.endsWith(".png") && tex == null) {
                    tex = f;
                }
            }
        }

        return geo == null ? null : new ModelLibrary.Model(project, d.getFileName().toString(), d, geo, Collections.unmodifiableList(anims), tex);
    }

    public static Collection<ModelLibrary.Model> all() {
        return Collections.unmodifiableCollection(MODELS.values());
    }

    public static ModelLibrary.Model byKey(String key) {
        return key == null ? null : MODELS.get(key);
    }

    public static ModelLibrary.Model find(String project, String name) {
        if (name == null) {
            return null;
        } else {
            String n = name.trim();
            if (n.startsWith("spraute:")) {
                n = n.substring(8);
            }

            ModelLibrary.Model m = MODELS.get(n);
            if (m != null) {
                return m;
            } else {
                if (project != null) {
                    m = MODELS.get(project + "/" + n);
                    if (m != null) {
                        return m;
                    }
                }

                for (ModelLibrary.Model o : MODELS.values()) {
                    if (o.name().equalsIgnoreCase(n)) {
                        return o;
                    }
                }

                return null;
            }
        }
    }

    public static List<String> animations(ModelLibrary.Model m) {
        List<String> out = new ArrayList<>();
        if (m == null) {
            return out;
        } else {
            for (Path f : m.animFiles()) {
                SpAnimationParser.AnimationSet s = parsedAnims(f);
                if (s != null) {
                    for (String k : s.names()) {
                        if (!SpAnimationParser.isMetaClipName(k)) {
                            out.add(k);
                        }
                    }
                }
            }

            Collections.sort(out);
            return out;
        }
    }

    public static Optional<Animation> animation(ModelLibrary.Model m, String name) {
        if (m != null && name != null) {
            String n = name.trim();
            Animation loose = null;

            for (Path f : m.animFiles()) {
                SpAnimationParser.AnimationSet s = parsedAnims(f);
                if (s != null) {
                    for (String k : s.names()) {
                        SpAnimationParser.AnimationClip clip = s.get(k);
                        if (clip != null) {
                            if (k.equals(n)) {
                                return Optional.of(new Animation("spraute:" + m.key() + "/" + k, clip));
                            }

                            if (loose == null && (k.endsWith("." + n) || k.endsWith("_" + n) || k.equalsIgnoreCase(n))) {
                                loose = new Animation("spraute:" + m.key() + "/" + k, clip);
                            }
                        }
                    }
                }
            }

            return Optional.ofNullable(loose);
        } else {
            return Optional.empty();
        }
    }

    private static String clipOf(SpAnimationParser.AnimationSet set, String file) {
        List<String> keys = new ArrayList<>(set.names());
        keys.removeIf(SpAnimationParser::isMetaClipName);
        if (keys.isEmpty()) {
            return null;
        } else {
            Collections.sort(keys);

            for (String k : keys) {
                if (k.equalsIgnoreCase(file) || k.endsWith("." + file) || k.endsWith("_" + file)) {
                    return k;
                }
            }

            return keys.get(0);
        }
    }

    private static Optional<Animation> projectClip(ModelLibrary.AnimFile f, SpAnimationParser.AnimationSet set, String clip) {
        SpAnimationParser.AnimationClip c = clip == null ? null : set.get(clip);
        return c == null ? Optional.empty() : Optional.of(new Animation("project:" + f.project() + "/" + f.name() + "!" + clip, c));
    }

    public static synchronized Optional<Animation> projectAnimation(String project, String name) {
        if (name != null && !name.isBlank()) {
            String n = name.trim();
            if (n.startsWith("project:")) {
                String s = n.substring("project:".length());
                int slash = s.indexOf(47);
                int bang = s.lastIndexOf(33);
                if (slash > 0 && bang > slash) {
                    for (ModelLibrary.AnimFile f : ANIM_FILES) {
                        if (f.project().equals(s.substring(0, slash)) && f.name().equals(s.substring(slash + 1, bang))) {
                            SpAnimationParser.AnimationSet set = parsedAnims(f.path());
                            if (set != null) {
                                return projectClip(f, set, s.substring(bang + 1));
                            }
                        }
                    }

                    return Optional.empty();
                } else {
                    return Optional.empty();
                }
            } else {
                List<ModelLibrary.AnimFile> files = new ArrayList<>(ANIM_FILES);
                if (project != null) {
                    files.sort((a, b) -> Boolean.compare(!a.project().equals(project), !b.project().equals(project)));
                }

                int bang = n.indexOf(33);
                String file = bang >= 0 ? n.substring(0, bang) : n;

                for (ModelLibrary.AnimFile fx : files) {
                    if (fx.name().equalsIgnoreCase(file)) {
                        SpAnimationParser.AnimationSet set = parsedAnims(fx.path());
                        if (set != null) {
                            return projectClip(fx, set, bang >= 0 ? n.substring(bang + 1) : clipOf(set, fx.name()));
                        }
                    }
                }

                if (bang >= 0) {
                    return Optional.empty();
                } else {
                    for (ModelLibrary.AnimFile fxx : files) {
                        SpAnimationParser.AnimationSet set = parsedAnims(fxx.path());
                        if (set != null) {
                            for (String k : set.names()) {
                                if (!SpAnimationParser.isMetaClipName(k)
                                    && (k.equals(n) || k.endsWith("." + n) || k.endsWith("_" + n) || k.equalsIgnoreCase(n))) {
                                    return projectClip(fxx, set, k);
                                }
                            }
                        }
                    }

                    return Optional.empty();
                }
            }
        } else {
            return Optional.empty();
        }
    }

    public static synchronized List<ModelLibrary.ProjectAnim> projectAnimations(String project) {
        List<ModelLibrary.ProjectAnim> out = new ArrayList<>();

        for (ModelLibrary.AnimFile f : ANIM_FILES) {
            SpAnimationParser.AnimationSet set = parsedAnims(f.path());
            if (set != null) {
                List<String> keys = new ArrayList<>(set.names());
                keys.removeIf(SpAnimationParser::isMetaClipName);
                Collections.sort(keys);

                for (String k : keys) {
                    SpAnimationParser.AnimationClip c = set.get(k);
                    if (c != null) {
                        String shortId = keys.size() == 1 ? f.name() : f.name() + "!" + k;
                        out.add(
                            new ModelLibrary.ProjectAnim(
                                f.project(), f.name(), k, shortId, "project:" + f.project() + "/" + f.name() + "!" + k, c.getLengthSec(), c.isLoop()
                            )
                        );
                    }
                }
            }
        }

        if (project != null) {
            out.sort((a, b) -> Boolean.compare(!a.project().equals(project), !b.project().equals(project)));
        }

        return out;
    }

    public static Optional<Animation> resolve(String id) {
        if (id == null) {
            return Optional.empty();
        } else {
            String s = id.startsWith("spraute:") ? id.substring(8) : id;
            int slash = s.lastIndexOf(47);
            if (slash <= 0) {
                return Optional.empty();
            } else {
                ModelLibrary.Model m = find(null, s.substring(0, slash));
                return m == null ? Optional.empty() : animation(m, s.substring(slash + 1));
            }
        }
    }

    public record AnimFile(String project, String name, Path path) {
    }

    public record Model(String project, String name, Path dir, Path geoFile, List<Path> animFiles, Path texture) {
        public String key() {
            return this.project + "/" + this.name;
        }

        public ResourceLocation textureId() {
            return new ResourceLocation("spraute_engine", "textures/models/" + Images.slug(this.project) + "/" + Images.slug(this.name) + ".png");
        }

        public SpGeoModel geometry() {
            return ModelLibrary.parsedGeo(this.geoFile);
        }
    }

    public record ProjectAnim(String project, String file, String clip, String shortId, String id, double seconds, boolean loops) {
    }

    private record Stamped<T>(long stamp, T value) {
    }
}
