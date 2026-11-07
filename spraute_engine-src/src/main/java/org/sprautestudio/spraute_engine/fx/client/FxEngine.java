package org.sprautestudio.spraute_engine.fx.client;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.runtime.models.BoneWorld;

public final class FxEngine {
    private static final int MAX_TOTAL = 40000;
    private static final Random RND = new Random();
    private static final List<FxEngine.Inst> INSTANCES = new ArrayList<>();
    private static long lastFrame = System.nanoTime();
    private static int total;
    private static final FxEngine.P[] POOL = new FxEngine.P[40000];
    private static int pooled;
    private static final Map<String, FxEngine.Batch> ALPHA = new HashMap<>();
    private static final Map<String, FxEngine.Batch> ADDITIVE = new HashMap<>();
    private static final double SLOW_FAR_SQR = 4096.0;
    private static final double SLOWEST_FAR_SQR = 16384.0;
    private static final float TOO_SMALL_SQR = 2380000.0F;
    private static float viewYaw;
    private static float viewPitch;
    private static float viewRoll;
    private static final float[] PLANES = new float[16];
    private static final List<FxEngine.Inst> PREVIEW = new ArrayList<>();
    private static long previewFrame = System.nanoTime();
    private static final float DEG = (float) (Math.PI / 180.0);
    private static final Vector3f SCRATCH_A = new Vector3f();
    private static final Vector3f SCRATCH_B = new Vector3f();
    private static float p00;
    private static float p10;
    private static float p20;
    private static float p30;
    private static float p01;
    private static float p11;
    private static float p21;
    private static float p31;
    private static float p02;
    private static float p12;
    private static float p22;
    private static float p32;

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(FxEngine::onRenderLevel);
        ClientHooks.CAMERA_ANGLES.add(Priority.LOWEST, FxEngine::onAnglesFinal);
        ClientHooks.RENDER_GUI.add(FxEngine::onGui);
        ClientHooks.CAMERA_ANGLES.add(Priority.LOW, FxEngine::onAngles);
        ClientHooks.LOGGING_OUT.add(FxEngine::onLogout);
    }

    private FxEngine() {
    }

    private static FxEngine.P newParticle() {
        if (pooled == 0) {
            return new FxEngine.P();
        } else {
            FxEngine.P p = POOL[--pooled];
            p.age = 0.0F;
            p.color = -1;
            return p;
        }
    }

    private static void free(FxEngine.P p) {
        if (pooled < POOL.length) {
            POOL[pooled++] = p;
        }
    }

    private static float layerTime(FxDef.Layer l, FxDef d) {
        if (l.time > 0.0F) {
            return l.time;
        } else {
            return d.duration > 0.0F ? Math.max(0.0F, d.duration - l.delay) : Float.MAX_VALUE;
        }
    }

    public static void play(String id, String project, FxDef def, double x, double y, double z, int entity, float scale) {
        play(id, project, def, x, y, z, entity, scale, -1);
    }

    public static void play(String id, String project, FxDef def, double x, double y, double z, int entity, float scale, int scriptId) {
        play(id, project, def, x, y, z, entity, scale, scriptId, "");
    }

    public static void play(String id, String project, FxDef def, double x, double y, double z, int entity, float scale, int scriptId, String bone) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            if (id != null && !id.isEmpty()) {
                stop(id, scriptId, true);
            }

            FxEngine.Inst in = new FxEngine.Inst(id == null ? "" : id, project, def, x, y, z, entity, Math.max(0.01F, scale));
            in.scriptId = scriptId;
            in.bone = bone == null ? "" : bone;
            follow(in, 1.0F);
            INSTANCES.add(in);
            if (def.sound != null && !def.sound.isBlank()) {
                ResourceLocation rl = ResourceLocation.tryParse(def.sound.contains(":") ? def.sound : "minecraft:" + def.sound);
                if (rl != null) {
                    mc.level.playLocalSound(in.ox, in.oy, in.oz, Mc.soundEvent(rl), SoundSource.AMBIENT, def.soundVolume, def.soundPitch, false);
                }
            }
        }
    }

    public static void launch(
        String id, String project, FxDef def, double x, double y, double z, double vx, double vy, double vz, double g, float scale, int scriptId
    ) {
        play(id, project, def, x, y, z, -1, scale, scriptId);
        if (!INSTANCES.isEmpty()) {
            FxEngine.Inst in = INSTANCES.get(INSTANCES.size() - 1);
            in.vx = vx;
            in.vy = vy;
            in.vz = vz;
            in.g = g;
            in.moving = true;
            in.flyLeft = 30.0F;
        }
    }

    public static void stop(String id, boolean now) {
        stop(id, -1, now);
    }

    public static void stop(String id, int scriptId, boolean now) {
        Iterator<FxEngine.Inst> it = INSTANCES.iterator();

        while (it.hasNext()) {
            FxEngine.Inst in = it.next();
            if ((scriptId < 0 || in.scriptId == scriptId) && (id == null || id.isEmpty() || in.id.equals(id))) {
                if (now) {
                    for (FxEngine.LayerState s : in.layers) {
                        total = total - s.ps.size();
                    }

                    it.remove();
                } else {
                    in.stopping = true;
                }
            }
        }
    }

    public static boolean running(String id) {
        for (FxEngine.Inst in : INSTANCES) {
            if (in.id.equals(id) && !in.stopping) {
                return true;
            }
        }

        return false;
    }

    public static int particles() {
        return total;
    }

    private static void follow(FxEngine.Inst in, float partial) {
        if (in.entity >= 0) {
            Minecraft mc = Minecraft.getInstance();
            Entity e = mc.level != null ? mc.level.getEntity(in.entity) : null;
            if (e != null) {
                Vec3 p = e.getPosition(partial);
                if (!in.bone.isEmpty()) {
                    try {
                        p = BoneWorld.position(e, in.bone, p, 0.0, 0.0, 0.0);
                    } catch (RuntimeException var6) {
                        in.bone = "";
                    }
                }

                in.ox = p.x;
                in.oy = p.y;
                in.oz = p.z;
            }
        }
    }

    private static float rnd(float spread) {
        return (RND.nextFloat() * 2.0F - 1.0F) * spread;
    }

    private static void emit(FxEngine.Inst in, FxEngine.LayerState s, int n) {
        FxDef.Layer l = s.l;
        float sc = in.scale;
        double evenStart = RND.nextDouble() * Math.PI * 2.0;

        for (int i = 0; i < n && s.ps.size() < l.max && total < 40000; i++) {
            FxEngine.P p = newParticle();
            double px = 0.0;
            double py = 0.0;
            double pz = 0.0;
            float r = l.radius * sc;
            float ri = Math.min(l.radiusInner, l.radius) * sc;
            String shape = l.shape;
            switch (shape) {
                case "sphere":
                case "shell":
                    Vector3f d = randomUnit();
                    double rrx = l.shape.equals("shell") ? r : Math.cbrt(ri * ri * ri + RND.nextDouble() * (r * r * r - ri * ri * ri));
                    px = d.x * rrx;
                    py = d.y * rrx;
                    pz = d.z * rrx;
                    break;
                case "disc":
                case "column": {
                    double a = RND.nextDouble() * Math.PI * 2.0;
                    double rr = Math.sqrt(ri * ri + RND.nextDouble() * (r * r - ri * ri));
                    px = Math.cos(a) * rr;
                    pz = Math.sin(a) * rr;
                    if (l.shape.equals("column")) {
                        py = RND.nextDouble() * l.height * sc;
                    }
                    break;
                }
                case "ring": {
                    double a = l.even ? evenStart + (Math.PI * 2) * i / n : RND.nextDouble() * Math.PI * 2.0;
                    px = Math.cos(a) * r;
                    pz = Math.sin(a) * r;
                    break;
                }
                case "box":
                    px = rnd(r);
                    py = RND.nextDouble() * l.height * sc;
                    pz = rnd(r);
                    break;
                case "line":
                    py = RND.nextDouble() * l.height * sc;
            }

            String direction = l.direction;
            Vector3f dir;
            switch (direction) {
                case "up":
                case "axis":
                    dir = new Vector3f(0.0F, 1.0F, 0.0F);
                    break;
                case "down":
                    dir = new Vector3f(0.0F, -1.0F, 0.0F);
                    break;
                case "random":
                    dir = randomUnit();
                    break;
                case "none":
                    dir = new Vector3f();
                    break;
                case "tangent":
                    dir = new Vector3f((float)(-pz), 0.0F, (float)px);
                    if (dir.lengthSquared() < 1.0E-6) {
                        dir = randomUnit();
                    }

                    dir.normalize();
                    break;
                case "cone":
                    dir = cone(l.angle);
                    break;
                default:
                    if (l.shape.equals("cone")) {
                        dir = cone(l.angle);
                    } else {
                        boolean flat = l.shape.equals("disc") || l.shape.equals("ring") || l.shape.equals("column");
                        dir = new Vector3f((float)px, flat ? 0.0F : (float)py, (float)pz);
                        if (dir.lengthSquared() < 1.0E-6) {
                            dir = randomUnit();
                        }

                        dir.normalize();
                    }
            }

            if (s.rot != null) {
                Vector3f turned = s.rot.transform(new Vector3f((float)px, (float)py, (float)pz));
                px = turned.x;
                py = turned.y;
                pz = turned.z;
                dir = s.rot.transform(new Vector3f(dir));
            }

            float sp = l.speed * sc * (1.0F + rnd(l.speedRandom));
            p.x = px + l.offsetX * sc;
            p.y = py + l.offsetY * sc;
            p.z = pz + l.offsetZ * sc;
            if (!l.local) {
                p.x = p.x + in.ox;
                p.y = p.y + in.oy;
                p.z = p.z + in.oz;
            }

            p.vx = dir.x * sp;
            p.vy = dir.y * sp;
            p.vz = dir.z * sp;
            Vector3f axis = !l.direction.equals("none") && !(dir.lengthSquared() < 1.0E-6) ? dir : new Vector3f(0.0F, 1.0F, 0.0F);
            p.dx = axis.x;
            p.dy = axis.y;
            p.dz = axis.z;
            p.life = Math.max(0.02F, l.life * (1.0F + rnd(l.lifeRandom)));
            p.sizeMul = 1.0F + rnd(l.sizeRandom);
            p.rot = RND.nextFloat() * 360.0F;
            p.spin = l.spin + rnd(l.spinRandom);
            p.seed = RND.nextFloat() * 1000.0F;
            if (l.colorRandom) {
                p.color = s.colors[RND.nextInt(s.colors.length)];
            }

            s.ps.add(p);
            total++;
        }
    }

    private static Vector3f randomUnit() {
        double z = RND.nextDouble() * 2.0 - 1.0;
        double a = RND.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(1.0 - z * z);
        return new Vector3f((float)(r * Math.cos(a)), (float)z, (float)(r * Math.sin(a)));
    }

    private static Vector3f cone(float angle) {
        double c = Math.cos(Math.toRadians(Math.max(0.0F, Math.min(180.0F, angle))));
        double z = c + RND.nextDouble() * (1.0 - c);
        double a = RND.nextDouble() * Math.PI * 2.0;
        double r = Math.sqrt(Math.max(0.0, 1.0 - z * z));
        return new Vector3f((float)(r * Math.cos(a)), (float)z, (float)(r * Math.sin(a)));
    }

    private static float wave(float seed, float t) {
        return (float)(Math.sin(t * 1.7 + seed) * 0.6 + Math.sin(t * 3.1 + seed * 1.3) * 0.4);
    }

    private static void update(FxEngine.Inst in, float dt) {
        in.t += dt;
        if (in.moving && !in.stopping) {
            in.ox = in.ox + in.vx * 20.0 * dt;
            in.oy = in.oy + in.vy * 20.0 * dt;
            in.oz = in.oz + in.vz * 20.0 * dt;
            in.vy = in.vy - in.g * 20.0 * dt;
            in.flyLeft -= dt;
            if (in.flyLeft <= 0.0F) {
                in.stopping = true;
            }
        }

        if (in.stopping) {
            in.fade -= dt / 0.6F;

            for (FxEngine.LayerState s : in.layers) {
                for (FxEngine.P p : s.ps) {
                    p.life = Math.min(p.life, p.age + 0.6F);
                }
            }
        }

        float sc = in.scale;

        for (FxEngine.LayerState s : in.layers) {
            FxDef.Layer l = s.l;
            double lt = in.t - l.delay;
            float lim = layerTime(l, in.def);
            if (l.type.equals("particles") && !in.stopping && lt >= 0.0 && (in.def.duration <= 0.0F || in.t < in.def.duration) && lt <= lim) {
                if (!s.burstDone && l.burst > 0) {
                    emit(in, s, l.burst);
                    s.burstDone = true;
                    s.nextBurst = lt + l.burstEvery;
                } else if (l.burstEvery > 0.0F && l.burst > 0 && lt >= s.nextBurst) {
                    emit(in, s, l.burst);
                    s.nextBurst = lt + l.burstEvery;
                }

                if (l.rate > 0.0F) {
                    s.accum = s.accum + l.rate * dt;
                    int n = (int)s.accum;
                    s.accum -= n;
                    emit(in, s, n);
                }
            }

            double cx = l.local ? 0.0 : in.ox;
            double cy = l.local ? 0.0 : in.oy;
            double cz = l.local ? 0.0 : in.oz;
            double orbit = Math.toRadians(l.orbit) * dt;
            double cosO = Math.cos(orbit);
            double sinO = Math.sin(orbit);
            float dragK = Math.max(0.0F, 1.0F - l.drag * dt);
            List<FxEngine.P> ps = s.ps;
            int count = ps.size();
            int alive = 0;

            for (int i = 0; i < count; i++) {
                FxEngine.P p = ps.get(i);
                p.age += dt;
                if (p.age >= p.life) {
                    free(p);
                    total--;
                } else {
                    if (alive != i) {
                        ps.set(alive, p);
                    }

                    alive++;
                    p.vy = p.vy - l.gravity * sc * dt;
                    if (l.turbulence != 0.0F) {
                        float k = l.turbulence * sc * dt;
                        p.vx = p.vx + wave(p.seed, p.age * 2.0F) * k;
                        p.vy = p.vy + wave(p.seed + 17.0F, p.age * 2.0F) * k * 0.6F;
                        p.vz = p.vz + wave(p.seed + 41.0F, p.age * 2.0F) * k;
                    }

                    if (l.attract != 0.0F) {
                        double rx = p.x - cx;
                        double ry = p.y - cy - l.offsetY * sc;
                        double rz = p.z - cz;
                        double len = Math.sqrt(rx * rx + ry * ry + rz * rz);
                        if (len > 1.0E-4) {
                            double k = l.attract * sc * dt / len;
                            p.vx -= rx * k;
                            p.vy -= ry * k;
                            p.vz -= rz * k;
                        }
                    }

                    p.vx *= dragK;
                    p.vy *= dragK;
                    p.vz *= dragK;
                    p.x = p.x + p.vx * dt;
                    p.y = p.y + p.vy * dt;
                    p.z = p.z + p.vz * dt;
                    if (orbit != 0.0 && s.axis != null) {
                        double ox = cx + l.offsetX * sc;
                        double oy = cy + l.offsetY * sc;
                        double oz = cz + l.offsetZ * sc;
                        Vector3f k = s.axis;
                        double rx = p.x - ox;
                        double ry = p.y - oy;
                        double rz = p.z - oz;
                        double dot = k.x * rx + k.y * ry + k.z * rz;
                        double crx = k.y * rz - k.z * ry;
                        double cry = k.z * rx - k.x * rz;
                        double crz = k.x * ry - k.y * rx;
                        p.x = ox + rx * cosO + crx * sinO + k.x * dot * (1.0 - cosO);
                        p.y = oy + ry * cosO + cry * sinO + k.y * dot * (1.0 - cosO);
                        p.z = oz + rz * cosO + crz * sinO + k.z * dot * (1.0 - cosO);
                        double vdot = k.x * p.vx + k.y * p.vy + k.z * p.vz;
                        double cvx = k.y * p.vz - k.z * p.vy;
                        double cvy = k.z * p.vx - k.x * p.vz;
                        double cvz = k.x * p.vy - k.y * p.vx;
                        double nvx = p.vx * cosO + cvx * sinO + k.x * vdot * (1.0 - cosO);
                        double nvy = p.vy * cosO + cvy * sinO + k.y * vdot * (1.0 - cosO);
                        p.vz = p.vz * cosO + cvz * sinO + k.z * vdot * (1.0 - cosO);
                        p.vx = nvx;
                        p.vy = nvy;
                    } else if (orbit != 0.0) {
                        double rx = p.x - cx;
                        double rz = p.z - cz;
                        p.x = cx + rx * cosO - rz * sinO;
                        p.z = cz + rx * sinO + rz * cosO;
                        double vx = p.vx * cosO - p.vz * sinO;
                        p.vz = p.vx * sinO + p.vz * cosO;
                        p.vx = vx;
                    }

                    p.rot = p.rot + p.spin * dt;
                }
            }

            if (alive < count) {
                ps.subList(alive, count).clear();
            }
        }
    }

    public static void onRenderLevel(ClientHooks.RenderLevel e) {
        if (e.getStage() == ClientHooks.LevelStage.AFTER_WEATHER) {
            long now = System.nanoTime();
            float dt = (float)Math.min(0.1, (now - lastFrame) / 1.0E9);
            lastFrame = now;
            Minecraft mc = Minecraft.getInstance();
            if (!INSTANCES.isEmpty() && mc.level != null) {
                if (mc.isPaused() && !(mc.screen instanceof SprauteScreen)) {
                    dt = 0.0F;
                }

                float partial = e.getPartialTick();
                Camera cam = e.getCamera();
                Vec3 camAt = cam.getPosition();
                Iterator<FxEngine.Inst> it = INSTANCES.iterator();

                while (it.hasNext()) {
                    FxEngine.Inst in = it.next();
                    follow(in, partial);
                    double ox = in.ox - camAt.x;
                    double oy = in.oy - camAt.y;
                    double oz = in.oz - camAt.z;
                    double far = ox * ox + oy * oy + oz * oz;
                    int every = far > 16384.0 ? 4 : (far > 4096.0 ? 2 : 1);
                    in.pendingDt += dt;
                    if (++in.frames % every == 0) {
                        update(in, in.pendingDt);
                        in.pendingDt = 0.0F;
                        if (in.finished()) {
                            for (FxEngine.LayerState s : in.layers) {
                                total = total - s.ps.size();

                                for (FxEngine.P p : s.ps) {
                                    free(p);
                                }
                            }

                            it.remove();
                        }
                    }
                }

                Matrix4f view = new Matrix4f()
                    .rotateZ((float)Math.toRadians(viewRoll))
                    .rotateX((float)Math.toRadians(viewPitch))
                    .rotateY((float)Math.toRadians(viewYaw + 180.0F));
                PoseStack mv = RenderSystem.getModelViewStack();
                mv.pushPose();
                mv.setIdentity();
                RenderSystem.applyModelViewMatrix();

                try {
                    drawAll(INSTANCES, camAt, Mc.cameraLeft(cam), Mc.cameraUp(cam), view, true, sidePlanes(e.getProjectionMatrix(), view));
                } finally {
                    mv.popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        }
    }

    public static void onAnglesFinal(ClientHooks.CameraAngles e) {
        viewYaw = e.getYaw();
        viewPitch = e.getPitch();
        viewRoll = e.getRoll();
    }

    private static float[] sidePlanes(Matrix4f projection, Matrix4f view) {
        if (projection == null) {
            return null;
        } else {
            Matrix4f m = new Matrix4f(projection).mul(view);

            for (int i = 0; i < 4; i++) {
                float sign = i % 2 == 0 ? 1.0F : -1.0F;
                float a;
                float b;
                float c;
                float d;
                if (i < 2) {
                    a = m.m03() + sign * m.m00();
                    b = m.m13() + sign * m.m10();
                    c = m.m23() + sign * m.m20();
                    d = m.m33() + sign * m.m30();
                } else {
                    a = m.m03() + sign * m.m01();
                    b = m.m13() + sign * m.m11();
                    c = m.m23() + sign * m.m21();
                    d = m.m33() + sign * m.m31();
                }

                float len = (float)Math.sqrt(a * a + b * b + c * c);
                if (!(len > 1.0E-6F) || Float.isNaN(d)) {
                    return null;
                }

                PLANES[i * 4] = a / len;
                PLANES[i * 4 + 1] = b / len;
                PLANES[i * 4 + 2] = c / len;
                PLANES[i * 4 + 3] = d / len;
            }

            return PLANES;
        }
    }

    private static boolean outside(float[] planes, float x, float y, float z, float radius) {
        for (int i = 0; i < 16; i += 4) {
            if (planes[i] * x + planes[i + 1] * y + planes[i + 2] * z + planes[i + 3] < -radius) {
                return true;
            }
        }

        return false;
    }

    private static void drawAll(List<FxEngine.Inst> instances, Vec3 cp, Vector3f left, Vector3f up, Matrix4f pose, boolean depth, float[] planes) {
        for (FxEngine.Batch b : ALPHA.values()) {
            b.n = 0;
        }

        for (FxEngine.Batch b : ADDITIVE.values()) {
            b.n = 0;
        }

        for (FxEngine.Inst in : instances) {
            float fade = Math.max(0.0F, in.fade);

            for (FxEngine.LayerState s : in.layers) {
                List<FxEngine.P> ps = s.ps;
                int count = ps.size();
                if (count != 0) {
                    FxDef.Layer l = s.l;
                    boolean additive = !l.blend.equals("alpha");
                    Map<String, FxEngine.Batch> group = additive ? ADDITIVE : ALPHA;
                    FxEngine.Batch batch = group.get(s.batch);
                    if (batch == null) {
                        batch = new FxEngine.Batch(s.batch);
                        group.put(s.batch, batch);
                    }

                    boolean sprite = !l.render.equals("beam") && !l.render.equals("stretch");
                    float reach = l.render.equals("beam") ? l.beamLength * in.scale : 0.0F;

                    for (int i = 0; i < count; i++) {
                        FxEngine.P p = ps.get(i);
                        float t = p.age / p.life;
                        float size = FxDef.curve(l.size, t) * p.sizeMul * in.scale;
                        float a = FxDef.curve(l.alpha, t) * fade;
                        if (!(size <= 0.0F) && !(a <= 0.002F)) {
                            p.drawSize = size;
                            p.drawAlpha = a;
                            if (planes != null) {
                                float x = (float)((l.local ? p.x + in.ox : p.x) - cp.x);
                                float y = (float)((l.local ? p.y + in.oy : p.y) - cp.y);
                                float z = (float)((l.local ? p.z + in.oz : p.z) - cp.z);
                                if (sprite && size * size * 2380000.0F < x * x + y * y + z * z) {
                                    continue;
                                }

                                float radius = size + reach + 0.5F;
                                if (l.render.equals("stretch")) {
                                    radius += (float)Math.sqrt(p.vx * p.vx + p.vy * p.vy + p.vz * p.vz) * l.stretch;
                                }

                                if (outside(planes, x, y, z, radius)) {
                                    continue;
                                }
                            }

                            batch.add(in, s, p);
                        }
                    }
                }
            }
        }

        RenderSystem.enableBlend();
        if (depth) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        try {
            RenderSystem.defaultBlendFunc();

            for (FxEngine.Batch b : ALPHA.values()) {
                drawGroup(b, cp, left, up, pose, false);
            }

            RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE);

            for (FxEngine.Batch b : ADDITIVE.values()) {
                drawGroup(b, cp, left, up, pose, true);
            }
        } finally {
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    public static void previewPlay(String project, FxDef def, float scale) {
        previewStop();
        PREVIEW.add(new FxEngine.Inst("preview", project, def, 0.0, 0.0, 0.0, -1, Math.max(0.01F, scale)));
    }

    public static void previewStop() {
        for (FxEngine.Inst in : PREVIEW) {
            for (FxEngine.LayerState s : in.layers) {
                total = total - s.ps.size();
            }
        }

        PREVIEW.clear();
    }

    public static boolean previewRunning() {
        return !PREVIEW.isEmpty();
    }

    public static double previewTime() {
        return PREVIEW.isEmpty() ? -1.0 : PREVIEW.get(0).t;
    }

    public static int previewParticles() {
        int n = 0;

        for (FxEngine.Inst in : PREVIEW) {
            for (FxEngine.LayerState s : in.layers) {
                n += s.ps.size();
            }
        }

        return n;
    }

    public static float previewExtent() {
        float m = 0.0F;

        for (FxEngine.Inst in : PREVIEW) {
            for (FxEngine.LayerState s : in.layers) {
                for (FxEngine.P p : s.ps) {
                    double d = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z) + FxDef.curve(s.l.size, p.age / p.life) * in.scale * 0.5;
                    if (d > m) {
                        m = (float)d;
                    }
                }
            }
        }

        return m;
    }

    public static void renderPreview(Gfx g, float cx, float groundY, float ppb, float yaw, float pitch, int vx, int vy, int vw, int vh, boolean paused) {
        long now = System.nanoTime();
        float dt = paused ? 0.0F : (float)Math.min(0.1, (now - previewFrame) / 1.0E9);
        previewFrame = now;
        Iterator<FxEngine.Inst> it = PREVIEW.iterator();

        while (it.hasNext()) {
            FxEngine.Inst in = it.next();
            update(in, dt);
            if (in.finished()) {
                for (FxEngine.LayerState s : in.layers) {
                    total = total - s.ps.size();
                }

                it.remove();
            }
        }

        float shake = screenOf(PREVIEW, "shake", null, false);
        if (shake > 0.0F) {
            double t = now / 1.0E9 * 22.0;
            float k = shake * shake;
            yaw += (float)(Math.sin(t * 1.13) + Math.sin(t * 2.71) * 0.5) * 3.5F * k;
            pitch += (float)(Math.sin(t * 1.37 + 2.0) + Math.sin(t * 3.3) * 0.5) * 2.5F * k;
        }

        Matrix4f rot = new Matrix4f().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw));
        Matrix4f inv = new Matrix4f(rot).invert();
        Vector3f right = inv.transformDirection(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f up = inv.transformDirection(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f toward = inv.transformDirection(new Vector3f(0.0F, 0.0F, 1.0F));
        Vec3 cp = new Vec3(toward.x * 2000.0F, toward.y * 2000.0F, toward.z * 2000.0F);
        Matrix4f m = new Matrix4f(Mc.pose(g.pose().last()))
            .translate(cx, groundY, 50.0F)
            .scale(ppb, -ppb, 0.001F)
            .mul(rot)
            .translate((float)cp.x, (float)cp.y, (float)cp.z);
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        grid(m, cp, ppb);
        person(m, cp, right);
        drawAll(PREVIEW, cp, new Vector3f(right).negate(), up, m, false, null);
        RenderSystem.enableDepthTest();
        float[] rgb = new float[3];
        float a = screenOf(PREVIEW, "flash", rgb, false);
        if (a > 0.003F) {
            int c = (int)(a * 255.0F) << 24 | (int)(rgb[0] * 255.0F) << 16 | (int)(rgb[1] * 255.0F) << 8 | (int)(rgb[2] * 255.0F);
            g.fill(vx, vy, vx + vw, vy + vh, c);
        }
    }

    private static void grid(Matrix4f m, Vec3 cp, float ppb) {
        int step = ppb >= 6.0F ? 1 : (ppb >= 1.5F ? 5 : 10);
        int n = step * 12;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.defaultBlendFunc();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = -n; i <= n; i += step) {
            int a = i == 0 ? 90 : (i % (step * 5) == 0 ? 55 : 30);
            line(bb, m, cp, i, -n, i, n, a);
            line(bb, m, cp, -n, i, n, i, a);
        }

        BufferUploader.drawWithShader(bb.end());
    }

    private static void line(BufferBuilder bb, Matrix4f m, Vec3 cp, float x0, float z0, float x1, float z1, int a) {
        Mc.vertex(bb, m, (float)(x0 - cp.x), (float)(-cp.y), (float)(z0 - cp.z)).color(255, 255, 255, a).endVertex();
        Mc.vertex(bb, m, (float)(x1 - cp.x), (float)(-cp.y), (float)(z1 - cp.z)).color(255, 255, 255, a).endVertex();
    }

    private static void person(Matrix4f m, Vec3 cp, Vector3f right) {
        Vector3f r = new Vector3f(right.x, 0.0F, right.z);
        if (r.lengthSquared() < 1.0E-6) {
            r.set(1.0F, 0.0F, 0.0F);
        }

        r.normalize();
        float px = -r.x * 2.2F;
        float pz = -r.z * 2.2F;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.defaultBlendFunc();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        box(bb, m, cp, px, pz, r, 0.3F, 0.0F, 1.35F, 150);
        box(bb, m, cp, px, pz, r, 0.22F, 1.4F, 1.85F, 170);
        BufferUploader.drawWithShader(bb.end());
    }

    private static void box(BufferBuilder bb, Matrix4f m, Vec3 cp, float px, float pz, Vector3f r, float hw, float y0, float y1, int a) {
        float ax = r.x * hw;
        float az = r.z * hw;
        Mc.vertex(bb, m, (float)(px - ax - cp.x), (float)(y0 - cp.y), (float)(pz - az - cp.z)).color(170, 176, 190, a).endVertex();
        Mc.vertex(bb, m, (float)(px + ax - cp.x), (float)(y0 - cp.y), (float)(pz + az - cp.z)).color(170, 176, 190, a).endVertex();
        Mc.vertex(bb, m, (float)(px + ax - cp.x), (float)(y1 - cp.y), (float)(pz + az - cp.z)).color(170, 176, 190, a).endVertex();
        Mc.vertex(bb, m, (float)(px - ax - cp.x), (float)(y1 - cp.y), (float)(pz - az - cp.z)).color(170, 176, 190, a).endVertex();
    }

    private static void drawGroup(FxEngine.Batch batch, Vec3 cp, Vector3f left, Vector3f up, Matrix4f pose, boolean additive) {
        int n = batch.n;
        if (n != 0) {
            if (batch.texture == null) {
                int bar = batch.key.indexOf(124);
                batch.texture = FxTextures.get(batch.key.substring(0, bar), batch.key.substring(bar + 1));
            }

            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, batch.texture);
            p00 = pose.m00();
            p10 = pose.m10();
            p20 = pose.m20();
            p30 = pose.m30();
            p01 = pose.m01();
            p11 = pose.m11();
            p21 = pose.m21();
            p31 = pose.m31();
            p02 = pose.m02();
            p12 = pose.m12();
            p22 = pose.m22();
            p32 = pose.m32();
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            if (additive) {
                for (int i = 0; i < n; i++) {
                    quad(bb, batch.insts[i], batch.layers[i], batch.particles[i], cp, left, up, true);
                }
            } else {
                if (batch.order.length < n) {
                    batch.order = new long[batch.particles.length];
                }

                long[] order = batch.order;

                for (int i = 0; i < n; i++) {
                    FxEngine.Inst in = batch.insts[i];
                    FxDef.Layer l = batch.layers[i].l;
                    FxEngine.P p = batch.particles[i];
                    float x = (float)((l.local ? p.x + in.ox : p.x) - cp.x);
                    float y = (float)((l.local ? p.y + in.oy : p.y) - cp.y);
                    float z = (float)((l.local ? p.z + in.oz : p.z) - cp.z);
                    order[i] = (long)(Integer.MAX_VALUE - Float.floatToRawIntBits(x * x + y * y + z * z)) << 32 | i;
                }

                Arrays.sort(order, 0, n);

                for (int k = 0; k < n; k++) {
                    int i = (int)order[k];
                    quad(bb, batch.insts[i], batch.layers[i], batch.particles[i], cp, left, up, false);
                }
            }

            BufferUploader.drawWithShader(bb.end());
        }
    }

    private static void quad(BufferBuilder bb, FxEngine.Inst in, FxEngine.LayerState state, FxEngine.P p, Vec3 cp, Vector3f left, Vector3f up, boolean additive) {
        FxDef.Layer l = state.l;
        float t = p.age / p.life;
        float size = p.drawSize;
        float a = p.drawAlpha;
        int rgb = p.color >= 0 ? p.color : FxDef.gradient(state.colors, t);
        float r = (rgb >> 16 & 0xFF) / 255.0F;
        float g = (rgb >> 8 & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float k = l.intensity;
        if (additive && k > 1.0F) {
            float w = Math.min(1.0F, (k - 1.0F) * 0.5F);
            r += (1.0F - r) * w;
            g += (1.0F - g) * w;
            b += (1.0F - b) * w;
            a = Math.min(1.0F, a * k);
        } else {
            a = Math.min(1.0F, a * Math.max(0.0F, k));
        }

        float x = (float)((l.local ? p.x + in.ox : p.x) - cp.x);
        float y = (float)((l.local ? p.y + in.oy : p.y) - cp.y);
        float z = (float)((l.local ? p.z + in.oz : p.z) - cp.z);
        int ri = (int)(r * 255.0F);
        int gi = (int)(g * 255.0F);
        int bi = (int)(b * 255.0F);
        int ai = (int)(a * 255.0F);
        float hs = size / 2.0F;
        switch (l.render) {
            case "flat": {
                float c = Mth.cos(p.rot * (float) (Math.PI / 180.0)) * hs;
                float s = Mth.sin(p.rot * (float) (Math.PI / 180.0)) * hs;
                float ax = c;
                float ay = 0.0F;
                float az = s;
                float bx = -s;
                float by = 0.0F;
                float bz = c;
                if (state.rot != null) {
                    Vector3f av = state.rot.transform(SCRATCH_A.set(c, 0.0F, s));
                    Vector3f bv = state.rot.transform(SCRATCH_B.set(-s, 0.0F, c));
                    ax = av.x;
                    ay = av.y;
                    az = av.z;
                    bx = bv.x;
                    by = bv.y;
                    bz = bv.z;
                }

                v(bb, x - ax - bx, y - ay - by, z - az - bz, 0.0F, 0.0F, ri, gi, bi, ai);
                v(bb, x + ax - bx, y + ay - by, z + az - bz, 1.0F, 0.0F, ri, gi, bi, ai);
                v(bb, x + ax + bx, y + ay + by, z + az + bz, 1.0F, 1.0F, ri, gi, bi, ai);
                v(bb, x - ax + bx, y - ay + by, z - az + bz, 0.0F, 1.0F, ri, gi, bi, ai);
                break;
            }
            case "beam": {
                float len = l.beamLength * in.scale;
                float sx = p.dy * -z - p.dz * -y;
                float sy = p.dz * -x - p.dx * -z;
                float sz = p.dx * -y - p.dy * -x;
                float sl2 = sx * sx + sy * sy + sz * sz;
                if (sl2 < 1.0E-8F) {
                    sx = 1.0F;
                    sy = 0.0F;
                    sz = 0.0F;
                    sl2 = 1.0F;
                }

                float k2 = hs / (float)Math.sqrt(sl2);
                sx *= k2;
                sy *= k2;
                sz *= k2;
                float mx = x + p.dx * len * 0.7F;
                float my = y + p.dy * len * 0.7F;
                float mz = z + p.dz * len * 0.7F;
                float ex = x + p.dx * len;
                float ey = y + p.dy * len;
                float ez = z + p.dz * len;
                v(bb, x - sx, y - sy, z - sz, 0.0F, 0.0F, ri, gi, bi, ai);
                v(bb, x + sx, y + sy, z + sz, 1.0F, 0.0F, ri, gi, bi, ai);
                v(bb, mx + sx, my + sy, mz + sz, 1.0F, 0.7F, ri, gi, bi, ai);
                v(bb, mx - sx, my - sy, mz - sz, 0.0F, 0.7F, ri, gi, bi, ai);
                v(bb, mx - sx, my - sy, mz - sz, 0.0F, 0.7F, ri, gi, bi, ai);
                v(bb, mx + sx, my + sy, mz + sz, 1.0F, 0.7F, ri, gi, bi, ai);
                v(bb, ex + sx, ey + sy, ez + sz, 1.0F, 1.0F, ri, gi, bi, 0);
                v(bb, ex - sx, ey - sy, ez - sz, 0.0F, 1.0F, ri, gi, bi, 0);
                break;
            }
            case "stretch": {
                float vx = (float)p.vx;
                float vy = (float)p.vy;
                float vz = (float)p.vz;
                float speed = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
                float ax = speed > 1.0E-4F ? vx / speed : p.dx;
                float ay = speed > 1.0E-4F ? vy / speed : p.dy;
                float az = speed > 1.0E-4F ? vz / speed : p.dz;
                float len = size + speed * l.stretch;
                float sx = ay * -z - az * -y;
                float sy = az * -x - ax * -z;
                float sz = ax * -y - ay * -x;
                float sl2 = sx * sx + sy * sy + sz * sz;
                if (sl2 < 1.0E-8F) {
                    sx = left.x;
                    sy = left.y;
                    sz = left.z;
                    sl2 = sx * sx + sy * sy + sz * sz;
                }

                float k2 = hs * 0.5F / (float)Math.sqrt(sl2);
                sx *= k2;
                sy *= k2;
                sz *= k2;
                float hx = ax * len / 2.0F;
                float hy = ay * len / 2.0F;
                float hz = az * len / 2.0F;
                v(bb, x - hx - sx, y - hy - sy, z - hz - sz, 0.0F, 0.0F, ri, gi, bi, ai);
                v(bb, x + hx - sx, y + hy - sy, z + hz - sz, 1.0F, 0.0F, ri, gi, bi, ai);
                v(bb, x + hx + sx, y + hy + sy, z + hz + sz, 1.0F, 1.0F, ri, gi, bi, ai);
                v(bb, x - hx + sx, y - hy + sy, z - hz + sz, 0.0F, 1.0F, ri, gi, bi, ai);
                break;
            }
            default: {
                float c = Mth.cos(p.rot * (float) (Math.PI / 180.0));
                float s = Mth.sin(p.rot * (float) (Math.PI / 180.0));
                float rx = (-left.x * c + up.x * s) * hs;
                float ry = (-left.y * c + up.y * s) * hs;
                float rz = (-left.z * c + up.z * s) * hs;
                float ux = (left.x * s + up.x * c) * hs;
                float uy = (left.y * s + up.y * c) * hs;
                float uz = (left.z * s + up.z * c) * hs;
                v(bb, x - rx - ux, y - ry - uy, z - rz - uz, 0.0F, 1.0F, ri, gi, bi, ai);
                v(bb, x + rx - ux, y + ry - uy, z + rz - uz, 1.0F, 1.0F, ri, gi, bi, ai);
                v(bb, x + rx + ux, y + ry + uy, z + rz + uz, 1.0F, 0.0F, ri, gi, bi, ai);
                v(bb, x - rx + ux, y - ry + uy, z - rz + uz, 0.0F, 0.0F, ri, gi, bi, ai);
            }
        }
    }

    private static void v(BufferBuilder bb, float x, float y, float z, float u, float vv, int r, int g, int b, int a) {
        bb.vertex(p00 * x + p10 * y + p20 * z + p30, p01 * x + p11 * y + p21 * z + p31, p02 * x + p12 * y + p22 * z + p32)
            .uv(u, vv)
            .color(r, g, b, a)
            .endVertex();
    }

    private static float screen(String type, float[] rgb) {
        return screenOf(INSTANCES, type, rgb, true);
    }

    private static float screenOf(List<FxEngine.Inst> instances, String type, float[] rgb, boolean byDistance) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 cp = mc.gameRenderer.getMainCamera().getPosition();
        float best = 0.0F;

        for (FxEngine.Inst in : instances) {
            for (FxEngine.LayerState s : in.layers) {
                FxDef.Layer l = s.l;
                if (l.type.equals(type)) {
                    double lt = in.t - l.delay;
                    float len = Math.max(0.05F, l.time > 0.0F ? l.time : 0.6F);
                    if (!(lt < 0.0) && !(lt > len)) {
                        float k = (float)(1.0 - lt / len);
                        float v = l.strength * k * k * Math.max(0.0F, in.fade);
                        if (l.range > 0.0F && byDistance) {
                            double d = cp.distanceTo(new Vec3(in.ox, in.oy, in.oz));
                            v *= (float)Math.max(0.0, 1.0 - d / (l.range * in.scale));
                        }

                        if (v > best) {
                            best = v;
                            if (rgb != null) {
                                int c = FxDef.parseColor(l.flashColor);
                                rgb[0] = (c >> 16 & 0xFF) / 255.0F;
                                rgb[1] = (c >> 8 & 0xFF) / 255.0F;
                                rgb[2] = (c & 0xFF) / 255.0F;
                            }
                        }
                    }
                }
            }
        }

        return Math.min(1.0F, best);
    }

    public static void onGui(ClientHooks.RenderGui e) {
        if (!INSTANCES.isEmpty()) {
            float[] rgb = new float[3];
            float a = screen("flash", rgb);
            if (!(a <= 0.003F)) {
                int c = (int)(a * 255.0F) << 24 | (int)(rgb[0] * 255.0F) << 16 | (int)(rgb[1] * 255.0F) << 8 | (int)(rgb[2] * 255.0F);
                int w = e.getWindow().getGuiScaledWidth();
                int h = e.getWindow().getGuiScaledHeight();
                e.getGuiGraphics().fill(0, 0, w, h, c);
            }
        }
    }

    public static void onAngles(ClientHooks.CameraAngles e) {
        if (!INSTANCES.isEmpty()) {
            float s = screen("shake", null);
            if (!(s <= 0.001F)) {
                double t = System.nanoTime() / 1.0E9 * 22.0;
                float k = s * s;
                e.setYaw(e.getYaw() + (float)(Math.sin(t * 1.13) + Math.sin(t * 2.71) * 0.5) * 3.5F * k);
                e.setPitch(e.getPitch() + (float)(Math.sin(t * 1.37 + 2.0) + Math.sin(t * 3.3) * 0.5) * 2.5F * k);
                e.setRoll(e.getRoll() + (float)(Math.sin(t * 0.91 + 5.0) + Math.sin(t * 2.2) * 0.5) * 2.5F * k);
            }
        }
    }

    public static void onLogout(ClientHooks.LoggingOut e) {
        INSTANCES.clear();
        total = 0;
        ALPHA.clear();
        ADDITIVE.clear();
    }

    private static final class Batch {
        final String key;
        ResourceLocation texture;
        FxEngine.Inst[] insts = new FxEngine.Inst[256];
        FxEngine.LayerState[] layers = new FxEngine.LayerState[256];
        FxEngine.P[] particles = new FxEngine.P[256];
        long[] order = new long[0];
        int n;

        Batch(String key) {
            this.key = key;
        }

        void add(FxEngine.Inst in, FxEngine.LayerState s, FxEngine.P p) {
            if (this.n == this.particles.length) {
                this.insts = Arrays.copyOf(this.insts, this.n * 2);
                this.layers = Arrays.copyOf(this.layers, this.n * 2);
                this.particles = Arrays.copyOf(this.particles, this.n * 2);
            }

            this.insts[this.n] = in;
            this.layers[this.n] = s;
            this.particles[this.n++] = p;
        }
    }

    private static final class Inst {
        final String id;
        final String project;
        int scriptId = -1;
        final FxDef def;
        final float scale;
        final int entity;
        String bone = "";
        double ox;
        double oy;
        double oz;
        double vx;
        double vy;
        double vz;
        double g;
        boolean moving;
        float flyLeft;
        double t;
        boolean stopping;
        float fade = 1.0F;
        float pendingDt;
        int frames;
        final List<FxEngine.LayerState> layers = new ArrayList<>();

        Inst(String id, String project, FxDef def, double x, double y, double z, int entity, float scale) {
            this.id = id;
            this.project = project;
            this.def = def;
            this.ox = x;
            this.oy = y;
            this.oz = z;
            this.entity = entity;
            this.scale = scale;

            for (FxDef.Layer l : def.layers) {
                if (l.enabled) {
                    this.layers.add(new FxEngine.LayerState(l, project));
                }
            }
        }

        boolean finished() {
            if (this.stopping && this.fade <= 0.0F) {
                return true;
            } else if (!(this.def.duration <= 0.0F) && !(this.t < this.def.duration)) {
                for (FxEngine.LayerState s : this.layers) {
                    if (!s.ps.isEmpty()) {
                        return false;
                    }

                    if (!s.l.type.equals("particles") && this.t < s.l.delay + FxEngine.layerTime(s.l, this.def)) {
                        return false;
                    }
                }

                return true;
            } else {
                return false;
            }
        }
    }

    private static final class LayerState {
        final FxDef.Layer l;
        final int[] colors;
        final List<FxEngine.P> ps = new ArrayList<>();
        final String batch;
        float accum;
        double nextBurst;
        boolean burstDone;
        final Quaternionf rot;
        final Vector3f axis;

        LayerState(FxDef.Layer l, String project) {
            this.l = l;
            this.batch = l.texture + "|" + project;
            if (l.rotX == 0.0F && l.rotY == 0.0F && l.rotZ == 0.0F) {
                this.rot = null;
                this.axis = null;
            } else {
                this.rot = new Quaternionf()
                    .rotateY((float)Math.toRadians(l.rotY))
                    .rotateX((float)Math.toRadians(l.rotX))
                    .rotateZ((float)Math.toRadians(l.rotZ));
                this.axis = this.rot.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            }

            this.colors = new int[l.color.length];

            for (int i = 0; i < this.colors.length; i++) {
                this.colors[i] = FxDef.parseColor(l.color[i]);
            }
        }
    }

    private static final class P {
        double x;
        double y;
        double z;
        double vx;
        double vy;
        double vz;
        float age;
        float life;
        float sizeMul;
        float rot;
        float spin;
        float seed;
        float dx;
        float dy;
        float dz;
        int color = -1;
        float drawSize;
        float drawAlpha;
    }
}
