package org.sprautestudio.spraute_engine.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

public final class BlockAssets {
    public static final String NS = "spraute_engine";
    private static final String[] FACINGS = new String[]{"north", "east", "south", "west"};
    private static final int[] FACING_Y = new int[]{0, 90, 180, 270};

    private BlockAssets() {
    }

    public static String picture(BlockDef d, String slot) {
        String own = switch (slot) {
            case "top" -> d.texTop;
            case "bottom" -> d.texBottom;
            case "side" -> d.texSide;
            case "front" -> d.texFront.isEmpty() ? d.texSide : d.texFront;
            default -> "";
        };
        return own.isEmpty() ? d.texture : own;
    }

    public static Map<String, String> pictures(BlockDef d, Predicate<String> exists) {
        Map<String, String> out = new LinkedHashMap<>();

        for (String slot : List.of("all", "top", "bottom", "side", "front")) {
            String p = picture(d, slot);
            if (!p.isEmpty() && exists.test(p)) {
                out.put(slot, p);
            }
        }

        return out;
    }

    public static String textureFile(BlockDef d, String slot) {
        return "assets/spraute_engine/textures/block/" + d.id + "_" + slot + ".png";
    }

    private static String look(BlockDef d) {
        String id = d.lookBlock != null && !d.lookBlock.isEmpty() ? d.lookBlock : "minecraft:stone";
        int colon = id.indexOf(58);
        return colon < 0 ? "minecraft:block/" + id : id.substring(0, colon + 1) + "block/" + id.substring(colon + 1);
    }

    private static String tex(BlockDef d, String slot, Predicate<String> exists) {
        String p = picture(d, slot);
        return !p.isEmpty() && exists.test(p) ? "spraute_engine:block/" + d.id + "_" + slot : look(d);
    }

    public static Map<String, String> assets(BlockDef d, Predicate<String> exists) {
        return assets(d, exists, null);
    }

    public static Map<String, String> assets(BlockDef d, Predicate<String> exists, JsonObject ownModel) {
        Map<String, JsonObject> models = new LinkedHashMap<>();
        String all = tex(d, "all", exists);
        String top = tex(d, "top", exists);
        String bottom = tex(d, "bottom", exists);
        String side = tex(d, "side", exists);
        String front = tex(d, "front", exists);
        String self = "spraute_engine:block/" + d.id;
        String item = self;
        JsonObject itemModel = null;
        String kind = d.kind;
        JsonObject state;
        switch (kind) {
            case "rotating":
            case "storage":
                models.put(
                    d.id,
                    ownModel != null
                        ? ownModel
                        : (
                            d.shaped()
                                ? box(d, top, bottom, side, front)
                                : model("minecraft:block/orientable_with_bottom", "front", front, "side", side, "top", top, "bottom", bottom)
                        )
                );
                state = facingVariants(self);
                break;
            case "pillar":
                models.put(d.id, model("minecraft:block/cube_column", "end", top, "side", side));
                models.put(d.id + "_horizontal", model("minecraft:block/cube_column_horizontal", "end", top, "side", side));
                state = variants();
                add(state, "axis=y", variant(self, 0, 0, false));
                add(state, "axis=z", variant(self + "_horizontal", 90, 0, false));
                add(state, "axis=x", variant(self + "_horizontal", 90, 90, false));
                break;
            case "slab":
                models.put(d.id, model("minecraft:block/slab", "bottom", bottom, "top", top, "side", side));
                models.put(d.id + "_top", model("minecraft:block/slab_top", "bottom", bottom, "top", top, "side", side));
                models.put(d.id + "_double", cube(top, bottom, side));
                state = variants();
                add(state, "type=bottom", variant(self, 0, 0, false));
                add(state, "type=top", variant(self + "_top", 0, 0, false));
                add(state, "type=double", variant(self + "_double", 0, 0, false));
                break;
            case "stairs":
                for (String m : List.of("stairs", "inner_stairs", "outer_stairs")) {
                    models.put(
                        m.equals("stairs") ? d.id : d.id + "_" + m.substring(0, 5), model("minecraft:block/" + m, "bottom", bottom, "top", top, "side", side)
                    );
                }

                state = stairs(self);
                break;
            case "wall":
                models.put(d.id + "_post", model("minecraft:block/template_wall_post", "wall", all));
                models.put(d.id + "_side", model("minecraft:block/template_wall_side", "wall", all));
                models.put(d.id + "_side_tall", model("minecraft:block/template_wall_side_tall", "wall", all));
                models.put(d.id + "_inventory", model("minecraft:block/wall_inventory", "wall", all));
                item = self + "_inventory";
                state = wall(self);
                break;
            case "fence":
                models.put(d.id + "_post", model("minecraft:block/fence_post", "texture", all));
                models.put(d.id + "_side", model("minecraft:block/fence_side", "texture", all));
                models.put(d.id + "_inventory", model("minecraft:block/fence_inventory", "texture", all));
                item = self + "_inventory";
                state = fence(self);
                break;
            case "fence_gate":
                models.put(d.id, model("minecraft:block/template_fence_gate", "texture", all));
                models.put(d.id + "_open", model("minecraft:block/template_fence_gate_open", "texture", all));
                models.put(d.id + "_wall", model("minecraft:block/template_fence_gate_wall", "texture", all));
                models.put(d.id + "_wall_open", model("minecraft:block/template_fence_gate_wall_open", "texture", all));
                state = gate(self);
                break;
            case "trapdoor":
                models.put(d.id + "_bottom", model("minecraft:block/template_orientable_trapdoor_bottom", "texture", all));
                models.put(d.id + "_top", model("minecraft:block/template_orientable_trapdoor_top", "texture", all));
                models.put(d.id + "_open", model("minecraft:block/template_orientable_trapdoor_open", "texture", all));
                item = self + "_bottom";
                state = trapdoor(self);
                break;
            case "button":
                models.put(d.id, model("minecraft:block/button", "texture", all));
                models.put(d.id + "_pressed", model("minecraft:block/button_pressed", "texture", all));
                models.put(d.id + "_inventory", model("minecraft:block/button_inventory", "texture", all));
                item = self + "_inventory";
                state = button(self);
                break;
            case "pressure_plate":
                models.put(d.id, model("minecraft:block/pressure_plate_up", "texture", all));
                models.put(d.id + "_down", model("minecraft:block/pressure_plate_down", "texture", all));
                state = variants();
                add(state, "powered=false", variant(self, 0, 0, false));
                add(state, "powered=true", variant(self + "_down", 0, 0, false));
                break;
            case "carpet":
                models.put(d.id, model("minecraft:block/carpet", "wool", all));
                state = single(self);
                break;
            case "pane":
                for (String m : List.of("post", "side", "side_alt", "noside", "noside_alt")) {
                    models.put(d.id + "_" + m, model("minecraft:block/template_glass_pane_" + m, "pane", all, "edge", side));
                }

                itemModel = model("minecraft:item/generated", "layer0", all);
                state = pane(self);
                break;
            case "plant":
                models.put(d.id, model("minecraft:block/cross", "cross", all));
                itemModel = model("minecraft:item/generated", "layer0", all);
                state = single(self);
                break;
            default:
                models.put(d.id, ownModel != null ? ownModel : (d.shaped() ? box(d, top, bottom, side, side) : cube(top, bottom, side)));
                state = single(self);
        }

        Map<String, String> out = new LinkedHashMap<>();
        out.put("assets/spraute_engine/blockstates/" + d.id + ".json", state.toString());
        models.forEach((name, json) -> out.put("assets/spraute_engine/models/block/" + name + ".json", json.toString()));
        if (itemModel == null) {
            itemModel = new JsonObject();
            itemModel.addProperty("parent", item);
        }

        out.put("assets/spraute_engine/models/item/" + d.id + ".json", itemModel.toString());
        return out;
    }

    private static JsonObject model(String parent, String... textures) {
        JsonObject m = new JsonObject();
        m.addProperty("parent", parent);
        JsonObject t = new JsonObject();

        for (int i = 0; i < textures.length; i += 2) {
            t.addProperty(textures[i], textures[i + 1]);
        }

        m.add("textures", t);
        return m;
    }

    private static JsonObject cube(String top, String bottom, String side) {
        return top.equals(bottom) && top.equals(side)
            ? model("minecraft:block/cube_all", "all", side)
            : model("minecraft:block/cube_bottom_top", "top", top, "bottom", bottom, "side", side);
    }

    private static JsonObject box(BlockDef d, String top, String bottom, String side, String front) {
        JsonObject m = model("minecraft:block/block", "particle", side, "top", top, "bottom", bottom, "side", side, "front", front);
        JsonArray elements = new JsonArray();

        for (BlockDef.Box whole : d.boxes) {
            BlockDef.Box b = new BlockDef.Box(
                Math.max(0, whole.x1), Math.max(0, whole.y1), Math.max(0, whole.z1), Math.min(16, whole.x2), Math.min(16, whole.y2), Math.min(16, whole.z2)
            );
            if (b.x2 > b.x1 && b.y2 > b.y1 && b.z2 > b.z1) {
                JsonObject e = new JsonObject();
                e.add("from", vec(b.x1, b.y1, b.z1));
                e.add("to", vec(b.x2, b.y2, b.z2));
                JsonObject faces = new JsonObject();
                faces.add("up", face("#top", b.x1, b.z1, b.x2, b.z2));
                faces.add("down", face("#bottom", b.x1, 16 - b.z2, b.x2, 16 - b.z1));
                faces.add("north", face("#front", 16 - b.x2, 16 - b.y2, 16 - b.x1, 16 - b.y1));
                faces.add("south", face("#side", b.x1, 16 - b.y2, b.x2, 16 - b.y1));
                faces.add("west", face("#side", b.z1, 16 - b.y2, b.z2, 16 - b.y1));
                faces.add("east", face("#side", 16 - b.z2, 16 - b.y2, 16 - b.z1, 16 - b.y1));
                e.add("faces", faces);
                elements.add(e);
            }
        }

        m.add("elements", elements);
        return m;
    }

    private static JsonArray vec(int... v) {
        JsonArray a = new JsonArray();

        for (int n : v) {
            a.add(n);
        }

        return a;
    }

    private static JsonObject face(String texture, int... uv) {
        JsonObject f = new JsonObject();
        f.add("uv", vec(uv));
        f.addProperty("texture", texture);
        return f;
    }

    private static JsonObject variants() {
        JsonObject s = new JsonObject();
        s.add("variants", new JsonObject());
        return s;
    }

    private static void add(JsonObject state, String key, JsonObject variant) {
        state.getAsJsonObject("variants").add(key, variant);
    }

    private static JsonObject variant(String model, int x, int y, boolean uvlock) {
        JsonObject v = new JsonObject();
        v.addProperty("model", model);
        if (x != 0) {
            v.addProperty("x", x);
        }

        if (y != 0) {
            v.addProperty("y", y);
        }

        if (uvlock) {
            v.addProperty("uvlock", true);
        }

        return v;
    }

    private static JsonObject single(String model) {
        JsonObject s = variants();
        add(s, "", variant(model, 0, 0, false));
        return s;
    }

    private static JsonObject facingVariants(String model) {
        JsonObject s = variants();

        for (int i = 0; i < 4; i++) {
            add(s, "facing=" + FACINGS[i], variant(model, 0, FACING_Y[i], false));
        }

        return s;
    }

    private static int stairY(String facing) {
        return switch (facing) {
            case "east" -> 0;
            case "south" -> 90;
            case "west" -> 180;
            default -> 270;
        };
    }

    private static JsonObject stairs(String self) {
        JsonObject s = variants();

        for (String facing : FACINGS) {
            int base = stairY(facing);

            for (String half : List.of("bottom", "top")) {
                boolean top = half.equals("top");

                for (String shape : List.of("straight", "inner_left", "inner_right", "outer_left", "outer_right")) {
                    String model = shape.equals("straight") ? self : self + "_" + shape.substring(0, 5);
                    int turn = shape.endsWith("left") ? (top ? 0 : -90) : (shape.endsWith("right") ? (top ? 90 : 0) : 0);
                    int y = Math.floorMod(base + turn, 360);
                    add(s, "facing=" + facing + ",half=" + half + ",shape=" + shape, variant(model, top ? 180 : 0, y, top || y != 0));
                }
            }
        }

        return s;
    }

    private static JsonObject part(String key, String value, JsonObject apply) {
        JsonObject p = new JsonObject();
        if (key != null) {
            JsonObject when = new JsonObject();
            when.addProperty(key, value);
            p.add("when", when);
        }

        p.add("apply", apply);
        return p;
    }

    private static JsonObject multipart(JsonArray parts) {
        JsonObject s = new JsonObject();
        s.add("multipart", parts);
        return s;
    }

    private static JsonObject wall(String self) {
        JsonArray parts = new JsonArray();
        parts.add(part("up", "true", variant(self + "_post", 0, 0, false)));

        for (int i = 0; i < 4; i++) {
            parts.add(part(FACINGS[i], "low", variant(self + "_side", 0, FACING_Y[i], true)));
            parts.add(part(FACINGS[i], "tall", variant(self + "_side_tall", 0, FACING_Y[i], true)));
        }

        return multipart(parts);
    }

    private static JsonObject fence(String self) {
        JsonArray parts = new JsonArray();
        parts.add(part(null, null, variant(self + "_post", 0, 0, false)));

        for (int i = 0; i < 4; i++) {
            parts.add(part(FACINGS[i], "true", variant(self + "_side", 0, FACING_Y[i], true)));
        }

        return multipart(parts);
    }

    private static JsonObject pane(String self) {
        JsonArray parts = new JsonArray();
        parts.add(part(null, null, variant(self + "_post", 0, 0, false)));
        parts.add(part("north", "true", variant(self + "_side", 0, 0, false)));
        parts.add(part("east", "true", variant(self + "_side", 0, 90, false)));
        parts.add(part("south", "true", variant(self + "_side_alt", 0, 0, false)));
        parts.add(part("west", "true", variant(self + "_side_alt", 0, 90, false)));
        parts.add(part("north", "false", variant(self + "_noside", 0, 0, false)));
        parts.add(part("east", "false", variant(self + "_noside_alt", 0, 0, false)));
        parts.add(part("south", "false", variant(self + "_noside_alt", 0, 90, false)));
        parts.add(part("west", "false", variant(self + "_noside", 0, 270, false)));
        return multipart(parts);
    }

    private static JsonObject gate(String self) {
        JsonObject s = variants();
        int[] y = new int[]{180, 270, 0, 90};

        for (int i = 0; i < 4; i++) {
            for (boolean wall : new boolean[]{false, true}) {
                for (boolean open : new boolean[]{false, true}) {
                    String model = self + (wall ? "_wall" : "") + (open ? "_open" : "");
                    add(s, "facing=" + FACINGS[i] + ",in_wall=" + wall + ",open=" + open, variant(model, 0, y[i], true));
                }
            }
        }

        return s;
    }

    private static JsonObject trapdoor(String self) {
        JsonObject s = variants();
        int[] topOpenY = new int[]{180, 270, 0, 90};

        for (int i = 0; i < 4; i++) {
            String f = "facing=" + FACINGS[i];
            add(s, f + ",half=bottom,open=false", variant(self + "_bottom", 0, FACING_Y[i], false));
            add(s, f + ",half=top,open=false", variant(self + "_top", 0, FACING_Y[i], false));
            add(s, f + ",half=bottom,open=true", variant(self + "_open", 0, FACING_Y[i], false));
            add(s, f + ",half=top,open=true", variant(self + "_open", 180, topOpenY[i], false));
        }

        return s;
    }

    private static JsonObject button(String self) {
        JsonObject s = variants();
        int[] ceiling = new int[]{180, 270, 0, 90};

        for (int i = 0; i < 4; i++) {
            for (boolean powered : new boolean[]{false, true}) {
                String model = powered ? self + "_pressed" : self;
                String end = ",facing=" + FACINGS[i] + ",powered=" + powered;
                add(s, "face=floor" + end, variant(model, 0, FACING_Y[i], false));
                add(s, "face=wall" + end, variant(model, 90, FACING_Y[i], true));
                add(s, "face=ceiling" + end, variant(model, 180, ceiling[i], false));
            }
        }

        return s;
    }

    public static Map<String, String> data(List<BlockDef> blocks) {
        Map<String, String> out = new LinkedHashMap<>();
        Map<String, TreeSet<String>> tags = new TreeMap<>();

        for (BlockDef d : blocks) {
            out.put("data/spraute_engine/loot_tables/blocks/" + d.id + ".json", loot(d).toString());
            String id = "spraute_engine:" + d.id;
            if (!d.tool.equals("none")) {
                tag(tags, "mineable/" + d.tool, id);
            }

            if (d.toolLevel == 1) {
                tag(tags, "needs_stone_tool", id);
            }

            if (d.toolLevel == 2) {
                tag(tags, "needs_iron_tool", id);
            }

            if (d.toolLevel >= 3) {
                tag(tags, "needs_diamond_tool", id);
            }

            String var6 = d.kind;
            switch (var6) {
                case "wall":
                    tag(tags, "walls", id);
                    break;
                case "fence":
                    tag(tags, "fences", id);
                    break;
                case "fence_gate":
                    tag(tags, "fence_gates", id);
                    break;
                case "slab":
                    tag(tags, "slabs", id);
                    break;
                case "stairs":
                    tag(tags, "stairs", id);
                    break;
                case "trapdoor":
                    tag(tags, "trapdoors", id);
                    break;
                case "button":
                    tag(tags, "buttons", id);
                    break;
                case "pressure_plate":
                    tag(tags, "pressure_plates", id);
            }
        }

        for (BlockDef d : blocks) {
            if (d.generates()) {
                ore(d, out);
            }
        }

        tags.forEach((name, ids) -> {
            JsonObject t = new JsonObject();
            t.addProperty("replace", false);
            JsonArray values = new JsonArray();
            ids.forEach(values::add);
            t.add("values", values);
            out.put("data/minecraft/tags/blocks/" + name + ".json", t.toString());
        });
        return out;
    }

    public static String oreFeature(BlockDef d) {
        return "ore_" + d.id;
    }

    private static void ore(BlockDef d, Map<String, String> out) {
        String name = oreFeature(d);
        String full = "spraute_engine:" + name;
        JsonObject config = new JsonObject();
        config.addProperty("size", d.oreSize);
        config.addProperty("discard_chance_on_air_exposure", d.oreAirDiscard);
        JsonArray targets = new JsonArray();
        String dimension = d.oreDimension;
        switch (dimension) {
            case "nether":
                targets.add(target(blockMatch("minecraft:netherrack"), d));
                break;
            case "end":
                targets.add(target(blockMatch("minecraft:end_stone"), d));
                break;
            default:
                targets.add(target(tagMatch("minecraft:stone_ore_replaceables"), d));
                targets.add(target(tagMatch("minecraft:deepslate_ore_replaceables"), d));
        }

        config.add("targets", targets);
        JsonObject configured = new JsonObject();
        configured.addProperty("type", "minecraft:ore");
        configured.add("config", config);
        out.put("data/spraute_engine/worldgen/configured_feature/" + name + ".json", configured.toString());
        JsonArray placement = new JsonArray();
        JsonObject count = new JsonObject();
        count.addProperty("type", "minecraft:count");
        count.addProperty("count", d.oreCount);
        placement.add(count);
        if (d.oreRarity > 0) {
            JsonObject rarity = new JsonObject();
            rarity.addProperty("type", "minecraft:rarity_filter");
            rarity.addProperty("chance", d.oreRarity);
            placement.add(rarity);
        }

        JsonObject square = new JsonObject();
        square.addProperty("type", "minecraft:in_square");
        placement.add(square);
        JsonObject height = new JsonObject();
        height.addProperty("type", d.oreMiddle ? "minecraft:trapezoid" : "minecraft:uniform");
        height.add("min_inclusive", absolute(d.oreMinY));
        height.add("max_inclusive", absolute(d.oreMaxY));
        JsonObject range = new JsonObject();
        range.addProperty("type", "minecraft:height_range");
        range.add("height", height);
        placement.add(range);
        JsonObject biome = new JsonObject();
        biome.addProperty("type", "minecraft:biome");
        placement.add(biome);
        JsonObject placed = new JsonObject();
        placed.addProperty("feature", full);
        placed.add("placement", placement);
        out.put("data/spraute_engine/worldgen/placed_feature/" + name + ".json", placed.toString());
        JsonObject modifier = new JsonObject();
        modifier.addProperty("type", "forge:add_features");
        modifier.addProperty("biomes", "#minecraft:is_" + d.oreDimension);
        modifier.addProperty("features", full);
        modifier.addProperty("step", "underground_ores");
        out.put("data/spraute_engine/forge/biome_modifier/" + name + ".json", modifier.toString());
    }

    private static JsonObject absolute(int y) {
        JsonObject o = new JsonObject();
        o.addProperty("absolute", y);
        return o;
    }

    private static JsonObject tagMatch(String tag) {
        JsonObject t = new JsonObject();
        t.addProperty("predicate_type", "minecraft:tag_match");
        t.addProperty("tag", tag);
        return t;
    }

    private static JsonObject blockMatch(String block) {
        JsonObject t = new JsonObject();
        t.addProperty("predicate_type", "minecraft:block_match");
        t.addProperty("block", block);
        return t;
    }

    private static JsonObject target(JsonObject rule, BlockDef d) {
        JsonObject state = new JsonObject();
        state.addProperty("Name", "spraute_engine:" + d.id);
        JsonObject t = new JsonObject();
        t.add("target", rule);
        t.add("state", state);
        return t;
    }

    private static void tag(Map<String, TreeSet<String>> tags, String name, String id) {
        tags.computeIfAbsent(name, k -> new TreeSet<>()).add(id);
    }

    static JsonObject loot(BlockDef d) {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        JsonArray pools = new JsonArray();
        if (!d.drop.equals("none")) {
            JsonObject pool = new JsonObject();
            pool.addProperty("rolls", 1);
            JsonArray entries = new JsonArray();
            JsonObject self = itemEntry("spraute_engine:" + d.id);
            if (d.kind.equals("slab")) {
                self.add("functions", slabDouble(d));
            }

            JsonObject main = d.drop.equals("item") ? otherEntry(d) : self;
            if (d.drop.equals("item") && d.dropSilk) {
                JsonObject silk = itemEntry("spraute_engine:" + d.id);
                silk.add("conditions", silkTouch());
                JsonObject choice = new JsonObject();
                choice.addProperty("type", "minecraft:alternatives");
                JsonArray both = new JsonArray();
                both.add(silk);
                both.add(main);
                choice.add("children", both);
                main = choice;
            }

            entries.add(main);
            pool.add("entries", entries);
            JsonArray conditions = new JsonArray();
            JsonObject survives = new JsonObject();
            survives.addProperty("condition", "minecraft:survives_explosion");
            conditions.add(survives);
            pool.add("conditions", conditions);
            pools.add(pool);
        }

        table.add("pools", pools);
        return table;
    }

    private static JsonObject itemEntry(String item) {
        JsonObject e = new JsonObject();
        e.addProperty("type", "minecraft:item");
        e.addProperty("name", item);
        return e;
    }

    private static JsonObject otherEntry(BlockDef d) {
        JsonObject e = itemEntry(d.dropItem);
        JsonArray functions = new JsonArray();
        if (d.dropMin != 1 || d.dropMax != 1) {
            JsonObject count = new JsonObject();
            count.addProperty("function", "minecraft:set_count");
            JsonObject range = new JsonObject();
            range.addProperty("type", "minecraft:uniform");
            range.addProperty("min", d.dropMin);
            range.addProperty("max", d.dropMax);
            count.add("count", range);
            functions.add(count);
        }

        if (d.dropFortune) {
            JsonObject bonus = new JsonObject();
            bonus.addProperty("function", "minecraft:apply_bonus");
            bonus.addProperty("enchantment", "minecraft:fortune");
            bonus.addProperty("formula", "minecraft:ore_drops");
            functions.add(bonus);
        }

        if (functions.size() > 0) {
            e.add("functions", functions);
        }

        return e;
    }

    private static JsonArray silkTouch() {
        JsonObject enchant = new JsonObject();
        enchant.addProperty("enchantment", "minecraft:silk_touch");
        JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        enchant.add("levels", levels);
        JsonArray enchants = new JsonArray();
        enchants.add(enchant);
        JsonObject predicate = new JsonObject();
        predicate.add("enchantments", enchants);
        JsonObject condition = new JsonObject();
        condition.addProperty("condition", "minecraft:match_tool");
        condition.add("predicate", predicate);
        JsonArray out = new JsonArray();
        out.add(condition);
        return out;
    }

    private static JsonArray slabDouble(BlockDef d) {
        JsonObject properties = new JsonObject();
        properties.addProperty("type", "double");
        JsonObject when = new JsonObject();
        when.addProperty("condition", "minecraft:block_state_property");
        when.addProperty("block", "spraute_engine:" + d.id);
        when.add("properties", properties);
        JsonArray conditions = new JsonArray();
        conditions.add(when);
        JsonObject count = new JsonObject();
        count.addProperty("function", "minecraft:set_count");
        count.addProperty("count", 2);
        count.add("conditions", conditions);
        JsonArray functions = new JsonArray();
        functions.add(count);
        return functions;
    }
}
