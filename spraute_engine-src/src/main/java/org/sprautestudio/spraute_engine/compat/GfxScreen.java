package org.sprautestudio.spraute_engine.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GfxScreen extends Screen {
    protected GfxScreen(Component title) {
        super(title);
    }

    public abstract void draw(Gfx var1, int var2, int var3, float var4);

    protected final void drawWidgets(Gfx g, int mouseX, int mouseY, float partialTick) {
        super.render(g.raw, mouseX, mouseY, partialTick);
    }

    protected final void drawBackground(Gfx g) {
        this.renderBackground(g.raw);
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.draw(Gfx.of(g), mouseX, mouseY, partialTick);
    }
}
