package org.sprautestudio.spraute_engine.editor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.addon.Addons;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.export.BundledProjects;
import org.sprautestudio.spraute_engine.export.ModExport;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.Label;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

final class ExportDialog {
    private static final String ENGINE = "spraute_engine";
    private static final String SETTINGS_FILE = ".export.json";
    private static final String FABRIC_API = "fabric-api";
    private static final Gson GSON = new Gson();

    private ExportDialog() {
    }

    static void show(Project project, Consumer<String> onError, Consumer<String> onDone) {
        BundledProjects.Stories stories = BundledProjects.stories(project);
        if (!stories.broken().isEmpty()) {
            Dialog.info(
                "В проекте есть ошибки", "Сначала исправьте эти файлы: иначе в моде эти сюжеты не заработают.\n\n" + String.join("\n", stories.broken())
            );
        } else {
            boolean canNest = Platform.get().modJar("spraute_engine") != null;
            JsonObject saved = settings(project);
            ExportDialog.Mode[] mode = new ExportDialog.Mode[]{canNest ? ExportDialog.Mode.INSIDE : ExportDialog.Mode.BESIDE};
            Set<String> addons = new HashSet<>();
            List<Addons.Info> available = new ArrayList<>();

            for (Addons.Info a : Addons.loaded()) {
                if (a.error() == null && Platform.get().modJar(a.id()) != null) {
                    available.add(a);
                    addons.add(a.id());
                }
            }

            Dialog d = new Dialog("Экспорт как мод", Icon.UPLOAD).width(Math.min(560, UI.get() != null ? UI.get().vw - 30 : 560));
            d.body.add(new Label("Получится обычный мод для папки mods: всё запустится само.").color(-6577995));
            TextField name = field(d, "Название", value(saved, "name", project.name()));
            TextField id = field(d, "Id мода (латиницей)", value(saved, "id", ModExport.suggestId(project.name())));
            TextField version = field(d, "Версия", value(saved, "version", "1.0.0"));
            TextField author = field(d, "Автор", value(saved, "author", ""));
            TextField description = field(d, "Описание", value(saved, "description", ""));
            boolean[] editor = new boolean[]{saved.has("editor") && saved.get("editor").getAsBoolean()};
            d.body.add(new Checkbox("Оставить игрокам редактор (клавиша и команда /editor)", () -> editor[0], on -> editor[0] = on));
            d.body.add(new Label("Что положить в файл").color(-9933435));
            if (canNest) {
                d.body.add(new Dropdown<>(() -> List.of(ExportDialog.Mode.values()), () -> mode[0], m -> m.title, m -> mode[0] = m));

                for (Addons.Info a : available) {
                    d.body.add(new Checkbox("Аддон «" + a.name() + "» " + a.version(), () -> addons.contains(a.id()), on -> {
                        if (on) {
                            addons.add(a.id());
                        } else {
                            addons.remove(a.id());
                        }
                    }));
                }
            } else {
                d.body
                    .add(
                        new Label(
                                "Игра запущена из разработки, а не из готового jar: положить движок внутрь нельзя. Будет мод только с проектом: движок ставится отдельно."
                            )
                            .color(-870325)
                    );
            }

            d.addButton("Отмена", Button.Style.DEFAULT, d::close);
            d.addButton("Экспортировать", Button.Style.PRIMARY, () -> {
                String bad = ModExport.checkId(id.text().trim());
                if (bad == null) {
                    bad = ModExport.checkVersion(version.text().trim());
                }

                if (bad == null && name.text().isBlank()) {
                    bad = "Введите название";
                }

                if (bad != null) {
                    onError.accept(bad);
                } else {
                    ModExport.Spec spec = new ModExport.Spec();
                    spec.id = id.text().trim();
                    spec.name = name.text().trim();
                    spec.version = version.text().trim();
                    spec.author = author.text().trim();
                    spec.description = description.text().trim();
                    spec.project = project.name();
                    spec.projectDir = project.dir;
                    spec.engineVersion = Platform.get().modVersion("spraute_engine");
                    spec.editor = editor[0];
                    String suffix = "";
                    if (mode[0] == ExportDialog.Mode.INSIDE) {
                        spec.loader = Platform.get().isModLoaded("fabricloader") ? ModExport.Loader.FABRIC : ModExport.Loader.FORGE;
                        spec.minecraft = Mc.gameVersion();
                        spec.nested.add(new ModExport.Nested("spraute_engine", spec.engineVersion, Platform.get().modJar("spraute_engine")));

                        for (Addons.Info ax : available) {
                            if (addons.contains(ax.id())) {
                                spec.nested.add(new ModExport.Nested(ax.id(), ax.version(), Platform.get().modJar(ax.id())));
                            }
                        }

                        Path fabricApi = Platform.get().modJar("fabric-api");
                        if (spec.loader == ModExport.Loader.FABRIC && fabricApi != null) {
                            spec.nested.add(new ModExport.Nested("fabric-api", Platform.get().modVersion("fabric-api"), fabricApi));
                        }

                        suffix = "-" + spec.loader.name().toLowerCase() + "-" + spec.minecraft;
                    }

                    save(project, spec);
                    d.close();
                    run(spec, Workspace.root().resolve("exports").resolve(spec.id + "-" + spec.version + suffix + ".jar"), onError, onDone);
                }
            });
            d.open();
        }
    }

    private static void run(ModExport.Spec spec, Path out, Consumer<String> onError, Consumer<String> onDone) {
        Thread t = new Thread(
            () -> {
                try {
                    ModExport.Result r = ModExport.write(spec, out);
                    long size = Files.size(r.jar());
                    Minecraft.getInstance()
                        .execute(
                            () -> {
                                onDone.accept("Мод готов: " + r.jar().getFileName() + ", " + Math.max(1L, size >> 20) + " МБ");
                                Dialog.confirm(
                                    "Мод готов",
                                    r.jar().toAbsolutePath() + "\n\nФайлов проекта: " + r.files() + ". Размер: " + Math.max(1L, size >> 20) + " МБ.",
                                    "Открыть папку",
                                    false,
                                    () -> Util.getPlatform().openFile(r.jar().toAbsolutePath().getParent().toFile())
                                );
                            }
                        );
                } catch (RuntimeException | IOException var7) {
                    SprauteEngine.LOGGER.error("Spraute: cannot export the mod {}", spec.id, var7);
                    Minecraft.getInstance().execute(() -> onError.accept("Не удалось экспортировать: " + var7.getMessage()));
                }
            },
            "Spraute mod export"
        );
        t.setDaemon(true);
        t.start();
    }

    private static TextField field(Dialog d, String label, String value) {
        d.body.add(new Label(label).color(-9933435));
        TextField f = new TextField();
        f.setText(value);
        d.body.add(f);
        return f;
    }

    private static String value(JsonObject o, String key, String fallback) {
        return o.has(key) ? o.get(key).getAsString() : fallback;
    }

    private static JsonObject settings(Project project) {
        try {
            Path f = project.dir.resolve(".export.json");
            JsonObject o = Files.exists(f) ? GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), JsonObject.class) : null;
            return o != null ? o : new JsonObject();
        } catch (RuntimeException | IOException var3) {
            return new JsonObject();
        }
    }

    private static void save(Project project, ModExport.Spec spec) {
        JsonObject o = new JsonObject();
        o.addProperty("id", spec.id);
        o.addProperty("name", spec.name);
        o.addProperty("version", spec.version);
        o.addProperty("author", spec.author);
        o.addProperty("description", spec.description);
        o.addProperty("editor", spec.editor);

        try {
            Files.writeString(project.dir.resolve(".export.json"), GSON.toJson((JsonElement)o), StandardCharsets.UTF_8);
        } catch (IOException var4) {
            SprauteEngine.LOGGER.warn("Spraute: cannot save the export settings", (Throwable)var4);
        }
    }

    private static enum Mode {
        INSIDE("Всё внутри: движок и аддоны в одном файле, для этой игры"),
        BESIDE("Только проект: для любого загрузчика и версии, движок ставится отдельно");

        final String title;

        private Mode(String title) {
            this.title = title;
        }
    }
}
