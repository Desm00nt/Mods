package org.sprautestudio.spraute_engine.engine.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.core.client.ScreenFx;
import org.sprautestudio.spraute_engine.core.client.SlotsClient;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class EngineClient {
    private static final ResourceLocation HEALTH_OVERLAY = new ResourceLocation("spraute_engine", "health_overlay");
    private static final ResourceLocation COINS_OVERLAY = new ResourceLocation("spraute_engine", "currency_overlay");
    private static final Map<ResourceLocation, String> VANILLA = new HashMap<>();
    private static EngineConfig config = new EngineConfig();
    private static final Map<String, String> WINDOW_JSON = new HashMap<>();
    private static final Map<String, GuiView> WINDOW_VIEW = new HashMap<>();

    public static void registerHooks() {
        ClientHooks.LOGGING_OUT.add(EngineClient::onLogout);
        ClientHooks.RENDER_HUD_PART.add(EngineClient::onOverlay);
        ClientHooks.CHAT_FROM_PLAYER.add(EngineClient::onChat);
    }

    private EngineClient() {
    }

    public static ItemStack coinItem() {
        String id = config().coinItem;
        return id != null && !id.isBlank() ? ItemSpec.stack(id.trim(), 1) : ItemStack.EMPTY;
    }

    public static EngineConfig config() {
        return config;
    }

    public static void receive(String json) {
        config = EngineConfig.fromJson(json);
        Skins.invalidate();
        ProjectImages.registerAll();
        PlayerSlots.clientEngine(config);
    }

    public static void onLogout(ClientHooks.LoggingOut event) {
        config = EngineConfig.fromJson("{}");
        PlayerSlots.clientEngine(config);
    }

    public static GuiView window(String role) {
        String json = config.layout(role);
        if (json == null) {
            WINDOW_JSON.remove(role);
            WINDOW_VIEW.remove(role);
            return null;
        } else {
            if (!json.equals(WINDOW_JSON.get(role))) {
                WINDOW_JSON.put(role, json);
                GuiView v = new GuiView(GuiDoc.fromJson(json), config.project);
                v.drawBackground = false;
                v.freeMouse = true;
                v.hudState = () -> Minecraft.getInstance().player != null ? state(Minecraft.getInstance().player) : null;
                v.restartAppear();
                WINDOW_VIEW.put(role, v);
            }

            return WINDOW_VIEW.get(role);
        }
    }

    public static GuiView hudLayout() {
        return window("hud");
    }

    public static void drawWindow(Gfx g, GuiView v, int mx, int my) {
        Minecraft mc = Minecraft.getInstance();
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        Canvas c = new Canvas(g, mc.font, 1.0F);
        v.bounds(0, 0, sw, sh);
        v.layout();
        v.render(c, mx, my, mc.getDeltaFrameTime() / 20.0F);
        c.flush();
    }

    private static boolean onLayout(GuiView layout, String type) {
        for (GuiDoc.Element e : layout.doc.all()) {
            if (type.equals(e.type)) {
                return true;
            }
        }

        return false;
    }

    public static HudPainter.State state(LocalPlayer p) {
        HudPainter.State s = new HudPainter.State();
        s.nav = config.nav;
        s.stHealth = config.health;
        s.hp = p.getHealth();
        s.maxHp = p.getMaxHealth();
        s.armor = p.getArmorValue();
        s.food = p.getFoodData().getFoodLevel();
        s.air = p.getAirSupply();
        s.maxAir = p.getMaxAirSupply();
        s.showAir = p.isEyeInFluid(FluidTags.WATER) || s.air < s.maxAir;
        s.exp = p.experienceProgress;
        s.level = p.experienceLevel;
        s.selected = p.getInventory().selected;

        for (int i = 0; i < 9; i++) {
            s.hotbar[i] = p.getInventory().getItem(i);
        }

        s.offhand = p.getOffhandItem();
        s.hotbarSize = SlotsClient.hotbarSize();

        for (int i = 0; i < 9; i++) {
            s.locked[i] = SlotsClient.locked(i);
        }

        s.coins = Coins.shown();
        s.coinsNeed = Coins.shownNeed();
        return s;
    }

    public static void onOverlay(ClientHooks.RenderHudPart event) {
        ResourceLocation id = event.id();
        String key = VANILLA.get(id);
        if (id.equals(HEALTH_OVERLAY)) {
            key = "health";
        }

        if (id.equals(COINS_OVERLAY)) {
            key = "coins";
        }

        if (key != null) {
            GuiView layout = hudLayout();
            if (layout == null || key.equals("coins") && config.currency && !onLayout(layout, "hud_coins")) {
                if (key.equals("health") && id.equals(HEALTH_OVERLAY) != config.health) {
                    event.setCanceled(true);
                } else if (key.equals("coins") && !config.currency) {
                    event.setCanceled(true);
                } else {
                    Minecraft mc = Minecraft.getInstance();
                    LocalPlayer player = mc.player;
                    if (player != null) {
                        if (key.equals("hotbar") && EngineConfig.gameDrawn(config.element(key).style) && SlotsClient.anyHotbarLocked() && !player.isSpectator()
                            )
                         {
                            event.setCanceled(true);
                            if (!mc.options.hideGui) {
                                int w = event.getWindow().getGuiScaledWidth();
                                int h = event.getWindow().getGuiScaledHeight();
                                HudPainter.State st = state(player);
                                HudPainter.standard(event.getGuiGraphics(), mc.font, key, HudPainter.rect(config, key, w, h, st), st);
                            }
                        } else {
                            EngineConfig.Element e = config.element(key);
                            if (!e.visible) {
                                event.setCanceled(true);
                            } else {
                                int sw = event.getWindow().getGuiScaledWidth();
                                int sh = event.getWindow().getGuiScaledHeight();
                                HudPainter.State s = state(player);
                                Gfx g = event.getGuiGraphics();
                                if (!EngineConfig.gameDrawn(e.style)) {
                                    event.setCanceled(true);
                                    if (!mc.options.hideGui && shown(mc, key, s)) {
                                        HudPainter.custom(g, mc.font, config, key, HudPainter.rect(config, key, sw, sh, s), s);
                                    }
                                } else {
                                    if (e.customPos && event.canDrawOriginal()) {
                                        int[] d = HudPainter.shift(config, key, sw, sh, s);
                                        if (d[0] != 0 || d[1] != 0) {
                                            event.setCanceled(true);
                                            g.pose().pushPose();
                                            g.pose().translate((float)d[0], (float)d[1], 0.0F);

                                            try {
                                                event.drawOriginal(sw, sh);
                                            } finally {
                                                g.pose().popPose();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                event.setCanceled(true);
            }
        }
    }

    public static boolean shown(Minecraft mc, String key, HudPainter.State s) {
        if (mc.gameMode != null && mc.player != null && !mc.player.isSpectator()) {
            boolean survival = mc.gameMode.canHurtPlayer();

            return switch (key) {
                case "health", "food" -> survival;
                case "armor" -> survival && s.armor > 0;
                case "air" -> survival && s.showAir;
                case "exp" -> mc.gameMode.hasExperience();
                default -> true;
            };
        } else {
            return false;
        }
    }

    public static void onChat(ClientHooks.ChatFromPlayer event) {
        if (config.chat) {
            event.setCanceled(true);
        }
    }

    static {
        VANILLA.put(ClientHooks.HudParts.PLAYER_HEALTH, "health");
        VANILLA.put(ClientHooks.HudParts.ARMOR_LEVEL, "armor");
        VANILLA.put(ClientHooks.HudParts.FOOD_LEVEL, "food");
        VANILLA.put(ClientHooks.HudParts.AIR_LEVEL, "air");
        VANILLA.put(ClientHooks.HudParts.EXPERIENCE_BAR, "exp");
        VANILLA.put(ClientHooks.HudParts.HOTBAR, "hotbar");
    }

    public static final class Overlays {
        public static void registerHooks() {
            ClientHooks.REGISTER_HUD.add(EngineClient.Overlays::register);
        }

        public static void register(ClientHooks.RegisterHud e) {
            e.registerAbove(ClientHooks.HudParts.PLAYER_HEALTH, EngineClient.HEALTH_OVERLAY.getPath(), (g, partial, w, h) -> draw(g, "health", w, h));
            e.registerAbove(ClientHooks.HudParts.PLAYER_HEALTH, EngineClient.COINS_OVERLAY.getPath(), (g, partial, w, h) -> draw(g, "coins", w, h));
        }

        private static void draw(Gfx g, String key, int w, int h) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && !mc.options.hideGui && mc.screen == null) {
                if (!ScreenFx.fullyCovered()) {
                    HudPainter.State s = EngineClient.state(mc.player);
                    if (EngineClient.shown(mc, key, s)) {
                        HudPainter.standard(g, mc.font, key, HudPainter.defaultRect(EngineClient.config, key, w, h, s), s);
                    }
                }
            }
        }
    }
}
