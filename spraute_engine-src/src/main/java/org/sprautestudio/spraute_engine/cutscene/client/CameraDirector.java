package org.sprautestudio.spraute_engine.cutscene.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.platform.InputConstants.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.camera.client.PlayerCam;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.cutscene.CutsceneDoc;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.post.client.PostEngine;
import org.sprautestudio.spraute_engine.runtime.orb.CameraEntity;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;

public final class CameraDirector {
    private static CameraDirector.Mode mode = CameraDirector.Mode.NONE;
    private static CameraEntity cam;
    private static CameraType savedType;
    private static final CutsceneDoc.Pose current = new CutsceneDoc.Pose();
    private static final CutsceneDoc.Pose shown = new CutsceneDoc.Pose();
    private static long lastFrame = System.nanoTime();
    private static final CutsceneDoc.Pose from = new CutsceneDoc.Pose();
    private static final CutsceneDoc.Pose to = new CutsceneDoc.Pose();
    private static double tweenStart;
    private static double tweenTime;
    private static String tweenEase = "smooth";
    private static int lookEntity = -1;
    private static Vec3 lookPoint;
    private static int targetEntity = -1;
    private static double distance = 4.0;
    private static double height = 1.5;
    private static double angle = 0.0;
    private static double radius = 5.0;
    private static double speed = 20.0;
    private static double orbitAngle;
    private static CutsceneDoc scene;
    private static String sceneName = "";
    private static int sceneScript = -1;
    private static double sceneStart;
    private static double sceneBegan;
    private static CutsceneDoc.Key holding;
    private static int holdsDone;
    private static double holdSince;
    private static boolean goOnEarly;
    private static final double PREVIEW_HOLD = 2.0;
    private static Vec3 origin = Vec3.ZERO;
    private static Map<String, Integer> looks = new HashMap<>();
    private static int marksDone;
    private static int clipsDone;
    private static final CutsceneDoc.Pose entry = new CutsceneDoc.Pose();
    private static BiConsumer<String, String> localEvents;
    private static double returnStart;
    private static double returnTime;
    private static final CutsceneDoc.Pose returnFrom = new CutsceneDoc.Pose();
    private static final CameraDirector.Tween bars = new CameraDirector.Tween();
    private static final CameraDirector.Tween fade = new CameraDirector.Tween();
    private static final CameraDirector.Tween fovAdd = new CameraDirector.Tween();
    private static final CameraDirector.Tween rollAdd = new CameraDirector.Tween();
    private static int fadeColor = -16777216;
    private static float trauma;
    private static float traumaDecay = 1.0F;
    private static boolean locked;
    private static boolean skippable;
    private static String hudHide = "none";
    private static float lockYaw;
    private static float lockPitch;
    private static String caption = "";
    private static double captionAt;
    private static final List<CutsceneDoc.Pose> dialogPath = new ArrayList<>();
    private static double dialogStart;
    private static double dialogTime;
    private static boolean dialogBack;
    private static boolean dialogLocked;
    private static String dialogHud = "none";
    public static double flySpeed = 8.0;
    public static boolean flyOverScreen;
    private static boolean reported;
    private static int reportIn;
    private static final Set<String> OWN_HUD = Set.of("health_overlay", "currency_overlay");

    public static void registerHooks() {
        ClientHooks.RENDER_PLAYER.add(CameraDirector::hideNearPlayer);
        ClientHooks.RENDER_TICK.add(CameraDirector::onRenderTick);
        ClientHooks.CAMERA_ANGLES.add(CameraDirector::onAngles);
        ClientHooks.COMPUTE_FOV.add(CameraDirector::onFov);
        ClientHooks.RENDER_HAND.add(CameraDirector::onHand);
        ClientHooks.MOVEMENT_INPUT.add(CameraDirector::onInput);
        ClientHooks.INTERACTION_KEY.add(CameraDirector::onInteract);
        ClientHooks.KEY.add(CameraDirector::onKey);
        ClientHooks.CLIENT_TICK.add(CameraDirector::onTick);
        ClientHooks.RENDER_HUD_PART.add(CameraDirector::onOverlay);
        ClientHooks.RENDER_GUI.add(CameraDirector::onGui);
        ClientHooks.LOGGING_OUT.add(CameraDirector::onLogout);
    }

    private CameraDirector() {
    }

    public static boolean hidesMinecraft() {
        return hudHide.equals("all") || hudHide.equals("minecraft");
    }

    public static boolean hidesSpraute() {
        return hudHide.equals("all") || hudHide.equals("classic");
    }

    public static boolean hidesCustom() {
        return hudHide.equals("all") || hudHide.equals("custom");
    }

    public static boolean fullyCovered() {
        return Math.round(fade.value() * 255.0) == 255L;
    }

    private static double now() {
        return System.nanoTime() / 1.0E9;
    }

    public static boolean dialogArrived() {
        return mode != CameraDirector.Mode.DIALOG || dialogBack || now() - dialogStart >= dialogTime;
    }

    private static double flightSeconds(double len) {
        double capped = Math.min(len, 20.0);
        return Math.max(1, (int)(capped * Math.exp(-0.05 * capped) * 5.0)) / 20.0;
    }

    public static void dialogFlyTo(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (entity(entityId) instanceof LivingEntity e && pl != null && mode != CameraDirector.Mode.SCENE) {
            boolean wasActive = mode != CameraDirector.Mode.NONE;
            CutsceneDoc.Pose init = new CutsceneDoc.Pose();
            if (wasActive) {
                init.set(current);
            } else {
                playerPose(init, 1.0F);
                Vec3 ahead = pl.getLookAngle().scale(0.25);
                init.x = init.x + ahead.x;
                init.y = init.y + ahead.y;
                init.z = init.z + ahead.z;
            }

            Vec3 look = e.getViewVector(1.0F);
            Vec3 horizontal = look.multiply(1.0, 0.0, 1.0).normalize();
            Vec3 angle = look.scale(1.6);
            float viewYRot = e.getViewYRot(0.0F);
            float viewXRot = e.getViewXRot(0.0F);
            Vec3 side = horizontal.multiply(0.1, 0.0, 0.1).yRot((float)Math.toRadians(90.0));
            Vec3 eye = e.position().add(0.0, e.getEyeHeight(), 0.0);
            CutsceneDoc.Pose end = pose(eye.add(angle).add(side), viewYRot + 165.0F, -viewXRot, init.fov);
            double len = Math.sqrt(sq(end.x - init.x) + sq(end.y - init.y) + sq(end.z - init.z));
            if (!(len <= 0.05)) {
                dialogPath.clear();
                dialogPath.add(init);
                Vec3 between = new Vec3(end.x - init.x, 0.0, end.z - init.z).normalize();
                if (between.dot(horizontal) > 0.75) {
                    Vec3 feet = e.position();
                    Vec3 a = horizontal.yRot((float)Math.toRadians(-90.0)).add(feet);
                    Vec3 b = horizontal.yRot((float)Math.toRadians(90.0)).add(feet);
                    double eyeUp = e.getEyeHeight();
                    Vec3 initFeet = new Vec3(init.x, init.y - pl.getEyeHeight(), init.z);
                    if (initFeet.distanceTo(a) > initFeet.distanceTo(b)) {
                        dialogPath.add(pose(b.add(0.0, eyeUp, 0.0), viewYRot + 90.0F, -viewXRot, init.fov));
                    } else {
                        dialogPath.add(pose(a.add(0.0, eyeUp, 0.0), viewYRot - 90.0F, -viewXRot, init.fov));
                    }
                }

                dialogPath.add(end);
                if (!wasActive) {
                    dialogHud = hudHide;
                    dialogLocked = locked;
                    activate();
                }

                hudHide = "all";
                locked = true;
                dialogBack = false;
                dialogStart = now();
                dialogTime = flightSeconds(len);
                mode = CameraDirector.Mode.DIALOG;
            }
        }
    }

    public static void hideNearPlayer(ClientHooks.RenderPlayer e) {
        if (mode != CameraDirector.Mode.NONE && mode != CameraDirector.Mode.FLY) {
            Minecraft mc = Minecraft.getInstance();
            if (e.getEntity() == mc.player) {
                Vec3 eye = mc.player.getEyePosition(e.getPartialTick());
                double dx = current.x - eye.x;
                double dy = current.y - eye.y;
                double dz = current.z - eye.z;
                if (dx * dx + dy * dy + dz * dz < 2.5600000000000005) {
                    e.setCanceled(true);
                }
            }
        }
    }

    public static void dialogFlyBack() {
        if (mode == CameraDirector.Mode.DIALOG) {
            CutsceneDoc.Pose init = new CutsceneDoc.Pose().set(current);
            CutsceneDoc.Pose end = new CutsceneDoc.Pose();
            playerPose(end, 1.0F);
            double len = Math.sqrt(sq(end.x - init.x) + sq(end.y - init.y) + sq(end.z - init.z));
            dialogPath.clear();
            dialogPath.add(init);
            dialogPath.add(end);
            dialogBack = true;
            dialogStart = now();
            dialogTime = len <= 0.05 ? 0.0 : flightSeconds(len);
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    private static CutsceneDoc.Pose pose(Vec3 p, float yaw, float pitch, float fov) {
        CutsceneDoc.Pose o = new CutsceneDoc.Pose();
        o.x = p.x;
        o.y = p.y;
        o.z = p.z;
        o.yaw = yaw;
        o.pitch = pitch;
        o.fov = fov;
        return o;
    }

    private static double catmull(double p0, double p1, double p2, double p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * (2.0 * p1 + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3);
    }

    private static CutsceneDoc.Pose dialogAt(double k) {
        int n = dialogPath.size();
        double u = Math.max(0.0, Math.min(1.0, k)) * (n - 1);
        int i = Math.min(n - 2, (int)Math.floor(u));
        double f = u - i;
        CutsceneDoc.Pose p0 = dialogPath.get(Math.max(0, i - 1));
        CutsceneDoc.Pose p1 = dialogPath.get(i);
        CutsceneDoc.Pose p2 = dialogPath.get(i + 1);
        CutsceneDoc.Pose p3 = dialogPath.get(Math.min(n - 1, i + 2));
        CutsceneDoc.Pose o = new CutsceneDoc.Pose().set(p1);
        o.x = catmull(p0.x, p1.x, p2.x, p3.x, f);
        o.y = catmull(p0.y, p1.y, p2.y, p3.y, f);
        o.z = catmull(p0.z, p1.z, p2.z, p3.z, f);
        o.yaw = p1.yaw + (float)(CutsceneDoc.wrap(p2.yaw - p1.yaw) * f);
        o.pitch = p1.pitch + (float)((p2.pitch - p1.pitch) * f);
        o.fov = p1.fov + (float)((p2.fov - p1.fov) * f);
        return o;
    }

    private static double easeInOutQuad(double x) {
        return x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0;
    }

    public static boolean active() {
        return mode != CameraDirector.Mode.NONE;
    }

    public static boolean inCutscene() {
        return mode == CameraDirector.Mode.SCENE;
    }

    public static void startFly(CutsceneDoc.Pose start) {
        activate();
        if (cam != null) {
            if (start != null) {
                current.set(start);
            }

            current.roll = 0.0F;
            current.fov = Minecraft.getInstance().options.fov().get().intValue();
            LocalPlayer p = Minecraft.getInstance().player;
            if (p != null) {
                p.setYRot(current.yaw);
                p.setXRot(current.pitch);
                p.yRotO = current.yaw;
                p.xRotO = current.pitch;
            }

            mode = CameraDirector.Mode.FLY;
            locked = true;
            hudHide = "all";
            bars.set(0.0);
        }
    }

    public static boolean flyingOrPreview() {
        return mode == CameraDirector.Mode.FLY || mode == CameraDirector.Mode.SCENE;
    }

    public static boolean flying() {
        return mode == CameraDirector.Mode.FLY;
    }

    public static void flyTo(CutsceneDoc.Pose p) {
        if (mode == CameraDirector.Mode.FLY) {
            current.set(p);
            current.roll = 0.0F;
            LocalPlayer pl = Minecraft.getInstance().player;
            if (pl != null) {
                pl.setYRot(p.yaw);
                pl.setXRot(p.pitch);
                pl.yRotO = p.yaw;
                pl.xRotO = p.pitch;
            }
        }
    }

    public static void flyAs(CutsceneDoc.Pose p) {
        if (mode == CameraDirector.Mode.FLY) {
            flyTo(p);
            current.roll = p.roll;
            current.fov = p.fov;
        }
    }

    public static void flyFree() {
        if (mode == CameraDirector.Mode.FLY) {
            current.roll = 0.0F;
            current.fov = Minecraft.getInstance().options.fov().get().intValue();
        }
    }

    public static void stopFly() {
        if (mode == CameraDirector.Mode.FLY) {
            locked = false;
            hudHide = "none";
            deactivate();
        }
    }

    public static CutsceneDoc.Pose pose() {
        return new CutsceneDoc.Pose().set(current);
    }

    private static boolean down(KeyMapping k) {
        Key key = ClientPlatform.get().boundKey(k);
        long win = Minecraft.getInstance().getWindow().getWindow();
        return key.getType() == Type.KEYSYM && InputConstants.isKeyDown(win, key.getValue());
    }

    private static void activate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            if (cam == null || Mc.level(cam) != mc.level) {
                cam = new CameraEntity(Orbs.CAMERA.get(), mc.level);
            }

            if (mode == CameraDirector.Mode.NONE) {
                playerPose(current, 1.0F);
                savedType = mc.options.getCameraType();
                lockYaw = mc.player.getYRot();
                lockPitch = mc.player.getXRot();
            }

            mc.options.setCameraType(CameraType.FIRST_PERSON);
            mc.setCameraEntity(cam);
        }
    }

    private static void deactivate() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getCameraEntity() == cam) {
            mc.setCameraEntity(mc.player);
        }

        if (savedType != null) {
            mc.options.setCameraType(savedType);
        }

        savedType = null;
        mode = CameraDirector.Mode.NONE;
        lookEntity = -1;
        lookPoint = null;
        targetEntity = -1;
        caption = "";
    }

    private static void playerPose(CutsceneDoc.Pose p, float partial) {
        LocalPlayer pl = Minecraft.getInstance().player;
        if (pl != null) {
            Vec3 e = pl.getEyePosition(partial);
            p.x = e.x;
            p.y = e.y;
            p.z = e.z;
            p.yaw = pl.getViewYRot(partial);
            p.pitch = pl.getViewXRot(partial);
            p.roll = 0.0F;
            p.fov = Minecraft.getInstance().options.fov().get().intValue();
            p.shake = 0.0F;
        }
    }

    public static void play(int script, String name, CutsceneDoc doc, Vec3 origin, Map<String, Integer> looks, BiConsumer<String, String> local) {
        if (doc.keys.isEmpty()) {
            report(script, "end", name, local);
        } else {
            activate();
            if (cam != null) {
                entry.set(current);
                scene = doc;
                sceneName = name;
                sceneScript = script;
                localEvents = local;
                CameraDirector.origin = doc.relative ? origin : Vec3.ZERO;
                CameraDirector.looks = looks;
                sceneStart = now();
                sceneBegan = sceneStart;
                holding = null;
                holdsDone = 0;
                goOnEarly = false;
                marksDone = 0;
                clipsDone = 0;
                mode = CameraDirector.Mode.SCENE;
                skippable = doc.skippable;
                locked = doc.lockPlayer;
                hudHide = doc.hideMode();
                bars.go(doc.bars, 0.6, "ease_out_cubic");
                fadeColor = GuiDoc.toColor(doc.fadeColor) | 0xFF000000;
                if (doc.fadeIn > 0.0F) {
                    fade.set(1.0);
                }
            }
        }
    }

    public static void stop(boolean skipped) {
        if (mode == CameraDirector.Mode.SCENE) {
            endScene(skipped);
        } else {
            returnToPlayer(0.6);
        }
    }

    private static void endScene(boolean skipped) {
        String name = sceneName;
        int script = sceneScript;
        BiConsumer<String, String> local = localEvents;
        CutsceneDoc doc = scene;
        scene = null;
        holding = null;
        localEvents = null;
        bars.go(0.0, 0.5, "ease_in_cubic");
        hudHide = "none";
        locked = false;
        if (doc != null && doc.fadeOut > 0.0F) {
            fade.go(0.0, Math.max(0.2, (double)doc.fadeOut), "smooth");
        }

        PostEngine.remove("cutscene", doc != null ? doc.blendOut : 0.5);
        returnToPlayer(skipped ? 0.4 : (doc != null ? doc.blendOut : 0.6));
        if (skipped) {
            report(script, "skip", name, local);
        }

        report(script, "end", name, local);
    }

    private static void goOn() {
        sceneStart = now() - holding.t;
        holding = null;
        holdsDone++;
    }

    private static void report(int script, String kind, String name, BiConsumer<String, String> local) {
        if (local != null) {
            local.accept(kind, name);
        } else if (script >= 0 && Minecraft.getInstance().getConnection() != null) {
            SprauteNetwork.sendToServer(new Packets.CameraEvent(script, kind, name));
        }
    }

    private static void returnToPlayer(double seconds) {
        if (mode != CameraDirector.Mode.NONE) {
            returnFrom.set(current);
            returnStart = now();
            returnTime = Math.max(0.01, seconds);
            mode = CameraDirector.Mode.RETURN;
            lookEntity = -1;
            lookPoint = null;
            caption = "";
        }
    }

    public static void op(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String op = o.get("op").getAsString();
        if (op.equals("rig")) {
            PlayerCam.op(o);
        } else {
            double time = num(o, "time", 0.0);
            String ease = o.has("ease") ? o.get("ease").getAsString() : "smooth";
            switch (op) {
                case "at":
                    activate();
                    if (mode == CameraDirector.Mode.SCENE) {
                        return;
                    }

                    from.set(current);
                    to.set(current);
                    to.x = num(o, "x", current.x);
                    to.y = num(o, "y", current.y);
                    to.z = num(o, "z", current.z);
                    if (o.has("yaw")) {
                        to.yaw = (float)num(o, "yaw", 0.0);
                    }

                    if (o.has("pitch")) {
                        to.pitch = (float)num(o, "pitch", 0.0);
                    }

                    if ((o.has("yaw") || o.has("pitch")) && !o.has("lookEntity") && !o.has("lookX")) {
                        lookEntity = -1;
                        lookPoint = null;
                    }

                    tween(time, ease);
                    setLook(o);
                    mode = CameraDirector.Mode.FREE;
                    break;
                case "turn":
                    activate();
                    if (mode == CameraDirector.Mode.SCENE) {
                        return;
                    }

                    from.set(current);
                    to.set(current);
                    to.yaw = (float)num(o, "yaw", current.yaw);
                    to.pitch = (float)num(o, "pitch", current.pitch);
                    lookEntity = -1;
                    lookPoint = null;
                    tween(time, ease);
                    if (mode != CameraDirector.Mode.FOLLOW && mode != CameraDirector.Mode.ORBIT) {
                        mode = CameraDirector.Mode.FREE;
                    }
                    break;
                case "look":
                    activate();
                    if (mode == CameraDirector.Mode.SCENE) {
                        return;
                    }

                    if (mode != CameraDirector.Mode.FOLLOW && mode != CameraDirector.Mode.ORBIT) {
                        from.set(current);
                        to.set(current);
                        tween(time, ease);
                        mode = CameraDirector.Mode.FREE;
                    }

                    setLook(o);
                    break;
                case "follow":
                case "orbit":
                    activate();
                    if (mode == CameraDirector.Mode.SCENE) {
                        return;
                    }

                    targetEntity = (int)num(o, "entity", -1.0);
                    distance = num(o, "distance", 4.0);
                    height = num(o, "height", 1.5);
                    angle = num(o, "angle", 0.0);
                    radius = num(o, "radius", 5.0);
                    speed = num(o, "speed", 20.0);
                    orbitAngle = current.yaw;
                    mode = op.equals("follow") ? CameraDirector.Mode.FOLLOW : CameraDirector.Mode.ORBIT;
                    break;
                case "reset":
                    returnToPlayer(Math.max(0.05, time));
                    break;
                case "stop":
                    stop(false);
                    break;
                case "continue":
                    if (mode != CameraDirector.Mode.SCENE) {
                        return;
                    }

                    if (holding != null) {
                        goOn();
                    } else {
                        goOnEarly = true;
                    }
                    break;
                case "fov":
                    fovAdd.go(num(o, "value", 70.0) - Minecraft.getInstance().options.fov().get().intValue(), time, ease);
                    break;
                case "roll":
                    rollAdd.go(num(o, "value", 0.0), time, ease);
                    break;
                case "shake":
                    trauma = (float)Math.max((double)trauma, Math.min(1.0, num(o, "value", 0.5)));
                    traumaDecay = (float)(time > 0.0 ? 1.0 / time : 1.0);
                    break;
                case "bars":
                    bars.go(Math.max(0.0, Math.min(0.45, num(o, "value", 0.11))), time > 0.0 ? time : 0.5, ease);
                    break;
                case "fade":
                    if (o.has("color")) {
                        fadeColor = GuiDoc.toColor(o.get("color").getAsString()) | 0xFF000000;
                    }

                    fade.go(Math.max(0.0, Math.min(1.0, num(o, "value", 1.0))), time, ease);
                    break;
                case "hud":
                    JsonPrimitive v = o.get("value").getAsJsonPrimitive();
                    hudHide = v.isBoolean()
                        ? (v.getAsBoolean() ? "none" : "all")
                        : (CutsceneDoc.HIDE_MODES.contains(v.getAsString()) ? v.getAsString() : "none");
                    break;
                case "lock":
                    locked = o.get("value").getAsBoolean();
                    if (locked && Minecraft.getInstance().player != null) {
                        lockYaw = Minecraft.getInstance().player.getYRot();
                        lockPitch = Minecraft.getInstance().player.getXRot();
                    }
            }
        }
    }

    private static void tween(double time, String ease) {
        tweenStart = now();
        tweenTime = time;
        tweenEase = ease;
    }

    private static void setLook(JsonObject o) {
        if (o.has("lookEntity")) {
            lookEntity = o.get("lookEntity").getAsInt();
            lookPoint = null;
        } else if (o.has("lookX")) {
            lookPoint = new Vec3(num(o, "lookX", 0.0), num(o, "lookY", 0.0), num(o, "lookZ", 0.0));
            lookEntity = -1;
        }
    }

    private static double num(JsonObject o, String k, double def) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : def;
    }

    private static Vec3 eyesOf(Entity e, float partial) {
        return e.getEyePosition(partial);
    }

    private static void lookAt(CutsceneDoc.Pose p, Vec3 target) {
        double dx = target.x - p.x;
        double dy = target.y - p.y;
        double dz = target.z - p.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        p.yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        p.pitch = (float)(-Math.toDegrees(Math.atan2(dy, h)));
    }

    private static Entity entity(int id) {
        Minecraft mc = Minecraft.getInstance();
        return id >= 0 && mc.level != null ? mc.level.getEntity(id) : null;
    }

    public static void onRenderTick(ClientHooks.RenderTick e) {
        if (e.phase == TickPhase.START) {
            double t = now();
            float dt = (float)Math.min(0.1, t - lastFrame / 1.0E9);
            lastFrame = System.nanoTime();
            if (mode == CameraDirector.Mode.NONE) {
                trauma = Math.max(0.0F, trauma - dt * traumaDecay);
            } else {
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null && mc.player != null && cam != null) {
                    float partial = e.renderTickTime;
                    CutsceneDoc.Pose d = new CutsceneDoc.Pose().set(current);
                    switch (mode) {
                        case SCENE:
                            double st = t - sceneStart;
                            if (holding == null) {
                                while (holdsDone < scene.keys.size() && !scene.keys.get(holdsDone).hold) {
                                    holdsDone++;
                                }

                                if (holdsDone < scene.keys.size() && st >= scene.keys.get(holdsDone).t) {
                                    if (goOnEarly) {
                                        goOnEarly = false;
                                        holdsDone++;
                                    } else {
                                        holding = scene.keys.get(holdsDone);
                                        holdSince = t;
                                        report(sceneScript, "hold", sceneName, localEvents);
                                    }
                                }
                            }

                            if (holding != null) {
                                st = holding.t;
                                double limit = holding.holdMax > 0.0F ? holding.holdMax : (localEvents != null ? 2.0 : 0.0);
                                if (limit > 0.0 && t - holdSince >= limit) {
                                    goOn();
                                }
                            }

                            CutsceneDoc.Pose p = scene.at(st);
                            p.x = p.x + origin.x;
                            p.y = p.y + origin.y;
                            p.z = p.z + origin.z;
                            CutsceneDoc.Key key = scene.keys.get(p.segment);
                            if (!key.look.isEmpty()) {
                                Entity le = (Entity)(key.look.equals("player") ? mc.player : entity(looks.getOrDefault(key.look, -1)));
                                if (le != null) {
                                    lookAt(p, eyesOf(le, partial));
                                }
                            }

                            String cap = !(st < scene.duration()) && holding == null ? "" : key.caption;
                            if (!cap.equals(caption)) {
                                captionAt = t;
                            }

                            for (caption = cap; marksDone < scene.keys.size() && (marksDone <= p.segment || st >= scene.duration()); marksDone++) {
                                String m = scene.keys.get(marksDone).mark;
                                if (!m.isEmpty()) {
                                    report(sceneScript, "mark", m, localEvents);
                                }
                            }

                            while (clipsDone < scene.clips.size() && scene.clips.get(clipsDone).t <= st) {
                                report(sceneScript, "clip", String.valueOf(clipsDone), localEvents);
                                clipsDone++;
                            }

                            double blend = scene.blendIn <= 0.0F ? 1.0 : Math.min(1.0, (t - sceneBegan) / scene.blendIn);
                            d.set(entry).lerp(p, Ease.apply("smooth", blend));
                            if (blend >= 1.0) {
                                d.set(p);
                            }

                            if (scene.fadeIn > 0.0F && st < scene.fadeIn + 0.05 && fade.toV == 1.0) {
                                fade.go(0.0, scene.fadeIn, "smooth");
                            }

                            if (holding == null && scene.fadeOut > 0.0F && st >= scene.duration() - scene.fadeOut && fade.toV == 0.0) {
                                fade.go(1.0, scene.fadeOut, "smooth");
                            }

                            if (holding == null && st >= scene.duration()) {
                                endScene(false);
                            }
                            break;
                        case FREE: {
                            double kxx = tweenTime <= 0.0 ? 1.0 : Math.min(1.0, (t - tweenStart) / tweenTime);
                            d.set(from).lerp(to, Ease.apply(tweenEase, kxx));
                            Vec3 target = lookTarget(partial);
                            if (target != null) {
                                CutsceneDoc.Pose look = new CutsceneDoc.Pose().set(d);
                                lookAt(look, target);
                                double lk = tweenTime <= 0.0 ? 1.0 : Math.min(1.0, (t - tweenStart) / Math.max(0.3, tweenTime));
                                d.yaw = from.yaw + CutsceneDoc.wrap(look.yaw - from.yaw) * (float)Ease.apply(tweenEase, lk);
                                d.pitch = from.pitch + (look.pitch - from.pitch) * (float)Ease.apply(tweenEase, lk);
                                if (lk >= 1.0) {
                                    d.yaw = look.yaw;
                                    d.pitch = look.pitch;
                                }
                            }
                            break;
                        }
                        case FOLLOW:
                        case ORBIT: {
                            Entity te = entity(targetEntity);
                            if (te == null) {
                                te = mc.player;
                            }

                            Vec3 base = te.getPosition(partial);
                            CutsceneDoc.Pose want = new CutsceneDoc.Pose().set(current);
                            double a;
                            double r;
                            if (mode == CameraDirector.Mode.ORBIT) {
                                orbitAngle = orbitAngle + speed * dt;
                                a = Math.toRadians(orbitAngle);
                                r = radius;
                            } else {
                                a = Math.toRadians(te.getViewYRot(partial) + angle);
                                r = distance;
                            }

                            want.x = base.x + Math.sin(a) * r;
                            want.z = base.z - Math.cos(a) * r;
                            want.y = base.y + height;
                            Vec3 target = lookTarget(partial);
                            lookAt(want, target != null ? target : eyesOf(te, partial));
                            double kxx = 1.0 - Math.exp(-dt * (mode == CameraDirector.Mode.ORBIT ? 12 : 5));
                            d.set(current).lerp(want, kxx);
                            break;
                        }
                        case FLY:
                            LocalPlayer plx = mc.player;
                            d.yaw = plx.getViewYRot(partial);
                            d.pitch = plx.getViewXRot(partial);
                            if (mc.screen == null || flyOverScreen) {
                                Options o = mc.options;
                                double f = (down(o.keyUp) ? 1 : 0) - (down(o.keyDown) ? 1 : 0);
                                double s = (down(o.keyRight) ? 1 : 0) - (down(o.keyLeft) ? 1 : 0);
                                double u = (down(o.keyJump) ? 1 : 0) - (down(o.keyShift) ? 1 : 0);
                                double sp = flySpeed * dt * (down(o.keySprint) ? 3 : 1);
                                double ya = Math.toRadians(d.yaw);
                                double pa = Math.toRadians(d.pitch);
                                double fx = -Math.sin(ya) * Math.cos(pa);
                                double fy = -Math.sin(pa);
                                double fz = Math.cos(ya) * Math.cos(pa);
                                double rx = -Math.cos(ya);
                                double rz = -Math.sin(ya);
                                d.x += (fx * f + rx * s) * sp;
                                d.y += (fy * f + u) * sp;
                                d.z += (fz * f + rz * s) * sp;
                            }
                            break;
                        case DIALOG:
                            double kx = dialogTime <= 0.0 ? 1.0 : Math.min(1.0, (t - dialogStart) / dialogTime);
                            if (dialogBack) {
                                playerPose(dialogPath.get(dialogPath.size() - 1), partial);
                            }

                            d.set(dialogAt(easeInOutQuad(kx)));
                            if (dialogBack && kx >= 1.0) {
                                hudHide = dialogHud;
                                locked = dialogLocked;
                                deactivate();
                                return;
                            }
                            break;
                        case RETURN:
                            CutsceneDoc.Pose pl = new CutsceneDoc.Pose();
                            playerPose(pl, partial);
                            double k = Math.min(1.0, (t - returnStart) / returnTime);
                            d.set(returnFrom).lerp(pl, Ease.apply("smooth", k));
                            if (k >= 1.0) {
                                deactivate();
                                return;
                            }
                    }

                    current.set(d);
                    shown.set(d);
                    shown.fov = (float)(d.fov + fovAdd.value());
                    shown.roll = (float)(d.roll + rollAdd.value());
                    trauma = Math.max(0.0F, trauma - dt * traumaDecay);
                    float s = Math.max(trauma, d.shake);
                    if (s > 0.0F) {
                        double tt = t * 18.0;
                        float kxxx = s * s;
                        shown.yaw = shown.yaw + (float)(noise(tt, 1) * 4.0 * kxxx);
                        shown.pitch = shown.pitch + (float)(noise(tt, 2) * 3.0 * kxxx);
                        shown.roll = shown.roll + (float)(noise(tt, 3) * 3.0 * kxxx);
                    }

                    cam.setPos(shown.x, shown.y, shown.z);
                    cam.xo = cam.xOld = shown.x;
                    cam.yo = cam.yOld = shown.y;
                    cam.zo = cam.zOld = shown.z;
                    cam.setYRot(shown.yaw);
                    cam.setXRot(shown.pitch);
                    cam.yRotO = shown.yaw;
                    cam.xRotO = shown.pitch;
                    if (locked && mode != CameraDirector.Mode.FLY) {
                        holdPlayer();
                    }
                } else {
                    deactivate();
                }
            }
        }
    }

    private static Vec3 lookTarget(float partial) {
        if (lookEntity >= 0) {
            Entity le = entity(lookEntity);
            if (le != null) {
                return eyesOf(le, partial);
            }
        }

        return lookPoint;
    }

    private static double noise(double t, int seed) {
        return (Math.sin(t * 1.13 + seed * 7.1) + Math.sin(t * 2.71 + seed * 3.3) * 0.5 + Math.sin(t * 0.61 + seed) * 0.7) / 2.2;
    }

    public static boolean locksView() {
        return locked && mode != CameraDirector.Mode.FLY && Minecraft.getInstance().screen == null;
    }

    private static void holdPlayer() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            p.setYRot(lockYaw);
            p.setXRot(lockPitch);
            p.yRotO = lockYaw;
            p.xRotO = lockPitch;
        }
    }

    public static void onAngles(ClientHooks.CameraAngles e) {
        if (mode != CameraDirector.Mode.NONE) {
            e.setYaw(shown.yaw);
            e.setPitch(shown.pitch);
            e.setRoll(shown.roll);
        }
    }

    public static void onFov(ClientHooks.ComputeFov e) {
        if (mode != CameraDirector.Mode.NONE) {
            e.setFOV(Math.max(5.0F, Math.min(170.0F, shown.fov)));
        } else if (fovAdd.value() != 0.0) {
            e.setFOV(e.getFOV() + fovAdd.value());
        }
    }

    public static void onHand(ClientHooks.RenderHand e) {
        if (mode != CameraDirector.Mode.NONE) {
            e.setCanceled(true);
        }
    }

    public static void onInput(ClientHooks.MovementInput e) {
        if (locked) {
            Input in = e.getInput();
            in.forwardImpulse = 0.0F;
            in.leftImpulse = 0.0F;
            in.up = in.down = in.left = in.right = false;
            in.jumping = false;
            in.shiftKeyDown = false;
        }
    }

    public static void onInteract(ClientHooks.InteractionKey e) {
        if (locked) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    public static void onKey(ClientHooks.Key e) {
        if (mode == CameraDirector.Mode.SCENE && e.getAction() == 1 && Minecraft.getInstance().screen == null) {
            if (e.getKey() == 32 || e.getKey() == 257) {
                if (holding != null && holding.holdPress) {
                    goOn();
                } else if (skippable) {
                    endScene(true);
                }
            }
        }
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END && locked && mode != CameraDirector.Mode.FLY) {
            holdPlayer();
        }

        if (e.phase == TickPhase.END) {
            reportToServer();
        }
    }

    private static void reportToServer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            boolean on = mode != CameraDirector.Mode.NONE && mode != CameraDirector.Mode.FLY;
            if (on || reported) {
                if (!on || --reportIn <= 0) {
                    reportIn = 5;
                    reported = on;
                    SprauteNetwork.sendToServer(new Packets.CameraPos(current.x, current.y, current.z, on));
                }
            }
        }
    }

    public static void onOverlay(ClientHooks.RenderHudPart e) {
        boolean cinema = mode != CameraDirector.Mode.NONE;
        if (!hudHide.equals("none") || cinema || fullyCovered()) {
            ResourceLocation id = e.id();
            if (id.getNamespace().equals("minecraft")) {
                String p = id.getPath();
                if (p.equals("chat_panel") && fullyCovered()) {
                    e.setCanceled(true);
                    return;
                }

                boolean crosshair = p.equals("crosshair");
                if (crosshair
                    ? mode != CameraDirector.Mode.FLY || hidesMinecraft()
                    : hidesMinecraft() && !p.equals("title_text") && !p.equals("subtitles") && !p.equals("chat_panel")) {
                    e.setCanceled(true);
                }
            } else if (id.getNamespace().equals("spraute_engine") && hidesSpraute() && OWN_HUD.contains(id.getPath())) {
                e.setCanceled(true);
            }
        }
    }

    public static void onGui(ClientHooks.RenderGui e) {
        Gfx g = e.getGuiGraphics();
        int w = e.getWindow().getGuiScaledWidth();
        int h = e.getWindow().getGuiScaledHeight();
        Minecraft mc = Minecraft.getInstance();
        double b = bars.value();
        if (b > 0.001) {
            int bh = (int)Math.round(h * b);
            g.fill(0, 0, w, bh, -16777216);
            g.fill(0, h - bh, w, h, -16777216);
        }

        if (!caption.isEmpty()) {
            float a = (float)Math.min(1.0, (now() - captionAt) / 0.3);
            int alpha = Math.max(8, Math.round(a * 255.0F));
            int y = h - (int)Math.round(h * Math.max(b, 0.04)) - 14;
            List<FormattedCharSequence> lines = mc.font.split(Component.literal(caption), Math.min(w - 40, 360));
            y -= (lines.size() - 1) * 10;

            for (FormattedCharSequence line : lines) {
                int lw = mc.font.width(line);
                g.drawString(mc.font, line, (w - lw) / 2, y, alpha << 24 | 16777215, true);
                y += 10;
            }
        }

        double f = fade.value();
        if (f > 0.001) {
            g.fill(0, 0, w, h, (int)Math.round(f * 255.0) << 24 | fadeColor & 16777215);
        }

        boolean pressToGo = holding != null && holding.holdPress;
        if (mode == CameraDirector.Mode.SCENE && (skippable || pressToGo)) {
            String hint = pressToGo ? "Пробел — дальше" : "Пробел — пропустить";
            g.drawString(mc.font, hint, w - mc.font.width(hint) - 6, h - 12, -1996488705, false);
        }
    }

    public static void onLogout(ClientHooks.LoggingOut e) {
        scene = null;
        deactivate();
        bars.set(0.0);
        fade.set(0.0);
        fovAdd.set(0.0);
        rollAdd.set(0.0);
        trauma = 0.0F;
        hudHide = "none";
        locked = false;
        cam = null;
    }

    private static enum Mode {
        NONE,
        FREE,
        FOLLOW,
        ORBIT,
        SCENE,
        RETURN,
        FLY,
        DIALOG;
    }

    private static final class Tween {
        double fromV;
        double toV;
        double start;
        double time;
        String ease = "smooth";

        void go(double target, double seconds, String e) {
            this.fromV = this.value();
            this.toV = target;
            this.start = CameraDirector.now();
            this.time = seconds;
            this.ease = e == null ? "smooth" : e;
        }

        void set(double v) {
            this.fromV = this.toV = v;
            this.time = 0.0;
        }

        double value() {
            if (this.time <= 0.0) {
                return this.toV;
            } else {
                double k = (CameraDirector.now() - this.start) / this.time;
                return k >= 1.0 ? this.toV : this.fromV + (this.toV - this.fromV) * Ease.apply(this.ease, k);
            }
        }
    }
}
