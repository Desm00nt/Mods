package org.sprautestudio.spraute_engine.custom;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.build.BuildLayout;
import org.sprautestudio.spraute_engine.build.StructureDef;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.platform.Hooks;

public final class Dungeons {
    public static final String FOLDER = "dungeons";

    private Dungeons() {
    }

    public static void registerHooks() {
        Hooks.REGISTER_COMMANDS.add(e -> registerCommand(e.getDispatcher()));
    }

    public static List<DungeonDef> read(Path project) throws IOException {
        List<DungeonDef> out = new ArrayList<>();
        return out;
    }

    private static List<Dungeons.Loaded> all() {
        List<Dungeons.Loaded> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Path root = CustomItems.projectsDir();
        if (!Files.isDirectory(root)) {
            return out;
        } else {
            try (Stream<Path> projects = Files.list(root)) {
                for (Path project : (Iterable<Path>) projects.filter(x$0 -> Files.isDirectory(x$0)).sorted()::iterator) {
                    for (DungeonDef d : read(project)) {
                        if (d.problem() == null) {
                            if (seen.add(d.id)) {
                                out.add(new Dungeons.Loaded(d, project));
                            } else {
                                SprauteEngine.LOGGER.error("Spraute: the dungeon {} is in two projects: the first is used", d.id);
                            }
                        }
                    }
                }
            } catch (IOException var10) {
                SprauteEngine.LOGGER.error("Spraute: cannot list the projects for their dungeons", (Throwable)var10);
            }

            return out;
        }
    }

    private static StructureDef building(Dungeons.Loaded d) {
        Path file = d.project().resolve("structures").resolve(d.def().structure + Project.Kind.STRUCTURE.ext);

        try {
            return Files.isRegularFile(file) ? StructureDef.fromJson(Files.readString(file, StandardCharsets.UTF_8)) : null;
        } catch (RuntimeException | IOException var3) {
            SprauteEngine.LOGGER.warn("Spraute: cannot read the building of the dungeon {}", d.def().id, var3);
            return null;
        }
    }

    static boolean any() {
        for (Dungeons.Loaded d : all()) {
            if (d.def().enabled) {
                return true;
            }
        }

        return false;
    }

    static void writeData(Path root) throws IOException {
        for (Entry<String, String> file : DungeonDef.landTag().entrySet()) {
            Path to = root.resolve(file.getKey());
            Files.createDirectories(to.getParent());
            Files.writeString(to, file.getValue(), StandardCharsets.UTF_8);
        }

        for (Dungeons.Loaded d : all()) {
            if (d.def().enabled) {
                StructureDef building = building(d);
                if (building == null) {
                    SprauteEngine.LOGGER.error("Spraute: the dungeon {} has no building {}: it is left out", d.def().id, d.def().structure);
                } else {
                    for (Entry<String, String> file : d.def().worldgen().entrySet()) {
                        Path to = root.resolve(file.getKey());
                        Files.createDirectories(to.getParent());
                        Files.writeString(to, file.getValue(), StandardCharsets.UTF_8);
                    }

                    Path template = root.resolve(d.def().templateFile());
                    Files.createDirectories(template.getParent());

                    try (OutputStream out = Files.newOutputStream(template)) {
                        NbtIo.writeCompressed(template(d.def(), building), out);
                    }
                }
            }
        }
    }

    private static ListTag ints(int... values) {
        ListTag list = new ListTag();

        for (int v : values) {
            list.add(IntTag.valueOf(v));
        }

        return list;
    }

    private static CompoundTag stateTag(String text) {
        CompoundTag tag = new CompoundTag();
        int open = text.indexOf(91);
        tag.putString("Name", open < 0 ? text : text.substring(0, open));
        if (open >= 0 && text.endsWith("]")) {
            CompoundTag properties = new CompoundTag();

            for (String pair : text.substring(open + 1, text.length() - 1).split(",")) {
                int eq = pair.indexOf(61);
                if (eq > 0) {
                    properties.putString(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
                }
            }

            if (!properties.isEmpty()) {
                tag.put("Properties", properties);
            }
        }

        return tag;
    }

    private static CompoundTag template(DungeonDef d, StructureDef s) {
        int[] grid = s.grid();
        StructureDef.Action[] actions = s.actions();
        Map<Integer, DungeonDef.Chest> chests = new HashMap<>();

        for (DungeonDef.Chest c : DungeonDef.chests(s, Dungeons::isStorage)) {
            chests.put(c.cell(), c);
        }

        ListTag palette = new ListTag();
        ListTag blocks = new ListTag();
        Map<String, Integer> known = new HashMap<>();

        for (int y = 0; y < s.sizeY; y++) {
            for (int z = 0; z < s.sizeZ; z++) {
                for (int x = 0; x < s.sizeX; x++) {
                    int cell = s.index(x, y, z);
                    if (actions[cell] != StructureDef.Action.SKIP) {
                        String text = actions[cell] == StructureDef.Action.CLEAR ? "minecraft:air" : s.finalBlock(s.palette.get(grid[cell]));
                        Integer state = known.get(text);
                        if (state == null) {
                            state = palette.size();
                            known.put(text, state);
                            palette.add(stateTag(text));
                        }

                        CompoundTag block = new CompoundTag();
                        block.put("pos", ints(x, y, z));
                        block.putInt("state", state);
                        CompoundTag nbt = cellNbt(d, s, cell, chests.get(cell));
                        if (nbt != null) {
                            block.put("nbt", nbt);
                        }

                        blocks.add(block);
                    }
                }
            }
        }

        CompoundTag root = new CompoundTag();
        root.put("size", ints(s.sizeX, s.sizeY, s.sizeZ));
        root.put("palette", palette);
        root.put("blocks", blocks);
        root.put("entities", new ListTag());
        root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        return root;
    }

    private static CompoundTag parsed(StructureDef s, int cell) {
        String text = s.entities.get(String.valueOf(cell));
        if (text == null) {
            return null;
        } else {
            try {
                return TagParser.parseTag(text);
            } catch (CommandSyntaxException var4) {
                return null;
            }
        }
    }

    private static CompoundTag cellNbt(DungeonDef d, StructureDef s, int cell, DungeonDef.Chest chest) {
        CompoundTag nbt = parsed(s, cell);
        if (chest == null) {
            return nbt;
        } else {
            String loot = d.lootOf(cell);
            if (loot.isBlank()) {
                return nbt;
            } else {
                if (nbt == null) {
                    nbt = new CompoundTag();
                }

                nbt.remove("Items");
                nbt.putString("LootTable", LootDef.ref(loot.trim()));
                if (isStorage(chest.block())) {
                    nbt.putBoolean("SprauteLootDone", true);
                }

                return nbt;
            }
        }
    }

    private static boolean isStorage(String block) {
        String prefix = "spraute_engine:";
        if (!block.startsWith(prefix)) {
            return false;
        } else {
            CustomBlocks.Registered r = CustomBlocks.registered().get(block.substring(prefix.length()));
            return r != null && r.def().kind.equals("storage");
        }
    }

    public static String placeNow(String id, ServerLevel level, BlockPos at) {
        Dungeons.Loaded d = null;

        for (Dungeons.Loaded l : all()) {
            if (l.def().id.equals(id)) {
                d = l;
            }
        }

        if (d == null) {
            return "Нет данжа «" + id + "»";
        } else {
            StructureDef building = building(d);
            if (building == null) {
                return "У данжа «" + id + "» нет постройки «" + d.def().structure + "»";
            } else {
                Random random = new Random();
                int turns = random.nextInt(4);
                Map<BlockPos, Integer> chests = new HashMap<>();

                for (DungeonDef.Chest c : DungeonDef.chests(building, Dungeons::isStorage)) {
                    int dx = 2 * c.x() - (building.sizeX - 1);
                    int dz = 2 * c.z() - (building.sizeZ - 1);

                    for (int t = 0; t < turns; t++) {
                        int next = -dz;
                        dz = dx;
                        dx = next;
                    }

                    chests.put(new BlockPos(at.getX() + Math.floorDiv(dx, 2), at.getY() + c.y(), at.getZ() + Math.floorDiv(dz, 2)), c.cell());
                }

                for (BuildLayout.Cell cell : BuildLayout.of(building, "up", at, turns * 90, "", 0.0F, level, random.nextLong())) {
                    BlockPos pos = cell.pos();
                    level.setBlock(pos, cell.state(), 18);
                    Integer chest = chests.get(pos);
                    BlockEntity be = cell.nbt() == null && chest == null ? null : level.getBlockEntity(pos);
                    if (be != null) {
                        if (cell.nbt() != null) {
                            CompoundTag tag = cell.nbt().copy();
                            tag.putInt("x", pos.getX());
                            tag.putInt("y", pos.getY());
                            tag.putInt("z", pos.getZ());
                            be.load(tag);
                        }

                        if (chest != null) {
                            giveLoot(d.def(), be, chest, random);
                        }

                        be.setChanged();
                    }
                }

                return null;
            }
        }
    }

    private static void giveLoot(DungeonDef d, BlockEntity be, int cell, Random random) {
        String loot = d.lootOf(cell);
        if (!loot.isBlank() && be instanceof RandomizableContainerBlockEntity box) {
            ResourceLocation table = ResourceLocation.tryParse(LootDef.ref(loot.trim()));
            if (table != null) {
                box.setLootTable(table, random.nextLong());
                if (be instanceof CustomStorage storage) {
                    storage.noLoot();
                }
            }
        }
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("dungeon")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("place")
                        .then(
                            Commands.argument("id", StringArgumentType.word())
                                .executes(
                                    c -> {
                                        CommandSourceStack source = c.getSource();
                                        String problem = placeNow(
                                            StringArgumentType.getString(c, "id"),
                                            source.getLevel(),
                                            new BlockPos(
                                                (int)Math.floor(source.getPosition().x),
                                                (int)Math.floor(source.getPosition().y),
                                                (int)Math.floor(source.getPosition().z)
                                            )
                                        );
                                        if (problem != null) {
                                            source.sendFailure(Component.literal(problem));
                                            return 0;
                                        } else {
                                            Mc.success(source, Component.literal("Данж поставлен"), false);
                                            return 1;
                                        }
                                    }
                                )
                        )
                )
        );
    }

    private record Loaded(DungeonDef def, Path project) {
    }
}
