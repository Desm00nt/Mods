package org.sprautestudio.spraute_engine.build.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.build.BlockQuake;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.net.Packets.BlocksShake;
import org.sprautestudio.spraute_engine.net.Packets.BlocksWave;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.ClientHooks.LevelStage;
import org.sprautestudio.spraute_engine.platform.ClientHooks.RenderLevel;

public final class BlockQuakeFx {
    private static final int MAX_BLOCKS = 6000;
    private static final int WAVE_UP = 4;
    private static final int WAVE_DOWN = 8;
    private static final double LINGER = 0.25;
    private static final int DUST_PER_FRAME = 16;
    private static final long TOGETHER = 7L;
    private static final List<BlockQuakeFx.Quake> QUAKES = new ArrayList<>();

    private BlockQuakeFx() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(BlockQuakeFx::onRender);
        ClientHooks.LOGGING_OUT.add(e -> clear());
    }

    private static double now() {
        return System.nanoTime() / 1.0E9;
    }

    private static void clear() {
        QUAKES.clear();
        BlockHide.clear();
    }

    private static int blocksNow() {
        int n = 0;

        for (BlockQuakeFx.Quake q : QUAKES) {
            n += q.pieces.size();
        }

        return n;
    }

    private static boolean movable(BlockState state) {
        return !state.isAir() && state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity() && state.getFluidState().isEmpty();
    }

    public static void wave(BlocksWave m) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && !(m.radius() <= 0.0F)) {
            int r = (int)Math.ceil(m.radius());
            int cx = (int)Math.floor(m.x());
            int cy = (int)Math.floor(m.y());
            int cz = (int)Math.floor(m.z());
            List<BlockQuakeFx.Piece> pieces = new ArrayList<>();
            int room = 6000 - blocksNow();
            MutableBlockPos at = new MutableBlockPos();

            for (int x = -r; x <= r && pieces.size() < room; x++) {
                for (int z = -r; z <= r && pieces.size() < room; z++) {
                    double rx = cx + x + 0.5 - m.x();
                    double rz = cz + z + 0.5 - m.z();
                    double d = Math.sqrt(rx * rx + rz * rz);
                    if (!(d > m.radius())) {
                        for (int y = cy + 4; y >= cy - 8; y--) {
                            at.set(cx + x, y, cz + z);
                            BlockState state = level.getBlockState(at);
                            if (!state.getFluidState().isEmpty()) {
                                break;
                            }

                            if (!state.getCollisionShape(level, at).isEmpty()) {
                                if (movable(state)) {
                                    pieces.add(new BlockQuakeFx.Piece(at.immutable(), state, d, at.asLong(), (float)Math.toDegrees(Math.atan2(rx, rz))));
                                }
                                break;
                            }
                        }
                    }
                }
            }

            BlockQuake.Wave shape = new BlockQuake.Wave(m.radius(), m.height(), m.step(), m.rise(), m.tilt());
            BlockQuakeFx.Quake q = new BlockQuakeFx.Quake(shape, shape.seconds(), 0.0, pieces.size());
            q.pieces.addAll(pieces);
            if (!pieces.isEmpty()) {
                QUAKES.add(q);
            }
        }
    }

    public static void shake(BlocksShake m) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level != null && !(m.seconds() <= 0.0F)) {
            BlockPos a = BlockPos.of(m.from());
            BlockPos b = BlockPos.of(m.to());
            int x0 = Math.min(a.getX(), b.getX());
            int y0 = Math.min(a.getY(), b.getY());
            int z0 = Math.min(a.getZ(), b.getZ());
            int x1 = Math.max(a.getX(), b.getX());
            int y1 = Math.max(a.getY(), b.getY());
            int z1 = Math.max(a.getZ(), b.getZ());
            double cx = (x0 + x1 + 1) / 2.0;
            double cy = (y0 + y1 + 1) / 2.0;
            double cz = (z0 + z1 + 1) / 2.0;
            double radius = m.round() ? (x1 - x0 + 1) / 2.0 : Double.MAX_VALUE;
            List<BlockQuakeFx.Piece> pieces = new ArrayList<>();
            int room = 6000 - blocksNow();
            MutableBlockPos at = new MutableBlockPos();

            for (int x = x0; x <= x1 && pieces.size() < room; x++) {
                for (int y = y0; y <= y1 && pieces.size() < room; y++) {
                    for (int z = z0; z <= z1 && pieces.size() < room; z++) {
                        double dx = x + 0.5 - cx;
                        double dy = y + 0.5 - cy;
                        double dz = z + 0.5 - cz;
                        if (radius == Double.MAX_VALUE || !(dx * dx + dy * dy + dz * dz > radius * radius)) {
                            at.set(x, y, z);
                            BlockState state = level.getBlockState(at);
                            if (movable(state) && exposed(level, at)) {
                                pieces.add(new BlockQuakeFx.Piece(at.immutable(), state, 0.0, m.together() ? 7L : at.asLong(), 0.0F));
                            }
                        }
                    }
                }
            }

            if (!pieces.isEmpty()) {
                BlockQuakeFx.Quake q = new BlockQuakeFx.Quake(null, m.seconds(), m.power(), pieces.size());
                q.pieces.addAll(pieces);
                q.shown = true;
                QUAKES.add(q);

                for (BlockQuakeFx.Piece p : pieces) {
                    BlockHide.hide(p.pos);
                }

                mc.levelRenderer.setBlocksDirty(x0, y0, z0, x1, y1, z1);
            }
        }
    }

    private static boolean exposed(ClientLevel level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos next = pos.relative(d);
            if (!level.getBlockState(next).isSolidRender(level, next)) {
                return true;
            }
        }

        return false;
    }

    private static void unhide(Minecraft mc, BlockQuakeFx.Quake q) {
        q.shown = false;
        int x0 = Integer.MAX_VALUE;
        int y0 = Integer.MAX_VALUE;
        int z0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int y1 = Integer.MIN_VALUE;
        int z1 = Integer.MIN_VALUE;

        for (BlockQuakeFx.Piece p : q.pieces) {
            BlockHide.show(p.pos);
            x0 = Math.min(x0, p.pos.getX());
            y0 = Math.min(y0, p.pos.getY());
            z0 = Math.min(z0, p.pos.getZ());
            x1 = Math.max(x1, p.pos.getX());
            y1 = Math.max(y1, p.pos.getY());
            z1 = Math.max(z1, p.pos.getZ());
        }

        mc.levelRenderer.setBlocksDirty(x0, y0, z0, x1, y1, z1);
    }

    private static void onRender(RenderLevel e) {
        if (e.getStage() == LevelStage.AFTER_PARTICLES && !QUAKES.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockRenderDispatcher blocks = mc.getBlockRenderer();
                BufferSource buffers = mc.renderBuffers().bufferSource();
                PoseStack ps = e.getPoseStack();
                Vec3 cam = e.getCamera().getPosition();
                double now = now();
                int dust = 0;
                Iterator<BlockQuakeFx.Quake> it = QUAKES.iterator();

                while (it.hasNext()) {
                    BlockQuakeFx.Quake q = it.next();
                    double age = now - q.start;
                    if (q.wave == null && q.shown && age >= q.seconds) {
                        unhide(mc, q);
                    }

                    if (age >= q.seconds + (q.wave != null ? 0.0 : 0.25)) {
                        it.remove();
                    } else {
                        for (int i = 0; i < q.pieces.size(); i++) {
                            BlockQuakeFx.Piece p = q.pieces.get(i);
                            double dx = 0.0;
                            double dz = 0.0;
                            float lean = 0.0F;
                            double dy;
                            if (q.wave != null) {
                                dy = q.wave.lift(p.distance, p.seed, age);
                                if (dy <= 0.001) {
                                    continue;
                                }

                                lean = (float)q.wave.lean(p.distance, p.seed, age);
                                if (!q.dusted[i] && dust < 16) {
                                    q.dusted[i] = true;
                                    dust++;
                                    mc.level
                                        .addParticle(
                                            new BlockParticleOption(ParticleTypes.BLOCK, p.state),
                                            p.pos.getX() + 0.5,
                                            p.pos.getY() + 1.1,
                                            p.pos.getZ() + 0.5,
                                            0.0,
                                            0.1,
                                            0.0
                                        );
                                }
                            } else {
                                double[] off = BlockQuake.shake(p.seed, q.power, q.seconds, age);
                                dx = off[0];
                                dy = off[1];
                                dz = off[2];
                            }

                            ps.pushPose();
                            ps.translate(p.pos.getX() - cam.x + dx, p.pos.getY() - cam.y + dy, p.pos.getZ() - cam.z + dz);
                            if (lean != 0.0F) {
                                ps.translate(0.5, 0.5, 0.5);
                                Mc.mulPose(ps, Axes.YP.rotationDegrees(p.away));
                                Mc.mulPose(ps, Axes.XP.rotationDegrees(lean));
                                Mc.mulPose(ps, Axes.YP.rotationDegrees(-p.away));
                                ps.translate(-0.5, -0.5, -0.5);
                            }

                            int light = LevelRenderer.getLightColor(mc.level, p.pos.above());
                            blocks.renderSingleBlock(p.state, ps, buffers, light, OverlayTexture.NO_OVERLAY);
                            ps.popPose();
                        }
                    }
                }

                buffers.endBatch();
            }
        }
    }

    private record Piece(BlockPos pos, BlockState state, double distance, long seed, float away) {
    }

    private static final class Quake {
        final List<BlockQuakeFx.Piece> pieces = new ArrayList<>();
        final boolean[] dusted;
        final BlockQuake.Wave wave;
        final double start = BlockQuakeFx.now();
        final double seconds;
        final double power;
        boolean shown;

        Quake(BlockQuake.Wave wave, double seconds, double power, int size) {
            this.wave = wave;
            this.seconds = seconds;
            this.power = power;
            this.dusted = new boolean[size];
        }
    }
}
