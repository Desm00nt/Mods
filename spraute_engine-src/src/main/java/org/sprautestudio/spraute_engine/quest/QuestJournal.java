package org.sprautestudio.spraute_engine.quest;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.sprautestudio.spraute_engine.lang.ItemText;

public class QuestJournal {
    private static final Gson GSON = new Gson();
    public List<QuestJournal.Entry> quests = new ArrayList<>();
    public String tracked = "";
    public String toast = "";
    public String toastKind = "";

    public QuestJournal.Entry entry(String key) {
        for (QuestJournal.Entry e : this.quests) {
            if (e.key.equals(key)) {
                return e;
            }
        }

        return null;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static QuestJournal fromJson(String json) {
        QuestJournal j = null;

        try {
            j = GSON.fromJson(json, QuestJournal.class);
        } catch (Exception var3) {
        }

        return j != null ? j : new QuestJournal();
    }

    public static String rewardText(QuestDef.Reward r, Function<QuestDef.Item, String> itemName) {
        if (!r.text.isEmpty()) {
            return r.text;
        } else {
            List<String> parts = new ArrayList<>();
            if (r.coins > 0) {
                parts.add(r.coins + " монет");
            }

            if (r.xp > 0) {
                parts.add(r.xp + " опыта");
            }

            for (QuestDef.Item i : r.items) {
                parts.add(itemName.apply(i) + (i.count > 1 ? " ×" + i.count : ""));
            }

            return String.join(", ", parts);
        }
    }

    public static QuestJournal preview(QuestDef d, String key) {
        QuestJournal j = new QuestJournal();
        QuestJournal.Entry e = new QuestJournal.Entry();
        e.key = key;
        e.title = d.title;
        e.giver = d.giver;
        e.description = d.description;
        e.reward = rewardText(d.reward, i -> ItemText.id(i.item).replace("minecraft:", ""));
        e.layout = d.layout;
        boolean before = true;

        for (QuestDef.Task t : d.tasks) {
            if (!t.hidden) {
                QuestJournal.Task x = new QuestJournal.Task();
                x.id = t.id;
                x.text = QuestGoals.text(t, QuestGoals::shortId);
                x.count = t.count;
                x.optional = t.optional;
                x.progress = e.tasks.isEmpty() ? t.count : 0;
                if (d.sequential && !before) {
                    break;
                }

                if (!x.done()) {
                    before = false;
                }

                e.tasks.add(x);
            }
        }

        j.quests.add(e);
        QuestJournal.Entry done = new QuestJournal.Entry();
        done.key = "example/done";
        done.title = "Первые шаги";
        done.giver = "";
        done.description = "Пример выполненного квеста.";
        done.reward = "";
        done.state = "completed";
        j.quests.add(done);
        j.tracked = key;
        return j;
    }

    public static class Entry {
        public String key;
        public String title;
        public String giver;
        public String description;
        public String reward;
        public String state = "active";
        public long order;
        public List<QuestJournal.Task> tasks = new ArrayList<>();
        public List<QuestDef.Section> layout;
    }

    public static class Task {
        public String id;
        public String text;
        public int count = 1;
        public int progress;
        public boolean optional;

        public boolean done() {
            return this.progress >= this.count;
        }
    }
}
