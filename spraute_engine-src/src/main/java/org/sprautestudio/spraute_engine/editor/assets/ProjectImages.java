package org.sprautestudio.spraute_engine.editor.assets;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.NativeImage.Format;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class ProjectImages {
    private static final Map<Path, ProjectImages.Loaded> LOADED = new HashMap<>();
    private static final int MAX_SIZE = 512;

    private ProjectImages() {
    }

    public static void registerAll() {
        for (Workspace.ProjectInfo info : Workspace.projects()) {
            try {
                register(Project.openReadOnly(info.dir()));
            } catch (Exception var3) {
                SprauteEngine.LOGGER.warn("Spraute: failed to load pictures of {}", info.dir(), var3);
            }
        }
    }

    public static void register(Project project) {
        for (Project.ImageRef ref : project.images()) {
            load(project, ref);
        }
    }

    public static ProjectImages.Loaded load(Project project, Project.ImageRef ref) {
        long modified;
        try {
            modified = Files.getLastModifiedTime(ref.path()).toMillis();
        } catch (IOException var10) {
            return null;
        }

        ProjectImages.Loaded old = LOADED.get(ref.path());
        if (old != null && old.modified == modified) {
            return old;
        } else {
            ResourceLocation rl = Images.texture(project.name(), ref.name());

            try {
                NativeImage img = read(ref.path());
                ProjectImages.Loaded l = new ProjectImages.Loaded(rl, img.getWidth(), img.getHeight(), modified);
                Runnable register = () -> Minecraft.getInstance().getTextureManager().register(rl, new DynamicTexture(img));
                if (RenderSystem.isOnRenderThread()) {
                    register.run();
                } else {
                    RenderSystem.recordRenderCall(register::run);
                }

                LOADED.put(ref.path(), l);
                return l;
            } catch (Throwable var9) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read picture {}", ref.path(), var9);
                return null;
            }
        }
    }

    public static int[] size(ResourceLocation texture) {
        for (ProjectImages.Loaded l : LOADED.values()) {
            if (l.texture().equals(texture)) {
                return new int[]{l.width(), l.height()};
            }
        }

        return new int[]{0, 0};
    }

    public static void forget(Path path) {
        ProjectImages.Loaded l = LOADED.remove(path);
        if (l != null) {
            RenderSystem.recordRenderCall(() -> Minecraft.getInstance().getTextureManager().release(l.texture()));
        }
    }

    private static NativeImage read(Path path) throws IOException {
        if (path.getFileName().toString().toLowerCase().endsWith(".png")) {
            NativeImage png;
            try (InputStream in = Files.newInputStream(path)) {
                png = NativeImage.read(in);
            }

            if (Math.max(png.getWidth(), png.getHeight()) <= 512) {
                return png;
            } else {
                double k = 512.0 / Math.max(png.getWidth(), png.getHeight());
                NativeImage scaled = new NativeImage(Math.max(1, (int)(png.getWidth() * k)), Math.max(1, (int)(png.getHeight() * k)), false);
                png.resizeSubRectTo(0, 0, png.getWidth(), png.getHeight(), scaled);
                png.close();
                return scaled;
            }
        } else {
            BufferedImage src = ImageIO.read(path.toFile());
            if (src == null) {
                throw new IOException("Неподдерживаемый формат картинки");
            } else {
                int w = src.getWidth();
                int h = src.getHeight();
                double k = Math.min(1.0, 512.0 / Math.max(w, h));
                int tw = Math.max(1, (int)Math.round(w * k));
                int th = Math.max(1, (int)Math.round(h * k));
                NativeImage img = new NativeImage(Format.RGBA, tw, th, false);

                for (int y = 0; y < th; y++) {
                    for (int x = 0; x < tw; x++) {
                        int argb = src.getRGB(Math.min(w - 1, (int)(x / k)), Math.min(h - 1, (int)(y / k)));
                        int abgr = argb & -16711936 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
                        img.setPixelRGBA(x, y, abgr);
                    }
                }

                return img;
            }
        }
    }

    public static List<String> names(Project project) {
        List<String> list = new ArrayList<>();
        if (project != null) {
            for (Project.ImageRef r : project.images()) {
                list.add(r.name());
            }
        }

        return list;
    }

    public record Loaded(ResourceLocation texture, int width, int height, long modified) {
    }
}
