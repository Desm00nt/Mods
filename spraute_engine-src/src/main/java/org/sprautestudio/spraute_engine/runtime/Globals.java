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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class Globals {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static Map<String, Object> values;

    private Globals() {
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("globals.json");
    }

    private static Map<String, Object> values() {
        if (values != null) {
            return values;
        } else {
            values = new LinkedHashMap<>();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    JsonObject o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();

                    for (Entry<String, JsonElement> e : o.entrySet()) {
                        JsonElement v = e.getValue();
                        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
                            values.put(e.getKey(), v.getAsDouble());
                        } else if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isBoolean()) {
                            values.put(e.getKey(), v.getAsBoolean());
                        } else if (v.isJsonPrimitive()) {
                            values.put(e.getKey(), v.getAsString());
                        }
                    }
                } catch (Exception var5) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read globals", (Throwable)var5);
                }
            }

            return values;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && values != null) {
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

            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson((JsonElement)o), StandardCharsets.UTF_8);
            } catch (IOException var3) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save globals", (Throwable)var3);
            }
        }
    }

    public static Object get(String name, Object fallback) {
        Object v = values().get(name);
        return v != null ? v : fallback;
    }

    public static boolean has(String name) {
        return values().containsKey(name);
    }

    public static void set(String name, Object value) {
        if (value instanceof Number n) {
            value = n.doubleValue();
        } else if (!(value instanceof Boolean) && !(value instanceof String)) {
            value = Eval.str(value);
        }

        values().put(name, value);
        save();
    }

    public static void add(String name, double by) {
        set(name, (values().get(name) instanceof Number n ? n.doubleValue() : 0.0) + by);
    }

    public static void clear(String name) {
        if (values().remove(name) != null) {
            save();
        }
    }

    public static Map<String, Object> all() {
        return values();
    }

    public static void unload() {
        values = null;
    }
}
