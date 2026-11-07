package org.sprautestudio.spraute_engine.stealth;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.path.PathDef;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Stealth {
    private static final int GRACE = 40;
    private static final int RESTORE_FLAGS = 18;
    private static final List<Stealth.Guard> GUARDS = new ArrayList<>();
    private static final Map<Integer, Stealth.Attempt> ATTEMPTS = new HashMap<>();

    private Stealth() {
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(Stealth::onTick);
        Hooks.BLOCK_BREAK.add(e -> {
            if (!ATTEMPTS.isEmpty() && e.getPlayer() instanceof ServerPlayer p) {
                saveAround(Mc.serverLevel(p), e.getPos());
            }
        });
        Hooks.RIGHT_CLICK_BLOCK.add(e -> {
            if (!ATTEMPTS.isEmpty() && e.getEntity() instanceof ServerPlayer p) {
                saveAround(Mc.serverLevel(p), e.getPos());
            }
        });
        Hooks.BLOCK_PLACE.add(e -> {
            if (!ATTEMPTS.isEmpty() && e.getEntity() instanceof ServerPlayer p) {
                Stealth.Spot var5 = new Stealth.Spot(Mc.serverLevel(p), e.getPos().immutable());

                for (Stealth.Attempt a : ATTEMPTS.values()) {
                    a.blocks.putIfAbsent(var5, new Stealth.Saved(Blocks.AIR.defaultBlockState(), null));
                }
            }
        });
        Hooks.ENTITY_JOIN.add(e -> {
            if (!ATTEMPTS.isEmpty() && !e.getLevel().isClientSide()) {
                if (e.getEntity() instanceof ItemEntity || e.getEntity() instanceof ExperienceOrb) {
                    for (Stealth.Attempt a : ATTEMPTS.values()) {
                        a.litter.add(e.getEntity());
                    }
                }
            }
        });
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p && !GUARDS.isEmpty()) {
                SprauteNetwork.sendTo(p, packet());
            }
        });
    }

    public static StealthDef style(String project, String name) {
        String json = SprauteRuntime.get().gui(project, "stealth:" + name);
        return json != null ? StealthDef.fromJson(json) : StealthDef.preset(name);
    }

    public static Stealth.Look look(StealthDef d) {
        Stealth.Look l = new Stealth.Look();
        l.shape = d.shape;
        l.range = d.range;
        l.angle = d.angle;
        l.width = d.width;
        l.height = d.height;
        l.sneak = d.sneak;
        l.notice = Math.round(d.notice * 20.0F);
        l.color = d.color & 16777215;
        l.through = d.through;
        return l;
    }

    public static void begin(int script, StealthDef d) {
        if (!ATTEMPTS.containsKey(script)) {
            Location back = d.back.equals("point") ? new Location(Level.OVERWORLD, d.backX, d.backY, d.backZ, d.backYaw, d.backPitch) : null;
            start(script, back, Set.copyOf(d.restore));
        }
    }

    public static void start(int script, Location back, Set<String> restore) {
        ATTEMPTS.put(script, new Stealth.Attempt(back, restore));

        for (ServerPlayer p : players()) {
            ATTEMPTS.get(script).players.put(p.getUUID(), new Stealth.PlayerSave(p));
        }
    }

    public static void guard(int script, Entity e, Stealth.Look look, String pathName, PathDef path, double speed, String mode) {
        if (!ATTEMPTS.containsKey(script)) {
            start(script, null, StealthShape.restores(""));
        }

        GUARDS.removeIf(gx -> gx.e == e);
        look.id = e.getId();
        Stealth.Guard g = new Stealth.Guard(script, e, look, pathName, path, speed, mode);
        GUARDS.add(g);
        g.patrol();
        sync();
    }

    public static void remove(Entity e) {
        for (Stealth.Guard g : new ArrayList<>(GUARDS)) {
            if (g.e == e) {
                drop(g);
            }
        }

        sync();
    }

    public static void stop(int script) {
        ATTEMPTS.remove(script);

        for (Stealth.Guard g : new ArrayList<>(GUARDS)) {
            if (g.script == script) {
                drop(g);
            }
        }

        sync();
    }

    public static void storyStopped(int script) {
        if (ATTEMPTS.containsKey(script)) {
            stop(script);
        }
    }

    public static void unload() {
        GUARDS.clear();
        ATTEMPTS.clear();
    }

    private static void drop(Stealth.Guard g) {
        GUARDS.remove(g);
        if (g.path != null && !g.e.isRemoved()) {
            Walkers.stop(g.e);
        }
    }

    private static List<ServerPlayer> players() {
        MinecraftServer server = Platform.get().server();
        return server == null ? List.of() : server.getPlayerList().getPlayers();
    }

    private static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END && !GUARDS.isEmpty()) {
            boolean changed = false;

            for (Stealth.Attempt a : ATTEMPTS.values()) {
                if (a.grace > 0) {
                    a.grace--;
                }

                for (ServerPlayer p : players()) {
                    a.players.computeIfAbsent(p.getUUID(), id -> new Stealth.PlayerSave(p));
                }
            }

            for (Stealth.Guard g : new ArrayList<>(GUARDS)) {
                if (!g.e.isRemoved() && g.e.isAlive()) {
                    Stealth.Attempt a = ATTEMPTS.get(g.script);
                    if (a != null && a.grace <= 0) {
                        float most = 0.0F;

                        for (ServerPlayer p : players()) {
                            if (Mc.level(p) == Mc.level(g.e) && !p.isSpectator() && p.isAlive()) {
                                int seen = g.seen.getOrDefault(p.getUUID(), 0);
                                seen = sees(g, p) ? seen + 1 : Math.max(0, seen - 1);
                                g.seen.put(p.getUUID(), seen);
                                if (seen > g.look.notice) {
                                    caught(g, a, p);
                                    return;
                                }

                                if (g.look.notice > 0) {
                                    most = Math.max(most, (float)seen / g.look.notice);
                                }
                            }
                        }

                        if (Math.abs(most - g.look.alert) >= 0.1F || most == 0.0F && g.look.alert != 0.0F) {
                            g.look.alert = most;
                            changed = true;
                        }
                    }
                }
            }

            if (changed) {
                sync();
            }
        }
    }

    private static boolean sees(Stealth.Guard g, ServerPlayer p) {
        Stealth.Look l = g.look;
        double dy = p.getY() - g.e.getY();
        if (!(dy < -1.5) && !(dy >= l.height)) {
            double k = p.isShiftKeyDown() ? l.sneak : 1.0;
            return !StealthShape.contains(l.shape, p.getX() - g.e.getX(), p.getZ() - g.e.getZ(), g.e.getYRot(), l.range * k, l.angle, l.width * k)
                ? false
                : l.through || inSight(g.e, p.getEyePosition()) || inSight(g.e, p.position().add(0.0, 0.4, 0.0));
        } else {
            return false;
        }
    }

    private static boolean inSight(Entity from, Vec3 to) {
        return Mc.level(from).clip(new ClipContext(from.getEyePosition(), to, Block.COLLIDER, Fluid.NONE, from)).getType() == Type.MISS;
    }

    private static void caught(Stealth.Guard by, Stealth.Attempt a, ServerPlayer p) {
        a.grace = 40;
        p.closeContainer();
        if (a.restore.contains("blocks") || a.restore.contains("loot")) {
            restoreBlocks(a);
        }

        if (a.restore.contains("blocks") || a.restore.contains("loot") || a.restore.contains("inventory")) {
            for (Entity e : a.litter) {
                if (!e.isRemoved()) {
                    e.discard();
                }
            }

            a.litter.clear();
        }

        Stealth.PlayerSave s = a.players.get(p.getUUID());
        if (s != null) {
            restorePlayer(a, s, p);
        }

        ServerLevel level = a.back == null && s != null ? s.level : Mc.serverLevel(p);
        Vec3 to = (Vec3)(a.back != null ? a.back : (s != null ? s.pos : p.position()));
        float yaw = a.back != null ? a.back.yaw : (s != null ? s.yaw : p.getYRot());
        float pitch = a.back != null ? a.back.pitch : (s != null ? s.pitch : p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0.0F;
        p.clearFire();
        Mc.teleportTo(p, level, to.x, to.y, to.z, yaw, pitch);

        for (Stealth.Guard g : GUARDS) {
            if (g.script == by.script && !g.e.isRemoved()) {
                g.seen.clear();
                g.look.alert = 0.0F;
                Walkers.stop(g.e);
                g.e.teleportTo(g.home.x, g.home.y, g.home.z);
                g.e.setYRot(g.homeYaw);
                g.e.setYHeadRot(g.homeYaw);
                g.patrol();
            }
        }

        sync();
        GameEvents.stealthCaught(p, by.e);
    }

    private static void restorePlayer(Stealth.Attempt a, Stealth.PlayerSave s, ServerPlayer p) {
        if (a.restore.contains("inventory")) {
            p.getInventory().load(s.inventory);
            p.inventoryMenu.broadcastChanges();
        }

        if (a.restore.contains("coins")) {
            Coins.set(p, s.coins);
        }

        if (a.restore.contains("health")) {
            p.setHealth(s.health);
            p.getFoodData().setFoodLevel(s.food);
            p.getFoodData().setSaturation(s.saturation);
        }

        if (a.restore.contains("xp")) {
            p.experienceProgress = s.xpProgress;
            p.totalExperience = s.xpTotal;
            p.setExperienceLevels(s.xpLevel);
        }
    }

    private static void restoreBlocks(Stealth.Attempt a) {
        Iterator<Entry<Stealth.Spot, Stealth.Saved>> it = a.blocks.entrySet().iterator();

        while (it.hasNext()) {
            Entry<Stealth.Spot, Stealth.Saved> en = it.next();
            ServerLevel level = en.getKey().level();
            BlockPos pos = en.getKey().pos();
            Stealth.Saved was = en.getValue();
            BlockState now = level.getBlockState(pos);
            boolean sameBlock = now.equals(was.state());
            if (sameBlock ? a.restore.contains("loot") : a.restore.contains("blocks")) {
                if (!sameBlock) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos, was.state(), 18);
                }

                if (was.nbt() != null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != null) {
                        be.load(was.nbt());
                        be.setChanged();
                        level.sendBlockUpdated(pos, was.state(), was.state(), 18);
                    }
                }

                it.remove();
            }
        }
    }

    private static void saveAround(ServerLevel level, BlockPos pos) {
        save(level, pos);
        save(level, pos.above(2));

        for (Direction d : Direction.values()) {
            save(level, pos.relative(d));
            if (d.getAxis().isHorizontal()) {
                save(level, pos.relative(d).above());
            }
        }
    }

    private static void save(ServerLevel level, BlockPos pos) {
        Stealth.Spot spot = new Stealth.Spot(level, pos.immutable());
        Stealth.Saved saved = null;

        for (Stealth.Attempt a : ATTEMPTS.values()) {
            if (!a.blocks.containsKey(spot)) {
                if (saved == null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    saved = new Stealth.Saved(level.getBlockState(pos), be != null ? be.saveWithFullMetadata() : null);
                }

                a.blocks.put(spot, saved);
            }
        }
    }

    private static Packets.StealthSync packet() {
        List<Stealth.Look> looks = new ArrayList<>();

        for (Stealth.Guard g : GUARDS) {
            looks.add(g.look);
        }

        return new Packets.StealthSync(new Gson().toJson(looks));
    }

    private static void sync() {
        SprauteNetwork.sendToAll(packet());
    }

    private static final class Attempt {
        final Location back;
        final Set<String> restore;
        final Map<UUID, Stealth.PlayerSave> players = new HashMap<>();
        final Map<Stealth.Spot, Stealth.Saved> blocks = new LinkedHashMap<>();
        final List<Entity> litter = new ArrayList<>();
        int grace;

        Attempt(Location back, Set<String> restore) {
            this.back = back;
            this.restore = restore;
        }
    }

    private static final class Guard {
        final int script;
        final Entity e;
        final Stealth.Look look;
        final Vec3 home;
        final float homeYaw;
        final String pathName;
        final PathDef path;
        final double speed;
        final String mode;
        final Map<UUID, Integer> seen = new HashMap<>();

        Guard(int script, Entity e, Stealth.Look look, String pathName, PathDef path, double speed, String mode) {
            this.script = script;
            this.e = e;
            this.look = look;
            this.home = e.position();
            this.homeYaw = e.getYRot();
            this.pathName = pathName;
            this.path = path;
            this.speed = speed;
            this.mode = mode;
        }

        void patrol() {
            if (this.path != null) {
                Walkers.start(this.e, this.pathName, this.path, this.speed, this.mode, 0);
            }
        }
    }

    public static final class Look {
        public int id;
        public String shape = "fan";
        public double range = 8.0;
        public double angle = 70.0;
        public double width = 4.0;
        public double height = 2.5;
        public double sneak = 0.5;
        public int notice;
        public int color = 16724016;
        public boolean through;
        public float alert;
    }

    private static final class PlayerSave {
        final ServerLevel level;
        final Vec3 pos;
        final float yaw;
        final float pitch;
        final float health;
        final float saturation;
        final float xpProgress;
        final ListTag inventory;
        final int coins;
        final int food;
        final int xpLevel;
        final int xpTotal;

        PlayerSave(ServerPlayer p) {
            this.level = Mc.serverLevel(p);
            this.pos = p.position();
            this.yaw = p.getYRot();
            this.pitch = p.getXRot();
            this.health = p.getHealth();
            this.saturation = p.getFoodData().getSaturationLevel();
            this.xpProgress = p.experienceProgress;
            this.inventory = p.getInventory().save(new ListTag());
            this.coins = Coins.get(p);
            this.food = p.getFoodData().getFoodLevel();
            this.xpLevel = p.experienceLevel;
            this.xpTotal = p.totalExperience;
        }
    }

    private record Saved(BlockState state, CompoundTag nbt) {
    }

    private record Spot(ServerLevel level, BlockPos pos) {
    }
}
