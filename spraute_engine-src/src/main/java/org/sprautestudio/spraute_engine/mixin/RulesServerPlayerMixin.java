package org.sprautestudio.spraute_engine.mixin;

import com.mojang.datafixers.util.Either;
import java.util.OptionalInt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player.BedSleepingProblem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(ServerPlayer.class)
public abstract class RulesServerPlayerMixin {
    private ServerPlayer spraute$self() {
        return (ServerPlayer)(Object)this;
    }

    @Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
    private void spraute$sleep(BlockPos pos, CallbackInfoReturnable<Either<BedSleepingProblem, Unit>> cir) {
        if (!PlayerRules.can(this.spraute$self(), "sleep")) {
            cir.setReturnValue(Either.left(BedSleepingProblem.OTHER_PROBLEM));
        }
    }

    @Inject(method = "openMenu", at = @At("HEAD"), cancellable = true)
    private void spraute$menu(MenuProvider provider, CallbackInfoReturnable<OptionalInt> cir) {
        if (!PlayerRules.can(this.spraute$self(), "containers")) {
            cir.setReturnValue(OptionalInt.empty());
        }
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void spraute$drop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerRules.can(this.spraute$self(), "drop")) {
            cir.setReturnValue(false);
        }
    }
}
