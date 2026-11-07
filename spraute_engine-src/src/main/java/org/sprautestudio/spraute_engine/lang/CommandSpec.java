package org.sprautestudio.spraute_engine.lang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CommandSpec {
    public final String key;
    public final String head;
    public final String sub;
    public final String category;
    public final String syntax;
    public final String doc;
    public final List<CommandSpec.Group> groups;

    public CommandSpec(String key, String category, String syntax, String doc) {
        this.key = key;
        int sp = key.indexOf(32);
        this.head = sp < 0 ? key : key.substring(0, sp);
        this.sub = sp < 0 ? null : key.substring(sp + 1);
        this.category = category;
        this.syntax = syntax;
        this.doc = doc;
        this.groups = Collections.unmodifiableList(parseSyntax(syntax));
    }

    private static List<CommandSpec.Group> parseSyntax(String syntax) {
        List<CommandSpec.Group> groups = new ArrayList<>();
        CommandSpec.Group current = null;
        boolean inOptional = false;

        for (String raw : syntax.trim().isEmpty() ? new String[0] : syntax.trim().split("\\s+")) {
            String w = raw;
            if (raw.startsWith("[")) {
                inOptional = true;
                current = new CommandSpec.Group(null, true);
                groups.add(current);
                w = raw.substring(1);
                if (w.isEmpty()) {
                    continue;
                }
            }

            boolean closes = w.endsWith("]");
            if (closes) {
                w = w.substring(0, w.length() - 1);
            }

            if (w.startsWith("<")) {
                String inner = w.substring(1, w.length() - 1);
                boolean vararg = inner.endsWith("...");
                if (vararg) {
                    inner = inner.substring(0, inner.length() - 3);
                }

                String[] nt = inner.split(":");
                CommandSpec.Param p = new CommandSpec.Param(nt[0], ParamType.byName(nt[1]), vararg);
                if (current == null || !inOptional && current.keyword == null && !current.params.isEmpty()) {
                    current = new CommandSpec.Group(null, false);
                    groups.add(current);
                }

                current.params.add(p);
                if (!inOptional && current.keyword == null) {
                    current = null;
                }
            } else if (!w.isEmpty()) {
                if (inOptional && current != null && current.keyword == null && current.params.isEmpty()) {
                    groups.remove(current);
                    current = new CommandSpec.Group(w, true);
                    groups.add(current);
                } else {
                    current = new CommandSpec.Group(w, inOptional);
                    groups.add(current);
                }
            }

            if (closes) {
                inOptional = false;
                current = null;
            }
        }

        return groups;
    }

    public List<CommandSpec.Group> flags() {
        List<CommandSpec.Group> list = new ArrayList<>();

        for (CommandSpec.Group g : this.groups) {
            if (g.isFlag()) {
                list.add(g);
            }
        }

        return list;
    }

    public List<CommandSpec.Param> allParams() {
        List<CommandSpec.Param> list = new ArrayList<>();

        for (CommandSpec.Group g : this.groups) {
            list.addAll(g.params);
        }

        return list;
    }

    public CommandSpec.Param param(String name) {
        for (CommandSpec.Group g : this.groups) {
            for (CommandSpec.Param p : g.params) {
                if (p.name.equals(name)) {
                    return p;
                }
            }
        }

        return null;
    }

    public CommandSpec.Group groupOf(String paramName) {
        for (CommandSpec.Group g : this.groups) {
            for (CommandSpec.Param p : g.params) {
                if (p.name.equals(paramName)) {
                    return g;
                }
            }
        }

        return null;
    }

    public String signature() {
        StringBuilder sb = new StringBuilder(this.key);

        for (CommandSpec.Group g : this.groups) {
            sb.append(' ');
            if (g.optional) {
                sb.append('[');
            }

            if (g.keyword != null) {
                sb.append(g.keyword);
            }

            for (int i = 0; i < g.params.size(); i++) {
                CommandSpec.Param p = g.params.get(i);
                if (g.keyword != null || i > 0) {
                    sb.append(' ');
                }

                sb.append('<').append(p.name).append(p.vararg ? "..." : "").append('>');
            }

            if (g.optional) {
                sb.append(']');
            }
        }

        return sb.toString();
    }

    public String snippet() {
        StringBuilder sb = new StringBuilder(this.key);
        int n = 1;

        for (CommandSpec.Group g : this.groups) {
            if (!g.optional) {
                sb.append(' ');
                if (g.keyword != null) {
                    sb.append(g.keyword).append(' ');
                }

                for (int i = 0; i < g.params.size(); i++) {
                    CommandSpec.Param p = g.params.get(i);
                    if (i > 0) {
                        sb.append(' ');
                    }
                    String ph = switch (p.type) {
                        case TEXT, SOUND, ITEM, EFFECT, ENTITY, ANIM, GUI, GUIEL, IMAGE -> "\"" + p.name + "\"";
                        case DURATION -> "1s";
                        case LOC -> "(0, 64, 0)";
                        default -> p.name;
                    };
                    if (p.type != ParamType.TEXT
                        && p.type != ParamType.SOUND
                        && p.type != ParamType.ITEM
                        && p.type != ParamType.EFFECT
                        && p.type != ParamType.ENTITY
                        && p.type != ParamType.ANIM
                        && p.type != ParamType.GUI
                        && p.type != ParamType.GUIEL
                        && p.type != ParamType.IMAGE
                        && p.type != ParamType.CUTSCENE
                        && p.type != ParamType.VFX
                        && p.type != ParamType.POST
                        && p.type != ParamType.CAMRIG
                        && p.type != ParamType.STRUCTURE
                        && p.type != ParamType.BUILDSTYLE
                        && p.type != ParamType.STEALTHSHAPE
                        && p.type != ParamType.STEALTH
                        && p.type != ParamType.SKILLTREE
                        && p.type != ParamType.SKILLNODE
                        && p.type != ParamType.SKILLMENU
                        && p.type != ParamType.PANELTURN
                        && p.type != ParamType.PANELLOOK
                        && p.type != ParamType.SLOTORDER
                        && p.type != ParamType.SLOTSET
                        && p.type != ParamType.PLAYERCAN
                        && p.type != ParamType.PLAYERSTAT
                        && p.type != ParamType.PLAYERRULE
                        && p.type != ParamType.SHOP
                        && p.type != ParamType.QUEST
                        && p.type != ParamType.TASK
                        && p.type != ParamType.ZONE
                        && p.type != ParamType.FIGHT
                        && p.type != ParamType.PATH) {
                        sb.append("${").append(n++).append(':').append(ph).append('}');
                    } else {
                        sb.append("\"${").append(n++).append(':').append(p.name).append("}\"");
                    }
                }
            }
        }

        return sb.toString();
    }

    public static final class Group {
        public final String keyword;
        public final boolean optional;
        public final List<CommandSpec.Param> params = new ArrayList<>();

        Group(String keyword, boolean optional) {
            this.keyword = keyword;
            this.optional = optional;
        }

        public boolean isFlag() {
            return this.keyword != null && this.params.isEmpty();
        }
    }

    public static final class Param {
        public final String name;
        public final ParamType type;
        public final boolean vararg;

        Param(String name, ParamType type, boolean vararg) {
            this.name = name;
            this.type = type;
            this.vararg = vararg;
        }
    }
}
