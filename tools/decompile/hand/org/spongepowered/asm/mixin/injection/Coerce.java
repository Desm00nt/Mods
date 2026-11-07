package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target(ElementType.PARAMETER) @Retention(RetentionPolicy.CLASS)
public @interface Coerce { }
