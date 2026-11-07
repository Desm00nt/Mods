package org.sprautestudio.spraute_engine.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GuiDoc {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public Map<String, String> props = new LinkedHashMap<>();
    public List<GuiDoc.Element> elements = new ArrayList<>();

    public String get(String key) {
        String v = this.props.get(key);
        if (v != null) {
            return v;
        } else {
            GuiSchema.Prop p = GuiSchema.docProp(key);
            return p != null ? p.def() : "";
        }
    }

    public void set(String key, String value) {
        GuiSchema.Prop p = GuiSchema.docProp(key);
        if (p != null && value.equals(p.def())) {
            this.props.remove(key);
        } else {
            this.props.put(key, value);
        }
    }

    public int width() {
        return Math.max(16, toInt(this.get("width")));
    }

    public int height() {
        return Math.max(16, toInt(this.get("height")));
    }

    public GuiDoc.Element element(String id) {
        if (id == null) {
            return null;
        } else {
            for (GuiDoc.Element e : this.all()) {
                if (id.equals(e.id)) {
                    return e;
                }
            }

            return null;
        }
    }

    public List<GuiDoc.Element> all() {
        List<GuiDoc.Element> out = new ArrayList<>();
        collect(this.elements, out);
        return out;
    }

    private static void collect(List<GuiDoc.Element> list, List<GuiDoc.Element> out) {
        for (GuiDoc.Element e : list) {
            out.add(e);
            collect(e.children, out);
        }
    }

    public List<String> ids() {
        List<String> list = new ArrayList<>();

        for (GuiDoc.Element e : this.all()) {
            list.add(e.id);
        }

        return list;
    }

    public List<GuiDoc.Element> listOf(GuiDoc.Element e) {
        if (this.elements.contains(e)) {
            return this.elements;
        } else {
            for (GuiDoc.Element p : this.all()) {
                if (p.children.contains(e)) {
                    return p.children;
                }
            }

            return null;
        }
    }

    public GuiDoc.Element parentOf(GuiDoc.Element e) {
        for (GuiDoc.Element p : this.all()) {
            if (p.children.contains(e)) {
                return p;
            }
        }

        return null;
    }

    public static boolean contains(GuiDoc.Element outer, GuiDoc.Element inner) {
        if (outer == inner) {
            return true;
        } else {
            for (GuiDoc.Element c : outer.children) {
                if (contains(c, inner)) {
                    return true;
                }
            }

            return false;
        }
    }

    public void renameUnique(GuiDoc.Element e) {
        if (this.element(e.id) != null) {
            e.id = this.freshId(e.type);
        }

        for (GuiDoc.Element c : e.children) {
            this.renameUnique(c);
        }
    }

    public String freshId(String type) {
        int n = 1;

        while (this.element(type + n) != null) {
            n++;
        }

        return type + n;
    }

    public GuiDoc.Element add(String type) {
        GuiDoc.Element e = new GuiDoc.Element();
        e.type = type;
        e.id = this.freshId(type);
        this.elements.add(e);
        return e;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static GuiDoc fromJson(String json) {
        GuiDoc d = null;

        try {
            d = GSON.fromJson(json, GuiDoc.class);
        } catch (RuntimeException var3) {
        }

        if (d == null) {
            d = new GuiDoc();
        }

        if (d.props == null) {
            d.props = new LinkedHashMap<>();
        }

        if (d.elements == null) {
            d.elements = new ArrayList<>();
        }

        fix(d, d.elements);
        return d;
    }

    private static void fix(GuiDoc d, List<GuiDoc.Element> list) {
        list.removeIf(ex -> ex == null);

        for (GuiDoc.Element e : list) {
            if (e.props == null) {
                e.props = new LinkedHashMap<>();
            }

            if (e.children == null) {
                e.children = new ArrayList<>();
            }

            if (e.type == null || GuiSchema.type(e.type) == null) {
                e.type = "panel";
            }

            if (GuiSchema.singleImage(e.type)) {
                String old = e.props.remove("backgroundImage");
                if (old != null && !old.isBlank() && e.get("image").isBlank()) {
                    e.set("image", old);
                }
            }

            if (e.id == null) {
                e.id = d.freshId(e.type);
            }

            if (!e.isContainer()) {
                e.children.clear();
            }

            fix(d, e.children);
        }
    }

    public static GuiDoc example(String title) {
        GuiDoc d = new GuiDoc();
        d.set("title", title);
        GuiDoc.Element bg = d.add("panel");
        bg.id = "window";
        bg.set("w", "300");
        bg.set("h", "170");
        bg.set("appear", "pop");
        GuiDoc.Element head = d.add("text");
        head.id = "title";
        head.set("text", title);
        head.set("y", "-68");
        head.set("w", "280");
        head.set("align", "center");
        head.set("scale", "1.3");
        head.set("color", "#FFA08AFF");
        GuiDoc.Element who = d.add("entity");
        who.id = "portrait";
        who.set("x", "-100");
        who.set("y", "4");
        who.set("w", "70");
        who.set("h", "100");
        who.set("followMouse", "true");
        GuiDoc.Element text = d.add("text");
        text.id = "message";
        text.set("text", "Привет! Это интерфейс Spraute.\\nКнопки сообщают сюжету, какую нажали.");
        text.set("x", "40");
        text.set("y", "-20");
        text.set("w", "190");
        text.set("h", "60");
        text.set("typewriter", "40");
        GuiDoc.Element ok = d.add("button");
        ok.id = "yes";
        ok.set("text", "Да");
        ok.set("x", "-10");
        ok.set("y", "55");
        ok.set("w", "90");
        ok.set("key", "1");
        GuiDoc.Element no = d.add("button");
        no.id = "no";
        no.set("text", "Нет");
        no.set("x", "90");
        no.set("y", "55");
        no.set("w", "90");
        no.set("key", "2");
        no.set("action", "event_close");
        return d;
    }

    public static int toInt(String s) {
        try {
            return (int)Math.round(Double.parseDouble(s.trim()));
        } catch (RuntimeException var2) {
            return 0;
        }
    }

    public static float toFloat(String s) {
        try {
            return Float.parseFloat(s.trim().replace(',', '.'));
        } catch (RuntimeException var2) {
            return 0.0F;
        }
    }

    public static boolean toBool(String s) {
        String v = s.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("да") || v.equals("yes") || v.equals("on");
    }

    public static int toColor(String s) {
        String v = s.trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }

        try {
            if (v.length() == 3) {
                int r = Integer.parseInt(v.substring(0, 1), 16);
                int g = Integer.parseInt(v.substring(1, 2), 16);
                int b = Integer.parseInt(v.substring(2, 3), 16);
                return 0xFF000000 | r * 17 << 16 | g * 17 << 8 | b * 17;
            }

            if (v.length() == 6) {
                return 0xFF000000 | Integer.parseInt(v, 16);
            }

            if (v.length() == 8) {
                return (int)Long.parseLong(v, 16);
            }
        } catch (NumberFormatException var5) {
        }

        return 0;
    }

    public static String colorString(int argb) {
        return String.format("#%08X", argb);
    }

    public static final class Element {
        public String id;
        public String type;
        public Map<String, String> props = new LinkedHashMap<>();
        public boolean locked;
        public List<GuiDoc.Element> children = new ArrayList<>();

        public boolean isContainer() {
            return GuiSchema.isContainer(this.type);
        }

        public GuiSchema.Type schema() {
            GuiSchema.Type t = GuiSchema.type(this.type);
            return t != null ? t : GuiSchema.type("panel");
        }

        public String get(String key) {
            String v = this.props.get(key);
            if (v != null) {
                return v;
            } else {
                GuiSchema.Prop p = this.schema().prop(key);
                return p != null ? p.def() : "";
            }
        }

        public void set(String key, String value) {
            GuiSchema.Prop p = this.schema().prop(key);
            if (p != null && value.equals(p.def())) {
                this.props.remove(key);
            } else {
                this.props.put(key, value);
            }
        }

        public int i(String key) {
            return GuiDoc.toInt(this.get(key));
        }

        public float f(String key) {
            return GuiDoc.toFloat(this.get(key));
        }

        public boolean b(String key) {
            return GuiDoc.toBool(this.get(key));
        }

        public int color(String key) {
            return GuiDoc.toColor(this.get(key));
        }

        public GuiDoc.Element copy() {
            GuiDoc.Element e = new GuiDoc.Element();
            e.id = this.id;
            e.type = this.type;
            e.props = new LinkedHashMap<>(this.props);
            e.locked = this.locked;

            for (GuiDoc.Element c : this.children) {
                e.children.add(c.copy());
            }

            return e;
        }
    }
}
