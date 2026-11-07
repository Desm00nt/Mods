package org.sprautestudio.spraute_engine.editor.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.gui.PanelLook;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Parser;

public final class WorldScene {
    public static final float PANEL_WIDTH = 3.0F;
    public static final float PANEL_WORN_WIDTH = 1.5F;
    private static final double NPC_HEIGHT = 1.9;
    private static final Set<String> GOES_ON = Set.of(
        "spawn",
        "tp",
        "despawn",
        "look",
        "stoplook",
        "name",
        "skin",
        "hitbox",
        "namecolor",
        "walkspeed",
        "hold",
        "health",
        "anim",
        "face",
        "stopanim",
        "clearanims",
        "moveanim",
        "resetanim",
        "sit",
        "stand",
        "wander",
        "follow",
        "stopwalk",
        "clearbehavior",
        "fx play",
        "fx stop",
        "fx clear",
        "gui place",
        "gui attach",
        "gui remove",
        "blocks stop",
        "blocks shake",
        "blocks wave",
        "build stop",
        "slot lock",
        "slot unlock",
        "slot size",
        "slot reset",
        "slot order",
        "slot icon",
        "rule deny",
        "rule allow",
        "rule set",
        "rule reset",
        "camera rig",
        "camera rigset",
        "camera rigoff",
        "post play",
        "post set",
        "post pulse",
        "post clear",
        "particles",
        "zone on",
        "zone off",
        "zone wall",
        "flag set",
        "flag clear",
        "global set",
        "global add",
        "global clear",
        "pvar set",
        "pvar add",
        "pvar clear",
        "time set",
        "time add",
        "time speed",
        "time freeze",
        "time unfreeze",
        "speaker",
        "clearchat",
        "message",
        "actionbar",
        "notify",
        "broadcast",
        "sound",
        "stopsound",
        "effect",
        "give",
        "enable",
        "disable",
        "setblock",
        "summon",
        "spawnitem",
        "log",
        "emit",
        "orb spawn",
        "orb clear",
        "quest start",
        "quest track",
        "overlay show",
        "overlay hide",
        "bossbar show",
        "bossbar hide",
        "tag add",
        "tag remove",
        "team join",
        "team leave",
        "team color",
        "team pvp",
        "checkpoint",
        "setprop",
        "fly",
        "refuse",
        "free"
    );
    private static final Set<String> PLAIN_BLOCKS = Set.of(
        "start", "character", "location", "var", "label", "set", "change", "comment", "var:set", "npc:look", "npc:gesture", "engine:toggle", "engine:on"
    );
    private static final String YES = "да";

    private WorldScene() {
    }

    public static boolean goesOn(String command) {
        return GOES_ON.contains(command);
    }

    public static List<WorldScene.Item> ofText(String source) {
        Ast.Program p = Parser.parse(source).program;
        return scan(p, source);
    }

    private static List<WorldScene.Item> scan(Ast.Program p, String source) {
        Map<String, WorldScene.Item> npcs = new LinkedHashMap<>();
        List<WorldScene.Item> effects = new ArrayList<>();

        for (Ast.Stmt s : p.body) {
            if (s instanceof Ast.CommandStmt c) {
                if (c.key.equals("build place")) {
                    WorldScene.Item it = textItem(p, source, c, WorldScene.Kind.BUILD);
                    it.name = c.arg("name") instanceof Ast.StringLit n ? n.value : "";
                    if (c.arg("style") instanceof Ast.StringLit st) {
                        it.style = st.value;
                        it.styleStart = st.start;
                        it.styleEnd = st.end;
                    }

                    if (c.arg("rotate") instanceof Ast.NumberLit ro) {
                        it.rotate = (int)ro.value;
                        it.rotateStart = ro.start;
                        it.rotateEnd = ro.end;
                    }

                    effects.add(it);
                    if (c.arg("nowait") == null) {
                        break;
                    }
                } else {
                    if (!GOES_ON.contains(c.key)) {
                        break;
                    }

                    switch (c.key) {
                        case "spawn":
                        case "tp": {
                            WorldScene.Item it = textItem(p, source, c, WorldScene.Kind.NPC);
                            it.name = nameOf(c.arg("who"));
                            it.type = typeOf(p, it.name);
                            if (c.key.equals("spawn")) {
                                readScale(it, c);
                            }

                            npcs.put(it.name, it);
                            break;
                        }
                        case "despawn":
                            npcs.remove(nameOf(c.arg("who")));
                            break;
                        case "gui place":
                        case "gui attach": {
                            WorldScene.Item it = textItem(p, source, c, WorldScene.Kind.PANEL);
                            it.name = c.arg("name") instanceof Ast.StringLit n ? n.value : "";
                            it.scale = c.key.equals("gui attach") ? 1.5F : 3.0F;
                            readScale(it, c);
                            readPanelArgs(it, c, source);
                            if (c.arg("pitch") instanceof Ast.NumberLit pi) {
                                it.pitch = (float)pi.value;
                                it.pitchStart = pi.start;
                                it.pitchEnd = pi.end;
                            }

                            it.face = c.arg("face") != null;
                            effects.add(it);
                            break;
                        }
                        case "fx play": {
                            WorldScene.Item it = textItem(p, source, c, WorldScene.Kind.EFFECT);
                            it.name = c.arg("name") instanceof Ast.StringLit n ? n.value : "";
                            readScale(it, c);
                            effects.add(it);
                        }
                    }
                }
            } else if (!(s instanceof Ast.SetStmt)
                && !(s instanceof Ast.OnStmt)
                && !(s instanceof Ast.LabelStmt)
                && !(s instanceof Ast.MethodStmt)
                && !(s instanceof Ast.EveryStmt)) {
                break;
            }
        }

        hangPanels(effects, npcs);
        List<WorldScene.Item> out = new ArrayList<>(npcs.values());
        out.addAll(effects);

        for (Ast.LocationDecl l : p.locations) {
            WorldScene.Item itx = new WorldScene.Item();
            itx.kind = WorldScene.Kind.POINT;
            itx.name = l.name;
            itx.point = l.name;
            itx.command = "location";
            itx.stmtStart = l.start;
            itx.stmtEnd = l.end;
            fillPlace(itx, l.pos, l, source);
            out.add(itx);
        }

        return out;
    }

    private static void readPanelArgs(WorldScene.Item it, Ast.CommandStmt c, String source) {
        if (c.arg("pos") != null) {
            it.rawPosStart = c.arg("pos").start;
            it.rawPosEnd = c.arg("pos").end;
        }

        List<String> keys = new ArrayList<>(PanelLook.VALUES);
        keys.addAll(List.of("who", "bone", "offset", "yaw"));

        for (String key : keys) {
            Ast.Expr e = c.arg(key);
            if (e != null) {
                it.spans.put(key, new int[]{e.start, e.end});
                it.args
                    .put(
                        key,
                        e instanceof Ast.StringLit s
                            ? s.value
                            : (e instanceof Ast.NumberLit n ? number(n.value) : source.substring(e.start, Math.min(source.length(), e.end)))
                    );
            }
        }

        for (String keyx : flagKeys()) {
            if (c.arg(keyx) != null) {
                it.args.put(keyx, "да");
            }
        }

        it.wearer = it.arg("who");
    }

    private static List<String> flagKeys() {
        List<String> flags = new ArrayList<>(PanelLook.FLAGS);
        flags.add("self");
        return flags;
    }

    private static boolean wordArg(String key) {
        return key.equals("turn") || key.equals("look") || key.equals("color") || key.equals("bone");
    }

    private static void hangPanels(List<WorldScene.Item> panels, Map<String, WorldScene.Item> npcs) {
        for (WorldScene.Item it : panels) {
            if (it.kind == WorldScene.Kind.PANEL && it.worn()) {
                WorldScene.Item npc = npcs.get(it.wearer);
                it.placed = false;
                it.computed = "прицеплен к " + it.wearer;
                float ownYaw = 0.0F;

                try {
                    if (!it.arg("yaw").isBlank()) {
                        ownYaw = Float.parseFloat(it.arg("yaw"));
                    }
                } catch (NumberFormatException var9) {
                }

                it.yaw = ownYaw;
                if (npc != null && npc.placed) {
                    double[] written = new double[]{0.0, 0.0, 0.0};

                    try {
                        if (!it.arg("offset").isBlank() && coordinates(Parser.parseExpression(it.arg("offset"))) != null) {
                            written = coordinates(Parser.parseExpression(it.arg("offset")));
                        }
                    } catch (RuntimeException var8) {
                    }

                    it.anchorX = npc.x;
                    it.anchorY = npc.y + 1.9 * npc.scale;
                    it.anchorZ = npc.z;
                    it.anchorYaw = npc.yaw;
                    double[] step = PanelLook.offset(npc.yaw, written[0], written[1], written[2]);
                    it.x = it.anchorX + step[0];
                    it.y = it.anchorY + step[1];
                    it.z = it.anchorZ + step[2];
                    it.placed = true;
                }
            }
        }
    }

    public static String withArg(String source, WorldScene.Item it, String key, String value) {
        boolean flag = flagKeys().contains(key);
        String stmt = source.substring(it.stmtStart, it.stmtEnd);
        if (!flag) {
            String written = wordArg(key) ? "\"" + value + "\"" : value;
            int[] span = it.spans.get(key);
            if (span == null) {
                return value.isBlank() ? source : source.substring(0, it.stmtEnd).stripTrailing() + " " + key + " " + written + source.substring(it.stmtEnd);
            } else if (!value.isBlank()) {
                return source.substring(0, span[0]) + written + source.substring(span[1]);
            } else {
                String before = source.substring(0, span[0]).stripTrailing();
                return before.substring(0, before.length() - key.length()).stripTrailing() + source.substring(span[1]);
            }
        } else {
            boolean want = !value.isBlank() && !value.equals("нет");
            if (want == it.args.containsKey(key)) {
                return source;
            } else {
                return want
                    ? source.substring(0, it.stmtEnd).stripTrailing() + " " + key + source.substring(it.stmtEnd)
                    : source.substring(0, it.stmtStart) + stmt.replaceFirst("\\s+" + key + "\\b", "") + source.substring(it.stmtEnd);
            }
        }
    }

    public static void argBlock(WorldScene.Item it, String key, String value) {
        if (it.block != null) {
            it.block.fields.put(key, value);
        }
    }

    public static String withWearer(String source, WorldScene.Item it, String who, double x, double y, double z) {
        if (!who.isEmpty()) {
            if (it.worn()) {
                int[] whoAt = it.spans.get("who");
                return source.substring(0, whoAt[0]) + who + source.substring(whoAt[1]);
            } else {
                String before = source.substring(it.stmtStart, it.rawPosStart).replaceFirst("^gui place", "gui attach").replaceFirst("\\s+at\\s*$", " to ");
                return source.substring(0, it.stmtStart) + before + who + source.substring(it.rawPosEnd);
            }
        } else if (!it.worn()) {
            return source;
        } else {
            List<String> gone = new ArrayList<>(List.of("bone", "offset", "yaw"));
            gone.removeIf(key -> !it.spans.containsKey(key));
            gone.sort((a, b) -> Integer.compare(it.spans.get(b)[0], it.spans.get(a)[0]));
            String tail = source.substring(it.stmtEnd);
            String head = source.substring(0, it.stmtEnd);

            for (String key : gone) {
                head = cutArg(head, it.spans.get(key), key);
            }

            int[] whoAt = it.spans.get("who");
            String before = head.substring(it.stmtStart, whoAt[0]).replaceFirst("^gui attach", "gui place").replaceFirst("\\s+to\\s*$", " at ");
            String after = head.substring(whoAt[1]).replaceFirst("\\s+self\\b", "");
            return head.substring(0, it.stmtStart) + before + placeTurned(x, y, z, it.yaw) + after + tail;
        }
    }

    private static String cutArg(String text, int[] span, String key) {
        String before = text.substring(0, span[0]).stripTrailing();
        return before.substring(0, before.length() - key.length()).stripTrailing() + text.substring(span[1]);
    }

    public static void wearerBlock(WorldScene.Item it, String who, double x, double y, double z) {
        if (it.block != null) {
            VisualScript.Block b = it.block;
            if (!who.isEmpty()) {
                if (!it.worn()) {
                    b.type = "cmd:gui attach";
                    b.fields.remove("pos");
                    b.inputs.remove("pos");
                }

                b.fields.put("who", who);
            } else if (it.worn()) {
                b.type = "cmd:gui place";

                for (String key : List.of("who", "bone", "offset", "yaw", "self")) {
                    b.fields.remove(key);
                    b.inputs.remove(key);
                }

                b.fields.put("pos", placeTurned(x, y, z, it.yaw));
            }
        }
    }

    private static void readScale(WorldScene.Item it, Ast.CommandStmt c) {
        it.sizable = true;
        if (c.arg("scale") instanceof Ast.NumberLit sc) {
            it.scale = (float)sc.value;
            it.scaleStart = sc.start;
            it.scaleEnd = sc.end;
        }
    }

    private static WorldScene.Item textItem(Ast.Program p, String source, Ast.CommandStmt c, WorldScene.Kind kind) {
        WorldScene.Item it = new WorldScene.Item();
        it.kind = kind;
        it.command = c.key;
        it.stmtStart = c.start;
        it.stmtEnd = c.end;
        Ast.Expr pos = c.arg("pos");
        Ast.LocationDecl decl = null;
        if (pos instanceof Ast.Ident id) {
            for (Ast.LocationDecl l : p.locations) {
                if (l.name.equals(id.name)) {
                    it.point = l.name;
                    pos = l.pos;
                    decl = l;
                }
            }
        }

        fillPlace(it, pos, decl, source);
        return it;
    }

    private static void fillPlace(WorldScene.Item it, Ast.Expr pos, Ast.LocationDecl decl, String source) {
        double[] at = coordinates(pos);
        if (at != null) {
            Ast.Tuple t = (Ast.Tuple)pos;
            it.placed = true;
            it.x = at[0];
            it.y = at[1];
            it.z = at[2];
            it.coordStart = t.items.get(0).start;
            it.coordEnd = t.items.get(2).end;
            Ast.Expr written = decl != null ? decl.yaw : (t.items.size() > 3 ? t.items.get(3) : null);
            it.yawAfter = decl != null ? t.end : it.coordEnd;
            if (written instanceof Ast.NumberLit n) {
                it.yaw = (float)n.value;
                it.yawStart = n.start;
                it.yawEnd = n.end;
                it.hasYaw = true;
            }
        } else if (pos != null) {
            it.computed = source.substring(pos.start, Math.min(source.length(), pos.end));
        }
    }

    private static String nameOf(Ast.Expr e) {
        return e instanceof Ast.Ident id ? id.name : "";
    }

    private static String typeOf(Ast.Program p, String character) {
        for (Ast.CharacterDecl d : p.characters) {
            if (d.name.equals(character) && d.type instanceof Ast.StringLit s) {
                return s.value;
            }
        }

        return "";
    }

    private static double[] coordinates(Ast.Expr e) {
        if (e instanceof Ast.Tuple t && t.items.size() >= 3) {
            double[] out = new double[3];

            for (int i = 0; i < 3; i++) {
                if (!(t.items.get(i) instanceof Ast.NumberLit n)) {
                    return null;
                }

                out[i] = n.value;
            }

            return out;
        } else {
            return null;
        }
    }

    public static String moved(String source, WorldScene.Item it, double x, double y, double z) {
        if (!it.placed) {
            return source;
        } else {
            return it.worn()
                ? withArg(source, it, "offset", wornOffset(it, x, y, z))
                : source.substring(0, it.coordStart) + number(x) + ", " + number(y) + ", " + number(z) + source.substring(it.coordEnd);
        }
    }

    public static String turned(String source, WorldScene.Item it, double yaw) {
        if (!it.placed) {
            return source;
        } else if (it.worn()) {
            return withArg(source, it, "yaw", number(yaw));
        } else if (it.yawStart >= 0) {
            return source.substring(0, it.yawStart) + number(yaw) + source.substring(it.yawEnd);
        } else {
            String added = it.point.isEmpty() ? ", " + number(yaw) : " yaw " + number(yaw);
            return source.substring(0, it.yawAfter) + added + source.substring(it.yawAfter);
        }
    }

    public static String withPitch(String source, WorldScene.Item it, double pitch) {
        return it.pitchStart >= 0
            ? source.substring(0, it.pitchStart) + number(pitch) + source.substring(it.pitchEnd)
            : source.substring(0, it.stmtEnd).stripTrailing() + " pitch " + number(pitch) + source.substring(it.stmtEnd);
    }

    public static String withFace(String source, WorldScene.Item it, boolean face) {
        String stmt = source.substring(it.stmtStart, it.stmtEnd);
        if (face == it.face) {
            return source;
        } else {
            return face
                ? source.substring(0, it.stmtEnd).stripTrailing() + " face" + source.substring(it.stmtEnd)
                : source.substring(0, it.stmtStart) + stmt.replaceFirst("\\s+face\\b", "") + source.substring(it.stmtEnd);
        }
    }

    public static void pitchBlock(WorldScene.Item it, double pitch) {
        if (it.block != null) {
            it.block.fields.put("pitch", number(pitch));
        }
    }

    public static void faceBlock(WorldScene.Item it, boolean face) {
        if (it.block != null) {
            it.block.fields.put("face", face ? "да" : "");
        }
    }

    public static WorldScene.Item panelAt(String source, WorldScene.Item it) {
        for (WorldScene.Item other : ofText(source)) {
            if (other.kind == WorldScene.Kind.PANEL && other.stmtStart == it.stmtStart) {
                return other;
            }
        }

        return it;
    }

    private static String wornOffset(WorldScene.Item it, double x, double y, double z) {
        double[] step = PanelLook.unoffset(it.anchorYaw, x - it.anchorX, y - it.anchorY, z - it.anchorZ);
        return place(step[0], step[1], step[2]);
    }

    public static String panelLine(String name, double x, double y, double z, double yaw) {
        return "gui place \"" + name + "\" at " + placeTurned(x, y, z, yaw);
    }

    public static VisualScript.Block panelBlock(String name, double x, double y, double z, double yaw) {
        VisualScript.Block b = VisualScript.Block.create("cmd:gui place");
        b.fields.put("name", name);
        b.fields.put("pos", placeTurned(x, y, z, yaw));
        return b;
    }

    public static String withStyle(String source, WorldScene.Item it, String style) {
        String quoted = "\"" + style + "\"";
        return it.styleStart >= 0
            ? source.substring(0, it.styleStart) + quoted + source.substring(it.styleEnd)
            : source.substring(0, it.stmtEnd).stripTrailing() + " style " + quoted + source.substring(it.stmtEnd);
    }

    public static String withRotate(String source, WorldScene.Item it, int rotate) {
        return it.rotateStart >= 0
            ? source.substring(0, it.rotateStart) + rotate + source.substring(it.rotateEnd)
            : source.substring(0, it.stmtEnd).stripTrailing() + " rotate " + rotate + source.substring(it.stmtEnd);
    }

    public static void styleBlock(WorldScene.Item it, String style) {
        if (it.block != null) {
            it.block.fields.put("style", style);
        }
    }

    public static void rotateBlock(WorldScene.Item it, int rotate) {
        if (it.block != null) {
            it.block.fields.put("rotate", String.valueOf(rotate));
        }
    }

    public static String buildLine(String name, double x, double y, double z) {
        return "build place \"" + name + "\" at " + place(x, y, z);
    }

    public static VisualScript.Block buildBlock(String name, double x, double y, double z) {
        VisualScript.Block b = VisualScript.Block.create("cmd:build place");
        b.fields.put("name", name);
        b.fields.put("pos", place(x, y, z));
        return b;
    }

    public static String scaled(String source, WorldScene.Item it, double scale) {
        return it.scaleStart >= 0
            ? source.substring(0, it.scaleStart) + number(scale) + source.substring(it.scaleEnd)
            : source.substring(0, it.stmtEnd).stripTrailing() + " scale " + number(scale) + source.substring(it.stmtEnd);
    }

    public static String removed(String source, WorldScene.Item it) {
        int from = source.lastIndexOf(10, Math.max(0, it.stmtStart - 1)) + 1;
        int to = source.indexOf(10, Math.max(from, it.stmtEnd - 1));
        return source.substring(0, from) + (to < 0 ? "" : source.substring(to + 1));
    }

    public static String inserted(String source, String line) {
        Ast.Program p = Parser.parse(source).program;

        for (Ast.Stmt s : p.body) {
            boolean goesOn = s instanceof Ast.CommandStmt c
                ? GOES_ON.contains(c.key)
                : s instanceof Ast.SetStmt
                    || s instanceof Ast.OnStmt
                    || s instanceof Ast.LabelStmt
                    || s instanceof Ast.MethodStmt
                    || s instanceof Ast.EveryStmt;
            if (!goesOn) {
                int from = source.lastIndexOf(10, Math.max(0, s.start - 1)) + 1;
                return source.substring(0, from) + source.substring(from, s.start).replaceAll("\\S", " ") + line + "\n" + source.substring(from);
            }
        }

        return source + (!source.isEmpty() && !source.endsWith("\n") ? "\n" : "") + line + "\n";
    }

    public static String spawnLine(String character, double x, double y, double z) {
        return "spawn " + character + " at " + place(x, y, z);
    }

    public static String effectLine(String effect, double x, double y, double z) {
        return "fx play \"" + effect + "\" at " + place(x, y, z);
    }

    public static String place(double x, double y, double z) {
        return "(" + number(x) + ", " + number(y) + ", " + number(z) + ")";
    }

    private static String number(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == Math.rint(r) ? String.valueOf((long)r) : String.valueOf(r);
    }

    private static void eachBlock(List<VisualScript.Block> list, BiConsumer<VisualScript.Block, List<VisualScript.Block>> visit) {
        for (VisualScript.Block b : list) {
            visit.accept(b, list);

            for (List<VisualScript.Block> slot : b.slots) {
                eachBlock(slot, visit);
            }
        }
    }

    private static void eachBlock(VisualScript script, BiConsumer<VisualScript.Block, List<VisualScript.Block>> visit) {
        for (VisualScript.Stack s : script.stacks) {
            eachBlock(s.blocks, visit);
        }
    }

    private static String commandOf(VisualScript.Block b) {
        if (b.type.startsWith("cmd:")) {
            return b.type.substring(4);
        } else {
            String mode = b.field("mode").trim();
            if (b.type.equals("npc:spawn")) {
                return mode.equals("исчезает") ? "despawn" : "spawn";
            } else if (b.type.equals("npc:go")) {
                return switch (mode) {
                    case "появляется у" -> "spawn";
                    case "телепортируется к" -> "tp";
                    case "следует за" -> "follow";
                    default -> "move";
                };
            } else {
                return null;
            }
        }
    }

    private static boolean blockGoesOn(VisualScript.Block b) {
        String command = commandOf(b);
        return command != null ? GOES_ON.contains(command) : PLAIN_BLOCKS.contains(b.type);
    }

    public static List<WorldScene.Item> ofBlocks(VisualScript script) {
        VisualScript.Stack main = script.mainStack();
        Map<String, WorldScene.Item> npcs = new LinkedHashMap<>();
        List<WorldScene.Item> out = new ArrayList<>();
        List<WorldScene.Item> effects = new ArrayList<>();
        Map<String, VisualScript.Block> points = new LinkedHashMap<>();
        Map<String, String> types = new LinkedHashMap<>();
        List<WorldScene.Item> pointItems = new ArrayList<>();
        eachBlock(script, (bx, list) -> {
            if (bx.type.equals("character")) {
                types.put(bx.field("name").trim(), unquote(bx.field("type")));
            }

            if (bx.type.equals("location")) {
                points.put(bx.field("name").trim(), bx);
                WorldScene.Item itx = new WorldScene.Item();
                itx.kind = WorldScene.Kind.POINT;
                itx.command = "location";
                itx.name = bx.field("name").trim();
                itx.point = itx.name;
                itx.block = bx;
                itx.list = list;
                itx.holder = bx;
                readBlockPlace(itx, bx.field("pos").trim());
                pointItems.add(itx);
            }
        });
        if (main != null) {
            for (VisualScript.Block b : main.blocks) {
                if (b.type.equals("cmd:build place")) {
                    WorldScene.Item it = blockItem(main, b, WorldScene.Kind.BUILD, "build place", points);
                    it.name = unquote(b.field("name"));
                    it.style = unquote(b.field("style"));

                    try {
                        if (!b.field("rotate").isBlank()) {
                            it.rotate = Integer.parseInt(b.field("rotate").trim());
                        }
                    } catch (NumberFormatException var19) {
                    }

                    effects.add(it);
                    if (b.field("nowait").isBlank() || b.field("nowait").equals("нет")) {
                        break;
                    }
                } else {
                    if (!blockGoesOn(b)) {
                        break;
                    }

                    String command = commandOf(b);
                    if (command != null) {
                        switch (command) {
                            case "spawn":
                            case "tp": {
                                WorldScene.Item it = blockItem(main, b, WorldScene.Kind.NPC, command, points);
                                it.name = b.field("who").trim();
                                it.type = types.getOrDefault(it.name, "");
                                if (command.equals("spawn")
                                    && (b.type.equals("cmd:spawn") || b.type.equals("npc:spawn") && b.field("mode").trim().equals("появляется у"))) {
                                    readBlockScale(it, b);
                                }

                                npcs.put(it.name, it);
                                break;
                            }
                            case "despawn":
                                npcs.remove(b.field("who").trim());
                                break;
                            case "gui place":
                            case "gui attach": {
                                WorldScene.Item it = blockItem(main, b, WorldScene.Kind.PANEL, command, points);
                                it.name = unquote(b.field("name"));
                                it.scale = command.equals("gui attach") ? 1.5F : 3.0F;
                                readBlockScale(it, b);
                                List<String> keys = new ArrayList<>(PanelLook.VALUES);
                                keys.addAll(List.of("bone", "offset", "yaw"));
                                keys.addAll(flagKeys());

                                for (String key : keys) {
                                    String v = unquote(b.field(key));
                                    if (!v.isEmpty() && !v.equals("нет")) {
                                        it.args.put(key, v);
                                    }
                                }

                                if (command.equals("gui attach")) {
                                    it.wearer = b.field("who").trim();
                                }

                                try {
                                    if (!b.field("pitch").isBlank()) {
                                        it.pitch = Float.parseFloat(b.field("pitch").trim());
                                    }
                                } catch (NumberFormatException var18) {
                                }

                                String face = b.field("face").trim();
                                it.face = !face.isEmpty() && !face.equals("нет");
                                effects.add(it);
                                break;
                            }
                            case "fx play": {
                                WorldScene.Item it = blockItem(main, b, WorldScene.Kind.EFFECT, command, points);
                                it.name = unquote(b.field("name"));
                                readBlockScale(it, b);
                                effects.add(it);
                            }
                        }
                    }
                }
            }
        }

        hangPanels(effects, npcs);
        out.addAll(npcs.values());
        out.addAll(effects);
        out.addAll(pointItems);
        return out;
    }

    private static void readBlockScale(WorldScene.Item it, VisualScript.Block b) {
        it.sizable = true;

        try {
            if (!b.field("scale").isBlank()) {
                it.scale = Float.parseFloat(b.field("scale").trim());
            }
        } catch (NumberFormatException var3) {
        }
    }

    private static WorldScene.Item blockItem(
        VisualScript.Stack main, VisualScript.Block b, WorldScene.Kind kind, String command, Map<String, VisualScript.Block> points
    ) {
        WorldScene.Item it = new WorldScene.Item();
        it.kind = kind;
        it.command = command;
        it.block = b;
        it.list = main.blocks;
        it.holder = b;
        it.posField = b.type.startsWith("npc:") ? "target" : "pos";
        if (b.type.equals("npc:spawn") && !b.field("mode").trim().equals("появляется у")) {
            it.computed = b.field("mode").trim();
            return it;
        } else if (b.inputs.containsKey(it.posField)) {
            it.computed = "блок в поле";
            return it;
        } else {
            String text = b.field(it.posField).trim();
            VisualScript.Block point = points.get(text);
            if (point != null) {
                it.point = text;
                it.holder = point;
                it.posField = "pos";
                text = point.field("pos").trim();
            }

            readBlockPlace(it, text);
            return it;
        }
    }

    private static void readBlockPlace(WorldScene.Item it, String text) {
        double[] at = null;

        try {
            Ast.Expr parsed = Parser.parseExpression(text);
            at = coordinates(parsed);
            if (at != null && it.point.isEmpty() && ((Ast.Tuple)parsed).items.size() > 3 && ((Ast.Tuple)parsed).items.get(3) instanceof Ast.NumberLit n) {
                it.yaw = (float)n.value;
                it.hasYaw = true;
            }

            if (at != null && !it.point.isEmpty() && !it.holder.field("yaw").isBlank()) {
                it.yaw = Float.parseFloat(it.holder.field("yaw").trim());
                it.hasYaw = true;
            }
        } catch (RuntimeException var6) {
        }

        if (at == null) {
            it.computed = text;
        } else {
            it.placed = true;
            it.x = at[0];
            it.y = at[1];
            it.z = at[2];
        }
    }

    private static String unquote(String s) {
        String t = s.trim();
        return t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"") ? t.substring(1, t.length() - 1) : t;
    }

    public static void moveBlock(WorldScene.Item it, double x, double y, double z) {
        if (it.placed && it.holder != null) {
            if (it.worn()) {
                it.holder.fields.put("offset", wornOffset(it, x, y, z));
            } else {
                it.holder.fields.put(it.posField, it.hasYaw && it.point.isEmpty() ? placeTurned(x, y, z, it.yaw) : place(x, y, z));
            }
        }
    }

    public static void turnBlock(WorldScene.Item it, double yaw) {
        if (it.placed && it.holder != null) {
            if (it.worn()) {
                it.holder.fields.put("yaw", number(yaw));
            } else {
                if (it.point.isEmpty()) {
                    it.holder.fields.put(it.posField, placeTurned(it.x, it.y, it.z, yaw));
                } else {
                    it.holder.fields.put("yaw", number(yaw));
                }
            }
        }
    }

    public static void scaleBlock(WorldScene.Item it, double scale) {
        if (it.block != null) {
            it.block.fields.put("scale", number(scale));
        }
    }

    private static String placeTurned(double x, double y, double z, double yaw) {
        return "(" + number(x) + ", " + number(y) + ", " + number(z) + ", " + number(yaw) + ")";
    }

    public static void removeBlock(WorldScene.Item it) {
        if (it.list != null) {
            it.list.remove(it.block);
        }
    }

    public static void insertBlock(VisualScript script, VisualScript.Block block) {
        VisualScript.Stack main = script.mainStack();
        if (main == null) {
            throw new IllegalStateException("Нет начального блока сюжета");
        } else {
            int at = main.blocks.size();

            for (int i = 0; i < main.blocks.size(); i++) {
                if (!blockGoesOn(main.blocks.get(i))) {
                    at = i;
                    break;
                }
            }

            main.blocks.add(at, block);
        }
    }

    public static VisualScript.Block spawnBlock(String character, double x, double y, double z) {
        VisualScript.Block b = VisualScript.Block.create("cmd:spawn");
        b.fields.put("who", character);
        b.fields.put("pos", place(x, y, z));
        return b;
    }

    public static VisualScript.Block effectBlock(String effect, double x, double y, double z) {
        VisualScript.Block b = VisualScript.Block.create("cmd:fx play");
        b.fields.put("name", effect);
        b.fields.put("pos", place(x, y, z));
        return b;
    }

    public static List<String> charactersOfText(String source) {
        List<String> out = new ArrayList<>();

        for (Ast.CharacterDecl d : Parser.parse(source).program.characters) {
            out.add(d.name);
        }

        return out;
    }

    public static List<String> charactersOfBlocks(VisualScript script) {
        List<String> out = new ArrayList<>();
        eachBlock(script, (b, list) -> {
            if (b.type.equals("character")) {
                out.add(b.field("name").trim());
            }
        });
        return out;
    }

    public static String characterLine(String name) {
        return "character " + name + " = \"spraute:npc\" name \"" + name + "\"";
    }

    public static String pointLine(String name, double x, double y, double z) {
        return "location " + name + " = " + place(x, y, z);
    }

    public static VisualScript.Block characterBlock(String name) {
        VisualScript.Block b = VisualScript.Block.create("character");
        b.fields.put("name", name);
        b.fields.put("type", "spraute:npc");
        b.fields.put("display", name);
        return b;
    }

    public static VisualScript.Block pointBlock(String name, double x, double y, double z) {
        VisualScript.Block b = VisualScript.Block.create("location");
        b.fields.put("name", name);
        b.fields.put("pos", place(x, y, z));
        return b;
    }

    public static final class Item {
        public WorldScene.Kind kind;
        public String name = "";
        public String command = "";
        public String type = "";
        public float scale = 1.0F;
        public boolean placed;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public boolean face;
        int pitchStart = -1;
        int pitchEnd;
        public String wearer = "";
        public final Map<String, String> args = new LinkedHashMap<>();
        final Map<String, int[]> spans = new LinkedHashMap<>();
        int rawPosStart = -1;
        int rawPosEnd;
        double anchorX;
        double anchorY;
        double anchorZ;
        public float anchorYaw;
        public String style = "";
        public int rotate;
        int styleStart = -1;
        int styleEnd;
        int rotateStart = -1;
        int rotateEnd;
        public boolean sizable;
        String posField = "pos";
        public String point = "";
        public String computed = "";
        int coordStart;
        int coordEnd;
        int yawStart = -1;
        int yawEnd;
        int yawAfter;
        int scaleStart = -1;
        int scaleEnd;
        boolean hasYaw;
        public int stmtStart;
        public int stmtEnd;
        VisualScript.Block holder;
        VisualScript.Block block;
        List<VisualScript.Block> list;

        public boolean worn() {
            return !this.wearer.isEmpty();
        }

        public String arg(String key) {
            return this.args.getOrDefault(key, "");
        }

        public String turn() {
            String own = PanelLook.of(this.args::get).turn();
            return !own.isEmpty() ? own : (!this.face && !this.worn() ? "fixed" : "viewer");
        }

        public PanelLook look() {
            return PanelLook.of(this.args::get);
        }
    }

    public static enum Kind {
        NPC,
        EFFECT,
        POINT,
        BUILD,
        PANEL;
    }
}
