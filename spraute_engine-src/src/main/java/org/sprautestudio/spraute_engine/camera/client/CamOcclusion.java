package org.sprautestudio.spraute_engine.camera.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.ClientHooks;

public final class CamOcclusion {
    private static final double END_MARGIN = 0.9;
    private static final int MAX_BLOCKS = 2048;
    private static final int FLAGS = 18;
    private static final Map<BlockPos, BlockState> HIDDEN = new HashMap<>();
    private static float ghost;

    private CamOcclusion() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(CamOcclusion::onRender);
        ClientHooks.LOGGING_OUT.add(e -> forget());
    }

    private static void forget() {
        HIDDEN.clear();
    }

    private static void restoreAll() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            HIDDEN.forEach((pos, state) -> restore(mc, pos, state));
        }

        HIDDEN.clear();
    }

    private static void restore(Minecraft mc, BlockPos pos, BlockState state) {
        if (mc.level.isLoaded(pos) && mc.level.getBlockState(pos).isAir()) {
            mc.level.setBlock(pos, state, 18);
        }
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        CamProfile r = PlayerCam.active();
        double[] cam = PlayerCam.shownPose();
        if (mc.level != null && mc.player != null && r != null && r.on("hide_blocks") && cam != null && PlayerCam.detached()) {
            ghost = (float)r.num("hide_alpha");
            Vec3 from = new Vec3(cam[0], cam[1], cam[2]);
            Vec3 to = mc.player.getEyePosition(1.0F);
            apply(mc, collect(mc, from, to, r.num("hide_radius")));
        } else {
            if (!HIDDEN.isEmpty()) {
                restoreAll();
            }
        }
    }

    private static Set<BlockPos> collect(Minecraft mc, Vec3 from, Vec3 to, double radius) {
        Set<BlockPos> out = new HashSet<>();
        double dist = from.distanceTo(to);
        if (dist < 1.0) {
            return out;
        } else {
            double pad = radius + 0.51;
            double limitSq = pad * pad;
            double end = dist - 0.9;
            Vec3 ab = to.subtract(from);
            Vec3 dir = ab.normalize();
            double len2 = ab.lengthSqr();
            int minX = Mth.floor(Math.min(from.x, to.x) - pad);
            int maxX = Mth.ceil(Math.max(from.x, to.x) + pad);
            int minY = Mth.floor(Math.min(from.y, to.y) - pad);
            int maxY = Mth.ceil(Math.max(from.y, to.y) + pad);
            int minZ = Mth.floor(Math.min(from.z, to.z) - pad);
            int maxZ = Mth.ceil(Math.max(from.z, to.z) + pad);

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (out.size() >= 2048) {
                            return out;
                        }

                        double px = x + 0.5;
                        double py = y + 0.5;
                        double pz = z + 0.5;
                        double t = Math.max(0.0, Math.min(1.0, ((px - from.x) * ab.x + (py - from.y) * ab.y + (pz - from.z) * ab.z) / len2));
                        double qx = from.x + ab.x * t;
                        double qy = from.y + ab.y * t;
                        double qz = from.z + ab.z * t;
                        double dx = px - qx;
                        double dy = py - qy;
                        double dz = pz - qz;
                        if (!(dx * dx + dy * dy + dz * dz > limitSq)) {
                            double along = (qx - from.x) * dir.x + (qy - from.y) * dir.y + (qz - from.z) * dir.z;
                            if (!(along > end)) {
                                BlockPos pos = new BlockPos(x, y, z);
                                if (mc.level.isLoaded(pos)) {
                                    BlockState state = HIDDEN.getOrDefault(pos, mc.level.getBlockState(pos));
                                    if (!state.isAir() && !state.getCollisionShape(mc.level, pos).isEmpty()) {
                                        out.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return out;
        }
    }

    private static void apply(Minecraft mc, Set<BlockPos> wanted) {
        BlockPos feet = mc.player.blockPosition();
        wanted.removeIf(posx -> posx.getY() < feet.getY() && Math.abs(posx.getX() - feet.getX()) <= 2 && Math.abs(posx.getZ() - feet.getZ()) <= 2);
        Iterator<Entry<BlockPos, BlockState>> it = HIDDEN.entrySet().iterator();

        while (it.hasNext()) {
            Entry<BlockPos, BlockState> e = it.next();
            if (!wanted.contains(e.getKey())) {
                restore(mc, e.getKey(), e.getValue());
                it.remove();
            }
        }

        for (BlockPos pos : wanted) {
            if (!HIDDEN.containsKey(pos)) {
                BlockState state = mc.level.getBlockState(pos);
                if (!state.isAir()) {
                    HIDDEN.put(pos, state);
                    mc.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                }
            }
        }
    }

    public static void onRender(ClientHooks.RenderLevel e) {
        if (e.getStage() == ClientHooks.LevelStage.AFTER_PARTICLES && !HIDDEN.isEmpty() && !(ghost < 0.02F)) {
            Vec3 cam = e.getCamera().getPosition();
            PoseStack ps = e.getPoseStack();
            Matrix4f m = Mc.pose(ps.last());
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float a = Math.min(0.85F, ghost);

            for (BlockPos pos : HIDDEN.keySet()) {
                float x0 = (float)(pos.getX() - cam.x);
                float y0 = (float)(pos.getY() - cam.y);
                float z0 = (float)(pos.getZ() - cam.z);
                float x1 = x0 + 1.0F;
                float y1 = y0 + 1.0F;
                float z1 = z0 + 1.0F;
                face(bb, m, a, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0);
                face(bb, m, a, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1);
                face(bb, m, a, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0);
                face(bb, m, a, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
                face(bb, m, a, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1);
                face(bb, m, a, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0);
            }

            BufferUploader.drawWithShader(bb.end());
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
        }
    }

    private static void face(
        BufferBuilder bb,
        Matrix4f m,
        float a,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3
    ) {
        Mc.vertex(bb, m, x0, y0, z0).color(0.72F, 0.78F, 0.86F, a).endVertex();
        Mc.vertex(bb, m, x1, y1, z1).color(0.72F, 0.78F, 0.86F, a).endVertex();
        Mc.vertex(bb, m, x2, y2, z2).color(0.72F, 0.78F, 0.86F, a).endVertex();
        Mc.vertex(bb, m, x3, y3, z3).color(0.72F, 0.78F, 0.86F, a).endVertex();
    }
}
