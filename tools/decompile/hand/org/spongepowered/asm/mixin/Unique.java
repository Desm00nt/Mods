package org.spongepowered.asm.mixin;
import java.lang.annotation.*;
@Target({ElementType.METHOD, ElementType.FIELD}) @Retention(RetentionPolicy.CLASS)
public @interface Unique { boolean silent() default false; }
