package net.minecraftforge.registries;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
public interface IForgeRegistry<V> extends Iterable<V> {
    ResourceLocation getRegistryName();
    boolean containsKey(ResourceLocation key);
    boolean containsValue(V value);
    boolean isEmpty();
    V getValue(ResourceLocation key);
    ResourceLocation getKey(V value);
    Optional<ResourceKey<V>> getResourceKey(V value);
    Set<ResourceLocation> getKeys();
    Collection<V> getValues();
    Set<Map.Entry<ResourceKey<V>, V>> getEntries();
}
