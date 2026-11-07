package org.sprautestudio.spraute_engine.core.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;

public final class SlotInventoryScreen extends GfxScreen {
    private static final int MENU_HOTBAR = 36;
    private static final int MENU_OFFHAND = 45;
    private static final int CELL = 18;
    private static final int CELL_GAP = 2;
    private static final int MODEL_SCALE = 40;
    private static final int MODEL_H = 90;
    private static final int ROW_GAP = 8;
    private static final int PER_ROW = 9;
    private static final int CRAFT_FROM_MIDDLE = 36;
    private static final int CRAFT_FROM_MODEL_TOP = 10;
    private static final int CRAFT_SIDE = 2;
    private static final int ARROW_W = 14;

    public SlotInventoryScreen() {
        super(Component.literal("Инвентарь"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int rowsHeight(int rows) {
        return rows * 20;
    }

    private List<int[]> rows() {
        List<int[]> rows = new ArrayList<>();

        for (int r = 0; r < 3; r++) {
            int[] row = new int[9];
            boolean open = false;

            for (int i = 0; i < 9; i++) {
                row[i] = 9 + r * 9 + i;
                open |= !SlotsClient.locked(row[i]);
            }

            if (open) {
                rows.add(row);
            }
        }

        int[] hotbar = new int[9];
        int i = 0;

        while (i < 9) {
            hotbar[i] = i++;
        }

        rows.add(hotbar);
        return rows;
    }

    private int feet(int rowCount) {
        return (this.height - 90 - 8 - this.rowsHeight(rowCount)) / 2 + 90;
    }

    private int craftX() {
        return this.width / 2 + 36;
    }

    private int craftY(int rowCount) {
        return this.feet(rowCount) - 90 + 10;
    }

    private List<SlotInventoryScreen.Cell> cells() {
        List<SlotInventoryScreen.Cell> out = new ArrayList<>();
        if (this.minecraft != null && this.minecraft.player != null) {
            List<int[]> rows = this.rows();
            int extra = PlayerSlots.extra(this.minecraft.player);
            int extraRows = (extra + 9 - 1) / 9;
            int count = rows.size() + extraRows;
            int y = this.feet(count) + 8;
            int step = 20;
            int craftX = this.craftX();
            int craftY = this.craftY(count);

            for (int i = 0; i < 4; i++) {
                out.add(new SlotInventoryScreen.Cell(SlotPlan.menuSlot(2000 + i), false, craftX + i % 2 * step, craftY + i / 2 * step));
            }

            out.add(new SlotInventoryScreen.Cell(SlotPlan.menuSlot(2004), false, craftX + 2 * step + 14, craftY + (step - 2) / 2 + 1));
            int left = (this.width - (9 * step - 2)) / 2;

            for (int[] row : rows) {
                for (int i = 0; i < 9; i++) {
                    int cell = row[i];
                    out.add(new SlotInventoryScreen.Cell(cell < 9 ? 36 + cell : cell, SlotsClient.locked(cell), left + i * step, y));
                }

                if (row[0] == 0 && SlotsClient.offhandAllowed()) {
                    out.add(new SlotInventoryScreen.Cell(45, false, left + 9 * step + 2, y));
                }

                y += step;
            }

            for (int i = 0; i < extra; i++) {
                out.add(new SlotInventoryScreen.Cell(46 + i, false, left + i % 9 * step, y + i / 9 * step));
            }

            return out;
        } else {
            return out;
        }
    }

    private SlotInventoryScreen.Cell cellAt(double mx, double my) {
        for (SlotInventoryScreen.Cell c : this.cells()) {
            if (mx >= c.x && mx < c.x + 18 && my >= c.y && my < c.y + 18) {
                return c;
            }
        }

        return null;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        SlotInventoryScreen.Cell c = this.cellAt(mx, my);
        if (c != null && this.minecraft != null && this.minecraft.player != null && this.minecraft.gameMode != null) {
            if (!c.locked) {
                ClickType type = Screen.hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
                this.minecraft
                    .gameMode
                    .handleInventoryMouseClick(this.minecraft.player.inventoryMenu.containerId, c.menuSlot, button, type, this.minecraft.player);
            }

            return true;
        } else {
            return super.mouseClicked(mx, my, button);
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }

        super.onClose();
    }

    @Override
    public void draw(Gfx g, int mx, int my, float partial) {
        this.drawBackground(g);
        if (this.minecraft != null && this.minecraft.player != null) {
            LocalPlayer p = this.minecraft.player;
            List<int[]> rows = this.rows();
            int feet = this.feet(rows.size() + (PlayerSlots.extra(p) + 9 - 1) / 9);
            Mc.inventoryEntity(g, this.width / 2, feet, 40, this.width / 2.0F - mx, feet - 63.0F - my, p);
            int step = 20;
            g.drawString(this.font, "→", this.craftX() + 2 * step + 2, this.craftY(rows.size() + (PlayerSlots.extra(p) + 9 - 1) / 9) + step - 4, -1, true);
            SlotInventoryScreen.Cell hover = this.cellAt(mx, my);

            for (SlotInventoryScreen.Cell c : this.cells()) {
                g.fill(c.x, c.y, c.x + 18, c.y + 18, c != hover && (hover == null || c.menuSlot != hover.menuSlot) ? -1071635152 : -1067423104);
                ItemStack st = p.inventoryMenu.getSlot(c.menuSlot).getItem();
                if (!st.isEmpty()) {
                    g.renderItem(st, c.x + 1, c.y + 1);
                    g.renderItemDecorations(this.font, st, c.x + 1, c.y + 1);
                }

                if (c.locked) {
                    g.pose().pushPose();
                    g.pose().translate(0.0F, 0.0F, 200.0F);
                    SlotOverlay.lock(g, c.x, c.y, 18);
                    g.pose().popPose();
                }
            }

            ItemStack carried = p.inventoryMenu.getCarried();
            if (!carried.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(0.0F, 0.0F, 300.0F);
                g.renderItem(carried, mx - 8, my - 8);
                g.renderItemDecorations(this.font, carried, mx - 8, my - 8);
                g.pose().popPose();
            } else if (hover != null && !hover.locked) {
                ItemStack stx = p.inventoryMenu.getSlot(hover.menuSlot).getItem();
                if (!stx.isEmpty()) {
                    g.renderTooltip(this.font, stx, mx, my);
                }
            }
        }
    }

    private record Cell(int menuSlot, boolean locked, int x, int y) {
    }
}
