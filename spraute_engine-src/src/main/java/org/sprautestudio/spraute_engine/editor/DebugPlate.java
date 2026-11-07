package org.sprautestudio.spraute_engine.editor;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.platform.ClientHooks;

public final class DebugPlate {
    private static final ResourceLocation ID = new ResourceLocation("spraute_engine", "debug_plate");
    private static final int MAX_LINES = 22;
    private static final int WIDTH = 280;
    private static long asked;

    private DebugPlate() {
    }

    public static void registerHooks() {
        ClientHooks.REGISTER_HUD.add(e -> e.registerAbove(ClientHooks.HudParts.CHAT_PANEL, ID.getPath(), (g, partial, w, h) -> draw(g)));
    }

    public static boolean shown() {
        return ClientEvents.editorMode() && Workspace.settings().debugPlate;
    }

    public static void setShown(boolean on) {
        Workspace.settings().debugPlate = on;
        Workspace.saveSettings();
    }

    private static void draw(Gfx g) {
        Minecraft mc = Minecraft.getInstance();
        if (shown() && mc.player != null && !mc.options.hideGui && mc.screen == null) {
            ClientState st = ClientState.get();
            long now = System.currentTimeMillis();
            if (st.statusAge() > 1000L && now - asked > 1000L) {
                asked = now;
                st.requestStatus();
            }

            List<DebugPlate.Line> lines = new ArrayList<>();

            for (Packets.ScriptEntry e : st.status().scripts()) {
                if (e.active) {
                    lines.add(
                        new DebugPlate.Line(
                            "#" + e.id + " " + (e.title.isEmpty() ? "Сюжет" : e.title) + (e.paused ? "  [пауза]" : ""), e.paused ? -14006 : -7352065
                        )
                    );
                    if (!e.doing.isEmpty()) {
                        lines.add(new DebugPlate.Line("  ▸ " + e.doing, -1513240));
                    }

                    for (Packets.Branch b : e.branches) {
                        if (b.count() >= 0) {
                            boolean waiting = b.doing().startsWith("ждёт:");
                            lines.add(
                                new DebugPlate.Line(
                                    "  ⚡ " + strip(b.name()) + "  ×" + b.count() + (waiting ? "" : "  → " + strip(b.doing())), waiting ? -5197648 : -6559601
                                )
                            );
                        } else {
                            lines.add(new DebugPlate.Line("  ⇉ " + b.name() + ": " + strip(b.doing()), -3623681));
                        }
                    }
                }
            }

            if (lines.isEmpty()) {
                lines.add(new DebugPlate.Line("Сюжеты не запущены", -6645094));
            }

            if (lines.size() > 22) {
                int more = lines.size() - 22 + 1;
                lines = new ArrayList<>(lines.subList(0, 21));
                lines.add(new DebugPlate.Line("  … ещё " + more, -6645094));
            }

            Font font = mc.font;
            int x = 4;
            int y = 4;
            int lh = 10;
            g.fill(x - 2, y - 2, x + 280 + 2, y + 12 + lines.size() * lh + 2, -1879048192);
            g.drawString(font, "Spraute · отладка  (скрыть — в панели сюжетов)", x, y, -9735552, false);
            y += 12;

            for (DebugPlate.Line l : lines) {
                g.drawString(font, fit(font, l.text, 280), x, y, l.color, false);
                y += lh;
            }
        }
    }

    private static String strip(String s) {
        String t = s.startsWith("ждёт: ") ? s.substring(6) : s;
        int colon = t.indexOf(": ");
        return colon > 0 && colon < 6 && t.substring(0, colon).chars().allMatch(Character::isDigit) ? t.substring(colon + 2) : t;
    }

    private static String fit(Font font, String s, int w) {
        if (font.width(s) <= w) {
            return s;
        } else {
            while (s.length() > 1 && font.width(s + "…") > w) {
                s = s.substring(0, s.length() - 1);
            }

            return s + "…";
        }
    }

    private record Line(String text, int color) {
    }
}
