package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class LiveDraw {
    private LiveDraw() {
    }

    public static ResourceLocation tex(String project, String own, EngineConfig.Skin skin, ResourceLocation def) {
        if (own != null && !own.isBlank()) {
            String v = own.trim();
            if (!v.contains(":")) {
                return Images.texture(project, v);
            }

            ResourceLocation rl = ResourceLocation.tryParse(v);
            if (rl != null) {
                return rl;
            }
        }

        return skin != null && skin.isSet() ? Skins.texture(project, skin) : def;
    }

    public static ResourceLocation tex(String project, GuiDoc.Element e, String prop, EngineConfig.Skin skin, ResourceLocation def) {
        return tex(project, e.get(prop), skin, def);
    }

    public static void image(Gfx g, GuiDoc.Element e, ResourceLocation rl, int x, int y, int w, int h, float op) {
        if (!e.b("nineSlice")) {
            image(g, rl, x, y, w, h, op);
        } else {
            EngineConfig.Skin skin = new EngineConfig.Skin();
            skin.nine = true;
            skin.left = e.i("sliceLeft");
            skin.top = e.i("sliceTop");
            skin.right = e.i("sliceRight");
            skin.bottom = e.i("sliceBottom");
            skin.scale = e.f("sliceScale");
            skin.tileCenter = e.b("sliceTileCenter");
            skin.tileEdges = e.b("sliceTileEdges");
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);

            try {
                Skins.draw(g, rl, skin, x, y, w, h);
            } finally {
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    public static void image(Gfx g, ResourceLocation rl, int x, int y, int w, int h, float op) {
        if (w > 0 && h > 0) {
            int[] s = Skins.size(rl);
            int tw = s != null && s[0] > 0 ? s[0] : w;
            int th = s != null && s[1] > 0 ? s[1] : h;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);
            g.blit(rl, x, y, w, h, 0.0F, 0.0F, tw, th, tw, th);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void sheet(Gfx g, ResourceLocation rl, int x, int y, int w, int h, int u, int v, int uw, int vh, int tw, int th, float op) {
        if (w > 0 && h > 0) {
            int[] s = Skins.size(rl);
            int rw = s != null && s[0] > 0 ? s[0] : tw;
            int rh = s != null && s[1] > 0 ? s[1] : th;
            float kx = (float)rw / tw;
            float ky = (float)rh / th;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);
            g.blit(rl, x, y, w, h, u * kx, v * ky, Math.round(uw * kx), Math.round(vh * ky), rw, rh);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static int a(int color, float op) {
        int al = Math.round((color >>> 24) * Math.max(0.0F, Math.min(1.0F, op)));
        return al << 24 | color & 16777215;
    }

    public static void text(Gfx g, Font font, String s, float x, float y, float scale, int color, boolean shadow) {
        if (s != null && !s.isEmpty() && color >>> 24 >= 5) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, s, 0, 0, color, shadow);
            g.pose().popPose();
        }
    }

    public static void text(Gfx g, Font font, Component s, float x, float y, float scale, int color, boolean shadow) {
        if (color >>> 24 >= 5) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, s, 0, 0, color, shadow);
            g.pose().popPose();
        }
    }

    public static void text(Gfx g, Font font, FormattedCharSequence s, float x, float y, float scale, int color, boolean shadow) {
        if (color >>> 24 >= 5) {
            g.pose().pushPose();
            g.pose().translate(x, y, 0.0F);
            g.pose().scale(scale, scale, 1.0F);
            g.drawString(font, s, 0, 0, color, shadow);
            g.pose().popPose();
        }
    }

    public static void item(Gfx g, ItemStack s, float x, float y, float scale) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.renderItem(s, 0, 0);
        g.pose().popPose();
    }

    public static void clip(Gfx g, int x, int y, int w, int h) {
        int[] c = ChatHeads.screenRect(g, x, y, w, h);
        g.enableScissor(c[0], c[1], c[2], c[3]);
    }

    public static boolean in(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }
}
