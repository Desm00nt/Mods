package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.build.client.BlockHide;

@Mixin(RenderChunkRegion.class)
public abstract class BlockHideMixin {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void spraute$hide(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (BlockHide.isHidden(pos)) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
