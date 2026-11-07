package org.sprautestudio.spraute_engine.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QuestDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String title = "Новый квест";
    public String giver = "";
    public String description = "";
    public List<QuestDef.Task> tasks = new ArrayList<>();
    public boolean sequential;
    public boolean autoComplete = true;
    public QuestDef.Reward reward = new QuestDef.Reward();
    public List<QuestDef.Section> layout;

    public static List<QuestDef.Section> defaultLayout() {
        List<QuestDef.Section> l = new ArrayList<>();
        l.add(new QuestDef.Section("description"));
        l.add(new QuestDef.Section("tasks"));
        l.add(new QuestDef.Section("reward"));
        return l;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static QuestDef fromJson(String json) {
        QuestDef d = null;

        try {
            d = GSON.fromJson(json, QuestDef.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new QuestDef();
        }

        d.fix();
        return d;
    }

    public void fix() {
        if (this.title == null) {
            this.title = "";
        }

        if (this.giver == null) {
            this.giver = "";
        }

        if (this.description == null) {
            this.description = "";
        }

        if (this.tasks == null) {
            this.tasks = new ArrayList<>();
        }

        if (this.reward == null) {
            this.reward = new QuestDef.Reward();
        }

        if (this.reward.items == null) {
            this.reward.items = new ArrayList<>();
        }

        if (this.reward.text == null) {
            this.reward.text = "";
        }

        if (this.layout == null) {
            this.layout = defaultLayout();
        }

        this.layout.removeIf(Objects::isNull);

        for (QuestDef.Section s : this.layout) {
            if (s.kind == null) {
                s.kind = "text";
            }

            if (s.header == null) {
                s.header = "";
            }

            if (s.text == null) {
                s.text = "";
            }

            if (s.color == null) {
                s.color = "";
            }
        }

        for (QuestDef.Task t : this.tasks) {
            if (t.id == null || t.id.isBlank()) {
                t.id = "task";
            }

            if (t.text == null) {
                t.text = "";
            }

            if (t.goal == null) {
                t.goal = "";
            }

            if (t.target == null) {
                t.target = "";
            }

            if (t.who == null) {
                t.who = "";
            }

            QuestGoals.Goal goal = QuestGoals.get(t.goal);
            t.count = goal != null && !goal.repeats() ? 1 : Math.max(1, t.count);
        }
    }

    public QuestDef.Task task(String id) {
        for (QuestDef.Task t : this.tasks) {
            if (t.id.equals(id)) {
                return t;
            }
        }

        return null;
    }

    public static QuestDef example(String title) {
        QuestDef d = new QuestDef();
        d.layout = defaultLayout();
        d.title = title;
        d.giver = "Старейшина";
        d.description = "Деревню каждую ночь тревожат зомби. Старейшина просит помочь: разобраться с ними и укрепить ворота.";
        d.tasks.add(task("talk", "Поговорить со старейшиной", 1, false, false));
        d.tasks.add(task("zombies", "Убить зомби у деревни", 5, false, false));
        d.tasks.add(task("planks", "Принести доски для ворот", 16, false, false));
        d.tasks.add(task("secret", "Найти, откуда они приходят", 1, true, true));
        d.reward.coins = 50;
        d.reward.xp = 30;
        d.reward.items.add(new QuestDef.Item("minecraft:iron_sword", 1));
        return d;
    }

    private static QuestDef.Task task(String id, String text, int count, boolean optional, boolean hidden) {
        QuestDef.Task t = new QuestDef.Task();
        t.id = id;
        t.text = text;
        t.count = count;
        t.optional = optional;
        t.hidden = hidden;
        return t;
    }

    public static class Item {
        public String item = "minecraft:emerald";
        public int count = 1;

        public Item() {
        }

        public Item(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    public static class Reward {
        public int coins;
        public int xp;
        public List<QuestDef.Item> items = new ArrayList<>();
        public String text = "";
    }

    public static class Section {
        public String kind = "text";
        public String header = "";
        public String text = "";
        public String color = "";

        public Section() {
        }

        public Section(String kind) {
            this.kind = kind;
        }
    }

    public static class Task {
        public String id = "task";
        public String text = "Сделать что-то";
        public int count = 1;
        public boolean optional;
        public boolean hidden;
        public String goal = "";
        public String target = "";
        public String who = "";
    }
}
