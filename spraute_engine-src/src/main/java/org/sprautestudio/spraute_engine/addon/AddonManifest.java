package org.sprautestudio.spraute_engine.addon;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public record AddonManifest(String id, String name, String version, String entry, boolean clientOnly, List<String> requires) {
    public static final String FILE = "spraute_addon.properties";
    public static final String FOLDER = "META-INF";
    private static final String ID_RULE = "[a-z0-9_.-]{1,64}";

    public static AddonManifest read(Reader text) throws IOException {
        Properties p = new Properties();
        p.load(text);
        String id = p.getProperty("id", "").trim();
        String entry = p.getProperty("entry", "").trim();
        String side = p.getProperty("side", "both").trim();
        if (!id.matches("[a-z0-9_.-]{1,64}")) {
            throw new IllegalArgumentException("id: латинские строчные буквы, цифры, «_», «.» и «-», а не «" + id + "»");
        } else if (entry.isEmpty()) {
            throw new IllegalArgumentException("entry: не указан класс аддона");
        } else if (!side.equals("both") && !side.equals("client")) {
            throw new IllegalArgumentException("side: both или client, а не «" + side + "»");
        } else {
            List<String> requires = new ArrayList<>();

            for (String r : p.getProperty("requires", "").split(",")) {
                if (!r.isBlank()) {
                    requires.add(r.trim());
                }
            }

            return new AddonManifest(id, p.getProperty("name", id).trim(), p.getProperty("version", "?").trim(), entry, side.equals("client"), requires);
        }
    }
}
