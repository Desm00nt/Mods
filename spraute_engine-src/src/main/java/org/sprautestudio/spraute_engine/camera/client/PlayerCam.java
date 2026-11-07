package org.sprautestudio.spraute_engine.camera.client;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class PlayerCam {
    private static final double PROBE = 0.1;
    private static final double GAP = 0.15;
    private static CamProfile rig;
    private static CamProfile preview;
    private static final Map<String, CamProfile> ZONES = new LinkedHashMap<>();
    private static double lockedX;
    private static double lockedY;
    private static double lockedZ;
    private static float ownYaw;
    private static float ownPitch;
    private static boolean ownLook;
    private static double lastMouse;
    private static double blendStart;
    private static double blendLength;
    private static double[] blendFrom;
    private static double[] shown;
    private static double[] smooth;
    private static long lastFrame;
    private static float shownYaw;
    private static boolean detached;
    private static double aimK;
    private static double runK;
    private static boolean lookingAtTarget;
    private static float bobPhase;
    private static float bobFade;
    private static float landPunch;
    private static float bobRoll;
    private static float bobFov;
    private static boolean wasOnGround = true;
    private static double lastFall;

    private PlayerCam() {
    }

    public static void registerHooks() {
        ClientHooks.MOVEMENT_INPUT.add(PlayerCam::onInput);
        ClientHooks.RENDER_HAND.add(PlayerCam::onHand);
        ClientHooks.COMPUTE_FOV.add(PlayerCam::onFov);
        ClientHooks.RENDER_HUD_PART.add(PlayerCam::onHud);
        ClientHooks.CAMERA_ANGLES.add(PlayerCam::onAngles);
        ClientHooks.CLIENT_TICK.add(PlayerCam::onTick);
        ClientHooks.LOGGING_OUT.add(e -> reset());
    }

    private static double now() {
        return System.nanoTime() / 1.0E9;
    }

    public static CamProfile active() {
        if (preview != null) {
            return preview;
        } else {
            CamProfile zone = null;

            for (CamProfile z : ZONES.values()) {
                zone = z;
            }

            return zone != null ? zone : rig;
        }
    }

    private static boolean isFirstPerson(CamProfile r) {
        return r == null || r.values.isEmpty();
    }

    public static void reset() {
        preview = null;
        rig = null;
        ZONES.clear();
        smooth = null;
        shown = null;
        blendFrom = null;
        ownLook = false;
        runK = 0.0;
        aimK = 0.0;
    }

    public static void show(CamProfile next, double seconds) {
        rig = next;
        begin(seconds);
    }

    public static void preview(CamProfile next) {
        preview = next;
        begin(0.2);
    }

    public static void set(String id, double value, double seconds) {
        if (rig == null) {
            rig = new CamProfile();
        }

        rig.values.put(id, value);
        begin(seconds);
    }

    private static void begin(double seconds) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (shown != null) {
            blendFrom = (double[])shown.clone();
        } else if (p != null) {
            Vec3 eyes = p.getEyePosition(1.0F);
            blendFrom = new double[]{eyes.x, eyes.y, eyes.z, p.getViewYRot(1.0F), p.getViewXRot(1.0F)};
        } else {
            blendFrom = null;
        }

        blendStart = now();
        blendLength = Math.max(0.0, seconds);
        smooth = null;
        CamProfile r = active();
        if (p != null && r != null) {
            CamProfile free = r.copy();
            free.values.remove("lock_x");
            free.values.remove("lock_y");
            free.values.remove("lock_z");
            double[] target = compute(p, free, 1.0F, 0.0, 0.0, 0.0);
            lockedX = target[0];
            lockedY = target[1];
            lockedZ = target[2];
            if (!ownLook) {
                ownYaw = p.getYRot();
                ownPitch = p.getXRot();
                ownLook = true;
            }
        }
    }

    public static void op(JsonObject o) {
        double time = o.has("time") ? o.get("time").getAsDouble() : 0.6;
        if (o.has("zone")) {
            String zone = o.get("zone").getAsString();
            ZONES.remove(zone);
            if (o.has("profile")) {
                ZONES.put(zone, CamProfile.fromJson(o.get("profile").getAsString()));
            }

            begin(time);
        } else if (o.has("profile")) {
            show(CamProfile.fromJson(o.get("profile").getAsString()), time);
        } else if (o.has("param")) {
            set(o.get("param").getAsString(), o.get("value").getAsDouble(), time);
        } else {
            show(null, time);
        }
    }

    private static Vec3 boom(double x, double y, double z, float yaw, float pitch) {
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw);
        Vec3 right = Vec3.directionFromRotation(0.0F, yaw + 90.0F);
        Vec3 up = forward.cross(right).scale(-1.0);
        return right.scale(x).add(up.scale(y)).add(forward.scale(-z));
    }

    private static float look(boolean locked, double set, float base) {
        return (float)(locked ? set : base + set);
    }

    private static double[] compute(Entity e, CamProfile r, float partial, double lx, double ly, double lz) {
        boolean turnsAlone = r.on("separate_turn") && ownLook;
        float baseYaw = turnsAlone ? ownYaw : e.getViewYRot(partial);
        float basePitch = turnsAlone ? ownPitch : e.getViewXRot(partial);
        float yaw = look(r.on("lock_yaw"), r.num("yaw"), baseYaw);
        float pitch = look(r.on("lock_pitch"), r.num("pitch"), basePitch);
        Vec3 eyes = e.getEyePosition(partial);
        Vec3 anchor = eyes;
        double dist = r.num("distance");
        double right = r.num("x");
        double up = r.num("y");
        if (r.on("aim") && aimK > 0.0) {
            dist += (r.num("aim_distance") - dist) * aimK;
            right += (r.num("aim_x") - right) * aimK;
        }

        dist += r.num("run_distance") * runK;
        float outYaw = yaw;
        float outPitch = pitch;
        double x;
        double y;
        double z;
        if (r.on("orbit")) {
            anchor = eyes.add(boom(r.num("pivot_x"), r.num("pivot_y"), r.num("pivot_z"), yaw, 0.0F));
            Vec3 at = anchor.add(boom(right, up, r.num("z") + dist, yaw + (float)r.num("orbit_yaw"), pitch + (float)r.num("orbit_pitch")));
            x = at.x;
            y = at.y;
            z = at.z;
            Vec3 d = anchor.subtract(at);
            outYaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
            outPitch = (float)(-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
        } else {
            Vec3 at = eyes.add(boom(right, up, r.num("z") + dist, yaw, pitch));
            x = at.x;
            y = at.y;
            z = at.z;
        }

        if (r.on("collide")) {
            Vec3 pulled = pullOut(e, anchor, new Vec3(x, y, z));
            x = pulled.x;
            y = pulled.y;
            z = pulled.z;
        }

        if (r.on("lock_x")) {
            x = r.has("world_x") ? r.num("world_x") : lx;
        }

        if (r.on("lock_y")) {
            y = r.has("world_y") ? r.num("world_y") : ly;
        }

        if (r.on("lock_z")) {
            z = r.has("world_z") ? r.num("world_z") : lz;
        }

        return new double[]{x, y, z, outYaw, outPitch};
    }

    private static Vec3 pullOut(Entity e, Vec3 anchor, Vec3 target) {
        Vec3 delta = target.subtract(anchor);
        double reach = delta.length();
        double nearest = reach;
        if (reach < 1.0E-4) {
            return target;
        } else {
            for (int corner = 0; corner < 8; corner++) {
                Vec3 from = anchor.add((corner & 1) == 0 ? -0.1 : 0.1, (corner & 2) == 0 ? -0.1 : 0.1, (corner & 4) == 0 ? -0.1 : 0.1);
                BlockHitResult hit = Mc.level(e).clip(new ClipContext(from, from.add(delta), Block.VISUAL, Fluid.NONE, e));
                if (hit.getType() != Type.MISS) {
                    nearest = Math.min(nearest, hit.getLocation().distanceTo(from));
                }
            }

            return nearest >= reach ? target : anchor.add(delta.scale(Math.max(0.0, nearest - 0.15) / reach));
        }
    }

    private static double angleLerp(double from, double to, double k) {
        return from + Mth.wrapDegrees((float)(to - from)) * k;
    }

    public static double[] view(Entity e, float partial) {
        Minecraft mc = Minecraft.getInstance();
        CamProfile r = active();
        boolean gliding = blendFrom != null && now() < blendStart + blendLength;
        if (e != mc.player || mc.getCameraEntity() != mc.player) {
            shown = null;
            return null;
        } else if (isFirstPerson(r) && !gliding) {
            shown = null;
            detached = false;
            return null;
        } else {
            long t = System.nanoTime();
            double dt = lastFrame == 0L ? 0.0 : Math.min(0.1, (t - lastFrame) / 1.0E9);
            lastFrame = t;
            if (r != null) {
                animate(r, mc.player, dt);
            }

            Vec3 eyes = e.getEyePosition(partial);
            double[] want = isFirstPerson(r)
                ? new double[]{eyes.x, eyes.y, eyes.z, e.getViewYRot(partial), e.getViewXRot(partial)}
                : compute(e, r, partial, lockedX, lockedY, lockedZ);
            double lag = r != null ? r.num("smooth") : 0.0;
            if (lag > 0.0 && smooth != null) {
                double k = 1.0 - Math.exp(-dt * (2.0 + (1.0 - lag) * (1.0 - lag) * 40.0));

                for (int i = 0; i < 3; i++) {
                    want[i] = smooth[i] + (want[i] - smooth[i]) * k;
                }
            }

            smooth = (double[])want.clone();
            double[] out = (double[])want.clone();
            if (gliding) {
                double k = (now() - blendStart) / Math.max(0.001, blendLength);
                k = k * k * (3.0 - 2.0 * k);

                for (int i = 0; i < 3; i++) {
                    out[i] = blendFrom[i] + (want[i] - blendFrom[i]) * k;
                }

                out[3] = angleLerp(blendFrom[3], want[3], k);
                out[4] = angleLerp(blendFrom[4], want[4], k);
            }

            shown = (double[])out.clone();
            shownYaw = (float)out[3];
            if (r != null && r.num("walk_bob") > 0.0) {
                walkBob(out, r.num("walk_bob"), mc.player, dt);
            } else {
                bobRoll = 0.0F;
                bobFov = 0.0F;
            }

            double dx = shown[0] - eyes.x;
            double dy = shown[1] - eyes.y;
            double dz = shown[2] - eyes.z;
            detached = dx * dx + dy * dy + dz * dz > 0.04;
            return out;
        }
    }

    private static void animate(CamProfile r, LocalPlayer p, double dt) {
        boolean aiming = aiming(r);
        aimK = aimK + ((aiming ? 1 : 0) - aimK) * (1.0 - Math.exp(-dt * 12.0));
        boolean running = p.isSprinting() && !aiming;
        runK = runK + ((running ? 1 : 0) - runK) * (1.0 - Math.exp(-dt * 4.0));
        double center = r.num("auto_center");
        if (center > 0.0 && r.on("separate_turn") && !aiming && now() - lastMouse > 1.2 && p.getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            ownYaw = ownYaw + Mth.wrapDegrees(p.getYRot() - ownYaw) * (float)(1.0 - Math.exp(-dt * center * 3.0));
        }
    }

    private static boolean aiming(CamProfile r) {
        Minecraft mc = Minecraft.getInstance();
        return r.on("aim") && mc.screen == null && mc.options.keyUse.isDown();
    }

    private static void walkBob(double[] out, double amount, LocalPlayer p, double dt) {
        bobRoll = 0.0F;
        bobFov = 0.0F;
        if (!p.isPassenger() && !p.getAbilities().flying) {
            float speed = (float)p.getDeltaMovement().horizontalDistance();
            boolean ground = Mc.onGround(p);
            double fall = p.getDeltaMovement().y;
            if (!wasOnGround && ground && lastFall < -0.28) {
                landPunch = (float)Math.min(1.4, landPunch - lastFall * 0.85);
            }

            wasOnGround = ground;
            lastFall = fall;
            landPunch = landPunch * (float)Math.exp(-dt * 5.0);
            float target = ground ? Mth.clamp(speed / 0.21F, 0.0F, 1.5F) : 0.0F;
            bobFade = bobFade + (target - bobFade) * Math.min(1.0F, (float)dt * 10.0F);
            if (bobFade > 0.01F && ground) {
                bobPhase += (float)(dt * (8.5 + speed * 18.0F));
            }

            double a = amount * bobFade;
            double yaw = Math.toRadians(out[3]);
            double side = Math.cos(bobPhase * 0.5) * 0.018 * a;
            out[0] += -Math.cos(yaw) * side;
            out[2] += -Math.sin(yaw) * side;
            out[1] += Math.sin(bobPhase) * 0.038 * a - landPunch * 0.07 * amount;
            out[4] += Math.cos(bobPhase) * 0.42 * a + landPunch * 2.8 * amount;
            bobRoll = (float)(Math.sin(bobPhase * 0.5) * 0.28 * a);
            bobFov = (float)(landPunch * 1.2 * amount + (p.isSprinting() && bobFade > 0.2F ? 1.1 * amount * Math.min(1.0F, bobFade) : 0.0));
        } else {
            bobFade = (float)Math.max(0.0, bobFade - dt * 6.0);
        }
    }

    public static boolean detached() {
        return detached;
    }

    public static double[] shownPose() {
        return shown;
    }

    private static boolean applies() {
        Minecraft mc = Minecraft.getInstance();
        return !isFirstPerson(active()) && mc.player != null && mc.getCameraEntity() == mc.player;
    }

    public static boolean takeMouse(double dx, double dy) {
        if (!applies()) {
            return false;
        } else {
            CamProfile r = active();
            if (r.on("separate_turn")) {
                Minecraft mc = Minecraft.getInstance();
                double s = mc.options.sensitivity().get() * 0.6 + 0.2;
                double k = s * s * s * 8.0 * 0.15;
                if (dx != 0.0 || dy != 0.0) {
                    lastMouse = now();
                }

                ownYaw += (float)(dx * k);
                ownPitch = Mth.clamp(ownPitch + (float)(dy * k * (mc.options.invertYMouse().get() ? -1 : 1)), -90.0F, 90.0F);
                return true;
            } else {
                return r.on("lock_yaw") && r.on("lock_pitch");
            }
        }
    }

    private static void turnBody(LocalPlayer p, float yaw) {
        p.setYRot(yaw);
        p.setYHeadRot(yaw);
        p.setYBodyRot(yaw);
    }

    public static void onInput(ClientHooks.MovementInput e) {
        if (applies()) {
            CamProfile r = active();
            LocalPlayer p = Minecraft.getInstance().player;
            if (!p.isPassenger()) {
                Input in = e.getInput();
                float forward = in.forwardImpulse;
                float left = in.leftImpulse;
                if (r.on("plane_move")) {
                    forward = 0.0F;
                }

                boolean relative = r.on("move_relative") || r.on("separate_turn") || r.on("lock_yaw") || r.on("orbit");
                if (relative) {
                    float camYaw = shown != null ? shownYaw : p.getYRot();
                    boolean aiming = aiming(r) && r.on("separate_turn");
                    boolean facesWalk = !aiming && !lookingAtTarget && (r.on("separate_turn") || r.on("lock_yaw"));
                    if (aiming) {
                        turnBody(p, ownYaw);
                        p.setXRot(ownPitch);
                    } else if (facesWalk && (forward != 0.0F || left != 0.0F)) {
                        float want = camYaw + (float)Math.toDegrees(Math.atan2(-left, forward));
                        float k = (float)(1.0 - Math.min(0.95, r.num("turn_smooth")));
                        turnBody(p, p.getYRot() + Mth.wrapDegrees(want - p.getYRot()) * k);
                    }

                    double d = Math.toRadians(Mth.wrapDegrees(camYaw - p.getYRot()));
                    double cos = Math.cos(d);
                    double sin = Math.sin(d);
                    in.leftImpulse = (float)(left * cos - forward * sin);
                    in.forwardImpulse = (float)(forward * cos + left * sin);
                } else {
                    in.forwardImpulse = forward;
                }

                if (r.on("plane_move")) {
                    in.up = in.down = false;
                }
            }
        }
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END) {
            CamOcclusion.tick();
            lookingAtTarget = false;
            if (applies() && active().on("look_at_target")) {
                Minecraft mc = Minecraft.getInstance();
                HitResult hit = mc.hitResult;
                LocalPlayer p = mc.player;
                if (hit != null && hit.getType() != Type.MISS) {
                    boolean busy = p.swinging || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown();
                    if (busy) {
                        lookingAtTarget = true;
                        Vec3 at = hit instanceof EntityHitResult eh ? eh.getEntity().getBoundingBox().getCenter() : hit.getLocation();
                        Vec3 d = at.subtract(p.getEyePosition(1.0F));
                        if (!(d.lengthSqr() < 1.0E-6)) {
                            float yaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
                            float pitch = (float)(-Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
                            float k = (float)(1.0 - Math.min(0.9, active().num("turn_smooth")));
                            turnBody(p, p.getYRot() + Mth.wrapDegrees(yaw - p.getYRot()) * k);
                            p.setXRot(Mth.clamp(p.getXRot() + (pitch - p.getXRot()) * k, -90.0F, 90.0F));
                        }
                    }
                }
            }
        }
    }

    public static void onHand(ClientHooks.RenderHand e) {
        if (detached && applies() && !active().on("show_hand")) {
            e.setCanceled(true);
        }
    }

    public static void onFov(ClientHooks.ComputeFov e) {
        if (applies()) {
            CamProfile r = active();
            double fov = r.num("fov") > 0.0 ? r.num("fov") : e.getFOV();
            if (r.on("aim") && r.num("aim_fov") > 0.0) {
                fov += (r.num("aim_fov") - fov) * aimK;
            }

            fov += r.num("run_fov") * runK + bobFov;
            if (fov != e.getFOV()) {
                e.setFOV(fov);
            }
        }
    }

    public static void onAngles(ClientHooks.CameraAngles e) {
        if (bobRoll != 0.0F && applies()) {
            e.setRoll(e.getRoll() + bobRoll);
        }
    }

    public static void onHud(ClientHooks.RenderHudPart e) {
        if (applies() && active().on("hide_crosshair") && e.id().getPath().equals("crosshair")) {
            e.setCanceled(true);
        }
    }

    public static boolean noVanillaBob() {
        CamProfile r = active();
        return applies() && (r.on("no_bob") || r.num("walk_bob") > 0.0);
    }

    public static boolean noVanillaHurt() {
        return applies() && active().on("no_hurt_shake");
    }

    public static boolean pick(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (applies() && active().on("screen_aim") && shown != null && mc.level != null && mc.gameMode != null) {
            Entity body = mc.getCameraEntity();
            if (body == null) {
                return false;
            } else {
                Vec3 cam = new Vec3(shown[0], shown[1], shown[2]);
                Vec3 dir = Vec3.directionFromRotation((float)shown[4], (float)shown[3]);
                double skip = Math.max(0.0, cam.distanceTo(body.getEyePosition(partial)) - 0.2);
                Vec3 start = cam.add(dir.scale(skip));
                double reach = mc.gameMode.getPickRange();
                if (mc.gameMode.hasFarPickRange()) {
                    reach = Math.max(reach, 6.0);
                }

                BlockHitResult block = mc.level.clip(new ClipContext(start, start.add(dir.scale(reach)), Block.OUTLINE, Fluid.NONE, body));
                double blockSq = block.getType() != Type.MISS ? start.distanceToSqr(block.getLocation()) : reach * reach;
                double limit = Math.max(reach, Math.sqrt(blockSq));
                Vec3 end = start.add(dir.scale(limit));
                EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                    body, start, end, new AABB(start, end).inflate(1.0), t -> !t.isSpectator() && t.isPickable() && t != body, reach * reach
                );
                mc.crosshairPickEntity = null;
                if (hit == null) {
                    mc.hitResult = block;
                    return true;
                } else {
                    double hitSq = start.distanceToSqr(hit.getLocation());
                    if (hitSq > reach * reach) {
                        Vec3 at = hit.getLocation();
                        mc.hitResult = BlockHitResult.miss(
                            at, Direction.getNearest(dir.x, dir.y, dir.z), new BlockPos(Mth.floor(at.x), Mth.floor(at.y), Mth.floor(at.z))
                        );
                    } else if (!(hitSq < blockSq) && block.getType() != Type.MISS) {
                        mc.hitResult = block;
                    } else {
                        mc.hitResult = hit;
                        if (hit.getEntity() instanceof LivingEntity || hit.getEntity() instanceof ItemFrame) {
                            mc.crosshairPickEntity = hit.getEntity();
                        }
                    }

                    return true;
                }
            }
        } else {
            return false;
        }
    }
}
