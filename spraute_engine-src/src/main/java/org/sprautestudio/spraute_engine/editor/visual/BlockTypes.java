package org.sprautestudio.spraute_engine.editor.visual;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.EventDefs;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.lang.Specs;

public final class BlockTypes {
    public static final String CAT_FLOW = "Поток";
    public static final String CAT_DECL = "Объявления";
    public static final String CAT_FUNC = "Функции";
    public static final String CAT_EVENTS = "События";
    public static final String CAT_VARS = "Переменные";
    public static final String CAT_OPS = "Операторы";
    public static final String CAT_VALUES = "Значения";
    public static final List<String> CATEGORIES = new ArrayList<>(
        List.of(
            "Поток",
            "События",
            "Функции",
            "Переменные",
            "Операторы",
            "Значения",
            "Речь",
            "Персонажи",
            "Анимации",
            "Камера",
            "Эффекты",
            "Интерфейс",
            "Ожидание",
            "Игрок и мир",
            "Экран",
            "Логика",
            "Игра и меню",
            "Рецепты и предметы",
            "Объявления"
        )
    );
    private static final Map<String, BlockTypes.Type> TYPES = new LinkedHashMap<>();
    private static final Map<String, BlockTypes.Variant> VARIANTS = new LinkedHashMap<>();
    private static final Set<String> HIDDEN = new HashSet<>();
    private static final List<String> FIRST = new ArrayList<>();
    public static final String START_SHARED = "всем игрокам";
    public static final String START_SOLO = "только игроку";
    public static final String EACH_ALL = "всех игроков";
    public static final String EACH_STORY = "игроков сюжета";
    public static final String EACH_OTHERS = "остальных игроков";
    private static final Map<String, List<String>> FIELD_CHOICES = new HashMap<>();
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    private static final Map<String, String> EVENT_TEMPLATES = Map.ofEntries(
        Map.entry("goal", "когда в квесте %name сделана цель %task"),
        Map.entry("button", "когда нажата кнопка %buttons"),
        Map.entry("guiclose", "когда закрыт интерфейс"),
        Map.entry("slot", "когда игрок изменил слот %slots"),
        Map.entry("key", "когда нажата клавиша %key"),
        Map.entry("click", "когда игрок кликнул по %who"),
        Map.entry("clickblock", "когда игрок кликнул по блоку типа %blocktype в точке %pos"),
        Map.entry("chat", "когда игрок написал %words"),
        Map.entry("near", "когда игрок рядом с %target радиус %radius"),
        Map.entry("flag", "когда поставлен флаг %name"),
        Map.entry("trigger", "когда сработал триггер"),
        Map.entry("enter", "когда игрок вошёл в зону %from – %to"),
        Map.entry("exit", "когда игрок вышел из зоны %from – %to"),
        Map.entry("visible", "когда игрок увидел %who")
    );
    private static final Map<String, String> FUNCTION_TITLES = Map.ofEntries(
        Map.entry("random", "случайное до %p0"),
        Map.entry("chance", "шанс %p0 %"),
        Map.entry("flag", "флаг %p0 стоит?"),
        Map.entry("flagvalue", "значение флага %p0"),
        Map.entry("global", "общая %p0"),
        Map.entry("js", "JS %p0"),
        Map.entry("nbt", "NBT %p0 путь %p1"),
        Map.entry("attr", "атрибут %p1 у %p0"),
        Map.entry("score", "очки %p1 у %p0"),
        Map.entry("cmdresult", "ответ команды %p0"),
        Map.entry("cmdvalue", "результат команды %p0"),
        Map.entry("hasmod", "есть мод %p0 ?"),
        Map.entry("hasclass", "есть Java-класс %p0 ?"),
        Map.entry("loader", "загрузчик игры"),
        Map.entry("isforge", "игра на Forge?"),
        Map.entry("isfabric", "игра на Fabric?"),
        Map.entry("java", "Java %p0"),
        Map.entry("jnew", "новый Java %p0 из %p1"),
        Map.entry("jcall", "Java: у %p0 вызвать %p1 с %p2"),
        Map.entry("jget", "Java: у %p0 поле %p1"),
        Map.entry("jset", "Java: у %p0 поле %p1 = %p2"),
        Map.entry("jclass", "Java-класс %p0"),
        Map.entry("jmembers", "Java: что есть у %p0"),
        Map.entry("hasglobal", "есть общая %p0 ?"),
        Map.entry("answer", "номер ответа"),
        Map.entry("near", "игрок рядом с %p0 радиус %p1 ?"),
        Map.entry("dist", "расстояние до %p0"),
        Map.entry("health", "здоровье игрока"),
        Map.entry("daytime", "время суток"),
        Map.entry("active", "сюжет %p0 запущен?"),
        Map.entry("playername", "ник игрока"),
        Map.entry("int", "целое от %p0"),
        Map.entry("str", "текст из %p0"),
        Map.entry("min", "минимум %p0 %p1"),
        Map.entry("max", "максимум %p0 %p1"),
        Map.entry("abs", "модуль %p0"),
        Map.entry("running", "ветка %p0 работает?"),
        Map.entry("round", "округлить %p0"),
        Map.entry("floor", "округлить вниз %p0"),
        Map.entry("randint", "случайное от %p0 до %p1"),
        Map.entry("len", "длина %p0"),
        Map.entry("contains", "%p0 содержит %p1 ?"),
        Map.entry("upper", "заглавными %p0"),
        Map.entry("lower", "строчными %p0"),
        Map.entry("num", "число из %p0"),
        Map.entry("button", "нажатая кнопка"),
        Map.entry("input", "текст поля %p0"),
        Map.entry("guiopen", "интерфейс открыт?"),
        Map.entry("slotitem", "предмет в слоте %p0"),
        Map.entry("slotcount", "количество в слоте %p0"),
        Map.entry("lastslot", "изменённый слот"),
        Map.entry("spawn", "заспавнить %p0 у %p1"),
        Map.entry("tp", "перенести %p0 к %p1"),
        Map.entry("despawn", "убрать %p0"),
        Map.entry("give", "выдать %p0 × %p1"),
        Map.entry("take", "забрать %p0 × %p1"),
        Map.entry("teleport", "телепортировать игрока к %p0"),
        Map.entry("effect", "эффект %p0 на %p1 с ур. %p2"),
        Map.entry("setblock", "поставить в %p0 блок %p1"),
        Map.entry("explode", "взрыв в %p0 сила %p1"),
        Map.entry("lightning", "молния в %p0"),
        Map.entry("message", "сообщение %p0"),
        Map.entry("actionbar", "над хотбаром %p0"),
        Map.entry("command", "выполнить команду %p0"),
        Map.entry("hasrecipe", "есть рецепт %p0 ?"),
        Map.entry("recipeknown", "игрок знает рецепт %p0 ?"),
        Map.entry("cancraft", "игроку хватает на рецепт %p0 ?"),
        Map.entry("recipes", "все рецепты"),
        Map.entry("recipesfor", "рецепты предмета %p0"),
        Map.entry("recipesusing", "рецепты из %p0"),
        Map.entry("recipeoutput", "что даёт рецепт %p0"),
        Map.entry("recipeoutputcount", "сколько даёт рецепт %p0"),
        Map.entry("recipetype", "вид рецепта %p0"),
        Map.entry("recipeingredients", "что нужно рецепту %p0"),
        Map.entry("customitems", "предметы проектов"),
        Map.entry("iscustomitem", "%p0 из модификаций ?"),
        Map.entry("itemname", "название %p0"),
        Map.entry("itemkind", "вид предмета %p0"),
        Map.entry("itemmaxstack", "размер стака %p0"),
        Map.entry("itemdurability", "прочность %p0"),
        Map.entry("itemoncooldown", "перезарядка %p0 идёт ?"),
        Map.entry("helddamage", "износ предмета в руке"),
        Map.entry("heldleft", "прочность предмета в руке"),
        Map.entry("copies", "живые копии %p0"),
        Map.entry("copycount", "сколько живых копий %p0"),
        Map.entry("itemcount", "сколько у игрока %p0"),
        Map.entry("hasitem", "у игрока есть %p0 ?"),
        Map.entry("helditem", "предмет в руке у %p0"),
        Map.entry("haseffect", "на %p0 действует %p1 ?"),
        Map.entry("effectlevel", "уровень эффекта %p1 у %p0"),
        Map.entry("effecttime", "секунд эффекта %p1 у %p0"),
        Map.entry("xplevel", "уровень опыта игрока"),
        Map.entry("food", "сытость игрока"),
        Map.entry("gamemode", "режим игры"),
        Map.entry("dimension", "измерение %p0"),
        Map.entry("weather", "погода"),
        Map.entry("biome", "биом в %p0"),
        Map.entry("light", "свет в %p0"),
        Map.entry("sky", "видно небо из %p0 ?"),
        Map.entry("posof", "позиция %p0"),
        Map.entry("charname", "имя %p0"),
        Map.entry("targetof", "цель %p0"),
        Map.entry("players", "все игроки"),
        Map.entry("playercount", "сколько игроков"),
        Map.entry("playerbyname", "игрок с ником %p0"),
        Map.entry("online", "игрок %p0 в сети ?"),
        Map.entry("allplayers", "все игроки сервера"),
        Map.entry("otherplayers", "остальные игроки"),
        Map.entry("randomplayer", "случайный игрок"),
        Map.entry("nearestplayer", "ближайший игрок к %p0"),
        Map.entry("playersnear", "игроки рядом с %p0 радиус %p1"),
        Map.entry("playersinzone", "игроки в зоне %p0"),
        Map.entry("owner", "главный игрок сюжета"),
        Map.entry("isowner", "%p0 главный игрок ?"),
        Map.entry("isafk", "%p0 отошёл ?"),
        Map.entry("pvar", "переменная игрока %p0 у %p1 иначе %p2"),
        Map.entry("haspvar", "есть переменная игрока %p0 у %p1 ?"),
        Map.entry("teamof", "команда %p0"),
        Map.entry("teamplayers", "игроки команды %p0"),
        Map.entry("sameteam", "%p0 и %p1 в одной команде ?"),
        Map.entry("hastag", "у %p1 есть тег %p0 ?"),
        Map.entry("tags", "теги %p0"),
        Map.entry("playerswithtag", "игроки с тегом %p0")
    );
    private static final Map<String, String> PARAM_LABELS = Map.ofEntries(
        Map.entry("who", "кто"),
        Map.entry("player", "игрок сюжета"),
        Map.entry("motion", "(x, y, z)"),
        Map.entry("delay", "0.5s"),
        Map.entry("text", "текст"),
        Map.entry("slot", "слот"),
        Map.entry("time", "время"),
        Map.entry("pitch", "наклон"),
        Map.entry("reach", "нажать с"),
        Map.entry("range", "видно с"),
        Map.entry("scale", "1"),
        Map.entry("id", "id"),
        Map.entry("bone", "кость"),
        Map.entry("rule", "правило"),
        Map.entry("which", "3-5, hotbar, main"),
        Map.entry("task", "задача"),
        Map.entry("param", "настройка"),
        Map.entry("power", "1"),
        Map.entry("in", "появл."),
        Map.entry("out", "уход"),
        Map.entry("blocktype", "любой"),
        Map.entry("pos", "точка"),
        Map.entry("target", "цель"),
        Map.entry("speed", "скорость"),
        Map.entry("name", "имя"),
        Map.entry("points", "точки"),
        Map.entry("radius", "радиус"),
        Map.entry("condition", "условие"),
        Map.entry("key", "клавиша"),
        Map.entry("words", "слова через запятую"),
        Map.entry("from", "угол 1"),
        Map.entry("to", "угол 2"),
        Map.entry("item", "предмет"),
        Map.entry("items", "ID через запятую"),
        Map.entry("hp", "здоровье"),
        Map.entry("times", "1"),
        Map.entry("effect", "эффект"),
        Map.entry("level", "0"),
        Map.entry("sound", "звук"),
        Map.entry("count", "1"),
        Map.entry("command", "команда"),
        Map.entry("value", "значение"),
        Map.entry("ids", "id сюжетов"),
        Map.entry("subtitle", "подзаголовок"),
        Map.entry("min", "мин"),
        Map.entry("max", "макс"),
        Map.entry("fadeIn", "появл."),
        Map.entry("stay", "показ"),
        Map.entry("fadeOut", "исчезн."),
        Map.entry("close", "закрыть"),
        Map.entry("hold", "держать"),
        Map.entry("open", "открыть"),
        Map.entry("r", "0"),
        Map.entry("g", "0"),
        Map.entry("b", "0"),
        Map.entry("a", "255"),
        Map.entry("step", "сек"),
        Map.entry("rise", "сек"),
        Map.entry("tilt", "градусы"),
        Map.entry("by", "(x, y, z)"),
        Map.entry("at", "точка"),
        Map.entry("height", "высота"),
        Map.entry("offset", "(вправо, вверх, вперёд)"),
        Map.entry("yaw", "0"),
        Map.entry("opacity", "0..1"),
        Map.entry("color", "#RRGGBB"),
        Map.entry("smooth", "сек"),
        Map.entry("bob", "блоков"),
        Map.entry("look", "вид"),
        Map.entry("turn", "поворот"),
        Map.entry("anim", "анимация"),
        Map.entry("layer", "action"),
        Map.entry("fade", "плавно"),
        Map.entry("state", "idle"),
        Map.entry("result", "результат"),
        Map.entry("input", "из чего"),
        Map.entry("xp", "0.1"),
        Map.entry("amount", "сколько"),
        Map.entry("enchant", "зачарование"),
        Map.entry("a1", "·"),
        Map.entry("a2", "·"),
        Map.entry("a3", "·"),
        Map.entry("b1", "·"),
        Map.entry("b2", "·"),
        Map.entry("b3", "·"),
        Map.entry("c1", "·"),
        Map.entry("c2", "·"),
        Map.entry("c3", "·"),
        Map.entry("spread", "блоков"),
        Map.entry("el", "элемент"),
        Map.entry("buttons", "любой"),
        Map.entry("prop", "свойство"),
        Map.entry("image", "картинка")
    );

    private BlockTypes() {
    }

    public static BlockTypes.Variant variant(String key) {
        return VARIANTS.get(key);
    }

    public static boolean hidden(String key) {
        return HIDDEN.contains(key);
    }

    public static String eachPlayerCode(String who) {
        return switch (who) {
            case "игроков сюжета" -> "players()";
            case "остальных игроков" -> "otherplayers()";
            default -> "allplayers()";
        };
    }

    public static List<String> choices(String key, String field) {
        List<String> own = FIELD_CHOICES.get(key + "/" + field);
        if (own != null) {
            return own;
        } else {
            BlockTypes.Variant v = VARIANTS.get(key);
            return v != null ? v.modes() : null;
        }
    }

    private static void moded(String key, String category, BlockTypes.Shape shape, String template, String doc, String[] modes, BlockTypes.Field... fields) {
        Map<String, String> codes = new LinkedHashMap<>();

        for (int i = 0; i + 1 < modes.length; i += 2) {
            codes.put(modes[i], modes[i + 1]);
        }

        String field = null;

        for (BlockTypes.Field f : fields) {
            if (f.kind() == ParamType.CHOICE) {
                field = f.name();
            }
        }

        List<BlockTypes.Field> list = List.of(fields);
        TYPES.put(key, new BlockTypes.Type(key, category, shape, parse(template, list), list, List.of(), null, doc, codes.values().iterator().next()));
        VARIANTS.put(key, new BlockTypes.Variant(field, codes));
    }

    private static void rep(String key, String category, BlockTypes.Shape shape, String template, String code, String doc, BlockTypes.Field... fields) {
        List<BlockTypes.Field> list = List.of(fields);
        TYPES.put(key, new BlockTypes.Type(key, category, shape, parse(template, list), list, List.of(), null, doc, code));
    }

    private static BlockTypes.Field f(String name, String label, ParamType kind, boolean optional, String def) {
        boolean quote = kind == ParamType.TEXT
            || kind == ParamType.SOUND
            || kind == ParamType.ITEM
            || kind == ParamType.EFFECT
            || kind == ParamType.ENTITY
            || kind == ParamType.ANIM;
        return new BlockTypes.Field(name, label, kind, optional, def, quote, false, null, 0, false);
    }

    private static void special(
        String key, String category, BlockTypes.Shape shape, String template, List<String> slots, String doc, BlockTypes.Field... fields
    ) {
        List<BlockTypes.Field> list = List.of(fields);
        TYPES.put(key, new BlockTypes.Type(key, category, shape, parse(template, list), list, slots, null, doc, null));
    }

    private static List<BlockTypes.Part> parse(String template, List<BlockTypes.Field> fields) {
        List<BlockTypes.Part> parts = new ArrayList<>();
        StringBuilder text = new StringBuilder();

        for (String word : template.split(" ")) {
            if (word.startsWith("%")) {
                String name = word.substring(1);
                int cut = name.length();

                while (cut > 0 && !Character.isLetterOrDigit(name.charAt(cut - 1)) && name.charAt(cut - 1) != '_') {
                    cut--;
                }

                String tail = name.substring(cut);
                name = name.substring(0, cut);
                BlockTypes.Field match = null;

                for (BlockTypes.Field f : fields) {
                    if (f.name.equals(name)) {
                        match = f;
                    }
                }

                if (match != null) {
                    if (text.length() > 0) {
                        parts.add(new BlockTypes.Part(text.toString().trim(), null));
                    }

                    text.setLength(0);
                    parts.add(new BlockTypes.Part(null, match));
                    if (!tail.isEmpty()) {
                        text.append(tail).append(' ');
                    }
                    continue;
                }
            }

            text.append(word).append(' ');
        }

        if (text.length() > 0) {
            parts.add(new BlockTypes.Part(text.toString().trim(), null));
        }

        return parts;
    }

    public static BlockTypes.Type get(String key) {
        return TYPES.get(key);
    }

    private static void commandBlock(CommandSpec spec) {
        List<BlockTypes.Field> fields = new ArrayList<>();
        int gi = 0;

        for (CommandSpec.Group g : spec.groups) {
            if (g.isFlag()) {
                fields.add(new BlockTypes.Field(g.keyword, "нет", ParamType.BOOL, true, "", false, false, g.keyword, gi++, true));
            } else {
                for (CommandSpec.Param p : g.params) {
                    boolean quote = !p.vararg
                        && (
                            p.type == ParamType.TEXT
                                || p.type == ParamType.SOUND
                                || p.type == ParamType.ITEM
                                || p.type == ParamType.EFFECT
                                || p.type == ParamType.ENTITY
                                || p.type == ParamType.ANIM
                                || p.type == ParamType.GUI
                                || p.type == ParamType.GUIEL
                                || p.type == ParamType.IMAGE
                                || p.type == ParamType.CUTSCENE
                                || p.type == ParamType.VFX
                                || p.type == ParamType.POST
                                || p.type == ParamType.CAMRIG
                                || p.type == ParamType.STRUCTURE
                                || p.type == ParamType.BUILDSTYLE
                                || p.type == ParamType.STEALTHSHAPE
                                || p.type == ParamType.STEALTH
                                || p.type == ParamType.SKILLTREE
                                || p.type == ParamType.SKILLNODE
                                || p.type == ParamType.SKILLMENU
                                || p.type == ParamType.PANELTURN
                                || p.type == ParamType.PANELLOOK
                                || p.type == ParamType.SLOTORDER
                                || p.type == ParamType.SLOTSET
                                || p.type == ParamType.PLAYERCAN
                                || p.type == ParamType.PLAYERSTAT
                                || p.type == ParamType.PLAYERRULE
                                || p.type == ParamType.SHOP
                                || p.type == ParamType.QUEST
                                || p.type == ParamType.TASK
                                || p.type == ParamType.ZONE
                                || p.type == ParamType.FIGHT
                                || p.type == ParamType.PATH
                        );

                    String def = switch (p.type) {
                        case LOC -> g.optional ? "" : "(0, 64, 0)";
                        case SPEAKER -> "0";
                        default -> "";
                        case DURATION -> g.optional ? "" : "1s";
                        case TEXT -> !g.optional && !p.vararg ? "текст" : "";
                    };
                    EventDefs.Def evd = spec.head.equals("wait") && spec.sub != null ? EventDefs.get(spec.sub) : null;
                    String label = evd != null && p.name.equals("what") ? "любой " + evd.filterLabel() : PARAM_LABELS.getOrDefault(p.name, p.name);
                    fields.add(new BlockTypes.Field(p.name, label, p.type, g.optional, def, quote, p.vararg, g.keyword, gi, false));
                }

                gi++;
            }
        }

        String template = TEMPLATES.getOrDefault(spec.key, spec.key);
        List<BlockTypes.Part> parts = parse(template, fields);

        for (BlockTypes.Field fl : fields) {
            boolean present = parts.stream().anyMatch(pt -> pt.field() == fl);
            if (!present) {
                parts.add(new BlockTypes.Part(fl.keyword != null ? fl.keyword : fl.name, null));
                parts.add(new BlockTypes.Part(null, fl));
            }
        }

        String key = "cmd:" + spec.key;
        TYPES.put(
            key, new BlockTypes.Type(key, spec.category, BlockTypes.Shape.COMMAND, parts, fields, List.of(), spec, spec.signature() + "\n" + spec.doc, null)
        );
        if (spec.head.equals("wait") && spec.sub != null && !spec.sub.equals("random") && !spec.sub.equals("until")) {
            EventDefs.Def evd = EventDefs.get(spec.sub);
            String et = EVENT_TEMPLATES.getOrDefault(
                spec.sub, evd != null ? "когда " + evd.title() + (evd.filter() != null ? " %what" : "") : "когда " + template.replaceFirst("^ждать ?", "")
            );
            List<BlockTypes.Part> eparts = parse(et, fields);

            for (BlockTypes.Field fl : fields) {
                boolean present = eparts.stream().anyMatch(pt -> pt.field() == fl);
                if (!present) {
                    eparts.add(new BlockTypes.Part(fl.keyword != null ? fl.keyword : fl.name, null));
                    eparts.add(new BlockTypes.Part(null, fl));
                }
            }

            String ek = "on:" + spec.sub;
            TYPES.put(
                ek,
                new BlockTypes.Type(
                    ek,
                    "События",
                    BlockTypes.Shape.C,
                    eparts,
                    fields,
                    List.of(""),
                    spec,
                    "Событие: блоки внутри выполняются в фоне каждый раз, когда это происходит — сюжет при этом идёт дальше.\nПоставьте его в сюжет до того места, с которого нужно слушать событие.\n"
                        + (evd != null ? "Данные — блок «данные события»: " + EventDefs.fieldsDoc(evd) + "\n" : "")
                        + "В тексте: on "
                        + spec.sub
                        + " ... { }",
                    null
                )
            );
        }
    }

    private static void functionBlock(Specs.FunctionSpec fs) {
        String sig = fs.signature();
        String inner = sig.substring(sig.indexOf(40) + 1, sig.lastIndexOf(41)).trim();
        List<BlockTypes.Field> fields = new ArrayList<>();
        StringBuilder tpl = new StringBuilder(fs.name());
        StringBuilder code = new StringBuilder(fs.name()).append('(');
        if (!inner.isEmpty()) {
            String[] args = inner.split(",");

            for (int i = 0; i < args.length; i++) {
                String raw = args[i].trim();
                boolean optional = raw.startsWith("[");
                if (optional) {
                    raw = raw.substring(1, raw.length() - 1);
                }

                boolean text = raw.startsWith("\"");
                String name = "p" + i;
                fields.add(new BlockTypes.Field(name, raw.replace("\"", ""), text ? ParamType.TEXT : ParamType.EXPR, optional, "", text, false, null, 0, false));
                tpl.append(" %").append(name);
                String arg = (i > 0 ? ", " : "") + "%" + name;
                code.append(optional ? "[" + arg + "]" : arg);
            }
        }

        code.append(')');
        CommandSpec same = Specs.command(fs.name());
        String cat = List.of("button", "input", "guiopen", "slotitem", "slotcount", "lastslot").contains(fs.name())
            ? "Интерфейс"
            : ((same == null || !fs.returns().equals("результат")) && !fs.name().equals("take") ? "Значения" : (same != null ? same.category : "Игрок и мир"));
        BlockTypes.Shape shape = fs.returns().equals("да/нет") ? BlockTypes.Shape.BOOLEAN : BlockTypes.Shape.REPORTER;
        List<BlockTypes.Part> parts = parse(FUNCTION_TITLES.getOrDefault(fs.name(), tpl.toString()), fields);
        String key = "fn:" + fs.name();
        TYPES.put(
            key, new BlockTypes.Type(key, cat, shape, parts, fields, List.of(), null, fs.signature() + " → " + fs.returns() + "\n" + fs.doc(), code.toString())
        );
    }

    public static void addCommand(CommandSpec spec, String words) {
        if (words != null) {
            TEMPLATES.put(spec.key, words);
        }

        if (!CATEGORIES.contains(spec.category)) {
            CATEGORIES.add(spec.category);
        }

        commandBlock(spec);
    }

    public static void addFunction(Specs.FunctionSpec fs) {
        functionBlock(fs);
    }

    public static void addChoices(String blockKey, String field, List<String> choices) {
        FIELD_CHOICES.put(blockKey + "/" + field, List.copyOf(choices));
    }

    public static Collection<BlockTypes.Type> all() {
        return TYPES.values();
    }

    public static List<BlockTypes.Type> inCategory(String category) {
        List<BlockTypes.Type> list = new ArrayList<>();

        for (String k : FIRST) {
            BlockTypes.Type t = TYPES.get(k);
            if (t != null && t.category.equals(category)) {
                list.add(t);
            }
        }

        for (BlockTypes.Type t : TYPES.values()) {
            if (t.category.equals(category) && !HIDDEN.contains(t.key) && !list.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }

    static {
        String[][] t = new String[][]{
            {"speaker", "говорящий в слоте %slot — %who"},
            {"say", "%who говорит %text не ждать F8 %nowait"},
            {"think", "подумать %text не ждать F8 %nowait"},
            {"clearchat", "очистить окно речи"},
            {"title", "заголовок %text подзаголовок %subtitle появление %fadeIn показ %stay исчезание %fadeOut"},
            {"message", "написать в чат %text"},
            {"web", "веб-сообщение %text"},
            {"warn", "веб-предупреждение %text"},
            {"wait", "ждать %time"},
            {"wait random", "ждать случайно от %min до %max"},
            {"wait until", "ждать, пока %condition"},
            {"wait flag", "ждать флаг %name"},
            {"wait key", "ждать клавишу %key"},
            {"wait trigger", "ждать триггер"},
            {"wait chat", "ждать в чате слова %words"},
            {"wait click", "ждать клик по %who"},
            {"wait clickblock", "ждать клик по блоку типа %blocktype в точке %pos"},
            {"wait near", "ждать, пока игрок у %target радиус %radius"},
            {"wait enter", "ждать вход в зону от %from до %to"},
            {"wait exit", "ждать выход из зоны от %from до %to"},
            {"wait visible", "ждать, пока игрок увидит %who"},
            {"spawn", "поставить %who в %pos размер %scale копий %count разброс %spread"},
            {
                    "gui place",
                    "поставить интерфейс %name в %pos ширина %scale наклон %pitch id %id нажать с %reach видно с %range к игроку %face вид %look поворот %turn непрозрачность %opacity оттенок %color сквозь стены %through один размер %keepsize таять с %fade плавность %smooth парит %bob"
            },
            {
                    "gui attach",
                    "прицепить интерфейс %name к %who кость %bone сдвиг %offset ширина %scale наклон %pitch поворот к телу %yaw id %id нажать с %reach видно с %range к игроку %face видит сам %self вид %look поворот %turn непрозрачность %opacity оттенок %color сквозь стены %through один размер %keepsize таять с %fade плавность %smooth парит %bob"
            },
            {"gui remove", "убрать интерфейс из мира %id"},
            {"slot lock", "закрыть ячейки %which игрок %player"},
            {"slot unlock", "открыть ячейки %which игрок %player"},
            {"slot size", "ячеек в инвентаре %count игрок %player"},
            {"slot order", "как закрывать ячейки %order игрок %player"},
            {"slot reset", "ячейки как в настройках, игрок %player"},
            {"slot icon", "текстура замка на ячейках %image"},
            {"rule deny", "запретить игроку %rule на время %time игрок %player"},
            {"rule allow", "разрешить игроку %rule на время %time игрок %player"},
            {"rule set", "настроить игроку %rule = %value на время %time игрок %player"},
            {"rule reset", "вернуть как было %rule игрок %player"},
            {"build place", "построить %name в %pos анимация %style за %time полёт %flight поворот %rotate зеркало %mirror воздух %air id %id не ждать %nowait"},
            {"build undo", "убрать постройку %id анимация %style за %time не ждать %nowait"},
            {"build stop", "закончить постройку %id сразу"},
            {"blocks move", "сдвинуть блоки от %from до %to на %by за %time плавность %ease id %id не ждать %nowait"},
            {"blocks back", "вернуть блоки %id за %time плавность %ease не ждать %nowait"},
            {"blocks stop", "закончить сдвиг блоков %id сразу"},
            {"blocks shake", "трясти блоки вокруг %at радиус %radius или от %from до %to размах %power за %time как одно целое %together"},
            {"blocks wave", "волна по блокам от %at радиус %radius высота %height за %time от блока к блоку %step в воздухе %rise наклон %tilt"},
            {"camera rig", "камера игрока %name за %time"},
            {"camera rigset", "настройка камеры игрока %param = %value за %time"},
            {"camera rigoff", "обычная камера за %time"},
            {"post play", "пост-эффект %name за %time сила %power"},
            {"post set", "настройка пост-эффекта %param = %value за %time"},
            {"post pulse", "пост-эффект на миг %name появление %in держать %hold уход %out сила %power"},
            {"post clear", "убрать пост-эффект за %time"},
            {"tp", "переместить %who в %pos"},
            {"despawn", "убрать %who"},
            {"move", "%who идёт к %target скорость %speed"},
            {"path", "%who идёт по маршруту %points скорость %speed"},
            {"look", "%who смотрит на %target"},
            {"follow", "%who следует за игроком скорость %speed дистанция %min – %max без телепорта %noteleport"},
            {"walkspeed", "скорость ходьбы %who = %speed"},
            {"wander", "%who бродит скорость %speed радиус %radius"},
            {"sit", "%who садится"},
            {"stand", "%who встаёт"},
            {"name", "имя %who = %name"},
            {"hold", "%who держит %item"},
            {"drop", "%who выбрасывает %item штук %count к %target сила %speed инерция %motion в переменную %var"},
            {"spawnitem", "создать предмет %item в %pos штук %count инерция %motion не подобрать %delay совсем нельзя %nopickup в переменную %var"},
            {"pickup", "%who подбирает предметы с ID %items всего штук %count остановить %off"},
            {"health", "здоровье %who = %hp"},
            {"free", "отпустить %who"},
            {"anim", "%who играет %anim слой %layer скорость %speed переход %fade раз %times цикл %loop замереть %hold ждать %wait"},
            {"face", "мимика %who %anim переход %fade"},
            {"gui open", "открыть интерфейс %name"},
            {"gui close", "закрыть интерфейс"},
            {"gui text", "интерфейс: текст %el = %text"},
            {"gui show", "показать элемент %el"},
            {"gui hide", "скрыть элемент %el"},
            {"gui progress", "полоса %el = %value"},
            {"gui image", "картинка элемента %el = %image"},
            {"gui set", "интерфейс: %el . %prop = %value"},
            {"gui undrag", "интерфейс: вернуть на место %el"},
            {"gui scroll", "прокрутить %el до %y"},
            {"wait button", "ждать нажатия кнопки %buttons"},
            {"wait guiclose", "ждать закрытия интерфейса"},
            {"gui slot", "положить в слот %el предмет %item количество %count"},
            {
                    "orb spawn",
                    "частицы %id у %pos количество %count монеты %coins опыт %xp ценность %value картинка %image предмет %item цвет %color размер %size притяжение %radius живут %life разлёт %spread только украшение %nopickup парят %float"
            },
            {"orb clear", "убрать частицы %id"},
            {"cutscene play", "катсцена %name от точки %pos не ждать %nowait"},
            {"cutscene stop", "прервать катсцену"},
            {"cutscene continue", "катсцена: идти дальше с кадра"},
            {"camera at", "камера в %pos смотрит на %look поворот %yaw наклон %pitch за %time плавность %ease"},
            {"camera look", "камера смотрит на %target за %time плавность %ease"},
            {"camera turn", "повернуть камеру: поворот %yaw наклон %pitch за %time плавность %ease"},
            {"camera follow", "камера следует за %who расстояние %distance высота %height угол %angle"},
            {"camera orbit", "камера облетает %who радиус %radius высота %height скорость %speed"},
            {"camera reset", "вернуть камеру игроку за %time"},
            {"camera fov", "зум камеры (FOV) %value за %time плавность %ease"},
            {"camera roll", "наклон камеры %value за %time плавность %ease"},
            {"camera shake", "тряска камеры %value затухает за %time"},
            {"camera bars", "кинополосы %value за %time"},
            {"camera fade", "затемнение %value цвет %color за %time"},
            {"camera hud", "интерфейс игрока виден %value"},
            {"camera hide", "скрыть интерфейс %what"},
            {"camera lock", "игрок заморожен %value"},
            {"fx play", "эффект %name у %pos кость %bone масштаб %scale следовать %follow id %id"},
            {"fx stop", "погасить эффект %id"},
            {"fx clear", "погасить все эффекты"},
            {"particles", "частицы Minecraft %type у %pos кость %bone количество %count разлёт %spread скорость %speed"},
            {"coins give", "дать монеты %amount"},
            {"coins take", "забрать монеты %amount"},
            {"dungeon place", "поставить данж %name в %pos"},
            {"mob spawn", "заспавнить моба %name в %pos штук %count в переменную %var"},
            {"loot give", "дать игроку лут из таблицы %name"},
            {"loot chest", "положить лут из таблицы %name в хранилище %pos"},
            {"coins set", "монет стало %amount"},
            {"coins drop", "уронить монеты %amount у %pos сфер %count"},
            {"overlay show", "показать наложение %name"},
            {"overlay hide", "убрать наложение %name"},
            {"gui move", "сдвинуть %el в X %x Y %y за %time"},
            {"gui resize", "размер %el ширина %w высота %h за %time"},
            {"gui animate", "анимировать %el свойство %prop до %value за %time"},
            {"gui clearslot", "очистить слот %el"},
            {"wait slot", "ждать изменения слота %slots"},
            {"stopanim", "остановить анимацию %who слой %layer переход %fade"},
            {"clearanims", "сбросить все анимации %who переход %fade"},
            {"moveanim", "%who : анимация состояния %state = %anim"},
            {"resetanim", "%who : стандартные анимации движения"},
            {"nod", "%who кивает %times раз"},
            {"nope", "%who качает головой %times раз"},
            {"laugh", "%who смеётся %times раз скорость %speed"},
            {"sigh", "%who вздыхает"},
            {"shake", "%who дрожит %time"},
            {"teleport", "телепорт игрока в %pos"},
            {"effect", "эффект %effect на %time уровень %level"},
            {"sound", "звук %sound в %pos громкость %volume высота %pitch по кругу %loop имя %id нарастает за %fade"},
            {"stopsound", "остановить звук %id затухает за %fade"},
            {
                    "highlight",
                    "подсветить %who заливка %fill прозрачность %fillalpha обводка %outline прозрачность %outlinealpha толщина %width только не за блоками %occluded имя %id на %time"
            },
            {"highlight off", "убрать подсветку с %who"},
            {"highlight clear", "убрать подсветку %id"},
            {"gizmo line", "линия от %from до %to цвет %color толщина %width сквозь блоки %through имя %id на %time"},
            {"gizmo box", "коробка от %from до %to рёбра %color заливка %fill прозрачность %fillalpha сквозь блоки %through имя %id на %time"},
            {"gizmo sphere", "сфера у %at радиус %radius цвет %color сквозь блоки %through имя %id на %time"},
            {"gizmo text", "надпись у %at %text цвет %color размер %scale сквозь блоки %through имя %id на %time"},
            {"gizmo screen text", "надпись на экране x %x y %y %text цвет %color размер %scale имя %id на %time"},
            {"gizmo screen rect", "рамка на экране x %x y %y ширина %w высота %h цвет %color заливка %fill имя %id на %time"},
            {"gizmo screen line", "линия на экране от %x %y до %x2 %y2 цвет %color толщина %width имя %id на %time"},
            {"gizmo clear", "убрать гизмо %id"},
            {"navigate to", "навигатор к %to подпись %title цвет %color дойти на %radius имя %id"},
            {"navigate zone", "навигатор к зоне %name подпись %title цвет %color имя %id"},
            {"navigate stop", "убрать навигатор %id"},
            {"give", "выдать %item × %count"},
            {"recipe shaped", "рецепт верстака %id даёт %result × %count : %a1 %a2 %a3 | %b1 %b2 %b3 | %c1 %c2 %c3"},
            {"recipe shapeless", "рецепт без формы %id даёт %result × %count из %a1 %a2 %a3 | %b1 %b2 %b3 | %c1 %c2 %c3"},
            {"recipe smelting", "рецепт печи %id : %input → %result за %time опыт %xp"},
            {"recipe blasting", "рецепт плавильной печи %id : %input → %result за %time опыт %xp"},
            {"recipe smoking", "рецепт коптильни %id : %input → %result за %time опыт %xp"},
            {"recipe campfire", "рецепт костра %id : %input → %result за %time опыт %xp"},
            {"recipe stonecutting", "рецепт камнереза %id : %input → %result × %count"},
            {"recipe disable", "отключить рецепт %id"},
            {"recipe enable", "включить рецепт %id"},
            {"recipe reset", "сбросить рецепт %id"},
            {"recipe learn", "открыть игроку рецепт %id"},
            {"recipe forget", "закрыть игроку рецепт %id"},
            {"item cooldown", "перезарядка %item на %time"},
            {"item repair", "починить предмет в руке на %amount"},
            {"item damage", "износить предмет в руке на %amount"},
            {"item rename", "назвать предмет в руке %name"},
            {"item enchant", "зачаровать предмет в руке: %enchant уровень %level"},
            {"command", "команда / %command"},
            {"broadcast", "написать всему серверу %text"},
            {"setowner", "главным игроком сюжета становится %player"},
            {"pvar set", "переменная игрока %name = %value у %player"},
            {"pvar add", "переменная игрока %name + %value у %player"},
            {"pvar clear", "удалить переменную игрока %name у %player"},
            {"team join", "добавить в команду %name игрока %player"},
            {"team leave", "вывести из команды игрока %player"},
            {"team color", "цвет команды %name = %color"},
            {"team pvp", "команда %name : запретить бой между своими %off"},
            {"tag add", "дать тег %name игроку %player"},
            {"tag remove", "снять тег %name с игрока %player"},
            {"blackout", "затемнение %time"},
            {"flash", "вспышка %time"},
            {"screen", "цвет экрана R %r G %g B %b A %a"},
            {"eyes", "закрыть глаза %close держать %hold открыть %open"},
            {"flag set", "поставить флаг %name значение %value"},
            {"flag clear", "снять флаг %name"},
            {"checkpoint", "точка сохранения %name"},
            {"restore", "вернуться к сохранению %name"},
            {"ensure active", "запустить сюжеты %ids"},
            {"ensure inactive", "остановить сюжеты %ids"},
            {"log", "лог %text"},
            {"emit", "послать событие %name значение %value"},
            {"summon", "призвать %type у %pos в переменную %var"},
            {"skin", "скин %who = %skin"},
            {"namecolor", "цвет имени %who = %color"},
            {"hitbox", "хитбокс %who: %modes"},
            {"cast", "снаряд %name из %from в %to скорость %speed урон %damage дальность %range"},
            {"shockwave", "ударная волна у %pos радиус %radius сила %power урон %damage"},
            {
                    "aoe",
                    "по области у %pos радиус %radius сектор %cone урон %damage оттолкнуть %push притянуть %pull поджечь %fire эффект %effect на %time сила %level лечить %heal"
            },
            {"motion add", "движение %who: прибавить %vec"},
            {"motion set", "движение %who: сделать %vec"},
            {"motion jump", "подбросить %who с силой %power"},
            {"motion forward", "толкнуть %who вперёд с силой %power вверх %up"},
            {"motion toward", "толкнуть %who к %to с силой %power вверх %up"},
            {"motion stop", "остановить движение %who"},
            {"ignite", "поджечь %who на %time"},
            {"breakblock", "сломать блок %pos инструментом игрока"},
            {"explode", "взрыв у %pos сила %power"},
            {"lightning", "молния в %pos"},
            {"bar set", "полоса %id = %value из %max"},
            {"bar hide", "убрать полосу %id"},
            {"actionbar", "надпись над хотбаром %text"},
            {"notify", "уведомление %text предмет %item картинка %icon без звука %silent"},
            {"js", "JS-код %code"},
            {"setnbt", "NBT %target путь %path = %value"},
            {"setattr", "атрибут %attr у %target = %value"},
            {"setscore", "очки %objective у %target = %value прибавить %add"},
            {"walk", "%who идёт по пути %path скорость %speed с точки %from по кругу %loop туда-обратно %pingpong не ждать %nowait"},
            {"stopwalk", "%who перестаёт идти по пути"},
            {"shop open", "открыть магазин %name продавец %who"},
            {"shop restock", "пополнить магазин %name"},
            {"shop stock", "в магазине %name товара %offer осталось %value"},
            {"shop close", "закрыть магазин"},
            {"fight", "%who дерётся: стиль %style, цель %target"},
            {"skills open", "открыть прокачку %tree меню %menu"},
            {"skills level", "прокачка %tree: поставить уровень %amount"},
            {"skills close", "закрыть прокачку"},
            {"skills xp", "прокачка %tree: дать опыта %amount"},
            {"skills points", "прокачка %tree: дать очков %amount вида %currency"},
            {"skills learn", "прокачка %tree: дать навык %node"},
            {"skills forget", "прокачка %tree: забрать навык %node"},
            {"skills reset", "прокачка %tree: сбросить всё"},
            {"skills enable", "прокачка %tree: включить"},
            {"skills disable", "прокачка %tree: выключить"},
            {"stealth start", "начать стелс: вернуть в %back откатить %restore"},
            {
                    "stealth guard",
                    "%who страж: стиль %style патруль %patrol скорость %speed форма %shape длина %range угол %angle ширина %width высота %height крадущимся × %sneak замечает за %notice цвет %color сквозь стены %through туда-обратно %pingpong"
            },
            {"stealth remove", "%who больше не страж"},
            {"stealth stop", "закончить стелс"},
            {"fight stop", "%who прекращает бой"},
            {"fight heal", "вылечить в бою %who на %value"},
            {"wait defeated", "ждать, пока %who будет повержен"},
            {"zone on", "включить зону %name"},
            {"zone off", "выключить зону %name"},
            {"zone wall", "стена зоны %name: %mode"},
            {"wait zone", "ждать, пока игрок войдёт в зону %name"},
            {"wait lookat", "ждать, пока игрок посмотрит на %target"},
            {"wait outside", "ждать, пока игрок выйдет из зоны %name"},
            {"quest start", "выдать квест %name"},
            {"quest progress", "квест %name: задача %task + %value"},
            {"wait goal", "ждать, пока в квесте %name сделана цель %task"},
            {"quest set", "квест %name: задача %task = %value"},
            {"quest reveal", "квест %name: открыть задачу %task"},
            {"quest done", "квест %name выполнен"},
            {"quest fail", "квест %name провален"},
            {"quest reset", "забыть квест %name"},
            {"quest track", "отслеживать квест %name"},
            {"quest journal", "открыть журнал квестов"},
            {"time set", "время суток = %value"},
            {"time add", "сдвинуть время на %value"},
            {"time speed", "скорость дня %value"},
            {"time freeze", "остановить время на %value"},
            {"time unfreeze", "время снова идёт"},
            {"setblock", "поставить блок %block в %pos"},
            {"setprop", "у блока в %pos свойство %prop = %value"},
            {
                    "attack",
                    "%who бьёт хитбоксом: часть %part размер %w %h %d сдвиг вправо %right вверх %up вперёд %forward через %delay длится %time урон %damage отброс %push эффект %impact кроме %except показать %show не ждать %nowait"
            },
            {"part", "часть %name у %who включена %enabled урон по ней × %damage бьёт касанием %attack"}
        };

        for (String[] e : t) {
            TEMPLATES.put(e[0], e[1]);
        }

        for (EventDefs.Def d : EventDefs.ALL.values()) {
            TEMPLATES.put("wait " + d.name(), "ждать: " + d.title() + (d.filter() != null ? " %what" : ""));
        }

        special(
            "clip_start",
            "Поток",
            BlockTypes.Shape.HAT,
            "в этот момент",
            List.of(),
            "Блоки под этим выполняются, когда камера доходит до этого места таймлайна."
        );
        HIDDEN.add("clip_start");
        special(
            "start",
            "Поток",
            BlockTypes.Shape.HAT,
            "когда начинается сюжет %id %title автозапуск %auto показывать %solo",
            List.of(),
            "Начало сюжета: id и название. Сюжет — это стопка блоков под этой шапкой.\nПоказывать: «всем игрокам» — один сюжет на всех, его видят все игроки; «только игроку» — у каждого игрока своя копия: своё продвижение и свои персонажи, которых видит только он.\nАвтозапуск: «при входе» — сюжет сам начинается, когда игрок заходит в мир; «один раз» — только при первом входе.",
            f("id", "id", ParamType.SCRIPT, false, "5001"),
            f("title", "название", ParamType.TEXT, true, "Новый сюжет"),
            f("auto", "нет", ParamType.CHOICE, true, "нет"),
            f("solo", "всем игрокам", ParamType.CHOICE, true, "всем игрокам")
        );
        FIELD_CHOICES.put("start/solo", List.of("всем игрокам", "только игроку"));
        Map<String, String> autoModes = new LinkedHashMap<>();

        for (String m : List.of("нет", "при входе", "один раз")) {
            autoModes.put(m, "%id %title %auto %solo");
        }

        autoModes.put("общий: с запуска игры", "%id %title %auto");
        VARIANTS.put("start", new BlockTypes.Variant("auto", autoModes));
        special("label", "Поток", BlockTypes.Shape.COMMAND, "метка %name", List.of(), "Метка, к которой можно перейти.", f("name", "имя", null, false, "start"));
        special("goto", "Поток", BlockTypes.Shape.CAP, "перейти к %label", List.of(), "Переход к метке.", f("label", "метка", ParamType.LABEL, false, ""));
        special(
            "if",
            "Поток",
            BlockTypes.Shape.C,
            "если %cond то",
            List.of(""),
            "Выполнить блоки, если условие истинно.",
            f("cond", "условие", ParamType.EXPR, false, "")
        );
        special(
            "ifelse",
            "Поток",
            BlockTypes.Shape.C,
            "если %cond то",
            List.of("", "иначе"),
            "Если условие истинно — первая ветка, иначе вторая.",
            f("cond", "условие", ParamType.EXPR, false, "")
        );
        special(
            "while",
            "Поток",
            BlockTypes.Shape.C,
            "пока %cond повторять",
            List.of(""),
            "Повторять, пока условие истинно.",
            f("cond", "условие", ParamType.EXPR, false, "")
        );
        special(
            "repeat", "Поток", BlockTypes.Shape.C, "повторить %count раз", List.of(""), "Повторить блоки N раз.", f("count", "раз", ParamType.EXPR, false, "3")
        );
        special(
            "choice",
            "Поток",
            BlockTypes.Shape.C,
            "%who спрашивает %text эмоция %emotion",
            List.of(),
            "Диалог с вариантами ответа, до четырёх. У каждого варианта своя ветка; «+» добавляет следующий, ПКМ — убрать последний.",
            f("who", "кто", ParamType.CHAR, false, ""),
            f("text", "вопрос", ParamType.TEXT, false, "Пойдёшь со мной?"),
            f("emotion", "эмоция", ParamType.EMOTION, true, "")
        );
        special(
            "parallel",
            "Поток",
            BlockTypes.Shape.C,
            "параллельно %name",
            List.of(""),
            "Ветка идёт одновременно с основной.",
            f("name", "имя", null, false, "ambient")
        );
        special(
            "set",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "установить %var = %value",
            List.of(),
            "Записывает значение в переменную.",
            f("var", "переменная", ParamType.VAR, false, "count"),
            f("value", "значение", ParamType.EXPR, false, "0")
        );
        special(
            "switch",
            "Поток",
            BlockTypes.Shape.C,
            "выбор по %value",
            List.of(),
            "Сравнивает значение с вариантами и выполняет подходящую ветку; последняя — «иначе». ПКМ — добавить/убрать вариант.\nНапример: «выбор по» + «нажатая кнопка», варианты \"yes\" и \"no\".",
            f("value", "значение", ParamType.EXPR, false, "button()")
        );
        special("stop", "Поток", BlockTypes.Shape.CAP, "конец сюжета", List.of(), "Завершает сюжет.");
        special(
            "for",
            "Поток",
            BlockTypes.Shape.C,
            "для каждого %var из %list",
            List.of(""),
            "Перебор: блоки внутри выполняются для каждого элемента списка (ключа словаря, числа 0..N-1). Например, «из» = enemies или range(1, 11).",
            f("var", "имя", null, false, "x"),
            f("list", "список", ParamType.EXPR, false, "range(10)")
        );
        special(
            "as",
            "Поток",
            BlockTypes.Shape.C,
            "для игрока %who",
            List.of(""),
            "Блоки внутри относятся к этому игроку: всё показанное (речь, GUI, камера, титры) видит только он. Например, «игрок события» в обработчике или переменная цикла по «всем игрокам».",
            f("who", "игрок", ParamType.EXPR, false, "actor")
        );
        special(
            "asall",
            "Поток",
            BlockTypes.Shape.C,
            "для %who по очереди",
            List.of(""),
            "Блоки внутри выполняются отдельно для каждого: выдать квест, предмет или монеты сразу всем. Внутри «игрок» — очередной игрок, и показанное видит только он.\nИдёт по очереди: реплика с ожиданием F8 задержит следующих. Чтобы у каждого свой ход сцены, выберите на старте сюжета «только игроку».\nВ коде: for p in allplayers() { as p { ... } }.",
            f("who", "", ParamType.CHOICE, false, "всех игроков")
        );
        FIELD_CHOICES.put("asall/who", List.of("всех игроков", "игроков сюжета", "остальных игроков"));
        special(
            "every",
            "Поток",
            BlockTypes.Shape.C,
            "каждые %time в фоне",
            List.of(""),
            "Повторяет блоки в фоне с этим интервалом, пока идёт сюжет (1t — каждый тик, 20t — раз в секунду).",
            f("time", "время", ParamType.DURATION, false, "20t")
        );
        special("break", "Поток", BlockTypes.Shape.COMMAND, "прервать цикл", List.of(), "Выходит из цикла (для каждого / пока / повторить).");
        special("continue", "Поток", BlockTypes.Shape.COMMAND, "к следующему шагу цикла", List.of(), "Сразу переходит к следующему шагу цикла.");
        special(
            "propset",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "%who . %prop = %value",
            List.of(),
            "Меняет свойство сущности (player, персонаж, переменная с сущностью) или словаря: health, name, pos, target, glowing…",
            f("who", "кто", null, false, "player"),
            f("prop", "свойство", null, false, "health"),
            f("value", "значение", ParamType.EXPR, false, "20")
        );
        special(
            "method",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "%who . %method ( %args )",
            List.of(),
            "Вызывает метод: у сущности — kill, tp, damage, heal, effect, give…; у списка — add, remove, sort…",
            f("who", "кто", null, false, "player"),
            f("method", "метод", null, false, "heal"),
            f("args", "аргументы", ParamType.EXPR, true, "4")
        );
        special(
            "listadd",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "добавить %value в список %var",
            List.of(),
            "Добавляет значение в конец списка.",
            f("value", "значение", ParamType.EXPR, false, "1"),
            f("var", "список", ParamType.VAR, false, "items")
        );
        special(
            "listremove",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "убрать %value из списка %var",
            List.of(),
            "Убирает первое такое значение из списка.",
            f("value", "значение", ParamType.EXPR, false, "1"),
            f("var", "список", ParamType.VAR, false, "items")
        );
        special(
            "listclear",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "очистить список %var",
            List.of(),
            "Убирает из списка всё.",
            f("var", "список", ParamType.VAR, false, "items")
        );
        special(
            "setat",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "%var [ %index ] = %value",
            List.of(),
            "Записывает значение в элемент списка (по номеру с 0) или словаря (по ключу).",
            f("var", "список", ParamType.VAR, false, "items"),
            f("index", "номер или ключ", ParamType.EXPR, false, "0"),
            f("value", "значение", ParamType.EXPR, false, "1")
        );
        special(
            "func",
            "Функции",
            BlockTypes.Shape.HAT,
            "функция %name ( %params )",
            List.of(),
            "Объявляет функцию: стопка блоков под этой шапкой выполняется при вызове. Параметры — через запятую, внутри это обычные переменные.",
            f("name", "имя", null, false, "greet"),
            f("params", "параметры", null, true, "")
        );
        special(
            "call",
            "Функции",
            BlockTypes.Shape.COMMAND,
            "вызвать %name ( %args )",
            List.of(),
            "Вызывает функцию и ждёт, пока она закончится. Аргументы — через запятую.",
            f("name", "функция", ParamType.FUNC, false, ""),
            f("args", "аргументы", ParamType.EXPR, true, "")
        );
        special(
            "asynccall",
            "Функции",
            BlockTypes.Shape.COMMAND,
            "в фоне вызвать %name ( %args ) как %as",
            List.of(),
            "Запускает функцию асинхронно: сюжет сразу идёт дальше. Имя («как») нужно, чтобы потом дождаться или отменить.",
            f("name", "функция", ParamType.FUNC, false, ""),
            f("args", "аргументы", ParamType.EXPR, true, ""),
            f("as", "имя ветки", null, true, "")
        );
        special(
            "async",
            "Функции",
            BlockTypes.Shape.C,
            "в фоне %name",
            List.of(""),
            "Асинхронная ветка: блоки внутри выполняются одновременно с основным сюжетом.",
            f("name", "имя ветки", null, true, "")
        );
        special(
            "await",
            "Функции",
            BlockTypes.Shape.COMMAND,
            "дождаться %name",
            List.of(),
            "Ждёт, пока асинхронная ветка закончится.",
            f("name", "ветка", ParamType.BRANCH, false, "")
        );
        special(
            "cancel",
            "Функции",
            BlockTypes.Shape.COMMAND,
            "отменить %name",
            List.of(),
            "Останавливает асинхронную ветку.",
            f("name", "ветка", ParamType.BRANCH, false, "")
        );
        special("return", "Функции", BlockTypes.Shape.CAP, "выйти из функции", List.of(), "Досрочно завершает функцию.");
        special("code", "Поток", BlockTypes.Shape.COMMAND, "код %code", List.of(), "Произвольная строка кода SST.", f("code", "код", ParamType.EXPR, false, ""));
        special(
            "character",
            "Объявления",
            BlockTypes.Shape.COMMAND,
            "персонаж %name = %type имя %display цвет имени %color хитбокс %hitbox скорость %speed иконка %icon лицо %portrait голова в чате: кость %headbone размер %headscale сдвиг X %headx Y %heady поворот %headyaw",
            List.of(),
            "Объявляет персонажа. Иконка — картинка в окне речи (без неё в чате видна голова модели), лицо — в диалоге с выбором.\nГолова в чате: размер (1 — обычный, 1.5 — крупнее), сдвиг в процентах рамки, поворот в градусах (0 — смотрит прямо).",
            f("name", "имя в коде", null, false, "alice"),
            f("type", "сущность", ParamType.ENTITY, false, "spraute:npc"),
            f("display", "имя", ParamType.TEXT, true, "Алиса"),
            f("color", "обычный", ParamType.TEXT, true, ""),
            f("hitbox", "обычный", ParamType.TEXT, true, ""),
            f("speed", "скорость", ParamType.NUMBER, true, ""),
            f("icon", "голова", ParamType.IMAGE, true, ""),
            f("portrait", "нет", ParamType.IMAGE, true, ""),
            f("headscale", "1", ParamType.NUMBER, true, ""),
            f("headx", "0", ParamType.NUMBER, true, ""),
            f("heady", "0", ParamType.NUMBER, true, ""),
            f("headyaw", "0", ParamType.NUMBER, true, ""),
            f("headbone", "авто", ParamType.TEXT, true, "")
        );
        special(
            "location",
            "Объявления",
            BlockTypes.Shape.COMMAND,
            "точка %name = %pos поворот %yaw",
            List.of(),
            "Объявляет точку в мире.",
            f("name", "имя", null, false, "point"),
            f("pos", "координаты", ParamType.LOC, false, "(0, 64, 0)"),
            f("yaw", "поворот", ParamType.NUMBER, true, "")
        );
        special(
            "var",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "переменная %name = %value",
            List.of(),
            "Объявляет переменную сюжета (начальное значение).",
            f("name", "имя", null, false, "count"),
            f("value", "значение", ParamType.EXPR, false, "0")
        );
        special("comment", "Объявления", BlockTypes.Shape.COMMAND, "// %text", List.of(), "Комментарий.", f("text", "текст", ParamType.TEXT, true, "заметка"));

        for (CommandSpec spec : Specs.commands()) {
            commandBlock(spec);
        }

        rep("varget", "Переменные", BlockTypes.Shape.REPORTER, "%name", "%name", "Значение переменной.", f("name", "переменная", ParamType.VAR, false, "count"));
        special(
            "change",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "изменить %var на %value",
            List.of(),
            "Прибавляет к переменной число (или дописывает текст).",
            f("var", "переменная", ParamType.VAR, false, "count"),
            f("value", "1", ParamType.EXPR, false, "1")
        );
        String[][] ops = new String[][]{
            {"op:add", "%a + %b", "(%a + %b)"},
            {"op:sub", "%a − %b", "(%a - %b)"},
            {"op:mul", "%a × %b", "(%a * %b)"},
            {"op:div", "%a ÷ %b", "(%a / %b)"},
            {"op:mod", "остаток %a ÷ %b", "(%a % %b)"}
        };

        for (String[] o : ops) {
            rep(
                o[0],
                "Операторы",
                BlockTypes.Shape.REPORTER,
                o[1],
                o[2],
                "Арифметика.",
                f("a", "", ParamType.NUMBER, false, ""),
                f("b", "", ParamType.NUMBER, false, "")
            );
        }

        String[][] cmp = new String[][]{
            {"op:eq", "%a = %b", "(%a == %b)"},
            {"op:ne", "%a ≠ %b", "(%a != %b)"},
            {"op:lt", "%a < %b", "(%a < %b)"},
            {"op:gt", "%a > %b", "(%a > %b)"},
            {"op:le", "%a ≤ %b", "(%a <= %b)"},
            {"op:ge", "%a ≥ %b", "(%a >= %b)"}
        };

        for (String[] o : cmp) {
            rep(
                o[0],
                "Операторы",
                BlockTypes.Shape.BOOLEAN,
                o[1],
                o[2],
                "Сравнение: числа сравниваются как числа, остальное — как текст.",
                f("a", "", ParamType.TEXT, false, ""),
                f("b", "", ParamType.TEXT, false, "")
            );
        }

        rep(
            "op:and",
            "Операторы",
            BlockTypes.Shape.BOOLEAN,
            "%a и %b",
            "(%a and %b)",
            "Истина, если верно и то и другое.",
            f("a", "", ParamType.BOOL, false, ""),
            f("b", "", ParamType.BOOL, false, "")
        );
        rep(
            "op:or",
            "Операторы",
            BlockTypes.Shape.BOOLEAN,
            "%a или %b",
            "(%a or %b)",
            "Истина, если верно хотя бы одно.",
            f("a", "", ParamType.BOOL, false, ""),
            f("b", "", ParamType.BOOL, false, "")
        );
        rep("op:not", "Операторы", BlockTypes.Shape.BOOLEAN, "не %a", "(not %a)", "Отрицание.", f("a", "", ParamType.BOOL, false, ""));
        rep(
            "op:join",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "соединить %a %b",
            "(str(%a) + str(%b))",
            "Склеивает два значения в текст.",
            f("a", "", ParamType.TEXT, false, "привет "),
            f("b", "", ParamType.TEXT, false, "мир")
        );
        rep(
            "op:random",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "случайное от %a до %b",
            "randint(%a, %b)",
            "Случайное целое число в диапазоне.",
            f("a", "", ParamType.NUMBER, false, "1"),
            f("b", "", ParamType.NUMBER, false, "10")
        );
        rep("op:round", "Операторы", BlockTypes.Shape.REPORTER, "округлить %a", "round(%a)", "Округляет до целого.", f("a", "", ParamType.NUMBER, false, ""));
        rep(
            "val:actor",
            "Значения",
            BlockTypes.Shape.REPORTER,
            "игрок события",
            "actor",
            "Игрок, вызвавший событие, которое сейчас обрабатывается. Вне события — главный игрок сюжета."
        );
        rep(
            "op:text",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "текст %a",
            "%a",
            "Просто текст — для подстановки в гнездо.",
            f("a", "", ParamType.TEXT, false, "текст")
        );
        rep("op:number", "Операторы", BlockTypes.Shape.REPORTER, "число %a", "%a", "Просто число.", f("a", "", ParamType.NUMBER, false, "0"));
        rep(
            "op:expr",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "выражение %a",
            "(%a)",
            "Любое выражение языка: hp * 2 + 1",
            f("a", "", ParamType.EXPR, false, "")
        );
        rep("op:true", "Операторы", BlockTypes.Shape.BOOLEAN, "да", "true", "Истина.");
        rep("op:false", "Операторы", BlockTypes.Shape.BOOLEAN, "нет", "false", "Ложь.");

        for (Specs.FunctionSpec fs : Specs.functions()) {
            functionBlock(fs);
        }

        List<String> modes = new ArrayList<>();

        for (Entry<String, String> fe : EventDefs.FIELDS.entrySet()) {
            modes.add(fe.getValue());
            modes.add("event(\"" + fe.getKey() + "\")");
        }

        moded(
            "ev:data",
            "События",
            BlockTypes.Shape.REPORTER,
            "событие: %field",
            "Данные события, которое сейчас обрабатывается (внутри блока «когда ...» или после «ждать: ...»).",
            modes.toArray(new String[0]),
            f("field", "", ParamType.CHOICE, false, EventDefs.FIELDS.values().iterator().next())
        );
        moded(
            "ent:prop",
            "Значения",
            BlockTypes.Shape.REPORTER,
            "%who : %prop",
            "Свойство сущности: player, персонаж или переменная с сущностью (то, что меняют команды tp, health, name, hold…).",
            new String[]{
                "X",
                "%who.x",
                "Y",
                "%who.y",
                "Z",
                "%who.z",
                "позиция",
                "%who.pos",
                "здоровье",
                "%who.health",
                "макс. здоровье",
                "%who.maxhealth",
                "имя",
                "%who.name",
                "тип",
                "%who.type",
                "предмет в руке",
                "%who.item",
                "поворот (yaw)",
                "%who.yaw",
                "наклон (pitch)",
                "%who.pitch",
                "направление взгляда",
                "%who.look",
                "глаза",
                "%who.eyes",
                "скорость",
                "%who.speed",
                "до игрока",
                "%who.dist",
                "цель",
                "%who.target",
                "уровень",
                "%who.level",
                "сытость",
                "%who.food",
                "измерение",
                "%who.dimension"
            },
            f("who", "кто", null, false, "player"),
            f("prop", "", ParamType.CHOICE, false, "X")
        );
        moded(
            "ent:is",
            "Значения",
            BlockTypes.Shape.BOOLEAN,
            "%who %prop ?",
            "Состояние сущности: player, персонаж или переменная с сущностью.",
            new String[]{
                "жив",
                "alive(%who)",
                "на земле",
                "%who.onground",
                "в воздухе",
                "%who.inair",
                "крадётся",
                "%who.sneaking",
                "бежит",
                "%who.sprinting",
                "летает",
                "%who.flying",
                "горит",
                "%who.fire",
                "светится",
                "%who.glowing",
                "невидим",
                "%who.invisible",
                "идёт по пути",
                "walking(%who)",
                "двигается",
                "moving(%who)",
                "в бою",
                "fighting(%who)"
            },
            f("who", "кто", null, false, "player"),
            f("prop", "", ParamType.CHOICE, false, "жив")
        );
        moded(
            "op:math",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "%a %op %b",
            "Арифметика: выберите действие в середине блока.",
            new String[]{"+", "(%a + %b)", "−", "(%a - %b)", "×", "(%a * %b)", "÷", "(%a / %b)", "остаток", "(%a % %b)"},
            f("a", "", ParamType.NUMBER, false, ""),
            f("op", "", ParamType.CHOICE, false, "+"),
            f("b", "", ParamType.NUMBER, false, "")
        );
        moded(
            "op:compare",
            "Операторы",
            BlockTypes.Shape.BOOLEAN,
            "%a %op %b",
            "Сравнение. Числа сравниваются как числа, остальное — как текст.",
            new String[]{"=", "(%a == %b)", "≠", "(%a != %b)", "<", "(%a < %b)", ">", "(%a > %b)", "≤", "(%a <= %b)", "≥", "(%a >= %b)"},
            f("a", "", ParamType.TEXT, false, ""),
            f("op", "", ParamType.CHOICE, false, "="),
            f("b", "", ParamType.TEXT, false, "")
        );
        moded(
            "op:logic",
            "Операторы",
            BlockTypes.Shape.BOOLEAN,
            "%a %op %b",
            "Логика: «и» — верно оба, «или» — хотя бы одно.",
            new String[]{"и", "(%a and %b)", "или", "(%a or %b)"},
            f("a", "", ParamType.BOOL, false, ""),
            f("op", "", ParamType.CHOICE, false, "и"),
            f("b", "", ParamType.BOOL, false, "")
        );
        moded(
            "op:mathfn",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "%op %a",
            "Округление и модуль числа.",
            new String[]{"округлить", "round(%a)", "округлить вниз", "floor(%a)", "модуль", "abs(%a)", "целая часть", "int(%a)"},
            f("op", "", ParamType.CHOICE, false, "округлить"),
            f("a", "", ParamType.NUMBER, false, "")
        );
        moded(
            "op:textfn",
            "Операторы",
            BlockTypes.Shape.REPORTER,
            "%op %a",
            "Работа с текстом.",
            new String[]{"длина", "len(%a)", "ЗАГЛАВНЫМИ", "upper(%a)", "строчными", "lower(%a)", "в число", "num(%a)", "в текст", "str(%a)"},
            f("op", "", ParamType.CHOICE, false, "длина"),
            f("a", "", ParamType.TEXT, false, "текст")
        );
        moded(
            "var:set",
            "Переменные",
            BlockTypes.Shape.COMMAND,
            "%var %op %value",
            "Изменяет переменную: установить, прибавить или отнять.",
            new String[]{"установить =", "set %var = %value", "увеличить на", "set %var += %value", "уменьшить на", "set %var -= %value"},
            f("var", "переменная", ParamType.VAR, false, "count"),
            f("op", "", ParamType.CHOICE, false, "установить ="),
            f("value", "значение", ParamType.EXPR, false, "1")
        );
        moded(
            "npc:spawn",
            "Персонажи",
            BlockTypes.Shape.COMMAND,
            "%who %mode %target размер %scale",
            "Ставит персонажа в мир (spawn): у цели — игрока, другого персонажа, точки или координат; «перед игроком» — в 3 блоках впереди, на земле. «Исчезает» убирает его из мира.",
            new String[]{
                "появляется у",
                "spawn %who at %target[ scale %scale]",
                "появляется перед игроком",
                "spawn %who at infront(player, 3)",
                "появляется рядом с игроком",
                "spawn %who at ground((player.x + 2, player.y, player.z))",
                "исчезает",
                "despawn %who"
            },
            f("who", "кто", ParamType.CHAR, false, "alice"),
            f("mode", "", ParamType.CHOICE, false, "появляется у"),
            f("target", "где", ParamType.TARGET, false, "player"),
            f("scale", "1", ParamType.NUMBER, true, "")
        );
        moded(
            "npc:look",
            "Персонажи",
            BlockTypes.Shape.COMMAND,
            "%who %mode %target",
            "Взгляд персонажа. Цель: игрок, другой персонаж, моб (выберите «моб…») или точка.",
            new String[]{"смотрит на", "look %who at %target", "всегда смотрит на", "look %who at %target always", "перестаёт смотреть", "stoplook %who"},
            f("who", "кто", ParamType.CHAR, false, "alice"),
            f("mode", "", ParamType.CHOICE, false, "смотрит на"),
            f("target", "цель", ParamType.TARGET, false, "player")
        );
        moded(
            "npc:go",
            "Персонажи",
            BlockTypes.Shape.COMMAND,
            "%who %mode %target скорость %speed",
            "Перемещение персонажа к цели: игрок, персонаж, моб или точка.",
            new String[]{
                "идёт к",
                "move %who to %target[ speed %speed]",
                "появляется у",
                "spawn %who at %target",
                "телепортируется к",
                "tp %who to %target",
                "следует за",
                "follow %who target %target[ speed %speed]"
            },
            f("who", "кто", ParamType.CHAR, false, "alice"),
            f("mode", "", ParamType.CHOICE, false, "идёт к"),
            f("target", "цель", ParamType.TARGET, false, "player"),
            f("speed", "скорость", ParamType.NUMBER, true, "")
        );
        moded(
            "npc:gesture",
            "Анимации",
            BlockTypes.Shape.COMMAND,
            "%who %mode",
            "Быстрые жесты и позы.",
            new String[]{
                "кивает",
                "nod %who",
                "качает головой",
                "nope %who",
                "смеётся",
                "laugh %who",
                "вздыхает",
                "sigh %who",
                "садится",
                "sit %who",
                "встаёт",
                "stand %who"
            },
            f("who", "кто", ParamType.CHAR, false, "alice"),
            f("mode", "", ParamType.CHOICE, false, "кивает")
        );
        moded(
            "screen:fx",
            "Экран",
            BlockTypes.Shape.COMMAND,
            "%mode %time",
            "Эффект экрана.",
            new String[]{"затемнение", "blackout %time", "вспышка", "flash %time"},
            f("mode", "", ParamType.CHOICE, false, "затемнение"),
            f("time", "время", ParamType.DURATION, false, "1s")
        );
        moded(
            "engine:toggle",
            "Игрок и мир",
            BlockTypes.Shape.COMMAND,
            "%mode %sys",
            "Включает или выключает систему движка прямо в сюжете. Сохраняется в мире.",
            new String[]{"включить", "enable %sys", "выключить", "disable %sys"},
            f("mode", "", ParamType.CHOICE, false, "включить"),
            f("sys", "система", ParamType.SYSTEM, false, "health")
        );
        moded(
            "engine:on",
            "Игрок и мир",
            BlockTypes.Shape.BOOLEAN,
            "%sys %mode",
            "Проверка, включена ли система движка.",
            new String[]{"включена?", "system(\"%sys\")", "выключена?", "(not system(\"%sys\"))"},
            f("sys", "система", ParamType.SYSTEM, false, "health"),
            f("mode", "", ParamType.CHOICE, false, "включена?")
        );
        moded(
            "screen:notify",
            "Речь",
            BlockTypes.Shape.COMMAND,
            "уведомление %text иконка: %mode %icon",
            "Уведомление Spraute слева вверху. Выберите, что будет иконкой — предмет или картинка (проекта или игры), — и укажите её.",
            new String[]{
                "предмет",
                "notify %text item %icon",
                "картинка",
                "notify %text icon %icon",
                "без иконки",
                "notify %text",
                "предмет, без звука",
                "notify %text item %icon silent",
                "картинка, без звука",
                "notify %text icon %icon silent"
            },
            f("text", "текст", ParamType.TEXT, false, "Получен ключ"),
            f("mode", "", ParamType.CHOICE, false, "предмет"),
            new BlockTypes.Field("icon", "иконка", ParamType.ITEM, true, "minecraft:tripwire_hook", true, false, null, 0, false)
        );
        moded(
            "player:tp",
            "Игрок и мир",
            BlockTypes.Shape.COMMAND,
            "телепортировать игрока к %target",
            "Телепорт игрока к цели: персонаж, моб или точка.",
            new String[]{"к цели", "teleport %target"},
            f("target", "цель", ParamType.TARGET, false, "(0, 64, 0)")
        );

        for (CommandSpec spec : Specs.commands()) {
            if (Specs.fullOnly(spec)) {
                HIDDEN.add("cmd:" + spec.key);
            }
        }

        HIDDEN.add("ev:mobspawn");
        HIDDEN.addAll(
            List.of(
                "op:add",
                "op:sub",
                "op:mul",
                "op:div",
                "op:mod",
                "op:eq",
                "op:ne",
                "op:lt",
                "op:gt",
                "op:le",
                "op:ge",
                "op:and",
                "op:or",
                "op:round",
                "set",
                "change",
                "cmd:look",
                "cmd:stoplook",
                "cmd:spawn",
                "cmd:tp",
                "cmd:move",
                "cmd:follow",
                "cmd:nod",
                "cmd:nope",
                "cmd:laugh",
                "cmd:sigh",
                "cmd:sit",
                "cmd:stand",
                "cmd:blackout",
                "cmd:flash",
                "cmd:teleport",
                "fn:round",
                "fn:floor",
                "fn:abs",
                "fn:int",
                "fn:len",
                "fn:upper",
                "fn:lower",
                "fn:num",
                "fn:str",
                "fn:randint",
                "cmd:enable",
                "cmd:disable",
                "fn:system",
                "cmd:notify"
            )
        );
        FIRST.addAll(
            List.of(
                "start",
                "ev:data",
                "on:interact",
                "on:break",
                "on:button",
                "on:key",
                "on:click",
                "var",
                "var:set",
                "varget",
                "op:math",
                "op:compare",
                "op:logic",
                "npc:spawn",
                "npc:go",
                "npc:look",
                "cmd:pickup",
                "cmd:wait npcpickup",
                "on:npcpickup",
                "cmd:wait goal",
                "on:goal",
                "cmd:anim",
                "npc:gesture",
                "screen:fx",
                "engine:toggle",
                "engine:on",
                "player:tp"
            )
        );
        rep(
            "gui:pressed",
            "Интерфейс",
            BlockTypes.Shape.BOOLEAN,
            "нажата кнопка %el ?",
            "(button() == %el)",
            "Истина, если последней нажата эта кнопка. Используйте после «ждать нажатия кнопки».",
            f("el", "кнопка", ParamType.GUIEL, false, "yes")
        );
    }

    public record Field(
        String name, String label, ParamType kind, boolean optional, String def, boolean quote, boolean vararg, String keyword, int group, boolean flag
    ) {
        public boolean acceptsValue() {
            if (!this.flag && this.kind != null) {
                return switch (this.kind) {
                    case CHAR, LABEL, FUNC, BRANCH, MOVETYPE, KEY, EMOTION, LAYER, TARGET, LOC, SPEAKER, VAR, CHOICE, SYSTEM, EASE -> false;
                    default -> true;
                };
            } else {
                return false;
            }
        }

        public boolean booleanSocket() {
            return this.kind == ParamType.BOOL || this.kind == ParamType.EXPR && (this.name.equals("cond") || this.name.equals("condition"));
        }
    }

    public record Part(String text, BlockTypes.Field field) {
    }

    public static enum Shape {
        HAT,
        COMMAND,
        C,
        CAP,
        REPORTER,
        BOOLEAN;
    }

    public record Type(
        String key,
        String category,
        BlockTypes.Shape shape,
        List<BlockTypes.Part> parts,
        List<BlockTypes.Field> fields,
        List<String> slotLabels,
        CommandSpec spec,
        String doc,
        String code
    ) {
        public boolean isValue() {
            return this.shape == BlockTypes.Shape.REPORTER || this.shape == BlockTypes.Shape.BOOLEAN;
        }

        public boolean isEvent() {
            return this.key.startsWith("on:");
        }

        public BlockTypes.Field field(String name) {
            for (BlockTypes.Field f : this.fields) {
                if (f.name.equals(name)) {
                    return f;
                }
            }

            return null;
        }

        public boolean isDeclaration() {
            return this.category.equals("Объявления") && !this.key.equals("comment");
        }
    }

    public record Variant(String field, Map<String, String> codes) {
        public String template(String mode) {
            String t = this.codes.get(mode);
            return t != null ? t : this.codes.values().iterator().next();
        }

        public List<String> modes() {
            return new ArrayList<>(this.codes.keySet());
        }
    }
}
