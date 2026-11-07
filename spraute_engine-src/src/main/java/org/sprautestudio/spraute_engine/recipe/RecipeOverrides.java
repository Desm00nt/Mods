package org.sprautestudio.spraute_engine.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.RecipeManager;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.platform.Hooks;

public final class RecipeOverrides {
    private static final String FOLDER = "recipes";
    static final String RESTORE = "spraute_restore";
    private static String applied = "";

    private RecipeOverrides() {
    }

    public static void registerHooks() {
        Hooks.SERVER_ABOUT_TO_START.add(e -> reload(e.getServer()));
    }

    public static Map<ResourceLocation, JsonElement> apply(Map<ResourceLocation, JsonElement> found) {
        Map<ResourceLocation, JsonObject> files = files();
        applied = files.toString();
        if (files.isEmpty()) {
            return found;
        } else {
            Map<ResourceLocation, JsonElement> out = new HashMap<>(found);
            files.forEach((id, json) -> {
                if (json.has("spraute_disabled") && json.get("spraute_disabled").getAsBoolean()) {
                    out.remove(id);
                } else {
                    out.put(id, json);
                }
            });
            return out;
        }
    }

    public static void reload(MinecraftServer server) {
        if (!files().toString().equals(applied)) {
            RecipeManager manager = server.getRecipeManager();
            ((RecipeReload)manager).spraute$reapply(server.getResourceManager());
            if (server.getPlayerList() != null) {
                ClientboundUpdateRecipesPacket packet = new ClientboundUpdateRecipesPacket(manager.getRecipes());

                for (ServerPlayer p : List.copyOf(server.getPlayerList().getPlayers())) {
                    p.connection.send(packet);
                    p.getRecipeBook().sendInitialRecipeBook(p);
                }
            }
        }
    }

    static Map<ResourceLocation, JsonObject> files() {
        Map<ResourceLocation, JsonObject> out = new TreeMap<>();
        Path root = ProjectContent.root();
        if (root == null) {
            return out;
        } else {
            try {
                if (Files.isDirectory(root)) {
                    try (Stream<Path> projects = Files.list(root)) {
                        for (Path project : (Iterable<Path>) projects.sorted()::iterator) {
                            read(project.resolve("recipes"), out);
                        }
                    }
                }

                read(storyDir(root), out);
            } catch (IOException var7) {
                SprauteEngine.LOGGER.warn("Spraute: cannot list the projects' recipes", (Throwable)var7);
            }

            out.values().removeIf(json -> json.has("spraute_restore"));
            return out;
        }
    }

    private static Path storyDir(Path contentRoot) {
        return contentRoot.resolveSibling("story_recipes");
    }

    private static Path storyFile(ResourceLocation id) {
        Path root = ProjectContent.root();
        if (root == null) {
            throw new IllegalStateException("no server");
        } else {
            return storyDir(root).resolve(id.getNamespace()).resolve(id.getPath() + ".json");
        }
    }

    static void writeStoryFile(ResourceLocation id, JsonObject json) throws IOException {
        Path f = storyFile(id);
        Files.createDirectories(f.getParent());
        Files.writeString(f, json.toString(), StandardCharsets.UTF_8);
    }

    static boolean deleteStoryFile(ResourceLocation id) throws IOException {
        return Files.deleteIfExists(storyFile(id));
    }

    private static void read(Path dir, Map<ResourceLocation, JsonObject> out) throws IOException {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> all = Files.walk(dir)) {
                for (Path f : (Iterable<Path>) all.filter(p -> p.toString().endsWith(".json")).sorted()::iterator) {
                    ResourceLocation id = idOf(dir.relativize(f).toString().replace('\\', '/'));
                    if (id == null) {
                        SprauteEngine.LOGGER.warn("Spraute: {} is not named as a recipe", f);
                    } else {
                        try {
                            out.put(id, JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject());
                        } catch (IOException | RuntimeException var8) {
                            SprauteEngine.LOGGER.warn("Spraute: cannot read the recipe {}", f, var8);
                        }
                    }
                }
            }
        }
    }

    private static ResourceLocation idOf(String rel) {
        int slash = rel.indexOf(47);
        return slash > 0 && rel.endsWith(".json")
            ? ResourceLocation.tryParse(rel.substring(0, slash) + ":" + rel.substring(slash + 1, rel.length() - 5))
            : null;
    }
}
