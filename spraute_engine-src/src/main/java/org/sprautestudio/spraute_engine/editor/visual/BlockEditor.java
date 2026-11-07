package org.sprautestudio.spraute_engine.editor.visual;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.sprautestudio.spraute_engine.build.BuildStyles;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.assets.HeadEditor;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.gui.PanelLook;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.post.PostParams;
import org.sprautestudio.spraute_engine.runtime.Colors;
import org.sprautestudio.spraute_engine.stealth.StealthShape;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.ColorPicker;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class BlockEditor extends Widget implements UI.CursorProvider {
    static final int ROW = 24;
    static final int HAT = 10;
    static final int PAD = 8;
    static final int GAP = 5;
    static final int PILL_H = 15;
    static final int ARM = 14;
    static final int SLOT_MIN = 18;
    static final int MID = 20;
    static final int BOTTOM = 14;
    static final int NOTCH_X = 12;
    static final int NOTCH_W = 14;
    static final int NOTCH_D = 3;
    static final int PALETTE_W = 210;
    static final int CAT_W = 50;
    static final int CAT_STEP = 40;
    static final int ARROW_W = 9;
    static final int SOCKET_W = 26;
    private static final int SNAP_DIST = 24;
    private VisualScript script;
    public Consumer<BlockEditor> onChange;
    public Function<VisualScript, BlockCodegen.Result> generator = BlockCodegen::generate;
    public Function<String, List<String>> outsideNames;
    public Supplier<String> positionProvider;
    public BlockEditor.AssetPicker assetPicker;
    private double viewX;
    private double viewY;
    private double zoom = 1.0;
    private final List<String> undo = new ArrayList<>();
    private final List<String> redo = new ArrayList<>();
    private VisualScript.Block selected;
    private final TextField search = new TextField().placeholder("Поиск блоков");
    private final List<BlockEditor.PaletteEntry> palette = new ArrayList<>();
    private double paletteScroll;
    private double paletteTarget;
    private int paletteHeight;
    private String activeCategory = BlockTypes.CATEGORIES.get(0);
    private BlockEditor.Mode mode = BlockEditor.Mode.NONE;
    private double pressX;
    private double pressY;
    private BlockEditor.Hit pressHit;
    private VisualScript.Block pressPaletteBlock;
    private VisualScript.Stack dragStack;
    private double dragOffX;
    private double dragOffY;
    private BlockEditor.Snap snap;
    private BlockCodegen.Result generated;
    private long lastChange;
    private boolean generatedDirty = true;
    private VisualScript.Block pressBlock;
    private BlockEditor.ValueHit pressValue;
    private BlockEditor.SocketTarget socketTarget;
    private double catScroll;
    private static final int ADD_W = 14;
    private static final int WRAP_W = 250;
    private VisualScript.Block clipboard;
    private VisualScript.Block lastClickBlock;
    private long lastClickAt;
    private VisualScript.Block flash;
    private long flashUntil;

    public BlockEditor(VisualScript script) {
        this.script = script;
        this.focusable = true;
        this.viewX = script.viewX;
        this.viewY = script.viewY;
        this.zoom = script.zoom;
        this.add(this.search);
        this.search.onChange = s -> this.buildPalette();
        this.buildPalette();
    }

    public VisualScript script() {
        this.script.viewX = this.viewX;
        this.script.viewY = this.viewY;
        this.script.zoom = this.zoom;
        return this.script;
    }

    public void appendDialogue(List<VisualScript.Block> blocks) {
        VisualScript.Stack main = this.script.mainStack();
        if (main == null) {
            throw new IllegalStateException("Нет начального блока сюжета");
        } else {
            this.snapshot();

            for (VisualScript.Block block : blocks) {
                main.blocks.add(block.deepCopy());
            }

            this.changed();
        }
    }

    public void edit(Runnable change) {
        this.snapshot();
        change.run();
        this.changed();
    }

    public BlockCodegen.Result generated() {
        if (this.generatedDirty || this.generated == null) {
            this.generated = this.generator.apply(this.script);
            this.generatedDirty = false;
        }

        return this.generated;
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    private void snapshot() {
        this.undo.add(this.script().toJson());
        if (this.undo.size() > 100) {
            this.undo.remove(0);
        }

        this.redo.clear();
    }

    private void changed() {
        for (VisualScript.Stack s : this.script.stacks) {
            s.dirty = true;
        }

        this.generatedDirty = true;
        this.lastChange = System.currentTimeMillis();
        if (this.onChange != null) {
            this.onChange.accept(this);
        }
    }

    public void undo() {
        if (!this.undo.isEmpty()) {
            this.redo.add(this.script().toJson());
            this.restore(this.undo.remove(this.undo.size() - 1));
        }
    }

    public void redo() {
        if (!this.redo.isEmpty()) {
            this.undo.add(this.script().toJson());
            this.restore(this.redo.remove(this.redo.size() - 1));
        }
    }

    private void restore(String json) {
        this.script = VisualScript.fromJson(json);
        this.selected = null;
        this.changed();
    }

    private int textW(String s) {
        return this.font().width(s);
    }

    private static String shown(VisualScript.Block b, BlockTypes.Field f) {
        String v = b.field(f.name()).trim();
        if (v.isEmpty()) {
            return f.label();
        } else if (f.kind() == ParamType.SYSTEM) {
            return Specs.systemTitle(v);
        } else {
            if (f.kind() == ParamType.TASK) {
                String line = AssetIndex.questTasks(b.field("name").replace("\"", "").trim()).get(v.replace("\"", ""));
                if (line != null && !line.isBlank()) {
                    return line.length() > 28 ? line.substring(0, 27) + "…" : line;
                }
            }

            if (f.kind() == ParamType.SPEAKER && v.equals("player")) {
                return "игрок";
            } else if (f.kind() == ParamType.EASE) {
                return Ease.TITLES.getOrDefault(v, v);
            } else {
                if (f.kind() == ParamType.TARGET) {
                    if (v.equals("player")) {
                        return "игрок";
                    }

                    if (v.equals("\"mob\"")) {
                        return "ближайший моб";
                    }

                    if (v.startsWith("\"")) {
                        return "моб " + v.replace("\"", "").replace("minecraft:", "");
                    }
                }

                return v;
            }
        }
    }

    private static ChatHeads.Params headParams(VisualScript.Block b) {
        return new ChatHeads.Params(
            num(b.field("headscale"), 1.0F),
            (int)num(b.field("headx"), 0.0F),
            (int)num(b.field("heady"), 0.0F),
            (int)num(b.field("headyaw"), 0.0F),
            b.field("headbone").isBlank() ? "auto" : b.field("headbone").replace("\"", "")
        );
    }

    private static float num(String s, float def) {
        try {
            return s != null && !s.isBlank() ? Float.parseFloat(s.trim()) : def;
        } catch (NumberFormatException var3) {
            return def;
        }
    }

    private static boolean isDropdown(BlockTypes.Field f) {
        if (f.kind() == null) {
            return false;
        } else {
            return switch (f.kind()) {
                case CHAR, LABEL, EMOTION, KEY, BOOL, TARGET, SPEAKER, LAYER, MOVETYPE, ANIM, ENTITY, SOUND, ITEM, FUNC, BRANCH, IMAGE, GUI, GUIEL, VAR, CHOICE, SYSTEM, EASE, CUTSCENE, VFX, POST, POSTPARAM, CAMRIG, CAMPARAM, STRUCTURE, BUILDSTYLE, STEALTHSHAPE, STEALTH, SKILLTREE, SKILLNODE, SKILLMENU, PANELTURN, PANELLOOK, SLOTORDER, PLAYERCAN, PLAYERSTAT, PLAYERRULE, SHOP, QUEST, TASK, ZONE, FIGHT, PATH -> true;
                default -> false;
            };
        }
    }

    private int pillW(VisualScript.Block b, BlockTypes.Field f) {
        return Math.max(isDropdown(f) ? 30 : 22, this.textW(shown(b, f)) + (isDropdown(f) ? 20 : 12));
    }

    private static boolean optionRows(VisualScript.Block b) {
        return b.type.equals("choice") || b.type.equals("switch");
    }

    private static boolean canAddAnswer(VisualScript.Block b) {
        return b.type.equals("choice") && b.options.size() < 4;
    }

    private void addAnswer(VisualScript.Block b) {
        this.snapshot();
        b.options.add("Вариант " + (b.options.size() + 1));
        b.slots.add(b.options.size() - 1, new ArrayList<>());
        this.changed();
    }

    private static String optionLabel(VisualScript.Block b) {
        return b.type.equals("switch") ? "если =" : "ответ";
    }

    private static int arrowW(BlockTypes.Field f) {
        return f.acceptsValue() ? 9 : 0;
    }

    private static boolean shown(VisualScript.Block b, BlockTypes.Part p) {
        if (p.field() == null) {
            return true;
        } else {
            BlockTypes.Variant v = BlockTypes.variant(b.type);
            return v != null && !p.field().name().equals(v.field()) ? v.template(b.field(v.field()).trim()).contains("%" + p.field().name()) : true;
        }
    }

    private int fieldW(VisualScript.Block b, BlockTypes.Field f) {
        VisualScript.Block in = b.inputs.get(f.name());
        if (in != null) {
            return in.lw;
        } else {
            return b.sockets.contains(f.name()) ? 26 : this.pillW(b, f);
        }
    }

    private int layoutList(List<VisualScript.Block> list, int x, int y) {
        int yy = y;

        for (VisualScript.Block b : list) {
            this.layoutBlock(b, x, yy);
            yy += b.lh;
        }

        return yy - y;
    }

    private String collapsedLabel(VisualScript.Block b) {
        String head = "";

        for (BlockTypes.Part p : b.typeInfo().parts()) {
            if (p.field() == null) {
                head = p.text().trim();
                break;
            }
        }

        int inside = 0;

        for (List<VisualScript.Block> slot : b.slots) {
            inside += countBlocks(slot);
        }

        return "▸ " + head + " …" + (inside > 0 ? "  (" + inside + " " + plural(inside) + ")" : "");
    }

    private static int countBlocks(List<VisualScript.Block> list) {
        int n = 0;

        for (VisualScript.Block b : list) {
            n++;

            for (List<VisualScript.Block> slot : b.slots) {
                n += countBlocks(slot);
            }
        }

        return n;
    }

    private static String plural(int n) {
        int m = n % 100;
        if (m >= 11 && m <= 14) {
            return "блоков";
        } else {
            return switch (n % 10) {
                case 1 -> "блок";
                case 2, 3, 4 -> "блока";
                default -> "блоков";
            };
        }
    }

    private int partWidth(VisualScript.Block b, BlockTypes.Part p) {
        if (p.field() == null) {
            return this.textW(p.text());
        } else {
            VisualScript.Block in = b.inputs.get(p.field().name());
            if (in != null) {
                this.layoutBlock(in, 0, 0);
            }

            return arrowW(p.field()) + this.fieldW(b, p.field());
        }
    }

    private void layoutBlock(VisualScript.Block b, int x, int y) {
        BlockTypes.Type t = b.typeInfo();
        boolean value = t.isValue();
        List<BlockTypes.Part> parts = t.parts();
        int n = parts.size();
        b.partX = new int[n];
        b.partW = new int[n];
        b.partY = new int[n];
        b.partRH = new int[n];
        int[] rowOf = new int[n];
        boolean folded = b.collapsed && !value;
        boolean wrap = !b.oneLine && !value && !folded;
        int pad = value ? (t.shape() == BlockTypes.Shape.BOOLEAN ? 10 : 7) : 8;
        int gap = value ? 4 : 5;
        int cx = pad;
        int row = 0;
        int maxX = pad;
        List<Integer> rowInner = new ArrayList<>();
        rowInner.add(15);

        for (int i = 0; i < n; i++) {
            BlockTypes.Part p = parts.get(i);
            rowOf[i] = row;
            if (!folded && shown(b, p)) {
                int pw = this.partWidth(b, p);
                if (wrap && p.field() == null && cx > pad) {
                    int look = pw + gap;
                    if (i + 1 < n && parts.get(i + 1).field() != null && shown(b, parts.get(i + 1))) {
                        look += this.partWidth(b, parts.get(i + 1));
                    }

                    if (cx + look > 250) {
                        row++;
                        rowInner.add(15);
                        cx = pad;
                        rowOf[i] = row;
                    }
                }

                if (wrap && p.field() != null && cx > pad && parts.get(i - 1).field() != null && cx + pw > 250) {
                    row++;
                    rowInner.add(15);
                    cx = pad;
                    rowOf[i] = row;
                }

                if (p.field() != null) {
                    VisualScript.Block in = b.inputs.get(p.field().name());
                    if (in != null) {
                        rowInner.set(row, Math.max(rowInner.get(row), in.lh));
                    }
                }

                b.partX[i] = cx;
                b.partW[i] = pw;
                cx += pw + gap;
                maxX = Math.max(maxX, cx);
            } else {
                b.partX[i] = cx;
                b.partW[i] = 0;
            }
        }

        b.lx = x;
        b.ly = y;
        if (value) {
            b.lh = Math.max(19, rowInner.get(0) + 4);
            b.headerH = b.lh;
            b.lw = Math.max(24, cx - 4 + pad);

            for (int ix = 0; ix < n; ix++) {
                b.partY[ix] = 0;
                b.partRH[ix] = b.lh;
            }
        } else {
            int hat = t.shape() == BlockTypes.Shape.HAT ? 10 : 0;
            int[] top = new int[rowInner.size()];
            int[] rh = new int[rowInner.size()];
            int yy0 = hat;

            for (int r = 0; r < rowInner.size(); r++) {
                rh[r] = Math.max(r == 0 ? 24 : 20, rowInner.get(r) + (r == 0 ? 9 : 5));
                top[r] = yy0;
                yy0 += rh[r];
            }

            b.headerH = yy0 + (rowInner.size() > 1 ? 3 : 0);
            if (rowInner.size() > 1) {
                rh[rh.length - 1] = rh[rh.length - 1] + 3;
            }

            for (int ix = 0; ix < n; ix++) {
                b.partY[ix] = top[rowOf[ix]];
                b.partRH[ix] = rh[rowOf[ix]];
            }

            int minW = t.shape() == BlockTypes.Shape.C && !folded ? 120 : 60;
            b.lw = folded ? Math.max(minW, 8 + this.textW(this.collapsedLabel(b)) + 8) : Math.max(minW, maxX - 5 + 8);
        }

        for (int ix = 0; ix < n; ix++) {
            BlockTypes.Field f = parts.get(ix).field();
            if (f != null && b.partW[ix] != 0) {
                VisualScript.Block in = b.inputs.get(f.name());
                if (in != null) {
                    this.layoutBlock(in, x + b.partX[ix] + arrowW(f), y + b.partY[ix] + (b.partRH[ix] - in.lh) / 2);
                }
            }
        }

        if (!value) {
            int yy = y + b.headerH;
            if (b.collapsed) {
                b.slotTop = null;
                b.slotH = null;
            }

            if (t.shape() == BlockTypes.Shape.C && !b.collapsed) {
                int ns = b.slots.size();
                b.slotTop = new int[ns];
                b.slotH = new int[ns];
                boolean options = optionRows(b);

                for (int ixx = 0; ixx < ns; ixx++) {
                    if (options || ixx > 0) {
                        if (options && ixx < b.options.size()) {
                            int more = ixx == b.options.size() - 1 && canAddAnswer(b) ? 19 : 0;
                            b.lw = Math.max(b.lw, 8 + this.textW(optionLabel(b)) + 5 + Math.max(22, this.textW(b.options.get(ixx)) + 12) + more + 8);
                        }

                        yy += 20;
                    }

                    b.slotTop[ixx] = yy;
                    int hgt = this.layoutList(b.slots.get(ixx), x + 14, yy);
                    b.slotH[ixx] = Math.max(18, hgt);
                    yy += b.slotH[ixx];
                }

                yy += 14;
            }

            b.lh = yy - y;
        }
    }

    private void layoutStack(VisualScript.Stack s) {
        if (s.dirty) {
            int hgt = this.layoutList(s.blocks, (int)s.x, (int)s.y);
            s.bh = hgt;
            s.bw = 0;

            for (VisualScript.Block b : s.blocks) {
                s.bw = Math.max(s.bw, this.extentRight(b) - (int)s.x);
            }

            s.dirty = false;
        }
    }

    private int extentRight(VisualScript.Block b) {
        int r = b.lx + b.lw;
        if (!b.collapsed) {
            for (List<VisualScript.Block> slot : b.slots) {
                for (VisualScript.Block c : slot) {
                    r = Math.max(r, this.extentRight(c));
                }
            }
        }

        return r;
    }

    private int canvasX() {
        return this.x + 210;
    }

    private int canvasW() {
        return this.w - 210;
    }

    private double toWorldX(double mx) {
        return (mx - this.canvasX()) / this.zoom + this.viewX;
    }

    private double toWorldY(double my) {
        return (my - this.y) / this.zoom + this.viewY;
    }

    private int toScreenX(double wx) {
        return (int)Math.round(this.canvasX() + (wx - this.viewX) * this.zoom);
    }

    private int toScreenY(double wy) {
        return (int)Math.round(this.y + (wy - this.viewY) * this.zoom);
    }

    @Override
    public void layout() {
        this.search.bounds(this.x + 50 + 4, this.y + 4, 152, 16);
    }

    @Override
    public int cursor(int mx, int my) {
        if (this.mode == BlockEditor.Mode.PAN) {
            return 221187;
        } else if (this.mode == BlockEditor.Mode.DRAG) {
            return 221188;
        } else {
            return mx >= this.canvasX() ? 221185 : 221188;
        }
    }

    private void buildPalette() {
        this.palette.clear();
        String q = this.search.text().trim().toLowerCase();
        int yy = 0;

        for (String cat : BlockTypes.CATEGORIES) {
            List<BlockTypes.Type> types = BlockTypes.inCategory(cat);
            List<BlockTypes.Type> shownTypes = new ArrayList<>();

            for (BlockTypes.Type t : types) {
                if (q.isEmpty() || searchText(t).contains(q)) {
                    shownTypes.add(t);
                }
            }

            if (!shownTypes.isEmpty()) {
                this.palette.add(new BlockEditor.PaletteEntry(null, cat, yy, true));
                yy += 20;

                for (BlockTypes.Type tx : shownTypes) {
                    VisualScript.Block b = VisualScript.Block.create(tx.key());
                    this.layoutBlock(b, 0, 0);
                    this.palette.add(new BlockEditor.PaletteEntry(b, cat, yy, false));
                    yy += b.lh + (tx.shape() == BlockTypes.Shape.C ? 8 : 6);
                }

                yy += 6;
            }
        }

        this.paletteHeight = yy;
        this.paletteTarget = this.paletteScroll = 0.0;
    }

    private static String searchText(BlockTypes.Type t) {
        StringBuilder sb = new StringBuilder(t.key()).append(' ');

        for (BlockTypes.Part p : t.parts()) {
            if (p.text() != null) {
                sb.append(p.text()).append(' ');
            }
        }

        return sb.toString().toLowerCase();
    }

    private int paletteTop() {
        return this.y + 24;
    }

    private void scrollPaletteTo(String category) {
        for (BlockEditor.PaletteEntry e : this.palette) {
            if (e.header && e.category.equals(category)) {
                this.paletteTarget = Math.max(0, Math.min(this.maxPaletteScroll(), e.y));
                return;
            }
        }
    }

    private int maxPaletteScroll() {
        return Math.max(0, this.paletteHeight - (this.h - 24) + 20);
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (Math.abs(this.paletteTarget - this.paletteScroll) > 0.5) {
            this.paletteScroll = this.paletteScroll + (this.paletteTarget - this.paletteScroll) * Math.min(1.0F, dt * 16.0F);
        } else {
            this.paletteScroll = this.paletteTarget;
        }

        if (this.generatedDirty && System.currentTimeMillis() - this.lastChange > 250L) {
            this.generated();
        }

        this.paintCanvas(c, mx, my);
        this.paintPalette(c, mx, my);
    }

    private void paintCanvas(Canvas c, int mx, int my) {
        int cx = this.canvasX();
        int cw = this.canvasW();
        c.fill(cx, this.y, cw, this.h, -15461093);
        double step = 24.0 * this.zoom;
        if (step >= 8.0) {
            double ox = -(this.viewX * this.zoom % step);
            double oy = -(this.viewY * this.zoom % step);

            for (double gx = ox; gx < cw; gx += step) {
                for (double gy = oy; gy < this.h; gy += step) {
                    c.fill(cx + (int)gx, this.y + (int)gy, 1, 1, 587202559);
                }
            }
        }

        c.pushClip(cx, this.y, cw, this.h);
        c.pushTransform(cx - (float)(this.viewX * this.zoom), this.y - (float)(this.viewY * this.zoom), (float)this.zoom);
        double vx0 = this.viewX;
        double vy0 = this.viewY;
        double vx1 = this.viewX + cw / this.zoom;
        double vy1 = this.viewY + this.h / this.zoom;
        VisualScript.Stack main = this.script.mainStack();

        for (VisualScript.Stack s : this.script.stacks) {
            this.layoutStack(s);
            if (!(s.x > vx1) && !(s.y > vy1) && !(s.x + s.bw < vx0) && !(s.y + s.bh + 3.0 < vy0)) {
                boolean dim = s != main && (s.blocks.isEmpty() || !s.blocks.get(0).type.equals("func"));
                this.paintList(c, s.blocks, vy0, vy1, dim);
            }
        }

        if (this.snap != null && this.dragStack != null) {
            int sw = Math.max(60, this.dragStack.bw);
            c.round(this.snap.x, this.snap.y - 2, sw, 4, 2, -855638017);
            c.round(this.snap.x, this.snap.y + 2, sw, Math.min(this.dragStack.bh, 60), 4, 587202559);
        }

        if (this.socketTarget != null && this.dragStack != null) {
            BlockEditor.SocketTarget st = this.socketTarget;
            c.outline(st.x() - 2, st.y() - 2, st.w() + 4, st.h() + 4, -1);
            c.outline(st.x() - 1, st.y() - 1, st.w() + 2, st.h() + 2, -1996488705);
        }

        if (this.dragStack != null) {
            this.layoutStack(this.dragStack);
            this.paintShadow(c, this.dragStack);
            this.paintList(c, this.dragStack.blocks, -1.0E9, 1.0E9, false);
        }

        this.paintFlash(c);
        c.popTransform();
        c.popClip();
        String zoomText = Math.round(this.zoom * 100.0) + "%";
        c.text(zoomText, cx + cw - c.width(zoomText) - 8, this.y + this.h - 14, -9933435);
        if (main == null) {
            c.textCentered("Перетащите блок «когда начинается сюжет» из категории «Поток»", cx + cw / 2, this.y + 12, -870325);
        }

        if (this.dragStack != null && mx < cx) {
            c.fill(this.x, this.y, 210, this.h, 871391354);
        }
    }

    private void paintShadow(Canvas c, VisualScript.Stack s) {
        c.round(s.blocks.get(0).lx + 4, s.blocks.get(0).ly + 6, Math.max(40, s.bw), s.bh, 4, 1140850688);
    }

    private void paintList(Canvas c, List<VisualScript.Block> list, double top, double bottom, boolean dim) {
        for (VisualScript.Block b : list) {
            if (b.ly > bottom) {
                break;
            }

            if (!(b.ly + b.lh + 3 < top)) {
                this.paintBlock(c, b, top, bottom, dim);
            }
        }
    }

    private static void hexagon(Canvas c, int x, int y, int w, int h, int color) {
        float half = h / 2.0F;

        for (int i = 0; i < h; i++) {
            int inset = Math.round(Math.abs(i + 0.5F - half) * Math.min(half, w / 4.0F) / half);
            c.fill(x + inset, y + i, w - 2 * inset, 1, color);
        }
    }

    private void paintValue(Canvas c, VisualScript.Block b, boolean dim) {
        BlockTypes.Type t = b.typeInfo();
        int color = Theme.category(t.category());
        if (dim) {
            color = Theme.lerp(color, -12960435, 0.45F);
        }

        int dark = Theme.darker(color, 0.72F);
        if (t.shape() == BlockTypes.Shape.BOOLEAN) {
            hexagon(c, b.lx, b.ly + 1, b.lw, b.lh, dark);
            hexagon(c, b.lx, b.ly, b.lw, b.lh, color);
        } else {
            c.round(b.lx, b.ly + 1, b.lw, b.lh, b.lh / 2, dark);
            c.round(b.lx, b.ly, b.lw, b.lh, b.lh / 2, color);
        }

        this.paintParts(c, b, b.ly, b.lh, dark, dim);
        if (b == this.selected) {
            c.outline(b.lx - 1, b.ly - 1, b.lw + 2, b.lh + 2, -855638017);
        }

        if (b.error) {
            c.outline(b.lx - 1, b.ly - 1, b.lw + 2, b.lh + 2, -1023878);
        }
    }

    private void paintParts(Canvas c, VisualScript.Block b, int hy, int hh, int dark, boolean dim) {
        List<BlockTypes.Part> parts = b.typeInfo().parts();
        int hy0 = hy;
        int hh0 = hh;

        for (int i = 0; i < parts.size(); i++) {
            BlockTypes.Part p = parts.get(i);
            if (b.partW[i] != 0) {
                hy = b.partY != null ? b.ly + b.partY[i] : hy0;
                hh = b.partRH != null ? b.partRH[i] : hh0;
                int py = hy + (hh - 15) / 2;
                int px = b.lx + b.partX[i];
                if (p.field() == null) {
                    c.text(p.text(), px, hy + (hh - 8) / 2, -1);
                } else {
                    BlockTypes.Field f = p.field();
                    int aw = arrowW(f);
                    VisualScript.Block in = b.inputs.get(f.name());
                    boolean socket = in != null || b.sockets.contains(f.name());
                    if (aw > 0) {
                        int ay = hy + hh / 2;
                        int col = socket ? -1 : -1711276033;
                        c.fill(px + 1, ay - 1, 1, 3, col);
                        c.fill(px + 2, ay - 2, 1, 1, col);
                        c.fill(px + 2, ay + 2, 1, 1, col);
                        c.fill(px + 6, ay - 1, 1, 3, col);
                        c.fill(px + 5, ay - 2, 1, 1, col);
                        c.fill(px + 5, ay + 2, 1, 1, col);
                    }

                    int fx = px + aw;
                    if (in != null) {
                        this.paintValue(c, in, dim);
                    } else if (socket) {
                        int sh = 15;
                        if (f.booleanSocket()) {
                            hexagon(c, fx, py, 26, sh, Theme.alpha(dark, 0.85F));
                        } else {
                            c.round(fx, py, 26, sh, sh / 2, Theme.alpha(dark, 0.85F));
                        }

                        c.textCentered("…", fx + 13, py + 3, -1996488705);
                    } else {
                        String v = b.field(f.name()).trim();
                        this.pill(c, fx, py, b.partW[i] - aw, shown(b, f), isDropdown(f), v.isEmpty(), dark);
                    }
                }
            }
        }
    }

    private void paintBlock(Canvas c, VisualScript.Block b, double top, double bottom, boolean dim) {
        BlockTypes.Type t = b.typeInfo();
        if (t.isValue()) {
            this.paintValue(c, b, dim);
        } else {
            int color = Theme.category(t.category());
            if (dim) {
                color = Theme.lerp(color, -12960435, 0.45F);
            }

            int dark = Theme.darker(color, 0.72F);
            int x = b.lx;
            int y = b.ly;
            int w = b.lw;
            boolean topNotch = t.shape() != BlockTypes.Shape.HAT;
            boolean bottomBump = t.shape() != BlockTypes.Shape.CAP;
            if (t.shape() == BlockTypes.Shape.HAT) {
                c.round(x, y, Math.min(w, 96), 22, 10, color);
            }

            int hy = y + (t.shape() == BlockTypes.Shape.HAT ? 10 : 0);
            int hh = b.headerH - (hy - y);
            if (t.shape() == BlockTypes.Shape.C && !b.slots.isEmpty() && !b.collapsed) {
                boolean choice = optionRows(b);
                int headerExtent = b.slotTop[0] - hy;
                this.shape(c, x, hy, w, headerExtent, color, dark, topNotch ? x + 12 : -1, x + 14 + 12);

                for (int i = 0; i < b.slots.size(); i++) {
                    int st = b.slotTop[i];
                    int sh = b.slotH[i];
                    c.fill(x, st, 14, sh, color);
                    c.fill(x, st, 2, sh, Theme.lerp(color, -1, 0.08F));
                    boolean last = i == b.slots.size() - 1;
                    int barY = st + sh;
                    int barH = last ? 14 : 20;
                    this.shape(c, x, barY, w, barH, color, dark, x + 14 + 12, last ? (bottomBump ? x + 12 : -1) : x + 14 + 12);
                }

                for (int i = 0; i < b.slots.size(); i++) {
                    if (choice || i != 0) {
                        int rowY = b.slotTop[i] - 20;
                        if (choice && i >= b.options.size()) {
                            c.text("иначе", x + 8, rowY + 6, -1);
                        } else if (choice) {
                            c.text(optionLabel(b), x + 8, rowY + 6, -1);
                            String opt = b.options.get(i);
                            int px = x + 8 + this.textW(optionLabel(b)) + 5;
                            int pw = Math.max(22, this.textW(opt) + 12);
                            this.pill(c, px, rowY + 2, pw, opt.isEmpty() ? "…" : opt, false, false, dark);
                            if (i == b.options.size() - 1 && canAddAnswer(b)) {
                                this.pill(c, px + pw + 5, rowY + 2, 14, "+", false, false, dark);
                            }
                        } else {
                            c.text(t.slotLabels().get(i), x + 8, rowY + 6, -1);
                        }
                    }
                }

                for (List<VisualScript.Block> slot : b.slots) {
                    this.paintList(c, slot, top, bottom, dim);
                }
            } else {
                this.shape(c, x, hy, w, hh, color, dark, topNotch ? x + 12 : -1, bottomBump ? x + 12 : -1);
            }

            this.paintParts(c, b, hy, hh, dark, dim);
            if (b.collapsed) {
                c.text(this.collapsedLabel(b), x + 8, hy + (hh - 8) / 2, -1);
            }

            if (b == this.selected) {
                c.outline(x - 1, y - 1, w + 2, b.lh + 2, -855638017);
            }

            if (b.error) {
                c.outline(x - 1, hy - 1, w + 2, hh + 2, -1023878);
                c.round(x + w - 6, hy - 5, 11, 11, 5, -1023878);
                c.text("!", x + w - 2, hy - 3, -1);
            }
        }
    }

    private void shape(Canvas c, int x, int y, int w, int h, int color, int dark, int dentX, int bumpX) {
        c.round(x, y + 3, w, h - 3, 3, color);

        for (int i = 0; i < 3; i++) {
            int inset = i == 0 ? 2 : (i == 1 ? 1 : 0);
            if (dentX < 0) {
                c.fill(x + inset, y + i, w - 2 * inset, 1, color);
            } else {
                c.fill(x + inset, y + i, dentX + i - x - inset, 1, color);
                int r = dentX + 14 - i;
                c.fill(r, y + i, x + w - inset - r, 1, color);
            }
        }

        c.fill(x + 2, y + h - 1, w - 4, 1, dark);
        if (bumpX >= 0) {
            for (int j = 0; j < 3; j++) {
                c.fill(bumpX + j, y + h + j, 14 - 2 * j, 1, j == 2 ? dark : color);
            }
        }

        c.fill(x + 2, y + 3, 1, 1, 587202559);
    }

    private void pill(Canvas c, int x, int y, int w, String text, boolean dropdown, boolean empty, int dark) {
        if (dropdown) {
            c.round(x, y, w, 15, 4, empty ? Theme.alpha(dark, 0.6F) : dark);
            c.text(text, x + 6, y + 4, empty ? -1426063361 : -1);
            c.icon(Icon.CHEVRON_DOWN, x + w - 12, y + 3, -855638017);
        } else {
            c.round(x, y, w, 15, 7, empty ? 1728053247 : -1);
            c.text(text, x + 6, y + 4, empty ? -7696474 : -12894124);
        }
    }

    private void paintPalette(Canvas c, int mx, int my) {
        c.fill(this.x, this.y, 210, this.h, -14934490);
        c.fill(this.x + 210 - 1, this.y, 1, this.h, -13749952);
        c.fill(this.x, this.y, 50, this.h, -15724266);
        this.catScroll = Math.max(0.0, Math.min((double)this.maxCatScroll(), this.catScroll));
        int cy = this.y + 6 - (int)Math.round(this.catScroll);
        int step = this.catStep();
        c.pushClip(this.x, this.y, 50, this.h);

        for (String cat : BlockTypes.CATEGORIES) {
            boolean active = cat.equals(this.activeCategory);
            boolean hov = mx >= this.x && mx < this.x + 50 && my >= cy && my < cy + step - 2 && this.isHoveredDeep();
            if (active) {
                c.fill(this.x, cy, 2, 36, Theme.category(cat));
            }

            if (hov || active) {
                c.round(this.x + 3, cy, 44, 38, 4, active ? 587202559 : 301989887);
            }

            int col = Theme.category(cat);
            c.round(this.x + 25 - 8, cy + 4, 16, 16, 8, col);
            c.round(this.x + 25 - 4, cy + 8, 8, 8, 4, Theme.lerp(col, -1, 0.35F));
            String label = shortName(cat);
            c.pushTransform(this.x + 25.0F, cy + 25, 0.75F);
            c.textCentered(label, 0, 0, active ? -1841682 : -6577995);
            c.popTransform();
            cy += step;
        }

        if (this.maxCatScroll() > 0) {
            int track = this.h - 8;
            int bar = Math.max(16, track * this.h / (BlockTypes.CATEGORIES.size() * step + 12));
            int by = this.y + 4 + (int)((track - bar) * this.catScroll / this.maxCatScroll());
            c.round(this.x + 50 - 3, by, 2, bar, 1, 1442840575);
        }

        c.popClip();
        int lx = this.x + 50 + 6;
        int top = this.paletteTop();
        c.pushClip(this.x + 50, top, 160, this.h - (top - this.y));
        c.pushTransform(lx, top - (float)this.paletteScroll, 1.0F);
        double viewTop = this.paletteScroll;
        double viewBottom = this.paletteScroll + this.h;
        String visibleCat = null;

        for (BlockEditor.PaletteEntry e : this.palette) {
            int ey = e.y;
            int eh = e.header ? 20 : e.block.lh;
            if (ey > viewBottom) {
                break;
            }

            if (!(ey + eh < viewTop)) {
                if (visibleCat == null && ey + eh > viewTop + 4.0) {
                    visibleCat = e.category;
                }

                if (e.header) {
                    c.text(e.category, 0, ey + 7, Theme.category(e.category));
                    c.fill(c.width(e.category) + 6, ey + 11, 136 - c.width(e.category), 1, -13749952);
                } else {
                    e.block.lx = 0;
                    this.layoutBlock(e.block, 0, ey);
                    this.paintBlock(c, e.block, -1.0E9, 1.0E9, false);
                }
            }
        }

        c.popTransform();
        c.popClip();
        if (visibleCat != null && this.mode == BlockEditor.Mode.NONE) {
            this.activeCategory = visibleCat;
        }
    }

    private int catStep() {
        return 40;
    }

    private int maxCatScroll() {
        return Math.max(0, BlockTypes.CATEGORIES.size() * this.catStep() + 12 - this.h);
    }

    private static String shortName(String cat) {
        return switch (cat) {
            case "Поток" -> "Поток";
            case "Речь" -> "Речь";
            case "Персонажи" -> "Перс.";
            case "Ожидание" -> "Ждать";
            case "Игрок и мир" -> "Мир";
            case "Экран" -> "Экран";
            case "Логика" -> "Логика";
            case "Объявления" -> "Объявл.";
            case "Функции" -> "Функции";
            case "Интерфейс" -> "GUI";
            case "События" -> "События";
            case "Переменные" -> "Перем.";
            case "Операторы" -> "Операт.";
            case "Значения" -> "Знач.";
            case "Анимации" -> "Аним.";
            default -> cat;
        };
    }

    private BlockEditor.Hit blockAt(double wx, double wy) {
        for (int i = this.script.stacks.size() - 1; i >= 0; i--) {
            VisualScript.Stack s = this.script.stacks.get(i);
            this.layoutStack(s);
            if (!(wx < s.x - 2.0) && !(wy < s.y - 2.0) && !(wx > s.x + s.bw + 2.0) && !(wy > s.y + s.bh + 2.0)) {
                BlockEditor.Hit h = this.hitList(s.blocks, wx, wy, s);
                if (h != null) {
                    return h;
                }
            }
        }

        return null;
    }

    private BlockEditor.Hit hitList(List<VisualScript.Block> list, double wx, double wy, VisualScript.Stack stack) {
        for (int i = 0; i < list.size(); i++) {
            VisualScript.Block b = list.get(i);
            if (!(wy < b.ly) && !(wy >= b.ly + b.lh)) {
                if (!b.collapsed) {
                    for (List<VisualScript.Block> slot : b.slots) {
                        BlockEditor.Hit inner = this.hitList(slot, wx, wy, stack);
                        if (inner != null) {
                            return inner;
                        }
                    }
                }

                if (wx >= b.lx && wx < b.lx + b.lw) {
                    if (b.slotTop != null) {
                        for (int s = 0; s < b.slots.size(); s++) {
                            if (wy >= b.slotTop[s] && wy < b.slotTop[s] + b.slotH[s] && wx >= b.lx + 14) {
                                return null;
                            }
                        }
                    }

                    return new BlockEditor.Hit(b, list, i, stack);
                }
            }
        }

        return null;
    }

    private BlockEditor.ValueHit valueAt(VisualScript.Block b, double wx, double wy) {
        for (BlockTypes.Part p : b.typeInfo().parts()) {
            if (p.field() != null) {
                VisualScript.Block in = b.inputs.get(p.field().name());
                if (in != null && wx >= in.lx && wx < in.lx + in.lw && wy >= in.ly && wy < in.ly + in.lh) {
                    BlockEditor.ValueHit deeper = this.valueAt(in, wx, wy);
                    return deeper != null ? deeper : new BlockEditor.ValueHit(b, p.field().name(), in);
                }
            }
        }

        return null;
    }

    private String arrowAt(VisualScript.Block b, double wx, double wy) {
        BlockTypes.Type t = b.typeInfo();
        List<BlockTypes.Part> parts = t.parts();

        for (int i = 0; i < parts.size(); i++) {
            BlockTypes.Field f = parts.get(i).field();
            if (f != null && arrowW(f) != 0 && b.partW[i] != 0 && !(wy < b.ly + b.partY[i]) && !(wy >= b.ly + b.partY[i] + b.partRH[i])) {
                int px = b.lx + b.partX[i];
                if (wx >= px - 1 && wx < px + 9) {
                    return f.name();
                }
            }
        }

        return null;
    }

    private String fieldAt(VisualScript.Block b, double wx, double wy) {
        BlockTypes.Type t = b.typeInfo();
        List<BlockTypes.Part> parts = t.parts();

        for (int i = 0; i < parts.size(); i++) {
            BlockTypes.Field f = parts.get(i).field();
            if (f != null
                && b.partW[i] != 0
                && !b.inputs.containsKey(f.name())
                && !b.sockets.contains(f.name())
                && !(wy < b.ly + b.partY[i])
                && !(wy >= b.ly + b.partY[i] + b.partRH[i])) {
                int px = b.lx + b.partX[i] + arrowW(f);
                if (wx >= px && wx < b.lx + b.partX[i] + b.partW[i]) {
                    return f.name();
                }
            }
        }

        if (optionRows(b) && b.slotTop != null) {
            for (int ix = 0; ix < b.options.size() && ix < b.slotTop.length; ix++) {
                int rowY = b.slotTop[ix] - 20;
                int px = b.lx + 8 + this.textW(optionLabel(b)) + 5;
                int pw = Math.max(22, this.textW(b.options.get(ix)) + 12);
                if (wy >= rowY && wy < rowY + 20 && wx >= px && wx < px + pw) {
                    return "opt:" + ix;
                }

                if (ix == b.options.size() - 1 && canAddAnswer(b) && wy >= rowY && wy < rowY + 20 && wx >= px + pw + 5 && wx < px + pw + 5 + 14) {
                    return "opt+";
                }
            }
        }

        return null;
    }

    private BlockEditor.PaletteEntry paletteAt(double mx, double my) {
        double py = my - this.paletteTop() + this.paletteScroll;
        double px = mx - (this.x + 50 + 6);

        for (BlockEditor.PaletteEntry e : this.palette) {
            if (!e.header) {
                VisualScript.Block b = e.block;
                if (py >= e.y && py < e.y + b.lh && px >= 0.0 && px < b.lw) {
                    return e;
                }
            }
        }

        return null;
    }

    @Override
    public String tooltipText(int mx, int my) {
        if (this.mode != BlockEditor.Mode.NONE) {
            return null;
        } else if (mx < this.x + 210) {
            BlockEditor.PaletteEntry e = this.paletteAt(mx, my);
            return e != null ? e.block.typeInfo().doc() : null;
        } else {
            BlockEditor.Hit h = this.blockAt(this.toWorldX(mx), this.toWorldY(my));
            if (h == null) {
                return null;
            } else {
                VisualScript.Block b = h.block;
                BlockEditor.ValueHit vh = this.valueAt(b, this.toWorldX(mx), this.toWorldY(my));
                if (vh != null) {
                    b = vh.value();
                }

                if (this.arrowAt(b, this.toWorldX(mx), this.toWorldY(my)) != null) {
                    return "‹› — превратить поле в гнездо для блока-значения (или обратно)";
                } else if (b.error && b.errorText != null) {
                    return "Ошибка: " + b.errorText;
                } else {
                    String f = this.fieldAt(b, this.toWorldX(mx), this.toWorldY(my));
                    if ("opt+".equals(f)) {
                        return "Добавить вариант ответа (всего до четырёх)";
                    } else {
                        if (f != null && !f.startsWith("opt:")) {
                            BlockTypes.Field fl = b.typeInfo().field(f);
                            if (fl != null && fl.kind() != null) {
                                return fl.label() + " — " + fl.kind().doc;
                            }
                        }

                        return null;
                    }
                }
            }
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        this.pressX = mx;
        this.pressY = my;
        if (mx < this.x + 50) {
            int idx = (int)((my - this.y - 6.0 + this.catScroll) / this.catStep());
            if (idx >= 0 && idx < BlockTypes.CATEGORIES.size()) {
                this.activeCategory = BlockTypes.CATEGORIES.get(idx);
                this.scrollPaletteTo(this.activeCategory);
            }

            return true;
        } else if (mx < this.x + 210) {
            if (my < this.paletteTop()) {
                return false;
            } else {
                BlockEditor.PaletteEntry e = this.paletteAt(mx, my);
                if (e != null && button == 0) {
                    this.mode = BlockEditor.Mode.PRESS_PALETTE;
                    this.pressPaletteBlock = e.block;
                    this.dragOffX = mx - (this.x + 50 + 6) - e.block.lx;
                    this.dragOffY = my - this.paletteTop() + this.paletteScroll - e.block.ly;
                }

                return true;
            }
        } else {
            double wx = this.toWorldX(mx);
            double wy = this.toWorldY(my);
            BlockEditor.Hit hit = this.blockAt(wx, wy);
            if (button == 1) {
                if (hit != null) {
                    BlockEditor.ValueHit vh = this.valueAt(hit.block, wx, wy);
                    if (vh != null) {
                        this.selected = vh.value();
                        this.blockMenu(new BlockEditor.Hit(vh.value(), null, -1, hit.stack), (int)mx, (int)my);
                        return true;
                    }

                    this.selected = hit.block;
                    this.blockMenu(hit, (int)mx, (int)my);
                } else {
                    this.canvasMenu((int)mx, (int)my, wx, wy);
                }

                return true;
            } else if (button != 2 && hit != null) {
                BlockEditor.ValueHit vh = this.valueAt(hit.block, wx, wy);
                VisualScript.Block target = vh != null ? vh.value() : hit.block;
                String arrow = this.arrowAt(target, wx, wy);
                if (arrow != null) {
                    this.toggleSocket(target, arrow);
                    return true;
                } else {
                    long now = System.currentTimeMillis();
                    if (button == 0
                        && vh == null
                        && this.fieldAt(target, wx, wy) == null
                        && target == this.lastClickBlock
                        && now - this.lastClickAt < 320L
                        && !target.typeInfo().isValue()) {
                        this.lastClickBlock = null;
                        this.toggleFold(target);
                        return true;
                    } else {
                        this.lastClickBlock = target;
                        this.lastClickAt = now;
                        this.selected = target;
                        this.pressHit = hit;
                        this.pressBlock = target;
                        this.pressValue = vh;
                        String field = this.fieldAt(target, wx, wy);
                        this.mode = field != null ? BlockEditor.Mode.PRESS_PILL : (vh != null ? BlockEditor.Mode.PRESS_VALUE : BlockEditor.Mode.PRESS_BLOCK);
                        this.dragOffX = wx - target.lx;
                        this.dragOffY = wy - target.ly;
                        return true;
                    }
                }
            } else {
                this.mode = BlockEditor.Mode.PAN;
                this.selected = button == 0 ? null : this.selected;
                return true;
            }
        }
    }

    private void toggleSocket(VisualScript.Block b, String field) {
        this.snapshot();
        VisualScript.Block in = b.inputs.remove(field);
        if (in != null) {
            b.sockets.remove(field);
            VisualScript.Stack s = new VisualScript.Stack();
            s.x = in.lx + 8;
            s.y = b.ly + b.lh + 12;
            s.blocks.add(in);
            this.script.stacks.add(s);
        } else if (b.sockets.contains(field)) {
            b.sockets.remove(field);
        } else {
            b.sockets.add(field);
        }

        this.changed();
    }

    private void startDragValue(BlockEditor.ValueHit vh) {
        this.snapshot();
        vh.parent().inputs.remove(vh.field());
        if (!vh.parent().sockets.contains(vh.field())) {
            vh.parent().sockets.add(vh.field());
        }

        this.dragStack = new VisualScript.Stack();
        this.dragStack.blocks.add(vh.value());
        this.mode = BlockEditor.Mode.DRAG;
        this.changed();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        double moved = Math.abs(mx - this.pressX) + Math.abs(my - this.pressY);
        switch (this.mode) {
            case PAN:
                this.viewX = this.viewX - dx / this.zoom;
                this.viewY = this.viewY - dy / this.zoom;
                break;
            case PRESS_BLOCK:
                if (moved > 4.0) {
                    this.startDragFromCanvas(UI.ctrl(modsNow()));
                }
                break;
            case PRESS_PILL:
                if (moved > 4.0) {
                    if (this.pressValue != null) {
                        this.startDragValue(this.pressValue);
                    } else {
                        this.startDragFromCanvas(UI.ctrl(modsNow()));
                    }
                }
                break;
            case PRESS_VALUE:
                if (moved > 4.0 && this.pressValue != null) {
                    this.startDragValue(this.pressValue);
                }
                break;
            case PRESS_PALETTE:
                if (moved > 4.0) {
                    VisualScript.Block copy = this.pressPaletteBlock.deepCopy();
                    this.dragStack = new VisualScript.Stack();
                    this.dragStack.blocks.add(copy);
                    this.mode = BlockEditor.Mode.DRAG;
                }
        }

        if (this.mode == BlockEditor.Mode.DRAG && this.dragStack != null) {
            this.dragStack.x = this.toWorldX(mx) - this.dragOffX;
            this.dragStack.y = this.toWorldY(my) - this.dragOffY;
            this.dragStack.dirty = true;
            this.layoutStack(this.dragStack);
            boolean value = this.dragStack.blocks.size() == 1 && this.dragStack.blocks.get(0).typeInfo().isValue();
            this.snap = !(mx < this.x + 210) && !value ? this.findSnap() : null;
            this.socketTarget = !(mx < this.x + 210) && value ? this.findSocket(this.dragStack.blocks.get(0)) : null;
            this.autoScroll(mx, my);
        }

        return true;
    }

    private static int modsNow() {
        long win = Minecraft.getInstance().getWindow().getWindow();
        int mods = 0;
        if (GLFW.glfwGetKey(win, 341) == 1 || GLFW.glfwGetKey(win, 345) == 1) {
            mods |= 2;
        }

        return mods;
    }

    private void autoScroll(double mx, double my) {
        double edge = 20.0;
        if (mx > this.x + this.w - edge) {
            this.viewX = this.viewX + 6.0 / this.zoom;
        }

        if (my > this.y + this.h - edge) {
            this.viewY = this.viewY + 6.0 / this.zoom;
        }

        if (my < this.y + edge) {
            this.viewY = this.viewY - 6.0 / this.zoom;
        }
    }

    private void startDragFromCanvas(boolean single) {
        BlockEditor.Hit h = this.pressHit;
        this.snapshot();
        this.dragStack = new VisualScript.Stack();
        if (single) {
            this.dragStack.blocks.add(h.list.remove(h.index));
        } else {
            List<VisualScript.Block> tail = new ArrayList<>(h.list.subList(h.index, h.list.size()));
            h.list.subList(h.index, h.list.size()).clear();
            this.dragStack.blocks.addAll(tail);
        }

        if (h.stack.blocks.isEmpty()) {
            this.script.stacks.remove(h.stack);
        }

        this.mode = BlockEditor.Mode.DRAG;
        this.changed();
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        BlockEditor.Mode m = this.mode;
        this.mode = BlockEditor.Mode.NONE;
        if (m == BlockEditor.Mode.DRAG && this.dragStack != null) {
            if (mx >= this.x + 210) {
                if (this.pressPaletteBlock != null) {
                    this.snapshot();
                }

                if (this.socketTarget != null) {
                    VisualScript.Block value = this.dragStack.blocks.get(0);
                    VisualScript.Block old = this.socketTarget.block().inputs.put(this.socketTarget.field(), value);
                    if (!this.socketTarget.block().sockets.contains(this.socketTarget.field())) {
                        this.socketTarget.block().sockets.add(this.socketTarget.field());
                    }

                    if (old != null) {
                        VisualScript.Stack s = new VisualScript.Stack();
                        s.x = this.socketTarget.x() + 10;
                        s.y = this.socketTarget.y() + 30;
                        s.blocks.add(old);
                        this.script.stacks.add(s);
                    }
                } else if (this.snap != null) {
                    this.snap.list.addAll(this.snap.index, this.dragStack.blocks);
                } else {
                    this.dragStack.dirty = true;
                    this.script.stacks.add(this.dragStack);
                }

                this.selected = this.dragStack.blocks.get(0);
            }

            this.dragStack = null;
            this.snap = null;
            this.socketTarget = null;
            this.pressPaletteBlock = null;
            this.pressValue = null;
            this.changed();
            return true;
        } else {
            if (m == BlockEditor.Mode.PRESS_PILL && this.pressBlock != null) {
                String field = this.fieldAt(this.pressBlock, this.toWorldX(mx), this.toWorldY(my));
                if (field != null) {
                    this.editField(this.pressBlock, field);
                }
            }

            this.pressValue = null;
            if (m == BlockEditor.Mode.PRESS_PALETTE && this.pressPaletteBlock != null) {
                VisualScript.Stack main = this.script.mainStack();
                this.snapshot();
                VisualScript.Block copy = this.pressPaletteBlock.deepCopy();
                if (main != null && copy.typeInfo().shape() != BlockTypes.Shape.HAT && !copy.typeInfo().isValue()) {
                    main.blocks.add(copy);
                    this.selected = copy;
                    this.changed();
                    this.revealBlock(copy);
                } else {
                    VisualScript.Stack s = new VisualScript.Stack();
                    s.x = this.viewX + 40.0 / this.zoom;
                    s.y = this.viewY + 40.0 / this.zoom;
                    s.blocks.add(copy);
                    this.script.stacks.add(s);
                    this.changed();
                }
            }

            this.pressPaletteBlock = null;
            this.pressHit = null;
            return true;
        }
    }

    private void revealBlock(VisualScript.Block b) {
        VisualScript.Stack main = this.script.mainStack();
        if (main != null) {
            this.layoutStack(main);
        }

        double bx = b.lx;
        double by = b.ly;
        double vw = this.canvasW() / this.zoom;
        double vh = this.h / this.zoom;
        if (by + b.lh > this.viewY + vh - 20.0) {
            this.viewY = by + b.lh - vh + 40.0;
        }

        if (by < this.viewY) {
            this.viewY = by - 20.0;
        }

        if (bx < this.viewX || bx > this.viewX + vw - 40.0) {
            this.viewX = bx - 40.0;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < this.x + 50) {
            this.catScroll = Math.max(0.0, Math.min((double)this.maxCatScroll(), this.catScroll - delta * 30.0));
            return true;
        } else if (mx < this.x + 210) {
            this.paletteTarget = Math.max(0.0, Math.min((double)this.maxPaletteScroll(), this.paletteTarget - delta * 40.0));
            return true;
        } else {
            if (Screen.hasControlDown()) {
                double wx = this.toWorldX(mx);
                double wy = this.toWorldY(my);
                this.zoom = Math.max(0.4, Math.min(2.0, this.zoom * (delta > 0.0 ? 1.1 : 0.9090909090909091)));
                this.viewX = wx - (mx - this.canvasX()) / this.zoom;
                this.viewY = wy - (my - this.y) / this.zoom;
            } else if (Screen.hasShiftDown()) {
                this.viewX = this.viewX - delta * 40.0 / this.zoom;
            } else {
                this.viewY = this.viewY - delta * 40.0 / this.zoom;
            }

            return true;
        }
    }

    private BlockEditor.SocketTarget findSocket(VisualScript.Block value) {
        double px = this.dragStack.x;
        double py = this.dragStack.y + value.lh / 2.0;
        BlockEditor.SocketTarget[] best = new BlockEditor.SocketTarget[]{null};
        double[] bestD = new double[]{22.0 / Math.max(0.5, this.zoom)};

        for (VisualScript.Stack s : this.script.stacks) {
            this.layoutStack(s);
            if (!(px > s.x + s.bw + 40.0) && !(px < s.x - 60.0) && !(py < s.y - 40.0) && !(py > s.y + s.bh + 40.0)) {
                for (VisualScript.Block b : s.blocks) {
                    this.socketsIn(b, px, py, best, bestD);
                }
            }
        }

        return best[0];
    }

    private void socketsIn(VisualScript.Block b, double px, double py, BlockEditor.SocketTarget[] best, double[] bestD) {
        BlockTypes.Type t = b.typeInfo();
        int hy = b.ly + (t.shape() == BlockTypes.Shape.HAT ? 10 : 0);
        int hh = b.headerH - (hy - b.ly);
        List<BlockTypes.Part> parts = t.parts();

        for (int i = 0; i < parts.size() && b.partX != null && i < b.partX.length; i++) {
            BlockTypes.Field f = parts.get(i).field();
            if (f != null && f.acceptsValue() && b.partW[i] != 0) {
                VisualScript.Block in = b.inputs.get(f.name());
                if (in != null) {
                    this.socketsIn(in, px, py, best, bestD);
                }

                int fx = b.lx + b.partX[i] + arrowW(f);
                int fw = b.partW[i] - arrowW(f);
                int fh = in != null ? in.lh : 15;
                int fy = b.ly + b.partY[i] + (b.partRH[i] - fh) / 2;
                double d = Math.hypot(px - fx, py - (fy + fh / 2.0));
                if (px > fx - 6 && px < fx + fw) {
                    d = Math.min(d, Math.abs(py - (fy + fh / 2.0)));
                }

                if (d < bestD[0]) {
                    bestD[0] = d;
                    best[0] = new BlockEditor.SocketTarget(b, f.name(), fx, fy, fw, fh);
                }
            }
        }

        if (!b.collapsed) {
            for (List<VisualScript.Block> slot : b.slots) {
                for (VisualScript.Block c : slot) {
                    this.socketsIn(c, px, py, best, bestD);
                }
            }
        }
    }

    private BlockEditor.Snap findSnap() {
        VisualScript.Block first = this.dragStack.blocks.get(0);
        if (first.typeInfo().isValue()) {
            return null;
        } else {
            VisualScript.Block last = this.dragStack.blocks.get(this.dragStack.blocks.size() - 1);
            boolean hat = first.typeInfo().shape() == BlockTypes.Shape.HAT;
            boolean cap = last.typeInfo().shape() == BlockTypes.Shape.CAP;
            double px = this.dragStack.x;
            double py = this.dragStack.y;
            BlockEditor.Snap best = null;
            double bestD = 24.0 / Math.max(0.5, this.zoom) * 1.0;

            for (VisualScript.Stack s : this.script.stacks) {
                this.layoutStack(s);
                if (!(px > s.x + s.bw + 60.0) && !(px + 80.0 < s.x - 40.0) && !(py > s.y + s.bh + 60.0) && !(py + this.dragStack.bh < s.y - 60.0)) {
                    if (!s.blocks.isEmpty() && s.blocks.get(0).typeInfo().shape() != BlockTypes.Shape.HAT && !cap) {
                        int ty = s.blocks.get(0).ly - this.dragStack.bh;
                        double d = Math.hypot(px - s.x, py - ty);
                        if (d < bestD) {
                            bestD = d;
                            best = new BlockEditor.Snap(s.blocks, 0, (int)s.x, s.blocks.get(0).ly, s);
                        }
                    }

                    if (!hat) {
                        best = this.snapList(s.blocks, s, px, py, cap, best, bestD);
                        if (best != null) {
                            bestD = Math.hypot(px - best.x, py - best.y);
                        }
                    }
                }
            }

            return best;
        }
    }

    private BlockEditor.Snap snapList(
        List<VisualScript.Block> list, VisualScript.Stack s, double px, double py, boolean cap, BlockEditor.Snap best, double bestD
    ) {
        for (int i = 0; i < list.size(); i++) {
            VisualScript.Block b = list.get(i);
            BlockTypes.Shape shape = b.typeInfo().shape();
            boolean canAfter = shape != BlockTypes.Shape.CAP && (!cap || i == list.size() - 1);
            if (canAfter) {
                int ay = b.ly + b.lh;
                double d = Math.hypot(px - b.lx, py - ay);
                if (d < bestD) {
                    bestD = d;
                    best = new BlockEditor.Snap(list, i + 1, b.lx, ay, s);
                }
            }

            for (int k = 0; k < b.slots.size() && b.slotTop != null; k++) {
                List<VisualScript.Block> slot = b.slots.get(k);
                if (!cap || slot.isEmpty()) {
                    int sy = b.slotTop[k];
                    double d = Math.hypot(px - (b.lx + 14), py - sy);
                    if (d < bestD) {
                        bestD = d;
                        best = new BlockEditor.Snap(slot, 0, b.lx + 14, sy, s);
                    }
                }

                BlockEditor.Snap inner = this.snapList(slot, s, px, py, cap, best, bestD);
                if (inner != best && inner != null) {
                    best = inner;
                    bestD = Math.hypot(px - inner.x, py - inner.y);
                }
            }
        }

        return best;
    }

    private void editField(VisualScript.Block b, String field) {
        if (field.equals("opt+")) {
            this.addAnswer(b);
            this.editField(b, "opt:" + (b.options.size() - 1));
        } else if (field.startsWith("opt:")) {
            int idx = Integer.parseInt(field.substring(4));
            this.openTextEditor(b, field, b.options.get(idx), v -> b.options.set(idx, v));
        } else {
            BlockTypes.Field f = b.typeInfo().field(field);
            if (f != null) {
                if (b.type.equals("character") && field.startsWith("head")) {
                    HeadEditor.open(b.field("type"), headParams(b), p -> {
                        this.setField(b, "headscale", p.scale() == 1.0F ? "" : String.valueOf(p.scale()));
                        this.setField(b, "headx", p.dx() == 0 ? "" : String.valueOf(p.dx()));
                        this.setField(b, "heady", p.dy() == 0 ? "" : String.valueOf(p.dy()));
                        this.setField(b, "headyaw", p.yaw() == 0 ? "" : String.valueOf(p.yaw()));
                        this.setField(b, "headbone", p.bone().equals("auto") ? "" : p.bone());
                    });
                } else if (!field.equals("color") || !b.type.equals("character") && !b.type.equals("namecolor")) {
                    if (f.kind() == ParamType.CHOICE) {
                        List<String> modes = BlockTypes.choices(b.type, field);
                        if (modes != null) {
                            PopupMenu pm = new PopupMenu();

                            for (String mode : modes) {
                                pm.item(mode, () -> this.setField(b, field, mode)).checked(() -> b.field(field).equals(mode));
                            }

                            int[] r = this.pillScreenRect(b, field);
                            pm.show(r[0], r[1] + r[3] + 2);
                        }
                    } else if (f.kind() == ParamType.TASK) {
                        String quest = b.field("name").replace("\"", "").trim();
                        Map<String, String> tasks = quest.isEmpty() ? Map.of() : AssetIndex.questTasks(quest);
                        PopupMenu pm = new PopupMenu();
                        if (tasks.isEmpty()) {
                            pm.item(quest.isEmpty() ? "Сначала выберите квест" : "В этом квесте нет задач", Icon.INFO, null, () -> {});
                        }

                        tasks.forEach(
                            (id, line) -> pm.item(line.isBlank() ? id : line + "  (" + id + ")", () -> this.setField(b, field, id))
                                .checked(() -> b.field(field).equals(id))
                        );
                        pm.separator();
                        pm.item("Ввести вручную…", Icon.EDIT, null, () -> this.openTextEditor(b, field, b.field(field), v -> b.fields.put(field, v)));
                        int[] r = this.pillScreenRect(b, field);
                        pm.show(r[0], r[1] + r[3] + 2);
                    } else if (f.kind() == ParamType.TARGET) {
                        PopupMenu pm = new PopupMenu();
                        pm.item("игрок", Icon.USER, null, () -> this.setField(b, field, "player")).checked(() -> b.field(field).equals("player"));
                        List<String> chars = this.names("character");
                        if (!chars.isEmpty()) {
                            pm.separator();
                        }

                        for (String ch : chars) {
                            pm.item("нпс: " + ch, Icon.USER, null, () -> this.setField(b, field, ch)).color(-11544898).checked(() -> b.field(field).equals(ch));
                        }

                        pm.separator();
                        pm.item("моб… (ближайший выбранного типа)", Icon.CUBE, null, () -> {
                            if (this.assetPicker != null) {
                                this.assetPicker.pick(ParamType.ENTITY, b.field(field).replace("\"", ""), v -> this.setField(b, field, "\"" + v + "\""));
                            }
                        });
                        pm.item("любой ближайший моб", Icon.CUBE, null, () -> this.setField(b, field, "\"mob\""));
                        List<String> locs = this.names("location");
                        if (!locs.isEmpty()) {
                            pm.separator();
                        }

                        for (String l : locs) {
                            pm.item("точка: " + l, Icon.PIN, null, () -> this.setField(b, field, l)).color(-13461).checked(() -> b.field(field).equals(l));
                        }

                        if (this.positionProvider != null && this.positionProvider.get() != null) {
                            String pos = this.positionProvider.get();
                            pm.item("моя позиция " + pos, Icon.PIN, null, () -> this.setField(b, field, pos));
                        }

                        pm.separator();
                        pm.item("Ввести вручную…", Icon.EDIT, null, () -> this.openTextEditor(b, field, b.field(field), v -> b.fields.put(field, v)));
                        int[] r = this.pillScreenRect(b, field);
                        pm.show(r[0], r[1] + r[3] + 2);
                    } else {
                        if (b.type.equals("screen:notify") && field.equals("icon")) {
                            String mode = b.field("mode");
                            if (mode.startsWith("картинка")) {
                                PopupMenu pmx = new PopupMenu();

                                for (String ch : AssetIndex.projectImages.get()) {
                                    pmx.item(ch, Icon.IMAGE, null, () -> this.setField(b, field, ch)).checked(() -> b.field(field).equals(ch));
                                }

                                if (AssetIndex.projectImages.get().isEmpty()) {
                                    pmx.item("В проекте нет картинок — добавьте в «Картинки»", Icon.INFO, null, () -> {});
                                }

                                pmx.separator();
                                pmx.item(
                                    "Текстура игры (minecraft:textures/…)…",
                                    Icon.EDIT,
                                    null,
                                    () -> this.openTextEditor(b, field, b.field(field), v -> b.fields.put(field, v))
                                );
                                int[] r = this.pillScreenRect(b, field);
                                pmx.show(r[0], r[1] + r[3] + 2);
                                return;
                            }

                            if (mode.startsWith("без иконки")) {
                                return;
                            }

                            if (this.assetPicker != null) {
                                this.assetPicker.pick(ParamType.ITEM, b.field(field), v -> this.setField(b, field, v));
                                return;
                            }
                        }

                        if (this.assetPicker == null
                            || f.kind() != ParamType.ANIM && f.kind() != ParamType.ENTITY && f.kind() != ParamType.SOUND && f.kind() != ParamType.ITEM) {
                            List<String> choices = this.choicesFor(f.kind());
                            if (choices == null) {
                                this.openTextEditor(b, field, b.field(field), v -> b.fields.put(field, v));
                            } else {
                                PopupMenu pmx = new PopupMenu();

                                for (String ch : choices) {
                                    pmx.item(ch, () -> this.setField(b, field, ch.startsWith("моя позиция ") ? ch.substring(12) : ch))
                                        .checked(() -> b.field(field).equals(ch));
                                }

                                if (!choices.isEmpty()) {
                                    pmx.separator();
                                }

                                pmx.item("Ввести вручную…", Icon.EDIT, null, () -> this.openTextEditor(b, field, b.field(field), v -> b.fields.put(field, v)));
                                if (f.optional() && !b.field(field).isEmpty()) {
                                    pmx.item("Очистить", Icon.CLOSE, null, () -> this.setField(b, field, ""));
                                }

                                int[] r = this.pillScreenRect(b, field);
                                pmx.show(r[0], r[1] + r[3] + 2);
                            }
                        } else {
                            if (f.kind() == ParamType.ANIM) {
                                org.sprautestudio.spraute_engine.editor.assets.AssetPicker.previewEntity = this.characterType(b.field("who"));
                            }

                            this.assetPicker.pick(f.kind(), b.field(field), v -> this.setField(b, field, v));
                        }
                    }
                } else {
                    PopupMenu pmx = new PopupMenu();
                    pmx.item("Выбрать цвет…", Icon.EDIT, null, () -> {
                        int argb = -22016;

                        try {
                            Integer c = Colors.parse(b.field(field).replace("\"", ""));
                            if (c != null) {
                                argb = 0xFF000000 | c;
                            }
                        } catch (RuntimeException var5) {
                        }

                        int[] rx = this.pillScreenRect(b, field);
                        this.snapshot();
                        ColorPicker.open(argb, rx[0], rx[1] + rx[3] + 2, c -> {
                            b.fields.put(field, String.format("#%06X", c & 16777215));
                            this.changed();
                        });
                    });
                    pmx.separator();

                    for (String n : List.of("gold", "orange", "yellow", "red", "pink", "purple", "aqua", "blue", "green", "white", "gray")) {
                        pmx.item(n, () -> this.setField(b, field, n)).checked(() -> b.field(field).replace("\"", "").equals(n));
                    }

                    pmx.separator();
                    pmx.item(
                        b.type.equals("character") ? "Обычный (из настроек движка)" : "Сбросить (reset)",
                        Icon.CLOSE,
                        null,
                        () -> this.setField(b, field, b.type.equals("character") ? "" : "reset")
                    );
                    int[] r = this.pillScreenRect(b, field);
                    pmx.show(r[0], r[1] + r[3] + 2);
                }
            }
        }
    }

    private void setField(VisualScript.Block b, String field, String value) {
        this.snapshot();
        b.fields.put(field, value);
        this.changed();
    }

    private List<String> choicesFor(ParamType kind) {
        if (kind == null) {
            return null;
        } else {
            return (List<String>)(switch (kind) {
                case CHAR -> this.names("character");
                case LABEL -> this.names("label");
                case EMOTION -> Specs.EMOTIONS;
                case KEY -> List.of("F8", "F9", "F10", "G", "H", "SPACE", "ENTER");
                case BOOL -> List.of("да", "нет");
                case TARGET -> {
                    List<String> l = new ArrayList<>(this.names("character"));
                    l.addAll(this.names("location"));
                    l.add("player");
                    yield l;
                }
                case SPEAKER -> {
                    List<String> l = new ArrayList<>(this.names("character"));
                    l.add("player");
                    l.add("0");
                    l.add("1");
                    yield l;
                }
                case LAYER -> Specs.LAYERS;
                case MOVETYPE -> Specs.MOVE_TYPES;
                default -> null;
                case ENTITY -> Specs.ST_ENTITIES;
                case FUNC -> this.names("func");
                case BRANCH -> {
                    Set<String> set = new LinkedHashSet<>();

                    for (VisualScript.Stack s : this.script.stacks) {
                        this.collectBranches(s.blocks, set);
                    }

                    yield new ArrayList<>(set);
                }
                case IMAGE -> AssetIndex.imageChoices();
                case GUI -> new ArrayList(AssetIndex.projectGuis.get().keySet());
                case GUIEL -> {
                    Set<String> set = new LinkedHashSet<>();

                    for (List<String> l : AssetIndex.projectGuis.get().values()) {
                        set.addAll(l);
                    }

                    yield new ArrayList<>(set);
                }
                case VAR -> this.names("var");
                case SYSTEM -> Specs.ENGINE_SYSTEMS;
                case EASE -> Ease.names();
                case CUTSCENE -> (List)AssetIndex.projectCutscenes.get();
                case VFX -> AssetIndex.vfxChoices();
                case POST -> AssetIndex.postChoices();
                case POSTPARAM -> PostParams.ids();
                case CAMRIG -> AssetIndex.camChoices();
                case CAMPARAM -> CamProfile.ids();
                case STRUCTURE -> AssetIndex.structureChoices();
                case BUILDSTYLE -> BuildStyles.names();
                case STEALTHSHAPE -> StealthShape.NAMES;
                case STEALTH -> AssetIndex.stealthChoices();
                case SKILLTREE -> (List)AssetIndex.projectSkills.get();
                case SKILLNODE -> (List)AssetIndex.projectSkillNodes.get();
                case SKILLMENU -> (List)AssetIndex.projectSkillMenus.get();
                case PANELTURN -> PanelLook.TURNS;
                case PANELLOOK -> PanelLook.looks();
                case SLOTORDER -> SlotPlan.ORDERS;
                case PLAYERCAN -> PlayerRuleDefs.switches();
                case PLAYERSTAT -> PlayerRuleDefs.numbers();
                case PLAYERRULE -> PlayerRuleDefs.names();
                case SHOP -> (List)AssetIndex.projectShops.get();
                case QUEST -> (List)AssetIndex.projectQuests.get();
                case ZONE -> (List)AssetIndex.projectZones.get();
                case FIGHT -> AssetIndex.fightChoices();
                case PATH -> (List)AssetIndex.projectPaths.get();
                case LOC -> {
                    List<String> l = new ArrayList<>(this.names("location"));
                    if (this.positionProvider != null && this.positionProvider.get() != null) {
                        l.add("моя позиция " + this.positionProvider.get());
                    }

                    yield l;
                }
            });
        }
    }

    private List<String> names(String type) {
        Set<String> set = new LinkedHashSet<>();

        for (VisualScript.Stack s : this.script.stacks) {
            this.collectNames(s.blocks, type, set);
        }

        if (this.outsideNames != null) {
            set.addAll(this.outsideNames.apply(type));
        }

        return new ArrayList<>(set);
    }

    private void collectBranches(List<VisualScript.Block> list, Set<String> out) {
        for (VisualScript.Block b : list) {
            if ((b.type.equals("async") || b.type.equals("parallel")) && !b.field("name").isBlank()) {
                out.add(b.field("name").trim());
            }

            if (b.type.equals("asynccall") && !b.field("as").isBlank()) {
                out.add(b.field("as").trim());
            }

            for (List<VisualScript.Block> slot : b.slots) {
                this.collectBranches(slot, out);
            }
        }
    }

    private String characterType(String name) {
        if (name != null && !name.isBlank()) {
            String n = name.trim();
            String[] found = new String[]{null};

            for (VisualScript.Stack s : this.script.stacks) {
                this.findCharacter(s.blocks, n, found);
            }

            return found[0];
        } else {
            return null;
        }
    }

    private void findCharacter(List<VisualScript.Block> list, String name, String[] out) {
        for (VisualScript.Block b : list) {
            if (out[0] != null) {
                return;
            }

            if (b.type.equals("character") && b.field("name").trim().equals(name)) {
                out[0] = b.field("type").replace("\"", "").trim();
                return;
            }

            for (List<VisualScript.Block> slot : b.slots) {
                this.findCharacter(slot, name, out);
            }
        }
    }

    private void collectNames(List<VisualScript.Block> list, String type, Set<String> out) {
        for (VisualScript.Block b : list) {
            if (b.type.equals(type) && !b.field("name").isBlank()) {
                out.add(b.field("name").trim());
            }

            for (List<VisualScript.Block> slot : b.slots) {
                this.collectNames(slot, type, out);
            }
        }
    }

    private int[] pillScreenRect(VisualScript.Block b, String field) {
        BlockTypes.Type t = b.typeInfo();
        int hy = b.ly + (t.shape() == BlockTypes.Shape.HAT ? 10 : 0);
        int hh = b.headerH - (hy - b.ly);
        int wx = b.lx;
        int ww = 60;
        int wy = hy + (hh - 15) / 2;
        if (field.startsWith("opt:")) {
            int idx = Integer.parseInt(field.substring(4));
            wx = b.lx + 8 + this.textW(optionLabel(b)) + 5;
            ww = Math.max(22, this.textW(b.options.get(idx)) + 12);
            int rowY = idx < b.slotTop.length ? b.slotTop[idx] - 20 : b.slotTop[b.slotTop.length - 1] + b.slotH[b.slotTop.length - 1];
            wy = rowY + 2;
        } else {
            List<BlockTypes.Part> parts = t.parts();

            for (int i = 0; i < parts.size(); i++) {
                if (parts.get(i).field() != null && parts.get(i).field().name().equals(field)) {
                    int aw = arrowW(parts.get(i).field());
                    wx = b.lx + b.partX[i] + aw;
                    ww = b.partW[i] - aw;
                    wy = b.ly + b.partY[i] + (b.partRH[i] - 15) / 2;
                }
            }
        }

        return new int[]{this.toScreenX(wx), this.toScreenY(wy), (int)Math.round(ww * this.zoom), (int)Math.round(15.0 * this.zoom)};
    }

    private void openTextEditor(VisualScript.Block b, String field, String value, Consumer<String> apply) {
        UI ui = UI.get();
        if (ui != null) {
            int[] r = this.pillScreenRect(b, field);
            BlockEditor.InlineEditor ed = new BlockEditor.InlineEditor(r[0] - 2, r[1] - 2, Math.max(120, r[2] + 60), Math.max(16, r[3] + 4));
            ed.setText(value);
            ed.selectAll();
            ed.commit = v -> {
                if (!v.equals(value)) {
                    this.snapshot();
                    apply.accept(v);
                    this.changed();
                }
            };
            ui.openOverlay(ed, false, true, ed::commitNow);
            ed.requestFocus();
        }
    }

    private void blockMenu(BlockEditor.Hit hit, int mx, int my) {
        VisualScript.Block b = hit.block;
        PopupMenu pm = new PopupMenu();
        pm.item("Дублировать", Icon.COPY, "Ctrl+D", () -> this.duplicate(b));
        pm.item("Копировать", Icon.COPY, "Ctrl+C", () -> this.clipboard = b.deepCopy());
        if (!b.typeInfo().isValue()) {
            pm.separator();
            pm.item(b.collapsed ? "Развернуть" : "Свернуть", b.collapsed ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT, "двойной клик", () -> this.toggleFold(b));
            boolean hasFields = b.typeInfo().parts().stream().anyMatch(p -> p.field() != null);
            if (hasFields && !b.collapsed) {
                pm.item(b.oneLine ? "В несколько строк" : "В одну строку", Icon.LIST, null, () -> {
                    this.snapshot();
                    b.oneLine = !b.oneLine;
                    this.changed();
                });
            }
        }

        this.jumpItems(pm);
        if (optionRows(b)) {
            boolean sw = b.type.equals("switch");
            pm.separator();
            if (sw) {
                pm.item("Добавить вариант", Icon.PLUS, null, () -> {
                    this.snapshot();
                    b.options.add("\"значение" + (b.options.size() + 1) + "\"");
                    b.slots.add(b.options.size() - 1, new ArrayList<>());
                    this.changed();
                });
            } else {
                pm.item("Добавить вариант ответа", Icon.PLUS, null, () -> this.addAnswer(b)).enabled(() -> canAddAnswer(b));
            }

            pm.item("Убрать последний вариант", Icon.CLOSE, null, () -> {
                if (b.options.size() > 1) {
                    this.snapshot();
                    int i = b.options.size() - 1;
                    b.options.remove(i);
                    b.slots.remove(i);
                    this.changed();
                }
            }).enabled(() -> b.options.size() > 1);
        }

        if (hit.list == null) {
            pm.separator();
            pm.item("Удалить", Icon.TRASH, "Del", () -> this.deleteValue(b)).color(-1023878);
            pm.show(mx, my);
        } else {
            pm.separator();
            pm.item("Справка", Icon.BOOK, null, () -> Dialog.info(b.typeInfo().key(), b.typeInfo().doc()));
            pm.separator();
            pm.item("Удалить блок", Icon.TRASH, "Del", () -> this.deleteBlock(hit, false)).color(-1023878);
            if (hit.index < hit.list.size() - 1) {
                pm.item("Удалить вместе с нижними", Icon.TRASH, null, () -> this.deleteBlock(hit, true)).color(-1023878);
            }

            pm.show(mx, my);
        }
    }

    private void canvasMenu(int mx, int my, double wx, double wy) {
        PopupMenu pm = new PopupMenu();
        pm.item("Вставить", Icon.FILE, "Ctrl+V", () -> this.paste(wx, wy)).enabled(() -> this.clipboard != null);
        pm.item("Упорядочить блоки", Icon.LIST, null, this::cleanUp);
        pm.item("К началу сюжета", Icon.HOME, "Home", this::goHome);
        pm.item("Масштаб 100%", Icon.SEARCH, "Ctrl+0", () -> this.zoom = 1.0);
        pm.item("Свернуть всё", Icon.CHEVRON_RIGHT, null, () -> this.foldAll(true));
        pm.item("Развернуть всё", Icon.CHEVRON_DOWN, null, () -> this.foldAll(false));
        this.jumpItems(pm);
        pm.show(mx, my);
    }

    private void toggleFold(VisualScript.Block b) {
        this.snapshot();
        b.collapsed = !b.collapsed;
        this.changed();
    }

    private void foldAll(boolean fold) {
        this.snapshot();

        for (VisualScript.Stack s : this.script.stacks) {
            foldIn(s.blocks, fold);
        }

        this.changed();
    }

    private static void foldIn(List<VisualScript.Block> list, boolean fold) {
        for (VisualScript.Block b : list) {
            if (!b.slots.isEmpty()) {
                b.collapsed = fold;
            } else if (!fold) {
                b.collapsed = false;
            }

            for (List<VisualScript.Block> slot : b.slots) {
                foldIn(slot, fold);
            }
        }
    }

    private void jumpItems(PopupMenu pm) {
        List<VisualScript.Block> marks = new ArrayList<>();

        for (VisualScript.Stack s : this.script.stacks) {
            collectMarks(s.blocks, marks);
        }

        pm.separator();
        if (marks.isEmpty()) {
            pm.item("Перейти: меток нет", Icon.FLAG, null, () -> {}).enabled(() -> false);
        } else {
            for (VisualScript.Block m : marks) {
                boolean label = m.type.equals("label");
                pm.item((label ? "→ метка " : "→ функция ") + m.field("name").trim(), label ? Icon.FLAG : Icon.CODE, null, () -> this.jumpTo(m))
                    .color(label ? Theme.category("Поток") : Theme.category("Функции"));
            }
        }
    }

    private static void collectMarks(List<VisualScript.Block> list, List<VisualScript.Block> out) {
        for (VisualScript.Block b : list) {
            if ((b.type.equals("label") || b.type.equals("func")) && !b.field("name").isBlank()) {
                out.add(b);
            }

            for (List<VisualScript.Block> slot : b.slots) {
                collectMarks(slot, out);
            }
        }
    }

    private void jumpTo(VisualScript.Block target) {
        boolean unfolded = false;

        for (VisualScript.Stack s : this.script.stacks) {
            unfolded |= unfoldPath(s.blocks, target);
        }

        if (unfolded) {
            this.changed();
        }

        for (VisualScript.Stack s : this.script.stacks) {
            s.dirty = true;
            this.layoutStack(s);
        }

        this.viewX = target.lx + target.lw / 2.0 - this.canvasW() / 2.0 / this.zoom;
        this.viewY = target.ly - this.h / 3.0 / this.zoom;
        this.selected = target;
        this.flash = target;
        this.flashUntil = System.currentTimeMillis() + 1200L;
    }

    private static boolean unfoldPath(List<VisualScript.Block> list, VisualScript.Block target) {
        for (VisualScript.Block b : list) {
            if (b == target) {
                return true;
            }

            for (List<VisualScript.Block> slot : b.slots) {
                if (unfoldPath(slot, target)) {
                    b.collapsed = false;
                    return true;
                }
            }
        }

        return false;
    }

    private void paintFlash(Canvas c) {
        if (this.flash != null && System.currentTimeMillis() <= this.flashUntil) {
            float k = (float)(this.flashUntil - System.currentTimeMillis()) / 1200.0F;
            int a = (int)(Math.abs(Math.sin(k * Math.PI * 3.0)) * 200.0);
            c.outline(this.flash.lx - 3, this.flash.ly - 3, this.flash.lw + 6, this.flash.lh + 6, a << 24 | 16766287);
            c.outline(this.flash.lx - 2, this.flash.ly - 2, this.flash.lw + 4, this.flash.lh + 4, a << 24 | 16766287);
        }
    }

    private void duplicate(VisualScript.Block b) {
        this.snapshot();
        VisualScript.Stack s = new VisualScript.Stack();
        s.x = b.lx + 30;
        s.y = b.ly + 30;
        s.blocks.add(b.deepCopy());
        this.script.stacks.add(s);
        this.changed();
    }

    private void paste(double wx, double wy) {
        if (this.clipboard != null) {
            this.snapshot();
            VisualScript.Stack s = new VisualScript.Stack();
            s.x = wx;
            s.y = wy;
            s.blocks.add(this.clipboard.deepCopy());
            this.script.stacks.add(s);
            this.changed();
        }
    }

    private void deleteValue(VisualScript.Block value) {
        this.snapshot();

        for (VisualScript.Stack s : this.script.stacks) {
            removeValueIn(s.blocks, value);
        }

        this.script.stacks.removeIf(sx -> sx.blocks.isEmpty() || sx.blocks.size() == 1 && sx.blocks.get(0) == value);
        if (this.selected == value) {
            this.selected = null;
        }

        this.changed();
    }

    private static boolean removeValueIn(List<VisualScript.Block> list, VisualScript.Block value) {
        for (VisualScript.Block b : list) {
            Iterator<Entry<String, VisualScript.Block>> it = b.inputs.entrySet().iterator();

            while (it.hasNext()) {
                Entry<String, VisualScript.Block> e = it.next();
                if (e.getValue() == value) {
                    it.remove();
                    return true;
                }

                if (removeValueIn(List.of(e.getValue()), value)) {
                    return true;
                }
            }

            for (List<VisualScript.Block> slot : b.slots) {
                if (removeValueIn(slot, value)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void deleteBlock(BlockEditor.Hit hit, boolean withBelow) {
        this.snapshot();
        if (withBelow) {
            hit.list.subList(hit.index, hit.list.size()).clear();
        } else {
            hit.list.remove(hit.index);
        }

        this.script.stacks.removeIf(s -> s.blocks.isEmpty());
        if (this.selected == hit.block) {
            this.selected = null;
        }

        this.changed();
    }

    private void cleanUp() {
        this.snapshot();
        double yy = 40.0;
        VisualScript.Stack main = this.script.mainStack();
        List<VisualScript.Stack> ordered = new ArrayList<>(this.script.stacks);
        if (main != null) {
            ordered.remove(main);
            ordered.add(0, main);
        }

        for (VisualScript.Stack s : ordered) {
            s.x = 40.0;
            s.y = yy;
            s.dirty = true;
            this.layoutStack(s);
            yy += s.bh + 40;
        }

        this.changed();
    }

    public void goHome() {
        VisualScript.Stack main = this.script.mainStack();
        if (main == null) {
            this.viewX = this.viewY = 0.0;
        } else {
            this.viewX = main.x - 40.0;
            this.viewY = main.y - 40.0;
        }
    }

    private BlockEditor.Hit findHit(VisualScript.Block target) {
        for (VisualScript.Stack s : this.script.stacks) {
            BlockEditor.Hit h = this.findIn(s.blocks, target, s);
            if (h != null) {
                return h;
            }
        }

        return null;
    }

    private BlockEditor.Hit findIn(List<VisualScript.Block> list, VisualScript.Block target, VisualScript.Stack s) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == target) {
                return new BlockEditor.Hit(target, list, i, s);
            }

            for (List<VisualScript.Block> slot : list.get(i).slots) {
                BlockEditor.Hit h = this.findIn(slot, target, s);
                if (h != null) {
                    return h;
                }
            }
        }

        return null;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = UI.ctrl(mods);
        if (ctrl) {
            switch (key) {
                case 48:
                    this.zoom = 1.0;
                    return true;
                case 67:
                    if (this.selected != null) {
                        this.clipboard = this.selected.deepCopy();
                    }

                    return true;
                case 68:
                    if (this.selected != null) {
                        this.duplicate(this.selected);
                    }

                    return true;
                case 70:
                    this.search.requestFocus();
                    return true;
                case 86:
                    UI ui = UI.get();
                    if (ui != null) {
                        this.paste(this.toWorldX(ui.mouseX()), this.toWorldY(ui.mouseY()));
                    }

                    return true;
                case 89:
                    this.redo();
                    return true;
                case 90:
                    if (UI.shift(mods)) {
                        this.redo();
                    } else {
                        this.undo();
                    }

                    return true;
                default:
                    return false;
            }
        } else {
            switch (key) {
                case 256:
                    if (this.selected != null) {
                        this.selected = null;
                        return true;
                    }

                    return false;
                case 259:
                case 261:
                    if (this.selected != null) {
                        BlockEditor.Hit h = this.findHit(this.selected);
                        if (h != null) {
                            this.deleteBlock(h, false);
                        } else {
                            this.deleteValue(this.selected);
                        }
                    }

                    return true;
                case 268:
                    this.goHome();
                    return true;
                default:
                    return false;
            }
        }
    }

    public interface AssetPicker {
        void pick(ParamType var1, String var2, Consumer<String> var3);
    }

    private record Hit(VisualScript.Block block, List<VisualScript.Block> list, int index, VisualScript.Stack stack) {
    }

    private static final class InlineEditor extends TextField implements UI.OverlayLayout {
        private final int ex;
        private final int ey;
        private final int ew;
        private final int eh;
        Consumer<String> commit;
        private boolean done;

        InlineEditor(int x, int y, int w, int h) {
            this.ex = x;
            this.ey = y;
            this.ew = w;
            this.eh = h;
            this.onSubmit = s -> this.close();
            this.onEscape = () -> {
                this.done = true;
                UI ui = UI.get();
                if (ui != null) {
                    ui.closeOverlay(this);
                }
            };
        }

        void commitNow() {
            if (!this.done) {
                this.done = true;
                if (this.commit != null) {
                    this.commit.accept(this.text().trim());
                }
            }
        }

        private void close() {
            this.commitNow();
            UI ui = UI.get();
            if (ui != null) {
                ui.closeOverlay(this);
            }
        }

        @Override
        public void focusLost() {
            this.close();
        }

        @Override
        public void layoutOverlay(int vw, int vh) {
            this.bounds(Math.min(this.ex, vw - this.ew - 2), this.ey, this.ew, this.eh);
        }
    }

    private static enum Mode {
        NONE,
        PAN,
        PRESS_BLOCK,
        DRAG,
        PRESS_PALETTE,
        PRESS_PILL,
        PRESS_VALUE;
    }

    private record PaletteEntry(VisualScript.Block block, String category, int y, boolean header) {
    }

    private record Snap(List<VisualScript.Block> list, int index, int x, int y, VisualScript.Stack stack) {
    }

    private record SocketTarget(VisualScript.Block block, String field, int x, int y, int w, int h) {
    }

    private record ValueHit(VisualScript.Block parent, String field, VisualScript.Block value) {
    }
}
