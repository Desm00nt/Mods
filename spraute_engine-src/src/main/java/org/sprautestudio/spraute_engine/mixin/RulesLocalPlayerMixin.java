package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(LocalPlayer.class)
public abstract class RulesLocalPlayerMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void spraute$drop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerRules.can((LocalPlayer)(Object)this, "drop")) {
            cir.setReturnValue(false);
        }
    }
}
