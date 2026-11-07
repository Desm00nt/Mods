package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;

@Mixin(AbstractContainerMenu.class)
public abstract class RulesMenuMixin {
    private static final int spraute$OUTSIDE = -999;
    @Shadow
    @Final
    public NonNullList<Slot> slots;

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void spraute$click(int slot, int button, ClickType type, Player player, CallbackInfo ci) {
        boolean throwing = type == ClickType.THROW || type == ClickType.PICKUP && slot == -999;
        if (throwing && !PlayerRules.can(player, "drop")) {
            ci.cancel();
        } else if (PlayerSlots.limited(player)) {
            if (type == ClickType.SWAP && PlayerSlots.locked(player, button)) {
                ci.cancel();
            } else if (slot >= 0 && slot < this.slots.size()) {
                Slot target = this.slots.get(slot);
                if (target.container == player.getInventory() && PlayerSlots.locked(player, target.getContainerSlot())) {
                    ci.cancel();
                }
            }
        }
    }
}
