package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;

@Mixin(LivingEntity.class)
public abstract class RulesLivingMixin {
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void spraute$jump(CallbackInfo ci) {
        if ((Object)this instanceof Player p && !PlayerRules.can(p, "jump")) {
            ci.cancel();
        }
    }

    @Inject(method = "getJumpPower", at = @At("RETURN"), cancellable = true)
    private void spraute$jumpPower(CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof Player p) {
            double k = PlayerRules.number(p, "jump_power", 1.0);
            if (k != 1.0) {
                cir.setReturnValue((float)(cir.getReturnValueF() * k));
            }
        }
    }

    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void spraute$sprint(boolean on, CallbackInfo ci) {
        if (on && (Object)this instanceof Player p && !PlayerRules.can(p, "sprint")) {
            ci.cancel();
        }
    }

    @Inject(method = "onClimbable", at = @At("HEAD"), cancellable = true)
    private void spraute$climb(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Player p && !PlayerRules.can(p, "climb")) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void spraute$noDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof Player p && amount > 0.0F && PlayerRules.damage(p, source, amount) <= 0.0F) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true)
    private float spraute$damage(float amount, DamageSource source) {
        return (Object)this instanceof Player p ? PlayerRules.damage(p, source, amount) : amount;
    }
}
