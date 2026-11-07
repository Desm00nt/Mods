package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(ItemEntity.class)
public abstract class RulesItemEntityMixin {
    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void spraute$pickup(Player player, CallbackInfo ci) {
        if (!PlayerRules.can(player, "pickup")) {
            ci.cancel();
        }
    }
}
