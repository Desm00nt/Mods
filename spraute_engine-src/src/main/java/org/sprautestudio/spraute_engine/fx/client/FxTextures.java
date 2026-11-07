package org.sprautestudio.spraute_engine.fx.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class FxTextures {
    public static final List<String> BUILT_IN = List.of("glow", "spark", "smoke", "ring", "star", "flare", "beam", "circle", "square");
    private static final Map<String, ResourceLocation> MADE = new HashMap<>();
    private static final int N = 64;

    private FxTextures() {
    }

    public static ResourceLocation get(String name, String project) {
        if (name != null && name.startsWith("image:")) {
            return Images.texture(project == null ? "" : project, name.substring(6));
        } else {
            String key = name != null && BUILT_IN.contains(name) ? name : "glow";
            ResourceLocation rl = MADE.get(key);
            if (rl != null) {
                return rl;
            } else {
                NativeImage img = new NativeImage(key.equals("smoke") ? 128 : 64, key.equals("smoke") ? 128 : 64, false);
                draw(key, img);
                rl = new ResourceLocation("spraute_engine", "fx/" + key);
                DynamicTexture tex = new DynamicTexture(img);
                tex.setFilter(true, false);
                Minecraft.getInstance().getTextureManager().register(rl, tex);
                MADE.put(key, rl);
                return rl;
            }
        }
    }

    private static void put(NativeImage img, int x, int y, float a) {
        int al = Math.max(0, Math.min(255, Math.round(a * 255.0F)));
        img.setPixelRGBA(x, y, al << 24 | 16777215);
    }

    private static void draw(String key, NativeImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        Random rnd = new Random(key.hashCode());
        float[][] grid = new float[9][9];

        for (float[] row : grid) {
            for (int i = 0; i < row.length; i++) {
                row[i] = rnd.nextFloat();
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float u = (x + 0.5F) / w * 2.0F - 1.0F;
                float v = (y + 0.5F) / h * 2.0F - 1.0F;
                float r = (float)Math.sqrt(u * u + v * v);

                put(
                    img,
                    x,
                    y,
                    Math.max(
                        0.0F,
                        Math.min(
                            1.0F,
                            switch (key) {
                                case "spark" -> {
                                    float across = Math.abs(v);
                                    float along = Math.abs(u);
                                    float a = (float)(Math.exp(-across * across * 60.0F) * (1.0 - Math.pow(along, 3.0)));
                                    yield a + (float)(Math.exp(-across * across * 8.0F) * 0.35 * (1.0F - along));
                                }
                                case "smoke" -> {
                                    float n = noise(grid, (u + 1.0F) * 4.0F, (v + 1.0F) * 4.0F) * 0.6F
                                        + noise(grid, (u + 1.0F) * 8.0F % 8.0F, (v + 1.0F) * 8.0F % 8.0F) * 0.4F;
                                    float edge = (float)Math.max(0.0, 1.0 - Math.pow(r, 1.6));
                                    float a = Math.max(0.0F, edge * (0.55F + 0.7F * n) - 0.12F);
                                    yield Math.min(1.0F, a * 1.2F);
                                }
                                case "ring" -> {
                                    float d = (r - 0.78F) / 0.09F;
                                    float ring = (float)Math.exp(-d * d) + (float)Math.exp(-Math.pow((r - 0.78F) / 0.25F, 2.0)) * 0.25F;
                                    yield r > 1.0F ? 0.0F : ring;
                                }
                                case "star" -> {
                                    float cross = (float)(
                                        Math.exp(-Math.abs(u) * 14.0F) * (1.0F - Math.abs(v)) + Math.exp(-Math.abs(v) * 14.0F) * (1.0F - Math.abs(u))
                                    );
                                    yield Math.min(1.0F, cross * 1.3F + (float)Math.exp(-r * r * 18.0F));
                                }
                                case "flare" -> {
                                    float core = (float)Math.exp(-r * r * 30.0F);
                                    float halo = (float)Math.exp(-r * r * 3.2) * 0.55F;
                                    float streak = (float)(Math.exp(-Math.abs(v) * 40.0F) * Math.max(0.0F, 1.0F - Math.abs(u)) * 0.6);
                                    yield Math.min(1.0F, core + halo + streak);
                                }
                                case "beam" -> {
                                    float across = Math.abs(u);
                                    float a = (float)(Math.exp(-across * across * 10.0F) * 0.75 + Math.exp(-across * across * 70.0F) * 0.5);
                                    yield Math.min(1.0F, a);
                                }
                                case "circle" -> Math.max(0.0F, Math.min(1.0F, (1.0F - r) * w / 4.0F));
                                case "square" -> Math.max(Math.abs(u), Math.abs(v)) < 0.92F ? 1.0F : 0.0F;
                                default -> (float)Math.exp(-r * r * 4.5) * (r < 1.0F ? 1 : 0);
                            }
                        )
                    )
                );
            }
        }
    }

    private static float noise(float[][] g, float x, float y) {
        int xi = (int)Math.floor(x);
        int yi = (int)Math.floor(y);
        float xf = x - xi;
        float yf = y - yi;
        xi = Math.floorMod(xi, 8);
        yi = Math.floorMod(yi, 8);
        float sx = xf * xf * (3.0F - 2.0F * xf);
        float sy = yf * yf * (3.0F - 2.0F * yf);
        float a = g[yi][xi];
        float b = g[yi][xi + 1];
        float c = g[yi + 1][xi];
        float d = g[yi + 1][xi + 1];
        return a + (b - a) * sx + (c + (d - c) * sx - (a + (b - a) * sx)) * sy;
    }
}
