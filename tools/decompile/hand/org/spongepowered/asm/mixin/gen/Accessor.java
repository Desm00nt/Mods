package org.spongepowered.asm.mixin.gen;
import java.lang.annotation.*;
@Target(ElementType.METHOD) @Retention(RetentionPolicy.CLASS)
public @interface Accessor { String value() default ""; boolean remap() default true; }
