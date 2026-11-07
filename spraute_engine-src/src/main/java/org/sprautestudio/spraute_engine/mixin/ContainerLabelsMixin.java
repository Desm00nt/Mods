package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.gui.client.MenuRuntime;

@Mixin(AbstractContainerScreen.class)
public abstract class ContainerLabelsMixin {
    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void spraute$labels(CallbackInfo ci) {
        if (MenuRuntime.flag(this, "hideLabels")) {
            ci.cancel();
        }
    }
}
