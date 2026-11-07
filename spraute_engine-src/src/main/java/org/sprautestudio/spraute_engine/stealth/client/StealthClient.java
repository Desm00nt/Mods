package org.sprautestudio.spraute_engine.stealth.client;

import com.google.gson.Gson;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.stealth.Stealth;
import org.sprautestudio.spraute_engine.stealth.StealthShape;

public final class StealthClient {
    private static final double RAY_STEP = 0.06544984694978735;
    private static final double RING_STEP = 0.75;
    private static final double RIM = 0.25;
    private static final double LIFT = 0.03;
    private static final double MAX_SEEN = 96.0;
    private static List<Stealth.Look> looks = List.of();

    private StealthClient() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(StealthClient::render);
    }

    public static void receive(String json) {
        Stealth.Look[] got = new Gson().fromJson(json, Stealth.Look[].class);
        looks = got == null ? List.of() : List.of(got);
    }

    private static void render(ClientHooks.RenderLevel e) {
        if (e.getStage() == ClientHooks.LevelStage.AFTER_PARTICLES && !looks.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null && !mc.options.hideGui) {
                Vec3 cam = e.getCamera().getPosition();
                List<Stealth.Look> near = new ArrayList<>();

                for (Stealth.Look l : looks) {
                    Entity guard = mc.level.getEntity(l.id);
                    if (guard != null && guard.distanceToSqr(cam) <= 9216.0) {
                        near.add(l);
                    }
                }

                if (!near.isEmpty()) {
                    float partial = mc.getFrameTime();
                    BufferBuilder b = Tesselator.getInstance().getBuilder();
                    b.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

                    for (Stealth.Look lx : near) {
                        Entity guard = mc.level.getEntity(lx.id);
                        double k = mc.player.isShiftKeyDown() ? lx.sneak : 1.0;
                        shape(
                            b,
                            mc.level,
                            lx,
                            guard.getPosition(partial).subtract(cam),
                            guard.getPosition(partial),
                            guard.getViewYRot(partial),
                            lx.range * k,
                            lx.width * k
                        );
                    }

                    PoseStack view = RenderSystem.getModelViewStack();
                    view.pushPose();
                    view.mulPoseMatrix(e.getPoseStack().last().pose());
                    RenderSystem.applyModelViewMatrix();
                    RenderSystem.setShader(GameRenderer::getPositionColorShader);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.enableDepthTest();
                    RenderSystem.disableCull();
                    RenderSystem.depthMask(false);
                    BufferUploader.drawWithShader(b.end());
                    RenderSystem.depthMask(true);
                    RenderSystem.enableCull();
                    RenderSystem.disableBlend();
                    view.popPose();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        }
    }

    private static void shape(BufferBuilder b, ClientLevel level, Stealth.Look l, Vec3 rel, Vec3 at, float yaw, double range, double width) {
        double span = StealthShape.span(l.shape, range, l.angle, width);
        int rays = Math.max(6, (int)Math.ceil(span * 2.0 / 0.06544984694978735));
        int bands = Math.max(2, (int)Math.ceil(Math.hypot(range, width / 2.0) / 0.75));
        float red = (l.color >> 16 & 0xFF) / 255.0F;
        float green = (l.color >> 8 & 0xFF) / 255.0F;
        float blue = (l.color & 0xFF) / 255.0F;
        float pulse = l.alert > 0.0F ? 0.5F + 0.5F * Mth.sin((float)System.currentTimeMillis() / (140.0F - 90.0F * l.alert)) : 0.0F;
        float inner = 0.36F + 0.3F * l.alert * pulse;
        float outer = 0.14F + 0.3F * l.alert * pulse;
        float rim = 0.8F;
        double yawRad = Math.toRadians(yaw);
        double[][] last = null;

        for (int i = 0; i <= rays; i++) {
            double theta = -span + span * 2.0 * i / rays;
            double len = StealthShape.reach(l.shape, theta * 0.999, range, l.angle, width);
            double dx = -Math.sin(yawRad + theta);
            double dz = Math.cos(yawRad + theta);
            if (!l.through && len > 0.0) {
                len = clipped(level, at, dx, dz, len);
            }

            double[][] now = new double[bands + 2][3];
            double y = at.y;

            for (int r = 0; r <= bands + 1; r++) {
                double d = r <= bands ? Math.max(0.0, len - 0.25) * r / bands : len;
                y = ground(level, at.x + dx * d, at.z + dz * d, y);
                now[r][0] = rel.x + dx * d;
                now[r][1] = rel.y + (y - at.y) + 0.03;
                now[r][2] = rel.z + dz * d;
            }

            if (last != null) {
                for (int r = 0; r <= bands; r++) {
                    float a0 = r == bands ? rim : Mth.lerp((float)r / bands, inner, outer);
                    float a1 = r >= bands ? rim : Mth.lerp((float)(r + 1) / bands, inner, outer);
                    put(b, last[r], red, green, blue, a0);
                    put(b, now[r], red, green, blue, a0);
                    put(b, now[r + 1], red, green, blue, a1);
                    put(b, last[r + 1], red, green, blue, a1);
                }
            }

            last = now;
        }
    }

    private static void put(BufferBuilder b, double[] v, float r, float g, float bl, float a) {
        b.vertex(v[0], v[1], v[2]).color(r, g, bl, a).endVertex();
    }

    private static double clipped(ClientLevel level, Vec3 at, double dx, double dz, double len) {
        Vec3 from = at.add(0.0, 1.0, 0.0);
        Vec3 to = from.add(dx * len, 0.0, dz * len);
        BlockHitResult hit = level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, Minecraft.getInstance().player));
        return hit.getType() == Type.MISS ? len : hit.getLocation().distanceTo(from);
    }

    private static double ground(ClientLevel level, double x, double z, double near) {
        MutableBlockPos p = new MutableBlockPos();
        int xi = Mth.floor(x);
        int zi = Mth.floor(z);
        int y = Mth.floor(near + 1.2);

        for (int bottom = Mth.floor(near - 3.0); y >= bottom; y--) {
            p.set(xi, y, zi);
            VoxelShape s = level.getBlockState(p).getCollisionShape(level, p);
            if (!s.isEmpty()) {
                return y + s.max(Axis.Y);
            }
        }

        return near;
    }
}
