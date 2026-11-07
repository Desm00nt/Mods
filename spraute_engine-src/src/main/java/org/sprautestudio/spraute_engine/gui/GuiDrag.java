package org.sprautestudio.spraute_engine.gui;

public final class GuiDrag {
    private GuiDrag() {
    }

    public static boolean accepts(String accept, String tag) {
        if (accept != null && !accept.isBlank()) {
            for (String want : accept.split(",")) {
                if (want.trim().equalsIgnoreCase(tag == null ? "" : tag.trim())) {
                    return true;
                }
            }

            return false;
        } else {
            return true;
        }
    }

    public static float snap(float v, int grid) {
        return grid <= 0 ? v : (float)Math.round(v / grid) * grid;
    }

    public static float inside(float pos, float size, float from, float to) {
        return Math.max(from, Math.min(to - size, pos));
    }
}
