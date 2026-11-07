package org.sprautestudio.spraute_engine.skills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillTree {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final List<String> VIEWS = List.of("tree", "fog", "grid");
    public static final List<String> INTERACTS = List.of("side", "hover");
    public static final List<String> LINKS = List.of("diagonal", "elbow");
    public static final List<String> SIZES = List.of("compact", "normal", "big");
    public static final List<String> CURVES = List.of("flat", "linear", "geometric");
    public static final List<String> DIRECTIONS = List.of("right", "left", "up", "down");
    public String title = "Навыки";
    public List<SkillTree.Currency> currencies = new ArrayList<>();
    public boolean levels = true;
    public int maxLevel = 50;
    public String curve = "geometric";
    public float xpBase = 100.0F;
    public float xpGrowth = 1.25F;
    public List<SkillTree.LevelReward> levelRewards = new ArrayList<>();
    public String levelCommand = "";
    public List<SkillTree.Source> sources = new ArrayList<>();
    public String view = "tree";
    public int accent = 9137407;
    public String background = "";
    public String openKey = "k";
    public List<SkillTree.Menu> menus = new ArrayList<>();
    public boolean hud = true;
    public boolean hudAbilities = true;
    public boolean allowReset = true;
    public float refund = 1.0F;
    public List<SkillTree.Node> nodes = new ArrayList<>();
    public static final List<String> EFFECTS = List.of("stat", "unlock", "item", "potion", "command");
    public static final List<String> ACTIONS = List.of("story", "heal", "dash", "potion", "command", "code");
    public static final Map<String, String> PRESET_TITLES = new LinkedHashMap<>();

    public SkillTree.Node node(String id) {
        for (SkillTree.Node n : this.nodes) {
            if (n.id.equals(id)) {
                return n;
            }
        }

        return null;
    }

    public SkillTree.Menu menu(String id) {
        for (SkillTree.Menu m : this.menus) {
            if (m.id.equals(id)) {
                return m;
            }
        }

        return this.menus.get(0);
    }

    public String freshMenuId(String name) {
        String base = slug(name);
        String id = base;

        for (int n = 2; this.hasMenu(id); n++) {
            id = base + "_" + n;
        }

        return id;
    }

    private boolean hasMenu(String id) {
        for (SkillTree.Menu m : this.menus) {
            if (m.id.equals(id)) {
                return true;
            }
        }

        return false;
    }

    public List<String> sections() {
        List<String> out = new ArrayList<>();

        for (SkillTree.Node n : this.nodes) {
            if (!out.contains(n.section)) {
                out.add(n.section);
            }
        }

        return out;
    }

    public SkillTree.Currency currency(String id) {
        for (SkillTree.Currency c : this.currencies) {
            if (c.id.equals(id)) {
                return c;
            }
        }

        return null;
    }

    public String freshNodeId(String title) {
        String base = slug(title);
        String id = base;

        for (int n = 2; this.node(id) != null; n++) {
            id = base + "_" + n;
        }

        return id;
    }

    public String freshCurrencyId(String name) {
        String base = slug(name);
        String id = base;

        for (int n = 2; this.currency(id) != null; n++) {
            id = base + "_" + n;
        }

        return id;
    }

    private static String slug(String text) {
        String s = text == null ? "" : text.trim().toLowerCase().replaceAll("[^\\p{L}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
        return s.isEmpty() ? "node" : s;
    }

    public void remove(SkillTree.Node gone) {
        this.nodes.remove(gone);

        for (SkillTree.Node n : this.nodes) {
            n.parents.remove(gone.id);
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static SkillTree fromJson(String json) {
        SkillTree d = null;

        try {
            d = GSON.fromJson(json, SkillTree.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new SkillTree();
        }

        d.fix();
        return d;
    }

    public void fix() {
        if (this.title == null) {
            this.title = "";
        }

        if (this.currencies == null) {
            this.currencies = new ArrayList<>();
        }

        if (this.currencies.isEmpty()) {
            this.currencies.add(new SkillTree.Currency());
        }

        if (this.levelRewards == null) {
            this.levelRewards = new ArrayList<>();
        }

        if (this.sources == null) {
            this.sources = new ArrayList<>();
        }

        if (this.nodes == null) {
            this.nodes = new ArrayList<>();
        }

        if (this.levelCommand == null) {
            this.levelCommand = "";
        }

        if (this.background == null) {
            this.background = "";
        }

        if (this.openKey == null) {
            this.openKey = "";
        }

        if (this.menus == null) {
            this.menus = new ArrayList<>();
        }

        if (this.menus.isEmpty()) {
            SkillTree.Menu m = new SkillTree.Menu();
            m.view = VIEWS.contains(this.view) ? this.view : "tree";
            m.background = this.background;
            m.openKey = this.openKey;
            m.ownLook = this.accent != (new SkillLook()).accent;
            m.look.accent = this.accent;
            this.menus.add(m);
        }

        for (SkillTree.Menu m : this.menus) {
            if (m.sections == null) {
                m.sections = new ArrayList<>();
            }

            if (m.look == null) {
                m.look = new SkillLook();
            }

            m.look.fix();
            if (m.background == null) {
                m.background = "";
            }

            if (m.openKey == null) {
                m.openKey = "";
            }

            if (m.title == null) {
                m.title = "";
            }

            if (m.layout == null) {
                m.layout = "skills";
            }

            if (!VIEWS.contains(m.view)) {
                m.view = "tree";
            }

            if (!INTERACTS.contains(m.interact)) {
                m.interact = "side";
            }

            if (!LINKS.contains(m.links)) {
                m.links = "diagonal";
            }

            if (!SIZES.contains(m.size)) {
                m.size = "normal";
            }

            m.columns = Math.max(0, m.columns);
            m.zoom = Math.max(0.0F, m.zoom);
            m.zoomMin = Math.max(0.05F, m.zoomMin);
            m.zoomMax = Math.max(m.zoomMin, m.zoomMax);
            if (!DIRECTIONS.contains(m.xpDirection)) {
                m.xpDirection = "right";
            }

            if (m.layers == null) {
                m.layers = new ArrayList<>();
            }

            m.layers.removeIf(lx -> lx == null);

            for (SkillTree.Layer l : m.layers) {
                if (l.image == null) {
                    l.image = "";
                }
            }
        }

        if (!VIEWS.contains(this.view)) {
            this.view = "tree";
        }

        if (!CURVES.contains(this.curve)) {
            this.curve = "geometric";
        }

        this.maxLevel = Math.max(1, this.maxLevel);
        this.xpBase = Math.max(1.0F, this.xpBase);
        this.xpGrowth = Math.max(0.0F, this.xpGrowth);
        this.refund = Math.max(0.0F, Math.min(1.0F, this.refund));

        for (SkillTree.Node n : this.nodes) {
            if (n.parents == null) {
                n.parents = new ArrayList<>();
            }

            if (n.ability == null) {
                n.ability = new SkillTree.Ability();
            }

            if (n.costs == null) {
                n.costs = new ArrayList<>();
            }

            if (n.effects == null) {
                n.effects = new ArrayList<>();
            }

            n.effects.removeIf(e -> e == null || !EFFECTS.contains(e.kind));
            if (n.ability == null) {
                n.ability = new SkillTree.Ability();
            }

            if (n.group == null) {
                n.group = "";
            }

            if (n.description == null) {
                n.description = "";
            }

            if (n.icon == null) {
                n.icon = "";
            }

            if (n.image == null) {
                n.image = "";
            }

            if (n.section == null) {
                n.section = "";
            }

            if (n.learnCode == null) {
                n.learnCode = "";
            }

            if (n.learnBlocks == null) {
                n.learnBlocks = "";
            }

            if (n.passiveCode == null) {
                n.passiveCode = "";
            }

            if (n.passiveBlocks == null) {
                n.passiveBlocks = "";
            }

            if (n.ability.code == null) {
                n.ability.code = "";
            }

            if (n.ability.blocks == null) {
                n.ability.blocks = "";
            }

            if (!"all".equals(n.parentMode)) {
                n.parentMode = "any";
            }

            n.maxRank = Math.max(1, n.maxRank);
            n.costStep = Math.max(0, n.costStep);
            n.level = Math.max(0, n.level);
            n.parents.removeIf(p -> p.equals(n.id) || this.node(p) == null);
        }
    }

    public static SkillTree preset(String key) {
        SkillTree big = SkillTemplates.build(key);
        if (big != null) {
            return big;
        } else {
            SkillTree t = new SkillTree();
            SkillTree.Currency pts = new SkillTree.Currency("очки", "Очки навыков", 16765530, "minecraft:experience_bottle");
            t.currencies.add(pts);
            switch (key) {
                case "classic":
                    earnsByPlaying(t);
                    t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 1, 2));
                    branches(t);
                    break;
                case "fog":
                    t.title = "Путь";
                    t.view = "fog";
                    earnsByPlaying(t);
                    t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 1, 2));
                    branches(t);
                    break;
                case "table":
                    t.title = "Улучшения";
                    t.view = "grid";
                    SkillTree.Menu shop = new SkillTree.Menu();
                    shop.view = "grid";
                    shop.interact = "hover";
                    shop.size = "big";
                    shop.openKey = "k";
                    t.menus.add(shop);
                    t.levels = false;
                    t.sources.add(new SkillTree.Source("kill", "", 0.0F, "очки", 1));
                    t.sources.add(new SkillTree.Source("questdone", "", 0.0F, "очки", 3));
                    t.nodes
                        .add(
                            new SkillTree.Node("здоровье", "Здоровье", "minecraft:golden_apple", 0, 0)
                                .cost("очки", 2)
                                .ranks(5, 1)
                                .gives(SkillTree.Effect.stat("max_health", 2.0F))
                                .says("Больше сердец.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("сила", "Сила", "minecraft:iron_sword", 40, 0)
                                .cost("очки", 2)
                                .ranks(5, 1)
                                .gives(SkillTree.Effect.stat("attack_damage", 0.1F))
                                .says("Удары сильнее.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("скорость", "Скорость", "minecraft:sugar", 80, 0)
                                .cost("очки", 2)
                                .ranks(5, 1)
                                .gives(SkillTree.Effect.stat("move_speed", 0.05F))
                                .says("Ноги быстрее.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("броня", "Толстая кожа", "minecraft:leather_chestplate", 120, 0)
                                .cost("очки", 3)
                                .ranks(3, 2)
                                .gives(SkillTree.Effect.stat("armor", 2.0F))
                                .says("Добавляет броню.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("удача", "Удача", "minecraft:rabbit_foot", 160, 0)
                                .cost("очки", 3)
                                .ranks(3, 2)
                                .gives(SkillTree.Effect.stat("luck", 1.0F))
                                .says("Лучше добыча.")
                        );
                    break;
                case "abilities":
                    t.title = "Способности";
                    earnsByPlaying(t);
                    t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 2, 2));
                    SkillTree.Node dash = new SkillTree.Node("рывок", "Рывок", "minecraft:feather", 0, 0).cost("очки", 1).says("R: рывок вперёд.");
                    ability(dash, "r", 6.0F, "dash");
                    dash.ability.power = 1.6F;
                    SkillTree.Node heal = new SkillTree.Node("лечение", "Лечение", "minecraft:glistering_melon_slice", 90, -40)
                        .needs("рывок")
                        .cost("очки", 2)
                        .says("F: восстановить здоровье.");
                    ability(heal, "f", 30.0F, "heal");
                    heal.ability.power = 8.0F;
                    SkillTree.Node rage = new SkillTree.Node("ярость", "Ярость", "minecraft:blaze_powder", 90, 40)
                        .needs("рывок")
                        .cost("очки", 2)
                        .says("G: на время сильнее.");
                    ability(rage, "g", 45.0F, "potion");
                    rage.ability.potion = "minecraft:strength";
                    rage.ability.seconds = 10.0F;
                    t.nodes.addAll(List.of(dash, heal, rage));
                    break;
                case "gear":
                    t.title = "Снаряжение";
                    earnsByPlaying(t);
                    t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 1, 2));
                    t.nodes
                        .add(
                            new SkillTree.Node("железо", "Железо", "minecraft:iron_sword", 0, 0)
                                .cost("очки", 1)
                                .gives(SkillTree.Effect.item("minecraft:iron_sword"))
                                .gives(SkillTree.Effect.item("minecraft:iron_chestplate"))
                                .says("Открывает железные меч и броню.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("алмаз", "Алмаз", "minecraft:diamond_sword", 90, 0)
                                .needs("железо")
                                .at(5)
                                .cost("очки", 3)
                                .gives(SkillTree.Effect.item("minecraft:diamond_sword"))
                                .gives(SkillTree.Effect.item("minecraft:diamond_chestplate"))
                                .says("Открывает алмазные меч и броню.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("лук", "Стрельба", "minecraft:bow", 0, 40)
                                .cost("очки", 1)
                                .gives(SkillTree.Effect.item("minecraft:bow"))
                                .says("Открывает лук.")
                        );
                    t.nodes
                        .add(
                            new SkillTree.Node("бег", "Бег", "minecraft:leather_boots", 0, -40)
                                .cost("очки", 1)
                                .gives(SkillTree.Effect.unlock("sprint"))
                                .says("Пока не изучено, бегать нельзя.")
                        );
                    break;
                case "two":
                    t.title = "Навыки и таланты";
                    t.currencies.add(new SkillTree.Currency("таланты", "Таланты", 6138358, "minecraft:nether_star"));
                    earnsByPlaying(t);
                    t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 1, 2));
                    t.levelRewards.add(new SkillTree.LevelReward("таланты", 1, 5, 5));
                    branches(t);
                    t.nodes
                        .add(
                            new SkillTree.Node("воин", "Путь воина", "minecraft:netherite_sword", 270, -30)
                                .at(10)
                                .cost("таланты", 1)
                                .gives(SkillTree.Effect.stat("attack_damage", 0.5F))
                                .says("Выбрать можно только один путь.")
                        );
                    t.nodes.get(t.nodes.size() - 1).group = "путь";
                    t.nodes
                        .add(
                            new SkillTree.Node("странник", "Путь странника", "minecraft:elytra", 270, 30)
                                .at(10)
                                .cost("таланты", 1)
                                .gives(SkillTree.Effect.stat("move_speed", 0.2F))
                                .says("Выбрать можно только один путь.")
                        );
                    t.nodes.get(t.nodes.size() - 1).group = "путь";
                    break;
                case "empty":
                    t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 1, 2));
                    t.sources.add(new SkillTree.Source("kill", "", 10.0F, "", 0));
                    break;
                default:
                    return null;
            }

            t.fix();
            return t;
        }
    }

    private static void earnsByPlaying(SkillTree t) {
        t.sources.add(new SkillTree.Source("kill", "", 10.0F, "", 0));
        t.sources.add(new SkillTree.Source("break", "minecraft:diamond_ore", 15.0F, "", 0));
        t.sources.add(new SkillTree.Source("questdone", "", 100.0F, "", 0));
    }

    private static void ability(SkillTree.Node n, String key, float cooldown, String action) {
        n.ability.on = true;
        n.ability.key = key;
        n.ability.cooldown = cooldown;
        n.ability.action = action;
    }

    private static void branches(SkillTree t) {
        t.nodes.add(new SkillTree.Node("начало", "Начало пути", "minecraft:nether_star", 0, 0).cost("очки", 1).says("С этого всё начинается."));
        t.nodes
            .add(
                new SkillTree.Node("здоровье", "Крепкое тело", "minecraft:golden_apple", 90, -60)
                    .needs("начало")
                    .cost("очки", 1)
                    .ranks(3, 1)
                    .gives(SkillTree.Effect.stat("max_health", 2.0F))
                    .says("Больше сердец.")
            );
        t.nodes
            .add(
                new SkillTree.Node("броня", "Толстая кожа", "minecraft:iron_chestplate", 180, -60)
                    .needs("здоровье")
                    .at(5)
                    .cost("очки", 2)
                    .ranks(3, 1)
                    .gives(SkillTree.Effect.stat("armor", 2.0F))
                    .says("Добавляет броню.")
            );
        t.nodes
            .add(
                new SkillTree.Node("сила", "Сила", "minecraft:iron_sword", 90, 0)
                    .needs("начало")
                    .cost("очки", 1)
                    .ranks(3, 1)
                    .gives(SkillTree.Effect.stat("attack_damage", 0.15F))
                    .says("Удары сильнее.")
            );
        t.nodes
            .add(
                new SkillTree.Node("темп", "Быстрые руки", "minecraft:golden_sword", 180, 0)
                    .needs("сила")
                    .at(5)
                    .cost("очки", 2)
                    .ranks(3, 1)
                    .gives(SkillTree.Effect.stat("attack_speed", 0.15F))
                    .says("Удары чаще.")
            );
        t.nodes
            .add(
                new SkillTree.Node("скорость", "Лёгкие ноги", "minecraft:sugar", 90, 60)
                    .needs("начало")
                    .cost("очки", 1)
                    .ranks(3, 1)
                    .gives(SkillTree.Effect.stat("move_speed", 0.05F))
                    .says("Ноги быстрее.")
            );
        t.nodes
            .add(
                new SkillTree.Node("прыжок", "Прыгучесть", "minecraft:rabbit_foot", 180, 60)
                    .needs("скорость")
                    .at(5)
                    .cost("очки", 2)
                    .ranks(2, 1)
                    .gives(SkillTree.Effect.stat("jump_power", 0.2F))
                    .says("Выше прыжки.")
            );
    }

    static {
        PRESET_TITLES.put("star", "Звезда: прокачка во все стороны из центра, 8 лучей, способности на скриптах");
        PRESET_TITLES.put("craft", "Ремесло: вкладки «Бой», «Добыча», «Движение»");
        PRESET_TITLES.put("paths", "Три пути: выбор одной дороги, в тумане");
        PRESET_TITLES.put("classic", "Дерево РПГ: уровни, очки, три ветки");
        PRESET_TITLES.put("fog", "Туман: новое открывается по мере изучения");
        PRESET_TITLES.put("table", "Таблички: простые улучшения без уровней");
        PRESET_TITLES.put("abilities", "Способности: клавиши и перезарядка");
        PRESET_TITLES.put("gear", "Снаряжение: предметы открываются навыками");
        PRESET_TITLES.put("two", "Две валюты: навыки и таланты");
        PRESET_TITLES.put("empty", "Пусто: начать с чистого листа");
    }

    public static final class Ability {
        public boolean on;
        public String key = "r";
        public float cooldown = 10.0F;
        public String action = "story";
        public float power = 6.0F;
        public String potion = "";
        public int level = 1;
        public float seconds = 8.0F;
        public String command = "";
        public String code = "";
        public String blocks = "";
    }

    public static final class Cost {
        public String currency = "points";
        public int amount = 1;

        public Cost() {
        }

        public Cost(String currency, int amount) {
            this.currency = currency;
            this.amount = amount;
        }
    }

    public static final class Currency {
        public String id = "points";
        public String name = "Очки";
        public int color = 16765530;
        public String icon = "minecraft:experience_bottle";
        public String image = "";

        public Currency() {
        }

        public Currency(String id, String name, int color, String icon) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.icon = icon;
        }
    }

    public static final class Effect {
        public String kind = "stat";
        public String rule = "max_health";
        public float amount = 2.0F;
        public String item = "";
        public String potion = "";
        public int level = 1;
        public String command = "";

        public static SkillTree.Effect stat(String rule, float amount) {
            SkillTree.Effect e = new SkillTree.Effect();
            e.rule = rule;
            e.amount = amount;
            return e;
        }

        public static SkillTree.Effect unlock(String rule) {
            SkillTree.Effect e = new SkillTree.Effect();
            e.kind = "unlock";
            e.rule = rule;
            return e;
        }

        public static SkillTree.Effect item(String item) {
            SkillTree.Effect e = new SkillTree.Effect();
            e.kind = "item";
            e.item = item;
            return e;
        }

        public static SkillTree.Effect potion(String potion, int level) {
            SkillTree.Effect e = new SkillTree.Effect();
            e.kind = "potion";
            e.potion = potion;
            e.level = level;
            return e;
        }
    }

    public static final class Layer {
        public String image = "";
        public float depthX = 8.0F;
        public float depthY = 4.0F;
    }

    public static final class LevelReward {
        public String currency = "points";
        public int amount = 1;
        public int every = 1;
        public int from = 2;

        public LevelReward() {
        }

        public LevelReward(String currency, int amount, int every, int from) {
            this.currency = currency;
            this.amount = amount;
            this.every = every;
            this.from = from;
        }
    }

    public static final class Menu {
        public String id = "main";
        public String name = "Основное";
        public String title = "";
        public String view = "tree";
        public String interact = "side";
        public String links = "diagonal";
        public String size = "normal";
        public int columns = 0;
        public List<String> sections = new ArrayList<>();
        public String background = "";
        public String openKey = "";
        public boolean showLevel = true;
        public boolean showPoints = true;
        public String layout = "skills";
        public boolean ownLook = false;
        public SkillLook look = new SkillLook();
        public float zoom = 1.0F;
        public boolean userZoom = false;
        public float zoomMin = 0.5F;
        public float zoomMax = 2.0F;
        public String xpDirection = "right";
        public List<SkillTree.Layer> layers = new ArrayList<>();
    }

    public static final class Node {
        public String id = "node";
        public String title = "Навык";
        public String description = "";
        public String icon = "minecraft:book";
        public String image = "";
        public String section = "";
        public String learnCode = "";
        public String learnBlocks = "";
        public String passiveCode = "";
        public String passiveBlocks = "";
        public int x;
        public int y;
        public List<String> parents = new ArrayList<>();
        public String parentMode = "any";
        public int level = 0;
        public List<SkillTree.Cost> costs = new ArrayList<>();
        public int maxRank = 1;
        public int costStep = 0;
        public String group = "";
        public List<SkillTree.Effect> effects = new ArrayList<>();
        public SkillTree.Ability ability = new SkillTree.Ability();

        public Node() {
        }

        public Node(String id, String title, String icon, int x, int y) {
            this.id = id;
            this.title = title;
            this.icon = icon;
            this.x = x;
            this.y = y;
        }

        SkillTree.Node needs(String... ids) {
            this.parents.addAll(List.of(ids));
            return this;
        }

        SkillTree.Node cost(String currency, int amount) {
            this.costs.add(new SkillTree.Cost(currency, amount));
            return this;
        }

        SkillTree.Node gives(SkillTree.Effect e) {
            this.effects.add(e);
            return this;
        }

        SkillTree.Node ranks(int max, int step) {
            this.maxRank = max;
            this.costStep = step;
            return this;
        }

        SkillTree.Node says(String text) {
            this.description = text;
            return this;
        }

        SkillTree.Node at(int level) {
            this.level = level;
            return this;
        }
    }

    public static final class Source {
        public String event = "kill";
        public String filter = "";
        public float xp = 10.0F;
        public String currency = "";
        public int points = 0;

        public Source() {
        }

        public Source(String event, String filter, float xp, String currency, int points) {
            this.event = event;
            this.filter = filter;
            this.xp = xp;
            this.currency = currency;
            this.points = points;
        }
    }
}
