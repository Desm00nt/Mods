package org.sprautestudio.spraute_engine.core.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;

public final class PlayerSkins {
    private static final Map<UUID, ResourceLocation> OVERRIDES = new HashMap<>();

    private PlayerSkins() {
    }

    public static void set(String player, String texture) {
        UUID id = UUID.fromString(player);
        if (texture.isEmpty()) {
            OVERRIDES.remove(id);
        } else {
            OVERRIDES.put(id, ResourceLocation.tryParse(texture));
        }
    }

    public static ResourceLocation of(UUID player) {
        return OVERRIDES.get(player);
    }

    public static void clear() {
        OVERRIDES.clear();
    }
}
