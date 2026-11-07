package org.sprautestudio.spraute_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BlockDef {
    public static final String FULL = "block";
    public static final String FALLING = "falling";
    public static final String ROTATING = "rotating";
    public static final String STORAGE = "storage";
    public static final String PILLAR = "pillar";
    public static final String SLAB = "slab";
    public static final String STAIRS = "stairs";
    public static final String WALL = "wall";
    public static final String FENCE = "fence";
    public static final String GATE = "fence_gate";
    public static final String TRAPDOOR = "trapdoor";
    public static final String BUTTON = "button";
    public static final String PLATE = "pressure_plate";
    public static final String CARPET = "carpet";
    public static final String PANE = "pane";
    public static final String PLANT = "plant";
    public static final List<String> KINDS = List.of(
        "block",
        "falling",
        "rotating",
        "storage",
        "pillar",
        "slab",
        "stairs",
        "wall",
        "fence",
        "fence_gate",
        "trapdoor",
        "button",
        "pressure_plate",
        "carpet",
        "pane",
        "plant"
    );
    public static final List<String> MATERIALS = List.of("stone", "wood", "metal", "dirt", "sand", "plant", "glass", "wool", "ice");
    public static final List<String> SOUNDS = List.of(
        "stone", "wood", "metal", "grass", "gravel", "sand", "glass", "wool", "snow", "bone", "amethyst", "deepslate", "netherrack", "honey", "slime"
    );
    public static final List<String> TOOLS = List.of("none", "pickaxe", "axe", "shovel", "hoe");
    public static final List<String> RENDER = List.of("solid", "cutout", "translucent");
    public static final List<String> DROPS = List.of("self", "none", "item");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String id = "";
    public String name = "";
    public String kind = "block";
    public List<String> lore = new ArrayList<>();
    public String tab = "building";
    public String rarity = "common";
    public int maxStack = 64;
    public boolean glint;
    public boolean fireResistant;
    public int burnTicks;
    public String texture = "";
    public String texTop = "";
    public String texBottom = "";
    public String texSide = "";
    public String texFront = "";
    public String lookBlock = "minecraft:stone";
    public ModelLook model = new ModelLook();
    public String render = "solid";
    public String material = "stone";
    public String sound = "stone";
    public float hardness = 1.5F;
    public float resistance = 6.0F;
    public boolean unbreakable;
    public String tool = "pickaxe";
    public int toolLevel;
    public boolean requiresTool = true;
    public int light;
    public float friction = 0.6F;
    public float speedFactor = 1.0F;
    public float jumpFactor = 1.0F;
    public boolean noCollision;
    public int redstone;
    public int flammability;
    public int fireSpread;
    public static final int BOX_MIN = -48;
    public static final int BOX_MAX = 64;
    public static final String SHAPE_FULL = "full";
    public static final String SHAPE_CUSTOM = "custom";
    private static final Map<String, List<BlockDef.Box>> SHAPE_PARTS = new LinkedHashMap<>();
    public static final List<String> SHAPES;
    public static final List<String> ORE_DIMENSIONS = List.of("overworld", "nether", "end");
    public boolean boxed;
    public int boxX1;
    public int boxY1;
    public int boxZ1;
    public int boxX2 = 16;
    public int boxY2 = 16;
    public int boxZ2 = 16;
    public String shape = "full";
    public List<BlockDef.Box> boxes = new ArrayList<>();
    public boolean ore;
    public String oreDimension = "overworld";
    public int oreSize = 9;
    public int oreCount = 8;
    public int oreMinY = -64;
    public int oreMaxY = 64;
    public boolean oreMiddle;
    public int oreRarity;
    public float oreAirDiscard;
    public int storageRows = 3;
    public String storageLoot = "";
    public String drop = "self";
    public String dropItem = "minecraft:cobblestone";
    public int dropMin = 1;
    public int dropMax = 1;
    public boolean dropFortune;
    public boolean dropSilk;
    public int xpMin;
    public int xpMax;
    public String clickCommand = "";
    public String clickSound = "";
    public List<ItemDef.Effect> clickEffects = new ArrayList<>();
    public float stepDamage;
    public String stepCommand = "";
    public List<ItemDef.Effect> stepEffects = new ArrayList<>();
    public String breakCommand = "";
    public String randomCommand = "";

    public static BlockDef create(String id, String name, String kind) {
        BlockDef d = new BlockDef();
        d.id = id;
        d.name = name;
        d.setKind(kind);
        return d;
    }

    public void setKind(String next) {
        this.kind = next;
        switch (next) {
            case "plant":
                this.render = "cutout";
                this.noCollision = true;
                this.hardness = 0.0F;
                this.resistance = 0.0F;
                this.tool = "none";
                this.requiresTool = false;
                this.material = "plant";
                this.sound = "grass";
                this.lookBlock = "minecraft:dandelion";
                break;
            case "pane":
                this.render = "cutout";
                this.material = "glass";
                this.sound = "glass";
                this.tool = "none";
                this.requiresTool = false;
                this.lookBlock = "minecraft:glass";
                break;
            case "trapdoor":
                this.render = "cutout";
                break;
            case "storage":
                this.material = "wood";
                this.sound = "wood";
                this.tool = "axe";
                this.requiresTool = false;
                this.hardness = 2.5F;
                this.resistance = 2.5F;
                this.lookBlock = "minecraft:oak_planks";
                break;
            case "falling":
                this.material = "sand";
                this.sound = "sand";
                this.tool = "shovel";
                this.requiresTool = false;
                this.hardness = 0.5F;
                this.resistance = 0.5F;
                this.lookBlock = "minecraft:sand";
                break;
            case "carpet":
                this.material = "wool";
                this.sound = "wool";
                this.tool = "none";
                this.requiresTool = false;
                this.hardness = 0.1F;
                this.resistance = 0.1F;
                this.lookBlock = "minecraft:white_wool";
        }
    }

    public boolean big() {
        if ((this.kind.equals("block") || this.kind.equals("rotating")) && this.shaped()) {
            for (BlockDef.Box b : this.boxes) {
                if (b.outside()) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public boolean hasModel() {
        return this.canBox() && this.model != null && this.model.active();
    }

    public boolean engineModel() {
        return this.hasModel() && this.model.engine() && (this.kind.equals("block") || this.kind.equals("rotating"));
    }

    public boolean canBox() {
        return this.kind.equals("block") || this.kind.equals("falling") || this.kind.equals("rotating") || this.kind.equals("storage");
    }

    public boolean shaped() {
        return this.canBox() && !this.shape.equals("full") && !this.boxes.isEmpty();
    }

    public void setShape(String next) {
        this.shape = next;
        if (next.equals("custom")) {
            if (this.boxes.isEmpty()) {
                this.boxes.add(new BlockDef.Box(4, 0, 4, 12, 8, 12));
            }
        } else {
            this.boxes = new ArrayList<>();

            for (BlockDef.Box b : SHAPE_PARTS.getOrDefault(next, List.of())) {
                this.boxes.add(new BlockDef.Box(b.x1, b.y1, b.z1, b.x2, b.y2, b.z2));
            }
        }
    }

    public boolean canBeOre() {
        return this.kind.equals("block") || this.kind.equals("falling") || this.kind.equals("rotating") || this.kind.equals("pillar");
    }

    public boolean generates() {
        return this.ore && this.canBeOre();
    }

    public void makeOre() {
        this.material = "stone";
        this.sound = "stone";
        this.hardness = 3.0F;
        this.resistance = 3.0F;
        this.tool = "pickaxe";
        this.toolLevel = 1;
        this.requiresTool = true;
        this.drop = "item";
        this.dropItem = "minecraft:diamond";
        this.dropMin = 1;
        this.dropMax = 1;
        this.dropFortune = true;
        this.dropSilk = true;
        this.xpMin = 1;
        this.xpMax = 3;
        this.ore = true;
        this.lookBlock = "minecraft:iron_ore";
    }

    public void oreFrom(BlockDef started) {
        this.ore = started.ore;
        this.oreDimension = started.oreDimension;
        this.oreSize = started.oreSize;
        this.oreCount = started.oreCount;
        this.oreMinY = started.oreMinY;
        this.oreMaxY = started.oreMaxY;
        this.oreMiddle = started.oreMiddle;
        this.oreRarity = started.oreRarity;
        this.oreAirDiscard = started.oreAirDiscard;
    }

    public boolean transparent() {
        return !this.render.equals("solid");
    }

    public void fix() {
        if (this.model == null) {
            this.model = new ModelLook();
        }

        this.model.fix();
        if (!KINDS.contains(this.kind)) {
            this.kind = "block";
        }

        if (!MATERIALS.contains(this.material)) {
            this.material = "stone";
        }

        if (!SOUNDS.contains(this.sound)) {
            this.sound = "stone";
        }

        if (!TOOLS.contains(this.tool)) {
            this.tool = "none";
        }

        if (!RENDER.contains(this.render)) {
            this.render = "solid";
        }

        if (!DROPS.contains(this.drop)) {
            this.drop = "self";
        }

        if (!ItemDef.TABS.contains(this.tab) && !this.tab.startsWith("custom:")) {
            this.tab = "building";
        }

        if (!ItemDef.RARITIES.contains(this.rarity)) {
            this.rarity = "common";
        }

        this.maxStack = clamp(this.maxStack, 1, 64);
        this.burnTicks = clamp(this.burnTicks, 0, 32767);
        this.hardness = Math.max(0.0F, this.hardness);
        this.resistance = Math.max(0.0F, this.resistance);
        this.toolLevel = clamp(this.toolLevel, 0, 3);
        this.light = clamp(this.light, 0, 15);
        this.redstone = clamp(this.redstone, 0, 15);
        this.friction = Math.max(0.01F, Math.min(1.5F, this.friction));
        this.speedFactor = Math.max(0.0F, this.speedFactor);
        this.jumpFactor = Math.max(0.0F, this.jumpFactor);
        this.flammability = clamp(this.flammability, 0, 300);
        this.fireSpread = clamp(this.fireSpread, 0, 300);
        if (this.boxed) {
            this.boxes.add(new BlockDef.Box(this.boxX1, this.boxY1, this.boxZ1, this.boxX2, this.boxY2, this.boxZ2));
            this.shape = "custom";
            this.boxed = false;
        }

        if (!SHAPE_PARTS.containsKey(this.shape)) {
            this.shape = "full";
        }

        this.storageRows = clamp(this.storageRows, 1, 6);
        if (this.storageLoot == null) {
            this.storageLoot = "";
        }

        this.boxes.forEach(BlockDef.Box::fix);
        if (!ORE_DIMENSIONS.contains(this.oreDimension)) {
            this.oreDimension = "overworld";
        }

        this.oreSize = clamp(this.oreSize, 1, 64);
        this.oreCount = clamp(this.oreCount, 1, 256);
        this.oreMinY = clamp(this.oreMinY, -2032, 2031);
        this.oreMaxY = clamp(this.oreMaxY, this.oreMinY, 2031);
        this.oreRarity = clamp(this.oreRarity, 0, 10000);
        this.oreAirDiscard = Math.max(0.0F, Math.min(1.0F, this.oreAirDiscard));
        this.dropMin = clamp(this.dropMin, 0, 64);
        this.dropMax = clamp(this.dropMax, this.dropMin, 64);
        this.xpMin = clamp(this.xpMin, 0, 1000);
        this.xpMax = clamp(this.xpMax, this.xpMin, 1000);
        this.stepDamage = Math.max(0.0F, this.stepDamage);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    public String problem() {
        if (!ItemDef.validId(this.id)) {
            return "Id: только латиница, цифры и _";
        } else {
            return this.name.isBlank() ? "Нет названия" : null;
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static BlockDef fromJson(String text) {
        BlockDef d = GSON.fromJson(text, BlockDef.class);
        if (d == null) {
            throw new IllegalArgumentException("empty block");
        } else {
            d.fix();
            return d;
        }
    }

    public BlockDef copy(String newId) {
        BlockDef d = fromJson(this.toJson());
        d.id = newId;
        return d;
    }

    public String restartKey() {
        BlockDef d = fromJson(this.toJson());
        d.name = "";
        d.lore = new ArrayList<>();
        d.glint = false;
        d.texture = "";
        d.texTop = "";
        d.texBottom = "";
        d.texSide = "";
        d.texFront = "";
        d.lookBlock = "";
        boolean modeled = d.hasModel();
        boolean engine = d.engineModel();
        d.model = new ModelLook();
        if (modeled) {
            d.model.name = "*";
        }

        if (engine) {
            d.model.render = "engine";
        }

        d.storageLoot = "";
        d.tool = "";
        d.toolLevel = 0;
        d.drop = "";
        d.dropItem = "";
        d.dropMin = 0;
        d.dropMax = 0;
        d.dropFortune = false;
        d.dropSilk = false;
        return d.toJson();
    }

    static {
        SHAPE_PARTS.put("full", List.of());
        SHAPE_PARTS.put("half", List.of(new BlockDef.Box(0, 0, 0, 16, 8, 16)));
        SHAPE_PARTS.put("half_top", List.of(new BlockDef.Box(0, 8, 0, 16, 16, 16)));
        SHAPE_PARTS.put("layer", List.of(new BlockDef.Box(0, 0, 0, 16, 2, 16)));
        SHAPE_PARTS.put("small", List.of(new BlockDef.Box(4, 0, 4, 12, 8, 12)));
        SHAPE_PARTS.put("center", List.of(new BlockDef.Box(4, 4, 4, 12, 12, 12)));
        SHAPE_PARTS.put("post", List.of(new BlockDef.Box(6, 0, 6, 10, 16, 10)));
        SHAPE_PARTS.put("column", List.of(new BlockDef.Box(3, 0, 3, 13, 16, 13)));
        SHAPE_PARTS.put("panel", List.of(new BlockDef.Box(0, 0, 7, 16, 16, 9)));
        SHAPE_PARTS.put("step", List.of(new BlockDef.Box(0, 0, 0, 16, 8, 16), new BlockDef.Box(0, 8, 8, 16, 16, 16)));
        SHAPE_PARTS.put("table", List.of(new BlockDef.Box(0, 13, 0, 16, 16, 16), new BlockDef.Box(6, 0, 6, 10, 13, 10)));
        SHAPE_PARTS.put(
            "pedestal", List.of(new BlockDef.Box(2, 0, 2, 14, 3, 14), new BlockDef.Box(5, 3, 5, 11, 13, 11), new BlockDef.Box(2, 13, 2, 14, 16, 14))
        );
        SHAPE_PARTS.put("lantern", List.of(new BlockDef.Box(5, 0, 5, 11, 9, 11), new BlockDef.Box(6, 9, 6, 10, 11, 10)));
        SHAPE_PARTS.put("custom", List.of());

        // decompile note: Vineflower hoisted this into the field declaration, where it ran before the block above
        // filled the source collection (and produced an empty list); the original <clinit> assigns it last.
        SHAPES = List.copyOf(SHAPE_PARTS.keySet());
    }

    public static final class Box {
        public int x1;
        public int y1;
        public int z1;
        public int x2 = 16;
        public int y2 = 16;
        public int z2 = 16;

        public Box() {
        }

        public Box(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
        }

        void fix() {
            this.x1 = BlockDef.clamp(this.x1, -48, 63);
            this.y1 = BlockDef.clamp(this.y1, -48, 63);
            this.z1 = BlockDef.clamp(this.z1, -48, 63);
            this.x2 = BlockDef.clamp(this.x2, this.x1 + 1, 64);
            this.y2 = BlockDef.clamp(this.y2, this.y1 + 1, 64);
            this.z2 = BlockDef.clamp(this.z2, this.z1 + 1, 64);
        }

        public boolean outside() {
            return this.x1 < 0 || this.y1 < 0 || this.z1 < 0 || this.x2 > 16 || this.y2 > 16 || this.z2 > 16;
        }
    }
}
