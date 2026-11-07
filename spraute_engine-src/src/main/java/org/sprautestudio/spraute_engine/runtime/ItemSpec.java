package org.sprautestudio.spraute_engine.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.lang.ItemText;

public final class ItemSpec {
    private static final Map<String, ItemSpec.Parsed> CACHE = new ConcurrentHashMap<>();

    private ItemSpec() {
    }

    public static void install() {
        ItemText.setNbtCheck(text -> {
            try {
                TagParser.parseTag(text);
                return null;
            } catch (Exception var2) {
                return var2.getMessage();
            }
        });
    }

    private static ItemSpec.Parsed parse(String spec) {
        String key = spec == null ? "" : spec.trim();
        ItemSpec.Parsed have = CACHE.get(key);
        if (have != null) {
            return have;
        } else {
            String id = ItemText.id(key);
            String text = ItemText.nbt(key);
            CompoundTag nbt = null;
            String error = null;
            if (!text.isEmpty()) {
                try {
                    nbt = TagParser.parseTag(text);
                } catch (Exception var8) {
                    error = var8.getMessage();
                    SprauteEngine.LOGGER.warn("Spraute: bad NBT of item {}: {}", key, error);
                }
            }

            ItemSpec.Parsed p = new ItemSpec.Parsed(id, text, nbt, error);
            if (CACHE.size() > 1024) {
                CACHE.clear();
            }

            CACHE.put(key, p);
            return p;
        }
    }

    public static String id(String spec) {
        return parse(spec).id;
    }

    public static String nbt(String spec) {
        return parse(spec).nbtText;
    }

    public static String error(String spec) {
        return parse(spec).error;
    }

    public static String with(String id, String nbt) {
        String text = nbt == null ? "" : nbt.trim();
        return text.isEmpty() ? id(id) : id(id) + text;
    }

    public static Item item(String spec) {
        ResourceLocation rl = ResourceLocation.tryParse(id(spec));
        return rl == null ? Items.AIR : Regs.ITEM.getOptional(rl).orElse(Items.AIR);
    }

    public static ItemStack stack(String spec, int count) {
        Item item = item(spec);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        } else {
            ItemStack s = new ItemStack(item, Math.max(1, count));
            CompoundTag nbt = parse(spec).nbt;
            if (nbt != null) {
                s.setTag(nbt.copy());
            }

            return s;
        }
    }

    public static String of(ItemStack st) {
        if (st != null && !st.isEmpty()) {
            String id = Regs.ITEM.getKey(st.getItem()).toString();
            return st.hasTag() ? id + st.getTag() : id;
        } else {
            return "";
        }
    }

    public static boolean matches(ItemStack st, String spec) {
        if (st != null && !st.isEmpty() && st.is(item(spec))) {
            CompoundTag nbt = parse(spec).nbt;
            return nbt == null || NbtUtils.compareNbt(nbt, st.getTag(), true);
        } else {
            return false;
        }
    }

    public static int count(Player p, String spec) {
        int n = 0;
        Inventory inv = p.getInventory();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (matches(s, spec)) {
                n += s.getCount();
            }
        }

        SimpleContainer extra = ExtraSlots.of(inv);

        for (int ix = 0; ix < extra.getContainerSize(); ix++) {
            ItemStack s = extra.getItem(ix);
            if (matches(s, spec)) {
                n += s.getCount();
            }
        }

        return n;
    }

    public static int take(Player p, String spec, int n) {
        int left = n;
        Inventory inv = p.getInventory();

        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (matches(s, spec)) {
                int k = Math.min(left, s.getCount());
                s.shrink(k);
                left -= k;
            }
        }

        SimpleContainer extra = ExtraSlots.of(inv);

        for (int ix = 0; ix < extra.getContainerSize() && left > 0; ix++) {
            ItemStack s = extra.getItem(ix);
            if (matches(s, spec)) {
                int k = Math.min(left, s.getCount());
                s.shrink(k);
                left -= k;
            }
        }

        extra.setChanged();
        inv.setChanged();
        return n - left;
    }

    private record Parsed(String id, String nbtText, CompoundTag nbt, String error) {
    }
}
