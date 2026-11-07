package org.sprautestudio.spraute_engine.export;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.visual.BlockCodegen;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.general.GeneralLang;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.zone.Zones;

public final class BundledProjects {
    private static final String MARK = ".spraute_bundle";
    private static final String ENGINE_FILE = "engine.json";
    private static final List<BundledProjects.Bundle> BUNDLES = new ArrayList<>();
    private static final Map<String, BundledProjects.Menu> MENUS = new LinkedHashMap<>();
    private static boolean worldWaiting;

    private BundledProjects() {
    }

    public static List<BundledProjects.Bundle> all() {
        return List.copyOf(BUNDLES);
    }

    public static boolean editorHidden() {
        for (BundledProjects.Bundle b : BUNDLES) {
            if (!b.authored() && !b.editor()) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPlayersGame() {
        for (BundledProjects.Bundle b : BUNDLES) {
            if (!b.authored()) {
                return true;
            }
        }

        return false;
    }

    public static void install() {
        for (Path manifest : Platform.get().modFiles("META-INF", "spraute_project.json")) {
            try {
                install(manifest);
            } catch (RuntimeException | IOException var3) {
                SprauteEngine.LOGGER.error("Spraute: cannot unpack the project of {}", manifest, var3);
            }
        }

        BUNDLES.sort(Comparator.comparing(BundledProjects.Bundle::id));

        for (BundledProjects.Bundle b : BUNDLES) {
            readMenus(b);
        }
    }

    private static void install(Path manifest) throws IOException {
        Map<String, String> m = ModExport.readManifest(Files.readString(manifest, StandardCharsets.UTF_8));
        String folder = m.get("folder");
        if (!folder.contains("/") && !folder.contains("\\") && !folder.contains("..")) {
            Path projects = Platform.get().gameDir().resolve("spraute").resolve("projects");
            Path dir = projects.resolve(folder);
            Path mark = dir.resolve(".spraute_bundle");
            boolean authored = Files.isDirectory(dir) && !Files.exists(mark);
            if (authored) {
                SprauteEngine.LOGGER
                    .info("Spraute: the mod {} holds the project {}, and this computer has its own: that one is used", m.get("id"), m.get("project"));
            } else if (!Files.exists(mark) || !Files.readString(mark, StandardCharsets.UTF_8).trim().equals(m.get("hash"))) {
                unpack(manifest.getParent().getParent().resolve("spraute_project"), projects, folder);
                Files.writeString(mark, m.get("hash"), StandardCharsets.UTF_8);
                SprauteEngine.LOGGER.info("Spraute: unpacked the project {} {} of the mod {}", m.get("project"), m.get("version"), m.get("id"));
            }

            BUNDLES.add(new BundledProjects.Bundle(m.get("id"), m.get("project"), m.get("version"), dir, authored, Boolean.parseBoolean(m.get("editor"))));
        } else {
            throw new IOException("Неверное имя папки проекта: " + folder);
        }
    }

    private static void unpack(Path source, Path projects, String folder) throws IOException {
        Path temp = projects.resolve("." + folder + ".unpacking");
        Path target = projects.resolve(folder);
        delete(temp);
        Files.createDirectories(temp);

        try (Stream<Path> all = Files.walk(source)) {
            for (Path f : (Iterable<Path>) all.filter(x$0 -> Files.isRegularFile(x$0))::iterator) {
                String rel = source.relativize(f).toString().replace('\\', '/');
                Path out = temp.resolve(rel).normalize();
                if (!out.startsWith(temp)) {
                    throw new IOException("Файл вне проекта: " + rel);
                }

                Files.createDirectories(out.getParent());

                try (InputStream in = Files.newInputStream(f)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        delete(target);
        Files.move(temp, target);
    }

    private static void delete(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> all = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) all.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(p);
                }
            }
        }
    }

    private static void readMenus(BundledProjects.Bundle b) {
        if (!b.authored()) {
            EngineConfig cfg = config(b);
            if (cfg != null) {
                cfg.layoutJson.forEach((role, json) -> {
                    if (role.startsWith("menu:") && json != null && !json.isBlank()) {
                        MENUS.put(role, new BundledProjects.Menu(b.project(), json));
                    }
                });
            }
        }
    }

    private static EngineConfig config(BundledProjects.Bundle b) {
        Path f = b.dir().resolve("engine.json");
        if (!Files.exists(f)) {
            return null;
        } else {
            try {
                return EngineConfig.fromJson(Files.readString(f, StandardCharsets.UTF_8));
            } catch (RuntimeException | IOException var3) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read the settings of the project {}", b.project(), var3);
                return null;
            }
        }
    }

    public static BundledProjects.Menu menu(String role) {
        return MENUS.get(role);
    }

    public static void worldOpened() {
        worldWaiting = !BUNDLES.isEmpty();
    }

    public static void tick(MinecraftServer server) {
        if (worldWaiting) {
            worldWaiting = false;

            for (BundledProjects.Bundle b : BUNDLES) {
                if (!b.authored()) {
                    try {
                        apply(server, b);
                    } catch (RuntimeException | IOException var4) {
                        SprauteEngine.LOGGER.error("Spraute: cannot put the project {} into the world", b.project(), var4);
                    }
                }
            }
        }
    }

    private static void apply(MinecraftServer server, BundledProjects.Bundle b) throws IOException {
        Project project = Project.openReadOnly(b.dir());
        SprauteRuntime runtime = SprauteRuntime.get();
        EngineConfig cfg = config(b);
        if (cfg != null) {
            cfg.project = b.project();
            if (!cfg.toJson().equals(EngineServer.config().toJson())) {
                EngineServer.set(server, cfg);
            }
        }

        runtime.putGuis(b.project(), project.deploySources());
        Zones.sourcesSaved(b.project());
        BundledProjects.Stories stories = stories(project);

        for (String file : stories.broken()) {
            SprauteEngine.LOGGER.error("Spraute: {} of the project {} has errors: not loaded", file, b.project());
        }

        Set<Integer> ids = new HashSet<>();

        for (String text : stories.texts()) {
            SprauteRuntime.DeployResult r = runtime.deploy(b.project(), text);
            if (r.ok()) {
                ids.add(r.id());
            } else {
                SprauteEngine.LOGGER.error("Spraute: a story of the project {} is not loaded: {}", b.project(), r.messages());
            }
        }

        runtime.keepOnly(b.project(), ids);
        ProjectContent.copyLocal(server, b.project(), b.dir(), () -> {});
        SprauteEngine.LOGGER.info("Spraute: the project {} {} is in this world: {} stories", b.project(), b.version(), ids.size());
    }

    public static BundledProjects.Stories stories(Project project) {
        List<String> texts = new ArrayList<>();
        List<String> broken = new ArrayList<>();

        for (Project.FileRef f : project.files()) {
            if (f.kind() == Project.Kind.SCRIPT || f.kind() == Project.Kind.BLOCKS) {
                try {
                    String text = project.read(f);
                    boolean bad;
                    if (f.kind() == Project.Kind.BLOCKS) {
                        BlockCodegen.Result r = BlockCodegen.generate(VisualScript.fromJson(text));
                        text = r.text;
                        bad = r.hasErrors();
                    } else {
                        bad = Analyzer.analyze(text).hasErrors();
                    }

                    if (bad) {
                        broken.add(f.fileName());
                    } else if (!GeneralLang.isGeneral(text)) {
                        texts.add(text);
                    }
                } catch (RuntimeException | IOException var8) {
                    broken.add(f.fileName());
                }
            }
        }

        return new BundledProjects.Stories(texts, broken);
    }

    public record Bundle(String id, String project, String version, Path dir, boolean authored, boolean editor) {
    }

    public record Menu(String project, String json) {
    }

    public record Stories(List<String> texts, List<String> broken) {
    }
}
