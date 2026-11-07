package org.sprautestudio.spraute_engine.path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.world.phys.Vec3;

public class PathDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public List<PathDef.Point> points = new ArrayList<>();
    public String mode = "once";
    public double speed = 1.0;
    public int color = -21965;
    public String dimension = "";

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PathDef fromJson(String json) {
        PathDef d = null;

        try {
            d = GSON.fromJson(json, PathDef.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new PathDef();
        }

        d.fix();
        return d;
    }

    public void fix() {
        if (this.points == null) {
            this.points = new ArrayList<>();
        }

        this.points.removeIf(Objects::isNull);
        if (this.mode == null || !List.of("once", "loop", "pingpong").contains(this.mode)) {
            this.mode = "once";
        }

        if (this.speed <= 0.0) {
            this.speed = 1.0;
        }

        if (this.dimension == null) {
            this.dimension = "";
        }

        for (PathDef.Point p : this.points) {
            if (p.anim == null) {
                p.anim = "";
            }

            p.wait = Math.max(0.0, p.wait);
        }
    }

    public double length() {
        double l = 0.0;

        for (int i = 1; i < this.points.size(); i++) {
            l += this.points.get(i).vec().distanceTo(this.points.get(i - 1).vec());
        }

        return l;
    }

    public static PathDef example(Vec3 at) {
        PathDef d = new PathDef();
        double x = Math.floor(at.x) + 0.5;
        double y = Math.floor(at.y);
        double z = Math.floor(at.z) + 0.5;
        d.points.add(new PathDef.Point(x, y, z));
        d.points.add(new PathDef.Point(x + 6.0, y, z));
        PathDef.Point p = new PathDef.Point(x + 6.0, y, z + 6.0);
        p.wait = 2.0;
        d.points.add(p);
        d.points.add(new PathDef.Point(x, y, z + 6.0));
        return d;
    }

    public static class Point {
        public double x;
        public double y;
        public double z;
        public double wait;
        public double speed;
        public String anim = "";
        public Double yaw;

        public Point() {
        }

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3 vec() {
            return new Vec3(this.x, this.y, this.z);
        }
    }
}
