package org.sprautestudio.spraute_engine.build;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class StructureDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Set<String> AIR = Set.of("minecraft:air", "minecraft:cave_air", "minecraft:void_air");
    private static final String VOID = "minecraft:structure_void";
    public static final String AIR_AUTO = "auto";
    public static final String AIR_KEEP = "keep";
    public static final String AIR_CLEAR = "clear";
    public int sizeX = 1;
    public int sizeY = 1;
    public int sizeZ = 1;
    public List<String> palette = new ArrayList<>();
    public String cells = "";
    public Map<String, String> entities = new LinkedHashMap<>();
    public String air = "auto";
    public Map<String, String> replace = new LinkedHashMap<>();
    public String foundation = "";
    public int foundationDepth = 16;
    private transient int[] decoded;

    public int index(int x, int y, int z) {
        return x + z * this.sizeX + y * this.sizeX * this.sizeZ;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static StructureDef fromJson(String json) {
        StructureDef d = null;

        try {
            d = GSON.fromJson(json, StructureDef.class);
        } catch (RuntimeException var3) {
        }

        if (d == null) {
            d = new StructureDef();
        }

        d.fix();
        return d;
    }

    private void fix() {
        this.sizeX = Math.max(1, this.sizeX);
        this.sizeY = Math.max(1, this.sizeY);
        this.sizeZ = Math.max(1, this.sizeZ);
        if (this.palette == null) {
            this.palette = new ArrayList<>();
        }

        if (this.cells == null) {
            this.cells = "";
        }

        if (this.entities == null) {
            this.entities = new LinkedHashMap<>();
        }

        if (this.replace == null) {
            this.replace = new LinkedHashMap<>();
        }

        if (this.foundation == null) {
            this.foundation = "";
        }

        if (this.air == null || !this.air.equals("keep") && !this.air.equals("clear")) {
            this.air = "auto";
        }

        this.foundationDepth = Math.max(0, Math.min(128, this.foundationDepth));
    }

    public static String problems(String json) {
        StructureDef d;
        try {
            d = GSON.fromJson(json, StructureDef.class);
        } catch (RuntimeException var7) {
            return "Не читается: " + var7.getMessage();
        }

        if (d == null) {
            return "Пусто";
        } else {
            d.fix();

            try {
                int n = d.grid().length;
                if (n != d.sizeX * d.sizeY * d.sizeZ) {
                    return "Клеток " + n + ", а размер даёт " + d.sizeX * d.sizeY * d.sizeZ;
                } else {
                    for (int c : d.grid()) {
                        if (c < 0 || c >= d.palette.size()) {
                            return "Номер блока " + c + " вне палитры";
                        }
                    }

                    return "";
                }
            } catch (RuntimeException var8) {
                return "Клетки не читаются: " + var8.getMessage();
            }
        }
    }

    public void setGrid(int[] grid) {
        this.decoded = grid;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (GZIPOutputStream gz = new GZIPOutputStream(bytes)) {
            for (int v : grid) {
                int left;
                for (left = v; (left & -128) != 0; left >>>= 7) {
                    gz.write(left & 127 | 128);
                }

                gz.write(left);
            }
        } catch (IOException var11) {
            throw new IllegalStateException(var11);
        }

        this.cells = Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    public int[] grid() {
        if (this.decoded != null) {
            return this.decoded;
        } else {
            int total = this.sizeX * this.sizeY * this.sizeZ;
            int[] out = new int[total];
            if (this.cells.isEmpty()) {
                return this.decoded = out;
            } else {
                byte[] packed;
                try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(this.cells)))) {
                    packed = gz.readAllBytes();
                } catch (IOException var9) {
                    throw new IllegalArgumentException(var9.getMessage());
                }

                this.decoded = unpackVarints(packed, total);
                return this.decoded;
            }
        }
    }

    public static boolean isAir(String block) {
        return AIR.contains(baseName(block));
    }

    public static boolean isVoid(String block) {
        return "minecraft:structure_void".equals(baseName(block));
    }

    private static String baseName(String block) {
        int at = block.indexOf(91);
        return at < 0 ? block : block.substring(0, at);
    }

    public String finalBlock(String block) {
        String name = baseName(block);
        String to = this.replace.get(block);
        if (to != null) {
            return to;
        } else {
            to = this.replace.get(name);
            if (to == null) {
                return block;
            } else {
                return !to.contains("[") && name.length() != block.length() ? to + block.substring(name.length()) : to;
            }
        }
    }

    public StructureDef.Action[] actions() {
        int[] g = this.grid();
        StructureDef.Action[] out = new StructureDef.Action[g.length];
        boolean[] air = new boolean[this.palette.size()];
        boolean[] nothing = new boolean[this.palette.size()];

        for (int i = 0; i < this.palette.size(); i++) {
            air[i] = isAir(this.palette.get(i));
            nothing[i] = isVoid(this.palette.get(i));
        }

        boolean[] outside = this.air.equals("auto") ? this.outsideAir(g, air) : null;

        for (int i = 0; i < g.length; i++) {
            int c = g[i];
            if (c >= this.palette.size() || nothing[c]) {
                out[i] = StructureDef.Action.SKIP;
            } else if (!air[c]) {
                out[i] = StructureDef.Action.PLACE;
            } else if (this.air.equals("clear")) {
                out[i] = StructureDef.Action.CLEAR;
            } else if (this.air.equals("keep")) {
                out[i] = StructureDef.Action.SKIP;
            } else {
                out[i] = outside[i] ? StructureDef.Action.SKIP : StructureDef.Action.CLEAR;
            }
        }

        return out;
    }

    private boolean[] outsideAir(int[] g, boolean[] air) {
        boolean[] seen = new boolean[g.length];
        ArrayDeque<Integer> open = new ArrayDeque<>();

        for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    if (x == 0 || z == 0 || x == this.sizeX - 1 || z == this.sizeZ - 1 || y == this.sizeY - 1) {
                        int i = this.index(x, y, z);
                        if (isOpen(g, air, i) && !seen[i]) {
                            seen[i] = true;
                            open.add(i);
                        }
                    }
                }
            }
        }

        int layer = this.sizeX * this.sizeZ;

        while (!open.isEmpty()) {
            int i = open.poll();
            int xx = i % this.sizeX;
            int z = i / this.sizeX % this.sizeZ;
            int y = i / layer;
            if (xx > 0) {
                visit(g, air, seen, open, i - 1);
            }

            if (xx < this.sizeX - 1) {
                visit(g, air, seen, open, i + 1);
            }

            if (z > 0) {
                visit(g, air, seen, open, i - this.sizeX);
            }

            if (z < this.sizeZ - 1) {
                visit(g, air, seen, open, i + this.sizeX);
            }

            if (y > 0) {
                visit(g, air, seen, open, i - layer);
            }

            if (y < this.sizeY - 1) {
                visit(g, air, seen, open, i + layer);
            }
        }

        return seen;
    }

    private static boolean isOpen(int[] g, boolean[] air, int i) {
        return g[i] < air.length && air[g[i]];
    }

    private static void visit(int[] g, boolean[] air, boolean[] seen, ArrayDeque<Integer> open, int i) {
        if (!seen[i] && isOpen(g, air, i)) {
            seen[i] = true;
            open.add(i);
        }
    }

    public StructureDef trimmed() {
        int[] g = this.grid();
        int minX = this.sizeX;
        int minY = this.sizeY;
        int minZ = this.sizeZ;
        int maxX = -1;
        int maxY = -1;
        int maxZ = -1;

        for (int y = 0; y < this.sizeY; y++) {
            for (int z = 0; z < this.sizeZ; z++) {
                for (int x = 0; x < this.sizeX; x++) {
                    String block = this.palette.get(g[this.index(x, y, z)]);
                    if (!isAir(block) && !isVoid(block)) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x);
                        maxY = Math.max(maxY, y);
                        maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }

        if (maxX < 0) {
            return this;
        } else {
            StructureDef out = new StructureDef();
            out.sizeX = maxX - minX + 1;
            out.sizeY = maxY - minY + 1;
            out.sizeZ = maxZ - minZ + 1;
            out.palette = new ArrayList<>(this.palette);
            out.air = this.air;
            out.replace = new LinkedHashMap<>(this.replace);
            out.foundation = this.foundation;
            out.foundationDepth = this.foundationDepth;
            int[] cut = new int[out.sizeX * out.sizeY * out.sizeZ];

            for (int y = 0; y < out.sizeY; y++) {
                for (int z = 0; z < out.sizeZ; z++) {
                    for (int xx = 0; xx < out.sizeX; xx++) {
                        int from = this.index(xx + minX, y + minY, z + minZ);
                        int to = out.index(xx, y, z);
                        cut[to] = g[from];
                        String tag = this.entities.get(String.valueOf(from));
                        if (tag != null) {
                            out.entities.put(String.valueOf(to), tag);
                        }
                    }
                }
            }

            out.setGrid(cut);
            return out;
        }
    }

    public Map<String, Integer> counts() {
        int[] per = new int[this.palette.size()];

        for (int c : this.grid()) {
            if (c < per.length) {
                per[c]++;
            }
        }

        List<Integer> order = new ArrayList<>();

        for (int i = 0; i < per.length; i++) {
            if (per[i] > 0 && !isAir(this.palette.get(i)) && !isVoid(this.palette.get(i))) {
                order.add(i);
            }
        }

        order.sort((a, b) -> per[b] - per[a]);
        Map<String, Integer> out = new LinkedHashMap<>();

        for (int ix : order) {
            out.merge(baseName(this.palette.get(ix)), per[ix], Integer::sum);
        }

        return out;
    }

    public static int[] unpackBits(long[] data, int bits, int count) {
        int[] out = new int[count];
        long mask = (1L << bits) - 1L;

        for (int i = 0; i < count; i++) {
            long start = (long)i * bits;
            int word = (int)(start >>> 6);
            int shift = (int)(start & 63L);
            if (word >= data.length) {
                break;
            }

            long v = data[word] >>> shift;
            if (shift + bits > 64 && word + 1 < data.length) {
                v |= data[word + 1] << 64 - shift;
            }

            out[i] = (int)(v & mask);
        }

        return out;
    }

    public static int[] unpackVarints(byte[] data, int count) {
        int[] out = new int[count];
        int at = 0;

        for (int i = 0; i < count && at < data.length; i++) {
            int value = 0;
            int shift = 0;

            while (true) {
                if (at < data.length) {
                    byte b = data[at++];
                    value |= (b & 127) << shift;
                    if ((b & 128) != 0) {
                        shift += 7;
                        continue;
                    }
                }

                out[i] = value;
                break;
            }
        }

        return out;
    }

    public static int bitsFor(int paletteSize) {
        return Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
    }

    public static enum Action {
        PLACE,
        CLEAR,
        SKIP;
    }
}
