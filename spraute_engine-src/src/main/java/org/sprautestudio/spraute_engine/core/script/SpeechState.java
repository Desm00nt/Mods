package org.sprautestudio.spraute_engine.core.script;

import java.util.HashMap;
import java.util.Map;

public final class SpeechState {
    private final Map<Integer, SpeechState.Speaker> slots = new HashMap<>();

    public void setEntityName(int slot, String name, Integer color, String icon) {
        this.slots.put(slot, new SpeechState.Speaker(name, color, icon));
    }

    public void setEntityName(int slot, String name, String icon) {
        this.setEntityName(slot, name, null, icon);
    }

    public SpeechState.Speaker get(int slot) {
        return this.slots.getOrDefault(slot, new SpeechState.Speaker("", null, "generic_man"));
    }

    public record Speaker(String name, Integer color, String icon) {
    }
}
