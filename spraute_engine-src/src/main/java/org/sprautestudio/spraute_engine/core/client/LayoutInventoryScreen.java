package org.sprautestudio.spraute_engine.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.player.SlotPlan;

public final class LayoutInventoryScreen extends GfxScreen {
    private static final int OUTSIDE = -999;
    private final GuiView view;

    public LayoutInventoryScreen(GuiView view) {
        super(Component.literal("Инвентарь"));
        this.view = view;
        view.cellClick = this::click;
        view.listener = (element, kind) -> {
            if (kind.equals("close")) {
                this.onClose();
            }
        };
        view.restartAppear();
    }

    private void click(int cell, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gameMode != null) {
            int slot = cell < 0 ? -999 : SlotPlan.menuSlot(cell);
            ClickType type = cell >= 0 && Screen.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, button, type, mc.player);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void draw(Gfx g, int mx, int my, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            g.fill(0, 0, this.width, this.height, GuiDoc.toColor(this.view.doc.get("background")));
            this.view.speaker = mc.player;
            EngineClient.drawWindow(g, this.view, mx, my);
            ItemStack hovered = this.view.hoveredCellStack();
            if (!hovered.isEmpty() && mc.player.inventoryMenu.getCarried().isEmpty()) {
                g.renderTooltip(this.font, hovered, mx, my);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        this.view.mousePressed(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.view.mouseReleased(mx, my, button);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        this.view.mouseScrolled(mx, my, delta);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (Minecraft.getInstance().options.keyInventory.matches(key, scan)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }

    @Override
    public void onClose() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.closeContainer();
        }

        super.onClose();
    }

    @Override
    public void removed() {
        super.removed();
        this.view.cellClick = null;
        this.view.speaker = null;
        this.view.listener = null;
    }
}
