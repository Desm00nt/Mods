package org.sprautestudio.spraute_engine.runtime;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.core.client.SpeechClient;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.ClientHooks;

public final class KeyReporter {
    public static void registerHooks() {
        ClientHooks.KEY.add(KeyReporter::onKey);
        ClientHooks.MOUSE_BUTTON.add(KeyReporter::onMouse);
        ClientHooks.LEFT_CLICK_EMPTY.add(KeyReporter::onLeftAir);
    }

    private KeyReporter() {
    }

    public static void onKey(ClientHooks.Key e) {
        if (e.getAction() == 1) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.screen == null && mc.getConnection() != null) {
                if (SpeechClient.ADVANCE.matches(e.getKey(), e.getScanCode())) {
                    SprauteNetwork.sendToServer(new Packets.KeyPress("f8"));
                }

                String name;
                if (mc.options.keyJump.matches(e.getKey(), e.getScanCode())) {
                    name = "jump";
                } else if (mc.options.keyShift.matches(e.getKey(), e.getScanCode())) {
                    name = "sneak";
                } else if (mc.options.keySprint.matches(e.getKey(), e.getScanCode())) {
                    name = "sprint";
                } else {
                    String full = InputConstants.getKey(e.getKey(), e.getScanCode()).getName();
                    name = full.startsWith("key.keyboard.") ? full.substring(13) : full;
                }

                if (!name.equals("f8")) {
                    SprauteNetwork.sendToServer(new Packets.KeyPress(name));
                }
            }
        }
    }

    public static void onMouse(ClientHooks.MouseButton e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.getAction() == 1 && mc.player != null && mc.screen == null && mc.getConnection() != null) {
            String name = switch (e.getButton()) {
                case 0 -> "mouse.left";
                case 1 -> "mouse.right";
                case 2 -> "mouse.middle";
                default -> "mouse." + e.getButton();
            };
            SprauteNetwork.sendToServer(new Packets.KeyPress(name));
        }
    }

    public static void onLeftAir(ClientHooks.LeftClickEmpty e) {
        SprauteNetwork.sendToServer(new Packets.KeyPress("air.left"));
    }
}
