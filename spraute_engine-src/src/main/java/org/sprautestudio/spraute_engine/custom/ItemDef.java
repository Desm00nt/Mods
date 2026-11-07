package org.sprautestudio.spraute_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public final class ItemDef {
    public static final String OWN_TAB = "custom:";
    public static final String GENERIC = "generic";
    public static final String FOOD = "food";
    public static final String SWORD = "sword";
    public static final String PICKAXE = "pickaxe";
    public static final String AXE = "axe";
    public static final String SHOVEL = "shovel";
    public static final String HOE = "hoe";
    public static final String ARMOR = "armor";
    public static final List<String> KINDS = List.of("generic", "food", "sword", "pickaxe", "axe", "shovel", "hoe", "armor");
    public static final List<String> TABS = List.of(
        "none", "building", "decorations", "redstone", "transport", "misc", "food", "tools", "combat", "materials", "brewing"
    );
    public static final List<String> RARITIES = List.of("common", "uncommon", "rare", "epic");
    public static final List<String> ARMOR_SLOTS = List.of("head", "chest", "legs", "feet");
    public static final List<String> ARMOR_LOOKS = List.of("leather", "chainmail", "iron", "gold", "diamond", "netherite", "turtle", "image");
    public static final List<String> USE_ANIMS = List.of("", "none", "eat", "drink", "block", "bow", "spear");
    public static final List<String> ATTRIBUTE_OPS = List.of("addition", "multiply_base", "multiply_total");
    public static final List<String> ATTRIBUTE_SLOTS = List.of("mainhand", "offhand", "head", "chest", "legs", "feet");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String id = "";
    public String name = "";
    public String kind = "generic";
    public List<String> lore = new ArrayList<>();
    public String tab = "misc";
    public String rarity = "common";
    public boolean glint;
    public boolean fireResistant;
    public boolean noBlockBreak;
    public String texture = "";
    public String lookItem = "minecraft:paper";
    public boolean handheld;
    public String swingAnim = "";
    public String holdAnim = "";
    public ModelLook model = new ModelLook();
    public int maxStack = 64;
    public int durability;
    public int enchantability;
    public String repair = "";
    public String craftRemainder = "";
    public int burnTicks;
    public int nutrition = 4;
    public float saturation = 0.3F;
    public boolean meat;
    public boolean alwaysEat;
    public boolean fastEat;
    public int useTicks;
    public String useAnim = "";
    public int cooldownTicks;
    public boolean consumeOnUse;
    public boolean damageOnUse;
    public String useSound = "";
    public String useCommand = "";
    public String useRemainder = "";
    public List<ItemDef.Effect> useEffects = new ArrayList<>();
    public List<ItemDef.Effect> hitEffects = new ArrayList<>();
    public int hitFireSeconds;
    public List<ItemDef.Effect> heldEffects = new ArrayList<>();
    public List<ItemDef.Effect> inventoryEffects = new ArrayList<>();
    public List<ItemDef.Effect> wornEffects = new ArrayList<>();
    public List<ItemDef.Attr> attributes = new ArrayList<>();
    public float attackDamage = 6.0F;
    public float attackSpeed = 1.6F;
    public float miningSpeed = 6.0F;
    public int miningLevel = 2;
    public String armorSlot = "chest";
    public int defense = 6;
    public float toughness;
    public float knockbackResistance;
    public String equipSound = "";
    public String armorLook = "iron";
    public String armorLayer1 = "";
    public String armorLayer2 = "";

    public static ItemDef create(String id, String name, String kind) {
        ItemDef d = new ItemDef();
        d.id = id;
        d.name = name;
        d.setKind(kind);
        return d;
    }

    public boolean isTool() {
        return this.kind.equals("sword") || this.kind.equals("pickaxe") || this.kind.equals("axe") || this.kind.equals("shovel") || this.kind.equals("hoe");
    }

    public boolean isArmor() {
        return this.kind.equals("armor");
    }

    public boolean isFood() {
        return this.kind.equals("food");
    }

    public boolean hasUseAction() {
        return this.useTicks > 0
            || this.cooldownTicks > 0
            || this.consumeOnUse
            || this.damageOnUse
            || !this.useSound.isEmpty()
            || !this.useCommand.isEmpty()
            || !this.useRemainder.isEmpty()
            || !this.useEffects.isEmpty();
    }

    public void setKind(String next) {
        this.kind = next;
        if (this.isTool()) {
            this.maxStack = 1;
            this.handheld = true;
            if (this.durability <= 0) {
                this.durability = 250;
            }

            switch (next) {
                case "sword":
                    this.tool(6.0F, 1.6F, "minecraft:iron_sword");
                    break;
                case "pickaxe":
                    this.tool(4.0F, 1.2F, "minecraft:iron_pickaxe");
                    break;
                case "axe":
                    this.tool(9.0F, 0.9F, "minecraft:iron_axe");
                    break;
                case "shovel":
                    this.tool(4.5F, 1.0F, "minecraft:iron_shovel");
                    break;
                default:
                    this.tool(1.0F, 1.0F, "minecraft:iron_hoe");
            }
        } else if (this.isArmor()) {
            this.maxStack = 1;
            this.handheld = false;
            if (this.durability <= 0) {
                this.durability = 200;
            }

            this.setArmorSlot(this.armorSlot);
        } else {
            this.handheld = false;
            if (this.isFood() && this.lookItem.equals("minecraft:paper")) {
                this.lookItem = "minecraft:apple";
            }
        }
    }

    private void tool(float damage, float speed, String look) {
        this.attackDamage = damage;
        this.attackSpeed = speed;
        this.lookItem = look;
    }

    public void setArmorSlot(String slot) {
        this.armorSlot = slot;

        this.defense = switch (slot) {
            case "head", "feet" -> 2;
            case "legs" -> 5;
            default -> 6;
        };

        this.lookItem = "minecraft:iron_" + switch (slot) {
            case "head" -> "helmet";
            case "legs" -> "leggings";
            case "feet" -> "boots";
            default -> "chestplate";
        };
    }

    public void fix() {
        if (this.model == null) {
            this.model = new ModelLook();
        }

        this.model.fix();
        if (this.swingAnim == null) {
            this.swingAnim = "";
        }

        if (this.holdAnim == null) {
            this.holdAnim = "";
        }

        if (!KINDS.contains(this.kind)) {
            this.kind = "generic";
        }

        if (!TABS.contains(this.tab) && !this.tab.startsWith("custom:")) {
            this.tab = "misc";
        }

        if (!RARITIES.contains(this.rarity)) {
            this.rarity = "common";
        }

        if (!ARMOR_SLOTS.contains(this.armorSlot)) {
            this.armorSlot = "chest";
        }

        if (!ARMOR_LOOKS.contains(this.armorLook)) {
            this.armorLook = "iron";
        }

        this.maxStack = clamp(this.maxStack, 1, 64);
        this.durability = clamp(this.durability, 0, 1000000);
        if (this.isTool() || this.isArmor()) {
            this.durability = Math.max(1, this.durability);
        }

        this.enchantability = clamp(this.enchantability, 0, 100);
        this.burnTicks = clamp(this.burnTicks, 0, 32767);
        this.nutrition = clamp(this.nutrition, 0, 1000);
        this.saturation = Math.max(0.0F, this.saturation);
        this.useTicks = clamp(this.useTicks, 0, 72000);
        this.cooldownTicks = clamp(this.cooldownTicks, 0, 72000);
        this.hitFireSeconds = clamp(this.hitFireSeconds, 0, 3600);
        this.attackDamage = Math.max(1.0F, this.attackDamage);
        this.attackSpeed = Math.max(0.05F, this.attackSpeed);
        this.miningSpeed = Math.max(0.0F, this.miningSpeed);
        this.miningLevel = clamp(this.miningLevel, 0, 100);
        this.defense = clamp(this.defense, 0, 1000);
        this.toughness = Math.max(0.0F, this.toughness);
        this.knockbackResistance = Math.max(0.0F, this.knockbackResistance);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public String problem() {
        if (!validId(this.id)) {
            return "Id: только латиница, цифры и _";
        } else {
            return this.name.isBlank() ? "Нет названия" : null;
        }
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_]+");
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ItemDef fromJson(String text) {
        ItemDef d = GSON.fromJson(text, ItemDef.class);
        if (d == null) {
            throw new IllegalArgumentException("empty item");
        } else {
            d.fix();
            return d;
        }
    }

    public ItemDef copy(String newId) {
        ItemDef d = fromJson(this.toJson());
        d.id = newId;
        return d;
    }

    public String restartKey() {
        ItemDef d = fromJson(this.toJson());
        d.name = "";
        d.lore = new ArrayList<>();
        d.glint = false;
        d.texture = "";
        d.lookItem = "";
        d.handheld = false;
        d.model = new ModelLook();
        d.swingAnim = "";
        d.holdAnim = "";
        d.armorLayer1 = "";
        d.armorLayer2 = "";
        return d.toJson();
    }

    public static final class Attr {
        public String id = "minecraft:generic.max_health";
        public double amount = 2.0;
        public String op = "addition";
        public String slot = "mainhand";
    }

    public static final class Effect {
        public String id = "minecraft:speed";
        public float seconds = 10.0F;
        public int level = 1;
        public float chance = 1.0F;
        public boolean hidden;
    }
}
