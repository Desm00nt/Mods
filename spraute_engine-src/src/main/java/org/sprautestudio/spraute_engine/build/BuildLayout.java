package org.sprautestudio.spraute_engine.build;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public final class BuildLayout {
    private BuildLayout() {
    }

    public static List<BuildLayout.Cell> of(
        StructureDef d, String styleName, BlockPos at, int rotate, String mirror, float viewerYaw, BlockGetter ground, long seed
    ) {
        int[] g = d.grid();
        StructureDef.Action[] actions = d.actions();
        int turns = Math.floorMod(Math.round(rotate / 90.0F), 4);
        Rotation rotation = Rotation.values()[turns];
        boolean flipX = "x".equalsIgnoreCase(mirror);
        boolean flipZ = "z".equalsIgnoreCase(mirror);
        double yaw = Math.toRadians(viewerYaw);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        int awayX = Math.abs(fx) >= Math.abs(fz) ? (fx > 0.0 ? 1 : -1) : 0;
        int awayZ = awayX != 0 ? 0 : (fz > 0.0 ? 1 : -1);
        int rightX = -awayZ;
        int rightZ = awayX;
        double width = Math.max(d.sizeX, d.sizeZ);
        List<BuildLayout.Cell> out = new ArrayList<>();
        boolean[] footed = new boolean[d.sizeX * d.sizeZ];
        BlockState footing = d.foundation.isBlank() ? null : BlockText.parse(d.foundation);

        for (int y = 0; y < d.sizeY; y++) {
            for (int z = 0; z < d.sizeZ; z++) {
                for (int x = 0; x < d.sizeX; x++) {
                    int i = d.index(x, y, z);
                    StructureDef.Action action = actions[i];
                    if (action != StructureDef.Action.SKIP) {
                        BlockState state;
                        if (action == StructureDef.Action.CLEAR) {
                            state = Blocks.AIR.defaultBlockState();
                        } else {
                            state = BlockText.parse(d.finalBlock(d.palette.get(g[i])));
                            if (state == null) {
                                continue;
                            }

                            if (flipX) {
                                state = state.mirror(Mirror.FRONT_BACK);
                            }

                            if (flipZ) {
                                state = state.mirror(Mirror.LEFT_RIGHT);
                            }

                            state = state.rotate(rotation);
                        }

                        int dx = 2 * x - (d.sizeX - 1);
                        int dz = 2 * z - (d.sizeZ - 1);
                        if (flipX) {
                            dx = -dx;
                        }

                        if (flipZ) {
                            dz = -dz;
                        }

                        for (int t = 0; t < turns; t++) {
                            int next = -dz;
                            dz = dx;
                            dx = next;
                        }

                        BlockPos pos = new BlockPos(at.getX() + Math.floorDiv(dx, 2), at.getY() + y, at.getZ() + Math.floorDiv(dz, 2));
                        double right = (dx * rightX + dz * rightZ) / 2.0;
                        double away = (dx * awayX + dz * awayZ) / 2.0;
                        BuildStyles.Plan plan = BuildStyles.plan(styleName, right, y, away, d.sizeY, width, seed);
                        float wx = (float)(rightX * plan.fromX() + awayX * plan.fromZ());
                        float wz = (float)(rightZ * plan.fromX() + awayZ * plan.fromZ());
                        CompoundTag nbt = null;
                        String text = d.entities.get(String.valueOf(i));
                        if (text != null) {
                            try {
                                nbt = TagParser.parseTag(text);
                            } catch (CommandSyntaxException var52) {
                            }
                        }

                        out.add(new BuildLayout.Cell(pos, state, nbt, wx, (float)plan.fromY(), wz, plan.key()));
                        int col = x + z * d.sizeX;
                        if (footing != null && action == StructureDef.Action.PLACE && !footed[col]) {
                            footed[col] = true;

                            for (int k = 1; k <= d.foundationDepth; k++) {
                                BlockPos below = pos.below(k);
                                BlockState there = ground.getBlockState(below);
                                if (!there.isAir() && there.getFluidState().isEmpty()) {
                                    break;
                                }

                                out.add(new BuildLayout.Cell(below, footing, null, wx, (float)plan.fromY(), wz, plan.key() - 1000.0 - k));
                            }
                        }
                    }
                }
            }
        }

        out.sort(Comparator.comparingDouble(BuildLayout.Cell::key));
        return out;
    }

    public record Cell(BlockPos pos, BlockState state, CompoundTag nbt, float fromX, float fromY, float fromZ, double key) {
    }
}
