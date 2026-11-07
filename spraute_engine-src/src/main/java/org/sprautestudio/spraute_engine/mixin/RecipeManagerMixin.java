package org.sprautestudio.spraute_engine.mixin;

import com.google.gson.JsonElement;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.sprautestudio.spraute_engine.recipe.RecipeOverrides;
import org.sprautestudio.spraute_engine.recipe.RecipeReload;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeReload {
    @Unique
    private Map<ResourceLocation, JsonElement> spraute$found;

    @Shadow
    protected abstract void apply(Map<ResourceLocation, JsonElement> var1, ResourceManager var2, ProfilerFiller var3);

    @ModifyVariable(
        method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Map<ResourceLocation, JsonElement> spraute$overrides(Map<ResourceLocation, JsonElement> found) {
        this.spraute$found = found;
        return RecipeOverrides.apply(found);
    }

    @Override
    public void spraute$reapply(ResourceManager resources) {
        if (this.spraute$found != null) {
            this.apply(this.spraute$found, resources, InactiveProfiler.INSTANCE);
        }
    }
}
