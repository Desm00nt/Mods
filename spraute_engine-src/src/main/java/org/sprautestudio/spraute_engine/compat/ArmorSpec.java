package org.sprautestudio.spraute_engine.compat;

import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.crafting.Ingredient;

public record ArmorSpec(
    String name, int durability, int defense, int enchantability, SoundEvent sound, float toughness, float knockbackResistance, Supplier<Ingredient> repair
) {
}
