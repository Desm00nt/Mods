package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;

public final class NpcItemPickup {
    private final AnimatedEntity owner;
    private final List<ItemStack> items = new ArrayList<>();
    private List<String> filters = List.of();
    private boolean active;
    private int limit = -1;
    private int collected;
    private int seen;
    private String lastItem = "";
    private boolean forGoal;
    private boolean storyStopped;

    public NpcItemPickup(AnimatedEntity owner) {
        this.owner = owner;
    }

    public void configure(List<String> filters, int limit) {
        this.begin(filters, limit);
        this.forGoal = false;
        this.storyStopped = false;
    }

    private void begin(List<String> filters, int limit) {
        this.filters = List.copyOf(filters);
        this.limit = limit < 0 ? -1 : limit;
        this.collected = 0;
        this.seen = 0;
        this.lastItem = "";
        this.active = true;
    }

    public void stop() {
        this.active = false;
        this.forGoal = false;
        this.storyStopped = true;
    }

    public void wantForGoal(List<String> goalFilters, int left) {
        if (!this.storyStopped && (!this.active || this.forGoal || this.limit >= 0 && this.collected >= this.limit)) {
            if (!this.active || !this.forGoal) {
                this.begin(goalFilters, left);
            }

            this.filters = List.copyOf(goalFilters);
            this.limit = this.collected + left;
            this.forGoal = true;
        }
    }

    public void endGoal() {
        if (this.forGoal) {
            this.forGoal = false;
            this.active = false;
        }
    }

    public boolean active() {
        return this.active;
    }

    public int collected() {
        return this.collected;
    }

    public String lastItem() {
        return this.lastItem;
    }

    public boolean finished() {
        return !this.active || (this.limit < 0 ? this.collected > this.seen : this.collected >= this.limit);
    }

    public void markSeen() {
        this.seen = this.collected;
    }

    public int count(String spec) {
        int n = 0;

        for (ItemStack stack : this.items) {
            if (spec == null || spec.isEmpty() || ItemSpec.matches(stack, spec)) {
                n += stack.getCount();
            }
        }

        return n;
    }

    public void tick() {
        if (this.active && !Mc.level(this.owner).isClientSide && this.owner.tickCount % 2 == 0 && (this.limit < 0 || this.collected < this.limit)) {
            for (ItemEntity entity : Mc.level(this.owner).getEntitiesOfClass(ItemEntity.class, this.owner.getBoundingBox().inflate(0.35, 0.25, 0.35))) {
                if (!entity.isRemoved() && !entity.hasPickUpDelay()) {
                    ItemStack ground = entity.getItem();
                    if (!ground.isEmpty() && this.allowed(ground)) {
                        int available = ground.getCount();
                        if (this.limit >= 0) {
                            available = Math.min(available, this.limit - this.collected);
                        }

                        if (available > 0) {
                            ItemStack taken = ground.copy();
                            taken.setCount(available);
                            this.store(taken);
                            ground.shrink(available);
                            this.owner.take(entity, available);
                            if (ground.isEmpty()) {
                                entity.discard();
                            } else {
                                entity.setItem(ground);
                            }

                            this.collected += available;
                            this.lastItem = Regs.ITEM.getKey(taken.getItem()).toString();
                            GameEvents.npcPickup(this.owner, taken);
                            if (this.limit >= 0 && this.collected >= this.limit) {
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean allowed(ItemStack stack) {
        if (this.filters.isEmpty()) {
            return true;
        } else {
            for (String filter : this.filters) {
                if (ItemSpec.matches(stack, filter)) {
                    return true;
                }
            }

            return false;
        }
    }

    private void store(ItemStack stack) {
        int left = stack.getCount();

        for (ItemStack stored : this.items) {
            if (ItemStack.isSameItemSameTags(stored, stack)) {
                int add = Math.min(left, stored.getMaxStackSize() - stored.getCount());
                if (add > 0) {
                    stored.grow(add);
                }

                left -= add;
                if (left == 0) {
                    return;
                }
            }
        }

        while (left > 0) {
            int addx = Math.min(left, stack.getMaxStackSize());
            ItemStack slot = stack.copy();
            slot.setCount(addx);
            this.items.add(slot);
            left -= addx;
        }
    }

    public void save(CompoundTag tag) {
        tag.putBoolean("Active", this.active);
        tag.putInt("Limit", this.limit);
        tag.putInt("Collected", this.collected);
        tag.putInt("Seen", this.seen);
        tag.putBoolean("ForGoal", this.forGoal);
        tag.putBoolean("StoryStopped", this.storyStopped);
        ListTag allowed = new ListTag();

        for (String filter : this.filters) {
            allowed.add(StringTag.valueOf(filter));
        }

        tag.put("Filters", allowed);
        ListTag inventory = new ListTag();

        for (ItemStack item : this.items) {
            inventory.add(item.save(new CompoundTag()));
        }

        tag.put("Items", inventory);
    }

    public void load(CompoundTag tag) {
        this.active = tag.getBoolean("Active");
        this.limit = tag.contains("Limit") ? tag.getInt("Limit") : -1;
        this.collected = Math.max(0, tag.getInt("Collected"));
        this.seen = tag.contains("Seen") ? tag.getInt("Seen") : this.collected;
        this.forGoal = tag.getBoolean("ForGoal");
        this.storyStopped = tag.getBoolean("StoryStopped");
        List<String> allowed = new ArrayList<>();
        ListTag savedFilters = tag.getList("Filters", 8);

        for (int i = 0; i < savedFilters.size(); i++) {
            allowed.add(savedFilters.getString(i));
        }

        this.filters = List.copyOf(allowed);
        this.items.clear();
        ListTag inventory = tag.getList("Items", 10);

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack item = ItemStack.of(inventory.getCompound(i));
            if (!item.isEmpty()) {
                this.items.add(item);
            }
        }
    }
}
