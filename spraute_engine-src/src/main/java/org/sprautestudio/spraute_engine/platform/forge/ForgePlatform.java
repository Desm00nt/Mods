package org.sprautestudio.spraute_engine.platform.forge;

import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.sprautestudio.spraute_engine.platform.PacketChannel;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.models.BodyPart;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;

final class ForgePlatform implements Platform {
    private final DeferredRegister<EntityType<?>> entities = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "spraute_engine");
    private final DeferredRegister<Block> blocks = DeferredRegister.create(ForgeRegistries.BLOCKS, "spraute_engine");
    private final DeferredRegister<BlockEntityType<?>> blockEntities = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "spraute_engine");
    private final DeferredRegister<Item> items = DeferredRegister.create(ForgeRegistries.ITEMS, "spraute_engine");
    private final List<Consumer<EntityAttributeCreationEvent>> attributes = new ArrayList<>();

    void registerOn(IEventBus modBus) {
        this.entities.register(modBus);
        this.blocks.register(modBus);
        this.blockEntities.register(modBus);
        this.items.register(modBus);
        ForgeVersion.registerTabs(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> this.attributes.forEach(a -> a.accept(e)));
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public String loader() {
        return "forge";
    }

    @Override
    public boolean isModLoaded(String id) {
        return ModList.get().isLoaded(id);
    }

    @Override
    public Attribute blockReach() {
        return ForgeVersion.blockReach();
    }

    @Override
    public Attribute entityReach() {
        return ForgeVersion.entityReach();
    }

    @Override
    public Path gameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path modResource(String... path) {
        return ModList.get().getModFileById("spraute_engine").getFile().findResource(path);
    }

    @Override
    public List<Path> modFiles(String folder, String name) {
        List<Path> out = new ArrayList<>();

        for (IModFileInfo file : ModList.get().getModFiles()) {
            Path found = file.getFile().findResource(new String[]{folder, name});
            if (Files.exists(found)) {
                out.add(found);
            }
        }

        return out;
    }

    @Override
    public Path modJar(String id) {
        IModFileInfo file = ModList.get().getModFileById(id);
        if (file == null) {
            return null;
        } else {
            Path path = file.getFile().getFilePath();
            return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar") ? path : null;
        }
    }

    @Override
    public String modVersion(String id) {
        return ModList.get().getModContainerById(id).map(c -> c.getModInfo().getVersion().toString()).orElse("0");
    }

    @Override
    public CompoundTag persistentData(Entity entity) {
        return entity.getPersistentData();
    }

    @Override
    public MinecraftServer server() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    @Override
    public PacketChannel channel(ResourceLocation id, String protocol) {
        return new ForgeChannel(id, protocol);
    }

    @Override
    public <T extends Entity> Supplier<EntityType<T>> entityType(String name, Supplier<EntityType<T>> type) {
        return this.entities.register(name, type);
    }

    @Override
    public Supplier<Block> block(String name, Supplier<Block> make) {
        return this.blocks.register(name, make);
    }

    @Override
    public <T extends BlockEntity> Supplier<BlockEntityType<T>> blockEntity(String name, BiFunction<BlockPos, BlockState, T> make, Supplier<Block[]> blocks) {
        return this.blockEntities.register(name, () -> Builder.of(make::apply, blocks.get()).build(null));
    }

    @Override
    public Supplier<Item> item(String name, Supplier<Item> make) {
        return this.items.register(name, make);
    }

    @Override
    public void creativeTab(Supplier<Item> item, String key) {
        ForgeVersion.creativeTab(item, key);
    }

    @Override
    public Object ownTab(String id, Supplier<ItemStack> icon, Supplier<List<ItemStack>> items) {
        return ForgeVersion.ownTab(id, icon, items);
    }

    @Override
    public void fuel(Supplier<Item> item, int ticks) {
        MinecraftForge.EVENT_BUS.addListener((FurnaceFuelBurnTimeEvent e) -> {
            if (e.getItemStack().getItem() == item.get()) {
                e.setBurnTime(ticks);
            }
        });
    }

    @Override
    public <T extends LivingEntity> void entityAttributes(Supplier<EntityType<T>> type, Supplier<AttributeSupplier> made) {
        this.attributes.add(e -> e.put(type.get(), made.get()));
    }

    @Override
    public ServerPlayer fakePlayer(ServerLevel level, GameProfile profile) {
        return FakePlayerFactory.get(level, profile);
    }

    @Override
    public SprauteModelEntity modelEntity(EntityType<SprauteModelEntity> type, Level level) {
        return new ForgeModelEntity(type, level);
    }

    @Override
    public BodyPart bodyPart(SprauteModelEntity body, int index) {
        return new ForgePart(body, index);
    }

    @Override
    public void listenLoaderEvent(String name) {
        ForgeBridge.listen(name);
    }
}
