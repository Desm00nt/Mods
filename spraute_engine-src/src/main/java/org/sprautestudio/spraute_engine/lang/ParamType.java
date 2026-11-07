package org.sprautestudio.spraute_engine.lang;

public enum ParamType {
    TEXT(
        "текст",
        "Строка в кавычках. Поддерживает подстановки: \"Привет, {имя}\". Прокачка: {skill:level} — уровень в первой системе проекта, {skill:магия:level}, следующий — {skill:nextlevel}, а также xp, xpnext, maxlevel, title, points[:вид], rank:навык"
    ),
    INT("целое", "Целое число"),
    NUMBER("число", "Число, можно дробное: 0.35"),
    DURATION("время", "Длительность: 20t (тики), 1.5s (секунды), 2m (минуты)"),
    CHAR("персонаж", "Имя персонажа, объявленного через character"),
    LOC("точка", "Имя точки (location) или координаты (x, y, z)"),
    TARGET("цель", "player — игрок, имя персонажа, \"minecraft:zombie\" — ближайший моб этого типа (\"mob\" — любой), точка или (x, y, z)"),
    SPEAKER("говорящий", "Слот речи (0 или 1), персонаж, player (сам игрок — в чате его лицо) или имя в кавычках"),
    LABEL("метка", "Имя метки, объявленной через label"),
    BOOL("да/нет", "true или false"),
    EXPR("выражение", "Любое выражение: hp > 5 and flag(\"x\")"),
    SOUND("звук", "Id звука: \"minecraft:entity.player.levelup\""),
    ITEM("предмет", "Id предмета: \"minecraft:diamond\""),
    EFFECT("эффект", "Id эффекта: \"minecraft:blindness\""),
    ENTITY("сущность", "Id типа сущности: \"spraute:npc\" или модель проекта: \"spraute:Робот\""),
    EMOTION("эмоция", "Эмоция диалога: HAPPY_NEW, SAD1, ANGRY..."),
    KEY("клавиша", "Клавиша: F8, F9, F10, G, SPACE..."),
    SCRIPT("скрипт", "Числовой id сюжета"),
    ANIM("анимация", "Анимация: \"entity/humanoid!walk\" или своя у модели: \"spraute:Робот/walk\". Смотрите вкладку «Ассеты»"),
    LAYER("слой", "Слой анимации: action (всё тело, перекрывает), ambient, emotion (лицо), eyes или номер 0–20 (накладываемые слои)"),
    MOVETYPE("тип движения", "Состояние персонажа: idle (стоит), walk (идёт), fastwalk (быстро идёт), run (бежит)"),
    FUNC("функция", "Имя функции, объявленной через func"),
    BRANCH("ветка", "Имя асинхронной ветки (async имя { } или async call ... as имя)"),
    IMAGE("картинка", "Картинка из папки «Картинки» проекта или встроенная иконка Spraute"),
    GUI("интерфейс", "Имя интерфейса из папки «Интерфейсы» проекта, в кавычках"),
    GUIEL("элемент", "Id элемента интерфейса в кавычках, например \"yes\""),
    VAR("переменная", "Переменная сюжета (var)"),
    CHOICE("вариант", "Выбор режима блока: меняет то, что блок делает"),
    EASE("плавность", "Плавность движения: smooth, linear, ease_in_out_sine, ease_out_cubic, ease_out_back, ease_out_bounce..."),
    CUTSCENE("катсцена", "Имя катсцены из папки «Катсцены» проекта, в кавычках"),
    SHOP("магазин", "Имя магазина из папки «Магазины» проекта, в кавычках"),
    QUEST("квест", "Имя квеста из папки «Квесты» проекта, в кавычках"),
    TASK("задача", "Id задачи квеста, в кавычках"),
    ZONE("зона", "Имя зоны из папки «Зоны» проекта, в кавычках"),
    PATH("путь", "Имя пути из папки «Пути» проекта, в кавычках"),
    FIGHT("стиль боя", "Стиль боя из папки «Стили боя» или встроенный: \"fists\", \"sword\", \"brute\", \"weak\", \"archer\", \"mage\""),
    VFX("эффект частиц", "Эффект из папки «Эффекты» проекта или встроенный: \"nuke\", \"sky_beam\", \"orbit_orbs\", \"explosion\"..."),
    POST("пост-эффект", "Вид из папки «Пост-эффекты» проекта или готовый: \"cinematic\", \"noir\", \"horror\", \"underwater\", \"pain\", \"vhs\"..."),
    STRUCTURE(
        "постройка",
        "Постройка из папки «Постройки», структура игры или мода (\"minecraft:village/plains/houses/plains_small_house_1\") или файл схемы из папок Create, WorldEdit, Litematica"
    ),
    BUILDSTYLE(
        "как строится", "Анимация: up, down, left, right, front, back, rain, rise, center, edges, spiral, tornado, scatter, assemble, wave, pop, instant"
    ),
    SKILLTREE("система прокачки", "Система из папки «Прокачка» проекта"),
    SKILLNODE("навык", "Навык системы прокачки"),
    SKILLMENU("меню прокачки", "Одно из меню системы прокачки"),
    STEALTH("стиль стелса", "Стиль стража из папки «Стили стелса» или готовый: \"guard\", \"sentry\", \"watcher\", \"beam\", \"blind\""),
    STEALTHSHAPE("форма", "Что страж видит на полу: fan — веер, circle — круг вокруг, rect — прямоугольник, triangle — треугольник"),
    PANELTURN("как повёрнут интерфейс", "fixed — как поставили, viewer — к смотрящему, camera — прямо в камеру, body — вместе с телом, bone — вместе с костью"),
    PANELLOOK("вид интерфейса", "Готовый вид: plate — табличка над головой, marker — метка сквозь стены, hologram — голограмма, sign — вывеска на теле"),
    SLOTORDER(
        "как закрывать ячейки",
        "top — рюкзак сверху вниз, bottom — снизу вверх, columns — столбцами справа (во всех хотбар последним), hotbar — сначала хотбар, list — свой список"
    ),
    SLOTSET(
        "ячейки",
        "Ячейки инвентаря в кавычках: \"3\", \"3-7\", \"hotbar\", \"main\", \"armor\", \"offhand\", \"all\" или несколько через запятую. 1–9 — хотбар, 10–36 — остальное"
    ),
    PLAYERCAN(
        "что может игрок",
        "Что можно запретить: move, jump, sprint, sneak, look, climb, elytra, ride, sleep, break, place, interact, containers, use_item, eat, attack, interact_entity, drop, pickup, pickup_xp, inventory, swap_hands, hotbar, take_damage, drown, burn, fly"
    ),
    PLAYERSTAT(
        "число игрока",
        "Что можно настроить: reach, attack_reach, move_speed, jump_power, mine_speed, fly_speed, gravity, step_height, attack_damage, attack_speed, attack_knockback, knockback_resist, max_health, armor, armor_toughness, luck, damage_taken, fall_damage, hunger_speed, xp_gain"
    ),
    PLAYERRULE("правило игрока", "Любое правило игрока: и запрет, и число"),
    CAMRIG(
        "камера игрока",
        "Камера из папки «Камеры» проекта или готовая: \"gta\", \"third_person\", \"over_the_shoulder\", \"souls\", \"top_down\", \"isometric\", \"side_x\", \"side_z\"..."
    ),
    CAMPARAM("настройка камеры", "Одна настройка: distance, x, y, z, yaw, pitch, lock_x, lock_y, lock_z, lock_yaw, lock_pitch, fov..."),
    POSTPARAM("настройка пост-эффекта", "Одна настройка: bloom, vignette, fog, contrast, saturation, dof, chromatic..."),
    SYSTEM(
        "система",
        "Система движка: health (полоска HP), coins (монеты), time (время суток), inventory (свой инвентарь), slots (блокировка слотов), chat (скрывать чат)"
    );

    public final String title;
    public final String doc;

    private ParamType(String title, String doc) {
        this.title = title;
        this.doc = doc;
    }

    public static ParamType byName(String s) {
        return switch (s) {
            case "text" -> TEXT;
            case "int" -> INT;
            case "number" -> NUMBER;
            case "duration" -> DURATION;
            case "char" -> CHAR;
            case "loc" -> LOC;
            case "target" -> TARGET;
            case "speaker" -> SPEAKER;
            case "label" -> LABEL;
            case "bool" -> BOOL;
            case "expr" -> EXPR;
            case "sound" -> SOUND;
            case "item" -> ITEM;
            case "effect" -> EFFECT;
            case "entity" -> ENTITY;
            case "emotion" -> EMOTION;
            case "key" -> KEY;
            case "script" -> SCRIPT;
            case "anim" -> ANIM;
            case "layer" -> LAYER;
            case "movetype" -> MOVETYPE;
            case "func" -> FUNC;
            case "branch" -> BRANCH;
            case "image" -> IMAGE;
            case "gui" -> GUI;
            case "guiel" -> GUIEL;
            case "var" -> VAR;
            case "choice" -> CHOICE;
            case "ease" -> EASE;
            case "cutscene" -> CUTSCENE;
            case "vfx" -> VFX;
            case "post" -> POST;
            case "postparam" -> POSTPARAM;
            case "structure" -> STRUCTURE;
            case "buildstyle" -> BUILDSTYLE;
            case "stealthshape" -> STEALTHSHAPE;
            case "stealth" -> STEALTH;
            case "skilltree" -> SKILLTREE;
            case "skillnode" -> SKILLNODE;
            case "skillmenu" -> SKILLMENU;
            case "panelturn" -> PANELTURN;
            case "panellook" -> PANELLOOK;
            case "slotorder" -> SLOTORDER;
            case "slotset" -> SLOTSET;
            case "playercan" -> PLAYERCAN;
            case "playerstat" -> PLAYERSTAT;
            case "playerrule" -> PLAYERRULE;
            case "camrig" -> CAMRIG;
            case "camparam" -> CAMPARAM;
            case "shop" -> SHOP;
            case "quest" -> QUEST;
            case "task" -> TASK;
            case "zone" -> ZONE;
            case "path" -> PATH;
            case "fight" -> FIGHT;
            case "system" -> SYSTEM;
            default -> throw new IllegalArgumentException("unknown param type " + s);
        };
    }
}
