package org.sprautestudio.spraute_engine.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;

public final class Canvas {
    public final Gfx g;
    public final Font font;
    private final float baseScale;
    private float tx;
    private float ty;
    private float zoom = 1.0F;
    private final Deque<float[]> transforms = new ArrayDeque<>();
    private final Deque<int[]> clips = new ArrayDeque<>();

    public Canvas(Gfx g, Font font, float baseScale) {
        this.g = g;
        this.font = font;
        this.baseScale = baseScale;
    }

    public void fill(int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0 && color >>> 24 != 0) {
            this.g.fill(x, y, x + w, y + h, color);
        }
    }

    public void fillF(float x, float y, float w, float h, int color) {
        if (!(w <= 0.0F) && !(h <= 0.0F) && color >>> 24 != 0) {
            this.g.pose().pushPose();
            this.g.pose().translate(x, y, 0.0F);
            this.g.pose().scale(w, h, 1.0F);
            this.g.fill(0, 0, 1, 1, color);
            this.g.pose().popPose();
        }
    }

    public void outline(int x, int y, int w, int h, int color) {
        this.fill(x, y, w, 1, color);
        this.fill(x, y + h - 1, w, 1, color);
        this.fill(x, y + 1, 1, h - 2, color);
        this.fill(x + w - 1, y + 1, 1, h - 2, color);
    }

    public void gradient(int x, int y, int w, int h, int top, int bottom) {
        if (w > 0 && h > 0) {
            this.g.fillGradient(x, y, x + w, y + h, top, bottom);
        }
    }

    public void round(int x, int y, int w, int h, int r, int color) {
        if (w > 0 && h > 0) {
            r = Math.min(r, Math.min(w, h) / 2);
            if (r <= 0) {
                this.fill(x, y, w, h, color);
            } else {
                this.fill(x, y + r, w, h - 2 * r, color);

                for (int i = 0; i < r; i++) {
                    double dy = r - i - 0.5;
                    int inset = (int)Math.round(r - Math.sqrt(r * r - dy * dy));
                    this.fill(x + inset, y + i, w - 2 * inset, 1, color);
                    this.fill(x + inset, y + h - 1 - i, w - 2 * inset, 1, color);
                }
            }
        }
    }

    public void roundBorder(int x, int y, int w, int h, int r, int fill, int border) {
        this.round(x, y, w, h, r, border);
        this.round(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    public void shadow(int x, int y, int w, int h, int r, int size) {
        for (int i = size; i > 0; i--) {
            int a = (int)(40.0 * (1.0 - (double)i / (size + 1)) * (1.0 / size) * 2.2);
            this.round(x - i, y - i + 2, w + 2 * i, h + 2 * i, r + i, a << 24);
        }
    }

    public void line(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (!(len < 0.01F)) {
            this.g.pose().pushPose();
            this.g.pose().translate(x1, y1, 0.0F);
            Mc.mulPose(this.g.pose(), Axes.ZP.rotation((float)Math.atan2(dy, dx)));
            this.g.pose().scale(len, thickness, 1.0F);
            this.g.pose().translate(0.0F, -0.5F, 0.0F);
            this.g.fill(0, 0, 1, 1, color);
            this.g.pose().popPose();
        }
    }

    public void bezier(float x1, float y1, float x2, float y2, float thickness, int color) {
        float d = Math.max(30.0F, Math.abs(x2 - x1) * 0.5F);
        float cx1 = x1 + d;
        float cx2 = x2 - d;
        int steps = Math.max(12, (int)(Math.abs(x2 - x1) + Math.abs(y2 - y1)) / 6);
        float px = x1;
        float py = y1;

        for (int i = 1; i <= steps; i++) {
            float t = (float)i / steps;
            float u = 1.0F - t;
            float nx = u * u * u * x1 + 3.0F * u * u * t * cx1 + 3.0F * u * t * t * cx2 + t * t * t * x2;
            float ny = u * u * u * y1 + 3.0F * u * u * t * y1 + 3.0F * u * t * t * y2 + t * t * t * y2;
            this.line(px, py, nx, ny, thickness, color);
            px = nx;
            py = ny;
        }
    }

    public void squiggle(int x, int y, int w, int color) {
        for (int i = 0; i < w; i += 2) {
            int off = i / 2 % 2 == 0 ? 0 : 1;
            this.fill(x + i, y + off, Math.min(2, w - i), 1, color);
        }
    }

    public void icon(Icon icon, int x, int y, int color) {
        icon.draw(this, x, y, color);
    }

    public int text(String s, int x, int y, int color) {
        return this.g.drawString(this.font, s, x, y, color, false);
    }

    public int text(Component s, int x, int y, int color) {
        return this.g.drawString(this.font, s, x, y, color, false);
    }

    public int textShadow(String s, int x, int y, int color) {
        return this.g.drawString(this.font, s, x, y, color, true);
    }

    public void textCentered(String s, int cx, int y, int color) {
        this.text(s, cx - this.font.width(s) / 2, y, color);
    }

    public void textEllipsized(String s, int x, int y, int maxWidth, int color) {
        this.text(this.ellipsize(s, maxWidth), x, y, color);
    }

    public String ellipsize(String s, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        } else if (this.font.width(s) <= maxWidth) {
            return s;
        } else {
            String dots = "…";
            int dw = this.font.width(dots);
            return this.font.plainSubstrByWidth(s, Math.max(0, maxWidth - dw)) + dots;
        }
    }

    public int width(String s) {
        return this.font.width(s);
    }

    public int lineHeight() {
        return 9;
    }

    public int wrapped(String s, int x, int y, int width, int color, int lineSpacing) {
        int yy = y;

        for (String paragraph : s.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                yy += 9 + lineSpacing;
            } else {
                for (FormattedCharSequence line : this.font.split(Component.literal(paragraph), Math.max(10, width))) {
                    this.g.drawString(this.font, line, x, yy, color, false);
                    yy += 9 + lineSpacing;
                }
            }
        }

        return yy - y;
    }

    public int wrappedHeight(String s, int width, int lineSpacing) {
        int h = 0;

        for (String paragraph : s.split("\n", -1)) {
            int lines = paragraph.isEmpty() ? 1 : this.font.split(Component.literal(paragraph), Math.max(10, width)).size();
            h += lines * (9 + lineSpacing);
        }

        return h;
    }

    public void pushTransform(float translateX, float translateY, float scale) {
        this.transforms.push(new float[]{this.tx, this.ty, this.zoom});
        this.g.pose().pushPose();
        this.g.pose().translate(translateX, translateY, 0.0F);
        this.g.pose().scale(scale, scale, 1.0F);
        this.tx = this.tx + translateX * this.zoom;
        this.ty = this.ty + translateY * this.zoom;
        this.zoom *= scale;
    }

    public void popTransform() {
        float[] t = this.transforms.pop();
        this.tx = t[0];
        this.ty = t[1];
        this.zoom = t[2];
        this.g.pose().popPose();
    }

    public void pushLayer(float z) {
        this.g.pose().pushPose();
        this.g.pose().translate(0.0F, 0.0F, z);
    }

    public void popLayer() {
        this.g.pose().popPose();
    }

    public void pushClip(int x, int y, int w, int h) {
        float s = this.baseScale;
        int x0 = (int)Math.floor((this.tx + x * this.zoom) * s);
        int y0 = (int)Math.floor((this.ty + y * this.zoom) * s);
        int x1 = (int)Math.ceil((this.tx + (x + w) * this.zoom) * s);
        int y1 = (int)Math.ceil((this.ty + (y + h) * this.zoom) * s);
        if (x1 < x0) {
            x1 = x0;
        }

        if (y1 < y0) {
            y1 = y0;
        }

        this.clips.push(new int[]{x0, y0, x1, y1});
        this.g.enableScissor(x0, y0, x1, y1);
    }

    public void popClip() {
        if (!this.clips.isEmpty()) {
            this.clips.pop();
            this.g.disableScissor();
        }
    }

    public void flush() {
        this.g.flush();
    }
}
