package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.anim.AnimationSystem;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.IAnimatedObject;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class EntityPreview {
    private final String typeId;
    private Entity entity;
    private boolean created;
    private String anim = "";
    public String error;

    public EntityPreview(String typeId) {
        this.typeId = typeId;
    }

    public String typeId() {
        return this.typeId;
    }

    public Entity entity() {
        if (this.created) {
            return this.entity;
        } else {
            this.created = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return null;
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(this.typeId.contains(":") ? this.typeId : "minecraft:" + this.typeId);
                EntityType<?> type = rl == null ? null : Regs.ENTITY_TYPE.getOptional(rl).orElse(null);
                if (type == null) {
                    this.error = "Нет сущности " + this.typeId;
                    return null;
                } else {
                    try {
                        this.entity = type.create(mc.level);
                        if (this.entity != null && mc.player != null) {
                            this.entity.setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ());
                        }
                    } catch (Throwable var5) {
                        this.entity = null;
                        this.error = var5.getClass().getSimpleName();
                    }

                    return this.entity;
                }
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

    public void animate(String id, String layer, boolean loop, float speed) {
        if (id == null) {
            id = "";
        }

        if (!id.equals(this.anim)) {
            this.anim = id;
            if (this.entity() != null) {
                AnimationSystem sys = this.system();
                if (sys != null) {
                    try {
                        String l = Specs.layerName(layer != null && !layer.isBlank() ? layer : "action");
                        if (l == null || sys.getLayer(l) == null) {
                            l = "action";
                        }

                        if (id.isBlank()) {
                            sys.stopAnimation(l, 0.2F);
                            return;
                        }

                        String layerName = l;
                        Anims.resolve(id).ifPresent(a -> {
                            ConfiguredAnimation ca = a.configure().transitionTime(0.2F);
                            if (loop) {
                                ca.loopMode(LoopMode.LOOP);
                            }

                            if (speed > 0.0F && speed != 1.0F) {
                                ca.speed(speed);
                            }

                            sys.startAnimationAt(layerName, ca);
                        });
                    } catch (Throwable var8) {
                    }
                }
            }
        }
    }

    public void tick() {
        if (this.created && this.entity != null) {
            this.entity.tickCount++;
        }
    }

    public void render(Canvas c, float cx, float feetY, float boxW, float boxH, float zoom, float yaw, float pitch, float lookX, float lookY) {
        Entity e = this.entity();
        if (e != null) {
            float eh = Math.max(0.4F, e.getBbHeight());
            float ew = Math.max(0.4F, e.getBbWidth());
            float scale = Math.min(boxW * 0.9F / (ew * 1.4F), boxH * 0.92F / eh) * zoom;
            c.flush();
            PoseStack ps = c.g.pose();
            Pose outer = ps.last();
            ps.pushPose();
            try {
                ps.translate(cx, feetY, 200.0F);
                Mc.mulPoseMatrix(ps, new Matrix4f().scaling(scale, scale, -scale));
                Quaternionf var31 = new Quaternionf().rotateZ((float) Math.PI);
                var31.mul(new Quaternionf().rotateX((float)Math.toRadians(pitch + lookY * 20.0F)));
                Mc.mulPose(ps, var31);
                Mc.mulPose(ps, Axes.YP.rotationDegrees(yaw));
                float body = lookX * 20.0F;
                float head = lookX * 40.0F;
                e.setYRot(head);
                e.yRotO = head;
                e.setXRot(-lookY * 20.0F);
                e.xRotO = e.getXRot();
                if (e instanceof LivingEntity le) {
                    le.yBodyRot = le.yBodyRotO = body;
                    le.yHeadRot = le.yHeadRotO = head;
                }

                Lighting.setupForEntityInInventory();
                EntityRenderDispatcher d = Minecraft.getInstance().getEntityRenderDispatcher();
                d.setRenderShadow(false);
                float partial = Minecraft.getInstance().getFrameTime();
                RenderSystem.runAsFancy(() -> d.render(e, 0.0, 0.0, 0.0, 0.0F, partial, ps, c.g.bufferSource(), 15728880));
                c.g.flush();
                d.setRenderShadow(true);
            } catch (Throwable var29) {
                this.error = var29.getClass().getSimpleName();

                try {
                    c.g.flush();
                } catch (Throwable var28) {
                }
            } finally {
                for (int i = 0; i < 64 && ps.last() != outer; i++) {
                    ps.popPose();
                }

                Lighting.setupFor3DItems();
                RenderSystem.clear(256, Minecraft.ON_OSX);
            }
        }
    }
}
