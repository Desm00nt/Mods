package org.spongepowered.asm.mixin;
import java.lang.annotation.*;
@Target({ElementType.METHOD, ElementType.FIELD}) @Retention(RetentionPolicy.CLASS)
public @interface Shadow { String prefix() default "shadow$"; boolean remap() default true; String[] aliases() default {}; }
