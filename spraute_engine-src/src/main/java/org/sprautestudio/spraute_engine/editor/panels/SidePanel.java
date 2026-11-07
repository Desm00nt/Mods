package org.sprautestudio.spraute_engine.editor.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.editor.tabs.ScriptTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;
import org.sprautestudio.spraute_engine.ui.widgets.TabBar;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class SidePanel extends Widget {
    private final TabBar tabs = new TabBar();
    private final ListView<SidePanel.Row> outline;
    private final TextField search = new TextField().placeholder("Поиск команды").icon(Icon.SEARCH);
    private final ListView<SidePanel.Cmd> commands;
    private final Supplier<EditorTab> active;
    private long lastRefresh;
    private SidePanel.Cmd selectedCmd;
    private final TabBar.Tab tInspector;
    private final TabBar.Tab tOutlineTab;
    private Widget inspector;

    public SidePanel(Supplier<EditorTab> active) {
        this.active = active;
        this.tabs.editorStyle = false;
        TabBar.Tab tOutline = this.tabs.add(new TabBar.Tab(() -> "Структура", Icon.LIST, "outline"));
        this.tInspector = this.tabs.add(new TabBar.Tab(() -> "Инспектор", Icon.GEAR, "inspector"));
        this.tabs.add(new TabBar.Tab(() -> "Команды", Icon.BOLT, "commands"));
        this.tOutlineTab = tOutline;
        this.tabs.select(tOutline);
        this.tabs.onSelect = t -> this.layout();
        this.add(this.tabs);
        this.outline = this.add(new ListView<>((c, r, x, y, w, h, sel, hov) -> {
            if (r.header != null) {
                c.text(r.header, x + 6, y + 5, -9933435);
            } else {
                Analyzer.Symbol s = r.symbol;
                Icon icon;
                int color;
                switch (s.kind()) {
                    case CHARACTER:
                        icon = Icon.USER;
                        color = -11544898;
                        break;
                    case LOCATION:
                        icon = Icon.PIN;
                        color = -13461;
                        break;
                    case VARIABLE:
                        icon = Icon.DOT;
                        color = -8531969;
                        break;
                    default:
                        icon = Icon.TAG;
                        color = -33098;
                }

                c.icon(icon, x + 12, y + 3, color);
                c.text(s.name(), x + 26, y + 4, -1841682);
                if (s.detail() != null && !s.detail().isEmpty()) {
                    int nx = x + 32 + c.width(s.name());
                    c.textEllipsized(s.detail(), nx, y + 4, w - (nx - x) - 6, -9933435);
                }
            }
        }));
        this.outline.rowHeight = 15;
        this.outline.background = -14934490;
        this.outline.emptyText = "Откройте скрипт, чтобы увидеть персонажей, точки, переменные и метки.";
        this.outline.activateOnSingleClick = true;
        this.outline.onActivate = r -> {
            EditorTab tab = active.get();
            if (r.symbol != null && tab != null) {
                tab.reveal(r.symbol.start());
            }
        };
        this.add(this.search);
        this.search.onChange = s -> this.refreshCommands();
        this.commands = this.add(new ListView<>((c, cmd, x, y, w, h, sel, hov) -> {
            c.round(x + 8, y + 5, 4, 7, 2, Theme.category(cmd.category));
            c.textEllipsized(cmd.title, x + 18, y + 4, w - 24, -1841682);
        }));
        this.commands.rowHeight = 15;
        this.commands.background = -14934490;
        this.commands.onSelect = cmd -> this.selectedCmd = cmd;
        this.commands.onActivate = cmd -> {
            EditorTab tab = active.get();
            if (tab instanceof ScriptTab st) {
                st.editor.insertAtCaret(cmd.snippet + "\n");
            }
        };
        this.refreshCommands();
    }

    private void refreshCommands() {
        String q = this.search.text().trim().toLowerCase();
        List<SidePanel.Cmd> list = new ArrayList<>();

        for (CommandSpec s : Specs.commands()) {
            SidePanel.Cmd cmd = new SidePanel.Cmd(s.signature(), s.snippet(), s.doc, s.category);
            if (q.isEmpty() || (s.signature() + " " + s.doc).toLowerCase().contains(q)) {
                list.add(cmd);
            }
        }

        this.commands.setItems(list);
    }

    private boolean isOutline() {
        TabBar.Tab t = this.tabs.selected();
        return t == null || "outline".equals(t.data);
    }

    private boolean isInspector() {
        return this.tabs.selected() == this.tInspector;
    }

    private void syncInspector() {
        EditorTab tab = this.active.get();
        Widget wanted = tab != null ? tab.inspector() : null;
        if (wanted != this.inspector) {
            if (this.inspector != null) {
                this.remove(this.inspector);
            }

            this.inspector = wanted;
            if (this.inspector != null) {
                this.add(this.inspector);
                this.tabs.select(this.tInspector);
            } else if (this.isInspector()) {
                this.tabs.select(this.tOutlineTab);
            }

            this.layout();
        }
    }

    @Override
    public void layout() {
        this.tabs.bounds(this.x, this.y, this.w, 22);
        boolean o = this.isOutline();
        boolean ins = this.isInspector();
        this.outline.visible = o;
        this.search.visible = this.commands.visible = !o && !ins;
        if (this.inspector != null) {
            this.inspector.visible = ins;
            this.inspector.bounds(this.x, this.y + 22, this.w, this.h - 22);
            this.inspector.layout();
        }

        this.outline.bounds(this.x, this.y + 22, this.w, this.h - 22);
        this.search.bounds(this.x + 6, this.y + 26, this.w - 12, 18);
        int docH = 90;
        this.commands.bounds(this.x, this.y + 48, this.w, this.h - 48 - docH);
    }

    @Override
    public void tick() {
        super.tick();
        this.syncInspector();
        long now = System.currentTimeMillis();
        if (now - this.lastRefresh >= 500L && this.isShown()) {
            this.lastRefresh = now;
            EditorTab tab = this.active.get();
            Analyzer.Result r = tab != null ? tab.analysis() : null;
            List<SidePanel.Row> rows = new ArrayList<>();
            if (r != null) {
                section(rows, "ПЕРСОНАЖИ", r.characters.values());
                section(rows, "ТОЧКИ", r.locations.values());
                section(rows, "ПЕРЕМЕННЫЕ", r.vars.values());
                section(rows, "МЕТКИ", r.labels.values());
            }

            this.outline.setItems(rows);
        }
    }

    private static void section(List<SidePanel.Row> rows, String title, Collection<Analyzer.Symbol> symbols) {
        if (!symbols.isEmpty()) {
            rows.add(new SidePanel.Row(title + " (" + symbols.size() + ")", null));

            for (Analyzer.Symbol s : symbols) {
                rows.add(new SidePanel.Row(null, s));
            }
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -14934490);
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        if (!this.isOutline()) {
            if (this.isInspector()) {
                if (this.inspector == null) {
                    c.wrapped("Здесь настраивается то, что выбрано во вкладке «Мир».", this.x + 8, this.y + 30, this.w - 16, -9933435, 2);
                }
            } else {
                int docY = this.y + this.h - 90;
                c.fill(this.x, docY, this.w, 1, -13749952);
                SidePanel.Cmd cmd = this.selectedCmd;
                if (cmd == null) {
                    c.wrapped("Двойной клик — вставить команду в текущий скрипт.", this.x + 8, docY + 6, this.w - 16, -9933435, 2);
                } else {
                    c.pushClip(this.x, docY + 1, this.w, 89);
                    c.textEllipsized(cmd.title, this.x + 8, docY + 6, this.w - 16, -8213761);
                    c.wrapped(cmd.doc, this.x + 8, docY + 20, this.w - 16, -6577995, 2);
                    c.popClip();
                }
            }
        }
    }

    private record Cmd(String title, String snippet, String doc, String category) {
    }

    private record Row(String header, Analyzer.Symbol symbol) {
    }
}
