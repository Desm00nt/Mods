package org.sprautestudio.spraute_engine.ui.widgets;

import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Widget;

public class ScrollView extends Widget {
    protected final Widget content;
    protected double scroll;
    protected double targetScroll;
    private boolean draggingBar;
    private double dragOffset;
    public static final int BAR = 6;

    public ScrollView(Widget content) {
        this.content = this.add(content);
    }

    public Widget content() {
        return this.content;
    }

    protected int contentHeight() {
        return Math.max(0, this.content.prefH);
    }

    public int maxScroll() {
        return Math.max(0, this.contentHeight() - this.h);
    }

    public void scrollTo(double value) {
        this.targetScroll = Math.max(0.0, Math.min((double)this.maxScroll(), value));
    }

    public void scrollToInstant(double value) {
        this.targetScroll = this.scroll = Math.max(0.0, Math.min((double)this.maxScroll(), value));
        this.layout();
    }

    public void ensureVisible(int top, int bottom) {
        double rel = top - this.y;
        if (top - (this.y - this.scroll) < this.scroll) {
            this.scrollTo(top - (this.y - this.scroll));
        } else if (bottom - (this.y - this.scroll) > this.scroll + this.h) {
            this.scrollTo(bottom - (this.y - this.scroll) - this.h);
        }
    }

    @Override
    public void layout() {
        int ch = this.contentHeight();
        this.targetScroll = Math.max(0.0, Math.min((double)this.maxScroll(), this.targetScroll));
        this.scroll = Math.max(0.0, Math.min((double)this.maxScroll(), this.scroll));
        this.content.bounds(this.x, this.y - (int)Math.round(this.scroll), this.w - (ch > this.h ? 6 : 0), Math.max(ch, this.h));
        this.content.layout();
    }

    @Override
    public void render(Canvas c, int mx, int my, float dt) {
        if (Math.abs(this.targetScroll - this.scroll) > 0.5) {
            this.scroll = this.scroll + (this.targetScroll - this.scroll) * Math.min(1.0F, dt * 18.0F);
            this.layout();
        } else if (this.scroll != this.targetScroll) {
            this.scroll = this.targetScroll;
            this.layout();
        }

        super.render(c, mx, my, dt);
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        int ch = this.contentHeight();
        if (ch > this.h) {
            int barH = Math.max(16, this.h * this.h / ch);
            int barY = this.y + (int)((this.h - barH) * (this.scroll / Math.max(1, this.maxScroll())));
            boolean hot = this.draggingBar || this.isHoveredDeep() && mx >= this.x + this.w - 6 - 2;
            c.round(this.x + this.w - 6 + 1, barY, 4, barH, 2, hot ? -1996488705 : 1157627903);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.maxScroll() <= 0) {
            return false;
        } else {
            this.scrollTo(this.targetScroll - delta * 36.0);
            return true;
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        int ch = this.contentHeight();
        if (button == 0 && ch > this.h && mx >= this.x + this.w - 6 - 1) {
            int barH = Math.max(16, this.h * this.h / ch);
            int barY = this.y + (int)((this.h - barH) * (this.scroll / Math.max(1, this.maxScroll())));
            this.draggingBar = true;
            this.dragOffset = my >= barY && my <= barY + barH ? my - barY : barH / 2.0;
            this.mouseDragged(mx, my, button, 0.0, 0.0);
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
            int ch = this.contentHeight();
            int barH = Math.max(16, this.h * this.h / ch);
            double t = (my - this.dragOffset - this.y) / Math.max(1, this.h - barH);
            this.scrollToInstant(t * this.maxScroll());
            return true;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.draggingBar = false;
        return true;
    }

    protected static int background() {
        return -14934490;
    }
}
