package org.sprautestudio.spraute_engine.custom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.server.packs.PackResources;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class BlockData {
    private BlockData() {
    }

    public static synchronized PackResources build() throws IOException {
        if (Platform.get() == null) {
            return null;
        } else {
            List<LootDef> loot = CustomLoot.all();
            if (CustomBlocks.registered().isEmpty() && loot.isEmpty() && !Dungeons.any() && CustomMobs.registered().isEmpty()) {
                return null;
            } else {
                Path root = Platform.get().gameDir().resolve("spraute").resolve("block_data");
                if (Files.exists(root)) {
                    try (Stream<Path> all = Files.walk(root)) {
                        for (Path p : (Iterable<Path>) all.sorted(Comparator.reverseOrder())::iterator) {
                            Files.delete(p);
                        }
                    }
                }

                List<BlockDef> defs = new ArrayList<>();

                for (CustomBlocks.Registered r : CustomBlocks.registered().values()) {
                    defs.add(CustomBlocks.current(r));
                }

                for (Entry<String, String> file : BlockAssets.data(defs).entrySet()) {
                    Path to = root.resolve(file.getKey());
                    Files.createDirectories(to.getParent());
                    Files.writeString(to, file.getValue(), StandardCharsets.UTF_8);
                }

                for (Entry<String, String> file : LootDef.data(loot).entrySet()) {
                    Path to = root.resolve(file.getKey());
                    Files.createDirectories(to.getParent());
                    Files.writeString(to, file.getValue(), StandardCharsets.UTF_8);
                }

                Dungeons.writeData(root);
                CustomMobs.writeData(root);
                Files.createDirectories(root);
                Files.writeString(root.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"Spraute\"}}", StandardCharsets.UTF_8);
                return Mc.folderPack(root);
            }
        }
    }
}
