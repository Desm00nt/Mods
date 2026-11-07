package org.sprautestudio.spraute_engine.post;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Posts {
    private static final Map<String, List<Packets.PostFx>> ACTIVE = new LinkedHashMap<>();

    private Posts() {
    }

    public static void registerHooks() {
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                for (List<Packets.PostFx> list : ACTIVE.values()) {
                    for (Packets.PostFx m : list) {
                        SprauteNetwork.sendTo(p, m);
                    }
                }
            }
        });
    }

    public static String json(String project, String name) {
        String own = SprauteRuntime.get().gui(project, "post:" + name);
        if (own != null) {
            return own;
        } else {
            PostPresets.Preset p = PostPresets.get(name);
            return p != null ? p.profile().toJson() : null;
        }
    }

    private static String layer(int script) {
        return "story:" + script;
    }

    public static void play(int script, String json, int in, double power) {
        Packets.PostFx m = new Packets.PostFx("show", layer(script), json, in, -1, 0, (float)power);
        List<Packets.PostFx> list = new ArrayList<>();
        list.add(m);
        ACTIVE.put(layer(script), list);
        SprauteNetwork.sendToAll(m);
    }

    public static void set(int script, String param, String value, int time) {
        Packets.PostFx m = new Packets.PostFx("set", layer(script), param + "=" + value, time, 0, 0, 1.0F);
        ACTIVE.computeIfAbsent(layer(script), k -> new ArrayList<>()).add(m);
        SprauteNetwork.sendToAll(m);
    }

    public static void pulse(int script, String json, int in, int hold, int out, double power) {
        SprauteNetwork.sendToAll(new Packets.PostFx("show", "pulse:" + script, json, in, hold, out, (float)power));
    }

    public static void clear(int script, int time) {
        ACTIVE.remove(layer(script));
        SprauteNetwork.sendToAll(new Packets.PostFx("remove", layer(script), "", time, 0, 0, 1.0F));
    }

    public static void storyStopped(int script) {
        if (ACTIVE.containsKey(layer(script))) {
            clear(script, 20);
        }
    }

    public static void cutscene(String json, int in) {
        SprauteNetwork.sendToAll(new Packets.PostFx("show", "cutscene", json, in, -1, 0, 1.0F));
    }

    public static void zoneEntered(ServerPlayer p, String key, String project, String post, int in) {
        String json = json(project, post);
        if (json != null) {
            SprauteNetwork.sendTo(p, new Packets.PostFx("show", "zone:" + key, json, in, -1, 0, 1.0F));
        }
    }

    public static void zoneLeft(ServerPlayer p, String key, int out) {
        SprauteNetwork.sendTo(p, new Packets.PostFx("remove", "zone:" + key, "", out, 0, 0, 1.0F));
    }
}
