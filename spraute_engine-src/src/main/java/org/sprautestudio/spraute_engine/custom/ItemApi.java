package org.sprautestudio.spraute_engine.custom;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class ItemApi {
    private ItemApi() {
    }

    private static ItemStack stack(String item) {
        ItemStack s = ItemSpec.stack(item, 1);
        if (s.isEmpty()) {
            throw new SprauteRuntimeException("Нет такого предмета: " + item);
        } else {
            return s;
        }
    }

    private static ItemDef custom(String item) {
        ResourceLocation key = ResourceLocation.tryParse(ItemSpec.id(item));
        if (key != null && key.getNamespace().equals("spraute_engine")) {
            CustomItems.Registered r = CustomItems.registered().get(key.getPath());
            return r == null ? null : r.def();
        } else {
            return null;
        }
    }

    public static List<Object> customItems() {
        List<Object> out = new ArrayList<>();

        for (String id : CustomItems.registered().keySet()) {
            out.add("spraute_engine:" + id);
        }

        for (String id : CustomBlocks.registered().keySet()) {
            out.add("spraute_engine:" + id);
        }

        return out;
    }

    public static boolean isCustom(String item) {
        if (custom(item) != null) {
            return true;
        } else {
            ResourceLocation key = ResourceLocation.tryParse(ItemSpec.id(item));
            return key != null && key.getNamespace().equals("spraute_engine") && CustomBlocks.registered().containsKey(key.getPath());
        }
    }

    public static String name(String item) {
        return stack(item).getHoverName().getString();
    }

    public static double maxStack(String item) {
        return stack(item).getMaxStackSize();
    }

    public static double durability(String item) {
        return stack(item).getMaxDamage();
    }

    public static String kind(String item) {
        ItemDef def = custom(item);
        if (def != null) {
            return def.kind;
        } else {
            ItemStack s = stack(item);
            Item it = s.getItem();
            if (it instanceof SwordItem) {
                return "sword";
            } else if (it instanceof PickaxeItem) {
                return "pickaxe";
            } else if (it instanceof AxeItem) {
                return "axe";
            } else if (it instanceof ShovelItem) {
                return "shovel";
            } else if (it instanceof HoeItem) {
                return "hoe";
            } else if (it instanceof ArmorItem) {
                return "armor";
            } else if (s.isEdible()) {
                return "food";
            } else {
                return it instanceof BlockItem ? "block" : "generic";
            }
        }
    }

    public static boolean onCooldown(ServerPlayer p, String item) {
        return p != null && p.getCooldowns().isOnCooldown(stack(item).getItem());
    }

    public static void cooldown(ServerPlayer p, String item, int ticks) {
        if (p != null) {
            p.getCooldowns().addCooldown(stack(item).getItem(), Math.max(0, ticks));
        }
    }

    private static ItemStack held(ServerPlayer p) {
        return p == null ? ItemStack.EMPTY : p.getMainHandItem();
    }

    public static void repair(ServerPlayer p, int amount) {
        ItemStack s = held(p);
        if (!s.isEmpty() && s.isDamageableItem()) {
            s.setDamageValue(amount < 0 ? 0 : Math.max(0, s.getDamageValue() - amount));
        }
    }

    public static void damage(ServerPlayer p, int amount) {
        ItemStack s = held(p);
        if (!s.isEmpty() && s.isDamageableItem() && amount > 0) {
            s.hurtAndBreak(amount, p, e -> e.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
    }

    public static void rename(ServerPlayer p, String name) {
        ItemStack s = held(p);
        if (!s.isEmpty()) {
            s.setHoverName(Component.literal(name.replace('&', '§')));
        }
    }

    public static void enchant(ServerPlayer p, String enchantment, int level) {
        ItemStack s = held(p);
        if (!s.isEmpty()) {
            String id = enchantment.contains(":") ? enchantment : "minecraft:" + enchantment;
            ResourceLocation key = ResourceLocation.tryParse(id);
            Enchantment e = key == null ? null : Regs.ENCHANTMENT.getOptional(key).orElse(null);
            if (e == null) {
                throw new SprauteRuntimeException("Нет такого зачарования: " + enchantment);
            } else {
                s.enchant(e, Math.max(1, level));
            }
        }
    }

    public static double heldDamage(ServerPlayer p) {
        return held(p).getDamageValue();
    }

    public static double heldLeft(ServerPlayer p) {
        ItemStack s = held(p);
        return s.isDamageableItem() ? s.getMaxDamage() - s.getDamageValue() : 0.0;
    }
}
