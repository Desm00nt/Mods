package org.sprautestudio.spraute_engine.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class ModelBlockRenderer implements BlockEntityRenderer<ModelBlockEntity> {
    public void render(ModelBlockEntity be, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        CustomModels.Engine e = CustomModels.ENGINE_BLOCKS.get(be.getBlockState().getBlock());
        if (e != null) {
            BlockState state = be.getBlockState();
            int turn = 0;
            if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction f = state.getValue(HorizontalDirectionalBlock.FACING);
                turn = f == Direction.EAST ? 1 : (f == Direction.SOUTH ? 2 : (f == Direction.WEST ? 3 : 0));
            }

            ModelDraw.draw(e, turn, pose, buffers, light, overlay);
        }
    }

    public boolean shouldRenderOffScreen(ModelBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
