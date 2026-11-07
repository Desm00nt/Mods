package org.sprautestudio.spraute_engine.editor.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class DocsTab extends EditorTab {
    private final TextField search = new TextField().placeholder("Поиск по справке").icon(Icon.SEARCH);
    private final List<DocsTab.Entry> entries = new ArrayList<>();
    private double scroll;
    private double target;
    private int contentH;

    public DocsTab() {
        super(null, null);
        this.add(this.search);
        this.search.onChange = s -> this.target = this.scroll = 0.0;
        this.build();
    }

    private void build() {
        this.entries.add(new DocsTab.Entry(0, "Spraute Story — язык сюжетов", null, -7639809));
        this.entries
            .add(
                new DocsTab.Entry(
                    2,
                    null,
                    "Файл .sst описывает один сюжет Spraute. Первая строка — заголовок:\n    script 5001 \"Название\"\nЗатем объявления (character, location, var) и команды — по одной на строку.\nМетки (label) видны в панели «Сюжеты»: можно запустить сюжет с метки или перейти к ней.\nВремя: 20t — тики, 1.5s — секунды, 2m — минуты. Строки поддерживают подстановки: \"Привет, {playername()}\".\nГорячие клавиши: Ctrl+Space — подсказки, Ctrl+клик / F12 — к объявлению, Alt+P — ваши координаты,\nCtrl+/ — комментарий, Ctrl+D — дублировать строку, Alt+↑↓ — переместить строку, Ctrl+Shift+F — форматировать,\nF5 — загрузить и запустить, F6 — только загрузить, Shift+F5 — остановить.",
                    0
                )
            );
        this.entries.add(new DocsTab.Entry(0, "Экспорт как мод", null, -7639809));
        this.entries
            .add(
                new DocsTab.Entry(
                    2,
                    null,
                    "Файл → «Экспорт как мод…» делает из проекта обычный мод в spraute/exports. Его кладут в папку mods.\nСюжеты сами загружаются в каждый мир, autostart запускается при входе, общие скрипты и меню работают с запуска игры.\n«Всё внутри» кладёт в файл движок, аддоны и (на Fabric) Fabric API: для того загрузчика и той версии, из которой экспортируете.\n«Только проект» подходит для любого загрузчика и версии, но движок ставится отдельно.\nВидео и FFmpeg: видео лежат в моде, FFmpeg скачивается при первом запуске. Проект автора с тем же именем мод не трогает.\nРедактор по умолчанию скрыт от игроков: нет клавиши и команды /editor. На компьютере с вашим проектом он остаётся.",
                    0
                )
            );
        this.entries.add(new DocsTab.Entry(0, "Общие скрипты", null, -7639809));
        this.entries
            .add(
                new DocsTab.Entry(
                    2,
                    null,
                    "Общий скрипт работает в самой игре: с её запуска и до закрытия, ещё до входа в мир. Заголовок:\n    script 9001 \"Меню\" general\nСервера, мира и игрока здесь нет: нет персонажей, диалогов и команд мира. Есть переменные, условия, циклы, функции,\nwait, on, every, log, sound, команды интерфейса и «Игра и меню»: menu open, world open, server join, link open, game quit.\nКнопка меню: on button \"id\" { ... }. Если ответить на кнопку самой игры (например menu.singleplayer), её обычное действие заменяется.\nОткрытие меню: on menu \"title\" { ... }. gui open открывает интерфейс проекта как экран.\nF5 перезапускает общие скрипты проекта. Запускаются только скрипты проектов, установленных в игре: присланное сервером здесь не выполняется.",
                    0
                )
            );
        this.entries.add(new DocsTab.Entry(0, "Ключевые слова", null, -3697942));

        for (Map.Entry<String, String> e : Specs.keywordDocs().entrySet()) {
            this.entries.add(new DocsTab.Entry(1, e.getKey(), e.getValue(), -3697942));
        }

        for (String cat : Specs.CATEGORIES) {
            this.entries.add(new DocsTab.Entry(0, "Команды: " + cat, null, Theme.category(cat)));

            for (CommandSpec s : Specs.commands()) {
                if (s.category.equals(cat)) {
                    StringBuilder body = new StringBuilder(s.doc);

                    for (CommandSpec.Param p : s.allParams()) {
                        body.append("\n  ").append(p.name).append(" — ").append(p.type.title);
                    }

                    this.entries.add(new DocsTab.Entry(1, s.signature(), body.toString(), -8213761));
                }
            }
        }

        this.entries.add(new DocsTab.Entry(0, "Функции в выражениях", null, -10880));

        for (Specs.FunctionSpec f : Specs.functions()) {
            this.entries.add(new DocsTab.Entry(1, f.signature() + " → " + f.returns(), f.doc(), -10880));
        }

        this.entries.add(new DocsTab.Entry(0, "Типы значений", null, -553876));

        for (ParamType t : ParamType.values()) {
            this.entries.add(new DocsTab.Entry(1, t.title, t.doc, -553876));
        }

        this.entries.add(new DocsTab.Entry(0, "Эмоции диалогов", null, -3938163));
        this.entries.add(new DocsTab.Entry(2, null, String.join(", ", Specs.EMOTIONS), 0));
    }

    @Override
    public String title() {
        return "Справочник";
    }

    @Override
    public void layout() {
        this.search.bounds(this.x + 16, this.y + 8, Math.min(320, this.w - 32), 18);
    }

    private boolean matches(DocsTab.Entry e, String q) {
        if (!q.isEmpty() && e.kind != 0) {
            String all = ((e.title == null ? "" : e.title) + " " + (e.body == null ? "" : e.body)).toLowerCase();
            return all.contains(q);
        } else {
            return true;
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (Math.abs(this.target - this.scroll) > 0.5) {
            this.scroll = this.scroll + (this.target - this.scroll) * Math.min(1.0F, dt * 18.0F);
        } else {
            this.scroll = this.target;
        }

        c.fill(this.x, this.y, this.w, this.h, -15461093);
        int top = this.y + 34;
        c.pushClip(this.x, top, this.w, this.h - 34);
        int width = Math.min(this.w - 48, 620);
        int left = this.x + 24;
        int yy = top + 4 - (int)this.scroll;
        String q = this.search.text().trim().toLowerCase();

        for (int i = 0; i < this.entries.size(); i++) {
            DocsTab.Entry e = this.entries.get(i);
            if (this.matches(e, q)) {
                if (e.kind == 0 && !q.isEmpty()) {
                    boolean any = false;

                    for (int j = i + 1; j < this.entries.size() && this.entries.get(j).kind != 0; j++) {
                        if (this.matches(this.entries.get(j), q)) {
                            any = true;
                        }
                    }

                    if (!any) {
                        continue;
                    }
                }

                int eh = this.height(c, e, width);
                if (yy + eh > top && yy < this.y + this.h) {
                    this.paintEntry(c, e, left, yy, width);
                }

                yy += eh;
            }
        }

        this.contentH = yy + (int)this.scroll - top + 20;
        c.popClip();
        c.fill(this.x, top - 1, this.w, 1, -13749952);
    }

    private int height(Canvas c, DocsTab.Entry e, int width) {
        return switch (e.kind) {
            case 0 -> 30;
            case 1 -> 14 + (e.body == null ? 0 : c.wrappedHeight(e.body, width - 16, 2)) + 8;
            default -> c.wrappedHeight(e.body, width, 2) + 8;
        };
    }

    private void paintEntry(Canvas c, DocsTab.Entry e, int left, int yy, int width) {
        switch (e.kind) {
            case 0:
                c.round(left, yy + 12, 4, 12, 2, e.color);
                c.pushTransform(left + 10, yy + 13, 1.25F);
                c.text(e.title, 0, 0, -1841682);
                c.popTransform();
                break;
            case 1:
                c.round(left, yy, width, this.height(c, e, width) - 4, 4, -14934490);
                c.text(e.title, left + 8, yy + 4, e.color);
                if (e.body != null) {
                    c.wrapped(e.body, left + 8, yy + 16, width - 16, -6577995, 2);
                }
                break;
            default:
                c.wrapped(e.body, left, yy, width, -6577995, 2);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        this.target = Math.max(0.0, Math.min((double)Math.max(0, this.contentH - (this.h - 34)), this.target - delta * 40.0));
        return true;
    }

    @Override
    public Icon icon() {
        return Icon.BOOK;
    }

    @Override
    public int iconColor() {
        return -12988235;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void save() {
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
        this.search.requestFocus();
    }

    private record Entry(int kind, String title, String body, int color) {
    }
}
