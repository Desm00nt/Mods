package org.sprautestudio.spraute_engine.custom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.sprautestudio.spraute_engine.compat.McBlocks;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class CustomParts {
    static final int REACH = 3;
    private static Supplier<Block> part;
    private static final Map<Block, BlockDef> BIG = new HashMap<>();

    private CustomParts() {
    }

    static void register(Map<String, CustomBlocks.Registered> blocks) {
        boolean any = false;

        for (CustomBlocks.Registered r : blocks.values()) {
            any |= r.def().big();
        }

        if (any) {
            part = Platform.get().block("part", () -> new CustomParts.Part(McBlocks.properties("stone").noOcclusion().strength(1.5F)));
        }
    }

    public static Block part() {
        return part == null ? null : part.get();
    }

    private static BlockDef bigDef(BlockState state) {
        Block b = state.getBlock();
        synchronized (BIG) {
            if (BIG.isEmpty()) {
                for (CustomBlocks.Registered r : CustomBlocks.registered().values()) {
                    if (r.def().big()) {
                        BIG.put(r.block().get(), r.def());
                    }
                }
            }

            return BIG.get(b);
        }
    }

    static List<BlockPos> cells(BlockDef d, BlockState state) {
        Set<BlockPos> out = new LinkedHashSet<>();

        for (AABB box : CustomBlocks.box(d, state).toAabbs()) {
            for (int x = floor(box.minX); x <= ceil(box.maxX) - 1; x++) {
                for (int y = floor(box.minY); y <= ceil(box.maxY) - 1; y++) {
                    for (int z = floor(box.minZ); z <= ceil(box.maxZ) - 1; z++) {
                        if ((x != 0 || y != 0 || z != 0) && Math.abs(x) <= 3 && Math.abs(y) <= 3 && Math.abs(z) <= 3) {
                            out.add(new BlockPos(x, y, z));
                        }
                    }
                }
            }
        }

        return new ArrayList<>(out);
    }

    private static int floor(double v) {
        return (int)Math.floor(v + 1.0E-6);
    }

    private static int ceil(double v) {
        return (int)Math.ceil(v - 1.0E-6);
    }

    static boolean fits(BlockDef d, BlockState state, BlockPlaceContext ctx) {
        if (state != null && d.big() && part != null) {
            Level level = ctx.getLevel();

            for (BlockPos c : cells(d, state)) {
                BlockPos p = ctx.getClickedPos().offset(c);
                if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced(BlockPlaceContext.at(ctx, p, ctx.getClickedFace()))) {
                    return false;
                }
            }

            return true;
        } else {
            return state != null;
        }
    }

    static void place(BlockDef d, BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide && d.big() && part != null) {
            for (BlockPos c : cells(d, state)) {
                BlockPos p = pos.offset(c);
                BlockState there = level.getBlockState(p);
                if (there.isAir() || there.canBeReplaced(Fluids.EMPTY)) {
                    level.setBlock(
                        p,
                        part.get()
                            .defaultBlockState()
                            .setValue(CustomParts.Part.DX, c.getX() + 3)
                            .setValue(CustomParts.Part.DY, c.getY() + 3)
                            .setValue(CustomParts.Part.DZ, c.getZ() + 3),
                        3
                    );
                }
            }
        }
    }

    static void remove(BlockDef d, BlockState state, Level level, BlockPos pos) {
        if (!level.isClientSide && d.big() && part != null) {
            for (BlockPos c : cells(d, state)) {
                BlockPos p = pos.offset(c);
                BlockState there = level.getBlockState(p);
                if (there.is(part.get()) && CustomParts.Part.main(there, p).equals(pos)) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
    }

    static VoxelShape collision(BlockDef d, BlockState state) {
        VoxelShape all = CustomBlocks.box(d, state);
        return d.big() ? Shapes.join(all, Shapes.block(), BooleanOp.AND) : all;
    }

    static final class Part extends Block {
        static final IntegerProperty DX = IntegerProperty.create("dx", 0, 6);
        static final IntegerProperty DY = IntegerProperty.create("dy", 0, 6);
        static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 6);

        Part(Properties properties) {
            super(properties);
            this.registerDefaultState(this.stateDefinition.any().setValue(DX, 3).setValue(DY, 3).setValue(DZ, 3));
        }

        @Override
        protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
            builder.add(DX, DY, DZ);
        }

        static BlockPos main(BlockState s, BlockPos pos) {
            return pos.offset(3 - s.getValue(DX), 3 - s.getValue(DY), 3 - s.getValue(DZ));
        }

        private static BlockState mainState(BlockGetter level, BlockPos pos, BlockState s) {
            BlockState m = level.getBlockState(main(s, pos));
            return CustomParts.bigDef(m) != null ? m : null;
        }

        @Override
        public RenderShape getRenderShape(BlockState state) {
            return RenderShape.INVISIBLE;
        }

        @Override
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            BlockState m = mainState(level, pos, state);
            if (m == null) {
                return Shapes.empty();
            } else {
                BlockPos at = main(state, pos);
                return CustomBlocks.box(CustomParts.bigDef(m), m).move(at.getX() - pos.getX(), at.getY() - pos.getY(), at.getZ() - pos.getZ());
            }
        }

        @Override
        public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            BlockState m = mainState(level, pos, state);
            return m != null && !CustomParts.bigDef(m).noCollision
                ? Shapes.join(this.getShape(state, level, pos, context), Shapes.block(), BooleanOp.AND)
                : Shapes.empty();
        }

        @Override
        public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            BlockState m = mainState(level, pos, state);
            return m == null ? 1.0F : m.getDestroyProgress(player, level, main(state, pos));
        }

        @Override
        public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
            BlockState m = mainState(level, pos, state);
            if (m != null && !level.isClientSide) {
                level.destroyBlock(main(state, pos), !player.isCreative(), player);
            }
        }

        @Override
        public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            BlockState m = mainState(level, pos, state);
            return m == null ? InteractionResult.PASS : m.use(level, player, hand, hit.withPosition(main(state, pos)));
        }

        @Override
        public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
            BlockState m = mainState(level, pos, state);
            return m == null ? ItemStack.EMPTY : new ItemStack(m.getBlock());
        }

        @Override
        public BlockState updateShape(BlockState state, Direction dir, BlockState next, LevelAccessor level, BlockPos pos, BlockPos nextPos) {
            return mainState(level, pos, state) == null ? Blocks.AIR.defaultBlockState() : state;
        }
    }
}
