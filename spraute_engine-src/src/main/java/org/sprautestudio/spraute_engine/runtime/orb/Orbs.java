package org.sprautestudio.spraute_engine.runtime.orb;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

public final class Orbs {
    public static final Supplier<EntityType<SprauteOrb>> ORB = Platform.get()
        .entityType("orb", () -> Builder.of(SprauteOrb::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(8).updateInterval(2).build("orb"));
    public static final Supplier<EntityType<CameraEntity>> CAMERA = Platform.get()
        .entityType(
            "camera", () -> Builder.of(CameraEntity::new, MobCategory.MISC).sized(0.001F, 0.001F).noSave().noSummon().clientTrackingRange(0).build("camera")
        );
    public static final Supplier<EntityType<SprauteModelEntity>> MODEL = Platform.get()
        .entityType("model", () -> Builder.of(Platform.get()::modelEntity, MobCategory.CREATURE).sized(0.6F, 1.9F).clientTrackingRange(10).build("model"));

    private Orbs() {
    }

    public static void init() {
        Platform.get().entityAttributes(MODEL, () -> Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FOLLOW_RANGE, 48.0).build());
    }
}
