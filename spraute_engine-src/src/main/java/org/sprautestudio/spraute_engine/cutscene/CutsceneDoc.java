package org.sprautestudio.spraute_engine.cutscene;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CutsceneDoc {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String title = "";
    public boolean smooth = true;
    public float bars = 0.11F;
    public boolean hideHud = true;
    public String hide;
    public static final List<String> HIDE_MODES = List.of("all", "minecraft", "classic", "custom", "none");
    public boolean lockPlayer = true;
    public boolean skippable = true;
    public float blendIn = 0.8F;
    public float blendOut = 0.8F;
    public String fadeColor = "#000000";
    public String post = "";
    public float fadeIn = 0.0F;
    public float fadeOut = 0.0F;
    public boolean relative = false;
    public boolean flow = false;
    public String flowEase = "ease_in_out_sine";
    public List<CutsceneDoc.Key> keys = new ArrayList<>();
    public List<CutsceneDoc.Clip> clips = new ArrayList<>();

    public static String hideTitle(String m) {
        return switch (m) {
            case "all" -> "весь интерфейс";
            case "minecraft" -> "Minecraft (хотбар, сердца, прицел…)";
            case "classic" -> "Spraute (HP, монеты, подсказки)";
            case "custom" -> "свои наложения (overlay)";
            default -> "ничего не скрывать";
        };
    }

    public String hideMode() {
        return this.hide != null && HIDE_MODES.contains(this.hide) ? this.hide : (this.hideHud ? "all" : "none");
    }

    public float duration() {
        float d = 0.0F;

        for (CutsceneDoc.Key k : this.keys) {
            d = Math.max(d, k.t);
        }

        for (CutsceneDoc.Clip c : this.clips) {
            d = Math.max(d, c.t);
        }

        return d;
    }

    public void sort() {
        this.keys.sort(Comparator.comparingDouble(k -> k.t));
        this.clips.sort(Comparator.comparingDouble(c -> c.t));
    }

    public String clientJson() {
        if (this.clips.isEmpty()) {
            return this.toJson();
        } else {
            CutsceneDoc d = fromJson(this.toJson());

            for (CutsceneDoc.Clip c : d.clips) {
                c.name = "";
                c.code = "";
                c.blocks = "";
            }

            return d.toJson();
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static CutsceneDoc fromJson(String json) {
        CutsceneDoc d = null;

        try {
            d = GSON.fromJson(json, CutsceneDoc.class);
        } catch (Exception var4) {
        }

        if (d == null) {
            d = new CutsceneDoc();
        }

        if (d.keys == null) {
            d.keys = new ArrayList<>();
        }

        for (CutsceneDoc.Key k : d.keys) {
            if (k.ease == null) {
                k.ease = "smooth";
            }

            if (k.look == null) {
                k.look = "";
            }

            if (k.caption == null) {
                k.caption = "";
            }

            if (k.mark == null) {
                k.mark = "";
            }
        }

        if (d.clips == null) {
            d.clips = new ArrayList<>();
        }

        for (CutsceneDoc.Clip c : d.clips) {
            if (c.name == null) {
                c.name = "";
            }

            if (c.code == null) {
                c.code = "";
            }

            if (c.blocks == null) {
                c.blocks = "";
            }
        }

        if (d.fadeColor == null) {
            d.fadeColor = "#000000";
        }

        if (d.title == null) {
            d.title = "";
        }

        if (d.flowEase == null) {
            d.flowEase = "ease_in_out_sine";
        }

        d.sort();
        return d;
    }

    public static float wrap(float a) {
        a %= 360.0F;
        if (a >= 180.0F) {
            a -= 360.0F;
        }

        if (a < -180.0F) {
            a += 360.0F;
        }

        return a;
    }

    public CutsceneDoc.Pose at(double t) {
        CutsceneDoc.Pose p = new CutsceneDoc.Pose();
        if (this.keys.isEmpty()) {
            return p;
        } else if (this.keys.size() != 1 && !(t <= this.keys.get(0).t)) {
            int n = this.keys.size();
            if (this.flow) {
                return this.atFlow(t);
            } else if (t >= this.keys.get(n - 1).t) {
                return from(p, this.keys.get(n - 1), n - 1);
            } else {
                int i = 0;

                while (i < n - 2 && t >= this.keys.get(i + 1).t) {
                    i++;
                }

                CutsceneDoc.Key a = this.keys.get(i);
                CutsceneDoc.Key b = this.keys.get(i + 1);
                double span = Math.max(1.0E-4, (double)(b.t - a.t));
                double u = Ease.apply(a.ease, (t - a.t) / span);
                CutsceneDoc.Key a0 = this.keys.get(Math.max(0, i - 1));
                CutsceneDoc.Key b1 = this.keys.get(Math.min(n - 1, i + 2));
                if (this.smooth) {
                    p.x = catmull(a0.x, a.x, b.x, b1.x, u);
                    p.y = catmull(a0.y, a.y, b.y, b1.y, u);
                    p.z = catmull(a0.z, a.z, b.z, b1.z, u);
                } else {
                    p.x = a.x + (b.x - a.x) * u;
                    p.y = a.y + (b.y - a.y) * u;
                    p.z = a.z + (b.z - a.z) * u;
                }

                p.yaw = (float)(a.yaw + wrap(b.yaw - a.yaw) * u);
                p.pitch = (float)(a.pitch + (b.pitch - a.pitch) * u);
                p.roll = (float)(a.roll + (b.roll - a.roll) * u);
                p.fov = (float)(a.fov + (b.fov - a.fov) * u);
                p.shake = a.shake;
                p.segment = i;
                return p;
            }
        } else {
            return from(p, this.keys.get(0), 0);
        }
    }

    private static CutsceneDoc.Pose from(CutsceneDoc.Pose p, CutsceneDoc.Key k, int i) {
        p.x = k.x;
        p.y = k.y;
        p.z = k.z;
        p.yaw = k.yaw;
        p.pitch = k.pitch;
        p.roll = k.roll;
        p.fov = k.fov;
        p.shake = k.shake;
        p.segment = i;
        return p;
    }

    private double[] point(int i, double u) {
        int n = this.keys.size();
        CutsceneDoc.Key a = this.keys.get(i);
        CutsceneDoc.Key b = this.keys.get(Math.min(n - 1, i + 1));
        if (!this.smooth) {
            return new double[]{a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u, a.z + (b.z - a.z) * u};
        } else {
            CutsceneDoc.Key a0 = this.keys.get(Math.max(0, i - 1));
            CutsceneDoc.Key b1 = this.keys.get(Math.min(n - 1, i + 2));
            return new double[]{catmull(a0.x, a.x, b.x, b1.x, u), catmull(a0.y, a.y, b.y, b1.y, u), catmull(a0.z, a.z, b.z, b1.z, u)};
        }
    }

    private CutsceneDoc.Pose atFlow(double t) {
        int n = this.keys.size();
        float t0 = this.keys.get(0).t;
        float t1 = this.keys.get(n - 1).t;
        if (t >= t1) {
            return from(new CutsceneDoc.Pose(), this.keys.get(n - 1), n - 1);
        } else {
            double g = Ease.apply(this.flowEase, (t - t0) / Math.max(1.0E-4, (double)(t1 - t0)));
            int steps = 24;
            double[][] cum = new double[n - 1][25];
            double[] start = new double[n];

            for (int i = 0; i < n - 1; i++) {
                double len = 0.0;
                double[] prev = this.point(i, 0.0);

                for (int s = 1; s <= 24; s++) {
                    double[] cur = this.point(i, s / 24.0);
                    len += Math.sqrt(sq(cur[0] - prev[0]) + sq(cur[1] - prev[1]) + sq(cur[2] - prev[2]));
                    cum[i][s] = len;
                    prev = cur;
                }

                start[i + 1] = start[i] + len;
            }

            double total = start[n - 1];
            int seg;
            double u;
            if (total < 1.0E-6) {
                double f = g * (n - 1);
                seg = Math.min(n - 2, (int)f);
                u = f - seg;
            } else {
                double dist = g * total;
                seg = 0;

                while (seg < n - 2 && dist > start[seg + 1]) {
                    seg++;
                }

                double local = dist - start[seg];
                int s = 1;

                while (s < 24 && cum[seg][s] < local) {
                    s++;
                }

                double c0 = cum[seg][s - 1];
                double c1 = cum[seg][s];
                u = (s - 1 + (c1 > c0 ? (local - c0) / (c1 - c0) : 0.0)) / 24.0;
            }

            CutsceneDoc.Pose p = new CutsceneDoc.Pose();
            double[] pos = this.point(seg, u);
            p.x = pos[0];
            p.y = pos[1];
            p.z = pos[2];
            float[] yaw = new float[n];
            yaw[0] = this.keys.get(0).yaw;

            for (int i = 1; i < n; i++) {
                yaw[i] = yaw[i - 1] + wrap(this.keys.get(i).yaw - this.keys.get(i - 1).yaw);
            }

            int a0 = Math.max(0, seg - 1);
            int b = Math.min(n - 1, seg + 1);
            int b1 = Math.min(n - 1, seg + 2);
            p.yaw = (float)catmull(yaw[a0], yaw[seg], yaw[b], yaw[b1], u);
            p.pitch = (float)catmull(this.keys.get(a0).pitch, this.keys.get(seg).pitch, this.keys.get(b).pitch, this.keys.get(b1).pitch, u);
            p.roll = (float)catmull(this.keys.get(a0).roll, this.keys.get(seg).roll, this.keys.get(b).roll, this.keys.get(b1).roll, u);
            p.fov = (float)catmull(this.keys.get(a0).fov, this.keys.get(seg).fov, this.keys.get(b).fov, this.keys.get(b1).fov, u);
            p.pitch = Math.max(-90.0F, Math.min(90.0F, p.pitch));
            p.shake = this.keys.get(seg).shake;
            p.segment = seg;
            return p;
        }
    }

    private static double sq(double v) {
        return v * v;
    }

    private static double catmull(double a, double b, double c, double d, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * (2.0 * b + (-a + c) * t + (2.0 * a - 5.0 * b + 4.0 * c - d) * t2 + (-a + 3.0 * b - 3.0 * c + d) * t3);
    }

    public static CutsceneDoc example(String name) {
        CutsceneDoc d = new CutsceneDoc();
        d.title = name;
        return d;
    }

    public static final class Clip {
        public float t;
        public String name = "";
        public String code = "";
        public String blocks = "";
        public boolean always = false;

        public CutsceneDoc.Clip copy() {
            CutsceneDoc.Clip c = new CutsceneDoc.Clip();
            c.t = this.t;
            c.name = this.name;
            c.code = this.code;
            c.blocks = this.blocks;
            c.always = this.always;
            return c;
        }

        public String title() {
            if (!this.name.isBlank()) {
                return this.name.trim();
            } else {
                for (String line : this.code.split("\\R")) {
                    if (!line.isBlank()) {
                        return line.trim();
                    }
                }

                return "пустой скрипт";
            }
        }
    }

    public static final class Key {
        public float t;
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float roll;
        public float fov = 70.0F;
        public String ease = "smooth";
        public String look = "";
        public String caption = "";
        public String mark = "";
        public float shake = 0.0F;
        public boolean hold = false;
        public float holdMax = 0.0F;
        public boolean holdPress = false;

        public CutsceneDoc.Key copy() {
            CutsceneDoc.Key k = new CutsceneDoc.Key();
            k.t = this.t;
            k.x = this.x;
            k.y = this.y;
            k.z = this.z;
            k.yaw = this.yaw;
            k.pitch = this.pitch;
            k.roll = this.roll;
            k.fov = this.fov;
            k.ease = this.ease;
            k.look = this.look;
            k.caption = this.caption;
            k.mark = this.mark;
            k.shake = this.shake;
            k.hold = this.hold;
            k.holdMax = this.holdMax;
            k.holdPress = this.holdPress;
            return k;
        }
    }

    public static final class Pose {
        public double x;
        public double y;
        public double z;
        public float yaw;
        public float pitch;
        public float roll;
        public float fov = 70.0F;
        public float shake;
        public int segment;

        public CutsceneDoc.Pose set(CutsceneDoc.Pose o) {
            this.x = o.x;
            this.y = o.y;
            this.z = o.z;
            this.yaw = o.yaw;
            this.pitch = o.pitch;
            this.roll = o.roll;
            this.fov = o.fov;
            this.shake = o.shake;
            this.segment = o.segment;
            return this;
        }

        public CutsceneDoc.Pose lerp(CutsceneDoc.Pose o, double k) {
            this.x = this.x + (o.x - this.x) * k;
            this.y = this.y + (o.y - this.y) * k;
            this.z = this.z + (o.z - this.z) * k;
            this.yaw = this.yaw + (float)(CutsceneDoc.wrap(o.yaw - this.yaw) * k);
            this.pitch = this.pitch + (float)((o.pitch - this.pitch) * k);
            this.roll = this.roll + (float)((o.roll - this.roll) * k);
            this.fov = this.fov + (float)((o.fov - this.fov) * k);
            this.shake = this.shake + (float)((o.shake - this.shake) * k);
            return this;
        }
    }
}
