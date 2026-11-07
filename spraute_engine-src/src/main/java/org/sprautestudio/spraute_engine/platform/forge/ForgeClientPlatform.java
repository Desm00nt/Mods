package org.sprautestudio.spraute_engine.platform.forge;

import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.lang.reflect.Field;
import java.util.List;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.internal.BrandingControl;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;

final class ForgeClientPlatform implements ClientPlatform {
    static void install() {
        ClientPlatform.install(new ForgeClientPlatform());
    }

    @Override
    public KeyMapping gameKey(String name, int glfwKey, String category) {
        return new KeyMapping(name, KeyConflictContext.IN_GAME, Type.KEYSYM, glfwKey, category);
    }

    @Override
    public void hideBranding(boolean version, boolean copyright) {
        try {
            for (String name : List.of("brandings", "brandingsNoMC")) {
                set(name, version ? List.of() : null);
            }

            set("overCopyrightBrandings", copyright ? List.of() : null);
        } catch (ReflectiveOperationException var5) {
            SprauteEngine.LOGGER.warn("Spraute: cannot hide the text of the main menu", (Throwable)var5);
        }
    }

    private static void set(String field, Object value) throws ReflectiveOperationException {
        Field f = BrandingControl.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Override
    public void renderLayer(Block block, String layer) {
        ItemBlockRenderTypes.setRenderLayer(block, layer.equals("translucent") ? RenderType.translucent() : RenderType.cutout());
    }

    @Override
    public Key boundKey(KeyMapping key) {
        return key.getKey();
    }
}
