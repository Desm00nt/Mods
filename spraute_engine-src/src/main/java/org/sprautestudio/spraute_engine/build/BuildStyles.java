package org.sprautestudio.spraute_engine.build;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class BuildStyles {
    public static final int NAMED_EASE = 100;
    private static final Map<String, BuildStyles.Style> ALL = new LinkedHashMap<>();
    private static final Map<String, BuildStyles.Planner> PLANNERS = new LinkedHashMap<>();

    private BuildStyles() {
    }

    public static void add(
        String name, String title, String doc, BuildStyles.Ease ease, double flight, boolean spinning, double swirl, BuildStyles.Planner planner
    ) {
        s(name, title, doc, ease, flight, spinning, swirl);
        PLANNERS.put(name, planner);
    }

    private static void s(String name, String title, String doc, BuildStyles.Ease ease, double flight, boolean spinning, double swirl) {
        ALL.put(name, new BuildStyles.Style(name, title, doc, ease, flight, spinning, swirl));
    }

    public static BuildStyles.Style get(String name) {
        return ALL.get(name);
    }

    public static List<String> names() {
        return new ArrayList<>(ALL.keySet());
    }

    public static List<BuildStyles.Style> all() {
        return new ArrayList<>(ALL.values());
    }

    public static BuildStyles.Plan plan(String style, double x, double y, double z, double height, double width, long seed) {
        BuildStyles.Planner own = PLANNERS.get(style);
        if (own != null) {
            return own.plan(x, y, z, height, width, seed);
        } else {
            Random r = new Random(seed * 31L + (long)(x * 73.0) + (long)(y * 7919.0) + (long)(z * 104729.0));
            double jitter = r.nextDouble();
            double reach = Math.max(10.0, width);
            switch (style) {
                case "down":
                    return new BuildStyles.Plan(-y + jitter * 0.3, 0.0, 1.4, 0.0);
                case "left":
                    return new BuildStyles.Plan(x + jitter * 0.7, -reach, 2.0 * jitter, 0.0);
                case "right":
                    return new BuildStyles.Plan(-x + jitter * 0.7, reach, 2.0 * jitter, 0.0);
                case "front":
                    return new BuildStyles.Plan(z + jitter * 0.7, 0.0, 2.0 * jitter, -reach);
                case "back":
                    return new BuildStyles.Plan(-z + jitter * 0.7, 0.0, 2.0 * jitter, reach);
                case "rain":
                    return new BuildStyles.Plan(y * 3.0 + jitter * 6.0, 0.0, 16.0 + height + jitter * 10.0, 0.0);
                case "rise":
                    return new BuildStyles.Plan(y * 0.15, 0.0, -(height + 2.0), 0.0);
                case "center": {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    return new BuildStyles.Plan(d + jitter * 0.5, -x * 0.7, -y * 0.7, -z * 0.7);
                }
                case "edges": {
                    double d = Math.sqrt(x * x + z * z);
                    double k = reach / Math.max(1.0, d);
                    return new BuildStyles.Plan(-d + jitter * 0.5, x * k * 0.7, 3.0 * jitter, z * k * 0.7);
                }
                case "spiral": {
                    double a = Math.atan2(z, x);
                    return new BuildStyles.Plan(y * Math.PI * 2.0 + a, -Math.sin(a) * 6.0, 1.5, Math.cos(a) * 6.0);
                }
                case "tornado": {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double d = reach * (0.6 + jitter * 0.5);
                    return new BuildStyles.Plan(y * 2.0 + jitter * 4.0, Math.cos(a) * d, 4.0 + jitter * 8.0, Math.sin(a) * d);
                }
                case "scatter": {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double u = r.nextDouble() * 2.0 - 1.0;
                    double k = Math.sqrt(1.0 - u * u);
                    double d = reach * 1.4;
                    return new BuildStyles.Plan(y * 0.5 + jitter * 8.0, Math.cos(a) * k * d, Math.abs(u) * d, Math.sin(a) * k * d);
                }
                case "assemble": {
                    long cx = Math.floorDiv((long)Math.floor(x), 4L);
                    long cy = Math.floorDiv((long)Math.floor(y), 4L);
                    long cz = Math.floorDiv((long)Math.floor(z), 4L);
                    Random cell = new Random(seed ^ cx * 73856093L ^ cy * 19349663L ^ cz * 83492791L);
                    double a = cell.nextDouble() * Math.PI * 2.0;
                    double d = reach * (0.5 + cell.nextDouble() * 0.6);
                    return new BuildStyles.Plan(cy * 100L + cell.nextDouble() * 90.0, Math.cos(a) * d, 4.0 + cell.nextDouble() * 8.0, Math.sin(a) * d);
                }
                case "wave":
                    return new BuildStyles.Plan(x + z + y * 0.4 + jitter * 0.3, 0.0, 3.0, 0.0);
                case "pop":
                    return new BuildStyles.Plan(y * 0.3 + jitter * 8.0, 0.0, 0.0, 0.0);
                case "instant":
                    return new BuildStyles.Plan(y, 0.0, 0.0, 0.0);
                default:
                    return new BuildStyles.Plan(y + jitter * 0.3, 0.0, -1.2, 0.0);
            }
        }
    }

    static {
        s("up", "Снизу вверх", "Слой за слоем растёт от земли.", BuildStyles.Ease.BACK, 0.45, false, 0.0);
        s("down", "Сверху вниз", "Появляется с крыши, как будто опускается.", BuildStyles.Ease.BACK, 0.45, false, 0.0);
        s("left", "Слева", "Блоки летят с левой стороны от зрителя и наращивают здание.", BuildStyles.Ease.OUT, 0.7, true, 0.0);
        s("right", "Справа", "С правой стороны от зрителя.", BuildStyles.Ease.OUT, 0.7, true, 0.0);
        s("front", "Спереди", "От зрителя к дальней стене.", BuildStyles.Ease.OUT, 0.7, true, 0.0);
        s("back", "Сзади", "Из-за здания к зрителю.", BuildStyles.Ease.OUT, 0.7, true, 0.0);
        s("rain", "С неба", "Блоки падают сверху и подпрыгивают при приземлении.", BuildStyles.Ease.BOUNCE, 0.9, true, 0.0);
        s("rise", "Из-под земли", "Здание выезжает из земли целиком.", BuildStyles.Ease.OUT, 1.2, false, 0.0);
        s("center", "От центра", "Распускается из середины.", BuildStyles.Ease.BACK, 0.6, false, 0.0);
        s("edges", "С краёв к центру", "Стены сходятся к середине.", BuildStyles.Ease.OUT, 0.7, false, 0.0);
        s("spiral", "По спирали", "Обегает здание по кругу, поднимаясь.", BuildStyles.Ease.OUT, 0.6, true, 0.0);
        s("tornado", "Вихрь", "Блоки кружат вихрем и садятся на места.", BuildStyles.Ease.OUT, 1.3, true, 2.6);
        s("scatter", "Отовсюду", "Слетаются со всех сторон.", BuildStyles.Ease.OUT, 1.0, true, 0.0);
        s("assemble", "Сборка кусками", "Прилетает готовыми секциями, как конструктор.", BuildStyles.Ease.BACK, 0.9, false, 0.0);
        s("wave", "Волной", "Волна бежит по диагонали.", BuildStyles.Ease.BACK, 0.5, false, 0.0);
        s("pop", "Вырастают", "Каждый блок раздувается из ничего, вразнобой.", BuildStyles.Ease.GROW, 0.35, false, 0.0);
        s("instant", "Сразу", "Без анимации.", BuildStyles.Ease.OUT, 0.0, false, 0.0);
    }

    public static enum Ease {
        OUT,
        BACK,
        BOUNCE,
        GROW;
    }

    public record Plan(double key, double fromX, double fromY, double fromZ) {
    }

    @FunctionalInterface
    public interface Planner {
        BuildStyles.Plan plan(double var1, double var3, double var5, double var7, double var9, long var11);
    }

    public record Style(String name, String title, String doc, BuildStyles.Ease ease, double flight, boolean spinning, double swirl) {
    }
}
