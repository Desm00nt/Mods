package org.sprautestudio.spraute_engine.custom;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;

public final class TabDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String id = "";
    public String name = "";
    public String icon = "minecraft:chest";
    public List<String> items = new ArrayList<>();

    public static TabDef create(String id, String name) {
        TabDef t = new TabDef();
        t.id = id;
        t.name = name;
        return t;
    }

    public String key() {
        return "custom:" + this.id;
    }

    public String problem() {
        if (!ItemDef.validId(this.id)) {
            return "Id: только латиница, цифры и _";
        } else {
            return this.name.isBlank() ? "Нет названия" : null;
        }
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static TabDef fromJson(String text) {
        TabDef t = GSON.fromJson(text, TabDef.class);
        if (t == null) {
            throw new IllegalArgumentException("empty tab");
        } else {
            return t;
        }
    }

    public TabDef copy(String newId) {
        TabDef t = fromJson(this.toJson());
        t.id = newId;
        return t;
    }
}
