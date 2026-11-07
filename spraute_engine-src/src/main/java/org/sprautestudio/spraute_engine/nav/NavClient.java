package org.sprautestudio.spraute_engine.nav;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.client.ScreenFx;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.HudPainter;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.platform.ClientHooks;

public final class NavClient {
    private static final Map<String, NavClient.Arrow> ARROWS = new LinkedHashMap<>();

    private NavClient() {
    }

    public static void registerHooks() {
        ClientHooks.REGISTER_HUD.add(e -> e.registerAbove(ClientHooks.HudParts.CHAT_PANEL, "navigator", (g, partial, w, h) -> draw(g, partial, w, h)));
        ClientHooks.LOGGING_OUT.add(e -> ARROWS.clear());
    }

    public static void receive(Packets.NavUpdate m) {
        if (m.remove()) {
            ARROWS.remove(m.id());
        } else {
            ARROWS.put(m.id(), new NavClient.Arrow(m.title(), m.color(), m.x(), m.y(), m.z()));
        }
    }

    private static void draw(Gfx g, float partial, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (!ARROWS.isEmpty() && mc.player != null && !mc.options.hideGui && mc.screen == null) {
            if (!ScreenFx.fullyCovered()) {
                EngineConfig engine = EngineClient.config();
                if (engine.element("nav").visible) {
                    GuiView layout = EngineClient.hudLayout();
                    if (layout != null) {
                        for (GuiDoc.Element e : layout.doc.all()) {
                            if ("hud_nav".equals(e.type)) {
                                return;
                            }
                        }
                    }

                    drawAt(g, HudPainter.rect(engine, "nav", sw, sh, EngineClient.state(mc.player)), partial);
                }
            }
        }
    }

    public static void drawAt(Gfx g, int[] r, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (!ARROWS.isEmpty() && mc.player != null) {
            EngineConfig.Nav cfg = EngineClient.config().nav;
            Font font = mc.font;
            float scale = Math.max(0.5F, cfg.scale);
            int rows = Math.min(Math.max(1, cfg.max), ARROWS.size());
            int rowH = 16;
            boolean low = r[1] > g.guiHeight() / 2;
            float cxs = (r[0] + r[2] / 2.0F) / scale;
            int y = Math.round((low ? r[1] + r[3] - rows * rowH * scale : r[1]) / scale);
            g.pose().pushPose();
            g.pose().scale(scale, scale, 1.0F);
            int shown = 0;

            for (NavClient.Arrow a : ARROWS.values()) {
                if (shown++ >= rows) {
                    break;
                }

                double dx = a.x - mc.player.getX();
                double dz = a.z - mc.player.getZ();
                float bearing = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
                float turn = Mth.wrapDegrees(bearing - mc.player.getViewYRot(partial));
                int dist = (int)Math.round(Math.sqrt(dx * dx + dz * dz));
                int color = a.color >= 0 ? 0xFF000000 | a.color & 16777215 : cfg.arrowColor;
                String label = (a.title.isEmpty() ? "" : a.title) + (cfg.distance ? (a.title.isEmpty() ? "" : " · ") + dist + cfg.unit : "");
                int cx = Math.round(cxs);
                int lw = font.width(label);
                if (cfg.back >>> 24 != 0) {
                    g.fill(cx - lw / 2 - 16, y - 3, cx + lw / 2 + 6, y + 11, cfg.back);
                }

                g.pose().pushPose();
                g.pose().translate(cx - lw / 2.0F - 8.0F, (float)(y + 4), 0.0F);
                Mc.mulPose(g.pose(), Axes.ZP.rotationDegrees(turn));
                String arrow = cfg.arrow != null && !cfg.arrow.isBlank() ? cfg.arrow : "▲";
                g.drawString(font, arrow, -font.width(arrow) / 2, -4, color, false);
                g.pose().popPose();
                g.drawString(font, label, cx - lw / 2 + 2, y, cfg.textColor, true);
                y += rowH;
            }

            g.pose().popPose();
        }
    }

    private record Arrow(String title, int color, double x, double y, double z) {
    }
}
