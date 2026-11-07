package org.sprautestudio.spraute_engine.custom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.McBlocks;
import org.sprautestudio.spraute_engine.mixin.FireBlockAccess;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class CustomBlocks {
    public static final String FOLDER = "game_blocks";
    private static final Map<String, CustomBlocks.Registered> REGISTERED = new LinkedHashMap<>();
    private static Supplier<BlockEntityType<CustomStorage>> storageType;
    private static Supplier<BlockEntityType<ModelBlockEntity>> modelType;
    private static final Map<String, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    private CustomBlocks() {
    }

    public static Map<String, CustomBlocks.Registered> registered() {
        return REGISTERED;
    }

    static BlockEntityType<CustomStorage> storageType() {
        return storageType.get();
    }

    static void registerStorage() {
        List<CustomBlocks.Registered> storages = new ArrayList<>();

        for (CustomBlocks.Registered r : REGISTERED.values()) {
            if (r.def().kind.equals("storage")) {
                storages.add(r);
            }
        }

        if (!storages.isEmpty()) {
            storageType = Platform.get().blockEntity("storage", CustomStorage::new, () -> {
                Block[] blocks = new Block[storages.size()];

                for (int i = 0; i < blocks.length; i++) {
                    blocks[i] = storages.get(i).block().get();
                }

                return blocks;
            });
        }
    }

    public static BlockEntityType<ModelBlockEntity> modelType() {
        return modelType == null ? null : modelType.get();
    }

    static void registerModels() {
        List<CustomBlocks.Registered> drawn = new ArrayList<>();

        for (CustomBlocks.Registered r : REGISTERED.values()) {
            if (r.def().engineModel()) {
                drawn.add(r);
            }
        }

        if (!drawn.isEmpty()) {
            modelType = Platform.get().blockEntity("model", ModelBlockEntity::new, () -> {
                Block[] blocks = new Block[drawn.size()];

                for (int i = 0; i < blocks.length; i++) {
                    blocks[i] = drawn.get(i).block().get();
                }

                return blocks;
            });
        }
    }

    public static void registerHooks() {
        Hooks.COMMON_SETUP.add(e -> {
            for (CustomBlocks.Registered r : REGISTERED.values()) {
                if (r.def().flammability > 0 || r.def().fireSpread > 0) {
                    ((FireBlockAccess)Blocks.FIRE).spraute$setFlammable(r.block().get(), r.def().flammability, r.def().fireSpread);
                }
            }
        });
    }

    static void registerProject(Path project) throws IOException {
        for (BlockDef d : read(project)) {
            String problem = d.problem();
            if (problem != null) {
                SprauteEngine.LOGGER.error("Spraute: the block {} of {} is not added: {}", d.id, project.getFileName(), problem);
            } else if (REGISTERED.containsKey(d.id)) {
                SprauteEngine.LOGGER
                    .error(
                        "Spraute: the block {} is in two projects, {} and {}: the first is used",
                        d.id,
                        REGISTERED.get(d.id).project().getFileName(),
                        project.getFileName()
                    );
            } else {
                Supplier<Block> block = Platform.get().block(d.id, () -> make(d));
                Supplier<Item> item = Platform.get().item(d.id, () -> new CustomBlockKinds.Placed(block.get(), d, itemProperties(d)));
                REGISTERED.put(d.id, new CustomBlocks.Registered(d, project, d.restartKey(), block, item));
                if (!d.tab.startsWith("custom:") && !d.tab.equals("none")) {
                    Platform.get().creativeTab(item, d.tab);
                }

                if (d.burnTicks > 0) {
                    Platform.get().fuel(item, d.burnTicks);
                }

                if (d.generates()) {
                    Platform.get().oreFeature(BlockAssets.oreFeature(d), d.oreDimension);
                }
            }
        }
    }

    public static List<BlockDef> read(Path project) throws IOException {
        List<BlockDef> out = new ArrayList<>();
        return out;
    }

    static BlockDef current(CustomBlocks.Registered r) {
        Path f = r.project().resolve("game_blocks").resolve(r.def().id + ".json");

        try {
            if (Files.isRegularFile(f)) {
                BlockDef d = BlockDef.fromJson(Files.readString(f, StandardCharsets.UTF_8));
                d.id = r.def().id;
                d.kind = r.def().kind;
                d.oreFrom(r.def());
                return d;
            }
        } catch (RuntimeException | IOException var3) {
            SprauteEngine.LOGGER.warn("Spraute: cannot read the block {}", f, var3);
        }

        return r.def();
    }

    public static void showChanges(BlockDef saved) {
        CustomBlocks.Registered r = REGISTERED.get(saved.id);
        if (r != null) {
            r.def().name = saved.name;
            r.def().lore = new ArrayList<>(saved.lore);
            r.def().glint = saved.glint;
        }
    }

    private static Properties itemProperties(BlockDef d) {
        Properties var10000 = new Properties().stacksTo(d.maxStack);
        String var2 = d.rarity;

        Properties p = var10000.rarity(switch (var2) {
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "epic" -> Rarity.EPIC;
            default -> Rarity.COMMON;
        });
        if (d.fireResistant) {
            p.fireResistant();
        }

        CustomItems.tabOf(p, d.tab);
        return p;
    }

    private static SoundType sound(String name) {
        return switch (name) {
            case "wood" -> SoundType.WOOD;
            case "metal" -> SoundType.METAL;
            case "grass" -> SoundType.GRASS;
            case "gravel" -> SoundType.GRAVEL;
            case "sand" -> SoundType.SAND;
            case "glass" -> SoundType.GLASS;
            case "wool" -> SoundType.WOOL;
            case "snow" -> SoundType.SNOW;
            case "bone" -> SoundType.BONE_BLOCK;
            case "amethyst" -> SoundType.AMETHYST;
            case "deepslate" -> SoundType.DEEPSLATE;
            case "netherrack" -> SoundType.NETHERRACK;
            case "honey" -> SoundType.HONEY_BLOCK;
            case "slime" -> SoundType.SLIME_BLOCK;
            default -> SoundType.STONE;
        };
    }

    private static Block make(BlockDef d) {
        net.minecraft.world.level.block.state.BlockBehaviour.Properties p = McBlocks.properties(d.material).sound(sound(d.sound));
        if (d.unbreakable) {
            p.strength(-1.0F, 3600000.0F);
        } else {
            p.strength(d.hardness, d.resistance);
        }

        if (d.requiresTool && !d.tool.equals("none")) {
            p.requiresCorrectToolForDrops();
        }

        if (d.light > 0) {
            int light = d.light;
            p.lightLevel(state -> light);
        }

        p.friction(d.friction).speedFactor(d.speedFactor).jumpFactor(d.jumpFactor);
        if (d.noCollision) {
            p.noCollission();
        }

        if (d.transparent() || d.shaped() || d.hasModel()) {
            p.noOcclusion();
        }

        if (!d.randomCommand.isBlank()) {
            p.randomTicks();
        }

        String var4 = d.kind;

        return (Block)(switch (var4) {
            case "falling" -> new CustomBlockKinds.Falling(d, p);
            case "rotating" -> d.engineModel() ? new CustomBlockKinds.ModelRotating(d, p) : new CustomBlockKinds.Rotating(d, p);
            case "storage" -> new CustomBlockKinds.Storage(d, p);
            case "pillar" -> new CustomBlockKinds.Pillar(d, p);
            case "slab" -> new CustomBlockKinds.Slab(d, p);
            case "stairs" -> new CustomBlockKinds.Stairs(d, p);
            case "wall" -> new CustomBlockKinds.Wall(d, p);
            case "fence" -> new CustomBlockKinds.Fence(d, p);
            case "fence_gate" -> new CustomBlockKinds.Gate(d, p);
            case "trapdoor" -> new CustomBlockKinds.Trapdoor(d, p);
            case "button" -> new CustomBlockKinds.Button(d, p);
            case "pressure_plate" -> new CustomBlockKinds.Plate(d, p);
            case "carpet" -> new CustomBlockKinds.Carpet(d, p);
            case "pane" -> new CustomBlockKinds.Pane(d, p);
            case "plant" -> new CustomBlockKinds.Plant(d, p);
            default -> d.engineModel() ? new CustomBlockKinds.ModelPlain(d, p) : new CustomBlockKinds.Plain(d, p);
        });
    }

    static VoxelShape box(BlockDef d, BlockState state) {
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        return SHAPES.computeIfAbsent(d.id + "/" + facing.getName(), k -> {
            VoxelShape all = Shapes.empty();

            for (BlockDef.Box b : d.boxes) {
                all = Shapes.or(all, turned(b, facing));
            }

            return all;
        });
    }

    private static VoxelShape turned(BlockDef.Box b, Direction facing) {
        return switch (facing) {
            case EAST -> Block.box(16 - b.z2, b.y1, b.x1, 16 - b.z1, b.y2, b.x2);
            case SOUTH -> Block.box(16 - b.x2, b.y1, 16 - b.z2, 16 - b.x1, b.y2, 16 - b.z1);
            case WEST -> Block.box(b.z1, b.y1, 16 - b.x2, b.z2, b.y2, 16 - b.x1);
            default -> Block.box(b.x1, b.y1, b.z1, b.x2, b.y2, b.z2);
        };
    }

    private static boolean clicks(BlockDef d) {
        return !d.clickCommand.isBlank() || !d.clickSound.isEmpty() || !d.clickEffects.isEmpty();
    }

    static InteractionResult use(BlockDef d, InteractionResult own, Level level, BlockPos pos, Player player) {
        if (!clicks(d)) {
            return own;
        } else if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            CustomItems.applyAll(d.clickEffects, player);
            SoundEvent sound = CustomItems.soundOf(d.clickSound);
            if (sound != null) {
                level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
            }

            byPlayer(player, d.clickCommand);
            return InteractionResult.CONSUME;
        }
    }

    static void step(BlockDef d, Level level, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living) {
            if (entity.tickCount % 20 == 0) {
                if (d.stepDamage > 0.0F) {
                    living.hurt(Mc.generic(level), d.stepDamage);
                }

                CustomItems.applyAll(d.stepEffects, living);
                if (entity instanceof Player player) {
                    byPlayer(player, d.stepCommand);
                }
            }
        }
    }

    static void broken(BlockDef d, Level level, Player player) {
        if (!level.isClientSide) {
            byPlayer(player, d.breakCommand);
        }
    }

    static void randomTick(BlockDef d, ServerLevel level, BlockPos pos) {
        if (!d.randomCommand.isBlank()) {
            MinecraftServer server = level.getServer();
            server.getCommands()
                .performPrefixedCommand(
                    server.createCommandSourceStack().withLevel(level).withPosition(Vec3.atCenterOf(pos)).withSuppressedOutput(), d.randomCommand
                );
        }
    }

    private static void byPlayer(Player player, String command) {
        if (!command.isBlank()) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(2).withSuppressedOutput(), command);
            }
        }
    }

    static int xp(BlockDef d, RandomSource random) {
        return d.xpMax <= 0 ? 0 : d.xpMin + random.nextInt(d.xpMax - d.xpMin + 1);
    }

    public record Registered(BlockDef def, Path project, String restartKey, Supplier<Block> block, Supplier<Item> item) {
    }
}
