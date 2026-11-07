package org.sprautestudio.spraute_engine.addon;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.sprautestudio.spraute_engine.core.world.Location;

public interface StoryCall {
    boolean has(String var1);

    String text(String var1);

    double number(String var1, double var2);

    int ticks(String var1, int var2);

    Object value(String var1);

    Location position(String var1);

    Entity entity(String var1);

    ServerPlayer player();

    int scriptId();

    String project();
}
