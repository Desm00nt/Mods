package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.camera.client.PlayerCam;

@Mixin(Camera.class)
public abstract class CameraRigMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double var1, double var3, double var5);

    @Shadow
    protected abstract void setRotation(float var1, float var2);

    @Inject(method = "setup", at = @At("TAIL"))
    private void spraute$rig(BlockGetter level, Entity entity, boolean third, boolean mirror, float partial, CallbackInfo ci) {
        double[] p = PlayerCam.view(entity, partial);
        if (p != null) {
            this.setRotation((float)p[3], (float)p[4]);
            this.setPosition(p[0], p[1], p[2]);
            this.detached = PlayerCam.detached();
        }
    }
}
