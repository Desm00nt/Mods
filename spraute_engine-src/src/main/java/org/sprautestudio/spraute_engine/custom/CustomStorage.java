package org.sprautestudio.spraute_engine.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class CustomStorage extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    private boolean lootDone;

    CustomStorage(BlockPos pos, BlockState state) {
        super(CustomBlocks.storageType(), pos, state);
    }

    private BlockDef def() {
        return this.getBlockState().getBlock() instanceof CustomBlockKinds.Storage s ? s.def() : null;
    }

    void noLoot() {
        this.lootDone = true;
        this.setChanged();
    }

    void prepareLoot() {
        if (!this.lootDone && this.level != null) {
            this.lootDone = true;
            this.setChanged();
            BlockDef d = this.def();
            CustomBlocks.Registered r = d != null ? CustomBlocks.registered().get(d.id) : null;
            if (r != null) {
                String loot = CustomBlocks.current(r).storageLoot;
                ResourceLocation table = loot.isBlank() ? null : ResourceLocation.tryParse(LootDef.ref(loot.trim()));
                if (table != null) {
                    this.setLootTable(table, this.level.getRandom().nextLong());
                }
            }
        }
    }

    @Override
    public int getContainerSize() {
        BlockDef d = this.def();
        return (d != null ? Math.max(1, Math.min(6, d.storageRows)) : 3) * 9;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> next) {
        this.items = next;
    }

    @Override
    protected Component getDefaultName() {
        BlockDef d = this.def();
        return Component.literal(d != null ? d.name : "");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        int rows = this.getContainerSize() / 9;

        MenuType<ChestMenu> type = switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            default -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
        };
        return new ChestMenu(type, id, inventory, this, rows);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items);
        }

        this.lootDone = tag.getBoolean("SprauteLootDone");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items);
        }

        tag.putBoolean("SprauteLootDone", this.lootDone);
    }
}
