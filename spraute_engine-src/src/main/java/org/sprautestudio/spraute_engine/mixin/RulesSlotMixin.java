package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;

@Mixin(Slot.class)
public abstract class RulesSlotMixin {
    @Shadow
    @Final
    public Container container;

    @Shadow
    public abstract int getContainerSlot();

    @Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
    private void spraute$place(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory inv && PlayerSlots.locked(inv.player, this.getContainerSlot())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getMaxStackSize()I", at = @At("HEAD"), cancellable = true)
    private void spraute$room(CallbackInfoReturnable<Integer> cir) {
        if (this.container instanceof Inventory inv && PlayerSlots.locked(inv.player, this.getContainerSlot())) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void spraute$pickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.container instanceof Inventory inv && PlayerSlots.locked(inv.player, this.getContainerSlot())) {
            cir.setReturnValue(false);
        }
    }
}
