package org.sprautestudio.spraute_engine.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public abstract class GfxWidget extends AbstractWidget {
    protected GfxWidget(int width, int height) {
        super(0, 0, width, height, Component.empty());
    }

    public abstract void draw(Gfx var1, int var2, int var3, float var4);

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.draw(Gfx.of(g), mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
