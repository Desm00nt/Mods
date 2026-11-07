package org.sprautestudio.spraute_engine.recipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class RecipeDef {
    public static final String SHAPED = "minecraft:crafting_shaped";
    public static final String SHAPELESS = "minecraft:crafting_shapeless";
    public static final String SMELTING = "minecraft:smelting";
    public static final String BLASTING = "minecraft:blasting";
    public static final String SMOKING = "minecraft:smoking";
    public static final String CAMPFIRE = "minecraft:campfire_cooking";
    public static final String STONECUTTING = "minecraft:stonecutting";
    public static final List<String> TYPES = List.of(
        "minecraft:crafting_shaped",
        "minecraft:crafting_shapeless",
        "minecraft:smelting",
        "minecraft:blasting",
        "minecraft:smoking",
        "minecraft:campfire_cooking",
        "minecraft:stonecutting"
    );
    public static final String OWN_NAMESPACE = "spraute";
    public static final String DISABLED = "spraute_disabled";
    private static final String PLACEHOLDER = "spraute_placeholder";
    public static final int SLOTS = 9;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String KEY_CHARS = "ABCDEFGHI";
    private static final Set<String> KNOWN = Set.of(
        "type", "group", "pattern", "key", "ingredients", "ingredient", "result", "count", "experience", "cookingtime", "spraute_disabled"
    );
    public String id = "";
    public String type = "minecraft:crafting_shaped";
    public String group = "";
    public final String[] slots = new String[9];
    public String result = "";
    public int count = 1;
    public String resultNbt = "";
    public float experience = 0.1F;
    public int cookingTime = 200;
    public boolean disabled;
    public JsonObject raw;
    private final JsonObject extras = new JsonObject();

    public RecipeDef() {
        Arrays.fill(this.slots, "");
    }

    public static RecipeDef create(String id, String type) {
        RecipeDef d = new RecipeDef();
        d.id = id;
        d.setType(type);
        return d;
    }

    public static RecipeDef other(String id, String type, String result) {
        RecipeDef d = new RecipeDef();
        d.id = id;
        d.type = type;
        d.result = result == null ? "" : result;
        d.raw = new JsonObject();
        d.raw.addProperty("type", type);
        d.raw.addProperty("spraute_placeholder", true);
        if (!d.result.isEmpty()) {
            d.raw.addProperty("result", d.result);
        }

        return d;
    }

    public boolean editable() {
        return this.raw == null;
    }

    public boolean placeholder() {
        return this.raw != null && this.raw.has("spraute_placeholder");
    }

    public boolean own() {
        return this.id.startsWith("spraute:");
    }

    public static boolean crafting(String type) {
        return type.equals("minecraft:crafting_shaped") || type.equals("minecraft:crafting_shapeless");
    }

    public static boolean cooking(String type) {
        return type.equals("minecraft:smelting")
            || type.equals("minecraft:blasting")
            || type.equals("minecraft:smoking")
            || type.equals("minecraft:campfire_cooking");
    }

    public static int defaultTime(String type) {
        return type.equals("minecraft:smelting") ? 200 : (type.equals("minecraft:campfire_cooking") ? 600 : 100);
    }

    public void setType(String next) {
        if (cooking(next) && (!cooking(this.type) || this.cookingTime == defaultTime(this.type))) {
            this.cookingTime = defaultTime(next);
        }

        if (!crafting(next)) {
            String first = "";

            for (String s : this.slots) {
                if (!s.isEmpty()) {
                    first = s;
                    break;
                }
            }

            Arrays.fill(this.slots, "");
            this.slots[0] = first;
        }

        if (cooking(next)) {
            this.count = 1;
        }

        this.type = next;
    }

    public RecipeDef copy(String newId) {
        RecipeDef d = fromJson(newId, this.toJson());
        d.disabled = false;
        return d;
    }

    public String problem() {
        if (this.editable() && !this.disabled) {
            if (this.result.isBlank()) {
                return "Не выбран результат";
            } else if (Arrays.stream(this.slots).allMatch(String::isBlank)) {
                return "Нет ингредиентов";
            } else {
                for (String s : this.slots) {
                    if (s.startsWith("{") || s.startsWith("[")) {
                        try {
                            JsonParser.parseString(s);
                        } catch (RuntimeException var6) {
                            return "Неверный JSON ингредиента";
                        }
                    }
                }

                return null;
            }
        } else {
            return null;
        }
    }

    public static JsonElement ingredientJson(String spec) {
        String s = spec == null ? "" : spec.trim();
        if (s.isEmpty()) {
            return new JsonArray();
        } else if (!s.startsWith("{") && !s.startsWith("[")) {
            String[] parts = s.split("\\|");
            if (parts.length == 1) {
                return single(parts[0].trim());
            } else {
                JsonArray all = new JsonArray();

                for (String p : parts) {
                    if (!p.isBlank()) {
                        all.add(single(p.trim()));
                    }
                }

                return all;
            }
        } else {
            return JsonParser.parseString(s);
        }
    }

    private static JsonObject single(String s) {
        JsonObject o = new JsonObject();
        if (s.startsWith("#")) {
            o.addProperty("tag", s.substring(1));
        } else {
            o.addProperty("item", s);
        }

        return o;
    }

    public static String specOf(JsonElement e) {
        if (e != null && !e.isJsonNull()) {
            if (e.isJsonObject()) {
                String simple = simple(e.getAsJsonObject());
                return simple != null ? simple : e.toString();
            } else if (!e.isJsonArray()) {
                return e.toString();
            } else {
                List<String> parts = new ArrayList<>();

                for (JsonElement p : e.getAsJsonArray()) {
                    String simple = p.isJsonObject() ? simple(p.getAsJsonObject()) : null;
                    if (simple == null) {
                        return e.toString();
                    }

                    parts.add(simple);
                }

                return String.join("|", parts);
            }
        } else {
            return "";
        }
    }

    private static String simple(JsonObject o) {
        if (o.size() != 1) {
            return null;
        } else if (o.has("item") && o.get("item").isJsonPrimitive()) {
            return o.get("item").getAsString();
        } else {
            return o.has("tag") && o.get("tag").isJsonPrimitive() ? "#" + o.get("tag").getAsString() : null;
        }
    }

    public String toJson() {
        return GSON.toJson((JsonElement)this.toObject());
    }

    public JsonObject toObject() {
        if (this.raw != null) {
            JsonObject o = this.raw.deepCopy();
            if (this.disabled) {
                o.addProperty("spraute_disabled", true);
            } else {
                o.remove("spraute_disabled");
            }

            return o;
        } else {
            JsonObject o = new JsonObject();
            o.addProperty("type", this.type);
            if (!this.group.isEmpty()) {
                o.addProperty("group", this.group);
            }

            if (this.type.equals("minecraft:crafting_shaped")) {
                this.writeShaped(o);
            } else if (this.type.equals("minecraft:crafting_shapeless")) {
                JsonArray list = new JsonArray();

                for (String s : this.slots) {
                    if (!s.isBlank()) {
                        list.add(ingredientJson(s));
                    }
                }

                o.add("ingredients", list);
            } else {
                o.add("ingredient", ingredientJson(this.slots[0]));
            }

            this.writeResult(o);
            if (cooking(this.type)) {
                o.addProperty("experience", this.experience);
                o.addProperty("cookingtime", this.cookingTime);
            }

            for (Entry<String, JsonElement> e : this.extras.entrySet()) {
                o.add(e.getKey(), e.getValue());
            }

            if (this.disabled) {
                o.addProperty("spraute_disabled", true);
            }

            return o;
        }
    }

    private void writeResult(JsonObject o) {
        if (this.type.equals("minecraft:stonecutting")) {
            o.addProperty("result", this.result);
            o.addProperty("count", this.count);
        } else if (cooking(this.type) && this.count <= 1 && this.resultNbt.isEmpty()) {
            o.addProperty("result", this.result);
        } else {
            JsonObject r = new JsonObject();
            r.addProperty("item", this.result);
            if (this.count > 1) {
                r.addProperty("count", this.count);
            }

            if (!this.resultNbt.isEmpty()) {
                r.addProperty("nbt", this.resultNbt);
            }

            o.add("result", r);
        }
    }

    private void writeShaped(JsonObject o) {
        int minR = 3;
        int maxR = -1;
        int minC = 3;
        int maxC = -1;

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                if (!this.slots[r * 3 + c].isBlank()) {
                    minR = Math.min(minR, r);
                    maxR = Math.max(maxR, r);
                    minC = Math.min(minC, c);
                    maxC = Math.max(maxC, c);
                }
            }
        }

        Map<String, Character> keys = new LinkedHashMap<>();
        JsonArray pattern = new JsonArray();

        for (int r = minR; r <= maxR; r++) {
            StringBuilder line = new StringBuilder();

            for (int cx = minC; cx <= maxC; cx++) {
                String s = this.slots[r * 3 + cx].trim();
                if (s.isEmpty()) {
                    line.append(' ');
                } else {
                    line.append(keys.computeIfAbsent(s, k -> "ABCDEFGHI".charAt(keys.size())).charValue());
                }
            }

            pattern.add(line.toString());
        }

        JsonObject key = new JsonObject();

        for (Entry<String, Character> e : keys.entrySet()) {
            key.add(String.valueOf(e.getValue()), ingredientJson(e.getKey()));
        }

        o.add("pattern", pattern);
        o.add("key", key);
    }

    public static RecipeDef fromJson(String id, String text) {
        JsonObject o = JsonParser.parseString(text).getAsJsonObject();
        RecipeDef d = new RecipeDef();
        d.id = id;
        d.type = o.has("type") ? o.get("type").getAsString() : "";
        d.disabled = o.has("spraute_disabled") && o.get("spraute_disabled").getAsBoolean();
        boolean known = TYPES.contains(d.type) && !o.has("spraute_placeholder");

        try {
            if (known) {
                d.readKnown(o);
            }
        } catch (RuntimeException var7) {
            known = false;
        }

        if (!known) {
            d.raw = o;
            d.readResult(o);
            return d;
        } else {
            for (Entry<String, JsonElement> e : o.entrySet()) {
                if (!KNOWN.contains(e.getKey())) {
                    d.extras.add(e.getKey(), e.getValue());
                }
            }

            return d;
        }
    }

    private void readKnown(JsonObject o) {
        if (o.has("group")) {
            this.group = o.get("group").getAsString();
        }

        if (this.type.equals("minecraft:crafting_shaped")) {
            this.readShaped(o);
        } else if (this.type.equals("minecraft:crafting_shapeless")) {
            JsonArray list = o.getAsJsonArray("ingredients");
            if (list.size() > 9) {
                throw new IllegalArgumentException("too many ingredients");
            }

            for (int i = 0; i < list.size(); i++) {
                this.slots[i] = specOf(list.get(i));
            }
        } else {
            this.slots[0] = specOf(o.get("ingredient"));
        }

        this.readResult(o);
        if (cooking(this.type)) {
            this.experience = o.has("experience") ? o.get("experience").getAsFloat() : 0.0F;
            this.cookingTime = o.has("cookingtime") ? o.get("cookingtime").getAsInt() : defaultTime(this.type);
        }
    }

    private void readShaped(JsonObject o) {
        JsonArray pattern = o.getAsJsonArray("pattern");
        JsonObject key = o.getAsJsonObject("key");
        if (pattern.size() > 3) {
            throw new IllegalArgumentException("pattern too tall");
        } else {
            for (int r = 0; r < pattern.size(); r++) {
                String line = pattern.get(r).getAsString();
                if (line.length() > 3) {
                    throw new IllegalArgumentException("pattern too wide");
                }

                for (int c = 0; c < line.length(); c++) {
                    char ch = line.charAt(c);
                    if (ch != ' ') {
                        this.slots[r * 3 + c] = specOf(key.get(String.valueOf(ch)));
                    }
                }
            }
        }
    }

    private void readResult(JsonObject o) {
        JsonElement r = o.get("result");
        if (r != null && !r.isJsonNull()) {
            if (r.isJsonPrimitive()) {
                this.result = r.getAsString();
                if (o.has("count")) {
                    this.count = o.get("count").getAsInt();
                }
            } else if (r.isJsonObject()) {
                JsonObject ro = r.getAsJsonObject();
                if (ro.has("item")) {
                    this.result = ro.get("item").getAsString();
                }

                if (ro.has("count")) {
                    this.count = ro.get("count").getAsInt();
                }

                if (ro.has("nbt")) {
                    this.resultNbt = ro.get("nbt").isJsonPrimitive() ? ro.get("nbt").getAsString() : ro.get("nbt").toString();
                }
            }
        }
    }
}
