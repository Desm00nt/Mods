package org.sprautestudio.spraute_engine.engine;

import org.sprautestudio.spraute_engine.gui.GuiDoc;

public final class PlateLayout {
    public static final String TYPE = "speech_plate";
    public static final String ICON = "speech_icon";
    public static final String TEXT = "speech_text";
    public static final int MARGIN = 8;
    private static final int TOP = 24;
    private static final int BOTTOM = 50;
    private static final int FRAME_H = 100;
    private static final int ICON_MIN = 4;
    private static final int ICON_MAX = 128;

    private PlateLayout() {
    }

    public static boolean onlyPlate(GuiDoc doc) {
        return doc.elements.size() == 1 && doc.elements.get(0).type.equals("speech_plate");
    }

    public static GuiDoc.Element icon(GuiDoc.Element plate) {
        return part(plate, "speech_icon");
    }

    public static GuiDoc.Element text(GuiDoc.Element plate) {
        return part(plate, "speech_text");
    }

    private static GuiDoc.Element part(GuiDoc.Element plate, String type) {
        for (GuiDoc.Element e : plate.children) {
            if (e.type.equals(type)) {
                return e;
            }
        }

        return null;
    }

    public static boolean isPart(GuiDoc doc, GuiDoc.Element e) {
        for (GuiDoc.Element p : doc.all()) {
            if (p.type.equals("speech_plate") && p.children.contains(e)) {
                return true;
            }
        }

        return false;
    }

    private static String place(EngineConfig.ChatLog log) {
        return log.speechPlace == null ? "bottom" : log.speechPlace;
    }

    private static String align(EngineConfig.ChatLog log) {
        return log.speechAlign == null ? "center" : log.speechAlign;
    }

    private static int baseX(String align) {
        return align.equals("left") ? 8 : (align.equals("right") ? -8 : 0);
    }

    private static int baseY(String place) {
        return place.equals("top") ? 24 : (place.equals("center") ? 0 : -50);
    }

    private static String anchor(String place, String align) {
        if (place.equals("center")) {
            return align.equals("center") ? "center" : align;
        } else {
            String row = place.equals("top") ? "top" : "bottom";
            return align.equals("center") ? row : row + "_" + align;
        }
    }

    public static void fill(GuiDoc.Element plate, EngineConfig.ChatLog log) {
        String place = place(log);
        String align = align(log);
        plate.set("anchor", anchor(place, align));
        plate.set("x", String.valueOf(baseX(align) + log.speechShiftX));
        plate.set("y", String.valueOf(baseY(place) - log.speechShift));
        plate.set("w", Math.max(1, log.speechWidth) + "%");
        plate.set("h", String.valueOf(100));
        plate.set("halign", align);
        plate.set("valign", place.equals("top") ? "top" : (place.equals("center") ? "middle" : "bottom"));
        plate.set("background", String.format("#%08X", log.speechBack));
        plate.set("textColor", String.format("#%08X", log.speechText));
        int size = Math.max(4, Math.min(128, log.speechIconSize));
        fillPart(plate, "speech_icon", "speech_portrait", log.speechIconX, log.speechIconY, size, size, log.speechIconShown);
        fillPart(plate, "speech_text", "speech_words", log.speechTextX, log.speechTextY, 100, 20, true);
    }

    private static void fillPart(GuiDoc.Element plate, String type, String id, int x, int y, int w, int h, boolean visible) {
        GuiDoc.Element e = part(plate, type);
        if (e == null) {
            e = new GuiDoc.Element();
            e.type = type;
            e.id = id;
            plate.children.add(e);
        }

        e.set("anchor", "top_left");
        e.set("x", String.valueOf(x));
        e.set("y", String.valueOf(y));
        e.set("w", String.valueOf(w));
        e.set("h", String.valueOf(h));
        e.set("visible", String.valueOf(visible));
    }

    public static void read(GuiDoc.Element plate, EngineConfig.ChatLog log, int screenW) {
        String place = place(log);
        String align = align(log);
        log.speechShiftX = number(plate.get("x"), baseX(align)) - baseX(align);
        log.speechShift = baseY(place) - number(plate.get("y"), baseY(place));
        String w = plate.get("w").trim();
        int percent = w.endsWith("%") ? number(w.substring(0, w.length() - 1), log.speechWidth) : Math.round(number(w, 0) * 100.0F / Math.max(1, screenW));
        log.speechWidth = Math.max(10, Math.min(100, percent));
        log.speechBack = plate.color("background");
        log.speechText = plate.color("textColor");
        GuiDoc.Element icon = icon(plate);
        GuiDoc.Element text = text(plate);
        log.speechIconShown = icon != null && icon.b("visible");
        if (icon != null) {
            log.speechIconX = number(icon.get("x"), 0);
            log.speechIconY = number(icon.get("y"), 0);
            log.speechIconSize = Math.max(4, Math.min(128, number(icon.get("w"), log.speechIconSize)));
        }

        if (text != null) {
            log.speechTextX = number(text.get("x"), 0);
            log.speechTextY = number(text.get("y"), 0);
        }
    }

    private static int number(String text, int fallback) {
        try {
            return Math.round(Float.parseFloat(text.trim()));
        } catch (NumberFormatException var3) {
            return fallback;
        }
    }
}
