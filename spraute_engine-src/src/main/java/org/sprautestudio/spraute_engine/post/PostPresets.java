package org.sprautestudio.spraute_engine.post;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PostPresets {
    private static final Map<String, PostPresets.Preset> ALL = new LinkedHashMap<>();

    private PostPresets() {
    }

    public static void add(String group, String name, String title, PostProfile profile) {
        ALL.put(name, new PostPresets.Preset(name, title, group, profile));
    }

    private static void p(String group, String name, String title, Object... kv) {
        PostProfile profile = new PostProfile();

        for (int i = 0; i < kv.length; i += 2) {
            profile.values.put((String)kv[i], String.valueOf(kv[i + 1]));
        }

        ALL.put(name, new PostPresets.Preset(name, title, group, profile));
    }

    public static PostPresets.Preset get(String name) {
        return ALL.get(name);
    }

    public static List<String> names() {
        return new ArrayList<>(ALL.keySet());
    }

    public static List<PostPresets.Preset> all() {
        return new ArrayList<>(ALL.values());
    }

    static {
        String look = "Настроение";
        p(
            look,
            "cinematic",
            "Кино",
            "tonemap",
            1,
            "contrast",
            1.1,
            "saturation",
            0.95,
            "vignette",
            0.5,
            "bloom",
            0.5,
            "grain",
            0.08,
            "shadow_amount",
            0.25,
            "shadow_color",
            "#1C3F52",
            "highlight_amount",
            0.2,
            "highlight_color",
            "#FFB877",
            "letterbox",
            0.1,
            "ao",
            1,
            "chromatic",
            0.15
        );
        p(look, "vivid", "Яркий мир", "saturation", 1.25, "vibrance", 0.5, "contrast", 1.08, "bloom", 0.5, "ao", 1, "sharpen", 0.4);
        p(look, "soft", "Мягкий свет", "bloom", 0.9, "bloom_threshold", 0.55, "contrast", 0.95, "saturation", 1.05, "vignette", 0.25, "temperature", 0.08);
        p(
            look,
            "golden_hour",
            "Золотой час",
            "temperature",
            0.45,
            "bloom",
            1,
            "bloom_threshold",
            0.55,
            "god_rays",
            1.4,
            "lens_flare",
            0.7,
            "saturation",
            1.1,
            "highlight_amount",
            0.4,
            "highlight_color",
            "#FFB25B",
            "tonemap",
            0.6,
            "vignette",
            0.35
        );
        p(
            look,
            "dawn",
            "Рассвет",
            "temperature",
            0.2,
            "tint",
            0.15,
            "fog",
            0.4,
            "fog_start",
            16,
            "fog_end",
            96,
            "fog_color",
            "#F2C6B4",
            "bloom",
            0.7,
            "god_rays",
            1
        );
        p(
            look,
            "moonlight",
            "Лунная ночь",
            "temperature",
            -0.5,
            "saturation",
            0.6,
            "exposure",
            -0.4,
            "contrast",
            1.1,
            "bloom",
            0.7,
            "shadow_amount",
            0.5,
            "shadow_color",
            "#16244D",
            "vignette",
            0.6
        );
        p(
            look,
            "storm",
            "Гроза",
            "saturation",
            0.55,
            "contrast",
            1.2,
            "exposure",
            -0.4,
            "temperature",
            -0.2,
            "fog",
            0.5,
            "fog_start",
            12,
            "fog_end",
            80,
            "fog_color",
            "#4D5560",
            "rain_lens",
            0.6,
            "vignette",
            0.6
        );
        p(look, "noir", "Нуар", "grayscale", 1, "contrast", 1.45, "vignette", 0.9, "grain", 0.25, "tonemap", 0.5);
        p(look, "black_white", "Чёрно-белое", "grayscale", 1, "contrast", 1.1);
        p(look, "sepia", "Старое фото", "sepia", 1, "vignette", 0.8, "grain", 0.3, "contrast", 0.92, "flicker", 0.1);
        p(
            look,
            "memory",
            "Воспоминание",
            "sepia",
            0.45,
            "blur",
            0.1,
            "bloom",
            1.4,
            "bloom_threshold",
            0.45,
            "vignette",
            0.8,
            "vignette_color",
            "#2A1B10",
            "dof",
            0.5,
            "grain",
            0.15
        );
        p(
            look,
            "dream",
            "Сон",
            "bloom",
            2,
            "bloom_threshold",
            0.4,
            "saturation",
            1.2,
            "hue",
            12,
            "wave",
            0.12,
            "wave_speed",
            0.4,
            "vignette",
            0.5,
            "vignette_color",
            "#3A2A6A",
            "chromatic",
            0.3,
            "blur",
            0.06
        );
        p(look, "miniature", "Макет", "tilt_shift", 1, "saturation", 1.35, "contrast", 1.1, "sharpen", 0.3);
        String mood = "Опасность и состояние";
        p(
            mood,
            "horror",
            "Ужас",
            "saturation",
            0.35,
            "contrast",
            1.3,
            "exposure",
            -0.6,
            "vignette",
            1.3,
            "grain",
            0.35,
            "chromatic",
            0.4,
            "flicker",
            0.2,
            "fog",
            0.7,
            "fog_start",
            4,
            "fog_end",
            36,
            "fog_color",
            "#0B0D10"
        );
        p(
            mood,
            "pain",
            "Боль",
            "pulse",
            1.2,
            "pulse_speed",
            2.2,
            "vignette",
            0.7,
            "vignette_color",
            "#7A0000",
            "chromatic",
            0.5,
            "saturation",
            0.7,
            "radial_blur",
            0.08
        );
        p(
            mood,
            "low_health",
            "Мало здоровья",
            "pulse",
            1.5,
            "pulse_speed",
            1.4,
            "vignette_color",
            "#6E0000",
            "saturation",
            0.45,
            "contrast",
            1.1,
            "vignette",
            0.8
        );
        p(
            mood,
            "concussion",
            "Контузия",
            "blur",
            0.25,
            "radial_blur",
            0.35,
            "chromatic",
            1.2,
            "saturation",
            0.5,
            "wave",
            0.2,
            "vignette",
            0.9,
            "exposure",
            0.35
        );
        p(mood, "drunk", "Опьянение", "drunk", 0.8, "wave", 0.25, "wave_speed", 0.7, "motion_blur", 0.6, "chromatic", 0.5, "saturation", 1.15, "bloom", 0.7);
        p(
            mood,
            "poison",
            "Яд",
            "temperature",
            -0.2,
            "tint",
            -0.65,
            "saturation",
            1.2,
            "wave",
            0.22,
            "pulse",
            0.8,
            "pulse_speed",
            1.1,
            "vignette",
            0.6,
            "vignette_color",
            "#143F07",
            "chromatic",
            0.3
        );
        p(
            mood,
            "fever",
            "Жар",
            "temperature",
            0.5,
            "heat_haze",
            0.6,
            "bloom",
            1.2,
            "bloom_threshold",
            0.5,
            "wave",
            0.1,
            "saturation",
            1.2,
            "vignette",
            0.6,
            "vignette_color",
            "#4A1500"
        );
        p(mood, "fear", "Страх", "saturation", 0.4, "pulse", 1, "pulse_speed", 3, "chromatic", 0.6, "vignette", 1.1, "lens_distort", 0.15, "grain", 0.2);
        p(mood, "unconscious", "Обморок", "fade", 0.75, "blur", 0.6, "saturation", 0.2, "vignette", 1.5, "radial_blur", 0.2);
        p(mood, "stunned", "Оглушение", "blur", 0.35, "motion_blur", 0.8, "radial_blur", 0.25, "saturation", 0.6, "exposure", 0.6);
        p(
            mood,
            "rage",
            "Ярость",
            "saturation",
            1.5,
            "contrast",
            1.3,
            "color_amount",
            0.35,
            "color",
            "#FF5B4A",
            "bloom",
            1.2,
            "pulse",
            0.8,
            "pulse_speed",
            2.5,
            "vignette",
            0.8,
            "vignette_color",
            "#4A0000",
            "chromatic",
            0.5
        );
        p(mood, "speed", "Скорость", "radial_blur", 0.45, "motion_blur", 0.4, "chromatic", 0.7, "vignette", 0.5, "lens_distort", -0.1);
        p(mood, "low_oxygen", "Нечем дышать", "vignette", 1.8, "pulse", 1, "pulse_speed", 0.7, "saturation", 0.3, "blur", 0.15);
        String place = "Места";
        p(
            place,
            "underwater",
            "Под водой",
            "underwater",
            0.8,
            "color_amount",
            0.55,
            "color",
            "#3D8FD1",
            "fog",
            0.9,
            "fog_start",
            2,
            "fog_end",
            44,
            "fog_color",
            "#154A73",
            "bloom",
            0.7,
            "god_rays",
            0.6,
            "vignette",
            0.5,
            "blur",
            0.05
        );
        p(
            place,
            "cave",
            "Пещера",
            "exposure",
            -0.5,
            "ao",
            2.2,
            "ao_radius",
            1.4,
            "vignette",
            1,
            "contrast",
            1.15,
            "saturation",
            0.75,
            "fog",
            0.6,
            "fog_start",
            6,
            "fog_end",
            48,
            "fog_color",
            "#08090B"
        );
        p(place, "swamp", "Болото", "tint", -0.4, "saturation", 0.7, "fog", 0.75, "fog_start", 4, "fog_end", 42, "fog_color", "#4A5A32", "vignette", 0.5);
        p(
            place,
            "desert",
            "Пустыня",
            "temperature",
            0.5,
            "heat_haze",
            0.5,
            "exposure",
            0.2,
            "bloom",
            0.8,
            "bloom_threshold",
            0.6,
            "saturation",
            1.1,
            "fog",
            0.25,
            "fog_start",
            30,
            "fog_end",
            160,
            "fog_color",
            "#E4C48B"
        );
        p(
            place,
            "snow",
            "Метель",
            "temperature",
            -0.35,
            "exposure",
            0.25,
            "bloom",
            0.8,
            "saturation",
            0.8,
            "frost",
            0.45,
            "fog",
            0.7,
            "fog_start",
            6,
            "fog_end",
            60,
            "fog_color",
            "#DDE7F2"
        );
        p(
            place,
            "nether",
            "Преисподняя",
            "temperature",
            0.7,
            "contrast",
            1.25,
            "heat_haze",
            0.7,
            "bloom",
            1.5,
            "bloom_threshold",
            0.45,
            "fog",
            0.5,
            "fog_color",
            "#4A1006",
            "vignette",
            0.9,
            "vignette_color",
            "#2A0500"
        );
        p(
            place,
            "void",
            "Пустота",
            "saturation",
            0.15,
            "exposure",
            -0.3,
            "chromatic",
            0.5,
            "vignette",
            1.4,
            "fog",
            0.9,
            "fog_start",
            4,
            "fog_end",
            40,
            "fog_color",
            "#050308",
            "grain",
            0.2
        );
        p(
            place,
            "magic",
            "Волшебный лес",
            "bloom",
            1.6,
            "bloom_threshold",
            0.5,
            "hue",
            -18,
            "saturation",
            1.3,
            "fog",
            0.35,
            "fog_color",
            "#6B4FD0",
            "fog_start",
            10,
            "fog_end",
            90,
            "god_rays",
            1,
            "vignette",
            0.4
        );
        p(
            place,
            "toxic",
            "Заражённая земля",
            "tint",
            -0.7,
            "color_amount",
            0.3,
            "color",
            "#8CFF5A",
            "fog",
            0.6,
            "fog_start",
            6,
            "fog_end",
            50,
            "fog_color",
            "#5D7A1E",
            "glitch",
            0.04,
            "grain",
            0.2
        );
        String tech = "Техника и стиль";
        p(tech, "vhs", "Кассета VHS", "vhs", 0.8, "scanlines", 0.3, "chromatic", 0.5, "grain", 0.25, "saturation", 0.85, "glitch", 0.05);
        p(tech, "crt", "Старый телевизор", "crt", 0.9, "scanlines", 0.6, "chromatic", 0.25, "vignette", 0.6, "bloom", 0.5, "flicker", 0.08);
        p(
            tech,
            "camera",
            "Камера наблюдения",
            "grayscale",
            0.85,
            "scanlines",
            0.5,
            "grain",
            0.4,
            "vignette",
            0.7,
            "lens_distort",
            0.25,
            "pixelate",
            2,
            "vhs",
            0.25
        );
        p(tech, "glitch", "Сбой", "glitch", 0.7, "chromatic", 1.2, "vhs", 0.5, "scanlines", 0.3);
        p(tech, "night_vision", "Ночное зрение", "night_vision", 1, "grain", 0.35, "vignette", 1, "scanlines", 0.2, "bloom", 1);
        p(tech, "thermal", "Тепловизор", "thermal", 1, "blur", 0.05, "scanlines", 0.15, "grain", 0.1);
        p(tech, "retro_8bit", "8 бит", "pixelate", 5, "posterize", 5, "dither", 0.6, "saturation", 1.2);
        p(tech, "gameboy", "Карманная приставка", "pixelate", 4, "posterize", 4, "grayscale", 1, "color_amount", 1, "color", "#9BBC0F", "dither", 0.5);
        p(tech, "comic", "Комикс", "outline", 0.9, "posterize", 6, "halftone", 0.4, "saturation", 1.35, "contrast", 1.1);
        p(tech, "sketch", "Карандаш", "sketch", 1, "grain", 0.15);
        p(tech, "cel", "Рисованный", "outline", 0.8, "posterize", 8, "saturation", 1.2, "ao", 1);
        p(
            tech,
            "hologram",
            "Голограмма",
            "grayscale",
            0.7,
            "color_amount",
            1,
            "color",
            "#46E8FF",
            "scanlines",
            0.7,
            "glitch",
            0.08,
            "bloom",
            1.2,
            "flicker",
            0.2
        );
    }

    public record Preset(String name, String title, String group, PostProfile profile) {
    }
}
