package org.sprautestudio.spraute_engine.zone;

import java.util.Random;
import net.minecraft.world.phys.Vec3;

public class ZoneShape {
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

    public ZoneShape copy() {
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

    public void fix() {
        if (this.shape == null || !this.shape.equals("box") && !this.shape.equals("sphere") && !this.shape.equals("cylinder")) {
            this.shape = "box";
        }

        this.radius = Math.max(0.5, this.radius);
        this.height = Math.max(0.5, this.height);
    }

    public void move(int dx, int dy, int dz) {
        this.x1 += dx;
        this.x2 += dx;
        this.y1 += dy;
        this.y2 += dy;
        this.z1 += dz;
        this.z2 += dz;
        this.cx += dx;
        this.cy += dy;
        this.cz += dz;
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

    public double volume() {
        String var1 = this.shape;

        return switch (var1) {
            case "sphere" -> (Math.PI * 4.0 / 3.0) * this.radius * this.radius * this.radius;
            case "cylinder" -> Math.PI * this.radius * this.radius * this.height;
            default -> (this.maxX() - this.minX()) * (this.maxY() - this.minY()) * (this.maxZ() - this.minZ());
        };
    }

    public Vec3 randomPoint(Random r) {
        for (int i = 0; i < 64; i++) {
            double x = this.minX() + r.nextDouble() * (this.maxX() - this.minX());
            double z = this.minZ() + r.nextDouble() * (this.maxZ() - this.minZ());
            double y = this.shape.equals("sphere") ? this.cy - this.radius + r.nextDouble() * this.radius * 2.0 : this.minY();
            if (this.contains(x, y + 0.01, z)) {
                return new Vec3(x, y, z);
            }
        }

        return new Vec3((this.minX() + this.maxX()) / 2.0, this.shape.equals("sphere") ? this.cy : this.minY(), (this.minZ() + this.maxZ()) / 2.0);
    }

    public String size() {
        String var1 = this.shape;

        return switch (var1) {
            case "sphere" -> "шар, радиус " + fmt(this.radius);
            case "cylinder" -> "цилиндр, радиус " + fmt(this.radius) + ", высота " + fmt(this.height);
            default -> Math.abs(this.x2 - this.x1) + 1 + " × " + (Math.abs(this.y2 - this.y1) + 1) + " × " + (Math.abs(this.z2 - this.z1) + 1) + " блоков";
        };
    }

    static String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long)v) : String.valueOf(Math.round(v * 10.0) / 10.0);
    }
}
