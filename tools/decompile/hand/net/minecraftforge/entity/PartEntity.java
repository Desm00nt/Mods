package net.minecraftforge.entity;
import net.minecraft.world.entity.Entity;
public abstract class PartEntity<T extends Entity> extends Entity {
    public PartEntity(T parent) { }
    public T getParent() { throw new RuntimeException(); }
}
