package org.sprautestudio.spraute_engine.runtime.models;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.content.ContentClient;

final class ClientModels {
    private static final Map<Path, Long> LOADED = new HashMap<>();

    private ClientModels() {
    }

    static Collection<Path> serverProjects() {
        return ContentClient.activeDirs();
    }

    static void registerTextures(Collection<ModelLibrary.Model> models) {
        for (ModelLibrary.Model m : models) {
            Path f = m.texture();
            if (f == null) {
                continue;
            }

            long stamp;
            try {
                stamp = Files.getLastModifiedTime(f).toMillis();
            } catch (Exception ignored) {
                continue;
            }

            Long old = LOADED.put(f, stamp);
            if (old == null || old != stamp) {
                try (InputStream in = Files.newInputStream(f)) {
                    NativeImage img = NativeImage.read(in);
                    Runnable reg = () -> Minecraft.getInstance().getTextureManager().register(m.textureId(), new DynamicTexture(img));
                    if (RenderSystem.isOnRenderThread()) {
                        reg.run();
                    } else {
                        RenderSystem.recordRenderCall(reg::run);
                    }
                } catch (Exception e) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read model texture {}", f, e);
                }
            }
        }
    }
}
