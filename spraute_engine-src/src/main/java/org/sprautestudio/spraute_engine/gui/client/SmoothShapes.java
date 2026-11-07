package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.ui.Canvas;

final class SmoothShapes {
    private static final int SIZE = 128;
    private static final ResourceLocation CIRCLE = new ResourceLocation("spraute_engine", "gui/smooth_circle");
    private static boolean made;

    private SmoothShapes() {
    }

    private static void make() {
        if (!made) {
            made = true;
            NativeImage image = new NativeImage(128, 128, false);
            float middle = 64.0F;

            for (int y = 0; y < 128; y++) {
                for (int x = 0; x < 128; x++) {
                    float dx = x + 0.5F - middle;
                    float dy = y + 0.5F - middle;
                    float cover = Math.max(0.0F, Math.min(1.0F, middle - (float)Math.sqrt(dx * dx + dy * dy)));
                    image.setPixelRGBA(x, y, Math.round(cover * 255.0F) << 24 | 16777215);
                }
            }

            DynamicTexture texture = new DynamicTexture(image);
            Minecraft.getInstance().getTextureManager().register(CIRCLE, texture);
            texture.setFilter(true, false);
        }
    }

    static void round(Canvas c, int x, int y, int w, int h, int radius, int color) {
        if (w > 0 && h > 0 && color >>> 24 != 0) {
            int r = Math.min(radius, Math.min(w, h) / 2);
            if (r <= 0) {
                c.fill(x, y, w, h, color);
            } else {
                make();
                c.fill(x + r, y, w - 2 * r, h, color);
                c.fill(x, y + r, r, h - 2 * r, color);
                c.fill(x + w - r, y + r, r, h - 2 * r, color);
                c.flush();
                GuiView.blit(c, CIRCLE, x, y, r, r, 0.0F, 0.0F, 0.5F, 0.5F, color, 1.0F);
                GuiView.blit(c, CIRCLE, x + w - r, y, r, r, 0.5F, 0.0F, 1.0F, 0.5F, color, 1.0F);
                GuiView.blit(c, CIRCLE, x, y + h - r, r, r, 0.0F, 0.5F, 0.5F, 1.0F, color, 1.0F);
                GuiView.blit(c, CIRCLE, x + w - r, y + h - r, r, r, 0.5F, 0.5F, 1.0F, 1.0F, color, 1.0F);
            }
        }
    }

    static void halo(Canvas c, int x, int y, int w, int h, int radius, int size, int color, float strength, int shiftY) {
        if (size > 0 && !(strength <= 0.0F)) {
            float base = (color >>> 24) / 255.0F * strength;

            for (int i = size; i > 0; i--) {
                float a = base * (1.0F - (float)i / (size + 1)) * 2.2F / size;
                round(c, x - i, y - i + shiftY, w + 2 * i, h + 2 * i, radius + i, Math.round(Math.min(1.0F, a) * 255.0F) << 24 | color & 16777215);
            }
        }
    }
}
