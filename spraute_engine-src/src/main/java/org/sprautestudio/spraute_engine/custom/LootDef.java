package org.sprautestudio.spraute_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LootDef {
    public static final List<String> REPLACEABLE = List.of(
        "",
        "minecraft:chests/simple_dungeon",
        "minecraft:chests/abandoned_mineshaft",
        "minecraft:chests/desert_pyramid",
        "minecraft:chests/jungle_temple",
        "minecraft:chests/stronghold_corridor",
        "minecraft:chests/igloo_chest",
        "minecraft:chests/woodland_mansion",
        "minecraft:chests/pillager_outpost",
        "minecraft:chests/shipwreck_treasure",
        "minecraft:chests/buried_treasure",
        "minecraft:chests/nether_bridge",
        "minecraft:chests/bastion_treasure",
        "minecraft:chests/end_city_treasure",
        "minecraft:chests/spawn_bonus_chest",
        "minecraft:chests/village/village_weaponsmith"
    );
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String id = "";
    public String name = "";
    public String replaces = "";
    public List<LootDef.Pool> pools = new ArrayList<>();

    public static LootDef create(String id, String name) {
        LootDef d = new LootDef();
        d.id = id;
        d.name = name;
        LootDef.Pool p = new LootDef.Pool();
        p.entries.add(new LootDef.Entry());
        d.pools.add(p);
        return d;
    }

    public static String ref(String name) {
        return name.contains(":") ? name : "spraute_engine:loot/" + name;
    }

    public String ref() {
        return ref(this.id);
    }

    public void fix() {
        if (this.name == null) {
            this.name = "";
        }

        if (this.replaces == null || !this.replaces.matches("([a-z0-9_.-]+:[a-z0-9_./-]+)?")) {
            this.replaces = "";
        }

        if (this.pools == null) {
            this.pools = new ArrayList<>();
        }

        for (LootDef.Pool p : this.pools) {
            if (p.entries == null) {
                p.entries = new ArrayList<>();
            }

            p.rollsMin = clamp(p.rollsMin, 0, 256);
            p.rollsMax = clamp(p.rollsMax, p.rollsMin, 256);

            for (LootDef.Entry e : p.entries) {
                if (e.item == null) {
                    e.item = "";
                }

                e.item = e.item.trim();
                e.weight = clamp(e.weight, 1, 100000);
                e.countMin = clamp(e.countMin, 1, 64);
                e.countMax = clamp(e.countMax, e.countMin, 64);
            }
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public String problem() {
        if (!ItemDef.validId(this.id)) {
            return "Id: только латиница, цифры и _";
        } else if (this.name == null || this.name.isBlank()) {
            return "Нет названия";
        } else if (this.pools != null && !this.pools.isEmpty()) {
            for (LootDef.Pool p : this.pools) {
                if (p.entries == null || p.entries.isEmpty()) {
                    return "В группе нет ни одного предмета";
                }

                for (LootDef.Entry e : p.entries) {
                    if (e.item != null && !e.item.isBlank() && !e.item.trim().matches("([a-z0-9_.-]+:)?[a-z0-9_./-]+")) {
                        return "Неверный предмет: " + e.item;
                    }
                }
            }

            return null;
        } else {
            return "Нет ни одной группы";
        }
    }

    private static JsonObject range(int min, int max) {
        JsonObject r = new JsonObject();
        r.addProperty("type", "minecraft:uniform");
        r.addProperty("min", min);
        r.addProperty("max", max);
        return r;
    }

    public JsonObject table() {
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:chest");
        JsonArray poolList = new JsonArray();

        for (LootDef.Pool p : this.pools) {
            JsonObject pool = new JsonObject();
            if (p.rollsMin == p.rollsMax) {
                pool.addProperty("rolls", p.rollsMin);
            } else {
                pool.add("rolls", range(p.rollsMin, p.rollsMax));
            }

            JsonArray entries = new JsonArray();

            for (LootDef.Entry e : p.entries) {
                entries.add(entry(e));
            }

            pool.add("entries", entries);
            poolList.add(pool);
        }

        table.add("pools", poolList);
        return table;
    }

    private static JsonObject entry(LootDef.Entry e) {
        JsonObject o = new JsonObject();
        if (e.item.isBlank()) {
            o.addProperty("type", "minecraft:empty");
            o.addProperty("weight", e.weight);
            return o;
        } else {
            o.addProperty("type", "minecraft:item");
            o.addProperty("name", e.item.contains(":") ? e.item : "minecraft:" + e.item);
            o.addProperty("weight", e.weight);
            JsonArray functions = new JsonArray();
            if (e.countMin != 1 || e.countMax != 1) {
                JsonObject count = new JsonObject();
                count.addProperty("function", "minecraft:set_count");
                if (e.countMin == e.countMax) {
                    count.addProperty("count", e.countMin);
                } else {
                    count.add("count", range(e.countMin, e.countMax));
                }

                functions.add(count);
            }

            if (e.enchant) {
                JsonObject enchant = new JsonObject();
                enchant.addProperty("function", "minecraft:enchant_randomly");
                functions.add(enchant);
            }

            if (functions.size() > 0) {
                o.add("functions", functions);
            }

            return o;
        }
    }

    public static Map<String, String> data(List<LootDef> tables) {
        Map<String, String> out = new LinkedHashMap<>();

        for (LootDef d : tables) {
            if (d.problem() == null) {
                d.fix();
                String text = d.table().toString();
                out.putIfAbsent("data/spraute_engine/loot_tables/loot/" + d.id + ".json", text);
                if (!d.replaces.isEmpty()) {
                    int colon = d.replaces.indexOf(58);
                    out.putIfAbsent("data/" + d.replaces.substring(0, colon) + "/loot_tables/" + d.replaces.substring(colon + 1) + ".json", text);
                }
            }
        }

        return out;
    }

    public LootDef copy(String newId) {
        LootDef d = fromJson(this.toJson());
        d.id = newId;
        return d;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static LootDef fromJson(String text) {
        LootDef d = GSON.fromJson(text, LootDef.class);
        if (d == null) {
            d = new LootDef();
        }

        d.fix();
        return d;
    }

    public static final class Entry {
        public String item = "minecraft:bread";
        public int weight = 1;
        public int countMin = 1;
        public int countMax = 1;
        public boolean enchant;
    }

    public static final class Pool {
        public int rollsMin = 1;
        public int rollsMax = 3;
        public List<LootDef.Entry> entries = new ArrayList<>();
    }
}
