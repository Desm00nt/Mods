package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.models.BodyPart;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.PartShape;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

public final class AttackBoxes {
    private static final List<AttackBoxes.Attack> ATTACKS = new ArrayList<>();
    private static final Map<UUID, Integer> LAST_HITS = new HashMap<>();
    private static final Map<String, Integer> CONTACT_READY = new HashMap<>();

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(AttackBoxes::onTick);
    }

    private AttackBoxes() {
    }

    private static int now() {
        MinecraftServer s = Platform.get().server();
        return s != null ? s.getTickCount() : 0;
    }

    public static void attack(
        Entity who,
        String part,
        Vector3f size,
        Vector3f offset,
        float damage,
        double push,
        int delay,
        int ticks,
        Entity except,
        BossMoves.Fx impact,
        boolean show,
        int scriptId
    ) {
        if (part != null && !(who instanceof SprauteModelEntity me && me.partNames().contains(part))) {
            throw new SprauteRuntimeException(
                "attack: у "
                    + who.getName().getString()
                    + " нет части «"
                    + part
                    + "»"
                    + (who instanceof SprauteModelEntity mex ? ". Есть: " + String.join(", ", mex.partNames()) : " (части есть только у сущностей spraute:…)")
            );
        } else {
            int s = now() + Math.max(0, delay);
            ATTACKS.add(new AttackBoxes.Attack(who, part, size, offset, damage, push, s, s + Math.max(1, ticks), except, impact, show, scriptId));
            LAST_HITS.put(who.getUUID(), 0);
        }
    }

    public static boolean active(Entity e) {
        if (e == null) {
            return false;
        } else {
            for (AttackBoxes.Attack a : ATTACKS) {
                if (a.who.equals(e.getUUID())) {
                    return true;
                }
            }

            return false;
        }
    }

    public static int hits(Entity e) {
        return e == null ? 0 : LAST_HITS.getOrDefault(e.getUUID(), 0);
    }

    public static void stop(Entity e) {
        ATTACKS.removeIf(a -> a.who.equals(e.getUUID()));
    }

    public static void stopScript(int scriptId) {
        ATTACKS.removeIf(a -> a.scriptId == scriptId);
    }

    private static PartShape shape(Entity e, AttackBoxes.Attack a) {
        float yaw = e instanceof LivingEntity le ? le.yBodyRot : e.getYRot();
        if (a.part != null && e instanceof SprauteModelEntity me) {
            List<EntityLibrary.Def.Part> defs = me.partList();

            for (int i = 0; i < defs.size(); i++) {
                if (defs.get(i).name.equals(a.part)) {
                    PartShape sh = me.shape(i);
                    return sh != null ? sh : PartShape.of(defs.get(i), 1.0F, yaw, me.freshPose(defs.get(i).bone));
                }
            }

            return null;
        } else {
            Quaternionf rot = new Quaternionf().rotationY((float)(-Math.toRadians(yaw)));
            Vector3f center = rot.transform(new Vector3f(-a.offset.x, a.offset.y, a.offset.z));
            return new PartShape(center, rot, new Vector3f(a.size).mul(0.5F));
        }
    }

    public static boolean touches(PartShape sh, Vec3 origin, AABB box) {
        AABB around = sh.bounds(origin.x, origin.y, origin.z);
        if (!around.intersects(box)) {
            return false;
        } else {
            Quaternionf inv = new Quaternionf(sh.rot()).conjugate();
            Vec3 c = box.getCenter().subtract(origin);
            Vector3f local = inv.transform(new Vector3f((float)c.x, (float)c.y, (float)c.z).sub(sh.center()));
            float hx = (float)box.getXsize() / 2.0F;
            float hy = (float)box.getYsize() / 2.0F;
            float hz = (float)box.getZsize() / 2.0F;
            Vector3f[] axes = new Vector3f[]{
                inv.transform(new Vector3f(1.0F, 0.0F, 0.0F)), inv.transform(new Vector3f(0.0F, 1.0F, 0.0F)), inv.transform(new Vector3f(0.0F, 0.0F, 1.0F))
            };
            float[] l = new float[]{local.x, local.y, local.z};
            float[] h = new float[]{sh.half().x, sh.half().y, sh.half().z};

            for (int k = 0; k < 3; k++) {
                float ext = Math.abs(axes[0].get(k)) * hx + Math.abs(axes[1].get(k)) * hy + Math.abs(axes[2].get(k)) * hz;
                if (Math.abs(l[k]) > h[k] + ext) {
                    return false;
                }
            }

            return true;
        }
    }

    private static boolean own(Entity body, Entity other) {
        return other == body || other instanceof BodyPart p && p.body() == body;
    }

    private static void hurt(ServerLevel lv, Entity by, Entity victim, float damage, double push) {
        if (damage > 0.0F) {
            DamageSource src = by instanceof LivingEntity le ? Mc.mobAttack(lv, le) : Mc.generic(lv);
            victim.hurt(src, damage);
        }

        if (push > 0.0) {
            Vec3 d = victim.position().subtract(by.position());
            d = new Vec3(d.x, 0.0, d.z);
            d = d.lengthSqr() < 1.0E-6 ? by.getLookAngle() : d.normalize();
            victim.push(d.x * push, 0.3 * push, d.z * push);
            victim.hurtMarked = true;
        }
    }

    private static void outline(ServerLevel lv, PartShape sh, Vec3 origin) {
        Vector3f[] c = sh.corners();
        int[][] edges = new int[][]{{0, 1}, {2, 3}, {0, 2}, {1, 3}, {4, 5}, {6, 7}, {4, 6}, {5, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
        DustParticleOptions red = Mc.dust(1.0F, 0.15F, 0.1F, 0.7F);

        for (int[] e : edges) {
            for (int i = 0; i <= 4; i++) {
                Vector3f p = new Vector3f(c[e[0]]).lerp(c[e[1]], i / 4.0F);
                lv.sendParticles(red, origin.x + p.x, origin.y + p.y, origin.z + p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    private static Entity find(UUID id) {
        MinecraftServer s = Platform.get().server();
        if (s == null) {
            return null;
        } else {
            for (ServerLevel lv : s.getAllLevels()) {
                Entity e = lv.getEntity(id);
                if (e != null) {
                    return e;
                }
            }

            return null;
        }
    }

    public static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END && !ATTACKS.isEmpty()) {
            int n = now();
            Iterator<AttackBoxes.Attack> it = ATTACKS.iterator();

            while (it.hasNext()) {
                AttackBoxes.Attack a = it.next();
                if (n >= a.start) {
                    Entity e = find(a.who);
                    if (e != null && e.isAlive() && n < a.end && Mc.level(e) instanceof ServerLevel lv) {
                        PartShape sh = shape(e, a);
                        if (sh != null) {
                            Vec3 o = e.position();
                            if (a.show) {
                                outline(lv, sh, o);
                            }

                            for (Entity v : lv.getEntities(e, sh.bounds(o.x, o.y, o.z).inflate(0.1), x -> x instanceof LivingEntity && x.isAlive())) {
                                if (!own(e, v) && !v.getUUID().equals(a.except) && !a.hit.contains(v.getUUID()) && touches(sh, o, v.getBoundingBox())) {
                                    a.hit.add(v.getUUID());
                                    LAST_HITS.merge(a.who, 1, Integer::sum);
                                    hurt(lv, e, v, a.damage, a.push);
                                    if (a.impact != null && a.impact.json() != null) {
                                        Vec3 at = v.getBoundingBox().getCenter();
                                        SprauteNetwork.sendToAudience(
                                            a.impact.scriptId(),
                                            new Packets.FxPlay(
                                                "", a.impact.project(), a.impact.json(), at.x, at.y, at.z, -1, a.impact.scale(), a.impact.scriptId()
                                            )
                                        );
                                    }
                                }
                            }
                        }
                    } else {
                        it.remove();
                    }
                }
            }
        }
    }

    public static void contact(SprauteModelEntity me) {
        if (Mc.level(me) instanceof ServerLevel lv && me.isAlive()) {
            List<EntityLibrary.Def.Part> defs = me.partList();
            int n = now();
            Vec3 o = me.position();

            for (int i = 0; i < defs.size(); i++) {
                float dmg = me.partAttack(defs.get(i).name);
                PartShape sh = me.shape(i);
                if (!(dmg <= 0.0F) && sh != null) {
                    for (Entity v : lv.getEntities(me, sh.bounds(o.x, o.y, o.z).inflate(0.1), x -> x instanceof LivingEntity && x.isAlive())) {
                        if (!own(me, v) && touches(sh, o, v.getBoundingBox())) {
                            String key = me.getUUID() + "/" + v.getUUID();
                            if (CONTACT_READY.getOrDefault(key, 0) <= n) {
                                CONTACT_READY.put(key, n + 10);
                                hurt(lv, me, v, dmg, 0.4);
                            }
                        }
                    }
                }
            }

            if (CONTACT_READY.size() > 512) {
                CONTACT_READY.values().removeIf(t -> t <= n);
            }
        }
    }

    private static final class Attack {
        final UUID who;
        final int scriptId;
        final int start;
        final int end;
        final String part;
        final Vector3f size;
        final Vector3f offset;
        final float damage;
        final double push;
        final UUID except;
        final BossMoves.Fx impact;
        final boolean show;
        final Set<UUID> hit = new HashSet<>();

        Attack(
            Entity who,
            String part,
            Vector3f size,
            Vector3f offset,
            float damage,
            double push,
            int start,
            int end,
            Entity except,
            BossMoves.Fx impact,
            boolean show,
            int scriptId
        ) {
            this.who = who.getUUID();
            this.part = part;
            this.size = size;
            this.offset = offset;
            this.damage = damage;
            this.push = push;
            this.start = start;
            this.end = end;
            this.except = except != null ? except.getUUID() : null;
            this.impact = impact;
            this.show = show;
            this.scriptId = scriptId;
        }
    }
}
