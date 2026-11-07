package org.sprautestudio.spraute_engine.hitbox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class Hitboxes {
    public static final int SOLID = 1;
    public static final int FIXED = 2;
    public static final int GHOST = 4;
    public static final int NOPICK = 8;
    public static final int DAMAGE = 16;
    public static final int NODAMAGE = 32;
    public static final int MORTAL = 64;
    private static final String TAG = "SprauteHitbox";
    private static final Map<Integer, Integer> SERVER = new ConcurrentHashMap<>();
    public static final Map<Integer, Integer> CLIENT = new ConcurrentHashMap<>();
    private static final List<Hitboxes.Pending> PENDING = new ArrayList<>();

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(Hitboxes::onTick);
        Hooks.ENTITY_JOIN.add(Hitboxes::onJoin);
        Hooks.ENTITY_LEAVE.add(Hitboxes::onLeave);
        Hooks.START_TRACKING.add(Hitboxes::onTrack);
        Hooks.LIVING_ATTACK.add(Priority.HIGH, Hitboxes::onAttack);
    }

    private Hitboxes() {
    }

    public static int flags(Entity e) {
        Map<Integer, Integer> m = Mc.level(e) != null && Mc.level(e).isClientSide ? CLIENT : SERVER;
        if (m.isEmpty()) {
            return 0;
        } else {
            Integer f = m.get(e.getId());
            return f == null ? 0 : f;
        }
    }

    public static int parse(int flags, List<String> words) {
        return HitboxFlags.parse(flags, words);
    }

    public static String describe(int f) {
        return HitboxFlags.describe(f);
    }

    public static void set(Entity e, int flags) {
        if (flags == 0) {
            SERVER.remove(e.getId());
        } else {
            SERVER.put(e.getId(), flags);
        }

        Platform.get().persistentData(e).putInt("SprauteHitbox", flags);
        applyDamage(e, flags);
        SprauteNetwork.sendToAll(new Packets.HitboxSync(e.getId(), flags));
    }

    private static void applyDamage(Entity e, int flags) {
        if (e instanceof AnimatedEntity ae) {
            if ((flags & 16) != 0) {
                ae.setVulnerable(true);
            } else if ((flags & 32) != 0) {
                ae.setVulnerable(false);
            }

            if ((flags & 64) != 0) {
                ae.canDie = true;
            } else if ((flags & 48) != 0) {
                ae.canDie = false;
            }
        }
    }

    public static void applyWhenReady(Supplier<Entity> entity, List<String> words) {
        PENDING.add(new Hitboxes.Pending(entity, words, new int[]{100}));
    }

    public static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END && !PENDING.isEmpty()) {
            Iterator<Hitboxes.Pending> it = PENDING.iterator();

            while (it.hasNext()) {
                Hitboxes.Pending p = it.next();

                Entity e;
                try {
                    e = p.entity().get();
                } catch (RuntimeException var6) {
                    e = null;
                }

                if (e != null) {
                    try {
                        set(e, parse(flags(e), p.words()));
                    } catch (IllegalArgumentException var5) {
                    }

                    it.remove();
                } else if (--p.ttl()[0] <= 0) {
                    it.remove();
                }
            }
        }
    }

    public static void onJoin(Hooks.EntityJoin ev) {
        if (!ev.getLevel().isClientSide) {
            Entity e = ev.getEntity();
            if (Platform.get().persistentData(e).contains("SprauteHitbox")) {
                int f = Platform.get().persistentData(e).getInt("SprauteHitbox");
                if (f != 0) {
                    SERVER.put(e.getId(), f);
                }
            }
        }
    }

    public static void onLeave(Hooks.EntityLeave ev) {
        if (ev.getLevel().isClientSide) {
            CLIENT.remove(ev.getEntity().getId());
        } else {
            SERVER.remove(ev.getEntity().getId());
        }
    }

    public static void onTrack(Hooks.StartTracking ev) {
        if (ev.getEntity() instanceof ServerPlayer p) {
            Integer f = SERVER.get(ev.getTarget().getId());
            if (f != null && f != 0) {
                SprauteNetwork.sendTo(p, new Packets.HitboxSync(ev.getTarget().getId(), f));
            }
        }
    }

    public static void onAttack(Hooks.LivingAttack ev) {
        if (!Mc.level(ev.getEntity()).isClientSide && !SERVER.isEmpty()) {
            if ((flags(ev.getEntity()) & 32) != 0 && !Mc.bypassesInvulnerability(ev.getSource())) {
                ev.setCanceled(true);
            }
        }
    }

    public static void clearServer() {
        SERVER.clear();
        PENDING.clear();
    }

    private record Pending(Supplier<Entity> entity, List<String> words, int[] ttl) {
    }
}
