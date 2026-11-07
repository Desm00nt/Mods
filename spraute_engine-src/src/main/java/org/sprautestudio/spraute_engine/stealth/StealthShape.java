package org.sprautestudio.spraute_engine.stealth;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StealthShape {
    public static final List<String> NAMES = List.of("fan", "circle", "rect", "triangle");
    public static final List<String> RESTORES = List.of("inventory", "coins", "health", "xp", "blocks", "loot");

    private StealthShape() {
    }

    public static double reach(String shape, double theta, double range, double angle, double width) {
        double a = Math.abs(Math.IEEEremainder(theta, Math.PI * 2));

        return switch (shape) {
            case "circle" -> range;
            case "rect" -> {
                if (a >= Math.PI / 2) {
                    yield 0.0;
                } else {
                    double s = Math.sin(a);
                    yield s < 1.0E-6 ? range : Math.min(range / Math.cos(a), width / 2.0 / s);
                }
            }
            case "triangle" -> a <= Math.atan2(width / 2.0, range) ? range / Math.cos(a) : 0.0;
            default -> a <= Math.toRadians(angle) / 2.0 ? range : 0.0;
        };
    }

    public static double span(String shape, double range, double angle, double width) {
        return switch (shape) {
            case "circle" -> Math.PI;
            case "rect" -> Math.PI / 2;
            case "triangle" -> Math.atan2(width / 2.0, range);
            default -> Math.min(Math.PI, Math.toRadians(angle) / 2.0);
        };
    }

    public static boolean contains(String shape, double dx, double dz, float yaw, double range, double angle, double width) {
        double dist = Math.hypot(dx, dz);
        return dist < 1.0E-6 ? true : dist <= reach(shape, Math.atan2(-dx, dz) - Math.toRadians(yaw), range, angle, width);
    }

    public static Set<String> restores(String text) {
        Set<String> out = new LinkedHashSet<>();

        for (String word : text.toLowerCase(Locale.ROOT).split("[\\s,]+")) {
            if (!word.isEmpty() && !word.equals("none")) {
                if (word.equals("all")) {
                    out.addAll(RESTORES);
                } else {
                    if (!RESTORES.contains(word)) {
                        throw new IllegalArgumentException("Нельзя откатить «" + word + "»: можно " + String.join(", ", RESTORES) + ", all или none");
                    }

                    out.add(word);
                }
            }
        }

        if (text.isBlank()) {
            out.addAll(RESTORES);
        }

        return out;
    }
}
