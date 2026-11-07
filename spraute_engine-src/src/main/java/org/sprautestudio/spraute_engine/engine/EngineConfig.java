package org.sprautestudio.spraute_engine.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.skills.SkillHud;
import org.sprautestudio.spraute_engine.skills.SkillLook;

public final class EngineConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean health;
    public boolean currency;
    public String coinItem = "";
    public boolean time;
    public boolean inventory;
    public boolean slots;
    public int hotbarSize = 18;
    public int openCells = -1;
    public String slotOrder = "top";
    public String closedCells = "";
    private transient String closedParsed;
    private transient long closedBits;
    public String offhand = "auto";
    public EngineConfig.Skin lock = new EngineConfig.Skin();
    public boolean lockShade = true;
    public boolean slotsHide;
    public boolean chat;
    public String project = "";
    public Map<String, EngineConfig.Element> hud = new LinkedHashMap<>();
    public EngineConfig.ChatLog chatLog = new EngineConfig.ChatLog();
    public EngineConfig.Choice choice = new EngineConfig.Choice();
    public EngineConfig.Clock clock = new EngineConfig.Clock();
    public EngineConfig.Shop shop = new EngineConfig.Shop();
    public EngineConfig.ZoneTitle zoneTitle = new EngineConfig.ZoneTitle();
    public EngineConfig.Quests quests = new EngineConfig.Quests();
    public EngineConfig.Upload upload = new EngineConfig.Upload();
    public EngineConfig.Bow bow = new EngineConfig.Bow();
    public EngineConfig.Nav nav = new EngineConfig.Nav();
    public EngineConfig.Body body = new EngineConfig.Body();
    public EngineConfig.CoinText coinText = new EngineConfig.CoinText();
    public SkillLook skills = new SkillLook();
    public SkillHud skillHud = new SkillHud();
    public String hudLayout = "";
    public String hudJson = "";
    public static final List<String> WINDOWS = new ArrayList<>(
        List.of("hud", "choice", "toast", "zone", "chatlog", "quest", "shop", "inventory", "dialog", "skills", "skills2", "skills3")
    );
    private static final Map<String, String> ADDED_WINDOWS = new HashMap<>();
    public Map<String, String> layouts = new LinkedHashMap<>();
    public Map<String, String> layoutJson = new LinkedHashMap<>();
    public static final List<String> ELEMENTS = List.of("hotbar", "exp", "health", "armor", "food", "air", "coins", "nav");

    public static void addWindow(String role, String title) {
        WINDOWS.add(role);
        ADDED_WINDOWS.put(role, title);
    }

    public static String windowTitle(String role) {
        return switch (role) {
            case "hud" -> "HUD игрока";
            case "choice" -> "Меню выбора";
            case "toast" -> "Уведомление квеста";
            case "zone" -> "Название зоны";
            case "chatlog" -> "История чата";
            case "quest" -> "Журнал квестов";
            case "shop" -> "Магазин";
            case "skills" -> "Прокачка, макет 1";
            case "skills2" -> "Прокачка, макет 2";
            case "skills3" -> "Прокачка, макет 3";
            case "inventory" -> "Инвентарь";
            case "dialog" -> "Реплики";
            default -> (String)ADDED_WINDOWS.getOrDefault(role, role);
        };
    }

    public boolean cellClosed(int cell) {
        if (cell == 40 && this.offhand.equals("off")) {
            return true;
        } else {
            return !"list".equals(this.slotOrder)
                ? SlotPlan.closedByCount(cell, this.openCells, this.slotOrder)
                : cell >= 0 && (this.listed() >> cell & 1L) != 0L;
        }
    }

    private long listed() {
        String text = this.closedCells;
        if (!text.equals(this.closedParsed)) {
            long bits = 0L;

            try {
                if (!text.isBlank()) {
                    bits = SlotPlan.parse(text);
                }
            } catch (IllegalArgumentException var5) {
            }

            this.closedBits = bits;
            this.closedParsed = text;
        }

        return this.closedBits;
    }

    public int shownHotbar() {
        if (this.slots && this.slotsHide && !"list".equals(this.slotOrder)) {
            int open = 0;

            while (open < 9 && !this.cellClosed(open)) {
                open++;
            }

            return open;
        } else {
            return 9;
        }
    }

    public String layout(String role) {
        String json = this.layoutJson.get(role);
        return json != null && !json.isBlank() ? json : null;
    }

    public static String title(String key) {
        return switch (key) {
            case "health" -> "Здоровье";
            case "armor" -> "Броня";
            case "food" -> "Голод";
            case "air" -> "Воздух";
            case "exp" -> "Опыт";
            case "hotbar" -> "Хотбар (9 слотов)";
            case "coins" -> "Монеты";
            case "nav" -> "Навигатор";
            default -> key;
        };
    }

    public static List<String> styles(String key) {
        return switch (key) {
            case "hotbar" -> List.of("default", "classic", "slots");
            case "exp" -> List.of("default", "bar", "orb");
            case "coins" -> List.of("default", "icon");
            case "nav" -> List.of("default");
            default -> List.of("default", "bar", "icons", "orb");
        };
    }

    public static String styleTitle(String style) {
        return switch (style) {
            case "bar" -> "Полоска (фон, заливка, рамка)";
            case "icons" -> "Ряд иконок (полная, половина, пустая)";
            case "slots" -> "Свои слоты (свои текстуры)";
            case "classic" -> "Хотбар Spraute";
            case "icon" -> "Картинка и число";
            case "orb" -> "Шар с жидкостью (заливается по кругу)";
            default -> "Стандартный";
        };
    }

    public static boolean gameDrawn(String style) {
        return style.equals("default") || style.equals("classic");
    }

    public EngineConfig.Element element(String key) {
        EngineConfig.Element e = this.hud.get(key);
        if (e == null) {
            e = defaults(key);
            this.hud.put(key, e);
        }

        return e;
    }

    public static EngineConfig.Element defaults(String key) {
        EngineConfig.Element e = new EngineConfig.Element();
        switch (key) {
            case "health":
                e.width = 81;
                e.height = 9;
                e.text = false;
                break;
            case "food":
            case "air":
                e.reverse = true;
                break;
            case "nav":
                e.anchor = "T";
                e.width = 160;
                e.height = 14;
                break;
            case "exp":
                e.width = 182;
                e.height = 5;
                e.text = true;
                e.textColor = -8323296;
                break;
            case "hotbar":
                e.width = 182;
                e.height = 22;
                e.count = 9;
                e.anchor = "B";
                break;
            case "coins":
                e.width = 16;
                e.height = 16;
                e.anchor = "TR";
        }

        return e;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static EngineConfig fromJson(String json) {
        EngineConfig c = null;

        try {
            c = GSON.fromJson(json, EngineConfig.class);
        } catch (Exception var3) {
        }

        if (c == null) {
            c = new EngineConfig();
        }

        c.fix();
        return c;
    }

    public EngineConfig copy() {
        return fromJson(this.toJson());
    }

    private void fix() {
        if (this.project == null) {
            this.project = "";
        }

        if (this.coinItem == null) {
            this.coinItem = "";
        }

        if (this.skills == null) {
            this.skills = new SkillLook();
        }

        this.skills.fix();
        if (this.skillHud == null) {
            this.skillHud = new SkillHud();
        }

        this.skillHud.fix();
        if (this.hud == null) {
            this.hud = new LinkedHashMap<>();
        }

        for (String k : ELEMENTS) {
            EngineConfig.Element e = this.hud.get(k);
            if (e == null) {
                this.hud.put(k, defaults(k));
            } else {
                e.fix();
            }
        }

        if (this.chatLog == null) {
            this.chatLog = new EngineConfig.ChatLog();
        }

        if (this.chatLog.back == null) {
            this.chatLog.back = new EngineConfig.Skin();
        }

        if (this.chatLog.frame == null) {
            this.chatLog.frame = new EngineConfig.Skin();
        }

        if (this.choice == null) {
            this.choice = new EngineConfig.Choice();
        }

        if (this.clock == null) {
            this.clock = new EngineConfig.Clock();
        }

        if (this.shop == null) {
            this.shop = new EngineConfig.Shop();
        }

        if (this.zoneTitle == null) {
            this.zoneTitle = new EngineConfig.ZoneTitle();
        }

        if (this.quests == null) {
            this.quests = new EngineConfig.Quests();
        }

        if (this.hudLayout == null) {
            this.hudLayout = "";
        }

        if (this.hudJson == null) {
            this.hudJson = "";
        }

        if (this.layouts == null) {
            this.layouts = new LinkedHashMap<>();
        }

        if (this.layoutJson == null) {
            this.layoutJson = new LinkedHashMap<>();
        }

        if (!this.hudLayout.isEmpty() && !this.layouts.containsKey("hud")) {
            this.layouts.put("hud", this.hudLayout);
            this.layoutJson.put("hud", this.hudJson);
        }

        this.hudLayout = "";
        this.hudJson = "";
        this.quests.fix();
        this.zoneTitle.fix();
        if (this.openCells < 0) {
            this.openCells = this.hotbarSize;
        }

        this.openCells = Math.max(0, Math.min(36, this.openCells));
        if (this.slotOrder == null || !SlotPlan.ORDERS.contains(this.slotOrder)) {
            this.slotOrder = "top";
        }

        if (this.closedCells == null) {
            this.closedCells = "";
        }

        if (this.offhand == null) {
            this.offhand = "auto";
        }

        if (this.lock == null) {
            this.lock = new EngineConfig.Skin();
        }

        this.shop.fix();
        this.clock.speed = Math.max(0.05F, Math.min(50.0F, this.clock.speed));
        this.choice.fix();
    }

    public static class Body {
        public boolean hunger = true;
        public boolean drowning = true;
        public boolean fallDamage = true;
        public boolean exp = true;
        public String regen = "game";
        public float regenAmount = 1.0F;
        public float regenEvery = 3.0F;
        public float regenDelay = 5.0F;
        public int regenFood = 0;
    }

    public static class Bow {
        public boolean enabled = true;
        public String draw = "entity/humanoid!bow_draw_right";
        public String release = "entity/humanoid!bow_release_right";
        public String drawLeft = "entity/humanoid!bow_draw";
        public String releaseLeft = "entity/humanoid!bow_release";
        public int fullDraw = 20;
    }

    public static final class ChatLog {
        public EngineConfig.Skin back = new EngineConfig.Skin();
        public EngineConfig.Skin frame = new EngineConfig.Skin();
        public boolean frameOver = false;
        public int nameColor = -1;
        public boolean f8Hint = true;
        public String speechPlace = "bottom";
        public int speechShift = 0;
        public String speechAlign = "center";
        public int speechShiftX = 0;
        public boolean speechIconShown = true;
        public int speechIconSize = 20;
        public int speechIconX = 0;
        public int speechIconY = 0;
        public int speechTextX = 0;
        public int speechTextY = 0;
        public int speechWidth = 60;
        public int speechBack = Integer.MIN_VALUE;
        public int speechText = -1;
        public boolean speechCustom;
        public float speechSeconds = 10.0F;
        public String speechSound = "";
        public float playerHeadScale = 1.0F;
        public int playerHeadX = 0;
        public int playerHeadY = 0;

        public boolean frameOnTop() {
            return !this.frame.isSet() || this.frameOver;
        }

        public boolean custom() {
            return this.back.isSet() || this.frame.isSet();
        }
    }

    public static final class Choice {
        public String style = "spraute";
        public String show = "none";
        public String mob = "minecraft:villager";
        public String framing = "body";
        public boolean camera = true;
        public EngineConfig.Skin panel = new EngineConfig.Skin();
        public EngineConfig.Skin portrait = new EngineConfig.Skin();
        public EngineConfig.Skin button = new EngineConfig.Skin();
        public EngineConfig.Skin buttonHover = new EngineConfig.Skin();
        public EngineConfig.Skin nameplate = new EngineConfig.Skin();
        public boolean nameLine = true;
        public int textColor = -12400641;
        public int nameColor = -12400641;
        public int answerColor = -12400641;
        public int answerHoverColor = -12400641;
        public Map<String, int[]> offsets = new LinkedHashMap<>();
        public float entityScale = 1.0F;
        public int entityX = 0;
        public int entityY = 0;
        public int cropLeft = 4;
        public int cropRight = 4;
        public int cropTop = 4;
        public int cropBottom = 4;
        public float uiScale = 1.0F;
        public String answerWidth = "fixed";
        public int answerMinW = 90;
        public int answerMaxW = 260;
        public float answerTextScale = 1.0F;

        void fix() {
            if (this.style == null) {
                this.style = "spraute";
            }

            if (this.entityScale <= 0.0F) {
                this.entityScale = 1.0F;
            }

            if (this.uiScale <= 0.0F) {
                this.uiScale = 1.0F;
            }

            if (this.answerWidth == null) {
                this.answerWidth = "fixed";
            }

            if (this.answerTextScale <= 0.0F) {
                this.answerTextScale = 1.0F;
            }

            this.answerMinW = Math.max(40, this.answerMinW);
            this.answerMaxW = Math.max(this.answerMinW, this.answerMaxW);
            this.show = "none";
            if (this.mob == null) {
                this.mob = "minecraft:villager";
            }

            if (this.framing == null) {
                this.framing = "body";
            }

            if (this.panel == null) {
                this.panel = new EngineConfig.Skin();
            }

            if (this.portrait == null) {
                this.portrait = new EngineConfig.Skin();
            }

            if (this.nameplate == null) {
                this.nameplate = new EngineConfig.Skin();
            }

            if (this.button == null) {
                this.button = new EngineConfig.Skin();
            }

            if (this.buttonHover == null) {
                this.buttonHover = new EngineConfig.Skin();
            }

            if (this.offsets == null) {
                this.offsets = new LinkedHashMap<>();
            }

            if (this.textColor == -1 && this.nameColor == -11654 && this.answerColor == -1 && this.answerHoverColor == -8054) {
                this.textColor = this.nameColor = this.answerColor = this.answerHoverColor = -12400641;
            }
        }
    }

    public static final class Clock {
        public boolean manage = true;
        public boolean cycle = true;
        public float speed = 1.0F;
        public int fixed = -1;
    }

    public static class CoinText {
        public String suffix = "";
        public boolean shorten = true;
        public int color = -1;
        public int lackColor = -43691;
    }

    public static final class Element {
        public boolean visible = true;
        public boolean customPos;
        public String anchor = "BL";
        public int x;
        public int y;
        public boolean percent;
        public float px;
        public float py;
        public int width = 81;
        public int height = 9;
        public String style = "default";
        public EngineConfig.Skin back = new EngineConfig.Skin();
        public EngineConfig.Skin fill = new EngineConfig.Skin();
        public EngineConfig.Skin frame = new EngineConfig.Skin();
        public EngineConfig.Skin full = new EngineConfig.Skin();
        public EngineConfig.Skin half = new EngineConfig.Skin();
        public EngineConfig.Skin empty = new EngineConfig.Skin();
        public int count = 10;
        public int pad = 1;
        public int gap = 0;
        public String fillDir = "right";
        public boolean reverse;
        public boolean text;
        public int textColor = -1;
        public int fillColor;
        public int backColor;
        public boolean waves = true;

        void fix() {
            if (this.back == null) {
                this.back = new EngineConfig.Skin();
            }

            if (this.fill == null) {
                this.fill = new EngineConfig.Skin();
            }

            if (this.frame == null) {
                this.frame = new EngineConfig.Skin();
            }

            if (this.full == null) {
                this.full = new EngineConfig.Skin();
            }

            if (this.half == null) {
                this.half = new EngineConfig.Skin();
            }

            if (this.empty == null) {
                this.empty = new EngineConfig.Skin();
            }

            if (this.anchor == null) {
                this.anchor = "BL";
            }

            if (this.style == null) {
                this.style = "default";
            }
        }
    }

    public static class Nav {
        public String place = "top";
        public int offset = 18;
        public int arrowColor = -12224;
        public int textColor = -1;
        public int back = Integer.MIN_VALUE;
        public boolean distance = true;
        public String unit = " м";
        public String arrow = "▲";
        public int max = 3;
        public float scale = 1.0F;
    }

    public static final class Quests {
        public float scale = 1.0F;
        public EngineConfig.Skin list = new EngineConfig.Skin();
        public EngineConfig.Skin card = new EngineConfig.Skin();
        public EngineConfig.Skin button = new EngineConfig.Skin();
        public EngineConfig.Skin buttonHover = new EngineConfig.Skin();
        public EngineConfig.Skin buttonOn = new EngineConfig.Skin();
        public EngineConfig.Skin taskLine = new EngineConfig.Skin();
        public EngineConfig.Skin tabCurrent = new EngineConfig.Skin();
        public EngineConfig.Skin tabDone = new EngineConfig.Skin();
        public EngineConfig.Skin tabFailed = new EngineConfig.Skin();
        public EngineConfig.Skin icons = new EngineConfig.Skin();
        public EngineConfig.Skin divider = new EngineConfig.Skin();
        public int textColor = -1;
        public int mutedColor = -6642508;
        public int accentColor = -11686;
        public int doneColor = -8658806;
        public int failColor = -38037;
        public int headerColor = -9518081;
        public int shadeColor = -1508305645;
        public Map<String, int[]> offsets = new LinkedHashMap<>();
        public boolean tracker = true;
        public EngineConfig.Quests.Toast toast = new EngineConfig.Quests.Toast();

        void fix() {
            if (this.scale <= 0.0F) {
                this.scale = 1.0F;
            }

            if (this.list == null) {
                this.list = new EngineConfig.Skin();
            }

            if (this.card == null) {
                this.card = new EngineConfig.Skin();
            }

            if (this.button == null) {
                this.button = new EngineConfig.Skin();
            }

            if (this.buttonHover == null) {
                this.buttonHover = new EngineConfig.Skin();
            }

            if (this.buttonOn == null) {
                this.buttonOn = new EngineConfig.Skin();
            }

            if (this.taskLine == null) {
                this.taskLine = new EngineConfig.Skin();
            }

            if (this.tabCurrent == null) {
                this.tabCurrent = new EngineConfig.Skin();
            }

            if (this.tabDone == null) {
                this.tabDone = new EngineConfig.Skin();
            }

            if (this.tabFailed == null) {
                this.tabFailed = new EngineConfig.Skin();
            }

            if (this.icons == null) {
                this.icons = new EngineConfig.Skin();
            }

            if (this.divider == null) {
                this.divider = new EngineConfig.Skin();
            }

            if (this.offsets == null) {
                this.offsets = new LinkedHashMap<>();
            }

            if (this.toast == null) {
                this.toast = new EngineConfig.Quests.Toast();
            }

            if (this.toast.seconds <= 0.0F) {
                this.toast.seconds = 4.0F;
            }

            if (this.toast.scale <= 0.0F) {
                this.toast.scale = 1.0F;
            }

            if (this.toast.width < 80) {
                this.toast.width = 225;
            }

            if (this.toast.startTitle == null) {
                this.toast.startTitle = "";
            }

            if (this.toast.taskTitle == null) {
                this.toast.taskTitle = "";
            }

            if (this.toast.revealTitle == null) {
                this.toast.revealTitle = "";
            }

            if (this.toast.completeTitle == null) {
                this.toast.completeTitle = "";
            }

            if (this.toast.failTitle == null) {
                this.toast.failTitle = "";
            }
        }

        public int[] off(String part) {
            int[] o = this.offsets.get(part);
            return o != null && o.length == 2 ? o : new int[]{0, 0};
        }

        public static final class Toast {
            public boolean enabled = true;
            public boolean onStart = true;
            public boolean onTask = true;
            public boolean onReveal = true;
            public boolean onComplete = true;
            public boolean onFail = true;
            public String startTitle = "Новый квест";
            public String taskTitle = "Задача выполнена";
            public String revealTitle = "Новая задача";
            public String completeTitle = "Квест выполнен";
            public String failTitle = "Квест провален";
            public float seconds = 4.0F;
            public float scale = 1.0F;
            public int y = 24;
            public int width = 225;
            public int background = -1341452522;
            public boolean icon = true;
            public boolean sound = true;
        }
    }

    public static final class Shop {
        public EngineConfig.Skin panel = new EngineConfig.Skin();
        public EngineConfig.Skin tab = new EngineConfig.Skin();
        public EngineConfig.Skin tabOn = new EngineConfig.Skin();
        public EngineConfig.Skin cell = new EngineConfig.Skin();
        public EngineConfig.Skin cellOn = new EngineConfig.Skin();
        public EngineConfig.Skin detail = new EngineConfig.Skin();
        public EngineConfig.Skin button = new EngineConfig.Skin();
        public EngineConfig.Skin buttonHover = new EngineConfig.Skin();
        public EngineConfig.Skin dealButton = new EngineConfig.Skin();
        public EngineConfig.Skin locked = new EngineConfig.Skin();
        public EngineConfig.Skin sold = new EngineConfig.Skin();
        public EngineConfig.Skin slider = new EngineConfig.Skin();
        public EngineConfig.Skin arrowMinus5 = new EngineConfig.Skin();
        public EngineConfig.Skin arrowMinus1 = new EngineConfig.Skin();
        public EngineConfig.Skin arrowPlus1 = new EngineConfig.Skin();
        public EngineConfig.Skin arrowPlus5 = new EngineConfig.Skin();
        public EngineConfig.Skin arrowMinus5Hover = new EngineConfig.Skin();
        public EngineConfig.Skin arrowMinus1Hover = new EngineConfig.Skin();
        public EngineConfig.Skin arrowPlus1Hover = new EngineConfig.Skin();
        public EngineConfig.Skin arrowPlus5Hover = new EngineConfig.Skin();
        public int titleColor = -6237;
        public int textColor = -1644304;
        public int mutedColor = -7564888;
        public int priceColor = -11686;
        public int sellColor = -8658806;
        public int accentColor = -9533697;
        public Map<String, int[]> offsets = new LinkedHashMap<>();

        public EngineConfig.Skin arrow(int step, boolean hover) {
            return switch (step) {
                case -5 -> hover ? this.arrowMinus5Hover : this.arrowMinus5;
                case -1 -> hover ? this.arrowMinus1Hover : this.arrowMinus1;
                case 1 -> hover ? this.arrowPlus1Hover : this.arrowPlus1;
                default -> hover ? this.arrowPlus5Hover : this.arrowPlus5;
            };
        }

        void fix() {
            if (this.panel == null) {
                this.panel = new EngineConfig.Skin();
            }

            if (this.tab == null) {
                this.tab = new EngineConfig.Skin();
            }

            if (this.tabOn == null) {
                this.tabOn = new EngineConfig.Skin();
            }

            if (this.cell == null) {
                this.cell = new EngineConfig.Skin();
            }

            if (this.cellOn == null) {
                this.cellOn = new EngineConfig.Skin();
            }

            if (this.detail == null) {
                this.detail = new EngineConfig.Skin();
            }

            if (this.button == null) {
                this.button = new EngineConfig.Skin();
            }

            if (this.buttonHover == null) {
                this.buttonHover = new EngineConfig.Skin();
            }

            if (this.dealButton == null) {
                this.dealButton = new EngineConfig.Skin();
            }

            if (this.locked == null) {
                this.locked = new EngineConfig.Skin();
            }

            if (this.sold == null) {
                this.sold = new EngineConfig.Skin();
            }

            if (this.slider == null) {
                this.slider = new EngineConfig.Skin();
            }

            if (this.arrowMinus5 == null) {
                this.arrowMinus5 = new EngineConfig.Skin();
            }

            if (this.arrowMinus1 == null) {
                this.arrowMinus1 = new EngineConfig.Skin();
            }

            if (this.arrowPlus1 == null) {
                this.arrowPlus1 = new EngineConfig.Skin();
            }

            if (this.arrowPlus5 == null) {
                this.arrowPlus5 = new EngineConfig.Skin();
            }

            if (this.arrowMinus5Hover == null) {
                this.arrowMinus5Hover = new EngineConfig.Skin();
            }

            if (this.arrowMinus1Hover == null) {
                this.arrowMinus1Hover = new EngineConfig.Skin();
            }

            if (this.arrowPlus1Hover == null) {
                this.arrowPlus1Hover = new EngineConfig.Skin();
            }

            if (this.arrowPlus5Hover == null) {
                this.arrowPlus5Hover = new EngineConfig.Skin();
            }

            if (this.offsets == null) {
                this.offsets = new LinkedHashMap<>();
            }
        }
    }

    public static final class Skin {
        public String image = "";
        public boolean nine;
        public int left = 4;
        public int top = 4;
        public int right = 4;
        public int bottom = 4;
        public float scale = 1.0F;
        public boolean tileCenter;
        public boolean tileEdges;

        public boolean isSet() {
            return this.image != null && !this.image.isBlank();
        }
    }

    public static final class Upload {
        public boolean onSave;
        public boolean onConnect;
        public boolean restart;
    }

    public static final class ZoneTitle {
        public float x = 0.5F;
        public float y = 0.27F;
        public float scale = 2.4F;
        public float subScale = 1.1F;
        public float seconds = 4.5F;
        public int titleColor = -3623;
        public int subColor = -2564376;
        public int lineColor = -1325409888;
        public int plateColor = -1728053248;
        public boolean bold = true;
        public boolean shadow = true;
        public boolean lines = true;
        public boolean spread = true;
        public boolean plate = false;
        public int padding = 8;
        public EngineConfig.Skin plateSkin = new EngineConfig.Skin();
        public String sound = "";

        void fix() {
            if (this.plateSkin == null) {
                this.plateSkin = new EngineConfig.Skin();
            }

            if (this.sound == null) {
                this.sound = "";
            }

            this.seconds = Math.max(0.5F, this.seconds);
            this.scale = Math.max(0.3F, this.scale);
            this.subScale = Math.max(0.3F, this.subScale);
        }
    }
}
