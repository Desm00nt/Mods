package org.sprautestudio.spraute_engine.gizmo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.ClientHooks;

public final class GizmoClient {
    private static final Map<String, GizmoClient.Gizmo> GIZMOS = new LinkedHashMap<>();
    private static Camera camera;
    private static TextureTarget saved;

    private GizmoClient() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(GizmoClient::onRenderLevel);
        ClientHooks.REGISTER_HUD.add(e -> e.registerAbove(ClientHooks.HudParts.CHAT_PANEL, "gizmos", (g, partial, w, h) -> drawScreen(g, w, h)));
        ClientHooks.LOGGING_OUT.add(e -> GIZMOS.clear());
    }

    public static void set(int script, String id, String json) {
        JsonObject d = JsonParser.parseString(json).getAsJsonObject();
        int ticks = d.has("time") ? d.get("time").getAsInt() : 0;
        GIZMOS.put(script + ":" + id, new GizmoClient.Gizmo(script, d, ticks > 0 ? System.currentTimeMillis() + ticks * 50L : 0L));
    }

    public static void remove(int script, String id) {
        if (id.isEmpty()) {
            GIZMOS.values().removeIf(g -> g.script == script);
        } else if (id.equals("#highlight")) {
            GIZMOS.values().removeIf(g -> g.script == script && kind(g.d).equals("highlight"));
        } else if (id.equals("#gizmo")) {
            GIZMOS.values().removeIf(g -> g.script == script && !kind(g.d).equals("highlight"));
        } else {
            GIZMOS.remove(script + ":" + id);
        }
    }

    private static void prune() {
        long now = System.currentTimeMillis();
        GIZMOS.values().removeIf(g -> g.until > 0L && now > g.until);
    }

    private static void onRenderLevel(ClientHooks.RenderLevel e) {
        if (e.getStage() == ClientHooks.LevelStage.AFTER_PARTICLES && !GIZMOS.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                prune();
                Map<UUID, Entity> ents = new HashMap<>();

                for (Entity en : mc.level.entitiesForRendering()) {
                    ents.put(en.getUUID(), en);
                }

                camera = e.getCamera();
                Vec3 cam = camera.getPosition();
                float partial = e.getPartialTick();
                PoseStack ps = e.getPoseStack();
                BufferSource src = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
                boolean anyThrough = false;

                for (GizmoClient.Gizmo g : GIZMOS.values()) {
                    if (through(g.d)) {
                        anyThrough = true;
                    } else if (!kind(g.d).startsWith("screen")) {
                        world(g.d, ps, cam, partial, src, ents, false);
                    }
                }

                if (anyThrough) {
                    RenderTarget main = mc.getMainRenderTarget();
                    if (saved == null || saved.width != main.width || saved.height != main.height) {
                        if (saved != null) {
                            saved.destroyBuffers();
                        }

                        saved = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                    }

                    saved.copyDepthFrom(main);
                    main.bindWrite(false);
                    RenderSystem.clear(256, Minecraft.ON_OSX);

                    try {
                        for (GizmoClient.Gizmo gx : GIZMOS.values()) {
                            if (through(gx.d) && !kind(gx.d).startsWith("screen")) {
                                world(gx.d, ps, cam, partial, src, ents, true);
                            }
                        }
                    } finally {
                        main.copyDepthFrom(saved);
                        main.bindWrite(false);
                    }
                }
            }
        }
    }

    private static String kind(JsonObject d) {
        return d.has("kind") ? d.get("kind").getAsString() : "";
    }

    private static boolean through(JsonObject d) {
        return d.has("through") && d.get("through").getAsBoolean();
    }

    private static int argb(JsonObject d, String key, int fallback) {
        return d.has(key) ? d.get(key).getAsInt() : fallback;
    }

    private static float num(JsonObject d, String key, float fallback) {
        return d.has(key) ? d.get(key).getAsFloat() : fallback;
    }

    private static Vec3 point(JsonElement p, Map<UUID, Entity> ents, float partial) {
        if (p == null) {
            return null;
        } else if (p.isJsonArray()) {
            JsonArray a = p.getAsJsonArray();
            return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
        } else {
            Entity en = ents.get(UUID.fromString(p.getAsJsonObject().get("e").getAsString()));
            return en == null ? null : en.getPosition(partial).add(0.0, en.getBbHeight() / 2.0F, 0.0);
        }
    }

    private static void world(JsonObject d, PoseStack ps, Vec3 cam, float partial, BufferSource src, Map<UUID, Entity> ents, boolean through) {
        ps.pushPose();
        ps.translate(-cam.x, -cam.y, -cam.z);

        try {
            switch (kind(d)) {
                case "highlight":
                    highlight(d, ps, cam, partial, src, ents, through);
                    return;
                case "line": {
                    Vec3 a = point(d.get("a"), ents, partial);
                    Vec3 b = point(d.get("b"), ents, partial);
                    if (a != null && b != null) {
                        RenderType t = GizmoTypes.lines(num(d, "width", 2.0F), through);
                        line(src.getBuffer(t), ps.last(), a, b, argb(d, "color", -1));
                        src.endBatch(t);
                        return;
                    }

                    return;
                }
                case "box": {
                    Vec3 a = point(d.get("a"), ents, partial);
                    Vec3 b = point(d.get("b"), ents, partial);
                    if (a != null && b != null) {
                        AABB box = new AABB(a, b);
                        int fill = argb(d, "fill", 0);
                        if (fill >>> 24 != 0) {
                            RenderType ft = through ? GizmoTypes.SHAPE_THROUGH : GizmoTypes.SHAPE;
                            faces(src.getBuffer(ft), ps.last(), box, fill);
                            src.endBatch(ft);
                        }

                        RenderType t = GizmoTypes.lines(num(d, "width", 2.0F), through);
                        edges(src.getBuffer(t), ps.last(), box, argb(d, "color", -1));
                        src.endBatch(t);
                        return;
                    }

                    return;
                }
                case "sphere": {
                    Vec3 c = point(d.get("a"), ents, partial);
                    if (c != null) {
                        RenderType t = GizmoTypes.lines(num(d, "width", 2.0F), through);
                        sphere(src.getBuffer(t), ps.last(), c, num(d, "radius", 1.0F), argb(d, "color", -1));
                        src.endBatch(t);
                        return;
                    }

                    return;
                }
                case "text": {
                    Vec3 at = point(d.get("a"), ents, partial);
                    if (at != null) {
                        JsonElement p = d.get("a");
                        if (p != null && p.isJsonObject()) {
                            Entity en = ents.get(UUID.fromString(p.getAsJsonObject().get("e").getAsString()));
                            if (en != null) {
                                at = at.add(0.0, en.getBbHeight() / 2.0F + 0.6, 0.0);
                            }
                        }

                        text(d, ps, at, src, through);
                        return;
                    }

                    return;
                }
            }
        } finally {
            ps.popPose();
        }
    }

    private static void highlight(JsonObject d, PoseStack ps, Vec3 cam, float partial, BufferSource src, Map<UUID, Entity> ents, boolean through) {
        int fill = argb(d, "fill", 1728053247);
        int outline = argb(d, "outline", -1);
        float width = num(d, "width", 0.05F);
        RenderType fillType = through ? GizmoTypes.FILL_DEPTH : GizmoTypes.FILL;
        if (d.has("block")) {
            JsonArray b = d.getAsJsonArray("block");
            BlockPos pos = new BlockPos(b.get(0).getAsInt(), b.get(1).getAsInt(), b.get(2).getAsInt());
            ClientLevel level = Minecraft.getInstance().level;
            VoxelShape shape = level.getBlockState(pos).getShape(level, pos);
            List<AABB> boxes = shape.isEmpty() ? List.of(new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)) : shape.toAabbs();
            if (through) {
                blockFaces(src, fillType, ps, boxes, pos, 0.0F, fill);
            }

            blockFaces(src, GizmoTypes.HULL, ps, boxes, pos, width, outline);
            if (!through) {
                blockFaces(src, fillType, ps, boxes, pos, 0.0F, fill);
            }
        } else {
            Entity en = d.has("entity") ? ents.get(UUID.fromString(d.get("entity").getAsString())) : null;
            if (en != null && (en != Minecraft.getInstance().getCameraEntity() || !Minecraft.getInstance().options.getCameraType().isFirstPerson())) {
                if (through) {
                    entity(en, src, fillType, ps, partial, 0.0F, fill);
                }

                entity(en, src, GizmoTypes.HULL, ps, partial, width, outline);
                if (!through) {
                    entity(en, src, fillType, ps, partial, 0.0F, fill);
                }
            }
        }
    }

    private static void blockFaces(BufferSource src, RenderType t, PoseStack ps, List<AABB> boxes, BlockPos pos, float grow, int color) {
        if (color >>> 24 != 0 || t == GizmoTypes.FILL_DEPTH) {
            VertexConsumer vc = src.getBuffer(t);

            for (AABB b : boxes) {
                faces(vc, ps.last(), b.move(pos).inflate(grow), color);
            }

            src.endBatch(t);
        }
    }

    private static void entity(Entity en, BufferSource src, RenderType t, PoseStack ps, float partial, float grow, int color) {
        if (color >>> 24 != 0 || t == GizmoTypes.FILL_DEPTH) {
            Minecraft mc = Minecraft.getInstance();
            Vec3 at = en.getPosition(partial);
            ps.pushPose();
            if (grow > 0.0F) {
                double w = Math.max(0.2, (double)en.getBbWidth());
                double h = Math.max(0.2, (double)en.getBbHeight());
                ps.translate(at.x, at.y + h / 2.0, at.z);
                ps.scale((float)((w + 2.0F * grow) / w), (float)((h + 2.0F * grow) / h), (float)((w + 2.0F * grow) / w));
                ps.translate(-at.x, -at.y - h / 2.0, -at.z);
            }

            try {
                float yaw = Mth.lerp(partial, en.yRotO, en.getYRot());
                mc.getEntityRenderDispatcher().render(en, at.x, at.y, at.z, yaw, partial, ps, new GizmoClient.Tinted(src.getBuffer(t), color), 15728880);
            } catch (RuntimeException var16) {
            } finally {
                ps.popPose();
            }

            src.endBatch(t);
        }
    }

    private static void vertex(VertexConsumer vc, Pose p, double x, double y, double z, int c) {
        vc.vertex(p.pose(), (float)x, (float)y, (float)z).color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24).endVertex();
    }

    private static void faces(VertexConsumer vc, Pose p, AABB b, int c) {
        double x0 = b.minX;
        double y0 = b.minY;
        double z0 = b.minZ;
        double x1 = b.maxX;
        double y1 = b.maxY;
        double z1 = b.maxZ;
        double[][] q = new double[][]{
            {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1},
            {x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0},
            {x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0},
            {x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1},
            {x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0},
            {x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1}
        };

        for (double[] f : q) {
            for (int i = 0; i < 4; i++) {
                vertex(vc, p, f[i * 3], f[i * 3 + 1], f[i * 3 + 2], c);
            }
        }
    }

    private static void line(VertexConsumer vc, Pose p, Vec3 a, Vec3 b, int c) {
        Vec3 n = b.subtract(a);
        if (!(n.lengthSqr() < 1.0E-10)) {
            n = n.normalize();

            for (Vec3 v : new Vec3[]{a, b}) {
                vc.vertex(p.pose(), (float)v.x, (float)v.y, (float)v.z)
                    .color(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, c >>> 24)
                    .normal(p.normal(), (float)n.x, (float)n.y, (float)n.z)
                    .endVertex();
            }
        }
    }

    private static void edges(VertexConsumer vc, Pose p, AABB b, int c) {
        double x0 = b.minX;
        double y0 = b.minY;
        double z0 = b.minZ;
        double x1 = b.maxX;
        double y1 = b.maxY;
        double z1 = b.maxZ;
        double[][] e = new double[][]{
            {x0, y0, z0, x1, y0, z0},
            {x0, y1, z0, x1, y1, z0},
            {x0, y0, z1, x1, y0, z1},
            {x0, y1, z1, x1, y1, z1},
            {x0, y0, z0, x0, y1, z0},
            {x1, y0, z0, x1, y1, z0},
            {x0, y0, z1, x0, y1, z1},
            {x1, y0, z1, x1, y1, z1},
            {x0, y0, z0, x0, y0, z1},
            {x1, y0, z0, x1, y0, z1},
            {x0, y1, z0, x0, y1, z1},
            {x1, y1, z0, x1, y1, z1}
        };

        for (double[] l : e) {
            line(vc, p, new Vec3(l[0], l[1], l[2]), new Vec3(l[3], l[4], l[5]), c);
        }
    }

    private static void sphere(VertexConsumer vc, Pose p, Vec3 c, double r, int color) {
        int n = 40;

        for (int ring = 0; ring < 3; ring++) {
            Vec3 prev = null;

            for (int i = 0; i <= n; i++) {
                double t = (Math.PI * 2) * i / n;
                double s = Math.sin(t) * r;
                double k = Math.cos(t) * r;

                Vec3 v = switch (ring) {
                    case 0 -> c.add(k, 0.0, s);
                    case 1 -> c.add(k, s, 0.0);
                    default -> c.add(0.0, s, k);
                };
                if (prev != null) {
                    line(vc, p, prev, v, color);
                }

                prev = v;
            }
        }
    }

    private static void text(JsonObject d, PoseStack ps, Vec3 at, BufferSource src, boolean through) {
        Minecraft mc = Minecraft.getInstance();
        String s = d.has("text") ? d.get("text").getAsString() : "";
        float scale = 0.025F * num(d, "scale", 1.0F);
        ps.pushPose();
        ps.translate(at.x, at.y, at.z);
        Mc.mulPose(ps, Axes.YP.rotationDegrees(-camera.getYRot()));
        Mc.mulPose(ps, Axes.XP.rotationDegrees(camera.getXRot()));
        ps.scale(-scale, -scale, scale);
        int w = mc.font.width(s);
        Mc.text(mc.font, s, -w / 2.0F, 0.0F, argb(d, "color", -1), false, ps.last(), src, through, 1073741824, 15728880);
        ps.popPose();
        src.endBatch();
    }

    private static void drawScreen(Gfx g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (!GIZMOS.isEmpty() && mc.player != null && !mc.options.hideGui) {
            prune();

            for (GizmoClient.Gizmo gz : GIZMOS.values()) {
                JsonObject d = gz.d;
                String k = kind(d);
                if (k.startsWith("screen")) {
                    int x = Math.round(num(d, "x", 0.0F) / 100.0F * sw);
                    int y = Math.round(num(d, "y", 0.0F) / 100.0F * sh);
                    int color = argb(d, "color", -1);
                    switch (k) {
                        case "screen_text":
                            float sc = num(d, "scale", 1.0F);
                            g.pose().pushPose();
                            g.pose().translate((float)x, (float)y, 0.0F);
                            g.pose().scale(sc, sc, 1.0F);
                            g.drawString(mc.font, d.get("text").getAsString(), 0, 0, color, true);
                            g.pose().popPose();
                            break;
                        case "screen_rect": {
                            int w = Math.round(num(d, "w", 10.0F) / 100.0F * sw);
                            int h = Math.round(num(d, "h", 10.0F) / 100.0F * sh);
                            int t = Math.max(1, Math.round(num(d, "width", 1.0F)));
                            int fill = argb(d, "fill", 0);
                            if (fill >>> 24 != 0) {
                                g.fill(x, y, x + w, y + h, fill);
                            }

                            if (color >>> 24 != 0) {
                                g.fill(x, y, x + w, y + t, color);
                                g.fill(x, y + h - t, x + w, y + h, color);
                                g.fill(x, y, x + t, y + h, color);
                                g.fill(x + w - t, y, x + w, y + h, color);
                            }
                            break;
                        }
                        case "screen_line": {
                            int x2 = Math.round(num(d, "x2", 0.0F) / 100.0F * sw);
                            int y2 = Math.round(num(d, "y2", 0.0F) / 100.0F * sh);
                            float t = Math.max(1.0F, num(d, "width", 1.0F));
                            double len = Math.hypot(x2 - x, y2 - y);
                            g.pose().pushPose();
                            g.pose().translate((float)x, (float)y, 0.0F);
                            Mc.mulPose(g.pose(), Axes.ZP.rotation((float)Math.atan2(y2 - y, x2 - x)));
                            g.fill(0, Math.round(-t / 2.0F), (int)Math.round(len), Math.max(1, Math.round(t / 2.0F)), color);
                            g.pose().popPose();
                        }
                    }
                }
            }
        }
    }

    private record Gizmo(int script, JsonObject d, long until) {
    }

    private static final class Tint implements VertexConsumer {
        static final GizmoClient.Tint NOTHING = new GizmoClient.Tint(null, 0);
        private final VertexConsumer to;
        private final int r;
        private final int g;
        private final int b;
        private final int a;
        private boolean colored;

        Tint(VertexConsumer to, int argb) {
            this.to = to;
            this.a = argb >>> 24;
            this.r = argb >> 16 & 0xFF;
            this.g = argb >> 8 & 0xFF;
            this.b = argb & 0xFF;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            if (this.to != null) {
                this.to.vertex(x, y, z);
            }

            this.colored = false;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            if (this.to != null) {
                this.to.color(this.r, this.g, this.b, this.a);
            }

            this.colored = true;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
            if (this.to != null) {
                if (!this.colored) {
                    this.to.color(this.r, this.g, this.b, this.a);
                }

                this.to.endVertex();
            }
        }

        @Override
        public void vertex(
            float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light, float nx, float ny, float nz
        ) {
            if (this.to != null) {
                this.to.vertex(x, y, z).color(this.r, this.g, this.b, this.a).endVertex();
            }
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }

    private record Tinted(VertexConsumer to, int color) implements MultiBufferSource {
        @Override
        public VertexConsumer getBuffer(RenderType type) {
            String n = type.toString();
            boolean body = (n.contains("entity") || n.contains("armor") || n.contains("item")) && !n.contains("shadow") && !n.contains("glint");
            return body ? new GizmoClient.Tint(this.to, this.color) : GizmoClient.Tint.NOTHING;
        }
    }
}
