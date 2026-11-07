package org.sprautestudio.spraute_engine.editor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.build.StructureImport;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.assets.AssetsTab;
import org.sprautestudio.spraute_engine.editor.assets.EntityTab;
import org.sprautestudio.spraute_engine.editor.assets.FaceSkin;
import org.sprautestudio.spraute_engine.editor.assets.FaceSkinDialog;
import org.sprautestudio.spraute_engine.editor.assets.ImageTab;
import org.sprautestudio.spraute_engine.editor.assets.ModelTab;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.editor.assets.SoundConvert;
import org.sprautestudio.spraute_engine.editor.camera.CameraTab;
import org.sprautestudio.spraute_engine.editor.cutscene.CutsceneTab;
import org.sprautestudio.spraute_engine.editor.engine.EngineStudio;
import org.sprautestudio.spraute_engine.editor.engine.EngineTab;
import org.sprautestudio.spraute_engine.editor.engine.GameShot;
import org.sprautestudio.spraute_engine.editor.gui.GuiTab;
import org.sprautestudio.spraute_engine.editor.panels.ConsolePanel;
import org.sprautestudio.spraute_engine.editor.panels.ExplorerPanel;
import org.sprautestudio.spraute_engine.editor.panels.ProblemsPanel;
import org.sprautestudio.spraute_engine.editor.panels.SidePanel;
import org.sprautestudio.spraute_engine.editor.panels.StoryPanel;
import org.sprautestudio.spraute_engine.editor.path.PathTab;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.ScriptTemplates;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.editor.quest.QuestTab;
import org.sprautestudio.spraute_engine.editor.recipe.RecipesTab;
import org.sprautestudio.spraute_engine.editor.shop.ShopTab;
import org.sprautestudio.spraute_engine.editor.tabs.BlocksTab;
import org.sprautestudio.spraute_engine.editor.tabs.DocsTab;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.editor.tabs.FromTextTab;
import org.sprautestudio.spraute_engine.editor.tabs.ScriptTab;
import org.sprautestudio.spraute_engine.editor.texture.TextureEditor;
import org.sprautestudio.spraute_engine.editor.visual.BlockCodegen;
import org.sprautestudio.spraute_engine.editor.visual.DialogueImport;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.editor.zone.ZoneTab;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.export.BundledProjects;
import org.sprautestudio.spraute_engine.fx.client.FxEngine;
import org.sprautestudio.spraute_engine.general.GeneralLang;
import org.sprautestudio.spraute_engine.general.client.GeneralRuntime;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.code.CodeEditor;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.Label;
import org.sprautestudio.spraute_engine.ui.widgets.MenuBar;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.SplitPane;
import org.sprautestudio.spraute_engine.ui.widgets.TabBar;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;
import org.sprautestudio.spraute_engine.ui.widgets.Window;
import org.sprautestudio.spraute_engine.zone.client.ZoneClient;

public final class SprauteScreen extends UI {
    private static SprauteScreen instance;
    private static final int TOP = 26;
    private static final int STATUS = 16;
    private Project project;
    private final List<EditorTab> tabs = new ArrayList<>();
    private final SprauteScreen.TopBar topBar = new SprauteScreen.TopBar();
    private final MenuBar menu = new MenuBar();
    private final Box runBox = Box.row().gap(3);
    private final SprauteScreen.StatusBar statusBar = new SprauteScreen.StatusBar();
    private final ExplorerPanel explorer;
    private final Widget editorArea = new SprauteScreen.EditorArea();
    private final TabBar editorTabs = new TabBar();
    private final SidePanel side;
    private final Widget bottom = new SprauteScreen.BottomArea();
    private final TabBar bottomTabs = new TabBar();
    private final ProblemsPanel problems;
    private final ConsolePanel console = new ConsolePanel();
    private final StoryPanel stories = new StoryPanel();
    private final SplitPane centerSplit;
    private final SplitPane verticalSplit;
    private final SplitPane mainSplit;
    private final WelcomePage welcome;
    private final TabBar.Tab problemsTab;
    private final TabBar.Tab consoleTab;
    private final TabBar.Tab storiesTab;
    private int tickCounter;
    private int imageCount = -1;
    private static final String LOGO_TEXT = "Spraute Engine";
    private boolean[] roomTaken;
    private final List<SprauteScreen.GuiWindow> guiWindows;
    private ScriptTab lastScript;
    private long guiCacheTime;
    private Map<String, List<String>> guiCache;
    private Path createIn;
    private static WeakReference<Object> uploadedConnection = new WeakReference<>(null);
    private static Path uploadedProject;

    public static void open() {
        if (!BundledProjects.editorHidden()) {
            GameShot.capture();
            if (instance == null) {
                instance = new SprauteScreen();
            }

            Minecraft.getInstance().setScreen(instance);
        }
    }

    public static String currentProjectName() {
        return instance != null && instance.project != null ? instance.project.name() : "";
    }

    public static void fileChangedOutside(Path p) {
        SprauteScreen s = instance;
        if (s != null && s.project != null) {
            try {
                s.explorer.reload();
            } catch (Throwable var5) {
            }

            for (EditorTab t : new ArrayList<>(s.tabs)) {
                if (t.file != null && t.file.path().toAbsolutePath().normalize().equals(p.toAbsolutePath().normalize()) && !t.isDirty()) {
                    Project.FileRef f = t.file;
                    s.removeTab(t);
                    if (Files.exists(p)) {
                        s.openFile(f);
                    }
                }
            }
        }
    }

    public static String openForAssistant(Path projectDir, Path file) {
        open();
        SprauteScreen s = instance;
        if (s.project == null || !s.project.dir.toAbsolutePath().normalize().equals(projectDir.toAbsolutePath().normalize())) {
            s.openProject(projectDir);
        }

        if (file == null) {
            return "Открыт проект";
        } else {
            String section = file.getFileName().toString();
            if (!section.equals("@recipes") && !section.equals("@mods") && !section.equals("@engine")) {
                String rel = projectDir.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
                Project.Kind k = Project.Kind.ofFile(rel);
                if (!rel.endsWith(k.ext)) {
                    return "Этот файл не открывается в редакторе";
                } else {
                    s.openFile(new Project.FileRef(file, k));
                    return "Открыто: " + rel;
                }
            } else {
                if (section.equals("@recipes")) {
                    s.openRecipes();
                } else if (section.equals("@engine")) {
                    s.openEngine();
                } else {
                    s.openMods();
                }

                return "Открыт раздел";
            }
        }
    }

    private SprauteScreen() {
        super(Component.literal("Spraute Engine"));
        this.root.add(new Widget() {
            @Override
            public void layout() {
                SprauteScreen.this.layoutRoot();
            }
        });
        this.guiWindows = new ArrayList<>();
        this.guiCache = Map.of();
        Workspace.Settings s = Workspace.settings();
        this.setUiScale(s.physicalScale());
        this.explorer = new ExplorerPanel(
            new ExplorerPanel.Actions() {
                @Override
                public void open(Project.FileRef f) {
                    SprauteScreen.this.openFile(f);
                }

                @Override
                public void newGeneral(boolean blocks) {
                    SprauteScreen.this.createGeneral(blocks);
                }

                @Override
                public void newScript() {
                    SprauteScreen.this.createScript(false);
                }

                @Override
                public void newBlocks() {
                    SprauteScreen.this.createScript(true);
                }

                @Override
                public void rename(Project.FileRef f) {
                    SprauteScreen.this.renameFile(f);
                }

                @Override
                public void duplicate(Project.FileRef f) {
                    try {
                        SprauteScreen.this.project.duplicate(f);
                        SprauteScreen.this.explorer.reload();
                    } catch (IOException var3) {
                        SprauteScreen.this.toastError(var3.getMessage());
                    }
                }

                @Override
                public void delete(Project.FileRef f) {
                    SprauteScreen.this.deleteFile(f);
                }

                @Override
                public void deploy(Project.FileRef f, boolean start) {
                    EditorTab t = SprauteScreen.this.openFile(f);
                    if (t != null) {
                        SprauteScreen.this.deploy(t, start);
                    }
                }

                @Override
                public void openDocs() {
                    SprauteScreen.this.openDocs();
                }

                @Override
                public void openAssets(AssetsTab.Mode mode) {
                    SprauteScreen.this.openAssets(mode);
                }

                @Override
                public void openModel(Project.ModelRef model) {
                    SprauteScreen.this.openModel(model);
                }

                @Override
                public void openEntity(Project.EntityRef entity) {
                    SprauteScreen.this.openEntity(entity);
                }

                @Override
                public void newEntity() {
                    SprauteScreen.this.createEntity(null);
                }

                @Override
                public void deleteEntity(Project.EntityRef entity) {
                    Dialog.confirm("Удалить сущность", "Сущность «" + entity.name() + "» будет удалена.", "Удалить", true, () -> {
                        for (EditorTab t : new ArrayList<>(SprauteScreen.this.tabs)) {
                            if (t instanceof EntityTab et && et.ref.path().equals(entity.path())) {
                                SprauteScreen.this.removeTab(t);
                            }
                        }

                        try {
                            Files.deleteIfExists(entity.path());
                        } catch (IOException var5) {
                            SprauteScreen.this.toastError(var5.getMessage());
                        }

                        ModelLibrary.scan();
                        SprauteScreen.this.explorer.reload();
                    });
                }

                @Override
                public void importModels() {
                    SprauteScreen.this.importModelsDialog();
                }

                @Override
                public void importAnimations() {
                    SprauteScreen.this.importAnimationsDialog();
                }

                @Override
                public void deleteAnimation(Path file) {
                    Dialog.confirm("Удалить анимацию", "Файл «" + file.getFileName() + "» будет удалён.", "Удалить", true, () -> {
                        try {
                            Files.deleteIfExists(file);
                        } catch (IOException var3) {
                            SprauteScreen.this.toastError(var3.getMessage());
                        }

                        ModelLibrary.scan();
                        SprauteScreen.this.explorer.reload();
                    });
                }

                @Override
                public void deleteModel(Project.ModelRef model) {
                    Dialog.confirm("Удалить модель", "Папка модели «" + model.name() + "» будет удалена.", "Удалить", true, () -> {
                        for (EditorTab t : new ArrayList<>(SprauteScreen.this.tabs)) {
                            if (t instanceof ModelTab mt && mt.model.dir().equals(model.dir())) {
                                SprauteScreen.this.removeTab(t);
                            }
                        }

                        try (Stream<Path> sx = Files.walk(model.dir())) {
                            for (Path p : (Iterable<Path>) sx.sorted(Comparator.reverseOrder())::iterator) {
                                Files.delete(p);
                            }
                        } catch (IOException var7) {
                            SprauteScreen.this.toastError(var7.getMessage());
                        }

                        ModelLibrary.scan();
                        SprauteScreen.this.explorer.reload();
                    });
                }

                @Override
                public void openEngine() {
                    SprauteScreen.this.openEngine();
                }

                @Override
                public void createIn(Path dir) {
                    SprauteScreen.this.createIn = dir;
                }

                @Override
                public void openRecipes() {
                    SprauteScreen.this.openRecipes();
                }

                @Override
                public void openMods() {
                    SprauteScreen.this.openMods();
                }

                @Override
                public void newGui() {
                    SprauteScreen.this.createGui();
                }

                @Override
                public void newCutscene() {
                    SprauteScreen.this.createCutscene();
                }

                @Override
                public void newEffect() {
                    SprauteScreen.this.createEffect();
                }

                @Override
                public void newPost() {
                    SprauteScreen.this.createPost();
                }

                @Override
                public void newCamera() {
                    SprauteScreen.this.createCamera();
                }

                @Override
                public void importStructures() {
                    if (SprauteScreen.this.project != null) {
                        SprauteScreen.this.pickFiles(
                            "Импорт постройки",
                            new String[]{"*.nbt", "*.schem", "*.litematic"},
                            "Схемы (NBT, Create, WorldEdit, Litematica)",
                            SprauteScreen.this::importStructureFiles
                        );
                    }
                }

                @Override
                public void newShop() {
                    SprauteScreen.this.createShop();
                }

                @Override
                public void newQuest() {
                    SprauteScreen.this.createQuest();
                }

                @Override
                public void newZone() {
                    SprauteScreen.this.createZone();
                }

                @Override
                public void newPath() {
                    SprauteScreen.this.createPath();
                }

                @Override
                public void move(Project.FileRef f, Path dir) {
                    if (!f.path().getParent().equals(dir)) {
                        for (EditorTab t : new ArrayList<>(SprauteScreen.this.tabs)) {
                            if (t.file != null && t.file.path().equals(f.path())) {
                                if (t.isDirty()) {
                                    SprauteScreen.this.save(t);
                                }

                                SprauteScreen.this.removeTab(t);
                            }
                        }

                        for (SprauteScreen.GuiWindow gw : new ArrayList<>(SprauteScreen.this.guiWindows)) {
                            if (gw.tab().file != null && gw.tab().file.path().equals(f.path())) {
                                if (gw.tab().isDirty()) {
                                    SprauteScreen.this.save(gw.tab());
                                }

                                SprauteScreen.this.guiWindows.remove(gw);
                                SprauteScreen.this.closeOverlay(gw.window());
                            }
                        }

                        try {
                            SprauteScreen.this.project.moveTo(f, dir);
                            SprauteScreen.this.toastSuccess("Перемещено: " + f.fileName());
                        } catch (IOException var5) {
                            SprauteScreen.this.toastError(var5.getMessage());
                        }

                        SprauteScreen.this.explorer.reload();
                    }
                }

                @Override
                public void newFight() {
                    SprauteScreen.this.createFight();
                }

                @Override
                public void newStealth() {
                    SprauteScreen.this.createStealth();
                }

                @Override
                public void newSkills() {
                    SprauteScreen.this.createSkills();
                }

                @Override
                public void openImage(Project.ImageRef image) {
                    SprauteScreen.this.openImage(image);
                }

                @Override
                public void importImages() {
                    SprauteScreen.this.importImagesDialog();
                }

                @Override
                public void renameImage(Project.ImageRef image) {
                    SprauteScreen.this.renameImage(image);
                }

                @Override
                public void newImage() {
                    SprauteScreen.this.newImageDialog();
                }

                @Override
                public void stSkin(Project.ImageRef image) {
                    FaceSkinDialog.open(SprauteScreen.this.project, image.path(), image.name(), made -> {
                        SprauteScreen.this.explorer.reload();
                        this.openImage(made);
                    });
                }

                @Override
                public void deleteImage(Project.ImageRef image) {
                    SprauteScreen.this.deleteImage(image);
                }

                @Override
                public void importVideos() {
                    if (SprauteScreen.this.project != null) {
                        SprauteScreen.this.pickFiles(
                            "Добавить видео в проект",
                            new String[]{"*.mp4", "*.webm", "*.mkv", "*.mov", "*.m4v", "*.avi"},
                            "Видео (MP4, WebM, MKV, MOV, AVI)",
                            SprauteScreen.this::importVideos
                        );
                    }
                }

                @Override
                public void renameVideo(Project.VideoRef video) {
                    Dialog.prompt(
                        "Переименовать видео", "Новое имя (используется в интерфейсах):", video.name(), v -> v.isBlank() ? "Введите имя" : null, v -> {
                            try {
                                SprauteScreen.this.project.renameVideo(video, v);
                                SprauteScreen.this.explorer.reload();
                                SprauteScreen.this.toastInfo("Не забудьте выбрать его заново в интерфейсах, где было «" + video.name() + "»");
                            } catch (IOException var4) {
                                SprauteScreen.this.toastError(var4.getMessage());
                            }
                        }
                    );
                }

                @Override
                public void deleteVideo(Project.VideoRef video) {
                    Dialog.confirm("Удалить видео", "Видео «" + video.name() + "» будет удалено из проекта.", "Удалить", true, () -> {
                        try {
                            Files.deleteIfExists(video.path());
                        } catch (IOException var3) {
                            SprauteScreen.this.toastError(var3.getMessage());
                        }

                        SprauteScreen.this.explorer.reload();
                    });
                }

                @Override
                public void openVideosFolder() {
                    if (SprauteScreen.this.project != null) {
                        try {
                            Files.createDirectories(SprauteScreen.this.project.videosDir());
                        } catch (IOException var2) {
                        }

                        Util.getPlatform().openFile(SprauteScreen.this.project.videosDir().toFile());
                    }
                }

                @Override
                public void importSounds() {
                    if (SprauteScreen.this.project != null) {
                        SprauteScreen.this.pickFiles(
                            "Добавить звуки в проект",
                            new String[]{"*.ogg", "*.wav", "*.mp3", "*.m4a", "*.aac", "*.wma", "*.flac"},
                            "Звуки (OGG, WAV, MP3, M4A, AAC, WMA, FLAC)",
                            SprauteScreen.this::importSounds
                        );
                    }
                }

                @Override
                public void playSound(Project.SoundRef sound) {
                    ProjectSounds.stop("__editor_preview");
                    ProjectSounds.play(SprauteScreen.this.project.name(), sound.name(), 0.0, 0.0, 0.0, false, 1.0F, 1.0F, false, "__editor_preview");
                }

                @Override
                public void renameSound(Project.SoundRef sound) {
                    Dialog.prompt("Переименовать звук", "Новое имя (используется в скриптах):", sound.name(), v -> v.isBlank() ? "Введите имя" : null, v -> {
                        try {
                            SprauteScreen.this.project.renameSound(sound, v);
                            SprauteScreen.this.explorer.reload();
                            SprauteScreen.this.toastInfo("Не забудьте обновить sound \"" + sound.name() + "\" в скриптах");
                        } catch (IOException var4) {
                            SprauteScreen.this.toastError(var4.getMessage());
                        }
                    });
                }

                @Override
                public void deleteSound(Project.SoundRef sound) {
                    Dialog.confirm("Удалить звук", "Звук «" + sound.name() + "» будет удалён из проекта.", "Удалить", true, () -> {
                        try {
                            Files.deleteIfExists(sound.path());
                        } catch (IOException var3) {
                            SprauteScreen.this.toastError(var3.getMessage());
                        }

                        SprauteScreen.this.explorer.reload();
                    });
                }

                @Override
                public void openSoundsFolder() {
                    if (SprauteScreen.this.project != null) {
                        try {
                            Files.createDirectories(SprauteScreen.this.project.soundsDir());
                        } catch (IOException var2) {
                        }

                        Util.getPlatform().openFile(SprauteScreen.this.project.soundsDir().toFile());
                    }
                }

                @Override
                public void openImagesFolder() {
                    if (SprauteScreen.this.project != null) {
                        try {
                            Files.createDirectories(SprauteScreen.this.project.imagesDir());
                        } catch (IOException var2) {
                        }

                        Util.getPlatform().openFile(SprauteScreen.this.project.imagesDir().toFile());
                    }
                }
            }
        );
        AssetIndex.projectImages = () -> ProjectImages.names(this.project);
        AssetIndex.projectSounds = () -> this.project != null ? this.project.soundNames() : List.of();
        AssetIndex.projectName = () -> this.project != null ? this.project.name() : "";
        AssetIndex.projectGuis = this::projectGuis;
        AssetIndex.projectCutscenes = () -> this.project != null ? this.project.cutsceneNames() : List.of();
        AssetIndex.projectEffects = () -> this.project != null ? this.project.effectNames() : List.of();
        AssetIndex.projectPosts = () -> this.project != null ? this.project.postNames() : List.of();
        AssetIndex.projectCams = () -> this.project != null ? this.project.cameraNames() : List.of();
        AssetIndex.projectStructures = () -> this.project != null ? this.project.structureNames() : List.of();
        AssetIndex.projectShops = () -> this.project != null ? this.project.shopNames() : List.of();
        AssetIndex.projectQuests = () -> this.project != null ? this.project.questNames() : List.of();
        AssetIndex.projectQuestTasks = n -> this.project != null ? this.project.questTasks(n) : Map.of();
        AssetIndex.projectCharacters = () -> this.project != null ? this.project.characterNames() : List.of();
        AssetIndex.projectZones = () -> this.project != null ? this.project.zoneNames() : List.of();
        AssetIndex.projectPaths = () -> this.project != null ? this.project.pathNames() : List.of();
        AssetIndex.projectFights = () -> this.project != null ? this.project.fightNames() : List.of();
        AssetIndex.projectStealths = () -> this.project != null ? this.project.stealthNames() : List.of();
        AssetIndex.projectSkills = () -> this.project != null ? this.project.skillNames() : List.of();
        AssetIndex.projectSkillNodes = () -> this.project != null ? this.project.skillNodeIds() : List.of();
        AssetIndex.projectSkillMenus = () -> this.project != null ? this.project.skillMenuIds() : List.of();
        ZoneClient.projectZones = () -> this.project != null ? this.project.zoneSources() : Map.of();
        this.side = new SidePanel(this::activeTab);
        this.problems = new ProblemsPanel(this::collectProblems);
        this.editorTabs.onSelect = t -> this.showTab((EditorTab)t.data);
        this.editorTabs.onClose = t -> this.closeTab((EditorTab)t.data);
        this.editorTabs.onContext = (t, mx, my) -> {
            EditorTab tab = (EditorTab)t.data;
            PopupMenu pm = new PopupMenu();
            pm.item("Закрыть", Icon.CLOSE, "Ctrl+W", () -> this.closeTab(tab));
            pm.item("Закрыть другие", Icon.CLOSE, null, () -> {
                for (EditorTab other : new ArrayList<>(this.tabs)) {
                    if (other != tab) {
                        this.closeTab(other);
                    }
                }
            });
            pm.item("Закрыть все", Icon.CLOSE, null, () -> {
                for (EditorTab other : new ArrayList<>(this.tabs)) {
                    this.closeTab(other);
                }
            });
            if (tab.source() != null) {
                pm.separator();
                pm.item("Загрузить и запустить", Icon.PLAY, "F5", () -> this.deploy(tab, true)).color(-11746422);
            }

            pm.show(mx, my);
        };
        this.editorArea.add(this.editorTabs);
        this.bottomTabs.editorStyle = false;
        this.problemsTab = this.bottomTabs.add(new TabBar.Tab(() -> "Проблемы", Icon.WARNING, this.problems));
        this.problemsTab.badge = () -> {
            int n = 0;

            for (EditorTab.Problem p : this.collectProblems()) {
                if (p.severity() == LanguageSupport.Severity.ERROR) {
                    n++;
                }
            }

            return n == 0 ? "" : String.valueOf(n);
        };
        this.problemsTab.badgeColor = -1023878;
        this.consoleTab = this.bottomTabs.add(new TabBar.Tab(() -> "Консоль", Icon.TERMINAL, this.console));
        this.storiesTab = this.bottomTabs.add(new TabBar.Tab(() -> "Сюжеты", Icon.PLAY, this.stories));
        this.storiesTab.badge = () -> {
            int n = 0;

            for (Packets.ScriptEntry e : ClientState.get().status().scripts()) {
                if (e.active) {
                    n++;
                }
            }

            return n == 0 ? "" : n + " ▶";
        };
        this.storiesTab.badgeColor = -11746422;
        this.bottomTabs.onSelect = t -> this.bottom.layout();
        this.bottom.add(this.bottomTabs);
        this.bottom.add(this.problems);
        this.bottom.add(this.console);
        this.bottom.add(this.stories);
        this.bottomTabs.select(this.storiesTab);
        this.stories.showConsole = () -> this.bottomTabs.select(this.consoleTab);
        this.stories.openInEditor = this::openScriptById;
        this.centerSplit = new SplitPane(true, this.editorArea, this.side, false, s.rightW);
        this.centerSplit.minSize = 140;
        this.verticalSplit = new SplitPane(false, this.centerSplit, this.bottom, false, s.bottomH);
        this.verticalSplit.minSize = 80;
        this.mainSplit = new SplitPane(true, this.explorer, this.verticalSplit, true, s.sidebarW);
        this.mainSplit.minSize = 120;
        this.centerSplit.onResize = v -> Workspace.settings().rightW = v;
        this.verticalSplit.onResize = v -> Workspace.settings().bottomH = v;
        this.mainSplit.onResize = v -> Workspace.settings().sidebarW = v;
        this.explorer.visible = s.showSidebar;
        this.side.visible = s.showRight;
        this.bottom.visible = s.showBottom;
        this.welcome = new WelcomePage(
            this::newProjectDialog,
            this::createDemo,
            this::openDocsWithoutProject,
            info -> this.openProject(info.dir()),
            info -> Dialog.confirm("Удалить проект", "Проект «" + info.name() + "» и все его файлы будут удалены безвозвратно.", "Удалить", true, () -> {
                try {
                    Workspace.deleteProject(info.dir());
                    this.reloadWelcome();
                } catch (IOException var3) {
                    this.toastError(var3.getMessage());
                }
            })
        );
        this.buildMenu();
        this.buildRunBox();
        this.root.add(this.topBar);
        this.topBar.add(this.menu);
        this.topBar.add(this.runBox);
        this.root.add(this.statusBar);
        this.root.add(this.welcome);
        this.root.add(this.mainSplit);
        this.mainSplit.visible = false;
        ClientState.get().deployListener = r -> {
            if (r.ok()) {
                this.toastSuccess(r.messages().isEmpty() ? "Сюжет загружен" : r.messages().get(0));
            } else {
                this.toastError("Не удалось загрузить сюжет #" + r.id());
                this.bottomTabs.select(this.consoleTab);
                this.showBottom(true);
            }
        };
        if (s.lastProject != null) {
            Path dir = Workspace.projectsDir().resolve(s.lastProject);
            if (Files.isDirectory(dir)) {
                this.openProject(dir);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        this.root.layout();
    }

    private void layoutRoot() {
        this.topBar.bounds(0, 0, this.vw, 26);
        this.statusBar.bounds(0, this.vh - 16, this.vw, 16);
        this.welcome.visible = this.project == null;
        this.mainSplit.visible = this.project != null;
        this.welcome.bounds(0, 26, this.vw, this.vh - 26 - 16);
        this.mainSplit.bounds(0, 26, this.vw, this.vh - 26 - 16);
    }

    @Override
    public void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
        EditorTab current = this.activeTab();

        for (EditorTab t : this.tabs) {
            if (t != current || !this.mainSplit.visible) {
                t.setShown(false);
            }
        }

        if (current != null && this.mainSplit.visible) {
            current.setShown(true);
        }

        this.makeRoom(current != null && this.mainSplit.visible && current.wantsRoom());
        this.explorer.setExtra(current != null && this.mainSplit.visible ? current.hierarchy() : null);
        super.draw(g, mouseX, mouseY, partialTick);
    }

    private void makeRoom(boolean want) {
        if (want != (this.roomTaken != null)) {
            if (want) {
                this.roomTaken = new boolean[]{this.explorer.visible, this.bottom.visible};
                this.explorer.visible = false;
                this.bottom.visible = false;
            } else {
                this.explorer.visible = this.roomTaken[0];
                this.bottom.visible = this.roomTaken[1];
                this.roomTaken = null;
            }

            this.relayout();
        }
    }

    private void buildMenu() {
        this.menu.menu("Файл", pm -> {
            pm.item("Новый проект…", Icon.PLUS, null, this::newProjectDialog);
            pm.item("Открыть проект…", Icon.FOLDER, null, this::closeProject);
            pm.separator();
            pm.item("Новый текстовый скрипт", Icon.CODE, "Ctrl+N", () -> this.createScript(false)).enabled(() -> this.project != null);
            pm.item("Новый блочный скрипт", Icon.GRAPH, "Ctrl+Shift+N", () -> this.createScript(true)).enabled(() -> this.project != null);
            pm.item("Новый интерфейс (GUI)", Icon.MENU, null, this::createGui).enabled(() -> this.project != null);
            pm.separator();
            pm.item("Сохранить", Icon.SAVE, "Ctrl+S", this::saveActive).enabled(() -> this.activeTab() != null);
            pm.item("Сохранить всё", Icon.SAVE, "Ctrl+Shift+S", this::saveAllByHand).enabled(() -> this.project != null);
            pm.item("Закрыть вкладку", Icon.CLOSE, "Ctrl+W", () -> this.closeTab(this.activeTab())).enabled(() -> this.activeTab() != null);
            pm.separator();
            pm.item("Экспорт как мод…", Icon.UPLOAD, null, this::exportMod).enabled(() -> this.project != null);
            pm.separator();
            pm.item("Закрыть проект", Icon.HOME, null, this::closeProject).enabled(() -> this.project != null);
            pm.item("Выйти из редактора", Icon.CLOSE, "Esc", this::onClose);
        });
        this.menu
            .menu(
                "Правка",
                pm -> {
                    pm.item("Отменить", Icon.UNDO, "Ctrl+Z", () -> {
                        EditorTab t = this.activeTab();
                        if (t != null) {
                            t.undo();
                        }
                    });
                    pm.item("Повторить", Icon.REDO, "Ctrl+Y", () -> {
                        EditorTab t = this.activeTab();
                        if (t != null) {
                            t.redo();
                        }
                    });
                    pm.separator();
                    pm.item("Найти", Icon.SEARCH, "Ctrl+F", () -> this.withEditor(e -> e.editor.openFind(false)));
                    pm.item("Заменить", Icon.SEARCH, "Ctrl+H", () -> this.withEditor(e -> e.editor.openFind(true)));
                    pm.item("Перейти к строке", Icon.ARROW_RIGHT, "Ctrl+G", () -> this.withEditor(e -> e.editor.promptGotoLine()));
                    pm.separator();
                    pm.item("Закомментировать", Icon.CODE, "Ctrl+/", () -> this.withEditor(e -> e.editor.toggleComment()));
                    pm.item("Форматировать", Icon.LIST, "Ctrl+Shift+F", () -> this.withEditor(e -> e.editor.format()));
                    pm.item("Из текста…", Icon.EDIT, null, this::openFromText)
                        .enabled(() -> this.activeTab() instanceof ScriptTab || this.activeTab() instanceof BlocksTab);
                    pm.item("Вставить мою позицию", Icon.PIN, "Alt+P", () -> this.withEditor(e -> {
                        String pos = this.playerPosition();
                        if (pos != null) {
                            e.editor.insertAtCaret(pos);
                        }
                    }));
                }
            );
        this.menu.menu("Запуск", pm -> {
            pm.item("Загрузить и запустить", Icon.PLAY, "F5", () -> this.deployActive(true)).color(-11746422).enabled(this::canRun);
            pm.item("Только загрузить в игру", Icon.UPLOAD, "F6", () -> this.deployActive(false)).enabled(this::canRun);
            pm.item("Остановить", Icon.STOP, "Shift+F5", () -> this.activeCommand("script stop %d")).color(-1023878).enabled(this::canRun);
            pm.separator();
            pm.item("Пауза", Icon.PAUSE, null, () -> this.activeCommand("script pause %d")).enabled(this::canRun);
            pm.item("Продолжить", Icon.PLAY, null, () -> this.activeCommand("script resume %d")).enabled(this::canRun);
            pm.item("Подготовить (setup)", Icon.SETUP, null, () -> this.activeCommand("script setup %d")).enabled(this::canRun);
            pm.item("Перейти к метке…", Icon.TAG, null, this::gotoLabelActive).enabled(this::canRun);
            pm.item("Триггер (wait trigger)", Icon.BOLT, null, () -> this.activeCommand("script trigger %d next")).enabled(this::canRun);
            pm.separator();
            pm.item("Остановить все сюжеты", Icon.STOP, null, () -> ClientState.get().command("script stop all")).color(-1023878);
            pm.item("Панель сюжетов", Icon.LIST, null, () -> {
                this.showBottom(true);
                this.bottomTabs.select(this.storiesTab);
            });
        });
        this.menu
            .menu(
                "Вид",
                pm -> {
                    pm.item("Проводник", Icon.FOLDER, "Ctrl+B", () -> this.toggle(this.explorer, this.mainSplit)).checked(() -> this.explorer.visible);
                    pm.item("Правая панель", Icon.LIST, "Ctrl+Alt+B", () -> this.toggle(this.side, this.centerSplit)).checked(() -> this.side.visible);
                    pm.item("Нижняя панель", Icon.TERMINAL, "Ctrl+J", () -> this.showBottom(!this.bottom.visible)).checked(() -> this.bottom.visible);
                    pm.separator();
                    pm.item("Модели", Icon.CUBE, "Ctrl+Shift+A", () -> this.openAssets(AssetsTab.Mode.MODELS)).enabled(() -> this.project != null);
                    pm.item("Анимации", Icon.FILM, null, () -> this.openAssets(AssetsTab.Mode.ANIMATIONS)).enabled(() -> this.project != null);
                    pm.item("Звуки", Icon.NOTE, null, () -> this.openAssets(AssetsTab.Mode.SOUNDS)).enabled(() -> this.project != null);
                    pm.item("Мини-карта кода", Icon.CODE, null, () -> {
                        Workspace.settings().minimap = !Workspace.settings().minimap;

                        for (EditorTab t : this.tabs) {
                            if (t instanceof ScriptTab st) {
                                st.editor.showMinimap = Workspace.settings().minimap;
                            }
                        }

                        this.relayout();
                    }).checked(() -> Workspace.settings().minimap);
                    pm.separator();
                    pm.item("Масштаб интерфейса: " + this.scaleText() + "…", Icon.SEARCH, "Ctrl+= / Ctrl+−", this::scaleDialog);
                    pm.item("Масштаб: как в Minecraft", Icon.SEARCH, null, () -> this.setPercent(0)).checked(() -> Workspace.settings().uiPercent <= 0);

                    for (int pc : new int[]{50, 75, 100, 125, 150, 200, 250, 300}) {
                        pm.item("Масштаб: " + pc + "%" + (pc == 100 ? " (как раньше 2x)" : ""), Icon.SEARCH, null, () -> this.setPercent(pc))
                            .checked(() -> Workspace.settings().uiPercent == pc);
                    }
                }
            );
        this.menu
            .menu(
                "Справка",
                pm -> {
                    pm.item("Справочник языка", Icon.BOOK, "F1", this::openDocs);
                    pm.item(
                        "Горячие клавиши",
                        Icon.INFO,
                        null,
                        () -> Dialog.info(
                            "Горячие клавиши",
                            "F5 — загрузить и запустить, F6 — загрузить, Shift+F5 — стоп\nCtrl+S — сохранить, Ctrl+Shift+S — сохранить всё, Ctrl+W — закрыть вкладку\nCtrl+Space — подсказки, Ctrl+клик / F12 — к объявлению\nCtrl+F / Ctrl+H — поиск / замена, Ctrl+G — к строке\nCtrl+/ — комментарий, Ctrl+D — дублировать, Alt+↑↓ — двигать строки\nCtrl+Shift+F — форматировать, Alt+P — вставить свои координаты\nCtrl+B / Ctrl+J — проводник / нижняя панель, Ctrl+Tab — следующая вкладка\nCtrl+Shift+A — модели, анимации и звуки\n/editor on — клавиша N (настраивается) открывает и закрывает редактор"
                        )
                    );
                    pm.item(
                        "О Spraute Engine",
                        Icon.INFO,
                        null,
                        () -> Dialog.info(
                            "Spraute Engine",
                            "Spraute Engine — движок сюжетов с визуальным редактором.\nТекстовые скрипты (.sst) и блочные (.svs) запускаются прямо в мире.\n\nt.me/spraute"
                        )
                    );
                }
            );
    }

    private void buildRunBox() {
        Button run = new Button("Запустить", () -> this.deployActive(true)).icon(Icon.PLAY).style(Button.Style.SUCCESS).enabledWhen(this::canRun);
        run.autoWidth();
        run.tooltip("Загрузить в игру и запустить (F5)");
        Button upload = Button.icon(Icon.UPLOAD, "Только загрузить в игру (F6)", () -> this.deployActive(false)).enabledWhen(this::canRun);
        Button stop = Button.icon(Icon.STOP, "Остановить сюжет (Shift+F5)", () -> this.activeCommand("script stop %d"))
            .iconColor(-1023878)
            .enabledWhen(this::canRun);
        Button pause = Button.icon(Icon.PAUSE, "Пауза / продолжить", this::togglePauseActive).iconColor(-870325).enabledWhen(this::canRun);
        Button label = Button.icon(Icon.TAG, "Перейти к метке", this::gotoLabelActive).iconColor(-33098).enabledWhen(this::canRun);
        Button close = Button.icon(Icon.CLOSE, "Закрыть редактор (Esc)", this::onClose);
        Button world = Button.icon(Icon.CUBE, "Открыть вкладку «Мир»: NPC, эффекты и зоны прямо в мире", this::openWorldEditor).iconColor(-7639809);

        for (Button b : List.of(upload, stop, pause, label, world)) {
            b.prefW = 20;
        }

        close.prefW = 20;
        this.runBox.add(world);
        this.runBox.add(run);
        this.runBox.add(upload);
        this.runBox.add(stop);
        this.runBox.add(pause);
        this.runBox.add(label);
        Widget sep = new Widget();
        sep.prefW = 6;
        this.runBox.add(sep);
        this.runBox.add(close);
    }

    private void toggle(Widget panel, SplitPane split) {
        panel.visible = !panel.visible;
        Workspace.settings().showSidebar = this.explorer.visible;
        Workspace.settings().showRight = this.side.visible;
        split.layout();
        this.relayout();
    }

    private void showBottom(boolean show) {
        this.bottom.visible = show;
        Workspace.settings().showBottom = show;
        this.relayout();
    }

    private void newProjectDialog() {
        Dialog.prompt("Новый проект", "Название проекта:", "Мой сюжет", v -> {
            if (v.isEmpty()) {
                return "Введите название";
            } else {
                return Files.exists(Workspace.projectsDir().resolve(Project.sanitize(v))) ? "Такой проект уже есть" : null;
            }
        }, v -> {
            try {
                Project p = Workspace.createProject(v);
                Project.FileRef f = p.newScript("main");
                this.openProject(p.dir);
                this.openFile(f);
            } catch (IOException var4) {
                this.toastError(var4.getMessage());
            }
        });
    }

    private void createDemo() {
        String name = "Пример";
        int n = 2;

        while (Files.exists(Workspace.projectsDir().resolve(name))) {
            name = "Пример " + n++;
        }

        try {
            Project p = Workspace.createProject(name);
            Project.FileRef script = new Project.FileRef(p.scriptsDir().resolve("встреча.sst"), Project.Kind.SCRIPT);
            p.write(script, ScriptTemplates.demo(p.meta.baseId));
            Project.FileRef blocks = new Project.FileRef(p.blocksDir().resolve("блоки.svs"), Project.Kind.BLOCKS);
            p.write(blocks, VisualScript.create(p.meta.baseId + 1, "Блочный пример").toJson());
            p.newGui("меню");
            this.openProject(p.dir);
            this.openFile(script);
            this.toastSuccess("Создан проект «" + name + "»");
        } catch (IOException var6) {
            this.toastError(var6.getMessage());
        }
    }

    private void openProject(Path dir) {
        if (this.project != null) {
            this.closeProject();
        }

        try {
            this.project = Project.open(dir);
        } catch (IOException var5) {
            this.toastError("Не удалось открыть проект: " + var5.getMessage());
            return;
        }

        Workspace.settings().lastProject = dir.getFileName().toString();
        Workspace.saveSettings();
        this.explorer.setProject(this.project);
        ProjectImages.register(this.project);

        for (String rel : new ArrayList<>(this.project.meta.openFiles)) {
            Path p = this.project.dir.resolve(rel);
            if (Files.exists(p)) {
                this.openFile(new Project.FileRef(p, Project.Kind.ofFile(rel)));
            }
        }

        this.relayout();
    }

    private void closeProject() {
        if (this.project != null) {
            this.saveAll();
            this.project.meta.openFiles.clear();

            for (EditorTab t : this.allEditors()) {
                if (t.file != null) {
                    this.project.meta.openFiles.add(this.project.dir.relativize(t.file.path()).toString().replace('\\', '/'));
                }
            }

            this.project.saveMeta();
        }

        this.closeGuiWindows();

        for (EditorTab tx : new ArrayList<>(this.tabs)) {
            this.removeTab(tx);
        }

        this.project = null;
        Workspace.settings().lastProject = null;
        Workspace.saveSettings();
        this.welcome.reload();
        this.relayout();
    }

    // Decompile note: the original lambda in the constructor read this.welcome directly;
    // javac accepts that, ECJ rejects it (blank final field), so the access goes through a method.
    private void reloadWelcome() {
        this.welcome.reload();
    }

    private EditorTab activeTab() {
        TabBar.Tab t = this.editorTabs.selected();
        return t == null ? null : (EditorTab)t.data;
    }

    private void withEditor(Consumer<ScriptTab> action) {
        if (this.activeTab() instanceof ScriptTab st) {
            action.accept(st);
        }
    }

    private EditorTab openGuiWindow(Project.FileRef f) {
        for (SprauteScreen.GuiWindow gw : this.guiWindows) {
            if (gw.tab().file != null && gw.tab().file.path().equals(f.path())) {
                gw.window().toFront();
                gw.tab().focusContent();
                return gw.tab();
            }
        }

        String content;
        try {
            content = this.project.read(f);
        } catch (IOException var15) {
            this.toastError("Не удалось открыть файл: " + var15.getMessage());
            return null;
        }

        boolean fxk = f.kind() == Project.Kind.EFFECT;
        boolean shopk = f.kind() == Project.Kind.SHOP;
        boolean questk = f.kind() == Project.Kind.QUEST;
        boolean zonek = f.kind() == Project.Kind.ZONE;
        boolean fightk = f.kind() == Project.Kind.FIGHT;
        boolean stealthk = f.kind() == Project.Kind.STEALTH;
        boolean skillsk = f.kind() == Project.Kind.SKILLS;
        boolean pathk = f.kind() == Project.Kind.PATH;
        boolean cut = f.kind() == Project.Kind.CUTSCENE;
        EditorTab tab = (EditorTab)(pathk
            ? new PathTab(this.project, f, content)
            : (
                zonek
                    ? new ZoneTab(this.project, f, content)
                    : (
                        questk
                            ? new QuestTab(this.project, f, content)
                            : (
                                shopk
                                    ? new ShopTab(this.project, f, content)
                                    : (cut ? new CutsceneTab(this.project, f, content) : new GuiTab(this.project, f, content))
                            )
                    )
            ));
        Window win = new Window(
            () -> (
                    pathk
                        ? "Путь · "
                        : (
                            skillsk
                                ? "Прокачка · "
                                : (
                                    stealthk
                                        ? "Стиль стелса · "
                                        : (
                                            fightk
                                                ? "Стиль боя · "
                                                : (
                                                    zonek
                                                        ? "Зона · "
                                                        : (
                                                            questk
                                                                ? "Квест · "
                                                                : (shopk ? "Магазин · " : (fxk ? "Эффект · " : (cut ? "Катсцена · " : "Интерфейс · ")))
                                                        )
                                                )
                                        )
                                )
                        )
                )
                + tab.title()
                + (tab.isDirty() ? "  •" : ""),
            pathk
                ? Icon.ARROW_RIGHT
                : (
                    skillsk
                        ? tab.icon()
                        : (
                            stealthk
                                ? tab.icon()
                                : (
                                    fightk
                                        ? Icon.BOLT
                                        : (zonek ? Icon.PIN : (questk ? Icon.FLAG : (shopk ? Icon.TAG : (fxk ? Icon.BOLT : (cut ? Icon.FILM : Icon.MENU)))))
                                )
                        )
                ),
            pathk
                ? -21965
                : (
                    skillsk
                        ? -5141249
                        : (
                            stealthk
                                ? -43691
                                : (fightk ? -38037 : (zonek ? -11549705 : (questk ? -11686 : (shopk ? -8658806 : (fxk ? -14006 : (cut ? -28838 : -10638858))))))
                        )
                ),
            tab
        );
        win.insetTop = 26;
        win.insetBottom = 16;
        SprauteScreen.GuiWindow gw = new SprauteScreen.GuiWindow(win, tab);
        win.onClose = () -> {
            if (tab.isDirty()) {
                this.save(tab);
            }

            this.guiWindows.remove(gw);
            if (tab instanceof ZoneTab zt) {
                zt.closed();
            }

            if (tab instanceof PathTab pt) {
                pt.closed();
            }

            if (fxk) {
                FxEngine.stop("__editor_preview", true);
                FxEngine.previewStop();
            }
        };
        this.guiWindows.add(gw);
        this.openOverlay(win, false, false, null);
        if (cut || fxk || shopk || questk || zonek || fightk || pathk) {
            win.toggleMaximize();
        }

        tab.focusContent();
        return tab;
    }

    private void closeGuiWindows() {
        for (SprauteScreen.GuiWindow gw : new ArrayList<>(this.guiWindows)) {
            gw.window().close();
        }
    }

    private EditorTab focusedEditor() {
        Widget f = this.focused();

        for (SprauteScreen.GuiWindow gw : this.guiWindows) {
            if (f != null && gw.window().isAncestorOf(f)) {
                return gw.tab();
            }
        }

        return this.activeTab();
    }

    private List<EditorTab> allEditors() {
        List<EditorTab> all = new ArrayList<>(this.tabs);

        for (SprauteScreen.GuiWindow gw : this.guiWindows) {
            all.add(gw.tab());
        }

        return all;
    }

    private void openWorldEditor() {
        this.toastInfo("Есть только в полной версии Spraute Engine");
    }

    private boolean worldShown() {
        EditorTab t = this.activeTab();
        return t != null && t.showsWorld();
    }

    private void openFileInEditor(Project.FileRef file, Integer offset) {
        EditorTab t = this.openFile(file);
        if (t != null && offset != null) {
            t.reveal(offset);
        }
    }

    private boolean zoneInWindow(Path file) {
        for (SprauteScreen.GuiWindow gw : this.guiWindows) {
            if (gw.tab().file != null && gw.tab().file.path().equals(file)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void removed() {
        for (EditorTab t : new ArrayList<>(this.tabs)) {
            t.setShown(false);
        }

        super.removed();
    }

    private EditorTab openFile(Project.FileRef f) {
        if (f.kind() != Project.Kind.EFFECT
            && f.kind() != Project.Kind.POST
            && f.kind() != Project.Kind.STRUCTURE
            && f.kind() != Project.Kind.FIGHT
            && f.kind() != Project.Kind.STEALTH
            && f.kind() != Project.Kind.SKILLS) {
            if (f.kind() != Project.Kind.GUI
                && f.kind() != Project.Kind.EFFECT
                && f.kind() != Project.Kind.SHOP
                && f.kind() != Project.Kind.QUEST
                && f.kind() != Project.Kind.ZONE
                && f.kind() != Project.Kind.FIGHT
                && f.kind() != Project.Kind.STEALTH
                && f.kind() != Project.Kind.SKILLS
                && f.kind() != Project.Kind.PATH) {
                for (EditorTab t : this.tabs) {
                    if (t.file != null && t.file.path().equals(f.path())) {
                        this.selectTab(t);
                        return t;
                    }
                }

                String content;
                try {
                    content = this.project.read(f);
                } catch (IOException var4) {
                    this.toastError("Не удалось открыть файл: " + var4.getMessage());
                    return null;
                }
                EditorTab tab = (EditorTab)(switch (f.kind()) {
                    case SCRIPT -> new ScriptTab(this.project, f, content, this::playerPosition, this::saveActive);
                    case BLOCKS -> new BlocksTab(this.project, f, content, this::playerPosition, this::convertToText);
                    case GUI -> throw new IllegalStateException("GUIs open in windows");
                    case CUTSCENE -> {
                        if (!this.side.visible) {
                            this.side.visible = true;
                            Workspace.settings().showRight = true;
                            this.relayout();
                        }

                        yield new CutsceneTab(this.project, f, content);
                    }
                    case EFFECT -> throw new IllegalStateException("effects open in windows");
                    case SHOP, QUEST, ZONE, FIGHT, STEALTH, SKILLS, PATH -> throw new IllegalStateException(
                        "shops, quests, zones, fights and paths open in windows"
                    );
                    case POST -> null;
                    case CAMERA -> new CameraTab(this.project, f, content);
                    case STRUCTURE -> null;
                });
                this.addTab(tab);
                return tab;
            } else {
                return this.openGuiWindow(f);
            }
        } else {
            this.toastInfo("Есть только в полной версии Spraute Engine");
            return null;
        }
    }

    private void addTab(EditorTab tab) {
        this.tabs.add(tab);
        this.editorArea.add(tab);
        TabBar.Tab t = this.editorTabs.add(new TabBar.Tab(tab::title, tab.icon(), tab));
        t.iconColor = tab.iconColor();
        t.closable = true;
        t.dirty = tab::isDirty;
        this.selectTab(tab);
    }

    private void openFromText() {
        EditorTab target = this.activeTab();
        if (target instanceof ScriptTab || target instanceof BlocksTab) {
            for (EditorTab tab : this.tabs) {
                if (tab instanceof FromTextTab fromText) {
                    fromText.target(target);
                    this.selectTab(fromText);
                    return;
                }
            }

            this.addTab(new FromTextTab(this.project, target, this::applyDialogue));
        }
    }

    private void applyDialogue(EditorTab target, DialogueImport.Plan plan) {
        if (!this.tabs.contains(target)) {
            throw new IllegalStateException("Исходный скрипт закрыт. Откройте его снова.");
        } else {
            if (target instanceof ScriptTab text) {
                text.editor.replaceAllText(plan.text());
            } else {
                if (!(target instanceof BlocksTab blocks)) {
                    throw new IllegalStateException("Откройте текстовый или блочный скрипт");
                }

                blocks.blocks.appendDialogue(plan.blocks());
            }

            this.selectTab(target);
            this.toastSuccess("Реплики добавлены в «" + target.title() + "»");
        }
    }

    private void selectTab(EditorTab tab) {
        TabBar.Tab t = this.editorTabs.find(tab);
        if (t != null) {
            this.editorTabs.select(t);
        }
    }

    private void showTab(EditorTab tab) {
        if (tab instanceof ScriptTab st) {
            this.lastScript = st;
        }

        this.editorArea.layout();
        tab.focusContent();
    }

    private void closeTab(EditorTab tab) {
        if (tab != null) {
            if (tab.isDirty()) {
                Dialog d = new Dialog("Несохранённые изменения", Icon.WARNING);
                Label l = new Label("Сохранить изменения в «" + tab.title() + "»?").color(-6577995);
                d.body.add(l);
                d.addButton("Не сохранять", Button.Style.DANGER, () -> {
                    d.close();
                    this.removeTab(tab);
                });
                d.addButton("Отмена", Button.Style.DEFAULT, d::close);
                d.addButton("Сохранить", Button.Style.PRIMARY, () -> {
                    d.close();
                    this.save(tab);
                    this.removeTab(tab);
                });
                d.open();
            } else {
                this.removeTab(tab);
            }
        }
    }

    private void removeTab(EditorTab tab) {
        TabBar.Tab t = this.editorTabs.find(tab);
        tab.setShown(false);
        this.tabs.remove(tab);
        this.editorArea.remove(tab);
        if (t != null) {
            this.editorTabs.removeTab(t);
        }

        TabBar.Tab sel = this.editorTabs.selected();
        if (sel != null) {
            this.editorTabs.select(sel);
        }

        this.editorArea.layout();
    }

    private void openDocs() {
        for (EditorTab t : this.tabs) {
            if (t instanceof DocsTab) {
                this.selectTab(t);
                return;
            }
        }

        this.addTab(new DocsTab());
    }

    private void openModel(Project.ModelRef model) {
        for (EditorTab t : this.tabs) {
            if (t instanceof ModelTab mt && mt.model.dir().equals(model.dir())) {
                this.selectTab(t);
                return;
            }
        }

        this.addTab(new ModelTab(this.project, model, this::createEntity));
    }

    private void openEntity(Project.EntityRef entity) {
        for (EditorTab t : this.tabs) {
            if (t instanceof EntityTab et && et.ref.path().equals(entity.path())) {
                this.selectTab(t);
                return;
            }
        }

        this.addTab(new EntityTab(this.project, entity, this::insertCode, this::importModelsDialog));
    }

    private void createEntity(String model) {
        if (this.project != null) {
            Dialog.prompt(
                "Новая сущность",
                "Имя сущности (в сюжете: character x = \"spraute:имя\"):",
                model != null ? model : "Житель",
                v -> v.isBlank()
                    ? "Введите имя"
                    : (Files.exists(this.project.entitiesDir().resolve(Project.sanitize(v) + ".sent")) ? "Такая сущность уже есть" : null),
                v -> {
                    try {
                        Project.EntityRef e = this.project.newEntity(v, model);
                        ModelLibrary.scan();
                        this.explorer.reload();
                        this.openEntity(e);
                    } catch (IOException var4) {
                        this.toastError(var4.getMessage());
                    }
                }
            );
        }
    }

    private void pickFiles(String title, String[] patterns, String description, Consumer<List<Path>> onPicked) {
        Thread t = new Thread(() -> {
            String result;
            try {
                MemoryStack stack = MemoryStack.stackPush();

                try {
                    PointerBuffer filters = stack.mallocPointer(patterns.length);

                    for (String p : patterns) {
                        filters.put(stack.UTF8(p));
                    }

                    filters.flip();
                    result = TinyFileDialogs.tinyfd_openFileDialog(title, "", filters, description, true);
                } catch (Throwable var13) {
                    if (stack != null) {
                        try {
                            stack.close();
                        } catch (Throwable var12) {
                            var13.addSuppressed(var12);
                        }
                    }

                    throw var13;
                }

                if (stack != null) {
                    stack.close();
                }
            } catch (Throwable var14) {
                Minecraft.getInstance().execute(() -> this.toastError("Не удалось открыть выбор файлов. Перетащите файлы прямо в окно редактора"));
                return;
            }

            if (result != null && !result.isBlank()) {
                List<Path> paths = new ArrayList<>();

                for (String p : result.split("\\|")) {
                    if (!p.isBlank()) {
                        paths.add(Path.of(p));
                    }
                }

                Minecraft.getInstance().execute(() -> onPicked.accept(paths));
            }
        }, "Spraute file dialog");
        t.setDaemon(true);
        t.start();
    }

    private void importModelsDialog() {
        if (this.project != null) {
            this.pickFiles(
                "Добавить модель (экспорт Blockbench)",
                new String[]{"*.geo.json", "*.animation.json", "*.png"},
                "Модель .geo.json, текстура .png, анимации .animation.json",
                this::importModels
            );
        }
    }

    private void importAnimationsDialog() {
        if (this.project != null) {
            this.pickFiles(
                "Добавить анимации (экспорт Blockbench)",
                new String[]{"*.animation.json", "*.animations.json"},
                "Анимации .animation.json",
                this::importAnimations
            );
        }
    }

    private void importAnimations(List<Path> paths) {
        if (this.project == null) {
            this.toastError("Сначала откройте проект");
        } else {
            try {
                int n = this.project.importAnimations(paths);
                ModelLibrary.scan();
                this.explorer.reload();
                Path first = paths.stream().filter(Project::isAnimationFile).findFirst().orElse(null);
                String name = first == null ? "" : first.getFileName().toString().replaceAll("(?i)\\.animations?\\.json$", "");
                this.toastSuccess("Анимаций добавлено: " + n + ". В сюжете: anim alice \"" + name + "\"");
            } catch (IOException var5) {
                this.toastError("Не удалось добавить анимации: " + var5.getMessage());
            }
        }
    }

    private void importModels(List<Path> paths) {
        if (this.project == null) {
            this.toastError("Сначала откройте проект");
        } else {
            if (paths.stream().noneMatch(p -> p.getFileName().toString().toLowerCase().endsWith(".geo.json"))
                && paths.stream().anyMatch(Project::isAnimationFile)) {
                this.importAnimations(paths);
                List<Path> rest = paths.stream().filter(p -> !Project.isAnimationFile(p)).toList();
                if (rest.isEmpty()) {
                    return;
                }

                paths = rest;
            }

            try {
                List<Project.ModelRef> made = this.project.importModels(paths);
                ModelLibrary.scan();
                this.explorer.reload();
                if (made.isEmpty()) {
                    this.toastError("Нужен файл модели .geo.json (экспорт Blockbench: Bedrock Geometry)");
                    return;
                }

                if (this.activeTab() instanceof EntityTab et) {
                    et.useModel(made.get(0).name());
                } else {
                    for (Project.ModelRef m : made) {
                        this.openModel(m);
                    }
                }

                this.toastSuccess("Модель добавлена: " + made.get(made.size() - 1).name() + ". В сюжете: \"spraute:" + made.get(made.size() - 1).name() + "\"");
            } catch (IOException var6) {
                this.toastError("Не удалось добавить модель: " + var6.getMessage());
            }
        }
    }

    private void openEngine() {
        if (this.project != null) {
            for (EditorTab t : new ArrayList<>(this.tabs)) {
                if (t instanceof EngineTab) {
                    this.closeTab(t);
                }
            }

            for (SprauteScreen.GuiWindow gw : this.guiWindows) {
                if (gw.tab() instanceof EngineStudio) {
                    gw.window().toFront();
                    gw.tab().focusContent();
                    return;
                }
            }

            EngineStudio tab = new EngineStudio(this.project);
            Window win = new Window(() -> "Настройки движка · " + tab.title() + (tab.isDirty() ? "  •" : ""), Icon.GEAR, tab.iconColor(), tab);
            win.insetTop = 26;
            win.insetBottom = 16;
            SprauteScreen.GuiWindow gw = new SprauteScreen.GuiWindow(win, tab);
            win.onClose = () -> {
                if (tab.isDirty()) {
                    this.save(tab);
                }

                this.guiWindows.remove(gw);
            };
            this.guiWindows.add(gw);
            this.openOverlay(win, false, false, null);
            win.toggleMaximize();
            tab.focusContent();
        }
    }

    private void openRecipes() {
        if (this.project != null) {
            for (SprauteScreen.GuiWindow gw : this.guiWindows) {
                if (gw.tab() instanceof RecipesTab) {
                    gw.window().toFront();
                    gw.tab().focusContent();
                    return;
                }
            }

            RecipesTab tab = new RecipesTab(this.project);
            tab.onSave = () -> this.saveRecipes(tab);
            Window win = new Window(() -> tab.title() + (tab.isDirty() ? "  •" : ""), tab.icon(), tab.iconColor(), tab);
            win.insetTop = 26;
            win.insetBottom = 16;
            SprauteScreen.GuiWindow gw = new SprauteScreen.GuiWindow(win, tab);
            win.onClose = () -> {
                if (tab.isDirty()) {
                    this.save(tab);
                }

                this.guiWindows.remove(gw);
            };
            this.guiWindows.add(gw);
            this.openOverlay(win, false, false, null);
            win.toggleMaximize();
            tab.focusContent();
        }
    }

    private void openMods() {
        this.toastInfo("Есть только в полной версии Spraute Engine");
    }

    private void saveMods(EditorTab tab) {
        this.save(tab);
        if (!tab.isDirty()) {
            this.toastSuccess("Предметы сохранены");
        }
    }

    private void saveRecipes(EditorTab tab) {
        this.save(tab);
        if (!tab.isDirty()) {
            this.toastSuccess("Рецепты сохранены и отправлены в мир");
        }
    }

    private void openAssets(AssetsTab.Mode mode) {
        for (EditorTab t : this.tabs) {
            if (t instanceof AssetsTab at) {
                at.setMode(mode);
                this.selectTab(t);
                return;
            }
        }

        AssetsTab at = new AssetsTab(this::insertCode, this::defaultCharacter);
        at.setMode(mode);
        this.addTab(at);
    }

    private void openImage(Project.ImageRef image) {
        for (SprauteScreen.GuiWindow gw : this.guiWindows) {
            if (gw.tab() instanceof TextureEditor te && te.image.path().equals(image.path())) {
                gw.window().toFront();
                te.focusContent();
                return;
            }
        }

        TextureEditor tab = new TextureEditor(this.project, image);
        Window win = new Window(() -> "Текстура · " + tab.title() + (tab.isDirty() ? "  •" : ""), Icon.IMAGE, -10638858, tab);
        win.insetTop = 26;
        win.insetBottom = 16;
        SprauteScreen.GuiWindow gw = new SprauteScreen.GuiWindow(win, tab);
        win.onClose = () -> {
            if (tab.isDirty()) {
                tab.save();
            }

            tab.closed();
            this.guiWindows.remove(gw);
        };
        this.guiWindows.add(gw);
        this.openOverlay(win, false, false, null);
        win.toggleMaximize();
        tab.focusContent();
    }

    private void newImageDialog() {
        if (this.project != null) {
            Dialog d = new Dialog("Новая картинка", Icon.IMAGE);
            d.body.add(new Label("Имя картинки:").color(-6577995));
            TextField name = new TextField();
            name.setText("картинка");
            name.selectAll();
            d.body.add(name);
            int[][] sizes = new int[][]{{16, 16}, {32, 32}, {64, 64}, {128, 64}, {64, 32}, {128, 128}, {256, 256}};
            int[] pick = new int[]{3};
            d.body
                .add(
                    new Dropdown<>(
                        () -> List.of(0, 1, 2, 3, 4, 5, 6),
                        () -> pick[0],
                        i -> sizes[i][0] + "×" + sizes[i][1] + (i == 2 ? " — скин Minecraft" : (i == 3 ? " — персонаж" : (i == 0 ? " — иконка, предмет" : ""))),
                        i -> pick[0] = i
                    )
                );
            d.addButton("Отмена", Button.Style.DEFAULT, d::close);
            Runnable ok = () -> {
                String n = name.text().trim();
                if (!n.isEmpty()) {
                    try {
                        Files.createDirectories(this.project.imagesDir());
                        Path p = this.project.imagesDir().resolve(Project.sanitize(n) + ".png");
                        int k = 2;

                        while (Files.exists(p)) {
                            p = this.project.imagesDir().resolve(Project.sanitize(n) + " " + k++ + ".png");
                        }

                        ImageIO.write(new BufferedImage(sizes[pick[0]][0], sizes[pick[0]][1], 2), "png", p.toFile());
                        d.close();
                        Project.ImageRef ref = new Project.ImageRef(p);
                        ProjectImages.load(this.project, ref);
                        this.explorer.reload();
                        this.openImage(ref);
                    } catch (IOException var9) {
                        this.toastError(var9.getMessage());
                    }
                }
            };
            d.addButton("Создать", Button.Style.PRIMARY, ok);
            d.onEnter = ok;
            d.open();
        }
    }

    private void importImagesDialog() {
        if (this.project != null) {
            String[] patterns = new String[Project.IMAGE_EXTENSIONS.size()];

            for (int i = 0; i < patterns.length; i++) {
                patterns[i] = "*" + Project.IMAGE_EXTENSIONS.get(i);
            }

            this.pickFiles("Добавить картинки в проект", patterns, "Картинки (PNG, JPG, BMP, GIF)", this::importImages);
        }
    }

    private Project.ImageRef makeFaceSkin(Path source, String name) {
        if (this.project == null) {
            return null;
        } else {
            try {
                Path out = FaceSkin.convertFile(source, this.project.imagesDir(), Project.sanitize(name) + " лицо");
                Project.ImageRef ref = new Project.ImageRef(out);
                ProjectImages.load(this.project, ref);
                this.explorer.reload();
                this.toastSuccess("Текстура Spraute «" + ref.name() + "» готова: глаза, брови и рот теперь двигаются");
                return ref;
            } catch (IOException var5) {
                this.toastError(var5.getMessage());
                return null;
            }
        }
    }

    public static void pickFaceSkin(Consumer<String> done) {
        SprauteScreen s = instance;
        if (s != null && s.project != null) {
            s.pickFiles("Скин Minecraft или Super Rig", new String[]{"*.png"}, "Скин PNG", paths -> {
                for (Path p : paths) {
                    String n = p.getFileName().toString();
                    FaceSkinDialog.open(s.project, p, n.endsWith(".png") ? n.substring(0, n.length() - 4) : n, r -> {
                        s.explorer.reload();
                        done.accept(r.name());
                    });
                }
            });
        }
    }

    private static boolean looksLikeSkin(Path p) {
        try {
            BufferedImage img = ImageIO.read(p.toFile());
            return img != null && FaceSkin.isSkin(img);
        } catch (Exception var2) {
            return false;
        }
    }

    private void importImages(List<Path> paths) {
        if (this.project == null) {
            this.toastError("Сначала откройте проект");
        } else {
            Project.ImageRef last = null;
            int n = 0;
            int skipped = 0;

            for (Path p : paths) {
                if (Project.isImage(p) && Files.isRegularFile(p)) {
                    try {
                        last = this.project.importImage(p);
                        if (ProjectImages.load(this.project, last) == null) {
                            this.toastError("Не удалось прочитать " + p.getFileName());
                        }

                        if (p.getFileName().toString().toLowerCase().endsWith(".png") && looksLikeSkin(last.path())) {
                            this.makeFaceSkin(last.path(), last.name());
                        }

                        n++;
                    } catch (IOException var8) {
                        this.toastError("Ошибка копирования " + p.getFileName() + ": " + var8.getMessage());
                    }
                } else {
                    skipped++;
                }
            }

            this.explorer.reload();
            if (n > 0) {
                this.toastSuccess(n == 1 ? "Картинка «" + last.name() + "» добавлена" : "Добавлено картинок: " + n);
            }

            if (skipped > 0) {
                this.toastInfo("Пропущено файлов (не картинки): " + skipped);
            }

            if (n == 1) {
                this.openImage(last);
            }
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        List<Path> videos = paths.stream().filter(Project::isVideo).toList();
        if (!videos.isEmpty()) {
            this.importVideos(videos);
            paths = paths.stream().filter(p -> !Project.isVideo(p)).toList();
            if (paths.isEmpty()) {
                return;
            }
        }

        boolean model = paths.stream().anyMatch(Project::isModelFile);
        Predicate<Path> audio = p -> Project.isSound(p) || SoundConvert.convertible(p);
        List<Path> sounds = paths.stream().filter(audio).toList();
        if (!sounds.isEmpty()) {
            this.importSounds(sounds);
            paths = paths.stream().filter(audio.negate()).toList();
            if (paths.isEmpty()) {
                return;
            }
        }

        if (model) {
            this.importModels(paths);
        } else {
            this.importImages(paths);
        }
    }

    private void importVideos(List<Path> paths) {
        if (this.project == null) {
            this.toastError("Сначала откройте проект");
        } else {
            Project target = this.project;
            Thread t = new Thread(() -> {
                int done = 0;
                int skipped = 0;
                String failed = null;

                for (Path p : paths) {
                    if (Project.isVideo(p) && Files.isRegularFile(p)) {
                        try {
                            target.importVideo(p);
                            done++;
                        } catch (IOException var9) {
                            failed = var9.getMessage();
                        }
                    } else {
                        skipped++;
                    }
                }

                int count = done;
                int left = skipped;
                String error = failed;
                Minecraft.getInstance().execute(() -> {
                    this.explorer.reload();
                    if (error != null) {
                        this.toastError("Не удалось добавить видео: " + error);
                    } else if (count > 0) {
                        this.toastSuccess("Видео в проекте: " + count + (left > 0 ? ", пропущено: " + left : ""));
                    } else {
                        this.toastError("Это не видео: подойдут MP4, WebM, MKV, MOV, AVI");
                    }
                });
            }, "Spraute video import");
            t.setDaemon(true);
            t.start();
        }
    }

    private void importSounds(List<Path> paths) {
        if (this.project == null) {
            this.toastError("Сначала откройте проект");
        } else {
            Project target = this.project;
            Project.SoundRef last = null;
            int n = 0;
            int skipped = 0;
            List<Path> convert = new ArrayList<>();

            for (Path p : paths) {
                if (Files.isRegularFile(p)) {
                    if (SoundConvert.convertible(p)) {
                        convert.add(p);
                    } else if (!Project.isSound(p)) {
                        skipped++;
                    } else {
                        try {
                            last = target.importSound(p);
                            n++;
                        } catch (IOException var10) {
                            this.toastError("Ошибка копирования " + p.getFileName() + ": " + var10.getMessage());
                        }
                    }
                }
            }

            this.explorer.reload();
            if (n > 0) {
                this.toastSuccess(n == 1 ? "Звук «" + last.name() + "» добавлен: sound \"" + last.name() + "\"" : "Добавлено звуков: " + n);
            }

            if (skipped > 0) {
                this.toastInfo("Пропущено файлов (не звуки): " + skipped);
            }

            if (!convert.isEmpty()) {
                this.toastInfo(
                    convert.size() == 1 ? "Перевожу «" + convert.get(0).getFileName() + "» в WAV…" : "Перевожу в WAV: " + convert.size() + " файла(ов)…"
                );
                Util.backgroundExecutor()
                    .execute(
                        () -> {
                            for (Path px : convert) {
                                String file = px.getFileName().toString();
                                String base = Project.sanitize(file.substring(0, file.lastIndexOf(46)));
                                String name = base;

                                for (int k = 2;
                                    Files.exists(target.soundsDir().resolve(name + ".wav")) || Files.exists(target.soundsDir().resolve(name + ".ogg"));
                                    k++
                                ) {
                                    name = base + " " + k;
                                }

                                String fname = name;

                                try {
                                    SoundConvert.toWav(px, target.soundsDir(), fname);
                                    Minecraft.getInstance().execute(() -> {
                                        this.explorer.reload();
                                        this.toastSuccess("Звук «" + fname + "» добавлен: sound \"" + fname + "\"");
                                    });
                                } catch (IOException var10x) {
                                    Minecraft.getInstance().execute(() -> this.toastError(file + ": " + var10x.getMessage()));
                                }
                            }
                        }
                    );
            }
        }
    }

    private void renameImage(Project.ImageRef image) {
        Dialog.prompt("Переименовать картинку", "Новое имя (используется в скриптах):", image.name(), v -> v.isBlank() ? "Введите имя" : null, v -> {
            try {
                for (EditorTab t : new ArrayList<>(this.tabs)) {
                    if (t instanceof ImageTab it && it.image.path().equals(image.path())) {
                        this.removeTab(t);
                    }
                }

                ProjectImages.forget(image.path());
                Project.ImageRef renamed = this.project.renameImage(image, v);
                ProjectImages.load(this.project, renamed);
                this.explorer.reload();
                this.toastInfo("Не забудьте обновить icon/portrait \"" + image.name() + "\" в скриптах");
            } catch (IOException var6) {
                this.toastError(var6.getMessage());
            }
        });
    }

    private void deleteImage(Project.ImageRef image) {
        Dialog.confirm("Удалить картинку", "Картинка «" + image.name() + "» будет удалена из проекта.", "Удалить", true, () -> {
            for (EditorTab t : new ArrayList<>(this.tabs)) {
                if (t instanceof ImageTab it && it.image.path().equals(image.path())) {
                    this.removeTab(t);
                }
            }

            ProjectImages.forget(image.path());

            try {
                Files.deleteIfExists(image.path());
            } catch (IOException var5) {
                this.toastError(var5.getMessage());
            }

            this.explorer.reload();
        });
    }

    private void insertCode(String line) {
        ScriptTab target = this.lastScript != null && this.tabs.contains(this.lastScript) ? this.lastScript : null;
        if (target == null) {
            for (EditorTab t : this.tabs) {
                if (t instanceof ScriptTab st) {
                    target = st;
                }
            }
        }

        if (target == null) {
            Minecraft.getInstance().keyboardHandler.setClipboard(line);
            this.toastInfo("Нет открытого текстового скрипта — строка скопирована в буфер");
        } else {
            this.selectTab(target);
            target.editor.insertLine(line);
            this.toastSuccess("Вставлено в " + target.title());
        }
    }

    private String defaultCharacter() {
        ScriptTab target = this.lastScript != null && this.tabs.contains(this.lastScript) ? this.lastScript : null;
        if (target == null) {
            for (EditorTab t : this.tabs) {
                if (t instanceof ScriptTab st) {
                    target = st;
                }
            }
        }

        Analyzer.Result r = target != null ? target.analysis() : null;
        return r != null && !r.characters.isEmpty() ? r.characters.keySet().iterator().next() : "alice";
    }

    private void openDocsWithoutProject() {
        DocsTab docs = new DocsTab();
        Dialog d = new Dialog("Справочник языка", Icon.BOOK).width(Math.min(700, this.vw - 40));
        docs.prefH = Math.min(380, this.vh - 120);
        d.body.add(docs);
        d.addButton("Закрыть", Button.Style.PRIMARY, d::close);
        d.open();
    }

    private void exportMod() {
        if (this.project != null) {
            this.saveAllByHand();
            ExportDialog.show(this.project, this::toastError, this::toastSuccess);
        }
    }

    private void createGeneral(boolean blocks) {
        if (this.project != null) {
            Dialog.prompt(
                blocks ? "Новый общий блочный скрипт" : "Новый общий скрипт",
                "Работает с запуска игры, ещё до мира. Имя файла:",
                "меню",
                v -> {
                    if (v.isEmpty()) {
                        return "Введите имя";
                    } else {
                        Path p = blocks
                            ? this.project.blocksDir().resolve(Project.sanitize(v) + ".svs")
                            : this.project.scriptsDir().resolve(Project.sanitize(v) + ".sst");
                        return Files.exists(p) ? "Такой файл уже есть" : null;
                    }
                },
                v -> {
                    try {
                        Project.FileRef f = this.project.newGeneral(v, blocks);
                        this.explorer.reload();
                        this.openFile(f);
                        GeneralRuntime.reload(this.project.name());
                    } catch (IOException var4) {
                        this.toastError(var4.getMessage());
                    }
                }
            );
        }
    }

    private void createScript(boolean blocks) {
        if (this.project != null) {
            Dialog.prompt(blocks ? "Новый блочный скрипт" : "Новый текстовый скрипт", "Имя файла:", blocks ? "блоки" : "сюжет", v -> {
                if (v.isEmpty()) {
                    return "Введите имя";
                } else {
                    String ext = blocks ? ".svs" : ".sst";
                    Path p = (blocks ? this.project.blocksDir() : this.project.scriptsDir()).resolve(Project.sanitize(v) + ext);
                    return Files.exists(p) ? "Такой файл уже есть" : null;
                }
            }, v -> {
                try {
                    Project.FileRef f = this.placed(blocks ? this.project.newBlocks(v) : this.project.newScript(v));
                    this.explorer.reload();
                    this.openFile(f);
                } catch (IOException var4) {
                    this.toastError(var4.getMessage());
                }
            });
        }
    }

    private Map<String, List<String>> projectGuis() {
        if (this.project == null) {
            return Map.of();
        } else {
            long now = System.currentTimeMillis();
            if (now - this.guiCacheTime < 1000L) {
                return this.guiCache;
            } else {
                this.guiCacheTime = now;
                Map<String, List<String>> map = new LinkedHashMap<>();
                this.project.guiSources().forEach((name, json) -> map.put(name, GuiDoc.fromJson(json).ids()));

                for (SprauteScreen.GuiWindow gw : this.guiWindows) {
                    if (gw.tab() instanceof GuiTab gt) {
                        map.put(gt.file.name(), gt.elementIds());
                    }
                }

                this.guiCache = map;
                return map;
            }
        }
    }

    private void createSkills() {
        if (this.project != null) {
            Dialog.prompt(
                "Новая система прокачки",
                "Имя системы (в сюжете: skills open \"имя\"). Начнётся как дерево РПГ — кнопка «Шаблон…» даст другие.",
                "навыки",
                v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.skillsDir().resolve(Project.sanitize(v) + Project.Kind.SKILLS.ext)) ? "Такая система уже есть" : null;
                    }
                },
                v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newSkills(v));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private void createStealth() {
        if (this.project != null) {
            Dialog.prompt(
                "Новый стиль стелса", "Имя стиля (в сюжете: stealth guard bob style \"имя\"). Настройте один раз и давайте любым персонажам.", "страж", v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.stealthsDir().resolve(Project.sanitize(v) + Project.Kind.STEALTH.ext)) ? "Такой стиль уже есть" : null;
                    }
                }, v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newStealth(v));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private void createFight() {
        if (this.project != null) {
            Dialog.prompt(
                "Новый стиль боя",
                "Имя стиля (в сюжете: fight alice style \"имя\"). Начнётся как кулачный бой — можно взять и другой шаблон.",
                "мечник",
                v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.fightsDir().resolve(Project.sanitize(v) + Project.Kind.FIGHT.ext)) ? "Такой стиль уже есть" : null;
                    }
                },
                v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newFight(v));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private void setPercent(int pc) {
        Workspace.settings().uiPercent = Math.max(0, Math.min(300, pc));
        Workspace.saveSettings();
        this.setUiScale(Workspace.settings().physicalScale());
        this.relayout();
    }

    private String scaleText() {
        int pc = Workspace.settings().uiPercent;
        return pc <= 0 ? "как в Minecraft" : pc + "%";
    }

    private void scaleDialog() {
        int cur = Workspace.settings().uiPercent;
        Dialog.prompt("Масштаб интерфейса", "От 10 до 300 процентов (100 — как раньше «2x», 0 — как в Minecraft):", String.valueOf(Math.max(0, cur)), v -> {
            try {
                int n = Integer.parseInt(v.trim().replace("%", ""));
                return n != 0 && (n < 10 || n > 300) ? "Число от 10 до 300 (или 0)" : null;
            } catch (NumberFormatException var2) {
                return "Нужно число";
            }
        }, v -> this.setPercent(Integer.parseInt(v.trim().replace("%", ""))));
    }

    private void createPath() {
        if (this.project != null) {
            Dialog.prompt("Новый путь", "Имя пути (в сюжете: walk alice \"имя\"). Появится рядом с вами; потом — «Редактировать в мире».", "обход", v -> {
                if (v.isBlank()) {
                    return "Введите имя";
                } else {
                    return Files.exists(this.project.pathsDir().resolve(Project.sanitize(v) + Project.Kind.PATH.ext)) ? "Такой путь уже есть" : null;
                }
            }, v -> {
                try {
                    LocalPlayer pl = Minecraft.getInstance().player;
                    Project.FileRef f = this.placed(this.project.newPath(v, pl != null ? pl.position() : Vec3.ZERO));
                    this.explorer.reload();
                    this.openFile(f);
                } catch (IOException var4) {
                    this.toastError(var4.getMessage());
                }
            });
        }
    }

    private void createZone() {
        if (this.project != null) {
            Dialog.prompt("Новая зона", "Имя зоны (в сюжете: on zoneenter \"имя\"). Появится вокруг вас; потом — «Редактировать в мире».", "деревня", v -> {
                if (v.isBlank()) {
                    return "Введите имя";
                } else {
                    return Files.exists(this.project.zonesDir().resolve(Project.sanitize(v) + Project.Kind.ZONE.ext)) ? "Такая зона уже есть" : null;
                }
            }, v -> {
                try {
                    LocalPlayer pl = Minecraft.getInstance().player;
                    Project.FileRef f = this.placed(this.project.newZone(v, pl != null ? pl.position() : Vec3.ZERO));
                    this.explorer.reload();
                    this.openFile(f);
                } catch (IOException var4) {
                    this.toastError(var4.getMessage());
                }
            });
        }
    }

    private void createQuest() {
        if (this.project != null) {
            Dialog.prompt(
                "Новый квест", "Имя квеста (в сюжете: quest start \"имя\"). Начнётся с примера: задачи, счётчик, скрытая задача и награда.", "ворота", v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.questsDir().resolve(Project.sanitize(v) + Project.Kind.QUEST.ext)) ? "Такой квест уже есть" : null;
                    }
                }, v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newQuest(v));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private void createShop() {
        if (this.project != null) {
            Dialog.prompt("Новый магазин", "Имя магазина (в сюжете: shop open \"имя\"). Начнётся с примера: еда, снаряжение, скупка и обмен.", "лавка", v -> {
                if (v.isBlank()) {
                    return "Введите имя";
                } else {
                    return Files.exists(this.project.shopsDir().resolve(Project.sanitize(v) + Project.Kind.SHOP.ext)) ? "Такой магазин уже есть" : null;
                }
            }, v -> {
                try {
                    Project.FileRef f = this.placed(this.project.newShop(v));
                    this.explorer.reload();
                    this.openFile(f);
                } catch (IOException var3) {
                    this.toastError(var3.getMessage());
                }
            });
        }
    }

    private void importStructureFiles(List<Path> paths) {
        if (this.project != null) {
            Project.FileRef last = null;

            for (Path p : paths) {
                try {
                    String n = p.getFileName().toString();
                    last = this.project.newStructure(n.substring(0, n.lastIndexOf(46)), StructureImport.read(p));
                } catch (RuntimeException | IOException var6) {
                    this.toastError("Не удалось прочитать " + p.getFileName() + ": " + var6.getMessage());
                }
            }

            this.explorer.reload();
            if (last != null) {
                this.openFile(last);
            }
        }
    }

    private void createCamera() {
        if (this.project != null) {
            Dialog.prompt(
                "Новая камера игрока", "Имя (в сюжете: camera rig \"имя\"). Откроется вместе с миром: настройки — в правой панели.", "моя камера", v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.camerasDir().resolve(Project.sanitize(v) + Project.Kind.CAMERA.ext)) ? "Такая камера уже есть" : null;
                    }
                }, v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newCamera(v, "third_person"));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private void createPost() {
        if (this.project != null) {
            Dialog.prompt("Новый пост-эффект", "Имя (в сюжете: post play \"имя\"). Откроется вместе с миром: настройки — в правой панели.", "мой вид", v -> {
                if (v.isBlank()) {
                    return "Введите имя";
                } else {
                    return Files.exists(this.project.postsDir().resolve(Project.sanitize(v) + Project.Kind.POST.ext)) ? "Такой пост-эффект уже есть" : null;
                }
            }, v -> {
                try {
                    Project.FileRef f = this.placed(this.project.newPost(v, "cinematic"));
                    this.explorer.reload();
                    this.openFile(f);
                } catch (IOException var3) {
                    this.toastError(var3.getMessage());
                }
            });
        }
    }

    private void createEffect() {
        if (this.project != null) {
            Dialog.prompt(
                "Новый эффект",
                "Имя эффекта (в сюжете: fx play \"имя\" at player). Начнётся с блёсток — в редакторе можно взять любой шаблон: ядерный взрыв, луч в небо, орбы…",
                "мой эффект",
                v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.effectsDir().resolve(Project.sanitize(v) + Project.Kind.EFFECT.ext)) ? "Такой эффект уже есть" : null;
                    }
                },
                v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newEffect(v, "sparkles"));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private void createCutscene() {
        if (this.project != null) {
            Dialog.prompt(
                "Новая катсцена",
                "Имя катсцены (в сюжете: cutscene play \"имя\"):",
                "вступление",
                v -> {
                    if (v.isBlank()) {
                        return "Введите имя";
                    } else {
                        return Files.exists(this.project.cutscenesDir().resolve(Project.sanitize(v) + Project.Kind.CUTSCENE.ext))
                            ? "Такая катсцена уже есть"
                            : null;
                    }
                },
                v -> {
                    try {
                        Project.FileRef f = this.placed(this.project.newCutscene(v));
                        this.explorer.reload();
                        this.openFile(f);
                    } catch (IOException var3) {
                        this.toastError(var3.getMessage());
                    }
                }
            );
        }
    }

    private Project.FileRef placed(Project.FileRef f) throws IOException {
        Path dir = this.createIn;
        this.createIn = null;
        return dir == null ? f : this.project.moveTo(f, dir);
    }

    private void createGui() {
        if (this.project != null) {
            Dialog.prompt("Новый интерфейс", "Имя интерфейса (по нему сюжет открывает его: gui open \"имя\"):", "меню", v -> {
                if (v.isBlank()) {
                    return "Введите имя";
                } else {
                    return Files.exists(this.project.guisDir().resolve(Project.sanitize(v) + Project.Kind.GUI.ext)) ? "Такой интерфейс уже есть" : null;
                }
            }, v -> {
                try {
                    Project.FileRef f = this.placed(this.project.newGui(v));
                    this.explorer.reload();
                    this.openFile(f);
                } catch (IOException var3) {
                    this.toastError(var3.getMessage());
                }
            });
        }
    }

    private void convertToText(String code) {
        if (this.project != null) {
            EditorTab src = this.activeTab();
            String base = src != null ? src.title() + " (текст)" : "из блоков";

            try {
                Project.FileRef f = new Project.FileRef(this.project.scriptsDir().resolve(Project.sanitize(base) + ".sst"), Project.Kind.SCRIPT);
                int n = 2;

                while (Files.exists(f.path())) {
                    f = new Project.FileRef(this.project.scriptsDir().resolve(Project.sanitize(base) + " " + n++ + ".sst"), Project.Kind.SCRIPT);
                }

                this.project.write(f, code);
                this.explorer.reload();
                this.openFile(f);
                this.toastInfo("Создан текстовый скрипт. Не забудьте сменить id, если оставляете оба.");
            } catch (IOException var6) {
                this.toastError(var6.getMessage());
            }
        }
    }

    private void renameFile(Project.FileRef f) {
        Dialog.prompt("Переименовать", "Новое имя:", f.name(), v -> v.isEmpty() ? "Введите имя" : null, v -> {
            try {
                EditorTab open = null;

                for (EditorTab t : this.tabs) {
                    if (t.file != null && t.file.path().equals(f.path())) {
                        open = t;
                    }
                }

                for (SprauteScreen.GuiWindow gw : new ArrayList<>(this.guiWindows)) {
                    if (gw.tab().file != null && gw.tab().file.path().equals(f.path())) {
                        open = gw.tab();
                        gw.window().close();
                    }
                }

                if (open != null) {
                    this.save(open);
                    if (this.tabs.contains(open)) {
                        this.removeTab(open);
                    }
                }

                Project.FileRef nf = this.project.rename(f, v);
                this.explorer.reload();
                if (open != null) {
                    this.openFile(nf);
                }
            } catch (IOException var6) {
                this.toastError(var6.getMessage());
            }
        });
    }

    private void deleteFile(Project.FileRef f) {
        Dialog.confirm("Удалить файл", "Файл «" + f.fileName() + "» будет удалён.", "Удалить", true, () -> {
            for (EditorTab t : new ArrayList<>(this.tabs)) {
                if (t.file != null && t.file.path().equals(f.path())) {
                    this.removeTab(t);
                }
            }

            for (SprauteScreen.GuiWindow gw : new ArrayList<>(this.guiWindows)) {
                if (gw.tab().file != null && gw.tab().file.path().equals(f.path())) {
                    this.guiWindows.remove(gw);
                    this.closeOverlay(gw.window());
                }
            }

            try {
                this.project.delete(f);
            } catch (IOException var4) {
                this.toastError(var4.getMessage());
            }

            this.explorer.reload();
        });
    }

    private void openScriptById(int id) {
        if (this.project != null) {
            for (EditorTab t : this.tabs) {
                if (t.scriptId() == id) {
                    this.selectTab(t);
                    return;
                }
            }

            for (Project.FileRef f : this.project.files()) {
                if (f.kind() != Project.Kind.GUI
                    && f.kind() != Project.Kind.CUTSCENE
                    && f.kind() != Project.Kind.EFFECT
                    && f.kind() != Project.Kind.SHOP
                    && f.kind() != Project.Kind.QUEST
                    && f.kind() != Project.Kind.POST
                    && f.kind() != Project.Kind.CAMERA
                    && f.kind() != Project.Kind.STRUCTURE
                    && f.kind() != Project.Kind.ZONE
                    && f.kind() != Project.Kind.FIGHT
                    && f.kind() != Project.Kind.STEALTH
                    && f.kind() != Project.Kind.SKILLS
                    && f.kind() != Project.Kind.PATH) {
                    try {
                        String text = this.project.read(f);
                        int fileId = -1;
                        if (f.kind() == Project.Kind.SCRIPT) {
                            Ast.Header header = Parser.parse(text).program.header;
                            if (header != null) {
                                fileId = header.id;
                            }
                        } else {
                            VisualScript.Stack main = VisualScript.fromJson(text).mainStack();
                            if (main != null) {
                                fileId = Integer.parseInt(main.blocks.get(0).field("id").trim());
                            }
                        }

                        if (fileId == id) {
                            this.openFile(f);
                            return;
                        }
                    } catch (Exception var7) {
                    }
                }
            }

            this.toastInfo("Файл сюжета #" + id + " не найден в текущем проекте");
        }
    }

    private void save(EditorTab t) {
        try {
            t.save();
            if (this.project != null && t.source() == null) {
                ClientState.get().sources(this.project.name(), this.project.deploySources());
            }
        } catch (IOException var3) {
            this.toastError("Ошибка сохранения: " + var3.getMessage());
        }
    }

    private void saveActive() {
        EditorTab t = this.focusedEditor();
        if (t instanceof RecipesTab) {
            this.saveRecipes(t);
        } else {
            if (t != null && t.file != null) {
                this.save(t);
                this.toastSuccess("Сохранено: " + t.title());
                this.uploadSaved(t);
            }
        }
    }

    private void saveAll() {
        for (EditorTab t : this.allEditors()) {
            if (t.isDirty()) {
                this.save(t);
            }
        }

        Workspace.saveSettings();
    }

    private void saveAllByHand() {
        List<EditorTab> changed = new ArrayList<>();

        for (EditorTab t : this.allEditors()) {
            if (t.isDirty()) {
                changed.add(t);
            }
        }

        this.saveAll();

        for (EditorTab tx : changed) {
            this.uploadSaved(tx);
        }
    }

    private void uploadSaved(EditorTab t) {
        if (this.project != null && t.file != null && t.source() != null && Minecraft.getInstance().getConnection() != null) {
            EngineConfig.Upload upload = EngineTab.load(this.project).upload;
            if (upload.onSave && !t.problems().stream().anyMatch(p -> p.severity() == LanguageSupport.Severity.ERROR)) {
                Packets.ScriptEntry running = ClientState.get().script(t.scriptId());
                this.deploy(t, upload.restart && running != null && running.active);
            }
        }
    }

    private void uploadOnConnect() {
        Object connection = Minecraft.getInstance().getConnection();
        if (connection != null && this.project != null) {
            if (connection != uploadedConnection.get() || !this.project.dir.equals(uploadedProject)) {
                uploadedConnection = new WeakReference<>(connection);
                uploadedProject = this.project.dir;
                if (EngineTab.load(this.project).upload.onConnect) {
                    this.uploadProject();
                }
            }
        }
    }

    private void uploadProject() {
        List<String> stories = new ArrayList<>();
        int broken = 0;

        for (Project.FileRef f : this.project.files()) {
            if (f.kind() == Project.Kind.SCRIPT || f.kind() == Project.Kind.BLOCKS) {
                try {
                    String text = this.project.read(f);
                    boolean bad;
                    if (f.kind() == Project.Kind.BLOCKS) {
                        BlockCodegen.Result r = BlockCodegen.generate(VisualScript.fromJson(text));
                        text = r.text;
                        bad = r.hasErrors();
                    } else {
                        bad = Analyzer.analyze(text).hasErrors();
                    }

                    if (bad) {
                        broken++;
                        ClientState.get().addLocal(3, "Автозагрузка", "Не загружен, в нём ошибки: " + f.fileName());
                    } else if (!GeneralLang.isGeneral(text)) {
                        stories.add(text);
                    }
                } catch (IOException var8) {
                    broken++;
                    ClientState.get().addLocal(3, "Автозагрузка", "Не прочитан " + f.fileName() + ": " + var8.getMessage());
                }
            }
        }

        ProjectImages.register(this.project);
        GeneralRuntime.reload(this.project.name());
        this.sendEngineSettings();
        ClientState.get().deployAll(this.project.name(), stories, this.project.deploySources());
        ClientState.get().addLocal(0, "Автозагрузка", "Сюжетов отправлено: " + stories.size() + (broken > 0 ? ", не загружено из-за ошибок: " + broken : ""));
    }

    private boolean canRun() {
        EditorTab t = this.activeTab();
        return t != null && t.source() != null && (Minecraft.getInstance().getConnection() != null || GeneralLang.isGeneral(t.source()));
    }

    private void deployActive(boolean start) {
        EditorTab t = this.activeTab();
        if (t != null) {
            this.deploy(t, start);
        }
    }

    private void deploy(EditorTab t, boolean start) {
        if (t.source() != null && this.project != null) {
            if (t.file != null && t.isDirty()) {
                this.save(t);
            }

            long errors = t.problems().stream().filter(p -> p.severity() == LanguageSupport.Severity.ERROR).count();
            if (errors > 0L) {
                this.toastError("В скрипте ошибок: " + errors + ". Исправьте их перед запуском.");
                this.showBottom(true);
                this.bottomTabs.select(this.problemsTab);
            } else {
                ProjectImages.register(this.project);
                if (GeneralLang.isGeneral(t.source())) {
                    GeneralRuntime.reload(this.project.name());
                    this.explorer.reload();
                    this.toastSuccess("Общие скрипты перезапущены");
                } else {
                    ClientState.get().addLocal(0, "Spraute Engine", (start ? "Запуск " : "Загрузка ") + t.title() + "…");
                    this.sendEngineSettings();
                    ClientState.get().deploy(this.project.name(), t.source(), start, this.project.deploySources());
                }
            }
        }
    }

    private void sendEngineSettings() {
        Path engine = EngineTab.file(this.project);
        if (Files.exists(engine)) {
            EngineConfig ec = EngineTab.load(this.project);
            ec.project = this.project.name();
            ClientState.get().engine(ec.toJson());
        }
    }

    private void activeCommand(String pattern) {
        EditorTab t = this.activeTab();
        if (t != null && t.scriptId() > 0) {
            ClientState.get().command(String.format(pattern, t.scriptId()));
        } else {
            this.toastError("У скрипта нет корректного id");
        }
    }

    private void togglePauseActive() {
        EditorTab t = this.activeTab();
        if (t != null) {
            Packets.ScriptEntry e = ClientState.get().script(t.scriptId());
            this.activeCommand(e != null && e.paused ? "script resume %d" : "script pause %d");
        }
    }

    private void gotoLabelActive() {
        EditorTab t = this.activeTab();
        if (t != null) {
            Analyzer.Result r = t.analysis();
            PopupMenu pm = new PopupMenu();
            Packets.ScriptEntry e = ClientState.get().script(t.scriptId());
            boolean active = e != null && e.active;
            if (r == null || r.labels.isEmpty()) {
                pm.item("В скрипте нет меток (label)", null).enabled(() -> false);
            }

            if (r != null) {
                for (String label : r.labels.keySet()) {
                    pm.item(label, Icon.TAG, null, () -> ClientState.get().command((active ? "script goto " : "script start ") + t.scriptId() + " " + label))
                        .color(-33098);
                }
            }

            pm.show(Math.max(0, this.mouseX() - 40), 26);
        }
    }

    private String playerPosition() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? null : "(" + (int)Math.floor(p.getX()) + ", " + (int)Math.floor(p.getY()) + ", " + (int)Math.floor(p.getZ()) + ")";
    }

    private List<EditorTab.Problem> collectProblems() {
        List<EditorTab.Problem> list = new ArrayList<>();

        for (EditorTab t : this.tabs) {
            list.addAll(t.problems());
        }

        list.sort((a, b) -> Integer.compare(a.severity().ordinal(), b.severity().ordinal()));
        return list;
    }

    @Override
    public void tick() {
        if (this.createIn != null && UI.get() != null && !UI.get().hasModal()) {
            this.createIn = null;
        }

        super.tick();
        if (++this.tickCounter % 40 == 0 && Minecraft.getInstance().getConnection() != null && ClientState.get().statusAge() > 1800L) {
            ClientState.get().requestStatus();
        }

        if (this.tickCounter % 1200 == 0) {
            this.saveAll();
        }

        if (this.tickCounter % 40 == 0) {
            this.uploadOnConnect();
        }

        if (this.tickCounter % 40 == 0 && this.project != null) {
            int before = this.imageCount;
            ProjectImages.register(this.project);
            ModelLibrary.scan();
            this.imageCount = this.project.images().size();
            if (before != this.imageCount) {
                this.explorer.reload();
            }
        }
    }

    @Override
    public void onClose() {
        this.saveAll();
        if (this.project != null) {
            this.project.meta.openFiles.clear();

            for (EditorTab t : this.allEditors()) {
                if (t.file != null) {
                    this.project.meta.openFiles.add(this.project.dir.relativize(t.file.path()).toString().replace('\\', '/'));
                }
            }

            this.project.saveMeta();
        }

        Workspace.saveSettings();
        super.onClose();
    }

    @Override
    protected boolean onShortcut(int key, int mods) {
        boolean ctrl = ctrl(mods);
        boolean shift = shift(mods);
        boolean alt = alt(mods);
        if (!ctrl || alt || key != 61 && key != 334 && key != 45 && key != 333) {
            if (key == 256) {
                this.onClose();
                return true;
            } else if (key == 294) {
                if (shift) {
                    this.activeCommand("script stop %d");
                } else if (this.canRun()) {
                    this.deployActive(true);
                }

                return true;
            } else if (key == 295 && this.canRun()) {
                this.deployActive(false);
                return true;
            } else if (!ctrl && !alt && ClientEvents.editorMode() && ClientEvents.isEditorKey(key) && !this.typing()) {
                this.onClose();
                return true;
            } else if (key == 290) {
                if (this.project != null) {
                    this.openDocs();
                } else {
                    this.openDocsWithoutProject();
                }

                return true;
            } else if (!ctrl) {
                return false;
            } else {
                switch (key) {
                    case 65:
                        if (!shift || this.project == null) {
                            return false;
                        }

                        this.openAssets(AssetsTab.Mode.MODELS);
                        break;
                    case 66:
                        if (alt) {
                            this.toggle(this.side, this.centerSplit);
                        } else {
                            this.toggle(this.explorer, this.mainSplit);
                        }
                        break;
                    case 74:
                        this.showBottom(!this.bottom.visible);
                        break;
                    case 78:
                        this.createScript(shift);
                        break;
                    case 83:
                        if (shift) {
                            this.saveAllByHand();
                        } else {
                            this.saveActive();
                        }
                        break;
                    case 87:
                        EditorTab txx = this.focusedEditor();
                        SprauteScreen.GuiWindow win = null;

                        for (SprauteScreen.GuiWindow gw : this.guiWindows) {
                            if (gw.tab() == txx) {
                                win = gw;
                            }
                        }

                        if (win != null) {
                            win.window().close();
                        } else {
                            this.closeTab(this.activeTab());
                        }
                        break;
                    case 89:
                        EditorTab tx = this.focusedEditor();
                        if (tx != null) {
                            tx.redo();
                        }
                        break;
                    case 90:
                        EditorTab t = this.focusedEditor();
                        if (t != null) {
                            if (shift) {
                                t.redo();
                            } else {
                                t.undo();
                            }
                        }
                        break;
                    case 258:
                        if (this.tabs.size() > 1) {
                            int i = this.tabs.indexOf(this.activeTab());
                            this.selectTab(this.tabs.get(Math.floorMod(i + (shift ? -1 : 1), this.tabs.size())));
                        }
                        break;
                    default:
                        return false;
                }

                return true;
            }
        } else {
            int now = Workspace.settings().uiPercent > 0
                ? Workspace.settings().uiPercent
                : (int)Math.round(Minecraft.getInstance().getWindow().getGuiScale() * 50.0);
            boolean up = key == 61 || key == 334;
            this.setPercent(Math.max(10, Math.min(300, now / 10 * 10 + (up ? 10 : -10))));
            this.toastInfo("Масштаб интерфейса: " + Workspace.settings().uiPercent + "%");
            return true;
        }
    }

    private boolean typing() {
        Widget f = this.focused();
        return f instanceof TextField || f instanceof CodeEditor;
    }

    static void log(String msg) {
        SprauteEngine.LOGGER.info("[Spraute] {}", msg);
    }

    public static Supplier<SprauteScreen> current() {
        return () -> instance;
    }

    private final class BottomArea extends Widget {
        @Override
        public void layout() {
            SprauteScreen.this.bottomTabs.bounds(this.x, this.y, this.w, 22);
            TabBar.Tab sel = SprauteScreen.this.bottomTabs.selected();

            for (Widget p : List.of(SprauteScreen.this.problems, SprauteScreen.this.console, SprauteScreen.this.stories)) {
                p.visible = sel != null && sel.data == p;
                p.bounds(this.x, this.y + 22, this.w, this.h - 22);
                p.layout();
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14934490);
        }
    }

    private final class EditorArea extends Widget {
        @Override
        public void layout() {
            SprauteScreen.this.editorTabs.bounds(this.x, this.y, this.w, 24);
            EditorTab t = SprauteScreen.this.activeTab();

            for (EditorTab tab : SprauteScreen.this.tabs) {
                tab.visible = tab == t;
            }

            if (t != null) {
                t.bounds(this.x, this.y + 24, this.w, this.h - 24);
                t.layout();
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            if (SprauteScreen.this.worldShown()) {
                c.fill(this.x, this.y, this.w, 24, -15461093);
            } else {
                c.fill(this.x, this.y, this.w, this.h, -15461093);
            }

            if (SprauteScreen.this.tabs.isEmpty()) {
                int cy = this.y + this.h / 2 - 40;
                c.textCentered("Откройте скрипт в проводнике или создайте новый", this.x + this.w / 2, cy, -6577995);
                String[] hints = new String[]{
                    "Ctrl+N — новый текстовый скрипт", "Ctrl+Shift+N — новый блочный скрипт", "F5 — загрузить и запустить", "F1 — справочник языка"
                };

                for (int i = 0; i < hints.length; i++) {
                    c.textCentered(hints[i], this.x + this.w / 2, cy + 20 + i * 12, -9933435);
                }
            }
        }
    }

    private record GuiWindow(Window window, EditorTab tab) {
    }

    private final class StatusBar extends Widget {
        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, 1435200767);
            c.fill(this.x, this.y, this.w, 1, -13749952);
            int tx = this.x + 8;
            boolean connected = Minecraft.getInstance().getConnection() != null;
            c.round(tx, this.y + 5, 6, 6, 3, connected && ClientState.get().status().ready() ? -11746422 : -870325);
            String conn = !connected ? "Нет мира" : (ClientState.get().status().ready() ? "Движок готов" : "Spraute Engine недоступен");
            c.text(conn, tx + 10, this.y + 4, -1841682);
            tx += c.width(conn) + 22;
            int errors = 0;
            int warnings = 0;

            for (EditorTab.Problem p : SprauteScreen.this.collectProblems()) {
                if (p.severity() == LanguageSupport.Severity.ERROR) {
                    errors++;
                } else if (p.severity() == LanguageSupport.Severity.WARNING) {
                    warnings++;
                }
            }

            c.icon(Icon.ERROR, tx, this.y + 4, errors > 0 ? -1023878 : -6577995);
            c.text(String.valueOf(errors), tx + 12, this.y + 4, -1841682);
            tx += 12 + c.width(String.valueOf(errors)) + 8;
            c.icon(Icon.WARNING, tx, this.y + 4, warnings > 0 ? -870325 : -6577995);
            c.text(String.valueOf(warnings), tx + 12, this.y + 4, -1841682);
            EditorTab t = SprauteScreen.this.activeTab();
            StringBuilder right = new StringBuilder();
            if (t instanceof ScriptTab st) {
                right.append("Стр ").append(st.editor.caretLine() + 1).append(", кол ").append(st.editor.caretCol() + 1);
                if (st.editor.selectionLength() > 0) {
                    right.append(" (выделено ").append(st.editor.selectionLength()).append(")");
                }

                right.append("   SST");
            } else if (t instanceof BlocksTab) {
                right.append("Блоки");
            }

            if (t != null && t.scriptId() > 0) {
                Packets.ScriptEntry e = ClientState.get().script(t.scriptId());
                right.append("   #").append(t.scriptId()).append(e == null ? " не загружен" : (e.active ? (e.paused ? " на паузе" : " запущен") : " загружен"));
            }

            String r = right.toString();
            c.text(r, this.x + this.w - c.width(r) - 8, this.y + 4, -1841682);
        }
    }

    private final class TopBar extends Widget {
        @Override
        public void layout() {
            int logoW = 25 + Minecraft.getInstance().font.width("Spraute Engine") + 12;
            SprauteScreen.this.menu.bounds(this.x + logoW, this.y, SprauteScreen.this.menu.preferredWidth(), this.h);
            int rw = 0;

            for (Widget c : SprauteScreen.this.runBox.children()) {
                rw += Math.max(0, c.prefW) + SprauteScreen.this.runBox.gap;
            }

            SprauteScreen.this.runBox.bounds(this.x + this.w - rw - 4, this.y + 4, rw, this.h - 8);
            SprauteScreen.this.runBox.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -15724266);
            c.fill(this.x, this.y + this.h - 1, this.w, 1, -13749952);
            c.round(this.x + 8, this.y + 7, 12, 12, 3, -7639809);
            c.round(this.x + 12, this.y + 11, 4, 4, 2, -12988235);
            c.text("Spraute Engine", this.x + 25, this.y + 9, -1841682);
            if (SprauteScreen.this.project != null) {
                String name = SprauteScreen.this.project.name();
                EditorTab t = SprauteScreen.this.activeTab();
                String title = t != null ? name + " — " + t.title() : name;
                int tw = c.width(title);
                int cx = this.x + (this.w - tw) / 2;
                if (cx > SprauteScreen.this.menu.x + SprauteScreen.this.menu.w + 10 && cx + tw < SprauteScreen.this.runBox.x - 10) {
                    c.text(title, cx, this.y + 9, -6577995);
                }
            }
        }
    }
}
