package org.sprautestudio.spraute_engine.core.system;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class PlayerTexture {
    private PlayerTexture() {
    }

    private static PlayerTexture.Saved saved(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(PlayerTexture.Saved::load, PlayerTexture.Saved::new, "spraute_player_texture");
    }

    public static void set(ServerPlayer p, ResourceLocation texture) {
        MinecraftServer s = Platform.get().server();
        if (s != null && p != null) {
            PlayerTexture.Saved d = saved(s);
            String id = p.getUUID().toString();
            if (texture == null) {
                d.textures.remove(id);
            } else {
                d.textures.put(id, texture.toString());
            }

            d.setDirty();
            SprauteNetwork.sendToAll(new Packets.PlayerSkin(id, texture == null ? "" : texture.toString()));
        }
    }

    public static void sendTo(ServerPlayer p) {
        saved(p.server).textures.forEach((id, tex) -> SprauteNetwork.sendTo(p, new Packets.PlayerSkin(id, tex)));
    }

    private static final class Saved extends SavedData {
        final Map<String, String> textures = new HashMap<>();

        @Override
        public CompoundTag save(CompoundTag tag) {
            this.textures.forEach(tag::putString);
            return tag;
        }

        static PlayerTexture.Saved load(CompoundTag tag) {
            PlayerTexture.Saved s = new PlayerTexture.Saved();

            for (String k : tag.getAllKeys()) {
                if (!k.equals("texture")) {
                    s.textures.put(k, tag.getString(k));
                }
            }

            return s;
        }
    }
}
