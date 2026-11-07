package org.sprautestudio.spraute_engine.platform;

import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.level.block.Block;

public interface ClientPlatform {
    static ClientPlatform get() {
        return ClientPlatform.Holder.instance;
    }

    static void install(ClientPlatform platform) {
        ClientPlatform.Holder.instance = platform;
    }

    KeyMapping gameKey(String var1, int var2, String var3);

    default void hideBranding(boolean version, boolean copyright) {
    }

    void renderLayer(Block var1, String var2);

    Key boundKey(KeyMapping var1);

    public static final class Holder {
        private static ClientPlatform instance;

        private Holder() {
        }
    }
}
