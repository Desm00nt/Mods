package org.sprautestudio.spraute_engine.editor.engine;

import com.mojang.blaze3d.platform.Window;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.sprautestudio.spraute_engine.addon.AddonClient;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.gui.TextValues;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.PlateLayout;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.GuiSchema;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.gui.client.MenuLook;
import org.sprautestudio.spraute_engine.gui.client.MenuRuntime;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.quest.client.JournalParts;
import org.sprautestudio.spraute_engine.quest.client.QuestClient;
import org.sprautestudio.spraute_engine.shop.client.ShopParts;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.ColorPicker;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class EngineStudio extends EditorTab {
    private static final int TOP = 46;
    private static final int LEFT = 236;
    private static final int RIGHT = 290;
    private static final int STATUS = 24;
    private static final String MENUS = "menus";
    private static final List<EngineStudio.Scene> SCENES = List.of(
        new EngineStudio.Scene("hud", "Игра (HUD)", "СЦЕНЫ", Icon.USER, "Что видно во время игры"),
        new EngineStudio.Scene("choice", "Меню выбора", "СЦЕНЫ", Icon.LIST, "Вопрос персонажа и ответы"),
        new EngineStudio.Scene("toast", "Уведомление квеста", "СЦЕНЫ", Icon.FLAG, "«Новый квест», «Задача выполнена»…"),
        new EngineStudio.Scene("zone", "Название зоны", "СЦЕНЫ", Icon.PIN, "Надпись при входе в зону"),
        new EngineStudio.Scene("dialog", "Реплики", "СЦЕНЫ", Icon.NOTE, "Сообщения персонажей внизу экрана: фон, портрет, имя, текст"),
        new EngineStudio.Scene("chatlog", "История чата", "СЦЕНЫ", Icon.NOTE, "Фон и рамка истории сообщений"),
        new EngineStudio.Scene("quest", "Журнал квестов", "ЭКРАНЫ", Icon.BOOK, "Журнал (J): вкладки, список, страница квеста"),
        new EngineStudio.Scene("shop", "Магазин", "ЭКРАНЫ", Icon.TAG, "Окно торговли: товары, выбранный товар, цена, кнопка"),
        new EngineStudio.Scene("inventory", "Инвентарь", "ЭКРАНЫ", Icon.CUBE, "Свой инвентарь: где ячейки, модель игрока, фон"),
        new EngineStudio.Scene("skills", "Прокачка", "ЭКРАНЫ", Icon.GRAPH, "Панель прокачки по частям: двигайте и добавляйте. Макет 1"),
        new EngineStudio.Scene("skills2", "Прокачка, макет 2", "ЭКРАНЫ", Icon.GRAPH, "Второй вид панели: например, для отдельного меню"),
        new EngineStudio.Scene("skills3", "Прокачка, макет 3", "ЭКРАНЫ", Icon.GRAPH, "Третий вид панели"),
        new EngineStudio.Scene("skillslook", "Прокачка: цвета", "ЭКРАНЫ", Icon.GRAPH, "Цвета и текстуры навыков и кнопок, общие для всех её меню"),
        new EngineStudio.Scene("menus", "Меню игры", "ЭКРАНЫ", Icon.LIST, "Главное меню, пауза, настройки, окна с предметами и другие экраны самой игры"),
        new EngineStudio.Scene("addons", "Аддоны", "СИСТЕМЫ", Icon.GEAR, "Какие аддоны загружены, что они добавили и что с ними не так"),
        new EngineStudio.Scene("systems", "Системы и время", "СИСТЕМЫ", Icon.GEAR, "Здоровье и монеты Spraute, инвентарь, слоты, чат, время суток"),
        new EngineStudio.Scene("speech", "Речь", "СИСТЕМЫ", Icon.USER, "Имена говорящих, подсказка F8, голова игрока, кого показывает меню выбора")
    );
    private static final String OWN_LOGO = "own_logo";
    private static final List<EngineStudio.Device> DEVICES = List.of(
        new EngineStudio.Device("16:9 · 1920×1080", 480, 270),
        new EngineStudio.Device("16:10 · 1920×1200", 480, 300),
        new EngineStudio.Device("4:3 · 1600×1200", 400, 300),
        new EngineStudio.Device("21:9 · 2560×1080", 640, 270),
        new EngineStudio.Device("16:9 · мелкий интерфейс", 640, 360)
    );
    private static String skillsPage = "skills";
    private static final List<String> SKILLS_PAGES = List.of("skills", "skills2", "skills3", "skillslook");
    private int ox;
    private int oy;
    private int vw;
    private int vh;
    private final EngineConfig cfg;
    private String savedJson;
    private String scene = "hud";
    private EngineStudio.Device device = devices().get(0);
    private EngineStudio.Device deviceBefore;
    private final Map<String, GuiDoc> docs = new HashMap<>();
    private final Map<String, GuiView> views = new HashMap<>();
    private final Map<String, Deque<String>> undo = new HashMap<>();
    private final Map<String, Deque<String>> redo = new HashMap<>();
    private final Map<String, EngineTab> pages = new HashMap<>();
    private GuiDoc.Element selected;
    private final Button applyBtn;
    private final Button saveBtn;
    private final Button texturesBtn;
    private final Button closeBtn;
    private final Button undoBtn;
    private final Button redoBtn;
    private final Dropdown<EngineStudio.Device> deviceBox;
    private final EngineStudio.Left left = new EngineStudio.Left();
    private final EngineStudio.Viewport viewport = new EngineStudio.Viewport();
    private final EngineStudio.Inspector inspector = new EngineStudio.Inspector();
    private final ScrollView inspectorScroll;
    private static final String[][] GRID = new String[][]{
        {"top_left", "top", "top_right"}, {"left", "center", "right"}, {"bottom_left", "bottom", "bottom_right"}
    };

    private static String skillsPageTitle(String page) {
        return switch (page) {
            case "skills2" -> "макет 2";
            case "skills3" -> "макет 3";
            case "skillslook" -> "цвета и текстуры";
            default -> "макет 1";
        };
    }

    private static List<EngineStudio.Scene> scenes() {
        List<EngineStudio.Scene> all = new ArrayList<>(SCENES);
        all.removeIf(s -> s.key().startsWith("skills"));

        for (AddonClient.Window w : AddonClient.windows()) {
            all.add(new EngineStudio.Scene(w.role(), w.title(), "ОКНА АДДОНОВ", Icon.GEAR, w.help()));
        }

        return all;
    }

    private static boolean isWindow(String key) {
        return EngineWindows.ROLES.contains(key) || isMenu(key);
    }

    private static boolean isMenu(String key) {
        return MenuRuntime.isMenu(key);
    }

    private static EngineStudio.Scene sceneOf(String key) {
        if (isMenu(key)) {
            key = "menus";
        }

        for (EngineStudio.Scene s : scenes()) {
            if (s.key().equals(key)) {
                return s;
            }
        }

        return SCENES.get(0);
    }

    private static List<EngineStudio.Device> devices() {
        List<EngineStudio.Device> out = new ArrayList<>();
        Window win = Minecraft.getInstance().getWindow();
        int gw = Math.max(1, win.getGuiScaledWidth());
        int gh = Math.max(1, win.getGuiScaledHeight());
        out.add(new EngineStudio.Device("Ваш экран · " + win.getWidth() + "×" + win.getHeight(), gw, gh));
        out.addAll(DEVICES);
        return out;
    }

    public EngineStudio(Project project) {
        super(project, null);
        this.cfg = EngineTab.load(project);
        this.cfg.project = project.name();
        this.savedJson = this.cfg.toJson();
        this.applyBtn = new Button("Сохранить и применить", this::apply).icon(Icon.UPLOAD).style(Button.Style.PRIMARY);
        this.applyBtn.tooltip("Ctrl+S — в проект (engine.json) и сразу в мир, всем игрокам");
        this.saveBtn = new Button("Сохранить", this::saveOnly).icon(Icon.SAVE).style(Button.Style.DEFAULT);
        this.saveBtn.tooltip("Только в проект");
        this.texturesBtn = new Button("Текстуры", this::exportTextures).icon(Icon.IMAGE).style(Button.Style.GHOST);
        this.texturesBtn.tooltip("Архив всех картинок движка по папкам и полям — рисовать свои поверх них");
        this.closeBtn = new Button("Стандартный вид", this::standardLook).icon(Icon.REFRESH).style(Button.Style.GHOST);
        this.closeBtn.tooltip("Вернуть окну сцены стандартную раскладку (Ctrl+Z — обратно)");
        this.undoBtn = Button.icon(Icon.UNDO, "Отменить (Ctrl+Z)", this::undoScene);
        this.redoBtn = Button.icon(Icon.REDO, "Повторить (Ctrl+Y)", this::redoScene);

        for (Button b : List.of(this.applyBtn, this.saveBtn, this.texturesBtn, this.closeBtn)) {
            b.autoWidth();
        }

        this.deviceBox = new Dropdown<>(EngineStudio::devices, () -> this.device, EngineStudio.Device::title, d -> {
            this.device = d;
            this.relayout();
        });
        this.deviceBox.tooltip("Формат экрана: посмотреть раскладку на других мониторах");
        this.inspectorScroll = new ScrollView(this.inspector) {
            @Override
            protected int contentHeight() {
                return EngineStudio.this.inspector.prefH;
            }
        };
        this.add(new EngineStudio.Chrome());
        this.openScene("hud");
    }

    @Override
    public String title() {
        return this.project.name();
    }

    @Override
    public Icon icon() {
        return Icon.GEAR;
    }

    @Override
    public int iconColor() {
        return -5141249;
    }

    @Override
    public boolean isDirty() {
        return this.dirty();
    }

    @Override
    public void save() {
        this.write();
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
        this.viewport.requestFocus();
    }

    @Override
    public void undo() {
        this.undoScene();
    }

    @Override
    public void redo() {
        this.redoScene();
    }

    private void relayout() {
        UI ui = UI.get();
        if (ui != null) {
            ui.relayout();
        }
    }

    private static void toastInfo(String t) {
        if (UI.get() != null) {
            UI.get().toastInfo(t);
        }
    }

    private static void toastSuccess(String t) {
        if (UI.get() != null) {
            UI.get().toastSuccess(t);
        }
    }

    private static void toastError(String t) {
        if (UI.get() != null) {
            UI.get().toastError(t);
        }
    }

    @Override
    public void layout() {
        this.ox = this.x;
        this.oy = this.y;
        this.vw = this.w;
        this.vh = this.h;

        for (Widget c : this.children()) {
            c.bounds(this.x, this.y, this.w, this.h);
        }

        super.layout();
    }

    private GuiDoc doc(String role) {
        return this.docs.computeIfAbsent(role, r -> {
            String json = this.cfg.layout(r);
            if (json == null) {
                json = EngineWindows.build(r, this.cfg);
                this.cfg.layoutJson.put(r, json);
            }

            GuiDoc d = GuiDoc.fromJson(json);
            if (r.equals("dialog") && PlateLayout.onlyPlate(d)) {
                PlateLayout.fill(d.elements.get(0), this.cfg.chatLog);
            }

            if (r.equals("hud")) {
                this.growthOnCanvas(d);
            }

            if (r.equals("inventory")) {
                this.craftWindow(d);
            }

            return d;
        });
    }

    private void craftWindow(GuiDoc d) {
        if (d.element("craft_window") == null) {
            List<GuiDoc.Element> parts = new ArrayList<>();

            for (String id : List.of("craft", "craft_arrow", "craft_result")) {
                GuiDoc.Element e = null;

                for (GuiDoc.Element root : d.elements) {
                    if (id.equals(root.id)) {
                        e = root;
                    }
                }

                if (e == null || !e.get("anchor").equals("top_left")) {
                    return;
                }

                parts.add(e);
            }

            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            int right = Integer.MIN_VALUE;
            int bottom = Integer.MIN_VALUE;
            int index = d.elements.size();

            for (GuiDoc.Element e : parts) {
                left = Math.min(left, e.i("x"));
                top = Math.min(top, e.i("y"));
                right = Math.max(right, e.i("x") + e.i("w"));
                bottom = Math.max(bottom, e.i("y") + e.i("h"));
                index = Math.min(index, d.elements.indexOf(e));
            }

            int x = left - 4;
            int y = top - 4;
            GuiDoc.Element group = EngineWindows.el("group", "craft_window", "top_left", x, y, right - x + 4, bottom - y + 4);

            for (GuiDoc.Element e : parts) {
                e.set("x", String.valueOf(e.i("x") - x));
                e.set("y", String.valueOf(e.i("y") - y));
                group.children.add(e);
            }

            d.elements.removeAll(parts);
            d.elements.add(Math.min(index, d.elements.size()), group);
        }
    }

    private void growthOnCanvas(GuiDoc d) {
    }

    private void growthFromCanvas() {
        boolean abilities = false;
        boolean level = false;

        for (GuiDoc.Element e : this.doc("hud").all()) {
            abilities |= e.type.equals("hud_skill_abilities");
            level |= e.type.equals("hud_skill_level");
        }

        this.cfg.skillHud.abilities.visible = abilities;
        this.cfg.skillHud.level.visible = level;
    }

    private GuiView view(String role) {
        return this.views.computeIfAbsent(role, r -> {
            GuiView v = new GuiView(this.doc(r), this.project.name());
            v.interactive = false;
            v.playAppear = false;
            v.drawBackground = false;
            return v;
        });
    }

    private void changed(String role) {
        if (role.equals("dialog")) {
            this.plateChanged();
        }

        if (role.equals("hud")) {
        }

        this.cfg.layoutJson.put(role, this.doc(role).toJson());
        if (isMenu(role)) {
            MenuRuntime.preview(role, this.doc(role), this.project.name());
        }

        this.view(role).layoutTree();
        this.inspector.refreshValues();
    }

    private void plateChanged() {
        GuiDoc d = this.doc("dialog");
        boolean alone = PlateLayout.onlyPlate(d);
        this.cfg.chatLog.speechCustom = !alone;
        if (alone) {
            GuiDoc.Element plate = d.elements.get(0);
            PlateLayout.read(plate, this.cfg.chatLog, this.device.w());
            PlateLayout.fill(plate, this.cfg.chatLog);
        }
    }

    private void plateSettings() {
        GuiDoc d = this.doc("dialog");
        if (PlateLayout.onlyPlate(d)) {
            PlateLayout.fill(d.elements.get(0), this.cfg.chatLog);
        }

        this.changed("dialog");
    }

    private void snapshot(String role) {
        this.undo.computeIfAbsent(role, k -> new ArrayDeque<>()).push(this.doc(role).toJson());
        if (this.undo.get(role).size() > 200) {
            this.undo.get(role).removeLast();
        }

        this.redo.computeIfAbsent(role, k -> new ArrayDeque<>()).clear();
    }

    private void restore(String role, String json) {
        String id = this.selected != null ? this.selected.id : null;
        GuiDoc d = GuiDoc.fromJson(json);
        this.docs.put(role, d);
        this.views.remove(role);
        this.cfg.layoutJson.put(role, json);
        this.selected = id != null ? d.element(id) : null;
        this.inspector.rebuild();
    }

    private void undoScene() {
        if (isWindow(this.scene)) {
            Deque<String> u = this.undo.get(this.scene);
            if (u != null && !u.isEmpty()) {
                this.redo.computeIfAbsent(this.scene, k -> new ArrayDeque<>()).push(this.doc(this.scene).toJson());
                this.restore(this.scene, u.pop());
            }
        }
    }

    private void redoScene() {
        if (isWindow(this.scene)) {
            Deque<String> r = this.redo.get(this.scene);
            if (r != null && !r.isEmpty()) {
                this.undo.computeIfAbsent(this.scene, k -> new ArrayDeque<>()).push(this.doc(this.scene).toJson());
                this.restore(this.scene, r.pop());
            }
        }
    }

    private void openScene(String key) {
        if (key.equals("hud") && this.docs.containsKey("hud")) {
            this.growthOnCanvas(this.docs.get("hud"));
            this.cfg.layoutJson.put("hud", this.docs.get("hud").toJson());
            this.views.remove("hud");
        }

        if (key.startsWith("skills")) {
            skillsPage = key;
        }

        EngineTab.skillsPages = new EngineTab.SkillsPages(SKILLS_PAGES, EngineStudio::skillsPageTitle, () -> skillsPage, this::openScene);
        this.scene = key.equals("menus") ? MenuRuntime.roles().get(0) : key;
        key = this.scene;
        if (isMenu(this.scene)) {
            if (this.deviceBefore == null) {
                this.deviceBefore = this.device;
            }

            GuiDoc menu = this.doc(this.scene);
            this.device = new EngineStudio.Device("Размер, в котором прочитаны кнопки", menu.width(), menu.height());
        } else if (this.deviceBefore != null) {
            this.device = this.deviceBefore;
            this.deviceBefore = null;
        }

        this.selected = null;
        if (!isWindow(key)) {
            this.pages.computeIfAbsent(key, k -> new EngineTab(this.project, this.cfg, k));
        }

        this.inspector.rebuild();
        this.relayout();
    }

    private boolean dirty() {
        return !this.cfg.toJson().equals(this.savedJson);
    }

    private boolean write() {
        this.cfg.project = this.project.name();
        this.cfg.layoutJson.keySet().removeIf(k -> !isWindow(k));
        String json = this.cfg.toJson();

        try {
            Files.writeString(EngineTab.file(this.project), json, StandardCharsets.UTF_8);
            MenuRuntime.saved(this.project.name(), this.cfg.layoutJson);
        } catch (IOException var3) {
            toastError("Не удалось сохранить: " + var3.getMessage());
            return false;
        }

        this.savedJson = json;
        return true;
    }

    private void saveOnly() {
        if (this.write()) {
            toastSuccess("Сохранено в проект");
        }
    }

    private void apply() {
        if (this.write()) {
            if (Minecraft.getInstance().getConnection() == null) {
                toastInfo("Сохранено. В мир применится, когда вы в него войдёте");
            } else {
                ClientState.get().engine(this.savedJson);
                toastSuccess("Применено в мире");
            }
        }
    }

    private void exportTextures() {
        try {
            Path zip = TextureExport.export(this.project.dir);
            Util.getPlatform().openFile(this.project.dir.toFile());
            toastSuccess("Сохранено: " + zip.getFileName());
        } catch (Exception var2) {
            toastError("Не удалось сохранить архив: " + var2.getMessage());
        }
    }

    private void standardLook() {
        if (isWindow(this.scene)) {
            this.snapshot(this.scene);
            if (this.scene.equals("dialog")) {
                EngineConfig.ChatLog plain = new EngineConfig.ChatLog();
                this.cfg.chatLog.speechPlace = plain.speechPlace;
                this.cfg.chatLog.speechShift = plain.speechShift;
                this.cfg.chatLog.speechAlign = plain.speechAlign;
                this.cfg.chatLog.speechShiftX = plain.speechShiftX;
                this.cfg.chatLog.speechIconShown = plain.speechIconShown;
                this.cfg.chatLog.speechIconSize = plain.speechIconSize;
                this.cfg.chatLog.speechIconX = plain.speechIconX;
                this.cfg.chatLog.speechIconY = plain.speechIconY;
                this.cfg.chatLog.speechTextX = plain.speechTextX;
                this.cfg.chatLog.speechTextY = plain.speechTextY;
                this.cfg.chatLog.speechWidth = plain.speechWidth;
                this.cfg.chatLog.speechBack = plain.speechBack;
                this.cfg.chatLog.speechText = plain.speechText;
                this.cfg.chatLog.speechCustom = false;
            }

            this.restore(this.scene, EngineWindows.build(this.scene, EngineConfig.fromJson("{}")));
            toastInfo("Стандартный вид. Ctrl+Z — вернуть как было");
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = (mods & 2) != 0;
        boolean shift = (mods & 1) != 0;
        if (key == 256) {
            if (this.selected != null) {
                this.select(null);
                return true;
            } else {
                return false;
            }
        } else if (ctrl && key == 83) {
            this.apply();
            return true;
        } else if (ctrl && key == 90) {
            if (shift) {
                this.redoScene();
            } else {
                this.undoScene();
            }

            return true;
        } else if (ctrl && key == 89) {
            this.redoScene();
            return true;
        } else {
            if (isWindow(this.scene) && this.selected != null && this.viewport.isFocused()) {
            }

            return false;
        }
    }

    private boolean inPlate(GuiDoc.Element e) {
        return e != null && (e.type.equals("speech_plate") || PlateLayout.isPart(this.doc(this.scene), e));
    }

    private boolean hasHandles(GuiDoc.Element e) {
        return !this.inPlate(e) || e.type.equals("speech_icon");
    }

    private void select(GuiDoc.Element e) {
        this.selected = e;
        this.inspector.rebuild();
    }

    private static String freshId(GuiDoc d, String base) {
        String b = base.replace("hud_", "");
        if (d.element(b) == null) {
            return b;
        } else {
            int i = 2;

            while (d.element(b + i) != null) {
                i++;
            }

            return b + i;
        }
    }

    private void addElement(String type) {
    }

    private void addParallaxLayer() {
    }

    private static List<GuiDoc.Element> parentList(GuiDoc d, GuiDoc.Element e) {
        if (d.elements.contains(e)) {
            return d.elements;
        } else {
            for (GuiDoc.Element p : d.all()) {
                if (p.children.contains(e)) {
                    return p.children;
                }
            }

            return d.elements;
        }
    }

    private void deleteSelected() {
        if (this.selected != null) {
        }
    }

    private void duplicateSelected() {
        if (this.selected != null && !this.inPlate(this.selected)) {
        }
    }

    private void move(int dir) {
        if (this.selected != null) {
            List<GuiDoc.Element> l = parentList(this.doc(this.scene), this.selected);
            int i = l.indexOf(this.selected);
            int j = i + dir;
            if (j >= 0 && j < l.size()) {
                this.snapshot(this.scene);
                l.remove(i);
                l.add(j, this.selected);
                this.changed(this.scene);
            }
        }
    }

    private static String typeTitle(String type) {
        GuiSchema.Type t = GuiSchema.type(type);
        return t != null ? t.title() : type;
    }

    private void addMenu(int mx, int my) {
        PopupMenu pm = new PopupMenu();

        for (GuiSchema.Type t : GuiSchema.windowTypes(this.scene)) {
            pm.item(t.title(), () -> this.addElement(t.key()));
        }

        pm.show(mx, my);
    }

    private void stretch(GuiDoc.Element e, String how) {
        this.snapshot(this.scene);
        String st = e.get("stretch");
        boolean sw = st.equals("width") || st.equals("both");
        boolean sh = st.equals("height") || st.equals("both");
        switch (how) {
            case "width":
                sw = true;
                break;
            case "height":
                sh = true;
                break;
            case "both":
                sh = true;
                sw = true;
                break;
            default:
                sh = false;
                sw = false;
        }

        e.set("stretch", sw && sh ? "both" : (sw ? "width" : (sh ? "height" : "none")));
        this.changed(this.scene);
        this.inspector.rebuild();
    }

    private static String anchorTitle(String a) {
        return switch (a) {
            case "top_left" -> "левый верх";
            case "top" -> "верх, центр";
            case "top_right" -> "правый верх";
            case "left" -> "левый край";
            case "right" -> "правый край";
            case "bottom_left" -> "левый низ";
            case "bottom" -> "низ, центр";
            case "bottom_right" -> "правый низ";
            default -> "центр";
        };
    }

    private final class AnchorPicker extends Widget implements UI.CursorProvider {
        private final GuiDoc.Element e;

        AnchorPicker(GuiDoc.Element e) {
            this.e = e;
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            int s = 46;
            c.roundBorder(this.x, this.y, s, s, 4, -15592678, -12762793);
            String cur = this.e.get("anchor");

            for (int r = 0; r < 3; r++) {
                for (int col = 0; col < 3; col++) {
                    int cx = this.x + Math.round((col + 0.5F) * s / 3.0F);
                    int cyy = this.y + Math.round((r + 0.5F) * s / 3.0F);
                    boolean on = EngineStudio.GRID[r][col].equals(cur);
                    boolean hov = mx >= this.x + col * s / 3 && mx < this.x + (col + 1) * s / 3 && my >= this.y + r * s / 3 && my < this.y + (r + 1) * s / 3;
                    int d = on ? 9 : 7;
                    c.round(cx - d / 2, cyy - d / 2, d, d, d / 2, on ? -7639809 : (hov ? -6577995 : -9933435));
                }
            }

            c.text("Привязка: " + EngineStudio.anchorTitle(cur), this.x + s + 10, this.y + 6, -1841682);
            c.wrapped(
                "Точка экрана (или группы), от которой считаются X и Y. Сменить — часть останется на месте",
                this.x + s + 10,
                this.y + 18,
                this.w - s - 12,
                -9933435,
                0
            );
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            int col = (int)((mx - this.x) / 15.333333F);
            int r = (int)((my - this.y) / 15.333333F);
            if (!(mx > this.x + 46) && col >= 0 && col <= 2 && r >= 0 && r <= 2) {
                String a = EngineStudio.GRID[r][col];
                if (a.equals(this.e.get("anchor"))) {
                    return true;
                } else {
                    GuiView v = EngineStudio.this.view(EngineStudio.this.scene);
                    int[] rect = (int[])v.rect(this.e).clone();
                    EngineStudio.this.snapshot(EngineStudio.this.scene);
                    this.e.set("anchor", a);
                    v.layoutTree();
                    int[] off = v.offsetFor(this.e, rect[0], rect[1], rect[2], rect[3]);
                    this.e.set("x", String.valueOf(off[0]));
                    this.e.set("y", String.valueOf(off[1]));
                    EngineStudio.this.changed(EngineStudio.this.scene);
                    return true;
                }
            } else {
                return true;
            }
        }

        @Override
        public int cursor(int mx, int my) {
            return 221188;
        }
    }

    private final class Chrome extends Widget {
        Chrome() {
            for (Widget b : List.of(
                EngineStudio.this.applyBtn,
                EngineStudio.this.saveBtn,
                EngineStudio.this.texturesBtn,
                EngineStudio.this.closeBtn,
                EngineStudio.this.undoBtn,
                EngineStudio.this.redoBtn,
                EngineStudio.this.deviceBox
            )) {
                this.add(b);
            }

            this.add(EngineStudio.this.left);
            this.add(EngineStudio.this.viewport);
            this.add(EngineStudio.this.inspectorScroll);
        }

        private Widget body() {
            return EngineStudio.isWindow(EngineStudio.this.scene) ? null : EngineStudio.this.pages.get(EngineStudio.this.scene);
        }

        @Override
        public void layout() {
            int bx = EngineStudio.this.ox + EngineStudio.this.vw - 12;

            for (Button b : List.of(EngineStudio.this.closeBtn, EngineStudio.this.applyBtn, EngineStudio.this.saveBtn, EngineStudio.this.texturesBtn)) {
                bx -= b.prefW;
                b.bounds(bx, EngineStudio.this.oy + 11, b.prefW, 24);
                bx -= 6;
            }

            if (EngineStudio.isWindow(EngineStudio.this.scene)) {
            }

            boolean win = false;
            EngineStudio.this.viewport.visible = false;
            int cx = EngineStudio.this.ox + 236 + 12;
            EngineStudio.this.closeBtn.visible = win;
            EngineStudio.this.undoBtn.bounds(win ? cx : -10000, EngineStudio.this.oy + 12, 22, 22);
            EngineStudio.this.redoBtn.bounds(win ? cx + 24 : -10000, EngineStudio.this.oy + 12, 22, 22);
            EngineStudio.this.deviceBox.bounds(win && !EngineStudio.isMenu(EngineStudio.this.scene) ? cx + 56 : -10000, EngineStudio.this.oy + 13, 180, 20);
            EngineStudio.this.left.bounds(EngineStudio.this.ox, EngineStudio.this.oy + 46, 236, EngineStudio.this.vh - 46 - 24);
            Widget body = this.body();

            for (Widget c : new ArrayList<>(this.children())) {
                if (c instanceof EngineTab && c != body) {
                    this.remove(c);
                }
            }

            if (body != null) {
                if (body.parent() != this) {
                    this.add(body);
                }

                body.bounds(EngineStudio.this.ox + 236 + 1, EngineStudio.this.oy + 46 + 1, EngineStudio.this.vw - 236 - 1, EngineStudio.this.vh - 46 - 24 - 1);
                EngineStudio.this.viewport.bounds(-10000, 0, 10, 10);
                EngineStudio.this.inspectorScroll.bounds(-10000, 0, 10, 10);
            } else {
                EngineStudio.this.viewport.bounds(-10000, 0, 10, 10);
                EngineStudio.this.inspectorScroll
                    .bounds(EngineStudio.this.ox + 236 + 1, EngineStudio.this.oy + 46 + 1, EngineStudio.this.vw - 236 - 1, EngineStudio.this.vh - 46 - 24 - 1);
            }

            super.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            int X = EngineStudio.this.ox;
            int Y = EngineStudio.this.oy;
            c.fill(X, Y, EngineStudio.this.vw, EngineStudio.this.vh, -15329507);
            c.fill(X, Y, EngineStudio.this.vw, 46, -14934490);
            c.fill(X, Y + 46, EngineStudio.this.vw, 1, -13749952);
            c.text(EngineStudio.sceneOf(EngineStudio.this.scene).title(), X + 14, Y + 12, -1841682);
            c.text(EngineStudio.sceneOf(EngineStudio.this.scene).doc(), X + 14, Y + 25, -9933435);
            if (EngineStudio.isWindow(EngineStudio.this.scene)) {
                c.fill(X + 236 + 1, Y + 8, 1, 30, -13749952);
                c.fill(X + EngineStudio.this.vw - 290 - 1, Y + 46, 1, EngineStudio.this.vh - 46 - 24, -13749952);
            }

            int sy = Y + EngineStudio.this.vh - 24;
            c.fill(X, sy, EngineStudio.this.vw, 24, -14934490);
            c.fill(X, sy, EngineStudio.this.vw, 1, -13749952);
            boolean d = EngineStudio.this.dirty();
            c.round(X + 12, sy + 9, 7, 7, 3, d ? -870325 : -11746422);
            c.text(d ? "Есть несохранённые изменения · Ctrl+S — сохранить и применить" : "Всё сохранено", X + 26, sy + 8, -6577995);
            if (EngineStudio.isWindow(EngineStudio.this.scene)) {
                String hint = "Картинки и доступные параметры окна — справа";
                c.textEllipsized(hint, X + EngineStudio.this.vw / 2, sy + 8, EngineStudio.this.vw / 2 - 12, -9933435);
            }
        }
    }

    private record Device(String title, int w, int h) {
    }

    private final class Inspector extends Widget {
        private final List<Runnable> refreshers = new ArrayList<>();

        void refreshValues() {
            for (Runnable r : this.refreshers) {
                r.run();
            }
        }

        void rebuild() {
            this.clear();
            this.refreshers.clear();
            if (EngineStudio.isWindow(EngineStudio.this.scene)) {
                if (EngineStudio.this.selected != null && EngineStudio.this.doc(EngineStudio.this.scene).all().contains(EngineStudio.this.selected)) {
                    boolean var6 = true;
                } else {
                    boolean var10000 = false;
                }

                this.liteWindow();

                for (GuiDoc.Element e : EngineStudio.this.doc(EngineStudio.this.scene).all()) {
                    if (e.schema().props().stream().anyMatch(p -> p.kind() == GuiSchema.Kind.IMAGE)) {
                        this.litePart(e);
                    }
                }
            }

            int hgt = 8;

            for (Widget c : this.children()) {
                hgt += c.prefH + 2;
            }

            this.prefH = hgt;
            EngineStudio.this.relayout();
        }

        @Override
        public void layout() {
            int yy = this.y + 8;

            for (Widget c : this.children()) {
                c.bounds(this.x, yy, this.w, c.prefH);
                yy += c.prefH + 2;
            }

            super.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, Math.max(this.h, this.prefH), -14934490);
        }

        private void header(String text) {
            final String t = text.toUpperCase();
            this.add(new Widget() {
                {
                    this.prefH = 22;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.text(t, this.x + 12, this.y + 9, -7639809);
                    int tw = c.width(t);
                    c.fill(this.x + 18 + tw, this.y + 13, Math.max(0, this.w - 30 - tw), 1, -13749952);
                }
            });
        }

        private void note(final String text) {
            final int lines = Math.max(1, (int)Math.ceil(Minecraft.getInstance().font.width(text) / 256.0) + 1);
            this.add(new Widget() {
                {
                    this.prefH = lines * 10 + 4;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.wrapped(text, this.x + 12, this.y + 2, this.w - 24, -9933435, 1);
                }
            });
        }

        private void row(String label, Widget editor) {
            this.add(new EngineStudio.Row(label, editor, 22, false));
        }

        private TextField text(String label, Supplier<String> get, Consumer<String> set) {
            TextField f = new TextField();
            f.setText(get.get());
            f.onChange = set::accept;
            this.refreshers.add(() -> {
                if (!f.text().equals(get.get()) && UI.get() != null && !this.isFocusedField(f)) {
                    f.setText(get.get());
                }
            });
            this.row(label, f);
            return f;
        }

        private void number(String label, Supplier<String> get, Consumer<String> set) {
            TextField f = new TextField();
            f.setText(get.get());
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.' || ch == '%';
            f.onChange = set::accept;
            this.refreshers.add(() -> {
                if (!f.text().equals(get.get()) && !this.isFocusedField(f)) {
                    f.setText(get.get());
                }
            });
            this.row(label, f);
        }

        private boolean isFocusedField(Widget f) {
            return f.isFocused();
        }

        private void check(String label, Supplier<Boolean> get, Consumer<Boolean> set) {
            this.row(label, new Checkbox("", get::get, set));
        }

        private <T> void choose(String label, Supplier<List<T>> options, Supplier<T> get, Function<T, String> title, Consumer<T> set) {
            this.row(label, new Dropdown<>(options, get, title, set));
        }

        private void color(String label, final Supplier<Integer> get, final Consumer<Integer> set) {
            this.row(label, new Widget() {
                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    int v = get.get();
                    c.roundBorder(this.x, this.y, 30, this.h, 3, 0xFF000000 | v, -12762793);
                    c.text(String.format("#%08X", v), this.x + 36, this.y + 6, -6577995);
                }

                @Override
                public boolean mousePressed(double mx, double my, int button) {
                    ColorPicker.open(get.get(), this.x, this.y + this.h + 2, set::accept);
                    return true;
                }
            });
        }

        private void button(String text, Icon icon, Runnable action) {
            Button b = new Button(text, action).icon(icon).style(Button.Style.DEFAULT);
            this.add(new EngineStudio.Row(null, b, 24, true));
        }

        private void prop(GuiDoc.Element e, GuiSchema.Prop p) {
            Supplier<String> get = () -> e.get(p.key());
            Consumer<String> set = v -> {
                if (!v.equals(e.get(p.key()))) {
                    EngineStudio.this.snapshot(EngineStudio.this.scene);
                    e.set(p.key(), v);
                    EngineStudio.this.changed(EngineStudio.this.scene);
                }
            };
            String label = p.label();
            switch (p.kind()) {
                case BOOL:
                    this.check(label, () -> GuiDoc.toBool(get.get()), b -> set.accept(String.valueOf(b)));
                    break;
                case COLOR:
                    this.color(label, () -> GuiDoc.toColor(get.get()), c -> set.accept(String.format("#%08X", c)));
                    break;
                case CHOICE:
                    this.choose(label, p::choices, get, v -> v, set);
                    break;
                case INT:
                case NUMBER:
                    this.number(label, get, set);
                    break;
                case IMAGE:
                    this.choose(label, () -> {
                        List<String> out = new ArrayList<>(List.of(""));

                        for (Project.ImageRef img : EngineStudio.this.project.images()) {
                            out.add(img.name());
                        }

                        String cur = get.get();
                        if (!out.contains(cur)) {
                            out.add(cur);
                        }

                        return out;
                    }, get, v -> v.isEmpty() ? "— нет —" : v, set);
                    break;
                case ELEMENT:
                    this.choose(label, () -> {
                        List<String> out = new ArrayList<>(List.of(""));

                        for (GuiDoc.Element o : EngineStudio.this.doc(EngineStudio.this.scene).all()) {
                            if (!o.id.equals(get.get())) {
                                out.add(o.id);
                            }
                        }

                        if (!get.get().isEmpty()) {
                            out.add(1, get.get());
                        }

                        return out;
                    }, get, v -> v.isEmpty() ? "— нет —" : v, v -> {
                        set.accept(v);
                        GuiDoc.Element menu = EngineStudio.this.doc(EngineStudio.this.scene).element(v);
                        if (menu != null) {
                            menu.set("visible", "false");
                            EngineStudio.this.changed(EngineStudio.this.scene);
                        }
                    });
                    break;
                case VIDEO:
                    this.choose(label, () -> {
                        List<String> out = new ArrayList<>(List.of(""));

                        for (Project.VideoRef v : EngineStudio.this.project.videos()) {
                            out.add(v.name());
                        }

                        String cur = get.get();
                        if (!out.contains(cur)) {
                            out.add(cur);
                        }

                        return out;
                    }, get, v -> v.isEmpty() ? "— нет —" : v, set);
                    break;
                default:
                    TextField f = this.text(label, get, set);
                    if (TextValues.takes(p.key())) {
                        TextValues.attach(f, EngineStudio.this.project, EngineStudio.this.scene, p.key());
                    }
            }
        }

        private void element(GuiDoc.Element e) {
            this.header(EngineStudio.typeTitle(e.type));
            this.text("id", () -> e.id, v -> {
                String n = v.trim();
                if (!n.isEmpty() && !n.equals(e.id) && EngineStudio.this.doc(EngineStudio.this.scene).element(n) == null) {
                    EngineStudio.this.snapshot(EngineStudio.this.scene);
                    e.id = n;
                    EngineStudio.this.changed(EngineStudio.this.scene);
                }
            });
            GuiSchema.Type t = GuiSchema.type(e.type);
            if (t != null && !t.doc().isEmpty()) {
                this.note(t.doc());
            }

            this.header("Привязка и место");
            this.add(new EngineStudio.Row(null, EngineStudio.this.new AnchorPicker(e), 48, true));
            this.button("На всю ширину экрана", Icon.ARROW_RIGHT, () -> EngineStudio.this.stretch(e, "width"));
            this.button("На всю высоту экрана", Icon.CHEVRON_DOWN, () -> EngineStudio.this.stretch(e, "height"));
            this.button("На весь экран", Icon.CUBE, () -> EngineStudio.this.stretch(e, "both"));
            if (!e.get("stretch").equals("none")) {
                this.button("Не растягивать", Icon.CLOSE, () -> EngineStudio.this.stretch(e, "none"));
            }

            for (String k : List.of("x", "y", "w", "h")) {
                GuiSchema.Prop p = e.schema().prop(k);
                if (p != null) {
                    this.prop(e, p);
                }
            }

            String group = "";

            for (GuiSchema.Prop p : e.schema().props()) {
                if (!List.of("anchor", "x", "y", "w", "h").contains(p.key())) {
                    if (!p.group().equals(group)) {
                        group = p.group();
                        if (!group.equals("Положение")) {
                            this.header(group);
                        }
                    }

                    this.prop(e, p);
                }
            }

            this.header("Действия");
            this.button("Дублировать  (Ctrl+D)", Icon.COPY, EngineStudio.this::duplicateSelected);
            this.button("На передний план", Icon.CHEVRON_UP, () -> EngineStudio.this.move(1));
            this.button("На задний план", Icon.CHEVRON_DOWN, () -> EngineStudio.this.move(-1));
            this.button("Удалить  (Del)", Icon.TRASH, EngineStudio.this::deleteSelected);
        }

        private boolean flag(GuiDoc d, String prop) {
            return "true".equals(d.get(prop));
        }

        private void setFlag(GuiDoc d, String prop, boolean on) {
            EngineStudio.this.snapshot(EngineStudio.this.scene);
            if (on) {
                d.set(prop, "true");
            } else {
                d.props.remove(prop);
            }

            EngineStudio.this.changed(EngineStudio.this.scene);
        }

        private List<String> pictures() {
            List<String> out = new ArrayList<>(List.of(""));

            for (Project.ImageRef img : EngineStudio.this.project.images()) {
                if (img.path().toString().toLowerCase().endsWith(".png")) {
                    out.add(img.name());
                }
            }

            return out;
        }

        private void toastSettings() {
            EngineConfig.Quests.Toast t = EngineStudio.this.cfg.quests.toast;
            this.header("Когда показывать");
            this.check("показывать", () -> t.enabled, v -> t.enabled = v);
            this.check("новый квест", () -> t.onStart, v -> t.onStart = v);
            this.text("  надпись", () -> t.startTitle, v -> t.startTitle = v);
            this.check("задача выполнена", () -> t.onTask, v -> t.onTask = v);
            this.text("  надпись", () -> t.taskTitle, v -> t.taskTitle = v);
            this.check("новая задача", () -> t.onReveal, v -> t.onReveal = v);
            this.text("  надпись", () -> t.revealTitle, v -> t.revealTitle = v);
            this.check("квест выполнен", () -> t.onComplete, v -> t.onComplete = v);
            this.text("  надпись", () -> t.completeTitle, v -> t.completeTitle = v);
            this.check("квест провален", () -> t.onFail, v -> t.onFail = v);
            this.text("  надпись", () -> t.failTitle, v -> t.failTitle = v);
            this.number("секунд", () -> String.valueOf(t.seconds), v -> {
                try {
                    t.seconds = Math.max(0.5F, Math.min(30.0F, Float.parseFloat(v)));
                } catch (NumberFormatException var3) {
                }
            });
            this.check("звук", () -> t.sound, v -> t.sound = v);
            this.button("Показать в игре", Icon.PLAY, () -> QuestClient.showToast("task", "Задача выполнена", "Пример задачи"));
        }

        private void coinSettings() {
            this.check("монеты Spraute", () -> EngineStudio.this.cfg.currency, v -> EngineStudio.this.cfg.currency = v);
            this.row(
                "предмет монеты",
                new Button(
                        () -> EngineStudio.this.cfg.coinItem.isEmpty() ? "выбрать…" : EngineStudio.this.cfg.coinItem.replace("minecraft:", ""),
                        () -> AssetPicker.open(ParamType.ITEM, EngineStudio.this.cfg.coinItem, v -> {
                            EngineStudio.this.cfg.coinItem = v.trim();
                            EngineStudio.this.cfg.currency = true;
                            this.rebuild();
                        })
                    )
                    .icon(Icon.CUBE)
                    .style(Button.Style.GHOST)
            );
            if (EngineStudio.this.cfg.coinItem.isEmpty()) {
                this.note("Монеты — свой кошелёк и сферы на земле. Если выбрать предмет, монетами станет он, прямо в инвентаре");
            } else {
                this.button("Вернуть кошелёк и сферы", Icon.CLOSE, () -> {
                    EngineStudio.this.cfg.coinItem = "";
                    this.rebuild();
                });
            }
        }

        private void windowScale(GuiDoc d) {
            this.choose(
                "масштаб",
                () -> List.of("screen", "fit", "height"),
                () -> d.get("layout"),
                v -> v.equals("screen") ? "к краям экрана" : (v.equals("height") ? "по высоте, на всю ширину" : "вписать целиком"),
                v -> {
                    EngineStudio.this.snapshot(EngineStudio.this.scene);
                    d.set("layout", v);
                    EngineStudio.this.changed(EngineStudio.this.scene);
                }
            );
        }

        private void liteWindow() {
            if (EngineStudio.isMenu(EngineStudio.this.scene)) {
                this.header("Какое меню");
                this.choose("экран", MenuRuntime::roles, () -> EngineStudio.this.scene, MenuRuntime::title, EngineStudio.this::openScene);
                GuiDoc menu = EngineStudio.this.doc(EngineStudio.this.scene);
                if (EngineStudio.this.scene.substring("menu:".length()).equals("title")) {
                    this.header("Логотип");
                    this.choose("свой логотип", this::pictures, () -> {
                        GuiDoc.Element own = menu.element("own_logo");
                        return own != null ? own.get("image") : "";
                    }, v -> v.isEmpty() ? "нет" : v, v -> this.ownLogo(menu, v));
                }
            }

            GuiDoc d = EngineStudio.this.doc(EngineStudio.this.scene);
            if (EngineStudio.this.scene.equals("hud")) {
                this.header("Монеты");
                this.coinSettings();
            }

            if (EngineStudio.this.scene.equals("toast")) {
                this.note("Картинки значков уведомлений доступны ниже");
                this.toastSettings();
            }

            if (EngineStudio.this.scene.equals("quest")) {
                this.note("Картинки списка квестов и вкладок доступны ниже");
                this.header("Вид");
                this.color("затемнение", () -> GuiDoc.toColor(d.get("background")), v -> {
                    EngineStudio.this.snapshot(EngineStudio.this.scene);
                    d.set("background", String.format("#%08X", v));
                    EngineStudio.this.changed(EngineStudio.this.scene);
                });
            }
        }

        private void litePart(GuiDoc.Element e) {
            this.header(e.id + " · " + EngineStudio.typeTitle(e.type));
            boolean any = false;

            for (GuiSchema.Prop p : e.schema().props()) {
                if (p.kind() == GuiSchema.Kind.IMAGE) {
                    any = true;
                    this.choose(p.label(), this::pictures, () -> e.get(p.key()), v -> v.isEmpty() ? "стандартная" : v, v -> {
                        EngineStudio.this.snapshot(EngineStudio.this.scene);
                        e.set(p.key(), v);
                        EngineStudio.this.changed(EngineStudio.this.scene);
                    });
                    if (!p.doc().isEmpty()) {
                        this.note(p.doc());
                    }
                }
            }

            if (!any) {
                this.note("В этой части нет картинки.");
            } else if (this.pictures().size() < 2) {
                this.note("Своих картинок пока нет: добавьте их в проект, в раздел «Картинки».");
            }
        }

        private void menuLook(GuiDoc d) {
            String key = EngineStudio.this.scene.substring("menu:".length());
            if (key.equals("title")) {
                this.titleLook(d);
            }

            if (MenuLook.hasBack(key)) {
                this.windowLook(d);
            }

            this.header("Затемнение");
            this.check("убрать затемнение за окном", () -> this.flag(d, "hideDim"), v -> this.setFlag(d, "hideDim", v));
        }

        private void titleLook(GuiDoc d) {
            this.header("Логотип и надписи");
            this.check("скрыть логотип Minecraft", () -> this.flag(d, "hideLogo"), v -> this.setFlag(d, "hideLogo", v));
            this.choose("свой логотип", this::pictures, () -> {
                GuiDoc.Element own = d.element("own_logo");
                return own != null ? own.get("image") : "";
            }, v -> v.isEmpty() ? "нет" : v, v -> this.ownLogo(d, v));
            this.note("Свой логотип — обычная картинка в сцене: двигайте и меняйте её размер мышью");
            this.check("скрыть жёлтую надпись", () -> this.flag(d, "hideSplash"), v -> this.setFlag(d, "hideSplash", v));
            this.check("скрыть текст слева снизу", () -> this.flag(d, "hideVersion"), v -> this.setFlag(d, "hideVersion", v));
            GuiDoc.Element copyright = MenuLook.copyright(d);
            if (copyright != null) {
                this.check("скрыть текст справа снизу", () -> !copyright.b("visible"), v -> {
                    EngineStudio.this.snapshot(EngineStudio.this.scene);
                    copyright.set("visible", String.valueOf(!v));
                    EngineStudio.this.changed(EngineStudio.this.scene);
                });
            }
        }

        private void ownLogo(GuiDoc d, String image) {
            EngineStudio.this.snapshot(EngineStudio.this.scene);
            GuiDoc.Element own = d.element("own_logo");
            if (image.isEmpty()) {
                if (own != null) {
                    d.elements.remove(own);
                }
            } else {
                if (own == null) {
                    own = new GuiDoc.Element();
                    own.type = "image";
                    own.id = "own_logo";
                    own.set("anchor", "top");
                    own.set("x", "0");
                    own.set("y", "30");
                    own.set("w", "256");
                    own.set("h", "44");
                    d.elements.add(0, own);
                }

                own.set("image", image);
            }

            if (image.isEmpty()) {
                d.props.remove("hideLogo");
            } else {
                d.set("hideLogo", "true");
            }

            EngineStudio.this.changed(EngineStudio.this.scene);
            this.rebuild();
        }

        private void windowLook(GuiDoc d) {
            this.header("Окно");
            this.choose("своя картинка фона", this::pictures, () -> d.get("backImage"), v -> v.isEmpty() ? "нет" : v, v -> {
                EngineStudio.this.snapshot(EngineStudio.this.scene);
                if (v.isEmpty()) {
                    d.props.remove("backImage");
                } else {
                    d.set("backImage", v);
                }

                EngineStudio.this.changed(EngineStudio.this.scene);
            });
            this.note("Растягивается на окно игры. Ячейки — часть этого фона: нарисуйте их сами или добавьте объект «Ячейки окна»");
            this.check("скрыть фон игры", () -> this.flag(d, "hideBack"), v -> this.setFlag(d, "hideBack", v));
            this.check("скрыть подписи окна", () -> this.flag(d, "hideLabels"), v -> this.setFlag(d, "hideLabels", v));
            this.note("Всё, что добавлено в сцену, рисуется над фоном и под предметами");
        }

        private void window() {
            if (EngineStudio.this.scene.startsWith("skills")) {
                this.header("Что настраивать");
                this.choose(
                    "страница", () -> EngineStudio.SKILLS_PAGES, () -> EngineStudio.skillsPage, EngineStudio::skillsPageTitle, EngineStudio.this::openScene
                );
            }

            if (EngineStudio.isMenu(EngineStudio.this.scene)) {
                this.header("Какое меню");
                this.choose("экран", MenuRuntime::roles, () -> EngineStudio.this.scene, MenuRuntime::title, EngineStudio.this::openScene);
                this.note("Здесь только экраны, где есть что менять. Остальные появятся, как только вы откроете их в игре и движок увидит в них кнопки");
                this.button("Считать кнопки заново", Icon.REFRESH, EngineStudio.this::standardLook);
                this.menuLook(EngineStudio.this.doc(EngineStudio.this.scene));
            }

            this.header(
                EngineStudio.isMenu(EngineStudio.this.scene) ? MenuRuntime.title(EngineStudio.this.scene) : EngineConfig.windowTitle(EngineStudio.this.scene)
            );
            this.note(EngineWindows.help(EngineStudio.this.scene));
            GuiDoc d = EngineStudio.this.doc(EngineStudio.this.scene);
            this.header("Окно");
            this.windowScale(d);
            this.number("ширина макета", () -> d.get("width"), v -> {
                d.set("width", v);
                EngineStudio.this.changed(EngineStudio.this.scene);
            });
            this.number("высота макета", () -> d.get("height"), v -> {
                d.set("height", v);
                EngineStudio.this.changed(EngineStudio.this.scene);
            });
            String var2 = EngineStudio.this.scene;
            switch (var2) {
                case "choice":
                    this.header("Поведение");
                    this.check("камера к говорящему", () -> EngineStudio.this.cfg.choice.camera, v -> EngineStudio.this.cfg.choice.camera = v);
                    break;
                case "toast":
                    this.toastSettings();
                    break;
                case "dialog":
                    if (PlateLayout.onlyPlate(d)) {
                        this.header("Где стоит плашка");
                        this.choose(
                            "по высоте",
                            () -> List.of("bottom", "center", "top"),
                            () -> EngineStudio.this.cfg.chatLog.speechPlace == null ? "bottom" : EngineStudio.this.cfg.chatLog.speechPlace,
                            v -> v.equals("top") ? "сверху" : (v.equals("center") ? "посередине" : "снизу"),
                            v -> {
                                EngineStudio.this.cfg.chatLog.speechPlace = v;
                                EngineStudio.this.plateSettings();
                            }
                        );
                        this.choose(
                            "по ширине",
                            () -> List.of("left", "center", "right"),
                            () -> EngineStudio.this.cfg.chatLog.speechAlign == null ? "center" : EngineStudio.this.cfg.chatLog.speechAlign,
                            v -> v.equals("left") ? "слева" : (v.equals("right") ? "справа" : "по центру"),
                            v -> {
                                EngineStudio.this.cfg.chatLog.speechAlign = v;
                                EngineStudio.this.plateSettings();
                            }
                        );
                        this.number("ширина, % экрана", () -> String.valueOf(EngineStudio.this.cfg.chatLog.speechWidth), v -> {
                            try {
                                EngineStudio.this.cfg.chatLog.speechWidth = Math.max(10, Math.min(100, Integer.parseInt(v.trim())));
                                EngineStudio.this.plateSettings();
                            } catch (NumberFormatException var3) {
                            }
                        });
                        this.check("показывать портрет", () -> EngineStudio.this.cfg.chatLog.speechIconShown, v -> {
                            EngineStudio.this.cfg.chatLog.speechIconShown = v;
                            EngineStudio.this.plateSettings();
                        });
                        this.note(
                            "Плашку, портрет и текст можно двигать мышью по отдельности, у портрета можно менять размер. Фон сам обтягивает содержимое. В игре всё так же"
                        );
                    } else {
                        this.header("Своё окно");
                        this.note("В сцене есть другие объекты, поэтому реплики рисует это окно. Кнопка «Стандартный вид» вернёт обычную плашку");
                    }

                    this.header("Поведение");
                    this.note("Плашка сама подстраивается под длину реплики");
                    this.number("секунд на экране", () -> String.valueOf(EngineStudio.this.cfg.chatLog.speechSeconds), v -> {
                        try {
                            EngineStudio.this.cfg.chatLog.speechSeconds = Math.max(0.5F, Math.min(120.0F, Float.parseFloat(v)));
                        } catch (NumberFormatException var3) {
                        }
                    });
                    this.text("звук реплики", () -> EngineStudio.this.cfg.chatLog.speechSound, v -> EngineStudio.this.cfg.chatLog.speechSound = v.trim());
                    break;
                case "zone":
                    this.header("Поведение");
                    this.number("секунд", () -> String.valueOf(EngineStudio.this.cfg.zoneTitle.seconds), v -> {
                        try {
                            EngineStudio.this.cfg.zoneTitle.seconds = Math.max(0.5F, Math.min(30.0F, Float.parseFloat(v)));
                        } catch (NumberFormatException var3) {
                        }
                    });
                    this.text("звук", () -> EngineStudio.this.cfg.zoneTitle.sound, v -> EngineStudio.this.cfg.zoneTitle.sound = v.trim());
                    break;
                case "quest":
                case "shop":
                case "inventory":
                    if (EngineStudio.this.scene.equals("inventory")) {
                        this.header("Система");
                        this.check("свой инвентарь включён", () -> EngineStudio.this.cfg.inventory, v -> EngineStudio.this.cfg.inventory = v);
                        this.note("Выключено — в игре обычное окно Minecraft, а эта раскладка не используется");
                    }

                    this.header("Вид");
                    this.color("затемнение", () -> GuiDoc.toColor(d.get("background")), v -> {
                        EngineStudio.this.snapshot(EngineStudio.this.scene);
                        d.set("background", String.format("#%08X", v));
                        EngineStudio.this.changed(EngineStudio.this.scene);
                    });
                    if (EngineStudio.this.scene.equals("quest")) {
                        this.note("Цели на экране — часть «HUD · Цели квеста» в сцене «Игра (HUD)». Плашки событий — сцена «Уведомление квеста»");
                    }
                    break;
                case "hud":
                    this.header("Системы");
                    this.check("здоровье Spraute", () -> EngineStudio.this.cfg.health, v -> EngineStudio.this.cfg.health = v);
                    this.coinSettings();
                    this.note("«HUD · Здоровье» рисует круглую шкалу Spraute, если она включена, иначе сердца");
            }

            this.header("Раскладка");
            this.button(
                "Добавить часть…",
                Icon.PLUS,
                () -> EngineStudio.this.addMenu(EngineStudio.this.ox + EngineStudio.this.vw - 290 + 12, EngineStudio.this.oy + 120)
            );
            this.header("Фон с глубиной (параллакс)");
            this.note("Несколько картинок одна за другой, каждая уходит за мышью на свой сдвиг: дальняя мало, ближняя много");
            this.button("Добавить слой фона", Icon.PLUS, EngineStudio.this::addParallaxLayer);
            this.button("Стандартный вид", Icon.REFRESH, EngineStudio.this::standardLook);
        }
    }

    private final class Left extends Widget implements UI.CursorProvider {
        private final List<int[]> sceneRows = new ArrayList<>();
        private final List<String> sceneKeys = new ArrayList<>();
        private final List<int[]> partRows = new ArrayList<>();
        private final List<GuiDoc.Element> partEls = new ArrayList<>();
        private int addY = -1;

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14605523);
            c.fill(this.x + this.w - 1, this.y, 1, this.h, -13749952);
            this.sceneRows.clear();
            this.sceneKeys.clear();
            this.partRows.clear();
            this.partEls.clear();
            this.addY = -1;
            int yy = this.y + 10;
            String group = "";

            for (EngineStudio.Scene s : EngineStudio.scenes()) {
                if (!s.group().equals(group)) {
                    group = s.group();
                    if (yy > this.y + 10) {
                        yy += 4;
                    }

                    c.text(group, this.x + 14, yy, -9933435);
                    yy += 13;
                }

                boolean sel = s.key().equals(EngineStudio.this.scene) || s.key().equals("menus") && EngineStudio.isMenu(EngineStudio.this.scene);
                boolean hov = mx >= this.x && mx < this.x + this.w && my >= yy && my < yy + 22;
                if (sel) {
                    c.round(this.x + 6, yy, this.w - 12, 22, 4, 1430088664);
                    c.fill(this.x + 6, yy + 5, 2, 12, -7639809);
                } else if (hov) {
                    c.round(this.x + 6, yy, this.w - 12, 22, 4, 352321535);
                }

                c.icon(s.icon(), this.x + 16, yy + 7, sel ? -7639809 : -6577995);
                c.text(s.title(), this.x + 32, yy + 7, sel ? -1841682 : -6577995);
                this.sceneRows.add(new int[]{yy, 22});
                this.sceneKeys.add(s.key());
                yy += 24;
            }

            if (EngineStudio.isWindow(EngineStudio.this.scene)) {
            }
        }

        private int parts(Canvas c, List<GuiDoc.Element> list, int depth, int yy, int mx, int my) {
            for (int i = list.size() - 1; i >= 0; i--) {
                GuiDoc.Element e = list.get(i);
                boolean sel = e == EngineStudio.this.selected;
                boolean hov = mx >= this.x && mx < this.x + this.w && my >= yy && my < yy + 18;
                if (sel) {
                    c.round(this.x + 6, yy, this.w - 12, 18, 3, 1430088664);
                } else if (hov) {
                    c.round(this.x + 6, yy, this.w - 12, 18, 3, 352321535);
                }

                int ix = this.x + 14 + depth * 12;
                boolean vis = e.b("visible");
                c.text(e.id, ix, yy + 5, vis ? (sel ? -1841682 : -6577995) : -9933435);
                int tx = ix + c.width(e.id) + 6;
                c.textEllipsized(EngineStudio.typeTitle(e.type), tx, yy + 5, Math.max(10, this.x + this.w - 30 - tx), -9933435);
                c.icon(vis ? Icon.CHECK : Icon.CLOSE, this.x + this.w - 22, yy + 5, vis ? -6577995 : -9933435);
                this.partRows.add(new int[]{yy, 18});
                this.partEls.add(e);
                yy += 19;
                if (!e.children.isEmpty()) {
                    yy = this.parts(c, e.children, depth + 1, yy, mx, my);
                }
            }

            return yy;
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            for (int i = 0; i < this.sceneRows.size(); i++) {
                int[] r = this.sceneRows.get(i);
                if (my >= r[0] && my < r[0] + r[1]) {
                    EngineStudio.this.openScene(this.sceneKeys.get(i));
                    return true;
                }
            }

            if (EngineStudio.isWindow(EngineStudio.this.scene)
                && this.addY >= 0
                && mx >= this.x + this.w - 88
                && mx < this.x + this.w - 10
                && my >= this.addY
                && my < this.addY + 16) {
                EngineStudio.this.addMenu((int)mx, (int)my + 12);
                return true;
            } else {
                for (int ix = 0; ix < this.partRows.size(); ix++) {
                    int[] r = this.partRows.get(ix);
                    if (my >= r[0] && my < r[0] + r[1]) {
                        GuiDoc.Element e = this.partEls.get(ix);
                        if (mx >= this.x + this.w - 28) {
                        }

                        EngineStudio.this.select(e);
                        return true;
                    }
                }

                return true;
            }
        }

        @Override
        public int cursor(int mx, int my) {
            return 221188;
        }
    }

    private static final class Row extends Widget {
        final String label;
        final Widget editor;
        final boolean wide;

        Row(String label, Widget editor, int height, boolean wide) {
            this.label = label;
            this.editor = editor;
            this.wide = wide;
            this.prefH = height;
            if (editor != null) {
                this.add(editor);
            }
        }

        @Override
        public void layout() {
            if (this.editor != null) {
                if (this.wide) {
                    this.editor.bounds(this.x + 12, this.y + 1, this.w - 24, this.h - 2);
                } else {
                    this.editor.bounds(this.x + 108, this.y + 1, this.w - 120, 20);
                }
            }

            super.layout();
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            if (this.label != null && !this.wide) {
                c.textEllipsized(this.label, this.x + 12, this.y + 7, 92, -6577995);
            }
        }
    }

    private record Scene(String key, String title, String group, Icon icon, String doc) {
    }

    private final class Viewport extends Widget implements UI.CursorProvider {
        private float gx;
        private float gy;
        private float k = 1.0F;
        private int dragMode;
        private double pressX;
        private double pressY;
        private int[] startRect;
        private int startX;
        private int startY;
        private String before;
        private boolean moved;
        private final List<float[]> guides = new ArrayList<>();

        Viewport() {
            this.focusable = true;
        }

        private void place() {
            int pad = 24;
            this.k = Math.max(
                0.2F, Math.min((float)(this.w - pad * 2) / EngineStudio.this.device.w(), (float)(this.h - pad * 2 - 18) / EngineStudio.this.device.h())
            );
            this.gx = this.x + (this.w - EngineStudio.this.device.w() * this.k) / 2.0F;
            this.gy = this.y + 18 + (this.h - 18 - EngineStudio.this.device.h() * this.k) / 2.0F;
        }

        private float[] onScreen(GuiView v, int[] r) {
            float sx = this.gx + (v.areaX() + r[0] * v.areaScale()) * this.k;
            float sy = this.gy + (v.areaY() + r[1] * v.areaScale()) * this.k;
            return new float[]{sx, sy, r[2] * v.areaScale() * this.k, r[3] * v.areaScale() * this.k};
        }

        private void drawView(Canvas c, GuiView v, float opacity) {
            v.opacity = opacity;
            v.bounds(0, 0, EngineStudio.this.device.w(), EngineStudio.this.device.h());
            v.layout();
            c.pushTransform(this.gx, this.gy, this.k);

            try {
                v.render(c, -10000, -10000, 0.05F);
            } catch (RuntimeException var5) {
            }

            c.popTransform();
        }

        private boolean owns(GuiDoc.Element e) {
            return e != null && EngineStudio.this.doc(EngineStudio.this.scene).all().contains(e);
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            if (EngineStudio.isWindow(EngineStudio.this.scene)) {
                this.place();
                c.fill(this.x, this.y, this.w, this.h, -15724266);
                int gw = Math.round(EngineStudio.this.device.w() * this.k);
                int gh = Math.round(EngineStudio.this.device.h() * this.k);
                int ix = Math.round(this.gx);
                int iy = Math.round(this.gy);
                c.text(
                    EngineStudio.this.device.title() + "  ·  экран интерфейса " + EngineStudio.this.device.w() + "×" + EngineStudio.this.device.h(),
                    ix,
                    iy - 14,
                    -9933435
                );
                c.shadow(ix, iy, gw, gh, 4, 10);
                c.pushClip(ix, iy, gw, gh);
                GameShot.draw(c, ix, iy, gw, gh);
                boolean screen = EngineStudio.this.scene.equals("quest")
                    || EngineStudio.this.scene.equals("shop")
                    || EngineStudio.this.scene.equals("inventory")
                    || EngineStudio.isMenu(EngineStudio.this.scene);
                if (!EngineStudio.this.scene.equals("chatlog") && !screen) {
                    this.drawView(c, EngineStudio.this.view("hud"), EngineStudio.this.scene.equals("hud") ? 1.0F : 0.5F);
                }

                if (screen) {
                    c.fill(ix, iy, gw, gh, GuiDoc.toColor(EngineStudio.this.doc(EngineStudio.this.scene).get("background")));
                    if (EngineStudio.this.scene.equals("quest")) {
                        JournalParts.vars(EngineStudio.this.view(EngineStudio.this.scene));
                    } else if (EngineStudio.this.scene.equals("shop")) {
                        ShopParts.vars(EngineStudio.this.view(EngineStudio.this.scene));
                    }
                }

                if (EngineStudio.isMenu(EngineStudio.this.scene)) {
                    c.flush();
                    c.pushTransform(this.gx, this.gy, this.k);
                    MenuRuntime.preview(EngineStudio.this.scene, EngineStudio.this.doc(EngineStudio.this.scene), EngineStudio.this.project.name());
                    MenuRuntime.drawReal(c.g, EngineStudio.this.scene, EngineStudio.this.device.w(), EngineStudio.this.device.h());
                    c.popTransform();
                    c.flush();
                }

                if (EngineStudio.this.scene.equals("choice")) {
                    c.fill(ix, iy, gw, gh, 855638016);
                }

                if (!EngineStudio.this.scene.equals("hud")) {
                    this.drawView(c, EngineStudio.this.view(EngineStudio.this.scene), 1.0F);
                }

                c.flush();
                c.popClip();
                c.outline(ix - 1, iy - 1, gw + 2, gh + 2, -12762793);
                GuiView v = EngineStudio.this.view(EngineStudio.this.scene);
                GuiDoc.Element hov = this.dragMode == 0 ? this.at(mx, my) : null;
                if (hov != null && hov != EngineStudio.this.selected) {
                    float[] r = this.onScreen(v, v.rect(hov));
                    c.outline(Math.round(r[0]), Math.round(r[1]), Math.round(r[2]), Math.round(r[3]), -1996488705);
                }

                for (float[] gl : this.guides) {
                    c.line(gl[0], gl[1], gl[2], gl[3], 1.0F, -45096);
                }

                if (this.owns(EngineStudio.this.selected)) {
                    float[] r = this.onScreen(v, v.rect(EngineStudio.this.selected));
                    c.outline(Math.round(r[0]), Math.round(r[1]), Math.round(r[2]), Math.round(r[3]), -7639809);
                    if (EngineStudio.this.hasHandles(EngineStudio.this.selected)) {
                        for (float[] hd : this.handles(r)) {
                            c.fill(Math.round(hd[0]) - 3, Math.round(hd[1]) - 3, 7, 7, -16777216);
                            c.fill(Math.round(hd[0]) - 2, Math.round(hd[1]) - 2, 5, 5, -7639809);
                        }
                    }

                    float[] ar = v.areaOf(EngineStudio.this.selected);
                    String a = EngineStudio.this.selected.get("anchor");
                    float fx = a.endsWith("left") ? 0.0F : (a.endsWith("right") ? 1.0F : 0.5F);
                    float fy = a.startsWith("top") ? 0.0F : (a.startsWith("bottom") ? 1.0F : 0.5F);
                    float ax = this.gx + (v.areaX() + (ar[0] + fx * ar[2]) * v.areaScale()) * this.k;
                    float ay = this.gy + (v.areaY() + (ar[1] + fy * ar[3]) * v.areaScale()) * this.k;
                    float px = r[0] + fx * r[2];
                    float py = r[1] + fy * r[3];
                    c.line(ax, ay, px, py, 1.0F, -865156097);
                    c.round(Math.round(ax) - 4, Math.round(ay) - 4, 8, 8, 4, -9518081);
                    int[] dr = v.rect(EngineStudio.this.selected);
                    String lab = EngineStudio.this.selected.id + "  " + dr[2] + "×" + dr[3];
                    int lw = c.width(lab) + 8;
                    int ly = Math.round(r[1]) - 13 < iy ? Math.round(r[1] + r[3]) + 2 : Math.round(r[1]) - 13;
                    c.round(Math.round(r[0]), ly, lw, 12, 2, -7639809);
                    c.text(lab, Math.round(r[0]) + 4, ly + 2, -1);
                }
            }
        }

        private float[][] handles(float[] r) {
            float x0 = r[0];
            float y0 = r[1];
            float x1 = r[0] + r[2];
            float y1 = r[1] + r[3];
            float xm = (x0 + x1) / 2.0F;
            float ym = (y0 + y1) / 2.0F;
            return new float[][]{{x0, y0}, {xm, y0}, {x1, y0}, {x1, ym}, {x1, y1}, {xm, y1}, {x0, y1}, {x0, ym}};
        }

        private GuiDoc.Element at(double mx, double my) {
            GuiView v = EngineStudio.this.view(EngineStudio.this.scene);
            List<GuiDoc.Element> all = EngineStudio.this.doc(EngineStudio.this.scene).all();

            for (int i = all.size() - 1; i >= 0; i--) {
                GuiDoc.Element e = all.get(i);
                if (!e.locked && e.b("visible")) {
                    float[] r = this.onScreen(v, v.rect(e));
                    if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                        return e;
                    }
                }
            }

            return null;
        }

        private int handleAt(double mx, double my) {
            if (EngineStudio.this.selected != null && !EngineStudio.this.hasHandles(EngineStudio.this.selected)) {
                return -1;
            } else if (!this.owns(EngineStudio.this.selected)) {
                return -1;
            } else {
                GuiView v = EngineStudio.this.view(EngineStudio.this.scene);
                float[][] hs = this.handles(this.onScreen(v, v.rect(EngineStudio.this.selected)));

                for (int i = 0; i < hs.length; i++) {
                    if (Math.abs(mx - hs[i][0]) <= 5.0 && Math.abs(my - hs[i][1]) <= 5.0) {
                        return i;
                    }
                }

                return -1;
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            this.requestFocus();
            this.guides.clear();
            GuiDoc.Element e = this.at(mx, my);
            if (e != EngineStudio.this.selected) {
                EngineStudio.this.select(e);
            }

            this.dragMode = 0;
            return true;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
            if (this.dragMode != 0 && EngineStudio.this.selected != null && this.startRect != null) {
                GuiView v = EngineStudio.this.view(EngineStudio.this.scene);
                float unit = Math.max(0.01F, v.areaScale() * this.k);
                if (!this.moved && Math.abs(mx - this.pressX) + Math.abs(my - this.pressY) < 2.0) {
                    return true;
                } else {
                    this.moved = true;
                    int dx = (int)Math.round((mx - this.pressX) / unit);
                    int dy = (int)Math.round((my - this.pressY) / unit);
                    int rx = this.startRect[0];
                    int ry = this.startRect[1];
                    int rw = this.startRect[2];
                    int rh = this.startRect[3];
                    this.guides.clear();
                    if (this.dragMode == 1) {
                        rx += dx;
                        ry += dy;
                        if (!Screen.hasAltDown() && !EngineStudio.this.inPlate(EngineStudio.this.selected)) {
                            int[] s = this.snapMove(v, rx, ry, rw, rh);
                            rx = s[0];
                            ry = s[1];
                        }
                    } else {
                        int hi = this.dragMode - 2;
                        boolean left = hi == 0 || hi == 6 || hi == 7;
                        boolean right = hi == 2 || hi == 3 || hi == 4;
                        boolean top = hi == 0 || hi == 1 || hi == 2;
                        boolean bottom = hi == 4 || hi == 5 || hi == 6;
                        if (left) {
                            rx = Math.min(this.startRect[0] + this.startRect[2] - 2, this.startRect[0] + dx);
                            rw = this.startRect[0] + this.startRect[2] - rx;
                        }

                        if (right) {
                            rw = Math.max(2, this.startRect[2] + dx);
                        }

                        if (top) {
                            ry = Math.min(this.startRect[1] + this.startRect[3] - 2, this.startRect[1] + dy);
                            rh = this.startRect[1] + this.startRect[3] - ry;
                        }

                        if (bottom) {
                            rh = Math.max(2, this.startRect[3] + dy);
                        }

                        String st = EngineStudio.this.selected.get("stretch");
                        boolean sw = st.equals("width") || st.equals("both");
                        boolean sh = st.equals("height") || st.equals("both");
                        if (sw && (left || right)) {
                            sw = false;
                        }

                        if (sh && (top || bottom)) {
                            sh = false;
                        }

                        EngineStudio.this.selected.set("stretch", sw && sh ? "both" : (sw ? "width" : (sh ? "height" : "none")));
                    }

                    if (EngineStudio.this.inPlate(EngineStudio.this.selected)) {
                        EngineStudio.this.selected.set("x", String.valueOf(this.startX + rx - this.startRect[0]));
                        EngineStudio.this.selected.set("y", String.valueOf(this.startY + ry - this.startRect[1]));
                        if (this.dragMode != 1) {
                            int side = Math.max(4, Math.min(128, Math.max(rw, rh)));
                            EngineStudio.this.selected.set("w", String.valueOf(side));
                            EngineStudio.this.selected.set("h", String.valueOf(side));
                        }

                        v.layoutTree();
                        EngineStudio.this.cfg.layoutJson.put(EngineStudio.this.scene, EngineStudio.this.doc(EngineStudio.this.scene).toJson());
                        EngineStudio.this.inspector.refreshValues();
                        return true;
                    } else {
                        EngineStudio.this.selected.set("w", String.valueOf(rw));
                        EngineStudio.this.selected.set("h", String.valueOf(rh));
                        v.layoutTree();
                        int[] off = v.offsetFor(EngineStudio.this.selected, rx, ry, rw, rh);
                        EngineStudio.this.selected.set("x", String.valueOf(off[0]));
                        EngineStudio.this.selected.set("y", String.valueOf(off[1]));
                        v.layoutTree();
                        EngineStudio.this.cfg.layoutJson.put(EngineStudio.this.scene, EngineStudio.this.doc(EngineStudio.this.scene).toJson());
                        EngineStudio.this.inspector.refreshValues();
                        return true;
                    }
                }
            } else {
                return true;
            }
        }

        private int[] snapMove(GuiView v, int rx, int ry, int rw, int rh) {
            float[] area = v.areaOf(EngineStudio.this.selected);
            List<Float> xs = new ArrayList<>(List.of(area[0], area[0] + area[2] / 2.0F, area[0] + area[2]));
            List<Float> ys = new ArrayList<>(List.of(area[1], area[1] + area[3] / 2.0F, area[1] + area[3]));

            for (GuiDoc.Element o : EngineStudio.this.doc(EngineStudio.this.scene).all()) {
                if (o != EngineStudio.this.selected && o.b("visible") && !EngineStudio.this.selected.children.contains(o)) {
                    int[] r = v.rect(o);
                    xs.add((float)r[0]);
                    xs.add(r[0] + r[2] / 2.0F);
                    xs.add((float)r[0] + r[2]);
                    ys.add((float)r[1]);
                    ys.add(r[1] + r[3] / 2.0F);
                    ys.add((float)r[1] + r[3]);
                }
            }

            float th = 5.0F / Math.max(0.01F, v.areaScale() * this.k);
            float bestX = Float.MAX_VALUE;
            float lineX = 0.0F;

            for (float m : new float[]{rx, rx + rw / 2.0F, rx + rw}) {
                for (float t : xs) {
                    if (Math.abs(t - m) < Math.abs(bestX)) {
                        bestX = t - m;
                        lineX = t;
                    }
                }
            }

            float bestY = Float.MAX_VALUE;
            float lineY = 0.0F;

            for (float m : new float[]{ry, ry + rh / 2.0F, ry + rh}) {
                for (float tx : ys) {
                    if (Math.abs(tx - m) < Math.abs(bestY)) {
                        bestY = tx - m;
                        lineY = tx;
                    }
                }
            }

            if (Math.abs(bestX) <= th) {
                rx += Math.round(bestX);
                float sx = this.gx + (v.areaX() + lineX * v.areaScale()) * this.k;
                this.guides.add(new float[]{sx, this.gy, sx, this.gy + EngineStudio.this.device.h() * this.k});
            }

            if (Math.abs(bestY) <= th) {
                ry += Math.round(bestY);
                float sy = this.gy + (v.areaY() + lineY * v.areaScale()) * this.k;
                this.guides.add(new float[]{this.gx, sy, this.gx + EngineStudio.this.device.w() * this.k, sy});
            }

            return new int[]{rx, ry};
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (this.dragMode >= 2
                && this.moved
                && EngineStudio.this.selected != null
                && EngineStudio.this.doc(EngineStudio.this.scene).elements.contains(EngineStudio.this.selected)) {
                GuiView v = EngineStudio.this.view(EngineStudio.this.scene);
                int[] rr = v.rect(EngineStudio.this.selected);
                float[] ar = v.areaOf(EngineStudio.this.selected);
                String st = EngineStudio.this.selected.get("stretch");
                boolean sw = st.equals("width") || st.equals("both");
                boolean sh = st.equals("height") || st.equals("both");
                if (rr[0] <= ar[0] + 1.0F && rr[0] + rr[2] >= ar[0] + ar[2] - 1.0F) {
                    sw = true;
                }

                if (rr[1] <= ar[1] + 1.0F && rr[1] + rr[3] >= ar[1] + ar[3] - 1.0F) {
                    sh = true;
                }

                String ns = sw && sh ? "both" : (sw ? "width" : (sh ? "height" : "none"));
                if (!ns.equals(st)) {
                    EngineStudio.this.selected.set("stretch", ns);
                    v.layoutTree();
                    EngineStudio.toastInfo(sw && sh ? "Растянуто на весь экран" : (sw ? "Растянуто на всю ширину экрана" : "Растянуто на всю высоту экрана"));
                }
            }

            if (this.dragMode != 0 && this.moved && this.before != null) {
                EngineStudio.this.undo.computeIfAbsent(EngineStudio.this.scene, kk -> new ArrayDeque<>()).push(this.before);
                EngineStudio.this.redo.computeIfAbsent(EngineStudio.this.scene, kk -> new ArrayDeque<>()).clear();
                EngineStudio.this.changed(EngineStudio.this.scene);
            }

            this.dragMode = 0;
            this.before = null;
            this.guides.clear();
            return true;
        }

        @Override
        public int cursor(int mx, int my) {
            int hi = this.handleAt(mx, my);
            if (hi < 0) {
                return this.at(mx, my) != null ? 221187 : 221185;
            } else {
                return hi != 1 && hi != 5 ? 221189 : 221190;
            }
        }
    }
}
