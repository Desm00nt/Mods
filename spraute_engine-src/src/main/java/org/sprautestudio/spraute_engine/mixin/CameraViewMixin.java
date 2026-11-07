package org.sprautestudio.spraute_engine.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.camera.client.PlayerCam;

@Mixin(GameRenderer.class)
public abstract class CameraViewMixin {
    @Inject(method = "pick", at = @At("HEAD"), cancellable = true, require = 0)
    private void spraute$pick(float partial, CallbackInfo ci) {
        if (PlayerCam.pick(partial)) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void spraute$bob(PoseStack pose, float partial, CallbackInfo ci) {
        if (PlayerCam.noVanillaBob()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void spraute$hurt(PoseStack pose, float partial, CallbackInfo ci) {
        if (PlayerCam.noVanillaHurt()) {
            ci.cancel();
        }
    }
}
