package org.sprautestudio.spraute_engine.editor.engine;

import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sprautestudio.spraute_engine.addon.AddonClient;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.PlateLayout;
import org.sprautestudio.spraute_engine.engine.client.ChoiceLayout;
import org.sprautestudio.spraute_engine.engine.client.HudPainter;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.MenuRuntime;
import org.sprautestudio.spraute_engine.skills.SkillHud;
import org.sprautestudio.spraute_engine.skills.SkillLook;

public final class EngineWindows {
    public static final List<String> ROLES = EngineConfig.WINDOWS;
    private static final int CHAT_LINES_W = 226;
    private static final int CHAT_LINES_H = 246;

    private EngineWindows() {
    }

    public static String help(String role) {
        AddonClient.Window addon = AddonClient.windowOf(role);
        if (addon != null) {
            return addon.help();
        } else if (MenuRuntime.isMenu(role)) {
            return "Под частями — настоящий экран игры: её фон, логотип, заголовки и окна. Кнопки экрана — части «Меню · Кнопка игры»: двигайте, меняйте размер и текст, скрывайте. Добавленное — картинки, панели, текст, кнопки — ляжет под ними, поверх фона экрана. Чтобы убрать фон и логотип, положите картинку или панель на весь экран. Списки внутри экрана целиком двигаются как одна часть; ячейки окон с предметами не двигаются";
        } else {
            return switch (role) {
                case "hud" -> "Части игры — элементы «HUD ·»: здоровье, еда, броня, воздух, опыт, хотбар, монеты, цели квеста, способности и уровни прокачки. Чего нет на холсте — того нет в игре. Тексты: {hp} {maxhp} {food} {armor} {air} {level} {exp} {coins} {x} {y} {z} {player} {time}";
                case "choice" -> "{name} — кто спрашивает, {question} — вопрос (печатается по буквам). «Варианты ответа» — кнопки: мышь, 1–9, стрелки. К тому, кто спрашивает, подлетает камера";
                case "toast" -> "Плашка сверху при событиях квеста. {title} — «Задача выполнена»…, {text} — задача или квест. «Значок квеста» сам меняется: новый, выполнен, провален. Когда и какие события показывать — страница «Журнал квестов»";
                case "zone" -> "Название зоны, когда игрок в неё входит. {zone} — название, {subtitle} — подпись. Сколько секунд держится и звук — в настройках зоны";
                case "quest" -> "Журнал квестов (J). Живые части — «Журнал ·»: вкладки, список квестов, заголовок, страница, итог. Тексты: {tab} — вкладка, {count} — сколько квестов, {quest}, {giver}, {state}, {close} — подсказка закрытия";
                case "shop" -> "Окно торговли. Живые части — «Магазин ·»: товары, ползунок, выбранный товар, цена и итог, стрелки, кнопка, описание, деньги. Тексты: {title} — название магазина, {balance} — деньги игрока";
                case "skills", "skills2", "skills3" -> "Панель прокачки. Живые части — «Прокачка ·»: навыки, полоса опыта, очки, вкладки, описание, кнопки: двигайте, меняйте размер, убирайте лишнее. Добавляйте картинки и текст. Тексты: {title} {level} {nextlevel} — следующий уровень, {xp} {xpnext} {points} {node}. Менюшку при наведении можно настроить: часть «Окно при наведении». Глубина фона: кнопка «Добавить слой фона» слева внизу, когда ничего не выбрано. Какой макет у какого меню — в редакторе системы, страница «Вид панели»";
                case "chatlog" -> "Окно истории сообщений (клавиша истории Spraute): фон, сообщения и рамка. «История · Сообщения» — сами сообщения, в редакторе показан пример; порядок частей решает, что поверх чего";
                case "dialog" -> "Реплика внизу экрана (say). «Реплика · Плашка целиком» — как в игре: подстраивается под текст, в редакторе показан пример. Хотите разобрать её на части — уберите плашку и добавьте картинку, текст и «Реплика · Портрет»: {name} — кто говорит, {text} — реплика, {hint} — подсказка «▸ F8», пока сюжет ждёт; у текста есть печать по буквам. История чата настраивается отдельно";
                case "inventory" -> "Окно инвентаря, когда включён «Свой инвентарь». Часть «craft_window» объединяет сетку крафта, стрелку и результат: задайте ей картинку фона и 9-slice. Живые части: «Инвентарь игрока» с настоящими ячейками и «Говорящий» — модель игрока. Закрытые ячейки показывают замок; его текстура и какие ячейки закрыты — на странице «Системы и время»";
                default -> "";
            };
        }
    }

    public static String icon(String role) {
        return role;
    }

    public static String build(String role, EngineConfig cfg) {
        AddonClient.Window addon = AddonClient.windowOf(role);
        if (addon != null) {
            return addon.standard().get().toJson();
        } else if (MenuRuntime.isMenu(role)) {
            return MenuRuntime.build(role);
        } else {
            GuiDoc d = switch (role) {
                case "choice" -> choice(cfg);
                case "toast" -> toast(cfg);
                case "zone" -> zone(cfg);
                case "chatlog" -> chatlog(cfg);
                case "quest" -> journal(cfg);
                case "shop" -> shop(cfg);
                case "skills", "skills2", "skills3" -> skills(cfg);
                case "inventory" -> inventory();
                case "dialog" -> dialog(cfg);
                default -> hud(cfg);
            };
            return d.toJson();
        }
    }

    private static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    static GuiDoc.Element el(String type, String id, String anchor, int x, int y, int w, int h, String... kv) {
        GuiDoc.Element e = new GuiDoc.Element();
        e.type = type;
        e.id = id;
        e.set("anchor", anchor);
        e.set("x", String.valueOf(x));
        e.set("y", String.valueOf(y));
        e.set("w", String.valueOf(w));
        e.set("h", String.valueOf(h));

        for (int i = 0; i + 1 < kv.length; i += 2) {
            e.set(kv[i], kv[i + 1]);
        }

        return e;
    }

    private static GuiDoc doc(String title, String layout, int w, int h) {
        GuiDoc d = new GuiDoc();
        d.set("title", title);
        d.set("layout", layout);
        d.set("width", String.valueOf(w));
        d.set("height", String.valueOf(h));
        d.set("back", "#00000000");
        return d;
    }

    private static String img(EngineConfig.Skin s, String stPath) {
        return s != null && s.isSet() ? s.image : "spraute_engine:textures/" + stPath + ".png";
    }

    static HudPainter.State sample(EngineConfig cfg) {
        HudPainter.State s = new HudPainter.State();
        s.stHealth = cfg.health;
        s.hp = 14.0F;
        s.armor = 8;
        s.food = 17;
        s.air = 200;
        s.exp = 0.45F;
        s.level = 7;
        s.coins = 1250;
        s.selected = 1;
        s.hotbarSize = Math.max(1, cfg.shownHotbar());

        for (int i = 0; i < 9; i++) {
            s.locked[i] = cfg.slots && i < s.hotbarSize && cfg.cellClosed(i);
        }

        ItemStack[] items = new ItemStack[]{
            new ItemStack(Items.IRON_SWORD),
            new ItemStack(Items.BREAD, 12),
            new ItemStack(Items.TORCH, 48),
            new ItemStack(Items.OAK_PLANKS, 64),
            new ItemStack(Items.MAP),
            new ItemStack(Items.APPLE, 3),
            ItemStack.EMPTY,
            new ItemStack(Items.COMPASS),
            new ItemStack(Items.WATER_BUCKET)
        };
        System.arraycopy(items, 0, s.hotbar, 0, 9);
        return s;
    }

    private static GuiDoc hud(EngineConfig cfg) {
        int sw = 480;
        int sh = 270;
        HudPainter.State s = sample(cfg);
        GuiDoc doc = doc("HUD игрока", "screen", sw, sh);

        for (String key : List.of("hotbar", "exp", "health", "armor", "food", "air", "coins")) {
            if (!key.equals("coins") || cfg.currency) {
                EngineConfig.Element p = cfg.element(key);
                if (p.visible) {
                    int[] r = HudPainter.rect(cfg, key, sw, sh, s);
                    float cx = r[0] + r[2] / 2.0F;
                    float cy = r[1] + r[3] / 2.0F;
                    String h = cx < sw / 3.0F ? "left" : (cx > sw * 2 / 3.0F ? "right" : "");
                    String v = cy < sh / 3.0F ? "top" : (cy > sh * 2 / 3.0F ? "bottom" : "");
                    String anchor = v.isEmpty() && h.isEmpty() ? "center" : (v.isEmpty() ? h : (h.isEmpty() ? v : v + "_" + h));
                    float fx = h.equals("left") ? 0.0F : (h.equals("right") ? 1.0F : 0.5F);
                    float fy = v.equals("top") ? 0.0F : (v.equals("bottom") ? 1.0F : 0.5F);
                    doc.elements
                        .add(
                            el(
                                "hud_" + key,
                                key,
                                anchor,
                                Math.round(r[0] - fx * sw + fx * r[2]),
                                Math.round(r[1] - fy * sh + fy * r[3]),
                                r[2],
                                r[3],
                                "style",
                                p.style,
                                "count",
                                String.valueOf(p.count),
                                "pad",
                                String.valueOf(p.pad),
                                "direction",
                                p.fillDir,
                                "gap",
                                String.valueOf(p.gap),
                                "reverse",
                                String.valueOf(p.reverse),
                                "showText",
                                String.valueOf(p.text),
                                "textColor",
                                hex(p.textColor),
                                "back",
                                p.back.image,
                                "fill",
                                p.fill.image,
                                "frame",
                                p.frame.image,
                                "full",
                                p.full.image,
                                "half",
                                p.half.image,
                                "empty",
                                p.empty.image
                            )
                        );
                }
            }
        }

        SkillHud growth = cfg.skillHud;
        if (cfg.quests.tracker) {
            doc.elements
                .add(
                    el("hud_quests", "quests", "top_right", -6, 46, 150, 60, "titleColor", hex(cfg.quests.accentColor), "doneColor", hex(cfg.quests.doneColor))
                );
        }

        return doc;
    }

    private static GuiDoc choice(EngineConfig cfg) {
        EngineConfig.Choice c = cfg.choice;
        GuiDoc doc = doc("Меню выбора", "height", 480, 270);
        Map<String, int[]> r = ChoiceLayout.rects(c, Minecraft.getInstance().font, 480, 270, List.of("Слышал.", "Их было двенадцать.", "Расскажи подробнее…"));
        int[] p = r.get("panel");
        int[] f = r.get("portrait");
        int[] n = r.get("name");
        int[] t = r.get("text");
        int[] a = r.get("answers");
        doc.elements.add(el("image", "panel", "top_left", p[0], p[1], p[2], p[3], "stretch", "width", "image", img(c.panel, "gui/talk/fade")));
        if (!c.show.equals("none")) {
            doc.elements
                .add(el("speaker", "speaker", "top_right", f[0] + f[2] - 480, f[1], f[2], f[3], "framing", c.framing, "scale", String.valueOf(c.entityScale)));
        }

        if (c.portrait.isSet()) {
            doc.elements.add(el("image", "portrait", "top_right", f[0] + f[2] - 480, f[1], f[2], f[3], "image", c.portrait.image));
        }

        if (c.nameLine) {
            doc.elements.add(el("image", "nameplate", "top_left", n[0], n[1] + 12, 103, 3, "image", img(c.nameplate, "gui/talk/divider")));
        }

        doc.elements.add(el("text", "name", "top_left", n[0], n[1], 104, 12, "text", "{name}", "align", "center", "color", hex(c.nameColor), "wrap", "false"));
        doc.elements.add(el("text", "question", "top_left", t[0], t[1], t[2], t[3], "text", "{question}", "color", hex(c.textColor), "shadow", "false"));
        boolean own = c.button.isSet();
        doc.elements
            .add(
                el(
                    "choice_answers",
                    "answers",
                    "top_left",
                    a[0],
                    a[1],
                    Math.max(a[2], 166),
                    160,
                    "style",
                    own ? "image" : "classic",
                    "image",
                    own ? c.button.image : "",
                    "hoverImage",
                    c.buttonHover.isSet() ? c.buttonHover.image : "",
                    "textColor",
                    hex(c.answerColor),
                    "hoverTextColor",
                    hex(c.answerHoverColor),
                    "textScale",
                    String.valueOf(c.answerTextScale),
                    "padding",
                    "12",
                    "minHeight",
                    "32",
                    "gap",
                    "5"
                )
            );
        return doc;
    }

    private static GuiDoc toast(EngineConfig cfg) {
        EngineConfig.Quests q = cfg.quests;
        EngineConfig.Quests.Toast t = q.toast;
        GuiDoc doc = doc("Уведомление квеста", "screen", 480, 270);
        int w = t.width;
        GuiDoc.Element g = el("group", "toast", "top", 0, t.y - 12, w, 32, "appear", "slide_down", "appearTime", "0.3");
        g.children.add(el("panel", "back", "top_left", 0, 0, w, 30, "color", hex(t.background), "borderWidth", "0", "radius", "0", "shadow", "0"));
        g.children.add(el("image", "line", "top_left", 0, 14, w, 16, "image", img(q.taskLine, "gui/journal/task_strip")));
        int tx = 6;
        if (t.icon) {
            g.children.add(el("quest_icon", "icon", "top_left", 3, 2, 21, 23, "image", q.icons.isSet() ? q.icons.image : ""));
            tx = 28;
        }

        g.children
            .add(
                el(
                    "text",
                    "title",
                    "top_left",
                    tx,
                    4,
                    w - tx - 4,
                    10,
                    "text",
                    "{title}",
                    "bold",
                    "true",
                    "color",
                    hex(q.accentColor),
                    "wrap",
                    "false",
                    "overflow",
                    "ellipsis"
                )
            );
        g.children
            .add(el("text", "text", "top_left", tx, 18, w - tx - 4, 10, "text", "{text}", "color", hex(q.textColor), "wrap", "false", "overflow", "ellipsis"));
        doc.elements.add(g);
        return doc;
    }

    private static GuiDoc zone(EngineConfig cfg) {
        EngineConfig.ZoneTitle z = cfg.zoneTitle;
        GuiDoc doc = doc("Название зоны", "screen", 480, 270);
        int y = Math.round(z.y * 270.0F) - 18;
        GuiDoc.Element g = el("group", "zone", "top", Math.round((z.x - 0.5F) * 480.0F), y, 360, 44, "appear", "fade", "appearTime", "0.7");
        g.children
            .add(
                el(
                    "text",
                    "title",
                    "top",
                    0,
                    0,
                    360,
                    Math.round(10.0F * z.scale),
                    "text",
                    "{zone}",
                    "align",
                    "center",
                    "wrap",
                    "false",
                    "scale",
                    String.valueOf(z.scale),
                    "bold",
                    String.valueOf(z.bold),
                    "shadow",
                    String.valueOf(z.shadow),
                    "color",
                    hex(z.titleColor)
                )
            );
        g.children
            .add(
                el(
                    "text",
                    "subtitle",
                    "top",
                    0,
                    Math.round(10.0F * z.scale) + 4,
                    360,
                    Math.round(10.0F * z.subScale),
                    "text",
                    "{subtitle}",
                    "align",
                    "center",
                    "wrap",
                    "false",
                    "scale",
                    String.valueOf(z.subScale),
                    "shadow",
                    String.valueOf(z.shadow),
                    "color",
                    hex(z.subColor)
                )
            );
        doc.elements.add(g);
        return doc;
    }

    private static String own(EngineConfig.Skin s) {
        return s != null && s.isSet() ? s.image : "";
    }

    private static GuiDoc journal(EngineConfig cfg) {
        EngineConfig.Quests q = cfg.quests;
        GuiDoc doc = doc("Журнал квестов", "fit", 392, 230);
        doc.set("back", hex(q.shadeColor));
        int[] ol = q.off("list");
        int[] oc = q.off("card");
        GuiDoc.Element list = el("group", "left", "top_left", ol[0], ol[1], 144, 230, "appear", "slide_down", "appearTime", "0.4");
        list.children.add(el("image", "list_back", "top_left", 0, 0, 144, 230, "image", img(q.list, "gui/journal/list")));
        list.children
            .add(
                el(
                    "journal_tabs",
                    "tabs",
                    "top_left",
                    7,
                    5,
                    75,
                    23,
                    "gap",
                    "3",
                    "current",
                    own(q.tabCurrent),
                    "done",
                    own(q.tabDone),
                    "failed",
                    own(q.tabFailed)
                )
            );
        list.children
            .add(
                el(
                    "text",
                    "tab_name",
                    "top_left",
                    88,
                    13,
                    54,
                    8,
                    "text",
                    "{tab} · {count}",
                    "scale",
                    "0.75",
                    "wrap",
                    "false",
                    "shadow",
                    "false",
                    "color",
                    hex(q.mutedColor)
                )
            );
        list.children
            .add(
                el(
                    "journal_list",
                    "list",
                    "top_left",
                    4,
                    31,
                    136,
                    195,
                    "entryHeight",
                    "35",
                    "gap",
                    "2",
                    "padding",
                    "2",
                    "textColor",
                    hex(q.textColor),
                    "mutedColor",
                    hex(q.mutedColor),
                    "accentColor",
                    hex(q.accentColor),
                    "button",
                    own(q.button),
                    "buttonHover",
                    own(q.buttonHover),
                    "buttonOn",
                    own(q.buttonOn),
                    "iconSheet",
                    own(q.icons)
                )
            );
        doc.elements.add(list);
        GuiDoc.Element card = el("group", "right", "top_left", 150 + oc[0], 2 + oc[1], 240, 228, "appear", "slide_down", "appearTime", "0.4");
        card.children.add(el("image", "card_back", "top_left", 0, 0, 240, 225, "image", img(q.card, "gui/journal/card")));
        card.children
            .add(
                el(
                    "journal_title",
                    "title",
                    "top_left",
                    10,
                    7,
                    222,
                    30,
                    "textColor",
                    hex(q.textColor),
                    "accentColor",
                    hex(q.accentColor),
                    "hoverColor",
                    hex(q.headerColor),
                    "mutedColor",
                    hex(q.mutedColor)
                )
            );
        card.children.add(el("image", "divider", "top_left", 10, 37, 129, 2, "image", img(q.divider, "gui/journal/divider")));
        card.children
            .add(
                el(
                    "journal_page",
                    "page",
                    "top_left",
                    6,
                    42,
                    232,
                    163,
                    "textColor",
                    hex(q.textColor),
                    "headerColor",
                    hex(q.headerColor),
                    "accentColor",
                    hex(q.accentColor),
                    "doneColor",
                    hex(q.doneColor),
                    "failColor",
                    hex(q.failColor),
                    "taskLine",
                    own(q.taskLine)
                )
            );
        card.children.add(el("journal_state", "state", "top_left", 102, 211, 130, 10, "doneColor", hex(q.doneColor), "failColor", hex(q.failColor)));
        card.children
            .add(el("text", "close", "top_left", 5, 227, 200, 5, "text", "{close}", "scale", "0.5", "wrap", "false", "shadow", "false", "color", "#80FFFFFF"));
        doc.elements.add(card);
        return doc;
    }

    private static GuiDoc skills(EngineConfig cfg) {
        SkillLook l = cfg.skills;
        GuiDoc doc = doc("Прокачка", "height", 480, 270);
        doc.set("back", "#90000000");
        doc.elements
            .add(el("panel", "fon", "top_left", 0, 0, 480, 270, "stretch", "both", "color", hex(l.panel), "borderWidth", "0", "radius", "0", "shadow", "0"));
        doc.elements
            .add(el("panel", "shapka", "top_left", 0, 0, 480, 34, "stretch", "width", "color", hex(l.header), "borderWidth", "0", "radius", "0", "shadow", "0"));
        doc.elements
            .add(
                el(
                    "text",
                    "zagolovok",
                    "top_left",
                    10,
                    6,
                    240,
                    10,
                    "text",
                    "{title}",
                    "wrap",
                    "false",
                    "color",
                    String.format("#FF%06X", l.accent),
                    "shadow",
                    "false"
                )
            );
        doc.elements.add(el("text", "uroven", "top_left", 10, 18, 80, 10, "text", "Уровень {level}", "wrap", "false", "color", hex(l.text), "shadow", "false"));
        doc.elements.add(el("skill_xp", "opyt", "top_left", 78, 20, 130, 5));
        doc.elements
            .add(el("text", "opyt_chisla", "top_left", 214, 18, 100, 10, "text", "{xp} / {xpnext}", "wrap", "false", "color", hex(l.muted), "shadow", "false"));
        doc.elements.add(el("skill_points", "ochki", "top_right", -8, 9, 120, 16));
        doc.elements.add(el("skill_tabs", "vkladki", "top_left", 4, 34, 296, 18));
        doc.elements.add(el("skill_field", "navyki", "top_left", 0, 52, 300, 218));
        doc.elements.add(el("skill_info", "opisanie", "top_right", -8, 42, 164, 150));
        doc.elements.add(el("skill_learn", "izuchit", "bottom_right", -8, -52, 164, 18));
        doc.elements.add(el("skill_use", "primenit", "bottom_right", -8, -30, 164, 18));
        doc.elements.add(el("skill_reset", "sbros", "bottom_right", -8, -8, 164, 18));
        return doc;
    }

    private static GuiDoc shop(EngineConfig cfg) {
        EngineConfig.Shop s = cfg.shop;
        GuiDoc doc = doc("Магазин", "fit", 445, 262);
        doc.set("back", "#A6191513");
        int[] ow = s.offsets.getOrDefault("window", new int[]{0, 0});
        int[] ol = s.offsets.getOrDefault("list", new int[]{0, 0});
        int[] oc = s.offsets.getOrDefault("card", new int[]{0, 0});
        int[] ot = s.offsets.getOrDefault("title", new int[]{0, 0});
        int[] om = s.offsets.getOrDefault("money", new int[]{0, 0});
        GuiDoc.Element list = el("group", "goods_side", "top_left", ow[0] + ol[0], ow[1] + ol[1] + 25, 254, 212, "appear", "slide_down", "appearTime", "0.5");
        list.children.add(el("image", "list_back", "top_left", 0, 0, 254, 212, "image", img(s.panel, "gui/store/list")));
        list.children
            .add(
                el(
                    "shop_goods",
                    "goods",
                    "top_left",
                    10,
                    10,
                    226,
                    192,
                    "columns",
                    "5",
                    "cellWidth",
                    "40",
                    "cellHeight",
                    "57",
                    "gapX",
                    "5",
                    "gapY",
                    "6",
                    "padX",
                    "3",
                    "padY",
                    "3",
                    "priceColor",
                    "#FFE2D7C4",
                    "sellColor",
                    hex(s.sellColor),
                    "mutedColor",
                    hex(s.mutedColor),
                    "cell",
                    own(s.cell),
                    "cellOn",
                    own(s.cellOn),
                    "locked",
                    own(s.locked),
                    "sold",
                    own(s.sold)
                )
            );
        list.children.add(el("shop_slider", "slider", "top_left", 239, 10, 9, 192, "image", own(s.slider)));
        doc.elements.add(list);
        doc.elements
            .add(
                el(
                    "text",
                    "title",
                    "top_left",
                    ow[0] + ot[0] + 16,
                    ow[1] + ot[1] + 11,
                    200,
                    10,
                    "text",
                    "{title}",
                    "wrap",
                    "false",
                    "color",
                    hex(s.titleColor),
                    "shadow",
                    "true",
                    "appear",
                    "slide_down",
                    "appearTime",
                    "0.5"
                )
            );
        doc.elements
            .add(
                el(
                    "shop_money",
                    "money",
                    "top_left",
                    ow[0] + om[0] + 148,
                    ow[1] + om[1] + 6,
                    90,
                    13,
                    "textColor",
                    hex(s.priceColor),
                    "appear",
                    "slide_down",
                    "appearTime",
                    "0.5"
                )
            );
        GuiDoc.Element card = el("group", "card", "top_left", ow[0] + oc[0] + 269, ow[1] + oc[1], 176, 262, "appear", "slide_down", "appearTime", "0.5");
        card.children.add(el("image", "card_back", "top_left", 0, 0, 176, 262, "image", img(s.detail, "gui/store/card")));
        card.children.add(el("shop_item", "item", "top_left", 72, 55, 40, 32));
        card.children.add(el("shop_info", "info", "top_left", 27, 118, 145, 76, "priceColor", hex(s.priceColor), "sellColor", hex(s.sellColor)));
        card.children
            .add(
                el(
                    "shop_arrows",
                    "arrows",
                    "top_left",
                    48,
                    102,
                    82,
                    15,
                    "minus5",
                    own(s.arrowMinus5),
                    "minus1",
                    own(s.arrowMinus1),
                    "plus1",
                    own(s.arrowPlus1),
                    "plus5",
                    own(s.arrowPlus5),
                    "minus5Hover",
                    own(s.arrowMinus5Hover),
                    "minus1Hover",
                    own(s.arrowMinus1Hover),
                    "plus1Hover",
                    own(s.arrowPlus1Hover),
                    "plus5Hover",
                    own(s.arrowPlus5Hover)
                )
            );
        card.children
            .add(
                el(
                    "shop_deal",
                    "deal",
                    "top_left",
                    44,
                    195,
                    88,
                    14,
                    "image",
                    own(s.dealButton.isSet() ? s.dealButton : s.button),
                    "hoverImage",
                    own(s.buttonHover)
                )
            );
        card.children.add(el("shop_desc", "description", "top_left", 35, 220, 106, 20, "textColor", hex(s.mutedColor)));
        doc.elements.add(card);
        return doc;
    }

    private static GuiDoc dialog(EngineConfig cfg) {
        GuiDoc doc = doc("Реплики", "screen", 480, 270);
        GuiDoc.Element plate = el("speech_plate", "plate", "bottom", 0, 0, 288, 100);
        PlateLayout.fill(plate, cfg.chatLog);
        doc.elements.add(plate);
        return doc;
    }

    private static GuiDoc inventory() {
        GuiDoc doc = doc("Инвентарь", "fit", 196, 180);
        doc.set("background", "#A6191513");
        doc.elements.add(el("panel", "back", "top_left", 0, 0, 196, 180, "color", "#E0202530", "borderWidth", "0", "radius", "4", "shadow", "0"));
        doc.elements.add(el("speaker", "player", "top_left", 40, 6, 60, 84, "framing", "body", "scale", "1"));
        GuiDoc.Element craft = el("group", "craft_window", "top_left", 102, 4, 81, 44);
        craft.children.add(el("inventory", "craft", "top_left", 4, 4, 36, 36, "rows", "craft", "cell", "18"));
        craft.children.add(el("text", "craft_arrow", "top_left", 41, 18, 17, 10, "text", "→", "align", "center", "wrap", "false"));
        craft.children.add(el("inventory", "craft_result", "top_left", 59, 13, 18, 18, "rows", "craft_result", "cell", "18"));
        doc.elements.add(craft);
        doc.elements.add(el("inventory", "armor", "top_left", 17, 8, 18, 72, "rows", "armor", "cell", "18"));
        doc.elements.add(el("inventory", "offhand", "top_left", 161, 62, 18, 18, "rows", "offhand", "cell", "18"));
        doc.elements.add(el("inventory", "cells", "top_left", 17, 94, 162, 76, "rows", "all", "cell", "18"));
        return doc;
    }

    private static GuiDoc chatlog(EngineConfig cfg) {
        GuiDoc doc = doc("История чата", "fit", 480, 270);
        int w = Math.round(298.17392F);
        doc.elements.add(el("image", "back", "center", 0, 0, w, 270, "image", img(cfg.chatLog.back, "gui/log/back")));
        GuiDoc.Element frame = el("image", "frame", "center", 0, 0, w, 270, "image", img(cfg.chatLog.frame, "gui/log/frame"));
        GuiDoc.Element lines = el("chat_lines", "lines", "center", 0, 0, 226, 246);
        doc.elements.add(cfg.chatLog.frameOnTop() ? lines : frame);
        doc.elements.add(cfg.chatLog.frameOnTop() ? frame : lines);
        return doc;
    }
}
