package org.sprautestudio.spraute_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.sprautestudio.spraute_engine.build.StructureDef;

public final class DungeonDef {
    public static final String SURFACE = "surface";
    public static final String UNDERGROUND = "underground";
    public static final List<String> PLACEMENTS = List.of("surface", "underground");
    public static final List<String> DIMENSIONS = List.of("overworld", "nether", "end");
    public static final List<String> ADAPTATIONS = List.of("none", "beard_thin", "beard_box", "bury");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final List<String> CONTAINERS = List.of("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel");
    public String id = "";
    public String name = "";
    public boolean enabled = true;
    public String structure = "";
    public String dimension = "overworld";
    public int spacing = 32;
    public float chance = 1.0F;
    public String placement = "surface";
    public int sink;
    public boolean onWater;
    public String adaptation = "beard_thin";
    public int minY = -40;
    public int maxY = 30;
    public List<String> biomes = new ArrayList<>();
    public String loot = "";
    public Map<String, String> chestLoot = new LinkedHashMap<>();

    public static DungeonDef create(String id, String name) {
        DungeonDef d = new DungeonDef();
        d.id = id;
        d.name = name;
        return d;
    }

    public void fix() {
        if (this.name == null) {
            this.name = "";
        }

        if (this.structure == null) {
            this.structure = "";
        }

        if (!DIMENSIONS.contains(this.dimension)) {
            this.dimension = "overworld";
        }

        if (!PLACEMENTS.contains(this.placement)) {
            this.placement = "surface";
        }

        if (!ADAPTATIONS.contains(this.adaptation)) {
            this.adaptation = "beard_thin";
        }

        this.spacing = Math.max(2, Math.min(4096, this.spacing));
        this.chance = Math.max(0.0F, Math.min(1.0F, this.chance));
        this.sink = Math.max(0, Math.min(256, this.sink));
        this.minY = Math.max(-2032, Math.min(2031, this.minY));
        this.maxY = Math.max(this.minY, Math.min(2031, this.maxY));
        if (this.biomes == null) {
            this.biomes = new ArrayList<>();
        }

        this.biomes.removeIf(b -> b == null || b.isBlank());
        if (this.loot == null) {
            this.loot = "";
        }

        if (this.chestLoot == null) {
            this.chestLoot = new LinkedHashMap<>();
        }

        this.chestLoot.values().removeIf(v -> v == null || v.isBlank());
    }

    public String problem() {
        if (!ItemDef.validId(this.id)) {
            return "Id: только латиница, цифры и _";
        } else if (this.name == null || this.name.isBlank()) {
            return "Нет названия";
        } else {
            return this.structure != null && !this.structure.isBlank() ? null : "Не выбрана постройка";
        }
    }

    public boolean underground() {
        return this.placement.equals("underground");
    }

    public String ref() {
        return "spraute_engine:dungeon/" + this.id;
    }

    public String templateFile() {
        return "data/spraute_engine/structures/dungeon/" + this.id + ".nbt";
    }

    public String lootOf(int cell) {
        String own = this.chestLoot.get(String.valueOf(cell));
        return own != null && !own.isBlank() ? own : this.loot;
    }

    private static JsonObject absolute(int y) {
        JsonObject o = new JsonObject();
        o.addProperty("absolute", y);
        return o;
    }

    private JsonObject startHeight() {
        if (!this.underground()) {
            return absolute(1 - this.sink);
        } else {
            JsonObject o = new JsonObject();
            o.addProperty("type", "minecraft:uniform");
            o.add("min_inclusive", absolute(this.minY + 1));
            o.add("max_inclusive", absolute(this.maxY + 1));
            return o;
        }
    }

    private String biomeTag() {
        String var1 = this.dimension;

        return switch (var1) {
            case "nether" -> "#minecraft:is_nether";
            case "end" -> "#minecraft:is_end";
            default -> !this.underground() && !this.onWater ? "#spraute_engine:dungeon_land" : "#minecraft:is_overworld";
        };
    }

    public static Map<String, String> landTag() {
        JsonArray values = new JsonArray();

        for (String tag : List.of("is_forest", "is_taiga", "is_jungle", "is_savanna", "is_badlands", "is_hill", "is_mountain")) {
            values.add("#minecraft:" + tag);
        }

        for (String biome : List.of(
            "plains", "sunflower_plains", "desert", "snowy_plains", "ice_spikes", "swamp", "meadow", "mangrove_swamp", "cherry_grove", "mushroom_fields"
        )) {
            JsonObject optional = new JsonObject();
            optional.addProperty("id", "minecraft:" + biome);
            optional.addProperty("required", false);
            values.add(optional);
        }

        JsonObject tag = new JsonObject();
        tag.addProperty("replace", false);
        tag.add("values", values);
        return Map.of("data/spraute_engine/tags/worldgen/biome/dungeon_land.json", tag.toString());
    }

    JsonObject structureJson() {
        JsonObject o = new JsonObject();
        o.addProperty("type", "minecraft:jigsaw");
        if (this.biomes.isEmpty()) {
            o.addProperty("biomes", this.biomeTag());
        } else {
            JsonArray list = new JsonArray();
            this.biomes.forEach(b -> list.add(b.trim()));
            o.add("biomes", list);
        }

        o.add("spawn_overrides", new JsonObject());
        o.addProperty("step", this.underground() ? "underground_structures" : "surface_structures");
        o.addProperty("terrain_adaptation", this.underground() ? "none" : this.adaptation);
        o.addProperty("start_pool", this.ref());
        o.addProperty("size", 1);
        o.add("start_height", this.startHeight());
        if (!this.underground()) {
            o.addProperty("project_start_to_heightmap", this.onWater ? "WORLD_SURFACE_WG" : "OCEAN_FLOOR_WG");
        }

        o.addProperty("max_distance_from_center", 80);
        o.addProperty("use_expansion_hack", false);
        return o;
    }

    JsonObject poolJson() {
        JsonObject element = new JsonObject();
        element.addProperty("element_type", "minecraft:single_pool_element");
        element.addProperty("location", this.ref());
        element.addProperty("processors", "minecraft:empty");
        element.addProperty("projection", "rigid");
        JsonObject entry = new JsonObject();
        entry.addProperty("weight", 1);
        entry.add("element", element);
        JsonArray elements = new JsonArray();
        elements.add(entry);
        JsonObject o = new JsonObject();
        o.addProperty("name", this.ref());
        o.addProperty("fallback", "minecraft:empty");
        o.add("elements", elements);
        return o;
    }

    JsonObject setJson() {
        JsonObject one = new JsonObject();
        one.addProperty("structure", this.ref());
        one.addProperty("weight", 1);
        JsonArray structures = new JsonArray();
        structures.add(one);
        JsonObject placed = new JsonObject();
        placed.addProperty("type", "minecraft:random_spread");
        placed.addProperty("spacing", this.spacing);
        placed.addProperty("separation", Math.max(1, this.spacing / 2));
        placed.addProperty("salt", Math.abs(this.id.hashCode() % 1000000000));
        if (this.chance < 1.0F) {
            placed.addProperty("frequency", this.chance);
        }

        JsonObject o = new JsonObject();
        o.add("structures", structures);
        o.add("placement", placed);
        return o;
    }

    public Map<String, String> worldgen() {
        Map<String, String> out = new LinkedHashMap<>();
        String base = "data/spraute_engine/worldgen/";
        out.put(base + "structure/dungeon/" + this.id + ".json", this.structureJson().toString());
        out.put(base + "template_pool/dungeon/" + this.id + ".json", this.poolJson().toString());
        out.put(base + "structure_set/dungeon/" + this.id + ".json", this.setJson().toString());
        return out;
    }

    public static List<DungeonDef.Chest> chests(StructureDef s, Predicate<String> storage) {
        List<DungeonDef.Chest> out = new ArrayList<>();
        int[] grid = s.grid();
        boolean[] container = new boolean[s.palette.size()];
        String[] names = new String[s.palette.size()];

        for (int i = 0; i < container.length; i++) {
            String block = s.finalBlock(s.palette.get(i));
            int cut = block.indexOf(91);
            names[i] = cut >= 0 ? block.substring(0, cut) : block;
            container[i] = CONTAINERS.contains(names[i]) || names[i].endsWith("shulker_box") || storage != null && storage.test(names[i]);
        }

        for (int y = 0; y < s.sizeY; y++) {
            for (int z = 0; z < s.sizeZ; z++) {
                for (int x = 0; x < s.sizeX; x++) {
                    int cell = s.index(x, y, z);
                    int c = grid[cell];
                    if (c < container.length && container[c]) {
                        out.add(new DungeonDef.Chest(cell, x, y, z, names[c]));
                    }
                }
            }
        }

        return out;
    }

    public DungeonDef copy(String newId) {
        DungeonDef d = fromJson(this.toJson());
        d.id = newId;
        return d;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static DungeonDef fromJson(String text) {
        DungeonDef d = GSON.fromJson(text, DungeonDef.class);
        if (d == null) {
            d = new DungeonDef();
        }

        d.fix();
        return d;
    }

    public record Chest(int cell, int x, int y, int z, String block) {
    }
}
