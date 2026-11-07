package org.sprautestudio.spraute_engine.editor.texture;

import java.util.ArrayDeque;
import java.util.function.IntBinaryOperator;

public final class PixelOps {
    private static final int[] BAYER4 = new int[]{0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};
    private static final int[] BAYER2 = new int[]{0, 2, 3, 1};

    private PixelOps() {
    }

    public static int a(int c) {
        return c >>> 24;
    }

    public static int over(int dst, int src, float opacity) {
        int sa = Math.round((src >>> 24) * opacity);
        if (sa == 0) {
            return dst;
        } else {
            int da = dst >>> 24;
            if (sa != 255 && da != 0) {
                float s = sa / 255.0F;
                float d = da / 255.0F * (1.0F - s);
                float oa = s + d;
                int r = Math.round(((src >> 16 & 0xFF) * s + (dst >> 16 & 0xFF) * d) / oa);
                int g = Math.round(((src >> 8 & 0xFF) * s + (dst >> 8 & 0xFF) * d) / oa);
                int b = Math.round(((src & 0xFF) * s + (dst & 0xFF) * d) / oa);
                return Math.round(oa * 255.0F) << 24 | r << 16 | g << 8 | b;
            } else {
                return sa << 24 | src & 16777215;
            }
        }
    }

    public static float[] hsv(int c) {
        float r = (c >> 16 & 0xFF) / 255.0F;
        float g = (c >> 8 & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h = 0.0F;
        if (d > 0.0F) {
            if (max == r) {
                h = (g - b) / d % 6.0F;
            } else if (max == g) {
                h = (b - r) / d + 2.0F;
            } else {
                h = (r - g) / d + 4.0F;
            }

            h = (h * 60.0F + 360.0F) % 360.0F;
        }

        return new float[]{h, max == 0.0F ? 0.0F : d / max, max};
    }

    public static int fromHsv(float h, float s, float v, int alpha) {
        h = (h % 360.0F + 360.0F) % 360.0F;
        s = clamp(s);
        v = clamp(v);
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

        return alpha << 24 | Math.round((r + m) * 255.0F) << 16 | Math.round((g + m) * 255.0F) << 8 | Math.round((b + m) * 255.0F);
    }

    static float clamp(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static float towards(float h, float target, float by) {
        float d = (target - h + 540.0F) % 360.0F - 180.0F;
        return h + Math.signum(d) * Math.min(Math.abs(d), by);
    }

    public static int shade(int c, int steps, float hueShift) {
        if (steps != 0 && c >>> 24 != 0) {
            float[] hsv = hsv(c);
            float h = hsv[0];
            float s = hsv[1];
            float v = hsv[2];
            boolean grey = s < 0.06F;

            for (int i = 0; i < Math.abs(steps); i++) {
                if (steps < 0) {
                    if (!grey) {
                        h = towards(h, 250.0F, hueShift);
                    }

                    s = clamp(s + (grey ? 0.02F : 0.07F));
                    v = clamp(v - 0.13F);
                } else {
                    if (!grey) {
                        h = towards(h, 55.0F, hueShift);
                    }

                    s = clamp(s - (grey ? 0.0F : 0.06F));
                    v = clamp(v + 0.12F);
                }
            }

            return fromHsv(h, grey ? Math.min(s, 0.12F) : s, v, c >>> 24);
        } else {
            return c;
        }
    }

    public static int[] ramp(int c, int n, float hueShift) {
        int[] out = new int[n];
        int mid = n / 2;

        for (int i = 0; i < n; i++) {
            out[i] = shade(c, i - mid, hueShift);
        }

        return out;
    }

    public static int[] blend(int a, int b, int n) {
        int[] out = new int[n];
        float[] ha = hsv(a);
        float[] hb = hsv(b);
        boolean greyA = ha[1] < 0.05F;
        boolean greyB = hb[1] < 0.05F;
        if (greyA) {
            ha[0] = hb[0];
        }

        if (greyB) {
            hb[0] = ha[0];
        }

        float dh = (hb[0] - ha[0] + 540.0F) % 360.0F - 180.0F;

        for (int i = 0; i < n; i++) {
            float t = n == 1 ? 0.0F : (float)i / (n - 1);
            int alpha = Math.round((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
            out[i] = fromHsv(ha[0] + dh * t, ha[1] + (hb[1] - ha[1]) * t, ha[2] + (hb[2] - ha[2]) * t, alpha);
        }

        return out;
    }

    public static float threshold(int x, int y, int pattern) {
        return switch (pattern) {
            case 1 -> (BAYER2[(y & 1) * 2 + (x & 1)] + 0.5F) / 4.0F;
            case 2 -> (BAYER4[(y & 3) * 4 + (x & 3)] + 0.5F) / 16.0F;
            default -> 0.5F;
        };
    }

    public static int gradientIndex(float t, int n, int x, int y, int pattern, float spread) {
        float p = clamp(t) * (n - 1);
        int base = (int)Math.floor(p);
        if (base >= n - 1) {
            return n - 1;
        } else {
            float frac = p - base;
            if (pattern != 0 && !(spread <= 0.0F)) {
                float s = Math.max(0.01F, spread);
                float f = clamp((frac - 0.5F) / s + 0.5F);
                return f > threshold(x, y, pattern) ? base + 1 : base;
            } else {
                return frac >= 0.5F ? base + 1 : base;
            }
        }
    }

    public static void line(int x0, int y0, int x1, int y1, PixelOps.Plot p) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            p.at(x0, y0);
            if (x0 == x1 && y0 == y1) {
                return;
            }

            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }

            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    public static void rect(int x0, int y0, int x1, int y1, boolean filled, PixelOps.Plot p) {
        int ax = Math.min(x0, x1);
        int bx = Math.max(x0, x1);
        int ay = Math.min(y0, y1);
        int by = Math.max(y0, y1);

        for (int y = ay; y <= by; y++) {
            for (int x = ax; x <= bx; x++) {
                if (filled || x == ax || x == bx || y == ay || y == by) {
                    p.at(x, y);
                }
            }
        }
    }

    public static void ellipse(int x0, int y0, int x1, int y1, boolean filled, PixelOps.Plot p) {
        int ax = Math.min(x0, x1);
        int bx = Math.max(x0, x1);
        int ay = Math.min(y0, y1);
        int by = Math.max(y0, y1);
        double cx = (ax + bx) / 2.0;
        double cy = (ay + by) / 2.0;
        double rx = (bx - ax) / 2.0 + 0.5;
        double ry = (by - ay) / 2.0 + 0.5;

        for (int y = ay; y <= by; y++) {
            for (int x = ax; x <= bx; x++) {
                if (insideE(x, y, cx, cy, rx, ry)) {
                    if (filled) {
                        p.at(x, y);
                    } else if (!insideE(x - 1, y, cx, cy, rx, ry)
                        || !insideE(x + 1, y, cx, cy, rx, ry)
                        || !insideE(x, y - 1, cx, cy, rx, ry)
                        || !insideE(x, y + 1, cx, cy, rx, ry)) {
                        p.at(x, y);
                    }
                }
            }
        }
    }

    private static boolean insideE(int x, int y, double cx, double cy, double rx, double ry) {
        double dx = (x - cx) / rx;
        double dy = (y - cy) / ry;
        return dx * dx + dy * dy <= 1.0;
    }

    public static boolean[] region(int[] px, int w, int h, int x, int y, boolean contiguous, int tolerance) {
        boolean[] m = new boolean[w * h];
        int target = px[y * w + x];
        if (!contiguous) {
            for (int i = 0; i < px.length; i++) {
                m[i] = close(px[i], target, tolerance);
            }

            return m;
        } else {
            ArrayDeque<Integer> todo = new ArrayDeque<>();
            todo.add(y * w + x);
            m[y * w + x] = true;

            while (!todo.isEmpty()) {
                int i = todo.poll();
                int cx = i % w;
                int cy = i / w;
                int[][] n = new int[][]{{cx - 1, cy}, {cx + 1, cy}, {cx, cy - 1}, {cx, cy + 1}};

                for (int[] q : n) {
                    if (q[0] >= 0 && q[1] >= 0 && q[0] < w && q[1] < h) {
                        int j = q[1] * w + q[0];
                        if (!m[j] && close(px[j], target, tolerance)) {
                            m[j] = true;
                            todo.add(j);
                        }
                    }
                }
            }

            return m;
        }
    }

    static boolean close(int a, int b, int tol) {
        if (a >>> 24 == 0 && b >>> 24 == 0) {
            return true;
        } else if (tol <= 0) {
            return a == b;
        } else {
            int d = Math.abs((a >>> 24) - (b >>> 24))
                + Math.abs((a >> 16 & 0xFF) - (b >> 16 & 0xFF))
                + Math.abs((a >> 8 & 0xFF) - (b >> 8 & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
            return d <= tol * 4;
        }
    }

    public static void apply(int[] px, boolean[] mask, IntBinaryOperator f) {
        for (int i = 0; i < px.length; i++) {
            if (mask == null || mask[i]) {
                px[i] = f.applyAsInt(px[i], i);
            }
        }
    }

    public interface Plot {
        void at(int var1, int var2);
    }
}
