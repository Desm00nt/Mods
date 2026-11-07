package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public final class PlayerTeams {
    private PlayerTeams() {
    }

    private static Scoreboard board() {
        MinecraftServer server = SprauteRuntime.server();
        if (server == null) {
            throw new SprauteRuntimeException("Сервер ещё не запущен");
        } else {
            return server.getScoreboard();
        }
    }

    private static PlayerTeam team(String name) {
        if (name.isEmpty()) {
            throw new SprauteRuntimeException("Пустое имя команды");
        } else {
            Scoreboard board = board();
            PlayerTeam team = board.getPlayerTeam(name);
            if (team != null) {
                return team;
            } else {
                try {
                    return board.addPlayerTeam(name);
                } catch (IllegalArgumentException var4) {
                    throw new SprauteRuntimeException("Нельзя создать команду «" + name + "»: " + var4.getMessage());
                }
            }
        }
    }

    private static void told(String event, ServerPlayer p, String team) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("team", team);
        GameEvents.post(event, p, data);
    }

    public static void join(ServerPlayer p, String name) {
        String before = teamOf(p);
        if (!before.equals(name)) {
            PlayerTeam team = team(name);
            board().addPlayerToTeam(p.getScoreboardName(), team);
            if (!before.isEmpty()) {
                told("teamleave", p, before);
            }

            told("teamjoin", p, name);
        }
    }

    public static void leave(ServerPlayer p) {
        String before = teamOf(p);
        if (!before.isEmpty()) {
            board().removePlayerFromTeam(p.getScoreboardName());
            told("teamleave", p, before);
        }
    }

    public static String teamOf(ServerPlayer p) {
        PlayerTeam team = board().getPlayersTeam(p.getScoreboardName());
        return team != null ? team.getName() : "";
    }

    public static List<ServerPlayer> online(String name) {
        List<ServerPlayer> out = new ArrayList<>();

        for (ServerPlayer p : SprauteRuntime.players()) {
            if (name.equals(teamOf(p))) {
                out.add(p);
            }
        }

        return out;
    }

    public static void color(String name, String color) {
        ChatFormatting c = ChatFormatting.getByName(color);
        if (c != null && c.isColor()) {
            team(name).setColor(c);
        } else {
            throw new SprauteRuntimeException("Неизвестный цвет команды «" + color + "» (red, gold, yellow, green, aqua, blue, light_purple, white, gray…)");
        }
    }

    public static void pvp(String name, boolean allowed) {
        team(name).setAllowFriendlyFire(allowed);
    }
}
