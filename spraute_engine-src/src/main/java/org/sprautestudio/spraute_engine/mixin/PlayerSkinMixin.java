package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.core.client.PlayerSkins;

@Mixin(AbstractClientPlayer.class)
public abstract class PlayerSkinMixin {
    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void spraute$skin(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation own = PlayerSkins.of(((Entity)(Object)this).getUUID());
        if (own != null) {
            cir.setReturnValue(own);
        }
    }
}
