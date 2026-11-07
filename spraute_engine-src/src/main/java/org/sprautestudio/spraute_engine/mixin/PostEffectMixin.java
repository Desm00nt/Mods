package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.post.client.PostEngine;

@Mixin(GameRenderer.class)
public abstract class PostEffectMixin {
    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = Shift.AFTER),
        require = 0
    )
    private void spraute$post(float partial, long nanos, boolean world, CallbackInfo ci) {
        PostEngine.afterWorld(partial);
    }
}
