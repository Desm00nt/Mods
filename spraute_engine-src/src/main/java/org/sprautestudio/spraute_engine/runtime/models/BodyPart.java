package org.sprautestudio.spraute_engine.runtime.models;

import net.minecraft.world.entity.Entity;

public interface BodyPart {
    SprauteModelEntity body();

    int index();

    boolean active();

    void setActive(boolean var1);

    void resize(float var1, float var2);

    default Entity entity() {
        return (Entity)this;
    }
}
