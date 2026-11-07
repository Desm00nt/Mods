package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.skills.Skills;

@Mixin(ItemStack.class)
public abstract class RulesItemStackMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void spraute$use(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack self = (ItemStack)(Object)this;
        if (Skills.blocked(player, self)) {
            cir.setReturnValue(InteractionResultHolder.fail(self));
        } else {
            if (!PlayerRules.can(player, "use_item") || self.isEdible() && !PlayerRules.can(player, "eat")) {
                cir.setReturnValue(InteractionResultHolder.fail(self));
            }
        }
    }

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void spraute$useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player != null) {
            if (Skills.blocked(player, (ItemStack)(Object)this)) {
                cir.setReturnValue(InteractionResult.FAIL);
            } else {
                boolean block = ((ItemStack)(Object)this).getItem() instanceof BlockItem;
                if (!PlayerRules.can(player, block ? "place" : "use_item")) {
                    cir.setReturnValue(InteractionResult.FAIL);
                }
            }
        }
    }

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true)
    private void spraute$useOnEntity(Player player, LivingEntity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!PlayerRules.can(player, "use_item")) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
