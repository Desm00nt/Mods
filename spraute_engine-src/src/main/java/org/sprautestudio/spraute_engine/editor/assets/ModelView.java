package org.sprautestudio.spraute_engine.editor.assets;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.AnimationSystem;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.IAnimatedObject;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.models.PartShape;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class ModelView extends Widget implements UI.CursorProvider {
    private EntityType<?> type;
    private Entity entity;
    private boolean created;
    private String error;
    private float yaw = 205.0F;
    private float pitch = 8.0F;
    private float zoom = 1.0F;
    private float targetZoom = 1.0F;
    private boolean autoSpin;
    private String playing;
    public String emptyText = "Выберите модель";
    public Supplier<List<ModelView.Box>> boxes;
    public IntConsumer onPickBox;
    public ModelView.MoveListener onMoveBox;
    private float lastCx;
    private float lastCy;
    private float lastScale;
    private boolean draggingBox;
    private Consumer<Entity> setup;

    public Entity previewEntity() {
        return this.created ? this.entity : null;
    }

    private Matrix4f view(float scale) {
        return new Matrix4f()
            .scaling(scale, scale, -scale)
            .rotateZ((float) Math.PI)
            .rotateX((float)Math.toRadians(this.pitch))
            .rotateY((float)Math.toRadians(this.yaw));
    }

    private float[] project(Matrix4f m, float px, float py, float pz) {
        Vector3f v = m.transformPosition(new Vector3f(px, py, pz));
        return new float[]{this.lastCx + v.x, this.lastCy + v.y};
    }

    private float[][] corners(Matrix4f m, ModelView.Box b) {
        Vector3f[] c = b.shape().corners();
        float[][] out = new float[8][];

        for (int i = 0; i < 8; i++) {
            out[i] = this.project(m, c[i].x, c[i].y, c[i].z);
        }

        return out;
    }

    private void paintBoxes(Canvas c) {
        if (this.boxes != null) {
            List<ModelView.Box> list = this.boxes.get();
            if (list != null && !list.isEmpty()) {
                Matrix4f m = this.view(this.lastScale);
                int[][] edges = new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

                for (ModelView.Box b : list) {
                    float[][] p = this.corners(m, b);
                    int col = b.selected() ? -8090 : b.color();
                    float th = b.selected() ? 2.0F : 1.0F;

                    for (int[] e : edges) {
                        c.line(p[e[0]][0], p[e[0]][1], p[e[1]][0], p[e[1]][1], th, col);
                    }

                    if (b.label() != null && !b.label().isEmpty()) {
                        PartShape sh = b.shape();
                        Vector3f t = sh.rot().transform(new Vector3f(0.0F, sh.half().y, 0.0F)).add(sh.center());
                        float[] top = this.project(m, t.x, t.y, t.z);
                        c.textCentered(b.label(), (int)top[0], (int)top[1] - 11, col);
                    }
                }
            }
        }
    }

    private int boxAt(double mx, double my) {
        if (this.boxes == null) {
            return -1;
        } else {
            List<ModelView.Box> list = this.boxes.get();
            if (list == null) {
                return -1;
            } else {
                Matrix4f m = this.view(this.lastScale);
                int best = -1;
                float bestArea = Float.MAX_VALUE;

                for (int i = 0; i < list.size(); i++) {
                    float[][] p = this.corners(m, list.get(i));
                    float x0 = Float.MAX_VALUE;
                    float y0 = Float.MAX_VALUE;
                    float x1 = -Float.MAX_VALUE;
                    float y1 = -Float.MAX_VALUE;

                    for (float[] q : p) {
                        x0 = Math.min(x0, q[0]);
                        y0 = Math.min(y0, q[1]);
                        x1 = Math.max(x1, q[0]);
                        y1 = Math.max(y1, q[1]);
                    }

                    float area = (x1 - x0) * (y1 - y0);
                    if (mx >= x0 && mx <= x1 && my >= y0 && my <= y1 && area < bestArea) {
                        best = i;
                        bestArea = area;
                    }
                }

                return best;
            }
        }
    }

    public void setType(EntityType<?> t, Consumer<Entity> setup) {
        this.setup = setup;
        this.type = null;
        this.setType(t);
    }

    public void setType(EntityType<?> t) {
        if (t != this.type) {
            this.type = t;
            this.entity = null;
            this.created = false;
            this.error = null;
            this.playing = null;
        }
    }

    public EntityType<?> type() {
        return this.type;
    }

    public void setAutoSpin(boolean spin) {
        this.autoSpin = spin;
    }

    public boolean autoSpin() {
        return this.autoSpin;
    }

    public String playing() {
        return this.playing;
    }

    public boolean isAnimated() {
        return this.entity() instanceof IAnimatedObject;
    }

    private Entity entity() {
        if (this.created) {
            return this.entity;
        } else {
            this.created = true;
            Minecraft mc = Minecraft.getInstance();
            if (this.type != null && mc.level != null) {
                try {
                    this.entity = this.type.create(mc.level);
                    if (this.entity == null) {
                        this.error = "Эта сущность не создаётся отдельно";
                    } else {
                        if (mc.player != null) {
                            this.entity.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                        }

                        if (this.setup != null) {
                            this.setup.accept(this.entity);
                        }
                    }
                } catch (Throwable var3) {
                    this.entity = null;
                    this.error = "Не удалось создать модель: " + var3.getClass().getSimpleName();
                }

                return this.entity;
            } else {
                return null;
            }
        }
    }

    private AnimationSystem system() {
        try {
            return this.entity instanceof IAnimatedObject ao ? ao.getAnimationSystem() : null;
        } catch (Throwable var3) {
            return null;
        }
    }

    public String play(String animId, String layer, boolean loop, float speed) {
        Entity e = this.entity();
        if (e == null) {
            return this.error != null ? this.error : "Нет модели";
        } else {
            AnimationSystem sys = this.system();
            if (sys == null) {
                return "Эта модель не поддерживает анимации";
            } else {
                Optional<Animation> a = Anims.resolve(animId);
                if (a.isEmpty()) {
                    return "Анимация не найдена";
                } else {
                    String layerName = Specs.layerName(layer);
                    if (layerName == null || sys.getLayer(layerName) == null) {
                        layerName = sys.getLayer("action") != null ? "action" : sys.getLayerNames().stream().findFirst().orElse(null);
                    }

                    if (layerName == null) {
                        return "У модели нет слоёв анимации";
                    } else {
                        try {
                            ConfiguredAnimation ca = a.get().configure().transitionTime(0.2F);
                            if (loop) {
                                ca.loopMode(LoopMode.LOOP);
                            }

                            if (speed != 1.0F) {
                                ca.speed(speed);
                            }

                            sys.startAnimationAt(layerName, ca);
                            this.playing = animId;
                            return null;
                        } catch (Throwable var10) {
                            return "Ошибка анимации: " + var10.getMessage();
                        }
                    }
                }
            }
        }
    }

    public String play(ConfiguredAnimation ca, String label) {
        Entity e = this.entity();
        if (e == null) {
            return this.error != null ? this.error : "Нет модели";
        } else {
            AnimationSystem sys = this.system();
            if (sys == null) {
                return "Эта модель не поддерживает анимации";
            } else {
                String layerName = sys.getLayer("action") != null ? "action" : sys.getLayerNames().stream().findFirst().orElse(null);
                if (layerName == null) {
                    return "У модели нет слоёв анимации";
                } else {
                    try {
                        sys.startAnimationAt(layerName, ca.transitionTime(0.2F).loopMode(LoopMode.LOOP));
                        this.playing = label;
                        return null;
                    } catch (Throwable var7) {
                        return "Ошибка анимации: " + var7.getMessage();
                    }
                }
            }
        }
    }

    public void stopAll() {
        AnimationSystem sys = this.system();
        this.playing = null;
        if (sys != null) {
            try {
                for (String l : sys.getLayerNames()) {
                    sys.stopAnimation(l, 0.2F);
                }
            } catch (Throwable var4) {
            }
        }
    }

    public void resetView() {
        this.yaw = 205.0F;
        this.pitch = 8.0F;
        this.targetZoom = 1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isShown()) {
            Entity e = this.created ? this.entity : null;
            if (e != null) {
                e.tickCount++;
            }
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.gradient(this.x, this.y, this.w, this.h, -14737104, -15592679);
        int floorY = this.y + this.h * 3 / 4;

        for (int i = -8; i <= 8; i++) {
            int gx = this.x + this.w / 2 + i * this.w / 18;
            c.fill(gx, floorY, 1, this.h - (floorY - this.y), 285212671);
        }

        for (int k = 0; k < 5; k++) {
            c.fill(this.x, floorY + k * k * 3, this.w, 1, 318767103);
        }

        c.fill(this.x, floorY, this.w, 1, 587202559);
        if (this.autoSpin) {
            this.yaw += dt * 25.0F;
        }

        this.zoom = this.zoom + (this.targetZoom - this.zoom) * Math.min(1.0F, dt * 12.0F);
        if (this.type == null) {
            c.icon(Icon.CUBE, this.x + this.w / 2 - 4, this.y + this.h / 2 - 16, -9933435);
            c.textCentered(this.emptyText, this.x + this.w / 2, this.y + this.h / 2, -9933435);
        } else {
            Entity e = this.entity();
            if (e == null) {
                c.icon(Icon.WARNING, this.x + this.w / 2 - 4, this.y + this.h / 2 - 16, -870325);
                c.textCentered(this.error != null ? this.error : "Нет мира — войдите в мир для превью", this.x + this.w / 2, this.y + this.h / 2, -6577995);
            } else {
                float eh = Math.max(0.4F, e.getBbHeight());
                float ew = Math.max(0.4F, e.getBbWidth());
                float scale = Math.min(this.w * 0.55F / (ew * 1.6F), this.h * 0.62F / eh) * this.zoom;
                float cx = this.x + this.w / 2.0F;
                float cy = floorY - 2;
                int sw = (int)(ew * scale * 1.1F);
                c.round((int)cx - sw / 2, (int)cy - 2, sw, 5, 2, 1140850688);
                this.lastCx = cx;
                this.lastCy = cy;
                this.lastScale = scale;
                this.renderEntity(c, e, cx, cy, scale);
                this.paintBoxes(c);
                if (this.error != null) {
                    c.textCentered(this.error, this.x + this.w / 2, this.y + 8, -1023878);
                }
            }
        }
    }

    private void renderEntity(Canvas c, Entity e, float cx, float cy, float scale) {
        c.flush();
        PoseStack ps = c.g.pose();
        Pose outer = ps.last();
        ps.pushPose();
        try {
            ps.translate(cx, cy, 200.0F);
            Mc.mulPoseMatrix(ps, new Matrix4f().scaling(scale, scale, -scale));
            Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
            rot.mul(new Quaternionf().rotateX((float)Math.toRadians(this.pitch)));
            Mc.mulPose(ps, rot);
            Mc.mulPose(ps, Axes.YP.rotationDegrees(this.yaw));
            e.setYRot(0.0F);
            e.yRotO = 0.0F;
            e.setXRot(0.0F);
            e.xRotO = 0.0F;
            if (e instanceof LivingEntity le) {
                le.yBodyRot = le.yBodyRotO = 0.0F;
                le.yHeadRot = le.yHeadRotO = 0.0F;
            }

            Lighting.setupForEntityInInventory();
            EntityRenderDispatcher d = Minecraft.getInstance().getEntityRenderDispatcher();
            d.setRenderShadow(false);
            float partial = Minecraft.getInstance().getFrameTime();
            RenderSystem.runAsFancy(() -> d.render(e, 0.0, 0.0, 0.0, 0.0F, partial, ps, c.g.bufferSource(), 15728880));
            c.g.flush();
            d.setRenderShadow(true);
        } catch (Throwable t) {
            if (this.error == null) {
                SprauteEngine.LOGGER.warn("Spraute: model preview failed", t);
            }

            this.error = "Ошибка отрисовки: " + t.getClass().getSimpleName();

            try {
                c.g.flush();
            } catch (Throwable ignored) {
            }
        } finally {
            for (int i = 0; i < 64 && ps.last() != outer; i++) {
                ps.popPose();
            }

            Lighting.setupFor3DItems();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.clear(256, Minecraft.ON_OSX);
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        this.draggingBox = false;
        if (button == 0 && this.onPickBox != null && this.boxes != null && this.lastScale > 0.0F) {
            int i = this.boxAt(mx, my);
            this.onPickBox.accept(i);
            this.draggingBox = i >= 0 && this.onMoveBox != null;
        }

        return button == 0 || button == 1;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (this.draggingBox && this.onMoveBox != null) {
            this.onMoveBox.released();
        }

        this.draggingBox = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.draggingBox && button == 0 && this.lastScale > 0.0F) {
            Matrix3f r = new Matrix3f().rotateZ((float) Math.PI).rotateX((float)Math.toRadians(this.pitch)).rotateY((float)Math.toRadians(this.yaw));
            Vector3f d = new Vector3f((float)dx / this.lastScale, (float)dy / this.lastScale, 0.0F);
            r.transpose().transform(d);
            this.onMoveBox.moved(d.x, d.y, d.z, (float)dx / this.lastScale, (float)dy / this.lastScale);
            return true;
        } else {
            this.yaw += (float)dx * 1.4F;
            this.pitch = Math.max(-60.0F, Math.min(60.0F, this.pitch - (float)dy * 1.0F));
            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        this.targetZoom = Math.max(0.3F, Math.min(4.0F, this.targetZoom * (delta > 0.0 ? 1.15F : 0.86956525F)));
        return true;
    }

    @Override
    public boolean mouseDoubleClicked(double mx, double my, int button) {
        this.resetView();
        return true;
    }

    @Override
    public int cursor(int mx, int my) {
        return 221188;
    }

    public record Box(PartShape shape, int color, String label, boolean selected) {
    }

    public interface MoveListener {
        void moved(float var1, float var2, float var3, float var4, float var5);

        void released();
    }
}
