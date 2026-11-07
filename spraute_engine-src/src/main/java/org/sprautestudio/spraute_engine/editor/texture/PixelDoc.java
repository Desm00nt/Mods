package org.sprautestudio.spraute_engine.editor.texture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.imageio.ImageIO;

public final class PixelDoc {
    public int w;
    public int h;
    public final List<PixelDoc.Layer> layers = new ArrayList<>();
    public int active;
    public int version;
    public int savedVersion;
    private final List<PixelDoc.State> undo = new ArrayList<>();
    private final List<PixelDoc.State> redo = new ArrayList<>();

    public PixelDoc(int w, int h) {
        this.w = w;
        this.h = h;
    }

    public PixelDoc.Layer layer() {
        return this.layers.get(Math.max(0, Math.min(this.active, this.layers.size() - 1)));
    }

    public boolean dirty() {
        return this.version != this.savedVersion;
    }

    public void checkpoint() {
        List<PixelDoc.Layer> copy = new ArrayList<>();

        for (PixelDoc.Layer l : this.layers) {
            copy.add(l.copy());
        }

        this.undo.add(new PixelDoc.State(this.w, this.h, copy, this.active));

        while (this.undo.size() > 80) {
            this.undo.remove(0);
        }

        this.redo.clear();
        this.version++;
    }

    public boolean undo() {
        if (this.undo.isEmpty()) {
            return false;
        } else {
            this.redo.add(this.snapshot());
            this.restore(this.undo.remove(this.undo.size() - 1));
            this.version++;
            return true;
        }
    }

    public boolean redo() {
        if (this.redo.isEmpty()) {
            return false;
        } else {
            this.undo.add(this.snapshot());
            this.restore(this.redo.remove(this.redo.size() - 1));
            this.version++;
            return true;
        }
    }

    private PixelDoc.State snapshot() {
        List<PixelDoc.Layer> copy = new ArrayList<>();

        for (PixelDoc.Layer l : this.layers) {
            copy.add(l.copy());
        }

        return new PixelDoc.State(this.w, this.h, copy, this.active);
    }

    private void restore(PixelDoc.State s) {
        this.w = s.w;
        this.h = s.h;
        this.layers.clear();
        this.layers.addAll(s.layers);
        this.active = s.active;
    }

    public boolean in(int x, int y) {
        return x >= 0 && y >= 0 && x < this.w && y < this.h;
    }

    public int[] flatten() {
        int[] out = new int[this.w * this.h];

        for (PixelDoc.Layer l : this.layers) {
            if (l.visible && l.opacity > 0) {
                for (int i = 0; i < out.length; i++) {
                    out[i] = PixelOps.over(out[i], l.px[i], l.opacity / 100.0F);
                }
            }
        }

        return out;
    }

    public int flatAt(int x, int y) {
        if (!this.in(x, y)) {
            return 0;
        } else {
            int c = 0;

            for (PixelDoc.Layer l : this.layers) {
                if (l.visible) {
                    c = PixelOps.over(c, l.px[y * this.w + x], l.opacity / 100.0F);
                }
            }

            return c;
        }
    }

    public PixelDoc.Layer addLayer(String name) {
        PixelDoc.Layer l = new PixelDoc.Layer(name, this.w, this.h);
        this.layers.add(this.active + 1, l);
        this.active++;
        return l;
    }

    public void mergeDown() {
        if (this.active > 0) {
            PixelDoc.Layer top = this.layers.get(this.active);
            PixelDoc.Layer below = this.layers.get(this.active - 1);

            for (int i = 0; i < below.px.length; i++) {
                if (top.visible) {
                    below.px[i] = PixelOps.over(below.px[i], top.px[i], top.opacity / 100.0F);
                }
            }

            this.layers.remove(this.active);
            this.active--;
        }
    }

    public void resize(int nw, int nh, int ax, int ay) {
        int ox = ax == 0 ? 0 : (ax == 1 ? (nw - this.w) / 2 : nw - this.w);
        int oy = ay == 0 ? 0 : (ay == 1 ? (nh - this.h) / 2 : nh - this.h);

        for (PixelDoc.Layer l : this.layers) {
            int[] n = new int[nw * nh];

            for (int y = 0; y < this.h; y++) {
                for (int x = 0; x < this.w; x++) {
                    int tx = x + ox;
                    int ty = y + oy;
                    if (tx >= 0 && ty >= 0 && tx < nw && ty < nh) {
                        n[ty * nw + tx] = l.px[y * this.w + x];
                    }
                }
            }

            l.px = n;
        }

        this.w = nw;
        this.h = nh;
    }

    public static Path layersFile(Path png) {
        String n = png.getFileName().toString();
        int dot = n.lastIndexOf(46);
        return png.getParent().resolve(".layers").resolve((dot > 0 ? n.substring(0, dot) : n) + ".json");
    }

    public static PixelDoc load(Path file) throws IOException {
        BufferedImage img = ImageIO.read(file.toFile());
        if (img == null) {
            throw new IOException("Картинка не читается");
        } else {
            Path lf = layersFile(file);
            if (Files.exists(lf)) {
                try {
                    JsonObject o = JsonParser.parseString(Files.readString(lf, StandardCharsets.UTF_8)).getAsJsonObject();
                    long stamp = o.has("stamp") ? o.get("stamp").getAsLong() : -1L;
                    if (stamp == Files.getLastModifiedTime(file).toMillis()
                        && o.get("w").getAsInt() == img.getWidth()
                        && o.get("h").getAsInt() == img.getHeight()) {
                        PixelDoc d = new PixelDoc(img.getWidth(), img.getHeight());

                        for (JsonElement e : o.getAsJsonArray("layers")) {
                            JsonObject lo = e.getAsJsonObject();
                            PixelDoc.Layer l = new PixelDoc.Layer(lo.get("name").getAsString(), d.w, d.h);
                            l.visible = !lo.has("visible") || lo.get("visible").getAsBoolean();
                            l.locked = lo.has("locked") && lo.get("locked").getAsBoolean();
                            l.opacity = lo.has("opacity") ? lo.get("opacity").getAsInt() : 100;
                            BufferedImage li = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(lo.get("png").getAsString())));
                            li.getRGB(0, 0, d.w, d.h, l.px, 0, d.w);
                            d.layers.add(l);
                        }

                        d.active = Math.max(0, Math.min(o.has("active") ? o.get("active").getAsInt() : 0, d.layers.size() - 1));
                        if (!d.layers.isEmpty()) {
                            return d;
                        }
                    }
                } catch (Exception var12) {
                }
            }

            PixelDoc d = new PixelDoc(img.getWidth(), img.getHeight());
            PixelDoc.Layer l = new PixelDoc.Layer("Фон", d.w, d.h);
            img.getRGB(0, 0, d.w, d.h, l.px, 0, d.w);
            d.layers.add(l);
            return d;
        }
    }

    public void save(Path file) throws IOException {
        BufferedImage out = new BufferedImage(this.w, this.h, 2);
        out.setRGB(0, 0, this.w, this.h, this.flatten(), 0, this.w);
        Path png = file.getFileName().toString().toLowerCase().endsWith(".png")
            ? file
            : file.resolveSibling(file.getFileName().toString().replaceAll("\\.[^.]+$", "") + ".png");
        ImageIO.write(out, "png", png.toFile());
        Path lf = layersFile(png);
        if (this.layers.size() == 1 && this.layers.get(0).visible && this.layers.get(0).opacity == 100) {
            Files.deleteIfExists(lf);
        } else {
            JsonObject o = new JsonObject();
            o.addProperty("w", this.w);
            o.addProperty("h", this.h);
            o.addProperty("active", this.active);
            o.addProperty("stamp", Files.getLastModifiedTime(png).toMillis());
            JsonArray arr = new JsonArray();

            for (PixelDoc.Layer l : this.layers) {
                JsonObject lo = new JsonObject();
                lo.addProperty("name", l.name);
                lo.addProperty("visible", l.visible);
                lo.addProperty("locked", l.locked);
                lo.addProperty("opacity", l.opacity);
                BufferedImage li = new BufferedImage(this.w, this.h, 2);
                li.setRGB(0, 0, this.w, this.h, l.px, 0, this.w);
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                ImageIO.write(li, "png", bo);
                lo.addProperty("png", Base64.getEncoder().encodeToString(bo.toByteArray()));
                arr.add(lo);
            }

            o.add("layers", arr);
            Files.createDirectories(lf.getParent());
            Files.writeString(lf, o.toString(), StandardCharsets.UTF_8);
        }

        this.savedVersion = this.version;
    }

    public static final class Layer {
        public String name;
        public int[] px;
        public boolean visible = true;
        public boolean locked;
        public int opacity = 100;

        public Layer(String name, int w, int h) {
            this.name = name;
            this.px = new int[w * h];
        }

        PixelDoc.Layer copy() {
            PixelDoc.Layer l = new PixelDoc.Layer(this.name, 0, 0);
            l.px = (int[])this.px.clone();
            l.visible = this.visible;
            l.locked = this.locked;
            l.opacity = this.opacity;
            return l;
        }
    }

    private record State(int w, int h, List<PixelDoc.Layer> layers, int active) {
    }
}
