package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.IntConsumer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class SplitPane extends Widget implements UI.CursorProvider {
    public final boolean horizontal;
    public final boolean fixedFirst;
    public final Widget first;
    public final Widget second;
    public int fixedSize;
    public int minSize = 60;
    public IntConsumer onResize;
    private boolean dragging;
    private static final int GRIP = 4;

    public SplitPane(boolean horizontal, Widget first, Widget second, boolean fixedFirst, int fixedSize) {
        this.horizontal = horizontal;
        this.first = this.add(first);
        this.second = this.add(second);
        this.fixedFirst = fixedFirst;
        this.fixedSize = fixedSize;
        this.clipChildren = false;
    }

    private Widget fixedPane() {
        return this.fixedFirst ? this.first : this.second;
    }

    @Override
    public void layout() {
        int total = this.horizontal ? this.w : this.h;
        boolean showFirst = this.first.visible;
        boolean showSecond = this.second.visible;
        if (showFirst && showSecond) {
            int fixed = Math.max(this.minSize, Math.min(total - this.minSize, this.fixedSize));
            int a = this.fixedFirst ? fixed : total - fixed - 1;
            if (this.horizontal) {
                this.first.bounds(this.x, this.y, a, this.h);
                this.second.bounds(this.x + a + 1, this.y, this.w - a - 1, this.h);
            } else {
                this.first.bounds(this.x, this.y, this.w, a);
                this.second.bounds(this.x, this.y + a + 1, this.w, this.h - a - 1);
            }

            this.first.layout();
            this.second.layout();
        } else {
            Widget only = showFirst ? this.first : this.second;
            only.bounds(this.x, this.y, this.w, this.h);
            only.layout();
        }
    }

    private int dividerPos() {
        return this.horizontal ? this.first.x + this.first.w : this.first.y + this.first.h;
    }

    private boolean onGrip(double mx, double my) {
        if (this.first.visible && this.second.visible) {
            int d = this.dividerPos();
            return this.horizontal
                ? Math.abs(mx - d) <= 4.0 && my >= this.y && my < this.y + this.h
                : Math.abs(my - d) <= 4.0 && mx >= this.x && mx < this.x + this.w;
        } else {
            return false;
        }
    }

    @Override
    public Widget hitTest(double mx, double my) {
        if (!this.visible || !this.contains(mx, my)) {
            return null;
        } else {
            return (Widget)(this.onGrip(mx, my) ? this : super.hitTest(mx, my));
        }
    }

    @Override
    public int cursor(int mx, int my) {
        return this.horizontal ? 221189 : 221190;
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        if (this.first.visible && this.second.visible) {
            int d = this.dividerPos();
            boolean hot = this.dragging || this.isHovered() && this.onGrip(mx, my);
            int color = hot ? -7639809 : -13749952;
            if (this.horizontal) {
                c.fill(d, this.y, hot ? 2 : 1, this.h, color);
            } else {
                c.fill(this.x, d, this.w, hot ? 2 : 1, color);
            }
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (button == 0 && this.onGrip(mx, my)) {
            this.dragging = true;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!this.dragging) {
            return false;
        } else {
            int total = this.horizontal ? this.w : this.h;
            int pos = (int)(this.horizontal ? mx - this.x : my - this.y);
            int size = this.fixedFirst ? pos : total - pos;
            this.fixedSize = Math.max(this.minSize, Math.min(total - this.minSize, size));
            this.layout();
            if (this.onResize != null) {
                this.onResize.accept(this.fixedSize);
            }

            return true;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.dragging = false;
        return true;
    }

    public void toggleFixed() {
        this.fixedPane().visible = !this.fixedPane().visible;
        this.layout();
    }
}
