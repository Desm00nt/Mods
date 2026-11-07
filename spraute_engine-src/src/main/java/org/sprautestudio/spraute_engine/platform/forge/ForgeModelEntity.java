package org.sprautestudio.spraute_engine.platform.forge;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.PartEntity;
import org.sprautestudio.spraute_engine.runtime.models.BodyPart;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

final class ForgeModelEntity extends SprauteModelEntity {
    private PartEntity<?>[] forgeParts;

    ForgeModelEntity(EntityType<SprauteModelEntity> type, Level level) {
        super(type, level);
    }

    public boolean isMultipartEntity() {
        return true;
    }

    public PartEntity<?>[] getParts() {
        if (this.forgeParts == null) {
            BodyPart[] parts = this.parts();
            this.forgeParts = new PartEntity[parts.length];

            for (int i = 0; i < parts.length; i++) {
                this.forgeParts[i] = (ForgePart)parts[i];
            }
        }

        return this.forgeParts;
    }
}
