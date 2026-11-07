package org.sprautestudio.spraute_engine.build;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.sprautestudio.spraute_engine.compat.Regs;

public final class BlockText {
    private static final Map<String, Optional<BlockState>> KNOWN = new ConcurrentHashMap<>();

    private BlockText() {
    }

    public static BlockState parse(String text) {
        return KNOWN.computeIfAbsent(text, BlockText::read).orElse(null);
    }

    private static Optional<BlockState> read(String text) {
        int open = text.indexOf(91);
        ResourceLocation id = ResourceLocation.tryParse(open < 0 ? text : text.substring(0, open));
        if (id == null) {
            return Optional.empty();
        } else {
            Optional<Block> block = Regs.BLOCK.getOptional(id);
            if (block.isEmpty()) {
                return Optional.empty();
            } else {
                BlockState state = block.get().defaultBlockState();
                if (open >= 0 && text.endsWith("]")) {
                    for (String pair : text.substring(open + 1, text.length() - 1).split(",")) {
                        int eq = pair.indexOf(61);
                        if (eq > 0) {
                            Property<?> p = block.get().getStateDefinition().getProperty(pair.substring(0, eq).trim());
                            if (p != null) {
                                state = with(state, p, pair.substring(eq + 1).trim());
                            }
                        }
                    }
                }

                return Optional.of(state);
            }
        }
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> p, String value) {
        return p.getValue(value).map(v -> state.setValue(p, v)).orElse(state);
    }

    public static String of(BlockState state) {
        return BlockStateParser.serialize(state);
    }
}
