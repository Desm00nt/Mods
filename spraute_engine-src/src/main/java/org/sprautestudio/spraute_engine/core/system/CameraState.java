package org.sprautestudio.spraute_engine.core.system;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.runtime.GameEvents;

public final class CameraState {
    private static final double MOVED = 0.01;
    private static final Map<UUID, CameraState.State> STATES = new HashMap<>();

    private CameraState() {
    }

    private static CameraState.State state(ServerPlayer p) {
        return STATES.computeIfAbsent(p.getUUID(), k -> new CameraState.State());
    }

    public static boolean active(ServerPlayer p) {
        return p != null && state(p).active;
    }

    public static Vec3 position(ServerPlayer p) {
        return p == null ? Vec3.ZERO : state(p).position;
    }

    public static void report(ServerPlayer p, double x, double y, double z, boolean on) {
        CameraState.State st = state(p);
        Vec3 now = new Vec3(x, y, z);
        boolean moved = on && now.distanceToSqr(st.position) > 0.01;
        if (st.moving && !moved) {
            Map<String, Object> d = new HashMap<>();
            d.put("x", x);
            d.put("y", y);
            d.put("z", z);
            GameEvents.fire("cameraend", p, d);
        }

        st.moving = moved;
        st.active = on;
        st.position = now;
        if (st.carriedFrom != null) {
            if (on) {
                p.teleportTo(x, y, z);
            } else {
                carry(p, false);
            }
        }
    }

    public static void carry(ServerPlayer p, boolean on) {
        if (p != null) {
            CameraState.State st = state(p);
            if (on && st.carriedFrom == null) {
                st.carriedFrom = p.gameMode.getGameModeForPlayer();
                st.carriedAt = p.position();
                p.setGameMode(GameType.SPECTATOR);
            } else if (!on && st.carriedFrom != null) {
                p.setGameMode(st.carriedFrom);
                p.teleportTo(st.carriedAt.x, st.carriedAt.y, st.carriedAt.z);
                st.carriedFrom = null;
            }
        }
    }

    public static void left(ServerPlayer p) {
        carry(p, false);
        STATES.remove(p.getUUID());
    }

    public static void unload() {
        STATES.clear();
    }

    private static final class State {
        boolean active;
        boolean moving;
        Vec3 position = Vec3.ZERO;
        GameType carriedFrom;
        Vec3 carriedAt;
    }
}
