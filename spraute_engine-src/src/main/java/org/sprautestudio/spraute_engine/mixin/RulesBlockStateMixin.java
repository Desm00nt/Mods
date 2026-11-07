package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(BlockStateBase.class)
public abstract class RulesBlockStateMixin {
    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void spraute$mining(Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (!PlayerRules.can(player, "break")) {
            cir.setReturnValue(0.0F);
        } else {
            double k = PlayerRules.number(player, "mine_speed", 1.0);
            if (k != 1.0) {
                cir.setReturnValue((float)(cir.getReturnValueF() * k));
            }
        }
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void spraute$touch(Level level, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!PlayerRules.can(player, "interact")) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
