package org.sprautestudio.spraute_engine.compat;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.crafting.Ingredient;

public abstract class McArmor extends ArmorItem {
    protected McArmor(ArmorSpec spec, String slot, Properties properties) {
        super(material(spec), type(slot), properties);
    }

    public final EquipmentSlot slot() {
        return this.getEquipmentSlot();
    }

    private static Type type(String slot) {
        return switch (slot) {
            case "head" -> Type.HELMET;
            case "legs" -> Type.LEGGINGS;
            case "feet" -> Type.BOOTS;
            default -> Type.CHESTPLATE;
        };
    }

    private static ArmorMaterial material(final ArmorSpec s) {
        return new ArmorMaterial() {
            @Override
            public int getDurabilityForType(Type type) {
                return s.durability();
            }

            @Override
            public int getDefenseForType(Type type) {
                return s.defense();
            }

            @Override
            public int getEnchantmentValue() {
                return s.enchantability();
            }

            @Override
            public SoundEvent getEquipSound() {
                return s.sound();
            }

            @Override
            public Ingredient getRepairIngredient() {
                return s.repair().get();
            }

            @Override
            public String getName() {
                return s.name();
            }

            @Override
            public float getToughness() {
                return s.toughness();
            }

            @Override
            public float getKnockbackResistance() {
                return s.knockbackResistance();
            }
        };
    }
}
