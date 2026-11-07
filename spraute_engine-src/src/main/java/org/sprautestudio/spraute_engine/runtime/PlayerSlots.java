package org.sprautestudio.spraute_engine.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.player.SlotPlan;

public final class PlayerSlots {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<UUID, SlotPlan> plans;
    private static volatile Predicate<Player> clientOwn = p -> false;
    private static volatile SlotPlan clientPlan;
    private static volatile EngineConfig clientEngine;

    private PlayerSlots() {
    }

    public static void registerHooks() {
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
            }
        });
    }

    private static SlotPlan planOf(Player p) {
        if (Mc.level(p).isClientSide) {
            return clientOwn.test(p) ? clientPlan : null;
        } else {
            return plans().get(p.getUUID());
        }
    }

    private static EngineConfig engine(Player p) {
        return Mc.level(p).isClientSide ? clientEngine : EngineServer.config();
    }

    private static boolean free(Player p) {
        return p.isCreative() || p.isSpectator();
    }

    public static boolean limited(Player p) {
        if (free(p)) {
            return false;
        } else if (planOf(p) != null) {
            return true;
        } else {
            EngineConfig cfg = engine(p);
            return cfg != null && cfg.slots;
        }
    }

    public static boolean locked(Player p, int cell) {
        if (free(p)) {
            return false;
        } else {
            SlotPlan plan = planOf(p);
            EngineConfig cfg = engine(p);
            if (plan != null) {
                return plan.isLocked(cell, cfg != null ? cfg.slotOrder : "top");
            } else {
                return cfg != null && cfg.slots ? cfg.cellClosed(cell) : false;
            }
        }
    }

    public static int extra(Player p) {
        if (free(p)) {
            return 0;
        } else {
            SlotPlan plan = planOf(p);
            return plan == null ? 0 : plan.extra();
        }
    }

    public static int size(Player p) {
        int open = 0;

        for (int i = 0; i < 36; i++) {
            if (!locked(p, i)) {
                open++;
            }
        }

        return open + extra(p);
    }

    private static SlotPlan own(ServerPlayer p) {
        return plans().computeIfAbsent(p.getUUID(), k -> new SlotPlan());
    }

    public static void lock(ServerPlayer p, String cells) {
        SlotPlan var10000 = own(p);
        var10000.locked = var10000.locked | checked(cells);
        changed(p);
    }

    public static void unlock(ServerPlayer p, String cells) {
        SlotPlan var10000 = own(p);
        var10000.locked = var10000.locked & ~checked(cells);
        changed(p);
    }

    public static void size(ServerPlayer p, double count) {
        own(p).size = SlotPlan.clampSize(count);
        changed(p);
    }

    public static void order(ServerPlayer p, String order) {
        own(p).order = order;
        changed(p);
    }

    public static void reset(ServerPlayer p) {
        if (plans().remove(p.getUUID()) != null) {
            changed(p);
        }
    }

    private static long checked(String cells) {
        try {
            return SlotPlan.parse(cells);
        } catch (IllegalArgumentException var2) {
            throw new SprauteRuntimeException(var2.getMessage());
        }
    }

    private static void changed(ServerPlayer p) {
        sync(p);
        save();
    }

    private static void sync(ServerPlayer p) {
        SlotPlan plan = plans().get(p.getUUID());
        SprauteNetwork.sendTo(
            p, plan == null ? new Packets.PlayerSlotsSync(false, 0L, 0, "") : new Packets.PlayerSlotsSync(true, plan.locked, plan.size, plan.order)
        );
    }

    public static void receive(Predicate<Player> own, Packets.PlayerSlotsSync msg) {
        clientOwn = own;
        if (!msg.own()) {
            clientPlan = null;
        } else {
            SlotPlan plan = new SlotPlan();
            plan.locked = msg.locked();
            plan.size = msg.size();
            plan.order = msg.order();
            clientPlan = plan;
        }
    }

    public static void clientEngine(EngineConfig cfg) {
        clientEngine = cfg;
    }

    public static void clientLeft() {
        clientOwn = p -> false;
        clientPlan = null;
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("playerslots.json");
    }

    private static Map<UUID, SlotPlan> plans() {
        if (plans != null) {
            return plans;
        } else {
            plans = new HashMap<>();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();

                    for (Entry<String, JsonElement> e : root.entrySet()) {
                        JsonObject o = e.getValue().getAsJsonObject();
                        SlotPlan plan = new SlotPlan();
                        plan.locked = o.get("locked").getAsLong();
                        plan.size = SlotPlan.clampSize(o.get("size").getAsDouble());
                        if (o.has("order")) {
                            plan.order = o.get("order").getAsString();
                        }

                        plans.put(UUID.fromString(e.getKey()), plan);
                    }
                } catch (Exception var6) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read player slots", (Throwable)var6);
                }
            }

            return plans;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && plans != null) {
            JsonObject root = new JsonObject();
            plans.forEach((id, plan) -> {
                JsonObject o = new JsonObject();
                o.addProperty("locked", plan.locked);
                o.addProperty("size", plan.size);
                o.addProperty("order", plan.order);
                root.add(id.toString(), o);
            });

            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson((JsonElement)root), StandardCharsets.UTF_8);
            } catch (IOException var3) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save player slots", (Throwable)var3);
            }
        }
    }

    public static void unload() {
        plans = null;
    }
}
