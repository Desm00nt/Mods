package org.sprautestudio.spraute_engine.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;

public final class SlotsClient {
    private static int lastAllowed;

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(SlotsClient::onTick);
        ClientHooks.MOUSE_SCROLL.add(SlotsClient::onScroll);
        ClientHooks.SCREEN_OPENING.add(SlotsClient::onOpen);
        ClientHooks.LOGGING_OUT.add(e -> PlayerSlots.clientLeft());
    }

    private SlotsClient() {
    }

    public static int hotbarSize() {
        Minecraft mc = Minecraft.getInstance();
        EngineConfig cfg = EngineClient.config();
        return mc.player != null && PlayerSlots.limited(mc.player) ? Math.max(1, cfg.shownHotbar()) : 9;
    }

    public static boolean locked(int cell) {
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && PlayerSlots.locked(p, cell);
    }

    public static boolean anyHotbarLocked() {
        for (int i = 0; i < 9; i++) {
            if (locked(i)) {
                return true;
            }
        }

        return false;
    }

    public static boolean offhandAllowed() {
        return !locked(40);
    }

    public static void onTick(ClientHooks.ClientTick e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (e.phase != TickPhase.START) {
                Inventory inv = mc.player.getInventory();
                if (!locked(inv.selected)) {
                    lastAllowed = inv.selected;
                } else if (!locked(lastAllowed)) {
                    inv.selected = lastAllowed;
                } else {
                    for (int i = 0; i < 9; i++) {
                        if (!locked(i)) {
                            inv.selected = i;
                            lastAllowed = i;
                            return;
                        }
                    }
                }
            } else {
                if (!offhandAllowed()) {
                    while (mc.options.keySwapOffhand.consumeClick()) {
                    }
                }
            }
        }
    }

    public static void onScroll(ClientHooks.MouseScroll e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && e.getScrollDelta() != 0.0 && anyHotbarLocked()) {
            e.setCanceled(true);
            Inventory inv = mc.player.getInventory();
            int step = -((int)Math.signum(e.getScrollDelta()));
            int at = inv.selected;

            for (int i = 0; i < 9; i++) {
                at = Math.floorMod(at + step, 9);
                if (!locked(at)) {
                    inv.selected = at;
                    lastAllowed = at;
                    return;
                }
            }
        }
    }

    public static void onOpen(ClientHooks.ScreenOpening e) {
        if (e.getNewScreen() instanceof InventoryScreen && EngineClient.config().inventory) {
            GuiView layout = EngineClient.window("inventory");
            e.setNewScreen((Screen)(layout != null ? new LayoutInventoryScreen(layout) : new SlotInventoryScreen()));
        }
    }
}
