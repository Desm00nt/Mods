package org.sprautestudio.spraute_engine.mixin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.custom.ItemPack;

@Mixin(ReloadableResourceManager.class)
public abstract class ItemPackMixin {
    @ModifyVariable(method = "createReload", at = @At("HEAD"), argsOnly = true)
    private List<PackResources> spraute$itemPack(List<PackResources> packs) {
        try {
            List<PackResources> all = new ArrayList<>(packs);
            all.add(ItemPack.build());
            return all;
        } catch (RuntimeException | IOException var3) {
            SprauteEngine.LOGGER.error("Spraute: cannot prepare the look of the projects' items", (Throwable)var3);
            return packs;
        }
    }
}
