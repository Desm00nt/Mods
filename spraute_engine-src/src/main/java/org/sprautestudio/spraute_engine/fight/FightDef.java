package org.sprautestudio.spraute_engine.fight;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FightDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String title = "Боец";
    public float health = 20.0F;
    public float armor = 0.0F;
    public float speed = 1.3F;
    public float giveUp = 40.0F;
    public String idle = "";
    public boolean stanceMoving = false;
    public List<FightDef.Attack> attacks = new ArrayList<>();
    public FightDef.Defense block = new FightDef.Defense();
    public FightDef.Defense dodge = new FightDef.Defense();
    public String hurt = "";
    public float staggerChance = 0.4F;
    public String defeat = "";
    public String defeatMode = "knockout";
    public float fleeAt = 0.0F;
    public boolean bossBar = false;
    public String bossName = "";
    public String hitSound = "minecraft:entity.player.attack.strong";
    public String weapon = "";
    public float distance = 0.0F;
    public float tooClose = 0.0F;
    public FightDef.Cover cover = new FightDef.Cover();
    public FightDef.Heal heal = new FightDef.Heal();
    public static final String MELEE = "melee";
    public static final String RANGED = "ranged";
    public static final List<String> KINDS = List.of("melee", "ranged");
    public static final List<String> PROJECTILES = List.of(
        "minecraft:arrow",
        "minecraft:spectral_arrow",
        "minecraft:small_fireball",
        "minecraft:snowball",
        "minecraft:egg",
        "minecraft:trident",
        "minecraft:wither_skull",
        "minecraft:llama_spit",
        "minecraft:shulker_bullet"
    );
    public static final Map<String, String> PRESET_TITLES = new LinkedHashMap<>();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static FightDef fromJson(String json) {
        FightDef d = null;

        try {
            d = GSON.fromJson(json, FightDef.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new FightDef();
        }

        d.fix();
        return d;
    }

    public FightDef copy() {
        return fromJson(this.toJson());
    }

    public void fix() {
        if (this.title == null) {
            this.title = "";
        }

        if (this.idle == null) {
            this.idle = "";
        }

        if (this.hurt == null) {
            this.hurt = "";
        }

        if (this.defeat == null) {
            this.defeat = "";
        }

        if (this.defeatMode == null) {
            this.defeatMode = "knockout";
        }

        if (this.bossName == null) {
            this.bossName = "";
        }

        if (this.hitSound == null) {
            this.hitSound = "";
        }

        if (this.attacks == null) {
            this.attacks = new ArrayList<>();
        }

        if (this.block == null) {
            this.block = new FightDef.Defense();
        }

        if (this.dodge == null) {
            this.dodge = new FightDef.Defense();
        }

        if (this.block.anim == null) {
            this.block.anim = "";
        }

        if (this.dodge.anim == null) {
            this.dodge.anim = "";
        }

        if (this.weapon == null) {
            this.weapon = "";
        }

        if (this.cover == null) {
            this.cover = new FightDef.Cover();
        }

        if (this.heal == null) {
            this.heal = new FightDef.Heal();
        }

        if (this.cover.anim == null) {
            this.cover.anim = "";
        }

        if (this.heal.anim == null) {
            this.heal.anim = "";
        }

        if (this.heal.sound == null) {
            this.heal.sound = "";
        }

        this.distance = Math.max(0.0F, this.distance);
        this.tooClose = Math.max(0.0F, this.tooClose);
        this.cover.radius = Math.max(3.0F, Math.min(32.0F, this.cover.radius));
        this.cover.time = Math.max(10, this.cover.time);
        this.heal.charges = Math.max(0, this.heal.charges);
        this.health = Math.max(1.0F, this.health);
        this.armor = Math.max(0.0F, Math.min(0.95F, this.armor));

        for (FightDef.Attack a : this.attacks) {
            if (a.name == null) {
                a.name = "Удар";
            }

            if (a.anim == null) {
                a.anim = "";
            }

            if (a.sound == null) {
                a.sound = "";
            }

            if (a.release == null) {
                a.release = "";
            }

            a.weight = Math.max(1, a.weight);
            a.range = Math.max(0.5F, a.range);
            if (!KINDS.contains(a.kind)) {
                a.kind = "melee";
            }

            if (a.projectile == null || a.projectile.isBlank()) {
                a.projectile = "minecraft:arrow";
            }

            a.shotSpeed = Math.max(0.1F, Math.min(6.0F, a.shotSpeed));
            a.spread = Math.max(0.0F, Math.min(45.0F, a.spread));
            a.shots = Math.max(1, Math.min(16, a.shots));
        }

        if (this.attacks.isEmpty()) {
            this.attacks.add(new FightDef.Attack("Удар", "", 2.0F, 2.2F, 0, 12, 1));
        }
    }

    public static FightDef preset(String name) {
        FightDef d = new FightDef();
        String var2 = name == null ? "" : name;
        switch (var2) {
            case "fists":
                d.title = "Кулачный бой";
                d.idle = "entity/classic!fight_idle";
                d.attacks.add(new FightDef.Attack("Левой", "entity/classic!punch_left", 2.0F, 2.2F, 0, 10, 3));
                d.attacks.add(new FightDef.Attack("Правой", "entity/classic!punch_right", 2.5F, 2.2F, 0, 10, 3));
                FightDef.Attack two = new FightDef.Attack("Двойной", "entity/classic!punch_combo", 4.0F, 2.3F, 0, 18, 1);
                two.knockback = 0.8F;
                d.attacks.add(two);
                d.block.anim = "entity/classic!block_hit";
                d.block.chance = 0.25F;
                d.block.power = 0.8F;
                d.dodge.anim = "entity/classic!dodge_left";
                d.dodge.chance = 0.1F;
                d.dodge.power = 2.0F;
                d.hurt = "entity/classic!hit_front";
                d.defeat = "entity/classic!knockdown";
                break;
            case "sword":
                d.title = "Мечник";
                d.health = 30.0F;
                d.armor = 0.15F;
                d.idle = "entity/classic!fight_idle_sword";
                d.attacks.add(new FightDef.Attack("Взмах", "entity/classic!slash_right", 5.0F, 2.8F, 0, 14, 3));
                FightDef.Attack heavy = new FightDef.Attack("Сильный удар", "entity/classic!slash_right", 8.0F, 3.0F, 0, 24, 1);
                heavy.knockback = 1.0F;
                heavy.lunge = 1.0F;
                d.attacks.add(heavy);
                d.block.anim = "entity/classic!block_hit";
                d.block.chance = 0.35F;
                d.block.power = 0.9F;
                d.dodge.anim = "entity/classic!dodge_left";
                d.dodge.chance = 0.1F;
                d.dodge.power = 2.5F;
                d.hurt = "entity/classic!hit_front";
                d.staggerChance = 0.25F;
                d.defeat = "entity/classic!knockdown";
                d.hitSound = "minecraft:entity.player.attack.sweep";
                break;
            case "brute":
                d.title = "Громила";
                d.health = 45.0F;
                d.armor = 0.25F;
                d.speed = 1.05F;
                d.idle = "entity/classic!fight_idle";
                FightDef.Attack swing = new FightDef.Attack("Размах", "entity/classic!slash_right", 6.0F, 2.6F, 0, 20, 2);
                swing.knockback = 1.2F;
                d.attacks.add(swing);
                FightDef.Attack spin = new FightDef.Attack("С разворота", "entity/classic!slash_right", 9.0F, 2.8F, 0, 30, 1);
                spin.knockback = 1.6F;
                d.attacks.add(spin);
                d.hurt = "entity/classic!hit_front";
                d.staggerChance = 0.1F;
                d.defeat = "entity/classic!knockdown";
                break;
            case "weak":
                d.title = "Слабак";
                d.health = 8.0F;
                d.speed = 1.15F;
                d.idle = "entity/classic!fight_idle";
                d.attacks.add(new FightDef.Attack("Тычок", "entity/classic!punch_right", 1.0F, 2.0F, 0, 18, 1));
                d.dodge.anim = "entity/classic!dodge_left";
                d.dodge.chance = 0.15F;
                d.dodge.power = 2.0F;
                d.hurt = "entity/classic!hit_front";
                d.staggerChance = 0.8F;
                d.fleeAt = 0.25F;
                d.defeat = "entity/classic!knockdown";
                break;
            case "archer":
                d.title = "Лучник";
                d.health = 20.0F;
                d.speed = 1.25F;
                d.idle = "entity/classic!fight_idle";
                FightDef.Attack shot = new FightDef.Attack("Выстрел", "entity/humanoid!bow_draw_right", 4.0F, 18.0F, 18, 30, 3);
                shot.release = "entity/humanoid!bow_release_right";
                shot.duration = 28;
                shot.kind = "ranged";
                shot.projectile = "minecraft:arrow";
                shot.shotSpeed = 1.6F;
                shot.spread = 3.0F;
                shot.sound = "minecraft:entity.skeleton.shoot";
                d.attacks.add(shot);
                FightDef.Attack kick = new FightDef.Attack("Пинок", "entity/classic!punch_right", 2.0F, 2.2F, 0, 16, 1);
                kick.knockback = 1.2F;
                d.attacks.add(kick);
                d.weapon = "minecraft:bow";
                d.distance = 9.0F;
                d.tooClose = 4.0F;
                d.dodge.anim = "entity/classic!dodge_left";
                d.dodge.chance = 0.15F;
                d.dodge.power = 2.0F;
                d.hurt = "entity/classic!hit_front";
                d.cover.chance = 0.2F;
                d.cover.time = 50;
                d.heal.at = 0.35F;
                d.heal.amount = 8.0F;
                d.heal.charges = 1;
                d.heal.time = 30;
                d.defeat = "entity/classic!knockdown";
                break;
            case "mage":
                d.title = "Маг";
                d.health = 24.0F;
                d.speed = 1.1F;
                d.idle = "entity/classic!fight_idle";
                FightDef.Attack bolt = new FightDef.Attack("Огненный шар", "", 5.0F, 20.0F, 12, 34, 2);
                bolt.kind = "ranged";
                bolt.projectile = "minecraft:small_fireball";
                bolt.shotSpeed = 1.2F;
                bolt.spread = 1.0F;
                bolt.sound = "minecraft:entity.blaze.shoot";
                d.attacks.add(bolt);
                FightDef.Attack volley = new FightDef.Attack("Залп", "", 3.0F, 16.0F, 12, 60, 1);
                volley.kind = "ranged";
                volley.projectile = "minecraft:snowball";
                volley.shotSpeed = 1.4F;
                volley.spread = 10.0F;
                volley.shots = 3;
                d.attacks.add(volley);
                d.distance = 11.0F;
                d.tooClose = 5.0F;
                d.hurt = "entity/classic!hit_front";
                d.staggerChance = 0.5F;
                d.cover.at = 0.5F;
                d.cover.time = 60;
                d.heal.at = 0.4F;
                d.heal.amount = 10.0F;
                d.heal.charges = 2;
                d.heal.time = 40;
                d.heal.sound = "minecraft:block.amethyst_block.chime";
                d.defeat = "entity/classic!knockdown";
                break;
            default:
                return null;
        }

        d.fix();
        return d;
    }

    public static FightDef example(String title) {
        FightDef d = preset("fists");
        d.title = title;
        return d;
    }

    static {
        PRESET_TITLES.put("fists", "Кулачный бой");
        PRESET_TITLES.put("sword", "Мечник");
        PRESET_TITLES.put("brute", "Громила");
        PRESET_TITLES.put("weak", "Слабак");
        PRESET_TITLES.put("archer", "Лучник");
        PRESET_TITLES.put("mage", "Маг");
    }

    public static class Attack {
        public String name = "Удар";
        public String anim = "";
        public float damage = 3.0F;
        public float range = 2.2F;
        public int windup = 0;
        public int duration = 0;
        public int cooldown = 12;
        public float knockback = 0.4F;
        public int weight = 1;
        public float lunge = 0.0F;
        public String sound = "";
        public String kind = "melee";
        public String projectile = "minecraft:arrow";
        public float shotSpeed = 1.6F;
        public float spread = 2.0F;
        public int shots = 1;
        public String release = "";

        public boolean ranged() {
            return "ranged".equals(this.kind);
        }

        public Attack() {
        }

        public Attack(String name, String anim, float damage, float range, int windup, int cooldown, int weight) {
            this.name = name;
            this.anim = anim;
            this.damage = damage;
            this.range = range;
            this.windup = windup;
            this.cooldown = cooldown;
            this.weight = weight;
        }
    }

    public static class Cover {
        public float at = 0.0F;
        public float chance = 0.0F;
        public float radius = 12.0F;
        public int time = 60;
        public String anim = "";
    }

    public static class Defense {
        public String anim = "";
        public float chance = 0.0F;
        public float power = 0.8F;
    }

    public static class Heal {
        public float at = 0.0F;
        public float amount = 8.0F;
        public int charges = 1;
        public int time = 0;
        public String anim = "";
        public String sound = "minecraft:entity.generic.drink";
        public boolean hide = true;
    }
}
