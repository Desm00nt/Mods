package org.sprautestudio.spraute_engine.runtime.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.client.SpGeoRenderer;
import org.sprautestudio.spraute_engine.core.math.SpMatrix4;
import org.sprautestudio.spraute_engine.core.math.SpVec3;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.core.model.SpModelInstance;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;

public class SprauteModelRenderer extends EntityRenderer<SprauteModelEntity> {
    private static final ResourceLocation MISSING = new ResourceLocation("minecraft", "textures/misc/unknown_pack.png");
    private static final String[] RIGHT = new String[]{
        "RightHandItem", "Right_placeholder", "RightItem", "right_item", "item_right", "RightHand", "right_hand", "hand_right"
    };
    private static final String[] LEFT = new String[]{
        "LeftHandItem", "Left_placeholder", "LeftItem", "left_item", "item_left", "LeftHand", "left_hand", "hand_left"
    };
    private static final String[] RIGHT_ARM = new String[]{"RightArm", "right_arm", "arm_right"};
    private static final String[] LEFT_ARM = new String[]{"LeftArm", "left_arm", "arm_left"};
    private static final double LOD_1 = 576.0;
    private static final double LOD_2 = 2304.0;
    private static final double LOD_3 = 9216.0;
    private static final int[] POSE_EVERY = new int[]{1, 2, 3, 5};
    private static final float EYE_OFFSET = 0.1F;
    private static final float EYE_TURN = 30.0F;
    private static final float PUPIL_SHIFT = 0.3F;
    private static final float PUPIL_EASE = 0.2F;
    private static final Map<SprauteModelEntity, SprauteModelRenderer.Posed> INSTANCES = new WeakHashMap<>();
    private static boolean portrait;
    private static String[] portraitBones;
    private boolean warned;

    private static int lod(SprauteModelEntity e, float scale) {
        if (!portrait && !CameraDirector.active() && Mc.level(e).getEntity(e.getId()) == e) {
            double d = Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(e) / Math.max(1.0F, scale * scale);
            return d < 576.0 ? 0 : (d < 2304.0 ? 1 : (d < 9216.0 ? 2 : 3));
        } else {
            return 0;
        }
    }

    public SprauteModelRenderer(Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4F;
    }

    public static void withPortrait(String bone, Runnable draw) {
        portrait = true;
        portraitBones = bone != null ? new String[]{bone} : null;

        try {
            draw.run();
        } finally {
            portrait = false;
            portraitBones = null;
        }
    }

    private static SprauteModelRenderer.Posed posed(SprauteModelEntity e, SpGeoModel model) {
        SprauteModelRenderer.Posed p = INSTANCES.get(e);
        if (p == null || p.model != model) {
            p = new SprauteModelRenderer.Posed(model, new SpModelInstance(model), e.getId());
            INSTANCES.put(e, p);
        }

        return p;
    }

    private static void pupils(SprauteModelEntity e, SprauteModelRenderer.Posed p) {
        Vec3 at = e.lookPoint();
        float wantL = 0.0F;
        float wantR = 0.0F;
        if (at != null) {
            float head = e.yHeadRot;
            double side = Math.toRadians(head + 90.0F);
            wantL = eyeShift(e, at, head, Math.cos(side) * 0.1F, Math.sin(side) * 0.1F);
            wantR = eyeShift(e, at, head, -Math.cos(side) * 0.1F, -Math.sin(side) * 0.1F);
        }

        p.pupilLeft = p.pupilLeft + (wantL - p.pupilLeft) * 0.2F;
        p.pupilRight = p.pupilRight + (wantR - p.pupilRight) * 0.2F;
        SpVec3 l = p.instance.animPosition("LEye");
        SpVec3 r = p.instance.animPosition("REye");
        if (l != null) {
            l.x = l.x + p.pupilLeft;
        }

        if (r != null) {
            r.x = r.x + p.pupilRight;
        }
    }

    private static float eyeShift(SprauteModelEntity e, Vec3 at, float head, double ox, double oz) {
        double dx = at.x - (e.getX() + ox);
        double dz = at.z - (e.getZ() + oz);
        float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
        return Mth.clamp(Mth.wrapDegrees(yaw - head) / 30.0F, -1.0F, 1.0F) * 0.3F;
    }

    public void render(SprauteModelEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        EntityLibrary.Look l = e.look();
        SpGeoModel model = l != null ? l.geometry() : null;
        if (model != null && !model.boneMap.isEmpty()) {
            Pose outer = pose.last();

            try {
                if (e.renderBase == null) {
                    e.renderBase = new Matrix4f(Mc.pose(pose.last()));
                } else {
                    e.renderBase.set(Mc.pose(pose.last()));
                }

                if (!portrait && Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) {
                    drawShapes(e, pose, buffers);
                }

                float lift = l.def() != null ? l.def().modelY : 0.0F;
                float scale = l.scale() * e.bodyScale();
                int lod = lod(e, scale);
                SprauteModelRenderer.Posed posed = posed(e, model);
                SpModelInstance inst = posed.instance;
                boolean posedNow = portrait || posed.stale || ++posed.age >= POSE_EVERY[lod];
                if (posedNow) {
                    posed.age = 0;
                    inst.resetAnims();
                    if (!portrait) {
                        e.getAnimationSystem().apply(inst);
                        turnHead(e, inst, partial);
                        if (lod < 2) {
                            pupils(e, posed);
                        }
                    }

                    inst.computeTransforms();
                    posed.stale = portrait;
                }

                pose.pushPose();
                if (lift != 0.0F) {
                    pose.translate(0.0F, lift, 0.0F);
                }

                Mc.mulPose(pose, Axes.YP.rotationDegrees(180.0F - e.renderedBodyYaw(partial)));
                if (e.deathTime > 0) {
                    float t = Math.min(1.0F, Mth.sqrt((e.deathTime + partial - 1.0F) / 20.0F * 1.6F));
                    Mc.mulPose(pose, Axes.ZP.rotationDegrees(t * 90.0F));
                }

                if (scale != 1.0F) {
                    pose.scale(scale, scale, scale);
                }

                int overlay = OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(e.hurtTime > 0 || e.deathTime > 0));
                VertexConsumer vc = buffers.getBuffer(lod >= 2 ? RenderType.entityCutoutNoCull(texture(e)) : RenderType.entityTranslucent(texture(e)));
                SpGeoRenderer.render(inst, pose, vc, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F, portrait ? portraitBones : null, lod);
                if (!portrait) {
                    if (posedNow) {
                        measureBones(e, inst, pose);
                    }

                    if (lod < 3) {
                        heldItems(e, inst, l, pose, buffers, light);
                    }
                }

                pose.popPose();
                if (!portrait) {
                    super.render(e, yaw, partial, pose, buffers, light);
                }
            } catch (Throwable t) {
                if (!this.warned) {
                    this.warned = true;
                    SprauteEngine.LOGGER.warn("Spraute: cannot draw entity {}", e.model(), t);
                }
            } finally {
                for (int i = 0; i < 64 && pose.last() != outer; i++) {
                    pose.popPose();
                }
            }
        } else {
            super.render(e, yaw, partial, pose, buffers, light);
        }
    }

    private static void turnHead(SprauteModelEntity e, SpModelInstance inst, float partial) {
        SpVec3 head = inst.animRotation("head");
        if (head != null) {
            float yaw = Mth.wrapDegrees(e.renderedHeadYaw(partial) - e.renderedBodyYaw(partial));
            head.add(Mth.lerp(partial, e.xRotO, e.getXRot()), Mth.clamp(yaw, -75.0F, 75.0F), 0.0F);
        }
    }

    private static void toBone(PoseStack pose, SpMatrix4 bone) {
        float[] m = bone.m;
        pose.translate(-m[12] / 16.0F, m[13] / 16.0F, m[14] / 16.0F);
        Mc.mulPose(pose, new Quaternionf().setFromNormalized(new Matrix3f(m[0], -m[1], -m[2], -m[4], m[5], m[6], -m[8], m[9], m[10])));
    }

    private static void measureBones(SprauteModelEntity e, SpModelInstance inst, PoseStack pose) {
        if (e.renderBase != null) {
            long now = System.currentTimeMillis();
            boolean all = now < e.boneWatchUntil;
            List<EntityLibrary.Def.Part> defs = e.partList();
            if (!defs.isEmpty() || all) {
                Matrix4f inv = new Matrix4f(e.renderBase).invert();
                if (!all) {
                    for (EntityLibrary.Def.Part p : defs) {
                        if (p.bone != null && !p.bone.isEmpty()) {
                            measure(e, inst, pose, inv, p.bone, now);
                        }
                    }
                } else {
                    for (String name : inst.getModel().boneMap.keySet()) {
                        measure(e, inst, pose, inv, name, now);
                    }
                }
            }
        }
    }

    private static void measure(SprauteModelEntity e, SpModelInstance inst, PoseStack pose, Matrix4f inv, String name, long now) {
        SpMatrix4 b = inst.getBoneMatrix(name);
        if (b != null) {
            pose.pushPose();
            toBone(pose, b);
            Matrix4f m = new Matrix4f(inv).mul(Mc.pose(pose.last()));
            e.bonePoses.put(name, new PartShape.BonePose(m.transformPosition(new Vector3f()), m.getNormalizedRotation(new Quaternionf()), now));
            pose.popPose();
        }
    }

    private static SpMatrix4 bone(SpModelInstance inst, String... names) {
        for (String n : names) {
            if (n != null && !n.isEmpty()) {
                SpMatrix4 m = inst.getBoneMatrix(n);
                if (m != null) {
                    return m;
                }
            }
        }

        return null;
    }

    private static void heldItems(SprauteModelEntity e, SpModelInstance inst, EntityLibrary.Look look, PoseStack pose, MultiBufferSource buffers, int light) {
        ItemStack main = e.getMainHandItem();
        ItemStack off = e.getOffhandItem();
        if (!main.isEmpty() || !off.isEmpty()) {
            EntityLibrary.Def d = look.def() != null ? look.def() : new EntityLibrary.Def();
            if (!main.isEmpty()) {
                hand(e, inst, main, d.itemBone, RIGHT, RIGHT_ARM, false, d, 1, pose, buffers, light);
            }

            if (!off.isEmpty()) {
                hand(e, inst, off, d.offhandBone, LEFT, LEFT_ARM, true, d, -1, pose, buffers, light);
            }
        }
    }

    private static void hand(
        SprauteModelEntity e,
        SpModelInstance inst,
        ItemStack stack,
        String chosen,
        String[] hands,
        String[] arms,
        boolean leftHand,
        EntityLibrary.Def d,
        int side,
        PoseStack pose,
        MultiBufferSource buffers,
        int light
    ) {
        SpMatrix4 b = chosen != null && !chosen.isEmpty() ? bone(inst, chosen) : bone(inst, hands);
        boolean arm = false;
        if (b == null) {
            b = bone(inst, arms);
            arm = true;
        }

        if (b != null) {
            pose.pushPose();
            toBone(pose, b);
            Mc.mulPose(pose, Axes.XP.rotationDegrees(-90.0F));
            Mc.mulPose(pose, Axes.YP.rotationDegrees(180.0F));
            if (arm) {
                pose.translate(0.0, 0.125, -0.625);
            }

            pose.translate(d.itemX / 16.0F * side, d.itemY / 16.0F, d.itemZ / 16.0F);
            if (d.itemTurnX != 0.0F) {
                Mc.mulPose(pose, Axes.XP.rotationDegrees(d.itemTurnX));
            }

            if (d.itemTurnY != 0.0F) {
                Mc.mulPose(pose, Axes.YP.rotationDegrees(d.itemTurnY * side));
            }

            if (d.itemTurnZ != 0.0F) {
                Mc.mulPose(pose, Axes.ZP.rotationDegrees(d.itemTurnZ * side));
            }

            if (d.itemScale != 1.0F && d.itemScale > 0.0F) {
                pose.scale(d.itemScale, d.itemScale, d.itemScale);
            }

            Mc.heldItem(stack, leftHand, e, pose, buffers, light);
            pose.popPose();
        }
    }

    public ResourceLocation getTextureLocation(SprauteModelEntity e) {
        return texture(e);
    }

    protected boolean shouldShowName(SprauteModelEntity e) {
        return e.hasCustomName() && e.isCustomNameVisible();
    }

    private static void drawShapes(SprauteModelEntity e, PoseStack pose, MultiBufferSource buffers) {
        VertexConsumer vc = buffers.getBuffer(RenderType.lines());
        Pose last = pose.last();
        int[][] edges = new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

        for (int i = 0; i < 8; i++) {
            PartShape sh = e.shape(i);
            if (sh != null) {
                Vector3f[] c = sh.corners();

                for (int[] ed : edges) {
                    Vector3f a = c[ed[0]];
                    Vector3f b = c[ed[1]];
                    Vector3f n = new Vector3f(b).sub(a);
                    if (!(n.lengthSquared() < 1.0E-8)) {
                        n.normalize();
                        Mc.normal(Mc.vertex(vc, Mc.pose(last), a.x, a.y, a.z).color(1.0F, 0.85F, 0.2F, 1.0F), Mc.normal(last), n.x, n.y, n.z).endVertex();
                        Mc.normal(Mc.vertex(vc, Mc.pose(last), b.x, b.y, b.z).color(1.0F, 0.85F, 0.2F, 1.0F), Mc.normal(last), n.x, n.y, n.z).endVertex();
                    }
                }
            }
        }
    }

    static ResourceLocation texture(SprauteModelEntity e) {
        ResourceLocation over = e.textureOverride();
        if (over != null) {
            return over;
        } else {
            EntityLibrary.Look l = e.look();
            return l != null && l.texture() != null ? l.texture() : MISSING;
        }
    }

    private static final class Posed {
        final SpGeoModel model;
        final SpModelInstance instance;
        float pupilLeft;
        float pupilRight;
        int age;
        boolean stale = true;

        Posed(SpGeoModel model, SpModelInstance instance, int id) {
            this.model = model;
            this.instance = instance;
            this.age = id & 7;
        }
    }
}
