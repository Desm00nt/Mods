package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.platform.Window;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class HudOverlays {
    private static final Map<String, HudOverlays.Shown> SHOWN = new LinkedHashMap<>();
    private static long lastFrame = System.nanoTime();

    public static void registerHooks() {
        ClientHooks.RENDER_GUI.add(HudOverlays::onRender);
        ClientHooks.CLIENT_TICK.add(HudOverlays::onTick);
        ClientHooks.LOGGING_OUT.add(HudOverlays::onLogout);
    }

    private HudOverlays() {
    }

    private static String key(int scriptId, String name) {
        return scriptId + "/" + name;
    }

    public static void show(Packets.OverlayShow msg) {
        GuiView v = new GuiView(GuiDoc.fromJson(msg.json()), msg.project());
        v.drawBackground = false;
        v.restartAppear();
        SHOWN.put(key(msg.scriptId(), msg.name()), new HudOverlays.Shown(msg.scriptId(), msg.name(), v));
    }

    public static void hide(Packets.OverlayHide msg) {
        if (msg.name().isEmpty()) {
            SHOWN.values().removeIf(s -> s.scriptId == msg.scriptId());
        } else {
            SHOWN.remove(key(msg.scriptId(), msg.name()));
        }
    }

    public static void update(Packets.GuiUpdate msg) {
        for (HudOverlays.Shown s : SHOWN.values()) {
            if (s.scriptId == msg.scriptId()) {
                s.view.update(msg.element(), msg.prop(), msg.value());
            }
        }
    }

    public static void onRender(ClientHooks.RenderGui event) {
        draw(event.getGuiGraphics(), event.getWindow());
    }

    private static void draw(Gfx graphics, Window window) {
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (float)(now - lastFrame) / 1.0E9F);
        lastFrame = now;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui) {
            int sw = window.getGuiScaledWidth();
            int sh = window.getGuiScaledHeight();
            Canvas c = new Canvas(graphics, mc.font, 1.0F);
            GuiView engineHud = EngineClient.hudLayout();
            if (engineHud != null && mc.player != null && !mc.player.isSpectator() && !CameraDirector.hidesMinecraft()) {
                try {
                    engineHud.bounds(0, 0, sw, sh);
                    engineHud.layout();
                    engineHud.render(c, -10000, -10000, dt);
                    c.flush();
                } catch (RuntimeException var14) {
                }
            }

            if (!SHOWN.isEmpty() && !CameraDirector.hidesCustom()) {
                for (HudOverlays.Shown s : SHOWN.values()) {
                    try {
                        s.view.bounds(0, 0, sw, sh);
                        s.view.layout();
                        s.view.render(c, -10000, -10000, dt);
                    } catch (RuntimeException var13) {
                    }
                }
            }
        }
    }

    public static void onTick(ClientHooks.ClientTick event) {
        if (event.phase == TickPhase.END) {
            for (HudOverlays.Shown s : SHOWN.values()) {
                s.view.tick();
            }
        }
    }

    public static void onLogout(ClientHooks.LoggingOut event) {
        SHOWN.clear();
    }

    private record Shown(int scriptId, String name, GuiView view) {
    }
}
