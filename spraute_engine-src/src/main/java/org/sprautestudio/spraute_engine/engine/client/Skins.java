package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class Skins {
    private static final Map<ResourceLocation, int[]> SIZES = new HashMap<>();

    private Skins() {
    }

    public static ResourceLocation texture(String project, EngineConfig.Skin skin) {
        return skin.image.contains(":") ? ResourceLocation.tryParse(skin.image) : Images.texture(project, skin.image);
    }

    public static void invalidate() {
        SIZES.clear();
    }

    public static int[] size(ResourceLocation rl) {
        int[] s = ProjectImages.size(rl);
        if (s[0] > 0) {
            return s;
        } else if (rl == null) {
            return new int[]{0, 0};
        } else {
            int[] known = SIZES.get(rl);
            if (known != null) {
                return known;
            } else {
                try {
                    int[] var6;
                    try (
                        InputStream in = Minecraft.getInstance().getResourceManager().open(rl);
                        NativeImage img = NativeImage.read(in);
                    ) {
                        int[] loaded = new int[]{img.getWidth(), img.getHeight()};
                        SIZES.put(rl, loaded);
                        var6 = loaded;
                    }

                    return var6;
                } catch (Exception var11) {
                    return new int[]{0, 0};
                }
            }
        }
    }

    public static boolean exists(ResourceLocation rl) {
        return size(rl)[0] > 0;
    }

    public static void drawSheet(Gfx g, String project, EngineConfig.Skin skin, ResourceLocation def, int x, int y, int w, int h, int u, int v, int tw, int th) {
        if (skin != null && skin.isSet()) {
            ResourceLocation rl = texture(project, skin);
            int[] sz = size(rl);
            int rw = sz != null && sz[0] > 0 ? sz[0] : tw;
            int rh = sz != null && sz[1] > 0 ? sz[1] : th;
            float kx = (float)rw / tw;
            float ky = (float)rh / th;
            RenderSystem.enableBlend();
            g.blit(rl, x, y, w, h, u * kx, v * ky, Math.round(w * kx), Math.round(h * ky), rw, rh);
        } else {
            g.blit(def, x, y, u, v, w, h, tw, th);
        }
    }

    public static void draw(Gfx g, String project, EngineConfig.Skin skin, int x, int y, int w, int h) {
        if (skin != null && skin.isSet()) {
            draw(g, texture(project, skin), skin, x, y, w, h);
        }
    }

    public static void draw(Gfx g, ResourceLocation rl, EngineConfig.Skin skin, int x, int y, int w, int h) {
        if (w > 0 && h > 0) {
            int[] s = size(rl);
            int tw = s[0];
            int th = s[1];
            if (tw > 0 && th > 0) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                if (skin != null && skin.nine) {
                    int l = clamp(skin.left, 0, tw / 2);
                    int r = clamp(skin.right, 0, tw - l - 1);
                    int t = clamp(skin.top, 0, th / 2);
                    int b = clamp(skin.bottom, 0, th - t - 1);
                    float k = skin.scale <= 0.0F ? 1.0F : skin.scale;
                    int dl = Math.round(l * k);
                    int dr = Math.round(r * k);
                    int dt = Math.round(t * k);
                    int db = Math.round(b * k);
                    if (dl + dr > w) {
                        float f = (float)w / (dl + dr);
                        dl = (int)(dl * f);
                        dr = w - dl;
                    }

                    if (dt + db > h) {
                        float f = (float)h / (dt + db);
                        dt = (int)(dt * f);
                        db = h - dt;
                    }

                    int mw = w - dl - dr;
                    int mh = h - dt - db;
                    int cw = tw - l - r;
                    int ch = th - t - b;
                    int[] dxs = new int[]{x, x + dl, x + w - dr};
                    int[] dws = new int[]{dl, mw, dr};
                    int[] dys = new int[]{y, y + dt, y + h - db};
                    int[] dhs = new int[]{dt, mh, db};
                    int[] us = new int[]{0, l, tw - r};
                    int[] uws = new int[]{l, cw, r};
                    int[] vs = new int[]{0, t, th - b};
                    int[] vhs = new int[]{t, ch, b};

                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            if (dws[col] > 0 && dhs[row] > 0 && uws[col] > 0 && vhs[row] > 0) {
                                boolean tileX = col == 1 && (row == 1 ? skin.tileCenter : skin.tileEdges);
                                boolean tileY = row == 1 && (col == 1 ? skin.tileCenter : skin.tileEdges);
                                if (!tileX && !tileY) {
                                    g.blit(rl, dxs[col], dys[row], dws[col], dhs[row], us[col], vs[row], uws[col], vhs[row], tw, th);
                                } else {
                                    int stepX = tileX ? Math.max(1, Math.round(uws[col] * k)) : dws[col];
                                    int stepY = tileY ? Math.max(1, Math.round(vhs[row] * k)) : dhs[row];

                                    for (int oy = 0; oy < dhs[row]; oy += stepY) {
                                        int drawH = Math.min(stepY, dhs[row] - oy);
                                        int srcH = tileY ? Math.max(1, Math.round((float)(vhs[row] * drawH) / stepY)) : vhs[row];

                                        for (int ox = 0; ox < dws[col]; ox += stepX) {
                                            int drawW = Math.min(stepX, dws[col] - ox);
                                            int srcW = tileX ? Math.max(1, Math.round((float)(uws[col] * drawW) / stepX)) : uws[col];
                                            g.blit(rl, dxs[col] + ox, dys[row] + oy, drawW, drawH, us[col], vs[row], srcW, srcH, tw, th);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    g.blit(rl, x, y, w, h, 0.0F, 0.0F, tw, th, tw, th);
                }
            }
        }
    }

    public static int[] filledPart(int x, int y, int w, int h, float fraction, String dir) {
        float f = Math.max(0.0F, Math.min(1.0F, fraction));
        String var7 = dir == null ? "right" : dir;

        return switch (var7) {
            case "left" -> new int[]{x + w - Math.round(w * f), y, Math.round(w * f), h};
            case "up" -> new int[]{x, y + h - Math.round(h * f), w, Math.round(h * f)};
            case "down" -> new int[]{x, y, w, Math.round(h * f)};
            default -> new int[]{x, y, Math.round(w * f), h};
        };
    }

    public static void drawFill(Gfx g, String project, EngineConfig.Skin skin, int x, int y, int w, int h, float fraction) {
        drawFill(g, project, skin, x, y, w, h, fraction, "right");
    }

    public static void drawFill(Gfx g, String project, EngineConfig.Skin skin, int x, int y, int w, int h, float fraction, String dir) {
        if (skin != null && skin.isSet() && !(fraction <= 0.0F)) {
            ResourceLocation rl = texture(project, skin);
            int[] part = filledPart(x, y, w, h, fraction, dir);
            if (part[2] > 0 && part[3] > 0) {
                if (skin.nine) {
                    draw(g, rl, skin, part[0], part[1], part[2], part[3]);
                } else {
                    int[] s = size(rl);
                    if (s[0] > 0) {
                        RenderSystem.enableBlend();
                        RenderSystem.defaultBlendFunc();
                        float f = Math.min(1.0F, fraction);
                        int uw = s[0];
                        int vh = s[1];
                        int u = 0;
                        int v = 0;
                        String var17 = dir == null ? "right" : dir;
                        switch (var17) {
                            case "left":
                                uw = Math.max(1, Math.round(s[0] * f));
                                u = s[0] - uw;
                                break;
                            case "up":
                                vh = Math.max(1, Math.round(s[1] * f));
                                v = s[1] - vh;
                                break;
                            case "down":
                                vh = Math.max(1, Math.round(s[1] * f));
                                break;
                            default:
                                uw = Math.max(1, Math.round(s[0] * f));
                        }

                        g.blit(rl, part[0], part[1], part[2], part[3], u, v, uw, vh, s[0], s[1]);
                    }
                }
            }
        }
    }

    public static void drawFillTexture(Gfx g, ResourceLocation rl, int x, int y, int w, int h, float fraction) {
        int[] s = size(rl);
        if (s[0] > 0 && !(fraction <= 0.0F)) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float f = Math.min(1.0F, fraction);
            g.blit(rl, x, y, Math.round(w * f), h, 0.0F, 0.0F, Math.max(1, Math.round(s[0] * f)), s[1], s[0], s[1]);
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
