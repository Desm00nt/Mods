package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.lang.Ast;

final class EventLog {
    private static final int KEEP = 128;
    private final ArrayDeque<EventLog.Entry> recent = new ArrayDeque<>();
    private long seq;

    long seq() {
        return this.seq;
    }

    void add(String type, Map<String, Object> data, SprauteScriptInstance s) {
        Map<String, Object> d = new LinkedHashMap<>(data);
        if (d.remove("__entity") instanceof Entity e) {
            d.put("ref", new EntityRef(e));
            s.characters.forEach((name, ch) -> {
                Entity ce = (Entity)s.optional((Character<?>)ch).orElse(null);
                if (ce == e) {
                    d.put("character", Ast.Program.copyOf(name));
                }
            });
        }

        d.putIfAbsent("character", "");
        if (d.remove("__killer") instanceof Entity ke) {
            d.put("killerref", new EntityRef(ke));
            s.characters.forEach((name, ch) -> {
                Entity ce = (Entity)s.optional((Character<?>)ch).orElse(null);
                if (ce == ke) {
                    d.put("by", Ast.Program.copyOf(name));
                }
            });
        }

        d.put("type", type);
        this.recent.addLast(new EventLog.Entry(++this.seq, type, d));

        while (this.recent.size() > 128) {
            this.recent.removeFirst();
        }
    }

    EventLog.Entry find(String type, long after, Predicate<Map<String, Object>> filter) {
        for (EventLog.Entry e : this.recent) {
            if (e.seq > after && e.type.equals(type) && filter.test(e.data)) {
                return e;
            }
        }

        return null;
    }

    record Entry(long seq, String type, Map<String, Object> data) {
    }
}
