package org.sprautestudio.spraute_engine.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class PlayerVars {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Map<String, Map<String, Object>> players;

    private PlayerVars() {
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("playervars.json");
    }

    private static Map<String, Map<String, Object>> players() {
        if (players != null) {
            return players;
        } else {
            players = new LinkedHashMap<>();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();

                    for (Entry<String, JsonElement> pe : root.entrySet()) {
                        Map<String, Object> values = new LinkedHashMap<>();

                        for (Entry<String, JsonElement> e : pe.getValue().getAsJsonObject().entrySet()) {
                            JsonPrimitive v = e.getValue().getAsJsonPrimitive();
                            values.put(e.getKey(), v.isNumber() ? v.getAsDouble() : (v.isBoolean() ? v.getAsBoolean() : v.getAsString()));
                        }

                        players.put(pe.getKey(), values);
                    }
                } catch (Exception var8) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read player variables", (Throwable)var8);
                }
            }

            return players;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && players != null) {
            JsonObject root = new JsonObject();
            players.forEach((player, values) -> {
                JsonObject o = new JsonObject();
                values.forEach((k, v) -> {
                    if (v instanceof Number n) {
                        o.addProperty(k, n.doubleValue());
                    } else if (v instanceof Boolean b) {
                        o.addProperty(k, b);
                    } else {
                        o.addProperty(k, String.valueOf(v));
                    }
                });
                root.add(player, o);
            });

            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson((JsonElement)root), StandardCharsets.UTF_8);
            } catch (IOException var3) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save player variables", (Throwable)var3);
            }
        }
    }

    private static Map<String, Object> of(ServerPlayer p) {
        return players().computeIfAbsent(p.getUUID().toString(), k -> new LinkedHashMap<>());
    }

    public static Object get(ServerPlayer p, String name, Object fallback) {
        Object v = of(p).get(name);
        return v != null ? v : fallback;
    }

    public static Map<String, Object> all(ServerPlayer p) {
        return new LinkedHashMap<>(of(p));
    }

    public static boolean has(ServerPlayer p, String name) {
        return of(p).containsKey(name);
    }

    public static void set(ServerPlayer p, String name, Object value) {
        if (value instanceof Number n) {
            value = n.doubleValue();
        } else if (!(value instanceof Boolean) && !(value instanceof String)) {
            value = Eval.str(value);
        }

        of(p).put(name, value);
        save();
    }

    public static void add(ServerPlayer p, String name, double by) {
        set(p, name, (of(p).get(name) instanceof Number n ? n.doubleValue() : 0.0) + by);
    }

    public static void clear(ServerPlayer p, String name) {
        if (of(p).remove(name) != null) {
            save();
        }
    }

    public static void unload() {
        players = null;
    }
}
