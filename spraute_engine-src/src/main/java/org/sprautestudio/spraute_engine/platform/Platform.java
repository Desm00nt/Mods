package org.sprautestudio.spraute_engine.platform;

import com.mojang.authlib.GameProfile;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.sprautestudio.spraute_engine.runtime.models.BodyPart;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

public interface Platform {
    static Platform get() {
        return Platform.Holder.instance;
    }

    static void install(Platform platform) {
        Platform.Holder.instance = platform;
    }

    boolean isClient();

    String loader();

    boolean isModLoaded(String var1);

    Path gameDir();

    Path modResource(String... var1);

    default List<Path> addonManifests() {
        return this.modFiles("META-INF", "spraute_addon.properties");
    }

    List<Path> modFiles(String var1, String var2);

    Path modJar(String var1);

    String modVersion(String var1);

    CompoundTag persistentData(Entity var1);

    MinecraftServer server();

    PacketChannel channel(ResourceLocation var1, String var2);

    <T extends Entity> Supplier<EntityType<T>> entityType(String var1, Supplier<EntityType<T>> var2);

    Supplier<Block> block(String var1, Supplier<Block> var2);

    default <T extends Mob> void mobSpawn(
        Supplier<EntityType<T>> type, MobCategory category, int weight, int min, int max, String dimension, List<String> biomes
    ) {
    }

    <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String var1, BiFunction<BlockPos, BlockState, T> var2, Supplier<Block[]> var3);

    Supplier<Item> item(String var1, Supplier<Item> var2);

    void creativeTab(Supplier<Item> var1, String var2);

    Object ownTab(String var1, Supplier<ItemStack> var2, Supplier<List<ItemStack>> var3);

    default void oreFeature(String feature, String dimension) {
    }

    void fuel(Supplier<Item> var1, int var2);

    <T extends LivingEntity> void entityAttributes(Supplier<EntityType<T>> var1, Supplier<AttributeSupplier> var2);

    ServerPlayer fakePlayer(ServerLevel var1, GameProfile var2);

    SprauteModelEntity modelEntity(EntityType<SprauteModelEntity> var1, Level var2);

    BodyPart bodyPart(SprauteModelEntity var1, int var2);

    default Attribute blockReach() {
        return null;
    }

    default Attribute entityReach() {
        return null;
    }

    default void listenLoaderEvent(String name) {
        throw new UnsupportedOperationException("This loader has no events by name");
    }

    public static final class Holder {
        private static Platform instance;

        private Holder() {
        }
    }
}
