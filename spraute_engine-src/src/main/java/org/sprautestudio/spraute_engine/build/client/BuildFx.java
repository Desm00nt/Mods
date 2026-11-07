package org.sprautestudio.spraute_engine.build.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.build.BuildLayout;
import org.sprautestudio.spraute_engine.build.BuildStyles;
import org.sprautestudio.spraute_engine.build.StructureDef;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.net.Packets.BuildFly;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.platform.ClientHooks.ClientTick;
import org.sprautestudio.spraute_engine.platform.ClientHooks.LevelStage;
import org.sprautestudio.spraute_engine.platform.ClientHooks.RenderLevel;

public final class BuildFx {
    private static final int MAX_FLYING = 6000;
    private static final double WAIT_FOR_REAL = 0.5;
    private static final int DUST_PER_FRAME = 24;
    private static final List<BuildFx.Ghost> GHOSTS = new ArrayList<>();
    private static final Random RND = new Random();
    private static List<BuildLayout.Cell> previewCells = List.of();
    private static BuildStyles.Style previewStyle;
    private static int previewNext;
    private static double previewPerTick;
    private static double previewOwed;

    private BuildFx() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_LEVEL.add(BuildFx::onRender);
        ClientHooks.CLIENT_TICK.add(BuildFx::onTick);
        ClientHooks.LOGGING_OUT.add(e -> clear());
    }

    private static double now() {
        return System.nanoTime() / 1.0E9;
    }

    public static void clear() {
        GHOSTS.clear();
        previewCells = List.of();
    }

    private static BuildFx.Ghost ghost(
        BlockPos pos, BlockState state, float fx, float fy, float fz, double length, int ease, boolean reverse, boolean spinning, float swirl
    ) {
        BuildFx.Ghost g = new BuildFx.Ghost();
        g.pos = pos;
        g.state = state;
        g.fx = fx;
        g.fy = fy;
        g.fz = fz;
        g.start = now();
        g.length = Math.max(0.05, length);
        g.ease = ease;
        g.reverse = reverse;
        g.spinning = spinning;
        g.swirl = swirl;
        g.spinX = (RND.nextFloat() - 0.5F) * 720.0F;
        g.spinY = (RND.nextFloat() - 0.5F) * 720.0F;
        GHOSTS.add(g);
        return g;
    }

    public static void receive(BuildFly msg) {
        for (int i = 0; i < msg.pos().length && GHOSTS.size() < 6000; i++) {
            BlockState state = Block.stateById(msg.states()[i]);
            ghost(
                BlockPos.of(msg.pos()[i]),
                state,
                msg.from()[i * 3],
                msg.from()[i * 3 + 1],
                msg.from()[i * 3 + 2],
                msg.flight() / 20.0,
                msg.ease(),
                msg.reverse(),
                msg.spinning(),
                msg.swirl()
            );
        }
    }

    public static void preview(StructureDef def, String styleName, BlockPos at, int rotate, String mirror, double seconds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.player != null) {
            GHOSTS.clear();
            BuildStyles.Style style = BuildStyles.get(styleName);
            if (style == null) {
                style = BuildStyles.get("up");
            }

            previewCells = new ArrayList<>(BuildLayout.of(def, style.name(), at, rotate, mirror, mc.player.getYRot(), mc.level, RND.nextLong()));
            previewCells.removeIf(c -> c.state().isAir());
            previewStyle = style;
            previewNext = 0;
            previewOwed = 0.0;
            double time = seconds > 0.0 ? seconds : Math.max(2.0, Math.min(30.0, previewCells.size() / 120.0));
            previewPerTick = previewCells.size() / (time * 20.0);
        }
    }

    public static boolean previewing() {
        return !previewCells.isEmpty();
    }

    public static void onTick(ClientTick e) {
        if (e.phase == TickPhase.END && !previewCells.isEmpty() && previewNext < previewCells.size()) {
            previewOwed = previewOwed + previewPerTick;
            int n = (int)previewOwed;
            previewOwed -= n;

            for (int i = 0; i < n && previewNext < previewCells.size() && GHOSTS.size() < 6000; i++) {
                BuildLayout.Cell c = previewCells.get(previewNext++);
                BuildFx.Ghost g = ghost(
                    c.pos(),
                    c.state(),
                    c.fromX(),
                    c.fromY(),
                    c.fromZ(),
                    previewStyle.flight(),
                    previewStyle.ease().ordinal(),
                    false,
                    previewStyle.spinning(),
                    (float)previewStyle.swirl()
                );
                g.keep = true;
            }
        }
    }

    private static float ease(int kind, float t) {
        if (kind >= 100) {
            List<String> names = Ease.names();
            int i = kind - 100;
            return (float)Ease.apply(i < names.size() ? names.get(i) : "smooth", t);
        } else {
            float u = 1.0F - t;

            return switch (kind) {
                case 1 -> {
                    float c = 1.70158F;
                    yield 1.0F + (c + 1.0F) * (t - 1.0F) * (t - 1.0F) * (t - 1.0F) + c * (t - 1.0F) * (t - 1.0F);
                }
                case 2 -> {
                    float n = 7.5625F;
                    float d = 2.75F;
                    if (t < 1.0F / d) {
                        yield n * t * t;
                    } else if (t < 2.0F / d) {
                        float k = t - 1.5F / d;
                        yield n * k * k + 0.75F;
                    } else if (t < 2.5F / d) {
                        float k = t - 2.25F / d;
                        yield n * k * k + 0.9375F;
                    } else {
                        float k = t - 2.625F / d;
                        yield n * k * k + 0.984375F;
                    }
                }
                default -> 1.0F - u * u * u;
            };
        }
    }

    public static void onRender(RenderLevel e) {
        if (e.getStage() == LevelStage.AFTER_PARTICLES && !GHOSTS.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                BlockRenderDispatcher blocks = mc.getBlockRenderer();
                BufferSource buffers = mc.renderBuffers().bufferSource();
                PoseStack ps = e.getPoseStack();
                Vec3 cam = e.getCamera().getPosition();
                double now = now();
                int dust = 0;
                Iterator<BuildFx.Ghost> it = GHOSTS.iterator();

                while (it.hasNext()) {
                    BuildFx.Ghost g = it.next();
                    double age = now - g.start;
                    float t = (float)Math.min(1.0, age / g.length);
                    if (t >= 1.0F) {
                        if (g.reverse) {
                            it.remove();
                            continue;
                        }

                        boolean real = mc.level.getBlockState(g.pos).equals(g.state);
                        if (!g.keep && (real || age > g.length + 0.5)) {
                            it.remove();
                            continue;
                        }

                        if (!g.dusted && dust < 24) {
                            g.dusted = true;
                            dust++;
                            mc.level
                                .addParticle(
                                    new BlockParticleOption(ParticleTypes.BLOCK, g.state),
                                    g.pos.getX() + 0.5,
                                    g.pos.getY() + 0.1,
                                    g.pos.getZ() + 0.5,
                                    0.0,
                                    0.0,
                                    0.0
                                );
                        }

                        if (real) {
                            continue;
                        }
                    }

                    if (g.state.getRenderShape() != RenderShape.INVISIBLE) {
                        float k = g.reverse ? 1.0F - ease(0, t) : ease(g.ease, t);
                        float left = 1.0F - k;
                        float fx = g.fx;
                        float fz = g.fz;
                        if (g.swirl != 0.0F) {
                            double a = g.swirl * Math.PI * left;
                            float co = (float)Math.cos(a);
                            float si = (float)Math.sin(a);
                            float nx = fx * co - fz * si;
                            fz = fx * si + fz * co;
                            fx = nx;
                        }

                        double x = g.pos.getX() + fx * left - cam.x;
                        double y = g.pos.getY() + g.fy * left - cam.y;
                        double z = g.pos.getZ() + fz * left - cam.z;
                        float scale = g.ease == BuildStyles.Ease.GROW.ordinal()
                            ? Math.max(0.01F, g.reverse ? (t < 1.0F ? 1.0F - t : 0.0F) : k)
                            : (g.reverse ? Math.max(0.05F, 1.0F - t) : 1.0F);
                        ps.pushPose();
                        ps.translate(x + 0.5, y + 0.5, z + 0.5);
                        if (g.spinning && left > 0.001F) {
                            Mc.mulPose(ps, Axes.YP.rotationDegrees(g.spinY * left));
                            Mc.mulPose(ps, Axes.XP.rotationDegrees(g.spinX * left));
                        }

                        if (scale != 1.0F) {
                            ps.scale(scale, scale, scale);
                        }

                        ps.translate(-0.5, -0.5, -0.5);
                        int light = LevelRenderer.getLightColor(mc.level, g.pos.above());
                        blocks.renderSingleBlock(g.state, ps, buffers, light, OverlayTexture.NO_OVERLAY);
                        ps.popPose();
                    }
                }

                buffers.endBatch();
            }
        }
    }

    private static final class Ghost {
        BlockPos pos;
        BlockState state;
        float fx;
        float fy;
        float fz;
        float spinX;
        float spinY;
        double start;
        double length;
        int ease;
        boolean reverse;
        boolean spinning;
        boolean keep;
        float swirl;
        boolean dusted;
    }
}
