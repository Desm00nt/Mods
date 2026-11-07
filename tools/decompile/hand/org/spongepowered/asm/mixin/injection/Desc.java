package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
public @interface Desc { String value(); }
