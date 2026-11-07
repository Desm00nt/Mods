package org.sprautestudio.spraute_engine.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.engine.EngineConfig;

public final class GuiSchema {
    private static final Map<String, GuiSchema.Type> TYPES = new LinkedHashMap<>();
    private static final List<GuiSchema.Prop> DOC_PROPS = new ArrayList<>();
    public static final List<String> ANCHORS = List.of("top_left", "top", "top_right", "left", "center", "right", "bottom_left", "bottom", "bottom_right");
    public static final List<String> APPEAR = GuiMotion.APPEAR;
    public static final List<String> HOVER_KEYS = List.of(
        "hoverScale",
        "hoverOpacity",
        "hoverRotation",
        "hoverRotX",
        "hoverRotY",
        "hoverX",
        "hoverY",
        "hoverTilt",
        "hoverGlow",
        "hoverSound",
        "pressScale",
        "hoverImage",
        "hoverTint",
        "hoverColor",
        "hoverBorderColor"
    );
    private static final List<String> ADDON_TYPES = new ArrayList<>();

    private GuiSchema() {
    }

    public static boolean reactsToMouse(Collection<String> set) {
        for (String key : HOVER_KEYS) {
            if (set.contains(key)) {
                return true;
            }
        }

        return false;
    }

    private static List<String> eases() {
        List<String> out = new ArrayList<>(List.of("auto"));
        out.addAll(Ease.names());
        return out;
    }

    private static GuiSchema.B common(int w, int h) {
        return common(w, h, "center");
    }

    private static GuiSchema.B common(int w, int h, String anchor) {
        return new GuiSchema.B()
            .group("Положение")
            .c("anchor", "привязка", anchor, ANCHORS, "Точка экрана, от которой отсчитываются X и Y (и сторона элемента, которой он к ней прижат)")
            .p("x", "X", GuiSchema.Kind.INT, "0", "Сдвиг по горизонтали от точки привязки. Можно в процентах ширины родителя: 10%")
            .p("y", "Y", GuiSchema.Kind.INT, "0", "Сдвиг по вертикали от точки привязки. Можно в процентах: -5%")
            .p("w", "ширина", GuiSchema.Kind.INT, String.valueOf(w), "Ширина в пикселях макета или в процентах родителя: 50%")
            .p("h", "высота", GuiSchema.Kind.INT, String.valueOf(h), "Высота в пикселях макета или в процентах родителя: 100%")
            .c(
                "stretch",
                "до краёв экрана",
                "none",
                List.of("none", "width", "height", "both"),
                "width — на всю ширину экрана, height — на всю высоту, both — на весь экран. Тянется и за рамку макета, когда экран шире или выше него"
            )
            .p("rotation", "поворот°", GuiSchema.Kind.NUMBER, "0", "Поворот в плоскости экрана, в градусах")
            .group("Вид")
            .p("visible", "видимый", GuiSchema.Kind.BOOL, "true", "Скрытые элементы можно показать командой gui show")
            .p("opacity", "непрозрачность", GuiSchema.Kind.NUMBER, "1", "0 — полностью прозрачный, 1 — непрозрачный")
            .group("Текстура и 9-slice")
            .p("backgroundImage", "картинка фона", GuiSchema.Kind.IMAGE, "", "Фон элемента; для кнопки заменяет обычную заливку")
            .p("nineSlice", "9-slice", GuiSchema.Kind.BOOL, "false", "Сохраняет углы и края картинки при изменении размера")
            .p("sliceLeft", "край слева", GuiSchema.Kind.INT, "4", "Пиксели исходной текстуры")
            .p("sliceTop", "край сверху", GuiSchema.Kind.INT, "4", "")
            .p("sliceRight", "край справа", GuiSchema.Kind.INT, "4", "")
            .p("sliceBottom", "край снизу", GuiSchema.Kind.INT, "4", "")
            .p("sliceScale", "масштаб краёв", GuiSchema.Kind.NUMBER, "1", "")
            .p("sliceTileCenter", "тайлинг внутри", GuiSchema.Kind.BOOL, "false", "Повторять середину 9-slice вместо растягивания")
            .p("sliceTileEdges", "тайлинг краёв", GuiSchema.Kind.BOOL, "false", "Повторять края 9-slice вместо растягивания")
            .p("itemSize", "размер предметов", GuiSchema.Kind.INT, "0", "0 — по размеру элемента или ячейки; иначе сторона иконки в пикселях")
            .group("Вид")
            .p("tooltip", "подсказка", GuiSchema.Kind.TEXT, "", "Текст при наведении мыши")
            .p("glow", "свечение", GuiSchema.Kind.INT, "0", "Размер мягкого свечения вокруг элемента, 0 — нет")
            .p("glowColor", "цвет свечения", GuiSchema.Kind.COLOR, "#FF8B6CFF", "")
            .group("3D-поворот")
            .p("rotX", "наклон вперёд°", GuiSchema.Kind.NUMBER, "0", "Поворот вокруг горизонтальной оси, с перспективой")
            .p("rotY", "разворот°", GuiSchema.Kind.NUMBER, "0", "Поворот вокруг вертикальной оси, с перспективой")
            .p("perspective", "перспектива", GuiSchema.Kind.NUMBER, "400", "Чем меньше, тем сильнее ближний край больше дальнего")
            .p("pivotX", "центр X", GuiSchema.Kind.NUMBER, "0.5", "Вокруг какой точки поворачивается и растёт: 0 — левый край, 1 — правый")
            .p("pivotY", "центр Y", GuiSchema.Kind.NUMBER, "0.5", "0 — верхний край, 1 — нижний")
            .group("Наведение")
            .p("hoverTime", "плавность, с", GuiSchema.Kind.NUMBER, "0.2", "За сколько секунд элемент переходит в вид при наведении и обратно")
            .p("hoverScale", "масштаб", GuiSchema.Kind.NUMBER, "1", "Например 1.05 — элемент чуть увеличивается под мышью")
            .p("hoverOpacity", "непрозрачность", GuiSchema.Kind.NUMBER, "1", "Во сколько раз меняется непрозрачность под мышью: 0.7 — бледнее")
            .p("hoverX", "сдвиг X", GuiSchema.Kind.NUMBER, "0", "На сколько пикселей уезжает под мышью")
            .p("hoverY", "сдвиг Y", GuiSchema.Kind.NUMBER, "0", "Например -3 — приподнимается")
            .p("hoverRotation", "поворот°", GuiSchema.Kind.NUMBER, "0", "Сколько добавить к повороту под мышью")
            .p("hoverRotX", "наклон вперёд°", GuiSchema.Kind.NUMBER, "0", "3D-поворот под мышью")
            .p("hoverRotY", "разворот°", GuiSchema.Kind.NUMBER, "0", "3D-поворот под мышью: 180 — карточка переворачивается")
            .p("hoverTilt", "наклон к мыши°", GuiSchema.Kind.NUMBER, "0", "Элемент наклоняется вслед за мышью, как карточка: 8–15 смотрится хорошо")
            .p("hoverGlow", "свечение", GuiSchema.Kind.INT, "0", "Размер свечения под мышью")
            .p("hoverSound", "звук", GuiSchema.Kind.SOUND, "", "Звук, когда мышь наводится на элемент")
            .p("pressScale", "масштаб при нажатии", GuiSchema.Kind.NUMBER, "1", "Кнопка: например 0.95 — чуть вдавливается")
            .p(
                "hoverShow",
                "показать менюшку",
                GuiSchema.Kind.ELEMENT,
                "",
                "Другой элемент или группа: появляется, пока мышь на этом элементе или на ней самой. Сама по себе она скрыта"
            )
            .p("hoverFollow", "менюшка у мыши", GuiSchema.Kind.BOOL, "false", "Менюшка ходит за мышью, а не стоит на своём месте")
            .group("Параллакс")
            .p(
                "parallax",
                "сдвиг за мышью X",
                GuiSchema.Kind.NUMBER,
                "0",
                "На сколько пикселей он уходит вслед за мышью. Несколько картинок с разной силой дают глубину: дальние 2, ближние 12; минус — в обратную сторону"
            )
            .p("parallaxY", "сдвиг за мышью Y", GuiSchema.Kind.NUMBER, "0", "То же по вертикали")
            .group("Анимация")
            .c(
                "loop",
                "постоянная анимация",
                "none",
                GuiMotion.LOOPS,
                "float — парит, pulse — дышит размером, spin — вращается, sway — покачивается, shake — дрожит, blink — мигает, breathe — плавно тускнеет, swing_3d — качается в 3D, turn_3d — вращается в 3D"
            )
            .p("loopSpeed", "оборотов в секунду", GuiSchema.Kind.NUMBER, "0.5", "")
            .p("loopPower", "сила", GuiSchema.Kind.NUMBER, "1", "Во сколько раз сильнее обычного; для spin — множитель угла")
            .group("Появление")
            .c("appear", "анимация", "none", APPEAR, "Как элемент появляется при открытии интерфейса")
            .c("appearEase", "плавность", "auto", eases(), "auto — своя у каждой анимации")
            .p("appearDelay", "задержка, с", GuiSchema.Kind.NUMBER, "0", "Через сколько секунд после открытия начинается появление")
            .p("appearTime", "длительность, с", GuiSchema.Kind.NUMBER, "0.3", "Длительность анимации появления");
    }

    public static boolean engineOnly(String type) {
        return type != null
            && (
                type.startsWith("hud_")
                    || type.startsWith("journal_")
                    || type.startsWith("shop_")
                    || type.startsWith("skill_")
                    || type.equals("choice_answers")
                    || type.equals("speaker")
                    || type.equals("quest_icon")
                    || type.equals("menu_widget")
                    || type.equals("speech_icon")
                    || type.equals("speech_plate")
                    || type.equals("chat_lines")
            );
    }

    public static List<GuiSchema.Type> uiTypes() {
        List<GuiSchema.Type> out = new ArrayList<>();

        for (GuiSchema.Type t : TYPES.values()) {
            if (!engineOnly(t.key())) {
                out.add(t);
            }
        }

        return out;
    }

    public static List<GuiSchema.Type> windowTypes(String role) {
        List<GuiSchema.Type> out = new ArrayList<>();
        boolean menu = role.startsWith("menu:");

        for (GuiSchema.Type t : TYPES.values()) {
            String k = t.key();
            if (!k.startsWith("hud_skill_")) {
                boolean var10000;
                if (menu) {
                    var10000 = k.equals("button") || k.equals("menu_slots");
                } else {
                    switch (role) {
                        case "hud":
                            var10000 = k.startsWith("hud_");
                            break;
                        case "choice":
                            var10000 = k.equals("choice_answers") || k.equals("speaker");
                            break;
                        case "toast":
                            var10000 = k.equals("quest_icon");
                            break;
                        case "quest":
                            var10000 = k.startsWith("journal_");
                            break;
                        case "shop":
                            var10000 = k.startsWith("shop_");
                            break;
                        case "skills":
                        case "skills2":
                        case "skills3":
                            var10000 = k.startsWith("skill_");
                            break;
                        case "inventory":
                            var10000 = k.equals("inventory") || k.equals("speaker");
                            break;
                        case "dialog":
                            var10000 = k.equals("speech_plate") || k.equals("speech_icon");
                            break;
                        case "chatlog":
                            var10000 = k.equals("chat_lines");
                            break;
                        default:
                            var10000 = false;
                    }
                }

                boolean own = var10000;
                if (own) {
                    out.add(t);
                }
            }
        }

        for (String k : List.of("group", "panel", "image", "text", "progress", "item", "clip")) {
            GuiSchema.Type tx = TYPES.get(k);
            if (tx != null) {
                out.add(tx);
            }
        }

        for (String kx : ADDON_TYPES) {
            GuiSchema.Type tx = TYPES.get(kx);
            if (tx != null) {
                out.add(tx);
            }
        }

        return out;
    }

    public static void addType(String key, String title, String doc, int width, int height, Consumer<GuiSchema.B> properties) {
        GuiSchema.B b = common(width, height);
        properties.accept(b);
        type(key, title, doc, b);
        ADDON_TYPES.add(key);
    }

    public static boolean isHud(String type) {
        return type != null && type.startsWith("hud_");
    }

    private static void hud(String key, String title, String doc, int w, int h, int x, int y) {
        String anchor = key.equals("coins") ? "top_right" : (key.equals("nav") ? "top" : "bottom");
        GuiSchema.B b = common(w, h, anchor);
        List<GuiSchema.Prop> ps = b.props;

        for (int i = 0; i < ps.size(); i++) {
            GuiSchema.Prop p = ps.get(i);
            if (p.key().equals("x")) {
                ps.set(i, new GuiSchema.Prop("x", p.label(), p.kind(), String.valueOf(key.equals("coins") ? -6 : x), p.choices(), p.group(), p.doc()));
            }

            if (p.key().equals("y")) {
                ps.set(i, new GuiSchema.Prop("y", p.label(), p.kind(), String.valueOf(key.equals("coins") ? 6 : y), p.choices(), p.group(), p.doc()));
            }
        }

        b.group("Часть HUD")
            .c(
                "style",
                "стиль",
                "default",
                EngineConfig.styles(key),
                "default — как в игре (вписывается в рамку элемента); bar — полоска; icons — ряд своих иконок; classic — хотбар Spraute; slots — свои ячейки; icon — значок и число; orb — шар, который заливается жидкостью"
            )
            .p("count", "иконок", GuiSchema.Kind.INT, "10", "Сколько иконок в ряду (стиль icons)")
            .p("pad", "отступ заливки", GuiSchema.Kind.INT, "1", "Отступ заливки полоски от краёв (стиль bar)")
            .c(
                "direction",
                "куда заполняется",
                "right",
                List.of("right", "left", "up", "down"),
                "Стиль bar: right — вправо, left — влево, up — снизу вверх, down — сверху вниз"
            )
            .p("gap", "промежуток", GuiSchema.Kind.INT, "0", "Между иконками или ячейками")
            .p("reverse", "справа налево", GuiSchema.Kind.BOOL, !key.equals("food") && !key.equals("air") ? "false" : "true", "Иконки заполняются справа")
            .p("showText", "число", GuiSchema.Kind.BOOL, "false", "Подписать значение (стиль bar)")
            .p("textColor", "цвет числа", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
            .p("fillColor", "цвет жидкости", GuiSchema.Kind.COLOR, "#00000000", "Стиль orb. Прозрачный — свой цвет части: красное здоровье, голубой воздух…")
            .p("backColor", "цвет стекла", GuiSchema.Kind.COLOR, "#00000000", "Стиль orb: фон шара. Прозрачный — тёмный")
            .p("waves", "волны", GuiSchema.Kind.BOOL, "true", "Стиль orb: поверхность колышется")
            .group("Текстуры")
            .p("back", "фон", GuiSchema.Kind.IMAGE, "", "bar — фон полоски; slots — ячейка; icon — подложка")
            .p("fill", "заливка", GuiSchema.Kind.IMAGE, "", "bar — заливка; slots — закрытая ячейка; icon — значок")
            .p("frame", "рамка", GuiSchema.Kind.IMAGE, "", "bar — рамка поверх; slots — выбранная ячейка")
            .p("full", "иконка: полная", GuiSchema.Kind.IMAGE, "", "Стиль icons")
            .p("half", "иконка: половина", GuiSchema.Kind.IMAGE, "", "Стиль icons")
            .p("empty", "иконка: пустая", GuiSchema.Kind.IMAGE, "", "Стиль icons");
        type("hud_" + key, title, doc, b);
    }

    private static void type(String key, String title, String doc, GuiSchema.B b) {
        if (singleImage(key)) {
            b.props.removeIf(p -> p.key().equals("backgroundImage"));
        }

        TYPES.put(key, new GuiSchema.Type(key, title, doc, Collections.unmodifiableList(b.props)));
    }

    public static boolean singleImage(String type) {
        return type.equals("image") || type.equals("skill_tooltip") || type.equals("skill_learn") || type.equals("skill_use") || type.equals("skill_reset");
    }

    public static boolean isLoose(String type) {
        return type.equals("group") || type.equals("drag") || type.equals("drop_zone");
    }

    public static boolean isContainer(String type) {
        return isLoose(type) || type.equals("clip") || type.equals("scroll") || type.equals("speech_plate");
    }

    public static GuiSchema.Type type(String key) {
        return TYPES.get(key);
    }

    public static List<GuiSchema.Type> types() {
        return new ArrayList<>(TYPES.values());
    }

    public static List<GuiSchema.Prop> docProps() {
        return Collections.unmodifiableList(DOC_PROPS);
    }

    public static GuiSchema.Prop docProp(String key) {
        for (GuiSchema.Prop p : DOC_PROPS) {
            if (p.key.equals(key)) {
                return p;
            }
        }

        return null;
    }

    static {
        DOC_PROPS.add(new GuiSchema.Prop("title", "название", GuiSchema.Kind.TEXT, "Интерфейс", List.of(), "Интерфейс", "Имя для редактора"));
        DOC_PROPS.add(
            new GuiSchema.Prop("width", "ширина макета", GuiSchema.Kind.INT, "384", List.of(), "Интерфейс", "Размер области, в которой расставлены элементы")
        );
        DOC_PROPS.add(
            new GuiSchema.Prop("height", "высота макета", GuiSchema.Kind.INT, "216", List.of(), "Интерфейс", "Размер области, в которой расставлены элементы")
        );
        DOC_PROPS.add(
            new GuiSchema.Prop(
                "layout",
                "масштаб",
                GuiSchema.Kind.CHOICE,
                "fit",
                List.of("fit", "screen", "height"),
                "Интерфейс",
                "fit — макет масштабируется под окно целиком; screen — элементы прижимаются к краям экрана, размер в пикселях интерфейса; height — масштаб по высоте экрана, а ширина вся, от края до края"
            )
        );
        DOC_PROPS.add(
            new GuiSchema.Prop("background", "затемнение фона", GuiSchema.Kind.COLOR, "#66000000", List.of(), "Интерфейс", "Цвет поверх мира под интерфейсом")
        );
        DOC_PROPS.add(
            new GuiSchema.Prop(
                "corners",
                "углы",
                GuiSchema.Kind.CHOICE,
                "smooth",
                List.of("smooth", "pixel"),
                "Интерфейс",
                "smooth — гладкие на любом размере; pixel — ступеньками, для пиксельного стиля"
            )
        );
        DOC_PROPS.add(
            new GuiSchema.Prop("closeOnEsc", "закрывать по Esc", GuiSchema.Kind.BOOL, "true", List.of(), "Поведение", "Можно ли закрыть интерфейс клавишей Esc")
        );
        DOC_PROPS.add(
            new GuiSchema.Prop(
                "pause", "пауза игры", GuiSchema.Kind.BOOL, "false", List.of(), "Поведение", "Ставить ли одиночную игру на паузу, пока открыт интерфейс"
            )
        );
        DOC_PROPS.add(new GuiSchema.Prop("openSound", "звук открытия", GuiSchema.Kind.SOUND, "", List.of(), "Поведение", "Звук при открытии"));
        hud(
            "health",
            "HUD · Здоровье",
            "Здоровье игрока: сердечки, круглая шкала Spraute (если включена система здоровья), полоска или свои иконки",
            81,
            9,
            -50,
            -30
        );
        hud("food", "HUD · Еда", "Сытость игрока", 81, 9, 50, -30);
        hud("armor", "HUD · Броня", "Броня игрока (видна, когда она есть)", 81, 9, -50, -40);
        hud("air", "HUD · Воздух", "Воздух под водой (виден, только пока кончается)", 81, 9, 50, -40);
        hud("exp", "HUD · Опыт", "Полоса опыта и уровень", 182, 5, 0, -24);
        hud("hotbar", "HUD · Хотбар", "Ячейки быстрого доступа", 182, 22, 0, 0);
        hud("coins", "HUD · Монеты", "Монеты Spraute (видны, когда система монет включена)", 56, 18, 0, 0);
        hud(
            "nav",
            "HUD · Навигатор",
            "Стрелки команды navigate: куда идти, подпись и расстояние. Вид стрелок — в настройках движка, часть «Навигатор»; подпись и цвет каждой стрелки задаёт сюжет",
            160,
            14,
            0,
            18
        );
        type(
            "hud_skill_abilities",
            "HUD · Способности",
            "Изученные способности с клавишами и перезарядкой. Размер значка — высота части (или ширина, если в столбик)",
            common(130, 20, "bottom")
                .group("Значки")
                .p("gap", "зазор", GuiSchema.Kind.NUMBER, "2", "")
                .p("vertical", "в столбик", GuiSchema.Kind.BOOL, "false", "")
                .p("keys", "клавиши", GuiSchema.Kind.BOOL, "true", "")
                .p("seconds", "секунды перезарядки", GuiSchema.Kind.BOOL, "true", "")
                .p("plate", "фон значка", GuiSchema.Kind.COLOR, "#A0101218", "")
                .p("plateImage", "картинка фона", GuiSchema.Kind.IMAGE, "", "")
                .p("frameImage", "рамка поверх значка", GuiSchema.Kind.IMAGE, "", "")
                .p("cooldownImage", "картинка перезарядки", GuiSchema.Kind.IMAGE, "", "Вместо затемнения")
                .p("pad", "отступ значка", GuiSchema.Kind.NUMBER, "2", "")
                .p("keyColor", "цвет клавиши", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
                .p("textColor", "цвет секунд", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("cooldown", "затемнение перезарядки", GuiSchema.Kind.COLOR, "#B0000000", "")
        );
        type(
            "hud_skill_level",
            "HUD · Уровни прокачки",
            "Название системы, уровень и полоска опыта",
            common(80, 2, "top_left")
                .group("Полоса")
                .c("direction", "куда заполняется", "right", List.of("right", "left", "up", "down"), "")
                .p("gap", "зазор", GuiSchema.Kind.NUMBER, "5", "")
                .p("showText", "название и уровень", GuiSchema.Kind.BOOL, "true", "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#80000000", "")
                .p("color", "заливка", GuiSchema.Kind.COLOR, "#00000000", "Прозрачная — главный цвет системы")
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
        );
        type(
            "hud_quests",
            "HUD · Цели квеста",
            "Отслеживаемый квест и его задачи (то, что было справа на экране)",
            common(150, 60, "top_right")
                .group("Цели")
                .p("titleColor", "цвет названия", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
                .p("textColor", "цвет задач", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("doneColor", "выполненные", GuiSchema.Kind.COLOR, "#FF7BE08A", "")
                .p("optionalColor", "необязательные", GuiSchema.Kind.COLOR, "#FFB8C0CC", "")
                .p("textScale", "размер задач", GuiSchema.Kind.NUMBER, "0.8", "")
                .p("maxTasks", "сколько задач", GuiSchema.Kind.INT, "5", "")
                .c("align", "выравнивание", "right", List.of("left", "center", "right"), "")
                .p("shadow", "тень", GuiSchema.Kind.BOOL, "true", "")
        );
        type(
            "choice_answers",
            "Окно · Варианты ответа",
            "Варианты меню выбора, столбиком сверху вниз: наведение мышью, клик, клавиши 1–9 и стрелки. Высота каждого — по тексту",
            common(160, 120, "top_left")
                .group("Варианты")
                .c("style", "стиль", "classic", List.of("classic", "flat", "image"), "classic — кнопки Spraute; flat — цветные плашки; image — свои картинки")
                .p("gap", "промежуток", GuiSchema.Kind.INT, "4", "")
                .p("minHeight", "мин. высота", GuiSchema.Kind.INT, "18", "")
                .p("padding", "отступ текста", GuiSchema.Kind.INT, "6", "")
                .p("textScale", "размер текста", GuiSchema.Kind.NUMBER, "0.75", "")
                .p("numbers", "номера", GuiSchema.Kind.BOOL, "false", "Подписывать «1.», «2.» перед вариантами")
                .group("Цвета")
                .p("textColor", "текст", GuiSchema.Kind.COLOR, "#FF42C7FF", "")
                .p("hoverTextColor", "текст под мышью", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("color", "фон (flat)", GuiSchema.Kind.COLOR, "#C0101420", "")
                .p("hoverColor", "фон под мышью (flat)", GuiSchema.Kind.COLOR, "#E0203048", "")
                .p("borderColor", "рамка (flat)", GuiSchema.Kind.COLOR, "#FF2E4058", "")
                .p("hoverBorderColor", "рамка под мышью (flat)", GuiSchema.Kind.COLOR, "#FF42C7FF", "")
                .p("radius", "скругление (flat)", GuiSchema.Kind.INT, "3", "")
                .group("Картинки (image)")
                .p("image", "кнопка", GuiSchema.Kind.IMAGE, "", "")
                .p("hoverImage", "кнопка под мышью", GuiSchema.Kind.IMAGE, "", "")
        );
        type(
            "speaker",
            "Окно · Говорящий",
            "Тот, кто задаёт вопрос (или игрок — в настройках меню выбора): во весь рост или голова",
            common(98, 98, "top_left")
                .group("Говорящий")
                .c("framing", "кадр", "body", List.of("body", "head"), "body — во весь рост, смотрит на мышь; head — только голова")
                .p("scale", "масштаб", GuiSchema.Kind.NUMBER, "1", "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#00000000", "")
        );
        type(
            "speech_plate",
            "Реплика · Плашка целиком",
            "Реплика как в игре: плашка по размеру текста, значок, «[Имя]: текст» и подсказка. Рамка элемента — только докуда текст может расти",
            common(288, 100, "bottom")
                .group("Плашка")
                .c("halign", "выравнивание", "center", List.of("left", "center", "right"), "К какому краю рамки прижата плашка по ширине")
                .c("valign", "прижата", "bottom", List.of("top", "middle", "bottom"), "К какому краю рамки прижата плашка, когда текста мало")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#80000000", "")
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "Имя — своим цветом, он задаётся у персонажа и на странице «Речь»")
        );
        type(
            "chat_lines",
            "История · Сообщения",
            "Сообщения истории чата, как в игре: новое внизу, колёсико прокручивает. Выше или ниже рамки в списке частей — под ней или поверх",
            common(226, 246, "center")
                .group("Сообщения")
                .p("background", "фон сообщения", GuiSchema.Kind.COLOR, "#B3000000", "")
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
        );
        type(
            "speech_text",
            "Реплика · Текст",
            "«[Имя]: текст» реплики: сам переносится по ширине плашки. Плашка растёт вместе с ним",
            common(100, 20, "top_left")
        );
        type(
            "speech_icon",
            "Реплика · Портрет",
            "Значок или голова того, кто говорит: сам меняется с каждой репликой",
            common(20, 20, "top_left").group("Портрет").p("background", "фон", GuiSchema.Kind.COLOR, "#00000000", "")
        );
        type(
            "menu_slots",
            "Меню · Ячейки окна",
            "Ячейки окна там, где их держит игра: для окна со своим фоном или без него. Двигать их нельзя: сюда игра кладёт предметы",
            common(176, 166, "top_left")
                .group("Ячейки")
                .p("color", "ячейка", GuiSchema.Kind.COLOR, "#FF8B8B8B", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#FF373737", "")
        );
        type(
            "menu_widget",
            "Меню · Кнопка игры",
            "Кнопка самого экрана игры: двигайте, меняйте размер и текст, скрывайте. Скрыть — глаз в списке частей; если удалить часть, кнопка просто останется как в игре",
            common(200, 20, "top_left")
                .group("Кнопка игры")
                .p("text", "текст", GuiSchema.Kind.TEXT, "", "Пока он такой же, как в игре, язык игры тоже действует")
                .p("enabled", "активна", GuiSchema.Kind.BOOL, "true", "Неактивную нельзя нажать")
                .group("Как в игре")
                .p("baseX", "X в игре", GuiSchema.Kind.INT, "0", "Откуда считается сдвиг: кнопка следует за раскладкой игры на других размерах экрана")
                .p("baseY", "Y в игре", GuiSchema.Kind.INT, "0", "")
                .p("baseW", "ширина в игре", GuiSchema.Kind.INT, "0", "")
                .p("baseH", "высота в игре", GuiSchema.Kind.INT, "0", "")
                .p("baseText", "текст в игре", GuiSchema.Kind.TEXT, "", "")
        );
        type(
            "quest_icon",
            "Окно · Значок квеста",
            "Значок уведомления квеста: новый, выполнен, провален — сам по событию",
            common(21, 23, "top_left")
                .group("Значок")
                .p(
                    "image",
                    "свой лист",
                    GuiSchema.Kind.IMAGE,
                    "",
                    "Лист 63×46: в ряд значки новой задачи, выполненной и проваленной, под ними такие же под мышью; пусто — значки Spraute"
                )
        );
        type(
            "journal_tabs",
            "Журнал · Вкладки",
            "Три вкладки: текущие, выполненные, проваленные. Квадраты в ряд по высоте элемента",
            common(75, 23, "top_left")
                .group("Вкладки")
                .p("gap", "промежуток", GuiSchema.Kind.INT, "3", "")
                .group("Текстуры")
                .p("current", "текущие", GuiSchema.Kind.IMAGE, "", "Лист 23×69: обычная, под мышью, выбранная — сверху вниз")
                .p("done", "выполненные", GuiSchema.Kind.IMAGE, "", "Лист 23×69")
                .p("failed", "проваленные", GuiSchema.Kind.IMAGE, "", "Лист 23×69")
        );
        type(
            "journal_list",
            "Журнал · Список квестов",
            "Квесты открытой вкладки: кнопки столбиком, колёсико прокручивает, клик выбирает",
            common(136, 195, "top_left")
                .group("Список")
                .p("entryHeight", "высота строки", GuiSchema.Kind.INT, "35", "")
                .p("gap", "промежуток", GuiSchema.Kind.INT, "2", "")
                .p("padding", "отступ", GuiSchema.Kind.INT, "2", "")
                .p("icons", "значки", GuiSchema.Kind.BOOL, "true", "Значок вкладки слева в строке")
                .p("scrollbar", "полоса прокрутки", GuiSchema.Kind.BOOL, "false", "")
                .group("Цвета")
                .p("textColor", "название", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("mutedColor", "подпись", GuiSchema.Kind.COLOR, "#FF9AA4B4", "")
                .p("accentColor", "отслеживаемый", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
                .group("Текстуры")
                .p("button", "строка", GuiSchema.Kind.IMAGE, "", "")
                .p("buttonHover", "строка под мышью", GuiSchema.Kind.IMAGE, "", "")
                .p("buttonOn", "выбранная строка", GuiSchema.Kind.IMAGE, "", "")
                .p(
                    "iconSheet",
                    "значки",
                    GuiSchema.Kind.IMAGE,
                    "",
                    "Лист 63×46: в ряд значки текущего, выполненного и проваленного квеста, под ними такие же под мышью"
                )
        );
        type(
            "journal_title",
            "Журнал · Заголовок квеста",
            "Название выбранного квеста (клик — отслеживать), кто выдал и «★ отслеживается»",
            common(222, 30, "top_left")
                .group("Заголовок")
                .p("trackHint", "«отслеживать»", GuiSchema.Kind.BOOL, "true", "Подсказка над названием")
                .p("giver", "кто выдал", GuiSchema.Kind.BOOL, "true", "")
                .group("Цвета")
                .p("textColor", "название", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("accentColor", "отслеживается", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
                .p("hoverColor", "под мышью", GuiSchema.Kind.COLOR, "#FF6EC3FF", "")
                .p("mutedColor", "кто выдал", GuiSchema.Kind.COLOR, "#FF9AA4B4", "")
        );
        type(
            "journal_page",
            "Журнал · Страница квеста",
            "Описание, задачи, награда и свои разделы квеста. Колёсико прокручивает",
            common(232, 163, "top_left")
                .group("Страница")
                .p("textScale", "размер текста", GuiSchema.Kind.NUMBER, "0.7", "")
                .group("Цвета")
                .p("textColor", "задачи", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("bodyColor", "описание", GuiSchema.Kind.COLOR, "#FFD8DEE8", "")
                .p("headerColor", "заголовки", GuiSchema.Kind.COLOR, "#FF6EC3FF", "")
                .p("accentColor", "прогресс, награда", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
                .p("rewardColor", "текст награды", GuiSchema.Kind.COLOR, "#FFFFE7A3", "")
                .p("doneColor", "выполнено", GuiSchema.Kind.COLOR, "#FF7BE08A", "")
                .p("failColor", "провалено", GuiSchema.Kind.COLOR, "#FFFF6B6B", "")
                .group("Текстуры")
                .p("taskLine", "полоса задачи", GuiSchema.Kind.IMAGE, "", "")
        );
        type(
            "journal_state",
            "Журнал · Итог квеста",
            "«Выполнен» или «Провален» у законченного квеста (прижат вправо)",
            common(130, 10, "top_left")
                .group("Итог")
                .p("textScale", "размер", GuiSchema.Kind.NUMBER, "0.75", "")
                .p("doneColor", "выполнен", GuiSchema.Kind.COLOR, "#FF7BE08A", "")
                .p("failColor", "провален", GuiSchema.Kind.COLOR, "#FFFF6B6B", "")
        );
        type(
            "skill_field",
            "Прокачка · Навыки",
            "Сами навыки: дерево или таблички, как задано в меню. Поле тащится мышью, таблички листаются колесом",
            common(300, 220, "top_left")
        );
        type(
            "skill_xp",
            "Прокачка · Полоса опыта",
            "Сколько набрано до следующего уровня",
            common(120, 6, "top_left")
                .group("Полоса")
                .c("direction", "куда заполняется", "right", List.of("right", "left", "up", "down"), "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#FF232635", "")
                .p("color", "заливка", GuiSchema.Kind.COLOR, "#00000000", "Прозрачная — главный цвет меню")
                .p("back", "картинка фона", GuiSchema.Kind.IMAGE, "", "")
                .p("fill", "картинка заливки", GuiSchema.Kind.IMAGE, "", "Открывается слева направо")
        );
        type(
            "skill_points",
            "Прокачка · Очки",
            "Значки и сколько очков каждого вида",
            common(120, 16, "top_right").group("Очки").c("align", "прижать", "right", List.of("left", "right"), "")
        );
        type(
            "skill_tooltip",
            "Прокачка · Окно при наведении",
            "Что показывается, когда мышь на навыке: всё о нём. Ширина — ширина части, высота подстраивается под текст",
            common(186, 90, "top_left")
                .group("Окно")
                .p("follow", "у мыши", GuiSchema.Kind.BOOL, "true", "Выключено — стоит там, куда поставлена")
                .p("pad", "отступ", GuiSchema.Kind.NUMBER, "8", "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#F01C1F2B", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#00000000", "Прозрачная — главный цвет меню")
                .p("image", "картинка фона", GuiSchema.Kind.IMAGE, "", "")
                .p("hint", "подсказка «Нажмите, чтобы изучить»", GuiSchema.Kind.BOOL, "true", "")
        );
        type("skill_tabs", "Прокачка · Вкладки", "Разделы, которые показывает меню: если их больше одного", common(300, 18, "top_left"));
        type(
            "skill_info",
            "Прокачка · Описание навыка",
            "Что выбрано или под мышью: название, описание, что даёт, цена, что мешает",
            common(160, 120, "top_right")
        );

        for (String[] b : new String[][]{
            {"skill_learn", "Прокачка · Кнопка «Изучить»", "Изучить"},
            {"skill_use", "Прокачка · Кнопка «Применить»", "Применить"},
            {"skill_reset", "Прокачка · Кнопка сброса", "Сбросить всё"}
        }) {
            type(
                b[0],
                b[1],
                "Видна, только когда нужна",
                common(120, 18, "top_right")
                    .group("Кнопка")
                    .p("text", "надпись", GuiSchema.Kind.TEXT, b[2], "")
                    .p("image", "картинка", GuiSchema.Kind.IMAGE, "", "Пусто — главный цвет меню")
                    .p("hoverImage", "под мышью", GuiSchema.Kind.IMAGE, "", "")
                    .p("offImage", "когда нельзя", GuiSchema.Kind.IMAGE, "", "")
            );
        }

        type(
            "shop_goods",
            "Магазин · Товары",
            "Карточки товаров рядами, цена под каждой. Колёсико прокручивает, клик выбирает",
            common(182, 140, "top_left")
                .group("Сетка")
                .p("columns", "в ряду", GuiSchema.Kind.INT, "5", "")
                .p("cellWidth", "ширина карточки", GuiSchema.Kind.INT, "30", "")
                .p("cellHeight", "высота карточки", GuiSchema.Kind.INT, "43", "")
                .p("gapX", "промежуток X", GuiSchema.Kind.INT, "6", "")
                .p("gapY", "промежуток Y", GuiSchema.Kind.INT, "7", "")
                .p("padX", "отступ X", GuiSchema.Kind.INT, "2", "")
                .p("padY", "отступ Y", GuiSchema.Kind.INT, "5", "")
                .group("Цвета")
                .p("priceColor", "цена", GuiSchema.Kind.COLOR, "#FFE2D7C4", "")
                .p("sellColor", "продажа", GuiSchema.Kind.COLOR, "#FF7BE08A", "")
                .p("mutedColor", "«пусто»", GuiSchema.Kind.COLOR, "#FF8C91A8", "")
                .group("Текстуры")
                .p("cell", "карточка", GuiSchema.Kind.IMAGE, "", "")
                .p("cellOn", "выбранная", GuiSchema.Kind.IMAGE, "", "")
                .p("locked", "заблокирована", GuiSchema.Kind.IMAGE, "", "")
                .p("sold", "распродана", GuiSchema.Kind.IMAGE, "", "")
        );
        type(
            "shop_slider",
            "Магазин · Ползунок",
            "Ползунок прокрутки товаров: ездит сверху вниз по элементу",
            common(9, 162, "top_left")
                .group("Ползунок")
                .p("thumbHeight", "высота ползунка", GuiSchema.Kind.INT, "10", "")
                .p("always", "всегда виден", GuiSchema.Kind.BOOL, "false", "")
                .p("image", "картинка", GuiSchema.Kind.IMAGE, "", "")
        );
        type(
            "shop_item",
            "Магазин · Выбранный товар",
            "Крупный предмет выбранного товара и «x сколько»",
            common(40, 32, "top_left")
                .group("Товар")
                .p("amount", "«x сколько»", GuiSchema.Kind.BOOL, "true", "")
                .p("textColor", "цвет", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
        );
        type(
            "shop_info",
            "Магазин · Цена и итог",
            "Название, цена, количество, итог и состояние сделки («Готово к покупке!»)",
            common(150, 76, "top_left")
                .group("Цвета")
                .p("textColor", "текст", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("priceColor", "цена", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
                .p("sellColor", "продажа", GuiSchema.Kind.COLOR, "#FF7BE08A", "")
                .p("okColor", "можно", GuiSchema.Kind.COLOR, "#FF55FF55", "")
                .p("badColor", "нельзя", GuiSchema.Kind.COLOR, "#FFFF5555", "")
        );
        type(
            "shop_arrows",
            "Магазин · Стрелки количества",
            "−5 −1 +1 +5: сколько раз совершить сделку",
            common(82, 15, "top_left")
                .group("Текстуры")
                .p("minus5", "−5", GuiSchema.Kind.IMAGE, "", "")
                .p("minus1", "−1", GuiSchema.Kind.IMAGE, "", "")
                .p("plus1", "+1", GuiSchema.Kind.IMAGE, "", "")
                .p("plus5", "+5", GuiSchema.Kind.IMAGE, "", "")
                .p("minus5Hover", "−5 под мышью", GuiSchema.Kind.IMAGE, "", "")
                .p("minus1Hover", "−1 под мышью", GuiSchema.Kind.IMAGE, "", "")
                .p("plus1Hover", "+1 под мышью", GuiSchema.Kind.IMAGE, "", "")
                .p("plus5Hover", "+5 под мышью", GuiSchema.Kind.IMAGE, "", "")
        );
        type(
            "shop_deal",
            "Магазин · Кнопка сделки",
            "КУПИТЬ / ПРОДАТЬ / ОБМЕНЯТЬ",
            common(88, 14, "top_left")
                .group("Кнопка")
                .p("textScale", "размер текста", GuiSchema.Kind.NUMBER, "0.75", "")
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("image", "кнопка", GuiSchema.Kind.IMAGE, "", "")
                .p("hoverImage", "под мышью", GuiSchema.Kind.IMAGE, "", "")
        );
        type(
            "shop_desc",
            "Магазин · Описание товара",
            "Описание выбранного товара, сколько строк влезет",
            common(106, 20, "top_left")
                .group("Описание")
                .p("textScale", "размер текста", GuiSchema.Kind.NUMBER, "0.6", "")
                .p("textColor", "цвет", GuiSchema.Kind.COLOR, "#FF8C91A8", "")
        );
        type(
            "shop_money",
            "Магазин · Деньги игрока",
            "Монета и сколько их у игрока (прижато вправо)",
            common(90, 13, "top_left").group("Деньги").p("textColor", "цвет", GuiSchema.Kind.COLOR, "#FFFFD25A", "")
        );
        type("group", "Группа", "Папка для элементов: двигается, скрывается и появляется целиком. Ничего не рисует", common(160, 100));
        type(
            "drag",
            "Перетаскивание",
            "Всё, что вложено сюда, игрок таскает мышью целиком: картинку, текст, кнопку. Куда бросать — объект «Зона сброса». В сюжете: on guidrop \"id\" { log event(\"zone\") }, guizone(\"id\"), gui undrag \"id\"",
            common(48, 48)
                .group("Перетаскивание")
                .p("enabled", "можно таскать", GuiSchema.Kind.BOOL, "true", "Из сюжета: gui set \"id\" \"enabled\" \"false\"")
                .p("tag", "метка", GuiSchema.Kind.TEXT, "", "Слово для зон: зона с «принимает: ключ» возьмёт только объекты с меткой ключ")
                .c("axis", "направление", "free", List.of("free", "x", "y"), "free — куда угодно, x — только по горизонтали, y — только по вертикали")
                .p("grid", "шаг сетки", GuiSchema.Kind.INT, "0", "Двигается шагами в столько пикселей, 0 — плавно")
                .c(
                    "bounds",
                    "границы",
                    "screen",
                    List.of("screen", "parent", "none"),
                    "screen — не вытащить за экран, parent — за объект, в который вложен, none — без границ"
                )
                .c("miss", "если бросили мимо зоны", "stay", List.of("stay", "return"), "stay — остаётся где бросили, return — возвращается, откуда взяли")
                .group("Пока тащат")
                .p("carryScale", "масштаб", GuiSchema.Kind.NUMBER, "1.05", "Например 1.1 — приподнимается над остальными")
                .p("carryOpacity", "непрозрачность", GuiSchema.Kind.NUMBER, "0.9", "")
                .p("takeSound", "звук, когда взяли", GuiSchema.Kind.SOUND, "", "")
                .p("dropSound", "звук, когда положили в зону", GuiSchema.Kind.SOUND, "", "")
        );
        type(
            "drop_zone",
            "Зона сброса",
            "Сюда бросают объекты «Перетаскивание». В неё можно вложить картинку или текст: они рисуются под брошенным",
            common(64, 64)
                .group("Зона")
                .p("accept", "принимает", GuiSchema.Kind.TEXT, "", "Метки через запятую: ключ, карта. Пусто — всё. Чужое отскочит обратно")
                .c("snap", "куда встаёт", "center", List.of("center", "none"), "center — в середину зоны, none — где отпустили")
                .p("limit", "сколько вмещает", GuiSchema.Kind.INT, "0", "0 — сколько угодно")
                .c("full", "если занята", "reject", List.of("reject", "swap"), "reject — новый отскочит, swap — поменяются местами")
                .group("Вид зоны")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#30FFFFFF", "")
                .p("radius", "скругление", GuiSchema.Kind.INT, "3", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#80FFFFFF", "")
                .p("borderWidth", "толщина рамки", GuiSchema.Kind.INT, "1", "")
                .p("hoverFill", "подсветка: можно", GuiSchema.Kind.COLOR, "#6042C7FF", "Когда над зоной держат подходящий объект")
                .p("denyFill", "подсветка: нельзя", GuiSchema.Kind.COLOR, "#60FF5555", "Когда объект не подходит или зона занята")
                .p("showFree", "подсказывать свободные", GuiSchema.Kind.BOOL, "true", "Пока объект тащат, подходящие зоны слегка подсвечены")
        );
        type(
            "clip",
            "Кроп-зона",
            "Обрезает всё, что внутри неё, по своим границам",
            common(160, 100)
                .group("Вид зоны")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#00000000", "")
                .p("radius", "скругление", GuiSchema.Kind.INT, "0", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#00000000", "")
                .p("borderWidth", "толщина рамки", GuiSchema.Kind.INT, "0", "")
        );
        type(
            "scroll",
            "Панель прокрутки",
            "Содержимое прокручивается колесом мыши и ползунком; лишнее обрезается",
            common(160, 120)
                .group("Прокрутка")
                .c("direction", "направление", "vertical", List.of("vertical", "horizontal", "both"), "")
                .p("contentWidth", "ширина содержимого", GuiSchema.Kind.INT, "0", "0 — по крайним элементам")
                .p("contentHeight", "высота содержимого", GuiSchema.Kind.INT, "0", "0 — по крайним элементам")
                .p("padding", "отступ в конце", GuiSchema.Kind.INT, "6", "Запас после последнего элемента")
                .p("speed", "шаг колеса", GuiSchema.Kind.INT, "24", "Сколько пикселей за щелчок колеса")
                .p("smooth", "плавно", GuiSchema.Kind.BOOL, "true", "")
                .p("scrollbar", "ползунок", GuiSchema.Kind.BOOL, "true", "")
                .p("barColor", "цвет ползунка", GuiSchema.Kind.COLOR, "#AA8B6CFF", "")
                .p("barWidth", "ширина ползунка", GuiSchema.Kind.INT, "4", "")
                .group("Вид панели")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#80101116", "")
                .p("radius", "скругление", GuiSchema.Kind.INT, "4", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#FF2E3140", "")
                .p("borderWidth", "толщина рамки", GuiSchema.Kind.INT, "1", "")
        );
        type(
            "panel",
            "Панель",
            "Прямоугольник: фон, рамка, скругление, тень, градиент",
            common(120, 70)
                .group("Заливка")
                .c(
                    "look",
                    "вид",
                    "flat",
                    List.of("flat", "minecraft", "minecraft_dark"),
                    "flat — свои цвета; minecraft — серое окно игры; minecraft_dark — тёмный камень, как в настройках игры"
                )
                .p("color", "цвет", GuiSchema.Kind.COLOR, "#E01C1E26", "Основной цвет фона")
                .c("gradient", "градиент", "none", List.of("none", "vertical", "horizontal"), "Переход от основного цвета ко второму")
                .p("color2", "второй цвет", GuiSchema.Kind.COLOR, "#E0101116", "Конечный цвет градиента")
                .p("hoverColor", "цвет под мышью", GuiSchema.Kind.COLOR, "#00000000", "Прозрачный — не меняется")
                .group("Рамка")
                .p("borderWidth", "толщина рамки", GuiSchema.Kind.INT, "1", "0 — без рамки")
                .p("borderColor", "цвет рамки", GuiSchema.Kind.COLOR, "#FF3D4157", "")
                .p("hoverBorderColor", "рамка под мышью", GuiSchema.Kind.COLOR, "#00000000", "Прозрачный — не меняется")
                .p("radius", "скругление", GuiSchema.Kind.INT, "4", "Радиус углов")
                .p("shadow", "тень", GuiSchema.Kind.INT, "6", "Размер мягкой тени, 0 — без тени")
        );
        type(
            "image",
            "Картинка",
            "Картинка проекта или текстура игры",
            common(64, 64)
                .group("Картинка")
                .p("image", "картинка", GuiSchema.Kind.IMAGE, "", "Имя картинки из папки «Картинки» или путь к текстуре: minecraft:textures/item/apple.png")
                .p("hoverImage", "картинка под мышью", GuiSchema.Kind.IMAGE, "", "Плавно сменяет обычную при наведении")
                .p("smooth", "сглаживать", GuiSchema.Kind.BOOL, "false", "Гладкая картинка при увеличении и уменьшении; выключено — чёткие пиксели")
                .p("tint", "оттенок", GuiSchema.Kind.COLOR, "#FFFFFFFF", "Цвет, на который умножается картинка")
                .p("hoverTint", "оттенок под мышью", GuiSchema.Kind.COLOR, "#00000000", "Прозрачный — не меняется")
                .c("fit", "вписывание", "stretch", List.of("stretch", "contain", "cover", "tile"), "Как картинка заполняет рамку")
                .p("flipX", "отразить по X", GuiSchema.Kind.BOOL, "false", "")
                .p("flipY", "отразить по Y", GuiSchema.Kind.BOOL, "false", "")
                .group("Область текстуры")
                .p("u0", "u от", GuiSchema.Kind.NUMBER, "0", "Часть текстуры: доля ширины 0..1")
                .p("v0", "v от", GuiSchema.Kind.NUMBER, "0", "Часть текстуры: доля высоты 0..1")
                .p("u1", "u до", GuiSchema.Kind.NUMBER, "1", "")
                .p("v1", "v до", GuiSchema.Kind.NUMBER, "1", "")
        );
        type(
            "text",
            "Текст",
            "Надпись с переносом, выравниванием, стилями и печатной машинкой",
            common(140, 20)
                .group("Текст")
                .p(
                    "text",
                    "текст",
                    GuiSchema.Kind.TEXT,
                    "Текст",
                    "Правая кнопка мыши: подставить значение, здоровье, уровень прокачки и другое. Поддерживает \\n для новой строки. Из сюжета: gui text \"id\" \"...\""
                )
                .c(
                    "font",
                    "шрифт",
                    "minecraft",
                    List.of("minecraft", "sans", "serif", "mono"),
                    "minecraft — пиксельный шрифт игры; остальные — гладкие системные шрифты: без засечек, с засечками, моноширинный"
                )
                .p("color", "цвет", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("hoverColor", "цвет под мышью", GuiSchema.Kind.COLOR, "#00000000", "Прозрачный — не меняется")
                .p("scale", "размер", GuiSchema.Kind.NUMBER, "1", "Масштаб шрифта")
                .c("align", "выравнивание", "left", List.of("left", "center", "right"), "")
                .c("valign", "по вертикали", "top", List.of("top", "middle", "bottom"), "")
                .p("wrap", "переносить", GuiSchema.Kind.BOOL, "true", "Переносить строки по ширине элемента")
                .c(
                    "overflow",
                    "что не влезло",
                    "visible",
                    List.of("visible", "clip", "ellipsis"),
                    "visible — видно и за рамкой; clip — обрезать по рамке; ellipsis — обрезать и поставить «…»"
                )
                .p("lineSpacing", "межстрочный", GuiSchema.Kind.INT, "1", "")
                .p("shadow", "тень", GuiSchema.Kind.BOOL, "true", "")
                .group("Стиль")
                .p("bold", "жирный", GuiSchema.Kind.BOOL, "false", "")
                .p("italic", "курсив", GuiSchema.Kind.BOOL, "false", "")
                .p("underline", "подчёркнутый", GuiSchema.Kind.BOOL, "false", "")
                .p("strike", "зачёркнутый", GuiSchema.Kind.BOOL, "false", "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#00000000", "Подложка под текстом")
                .p("padding", "отступ", GuiSchema.Kind.INT, "0", "")
                .p("typewriter", "печать, симв/с", GuiSchema.Kind.NUMBER, "0", "Текст появляется по буквам; 0 — сразу")
                .p("shrink", "уменьшать, если не влезает", GuiSchema.Kind.BOOL, "false", "Длинный текст становится мельче, но не меньше половины размера")
                .p("speakerColor", "цвет говорящего", GuiSchema.Kind.BOOL, "false", "В окне реплик: цвет имени персонажа, если он задан; иначе цвет выше")
        );
        type(
            "button",
            "Кнопка",
            "Кнопка с событием для сюжета: wait button \"id\"",
            common(110, 22)
                .group("Кнопка")
                .c("look", "вид", "flat", List.of("flat", "minecraft"), "flat — свои цвета; minecraft — как кнопки игры, цвета фона ниже не действуют")
                .c(
                    "menuAction",
                    "действие в меню игры",
                    "none",
                    List.of("none", "close", "singleplayer", "multiplayer", "options", "quit"),
                    "Только для кнопок, добавленных в меню игры: что она делает"
                )
                .p("text", "текст", GuiSchema.Kind.TEXT, "Кнопка", "")
                .c(
                    "font",
                    "шрифт",
                    "minecraft",
                    List.of("minecraft", "sans", "serif", "mono"),
                    "minecraft — пиксельный шрифт игры; остальные — гладкие системные шрифты: без засечек, с засечками, моноширинный"
                )
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("textScale", "размер текста", GuiSchema.Kind.NUMBER, "1", "")
                .p("enabled", "активна", GuiSchema.Kind.BOOL, "true", "Неактивную кнопку нельзя нажать")
                .c(
                    "action",
                    "действие",
                    "event",
                    List.of("event", "close", "event_close"),
                    "event — сообщить сюжету; close — закрыть интерфейс; event_close — и то и другое"
                )
                .p("key", "клавиша", GuiSchema.Kind.TEXT, "", "Горячая клавиша: 1, 2, E, SPACE, ENTER...")
                .p("sound", "звук", GuiSchema.Kind.SOUND, "minecraft:ui.button.click", "")
                .group("Цвета")
                .p("color", "фон", GuiSchema.Kind.COLOR, "#FF2B2F45", "")
                .p("hoverColor", "под мышью", GuiSchema.Kind.COLOR, "#FF3A4066", "")
                .p("pressColor", "нажата", GuiSchema.Kind.COLOR, "#FF8B6CFF", "")
                .p("disabledColor", "неактивна", GuiSchema.Kind.COLOR, "#FF22242E", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#FF4A5070", "")
                .p("hoverBorderColor", "рамка под мышью", GuiSchema.Kind.COLOR, "#FFA08AFF", "")
                .p("borderWidth", "толщина рамки", GuiSchema.Kind.INT, "1", "")
                .p("radius", "скругление", GuiSchema.Kind.INT, "4", "")
                .group("Иконка")
                .p("buttonHoverImage", "фон при наведении", GuiSchema.Kind.IMAGE, "", "Использует те же края 9-slice")
                .p("buttonPressImage", "фон при нажатии", GuiSchema.Kind.IMAGE, "", "")
                .p("buttonDisabledImage", "фон выключенной кнопки", GuiSchema.Kind.IMAGE, "", "")
                .p("icon", "иконка", GuiSchema.Kind.IMAGE, "", "Картинка слева от текста")
                .p("iconSize", "размер иконки", GuiSchema.Kind.INT, "14", "")
        );
        type(
            "entity",
            "Сущность",
            "Живая 3D-модель сущности с анимацией",
            common(70, 110)
                .group("Модель")
                .p("entity", "сущность", GuiSchema.Kind.ENTITY, "spraute:npc", "Тип сущности")
                .p("scale", "масштаб", GuiSchema.Kind.NUMBER, "1", "")
                .p("yaw", "поворот", GuiSchema.Kind.NUMBER, "200", "Поворот модели по горизонтали")
                .p("pitch", "наклон", GuiSchema.Kind.NUMBER, "5", "")
                .p("followMouse", "смотрит на мышь", GuiSchema.Kind.BOOL, "false", "Как в окне инвентаря")
                .p("spin", "вращение, °/с", GuiSchema.Kind.NUMBER, "0", "Автоматическое вращение")
                .group("Анимация")
                .p("anim", "анимация", GuiSchema.Kind.ANIM, "", "Анимация анимаций на модели")
                .p("animLayer", "слой", GuiSchema.Kind.TEXT, "action", "")
                .p("animLoop", "цикл", GuiSchema.Kind.BOOL, "true", "")
                .p("animSpeed", "скорость", GuiSchema.Kind.NUMBER, "1", "")
                .group("Фон")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#00000000", "")
        );
        type(
            "item",
            "Предмет",
            "Иконка предмета",
            common(32, 32)
                .group("Предмет")
                .p("item", "предмет", GuiSchema.Kind.ITEM, "minecraft:diamond", "")
                .p("count", "количество", GuiSchema.Kind.INT, "1", "")
                .p("showCount", "показывать число", GuiSchema.Kind.BOOL, "true", "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#00000000", "")
        );
        type(
            "slot",
            "Слот",
            "Ячейка для предмета. Игрок кладёт в неё предметы из элемента «Инвентарь» и забирает их. Сюжет: slotitem(\"id\"), slotcount(\"id\"), gui slot \"id\" \"minecraft:apple\" count 3, wait slot \"id\"",
            common(20, 20)
                .group("Слот")
                .c(
                    "mode",
                    "режим",
                    "storage",
                    List.of("storage", "take", "put", "display"),
                    "storage — класть и забирать; take — только забрать (награда); put — только положить (сдать предмет); display — только показ"
                )
                .p("item", "предмет", GuiSchema.Kind.ITEM, "", "Что лежит в слоте при первом открытии")
                .p("count", "количество", GuiSchema.Kind.INT, "1", "")
                .p("filter", "только предмет", GuiSchema.Kind.ITEM, "", "Пусто — подходит любой предмет")
                .p("max", "максимум", GuiSchema.Kind.INT, "64", "Сколько предметов помещается")
                .group("Вид")
                .c("style", "стиль", "vanilla", List.of("vanilla", "flat", "none"), "vanilla — как ячейка Minecraft, flat — плоская, none — без фона")
                .p("texture", "картинка фона", GuiSchema.Kind.IMAGE, "", "Своя картинка ячейки вместо стиля")
                .p("background", "цвет (flat)", GuiSchema.Kind.COLOR, "#AA000000", "")
                .p("hoverColor", "подсветка", GuiSchema.Kind.COLOR, "#80FFFFFF", "")
                .p("ghost", "подсказка-предмет", GuiSchema.Kind.ITEM, "", "Полупрозрачный предмет в пустом слоте: что сюда положить")
        );
        type(
            "inventory",
            "Инвентарь игрока",
            "Ячейки инвентаря игрока: из них игрок берёт предметы и кладёт в слоты",
            common(162, 76)
                .group("Инвентарь")
                .c(
                    "rows",
                    "ячейки",
                    "all",
                    List.of("all", "main", "hotbar", "armor", "offhand", "craft", "craft_result", "single"),
                    "all — рюкзак и хотбар, main — только рюкзак, hotbar — только хотбар, armor — броня столбиком, offhand — вторая рука, craft — сетка крафта 2×2, craft_result — результат крафта, single — одна ячейка (номер ниже): так каждую ячейку можно поставить куда угодно. Крафт работает только в окне «Инвентарь» настроек движка"
                )
                .p(
                    "slot",
                    "номер ячейки",
                    GuiSchema.Kind.TEXT,
                    "1",
                    "Для single: 1–9 — хотбар, 10–36 — рюкзак, head, chest, legs, feet — броня, offhand — вторая рука"
                )
                .p("cell", "размер ячейки", GuiSchema.Kind.INT, "18", "")
                .group("Вид")
                .c("style", "стиль", "vanilla", List.of("vanilla", "flat", "none"), "")
                .p("texture", "картинка ячейки", GuiSchema.Kind.IMAGE, "", "")
                .p("background", "цвет (flat)", GuiSchema.Kind.COLOR, "#AA000000", "")
                .p("hoverColor", "подсветка", GuiSchema.Kind.COLOR, "#80FFFFFF", "")
        );
        type(
            "progress",
            "Полоса",
            "Полоса прогресса, здоровья, загрузки",
            common(120, 10)
                .group("Значение")
                .p("value", "значение", GuiSchema.Kind.NUMBER, "0.6", "От 0 до 1. Из сюжета: gui progress \"id\" 0.4")
                .c(
                    "bind",
                    "показывает",
                    "none",
                    List.of("none", "health", "food", "armor", "air", "exp"),
                    "Сама показывает здоровье, еду, броню, воздух или опыт игрока (none — значение выше или из сюжета)"
                )
                .c("direction", "направление", "right", List.of("right", "left", "up", "down"), "")
                .p("smooth", "плавность", GuiSchema.Kind.NUMBER, "8", "Скорость плавного изменения, 0 — мгновенно")
                .group("Цвета")
                .p("color", "заполнение", GuiSchema.Kind.COLOR, "#FF4CC38A", "")
                .p("color2", "конец градиента", GuiSchema.Kind.COLOR, "", "Пусто — без градиента")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#FF16171D", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#FF3D4157", "")
                .p("borderWidth", "толщина рамки", GuiSchema.Kind.INT, "1", "")
                .p("radius", "скругление", GuiSchema.Kind.INT, "3", "")
                .group("Надпись")
                .p("label", "текст", GuiSchema.Kind.TEXT, "", "{p} — проценты. Например: {p}%")
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
        );
        type(
            "input",
            "Поле ввода",
            "Поле, куда игрок пишет текст. Сюжет читает его функцией input(\"id\")",
            common(140, 20)
                .group("Поле")
                .p("text", "текст", GuiSchema.Kind.TEXT, "", "Начальное значение")
                .p("placeholder", "подсказка", GuiSchema.Kind.TEXT, "Введите текст", "Серый текст в пустом поле")
                .p("maxLength", "макс. длина", GuiSchema.Kind.INT, "64", "")
                .p("textColor", "цвет текста", GuiSchema.Kind.COLOR, "#FFFFFFFF", "")
                .p("background", "фон", GuiSchema.Kind.COLOR, "#FF12131A", "")
                .p("borderColor", "рамка", GuiSchema.Kind.COLOR, "#FF3D4157", "")
                .p("focusColor", "рамка в фокусе", GuiSchema.Kind.COLOR, "#FF8B6CFF", "")
                .p("radius", "скругление", GuiSchema.Kind.INT, "3", "")
        );
    }

    public static final class B {
        final List<GuiSchema.Prop> props = new ArrayList<>();
        String group = "Основное";

        public GuiSchema.B group(String g) {
            this.group = g;
            return this;
        }

        public GuiSchema.B p(String key, String label, GuiSchema.Kind kind, String def, String doc) {
            this.props.add(new GuiSchema.Prop(key, label, kind, def, List.of(), this.group, doc));
            return this;
        }

        public GuiSchema.B c(String key, String label, String def, List<String> choices, String doc) {
            this.props.add(new GuiSchema.Prop(key, label, GuiSchema.Kind.CHOICE, def, choices, this.group, doc));
            return this;
        }
    }

    public static enum Kind {
        INT,
        NUMBER,
        TEXT,
        COLOR,
        BOOL,
        CHOICE,
        IMAGE,
        VIDEO,
        ENTITY,
        ITEM,
        ANIM,
        SOUND,
        ELEMENT;
    }

    public record Prop(String key, String label, GuiSchema.Kind kind, String def, List<String> choices, String group, String doc) {
    }

    public record Type(String key, String title, String doc, List<GuiSchema.Prop> props) {
        public GuiSchema.Prop prop(String key) {
            for (GuiSchema.Prop p : this.props) {
                if (p.key.equals(key)) {
                    return p;
                }
            }

            return null;
        }
    }
}
