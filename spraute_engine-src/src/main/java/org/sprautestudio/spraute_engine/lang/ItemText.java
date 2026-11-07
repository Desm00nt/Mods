package org.sprautestudio.spraute_engine.lang;

import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemText {
    private static final Pattern NBT_START = Pattern.compile("\\{\\s*(?:[\\w.+-]+\\s*:|\"|})");
    private static UnaryOperator<String> nbtCheck = nbt -> null;

    private ItemText() {
    }

    public static void setNbtCheck(UnaryOperator<String> check) {
        nbtCheck = check;
    }

    public static String id(String spec) {
        String key = spec == null ? "" : spec.trim();
        Matcher m = NBT_START.matcher(key);
        String id = m.find() ? key.substring(0, m.start()).trim() : key;
        return !id.isEmpty() && !id.contains(":") ? "minecraft:" + id : id;
    }

    public static String nbt(String spec) {
        String key = spec == null ? "" : spec.trim();
        Matcher m = NBT_START.matcher(key);
        return m.find() ? key.substring(m.start()).trim() : "";
    }

    public static String error(String spec) {
        String text = nbt(spec);
        return text.isEmpty() ? null : nbtCheck.apply(text);
    }
}
