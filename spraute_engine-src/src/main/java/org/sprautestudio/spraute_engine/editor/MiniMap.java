package org.sprautestudio.spraute_engine.editor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class MiniMap {
    private static int counter;
    private final ResourceLocation id;
    private DynamicTexture tex;
    private int texW;
    private int texH;
    private double bx0;
    private double bz0;
    private double step;
    private double builtScale;
    private long builtAt;
    private String builtDim;
    public boolean enabled;
    public float alpha;
    private static final int MAX = 384;
    private static final int EMPTY = -15395300;

    public MiniMap() {
        this.id = new ResourceLocation("spraute_engine", "minimap/" + counter++);
        this.step = 1.0;
        this.builtScale = -1.0;
        this.builtDim = "";
        this.enabled = true;
        this.alpha = 0.85F;
    }

    public void draw(Canvas c, int x, int y, int w, int h, double cx, double cz, double scale) {
        if (this.enabled && w > 0 && h > 0 && !(scale <= 0.0)) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level != null) {
                double viewW = w / scale;
                double viewH = h / scale;
                double vx0 = cx - viewW / 2.0;
                double vz0 = cz - viewH / 2.0;
                boolean inside = this.tex != null
                    && vx0 >= this.bx0
                    && vz0 >= this.bz0
                    && vx0 + viewW <= this.bx0 + this.texW * this.step
                    && vz0 + viewH <= this.bz0 + this.texH * this.step;
                boolean zoomed = this.builtScale <= 0.0 || scale > this.builtScale * 1.8 || scale < this.builtScale / 1.8;
                String dim = level.dimension().location().toString();
                if (!inside || zoomed || !dim.equals(this.builtDim) || System.currentTimeMillis() - this.builtAt > 4000L) {
                    this.build(level, cx, cz, viewW, viewH, scale);
                    this.builtDim = dim;
                }

                if (this.tex != null) {
                    c.flush();
                    Gfx g = c.g;
                    float sx = (float)(x + w / 2.0 + (this.bx0 - cx) * scale);
                    float sz = (float)(y + h / 2.0 + (this.bz0 - cz) * scale);
                    float sw = (float)(this.texW * this.step * scale);
                    float sh = (float)(this.texH * this.step * scale);
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
                    g.pose().pushPose();
                    g.pose().translate(sx, sz, 0.0F);
                    g.pose().scale(sw / this.texW, sh / this.texH, 1.0F);
                    g.blit(this.id, 0, 0, 0.0F, 0.0F, this.texW, this.texH, this.texW, this.texH);
                    g.pose().popPose();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    g.flush();
                }
            }
        }
    }

    private void build(ClientLevel level, double cx, double cz, double viewW, double viewH, double scale) {
        double areaW = viewW * 2.0;
        double areaH = viewH * 2.0;
        this.step = Math.max(1.0, Math.ceil(Math.max(areaW, areaH) / 384.0));
        int tw = (int)Math.max(8.0, Math.min(384.0, Math.ceil(areaW / this.step)));
        int th = (int)Math.max(8.0, Math.min(384.0, Math.ceil(areaH / this.step)));
        this.bx0 = Math.floor(cx - tw * this.step / 2.0);
        this.bz0 = Math.floor(cz - th * this.step / 2.0);
        this.builtScale = scale;
        this.builtAt = System.currentTimeMillis();
        if (this.tex == null || tw != this.texW || th != this.texH) {
            if (this.tex != null) {
                this.tex.close();
            }

            this.texW = tw;
            this.texH = th;
            this.tex = new DynamicTexture(tw, th, false);
            Minecraft.getInstance().getTextureManager().register(this.id, this.tex);
        }

        NativeImage img = this.tex.getPixels();
        if (img != null) {
            MutableBlockPos pos = new MutableBlockPos();
            int s = (int)this.step;

            for (int tz = 0; tz < th; tz++) {
                int wz = (int)(this.bz0 + tz * this.step);
                int prevNorth = Integer.MIN_VALUE;

                for (int tx = 0; tx < tw; tx++) {
                    int wx = (int)(this.bx0 + tx * this.step);
                    int argb = -15395300;
                    if (level.hasChunk(wx >> 4, wz >> 4)) {
                        int top = level.getHeight(Types.WORLD_SURFACE, wx, wz) - 1;
                        int north = level.hasChunk(wx >> 4, wz - s >> 4) ? level.getHeight(Types.WORLD_SURFACE, wx, wz - s) - 1 : top;
                        pos.set(wx, top, wz);
                        BlockState st = level.getBlockState(pos);
                        int mapColor = Mc.mapColor(st, level, pos);
                        int rgb = mapColor < 0 ? 2105376 : mapColor;
                        float shade = top > north ? 1.0F : (top < north ? 0.7F : 0.86F);
                        if (Mc.mapWater(st, level, pos)) {
                            int depth = 0;
                            MutableBlockPos d = new MutableBlockPos(wx, top - 1, wz);

                            while (depth < 10 && level.getBlockState(d).getFluidState().isSource()) {
                                depth++;
                                d.move(0, -1, 0);
                            }

                            shade = depth > 6 ? 0.68F : (depth > 2 ? 0.82F : 0.95F);
                        }

                        int r = (int)((rgb >> 16 & 0xFF) * shade);
                        int gg = (int)((rgb >> 8 & 0xFF) * shade);
                        int b = (int)((rgb & 0xFF) * shade);
                        argb = 0xFF000000 | r << 16 | gg << 8 | b;
                    }

                    int abgr = argb & -16711936 | argb >> 16 & 0xFF | (argb & 0xFF) << 16;
                    img.setPixelRGBA(tx, tz, abgr);
                }
            }

            this.tex.upload();
        }
    }

    public void close() {
        if (this.tex != null) {
            this.tex.close();
            Minecraft.getInstance().getTextureManager().release(this.id);
            this.tex = null;
        }
    }
}
