package org.sprautestudio.spraute_engine.core.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.entity.NpcChunks;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Characters {
    private static final String TAG = "SprauteCharacter";
    private static final Map<String, Characters.Entry> ENTRIES = new HashMap<>();
    private static final Map<String, Characters.Entry> FREED = new HashMap<>();
    private static final String FREED_KEY = "$freed";
    private static long orders;

    private Characters() {
    }

    private static Characters.Entry entry(Character<?> c) {
        Characters.Entry e = ENTRIES.computeIfAbsent(c.getId(), Characters.Entry::new);
        e.character = c;
        return e;
    }

    private static Characters.Claim claim(ScriptInstance s, Character<?> c) {
        Characters.Entry e = entry(c);

        for (Characters.Claim cl : e.claims) {
            if (cl.script == s.getId()) {
                return cl;
            }
        }

        Characters.Claim clx = new Characters.Claim(s.getId(), s.priorityOf(c), orders++);
        e.claims.add(clx);
        return clx;
    }

    private static Entity bodyOf(Characters.Entry e) {
        MinecraftServer s = Platform.get().server();
        if (e.body != null && s != null) {
            ServerLevel usual = e.last != null ? s.getLevel(e.last.dimension) : null;
            if (usual != null) {
                Entity b = usual.getEntity(e.body);
                if (b != null) {
                    return b.isAlive() ? b : null;
                }
            }

            for (ServerLevel l : s.getAllLevels()) {
                if (l != usual) {
                    Entity b = l.getEntity(e.body);
                    if (b != null) {
                        return b.isAlive() ? b : null;
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private static void wake(Characters.Entry e) {
        MinecraftServer s = Platform.get().server();
        if (s != null && e.last != null && !e.dead) {
            ServerLevel level = s.getLevel(e.last.dimension);
            if (level != null) {
                NpcChunks.wake(level, new ChunkPos(Mc.blockPos(e.last)));
            }
        }
    }

    public static <T extends Entity> Optional<T> find(Character<T> c) {
        if (c == null) {
            return Optional.empty();
        } else {
            Characters.Entry e = ENTRIES.get(c.getId());
            Entity body = e != null ? bodyOf(e) : null;
            if (body == null) {
                Characters.Entry freed = FREED.get(c.getId());
                if (freed != null) {
                    body = bodyOf(freed);
                }
            }

            return Optional.ofNullable((T)body);
        }
    }

    public static boolean controls(ScriptInstance s, Character<?> c) {
        Characters.Entry e = ENTRIES.get(c.getId());
        return e != null && e.active != null && e.active.script == s.getId();
    }

    public static void act(ScriptInstance s, Character<?> c, Consumer<Entity> action) {
        if (controls(s, c) && !s.replaying()) {
            Optional<? extends Entity> body = find((Character<? extends Entity>)c);
            if (body.isPresent()) {
                action.accept(body.get());
            } else {
                wake(ENTRIES.get(c.getId()));
            }
        }
    }

    public static void state(ScriptInstance s, Character<?> c, String key, Consumer<Entity> action) {
        Characters.Claim cl = claim(s, c);
        cl.state.remove(key);
        cl.state.put(key, action);
        Characters.Entry e = entry(c);
        update(e);
        if (controls(s, c)) {
            Optional<? extends Entity> body = find((Character<? extends Entity>)c);
            if (body.isPresent()) {
                action.accept(body.get());
            } else {
                e.needsApply = true;
                wake(e);
            }
        }
    }

    public static void forget(ScriptInstance s, Character<?> c, String keyPrefix) {
        Characters.Entry e = ENTRIES.get(c.getId());
        s.forgetNotes(c.getId() + "|" + keyPrefix);
        if (e != null) {
            for (Characters.Claim cl : e.claims) {
                if (cl.script == s.getId()) {
                    cl.state.keySet().removeIf(k -> k.startsWith(keyPrefix));
                }
            }
        }
    }

    public static void place(ScriptInstance s, Character<?> c, Location at) {
        if (at != null) {
            Characters.Claim cl = claim(s, c);
            cl.at = at;
            Characters.Entry e = entry(c);
            e.dead = false;
            update(e);
            if (e.active == cl) {
                put(e, at, cl);
            }
        }
    }

    private static void put(Characters.Entry e, Location at, Characters.Claim cl) {
        MinecraftServer s = Platform.get().server();
        if (s != null) {
            ServerLevel level = s.getLevel(at.dimension);
            if (level == null) {
                level = s.overworld();
            }

            NpcChunks.load(level, new Vec3(at.x, at.y, at.z));
            Entity old = bodyOf(e);
            if (old != null) {
                if (Mc.level(old) == level) {
                    old.teleportTo(at.x, at.y, at.z);
                    face(old, at.yaw);
                } else {
                    Mc.teleportTo(old, level, at.x, at.y, at.z, at.yaw, at.pitch);
                }

                e.last = at;
            } else {
                spawn(e, level, at, cl);
            }
        }
    }

    private static void face(Entity e, float yaw) {
        e.setYRot(yaw);
        e.yRotO = yaw;
        e.setYHeadRot(yaw);
        if (e instanceof LivingEntity le) {
            le.yBodyRot = yaw;
            le.yBodyRotO = yaw;
            le.yHeadRotO = yaw;
        }
    }

    private static void spawn(Characters.Entry e, ServerLevel level, Location at, Characters.Claim cl) {
        Character c = e.character;
        if (c != null) {
            Entity b = Mc.createEntity(c.getType(), level, Mc.blockPos(at));
            if (b == null) {
                throw new IllegalStateException("Сущность " + c.getId() + " не создаётся");
            } else {
                b.moveTo(at.x, at.y, at.z, at.yaw, at.pitch);
                face(b, at.yaw);
                b.setCustomName(Component.literal(c.getName()));
                b.setCustomNameVisible(false);
                Platform.get().persistentData(b).putString("SprauteCharacter", e.id);
                if (cl != null) {
                    SprauteRuntime.env().ifPresent(env -> SprauteRuntime.markOwned(b, env.scriptInstances.get(cl.script)));
                }

                if (b instanceof AnimatedEntity ae && c.texture() != null) {
                    ae.setTextureOverride(c.texture());
                }

                if (b instanceof LivingEntity le && c.speed() != null) {
                    AttributeInstance a = le.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (a != null) {
                        a.setBaseValue(0.25 * c.speed());
                    }
                }

                e.body = b.getUUID();
                e.last = at;
                FREED.remove(e.id);
                level.addFreshEntity(b);
                e.needsApply = false;
                if (cl != null) {
                    for (Consumer<Entity> part : new ArrayList<>(cl.state.values())) {
                        part.accept(b);
                    }
                }

                GameEvents.fire("charspawn", null, eventData(e, b));
            }
        }
    }

    private static Map<String, Object> eventData(Characters.Entry e, Entity b) {
        Map<String, Object> d = new HashMap<>();
        d.put("id", e.id);
        d.put("name", b.getName().getString());
        d.put("__entity", b);
        d.put("x", Math.floor(b.getX()));
        d.put("y", Math.floor(b.getY()));
        d.put("z", Math.floor(b.getZ()));
        return d;
    }

    private static void update(Characters.Entry e) {
        Characters.Claim best = null;

        for (Characters.Claim c : e.claims) {
            if (best == null || c.priority > best.priority || c.priority == best.priority && c.order < best.order) {
                best = c;
            }
        }

        if (best != e.active) {
            Entity b = bodyOf(e);
            if (e.active != null && b != null && !e.claims.isEmpty()) {
                quiet(b);
            }

            e.active = best;
            if (best == null) {
                if (b != null) {
                    GameEvents.fire("charremove", null, eventData(e, b));
                    b.discard();
                }

                ENTRIES.remove(e.id);
            } else {
                if (best.at != null) {
                    put(e, best.at, best);
                }

                Entity now = bodyOf(e);
                if (now != null) {
                    for (Consumer<Entity> part : new ArrayList<>(best.state.values())) {
                        part.accept(now);
                    }
                }
            }
        }
    }

    private static void quiet(Entity b) {
        Behaviours.clear(b);
        if (b instanceof AnimatedEntity ae) {
            ae.setLookTarget(null, false);

            for (String l : ae.getAnimationSystem().getLayerNames()) {
                if (!l.equals("movement")) {
                    ae.getAnimationSystem().stopAnimation(l, 0.2F);
                }
            }

            ae.clearAnimationOverrides();
        }
    }

    public static void release(int script) {
        for (Characters.Entry e : new ArrayList<>(ENTRIES.values())) {
            if (e.claims.removeIf(c -> c.script == script)) {
                update(e);
            }
        }
    }

    public static void remove(ScriptInstance s, Character<?> c) {
        Characters.Entry e = ENTRIES.get(c.getId());
        s.forgetNotes(c.getId() + "|");
        if (e != null && e.claims.removeIf(cl -> cl.script == s.getId())) {
            update(e);
        }
    }

    public static void free(Character<?> c) {
        Characters.Entry e = ENTRIES.remove(c.getId());
        Entity b = e != null ? bodyOf(e) : null;
        if (b != null) {
            Platform.get().persistentData(b).remove("SprauteCharacter");
            Behaviours.clear(b);
            FREED.put(c.getId(), e);
        }
    }

    public static void rollback(int script) {
        for (Characters.Entry e : ENTRIES.values()) {
            if (e.active != null && e.active.script == script) {
                Entity b = bodyOf(e);
                if (b != null) {
                    Behaviours.clear(b);
                    if (b instanceof AnimatedEntity ae) {
                        ae.setLookTarget(null, false);
                    }

                    e.active.state.remove("look");
                }
            }
        }
    }

    static boolean belongs(Entity b) {
        String id = Platform.get().persistentData(b).getString("SprauteCharacter");
        if (id.isEmpty()) {
            return true;
        } else {
            Characters.Entry e = ENTRIES.get(id);
            return e != null && b.getUUID().equals(e.body);
        }
    }

    static void died(Entity b) {
        if (!(b instanceof AnimatedEntity ae && !ae.canDie)) {
            for (Characters.Entry e : ENTRIES.values()) {
                if (b.getUUID().equals(e.body)) {
                    e.dead = true;
                }
            }
        }
    }

    static void tick(MinecraftServer s) {
        for (Characters.Entry e : new ArrayList<>(ENTRIES.values())) {
            if (e.active != null) {
                Entity b = bodyOf(e);
                if (b != null) {
                    e.last = new Location(b);
                    if (e.needsApply) {
                        e.needsApply = false;

                        for (Consumer<Entity> part : new ArrayList<>(e.active.state.values())) {
                            part.accept(b);
                        }
                    }
                } else if (!e.dead && e.last != null && s.getTickCount() % 20 == 0) {
                    ServerLevel level = s.getLevel(e.last.dimension);
                    if (level != null && level.hasChunkAt(Mc.blockPos(e.last)) && level.areEntitiesLoaded(ChunkPos.asLong(Mc.blockPos(e.last)))) {
                        spawn(e, level, e.last, e.active);
                    }
                }
            }
        }
    }

    static CompoundTag save() {
        CompoundTag root = new CompoundTag();
        CompoundTag freed = new CompoundTag();
        FREED.forEach((id, e) -> {
            if (e.body != null) {
                freed.putUUID(id, e.body);
            }
        });
        root.put("$freed", freed);
        ENTRIES.forEach((id, e) -> {
            CompoundTag t = new CompoundTag();
            if (e.body != null) {
                t.putUUID("body", e.body);
            }

            t.putBoolean("dead", e.dead);
            if (e.last != null) {
                t.putString("dim", e.last.dimension.location().toString());
                t.putDouble("x", e.last.x);
                t.putDouble("y", e.last.y);
                t.putDouble("z", e.last.z);
                t.putFloat("yaw", e.last.yaw);
            }

            ListTag claims = new ListTag();

            for (Characters.Claim c : e.claims) {
                CompoundTag ct = new CompoundTag();
                ct.putInt("script", c.script);
                ct.putInt("priority", c.priority);
                ct.putLong("order", c.order);
                claims.add(ct);
            }

            t.put("claims", claims);
            if (e.active != null) {
                t.putInt("active", e.active.script);
            }

            root.put(id, t);
        });
        return root;
    }

    static void load(CompoundTag root) {
        ENTRIES.clear();
        FREED.clear();
        CompoundTag freed = root.getCompound("$freed");

        for (String id : freed.getAllKeys()) {
            Characters.Entry e = new Characters.Entry(id);
            e.body = freed.getUUID(id);
            FREED.put(id, e);
        }

        for (String id : root.getAllKeys()) {
            if (!id.equals("$freed")) {
                CompoundTag t = root.getCompound(id);
                Characters.Entry e = new Characters.Entry(id);
                if (t.hasUUID("body")) {
                    e.body = t.getUUID("body");
                }

                e.dead = t.getBoolean("dead");
                if (t.contains("dim")) {
                    ResourceKey<Level> dim = Mc.dimension(new ResourceLocation(t.getString("dim")));
                    e.last = new Location(dim, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), 0.0F);
                }

                ListTag claims = t.getList("claims", 10);

                for (int i = 0; i < claims.size(); i++) {
                    CompoundTag ct = claims.getCompound(i);
                    Characters.Claim c = new Characters.Claim(ct.getInt("script"), ct.getInt("priority"), ct.getLong("order"));
                    orders = Math.max(orders, c.order + 1L);
                    e.claims.add(c);
                    if (t.contains("active") && t.getInt("active") == c.script) {
                        e.active = c;
                    }
                }

                ENTRIES.put(id, e);
            }
        }
    }

    static void afterRestore(ScriptEnvironment env) {
        for (Characters.Entry e : new ArrayList<>(ENTRIES.values())) {
            e.character = e.character != null ? e.character : Character.getById(e.id);
            if (e.claims.removeIf(c -> !env.scriptInstances.containsKey(c.script))) {
                update(e);
            }
        }
    }

    static void clear() {
        ENTRIES.clear();
        FREED.clear();
    }

    private static final class Claim {
        final int script;
        final int priority;
        final long order;
        Location at;
        final Map<String, Consumer<Entity>> state = new LinkedHashMap<>();

        Claim(int script, int priority, long order) {
            this.script = script;
            this.priority = priority;
            this.order = order;
        }
    }

    private static final class Entry {
        final String id;
        Character<?> character;
        UUID body;
        Location last;
        boolean dead;
        boolean needsApply;
        final List<Characters.Claim> claims = new ArrayList<>();
        Characters.Claim active;

        Entry(String id) {
            this.id = id;
        }
    }
}
