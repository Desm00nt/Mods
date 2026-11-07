package org.sprautestudio.spraute_engine.camera;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class CamProfile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Map<String, CamProfile.Param> ALL = new LinkedHashMap<>();
    public static final String PLACE = "Положение";
    public static final String LOOK = "Куда смотрит";
    public static final String AXES = "Не следить по осям";
    public static final String ORBIT = "Орбита";
    public static final String MOVE = "Управление";
    public static final String ACTION = "Прицел и бег";
    public static final String VIEW = "Вид";
    public static final String WALLS = "Стены перед камерой";
    public Map<String, Double> values = new LinkedHashMap<>();
    private static final Map<String, CamProfile.Preset> PRESETS = new LinkedHashMap<>();

    private static void n(String id, String title, String group, double min, double max, double def, String doc) {
        ALL.put(id, new CamProfile.Param(id, title, group, min, max, def, false, false, doc));
    }

    private static void t(String id, String title, String group, String doc) {
        ALL.put(id, new CamProfile.Param(id, title, group, 0.0, 0.0, 0.0, true, false, doc));
    }

    private static void w(String id, String title, String group, String doc) {
        ALL.put(id, new CamProfile.Param(id, title, group, 0.0, 0.0, 0.0, false, true, doc));
    }

    public static CamProfile.Param param(String id) {
        return ALL.get(id);
    }

    public static List<CamProfile.Param> params() {
        return Collections.unmodifiableList(new ArrayList<>(ALL.values()));
    }

    public static List<String> ids() {
        return new ArrayList<>(ALL.keySet());
    }

    public static List<String> groups() {
        return List.of("Положение", "Куда смотрит", "Не следить по осям", "Орбита", "Управление", "Прицел и бег", "Вид", "Стены перед камерой");
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static CamProfile fromJson(String json) {
        CamProfile p = null;

        try {
            p = GSON.fromJson(json, CamProfile.class);
        } catch (RuntimeException var3) {
        }

        if (p == null) {
            p = new CamProfile();
        }

        if (p.values == null) {
            p.values = new LinkedHashMap<>();
        }

        p.values.keySet().removeIf(k -> ALL.get(k) == null);
        p.values.values().removeIf(Objects::isNull);
        return p;
    }

    public static String problems(String json) {
        CamProfile p;
        try {
            p = GSON.fromJson(json, CamProfile.class);
        } catch (RuntimeException var3) {
            return "Не читается: " + var3.getMessage();
        }

        if (p != null && p.values != null) {
            List<String> out = new ArrayList<>();
            p.values.forEach((id, v) -> {
                CamProfile.Param param = ALL.get(id);
                if (param == null) {
                    out.add("нет настройки " + id);
                } else if (v == null) {
                    out.add(id + ": пусто");
                } else if (!param.toggle() && !param.free() && (v < param.min() || v > param.max())) {
                    out.add(id + ": " + v + " вне " + param.min() + ".." + param.max());
                }
            });
            return String.join("; ", out);
        } else {
            return "";
        }
    }

    public CamProfile copy() {
        CamProfile p = new CamProfile();
        p.values.putAll(this.values);
        return p;
    }

    public double num(String id) {
        Double v = this.values.get(id);
        if (v != null) {
            return v;
        } else {
            CamProfile.Param p = ALL.get(id);
            return p != null ? p.def() : 0.0;
        }
    }

    public boolean on(String id) {
        return this.num(id) > 0.5;
    }

    public boolean has(String id) {
        return this.values.containsKey(id);
    }

    public static void addPreset(String name, String title, String doc, CamProfile profile) {
        PRESETS.put(name, new CamProfile.Preset(name, title, doc, profile));
    }

    private static void p(String name, String title, String doc, Object... kv) {
        CamProfile profile = new CamProfile();

        for (int i = 0; i < kv.length; i += 2) {
            profile.values.put((String)kv[i], ((Number)kv[i + 1]).doubleValue());
        }

        PRESETS.put(name, new CamProfile.Preset(name, title, doc, profile));
    }

    public static CamProfile.Preset preset(String name) {
        return PRESETS.get(name);
    }

    public static List<String> presetNames() {
        return new ArrayList<>(PRESETS.keySet());
    }

    public static List<CamProfile.Preset> presets() {
        return new ArrayList<>(PRESETS.values());
    }

    static {
        n("distance", "Дистанция", "Положение", 0.0, 64.0, 0.0, "Отодвигает камеру назад вдоль взгляда. 4 — как обычное третье лицо.");
        n("x", "Вправо", "Положение", -16.0, 16.0, 0.0, "Смещение от глаз вправо: камера через плечо.");
        n("y", "Вверх", "Положение", -16.0, 16.0, 0.0, "");
        n("z", "Назад", "Положение", -16.0, 16.0, 0.0, "");
        n("smooth", "Плавность", "Положение", 0.0, 1.0, 0.0, "Камера догоняет игрока с запаздыванием. 0 — намертво.");
        t("collide", "Не проходить сквозь блоки", "Положение", "Камера останавливается перед стеной, а не уходит в неё.");
        t("lock_yaw", "Закрепить поворот", "Куда смотрит", "Камера не поворачивается за мышью по горизонтали.");
        n("yaw", "Поворот", "Куда смотрит", -180.0, 180.0, 0.0, "С закреплением — куда смотрит в мире (0 — на юг, +Z). Без него — добавка к взгляду.");
        t("lock_pitch", "Закрепить наклон", "Куда смотрит", "Камера не наклоняется за мышью.");
        n("pitch", "Наклон", "Куда смотрит", -90.0, 90.0, 0.0, "С закреплением — наклон в мире (90 — строго вниз). Без него — добавка.");
        t("lock_x", "Не следить по X", "Не следить по осям", "По этой оси камера стоит на месте, а по остальным едет за игроком. Так делается 2D.");
        w("world_x", "Где стоять по X", "Не следить по осям", "Пусто — там, где камера была при включении.");
        t("lock_y", "Не следить по Y", "Не следить по осям", "");
        w("world_y", "Где стоять по Y", "Не следить по осям", "");
        t("lock_z", "Не следить по Z", "Не следить по осям", "");
        w("world_z", "Где стоять по Z", "Не следить по осям", "");
        t("orbit", "Орбита вокруг точки", "Орбита", "Камера висит на точке рядом с игроком и всегда смотрит на неё: игрок в центре кадра.");
        n("orbit_yaw", "Угол вокруг", "Орбита", -180.0, 180.0, 0.0, "");
        n("orbit_pitch", "Угол сверху", "Орбита", -89.0, 89.0, 0.0, "");
        n("pivot_x", "Точка: вправо", "Орбита", -16.0, 16.0, 0.0, "");
        n("pivot_y", "Точка: вверх", "Орбита", -16.0, 16.0, 0.0, "");
        n("pivot_z", "Точка: назад", "Орбита", -16.0, 16.0, 0.0, "");
        t("move_relative", "Идти относительно камеры", "Управление", "WASD считаются от взгляда камеры, а не от поворота тела.");
        t("separate_turn", "Тело поворачивается отдельно", "Управление", "Мышь крутит камеру, тело смотрит туда, куда идёт.");
        n("turn_smooth", "Плавность поворота тела", "Управление", 0.0, 1.0, 0.5, "0 — сразу, 1 — медленно.");
        t("plane_move", "Только влево-вправо", "Управление", "W и S не работают: ходьба в одной плоскости, как в 2D.");
        n(
            "auto_center",
            "Камера возвращается за спину",
            "Управление",
            0.0,
            2.0,
            0.0,
            "Как в GTA: идёшь, мышь не трогаешь — камера сама заходит за спину. Нужен отдельный поворот тела."
        );
        t("look_at_target", "Смотреть на то, что делаешь", "Управление", "Пока игрок бьёт, ломает или использует, тело смотрит на цель.");
        t("screen_aim", "Прицел по центру экрана", "Прицел и бег", "Удары, блоки и использование идут туда, куда смотрит камера, а не голова.");
        t("aim", "Прицеливание на ПКМ", "Прицел и бег", "Пока зажата кнопка использования: камера подходит к плечу, тело смотрит вперёд и ходит боком.");
        n("aim_distance", "Дистанция в прицеле", "Прицел и бег", 0.0, 16.0, 1.6, "");
        n("aim_x", "Плечо в прицеле", "Прицел и бег", -4.0, 4.0, 0.6, "");
        n("aim_fov", "Обзор в прицеле", "Прицел и бег", 0.0, 170.0, 0.0, "0 — не менять.");
        n("run_distance", "Отъезд при беге", "Прицел и бег", -8.0, 16.0, 0.0, "На сколько блоков камера отстаёт на бегу.");
        n("run_fov", "Обзор при беге", "Прицел и бег", -40.0, 60.0, 0.0, "Добавка к обзору на бегу.");
        n("fov", "Угол обзора", "Вид", 0.0, 170.0, 0.0, "0 — как в настройках игры.");
        t("show_hand", "Показывать руку", "Вид", "Рука видна, даже если камера отодвинута.");
        t("hide_crosshair", "Скрыть прицел", "Вид", "");
        n(
            "walk_bob",
            "Ход головы",
            "Вид",
            0.0,
            2.0,
            0.0,
            "Покачивание при ходьбе и удар при приземлении, на самой камере. Обычное покачивание игры при этом выключено."
        );
        t("no_bob", "Без покачивания игры", "Вид", "");
        t("no_hurt_shake", "Без тряски от урона", "Вид", "");
        t(
            "hide_blocks",
            "Убирать блоки перед игроком",
            "Стены перед камерой",
            "Только на экране этого игрока: стены между камерой и игроком пропадают. На сервере они остаются: сквозь них не пройти, сервер вернёт игрока назад рывком."
        );
        n("hide_alpha", "Призрак блока", "Стены перед камерой", 0.0, 1.0, 0.25, "0 — совсем не видно, 1 — плотный силуэт.");
        n("hide_radius", "Ширина прохода", "Стены перед камерой", 0.0, 8.0, 1.0, "В блоках вокруг линии камера — игрок.");
        p("first_person", "От первого лица", "Обычная камера игры.");
        p("third_person", "Третье лицо", "За спиной, как F5.", "distance", 4, "collide", 1);
        p(
            "shoulder",
            "Через плечо",
            "Камера у правого плеча, прицел по экрану.",
            "distance",
            2.6,
            "x",
            0.8,
            "y",
            0.15,
            "collide",
            1,
            "smooth",
            0.25,
            "screen_aim",
            1
        );
        p(
            "action",
            "Боевик",
            "Мышь крутит камеру вокруг героя, он бежит туда, куда идёт.",
            "distance",
            4.5,
            "y",
            0.4,
            "collide",
            1,
            "separate_turn",
            1,
            "move_relative",
            1,
            "smooth",
            0.3,
            "turn_smooth",
            0.4,
            "screen_aim",
            1,
            "look_at_target",
            1
        );
        p(
            "gta",
            "Как в GTA",
            "Камера вокруг героя, сама заходит за спину на ходу, на бегу отстаёт, на ПКМ — прицел через плечо.",
            "distance",
            3.8,
            "y",
            0.45,
            "collide",
            1,
            "smooth",
            0.25,
            "separate_turn",
            1,
            "move_relative",
            1,
            "turn_smooth",
            0.35,
            "auto_center",
            0.5,
            "screen_aim",
            1,
            "look_at_target",
            1,
            "aim",
            1,
            "aim_distance",
            1.5,
            "aim_x",
            0.65,
            "aim_fov",
            55,
            "run_distance",
            1.2,
            "run_fov",
            8
        );
        p(
            "over_the_shoulder",
            "Шутер от третьего лица",
            "Камера всегда у плеча, тело поворачивается мышью, как в Resident Evil 4.",
            "distance",
            1.8,
            "x",
            0.65,
            "y",
            0.1,
            "collide",
            1,
            "smooth",
            0.15,
            "screen_aim",
            1,
            "aim",
            1,
            "aim_distance",
            1.1,
            "aim_x",
            0.6,
            "aim_fov",
            50,
            "run_fov",
            6,
            "no_bob",
            1
        );
        p(
            "souls",
            "Как в Dark Souls",
            "Просторная камера, плавно за спиной, тело разворачивается к цели удара.",
            "distance",
            5,
            "y",
            0.6,
            "collide",
            1,
            "smooth",
            0.35,
            "separate_turn",
            1,
            "move_relative",
            1,
            "turn_smooth",
            0.5,
            "auto_center",
            0.25,
            "screen_aim",
            1,
            "look_at_target",
            1,
            "hide_blocks",
            1,
            "hide_alpha",
            0.2
        );
        p(
            "platformer_3d",
            "3D-платформер",
            "Камера высоко сзади, как в Mario: идёшь куда жмёшь, она догоняет.",
            "distance",
            8,
            "pitch",
            22,
            "lock_pitch",
            1,
            "collide",
            1,
            "smooth",
            0.45,
            "separate_turn",
            1,
            "move_relative",
            1,
            "turn_smooth",
            0.25,
            "auto_center",
            0.8,
            "hide_crosshair",
            1
        );
        p(
            "first_person_plus",
            "От первого лица с ходом головы",
            "Как в современных шутерах: шаги и приземление чувствуются.",
            "walk_bob",
            1,
            "no_hurt_shake",
            1
        );
        p(
            "top_down",
            "Сверху вниз",
            "Вид строго сверху: герой идёт куда жмёшь.",
            "distance",
            16,
            "lock_yaw",
            1,
            "lock_pitch",
            1,
            "pitch",
            89,
            "move_relative",
            1,
            "smooth",
            0.4,
            "turn_smooth",
            0.2,
            "hide_crosshair",
            1,
            "hide_blocks",
            1,
            "look_at_target",
            1
        );
        p(
            "isometric",
            "Изометрия",
            "Вид под углом, поворот закреплён.",
            "distance",
            22,
            "lock_yaw",
            1,
            "yaw",
            45,
            "lock_pitch",
            1,
            "pitch",
            35,
            "fov",
            30,
            "move_relative",
            1,
            "smooth",
            0.45,
            "turn_smooth",
            0.2,
            "hide_crosshair",
            1,
            "hide_blocks",
            1,
            "look_at_target",
            1
        );
        p(
            "rpg",
            "Сверху под углом",
            "Как в классических ролевых играх, поворот мышью сохраняется.",
            "distance",
            12,
            "lock_pitch",
            1,
            "pitch",
            50,
            "collide",
            1,
            "smooth",
            0.3,
            "separate_turn",
            1,
            "move_relative",
            1
        );
        p(
            "side_x",
            "2D: идём вдоль X",
            "Вид сбоку: камера не приближается и не отдаляется, ходьба только влево-вправо.",
            "distance",
            14,
            "lock_yaw",
            1,
            "yaw",
            0,
            "lock_pitch",
            1,
            "pitch",
            8,
            "lock_z",
            1,
            "fov",
            45,
            "move_relative",
            1,
            "plane_move",
            1,
            "smooth",
            0.35,
            "turn_smooth",
            0,
            "hide_crosshair",
            1
        );
        p(
            "side_z",
            "2D: идём вдоль Z",
            "То же, но уровень вытянут вдоль Z.",
            "distance",
            14,
            "lock_yaw",
            1,
            "yaw",
            90,
            "lock_pitch",
            1,
            "pitch",
            8,
            "lock_x",
            1,
            "fov",
            45,
            "move_relative",
            1,
            "plane_move",
            1,
            "smooth",
            0.35,
            "turn_smooth",
            0,
            "hide_crosshair",
            1
        );
        p(
            "side_flat",
            "2D без высоты",
            "Вид сбоку, камера не следит и за высотой: платформер на одном экране по вертикали.",
            "distance",
            18,
            "lock_yaw",
            1,
            "lock_pitch",
            1,
            "lock_z",
            1,
            "lock_y",
            1,
            "fov",
            40,
            "move_relative",
            1,
            "plane_move",
            1,
            "smooth",
            0.35,
            "turn_smooth",
            0,
            "hide_crosshair",
            1
        );
        p(
            "rail_x",
            "Едет за игроком вдоль X",
            "Камера стоит и катится по одной оси, а мышью можно смотреть.",
            "distance",
            8,
            "lock_y",
            1,
            "lock_z",
            1,
            "move_relative",
            1
        );
        p(
            "fixed",
            "Камера наблюдения",
            "Стоит на месте и не следит ни по одной оси, смотрит в заданную сторону.",
            "lock_x",
            1,
            "lock_y",
            1,
            "lock_z",
            1,
            "lock_yaw",
            1,
            "lock_pitch",
            1,
            "pitch",
            25,
            "move_relative",
            1,
            "hide_crosshair",
            1
        );
    }

    public record Param(String id, String title, String group, double min, double max, double def, boolean toggle, boolean free, String doc) {
    }

    public record Preset(String name, String title, String doc, CamProfile profile) {
    }
}
