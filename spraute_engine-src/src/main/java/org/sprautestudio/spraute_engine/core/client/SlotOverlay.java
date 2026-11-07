package org.sprautestudio.spraute_engine.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;

public final class SlotOverlay {
    private static final int PANEL = -3750202;
    private static final int EDGE = -11184811;
    private static final int WELL = -7631989;
    private static final int WELL_EDGE = -13158601;
    private static final int ON_TOP = 300;

    private SlotOverlay() {
    }

    public static void before(Gfx g, Screen screen, int left, int top) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && screen instanceof InventoryScreen) {
            int n = PlayerSlots.extra(p);
            if (n > 0) {
                int rows = (n + 6 - 1) / 6;
                int x = left + 184 - 7;
                int y = top + 8 - 7;
                int w = 122;
                int h = rows * 18 + 14;
                g.fill(x, y, x + w, y + h, -3750202);
                g.renderOutline(x, y, w, h, -11184811);

                for (int i = 0; i < n; i++) {
                    int cx = left + 184 + i % 6 * 18 - 1;
                    int cy = top + 8 + i / 6 * 18 - 1;
                    g.fill(cx, cy, cx + 18, cy + 18, -7631989);
                    g.renderOutline(cx, cy, 18, 18, -13158601);
                }
            }
        }
    }

    public static void after(Gfx g, AbstractContainerMenu menu, int left, int top) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && PlayerSlots.limited(p)) {
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);

            for (Slot slot : menu.slots) {
                if (slot.container == p.getInventory() && slot.isActive() && PlayerSlots.locked(p, slot.getContainerSlot())) {
                    lock(g, left + slot.x, top + slot.y, 16);
                }
            }

            g.pose().popPose();
        }
    }

    public static void lock(Gfx g, int x, int y, int size) {
        EngineConfig cfg = EngineClient.config();
        if (cfg.lock.isSet()) {
            if (cfg.lockShade) {
                g.fill(x, y, x + size, y + size, -1340598248);
            }

            Skins.draw(g, cfg.project, cfg.lock, x, y, size, size);
        } else {
            LockIcon.draw((rx, ry, rw, rh, color) -> g.fill(rx, ry, rx + rw, ry + rh, color), x, y, size);
        }
    }
}
