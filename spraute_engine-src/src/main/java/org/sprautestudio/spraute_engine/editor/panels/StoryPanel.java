package org.sprautestudio.spraute_engine.editor.panels;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.DebugPlate;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class StoryPanel extends Widget {
    private final Box toolbar = Box.row().gap(3).pad(6, 3);
    private final TextField search = new TextField().placeholder("Фильтр").icon(Icon.SEARCH);
    private final StoryPanel.StoryList list = new StoryPanel.StoryList();
    private String filter = "Все";
    public Runnable showConsole;
    public IntConsumer openInEditor;
    private final Button plate;

    public StoryPanel() {
        this.add(this.toolbar);
        this.add(this.list);
        this.tool(Icon.REFRESH, "Обновить", () -> ClientState.get().requestStatus());
        this.plate = this.tool(
            Icon.INFO,
            "Плашка в мире: что ждут сюжеты и их on (видна, пока включён режим редактора /editor on)",
            () -> DebugPlate.setShown(!Workspace.settings().debugPlate)
        );
        this.tool(Icon.STOP, "Остановить все сюжеты (/script stop all)", () -> run("script stop all")).iconColor(-1023878);
        Button startMany = new Button(
                "Запустить…",
                () -> Dialog.prompt(
                    "Запустить сюжеты",
                    "Id через пробел или запятую:",
                    "",
                    v -> v.matches("[0-9 ,]+") ? null : "Только числа",
                    v -> run("script start all \"" + v.replace(",", " ").trim() + "\"")
                )
            )
            .icon(Icon.PLAY)
            .style(Button.Style.GHOST);
        startMany.autoWidth();
        startMany.tooltip("/script start all <id...>");
        this.toolbar.add(startMany);
        this.menuButton("Категории", Icon.LIST, pm -> {
            List<String> cats = ClientState.get().status().categories();
            if (cats.isEmpty()) {
                pm.item("(нет категорий)", null).enabled(() -> false);
            }

            for (String c : cats) {
                pm.item("Запустить " + c, Icon.PLAY, null, () -> run("script start category " + c)).color(-11746422);
            }

            if (!cats.isEmpty()) {
                pm.separator();
            }

            for (String c : cats) {
                pm.item("Остановить " + c, Icon.STOP, null, () -> run("script stop category " + c)).color(-1023878);
            }
        });
        this.menuButton("Остановить", Icon.STOP, pm -> {
            pm.item("Все сюжеты", Icon.STOP, null, () -> run("script stop all"));
            pm.item("Только сюжеты (scripts)", Icon.STOP, null, () -> run("script stop scripts"));
            pm.item("Фоновые (backscripts)", Icon.STOP, null, () -> run("script stop backscripts"));
        });
        this.menuButton("Окружение", Icon.GEAR, pm -> {
            pm.item("Состояние окружения", Icon.INFO, null, () -> this.runShow("script environment"));
            pm.item("Активные сюжеты", Icon.LIST, null, () -> this.runShow("script environment active"));
        });
        Dropdown<String> filterBox = new Dropdown<>(() -> List.of("Все", "Активные"), () -> this.filter, s -> s, s -> this.filter = s);
        filterBox.prefW = 96;
        this.toolbar.add(new Widget()).flex = 1.0F;
        this.toolbar.add(filterBox);
        this.search.prefW = 130;
        this.toolbar.add(this.search);
    }

    private Button tool(Icon icon, String tooltip, Runnable action) {
        return this.toolbar.add(Button.icon(icon, tooltip, action));
    }

    private void menuButton(String text, Icon icon, Consumer<PopupMenu> build) {
        Button b = new Button(text, null).icon(icon).style(Button.Style.GHOST);
        b.onClick = () -> {
            PopupMenu pm = new PopupMenu();
            build.accept(pm);
            pm.show(b.x, b.y + b.h + 1, b);
        };
        b.autoWidth();
        b.prefW += 4;
        this.toolbar.add(b);
    }

    private static void run(String command) {
        ClientState.get().command(command);
    }

    private void runShow(String command) {
        run(command);
        if (this.showConsole != null) {
            this.showConsole.run();
        }
    }

    @Override
    public void layout() {
        this.toolbar.bounds(this.x, this.y, this.w, 24);
        this.toolbar.layout();
        this.list.bounds(this.x, this.y + 24, this.w, this.h - 24);
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -14934490);
        c.fill(this.x, this.y + 23, this.w, 1, -13749952);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.visible && this.isShown() && ClientState.get().statusAge() > 1000L) {
            ClientState.get().requestStatus();
        }

        this.plate.iconColor(Workspace.settings().debugPlate ? -11746422 : -9933435);
    }

    private List<Packets.ScriptEntry> visibleEntries() {
        String q = this.search.text().trim().toLowerCase();
        List<Packets.ScriptEntry> out = new ArrayList<>();

        for (Packets.ScriptEntry e : ClientState.get().status().scripts()) {
            String var5 = this.filter;
            switch (var5) {
                case "Активные":
                    if (!e.active) {
                        break;
                    }
                default:
                    if (q.isEmpty() || ("#" + e.id + " " + e.title + " " + e.project).toLowerCase().contains(q)) {
                        out.add(e);
                    }
            }
        }

        out.sort((a, b) -> a.active != b.active ? (a.active ? -1 : 1) : Integer.compare(a.id, b.id));
        return out;
    }

    private final class StoryList extends Widget implements UI.CursorProvider {
        private static final int ROW = 24;
        private static final int BTN = 18;
        private static final int LINE = 14;
        private double scroll;
        private final Set<Integer> open = new HashSet<>();
        private static final String[] ACTIONS = new String[]{"run", "pause", "setup", "goto", "trigger", "more"};

        private List<StoryPanel.StoryList.Row> rows() {
            List<StoryPanel.StoryList.Row> out = new ArrayList<>();
            Packets.Status st = ClientState.get().status();

            for (Packets.ScriptEntry e : StoryPanel.this.visibleEntries()) {
                out.add(new StoryPanel.StoryList.Row(e, true, "", "", 0, null, 0));
                if (this.open.contains(e.id)) {
                    if (!e.active) {
                        out.add(new StoryPanel.StoryList.Row(e, false, "Не запущен", "", -9933435, null, 1));
                    } else {
                        out.add(new StoryPanel.StoryList.Row(e, false, "Сейчас", e.doing.isEmpty() ? "—" : e.doing, -1841682, null, 1));
                        if (!e.branches.isEmpty()) {
                            out.add(new StoryPanel.StoryList.Row(e, false, "Ветки и on", "", -9933435, null, 1));
                        }

                        for (Packets.Branch b : e.branches) {
                            String what = b.count() >= 0 ? "сработал " + b.count() + " раз · " + b.doing() : b.doing();
                            out.add(new StoryPanel.StoryList.Row(e, false, b.name(), what, b.count() >= 0 ? -870325 : -33098, null, 2));
                        }

                        out.add(new StoryPanel.StoryList.Row(e, false, "Переменные сюжета", e.vars.isEmpty() ? "нет" : "", -9933435, null, 1));

                        for (Packets.Value v : e.vars) {
                            out.add(
                                new StoryPanel.StoryList.Row(
                                    e,
                                    false,
                                    v.name(),
                                    v.value(),
                                    -6255873,
                                    () -> this.edit(
                                        "Переменная сюжета #" + e.id, v.name(), v.value(), val -> ClientState.get().debugSet(e.id, "var", v.name(), val)
                                    ),
                                    2
                                )
                            );
                        }

                        out.add(
                            new StoryPanel.StoryList.Row(
                                e,
                                false,
                                "+ переменная сюжета",
                                "",
                                -6577995,
                                () -> Dialog.prompt(
                                    "Новая переменная",
                                    "Имя:",
                                    "",
                                    n -> n.matches("[\\p{L}_][\\p{L}0-9_]*") ? null : "Буквы, цифры и _",
                                    n -> this.edit("Переменная сюжета #" + e.id, n, "0", val -> ClientState.get().debugSet(e.id, "var", n, val))
                                ),
                                2
                            )
                        );
                        out.add(new StoryPanel.StoryList.Row(e, false, "Игрок", "", -9933435, null, 1));
                        if (st.coins() >= 0) {
                            out.add(
                                new StoryPanel.StoryList.Row(
                                    e,
                                    false,
                                    "монеты",
                                    String.valueOf(st.coins()),
                                    -870325,
                                    () -> this.edit(
                                        "Монеты игрока", "монеты", String.valueOf(st.coins()), val -> ClientState.get().debugSet(e.id, "coins", "", val)
                                    ),
                                    2
                                )
                            );
                        }

                        for (Packets.Value v : st.playerVars()) {
                            out.add(
                                new StoryPanel.StoryList.Row(
                                    e,
                                    false,
                                    v.name(),
                                    v.value(),
                                    -6255873,
                                    () -> this.edit(
                                        "Переменная игрока (pvar)", v.name(), v.value(), val -> ClientState.get().debugSet(e.id, "pvar", v.name(), val)
                                    ),
                                    2
                                )
                            );
                        }

                        out.add(
                            new StoryPanel.StoryList.Row(
                                e,
                                false,
                                "+ переменная игрока",
                                "",
                                -6577995,
                                () -> Dialog.prompt(
                                    "Новая переменная игрока",
                                    "Имя:",
                                    "",
                                    n -> n.isBlank() ? "Введите имя" : null,
                                    n -> this.edit("Переменная игрока (pvar)", n, "0", val -> ClientState.get().debugSet(e.id, "pvar", n.trim(), val))
                                ),
                                2
                            )
                        );
                    }
                }
            }

            return out;
        }

        private void edit(String title, String name, String now, Consumer<String> set) {
            Dialog.prompt(title, name + " = (как в сюжете: 5, \"текст\", true, [1, 2])", now, v -> null, v -> {
                set.accept(v);
                ClientState.get().requestStatus();
            });
        }

        private StoryPanel.StoryList.Row rowAt(double my, int[] top) {
            int ry = this.y - (int)this.scroll;

            for (StoryPanel.StoryList.Row r : this.rows()) {
                if (my >= ry && my < ry + r.height()) {
                    if (top != null) {
                        top[0] = ry;
                    }

                    return r;
                }

                ry += r.height();
            }

            return null;
        }

        private int buttonsX() {
            return this.x + this.w - ACTIONS.length * 20 - 8;
        }

        private int actionAt(double mx) {
            int bx = this.buttonsX();

            for (int i = 0; i < ACTIONS.length; i++) {
                int ax = bx + i * 20;
                if (mx >= ax && mx < ax + 18) {
                    return i;
                }
            }

            return -1;
        }

        @Override
        public int cursor(int mx, int my) {
            StoryPanel.StoryList.Row r = this.rowAt(my, null);
            if (r == null) {
                return 221185;
            } else {
                return r.head ? (this.actionAt(mx) >= 0 ? 221188 : 221185) : (r.click != null ? 221188 : 221185);
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            List<Packets.ScriptEntry> entries = StoryPanel.this.visibleEntries();
            Packets.Status st = ClientState.get().status();
            c.pushClip(this.x, this.y, this.w, this.h);
            if (!st.ready()) {
                c.wrapped(
                    Minecraft.getInstance().getConnection() == null
                        ? "Войдите в мир, чтобы управлять сюжетами."
                        : "У вас нет прав оператора, или мир ещё загружается.",
                    this.x + 10,
                    this.y + 10,
                    this.w - 20,
                    -9933435,
                    2
                );
            }

            List<StoryPanel.StoryList.Row> rows = this.rows();
            int total = 0;

            for (StoryPanel.StoryList.Row r : rows) {
                total += r.height();
            }

            int max = Math.max(0, total - this.h);
            this.scroll = Math.max(0.0, Math.min((double)max, this.scroll));
            int ry = this.y - (int)this.scroll;

            for (StoryPanel.StoryList.Row r : rows) {
                if (ry > this.y + this.h) {
                    break;
                }

                if (ry + r.height() >= this.y) {
                    if (r.head) {
                        this.paintRow(c, r.e, ry, mx, my);
                    } else {
                        this.paintLine(c, r, ry, mx, my);
                    }
                }

                ry += r.height();
            }

            c.popClip();
        }

        private void paintLine(Canvas c, StoryPanel.StoryList.Row r, int ry, int mx, int my) {
            boolean hov = this.isHovered() && r.click != null && my >= ry && my < ry + 14;
            c.fill(this.x, ry, this.w, 14, Theme.alpha(-7639809, 0.05F));
            if (hov) {
                c.fill(this.x, ry, this.w, 14, 352321535);
            }

            int lx = this.x + 22 + r.indent * 12;
            if (r.value.isEmpty()) {
                c.textEllipsized(r.text, lx, ry + 3, this.w - (lx - this.x) - 8, r.color);
            } else {
                int nameW = Math.min(160, Math.max(60, c.width(r.text) + 8));
                c.textEllipsized(r.text, lx, ry + 3, nameW - 4, r.color);
                c.textEllipsized(r.value, lx + nameW, ry + 3, this.w - (lx - this.x) - nameW - 8, -6577995);
            }
        }

        private void paintRow(Canvas c, Packets.ScriptEntry e, int ry, int mx, int my) {
            boolean hov = this.isHovered() && my >= ry && my < ry + 24;
            if (hov) {
                c.fill(this.x, ry, this.w, 24, 352321535);
            }

            c.fill(this.x + 8, ry + 24 - 1, this.w - 16, 1, 285212671);
            int dot = e.active ? (e.paused ? -870325 : -11746422) : -9933435;
            c.round(this.x + 10, ry + 9, 6, 6, 3, dot);
            c.icon(this.open.contains(e.id) ? Icon.CHEVRON_DOWN : Icon.CHEVRON_RIGHT, this.x + 1, ry + 8, -9933435);
            c.text("#" + e.id, this.x + 22, ry + 8, -6577995);
            int tx = this.x + 64;
            int bx = this.buttonsX();
            int progressW = 110;
            int titleMax = bx - tx - progressW - 16;
            String title = e.title.isEmpty() ? "Сюжет " + e.id : e.title;
            if (e.spraute) {
                int bw = c.width("Spraute") + 8;
                c.round(tx, ry + 6, bw, 12, 4, Theme.alpha(-7639809, 0.25F));
                c.text("Spraute", tx + 4, ry + 8, -6255873);
                tx += bw + 6;
                titleMax -= bw + 6;
            }

            c.textEllipsized(title, tx, ry + 8, titleMax, e.active ? -1841682 : -6577995);
            int px = bx - progressW - 10;
            if (e.active && e.actionCount > 0) {
                float p = Math.min(1.0F, (float)e.actionIndex / e.actionCount);
                c.round(px, ry + 15, progressW, 4, 2, -13749952);
                c.round(px, ry + 15, Math.max(4, (int)(progressW * p)), 4, 2, e.paused ? -870325 : -12988235);
                String stage = e.stage != null && !e.stage.equals("null") ? e.stage : "";
                c.textEllipsized(e.actionIndex + "/" + e.actionCount + (stage.isEmpty() ? "" : " · " + stage), px, ry + 4, progressW, -9933435);
            } else {
                c.text(e.active ? "активен" : "не запущен", px, ry + 8, -9933435);
            }

            for (int i = 0; i < ACTIONS.length; i++) {
                int ax = bx + i * 20;
                int ay = ry + 3;
                boolean hot = hov && mx >= ax && mx < ax + 18 && my >= ay && my < ay + 18;
                boolean enabled = this.isEnabled(ACTIONS[i], e);
                if (hot && enabled) {
                    c.round(ax, ay, 18, 18, 3, -13618102);
                }

                String var21 = ACTIONS[i];
                Icon icon;
                int color;
                switch (var21) {
                    case "run":
                        icon = e.active ? Icon.STOP : Icon.PLAY;
                        color = e.active ? -1023878 : -11746422;
                        break;
                    case "pause":
                        icon = e.paused ? Icon.PLAY : Icon.PAUSE;
                        color = -870325;
                        break;
                    case "setup":
                        icon = Icon.SETUP;
                        color = -10638858;
                        break;
                    case "goto":
                        icon = Icon.TAG;
                        color = -33098;
                        break;
                    case "trigger":
                        icon = Icon.BOLT;
                        color = -870325;
                        break;
                    default:
                        icon = Icon.MENU;
                        color = -6577995;
                }

                c.icon(icon, ax + 4, ay + 4, enabled ? color : Theme.alpha(color, 0.3F));
            }
        }

        private boolean isEnabled(String action, Packets.ScriptEntry e) {
            return switch (action) {
                case "pause", "trigger" -> e.active;
                default -> true;
            };
        }

        @Override
        public String tooltipText(int mx, int my) {
            StoryPanel.StoryList.Row row = this.rowAt(my, null);
            if (row != null && !row.head) {
                return row.click != null ? "Нажмите, чтобы изменить" : (row.value.isEmpty() ? null : row.text + "\n" + row.value);
            } else {
                Packets.ScriptEntry e = this.entryAt(my);
                if (e == null) {
                    return null;
                } else {
                    int a = this.actionAt(mx);
                    if (a < 0) {
                        return "#"
                            + e.id
                            + " "
                            + e.title
                            + "\nНажмите — развернуть: что ждёт, ветки и on, переменные"
                            + (e.project.isEmpty() ? "" : "\nПроект: " + e.project)
                            + (e.active ? "\nЭтап: " + e.stage + "\nДействие " + e.actionIndex + " из " + e.actionCount : "");
                    } else {
                        String var6 = ACTIONS[a];

                        return switch (var6) {
                            case "run" -> e.active ? "Остановить (/script stop " + e.id + ")" : "Запустить (/script start " + e.id + ")";
                            case "pause" -> e.paused ? "Продолжить (/script resume)" : "Пауза (/script pause)";
                            case "setup" -> "Подготовить (/script setup " + e.id + ")";
                            case "goto" -> e.active ? "Перейти к метке (/script goto)" : "Запустить с метки (/script start <id> <метка>)";
                            case "trigger" -> "Триггер (/script trigger)";
                            default -> "Ещё: индекс, фаза, действия, справка, данные";
                        };
                    }
                }
            }
        }

        private Packets.ScriptEntry entryAt(double my) {
            StoryPanel.StoryList.Row r = this.rowAt(my, null);
            return r != null && r.head ? r.e : null;
        }

        @Override
        public boolean mousePressed(double mx, double my, int button) {
            StoryPanel.StoryList.Row row = this.rowAt(my, null);
            if (row != null && !row.head) {
                if (row.click != null && button == 0) {
                    row.click.run();
                }

                return true;
            } else {
                Packets.ScriptEntry e = this.entryAt(my);
                if (e == null) {
                    return true;
                } else {
                    int a = this.actionAt(mx);
                    if (a < 0 && button == 0) {
                        if (!this.open.remove(e.id)) {
                            this.open.add(e.id);
                            ClientState.get().requestStatus();
                        }

                        return true;
                    } else if (button != 1 && (a < 0 || !ACTIONS[a].equals("more"))) {
                        if (a >= 0 && this.isEnabled(ACTIONS[a], e)) {
                            String var9 = ACTIONS[a];
                            switch (var9) {
                                case "run":
                                    StoryPanel.run(e.active ? "script stop " + e.id : "script start " + e.id);
                                    break;
                                case "pause":
                                    StoryPanel.run(e.paused ? "script resume " + e.id : "script pause " + e.id);
                                    break;
                                case "setup":
                                    StoryPanel.run("script setup " + e.id);
                                    break;
                                case "goto":
                                    this.labelsMenu(e, (int)mx, (int)my);
                                    break;
                                case "trigger":
                                    if (e.spraute) {
                                        StoryPanel.run("script trigger " + e.id + " next");
                                    } else {
                                        Dialog.prompt(
                                            "Триггер",
                                            "Имя триггера сюжета #" + e.id + ":",
                                            "",
                                            v -> v.isEmpty() ? "Введите имя" : null,
                                            v -> StoryPanel.run("script trigger " + e.id + " " + v)
                                        );
                                    }
                            }

                            return true;
                        } else {
                            return true;
                        }
                    } else {
                        this.moreMenu(e, (int)mx, (int)my);
                        return true;
                    }
                }
            }
        }

        private void labelsMenu(Packets.ScriptEntry e, int mx, int my) {
            PopupMenu pm = new PopupMenu();
            if (e.labels.isEmpty()) {
                pm.item(e.active ? "(меток нет)" : "(метки видны у запущенного сюжета)", null).enabled(() -> false);
            }

            for (String label : e.labels) {
                pm.item(label, Icon.TAG, null, () -> StoryPanel.run(e.active ? "script goto " + e.id + " " + label : "script start " + e.id + " " + label))
                    .color(-33098);
            }

            pm.separator();
            pm.item(
                "Ввести метку…",
                Icon.EDIT,
                null,
                () -> Dialog.prompt(
                    "Метка",
                    "Имя метки:",
                    "",
                    v -> v.isEmpty() ? "Введите имя" : null,
                    v -> StoryPanel.run(e.active ? "script goto " + e.id + " " + v : "script start " + e.id + " " + v)
                )
            );
            pm.show(mx, my);
        }

        private void moreMenu(Packets.ScriptEntry e, int mx, int my) {
            PopupMenu pm = new PopupMenu();
            pm.item(
                e.active ? "Остановить" : "Запустить",
                e.active ? Icon.STOP : Icon.PLAY,
                null,
                () -> StoryPanel.run(e.active ? "script stop " + e.id : "script start " + e.id)
            );
            pm.item("Подготовить (setup)", Icon.SETUP, null, () -> StoryPanel.run("script setup " + e.id));
            pm.item(
                    "Перейти к действию №…",
                    Icon.SKIP,
                    null,
                    () -> Dialog.prompt(
                        "Переход",
                        "Номер действия (0–" + Math.max(0, e.actionCount - 1) + "):",
                        String.valueOf(e.actionIndex),
                        v -> v.matches("\\d+") ? null : "Введите число",
                        v -> StoryPanel.run("script jump " + e.id + " " + v)
                    )
                )
                .enabled(() -> e.active);
            pm.separator();
            pm.item("Фаза (phase)", Icon.INFO, null, () -> StoryPanel.this.runShow("script phase " + e.id)).enabled(() -> e.active);
            pm.item("Действия (actions)", Icon.LIST, null, () -> StoryPanel.this.runShow("script actions " + e.id));
            pm.item("Данные (data)", Icon.FILE, null, () -> StoryPanel.this.runShow("script data " + e.id)).enabled(() -> e.active);
            if (e.spraute) {
                pm.separator();
                if (StoryPanel.this.openInEditor != null) {
                    pm.item("Открыть в редакторе", Icon.EDIT, null, () -> StoryPanel.this.openInEditor.accept(e.id));
                }

                pm.item(
                        "Выгрузить из игры",
                        Icon.TRASH,
                        null,
                        () -> Dialog.confirm(
                            "Выгрузить сюжет",
                            "Сюжет #" + e.id + " будет остановлен и удалён из мира. Файл в проекте останется.",
                            "Выгрузить",
                            true,
                            () -> ClientState.get().undeploy(e.id)
                        )
                    )
                    .color(-1023878);
            }

            pm.show(mx, my);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            this.scroll -= delta * 24.0 * 2.0;
            return true;
        }

        private record Row(Packets.ScriptEntry e, boolean head, String text, String value, int color, Runnable click, int indent) {
            int height() {
                return this.head ? 24 : 14;
            }
        }
    }
}
