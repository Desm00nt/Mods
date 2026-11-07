package org.sprautestudio.spraute_engine.runtime;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class ExtraSlots {
    public static final int COLUMNS = 6;
    public static final int LEFT = 184;
    public static final int TOP = 8;
    public static final int CELL = 18;
    public static final int MENU_FIRST = 46;
    public static final int COUNT = 54;

    private ExtraSlots() {
    }

    public static SimpleContainer of(Inventory inv) {
        return ((ExtraSlots.Holder)inv).spraute$extra();
    }

    public static void add(Player p, ItemStack stack) {
        Container extra = of(p.getInventory());
        int open = Math.min(PlayerSlots.extra(p), extra.getContainerSize());

        for (int i = 0; i < open && !stack.isEmpty(); i++) {
            ItemStack there = extra.getItem(i);
            if (!there.isEmpty() && ItemStack.isSameItemSameTags(there, stack)) {
                int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                if (moved > 0) {
                    there.grow(moved);
                    stack.shrink(moved);
                    extra.setChanged();
                }
            }
        }

        for (int ix = 0; ix < open && !stack.isEmpty(); ix++) {
            if (extra.getItem(ix).isEmpty()) {
                extra.setItem(ix, stack.split(stack.getCount()));
            }
        }
    }

    public static final class Cell extends Slot {
        private final Player player;

        public Cell(Container container, int index, Player player) {
            super(container, index, 184 + index % 6 * 18, 8 + index / 6 * 18);
            this.player = player;
        }

        @Override
        public boolean isActive() {
            return this.getContainerSlot() < PlayerSlots.extra(this.player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.isActive();
        }

        @Override
        public boolean mayPickup(Player who) {
            return this.isActive();
        }
    }

    public interface Holder {
        SimpleContainer spraute$extra();
    }
}
