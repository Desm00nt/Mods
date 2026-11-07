package net.minecraftforge.registries;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
public final class RegistryObject<T> implements Supplier<T> {
    public T get() { throw new RuntimeException(); }
    public ResourceLocation getId() { throw new RuntimeException(); }
    public boolean isPresent() { throw new RuntimeException(); }
}
