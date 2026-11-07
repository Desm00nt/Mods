package net.minecraftforge.registries;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraftforge.eventbus.api.IEventBus;
public class DeferredRegister<T> {
    public static <B> DeferredRegister<B> create(IForgeRegistry<B> reg, String modid) { throw new RuntimeException(); }
    public static <B> DeferredRegister<B> create(ResourceKey<? extends Registry<B>> key, String modid) { throw new RuntimeException(); }
    public <I extends T> RegistryObject<I> register(String name, Supplier<? extends I> sup) { throw new RuntimeException(); }
    public void register(IEventBus bus) { throw new RuntimeException(); }
}
