package org.sprautestudio.spraute_engine.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sprautestudio.spraute_engine.lang.ItemText;
import org.sprautestudio.spraute_engine.lang.ParamType;

public final class QuestGoals {
    private static final Map<String, QuestGoals.Goal> ALL = new LinkedHashMap<>();
    private static final Set<String> EVENTS;

    private QuestGoals() {
    }

    private static void goal(QuestGoals.Goal g) {
        ALL.put(g.id(), g);
    }

    public static QuestGoals.Goal get(String id) {
        return id == null ? null : ALL.get(id);
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(ALL.keySet());
        return out;
    }

    public static boolean listens(String event) {
        return EVENTS.contains(event);
    }

    public static int amount(QuestDef.Task t, String event, Map<String, Object> data) {
        QuestGoals.Goal g = get(t.goal);
        if (g == null || !g.event().equals(event)) {
            return 0;
        } else if (g.who() && !t.who.isBlank() && !isEntity(t.who, data)) {
            return 0;
        } else {
            if (!t.target.isBlank()) {
                boolean match = g.field() != null ? sameId(t.target, data.get(g.field())) : sameId(t.target, data.get("entity")) || isEntity(t.target, data);
                if (!match) {
                    return 0;
                }
            }

            return g.byCount() && data.get("count") instanceof Number n ? Math.max(1, (int)Math.round(n.doubleValue())) : 1;
        }
    }

    private static boolean sameId(String wanted, Object have) {
        return have != null && shortId(wanted).equalsIgnoreCase(shortId(have.toString()));
    }

    private static boolean isEntity(String wanted, Map<String, Object> data) {
        String w = wanted.trim().toLowerCase(Locale.ROOT);
        Object var4 = data.get("characters");
        if (var4 instanceof Collection) {
            for (Object n : (Collection)var4) {
                if (w.equals(String.valueOf(n).toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }

        return data.get("name") != null && w.equals(data.get("name").toString().toLowerCase(Locale.ROOT));
    }

    public static String shortId(String id) {
        return ItemText.id(id.trim()).replace("minecraft:", "");
    }

    public static String text(QuestDef.Task t, Function<String, String> targetName) {
        QuestGoals.Goal g = get(t.goal);
        if (t.text.isBlank() && g != null) {
            String line = t.target.isBlank() ? g.anything() : g.sentence().replace("%s", targetName.apply(t.target));
            return line.replace("%w", t.who.isBlank() ? "Персонаж" : t.who);
        } else {
            return t.text;
        }
    }

    static {
        goal(new QuestGoals.Goal("kill", "Убить", "kill", ParamType.TARGET, "Кого", null, false, false, true, "Убить: %s", "Убить кого угодно"));
        goal(
            new QuestGoals.Goal(
                "pickup", "Игрок подбирает предмет", "pickup", ParamType.ITEM, "Предмет", "item", true, false, true, "Подобрать: %s", "Подобрать что угодно"
            )
        );
        goal(
            new QuestGoals.Goal(
                "npcpickup",
                "NPC подбирает предмет",
                "npcpickup",
                ParamType.ITEM,
                "Предмет",
                "item",
                true,
                true,
                true,
                "%w подбирает: %s",
                "%w подбирает что угодно"
            )
        );
        goal(
            new QuestGoals.Goal(
                "zone", "Прийти в зону", "zoneenter", ParamType.ZONE, "Зона", "zone", false, false, false, "Прийти в зону: %s", "Прийти в любую зону"
            )
        );
        goal(
            new QuestGoals.Goal(
                "interact",
                "Поговорить с персонажем",
                "interact",
                ParamType.TARGET,
                "С кем",
                null,
                false,
                false,
                true,
                "Поговорить: %s",
                "Поговорить с кем угодно"
            )
        );
        goal(new QuestGoals.Goal("break", "Сломать блок", "break", ParamType.ITEM, "Блок", "block", false, false, true, "Сломать: %s", "Сломать любой блок"));
        goal(
            new QuestGoals.Goal(
                "craft", "Скрафтить предмет", "craft", ParamType.ITEM, "Предмет", "item", true, false, true, "Скрафтить: %s", "Скрафтить что угодно"
            )
        );

        // decompile note: Vineflower hoisted this into the field declaration, where it ran before the block above
        // filled the source collection (and produced an empty list); the original <clinit> assigns it last.
        EVENTS = ALL.values().stream().map(QuestGoals.Goal::event).collect(Collectors.toSet());
    }

    public record Goal(
        String id,
        String label,
        String event,
        ParamType target,
        String targetLabel,
        String field,
        boolean byCount,
        boolean who,
        boolean repeats,
        String sentence,
        String anything
    ) {
    }
}
