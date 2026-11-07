package org.sprautestudio.spraute_engine.fx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public class FxDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public float duration = 3.0F;
    public String sound = "";
    public float soundVolume = 1.0F;
    public float soundPitch = 1.0F;
    public List<FxDef.Layer> layers = new ArrayList<>();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static FxDef fromJson(String json) {
        FxDef d = null;

        try {
            d = GSON.fromJson(json, FxDef.class);
        } catch (Exception var4) {
        }

        if (d == null) {
            d = new FxDef();
        }

        if (d.layers == null) {
            d.layers = new ArrayList<>();
        }

        if (d.sound == null) {
            d.sound = "";
        }

        for (FxDef.Layer l : d.layers) {
            l.fix();
        }

        return d;
    }

    public FxDef copy() {
        return fromJson(this.toJson());
    }

    public static float curve(float[] keys, float t) {
        if (keys != null && keys.length != 0) {
            if (keys.length == 1) {
                return keys[0];
            } else {
                float u = Math.max(0.0F, Math.min(1.0F, t)) * (keys.length - 1);
                int i = Math.min(keys.length - 2, (int)u);
                float f = u - i;
                return keys[i] + (keys[i + 1] - keys[i]) * f;
            }
        } else {
            return 1.0F;
        }
    }

    public static int parseColor(String s) {
        if (s == null) {
            return 16777215;
        } else {
            String h = s.trim().replace("#", "");

            try {
                if (h.length() == 3) {
                    h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
                }

                return Integer.parseInt(h.substring(0, Math.min(6, h.length())), 16);
            } catch (Exception var3) {
                return 16777215;
            }
        }
    }

    public static int gradient(int[] colors, float t) {
        if (colors.length == 0) {
            return 16777215;
        } else if (colors.length == 1) {
            return colors[0];
        } else {
            float u = Math.max(0.0F, Math.min(1.0F, t)) * (colors.length - 1);
            int i = Math.min(colors.length - 2, (int)u);
            float f = u - i;
            int a = colors[i];
            int b = colors[i + 1];
            int r = (int)((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * f);
            int g = (int)((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * f);
            int bl = (int)((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * f);
            return r << 16 | g << 8 | bl;
        }
    }

    public static float[] parseCurve(String s, float[] fallback) {
        try {
            String[] p = s.split("[,;\\s]+");
            List<Float> out = new ArrayList<>();

            for (String x : p) {
                if (!x.isBlank()) {
                    out.add(Float.parseFloat(x.trim()));
                }
            }

            if (out.isEmpty()) {
                return fallback;
            } else {
                float[] r = new float[out.size()];

                for (int i = 0; i < r.length; i++) {
                    r[i] = out.get(i);
                }

                return r;
            }
        } catch (Exception var8) {
            return fallback;
        }
    }

    public static String curveText(float[] k) {
        StringBuilder b = new StringBuilder();

        for (int i = 0; i < k.length; i++) {
            if (i > 0) {
                b.append(", ");
            }

            float v = Math.round(k[i] * 1000.0F) / 1000.0F;
            b.append(v == (float)((long)v) ? String.valueOf((long)v) : String.valueOf(v));
        }

        return b.toString();
    }

    public static FxDef.Layer layer(String name) {
        FxDef.Layer l = new FxDef.Layer();
        l.name = name;
        return l;
    }

    public static class Layer {
        public String name = "Слой";
        public boolean enabled = true;
        public String type = "particles";
        public float delay;
        public float time;
        public float rate;
        public int burst = 20;
        public float burstEvery;
        public int max = 600;
        public String shape = "point";
        public float radius = 0.5F;
        public float radiusInner;
        public float height = 1.0F;
        public float angle = 30.0F;
        public boolean even;
        public float offsetX;
        public float offsetY;
        public float offsetZ;
        public float rotX;
        public float rotY;
        public float rotZ;
        public String direction = "out";
        public float speed = 2.0F;
        public float speedRandom = 0.5F;
        public float gravity;
        public float drag = 0.5F;
        public float orbit;
        public float turbulence;
        public float attract;
        public boolean local = true;
        public String texture = "glow";
        public String blend = "add";
        public String render = "billboard";
        public float life = 1.0F;
        public float lifeRandom = 0.3F;
        public float[] size = new float[]{0.4F, 0.1F};
        public float sizeRandom = 0.2F;
        public float[] alpha = new float[]{0.0F, 1.0F, 1.0F, 0.0F};
        public String[] color = new String[]{"#FFFFFF"};
        public boolean colorRandom;
        public float intensity = 1.0F;
        public float spin;
        public float spinRandom = 0.0F;
        public float stretch = 0.15F;
        public float beamLength = 20.0F;
        public String flashColor = "#FFFFFF";
        public float strength = 0.8F;
        public float range = 64.0F;

        void fix() {
            if (this.name == null) {
                this.name = "Слой";
            }

            if (this.type == null) {
                this.type = "particles";
            }

            if (this.shape == null) {
                this.shape = "point";
            }

            if (this.direction == null) {
                this.direction = "out";
            }

            if (this.texture == null) {
                this.texture = "glow";
            }

            if (this.blend == null) {
                this.blend = "add";
            }

            if (this.render == null) {
                this.render = "billboard";
            }

            if (this.size == null || this.size.length == 0) {
                this.size = new float[]{0.4F};
            }

            if (this.alpha == null || this.alpha.length == 0) {
                this.alpha = new float[]{1.0F};
            }

            if (this.color == null || this.color.length == 0) {
                this.color = new String[]{"#FFFFFF"};
            }

            if (this.flashColor == null) {
                this.flashColor = "#FFFFFF";
            }

            this.max = Math.max(1, Math.min(20000, this.max));
        }
    }
}
