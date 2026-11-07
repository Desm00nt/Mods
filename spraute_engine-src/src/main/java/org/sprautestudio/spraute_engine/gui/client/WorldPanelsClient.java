package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Matrix4f;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.PanelLook;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.models.BoneWorld;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class WorldPanelsClient {
    private static final int MAX_PIXELS = 2048;
    private static final int SHARP_BELOW = 512;
    private static final float IDLE_FRAME = 0.033333335F;
    private static final int OUT_OF_SIGHT = -10000;
    private static final int BUTTON_LEFT = 0;
    private static final int BUTTON_RIGHT = 1;
    private static final int PRESS = 1;
    private static final int RELEASE = 0;
    private static final long PREVIEW_LIFE = 500000000L;
    private static final double KEEP_SIZE_AT = 8.0;
    private static final double SNAP_SQR = 256.0;
    private static final double BOB_SECONDS = 3.0;
    private static final Map<String, WorldPanelsClient.Panel> PANELS = new LinkedHashMap<>();
    private static final Map<String, WorldPanelsClient.Panel> PREVIEWS = new LinkedHashMap<>();
    private static WorldPanelsClient.Panel aimed;
    private static WorldPanelsClient.Panel pressed;
    private static int pressedButton;
    private static long lastFrame = System.nanoTime();

    private WorldPanelsClient() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_TICK.add(WorldPanelsClient::onRenderTick);
        ClientHooks.RENDER_LEVEL.add(WorldPanelsClient::onRenderLevel);
        ClientHooks.CLIENT_TICK.add(WorldPanelsClient::onTick);
        ClientHooks.MOUSE_BUTTON.add(WorldPanelsClient::onMouse);
        ClientHooks.MOUSE_SCROLL.add(WorldPanelsClient::onScroll);
        ClientHooks.LOGGING_OUT.add(e -> clear());
    }

    private static void clear() {
        for (WorldPanelsClient.Panel p : PANELS.values()) {
            p.drop();
        }

        PANELS.clear();
        previewClear();
        aimed = null;
        pressed = null;
    }

    public static void show(Packets.WorldPanelShow m) {
        WorldPanelsClient.Panel p = PANELS.get(m.id());
        if (p == null || !p.json.equals(m.json())) {
            if (p != null) {
                p.drop();
            }

            p = new WorldPanelsClient.Panel();
            p.view = view(m.json(), m.project());
            p.json = m.json();
            PANELS.put(m.id(), p);
            WorldPanelsClient.Panel panel = p;
            p.view.listener = (element, kind) -> event(panel, element, kind.equals("close") ? "panel_close:" + panel.id : kind);
        }

        p.id = m.id();
        p.scriptId = m.scriptId();
        p.name = m.name();
        p.project = m.project();
        p.dimension = m.dimension();
        p.x = m.x();
        p.y = m.y();
        p.z = m.z();
        p.yaw = m.yaw();
        p.pitch = m.pitch();
        p.width = m.width();
        p.reach = m.reach();
        p.range = m.range();
        p.face = m.face();
        p.look = m.look();
        p.self = m.self();
        p.bone = m.bone();
        p.offsetX = m.offsetX();
        p.offsetY = m.offsetY();
        p.offsetZ = m.offsetZ();
        if (p.entity != m.entity()) {
            p.followed = null;
        }

        p.entity = m.entity();
        p.size();
    }

    public static void hide(Packets.WorldPanelHide m) {
        List<String> gone = new ArrayList<>();

        for (Entry<String, WorldPanelsClient.Panel> e : PANELS.entrySet()) {
            if (e.getValue().scriptId == m.scriptId() && (m.id().isEmpty() || e.getKey().equals(m.id()))) {
                gone.add(e.getKey());
            }
        }

        for (String id : gone) {
            WorldPanelsClient.Panel p = PANELS.remove(id);
            p.drop();
            if (aimed == p) {
                aimed = null;
            }

            if (pressed == p) {
                pressed = null;
            }
        }
    }

    public static void update(Packets.GuiUpdate m) {
        int slash = m.element().indexOf(47);
        WorldPanelsClient.Panel only = slash > 0 ? PANELS.get(m.element().substring(0, slash)) : null;
        if (only != null) {
            if (only.scriptId == m.scriptId()) {
                only.view.update(m.element().substring(slash + 1), m.prop(), m.value());
            }
        } else {
            for (WorldPanelsClient.Panel p : PANELS.values()) {
                if (p.scriptId == m.scriptId()) {
                    p.view.update(m.element(), m.prop(), m.value());
                }
            }
        }
    }

    private static GuiView view(String json, String project) {
        GuiView v = new GuiView(GuiDoc.fromJson(json), project);
        v.freeMouse = true;
        v.restartAppear();
        return v;
    }

    private static void event(WorldPanelsClient.Panel p, String element, String kind) {
        if (!p.preview) {
            SprauteNetwork.sendToServer(new Packets.GuiEvent(p.scriptId, p.name, element, kind, p.view.inputs()));
        }
    }

    public static void preview(
        String key, Project project, String json, double x, double y, double z, float yaw, float pitch, float width, boolean face, PanelLook look
    ) {
        WorldPanelsClient.Panel p = PREVIEWS.get(key);
        if (p == null || !p.json.equals(json)) {
            if (p != null) {
                p.drop();
            }

            p = new WorldPanelsClient.Panel();
            p.preview = true;
            p.json = json;
            p.view = view(json, project.name());
            p.view.localProject = project;
            p.view.interactive = false;
            p.view.playAppear = false;
            p.size();
            PREVIEWS.put(key, p);
        }

        Minecraft mc = Minecraft.getInstance();
        p.dimension = mc.level != null ? mc.level.dimension().location().toString() : "";
        p.x = x;
        p.y = y;
        p.z = z;
        p.yaw = yaw;
        p.pitch = pitch;
        p.width = width;
        p.face = face;
        p.look = look;
        p.seenAt = System.nanoTime();
    }

    public static void previewClear() {
        for (WorldPanelsClient.Panel p : PREVIEWS.values()) {
            p.drop();
        }

        PREVIEWS.clear();
    }

    public static double[] bounds(double x, double y, double z, float yaw, float pitch, float width, float height) {
        Vec3[] axes = axes(yaw, pitch);
        Vec3 origin = new Vec3(x, y, z);
        Vec3 side = axes[0].scale(width / 2.0);
        Vec3 top = axes[1].scale(height);
        double[] out = new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};

        for (Vec3 c : new Vec3[]{origin.add(side), origin.subtract(side), origin.add(side).add(top), origin.subtract(side).add(top)}) {
            out[0] = Math.min(out[0], c.x);
            out[1] = Math.min(out[1], c.y);
            out[2] = Math.min(out[2], c.z);
            out[3] = Math.max(out[3], c.x);
            out[4] = Math.max(out[4], c.y);
            out[5] = Math.max(out[5], c.z);
        }

        return out;
    }

    private static Vec3[] axes(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        Vec3 normal = new Vec3(-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p));
        Vec3 look = normal.reverse();
        Vec3 right = new Vec3(-look.z, 0.0, look.x);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(Math.cos(y), 0.0, Math.sin(y)) : right.normalize();
        Vec3 up = right.cross(look).normalize();
        return new Vec3[]{right, up, normal};
    }

    private static void follow(WorldPanelsClient.Panel p, Minecraft mc, float partial) {
        if (p.entity != -1) {
            p.wearer = mc.level != null ? mc.level.getEntity(p.entity) : null;
            if (p.wearer == null) {
                p.followed = null;
            } else {
                Entity e = p.wearer;
                Vec3 feet = e.getPosition(partial);
                p.wearerYaw = e instanceof LivingEntity le ? Mth.rotLerp(partial, le.yBodyRotO, le.yBodyRot) : e.getViewYRot(partial);
                Vec3 anchor = feet.add(0.0, e.getBbHeight(), 0.0);
                if (!p.bone.isEmpty()) {
                    try {
                        anchor = BoneWorld.position(e, p.bone, feet, 0.0, 0.0, 0.0);
                    } catch (RuntimeException var7) {
                    }
                }

                double[] step = PanelLook.offset(p.wearerYaw, p.offsetX, p.offsetY, p.offsetZ);
                p.x = anchor.x + step[0];
                p.y = anchor.y + step[1];
                p.z = anchor.z + step[2];
            }
        }
    }

    private static void place(WorldPanelsClient.Panel p, Vec3 eye, float dt, long now) {
        Vec3 want = new Vec3(p.x, p.y, p.z);
        if (p.followed != null && !(p.look.smooth() <= 0.0F) && !(p.followed.distanceToSqr(want) > 256.0)) {
            p.followed = p.followed.add(want.subtract(p.followed).scale(1.0 - Math.exp(-dt / p.look.smooth())));
        } else {
            p.followed = want;
        }

        p.origin = p.look.bob() == 0.0F ? p.followed : p.followed.add(0.0, p.look.bob() * Math.sin(now / 1.0E9 * 2.0 * Math.PI / 3.0), 0.0);
        p.shownWidth = p.look.keepSize() ? (float)(p.width * eye.distanceTo(p.origin) / 8.0) : p.width;
        float yaw = p.yaw;
        float pitch = p.pitch;
        String turn = !p.look.turn().isEmpty() ? p.look.turn() : (!p.face && p.entity == -1 ? "fixed" : "viewer");
        double dx = eye.x - p.origin.x;
        double dy = eye.y - p.origin.y - p.height() / 2.0F;
        double dz = eye.z - p.origin.z;
        switch (turn) {
            case "viewer":
                yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
                pitch = 0.0F;
                break;
            case "camera":
                yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
                pitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
                break;
            case "body":
                if (p.wearer != null) {
                    yaw += p.wearerYaw;
                }
                break;
            case "bone":
                if (p.wearer != null && !p.bone.isEmpty()) {
                    try {
                        Vec3 f = BoneWorld.forward(p.wearer, p.bone);
                        yaw = (float)Math.toDegrees(Math.atan2(-f.x, f.z)) + p.yaw;
                        pitch = (float)(-Math.toDegrees(Math.asin(Mth.clamp(f.y, -1.0, 1.0)))) + p.pitch;
                    } catch (RuntimeException var18) {
                    }
                }
        }

        Vec3[] a = axes(yaw, pitch);
        p.right = a[0];
        p.up = a[1];
        p.normal = a[2];
        if (eye.subtract(p.origin.add(p.up.scale(p.height() / 2.0F))).dot(p.normal) < 0.0) {
            p.right = p.right.reverse();
            p.normal = p.normal.reverse();
        }
    }

    private static boolean own(WorldPanelsClient.Panel p, Minecraft mc) {
        return p.wearer != null && p.wearer == mc.player;
    }

    private static boolean visible(WorldPanelsClient.Panel p, Minecraft mc, Vec3 eye) {
        if (mc.level == null || !p.dimension.equals(mc.level.dimension().location().toString())) {
            return false;
        } else if (p.preview) {
            return true;
        } else {
            if (p.entity != -1) {
                if (p.wearer == null) {
                    return false;
                }

                if (own(p, mc) && !p.self && mc.options.getCameraType().isFirstPerson()) {
                    return false;
                }
            }

            return eye.distanceToSqr(p.origin) < p.range * p.range;
        }
    }

    private static void aim(Minecraft mc, float partial) {
        WorldPanelsClient.Panel best = null;
        double bestAt = Double.MAX_VALUE;
        LocalPlayer player = mc.player;
        if (player != null && mc.screen == null) {
            Vec3 eye = player.getEyePosition(partial);
            Vec3 dir = player.getViewVector(partial);

            for (WorldPanelsClient.Panel p : PANELS.values()) {
                if (visible(p, mc, eye) && (!own(p, mc) || p.self)) {
                    double facing = dir.dot(p.normal);
                    if (!(facing >= -1.0E-4)) {
                        double t = p.origin.subtract(eye).dot(p.normal) / facing;
                        if (!(t <= 0.0) && !(t > p.reach) && !(t >= bestAt)) {
                            Vec3 d = eye.add(dir.scale(t)).subtract(p.origin);
                            double u = d.dot(p.right) / p.shownWidth + 0.5;
                            double v = 1.0 - d.dot(p.up) / p.height();
                            if (!(u < 0.0) && !(u > 1.0) && !(v < 0.0) && !(v > 1.0)) {
                                best = p;
                                bestAt = t;
                                p.aimU = u;
                                p.aimV = v;
                                p.aimDistance = t;
                            }
                        }
                    }
                }
            }

            HitResult hit = mc.hitResult;
            boolean ownWearer = best != null && hit instanceof EntityHitResult on && on.getEntity() == best.wearer;
            if (best != null
                && !best.look.through()
                && !ownWearer
                && hit != null
                && hit.getType() != Type.MISS
                && hit.getLocation().distanceTo(eye) < bestAt - 0.05) {
                best = null;
            }
        }

        if (aimed != best && aimed != null) {
            leave(aimed);
        }

        aimed = best;
        if (aimed != null) {
            GuiDoc.Element e = aimed.view.elementAt(aimed.view.toDesignX(pixelX(aimed)), aimed.view.toDesignY(pixelY(aimed)), false);
            String element = e != null ? e.id : "";
            if (!element.equals(aimed.hoverElement)) {
                if (!aimed.hoverElement.isEmpty()) {
                    event(aimed, aimed.hoverElement, "panel_leave:" + aimed.id);
                }

                aimed.hoverElement = element;
                if (!element.isEmpty()) {
                    event(aimed, element, "panel_hover:" + aimed.id);
                }
            }
        }
    }

    private static void leave(WorldPanelsClient.Panel p) {
        if (!p.hoverElement.isEmpty()) {
            event(p, p.hoverElement, "panel_leave:" + p.id);
        }

        p.hoverElement = "";
    }

    private static double pixelX(WorldPanelsClient.Panel p) {
        return p.aimU * p.pixelsW;
    }

    private static double pixelY(WorldPanelsClient.Panel p) {
        return p.aimV * p.pixelsH;
    }

    private static void onRenderTick(ClientHooks.RenderTick e) {
        if (e.phase == TickPhase.START) {
            long now = System.nanoTime();
            float dt = Math.min(0.1F, (float)(now - lastFrame) / 1.0E9F);
            lastFrame = now;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null) {
                PREVIEWS.values().removeIf(px -> {
                    if (now - px.seenAt < 500000000L) {
                        return false;
                    } else {
                        px.drop();
                        return true;
                    }
                });
                if (!PANELS.isEmpty() || !PREVIEWS.isEmpty()) {
                    Vec3 eye = mc.player.getEyePosition(e.renderTickTime);
                    List<WorldPanelsClient.Panel> all = new ArrayList<>(PANELS.values());
                    all.addAll(PREVIEWS.values());
                    Vec3 seenFrom = mc.gameRenderer.getMainCamera().getPosition();

                    for (WorldPanelsClient.Panel p : all) {
                        follow(p, mc, e.renderTickTime);
                        place(p, seenFrom, dt, now);
                    }

                    aim(mc, e.renderTickTime);
                    List<WorldPanelsClient.Panel> due = new ArrayList<>();

                    for (WorldPanelsClient.Panel p : all) {
                        if (visible(p, mc, eye) && (p == aimed || p.target == null || (float)(now - p.drawnAt) / 1.0E9F >= 0.033333335F)) {
                            due.add(p);
                        }
                    }

                    if (!due.isEmpty()) {
                        RenderSystem.backupProjectionMatrix();
                        PoseStack modelView = RenderSystem.getModelViewStack();
                        modelView.pushPose();
                        modelView.setIdentity();
                        modelView.translate(0.0F, 0.0F, -2000.0F);
                        RenderSystem.applyModelViewMatrix();
                        Lighting.setupFor3DItems();

                        for (WorldPanelsClient.Panel px : due) {
                            draw(mc, px, now, dt);
                        }

                        modelView.popPose();
                        RenderSystem.applyModelViewMatrix();
                        RenderSystem.restoreProjectionMatrix();
                        mc.getMainRenderTarget().bindWrite(true);
                    }
                }
            }
        }
    }

    private static void draw(Minecraft mc, WorldPanelsClient.Panel p, long now, float dt) {
        if (p.target == null) {
            p.target = new TextureTarget(p.pixelsW, p.pixelsH, true, Minecraft.ON_OSX);
            p.target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        float passed = p.drawnAt == 0L ? dt : Math.min(0.1F, (float)(now - p.drawnAt) / 1.0E9F);
        p.drawnAt = now;
        p.target.clear(Minecraft.ON_OSX);
        p.target.bindWrite(true);
        Mc.guiProjection(p.pixelsW, p.pixelsH);
        Gfx.beginOffscreen(p.pixelsH);

        try {
            Gfx g = Gfx.offscreen();
            Canvas c = new Canvas(g, mc.font, 1.0F);
            p.view.bounds(0, 0, p.pixelsW, p.pixelsH);
            p.view.layout();
            boolean aim = p == aimed;
            p.view.render(c, aim ? (int)pixelX(p) : -10000, aim ? (int)pixelY(p) : -10000, passed);
            c.flush();
        } catch (RuntimeException var12) {
            SprauteEngine.LOGGER.warn("Spraute: world interface {}", p.id, var12);
        } finally {
            Gfx.endOffscreen();
        }
    }

    private static void onRenderLevel(ClientHooks.RenderLevel e) {
        if (e.getStage() == ClientHooks.LevelStage.AFTER_PARTICLES && (!PANELS.isEmpty() || !PREVIEWS.isEmpty())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                Vec3 cam = e.getCamera().getPosition();
                Vec3 eye = mc.player.getEyePosition(e.getPartialTick());
                PoseStack modelView = RenderSystem.getModelViewStack();
                modelView.pushPose();
                modelView.setIdentity();
                RenderSystem.applyModelViewMatrix();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
                RenderSystem.setShader(GameRenderer::getPositionTexShader);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                PoseStack pose = e.getPoseStack();

                for (WorldPanelsClient.Panel p : PANELS.values()) {
                    if (p.target != null && visible(p, mc, eye)) {
                        quad(p, cam, eye, pose);
                    }
                }

                for (WorldPanelsClient.Panel px : PREVIEWS.values()) {
                    if (px.target != null && visible(px, mc, eye)) {
                        quad(px, cam, eye, pose);
                    }
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                RenderSystem.enableDepthTest();
                RenderSystem.enableCull();
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableBlend();
                modelView.popPose();
                RenderSystem.applyModelViewMatrix();
            }
        }
    }

    private static void quad(WorldPanelsClient.Panel p, Vec3 cam, Vec3 eye, PoseStack pose) {
        float clear = p.look.clearness(eye.distanceTo(p.origin), p.range);
        if (!(clear <= 0.001F)) {
            int tint = p.look.tint();
            RenderSystem.setShaderColor((tint >> 16 & 0xFF) / 255.0F * clear, (tint >> 8 & 0xFF) / 255.0F * clear, (tint & 0xFF) / 255.0F * clear, clear);
            if (p.look.through()) {
                RenderSystem.disableDepthTest();
            } else {
                RenderSystem.enableDepthTest();
            }

            Matrix4f matrix = pose.last().pose();
            Vec3 side = p.right.scale(p.shownWidth / 2.0);
            Vec3 top = p.up.scale(p.height());
            Vec3 o = p.origin.subtract(cam);
            Vec3 lb = o.subtract(side);
            Vec3 rb = o.add(side);
            Vec3 rt = rb.add(top);
            Vec3 lt = lb.add(top);
            RenderSystem.setShaderTexture(0, p.target.getColorTextureId());
            BufferBuilder b = Tesselator.getInstance().getBuilder();
            b.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            b.vertex(matrix, (float)lb.x, (float)lb.y, (float)lb.z).uv(0.0F, 0.0F).endVertex();
            b.vertex(matrix, (float)rb.x, (float)rb.y, (float)rb.z).uv(1.0F, 0.0F).endVertex();
            b.vertex(matrix, (float)rt.x, (float)rt.y, (float)rt.z).uv(1.0F, 1.0F).endVertex();
            b.vertex(matrix, (float)lt.x, (float)lt.y, (float)lt.z).uv(0.0F, 1.0F).endVertex();
            BufferUploader.drawWithShader(b.end());
        }
    }

    private static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END) {
            for (WorldPanelsClient.Panel p : PANELS.values()) {
                p.view.tick();
            }
        }
    }

    private static void onMouse(ClientHooks.MouseButton e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) {
            if (e.getAction() == 0) {
                if (pressed != null && e.getButton() == pressedButton) {
                    WorldPanelsClient.Panel p = pressed;
                    pressed = null;
                    p.view.mouseReleased(pixelX(p), pixelY(p), e.getButton());
                    e.setCanceled(true);
                }
            } else if (e.getAction() == 1 && aimed != null && (e.getButton() == 0 || e.getButton() == 1)) {
                e.setCanceled(true);
                WorldPanelsClient.Panel p = aimed;
                pressed = p;
                pressedButton = e.getButton();
                event(p, p.hoverElement, "panel_click:" + (e.getButton() == 0 ? "left" : "right") + ":" + p.id);
                p.view.mousePressed(pixelX(p), pixelY(p), e.getButton());
                if (p.view.hasFocusedInput()) {
                    mc.setScreen(new PanelInputScreen(p.view, () -> event(p, "", "panel_input:" + p.id)));
                }
            }
        }
    }

    private static void onScroll(ClientHooks.MouseScroll e) {
        if (Minecraft.getInstance().screen == null && aimed != null) {
            if (aimed.view.mouseScrolled(pixelX(aimed), pixelY(aimed), e.getScrollDelta())) {
                e.setCanceled(true);
            }
        }
    }

    private static final class Panel {
        String id = "";
        String name = "";
        String project = "";
        String json = "";
        String dimension = "";
        int scriptId = -1;
        boolean preview;
        GuiView view;
        double x;
        double y;
        double z;
        float yaw;
        float pitch;
        float width = 3.0F;
        float shownWidth = 3.0F;
        float reach = 6.0F;
        float range = 64.0F;
        boolean face;
        boolean self;
        PanelLook look = PanelLook.PLAIN;
        int entity = -1;
        Entity wearer;
        String bone = "";
        float offsetX;
        float offsetY;
        float offsetZ;
        float wearerYaw;
        Vec3 followed;
        RenderTarget target;
        int pixelsW;
        int pixelsH;
        long drawnAt;
        long seenAt;
        double aimU;
        double aimV;
        double aimDistance;
        String hoverElement = "";
        Vec3 origin = Vec3.ZERO;
        Vec3 right = new Vec3(1.0, 0.0, 0.0);
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 normal = new Vec3(0.0, 0.0, 1.0);

        float height() {
            return this.shownWidth * this.pixelsH / Math.max(1, this.pixelsW);
        }

        void size() {
            int w = this.view.doc.width();
            int h = this.view.doc.height();
            int k = Math.max(w, h) < 512 && !this.view.doc.get("layout").equals("screen") ? 2 : 1;
            float fit = Math.min(1.0F, 2048.0F / Math.max(w, h) / k);
            this.pixelsW = Math.max(16, Math.round(w * k * fit));
            this.pixelsH = Math.max(16, Math.round(h * k * fit));
        }

        void drop() {
            if (this.target != null) {
                this.target.destroyBuffers();
            }

            this.target = null;
        }
    }
}
