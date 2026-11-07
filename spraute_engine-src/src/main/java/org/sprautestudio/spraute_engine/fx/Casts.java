package org.sprautestudio.spraute_engine.fx;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Casts {
    private static final List<Casts.Cast> FLYING = new ArrayList<>();
    private static int counter;

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(Casts::onTick);
    }

    private Casts() {
    }

    public static String nextId() {
        return "cast_" + ++counter;
    }

    public static void launch(Casts.Cast c, String fxJson) {
        FLYING.add(c);
        SprauteNetwork.sendToAudience(
            c.scriptId, new Packets.FxLaunch(c.fxId, c.project, fxJson, c.pos.x, c.pos.y, c.pos.z, c.vel.x, c.vel.y, c.vel.z, c.gravity, c.scale, c.scriptId)
        );
    }

    public static void stopScript(int scriptId) {
        FLYING.removeIf(c -> c.scriptId == scriptId);
    }

    public static void unload() {
        FLYING.clear();
    }

    public static void onTick(Hooks.ServerTick e) {
        if (e.phase == TickPhase.END && !FLYING.isEmpty()) {
            Iterator<Casts.Cast> it = FLYING.iterator();

            while (it.hasNext()) {
                Casts.Cast c = it.next();

                try {
                    if (step(c)) {
                        it.remove();
                    }
                } catch (RuntimeException var4) {
                    it.remove();
                    SprauteEngine.LOGGER.warn("Spraute cast {}", c.id, var4);
                }
            }
        }
    }

    private static boolean step(Casts.Cast c) {
        Vec3 from = c.pos;
        Vec3 to = from.add(c.vel);
        Entity ctx = (Entity)(c.owner != null ? c.owner : SprauteRuntime.playerOf(c.scriptId));
        BlockHitResult bh = ctx == null ? null : c.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, ctx));
        Vec3 end = bh != null && bh.getType() == Type.BLOCK ? bh.getLocation() : to;
        AABB box = new AABB(from, end).inflate(c.radius + 1.0);
        List<Entity> hits = new ArrayList<>();

        for (Entity en : c.level.getEntities(c.owner, box, enx -> enx instanceof LivingEntity && enx.isAlive() && !enx.isSpectator() && !c.hit.contains(enx))) {
            AABB bb = en.getBoundingBox().inflate(c.radius);
            if (bb.contains(from) || bb.clip(from, end).isPresent()) {
                hits.add(en);
            }
        }

        hits.sort(Comparator.comparingDouble(enx -> enx.distanceToSqr(from)));

        for (Entity enx : hits) {
            Vec3 at = enx.getBoundingBox().inflate(c.radius).clip(from, end).orElse(enx.getBoundingBox().getCenter());
            strike(c, enx, at);
            if (!c.pierce) {
                finish(c, at);
                return true;
            }
        }

        if (bh != null && bh.getType() == Type.BLOCK) {
            report(c, "block", null, bh.getLocation(), bh.getBlockPos());
            finish(c, bh.getLocation());
            return true;
        } else {
            c.pos = to;
            c.vel = c.vel.add(0.0, -c.gravity, 0.0);
            if (--c.life <= 0) {
                report(c, "none", null, c.pos, null);
                finish(c, null);
                return true;
            } else {
                return false;
            }
        }
    }

    private static void strike(Casts.Cast c, Entity en, Vec3 at) {
        c.hit.add(en);
        if (c.damage > 0.0F && en instanceof LivingEntity le) {
            DamageSource src = c.owner != null ? Mc.indirectMagic(c.level, c.owner, c.owner) : Mc.magic(c.level);
            le.hurt(src, c.damage);
            if (c.knockback > 0.0F) {
                Vec3 dir = c.vel.multiply(1.0, 0.0, 1.0);
                if (dir.lengthSqr() > 1.0E-6) {
                    dir = dir.normalize();
                    le.knockback(c.knockback, -dir.x, -dir.z);
                    le.hurtMarked = true;
                }
            }
        }

        report(c, "entity", en, at, null);
        if (c.pierce && !c.impactJson.isEmpty()) {
            impact(c, at);
        }
    }

    private static void impact(Casts.Cast c, Vec3 at) {
        SprauteNetwork.sendToAudience(c.scriptId, new Packets.FxPlay("", c.project, c.impactJson, at.x, at.y, at.z, -1, c.scale, c.scriptId));
    }

    private static void finish(Casts.Cast c, Vec3 at) {
        SprauteNetwork.sendToAudience(c.scriptId, new Packets.FxStop(c.fxId, -1));
        if (at != null && !c.impactJson.isEmpty()) {
            impact(c, at);
        }
    }

    private static void report(Casts.Cast c, String kind, Entity target, Vec3 at, BlockPos block) {
        Map<String, Object> d = new HashMap<>();
        d.put("id", c.id);
        d.put("fx", c.fx);
        d.put("hit", kind);
        d.put("damage", (double)(kind.equals("entity") ? c.damage : 0.0F));
        d.put("x", at.x);
        d.put("y", at.y);
        d.put("z", at.z);
        if (target != null) {
            d.put("__entity", target);
            d.put("entity", target instanceof Player ? "player" : String.valueOf(Regs.ENTITY_TYPE.getKey(target.getType())));
            d.put("name", target.getName().getString());
        } else {
            d.put("entity", "");
            d.put("name", "");
        }

        d.put("block", block != null ? String.valueOf(Regs.BLOCK.getKey(c.level.getBlockState(block).getBlock())) : "");
        GameEvents.post("casthit", SprauteRuntime.playerOf(c.scriptId), d);
    }

    public static boolean flying(String id) {
        for (Casts.Cast c : FLYING) {
            if (c.id.equals(id)) {
                return true;
            }
        }

        return false;
    }

    public static final class Cast {
        public String id = "";
        public String fx = "";
        public String project = "";
        public String impactJson = "";
        public String fxId = Casts.nextId();
        public Vec3 pos;
        public Vec3 vel;
        public double gravity;
        public double radius = 0.4;
        public float damage = 4.0F;
        public float knockback = 0.4F;
        public float scale = 1.0F;
        public int life = 60;
        public int scriptId = -1;
        public Entity owner;
        public ServerLevel level;
        public boolean pierce;
        final Set<Entity> hit = new HashSet<>();
    }
}
