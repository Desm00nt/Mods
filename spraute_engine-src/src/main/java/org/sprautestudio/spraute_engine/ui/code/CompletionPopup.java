package org.sprautestudio.spraute_engine.ui.code;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class CompletionPopup extends Widget implements UI.OverlayLayout, UI.CursorProvider {
    private List<LanguageSupport.Completion> items = new ArrayList<>();
    private int selected;
    private int scroll;
    private int anchorX;
    private int anchorY;
    private int lineH;
    private static final int ROW = 14;
    private static final int MAX_ROWS = 10;
    private static final int LIST_W = 210;
    private static final int DOC_W = 200;
    private final Consumer<LanguageSupport.Completion> onAccept;

    public CompletionPopup(Consumer<LanguageSupport.Completion> onAccept) {
        this.onAccept = onAccept;
    }

    public void setItems(List<LanguageSupport.Completion> items) {
        this.items = items;
        this.selected = 0;
        this.scroll = 0;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public void anchor(int x, int y, int lineH) {
        this.anchorX = x;
        this.anchorY = y;
        this.lineH = lineH;
        UI ui = UI.get();
        if (ui != null) {
            this.layoutOverlay(ui.vw, ui.vh);
        }
    }

    public LanguageSupport.Completion selected() {
        return this.selected >= 0 && this.selected < this.items.size() ? this.items.get(this.selected) : null;
    }

    public void move(int delta) {
        if (!this.items.isEmpty()) {
            this.selected = Math.floorMod(this.selected + delta, this.items.size());
            if (this.selected < this.scroll) {
                this.scroll = this.selected;
            }

            if (this.selected >= this.scroll + 10) {
                this.scroll = this.selected - 10 + 1;
            }
        }
    }

    private int listH() {
        return Math.min(this.items.size(), 10) * 14 + 4;
    }

    @Override
    public void layoutOverlay(int vw, int vh) {
        int hgt = this.listH();
        int py = this.anchorY + this.lineH + 2;
        if (py + hgt > vh - 4) {
            py = this.anchorY - hgt - 2;
        }

        int totalW = 410;
        int px = Math.min(this.anchorX, vw - totalW - 4);
        this.bounds(Math.max(2, px), py, totalW, Math.max(hgt, 60));
    }

    @Override
    public Widget hitTest(double mx, double my) {
        if (!this.visible) {
            return null;
        } else {
            return mx >= this.x && mx < this.x + 210 && my >= this.y && my < this.y + this.listH() ? this : null;
        }
    }

    @Override
    public int cursor(int mx, int my) {
        return 221188;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (!this.items.isEmpty()) {
            int lh = this.listH();
            c.shadow(this.x, this.y, 210, lh, 4, 6);
            c.roundBorder(this.x, this.y, 210, lh, 4, -14210506, -12762793);
            int end = Math.min(this.items.size(), this.scroll + 10);

            for (int i = this.scroll; i < end; i++) {
                LanguageSupport.Completion it = this.items.get(i);
                int ry = this.y + 2 + (i - this.scroll) * 14;
                if (i == this.selected) {
                    c.round(this.x + 2, ry, 206, 14, 3, 1435200767);
                }

                int var10002 = this.x + 6;
                c.icon(it.kind().icon, var10002, ry + 2, it.kind().color);
                int detailW = it.detail() != null ? Math.min(80, c.width(it.detail())) : 0;
                c.textEllipsized(it.label(), this.x + 19, ry + 3, 182 - detailW, -1841682);
                if (it.detail() != null) {
                    c.textEllipsized(it.detail(), this.x + 210 - 6 - detailW, ry + 3, detailW, -9933435);
                }
            }

            if (this.items.size() > 10) {
                int barH = Math.max(10, lh * 10 / this.items.size());
                int barY = this.y + (lh - barH) * this.scroll / Math.max(1, this.items.size() - 10);
                c.fill(this.x + 210 - 3, barY, 2, barH, 1442840575);
            }

            LanguageSupport.Completion sel = this.selected();
            if (sel != null && sel.doc() != null && !sel.doc().isEmpty()) {
                int dx = this.x + 210 + 4;
                String title = sel.label();
                int bodyH = c.wrappedHeight(sel.doc(), 184, 2);
                int dh = Math.min(220, bodyH + 26);
                c.shadow(dx, this.y, 200, dh, 4, 6);
                c.roundBorder(dx, this.y, 200, dh, 4, -14934490, -12762793);
                c.textEllipsized(title, dx + 8, this.y + 6, 184, sel.kind().color);
                c.fill(dx + 8, this.y + 17, 184, 1, -13749952);
                c.pushClip(dx, this.y + 19, 200, dh - 20);
                c.wrapped(sel.doc(), dx + 8, this.y + 22, 184, -6577995, 2);
                c.popClip();
            }
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        int idx = this.scroll + (int)((my - this.y - 2.0) / 14.0);
        if (idx >= 0 && idx < this.items.size()) {
            if (idx == this.selected) {
                this.onAccept.accept(this.items.get(idx));
            } else {
                this.selected = idx;
            }
        }

        return true;
    }

    @Override
    public boolean mouseDoubleClicked(double mx, double my, int button) {
        LanguageSupport.Completion s = this.selected();
        if (s != null) {
            this.onAccept.accept(s);
        }

        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        this.scroll = Math.max(0, Math.min(Math.max(0, this.items.size() - 10), this.scroll - (int)Math.signum(delta) * 2));
        return true;
    }
}
