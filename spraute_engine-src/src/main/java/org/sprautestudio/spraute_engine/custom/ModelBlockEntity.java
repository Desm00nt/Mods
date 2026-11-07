package org.sprautestudio.spraute_engine.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ModelBlockEntity extends BlockEntity {
    public ModelBlockEntity(BlockPos pos, BlockState state) {
        super(CustomBlocks.modelType(), pos, state);
    }

    public AABB getRenderBoundingBox() {
        CustomModels.Engine e = CustomModels.ENGINE_BLOCKS.get(this.getBlockState().getBlock());
        double r = e == null ? 1.0 : CustomModels.reach(e);
        return new AABB(this.worldPosition).inflate(r);
    }
}
