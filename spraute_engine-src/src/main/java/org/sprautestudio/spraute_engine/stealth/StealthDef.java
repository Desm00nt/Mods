package org.sprautestudio.spraute_engine.stealth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StealthDef {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public String title = "Страж";
    public String shape = "fan";
    public float range = 8.0F;
    public float angle = 70.0F;
    public float width = 4.0F;
    public float height = 2.5F;
    public float sneak = 0.5F;
    public float notice = 0.0F;
    public float speed = 0.0F;
    public int color = 16724016;
    public boolean through;
    public List<String> restore = new ArrayList<>(StealthShape.RESTORES);
    public String back = "start";
    public double backX;
    public double backY;
    public double backZ;
    public float backYaw;
    public float backPitch;
    public static final Map<String, String> PRESET_TITLES = new LinkedHashMap<>();

    public static StealthDef preset(String name) {
        StealthDef d = new StealthDef();
        switch (name) {
            case "guard":
                d.title = "Страж";
                break;
            case "sentry":
                d.title = "Часовой";
                d.range = 14.0F;
                d.angle = 30.0F;
                break;
            case "watcher":
                d.title = "Сторож";
                d.shape = "circle";
                d.range = 5.0F;
                break;
            case "beam":
                d.title = "Луч";
                d.shape = "rect";
                d.range = 12.0F;
                d.width = 3.0F;
                break;
            case "blind":
                d.title = "Подслеповатый";
                d.range = 6.0F;
                d.angle = 90.0F;
                d.notice = 1.5F;
                break;
            default:
                return null;
        }

        return d;
    }

    public static StealthDef example(String title) {
        StealthDef d = new StealthDef();
        d.title = title;
        return d;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static StealthDef fromJson(String json) {
        StealthDef d = null;

        try {
            d = GSON.fromJson(json, StealthDef.class);
        } catch (Exception var3) {
        }

        if (d == null) {
            d = new StealthDef();
        }

        d.fix();
        return d;
    }

    public void fix() {
        if (this.title == null) {
            this.title = "";
        }

        if (this.shape == null || !StealthShape.NAMES.contains(this.shape)) {
            this.shape = "fan";
        }

        if (this.back == null) {
            this.back = "start";
        }

        if (this.restore == null) {
            this.restore = new ArrayList<>(StealthShape.RESTORES);
        }

        this.range = Math.max(0.5F, this.range);
        this.angle = Math.max(1.0F, Math.min(360.0F, this.angle));
        this.width = Math.max(0.5F, this.width);
        this.height = Math.max(0.1F, this.height);
        this.sneak = Math.max(0.0F, this.sneak);
        this.notice = Math.max(0.0F, this.notice);
        this.speed = Math.max(0.0F, this.speed);
    }

    static {
        PRESET_TITLES.put("guard", "Страж — обычный веер");
        PRESET_TITLES.put("sentry", "Часовой — узко и далеко");
        PRESET_TITLES.put("watcher", "Сторож — видит вокруг себя");
        PRESET_TITLES.put("beam", "Луч — полоса вперёд");
        PRESET_TITLES.put("blind", "Подслеповатый — замечает не сразу");
    }
}
