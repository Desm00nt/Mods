package org.sprautestudio.spraute_engine.editor.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sprautestudio.spraute_engine.addon.Addons;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.ChoiceLayout;
import org.sprautestudio.spraute_engine.engine.client.ChoiceScreen;
import org.sprautestudio.spraute_engine.engine.client.HudPainter;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.quest.QuestDef;
import org.sprautestudio.spraute_engine.quest.QuestJournal;
import org.sprautestudio.spraute_engine.quest.client.QuestClient;
import org.sprautestudio.spraute_engine.quest.client.QuestScreen;
import org.sprautestudio.spraute_engine.shop.ShopView;
import org.sprautestudio.spraute_engine.shop.client.ShopPanel;
import org.sprautestudio.spraute_engine.skills.SkillHud;
import org.sprautestudio.spraute_engine.skills.SkillLook;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Checkbox;
import org.sprautestudio.spraute_engine.ui.widgets.ColorPicker;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;
import org.sprautestudio.spraute_engine.zone.client.ZoneClient;

public class EngineTab extends EditorTab {
    public static final String FILE = "engine.json";
    private static final int VW = 480;
    private static final int VH = 270;
    private static final List<String> ANCHORS = List.of("TL", "T", "TR", "L", "C", "R", "BL", "B", "BR");
    private EngineConfig cfg;
    private String savedJson;
    private EngineTab.Mode mode = EngineTab.Mode.HUD;
    private String selected = "health";
    private float sample = 0.7F;
    private final EngineTab.Form form = new EngineTab.Form();
    private final ScrollView scroll;
    private final Button apply;
    private final Button reset;
    private final Button hudBtn;
    private final Button chatBtn;
    private final Button choiceBtn;
    private final Button shopBtn;
    private final Button zoneBtn;
    private final Button questBtn;
    private int top = 28;
    private boolean dragging;
    private boolean resizing;
    private boolean moved;
    private double dragX;
    private double dragY;
    private int[] dragStart;
    private float k = 1.0F;
    private int sx;
    private int sy;
    private String page;
    public static EngineTab.SkillsPages skillsPages;
    private static final List<Integer> FIXED_TIMES = List.of(-1, 0, 1000, 6000, 12000, 13000, 18000, 23000);
    private static final List<String> CHOICE_STYLES = List.of("spraute");
    private static final List<String> CHOICE_SHOWS = List.of("speaker", "player", "mob", "none");
    private String shopPart = "list";
    private int[] shopDragStart = new int[]{0, 0};
    private final ShopPanel shopSample = new ShopPanel(ShopView.sample());
    private final QuestScreen questSample = new QuestScreen(QuestJournal.preview(QuestDef.example("Ворота деревни"), "example/gate"));
    private String questPart = "list";
    private int[] questDragStart = new int[]{0, 0};
    private float questK = 1.0F;
    private float questOX;
    private float questOY;
    private long zonePreviewAt = System.currentTimeMillis();
    private static final int LABEL_W = 104;
    private static final int FORM_W = 330;
    private boolean zoneDrag;
    private String choicePart = "answers";
    private int[] choiceDragStart;
    private static final List<String> SAMPLE_ANSWERS = List.of("Да", "Нет");

    public EngineTab(Project project, EngineConfig shared, String page) {
        this(project, shared);
        this.page = page;

        this.mode = switch (page) {
            case "quest" -> EngineTab.Mode.QUEST;
            case "shop" -> EngineTab.Mode.SHOP;
            case "speech" -> EngineTab.Mode.CHAT;
            default -> EngineTab.Mode.HUD;
        };

        for (Button b : List.of(this.apply, this.reset, this.hudBtn, this.chatBtn, this.choiceBtn, this.shopBtn, this.zoneBtn, this.questBtn)) {
            b.visible = false;
        }

        this.buildForm();
    }

    private boolean formOnly() {
        return this.page != null && (this.page.equals("systems") || this.page.equals("speech") || this.page.equals("addons") || this.page.equals("skillslook"));
    }

    public EngineTab(Project project) {
        this(project, load(project));
    }

    private EngineTab(Project project, EngineConfig loaded) {
        super(project, null);
        this.cfg = loaded;
        this.cfg.project = project.name();
        this.savedJson = this.cfg.toJson();
        this.scroll = this.add(new ScrollView(this.form));
        this.apply = this.add(new Button("Сохранить и применить", this::applyToWorld).icon(Icon.UPLOAD).style(Button.Style.PRIMARY));
        this.apply.tooltip("Ctrl+S. Настройки сохраняются в проект (engine.json) и сразу включаются в текущем мире");
        this.reset = this.add(new Button("Сбросить", () -> {
            this.cfg = EngineConfig.fromJson("{}");
            this.cfg.project = project.name();
            this.buildForm();
        }).icon(Icon.REFRESH).style(Button.Style.GHOST));
        this.reset.tooltip("Вернуть всё по умолчанию: системы выключены, HUD как в обычном Minecraft");
        this.hudBtn = this.add(new Button("HUD", () -> {
            this.mode = EngineTab.Mode.HUD;
            this.buildForm();
        }).style(Button.Style.GHOST));
        this.hudBtn.active = () -> this.mode == EngineTab.Mode.HUD;
        this.chatBtn = this.add(new Button("История чата", () -> {
            this.mode = EngineTab.Mode.CHAT;
            this.buildForm();
        }).style(Button.Style.GHOST));
        this.chatBtn.active = () -> this.mode == EngineTab.Mode.CHAT;
        this.choiceBtn = this.add(new Button("Меню выбора", () -> {
            this.mode = EngineTab.Mode.CHOICE;
            this.buildForm();
        }).style(Button.Style.GHOST));
        this.choiceBtn.active = () -> this.mode == EngineTab.Mode.CHOICE;
        this.shopBtn = this.add(new Button("Торговля", () -> {
            this.mode = EngineTab.Mode.SHOP;
            this.buildForm();
        }).style(Button.Style.GHOST));
        this.shopBtn.active = () -> this.mode == EngineTab.Mode.SHOP;
        this.zoneBtn = this.add(new Button("Название зоны", () -> {
            this.mode = EngineTab.Mode.ZONE;
            this.buildForm();
        }).style(Button.Style.GHOST));
        this.zoneBtn.active = () -> this.mode == EngineTab.Mode.ZONE;
        this.questBtn = this.add(new Button("Журнал квестов", () -> {
            this.mode = EngineTab.Mode.QUEST;
            this.buildForm();
        }).style(Button.Style.GHOST));
        this.questBtn.active = () -> this.mode == EngineTab.Mode.QUEST;

        for (Button b : List.of(this.apply, this.reset, this.hudBtn, this.chatBtn, this.choiceBtn, this.shopBtn, this.zoneBtn, this.questBtn)) {
            b.autoWidth();
        }

        this.buildForm();
    }

    public static Path file(Project project) {
        return project.dir.resolve("engine.json");
    }

    public static EngineConfig load(Project project) {
        Path f = file(project);

        try {
            if (Files.exists(f)) {
                return EngineConfig.fromJson(Files.readString(f, StandardCharsets.UTF_8));
            }
        } catch (IOException var3) {
        }

        return EngineConfig.fromJson("{}");
    }

    private void applyToWorld() {
        UI ui = UI.get();

        try {
            this.save();
        } catch (IOException var3) {
            if (ui != null) {
                ui.toastError("Не удалось сохранить: " + var3.getMessage());
            }

            return;
        }

        if (ui != null) {
            if (Minecraft.getInstance().getConnection() == null) {
                ui.toastInfo("Сохранено. В мир применится, когда вы в него войдёте и сохраните снова");
            } else {
                ui.toastSuccess("Настройки движка применены");
            }
        }
    }

    private HudPainter.State sampleState() {
        HudPainter.State s = new HudPainter.State();
        s.nav = this.cfg.nav;
        s.stHealth = this.cfg.health;
        s.hp = 20.0F * this.sample;
        s.maxHp = 20.0F;
        s.armor = Math.round(20.0F * this.sample);
        s.food = Math.round(20.0F * this.sample);
        s.air = Math.round(300.0F * this.sample);
        s.exp = this.sample;
        s.level = 7;
        s.coins = 1250;
        s.selected = 1;
        s.hotbarSize = Math.max(1, this.cfg.shownHotbar());

        for (int i = 0; i < 9; i++) {
            s.locked[i] = this.cfg.slots && i < s.hotbarSize && this.cfg.cellClosed(i);
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
        s.offhand = new ItemStack(Items.SHIELD);
        return s;
    }

    private boolean present(String key) {
        return !key.equals("coins") || this.cfg.currency;
    }

    private void buildForm() {
        this.form.clear();
        if (this.page != null && !this.page.equals("systems")) {
            String var3 = this.page;
            switch (var3) {
                case "quest":
                    this.questForm();
                    break;
                case "shop":
                    this.shopForm();
                    break;
                case "addons":
                    this.addonsForm();
                    break;
                case "skillslook":
                    this.skillsForm();
                    break;
                default:
                    this.speechForm();
            }

            this.relayout();
        } else {
            this.form.header("Системы движка");
            this.form.toggle("Своё здоровье", "Круглая полоска здоровья Spraute вместо сердечек", () -> this.cfg.health, v -> {
                this.cfg.health = v;
                this.buildForm();
            });
            this.form.toggle("Монеты (монеты)", "Счётчик монет справа вверху", () -> this.cfg.currency, v -> {
                this.cfg.currency = v;
                if (!v && this.selected.equals("coins")) {
                    this.selected = "health";
                }

                this.buildForm();
            });
            this.form
                .item(
                    "Предмет монеты",
                    "Монетами становится этот предмет, прямо в инвентаре. Не выбран — свой кошелёк и сферы на земле",
                    () -> this.cfg.coinItem,
                    v -> {
                        this.cfg.coinItem = v.trim();
                        this.cfg.currency = true;
                        this.buildForm();
                    }
                );
            if (!this.cfg.coinItem.isEmpty()) {
                this.form.action("Вернуть кошелёк и сферы", () -> {
                    this.cfg.coinItem = "";
                    this.buildForm();
                });
            }

            this.form
                .toggle(
                    "Часы мира", "Своё время суток: долгий день, заморозка, счётчик дней. Выключено — обычная смена дня и ночи", () -> this.cfg.time, v -> {
                        this.cfg.time = v;
                        this.buildForm();
                    }
                );
            this.form
                .toggle(
                    "Свой инвентарь",
                    "Модель игрока и только разрешённые ячейки вместо обычного инвентаря",
                    () -> this.cfg.inventory,
                    v -> this.cfg.inventory = v
                );
            this.form.toggle("Блокировка слотов", "Часть ячеек инвентаря закрыта замком; в творческом режиме ограничений нет", () -> this.cfg.slots, v -> {
                this.cfg.slots = v;
                this.buildForm();
            });
            if (this.cfg.slots) {
                this.form.dropdown("Как закрывать", null, () -> SlotPlan.ORDERS, () -> this.cfg.slotOrder, SlotPlan::orderTitle, v -> {
                    this.cfg.slotOrder = v;
                    this.buildForm();
                });
                if (this.cfg.slotOrder.equals("list")) {
                    this.form
                        .text(
                            "Закрытые ячейки",
                            "1–9 — хотбар, 10–36 — рюкзак: \"6-9, 10-18\". Ещё hotbar, main, armor, offhand",
                            () -> this.cfg.closedCells,
                            v -> this.cfg.closedCells = v.trim()
                        );
                } else {
                    this.form.decimal("Открытых ячеек всего", () -> this.cfg.openCells, v -> this.cfg.openCells = (int)Math.max(0.0, Math.min(36.0, v)));
                    this.form.note("Из 36: 9 в хотбаре и 27 в рюкзаке. Например, 18 — весь хотбар и 9 ячеек рюкзака");
                }

                this.form.skin("Текстура замка", this.cfg.lock);
                this.form
                    .toggle(
                        "Затемнять под замком",
                        "Закрытая ячейка темнеет под вашей текстурой. Стандартный замок темнит всегда",
                        () -> this.cfg.lockShade,
                        v -> this.cfg.lockShade = v
                    );
                this.form
                    .toggle(
                        "Скрывать закрытые",
                        "Закрытые ячейки отрезаются от хотбара, а не показывают замок",
                        () -> this.cfg.slotsHide,
                        v -> this.cfg.slotsHide = v
                    );
            }

            this.form
                .toggle(
                    "Скрывать чат",
                    "Сообщения игроков не показываются в чате. Сюжет всё равно их получает: on chat",
                    () -> this.cfg.chat,
                    v -> this.cfg.chat = v
                );
            this.form.header("Тело игрока");
            this.form.toggle("Голод", "Выключено — игрок не голодает, еда всегда полная", () -> this.cfg.body.hunger, v -> this.cfg.body.hunger = v);
            this.form.toggle("Утопление", "Выключено — под водой воздух не кончается", () -> this.cfg.body.drowning, v -> this.cfg.body.drowning = v);
            this.form.toggle("Урон от падения", null, () -> this.cfg.body.fallDamage, v -> this.cfg.body.fallDamage = v);
            this.form.toggle("Опыт", "Выключено — опыт не копится, уровень всегда 0", () -> this.cfg.body.exp, v -> this.cfg.body.exp = v);
            this.form.dropdown("Лечение", "Как здоровье возвращается само", () -> List.of("game", "off", "engine"), () -> this.cfg.body.regen, v -> {
                return switch (v) {
                    case "off" -> "Не восстанавливается само";
                    case "engine" -> "Своё: сколько и как часто";
                    default -> "Как в игре (от сытости)";
                };
            }, v -> {
                this.cfg.body.regen = v;
                this.buildForm();
            });
            if (this.cfg.body.regen.equals("engine")) {
                this.form.decimal("Сколько за раз", () -> this.cfg.body.regenAmount, v -> this.cfg.body.regenAmount = (float)Math.max(0.1, v));
                this.form.decimal("Раз в секунд", () -> this.cfg.body.regenEvery, v -> this.cfg.body.regenEvery = (float)Math.max(0.05, v));
                this.form.decimal("Пауза после урона, сек", () -> this.cfg.body.regenDelay, v -> this.cfg.body.regenDelay = (float)Math.max(0.0, v));
                this.form.number("Только при еде от", () -> this.cfg.body.regenFood, v -> this.cfg.body.regenFood = Math.max(0, Math.min(20, v)));
                this.form.note("Сердце = 2 здоровья. Еда 0 — лечится при любой сытости");
            }

            this.form.header("Навигатор");
            this.form.note("Стрелки navigate: куда идти. Где они стоят — часть «Навигатор» на странице «Игра (HUD)» (и «HUD · Навигатор» в макете)");
            this.navForm();
            this.form.header("Лук игрока");
            this.form
                .toggle(
                    "Анимация лука",
                    "Натягивая лук (обычный или мода), игрок встаёт в стойку лучника: натяжение идёт вместе с тетивой, при выстреле — отдача. Видят все",
                    () -> this.cfg.bow.enabled,
                    v -> {
                        this.cfg.bow.enabled = v;
                        this.buildForm();
                    }
                );
            if (this.cfg.bow.enabled) {
                this.form
                    .text(
                        "Натягивание",
                        "Анимация, лук в правой руке. По умолчанию — Super Rig: entity/humanoid!bow_draw_right",
                        () -> this.cfg.bow.draw,
                        v -> this.cfg.bow.draw = v.trim()
                    );
                this.form.text("Выстрел", "Играет один раз, когда игрок отпускает тетиву", () -> this.cfg.bow.release, v -> this.cfg.bow.release = v.trim());
                this.form
                    .text(
                        "Натягивание левой",
                        "Лук в левой руке (левша или лук во второй руке)",
                        () -> this.cfg.bow.drawLeft,
                        v -> this.cfg.bow.drawLeft = v.trim()
                    );
                this.form.text("Выстрел левой", null, () -> this.cfg.bow.releaseLeft, v -> this.cfg.bow.releaseLeft = v.trim());
                this.form.number("Тиков до полного натяга", () -> this.cfg.bow.fullDraw, v -> this.cfg.bow.fullDraw = Math.max(1, v));
            }

            this.form.header("Время суток");
            EngineConfig.Clock clock = this.cfg.clock;
            this.form
                .toggle(
                    "Управлять временем",
                    "Движок сам ставит смену дня и ночи при загрузке мира. Выключено — решают /gamerule и /time",
                    () -> clock.manage,
                    v -> {
                        clock.manage = v;
                        this.buildForm();
                    }
                );
            if (this.cfg.time) {
                this.form.note("Время ведут часы мира; день длится 80 минут");
            } else if (clock.manage) {
                this.form
                    .dropdown("Время стоит", "Зафиксировать время суток (день не идёт)", () -> FIXED_TIMES, () -> clock.fixed, EngineTab::fixedTitle, v -> {
                        clock.fixed = v;
                        this.buildForm();
                    });
                if (clock.fixed < 0) {
                    this.form.toggle("Смена дня и ночи", "Время идёт (правило doDaylightCycle)", () -> clock.cycle, v -> clock.cycle = v);
                }

                this.form.decimal("Скорость дня", () -> clock.speed, v -> clock.speed = (float)Math.max(0.05, Math.min(50.0, v)));
                this.form.note("1 — обычные 20 минут, 2 — вдвое быстрее, 0.5 — вдвое медленнее");
            }

            this.form.header("Автозагрузка на сервер");
            this.form
                .toggle(
                    "При подключении",
                    "Все сюжеты проекта без ошибок загружаются в мир или на сервер при входе с открытым проектом и при открытии проекта в мире. Идущие сюжеты не прерываются",
                    () -> this.cfg.upload.onConnect,
                    v -> this.cfg.upload.onConnect = v
                );
            this.form
                .toggle(
                    "При сохранении",
                    "Сюжет без ошибок загружается, когда вы сохраняете его сами (Ctrl+S, Ctrl+Shift+S). Автосохранение ничего не загружает",
                    () -> this.cfg.upload.onSave,
                    v -> {
                        this.cfg.upload.onSave = v;
                        this.buildForm();
                    }
                );
            if (this.cfg.upload.onSave) {
                this.form
                    .toggle(
                        "Перезапускать идущий сюжет",
                        "Иначе идущий сюжет доигрывает старый текст, а новый начнётся со следующего запуска. Сюжет «только игроку» начнётся заново только у вас, а у остальных остановится",
                        () -> this.cfg.upload.restart,
                        v -> this.cfg.upload.restart = v
                    );
            }

            this.form.note("Нужны права оператора. Загруженное хранится в папке мира и переживает перезапуск сервера");
            if ("systems".equals(this.page)) {
                this.relayout();
            } else {
                if (this.mode == EngineTab.Mode.QUEST) {
                    this.questForm();
                } else if (this.mode == EngineTab.Mode.ZONE) {
                    this.zoneTitleForm();
                } else if (this.mode == EngineTab.Mode.SHOP) {
                    this.shopForm();
                } else if (this.mode == EngineTab.Mode.CHOICE) {
                    this.choiceForm();
                } else if (this.mode == EngineTab.Mode.CHAT) {
                    this.form.header("История чата");
                    this.form.note("Окно истории сообщений. Пустое поле — стандартная текстура");
                    this.form.skin("Фон", this.cfg.chatLog.back);
                    this.form.skin("Рамка", this.cfg.chatLog.frame);
                    this.form
                        .toggle(
                            "Рамка поверх сообщений",
                            "Включите, если в середине вашей рамки прозрачная дыра под текст (как у Spraute). Выключено — рамка под сообщениями, сразу над фоном",
                            () -> this.cfg.chatLog.frameOver,
                            v -> this.cfg.chatLog.frameOver = v
                        );
                    this.form.header("Имена в речи");
                    this.form.note("Цвет имени говорящего в окне речи и в истории сообщений");
                    this.form
                        .toggle(
                            "Один цвет для всех",
                            "Персонажам без своего color — этот цвет. Выключено — обычный цвет Spraute",
                            () -> this.cfg.chatLog.nameColor >= 0,
                            v -> {
                                this.cfg.chatLog.nameColor = v ? 16755200 : -1;
                                this.buildForm();
                            }
                        );
                    if (this.cfg.chatLog.nameColor >= 0) {
                        this.form.color("Цвет имён", () -> 0xFF000000 | this.cfg.chatLog.nameColor, v -> this.cfg.chatLog.nameColor = v & 16777215);
                    }

                    this.form.note("Свой цвет персонажу: character … color \"gold\"  ·  namecolor alice \"#FF8800\"");
                    this.form
                        .toggle(
                            "Подсказка «▸ F8»",
                            "У имени говорящего, пока сюжет ждёт нажатия F8 (реплика без nowait)",
                            () -> this.cfg.chatLog.f8Hint,
                            v -> this.cfg.chatLog.f8Hint = v
                        );
                    this.form.header("Голова игрока в речи");
                    this.form
                        .decimal(
                            "Размер", () -> this.cfg.chatLog.playerHeadScale, v -> this.cfg.chatLog.playerHeadScale = (float)Math.max(0.2, Math.min(4.0, v))
                        );
                    this.form.number("Сдвиг X (%)", () -> this.cfg.chatLog.playerHeadX, v -> this.cfg.chatLog.playerHeadX = v);
                    this.form.number("Сдвиг Y (%)", () -> this.cfg.chatLog.playerHeadY, v -> this.cfg.chatLog.playerHeadY = v);
                    this.form.note("Новые размеры — со следующего запуска сюжета");
                } else {
                    this.elementForm();
                }

                this.relayout();
            }
        }
    }

    private void skillsForm() {
        SkillLook l = this.cfg.skills;
        this.form.header("Панели прокачки");
        if (skillsPages != null) {
            this.form.dropdown("Страница", null, skillsPages::pages, skillsPages.now(), skillsPages.title(), skillsPages.open());
        }

        this.form.note("Общий вид всех меню прокачки. Меню с галочкой «Свой вид» в редакторе системы настраивается отдельно");
        this.form.header("Цвета");
        this.form.color("Главный цвет", () -> l.accent, v -> l.accent = v);
        this.form.colorAlpha("Фон панели", () -> l.panel, v -> l.panel = v);
        this.form.colorAlpha("Верхняя полоса", () -> l.header, v -> l.header = v);
        this.form.colorAlpha("Навык", () -> l.tile, v -> l.tile = v);
        this.form.colorAlpha("Рамки", () -> l.border, v -> l.border = v);
        this.form.color("Текст", () -> l.text, v -> l.text = v);
        this.form.color("Приглушённый текст", () -> l.muted, v -> l.muted = v);
        this.form.color("Можно изучить", () -> l.good, v -> l.good = v);
        this.form.color("Нельзя", () -> l.bad, v -> l.bad = v);
        this.form.header("Текстуры");
        this.form.note("Пусто — просто цвета");
        this.form.skin("Фон панели", l.panelImage);
        this.form.skin("Навык", l.tileImage);
        this.form.skin("Навык изучен", l.tileLearned);
        this.form.skin("Навык закрыт", l.tileLocked);
        this.form.skin("Кнопка", l.buttonImage);
        this.form.skin("Окно при наведении", l.tooltipImage);
        this.form.number("Толщина связи", () -> l.linkWidth, v -> l.linkWidth = Math.max(1, Math.min(32, v)));
        this.form.skin("Связь навыков", l.linkImage);
        this.form.skin("Связь изучена", l.linkLearned);
        this.form.skin("Стрелка связи", l.arrowImage);
        this.form.skin("Стрелка изучена", l.arrowLearned);
        SkillHud sh = this.cfg.skillHud;
        this.form.header("Способности на экране");
        this.form.note("Изученные способности над хотбаром: значки, клавиши, перезарядка");
        if (skillsPages != null) {
            this.form.action("Двигать мышью: сцена «Игра (HUD)»", () -> skillsPages.open().accept("hud"));
        }

        this.form.toggle("Показывать", null, () -> sh.abilities.visible, v -> sh.abilities.visible = v);
        this.placeRows(
            () -> sh.abilities.anchor,
            v -> sh.abilities.anchor = v,
            () -> sh.abilities.x,
            v -> sh.abilities.x = v,
            () -> sh.abilities.y,
            v -> sh.abilities.y = v
        );
        this.form.number("Размер значка", () -> sh.abilities.size, v -> sh.abilities.size = Math.max(4, v));
        this.form.number("Зазор", () -> sh.abilities.gap, v -> sh.abilities.gap = v);
        this.form.number("Отступ значка", () -> sh.abilities.pad, v -> sh.abilities.pad = v);
        this.form.toggle("В столбик", null, () -> sh.abilities.vertical, v -> sh.abilities.vertical = v);
        this.form.toggle("Клавиши", null, () -> sh.abilities.keys, v -> sh.abilities.keys = v);
        this.form.toggle("Секунды перезарядки", null, () -> sh.abilities.seconds, v -> sh.abilities.seconds = v);
        this.form.color("Фон значка", () -> sh.abilities.plate, v -> sh.abilities.plate = v);
        this.form.skin("Картинка фона", sh.abilities.plateImage);
        this.form.skin("Рамка поверх значка", sh.abilities.frameImage);
        this.form.skin("Картинка перезарядки", sh.abilities.cooldownImage);
        this.form.color("Цвет клавиши", () -> sh.abilities.keyColor, v -> sh.abilities.keyColor = v);
        this.form.color("Цвет секунд", () -> sh.abilities.textColor, v -> sh.abilities.textColor = v);
        this.form.color("Затемнение перезарядки", () -> sh.abilities.cooldown, v -> sh.abilities.cooldown = v);
        this.form.header("Уровни на экране");
        this.form.toggle("Показывать", null, () -> sh.level.visible, v -> sh.level.visible = v);
        this.placeRows(() -> sh.level.anchor, v -> sh.level.anchor = v, () -> sh.level.x, v -> sh.level.x = v, () -> sh.level.y, v -> sh.level.y = v);
        this.form.dropdown("Куда заполняется", null, () -> SkillTree.DIRECTIONS, () -> sh.level.direction, EngineTab::fillTitle, v -> sh.level.direction = v);
        this.form.number("Ширина", () -> sh.level.width, v -> sh.level.width = v);
        this.form.number("Высота", () -> sh.level.height, v -> sh.level.height = v);
        this.form.number("Зазор", () -> sh.level.gap, v -> sh.level.gap = v);
        this.form.toggle("Название и уровень", null, () -> sh.level.text, v -> sh.level.text = v);
        this.form.color("Фон", () -> sh.level.back, v -> sh.level.back = v);
        this.form.color("Цвет текста", () -> sh.level.textColor, v -> sh.level.textColor = v);
    }

    private static String fillTitle(String v) {
        return switch (v) {
            case "left" -> "влево";
            case "up" -> "вверх";
            case "down" -> "вниз";
            default -> "вправо";
        };
    }

    private void placeRows(Supplier<String> anchor, Consumer<String> setAnchor, IntSupplier x, IntConsumer setX, IntSupplier y, IntConsumer setY) {
        this.form.dropdown("Угол экрана", null, () -> ANCHORS, anchor, v -> {
            return switch (v) {
                case "TL" -> "слева вверху";
                case "T" -> "сверху";
                case "TR" -> "справа вверху";
                case "L" -> "слева";
                case "C" -> "по центру";
                case "R" -> "справа";
                case "BL" -> "слева внизу";
                case "B" -> "снизу";
                default -> "справа внизу";
            };
        }, setAnchor);
        this.form.number("Сдвиг вправо", x, setX);
        this.form.number("Сдвиг вниз", y, setY);
    }

    private void addonsForm() {
        List<Addons.Info> all = Addons.loaded();
        this.form.header("Загруженные аддоны");
        if (all.isEmpty()) {
            this.form.note("Аддонов нет. Аддон — обычный мод с файлом META-INF/spraute_addon.properties: положите его в папку mods");
        }

        for (Addons.Info a : all) {
            this.form.header(a.name() + " · " + a.version());
            StringBuilder added = new StringBuilder();
            a.added().forEach((what, n) -> added.append(added.length() > 0 ? ", " : "").append(what).append(": ").append(n));
            this.form.note(a.id() + (added.length() > 0 ? " · " + added : ""));
            if (a.error() != null) {
                this.form.note("Ошибка: " + a.error());
            }
        }
    }

    private void speechForm() {
        this.form.header("Где показывать реплики");
        this.form.note("Полный вид реплики — фон, портрет, имя, текст — правится в сцене «Реплики». Когда там включён свой вид, положение ниже не действует");
        this.form
            .dropdown(
                "Положение",
                null,
                () -> List.of("bottom", "center", "top"),
                () -> this.cfg.chatLog.speechPlace == null ? "bottom" : this.cfg.chatLog.speechPlace,
                v -> {
                    return switch (v) {
                        case "center" -> "По центру экрана";
                        case "top" -> "Сверху";
                        default -> "Снизу";
                    };
                },
                v -> this.cfg.chatLog.speechPlace = v
            );
        this.form
            .dropdown(
                "По ширине",
                null,
                () -> List.of("left", "center", "right"),
                () -> this.cfg.chatLog.speechAlign == null ? "center" : this.cfg.chatLog.speechAlign,
                v -> {
                    return switch (v) {
                        case "left" -> "Слева";
                        case "right" -> "Справа";
                        default -> "По центру";
                    };
                },
                v -> this.cfg.chatLog.speechAlign = v
            );
        this.form.number("Сдвиг вверх", () -> this.cfg.chatLog.speechShift, v -> this.cfg.chatLog.speechShift = v);
        this.form.number("Сдвиг вправо", () -> this.cfg.chatLog.speechShiftX, v -> this.cfg.chatLog.speechShiftX = v);
        this.form.header("Имена в речи");
        this.form.note("Цвет имени говорящего в окне речи и в истории сообщений");
        this.form
            .toggle(
                "Один цвет для всех",
                "Персонажам без своего color — этот цвет. Выключено — обычный цвет Spraute",
                () -> this.cfg.chatLog.nameColor >= 0,
                v -> {
                    this.cfg.chatLog.nameColor = v ? 16755200 : -1;
                    this.buildForm();
                }
            );
        if (this.cfg.chatLog.nameColor >= 0) {
            this.form.color("Цвет имён", () -> 0xFF000000 | this.cfg.chatLog.nameColor, v -> this.cfg.chatLog.nameColor = v & 16777215);
        }

        this.form.note("Свой цвет персонажу: character … color \"gold\"  ·  namecolor alice \"#FF8800\"");
        this.form
            .toggle(
                "Подсказка «▸ F8»",
                "У имени говорящего, пока сюжет ждёт нажатия F8 (реплика без nowait)",
                () -> this.cfg.chatLog.f8Hint,
                v -> this.cfg.chatLog.f8Hint = v
            );
        this.form.header("Голова игрока в речи");
        this.form.decimal("Размер", () -> this.cfg.chatLog.playerHeadScale, v -> this.cfg.chatLog.playerHeadScale = (float)Math.max(0.2, Math.min(4.0, v)));
        this.form.number("Сдвиг X (%)", () -> this.cfg.chatLog.playerHeadX, v -> this.cfg.chatLog.playerHeadX = v);
        this.form.number("Сдвиг Y (%)", () -> this.cfg.chatLog.playerHeadY, v -> this.cfg.chatLog.playerHeadY = v);
        this.form.note("Новые размеры — со следующего запуска сюжета");
        this.form.header("Меню выбора: поведение");
        this.form
            .toggle(
                "Камера к говорящему", "Камера подлетает к тому, кто спрашивает, как в Spraute", () -> this.cfg.choice.camera, v -> this.cfg.choice.camera = v
            );
    }

    private static String fixedTitle(int t) {
        return switch (t) {
            case -1 -> "нет — время идёт";
            case 0 -> "рассвет (0)";
            case 1000 -> "утро (1000)";
            case 6000 -> "полдень (6000)";
            case 12000 -> "закат (12000)";
            case 13000 -> "ночь (13000)";
            case 18000 -> "полночь (18000)";
            case 23000 -> "перед рассветом (23000)";
            default -> String.valueOf(t);
        };
    }

    private void shopForm() {
        EngineConfig.Shop s = this.cfg.shop;
        this.form.header("Окно магазина");
        this.form.note("Как в Spraute. Справа — пример: щёлкайте товары, стрелки, колесо листает. Пусто — текстуры Spraute");
        this.form.skin("Список товаров (254×212)", s.panel);
        this.form.skin("Карточка (176×262)", s.detail);
        this.form.skin("Ячейка (30×43)", s.cell);
        this.form.skin("Ячейка выбрана", s.cellOn);
        this.form.skin("Кнопка (88×14)", s.dealButton);
        this.form.skin("Кнопка наведена", s.buttonHover);
        this.form.skin("Ячейка — закрыто", s.locked);
        this.form.skin("Ячейка — продано", s.sold);
        this.form.skin("Ползунок (9×10)", s.slider);
        this.form.skin("Стрелка −5", s.arrowMinus5);
        this.form.skin("Стрелка −5 наведена", s.arrowMinus5Hover);
        this.form.skin("Стрелка −1", s.arrowMinus1);
        this.form.skin("Стрелка −1 наведена", s.arrowMinus1Hover);
        this.form.skin("Стрелка +1", s.arrowPlus1);
        this.form.skin("Стрелка +1 наведена", s.arrowPlus1Hover);
        this.form.skin("Стрелка +5", s.arrowPlus5);
        this.form.skin("Стрелка +5 наведена", s.arrowPlus5Hover);
        this.form.header("Расположение");
        this.form.note("Перетаскивайте части окна справа; стрелки — по 1, с Shift — по 10");
        this.form
            .dropdown(
                "Двигаю",
                "Что двигать. «Всё окно» — только отсюда: по превью таскаются части",
                () -> EngineTab.ShopPanelParts.ALL,
                () -> this.shopPart,
                ShopPanel::partTitle,
                v -> {
                    this.shopPart = v;
                    this.buildForm();
                }
            );
        this.form.button("Вернуть на место", null, () -> {
            s.offsets.remove(this.shopPart);
            this.buildForm();
        });
        this.form.toggle("Всё как в Spraute", null, () -> s.offsets.isEmpty(), v -> {
            s.offsets.clear();
            this.buildForm();
        });
        this.form.header("Цвета");
        this.form.color("Название", () -> s.titleColor, v -> s.titleColor = v);
        this.form.color("Описание", () -> s.mutedColor, v -> s.mutedColor = v);
        this.form.color("Цена", () -> s.priceColor, v -> s.priceColor = v);
        this.form.color("Продажа / удача", () -> s.sellColor, v -> s.sellColor = v);
    }

    private void questForm() {
        EngineConfig.Quests q = this.cfg.quests;
        this.form.header("Журнал квестов");
        this.form.note("Справа — пример. Тяните список и карточку, стрелки — точный сдвиг");
        this.form.decimal("Размер", () -> q.scale, v -> q.scale = (float)Math.max(0.3, Math.min(1.5, v)));
        this.form.note("1 — на весь экран (журнал сохраняет форму), 0.8 — меньше");
        this.form
            .dropdown("Двигаю", null, () -> List.of("list", "card"), () -> this.questPart, v -> v.equals("list") ? "список квестов" : "карточку квеста", v -> {
                this.questPart = v;
                this.buildForm();
            });
        this.form.button("Вернуть на место", null, () -> {
            q.offsets.remove(this.questPart);
            this.buildForm();
        });
        this.form.header("Цели на экране");
        this.form
            .toggle(
                "Цели справа",
                "Отслеживаемый квест и его задачи у правого края экрана. Выключено — задачи видны только в журнале (J)",
                () -> q.tracker,
                v -> q.tracker = v
            );
        EngineConfig.Quests.Toast t = q.toast;
        this.form.header("Уведомление квеста");
        this.form.note("Плашка сверху экрана: новый квест, задача выполнена, квест выполнен…");
        this.form.toggle("Показывать", "Выключено — никаких плашек, всё только в журнале", () -> t.enabled, v -> {
            t.enabled = v;
            this.buildForm();
        });
        if (t.enabled) {
            this.form.toggle("Новый квест", null, () -> t.onStart, v -> t.onStart = v);
            this.form.text("  надпись", "Пусто — «Новый квест»", () -> t.startTitle, v -> t.startTitle = v);
            this.form.toggle("Задача выполнена", null, () -> t.onTask, v -> t.onTask = v);
            this.form.text("  надпись", "Пусто — «Задача выполнена»", () -> t.taskTitle, v -> t.taskTitle = v);
            this.form.toggle("Новая задача", "Скрытая задача открылась (quest reveal)", () -> t.onReveal, v -> t.onReveal = v);
            this.form.text("  надпись", "Пусто — «Новая задача»", () -> t.revealTitle, v -> t.revealTitle = v);
            this.form.toggle("Квест выполнен", null, () -> t.onComplete, v -> t.onComplete = v);
            this.form.text("  надпись", "Пусто — «Квест выполнен»", () -> t.completeTitle, v -> t.completeTitle = v);
            this.form.toggle("Квест провален", null, () -> t.onFail, v -> t.onFail = v);
            this.form.text("  надпись", "Пусто — «Квест провален»", () -> t.failTitle, v -> t.failTitle = v);
            this.form.decimal("Секунд на экране", () -> t.seconds, v -> t.seconds = (float)Math.max(0.5, Math.min(30.0, v)));
            this.form.number("Отступ сверху", () -> t.y, v -> t.y = Math.max(0, Math.min(400, v)));
            this.form.number("Ширина", () -> t.width, v -> t.width = Math.max(80, Math.min(600, v)));
            this.form.decimal("Размер", () -> t.scale, v -> t.scale = (float)Math.max(0.3, Math.min(3.0, v)));
            this.form.color("Фон", () -> t.background, v -> t.background = t.background & 0xFF000000 | v & 16777215);
            this.form
                .number(
                    "Фон: прозрачность (0..255)",
                    () -> t.background >>> 24 & 0xFF,
                    v -> t.background = Math.max(0, Math.min(255, v)) << 24 | t.background & 16777215
                );
            this.form.toggle("Значок", null, () -> t.icon, v -> t.icon = v);
            this.form.toggle("Звук", null, () -> t.sound, v -> t.sound = v);
            this.form.note("Цвета надписи — из «Цвета» ниже: отслеживаемый (новый квест, задача), выполнено, провалено");
            this.form
                .button(
                    "Показать для проверки",
                    "Плашка «Задача выполнена» появится в игре",
                    () -> QuestClient.showToast("task", "Задача выполнена", "Пример задачи")
                );
        }

        this.form.header("Текстуры");
        this.form.note("Пусто — текстуры Spraute");
        this.form.skin("Список (144×230)", q.list);
        this.form.skin("Карточка (240×225)", q.card);
        this.form.skin("Кнопка квеста (131×35)", q.button);
        this.form.skin("Кнопка наведена", q.buttonHover);
        this.form.skin("Кнопка выбрана", q.buttonOn);
        this.form.skin("Полоса задачи (225×16)", q.taskLine);
        this.form.note("Листы — как в архиве текстур: несколько состояний друг под другом, можно крупнее в 2–4 раза");
        this.form.skin("Вкладка «Текущие» (23×69)", q.tabCurrent);
        this.form.skin("Вкладка «Выполненные» (23×69)", q.tabDone);
        this.form.skin("Вкладка «Проваленные» (23×69)", q.tabFailed);
        this.form.skin("Значки (63×46)", q.icons);
        this.form.skin("Разделитель (129×2)", q.divider);
        this.form.header("Цвета");
        this.form.color("Текст", () -> q.textColor, v -> q.textColor = v);
        this.form.color("Приглушённый", () -> q.mutedColor, v -> q.mutedColor = v);
        this.form.color("Отслеживаемый, счётчики", () -> q.accentColor, v -> q.accentColor = v);
        this.form.color("Заголовки", () -> q.headerColor, v -> q.headerColor = v);
        this.form.color("Выполнено", () -> q.doneColor, v -> q.doneColor = v);
        this.form.color("Провалено", () -> q.failColor, v -> q.failColor = v);
        this.form.color("Затемнение экрана", () -> q.shadeColor, v -> q.shadeColor = q.shadeColor & 0xFF000000 | v & 16777215);
        this.form
            .number("Затемнение (0..255)", () -> q.shadeColor >>> 24 & 0xFF, v -> q.shadeColor = Math.max(0, Math.min(255, v)) << 24 | q.shadeColor & 16777215);
        this.form.button("Всё как в Spraute", null, () -> {
            this.cfg.quests = new EngineConfig.Quests();
            this.buildForm();
        });
    }

    private String questPartAt(double jx, double jy) {
        int[] ol = this.cfg.quests.off("list");
        int[] oc = this.cfg.quests.off("card");
        if (jx >= 150 + oc[0] && jx < 390 + oc[0] && jy >= 2 + oc[1] && jy < 227 + oc[1]) {
            return "card";
        } else {
            return jx >= ol[0] && jx < 144 + ol[0] && jy >= ol[1] && jy < 230 + ol[1] ? "list" : null;
        }
    }

    private void zoneTitleForm() {
        EngineConfig.ZoneTitle z = this.cfg.zoneTitle;
        this.form.header("Название зоны");
        this.form.note("Плашка с названием, когда игрок входит в зону. Справа — щёлкните или тяните, чтобы поставить");
        this.form.decimal("Где по X (0..1)", () -> z.x, v -> z.x = (float)Math.max(0.0, Math.min(1.0, v)));
        this.form.decimal("Где по Y (0..1)", () -> z.y, v -> z.y = (float)Math.max(0.0, Math.min(1.0, v)));
        this.form.decimal("Размер названия", () -> z.scale, v -> z.scale = (float)Math.max(0.3, v));
        this.form.decimal("Размер подписи", () -> z.subScale, v -> z.subScale = (float)Math.max(0.3, v));
        this.form.decimal("Сколько секунд", () -> z.seconds, v -> z.seconds = (float)Math.max(0.5, v));
        this.form.toggle("Жирный", null, () -> z.bold, v -> z.bold = v);
        this.form.toggle("Тень", null, () -> z.shadow, v -> z.shadow = v);
        this.form.toggle("Линии под названием", null, () -> z.lines, v -> z.lines = v);
        this.form.toggle("Буквы сходятся", "Название чуть шире при появлении и сжимается", () -> z.spread, v -> z.spread = v);
        this.form.header("Цвета");
        this.form.color("Название", () -> z.titleColor, v -> z.titleColor = v);
        this.form.color("Подпись", () -> z.subColor, v -> z.subColor = v);
        this.form.color("Линии", () -> z.lineColor, v -> z.lineColor = z.lineColor & 0xFF000000 | v & 16777215);
        this.form.header("Плашка");
        this.form.toggle("Плашка под текстом", null, () -> z.plate, v -> {
            z.plate = v;
            this.buildForm();
        });
        if (z.plate) {
            this.form.color("Цвет плашки", () -> z.plateColor, v -> z.plateColor = z.plateColor & 0xFF000000 | v & 16777215);
            this.form
                .number(
                    "Прозрачность (0..255)",
                    () -> z.plateColor >>> 24 & 0xFF,
                    v -> z.plateColor = Math.max(0, Math.min(255, v)) << 24 | z.plateColor & 16777215
                );
            this.form.number("Отступ", () -> z.padding, v -> z.padding = Math.max(0, v));
            this.form.skin("Своя текстура плашки", z.plateSkin);
        }

        this.form.header("Звук");
        this.form
            .text("Звук появления", "Id звука, например minecraft:ui.toast.challenge_complete (пусто — без звука)", () -> z.sound, v -> z.sound = v.trim());
        this.form.button("Всё как было", null, () -> {
            this.cfg.zoneTitle = new EngineConfig.ZoneTitle();
            this.buildForm();
        });
    }

    private void choiceForm() {
        EngineConfig.Choice ch = this.cfg.choice;
        this.form.header("Меню выбора (choice)");
        this.form.dropdown("Меню", "Меню выбора в диалоге", () -> CHOICE_STYLES, () -> ch.style, v -> v.equals("spraute") ? "своё (движок)" : "Spraute", v -> {
            ch.style = v;
            this.buildForm();
        });
        if (!ch.style.equals("spraute")) {
            this.relayout();
        } else {
            this.form
                .toggle(
                    "Камера к говорящему",
                    "Как в диалогах Spraute: камера подлетает к лицу того, кто спрашивает, а после ответа возвращается",
                    () -> ch.camera,
                    v -> ch.camera = v
                );
            if (!ch.show.equals("none")) {
                this.form
                    .dropdown(
                        "Кадр",
                        null,
                        () -> List.of("body", "head"),
                        () -> ch.framing,
                        v -> v.equals("head") ? "только голова (как в чате)" : "во весь рост",
                        v -> ch.framing = v
                    );
                this.form.header("Персонаж в рамке");
                this.form.decimal("Масштаб", () -> ch.entityScale, v -> ch.entityScale = (float)Math.max(0.1, v));
                this.form.number("Сдвиг X", () -> ch.entityX, v -> ch.entityX = v);
                this.form.number("Сдвиг Y", () -> ch.entityY, v -> ch.entityY = v);
                this.form.note("Обрезка — сколько пикселей рамки срезать с каждой стороны");
                this.form.number("Обрезка слева", () -> ch.cropLeft, v -> ch.cropLeft = Math.max(0, v));
                this.form.number("Обрезка справа", () -> ch.cropRight, v -> ch.cropRight = Math.max(0, v));
                this.form.number("Обрезка сверху", () -> ch.cropTop, v -> ch.cropTop = Math.max(0, v));
                this.form.number("Обрезка снизу", () -> ch.cropBottom, v -> ch.cropBottom = Math.max(0, v));
                this.form.button("Как было", "Масштаб 1, без сдвига, обрезка 4 со всех сторон", () -> {
                    ch.entityScale = 1.0F;
                    ch.entityX = ch.entityY = 0;
                    ch.cropLeft = ch.cropRight = ch.cropTop = ch.cropBottom = 4;
                    this.buildForm();
                });
            }

            this.form.header("Размер");
            this.form.decimal("Размер меню", () -> ch.uiScale, v -> ch.uiScale = (float)Math.max(0.3, Math.min(3.0, v)));
            this.form.note("1 — как окно Spraute, на любом экране и масштабе интерфейса одинаково");
            this.form
                .dropdown(
                    "Ширина кнопок",
                    "По тексту — кнопка растягивается под свой ответ",
                    () -> List.of("fixed", "fit"),
                    () -> ch.answerWidth,
                    v -> v.equals("fit") ? "по тексту" : "как в Spraute (166)",
                    v -> {
                        ch.answerWidth = v;
                        this.buildForm();
                    }
                );
            if (ch.answerWidth.equals("fit")) {
                this.form.number("Мин. ширина", () -> ch.answerMinW, v -> ch.answerMinW = Math.max(40, v));
                this.form.number("Макс. ширина", () -> ch.answerMaxW, v -> ch.answerMaxW = Math.max(60, v));
            }

            this.form.decimal("Размер текста ответов", () -> ch.answerTextScale, v -> ch.answerTextScale = (float)Math.max(0.4, Math.min(3.0, v)));
            this.form.header("Расположение");
            this.form
                .note("Перетаскивайте части меню на экране справа (стрелки — сдвиг на 1, с Shift — на 10). Выбрано: " + ChoiceLayout.title(this.choicePart));
            this.form.button("Вернуть выбранное на место", null, () -> {
                ch.offsets.remove(this.choicePart);
                this.buildForm();
            });
            this.form.toggle("Всё как в Spraute", null, () -> ch.offsets.isEmpty(), v -> {
                ch.offsets.clear();
                this.buildForm();
            });
            this.form.header("Текстуры");
            this.form.note("Пустое поле — текстуры Spraute");
            this.form.skin("Панель (480×270)", ch.panel);
            this.form.skin("Поверх портрета", ch.portrait);
            this.form.skin("Кнопка", ch.button);
            this.form.skin("Кнопка (наведена)", ch.buttonHover);
            this.form.toggle("Полоска под именем", "Выключено — под именем спрашивающего ничего нет", () -> ch.nameLine, v -> {
                ch.nameLine = v;
                this.buildForm();
            });
            if (ch.nameLine) {
                this.form.skin("Полоска под именем (80×3)", ch.nameplate);
            }

            this.form.header("Цвета");
            this.form.color("Имя", () -> ch.nameColor, v -> ch.nameColor = v);
            this.form.color("Вопрос", () -> ch.textColor, v -> ch.textColor = v);
            this.form.color("Ответ", () -> ch.answerColor, v -> ch.answerColor = v);
            this.form.color("Ответ (наведён)", () -> ch.answerHoverColor, v -> ch.answerHoverColor = v);
        }
    }

    private void elementForm() {
        String key = this.selected;
        EngineConfig.Element e = this.cfg.element(key);
        this.form.header(EngineConfig.title(key));
        if (key.equals("coins")) {
            this.form
                .text(
                    "Подпись после числа",
                    "Например « монет» или « ₽». Пробел в начале — отступ",
                    () -> this.cfg.coinText.suffix,
                    v -> this.cfg.coinText.suffix = v
                );
            this.form.toggle("Сокращать большие числа", "12500 → 12.5k, 3000000 → 3M", () -> this.cfg.coinText.shorten, v -> this.cfg.coinText.shorten = v);
            this.form.color("Цвет числа", () -> this.cfg.coinText.color, v -> this.cfg.coinText.color = v);
            this.form.color("Цвет, когда не хватает", () -> this.cfg.coinText.lackColor, v -> this.cfg.coinText.lackColor = v);
            this.form.note("Для вида «Картинка и число» цвет числа — ниже, в его настройках");
        }

        if (key.equals("health")) {
            this.form.note(this.cfg.health ? "Стандартный вид — круглая полоска Spraute" : "Стандартный вид — сердечки Minecraft");
        }

        this.form.toggle("Показывать", "Выключите, чтобы убрать элемент с экрана", () -> e.visible, v -> e.visible = v);
        this.form
            .toggle(
                "Своя позиция",
                "Выключено — элемент стоит на своём обычном месте. Перетащите его на экране справа, чтобы переставить",
                () -> e.customPos,
                v -> {
                    if (v) {
                        this.place(key, e, HudPainter.rect(this.cfg, key, 480, 270, this.sampleState()));
                    }

                    e.customPos = v;
                    this.buildForm();
                }
            );
        if (e.customPos) {
            this.form
                .dropdown(
                    "Якорь",
                    "Точка экрана, от которой отсчитывается позиция: при другом размере окна элемент держится её",
                    () -> ANCHORS,
                    () -> e.anchor,
                    EngineTab::anchorTitle,
                    v -> {
                        int[] r = HudPainter.rect(this.cfg, key, 480, 270, this.sampleState());
                        e.anchor = v;
                        this.place(key, e, r);
                        this.buildForm();
                    }
                );
            this.form
                .toggle(
                    "В процентах",
                    "Сдвиг от якоря в процентах размера экрана: элемент держит своё место при любом размере окна и масштабе интерфейса",
                    () -> e.percent,
                    v -> {
                        e.percent = v;
                        this.buildForm();
                    }
                );
            if (e.percent) {
                this.form.decimal("X, %", () -> e.px, v -> e.px = (float)v);
                this.form.decimal("Y, %", () -> e.py, v -> e.py = (float)v);
            } else {
                this.form.number("X", () -> e.x, v -> e.x = v);
                this.form.number("Y", () -> e.y, v -> e.y = v);
            }
        }

        if (key.equals("nav")) {
            this.navForm();
        } else {
            this.form.dropdown("Вид", null, () -> EngineConfig.styles(key), () -> e.style, EngineConfig::styleTitle, v -> {
                int[] r = HudPainter.rect(this.cfg, key, 480, 270, this.sampleState());
                e.style = v;
                if (!EngineConfig.gameDrawn(v) && !e.customPos) {
                    e.customPos = true;
                    this.place(key, e, r);
                }

                this.buildForm();
            });
            String var3 = e.style;
            switch (var3) {
                case "bar":
                    this.form.number("Ширина", () -> e.width, v -> e.width = Math.max(2, v));
                    this.form.number("Высота", () -> e.height, v -> e.height = Math.max(2, v));
                    this.form.number("Отступ заливки", () -> e.pad, v -> e.pad = Math.max(0, v));
                    this.form.dropdown("Куда заполняется", null, () -> List.of("right", "left", "up", "down"), () -> e.fillDir, v -> {
                        return switch (v) {
                            case "left" -> "Влево";
                            case "up" -> "Снизу вверх";
                            case "down" -> "Сверху вниз";
                            default -> "Вправо";
                        };
                    }, v -> e.fillDir = v);
                    this.form.skin("Фон", e.back);
                    this.form.skin("Заливка", e.fill);
                    this.form.skin("Рамка", e.frame);
                    this.form.color("Цвет заливки", () -> e.fillColor, v -> e.fillColor = v);
                    this.form.color("Цвет фона", () -> e.backColor, v -> e.backColor = v);
                    this.form.note("Цвета — когда нет своих картинок. Прозрачный — свой цвет части");
                    this.form.toggle(key.equals("exp") ? "Уровень над полоской" : "Число на полоске", null, () -> e.text, v -> e.text = v);
                    this.form.color("Цвет текста", () -> e.textColor, v -> e.textColor = v);
                    break;
                case "icons":
                    this.form.number("Количество", () -> e.count, v -> e.count = Math.max(1, Math.min(40, v)));
                    this.form.number("Размер иконки", () -> e.height, v -> e.height = Math.max(2, v));
                    this.form.number("Промежуток", () -> e.gap, v -> e.gap = v);
                    this.form.toggle("Заполнять справа", "Как еда в Minecraft: убывает слева направо", () -> e.reverse, v -> e.reverse = v);
                    this.form.skin("Полная", e.full);
                    this.form.skin("Половина", e.half);
                    this.form.skin("Пустая", e.empty);
                    this.form.color("Цвет полной", () -> e.fillColor, v -> e.fillColor = v);
                    this.form.color("Цвет пустой", () -> e.backColor, v -> e.backColor = v);
                    this.form.toggle("Число рядом", null, () -> e.text, v -> e.text = v);
                    break;
                case "slots":
                    this.form.number("Размер слота", () -> e.height, v -> e.height = Math.max(8, v));
                    this.form.number("Промежуток", () -> e.gap, v -> e.gap = v);
                    this.form.skin("Слот", e.back);
                    this.form.skin("Выбранный", e.frame);
                    this.form.skin("Закрытый", e.fill);
                    this.form.note("Закрытые слоты — при включённой блокировке слотов");
                    break;
                case "icon":
                    this.form.number("Размер иконки", () -> e.height, v -> e.height = Math.max(4, v));
                    this.form.skin("Иконка", e.fill);
                    this.form.skin("Подложка", e.back);
                    this.form.color("Цвет числа", () -> e.textColor, v -> e.textColor = v);
                    break;
                case "orb":
                    this.form.number("Размер", () -> e.height, v -> e.height = Math.max(8, Math.min(200, v)));
                    this.form.color("Жидкость", () -> e.fillColor, v -> e.fillColor = v);
                    this.form.color("Стекло (фон)", () -> e.backColor, v -> e.backColor = v);
                    this.form.toggle("Волны", "Поверхность жидкости колышется", () -> e.waves, v -> e.waves = v);
                    this.form.toggle("Число под шаром", null, () -> e.text, v -> e.text = v);
                    this.form.color("Цвет числа", () -> e.textColor, v -> e.textColor = v);
                    this.form.note("Цвет 0 (прозрачный) — свой цвет части: красное здоровье, голубой воздух…");
                    break;
                case "classic":
                    this.form.note("Широкая рамка, выделение и замки слотов из движка Spraute");
                    break;
                default:
                    this.form.note("Обычный вид игры. Выберите другой вид, чтобы поставить свои текстуры");
            }
        }
    }

    private void navForm() {
        this.form.note("Стрелки команды navigate: куда идти, подпись и расстояние. Цвет и подпись из сюжета важнее этих");
        if (!this.cfg.element("nav").customPos) {
            this.form
                .dropdown(
                    "Где",
                    null,
                    () -> List.of("top", "bottom"),
                    () -> this.cfg.nav.place,
                    v -> v.equals("bottom") ? "Снизу экрана" : "Сверху экрана",
                    v -> this.cfg.nav.place = v
                );
            this.form.number("Отступ от края", () -> this.cfg.nav.offset, v -> this.cfg.nav.offset = Math.max(0, v));
        }

        this.form.decimal("Размер", () -> this.cfg.nav.scale, v -> this.cfg.nav.scale = (float)Math.max(0.5, Math.min(4.0, v)));
        this.form
            .text(
                "Стрелка",
                "Любой знак, который поворачивается к цели: ▲ ➤ ↑ ⬆",
                () -> this.cfg.nav.arrow,
                v -> this.cfg.nav.arrow = v.isBlank() ? "▲" : v.trim()
            );
        this.form.color("Цвет стрелки", () -> this.cfg.nav.arrowColor, v -> this.cfg.nav.arrowColor = v);
        this.form.color("Цвет текста", () -> this.cfg.nav.textColor, v -> this.cfg.nav.textColor = v);
        this.form.color("Подложка", () -> this.cfg.nav.back, v -> this.cfg.nav.back = v);
        this.form.toggle("Расстояние", null, () -> this.cfg.nav.distance, v -> this.cfg.nav.distance = v);
        this.form.text("Единица", "После числа: « м», « бл.»", () -> this.cfg.nav.unit, v -> this.cfg.nav.unit = v);
        this.form.number("Стрелок на экране", () -> this.cfg.nav.max, v -> this.cfg.nav.max = Math.max(1, Math.min(10, v)));
        this.form.note("Подпись и цвет каждой стрелки задаёт сюжет: navigate to alice title \"К Алисе\" color \"#FFD040\"");
    }

    private void place(String key, EngineConfig.Element e, int[] r) {
        int[] full = r.length >= 4 ? r : new int[]{r[0], r[1], 0, 0};
        if (r.length < 4) {
            int[] cur = HudPainter.rect(this.cfg, key, 480, 270, this.sampleState());
            full[2] = cur[2];
            full[3] = cur[3];
        }

        HudPainter.place(e, full, 480, 270);
    }

    private static String anchorTitle(String a) {
        return switch (a) {
            case "TL" -> "↖ слева сверху";
            case "T" -> "↑ по центру сверху";
            case "TR" -> "↗ справа сверху";
            case "L" -> "← слева по центру";
            case "C" -> "• центр";
            case "R" -> "→ справа по центру";
            case "BL" -> "↙ слева снизу";
            case "B" -> "↓ по центру снизу";
            default -> "↘ справа снизу";
        };
    }

    private void relayout() {
        if (this.w > 0) {
            this.layout();
        }
    }

    private List<String> imageNames() {
        List<String> names = new ArrayList<>();
        names.add("");

        for (Project.ImageRef r : this.project.images()) {
            names.add(r.name());
        }

        return names;
    }

    private static String trim(float f) {
        return f == (int)f ? String.valueOf((int)f) : String.valueOf(f);
    }

    private int formW() {
        return this.formOnly() ? this.w : Math.min(330, this.w / 2);
    }

    @Override
    public void layout() {
        int fw = this.formW();
        this.scroll.bounds(this.x, this.y, fw, this.h - 34);
        this.form.layout();
        this.scroll.layout();
        this.apply.bounds(this.x + 8, this.y + this.h - 27, this.apply.prefW, 20);
        this.reset.bounds(this.x + 14 + this.apply.prefW, this.y + this.h - 27, this.reset.prefW, 20);
        int px = this.x + fw + 10;
        int bx = px;
        int by = this.y + 6;

        for (Button b : List.of(this.hudBtn, this.chatBtn, this.choiceBtn, this.shopBtn, this.zoneBtn, this.questBtn)) {
            if (bx + b.prefW > this.x + this.w - 6 && bx > px) {
                bx = px;
                by += 22;
            }

            b.bounds(bx, by, b.prefW, 18);
            bx += b.prefW + 4;
        }

        this.top = by - this.y + 22;
        if (this.page != null) {
            this.top = -20;
            this.scroll.bounds(this.x, this.y, fw, this.h);
            this.form.layout();
            this.scroll.layout();
        }

        super.layout();
    }

    private boolean screenArea() {
        int fw = this.formW();
        int px = this.x + fw + 10;
        int py = this.y + this.top + 26;
        int pw = this.w - fw - 20;
        int ph = this.h - this.top - 26 - 30;
        if (pw >= 60 && ph >= 40) {
            this.k = Math.min(pw / 480.0F, ph / 270.0F);
            int sw = Math.round(480.0F * this.k);
            int sh = Math.round(270.0F * this.k);
            this.sx = px + (pw - sw) / 2;
            this.sy = py + (ph - sh) / 2;
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        int fw = this.formW();
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        c.fill(this.x, this.y, fw, this.h, -14934490);
        c.fill(this.x + fw, this.y, 1, this.h, -13749952);
        c.fill(this.x, this.y + this.h - 34, fw, 1, -13749952);
        int px = this.x + fw + 10;
        if (this.mode == EngineTab.Mode.HUD) {
            int cx = px;

            for (String key : EngineConfig.ELEMENTS) {
                if (this.present(key)) {
                    String t = EngineConfig.title(key);
                    int tw = c.width(t) + 12;
                    boolean sel = key.equals(this.selected);
                    boolean vis = this.cfg.element(key).visible;
                    boolean hov = mx >= cx && mx < cx + tw && my >= this.y + this.top + 2 && my < this.y + this.top + 18;
                    c.round(cx, this.y + this.top + 2, tw, 16, 3, sel ? -7639809 : (hov ? -14210506 : -15592678));
                    c.text(t, cx + 6, this.y + this.top + 6, sel ? -1 : (vis ? -1841682 : -9933435));
                    cx += tw + 4;
                }
            }
        }

        if (this.screenArea()) {
            int sw = Math.round(480.0F * this.k);
            int sh = Math.round(270.0F * this.k);
            c.shadow(this.sx, this.sy, sw, sh, 4, 8);
            c.pushClip(this.sx, this.sy, sw, sh);
            c.pushTransform(this.sx, this.sy, this.k);
            Gfx g = c.g;
            Font font = Minecraft.getInstance().font;
            g.fillGradient(0, 0, 480, 180, -9459492, -4597776);
            g.fillGradient(0, 180, 480, 270, -10646470, -12623065);
            HudPainter.State s = this.sampleState();
            if (this.mode == EngineTab.Mode.HUD) {
                for (String keyx : EngineConfig.ELEMENTS) {
                    if (this.present(keyx)) {
                        EngineConfig.Element e = this.cfg.element(keyx);
                        int[] r = HudPainter.rect(this.cfg, keyx, 480, 270, s);
                        if (e.visible) {
                            if (EngineConfig.gameDrawn(e.style)) {
                                HudPainter.standard(g, font, keyx, e.style, r, s);
                            } else {
                                HudPainter.custom(g, font, this.cfg, keyx, r, s);
                            }
                        } else if (keyx.equals(this.selected)) {
                            g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 872415231);
                        }
                    }
                }

                int[] r = HudPainter.rect(this.cfg, this.selected, 480, 270, s);
                g.renderOutline(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2, -11549705);
                if (this.canResize()) {
                    g.fill(r[0] + r[2] - 2, r[1] + r[3] - 2, r[0] + r[2] + 2, r[1] + r[3] + 2, -11549705);
                }
            } else if (this.mode == EngineTab.Mode.SHOP) {
                g.fill(0, 0, 480, 270, -2013265920);
                this.shopSample.style = this.cfg.shop;
                this.shopSample.layout(480, 270);
                this.shopSample.render(g, font, this.cfg, this.vx(mx), this.vy(my));
                int[] pr = this.shopSample.partRect(this.shopPart, font);
                g.renderOutline(pr[0] - 1, pr[1] - 1, pr[2] + 2, pr[3] + 2, -11549705);
            } else if (this.mode == EngineTab.Mode.CHOICE) {
                this.choicePreview(g, font);
            } else if (this.mode == EngineTab.Mode.QUEST) {
                EngineConfig.Quests q = this.cfg.quests;
                g.fill(0, 0, 480, 270, q.shadeColor);
                this.questK = Math.min(1.1836735F, 1.1043478F) * q.scale;
                this.questOX = (480.0F - 392.0F * this.questK) / 2.0F;
                this.questOY = (270.0F - 230.0F * this.questK) / 2.0F;
                this.questSample.styleFrom = this.cfg;
                g.pose().pushPose();
                g.pose().translate(this.questOX, this.questOY, 0.0F);

                try {
                    this.questSample.paintPreview(g, this.questK, this.vx(mx) - this.questOX, this.vy(my) - this.questOY);
                } catch (Throwable var19) {
                }

                g.pose().popPose();
                int[] o = q.off(this.questPart);
                int[] pr = this.questPart.equals("card") ? new int[]{150, 2, 240, 225} : new int[]{0, 0, 144, 230};
                g.renderOutline(
                    Math.round(this.questOX + (pr[0] + o[0]) * this.questK) - 1,
                    Math.round(this.questOY + (pr[1] + o[1]) * this.questK) - 1,
                    Math.round(pr[2] * this.questK) + 2,
                    Math.round(pr[3] * this.questK) + 2,
                    -11549705
                );
            } else if (this.mode == EngineTab.Mode.ZONE) {
                g.fill(0, 0, 480, 270, 1426063360);
                EngineConfig.ZoneTitle z = this.cfg.zoneTitle;
                long loop = (long)(z.seconds * 1000.0F) + 700L;
                long t = (System.currentTimeMillis() - this.zonePreviewAt) % loop;
                ZoneClient.drawTitle(g, font, 480, 270, z, this.cfg.project, "Деревня Ольховка", "Тихое место у реки", t);
                int cx = Math.round(480.0F * z.x);
                int cy = Math.round(270.0F * z.y);
                g.fill(cx - 3, cy, cx + 4, cy + 1, -11549705);
                g.fill(cx, cy - 3, cx + 1, cy + 4, -11549705);
            } else {
                g.fill(0, 0, 480, 270, -2013265920);
                int[] r = HudPainter.chatRect(480, 270);
                HudPainter.chatBack(g, this.cfg, r[0], r[1], r[2], r[3]);
                if (!this.cfg.chatLog.frameOnTop()) {
                    HudPainter.chatFrame(g, this.cfg, r[0], r[1], r[2], r[3]);
                }

                String[] lines = new String[]{"Алиса: Ты наконец проснулся?", "Игрок: Где я?", "Алиса: В деревне у реки. Пойдём,", "покажу дорогу."};
                int ly = 105;

                for (String l : lines) {
                    g.drawString(font, l, r[0] + r[2] / 6, ly, -1, true);
                    ly += 14;
                }

                if (this.cfg.chatLog.frameOnTop()) {
                    HudPainter.chatFrame(g, this.cfg, r[0], r[1], r[2], r[3]);
                }
            }

            c.popTransform();
            c.popClip();
            c.outline(this.sx - 1, this.sy - 1, sw + 2, sh + 2, -12762793);
            String hint;
            if (this.mode == EngineTab.Mode.CHAT) {
                hint = "Примерный вид окна истории сообщений";
            } else if (this.mode == EngineTab.Mode.QUEST) {
                int[] o = this.cfg.quests.off(this.questPart);
                hint = (this.questPart.equals("list") ? "Список квестов" : "Карточка квеста")
                    + "  сдвиг "
                    + o[0]
                    + ", "
                    + o[1]
                    + "   ·   тяните части журнала, стрелки — точный сдвиг";
            } else if (this.mode == EngineTab.Mode.ZONE) {
                hint = "Название зоны: щёлкните или тяните, чтобы поставить его центр  ("
                    + Math.round(this.cfg.zoneTitle.x * 100.0F)
                    + "%, "
                    + Math.round(this.cfg.zoneTitle.y * 100.0F)
                    + "%)";
            } else if (this.mode == EngineTab.Mode.SHOP) {
                int[] o = this.shopSample.off(this.shopPart);
                hint = ShopPanel.partTitle(this.shopPart)
                    + "  сдвиг "
                    + o[0]
                    + ", "
                    + o[1]
                    + "   ·   перетаскивайте части окна, стрелки — точный сдвиг, колесо — листать товары";
            } else if (this.mode == EngineTab.Mode.CHOICE) {
                int[] o = ChoiceLayout.offset(this.cfg.choice, this.choicePart);
                hint = ChoiceLayout.title(this.choicePart) + "  сдвиг " + o[0] + ", " + o[1] + "   ·   перетаскивайте части меню, стрелки — точный сдвиг";
            } else {
                int[] rx = HudPainter.rect(this.cfg, this.selected, 480, 270, s);
                hint = EngineConfig.title(this.selected)
                    + "  "
                    + rx[0]
                    + ", "
                    + rx[1]
                    + "  "
                    + rx[2]
                    + "×"
                    + rx[3]
                    + "   ·   перетаскивайте элементы, стрелки — сдвиг, колёсико — заполненность "
                    + Math.round(this.sample * 100.0F)
                    + "%";
            }

            c.textEllipsized(hint, px, this.y + this.h - 22, this.w - fw - 20, -9933435);
        }
    }

    private void choicePreview(Gfx g, Font font) {
        EngineConfig.Choice ch = this.cfg.choice;
        if (!ch.style.equals("spraute")) {
            g.drawCenteredString(font, "Меню выбора Spraute", 240, 135, -1);
        } else {
            LivingEntity subject = ch.show.equals("none") ? null : ChoiceScreen.previewSubject(ch);
            ChoiceLayout.draw(
                g, font, this.cfg, 480, 270, new ChoiceLayout.Content("Readix", "Пойдёшь со мной?", 999, SAMPLE_ANSWERS, 0, subject, 240.0F, 135.0F)
            );
            int[] r = ChoiceLayout.rects(ch, font, 480, 270, SAMPLE_ANSWERS).get(this.choicePart);
            if (r != null) {
                int ry = this.choicePart.equals("panel") ? r[1] + 190 : r[1];
                int rh = this.choicePart.equals("panel") ? r[3] - 190 : r[3];
                g.renderOutline(r[0] - 1, ry - 1, r[2] + 2, rh + 2, -11549705);
            }
        }
    }

    private boolean choicePress(double mx, double my) {
        double px = this.vx(mx);
        double py = this.vy(my);
        if (!(px < 0.0) && !(py < 0.0) && !(px > 480.0) && !(py > 270.0)) {
            Font font = Minecraft.getInstance().font;
            String hit = ChoiceLayout.partAt(this.cfg.choice, font, 480, 270, SAMPLE_ANSWERS, px, py);
            if (hit == null) {
                return true;
            } else {
                this.choicePart = hit;
                int[] o = ChoiceLayout.offset(this.cfg.choice, hit);
                this.choiceDragStart = new int[]{o[0], o[1]};
                this.dragging = true;
                this.moved = false;
                this.dragX = px;
                this.dragY = py;
                this.buildForm();
                return true;
            }
        } else {
            return false;
        }
    }

    private void choiceMove(int dx, int dy, boolean fromStart) {
        int[] base = fromStart ? this.choiceDragStart : ChoiceLayout.offset(this.cfg.choice, this.choicePart);
        this.cfg.choice.offsets.put(this.choicePart, new int[]{base[0] + dx, base[1] + dy});
    }

    private boolean canResize() {
        return !EngineConfig.gameDrawn(this.cfg.element(this.selected).style);
    }

    private double vx(double mx) {
        return (mx - this.sx) / this.k;
    }

    private double vy(double my) {
        return (my - this.sy) / this.k;
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (super.mousePressed(mx, my, button)) {
            return true;
        } else {
            int fw = this.formW();
            if (mx >= this.x + fw && this.mode == EngineTab.Mode.CHOICE && this.screenArea() && button == 0) {
                return this.choicePress(mx, my);
            } else if (mx >= this.x + fw && this.mode == EngineTab.Mode.QUEST && this.screenArea() && button == 0) {
                double px = this.vx(mx);
                double py = this.vy(my);
                if (!(px < 0.0) && !(py < 0.0) && !(px > 480.0) && !(py > 270.0)) {
                    String part = this.questPartAt((px - this.questOX) / this.questK, (py - this.questOY) / this.questK);
                    if (part == null) {
                        return true;
                    } else {
                        if (!part.equals(this.questPart)) {
                            this.questPart = part;
                            this.buildForm();
                        }

                        this.questDragStart = (int[])this.cfg.quests.off(this.questPart).clone();
                        this.dragging = true;
                        this.moved = false;
                        this.dragX = px;
                        this.dragY = py;
                        return true;
                    }
                } else {
                    return false;
                }
            } else if (mx >= this.x + fw && this.mode == EngineTab.Mode.ZONE && this.screenArea() && button == 0) {
                double px = this.vx(mx);
                double py = this.vy(my);
                if (!(px < 0.0) && !(py < 0.0) && !(px > 480.0) && !(py > 270.0)) {
                    this.cfg.zoneTitle.x = (float)Math.round(px / 480.0 * 100.0) / 100.0F;
                    this.cfg.zoneTitle.y = (float)Math.round(py / 270.0 * 100.0) / 100.0F;
                    this.dragging = true;
                    this.moved = true;
                    this.zoneDrag = true;
                    return true;
                } else {
                    return false;
                }
            } else if (mx >= this.x + fw && this.mode == EngineTab.Mode.SHOP && this.screenArea() && button == 0) {
                double px = this.vx(mx);
                double py = this.vy(my);
                if (!(px < 0.0) && !(py < 0.0) && !(px > 480.0) && !(py > 270.0)) {
                    String part = this.shopPart.equals("window") ? "window" : this.shopSample.partAt(px, py, Minecraft.getInstance().font);
                    if (part == null) {
                        return true;
                    } else {
                        if (!part.equals(this.shopPart)) {
                            this.shopPart = part;
                            this.buildForm();
                        }

                        this.shopDragStart = (int[])this.shopSample.off(this.shopPart).clone();
                        this.dragging = true;
                        this.moved = false;
                        this.dragX = px;
                        this.dragY = py;
                        return true;
                    }
                } else {
                    return false;
                }
            } else if (!(mx < this.x + fw) && this.mode == EngineTab.Mode.HUD) {
                int cx = this.x + fw + 10;
                Font font = Minecraft.getInstance().font;

                for (String key : EngineConfig.ELEMENTS) {
                    if (this.present(key)) {
                        int tw = font.width(EngineConfig.title(key)) + 12;
                        if (mx >= cx && mx < cx + tw && my >= this.y + this.top + 2 && my < this.y + this.top + 18) {
                            this.select(key);
                            return true;
                        }

                        cx += tw + 4;
                    }
                }

                if (this.screenArea() && button == 0) {
                    double px = this.vx(mx);
                    double py = this.vy(my);
                    if (!(px < 0.0) && !(py < 0.0) && !(px > 480.0) && !(py > 270.0)) {
                        HudPainter.State s = this.sampleState();
                        int[] sel = HudPainter.rect(this.cfg, this.selected, 480, 270, s);
                        this.resizing = this.canResize() && Math.abs(px - (sel[0] + sel[2])) <= 4.0 && Math.abs(py - (sel[1] + sel[3])) <= 4.0;
                        if (!this.resizing) {
                            String hit = null;
                            List<String> order = new ArrayList<>(EngineConfig.ELEMENTS);
                            Collections.reverse(order);

                            for (String keyx : order) {
                                if (this.present(keyx) && this.cfg.element(keyx).visible) {
                                    int[] r = HudPainter.rect(this.cfg, keyx, 480, 270, s);
                                    if (px >= r[0] - 2 && px <= r[0] + r[2] + 2 && py >= r[1] - 2 && py <= r[1] + r[3] + 2) {
                                        hit = keyx;
                                        break;
                                    }
                                }
                            }

                            if (hit == null) {
                                return true;
                            }

                            if (!hit.equals(this.selected)) {
                                this.select(hit);
                            }
                        }

                        EngineConfig.Element e = this.cfg.element(this.selected);
                        this.dragging = true;
                        this.moved = false;
                        this.dragX = px;
                        this.dragY = py;
                        this.dragStart = HudPainter.rect(this.cfg, this.selected, 480, 270, s);
                        if (this.resizing) {
                            this.dragStart = new int[]{this.dragStart[0], this.dragStart[1], e.width, e.height};
                        }

                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!this.dragging) {
            return super.mouseDragged(mx, my, button, dx, dy);
        } else {
            int ddx = (int)Math.round(this.vx(mx) - this.dragX);
            int ddy = (int)Math.round(this.vy(my) - this.dragY);
            if (!this.moved && Math.abs(ddx) + Math.abs(ddy) < 2) {
                return true;
            } else if (this.mode == EngineTab.Mode.CHOICE) {
                this.choiceMove(ddx, ddy, true);
                this.moved = true;
                return true;
            } else if (this.mode == EngineTab.Mode.QUEST) {
                this.cfg
                    .quests
                    .offsets
                    .put(
                        this.questPart,
                        new int[]{this.questDragStart[0] + Math.round(ddx / this.questK), this.questDragStart[1] + Math.round(ddy / this.questK)}
                    );
                this.moved = true;
                return true;
            } else if (this.mode == EngineTab.Mode.ZONE && this.zoneDrag) {
                this.cfg.zoneTitle.x = (float)Math.max(0.0, Math.min(1.0, Math.round(this.vx(mx) / 480.0 * 100.0) / 100.0));
                this.cfg.zoneTitle.y = (float)Math.max(0.0, Math.min(1.0, Math.round(this.vy(my) / 270.0 * 100.0) / 100.0));
                return true;
            } else if (this.mode == EngineTab.Mode.SHOP) {
                this.cfg.shop.offsets.put(this.shopPart, new int[]{this.shopDragStart[0] + ddx, this.shopDragStart[1] + ddy});
                this.moved = true;
                return true;
            } else {
                EngineConfig.Element e = this.cfg.element(this.selected);
                if (this.resizing) {
                    String wasCustom = e.style;
                    switch (wasCustom) {
                        case "icons":
                        case "slots":
                        case "icon":
                            e.height = Math.max(4, this.dragStart[3] + ddy);
                            break;
                        default:
                            e.width = Math.max(4, this.dragStart[2] + ddx);
                            e.height = Math.max(2, this.dragStart[3] + ddy);
                    }
                } else {
                    boolean wasCustom = e.customPos;
                    if (!e.customPos) {
                        e.customPos = true;
                        e.anchor = nearestAnchor(this.dragStart);
                    }

                    int nx = this.dragStart[0] + ddx;
                    int ny = this.dragStart[1] + ddy;
                    int cxm = nx + this.dragStart[2] / 2;
                    if (Math.abs(cxm - 240) <= 3) {
                        nx = 240 - this.dragStart[2] / 2;
                    }

                    this.place(this.selected, e, new int[]{nx, ny, this.dragStart[2], this.dragStart[3]});
                    if (!wasCustom) {
                        this.buildForm();
                    }
                }

                this.moved = true;
                return true;
            }
        }
    }

    private static String nearestAnchor(int[] r) {
        int cx = r[0] + r[2] / 2;
        int cy = r[1] + r[3] / 2;
        String col = cx < 160 ? "L" : (cx > 320 ? "R" : "");
        String row = cy < 90 ? "T" : (cy > 180 ? "B" : "");
        if (row.isEmpty() && col.isEmpty()) {
            return "C";
        } else if (row.isEmpty()) {
            return col;
        } else {
            return col.isEmpty() ? row : row + col;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (this.dragging) {
            this.dragging = false;
            this.resizing = false;
            return true;
        } else {
            return super.mouseReleased(mx, my, button);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx > this.x + this.formW() && this.mode == EngineTab.Mode.SHOP) {
            this.shopSample.scroll(delta);
            return true;
        } else if (mx > this.x + this.formW() && this.mode == EngineTab.Mode.HUD) {
            this.sample = Math.max(0.0F, Math.min(1.0F, this.sample + (float)delta * 0.05F));
            return true;
        } else {
            return super.mouseScrolled(mx, my, delta);
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (super.keyPressed(key, scan, mods)) {
            return true;
        } else if ((
                this.mode == EngineTab.Mode.HUD || this.mode == EngineTab.Mode.CHOICE || this.mode == EngineTab.Mode.SHOP || this.mode == EngineTab.Mode.QUEST
            )
            && this.isHoveredDeep()) {
            int step = (mods & 1) != 0 ? 10 : 1;

            int dx = switch (key) {
                case 262 -> step;
                case 263 -> -step;
                default -> 0;
            };

            int dy = switch (key) {
                case 264 -> step;
                case 265 -> -step;
                default -> 0;
            };
            if (dx == 0 && dy == 0) {
                return false;
            } else if (this.mode == EngineTab.Mode.CHOICE) {
                this.choiceMove(dx, dy, false);
                return true;
            } else if (this.mode == EngineTab.Mode.SHOP) {
                int[] o = this.shopSample.off(this.shopPart);
                this.cfg.shop.offsets.put(this.shopPart, new int[]{o[0] + dx, o[1] + dy});
                return true;
            } else if (this.mode == EngineTab.Mode.QUEST) {
                int[] o = this.cfg.quests.off(this.questPart);
                this.cfg.quests.offsets.put(this.questPart, new int[]{o[0] + dx, o[1] + dy});
                return true;
            } else {
                EngineConfig.Element e = this.cfg.element(this.selected);
                if (!e.customPos) {
                    int[] r = HudPainter.rect(this.cfg, this.selected, 480, 270, this.sampleState());
                    e.customPos = true;
                    e.anchor = nearestAnchor(r);
                    this.place(this.selected, e, r);
                    this.buildForm();
                }

                int[] r = HudPainter.rect(this.cfg, this.selected, 480, 270, this.sampleState());
                this.place(this.selected, e, new int[]{r[0] + dx, r[1] + dy, r[2], r[3]});
                return true;
            }
        } else {
            return false;
        }
    }

    private void select(String key) {
        this.selected = key;
        this.buildForm();
    }

    @Override
    public String title() {
        return "Настройки движка";
    }

    @Override
    public Icon icon() {
        return Icon.GEAR;
    }

    @Override
    public int iconColor() {
        return -7639809;
    }

    @Override
    public boolean isDirty() {
        return !this.cfg.toJson().equals(this.savedJson);
    }

    @Override
    public void save() throws IOException {
        this.cfg.project = this.project.name();
        String json = this.cfg.toJson();
        Files.writeString(file(this.project), json, StandardCharsets.UTF_8);
        this.savedJson = json;
        if (Minecraft.getInstance().getConnection() != null) {
            ClientState.get().engine(json);
        }
    }

    @Override
    public String source() {
        return null;
    }

    @Override
    public int scriptId() {
        return -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        return List.of();
    }

    @Override
    public Analyzer.Result analysis() {
        return null;
    }

    @Override
    public void reveal(int offset) {
    }

    @Override
    public void focusContent() {
    }

    private final class Form extends Widget {
        void header(final String text) {
            this.add(new Widget() {
                {
                    this.prefH = 26;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.fill(this.x + 8, this.y + 8, this.w - 16, 1, -13749952);
                    c.text(text.toUpperCase(), this.x + 10, this.y + 14, -7639809);
                }
            });
        }

        void note(final String text) {
            this.add(new Widget() {
                {
                    this.prefH = 13;
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.textEllipsized(text, this.x + 10, this.y + 2, this.w - 16, -9933435);
                }
            }).tooltip(text);
        }

        void button(String label, String doc, Runnable action) {
            final Button b = new Button(label, action).style(Button.Style.DEFAULT);
            if (doc != null) {
                b.tooltip(doc);
            }

            this.add(new Widget() {
                final Button btn = this.add(b);

                {
                    this.prefH = 24;
                }

                @Override
                public void layout() {
                    this.btn.bounds(this.x + 10, this.y + 2, Math.max(60, this.w - 20), 20);
                    super.layout();
                }
            });
        }

        void toggle(String label, String doc, BooleanSupplier get, Consumer<Boolean> set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            r.editor = r.add(new Checkbox("", get, set));
            if (doc != null) {
                r.tooltip(doc);
            }
        }

        <T> void dropdown(String label, String doc, Supplier<List<T>> options, Supplier<T> get, Function<T, String> labeler, Consumer<T> set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            r.editor = r.add(new Dropdown<>(options, get, labeler, set));
            if (doc != null) {
                r.tooltip(doc);
            }
        }

        void number(String label, final IntSupplier get, IntConsumer set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            TextField f = new TextField() {
                @Override
                public void tick() {
                    super.tick();
                    if (!this.isFocused()) {
                        String s = String.valueOf(get.getAsInt());
                        if (!s.equals(this.text())) {
                            this.setText(s);
                        }
                    }
                }
            };
            f.setText(String.valueOf(get.getAsInt()));
            f.filter = ch -> Character.isDigit(ch) || ch == '-';
            f.onChange = s -> {
                try {
                    set.accept(Integer.parseInt(s.trim()));
                } catch (NumberFormatException var3x) {
                }
            };
            r.editor = r.add(f);
        }

        void decimal(String label, final DoubleSupplier get, DoubleConsumer set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            TextField f = new TextField() {
                @Override
                public void tick() {
                    super.tick();
                    if (!this.isFocused()) {
                        String s = EngineTab.trim((float)get.getAsDouble());
                        if (!s.equals(this.text())) {
                            this.setText(s);
                        }
                    }
                }
            };
            f.setText(EngineTab.trim((float)get.getAsDouble()));
            f.filter = ch -> Character.isDigit(ch) || ch == '-' || ch == '.';
            f.onChange = s -> {
                try {
                    set.accept(Double.parseDouble(s.trim()));
                } catch (NumberFormatException var3x) {
                }
            };
            r.editor = r.add(f);
        }

        void item(String label, String doc, Supplier<String> get, Consumer<String> set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            r.editor = r.add(
                new Button(() -> get.get().isEmpty() ? "выбрать…" : get.get().replace("minecraft:", ""), () -> AssetPicker.open(ParamType.ITEM, get.get(), set))
                    .icon(Icon.CUBE)
                    .style(Button.Style.GHOST)
            );
            r.tooltip(doc);
        }

        void action(String label, Runnable run) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(""));
            r.editor = r.add(new Button(label, run).icon(Icon.CLOSE).style(Button.Style.GHOST));
        }

        void text(String label, String doc, Supplier<String> get, Consumer<String> set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            TextField f = new TextField();
            f.setText(get.get());
            f.onChange = set::accept;
            r.editor = r.add(f);
            if (doc != null) {
                r.tooltip(doc);
            }
        }

        void color(String label, final IntSupplier get, final IntConsumer set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            final TextField f = new TextField();
            f.setText(String.format("#%06X", get.getAsInt() & 16777215));
            f.onChange = s -> {
                String h = s.trim().replace("#", "");

                try {
                    if (h.length() == 6) {
                        set.accept(0xFF000000 | Integer.parseInt(h, 16));
                    }
                } catch (NumberFormatException var4x) {
                }
            };
            r.editor = r.add(new Widget() {
                final TextField field = this.add(f);

                @Override
                public void layout() {
                    this.field.bounds(this.x + 24, this.y, this.w - 24, this.h);
                    super.layout();
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.round(this.x, this.y, 20, this.h, 3, -13749952);
                    c.round(this.x + 1, this.y + 1, 18, this.h - 2, 2, 0xFF000000 | get.getAsInt());
                }

                @Override
                public boolean mousePressed(double mx, double my, int button) {
                    if (mx < this.x + 22) {
                        ColorPicker.open(0xFF000000 | get.getAsInt(), this.x - 150, this.y + this.h + 2, v -> {
                            set.accept(0xFF000000 | v);
                            f.setText(String.format("#%06X", v & 16777215));
                        });
                        return true;
                    } else {
                        return super.mousePressed(mx, my, button);
                    }
                }
            });
            r.tooltip("Цвет #RRGGBB; щелчок по квадрату — палитра");
        }

        void colorAlpha(String label, final IntSupplier get, final IntConsumer set) {
            EngineTab.Row r = this.add(EngineTab.this.new Row(label));
            final TextField f = new TextField();
            f.setText(String.format("#%08X", get.getAsInt()));
            f.onChange = s -> {
                String h = s.trim().replace("#", "");

                try {
                    if (h.length() == 8) {
                        set.accept((int)Long.parseLong(h, 16));
                    }
                } catch (NumberFormatException var4x) {
                }
            };
            r.editor = r.add(new Widget() {
                final TextField field = this.add(f);

                @Override
                public void layout() {
                    this.field.bounds(this.x + 24, this.y, this.w - 24, this.h);
                    super.layout();
                }

                @Override
                protected void paint(Canvas c, int mx, int my, float dt) {
                    c.round(this.x, this.y, 20, this.h, 3, -7829368);
                    c.round(this.x + 1, this.y + 1, 18, this.h - 2, 2, get.getAsInt());
                }

                @Override
                public boolean mousePressed(double mx, double my, int button) {
                    if (mx < this.x + 22) {
                        ColorPicker.open(get.getAsInt(), this.x - 150, this.y + this.h + 2, v -> {
                            set.accept(v);
                            f.setText(String.format("#%08X", v));
                        });
                        return true;
                    } else {
                        return super.mousePressed(mx, my, button);
                    }
                }
            });
            r.tooltip("Цвет #AARRGGBB; 00 в начале — без цветного фона");
        }

        void skin(String label, EngineConfig.Skin skin) {
            this.add(EngineTab.this.new SkinRow(label, skin));
        }

        @Override
        public void layout() {
            int cy = this.y;

            for (Widget c : this.children()) {
                c.bounds(this.x, cy, this.w, c.prefH);
                c.layout();
                cy += c.prefH;
            }

            this.prefH = cy - this.y + 8;
        }
    }

    private static enum Mode {
        HUD,
        CHAT,
        CHOICE,
        SHOP,
        ZONE,
        QUEST;
    }

    private class Row extends Widget {
        final String label;
        Widget editor;

        Row(String label) {
            this.label = label;
            this.prefH = 22;
        }

        @Override
        public void layout() {
            if (this.editor != null) {
                this.editor.bounds(this.x + 104, this.y + 2, this.w - 104 - 10, 18);
                this.editor.layout();
            }
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            c.textEllipsized(this.label, this.x + 10, this.y + 7, 90, -6577995);
        }
    }

    private static final class ShopPanelParts {
        static final List<String> ALL = ShopPanel.PARTS;
    }

    public record SkillsPages(List<String> pages, Function<String, String> title, Supplier<String> now, Consumer<String> open) {
    }

    private final class SkinRow extends EngineTab.Row {
        final EngineConfig.Skin skin;
        final Checkbox nine;
        final Checkbox tileCenter;
        final Checkbox tileEdges;
        final TextField[] borders = new TextField[5];

        SkinRow(String label, EngineConfig.Skin skin) {
            super(label);
            this.skin = skin;
            Dropdown<String> dd = new Dropdown<>(
                EngineTab.this::imageNames, () -> skin.image != null && !skin.image.isEmpty() ? skin.image : null, v -> v.isEmpty() ? "— нет —" : v, v -> {
                    skin.image = v;
                    this.sizeBorders();
                }
            );
            dd.placeholder = "— нет (картинка из «Картинок») —";
            this.editor = this.add(dd);
            this.nine = this.add(new Checkbox("9-slice", () -> skin.nine, v -> {
                skin.nine = v;
                EngineTab.this.relayout();
            }));
            this.nine.tooltip("Углы не растягиваются. Края и середину можно растянуть или повторять. Для рамок, панелей и кнопок");
            this.tileCenter = this.add(new Checkbox("Тайлинг внутри", () -> skin.tileCenter, v -> skin.tileCenter = v));
            this.tileEdges = this.add(new Checkbox("Тайлинг краёв", () -> skin.tileEdges, v -> skin.tileEdges = v));
            String[] names = new String[]{"Слева", "Сверху", "Справа", "Снизу", "Масштаб"};

            for (int i = 0; i < 5; i++) {
                TextField f = this.add(new TextField());
                int idx = i;
                f.setText(i == 4 ? EngineTab.trim(skin.scale) : String.valueOf(new int[]{skin.left, skin.top, skin.right, skin.bottom}[i]));
                f.filter = ch -> Character.isDigit(ch) || idx == 4 && ch == '.';
                f.onChange = s -> this.setBorder(idx, s);
                f.tooltip(
                    i == 4 ? "Масштаб углов: 1 — пиксель картинки = пиксель интерфейса, 2 — вдвое крупнее" : names[i] + ": ширина края в пикселях картинки"
                );
                this.borders[i] = f;
            }
        }

        private void sizeBorders() {
            if (this.skin.isSet()) {
                for (Project.ImageRef r : EngineTab.this.project.images()) {
                    if (r.name().equals(this.skin.image)) {
                        ProjectImages.Loaded l = ProjectImages.load(EngineTab.this.project, r);
                        if (l == null) {
                            return;
                        }

                        int b = Math.max(1, Math.min(l.width(), l.height()) / 4);
                        this.skin.left = this.skin.top = this.skin.right = this.skin.bottom = b;

                        for (int i = 0; i < 4; i++) {
                            this.borders[i].setText(String.valueOf(b));
                        }
                    }
                }
            }
        }

        private void setBorder(int i, String s) {
            try {
                if (i == 4) {
                    this.skin.scale = Math.max(0.1F, Float.parseFloat(s.trim()));
                    return;
                }

                int v = Math.max(0, Integer.parseInt(s.trim()));
                switch (i) {
                    case 0:
                        this.skin.left = v;
                        break;
                    case 1:
                        this.skin.top = v;
                        break;
                    case 2:
                        this.skin.right = v;
                        break;
                    default:
                        this.skin.bottom = v;
                }
            } catch (NumberFormatException var4) {
            }
        }

        @Override
        public void layout() {
            this.prefH = this.skin.nine ? 88 : 42;
            int ew = this.w - 104 - 10;
            this.editor.bounds(this.x + 104, this.y + 2, ew, 18);
            this.editor.layout();
            this.nine.bounds(this.x + 104, this.y + 23, 70, 16);
            int fw = Math.max(24, (ew - 16) / 5);

            for (int i = 0; i < 5; i++) {
                TextField f = this.borders[i];
                f.visible = this.skin.nine;
                f.bounds(this.x + 104 + i * (fw + 4), this.y + 44, fw, 18);
                f.layout();
            }

            this.tileCenter.visible = this.tileEdges.visible = this.skin.nine;
            this.tileCenter.bounds(this.x + 104, this.y + 67, Math.max(100, ew / 2), 16);
            this.tileEdges.bounds(this.x + 104 + Math.max(100, ew / 2), this.y + 67, Math.max(95, ew / 2), 16);
        }

        @Override
        protected void paint(Canvas c, int mx, int my, float dt) {
            super.paint(c, mx, my, dt);
            if (this.skin.nine) {
                c.text("края и масштаб", this.x + 104 + 76, this.y + 27, -9933435);
            }
        }
    }
}
