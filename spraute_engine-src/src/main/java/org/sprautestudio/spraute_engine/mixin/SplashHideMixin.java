package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.gui.client.MenuRuntime;

@Mixin(SplashManager.class)
public abstract class SplashHideMixin {
    @Inject(method = "getSplash", at = @At("HEAD"), cancellable = true)
    private void spraute$splash(CallbackInfoReturnable<?> cir) {
        if (MenuRuntime.splashHidden()) {
            cir.setReturnValue(null);
        }
    }
}
