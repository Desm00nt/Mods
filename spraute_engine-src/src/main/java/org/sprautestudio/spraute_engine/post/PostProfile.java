package org.sprautestudio.spraute_engine.post;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class PostProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public Map<String, String> values = new LinkedHashMap<>();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PostProfile fromJson(String json) {
        PostProfile p = null;

        try {
            p = GSON.fromJson(json, PostProfile.class);
        } catch (RuntimeException var3) {
        }

        if (p == null) {
            p = new PostProfile();
        }

        if (p.values == null) {
            p.values = new LinkedHashMap<>();
        }

        p.values.keySet().removeIf(k -> PostParams.get(k) == null);
        return p;
    }

    public static String problems(String json) {
        PostProfile p;
        try {
            p = GSON.fromJson(json, PostProfile.class);
        } catch (RuntimeException var3) {
            return "Не читается: " + var3.getMessage();
        }

        if (p != null && p.values != null) {
            List<String> out = new ArrayList<>();
            p.values.forEach((id, text) -> {
                PostParams.Param param = PostParams.get(id);
                if (param == null) {
                    out.add("нет настройки " + id);
                } else if (!param.color() && parse(id, text) == null) {
                    out.add(id + ": не число (" + text + ")");
                } else if (!param.color() && !text.isBlank()) {
                    double v = Double.parseDouble(text.trim().replace(',', '.'));
                    if (v < param.min() || v > param.max()) {
                        out.add(id + ": " + text + " вне " + param.min() + ".." + param.max());
                    }
                }
            });
            return String.join("; ", out);
        } else {
            return "";
        }
    }

    public PostProfile copy() {
        PostProfile p = new PostProfile();
        p.values.putAll(this.values);
        return p;
    }

    public double number(String id) {
        PostParams.Param p = PostParams.get(id);
        String v = this.values.get(id);
        if (p == null) {
            return 0.0;
        } else if (v == null) {
            return p.def();
        } else {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException var5) {
                return p.def();
            }
        }
    }

    public String colorText(String id) {
        PostParams.Param p = PostParams.get(id);
        String v = this.values.get(id);
        return v != null ? v : (p != null ? p.colorDef() : "#FFFFFF");
    }

    public Map<String, float[]> resolved() {
        Map<String, float[]> out = new LinkedHashMap<>();

        for (Entry<String, String> e : this.values.entrySet()) {
            float[] v = parse(e.getKey(), e.getValue());
            if (v != null) {
                out.put(e.getKey(), v);
            }
        }

        return out;
    }

    public static float[] parse(String id, String text) {
        PostParams.Param p = PostParams.get(id);
        if (p == null) {
            return null;
        } else if (p.color()) {
            return PostParams.rgb(text);
        } else {
            try {
                double d = Double.parseDouble(text.trim().replace(',', '.'));
                return new float[]{(float)Math.max(p.min(), Math.min(p.max(), d))};
            } catch (RuntimeException var5) {
                return null;
            }
        }
    }
}
