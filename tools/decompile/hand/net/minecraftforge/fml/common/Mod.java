package net.minecraftforge.fml.common;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Mod {
    String value();
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
    @interface EventBusSubscriber { String modid() default ""; Bus bus() default Bus.FORGE; net.minecraftforge.api.distmarker.Dist[] value() default {}; enum Bus { FORGE, MOD } }
}
