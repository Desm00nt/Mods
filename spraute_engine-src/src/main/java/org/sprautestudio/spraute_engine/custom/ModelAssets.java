package org.sprautestudio.spraute_engine.custom;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map.Entry;
import org.sprautestudio.spraute_engine.core.model.SpJavaModel;

public final class ModelAssets {
    public static final List<String> PLACES = List.of(
        "thirdperson_righthand", "thirdperson_lefthand", "firstperson_righthand", "firstperson_lefthand", "gui", "head", "ground", "fixed"
    );

    private ModelAssets() {
    }

    public static JsonObject model(SpJavaModel.Result r, String texture, JsonObject display) {
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/block");
        JsonObject t = new JsonObject();
        t.addProperty("particle", texture);
        t.addProperty("0", texture);
        m.add("textures", t);
        m.add("elements", r.json("#0"));
        if (display != null) {
            m.add("display", display);
        }

        return m;
    }

    public static JsonObject particles(String texture) {
        JsonObject m = new JsonObject();
        m.addProperty("parent", "minecraft:block/block");
        JsonObject t = new JsonObject();
        t.addProperty("particle", texture);
        m.add("textures", t);
        return m;
    }

    public static JsonObject engineItem(String texture, JsonObject display) {
        JsonObject m = new JsonObject();
        m.addProperty("parent", "builtin/entity");
        JsonObject t = new JsonObject();
        t.addProperty("particle", texture);
        m.add("textures", t);
        JsonObject all = blockDisplay();
        if (display != null) {
            for (Entry<String, JsonElement> e : display.entrySet()) {
                all.add(e.getKey(), e.getValue());
            }
        }

        m.add("display", all);
        return m;
    }

    public static JsonObject display(ModelLook look, JsonObject fromBlockbench) {
        String var2 = look.hold;

        return switch (var2) {
            case "handheld" -> handheld();
            case "block" -> null;
            default -> fromBlockbench == null ? null : clean(fromBlockbench);
        };
    }

    public static JsonObject clean(JsonObject display) {
        JsonObject out = new JsonObject();

        for (String place : PLACES) {
            if (display.has(place) && display.get(place).isJsonObject()) {
                JsonObject from = display.getAsJsonObject(place);
                JsonObject to = new JsonObject();

                for (String k : List.of("rotation", "translation", "scale")) {
                    JsonElement v = from.get(k);
                    if (v != null && v.isJsonArray() && v.getAsJsonArray().size() == 3) {
                        to.add(k, v);
                    }
                }

                out.add(place, to);
            }
        }

        return out;
    }

    private static JsonObject blockDisplay() {
        JsonObject d = new JsonObject();
        d.add("gui", transform(30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F));
        d.add("ground", transform(0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 0.0F, 0.25F));
        d.add("fixed", transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        d.add("thirdperson_righthand", transform(75.0F, 45.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F));
        d.add("thirdperson_lefthand", transform(75.0F, 45.0F, 0.0F, 0.0F, 2.5F, 0.0F, 0.375F));
        d.add("firstperson_righthand", transform(0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F));
        d.add("firstperson_lefthand", transform(0.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F));
        return d;
    }

    private static JsonObject handheld() {
        JsonObject d = new JsonObject();
        d.add("thirdperson_righthand", transform(0.0F, -90.0F, 55.0F, 0.0F, 4.0F, 0.5F, 0.85F));
        d.add("thirdperson_lefthand", transform(0.0F, 90.0F, -55.0F, 0.0F, 4.0F, 0.5F, 0.85F));
        d.add("firstperson_righthand", transform(0.0F, -90.0F, 25.0F, 1.13F, 3.2F, 1.13F, 0.68F));
        d.add("firstperson_lefthand", transform(0.0F, 90.0F, -25.0F, 1.13F, 3.2F, 1.13F, 0.68F));
        return d;
    }

    private static JsonObject transform(float rx, float ry, float rz, float tx, float ty, float tz, float s) {
        JsonObject o = new JsonObject();
        o.add("rotation", arr(rx, ry, rz));
        o.add("translation", arr(tx, ty, tz));
        o.add("scale", arr(s, s, s));
        return o;
    }

    private static JsonArray arr(float a, float b, float c) {
        JsonArray x = new JsonArray();
        x.add(a);
        x.add(b);
        x.add(c);
        return x;
    }
}
