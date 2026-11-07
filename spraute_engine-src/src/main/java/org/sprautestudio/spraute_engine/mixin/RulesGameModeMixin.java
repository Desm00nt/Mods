package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(ServerPlayerGameMode.class)
public abstract class RulesGameModeMixin {
    @Shadow
    @Final
    protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void spraute$destroy(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerRules.can(this.player, "break")) {
            cir.setReturnValue(false);
        }
    }
}
