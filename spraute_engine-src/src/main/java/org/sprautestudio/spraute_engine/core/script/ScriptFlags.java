package org.sprautestudio.spraute_engine.core.script;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;

public final class ScriptFlags {
    private final Map<String, String> flags = new HashMap<>();

    public boolean has(String name) {
        return this.flags.containsKey(name);
    }

    public String get(String name) {
        return this.flags.get(name);
    }

    public void set(String name) {
        this.flags.put(name, "");
    }

    public void set(String name, String value) {
        this.flags.put(name, value);
    }

    public boolean consume(String name) {
        return this.flags.remove(name) != null;
    }

    public void remove(String name) {
        this.flags.remove(name);
    }

    CompoundTag save() {
        CompoundTag t = new CompoundTag();
        this.flags.forEach(t::putString);
        return t;
    }

    void load(CompoundTag t) {
        this.flags.clear();

        for (String k : t.getAllKeys()) {
            this.flags.put(k, t.getString(k));
        }
    }
}
