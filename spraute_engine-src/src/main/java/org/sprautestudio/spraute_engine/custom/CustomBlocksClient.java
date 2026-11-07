package org.sprautestudio.spraute_engine.custom;

import net.minecraft.world.level.block.entity.BlockEntityType;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;
import org.sprautestudio.spraute_engine.platform.Hooks;

public final class CustomBlocksClient {
    private CustomBlocksClient() {
    }

    public static void registerHooks() {
        ClientHooks.REGISTER_RENDERERS.add(e -> {
            BlockEntityType<ModelBlockEntity> type = CustomBlocks.modelType();
            if (type != null) {
                e.registerBlockEntityRenderer(type, ctx -> new ModelBlockRenderer());
            }
        });
        Hooks.COMMON_SETUP.add(e -> {
            for (CustomBlocks.Registered r : CustomBlocks.registered().values()) {
                if (r.def().transparent()) {
                    ClientPlatform.get().renderLayer(r.block().get(), r.def().render);
                } else if (r.def().hasModel()) {
                    ClientPlatform.get().renderLayer(r.block().get(), "cutout");
                }
            }
        });
    }
}
