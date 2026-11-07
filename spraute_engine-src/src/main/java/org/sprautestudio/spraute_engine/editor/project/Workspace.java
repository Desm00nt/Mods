package org.sprautestudio.spraute_engine.editor.project;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.SprauteEngine;

public final class Workspace {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Workspace.Settings settings;

    public static Path root() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("spraute");
    }

    public static Path projectsDir() {
        return root().resolve("projects");
    }

    public static Workspace.Settings settings() {
        if (settings == null) {
            Path f = root().resolve("settings.json");

            try {
                if (Files.exists(f)) {
                    settings = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Workspace.Settings.class);
                }
            } catch (Exception var2) {
                SprauteEngine.LOGGER.warn("Failed to read Spraute settings", (Throwable)var2);
            }

            if (settings == null) {
                settings = new Workspace.Settings();
            }
        }

        return settings;
    }

    public static void saveSettings() {
        try {
            Files.createDirectories(root());
            Files.writeString(root().resolve("settings.json"), GSON.toJson(settings()), StandardCharsets.UTF_8);
        } catch (IOException var1) {
            SprauteEngine.LOGGER.warn("Failed to save Spraute settings", (Throwable)var1);
        }
    }

    public static List<Workspace.ProjectInfo> projects() {
        List<Workspace.ProjectInfo> list = new ArrayList<>();
        Path dir = projectsDir();
        if (!Files.isDirectory(dir)) {
            return list;
        } else {
            try (Stream<Path> s = Files.list(dir)) {
                for (Path p : (Iterable<Path>) s.filter(x$0 -> Files.isDirectory(x$0))::iterator) {
                    try {
                        Project pr = Project.openReadOnly(p);
                        list.add(new Workspace.ProjectInfo(pr.name(), p, pr.meta.opened, pr.files().size()));
                    } catch (Exception var7) {
                        list.add(new Workspace.ProjectInfo(p.getFileName().toString(), p, 0L, 0));
                    }
                }
            } catch (IOException var9) {
                SprauteEngine.LOGGER.warn("Failed to list projects", (Throwable)var9);
            }

            list.sort(Comparator.comparingLong(Workspace.ProjectInfo::opened).reversed());
            return list;
        }
    }

    public static Project createProject(String name) throws IOException {
        Path dir = projectsDir().resolve(Project.sanitize(name));
        if (Files.exists(dir)) {
            throw new IOException("Проект с таким именем уже существует");
        } else {
            return Project.create(dir, name);
        }
    }

    public static void deleteProject(Path dir) throws IOException {
        if (!dir.startsWith(projectsDir())) {
            throw new IOException("Not a project directory");
        } else {
            try (Stream<Path> s = Files.walk(dir)) {
                for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    public record ProjectInfo(String name, Path dir, long opened, int files) {
    }

    public static final class Settings {
        public int uiScale;
        public int uiPercent = -1;
        public boolean minimap = true;
        public String lastProject;
        public int sidebarW = 200;
        public int rightW = 250;
        public int bottomH = 170;
        public boolean showSidebar = true;
        public boolean showRight = true;
        public boolean showBottom = true;
        public boolean editorMode;
        public boolean showZones = true;
        public boolean debugPlate = true;
        public boolean mcp = true;
        public int mcpPort = 25599;

        public double physicalScale() {
            if (this.uiPercent < 0) {
                this.uiPercent = this.uiScale > 0 ? this.uiScale * 50 : 0;
            }

            return this.uiPercent <= 0 ? 0.0 : this.uiPercent / 50.0;
        }
    }
}
