package org.sprautestudio.spraute_engine.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.custom.BlockData;

@Mixin(MultiPackResourceManager.class)
public abstract class BlockDataMixin {
    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true)
    private static List<PackResources> spraute$blockData(List<PackResources> packs) {
        try {
            PackResources data = BlockData.build();
            if (data == null) {
                return packs;
            } else {
                List<PackResources> all = new ArrayList<>(packs);
                all.add(data);
                return all;
            }
        } catch (RuntimeException | IOException var3) {
            SprauteEngine.LOGGER.error("Spraute: cannot prepare the data of the projects' blocks", (Throwable)var3);
            return packs;
        }
    }
}
