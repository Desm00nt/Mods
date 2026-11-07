package org.sprautestudio.spraute_engine.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractWidget.class)
public interface WidgetSizeMixin {
    @Accessor("height")
    void spraute$setHeight(int var1);
}
