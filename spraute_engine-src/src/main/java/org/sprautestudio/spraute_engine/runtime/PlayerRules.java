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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;

public final class PlayerRules {
    private static final Map<String, PlayerRules.Stat> STATS = new HashMap<>();
    private static final double DEFAULT_BLOCK_REACH = 4.5;
    private static final double DEFAULT_ATTACK_REACH = 3.0;
    private static final double REACH_MARGIN = 1.5;
    private static final double GAME_REACH_LIMIT_SQR = 36.0;
    private static final double DEFAULT_FLY_SPEED = 0.05;
    private static final float DEFAULT_STEP = 0.6F;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<UUID, Map<String, Double>> rules;
    private static final Map<UUID, Map<String, PlayerRules.Timed>> TIMED = new HashMap<>();
    private static volatile Map<String, Double> clientRules = Map.of();
    private static volatile Predicate<Player> clientOwn = p -> false;

    private PlayerRules() {
    }

    private static PlayerRules.Stat stat(String name) {
        return switch (name) {
            case "reach" -> Platform.get().blockReach() == null ? null : new PlayerRules.Stat(Platform.get().blockReach(), Operation.ADDITION, 4.5);
            case "attack_reach" -> Platform.get().entityReach() == null ? null : new PlayerRules.Stat(Platform.get().entityReach(), Operation.ADDITION, 3.0);
            default -> (PlayerRules.Stat)STATS.get(name);
        };
    }

    public static double reachLimitSqr(Player p) {
        double reach = Math.max(number(p, "reach", 0.0), number(p, "attack_reach", 0.0)) + 1.5;
        return Math.max(36.0, reach * reach);
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END) {
                tick();
            }
        });
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                sync(p);
                apply(p);
            }
        });
        Hooks.PLAYER_RESPAWN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                apply(p);
            }
        });
    }

    private static Map<String, Double> of(Player p) {
        if (Mc.level(p).isClientSide) {
            return clientOwn.test(p) ? clientRules : null;
        } else {
            return rules().get(p.getUUID());
        }
    }

    public static boolean can(Player p, String rule) {
        Map<String, Double> m = of(p);
        if (m != null && !m.isEmpty() && !p.isCreative() && !p.isSpectator()) {
            Double v = m.get(rule);
            return v == null || v != 0.0;
        } else {
            return true;
        }
    }

    public static double number(Player p, String rule, double def) {
        Map<String, Double> m = of(p);
        if (m != null && !m.isEmpty()) {
            Double v = m.get(rule);
            return v != null ? v : def;
        } else {
            return def;
        }
    }

    public static float damage(Player p, DamageSource source, float amount) {
        Map<String, Double> m = of(p);
        if (m == null || m.isEmpty()) {
            return amount;
        } else if (!can(p, "take_damage")) {
            return 0.0F;
        } else {
            String id = source.getMsgId();
            if (id.equals("drown") && !can(p, "drown")) {
                return 0.0F;
            } else if ((id.equals("inFire") || id.equals("onFire") || id.equals("lava") || id.equals("hotFloor")) && !can(p, "burn")) {
                return 0.0F;
            } else {
                float k = (float)number(p, "damage_taken", 1.0);
                if (id.equals("fall")) {
                    k *= (float)number(p, "fall_damage", 1.0);
                }

                return amount * k;
            }
        }
    }

    public static void set(ServerPlayer p, String name, double value, double seconds) {
        PlayerRuleDefs.Rule rule = PlayerRuleDefs.get(name);
        if (rule == null) {
            throw new SprauteRuntimeException("Нет такого правила игрока: " + name);
        } else {
            Map<String, Double> m = rules().computeIfAbsent(p.getUUID(), k -> new HashMap<>());
            Map<String, PlayerRules.Timed> timed = TIMED.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
            if (seconds > 0.0) {
                PlayerRules.Timed old = timed.get(name);
                timed.put(name, new PlayerRules.Timed(Mechanics.now() + (int)Math.round(seconds * 20.0), old != null ? old.before : m.get(name)));
            } else {
                timed.remove(name);
            }

            m.put(name, PlayerRuleDefs.clamp(rule, value));
            changed(p);
        }
    }

    public static boolean timed(ServerPlayer p, String name) {
        Map<String, PlayerRules.Timed> timed = TIMED.get(p.getUUID());
        return timed != null && timed.containsKey(name);
    }

    public static void reset(ServerPlayer p, String name) {
        Map<String, Double> m = rules().get(p.getUUID());
        Map<String, PlayerRules.Timed> timed = TIMED.get(p.getUUID());
        if (timed != null) {
            timed.remove(name);
        }

        if (m != null && m.remove(name) != null) {
            restore(p, name);
            changed(p);
        }
    }

    public static void resetAll(ServerPlayer p) {
        Map<String, Double> m = rules().get(p.getUUID());
        TIMED.remove(p.getUUID());
        if (m != null) {
            List<String> names = new ArrayList<>(m.keySet());
            m.clear();

            for (String n : names) {
                restore(p, n);
            }

            changed(p);
        }
    }

    private static void changed(ServerPlayer p) {
        apply(p);
        sync(p);
        save();
    }

    private static void sync(ServerPlayer p) {
        Map<String, Double> m = rules().get(p.getUUID());
        int n = m == null ? 0 : m.size();
        String[] names = new String[n];
        double[] values = new double[n];
        if (m != null) {
            int i = 0;

            for (Entry<String, Double> e : m.entrySet()) {
                names[i] = e.getKey();
                values[i++] = e.getValue();
            }
        }

        SprauteNetwork.sendTo(p, new Packets.PlayerRulesSync(names, values));
    }

    public static void receive(Predicate<Player> own, String[] names, double[] values) {
        Map<String, Double> m = new HashMap<>();

        for (int i = 0; i < names.length; i++) {
            m.put(names[i], values[i]);
        }

        clientOwn = own;
        clientRules = m;
    }

    public static void clientLeft() {
        clientOwn = p -> false;
        clientRules = Map.of();
    }

    private static void tick() {
        if (rules != null && !rules.isEmpty()) {
            MinecraftServer server = Platform.get().server();
            if (server != null) {
                expire(server);
                if (server.getTickCount() % 5 == 0) {
                    for (UUID id : new ArrayList<>(rules.keySet())) {
                        ServerPlayer p = server.getPlayerList().getPlayer(id);
                        if (p != null) {
                            apply(p);
                        }
                    }
                }
            }
        }
    }

    private static void expire(MinecraftServer server) {
        if (!TIMED.isEmpty()) {
            int now = Mechanics.now();

            for (UUID id : new ArrayList<>(TIMED.keySet())) {
                Map<String, PlayerRules.Timed> timed = TIMED.get(id);
                List<String> over = new ArrayList<>();
                timed.forEach((namex, t) -> {
                    if (t.until <= now) {
                        over.add(namex);
                    }
                });
                if (!over.isEmpty()) {
                    ServerPlayer p = server.getPlayerList().getPlayer(id);
                    Map<String, Double> m = rules().get(id);

                    for (String name : over) {
                        Double before = timed.remove(name).before;
                        if (m != null) {
                            if (before != null) {
                                m.put(name, before);
                            } else {
                                m.remove(name);
                            }

                            if (p != null && before == null) {
                                restore(p, name);
                            }
                        }
                    }

                    if (timed.isEmpty()) {
                        TIMED.remove(id);
                    }

                    if (p != null) {
                        changed(p);
                    } else {
                        save();
                    }
                }
            }
        }
    }

    private static UUID modifierId(String rule) {
        return UUID.nameUUIDFromBytes(("spraute:player:" + rule).getBytes(StandardCharsets.UTF_8));
    }

    private static void apply(ServerPlayer p) {
        Map<String, Double> m = rules().get(p.getUUID());
        if (m != null && !m.isEmpty()) {
            boolean abilities = false;

            for (Entry<String, Double> e : m.entrySet()) {
                String name = e.getKey();
                double v = e.getValue();
                PlayerRules.Stat stat = stat(name);
                if (stat != null) {
                    AttributeInstance ai = p.getAttribute(stat.attribute);
                    if (ai != null) {
                        UUID id = modifierId(name);
                        double amount = v - stat.base;
                        AttributeModifier cur = ai.getModifier(id);
                        if (cur == null || cur.getAmount() != amount) {
                            ai.removeModifier(id);
                            ai.addTransientModifier(new AttributeModifier(id, "spraute " + name, amount, stat.op));
                            if (p.getHealth() > p.getMaxHealth()) {
                                p.setHealth(p.getMaxHealth());
                            }
                        }
                    }
                } else {
                    switch (name) {
                        case "fly":
                            if (!p.isCreative() && !p.isSpectator()) {
                                boolean on = v != 0.0;
                                if (p.getAbilities().mayfly != on) {
                                    p.getAbilities().mayfly = on;
                                    if (!on) {
                                        p.getAbilities().flying = false;
                                    }

                                    abilities = true;
                                }
                            }
                            break;
                        case "fly_speed":
                            float speed = (float)(0.05 * v);
                            if (p.getAbilities().getFlyingSpeed() != speed) {
                                p.getAbilities().setFlyingSpeed(speed);
                                abilities = true;
                            }
                            break;
                        case "step_height":
                            Mc.setMaxUpStep(p, (float)v);
                    }
                }
            }

            if (abilities) {
                p.onUpdateAbilities();
            }
        }
    }

    private static void restore(ServerPlayer p, String name) {
        PlayerRules.Stat stat = stat(name);
        if (stat != null) {
            AttributeInstance ai = p.getAttribute(stat.attribute);
            if (ai != null) {
                ai.removeModifier(modifierId(name));
            }

            if (p.getHealth() > p.getMaxHealth()) {
                p.setHealth(p.getMaxHealth());
            }
        } else {
            switch (name) {
                case "fly":
                    if (p.isCreative() || p.isSpectator()) {
                        return;
                    }

                    p.getAbilities().mayfly = false;
                    p.getAbilities().flying = false;
                    p.onUpdateAbilities();
                    break;
                case "fly_speed":
                    p.getAbilities().setFlyingSpeed(0.05F);
                    p.onUpdateAbilities();
                    break;
                case "step_height":
                    Mc.setMaxUpStep(p, 0.6F);
            }
        }
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("playerrules.json");
    }

    private static Map<UUID, Map<String, Double>> rules() {
        if (rules != null) {
            return rules;
        } else {
            rules = new HashMap<>();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();

                    for (Entry<String, JsonElement> pe : root.entrySet()) {
                        Map<String, Double> values = new HashMap<>();

                        for (Entry<String, JsonElement> e : pe.getValue().getAsJsonObject().entrySet()) {
                            if (PlayerRuleDefs.get(e.getKey()) != null) {
                                values.put(e.getKey(), e.getValue().getAsDouble());
                            }
                        }

                        rules.put(UUID.fromString(pe.getKey()), values);
                    }
                } catch (Exception var7) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read player rules", (Throwable)var7);
                }
            }

            return rules;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && rules != null) {
            JsonObject root = new JsonObject();
            rules.forEach((id, values) -> {
                Map<String, PlayerRules.Timed> timed = TIMED.getOrDefault(id, Map.of());
                JsonObject o = new JsonObject();
                values.forEach((k, v) -> {
                    PlayerRules.Timed t = timed.get(k);
                    Double keep = t != null ? t.before : v;
                    if (keep != null) {
                        o.addProperty(k, keep);
                    }
                });
                if (o.size() > 0) {
                    root.add(id.toString(), o);
                }
            });

            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson((JsonElement)root), StandardCharsets.UTF_8);
            } catch (IOException var3) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save player rules", (Throwable)var3);
            }
        }
    }

    public static void unload() {
        rules = null;
        TIMED.clear();
    }

    static {
        STATS.put("move_speed", new PlayerRules.Stat(Attributes.MOVEMENT_SPEED, Operation.MULTIPLY_TOTAL, 1.0));
        STATS.put("attack_damage", new PlayerRules.Stat(Attributes.ATTACK_DAMAGE, Operation.MULTIPLY_TOTAL, 1.0));
        STATS.put("attack_speed", new PlayerRules.Stat(Attributes.ATTACK_SPEED, Operation.MULTIPLY_TOTAL, 1.0));
        STATS.put("attack_knockback", new PlayerRules.Stat(Attributes.ATTACK_KNOCKBACK, Operation.ADDITION, 0.0));
        STATS.put("knockback_resist", new PlayerRules.Stat(Attributes.KNOCKBACK_RESISTANCE, Operation.ADDITION, 0.0));
        STATS.put("max_health", new PlayerRules.Stat(Attributes.MAX_HEALTH, Operation.ADDITION, 20.0));
        STATS.put("armor", new PlayerRules.Stat(Attributes.ARMOR, Operation.ADDITION, 0.0));
        STATS.put("armor_toughness", new PlayerRules.Stat(Attributes.ARMOR_TOUGHNESS, Operation.ADDITION, 0.0));
        STATS.put("luck", new PlayerRules.Stat(Attributes.LUCK, Operation.ADDITION, 0.0));
    }

    private record Stat(Attribute attribute, Operation op, double base) {
    }

    private record Timed(int until, Double before) {
    }
}
