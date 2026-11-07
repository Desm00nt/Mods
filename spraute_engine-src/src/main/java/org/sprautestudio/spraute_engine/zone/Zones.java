package org.sprautestudio.spraute_engine.zone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.camera.Cams;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.post.Posts;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class Zones {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Zones.Saved saved;
    private static final Map<String, ZoneDef> PARSED = new HashMap<>();
    private static final Map<String, String> PARSED_FROM = new HashMap<>();
    private static final Map<UUID, Set<String>> INSIDE = new HashMap<>();
    private static final Map<UUID, Vec3> PREV = new HashMap<>();
    private static final Map<UUID, Long> WARNED = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static String shownJson = "";
    private static int shownAt;

    private Zones() {
    }

    public static String key(String project, String name) {
        return project + "/" + name;
    }

    private static ZoneDef parse(String key, String json) {
        if (json == null) {
            return null;
        } else {
            if (!json.equals(PARSED_FROM.get(key))) {
                if (PARSED_FROM.containsKey(key) && saved().zones.remove(key) != null) {
                    save();
                    shownJson = "";
                }

                PARSED.put(key, ZoneDef.fromJson(json));
                PARSED_FROM.put(key, json);
            }

            return PARSED.get(key);
        }
    }

    public static ZoneDef def(String project, String name) {
        return parse(key(project, name), SprauteRuntime.get().gui(project, "zone:" + name));
    }

    private static Map<String, ZoneDef> all() {
        Map<String, ZoneDef> out = new LinkedHashMap<>();

        for (Entry<String, Map<String, String>> p : SprauteRuntime.get().allGuis().entrySet()) {
            for (Entry<String, String> e : p.getValue().entrySet()) {
                if (e.getKey().startsWith("zone:")) {
                    String key = key(p.getKey(), e.getKey().substring(5));
                    ZoneDef d = parse(key, e.getValue());
                    if (d != null) {
                        out.put(key, d);
                    }
                }
            }
        }

        return out;
    }

    private static boolean enabled(String key, ZoneDef d) {
        Zones.Override o = saved().zones.get(key);
        return o != null && o.enabled != null ? o.enabled : d.enabled;
    }

    private static String wall(String key, ZoneDef d) {
        Zones.Override o = saved().zones.get(key);
        return o != null && o.wall != null ? o.wall : d.wall;
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("zones.json");
    }

    private static Zones.Saved saved() {
        if (saved != null) {
            return saved;
        } else {
            saved = new Zones.Saved();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    Zones.Saved s = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), Zones.Saved.class);
                    if (s != null) {
                        if (s.zones == null) {
                            s.zones = new LinkedHashMap<>();
                        }

                        if (s.seen == null) {
                            s.seen = new LinkedHashMap<>();
                        }

                        saved = s;
                    }
                } catch (Exception var2) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read zones", (Throwable)var2);
                }
            }

            return saved;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && saved != null) {
            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson(saved), StandardCharsets.UTF_8);
            } catch (IOException var2) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save zones", (Throwable)var2);
            }
        }
    }

    public static void unload() {
        saved = null;
        shownJson = "";
        PARSED.clear();
        PARSED_FROM.clear();
        INSIDE.clear();
        PREV.clear();
        WARNED.clear();
    }

    private static String shownNow(Map<String, ZoneDef> zones) {
        JsonObject o = new JsonObject();

        for (Entry<String, ZoneDef> e : zones.entrySet()) {
            ZoneDef d = e.getValue();
            String w = wall(e.getKey(), d);
            if (enabled(e.getKey(), d) && (!d.hideInGame || !w.equals("none"))) {
                ZoneDef c = ZoneDef.fromJson(d.toJson());
                c.wall = w;
                o.add(e.getKey(), JsonParser.parseString(c.toJson()));
            }
        }

        return o.toString();
    }

    public static void syncTo(ServerPlayer p) {
        SprauteNetwork.sendTo(p, new Packets.ZoneShow(shownNow(all())));
    }

    public static void tick(MinecraftServer server) {
        Map<String, ZoneDef> zones = all();
        if (++shownAt >= 40) {
            shownAt = 0;
            String js = shownNow(zones);
            if (!js.equals(shownJson)) {
                shownJson = js;
                SprauteNetwork.sendToAll(new Packets.ZoneShow(js));
            }
        }

        if (zones.isEmpty()) {
            INSIDE.clear();
        } else {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (!p.isSpectator()) {
                    UUID id = p.getUUID();
                    Set<String> was = INSIDE.computeIfAbsent(id, k -> new HashSet<>());
                    Vec3 pos = p.position();
                    String dim = Mc.level(p).dimension().location().toString();
                    boolean pushed = false;
                    Vec3 before = PREV.get(id);
                    boolean teleported = before == null || before.distanceToSqr(pos) > 9.0;
                    if (teleported) {
                        pushed = true;
                    }

                    Set<String> now = new HashSet<>();

                    for (Entry<String, ZoneDef> e : zones.entrySet()) {
                        ZoneDef d = e.getValue();
                        String key = e.getKey();
                        if (enabled(key, d) && (d.dimension.isEmpty() || d.dimension.equals(dim))) {
                            boolean in = d.holds(pos.x, pos.y, pos.z);
                            String w = wall(key, d);
                            if (!pushed && w.equals("in") && was.contains(key) && !in) {
                                pushed = pushBack(p, d);
                                in = true;
                            } else if (!pushed && w.equals("out") && !was.contains(key) && in) {
                                pushed = pushBack(p, d);
                                in = false;
                            }

                            if (in) {
                                now.add(key);
                            }
                        }
                    }

                    for (String key : now) {
                        if (!was.contains(key)) {
                            entered(p, key, zones.get(key));
                        }
                    }

                    for (String keyx : was) {
                        if (!now.contains(keyx)) {
                            ZoneDef left = zones.get(keyx);
                            if (left != null && !left.post.isEmpty()) {
                                Posts.zoneLeft(p, keyx, (int)(left.postFade * 20.0));
                            }

                            if (left != null && !left.camera.isEmpty()) {
                                Cams.zoneLeft(p, keyx, left.cameraFade);
                            }

                            post(p, "zoneleave", keyx);
                        }
                    }

                    INSIDE.put(id, now);
                    if (!pushed) {
                        PREV.put(id, pos);
                    }
                }
            }

            INSIDE.keySet().removeIf(u -> server.getPlayerList().getPlayer(u) == null);
        }
    }

    private static boolean pushBack(ServerPlayer p, ZoneDef d) {
        Vec3 back = PREV.get(p.getUUID());
        if (back == null) {
            return false;
        } else {
            p.teleportTo(Mc.serverLevel(p), back.x, back.y, back.z, p.getYRot(), p.getXRot());
            p.setDeltaMovement(Vec3.ZERO);
            p.hurtMarked = true;
            long t = System.currentTimeMillis();
            if (!d.wallText.isEmpty() && t - WARNED.getOrDefault(p.getUUID(), 0L) > 2000L) {
                WARNED.put(p.getUUID(), t);
                p.displayClientMessage(Component.literal(d.wallText).withStyle(ChatFormatting.GOLD), true);
            }

            return true;
        }
    }

    private static void entered(ServerPlayer p, String key, ZoneDef d) {
        if (!d.title.isEmpty() && !d.titleMode.equals("never")) {
            Set<String> seen = saved().seen.computeIfAbsent(p.getUUID().toString(), k -> new LinkedHashSet<>());
            if (d.titleMode.equals("always") || !seen.contains(key)) {
                if (seen.add(key)) {
                    save();
                }

                SprauteNetwork.sendTo(p, new Packets.ZoneTitle(d.title, d.subtitle));
            }
        }

        if (!d.post.isEmpty()) {
            Posts.zoneEntered(p, key, key.substring(0, Math.max(0, key.indexOf(47))), d.post, (int)(d.postFade * 20.0));
        }

        if (!d.camera.isEmpty()) {
            Cams.zoneEntered(p, key, key.substring(0, Math.max(0, key.indexOf(47))), d.camera, d.cameraFade);
        }

        post(p, "zoneenter", key);
    }

    private static void post(ServerPlayer p, String type, String key) {
        Map<String, Object> data = new HashMap<>();
        data.put("zone", key.substring(key.indexOf(47) + 1));
        GameEvents.post(type, p, data);
    }

    public static boolean setEnabled(String project, String name, boolean on) {
        if (def(project, name) == null) {
            return false;
        } else {
            saved().zones.computeIfAbsent(key(project, name), k -> new Zones.Override()).enabled = on;
            save();
            shownAt = 40;
            return true;
        }
    }

    public static void sourcesSaved(String project) {
        if (saved().zones.keySet().removeIf(k -> k.startsWith(project + "/"))) {
            save();
            shownAt = 40;
            shownJson = "";
        }
    }

    public static boolean setWall(String project, String name, String mode) {
        if (def(project, name) == null) {
            return false;
        } else {
            String var4 = mode.toLowerCase();

            String m = switch (var4) {
                case "in", "inside", "внутри", "держать" -> "in";
                case "out", "outside", "снаружи", "не пускать" -> "out";
                default -> "none";
            };
            saved().zones.computeIfAbsent(key(project, name), k -> new Zones.Override()).wall = m;
            save();
            shownAt = 40;
            return true;
        }
    }

    public static boolean playerIn(ServerPlayer p, String project, String name) {
        if (p == null) {
            return false;
        } else {
            Set<String> in = INSIDE.get(p.getUUID());
            if (in != null && in.contains(key(project, name))) {
                return true;
            } else {
                ZoneDef d = def(project, name);
                return d != null && enabled(key(project, name), d) && d.holds(p.getX(), p.getY(), p.getZ());
            }
        }
    }

    public static boolean contains(String project, String name, Vec3 at) {
        ZoneDef d = require(project, name);
        return d.holds(at.x, at.y, at.z);
    }

    public static ZoneDef require(String project, String name) {
        ZoneDef d = def(project, name);
        if (d == null) {
            throw new SprauteRuntimeException("Зона «" + name + "» не найдена в папке «Зоны» проекта");
        } else {
            return d;
        }
    }

    public static Vec3 randomPoint(String project, String name) {
        return require(project, name).randomPoint(RANDOM);
    }

    public static List<String> zonesOf(ServerPlayer p, String project) {
        List<String> out = new ArrayList<>();
        if (p == null) {
            return out;
        } else {
            Set<String> in = INSIDE.get(p.getUUID());
            if (in == null) {
                return out;
            } else {
                for (String k : in) {
                    if (k.startsWith(project + "/")) {
                        out.add(k.substring(project.length() + 1));
                    }
                }

                Collections.sort(out);
                return out;
            }
        }
    }

    static final class Override {
        Boolean enabled;
        String wall;
    }

    static final class Saved {
        Map<String, Zones.Override> zones = new LinkedHashMap<>();
        Map<String, Set<String>> seen = new LinkedHashMap<>();
    }
}
