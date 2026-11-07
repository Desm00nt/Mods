package org.sprautestudio.spraute_engine.editor.panels;

import java.util.List;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;

public class ProblemsPanel extends ListView<EditorTab.Problem> {
    private final Supplier<List<EditorTab.Problem>> source;
    private long lastRefresh;

    public ProblemsPanel(Supplier<List<EditorTab.Problem>> source) {
        super(null);
        this.source = source;
        this.rowHeight = 16;
        this.background = -15461093;
        this.emptyText = "Проблем не найдено — сюжет готов к запуску.";
        this.painter = (c, p, x, y, w, h, sel, hov) -> {
            Icon icon = p.severity() == LanguageSupport.Severity.ERROR
                ? Icon.ERROR
                : (p.severity() == LanguageSupport.Severity.WARNING ? Icon.WARNING : Icon.INFO);
            int color = p.severity() == LanguageSupport.Severity.ERROR ? -1023878 : (p.severity() == LanguageSupport.Severity.WARNING ? -870325 : -10638858);
            c.icon(icon, x + 8, y + 3, color);
            int whereW = c.width(p.where()) + 10;
            c.textEllipsized(p.message(), x + 22, y + 4, w - 30 - whereW, -1841682);
            c.text(p.where(), x + w - whereW, y + 4, -9933435);
        };
        this.activateOnSingleClick = true;
        this.onActivate = p -> {
            if (p.reveal() != null) {
                p.reveal().run();
            }
        };
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - this.lastRefresh > 400L && this.isShown()) {
            this.lastRefresh = now;
            this.setItems(this.source.get());
        }
    }
}
