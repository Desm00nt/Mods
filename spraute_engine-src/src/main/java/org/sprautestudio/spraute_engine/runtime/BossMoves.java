package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class BossMoves {
    private static final Map<UUID, BossMoves.Move> MOVES = new LinkedHashMap<>();
    private static final List<BossMoves.Strike> STRIKES = new ArrayList<>();
    private static final List<BossMoves.Beam> BEAMS = new ArrayList<>();
    private static final Map<UUID, BossMoves.Bar> BARS = new LinkedHashMap<>();

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(BossMoves::onTick);
    }

    private BossMoves() {
    }

    private static void play(BossMoves.Fx fx, Vec3 at) {
        if (fx != null && fx.json != null) {
            SprauteNetwork.sendToAudience(fx.scriptId, new Packets.FxPlay("", fx.project, fx.json, at.x, at.y, at.z, -1, fx.scale, fx.scriptId));
        }
    }

    private static int now() {
        MinecraftServer s = Platform.get().server();
        return s == null ? 0 : s.getTickCount();
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

    private static void face(Entity e, Vec3 dir) {
        if (!(dir.x * dir.x + dir.z * dir.z < 1.0E-6)) {
            float yaw = (float)(Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0);
            e.setYRot(yaw);
            e.setYHeadRot(yaw);
            if (e instanceof LivingEntity le) {
                le.setYBodyRot(yaw);
            }
        }
    }

    private static void place(Entity e, Vec3 to) {
        Vec3 d = to.subtract(e.position());
        e.setDeltaMovement(d);
        e.setPos(to.x, to.y, to.z);
        e.fallDistance = 0.0F;
        e.hasImpulse = true;
        e.hurtMarked = true;
        if (e instanceof Mob m) {
            m.getNavigation().stop();
        }
    }

    private static void hurt(ServerLevel lv, Entity victim, Entity by, float damage) {
        if (!(damage <= 0.0F)) {
            DamageSource src = by != null ? Mc.indirectMagic(lv, by, by) : Mc.magic(lv);
            victim.hurt(src, damage);
        }
    }

    private static void begin(Entity e, BossMoves.Move m, int ticks) {
        BossMoves.Move old = MOVES.put(e.getUUID(), m);
        if (old != null && Mc.level(e) instanceof ServerLevel lv) {
            old.end(lv, e);
        }

        Fights.hold(e, ticks + 2);
    }

    public static boolean moving(Entity e) {
        return e != null && MOVES.containsKey(e.getUUID());
    }

    public static void stop(Entity e) {
        BossMoves.Move m = MOVES.remove(e.getUUID());
        if (m != null && Mc.level(e) instanceof ServerLevel lv) {
            m.end(lv, e);
        }

        Fights.hold(e, 0);
        BEAMS.removeIf(b -> b.from.equals(e.getUUID()));
    }

    public static void stopScript(int scriptId) {
        AttackBoxes.stopScript(scriptId);
        MOVES.values().removeIf(m -> m.scriptId == scriptId);
        STRIKES.removeIf(s -> s.scriptId == scriptId);
        BEAMS.removeIf(bx -> bx.scriptId == scriptId);
        Iterator<BossMoves.Bar> it = BARS.values().iterator();

        while (it.hasNext()) {
            BossMoves.Bar b = it.next();
            if (b.scriptId == scriptId) {
                b.event.removeAllPlayers();
                it.remove();
            }
        }
    }

    public static void leap(Entity e, int scriptId, Vec3 to, double height, int ticks, BossMoves.Fx impact, double radius, float damage, double push) {
        begin(e, new BossMoves.Leap(e, scriptId, WorldApi.ground(to), height, ticks, impact, radius, damage, push), ticks);
    }

    public static void charge(Entity e, int scriptId, Vec3 toward, double speed, double distance, float damage, double push, BossMoves.Fx trail) {
        Vec3 d = toward.subtract(e.position());
        Vec3 flat = new Vec3(d.x, 0.0, d.z);
        if (flat.lengthSqr() < 1.0E-6) {
            flat = Vec3.directionFromRotation(0.0F, e.getYRot());
        }

        double dist = distance > 0.0 ? distance : flat.length() + 2.0;
        flat = flat.normalize();
        int ticks = (int)Math.ceil(dist / Math.max(0.05, speed / 20.0)) + 2;
        begin(e, new BossMoves.Charge(e, scriptId, flat, speed, dist, damage, push, trail), ticks);
    }

    public static void circle(Entity e, int scriptId, Entity center, Vec3 point, double radius, double degPerSecond, int ticks) {
        begin(e, new BossMoves.Circle(e, scriptId, center, point, radius, degPerSecond, ticks), ticks);
    }

    public static void strike(
        ServerLevel lv,
        Vec3 at,
        double radius,
        int delayTicks,
        float damage,
        double push,
        BossMoves.Fx warn,
        BossMoves.Fx impact,
        Entity by,
        Entity except,
        int scriptId
    ) {
        play(warn, at);
        STRIKES.add(new BossMoves.Strike(lv, at, radius, now() + Math.max(0, delayTicks), damage, push, impact, by, except, scriptId));
    }

    private static void tickStrikes() {
        if (!STRIKES.isEmpty()) {
            int n = now();
            Iterator<BossMoves.Strike> it = STRIKES.iterator();

            while (it.hasNext()) {
                BossMoves.Strike s = it.next();
                if (s.due <= n) {
                    it.remove();
                    play(s.impact, s.at);
                    Entity by = s.by != null ? find(s.by) : null;
                    Entity except = s.except != null ? find(s.except) : by;
                    Mechanics.shockwave(s.lv, s.at, s.radius, s.push, s.damage, except, by);
                }
            }
        }
    }

    private static ParticleOptions particle(String id) {
        if (id != null && !id.isBlank()) {
            ResourceLocation rl = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
            if ((rl != null ? Regs.PARTICLE_TYPE.get(rl) : null) instanceof SimpleParticleType s) {
                return s;
            } else {
                throw new SprauteRuntimeException("beam: частица «" + id + "» не подходит (нужна простая: end_rod, flame, soul_fire_flame, electric_spark…)");
            }
        } else {
            return ParticleTypes.END_ROD;
        }
    }

    public static void beam(
        Entity from,
        Entity target,
        Vec3 point,
        int ticks,
        float dps,
        double range,
        double width,
        double track,
        String particle,
        BossMoves.Fx impact,
        int scriptId
    ) {
        BEAMS.removeIf(b -> b.from.equals(from.getUUID()));
        BEAMS.add(new BossMoves.Beam(from, target, point, ticks, dps, range, width, track, particle(particle), impact, scriptId));
    }

    public static boolean beaming(Entity e) {
        for (BossMoves.Beam b : BEAMS) {
            if (b.from.equals(e.getUUID())) {
                return true;
            }
        }

        return false;
    }

    private static Vec3 turn(Vec3 cur, Vec3 want, double maxDeg) {
        double cos = Math.max(-1.0, Math.min(1.0, cur.dot(want)));
        double ang = Math.toDegrees(Math.acos(cos));
        if (!(ang <= maxDeg) && !(ang < 0.001)) {
            double k = maxDeg / ang;
            return cur.lerp(want, k).normalize();
        } else {
            return want;
        }
    }

    private static void tickBeams() {
        if (!BEAMS.isEmpty()) {
            int n = now();
            Iterator<BossMoves.Beam> it = BEAMS.iterator();

            while (it.hasNext()) {
                BossMoves.Beam b = it.next();
                Entity src = find(b.from);
                if (src != null && src.isAlive() && n < b.until && Mc.level(src) instanceof ServerLevel lv) {
                    b.t++;
                    Vec3 var16 = src.getEyePosition();
                    Entity te = b.targetId != null ? find(b.targetId) : null;
                    Vec3 aim = te != null ? te.getBoundingBox().getCenter() : b.targetPoint;
                    if (aim == null) {
                        it.remove();
                    } else {
                        Vec3 want = aim.subtract(var16);
                        want = want.lengthSqr() < 1.0E-6 ? src.getLookAngle() : want.normalize();
                        b.dir = b.dir == null ? want : turn(b.dir, want, b.turnPerTick);
                        Vec3 end = var16.add(b.dir.scale(b.range));
                        BlockHitResult hitBlock = lv.clip(new ClipContext(var16, end, Block.COLLIDER, Fluid.NONE, src));
                        if (hitBlock.getType() != Type.MISS) {
                            end = hitBlock.getLocation();
                        }

                        face(src, b.dir);
                        double len = end.distanceTo(var16);

                        for (double d = 0.6; d < len; d += 0.45) {
                            Vec3 p = var16.add(b.dir.scale(d));
                            lv.sendParticles(b.particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                        }

                        if (b.t % 5 == 0) {
                            AABB area = new AABB(var16, end).inflate(b.width + 1.0);

                            for (Entity v : lv.getEntities(src, area, x -> x instanceof LivingEntity && x.isAlive() && !x.isSpectator())) {
                                if (v.getBoundingBox().inflate(b.width).clip(var16, end).isPresent()) {
                                    hurt(lv, v, src, b.dps / 4.0F);
                                }
                            }
                        }

                        if (b.t % 10 == 1) {
                            play(b.impact, end);
                        }
                    }
                } else {
                    it.remove();
                }
            }
        }
    }

    private static BossBarColor color(String c) {
        if (c == null) {
            return BossBarColor.RED;
        } else {
            String var1 = c.trim().toLowerCase();

            return switch (var1) {
                case "pink", "розовый" -> BossBarColor.PINK;
                case "blue", "синий" -> BossBarColor.BLUE;
                case "green", "зелёный", "зеленый" -> BossBarColor.GREEN;
                case "yellow", "жёлтый", "желтый" -> BossBarColor.YELLOW;
                case "purple", "фиолетовый" -> BossBarColor.PURPLE;
                case "white", "белый" -> BossBarColor.WHITE;
                default -> BossBarColor.RED;
            };
        }
    }

    private static BossBarOverlay overlay(String s) {
        if (s == null) {
            return BossBarOverlay.PROGRESS;
        } else {
            String var1 = s.trim();

            return switch (var1) {
                case "6" -> BossBarOverlay.NOTCHED_6;
                case "10" -> BossBarOverlay.NOTCHED_10;
                case "12" -> BossBarOverlay.NOTCHED_12;
                case "20" -> BossBarOverlay.NOTCHED_20;
                default -> BossBarOverlay.PROGRESS;
            };
        }
    }

    public static void bar(Entity e, String title, String color, String style, int scriptId) {
        hideBar(e);
        String name = title != null && !title.isBlank() ? title : e.getName().getString();
        ServerBossEvent ev = new ServerBossEvent(Component.literal(name), color(color), overlay(style));

        for (ServerPlayer p : SprauteRuntime.audience(scriptId)) {
            ev.addPlayer(p);
        }

        ev.setProgress((float)frac(e));
        BARS.put(e.getUUID(), new BossMoves.Bar(ev, scriptId));
    }

    public static void hideBar(Entity e) {
        BossMoves.Bar b = BARS.remove(e.getUUID());
        if (b != null) {
            b.event.removeAllPlayers();
        }
    }

    public static double hp(Entity e) {
        if (e == null) {
            return 0.0;
        } else {
            return !Fights.fighting(e) && !Fights.defeated(e) ? (e instanceof LivingEntity le ? Math.max(0.0F, le.getHealth()) : 0.0F) : Fights.hp(e);
        }
    }

    public static double maxHp(Entity e) {
        if (e == null) {
            return 0.0;
        } else if (!Fights.fighting(e) && !Fights.defeated(e)) {
            return e instanceof LivingEntity le ? le.getMaxHealth() : 0.0;
        } else {
            return Fights.maxHp(e);
        }
    }

    public static double frac(Entity e) {
        double m = maxHp(e);
        return m <= 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, hp(e) / m));
    }

    private static void tickBars() {
        if (!BARS.isEmpty()) {
            Iterator<Entry<UUID, BossMoves.Bar>> it = BARS.entrySet().iterator();

            while (it.hasNext()) {
                Entry<UUID, BossMoves.Bar> en = it.next();
                Entity e = find(en.getKey());
                ServerBossEvent ev = en.getValue().event;
                if (e != null || Fights.gone(en.getKey())) {
                    if (e != null && e.isAlive() && !Fights.defeated(e)) {
                        ev.setProgress((float)frac(e));

                        for (ServerPlayer p : SprauteRuntime.players()) {
                            if (!ev.getPlayers().contains(p)) {
                                ev.addPlayer(p);
                            }
                        }
                    } else {
                        ev.removeAllPlayers();
                        it.remove();
                    }
                }
            }
        }
    }

    public static void onTick(Hooks.ServerTick ev) {
        if (ev.phase == TickPhase.END) {
            try {
                if (!MOVES.isEmpty()) {
                    Iterator<Entry<UUID, BossMoves.Move>> it = MOVES.entrySet().iterator();

                    while (it.hasNext()) {
                        BossMoves.Move m = it.next().getValue();
                        Entity e = find(m.who);
                        if (!(e != null && e.isAlive() && Mc.level(e) instanceof ServerLevel lv)) {
                            it.remove();
                        } else if (m.step(lv, e)) {
                            it.remove();
                            m.end(lv, e);
                            Fights.hold(e, 0);
                        }
                    }
                }

                tickStrikes();
                tickBeams();
                if (now() % 2 == 0) {
                    tickBars();
                }
            } catch (RuntimeException var6) {
                SprauteEngine.LOGGER.warn("Spraute boss moves", (Throwable)var6);
            }
        }
    }

    private static final class Bar {
        final ServerBossEvent event;
        final int scriptId;

        Bar(ServerBossEvent event, int scriptId) {
            this.event = event;
            this.scriptId = scriptId;
        }
    }

    private static final class Beam {
        final UUID from;
        final UUID targetId;
        final Vec3 targetPoint;
        final int until;
        final int scriptId;
        final double range;
        final double width;
        final double turnPerTick;
        final float dps;
        final ParticleOptions particle;
        final BossMoves.Fx impact;
        Vec3 dir;
        int t;

        Beam(
            Entity from,
            Entity target,
            Vec3 point,
            int ticks,
            float dps,
            double range,
            double width,
            double turnDegPerSecond,
            ParticleOptions particle,
            BossMoves.Fx impact,
            int scriptId
        ) {
            this.from = from.getUUID();
            this.targetId = target != null ? target.getUUID() : null;
            this.targetPoint = point;
            this.until = BossMoves.now() + Math.max(1, ticks);
            this.dps = dps;
            this.range = range;
            this.width = width;
            this.turnPerTick = turnDegPerSecond <= 0.0 ? 360.0 : turnDegPerSecond / 20.0;
            this.particle = particle;
            this.impact = impact;
            this.scriptId = scriptId;
        }
    }

    private static final class Charge extends BossMoves.Move {
        final Vec3 dir;
        final double step;
        final double distance;
        final double push;
        final float damage;
        final BossMoves.Fx trail;
        final Set<UUID> hit = new HashSet<>();
        double gone;

        Charge(Entity e, int scriptId, Vec3 dir, double speed, double distance, float damage, double push, BossMoves.Fx trail) {
            super(e, scriptId);
            this.dir = dir;
            this.step = Math.max(0.05, speed / 20.0);
            this.distance = distance;
            this.damage = damage;
            this.push = push;
            this.trail = trail;
        }

        @Override
        boolean step(ServerLevel lv, Entity e) {
            this.t++;
            Vec3 next = e.position().add(this.dir.scale(this.step));
            Vec3 floor = WorldApi.ground(new Vec3(next.x, e.getY() + 1.2, next.z));
            if (Math.abs(floor.y - e.getY()) > 1.2) {
                return true;
            } else {
                next = new Vec3(next.x, floor.y, next.z);
                if (!lv.noCollision(e, e.getBoundingBox().move(next.subtract(e.position())).deflate(0.05))) {
                    return true;
                } else {
                    BossMoves.face(e, this.dir);
                    BossMoves.place(e, next);
                    this.gone = this.gone + this.step;
                    AABB box = e.getBoundingBox().inflate(0.4);

                    for (Entity v : lv.getEntities(e, box, x -> x instanceof LivingEntity && x.isAlive() && !x.isSpectator())) {
                        if (this.hit.add(v.getUUID())) {
                            BossMoves.hurt(lv, v, e, this.damage);
                            v.push(this.dir.x * this.push, 0.25 + this.push * 0.2, this.dir.z * this.push);
                            v.hurtMarked = true;
                        }
                    }

                    if (this.trail != null && this.t % 3 == 0) {
                        BossMoves.play(this.trail, e.position());
                    }

                    return this.gone >= this.distance;
                }
            }
        }

        @Override
        void end(ServerLevel lv, Entity e) {
            e.setDeltaMovement(Vec3.ZERO);
        }
    }

    private static final class Circle extends BossMoves.Move {
        final UUID centerId;
        final Vec3 centerPoint;
        final double radius;
        final double degPerTick;
        final int len;
        double angle;

        Circle(Entity e, int scriptId, Entity center, Vec3 point, double radius, double degPerSecond, int len) {
            super(e, scriptId);
            this.centerId = center != null ? center.getUUID() : null;
            this.centerPoint = point;
            this.radius = Math.max(1.0, radius);
            this.degPerTick = degPerSecond / 20.0;
            this.len = Math.max(1, len);
            Vec3 c = center != null ? center.position() : point;
            this.angle = Math.toDegrees(Math.atan2(e.getZ() - c.z, e.getX() - c.x));
        }

        @Override
        boolean step(ServerLevel lv, Entity e) {
            this.t++;
            Entity ce = this.centerId != null ? BossMoves.find(this.centerId) : null;
            Vec3 c = ce != null ? ce.position() : this.centerPoint;
            if (c == null) {
                return true;
            } else {
                this.angle = this.angle + this.degPerTick;
                double a = Math.toRadians(this.angle);
                Vec3 want = new Vec3(c.x + Math.cos(a) * this.radius, e.getY() + 1.2, c.z + Math.sin(a) * this.radius);
                Vec3 floor = WorldApi.ground(want);
                Vec3 to = new Vec3(floor.x, floor.y, floor.z);
                Vec3 d = to.subtract(e.position());
                double max = 0.6;
                if (d.length() > max) {
                    to = e.position().add(d.normalize().scale(max));
                }

                if (Math.abs(floor.y - e.getY()) <= 1.2) {
                    BossMoves.place(e, new Vec3(to.x, floor.y, to.z));
                }

                BossMoves.face(e, c.subtract(e.position()));
                return this.t >= this.len;
            }
        }
    }

    public record Fx(String json, String project, int scriptId, float scale) {
    }

    private static final class Leap extends BossMoves.Move {
        final Vec3 from;
        final Vec3 to;
        final double height;
        final int len;
        final BossMoves.Fx impact;
        final double radius;
        final double push;
        final float damage;

        Leap(Entity e, int scriptId, Vec3 to, double height, int len, BossMoves.Fx impact, double radius, float damage, double push) {
            super(e, scriptId);
            this.from = e.position();
            this.to = to;
            this.height = height;
            this.len = Math.max(4, len);
            this.impact = impact;
            this.radius = radius;
            this.damage = damage;
            this.push = push;
        }

        @Override
        boolean step(ServerLevel lv, Entity e) {
            this.t++;
            double k = Math.min(1.0, (double)this.t / this.len);
            Vec3 p = this.from.lerp(this.to, k).add(0.0, 4.0 * this.height * k * (1.0 - k), 0.0);
            BossMoves.face(e, this.to.subtract(this.from));
            BossMoves.place(e, p);
            return k >= 1.0;
        }

        @Override
        void end(ServerLevel lv, Entity e) {
            if (this.t >= this.len) {
                e.setDeltaMovement(Vec3.ZERO);
                BossMoves.play(this.impact, this.to);
                if (this.radius > 0.0) {
                    Mechanics.shockwave(lv, this.to, this.radius, this.push, this.damage, e, e);
                }
            }
        }
    }

    private abstract static class Move {
        final UUID who;
        final int scriptId;
        int t;

        Move(Entity e, int scriptId) {
            this.who = e.getUUID();
            this.scriptId = scriptId;
        }

        abstract boolean step(ServerLevel var1, Entity var2);

        void end(ServerLevel lv, Entity e) {
        }
    }

    private static final class Strike {
        final ServerLevel lv;
        final Vec3 at;
        final double radius;
        final double push;
        final float damage;
        final int due;
        final int scriptId;
        final BossMoves.Fx impact;
        final UUID by;
        final UUID except;

        Strike(ServerLevel lv, Vec3 at, double radius, int due, float damage, double push, BossMoves.Fx impact, Entity by, Entity except, int scriptId) {
            this.lv = lv;
            this.at = at;
            this.radius = radius;
            this.due = due;
            this.damage = damage;
            this.push = push;
            this.impact = impact;
            this.by = by != null ? by.getUUID() : null;
            this.except = except != null ? except.getUUID() : null;
            this.scriptId = scriptId;
        }
    }
}
