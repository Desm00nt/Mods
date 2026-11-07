package org.sprautestudio.spraute_engine.addon;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class Addons {
    static final Map<String, AddonContext.Command> COMMANDS = new HashMap<>();
    static final Map<String, AddonContext.StoryFunction> FUNCTIONS = new HashMap<>();
    static final List<IntConsumer> STORY_STOP = new ArrayList<>();
    private static final List<Addons.Info> INFO = new ArrayList<>();

    private Addons() {
    }

    public static AddonContext.Command command(String key) {
        return COMMANDS.get(key);
    }

    public static AddonContext.StoryFunction function(String name) {
        return FUNCTIONS.get(name);
    }

    public static List<Addons.Info> loaded() {
        return List.copyOf(INFO);
    }

    public static void storyStopped(int script) {
        for (IntConsumer l : STORY_STOP) {
            try {
                l.accept(script);
            } catch (RuntimeException var4) {
                SprauteEngine.LOGGER.warn("Spraute: an addon failed when a story stopped", (Throwable)var4);
            }
        }
    }

    public static void load() {
        List<AddonManifest> waiting = new ArrayList<>();
        Set<String> found = new HashSet<>();

        for (Path file : Platform.get().addonManifests()) {
            try (Reader text = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                AddonManifest m = AddonManifest.read(text);
                if (found.add(m.id())) {
                    waiting.add(m);
                } else {
                    INFO.add(new Addons.Info(m.id(), m.name(), m.version(), Map.of(), "Два аддона с одним id"));
                }
            } catch (Exception var9) {
                INFO.add(new Addons.Info(file.toString(), file.toString(), "?", Map.of(), "Не удалось прочитать манифест: " + var9.getMessage()));
                SprauteEngine.LOGGER.error("Spraute: cannot read the addon manifest {}", file, var9);
            }
        }

        waiting.sort(Comparator.comparing(AddonManifest::id));
        Set<String> started = new HashSet<>();

        while (!waiting.isEmpty()) {
            AddonManifest next = null;

            for (AddonManifest m : waiting) {
                if (started.containsAll(m.requires())) {
                    next = m;
                    break;
                }
            }

            if (next == null) {
                for (AddonManifest mx : waiting) {
                    INFO.add(
                        new Addons.Info(
                            mx.id(), mx.name(), mx.version(), Map.of(), "Нет нужных аддонов, или они не запустились: " + String.join(", ", mx.requires())
                        )
                    );
                }

                return;
            }

            waiting.remove(next);
            if (start(next)) {
                started.add(next.id());
            }
        }
    }

    private static boolean start(AddonManifest m) {
        AddonContext context = new AddonContext(m);
        if (m.clientOnly() && !Platform.get().isClient()) {
            INFO.add(new Addons.Info(m.id(), m.name(), m.version(), Map.of(), "Только для игры с окном: на сервере не запущен"));
            return false;
        } else {
            try {
                SprauteAddon addon = (SprauteAddon)Class.forName(m.entry(), true, SprauteEngine.class.getClassLoader()).getDeclaredConstructor().newInstance();
                addon.register(context);
                if (Platform.get().isClient()) {
                    addon.registerClient(new AddonClient(context));
                }

                INFO.add(new Addons.Info(m.id(), m.name(), m.version(), context.added, null));
                SprauteEngine.LOGGER.info("Spraute: addon {} {} started: {}", m.id(), m.version(), context.added);
                return true;
            } catch (Throwable var3) {
                INFO.add(new Addons.Info(m.id(), m.name(), m.version(), context.added, var3.toString()));
                SprauteEngine.LOGGER.error("Spraute: the addon {} failed to start", m.id(), var3);
                return false;
            }
        }
    }

    public record Info(String id, String name, String version, Map<String, Integer> added, String error) {
    }
}
