package org.sprautestudio.spraute_engine.gui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.sprautestudio.spraute_engine.fx.FxDef;

public record PanelLook(String turn, float opacity, int tint, boolean through, boolean keepSize, float fade, float smooth, float bob) {
    public static final PanelLook PLAIN = new PanelLook("", 1.0F, 16777215, false, false, 0.0F, 0.0F, 0.0F);
    public static final String FIXED = "fixed";
    public static final String VIEWER = "viewer";
    public static final String CAMERA = "camera";
    public static final String BODY = "body";
    public static final String BONE = "bone";
    public static final List<String> TURNS = List.of("fixed", "viewer", "camera", "body", "bone");
    public static final List<String> VALUES = List.of("turn", "look", "opacity", "color", "fade", "smooth", "bob");
    public static final List<String> FLAGS = List.of("through", "keepsize");
    private static final Map<String, Map<String, String>> LOOKS = new LinkedHashMap<>();

    public static List<String> looks() {
        return List.copyOf(LOOKS.keySet());
    }

    public static boolean isLook(String name) {
        return LOOKS.containsKey(name);
    }

    public static String lookTitle(String name) {
        return switch (name) {
            case "plate" -> "Табличка: над головой, тает вдали";
            case "marker" -> "Метка: видна сквозь стены, размер постоянный";
            case "hologram" -> "Голограмма: полупрозрачная, голубая, парит";
            case "sign" -> "Вывеска: повёрнута вместе с телом";
            default -> name;
        };
    }

    public static String turnTitle(String turn) {
        return switch (turn) {
            case "fixed" -> "Стоит как поставили";
            case "viewer" -> "Поворачивается к смотрящему";
            case "camera" -> "Всегда прямо в камеру";
            case "body" -> "Вместе с телом";
            case "bone" -> "Вместе с костью";
            default -> turn;
        };
    }

    public static PanelLook of(Function<String, String> written) {
        String lookName = written.apply("look");
        Map<String, String> ready = lookName != null ? LOOKS.getOrDefault(lookName.trim(), Map.of()) : Map.of();
        Function<String, String> value = key -> {
            String own = written.apply(key);
            return own != null && !own.isBlank() ? own.trim() : ready.get(key);
        };
        String turn = value.apply("turn");
        String color = value.apply("color");
        return new PanelLook(
            turn != null && TURNS.contains(turn) ? turn : "",
            clamp(number(value.apply("opacity"), 1.0F), 0.0F, 1.0F),
            color != null ? FxDef.parseColor(color) : 16777215,
            flag(value.apply("through")),
            flag(value.apply("keepsize")),
            Math.max(0.0F, number(value.apply("fade"), 0.0F)),
            Math.max(0.0F, number(value.apply("smooth"), 0.0F)),
            number(value.apply("bob"), 0.0F)
        );
    }

    private static boolean flag(String v) {
        return v != null && !v.isBlank() && !v.equals("нет");
    }

    private static float number(String v, float fallback) {
        if (v == null) {
            return fallback;
        } else {
            try {
                return Float.parseFloat(v.replace(',', '.'));
            } catch (NumberFormatException var3) {
                return fallback;
            }
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public float clearness(double distance, float range) {
        return !(this.fade <= 0.0F) && !(range <= this.fade)
            ? this.opacity * clamp((float)(1.0 - (distance - this.fade) / (range - this.fade)), 0.0F, 1.0F)
            : this.opacity;
    }

    public static double[] offset(double bodyYaw, double right, double up, double forward) {
        double y = Math.toRadians(bodyYaw);
        double s = Math.sin(y);
        double c = Math.cos(y);
        return new double[]{-c * right - s * forward, up, -s * right + c * forward};
    }

    public static double[] unoffset(double bodyYaw, double dx, double dy, double dz) {
        double y = Math.toRadians(bodyYaw);
        double s = Math.sin(y);
        double c = Math.cos(y);
        return new double[]{-c * dx - s * dz, dy, -s * dx + c * dz};
    }

    static {
        LOOKS.put("plate", Map.of("turn", "viewer", "fade", "20", "smooth", "0.08"));
        LOOKS.put("marker", Map.of("turn", "camera", "through", "да", "keepsize", "да", "bob", "0.08"));
        LOOKS.put("hologram", Map.of("turn", "viewer", "opacity", "0.8", "color", "#88DDFF", "bob", "0.06", "smooth", "0.15"));
        LOOKS.put("sign", Map.of("turn", "body", "smooth", "0.05"));
    }
}
