package org.sprautestudio.spraute_engine.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.core.model.SpGeoBounds;
import org.sprautestudio.spraute_engine.core.model.SpGeoModel;
import org.sprautestudio.spraute_engine.core.model.SpJavaModel;
import org.sprautestudio.spraute_engine.core.parser.SpGeoParser;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public final class CustomModels {
    public static final Map<Item, CustomModels.Engine> ENGINE_ITEMS = new ConcurrentHashMap<>();
    public static final Map<Block, CustomModels.Engine> ENGINE_BLOCKS = new ConcurrentHashMap<>();

    private CustomModels() {
    }

    public static List<String> names(Path project) {
        List<String> out = new ArrayList<>();
        Path dir = project.resolve("models");
        if (!Files.isDirectory(dir)) {
            return out;
        } else {
            try (Stream<Path> all = Files.list(dir)) {
                for (Path d : (Iterable<Path>) all.filter(x$0 -> Files.isDirectory(x$0)).sorted()::iterator) {
                    if (file(d, ".geo.json") != null) {
                        out.add(d.getFileName().toString());
                    }
                }
            } catch (IOException var8) {
                SprauteEngine.LOGGER.warn("Spraute: cannot list models of {}", project, var8);
            }

            return out;
        }
    }

    public static Path texture(Path project, String name) {
        Path d = project.resolve("models").resolve(name);
        return Files.isDirectory(d) ? file(d, ".png") : null;
    }

    public static SpJavaModel.Result convert(Path project, ModelLook look) {
        if (look != null && look.active()) {
            Path d = project.resolve("models").resolve(look.name);
            Path geo = Files.isDirectory(d) ? file(d, ".geo.json") : null;
            if (geo == null) {
                return null;
            } else {
                try {
                    SpGeoModel m = SpGeoParser.parse(Files.readString(geo, StandardCharsets.UTF_8));
                    if (m == null) {
                        return null;
                    } else {
                        SpJavaModel.Result r = SpJavaModel.convert(m, look.scale, look.turn, look.offset(), look.fit);
                        if (r.approximated() > 0) {
                            SprauteEngine.LOGGER
                                .info(
                                    "Spraute: model {}: {} cubes turned about several axes or not by 22.5° — set at the nearest turn the game allows",
                                    look.name,
                                    r.approximated()
                                );
                        }

                        return r;
                    }
                } catch (RuntimeException | IOException var6) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read the model {}", geo, var6);
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static JsonObject display(Path project, String name) {
        Path d = project.resolve("models").resolve(name);
        if (!Files.isDirectory(d)) {
            return null;
        } else {
            try {
                try (Stream<Path> files = Files.list(d)) {
                    for (Path f : (Iterable<Path>) files.sorted()::iterator) {
                        String n = f.getFileName().toString().toLowerCase();
                        boolean json = n.endsWith(".json") && !n.endsWith(".geo.json") && !n.endsWith(".animation.json") && !n.endsWith(".animations.json");
                        if (json || n.endsWith(".bbmodel")) {
                            try {
                                JsonElement o = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
                                if (o.isJsonObject() && o.getAsJsonObject().get("display") instanceof JsonObject disp && disp.size() > 0) {
                                    return disp;
                                }
                            } catch (RuntimeException | IOException var12) {
                            }
                        }
                    }
                }

                return null;
            } catch (IOException var14) {
                return null;
            }
        }
    }

    public static double reach(CustomModels.Engine e) {
        ModelLibrary.Model m = ModelLibrary.find(e.project(), e.look().name);
        if (m == null) {
            return 1.0;
        } else {
            try {
                SpGeoBounds b = m.geometry().baked.bounds;
                float px = Math.max(
                    Math.max(Math.abs(b.minX()), Math.abs(b.maxX())),
                    Math.max(Math.max(Math.abs(b.minZ()), Math.abs(b.maxZ())), Math.max(Math.abs(b.minY()), Math.abs(b.maxY())))
                );
                float shift = Math.max(Math.abs(e.look().x), Math.max(Math.abs(e.look().y), Math.abs(e.look().z)));
                return Math.max(1.0, (px * e.look().scale + shift) / 16.0F + 0.5);
            } catch (RuntimeException var5) {
                return 1.0;
            }
        }
    }

    public static List<BlockDef.Box> boxes(Path project, ModelLook look) {
        List<BlockDef.Box> out = new ArrayList<>();
        boolean fit = look.fit;
        look.fit = false;

        SpJavaModel.Result r;
        try {
            r = convert(project, look);
        } finally {
            look.fit = fit;
        }

        if (r == null) {
            return out;
        } else {
            for (int[] b : r.boxes()) {
                out.add(new BlockDef.Box(b[0], b[1], b[2], b[3], b[4], b[5]));
            }

            return out;
        }
    }

    private static Path file(Path dir, String ending) {
        try {
            Path var3;
            try (Stream<Path> files = Files.list(dir)) {
                var3 = files.filter(f -> Files.isRegularFile(f) && f.getFileName().toString().toLowerCase().endsWith(ending)).sorted().findFirst().orElse(null);
            }

            return var3;
        } catch (IOException var7) {
            return null;
        }
    }

    public record Engine(String project, ModelLook look) {
    }
}
