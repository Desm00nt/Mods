package org.sprautestudio.spraute_engine.build.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

public final class BlockHide {
    private static final Map<Long, Integer> HIDDEN = new ConcurrentHashMap<>();

    private BlockHide() {
    }

    public static boolean isHidden(BlockPos pos) {
        return !HIDDEN.isEmpty() && HIDDEN.containsKey(pos.asLong());
    }

    static void hide(BlockPos pos) {
        HIDDEN.merge(pos.asLong(), 1, Integer::sum);
    }

    static void show(BlockPos pos) {
        HIDDEN.computeIfPresent(pos.asLong(), (k, n) -> n > 1 ? n - 1 : null);
    }

    static void clear() {
        HIDDEN.clear();
    }
}
