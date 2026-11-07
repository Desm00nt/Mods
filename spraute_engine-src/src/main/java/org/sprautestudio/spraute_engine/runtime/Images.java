package org.sprautestudio.spraute_engine.runtime;

import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.lang.Specs;

public final class Images {
    private static final String[] CYRILLIC = new String[]{
        "a",
        "b",
        "v",
        "g",
        "d",
        "e",
        "zh",
        "z",
        "i",
        "y",
        "k",
        "l",
        "m",
        "n",
        "o",
        "p",
        "r",
        "s",
        "t",
        "u",
        "f",
        "h",
        "c",
        "ch",
        "sh",
        "sch",
        "",
        "y",
        "",
        "e",
        "yu",
        "ya"
    };

    private Images() {
    }

    public static String slug(String name) {
        StringBuilder sb = new StringBuilder();

        for (char ch : name.trim().toLowerCase().toCharArray()) {
            if ((ch < 'a' || ch > 'z') && (ch < '0' || ch > '9') && ch != '_') {
                if (ch >= 1072 && ch <= 1103) {
                    sb.append(CYRILLIC[ch - 1072]);
                } else if (ch == 1105) {
                    sb.append("e");
                } else if (ch != ' ' && ch != '-' && ch != '.') {
                    sb.append('x').append(Integer.toHexString(ch));
                } else {
                    sb.append('_');
                }
            } else {
                sb.append(ch);
            }
        }

        return sb.length() == 0 ? "image" : sb.toString();
    }

    public static String iconPath(String project, String picture) {
        return "spraute/" + slug(project) + "/" + slug(picture);
    }

    public static ResourceLocation texture(String project, String picture) {
        return new ResourceLocation("spraute_engine", "textures/gui/chat/" + iconPath(project, picture) + ".png");
    }

    public static String speechIcon(String project, String value) {
        return Specs.SPEECH_ICONS.contains(value) ? value : iconPath(project, value);
    }

    public static ResourceLocation portrait(String project, String value) {
        return Specs.SPEECH_ICONS.contains(value) ? new ResourceLocation("spraute_engine", "textures/gui/chat/" + value + ".png") : texture(project, value);
    }
}
