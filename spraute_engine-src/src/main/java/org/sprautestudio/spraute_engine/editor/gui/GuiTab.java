package org.sprautestudio.spraute_engine.editor.gui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.GuiSchema;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.gui.client.SprauteGuiScreen;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.ColorPicker;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class GuiTab extends EditorTab {
    private static final int LEFT_W = 168;
    private static final int RIGHT_W = 236;
    private static final int TOOLBAR = 28;
    private static final Gson GSON = new Gson();
    private GuiDoc doc;
    private final LinkedHashSet<String> selection = new LinkedHashSet<>();
    private boolean showHover;
    private boolean dirty;
    private final List<String> undo = new ArrayList<>();
    private final List<String> redo = new ArrayList<>();
    private String editingKey;
    private static String clipboard;
    private final Box toolbar = Box.row().gap(3).pad(6, 4).background(-14934490);
    private final GuiTab.EditCanvas canvas;
    private final ListView<GuiSchema.Type> palette;
    private final GuiTab.LayerTree layers = new GuiTab.LayerTree();
    private final GuiTab.Inspector inspector = new GuiTab.Inspector();
    private final ScrollView inspectorScroll;
    private boolean showGrid = true;
    private boolean snap = true;
    private int grid = 8;
    private List<GuiSchema.Type> types = GuiSchema.uiTypes();
    private String ownTitle;
    private Runnable ownSave;
    public Runnable reopen = SprauteScreen::open;

    public GuiTab(Project project, String json, String title, List<GuiSchema.Type> types, Runnable save) {
        this(project, null, json);
        this.ownTitle = title;
        this.ownSave = save;
        this.types = types;
        this.palette.setItems(types);
    }

    public String json() {
        return this.doc.toJson();
    }

    public void markSaved() {
        this.dirty = false;
        if (this.onDirtyChanged != null) {
            this.onDirtyChanged.run();
        }
    }

    public GuiTab(Project project, Project.FileRef file, String json) {
        super(project, file);
        this.doc = GuiDoc.fromJson(json);
        this.canvas = new GuiTab.EditCanvas();
        this.add(this.canvas);
        this.palette = this.add(new ListView<>((c, t, x, y, w, h, sel, hov) -> {
            c.icon(iconOf(t.key()), x + 8, y + (h - 9) / 2, colorOf(t.key()));
            c.text(t.title(), x + 22, y + (h - 8) / 2, -1841682);
        }));
        this.palette.rowHeight = 16;
        this.palette.background = -14934490;
        this.palette.setItems(this.types);
        this.palette.activateOnSingleClick = true;
        this.palette.onActivate = t -> this.addElement(t.key());
        this.palette.tooltip("Клик — добавить элемент. Если выбрана папка, кроп-зона или прокрутка — элемент попадёт внутрь");
        this.add(this.layers);
        this.inspectorScroll = this.add(new ScrollView(this.inspector) {
            @Override
            protected int contentHeight() {
                return GuiTab.this.inspector.prefH;
            }
        });
        this.buildToolbar();
        this.add(this.toolbar);
        this.layers.rebuild();
        this.inspector.rebuild();
    }

    private Button tool(Icon icon, String tip, Runnable action) {
        Button b = Button.icon(icon, tip, action);
        b.prefW = 20;
        this.toolbar.add(b);
        return b;
    }

    private Button toolText(String text, Icon icon, String tip, Runnable action) {
        Button b = new Button(text, action).icon(icon).style(Button.Style.GHOST);
        b.autoWidth();
        b.tooltip(tip);
        this.toolbar.add(b);
        return b;
    }

    private void sep() {
        Widget s = new Widget();
        s.prefW = 8;
        this.toolbar.add(s);
    }

    private void buildToolbar() {
        this.tool(Icon.UNDO, "Отменить (Ctrl+Z)", this::undo);
        this.tool(Icon.REDO, "Повторить (Ctrl+Y)", this::redo);
        this.sep();
        this.tool(Icon.LIST, "Сетка", () -> this.showGrid = !this.showGrid).toggle(() -> this.showGrid);
        this.tool(Icon.PIN, "Привязка к сетке и элементам (Alt при перетаскивании — без привязки)", () -> this.snap = !this.snap).toggle(() -> this.snap);
        this.tool(Icon.SEARCH, "Вписать макет в окно", this.canvas::fit);
        this.sep();
        this.tool(Icon.ARROW_RIGHT, "По левому краю (родителя или выделения)", () -> this.align(0, -1));
        this.tool(Icon.DOT, "По центру по горизонтали", () -> this.align(0, 0));
        this.tool(Icon.CHEVRON_RIGHT, "По правому краю", () -> this.align(0, 1));
        this.tool(Icon.CHEVRON_UP, "По верхнему краю", () -> this.align(1, -1));
        this.tool(Icon.MENU, "По центру по вертикали", () -> this.align(1, 0));
        this.tool(Icon.CHEVRON_DOWN, "По нижнему краю", () -> this.align(1, 1));
        this.tool(Icon.SKIP, "Распределить по горизонтали (от 3 элементов)", () -> this.distribute(0));
        this.tool(Icon.LIST, "Распределить по вертикали (от 3 элементов)", () -> this.distribute(1));
        this.sep();
        this.tool(Icon.FOLDER, "Сгруппировать в папку (Ctrl+G)", this::groupSelection);
        this.tool(Icon.CHEVRON_UP, "Вперёд (PageUp)", () -> this.move(1));
        this.tool(Icon.CHEVRON_DOWN, "Назад (PageDown)", () -> this.move(-1));
        this.tool(Icon.COPY, "Дублировать (Ctrl+D)", this::duplicate);
        this.tool(Icon.TRASH, "Удалить (Delete)", this::deleteSelected).iconColor(-1023878);
        this.sep();
        this.toolText("Анимация", Icon.PLAY, "Проиграть анимации появления и постоянные анимации на макете", () -> {
            this.canvas.playAppear = true;
            this.canvas.restartAppear();
            this.canvas.playUntil = System.currentTimeMillis() + 4000L;
        });
        this.toolText("Наведение", Icon.DOT, "Показать выделенный элемент так, будто мышь на нём", () -> this.showHover = !this.showHover)
            .toggle(() -> this.showHover);
        this.toolText("Открыть в игре", Icon.UPLOAD, "Показать интерфейс в игре, как его увидит игрок (Esc — назад)", this::testInGame)
            .style(Button.Style.SUCCESS);
    }

    private void testInGame() {
        if (Minecraft.getInstance().level == null) {
            UI ui = UI.get();
            if (ui != null) {
                ui.toastError("Войдите в мир, чтобы открыть интерфейс");
            }
        } else {
            this.save0();
            SprauteGuiScreen.openLocal(
                this.doc,
                this.project,
                (el, kind) -> {
                    if (Minecraft.getInstance().player != null) {
                        Minecraft.getInstance()
                            .player
                            .displayClientMessage(
                                Component.literal(
                                    kind.equals("click") ? "[Spraute] нажата кнопка «" + el + "» → button() = \"" + el + "\"" : "[Spraute] интерфейс закрыт"
                                ),
                                true
                            );
                    }
                },
                this.reopen
            );
        }
    }

    private GuiDoc.Element primary() {
        String last = null;

        for (String s : this.selection) {
            last = s;
        }

        return last == null ? null : this.doc.element(last);
    }

    private List<GuiDoc.Element> selected() {
        List<GuiDoc.Element> list = new ArrayList<>();

        for (String id : this.selection) {
            GuiDoc.Element e = this.doc.element(id);
            if (e != null) {
                list.add(e);
            }
        }

        return list;
    }

    private List<GuiDoc.Element> selectedRoots() {
        List<GuiDoc.Element> all = this.selected();
        List<GuiDoc.Element> out = new ArrayList<>();

        for (GuiDoc.Element e : all) {
            boolean nested = false;

            for (GuiDoc.Element o : all) {
                if (o != e && GuiDoc.contains(o, e)) {
                    nested = true;
                }
            }

            if (!nested) {
                out.add(e);
            }
        }

        return out;
    }

    private void select(String id) {
        if (this.selection.size() != 1 || id == null || !this.selection.contains(id)) {
            this.selection.clear();
            if (id != null) {
                this.selection.add(id);
            }

            this.selectionChanged();
        }
    }

    private void toggleSelect(String id) {
        if (!this.selection.remove(id)) {
            this.selection.add(id);
        }

        this.selectionChanged();
    }

    private void selectionChanged() {
        this.selection.removeIf(id -> this.doc.element(id) == null);
        this.editingKey = null;
        this.inspector.rebuild();
        this.layers.reveal(this.primary());
    }

    void snapshot() {
        String json = this.doc.toJson();
        if (this.undo.isEmpty() || !this.undo.get(this.undo.size() - 1).equals(json)) {
            this.undo.add(json);
            if (this.undo.size() > 200) {
                this.undo.remove(0);
            }

            this.redo.clear();
        }
    }

    private void changed() {
        if (!this.dirty) {
            this.dirty = true;
            if (this.onDirtyChanged != null) {
                this.onDirtyChanged.run();
            }
        }

        this.canvas.layoutTreeSafe();
        this.layers.rebuild();
    }

    private void restore(String json) {
        this.doc = GuiDoc.fromJson(json);
        this.canvas.doc = this.doc;
        this.selection.removeIf(id -> this.doc.element(id) == null);
        this.inspector.rebuild();
        this.changed();
    }

    @Override
    public void undo() {
        if (!this.undo.isEmpty()) {
            this.redo.add(this.doc.toJson());
            this.restore(this.undo.remove(this.undo.size() - 1));
        }
    }

    @Override
    public void redo() {
        if (!this.redo.isEmpty()) {
            this.undo.add(this.doc.toJson());
            this.restore(this.redo.remove(this.redo.size() - 1));
        }
    }

    private List<GuiDoc.Element> targetList() {
        GuiDoc.Element p = this.primary();
        if (p == null) {
            return this.doc.elements;
        } else if (p.isContainer()) {
            return p.children;
        } else {
            List<GuiDoc.Element> l = this.doc.listOf(p);
            return l != null ? l : this.doc.elements;
        }
    }

    private void addElement(String type) {
        this.snapshot();
        GuiDoc.Element e = new GuiDoc.Element();
        e.type = type;
        e.id = this.doc.freshId(type);
        List<GuiDoc.Element> list = this.targetList();
        list.add(e);
        if (list == this.doc.elements) {
            int n = 0;

            for (GuiDoc.Element o : this.doc.elements) {
                if (o != e && o.get("anchor").equals("center") && o.i("x") == n * 8 && o.i("y") == n * 8) {
                    n++;
                }
            }

            e.set("x", String.valueOf(n * 8));
            e.set("y", String.valueOf(n * 8));
        } else {
            e.set("anchor", "top_left");
            e.set("x", "6");
            e.set("y", String.valueOf(6 + (list.size() - 1) * 4));
        }

        this.changed();
        this.select(e.id);
    }

    private void deleteSelected() {
        List<GuiDoc.Element> roots = this.selectedRoots();
        if (!roots.isEmpty()) {
            this.snapshot();

            for (GuiDoc.Element e : roots) {
                List<GuiDoc.Element> l = this.doc.listOf(e);
                if (l != null) {
                    l.remove(e);
                }
            }

            this.selection.clear();
            this.selectionChanged();
            this.changed();
        }
    }

    private GuiDoc.Element copyWithFreshIds(GuiDoc.Element e) {
        GuiDoc.Element c = e.copy();
        this.renameAll(c, new HashSet<>());
        return c;
    }

    private void renameAll(GuiDoc.Element e, Set<String> taken) {
        String id = e.id;
        int n = 2;

        while (this.doc.element(id) != null || taken.contains(id)) {
            id = e.id.replaceAll("\\d+$", "") + n++;
        }

        e.id = id;
        taken.add(id);

        for (GuiDoc.Element c : e.children) {
            this.renameAll(c, taken);
        }
    }

    private void duplicate() {
        List<GuiDoc.Element> roots = this.selectedRoots();
        if (!roots.isEmpty()) {
            this.snapshot();
            List<String> fresh = new ArrayList<>();

            for (GuiDoc.Element e : roots) {
                List<GuiDoc.Element> l = this.doc.listOf(e);
                if (l != null) {
                    GuiDoc.Element copy = this.copyWithFreshIds(e);
                    copy.set("x", String.valueOf(e.i("x") + 8));
                    copy.set("y", String.valueOf(e.i("y") + 8));
                    copy.locked = false;
                    l.add(l.indexOf(e) + 1, copy);
                    fresh.add(copy.id);
                }
            }

            this.selection.clear();
            this.selection.addAll(fresh);
            this.changed();
            this.selectionChanged();
        }
    }

    private void copySelected() {
        List<GuiDoc.Element> roots = this.selectedRoots();
        if (!roots.isEmpty()) {
            clipboard = GSON.toJson(roots);
        }
    }

    private void paste() {
        if (clipboard != null) {
            List<GuiDoc.Element> items;
            try {
                items = GSON.fromJson(clipboard, (new TypeToken<List<GuiDoc.Element>>() {}).getType());
            } catch (RuntimeException var8) {
                return;
            }

            if (items != null && !items.isEmpty()) {
                this.snapshot();
                GuiDoc tmp = GuiDoc.fromJson(GSON.toJson(Map.of("elements", items)));
                List<GuiDoc.Element> list = this.targetList();
                List<String> fresh = new ArrayList<>();

                for (GuiDoc.Element e : tmp.elements) {
                    GuiDoc.Element c = this.copyWithFreshIds(e);
                    list.add(c);
                    fresh.add(c.id);
                }

                this.selection.clear();
                this.selection.addAll(fresh);
                this.changed();
                this.selectionChanged();
            }
        }
    }

    private void reparent(GuiDoc.Element e, List<GuiDoc.Element> target, int index) {
        int[] abs = (int[])this.canvas.rect(e).clone();
        List<GuiDoc.Element> from = this.doc.listOf(e);
        if (from != null) {
            int old = from.indexOf(e);
            from.remove(e);
            if (from == target && old < index) {
                index--;
            }

            target.add(Math.max(0, Math.min(target.size(), index)), e);
            this.canvas.layoutTreeSafe();
            this.applyRect(e, abs);
            this.canvas.layoutTreeSafe();
        }
    }

    private GuiDoc.Element containerOf(List<GuiDoc.Element> list) {
        for (GuiDoc.Element p : this.doc.all()) {
            if (p.children == list) {
                return p;
            }
        }

        return null;
    }

    private void groupSelection() {
        List<GuiDoc.Element> roots = this.selectedRoots();
        if (!roots.isEmpty()) {
            List<GuiDoc.Element> list = this.doc.listOf(roots.get(roots.size() - 1));
            if (list != null) {
                roots.removeIf(ex -> this.doc.listOf(ex) != list);
                this.snapshot();
                int x0 = Integer.MAX_VALUE;
                int y0 = Integer.MAX_VALUE;
                int x1 = Integer.MIN_VALUE;
                int y1 = Integer.MIN_VALUE;
                Map<GuiDoc.Element, int[]> abs = new IdentityHashMap<>();
                int at = 0;

                for (GuiDoc.Element e : roots) {
                    int[] r = (int[])this.canvas.rect(e).clone();
                    abs.put(e, r);
                    x0 = Math.min(x0, r[0]);
                    y0 = Math.min(y0, r[1]);
                    x1 = Math.max(x1, r[0] + r[2]);
                    y1 = Math.max(y1, r[1] + r[3]);
                    at = Math.max(at, list.indexOf(e));
                }

                GuiDoc.Element g = new GuiDoc.Element();
                g.type = "group";
                g.id = this.doc.freshId("group");
                g.set("anchor", "top_left");
                list.add(at + 1, g);
                this.canvas.layoutTreeSafe();
                this.applyRect(g, new int[]{x0, y0, x1 - x0, y1 - y0});
                this.canvas.layoutTreeSafe();
                List<GuiDoc.Element> ordered = new ArrayList<>(list);
                ordered.retainAll(roots);

                for (GuiDoc.Element e : ordered) {
                    list.remove(e);
                    g.children.add(e);
                }

                this.canvas.layoutTreeSafe();

                for (GuiDoc.Element e : ordered) {
                    e.set("anchor", "top_left");
                    this.applyRect(e, abs.get(e));
                }

                this.selection.clear();
                this.selection.add(g.id);
                this.changed();
                this.selectionChanged();
            }
        }
    }

    private void ungroup() {
        List<GuiDoc.Element> roots = this.selectedRoots();
        List<String> released = new ArrayList<>();
        boolean any = false;

        for (GuiDoc.Element g : roots) {
            if (g.isContainer()) {
                any = true;
            }
        }

        if (any) {
            this.snapshot();

            for (GuiDoc.Element gx : roots) {
                if (gx.isContainer()) {
                    List<GuiDoc.Element> list = this.doc.listOf(gx);
                    if (list != null) {
                        int at = list.indexOf(gx);
                        Map<GuiDoc.Element, int[]> abs = new IdentityHashMap<>();

                        for (GuiDoc.Element c : gx.children) {
                            abs.put(c, (int[])this.canvas.rect(c).clone());
                        }

                        List<GuiDoc.Element> kids = new ArrayList<>(gx.children);
                        gx.children.clear();
                        list.remove(gx);

                        for (int i = 0; i < kids.size(); i++) {
                            list.add(at + i, kids.get(i));
                        }

                        this.canvas.layoutTreeSafe();

                        for (GuiDoc.Element c : kids) {
                            this.applyRect(c, abs.get(c));
                            released.add(c.id);
                        }
                    }
                }
            }

            this.selection.clear();
            this.selection.addAll(released);
            this.changed();
            this.selectionChanged();
        }
    }

    private void move(int dir) {
        List<GuiDoc.Element> roots = this.selectedRoots();
        if (!roots.isEmpty()) {
            this.snapshot();

            for (GuiDoc.Element e : dir > 0 ? reversed(roots) : roots) {
                List<GuiDoc.Element> l = this.doc.listOf(e);
                if (l != null) {
                    int i = l.indexOf(e);
                    int j = dir == 2 ? l.size() - 1 : (dir == -2 ? 0 : Math.max(0, Math.min(l.size() - 1, i + dir)));
                    if (i != j) {
                        l.remove(i);
                        l.add(j, e);
                    }
                }
            }

            this.changed();
        }
    }

    private static <T> List<T> reversed(List<T> l) {
        List<T> r = new ArrayList<>(l);
        Collections.reverse(r);
        return r;
    }

    private void align(int axis, int where) {
        List<GuiDoc.Element> els = this.selectedRoots();
        if (!els.isEmpty()) {
            this.snapshot();
            float b0;
            float b1;
            if (els.size() == 1) {
                float[] ar = this.canvas.areaOf(els.get(0));
                b0 = ar[axis];
                b1 = ar[axis] + ar[axis + 2];
            } else {
                b0 = Float.MAX_VALUE;
                b1 = -Float.MAX_VALUE;

                for (GuiDoc.Element e : els) {
                    int[] r = this.canvas.rect(e);
                    b0 = Math.min(b0, (float)r[axis]);
                    b1 = Math.max(b1, (float)(r[axis] + r[axis + 2]));
                }
            }

            for (GuiDoc.Element e : els) {
                int[] r = (int[])this.canvas.rect(e).clone();
                r[axis] = where < 0 ? Math.round(b0) : (where == 0 ? Math.round((b0 + b1 - r[axis + 2]) / 2.0F) : Math.round(b1 - r[axis + 2]));
                this.applyRect(e, r);
            }

            this.changed();
        }
    }

    private void distribute(int axis) {
        List<GuiDoc.Element> els = this.selectedRoots();
        if (els.size() < 3) {
            UI ui = UI.get();
            if (ui != null) {
                ui.toastInfo("Выберите хотя бы 3 элемента (Shift+клик)");
            }
        } else {
            this.snapshot();
            els.sort((a, b) -> Integer.compare(this.canvas.rect(a)[axis], this.canvas.rect(b)[axis]));
            int[] first = this.canvas.rect(els.get(0));
            int[] last = this.canvas.rect(els.get(els.size() - 1));
            int total = 0;

            for (GuiDoc.Element e : els) {
                total += this.canvas.rect(e)[axis + 2];
            }

            float gap = (float)(last[axis] + last[axis + 2] - first[axis] - total) / (els.size() - 1);
            float pos = first[axis];

            for (GuiDoc.Element e : els) {
                int[] r = (int[])this.canvas.rect(e).clone();
                r[axis] = Math.round(pos);
                pos += r[axis + 2] + gap;
                this.applyRect(e, r);
            }

            this.changed();
        }
    }

    void applyRect(GuiDoc.Element e, int[] r) {
        int[] off = this.canvas.offsetFor(e, r[0], r[1], r[2], r[3]);
        float[] ar = this.canvas.areaOf(e);
        e.set("x", keepUnit(e.get("x"), off[0], ar[2]));
        e.set("y", keepUnit(e.get("y"), off[1], ar[3]));
        e.set("w", keepUnit(e.get("w"), Math.max(1, r[2]), ar[2]));
        e.set("h", keepUnit(e.get("h"), Math.max(1, r[3]), ar[3]));
    }

    private static String keepUnit(String old, int px, float base) {
        if (old != null && old.trim().endsWith("%") && base > 0.0F) {
            float pct = Math.round(px * 1000.0F / base) / 10.0F;
            return (pct == (int)pct ? String.valueOf((int)pct) : String.valueOf(pct)) + "%";
        } else {
            return String.valueOf(px);
        }
    }

    private void renameElement(GuiDoc.Element e) {
        Dialog.prompt("Имя элемента", "Id (по нему сюжет обращается к элементу):", e.id, v -> {
            if (v.isBlank()) {
                return "Введите имя";
            } else if (!v.matches("[A-Za-z0-9_\\-а-яА-ЯёЁ]+")) {
                return "Только буквы, цифры, _ и -";
            } else {
                GuiDoc.Element other = this.doc.element(v);
                return other != null && other != e ? "Такой id уже есть" : null;
            }
        }, v -> {
            this.snapshot();
            boolean was = this.selection.remove(e.id);
            e.id = v;
            if (was) {
                this.selection.add(v);
            }

            this.inspector.rebuild();
            this.changed();
        });
    }

    private void elementMenu(GuiDoc.Element e, int mx, int my) {
        if (!this.selection.contains(e.id)) {
            this.select(e.id);
        }

        PopupMenu pm = new PopupMenu();
        pm.item("Переименовать…", Icon.EDIT, "F2", () -> this.renameElement(e));
        pm.item("Копировать id", Icon.COPY, null, () -> Minecraft.getInstance().keyboardHandler.setClipboard(e.id));
        pm.separator();
        pm.item(e.b("visible") ? "Скрыть" : "Показать", Icon.SEARCH, "Ctrl+H", () -> this.toggleVisible(e));
        pm.item(e.locked ? "Разблокировать" : "Заблокировать на холсте", Icon.FLAG, "Ctrl+L", () -> {
            e.locked = !e.locked;
            this.changed();
        });
        pm.separator();
        pm.item("Сгруппировать в папку", Icon.FOLDER, "Ctrl+G", this::groupSelection);
        if (e.isContainer()) {
            pm.item("Разгруппировать", Icon.FOLDER, "Ctrl+Shift+G", this::ungroup);
        }

        List<GuiDoc.Element> containers = new ArrayList<>();

        for (GuiDoc.Element c : this.doc.all()) {
            if (c.isContainer() && !GuiDoc.contains(e, c)) {
                containers.add(c);
            }
        }

        if (!containers.isEmpty() || this.doc.listOf(e) != this.doc.elements) {
            pm.item("Переместить в…", Icon.ARROW_RIGHT, null, () -> {
                PopupMenu sub = new PopupMenu();
                sub.item("Корень интерфейса", Icon.HOME, null, () -> this.moveSelectionInto(this.doc.elements));

                for (GuiDoc.Element cx : containers) {
                    sub.item(cx.id + "  (" + cx.schema().title() + ")", iconOf(cx.type), null, () -> this.moveSelectionInto(cx.children));
                }

                sub.show(mx + 20, my);
            });
        }

        pm.separator();
        pm.item("На передний план", Icon.CHEVRON_UP, "Ctrl+PageUp", () -> this.move(2));
        pm.item("На задний план", Icon.CHEVRON_DOWN, "Ctrl+PageDown", () -> this.move(-2));
        pm.item("Дублировать", Icon.COPY, "Ctrl+D", this::duplicate);
        pm.item("Удалить", Icon.TRASH, "Delete", this::deleteSelected).color(-1023878);
        pm.show(mx, my);
    }

    private void moveSelectionInto(List<GuiDoc.Element> target) {
        this.snapshot();

        for (GuiDoc.Element e : this.selectedRoots()) {
            GuiDoc.Element owner = this.containerOf(target);
            if (owner == null || !GuiDoc.contains(e, owner)) {
                this.reparent(e, target, target.size());
            }
        }

        this.changed();
    }

    private void toggleVisible(GuiDoc.Element e) {
        this.snapshot();
        e.set("visible", e.b("visible") ? "false" : "true");
        this.changed();
    }

    static Icon iconOf(String type) {
        return switch (type) {
            case "group" -> Icon.FOLDER;
            case "drag" -> Icon.CUBE;
            case "drop_zone" -> Icon.PIN;
            case "clip" -> Icon.SKIP;
            case "scroll" -> Icon.LIST;
            case "panel" -> Icon.FILE;
            case "image" -> Icon.IMAGE;
            case "text" -> Icon.EDIT;
            case "button" -> Icon.PLAY;
            case "entity" -> Icon.USER;
            case "item" -> Icon.CUBE;
            case "slot" -> Icon.CUBE;
            case "inventory" -> Icon.LIST;
            case "progress" -> Icon.MENU;
            case "input" -> Icon.CODE;
            default -> Icon.DOT;
        };
    }

    static int colorOf(String type) {
        return switch (type) {
            case "group" -> -870325;
            case "drag" -> -10625910;
            case "drop_zone" -> -12400641;
            case "clip" -> -13461;
            case "scroll" -> -10638858;
            case "panel" -> -5195831;
            case "image" -> -10638858;
            case "text" -> -1841682;
            case "button" -> -7639809;
            case "entity" -> -12988235;
            case "item" -> -870325;
            case "slot" -> -28838;
            case "inventory" -> -28838;
            case "progress" -> -11746422;
            case "input" -> -3938163;
            default -> -6577995;
        };
    }

    private static boolean key(int k) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), k) == 1;
    }

    private static boolean shiftDown() {
        return key(340) || key(344);
    }

    private static boolean ctrlDown() {
        return key(341) || key(345);
    }

    private void docMenu(int mx, int my) {
        PopupMenu pm = new PopupMenu();

        for (GuiSchema.Type t : this.types) {
            pm.item("Добавить: " + t.title(), iconOf(t.key()), null, () -> this.addElement(t.key())).color(colorOf(t.key()));
        }

        pm.separator();
        pm.item("Вставить", Icon.COPY, "Ctrl+V", this::paste).enabled(() -> clipboard != null);
        pm.item("Вписать в окно", Icon.SEARCH, null, this.canvas::fit);
        pm.show(mx, my);
    }

    @Override
    public void layout() {
        this.toolbar.bounds(this.x, this.y, this.w, 28);
        this.toolbar.layout();
        int top = this.y + 28;
        int bodyH = this.h - 28;
        int paletteH = Math.min(this.types.size() * 16 + 4, bodyH * 2 / 5);
        this.palette.bounds(this.x, top + 16, 168, paletteH);
        this.layers.bounds(this.x, top + 16 + paletteH + 16, 168, bodyH - paletteH - 32);
        this.canvas.bounds(this.x + 168 + 1, top, this.w - 168 - 236 - 2, bodyH);
        this.inspectorScroll.bounds(this.x + this.w - 236, top, 236, bodyH);
        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -14934490);
        int top = this.y + 28;
        c.fill(this.x, top - 1, this.w, 1, -13749952);
        c.fill(this.x + 168, top, 1, this.h - 28, -13749952);
        c.fill(this.x + this.w - 236 - 1, top, 1, this.h - 28, -13749952);
        c.text("ЭЛЕМЕНТЫ", this.x + 8, top + 5, -9933435);
        c.text("СЛОИ  ·  сверху — ближе", this.x + 8, this.palette.y + this.palette.h + 5, -9933435);
    }

    public List<String> elementIds() {
        return this.doc.ids();
    }

    @Override
    public String title() {
        return this.ownTitle != null ? this.ownTitle : (this.file != null ? this.file.name() : "Интерфейс");
    }

    private void save0() {
        try {
            this.save();
        } catch (IOException var3) {
            UI ui = UI.get();
            if (ui != null) {
                ui.toastError("Ошибка сохранения: " + var3.getMessage());
            }
        }
    }

    @Override
    public Icon icon() {
        return Icon.MENU;
    }

    @Override
    public int iconColor() {
        return -10638858;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void save() throws IOException {
        if (this.file == null) {
            if (this.ownSave != null) {
                this.ownSave.run();
            }
        } else {
            this.project.write(this.file, this.doc.toJson());
        }

        this.dirty = false;
        if (this.onDirtyChanged != null) {
            this.onDirtyChanged.run();
        }
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
        this.canvas.requestFocus();
    }

    private final class EditCanvas extends GuiView implements UI.CursorProvider {
        private float zoom = 2.0F;
        private float panX;
        private float panY;
        private boolean fitted;
        long playUntil;
        private int mode;
        private int handle;
        private final Map<GuiDoc.Element, int[]> startRects = new IdentityHashMap<>();
        private double startX;
        private double startY;
        private double curX;
        private double curY;
        private final List<float[]> guides = new ArrayList<>();

        EditCanvas() {
            super(GuiTab.this.doc, GuiTab.this.project.name());
            this.interactive = false;
            this.playAppear = false;
            this.localProject = GuiTab.this.project;
        }

        void layoutTreeSafe() {
            this.placeArea();
            this.layoutTree();
        }

        void fit() {
            float aw = this.doc.width();
            float ah = this.doc.height();
            this.zoom = Math.max(0.25F, Math.min((this.w - 40) / aw, (this.h - 40) / ah));
            this.panX = (this.w - aw * this.zoom) / 2.0F;
            this.panY = (this.h - ah * this.zoom) / 2.0F;
            this.fitted = true;
        }

        @Override
        public void layout() {
            super.layout();
            if (!this.fitted && this.w > 0 && this.h > 0) {
                this.fit();
            }
        }

        @Override
        protected void placeArea() {
            this.areaW = this.doc.width();
            this.areaH = this.doc.height();
            this.areaScale = this.zoom;
            this.areaX = this.x + this.panX;
            this.areaY = this.y + this.panY;
        }

        @Override
        protected boolean showHidden() {
            return true;
        }

        @Override
        protected String forcedHover() {
            GuiDoc.Element e = GuiTab.this.showHover ? GuiTab.this.primary() : null;
            return e != null ? e.id : null;
        }

        @Override
        protected float time() {
            if (this.playAppear && System.currentTimeMillis() > this.playUntil) {
                this.playAppear = false;
            }

            return super.time();
        }

        @Override
        protected void paintBackdrop(Canvas c) {
            c.fill(this.x, this.y, this.w, this.h, -15921646);
            int ax = Math.round(this.areaX);
            int ay = Math.round(this.areaY);
            int aw = Math.round(this.areaW * this.areaScale);
            int ah = Math.round(this.areaH * this.areaScale);
            int vx0 = Math.max(ax, this.x);
            int vy0 = Math.max(ay, this.y);
            int vx1 = Math.min(ax + aw, this.x + this.w);
            int vy1 = Math.min(ay + ah, this.y + this.h);
            if (vx1 > vx0 && vy1 > vy0) {
                c.fill(vx0, vy0, vx1 - vx0, vy1 - vy0, -13351884);
                int cell = 24;
                int cx0 = ax + Math.floorDiv(vx0 - ax, cell) * cell;
                int cy0 = ay + Math.floorDiv(vy0 - ay, cell) * cell;

                for (int yy = cy0; yy < vy1; yy += cell) {
                    int row = (yy - ay) / cell;

                    for (int xx = cx0 + ((row + xx(cx0, ax, cell) & 1) == 0 ? 0 : cell); xx < vx1; xx += cell * 2) {
                        int fx = Math.max(xx, vx0);
                        int fy = Math.max(yy, vy0);
                        c.fill(fx, fy, Math.min(xx + cell, vx1) - fx, Math.min(yy + cell, vy1) - fy, -12957126);
                    }
                }

                int bg = GuiDoc.toColor(this.doc.get("background"));
                if (bg >>> 24 != 0) {
                    c.fill(vx0, vy0, vx1 - vx0, vy1 - vy0, bg);
                }

                if (GuiTab.this.showGrid && GuiTab.this.grid > 0) {
                    int step = GuiTab.this.grid;

                    while (step * this.areaScale < 8.0F) {
                        step *= 2;
                    }

                    int gx0 = (int)Math.ceil(Math.max(0.0F, this.toDesignX(vx0)) / step) * step;
                    int gy0 = (int)Math.ceil(Math.max(0.0F, this.toDesignY(vy0)) / step) * step;

                    for (int gx = gx0; gx < this.areaW && this.areaX + gx * this.areaScale < vx1; gx += step) {
                        c.fill(Math.round(this.areaX + gx * this.areaScale), vy0, 1, vy1 - vy0, gx / step % 4 == 0 ? 654311423 : 318767103);
                    }

                    for (int gy = gy0; gy < this.areaH && this.areaY + gy * this.areaScale < vy1; gy += step) {
                        c.fill(vx0, Math.round(this.areaY + gy * this.areaScale), vx1 - vx0, 1, gy / step % 4 == 0 ? 654311423 : 318767103);
                    }
                }

                c.fill(ax + aw / 2, vy0, 1, vy1 - vy0, 580946687);
                c.fill(vx0, ay + ah / 2, vx1 - vx0, 1, 580946687);
            }

            c.outline(ax - 1, ay - 1, aw + 2, ah + 2, -12762793);
            String size = this.doc.width()
                + " × "
                + this.doc.height()
                + (this.doc.get("layout").equals("screen") ? " (экран)" : "")
                + "   "
                + Math.round(this.zoom * 100.0F)
                + "%";
            c.text(size, ax, ay - 11, -9933435);
        }

        private static int xx(int cx0, int ax, int cell) {
            return Math.floorDiv(cx0 - ax, cell);
        }

        private int[] screenRect(GuiDoc.Element e) {
            int[] r = this.rect(e);
            return new int[]{
                Math.round(this.areaX + r[0] * this.areaScale),
                Math.round(this.areaY + r[1] * this.areaScale),
                Math.round(r[2] * this.areaScale),
                Math.round(r[3] * this.areaScale)
            };
        }

        private int[][] handles(int[] s) {
            int x0 = s[0];
            int y0 = s[1];
            int x1 = s[0] + s[2];
            int y1 = s[1] + s[3];
            int xm = (x0 + x1) / 2;
            int ym = (y0 + y1) / 2;
            return new int[][]{{x0, y0}, {xm, y0}, {x1, y0}, {x1, ym}, {x1, y1}, {xm, y1}, {x0, y1}, {x0, ym}};
        }

        @Override
        protected void paintOver(Canvas c, int mx, int my, float dt) {
            c.pushClip(this.x, this.y, this.w, this.h);
            if (this.mode == 0) {
                GuiDoc.Element hov = this.target(mx, my, false);
                if (hov != null && !GuiTab.this.selection.contains(hov.id)) {
                    int[] s = this.screenRect(hov);
                    c.outline(s[0], s[1], s[2], s[3], -2002744577);
                }
            }

            for (GuiDoc.Element e : this.doc.all()) {
                if (e.type.equals("group") && !GuiTab.this.selection.contains(e.id)) {
                    int[] s = this.screenRect(e);
                    this.dashed(c, s, 872396875);
                }
            }

            List<GuiDoc.Element> sel = GuiTab.this.selected();

            for (GuiDoc.Element ex : sel) {
                int[] s = this.screenRect(ex);
                c.outline(s[0] - 1, s[1] - 1, s[2] + 2, s[3] + 2, -7639809);
            }

            GuiDoc.Element p = GuiTab.this.primary();
            if (p != null && sel.size() == 1 && !p.locked) {
                for (int[] hp : this.handles(this.screenRect(p))) {
                    c.fill(hp[0] - 3, hp[1] - 3, 7, 7, -7639809);
                    c.fill(hp[0] - 2, hp[1] - 2, 5, 5, -1);
                }
            }

            if (p != null) {
                int[] s = this.screenRect(p);
                String label = (sel.size() > 1 ? sel.size() + " эл.  " : "")
                    + p.id
                    + "  "
                    + GuiTab.this.canvas.rect(p)[2]
                    + "×"
                    + GuiTab.this.canvas.rect(p)[3];
                int lw = c.width(label) + 8;
                c.round(s[0], s[1] - 13, lw, 12, 3, -7639809);
                c.text(label, s[0] + 4, s[1] - 11, -1);
            }

            for (float[] g : this.guides) {
                if (g[0] == 0.0F) {
                    c.fill(Math.round(this.areaX + g[1] * this.areaScale), this.y, 1, this.h, -42336);
                } else {
                    c.fill(this.x, Math.round(this.areaY + g[1] * this.areaScale), this.w, 1, -42336);
                }
            }

            if (this.mode == 4) {
                int x0 = (int)Math.min(this.startX, this.curX);
                int y0 = (int)Math.min(this.startY, this.curY);
                int ww = (int)Math.abs(this.curX - this.startX);
                int hh = (int)Math.abs(this.curY - this.startY);
                c.fill(x0, y0, ww, hh, 579562751);
                c.outline(x0, y0, ww, hh, -7639809);
            }

            c.popClip();
            if (this.doc.elements.isEmpty()) {
                c.textCentered("Добавьте элементы из палитры слева", this.x + this.w / 2, this.y + this.h / 2 - 4, -9933435);
            }
        }

        private void dashed(Canvas c, int[] s, int color) {
            for (int i = 0; i < s[2]; i += 6) {
                c.fill(s[0] + i, s[1], Math.min(3, s[2] - i), 1, color);
                c.fill(s[0] + i, s[1] + s[3] - 1, Math.min(3, s[2] - i), 1, color);
            }

            for (int i = 0; i < s[3]; i += 6) {
                c.fill(s[0], s[1] + i, 1, Math.min(3, s[3] - i), color);
                c.fill(s[0] + s[2] - 1, s[1] + i, 1, Math.min(3, s[3] - i), color);
            }
        }

        private List<GuiDoc.Element> chainAt(double mx, double my) {
            GuiDoc.Element deep = this.pick(this.toDesignX(mx), this.toDesignY(my), ex -> !ex.locked);
            List<GuiDoc.Element> chain = new ArrayList<>();

            for (GuiDoc.Element e = deep; e != null; e = this.doc.parentOf(e)) {
                chain.add(0, e);
            }

            return chain;
        }

        private GuiDoc.Element target(double mx, double my, boolean deeper) {
            List<GuiDoc.Element> chain = this.chainAt(mx, my);
            if (chain.isEmpty()) {
                return null;
            } else {
                int level = 0;

                for (GuiDoc.Element s : GuiTab.this.selected()) {
                    int idx = chain.indexOf(s);
                    if (idx >= 0) {
                        level = Math.max(level, deeper ? idx + 1 : idx);
                    } else {
                        int pi = chain.indexOf(this.doc.parentOf(s));
                        if (pi >= 0) {
                            level = Math.max(level, pi + 1);
                        }
                    }
                }

                return chain.get(Math.min(level, chain.size() - 1));
            }
        }

        private int handleAt(double mx, double my) {
            GuiDoc.Element e = GuiTab.this.primary();
            if (e != null && !e.locked && GuiTab.this.selection.size() == 1) {
                int[][] hs = this.handles(this.screenRect(e));

                for (int i = 0; i < hs.length; i++) {
                    if (Math.abs(mx - hs[i][0]) <= 4.0 && Math.abs(my - hs[i][1]) <= 4.0) {
                        return i;
                    }
                }

                return -1;
            } else {
                return -1;
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            this.computeArea();
            this.requestFocus();
            this.startX = this.curX = mx;
            this.startY = this.curY = my;
            if (button != 2 && (button != 0 || !GuiTab.key(32))) {
                if (button == 1) {
                    GuiDoc.Element e = this.target(mx, my, false);
                    if (e != null) {
                        GuiTab.this.elementMenu(e, (int)mx, (int)my);
                    } else {
                        GuiTab.this.docMenu((int)mx, (int)my);
                    }

                    return true;
                } else {
                    int hd = this.handleAt(mx, my);
                    if (hd >= 0) {
                        GuiTab.this.snapshot();
                        this.mode = 2;
                        this.handle = hd;
                        this.startRects.clear();
                        this.startRects.put(GuiTab.this.primary(), (int[])this.rect(GuiTab.this.primary()).clone());
                        return true;
                    } else {
                        GuiDoc.Element e = this.target(mx, my, false);
                        boolean additive = GuiTab.shiftDown() || GuiTab.ctrlDown();
                        if (e == null) {
                            if (!additive) {
                                GuiTab.this.select(null);
                            }

                            this.mode = 4;
                            return true;
                        } else {
                            if (additive) {
                                GuiTab.this.toggleSelect(e.id);
                            } else if (!GuiTab.this.selection.contains(e.id)) {
                                GuiTab.this.select(e.id);
                            } else {
                                GuiTab.this.selection.remove(e.id);
                                GuiTab.this.selection.add(e.id);
                                GuiTab.this.inspector.rebuild();
                            }

                            GuiTab.this.snapshot();
                            this.mode = 1;
                            this.startRects.clear();

                            for (GuiDoc.Element s : GuiTab.this.selectedRoots()) {
                                if (!s.locked) {
                                    this.startRects.put(s, (int[])this.rect(s).clone());
                                }
                            }

                            return true;
                        }
                    }
                }
            } else {
                this.mode = 3;
                return true;
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
            this.curX = mx;
            this.curY = my;
            if (this.mode == 3) {
                this.panX += (float)ddx;
                this.panY += (float)ddy;
                return true;
            } else if (this.mode == 4) {
                return true;
            } else {
                GuiDoc.Element p = GuiTab.this.primary();
                if (p != null && !this.startRects.isEmpty()) {
                    float dx = (float)((mx - this.startX) / this.areaScale);
                    float dy = (float)((my - this.startY) / this.areaScale);
                    this.guides.clear();
                    boolean useSnap = GuiTab.this.snap && !GuiTab.key(342);
                    if (this.mode == 1) {
                        int[] base = this.startRects.getOrDefault(p, this.startRects.values().iterator().next());
                        int[] moved = (int[])base.clone();
                        moved[0] = Math.round(base[0] + dx);
                        moved[1] = Math.round(base[1] + dy);
                        if (useSnap) {
                            this.snapMove(moved, this.startRects.keySet());
                        }

                        int sx = moved[0] - base[0];
                        int sy = moved[1] - base[1];

                        for (Entry<GuiDoc.Element, int[]> en : this.startRects.entrySet()) {
                            int[] r = (int[])en.getValue().clone();
                            r[0] += sx;
                            r[1] += sy;
                            GuiTab.this.applyRect(en.getKey(), r);
                        }
                    } else if (this.mode == 2) {
                        int[] s = this.startRects.get(p);
                        int x0 = s[0];
                        int y0 = s[1];
                        int x1 = x0 + s[2];
                        int y1 = y0 + s[3];
                        boolean left = this.handle == 0 || this.handle == 6 || this.handle == 7;
                        boolean right = this.handle == 2 || this.handle == 3 || this.handle == 4;
                        boolean top = this.handle == 0 || this.handle == 1 || this.handle == 2;
                        boolean bottom = this.handle == 4 || this.handle == 5 || this.handle == 6;
                        if (left) {
                            x0 = this.snapEdge(p, Math.round(s[0] + dx), 0, useSnap);
                        }

                        if (right) {
                            x1 = this.snapEdge(p, Math.round(s[0] + s[2] + dx), 0, useSnap);
                        }

                        if (top) {
                            y0 = this.snapEdge(p, Math.round(s[1] + dy), 1, useSnap);
                        }

                        if (bottom) {
                            y1 = this.snapEdge(p, Math.round(s[1] + s[3] + dy), 1, useSnap);
                        }

                        if (x1 - x0 < 2) {
                            if (left) {
                                x0 = x1 - 2;
                            } else {
                                x1 = x0 + 2;
                            }
                        }

                        if (y1 - y0 < 2) {
                            if (top) {
                                y0 = y1 - 2;
                            } else {
                                y1 = y0 + 2;
                            }
                        }

                        if (GuiTab.shiftDown() && s[3] > 0) {
                            float ratio = (float)s[2] / s[3];
                            int nh = Math.round((x1 - x0) / ratio);
                            if (top) {
                                y0 = y1 - nh;
                            } else {
                                y1 = y0 + nh;
                            }
                        }

                        GuiTab.this.applyRect(p, new int[]{x0, y0, x1 - x0, y1 - y0});
                    }

                    this.layoutTree();
                    this.dirtyQuiet();
                    return true;
                } else {
                    return true;
                }
            }
        }

        private void dirtyQuiet() {
            if (!GuiTab.this.dirty) {
                GuiTab.this.dirty = true;
                if (GuiTab.this.onDirtyChanged != null) {
                    GuiTab.this.onDirtyChanged.run();
                }
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (this.mode == 4) {
                float dx0 = this.toDesignX(Math.min(this.startX, this.curX));
                float dy0 = this.toDesignY(Math.min(this.startY, this.curY));
                float dx1 = this.toDesignX(Math.max(this.startX, this.curX));
                float dy1 = this.toDesignY(Math.max(this.startY, this.curY));
                if (dx1 - dx0 > 1.0F || dy1 - dy0 > 1.0F) {
                    if (!GuiTab.shiftDown() && !GuiTab.ctrlDown()) {
                        GuiTab.this.selection.clear();
                    }

                    for (GuiDoc.Element e : this.doc.elements) {
                        if (!e.locked && e.b("visible")) {
                            int[] r = this.rect(e);
                            if (r[0] < dx1 && r[0] + r[2] > dx0 && r[1] < dy1 && r[1] + r[3] > dy0) {
                                GuiTab.this.selection.add(e.id);
                            }
                        }
                    }

                    GuiTab.this.selectionChanged();
                }
            }

            if (this.mode == 1 || this.mode == 2) {
                GuiTab.this.inspector.rebuild();
                GuiTab.this.layers.rebuild();
            }

            this.mode = 0;
            this.guides.clear();
            this.startRects.clear();
            return true;
        }

        private float threshold() {
            return 5.0F / this.areaScale;
        }

        private List<Float> lines(Set<GuiDoc.Element> moving, GuiDoc.Element ref, int axis) {
            List<Float> list = new ArrayList<>();
            float[] ar = this.areaOf(ref);
            list.add(ar[axis]);
            list.add(ar[axis] + ar[axis + 2] / 2.0F);
            list.add(ar[axis] + ar[axis + 2]);

            for (GuiDoc.Element o : this.doc.all()) {
                boolean skip = false;

                for (GuiDoc.Element m : moving) {
                    if (GuiDoc.contains(m, o)) {
                        skip = true;
                    }
                }

                if (!skip && o.b("visible")) {
                    int[] r = this.rect(o);
                    int pos = r[axis];
                    int s = r[axis + 2];
                    list.add((float)pos);
                    list.add(pos + s / 2.0F);
                    list.add((float)(pos + s));
                }
            }

            return list;
        }

        private void snapMove(int[] r, Set<GuiDoc.Element> moving) {
            GuiDoc.Element ref = GuiTab.this.primary();

            for (int axis = 0; axis < 2; axis++) {
                int pos = r[axis];
                int size = r[axis + 2];
                float best = this.threshold() + 1.0F;
                float shift = 0.0F;
                float at = 0.0F;

                for (float line : this.lines(moving, ref, axis)) {
                    for (float edge : new float[]{pos, pos + size / 2.0F, pos + size}) {
                        float d = Math.abs(line - edge);
                        if (d < best) {
                            best = d;
                            shift = line - edge;
                            at = line;
                        }
                    }
                }

                if (best <= this.threshold()) {
                    r[axis] = Math.round(pos + shift);
                    this.guides.add(new float[]{axis, at});
                } else if (GuiTab.this.grid > 0) {
                    float origin = this.areaOf(ref)[axis];
                    r[axis] = Math.round(origin + Math.round((pos - origin) / GuiTab.this.grid) * GuiTab.this.grid);
                }
            }
        }

        private int snapEdge(GuiDoc.Element e, int value, int axis, boolean useSnap) {
            if (!useSnap) {
                return value;
            } else {
                float best = this.threshold() + 1.0F;
                float at = value;

                for (float line : this.lines(Set.of(e), e, axis)) {
                    float d = Math.abs(line - value);
                    if (d < best) {
                        best = d;
                        at = line;
                    }
                }

                if (best <= this.threshold()) {
                    this.guides.add(new float[]{axis, at});
                    return Math.round(at);
                } else if (GuiTab.this.grid <= 0) {
                    return value;
                } else {
                    float origin = this.areaOf(e)[axis];
                    return Math.round(origin + Math.round((value - origin) / GuiTab.this.grid) * GuiTab.this.grid);
                }
            }
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            this.computeArea();
            if (!GuiTab.ctrlDown() && GuiTab.shiftDown()) {
                this.scrollAt(this.toDesignX(mx), this.toDesignY(my), delta, false);
            } else {
                float before = this.zoom;
                this.zoom = Math.max(0.25F, Math.min(8.0F, this.zoom * (delta > 0.0 ? 1.15F : 0.86956525F)));
                float k = this.zoom / before;
                this.panX = (float)(mx - this.x - (mx - this.x - this.panX) * k);
                this.panY = (float)(my - this.y - (my - this.y - this.panY) * k);
            }

            return true;
        }

        @Override
        public boolean mouseDoubleClicked(double mx, double my, int button) {
            GuiDoc.Element e = this.target(mx, my, true);
            if (e == null) {
                return true;
            } else if (GuiTab.this.selection.contains(e.id) && GuiTab.this.selection.size() <= 1) {
                GuiTab.this.inspector.focus("text");
                return true;
            } else {
                GuiTab.this.select(e.id);
                return true;
            }
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            boolean ctrl = (mods & 2) != 0;
            boolean shift = (mods & 1) != 0;
            GuiDoc.Element e = GuiTab.this.primary();
            switch (key) {
                case 65:
                    if (!ctrl) {
                        return false;
                    }

                    GuiTab.this.selection.clear();
                    List<GuiDoc.Element> scope = e != null ? this.doc.listOf(e) : this.doc.elements;

                    for (GuiDoc.Element o : scope != null ? scope : this.doc.elements) {
                        GuiTab.this.selection.add(o.id);
                    }

                    GuiTab.this.selectionChanged();
                    break;
                case 67:
                    if (!ctrl) {
                        return false;
                    }

                    GuiTab.this.copySelected();
                    break;
                case 68:
                    if (!ctrl) {
                        return false;
                    }

                    GuiTab.this.duplicate();
                    break;
                case 71:
                    if (!ctrl) {
                        return false;
                    }

                    if (shift) {
                        GuiTab.this.ungroup();
                    } else {
                        GuiTab.this.groupSelection();
                    }
                    break;
                case 72:
                    if (!ctrl) {
                        return false;
                    }

                    GuiTab.this.snapshot();

                    for (GuiDoc.Element sx : GuiTab.this.selected()) {
                        sx.set("visible", sx.b("visible") ? "false" : "true");
                    }

                    GuiTab.this.changed();
                    break;
                case 76:
                    if (!ctrl) {
                        return false;
                    }

                    for (GuiDoc.Element sx : GuiTab.this.selected()) {
                        sx.locked = !sx.locked;
                    }

                    GuiTab.this.changed();
                    break;
                case 86:
                    if (!ctrl) {
                        return false;
                    }

                    GuiTab.this.paste();
                    break;
                case 88:
                    if (!ctrl) {
                        return false;
                    }

                    GuiTab.this.copySelected();
                    GuiTab.this.deleteSelected();
                    break;
                case 256:
                    GuiDoc.Element p = GuiTab.this.primary();
                    GuiDoc.Element parent = p != null ? this.doc.parentOf(p) : null;
                    GuiTab.this.select(parent != null ? parent.id : null);
                    break;
                case 259:
                case 261:
                    GuiTab.this.deleteSelected();
                    break;
                case 262:
                case 263:
                case 264:
                case 265:
                    List<GuiDoc.Element> roots = GuiTab.this.selectedRoots();
                    if (roots.isEmpty()) {
                        return true;
                    }

                    int step = shift ? 10 : 1;
                    GuiTab.this.snapshot();

                    for (GuiDoc.Element s : roots) {
                        if (!s.locked) {
                            int[] r = (int[])this.rect(s).clone();
                            if (key == 263) {
                                r[0] -= step;
                            }

                            if (key == 262) {
                                r[0] += step;
                            }

                            if (key == 265) {
                                r[1] -= step;
                            }

                            if (key == 264) {
                                r[1] += step;
                            }

                            GuiTab.this.applyRect(s, r);
                        }
                    }

                    this.layoutTree();
                    this.dirtyQuiet();
                    break;
                case 266:
                    GuiTab.this.move(ctrl ? 2 : 1);
                    break;
                case 267:
                    GuiTab.this.move(ctrl ? -2 : -1);
                    break;
                case 291:
                    if (e != null) {
                        GuiTab.this.renameElement(e);
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }

        @Override
        public int cursor(int mx, int my) {
            if (this.mode == 3) {
                return 221187;
            } else {
                int hd = this.handleAt(mx, my);
                if (hd == 1 || hd == 5) {
                    return 221190;
                } else if (hd == 3 || hd == 7) {
                    return 221189;
                } else if (hd >= 0) {
                    return 221187;
                } else {
                    return this.target(mx, my, false) != null ? 221188 : 221185;
                }
            }
        }
    }

    private final class Inspector extends Widget {
        private final List<GuiTab.Inspector.Row> rows = new ArrayList<>();
        private final List<Runnable> refreshers = new ArrayList<>();
        private final Map<String, TextField> fieldsByKey = new HashMap<>();

        void focus(String key) {
            TextField f = this.fieldsByKey.get(key);
            if (f != null) {
                f.requestFocus();
                f.selectAll();
            }
        }

        void rebuild() {
            this.clear();
            this.rows.clear();
            this.refreshers.clear();
            this.fieldsByKey.clear();
            GuiDoc.Element e = GuiTab.this.primary();
            if (e == null) {
                this.addRow(new GuiTab.Inspector.Header("Интерфейс"));
                String group = null;

                for (GuiSchema.Prop p : GuiSchema.docProps()) {
                    if (!p.group().equals(group)) {
                        group = p.group();
                        if (!group.equals("Интерфейс")) {
                            this.addRow(new GuiTab.Inspector.Header(group));
                        }
                    }

                    this.addRow(this.propRow(p, () -> GuiTab.this.doc.get(p.key()), v -> GuiTab.this.doc.set(p.key(), v)));
                }

                this.addRow(new GuiTab.Inspector.Header("Подсказка"));
                GuiTab.Inspector.Row hint = new GuiTab.Inspector.Row("") {
                    @Override
                    protected void paint(Canvas c, int mx, int my, float dt) {
                        c.wrapped(
                            "Клик — выбрать (в группе: двойной клик — внутрь, Esc — наружу), Shift/Ctrl — добавить, рамка — выделить несколько.\nCtrl+G — в папку, Ctrl+Shift+G — разгруппировать, Alt — без привязки.\nСюжет: gui open \""
                                + (GuiTab.this.file != null ? GuiTab.this.file.name() : "имя")
                                + "\", wait button \"id\".",
                            this.x + 8,
                            this.y + 4,
                            this.w - 16,
                            -9933435,
                            2
                        );
                    }
                };
                hint.prefH = 92;
                this.addRow(hint);
            } else {
                final GuiSchema.Type t = e.schema();
                final int count = GuiTab.this.selection.size();
                GuiTab.Inspector.Row head = new GuiTab.Inspector.Row("") {
                    @Override
                    protected void paint(Canvas c, int mx, int my, float dt) {
                        c.icon(GuiTab.iconOf(t.key()), this.x + 8, this.y + 6, GuiTab.colorOf(t.key()));
                        c.text(t.title() + (count > 1 ? "   · выбрано " + count : ""), this.x + 22, this.y + 6, -1841682);
                        c.textEllipsized(
                            count > 1 ? "Изменения применяются ко всем выбранным, у кого есть это свойство" : t.doc(),
                            this.x + 8,
                            this.y + 20,
                            this.w - 16,
                            -9933435
                        );
                    }
                };
                head.prefH = 32;
                this.addRow(head);
                TextField id = new TextField();
                id.setText(e.id);
                id.onSubmit = v -> this.commitId(e, v, id);
                this.addRow(new GuiTab.Inspector.PropRow("id", "Имя элемента для сюжета: gui text \"" + e.id + "\" \"...\". Enter — применить", id, null));
                String group = null;

                for (GuiSchema.Prop p : t.props()) {
                    if (!p.group().equals(group)) {
                        group = p.group();
                        this.addRow(new GuiTab.Inspector.Header(group));
                    }

                    String key = p.key();
                    this.addRow(this.propRow(p, () -> e.get(key), v -> {
                        for (GuiDoc.Element s : GuiTab.this.selected()) {
                            if (s.schema().prop(key) != null) {
                                s.set(key, v);
                            }
                        }
                    }));
                }
            }

            int hsum = 4;

            for (GuiTab.Inspector.Row r : this.rows) {
                hsum += r.prefH;
            }

            this.prefH = hsum + 10;
            GuiTab.this.layout();
        }

        private void commitId(GuiDoc.Element e, String v, TextField field) {
            v = v.trim();
            GuiDoc.Element other = GuiTab.this.doc.element(v);
            UI ui = UI.get();
            if (v.isEmpty() || other != null && other != e) {
                if (ui != null) {
                    ui.toastError(v.isEmpty() ? "Пустой id" : "Такой id уже есть");
                }

                field.setText(e.id);
            } else {
                GuiTab.this.snapshot();
                GuiTab.this.selection.remove(e.id);
                e.id = v;
                GuiTab.this.selection.add(v);
                GuiTab.this.changed();
            }
        }

        private void addRow(GuiTab.Inspector.Row r) {
            this.rows.add(r);
            this.add(r);
        }

        private void apply(String key, Consumer<String> setter, String value) {
            if (!key.equals(GuiTab.this.editingKey)) {
                GuiTab.this.snapshot();
                GuiTab.this.editingKey = key;
            }

            setter.accept(value);
            GuiTab.this.changed();
        }

        private GuiTab.Inspector.Row propRow(GuiSchema.Prop p, final Supplier<String> getter, final Consumer<String> setter) {
            final String key = p.key();
            switch (p.kind()) {
                case ENTITY:
                case ANIM:
                case ITEM:
                case SOUND: {
                    TextField tf = this.field(key, getter, setter);

                    ParamType pt = switch (p.kind()) {
                        case ENTITY -> ParamType.ENTITY;
                        case ANIM -> ParamType.ANIM;
                        case ITEM -> ParamType.ITEM;
                        default -> ParamType.SOUND;
                    };
                    boolean item = pt == ParamType.ITEM;
                    Button pick = Button.icon(Icon.SEARCH, "Выбрать…", () -> AssetPicker.open(pt, item ? ItemSpec.id(getter.get()) : getter.get(), v -> {
                        String next = item ? ItemSpec.with(v, ItemSpec.nbt(getter.get())) : v;
                        this.apply(key + System.nanoTime(), setter, next);
                        tf.setText(next);
                    }));
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), tf, pick);
                }
                case BOOL:
                    Checkbox cb = new Checkbox("", () -> GuiDoc.toBool(getter.get()), v -> this.apply(key + System.nanoTime(), setter, v ? "true" : "false"));
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), cb, null);
                case CHOICE:
                    Dropdown<String> dd = new Dropdown<>(p::choices, getter, s -> s, v -> this.apply(key + System.nanoTime(), setter, v));
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), dd, null);
                case COLOR: {
                    final TextField tf = this.field(key, getter, setter);
                    Widget swatch = new Widget() {
                        @Override
                        protected void paint(Canvas c, int mx, int my, float dt) {
                            c.fill(this.x + 1, this.y + 1, this.w - 2, this.h - 2, -7829368);
                            c.fill(this.x + 1, this.y + 1, (this.w - 2) / 2, (this.h - 2) / 2, -4473925);
                            c.fill(
                                this.x + 1 + (this.w - 2) / 2,
                                this.y + 1 + (this.h - 2) / 2,
                                this.w - 2 - (this.w - 2) / 2,
                                this.h - 2 - (this.h - 2) / 2,
                                -4473925
                            );
                            c.fill(this.x + 1, this.y + 1, this.w - 2, this.h - 2, GuiDoc.toColor(getter.get()));
                            c.outline(this.x, this.y, this.w, this.h, this.isHovered() ? -7639809 : -12762793);
                        }

                        @Override
                        public boolean mousePressed(double mx, double my, int button) {
                            GuiTab.this.snapshot();
                            GuiTab.this.editingKey = key + ":picker";
                            ColorPicker.open(GuiDoc.toColor(getter.get()), this.x - 150, this.y + this.h + 2, v -> {
                                setter.accept(GuiDoc.colorString(v));
                                tf.setText(getter.get());
                                GuiTab.this.changed();
                            });
                            return true;
                        }
                    };
                    swatch.tooltip("Палитра");
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), tf, swatch);
                }
                case IMAGE: {
                    TextField tf = this.field(key, getter, setter);
                    Button pick = Button.icon(Icon.IMAGE, "Картинка проекта", () -> {
                        PopupMenu pm = new PopupMenu();
                        List<String> names = AssetIndex.projectImages.get();
                        if (names.isEmpty()) {
                            pm.item("В проекте нет картинок — добавьте в «Картинки»", null).enabled(() -> false);
                        }

                        for (String n : names) {
                            pm.item(n, Icon.IMAGE, null, () -> {
                                this.apply(key + System.nanoTime(), setter, n);
                                tf.setText(n);
                            }).checked(() -> getter.get().equals(n));
                        }

                        pm.separator();
                        pm.item("Без картинки", Icon.CLOSE, null, () -> {
                            this.apply(key + System.nanoTime(), setter, "");
                            tf.setText("");
                        });
                        UI ui = UI.get();
                        pm.show(ui != null ? ui.mouseX() - 120 : 0, ui != null ? ui.mouseY() + 8 : 0);
                    });
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), tf, pick);
                }
                case ELEMENT: {
                    TextField tf = this.field(key, getter, setter);
                    Button pick = Button.icon(Icon.LIST, "Элемент этого интерфейса", () -> {
                        PopupMenu pm = new PopupMenu();

                        for (GuiDoc.Element o : GuiTab.this.doc.all()) {
                            pm.item(o.id, Icon.LIST, null, () -> {
                                this.apply(key + System.nanoTime(), setter, o.id);
                                tf.setText(o.id);
                                o.set("visible", "false");
                            }).checked(() -> getter.get().equals(o.id));
                        }

                        pm.separator();
                        pm.item("Не показывать", Icon.CLOSE, null, () -> {
                            this.apply(key + System.nanoTime(), setter, "");
                            tf.setText("");
                        });
                        UI ui = UI.get();
                        pm.show(ui != null ? ui.mouseX() - 120 : 0, ui != null ? ui.mouseY() + 8 : 0);
                    });
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), tf, pick);
                }
                case VIDEO: {
                    TextField tf = this.field(key, getter, setter);
                    Button pick = Button.icon(Icon.PLAY, "Видео проекта", () -> {
                        PopupMenu pm = new PopupMenu();
                        List<Project.VideoRef> videos = GuiTab.this.project.videos();
                        if (videos.isEmpty()) {
                            pm.item("В проекте нет видео — добавьте в «Видео»", null).enabled(() -> false);
                        }

                        for (Project.VideoRef v : videos) {
                            pm.item(v.name(), Icon.PLAY, null, () -> {
                                this.apply(key + System.nanoTime(), setter, v.name());
                                tf.setText(v.name());
                            }).checked(() -> getter.get().equals(v.name()));
                        }

                        UI ui = UI.get();
                        pm.show(ui != null ? ui.mouseX() - 120 : 0, ui != null ? ui.mouseY() + 8 : 0);
                    });
                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), tf, pick);
                }
                default: {
                    TextField tf = this.field(key, getter, setter);
                    if (TextValues.takes(key)) {
                        TextValues.attach(tf, GuiTab.this.project, null, key);
                    }

                    return new GuiTab.Inspector.PropRow(p.label(), p.doc(), tf, null);
                }
            }
        }

        private TextField field(String key, Supplier<String> getter, Consumer<String> setter) {
            TextField tf = new TextField();
            tf.setText(getter.get());
            tf.onChange = v -> this.apply(key, setter, v);
            this.fieldsByKey.put(key, tf);
            this.refreshers.add(() -> {
                if (!tf.isFocused() && !tf.text().equals(getter.get())) {
                    tf.setText(getter.get());
                }
            });
            return tf;
        }

        @Override
        public void tick() {
            super.tick();

            for (Runnable r : this.refreshers) {
                r.run();
            }
        }

        @Override
        public void layout() {
            int yy = this.y + 2;

            for (GuiTab.Inspector.Row r : this.rows) {
                r.bounds(this.x, yy, this.w, r.prefH);
                r.layout();
                yy += r.prefH;
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, Math.max(this.h, this.prefH), -14934490);
        }

        final class Header extends GuiTab.Inspector.Row {
            Header(String text) {
                super(text);
                this.prefH = 22;
            }

            @Override
            protected void paint(Canvas c, int mx, int my, float dt) {
                c.fill(this.x, this.y + 4, this.w, 1, -13749952);
                c.text(this.label.toUpperCase(), this.x + 8, this.y + 9, -9933435);
            }
        }

        final class PropRow extends GuiTab.Inspector.Row {
            final Widget editor;
            final Widget extra;

            PropRow(String label, String doc, Widget editor, Widget extra) {
                super(label);
                this.editor = this.add(editor);
                this.extra = extra != null ? this.add(extra) : null;
                this.tooltip(doc != null && !doc.isEmpty() ? doc : null);
            }

            @Override
            public void layout() {
                int lw = Math.min(92, this.w * 42 / 100);
                int ex = this.extra != null ? 20 : 0;
                this.editor.bounds(this.x + lw, this.y + 1, this.w - lw - 6 - ex, 18);
                if (this.extra != null) {
                    this.extra.bounds(this.x + this.w - 6 - 18, this.y + 1, 18, 18);
                }

                this.editor.layout();
            }

            @Override
            protected void paint(Canvas c, int mx, int my, float dt) {
                int lw = Math.min(92, this.w * 42 / 100);
                c.textEllipsized(this.label, this.x + 8, this.y + 6, lw - 10, -6577995);
            }
        }

        abstract class Row extends Widget {
            final String label;

            Row(String label) {
                this.label = label;
                this.prefH = 20;
            }
        }
    }

    private final class LayerTree extends Widget implements UI.CursorProvider {
        private static final int ROW = 17;
        private final List<GuiTab.LayerTree.Row> rows = new ArrayList<>();
        private final Set<String> collapsed = new HashSet<>();
        private double scroll;
        private double target;
        private GuiDoc.Element pressEl;
        private GuiDoc.Element dragEl;
        private double pressY;
        private int dropRow = -1;
        private int dropMode;

        LayerTree() {
            this.focusable = true;
        }

        void rebuild() {
            this.rows.clear();
            this.flatten(GuiTab.this.doc.elements, 0);
        }

        private void flatten(List<GuiDoc.Element> list, int depth) {
            for (int i = list.size() - 1; i >= 0; i--) {
                GuiDoc.Element e = list.get(i);
                this.rows.add(new GuiTab.LayerTree.Row(e, depth));
                if (e.isContainer() && !this.collapsed.contains(e.id)) {
                    this.flatten(e.children, depth + 1);
                }
            }
        }

        void reveal(GuiDoc.Element e) {
            if (e != null) {
                boolean changedTree = false;

                for (GuiDoc.Element p = GuiTab.this.doc.parentOf(e); p != null; p = GuiTab.this.doc.parentOf(p)) {
                    changedTree |= this.collapsed.remove(p.id);
                }

                if (changedTree) {
                    this.rebuild();
                }

                for (int i = 0; i < this.rows.size(); i++) {
                    if (this.rows.get(i).e() == e) {
                        int top = i * 17;
                        int view = this.h;
                        if (top < this.target) {
                            this.target = top;
                        } else if (top + 17 > this.target + view) {
                            this.target = top + 17 - view;
                        }
                    }
                }
            }
        }

        private int maxScroll() {
            return Math.max(0, this.rows.size() * 17 - this.h + 4);
        }

        private int rowAt(double my) {
            int i = (int)Math.floor((my - this.y + this.scroll) / 17.0);
            return i >= 0 && i < this.rows.size() ? i : -1;
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            this.target = Math.max(0.0, Math.min((double)this.maxScroll(), this.target));
            this.scroll = this.scroll + (this.target - this.scroll) * Math.min(1.0F, dt * 18.0F);
            if (Math.abs(this.target - this.scroll) < 0.5) {
                this.scroll = this.target;
            }

            c.fill(this.x, this.y, this.w, this.h, -14934490);
            c.pushClip(this.x, this.y, this.w, this.h);
            int first = Math.max(0, (int)(this.scroll / 17.0));
            int last = Math.min(this.rows.size(), first + this.h / 17 + 2);
            int hoverRow = this.isHovered() ? this.rowAt(my) : -1;

            for (int i = first; i < last; i++) {
                GuiTab.LayerTree.Row r = this.rows.get(i);
                GuiDoc.Element e = r.e();
                int ry = this.y + i * 17 - (int)this.scroll;
                boolean sel = GuiTab.this.selection.contains(e.id);
                if (sel) {
                    c.fill(this.x, ry, this.w, 17, e == GuiTab.this.primary() ? 1430088664 : 859663320);
                } else if (i == hoverRow) {
                    c.fill(this.x, ry, this.w, 17, 352321535);
                }

                int ix = this.x + 6 + r.depth() * 10;

                for (int d = 0; d < r.depth(); d++) {
                    c.fill(this.x + 10 + d * 10, ry, 1, 17, 352321535);
                }

                if (e.isContainer()) {
                    c.icon(this.collapsed.contains(e.id) ? Icon.CHEVRON_RIGHT : Icon.CHEVRON_DOWN, ix, ry + 4, -9933435);
                }

                ix += 10;
                boolean vis = e.b("visible");
                c.icon(GuiTab.iconOf(e.type), ix, ry + 4, vis ? GuiTab.colorOf(e.type) : -9933435);
                int tx = ix + 13;
                c.textEllipsized(e.id, tx, ry + 5, this.w - (tx - this.x) - 32, vis ? (sel ? -1841682 : -6577995) : -9933435);
                boolean rowHover = i == hoverRow;
                if (!vis || rowHover) {
                    int ex = this.x + this.w - 28;
                    c.round(ex, ry + 6, 9, 5, 2, vis ? -1711276033 : 1157627903);
                    c.fill(ex + 4, ry + 7, 2, 3, vis ? -14934490 : 1157627903);
                    if (!vis) {
                        c.fill(ex - 1, ry + 8, 11, 1, -1023878);
                    }
                }

                if (e.locked || rowHover) {
                    c.icon(Icon.FLAG, this.x + this.w - 14, ry + 4, e.locked ? -870325 : 1442840575);
                }
            }

            if (this.dragEl != null && this.dropRow >= 0 && this.dropRow < this.rows.size()) {
                GuiTab.LayerTree.Row rx = this.rows.get(this.dropRow);
                int ryx = this.y + this.dropRow * 17 - (int)this.scroll;
                int ix = this.x + 6 + rx.depth() * 10;
                if (this.dropMode == 2) {
                    c.outline(this.x + 1, ryx, this.w - 2, 17, -7639809);
                } else {
                    c.fill(ix, this.dropMode == 0 ? ryx : ryx + 17 - 1, this.w - (ix - this.x) - 4, 2, -7639809);
                }
            }

            c.popClip();
            if (this.rows.isEmpty()) {
                c.textCentered("Нет элементов", this.x + this.w / 2, this.y + 10, -9933435);
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            this.requestFocus();
            int i = this.rowAt(my);
            if (i < 0) {
                if (button == 0) {
                    GuiTab.this.select(null);
                }

                return true;
            } else {
                GuiTab.LayerTree.Row r = this.rows.get(i);
                GuiDoc.Element e = r.e();
                if (button == 1) {
                    GuiTab.this.elementMenu(e, (int)mx, (int)my);
                    return true;
                } else {
                    int ix = this.x + 6 + r.depth() * 10;
                    if (e.isContainer() && mx >= ix - 2 && mx < ix + 10) {
                        if (!this.collapsed.remove(e.id)) {
                            this.collapsed.add(e.id);
                        }

                        this.rebuild();
                        return true;
                    } else if (mx >= this.x + this.w - 30 && mx < this.x + this.w - 17) {
                        GuiTab.this.toggleVisible(e);
                        return true;
                    } else if (mx >= this.x + this.w - 16) {
                        e.locked = !e.locked;
                        GuiTab.this.changed();
                        return true;
                    } else {
                        if (GuiTab.shiftDown() || GuiTab.ctrlDown()) {
                            GuiTab.this.toggleSelect(e.id);
                        } else if (!GuiTab.this.selection.contains(e.id)) {
                            GuiTab.this.select(e.id);
                        }

                        this.pressEl = e;
                        this.pressY = my;
                        return true;
                    }
                }
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.pressEl == null) {
                return true;
            } else {
                if (this.dragEl == null && Math.abs(my - this.pressY) > 3.0) {
                    this.dragEl = this.pressEl;
                }

                if (this.dragEl == null) {
                    return true;
                } else {
                    int i = this.rowAt(my);
                    if (my < this.y) {
                        this.target -= 4.0;
                    }

                    if (my > this.y + this.h) {
                        this.target += 4.0;
                    }

                    if (i < 0) {
                        this.dropRow = this.rows.isEmpty() ? -1 : this.rows.size() - 1;
                        this.dropMode = 1;
                        return true;
                    } else {
                        GuiTab.LayerTree.Row r = this.rows.get(i);
                        double local = (my - (this.y + i * 17 - this.scroll)) / 17.0;
                        this.dropRow = i;
                        if (r.e().isContainer() && local > 0.28 && local < 0.72) {
                            this.dropMode = 2;
                        } else {
                            this.dropMode = local < 0.5 ? 0 : 1;
                        }

                        if (GuiDoc.contains(this.dragEl, r.e())) {
                            this.dropRow = -1;
                        }

                        return true;
                    }
                }
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (this.dragEl != null && this.dropRow >= 0 && this.dropRow < this.rows.size()) {
                GuiDoc.Element target = this.rows.get(this.dropRow).e();
                GuiTab.this.snapshot();

                for (GuiDoc.Element m : GuiTab.this.selection.contains(this.dragEl.id) ? GuiTab.this.selectedRoots() : List.of(this.dragEl)) {
                    if (!GuiDoc.contains(m, target)) {
                        if (this.dropMode == 2) {
                            GuiTab.this.reparent(m, target.children, target.children.size());
                            this.collapsed.remove(target.id);
                        } else {
                            List<GuiDoc.Element> list = GuiTab.this.doc.listOf(target);
                            if (list != null) {
                                int idx = list.indexOf(target) + (this.dropMode == 0 ? 1 : 0);
                                GuiTab.this.reparent(m, list, idx);
                            }
                        }
                    }
                }

                GuiTab.this.changed();
            }

            this.pressEl = null;
            this.dragEl = null;
            this.dropRow = -1;
            return true;
        }

        @Override
        public boolean mouseDoubleClicked(double mx, double my, int button) {
            int i = this.rowAt(my);
            if (i >= 0 && mx < this.x + this.w - 30) {
                GuiTab.this.renameElement(this.rows.get(i).e());
            }

            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            this.target = Math.max(0.0, Math.min((double)this.maxScroll(), this.target - delta * 17.0 * 2.0));
            return true;
        }

        @Override
        public boolean keyPressed(int key, int scan, int mods) {
            return GuiTab.this.canvas.keyPressed(key, scan, mods);
        }

        @Override
        public int cursor(int mx, int my) {
            return this.dragEl != null ? 221187 : 221188;
        }

        private record Row(GuiDoc.Element e, int depth) {
        }
    }
}
