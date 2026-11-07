package org.sprautestudio.spraute_engine.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.core.system.WorldClock;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class EngineServer {
    private static EngineConfig config = new EngineConfig();
    private static double speedCarry;
    private static boolean clockPending;

    private EngineServer() {
    }

    public static EngineConfig config() {
        return config;
    }

    private static Path file(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("engine.json");
    }

    public static void load(MinecraftServer server) {
        Path f = file(server);
        EngineConfig c = new EngineConfig();
        if (Files.exists(f)) {
            try {
                c = EngineConfig.fromJson(Files.readString(f, StandardCharsets.UTF_8));
            } catch (IOException var4) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read {}", f, var4);
            }
        }

        config = c;
        applyClock(server);
    }

    public static void applyClock(MinecraftServer server) {
        if (server.overworld() == null) {
            clockPending = true;
        } else {
            clockPending = false;
            if (!config.time) {
                EngineConfig.Clock k = config.clock;
                if (k.manage) {
                    boolean run = k.cycle && k.fixed < 0;
                    server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(run, server);
                    if (k.fixed >= 0) {
                        setTimeOfDay(server, k.fixed);
                    }
                }
            }
        }
    }

    public static void setTimeOfDay(MinecraftServer server, long timeOfDay) {
        if (config.time) {
            WorldClock.setTimeOfDay(server, timeOfDay);
        } else {
            ServerLevel level = server.overworld();
            if (level != null) {
                long day = Math.floorDiv(level.getDayTime(), 24000L);
                long t = day * 24000L + Math.floorMod(timeOfDay, 24000L);

                for (ServerLevel l : server.getAllLevels()) {
                    l.setDayTime(t);
                }

                syncTime(server);
            }
        }
    }

    public static void addTime(MinecraftServer server, long ticks) {
        if (config.time) {
            WorldClock.add(server, ticks);
        } else {
            long now = server.overworld().getDayTime();

            for (ServerLevel l : server.getAllLevels()) {
                l.setDayTime(now + ticks);
            }

            syncTime(server);
        }
    }

    private static void syncTime(MinecraftServer server) {
        boolean tick = server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            ServerLevel l = Mc.serverLevel(p);
            p.connection.send(new ClientboundSetTimePacket(l.getGameTime(), l.getDayTime(), tick));
        }
    }

    public static void tick(MinecraftServer server) {
        if (config.time && server.overworld() != null) {
            WorldClock.tick(server);
        }

        if (clockPending) {
            try {
                applyClock(server);
            } catch (RuntimeException var6) {
                clockPending = false;
                SprauteEngine.LOGGER.warn("Spraute: cannot apply the time settings", (Throwable)var6);
            }
        }

        EngineConfig.Clock k = config.clock;
        if (!config.time && k.manage && k.cycle && k.fixed < 0 && k.speed != 1.0F) {
            if (server.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
                speedCarry = speedCarry + (k.speed - 1.0F);
                long whole = (long)speedCarry;
                if (whole != 0L) {
                    speedCarry -= whole;

                    for (ServerLevel l : server.getAllLevels()) {
                        l.setDayTime(l.getDayTime() + whole);
                    }

                    if (server.getTickCount() % 4 == 0) {
                        syncTime(server);
                    }
                }
            }
        }
    }

    public static void unload() {
        BodyRules.unload();
        config = new EngineConfig();
    }

    public static void set(MinecraftServer server, EngineConfig c) {
        if (config.time && !c.time) {
            WorldClock.release(server);
        }

        config = c;
        applyClock(server);
        Path f = file(server);

        try {
            Files.createDirectories(f.getParent());
            Files.writeString(f, c.toJson(), StandardCharsets.UTF_8);
        } catch (IOException var5) {
            SprauteEngine.LOGGER.warn("Spraute: cannot save {}", f, var5);
        }

        SprauteNetwork.sendToEveryone(new Packets.EngineSync(c.toJson()));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ProjectContent.sendIndex(player);
        }
    }

    public static boolean enabled(String system) {
        String var1 = system.toLowerCase();

        return switch (var1) {
            case "health" -> config.health;
            case "coins" -> config.currency;
            case "time" -> config.time;
            case "inventory" -> config.inventory;
            case "slots" -> config.slots;
            case "chat" -> config.chat;
            default -> false;
        };
    }

    public static void setSystem(String system, boolean on) {
        MinecraftServer server = Platform.get().server();
        if (server != null && enabled(system) != on) {
            EngineConfig c = config.copy();
            String var4 = system.toLowerCase();
            switch (var4) {
                case "health":
                    c.health = on;
                    break;
                case "coins":
                    c.currency = on;
                    break;
                case "time":
                    c.time = on;
                    break;
                case "inventory":
                    c.inventory = on;
                    break;
                case "slots":
                    c.slots = on;
                    break;
                case "chat":
                    c.chat = on;
                    break;
                default:
                    return;
            }

            set(server, c);
        }
    }

    public static void sync(ServerPlayer player) {
        SprauteNetwork.sendTo(player, new Packets.EngineSync(config.toJson()));
    }
}
