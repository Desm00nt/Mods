package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.runtime.ExtraSlots;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;

@Mixin(Inventory.class)
public abstract class RulesInventoryMixin implements ExtraSlots.Holder {
    @Unique
    private static final int spraute$SAVED_FROM = 200;
    @Shadow
    @Final
    public NonNullList<ItemStack> items;
    @Shadow
    @Final
    public Player player;
    @Shadow
    public int selected;
    @Unique
    private final SimpleContainer spraute$extra = new SimpleContainer(54);

    @Shadow
    private boolean hasRemainingSpaceForItem(ItemStack there, ItemStack stack) {
        throw new AssertionError();
    }

    @Shadow
    public abstract ItemStack getItem(int var1);

    @Override
    public SimpleContainer spraute$extra() {
        return this.spraute$extra;
    }

    @Inject(method = "getFreeSlot", at = @At("HEAD"), cancellable = true)
    private void spraute$free(CallbackInfoReturnable<Integer> cir) {
        if (PlayerSlots.limited(this.player)) {
            for (int i = 0; i < this.items.size(); i++) {
                if (this.items.get(i).isEmpty() && !PlayerSlots.locked(this.player, i)) {
                    cir.setReturnValue(i);
                    return;
                }
            }

            cir.setReturnValue(-1);
        }
    }

    @Inject(method = "getSlotWithRemainingSpace", at = @At("HEAD"), cancellable = true)
    private void spraute$space(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (PlayerSlots.limited(this.player)) {
            int answer = -1;
            if (!PlayerSlots.locked(this.player, this.selected) && this.hasRemainingSpaceForItem(this.getItem(this.selected), stack)) {
                answer = this.selected;
            } else if (!PlayerSlots.locked(this.player, 40) && this.hasRemainingSpaceForItem(this.getItem(40), stack)) {
                answer = 40;
            } else {
                for (int i = 0; i < this.items.size(); i++) {
                    if (!PlayerSlots.locked(this.player, i) && this.hasRemainingSpaceForItem(this.items.get(i), stack)) {
                        answer = i;
                        break;
                    }
                }
            }

            cir.setReturnValue(answer);
        }
    }

    @Inject(method = "add(ILnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true)
    private void spraute$overflow(int cell, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (cell == -1 && !stack.isEmpty() && !Mc.level(this.player).isClientSide && PlayerSlots.extra(this.player) > 0) {
            int before = stack.getCount();
            ExtraSlots.add(this.player, stack);
            if (stack.getCount() < before) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void spraute$save(ListTag list, CallbackInfoReturnable<ListTag> cir) {
        for (int i = 0; i < this.spraute$extra.getContainerSize(); i++) {
            ItemStack st = this.spraute$extra.getItem(i);
            if (!st.isEmpty()) {
                CompoundTag tag = new CompoundTag();
                tag.putByte("Slot", (byte)(200 + i));
                st.save(tag);
                list.add(tag);
            }
        }
    }

    @Inject(method = "load", at = @At("TAIL"))
    private void spraute$load(ListTag list, CallbackInfo ci) {
        this.spraute$extra.clearContent();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            int cell = (tag.getByte("Slot") & 255) - 200;
            if (cell >= 0 && cell < this.spraute$extra.getContainerSize()) {
                this.spraute$extra.setItem(cell, ItemStack.of(tag));
            }
        }
    }

    @Inject(method = "dropAll", at = @At("TAIL"))
    private void spraute$drop(CallbackInfo ci) {
        for (int i = 0; i < this.spraute$extra.getContainerSize(); i++) {
            ItemStack st = this.spraute$extra.getItem(i);
            if (!st.isEmpty()) {
                this.player.drop(st, true, false);
                this.spraute$extra.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Inject(method = "replaceWith", at = @At("TAIL"))
    private void spraute$copy(Inventory other, CallbackInfo ci) {
        SimpleContainer from = ExtraSlots.of(other);

        for (int i = 0; i < this.spraute$extra.getContainerSize(); i++) {
            this.spraute$extra.setItem(i, from.getItem(i));
        }
    }

    @Inject(method = "clearContent", at = @At("TAIL"))
    private void spraute$clear(CallbackInfo ci) {
        this.spraute$extra.clearContent();
    }
}
