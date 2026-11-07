package org.sprautestudio.spraute_engine.editor.zone;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.MiniMap;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;
import org.sprautestudio.spraute_engine.zone.ZoneDef;
import org.sprautestudio.spraute_engine.zone.ZoneShape;
import org.sprautestudio.spraute_engine.zone.client.ZoneClient;

public class ZoneTab extends EditorTab {
    private ZoneDef doc;
    private String saved;
    private final ZoneTab.Form form = new ZoneTab.Form();
    private final ScrollView inspector;
    private final ZoneTab.MapView map = new ZoneTab.MapView();
    private final List<Button> tools = new ArrayList<>();
    private static final List<String> SHAPES = List.of("box", "sphere", "cylinder");
    private static final List<String> TITLE_MODES = List.of("first", "always", "never");
    private static final List<String> WALLS = List.of("none", "in", "out");
    private final ArrayDeque<String> history = new ArrayDeque<>();
    private static final int LABEL_W = 122;
    private static final int INSPECTOR_W = 340;
    private int toolbarH = 28;

    public ZoneTab(Project project, Project.FileRef file, String content) {
        super(project, file);
        this.doc = ZoneDef.fromJson(content);
        this.saved = this.doc.toJson();
        this.add(this.map);
        this.inspector = this.add(new ScrollView(this.form));
        this.changed();
        this.rebuildAll();
    }

    private void tool(String text, Icon icon, String tip, Runnable r) {
        Button b = this.add(new Button(text, r).icon(icon).style(Button.Style.GHOST));
        b.tooltip(tip);
        b.autoWidth();
        this.tools.add(b);
    }

    private String name() {
        return this.file != null ? this.file.name() : "зона";
    }

    private void changed() {
        this.doc.fix();
        ZoneClient.LIVE.put(this.name(), this.doc);
        ZoneClient.highlighted = this.name();
    }

    private static BlockPos standing() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? null : p.blockPosition();
    }

    private static BlockPos looking() {
        HitResult h = Minecraft.getInstance().hitResult;
        return h instanceof BlockHitResult b && h.getType() == Type.BLOCK ? b.getBlockPos() : null;
    }

    private void corner(int which, boolean look) {
        BlockPos pos = look ? looking() : standing();
        if (pos != null) {
            if (which == 1) {
                this.doc.x1 = pos.getX();
                this.doc.y1 = pos.getY();
                this.doc.z1 = pos.getZ();
            } else {
                this.doc.x2 = pos.getX();
                this.doc.y2 = pos.getY();
                this.doc.z2 = pos.getZ();
            }

            this.changed();
            this.buildForm();
        }
    }

    private void centerHere() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            if (this.doc.shape.equals("box")) {
                int w = Math.abs(this.doc.x2 - this.doc.x1);
                int h = Math.abs(this.doc.y2 - this.doc.y1);
                int d = Math.abs(this.doc.z2 - this.doc.z1);
                BlockPos b = p.blockPosition();
                this.doc.x1 = b.getX() - w / 2;
                this.doc.x2 = this.doc.x1 + w;
                this.doc.y1 = b.getY() - 1;
                this.doc.y2 = this.doc.y1 + h;
                this.doc.z1 = b.getZ() - d / 2;
                this.doc.z2 = this.doc.z1 + d;
            } else {
                this.doc.cx = Math.floor(p.getX()) + 0.5;
                this.doc.cy = Math.floor(p.getY());
                this.doc.cz = Math.floor(p.getZ()) + 0.5;
            }

            if (this.doc.dimension.isEmpty() && Mc.level(p) != null) {
                this.doc.dimension = Mc.level(p).dimension().location().toString();
            }

            this.changed();
            this.buildForm();
        }
    }

    private void buildForm() {
        this.form.clear();
        this.form.header("Зона");
        this.form.choice("Форма", null, () -> SHAPES, () -> this.doc.shape, v -> {
            return switch (v) {
                case "sphere" -> "шар";
                case "cylinder" -> "цилиндр (столб)";
                default -> "коробка из блоков";
            };
        }, this::setShape);
        this.form.note(this.doc.size());
        this.form.header("Части зоны — " + this.doc.pieces());
        this.form.note("Зона — все части вместе (вход в любую = вход в зону)");
        this.form.pieces();
        this.form.header("Часть " + (this.doc.editIndex + 1) + " — форма и размер");
        if (this.doc.shape.equals("box")) {
            this.form.integers("Угол 1", () -> new int[]{this.doc.x1, this.doc.y1, this.doc.z1}, v -> {
                this.doc.x1 = v[0];
                this.doc.y1 = v[1];
                this.doc.z1 = v[2];
            });
            this.form.integers("Угол 2", () -> new int[]{this.doc.x2, this.doc.y2, this.doc.z2}, v -> {
                this.doc.x2 = v[0];
                this.doc.y2 = v[1];
                this.doc.z2 = v[2];
            });
        } else {
            this.form.integers("Центр", () -> new int[]{(int)Math.floor(this.doc.cx), (int)Math.floor(this.doc.cy), (int)Math.floor(this.doc.cz)}, v -> {
                this.doc.cx = v[0] + 0.5;
                this.doc.cy = v[1];
                this.doc.cz = v[2] + 0.5;
            });
            this.form.number("Радиус", null, () -> this.doc.radius, v -> this.doc.radius = v);
            if (this.doc.shape.equals("cylinder")) {
                this.form.number("Высота", "Вверх от центра", () -> this.doc.height, v -> this.doc.height = v);
            }
        }

        this.form.text("Мир", "Пусто — в любом. minecraft:overworld, minecraft:the_nether…", () -> this.doc.dimension, v -> this.doc.dimension = v.trim());
        this.form.check("Включена", "Выключенная зона ничего не слышит (в сюжете: zone on / zone off)", () -> this.doc.enabled, v -> this.doc.enabled = v);
        this.form
            .check(
                "Скрывать в игре",
                "Включено — контур виден только в 3D-редакторе. Выключено — все игроки видят границу зоны в мире",
                () -> this.doc.hideInGame,
                v -> this.doc.hideInGame = v
            );
        this.form.color("Цвет контура", () -> this.doc.color, v -> this.doc.color = v);
        this.form.header("Название на экране");
        this.form.text("Название", "Крупно посреди экрана при входе (пусто — не показывать)", () -> this.doc.title, v -> this.doc.title = v);
        this.form.text("Подпись", null, () -> this.doc.subtitle, v -> this.doc.subtitle = v);
        this.form.choice("Когда", null, () -> TITLE_MODES, () -> this.doc.titleMode, v -> {
            return switch (v) {
                case "always" -> "при каждом входе";
                case "never" -> "никогда";
                default -> "только в первый раз";
            };
        }, v -> this.doc.titleMode = v);
        this.form.header("Стена");
        this.form.choice("Граница", "Невидимая стена по краю зоны (в сюжете: zone wall)", () -> WALLS, () -> this.doc.wall, v -> {
            return switch (v) {
                case "in" -> "не выпускать изнутри";
                case "out" -> "не пускать внутрь";
                default -> "нет — проходи свободно";
            };
        }, v -> {
            this.doc.wall = v;
            this.buildForm();
        });
        if (!this.doc.wall.equals("none")) {
            this.form.text("Сообщение", "Над хотбаром, когда упёрся", () -> this.doc.wallText, v -> this.doc.wallText = v);
        }

        String n = this.name();
        this.form.header("В сюжете");
        this.form.note("on zoneenter \"" + n + "\" { … }   ·   on zoneleave \"" + n + "\" { … }");
        this.form.note("wait zone \"" + n + "\"   ·   wait outside \"" + n + "\"");
        this.form.note("if inzone(\"" + n + "\") { … }   ·   inzone(\"" + n + "\", alice)");
        this.form.note("zone off \"" + n + "\"   ·   zone wall \"" + n + "\" \"in\"");
        this.form.note("summon(\"minecraft:zombie\", zonepoint(\"" + n + "\"))");
        if (this.w > 0) {
            this.layout();
        }
    }

    private void setShape(String v) {
        if (!this.doc.shape.equals(v)) {
            Vec3 c = this.doc.center();
            double half = Math.max(this.doc.maxX() - this.doc.minX(), this.doc.maxZ() - this.doc.minZ()) / 2.0;
            this.doc.shape = v;
            if (v.equals("box")) {
                int r = (int)Math.max(1L, Math.round(half));
                this.doc.x1 = (int)Math.floor(c.x) - r;
                this.doc.x2 = (int)Math.floor(c.x) + r;
                this.doc.z1 = (int)Math.floor(c.z) - r;
                this.doc.z2 = (int)Math.floor(c.z) + r;
                this.doc.y1 = (int)Math.floor(c.y);
                this.doc.y2 = this.doc.y1 + 5;
            } else {
                this.doc.cx = c.x;
                this.doc.cz = c.z;
                this.doc.cy = c.y;
                this.doc.radius = Math.max(1.0, half);
            }
        }

        this.changed();
        this.rebuildAll();
    }

    ZoneDef doc() {
        return this.doc;
    }

    void snapshot() {
        this.history.push(this.doc.toJson());

        while (this.history.size() > 100) {
            this.history.removeLast();
        }
    }

    boolean undoZone() {
        if (this.history.isEmpty()) {
            return false;
        } else {
            ZoneDef old = ZoneDef.fromJson(this.history.pop());
            boolean shape = !old.shape.equals(this.doc.shape);
            this.doc = old;
            this.changed();
            if (shape) {
                this.rebuildAll();
            } else {
                this.buildForm();
            }

            return true;
        }
    }

    void edited(boolean rebuild) {
        this.changed();
        if (rebuild) {
            this.buildForm();
        }
    }

    void cycleShape() {
        String var1 = this.doc.shape;

        this.setShape(switch (var1) {
            case "box" -> "sphere";
            case "sphere" -> "cylinder";
            default -> "box";
        });
    }

    void saveNow() {
        try {
            this.save();
            ClientState.get().sources(this.project.name(), this.project.deploySources());
        } catch (IOException var2) {
        }
    }

    void newPiece() {
        this.snapshot();
        this.doc.addPiece();
        this.changed();
        this.rebuildAll();
    }

    void nextPiece() {
        if (!this.doc.more.isEmpty()) {
            this.doc.nextPiece();
            this.changed();
            this.rebuildAll();
        }
    }

    boolean removePiece() {
        this.snapshot();
        boolean ok = this.doc.removePiece();
        this.changed();
        this.rebuildAll();
        return ok;
    }

    private void selectPiece(int k) {
        if (k != this.doc.editIndex) {
            this.doc.editPiece(k);
            this.changed();
            this.rebuildAll();
        }
    }

    private void rebuildAll() {
        for (Button b : this.tools) {
            this.remove(b);
        }

        this.tools.clear();
        Button world = this.add(new Button("Редактировать в мире", () -> ZoneWorldEditor.start(this)).icon(Icon.PLAY).style(Button.Style.PRIMARY));
        world.tooltip("Свободная камера в мире: ПКМ по блокам — углы, ЛКМ по меткам — тянуть грани, G — перенести зону");
        world.autoWidth();
        this.tools.add(world);
        if (this.doc.shape.equals("box")) {
            this.tool("Угол 1 — я", Icon.USER, "Первый угол — блок, где вы стоите", () -> this.corner(1, false));
            this.tool("Угол 2 — я", Icon.USER, "Второй угол — блок, где вы стоите", () -> this.corner(2, false));
            this.tool("Угол 1 — прицел", Icon.PIN, "Первый угол — блок, на который вы смотрите", () -> this.corner(1, true));
            this.tool("Угол 2 — прицел", Icon.PIN, "Второй угол — блок, на который вы смотрите", () -> this.corner(2, true));
        }

        this.tool("Центр — я", Icon.HOME, "Центр зоны там, где вы стоите", this::centerHere);
        this.buildForm();
    }

    private static String fmt(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == (long)r ? String.valueOf((long)r) : String.valueOf(r);
    }

    @Override
    public void layout() {
        int bx = this.x + 6;
        int by = this.y + 4;

        for (Button b : new ArrayList<>(this.tools)) {
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
        ZoneClient.LIVE.put(this.name(), this.doc);
        ZoneClient.highlighted = this.name();
    }

    public void closed() {
        ZoneClient.LIVE.remove(this.name());
        if (ZoneClient.highlighted.equals(this.name())) {
            ZoneClient.highlighted = "";
        }
    }

    @Override
    public String title() {
        return this.file != null ? this.file.name() : "Зона";
    }

    @Override
    public Icon icon() {
        return Icon.PIN;
    }

    @Override
    public int iconColor() {
        return -11549705;
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
        private static final int[] COLORS = new int[]{-11549705, -8658806, -11686, -28838, -38037, -3697942, -1};

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

        ZoneTab.Row row(String label, String doc) {
            ZoneTab.Row r = this.add(ZoneTab.this.new Row(label));
            if (doc != null) {
                r.tooltip(doc);
            }

            return r;
        }

        void text(String label, String doc, Supplier<String> get, Consumer<String> set) {
            ZoneTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(get.get());
            f.onChange = s -> {
                set.accept(s);
                ZoneTab.this.changed();
            };
            r.editor = r.add(f);
        }

        void number(String label, String doc, DoubleSupplier get, DoubleConsumer set) {
            ZoneTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(ZoneTab.fmt(get.getAsDouble()));
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.';
            f.onChange = s -> {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                    ZoneTab.this.changed();
                } catch (NumberFormatException var4x) {
                }
            };
            r.editor = r.add(f);
        }

        void integers(String label, final Supplier<int[]> get, final Consumer<int[]> set) {
            ZoneTab.Row r = this.row(label, "x  y  z");
            r.editor = r.add(new Widget() {
                final TextField[] f = new TextField[]{this.add(new TextField()), this.add(new TextField()), this.add(new TextField())};

                {
                    int[] v = get.get();

                    for (int i = 0; i < 3; i++) {
                        this.f[i].setText(String.valueOf(v[i]));
                        this.f[i].filter = ch -> Character.isDigit(ch) || ch == '-';
                        this.f[i].onChange = s -> {
                            try {
                                int[] nv = new int[3];

                                for (int k = 0; k < 3; k++) {
                                    nv[k] = Integer.parseInt(this.f[k].text().trim());
                                }

                                set.accept(nv);
                                ZoneTab.this.changed();
                            } catch (NumberFormatException var5x) {
                            }
                        };
                    }
                }

                @Override
                public void layout() {
                    int cw = (this.w - 8) / 3;

                    for (int i = 0; i < 3; i++) {
                        this.f[i].bounds(this.x + i * (cw + 4), this.y, cw, this.h);
                    }

                    super.layout();
                }
            });
        }

        void pieces() {
            this.add(new Widget() {
                {
                    this.prefH = ZoneTab.this.doc.pieces() * 22 + 6 + 26;
                }

                private int rowAt(double my) {
                    return (int)Math.floor((my - this.y - 3.0) / 22.0);
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    List<ZoneShape> all = ZoneTab.this.doc.ordered();
                    int col = ZoneTab.this.doc.color | 0xFF000000;

                    for (int i = 0; i < all.size(); i++) {
                        int ry = this.y + 3 + i * 22;
                        boolean on = i == ZoneTab.this.doc.editIndex;
                        boolean hov = this.isHovered() && this.rowAt(my) == i && my < this.y + 3 + all.size() * 22;
                        c.round(this.x + 8, ry, this.w - 16, 20, 4, on ? 1435200767 : (hov ? -14210506 : -15592678));
                        if (on) {
                            c.fill(this.x + 8, ry, 3, 20, -7639809);
                        }

                        c.round(this.x + 16, ry + 6, 8, 8, 2, on ? col : col & 16777215 | -2013265920);
                        c.text("Часть " + (i + 1), this.x + 30, ry + 6, on ? -1841682 : -6577995);
                        String sz = all.get(i).size();
                        int tagW = on ? c.width("правится") + 10 : 0;
                        c.textEllipsized(sz, this.x + 84, ry + 6, this.w - 100 - tagW, -9933435);
                        if (on) {
                            c.text("правится", this.x + this.w - 14 - c.width("правится"), ry + 6, -7639809);
                        }
                    }

                    int by = this.y + 6 + all.size() * 22;
                    int bw = (this.w - 24) / 2;
                    boolean hovAdd = this.isHovered() && my >= by && my < by + 20 && mx < this.x + 8 + bw;
                    boolean hovDel = this.isHovered() && my >= by && my < by + 20 && mx >= this.x + 16 + bw;
                    c.round(this.x + 8, by, bw, 20, 4, hovAdd ? -7639809 : 1435200767);
                    c.text("+ Добавить часть", this.x + 8 + (bw - c.width("+ Добавить часть")) / 2, by + 6, -1841682);
                    boolean can = ZoneTab.this.doc.pieces() > 1;
                    c.round(this.x + 16 + bw, by, bw, 20, 4, can ? (hovDel ? -7720912 : -10869210) : -15592678);
                    c.text("Удалить эту", this.x + 16 + bw + (bw - c.width("Удалить эту")) / 2, by + 6, can ? -1841682 : -9933435);
                }

                @Override
                public boolean mousePressed(double mx, double my, int button) {
                    int n = ZoneTab.this.doc.pieces();
                    int by = this.y + 6 + n * 22;
                    int bw = (this.w - 24) / 2;
                    if (my >= by && my < by + 20) {
                        if (mx < this.x + 8 + bw) {
                            ZoneTab.this.newPiece();
                        } else if (mx >= this.x + 16 + bw && n > 1) {
                            ZoneTab.this.removePiece();
                        }

                        return true;
                    } else {
                        int i = this.rowAt(my);
                        if (i >= 0 && i < n) {
                            ZoneTab.this.selectPiece(i);
                        }

                        return true;
                    }
                }
            }).tooltip("Щёлкните часть, чтобы править её. На карте и в мире правимая часть яркая, остальные бледные");
        }

        void button(String text, Runnable r) {
            ZoneTab.Row row = this.row("", null);
            row.editor = row.add(new Button(text, r).style(Button.Style.GHOST));
        }

        void check(String label, String doc, BooleanSupplier get, Consumer<Boolean> set) {
            ZoneTab.Row r = this.row(label, doc);
            r.editor = r.add(new Checkbox("", get, v -> {
                set.accept(v);
                ZoneTab.this.changed();
            }));
        }

        void choice(String label, String doc, Supplier<List<String>> options, Supplier<String> get, Function<String, String> labeler, Consumer<String> set) {
            ZoneTab.Row r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(options, get, labeler, v -> {
                set.accept(v);
                ZoneTab.this.changed();
            }));
        }

        void color(String label, final IntSupplier get, final IntConsumer set) {
            ZoneTab.Row r = this.row(label, null);
            r.editor = r.add(new Widget() {
                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    for (int i = 0; i < ZoneTab.Form.COLORS.length; i++) {
                        int sx = this.x + i * 20;
                        if ((get.getAsInt() | 0xFF000000) == ZoneTab.Form.COLORS[i]) {
                            c.round(sx - 1, this.y - 1, 18, this.h + 2, 3, -1841682);
                        }

                        c.round(sx + 1, this.y + 1, 14, this.h - 2, 2, ZoneTab.Form.COLORS[i]);
                    }
                }

                @Override
                public boolean mousePressed(double mx, double my, int button) {
                    int i = (int)((mx - this.x) / 20.0);
                    if (i >= 0 && i < ZoneTab.Form.COLORS.length) {
                        set.accept(ZoneTab.Form.COLORS[i]);
                        ZoneTab.this.changed();
                    }

                    return true;
                }
            });
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
        private double k = 1.0;
        private double ox;
        private double oz;
        private String drag;
        private double grabX;
        private double grabZ;
        private double zoom = 1.0;
        private final MiniMap minimap = new MiniMap();

        private void fit() {
            double x0 = ZoneTab.this.doc.minX();
            double x1 = ZoneTab.this.doc.maxX();
            double z0 = ZoneTab.this.doc.minZ();
            double z1 = ZoneTab.this.doc.maxZ();
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null && Math.abs(p.getX() - (x0 + x1) / 2.0) < 120.0 && Math.abs(p.getZ() - (z0 + z1) / 2.0) < 120.0) {
                x0 = Math.min(x0, p.getX() - 2.0);
                x1 = Math.max(x1, p.getX() + 2.0);
                z0 = Math.min(z0, p.getZ() - 2.0);
                z1 = Math.max(z1, p.getZ() + 2.0);
            }

            double span = Math.max(x1 - x0, z1 - z0) + 8.0;
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

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -15724007);
            if (this.drag == null) {
                this.fit();
            }

            c.pushClip(this.x, this.y, this.w, this.h);
            this.minimap.draw(c, this.x, this.y, this.w, this.h, this.ox, this.oz, this.k);
            c.popClip();
            int step = this.k >= 6.0 ? 1 : (this.k >= 1.5 ? 8 : 16);

            for (double gx = Math.floor(this.wx(this.x) / step) * step; gx <= this.wx(this.x + this.w); gx += step) {
                int col = Math.floorMod((long)gx, 16) == 0 ? 1090519039 : 385875967;
                c.fill(Math.round(this.sx(gx)), this.y, 1, this.h, col);
            }

            for (double gz = Math.floor(this.wz(this.y) / step) * step; gz <= this.wz(this.y + this.h); gz += step) {
                int col = Math.floorMod((long)gz, 16) == 0 ? 1090519039 : 385875967;
                c.fill(this.x, Math.round(this.sy(gz)), this.w, 1, col);
            }

            int col = ZoneTab.this.doc.color | 0xFF000000;
            int fillc = ZoneTab.this.doc.color & 16777215 | 855638016;
            int dim = ZoneTab.this.doc.color & 16777215 | -2013265920;
            int dimFill = ZoneTab.this.doc.color & 16777215 | 402653184;

            for (ZoneShape sh : ZoneTab.this.doc.more) {
                if (sh.shape.equals("box")) {
                    int ax = Math.round(this.sx(sh.minX()));
                    int az = Math.round(this.sy(sh.minZ()));
                    int bx = Math.round(this.sx(sh.maxX()));
                    int bz = Math.round(this.sy(sh.maxZ()));
                    c.fill(ax, az, bx - ax, bz - az, dimFill);
                    c.outline(ax, az, bx - ax, bz - az, dim);
                } else {
                    float cx = this.sx(sh.cx);
                    float cz = this.sy(sh.cz);
                    float r = (float)(sh.radius * this.k);
                    int n = Math.max(32, (int)(r * 0.8));

                    for (int i = 0; i < n; i++) {
                        double a0 = (Math.PI * 2) * i / n;
                        double a1 = (Math.PI * 2) * (i + 1) / n;
                        c.line(
                            cx + (float)Math.cos(a0) * r, cz + (float)Math.sin(a0) * r, cx + (float)Math.cos(a1) * r, cz + (float)Math.sin(a1) * r, 1.0F, dim
                        );
                    }
                }
            }

            if (ZoneTab.this.doc.shape.equals("box")) {
                int ax = Math.round(this.sx(ZoneTab.this.doc.minX()));
                int az = Math.round(this.sy(ZoneTab.this.doc.minZ()));
                int bx = Math.round(this.sx(ZoneTab.this.doc.maxX()));
                int bz = Math.round(this.sy(ZoneTab.this.doc.maxZ()));
                c.fill(ax, az, bx - ax, bz - az, fillc);
                c.outline(ax, az, bx - ax, bz - az, col);
                this.handle(c, this.sx(ZoneTab.this.doc.x1 + 0.5), this.sy(ZoneTab.this.doc.z1 + 0.5), "1", mx, my);
                this.handle(c, this.sx(ZoneTab.this.doc.x2 + 0.5), this.sy(ZoneTab.this.doc.z2 + 0.5), "2", mx, my);
            } else {
                float cx = this.sx(ZoneTab.this.doc.cx);
                float cz = this.sy(ZoneTab.this.doc.cz);
                float r = (float)(ZoneTab.this.doc.radius * this.k);

                for (int row = (int)(-r); row <= r; row++) {
                    int half = (int)Math.sqrt(Math.max(0.0F, r * r - row * row));
                    c.fill(Math.round(cx - half), Math.round(cz + row), half * 2, 1, fillc);
                }

                int n = Math.max(32, (int)(r * 0.8));

                for (int i = 0; i < n; i++) {
                    double a0 = (Math.PI * 2) * i / n;
                    double a1 = (Math.PI * 2) * (i + 1) / n;
                    c.line(cx + (float)Math.cos(a0) * r, cz + (float)Math.sin(a0) * r, cx + (float)Math.cos(a1) * r, cz + (float)Math.sin(a1) * r, 1.0F, col);
                }

                this.handle(c, cx, cz, "•", mx, my);
                this.handle(c, cx + r, cz, "R", mx, my);
            }

            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) {
                float px = this.sx(p.getX());
                float pz = this.sy(p.getZ());
                if (px > this.x && px < this.x + this.w && pz > this.y && pz < this.y + this.h) {
                    double yaw = Math.toRadians(p.getYRot());
                    c.line(px, pz, px - (float)Math.sin(yaw) * 10.0F, pz + (float)Math.cos(yaw) * 10.0F, 1.5F, -1);
                    c.round(Math.round(px) - 3, Math.round(pz) - 3, 6, 6, 3, -1);
                    c.round(Math.round(px) - 2, Math.round(pz) - 2, 4, 4, 2, -11156993);
                }

                boolean in = ZoneTab.this.doc.holds(p.getX(), p.getY(), p.getZ())
                    && (ZoneTab.this.doc.dimension.isEmpty() || ZoneTab.this.doc.dimension.equals(Mc.level(p).dimension().location().toString()));
                String where = "Вы: "
                    + p.blockPosition().getX()
                    + " "
                    + p.blockPosition().getY()
                    + " "
                    + p.blockPosition().getZ()
                    + (in ? "  — внутри зоны" : "  — снаружи");
                c.text(where, this.x + 8, this.y + 8, in ? -8658806 : -9933435);
            }

            c.text(
                "Высота: " + ZoneTab.fmt(ZoneTab.this.doc.minY()) + " … " + ZoneTab.fmt(ZoneTab.this.doc.maxY()) + "   ·   " + ZoneTab.this.doc.size(),
                this.x + 8,
                this.y + 20,
                -9933435
            );
            c.textEllipsized(
                "Вид сверху: север вверху. Тяните метки, колесо — масштаб. В мире зона видна при /editor on",
                this.x + 8,
                this.y + this.h - 14,
                this.w - 16,
                -9933435
            );
        }

        private void handle(Canvas c, float hx, float hy, String label, int mx, int my) {
            boolean hov = Math.abs(mx - hx) < 6.0F && Math.abs(my - hy) < 6.0F;
            c.round(Math.round(hx) - 5, Math.round(hy) - 5, 10, 10, 2, !hov && !label.equals(this.drag) ? -1 : -8070);
            c.round(Math.round(hx) - 4, Math.round(hy) - 4, 8, 8, 2, ZoneTab.this.doc.color | 0xFF000000);
            c.text(label, Math.round(hx) + 7, Math.round(hy) - 4, -1);
        }

        private String handleAt(double mx, double my) {
            List<Object[]> hs = new ArrayList<>();
            if (ZoneTab.this.doc.shape.equals("box")) {
                hs.add(new Object[]{"1", this.sx(ZoneTab.this.doc.x1 + 0.5), this.sy(ZoneTab.this.doc.z1 + 0.5)});
                hs.add(new Object[]{"2", this.sx(ZoneTab.this.doc.x2 + 0.5), this.sy(ZoneTab.this.doc.z2 + 0.5)});
            } else {
                hs.add(new Object[]{"R", this.sx(ZoneTab.this.doc.cx) + (float)(ZoneTab.this.doc.radius * this.k), this.sy(ZoneTab.this.doc.cz)});
                hs.add(new Object[]{"•", this.sx(ZoneTab.this.doc.cx), this.sy(ZoneTab.this.doc.cz)});
            }

            for (Object[] h : hs) {
                if (Math.abs(mx - ((Float)h[1]).floatValue()) < 7.0 && Math.abs(my - ((Float)h[2]).floatValue()) < 7.0) {
                    return (String)h[0];
                }
            }

            return !ZoneTab.this.doc.containsMain(this.wx(mx), ZoneTab.this.doc.minY() + 0.01, this.wz(my))
                    && (!ZoneTab.this.doc.shape.equals("sphere") || !ZoneTab.this.doc.containsMain(this.wx(mx), ZoneTab.this.doc.cy, this.wz(my)))
                ? null
                : "move";
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            this.drag = this.handleAt(mx, my);
            if (this.drag == null) {
                double px = this.wx(mx);
                double pz = this.wz(my);

                for (int i = 0; i < ZoneTab.this.doc.more.size(); i++) {
                    ZoneShape sh = ZoneTab.this.doc.more.get(i);
                    if (px >= sh.minX() && px < sh.maxX() && pz >= sh.minZ() && pz < sh.maxZ()) {
                        ZoneTab.this.selectPiece(ZoneTab.this.doc.orderOfMore(i));
                        return true;
                    }
                }
            }

            this.grabX = this.wx(mx);
            this.grabZ = this.wz(my);
            return true;
        }

        @Override
        public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
            if (this.drag == null) {
                return true;
            } else {
                double px = this.wx(mx);
                double pz = this.wz(my);
                String var14 = this.drag;
                switch (var14) {
                    case "1":
                        ZoneTab.this.doc.x1 = (int)Math.floor(px);
                        ZoneTab.this.doc.z1 = (int)Math.floor(pz);
                        break;
                    case "2":
                        ZoneTab.this.doc.x2 = (int)Math.floor(px);
                        ZoneTab.this.doc.z2 = (int)Math.floor(pz);
                        break;
                    case "R":
                        ZoneTab.this.doc.radius = Math.max(1.0, Math.round(Math.hypot(px - ZoneTab.this.doc.cx, pz - ZoneTab.this.doc.cz) * 2.0) / 2.0);
                        break;
                    case "•":
                        ZoneTab.this.doc.cx = Math.floor(px) + 0.5;
                        ZoneTab.this.doc.cz = Math.floor(pz) + 0.5;
                        break;
                    case "move":
                        int mdx = (int)Math.round(px - this.grabX);
                        int mdz = (int)Math.round(pz - this.grabZ);
                        if (mdx == 0 && mdz == 0) {
                            return true;
                        }

                        ZoneTab.this.doc.x1 += mdx;
                        ZoneTab.this.doc.x2 += mdx;
                        ZoneTab.this.doc.z1 += mdz;
                        ZoneTab.this.doc.z2 += mdz;
                        ZoneTab.this.doc.cx += mdx;
                        ZoneTab.this.doc.cz += mdz;
                        this.grabX += mdx;
                        this.grabZ += mdz;
                }

                ZoneTab.this.changed();
                return true;
            }
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            if (this.drag != null) {
                this.drag = null;
                ZoneTab.this.buildForm();
            }

            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            this.zoom = Math.max(0.2, Math.min(5.0, this.zoom * (delta > 0.0 ? 1.2 : 0.8333333333333334)));
            return true;
        }

        @Override
        public int cursor(int mx, int my) {
            return this.handleAt(mx, my) != null ? 221188 : 221185;
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
