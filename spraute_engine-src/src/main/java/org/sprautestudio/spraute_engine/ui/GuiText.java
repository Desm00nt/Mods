package org.sprautestudio.spraute_engine.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;

public final class GuiText {
    private static final int FULL_BRIGHT = 15728880;

    private GuiText() {
    }

    public static void draw(Gfx g, Font font, String text, float x, float y, int color, boolean shadow) {
        if (text != null) {
            Mc.text(font, text, x, y, color, shadow, g.pose().last(), g.bufferSource(), false, 0, 15728880);
            g.flush();
        }
    }

    public static void draw(Gfx g, Font font, FormattedCharSequence text, float x, float y, int color, boolean shadow) {
        Mc.text(font, text, x, y, color, shadow, g.pose().last(), g.bufferSource(), false, 0, 15728880);
        g.flush();
    }
}
