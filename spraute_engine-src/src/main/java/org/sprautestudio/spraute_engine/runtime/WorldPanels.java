package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class WorldPanels {
    private static final Map<String, Packets.WorldPanelShow> PANELS = new LinkedHashMap<>();
    private static final Map<String, Entity> WEARERS = new HashMap<>();
    private static final int CHECK_EVERY = 20;
    private static int ticks;

    private WorldPanels() {
    }

    public static void registerHooks() {
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                for (Packets.WorldPanelShow m : PANELS.values()) {
                    SprauteNetwork.sendTo(p, m);
                }
            }
        });
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END && !WEARERS.isEmpty() && ++ticks % 20 == 0) {
                checkWearers();
            }
        });
        Hooks.SERVER_STOPPING.add(e -> {
            PANELS.clear();
            WEARERS.clear();
        });
    }

    public static void place(Packets.WorldPanelShow panel) {
        place(panel, null);
    }

    public static void place(Packets.WorldPanelShow panel, Entity wearer) {
        PANELS.put(panel.id(), panel);
        if (wearer != null) {
            WEARERS.put(panel.id(), wearer);
        } else {
            WEARERS.remove(panel.id());
        }

        SprauteNetwork.sendToAll(panel);
    }

    public static void remove(int script, String id) {
        Packets.WorldPanelShow panel = PANELS.get(id);
        if (panel != null && panel.scriptId() == script) {
            PANELS.remove(id);
            WEARERS.remove(id);
            SprauteNetwork.sendToAll(new Packets.WorldPanelHide(script, id));
        }
    }

    public static void removeAll(int script) {
        if (PANELS.values().removeIf(p -> p.scriptId() == script)) {
            WEARERS.keySet().retainAll(PANELS.keySet());
            SprauteNetwork.sendToAll(new Packets.WorldPanelHide(script, ""));
        }
    }

    private static void checkWearers() {
        MinecraftServer server = Platform.get().server();
        if (server != null) {
            for (Entry<String, Entity> e : new ArrayList<>(WEARERS.entrySet())) {
                Entity wearer = e.getValue();
                Packets.WorldPanelShow panel = PANELS.get(e.getKey());
                if (panel != null) {
                    if (wearer instanceof ServerPlayer) {
                        if (server.getPlayerList().getPlayer(wearer.getUUID()) == null) {
                            remove(panel.scriptId(), panel.id());
                        }
                    } else if (wearer.isRemoved()) {
                        RemovalReason why = wearer.getRemovalReason();
                        if (why != null && why.shouldDestroy()) {
                            remove(panel.scriptId(), panel.id());
                        } else {
                            for (ServerLevel level : server.getAllLevels()) {
                                Entity back = level.getEntity(wearer.getUUID());
                                if (back != null) {
                                    place(panel.onEntity(back.getId()), back);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean has(int script) {
        for (Packets.WorldPanelShow p : PANELS.values()) {
            if (p.scriptId() == script) {
                return true;
            }
        }

        return false;
    }
}
