package org.sprautestudio.spraute_engine.mixin;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.sprautestudio.spraute_engine.gui.client.MenuRuntime;

@Mixin(Screen.class)
public abstract class MenuScreenMixin {
    @Shadow
    @Final
    private List<GuiEventListener> children;
    @Shadow
    @Final
    private List renderables;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void spraute$dim(CallbackInfo ci) {
        if (MenuRuntime.flag(this, "hideDim")) {
            ci.cancel();
        }
    }

    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("RETURN"))
    private void spraute$opened(Minecraft mc, int width, int height, CallbackInfo ci) {
        MenuRuntime.ready((Screen)(Object)this, this.children, this.renderables);
    }

    @Inject(method = "rebuildWidgets", at = @At("RETURN"))
    private void spraute$rebuilt(CallbackInfo ci) {
        MenuRuntime.ready((Screen)(Object)this, this.children, this.renderables);
    }
}
