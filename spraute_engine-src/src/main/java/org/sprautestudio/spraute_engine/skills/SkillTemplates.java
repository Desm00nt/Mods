package org.sprautestudio.spraute_engine.skills;

final class SkillTemplates {
    private static final String PTS = "очки";
    private static final String HERE = "(event(\"x\"), event(\"y\"), event(\"z\"))";
    private static final String DOUBLE_JUMP = "var jumps = 0\non keypress \"jump\" {\n    if event(\"onground\") {\n        set jumps = 0\n    } else if jumps < skillpower() {\n        set jumps = jumps + 1\n        motion jump player 0.55 safe\n        particles \"minecraft:cloud\" at player count 12 spread 0.4\n        sound \"minecraft:entity.phantom.flap\"\n    }\n}";
    private static final String SLAM = "on keypress \"sneak\" {\n    if not event(\"onground\") {\n        motion jump player -1.4\n        particles \"minecraft:cloud\" at player count 8 spread 0.3\n    }\n}";
    private static final String FIRE_SWORD = "on attack {\n    if contains(helditem(player), \"sword\") {\n        ignite evententity() 5s\n        particles \"minecraft:flame\" at evententity() count 10 spread 0.4\n    }\n}";
    private static final String FROST_SWORD = "on attack {\n    if contains(helditem(player), \"sword\") {\n        aoe at evententity() radius 1 effect \"minecraft:slowness\" time 3s level 2\n        particles \"minecraft:snowflake\" at evententity() count 10 spread 0.4\n    }\n}";
    private static final String VAMPIRE = "on attack {\n    if contains(helditem(player), \"sword\") {\n        aoe at player radius 0.5 allies heal (skillpower())\n        particles \"minecraft:heart\" at player count 2 spread 0.4\n    }\n}";
    private static final String THORNS = "on hurt {\n    aoe at evententity() radius 1 damage (1 + skillpower())\n}";
    private static final String LUMBERJACK = "on break {\n    if contains(event(\"block\"), \"_log\") and contains(helditem(player), \"_axe\") {\n        for dy in range(1, 8) {\n            for b in list((event(\"x\"), event(\"y\") + dy, event(\"z\"))) {\n                if contains(block(b), \"_log\") {\n                    breakblock b\n                }\n            }\n        }\n    }\n}";
    private static final String KILL_HEAL = "on kill {\n    aoe at player radius 0.5 allies heal (2 * skillpower())\n}";
    private static final String DASH = "motion forward player 1.6 up 0.2 safe\nsound \"minecraft:entity.ender_dragon.flap\"\nparticles \"minecraft:cloud\" at player count 10 spread 0.5";
    private static final String ROCKET = "motion jump player 1.3 safe\nsound \"minecraft:entity.firework_rocket.launch\"\nparticles \"minecraft:firework\" at player count 20 spread 0.4";
    private static final String HASTE = "effect \"minecraft:speed\" 10s level 2\nsound \"minecraft:entity.horse.gallop\"";
    private static final String SHOVE = "shockwave at player radius 5 power 1.1\nsound \"minecraft:entity.generic.explode\"";
    private static final String PULL = "motion toward lookentity(player, 16) player 1.3 up 0.3\nsound \"minecraft:entity.fishing_bobber.retrieve\"";
    private static final String SPIN = "aoe at player radius 3.5 damage (3 + skillpower() * 2) push 0.5\nsound \"minecraft:entity.player.attack.sweep\"\nparticles \"minecraft:sweep_attack\" at player count 8 spread 1.5";
    private static final String BLINK = "teleport aimpoint(player, 10)\nsound \"minecraft:entity.enderman.teleport\"";

    private SkillTemplates() {
    }

    static SkillTree build(String key) {
        return switch (key) {
            case "star" -> star();
            case "craft" -> craft();
            case "paths" -> paths();
            default -> null;
        };
    }

    private static String areaTool(String tool, int width, int height) {
        return "on break {\n    if contains(helditem(player), \""
            + tool
            + "\") {\n        for b in blockarea((event(\"x\"), event(\"y\"), event(\"z\")), player, "
            + width
            + ", "
            + height
            + ") {\n            breakblock b\n        }\n    }\n}";
    }

    private static SkillTree base(String title, int accent) {
        SkillTree t = new SkillTree();
        t.title = title;
        t.accent = accent;
        t.currencies.add(new SkillTree.Currency("очки", "Очки навыков", 16765530, "minecraft:experience_bottle"));
        t.levelRewards.add(new SkillTree.LevelReward("очки", 1, 1, 2));
        t.sources.add(new SkillTree.Source("kill", "", 12.0F, "", 0));
        t.sources.add(new SkillTree.Source("break", "minecraft:diamond_ore", 20.0F, "", 0));
        t.sources.add(new SkillTree.Source("questdone", "", 150.0F, "", 0));
        return t;
    }

    private static SkillTree done(SkillTree t, String view, String interact) {
        SkillTree.Menu m = new SkillTree.Menu();
        m.view = view;
        m.interact = interact;
        m.openKey = t.openKey;
        m.ownLook = true;
        m.look.accent = t.accent;
        t.menus.add(m);
        t.fix();
        return t;
    }

    private static SkillTree.Node node(SkillTree t, String section, String title, String icon, int x, int y, int price, String says) {
        SkillTree.Node n = new SkillTree.Node(t.freshNodeId(title), title, icon, x, y);
        n.section = section;
        n.description = says;
        n.cost("очки", price);
        t.nodes.add(n);
        return n;
    }

    private static SkillTree.Node after(SkillTree.Node n, SkillTree.Node... parents) {
        for (SkillTree.Node p : parents) {
            n.parents.add(p.id);
        }

        return n;
    }

    private static SkillTree.Node stat(
        SkillTree t, String section, String title, String icon, int x, int y, int price, int ranks, String rule, float amount, String says
    ) {
        return node(t, section, title, icon, x, y, price, says).ranks(ranks, 1).gives(SkillTree.Effect.stat(rule, amount));
    }

    private static SkillTree.Node passive(SkillTree t, String section, String title, String icon, int x, int y, int price, String code, String says) {
        SkillTree.Node n = node(t, section, title, icon, x, y, price, says);
        n.passiveCode = code;
        return n;
    }

    private static SkillTree.Node key(
        SkillTree t, String section, String title, String icon, int x, int y, int price, String key, float cooldown, String code, String says
    ) {
        SkillTree.Node n = node(t, section, title, icon, x, y, price, says);
        n.ability.on = true;
        n.ability.key = key;
        n.ability.cooldown = cooldown;
        n.ability.action = "code";
        n.ability.code = code;
        return n;
    }

    private static int rx(double ray, int radius) {
        return (int)Math.round(Math.cos((Math.PI * 2) * ray / 8.0 - (Math.PI / 2)) * radius / 10.0) * 10;
    }

    private static int ry(double ray, int radius) {
        return (int)Math.round(Math.sin((Math.PI * 2) * ray / 8.0 - (Math.PI / 2)) * radius / 10.0) * 10;
    }

    private static SkillTree star() {
        SkillTree t = base("Звезда талантов", 16757575);
        SkillTree.Node core = node(t, "", "Начало", "minecraft:nether_star", 0, 0, 1, "Центр. Отсюда прокачка идёт во все стороны.");
        int a = 100;
        int b = 190;
        int c = 280;
        int d = 370;
        SkillTree.Node[] inner = new SkillTree.Node[8];
        SkillTree.Node[] outer = new SkillTree.Node[8];
        inner[0] = after(stat(t, "", "Сила", "minecraft:iron_sword", rx(0.0, a), ry(0.0, a), 1, 3, "attack_damage", 0.05F, "Урон +5% за ранг."), core);
        outer[0] = after(
            stat(t, "", "Больше силы", "minecraft:diamond_sword", rx(0.0, b), ry(0.0, b), 2, 3, "attack_damage", 0.05F, "Ещё урон +5% за ранг."), inner[0]
        );
        SkillTree.Node fire = after(
            passive(
                t,
                "",
                "Огненный меч",
                "minecraft:blaze_powder",
                rx(0.0, c) - 40,
                ry(0.0, c),
                3,
                "on attack {\n    if contains(helditem(player), \"sword\") {\n        ignite evententity() 5s\n        particles \"minecraft:flame\" at evententity() count 10 spread 0.4\n    }\n}",
                "Меч поджигает тех, кого бьёт."
            ),
            outer[0]
        );
        SkillTree.Node frost = after(
            passive(
                t,
                "",
                "Ледяной меч",
                "minecraft:blue_ice",
                rx(0.0, c) + 40,
                ry(0.0, c),
                3,
                "on attack {\n    if contains(helditem(player), \"sword\") {\n        aoe at evententity() radius 1 effect \"minecraft:slowness\" time 3s level 2\n        particles \"minecraft:snowflake\" at evententity() count 10 spread 0.4\n    }\n}",
                "Меч замедляет тех, кого бьёт."
            ),
            outer[0]
        );
        after(
            key(
                    t,
                    "",
                    "Вихрь",
                    "minecraft:netherite_sword",
                    rx(0.0, d),
                    ry(0.0, d),
                    5,
                    "r",
                    10.0F,
                    "aoe at player radius 3.5 damage (3 + skillpower() * 2) push 0.5\nsound \"minecraft:entity.player.attack.sweep\"\nparticles \"minecraft:sweep_attack\" at player count 8 spread 1.5",
                    "R: удар по всем вокруг. Растёт с рангом."
                )
                .ranks(3, 2),
            fire,
            frost
        );
        inner[1] = after(
            stat(t, "", "Быстрые руки", "minecraft:golden_sword", rx(1.0, a), ry(1.0, a), 1, 3, "attack_speed", 0.1F, "Скорость ударов +10% за ранг."), core
        );
        outer[1] = after(
            stat(t, "", "Тяжёлая рука", "minecraft:iron_axe", rx(1.0, b), ry(1.0, b), 2, 3, "attack_knockback", 0.25F, "Удар отбрасывает дальше."), inner[1]
        );
        SkillTree.Node vamp = after(
            passive(
                    t,
                    "",
                    "Вампиризм",
                    "minecraft:ghast_tear",
                    rx(1.0, c),
                    ry(1.0, c),
                    3,
                    "on attack {\n    if contains(helditem(player), \"sword\") {\n        aoe at player radius 0.5 allies heal (skillpower())\n        particles \"minecraft:heart\" at player count 2 spread 0.4\n    }\n}",
                    "Удар мечом лечит, ранг — сильнее."
                )
                .ranks(2, 2),
            outer[1]
        );
        after(
            passive(
                    t,
                    "",
                    "Жажда",
                    "minecraft:rotten_flesh",
                    rx(1.0, d),
                    ry(1.0, d),
                    4,
                    "on kill {\n    aoe at player radius 0.5 allies heal (2 * skillpower())\n}",
                    "Убийство лечит."
                )
                .ranks(2, 2),
            vamp
        );
        inner[2] = after(stat(t, "", "Скорость", "minecraft:sugar", rx(2.0, a), ry(2.0, a), 1, 3, "move_speed", 0.1F, "Скорость +10% за ранг."), core);
        outer[2] = after(
            stat(t, "", "Лёгкий шаг", "minecraft:leather_boots", rx(2.0, b), ry(2.0, b), 2, 2, "step_height", 0.2F, "Заходишь на ступени выше."), inner[2]
        );
        SkillTree.Node dash = after(
            key(
                t,
                "",
                "Рывок",
                "minecraft:feather",
                rx(2.0, c),
                ry(2.0, c),
                3,
                "f",
                6.0F,
                "motion forward player 1.6 up 0.2 safe\nsound \"minecraft:entity.ender_dragon.flap\"\nparticles \"minecraft:cloud\" at player count 10 spread 0.5",
                "F: резкий рывок туда, куда смотришь."
            ),
            outer[2]
        );
        after(
            key(
                t,
                "",
                "Спринт",
                "minecraft:rabbit_foot",
                rx(2.0, d),
                ry(2.0, d),
                3,
                "g",
                25.0F,
                "effect \"minecraft:speed\" 10s level 2\nsound \"minecraft:entity.horse.gallop\"",
                "G: десять секунд очень быстро."
            ),
            dash
        );
        inner[3] = after(
            stat(t, "", "Прыгучесть", "minecraft:slime_ball", rx(3.0, a), ry(3.0, a), 1, 3, "jump_power", 0.1F, "Прыжок выше на 10% за ранг."), core
        );
        outer[3] = after(
            stat(t, "", "Мягкая посадка", "minecraft:hay_block", rx(3.0, b), ry(3.0, b), 2, 3, "fall_damage", -0.25F, "Урон от падения -25% за ранг."),
            inner[3]
        );
        SkillTree.Node jump = after(
            passive(
                    t,
                    "",
                    "Двойной прыжок",
                    "minecraft:firework_rocket",
                    rx(3.0, c),
                    ry(3.0, c),
                    4,
                    "var jumps = 0\non keypress \"jump\" {\n    if event(\"onground\") {\n        set jumps = 0\n    } else if jumps < skillpower() {\n        set jumps = jumps + 1\n        motion jump player 0.55 safe\n        particles \"minecraft:cloud\" at player count 12 spread 0.4\n        sound \"minecraft:entity.phantom.flap\"\n    }\n}",
                    "Ещё один прыжок в воздухе. Второй ранг — ещё один."
                )
                .ranks(2, 3),
            outer[3]
        );
        after(
            passive(
                t,
                "",
                "Удар с неба",
                "minecraft:anvil",
                rx(3.0, d) - 40,
                ry(3.0, d),
                3,
                "on keypress \"sneak\" {\n    if not event(\"onground\") {\n        motion jump player -1.4\n        particles \"minecraft:cloud\" at player count 8 spread 0.3\n    }\n}",
                "В воздухе присядь: камнем вниз."
            ),
            jump
        );
        after(
            key(
                t,
                "",
                "Ракета",
                "minecraft:gunpowder",
                rx(3.0, d) + 40,
                ry(3.0, d),
                3,
                "v",
                12.0F,
                "motion jump player 1.3 safe\nsound \"minecraft:entity.firework_rocket.launch\"\nparticles \"minecraft:firework\" at player count 20 spread 0.4",
                "V: высоко вверх."
            ),
            jump
        );
        inner[4] = after(
            stat(t, "", "Скорость добычи", "minecraft:iron_pickaxe", rx(4.0, a), ry(4.0, a), 1, 3, "mine_speed", 0.1F, "Добыча быстрее на 10% за ранг."), core
        );
        outer[4] = after(
            stat(t, "", "Рудокоп", "minecraft:diamond_pickaxe", rx(4.0, b), ry(4.0, b), 2, 3, "mine_speed", 0.1F, "Ещё быстрее на 10% за ранг."), inner[4]
        );
        SkillTree.Node pick3 = after(
            passive(t, "", "Кирка 3 на 1", "minecraft:golden_pickaxe", rx(4.0, c), ry(4.0, c), 4, areaTool("pickaxe", 1, 3), "Кирка ломает три блока в высоту."),
            outer[4]
        );
        after(
            passive(
                    t,
                    "",
                    "Кирка 3 на 3",
                    "minecraft:netherite_pickaxe",
                    rx(4.0, d),
                    ry(4.0, d),
                    8,
                    areaTool("pickaxe", 3, 3),
                    "Кирка ломает девять блоков разом."
                )
                .at(15),
            pick3
        );
        inner[5] = after(stat(t, "", "Длинные руки", "minecraft:stick", rx(5.0, a), ry(5.0, a), 1, 2, "reach", 0.5F, "Достаёшь до блоков дальше."), core);
        outer[5] = after(
            passive(
                t,
                "",
                "Лесоруб",
                "minecraft:iron_axe",
                rx(5.0, b),
                ry(5.0, b),
                3,
                "on break {\n    if contains(event(\"block\"), \"_log\") and contains(helditem(player), \"_axe\") {\n        for dy in range(1, 8) {\n            for b in list((event(\"x\"), event(\"y\") + dy, event(\"z\"))) {\n                if contains(block(b), \"_log\") {\n                    breakblock b\n                }\n            }\n        }\n    }\n}",
                "Топор валит ствол целиком."
            ),
            inner[5]
        );
        SkillTree.Node spade = after(
            passive(t, "", "Широкая лопата", "minecraft:iron_shovel", rx(5.0, c), ry(5.0, c), 3, areaTool("shovel", 3, 3), "Лопата копает три на три."),
            outer[5]
        );
        after(
            key(
                t,
                "",
                "Скачок",
                "minecraft:ender_pearl",
                rx(5.0, d),
                ry(5.0, d),
                5,
                "x",
                15.0F,
                "teleport aimpoint(player, 10)\nsound \"minecraft:entity.enderman.teleport\"",
                "X: мгновенно туда, куда смотришь."
            ),
            spade
        );
        inner[6] = after(stat(t, "", "Здоровье", "minecraft:golden_apple", rx(6.0, a), ry(6.0, a), 1, 5, "max_health", 2.0F, "Одно сердце за ранг."), core);
        outer[6] = after(stat(t, "", "Броня", "minecraft:iron_chestplate", rx(6.0, b), ry(6.0, b), 2, 3, "armor", 1.0F, "Броня +1 за ранг."), inner[6]);
        SkillTree.Node thorns = after(
            passive(
                    t,
                    "",
                    "Шипы",
                    "minecraft:cactus",
                    rx(6.0, c),
                    ry(6.0, c),
                    3,
                    "on hurt {\n    aoe at evententity() radius 1 damage (1 + skillpower())\n}",
                    "Кто тебя ударил, сам получит."
                )
                .ranks(2, 2),
            outer[6]
        );
        after(
            key(
                t,
                "",
                "Толчок",
                "minecraft:piston",
                rx(6.0, d),
                ry(6.0, d),
                3,
                "c",
                10.0F,
                "shockwave at player radius 5 power 1.1\nsound \"minecraft:entity.generic.explode\"",
                "C: отбросить всех вокруг."
            ),
            thorns
        );
        inner[7] = after(stat(t, "", "Удача", "minecraft:gold_nugget", rx(7.0, a), ry(7.0, a), 1, 3, "luck", 0.5F, "Добыча из сундуков и рыбалки лучше."), core);
        outer[7] = after(
            stat(t, "", "Опытность", "minecraft:experience_bottle", rx(7.0, b), ry(7.0, b), 2, 3, "xp_gain", 0.1F, "Опыта Minecraft +10% за ранг."), inner[7]
        );
        SkillTree.Node full = after(
            stat(t, "", "Сытость", "minecraft:bread", rx(7.0, c), ry(7.0, c), 2, 3, "hunger_speed", -0.1F, "Голод растёт на 10% медленнее за ранг."), outer[7]
        );
        after(
            key(
                t,
                "",
                "Притянуть",
                "minecraft:fishing_rod",
                rx(7.0, d),
                ry(7.0, d),
                3,
                "h",
                8.0F,
                "motion toward lookentity(player, 16) player 1.3 up 0.3\nsound \"minecraft:entity.fishing_bobber.retrieve\"",
                "H: притянуть к себе того, на кого смотришь."
            ),
            full
        );
        String[][] between = new String[][]{
            {"Боевой дух", "minecraft:redstone", "attack_damage", "0.05", "Урон +5%."},
            {"Быстрый удар", "minecraft:gold_ingot", "attack_speed", "0.1", "Скорость ударов +10%."},
            {"Попутный ветер", "minecraft:lead", "move_speed", "0.1", "Скорость +10%."},
            {"Лёгкость", "minecraft:phantom_membrane", "gravity", "-0.1", "Падаешь чуть медленнее."},
            {"Закалка", "minecraft:iron_ingot", "mine_speed", "0.1", "Добыча +10%."},
            {"Живая сила", "minecraft:apple", "max_health", "2", "Одно сердце."},
            {"Стойкость", "minecraft:obsidian", "knockback_resist", "0.2", "Удары меньше сдвигают."},
            {"Везунчик", "minecraft:emerald", "luck", "0.5", "Ещё удачи."}
        };

        for (int i = 0; i < 8; i++) {
            String[] g = between[i];
            SkillTree.Node link = stat(
                t, "", g[0], g[1], rx(i + 0.5, 230), ry(i + 0.5, 230), 2, 1, g[2], Float.parseFloat(g[3]), g[4] + " Нужны оба соседних луча."
            );
            after(link, outer[i], outer[(i + 1) % 8]);
            link.parentMode = "all";
        }

        done(t, "tree", "hover");
        t.menus.get(0).zoom = 0.0F;
        t.menus.get(0).userZoom = true;
        return t;
    }

    private static SkillTree craft() {
        SkillTree t = base("Ремесло", 8112236);
        String f = "Бой";
        String m = "Добыча";
        String w = "Движение";
        SkillTree.Node s1 = stat(t, f, "Урон", "minecraft:iron_sword", 0, 0, 1, 5, "attack_damage", 0.05F, "Урон +5% за ранг.");
        SkillTree.Node s2 = after(
            stat(t, f, "Частые удары", "minecraft:golden_sword", 90, -60, 1, 3, "attack_speed", 0.1F, "Скорость ударов +10% за ранг."), s1
        );
        SkillTree.Node s3 = after(stat(t, f, "Здоровье", "minecraft:golden_apple", 90, 60, 1, 5, "max_health", 2.0F, "Одно сердце за ранг."), s1);
        SkillTree.Node s4 = after(
            passive(
                t,
                f,
                "Огненный меч",
                "minecraft:blaze_powder",
                180,
                -90,
                3,
                "on attack {\n    if contains(helditem(player), \"sword\") {\n        ignite evententity() 5s\n        particles \"minecraft:flame\" at evententity() count 10 spread 0.4\n    }\n}",
                "Меч поджигает."
            ),
            s2
        );
        SkillTree.Node s5 = after(
            passive(
                t,
                f,
                "Ледяной меч",
                "minecraft:blue_ice",
                180,
                -30,
                3,
                "on attack {\n    if contains(helditem(player), \"sword\") {\n        aoe at evententity() radius 1 effect \"minecraft:slowness\" time 3s level 2\n        particles \"minecraft:snowflake\" at evententity() count 10 spread 0.4\n    }\n}",
                "Меч замедляет."
            ),
            s2
        );
        SkillTree.Node s6 = after(stat(t, f, "Броня", "minecraft:iron_chestplate", 180, 60, 2, 3, "armor", 1.0F, "Броня +1 за ранг."), s3);
        SkillTree.Node s7 = after(
            passive(
                    t,
                    f,
                    "Вампиризм",
                    "minecraft:ghast_tear",
                    270,
                    -60,
                    4,
                    "on attack {\n    if contains(helditem(player), \"sword\") {\n        aoe at player radius 0.5 allies heal (skillpower())\n        particles \"minecraft:heart\" at player count 2 spread 0.4\n    }\n}",
                    "Удар мечом лечит."
                )
                .ranks(2, 2),
            s4,
            s5
        );
        SkillTree.Node s8 = after(
            passive(
                    t,
                    f,
                    "Шипы",
                    "minecraft:cactus",
                    270,
                    60,
                    3,
                    "on hurt {\n    aoe at evententity() radius 1 damage (1 + skillpower())\n}",
                    "Кто ударил, сам получит."
                )
                .ranks(2, 2),
            s6
        );
        SkillTree.Node s9 = after(
            key(
                    t,
                    f,
                    "Вихрь",
                    "minecraft:netherite_sword",
                    360,
                    -60,
                    5,
                    "r",
                    10.0F,
                    "aoe at player radius 3.5 damage (3 + skillpower() * 2) push 0.5\nsound \"minecraft:entity.player.attack.sweep\"\nparticles \"minecraft:sweep_attack\" at player count 8 spread 1.5",
                    "R: удар по всем вокруг. Растёт с рангом."
                )
                .ranks(3, 2)
                .at(10),
            s7
        );
        after(
            key(
                t,
                f,
                "Толчок",
                "minecraft:piston",
                360,
                60,
                3,
                "c",
                10.0F,
                "shockwave at player radius 5 power 1.1\nsound \"minecraft:entity.generic.explode\"",
                "C: отбросить всех вокруг."
            ),
            s8
        );
        after(
            passive(
                    t,
                    f,
                    "Жажда",
                    "minecraft:rotten_flesh",
                    450,
                    -60,
                    4,
                    "on kill {\n    aoe at player radius 0.5 allies heal (2 * skillpower())\n}",
                    "Убийство лечит."
                )
                .ranks(2, 2),
            s9
        );
        SkillTree.Node m1 = stat(t, m, "Скорость добычи", "minecraft:iron_pickaxe", 0, 0, 1, 5, "mine_speed", 0.1F, "Добыча быстрее на 10% за ранг.");
        SkillTree.Node m2 = after(
            passive(t, m, "Кирка 3 на 1", "minecraft:golden_pickaxe", 90, -60, 3, areaTool("pickaxe", 1, 3), "Кирка ломает три блока в высоту."), m1
        );
        SkillTree.Node m3 = after(
            passive(
                t,
                m,
                "Лесоруб",
                "minecraft:iron_axe",
                90,
                60,
                3,
                "on break {\n    if contains(event(\"block\"), \"_log\") and contains(helditem(player), \"_axe\") {\n        for dy in range(1, 8) {\n            for b in list((event(\"x\"), event(\"y\") + dy, event(\"z\"))) {\n                if contains(block(b), \"_log\") {\n                    breakblock b\n                }\n            }\n        }\n    }\n}",
                "Топор валит ствол целиком."
            ),
            m1
        );
        SkillTree.Node m4 = after(
            passive(t, m, "Широкая кирка", "minecraft:diamond_pickaxe", 180, -60, 5, areaTool("pickaxe", 3, 1), "Кирка ломает три блока в ширину."), m2
        );
        SkillTree.Node m5 = after(
            passive(t, m, "Широкая лопата", "minecraft:iron_shovel", 180, 60, 3, areaTool("shovel", 3, 3), "Лопата копает три на три."), m3
        );
        after(
            passive(t, m, "Кирка 3 на 3", "minecraft:netherite_pickaxe", 270, -60, 8, areaTool("pickaxe", 3, 3), "Кирка ломает девять блоков разом.").at(15),
            m4
        );
        SkillTree.Node m6 = after(stat(t, m, "Длинные руки", "minecraft:stick", 270, 60, 2, 2, "reach", 0.5F, "Достаёшь дальше."), m5);
        after(stat(t, m, "Удача", "minecraft:gold_nugget", 360, 60, 2, 3, "luck", 0.5F, "Добыча из сундуков и рыбалки лучше."), m6);
        after(
            node(t, m, "Ночное зрение", "minecraft:golden_carrot", 90, 0, 3, "В шахте светло.").gives(SkillTree.Effect.potion("minecraft:night_vision", 1)), m1
        );
        SkillTree.Node w1 = stat(t, w, "Скорость", "minecraft:sugar", 0, 0, 1, 5, "move_speed", 0.1F, "Скорость +10% за ранг.");
        SkillTree.Node w2 = after(stat(t, w, "Прыгучесть", "minecraft:slime_ball", 90, -60, 1, 3, "jump_power", 0.1F, "Прыжок выше на 10% за ранг."), w1);
        SkillTree.Node w3 = after(
            key(
                t,
                w,
                "Рывок",
                "minecraft:feather",
                90,
                60,
                2,
                "f",
                6.0F,
                "motion forward player 1.6 up 0.2 safe\nsound \"minecraft:entity.ender_dragon.flap\"\nparticles \"minecraft:cloud\" at player count 10 spread 0.5",
                "F: резкий рывок."
            ),
            w1
        );
        SkillTree.Node w4 = after(
            passive(
                    t,
                    w,
                    "Двойной прыжок",
                    "minecraft:firework_rocket",
                    180,
                    -60,
                    4,
                    "var jumps = 0\non keypress \"jump\" {\n    if event(\"onground\") {\n        set jumps = 0\n    } else if jumps < skillpower() {\n        set jumps = jumps + 1\n        motion jump player 0.55 safe\n        particles \"minecraft:cloud\" at player count 12 spread 0.4\n        sound \"minecraft:entity.phantom.flap\"\n    }\n}",
                    "Ещё один прыжок в воздухе. Второй ранг — ещё один."
                )
                .ranks(2, 3),
            w2
        );
        SkillTree.Node w5 = after(stat(t, w, "Мягкая посадка", "minecraft:hay_block", 180, 0, 2, 3, "fall_damage", -0.25F, "Урон от падения -25% за ранг."), w2);
        SkillTree.Node w6 = after(
            key(
                t,
                w,
                "Спринт",
                "minecraft:rabbit_foot",
                180,
                60,
                3,
                "g",
                25.0F,
                "effect \"minecraft:speed\" 10s level 2\nsound \"minecraft:entity.horse.gallop\"",
                "G: десять секунд очень быстро."
            ),
            w3
        );
        after(
            passive(
                t,
                w,
                "Удар с неба",
                "minecraft:anvil",
                270,
                -90,
                3,
                "on keypress \"sneak\" {\n    if not event(\"onground\") {\n        motion jump player -1.4\n        particles \"minecraft:cloud\" at player count 8 spread 0.3\n    }\n}",
                "В воздухе присядь: камнем вниз."
            ),
            w4
        );
        after(
            key(
                t,
                w,
                "Ракета",
                "minecraft:gunpowder",
                270,
                -30,
                3,
                "v",
                12.0F,
                "motion jump player 1.3 safe\nsound \"minecraft:entity.firework_rocket.launch\"\nparticles \"minecraft:firework\" at player count 20 spread 0.4",
                "V: высоко вверх."
            ),
            w4,
            w5
        );
        SkillTree.Node w7 = after(
            key(
                    t,
                    w,
                    "Скачок",
                    "minecraft:ender_pearl",
                    270,
                    60,
                    5,
                    "x",
                    15.0F,
                    "teleport aimpoint(player, 10)\nsound \"minecraft:entity.enderman.teleport\"",
                    "X: мгновенно туда, куда смотришь."
                )
                .at(10),
            w6
        );
        after(
            key(
                t,
                w,
                "Притянуть",
                "minecraft:fishing_rod",
                360,
                60,
                3,
                "h",
                8.0F,
                "motion toward lookentity(player, 16) player 1.3 up 0.3\nsound \"minecraft:entity.fishing_bobber.retrieve\"",
                "H: притянуть того, на кого смотришь."
            ),
            w7
        );
        return done(t, "tree", "side");
    }

    private static SkillTree paths() {
        SkillTree t = base("Три пути", 13146940);
        SkillTree.Node start = node(t, "", "Развилка", "minecraft:compass", 0, 0, 1, "Отсюда три дороги, идти можно только по одной.");
        SkillTree.Node fighter = after(
            stat(t, "", "Путь бойца", "minecraft:iron_sword", 90, -120, 1, 1, "attack_damage", 0.1F, "Выбор пути. Урон +10%."), start
        );
        SkillTree.Node miner = after(stat(t, "", "Путь рудокопа", "minecraft:iron_pickaxe", 90, 0, 1, 1, "mine_speed", 0.2F, "Выбор пути. Добыча +20%."), start);
        SkillTree.Node runner = after(
            stat(t, "", "Путь странника", "minecraft:leather_boots", 90, 120, 1, 1, "move_speed", 0.15F, "Выбор пути. Скорость +15%."), start
        );

        for (SkillTree.Node n : new SkillTree.Node[]{fighter, miner, runner}) {
            n.group = "путь";
        }

        SkillTree.Node f1 = after(stat(t, "", "Сила", "minecraft:diamond_sword", 180, -160, 1, 3, "attack_damage", 0.05F, "Урон +5% за ранг."), fighter);
        SkillTree.Node f2 = after(stat(t, "", "Здоровье", "minecraft:golden_apple", 180, -100, 1, 3, "max_health", 2.0F, "Одно сердце за ранг."), fighter);
        SkillTree.Node f3 = after(
            passive(
                t,
                "",
                "Огненный меч",
                "minecraft:blaze_powder",
                270,
                -160,
                3,
                "on attack {\n    if contains(helditem(player), \"sword\") {\n        ignite evententity() 5s\n        particles \"minecraft:flame\" at evententity() count 10 spread 0.4\n    }\n}",
                "Меч поджигает."
            ),
            f1
        );
        SkillTree.Node f4 = after(
            passive(
                    t,
                    "",
                    "Шипы",
                    "minecraft:cactus",
                    270,
                    -100,
                    3,
                    "on hurt {\n    aoe at evententity() radius 1 damage (1 + skillpower())\n}",
                    "Кто ударил, сам получит."
                )
                .ranks(2, 2),
            f2
        );
        SkillTree.Node f5 = after(
            key(
                    t,
                    "",
                    "Вихрь",
                    "minecraft:netherite_sword",
                    360,
                    -130,
                    5,
                    "r",
                    10.0F,
                    "aoe at player radius 3.5 damage (3 + skillpower() * 2) push 0.5\nsound \"minecraft:entity.player.attack.sweep\"\nparticles \"minecraft:sweep_attack\" at player count 8 spread 1.5",
                    "R: удар по всем вокруг. Растёт с рангом."
                )
                .ranks(3, 2),
            f3,
            f4
        );
        after(
            passive(
                    t,
                    "",
                    "Жажда",
                    "minecraft:rotten_flesh",
                    450,
                    -130,
                    4,
                    "on kill {\n    aoe at player radius 0.5 allies heal (2 * skillpower())\n}",
                    "Убийство лечит."
                )
                .ranks(2, 2),
            f5
        );
        SkillTree.Node m1 = after(stat(t, "", "Скорость добычи", "minecraft:golden_pickaxe", 180, -30, 1, 3, "mine_speed", 0.1F, "Добыча +10% за ранг."), miner);
        SkillTree.Node m2 = after(stat(t, "", "Удача", "minecraft:gold_nugget", 180, 30, 1, 3, "luck", 0.5F, "Добыча лучше."), miner);
        SkillTree.Node m3 = after(
            passive(t, "", "Кирка 3 на 1", "minecraft:diamond_pickaxe", 270, -30, 3, areaTool("pickaxe", 1, 3), "Три блока в высоту."), m1
        );
        SkillTree.Node m4 = after(
            passive(
                t,
                "",
                "Лесоруб",
                "minecraft:iron_axe",
                270,
                30,
                3,
                "on break {\n    if contains(event(\"block\"), \"_log\") and contains(helditem(player), \"_axe\") {\n        for dy in range(1, 8) {\n            for b in list((event(\"x\"), event(\"y\") + dy, event(\"z\"))) {\n                if contains(block(b), \"_log\") {\n                    breakblock b\n                }\n            }\n        }\n    }\n}",
                "Топор валит ствол целиком."
            ),
            m2
        );
        SkillTree.Node m5 = after(
            passive(t, "", "Кирка 3 на 3", "minecraft:netherite_pickaxe", 360, 0, 8, areaTool("pickaxe", 3, 3), "Девять блоков разом.").at(15), m3, m4
        );
        after(
            node(t, "", "Ночное зрение", "minecraft:golden_carrot", 450, 0, 3, "В шахте светло.").gives(SkillTree.Effect.potion("minecraft:night_vision", 1)),
            m5
        );
        SkillTree.Node r1 = after(stat(t, "", "Скорость", "minecraft:sugar", 180, 100, 1, 3, "move_speed", 0.1F, "Скорость +10% за ранг."), runner);
        SkillTree.Node r2 = after(stat(t, "", "Прыгучесть", "minecraft:slime_ball", 180, 160, 1, 3, "jump_power", 0.1F, "Прыжок выше на 10% за ранг."), runner);
        SkillTree.Node r3 = after(
            key(
                t,
                "",
                "Рывок",
                "minecraft:feather",
                270,
                100,
                3,
                "f",
                6.0F,
                "motion forward player 1.6 up 0.2 safe\nsound \"minecraft:entity.ender_dragon.flap\"\nparticles \"minecraft:cloud\" at player count 10 spread 0.5",
                "F: резкий рывок."
            ),
            r1
        );
        SkillTree.Node r4 = after(
            passive(
                    t,
                    "",
                    "Двойной прыжок",
                    "minecraft:firework_rocket",
                    270,
                    160,
                    4,
                    "var jumps = 0\non keypress \"jump\" {\n    if event(\"onground\") {\n        set jumps = 0\n    } else if jumps < skillpower() {\n        set jumps = jumps + 1\n        motion jump player 0.55 safe\n        particles \"minecraft:cloud\" at player count 12 spread 0.4\n        sound \"minecraft:entity.phantom.flap\"\n    }\n}",
                    "Ещё один прыжок в воздухе. Второй ранг — ещё один."
                )
                .ranks(2, 3),
            r2
        );
        SkillTree.Node r5 = after(
            key(
                t,
                "",
                "Скачок",
                "minecraft:ender_pearl",
                360,
                130,
                5,
                "x",
                15.0F,
                "teleport aimpoint(player, 10)\nsound \"minecraft:entity.enderman.teleport\"",
                "X: мгновенно туда, куда смотришь."
            ),
            r3,
            r4
        );
        after(
            passive(
                t,
                "",
                "Удар с неба",
                "minecraft:anvil",
                450,
                130,
                3,
                "on keypress \"sneak\" {\n    if not event(\"onground\") {\n        motion jump player -1.4\n        particles \"minecraft:cloud\" at player count 8 spread 0.3\n    }\n}",
                "В воздухе присядь: камнем вниз."
            ),
            r5
        );
        return done(t, "fog", "side");
    }
}
