package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.camera.client.PlayerCam;
import org.sprautestudio.spraute_engine.core.client.PlayerRulesClient;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;

@Mixin(MouseHandler.class)
public abstract class MouseLockMixin {
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void spraute$lockView(CallbackInfo ci) {
        if (PlayerCam.takeMouse(this.accumulatedDX, this.accumulatedDY)) {
            this.accumulatedDX = 0.0;
            this.accumulatedDY = 0.0;
            ci.cancel();
        } else if (CameraDirector.locksView() || !PlayerRulesClient.canLook()) {
            this.accumulatedDX = 0.0;
            this.accumulatedDY = 0.0;
            ci.cancel();
        }
    }
}
