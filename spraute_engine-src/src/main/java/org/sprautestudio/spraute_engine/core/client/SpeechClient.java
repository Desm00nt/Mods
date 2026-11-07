package org.sprautestudio.spraute_engine.core.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.PlateLayout;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class SpeechClient {
    private static final int FADE_TICKS = 15;
    private static final float LEAST_SECONDS = 0.5F;
    private static final int TICKS_PER_SECOND = 20;
    private static final int ICON = 20;
    private static final int PAD = 4;
    private static final int BOTTOM = 50;
    private static final int BOTTOM_CINEMA = 25;
    private static final int TOP = 24;
    private static final float MAX_WIDTH = 0.6F;
    private static final int MAX_HISTORY = 500;
    private static final int DEFAULT_NAME_COLOR = 16765530;
    private static final Map<Integer, List<SpeechClient.Line>> HISTORY = new HashMap<>();
    private static int newest = -1;
    private static SpeechClient.Line current;
    private static int age;
    public static final KeyMapping ADVANCE = new KeyMapping("key.spraute_engine.advance", 297, "key.categories.spraute_engine");
    private static final KeyMapping LOG_KEY = new KeyMapping("key.spraute_engine.chat_history", 74, "key.categories.spraute_engine");

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(SpeechClient::onTick);
        ClientHooks.RENDER_GUI.add(SpeechClient::onRender);
        ClientHooks.LOGGING_OUT.add(SpeechClient::onLogout);
    }

    private SpeechClient() {
    }

    public static List<SpeechClient.Line> history() {
        return newest < 0 ? null : HISTORY.computeIfAbsent(newest, k -> new ArrayList<>());
    }

    public static void newest(int scriptId) {
        newest = scriptId;
    }

    public static void receive(Packets.Speech msg) {
        SpeechClient.Line l = new SpeechClient.Line(msg.name(), msg.color(), msg.icon(), msg.text(), msg.hint(), msg.think());
        List<SpeechClient.Line> h = HISTORY.computeIfAbsent(msg.scriptId(), k -> new ArrayList<>());
        h.add(l);
        if (h.size() > 500) {
            h.remove(0);
        }

        if (!msg.historyOnly()) {
            current = l;
            age = 0;
            EngineConfig cfg = EngineClient.config();
            GuiView lay = layout();
            if (lay != null) {
                lay.restartAppear();
            }

            ProjectSounds.ui(cfg.project, cfg.chatLog.speechSound);
        }
    }

    private static GuiView layout() {
        if (!EngineClient.config().chatLog.speechCustom) {
            return null;
        } else {
            GuiView v = EngineClient.window("dialog");
            return v != null && !PlateLayout.onlyPlate(v.doc) ? v : null;
        }
    }

    private static int stayTicks() {
        return Math.round(Math.max(0.5F, EngineClient.config().chatLog.speechSeconds) * 20.0F);
    }

    private static int nameColor(SpeechClient.Line l) {
        return l.color() >= 0 ? l.color() : (EngineClient.config().chatLog.nameColor >= 0 ? EngineClient.config().chatLog.nameColor : 16765530);
    }

    public static void clear(int scriptId) {
        current = null;
        HISTORY.remove(scriptId);
    }

    public static ResourceLocation iconTexture(String icon) {
        return new ResourceLocation("spraute_engine", "textures/gui/chat/" + (icon != null && !icon.isEmpty() ? icon : "generic_man") + ".png");
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (current != null && ++age > stayTicks() + 15) {
                current = null;
            }

            while (LOG_KEY.consumeClick()) {
                if (mc.screen == null && history() != null) {
                    mc.setScreen(new ChatLogScreen());
                }
            }
        }
    }

    static Component message(SpeechClient.Line l) {
        return message(l, true);
    }

    static Component message(SpeechClient.Line l, boolean hint) {
        int color = nameColor(l);
        MutableComponent c = Component.empty();
        if (!l.name().isEmpty()) {
            Style named = Style.EMPTY.withColor(TextColor.fromRgb(color));
            c.append(Component.literal("[" + l.name() + "]").withStyle(named));
            if (hint && !l.hint().isEmpty()) {
                c.append(Component.literal(" " + l.hint().replace("F8", ADVANCE.getTranslatedKeyMessage().getString())).withStyle(named));
            }

            c.append(Component.literal(": "));
        }

        c.append(l.think() ? Component.literal(l.text()).withStyle(ChatFormatting.ITALIC) : Component.literal(l.text()));
        return c;
    }

    public static SpeechClient.PlateBox plateBox(Font font, SpeechClient.Line l, int maxW, SpeechClient.PlateStyle s, boolean hint) {
        int size = s.icon() ? Math.max(1, s.iconSize()) : 0;
        int before = s.icon() ? size + 8 : 4;
        List<FormattedCharSequence> rows = font.split(message(l, hint), Math.max(40, maxW - before - 4));
        int tw = 0;

        for (FormattedCharSequence r : rows) {
            tw = Math.max(tw, font.width(r));
        }

        int th = rows.size() * 9;
        int tall = Math.max(size, th);
        int ix = 4 + s.iconX();
        int iy = 4 + (tall - size) / 2 + s.iconY();
        int tx = before + s.textX();
        int ty = 4 + (tall - th) / 2 + s.textY();
        int left = s.icon() ? Math.min(ix, tx) : tx;
        int top = s.icon() ? Math.min(iy, ty) : ty;
        int right = s.icon() ? Math.max(ix + size, tx + tw) : tx + tw;
        int bottom = s.icon() ? Math.max(iy + size, ty + th) : ty + th;
        int dx = 4 - left;
        int dy = 4 - top;
        return new SpeechClient.PlateBox(right - left + 8, bottom - top + 8, s.icon(), ix + dx, iy + dy, size, tx + dx, ty + dy, tw, th, rows);
    }

    static int plate(Gfx g, Font font, SpeechClient.Line l, int centerX, int y, int maxW, int backAlpha, float alpha) {
        return plate(g, font, l, centerX, y, maxW, backAlpha, alpha, true);
    }

    static int plate(Gfx g, Font font, SpeechClient.Line l, int centerX, int y, int maxW, int backAlpha, float alpha, boolean hint) {
        return plate(g, font, l, centerX, y, maxW, backAlpha << 24, 16777215, alpha, hint);
    }

    public static int plate(Gfx g, Font font, SpeechClient.Line l, int centerX, int y, int maxW, int back, int text, float alpha, boolean hint) {
        SpeechClient.PlateBox box = plateBox(font, l, maxW, SpeechClient.PlateStyle.USUAL, hint);
        drawPlate(g, font, box, l.icon(), centerX - box.w() / 2, y, back, text, alpha);
        return box.h();
    }

    static void drawPlate(Gfx g, Font font, SpeechClient.PlateBox box, String icon, int x, int y, int back, int text, float alpha) {
        g.fill(x, y, x + box.w(), y + box.h(), Math.round((back >>> 24) * alpha) << 24 | back & 16777215);
        if (box.icon()) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            ChatHeads.drawIcon(g, iconTexture(icon), x + box.iconX(), y + box.iconY(), box.iconSize(), box.iconSize());
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        drawPlateText(g, font, box, x + box.textX(), y + box.textY(), text, alpha);
    }

    public static void drawPlateText(Gfx g, Font font, SpeechClient.PlateBox box, int x, int y, int text, float alpha) {
        int a = Math.max(4, Math.round(alpha * 255.0F)) << 24;
        int ty = y;

        for (FormattedCharSequence r : box.rows()) {
            g.drawString(font, r, x, ty, a | text & 16777215, true);
            ty += 9;
        }
    }

    public static int plateWidth(Font font, SpeechClient.Line l, int maxW) {
        return plateBox(font, l, maxW, SpeechClient.PlateStyle.USUAL, true).w();
    }

    public static int plateCenter(String align, int left, int width, int plate) {
        String var4 = align == null ? "center" : align;

        return switch (var4) {
            case "left" -> left + plate / 2;
            case "right" -> left + width - plate + plate / 2;
            default -> left + width / 2;
        };
    }

    public static int plateHeight(Font font, SpeechClient.Line l, int maxW) {
        return plateBox(font, l, maxW, SpeechClient.PlateStyle.USUAL, true).h();
    }

    public static void onRender(ClientHooks.RenderGui e) {
        Minecraft mc = Minecraft.getInstance();
        if (current != null && !mc.options.hideGui && mc.screen == null) {
            Gfx g = e.getGuiGraphics();
            float t = age + e.getPartialTick() - stayTicks();
            float alpha = t <= 0.0F ? 1.0F : Math.max(0.0F, 1.0F - t / 15.0F);
            if (!(alpha <= 0.0F)) {
                GuiView lay = layout();
                if (lay != null) {
                    lay.speechLine = current;
                    lay.vars.put("name", current.name());
                    lay.vars.put("text", (current.think() ? "§o" : "") + current.text());
                    lay.vars.put("hint", current.hint().replace("F8", ADVANCE.getTranslatedKeyMessage().getString()));
                    lay.speechIcon = current.icon();
                    lay.speakerColor = nameColor(current);
                    lay.opacity = alpha;
                    EngineClient.drawWindow(g, lay, -10000, -10000);
                } else {
                    EngineConfig.ChatLog cfg = EngineClient.config().chatLog;
                    int maxW = Math.round(g.guiWidth() * (cfg.speechWidth > 0 ? cfg.speechWidth / 100.0F : 0.6F));
                    SpeechClient.PlateBox box = plateBox(mc.font, current, maxW, SpeechClient.PlateStyle.of(cfg), true);
                    int h = box.h();
                    String place = cfg.speechPlace == null ? "bottom" : cfg.speechPlace;

                    int y = switch (place) {
                        case "center" -> (g.guiHeight() - h) / 2;
                        case "top" -> 24;
                        default -> g.guiHeight() - (CameraDirector.active() && CameraDirector.hidesMinecraft() ? 25 : 50) - h;
                    };
                    int margin = 8;
                    int center = plateCenter(cfg.speechAlign, margin, g.guiWidth() - margin * 2, box.w()) + cfg.speechShiftX;
                    drawPlate(g, mc.font, box, current.icon(), center - box.w() / 2, y - cfg.speechShift, cfg.speechBack, cfg.speechText, alpha);
                }
            }
        }
    }

    public static void onLogout(ClientHooks.LoggingOut e) {
        PlayerSkins.clear();
        current = null;
        HISTORY.clear();
        newest = -1;
    }

    public static final class Keys {
        public static void registerHooks() {
            ClientHooks.REGISTER_KEYS.add(SpeechClient.Keys::register);
        }

        public static void register(ClientHooks.RegisterKeys e) {
            e.register(SpeechClient.LOG_KEY);
            e.register(SpeechClient.ADVANCE);
        }
    }

    public record Line(String name, int color, String icon, String text, String hint, boolean think) {
    }

    public record PlateBox(
        int w, int h, boolean icon, int iconX, int iconY, int iconSize, int textX, int textY, int textW, int textH, List<FormattedCharSequence> rows
    ) {
    }

    public record PlateStyle(boolean icon, int iconSize, int iconX, int iconY, int textX, int textY) {
        public static final SpeechClient.PlateStyle USUAL = new SpeechClient.PlateStyle(true, 20, 0, 0, 0, 0);

        public static SpeechClient.PlateStyle of(EngineConfig.ChatLog log) {
            return new SpeechClient.PlateStyle(log.speechIconShown, log.speechIconSize, log.speechIconX, log.speechIconY, log.speechTextX, log.speechTextY);
        }
    }
}
