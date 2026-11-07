package org.sprautestudio.spraute_engine.zone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import net.minecraft.world.phys.Vec3;

public class ZoneDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String shape = "box";
    public int x1;
    public int y1;
    public int z1;
    public int x2 = 10;
    public int y2 = 5;
    public int z2 = 10;
    public double cx;
    public double cy;
    public double cz;
    public double radius = 8.0;
    public double height = 6.0;
    public String dimension = "";
    public boolean enabled = true;
    public String title = "";
    public String subtitle = "";
    public String titleMode = "first";
    public String wall = "none";
    public String wallText = "";
    public String post = "";
    public double postFade = 1.0;
    public String camera = "";
    public double cameraFade = 1.0;
    public int color = -11549705;
    public boolean hideInGame = true;
    public List<ZoneShape> more = new ArrayList<>();
    public int editIndex = 0;

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ZoneDef fromJson(String json) {
        ZoneDef d = null;

        try {
            d = GSON.fromJson(json, ZoneDef.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new ZoneDef();
        }

        d.fix();
        return d;
    }

    public void fix() {
        if (this.shape == null || !this.shape.equals("box") && !this.shape.equals("sphere") && !this.shape.equals("cylinder")) {
            this.shape = "box";
        }

        if (this.dimension == null) {
            this.dimension = "";
        }

        if (this.title == null) {
            this.title = "";
        }

        if (this.subtitle == null) {
            this.subtitle = "";
        }

        if (this.titleMode == null) {
            this.titleMode = "first";
        }

        if (this.wall == null) {
            this.wall = "none";
        }

        if (this.wallText == null) {
            this.wallText = "";
        }

        if (this.post == null) {
            this.post = "";
        }

        if (this.camera == null) {
            this.camera = "";
        }

        this.radius = Math.max(0.5, this.radius);
        this.height = Math.max(0.5, this.height);
        if (this.more == null) {
            this.more = new ArrayList<>();
        }

        this.more.removeIf(Objects::isNull);

        for (ZoneShape s : this.more) {
            s.fix();
        }

        this.editIndex = Math.max(0, Math.min(this.editIndex, this.more.size()));
    }

    public ZoneShape primary() {
        ZoneShape s = new ZoneShape();
        s.shape = this.shape;
        s.x1 = this.x1;
        s.y1 = this.y1;
        s.z1 = this.z1;
        s.x2 = this.x2;
        s.y2 = this.y2;
        s.z2 = this.z2;
        s.cx = this.cx;
        s.cy = this.cy;
        s.cz = this.cz;
        s.radius = this.radius;
        s.height = this.height;
        return s;
    }

    public void setPrimary(ZoneShape s) {
        this.shape = s.shape;
        this.x1 = s.x1;
        this.y1 = s.y1;
        this.z1 = s.z1;
        this.x2 = s.x2;
        this.y2 = s.y2;
        this.z2 = s.z2;
        this.cx = s.cx;
        this.cy = s.cy;
        this.cz = s.cz;
        this.radius = s.radius;
        this.height = s.height;
    }

    public List<ZoneShape> shapes() {
        List<ZoneShape> out = new ArrayList<>();
        out.add(this.primary());
        out.addAll(this.more);
        return out;
    }

    public int pieces() {
        return 1 + this.more.size();
    }

    public List<ZoneShape> ordered() {
        List<ZoneShape> all = new ArrayList<>(this.more);
        all.add(Math.max(0, Math.min(this.editIndex, this.more.size())), this.primary());
        return all;
    }

    private void setAll(List<ZoneShape> all, int k) {
        k = Math.max(0, Math.min(k, all.size() - 1));
        this.setPrimary(all.get(k));
        List<ZoneShape> rest = new ArrayList<>(all);
        rest.remove(k);
        this.more = rest;
        this.editIndex = k;
    }

    public void editPiece(int k) {
        this.setAll(this.ordered(), k);
    }

    public void nextPiece() {
        this.setAll(this.ordered(), (this.editIndex + 1) % this.pieces());
    }

    public int orderOfMore(int i) {
        return i < this.editIndex ? i : i + 1;
    }

    public void addPiece() {
        ZoneShape cur = this.primary();
        ZoneShape n = cur.copy();
        n.move((int)Math.ceil(cur.maxX() - cur.minX()) + 1, 0, 0);
        List<ZoneShape> all = this.ordered();
        all.add(n);
        this.setAll(all, all.size() - 1);
    }

    public boolean removePiece() {
        if (this.more.isEmpty()) {
            return false;
        } else {
            List<ZoneShape> all = this.ordered();
            int k = this.editIndex;
            all.remove(k);
            this.setAll(all, Math.min(k, all.size() - 1));
            return true;
        }
    }

    public double uMinX() {
        double v = this.minX();

        for (ZoneShape s : this.more) {
            v = Math.min(v, s.minX());
        }

        return v;
    }

    public double uMaxX() {
        double v = this.maxX();

        for (ZoneShape s : this.more) {
            v = Math.max(v, s.maxX());
        }

        return v;
    }

    public double uMinY() {
        double v = this.minY();

        for (ZoneShape s : this.more) {
            v = Math.min(v, s.minY());
        }

        return v;
    }

    public double uMaxY() {
        double v = this.maxY();

        for (ZoneShape s : this.more) {
            v = Math.max(v, s.maxY());
        }

        return v;
    }

    public double uMinZ() {
        double v = this.minZ();

        for (ZoneShape s : this.more) {
            v = Math.min(v, s.minZ());
        }

        return v;
    }

    public double uMaxZ() {
        double v = this.maxZ();

        for (ZoneShape s : this.more) {
            v = Math.max(v, s.maxZ());
        }

        return v;
    }

    public static ZoneDef example(String name, Vec3 at) {
        ZoneDef d = new ZoneDef();
        int x = (int)Math.floor(at.x);
        int y = (int)Math.floor(at.y);
        int z = (int)Math.floor(at.z);
        d.x1 = x - 5;
        d.y1 = y - 1;
        d.z1 = z - 5;
        d.x2 = x + 5;
        d.y2 = y + 5;
        d.z2 = z + 5;
        d.cx = x + 0.5;
        d.cy = y;
        d.cz = z + 0.5;
        d.title = name;
        return d;
    }

    public double minX() {
        return this.shape.equals("box") ? Math.min(this.x1, this.x2) : this.cx - this.radius;
    }

    public double maxX() {
        return this.shape.equals("box") ? Math.max(this.x1, this.x2) + 1 : this.cx + this.radius;
    }

    public double minY() {
        return this.shape.equals("box") ? Math.min(this.y1, this.y2) : (this.shape.equals("sphere") ? this.cy - this.radius : this.cy);
    }

    public double maxY() {
        return this.shape.equals("box") ? Math.max(this.y1, this.y2) + 1 : (this.shape.equals("sphere") ? this.cy + this.radius : this.cy + this.height);
    }

    public double minZ() {
        return this.shape.equals("box") ? Math.min(this.z1, this.z2) : this.cz - this.radius;
    }

    public double maxZ() {
        return this.shape.equals("box") ? Math.max(this.z1, this.z2) + 1 : this.cz + this.radius;
    }

    public boolean contains(double x, double y, double z) {
        if (this.containsMain(x, y, z)) {
            return true;
        } else {
            for (ZoneShape s : this.more) {
                if (s.contains(x, y, z)) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean containsMain(double x, double y, double z) {
        String var7 = this.shape;
        switch (var7) {
            case "sphere": {
                double dx = x - this.cx;
                double dy = y - this.cy;
                double dz = z - this.cz;
                return dx * dx + dy * dy + dz * dz <= this.radius * this.radius;
            }
            case "cylinder": {
                double dx = x - this.cx;
                double dz = z - this.cz;
                return y >= this.cy && y <= this.cy + this.height && dx * dx + dz * dz <= this.radius * this.radius;
            }
            default:
                return x >= this.minX() && x < this.maxX() && y >= this.minY() && y < this.maxY() && z >= this.minZ() && z < this.maxZ();
        }
    }

    public boolean holds(double x, double y, double z) {
        return this.contains(x, y + 0.1, z) || this.contains(x, y + 1.0, z);
    }

    public Vec3 center() {
        return this.more.isEmpty()
            ? new Vec3((this.minX() + this.maxX()) / 2.0, this.shape.equals("sphere") ? this.cy : this.minY(), (this.minZ() + this.maxZ()) / 2.0)
            : new Vec3((this.uMinX() + this.uMaxX()) / 2.0, this.uMinY(), (this.uMinZ() + this.uMaxZ()) / 2.0);
    }

    public Vec3 randomPoint(Random r) {
        List<ZoneShape> all = this.shapes();
        double total = 0.0;

        for (ZoneShape s : all) {
            total += Math.max(0.001, s.volume());
        }

        double pick = r.nextDouble() * total;

        for (ZoneShape s : all) {
            pick -= Math.max(0.001, s.volume());
            if (pick <= 0.0) {
                return s.randomPoint(r);
            }
        }

        return all.get(0).randomPoint(r);
    }

    public String size() {
        return this.primary().size() + (this.more.isEmpty() ? "" : "  · частей: " + this.pieces());
    }

    private static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long)v) : String.valueOf(Math.round(v * 10.0) / 10.0);
    }
}
