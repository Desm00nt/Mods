package net.minecraftforge.api.distmarker;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS)
public @interface OnlyIn { Dist value(); Class<?> _interface() default Object.class; }
