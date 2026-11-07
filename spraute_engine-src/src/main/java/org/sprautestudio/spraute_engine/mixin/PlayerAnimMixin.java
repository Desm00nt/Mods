package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.playeranim.PlayerAnimsClient;

@Mixin(PlayerModel.class)
public abstract class PlayerAnimMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void spraute$anim(LivingEntity entity, float limbSwing, float limbAmount, float age, float yaw, float pitch, CallbackInfo ci) {
        PlayerAnimsClient.apply((PlayerModel<?>)(Object)this, entity);
    }
}
