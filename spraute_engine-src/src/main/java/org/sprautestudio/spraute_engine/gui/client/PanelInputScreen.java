package org.sprautestudio.spraute_engine.gui.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;

final class PanelInputScreen extends GfxScreen {
    private static final int FAR_AWAY = -10000;
    private final GuiView view;
    private final Runnable done;

    PanelInputScreen(GuiView view, Runnable done) {
        super(Component.literal("Ввод"));
        this.view = view;
        this.done = done;
    }

    @Override
    public void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key != 256 && key != 257 && key != 335) {
            return this.view.keyPressed(key, scan, mods);
        } else {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        return this.view.charTyped(ch, mods);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        Minecraft.getInstance().setScreen(null);
        return true;
    }

    @Override
    public void removed() {
        this.view.mousePressed(-10000.0, -10000.0, 0);
        this.view.mouseReleased(-10000.0, -10000.0, 0);
        this.done.run();
        super.removed();
    }
}
