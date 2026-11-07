package org.sprautestudio.spraute_engine.editor.engine;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class GameShot {
    private static final ResourceLocation ID = new ResourceLocation("spraute_engine", "studio/game_shot");
    private static DynamicTexture texture;
    private static int tw;
    private static int th;

    private GameShot() {
    }

    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.screen == null) {
            try {
                NativeImage img = Screenshot.takeScreenshot(mc.getMainRenderTarget());
                if (texture != null) {
                    texture.close();
                }

                texture = new DynamicTexture(img);
                tw = img.getWidth();
                th = img.getHeight();
                mc.getTextureManager().register(ID, texture);
            } catch (RuntimeException var2) {
                texture = null;
            }
        }
    }

    public static void draw(Canvas c, int x, int y, int w, int h) {
        if (texture != null && tw > 0 && th > 0) {
            float ar = (float)w / h;
            float sar = (float)tw / th;
            int u = 0;
            int v = 0;
            int uw = tw;
            int vh = th;
            if (sar > ar) {
                uw = Math.round(th * ar);
                u = (tw - uw) / 2;
            } else {
                vh = Math.round(tw / ar);
                v = (th - vh) / 2;
            }

            c.flush();
            c.g.blit(ID, x, y, w, h, u, v, uw, vh, tw, th);
            c.flush();
        } else {
            c.g.fillGradient(x, y, x + w, y + h, -14011056, -15855592);
            c.g.fillGradient(x, y + h * 2 / 3, x + w, y + h, 0, -14931432);
        }
    }
}
