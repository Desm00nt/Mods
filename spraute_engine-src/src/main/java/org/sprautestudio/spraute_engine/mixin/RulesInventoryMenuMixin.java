package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.runtime.ExtraSlots;

@Mixin(InventoryMenu.class)
public abstract class RulesInventoryMenuMixin extends AbstractContainerMenu {
    protected RulesInventoryMenuMixin(MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void spraute$cells(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        SimpleContainer extra = ExtraSlots.of(inventory);

        for (int i = 0; i < 54; i++) {
            this.addSlot(new ExtraSlots.Cell(extra, i, owner));
        }
    }
}
