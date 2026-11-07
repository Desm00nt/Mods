package org.sprautestudio.spraute_engine.custom;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.sprautestudio.spraute_engine.compat.McBlocks;

final class CustomBlockKinds {
    private CustomBlockKinds() {
    }

    static final class Button extends McBlocks.Button {
        private final BlockDef def;

        Button(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Carpet extends CarpetBlock {
        private final BlockDef def;

        Carpet(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Falling extends FallingBlock {
        private final BlockDef def;

        Falling(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.def.shaped() ? CustomBlocks.box(this.def, state) : super.getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (this.def.noCollision) {
                return Shapes.empty();
            } else {
                return this.def.shaped() ? CustomBlocks.box(this.def, state) : super.getCollisionShape(state, level, pos, context);
            }
        }
    }

    static final class Fence extends FenceBlock {
        private final BlockDef def;

        Fence(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Gate extends McBlocks.Gate {
        private final BlockDef def;

        Gate(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class ModelPlain extends CustomBlockKinds.Plain implements EntityBlock {
        ModelPlain(BlockDef def, Properties properties) {
            super(def, properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ModelBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }
    }

    static final class ModelRotating extends CustomBlockKinds.Rotating implements EntityBlock {
        ModelRotating(BlockDef def, Properties properties) {
            super(def, properties);
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new ModelBlockEntity(pos, state);
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }
    }

    static final class Pane extends IronBarsBlock {
        private final BlockDef def;

        Pane(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Pillar extends RotatedPillarBlock {
        private final BlockDef def;

        Pillar(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Placed extends BlockItem {
        private final BlockDef def;

        Placed(Block block, BlockDef def, net.minecraft.world.item.Item.Properties properties) {
            super(block, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return Component.literal(this.def.name);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);

            for (String line : this.def.lore) {
                out.add(Component.literal(line.replace('&', '§')));
            }
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }
    }

    static class Plain extends Block {
        private final BlockDef def;

        Plain(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.def.shaped() ? CustomBlocks.box(this.def, state) : super.getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (this.def.noCollision) {
                return Shapes.empty();
            } else {
                return this.def.shaped() ? CustomParts.collision(this.def, state) : super.getCollisionShape(state, level, pos, context);
            }
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState s = super.getStateForPlacement(context);
            return CustomParts.fits(this.def, s, context) ? s : null;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            super.onPlace(state, level, pos, old, moving);
            if (!old.is(this)) {
                CustomParts.place(this.def, state, level, pos);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (!next.is(this)) {
                CustomParts.remove(this.def, state, level, pos);
            }

            super.onRemove(state, level, pos, next, moving);
        }
    }

    static final class Plant extends BushBlock {
        private final BlockDef def;
        private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 13.0, 14.0);

        Plant(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    static final class Plate extends McBlocks.Plate {
        private final BlockDef def;

        Plate(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static class Rotating extends HorizontalDirectionalBlock {
        private final BlockDef def;

        Rotating(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
            this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }

        @Override
        protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            BlockState s = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
            return CustomParts.fits(this.def, s, context) ? s : null;
        }

        @Override
        public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
            super.onPlace(state, level, pos, old, moving);
            if (!old.is(this)) {
                CustomParts.place(this.def, state, level, pos);
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (!next.is(this)) {
                CustomParts.remove(this.def, state, level, pos);
            }

            super.onRemove(state, level, pos, next, moving);
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.def.shaped() ? CustomBlocks.box(this.def, state) : super.getShape(state, level, pos, context);
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            if (this.def.noCollision) {
                return Shapes.empty();
            } else {
                return this.def.shaped() ? CustomParts.collision(this.def, state) : super.getCollisionShape(state, level, pos, context);
            }
        }
    }

    static final class Slab extends SlabBlock {
        private final BlockDef def;

        Slab(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Stairs extends StairBlock {
        private final BlockDef def;

        Stairs(BlockDef def, Properties properties) {
            super(Blocks.STONE.defaultBlockState(), properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Storage extends CustomBlockKinds.Rotating implements EntityBlock {
        private final BlockDef def;

        Storage(BlockDef def, Properties properties) {
            super(def, properties);
            this.def = def;
        }

        BlockDef def() {
            return this.def;
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CustomStorage(pos, state);
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            } else {
                CustomBlocks.use(this.def, InteractionResult.CONSUME, level, pos, player);
                if (level.getBlockEntity(pos) instanceof CustomStorage box) {
                    box.prepareLoot();
                    player.openMenu(box);
                }

                return InteractionResult.CONSUME;
            }
        }

        @Override
        public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof CustomStorage box ? box : null;
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (level.getBlockEntity(pos) instanceof CustomStorage box) {
                if (stack.hasCustomHoverName()) {
                    box.setCustomName(stack.getHoverName());
                }

                if (placer instanceof Player p && !p.getAbilities().instabuild) {
                    box.noLoot();
                }
            }
        }

        @Override
        public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
            if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CustomStorage box) {
                Containers.dropContents(level, pos, box);
                level.updateNeighbourForOutputSignal(pos, this);
            }

            super.onRemove(state, level, pos, next, moving);
        }

        @Override
        public boolean hasAnalogOutputSignal(BlockState state) {
            return true;
        }

        @Override
        public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
        }
    }

    static final class Trapdoor extends McBlocks.Trapdoor {
        private final BlockDef def;

        Trapdoor(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }

    static final class Wall extends WallBlock {
        private final BlockDef def;

        Wall(BlockDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return CustomBlocks.use(this.def, super.use(state, level, pos, player, hand, hit), level, pos, player);
        }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            super.stepOn(level, pos, state, entity);
            CustomBlocks.step(this.def, level, entity);
        }

        @Override
        public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            super.entityInside(state, level, pos, entity);
            if (this.def.noCollision) {
                CustomBlocks.step(this.def, level, entity);
            }
        }

        @Override
        public boolean isSignalSource(BlockState state) {
            return this.def.redstone > 0 || super.isSignalSource(state);
        }

        @Override
        public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
            return Math.max(this.def.redstone, super.getSignal(state, level, pos, side));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            super.playerWillDestroy(level, pos, state, player);
            CustomBlocks.broken(this.def, level, player);
        }

        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            super.randomTick(state, level, pos, random);
            CustomBlocks.randomTick(this.def, level, pos);
        }

        @Override
        public void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean drops) {
            super.spawnAfterBreak(state, level, pos, tool, drops);
            if (drops && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) == 0) {
                this.popExperience(level, pos, CustomBlocks.xp(this.def, level.getRandom()));
            }
        }

        public int getExpDrop(BlockState state, LevelReader level, RandomSource random, BlockPos pos, int fortune, int silkTouch) {
            return silkTouch > 0 ? 0 : CustomBlocks.xp(this.def, random);
        }
    }
}
