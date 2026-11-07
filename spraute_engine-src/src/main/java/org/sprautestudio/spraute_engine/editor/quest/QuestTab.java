package org.sprautestudio.spraute_engine.editor.quest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.quest.QuestDef;
import org.sprautestudio.spraute_engine.quest.QuestGoals;
import org.sprautestudio.spraute_engine.quest.QuestJournal;
import org.sprautestudio.spraute_engine.quest.client.QuestScreen;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class QuestTab extends EditorTab {
    private QuestDef doc;
    private String saved;
    private String sel = "";
    private final QuestTab.Form form = new QuestTab.Form();
    private final ScrollView inspector;
    private final QuestTab.TaskList list = new QuestTab.TaskList();
    private final QuestTab.Preview preview = new QuestTab.Preview();
    private final List<Button> tools = new ArrayList<>();
    private QuestScreen journal;
    private static final int LABEL_W = 122;
    private static final String DEFAULT_TEXT = (new QuestDef.Task()).text;
    private static final int LIST_W = 190;
    private static final int INSPECTOR_W = 330;
    private int toolbarH = 28;

    public QuestTab(Project project, Project.FileRef file, String content) {
        super(project, file);
        this.doc = QuestDef.fromJson(content);
        this.saved = this.doc.toJson();
        this.add(this.list);
        this.add(this.preview);
        this.inspector = this.add(new ScrollView(this.form));
        this.tool("Задача", Icon.PLUS, Button.Style.PRIMARY, "Новая задача квеста", this::addTask);
        this.tool("Выше", Icon.CHEVRON_UP, Button.Style.GHOST, "Поднять задачу", () -> this.move(-1));
        this.tool("Ниже", Icon.CHEVRON_DOWN, Button.Style.GHOST, "Опустить задачу", () -> this.move(1));
        this.tool("Удалить", Icon.TRASH, Button.Style.GHOST, "Удалить выбранную задачу", this::delete);
        this.tool("Квест", Icon.GEAR, Button.Style.GHOST, "Название, описание и награда", () -> this.select(""));
        this.changed();
        this.buildForm();
    }

    private void tool(String text, Icon icon, Button.Style style, String tip, Runnable r) {
        Button b = this.add(new Button(text, r).icon(icon).style(style));
        b.tooltip(tip);
        b.autoWidth();
        this.tools.add(b);
    }

    private QuestDef.Task task() {
        return this.sel.isEmpty() ? null : this.doc.task(this.sel);
    }

    private void select(String s) {
        this.sel = s;
        this.buildForm();
    }

    private String key() {
        return "preview/" + (this.file != null ? this.file.name() : "quest");
    }

    private void changed() {
        this.doc.fix();
        QuestJournal j = QuestJournal.preview(this.doc, this.key());
        if (this.journal == null) {
            this.journal = new QuestScreen(j);
        } else {
            this.journal.update(j);
        }
    }

    private void addTask() {
        QuestDef.Task t = new QuestDef.Task();
        String id = "task";
        int n = 2;

        while (this.doc.task(id) != null) {
            id = "task" + n++;
        }

        t.id = id;
        QuestDef.Task cur = this.task();
        this.doc.tasks.add(cur != null ? this.doc.tasks.indexOf(cur) + 1 : this.doc.tasks.size(), t);
        this.changed();
        this.select(id);
    }

    private void move(int d) {
        QuestDef.Task t = this.task();
        if (t != null) {
            int i = this.doc.tasks.indexOf(t);
            int j = i + d;
            if (j >= 0 && j < this.doc.tasks.size()) {
                this.doc.tasks.remove(i);
                this.doc.tasks.add(j, t);
                this.changed();
            }
        }
    }

    private void delete() {
        QuestDef.Task t = this.task();
        if (t != null) {
            this.doc.tasks.remove(t);
            this.changed();
            this.select("");
        }
    }

    private void buildForm() {
        this.form.clear();
        QuestDef.Task t = this.task();
        if (t != null) {
            this.taskForm(t);
        } else {
            this.questForm();
        }

        if (this.w > 0) {
            this.layout();
        }
    }

    private String name() {
        return this.file != null ? this.file.name() : "квест";
    }

    private void moveSection(QuestDef.Section s, int d) {
        int i = this.doc.layout.indexOf(s);
        int j = i + d;
        if (i >= 0 && j >= 0 && j < this.doc.layout.size()) {
            Collections.swap(this.doc.layout, i, j);
            this.changed();
            this.buildForm();
        }
    }

    private void questForm() {
        this.form.header("Квест");
        this.form.text("Название", null, () -> this.doc.title, v -> this.doc.title = v);
        this.form.text("Кто выдал", "Пусто — не показывать", () -> this.doc.giver, v -> this.doc.giver = v);
        this.form.text("Описание", "История квеста в журнале", () -> this.doc.description, v -> this.doc.description = v);
        this.form.check("По одной задаче", "Следующая задача появляется, когда сделана предыдущая", () -> this.doc.sequential, v -> this.doc.sequential = v);
        this.form
            .check(
                "Завершать сам",
                "Квест выполнен (и награда выдана), как только сделаны все обязательные задачи",
                () -> this.doc.autoComplete,
                v -> this.doc.autoComplete = v
            );
        this.form.header("Страница в журнале");
        this.form.note("Порядок разделов сверху вниз. ▲▼ — переставить, ✕ — убрать со страницы");

        for (int i = 0; i < this.doc.layout.size(); i++) {
            this.form.section(i);
        }

        this.form.button("+ свой текст", () -> {
            QuestDef.Section s = new QuestDef.Section("text");
            s.text = "Новый текст";
            this.doc.layout.add(s);
            this.changed();
            this.buildForm();
        });

        for (String k : new String[]{"description", "tasks", "reward"}) {
            if (!this.doc.layout.stream().anyMatch(s -> s.kind.equals(k))) {
                this.form.button("+ вернуть: " + sectionTitle(k).toLowerCase(), () -> {
                    this.doc.layout.add(new QuestDef.Section(k));
                    this.changed();
                    this.buildForm();
                });
            }
        }

        this.form.header("Награда");
        if (this.doc.layout.stream().noneMatch(s -> s.kind.equals("reward"))) {
            this.form.note("Раздел награды скрыт со страницы, но награда всё равно выдаётся");
        }

        this.form.integer("Монеты", null, () -> this.doc.reward.coins, v -> this.doc.reward.coins = Math.max(0, v));
        this.form.integer("Опыт", null, () -> this.doc.reward.xp, v -> this.doc.reward.xp = Math.max(0, v));

        for (int i = 0; i < this.doc.reward.items.size(); i++) {
            QuestDef.Item item = this.doc.reward.items.get(i);
            this.form.rewardItem(i);
            this.form
                .text(
                    "NBT",
                    "Необязательно. Как в командах: {Damage:5,display:{Name:'{\"text\":\"Меч\"}'}}",
                    () -> ItemSpec.nbt(item.item),
                    v -> item.item = ItemSpec.with(item.item, v)
                );
        }

        this.form.button("+ предмет в награду", () -> {
            this.doc.reward.items.add(new QuestDef.Item("minecraft:emerald", 1));
            this.changed();
            this.buildForm();
        });
        this.form.text("Своя строка", "Как описать награду в журнале (пусто — составится сама)", () -> this.doc.reward.text, v -> this.doc.reward.text = v);
        this.form.header("В сюжете");
        this.form.note("quest start \"" + this.name() + "\"");
        this.form.note("quest progress \"" + this.name() + "\" \"zombies\"   (+1)");
        this.form.note("quest done \"" + this.name() + "\"   ·   quest fail \"" + this.name() + "\"");
        this.form.note("if queststate(\"" + this.name() + "\") == \"completed\" { … }");
        this.form.note("on questdone \"" + this.name() + "\" { … }");
        this.form.note("Выберите задачу слева, чтобы править её");
    }

    private void taskForm(QuestDef.Task t) {
        this.form.header("Задача");
        this.form.text("Id", "Имя для сюжета: quest progress \"" + this.name() + "\" \"id\"", () -> t.id, v -> {
            String nv = v.trim();
            if (!nv.isEmpty() && (this.doc.task(nv) == null || this.doc.task(nv) == t)) {
                t.id = nv;
                this.sel = nv;
            }
        });
        this.form.text("Текст", "Как задача видна в журнале. Пусто у задачи с целью — строка составится из цели", () -> t.text, v -> t.text = v);
        this.form.header("Цель");
        this.form
            .choice("Что сделать", "Задача с целью считается сама, пока квест выдан. Без цели её двигает только сюжет", QuestGoals::ids, () -> t.goal, v -> {
                QuestGoals.Goal g = QuestGoals.get(v);
                return g != null ? g.label() : "из сюжета";
            }, v -> {
                t.goal = v;
                t.target = "";
                t.who = "";
                if (v.isEmpty() || !t.text.isBlank() && !t.text.equals(DEFAULT_TEXT)) {
                    if (v.isEmpty() && t.text.isBlank()) {
                        t.text = DEFAULT_TEXT;
                    }
                } else {
                    t.text = "";
                }

                this.doc.fix();
                this.buildForm();
            });
        QuestGoals.Goal goal = QuestGoals.get(t.goal);
        if (goal != null) {
            if (goal.who()) {
                this.form
                    .choice(
                        "Кто",
                        "Выбранный персонаж сам начнёт подбирать, пока задача открыта. «Любой» — засчитается любому, но подбор запускайте командой pickup",
                        () -> {
                            List<String> names = new ArrayList<>();
                            names.add("");
                            names.addAll(AssetIndex.characters());
                            if (!names.contains(t.who)) {
                                names.add(t.who);
                            }

                            return names;
                        },
                        () -> t.who,
                        v -> v.isEmpty() ? "любой персонаж" : v,
                        v -> t.who = v
                    );
                if (AssetIndex.characters().isEmpty()) {
                    this.form.note("В сюжетах проекта пока нет персонажей");
                }
            }

            this.form.target(t, goal);
        }

        if (goal == null || goal.repeats()) {
            this.form.integer("Сколько раз", "1 — просто сделать; 5 — счётчик 0/5", () -> t.count, v -> t.count = Math.max(1, v));
        }

        this.form.check("Необязательная", "Квест можно выполнить и без неё", () -> t.optional, v -> t.optional = v);
        this.form.check("Скрытая", "Не видна, пока сюжет её не откроет (quest reveal) или она не сдвинется", () -> t.hidden, v -> t.hidden = v);
        this.form.header("В сюжете");
        this.form.note("quest progress \"" + this.name() + "\" \"" + t.id + "\"" + (t.count > 1 ? " 2" : ""));
        this.form.note("quest set \"" + this.name() + "\" \"" + t.id + "\" " + t.count + "   (выполнить)");
        if (t.hidden) {
            this.form.note("quest reveal \"" + this.name() + "\" \"" + t.id + "\"");
        }

        this.form.note("questprogress(\"" + this.name() + "\", \"" + t.id + "\") — сколько сделано");
        this.form.note("wait goal \"" + this.name() + "\" \"" + t.id + "\"   (ждать, пока сделана)");
        this.form.note("on goal \"" + this.name() + "\" \"" + t.id + "\" { … }");
    }

    private static String sectionTitle(String kind) {
        return switch (kind) {
            case "description" -> "Описание";
            case "tasks" -> "Задачи";
            case "reward" -> "Награда";
            default -> "Текст";
        };
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
        return this.file != null ? this.file.name() : "Квест";
    }

    @Override
    public Icon icon() {
        return Icon.FLAG;
    }

    @Override
    public int iconColor() {
        return -11686;
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

        QuestTab.Row row(String label, String doc) {
            QuestTab.Row r = this.add(QuestTab.this.new Row(label));
            if (doc != null) {
                r.tooltip(doc);
            }

            return r;
        }

        void text(String label, String doc, Supplier<String> get, Consumer<String> set) {
            QuestTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(get.get());
            f.onChange = s -> {
                set.accept(s);
                QuestTab.this.changed();
            };
            r.editor = r.add(f);
        }

        void integer(String label, String doc, IntSupplier get, IntConsumer set) {
            QuestTab.Row r = this.row(label, doc);
            TextField f = new TextField();
            f.setText(String.valueOf(get.getAsInt()));
            f.filter = ch -> Character.isDigit(ch) || ch == '-';
            f.onChange = s -> {
                try {
                    set.accept(Integer.parseInt(s.trim()));
                    QuestTab.this.changed();
                } catch (NumberFormatException var4x) {
                }
            };
            r.editor = r.add(f);
        }

        void check(String label, String doc, BooleanSupplier get, Consumer<Boolean> set) {
            QuestTab.Row r = this.row(label, doc);
            r.editor = r.add(new Checkbox("", get, v -> {
                set.accept(v);
                QuestTab.this.changed();
                QuestTab.this.buildForm();
            }));
        }

        void choice(String label, String doc, Supplier<List<String>> options, Supplier<String> get, Function<String, String> labeler, Consumer<String> set) {
            QuestTab.Row r = this.row(label, doc);
            r.editor = r.add(new Dropdown<>(options, get, labeler, v -> {
                set.accept(v);
                QuestTab.this.changed();
            }));
        }

        void target(final QuestDef.Task t, QuestGoals.Goal g) {
            if (g.target() == ParamType.ZONE) {
                this.choice(g.targetLabel(), "Зона из папки «Зоны»", () -> {
                    List<String> zones = new ArrayList<>();
                    zones.add("");
                    zones.addAll(AssetIndex.projectZones.get());
                    return zones;
                }, () -> t.target, v -> v.isEmpty() ? "любая" : v, v -> t.target = v);
            } else {
                final boolean item = g.target() == ParamType.ITEM;
                QuestTab.Row r = this.row(
                    g.targetLabel(), item ? "Предмет или блок из списка. Не выбран — любой" : "Персонаж из сюжетов или существо из списка. Не выбран — любой"
                );
                r.editor = r.add(
                    new Widget() {
                        final Button pick = this.add(
                            new Button(() -> t.target.isEmpty() ? "любой" : QuestGoals.shortId(t.target), () -> this.chooseTarget())
                                .icon(item ? Icon.CUBE : Icon.USER)
                                .style(Button.Style.GHOST)
                        );
                        final Button clear = this.add(Button.icon(Icon.CLOSE, "Любой", () -> this.setTarget("")));

                        void setTarget(String v) {
                            t.target = v;
                            QuestTab.this.changed();
                            QuestTab.this.buildForm();
                        }

                        void chooseTarget() {
                            if (item) {
                                AssetPicker.open(ParamType.ITEM, t.target, this::setTarget);
                            } else {
                                PopupMenu pm = new PopupMenu();

                                for (String ch : AssetIndex.characters()) {
                                    pm.item("нпс: " + ch, Icon.USER, null, () -> this.setTarget(ch)).checked(() -> t.target.equals(ch));
                                }

                                if (!AssetIndex.characters().isEmpty()) {
                                    pm.separator();
                                }

                                pm.item("Существо из списка…", Icon.CUBE, null, () -> AssetPicker.open(ParamType.ENTITY, t.target, this::setTarget));
                                pm.show(this.x, this.y + this.h + 2);
                            }
                        }

                        @Override
                        public void layout() {
                            this.pick.bounds(this.x, this.y, this.w - 24, this.h);
                            this.clear.bounds(this.x + this.w - 20, this.y, 20, this.h);
                            super.layout();
                        }
                    }
                );
            }
        }

        void rewardItem(int i) {
            final QuestDef.Item st = QuestTab.this.doc.reward.items.get(i);
            QuestTab.Row r = this.row("Предмет " + (i + 1), null);
            r.editor = r.add(
                new Widget() {
                    final Button pick = this.add(
                        new Button(() -> ItemSpec.id(st.item).replace("minecraft:", ""), () -> AssetPicker.open(ParamType.ITEM, ItemSpec.id(st.item), v -> {
                            st.item = ItemSpec.with(v, ItemSpec.nbt(st.item));
                            QuestTab.this.changed();
                        })).icon(Icon.CUBE).style(Button.Style.GHOST)
                    );
                    final TextField n = this.add(new TextField());
                    final Button del = this.add(Button.icon(Icon.CLOSE, "Убрать", () -> {
                        QuestTab.this.doc.reward.items.remove(st);
                        QuestTab.this.changed();
                        QuestTab.this.buildForm();
                    }));

                    {
                        this.n.setText(String.valueOf(st.count));
                        this.n.filter = Character::isDigit;
                        this.n.onChange = s -> {
                            try {
                                st.count = Math.max(1, Integer.parseInt(s.trim()));
                                QuestTab.this.changed();
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

        void section(int i) {
            final QuestDef.Section s = QuestTab.this.doc.layout.get(i);
            QuestTab.Row r = this.row(i + 1 + ". " + QuestTab.sectionTitle(s.kind), s.kind.equals("text") ? "Свой текст на странице квеста" : null);
            r.editor = r.add(
                new Widget() {
                    final TextField f = this.add(new TextField());
                    final Button up = this.add(Button.icon(Icon.CHEVRON_UP, "Выше", () -> QuestTab.this.moveSection(s, -1)));
                    final Button down = this.add(Button.icon(Icon.CHEVRON_DOWN, "Ниже", () -> QuestTab.this.moveSection(s, 1)));
                    final Button del = this.add(Button.icon(Icon.CLOSE, s.kind.equals("text") ? "Удалить текст" : "Убрать со страницы", () -> {
                        QuestTab.this.doc.layout.remove(s);
                        QuestTab.this.changed();
                        QuestTab.this.buildForm();
                    }));

                    {
                        boolean own = s.kind.equals("text");
                        this.f
                            .placeholder(
                                own
                                    ? "Текст"
                                    : (
                                        s.kind.equals("description")
                                            ? "Заголовок (пусто — без него)"
                                            : "Заголовок (пусто — " + QuestTab.sectionTitle(s.kind).toUpperCase() + ")"
                                    )
                            );
                        this.f.setText(own ? s.text : s.header);
                        this.f.onChange = v -> {
                            if (own) {
                                s.text = v;
                            } else {
                                s.header = v;
                            }

                            QuestTab.this.changed();
                        };
                    }

                    @Override
                    public void layout() {
                        this.f.bounds(this.x, this.y, this.w - 66, this.h);
                        this.up.bounds(this.x + this.w - 62, this.y, 20, this.h);
                        this.down.bounds(this.x + this.w - 41, this.y, 20, this.h);
                        this.del.bounds(this.x + this.w - 20, this.y, 20, this.h);
                        super.layout();
                    }
                }
            );
            if (s.kind.equals("text")) {
                this.text("   заголовок", "Над текстом (пусто — без заголовка)", () -> s.header, v -> s.header = v);
                this.text("   цвет", "Например #FFD25A (пусто — обычный)", () -> s.color, v -> s.color = v);
            }
        }

        void button(String text, Runnable r) {
            QuestTab.Row row = this.row("", null);
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

    private final class Preview extends Widget {
        float lastK = 1.0F;
        float lastX;
        float lastY;

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.gradient(this.x, this.y, this.w, this.h, -14868432, -15921644);
            c.flush();
            float k = Math.min(1.5F, Math.min((this.w - 16) / 392.0F, (this.h - 40) / 230.0F));
            Gfx g = c.g;
            float ox = this.x + (this.w - 392.0F * k) / 2.0F;
            float oy = this.y + 10 + (this.h - 40 - 230.0F * k) / 2.0F;
            g.pose().pushPose();
            g.pose().translate(ox, oy, 0.0F);

            try {
                QuestTab.this.journal.paintPreview(g, k, mx - ox, my - oy);
            } catch (Throwable var10) {
            }

            g.flush();
            g.pose().popPose();
            c.textEllipsized(
                "Так квест выглядит в журнале (J): первая задача сделана для примера. Щёлкните название — отслеживать",
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
            QuestTab.this.journal.clickPreview(mx - this.lastX, my - this.lastY);
            return true;
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            QuestTab.this.journal.scrollPreview(mx - this.lastX, my - this.lastY, delta);
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

    private final class TaskList extends Widget implements UI.CursorProvider {
        private static final int ROW = 18;

        private int rowAt(double my) {
            return (int)Math.floor((my - this.y - 4.0) / 18.0);
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.fill(this.x, this.y, this.w, this.h, -14934490);
            c.fill(this.x + this.w - 1, this.y, 1, this.h, -13749952);
            int ry = this.y + 4;
            boolean hov0 = this.isHovered() && this.rowAt(my) == 0;
            c.round(this.x + 4, ry, this.w - 8, 17, 3, QuestTab.this.sel.isEmpty() ? 1435200767 : (hov0 ? -14210506 : 0));
            c.icon(Icon.FLAG, this.x + 8, ry + 5, -11686);
            c.textEllipsized(QuestTab.this.doc.title, this.x + 22, ry + 5, this.w - 30, -1841682);
            ry += 18;

            for (int i = 0; i < QuestTab.this.doc.tasks.size(); i++) {
                QuestDef.Task t = QuestTab.this.doc.tasks.get(i);
                boolean on = t.id.equals(QuestTab.this.sel);
                boolean hov = this.isHovered() && this.rowAt(my) == i + 1;
                c.round(this.x + 4, ry, this.w - 8, 17, 3, on ? 1435200767 : (hov ? -14210506 : 0));
                c.fill(this.x + 14, ry + 5, 3, 8, t.hidden ? -9533697 : (t.optional ? -7564888 : -8658806));
                c.textEllipsized(QuestGoals.text(t, QuestGoals::shortId), this.x + 22, ry + 5, this.w - 52, -6577995);
                if (t.count > 1) {
                    String n = "×" + t.count;
                    c.text(n, this.x + this.w - 8 - c.width(n), ry + 5, -9933435);
                }

                ry += 18;
            }
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            int i = this.rowAt(my);
            if (i == 0) {
                QuestTab.this.select("");
            } else if (i > 0 && i <= QuestTab.this.doc.tasks.size()) {
                QuestTab.this.select(QuestTab.this.doc.tasks.get(i - 1).id);
            }

            return true;
        }

        @Override
        public int cursor(int mx, int my) {
            return 221188;
        }
    }
}
