package org.sprautestudio.spraute_engine.core.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

public final class PlayerRulesClient {
    private static final double GAME_GRAVITY = 0.08;
    private static final double AIR_DRAG = 0.98;
    private static int heldSlot;
    private static boolean stepWasSet;

    private PlayerRulesClient() {
    }

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(PlayerRulesClient::onTick);
        ClientHooks.MOVEMENT_INPUT.add(PlayerRulesClient::onInput);
        ClientHooks.MOUSE_SCROLL.add(PlayerRulesClient::onScroll);
        ClientHooks.LOGGING_OUT.add(e -> PlayerRules.clientLeft());
    }

    public static boolean canLook() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null || PlayerRules.can(p, "look");
    }

    private static void onTick(ClientHooks.ClientTick e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p != null) {
            if (e.phase != TickPhase.START) {
                Inventory inv = p.getInventory();
                if (PlayerRules.can(p, "hotbar")) {
                    heldSlot = inv.selected;
                } else {
                    inv.selected = heldSlot;
                }

                double step = PlayerRules.number(p, "step_height", -1.0);
                if (step >= 0.0) {
                    Mc.setMaxUpStep(p, (float)step);
                } else if (stepWasSet) {
                    Mc.setMaxUpStep(p, 0.6F);
                }

                stepWasSet = step >= 0.0;
                double gravity = PlayerRules.number(p, "gravity", 1.0);
                if (gravity != 1.0 && !Mc.onGround(p) && !p.getAbilities().flying && !p.isInWater() && !p.isInLava() && !p.isFallFlying() && !p.onClimbable()) {
                    Vec3 m = p.getDeltaMovement();
                    p.setDeltaMovement(m.x, m.y + 0.08 * (1.0 - gravity) * 0.98, m.z);
                }
            } else {
                if (!PlayerRules.can(p, "swap_hands")) {
                    drain(mc.options.keySwapOffhand);
                }

                if (!PlayerRules.can(p, "inventory")) {
                    drain(mc.options.keyInventory);
                }

                if (!PlayerRules.can(p, "hotbar")) {
                    for (KeyMapping k : mc.options.keyHotbarSlots) {
                        drain(k);
                    }
                }
            }
        }
    }

    private static void drain(KeyMapping key) {
        while (key.consumeClick()) {
        }
    }

    private static void onInput(ClientHooks.MovementInput e) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            Input in = e.getInput();
            if (!PlayerRules.can(p, "move")) {
                in.forwardImpulse = 0.0F;
                in.leftImpulse = 0.0F;
                in.up = false;
                in.down = false;
                in.left = false;
                in.right = false;
            }

            if (!PlayerRules.can(p, "jump")) {
                in.jumping = false;
            }

            if (!PlayerRules.can(p, "sneak")) {
                in.shiftKeyDown = false;
            }
        }
    }

    private static void onScroll(ClientHooks.MouseScroll e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null && !PlayerRules.can(mc.player, "hotbar")) {
            e.setCanceled(true);
        }
    }
}
