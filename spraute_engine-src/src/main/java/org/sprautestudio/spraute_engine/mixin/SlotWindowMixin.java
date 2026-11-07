package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.core.client.SlotOverlay;

@Mixin(AbstractContainerScreen.class)
public abstract class SlotWindowMixin {
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    @Final
    protected AbstractContainerMenu menu;

    @Inject(method = "render", at = @At("HEAD"))
    private void spraute$before(GuiGraphics g, int mx, int my, float partial, CallbackInfo ci) {
        SlotOverlay.before(Gfx.of(g), (Screen)(Object)this, this.leftPos, this.topPos);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void spraute$after(GuiGraphics g, int mx, int my, float partial, CallbackInfo ci) {
        SlotOverlay.after(Gfx.of(g), this.menu, this.leftPos, this.topPos);
    }
}
