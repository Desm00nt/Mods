package org.sprautestudio.spraute_engine.runtime;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public final class Colors {
    public static final Map<String, Integer> NAMES = new LinkedHashMap<>();

    private Colors() {
    }

    private static void name(int rgb, String... names) {
        for (String n : names) {
            NAMES.put(n, rgb);
        }
    }

    public static Integer parse(String s) {
        if (s == null) {
            return null;
        } else {
            String t = s.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
            if (!t.isEmpty() && !t.equals("default") && !t.equals("reset") && !t.equals("обычный")) {
                for (Entry<String, Integer> e : NAMES.entrySet()) {
                    if (e.getKey().replace('ё', 'е').equals(t)) {
                        return e.getValue();
                    }
                }

                if (t.startsWith("#")) {
                    t = t.substring(1);
                }

                if (t.startsWith("0x")) {
                    t = t.substring(2);
                }

                if (t.length() == 3) {
                    t = "" + t.charAt(0) + t.charAt(0) + t.charAt(1) + t.charAt(1) + t.charAt(2) + t.charAt(2);
                }

                if (t.length() == 8) {
                    t = t.substring(2);
                }

                if (t.length() != 6) {
                    throw new SprauteRuntimeException("Цвет «" + s + "»: нужен #RRGGBB или имя (red, gold, aqua, белый, золотой…)");
                } else {
                    try {
                        return Integer.parseInt(t, 16);
                    } catch (NumberFormatException var4) {
                        throw new SprauteRuntimeException("Цвет «" + s + "»: нужен #RRGGBB или имя (red, gold, aqua, белый, золотой…)");
                    }
                }
            } else {
                return null;
            }
        }
    }

    static {
        name(16777215, "white", "белый");
        name(11184810, "gray", "серый");
        name(5592405, "dark_gray", "тёмно-серый");
        name(0, "black", "чёрный");
        name(16733525, "red", "красный");
        name(11141120, "dark_red", "тёмно-красный");
        name(16755200, "gold", "золотой");
        name(16747546, "orange", "оранжевый");
        name(16777045, "yellow", "жёлтый");
        name(5635925, "green", "зелёный");
        name(43520, "dark_green", "тёмно-зелёный");
        name(5636095, "aqua", "голубой");
        name(43690, "dark_aqua", "бирюзовый");
        name(5592575, "blue", "синий");
        name(170, "dark_blue", "тёмно-синий");
        name(16733695, "pink", "розовый");
        name(16733695, "light_purple", "сиреневый");
        name(11141290, "purple", "фиолетовый");
    }
}
