package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(Entity.class)
public abstract class RulesRidingMixin {
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At("HEAD"), cancellable = true)
    private void spraute$ride(Entity vehicle, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (!force && (Object)this instanceof Player p && !PlayerRules.can(p, "ride")) {
            cir.setReturnValue(false);
        }
    }
}
