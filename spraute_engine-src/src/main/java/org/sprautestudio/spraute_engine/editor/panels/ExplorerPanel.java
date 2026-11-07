package org.sprautestudio.spraute_engine.editor.panels;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.editor.assets.AssetsTab;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.TreeView;

public class ExplorerPanel extends Widget {
    private final TreeView tree = new TreeView();
    private final Button addScript;
    private final Button addBlocks;
    private final Button refresh;
    private final Button addImage;
    private final ExplorerPanel.Actions actions;
    private Project project;
    private Widget extra;
    private static final String GENERALS = "Общие скрипты";
    private static final String STORIES = "Сюжеты";
    private static final String WORLD = "Мир и постановка";
    private static final String SYSTEMS = "Игровые системы";
    private static final String LOOK = "Интерфейс";
    private static final String RESOURCES = "Персонажи и ресурсы";
    private final Set<String> expandedKeys = new HashSet<>();
    private final Set<String> collapsedKeys = new HashSet<>();

    public void setExtra(Widget w) {
        if (w != this.extra) {
            if (this.extra != null) {
                this.remove(this.extra);
            }

            this.extra = w;
            if (this.extra != null) {
                this.add(this.extra);
            }

            this.layout();
        }
    }

    public ExplorerPanel(ExplorerPanel.Actions actions) {
        this.actions = actions;
        this.add(this.tree);
        this.addScript = this.add(Button.icon(Icon.CODE, "Новый текстовый скрипт (Ctrl+N)", actions::newScript));
        this.addBlocks = this.add(Button.icon(Icon.GRAPH, "Новый блочный скрипт", actions::newBlocks));
        this.refresh = this.add(Button.icon(Icon.REFRESH, "Обновить", this::reload));
        this.addImage = this.add(Button.icon(Icon.IMAGE, "Добавить картинки (или перетащите файлы в окно)", actions::importImages));
        this.tree.background = -14934490;
        this.tree.onOpen = n -> {
            if (n.data instanceof Project.FileRef f) {
                actions.open(f);
            }

            if (n.data instanceof AssetsTab.Mode m) {
                actions.openAssets(m);
            }

            if (n.data instanceof Project.ImageRef img) {
                actions.openImage(img);
            }

            if (n.data instanceof Project.SoundRef snd) {
                actions.playSound(snd);
            }

            if (n.data instanceof ExplorerPanel.EngineNode) {
                actions.openEngine();
            }

            if (n.data instanceof ExplorerPanel.RecipesNode) {
                actions.openRecipes();
            }

            if (n.data instanceof ExplorerPanel.ModsNode) {
                actions.openMods();
            }

            if (n.data instanceof Project.ModelRef m) {
                actions.openModel(m);
            }

            if (n.data instanceof Project.EntityRef en) {
                actions.openEntity(en);
            }
        };
        this.tree.onNodeContext = (n, mx, my) -> {
            PopupMenu pm = new PopupMenu();
            if (n == null || !(n.data instanceof ExplorerPanel.ImagesFolder) && !(n.data instanceof Project.ImageRef)) {
                if (n == null || !(n.data instanceof ExplorerPanel.VideosFolder) && !(n.data instanceof Project.VideoRef)) {
                    if (n == null || !(n.data instanceof ExplorerPanel.SoundsFolder) && !(n.data instanceof Project.SoundRef)) {
                        if (n == null || !(n.data instanceof ExplorerPanel.EntitiesFolder) && !(n.data instanceof Project.EntityRef)) {
                            if (n == null || !(n.data instanceof ExplorerPanel.ModelsFolder) && !(n.data instanceof Project.ModelRef)) {
                                if (n == null || !(n.data instanceof ExplorerPanel.AnimationsFolder) && !(n.data instanceof ExplorerPanel.AnimationFile)) {
                                    if (n != null && n.data instanceof AssetsTab.Mode m) {
                                        pm.item("Открыть", Icon.CUBE, null, () -> actions.openAssets(m));
                                        pm.separator();
                                        this.otherSections(pm, null);
                                        pm.show(mx, my);
                                    } else {
                                        if (n != null && (n.data instanceof ExplorerPanel.Folder || n.data instanceof ExplorerPanel.SubFolder)) {
                                            Project.Kind kind = n.data instanceof ExplorerPanel.Folder fo
                                                ? fo.kind()
                                                : ((ExplorerPanel.SubFolder)n.data).kind();
                                            Path dir = n.data instanceof ExplorerPanel.SubFolder sf ? sf.dir() : this.project.kindDir(kind);
                                            this.add(pm, this.newFile(kind), n.data instanceof ExplorerPanel.SubFolder ? dir : null);
                                            pm.separator();
                                            pm.item("Новая папка…", Icon.FOLDER, null, () -> this.newFolder(dir)).color(-11746422);
                                            if (n.data instanceof ExplorerPanel.SubFolder sf) {
                                                pm.item("Переименовать папку", Icon.EDIT, null, () -> this.renameFolder(sf.dir()));
                                                pm.item("Удалить папку (пустую)", Icon.TRASH, null, () -> this.deleteFolder(sf.dir())).color(-1023878);
                                            }

                                            pm.item("Открыть в проводнике Windows", Icon.FOLDER, null, () -> {
                                                try {
                                                    Files.createDirectories(dir);
                                                } catch (IOException var2x) {
                                                }

                                                Util.getPlatform().openFile(dir.toFile());
                                            });
                                            pm.separator();
                                        }

                                        if (n != null && n.data instanceof Project.FileRef f) {
                                            pm.item("Открыть", Icon.FILE, null, () -> actions.open(f));
                                            pm.item("Загрузить в игру", Icon.UPLOAD, null, () -> actions.deploy(f, false));
                                            pm.item("Загрузить и запустить", Icon.PLAY, null, () -> actions.deploy(f, true)).color(-11746422);
                                            pm.separator();
                                            pm.item("Переименовать", Icon.EDIT, null, () -> actions.rename(f));
                                            pm.item("Дублировать", Icon.COPY, null, () -> actions.duplicate(f));
                                            pm.item("Переместить в папку…", Icon.FOLDER, null, () -> this.moveMenu(f, mx, my));
                                            pm.item("Удалить", Icon.TRASH, null, () -> actions.delete(f)).color(-1023878);
                                            pm.separator();
                                            if (this.project.isGeneral(f)) {
                                                this.addGenerals(pm);
                                            } else {
                                                Path here = f.path().getParent();
                                                this.add(pm, this.newFile(f.kind()), here.equals(this.project.kindDir(f.kind())) ? null : here);
                                            }

                                            pm.separator();
                                        }

                                        String own = n != null && n.data instanceof ExplorerPanel.Group g ? g.name() : null;
                                        if (own != null) {
                                            for (ExplorerPanel.NewFile f : this.section(own)) {
                                                this.add(pm, f, null);
                                            }

                                            pm.separator();
                                        } else if (n != null && n.label.equals("Общие скрипты")) {
                                            this.addGenerals(pm);
                                            pm.separator();
                                        }

                                        this.otherSections(pm, own);
                                        pm.show(mx, my);
                                    }
                                } else {
                                    if (n.data instanceof ExplorerPanel.AnimationFile a) {
                                        String id = a.path().getFileName().toString().replaceAll("(?i)\\.animations?\\.json$", "");
                                        pm.item("Копировать имя для anim", Icon.COPY, null, () -> Minecraft.getInstance().keyboardHandler.setClipboard(id));
                                        pm.separator();
                                        pm.item("Удалить", Icon.TRASH, null, () -> actions.deleteAnimation(a.path())).color(-1023878);
                                        pm.separator();
                                    }

                                    pm.item("Добавить анимации (Blockbench)…", Icon.PLUS, null, actions::importAnimations).color(-11746422);
                                    pm.separator();
                                    this.otherSections(pm, null);
                                    pm.show(mx, my);
                                }
                            } else {
                                if (n.data instanceof Project.ModelRef m) {
                                    pm.item("Открыть", Icon.CUBE, null, () -> actions.openModel(m));
                                    pm.item(
                                        "Копировать id для персонажа",
                                        Icon.COPY,
                                        null,
                                        () -> Minecraft.getInstance().keyboardHandler.setClipboard("spraute:" + m.name())
                                    );
                                    pm.item("Папка модели", Icon.FOLDER, null, () -> Util.getPlatform().openFile(m.dir().toFile()));
                                    pm.separator();
                                    pm.item("Удалить", Icon.TRASH, null, () -> actions.deleteModel(m)).color(-1023878);
                                    pm.separator();
                                }

                                pm.item("Добавить файлы модели (Blockbench)…", Icon.PLUS, null, actions::importModels).color(-11746422);
                                pm.separator();
                                this.otherSections(pm, null);
                                pm.show(mx, my);
                            }
                        } else {
                            if (n.data instanceof Project.EntityRef en) {
                                pm.item("Открыть", Icon.USER, null, () -> actions.openEntity(en));
                                pm.item(
                                    "Копировать id для персонажа",
                                    Icon.COPY,
                                    null,
                                    () -> Minecraft.getInstance().keyboardHandler.setClipboard("spraute:" + en.name())
                                );
                                pm.separator();
                                pm.item("Удалить", Icon.TRASH, null, () -> actions.deleteEntity(en)).color(-1023878);
                                pm.separator();
                            }

                            pm.item("Новая сущность…", Icon.PLUS, null, actions::newEntity).color(-11746422);
                            pm.separator();
                            this.otherSections(pm, null);
                            pm.show(mx, my);
                        }
                    } else {
                        if (n.data instanceof Project.SoundRef snd) {
                            pm.item("Прослушать", Icon.PLAY, null, () -> actions.playSound(snd));
                            pm.item(
                                "Копировать команду",
                                Icon.COPY,
                                null,
                                () -> Minecraft.getInstance().keyboardHandler.setClipboard("sound \"" + snd.name() + "\"")
                            );
                            pm.separator();
                            pm.item("Переименовать", Icon.EDIT, null, () -> actions.renameSound(snd));
                            pm.item("Удалить", Icon.TRASH, null, () -> actions.deleteSound(snd)).color(-1023878);
                            pm.separator();
                        }

                        pm.item("Добавить звуки…", Icon.PLUS, null, actions::importSounds).color(-11746422);
                        pm.item("Остановить звуки", Icon.STOP, null, () -> ProjectSounds.stop(""));
                        pm.item("Открыть папку звуков", Icon.FOLDER, null, actions::openSoundsFolder);
                        pm.separator();
                        this.otherSections(pm, null);
                        pm.show(mx, my);
                    }
                } else {
                    if (n.data instanceof Project.VideoRef video) {
                        pm.item("Копировать имя", Icon.COPY, null, () -> Minecraft.getInstance().keyboardHandler.setClipboard(video.name()));
                        pm.separator();
                        pm.item("Переименовать", Icon.EDIT, null, () -> actions.renameVideo(video));
                        pm.item("Удалить", Icon.TRASH, null, () -> actions.deleteVideo(video)).color(-1023878);
                        pm.separator();
                    }

                    pm.item("Добавить видео…", Icon.PLUS, null, actions::importVideos).color(-11746422);
                    pm.item("Открыть папку видео", Icon.FOLDER, null, actions::openVideosFolder);
                    pm.separator();
                    this.otherSections(pm, null);
                    pm.show(mx, my);
                }
            } else {
                if (n.data instanceof Project.ImageRef img) {
                    pm.item("Открыть", Icon.IMAGE, null, () -> actions.openImage(img));
                    pm.item("Копировать имя", Icon.COPY, null, () -> Minecraft.getInstance().keyboardHandler.setClipboard(img.name()));
                    pm.separator();
                    pm.item("Сделать текстурой Spraute (64×64)", Icon.USER, null, () -> actions.stSkin(img));
                    pm.item("Переименовать", Icon.EDIT, null, () -> actions.renameImage(img));
                    pm.item("Удалить", Icon.TRASH, null, () -> actions.deleteImage(img)).color(-1023878);
                    pm.separator();
                }

                pm.item("Добавить картинки…", Icon.PLUS, null, actions::importImages).color(-11746422);
                pm.item("Новая картинка…", Icon.PLUS, null, actions::newImage);
                pm.item("Открыть папку картинок", Icon.FOLDER, null, actions::openImagesFolder);
                pm.separator();
                this.otherSections(pm, null);
                pm.show(mx, my);
            }
        };
    }

    private ExplorerPanel.NewFile newFile(Project.Kind kind) {
        return switch (kind) {
            case SCRIPT -> new ExplorerPanel.NewFile("Новый текстовый скрипт", Icon.CODE, -8213761, this.actions::newScript);
            case BLOCKS -> new ExplorerPanel.NewFile("Новый блочный скрипт", Icon.GRAPH, -870325, this.actions::newBlocks);
            case GUI -> new ExplorerPanel.NewFile("Новый интерфейс (GUI)", Icon.MENU, -10638858, this.actions::newGui);
            case CUTSCENE -> new ExplorerPanel.NewFile("Новая катсцена", Icon.FILM, -28838, this.actions::newCutscene);
            case EFFECT -> new ExplorerPanel.NewFile("Новый эффект", Icon.BOLT, -14006, this.actions::newEffect);
            case POST -> new ExplorerPanel.NewFile("Новый пост-эффект", Icon.IMAGE, -1934337, this.actions::newPost);
            case CAMERA -> new ExplorerPanel.NewFile("Новая камера игрока", Icon.FILM, -9706552, this.actions::newCamera);
            case STRUCTURE -> new ExplorerPanel.NewFile("Импорт постройки (.nbt, .schem, .litematic)…", Icon.CUBE, -2514842, this.actions::importStructures);
            case SHOP -> new ExplorerPanel.NewFile("Новый магазин", Icon.TAG, -8658806, this.actions::newShop);
            case QUEST -> new ExplorerPanel.NewFile("Новый квест", Icon.FLAG, -11686, this.actions::newQuest);
            case ZONE -> new ExplorerPanel.NewFile("Новая зона", Icon.PIN, -11549705, this.actions::newZone);
            case PATH -> new ExplorerPanel.NewFile("Новый путь", Icon.ARROW_RIGHT, -21965, this.actions::newPath);
            case FIGHT -> new ExplorerPanel.NewFile("Новый стиль боя", Icon.BOLT, -38037, this.actions::newFight);
            case STEALTH -> new ExplorerPanel.NewFile("Новый стиль стелса", Icon.SEARCH, -43691, this.actions::newStealth);
            case SKILLS -> new ExplorerPanel.NewFile("Новая система прокачки", Icon.GRAPH, -5141249, this.actions::newSkills);
        };
    }

    private List<ExplorerPanel.NewFile> generals() {
        return List.of(
            new ExplorerPanel.NewFile("Новый общий скрипт", Icon.CODE, -7351921, () -> this.actions.newGeneral(false)),
            new ExplorerPanel.NewFile("Новый общий блочный скрипт", Icon.GRAPH, -7351921, () -> this.actions.newGeneral(true))
        );
    }

    private List<ExplorerPanel.NewFile> section(String name) {
        List<ExplorerPanel.NewFile> out = new ArrayList<>();
        switch (name) {
            case "Сюжеты":
                out.add(this.newFile(Project.Kind.SCRIPT));
                out.add(this.newFile(Project.Kind.BLOCKS));
                out.addAll(this.generals());
                break;
            case "Мир и постановка":
                for (Project.Kind k : List.of(
                    Project.Kind.CUTSCENE,
                    Project.Kind.ZONE,
                    Project.Kind.PATH,
                    Project.Kind.STRUCTURE,
                    Project.Kind.EFFECT,
                    Project.Kind.POST,
                    Project.Kind.CAMERA
                )) {
                    out.add(this.newFile(k));
                }
                break;
            case "Игровые системы":
                for (Project.Kind k : List.of(Project.Kind.QUEST, Project.Kind.SHOP, Project.Kind.FIGHT, Project.Kind.STEALTH, Project.Kind.SKILLS)) {
                    out.add(this.newFile(k));
                }
                break;
            case "Интерфейс":
                out.add(this.newFile(Project.Kind.GUI));
                break;
            case "Персонажи и ресурсы":
                out.add(new ExplorerPanel.NewFile("Новая сущность…", Icon.USER, -11544898, this.actions::newEntity));
                out.add(new ExplorerPanel.NewFile("Добавить файлы модели (Blockbench)…", Icon.CUBE, -12988235, this.actions::importModels));
                out.add(new ExplorerPanel.NewFile("Добавить анимации (Blockbench)…", Icon.FILM, -12988235, this.actions::importAnimations));
                out.add(new ExplorerPanel.NewFile("Добавить картинки…", Icon.IMAGE, -10638858, this.actions::importImages));
                out.add(new ExplorerPanel.NewFile("Новая картинка…", Icon.IMAGE, -10638858, this.actions::newImage));
                out.add(new ExplorerPanel.NewFile("Добавить звуки…", Icon.NOTE, -5141249, this.actions::importSounds));
                out.add(new ExplorerPanel.NewFile("Добавить видео…", Icon.PLAY, -32897, this.actions::importVideos));
        }

        Set<String> cut = new HashSet<>(List.of("Добавить файлы модели (Blockbench)…", "Добавить анимации (Blockbench)…"));

        for (Project.Kind k : List.of(
            Project.Kind.EFFECT, Project.Kind.POST, Project.Kind.STRUCTURE, Project.Kind.FIGHT, Project.Kind.STEALTH, Project.Kind.SKILLS
        )) {
            cut.add(this.newFile(k).title());
        }

        out.removeIf(f -> cut.contains(f.title()));
        return out;
    }

    private void add(PopupMenu pm, ExplorerPanel.NewFile f, Path dir) {
        pm.item(f.title(), f.icon(), null, () -> {
            this.actions.createIn(dir);
            f.make().run();
        }).color(f.color());
    }

    private void addGenerals(PopupMenu pm) {
        for (ExplorerPanel.NewFile f : this.generals()) {
            this.add(pm, f, null);
        }
    }

    private void otherSections(PopupMenu pm, String except) {
        Object[][] all = new Object[][]{
            {"Сюжеты", Icon.CODE, -8213761},
            {"Мир и постановка", Icon.FILM, -28838},
            {"Игровые системы", Icon.FLAG, -11686},
            {"Интерфейс", Icon.MENU, -10638858},
            {"Персонажи и ресурсы", Icon.USER, -11544898}
        };

        for (Object[] one : all) {
            String name = (String)one[0];
            if (!name.equals(except)) {
                pm.submenu(name, (Icon)one[1], sub -> {
                    for (ExplorerPanel.NewFile f : this.section(name)) {
                        this.add(sub, f, null);
                    }
                }).color((Integer)one[2]);
            }
        }

        pm.separator();
        pm.item("Справочник языка", Icon.BOOK, "F1", this.actions::openDocs);
    }

    private void newFolder(Path parent) {
        Dialog.prompt("Новая папка", "Имя папки", "новая папка", v -> {
            if (v.isBlank()) {
                return "Введите имя";
            } else if (v.matches(".*[\\\\/:*?\"<>|].*")) {
                return "Без символов \\ / : * ? \" < > |";
            } else {
                return Files.exists(parent.resolve(v.trim())) ? "Такая папка уже есть" : null;
            }
        }, v -> {
            try {
                Files.createDirectories(parent.resolve(v.trim()));
                this.expandedKeys.add("dir:" + parent.resolve(v.trim()));
                this.expandedKeys.add("dir:" + parent);
            } catch (IOException var4) {
            }

            this.reload();
        });
    }

    private void renameFolder(Path dir) {
        Dialog.prompt("Переименовать папку", "Новое имя", dir.getFileName().toString(), v -> {
            if (v.isBlank()) {
                return "Введите имя";
            } else if (v.matches(".*[\\\\/:*?\"<>|].*")) {
                return "Без символов \\ / : * ? \" < > |";
            } else {
                return Files.exists(dir.resolveSibling(v.trim())) ? "Такая папка уже есть" : null;
            }
        }, v -> {
            try {
                Files.move(dir, dir.resolveSibling(v.trim()));
            } catch (IOException var4) {
            }

            this.reload();
        });
    }

    private void deleteFolder(Path dir) {
        try {
            label54: {
                try (Stream<Path> s = Files.list(dir)) {
                    if (!s.findAny().isPresent()) {
                        Files.delete(dir);
                        break label54;
                    }

                    Dialog.confirm("Папка не пуста", "Сначала перенесите или удалите файлы из «" + dir.getFileName() + "».", "Понятно", false, () -> {});
                }

                return;
            }
        } catch (IOException var7) {
        }

        this.reload();
    }

    private void moveMenu(Project.FileRef f, int mx, int my) {
        PopupMenu pm = new PopupMenu();
        Path root = this.project.kindDir(f.kind());
        Path cur = f.path().getParent();
        pm.item("(корень) " + root.getFileName(), Icon.FOLDER, null, () -> this.actions.move(f, root)).checked(() -> cur.equals(root));

        for (Path d : this.project.subfolders(f.kind())) {
            String label = root.relativize(d).toString().replace('\\', '/');
            pm.item(label, Icon.FOLDER, null, () -> this.actions.move(f, d)).checked(() -> cur.equals(d));
        }

        pm.separator();
        pm.item("Новая папка…", Icon.PLUS, null, () -> this.newFolder(cur));
        pm.show(mx, my);
    }

    private static String key(Object data) {
        if (data instanceof ExplorerPanel.Folder f) {
            return "kind:" + f.kind();
        } else if (data instanceof ExplorerPanel.SubFolder s) {
            return "dir:" + s.dir();
        } else if (data instanceof ExplorerPanel.Group g) {
            return "grp:" + g.name();
        } else if (data instanceof ExplorerPanel.AssetsFolder) {
            return "asset";
        } else {
            return data == null ? "" : data.getClass().getSimpleName();
        }
    }

    private void remember(List<TreeView.Node> nodes) {
        for (TreeView.Node n : nodes) {
            if (n.data instanceof TreeView.Folder) {
                String k = key(n.data);
                if (n.expanded) {
                    this.expandedKeys.add(k);
                    this.collapsedKeys.remove(k);
                } else {
                    this.collapsedKeys.add(k);
                    this.expandedKeys.remove(k);
                }

                this.remember(n.children);
            }
        }
    }

    private void restore(List<TreeView.Node> nodes) {
        for (TreeView.Node n : nodes) {
            if (n.data instanceof TreeView.Folder) {
                String k = key(n.data);
                if (this.expandedKeys.contains(k)) {
                    n.expanded = true;
                } else if (this.collapsedKeys.contains(k)) {
                    n.expanded = false;
                } else {
                    n.expanded = false;
                }

                this.restore(n.children);
            }
        }
    }

    private static int countFiles(TreeView.Node n) {
        int c = 0;

        for (TreeView.Node ch : n.children) {
            c += ch.data instanceof Project.FileRef ? 1 : countFiles(ch);
        }

        return c;
    }

    private static TreeView.Node folderNode(Map<Path, TreeView.Node> made, TreeView.Node kindNode, Path root, Path dir, Project.Kind kind, int color) {
        if (dir != null && !dir.equals(root)) {
            TreeView.Node n = made.get(dir);
            if (n != null) {
                return n;
            } else {
                TreeView.Node parent = folderNode(made, kindNode, root, dir.getParent(), kind, color);
                n = new TreeView.Node(dir.getFileName().toString(), Icon.FOLDER, new ExplorerPanel.SubFolder(kind, dir));
                n.iconColor = color;
                parent.add(n);
                made.put(dir, n);
                return n;
            }
        } else {
            return kindNode;
        }
    }

    public void setProject(Project p) {
        this.project = p;
        this.reload();
    }

    public void reload() {
        if (this.project == null) {
            this.tree.setRoots(List.of());
        } else {
            List<TreeView.Node> roots = new ArrayList<>();
            TreeView.Node scripts = new TreeView.Node("Текстовые скрипты", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.SCRIPT));
            scripts.iconColor = -8213761;
            TreeView.Node blocks = new TreeView.Node("Блочные скрипты", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.BLOCKS));
            blocks.iconColor = -870325;
            TreeView.Node generals = new TreeView.Node("Общие скрипты", Icon.FOLDER, null);
            generals.iconColor = -7351921;
            TreeView.Node guis = new TreeView.Node("Интерфейсы", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.GUI));
            guis.iconColor = -10638858;
            TreeView.Node cutscenes = new TreeView.Node("Катсцены", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.CUTSCENE));
            cutscenes.iconColor = -28838;
            TreeView.Node effects = new TreeView.Node("Эффекты", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.EFFECT));
            effects.iconColor = -14006;
            TreeView.Node posts = new TreeView.Node("Пост-эффекты", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.POST));
            posts.iconColor = -1934337;
            TreeView.Node cameras = new TreeView.Node("Камеры", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.CAMERA));
            cameras.iconColor = -9706552;
            TreeView.Node structures = new TreeView.Node("Постройки", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.STRUCTURE));
            structures.iconColor = -2514842;
            TreeView.Node shops = new TreeView.Node("Магазины", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.SHOP));
            shops.iconColor = -8658806;
            TreeView.Node quests = new TreeView.Node("Квесты", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.QUEST));
            quests.iconColor = -11686;
            TreeView.Node zones = new TreeView.Node("Зоны", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.ZONE));
            zones.iconColor = -11549705;
            TreeView.Node fights = new TreeView.Node("Стили боя", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.FIGHT));
            fights.iconColor = -38037;
            TreeView.Node stealths = new TreeView.Node("Стили стелса", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.STEALTH));
            stealths.iconColor = -43691;
            TreeView.Node skills = new TreeView.Node("Прокачка", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.SKILLS));
            skills.iconColor = -5141249;
            TreeView.Node paths = new TreeView.Node("Пути", Icon.FOLDER, new ExplorerPanel.Folder(Project.Kind.PATH));
            paths.iconColor = -21965;
            TreeView.Node images = new TreeView.Node("Картинки", Icon.FOLDER, new ExplorerPanel.ImagesFolder());
            images.iconColor = -10638858;

            for (Project.ImageRef img : this.project.images()) {
                TreeView.Node n = new TreeView.Node(img.name(), Icon.IMAGE, img);
                n.iconColor = -10638858;
                images.add(n);
            }

            images.badge = String.valueOf(images.children.size());
            TreeView.Node ownSounds = new TreeView.Node("Свои звуки", Icon.FOLDER, new ExplorerPanel.SoundsFolder());
            ownSounds.iconColor = -5141249;

            for (Project.SoundRef snd : this.project.sounds()) {
                TreeView.Node n = new TreeView.Node(snd.name(), Icon.NOTE, snd);
                n.iconColor = -5141249;
                ownSounds.add(n);
            }

            ownSounds.badge = String.valueOf(ownSounds.children.size());
            TreeView.Node ownVideos = new TreeView.Node("Видео", Icon.FOLDER, new ExplorerPanel.VideosFolder());
            ownVideos.iconColor = -32897;

            for (Project.VideoRef video : this.project.videos()) {
                TreeView.Node n = new TreeView.Node(video.name(), Icon.PLAY, video);
                n.iconColor = -32897;
                ownVideos.add(n);
            }

            ownVideos.badge = String.valueOf(ownVideos.children.size());
            TreeView.Node entities = new TreeView.Node("Сущности", Icon.FOLDER, new ExplorerPanel.EntitiesFolder());
            entities.iconColor = -11544898;

            for (Project.EntityRef er : this.project.entities()) {
                TreeView.Node n = new TreeView.Node(er.name(), Icon.USER, er);
                n.iconColor = -11544898;
                entities.add(n);
            }

            entities.badge = String.valueOf(entities.children.size());
            TreeView.Node customModels = new TreeView.Node("Мои модели (Blockbench)", Icon.FOLDER, new ExplorerPanel.ModelsFolder());
            customModels.iconColor = -12988235;

            for (Project.ModelRef mr : this.project.models()) {
                TreeView.Node n = new TreeView.Node(mr.name(), Icon.CUBE, mr);
                n.iconColor = -12988235;
                customModels.add(n);
            }

            customModels.badge = String.valueOf(customModels.children.size());
            TreeView.Node ownAnimations = new TreeView.Node("Мои анимации", Icon.FOLDER, new ExplorerPanel.AnimationsFolder());
            ownAnimations.iconColor = -12988235;

            for (Path f : this.project.animationFiles()) {
                TreeView.Node n = new TreeView.Node(
                    f.getFileName().toString().replaceAll("(?i)\\.animations?\\.json$", ""), Icon.FILM, new ExplorerPanel.AnimationFile(f)
                );
                n.iconColor = -12988235;
                ownAnimations.add(n);
            }

            ownAnimations.badge = String.valueOf(ownAnimations.children.size());
            TreeView.Node assets = new TreeView.Node("Справочник игры", Icon.FOLDER, new ExplorerPanel.AssetsFolder());
            assets.iconColor = -28838;
            this.remember(this.tree.roots());
            TreeView.Node models = new TreeView.Node("Модели", Icon.CUBE, AssetsTab.Mode.MODELS);
            models.iconColor = -12988235;
            TreeView.Node anims = new TreeView.Node("Анимации", Icon.FILM, AssetsTab.Mode.ANIMATIONS);
            anims.iconColor = -28838;
            TreeView.Node sounds = new TreeView.Node("Звуки", Icon.NOTE, AssetsTab.Mode.SOUNDS);
            sounds.iconColor = -10638858;
            assets.add(models);
            assets.add(anims);
            assets.add(sounds);
            Map<Path, TreeView.Node> made = new HashMap<>();
            Map<Project.Kind, TreeView.Node> byKind = new EnumMap<>(Project.Kind.class);
            byKind.put(Project.Kind.SCRIPT, scripts);
            byKind.put(Project.Kind.BLOCKS, blocks);
            byKind.put(Project.Kind.GUI, guis);
            byKind.put(Project.Kind.CUTSCENE, cutscenes);
            byKind.put(Project.Kind.EFFECT, effects);
            byKind.put(Project.Kind.POST, posts);
            byKind.put(Project.Kind.CAMERA, cameras);
            byKind.put(Project.Kind.STRUCTURE, structures);
            byKind.put(Project.Kind.SHOP, shops);
            byKind.put(Project.Kind.QUEST, quests);
            byKind.put(Project.Kind.ZONE, zones);
            byKind.put(Project.Kind.FIGHT, fights);
            byKind.put(Project.Kind.STEALTH, stealths);
            byKind.put(Project.Kind.SKILLS, skills);
            byKind.put(Project.Kind.PATH, paths);

            for (Entry<Project.Kind, TreeView.Node> en : byKind.entrySet()) {
                for (Path d : this.project.subfolders(en.getKey())) {
                    folderNode(made, en.getValue(), this.project.kindDir(en.getKey()), d, en.getKey(), en.getValue().iconColor);
                }
            }

            for (Project.FileRef f : this.project.files()) {
                Icon icon = switch (f.kind()) {
                    case SCRIPT -> Icon.CODE;
                    case BLOCKS -> Icon.GRAPH;
                    case GUI -> Icon.MENU;
                    case CUTSCENE -> Icon.FILM;
                    case EFFECT -> Icon.BOLT;
                    case POST -> Icon.IMAGE;
                    case CAMERA -> Icon.FILM;
                    case STRUCTURE -> Icon.CUBE;
                    case SHOP -> Icon.TAG;
                    case QUEST -> Icon.FLAG;
                    case ZONE -> Icon.PIN;
                    case PATH -> Icon.ARROW_RIGHT;
                    case FIGHT -> Icon.BOLT;
                    case STEALTH -> Icon.SEARCH;
                    case SKILLS -> Icon.GRAPH;
                };
                TreeView.Node n = new TreeView.Node(f.fileName(), icon, f);

                n.iconColor = switch (f.kind()) {
                    case SCRIPT -> -8213761;
                    case BLOCKS -> -870325;
                    case GUI -> -10638858;
                    case CUTSCENE -> -28838;
                    case EFFECT -> -14006;
                    case POST -> -1934337;
                    case CAMERA -> -9706552;
                    case STRUCTURE -> -2514842;
                    case SHOP -> -8658806;
                    case QUEST -> -11686;
                    case ZONE -> -11549705;
                    case PATH -> -21965;
                    case FIGHT -> -38037;
                    case STEALTH -> -43691;
                    case SKILLS -> -5141249;
                };
                if (this.project.isGeneral(f)) {
                    n.iconColor = generals.iconColor;
                    generals.add(n);
                } else {
                    TreeView.Node kindNode = byKind.get(f.kind());
                    folderNode(made, kindNode, this.project.kindDir(f.kind()), f.path().getParent(), f.kind(), kindNode.iconColor).add(n);
                }
            }

            for (TreeView.Node k : byKind.values()) {
                k.badge = String.valueOf(countFiles(k));
            }

            for (TreeView.Node sub : made.values()) {
                sub.badge = String.valueOf(countFiles(sub));
            }

            generals.badge = String.valueOf(generals.children.size());
            TreeView.Node story = group("Сюжеты", Icon.CODE, -8213761, scripts, blocks, generals);
            TreeView.Node world = group("Мир и постановка", Icon.FILM, -28838, cutscenes, zones, paths, structures, effects, posts, cameras);
            TreeView.Node systems = group("Игровые системы", Icon.FLAG, -11686, quests, shops, fights, stealths, skills);
            TreeView.Node look = group("Интерфейс", Icon.MENU, -10638858, guis);
            entities.add(customModels);
            entities.add(ownAnimations);
            TreeView.Node res = group("Персонажи и ресурсы", Icon.USER, -11544898, entities, images, ownSounds, ownVideos);
            world.children.removeAll(List.of(structures, effects, posts));
            systems.children.remove(fights);
            systems.children.remove(stealths);
            systems.children.remove(skills);
            entities.children.removeAll(List.of(customModels, ownAnimations));
            roots.add(story);
            roots.add(world);
            roots.add(systems);
            roots.add(look);
            roots.add(res);
            roots.add(assets);
            TreeView.Node mods = new TreeView.Node("Модификации", Icon.CUBE, new ExplorerPanel.ModsNode());
            mods.iconColor = -5141249;
            TreeView.Node recipes = new TreeView.Node("Рецепты", Icon.LIST, new ExplorerPanel.RecipesNode());
            recipes.iconColor = -6501275;
            roots.add(recipes);
            TreeView.Node engine = new TreeView.Node("Настройки движка", Icon.GEAR, new ExplorerPanel.EngineNode());
            engine.iconColor = -7639809;
            roots.add(engine);
            this.restore(roots);
            this.tree.setRoots(roots);
        }
    }

    private static TreeView.Node group(String name, Icon icon, int color, TreeView.Node... folders) {
        TreeView.Node g = new TreeView.Node(name, icon, new ExplorerPanel.Group(name));
        g.iconColor = color;
        int n = 0;

        for (TreeView.Node f : folders) {
            g.add(f);
            n += f.data instanceof ExplorerPanel.Folder ? countFiles(f) : f.children.size();
        }

        g.badge = String.valueOf(n);
        return g;
    }

    @Override
    public void layout() {
        this.refresh.bounds(this.x + this.w - 22, this.y + 3, 18, 18);
        this.addBlocks.bounds(this.x + this.w - 42, this.y + 3, 18, 18);
        this.addScript.bounds(this.x + this.w - 62, this.y + 3, 18, 18);
        this.addImage.bounds(this.x + this.w - 82, this.y + 3, 18, 18);
        int extraH = this.extra != null ? Math.max(120, (this.h - 24) * 3 / 5) : 0;
        this.tree.bounds(this.x, this.y + 24, this.w, this.h - 24 - extraH);
        if (this.extra != null) {
            this.extra.bounds(this.x, this.y + this.h - extraH, this.w, extraH);
            this.extra.layout();
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -14934490);
        String title = this.project == null ? "ПРОВОДНИК" : this.project.name().toUpperCase();
        c.textEllipsized(title, this.x + 8, this.y + 8, this.w - 92, -6577995);
        c.fill(this.x, this.y + 23, this.w, 1, -13749952);
    }

    public interface Actions {
        void importVideos();

        void renameVideo(Project.VideoRef var1);

        void deleteVideo(Project.VideoRef var1);

        void openVideosFolder();

        void open(Project.FileRef var1);

        void newScript();

        void newGeneral(boolean var1);

        void newBlocks();

        void rename(Project.FileRef var1);

        void duplicate(Project.FileRef var1);

        void delete(Project.FileRef var1);

        void deploy(Project.FileRef var1, boolean var2);

        void openDocs();

        void openAssets(AssetsTab.Mode var1);

        void openEngine();

        default void openRecipes() {
        }

        default void openMods() {
        }

        void newGui();

        default void createIn(Path dir) {
        }

        void newCutscene();

        void newEffect();

        void newShop();

        void newQuest();

        void newZone();

        default void newPath() {
        }

        default void newPost() {
        }

        default void newCamera() {
        }

        default void importStructures() {
        }

        void newFight();

        void newStealth();

        void newSkills();

        void move(Project.FileRef var1, Path var2);

        void openImage(Project.ImageRef var1);

        void openModel(Project.ModelRef var1);

        void openEntity(Project.EntityRef var1);

        void newEntity();

        void deleteEntity(Project.EntityRef var1);

        void importModels();

        void deleteModel(Project.ModelRef var1);

        void importAnimations();

        void deleteAnimation(Path var1);

        void importImages();

        void renameImage(Project.ImageRef var1);

        default void newImage() {
        }

        default void stSkin(Project.ImageRef image) {
        }

        void deleteImage(Project.ImageRef var1);

        void openImagesFolder();

        default void importSounds() {
        }

        default void playSound(Project.SoundRef sound) {
        }

        default void renameSound(Project.SoundRef sound) {
        }

        default void deleteSound(Project.SoundRef sound) {
        }

        default void openSoundsFolder() {
        }
    }

    private record AnimationFile(Path path) {
    }

    private record AnimationsFolder() implements TreeView.Folder {
    }

    private record AssetsFolder() implements TreeView.Folder {
    }

    private record EngineNode() {
    }

    private record EntitiesFolder() implements TreeView.Folder {
    }

    private record Folder(Project.Kind kind) implements TreeView.Folder {
    }

    private record Group(String name) implements TreeView.Folder {
    }

    private record ImagesFolder() implements TreeView.Folder {
    }

    private record ModelsFolder() implements TreeView.Folder {
    }

    private record ModsNode() {
    }

    private record NewFile(String title, Icon icon, int color, Runnable make) {
    }

    private record RecipesNode() {
    }

    private record SoundsFolder() implements TreeView.Folder {
    }

    private record SubFolder(Project.Kind kind, Path dir) implements TreeView.Folder {
    }

    private record VideosFolder() {
    }
}
