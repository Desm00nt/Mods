package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.function.Supplier;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.custom.CustomMobs;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelRenderer;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;
import org.sprautestudio.spraute_engine.runtime.orb.SprauteOrb;

public class OrbRenderer extends EntityRenderer<SprauteOrb> {
    private static final ResourceLocation XP = new ResourceLocation("minecraft", "textures/entity/experience_orb.png");

    public OrbRenderer(Context ctx) {
        super(ctx);
        this.shadowRadius = 0.08F;
    }

    public void render(SprauteOrb orb, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float t = orb.age + partial;
        float size = orb.size();
        int life = orb.life();
        float alpha = Math.min(1.0F, t / 6.0F);
        if (life > 0) {
            alpha = Math.min(alpha, Math.max(0.0F, (life - t) / 20.0F));
        }

        float pulse = 1.0F + 0.08F * Mth.sin(t * 0.25F);
        String look = orb.look();
        pose.pushPose();
        pose.translate(0.0F, size / 2.0F + 0.05F * Mth.sin(t * 0.1F), 0.0F);
        if (look.startsWith("item:")) {
            Mc.mulPose(pose, Axes.YP.rotationDegrees(t * 4.0F));
            pose.scale(size * 2.0F * pulse, size * 2.0F * pulse, size * 2.0F * pulse);
            Mc.groundItem(ItemSpec.stack(look.substring(5), 1), 15728880, pose, buffers, Mc.level(orb), orb.getId());
            pose.popPose();
        } else {
            Mc.faceCamera(pose);
            Mc.mulPose(pose, Axes.YP.rotationDegrees(180.0F));
            pose.scale(size * pulse, size * pulse, size * pulse);
            ResourceLocation tex = look.isEmpty() ? XP : ResourceLocation.tryParse(look);
            if (tex == null) {
                tex = XP;
            }

            float u0 = 0.0F;
            float u1 = 1.0F;
            float v0 = 0.0F;
            float v1 = 1.0F;
            if (tex == XP) {
                int icon = 3;
                u0 = icon % 4 * 16 / 64.0F;
                u1 = (icon % 4 * 16 + 16) / 64.0F;
                v0 = icon / 4 * 16 / 64.0F;
                v1 = (icon / 4 * 16 + 16) / 64.0F;
            }

            int c = orb.color();
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            int a = Math.round((c >>> 24) / 255.0F * alpha * 255.0F);
            VertexConsumer vc = buffers.getBuffer(RenderType.itemEntityTranslucentCull(tex));
            Pose last = pose.last();
            Matrix4f m = Mc.pose(last);
            Matrix3f n = Mc.normal(last);
            vertex(vc, m, n, -0.5F, -0.5F, r, g, b, a, u0, v1);
            vertex(vc, m, n, 0.5F, -0.5F, r, g, b, a, u1, v1);
            vertex(vc, m, n, 0.5F, 0.5F, r, g, b, a, u1, v0);
            vertex(vc, m, n, -0.5F, 0.5F, r, g, b, a, u0, v0);
            pose.popPose();
            super.render(orb, yaw, partial, pose, buffers, light);
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, int r, int g, int b, int a, float u, float v) {
        Mc.normal(Mc.vertex(vc, m, x, y, 0.0F).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880), n, 0.0F, 1.0F, 0.0F)
            .endVertex();
    }

    public ResourceLocation getTextureLocation(SprauteOrb orb) {
        return XP;
    }

    public static final class Registration {
        public static void registerHooks() {
            ClientHooks.REGISTER_RENDERERS.add(OrbRenderer.Registration::renderers);
        }

        public static void renderers(ClientHooks.RegisterRenderers e) {
            e.registerEntityRenderer(Orbs.ORB.get(), OrbRenderer::new);
            e.registerEntityRenderer(Orbs.CAMERA.get(), NoopRenderer::new);
            e.registerEntityRenderer(Orbs.MODEL.get(), SprauteModelRenderer::new);

            for (Supplier<EntityType<SprauteModelEntity>> type : CustomMobs.types()) {
                e.registerEntityRenderer(type.get(), SprauteModelRenderer::new);
            }
        }
    }
}
