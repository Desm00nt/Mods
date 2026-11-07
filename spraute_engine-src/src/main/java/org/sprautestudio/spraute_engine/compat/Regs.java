package org.sprautestudio.spraute_engine.compat;

import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class Regs {
    public static final Registry<Attribute> ATTRIBUTE = BuiltInRegistries.ATTRIBUTE;
    public static final DefaultedRegistry<Block> BLOCK = BuiltInRegistries.BLOCK;
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITY_TYPE = BuiltInRegistries.BLOCK_ENTITY_TYPE;
    public static final Registry<Enchantment> ENCHANTMENT = BuiltInRegistries.ENCHANTMENT;
    public static final DefaultedRegistry<EntityType<?>> ENTITY_TYPE = BuiltInRegistries.ENTITY_TYPE;
    public static final DefaultedRegistry<Item> ITEM = BuiltInRegistries.ITEM;
    public static final Registry<MenuType<?>> MENU = BuiltInRegistries.MENU;
    public static final Registry<MobEffect> MOB_EFFECT = BuiltInRegistries.MOB_EFFECT;
    public static final Registry<ParticleType<?>> PARTICLE_TYPE = BuiltInRegistries.PARTICLE_TYPE;
    public static final Registry<SoundEvent> SOUND_EVENT = BuiltInRegistries.SOUND_EVENT;

    private Regs() {
    }
}
