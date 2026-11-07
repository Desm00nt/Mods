package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;

@Mixin(Entity.class)
public abstract class EntityHitboxMixin {
    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void spraute$solid(CallbackInfoReturnable<Boolean> cir) {
        int f = Hitboxes.flags((Entity)(Object)this);
        if ((f & 1) != 0) {
            cir.setReturnValue(true);
        }
    }
}
