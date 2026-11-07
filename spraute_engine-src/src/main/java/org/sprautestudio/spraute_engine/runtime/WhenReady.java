package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.world.entity.Entity;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class WhenReady {
    private static final List<WhenReady.Pending> PENDING = new ArrayList<>();

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(WhenReady::onTick);
    }

    private WhenReady() {
    }

    public static void run(Supplier<Entity> entity, Consumer<Entity> action) {
        PENDING.add(new WhenReady.Pending(entity, action, new int[]{100}));
    }

    public static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END && !PENDING.isEmpty()) {
            for (WhenReady.Pending p : new ArrayList<>(PENDING)) {
                Entity e;
                try {
                    e = p.entity().get();
                } catch (RuntimeException var6) {
                    e = null;
                }

                if (e != null) {
                    try {
                        p.action().accept(e);
                    } catch (RuntimeException var5) {
                        SprauteEngine.LOGGER.warn("Spraute: {}", var5.getMessage());
                    }

                    PENDING.remove(p);
                } else if (--p.ttl()[0] <= 0) {
                    PENDING.remove(p);
                }
            }
        }
    }

    private record Pending(Supplier<Entity> entity, Consumer<Entity> action, int[] ttl) {
    }
}
