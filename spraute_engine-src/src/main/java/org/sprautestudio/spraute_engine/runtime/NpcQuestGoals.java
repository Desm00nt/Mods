package org.sprautestudio.spraute_engine.runtime;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.core.script.ScriptInstance;

public final class NpcQuestGoals {
    private NpcQuestGoals() {
    }

    public static String key(String project, String character) {
        return project + "/" + character.trim().toLowerCase(Locale.ROOT);
    }

    public static void sync(Map<String, NpcQuestGoals.Wanted> wanted) {
        SprauteRuntime.env().ifPresent(env -> {
            for (ScriptInstance si : env.scriptInstances.values()) {
                if (si instanceof SprauteScriptInstance s) {
                    s.characters.forEach((name, ch) -> {
                        Entity body = (Entity)s.optional((Character<?>)ch).orElse(null);
                        if (body instanceof AnimatedEntity npc) {
                            NpcQuestGoals.Wanted w = wanted.get(key(s.deployed.project, name));
                            if (w == null) {
                                w = wanted.get(key(s.deployed.project, body.getName().getString()));
                            }

                            if (w != null && s.solo() && !w.players().contains(s.owner)) {
                                w = null;
                            }

                            if (w != null) {
                                npc.itemPickup().wantForGoal(w.filters(), w.left());
                            } else {
                                npc.itemPickup().endGoal();
                            }
                        }
                    });
                }
            }
        });
    }

    public record Wanted(List<String> filters, int left, Set<UUID> players) {
    }
}
