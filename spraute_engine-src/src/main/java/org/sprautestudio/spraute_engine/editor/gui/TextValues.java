package org.sprautestudio.spraute_engine.editor.gui;

import java.io.IOException;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class TextValues {
    private TextValues() {
    }

    public static boolean takes(String key) {
        return key.equals("text") || key.equals("label") || key.equals("tooltip");
    }

    public static void attach(TextField field, Project project, String role, String key) {
        field.contextMenu = pm -> {
            if (key.equals("label")) {
                pm.item("Сколько процентов", null, "{p}", () -> field.insert("{p}"));
                pm.separator();
            }

            String[][] own = ofWindow(role);
            if (own.length > 0) {
                for (String[] v : own) {
                    value(pm, field, v[0], v[1]);
                }

                pm.separator();
            }

            pm.submenu("Игрок", Icon.USER, rows -> {
                value(rows, field, "Имя", "player");
                value(rows, field, "Здоровье", "hp");
                value(rows, field, "Здоровье, максимум", "maxhp");
                value(rows, field, "Еда", "food");
                value(rows, field, "Броня", "armor");
                value(rows, field, "Воздух", "air");
                value(rows, field, "Уровень опыта Minecraft", "level");
                value(rows, field, "Опыт Minecraft, %", "exp");
                value(rows, field, "Монеты", "coins");
            });
            pm.submenu("Мир", Icon.PIN, rows -> {
                value(rows, field, "Координата X", "x");
                value(rows, field, "Координата Y", "y");
                value(rows, field, "Координата Z", "z");
                value(rows, field, "Время суток", "time");
            });
            if (project != null) {
            }
        };
    }

    private static void system(PopupMenu rows, TextField field, Project project, Project.FileRef f) {
        String head = "skill:" + f.name() + ":";
        value(rows, field, "Уровень", head + "level");
        value(rows, field, "Опыт на этом уровне", head + "xp");
        value(rows, field, "Опыта до следующего", head + "xpnext");
        value(rows, field, "Последний уровень", head + "maxlevel");

        SkillTree tree;
        try {
            tree = SkillTree.fromJson(project.read(f));
        } catch (IOException var8) {
            return;
        }

        value(rows, field, "Название", head + "title");

        for (SkillTree.Currency c : tree.currencies) {
            value(rows, field, c.name, head + "points:" + c.id);
        }

        if (!tree.nodes.isEmpty()) {
            rows.submenu("Ранг навыка", Icon.LIST, nodes -> {
                for (SkillTree.Node n : tree.nodes) {
                    value(nodes, field, n.title, head + "rank:" + n.id);
                }
            });
        }
    }

    private static void value(PopupMenu pm, TextField field, String title, String name) {
        String token = "{" + name + "}";
        pm.item(title, null, name.startsWith("skill:") ? null : token, () -> field.insert(token));
    }

    private static String[][] ofWindow(String role) {
        if (role == null) {
            return new String[0][];
        } else {
            return switch (role) {
                case "choice" -> new String[][]{{"Кто спрашивает", "name"}, {"Вопрос", "question"}};
                case "dialog" -> new String[][]{{"Кто говорит", "name"}, {"Реплика", "text"}, {"Подсказка «▸ F8»", "hint"}};
                case "toast" -> new String[][]{{"Заголовок", "title"}, {"Задача или квест", "text"}};
                case "zone" -> new String[][]{{"Название зоны", "zone"}, {"Подпись", "subtitle"}};
                case "quest" -> new String[][]{
                    {"Вкладка", "tab"},
                    {"Сколько квестов", "count"},
                    {"Квест", "quest"},
                    {"Кто дал", "giver"},
                    {"Итог", "state"},
                    {"Подсказка закрытия", "close"}
                };
                case "shop" -> new String[][]{{"Название магазина", "title"}, {"Деньги игрока", "balance"}, {"Как зовут деньги", "currency"}};
                case "skills", "skills2", "skills3" -> new String[][]{
                    {"Название системы", "title"},
                    {"Уровень системы", "level"},
                    {"Опыт", "xp"},
                    {"Опыта до следующего", "xpnext"},
                    {"Очки", "points"},
                    {"Выбранный навык", "node"}
                };
                default -> new String[0][];
            };
        }
    }
}
