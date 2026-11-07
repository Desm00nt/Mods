package org.sprautestudio.spraute_engine.player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerRuleDefs {
    private static final Map<String, PlayerRuleDefs.Rule> ALL = new LinkedHashMap<>();

    private PlayerRuleDefs() {
    }

    private static void can(String name, String title, String doc) {
        ALL.put(name, new PlayerRuleDefs.Rule(name, title, doc, true, 1.0, 0.0, 1.0));
    }

    private static void num(String name, String title, String doc, double def, double min, double max) {
        ALL.put(name, new PlayerRuleDefs.Rule(name, title, doc, false, def, min, max));
    }

    public static void addSwitch(String name, String title, String doc) {
        can(name, title, doc);
    }

    public static void addNumber(String name, String title, String doc, double def, double min, double max) {
        num(name, title, doc, def, min, max);
    }

    public static PlayerRuleDefs.Rule get(String name) {
        return name == null ? null : ALL.get(name.trim());
    }

    public static List<PlayerRuleDefs.Rule> all() {
        return new ArrayList<>(ALL.values());
    }

    public static List<String> names() {
        return new ArrayList<>(ALL.keySet());
    }

    public static List<String> switches() {
        List<String> out = new ArrayList<>();

        for (PlayerRuleDefs.Rule r : ALL.values()) {
            if (r.isSwitch()) {
                out.add(r.name());
            }
        }

        return out;
    }

    public static List<String> numbers() {
        List<String> out = new ArrayList<>();

        for (PlayerRuleDefs.Rule r : ALL.values()) {
            if (!r.isSwitch()) {
                out.add(r.name());
            }
        }

        return out;
    }

    public static double clamp(PlayerRuleDefs.Rule r, double value) {
        return Math.max(r.min(), Math.min(r.max(), value));
    }

    static {
        can("move", "Ходить", "Шаги в стороны. Прыжок запрещается отдельно: jump.");
        can("jump", "Прыгать", "Прыжок с земли.");
        can("sprint", "Бегать", "Бег.");
        can("sneak", "Красться", "Присед.");
        can("look", "Вращать камерой", "Мышь не поворачивает игрока.");
        can("climb", "Лазить", "Лестницы, лианы и строительные леса не держат.");
        can("elytra", "Лететь на элитрах", "Элитры не раскрываются.");
        can("ride", "Садиться верхом", "Лошади, лодки, вагонетки.");
        can("sleep", "Спать", "Кровать не укладывает.");
        can("break", "Ломать блоки", "Блок не поддаётся.");
        can("place", "Ставить блоки", "Предмет-блок не ставится.");
        can("interact", "Трогать блоки", "Двери, рычаги, кнопки, сундуки и верстаки не реагируют на ПКМ.");
        can("containers", "Открывать хранилища", "Сундуки, печи, верстаки не открывают окно. Остальные блоки работают.");
        can("use_item", "Использовать предметы", "ПКМ предметом в руке: еда, лук, вёдра, инструменты по блокам.");
        can("eat", "Есть", "Еда не съедается, остальные предметы работают.");
        can("attack", "Бить", "Удары по существам.");
        can("interact_entity", "Трогать существ", "ПКМ по существам и NPC.");
        can("drop", "Выбрасывать предметы", "Клавиша выброса и выброс из окна инвентаря.");
        can("pickup", "Подбирать предметы", "Лежащие предметы не подбираются.");
        can("pickup_xp", "Подбирать опыт", "Шары опыта не подбираются.");
        can("inventory", "Открывать инвентарь", "Клавиша инвентаря не работает.");
        can("swap_hands", "Менять руки", "Клавиша смены рук не работает.");
        can("hotbar", "Менять слот", "Выбранный слот хотбара закреплён.");
        can("take_damage", "Получать урон", "Запрет: никакой урон не проходит.");
        can("drown", "Тонуть", "Запрет: вода не вредит.");
        can("burn", "Гореть", "Запрет: огонь и лава не вредят.");
        ALL.put(
            "fly",
            new PlayerRuleDefs.Rule(
                "fly", "Летать", "Разрешение: двойной пробел включает полёт, как в творческом режиме. По умолчанию запрещено.", true, 0.0, 0.0, 1.0
            )
        );
        num("move_speed", "Скорость ходьбы", "Множитель: 1 — обычная, 2 — вдвое быстрее, 0 — стоит на месте.", 1.0, 0.0, 20.0);
        num("jump_power", "Сила прыжка", "Множитель: 2 — примерно вчетверо выше.", 1.0, 0.0, 10.0);
        num("mine_speed", "Скорость кирки", "Множитель скорости добычи: 3 — втрое быстрее, 0.2 — еле-еле.", 1.0, 0.0, 1000.0);
        num("fly_speed", "Скорость полёта", "Множитель полёта: 1 — обычная.", 1.0, 0.0, 20.0);
        num(
            "gravity",
            "Гравитация",
            "Множитель падения и прыжка: 0.3 — как на Луне, 2 — тяжелее, 0 — парит. На выделенном сервере малой гравитации нужен allow-flight=true.",
            1.0,
            0.0,
            5.0
        );
        num("step_height", "Высота шага", "На сколько блоков игрок заходит без прыжка: обычно 0.6.", 0.6, 0.0, 5.0);
        num("reach", "Дальность руки", "На сколько блоков игрок дотягивается до блоков: обычно 4.5.", 4.5, 0.0, 128.0);
        num("attack_reach", "Дальность удара", "На сколько блоков игрок достаёт до существ: обычно 3.", 3.0, 0.0, 128.0);
        num("attack_damage", "Сила удара", "Множитель урона от ударов: 2 — вдвое больше.", 1.0, 0.0, 100.0);
        num("attack_speed", "Скорость ударов", "Множитель скорости: 2 — перезарядка удара вдвое короче.", 1.0, 0.0, 100.0);
        num("attack_knockback", "Отбрасывание ударом", "Прибавка к отбрасыванию: 0 — как обычно, 2 — далеко.", 0.0, 0.0, 5.0);
        num("knockback_resist", "Стойкость к отбрасыванию", "От 0 до 1: 1 — удары не сдвигают игрока.", 0.0, 0.0, 1.0);
        num("max_health", "Максимум здоровья", "Очки здоровья: 20 — десять сердец.", 20.0, 1.0, 1024.0);
        num("armor", "Броня", "Прибавка к броне: 20 — вся полоска.", 0.0, 0.0, 30.0);
        num("armor_toughness", "Прочность брони", "Прибавка к прочности брони.", 0.0, 0.0, 20.0);
        num("luck", "Удача", "Прибавка к удаче: сундуки и рыбалка.", 0.0, -1024.0, 1024.0);
        num("damage_taken", "Получаемый урон", "Множитель: 0.5 — вдвое меньше, 2 — вдвое больше.", 1.0, 0.0, 100.0);
        num("fall_damage", "Урон от падения", "Множитель: 0 — не разбивается.", 1.0, 0.0, 100.0);
        num("hunger_speed", "Скорость голода", "Множитель: 0 — голод не растёт, 2 — вдвое быстрее.", 1.0, 0.0, 100.0);
        num("xp_gain", "Получаемый опыт", "Множитель: 2 — вдвое больше.", 1.0, 0.0, 100.0);
    }

    public record Rule(String name, String title, String doc, boolean isSwitch, double def, double min, double max) {
    }
}
