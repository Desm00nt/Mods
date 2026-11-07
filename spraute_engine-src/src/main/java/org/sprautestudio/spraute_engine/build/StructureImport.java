package org.sprautestudio.spraute_engine.build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class StructureImport {
    private static final String NOTHING = "minecraft:structure_void";

    private StructureImport() {
    }

    public static boolean canRead(String fileName) {
        String n = fileName.toLowerCase(Locale.ROOT);
        return n.endsWith(".nbt") || n.endsWith(".schem") || n.endsWith(".litematic");
    }

    public static StructureDef read(Path file) throws IOException {
        CompoundTag root;
        try (InputStream in = Files.newInputStream(file)) {
            root = NbtIo.readCompressed(in);
        }

        String n = file.getFileName().toString().toLowerCase(Locale.ROOT);
        StructureDef d = n.endsWith(".litematic") ? litematic(root) : (n.endsWith(".schem") ? schem(root) : template(root));
        return d.trimmed();
    }

    private static String blockName(CompoundTag entry) {
        String name = entry.getString("Name");
        if (!entry.contains("Properties", 10)) {
            return name;
        } else {
            CompoundTag p = entry.getCompound("Properties");
            Map<String, String> sorted = new TreeMap<>();

            for (String k : p.getAllKeys()) {
                sorted.put(k, p.getString(k));
            }

            if (sorted.isEmpty()) {
                return name;
            } else {
                StringBuilder sb = new StringBuilder(name).append('[');
                sorted.forEach((kx, v) -> sb.append(kx).append('=').append(v).append(','));
                sb.setCharAt(sb.length() - 1, ']');
                return sb.toString();
            }
        }
    }

    public static StructureDef template(CompoundTag root) {
        StructureDef d = new StructureDef();
        ListTag size = root.getList("size", 3);
        d.sizeX = size.getInt(0);
        d.sizeY = size.getInt(1);
        d.sizeZ = size.getInt(2);
        ListTag palette = root.contains("palette", 9) ? root.getList("palette", 10) : root.getList("palettes", 9).getList(0);
        d.palette = new ArrayList<>();
        d.palette.add("minecraft:structure_void");

        for (int i = 0; i < palette.size(); i++) {
            d.palette.add(blockName(palette.getCompound(i)));
        }

        int[] g = new int[d.sizeX * d.sizeY * d.sizeZ];
        ListTag blocks = root.getList("blocks", 10);

        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag b = blocks.getCompound(i);
            ListTag pos = b.getList("pos", 3);
            int at = d.index(pos.getInt(0), pos.getInt(1), pos.getInt(2));
            if (at >= 0 && at < g.length) {
                g[at] = b.getInt("state") + 1;
                if (b.contains("nbt", 10)) {
                    d.entities.put(String.valueOf(at), b.getCompound("nbt").toString());
                }
            }
        }

        d.setGrid(g);
        return d;
    }

    private static StructureDef schem(CompoundTag file) {
        CompoundTag root = file.contains("Schematic", 10) ? file.getCompound("Schematic") : file;
        CompoundTag holder = root.contains("Blocks", 10) ? root.getCompound("Blocks") : root;
        StructureDef d = new StructureDef();
        d.sizeX = root.getShort("Width") & '\uffff';
        d.sizeY = root.getShort("Height") & '\uffff';
        d.sizeZ = root.getShort("Length") & '\uffff';
        CompoundTag palette = holder.getCompound("Palette");
        String[] names = new String[palette.size()];

        for (String name : palette.getAllKeys()) {
            int i = palette.getInt(name);
            if (i >= 0 && i < names.length) {
                names[i] = name;
            }
        }

        d.palette = new ArrayList<>(List.of(names));
        byte[] data = holder.contains("BlockData") ? holder.getByteArray("BlockData") : holder.getByteArray("Data");
        d.setGrid(StructureDef.unpackVarints(data, d.sizeX * d.sizeY * d.sizeZ));
        String listKey = holder.contains("BlockEntities", 9) ? "BlockEntities" : "TileEntities";
        ListTag list = holder.getList(listKey, 10);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag be = list.getCompound(i);
            int[] pos = be.getIntArray("Pos");
            if (pos.length == 3) {
                CompoundTag tag = be.contains("Data", 10) ? be.getCompound("Data") : be.copy();
                tag.remove("Pos");
                tag.remove("Id");
                d.entities.put(String.valueOf(d.index(pos[0], pos[1], pos[2])), tag.toString());
            }
        }

        return d;
    }

    private static StructureDef litematic(CompoundTag root) {
        CompoundTag regions = root.getCompound("Regions");
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (String name : regions.getAllKeys()) {
            CompoundTag r = regions.getCompound(name);
            int[] c = corner(r);
            int[] s = absSize(r);
            minX = Math.min(minX, c[0]);
            minY = Math.min(minY, c[1]);
            minZ = Math.min(minZ, c[2]);
            maxX = Math.max(maxX, c[0] + s[0]);
            maxY = Math.max(maxY, c[1] + s[1]);
            maxZ = Math.max(maxZ, c[2] + s[2]);
        }

        StructureDef d = new StructureDef();
        if (minX == Integer.MAX_VALUE) {
            return d;
        } else {
            d.sizeX = maxX - minX;
            d.sizeY = maxY - minY;
            d.sizeZ = maxZ - minZ;
            d.palette = new ArrayList<>();
            d.palette.add("minecraft:structure_void");
            Map<String, Integer> numbers = new HashMap<>();
            numbers.put("minecraft:structure_void", 0);
            int[] g = new int[d.sizeX * d.sizeY * d.sizeZ];

            for (String name : regions.getAllKeys()) {
                CompoundTag r = regions.getCompound(name);
                int[] c = corner(r);
                int[] s = absSize(r);
                ListTag palette = r.getList("BlockStatePalette", 10);
                int[] local = new int[palette.size()];

                for (int i = 0; i < palette.size(); i++) {
                    String block = blockName(palette.getCompound(i));
                    Integer known = numbers.get(block);
                    if (known == null) {
                        known = d.palette.size();
                        d.palette.add(block);
                        numbers.put(block, known);
                    }

                    local[i] = known;
                }

                int[] values = StructureDef.unpackBits(r.getLongArray("BlockStates"), StructureDef.bitsFor(palette.size()), s[0] * s[1] * s[2]);

                for (int y = 0; y < s[1]; y++) {
                    for (int z = 0; z < s[2]; z++) {
                        for (int x = 0; x < s[0]; x++) {
                            int v = values[y * s[0] * s[2] + z * s[0] + x];
                            if (v < local.length) {
                                g[d.index(c[0] - minX + x, c[1] - minY + y, c[2] - minZ + z)] = local[v];
                            }
                        }
                    }
                }

                ListTag list = r.getList("TileEntities", 10);

                for (int i = 0; i < list.size(); i++) {
                    CompoundTag be = list.getCompound(i).copy();
                    int at = d.index(c[0] - minX + be.getInt("x"), c[1] - minY + be.getInt("y"), c[2] - minZ + be.getInt("z"));
                    be.remove("x");
                    be.remove("y");
                    be.remove("z");
                    d.entities.put(String.valueOf(at), be.toString());
                }
            }

            d.setGrid(g);
            return d;
        }
    }

    private static int[] absSize(CompoundTag region) {
        CompoundTag s = region.getCompound("Size");
        return new int[]{Math.abs(s.getInt("x")), Math.abs(s.getInt("y")), Math.abs(s.getInt("z"))};
    }

    private static int[] corner(CompoundTag region) {
        CompoundTag p = region.getCompound("Position");
        CompoundTag s = region.getCompound("Size");
        return new int[]{
            p.getInt("x") + (s.getInt("x") < 0 ? s.getInt("x") + 1 : 0),
            p.getInt("y") + (s.getInt("y") < 0 ? s.getInt("y") + 1 : 0),
            p.getInt("z") + (s.getInt("z") < 0 ? s.getInt("z") + 1 : 0)
        };
    }

    public static StructureDef capture(Level level, BlockPos a, BlockPos b) {
        int x0 = Math.min(a.getX(), b.getX());
        int y0 = Math.min(a.getY(), b.getY());
        int z0 = Math.min(a.getZ(), b.getZ());
        StructureDef d = new StructureDef();
        d.sizeX = Math.abs(a.getX() - b.getX()) + 1;
        d.sizeY = Math.abs(a.getY() - b.getY()) + 1;
        d.sizeZ = Math.abs(a.getZ() - b.getZ()) + 1;
        d.palette = new ArrayList<>();
        d.palette.add("minecraft:air");
        Map<String, Integer> numbers = new HashMap<>();
        numbers.put("minecraft:air", 0);
        int[] g = new int[d.sizeX * d.sizeY * d.sizeZ];

        for (int y = 0; y < d.sizeY; y++) {
            for (int z = 0; z < d.sizeZ; z++) {
                for (int x = 0; x < d.sizeX; x++) {
                    BlockPos pos = new BlockPos(x0 + x, y0 + y, z0 + z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isAir()) {
                        String text = BlockText.of(state);
                        Integer n = numbers.get(text);
                        if (n == null) {
                            n = d.palette.size();
                            d.palette.add(text);
                            numbers.put(text, n);
                        }

                        int at = d.index(x, y, z);
                        g[at] = n;
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be != null) {
                            d.entities.put(String.valueOf(at), be.saveWithoutMetadata().toString());
                        }
                    }
                }
            }
        }

        d.setGrid(g);
        return d.trimmed();
    }
}
