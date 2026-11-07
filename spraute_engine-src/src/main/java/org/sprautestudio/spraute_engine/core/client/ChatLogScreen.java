package org.sprautestudio.spraute_engine.core.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.HudPainter;
import org.sprautestudio.spraute_engine.gui.client.GuiView;

public final class ChatLogScreen extends GfxScreen {
    private static final int GAP = 4;
    private static final int EDGE = 12;
    private static final int BACK_ALPHA = 179;
    private static final float MAX_WIDTH = 0.47F;
    private static final float FADE = 0.25F;
    private static final float CASCADE = 0.03F;
    private final long opened = System.nanoTime();
    private final boolean hudWas = Minecraft.getInstance().options.hideGui;
    private int scroll;

    public ChatLogScreen() {
        super(Component.literal("История речи"));
        Minecraft.getInstance().options.hideGui = true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void removed() {
        Minecraft.getInstance().options.hideGui = this.hudWas;
    }

    private int total(List<SpeechClient.Line> lines, int maxW) {
        int h = 0;

        for (SpeechClient.Line l : lines) {
            h += SpeechClient.plateHeight(this.font, l, maxW) + 4;
        }

        return h;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        List<SpeechClient.Line> lines = SpeechClient.history();
        if (lines == null) {
            return true;
        } else {
            int view = this.height - 24;
            int most = Math.max(0, this.total(lines, Math.round(this.width * 0.47F)) - view);
            this.scroll = Math.max(0, Math.min(most, this.scroll + (int)(delta * 24.0)));
            return true;
        }
    }

    @Override
    public void draw(Gfx g, int mx, int my, float partial) {
        EngineConfig cfg = EngineClient.config();
        GuiView lay = EngineClient.window("chatlog");
        int[] r = HudPainter.chatRect(this.width, this.height);
        List<SpeechClient.Line> lines = SpeechClient.history();
        float since = (float)(System.nanoTime() - this.opened) / 1.0E9F;
        boolean inside = lay != null && lay.doc.all().stream().anyMatch(e -> e.type.equals("chat_lines"));
        if (lay != null) {
            lay.chatLines = lines != null ? lines : List.of();
            lay.chatScroll = this.scroll;
            lay.chatSince = since;
            EngineClient.drawWindow(g, lay, mx, my);
        } else {
            HudPainter.chatBack(g, cfg, r[0], r[1], r[2], r[3]);
            if (!cfg.chatLog.frameOnTop()) {
                HudPainter.chatFrame(g, cfg, r[0], r[1], r[2], r[3]);
            }
        }

        if (lines != null && !inside) {
            g.enableScissor(0, 12, this.width, this.height - 12);
            drawLines(g, this.font, lines, this.width / 2, Math.round(this.width * 0.47F), 12, this.height - 12, this.scroll, since, -1291845632, 16777215);
            g.disableScissor();
        }

        if (lay == null && cfg.chatLog.frameOnTop()) {
            HudPainter.chatFrame(g, cfg, r[0], r[1], r[2], r[3]);
        }
    }

    public static void drawLines(
        Gfx g, Font font, List<SpeechClient.Line> lines, int centerX, int maxW, int top, int bottom, int scroll, float since, int back, int text
    ) {
        int y = bottom + scroll;
        int shown = 0;

        for (int i = lines.size() - 1; i >= 0 && y > top; i--) {
            SpeechClient.Line l = lines.get(i);
            y -= SpeechClient.plateHeight(font, l, maxW) + 4;
            float alpha = Math.max(0.0F, Math.min(1.0F, (since - shown * 0.03F) / 0.25F));
            shown++;
            if (y < bottom && alpha > 0.0F) {
                SpeechClient.plate(g, font, l, centerX, y, maxW, back, text, alpha, false);
            }
        }
    }
}
