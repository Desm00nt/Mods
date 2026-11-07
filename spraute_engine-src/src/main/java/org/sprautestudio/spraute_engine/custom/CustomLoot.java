package org.sprautestudio.spraute_engine.custom;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.sprautestudio.spraute_engine.SprauteEngine;

public final class CustomLoot {
    public static final String FOLDER = "loot_tables";

    private CustomLoot() {
    }

    public static List<LootDef> read(Path project) throws IOException {
        List<LootDef> out = new ArrayList<>();
        return out;
    }

    static List<LootDef> all() {
        List<LootDef> out = new ArrayList<>();
        Path root = CustomItems.projectsDir();
        if (!Files.isDirectory(root)) {
            return out;
        } else {
            try (Stream<Path> projects = Files.list(root)) {
                for (Path project : (Iterable<Path>) projects.filter(x$0 -> Files.isDirectory(x$0)).sorted()::iterator) {
                    out.addAll(read(project));
                }
            } catch (IOException var7) {
                SprauteEngine.LOGGER.error("Spraute: cannot list the projects for their loot tables", (Throwable)var7);
            }

            return out;
        }
    }
}
