package org.sprautestudio.spraute_engine.platform.forge;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderGuiEvent.Post;
import net.minecraftforge.client.event.RenderGuiOverlayEvent.Pre;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

final class ForgeClientVersion {
    private ForgeClientVersion() {
    }

    static Gfx gfx(Post f) {
        return Gfx.of(f.getGuiGraphics());
    }

    static Gfx gfx(Pre f) {
        return Gfx.of(f.getGuiGraphics());
    }

    static void drawOverlay(Pre f, Gfx g, float partialTick, int width, int height) {
        f.getOverlay().overlay().render((ForgeGui)Minecraft.getInstance().gui, g.raw, partialTick, width, height);
    }

    static Matrix4f projection(RenderLevelStageEvent f) {
        return f.getProjectionMatrix();
    }

    static void registerChat() {
        MinecraftForge.EVENT_BUS.addListener((ClientChatReceivedEvent.Player f) -> {
            ClientHooks.ChatFromPlayer e = new ClientHooks.ChatFromPlayer();
            ClientHooks.CHAT_FROM_PLAYER.fire(e);
            if (e.isCanceled()) {
                f.setCanceled(true);
            }
        });
    }
}
