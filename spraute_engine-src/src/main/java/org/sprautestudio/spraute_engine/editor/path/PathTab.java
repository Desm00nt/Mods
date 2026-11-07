package org.sprautestudio.spraute_engine.editor.path;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.MiniMap;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.path.PathDef;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class PathTab extends EditorTab {
    private PathDef doc;
    private String saved;
    int sel = 0;
    private final Deque<String> history = new ArrayDeque<>();
    private final PathTab.Form form = new PathTab.Form();
    private final ScrollView inspector;
    private final PathTab.MapView map = new PathTab.MapView();
    private final List<Button> tools = new ArrayList<>();
    private static final List<String> MODES = List.of("once", "loop", "pingpong");
    private static final int LABEL_W = 122;
    private int toolbarH = 28;

    public PathTab(Project project, Project.FileRef file, String content) {
        super(project, file);
        this.doc = PathDef.fromJson(content);
        this.saved = this.doc.toJson();
        this.add(this.map);
        this.inspector = this.add(new ScrollView(this.form));
        Button world = this.add(new Button("Редактировать в мире", () -> PathWorldEditor.start(this)).icon(Icon.PLAY).style(Button.Style.PRIMARY));
        world.tooltip("Свободная камера: ПКМ по блоку — новая точка, ЛКМ по точке — взять и перенести");
        world.autoWidth();
        this.tools.add(world);
        this.tool("Точка — где я", Icon.USER, "Добавить точку там, где вы стоите (после выбранной)", () -> this.addPoint(standing()));
        this.tool("Точка — прицел", Icon.PIN, "Добавить точку на блок, на который вы смотрите", () -> this.addPoint(looking()));
        this.tool("Перевернуть", Icon.REFRESH, "Идти в обратном порядке", () -> {
            this.snapshot();
            Collections.reverse(this.doc.points);
            this.sel = this.doc.points.isEmpty() ? -1 : this.doc.points.size() - 1 - Math.max(0, this.sel);
            this.edited(true);
        });
        this.buildForm();
    }

    PathDef doc() {
        return this.doc;
    }

    private void tool(String text, Icon icon, String tip, Runnable r) {
        Button b = this.add(new Button(text, r).icon(icon).style(Button.Style.GHOST));
        b.tooltip(tip);
        b.autoWidth();
        this.tools.add(b);
    }

    String name() {
        return this.file != null ? this.file.name() : "путь";
    }

    void snapshot() {
        this.history.push(this.doc.toJson());

        while (this.history.size() > 100) {
            this.history.removeLast();
        }
    }

    boolean undoPath() {
        if (this.history.isEmpty()) {
            return false;
        } else {
            this.doc = PathDef.fromJson(this.history.pop());
            this.sel = Math.min(this.sel, this.doc.points.size() - 1);
            this.buildForm();
            return true;
        }
    }

    void edited(boolean rebuild) {
        this.doc.fix();
        if (rebuild) {
            this.buildForm();
        }
    }

    void saveNow() {
        try {
            this.save();
            ClientState.get().sources(this.project.name(), this.project.deploySources());
        } catch (IOException var2) {
        }
    }

    void addPoint(Vec3 at) {
        if (at != null) {
            this.snapshot();
            PathDef.Point p = new PathDef.Point(Math.floor(at.x) + 0.5, Math.floor(at.y), Math.floor(at.z) + 0.5);
            int i = this.sel >= 0 && this.sel < this.doc.points.size() ? this.sel + 1 : this.doc.points.size();
            this.doc.points.add(i, p);
            this.sel = i;
            LocalPlayer pl = Minecraft.getInstance().player;
            if (this.doc.dimension.isEmpty() && pl != null) {
                this.doc.dimension = Mc.level(pl).dimension().location().toString();
            }

            this.edited(true);
        }
    }

    void removePoint(int i) {
        if (i >= 0 && i < this.doc.points.size()) {
            this.snapshot();
            this.doc.points.remove(i);
            this.sel = Math.min(i, this.doc.points.size() - 1);
            this.edited(true);
        }
    }

    private void movePoint(int i, int d) {
        int j = i + d;
        if (i >= 0 && j >= 0 && j < this.doc.points.size()) {
            this.snapshot();
            Collections.swap(this.doc.points, i, j);
            this.sel = j;
            this.edited(true);
        }
    }

    private static Vec3 standing() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? null : new Vec3(p.getX(), Math.floor(p.getY() + 0.01), p.getZ());
    }

    private static Vec3 looking() {
        HitResult h = Minecraft.getInstance().hitResult;
        if (h instanceof BlockHitResult b && h.getType() == Type.BLOCK) {
            BlockPos up = b.getBlockPos().above();
            return new Vec3(up.getX(), up.getY(), up.getZ());
        } else {
            return null;
        }
    }

    static double groundAt(double x, double z, double fallback) {
        ClientLevel lv = Minecraft.getInstance().level;
        if (lv == null) {
            return fallback;
        } else {
            int bx = (int)Math.floor(x);
            int bz = (int)Math.floor(z);
            return !lv.hasChunk(bx >> 4, bz >> 4) ? fallback : lv.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        }
    }

    void buildForm() {
        this.form.clear();
        this.form.header("Путь");
        this.form.choice("Как идти", "До конца, по кругу или туда-обратно", () -> MODES, () -> this.doc.mode, v -> {
            return switch (v) {
                case "loop" -> "по кругу (без конца)";
                case "pingpong" -> "туда и обратно (без конца)";
                default -> "один раз до конца";
            };
        }, v -> this.doc.mode = v);
        this.form
            .number(
                "Скорость",
                "Одна для всех персонажей: 1 — обычный шаг (2.6 бл/с), 0.5 — медленно, от 1.6 — быстрый шаг, от 2 — бег",
                () -> this.doc.speed,
                v -> this.doc.speed = Math.max(0.05, v)
            );
        this.form.note(this.doc.points.size() + " точек · длина " + fmt(this.doc.length()) + " блоков");
        this.form.header("Точки — " + this.doc.points.size());
        this.form.points();
        if (this.sel >= 0 && this.sel < this.doc.points.size()) {
            PathDef.Point p = this.doc.points.get(this.sel);
            this.form.header("Точка " + (this.sel + 1));
            this.form.number("X", null, () -> p.x, v -> p.x = v);
            this.form.number("Y", "Высота (пол под ногами)", () -> p.y, v -> p.y = v);
            this.form.number("Z", null, () -> p.z, v -> p.z = v);
            this.form.number("Постоять, сек", "Сколько стоять на точке, прежде чем идти дальше", () -> p.wait, v -> p.wait = Math.max(0.0, v));
            this.form.number("Скорость к ней", "0 — как у пути; 1 — обычный шаг", () -> p.speed, v -> p.speed = Math.max(0.0, v));
            this.form.anim("Анимация стоя", () -> p.anim, v -> p.anim = v);
            this.form
                .number(
                    "Смотреть (угол)",
                    "Куда повернуться стоя: 0 — юг, 90 — запад, 180 — север, -90 — восток. Пусто/NaN — как пришёл",
                    () -> p.yaw == null ? Double.NaN : p.yaw,
                    v -> p.yaw = Double.isNaN(v) ? null : v
                );
            this.form.button("Повернуть как я смотрю", () -> {
                LocalPlayer pl = Minecraft.getInstance().player;
                if (pl != null) {
                    p.yaw = (double)Math.round(pl.getYRot());
                    this.edited(true);
                }
            });
            this.form.button("Перенести сюда (где я стою)", () -> {
                Vec3 at = standing();
                if (at != null) {
                    this.snapshot();
                    p.x = Math.floor(at.x) + 0.5;
                    p.y = at.y;
                    p.z = Math.floor(at.z) + 0.5;
                    this.edited(true);
                }
            });
        }

        this.form.header("В сюжете");
        this.form.note("walk alice \"" + this.name() + "\"   — пройти (сюжет ждёт конца)");
        this.form.note("walk alice \"" + this.name() + "\" nowait   — идти, а сюжет дальше");
        this.form.note("walk alice \"" + this.name() + "\" loop   ·   pingpong   ·   speed 1.4");
        this.form.note("stopwalk alice   ·   on pathend \"" + this.name() + "\" { … }");
        if (this.w > 0) {
            this.layout();
        }
    }

    static String fmt(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == (long)r ? String.valueOf((long)r) : String.valueOf(r);
    }

    @Override
    public void layout() {
        int bx = this.x + 6;
        int by = this.y + 4;

        for (Button b : this.tools) {
            if (bx + b.prefW > this.x + this.w - 6 && bx > this.x + 6) {
                bx = this.x + 6;
                by += 24;
            }

            b.bounds(bx, by, b.prefW, 20);
            bx += b.prefW + 4;
        }

        this.toolbarH = by - this.y + 24;
        int iw = Math.min(340, this.w / 2);
        this.map.bounds(this.x, this.y + this.toolbarH, this.w - iw, this.h - this.toolbarH);
        this.inspector.bounds(this.x + this.w - iw, this.y + this.toolbarH, iw, this.h - this.toolbarH);
        this.form.layout();
        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        c.fill(this.x, this.y, this.w, this.toolbarH, -14934490);
        c.fill(this.x, this.y + this.toolbarH - 1, this.w, 1, -13749952);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (UI.ctrl(mods) && key == 90) {
            return this.undoPath();
        } else if (key == 261 && this.sel >= 0) {
            this.removePoint(this.sel);
            return true;
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }

    public void closed() {
        if (PathWorldEditor.tab() == this) {
            PathWorldEditor.stopEditing();
        }
    }

    @Override
    public String title() {
        return this.file != null ? this.file.name() : "Путь";
    }

    @Override
    public Icon icon() {
        return Icon.ARROW_RIGHT;
    }

    @Override
    public int iconColor() {
        return -21965;
    }

    @Override
    public boolean isDirty() {
        return !this.doc.toJson().equals(this.saved);
    }

    @Override
    public void save() throws IOException {
        this.doc.fix();
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

        PathTab.Row row(String label, String doc) {
            PathTab.Row r = this.add(PathTab.this.new Row(label));
            if (doc != null) {
                r.tooltip(doc);
            }

            return r;
        }

        void number(String label, String doc, DoubleSupplier get, DoubleConsumer set) {
            PathTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            double v0 = get.getAsDouble();
            f.setText(Double.isNaN(v0) ? "" : PathTab.fmt(v0));
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.';
            f.onChange = s -> {
                String t = s.trim();

                try {
                    set.accept(t.isEmpty() ? Double.NaN : Double.parseDouble(t));
                    PathTab.this.edited(false);
                } catch (NumberFormatException var5x) {
                }
            };
            r.editor = r.add(f);
        }

        void anim(String label, final Supplier<String> get, final Consumer<String> set) {
            PathTab.Row r = this.row(label, "Анимация, пока стоит на точке");
            r.editor = r.add(new Widget() {
                final TextField f = this.add(new TextField());
                final Button pick = this.add(Button.icon(Icon.SEARCH, "Выбрать анимацию…", () -> AssetPicker.open(ParamType.ANIM, get.get(), v -> {
                    set.accept(v);
                    this.f.setText(v);
                    PathTab.this.edited(false);
                })));

                {
                    this.f.setText(get.get());
                    this.f.placeholder("нет");
                    this.f.onChange = s -> {
                        set.accept(s.trim());
                        PathTab.this.edited(false);
                    };
                }

                @Override
                public void layout() {
                    this.f.bounds(this.x, this.y, this.w - 22, this.h);
                    this.pick.bounds(this.x + this.w - 20, this.y, 20, this.h);
                    super.layout();
                }
            });
        }

        void choice(String label, String doc, Supplier<List<String>> options, Supplier<String> get, Function<String, String> labeler, Consumer<String> set) {
            PathTab.Row r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(options, get, labeler, v -> {
                set.accept(v);
                PathTab.this.edited(true);
            }));
        }

        void button(String text, Runnable run) {
            PathTab.Row row = this.row("", null);
            row.editor = row.add(new Button(text, run).style(Button.Style.GHOST));
        }

        void points() {
            this.add(
                new Widget() {
                    {
                        this.prefH = Math.max(1, PathTab.this.doc.points.size()) * 20 + 6;
                    }

                    @Override
                    protected void paint(Canvas c, int mx, int my, float dt) {
                        if (PathTab.this.doc.points.isEmpty()) {
                            c.text("Точек нет — добавьте кнопками сверху или ПКМ по карте", this.x + 10, this.y + 6, -9933435);
                        } else {
                            for (int i = 0; i < PathTab.this.doc.points.size(); i++) {
                                PathDef.Point p = PathTab.this.doc.points.get(i);
                                int ry = this.y + 3 + i * 20;
                                boolean on = i == PathTab.this.sel;
                                boolean hov = this.isHovered() && my >= ry && my < ry + 18;
                                c.round(this.x + 8, ry, this.w - 16, 18, 4, on ? 1435200767 : (hov ? -14210506 : -15592678));
                                c.text(String.valueOf(i + 1), this.x + 14, ry + 5, on ? -1841682 : -6577995);
                                String s = PathTab.fmt(p.x)
                                    + "  "
                                    + PathTab.fmt(p.y)
                                    + "  "
                                    + PathTab.fmt(p.z)
                                    + (p.wait > 0.0 ? "   ⏸ " + PathTab.fmt(p.wait) + "с" : "")
                                    + (p.anim.isEmpty() ? "" : "   ♪");
                                c.textEllipsized(s, this.x + 34, ry + 5, this.w - 110, -9933435);
                                if (on) {
                                    String[] b = new String[]{"▲", "▼", "✕"};

                                    for (int k = 0; k < 3; k++) {
                                        c.text(b[k], this.x + this.w - 60 + k * 16, ry + 5, k == 2 ? -1023878 : -1841682);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public boolean mousePressed(double mx, double my, int button) {
                        int i = (int)Math.floor((my - this.y - 3.0) / 20.0);
                        if (i >= 0 && i < PathTab.this.doc.points.size()) {
                            if (i == PathTab.this.sel && mx >= this.x + this.w - 62) {
                                int k = (int)((mx - (this.x + this.w - 62)) / 16.0);
                                if (k == 0) {
                                    PathTab.this.movePoint(i, -1);
                                } else if (k == 1) {
                                    PathTab.this.movePoint(i, 1);
                                } else {
                                    PathTab.this.removePoint(i);
                                }

                                return true;
                            } else {
                                PathTab.this.sel = i;
                                PathTab.this.buildForm();
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            );
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

    private final class MapView extends Widget implements UI.CursorProvider {
        private double k = 4.0;
        private double ox;
        private double oz;
        private double zoom = 1.0;
        private int drag = -1;
        private boolean panning;
        private boolean userView;
        private double panFromX;
        private double panFromZ;
        private final MiniMap minimap = new MiniMap();

        private void fit() {
            LocalPlayer p = Minecraft.getInstance().player;
            double x0;
            double x1;
            double z0;
            double z1;
            if (PathTab.this.doc.points.isEmpty()) {
                double cx = p != null ? p.getX() : 0.0;
                double cz = p != null ? p.getZ() : 0.0;
                x0 = cx - 10.0;
                x1 = cx + 10.0;
                z0 = cz - 10.0;
                z1 = cz + 10.0;
            } else {
                z0 = Double.MAX_VALUE;
                x0 = Double.MAX_VALUE;
                z1 = -Double.MAX_VALUE;
                x1 = -Double.MAX_VALUE;

                for (PathDef.Point q : PathTab.this.doc.points) {
                    x0 = Math.min(x0, q.x);
                    x1 = Math.max(x1, q.x);
                    z0 = Math.min(z0, q.z);
                    z1 = Math.max(z1, q.z);
                }
            }

            double span = Math.max(x1 - x0, z1 - z0) + 10.0;
            this.k = Math.max(0.5, Math.min(40.0, Math.min(this.w - 20, this.h - 40) / span)) * this.zoom;
            this.ox = (x0 + x1) / 2.0;
            this.oz = (z0 + z1) / 2.0;
        }

        private float sx(double wx) {
            return (float)(this.x + this.w / 2.0 + (wx - this.ox) * this.k);
        }

        private float sy(double wz) {
            return (float)(this.y + this.h / 2.0 + (wz - this.oz) * this.k);
        }

        private double wx(double px) {
            return this.ox + (px - this.x - this.w / 2.0) / this.k;
        }

        private double wz(double py) {
            return this.oz + (py - this.y - this.h / 2.0) / this.k;
        }

        private int pointAt(double mx, double my) {
            for (int i = PathTab.this.doc.points.size() - 1; i >= 0; i--) {
                PathDef.Point p = PathTab.this.doc.points.get(i);
                if (Math.abs(mx - this.sx(p.x)) < 7.0 && Math.abs(my - this.sy(p.z)) < 7.0) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -15724007);
            if (this.drag < 0 && !this.panning && !this.userView) {
                this.fit();
            }

            c.pushClip(this.x, this.y, this.w, this.h);
            this.minimap.draw(c, this.x, this.y, this.w, this.h, this.ox, this.oz, this.k);
            int step = this.k >= 6.0 ? 1 : (this.k >= 1.5 ? 8 : 16);

            for (double gx = Math.floor(this.wx(this.x) / step) * step; gx <= this.wx(this.x + this.w); gx += step) {
                c.fill(Math.round(this.sx(gx)), this.y, 1, this.h, Math.floorMod((long)gx, 16) == 0 ? 1090519039 : 385875967);
            }

            for (double gz = Math.floor(this.wz(this.y) / step) * step; gz <= this.wz(this.y + this.h); gz += step) {
                c.fill(this.x, Math.round(this.sy(gz)), this.w, 1, Math.floorMod((long)gz, 16) == 0 ? 1090519039 : 385875967);
            }

            int col = PathTab.this.doc.color | 0xFF000000;
            List<PathDef.Point> pts = PathTab.this.doc.points;

            for (int i = 1; i < pts.size(); i++) {
                PathDef.Point a = pts.get(i - 1);
                PathDef.Point b = pts.get(i);
                c.line(this.sx(a.x), this.sy(a.z), this.sx(b.x), this.sy(b.z), 2.0F, col);
                this.arrow(c, this.sx(a.x), this.sy(a.z), this.sx(b.x), this.sy(b.z), col);
            }

            if (PathTab.this.doc.mode.equals("loop") && pts.size() > 2) {
                PathDef.Point a = pts.get(pts.size() - 1);
                PathDef.Point b = pts.get(0);
                c.line(this.sx(a.x), this.sy(a.z), this.sx(b.x), this.sy(b.z), 1.0F, col & 16777215 | -2013265920);
            }

            for (int i = 0; i < pts.size(); i++) {
                PathDef.Point p = pts.get(i);
                int px = Math.round(this.sx(p.x));
                int pz = Math.round(this.sy(p.z));
                boolean on = i == PathTab.this.sel;
                boolean hov = Math.abs(mx - px) < 7 && Math.abs(my - pz) < 7;
                c.round(px - 6, pz - 6, 12, 12, 6, on ? -8070 : (hov ? -1 : -14539474));
                c.round(px - 5, pz - 5, 10, 10, 5, i == 0 ? -8658806 : (i == pts.size() - 1 ? -38037 : col));
                c.text(String.valueOf(i + 1), px + 8, pz - 4, -1);
                if (p.wait > 0.0) {
                    c.text("⏸" + PathTab.fmt(p.wait), px + 8, pz + 5, -5195576);
                }
            }

            LocalPlayer pl = Minecraft.getInstance().player;
            if (pl != null) {
                float px = this.sx(pl.getX());
                float pz = this.sy(pl.getZ());
                double yaw = Math.toRadians(pl.getYRot());
                c.line(px, pz, px - (float)Math.sin(yaw) * 10.0F, pz + (float)Math.cos(yaw) * 10.0F, 1.5F, -1);
                c.round(Math.round(px) - 3, Math.round(pz) - 3, 6, 6, 3, -11156993);
            }

            c.popClip();
            c.text(pts.size() + " точек · " + PathTab.fmt(PathTab.this.doc.length()) + " блоков", this.x + 8, this.y + 8, -6577995);
            c.textEllipsized(
                "Тяните точки · ПКМ — новая точка на земле · средняя кнопка — двигать вид · колесо — масштаб · зелёная — начало, красная — конец",
                this.x + 8,
                this.y + this.h - 14,
                this.w - 16,
                -9933435
            );
        }

        private void arrow(Canvas c, float x0, float y0, float x1, float y1, int col) {
            float dx = x1 - x0;
            float dy = y1 - y0;
            float len = (float)Math.hypot(dx, dy);
            if (!(len < 24.0F)) {
                float mx = (x0 + x1) / 2.0F;
                float my = (y0 + y1) / 2.0F;
                float ux = dx / len;
                float uy = dy / len;
                c.line(mx, my, mx - ux * 6.0F - uy * 4.0F, my - uy * 6.0F + ux * 4.0F, 2.0F, col);
                c.line(mx, my, mx - ux * 6.0F + uy * 4.0F, my - uy * 6.0F - ux * 4.0F, 2.0F, col);
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            if (button == 2) {
                this.panning = this.userView = true;
                this.panFromX = this.wx(mx);
                this.panFromZ = this.wz(my);
                return true;
            } else {
                int i = this.pointAt(mx, my);
                if (button == 1) {
                    if (i >= 0) {
                        PathTab.this.removePoint(i);
                        return true;
                    } else {
                        double wxx = this.wx(mx);
                        double wzz = this.wz(my);
                        LocalPlayer pl = Minecraft.getInstance().player;
                        double fallback = PathTab.this.sel >= 0 && PathTab.this.sel < PathTab.this.doc.points.size()
                            ? PathTab.this.doc.points.get(PathTab.this.sel).y
                            : (pl != null ? Math.floor(pl.getY()) : 64.0);
                        PathTab.this.addPoint(new Vec3(wxx, PathTab.groundAt(wxx, wzz, fallback), wzz));
                        return true;
                    }
                } else {
                    if (i >= 0) {
                        PathTab.this.snapshot();
                        this.drag = i;
                        PathTab.this.sel = i;
                        PathTab.this.buildForm();
                    }

                    return true;
                }
            }
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.panning) {
                this.ox = this.ox - (this.wx(mx) - this.panFromX);
                this.oz = this.oz - (this.wz(my) - this.panFromZ);
                return true;
            } else if (this.drag >= 0 && this.drag < PathTab.this.doc.points.size()) {
                PathDef.Point p = PathTab.this.doc.points.get(this.drag);
                p.x = Math.floor(this.wx(mx)) + 0.5;
                p.z = Math.floor(this.wz(my)) + 0.5;
                p.y = PathTab.groundAt(p.x, p.z, p.y);
                return true;
            } else {
                return true;
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            this.panning = false;
            if (this.drag >= 0) {
                this.drag = -1;
                PathTab.this.buildForm();
            }

            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            double f = delta > 0.0 ? 1.2 : 0.8333333333333334;
            this.zoom = Math.max(0.2, Math.min(8.0, this.zoom * f));
            this.k = Math.max(0.1, Math.min(80.0, this.k * f));
            this.userView = this.userView || this.drag >= 0;
            return true;
        }

        @Override
        public int cursor(int mx, int my) {
            return this.pointAt(mx, my) >= 0 ? 221188 : 221185;
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
                this.editor.bounds(this.x + 122, this.y + 2, this.w - 122 - 10, 18);
                this.editor.layout();
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.textEllipsized(this.label, this.x + 10, this.y + 7, 108, -6577995);
        }
    }
}
