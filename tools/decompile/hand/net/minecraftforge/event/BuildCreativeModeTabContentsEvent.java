package net.minecraftforge.event;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.item.ItemStack;
public class BuildCreativeModeTabContentsEvent extends net.minecraftforge.eventbus.api.Event {
    public ResourceKey<CreativeModeTab> getTabKey() { throw new RuntimeException(); }
    public CreativeModeTab getTab() { throw new RuntimeException(); }
    public void accept(ItemLike item) { throw new RuntimeException(); }
    public void accept(ItemStack stack) { throw new RuntimeException(); }
}
