package org.sprautestudio.spraute_engine.custom;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.client.SpGeoRenderer;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.core.model.SpModelInstance;
import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public final class ModelDraw {
    private static final Map<String, SpModelInstance> POSED = new HashMap<>();

    private ModelDraw() {
    }

    public static void draw(CustomModels.Engine e, int facingTurn, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ModelLibrary.Model m = ModelLibrary.find(e.project(), e.look().name);
        if (m != null) {
            SpGeoModel geo = m.geometry();
            if (geo != null) {
                SpModelInstance inst = POSED.get(m.key());
                if (inst == null || inst.getModel() != geo) {
                    inst = new SpModelInstance(geo);
                    POSED.put(m.key(), inst);
                }

                inst.resetAnims();
                if (!e.look().anim.isEmpty()) {
                    SpAnimationParser.AnimationClip clip = ModelLibrary.animation(m, e.look().anim).map(a -> a.clip()).orElse(null);
                    if (clip != null && clip.getLengthSec() > 0.0F) {
                        clip.apply(inst, (float)(System.nanoTime() / 1.0E9 % clip.getLengthSec()), 1.0F, true, false);
                    }
                }

                inst.computeTransforms();
                ModelLook l = e.look();
                pose.pushPose();
                pose.translate(0.5, 0.0, 0.5);
                if (facingTurn != 0) {
                    Mc.mulPose(pose, Axes.YP.rotationDegrees(-90.0F * facingTurn));
                }

                pose.translate(l.x / 16.0F, l.y / 16.0F, l.z / 16.0F);
                if (l.turn != 0) {
                    Mc.mulPose(pose, Axes.YP.rotationDegrees(-90.0F * l.turn));
                }

                if (l.scale != 1.0F) {
                    pose.scale(l.scale, l.scale, l.scale);
                }

                SpGeoRenderer.render(
                    inst, pose, buffers.getBuffer(RenderType.entityCutoutNoCull(m.textureId())), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F, null, 0
                );
                pose.popPose();
            }
        }
    }
}
