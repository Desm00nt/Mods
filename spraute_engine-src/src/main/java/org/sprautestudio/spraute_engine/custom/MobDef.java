package org.sprautestudio.spraute_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MobDef {
    public static final String PASSIVE = "passive";
    public static final String NEUTRAL = "neutral";
    public static final String HOSTILE = "hostile";
    public static final List<String> BEHAVIOURS = List.of("passive", "neutral", "hostile");
    public static final List<String> DIMENSIONS = List.of("overworld", "nether", "end");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String id = "";
    public String name = "";
    public String look = "";
    public String behaviour = "neutral";
    public float health = 20.0F;
    public float damage = 3.0F;
    public float speed = 1.0F;
    public float followRange = 16.0F;
    public float armor;
    public float knockbackResistance;
    public int xp = 5;
    public String attackAnim = "";
    public String ambientSound = "";
    public String hurtSound = "";
    public String deathSound = "";
    public String loot = "";
    public boolean spawns;
    public String dimension = "overworld";
    public List<String> biomes = new ArrayList<>();
    public int weight = 10;
    public int groupMin = 1;
    public int groupMax = 3;
    public String spawnCommand = "";
    public String deathCommand = "";

    public static MobDef create(String id, String name) {
        MobDef d = new MobDef();
        d.id = id;
        d.name = name;
        return d;
    }

    public static String ref(String name) {
        return name.contains(":") ? name : "spraute_engine:mob/" + name;
    }

    public String ref() {
        return ref(this.id);
    }

    public boolean hostile() {
        return "hostile".equals(this.behaviour);
    }

    public boolean passive() {
        return "passive".equals(this.behaviour);
    }

    public void fix() {
        if (this.name == null) {
            this.name = "";
        }

        if (this.look == null) {
            this.look = "";
        }

        if (!BEHAVIOURS.contains(this.behaviour)) {
            this.behaviour = "neutral";
        }

        this.health = Math.max(0.5F, Math.min(10000.0F, this.health));
        this.damage = Math.max(0.0F, Math.min(10000.0F, this.damage));
        this.speed = Math.max(0.0F, Math.min(20.0F, this.speed));
        this.followRange = Math.max(1.0F, Math.min(512.0F, this.followRange));
        this.armor = Math.max(0.0F, Math.min(30.0F, this.armor));
        this.knockbackResistance = Math.max(0.0F, Math.min(1.0F, this.knockbackResistance));
        this.xp = Math.max(0, Math.min(100000, this.xp));
        if (this.attackAnim == null) {
            this.attackAnim = "";
        }

        if (this.ambientSound == null) {
            this.ambientSound = "";
        }

        if (this.hurtSound == null) {
            this.hurtSound = "";
        }

        if (this.deathSound == null) {
            this.deathSound = "";
        }

        if (this.loot == null) {
            this.loot = "";
        }

        if (!DIMENSIONS.contains(this.dimension)) {
            this.dimension = "overworld";
        }

        if (this.biomes == null) {
            this.biomes = new ArrayList<>();
        }

        this.biomes.removeIf(b -> b == null || b.isBlank());
        this.weight = Math.max(1, Math.min(1000, this.weight));
        this.groupMin = Math.max(1, Math.min(64, this.groupMin));
        this.groupMax = Math.max(this.groupMin, Math.min(64, this.groupMax));
        if (this.spawnCommand == null) {
            this.spawnCommand = "";
        }

        if (this.deathCommand == null) {
            this.deathCommand = "";
        }
    }

    public String problem() {
        if (!ItemDef.validId(this.id)) {
            return "Id: только латиница, цифры и _";
        } else {
            return this.name != null && !this.name.isBlank() ? null : "Нет названия";
        }
    }

    public String restartKey() {
        this.fix();
        return this.behaviour + "|" + this.spawns + "|" + this.dimension + "|" + this.biomes + "|" + this.weight + "|" + this.groupMin + "|" + this.groupMax;
    }

    public Map<String, String> forgeData() {
        Map<String, String> out = new LinkedHashMap<>();
        if (!this.spawns) {
            return out;
        } else {
            JsonObject spawner = new JsonObject();
            spawner.addProperty("type", this.ref());
            spawner.addProperty("weight", this.weight);
            spawner.addProperty("minCount", this.groupMin);
            spawner.addProperty("maxCount", this.groupMax);
            JsonObject o = new JsonObject();
            o.addProperty("type", "forge:add_spawns");
            if (this.biomes.isEmpty()) {
                o.addProperty("biomes", "#minecraft:is_" + this.dimension);
            } else {
                JsonArray list = new JsonArray();
                this.biomes.forEach(b -> list.add(b.trim()));
                o.add("biomes", list);
            }

            o.add("spawners", spawner);
            out.put("data/spraute_engine/forge/biome_modifier/mob_" + this.id + ".json", o.toString());
            return out;
        }
    }

    public MobDef copy(String newId) {
        MobDef d = fromJson(this.toJson());
        d.id = newId;
        return d;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static MobDef fromJson(String text) {
        MobDef d = GSON.fromJson(text, MobDef.class);
        if (d == null) {
            d = new MobDef();
        }

        d.fix();
        return d;
    }
}
