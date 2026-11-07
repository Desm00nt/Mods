package org.sprautestudio.spraute_engine.custom;

import com.google.common.collect.Multimap;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.sprautestudio.spraute_engine.compat.McArmor;

final class CustomItemKinds {
    private CustomItemKinds() {
    }

    static final class Armor extends McArmor {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Armor(ItemDef def, Properties properties) {
            super(CustomItems.armor(def), def.armorSlot, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }
    }

    static final class Axe extends AxeItem {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Axe(ItemDef def, Properties properties) {
            super(CustomItems.tier(def), CustomItems.wholeDamage(def), def.attackSpeed - 4.0F, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (this.def.isFood()) {
                return super.use(level, player, hand);
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (CustomItems.holds(this.def)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else {
                    return !this.def.hasUseAction()
                        ? super.use(level, player, hand)
                        : InteractionResultHolder.sidedSuccess(CustomItems.act(this.def, this, stack, level, player, hand), level.isClientSide);
                }
            }
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            ItemStack left = super.finishUsingItem(stack, level, user);
            return CustomItems.act(this.def, this, left, level, user, user.getUsedItemHand());
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return this.def.useTicks > 0 ? this.def.useTicks : super.getUseDuration(stack);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            UseAnim anim = CustomItems.useAnim(this.def);
            return anim != null ? anim : super.getUseAnimation(stack);
        }
    }

    static final class Hoe extends HoeItem {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Hoe(ItemDef def, Properties properties) {
            super(CustomItems.tier(def), CustomItems.wholeDamage(def), def.attackSpeed - 4.0F, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (this.def.isFood()) {
                return super.use(level, player, hand);
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (CustomItems.holds(this.def)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else {
                    return !this.def.hasUseAction()
                        ? super.use(level, player, hand)
                        : InteractionResultHolder.sidedSuccess(CustomItems.act(this.def, this, stack, level, player, hand), level.isClientSide);
                }
            }
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            ItemStack left = super.finishUsingItem(stack, level, user);
            return CustomItems.act(this.def, this, left, level, user, user.getUsedItemHand());
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return this.def.useTicks > 0 ? this.def.useTicks : super.getUseDuration(stack);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            UseAnim anim = CustomItems.useAnim(this.def);
            return anim != null ? anim : super.getUseAnimation(stack);
        }
    }

    static final class Pickaxe extends PickaxeItem {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Pickaxe(ItemDef def, Properties properties) {
            super(CustomItems.tier(def), CustomItems.wholeDamage(def), def.attackSpeed - 4.0F, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (this.def.isFood()) {
                return super.use(level, player, hand);
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (CustomItems.holds(this.def)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else {
                    return !this.def.hasUseAction()
                        ? super.use(level, player, hand)
                        : InteractionResultHolder.sidedSuccess(CustomItems.act(this.def, this, stack, level, player, hand), level.isClientSide);
                }
            }
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            ItemStack left = super.finishUsingItem(stack, level, user);
            return CustomItems.act(this.def, this, left, level, user, user.getUsedItemHand());
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return this.def.useTicks > 0 ? this.def.useTicks : super.getUseDuration(stack);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            UseAnim anim = CustomItems.useAnim(this.def);
            return anim != null ? anim : super.getUseAnimation(stack);
        }
    }

    static final class Plain extends Item {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Plain(ItemDef def, Properties properties) {
            super(properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (this.def.isFood()) {
                return super.use(level, player, hand);
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (CustomItems.holds(this.def)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else {
                    return !this.def.hasUseAction()
                        ? super.use(level, player, hand)
                        : InteractionResultHolder.sidedSuccess(CustomItems.act(this.def, this, stack, level, player, hand), level.isClientSide);
                }
            }
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            ItemStack left = super.finishUsingItem(stack, level, user);
            return CustomItems.act(this.def, this, left, level, user, user.getUsedItemHand());
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return this.def.useTicks > 0 ? this.def.useTicks : super.getUseDuration(stack);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            UseAnim anim = CustomItems.useAnim(this.def);
            return anim != null ? anim : super.getUseAnimation(stack);
        }

        @Override
        public boolean isEnchantable(ItemStack stack) {
            return this.def.enchantability > 0 && (stack.getMaxStackSize() == 1 || super.isEnchantable(stack));
        }

        @Override
        public int getEnchantmentValue() {
            return this.def.enchantability;
        }

        @Override
        public boolean isValidRepairItem(ItemStack stack, ItemStack with) {
            return CustomItems.ingredient(this.def.repair).test(with);
        }
    }

    static final class Shovel extends ShovelItem {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Shovel(ItemDef def, Properties properties) {
            super(CustomItems.tier(def), CustomItems.wholeDamage(def), def.attackSpeed - 4.0F, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (this.def.isFood()) {
                return super.use(level, player, hand);
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (CustomItems.holds(this.def)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else {
                    return !this.def.hasUseAction()
                        ? super.use(level, player, hand)
                        : InteractionResultHolder.sidedSuccess(CustomItems.act(this.def, this, stack, level, player, hand), level.isClientSide);
                }
            }
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            ItemStack left = super.finishUsingItem(stack, level, user);
            return CustomItems.act(this.def, this, left, level, user, user.getUsedItemHand());
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return this.def.useTicks > 0 ? this.def.useTicks : super.getUseDuration(stack);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            UseAnim anim = CustomItems.useAnim(this.def);
            return anim != null ? anim : super.getUseAnimation(stack);
        }
    }

    static final class Sword extends SwordItem {
        private final ItemDef def;
        private final Map<EquipmentSlot, Multimap<Attribute, AttributeModifier>> modifiers = new EnumMap<>(EquipmentSlot.class);

        Sword(ItemDef def, Properties properties) {
            super(CustomItems.tier(def), CustomItems.wholeDamage(def), def.attackSpeed - 4.0F, properties);
            this.def = def;
        }

        @Override
        public Component getName(ItemStack stack) {
            return CustomItems.name(this.def);
        }

        @Override
        public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> out, TooltipFlag flag) {
            super.appendHoverText(stack, level, out, flag);
            CustomItems.tooltip(this.def, out);
        }

        @Override
        public boolean isFoil(ItemStack stack) {
            return this.def.glint || super.isFoil(stack);
        }

        @Override
        public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
            super.inventoryTick(stack, level, entity, slot, selected);
            CustomItems.tick(this.def, stack, level, entity, selected);
        }

        @Override
        public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
            boolean used = super.hurtEnemy(stack, target, attacker);
            CustomItems.hit(this.def, target);
            return used;
        }

        @Override
        public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
            return !this.def.noBlockBreak && super.canAttackBlock(state, level, pos, player);
        }

        @Override
        public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
            return this.modifiers.computeIfAbsent(slot, s -> CustomItems.modifiers(this.def, super.getDefaultAttributeModifiers(s), s));
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            if (this.def.isFood()) {
                return super.use(level, player, hand);
            } else {
                ItemStack stack = player.getItemInHand(hand);
                if (CustomItems.holds(this.def)) {
                    player.startUsingItem(hand);
                    return InteractionResultHolder.consume(stack);
                } else {
                    return !this.def.hasUseAction()
                        ? super.use(level, player, hand)
                        : InteractionResultHolder.sidedSuccess(CustomItems.act(this.def, this, stack, level, player, hand), level.isClientSide);
                }
            }
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
            ItemStack left = super.finishUsingItem(stack, level, user);
            return CustomItems.act(this.def, this, left, level, user, user.getUsedItemHand());
        }

        @Override
        public int getUseDuration(ItemStack stack) {
            return this.def.useTicks > 0 ? this.def.useTicks : super.getUseDuration(stack);
        }

        @Override
        public UseAnim getUseAnimation(ItemStack stack) {
            UseAnim anim = CustomItems.useAnim(this.def);
            return anim != null ? anim : super.getUseAnimation(stack);
        }
    }
}
