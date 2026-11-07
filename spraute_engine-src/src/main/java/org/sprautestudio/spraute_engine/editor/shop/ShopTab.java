package org.sprautestudio.spraute_engine.editor.shop;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.shop.ShopDef;
import org.sprautestudio.spraute_engine.shop.ShopView;
import org.sprautestudio.spraute_engine.shop.client.ShopPanel;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class ShopTab extends EditorTab {
    private ShopDef doc;
    private String saved;
    private String sel = "";
    private final ShopTab.Form form = new ShopTab.Form();
    private final ScrollView inspector;
    private final ShopTab.OfferList list = new ShopTab.OfferList();
    private final ShopTab.Preview preview = new ShopTab.Preview();
    private final List<Button> tools = new ArrayList<>();
    private final ShopPanel panel;
    private static final List<String> TYPES = List.of("buy", "sell", "trade");
    private static final List<String> RESTOCK = List.of("never", "daily");
    private static final int LABEL_W = 122;
    private static final int LIST_W = 190;
    private static final int INSPECTOR_W = 330;
    private int toolbarH = 28;

    public ShopTab(Project project, Project.FileRef file, String content) {
        super(project, file);
        this.doc = ShopDef.fromJson(content);
        this.saved = this.doc.toJson();
        this.panel = new ShopPanel(ShopView.preview(this.doc));
        this.add(this.list);
        this.add(this.preview);
        this.inspector = this.add(new ScrollView(this.form));
        this.tool("Товар", Icon.PLUS, Button.Style.PRIMARY, "Новый товар во вкладке, где вы сейчас", this::addOffer);
        this.tool("Копия", Icon.COPY, Button.Style.GHOST, "Копия выбранного товара", this::duplicate);
        this.tool("Удалить", Icon.TRASH, Button.Style.GHOST, "Удалить выбранный товар или вкладку", this::delete);
        this.tool("Магазин", Icon.GEAR, Button.Style.GHOST, "Название и деньги магазина", () -> this.select(""));
        this.buildForm();
    }

    private void tool(String text, Icon icon, Button.Style style, String tip, Runnable r) {
        Button b = this.add(new Button(text, r).icon(icon).style(style));
        b.tooltip(tip);
        b.autoWidth();
        this.tools.add(b);
    }

    private ShopDef.Offer offer() {
        return this.sel.startsWith("offer:") ? this.doc.offer(this.sel.substring(6)) : null;
    }

    private ShopDef.Tab tab() {
        if (!this.sel.startsWith("tab:")) {
            return null;
        } else {
            for (ShopDef.Tab t : this.doc.tabs) {
                if (t.id.equals(this.sel.substring(4))) {
                    return t;
                }
            }

            return null;
        }
    }

    private void select(String s) {
        this.sel = s;
        ShopDef.Offer o = this.offer();
        if (o != null) {
            this.panel.selected = o.id;
        }

        this.buildForm();
    }

    private void changed() {
        this.doc.fix();
        this.panel.setView(ShopView.preview(this.doc));
    }

    private String uniqueId(String base) {
        String id = base;
        int n = 2;

        while (this.doc.offer(id) != null) {
            id = base + n++;
        }

        return id;
    }

    private String currentTab() {
        ShopDef.Offer o = this.offer();
        if (o != null) {
            return o.tab;
        } else {
            ShopDef.Tab t = this.tab();
            return t != null ? t.id : this.doc.tabs.get(0).id;
        }
    }

    private void addOffer() {
        ShopDef.Offer o = new ShopDef.Offer();
        o.id = this.uniqueId("item");
        o.tab = this.currentTab();
        this.doc.offers.add(o);
        this.changed();
        this.select("offer:" + o.id);
    }

    private void addTab() {
        String id = "tab";
        int n = 2;

        while (true) {
            String cand = id;
            if (this.doc.tabs.stream().noneMatch(t -> t.id.equals(cand))) {
                this.doc.tabs.add(new ShopDef.Tab(id, "Вкладка " + this.doc.tabs.size(), "minecraft:chest"));
                this.changed();
                this.select("tab:" + id);
                return;
            }

            id = "tab" + n++;
        }
    }

    private void duplicate() {
        ShopDef.Offer o = this.offer();
        if (o != null) {
            ShopDef tmp = new ShopDef();
            tmp.offers.add(o);
            ShopDef.Offer c = ShopDef.fromJson(tmp.toJson()).offers.get(0);
            c.id = this.uniqueId(o.id);
            this.doc.offers.add(this.doc.offers.indexOf(o) + 1, c);
            this.changed();
            this.select("offer:" + c.id);
        }
    }

    private void delete() {
        ShopDef.Offer o = this.offer();
        if (o != null) {
            this.doc.offers.remove(o);
            this.changed();
            this.select("");
        } else {
            ShopDef.Tab t = this.tab();
            if (t != null && this.doc.tabs.size() > 1) {
                this.doc.tabs.remove(t);
                String to = this.doc.tabs.get(0).id;

                for (ShopDef.Offer x : this.doc.offers) {
                    if (x.tab.equals(t.id)) {
                        x.tab = to;
                    }
                }

                this.changed();
                this.select("");
            }
        }
    }

    private static String typeTitle(String t) {
        return switch (t) {
            case "sell" -> "игрок продаёт (скупка)";
            case "trade" -> "обмен предметов";
            default -> "игрок покупает";
        };
    }

    private List<String> currencies() {
        List<String> out = new ArrayList<>(List.of("coins", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:diamond"));
        if (!out.contains(this.doc.currency)) {
            out.add(this.doc.currency);
        }

        return out;
    }

    private void buildForm() {
        this.form.clear();
        ShopDef.Offer o = this.offer();
        ShopDef.Tab t = this.tab();
        if (o != null) {
            this.offerForm(o);
        } else if (t != null) {
            this.tabForm(t);
        } else {
            this.shopForm();
        }

        if (this.w > 0) {
            this.layout();
        }
    }

    private void shopForm() {
        this.form.header("Магазин");
        this.form.text("Название", "Заголовок окна", () -> this.doc.title, v -> this.doc.title = v);
        this.form
            .choice(
                "Деньги",
                "Монеты движка или любой предмет как деньги",
                this::currencies,
                () -> this.doc.currency,
                v -> v.equals("coins") ? "монеты движка" : v.replace("minecraft:", ""),
                v -> this.doc.currency = v
            );
        this.form.item("Свой предмет-деньги", () -> this.doc.currency.equals("coins") ? "" : this.doc.currency, v -> this.doc.currency = v);
        this.form.text("Как называть", "«монет», «изумрудов»… (пусто — само)", () -> this.doc.currencyName, v -> this.doc.currencyName = v);
        this.form.note("В сюжете: shop open \"" + (this.file != null ? this.file.name() : "имя") + "\" with торговец");
        this.form.note("Покупки: on purchase \"" + (this.file != null ? this.file.name() : "имя") + "\" { event(\"offer\") }");
        this.form.note("Выберите товар слева (или щёлкните его в окне)");
    }

    private void tabForm(ShopDef.Tab t) {
        this.form.header("Вкладка");
        this.form.text("Название", null, () -> t.name, v -> t.name = v);
        this.form.item("Значок", () -> t.icon, v -> t.icon = v);
        this.form.text("Id", "Для товаров (поле «Вкладка»)", () -> t.id, v -> {
            String nv = v.trim();
            if (!nv.isEmpty()) {
                for (ShopDef.Offer x : this.doc.offers) {
                    if (x.tab.equals(t.id)) {
                        x.tab = nv;
                    }
                }

                t.id = nv;
                this.sel = "tab:" + nv;
            }
        });
    }

    private void offerForm(ShopDef.Offer o) {
        this.form.header("Товар");
        this.form.text("Id", "Имя товара для сюжета: bought(\"лавка\", \"id\")", () -> o.id, v -> {
            String nv = v.trim();
            if (!nv.isEmpty() && (this.doc.offer(nv) == null || this.doc.offer(nv) == o)) {
                o.id = nv;
                this.sel = "offer:" + nv;
            }
        });
        this.form.choice("Сделка", null, () -> TYPES, () -> o.type, ShopTab::typeTitle, v -> {
            o.type = v;
            this.buildForm();
        });
        this.form.item("Предмет", () -> o.item, v -> o.item = v);
        this.form.integer("Сколько штук", "За одну сделку", () -> o.count, v -> o.count = Math.max(1, v));
        this.form.text("Название", "Пусто — название предмета", () -> o.name, v -> o.name = v);
        this.form.text("Описание", null, () -> o.description, v -> o.description = v);
        this.form.header(o.type.equals("sell") ? "Сколько платит торговец" : "Цена");
        this.form
            .integer(
                o.type.equals("trade") ? "Плюс денег" : "Денег",
                o.type.equals("trade") ? "0 — только предметы" : null,
                () -> o.price,
                v -> o.price = Math.max(0, v)
            );
        this.form.number("Скидка (доля)", "1 — полная цена, 0.8 — скидка 20%", () -> o.discount, v -> o.discount = (float)Math.max(0.01, v));
        this.form.number("Рост цены", "Каждая покупка дороже на эту долю: 0.25 — +25%", () -> o.priceGrowth, v -> o.priceGrowth = (float)Math.max(0.0, v));
        this.form.note(o.type.equals("sell") ? "Предметы, которые торговец берёт сверху:" : "Предметы, которые нужно отдать:");

        for (int i = 0; i < o.costs.size(); i++) {
            ShopDef.Stack cost = o.costs.get(i);
            this.form.cost(o, i);
            this.form.nbt(() -> cost.item, v -> cost.item = v);
        }

        this.form.button("+ предмет в цену", () -> {
            o.costs.add(new ShopDef.Stack("minecraft:emerald", 1));
            this.changed();
            this.buildForm();
        });
        this.form.header("Запас и условия");
        this.form.integer("Запас", "Сколько раз можно купить, -1 — без конца", () -> o.stock, v -> o.stock = Math.max(-1, v));
        if (o.stock >= 0) {
            this.form
                .choice(
                    "Пополняется",
                    null,
                    () -> RESTOCK,
                    () -> o.restock,
                    v -> v.equals("daily") ? "каждый новый день" : "только командой shop restock",
                    v -> o.restock = v
                );
        }

        this.form.integer("Лимит на игрока", "-1 — без лимита", () -> o.limit, v -> o.limit = Math.max(-1, v));
        this.form.text("Виден, если", "Выражение сюжета: flag(\"met\"), level >= 3 (пусто — всегда)", () -> o.condition, v -> o.condition = v.trim());
        this.form.text("Открыт, если", "Пока неверно — товар виден, но закрыт", () -> o.unlock, v -> o.unlock = v.trim());
        if (!o.unlock.isEmpty()) {
            this.form.text("Почему закрыт", null, () -> o.lockText, v -> o.lockText = v);
        }
    }

    private static String fmt(double v) {
        double r = Math.round(v * 1000.0) / 1000.0;
        return r == (long)r ? String.valueOf((long)r) : String.valueOf(r);
    }

    private static ShopView.Line lineOf(ShopDef.Offer o) {
        ShopView.Line l = new ShopView.Line();
        l.item = o.item;
        l.name = o.name;
        return l;
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
        int lw = Math.min(190, this.w / 4);
        int iw = Math.min(330, this.w / 3);
        this.list.bounds(this.x, this.y + this.toolbarH, lw, this.h - this.toolbarH);
        this.preview.bounds(this.x + lw, this.y + this.toolbarH, this.w - lw - iw, this.h - this.toolbarH);
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
    public String title() {
        return this.file != null ? this.file.name() : "Магазин";
    }

    @Override
    public Icon icon() {
        return Icon.TAG;
    }

    @Override
    public int iconColor() {
        return -8658806;
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

        ShopTab.Row row(String label, String doc) {
            ShopTab.Row r = this.add(ShopTab.this.new Row(label));
            if (doc != null) {
                r.tooltip(doc);
            }

            return r;
        }

        void text(String label, String doc, Supplier<String> get, Consumer<String> set) {
            ShopTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(get.get());
            f.onChange = s -> {
                set.accept(s);
                ShopTab.this.changed();
            };
            r.editor = r.add(f);
        }

        void number(String label, String doc, DoubleSupplier get, DoubleConsumer set) {
            ShopTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(ShopTab.fmt(get.getAsDouble()));
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.';
            f.onChange = s -> {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                    ShopTab.this.changed();
                } catch (NumberFormatException var4x) {
                }
            };
            r.editor = r.add(f);
        }

        void integer(String label, String doc, IntSupplier get, IntConsumer set) {
            this.number(label, doc, get::getAsInt, v -> set.accept((int)Math.round(v)));
        }

        void choice(String label, String doc, Supplier<List<String>> options, Supplier<String> get, Function<String, String> labeler, Consumer<String> set) {
            ShopTab.Row r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(options, get, labeler, v -> {
                set.accept(v);
                ShopTab.this.changed();
            }));
        }

        void item(String label, Supplier<String> get, Consumer<String> set) {
            ShopTab.Row r = this.row(label, "Нажмите, чтобы выбрать предмет");
            r.editor = r.add(
                new Button(
                        () -> get.get().isEmpty() ? "выбрать…" : ItemSpec.id(get.get()).replace("minecraft:", ""),
                        () -> AssetPicker.open(ParamType.ITEM, ItemSpec.id(get.get()), v -> {
                            set.accept(ItemSpec.with(v, ItemSpec.nbt(get.get())));
                            ShopTab.this.changed();
                            ShopTab.this.buildForm();
                        })
                    )
                    .icon(Icon.CUBE)
                    .style(Button.Style.GHOST)
            );
            if (!get.get().isEmpty()) {
                this.nbt(get, set);
            }
        }

        void nbt(Supplier<String> get, Consumer<String> set) {
            this.text(
                "NBT",
                "Необязательно. Как в командах: {Damage:5,display:{Name:'{\"text\":\"Меч\"}'}}",
                () -> ItemSpec.nbt(get.get()),
                v -> set.accept(ItemSpec.with(get.get(), v))
            );
        }

        void cost(final ShopDef.Offer o, int i) {
            final ShopDef.Stack st = o.costs.get(i);
            ShopTab.Row r = this.row("Предмет " + (i + 1), null);
            r.editor = r.add(
                new Widget() {
                    final Button pick = this.add(
                        new Button(() -> ItemSpec.id(st.item).replace("minecraft:", ""), () -> AssetPicker.open(ParamType.ITEM, ItemSpec.id(st.item), v -> {
                            st.item = ItemSpec.with(v, ItemSpec.nbt(st.item));
                            ShopTab.this.changed();
                        })).icon(Icon.CUBE).style(Button.Style.GHOST)
                    );
                    final TextField n = this.add(new TextField());
                    final Button del = this.add(Button.icon(Icon.CLOSE, "Убрать", () -> {
                        o.costs.remove(st);
                        ShopTab.this.changed();
                        ShopTab.this.buildForm();
                    }));

                    {
                        this.n.setText(String.valueOf(st.count));
                        this.n.filter = Character::isDigit;
                        this.n.onChange = s -> {
                            try {
                                st.count = Math.max(1, Integer.parseInt(s.trim()));
                                ShopTab.this.changed();
                            } catch (NumberFormatException var4) {
                            }
                        };
                    }

                    @Override
                    public void layout() {
                        this.pick.bounds(this.x, this.y, this.w - 64, this.h);
                        this.n.bounds(this.x + this.w - 60, this.y, 36, this.h);
                        this.del.bounds(this.x + this.w - 20, this.y, 20, this.h);
                        super.layout();
                    }
                }
            );
        }

        void button(String text, Runnable r) {
            ShopTab.Row row = this.row("", null);
            row.editor = row.add(new Button(text, r).style(Button.Style.GHOST));
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

    private final class OfferList extends Widget implements UI.CursorProvider {
        private static final int ROW = 18;

        private List<String> rows() {
            List<String> out = new ArrayList<>();

            for (ShopDef.Offer o : ShopTab.this.doc.offers) {
                out.add("offer:" + o.id);
            }

            return out;
        }

        private int rowAt(double my) {
            return (int)Math.floor((my - this.y - 4.0) / 18.0);
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14934490);
            c.fill(this.x + this.w - 1, this.y, 1, this.h, -13749952);
            List<String> rows = this.rows();
            int ry = this.y + 4;

            for (int i = 0; i < rows.size(); i++) {
                String r = rows.get(i);
                boolean on = r.equals(ShopTab.this.sel);
                boolean hov = this.isHovered() && this.rowAt(my) == i;
                c.round(this.x + 4, ry, this.w - 8, 17, 3, on ? 1435200767 : (hov ? -14210506 : 0));
                if (r.startsWith("tab:")) {
                    ShopDef.Tab t = null;

                    for (ShopDef.Tab x2 : ShopTab.this.doc.tabs) {
                        if (x2.id.equals(r.substring(4))) {
                            t = x2;
                        }
                    }

                    c.icon(Icon.FOLDER, this.x + 8, ry + 5, -8658806);
                    c.textEllipsized(t != null ? t.name : r, this.x + 22, ry + 5, this.w - 30, -1841682);
                } else {
                    ShopDef.Offer o = ShopTab.this.doc.offer(r.substring(6));
                    if (o == null) {
                        continue;
                    }

                    int col = o.type.equals("sell") ? -8658806 : (o.type.equals("trade") ? -9533697 : -11686);
                    c.fill(this.x + 18, ry + 5, 3, 8, col);
                    String label = ShopPanel.title(ShopTab.lineOf(o)) + (o.count > 1 ? " ×" + o.count : "");
                    c.textEllipsized(label, this.x + 26, ry + 5, this.w - 60, -6577995);
                    String price = o.type.equals("trade") && o.price == 0 ? "обмен" : String.valueOf(o.price);
                    c.text(price, this.x + this.w - 8 - c.width(price), ry + 5, -9933435);
                }

                ry += 18;
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            List<String> rows = this.rows();
            int i = this.rowAt(my);
            if (i >= 0 && i < rows.size()) {
                ShopTab.this.select(rows.get(i));
            }

            return true;
        }

        @Override
        public int cursor(int mx, int my) {
            return 221188;
        }
    }

    private final class Preview extends Widget {
        float lastK = 1.0F;
        float lastX;
        float lastY;

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.gradient(this.x, this.y, this.w, this.h, -14868432, -15921644);
            c.flush();
            float k = Math.min(1.0F, Math.min((this.w - 16) / 445.0F, (this.h - 40) / 262.0F));
            Gfx g = c.g;
            g.pose().pushPose();
            float ox = this.x + (this.w - 445.0F * k) / 2.0F;
            float oy = this.y + 10 + (this.h - 40 - 262.0F * k) / 2.0F;
            g.pose().translate(ox, oy, 0.0F);
            g.pose().scale(k, k, 1.0F);
            ShopTab.this.panel.style = EngineClient.config().shop;
            ShopTab.this.panel.place(0, 0);

            try {
                ShopTab.this.panel.render(g, Minecraft.getInstance().font, EngineClient.config(), (mx - ox) / k, (my - oy) / k);
            } catch (Throwable var10) {
            }

            g.flush();
            g.pose().popPose();
            c.textEllipsized(
                "Так увидит игрок (деньги — для примера). Щёлкните товар, чтобы править его; колесо — листать",
                this.x + 8,
                this.y + this.h - 14,
                this.w - 16,
                -9933435
            );
            this.lastK = k;
            this.lastX = ox;
            this.lastY = oy;
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            double px = (mx - this.lastX) / this.lastK;
            double py = (my - this.lastY) / this.lastK;
            String h = ShopTab.this.panel.hit(px, py);
            ShopTab.this.panel.click(px, py);
            if (h != null && h.startsWith("cell:")) {
                ShopTab.this.select("offer:" + h.substring(5));
            }

            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            ShopTab.this.panel.scroll(delta);
            return true;
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
