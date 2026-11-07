package org.sprautestudio.spraute_engine.custom;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.server.packs.PackResources;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.model.SpJavaModel;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.playeranim.PlayerAnimsClient;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public final class ItemPack {
    private ItemPack() {
    }

    public static PackResources build() throws IOException {
        Path root = Platform.get().gameDir().resolve("spraute").resolve("item_pack");
        clear(root);
        Files.createDirectories(root);
        JsonObject lang = new JsonObject();
        CustomModels.ENGINE_ITEMS.clear();
        PlayerAnimsClient.ITEMS.clear();
        CustomModels.ENGINE_BLOCKS.clear();

        for (CustomItems.Registered r : CustomItems.registered().values()) {
            ItemDef d = current(r);
            if (!d.swingAnim.isBlank() || !d.holdAnim.isBlank()) {
                PlayerAnimsClient.ITEMS.put(r.item().get(), new PlayerAnimsClient.ItemAnims(d.holdAnim.trim(), d.swingAnim.trim()));
            }

            try {
                write(root, r, d);
                lang.addProperty("item.spraute_engine." + d.id, d.name);
            } catch (RuntimeException | IOException var7) {
                SprauteEngine.LOGGER.warn("Spraute: cannot prepare the look of the item {}", d.id, var7);
            }
        }

        for (CustomItems.RegisteredTab t : CustomItems.tabs().values()) {
            lang.addProperty("itemGroup.spraute_engine." + t.def().id, currentTabName(t));
        }

        for (CustomBlocks.Registered r : CustomBlocks.registered().values()) {
            BlockDef d = CustomBlocks.current(r);

            try {
                writeBlock(root, r, d);
                lang.addProperty("block.spraute_engine." + d.id, d.name);
            } catch (RuntimeException | IOException var6) {
                SprauteEngine.LOGGER.warn("Spraute: cannot prepare the look of the block {}", d.id, var6);
            }
        }

        if (CustomParts.part() != null) {
            put(root.resolve("assets/spraute_engine/blockstates/part.json"), "{\"variants\":{\"\":{\"model\":\"spraute_engine:block/part\"}}}");
            put(root.resolve("assets/spraute_engine/models/block/part.json"), "{\"textures\":{\"particle\":\"minecraft:block/stone\"}}");
        }

        if (lang.size() > 0) {
            for (String language : List.of("en_us", "ru_ru")) {
                put(root.resolve("assets/spraute_engine/lang/" + language + ".json"), lang.toString());
            }
        }

        put(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"Spraute\"}}");
        return Mc.folderPack(root);
    }

    private static ItemDef current(CustomItems.Registered r) {
        Path f = r.project().resolve("items").resolve(r.def().id + ".json");

        try {
            if (Files.isRegularFile(f)) {
                ItemDef d = ItemDef.fromJson(Files.readString(f, StandardCharsets.UTF_8));
                d.id = r.def().id;
                return d;
            }
        } catch (RuntimeException | IOException var3) {
            SprauteEngine.LOGGER.warn("Spraute: cannot read the item {}", f, var3);
        }

        return r.def();
    }

    private static void writeBlock(Path root, CustomBlocks.Registered reg, BlockDef d) throws IOException {
        Path project = reg.project();
        Predicate<String> exists = name -> picture(project, name) != null;

        for (Entry<String, String> slot : BlockAssets.pictures(d, exists).entrySet()) {
            copy(picture(project, slot.getValue()), root.resolve(BlockAssets.textureFile(d, slot.getKey())));
        }

        JsonObject own = null;
        JsonObject ownItem = null;
        if (d.hasModel() && d.engineModel() && reg.def().engineModel()) {
            String tex = modelTexture(project, d.model, root, "block", d.id);
            own = ModelAssets.particles(tex);
            ownItem = ModelAssets.engineItem(tex, ModelAssets.display(d.model, CustomModels.display(project, d.model.name)));
            CustomModels.Engine engine = new CustomModels.Engine(ModelLibrary.projectNameOf(project), d.model);
            CustomModels.ENGINE_BLOCKS.put(reg.block().get(), engine);
            CustomModels.ENGINE_ITEMS.put(reg.item().get(), engine);
        } else if (d.hasModel()) {
            SpJavaModel.Result r = CustomModels.convert(project, d.model);
            if (r != null) {
                own = ModelAssets.model(
                    r, modelTexture(project, d.model, root, "block", d.id), ModelAssets.display(d.model, CustomModels.display(project, d.model.name))
                );
            }
        }

        for (Entry<String, String> file : BlockAssets.assets(d, exists, own).entrySet()) {
            put(root.resolve(file.getKey()), file.getValue());
        }

        if (ownItem != null) {
            put(root.resolve("assets/spraute_engine/models/item/" + d.id + ".json"), ownItem.toString());
        }
    }

    private static String currentTabName(CustomItems.RegisteredTab t) {
        Path f = t.project().resolve("tabs").resolve(t.def().id + ".json");

        try {
            if (Files.isRegularFile(f)) {
                return TabDef.fromJson(Files.readString(f, StandardCharsets.UTF_8)).name;
            }
        } catch (RuntimeException | IOException var3) {
            SprauteEngine.LOGGER.warn("Spraute: cannot read the tab {}", f, var3);
        }

        return t.def().name;
    }

    private static String modelTexture(Path project, ModelLook look, Path root, String folder, String id) throws IOException {
        Path png = CustomModels.texture(project, look.name);
        if (png == null) {
            return "minecraft:block/white_concrete";
        } else {
            copy(png, root.resolve("assets/spraute_engine/textures/" + folder + "/" + id + "_model.png"));
            return "spraute_engine:" + folder + "/" + id + "_model";
        }
    }

    private static void write(Path root, CustomItems.Registered reg, ItemDef d) throws IOException {
        Path project = reg.project();
        JsonObject model = new JsonObject();
        Path picture = picture(project, d.texture);
        SpJavaModel.Result own = d.model.active() && !d.model.engine() ? CustomModels.convert(project, d.model) : null;
        JsonObject display = d.model.active() ? ModelAssets.display(d.model, CustomModels.display(project, d.model.name)) : null;
        if (d.model.engine()) {
            model = ModelAssets.engineItem(modelTexture(project, d.model, root, "item", d.id), display);
            CustomModels.ENGINE_ITEMS.put(reg.item().get(), new CustomModels.Engine(ModelLibrary.projectNameOf(project), d.model));
        } else if (own != null) {
            model = ModelAssets.model(own, modelTexture(project, d.model, root, "item", d.id), display);
        } else if (picture != null) {
            model.addProperty("parent", d.handheld ? "minecraft:item/handheld" : "minecraft:item/generated");
            JsonObject textures = new JsonObject();
            textures.addProperty("layer0", "spraute_engine:item/" + d.id);
            model.add("textures", textures);
            copy(picture, root.resolve("assets/spraute_engine/textures/item/" + d.id + ".png"));
        } else {
            model.addProperty("parent", itemModel(d.lookItem));
        }

        put(root.resolve("assets/spraute_engine/models/item/" + d.id + ".json"), model.toString());
        if (d.isArmor() && d.armorLook.equals("image")) {
            Path one = picture(project, d.armorLayer1);
            Path two = picture(project, d.armorLayer2);
            String base = "assets/minecraft/textures/models/armor/" + CustomItems.armorName(d) + "_layer_";
            if (one != null) {
                copy(one, root.resolve(base + "1.png"));
            }

            if (two != null) {
                copy(two, root.resolve(base + "2.png"));
            }
        }
    }

    private static String itemModel(String id) {
        if (id != null && !id.isEmpty()) {
            int colon = id.indexOf(58);
            return colon < 0 ? "minecraft:item/" + id : id.substring(0, colon + 1) + "item/" + id.substring(colon + 1);
        } else {
            return "minecraft:item/paper";
        }
    }

    private static Path picture(Path project, String name) {
        if (name != null && !name.isEmpty()) {
            Path f = project.resolve("images").resolve(name + ".png");
            return Files.isRegularFile(f) ? f : null;
        } else {
            return null;
        }
    }

    private static void copy(Path from, Path to) throws IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void put(Path to, String text) throws IOException {
        Files.createDirectories(to.getParent());
        Files.writeString(to, text, StandardCharsets.UTF_8);
    }

    private static void clear(Path root) throws IOException {
        if (Files.exists(root)) {
            try (Stream<Path> all = Files.walk(root)) {
                for (Path p : (Iterable<Path>) all.sorted(Comparator.reverseOrder())::iterator) {
                    Files.delete(p);
                }
            }
        }
    }
}
