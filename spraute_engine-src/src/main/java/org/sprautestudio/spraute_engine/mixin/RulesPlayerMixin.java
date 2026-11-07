package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.skills.Skills;

@Mixin(Player.class)
public abstract class RulesPlayerMixin {
    private Player spraute$self() {
        return (Player)(Object)this;
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void spraute$attack(Entity target, CallbackInfo ci) {
        if (!PlayerRules.can(this.spraute$self(), "attack") || Skills.blocked(this.spraute$self(), this.spraute$self().getMainHandItem())) {
            ci.cancel();
        }
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void spraute$interact(Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!PlayerRules.can(this.spraute$self(), "interact_entity")) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void spraute$elytra(CallbackInfoReturnable<Boolean> cir) {
        if (!PlayerRules.can(this.spraute$self(), "elytra")) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float spraute$hunger(float amount) {
        return amount * (float)PlayerRules.number(this.spraute$self(), "hunger_speed", 1.0);
    }

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int spraute$xp(int amount) {
        double k = PlayerRules.number(this.spraute$self(), "xp_gain", 1.0);
        return k == 1.0 ? amount : (int)Math.round(amount * k);
    }
}
