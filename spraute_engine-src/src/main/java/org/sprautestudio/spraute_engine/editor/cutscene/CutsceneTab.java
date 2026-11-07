package org.sprautestudio.spraute_engine.editor.cutscene;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.cutscene.ClipSim;
import org.sprautestudio.spraute_engine.cutscene.CutsceneDoc;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.MiniMap;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.editor.SstLanguage;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.editor.visual.BlockCodegen;
import org.sprautestudio.spraute_engine.editor.visual.BlockEditor;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.path.PathDef;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.code.CodeEditor;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class CutsceneTab extends EditorTab {
    private CutsceneDoc doc;
    private String saved;
    private int sel = -1;
    private final Deque<String> undo = new ArrayDeque<>();
    private final Deque<String> redo = new ArrayDeque<>();
    private final CutsceneTab.Form form = new CutsceneTab.Form();
    private final Widget inspector;
    private final CutsceneTab.MapView map = new CutsceneTab.MapView();
    private final CutsceneTab.View3D view = new CutsceneTab.View3D();
    private final CutsceneTab.Preview clipPreview;
    private boolean shown;
    private boolean throughCamera;
    private float savedYaw;
    private float savedPitch;
    private CutsceneDoc.Pose flyPose;
    private String simmed;
    private int simWait;
    private BlockEditor clipBlocks;
    private CutsceneDoc.Clip blocksOf;
    private boolean blocksMode;
    private final CutsceneTab.Timeline timeline = new CutsceneTab.Timeline();
    private final CutsceneTab.ClipPane clipPane;
    private final SstLanguage clipLanguage = new SstLanguage();
    private final CodeEditor clipCode;
    private final Map<String, Boolean> clipErrors = new HashMap<>();
    private int clipSel = -1;
    private final List<Button> tools;
    private Vec3 relAnchor;
    private double playhead = 0.0;
    private long playStarted = -1L;
    private double playFrom;
    private boolean compactTools;
    private String clipStoryName;
    private static final int LABEL_W = 108;
    private static final int TIMELINE_H = 96;
    private int toolbarH = 28;

    public CutsceneTab(Project project, Project.FileRef file, String content) {
        super(project, file);
        this.doc = CutsceneDoc.fromJson(content);
        this.saved = this.doc.toJson();
        this.add(this.map);
        this.add(this.view);
        this.add(this.timeline);
        this.clipPreview = new CutsceneTab.Preview() {};
        this.clipLanguage.clip = true;
        this.clipLanguage.knownScripts = () -> ClientState.get().scriptIds();
        this.clipCode = new CodeEditor("", this.clipLanguage) {
            @Override
            public void focusGained() {
                super.focusGained();
                CutsceneTab.this.snapshot();
            }
        };
        this.clipCode.showMinimap = false;
        this.clipCode.emptyHint = "Что случится в этот момент катсцены. Например: spawn zombie at arena count 5";
        this.clipCode.positionProvider = this::cameraPosition;
        this.clipCode.onChange = e -> {
            CutsceneDoc.Clip cl = this.selectedClip();
            if (cl != null && cl.blocks.isEmpty()) {
                cl.code = e.text();
            }
        };
        this.clipPane = this.add(new CutsceneTab.ClipPane());
        this.clipPane.add(this.clipCode);
        this.findStory();
        this.inspector = new CutsceneTab.Inspector();
        this.tools = List.of(
            this.tool(
                "Редактор в мире",
                Icon.CUBE,
                "Летать камерой по миру и править кадры прямо там: виден весь путь, кадры и куда они смотрят",
                this::openWorldEditor,
                Button.Style.PRIMARY
            ),
            this.tool("Кадр здесь", Icon.PLUS, "Добавить кадр там, куда смотрит камера вида (без мира — где стоит игрок)", this::addHere, Button.Style.DEFAULT),
            this.tool("Запись в игре", Icon.FILM, "Закрыть редактор и летать по миру: K — добавить кадр, N — вернуться", this::record, Button.Style.DEFAULT),
            this.tool("Обновить кадр", Icon.REFRESH, "Поставить выбранный кадр в позицию и взгляд игрока", this::updateFromPlayer, Button.Style.GHOST),
            this.tool("К кадру", Icon.PIN, "Телепортировать игрока в выбранный кадр (посмотреть ракурс)", this::teleport, Button.Style.GHOST),
            this.tool(
                "Скрипт здесь",
                Icon.CODE,
                "Добавить на таймлайн скрипт, который выполнится в момент под зелёной линией",
                () -> this.addClip(this.playhead),
                Button.Style.DEFAULT
            ),
            this.tool("Дублировать", Icon.COPY, "Копия выбранного кадра или скрипта через секунду", this::duplicate, Button.Style.GHOST),
            this.tool("Удалить", Icon.TRASH, "Удалить выбранный кадр или скрипт (Delete)", this::deleteAny, Button.Style.GHOST),
            this.tool(
                "Играть",
                Icon.PLAY,
                "Проиграть таймлайн здесь: персонажи и эффекты из скриптов идут вместе с ним (пробел)",
                this::togglePlay,
                Button.Style.DEFAULT
            ),
            this.toggleTool(
                "Из камеры", Icon.FILM, "Смотреть через камеру катсцены в момент под зелёной линией", () -> this.throughCamera, this::toggleThroughCamera
            ),
            this.tool(
                "Просмотр",
                Icon.PLAY,
                "Проиграть катсцену в игре (пробел — пропустить). Сами скрипты выполняются только в сюжете",
                () -> this.preview(0),
                Button.Style.SUCCESS
            ),
            this.tool("С кадра", Icon.SKIP, "Проиграть в игре начиная с выбранного кадра", () -> this.preview(this.sel), Button.Style.GHOST)
        );
        this.buildForm();
    }

    private Button tool(String text, Icon icon, String tip, Runnable r, Button.Style style) {
        boolean keepsWord = style == Button.Style.SUCCESS;
        Button b = this.add(new Button(() -> this.compactTools && !keepsWord ? "" : text, r).icon(icon).style(style));
        if (text.equals("Редактор в мире") || text.equals("Скрипт здесь")) {
            b.visible = false;
        }

        b.tooltip(text + ": " + tip);
        b.autoWidth();
        return b;
    }

    private Button toggleTool(String text, Icon icon, String tip, BooleanSupplier on, Runnable r) {
        Button b = this.add(new Button(() -> this.compactTools ? "" : text, r).icon(icon).style(Button.Style.GHOST).toggle(on));
        b.visible = false;
        b.tooltip(text + ": " + tip);
        b.autoWidth();
        return b;
    }

    @Override
    public boolean showsWorld() {
        return this.shown;
    }

    @Override
    public boolean wantsRoom() {
        return true;
    }

    @Override
    public Widget inspector() {
        return this.inspector;
    }

    @Override
    public void setShown(boolean want) {
        Minecraft mc = Minecraft.getInstance();
        if (want) {
            if (mc.player != null) {
            }

            want = false;
        }

        if (want != this.shown) {
            this.shown = want;
            if (want) {
                this.savedYaw = mc.player.getYRot();
                this.savedPitch = mc.player.getXRot();
                CutsceneDoc.Pose start = this.flyPose;
                if (start == null) {
                    start = new CutsceneDoc.Pose();
                    if (!this.doc.keys.isEmpty()) {
                        start.set(this.doc.at(this.playhead));
                    } else {
                        Vec3 e = mc.player.getEyePosition(1.0F);
                        start.x = e.x;
                        start.y = e.y;
                        start.z = e.z;
                        start.yaw = this.savedYaw;
                        start.pitch = this.savedPitch;
                    }

                    if (!this.doc.keys.isEmpty()) {
                        Vec3 o = this.origin();
                        start.x = start.x + o.x;
                        start.y = start.y + o.y;
                        start.z = start.z + o.z;
                    }
                }

                CameraDirector.startFly(start);
                this.findStory();
                this.simmed = null;
                this.clipPreview.forgetRoutes();
                this.clipPreview.start();
            } else {
                this.view.stopHolding();
                this.clipPreview.stop();
                this.playStarted = -1L;
                this.flyPose = CameraDirector.pose();
                CameraDirector.stopFly();
                if (mc.player != null) {
                    mc.player.setYRot(this.savedYaw);
                    mc.player.setXRot(this.savedPitch);
                    mc.player.yRotO = this.savedYaw;
                    mc.player.xRotO = this.savedPitch;
                }
            }

            this.map.visible = !this.shown;
            this.view.visible = this.shown;
            this.relayoutTab();
        }
    }

    private void relayoutTab() {
        if (this.w > 0) {
            this.layout();
        }
    }

    boolean seeingThroughCamera() {
        return this.throughCamera;
    }

    CutsceneTab.Preview preview() {
        return this.clipPreview;
    }

    private void openWorldEditor() {
    }

    double playhead() {
        return this.playhead;
    }

    private void toggleThroughCamera() {
        if (this.shown && !this.doc.keys.isEmpty()) {
            this.throughCamera = !this.throughCamera;
            if (!this.throughCamera) {
                CameraDirector.flyFree();
            }
        } else {
            if (ui() != null) {
                ui().toastInfo(this.shown ? "Сначала добавьте кадры" : "Войдите в мир");
            }
        }
    }

    private void showCamera() {
        CutsceneDoc.Pose p = this.doc.at(this.playhead);
        Vec3 o = this.origin();
        p.x = p.x + o.x;
        p.y = p.y + o.y;
        p.z = p.z + o.z;
        String look = this.doc.keys.get(Math.min(p.segment, this.doc.keys.size() - 1)).look;
        Vec3 target = null;
        LocalPlayer pl = Minecraft.getInstance().player;
        if (look.equals("player") && pl != null) {
            target = pl.getEyePosition(1.0F);
        } else if (!look.isEmpty()) {
            target = this.clipPreview.where(look);
        }

        if (target != null) {
            double dx = target.x - p.x;
            double dy = target.y - p.y;
            double dz = target.z - p.z;
            p.yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
            p.pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        }

        CameraDirector.flyAs(p);
    }

    private String cameraPosition() {
        if (this.shown) {
            CutsceneDoc.Pose c = CameraDirector.pose();
            return "(" + (int)Math.floor(c.x) + ", " + (int)Math.floor(c.y) + ", " + (int)Math.floor(c.z) + ")";
        } else {
            LocalPlayer p = Minecraft.getInstance().player;
            return p == null ? null : "(" + (int)Math.floor(p.getX()) + ", " + (int)Math.floor(p.getY()) + ", " + (int)Math.floor(p.getZ()) + ")";
        }
    }

    private void addHere() {
        if (this.shown) {
            this.addFromPose(CameraDirector.pose());
        } else {
            this.addFromPlayer();
        }
    }

    private static List<double[]> noPath() {
        return null;
    }

    private List<double[]> pathPoints(String name) {
        try {
            Path f = this.project.pathsDir().resolve(Project.sanitize(name) + Project.Kind.PATH.ext);
            if (!Files.isRegularFile(f)) {
                return noPath();
            } else {
                PathDef def = new Gson().fromJson(Files.readString(f), PathDef.class);
                List<double[]> out = new ArrayList<>();

                for (PathDef.Point pt : def.points) {
                    out.add(new double[]{pt.x, pt.y, pt.z});
                }

                return out;
            }
        } catch (RuntimeException | IOException var7) {
            return noPath();
        }
    }

    private void updateSim() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            StringBuilder sb = new StringBuilder();

            for (CutsceneDoc.Clip c : this.doc.clips) {
                sb.append(c.t).append('|').append(c.code).append('\n');
            }

            String now = sb.toString();
            if (now.equals(this.simmed)) {
                this.simWait = 0;
            } else if (this.simmed == null || ++this.simWait >= 6) {
                this.simWait = 0;
                this.simmed = now;

                try {
                    ClipSim.Result result = ClipSim.run(
                        this.clipLanguage.clipStory,
                        this.file != null ? this.file.name() : "",
                        this.doc,
                        new double[]{p.getX(), p.getY(), p.getZ()},
                        this::pathPoints,
                        this.clipPreview::route
                    );
                    this.clipPreview.show(result);
                    this.buildForm();
                } catch (RuntimeException var5) {
                    SprauteEngine.LOGGER.warn("Cutscene preview failed", (Throwable)var5);
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.shown) {
            this.updateSim();
            this.clipPreview.seek(this.playhead);
        }
    }

    CutsceneDoc doc() {
        return this.doc;
    }

    Vec3 origin() {
        if (!this.doc.relative) {
            return Vec3.ZERO;
        } else {
            if (this.relAnchor == null) {
                LocalPlayer p = Minecraft.getInstance().player;
                this.relAnchor = p != null ? p.position() : Vec3.ZERO;
            }

            return this.relAnchor;
        }
    }

    CutsceneDoc.Key selectedKey() {
        return this.selected();
    }

    int selectedIndex() {
        return this.selected() != null ? this.sel : -1;
    }

    void selectIndex(int i) {
        this.select(Math.max(-1, Math.min(this.doc.keys.size() - 1, i)));
    }

    void snapshotNow() {
        this.snapshot();
    }

    void changed() {
        this.resort();
        this.buildForm();
    }

    void toggleFlow() {
        this.snapshot();
        this.doc.flow = !this.doc.flow;
        this.buildForm();
    }

    private CutsceneDoc.Key keyOf(CutsceneDoc.Pose p) {
        Vec3 o = this.origin();
        CutsceneDoc.Key k = new CutsceneDoc.Key();
        k.x = round(p.x - o.x);
        k.y = round(p.y - o.y);
        k.z = round(p.z - o.z);
        k.yaw = (float)round(CutsceneDoc.wrap(p.yaw));
        k.pitch = (float)round(p.pitch);
        k.fov = Minecraft.getInstance().options.fov().get().intValue();
        return k;
    }

    void addFromPose(CutsceneDoc.Pose p) {
        CutsceneDoc.Key k = this.keyOf(p);
        this.snapshot();
        CutsceneDoc.Key s = this.selected();
        if (s != null && this.sel < this.doc.keys.size() - 1) {
            CutsceneDoc.Key next = this.doc.keys.get(this.sel + 1);
            k.t = (float)(Math.round((s.t + next.t) / 2.0F * 100.0F) / 100.0);
            k.ease = s.ease;
            k.fov = s.fov;
        } else {
            CutsceneDoc.Key last = this.doc.keys.isEmpty() ? null : this.doc.keys.get(this.doc.keys.size() - 1);
            k.t = last == null ? 0.0F : last.t + 2.0F;
            if (last != null) {
                k.ease = last.ease;
                k.fov = last.fov;
            }
        }

        this.doc.keys.add(k);
        this.resort();
        this.sel = this.doc.keys.indexOf(k);
        this.buildForm();
    }

    void setSelectedTo(CutsceneDoc.Pose p, boolean undoable) {
        CutsceneDoc.Key s = this.selected();
        if (s != null) {
            if (undoable) {
                this.snapshot();
            }

            CutsceneDoc.Key k = this.keyOf(p);
            s.x = k.x;
            s.y = k.y;
            s.z = k.z;
            s.yaw = k.yaw;
            s.pitch = k.pitch;
        }
    }

    private void snapshot() {
        this.undo.push(this.doc.toJson());
        if (this.undo.size() > 100) {
            this.undo.removeLast();
        }

        this.redo.clear();
    }

    @Override
    public void undo() {
        if (this.clipCode.isFocused() && this.clipCode.canUndo()) {
            this.clipCode.undo();
        } else if (this.clipBlocks != null && this.clipBlocks.visible && this.clipBlocks.hasFocusWithin()) {
            this.clipBlocks.undo();
        } else if (!this.undo.isEmpty()) {
            this.redo.push(this.doc.toJson());
            this.doc = CutsceneDoc.fromJson(this.undo.pop());
            this.sel = Math.min(this.sel, this.doc.keys.size() - 1);
            this.clipSel = Math.min(this.clipSel, this.doc.clips.size() - 1);
            this.buildForm();
        }
    }

    @Override
    public void redo() {
        if (this.clipCode.isFocused() && this.clipCode.canRedo()) {
            this.clipCode.redo();
        } else if (this.clipBlocks != null && this.clipBlocks.visible && this.clipBlocks.hasFocusWithin()) {
            this.clipBlocks.redo();
        } else if (!this.redo.isEmpty()) {
            this.undo.push(this.doc.toJson());
            this.doc = CutsceneDoc.fromJson(this.redo.pop());
            this.sel = Math.min(this.sel, this.doc.keys.size() - 1);
            this.clipSel = Math.min(this.clipSel, this.doc.clips.size() - 1);
            this.buildForm();
        }
    }

    private CutsceneDoc.Key selected() {
        return this.sel >= 0 && this.sel < this.doc.keys.size() ? this.doc.keys.get(this.sel) : null;
    }

    private void select(int i) {
        this.sel = i;
        this.clipSel = -1;
        this.buildForm();
    }

    private CutsceneDoc.Clip selectedClip() {
        return this.clipSel >= 0 && this.clipSel < this.doc.clips.size() ? this.doc.clips.get(this.clipSel) : null;
    }

    private void selectClip(int i) {
        this.clipSel = i;
        this.sel = -1;
        CutsceneDoc.Clip cl = this.selectedClip();
        this.blocksMode = cl != null && !cl.blocks.isEmpty();
        this.buildForm();
    }

    private static double snapped(double t) {
        double snap = Screen.hasShiftDown() ? 0.5 : 0.05;
        return Math.round(t / snap) * snap;
    }

    private void addClip(double t) {
        this.snapshot();
        CutsceneDoc.Clip cl = new CutsceneDoc.Clip();
        cl.t = (float)round(snapped(Math.max(0.0, t)));
        this.doc.clips.add(cl);
        this.doc.sort();
        this.selectClip(this.doc.clips.indexOf(cl));
        this.clipCode.requestFocus();
    }

    private void duplicateClip(CutsceneDoc.Clip cl) {
        this.snapshot();
        CutsceneDoc.Clip copy = cl.copy();
        copy.t = cl.t + 1.0F;
        this.doc.clips.add(copy);
        this.doc.sort();
        this.selectClip(this.doc.clips.indexOf(copy));
    }

    private void deleteClip(int i) {
        if (i >= 0 && i < this.doc.clips.size()) {
            this.snapshot();
            this.doc.clips.remove(i);
            this.selectClip(-1);
        }
    }

    private void resortClips() {
        CutsceneDoc.Clip cl = this.selectedClip();
        this.doc.sort();
        if (cl != null) {
            this.clipSel = this.doc.clips.indexOf(cl);
        }
    }

    private boolean clipHasError(CutsceneDoc.Clip cl) {
        return this.clipErrors
            .computeIfAbsent(cl.code, code -> this.clipLanguage.analyze(code).problems().stream().anyMatch(p -> p.severity() == LanguageSupport.Severity.ERROR));
    }

    private void findStory() {
        this.clipStoryName = null;
        this.clipLanguage.clipStory = null;
        this.clipErrors.clear();
        if (this.file != null) {
            Pattern plays = Pattern.compile("cutscene\\s+play\\s+\"" + Pattern.quote(this.file.name()) + "\"");

            try {
                for (Project.FileRef f : this.project.files()) {
                    if (f.kind() == Project.Kind.SCRIPT || f.kind() == Project.Kind.BLOCKS) {
                        String text = this.project.read(f);
                        if (f.kind() == Project.Kind.BLOCKS) {
                            text = BlockCodegen.generate(VisualScript.fromJson(text)).text;
                        }

                        if (plays.matcher(text).find()) {
                            this.clipLanguage.clipStory = Parser.parse(text).program;
                            this.clipStoryName = f.name();
                            return;
                        }
                    }
                }
            } catch (RuntimeException | IOException var5) {
            }
        }
    }

    private void setBlocksMode(boolean on) {
        this.blocksMode = on;
        this.syncBlocks();
        this.relayoutTab();
    }

    private List<String> storyNames(String blockType) {
        Ast.Program story = this.clipLanguage.clipStory;
        List<String> out = new ArrayList<>();
        if (story == null) {
            return out;
        } else {
            switch (blockType) {
                case "character":
                    story.characters.forEach(c -> out.add(c.name));
                    break;
                case "location":
                    story.locations.forEach(l -> out.add(l.name));
                    break;
                case "var":
                    story.vars.forEach(v -> out.add(v.name));
                    break;
                case "func":
                    story.funcs.forEach(f -> out.add(f.name));
            }

            return out;
        }
    }

    private void syncBlocks() {
        CutsceneDoc.Clip cl = this.selectedClip();
        if (cl != null && this.blocksMode) {
            if (this.clipBlocks == null || this.blocksOf != cl) {
                if (this.clipBlocks != null) {
                    this.clipPane.remove(this.clipBlocks);
                }

                VisualScript script = cl.blocks.isEmpty() ? VisualScript.createClip() : VisualScript.fromJson(cl.blocks);
                this.clipBlocks = this.clipPane.add(new BlockEditor(script));
                this.blocksOf = cl;
                this.clipBlocks.positionProvider = this::cameraPosition;
                this.clipBlocks.generator = s -> BlockCodegen.generateClip(s, this.clipLanguage.clipStory);
                this.clipBlocks.outsideNames = this::storyNames;
                this.clipBlocks.assetPicker = AssetPicker::open;
                this.clipBlocks.onChange = b -> {
                    CutsceneDoc.Clip now = this.selectedClip();
                    if (now != null) {
                        now.blocks = b.script().toJson();
                        now.code = b.generated().text;
                        if (b.script().stacks.size() <= 1 && (b.script().stacks.isEmpty() || b.script().stacks.get(0).blocks.size() <= 1)) {
                            now.blocks = "";
                        }

                        this.clipCode.readOnly = !now.blocks.isEmpty();
                        this.clipCode.setText(now.code);
                    }
                };
                if (!cl.blocks.isEmpty()) {
                    this.clipCode.readOnly = true;
                } else if (!cl.code.isBlank() && ui() != null) {
                    ui().toastInfo("Текст скрипта заменится блоками, как только вы их добавите (Ctrl+Z вернёт)");
                }
            }
        } else {
            if (cl == null && this.clipBlocks != null) {
                this.clipPane.remove(this.clipBlocks);
                this.clipBlocks = null;
                this.blocksOf = null;
            }

            this.clipCode.readOnly = cl != null && !cl.blocks.isEmpty();
        }
    }

    private void resort() {
        CutsceneDoc.Key k = this.selected();
        this.doc.sort();
        if (k != null) {
            this.sel = this.doc.keys.indexOf(k);
        }
    }

    private static UI ui() {
        return UI.get();
    }

    private CutsceneDoc.Key fromPlayer() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) {
            if (ui() != null) {
                ui().toastError("Войдите в мир, чтобы брать кадры с игрока");
            }

            return null;
        } else {
            CutsceneDoc.Key k = new CutsceneDoc.Key();
            Vec3 e = p.getEyePosition(1.0F);
            if (this.doc.relative) {
                if (this.relAnchor == null) {
                    this.relAnchor = this.doc.keys.isEmpty()
                        ? p.position()
                        : p.position().subtract(this.doc.keys.get(this.doc.keys.size() - 1).x, 0.0, this.doc.keys.get(this.doc.keys.size() - 1).z);
                }

                e = e.subtract(this.relAnchor);
            }

            k.x = round(e.x);
            k.y = round(e.y);
            k.z = round(e.z);
            k.yaw = (float)round(CutsceneDoc.wrap(p.getYRot()));
            k.pitch = (float)round(p.getXRot());
            k.fov = Minecraft.getInstance().options.fov().get().intValue();
            return k;
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public void addFromPlayer() {
        CutsceneDoc.Key k = this.fromPlayer();
        if (k != null) {
            this.snapshot();
            CutsceneDoc.Key last = this.doc.keys.isEmpty() ? null : this.doc.keys.get(this.doc.keys.size() - 1);
            k.t = last == null ? 0.0F : last.t + 2.0F;
            if (last != null) {
                k.ease = last.ease;
            }

            this.doc.keys.add(k);
            this.resort();
            this.sel = this.doc.keys.indexOf(k);
            this.buildForm();
        }
    }

    public int frames() {
        return this.doc.keys.size();
    }

    private void updateFromPlayer() {
        CutsceneDoc.Key s = this.selected();
        CutsceneDoc.Key k = this.fromPlayer();
        if (s != null && k != null) {
            this.snapshot();
            s.x = k.x;
            s.y = k.y;
            s.z = k.z;
            s.yaw = k.yaw;
            s.pitch = k.pitch;
            this.buildForm();
        }
    }

    private void teleport() {
        CutsceneDoc.Key s = this.selected();
        if (s != null) {
            double ox = 0.0;
            double oy = 0.0;
            double oz = 0.0;
            if (this.doc.relative && this.relAnchor != null) {
                ox = this.relAnchor.x;
                oy = this.relAnchor.y;
                oz = this.relAnchor.z;
            }

            ClientState.get().command(String.format(Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f %.1f", s.x + ox, s.y + oy - 1.62, s.z + oz, s.yaw, s.pitch));
        }
    }

    private void duplicate() {
        if (this.selectedClip() != null) {
            this.duplicateClip(this.selectedClip());
        } else {
            CutsceneDoc.Key s = this.selected();
            if (s != null) {
                this.snapshot();
                CutsceneDoc.Key k = s.copy();
                k.t = s.t + 1.0F;

                for (CutsceneDoc.Key o : this.doc.keys) {
                    if (o.t > s.t) {
                        o.t++;
                    }
                }

                k.mark = "";
                k.hold = false;
                this.doc.keys.add(k);
                this.resort();
                this.sel = this.doc.keys.indexOf(k);
                this.buildForm();
            }
        }
    }

    private void deleteAny() {
        if (this.selectedClip() != null) {
            this.deleteClip(this.clipSel);
        } else {
            this.deleteSelected();
        }
    }

    void deleteSelected() {
        if (this.selected() != null) {
            this.snapshot();
            this.doc.keys.remove(this.sel);
            this.sel = Math.min(this.sel, this.doc.keys.size() - 1);
            this.buildForm();
        }
    }

    private void record() {
        if (Minecraft.getInstance().player == null) {
            if (ui() != null) {
                ui().toastError("Войдите в мир");
            }
        } else {
            CutsceneRecorder.start(this);
            Minecraft.getInstance().setScreen(null);
        }
    }

    private void preview(int from) {
        this.previewFrom(from, SprauteScreen::open);
    }

    void previewFrom(int from, Runnable onEnd) {
        if (Minecraft.getInstance().player == null) {
            if (ui() != null) {
                ui().toastError("Войдите в мир, чтобы посмотреть катсцену");
            }
        } else if (!this.doc.keys.isEmpty()) {
            CutsceneDoc d = CutsceneDoc.fromJson(this.doc.toJson());
            if (from > 0 && from < d.keys.size()) {
                float t0 = d.keys.get(from).t;
                d.keys = new ArrayList<>(d.keys.subList(from, d.keys.size()));

                for (CutsceneDoc.Key k : d.keys) {
                    k.t -= t0;
                }

                d.clips.removeIf(clx -> clx.t < t0);

                for (CutsceneDoc.Clip cl : d.clips) {
                    cl.t -= t0;
                }

                d.fadeIn = 0.0F;
            }

            Vec3 origin = this.doc.relative ? this.origin() : Vec3.ZERO;
            this.setShown(false);
            Minecraft.getInstance().setScreen(null);
            CameraDirector.play(-1, this.file != null ? this.file.name() : "preview", d, origin, Map.of(), (kind, name) -> {
                if (kind.equals("end")) {
                    onEnd.run();
                } else if (kind.equals("mark")) {
                    Minecraft.getInstance().gui.setOverlayMessage(Component.literal("метка: " + name), false);
                } else if (kind.equals("hold")) {
                    Minecraft.getInstance().gui.setOverlayMessage(Component.literal("здесь катсцена ждёт: в игре — до cutscene continue"), false);
                } else if (kind.equals("clip")) {
                    CutsceneDoc.Clip clx = d.clips.get(Integer.parseInt(name));
                    Minecraft.getInstance().gui.setOverlayMessage(Component.literal("скрипт: " + clx.title()), false);
                }
            });
        }
    }

    private void buildForm() {
        this.form.clear();
        CutsceneDoc.Clip cl = this.selectedClip();
        boolean code = cl != null;
        if (code && !cl.code.equals(this.clipCode.text())) {
            this.clipCode.setText(cl.code);
        }

        if (!code && this.clipCode.isFocused()) {
            UI.get().setFocus(null);
        }

        this.clipPane.visible = code;
        this.syncBlocks();
        CutsceneDoc.Key k = this.selected();
        if (code) {
            this.form.header("Скрипт");
            this.form.decimal("Время, с", () -> cl.t, v -> {
                cl.t = (float)Math.max(0.0, v);
                this.resortClips();
            });
            this.form.text("Название", "Подпись на таймлайне. Пусто — первая строка кода", () -> cl.name, v -> cl.name = v);
            this.form
                .toggle(
                    "Обязательный",
                    "Выключено: если катсцену пропустят, скрипт не начнётся, а уже начатый остановится. Включено: выполнится сразу при пропуске. Нужно для важного, например появления персонажа",
                    () -> cl.always,
                    v -> cl.always = v
                );
        } else if (k != null) {
            this.form.header("Кадр " + (this.sel + 1) + " из " + this.doc.keys.size());
            this.form.decimal("Время, с", () -> k.t, v -> {
                k.t = (float)Math.max(0.0, v);
                this.resort();
            });
            this.form.decimal("X", () -> k.x, v -> k.x = v);
            this.form.decimal("Y", () -> k.y, v -> k.y = v);
            this.form.decimal("Z", () -> k.z, v -> k.z = v);
            this.form.decimal("Поворот (yaw)", () -> k.yaw, v -> k.yaw = (float)v);
            this.form.decimal("Наклон (pitch)", () -> k.pitch, v -> k.pitch = (float)Math.max(-90.0, Math.min(90.0, v)));
            this.form.decimal("Крен (roll)", () -> k.roll, v -> k.roll = (float)v);
            this.form.decimal("Обзор (FOV)", () -> k.fov, v -> k.fov = (float)Math.max(5.0, Math.min(170.0, v)));
            this.form
                .dropdown(
                    "Плавность", "Как камера движется от этого кадра к следующему", Ease::names, () -> k.ease, e -> Ease.TITLES.getOrDefault(e, e), e -> {
                        this.snapshot();
                        k.ease = e;
                    }
                );
            this.form
                .text(
                    "Смотреть на",
                    "player — на игрока, имя персонажа сюжета или моба (\"minecraft:wolf\"). Пусто — заданный поворот",
                    () -> k.look,
                    v -> k.look = v.replace("\"", "").trim()
                );
            this.form.text("Субтитр", "Текст внизу экрана, пока камера летит от этого кадра", () -> k.caption, v -> k.caption = v);
            this.form.text("Метка", "Событие при достижении кадра: on cutscenemark \"имя\" { ... }", () -> k.mark, v -> k.mark = v.trim());
            this.form.decimal("Тряска 0..1", () -> k.shake, v -> k.shake = (float)Math.max(0.0, Math.min(1.0, v)));
            this.form
                .toggle(
                    "Ждать на этом кадре",
                    "Катсцена стоит на кадре, пока сюжет не скажет cutscene continue (блок «катсцена: идти дальше с кадра»). В просмотре стоит 2 секунды",
                    () -> k.hold,
                    v -> {
                        this.snapshot();
                        k.hold = v;
                        this.buildForm();
                    }
                );
            if (k.hold) {
                this.form
                    .decimal(
                        "Не дольше, с",
                        "0 — сколько угодно. Иначе катсцена сама пойдёт дальше через это время",
                        () -> k.holdMax,
                        v -> k.holdMax = (float)Math.max(0.0, v)
                    );
                this.form.toggle("Игрок идёт дальше сам", "Пробел или Enter: продолжить, а не пропустить катсцену", () -> k.holdPress, v -> k.holdPress = v);
                this.form.note("Сюжет: cutscene play \"" + (this.file != null ? this.file.name() : "имя") + "\" nowait");
                this.form.note("wait cutscenehold \"" + (this.file != null ? this.file.name() : "имя") + "\", затем cutscene continue");
                if (this.doc.flow) {
                    this.form.note("При «Сплошном движении» камера встанет там, куда долетит к этому времени");
                }
            }
        } else {
            this.form.header("Кадры");
            this.form.note(this.doc.keys.isEmpty() ? "Нет кадров: нажмите «Кадр с игрока» или «Запись в игре»" : "Выберите кадр на карте или таймлайне");
        }

        if (!code) {
        }

        if (this.shown && !this.clipPreview.sim().notes.isEmpty()) {
            this.form.header("Что не видно в просмотре");

            for (String n : this.clipPreview.sim().notes) {
                this.form.note(n);
            }
        }

        this.form.header("Катсцена");
        this.form.toggle("Плавный путь", "Камера летит по кривой через кадры; выключено — по прямым", () -> this.doc.smooth, v -> this.doc.smooth = v);
        this.form
            .toggle(
                "Сплошное движение",
                "Одно плавное движение по всей линии с ровной скоростью, без замедлений у кадров. Время кадров задаёт только общую длину",
                () -> this.doc.flow,
                v -> {
                    this.doc.flow = v;
                    this.buildForm();
                }
            );
        if (this.doc.flow) {
            this.form
                .dropdown(
                    "Общая плавность",
                    "Как движение разгоняется в начале и тормозит в конце",
                    Ease::names,
                    () -> this.doc.flowEase,
                    e -> Ease.TITLES.getOrDefault(e, e),
                    e -> {
                        this.snapshot();
                        this.doc.flowEase = e;
                    }
                );
        }

        this.form
            .decimal(
                "Кинополосы",
                "Доля высоты экрана сверху и снизу: 0.11 — как в кино, 0 — без полос",
                () -> this.doc.bars,
                v -> this.doc.bars = (float)Math.max(0.0, Math.min(0.45, v))
            );
        this.form
            .dropdown(
                "Скрыть интерфейс",
                "Что прячется на время катсцены. Речь, титры и субтитры видны всегда",
                () -> CutsceneDoc.HIDE_MODES,
                this.doc::hideMode,
                CutsceneDoc::hideTitle,
                v -> {
                    this.doc.hide = v;
                    this.doc.hideHud = !v.equals("none");
                }
            );
        this.form.toggle("Игрок не двигается", null, () -> this.doc.lockPlayer, v -> this.doc.lockPlayer = v);
        this.form
            .toggle("Можно пропустить", "Пробел или Enter пропускают катсцену (событие cutsceneskip)", () -> this.doc.skippable, v -> this.doc.skippable = v);
        this.form.decimal("Вход, с", "Плавный переход от вида игрока к первому кадру", () -> this.doc.blendIn, v -> this.doc.blendIn = (float)Math.max(0.0, v));
        this.form.decimal("Выход, с", "Плавный возврат камеры игроку после конца", () -> this.doc.blendOut, v -> this.doc.blendOut = (float)Math.max(0.0, v));
        this.form
            .decimal("Из затемнения, с", "Начать с затемнённого экрана и проявиться", () -> this.doc.fadeIn, v -> this.doc.fadeIn = (float)Math.max(0.0, v));
        this.form.decimal("В затемнение, с", "Затемнить экран к концу", () -> this.doc.fadeOut, v -> this.doc.fadeOut = (float)Math.max(0.0, v));
        this.form.text("Цвет затемнения", "#RRGGBB", () -> this.doc.fadeColor, v -> this.doc.fadeColor = v.trim());
        this.form
            .toggle(
                "Относительно точки",
                "Координаты кадров — смещения от точки, где играют катсцену (cutscene play \"имя\" at alice)",
                () -> this.doc.relative,
                v -> {
                    this.snapshot();
                    this.toggleRelative(v);
                }
            );
        if (this.shown) {
            this.form.note("Персонажи и эффекты в мире только показаны: сервер их не видит");
        }

        this.form
            .note(
                "Длина: "
                    + String.format(Locale.ROOT, "%.1f", this.doc.duration())
                    + " с   ·   в сюжете: cutscene play \""
                    + (this.file != null ? this.file.name() : "")
                    + "\""
            );
        if (this.w > 0) {
            this.layout();
        }

        this.inspector.layout();
    }

    private void toggleRelative(boolean on) {
        if (on != this.doc.relative) {
            if (on && !this.doc.keys.isEmpty()) {
                CutsceneDoc.Key f = this.doc.keys.get(0);
                LocalPlayer p = Minecraft.getInstance().player;
                Vec3 a = new Vec3(f.x, f.y - 1.62, f.z);
                this.relAnchor = p != null ? a : null;

                for (CutsceneDoc.Key k : this.doc.keys) {
                    k.x = round(k.x - a.x);
                    k.y = round(k.y - a.y);
                    k.z = round(k.z - a.z);
                }
            } else if (!on && this.relAnchor != null) {
                for (CutsceneDoc.Key k : this.doc.keys) {
                    k.x = round(k.x + this.relAnchor.x);
                    k.y = round(k.y + this.relAnchor.y);
                    k.z = round(k.z + this.relAnchor.z);
                }

                this.relAnchor = null;
            }

            this.doc.relative = on;
            this.buildForm();
        }
    }

    private static String fmt(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == (long)r ? String.valueOf((long)r) : String.valueOf(r);
    }

    @Override
    public void layout() {
        this.compactTools = false;
        int wide = 12;

        for (Button b : this.tools) {
            if (b.visible) {
                b.autoWidth();
                wide += b.prefW + 4;
            }
        }

        this.compactTools = wide > this.w;
        if (this.compactTools) {
            for (Button bx : this.tools) {
                bx.autoWidth();
            }
        }

        int bx = this.x + 6;
        int by = this.y + 4;

        for (Button bxx : this.tools) {
            if (bxx.visible) {
                if (bx + bxx.prefW > this.x + this.w - 6 && bx > this.x + 6) {
                    bx = this.x + 6;
                    by += 24;
                }

                bxx.bounds(bx, by, bxx.prefW, 20);
                bx += bxx.prefW + 4;
            }
        }

        this.toolbarH = by - this.y + 24;
        int TOOLBAR_H = this.toolbarH;
        int centre = this.h - TOOLBAR_H - 96;
        int paneW = this.clipPane.visible ? Math.max(260, this.w * (this.blocksMode ? 68 : 45) / 100) : 0;
        this.map.bounds(this.x + paneW, this.y + TOOLBAR_H, this.w - paneW, centre);
        this.view.bounds(this.x + paneW, this.y + TOOLBAR_H, this.w - paneW, centre);
        this.clipPane.bounds(this.x, this.y + TOOLBAR_H, paneW, centre);
        this.timeline.bounds(this.x, this.y + this.h - 96, this.w, 96);
        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (!this.shown) {
            c.fill(this.x, this.y, this.w, this.h, -15461093);
        }

        int TOOLBAR_H = this.toolbarH;
        c.fill(this.x, this.y, this.w, TOOLBAR_H, -14934490);
        c.fill(this.x, this.y + TOOLBAR_H - 1, this.w, 1, -13749952);
        if (this.playStarted >= 0L) {
            this.playhead = this.playFrom + (System.nanoTime() - this.playStarted) / 1.0E9;
            if (this.playhead >= this.doc.duration()) {
                this.playhead = this.doc.duration();
                this.playStarted = -1L;
            }
        }

        if (this.shown && this.throughCamera && !this.doc.keys.isEmpty() && !this.view.looking) {
            this.showCamera();
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (super.keyPressed(key, scan, mods)) {
            return true;
        } else if (this.isHoveredDeep()
            && !this.clipCode.isFocused()
            && !this.view.looking
            && !this.view.rightDown
            && (this.clipBlocks == null || !this.clipBlocks.hasFocusWithin())) {
            switch (key) {
                case 32:
                    this.togglePlay();
                    break;
                case 261:
                    this.deleteAny();
                    break;
                case 262:
                    if (this.sel < this.doc.keys.size() - 1) {
                        this.select(this.sel + 1);
                    }
                    break;
                case 263:
                    if (this.sel > 0) {
                        this.select(this.sel - 1);
                    }
                    break;
                default:
                    return false;
            }

            return true;
        } else {
            return false;
        }
    }

    private void togglePlay() {
        if (this.playStarted >= 0L) {
            this.playStarted = -1L;
        } else {
            if (this.playhead >= this.doc.duration()) {
                this.playhead = 0.0;
            }

            this.playFrom = this.playhead;
            this.playStarted = System.nanoTime();
        }
    }

    @Override
    public Icon icon() {
        return Icon.FILM;
    }

    @Override
    public int iconColor() {
        return -28838;
    }

    @Override
    public boolean isDirty() {
        return !this.doc.toJson().equals(this.saved);
    }

    @Override
    public void save() throws IOException {
        this.doc.sort();
        String json = this.doc.toJson();
        this.project.write(this.file, json);
        this.saved = json;
    }

    @Override
    public String source() {
        return null;
    }

    @Override
    public int scriptId() {
        return -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        return List.of();
    }

    @Override
    public Analyzer.Result analysis() {
        return null;
    }

    @Override
    public void reveal(int offset) {
    }

    @Override
    public void focusContent() {
    }

    private final class ClipPane extends Widget {
        private final Button back = this.add(new Button("", () -> CutsceneTab.this.selectClip(-1)).icon(Icon.CLOSE).style(Button.Style.GHOST));
        private final Button code = this.add(
            new Button("Код", () -> CutsceneTab.this.setBlocksMode(false)).icon(Icon.CODE).style(Button.Style.GHOST).toggle(() -> !CutsceneTab.this.blocksMode)
        );
        private final Button blocks = this.add(
            new Button("Блоки", () -> CutsceneTab.this.setBlocksMode(true))
                .icon(Icon.GRAPH)
                .style(Button.Style.GHOST)
                .toggle(() -> CutsceneTab.this.blocksMode)
        );

        ClipPane() {
            this.back.autoWidth();
            this.code.autoWidth();
            this.blocks.autoWidth();
            this.back.tooltip("Закрыть скрипт");
            this.blocks.tooltip("Собрать скрипт из блоков: код получится из них сам");
        }

        @Override
        public void layout() {
            int bx = this.x + this.w - this.back.prefW - 4;
            this.back.bounds(bx, this.y + 3, this.back.prefW, 18);
            bx -= this.blocks.prefW + 2;
            this.blocks.bounds(bx, this.y + 3, this.blocks.prefW, 18);
            bx -= this.code.prefW + 2;
            this.code.bounds(bx, this.y + 3, this.code.prefW, 18);
            CutsceneTab.this.clipCode.bounds(this.x, this.y + 38, this.w, this.h - 38);
            CutsceneTab.this.clipCode.visible = !CutsceneTab.this.blocksMode;
            if (CutsceneTab.this.clipBlocks != null) {
                CutsceneTab.this.clipBlocks.bounds(this.x, this.y + 38, this.w, this.h - 38);
                CutsceneTab.this.clipBlocks.visible = CutsceneTab.this.blocksMode;
            }

            super.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, 38, -14934490);
            c.fill(this.x, this.y + 37, this.w, 1, -13749952);
            CutsceneDoc.Clip cl = CutsceneTab.this.selectedClip();
            if (cl != null) {
                c.textEllipsized("Скрипт на " + CutsceneTab.fmt((double)cl.t) + " с", this.x + 8, this.y + 8, this.code.x - this.x - 12, -1841682);
            }

            String how = CutsceneTab.this.clipStoryName != null
                ? "Выполняется в сюжете «" + CutsceneTab.this.clipStoryName + "»: видны его персонажи, точки и переменные"
                : "Сюжет с cutscene play этой катсцены не найден: имена появятся, когда он будет";
            c.textEllipsized(how, this.x + 8, this.y + 23, this.w - 16, -9933435);
        }
    }

    private final class Form extends Widget {
        void header(final String text) {
            this.add(new Widget() {
                {
                    this.prefH = 24;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.fill(this.x + 8, this.y + 7, this.w - 16, 1, -13749952);
                    c.text(text.toUpperCase(), this.x + 10, this.y + 13, -7639809);
                }
            });
        }

        void note(final String text) {
            this.add(new Widget() {
                {
                    this.prefH = 14;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.textEllipsized(text, this.x + 10, this.y + 3, this.w - 16, -9933435);
                }
            }).tooltip(text);
        }

        CutsceneTab.Row row(String label, String doc) {
            CutsceneTab.Row r = this.add(CutsceneTab.this.new Row(label));
            if (doc != null) {
                r.tooltip(doc);
            }

            return r;
        }

        void toggle(String label, String doc, BooleanSupplier get, Consumer<Boolean> set) {
            CutsceneTab.Row r = this.row(label, doc);
            r.editor = r.add(new Checkbox("", get, v -> {
                CutsceneTab.this.snapshot();
                set.accept(v);
            }));
        }

        void decimal(String label, DoubleSupplier get, DoubleConsumer set) {
            this.decimal(label, null, get, set);
        }

        void decimal(String label, String doc, final DoubleSupplier get, DoubleConsumer set) {
            CutsceneTab.Row r = this.row(label, doc);
            TextField f = new TextField() {
                @Override
                public void tick() {
                    super.tick();
                    if (!this.isFocused()) {
                        String s = CutsceneTab.fmt(get.getAsDouble());
                        if (!s.equals(this.text())) {
                            this.setText(s);
                        }
                    }
                }

                @Override
                public void focusGained() {
                    super.focusGained();
                    CutsceneTab.this.snapshot();
                }
            };
            f.setText(CutsceneTab.fmt(get.getAsDouble()));
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.';
            f.onChange = s -> {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                } catch (NumberFormatException var3x) {
                }
            };
            r.editor = r.add(f);
        }

        void text(String label, String doc, Supplier<String> get, Consumer<String> set) {
            CutsceneTab.Row r = this.row(label, doc);
            TextField f = new TextField() {
                @Override
                public void focusGained() {
                    super.focusGained();
                    CutsceneTab.this.snapshot();
                }
            };
            f.setText(get.get());
            f.onChange = set;
            r.editor = r.add(f);
        }

        <T> void dropdown(String label, String doc, Supplier<List<T>> options, Supplier<T> get, Function<T, String> labeler, Consumer<T> set) {
            CutsceneTab.Row r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(options, get, labeler, set));
        }

        @Override
        public void layout() {
            int cy = this.y;

            for (Widget c : this.children()) {
                c.bounds(this.x, cy, this.w, c.prefH);
                c.layout();
                cy += c.prefH;
            }

            this.prefH = cy - this.y + 8;
        }
    }

    private final class Inspector extends Widget {
        private final ScrollView scroll = this.add(new ScrollView(CutsceneTab.this.form));

        @Override
        public void layout() {
            int total = 8;

            for (Widget c : CutsceneTab.this.form.children()) {
                total += c.prefH;
            }

            CutsceneTab.this.form.prefH = total;
            this.scroll.bounds(this.x, this.y, this.w, this.h);
            this.scroll.layout();
            CutsceneTab.this.form.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14934490);
        }
    }

    private final class MapView extends Widget {
        double cx;
        double cz;
        double scale = 8.0;
        boolean fitted;
        final MiniMap minimap = new MiniMap();
        int dragKey = -1;
        int dragYaw = -1;
        boolean panning;
        long lastClick;

        int sx(double wx) {
            return (int)Math.round(this.x + this.w / 2.0 + (wx - this.cx) * this.scale);
        }

        int sy(double wz) {
            return (int)Math.round(this.y + this.h / 2.0 + (wz - this.cz) * this.scale);
        }

        double wx(double px) {
            return this.cx + (px - this.x - this.w / 2.0) / this.scale;
        }

        double wz(double py) {
            return this.cz + (py - this.y - this.h / 2.0) / this.scale;
        }

        void fit() {
            if (CutsceneTab.this.doc.keys.isEmpty()) {
                LocalPlayer p = Minecraft.getInstance().player;
                if (p != null && !CutsceneTab.this.doc.relative) {
                    this.cx = p.getX();
                    this.cz = p.getZ();
                }
            } else {
                double x0 = 1.0E9;
                double x1 = -1.0E9;
                double z0 = 1.0E9;
                double z1 = -1.0E9;

                for (CutsceneDoc.Key k : CutsceneTab.this.doc.keys) {
                    x0 = Math.min(x0, k.x);
                    x1 = Math.max(x1, k.x);
                    z0 = Math.min(z0, k.z);
                    z1 = Math.max(z1, k.z);
                }

                this.cx = (x0 + x1) / 2.0;
                this.cz = (z0 + z1) / 2.0;
                double span = Math.max(Math.max(x1 - x0, z1 - z0), 8.0);
                this.scale = Math.max(1.0, Math.min(60.0, Math.min(this.w, this.h) * 0.8 / span));
            }
        }

        int[] yawTip(CutsceneDoc.Key k) {
            double a = Math.toRadians(k.yaw);
            int px = this.sx(k.x);
            int py = this.sy(k.z);
            return new int[]{(int)Math.round(px - Math.sin(a) * 22.0), (int)Math.round(py + Math.cos(a) * 22.0)};
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            if (!this.fitted && this.w > 0) {
                this.fit();
                this.fitted = true;
            }

            c.pushClip(this.x, this.y, this.w, this.h);
            c.fill(this.x, this.y, this.w, this.h, -15395300);
            Vec3 off = CutsceneTab.this.doc.relative ? CutsceneTab.this.origin() : Vec3.ZERO;
            this.minimap.draw(c, this.x, this.y, this.w, this.h, this.cx + off.x, this.cz + off.z, this.scale);
            double step = this.scale >= 20.0 ? 1.0 : (this.scale >= 6.0 ? 5.0 : (this.scale >= 2.0 ? 10.0 : 50.0));
            double gx0 = Math.floor(this.wx(this.x) / step) * step;
            double gz0 = Math.floor(this.wz(this.y) / step) * step;

            for (double gx = gx0; this.sx(gx) < this.x + this.w; gx += step) {
                boolean major = Math.round(gx / step) % 5L == 0L;
                c.fill(this.sx(gx), this.y, 1, this.h, major ? 1090519039 : 385875967);
            }

            for (double gz = gz0; this.sy(gz) < this.y + this.h; gz += step) {
                boolean major = Math.round(gz / step) % 5L == 0L;
                c.fill(this.x, this.sy(gz), this.w, 1, major ? 1090519039 : 385875967);
            }

            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null && !CutsceneTab.this.doc.relative) {
                int px = this.sx(p.getX());
                int py = this.sy(p.getZ());
                c.round(px - 4, py - 4, 8, 8, 4, -1);
                c.text("игрок", px + 6, py - 4, -1711276033);
            } else if (CutsceneTab.this.doc.relative) {
                int px = this.sx(0.0);
                int py = this.sy(0.0);
                c.round(px - 3, py - 3, 6, 6, 3, -13461);
                c.text("точка запуска", px + 6, py - 4, -1711289493);
            }

            if (CutsceneTab.this.doc.keys.size() > 1) {
                double dur = CutsceneTab.this.doc.duration();
                int n = Math.max(20, (int)(dur * 20.0));
                CutsceneDoc.Pose prev = CutsceneTab.this.doc.at(0.0);

                for (int i = 1; i <= n; i++) {
                    CutsceneDoc.Pose cur = CutsceneTab.this.doc.at(dur * i / n);
                    c.line(this.sx(prev.x), this.sy(prev.z), this.sx(cur.x), this.sy(cur.z), 1.5F, -11549705);
                    prev = cur;
                }
            }

            for (int i = 0; i < CutsceneTab.this.doc.keys.size(); i++) {
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(i);
                int px = this.sx(k.x);
                int py = this.sy(k.z);
                int[] tip = this.yawTip(k);
                boolean s = i == CutsceneTab.this.sel;
                c.line(px, py, tip[0], tip[1], s ? 2.0F : 1.2F, s ? -13461 : -1426076821);
                if (s) {
                    c.round(tip[0] - 3, tip[1] - 3, 6, 6, 3, -13461);
                }

                c.round(px - 6, py - 6, 12, 12, 6, s ? -7639809 : -12762793);
                c.outline(px - 6, py - 6, 12, 12, s ? -1 : -10854534);
                String num = String.valueOf(i + 1);
                c.text(num, px - c.width(num) / 2 + 1, py - 3, -1);
                if (!k.mark.isEmpty()) {
                    c.text("⚑ " + k.mark, px + 9, py + 4, -28838);
                }

                if (k.hold) {
                    c.fill(px + 8, py - 10, 2, 6, -10036737);
                    c.fill(px + 12, py - 10, 2, 6, -10036737);
                }
            }

            if (!CutsceneTab.this.doc.keys.isEmpty()) {
                CutsceneDoc.Pose cam = CutsceneTab.this.doc.at(CutsceneTab.this.playhead);
                int pxx = this.sx(cam.x);
                int pyx = this.sy(cam.z);
                double a = Math.toRadians(cam.yaw);
                c.line(pxx, pyx, (float)(pxx - Math.sin(a) * 14.0), (float)(pyx + Math.cos(a) * 14.0), 2.0F, -10027111);
                c.round(pxx - 4, pyx - 4, 8, 8, 2, -10027111);
            }

            c.popClip();
            String help = "Тяните кадры и их стрелки взгляда · ПКМ — двигать карту · колёсико — масштаб · двойной клик — кадр здесь";
            c.textEllipsized(help, this.x + 6, this.y + this.h - 12, this.w - 12, 2013265919);
            c.text("Вид сверху (X → , Z ↓)", this.x + 6, this.y + 5, -1996488705);
        }

        int keyAt(double mx, double my) {
            for (int i = CutsceneTab.this.doc.keys.size() - 1; i >= 0; i--) {
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(i);
                if (Math.abs(mx - this.sx(k.x)) <= 7.0 && Math.abs(my - this.sy(k.z)) <= 7.0) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (button == 1 || button == 2) {
                this.panning = true;
                return true;
            } else if (button != 0) {
                return false;
            } else {
                CutsceneDoc.Key s = CutsceneTab.this.selected();
                if (s != null) {
                    int[] tip = this.yawTip(s);
                    if (Math.abs(mx - tip[0]) <= 5.0 && Math.abs(my - tip[1]) <= 5.0) {
                        CutsceneTab.this.snapshot();
                        this.dragYaw = CutsceneTab.this.sel;
                        return true;
                    }
                }

                int k = this.keyAt(mx, my);
                long now = System.currentTimeMillis();
                if (k < 0 && now - this.lastClick < 350L) {
                    CutsceneTab.this.snapshot();
                    CutsceneDoc.Key base = s != null
                        ? s
                        : (CutsceneTab.this.doc.keys.isEmpty() ? null : CutsceneTab.this.doc.keys.get(CutsceneTab.this.doc.keys.size() - 1));
                    CutsceneDoc.Key nk = base != null ? base.copy() : new CutsceneDoc.Key();
                    if (base == null) {
                        LocalPlayer p = Minecraft.getInstance().player;
                        nk.y = p != null ? CutsceneTab.round(p.getEyeY()) : 70.0;
                    }

                    nk.x = CutsceneTab.round(this.wx(mx));
                    nk.z = CutsceneTab.round(this.wz(my));
                    nk.t = CutsceneTab.this.doc.keys.isEmpty() ? 0.0F : CutsceneTab.this.doc.keys.get(CutsceneTab.this.doc.keys.size() - 1).t + 2.0F;
                    nk.mark = "";
                    nk.hold = false;
                    CutsceneTab.this.doc.keys.add(nk);
                    CutsceneTab.this.resort();
                    CutsceneTab.this.select(CutsceneTab.this.doc.keys.indexOf(nk));
                    return true;
                } else {
                    this.lastClick = now;
                    if (k >= 0) {
                        if (k != CutsceneTab.this.sel) {
                            CutsceneTab.this.select(k);
                        }

                        CutsceneTab.this.snapshot();
                        this.dragKey = k;
                    }

                    return true;
                }
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.panning) {
                this.cx = this.cx - dx / this.scale;
                this.cz = this.cz - dy / this.scale;
                return true;
            } else if (this.dragYaw >= 0 && this.dragYaw < CutsceneTab.this.doc.keys.size()) {
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(this.dragYaw);
                double ax = mx - this.sx(k.x);
                double az = my - this.sy(k.z);
                k.yaw = (float)CutsceneTab.round(Math.toDegrees(Math.atan2(-ax, az)));
                return true;
            } else if (this.dragKey >= 0 && this.dragKey < CutsceneTab.this.doc.keys.size()) {
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(this.dragKey);
                double snap = Screen.hasShiftDown() ? 0.5 : 0.05;
                k.x = Math.round(this.wx(mx) / snap) * snap;
                k.z = Math.round(this.wz(my) / snap) * snap;
                k.x = CutsceneTab.round(k.x);
                k.z = CutsceneTab.round(k.z);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            this.panning = false;
            this.dragKey = -1;
            this.dragYaw = -1;
            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            double bx = this.wx(mx);
            double bz = this.wz(my);
            this.scale = Math.max(0.5, Math.min(80.0, this.scale * (delta > 0.0 ? 1.2 : 0.8333333333333334)));
            this.cx = bx - (mx - this.x - this.w / 2.0) / this.scale;
            this.cz = bz - (my - this.y - this.h / 2.0) / this.scale;
            return true;
        }
    }

    interface Preview {
        default void start() {
        }

        default void stop() {
        }

        default ClipSim.Result sim() {
            return null;
        }

        default void show(ClipSim.Result next) {
        }

        default void seek(double t) {
        }

        default void forgetRoutes() {
        }

        default List<double[]> route(ClipSim.Actor a, double[] from, double[] to) {
            return null;
        }

        default Vec3 where(String actor) {
            return null;
        }
    }

    private final class Row extends Widget {
        final String label;
        Widget editor;

        Row(String label) {
            this.label = label;
            this.prefH = 22;
        }

        @Override
        public void layout() {
            if (this.editor != null) {
                this.editor.bounds(this.x + 108, this.y + 2, this.w - 108 - 10, 18);
                this.editor.layout();
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.textEllipsized(this.label, this.x + 10, this.y + 7, 94, -6577995);
        }
    }

    private final class Timeline extends Widget {
        int dragKey = -1;
        int dragClip = -1;
        boolean scrubbing;
        long lastLaneClick;
        int[] chipEnd = new int[0];

        int laneY() {
            return this.y + this.h - 24;
        }

        int clipAt(double mx, double my) {
            return -1;
        }

        void paintClips(Canvas c, int mx, int my) {
        }

        void menu(int mx, int my) {
            PopupMenu pm = new PopupMenu();
            int i = this.clipAt(mx, my);
            if (i >= 0) {
                CutsceneDoc.Clip cl = CutsceneTab.this.doc.clips.get(i);
                pm.item("Открыть код", Icon.CODE, null, () -> CutsceneTab.this.selectClip(CutsceneTab.this.doc.clips.indexOf(cl)));
                pm.item("Дублировать", Icon.COPY, null, () -> CutsceneTab.this.duplicateClip(cl));
                pm.separator();
                pm.item("Удалить", Icon.TRASH, null, () -> CutsceneTab.this.deleteClip(CutsceneTab.this.doc.clips.indexOf(cl)));
            } else {
                double t = this.tAt(mx);
                pm.item("Скрипт на " + CutsceneTab.fmt(CutsceneTab.round(CutsceneTab.snapped(t))) + " с", Icon.CODE, null, () -> CutsceneTab.this.addClip(t));
            }

            pm.show(mx, my);
        }

        double span() {
            return Math.max(5.0, Math.ceil(CutsceneTab.this.doc.duration() + 2.0F));
        }

        int tx(double t) {
            return (int)Math.round(this.x + 14 + t / this.span() * (this.w - 28));
        }

        double tAt(double px) {
            return Math.max(0.0, (px - this.x - 14.0) / (this.w - 28) * this.span());
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14934490);
            c.fill(this.x, this.y, this.w, 1, -13749952);
            double span = this.span();
            double step = 1.0;

            for (double s : new double[]{1.0, 2.0, 5.0, 10.0, 20.0, 30.0, 60.0, 120.0}) {
                step = s;
                if (s / span * (this.w - 28) >= 34.0) {
                    break;
                }
            }

            for (double t = 0.0; t <= span + 1.0E-6; t += step) {
                int px = this.tx(t);
                c.fill(px, this.y + 14, 1, 6, -12762793);
                c.text(CutsceneTab.fmt(t) + "с", px + 2, this.y + 5, -9933435);
            }

            int ly = this.y + 30;
            c.fill(this.x + 14, ly + 6, this.w - 28, 2, -14013384);

            for (int i = 0; i + 1 < CutsceneTab.this.doc.keys.size(); i++) {
                CutsceneDoc.Key a = CutsceneTab.this.doc.keys.get(i);
                CutsceneDoc.Key b = CutsceneTab.this.doc.keys.get(i + 1);
                if (!a.caption.isEmpty()) {
                    c.round(this.tx(a.t), ly + 18, Math.max(2, this.tx(b.t) - this.tx(a.t)), 12, 2, -12959910);
                    c.textEllipsized(a.caption, this.tx(a.t) + 3, ly + 20, this.tx(b.t) - this.tx(a.t) - 5, -6577995);
                }
            }

            for (int ix = 0; ix < CutsceneTab.this.doc.keys.size(); ix++) {
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(ix);
                int px = this.tx(k.t);
                boolean sx = ix == CutsceneTab.this.sel;

                for (int d = 0; d <= 5; d++) {
                    c.fill(px - d, ly + 7 - (5 - d), d * 2 + 1, 1, sx ? -7639809 : -5195831);
                }

                for (int d = 0; d <= 5; d++) {
                    c.fill(px - d, ly + 7 + (5 - d), d * 2 + 1, 1, sx ? -7639809 : -5195831);
                }

                if (!k.mark.isEmpty()) {
                    c.text("⚑", px - 3, ly - 8, -28838);
                }

                if (k.hold) {
                    c.fill(px + 7, ly + 2, 2, 8, -10036737);
                    c.fill(px + 11, ly + 2, 2, 8, -10036737);
                }
            }

            this.paintClips(c, mx, my);
            int ph = this.tx(CutsceneTab.this.playhead);
            c.fill(ph, this.y + 12, 1, this.h - 14, -10027111);
            String info = CutsceneTab.fmt(CutsceneTab.this.playhead)
                + " / "
                + CutsceneTab.fmt((double)CutsceneTab.this.doc.duration())
                + " с   ·   пробел — проиграть здесь";
            if (c.width(info) < this.w / 2) {
                c.text(info, this.x + this.w - c.width(info) - 8, this.y + 21, -9933435);
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (button == 1) {
            }

            if (button != 0) {
                return false;
            } else {
                int clip = this.clipAt(mx, my);
                if (clip >= 0) {
                    if (clip != CutsceneTab.this.clipSel) {
                        CutsceneTab.this.selectClip(clip);
                    }

                    CutsceneTab.this.snapshot();
                    this.dragClip = clip;
                    return true;
                } else {
                    if (my >= this.laneY() - 2) {
                    }

                    for (int i = 0; i < CutsceneTab.this.doc.keys.size(); i++) {
                        if (Math.abs(mx - this.tx(CutsceneTab.this.doc.keys.get(i).t)) <= 6.0 && my >= this.y + 24 && my <= this.y + 44) {
                            if (i != CutsceneTab.this.sel) {
                                CutsceneTab.this.select(i);
                            }

                            CutsceneTab.this.snapshot();
                            this.dragKey = i;
                            return true;
                        }
                    }

                    this.scrubbing = true;
                    CutsceneTab.this.playStarted = -1L;
                    CutsceneTab.this.playhead = Math.min((double)CutsceneTab.this.doc.duration(), this.tAt(mx));
                    return true;
                }
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.dragClip >= 0 && this.dragClip < CutsceneTab.this.doc.clips.size()) {
                CutsceneDoc.Clip cl = CutsceneTab.this.doc.clips.get(this.dragClip);
                cl.t = (float)CutsceneTab.round(CutsceneTab.snapped(this.tAt(mx)));
                CutsceneTab.this.resortClips();
                this.dragClip = CutsceneTab.this.doc.clips.indexOf(cl);
                return true;
            } else if (this.dragKey >= 0 && this.dragKey < CutsceneTab.this.doc.keys.size()) {
                double snap = Screen.hasShiftDown() ? 0.5 : 0.05;
                CutsceneTab.this.doc.keys.get(this.dragKey).t = (float)(Math.round(this.tAt(mx) / snap) * snap);
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(this.dragKey);
                CutsceneTab.this.resort();
                this.dragKey = CutsceneTab.this.doc.keys.indexOf(k);
                return true;
            } else if (this.scrubbing) {
                CutsceneTab.this.playhead = Math.min((double)CutsceneTab.this.doc.duration(), this.tAt(mx));
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (this.dragKey >= 0) {
                CutsceneTab.this.buildForm();
            }

            this.dragKey = -1;
            this.dragClip = -1;
            this.scrubbing = false;
            return true;
        }
    }

    private final class View3D extends Widget implements UI.CursorProvider {
        boolean rightDown;
        boolean looking;
        int rightMoved;
        int rightX;
        int rightY;
        int dragging = -1;

        View3D() {
            this.visible = false;
        }

        @Override
        public int cursor(int mx, int my) {
            return this.dragging < 0 && this.frameAt(mx, my) < 0 ? 221185 : 221188;
        }

        void stopHolding() {
            this.rightDown = false;
            this.dragging = -1;
            CameraDirector.flyOverScreen = false;
            if (this.looking) {
                this.looking = false;
                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), 208897, 212993);
            }
        }

        private Vec3[] basis() {
            CutsceneDoc.Pose p = CameraDirector.pose();
            Vec3 forward = Vec3.directionFromRotation(p.pitch, p.yaw);
            Vec3 right = Vec3.directionFromRotation(0.0F, p.yaw + 90.0F);
            return new Vec3[]{forward, right, forward.cross(right).scale(-1.0)};
        }

        private float[] project(Vec3 at) {
            UI ui = UI.get();
            CutsceneDoc.Pose p = CameraDirector.pose();
            Vec3[] b = this.basis();
            Vec3 v = at.subtract(p.x, p.y, p.z);
            double z = v.dot(b[0]);
            if (z < 0.05) {
                return null;
            } else {
                double tan = Math.tan(Math.toRadians(p.fov) / 2.0);
                double nx = v.dot(b[1]) / (z * tan * ui.vw / ui.vh);
                double ny = v.dot(b[2]) / (z * tan);
                return new float[]{(float)((nx + 1.0) / 2.0 * ui.vw), (float)((1.0 - ny) / 2.0 * ui.vh)};
            }
        }

        int frameAt(double mx, double my) {
            if (CutsceneTab.this.throughCamera) {
                return -1;
            } else {
                Vec3 o = CutsceneTab.this.origin();
                int best = -1;
                double bestD = 100.0;

                for (int i = 0; i < CutsceneTab.this.doc.keys.size(); i++) {
                    CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(i);
                    float[] s = this.project(new Vec3(k.x + o.x, k.y + o.y, k.z + o.z));
                    if (s != null) {
                        double d = (s[0] - mx) * (s[0] - mx) + (s[1] - my) * (s[1] - my);
                        if (d < bestD) {
                            bestD = d;
                            best = i;
                        }
                    }
                }

                return best;
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            if (CutsceneTab.this.throughCamera) {
                c.textEllipsized("Вид из камеры · ПКМ — вернуться к свободному полёту", this.x + 6, this.y + 5, this.w - 12, -855638017);
            } else {
                c.textEllipsized(
                    "Зажмите ПКМ: мышь и WASD — полёт · колёсико — скорость · ЛКМ по кадру — выбрать и двигать",
                    this.x + 6,
                    this.y + 5,
                    this.w - 12,
                    -855638017
                );
                Vec3 o = CutsceneTab.this.origin();

                for (int i = 0; i < CutsceneTab.this.doc.keys.size(); i++) {
                    CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(i);
                    float[] s = this.project(new Vec3(k.x + o.x, k.y + o.y, k.z + o.z));
                    if (s != null && i == CutsceneTab.this.sel && s[0] > this.x && s[0] < this.x + this.w && s[1] > this.y && s[1] < this.y + this.h) {
                        c.outline((int)s[0] - 7, (int)s[1] - 7, 14, 14, -13461);
                    }
                }
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (button == 1) {
                this.rightDown = true;
                this.rightMoved = 0;
                this.rightX = (int)mx;
                this.rightY = (int)my;
                CameraDirector.flyOverScreen = true;
                return true;
            } else if (button != 0) {
                return false;
            } else {
                int f = this.frameAt(mx, my);
                if (f >= 0) {
                    if (f != CutsceneTab.this.sel) {
                        CutsceneTab.this.select(f);
                    }

                    CutsceneTab.this.snapshot();
                    this.dragging = f;
                } else if (CutsceneTab.this.clipSel >= 0 || CutsceneTab.this.sel >= 0) {
                    CutsceneTab.this.select(-1);
                }

                return true;
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.rightDown && !this.looking) {
                this.rightMoved = (int)(this.rightMoved + (Math.abs(dx) + Math.abs(dy)));
                if (this.rightMoved < 4) {
                    return true;
                }

                this.looking = true;
                if (CutsceneTab.this.throughCamera) {
                    CutsceneTab.this.throughCamera = false;
                    CameraDirector.flyFree();
                }

                GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), 208897, 212995);
            }

            if (this.looking) {
                LocalPlayer p = Minecraft.getInstance().player;
                if (p != null) {
                    float yaw = p.getYRot() + (float)dx * 0.35F;
                    float pitch = Math.max(-90.0F, Math.min(90.0F, p.getXRot() + (float)dy * 0.35F));
                    p.setYRot(yaw);
                    p.setXRot(pitch);
                    p.yRotO = yaw;
                    p.xRotO = pitch;
                }

                return true;
            } else if (this.dragging >= 0 && this.dragging < CutsceneTab.this.doc.keys.size()) {
                UI ui = UI.get();
                CutsceneDoc.Key k = CutsceneTab.this.doc.keys.get(this.dragging);
                CutsceneDoc.Pose cam = CameraDirector.pose();
                Vec3 o = CutsceneTab.this.origin();
                Vec3[] b = this.basis();
                double dist = Math.max(1.0, new Vec3(k.x + o.x, k.y + o.y, k.z + o.z).subtract(cam.x, cam.y, cam.z).dot(b[0]));
                double perPixel = 2.0 * dist * Math.tan(Math.toRadians(cam.fov) / 2.0) / ui.vh;
                Vec3 move = b[1].scale(dx * perPixel).add(b[2].scale(-dy * perPixel));
                k.x = CutsceneTab.round(k.x + move.x);
                k.y = CutsceneTab.round(k.y + move.y);
                k.z = CutsceneTab.round(k.z + move.z);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (this.rightDown) {
                boolean clicked = !this.looking;
                int cx = this.rightX;
                int cy = this.rightY;
                this.stopHolding();
                if (clicked) {
                    this.menu(cx, cy);
                }

                return true;
            } else {
                if (this.dragging >= 0) {
                    CutsceneTab.this.buildForm();
                }

                this.dragging = -1;
                return true;
            }
        }

        private void menu(int mx, int my) {
            PopupMenu pm = new PopupMenu();
            pm.item("Кадр здесь", Icon.PLUS, null, CutsceneTab.this::addHere);
            pm.item(
                "Скрипт на " + CutsceneTab.fmt(CutsceneTab.round(CutsceneTab.snapped(CutsceneTab.this.playhead))) + " с",
                Icon.CODE,
                null,
                () -> CutsceneTab.this.addClip(CutsceneTab.this.playhead)
            );
            pm.show(mx, my);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            CameraDirector.flySpeed = Math.max(0.5, Math.min(64.0, CameraDirector.flySpeed * (delta > 0.0 ? 1.25 : 0.8)));
            return true;
        }
    }
}
