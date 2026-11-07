package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHitboxMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void spraute$pushable(CallbackInfoReturnable<Boolean> cir) {
        int f = Hitboxes.flags((Entity)(Object)this);
        if ((f & 7) != 0) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "pushEntities", at = @At("HEAD"), cancellable = true)
    private void spraute$pushOthers(CallbackInfo ci) {
        int f = Hitboxes.flags((Entity)(Object)this);
        if ((f & 5) != 0) {
            ci.cancel();
        }
    }

    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void spraute$pickable(CallbackInfoReturnable<Boolean> cir) {
        int f = Hitboxes.flags((Entity)(Object)this);
        if ((f & 8) != 0) {
            cir.setReturnValue(false);
        }
    }
}
