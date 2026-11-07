package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.SpawnPlacements.SpawnPredicate;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpawnPlacements.class)
public interface SpawnPlacementsAccess {
    @Invoker("register")
    static <T extends Mob> void spraute$register(EntityType<T> type, Type place, Types height, SpawnPredicate<T> rule) {
        throw new AssertionError();
    }
}
