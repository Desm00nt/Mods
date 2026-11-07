package org.sprautestudio.spraute_engine.post.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.post.PostParams;

public final class PostEngine {
    private static final List<PostEngine.Layer> LAYERS = new ArrayList<>();
    private static final List<PostParams.Param> PARAMS = PostParams.all();
    private static final Map<String, Integer> INDEX = new HashMap<>();
    private static final float[][] VALUES = new float[PARAMS.size()][];
    private static boolean tried;
    private static boolean broken;
    private static PostEngine.Program down;
    private static PostEngine.Program up;
    private static PostEngine.Program aoProgram;
    private static PostEngine.Program raysProgram;
    private static PostEngine.Program finalProgram;
    private static final int LEVELS = 5;
    private static RenderTarget scene;
    private static RenderTarget history;
    private static RenderTarget aoTarget;
    private static RenderTarget aoSoft;
    private static RenderTarget raysTarget;
    private static RenderTarget[] bloomDown;
    private static RenderTarget[] bloomUp;
    private static RenderTarget[] blurDown;
    private static RenderTarget[] blurUp;
    private static int width;
    private static int height;
    private static boolean historyFilled;
    private static float m00;
    private static float m11;
    private static float focus;
    private static boolean ranThisFrame;

    private PostEngine() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(PostEngine::onRenderLevel);
        ClientHooks.RENDER_GUI.add(Priority.HIGHEST, PostEngine::onGui);
        ClientHooks.LOGGING_OUT.add(e -> clear());
    }

    private static double now() {
        return System.nanoTime() / 1.0E9;
    }

    private static PostEngine.Layer layer(String id) {
        for (PostEngine.Layer l : LAYERS) {
            if (l.id.equals(id) && !l.leaving) {
                return l;
            }
        }

        return null;
    }

    public static void show(String id, Map<String, float[]> values, double in, double hold, double out, double power) {
    }

    public static void set(String id, String name, float[] value, double seconds) {
        Integer i = INDEX.get(name);
        if (i != null) {
            double now = now();
            PostEngine.Layer l = layer(id);
            if (l == null) {
                l = new PostEngine.Layer(id);
                l.weightFrom = 1.0F;
                LAYERS.add(l);
                LAYERS.sort(Comparator.comparingInt(a -> a.order));
            }

            PostEngine.Ramp r = l.ramps.get(i);
            float[] from = r != null ? new float[value.length] : PostParams.defaults(PARAMS.get(i));
            if (r != null) {
                for (int k = 0; k < from.length; k++) {
                    from[k] = r.value(k, now);
                }
            }

            r = new PostEngine.Ramp();
            r.from = from;
            r.to = (float[])value.clone();
            r.start = now;
            r.length = seconds;
            l.ramps.put(i, r);
        }
    }

    public static void remove(String id, double seconds) {
        double now = now();

        for (PostEngine.Layer l : LAYERS) {
            if (l.id.equals(id) && !l.leaving) {
                l.leaving = true;
                l.fadeTo(0.0F, Math.max(seconds, 0.001), now);
            }
        }
    }

    public static void removeAll(String prefix, double seconds) {
        double now = now();

        for (PostEngine.Layer l : LAYERS) {
            if (l.id.startsWith(prefix) && !l.leaving) {
                l.leaving = true;
                l.fadeTo(0.0F, Math.max(seconds, 0.001), now);
            }
        }
    }

    public static void clear() {
        LAYERS.clear();
    }

    private static boolean mix(double now) {
        for (int i = 0; i < VALUES.length; i++) {
            float[] d = PostParams.defaults(PARAMS.get(i));
            System.arraycopy(d, 0, VALUES[i], 0, d.length);
        }

        boolean any = false;
        Iterator<PostEngine.Layer> it = LAYERS.iterator();

        while (it.hasNext()) {
            PostEngine.Layer l = it.next();
            if (!l.leaving && now >= l.leaveAt) {
                l.leaving = true;
                l.fadeTo(0.0F, Math.max(l.leaveLength, 0.001), now);
            }

            float w = l.weight(now);
            if (l.leaving && w <= 0.001F) {
                it.remove();
            } else if (!(w <= 0.001F)) {
                any = true;

                for (Entry<Integer, PostEngine.Ramp> e : l.ramps.entrySet()) {
                    float[] v = VALUES[e.getKey()];
                    PostEngine.Ramp r = e.getValue();
                    float k = v.length > 1 ? Math.min(1.0F, w) : w;

                    for (int c = 0; c < v.length; c++) {
                        v[c] += (r.value(c, now) - v[c]) * k;
                    }
                }
            }
        }

        return any;
    }

    private static float v(String id) {
        return VALUES[INDEX.get(id)][0];
    }

    private static boolean changed(String id) {
        PostParams.Param p = PARAMS.get(INDEX.get(id));
        return Math.abs(VALUES[INDEX.get(id)][0] - p.def()) > 0.001F;
    }

    private static String resource(String name) throws IOException {
        return Files.readString(Platform.get().modResource("assets", "spraute_engine", "post", name));
    }

    private static int shader(int type, String source, String name) {
        int s = GL20.glCreateShader(type);
        GL20.glShaderSource(s, source);
        GL20.glCompileShader(s);
        if (GL20.glGetShaderi(s, 35713) == 0) {
            String log = GL20.glGetShaderInfoLog(s, 4096);
            GL20.glDeleteShader(s);
            throw new IllegalStateException(name + ": " + log);
        } else {
            return s;
        }
    }

    private static PostEngine.Program program(String fragment, String extra) throws IOException {
        int vs = shader(35633, resource("screen.vsh"), "screen.vsh");
        String text = resource(fragment);
        int line = text.indexOf(10) + 1;
        int fs = shader(35632, text.substring(0, line) + extra + text.substring(line), fragment);
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs);
        GL20.glAttachShader(p, fs);
        GL20.glBindAttribLocation(p, 0, "Position");
        GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);
        GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, 35714) == 0) {
            String log = GL20.glGetProgramInfoLog(p, 4096);
            GL20.glDeleteProgram(p);
            throw new IllegalStateException(fragment + ": " + log);
        } else {
            return new PostEngine.Program(p);
        }
    }

    private static boolean ready() {
        if (tried) {
            return !broken;
        } else {
            tried = true;

            try {
                StringBuilder uniforms = new StringBuilder();

                for (PostParams.Param p : PARAMS) {
                    uniforms.append(p.color() ? "uniform vec3 u_" : "uniform float u_").append(p.id()).append(";\n");
                }

                down = program("down.fsh", "");
                up = program("up.fsh", "");
                aoProgram = program("ao.fsh", "");
                raysProgram = program("rays.fsh", "");
                finalProgram = program("final.fsh", uniforms.toString());
            } catch (RuntimeException | IOException var3) {
                broken = true;
                SprauteEngine.LOGGER.error("Spraute: post-processing is off, its shaders did not compile", (Throwable)var3);
            }

            return !broken;
        }
    }

    private static RenderTarget target(int w, int h) {
        TextureTarget t = new TextureTarget(Math.max(1, w), Math.max(1, h), false, Minecraft.ON_OSX);
        t.setFilterMode(9729);
        return t;
    }

    private static void drop(RenderTarget t) {
        if (t != null) {
            t.destroyBuffers();
        }
    }

    private static void drop(RenderTarget[] ts) {
        if (ts != null) {
            for (RenderTarget t : ts) {
                drop(t);
            }
        }
    }

    private static void resize(int w, int h) {
        if (w != width || h != height) {
            width = w;
            height = h;
            drop(scene);
            drop(history);
            drop(aoTarget);
            drop(aoSoft);
            drop(raysTarget);
            drop(bloomDown);
            drop(bloomUp);
            drop(blurDown);
            drop(blurUp);
            raysTarget = null;
            aoSoft = null;
            aoTarget = null;
            history = null;
            scene = null;
            blurUp = null;
            blurDown = null;
            bloomUp = null;
            bloomDown = null;
            historyFilled = false;
        }
    }

    private static RenderTarget[] chain() {
        RenderTarget[] c = new RenderTarget[5];

        for (int i = 0; i < 5; i++) {
            c[i] = target(width >> i + 1, height >> i + 1);
        }

        return c;
    }

    private static void quad() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION);
        bb.vertex(0.0, 0.0, 0.0).endVertex();
        bb.vertex(1.0, 0.0, 0.0).endVertex();
        bb.vertex(1.0, 1.0, 0.0).endVertex();
        bb.vertex(0.0, 1.0, 0.0).endVertex();
        BufferUploader.draw(bb.end());
    }

    private static void use(PostEngine.Program p, RenderTarget to) {
        to.bindWrite(true);
        GlStateManager._glUseProgram(p.id);
    }

    private static void copy(RenderTarget from, RenderTarget to) {
        GL30.glBindFramebuffer(36008, from.frameBufferId);
        GL30.glBindFramebuffer(36009, to.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, from.width, from.height, 0, 0, to.width, to.height, 16384, 9728);
    }

    private static int pyramid(RenderTarget source, RenderTarget[] downs, RenderTarget[] ups, float threshold, float radius, boolean additive) {
        int from = source.getColorTextureId();

        for (int i = 0; i < 5; i++) {
            use(down, downs[i]);
            down.tex("tex", 0, from);
            down.vec2("texel", 0.5F / downs[i].width, 0.5F / downs[i].height);
            down.f("threshold", i == 0 ? threshold : 0.0F);
            quad();
            from = downs[i].getColorTextureId();
        }

        for (int i = 3; i >= 0; i--) {
            use(up, ups[i]);
            up.tex("tex", 0, from);
            up.tex("over", 1, downs[i].getColorTextureId());
            up.vec2("texel", 0.5F / ups[i].width, 0.5F / ups[i].height);
            up.f("radius", radius);
            up.f("additive", additive ? 1.0F : 0.0F);
            quad();
            from = ups[i].getColorTextureId();
        }

        return from;
    }

    public static void onRenderLevel(ClientHooks.RenderLevel e) {
        if (e.getProjectionMatrix() != null) {
            m00 = e.getProjectionMatrix().m00();
            m11 = e.getProjectionMatrix().m11();
        }
    }

    private static float[] sunOnScreen(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.dimensionType().hasSkyLight()) {
            double a = mc.level.getTimeOfDay(partial) * Math.PI * 2.0;
            Vector3f sun = new Vector3f((float)(-Math.sin(a)), (float)Math.cos(a), 0.0F);
            float dim = 1.0F;
            if (sun.y < 0.0F) {
                sun.negate();
                dim = 0.35F;
            }

            Camera cam = mc.gameRenderer.getMainCamera();
            Vector3f left = Mc.cameraLeft(cam);
            Vector3f upV = Mc.cameraUp(cam);
            Vector3f forward = new Vector3f(left).cross(upV);
            float z = sun.dot(forward);
            if (z < 0.05F) {
                return new float[]{0.5F, 0.5F, 0.0F};
            } else {
                float x = -sun.dot(left) / z * m00;
                float y = sun.dot(upV) / z * m11;
                float edge = Math.max(Math.abs(x), Math.abs(y));
                float seen = Math.max(0.0F, Math.min(1.0F, (1.6F - edge) / 0.6F));
                return new float[]{x * 0.5F + 0.5F, y * 0.5F + 0.5F, seen * dim * Math.min(1.0F, sun.y * 5.0F + 0.2F)};
            }
        } else {
            return new float[]{0.5F, 0.5F, 0.0F};
        }
    }

    private static void autoFocus(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            Camera cam = mc.gameRenderer.getMainCamera();
            Vec3 o = cam.getPosition();
            Vector3f f = new Vector3f(Mc.cameraLeft(cam)).cross(Mc.cameraUp(cam));
            Vec3 to = o.add(f.x * 128.0F, f.y * 128.0F, f.z * 128.0F);
            BlockHitResult h = mc.level.clip(new ClipContext(o, to, Block.OUTLINE, Fluid.NONE, mc.player));
            float want = h.getType() == Type.BLOCK ? (float)h.getLocation().distanceTo(o) : 128.0F;
            focus = focus + (want - focus) * 0.08F;
        }
    }

    public static void afterWorld(float partial) {
        ranThisFrame = true;
        process(partial);
    }

    public static void onGui(ClientHooks.RenderGui e) {
        if (!ranThisFrame) {
            process(e.getPartialTick());
        }

        ranThisFrame = false;
    }

    private static void process(float partial) {
        if (!LAYERS.isEmpty()) {
            double now = now();
            if (mix(now) && ready()) {
                Minecraft mc = Minecraft.getInstance();
                RenderTarget main = mc.getMainRenderTarget();
                resize(main.width, main.height);
                if (scene == null) {
                    scene = target(width, height);
                }

                boolean useDepth = !Platform.get().isModLoaded("iris") && !Platform.get().isModLoaded("oculus");
                float near = 0.05F;
                float far = mc.gameRenderer.getRenderDistance() * 4.0F;
                boolean wantsBloom = v("bloom") > 0.001F;
                boolean wantsBlur = v("blur") > 0.001F || v("tilt_shift") > 0.001F || useDepth && v("dof") > 0.001F;
                boolean wantsAo = useDepth && v("ao") > 0.001F;
                float[] sun = sunOnScreen(partial);
                boolean wantsRays = v("god_rays") > 0.001F && sun[2] > 0.001F;
                boolean wantsHistory = v("motion_blur") > 0.001F;
                if (useDepth && v("dof") > 0.001F && v("dof_focus") <= 0.0F) {
                    autoFocus(partial);
                }

                if (!wantsHistory) {
                    historyFilled = false;
                }

                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                try {
                    copy(main, scene);
                    int unit = 0;
                    int blurTex = 0;
                    int aoTex = 0;
                    int raysTex = 0;
                    if (wantsBloom) {
                        if (bloomDown == null) {
                            bloomDown = chain();
                            bloomUp = chain();
                        }

                        unit = pyramid(scene, bloomDown, bloomUp, Math.max(0.001F, v("bloom_threshold")), v("bloom_radius"), true);
                    }

                    if (wantsBlur) {
                        if (blurDown == null) {
                            blurDown = chain();
                            blurUp = chain();
                        }

                        blurTex = pyramid(scene, blurDown, blurUp, 0.0F, 1.0F, false);
                    }

                    int depth = main.getDepthTextureId();
                    if (wantsAo) {
                        if (aoTarget == null) {
                            aoTarget = target(width / 2, height / 2);
                            aoSoft = target(width / 2, height / 2);
                        }

                        use(aoProgram, aoTarget);
                        aoProgram.tex("depthTex", 0, depth);
                        aoProgram.f("nearPlane", near);
                        aoProgram.f("farPlane", far);
                        aoProgram.f("radius", v("ao_radius"));
                        aoProgram.f("aspect", (float)width / height);
                        aoProgram.f("tanFov", 1.0F / m11);
                        quad();
                        use(down, aoSoft);
                        down.tex("tex", 0, aoTarget.getColorTextureId());
                        down.vec2("texel", 1.0F / aoSoft.width, 1.0F / aoSoft.height);
                        down.f("threshold", 0.0F);
                        quad();
                        aoTex = aoSoft.getColorTextureId();
                    }

                    if (wantsRays) {
                        if (raysTarget == null) {
                            raysTarget = target(width / 2, height / 2);
                        }

                        use(raysProgram, raysTarget);
                        raysProgram.tex("tex", 0, scene.getColorTextureId());
                        raysProgram.tex("depthTex", 1, depth);
                        raysProgram.vec2("sunPos", sun[0], sun[1]);
                        raysProgram.f("useDepth", useDepth ? 1.0F : 0.0F);
                        quad();
                        raysTex = raysTarget.getColorTextureId();
                    }

                    if (wantsHistory && history == null) {
                        history = target(width, height);
                    }

                    use(finalProgram, main);
                    PostEngine.Program f = finalProgram;
                    f.tex("sceneTex", 0, scene.getColorTextureId());
                    f.tex("depthTex", 1, depth);
                    f.tex("bloomTex", 2, unit);
                    f.tex("blurTex", 3, blurTex);
                    f.tex("aoTex", 4, aoTex);
                    f.tex("raysTex", 5, raysTex);
                    f.tex("historyTex", 6, wantsHistory ? history.getColorTextureId() : 0);
                    f.vec2("screenSize", width, height);
                    f.f("time", (float)(now % 3600.0));
                    f.f("nearPlane", near);
                    f.f("farPlane", far);
                    f.f("focusAuto", focus);
                    f.f("useDepth", useDepth ? 1.0F : 0.0F);
                    f.f("hasBloom", wantsBloom ? 1.0F : 0.0F);
                    f.f("hasBlur", wantsBlur ? 1.0F : 0.0F);
                    f.f("hasAo", wantsAo ? 1.0F : 0.0F);
                    f.f("hasRays", wantsRays ? 1.0F : 0.0F);
                    f.f("hasHistory", wantsHistory && historyFilled ? 1.0F : 0.0F);
                    f.vec2("sunPos", sun[0], sun[1]);
                    f.f("sunVisible", sun[2]);

                    for (int i = 0; i < PARAMS.size(); i++) {
                        PostParams.Param p = PARAMS.get(i);
                        int at = f.at("u_" + p.id());
                        if (at >= 0) {
                            float[] val = VALUES[i];
                            if (p.color()) {
                                GL20.glUniform3f(at, val[0], val[1], val[2]);
                            } else {
                                GL20.glUniform1f(at, val[0]);
                            }
                        }
                    }

                    quad();
                    if (wantsHistory) {
                        copy(main, history);
                        historyFilled = true;
                    }
                } finally {
                    GlStateManager._glUseProgram(0);

                    for (int unit = 6; unit >= 0; unit--) {
                        GlStateManager._activeTexture(33984 + unit);
                        GlStateManager._bindTexture(0);
                    }

                    main.bindWrite(true);
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                    RenderSystem.enableCull();
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                }
            }
        }
    }

    static {
        for (int i = 0; i < PARAMS.size(); i++) {
            INDEX.put(PARAMS.get(i).id(), i);
            VALUES[i] = PostParams.defaults(PARAMS.get(i));
        }

        m00 = 1.4F;
        m11 = 2.5F;
        focus = 16.0F;
    }

    private static final class Layer {
        final String id;
        final int order;
        final Map<Integer, PostEngine.Ramp> ramps = new HashMap<>();
        float weightFrom;
        float weightTo = 1.0F;
        double weightStart;
        double weightLength;
        double leaveAt = Double.MAX_VALUE;
        double leaveLength;
        boolean leaving;

        Layer(String id) {
            this.id = id;
            this.order = id.startsWith("zone") ? 1 : (id.startsWith("story") ? 2 : (id.startsWith("pulse") ? 3 : (id.startsWith("cutscene") ? 4 : 5)));
        }

        float weight(double now) {
            if (!(this.weightLength <= 0.0) && !(now >= this.weightStart + this.weightLength)) {
                double k = (now - this.weightStart) / this.weightLength;
                k = k * k * (3.0 - 2.0 * k);
                return (float)(this.weightFrom + (this.weightTo - this.weightFrom) * k);
            } else {
                return this.weightTo;
            }
        }

        void fadeTo(float to, double length, double now) {
            this.weightFrom = this.weight(now);
            this.weightTo = to;
            this.weightStart = now;
            this.weightLength = length;
        }
    }

    private static final class Program {
        final int id;
        final Map<String, Integer> uniforms = new HashMap<>();

        Program(int id) {
            this.id = id;
        }

        int at(String name) {
            return this.uniforms.computeIfAbsent(name, n -> GL20.glGetUniformLocation(this.id, n));
        }

        void f(String name, float v) {
            int l = this.at(name);
            if (l >= 0) {
                GL20.glUniform1f(l, v);
            }
        }

        void vec2(String name, float x, float y) {
            int l = this.at(name);
            if (l >= 0) {
                GL20.glUniform2f(l, x, y);
            }
        }

        void tex(String name, int unit, int texture) {
            int l = this.at(name);
            if (l >= 0) {
                GL20.glUniform1i(l, unit);
                GlStateManager._activeTexture(33984 + unit);
                GlStateManager._bindTexture(texture);
            }
        }
    }

    private static final class Ramp {
        float[] from;
        float[] to;
        double start;
        double length;

        float value(int i, double now) {
            if (!(this.length <= 0.0) && !(now >= this.start + this.length)) {
                double k = Math.max(0.0, (now - this.start) / this.length);
                return (float)(this.from[i] + (this.to[i] - this.from[i]) * k);
            } else {
                return this.to[i];
            }
        }
    }
}
