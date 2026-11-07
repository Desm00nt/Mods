package org.sprautestudio.spraute_engine.editor.visual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class VisualScript {
    public int version = 2;
    public List<VisualScript.Stack> stacks = new ArrayList<>();
    public double viewX = 0.0;
    public double viewY = 0.0;
    public double zoom = 1.0;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String CLIP_START = "clip_start";
    public static final String GENERAL_MODE = "общий: с запуска игры";

    public static VisualScript fromJson(String json) {
        VisualScript s = null;

        try {
            s = GSON.fromJson(json, VisualScript.class);
        } catch (RuntimeException var4) {
        }

        if (s == null) {
            s = new VisualScript();
        }

        if (s.stacks == null) {
            s.stacks = new ArrayList<>();
        }

        for (VisualScript.Stack st : s.stacks) {
            if (st.blocks == null) {
                st.blocks = new ArrayList<>();
            }

            st.dirty = true;
            fix(st.blocks);
        }

        if (s.zoom <= 0.0) {
            s.zoom = 1.0;
        }

        return s;
    }

    private static void fix(List<VisualScript.Block> blocks) {
        blocks.removeIf(bx -> bx == null || bx.type == null || BlockTypes.get(bx.type) == null);

        for (VisualScript.Block b : blocks) {
            if (b.fields == null) {
                b.fields = new LinkedHashMap<>();
            }

            if (b.options == null) {
                b.options = new ArrayList<>();
            }

            if (b.slots == null) {
                b.slots = new ArrayList<>();
            }

            if (b.sockets == null) {
                b.sockets = new ArrayList<>();
            }

            if (b.inputs == null) {
                b.inputs = new LinkedHashMap<>();
            }

            b.inputs.values().removeIf(r -> r == null || r.type == null || BlockTypes.get(r.type) == null);
            fix(new ArrayList<>(b.inputs.values()));
            int need = b.type.equals("choice") ? b.options.size() : (b.type.equals("switch") ? b.options.size() + 1 : b.typeInfo().slotLabels().size());

            while (b.slots.size() < need) {
                b.slots.add(new ArrayList<>());
            }

            for (List<VisualScript.Block> slot : b.slots) {
                fix(slot);
            }
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public VisualScript.Stack mainStack() {
        for (VisualScript.Stack s : this.stacks) {
            if (!s.blocks.isEmpty()) {
                String head = s.blocks.get(0).type;
                if (head.equals("start") || head.equals("clip_start")) {
                    return s;
                }
            }
        }

        return null;
    }

    public static VisualScript createClip() {
        VisualScript s = new VisualScript();
        VisualScript.Stack st = new VisualScript.Stack();
        st.x = 30.0;
        st.y = 30.0;
        st.blocks.add(VisualScript.Block.create("clip_start"));
        s.stacks.add(st);
        return s;
    }

    public static VisualScript createGeneral(int id, String title) {
        VisualScript s = new VisualScript();
        VisualScript.Stack st = new VisualScript.Stack();
        st.x = 40.0;
        st.y = 40.0;
        VisualScript.Block start = VisualScript.Block.create("start");
        start.fields.put("id", String.valueOf(id));
        start.fields.put("title", title);
        start.fields.put("auto", "общий: с запуска игры");
        st.blocks.add(start);
        s.stacks.add(st);
        return s;
    }

    public static VisualScript create(int id, String title) {
        VisualScript s = new VisualScript();
        VisualScript.Stack st = new VisualScript.Stack();
        st.x = 40.0;
        st.y = 40.0;
        VisualScript.Block start = VisualScript.Block.create("start");
        start.fields.put("id", String.valueOf(id));
        start.fields.put("title", title);
        st.blocks.add(start);
        VisualScript.Block ch = VisualScript.Block.create("character");
        st.blocks.add(ch);
        VisualScript.Block say = VisualScript.Block.create("cmd:say");
        say.fields.put("who", "alice");
        say.fields.put("text", "Привет!");
        st.blocks.add(say);
        s.stacks.add(st);
        return s;
    }

    public static final class Block {
        public String type;
        public Map<String, String> fields = new LinkedHashMap<>();
        public List<List<VisualScript.Block>> slots = new ArrayList<>();
        public List<String> options = new ArrayList<>();
        public Map<String, VisualScript.Block> inputs = new LinkedHashMap<>();
        public List<String> sockets = new ArrayList<>();
        public boolean collapsed;
        public boolean oneLine;
        transient int lx;
        transient int ly;
        transient int lw;
        transient int lh;
        transient int headerH;
        transient int[] slotTop;
        transient int[] slotH;
        transient int[] partX;
        transient int[] partW;
        transient int[] partY;
        transient int[] partRH;
        transient boolean error;
        transient String errorText;

        public BlockTypes.Type typeInfo() {
            return BlockTypes.get(this.type);
        }

        public String field(String name) {
            String v = this.fields.get(name);
            return v == null ? "" : v;
        }

        public static VisualScript.Block create(String type) {
            VisualScript.Block b = new VisualScript.Block();
            b.type = type;
            BlockTypes.Type t = BlockTypes.get(type);
            if (t != null) {
                for (BlockTypes.Field f : t.fields()) {
                    if (f.def() != null && !f.def().isEmpty()) {
                        b.fields.put(f.name(), f.def());
                    }
                }

                if (type.equals("choice")) {
                    b.options.add("Да");
                    b.slots.add(new ArrayList<>());
                } else if (type.equals("switch")) {
                    b.options.add("\"yes\"");
                    b.options.add("\"no\"");
                    b.slots.add(new ArrayList<>());
                    b.slots.add(new ArrayList<>());
                    b.slots.add(new ArrayList<>());
                } else {
                    for (int i = 0; i < t.slotLabels().size(); i++) {
                        b.slots.add(new ArrayList<>());
                    }
                }
            }

            return b;
        }

        public VisualScript.Block deepCopy() {
            VisualScript.Block b = new VisualScript.Block();
            b.type = this.type;
            b.fields = new LinkedHashMap<>(this.fields);
            b.options = new ArrayList<>(this.options);
            b.sockets = new ArrayList<>(this.sockets);
            b.collapsed = this.collapsed;
            b.oneLine = this.oneLine;

            for (Entry<String, VisualScript.Block> e : this.inputs.entrySet()) {
                b.inputs.put(e.getKey(), e.getValue().deepCopy());
            }

            for (List<VisualScript.Block> slot : this.slots) {
                List<VisualScript.Block> copy = new ArrayList<>();

                for (VisualScript.Block c : slot) {
                    copy.add(c.deepCopy());
                }

                b.slots.add(copy);
            }

            return b;
        }
    }

    public static final class Stack {
        public double x;
        public double y;
        public List<VisualScript.Block> blocks = new ArrayList<>();
        transient boolean dirty = true;
        transient int bw;
        transient int bh;
    }
}
