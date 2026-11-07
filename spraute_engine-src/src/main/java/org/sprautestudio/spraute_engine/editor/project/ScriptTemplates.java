package org.sprautestudio.spraute_engine.editor.project;

public final class ScriptTemplates {
    private ScriptTemplates() {
    }

    public static String newScript(int id, String title) {
        return "script %d \"%s\"\n\n// Персонажи и точки\ncharacter alice = \"spraute:npc\" name \"Алиса\"\nlocation start = (0, 64, 0)\n\nlabel begin\nspawn alice at start\nspeaker 1 alice\n// say ждёт, пока игрок нажмёт F8 (добавьте nowait, чтобы не ждать)\nsay 1 \"Привет! Это новый сюжет.\"\nlaugh alice\nwait 2s\n"
            .formatted(id, title.replace("\"", "'"));
    }

    public static String general(int id, String title) {
        return "script %d \"%s\" general\n\n// Общий скрипт работает с запуска игры, ещё до входа в мир: здесь нет мира и игрока.\n// Кнопка меню: в редакторе «Параметры движка» добавьте кнопку в главное меню и напишите её id.\n// Если ответить на кнопку самой игры, её обычное действие заменяется этим.\n\non button \"play\" {\n    menu open \"worlds\"\n}\n\non menu \"title\" {\n    log \"Открыто главное меню\"\n}\n"
            .formatted(id, title.replace("\"", "'"));
    }

    public static String demo(int id) {
        return "script %d \"Встреча у костра\"\n\n// Ctrl+Space — подсказки, наведите мышь на команду — справка.\n// Alt+P — вставить ваши текущие координаты.\n// Реплики (say) ждут нажатия F8. Модели и анимации — в проводнике, «Ассеты игры».\n\ncharacter alice = \"spraute:npc\" name \"Алиса\"\nlocation camp = (0, 64, 0)\nlocation gate = (8, 64, 4) yaw 90\nvar visits = 0\n\nlabel begin\ntitle \"Глава 1\" sub \"Костёр\"\nspawn alice at camp\nlook alice at player\nspeaker 1 alice\nsay 1 \"Привет, {playername()}!\"\nwait near alice radius 4\n\nchoice alice \"Пойдёшь со мной к воротам?\" emotion HAPPY_NEW {\n    \"Да\" {\n        say 1 \"Тогда за мной!\"\n        move alice to gate speed 1\n        wait near gate\n    }\n    \"Нет\" {\n        set visits += 1\n        say 1 \"Жаль... Подумай ещё.\"\n        wait 3s\n        if visits < 3 {\n            goto begin\n        }\n    }\n}\n\nthink \"Интересно, что будет дальше?\"\nstop\n"
            .formatted(id);
    }
}
