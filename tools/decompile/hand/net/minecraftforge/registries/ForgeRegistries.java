package net.minecraftforge.registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
public class ForgeRegistries {
    public static IForgeRegistry<Block> BLOCKS;
    public static IForgeRegistry<Item> ITEMS;
    public static IForgeRegistry<EntityType<?>> ENTITY_TYPES;
    public static IForgeRegistry<BlockEntityType<?>> BLOCK_ENTITY_TYPES;
    public static IForgeRegistry<SoundEvent> SOUND_EVENTS;
    public static IForgeRegistry<MobEffect> MOB_EFFECTS;
}
