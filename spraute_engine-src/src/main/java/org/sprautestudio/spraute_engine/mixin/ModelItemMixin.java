package org.sprautestudio.spraute_engine.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.custom.CustomModels;
import org.sprautestudio.spraute_engine.custom.ModelDraw;

@Mixin(BlockEntityWithoutLevelRenderer.class)
public abstract class ModelItemMixin {
    @Inject(method = "renderByItem", at = @At("HEAD"), cancellable = true)
    private void spraute$model(ItemStack stack, @Coerce Object place, PoseStack pose, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        CustomModels.Engine e = CustomModels.ENGINE_ITEMS.get(stack.getItem());
        if (e != null) {
            ModelDraw.draw(e, 0, pose, buffers, light, overlay);
            ci.cancel();
        }
    }
}
