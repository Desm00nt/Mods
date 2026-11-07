package org.sprautestudio.spraute_engine.cutscene;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Ease {
    public static final Map<String, String> TITLES = new LinkedHashMap<>();

    private Ease() {
    }

    public static List<String> names() {
        return List.copyOf(TITLES.keySet());
    }

    public static boolean known(String name) {
        return name != null && TITLES.containsKey(name.toLowerCase());
    }

    public static double apply(String name, double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        if (name == null) {
            name = "smooth";
        }

        name = name.toLowerCase();
        if (name.equals("linear")) {
            return t;
        } else if (name.equals("smooth")) {
            return t * t * (3.0 - 2.0 * t);
        } else {
            String kind = name.startsWith("ease_in_out_") ? "inout" : (name.startsWith("ease_in_") ? "in" : (name.startsWith("ease_out_") ? "out" : "inout"));
            String f = name.substring(name.lastIndexOf(95) + 1);

            return switch (kind) {
                case "in" -> in(f, t);
                case "out" -> 1.0 - in(f, 1.0 - t);
                default -> t < 0.5 ? in(f, 2.0 * t) / 2.0 : 1.0 - in(f, 2.0 - 2.0 * t) / 2.0;
            };
        }
    }

    private static double in(String f, double t) {
        return switch (f) {
            case "sine" -> 1.0 - Math.cos(t * Math.PI / 2.0);
            case "quad" -> t * t;
            case "cubic" -> t * t * t;
            case "quart" -> t * t * t * t;
            case "expo" -> t == 0.0 ? 0.0 : Math.pow(2.0, 10.0 * t - 10.0);
            case "circ" -> 1.0 - Math.sqrt(1.0 - t * t);
            case "back" -> 2.70158 * t * t * t - 1.70158 * t * t;
            case "elastic" -> t != 0.0 && t != 1.0 ? -Math.pow(2.0, 10.0 * t - 10.0) * Math.sin((t * 10.0 - 10.75) * (Math.PI * 2) / 3.0) : t;
            case "bounce" -> 1.0 - bounceOut(1.0 - t);
            default -> t * t * (3.0 - 2.0 * t);
        };
    }

    private static double bounceOut(double t) {
        double n = 7.5625;
        double d = 2.75;
        if (t < 1.0 / d) {
            return n * t * t;
        } else if (t < 2.0 / d) {
            double var8;
            return n * (var8 = t - 1.5 / d) * var8 + 0.75;
        } else {
            double var6;
            double var7;
            return t < 2.5 / d ? n * (var6 = t - 2.25 / d) * var6 + 0.9375 : n * (var7 = t - 2.625 / d) * var7 + 0.984375;
        }
    }

    static {
        TITLES.put("linear", "равномерно");
        TITLES.put("smooth", "плавно (по умолчанию)");

        for (String f : List.of("sine", "quad", "cubic", "quart", "expo", "circ", "back", "elastic", "bounce")) {
            String ru = switch (f) {
                case "sine" -> "мягко";
                case "quad" -> "умеренно";
                case "cubic" -> "заметно";
                case "quart" -> "сильно";
                case "expo" -> "резко";
                case "circ" -> "по дуге";
                case "back" -> "с отскоком назад";
                case "elastic" -> "пружина";
                default -> "прыжки";
            };
            TITLES.put("ease_in_" + f, "разгон: " + ru);
            TITLES.put("ease_out_" + f, "торможение: " + ru);
            TITLES.put("ease_in_out_" + f, "разгон и торможение: " + ru);
        }
    }
}
