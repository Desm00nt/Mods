package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.IntConsumer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class ColorPicker extends Widget implements UI.OverlayLayout, UI.CursorProvider {
    private static final int[] PALETTE = new int[]{
        -1,
        -1841682,
        -6577995,
        -9933435,
        -12762793,
        -14210506,
        -14934490,
        -15724266,
        -16777216,
        -1023878,
        -553876,
        -870325,
        -13461,
        -11746422,
        -12988235,
        -10638858,
        -7639809,
        -1737768,
        0,
        1711276032,
        -872415232,
        -535028186,
        -1996488705,
        -5636096,
        -16733696,
        -16777046,
        -22016
    };
    private static final int W = 180;
    private static final int SQ = 120;
    private static final int BAR = 10;
    private float hue;
    private float sat;
    private float val;
    private float alpha;
    private final IntConsumer onChange;
    private final TextField hex = new TextField();
    private int ax;
    private int ay;
    private int dragging;

    private ColorPicker(int argb, IntConsumer onChange, int ax, int ay) {
        this.onChange = onChange;
        this.ax = ax;
        this.ay = ay;
        this.setFrom(argb);
        this.add(this.hex);
        this.hex.setText(String.format("#%08X", argb));
        this.hex.onSubmit = s -> this.parseHex();
        this.hex.onChange = s -> this.parseHex();
        this.focusable = true;
    }

    public static ColorPicker open(int argb, int x, int y, IntConsumer onChange) {
        ColorPicker p = new ColorPicker(argb, onChange, x, y);
        UI ui = UI.get();
        if (ui != null) {
            ui.openOverlay(p, false, true, null);
        }

        return p;
    }

    private void setFrom(int argb) {
        this.alpha = (argb >>> 24 & 0xFF) / 255.0F;
        float r = (argb >> 16 & 0xFF) / 255.0F;
        float g = (argb >> 8 & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        this.val = max;
        this.sat = max == 0.0F ? 0.0F : (max - min) / max;
        float d = max - min;
        if (d != 0.0F) {
            float h;
            if (max == r) {
                h = (g - b) / d % 6.0F;
            } else if (max == g) {
                h = (b - r) / d + 2.0F;
            } else {
                h = (r - g) / d + 4.0F;
            }

            this.hue = (h * 60.0F + 360.0F) % 360.0F;
        }
    }

    private static int hsv(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0F - Math.abs(h / 60.0F % 2.0F - 1.0F));
        float m = v - c;
        float r;
        float g;
        float b;
        if (h < 60.0F) {
            r = c;
            g = x;
            b = 0.0F;
        } else if (h < 120.0F) {
            r = x;
            g = c;
            b = 0.0F;
        } else if (h < 180.0F) {
            r = 0.0F;
            g = c;
            b = x;
        } else if (h < 240.0F) {
            r = 0.0F;
            g = x;
            b = c;
        } else if (h < 300.0F) {
            r = x;
            g = 0.0F;
            b = c;
        } else {
            r = c;
            g = 0.0F;
            b = x;
        }

        return 0xFF000000 | Math.round((r + m) * 255.0F) << 16 | Math.round((g + m) * 255.0F) << 8 | Math.round((b + m) * 255.0F);
    }

    private int value() {
        return Math.round(this.alpha * 255.0F) << 24 | hsv(this.hue, this.sat, this.val) & 16777215;
    }

    private void changed(boolean updateHex) {
        int v = this.value();
        if (updateHex) {
            this.hex.setText(String.format("#%08X", v));
        }

        this.onChange.accept(v);
    }

    private void parseHex() {
        String s = this.hex.text().trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }

        try {
            int v;
            if (s.length() == 6) {
                v = 0xFF000000 | Integer.parseInt(s, 16);
            } else {
                if (s.length() != 8) {
                    return;
                }

                v = (int)Long.parseLong(s, 16);
            }

            this.setFrom(v);
            this.onChange.accept(v);
        } catch (NumberFormatException var3) {
        }
    }

    @Override
    public void layoutOverlay(int vw, int vh) {
        int h = 238;
        int px = Math.min(this.ax, vw - 180 - 4);
        int py = this.ay + h > vh - 4 ? Math.max(4, this.ay - h - 24) : this.ay;
        this.bounds(px, py, 180, h);
        this.hex.bounds(this.x + 8, this.y + h - 26, 164, 18);
    }

    @Override
    public void layout() {
        UI ui = UI.get();
        if (ui != null) {
            this.layoutOverlay(ui.vw, ui.vh);
        }
    }

    private int sqX() {
        return this.x + 8;
    }

    private int sqY() {
        return this.y + 8;
    }

    private int sqW() {
        return 164;
    }

    private int hueY() {
        return this.sqY() + 120 + 6;
    }

    private int alphaY() {
        return this.hueY() + 10 + 4;
    }

    private int palY() {
        return this.alphaY() + 10 + 6;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.shadow(this.x, this.y, this.w, this.h, 5, 8);
        c.roundBorder(this.x, this.y, this.w, this.h, 5, -14934490, -12762793);
        int sx = this.sqX();
        int sy = this.sqY();
        int sw = this.sqW();
        int steps = 48;

        for (int i = 0; i < steps; i++) {
            int x0 = sx + sw * i / steps;
            int x1 = sx + sw * (i + 1) / steps;
            c.fill(x0, sy, x1 - x0, 120, hsv(this.hue, (float)i / (steps - 1), 1.0F));
        }

        c.gradient(sx, sy, sw, 120, 0, -16777216);
        int kx = sx + Math.round(this.sat * sw);
        int ky = sy + Math.round((1.0F - this.val) * 120.0F);
        c.outline(kx - 3, ky - 3, 7, 7, -1);
        c.outline(kx - 2, ky - 2, 5, 5, -16777216);

        for (int i = 0; i < steps; i++) {
            int x0 = sx + sw * i / steps;
            int x1 = sx + sw * (i + 1) / steps;
            c.fill(x0, this.hueY(), x1 - x0, 10, hsv(360.0F * i / steps, 1.0F, 1.0F));
        }

        int hx = sx + Math.round(this.hue / 360.0F * sw);
        c.fill(hx - 1, this.hueY() - 1, 3, 12, -1);

        for (int i = 0; i < sw; i += 5) {
            c.fill(sx + i, this.alphaY(), Math.min(5, sw - i), 10, i / 5 % 2 == 0 ? -7829368 : -4473925);
        }

        int rgb = hsv(this.hue, this.sat, this.val) & 16777215;

        for (int i = 0; i < steps; i++) {
            int x0 = sx + sw * i / steps;
            int x1 = sx + sw * (i + 1) / steps;
            c.fill(x0, this.alphaY(), x1 - x0, 10, Math.round(255.0F * i / (steps - 1)) << 24 | rgb);
        }

        int alx = sx + Math.round(this.alpha * sw);
        c.fill(alx - 1, this.alphaY() - 1, 3, 12, -1);
        int cols = 9;
        int cell = (sw - (cols - 1) * 2) / cols;

        for (int i = 0; i < PALETTE.length; i++) {
            int cx = sx + i % cols * (cell + 2);
            int cy = this.palY() + i / cols * 14;
            if (PALETTE[i] >>> 24 < 255) {
                c.fill(cx, cy, cell, 12, -7829368);
                c.fill(cx, cy, cell / 2, 6, -4473925);
                c.fill(cx + cell / 2, cy + 6, cell - cell / 2, 6, -4473925);
            }

            c.fill(cx, cy, cell, 12, PALETTE[i]);
            c.outline(cx, cy, cell, 12, 872415231);
        }
    }

    private boolean pick(double mx, double my, int mode) {
        int sx = this.sqX();
        int sw = this.sqW();
        float fx = (float)Math.max(0.0, Math.min(1.0, (mx - sx) / sw));
        switch (mode) {
            case 1:
                this.sat = fx;
                this.val = (float)Math.max(0.0, Math.min(1.0, 1.0 - (my - this.sqY()) / 120.0));
                break;
            case 2:
                this.hue = Math.min(359.9F, fx * 360.0F);
                break;
            case 3:
                this.alpha = fx;
                break;
            default:
                return false;
        }

        if (this.alpha == 0.0F && mode != 3) {
            this.alpha = 1.0F;
        }

        this.changed(true);
        return true;
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        int sx = this.sqX();
        int sw = this.sqW();
        if (mx >= sx && mx < sx + sw) {
            if (my >= this.sqY() && my < this.sqY() + 120) {
                this.dragging = 1;
            } else if (my >= this.hueY() && my < this.hueY() + 10) {
                this.dragging = 2;
            } else if (my >= this.alphaY() && my < this.alphaY() + 10) {
                this.dragging = 3;
            } else {
                this.dragging = 0;
            }

            if (this.dragging != 0) {
                return this.pick(mx, my, this.dragging);
            }

            int cols = 9;
            int cell = (sw - (cols - 1) * 2) / cols;

            for (int i = 0; i < PALETTE.length; i++) {
                int cx = sx + i % cols * (cell + 2);
                int cy = this.palY() + i / cols * 14;
                if (mx >= cx && mx < cx + cell && my >= cy && my < cy + 12) {
                    this.setFrom(PALETTE[i]);
                    if (PALETTE[i] >>> 24 == 0) {
                        this.alpha = 0.0F;
                    }

                    this.hex.setText(String.format("#%08X", PALETTE[i]));
                    this.onChange.accept(PALETTE[i]);
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return this.dragging != 0 && this.pick(mx, my, this.dragging);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.dragging = 0;
        return true;
    }

    @Override
    public int cursor(int mx, int my) {
        return my < this.palY() ? 221187 : 221188;
    }
}
