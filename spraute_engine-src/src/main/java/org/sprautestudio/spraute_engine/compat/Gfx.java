package org.sprautestudio.spraute_engine.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public final class Gfx {
    public final GuiGraphics raw;
    private static int offscreenHeight;
    private static final Deque<int[]> OFFSCREEN_CLIPS = new ArrayDeque<>();

    private Gfx(GuiGraphics raw) {
        this.raw = raw;
    }

    public static Gfx of(GuiGraphics g) {
        return new Gfx(g);
    }

    public PoseStack pose() {
        return this.raw.pose();
    }

    public BufferSource bufferSource() {
        return this.raw.bufferSource();
    }

    public void flush() {
        this.raw.flush();
    }

    public int guiWidth() {
        return this.raw.guiWidth();
    }

    public int guiHeight() {
        return this.raw.guiHeight();
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        this.raw.fill(x1, y1, x2, y2, color);
    }

    public void fillGradient(int x1, int y1, int x2, int y2, int top, int bottom) {
        this.raw.fillGradient(x1, y1, x2, y2, top, bottom);
    }

    public void renderOutline(int x, int y, int w, int h, int color) {
        this.raw.renderOutline(x, y, w, h, color);
    }

    public static Gfx offscreen() {
        Minecraft mc = Minecraft.getInstance();
        return new Gfx(new GuiGraphics(mc, mc.renderBuffers().bufferSource()));
    }

    public static void beginOffscreen(int height) {
        offscreenHeight = height;
        OFFSCREEN_CLIPS.clear();
    }

    public static void endOffscreen() {
        offscreenHeight = 0;
        OFFSCREEN_CLIPS.clear();
        RenderSystem.disableScissor();
    }

    private static void applyOffscreenClip(int[] c) {
        RenderSystem.enableScissor(c[0], offscreenHeight - c[3], Math.max(0, c[2] - c[0]), Math.max(0, c[3] - c[1]));
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        if (offscreenHeight <= 0) {
            this.raw.enableScissor(x1, y1, x2, y2);
        } else {
            this.raw.flush();
            int[] clip = new int[]{x1, y1, x2, y2};
            int[] outer = OFFSCREEN_CLIPS.peek();
            if (outer != null) {
                clip = new int[]{Math.max(x1, outer[0]), Math.max(y1, outer[1]), Math.min(x2, outer[2]), Math.min(y2, outer[3])};
            }

            OFFSCREEN_CLIPS.push(clip);
            applyOffscreenClip(clip);
        }
    }

    public void disableScissor() {
        if (offscreenHeight <= 0) {
            this.raw.disableScissor();
        } else {
            this.raw.flush();
            OFFSCREEN_CLIPS.poll();
            int[] outer = OFFSCREEN_CLIPS.peek();
            if (outer != null) {
                applyOffscreenClip(outer);
            } else {
                RenderSystem.disableScissor();
            }
        }
    }

    public int drawString(Font font, String s, int x, int y, int color) {
        return this.raw.drawString(font, s, x, y, color);
    }

    public int drawString(Font font, String s, int x, int y, int color, boolean shadow) {
        return this.raw.drawString(font, s, x, y, color, shadow);
    }

    public int drawString(Font font, Component s, int x, int y, int color) {
        return this.raw.drawString(font, s, x, y, color);
    }

    public int drawString(Font font, Component s, int x, int y, int color, boolean shadow) {
        return this.raw.drawString(font, s, x, y, color, shadow);
    }

    public int drawString(Font font, FormattedCharSequence s, int x, int y, int color) {
        return this.raw.drawString(font, s, x, y, color);
    }

    public int drawString(Font font, FormattedCharSequence s, int x, int y, int color, boolean shadow) {
        return this.raw.drawString(font, s, x, y, color, shadow);
    }

    public void drawCenteredString(Font font, String s, int x, int y, int color) {
        this.raw.drawCenteredString(font, s, x, y, color);
    }

    public void drawCenteredString(Font font, Component s, int x, int y, int color) {
        this.raw.drawCenteredString(font, s, x, y, color);
    }

    public void blit(ResourceLocation t, int x, int y, int u, int v, int w, int h) {
        this.raw.blit(t, x, y, u, v, w, h);
    }

    public void blit(ResourceLocation t, int x, int y, float u, float v, int w, int h, int texW, int texH) {
        this.raw.blit(t, x, y, u, v, w, h, texW, texH);
    }

    public void blit(ResourceLocation t, int x, int y, int w, int h, float u, float v, int uw, int vh, int texW, int texH) {
        this.raw.blit(t, x, y, w, h, u, v, uw, vh, texW, texH);
    }

    public void renderItem(ItemStack stack, int x, int y) {
        this.raw.renderItem(stack, x, y);
    }

    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        this.raw.renderItemDecorations(font, stack, x, y);
    }

    public void renderTooltip(Font font, ItemStack stack, int x, int y) {
        this.raw.renderTooltip(font, stack, x, y);
    }

    public void renderTooltip(Font font, Component text, int x, int y) {
        this.raw.renderTooltip(font, text, x, y);
    }
}
