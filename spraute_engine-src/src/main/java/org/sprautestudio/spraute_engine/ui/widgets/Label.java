package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Widget;

public class Label extends Widget {
    private Supplier<String> text;
    public int color = -1841682;
    public Label.Align align = Label.Align.LEFT;
    public Icon icon;
    public int iconColor = -6577995;
    public boolean wrap;
    public int scale = 1;

    public Label(String text) {
        this(() -> text);
    }

    public Label(Supplier<String> text) {
        this.text = text;
        this.prefH = 12;
    }

    public Label color(int c) {
        this.color = c;
        return this;
    }

    public Label align(Label.Align a) {
        this.align = a;
        return this;
    }

    public Label icon(Icon icon, int color) {
        this.icon = icon;
        this.iconColor = color;
        return this;
    }

    public Label wrap() {
        this.wrap = true;
        return this;
    }

    public Label big() {
        this.scale = 2;
        this.prefH = 22;
        return this;
    }

    public void setText(String s) {
        this.text = () -> s;
    }

    public String text() {
        String s = this.text.get();
        return s == null ? "" : s;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        String s = this.text();
        if (this.wrap) {
            c.wrapped(s, this.x, this.y + 1, this.w, this.color, 2);
        } else {
            int ix = this.x;
            int avail = this.w - (this.icon != null ? 13 : 0);
            if (this.scale != 1) {
                int tw = c.width(s) * this.scale;
                int tx = this.align == Label.Align.CENTER ? this.x + (this.w - tw) / 2 : (this.align == Label.Align.RIGHT ? this.x + this.w - tw : this.x);
                c.pushTransform(tx, this.y + (this.h - 9 * this.scale) / 2.0F, this.scale);
                c.text(s, 0, 0, this.color);
                c.popTransform();
            } else {
                String shown = c.ellipsize(s, avail);
                int tw = c.width(shown) + (this.icon != null ? 13 : 0);
                if (this.align == Label.Align.CENTER) {
                    ix = this.x + (this.w - tw) / 2;
                } else if (this.align == Label.Align.RIGHT) {
                    ix = this.x + this.w - tw;
                }

                int ty = this.y + (this.h - 8) / 2;
                int textX;
                if (this.icon != null) {
                    c.icon(this.icon, ix, ty, this.iconColor);
                    textX = ix + 13;
                } else {
                    textX = ix;
                }

                c.text(shown, textX, ty, this.color);
            }
        }
    }

    public static enum Align {
        LEFT,
        CENTER,
        RIGHT;
    }
}
