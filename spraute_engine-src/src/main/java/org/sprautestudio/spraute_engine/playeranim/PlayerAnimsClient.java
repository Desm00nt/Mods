package org.sprautestudio.spraute_engine.playeranim;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.math.SpVec3;
import org.sprautestudio.spraute_engine.core.parser.SpAnimationParser;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.runtime.Anims;

public final class PlayerAnimsClient {
    public static final Map<Item, PlayerAnimsClient.ItemAnims> ITEMS = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerAnimsClient.Play> STORY = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerAnimsClient.Play> SWING = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> SWING_TICK = new ConcurrentHashMap<>();
    private static final Map<String, Animation> HELD = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> DRAWING = new ConcurrentHashMap<>();
    private static final SpVec3 ROT = new SpVec3();
    private static final SpVec3 POS = new SpVec3();

    private PlayerAnimsClient() {
    }

    public static void play(UUID player, String anim, float speed, float fade, int times, int mode) {
        if (anim.isEmpty()) {
            PlayerAnimsClient.Play p = STORY.get(player);
            if (p != null) {
                p.stopAt = System.nanoTime();
                p.stopFade = fade;
            }
        } else {
            Anims.resolve(anim).ifPresent(a -> STORY.put(player, new PlayerAnimsClient.Play(a, speed, fade, times, mode)));
        }
    }

    public static void clear() {
        STORY.clear();
        SWING.clear();
        SWING_TICK.clear();
        HELD.clear();
        DRAWING.clear();
    }

    public static void apply(PlayerModel<?> model, LivingEntity e) {
        long now = System.nanoTime();
        UUID id = e.getUUID();
        PlayerAnimsClient.ItemAnims items = ITEMS.isEmpty() ? null : ITEMS.get(e.getMainHandItem().getItem());
        boolean any = false;
        if (items != null && !items.hold.isEmpty()) {
            Animation a = HELD.computeIfAbsent(items.hold, k -> Anims.resolve(k).orElse(Animation.NULL));
            if (a.clip() != null && a.clip().getLengthSec() > 0.0F) {
                pose(model, a.clip(), (float)now / 1.0E9F % a.clip().getLengthSec(), 1.0F, false);
                any = true;
            }
        }

        if (items != null && !items.swing.isEmpty() && e.swinging && e.swingTime <= 0) {
            Integer last = SWING_TICK.get(id);
            if (last == null || last != e.tickCount) {
                SWING_TICK.put(id, e.tickCount);
                Anims.resolve(items.swing).ifPresent(a -> SWING.put(id, new PlayerAnimsClient.Play(a, 1.0F, 0.05F, 1, 0)));
            }
        }

        any |= bow(model, e, id);
        any |= layer(SWING, id, model, now);
        any |= layer(STORY, id, model, now);
        if (any) {
            copyOuter(model);
        }
    }

    private static boolean layer(Map<UUID, PlayerAnimsClient.Play> plays, UUID id, PlayerModel<?> model, long now) {
        PlayerAnimsClient.Play p = plays.get(id);
        if (p == null) {
            return false;
        } else {
            float t = p.clipTime(now);
            float w = p.weight(now);
            if (!Float.isNaN(t) && (p.stopAt == 0L || !(w <= 0.0F))) {
                if (p.anim.clip() != null && !(w <= 0.0F)) {
                    pose(model, p.anim.clip(), t, w, p.upper);
                    if (p.upper) {
                        aim(model, w);
                    }

                    return true;
                } else {
                    return false;
                }
            } else {
                plays.remove(id);
                return false;
            }
        }
    }

    private static boolean bow(PlayerModel<?> model, LivingEntity e, UUID id) {
        EngineConfig.Bow cfg = EngineClient.config().bow;
        if (cfg != null && cfg.enabled) {
            boolean drawing = e.isUsingItem() && e.getUseItem().getUseAnimation() == UseAnim.BOW;
            if (drawing) {
                boolean right = e.getUsedItemHand() == InteractionHand.MAIN_HAND == (e.getMainArm() == HumanoidArm.RIGHT);
                DRAWING.put(id, right);
                Animation a = HELD.computeIfAbsent(right ? cfg.draw : cfg.drawLeft, k -> Anims.resolve(k).orElse(Animation.NULL));
                if (a.clip() == null) {
                    return false;
                } else {
                    float pulled = Math.min(1.0F, (float)e.getTicksUsingItem() / Math.max(1, cfg.fullDraw));
                    pose(model, a.clip(), pulled * a.clip().getLengthSec(), 1.0F, true);
                    aim(model, 1.0F);
                    return true;
                }
            } else {
                Boolean right = DRAWING.remove(id);
                if (right != null) {
                    Anims.resolve(right ? cfg.release : cfg.releaseLeft).ifPresent(ax -> {
                        PlayerAnimsClient.Play p = new PlayerAnimsClient.Play(ax, 1.0F, 0.08F, 1, 0);
                        p.upper = true;
                        SWING.put(id, p);
                    });
                }

                return false;
            }
        } else {
            DRAWING.remove(id);
            return false;
        }
    }

    private static void aim(PlayerModel<?> m, float w) {
        m.rightArm.xRot = m.rightArm.xRot + m.head.xRot * w;
        m.leftArm.xRot = m.leftArm.xRot + m.head.xRot * w;
        m.rightArm.yRot = m.rightArm.yRot + m.head.yRot * w;
        m.leftArm.yRot = m.leftArm.yRot + m.head.yRot * w;
    }

    private static void pose(PlayerModel<?> m, SpAnimationParser.AnimationClip clip, float t, float w, boolean upper) {
        float bx = 0.0F;
        float by = 0.0F;
        float bz = 0.0F;
        boolean body = false;

        for (String bone : clip.bones().keySet()) {
            if (clip.sampleBone(bone, t, ROT, POS)) {
                String n = bone.toLowerCase(Locale.ROOT).replace("_", "");
                if (n.equals("head")) {
                    if (!upper) {
                        m.head.xRot = m.head.xRot + (float)Math.toRadians(ROT.x) * w;
                        m.head.yRot = m.head.yRot + (float)Math.toRadians(ROT.y) * w;
                        m.head.zRot = m.head.zRot + (float)Math.toRadians(ROT.z) * w;
                    }
                } else {
                    ModelPart part = switch (n) {
                        case "rightarm" -> m.rightArm;
                        case "leftarm" -> m.leftArm;
                        case "rightleg" -> upper ? null : m.rightLeg;
                        case "leftleg" -> upper ? null : m.leftLeg;
                        default -> null;
                    };
                    if (part != null) {
                        set(part, ROT, POS, w);
                    } else if (n.equals("body") || n.equals("bodymain") || n.equals("bodyknee") || n.equals("torso") || n.equals("upperbody")) {
                        bx += ROT.x;
                        by += ROT.y;
                        bz += ROT.z;
                        body = true;
                    }
                }
            }
        }

        if (body) {
            ROT.set(bx, by, bz);
            POS.set(0.0F, 0.0F, 0.0F);
            set(m.body, ROT, POS, w);
        }
    }

    private static void set(ModelPart p, SpVec3 rot, SpVec3 pos, float w) {
        float rx = (float)Math.toRadians(rot.x);
        float ry = (float)Math.toRadians(rot.y);
        float rz = (float)Math.toRadians(rot.z);
        p.xRot = p.xRot + (rx - p.xRot) * w;
        p.yRot = p.yRot + (ry - p.yRot) * w;
        p.zRot = p.zRot + (rz - p.zRot) * w;
        if (pos.x != 0.0F || pos.y != 0.0F || pos.z != 0.0F) {
            PartPose rest = p.getInitialPose();
            p.x = p.x + (rest.x + pos.x - p.x) * w;
            p.y = p.y + (rest.y - pos.y - p.y) * w;
            p.z = p.z + (rest.z + pos.z - p.z) * w;
        }
    }

    private static void copyOuter(PlayerModel<?> m) {
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
    }

    public record ItemAnims(String hold, String swing) {
    }

    private static final class Play {
        final Animation anim;
        final long start = System.nanoTime();
        final float speed;
        final float fade;
        final int times;
        final int mode;
        long stopAt;
        float stopFade;
        boolean upper;

        Play(Animation anim, float speed, float fade, int times, int mode) {
            this.anim = anim;
            this.speed = speed <= 0.0F ? 1.0F : speed;
            this.fade = Math.max(0.0F, fade);
            this.times = Math.max(1, times);
            this.mode = mode;
        }

        float seconds(long now) {
            return (float)(now - this.start) / 1.0E9F * this.speed;
        }

        float clipTime(long now) {
            float len = this.anim.clip() == null ? 0.0F : this.anim.clip().getLengthSec();
            float t = this.seconds(now);
            if (!(len <= 0.0F)) {
                return switch (this.mode) {
                    case 1 -> t % len;
                    case 2 -> Math.min(t, len);
                    default -> t >= len * this.times ? Float.NaN : t % len;
                };
            } else {
                return this.mode == 0 && t > 0.05F ? Float.NaN : 0.0F;
            }
        }

        float weight(long now) {
            float w = this.fade <= 0.0F ? 1.0F : Math.min(1.0F, (float)(now - this.start) / 1.0E9F / this.fade);
            if (this.mode == 0 && this.fade > 0.0F && this.anim.clip() != null) {
                float left = this.anim.clip().getLengthSec() * this.times - this.seconds(now);
                w = Math.min(w, Math.max(0.0F, left / this.speed / this.fade));
            }

            if (this.stopAt != 0L) {
                w *= this.stopFade <= 0.0F ? 0.0F : Math.max(0.0F, 1.0F - (float)(now - this.stopAt) / 1.0E9F / this.stopFade);
            }

            return w;
        }
    }
}
