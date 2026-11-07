package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class Button extends Widget implements UI.CursorProvider {
    private Supplier<String> text;
    public Icon icon;
    public Button.Style style = Button.Style.DEFAULT;
    public Runnable onClick;
    public int iconColor = 0;
    public BooleanSupplier active;
    public BooleanSupplier enabledWhen;
    private float pressAnim;

    public Button(String text, Runnable onClick) {
        this(() -> text, onClick);
    }

    public Button(Supplier<String> text, Runnable onClick) {
        this.text = text;
        this.onClick = onClick;
        this.prefH = 18;
        this.prefW = -1;
    }

    public static Button icon(Icon icon, String tooltip, Runnable onClick) {
        Button b = new Button("", onClick);
        b.icon = icon;
        b.style = Button.Style.GHOST;
        b.prefW = 18;
        b.tooltip(tooltip);
        return b;
    }

    public Button icon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public Button style(Button.Style s) {
        this.style = s;
        return this;
    }

    public Button iconColor(int c) {
        this.iconColor = c;
        return this;
    }

    public Button toggle(BooleanSupplier active) {
        this.active = active;
        return this;
    }

    public Button enabledWhen(BooleanSupplier b) {
        this.enabledWhen = b;
        return this;
    }

    public Button autoWidth() {
        String s = this.text.get();
        int tw = Minecraft.getInstance().font.width(s);
        this.prefW = tw + (this.icon != null ? (s.isEmpty() ? 18 : 27) : 16);
        return this;
    }

    public String text() {
        return this.text.get();
    }

    private boolean isEnabledNow() {
        return this.enabled && (this.enabledWhen == null || this.enabledWhen.getAsBoolean());
    }

    @Override
    public int cursor(int mx, int my) {
        return this.isEnabledNow() ? 221188 : 221185;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        boolean on = this.active != null && this.active.getAsBoolean();
        boolean en = this.isEnabledNow();
        this.pressAnim = this.pressAnim + (this.isPressed() ? 1 : -1) * dt * 12.0F;
        this.pressAnim = Math.max(0.0F, Math.min(1.0F, this.pressAnim));
        int border = 0;
        int bg;
        int fg;
        switch (this.style) {
            case PRIMARY:
                bg = Theme.lerp(-7639809, -6255873, this.hoverAnim);
                fg = -1;
                break;
            case DANGER:
                bg = Theme.lerp(-7721922, -1023878, this.hoverAnim);
                fg = -1;
                break;
            case SUCCESS:
                bg = Theme.lerp(-13796779, -11746422, this.hoverAnim);
                fg = -1;
                break;
            case GHOST:
                bg = on ? 1435200767 : Theme.alpha(587202559, this.hoverAnim);
                fg = on ? -1841682 : Theme.lerp(-6577995, -1841682, this.hoverAnim);
                break;
            default:
                bg = on ? 1435200767 : Theme.lerp(-14210506, -13618102, this.hoverAnim);
                fg = -1841682;
                border = -12762793;
        }

        if (!en) {
            bg = this.style == Button.Style.GHOST ? 0 : -14210506;
            fg = -9933435;
        }

        int inset = (int)(this.pressAnim * 1.0F);
        if (bg >>> 24 != 0) {
            if (border != 0) {
                c.roundBorder(
                    this.x + inset, this.y + inset, this.w - 2 * inset, this.h - 2 * inset, 3, bg, Theme.lerp(border, -7639809, this.hoverAnim * 0.4F)
                );
            } else {
                c.round(this.x + inset, this.y + inset, this.w - 2 * inset, this.h - 2 * inset, 3, bg);
            }
        }

        if (this.isFocused()) {
            c.outline(this.x, this.y, this.w, this.h, 1435200767);
        }

        String s = this.text.get();
        int tw = s.isEmpty() ? 0 : c.width(s);
        int contentW = tw + (this.icon != null ? (s.isEmpty() ? 9 : 14) : 0);
        int cx = this.x + Math.max(4, (this.w - contentW) / 2);
        int cy = this.y + (this.h - 8) / 2;
        if (this.icon != null) {
            int ic = !en ? -9933435 : (this.iconColor != 0 ? this.iconColor : fg);
            c.icon(this.icon, cx, this.y + (this.h - 9) / 2, ic);
            cx += 14;
        }

        if (!s.isEmpty()) {
            c.text(c.ellipsize(s, this.w - (cx - this.x) - 4), cx, cy, fg);
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        return button == 0 && this.isEnabledNow();
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && this.contains(mx, my) && this.isEnabledNow() && this.onClick != null) {
            this.onClick.run();
        }

        return true;
    }

    public static enum Style {
        DEFAULT,
        PRIMARY,
        GHOST,
        DANGER,
        SUCCESS;
    }
}
