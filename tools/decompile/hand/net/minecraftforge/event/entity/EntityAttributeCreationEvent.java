package net.minecraftforge.event.entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
public class EntityAttributeCreationEvent extends net.minecraftforge.eventbus.api.Event {
    public void put(EntityType<? extends LivingEntity> entity, AttributeSupplier map) { throw new RuntimeException(); }
}
