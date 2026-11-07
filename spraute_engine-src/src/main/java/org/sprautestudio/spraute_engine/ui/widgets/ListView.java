package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class ListView<T> extends Widget implements UI.CursorProvider {
    private List<T> items = new ArrayList<>();
    public int rowHeight = 18;
    public ListView.RowPainter<T> painter;
    public Consumer<T> onSelect;
    public Consumer<T> onActivate;
    public ListView.ContextHandler<T> onContext;
    public String emptyText = "";
    public boolean activateOnSingleClick;
    public int background = 0;
    private int selected = -1;
    private double scroll;
    private double targetScroll;
    private boolean draggingBar;

    public ListView(ListView.RowPainter<T> painter) {
        this.painter = painter;
        this.focusable = true;
    }

    public void setItems(List<T> items) {
        T sel = this.selectedItem();
        this.items = new ArrayList<>(items);
        this.selected = sel != null ? this.items.indexOf(sel) : -1;
        this.clampScroll();
    }

    public List<T> items() {
        return this.items;
    }

    public T selectedItem() {
        return this.selected >= 0 && this.selected < this.items.size() ? this.items.get(this.selected) : null;
    }

    public int selectedIndex() {
        return this.selected;
    }

    public void select(int index) {
        if (this.items.isEmpty()) {
            this.selected = -1;
        } else {
            this.selected = Math.max(0, Math.min(this.items.size() - 1, index));
            this.ensureVisible(this.selected);
            if (this.onSelect != null) {
                this.onSelect.accept(this.items.get(this.selected));
            }
        }
    }

    public void clearSelection() {
        this.selected = -1;
    }

    public void ensureVisible(int index) {
        int top = index * this.rowHeight;
        if (top < this.targetScroll) {
            this.targetScroll = top;
        } else if (top + this.rowHeight > this.targetScroll + this.h) {
            this.targetScroll = top + this.rowHeight - this.h;
        }

        this.clampScroll();
    }

    private int maxScroll() {
        return Math.max(0, this.items.size() * this.rowHeight - this.h);
    }

    private void clampScroll() {
        this.targetScroll = Math.max(0.0, Math.min((double)this.maxScroll(), this.targetScroll));
        this.scroll = Math.max(0.0, Math.min((double)this.maxScroll(), this.scroll));
    }

    private int rowAt(double my) {
        int idx = (int)Math.floor((my - this.y + this.scroll) / this.rowHeight);
        return idx >= 0 && idx < this.items.size() ? idx : -1;
    }

    @Override
    public int cursor(int mx, int my) {
        return this.rowAt(my) >= 0 ? 221188 : 221185;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (Math.abs(this.targetScroll - this.scroll) > 0.5) {
            this.scroll = this.scroll + (this.targetScroll - this.scroll) * Math.min(1.0F, dt * 18.0F);
        } else {
            this.scroll = this.targetScroll;
        }

        if (this.background != 0) {
            c.fill(this.x, this.y, this.w, this.h, this.background);
        }

        c.pushClip(this.x, this.y, this.w, this.h);
        if (this.items.isEmpty() && !this.emptyText.isEmpty()) {
            c.wrapped(this.emptyText, this.x + 8, this.y + 8, this.w - 16, -9933435, 2);
        }

        int first = Math.max(0, (int)(this.scroll / this.rowHeight));
        int last = Math.min(this.items.size() - 1, (int)((this.scroll + this.h) / this.rowHeight));
        int hoverRow = this.isHovered() ? this.rowAt(my) : -1;
        boolean bar = this.maxScroll() > 0;
        int rw = this.w - (bar ? 6 : 0);

        for (int i = first; i <= last; i++) {
            int ry = this.y + i * this.rowHeight - (int)Math.round(this.scroll);
            boolean sel = i == this.selected;
            if (sel) {
                c.fill(this.x, ry, rw, this.rowHeight, this.hasFocusWithin() ? 1430088664 : 859663320);
            } else if (i == hoverRow) {
                c.fill(this.x, ry, rw, this.rowHeight, 352321535);
            }

            this.painter.paint(c, this.items.get(i), this.x, ry, rw, this.rowHeight, sel, i == hoverRow);
        }

        c.popClip();
        if (bar) {
            int total = this.items.size() * this.rowHeight;
            int barH = Math.max(16, this.h * this.h / total);
            int barY = this.y + (int)((this.h - barH) * (this.scroll / Math.max(1, this.maxScroll())));
            c.round(this.x + this.w - 6 + 1, barY, 4, barH, 2, this.draggingBar ? -1996488705 : 1157627903);
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (button == 0 && this.maxScroll() > 0 && mx >= this.x + this.w - 6 - 1) {
            this.draggingBar = true;
            this.mouseDragged(mx, my, button, 0.0, 0.0);
            return true;
        } else {
            int row = this.rowAt(my);
            if (row >= 0) {
                this.selected = row;
                if (this.onSelect != null) {
                    this.onSelect.accept(this.items.get(row));
                }

                if (button == 1 && this.onContext != null) {
                    this.onContext.open(this.items.get(row), (int)mx, (int)my);
                } else if (button == 0 && this.activateOnSingleClick && this.onActivate != null) {
                    this.onActivate.accept(this.items.get(row));
                }

                return true;
            } else if (button == 1 && this.onContext != null) {
                this.onContext.open(null, (int)mx, (int)my);
                return true;
            } else {
                return true;
            }
        }
    }

    @Override
    public boolean mouseDoubleClicked(double mx, double my, int button) {
        int row = this.rowAt(my);
        if (button == 0 && row >= 0 && this.onActivate != null && !this.activateOnSingleClick) {
            this.selected = row;
            this.onActivate.accept(this.items.get(row));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!this.draggingBar) {
            return false;
        } else {
            int total = this.items.size() * this.rowHeight;
            int barH = Math.max(16, this.h * this.h / Math.max(1, total));
            double t = (my - this.y - barH / 2.0) / Math.max(1, this.h - barH);
            this.targetScroll = this.scroll = Math.max(0.0, Math.min((double)this.maxScroll(), t * this.maxScroll()));
            return true;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.draggingBar = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.maxScroll() <= 0) {
            return false;
        } else {
            this.targetScroll = this.targetScroll - delta * this.rowHeight * 3.0;
            this.clampScroll();
            return true;
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        switch (key) {
            case 257:
            case 335:
                if (this.selectedItem() != null && this.onActivate != null) {
                    this.onActivate.accept(this.selectedItem());
                }
                break;
            case 264:
                this.select(this.selected + 1);
                break;
            case 265:
                this.select(this.selected <= 0 ? 0 : this.selected - 1);
                break;
            case 266:
                this.select(this.selected - Math.max(1, this.h / this.rowHeight));
                break;
            case 267:
                this.select(this.selected + Math.max(1, this.h / this.rowHeight));
                break;
            case 268:
                this.select(0);
                break;
            case 269:
                this.select(this.items.size() - 1);
                break;
            default:
                return false;
        }

        return true;
    }

    public interface ContextHandler<T> {
        void open(T var1, int var2, int var3);
    }

    public interface RowPainter<T> {
        void paint(Canvas var1, T var2, int var3, int var4, int var5, int var6, boolean var7, boolean var8);
    }
}
