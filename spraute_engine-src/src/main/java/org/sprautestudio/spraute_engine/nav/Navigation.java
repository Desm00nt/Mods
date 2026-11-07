package org.sprautestudio.spraute_engine.nav;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;

public final class Navigation {
    private static final Map<UUID, Map<String, Navigation.Target>> TARGETS = new HashMap<>();
    private static int tick;

    private Navigation() {
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END && ++tick % 5 == 0) {
                update(e.getServer());
            }
        });
        Hooks.PLAYER_LOGGED_OUT.add(e -> TARGETS.remove(e.getEntity().getUUID()));
    }

    public static void start(ServerPlayer p, Navigation.Target t) {
        TARGETS.computeIfAbsent(p.getUUID(), k -> new LinkedHashMap<>()).put(t.id(), t);
        send(p, t);
    }

    public static void stop(ServerPlayer p, String id) {
        Map<String, Navigation.Target> mine = TARGETS.get(p.getUUID());
        if (mine != null) {
            Iterator<String> it = mine.keySet().iterator();

            while (it.hasNext()) {
                String k = it.next();
                if (id.isEmpty() || k.equals(id)) {
                    it.remove();
                    SprauteNetwork.sendTo(p, new Packets.NavUpdate(k, "", 0, 0.0, 0.0, 0.0, true));
                }
            }
        }
    }

    public static void unload() {
        TARGETS.clear();
    }

    private static void update(MinecraftServer server) {
        if (!TARGETS.isEmpty()) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Map<String, Navigation.Target> mine = TARGETS.get(p.getUUID());
                if (mine != null && !mine.isEmpty()) {
                    Iterator<Navigation.Target> it = mine.values().iterator();

                    while (it.hasNext()) {
                        Navigation.Target t = it.next();

                        boolean there;
                        try {
                            there = t.arrived().test(p);
                        } catch (RuntimeException var8) {
                            there = false;
                        }

                        if (!there) {
                            send(p, t);
                        } else {
                            it.remove();
                            SprauteNetwork.sendTo(p, new Packets.NavUpdate(t.id(), "", 0, 0.0, 0.0, 0.0, true));
                            Map<String, Object> d = new HashMap<>();
                            d.put("id", t.id());
                            d.put("title", t.title());
                            GameEvents.post("navarrive", p, d);
                        }
                    }
                }
            }
        }
    }

    private static void send(ServerPlayer p, Navigation.Target t) {
        Vec3 at;
        try {
            at = t.where().get();
        } catch (RuntimeException var4) {
            at = null;
        }

        if (at != null) {
            SprauteNetwork.sendTo(p, new Packets.NavUpdate(t.id(), t.title(), t.color(), at.x, at.y, at.z, false));
        }
    }

    public record Target(String id, String title, int color, Supplier<Vec3> where, Predicate<ServerPlayer> arrived) {
    }
}
