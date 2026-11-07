package org.sprautestudio.spraute_engine.gui.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.platform.Window;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.ChatOptionsScreen;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.MouseSettingsScreen;
import net.minecraft.client.gui.screens.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.SkinCustomizationScreen;
import net.minecraft.client.gui.screens.SoundOptionsScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.client.gui.screens.WinScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BlastFurnaceScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.HorseInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.LecternScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.SmokerScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SmokerMenu;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxWidget;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.export.BundledProjects;
import org.sprautestudio.spraute_engine.general.client.GeneralRuntime;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.MenuDoc;
import org.sprautestudio.spraute_engine.mixin.WidgetSizeMixin;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class MenuRuntime {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String OUR_CLASSES = "org.sprautestudio.";
    private static final List<MenuRuntime.Known> KNOWN = new ArrayList<>();
    private static MenuRuntime.Store store;
    private static boolean reading;
    private static boolean hiding;
    private static Screen shown;
    private static String shownRole = "";
    private static int shownW;
    private static int shownH;
    private static boolean shownBroken;
    private static final int NO_MOUSE = -10000;
    private static Boolean previewSplash;
    private static final Map<String, GuiView> VIEWS = new LinkedHashMap<>();
    private static final Map<String, String> VIEW_JSON = new LinkedHashMap<>();
    private static boolean pictures;
    private static Screen lastOpened;

    private MenuRuntime() {
    }

    private static void known(String key, String title, Class<? extends Screen> type) {
        KNOWN.add(new MenuRuntime.Known(key, title, type, null));
    }

    private static void known(String key, String title, Class<? extends Screen> type, Supplier<Screen> make) {
        KNOWN.add(new MenuRuntime.Known(key, title, type, make));
    }

    private static Screen withInventory(Function<Inventory, Screen> make) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : make.apply(player.getInventory());
    }

    private static Path file() {
        return Platform.get().gameDir().resolve("config").resolve("spraute_menus.json");
    }

    private static MenuRuntime.Store store() {
        if (store != null) {
            return store;
        } else {
            store = new MenuRuntime.Store();
            Path f = file();
            if (Files.exists(f)) {
                try {
                    MenuRuntime.Store read = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), (new TypeToken<MenuRuntime.Store>() {}).getType());
                    if (read != null) {
                        if (read.project != null) {
                            store.project = read.project;
                        }

                        if (read.layouts != null) {
                            store.layouts = read.layouts;
                        }

                        if (read.seen != null) {
                            store.seen = read.seen;
                        }
                    }
                } catch (Exception var2) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read {}", f, var2);
                }
            }

            return store;
        }
    }

    private static void save() {
        Path f = file();

        try {
            Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(store()), StandardCharsets.UTF_8);
        } catch (IOException var2) {
            SprauteEngine.LOGGER.warn("Spraute: cannot write {}", f, var2);
        }
    }

    public static boolean isMenu(String role) {
        return role.startsWith("menu:");
    }

    private static String roleKey(String role) {
        return role.substring("menu:".length());
    }

    public static List<String> roles() {
        for (MenuRuntime.Known k : KNOWN) {
            if (k.make != null && !store().seen.containsKey(k.key)) {
                readNow(k.key);
            }
        }

        List<String> out = new ArrayList<>();

        for (MenuRuntime.Known kx : KNOWN) {
            if (worthShowing(kx.key) || kx.key.equals(KNOWN.get(0).key) || kx.make != null && MenuLook.hasBack(kx.key)) {
                out.add("menu:" + kx.key);
            }
        }

        for (String key : store().seen.keySet()) {
            String role = "menu:" + key;
            if (!out.contains(role) && worthShowing(key)) {
                out.add(role);
            }
        }

        return out;
    }

    private static boolean worthShowing(String key) {
        MenuRuntime.Seen seen = store().seen.get(key);
        String layout = store().layouts.get("menu:" + key);
        GuiDoc own = null;
        if (layout != null && !layout.isBlank()) {
            own = GuiDoc.fromJson(layout);
            own.elements.removeIf(e -> e.type.equals("menu_widget"));
        }

        return MenuDoc.worthShowing(seen != null ? seen.parts.size() : 0, own);
    }

    public static void drawReal(Gfx g, String role, int width, int height) {
        if (!role.equals(shownRole) || shownW != width || shownH != height) {
            shown = makeReal(roleKey(role), width, height);
            shownRole = role;
            shownW = width;
            shownH = height;
            shownBroken = false;
        }

        if (shown != null && !shownBroken) {
            MenuLook.slotsOf(shown);

            try {
                Mc.renderScreen(shown, g, -10000, -10000, 0.0F);
            } catch (Throwable var5) {
                shownBroken = true;
                SprauteEngine.LOGGER.warn("Spraute: cannot show the menu {} in the editor", role, var5);
            }
        }
    }

    public static void preview(String role, GuiDoc doc, String project) {
        String key = roleKey(role);
        MenuLook.apply(key, doc, project);
        if (key.equals("title")) {
            boolean hide = "true".equals(doc.get("hideSplash"));
            if (previewSplash == null || previewSplash != hide) {
                shownRole = "";
            }

            previewSplash = hide;
        }
    }

    public static boolean splashHidden() {
        if (previewSplash != null) {
            return previewSplash;
        } else {
            GuiView view = view("menu:title");
            return view != null && "true".equals(view.doc.get("hideSplash"));
        }
    }

    public static boolean flag(Object screen, String prop) {
        return screen instanceof Screen s && MenuLook.flag(keyOf(s), prop);
    }

    private static Screen makeReal(String key, int width, int height) {
        for (MenuRuntime.Known k : KNOWN) {
            if (k.key.equals(key) && k.make != null) {
                Screen open = Minecraft.getInstance().screen;
                reading = true;

                Object var7;
                try {
                    Screen s = k.make.get();
                    if (s != null) {
                        hiding = true;
                        s.init(Minecraft.getInstance(), width, height);
                        return s;
                    }

                    var7 = null;
                } catch (Throwable var11) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read the menu {}", key, var11);
                    continue;
                } finally {
                    reading = false;
                    hiding = false;
                    if (Minecraft.getInstance().screen != open) {
                        Minecraft.getInstance().setScreen(open);
                    }
                }

                return (Screen)var7;
            }
        }

        return null;
    }

    public static String title(String role) {
        String key = roleKey(role);

        for (MenuRuntime.Known k : KNOWN) {
            if (k.key.equals(key)) {
                return k.title;
            }
        }

        return "Экран мода: " + key.substring(key.lastIndexOf(46) + 1);
    }

    public static String build(String role) {
        String key = roleKey(role);
        if (!store().seen.containsKey(key)) {
            readNow(key);
        }

        MenuRuntime.Seen seen = store().seen.get(key);
        Window window = Minecraft.getInstance().getWindow();
        int w = seen != null ? seen.width : window.getGuiScaledWidth();
        int h = seen != null ? seen.height : window.getGuiScaledHeight();
        return MenuDoc.build(title(role), w, h, seen != null ? seen.parts : List.of()).toJson();
    }

    private static void readNow(String key) {
        for (MenuRuntime.Known k : KNOWN) {
            if (k.key.equals(key) && k.make != null) {
                reading = true;

                try {
                    Window window = Minecraft.getInstance().getWindow();
                    Screen s = k.make.get();
                    if (s != null) {
                        Screen open = Minecraft.getInstance().screen;
                        s.init(Minecraft.getInstance(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
                        s.removed();
                        if (Minecraft.getInstance().screen != open) {
                            Minecraft.getInstance().setScreen(open);
                        }
                    }
                } catch (Throwable var9) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read the menu {}", key, var9);
                } finally {
                    reading = false;
                }
            }
        }
    }

    public static void saved(String project, Map<String, String> layouts) {
        MenuRuntime.Store s = store();
        s.project = project;
        s.layouts.clear();
        layouts.forEach((role, json) -> {
            if (isMenu(role)) {
                s.layouts.put(role, json);
            }
        });
        save();
        VIEWS.clear();
    }

    private static String keyOf(Screen screen) {
        for (MenuRuntime.Known k : KNOWN) {
            if (k.type == screen.getClass()) {
                return k.key;
            }
        }

        return screen.getClass().getName();
    }

    public static void ready(Screen screen, List<GuiEventListener> children, List renderables) {
        try {
            if (screen.getClass().getName().startsWith("org.sprautestudio.")) {
                return;
            }

            children.removeIf(c -> c instanceof MenuRuntime.Layer);
            renderables.removeIf(r -> r instanceof MenuRuntime.Layer);
            Map<String, AbstractWidget> widgets = widgets(renderables);
            String key = keyOf(screen);
            learn(key, screen, widgets);
            if (hiding) {
                widgets.values().forEach(wx -> wx.visible = false);
            }

            if (reading) {
                return;
            }

            if (!hiding && lastOpened != screen) {
                lastOpened = screen;
                GeneralRuntime.menu(key);
            }

            GuiView view = view("menu:" + key);
            if (!hiding) {
                previewSplash = null;
                MenuLook.opened();
                MenuLook.apply(key, view != null ? view.doc : null, view != null ? view.project : null);
            }

            if (view == null) {
                return;
            }

            for (GuiDoc.Element e : view.doc.all()) {
                if (e.type.equals("menu_widget")) {
                    AbstractWidget w = widgets.get(e.id);
                    if (w != null) {
                        change(w, MenuDoc.change(e));
                    }
                }
            }

            MenuRuntime.Layer layer = new MenuRuntime.Layer(screen, view);
            renderables.add(0, layer);
            children.add(0, layer);
        } catch (RuntimeException var9) {
            SprauteEngine.LOGGER.warn("Spraute: cannot change the screen {}", screen.getClass().getName(), var9);
        }
    }

    private static Map<String, AbstractWidget> widgets(List<?> renderables) {
        Map<String, AbstractWidget> out = new LinkedHashMap<>();
        Set<String> used = new HashSet<>();
        int index = 0;

        for (Object r : renderables) {
            if (r instanceof AbstractWidget w) {
                String translation = w.getMessage().getContents() instanceof TranslatableContents t ? t.getKey() : null;
                out.put(MenuDoc.key(translation, index++, used), w);
            }
        }

        return out;
    }

    private static void learn(String key, Screen screen, Map<String, AbstractWidget> widgets) {
        MenuRuntime.Seen seen = new MenuRuntime.Seen();
        seen.width = screen.width;
        seen.height = screen.height;
        widgets.forEach((k, w) -> seen.parts.add(new MenuDoc.Part(k, Mc.widgetX(w), Mc.widgetY(w), w.getWidth(), w.getHeight(), w.getMessage().getString())));
        MenuRuntime.Seen before = store().seen.get(key);
        if (before == null || !sameButtons(before, seen)) {
            if (before == null || view("menu:" + key) == null) {
                store().seen.put(key, seen);
                save();
            }
        }
    }

    private static boolean sameButtons(MenuRuntime.Seen a, MenuRuntime.Seen b) {
        if (a.parts.size() != b.parts.size()) {
            return false;
        } else {
            for (int i = 0; i < a.parts.size(); i++) {
                if (!a.parts.get(i).key().equals(b.parts.get(i).key())) {
                    return false;
                }
            }

            return true;
        }
    }

    private static void change(AbstractWidget w, MenuDoc.Change c) {
        Mc.widgetMove(w, Mc.widgetX(w) + c.dx(), Mc.widgetY(w) + c.dy());
        if (c.w() > 0) {
            w.setWidth(c.w());
        }

        if (c.h() > 0) {
            ((WidgetSizeMixin)w).spraute$setHeight(c.h());
        }

        if (c.text() != null) {
            w.setMessage(Component.literal(c.text()));
        }

        w.visible = c.visible();
        if (!c.enabled()) {
            w.active = false;
        }
    }

    private static GuiView view(String role) {
        EngineConfig cfg = EngineClient.config();
        String json = cfg.layout(role);
        String project = cfg.project;
        if (json == null) {
            json = store().layouts.get(role);
            project = store().project;
        }

        if (json == null || json.isBlank()) {
            BundledProjects.Menu bundled = BundledProjects.menu(role);
            if (bundled == null) {
                return null;
            }

            json = bundled.json();
            project = bundled.project();
        }

        GuiView known = VIEWS.get(role);
        if (known != null && json.equals(VIEW_JSON.get(role))) {
            return known;
        } else {
            if (!pictures) {
                pictures = true;
                ProjectImages.registerAll();
            }

            GuiView v = new GuiView(GuiDoc.fromJson(json), project);
            v.freeMouse = true;
            VIEWS.put(role, v);
            VIEW_JSON.put(role, json);
            return v;
        }
    }

    public static void update(String project, String element, String prop, String value) {
        for (GuiView v : VIEWS.values()) {
            if (Objects.equals(v.project, project)) {
                v.update(element, prop, value);
            }
        }
    }

    public static String input(String project, String element) {
        for (GuiView v : VIEWS.values()) {
            if (Objects.equals(v.project, project)) {
                String text = v.inputs().get(element);
                if (text != null) {
                    return text;
                }
            }
        }

        return "";
    }

    public static void open(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screen now = mc.screen;
        switch (name) {
            case "back":
                if (now != null) {
                    now.onClose();
                }
                break;
            case "title":
                mc.setScreen(new TitleScreen());
                break;
            case "worlds":
            case "singleplayer":
                mc.setScreen(new SelectWorldScreen(now));
                break;
            case "multiplayer":
                mc.setScreen(new JoinMultiplayerScreen(now));
                break;
            case "options":
                mc.setScreen(new OptionsScreen(now, mc.options));
                break;
            default:
                for (MenuRuntime.Known k : KNOWN) {
                    if (k.key.equals(name)) {
                        if (k.make != null) {
                            mc.setScreen(k.make.get());
                            return;
                        }
                        break;
                    }
                }

                throw new SprauteRuntimeException("Этот экран нельзя открыть: «" + name + "»");
        }
    }

    private static void act(Screen screen, String action) {
        Minecraft mc = Minecraft.getInstance();
        switch (action) {
            case "close":
                screen.onClose();
                break;
            case "quit":
                mc.stop();
                break;
            case "singleplayer":
                mc.setScreen(new SelectWorldScreen(screen));
                break;
            case "multiplayer":
                mc.setScreen(new JoinMultiplayerScreen(screen));
                break;
            case "options":
                mc.setScreen(new OptionsScreen(screen, mc.options));
        }
    }

    static {
        known("title", "Главное меню", TitleScreen.class, TitleScreen::new);
        known("pause", "Пауза", PauseScreen.class, () -> new PauseScreen(true));
        known("options", "Настройки", OptionsScreen.class, () -> new OptionsScreen(null, Minecraft.getInstance().options));
        known("video", "Настройки: графика", VideoSettingsScreen.class);
        known("sound", "Настройки: звук", SoundOptionsScreen.class);
        known("controls", "Настройки: управление", ControlsScreen.class);
        known("keys", "Настройки: клавиши", KeyBindsScreen.class);
        known("mouse", "Настройки: мышь", MouseSettingsScreen.class);
        known("chatoptions", "Настройки: чат", ChatOptionsScreen.class);
        known("skin", "Настройки: скин", SkinCustomizationScreen.class);
        known("language", "Настройки: язык", LanguageSelectScreen.class);
        known("accessibility", "Настройки: специальные возможности", AccessibilityOptionsScreen.class);
        known("online", "Настройки: сеть", OnlineOptionsScreen.class);
        known("packs", "Наборы ресурсов и данных", PackSelectionScreen.class);
        known("worlds", "Выбор мира", SelectWorldScreen.class);
        known("createworld", "Создание мира", CreateWorldScreen.class);
        known("multiplayer", "Сетевая игра", JoinMultiplayerScreen.class);
        known("direct", "Прямое подключение", DirectJoinServerScreen.class);
        known("addserver", "Добавление сервера", EditServerScreen.class);
        known("lan", "Открыть для сети", ShareToLanScreen.class);
        known("death", "Смерть", DeathScreen.class, () -> new DeathScreen(Component.empty(), false));
        known("credits", "Титры", WinScreen.class);
        known("stats", "Статистика", StatsScreen.class);
        known("advancements", "Достижения", AdvancementsScreen.class);
        known("chat", "Чат", ChatScreen.class);
        known("loading", "Загрузка мира", LevelLoadingScreen.class);
        known("connecting", "Подключение", ConnectScreen.class);
        known("disconnected", "Отключение от сервера", DisconnectedScreen.class);
        known("inventory", "Инвентарь", InventoryScreen.class, () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player != null && mc.gameMode != null && !mc.gameMode.hasInfiniteItems() ? new InventoryScreen(mc.player) : null;
        });
        known("creative", "Творческий инвентарь", CreativeModeInventoryScreen.class);
        known("chest", "Сундук", ContainerScreen.class);
        known("shulker", "Шалкер", ShulkerBoxScreen.class);
        known("hopper", "Воронка", HopperScreen.class);
        known("dispenser", "Раздатчик", DispenserScreen.class);
        known(
            "crafting",
            "Верстак",
            CraftingScreen.class,
            () -> withInventory(inv -> new CraftingScreen(new CraftingMenu(0, inv), inv, Component.translatable("container.crafting")))
        );
        known(
            "furnace",
            "Печь",
            FurnaceScreen.class,
            () -> withInventory(inv -> new FurnaceScreen(new FurnaceMenu(0, inv), inv, Component.translatable("container.furnace")))
        );
        known(
            "smoker",
            "Коптильня",
            SmokerScreen.class,
            () -> withInventory(inv -> new SmokerScreen(new SmokerMenu(0, inv), inv, Component.translatable("container.smoker")))
        );
        known(
            "blast",
            "Плавильная печь",
            BlastFurnaceScreen.class,
            () -> withInventory(inv -> new BlastFurnaceScreen(new BlastFurnaceMenu(0, inv), inv, Component.translatable("container.blast_furnace")))
        );
        known("brewing", "Варочная стойка", BrewingStandScreen.class);
        known("enchanting", "Стол зачарования", EnchantmentScreen.class);
        known(
            "anvil",
            "Наковальня",
            AnvilScreen.class,
            () -> withInventory(inv -> new AnvilScreen(new AnvilMenu(0, inv), inv, Component.translatable("container.repair")))
        );
        known("smithing", "Стол кузнеца", SmithingScreen.class);
        known("grindstone", "Точило", GrindstoneScreen.class);
        known("stonecutter", "Камнерез", StonecutterScreen.class);
        known("loom", "Ткацкий станок", LoomScreen.class);
        known("cartography", "Стол картографа", CartographyTableScreen.class);
        known("beacon", "Маяк", BeaconScreen.class);
        known("merchant", "Торговля", MerchantScreen.class, () -> withInventory(inv -> new MerchantScreen(new MerchantMenu(0, inv), inv, Component.empty())));
        known("horse", "Лошадь", HorseInventoryScreen.class);
        known("lectern", "Кафедра", LecternScreen.class);
        known("book", "Книга", BookViewScreen.class);
        known("bookedit", "Книга с пером", BookEditScreen.class);
        known("sign", "Табличка", SignEditScreen.class);
    }

    private record Known(String key, String title, Class<? extends Screen> type, Supplier<Screen> make) {
    }

    private static final class Layer extends GfxWidget {
        private final Screen screen;
        private final GuiView view;

        Layer(Screen screen, GuiView view) {
            super(screen.width, screen.height);
            this.screen = screen;
            this.view = view;
            this.active = false;
            view.restartAppear();
            view.listener = (element, kind) -> {
                if (kind.equals("click")) {
                    GeneralRuntime.button(view.project, element);
                    GuiDoc.Element e = view.doc.element(element);
                    if (e != null) {
                        MenuRuntime.act(screen, e.get("menuAction"));
                    }
                }
            };
        }

        @Override
        public void draw(Gfx g, int mx, int my, float partial) {
            MenuLook.slotsOf(this.screen);
            MenuLook.keepLogoHidden();
            EngineClient.drawWindow(g, this.view, mx, my);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            this.view.bounds(0, 0, this.screen.width, this.screen.height);
            GuiDoc.Element own = this.view.pick(this.view.toDesignX(mx), this.view.toDesignY(my), x -> x.type.equals("menu_widget"));
            if (own != null && GeneralRuntime.listens(this.view.project, own.id)) {
                ProjectSounds.ui(this.view.project, "minecraft:ui.button.click");
                GeneralRuntime.button(this.view.project, own.id);
                return true;
            } else {
                GuiDoc.Element e = this.view.elementAt(this.view.toDesignX(mx), this.view.toDesignY(my), true);
                if (e != null && e.type.equals("button")) {
                    this.view.mousePressed(mx, my, button);
                    this.view.mouseReleased(mx, my, button);
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    private static final class Seen {
        int width;
        int height;
        List<MenuDoc.Part> parts = new ArrayList<>();
    }

    private static final class Store {
        String project = "";
        Map<String, String> layouts = new LinkedHashMap<>();
        Map<String, MenuRuntime.Seen> seen = new LinkedHashMap<>();
    }
}
