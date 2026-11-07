package org.sprautestudio.spraute_engine.core.client;

import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class ScreenFx {
    private static String kind = "";
    private static int in;
    private static int hold;
    private static int out;
    private static int argb;
    private static int age;
    private static int tint;

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(ScreenFx::onTick);
        ClientHooks.RENDER_HUD_PART.add(ScreenFx::underChat);
        ClientHooks.RENDER_GUI.add(ScreenFx::onRender);
        ClientHooks.LOGGING_OUT.add(ScreenFx::onLogout);
    }

    private ScreenFx() {
    }

    public static void play(String k, int fadeIn, int stay, int fadeOut, int color) {
        if (k.equals("color")) {
            tint = color;
        } else {
            kind = k;
            in = fadeIn;
            hold = stay;
            out = fadeOut;
            argb = color;
            age = 0;
        }
    }

    public static boolean fullyCovered() {
        return tint >>> 24 == 255 || !kind.isEmpty() && !kind.equals("pulse_under") && amount(0.0F) >= 1.0F && argb >>> 24 == 255;
    }

    private static float amount(float partial) {
        float t = age + partial;
        if (t < in) {
            return t / Math.max(1, in);
        } else if (t < in + hold) {
            return 1.0F;
        } else {
            return t < in + hold + out ? 1.0F - (t - in - hold) / Math.max(1, out) : 0.0F;
        }
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END && !kind.isEmpty()) {
            if (++age > in + hold + out) {
                kind = "";
            }
        }
    }

    private static void fill(Gfx g, float partial) {
        if (!kind.isEmpty()) {
            float a = amount(partial);
            if (!(a <= 0.0F)) {
                int w = g.guiWidth();
                int h = g.guiHeight();
                if (kind.equals("eyes")) {
                    int lid = Math.round(h / 2.0F * a);
                    g.fill(0, 0, w, lid, argb);
                    g.fill(0, h - lid, w, h, argb);
                } else {
                    int alpha = Math.round((argb >>> 24) * a);
                    g.fill(0, 0, w, h, alpha << 24 | argb & 16777215);
                }
            }
        }
    }

    public static void underChat(ClientHooks.RenderHudPart e) {
        if (e.id().equals(ClientHooks.HudParts.CHAT_PANEL)) {
            if (fullyCovered()) {
                e.setCanceled(true);
            } else if (kind.equals("pulse_under")) {
                fill(e.getGuiGraphics(), e.getPartialTick());
            }
        }
    }

    public static void onRender(ClientHooks.RenderGui e) {
        if (Minecraft.getInstance().screen == null || !kind.isEmpty() || tint != 0) {
            Gfx g = e.getGuiGraphics();
            if (tint >>> 24 != 0) {
                g.fill(0, 0, g.guiWidth(), g.guiHeight(), tint);
            }

            if (!kind.equals("pulse_under")) {
                fill(g, e.getPartialTick());
            }
        }
    }

    public static void onLogout(ClientHooks.LoggingOut e) {
        kind = "";
        tint = 0;
    }
}
