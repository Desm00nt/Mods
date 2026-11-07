package org.sprautestudio.spraute_engine.core.system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.minecraft.world.level.saveddata.SavedData;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.runtime.GameEvents;

public final class WorldClock {
    public static final long DAY = 24000L;
    private static final double DEFAULT_SPEED = 0.25;
    private static final WorldClock.Task DEFAULT = new WorldClock.Task("default", Integer.MIN_VALUE, 0.25, null);
    private static final List<WorldClock.Task> TASKS = new ArrayList<>(List.of(DEFAULT));
    private static double carry;

    private WorldClock() {
    }

    private static WorldClock.Saved saved(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(WorldClock.Saved::load, () -> {
            WorldClock.Saved n = new WorldClock.Saved();
            n.time = s.overworld().getDayTime();
            n.day = n.time / 24000L;
            return n;
        }, "spraute_clock");
    }

    public static WorldClock.Task addTask(String id, int priority, double speed, BooleanSupplier until) {
        removeTask(id);
        WorldClock.Task t = new WorldClock.Task(id, priority, speed, until);
        TASKS.add(t);
        return t;
    }

    public static void removeTask(String id) {
        TASKS.removeIf(t -> t != DEFAULT && t.id.equals(id));
    }

    private static WorldClock.Task running() {
        WorldClock.Task best = DEFAULT;

        for (WorldClock.Task t : TASKS) {
            if (!t.stopped && t.priority > best.priority) {
                best = t;
            }
        }

        return best;
    }

    public static void tick(MinecraftServer s) {
        WorldClock.Saved d = saved(s);
        server(s).set(false, s);
        TASKS.removeIf(tx -> tx != DEFAULT && tx.until != null && tx.until.getAsBoolean());
        if (!d.frozen) {
            WorldClock.Task t = running();
            carry = carry + t.speed * t.multiplier * d.factor;
            long whole = (long)carry;
            if (whole != 0L) {
                carry -= whole;
                move(s, d, d.time + whole);
            }
        }

        if (s.getTickCount() % 20 == 0) {
            apply(s, d);
        }
    }

    private static BooleanValue server(MinecraftServer s) {
        return s.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
    }

    public static void release(MinecraftServer s) {
        server(s).set(true, s);
    }

    private static void move(MinecraftServer s, WorldClock.Saved d, long to) {
        d.time = to;
        long night = Math.floorDiv(to + 6000L, 24000L);
        if (night > d.day) {
            d.day = night;
            HashMap<String, Object> data = new HashMap<>();
            data.put("amount", (double)night);
            GameEvents.fire("newday", null, data);
        }

        d.setDirty();
        apply(s, d);
    }

    private static void apply(MinecraftServer s, WorldClock.Saved d) {
        for (ServerLevel l : s.getAllLevels()) {
            l.setDayTime(d.time);
        }

        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTimePacket(Mc.level(p).getGameTime(), d.time, false));
        }
    }

    public static void setTimeOfDay(MinecraftServer s, long timeOfDay) {
        WorldClock.Saved d = saved(s);
        move(s, d, Math.floorDiv(d.time, 24000L) * 24000L + Math.floorMod(timeOfDay, 24000L));
    }

    public static void set(MinecraftServer s, long day, long timeOfDay) {
        WorldClock.Saved d = saved(s);
        d.day = day;
        move(s, d, day * 24000L + Math.floorMod(timeOfDay, 24000L));
    }

    public static void add(MinecraftServer s, long ticks) {
        WorldClock.Saved d = saved(s);
        move(s, d, d.time + ticks);
    }

    public static void setFactor(MinecraftServer s, double factor) {
        WorldClock.Saved d = saved(s);
        d.factor = Math.max(0.0, factor);
        d.setDirty();
    }

    public static void freeze(MinecraftServer s) {
        WorldClock.Saved d = saved(s);
        if (!d.frozen) {
            d.frozen = true;
            d.frozenAt = d.time;
            d.setDirty();
        }
    }

    public static void unfreeze(MinecraftServer s, boolean back) {
        WorldClock.Saved d = saved(s);
        if (d.frozen) {
            d.frozen = false;
            if (back) {
                move(s, d, d.frozenAt);
            }

            d.setDirty();
        }
    }

    public static long day(MinecraftServer s) {
        return saved(s).day;
    }

    public static void unload() {
        TASKS.clear();
        TASKS.add(DEFAULT);
        carry = 0.0;
    }

    private static final class Saved extends SavedData {
        long time;
        long day;
        boolean frozen;
        long frozenAt;
        double factor = 1.0;

        @Override
        public CompoundTag save(CompoundTag t) {
            t.putLong("time", this.time);
            t.putLong("day", this.day);
            t.putBoolean("frozen", this.frozen);
            t.putLong("frozenAt", this.frozenAt);
            t.putDouble("factor", this.factor);
            return t;
        }

        static WorldClock.Saved load(CompoundTag t) {
            WorldClock.Saved s = new WorldClock.Saved();
            s.time = t.getLong("time");
            s.day = t.getLong("day");
            s.frozen = t.getBoolean("frozen");
            s.frozenAt = t.getLong("frozenAt");
            s.factor = t.contains("factor") ? t.getDouble("factor") : 1.0;
            return s;
        }
    }

    public static final class Task {
        final String id;
        final int priority;
        double speed;
        double multiplier = 1.0;
        BooleanSupplier until;
        public boolean stopped;

        Task(String id, int priority, double speed, BooleanSupplier until) {
            this.id = id;
            this.priority = priority;
            this.speed = speed;
            this.until = until;
        }
    }
}
