package org.sprautestudio.spraute_engine.camera;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Cams {
    private static final Map<Integer, List<String>> ACTIVE = new LinkedHashMap<>();

    private Cams() {
    }

    public static void registerHooks() {
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                ACTIVE.forEach((id, ops) -> {
                    for (String op : ops) {
                        SprauteNetwork.sendTo(p, new Packets.CameraOp(id, op));
                    }
                });
            }
        });
    }

    public static String json(String project, String name) {
        String own = SprauteRuntime.get().gui(project, "cam:" + name);
        if (own != null) {
            return own;
        } else {
            CamProfile.Preset p = CamProfile.preset(name);
            return p != null ? p.profile().toJson() : null;
        }
    }

    private static JsonObject op() {
        JsonObject o = new JsonObject();
        o.addProperty("op", "rig");
        return o;
    }

    private static void send(int script, JsonObject o, boolean fresh) {
        List<String> list = (List<String>)(fresh ? new ArrayList<>() : ACTIVE.computeIfAbsent(script, k -> new ArrayList<>()));
        list.add(o.toString());
        if (fresh) {
            ACTIVE.put(script, list);
        }

        SprauteNetwork.sendToAll(new Packets.CameraOp(script, o.toString()));
    }

    public static void rig(int script, String json, double seconds) {
        JsonObject o = op();
        o.addProperty("profile", json);
        o.addProperty("time", seconds);
        send(script, o, true);
    }

    public static void set(int script, String param, double value, double seconds) {
        JsonObject o = op();
        o.addProperty("param", param);
        o.addProperty("value", value);
        o.addProperty("time", seconds);
        send(script, o, false);
    }

    public static void off(int script, double seconds) {
        ACTIVE.remove(script);
        JsonObject o = op();
        o.addProperty("time", seconds);
        SprauteNetwork.sendToAll(new Packets.CameraOp(script, o.toString()));
    }

    public static void zoneEntered(ServerPlayer p, String key, String project, String camera, double seconds) {
        String json = json(project, camera);
        if (json != null) {
            JsonObject o = op();
            o.addProperty("zone", key);
            o.addProperty("profile", json);
            o.addProperty("time", seconds);
            SprauteNetwork.sendTo(p, new Packets.CameraOp(-1, o.toString()));
        }
    }

    public static void zoneLeft(ServerPlayer p, String key, double seconds) {
        JsonObject o = op();
        o.addProperty("zone", key);
        o.addProperty("time", seconds);
        SprauteNetwork.sendTo(p, new Packets.CameraOp(-1, o.toString()));
    }

    public static void storyStopped(int script) {
        if (ACTIVE.containsKey(script)) {
            off(script, 0.6);
        }
    }
}
