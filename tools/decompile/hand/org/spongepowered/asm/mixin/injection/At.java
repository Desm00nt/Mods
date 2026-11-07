package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Target({}) @Retention(RetentionPolicy.RUNTIME)
public @interface At {
    enum Shift { NONE, BEFORE, AFTER, BY }
    String id() default ""; String value(); String slice() default ""; Shift shift() default Shift.NONE; int by() default 0;
    String[] args() default {}; String target() default ""; Desc desc() default @Desc(""); int ordinal() default -1; int opcode() default -1; boolean remap() default true;
}
