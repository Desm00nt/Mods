package org.sprautestudio.spraute_engine.gui;

import java.util.List;
import org.sprautestudio.spraute_engine.cutscene.Ease;

public final class GuiMotion {
    public static final String NONE = "none";
    public static final String AUTO = "auto";
    public static final List<String> APPEAR = List.of(
        "none", "fade", "slide_up", "slide_down", "slide_left", "slide_right", "zoom", "pop", "rotate", "flip_x", "flip_y", "drop"
    );
    public static final List<String> LOOPS = List.of("none", "float", "pulse", "spin", "sway", "shake", "blink", "breathe", "swing_3d", "turn_3d");
    private static final float SLIDE_V = 24.0F;
    private static final float SLIDE_H = 32.0F;
    private static final float DROP = 40.0F;
    private static final float QUARTER = 90.0F;
    private static final float FULL = 360.0F;
    private static final float ZOOM_FROM = 0.6F;
    private static final float POP_FROM = 0.5F;
    private static final double FLOAT_PIXELS = 4.0;
    private static final double PULSE = 0.06;
    private static final double SWAY_DEGREES = 6.0;
    private static final double SHAKE_PIXELS = 1.5;
    private static final double SHAKE_RATE = 9.0;
    private static final double SHAKE_RATE_Y = 13.0;
    private static final double SWING_DEGREES = 20.0;
    private static final double BLINK_LOW = 0.25;
    private static final double BREATHE_LOW = 0.6;

    private GuiMotion() {
    }

    public static GuiMotion.Frame appear(String kind, double t, String ease) {
        GuiMotion.Frame f = new GuiMotion.Frame();
        boolean auto = ease == null || ease.isEmpty() || ease.equals("auto");
        float k = (float)(auto ? (kind.equals("pop") ? overshoot(t) : Ease.apply("ease_out_cubic", t)) : Ease.apply(ease, t));
        float left = 1.0F - k;
        float seen = (float)Math.max(0.0, Math.min(1.0, kind.equals("pop") ? t * 3.0 : t * 2.0));
        switch (kind) {
            case "fade":
                f.opacity = (float)Math.max(0.0, Math.min(1.0, t));
                break;
            case "slide_up":
                slide(f, 0.0F, left * 24.0F, seen);
                break;
            case "slide_down":
                slide(f, 0.0F, -left * 24.0F, seen);
                break;
            case "slide_left":
                slide(f, left * 32.0F, 0.0F, seen);
                break;
            case "slide_right":
                slide(f, -left * 32.0F, 0.0F, seen);
                break;
            case "drop":
                slide(f, 0.0F, -left * 40.0F, seen);
                break;
            case "zoom":
                f.scale = 0.6F + 0.39999998F * k;
                f.opacity = seen;
                break;
            case "pop":
                f.scale = 0.5F + 0.5F * k;
                f.opacity = seen;
                break;
            case "rotate":
                f.rotZ = -left * 90.0F;
                f.scale = 0.6F + 0.39999998F * k;
                f.opacity = seen;
                break;
            case "flip_x":
                f.rotX = left * 90.0F;
                f.opacity = seen;
                break;
            case "flip_y":
                f.rotY = left * 90.0F;
                f.opacity = seen;
        }

        return f;
    }

    private static void slide(GuiMotion.Frame f, float dx, float dy, float seen) {
        f.dx = dx;
        f.dy = dy;
        f.opacity = seen;
    }

    private static double overshoot(double t) {
        double u = Math.max(0.0, Math.min(1.0, t)) - 1.0;
        double c = 1.70158;
        return 1.0 + (c + 1.0) * u * u * u + c * u * u;
    }

    public static GuiMotion.Frame loop(String kind, double seconds, double speed, double power) {
        GuiMotion.Frame f = new GuiMotion.Frame();
        double a = seconds * speed * 2.0 * Math.PI;
        double wave = Math.sin(a);
        switch (kind) {
            case "float":
                f.dy = (float)(wave * 4.0 * power);
                break;
            case "pulse":
                f.scale = (float)(1.0 + wave * 0.06 * power);
                break;
            case "spin":
                f.rotZ = (float)(seconds * speed * 360.0 * power % 360.0);
                break;
            case "sway":
                f.rotZ = (float)(wave * 6.0 * power);
                break;
            case "shake":
                f.dx = (float)(Math.sin(a * 9.0) * 1.5 * power);
                f.dy = (float)(Math.cos(a * 13.0) * 1.5 * power);
                break;
            case "blink":
                f.opacity = wave >= 0.0 ? 1.0F : 0.25F;
                break;
            case "breathe":
                f.opacity = (float)(0.6 + 0.4 * (wave + 1.0) / 2.0);
                break;
            case "swing_3d":
                f.rotY = (float)(wave * 20.0 * power);
                break;
            case "turn_3d":
                f.rotY = (float)(seconds * speed * 360.0 % 360.0);
        }

        return f;
    }

    public static float[] crossfade(float h) {
        float t = Math.max(0.0F, Math.min(1.0F, h));
        return new float[]{1.0F - Math.max(0.0F, 2.0F * t - 1.0F), Math.min(1.0F, 2.0F * t)};
    }

    public static float hoverStep(float dt, float seconds) {
        return Math.min(1.0F, dt * 2.8F / Math.max(0.01F, seconds));
    }

    public static float[] tilt(float mouseX, float mouseY, float degrees) {
        float x = Math.max(-1.0F, Math.min(1.0F, mouseX));
        float y = Math.max(-1.0F, Math.min(1.0F, mouseY));
        return new float[]{-y * degrees, x * degrees};
    }

    public static final class Frame {
        public float dx;
        public float dy;
        public float rotZ;
        public float rotX;
        public float rotY;
        public float scale = 1.0F;
        public float opacity = 1.0F;
    }
}
