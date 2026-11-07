package org.sprautestudio.spraute_engine.gui;

import java.util.List;
import java.util.Set;

public final class MenuDoc {
    public static final String WIDGET = "menu_widget";
    public static final String ROLE = "menu:";
    public static final String SLOTS = "menu_slots";
    public static final String TITLE = "title";
    public static final String HIDE_LOGO = "hideLogo";
    public static final String HIDE_SPLASH = "hideSplash";
    public static final String HIDE_VERSION = "hideVersion";
    public static final String HIDE_BACK = "hideBack";
    public static final String BACK_IMAGE = "backImage";
    public static final String HIDE_LABELS = "hideLabels";
    public static final String HIDE_DIM = "hideDim";

    private MenuDoc() {
    }

    public static String key(String translationKey, int index, Set<String> used) {
        String base = translationKey != null && !translationKey.isEmpty() ? translationKey : "button" + index;
        String key = base;

        for (int n = 2; !used.add(key); n++) {
            key = base + "#" + n;
        }

        return key;
    }

    public static GuiDoc build(String title, int width, int height, List<MenuDoc.Part> parts) {
        GuiDoc doc = new GuiDoc();
        doc.set("title", title);
        doc.set("layout", "screen");
        doc.set("width", String.valueOf(width));
        doc.set("height", String.valueOf(height));
        doc.set("background", "#00000000");

        for (MenuDoc.Part p : parts) {
            GuiDoc.Element e = new GuiDoc.Element();
            e.type = "menu_widget";
            e.id = p.key();
            e.set("anchor", "top_left");
            e.set("x", String.valueOf(p.x()));
            e.set("y", String.valueOf(p.y()));
            e.set("w", String.valueOf(p.w()));
            e.set("h", String.valueOf(p.h()));
            e.set("text", p.text());
            e.set("baseX", String.valueOf(p.x()));
            e.set("baseY", String.valueOf(p.y()));
            e.set("baseW", String.valueOf(p.w()));
            e.set("baseH", String.valueOf(p.h()));
            e.set("baseText", p.text());
            doc.elements.add(e);
        }

        return doc;
    }

    public static boolean worthShowing(int knownButtons, GuiDoc saved) {
        return knownButtons > 0 || saved != null && !saved.elements.isEmpty();
    }

    public static MenuDoc.Change change(GuiDoc.Element e) {
        String text = e.get("text");
        int baseW = e.i("baseW");
        int baseH = e.i("baseH");
        return new MenuDoc.Change(
            e.i("x") - e.i("baseX"),
            e.i("y") - e.i("baseY"),
            e.i("w") == baseW ? -1 : Math.max(1, e.i("w")),
            e.i("h") == baseH ? -1 : Math.max(1, e.i("h")),
            text.equals(e.get("baseText")) ? null : text,
            e.b("visible"),
            e.b("enabled")
        );
    }

    public record Change(int dx, int dy, int w, int h, String text, boolean visible, boolean enabled) {
    }

    public record Part(String key, int x, int y, int w, int h, String text) {
    }
}
