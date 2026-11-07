package org.sprautestudio.spraute_engine.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sprautestudio.spraute_engine.core.anim.Emotions;

public final class Specs {
    private static final Map<String, CommandSpec> COMMANDS = new LinkedHashMap<>();
    private static final Map<String, Specs.FunctionSpec> FUNCTIONS = new LinkedHashMap<>();
    private static final Map<String, String> KEYWORD_DOCS = new LinkedHashMap<>();
    public static final String CAT_SPEECH = "Речь";
    public static final String CAT_WAIT = "Ожидание";
    public static final String CAT_CHARACTER = "Персонажи";
    public static final String CAT_PLAYER = "Игрок и мир";
    public static final String CAT_SCREEN = "Экран";
    public static final String CAT_LOGIC = "Логика";
    public static final String CAT_ANIM = "Анимации";
    public static final String CAT_GUI = "Интерфейс";
    public static final String CAT_CAMERA = "Камера";
    public static final String CAT_FX = "Эффекты";
    public static final String CAT_BOSS = "Бой и боссы";
    public static final String CAT_GENERAL = "Игра и меню";
    public static final String CAT_CRAFT = "Рецепты и предметы";
    public static final List<String> CATEGORIES = new ArrayList<>(
        List.of(
            "Речь",
            "Ожидание",
            "Персонажи",
            "Анимации",
            "Камера",
            "Эффекты",
            "Бой и боссы",
            "Интерфейс",
            "Игрок и мир",
            "Экран",
            "Логика",
            "Игра и меню",
            "Рецепты и предметы"
        )
    );
    public static final List<String> LAYERS = List.of("action", "ambient", "emotion", "eyes", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
    public static final List<String> MOVE_TYPES = List.of("idle", "walk", "fastwalk", "run");
    public static final List<String> ENGINE_SYSTEMS = List.of("health", "coins", "time", "inventory", "slots", "chat");
    public static final List<String> SPEECH_ICONS = List.of("generic_man");
    public static final List<String> KEYS;
    public static final List<String> EMOTIONS;
    public static final List<String> ST_ENTITIES;

    private Specs() {
    }

    public static boolean fullOnly(CommandSpec spec) {
        String k = spec.key;
        return spec.category.equals("Бой и боссы")
            || k.startsWith("post ")
            || k.startsWith("build ")
            || k.startsWith("loot ")
            || k.startsWith("stealth ")
            || k.startsWith("skills ")
            || k.equals("cast")
            || k.equals("aoe")
            || k.equals("mob spawn")
            || k.equals("dungeon place")
            || k.endsWith(" mobspawn")
            || k.endsWith(" skilllearn")
            || k.endsWith(" skilllevel")
            || k.endsWith(" skilluse");
    }

    public static String systemTitle(String id) {
        return switch (id) {
            case "health" -> "полоска здоровья";
            case "coins" -> "монеты (монеты)";
            case "time" -> "время суток";
            case "inventory" -> "свой инвентарь";
            case "slots" -> "блокировка слотов";
            case "chat" -> "скрытие чата";
            default -> id;
        };
    }

    public static String defaultSpeechIcon(String entityType) {
        return "generic_man";
    }

    public static String layerName(String layer) {
        String l = layer.trim().toLowerCase();
        if (l.matches("\\d+")) {
            int n = Integer.parseInt(l);
            return n <= 20 ? "ANIMATION_" + n : null;
        } else {
            return switch (l) {
                case "action" -> "action";
                case "ambient" -> "ambient";
                case "emotion", "face" -> "emotion";
                case "eyes" -> "EYES";
                default -> null;
            };
        }
    }

    private static void cmd(String key, String cat, String syntax, String doc) {
        COMMANDS.put(key, new CommandSpec(key, cat, syntax, doc));
    }

    private static void fn(String name, String sig, String returns, String doc) {
        FUNCTIONS.put(name, new Specs.FunctionSpec(name, sig, returns, doc));
    }

    public static void addCommand(String key, String category, String syntax, String doc) {
        if (!CATEGORIES.contains(category)) {
            CATEGORIES.add(category);
        }

        cmd(key, category, syntax, doc);
    }

    public static void addFunction(String name, String signature, String returns, String doc) {
        fn(name, signature, returns, doc);
    }

    public static CommandSpec addEventCommand(EventDefs.Def d) {
        cmd("wait " + d.name(), "Ожидание", EventDefs.syntax(d), eventDoc(d));
        return COMMANDS.get("wait " + d.name());
    }

    private static String eventDoc(EventDefs.Def d) {
        return "Ждёт: "
            + d.title()
            + ". "
            + d.doc()
            + (d.filter() != null ? "\nФильтр (" + d.filterLabel() + ") необязателен." : "")
            + "\nДанные — event(\"поле\"): "
            + EventDefs.fieldsDoc(d)
            + "\nВ фоне при каждом событии: on "
            + d.name()
            + (d.filter() != null ? " ..." : "")
            + " { ... }";
    }

    public static Collection<CommandSpec> commands() {
        return Collections.unmodifiableCollection(COMMANDS.values());
    }

    public static CommandSpec command(String key) {
        return COMMANDS.get(key);
    }

    public static boolean isCommandHead(String word) {
        for (CommandSpec s : COMMANDS.values()) {
            if (s.head.equals(word)) {
                return true;
            }
        }

        return false;
    }

    public static List<CommandSpec> withHead(String head) {
        List<CommandSpec> list = new ArrayList<>();

        for (CommandSpec s : COMMANDS.values()) {
            if (s.head.equals(head)) {
                list.add(s);
            }
        }

        return list;
    }

    public static Collection<Specs.FunctionSpec> functions() {
        return Collections.unmodifiableCollection(FUNCTIONS.values());
    }

    public static Specs.FunctionSpec function(String name) {
        return FUNCTIONS.get(name);
    }

    public static Map<String, String> keywordDocs() {
        return Collections.unmodifiableMap(KEYWORD_DOCS);
    }

    public static int glfwKey(String name) {
        String n = name.toUpperCase();
        if (n.matches("F([1-9]|1[0-2])")) {
            return 290 + Integer.parseInt(n.substring(1)) - 1;
        } else if (n.length() == 1 && n.charAt(0) >= 'A' && n.charAt(0) <= 'Z') {
            return n.charAt(0);
        } else if (n.length() == 1 && n.charAt(0) >= '0' && n.charAt(0) <= '9') {
            return n.charAt(0);
        } else {
            return switch (n) {
                case "SPACE" -> 32;
                case "ENTER" -> 257;
                case "TAB" -> 258;
                case "SHIFT" -> 340;
                case "ESCAPE", "ESC" -> 256;
                default -> -1;
            };
        }
    }

    static {
        cmd(
            "speaker",
            "Речь",
            "<slot:int> <who:speaker>",
            "Назначает говорящего для слота речи (0 — слева, 1 — справа). Можно указать персонажа или имя в кавычках."
        );
        cmd(
            "say",
            "Речь",
            "<who:speaker> <text:text> [nowait]",
            "Реплика в окне речи Spraute. Говорящий — номер слота или персонаж, привязанный через speaker.\nСюжет ждёт, пока игрок прочитает реплику и нажмёт F8. С флагом nowait — сразу идёт дальше."
        );
        cmd("think", "Речь", "<text:text> [nowait]", "Мысль главного героя. Как и say, ждёт F8; nowait — не ждать.");
        cmd("clearchat", "Речь", "", "Очищает окно речи.");
        cmd(
            "title",
            "Речь",
            "<text:text> [sub <subtitle:text>] [time <fadeIn:duration> <stay:duration> <fadeOut:duration>]",
            "Показывает большой заголовок на экране."
        );
        cmd("message", "Речь", "<text:text>", "Обычное сообщение в чат всем игрокам.");
        cmd("web", "Речь", "<text:text>", "Сообщение в «веб»-ленту Spraute (интерфейс симулятора).");
        cmd("warn", "Речь", "<text:text>", "Предупреждение в веб-ленте Spraute.");
        cmd("wait", "Ожидание", "<time:duration>", "Пауза. Пример: wait 2s");
        cmd("wait random", "Ожидание", "<min:duration> <max:duration>", "Пауза случайной длины между min и max.");
        cmd("wait until", "Ожидание", "<condition:expr>", "Ждёт, пока условие не станет истинным. Пример: wait until near(camp, 4)");
        cmd("wait flag", "Ожидание", "<name:text>", "Ждёт установки флага сюжета.");
        cmd("wait key", "Ожидание", "<key:key>", "Ждёт нажатия клавиши игроком.");
        cmd("wait dimension", "Ожидание", "<dim:text>", "Ждёт, пока игрок окажется в измерении.");
        cmd("wait trigger", "Ожидание", "", "Ждёт команды /script trigger (или кнопки «Триггер» в панели сюжетов).");
        cmd(
            "wait chat",
            "Ожидание",
            "<words:text...>",
            "Ждёт, пока игрок напишет в чат одно из слов (регистр не важен). Что написал — event(\"text\"). В фоне: on chat \"привет\" { ... }"
        );
        cmd("wait click", "Ожидание", "<who:char>", "Ждёт клика игрока по персонажу.");
        cmd(
            "wait clickblock",
            "Ожидание",
            "[<pos:loc>] [block <blocktype:item>]",
            "Ждёт правого клика игрока по блоку: в этой точке, этого типа, того и другого сразу или, если ничего не указано, по любому. Примеры: wait clickblock (10, 64, 20); wait clickblock block \"minecraft:lever\"; wait clickblock (10, 64, 20) block \"minecraft:lever\". Что нажато — event(\"block\"), event(\"x\"), event(\"y\"), event(\"z\"). В фоне: on clickblock ... { ... }."
        );
        cmd(
            "wait lookat",
            "Ожидание",
            "<target:target>",
            "Ждёт, пока игрок посмотрит на персонажа, существо или точку: wait lookat alice, wait lookat (10, 70, 5)."
        );
        cmd("wait near", "Ожидание", "<target:target> [radius <radius:number>]", "Ждёт, пока игрок подойдёт к точке или персонажу (радиус по умолчанию 3).");
        cmd("wait enter", "Ожидание", "<from:loc> <to:loc>", "Ждёт, пока игрок войдёт в область между двумя углами.");
        cmd("wait exit", "Ожидание", "<from:loc> <to:loc>", "Ждёт, пока игрок выйдет из области.");
        cmd("wait visible", "Ожидание", "<who:char> [nosight]", "Ждёт, пока игрок увидит персонажа.");
        cmd(
            "spawn",
            "Персонажи",
            "<who:char> at <pos:target> [scale <scale:number>] [count <count:int>] [spread <spread:number>]",
            "Создаёт персонажа (если его ещё нет) и ставит к цели: точка, player, другой персонаж или моб (\"minecraft:zombie\"). scale — размер (2 — вдвое больше). count — сколько копий одного персонажа, обычным числом до 100: spawn zombie at arena count 10 spread 5. spread — на сколько блоков они разойдутся вокруг точки. Потом команды с этим именем действуют на всех живых копий сразу: move zombie to player, wait defeated zombie, despawn zombie. Сколько живых — copycount(zombie)."
        );
        cmd("tp", "Персонажи", "<who:char> to <pos:target>", "Мгновенно переносит персонажа к цели: точка, player, персонаж или моб.");
        cmd("despawn", "Персонажи", "<who:char>", "Убирает персонажа из мира.");
        cmd(
            "move",
            "Персонажи",
            "<who:char> to <target:target> [speed <speed:number>]",
            "Персонаж идёт к цели (точка, player, персонаж или моб); сюжет ждёт, пока он дойдёт. Скорость одна для всех персонажей: 1 — обычный шаг (2.6 бл/с), 0.5 — медленно, от 1.6 — быстрый шаг, от 2 — бег (по умолчанию 1)."
        );
        cmd(
            "path",
            "Персонажи",
            "<who:char> <points:loc...> [speed <speed:number>]",
            "Персонаж проходит маршрут через несколько точек. Скорость одна для всех: 1 — обычный шаг (2.6 бл/с), 0.5 — медленно, от 1.6 — быстрый шаг, от 2 — бег."
        );
        cmd(
            "walk",
            "Персонажи",
            "<who:char> <path:path> [speed <speed:number>] [from <from:number>] [loop] [pingpong] [nowait]",
            "Персонаж идёт по пути из папки «Пути» — от точки к точке, с паузами и анимациями точек. Сюжет ждёт, пока он дойдёт до конца (с nowait — сразу дальше). speed — одна для всех персонажей: 1 — обычный шаг (2.6 бл/с), 0.5 — медленно, от 1.6 — быстрый шаг, от 2 — бег. loop — по кругу, pingpong — туда-обратно (бесконечно, сюжет не ждёт). from 3 — начать с 3-й точки, from 0 — с ближайшей. Конец пути — событие pathend."
        );
        cmd("stopwalk", "Персонажи", "<who:char>", "Останавливает персонажа, идущего по пути.");
        cmd(
            "look",
            "Персонажи",
            "<who:char> at <target:target> [always] [instant] [wait]",
            "Персонаж смотрит на цель: player, другой персонаж, моб (\"minecraft:zombie\" или \"mob\" — ближайший) или точка.\nЦель держится, пока её не заменят или не снимут (stoplook). instant — повернуться сразу, wait — дождаться поворота."
        );
        cmd("stoplook", "Персонажи", "<who:char>", "Персонаж перестаёт поворачиваться и остаётся смотреть туда, куда смотрел.");
        cmd(
            "follow",
            "Персонажи",
            "<who:char> [target <target:target>] [speed <speed:number>] [distance <min:number> <max:number>] [noteleport]",
            "Персонаж следует за целью: игрок (по умолчанию) или другой персонаж. distance — ближе min не подходит; дальше max (6) отставший персонаж телепортируется к игроку. noteleport — никогда не телепортироваться, только идти: follow alice speed 1.3 noteleport."
        );
        cmd("walkspeed", "Персонажи", "<who:char> <speed:number>", "Скорость ходьбы персонажа.");
        cmd("wander", "Персонажи", "<who:char> [speed <speed:number>] [radius <radius:number>]", "Персонаж бродит вокруг случайным образом.");
        cmd("sit", "Персонажи", "<who:char> [at <pos:target>]", "Персонаж садится на невидимое сиденье; at — на заданную точку, поворот тела заблокирован.");
        cmd(
            "clearbehavior",
            "Персонажи",
            "<who:char> <type:text>",
            "Снимает поведение типа: movement, look, combat, speech, action, interaction, emotion, complex."
        );
        cmd("fly", "Персонажи", "<who:char> to <target:target> [speed <speed:number>]", "Прямолинейное движение без физики: для летающих и особых тел.");
        cmd("refuse", "Персонажи", "<who:char> [times <times:number>]", "Жест отказа.");
        cmd("stand", "Персонажи", "<who:char>", "Персонаж встаёт.");
        cmd("name", "Персонажи", "<who:char> <name:text>", "Меняет отображаемое имя персонажа.");
        cmd("hold", "Персонажи", "<who:char> <item:item>", "Даёт персонажу предмет в руку.");
        cmd(
            "drop",
            "Персонажи",
            "<who:char> [item <item:item>] [count <count:int>] [to <target:target>] [speed <speed:number>] [motion <motion:expr>] [into <var:var>]",
            "Персонаж выбрасывает предмет. Без item — то, что в руке (рука пустеет, с count — только столько штук); с item — этот предмет, рука не меняется. Летит туда, куда он смотрит, с to — к цели (точка, player, персонаж). speed — сила броска (0.3), motion — своя скорость по x, y, z: motion (0, 0.4, 0.2). into — переменная, куда положить предмет. Примеры: drop alice; drop alice item \"minecraft:apple\" count 3 to player speed 0.5."
        );
        cmd(
            "pickup",
            "Персонажи",
            "<who:char> [items <items:item...>] [count <count:int>] [off]",
            "Персонаж подбирает лежащие рядом предметы, сюжет при этом идёт дальше. Без items — любые; items ограничивает список, count — сколько штук суммарно подобрать после этой команды (без count — без лимита). off останавливает подбор. Дождаться — wait npcpickup alice, реагировать на каждый подбор — on npcpickup alice { ... }. Примеры: pickup alice; pickup alice items \"minecraft:diamond\" count 3; pickup alice off."
        );
        cmd("health", "Персонажи", "<who:char> <hp:number>", "Максимальное здоровье персонажа.");
        cmd(
            "free",
            "Персонажи",
            "<who:char>",
            "Отпускает персонажа: его тело остаётся в мире и после конца сюжета. Сюжет по-прежнему его узнаёт: on click, shop open with, look работают. Новый spawn даст персонажу другое тело."
        );
        cmd(
            "anim",
            "Анимации",
            "<who:char> <anim:anim> [layer <layer:layer>] [speed <speed:number>] [fade <fade:duration>] [times <times:int>] [loop] [hold] [wait] [from <from:duration>]",
            "Проигрывает анимацию на персонаже.\nlayer — слой (по умолчанию action: всё тело, перекрывает стойку; 0–20 — накладываются поверх),\nspeed — скорость (1 — обычная), fade — плавный переход, times — проиграть N раз и остановить (сюжет ждёт),\nloop — зациклить, hold — замереть на последнем кадре, wait — дождаться конца анимации.\nПример: anim alice \"entity/humanoid!attack\" layer 3 times 2 wait"
        );
        cmd(
            "playeranim",
            "Анимации",
            "<who:target> <anim:anim> [speed <speed:number>] [fade <fade:duration>] [times <times:int>] [loop] [hold] [wait]",
            "Анимация на игроке — те же файлы, что у персонажей, например \"entity/humanoid!waving\" или свои из папки «Анимации». Двигаются голова, тело, руки и ноги (кости head, body, rightArm, leftArm, rightLeg, leftLeg); что анимация не трогает — как в игре. Её видят все, сам игрок — от третьего лица.\ntimes — проиграть N раз, loop — по кругу, hold — замереть на последнем кадре, wait — сюжет ждёт конца. Пример: playeranim player \"entity/humanoid!bigWaving\" times 2 wait"
        );
        cmd("playeranim stop", "Анимации", "<who:target> [fade <fade:duration>]", "Останавливает анимацию игрока из сюжета: playeranim stop player fade 10t.");
        cmd(
            "face",
            "Анимации",
            "<who:char> <anim:anim> [fade <fade:duration>]",
            "Мимика: анимация на слое лица (emotion). Пример: face alice \"entity/humanoid!attack\""
        );
        cmd("stopanim", "Анимации", "<who:char> [layer <layer:layer>] [fade <fade:duration>]", "Останавливает анимацию на слое (по умолчанию action).");
        cmd("clearanims", "Анимации", "<who:char> [fade <fade:duration>]", "Останавливает все анимации персонажа, включая мимику.");
        cmd(
            "moveanim",
            "Анимации",
            "<who:char> <state:movetype> <anim:anim>",
            "Заменяет стандартную анимацию состояния: idle — стойка, walk — ходьба, fastwalk — быстрая ходьба, run — бег."
        );
        cmd("resetanim", "Анимации", "<who:char>", "Возвращает стандартные анимации стойки и ходьбы.");
        cmd("nod", "Анимации", "<who:char> [times <times:int>]", "Жест: персонаж кивает.");
        cmd("nope", "Анимации", "<who:char> [times <times:int>]", "Жест: персонаж качает головой «нет».");
        cmd("laugh", "Анимации", "<who:char> [times <times:int>] [speed <speed:number>]", "Жест: персонаж смеётся.");
        cmd("sigh", "Анимации", "<who:char>", "Жест: персонаж вздыхает.");
        cmd("shake", "Анимации", "<who:char> <time:duration>", "Жест: персонаж дрожит.");
        cmd(
            "gui open",
            "Интерфейс",
            "<name:gui>",
            "Открывает игроку интерфейс из папки «Интерфейсы» проекта. Сюжет идёт дальше; чтобы дождаться кнопки — wait button."
        );
        cmd("gui close", "Интерфейс", "", "Закрывает открытый интерфейс.");
        cmd(
            "gui text",
            "Интерфейс",
            "<el:guiel> <text:text>",
            "Меняет текст элемента (текст, кнопка, поле, подпись полосы). Поддерживает подстановки: \"Здоровье: {hp}\""
        );
        cmd("gui show", "Интерфейс", "<el:guiel>", "Показывает скрытый элемент.");
        cmd("gui hide", "Интерфейс", "<el:guiel>", "Скрывает элемент.");
        cmd("gui progress", "Интерфейс", "<el:guiel> <value:expr>", "Значение полосы от 0 до 1. Пример: gui progress \"hp\" hp / 20");
        cmd("gui image", "Интерфейс", "<el:guiel> <image:image>", "Меняет картинку элемента.");
        cmd("gui scroll", "Интерфейс", "<el:guiel> <y:expr>", "Прокручивает панель прокрутки до позиции (в пикселях от начала). Пример: gui scroll \"list\" 0");
        cmd("gui undrag", "Интерфейс", "<el:guiel>", "Возвращает объект «Перетаскивание» на его место: gui undrag \"карта\".");
        cmd(
            "gui set",
            "Интерфейс",
            "<el:guiel> <prop:text> <value:text>",
            "Меняет любое свойство элемента, как в инспекторе редактора: gui set \"title\" \"color\" \"#FFFF5555\""
        );
        cmd(
            "menu open",
            "Игра и меню",
            "<name:text>",
            "Только в общем скрипте. Открывает экран игры: title (главное меню), worlds (выбор мира), multiplayer, options и другие меню из настроек интерфейса; back — назад. Пример: menu open \"worlds\""
        );
        cmd(
            "wait menu",
            "Игра и меню",
            "[<name:text>]",
            "Только в общем скрипте. Ждёт, пока откроется меню (любое или это). Какое — menu(). В фоне: on menu \"title\" { ... }"
        );
        cmd("game quit", "Игра и меню", "", "Только в общем скрипте. Закрывает игру.");
        cmd("world open", "Игра и меню", "<name:text>", "Только в общем скрипте. Заходит в одиночный мир по имени его папки: world open \"Сюжет\"");
        cmd("server join", "Игра и меню", "<address:text>", "Только в общем скрипте. Подключается к серверу: server join \"play.example.com\"");
        cmd("link open", "Игра и меню", "<url:text>", "Только в общем скрипте. Открывает ссылку http или https в браузере.");
        cmd(
            "wait button",
            "Интерфейс",
            "[<buttons:guiel...>]",
            "Ждёт нажатия кнопки интерфейса (любой или из списка). Какую нажали — button(). Пример: wait button \"yes\" \"no\""
        );
        String panelLook = " [look <look:panellook>] [turn <turn:panelturn>] [opacity <opacity:number>] [color <color:text>] [through] [keepsize] [fade <fade:number>] [smooth <smooth:number>] [bob <bob:number>]";
        String panelLookDoc = "\nВид: look — готовый (plate, marker, hologram, sign), отдельные настройки важнее него. turn — как повёрнут: fixed, viewer, camera, body, bone. opacity — непрозрачность 0..1, color — оттенок \"#RRGGBB\", through — виден сквозь стены, keepsize — на экране одного размера с любого расстояния (scale — ширина как с 8 блоков), fade — с какого расстояния начинает таять до range, smooth — плавность следования в секундах, bob — парит вверх-вниз на столько блоков.";
        cmd(
            "gui attach",
            "Интерфейс",
            "<name:gui> to <who:target> [bone <bone:text>] [offset <offset:expr>] [scale <scale:number>] [pitch <pitch:number>] [yaw <yaw:number>] [id <id:text>] [reach <reach:number>] [range <range:number>] [face] [self]"
                + panelLook,
            "Цепляет интерфейс к игроку, NPC или мобу: он ходит за ним. Без кости висит над головой, bone — на кости модели NPC. offset (вправо, вверх, вперёд) — сдвиг в блоках от этого места, считается по тому, куда повёрнуто тело. scale — ширина в блоках (1.5), yaw — поворот относительно тела при turn \"body\". По умолчанию поворачивается к смотрящему. Сам игрок свой интерфейс видит только от третьего лица и нажать на него не может; self — видит всегда и может нажимать. Убирается через gui remove, когда носителя убрали или игрок вышел, и с остановкой сюжета. Одна и та же раскладка на нескольких телах: gui progress \"id/элемент\" 50 меняет элемент только в панели с этим id.\nПример: gui attach \"hp\" to boss id \"hp_boss\" look \"plate\" offset (0, 0.3, 0)"
                + panelLookDoc
        );
        cmd(
            "gui place",
            "Интерфейс",
            "<name:gui> at <pos:target> [scale <scale:number>] [pitch <pitch:number>] [id <id:text>] [reach <reach:number>] [range <range:number>] [face]"
                + panelLook,
            "Ставит интерфейс прямо в мир, как экран на стене: at — середина его нижнего края, (x, y, z, yaw) — куда он повёрнут. scale — ширина в блоках (по умолчанию 3), pitch — наклон. face — сам поворачивается к смотрящему. reach — с какого расстояния на него можно нажать (6), range — откуда его видно (64). id — имя, чтобы заменить или убрать; по умолчанию имя интерфейса. Прицел работает как мышь: кнопки нажимаются, wait button и gui text работают как обычно. События: panelclick, panelhover, panelleave. Видят все игроки; убирается вместе с остановкой сюжета."
                + panelLookDoc
        );
        cmd("gui remove", "Интерфейс", "[<id:text>]", "Убирает интерфейс из мира; без id — все интерфейсы этого сюжета в мире.");
        cmd(
            "overlay show",
            "Интерфейс",
            "<name:gui>",
            "Показывает интерфейс поверх игры как наложение (свой HUD: счётчики, подсказки, таймеры). Мышь и клавиатура остаются у игрока.\nМенять его можно теми же командами: gui text, gui progress, gui move... Для привязки к краям экрана поставьте интерфейсу масштаб «screen»."
        );
        cmd("overlay hide", "Интерфейс", "[<name:gui>]", "Убирает наложение (без имени — все наложения сюжета).");
        cmd(
            "gui move",
            "Интерфейс",
            "<el:guiel> x <x:expr> y <y:expr> [time <time:duration>]",
            "Двигает элемент (X, Y от точки привязки, можно в процентах: \"50%\"). С time — плавно. Пример: gui move \"card\" x 0 y -40 time 0.5s"
        );
        cmd(
            "gui resize",
            "Интерфейс",
            "<el:guiel> w <w:expr> h <h:expr> [time <time:duration>]",
            "Меняет размер элемента, можно плавно. Пример: gui resize \"bar\" w hp * 10 h 8 time 1s"
        );
        cmd(
            "gui animate",
            "Интерфейс",
            "<el:guiel> <prop:text> <value:expr> [time <time:duration>]",
            "Плавно меняет любое числовое свойство: opacity, rotation, value, x, y... Пример: gui animate \"logo\" \"rotation\" 360 time 2s"
        );

        for (EventDefs.Def d : EventDefs.ALL.values()) {
            cmd(
                "wait " + d.name(),
                "Ожидание",
                EventDefs.syntax(d),
                "Ждёт: "
                    + d.title()
                    + ". "
                    + d.doc()
                    + (d.filter() != null ? "\nФильтр (" + d.filterLabel() + ") необязателен." : "")
                    + "\nДанные — event(\"поле\"): "
                    + EventDefs.fieldsDoc(d)
                    + "\nВ фоне при каждом событии: on "
                    + d.name()
                    + (d.filter() != null ? " ..." : "")
                    + " { ... }"
            );
        }

        cmd(
            "cutscene play",
            "Камера",
            "<name:cutscene> [at <pos:target>] [nowait]",
            "Проигрывает катсцену из папки «Катсцены». Сюжет ждёт её конца (или пропуска), с nowait — идёт дальше.\nat — точка отсчёта для катсцен с относительными координатами."
        );
        cmd("cutscene stop", "Камера", "", "Обрывает катсцену и плавно возвращает камеру игроку.");
        cmd(
            "cutscene continue",
            "Камера",
            "",
            "Катсцена идёт дальше с кадра, на котором стоит (у кадра галочка «Ждать на этом кадре»). Если она ещё не дошла до него, этот кадр она пройдёт не останавливаясь. Сюжет должен идти дальше, пока она стоит: cutscene play \"сцена\" nowait, wait cutscenehold \"сцена\", say …, cutscene continue."
        );
        cmd(
            "camera at",
            "Камера",
            "<pos:target> [look <look:target>] [yaw <yaw:number>] [pitch <pitch:number>] [time <time:duration>] [ease <ease:ease>]",
            "Ставит камеру в точку (у персонажа или игрока — на уровне глаз). С time — плавный перелёт. look — следить взглядом за целью.\nПример: camera at (10, 70, 5) look alice time 3s ease ease_in_out_sine"
        );
        cmd("camera look", "Камера", "<target:target> [time <time:duration>] [ease <ease:ease>]", "Камера поворачивается к цели и продолжает за ней следить.");
        cmd(
            "camera turn",
            "Камера",
            "yaw <yaw:expr> pitch <pitch:expr> [time <time:duration>] [ease <ease:ease>]",
            "Поворачивает камеру на заданные углы (yaw — по горизонтали, pitch — вверх/вниз)."
        );
        cmd(
            "camera follow",
            "Камера",
            "<who:target> [distance <distance:number>] [height <height:number>] [angle <angle:number>]",
            "Камера следует за целью сзади (angle 180 — спереди, 90 — сбоку)."
        );
        cmd(
            "camera orbit",
            "Камера",
            "<who:target> [radius <radius:number>] [height <height:number>] [speed <speed:number>]",
            "Камера облетает цель по кругу (speed — градусов в секунду, минус — в другую сторону)."
        );
        cmd(
            "camera rig",
            "Камера",
            "<name:camrig> [time <time:duration>]",
            "Камера, с которой игрок играет: готовая (gta, third_person, over_the_shoulder, souls, top_down, isometric, platformer_3d, side_x — 2D вид сбоку…) или своя из папки «Камеры». Плавно сменяет прежнюю, игрок ходит как обычно. Держится до camera rigoff и пока идёт сюжет."
        );
        cmd(
            "camera rigset",
            "Камера",
            "<param:camparam> <value:expr> [time <time:duration>]",
            "Меняет одну настройку этой камеры: camera rigset distance 20 time 2s, camera rigset lock_z 1, camera rigset world_z 100."
        );
        cmd("camera rigoff", "Камера", "[time <time:duration>]", "Возвращает обычную камеру.");
        cmd("camera reset", "Камера", "[time <time:duration>]", "Плавно возвращает камеру игроку.");
        cmd(
            "camera fov",
            "Камера",
            "<value:expr> [time <time:duration>] [ease <ease:ease>]",
            "Угол обзора (зум): 30 — приближение, 70 — обычно, 110 — широкий угол."
        );
        cmd("camera roll", "Камера", "<value:expr> [time <time:duration>] [ease <ease:ease>]", "Наклон камеры набок, в градусах (0 — ровно).");
        cmd("camera shake", "Камера", "<value:expr> [time <time:duration>]", "Тряска камеры: сила 0..1, затухает за time (по умолчанию 1 с).");
        cmd("camera bars", "Камера", "<value:expr> [time <time:duration>]", "Кинополосы сверху и снизу: доля высоты экрана, 0.11 — как в кино, 0 — убрать.");
        cmd(
            "camera fade",
            "Камера",
            "<value:expr> [color <color:text>] [time <time:duration>]",
            "Затемнение экрана: 1 — полностью, 0 — снять. color — цвет (\"#000000\", \"#FFFFFF\")."
        );
        cmd("camera hud", "Камера", "<value:bool>", "Показать (true) или спрятать (false) интерфейс игрока.");
        cmd(
            "camera hide",
            "Камера",
            "<what:text>",
            "Какой интерфейс скрыть: \"all\" — весь, \"minecraft\" — хотбар, сердца, прицел, \"classic\" — HP и монеты Spraute, \"custom\" — свои наложения (overlay), \"none\" — ничего. Речь и титры видны всегда."
        );
        cmd(
            "time set",
            "Игрок и мир",
            "<value:expr> [day <day:expr>]",
            "Время суток: число 0..23999 или day, noon, sunset, night, midnight, sunrise. День не сбивается: time set night."
        );
        cmd("time add", "Игрок и мир", "<value:expr>", "Сдвигает время вперёд (тики, 1000 — около 50 секунд дня): time add 6000.");
        cmd("time speed", "Игрок и мир", "<value:expr>", "Скорость дня: 1 — обычная, 3 — втрое быстрее, 0.2 — медленно. Сохраняется в настройках мира.");
        cmd("time freeze", "Игрок и мир", "[at <value:expr>]", "Останавливает время (at — сначала поставить: time freeze at noon).");
        cmd(
            "time task",
            "Игрок и мир",
            "<id:text> priority <priority:expr> speed <speed:expr> [until <until:expr>]",
            "Задача хода времени: работает задача с наивысшим приоритетом; until — условие конца. Только при включённых часах мира."
        );
        cmd("time taskstop", "Игрок и мир", "<id:text>", "Убирает задачу хода времени.");
        cmd(
            "camera carry",
            "Камера",
            "<value:expr>",
            "Вынос камеры: игрок становится наблюдателем и движется с камерой, чтобы подгружались чанки; режим игры и место возвращаются в конце."
        );
        cmd("time unfreeze", "Игрок и мир", "[back]", "Время снова идёт.");
        cmd(
            "shop open",
            "Интерфейс",
            "<name:shop> [with <who:target>]",
            "Открывает магазин из папки «Магазины»: shop open \"лавка\" with alice. Покупки ловит on purchase \"лавка\" { event(\"offer\") }."
        );
        cmd("shop restock", "Интерфейс", "<name:shop>", "Возвращает весь запас товаров магазина.");
        cmd("shop stock", "Интерфейс", "<name:shop> <offer:text> <value:expr>", "Сколько товара осталось: shop stock \"лавка\" \"apple\" 5 (-1 — без конца).");
        cmd("shop close", "Интерфейс", "", "Закрывает окно магазина.");
        cmd(
            "fight",
            "Персонажи",
            "<who:char> [style <style:fight>] [target <target:target>]",
            "Персонаж начинает драться: догоняет цель (по умолчанию игрока), бьёт атаками стиля, блокирует, уклоняется, падает без сил. Стиль — из папки «Стили боя» или готовый: \"fists\" (кулачный бой), \"sword\" (мечник), \"brute\" (громила), \"weak\" (слабак), \"archer\" (лучник: держит дистанцию, прячется, лечится), \"mage\" (маг: огненные шары). fight alice style \"sword\" target bob — драться с другим персонажем."
        );
        cmd("fight stop", "Персонажи", "<who:char>", "Персонаж прекращает бой.");
        cmd("fight heal", "Персонажи", "<who:char> [<value:expr>]", "Возвращает здоровье боя (всё или сколько указано); поверженный встаёт и дерётся дальше.");
        cmd(
            "skills open",
            "Игрок и мир",
            "<tree:skilltree> [menu <menu:skillmenu>]",
            "Открывает игроку панель прокачки из папки «Прокачка». menu — какое из её меню: skills open \"навыки\" menu \"кузница\"."
        );
        cmd("skills level", "Игрок и мир", "<tree:skilltree> <amount:expr>", "Ставит игроку уровень системы. Награды за пропущенные уровни не выдаются.");
        cmd("skills close", "Игрок и мир", "", "Закрывает панель прокачки.");
        cmd("skills xp", "Игрок и мир", "<tree:skilltree> <amount:expr>", "Даёт игроку опыт этой системы: skills xp \"навыки\" 50. Новые уровни приходят сами.");
        cmd(
            "skills points",
            "Игрок и мир",
            "<tree:skilltree> <amount:expr> [currency <currency:text>]",
            "Даёт очки системы, можно и отнимать: skills points \"навыки\" 2. currency — какие, если их несколько; по умолчанию первые."
        );
        cmd("skills learn", "Игрок и мир", "<tree:skilltree> <node:skillnode>", "Даёт навык бесплатно, что бы он ни требовал: награда сюжета.");
        cmd("skills forget", "Игрок и мир", "<tree:skilltree> <node:skillnode>", "Забирает навык, очки не возвращаются.");
        cmd("skills reset", "Игрок и мир", "<tree:skilltree>", "Забывает все навыки системы и возвращает очки, сколько сказано в её настройках.");
        cmd("skills enable", "Игрок и мир", "<tree:skilltree>", "Включает систему для игрока: снова идут опыт и очки, панель открывается.");
        cmd(
            "skills disable",
            "Игрок и мир",
            "<tree:skilltree>",
            "Выключает систему для игрока: опыт и очки не идут, панель не открывается, блокировки предметов не действуют."
        );
        cmd(
            "stealth start",
            "Бой и боссы",
            "[back <back:target>] [restore <restore:text>]",
            "Начинает стелс: то, что есть сейчас, и станет началом попытки. Пойманного игрока вернёт сюда или в back. restore — что откатить при поимке, через запятую: inventory, coins, health (и сытость), xp, blocks (сломанное и построенное игроком), loot (содержимое сундуков), all или none. По умолчанию all. Мобы и то, что изменили не игроки, не откатываются. Пример: stealth start back (10, 64, 10) restore \"inventory, blocks\"."
        );
        cmd(
            "stealth guard",
            "Бой и боссы",
            "<who:char> [style <style:stealth>] [patrol <patrol:path>] [speed <speed:number>] [shape <shape:stealthshape>] [range <range:number>] [angle <angle:number>] [width <width:number>] [height <height:number>] [sneak <sneak:number>] [notice <notice:duration>] [color <color:text>] [through] [pingpong]",
            "Персонаж становится стражем: перед ним на полу виден красный веер, и кто в него попал, тот пойман. style — стиль из папки «Стили стелса» или готовый: \"guard\", \"sentry\", \"watcher\", \"beam\", \"blind\"; остальные параметры переопределяют его. patrol — путь из папки «Пути», по нему он ходит по кругу. shape — fan (веер), circle (круг вокруг), rect (прямоугольник), triangle (треугольник). range — длина (8), angle — ширина веера в градусах (70), width — ширина прямоугольника и треугольника (4), height — на сколько блоков над полом он ловит (2.5). sneak — во сколько раз уменьшается, пока игрок крадётся с Shift (0.5). notice — сколько надо пробыть внутри, чтобы поймали (сразу). color — цвет \"#FF3030\". through — видит сквозь стены. Без stealth start попытка начнётся здесь. Поймали — событие stealthcaught: игрок и стражи уже на своих местах."
        );
        cmd("stealth remove", "Бой и боссы", "<who:char>", "Персонаж больше не страж: веер исчезает, патруль кончается.");
        cmd("stealth stop", "Бой и боссы", "", "Стелс окончен: все стражи перестают ловить, а всё, что игрок сделал, остаётся.");
        cmd(
            "fight tune",
            "Бой и боссы",
            "<who:target> [armor <armor:number>] [speed <speed:number>] [power <power:number>] [stagger <stagger:number>] [block <block:number>] [dodge <dodge:number>]",
            "Меняет идущий бой на ходу — например, вторая фаза босса. armor — какая доля урона гасится (0..1), speed — скорость погони (1 — шаг, 1.5 — бег), power — множитель урона его ударов (2 — вдвое сильнее), stagger — шанс вздрогнуть от удара (0 — не сбивается), block / dodge — шансы блока и уворота (0..1). Не указанное не меняется.\nif hpfrac(boss) < 0.3 { fight tune boss speed 1.8 power 1.5 stagger 0 }"
        );
        cmd(
            "bossbar show",
            "Бой и боссы",
            "<who:target> [title <title:text>] [color <color:text>] [style <style:text>]",
            "Полоса босса сверху экрана, которая сама следит за здоровьем: в бою (fight) — за боевым, иначе — за обычным. Сама исчезает, когда тот погиб, повержен или пропал. color: red, pink, blue, green, yellow, purple, white; style: \"6\", \"10\", \"12\", \"20\" — полоса на делениях. bossbar show boss title \"Тихий Звонарь\" color \"purple\""
        );
        cmd("bossbar hide", "Бой и боссы", "<who:target>", "Убирает полосу босса.");
        cmd(
            "leap",
            "Бой и боссы",
            "<who:target> to <to:target> [height <height:number>] [time <time:duration>] [impact <impact:vfx>] [radius <radius:number>] [damage <damage:number>] [push <push:number>] [nowait]",
            "Прыжок по дуге в точку или к цели (приземляется на пол рядом). height — высота дуги (3), time — сколько в воздухе (0.8s). При приземлении: impact — эффект, radius — ударная волна этого радиуса (0 — без неё), damage — урон, push — отбрасывание (1.2). Пока летит, бой (fight) его не ведёт. Сюжет ждёт приземления, с nowait — сразу дальше.\nleap boss to player height 5 time 1s impact \"explosion\" radius 4 damage 6"
        );
        cmd(
            "charge",
            "Бой и боссы",
            "<who:target> to <to:target> [speed <speed:number>] [distance <distance:number>] [damage <damage:number>] [push <push:number>] [trail <trail:vfx>] [nowait]",
            "Таран: несётся по прямой в сторону цели (по земле) и бьёт каждого на пути один раз. speed — блоков в секунду (14), distance — сколько пробежать (по умолчанию до цели и ещё 2 блока), damage — урон (4), push — отбрасывание (1), trail — эффект-след. Стена или обрыв останавливают. Сюжет ждёт конца, с nowait — сразу дальше.\ncharge boss to player speed 18 damage 5 trail \"smoke\""
        );
        cmd(
            "circle",
            "Бой и боссы",
            "<who:target> around <center:target> [radius <radius:number>] [speed <speed:number>] [time <time:duration>] [nowait]",
            "Ходит кругом вокруг цели или точки: radius (5), speed — градусов в секунду (90, минус — в другую сторону), time — сколько (3s). Смотрит на центр. Пока кружит, бой его не ведёт — удобно для «кружит и стреляет». Сюжет ждёт, с nowait — сразу дальше."
        );
        cmd(
            "part",
            "Бой и боссы",
            "<who:target> <name:text> [enabled <enabled:bool>] [damage <damage:number>] [attack <attack:number>]",
            "Часть хитбокса персонажа (их задают в редакторе сущности): enabled false — выключить (удары проходят мимо), true — включить; damage — множитель урона по ней для этого персонажа (2 — уязвимое место, 0 — броня, удар только звенит); attack — часть сама ранит всех, кого касается (урон раз в полсекунды), attack 0 — перестаёт.\npart boss \"панцирь\" enabled false  ·  part boss \"глаз\" damage 3  ·  part boss \"лезвие\" attack 4"
        );
        cmd(
            "attack",
            "Бой и боссы",
            "<who:target> [part <part:text>] [size <w:number> <h:number> <d:number>] [offset <right:number> <up:number> <forward:number>] [delay <delay:duration>] [time <time:duration>] [damage <damage:number>] [push <push:number>] [impact <impact:vfx>] [except <except:target>] [show] [nowait]",
            "Удар хитбоксом: через delay (0) на время time (0.3s) появляется коробка удара, и все, кого она касается, получают damage (4) и отбрасываются (push 0.5) — каждый один раз за удар. Коробка: size ширина высота глубина (2 2 2) со сдвигом offset вправо вверх вперёд (0 1 1.5) от ног, повёрнута вместе с телом; или part \"имя\" — часть сущности spraute:… (едет на своей кости в анимации, может быть выключена для ударов по ней). show — показать коробку частицами (для настройки). Сюжет ждёт конца удара, если нет nowait.\nanim boss \"удар\"  ·  attack boss part \"клешня\" delay 0.4s time 0.3s damage 8 impact \"explosion\"\nattack alice size 2 1.5 2.5 offset 0 1 1.5 damage 5 show"
        );
        cmd("stopmove", "Бой и боссы", "<who:target>", "Обрывает прыжок, таран или кружение и гасит его лучи (beam). Бой снова ведёт его сам.");
        cmd(
            "strike",
            "Бой и боссы",
            "at <pos:target> [radius <radius:number>] [delay <delay:duration>] [damage <damage:number>] [push <push:number>] [warn <warn:vfx>] [impact <impact:vfx>] [by <by:target>] [except <except:target>]",
            "Отложенный удар по месту — «телеграф» босса: эффект warn играет сразу (метка на земле), а через delay (1s) — эффект impact и удар: всех в radius (2.5) отбрасывает (push 0.8) и ранит (damage 4). by — кто бьёт (его не задевает), except — кого ещё не трогать. Сюжет не ждёт — можно поставить много ударов подряд.\nfor a in range(0, 360, 45) { strike at around(boss, 5, a) delay 1.2s warn \"метка\" impact \"explosion\" by boss }"
        );
        cmd(
            "beam",
            "Бой и боссы",
            "from <from:target> to <to:target> [time <time:duration>] [damage <damage:number>] [range <range:number>] [width <width:number>] [track <track:number>] [particle <particle:text>] [impact <impact:vfx>] [wait]",
            "Луч из глаз персонажа к цели: блоки его обрывают, всех, кого касается, жжёт (damage — урон в секунду, 6). time — сколько горит (2s), range — дальность (24), width — толщина (0.3), track — как быстро поворачивается за движущейся целью, градусов в секунду (0 — сразу; 40 — медленно, от луча можно убежать), particle — частицы луча (end_rod, flame, soul_fire_flame, electric_spark…), impact — эффект там, куда упирается. Сюжет не ждёт, с wait — ждёт, пока луч погаснет.\nbeam from boss to player time 3s damage 8 track 35 particle \"soul_fire_flame\""
        );
        cmd("wait defeated", "Ожидание", "<who:char>", "Ждёт, пока персонаж будет повержен в бою.");
        cmd("zone on", "Игрок и мир", "<name:zone>", "Включает зону из папки «Зоны»: она снова слышит игрока и держит стену.");
        cmd("zone off", "Игрок и мир", "<name:zone>", "Выключает зону: ни событий, ни стены.");
        cmd(
            "zone wall",
            "Игрок и мир",
            "<name:zone> <mode:text>",
            "Стена по краю зоны: \"in\" — не выпускать изнутри, \"out\" — не пускать внутрь, \"none\" — убрать. zone wall \"арена\" \"in\"."
        );
        cmd("wait zone", "Ожидание", "<name:zone>", "Ждёт, пока игрок войдёт в зону: wait zone \"ворота\".");
        cmd("wait outside", "Ожидание", "<name:zone>", "Ждёт, пока игрок выйдет из зоны.");
        cmd(
            "quest start",
            "Интерфейс",
            "<name:quest>",
            "Выдаёт квест из папки «Квесты»: он появляется в журнале (J) и в углу экрана. Выполненный или проваленный — начнётся заново."
        );
        cmd(
            "quest progress",
            "Интерфейс",
            "<name:quest> <task:task> [<value:expr>]",
            "Продвигает задачу: quest progress \"ворота\" \"zombies\" (на 1) или ... \"planks\" 4. Когда сделаны все обязательные задачи, квест выполняется сам."
        );
        cmd("quest set", "Интерфейс", "<name:quest> <task:task> <value:expr>", "Ставит, сколько сделано: quest set \"ворота\" \"talk\" 1.");
        cmd("quest reveal", "Интерфейс", "<name:quest> <task:task>", "Открывает скрытую задачу в журнале.");
        cmd("quest done", "Интерфейс", "<name:quest>", "Квест выполнен: выдаётся награда.");
        cmd("quest fail", "Интерфейс", "<name:quest>", "Квест провален.");
        cmd("quest reset", "Интерфейс", "<name:quest>", "Забыть квест, будто его не выдавали.");
        cmd("quest track", "Интерфейс", "<name:quest>", "Показывать этот квест в углу экрана.");
        cmd("quest journal", "Интерфейс", "", "Открывает журнал квестов (как клавиша J).");
        cmd(
            "wait goal",
            "Ожидание",
            "<name:quest> <task:task>",
            "Ждёт, пока задача квеста будет сделана: wait goal \"ворота\" \"planks\". Если она уже сделана — сюжет идёт сразу. Пока квест не выдан или провален, сюжет продолжает ждать. В фоне: on goal \"ворота\" \"planks\" { ... }."
        );
        cmd(
            "hitbox",
            "Персонажи",
            "<who:target> <modes:text...>",
            "Как ведёт себя тело персонажа (или игрока, моба). Столкновения: \"normal\" — как обычно, \"solid\" — твёрдый как стена (не пройти, можно встать сверху, не сдвинуть), \"fixed\" — не сдвинуть, \"ghost\" — проходят насквозь. Удары: \"damage\" — урон проходит, \"nodamage\" — не проходит, \"mortal\" — урон проходит и может погибнуть, \"immortal\" — не погибнет. \"nopick\" — нельзя навестись и ударить рукой, \"pick\" — снова можно. hitbox alice \"solid\" \"damage\". При объявлении: character alice = … hitbox \"ghost\"."
        );
        cmd(
            "namecolor",
            "Персонажи",
            "<who:target> <color:text>",
            "Цвет имени в речи и в истории сообщений: namecolor alice \"#FF8800\" или \"gold\", \"красный\"… \"reset\" — свой цвет убран (тогда общий из настроек движка → Чат). Сразу можно задать при объявлении: character alice = … color \"gold\"."
        );
        cmd(
            "skin",
            "Персонажи",
            "<who:target> <skin:image>",
            "Меняет скин персонажа или игрока: картинка проекта (skin alice \"Алиса в пальто\") или текстура игры (skin alice \"spraute_engine:textures/entity/humanoid.png\"). skin alice \"reset\" — вернуть обычный."
        );
        cmd(
            "summon",
            "Игрок и мир",
            "<type:entity> at <pos:target> [into <var:var>]",
            "Призывает сущность. into — переменная, куда положить её (если там список — добавит в него): summon \"minecraft:zombie\" at (0, 64, 0) into enemies."
        );
        cmd(
            "spawnitem",
            "Игрок и мир",
            "<item:item> at <pos:target> [count <count:int>] [motion <motion:expr>] [delay <delay:duration>] [nopickup] [into <var:var>]",
            "Создаёт предмет на земле, в точке или у цели. motion — инерция, скорость по x, y, z в блоках за тик: motion (0, 0.5, 0.2) подбросит его вверх и вперёд. delay — сколько его нельзя подобрать (0.5s), nopickup — нельзя вообще. into — переменная, куда положить предмет; потом инерцию можно менять: set it.velocity = (0, 0.3, 0) или it.push((0.2, 0, 0)). Пример: spawnitem \"minecraft:diamond\" at alice count 2 motion (0, 0.4, 0.1) into it."
        );
        cmd(
            "build place",
            "Игрок и мир",
            "<name:structure> at <pos:target> [style <style:buildstyle>] [time <time:duration>] [flight <flight:duration>] [rotate <rotate:number>] [mirror <mirror:text>] [air <air:text>] [id <id:text>] [nowait]",
            "Строит постройку с анимацией: at — середина её пола. style — откуда идёт стройка: up, down, left, right, front, back (стороны — от зрителя), rain, rise, center, edges, spiral, tornado, scatter, assemble, wave, pop, instant. time — вся стройка (по умолчанию от размера), flight — полёт одного блока, rotate — 0/90/180/270, mirror — \"x\" или \"z\". air — что делать с воздухом: \"auto\" (расчистить внутри, снаружи не трогать), \"keep\", \"clear\". id — имя, чтобы потом убрать: build undo. Сюжет ждёт конца, если нет nowait; событие buildend."
        );
        cmd(
            "build undo",
            "Игрок и мир",
            "<id:text> [style <style:buildstyle>] [time <time:duration>] [nowait]",
            "Убирает постройку с этим id: блоки разлетаются, на их место возвращается то, что стояло. Работает, пока мир не перезапущен."
        );
        cmd("build stop", "Игрок и мир", "<id:text>", "Заканчивает постройку сразу.");
        cmd(
            "blocks move",
            "Игрок и мир",
            "from <from:target> to <to:target> by <by:expr> [time <time:duration>] [ease <ease:ease>] [id <id:text>] [nowait]",
            "Плавно сдвигает настоящие блоки: from и to — углы области, by (x, y, z) — на сколько блоков. Блоки покидают место сразу, летят как картинка и становятся настоящими по прибытии: в пути на них нельзя стоять. Содержимое сундуков едет с ними. time — за сколько (1 с), ease — плавность (smooth), id — чтобы потом вернуть. Ждёт конца движения, если нет nowait. Событие blocksmoved. Пример: blocks move from (10, 64, 0) to (12, 67, 0) by (0, 0, 3) time 2s id \"ворота\""
        );
        cmd(
            "blocks back",
            "Игрок и мир",
            "<id:text> [time <time:duration>] [ease <ease:ease>] [nowait]",
            "Возвращает блоки, сдвинутые через blocks move с этим id, и то, что стояло на их новом месте."
        );
        cmd("blocks stop", "Игрок и мир", "<id:text>", "Заканчивает сдвиг блоков сразу.");
        cmd(
            "blocks shake",
            "Игрок и мир",
            "[at <at:target>] [radius <radius:number>] [from <from:target>] [to <to:target>] [power <power:number>] [time <time:duration>] [together]",
            "Трясёт блоки: только картинка, мир не меняется и по ним можно ходить. Где: at и radius (6) — шар вокруг точки, или from и to — углы области. power — размах в блоках (0.08), time — сколько (1 с), затухает к концу. together — вся область трясётся как одно целое, иначе каждый блок сам по себе. Сундуки и вода не трясутся. Не ждёт конца. Пример: blocks shake from (0, 64, 0) to (5, 70, 5) power 0.1 time 2s together"
        );
        cmd(
            "blocks wave",
            "Игрок и мир",
            "at <at:target> [radius <radius:number>] [height <height:number>] [time <time:duration>] [step <step:number>] [rise <rise:number>] [tilt <tilt:number>]",
            "Волна по земле от точки: блоки подпрыгивают друг за другом, наклоняясь и поднимая пыль. Только картинка, мир не меняется. radius — докуда дойдёт (8), height — как высоко в начале (0.6), дальше ниже; time — за сколько дойдёт до края (0.8 с). step — сколько секунд идёт от блока к блоку, вместо time: 0.1 — медленно, по одному. rise — сколько секунд один блок в воздухе (0.3): чем меньше, тем тоньше кольцо. tilt — на сколько градусов блок наклоняется наружу (10), 0 — не наклоняется. Не ждёт конца. Босс приземлился: leap boss to player, затем blocks wave at boss radius 12 height 0.8 и camera shake 0.6"
        );
        cmd(
            "slot lock",
            "Игрок и мир",
            "<which:slotset> [to <player:expr>]",
            "Закрывает ячейки инвентаря: slot lock \"3-5\", slot lock \"main\", slot lock \"offhand\". На них замок, из них ничего не взять и в них ничего не положить, подбираемые предметы их обходят. Что лежало, остаётся там. 1–9 — хотбар, 10–36 — остальное, ещё hotbar, main, armor, offhand, all. Сохраняется в мире, на творческий режим не действует."
        );
        cmd("slot unlock", "Игрок и мир", "<which:slotset> [to <player:expr>]", "Открывает ячейки, закрытые через slot lock.");
        cmd(
            "slot size",
            "Игрок и мир",
            "<count:expr> [to <player:expr>]",
            "Сколько ячеек в инвентаре игрока, от 0 до 90 (в игре 36). Меньше — лишние закрываются замком: сначала рюкзак сверху вниз, хотбар последним (другой порядок — slot order): slot size 12. Больше — в окне инвентаря появляются новые: slot size 45 добавит 9. Убранные дополнительные хранят предметы, пока их не вернут."
        );
        cmd(
            "slot order",
            "Игрок и мир",
            "<order:slotorder> [to <player:expr>]",
            "Как закрываются ячейки этого игрока при slot size: top — рюкзак сверху вниз, bottom — снизу вверх, columns — столбцами справа (во всех хотбар последним), hotbar — сначала хотбар, потом рюкзак. Пример: slot order \"bottom\""
        );
        cmd("slot reset", "Игрок и мир", "[to <player:expr>]", "Убирает свои настройки ячеек игрока: снова как в настройках движка.");
        cmd(
            "rule deny",
            "Игрок и мир",
            "<rule:playercan> [time <time:duration>] [to <player:expr>]",
            "Запрещает игроку действие: rule deny \"jump\". Есть: move, jump, sprint, sneak, look, climb, elytra, ride, sleep, break, place, interact, containers, use_item, eat, attack, interact_entity, drop, pickup, pickup_xp, inventory, swap_hands, hotbar, take_damage, drown, burn, fly. time — на время: потом как было. Запреты сохраняются в мире и не действуют на игроков в творческом режиме и наблюдателей."
        );
        cmd(
            "rule allow",
            "Игрок и мир",
            "<rule:playercan> [time <time:duration>] [to <player:expr>]",
            "Разрешает действие: снимает запрет или, для fly, даёт полёт: rule allow \"fly\"."
        );
        cmd(
            "rule set",
            "Игрок и мир",
            "<rule:playerstat> <value:expr> [time <time:duration>] [to <player:expr>]",
            "Настраивает игрока: rule set \"mine_speed\" 3. Есть: reach, attack_reach, move_speed, jump_power, mine_speed, fly_speed, gravity, step_height, attack_damage, attack_speed, attack_knockback, knockback_resist, max_health, armor, armor_toughness, luck, damage_taken, fall_damage, hunger_speed, xp_gain. time — на время: потом как было."
        );
        cmd(
            "rule reset",
            "Игрок и мир",
            "[<rule:playerrule>] [to <player:expr>]",
            "Возвращает правило как в обычной игре; без имени — все правила этого игрока."
        );
        cmd(
            "setblock",
            "Игрок и мир",
            "<pos:target> <block:text>",
            "Ставит блок со свойствами, как /setblock: setblock (1, 64, 2) \"minecraft:lever[facing=south,powered=true]\"."
        );
        cmd(
            "use",
            "Игрок и мир",
            "<who:target> at <pos:target>",
            "Использует блок, как правый клик: дверь, рычаг, кнопка, сундук, кровать. Может и игрок, и персонаж: use anselm at (1, 64, 2), use player at door. Персонаж держит то, что у него в руке, и не должен стоять рядом. Блоки с окном, как сундук, откроются только игроку. Событие useblock не срабатывает."
        );
        cmd(
            "setprop",
            "Игрок и мир",
            "<pos:target> <prop:text> <value:expr>",
            "Меняет одно свойство блока, который уже стоит: setprop (1, 64, 2) \"facing\" \"south\", setprop door \"open\" true."
        );
        cmd(
            "emit",
            "Логика",
            "<name:text> [with <value:expr>]",
            "Посылает своё событие во все сюжеты: emit \"волна\" with 3. Принимают его on signal \"волна\" { event(\"value\") } или wait signal \"волна\"."
        );
        cmd(
            "shockwave",
            "Эффекты",
            "at <pos:target> [radius <radius:number>] [power <power:number>] [damage <damage:number>] [except <except:target>]",
            "Ударная волна: всех живых в радиусе (6) отбрасывает от центра и чуть вверх (power 1.2), damage — урон. Если центр — существо (shockwave at player), оно само не задевается. except — кого ещё не трогать."
        );
        cmd(
            "aoe",
            "Эффекты",
            "at <pos:target> [radius <radius:number>] [cone <cone:number>] [damage <damage:number>] [push <push:number>] [pull <pull:number>] [fire <fire:duration>] [effect <effect:effect>] [time <time:duration>] [level <level:int>] [heal <heal:number>] [by <by:target>] [only <only:entity>] [except <except:target>] [allies] [everyone] [self]",
            "Действие по области: задевает существ в radius (5). Кого: без указаний — врагов того, кто бьёт (by, по умолчанию игрок): игрок не заденет других игроков и своих питомцев, а персонаж — бьёт всех вокруг. allies — наоборот, только своих, вместе с ним самим; everyone — всех; self — его самого тоже; only — только такой тип существ. Убитые засчитываются ему. cone — только в секторе перед ним, в градусах. damage — урон, push — от центра, pull — к центру, fire — поджигает, effect с time и level — накладывает эффект, heal — лечит. Замедлить всех вокруг: aoe at player radius 6 effect \"minecraft:slowness\" time 4s level 3. Лечение: aoe at player radius 8 allies heal 6."
        );
        cmd(
            "motion add",
            "Персонажи",
            "<who:target> <vec:expr> [safe]",
            "Прибавляет к движению сущности вектор, блоков за тик: motion add player (0, 0.5, 0). Работает на игроке, персонаже и любом мобе. safe — падение до этого момента не считается."
        );
        cmd("motion set", "Персонажи", "<who:target> <vec:expr> [safe]", "Заменяет движение сущности на этот вектор.");
        cmd(
            "motion jump",
            "Персонажи",
            "<who:target> <power:expr> [safe]",
            "Подбрасывает вверх: движение по вертикали становится power, а вбок не меняется. Двойной прыжок: motion jump player 0.55 safe. Меньше нуля — швыряет вниз."
        );
        cmd(
            "motion forward",
            "Персонажи",
            "<who:target> <power:expr> [up <up:expr>] [safe]",
            "Толкает туда, куда сущность смотрит: рывок. up — сколько ещё вверх."
        );
        cmd(
            "motion toward",
            "Персонажи",
            "<who:target> <to:target> <power:expr> [up <up:expr>] [safe]",
            "Толкает сущность к цели; с отрицательной силой от неё: motion toward lookentity(player, 16) player 1.2."
        );
        cmd("motion stop", "Персонажи", "<who:target>", "Останавливает движение сущности.");
        cmd("ignite", "Персонажи", "<who:target> <time:duration>", "Поджигает сущность на это время: ignite evententity() 5s. 0s — тушит.");
        cmd(
            "breakblock",
            "Игрок и мир",
            "<pos:target> [nodrops] [nowear] [force]",
            "Ломает блок как инструмент в руке игрока: с его добычей и износом, но без события break, так что из on break можно ломать соседние. Неломаемое и воздух пропускаются, как и то, что этим инструментом не добыть (force — всё равно). nodrops — без добычи, nowear — не изнашивает. В творческом режиме ничего не падает и не изнашивается."
        );
        cmd(
            "explode",
            "Эффекты",
            "at <pos:target> [power <power:number>] [fire] [break]",
            "Взрыв (power 3 — как крипер). Без break блоки не ломаются, fire — поджигает."
        );
        cmd("lightning", "Эффекты", "at <pos:target> [harmless]", "Молния в точку (harmless — только вспышка и гром, без урона и огня).");
        cmd(
            "bar set",
            "Интерфейс",
            "<id:text> <value:expr> [max <max:expr>] [title <title:text>] [color <color:text>]",
            "Полоса сверху экрана (как у босса): мана, заряд, время… bar set \"мана\" mana max 100 title \"Мана\" color \"blue\". Цвета: purple, blue, red, green, yellow, white, pink."
        );
        cmd("bar hide", "Интерфейс", "<id:text>", "Убирает полосу.");
        cmd("actionbar", "Речь", "<text:text>", "Короткая надпись над хотбаром.");
        cmd(
            "notify",
            "Речь",
            "<text:text> [item <item:item>] [icon <icon:image>] [silent]",
            "Уведомление Spraute слева вверху. Иконка — предмет (notify \"Получен ключ\" item \"minecraft:tripwire_hook\") или картинка: проекта (icon \"ключ\") или игры (icon \"minecraft:textures/item/diamond.png\"). silent — без звука."
        );
        cmd(
            "cast",
            "Эффекты",
            "<name:vfx> [from <from:target>] [to <to:target>] [speed <speed:number>] [damage <damage:number>] [range <range:number>] [gravity <gravity:number>] [size <size:number>] [push <push:number>] [impact <impact:vfx>] [scale <scale:number>] [pierce] [id <id:text>] [count <count:int>] [spread <spread:number>] [ring]",
            "Снаряд: эффект летит и бьёт первого, в кого попал (или в блок). Откуда — from (по умолчанию игрок, из глаз), куда — to (цель или точка) или туда, куда смотрит from. speed — блоков в секунду (20), damage — урон (4), range — дальность (48), gravity — падение (0 — летит прямо), size — толщина для попадания (0.4), push — отбрасывание, impact — эффект на месте попадания, pierce — летит сквозь существ.\ncast \"fireball\" impact \"explosion\" damage 6 id \"огонь\" — и on casthit \"огонь\" { … }.\nЗалпом: count — сколько снарядов, spread — веер в градусах (count 5 spread 60 — пять веером на 60°), ring — кольцом во все стороны по горизонтали (cast \"magic_orb\" from boss count 12 ring)."
        );
        cmd(
            "fx play",
            "Эффекты",
            "<name:vfx> at <pos:target> [bone <bone:text>] [scale <scale:number>] [follow] [id <id:text>]",
            "Эффект частиц: свой из папки «Эффекты» или встроенный (nuke — ядерный взрыв, sky_beam — луч в небо, orbit_orbs — орбы вокруг, fire — огонь, standing_portal — стоячий портал, explosion, magic_circle, portal, fire_tornado, lightning, fireworks, heal, aura, smoke, sparkles).\nscale — размер (2 — вдвое больше), follow — едет за целью, bone — кость модели цели: fx play \"fire\" at alice bone \"hand\" follow — огонь в руке. id — имя, чтобы потом остановить: fx stop \"id\"."
        );
        cmd("fx stop", "Эффекты", "<id:text>", "Плавно гасит эффект, запущенный с этим id.");
        cmd("fx clear", "Эффекты", "", "Гасит все эффекты.");
        cmd("camera lock", "Камера", "<value:bool>", "Запретить (true) или разрешить (false) игроку ходить и смотреть по сторонам.");
        cmd(
            "orb spawn",
            "Игрок и мир",
            "<id:text> at <pos:target> [count <count:expr>] [value <value:expr>] [coins <coins:expr>] [xp <xp:expr>] [image <image:image>] [item <item:item>] [color <color:text>] [size <size:number>] [radius <radius:number>] [life <life:duration>] [spread <spread:number>] [nopickup] [float]",
            "Создаёт частицы сюжета, как шарики опыта: они летят к игроку и подбираются (событие on orb \"id\").\ncoins — сколько монет даёт каждая, xp — сколько опыта, value — своя ценность (event(\"amount\")).\nВид: image — картинка проекта, item — предмет, иначе светящийся шарик цвета color (\"#66CCFF\"). size — размер в блоках.\nradius — с какого расстояния притягиваются, life — сколько живут (0 — всегда), spread — разлёт.\nnopickup — только украшение (искры, пыльца), float — парят без гравитации.\nПример: orb spawn \"gem\" at alice count 5 coins 1 color \"#7FE0FF\" spread 1"
        );
        cmd("orb clear", "Игрок и мир", "[<id:text>]", "Убирает частицы сюжета (все или с этим именем).");
        cmd(
            "particles",
            "Экран",
            "<type:text> at <pos:target> [bone <bone:text>] [count <count:expr>] [spread <spread:number>] [speed <speed:number>]",
            "Частицы Minecraft: \"minecraft:flame\", \"heart\", \"happy_villager\", \"end_rod\", \"cloud\", \"enchant\"... Пример: particles \"heart\" at alice count 8 spread 0.5. bone — из кости модели: particles \"flame\" at alice bone \"hand\"."
        );
        cmd("coins give", "Игрок и мир", "<amount:expr>", "Даёт игроку монеты.");
        cmd("coins take", "Игрок и мир", "<amount:expr>", "Забирает у игрока монеты (не меньше нуля).");
        cmd("coins need", "Игрок и мир", "<amount:expr>", "Показывает у монет «есть/нужно» (-1 — убрать).");
        cmd(
            "slots",
            "Игрок и мир",
            "<count:expr> [order <order:slotorder>] [cells <cells:slotset>] [offhand <offhand:text>]",
            "Сколько ячеек из 36 открыто у всех игроков. По умолчанию рюкзак закрывается сверху вниз, а хотбар последним: slots 18 — весь хотбар и 9 ячеек рюкзака. order — другой способ: top, bottom, columns, hotbar. cells — свой список закрытых ячеек, число тогда не важно: slots 36 cells \"6-9, 10-18\". offhand — вторая рука: auto, off или on. В творческом режиме ограничений нет."
        );
        cmd(
            "slot icon",
            "Игрок и мир",
            "<image:image>",
            "Меняет текстуру замка на закрытых ячейках у всех игроков: slot icon \"lock.png\". Пустая строка — стандартный замок."
        );
        cmd("coins set", "Игрок и мир", "<amount:expr>", "Задаёт число монет игрока.");
        cmd(
            "coins drop",
            "Игрок и мир",
            "<amount:expr> at <pos:target> [count <count:expr>]",
            "Роняет монеты движка Spraute (их сферы), игрок их подбирает. count — на сколько сфер разделить. Если в настройках движка монетой назначен предмет, падает он."
        );
        cmd(
            "gui slot",
            "Интерфейс",
            "<el:guiel> <item:item> [count <count:expr>]",
            "Кладёт предмет в слот интерфейса (заменяет то, что там было). Пример: gui slot \"reward\" \"minecraft:diamond\" count 3"
        );
        cmd("gui clearslot", "Интерфейс", "<el:guiel>", "Очищает слот: предмет пропадает (например, игрок его «сдал»).");
        cmd(
            "wait slot",
            "Интерфейс",
            "[<slots:guiel...>]",
            "Ждёт, пока игрок положит предмет в слот или заберёт его (любой слот или из списка). Какой — lastslot()"
        );
        cmd("wait guiclose", "Интерфейс", "", "Ждёт, пока игрок закроет интерфейс.");
        cmd(
            "enable",
            "Игрок и мир",
            "<sys:system>",
            "Включает систему движка: health (полоска HP), coins (монеты), time (время суток), inventory (свой инвентарь), slots (блокировка слотов).\nНастройка сохраняется в мире, как в «Настройках движка»."
        );
        cmd("disable", "Игрок и мир", "<sys:system>", "Выключает систему движка: health, coins, time, inventory или slots. Пример: disable slots");
        cmd("teleport", "Игрок и мир", "<pos:target>", "Телепортирует игрока к цели: точка, персонаж или моб.");
        cmd("effect", "Игрок и мир", "<effect:effect> <time:duration> [level <level:int>]", "Накладывает эффект на игрока.");
        cmd(
            "mob spawn",
            "Игрок и мир",
            "<name:text> at <pos:target> [count <count:int>] [into <var:var>]",
            "Призывает моба из «Модификаций» по его id. count — сколько, рядом друг с другом. into — переменная: туда кладётся моб, а если их несколько или это список, то все."
        );
        cmd(
            "dungeon place",
            "Игрок и мир",
            "<name:text> at <pos:target>",
            "Сразу ставит данж из «Модификаций» в этой точке, с его сундуками и лутом, какими бы ни были его правила появления."
        );
        cmd(
            "loot give",
            "Игрок и мир",
            "<name:text>",
            "Выдаёт игроку лут из таблицы: имя таблицы из «Модификаций» или таблица игры (\"minecraft:chests/simple_dungeon\")."
        );
        cmd(
            "loot chest",
            "Игрок и мир",
            "<name:text> at <pos:target>",
            "Кладёт лут из таблицы в сундук или другое хранилище в этой точке. Хранилище уже должно там стоять."
        );
        cmd(
            "sound",
            "Игрок и мир",
            "<sound:sound> [at <pos:loc>] [volume <volume:number>] [pitch <pitch:number>] [loop] [id <id:text>] [fade <fade:duration>]",
            "Проигрывает звук (по умолчанию у игрока): звук игры (\"minecraft:block.bell.use\") или свой из папки «Звуки» проекта по имени (\"Колокол\", файлы .ogg и .wav). at — в точке мира (стихает с расстоянием; только моно-файлы), volume и pitch — громкость и высота (1 — как есть), loop — по кругу (свои звуки), id — имя, чтобы остановить: stopsound id \"музыка\", fade — нарастает из тишины за это время (свои звуки): sound \"Тема\" loop id \"музыка\" fade 3s"
        );
        cmd(
            "highlight",
            "Экран",
            "<who:target> [fill <fill:text>] [fillalpha <fillalpha:number>] [outline <outline:text>] [outlinealpha <outlinealpha:number>] [width <width:number>] [occluded] [id <id:text>] [time <time:duration>]",
            "Подсветка, как Highlight в Roblox: заливка поверх персонажа, игрока, моба или блока (точка — блок в ней) и обводка вокруг. fill / outline — цвета \"#RRGGBB\" (белый), fillalpha — прозрачность заливки 0..1 (0.4, 0 — без заливки), outlinealpha — обводки (1, 0 — без обводки), width — толщина обводки в блоках (0.05). По умолчанию видна сквозь блоки; occluded — только там, где её не закрывают блоки. time — сама исчезнет. Повторная подсветка того же меняет её. highlight boss outline \"#FF3030\" fill \"#FF3030\" fillalpha 0.2"
        );
        cmd("highlight off", "Экран", "<who:target>", "Убирает подсветку с персонажа, моба или блока.");
        cmd("highlight clear", "Экран", "[id <id:text>]", "Убирает подсветку с этим id, без id — всю подсветку сюжета.");
        cmd(
            "gizmo line",
            "Экран",
            "from <from:target> to <to:target> [color <color:text>] [alpha <alpha:number>] [width <width:number>] [through] [id <id:text>] [time <time:duration>]",
            "Линия в мире между точками или персонажами (за ними и тянется). width — толщина в пикселях (2), through — видна сквозь блоки, time — сама исчезнет, id — чтобы убрать или заменить: gizmo line from alice to player color \"#40C0FF\" through"
        );
        cmd(
            "gizmo box",
            "Экран",
            "from <from:target> to <to:target> [color <color:text>] [alpha <alpha:number>] [fill <fill:text>] [fillalpha <fillalpha:number>] [width <width:number>] [through] [id <id:text>] [time <time:duration>]",
            "Коробка в мире между двумя углами: рёбра цветом color, грани — fill с прозрачностью fillalpha (0.25). gizmo box from (0, 64, 0) to (5, 70, 5) fill \"#30FF60\""
        );
        cmd(
            "gizmo sphere",
            "Экран",
            "at <at:target> radius <radius:number> [color <color:text>] [alpha <alpha:number>] [width <width:number>] [through] [id <id:text>] [time <time:duration>]",
            "Сфера из трёх колец вокруг точки или персонажа: радиус атаки, зона слышимости. gizmo sphere at boss radius 6 color \"#FF4040\""
        );
        cmd(
            "gizmo text",
            "Экран",
            "at <at:target> <text:text> [color <color:text>] [alpha <alpha:number>] [scale <scale:number>] [through] [id <id:text>] [time <time:duration>]",
            "Надпись в мире, всегда лицом к игроку: над персонажем она едет вместе с ним. gizmo text at alice \"Квест!\" color \"#FFD040\" scale 2"
        );
        cmd(
            "gizmo screen text",
            "Экран",
            "<x:number> <y:number> <text:text> [color <color:text>] [alpha <alpha:number>] [scale <scale:number>] [id <id:text>] [time <time:duration>]",
            "Надпись на экране. x и y — в процентах экрана: (0, 0) — левый верхний угол, (100, 100) — правый нижний."
        );
        cmd(
            "gizmo screen rect",
            "Экран",
            "<x:number> <y:number> <w:number> <h:number> [color <color:text>] [alpha <alpha:number>] [fill <fill:text>] [fillalpha <fillalpha:number>] [width <width:number>] [id <id:text>] [time <time:duration>]",
            "Рамка на экране, в процентах экрана; fill — заливка. gizmo screen rect 40 40 20 20 color \"#FFFFFF\" fill \"#000000\" fillalpha 0.4"
        );
        cmd(
            "gizmo screen line",
            "Экран",
            "<x:number> <y:number> <x2:number> <y2:number> [color <color:text>] [alpha <alpha:number>] [width <width:number>] [id <id:text>] [time <time:duration>]",
            "Линия на экране от (x, y) до (x2, y2), в процентах экрана; width — толщина в пикселях."
        );
        cmd("gizmo clear", "Экран", "[id <id:text>]", "Убирает гизмо с этим id, без id — все гизмо сюжета (подсветку — highlight clear).");
        cmd(
            "navigate to",
            "Игрок и мир",
            "<to:target> [title <title:text>] [id <id:text>] [color <color:text>] [radius <radius:number>]",
            "Навигатор: вверху экрана стрелка показывает игроку, куда идти, с подписью и расстоянием. Цель — точка или персонаж (стрелка идёт за ним). Дошёл ближе radius блоков (3) — стрелка исчезает, срабатывает событие navarrive. Несколько стрелок — с разными id. navigate to alice title \"Найди Алису\" color \"#FFD040\""
        );
        cmd(
            "navigate zone",
            "Игрок и мир",
            "<name:zone> [title <title:text>] [id <id:text>] [color <color:text>]",
            "Навигатор к зоне проекта: стрелка на её середину, исчезает, когда игрок вошёл в зону. navigate zone \"Камеры\" title \"К камерам\""
        );
        cmd("navigate stop", "Игрок и мир", "[id <id:text>]", "Убирает стрелку навигатора с этим id, без id — все.");
        cmd(
            "stopsound",
            "Игрок и мир",
            "[id <id:text>] [fade <fade:duration>]",
            "Останавливает свой звук, запущенный с id, или без id — все свои звуки. fade — плавно затихает за это время: stopsound id \"музыка\" fade 2s. Звуки игры (minecraft:…) с id тоже останавливаются, но сразу."
        );
        cmd(
            "give",
            "Игрок и мир",
            "<item:item> [count <count:int>]",
            "После id можно указать NBT: give \"minecraft:diamond_sword{Damage:5}\". Это работает у всех предметов. Выдаёт игроку предмет."
        );
        cmd(
            "recipe shaped",
            "Рецепты и предметы",
            "<id:text> <result:item> [count <count:int>] [a1 <a1:item>] [a2 <a2:item>] [a3 <a3:item>] [b1 <b1:item>] [b2 <b2:item>] [b3 <b3:item>] [c1 <c1:item>] [c2 <c2:item>] [c3 <c3:item>]",
            "Рецепт верстака по форме. Ряды a, b, c, столбцы 1, 2, 3: recipe shaped \"ruby_sword\" \"minecraft:diamond_sword\" a2 \"minecraft:diamond\" b2 \"minecraft:diamond\" c2 \"minecraft:stick\". В ячейке id, #тег (\"#minecraft:planks\") или варианты через |. Id только латиницей; без двоеточия становится spraute:имя. Рецепт хранится в мире, пока его не сбросит recipe reset."
        );
        cmd(
            "recipe shapeless",
            "Рецепты и предметы",
            "<id:text> <result:item> [count <count:int>] [a1 <a1:item>] [a2 <a2:item>] [a3 <a3:item>] [b1 <b1:item>] [b2 <b2:item>] [b3 <b3:item>] [c1 <c1:item>] [c2 <c2:item>] [c3 <c3:item>]",
            "Рецепт верстака без формы: ингредиенты можно класть как угодно. Записывается как recipe shaped."
        );
        cmd(
            "recipe smelting",
            "Рецепты и предметы",
            "<id:text> <input:item> <result:item> [time <time:duration>] [xp <xp:number>]",
            "Рецепт печи. Без time — как обычно, 10 секунд."
        );
        cmd(
            "recipe blasting",
            "Рецепты и предметы",
            "<id:text> <input:item> <result:item> [time <time:duration>] [xp <xp:number>]",
            "Рецепт плавильной печи. Без time — 5 секунд."
        );
        cmd(
            "recipe smoking",
            "Рецепты и предметы",
            "<id:text> <input:item> <result:item> [time <time:duration>] [xp <xp:number>]",
            "Рецепт коптильни. Без time — 5 секунд."
        );
        cmd(
            "recipe campfire",
            "Рецепты и предметы",
            "<id:text> <input:item> <result:item> [time <time:duration>] [xp <xp:number>]",
            "Рецепт костра. Без time — 30 секунд."
        );
        cmd("recipe stonecutting", "Рецепты и предметы", "<id:text> <input:item> <result:item> [count <count:int>]", "Рецепт камнереза.");
        cmd(
            "recipe disable",
            "Рецепты и предметы",
            "<id:text>",
            "Отключает рецепт во всём мире: recipe disable \"minecraft:stick\". Id без двоеточия — рецепт игры."
        );
        cmd("recipe enable", "Рецепты и предметы", "<id:text>", "Снова включает отключённый рецепт.");
        cmd(
            "recipe reset",
            "Рецепты и предметы",
            "<id:text>",
            "Забывает всё, что сюжеты сделали с этим рецептом: созданный исчезает, изменённый и отключённый возвращаются к виду из проекта или игры."
        );
        cmd("recipe learn", "Рецепты и предметы", "<id:text>", "Открывает рецепт игроку в книге рецептов. Для другого игрока: as p { recipe learn \"id\" }.");
        cmd("recipe forget", "Рецепты и предметы", "<id:text>", "Закрывает рецепт в книге рецептов игрока.");
        cmd(
            "item cooldown",
            "Рецепты и предметы",
            "<item:item> <time:duration>",
            "Перезарядка предмета у игрока: пока она идёт, правая кнопка им не работает. item cooldown \"spraute_engine:magic_wand\" 5s."
        );
        cmd("item repair", "Рецепты и предметы", "[amount <amount:int>]", "Чинит предмет в руке игрока: на amount единиц или, без него, полностью.");
        cmd("item damage", "Рецепты и предметы", "<amount:int>", "Изнашивает предмет в руке игрока; когда прочность кончится, он сломается.");
        cmd("item rename", "Рецепты и предметы", "<name:text>", "Новое имя предмету в руке игрока, цвет через &: item rename \"&cПроклятый меч\".");
        cmd(
            "item enchant",
            "Рецепты и предметы",
            "<enchant:text> [level <level:int>]",
            "Зачаровывает предмет в руке игрока: item enchant \"minecraft:sharpness\" level 3. Подходит ли зачарование предмету, не проверяется."
        );
        cmd(
            "broadcast",
            "Речь",
            "<text:text>",
            "Сообщение в чат всем игрокам сервера, даже из блока «для игрока» и из сюжета «только игроку». message пишет только тем, кто видит сюжет."
        );
        cmd(
            "setowner",
            "Игрок и мир",
            "<player:expr>",
            "Делает игрока главным в общем сюжете: теперь player — это он. Удобно, когда главный вышел: on leave { if playercount() > 0 { setowner randomplayer() } }."
        );
        cmd(
            "pvar set",
            "Игрок и мир",
            "<name:text> <value:expr> [to <player:expr>]",
            "Своя переменная у каждого игрока: общая для всех сюжетов, хранится в мире. pvar set \"золото\" 10, pvar set \"золото\" 10 to actor. Прочитать — pvar(\"золото\")."
        );
        cmd("pvar add", "Игрок и мир", "<name:text> <value:expr> [to <player:expr>]", "Прибавляет к переменной игрока: pvar add \"золото\" 5 (или -5).");
        cmd("pvar clear", "Игрок и мир", "<name:text> [from <player:expr>]", "Удаляет переменную игрока.");
        cmd(
            "team join",
            "Игрок и мир",
            "<name:text> [to <player:expr>]",
            "Добавляет игрока в команду, создавая её при необходимости; из старой команды он выходит сам. Это обычные команды Minecraft: их видят /team и @a[team=красные]. team join \"красные\", team join \"красные\" to actor."
        );
        cmd("team leave", "Игрок и мир", "[from <player:expr>]", "Выводит игрока из его команды.");
        cmd(
            "team color",
            "Игрок и мир",
            "<name:text> <color:text>",
            "Цвет ников команды: team color \"красные\" \"red\" (red, gold, yellow, green, aqua, blue, light_purple, white, gray…)."
        );
        cmd(
            "team pvp",
            "Игрок и мир",
            "<name:text> [off]",
            "Могут ли игроки команды ранить друг друга: team pvp \"красные\" off — нет, team pvp \"красные\" — да."
        );
        cmd(
            "tag add",
            "Игрок и мир",
            "<name:text> [to <player:expr>]",
            "Даёт игроку тег: tag add \"vip\" — игроку сюжета, tag add \"vip\" to actor — игроку события. Тег хранится в мире, остаётся после смерти и выхода; это обычный тег Minecraft: его видят /tag и @a[tag=vip]. Проверка — hastag(\"vip\"), событие — on tagadd \"vip\" { … }."
        );
        cmd(
            "tag remove",
            "Игрок и мир",
            "<name:text> [from <player:expr>]",
            "Снимает тег с игрока: tag remove \"vip\", tag remove \"vip\" from actor. Событие — on tagremove \"vip\" { … }."
        );
        cmd("command", "Игрок и мир", "<command:text>", "Выполняет команду Minecraft от имени сервера. Пример: command \"time set night\"");
        cmd("blackout", "Экран", "<time:duration>", "Затемнение экрана.");
        cmd("flash", "Экран", "<time:duration>", "Белая вспышка.");
        cmd("screen", "Экран", "<r:int> <g:int> <b:int> <a:int>", "Заливает экран цветом (0–255 на канал, a — прозрачность).");
        cmd("eyes", "Экран", "<close:duration> <hold:duration> <open:duration>", "Эффект закрывающихся глаз: закрыть, подержать, открыть.");
        cmd(
            "post play",
            "Экран",
            "<name:post> [time <time:duration>] [power <power:number>]",
            "Включает пост-эффект: готовый (cinematic, noir, horror, dream, underwater, pain, vhs…) или свой из папки «Пост-эффекты». time — за сколько проявится, power — сила (1 — как задумано). Новый вид плавно сменяет прежний; держится до post clear и пока идёт сюжет."
        );
        cmd(
            "post set",
            "Экран",
            "<param:postparam> <value:expr> [time <time:duration>]",
            "Меняет одну настройку: post set bloom 1.5 time 2s, post set fog_color \"#102030\". Можно делать это прямо в ходе сцены."
        );
        cmd(
            "post pulse",
            "Экран",
            "<name:post> [in <in:duration>] [hold <hold:duration>] [out <out:duration>] [power <power:number>]",
            "Эффект на миг, сам уходит: удар, боль, вспышка ярости. Не мешает тому, что включено через post play."
        );
        cmd("post clear", "Экран", "[time <time:duration>]", "Убирает пост-эффект этого сюжета.");
        cmd(
            "js",
            "Логика",
            "<code:text>",
            "Выполняет JavaScript: js { … } (многострочный код с фигурными скобками) или js \"код в одну строку\". Видны story (переменные сюжета: story.get(\"x\"), story.set(\"x\", 5), story.character(\"alice\"), story.log(…)), player (x y z, health, give(…), effect(…), nbt(…)…) и world (command(…), block(x,y,z), setBlock(…), entities(…), summon(…), time, hasMod(…)). Классы Java — только если оператор мира разрешил: /sprautejs unsafe on."
        );
        cmd(
            "setnbt",
            "Логика",
            "<target:target> <path:text> <value:expr>",
            "Меняет NBT существа или блока с данными (сундук…): setnbt kent \"NoAI\" 1, setnbt (1, 64, 2) \"Items[0].Count\" 5. Значение может быть SNBT: \"5b\", \"{CustomName:'\\\"Имя\\\"'}\"."
        );
        cmd(
            "setattr",
            "Логика",
            "<target:target> <attr:text> <value:number>",
            "Атрибут существа: setattr kent \"generic.attack_damage\" 8 (также generic.movement_speed, generic.max_health, атрибуты модов)."
        );
        cmd(
            "setscore",
            "Логика",
            "<target:target> <objective:text> <value:number> [add]",
            "Очки табло (scoreboard): setscore player \"kills\" 5; с add — прибавить. Табло создаётся, если его нет."
        );
        cmd(
            "global set",
            "Логика",
            "<name:text> <value:expr>",
            "Общая переменная всех сюжетов мира (сохраняется навсегда): global set \"золото\" 10. В другом скрипте: global(\"золото\")."
        );
        cmd("global add", "Логика", "<name:text> <value:expr>", "Прибавляет к общей переменной: global add \"золото\" 5 (или -5).");
        cmd("global clear", "Логика", "<name:text>", "Удаляет общую переменную.");
        cmd("flag set", "Логика", "<name:text> [value <value:text>]", "Устанавливает флаг сюжета.");
        cmd("flag clear", "Логика", "<name:text>", "Снимает флаг сюжета.");
        cmd("checkpoint", "Логика", "<name:text>", "Точка сохранения: при перезаходе сюжет продолжится отсюда.");
        cmd("restore", "Логика", "<name:text>", "Возвращает сюжет к точке сохранения.");
        cmd("ensure active", "Логика", "<ids:script...>", "Гарантирует, что указанные сюжеты запущены.");
        cmd("ensure inactive", "Логика", "<ids:script...>", "Гарантирует, что указанные сюжеты остановлены.");
        cmd("log", "Логика", "<text:text>", "Пишет строку в консоль редактора Spraute (для отладки).");
        KEYWORD_DOCS.put(
            "script",
            "Заголовок файла: script <id> \"Название\". Id — уникальный номер сюжета.\nscript 5001 \"Вступление\" autostart — сюжет сам запускается, когда игрок заходит в мир (если он ещё не идёт).\nscript 5001 \"Вступление\" solo — у каждого игрока своя копия сюжета: своё продвижение и свои персонажи, которых видит только он. Без solo сюжет один на всех: его видят все, а player — тот, кто его запустил.\nscript 5001 \"Вступление\" autostart once — только при первом входе в этот мир."
        );
        KEYWORD_DOCS.put(
            "character",
            "Объявляет персонажа:\ncharacter alice = \"spraute:npc\" name \"Алиса\" [speed 0.3] [skin \"...\"] [icon \"лицо\"] [portrait \"лицо\"]\nicon — картинка в окне речи (say), portrait — лицо в диалоге с выбором (choice).\nКартинки добавляются в папку проекта «Картинки»"
        );
        KEYWORD_DOCS.put("location", "Объявляет точку: location camp = (100, 64, 200) [yaw 90] [pitch 0] [dim \"minecraft:overworld\"]");
        KEYWORD_DOCS.put("var", "Переменная сюжета: var count = 0");
        KEYWORD_DOCS.put("label", "Метка, на которую можно перейти через goto. Также видна в панели сюжетов (Перейти к метке).");
        KEYWORD_DOCS.put("goto", "Переход к метке: goto start");
        KEYWORD_DOCS.put("if", "Условие:\nif hp > 5 {\n    ...\n} else {\n    ...\n}");
        KEYWORD_DOCS.put("else", "Ветка «иначе» для if.");
        KEYWORD_DOCS.put("while", "Цикл, пока условие истинно:\nwhile not flag(\"done\") { ... }");
        KEYWORD_DOCS.put("repeat", "Повторить блок N раз: repeat 3 { ... }");
        KEYWORD_DOCS.put(
            "for",
            "Перебор: for enemy in enemies { ... }. Список — каждый элемент; словарь — ключи; число N — 0..N-1; текст — буквы.\nfor i in range(1, 11) { say 0 \"{i}\" }"
        );
        KEYWORD_DOCS.put("in", "for x in список { ... }");
        KEYWORD_DOCS.put("every", "Повторять в фоне с интервалом, пока идёт сюжет:\nevery 5t { if near(player, 3) { ... } }\non tick { ... } — каждый тик.");
        KEYWORD_DOCS.put("break", "Выйти из цикла for, while или repeat.");
        KEYWORD_DOCS.put(
            "player",
            "Игрок. В тексте — его ник; как сущность — свойства и методы:\nplayer.health, player.pos, player.look, player.item, player.level\nset player.health = 20\nplayer.give(\"minecraft:diamond\", 3)\nТак же работают персонажи (alice.pos) и переменные с сущностями (boss.health).\nСвойства: pos x y z eyes look yaw pitch speed onground name type health maxhealth target item alive dist sneaking sprinting level food fire glowing invisible inair flying canfly flyspeed movespeed falldistance fallsafe slotsize\nМенять (set x.свойство = ...): health maxhealth name pos x y z yaw speed target ai glowing invisible gravity silent invulnerable fire item level food flying canfly flyspeed movespeed falldistance fallsafe\nundying — не умирает по-настоящему: смертельный удар оставляет половину здоровья, а сюжет получает on death (set player.undying = true — бой без возрождения вдали)\nМетоды: kill() remove() tp(pos) damage(n) heal(n) effect(id, сек, ур) cleareffects() push(вектор) dash(сила) launch(сила) knockback(от, сила) pull(к, сила) stop() lookat(цель) can(правило) rule(правило) slotlocked(номер) give(item, n) equip(слот, item) distance(цель) has(item, n) count(item)"
        );
        KEYWORD_DOCS.put("continue", "Сразу к следующему шагу цикла.");
        KEYWORD_DOCS.put("set", "Изменить переменную: set count = count + 1 (также += и -=)");
        KEYWORD_DOCS.put(
            "choice",
            "Диалог с вариантами ответа:\nchoice alice \"Пойдёшь со мной?\" {\n    \"Да\" { ... }\n    \"Нет\" { ... }\n}\nПосле него answer() вернёт номер ответа (с 1)."
        );
        KEYWORD_DOCS.put("parallel", "Параллельная ветка, идущая одновременно с основной (то же, что async с именем):\nparallel ambient { ... }");
        KEYWORD_DOCS.put(
            "func",
            "Функция — кусок сюжета, который можно вызывать много раз:\nfunc greet(who) {\n    say alice \"Привет, {who}!\"\n}\nПараметры — обычные переменные сюжета. Объявляется на верхнем уровне файла."
        );
        KEYWORD_DOCS.put("call", "Вызов функции и ожидание её конца: call greet(\"Боб\")\nasync call greet(\"Боб\") as job — запустить в фоне.");
        KEYWORD_DOCS.put("return", "Досрочно выйти из функции.");
        KEYWORD_DOCS.put(
            "async",
            "Асинхронно — не ждать завершения:\nasync { ... }          — фоновая ветка\nasync walk { ... }     — ветка с именем (для await/cancel)\nasync call f() as job  — функция в фоне"
        );
        KEYWORD_DOCS.put("await", "Ждать завершения асинхронных веток: await walk, job");
        KEYWORD_DOCS.put("cancel", "Остановить асинхронную ветку: cancel walk");
        KEYWORD_DOCS.put(
            "solo",
            "В заголовке сюжета: script 5001 \"Вступление\" solo. У каждого игрока своя копия сюжета: своё продвижение и свои персонажи, которых видит только он.\nБез solo сюжет один на всех, и все игроки видят одно и то же."
        );
        KEYWORD_DOCS.put(
            "autostart", "В заголовке сюжета: script 5001 \"Вступление\" autostart. Сюжет сам запускается, когда игрок заходит в мир (если он ещё не идёт)."
        );
        KEYWORD_DOCS.put("once", "В заголовке сюжета: autostart once. Сюжет запускается сам только при первом входе в этот мир.");
        KEYWORD_DOCS.put(
            "actor", "Игрок, вызвавший обрабатываемое событие: on key H { as actor { say player \"Это я\" } }.\nВне события — главный игрок сюжета."
        );
        KEYWORD_DOCS.put(
            "as",
            "Блок про конкретного игрока: as actor { give \"minecraft:bread\" } или for p in players() { as p { quest start \"поход\" } }.\nВнутри player — этот игрок, и всё показанное (say, gui, camera, title) видит только он. actor — игрок, вызвавший обрабатываемое событие.\nЕсли игрок вышел, остаток блока пропускается: сюжет идёт дальше и не ждёт его."
        );
        KEYWORD_DOCS.put(
            "on",
            "Обработчик события — работает в фоне и срабатывает каждый раз:\non button \"yes\" { ... }\non key H { ... }\non click alice { ... }\nСобытием может быть любая команда wait: button, guiclose, key, click, chat, near, flag, trigger...\nСтавьте обработчики в начало сюжета."
        );
        KEYWORD_DOCS.put("switch", "Выбор по значению:\nswitch button() {\n    \"yes\" { ... }\n    \"no\", \"cancel\" { ... }\n    else { ... }\n}");
        KEYWORD_DOCS.put("stop", "Завершает сюжет.");
        KEYWORD_DOCS.put("player", "Игрок. Можно использовать как цель: look alice at player");
        KEYWORD_DOCS.put("and", "Логическое И");
        KEYWORD_DOCS.put("or", "Логическое ИЛИ");
        KEYWORD_DOCS.put("not", "Логическое НЕ");
        KEYWORD_DOCS.put("true", "Истина");
        KEYWORD_DOCS.put("false", "Ложь");
        fn("random", "random(n)", "число", "Случайное целое от 0 до n-1.");
        fn("chance", "chance(percent)", "да/нет", "Истина с указанной вероятностью в процентах.");
        fn("flag", "flag(\"name\")", "да/нет", "Установлен ли флаг сюжета.");
        fn("walking", "walking(who)", "да/нет", "Идёт ли персонаж по пути (walk).");
        fn("js", "js(code)", "значение", "Значение JavaScript-кода: js(\"return player.health < 5\"), js(\"world.time\"). См. команду js.");
        fn(
            "nbt",
            "nbt(target, path)",
            "значение",
            "NBT существа или блока с данными: nbt(kent, \"Health\"), nbt((1, 64, 2), \"Items[0].id\"). Без пути — весь SNBT. Нет такого — \"\"."
        );
        fn("attr", "attr(target, id)", "число", "Атрибут существа или игрока: attr(player, \"generic.max_health\").");
        fn("score", "score(who, objective)", "число", "Очки табло: score(player, \"kills\"). Нет — 0.");
        fn(
            "cmdresult",
            "cmdresult(command)",
            "текст",
            "Выполняет команду Minecraft или мода и возвращает её ответ текстом: cmdresult(\"/time query daytime\")."
        );
        fn(
            "cmdvalue",
            "cmdvalue(command)",
            "число",
            "Выполняет команду и возвращает её числовой результат (как в /execute store): cmdvalue(\"/time query daytime\")."
        );
        fn("hasmod", "hasmod(id)", "да/нет", "Установлен ли мод: hasmod(\"kubejs\").");
        fn("loader", "loader()", "текст", "На каком загрузчике запущена игра: \"forge\" или \"fabric\".");
        fn("isforge", "isforge()", "да/нет", "Запущена ли игра на Forge.");
        fn("isfabric", "isfabric()", "да/нет", "Запущена ли игра на Fabric: if isfabric() { … } else { … }.");
        fn(
            "hasclass",
            "hasclass(\"класс\")",
            "да/нет",
            "Есть ли в игре Java-класс (мод установлен): hasclass(\"com.example.mod.api.StatsApi\"). Работает и без разрешения Java."
        );
        fn(
            "java",
            "java(what)",
            "Java-объект",
            "Java другого мода. java(\"полное.имя.Класса\") — класс: через точку его статические поля и методы; java(player) — сам объект игры. Дальше через точку — поля, свойства (getLevel() → .level) и методы: java(\"com.example.mod.api.StatsApi\").get(player).level. Числа, текст, да/нет, сущности, списки и словари становятся обычными значениями сюжета, остальное — Java-объектом. Классы самого Minecraft вне среды разработки обфусцированы, имена других модов — как в их исходниках. Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn(
            "jnew",
            "jnew(\"класс\", [args...])",
            "Java-объект",
            "Создаёт объект Java-класса: jnew(\"java.util.ArrayList\"), jnew(\"com.example.mod.Data\", 5, \"имя\"). Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn(
            "jcall",
            "jcall(target, \"method\", [args...])",
            "значение",
            "Вызывает метод по имени: jcall(obj, \"addXp\", player, 10). target — Java-объект, сущность или имя класса (тогда статический метод): jcall(\"com.example.mod.api.Roles\", \"roleOf\", player). Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn(
            "jget",
            "jget(target, \"field\")",
            "значение",
            "Поле или свойство по имени: jget(obj, \"level\"), jget(\"com.example.mod.Config\", \"MAX_LEVEL\"). Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn(
            "jset",
            "jset(target, \"field\", value)",
            "значение",
            "Меняет поле (или вызывает setField): jset(obj, \"level\", 5). То же — set obj.level = 5. Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn(
            "jclass",
            "jclass(x)",
            "текст",
            "Полное имя Java-класса объекта: jclass(player), jclass(java(\"...\").get(player)). Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn(
            "jmembers",
            "jmembers(x)",
            "список",
            "Поля и методы Java-объекта или класса — чтобы узнать, что у него есть: log jmembers(obj). Нужно разрешение оператора мира: /sprautejs unsafe on."
        );
        fn("global", "global(\"name\", [default])", "значение", "Общая переменная всех сюжетов (global set). Если её нет — default или 0.");
        fn("hasglobal", "hasglobal(\"name\")", "да/нет", "Есть ли общая переменная.");
        fn("flagvalue", "flagvalue(\"name\")", "текст", "Значение флага сюжета.");
        fn("answer", "answer()", "число", "Номер ответа в последнем choice (с 1).");
        fn("near", "near(target, radius)", "да/нет", "Находится ли игрок рядом с точкой или персонажем.");
        fn("dist", "dist(target)", "число", "Расстояние от игрока до точки или персонажа.");
        fn("health", "health()", "число", "Здоровье игрока.");
        fn("daytime", "daytime()", "число", "Время суток в тиках (0–24000).");
        fn("active", "active(id)", "да/нет", "Запущен ли сюжет с указанным id.");
        fn("running", "running(\"ветка\")", "да/нет", "Выполняется ли сейчас асинхронная ветка.");
        fn("round", "round(x)", "число", "Округляет до целого.");
        fn("floor", "floor(x)", "число", "Округляет вниз.");
        fn("randint", "randint(a, b)", "число", "Случайное целое от a до b включительно.");
        fn("len", "len(text)", "число", "Длина текста.");
        fn("contains", "contains(text, part)", "да/нет", "Содержит ли текст фрагмент (без учёта регистра).");
        fn("upper", "upper(text)", "текст", "Текст заглавными буквами.");
        fn("lower", "lower(text)", "текст", "Текст строчными буквами.");
        fn("num", "num(text)", "число", "Превращает текст в число (например, из поля ввода).");
        fn("menu", "menu()", "текст", "Только в общем скрипте. Какое меню открылось последним: title, options…");
        fn("button", "button()", "текст", "Id последней нажатой кнопки интерфейса: if button() == \"yes\" { ... }");
        fn("input", "input(\"id\")", "текст", "Текст, который игрок ввёл в поле интерфейса.");
        fn("skilllevel", "skilllevel(\"система\")", "число", "Уровень игрока в системе прокачки.");
        fn("skillpower", "skillpower()", "число", "В скрипте навыка: сколько его рангов изучено. Чтобы способность росла: damage (4 + skillpower() * 2).");
        fn(
            "evententity",
            "evententity()",
            "сущность",
            "Сущность, о которой событие: кого ударили (attack), кто ударил (hurt), кого убили (kill). Пусто, если её нет."
        );
        fn("velocity", "velocity(who)", "точка", "Как сущность движется сейчас, блоков за тик: velocity(player).y < 0 — падает.");
        fn("onground", "onground(who)", "да/нет", "Стоит ли сущность на земле.");
        fn(
            "blockarea",
            "blockarea(pos, who, width, height, [depth])",
            "список",
            "Блоки области вокруг pos, развёрнутой туда, куда смотрит who, без самого pos: кирка 3 на 3 — for b in blockarea((event(\"x\"), event(\"y\"), event(\"z\")), player, 3, 3) { breakblock b }. Смотрит вбок: width — поперёк, height — вверх и вниз. Смотрит вверх или вниз: область лежит плашмя. depth — вглубь от него."
        );
        fn("skillxp", "skillxp(\"система\")", "число", "Сколько опыта набрано на текущем уровне.");
        fn("skillxpnext", "skillxpnext(\"система\")", "число", "Сколько опыта всего нужно до следующего уровня.");
        fn("skillmaxlevel", "skillmaxlevel(\"система\")", "число", "Последний уровень системы.");
        fn("skillknows", "skillknows(\"система\", \"навык\")", "да/нет", "Изучен ли навык хоть на один ранг.");
        fn("skillcan", "skillcan(\"система\", \"навык\")", "да/нет", "Может ли игрок изучить навык прямо сейчас.");
        fn("skillrank", "skillrank(\"система\", \"навык\")", "число", "Сколько рангов навыка изучено: 0 — не изучен.");
        fn(
            "skillpoints",
            "skillpoints(\"система\")",
            "число",
            "Сколько у игрока очков системы; вторым можно указать, каких: skillpoints(\"навыки\", \"таланты\")."
        );
        fn("guizone", "guizone(\"id\")", "текст", "В какой зоне сброса лежит объект «Перетаскивание»; пусто — ни в какой.");
        fn("guiopen", "guiopen()", "да/нет", "Открыт ли сейчас интерфейс сюжета.");
        fn(
            "event",
            "event(\"поле\")",
            "текст/число",
            "Данные последнего события (внутри on ... { } или после wait события): player, block, item, count, entity, character (персонаж сюжета), name, amount, source, slot, level, id, from, to, biome, x, y, z"
        );
        fn("incutscene", "incutscene()", "да/нет", "Идёт ли сейчас катсцена этого сюжета.");
        fn("coins", "coins()", "число", "Сколько монет у игрока.");
        fn(
            "slotitem",
            "slotitem(\"id\")",
            "текст",
            "Id предмета в слоте интерфейса или пустой текст. Пример: if slotitem(\"altar\") == \"minecraft:apple\" { ... }"
        );
        fn("slotcount", "slotcount(\"id\")", "число", "Сколько предметов в слоте интерфейса.");
        fn("lastslot", "lastslot()", "текст", "Id слота, который игрок изменил последним.");
        fn("system", "system(\"health\")", "да/нет", "Включена ли система движка: health, coins, time, inventory, slots.");
        fn("playername", "playername()", "текст", "Ник игрока.");
        fn("int", "int(x)", "число", "Отбрасывает дробную часть.");
        fn("str", "str(x)", "текст", "Превращает значение в текст.");
        fn("min", "min(a, b)", "число", "Минимум.");
        fn("max", "max(a, b)", "число", "Максимум.");
        fn("abs", "abs(x)", "число", "Модуль числа.");
        fn("players", "players()", "список", "Все игроки, которые видят этот сюжет: for p in players() { as p { give \"minecraft:bread\" } }.");
        fn("playercount", "playercount()", "число", "Сколько игроков видит этот сюжет.");
        fn(
            "playerbyname",
            "playerbyname(\"ник\")",
            "сущность",
            "Игрок с этим ником на всём сервере, \"\" — такого нет в сети: var p = playerbyname(\"Readix\"); if p { as p { give \"minecraft:bread\" } }."
        );
        fn("online", "online(\"ник\")", "да/нет", "В сети ли игрок с этим ником.");
        fn(
            "allplayers",
            "allplayers()",
            "список",
            "Все игроки в сети на всём сервере, даже в сюжете «только игроку». players() — только те, кто видит этот сюжет."
        );
        fn(
            "otherplayers",
            "otherplayers()",
            "список",
            "Все игроки сервера, кроме игрока сюжета: for p in otherplayers() { as p { title \"{player} нашёл ключ\" } }."
        );
        fn("randomplayer", "randomplayer()", "сущность", "Случайный игрок на сервере; \"\" — никого нет.");
        fn(
            "nearestplayer",
            "nearestplayer(цель)",
            "сущность",
            "Ближайший игрок к точке, персонажу или другому игроку (сам он не считается) в том же мире; \"\" — никого нет."
        );
        fn(
            "playersnear",
            "playersnear(цель, радиус)",
            "список",
            "Игроки в радиусе от точки, персонажа или другого игрока, ближние первыми. Сама цель не считается: playersnear(player, 10) — кто рядом со мной."
        );
        fn("playersinzone", "playersinzone(\"зона\")", "список", "Игроки внутри зоны проекта.");
        fn(
            "owner",
            "owner()",
            "сущность",
            "Главный игрок сюжета: кто его запустил или кого назначил setowner. Если он вышел из общего сюжета — первый игрок на сервере. \"\" — никого нет."
        );
        fn("isowner", "isowner([игрок])", "да/нет", "Главный ли это игрок сюжета.");
        fn("isafk", "isafk([игрок])", "да/нет", "Стоит ли игрок без движения 3 минуты и дольше.");
        fn(
            "pvar",
            "pvar(\"имя\", [игрок], [default])",
            "значение",
            "Переменная этого игрока (pvar set). Нет такой — default или 0: pvar(\"золото\"), pvar(\"золото\", actor), pvar(\"ранг\", player, \"новичок\")."
        );
        fn("haspvar", "haspvar(\"имя\", [игрок])", "да/нет", "Есть ли у игрока такая переменная.");
        fn("teamof", "teamof([игрок])", "текст", "Имя команды игрока; \"\" — он ни в какой.");
        fn("teamplayers", "teamplayers(\"команда\")", "список", "Игроки этой команды в сети.");
        fn("sameteam", "sameteam(игрок1, игрок2)", "да/нет", "В одной ли команде два игрока.");
        fn(
            "hastag",
            "hastag(\"тег\", [игрок])",
            "да/нет",
            "Есть ли тег у игрока (без второго аргумента — у игрока сюжета): if hastag(\"vip\") { … }, hastag(\"vip\", actor)."
        );
        fn("tags", "tags([игрок])", "список", "Все теги игрока, по алфавиту, включая выданные командой /tag.");
        fn(
            "playerswithtag",
            "playerswithtag(\"тег\")",
            "список",
            "Игроки в сети на всём сервере с этим тегом: for p in playerswithtag(\"vip\") { as p { give \"minecraft:bread\" } }."
        );
        fn("day", "day()", "число", "Номер текущего дня мира (с 0).");
        fn("bought", "bought(shop, offer)", "число", "Сколько раз купили (продали) товар: bought(\"лавка\", \"sword\").");
        fn("stock", "stock(shop, offer)", "число", "Сколько товара осталось (-1 — без конца).");
        fn("hitbox", "hitbox(who)", "текст", "Как сейчас настроено тело: \"solid damage\", \"ghost\", \"normal\"…");
        fn("fighthp", "fighthp(who)", "число", "Здоровье персонажа в бою (0 — не дерётся или повержен).");
        fn("fighting", "fighting(who)", "логическое", "Дерётся ли персонаж сейчас.");
        fn("defeated", "defeated(who)", "логическое", "Повержен ли персонаж в бою.");
        fn(
            "cooldown",
            "cooldown(name, seconds)",
            "логическое",
            "Перезарядка: true — можно (и отсчёт начинается заново), false — ещё рано. if cooldown(\"рывок\", 2) { player.dash(1.5) }."
        );
        fn("cooldownleft", "cooldownleft(name)", "число", "Сколько секунд осталось до готовности.");
        fn("ticks", "ticks()", "число", "Тики сервера с запуска (20 в секунду) — для своих таймеров.");
        fn(
            "infront",
            "infront(who, [dist])",
            "точка",
            "Точка в dist блоках (3) перед игроком или персонажем — на земле, не в блоках: spawn mage at infront(player, 3)."
        );
        fn("ground", "ground(point)", "точка", "Опускает точку на пол (или поднимает из земли): ground((player.x + 5, player.y, player.z)).");
        fn("aimpoint", "aimpoint(who, [dist])", "точка", "Куда смотрит игрок или персонаж: блок под прицелом или точка на дальности (48).");
        fn(
            "raycast",
            "raycast(from, [to], [dist])",
            "словарь",
            "Луч. raycast(player) — куда смотрит игрок (или персонаж), raycast(player, 10) — не дальше 10 блоков, raycast(alice, bob) или raycast((0, 70, 0), (10, 70, 0)) — от точки к точке. Ответ: hit (\"block\", \"entity\" или \"none\"), pos — точка попадания, dist, block — id блока, blockpos, face (up, north…), entity, bone — кость модели, в которую попал, bonepos — где она. var r = raycast(player)  if r.hit == \"block\" { log r.block }"
        );
        fn(
            "bone",
            "bone(who, name, [offset])",
            "точка",
            "Где кость модели сейчас, с учётом анимации. Подходит везде, где нужна точка: tp bob to bone(alice, \"head\"). Можно имя локатора. offset — сдвиг от кости в пикселях модели: bone(alice, \"hand\", (0, -10, 0)). Только у NPC с моделью."
        );
        fn("bonedir", "bonedir(who, name)", "вектор", "Куда смотрит перёд кости, вектор длины 1: bone(alice, \"head\") + bonedir(alice, \"head\") * 3.");
        fn("bones", "bones(who)", "список", "Имена всех костей модели: log bones(alice).");
        fn(
            "lookbone",
            "lookbone(who, [dist])",
            "текст",
            "Кость модели, на которую смотрит игрок или персонаж, или пусто: if lookbone(player) == \"head\" { … }."
        );
        fn(
            "lookblock",
            "lookblock(who, [dist])",
            "точка",
            "Блок, на который смотрит игрок или персонаж (сквозь существ), или пусто: var b = lookblock(player)  ·  setblock b \"minecraft:gold_block\"."
        );
        fn(
            "lookentity",
            "lookentity(who, [dist])",
            "сущность",
            "Существо, на которое смотрит игрок или персонаж, или пусто: if lookentity(player) == alice { … }."
        );
        fn("cansee", "cansee(a, b)", "логическое", "Видит ли a цель b: между глазами и целью нет блоков (существа не мешают). cansee(alice, player).");
        fn(
            "lookingat",
            "lookingat(who, target, [dist])",
            "логическое",
            "Смотрит ли игрок или персонаж на цель: на существо — прямо в него, на точку — примерно в её сторону и без стены между."
        );
        fn("inzone", "inzone(zone, [who])", "логическое", "Внутри ли зоны: inzone(\"деревня\") — игрок; inzone(\"деревня\", alice) — сущность или точка.");
        fn("zonepoint", "zonepoint(zone)", "точка", "Случайная точка в зоне (на полу): summon(\"minecraft:zombie\", zonepoint(\"арена\")).");
        fn("zonecenter", "zonecenter(zone)", "точка", "Центр зоны (по полу).");
        fn("zones", "zones()", "список", "Зоны проекта, в которых сейчас игрок.");
        fn("queststate", "queststate(quest)", "текст", "Состояние квеста: \"none\" (не выдан), \"active\", \"completed\" или \"failed\".");
        fn("questprogress", "questprogress(quest, task)", "число", "Сколько сделано в задаче: questprogress(\"ворота\", \"zombies\").");
        fn(
            "summon",
            "summon(type, pos)",
            "сущность",
            "Призывает сущность и возвращает её: var boss = summon(\"minecraft:zombie\", (10, 64, 10)). Дальше: boss.health, set boss.name = \"Босс\", boss.kill(), on kill boss { }."
        );
        fn(
            "entities",
            "entities(type, center, [radius])",
            "список",
            "Сущности рядом (ближние первыми): entities(\"minecraft:zombie\", player, 10). Тип: id, \"mob\" — любые мобы, \"player\", \"any\"."
        );
        fn("nearest", "nearest(type, center, [radius])", "сущность", "Ближайшая сущность этого типа или пустое значение.");
        fn("alive", "alive(x)", "да/нет", "Жива ли сущность (и есть ли она ещё в мире).");
        fn("block", "block(pos)", "текст", "Id блока в точке: block((1, 64, 2)) == \"minecraft:stone\".");
        fn("blockprop", "blockprop(pos, prop)", "значение", "Свойство блока: blockprop(pos, \"facing\") — \"north\", blockprop(pos, \"powered\") — да/нет.");
        fn(
            "inbox",
            "inbox(pos, corner1, corner2)",
            "да/нет",
            "Внутри ли точка (или сущность) коробки между двумя углами: inbox(player, (0, 60, 0), (10, 70, 10))."
        );
        fn("range", "range(a, [b], [step])", "список", "Числа подряд: range(5) — 0..4, range(1, 6) — 1..5, range(0, 10, 2) — 0, 2, 4, 6, 8.");
        fn("list", "list(...)", "список", "Список из значений: list(1, 2, 3) — то же, что [1, 2, 3].");
        fn("has", "has(list, x)", "да/нет", "Есть ли x в списке (или ключ в словаре).");
        fn("indexof", "indexof(list, x)", "число", "Номер x в списке (с 0) или -1.");
        fn("keys", "keys(dict)", "список", "Ключи словаря.");
        fn("values", "values(dict)", "список", "Значения словаря.");
        fn("sum", "sum(list)", "число", "Сумма чисел списка.");
        fn("join", "join(list, [sep])", "текст", "Склеивает список в текст: join([1, 2], \" и \") — \"1 и 2\".");
        fn("split", "split(text, [sep])", "список", "Разрезает текст: split(\"a,b\", \",\") — [\"a\", \"b\"].");
        fn("sorted", "sorted(list)", "список", "Копия списка по возрастанию.");
        fn("pick", "pick(list)", "значение", "Случайный элемент списка.");
        fn("hp", "hp(who)", "число", "Здоровье: в бою (fight) — боевое, иначе обычное. 0 — нет или погиб.");
        fn("maxhp", "maxhp(who)", "число", "Полное здоровье: в бою — из стиля боя, иначе максимум существа.");
        fn("hpfrac", "hpfrac(who)", "число", "Доля здоровья 0..1 — для фаз босса: if hpfrac(boss) < 0.5 { … }.");
        fn("parts", "parts(who)", "список", "Имена частей хитбокса персонажа (из редактора сущности).");
        fn("lastpart", "lastpart(who)", "текст", "Часть, в которую попали последней: имя, \"body\" — основной хитбокс, \"\" — ещё не попадали.");
        fn("parton", "parton(who, name)", "да/нет", "Включена ли часть хитбокса.");
        fn("partpos", "partpos(who, name)", "точка", "Центр части хитбокса сейчас: fx play \"magic_hit\" at partpos(boss, \"глаз\").");
        fn("moving", "moving(who)", "да/нет", "Идёт ли сейчас приём движения: leap, charge или circle.");
        fn("beaming", "beaming(who)", "да/нет", "Горит ли сейчас его луч (beam).");
        fn("attacking", "attacking(who)", "да/нет", "Идёт ли сейчас его удар хитбоксом (attack ... nowait).");
        fn("attackhits", "attackhits(who)", "число", "Скольких задел его последний удар хитбоксом (attack).");
        fn("behind", "behind(who, [dist])", "точка", "Точка за спиной игрока или персонажа в dist блоках (3), на земле: tp boss to behind(player, 2).");
        fn(
            "around",
            "around(center, radius, angle)",
            "точка",
            "Точка на окружности вокруг цели или точки, на земле; angle — градусы (0 — на восток, +X; 90 — на юг, +Z). for a in range(0, 360, 60) { strike at around(boss, 6, a) }."
        );
        fn("randaround", "randaround(center, radius)", "точка", "Случайная точка в круге вокруг цели или точки, на земле.");
        fn("dirto", "dirto(from, to)", "точка", "Направление от одного к другому длиной 1 (по горизонтали): vmul(dirto(boss, player), 4).");
        fn("vadd", "vadd(a, b)", "точка", "Сумма точек: vadd((1, 2, 3), (0, 1, 0)).");
        fn("vsub", "vsub(a, b)", "точка", "Разность точек (направление от b к a).");
        fn("vmul", "vmul(a, k)", "точка", "Точка, умноженная на число.");
        fn("vlen", "vlen(a)", "число", "Длина вектора.");
        fn("vdist", "vdist(a, b)", "число", "Расстояние между точками.");
        fn("vnorm", "vnorm(a)", "точка", "Вектор длиной 1 в ту же сторону.");
        fn("spawn", "spawn(who, pos)", "результат", "Ставит персонажа в мир сразу, как команда spawn: set ok = spawn(alice, player). Вернёт true.");
        fn("tp", "tp(who, pos)", "результат", "Переносит персонажа (команда tp). Вернёт true.");
        fn("despawn", "despawn(who)", "результат", "Убирает персонажа из мира (команда despawn). Вернёт true.");
        fn("give", "give(\"item\", [count])", "число", "Выдаёт игроку предметы (команда give) и возвращает, сколько выдано: give(\"minecraft:apple\", 3).");
        fn(
            "take",
            "take(\"item\", [count])",
            "число",
            "Забирает у игрока предметы и возвращает, сколько удалось забрать: if take(\"minecraft:emerald\", 5) == 5 { ... }."
        );
        fn("teleport", "teleport(pos)", "результат", "Телепортирует игрока (команда teleport). Вернёт true.");
        fn(
            "effect",
            "effect(\"effect\", seconds, [level])",
            "результат",
            "Накладывает эффект на игрока (команда effect), время — в секундах: effect(\"minecraft:speed\", 10, 2)."
        );
        fn("setblock", "setblock(pos, \"block\")", "текст", "Ставит блок (команда setblock) и возвращает id блока, который был там раньше.");
        fn("explode", "explode(pos, [power])", "результат", "Взрыв в точке (команда explode), сила по умолчанию 3.");
        fn("lightning", "lightning(pos)", "результат", "Молния в точке (команда lightning).");
        fn("message", "message(text)", "результат", "Сообщение в чат (команда message).");
        fn("actionbar", "actionbar(text)", "результат", "Текст над хотбаром (команда actionbar).");
        fn("command", "command(\"command\")", "текст", "Выполняет команду Minecraft (как команда command) и возвращает её ответ.");
        fn("copies", "copies(who)", "список", "Все живые копии персонажа, которых поставил spawn с count: for z in copies(zombie) { log z.health }.");
        fn("copycount", "copycount(who)", "число", "Сколько копий персонажа сейчас живо: if copycount(zombie) == 0 { … }.");
        fn("hasrecipe", "hasrecipe(\"id\")", "да/нет", "Есть ли такой рецепт в мире. Отключённого нет: hasrecipe(\"minecraft:stick\").");
        fn("recipeknown", "recipeknown(\"id\")", "да/нет", "Открыт ли рецепт у игрока в книге рецептов.");
        fn("cancraft", "cancraft(\"id\")", "да/нет", "Хватает ли у игрока предметов на этот рецепт. Для особых рецептов без списка ингредиентов — нет.");
        fn("recipes", "recipes()", "список", "Id всех рецептов мира.");
        fn("recipesfor", "recipesfor(\"item\")", "список", "Id рецептов, которые делают этот предмет.");
        fn("recipesusing", "recipesusing(\"item\")", "список", "Id рецептов, куда этот предмет идёт ингредиентом.");
        fn("recipeoutput", "recipeoutput(\"id\")", "текст", "Какой предмет даёт рецепт.");
        fn("recipeoutputcount", "recipeoutputcount(\"id\")", "число", "Сколько штук даёт рецепт.");
        fn("recipetype", "recipetype(\"id\")", "текст", "Вид рецепта: minecraft:crafting, minecraft:smelting…");
        fn("recipeingredients", "recipeingredients(\"id\")", "список", "Что нужно рецепту: по первому варианту на каждую ячейку.");
        fn("customitems", "customitems()", "список", "Id всех предметов проектов, которые есть в игре.");
        fn("iscustomitem", "iscustomitem(\"item\")", "да/нет", "Это предмет из «Модификаций»?");
        fn("itemname", "itemname(\"item\")", "текст", "Название предмета.");
        fn("itemkind", "itemkind(\"item\")", "текст", "Вид предмета: generic, food, sword, pickaxe, axe, shovel, hoe, armor или block.");
        fn("itemmaxstack", "itemmaxstack(\"item\")", "число", "Сколько предметов помещается в стак.");
        fn("itemdurability", "itemdurability(\"item\")", "число", "Полная прочность предмета; 0 — не изнашивается.");
        fn("itemoncooldown", "itemoncooldown(\"item\")", "да/нет", "Идёт ли у игрока перезарядка этого предмета.");
        fn("helddamage", "helddamage()", "число", "Насколько изношен предмет в руке игрока.");
        fn("heldleft", "heldleft()", "число", "Сколько прочности осталось у предмета в руке игрока.");
        fn("itemcount", "itemcount(\"item\")", "число", "Сколько таких предметов у игрока (то, что выдаёт give): itemcount(\"minecraft:diamond\").");
        fn("hasitem", "hasitem(\"item\")", "да/нет", "Есть ли у игрока хоть один такой предмет.");
        fn("helditem", "helditem(who)", "текст", "Предмет в руке (то, что даёт hold): helditem(player), helditem(alice). \"minecraft:air\" — пусто.");
        fn("haseffect", "haseffect(who, \"effect\")", "да/нет", "Действует ли эффект (команда effect): haseffect(player, \"minecraft:speed\").");
        fn("effectlevel", "effectlevel(who, \"effect\")", "число", "Уровень эффекта (1, 2…), 0 — эффекта нет.");
        fn("effecttime", "effecttime(who, \"effect\")", "число", "Сколько секунд эффекту осталось, 0 — эффекта нет.");
        fn("xplevel", "xplevel()", "число", "Уровень опыта игрока.");
        fn("food", "food()", "число", "Сытость игрока (0–20).");
        fn("gamemode", "gamemode()", "текст", "Режим игры: survival, creative, adventure, spectator.");
        fn("dimension", "dimension(who)", "текст", "Измерение сущности: \"minecraft:overworld\", \"minecraft:the_nether\"…");
        fn("weather", "weather()", "текст", "Погода: clear, rain или thunder.");
        fn("biome", "biome(pos)", "текст", "Биом в точке: biome(player) == \"minecraft:plains\".");
        fn("light", "light(pos)", "число", "Освещённость в точке (0–15).");
        fn("sky", "sky(pos)", "да/нет", "Видно ли небо из точки (не под крышей).");
        fn("posof", "posof(who)", "точка", "Где сейчас сущность или персонаж: (x, y, z). Для точки — она сама.");
        fn("charname", "charname(who)", "текст", "Отображаемое имя (то, что задаёт команда name).");
        fn("targetof", "targetof(who)", "сущность", "Кого атакует моб или персонаж, \"\" — никого.");
        KEYS = List.of(
            "F1",
            "F2",
            "F3",
            "F4",
            "F5",
            "F6",
            "F7",
            "F8",
            "F9",
            "F10",
            "F11",
            "F12",
            "SPACE",
            "ENTER",
            "SHIFT",
            "TAB",
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "G",
            "H",
            "I",
            "J",
            "K",
            "L",
            "M",
            "N",
            "O",
            "P",
            "Q",
            "R",
            "S",
            "T",
            "U",
            "V",
            "W",
            "X",
            "Y",
            "Z"
        );
        EMOTIONS = Emotions.NAMES;
        ST_ENTITIES = List.of(
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc",
            "spraute:npc"
        );
    }

    public record FunctionSpec(String name, String signature, String returns, String doc) {
    }
}
