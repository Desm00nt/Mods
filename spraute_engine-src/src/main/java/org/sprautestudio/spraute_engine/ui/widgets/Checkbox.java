package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class Checkbox extends Widget implements UI.CursorProvider {
    private final String text;
    private final BooleanSupplier value;
    private final Consumer<Boolean> onChange;
    private float anim;

    public Checkbox(String text, BooleanSupplier value, Consumer<Boolean> onChange) {
        this.text = text;
        this.value = value;
        this.onChange = onChange;
        this.prefH = 16;
    }

    @Override
    public int cursor(int mx, int my) {
        return 221188;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        boolean on = this.value.getAsBoolean();
        this.anim += (on ? 1 : -1) * dt * 10.0F;
        this.anim = Math.max(0.0F, Math.min(1.0F, this.anim));
        int sw = 22;
        int sh = 12;
        int sy = this.y + (this.h - sh) / 2;
        c.round(this.x, sy, sw, sh, 6, Theme.lerp(-12762793, -7639809, this.anim));
        int knob = (int)(this.x + 2 + this.anim * (sw - sh));
        c.round(knob, sy + 2, sh - 4, sh - 4, 4, -1);
        c.text(this.text, this.x + sw + 6, this.y + (this.h - 8) / 2, Theme.lerp(-6577995, -1841682, this.hoverAnim));
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (button != 0) {
            return false;
        } else {
            this.onChange.accept(!this.value.getAsBoolean());
            return true;
        }
    }

    public static void badge(Canvas c, String text, int x, int y, int color) {
        int w = c.width(text) + 8;
        c.round(x, y, w, 11, 4, Theme.alpha(color, 0.22F));
        c.text(text, x + 4, y + 2, color);
    }

    public static int badgeWidth(Canvas c, String text) {
        return c.width(text) + 8;
    }

    public static void iconBadge(Canvas c, Icon icon, int x, int y, int color) {
        c.round(x, y, 13, 13, 3, Theme.alpha(color, 0.22F));
        c.icon(icon, x + 2, y + 2, color);
    }
}
