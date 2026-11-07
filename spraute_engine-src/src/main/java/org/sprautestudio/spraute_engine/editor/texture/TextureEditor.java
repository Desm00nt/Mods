package org.sprautestudio.spraute_engine.editor.texture;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.ColorPicker;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.Label;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class TextureEditor extends EditorTab {
    public final Project.ImageRef image;
    private PixelDoc doc;
    private String loadError;
    private TextureEditor.Tool tool = TextureEditor.Tool.PENCIL;
    private int primary = -13948117;
    private int secondary = -1;
    private int size = 1;
    private boolean pixelPerfect = true;
    private boolean filled = false;
    private boolean contiguous = true;
    private boolean mirrorX;
    private boolean mirrorY;
    private boolean grid = true;
    private boolean skinGuide = true;
    private int tolerance = 0;
    private int gradColors = 0;
    private int gradSteps = 5;
    private int gradDither = 2;
    private int gradArea = 0;
    private float gradSpread = 0.5F;
    private float hueShift = 8.0F;
    private boolean gradRadial;
    private boolean gradReverse;
    private final List<Integer> recent = new ArrayList<>();
    private static final int[] ENDESGA = new int[]{
        12470831,
        14120515,
        15389866,
        14984818,
        12087120,
        7552569,
        4073265,
        10626611,
        14957380,
        16217634,
        16690740,
        16705377,
        6539085,
        4098376,
        2513986,
        1653822,
        1199753,
        39387,
        2943221,
        16777215,
        12635100,
        9149364,
        5925256,
        3818598,
        2501444,
        1578021,
        16711748,
        6830188,
        11882632,
        16151930,
        15251350,
        12748137
    };
    private boolean paletteFromImage = true;
    private int zoom = 8;
    private int panX;
    private int panY;
    private boolean fitted;
    private int canvasX;
    private int canvasY;
    private int canvasW;
    private int canvasH;
    private static final int TOP = 30;
    private static final int LEFT = 44;
    private static final int RIGHT = 240;
    private static final int STATUS = 20;
    private boolean drawing;
    private boolean panning;
    private boolean moving;
    private int button;
    private int sx;
    private int sy;
    private int lx;
    private int ly;
    private int curX = -1;
    private int curY = -1;
    private double panFromX;
    private double panFromY;
    private int[] base;
    private boolean[] touched;
    private final List<int[]> strokePts = new ArrayList<>();
    private boolean[] selection;
    private boolean[] lifted;
    private boolean[] gradMask;
    private int[] floatPx;
    private boolean selectAdd;
    private DynamicTexture tex;
    private ResourceLocation texId;
    private int texW;
    private int texH;
    private int texVersion = -1;
    private DynamicTexture checker;
    private ResourceLocation checkerId;
    private int checkerW;
    private int checkerH;
    private List<Integer> paletteCache;
    private int paletteVersion = -1;
    private int drawVersion;
    private static int counter;
    private final List<TextureEditor.Hit> hits = new ArrayList<>();
    private int[] clipboard;
    private int clipX;
    private int clipY;
    private int clipW;
    private int clipH;
    private int layerScroll;
    private static final int[] ZOOMS = new int[]{1, 2, 3, 4, 5, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56, 64};

    public TextureEditor(Project project, Project.ImageRef image) {
        super(project, null);
        this.image = image;
        this.focusable = true;

        try {
            this.doc = PixelDoc.load(image.path());
        } catch (IOException var4) {
            this.loadError = var4.getMessage();
        }
    }

    @Override
    public String title() {
        return this.image.name();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        this.hits.clear();
        c.fill(this.x, this.y, this.w, this.h, -15724266);
        if (this.doc == null) {
            c.textCentered("Картинка не открылась: " + this.loadError, this.x + this.w / 2, this.y + this.h / 2, -1023878);
        } else {
            this.canvasX = this.x + 44;
            this.canvasY = this.y + 30;
            this.canvasW = this.w - 44 - 240;
            this.canvasH = this.h - 30 - 20;
            if (!this.fitted && this.canvasW > 0) {
                this.fit();
            }

            this.paintCanvas(c, mx, my);
            this.paintTopBar(c, mx, my);
            this.paintTools(c, mx, my);
            this.paintRight(c, mx, my);
            this.paintStatus(c, mx, my);
        }
    }

    private void fit() {
        this.fitted = true;
        this.zoom = Math.max(1, Math.min(48, Math.min((this.canvasW - 40) / this.doc.w, (this.canvasH - 40) / this.doc.h)));
        this.panX = 0;
        this.panY = 0;
    }

    private int originX() {
        return this.canvasX + (this.canvasW - this.doc.w * this.zoom) / 2 + this.panX;
    }

    private int originY() {
        return this.canvasY + (this.canvasH - this.doc.h * this.zoom) / 2 + this.panY;
    }

    private void paintCanvas(Canvas c, int mx, int my) {
        c.pushClip(this.canvasX, this.canvasY, this.canvasW, this.canvasH);
        c.fill(this.canvasX, this.canvasY, this.canvasW, this.canvasH, -15921646);
        int ox = this.originX();
        int oy = this.originY();
        int iw = this.doc.w * this.zoom;
        int ih = this.doc.h * this.zoom;
        c.shadow(ox, oy, iw, ih, 2, 10);
        this.uploadTexture();
        if (this.checkerId != null) {
            c.g.blit(this.checkerId, ox, oy, iw, ih, 0.0F, 0.0F, this.doc.w * 2, this.doc.h * 2, this.doc.w * 2, this.doc.h * 2);
        }

        if (this.texId != null) {
            RenderSystem.enableBlend();
            c.g.blit(this.texId, ox, oy, iw, ih, 0.0F, 0.0F, this.doc.w, this.doc.h, this.doc.w, this.doc.h);
        }

        if (this.skinGuide && this.isSkinSize()) {
            for (TextureEditor.Part p : this.parts()) {
                c.outline(ox + p.x * this.zoom, oy + p.y * this.zoom, p.w * this.zoom, p.h * this.zoom, p.color);
            }
        }

        if (this.grid && this.zoom >= 6) {
            int gc = this.zoom >= 12 ? 587202559 : 352321535;

            for (int i = 1; i < this.doc.w; i++) {
                int gx = ox + i * this.zoom;
                if (gx >= this.canvasX && gx < this.canvasX + this.canvasW) {
                    c.fill(gx, Math.max(oy, this.canvasY), 1, Math.min(ih, this.canvasH), i % 8 == 0 ? 872415231 : gc);
                }
            }

            for (int ix = 1; ix < this.doc.h; ix++) {
                int gy = oy + ix * this.zoom;
                if (gy >= this.canvasY && gy < this.canvasY + this.canvasH) {
                    c.fill(Math.max(ox, this.canvasX), gy, Math.min(iw, this.canvasW), 1, ix % 8 == 0 ? 872415231 : gc);
                }
            }
        }

        c.outline(ox - 1, oy - 1, iw + 2, ih + 2, -11907486);
        if (this.selection != null) {
            long t = System.currentTimeMillis() / 120L;

            for (int py = 0; py < this.doc.h; py++) {
                for (int px = 0; px < this.doc.w; px++) {
                    int ixx = py * this.doc.w + px;
                    if (this.selection[ixx]) {
                        int col = (px + py + t & 3L) < 2L ? -1 : -15658735;
                        if (px == 0 || !this.selection[ixx - 1]) {
                            c.fill(ox + px * this.zoom, oy + py * this.zoom, 1, this.zoom, col);
                        }

                        if (px == this.doc.w - 1 || !this.selection[ixx + 1]) {
                            c.fill(ox + (px + 1) * this.zoom - 1, oy + py * this.zoom, 1, this.zoom, col);
                        }

                        if (py == 0 || !this.selection[ixx - this.doc.w]) {
                            c.fill(ox + px * this.zoom, oy + py * this.zoom, this.zoom, 1, col);
                        }

                        if (py == this.doc.h - 1 || !this.selection[ixx + this.doc.w]) {
                            c.fill(ox + px * this.zoom, oy + (py + 1) * this.zoom - 1, this.zoom, 1, col);
                        }
                    }
                }
            }
        }

        if (this.curX >= 0
            && !this.panning
            && this.inCanvas(mx, my)
            && (this.tool == TextureEditor.Tool.PENCIL || this.tool == TextureEditor.Tool.ERASER || this.tool == TextureEditor.Tool.SHADE)) {
            int half = (this.size - 1) / 2;
            int bx = ox + (this.curX - half) * this.zoom;
            int by = oy + (this.curY - half) * this.zoom;
            if (this.tool == TextureEditor.Tool.PENCIL && !this.drawing) {
                c.fill(bx, by, this.size * this.zoom, this.size * this.zoom, this.primary & 16777215 | 1711276032);
            }

            c.outline(bx, by, this.size * this.zoom, this.size * this.zoom, -855638017);
            if (this.mirrorX) {
                c.outline(ox + (this.doc.w - 1 - this.curX - half) * this.zoom, by, this.size * this.zoom, this.size * this.zoom, 1728053247);
            }

            if (this.mirrorY) {
                c.outline(bx, oy + (this.doc.h - 1 - this.curY - half) * this.zoom, this.size * this.zoom, this.size * this.zoom, 1728053247);
            }
        } else if (this.doc.in(this.curX, this.curY) && this.inCanvas(mx, my) && !this.panning) {
            c.outline(ox + this.curX * this.zoom, oy + this.curY * this.zoom, this.zoom, this.zoom, -1426063361);
        }

        if (this.mirrorX) {
            c.fill(ox + iw / 2, oy, 1, ih, 1728015320);
        }

        if (this.mirrorY) {
            c.fill(ox, oy + ih / 2, iw, 1, 1728015320);
        }

        int pw = Math.min(this.doc.w * 2, 160);
        int ph = pw * this.doc.h / this.doc.w;
        int px0 = this.canvasX + this.canvasW - pw - 10;
        int py0 = this.canvasY + 10;
        c.roundBorder(px0 - 4, py0 - 4, pw + 8, ph + 8, 4, -300542179, -13749952);
        if (this.texId != null) {
            c.g.blit(this.texId, px0, py0, pw, ph, 0.0F, 0.0F, this.doc.w, this.doc.h, this.doc.w, this.doc.h);
        }

        c.popClip();
    }

    private void uploadTexture() {
        int ver = this.doc.version * 1000 + this.drawVersion;
        if (this.tex == null || this.texW != this.doc.w || this.texH != this.doc.h || this.texVersion != ver) {
            try {
                if (this.tex == null || this.texW != this.doc.w || this.texH != this.doc.h) {
                    if (this.tex != null) {
                        this.tex.close();
                    }

                    this.tex = new DynamicTexture(new NativeImage(this.doc.w, this.doc.h, true));
                    this.texId = new ResourceLocation("spraute_engine", "texture_editor/" + counter++);
                    Minecraft.getInstance().getTextureManager().register(this.texId, this.tex);
                    this.texW = this.doc.w;
                    this.texH = this.doc.h;
                }

                if (this.checker == null || this.checkerW != this.doc.w || this.checkerH != this.doc.h) {
                    if (this.checker != null) {
                        Minecraft.getInstance().getTextureManager().release(this.checkerId);
                    }

                    NativeImage ci = new NativeImage(this.doc.w * 2, this.doc.h * 2, false);

                    for (int yy = 0; yy < this.doc.h * 2; yy++) {
                        for (int xx = 0; xx < this.doc.w * 2; xx++) {
                            ci.setPixelRGBA(xx, yy, (xx + yy & 1) == 0 ? -12108486 : -13226452);
                        }
                    }

                    this.checker = new DynamicTexture(ci);
                    this.checkerId = new ResourceLocation("spraute_engine", "texture_editor/checker" + counter++);
                    Minecraft.getInstance().getTextureManager().register(this.checkerId, this.checker);
                    this.checkerW = this.doc.w;
                    this.checkerH = this.doc.h;
                }

                NativeImage ni = this.tex.getPixels();
                if (ni == null) {
                    return;
                }

                int[] flat = this.doc.flatten();

                for (int i = 0; i < flat.length; i++) {
                    int a = flat[i];
                    ni.setPixelRGBA(i % this.doc.w, i / this.doc.w, a & -16711936 | a >> 16 & 0xFF | (a & 0xFF) << 16);
                }

                this.tex.upload();
                this.texVersion = ver;
            } catch (Throwable var6) {
                SprauteEngine.LOGGER.warn("Texture editor upload", var6);
            }
        }
    }

    private void paintTopBar(Canvas c, int mx, int my) {
        c.fill(this.x, this.y, this.w, 30, -14934490);
        c.fill(this.x, this.y + 30 - 1, this.w, 1, -13749952);
        int bx = this.x + 8;
        c.text(this.image.name() + (this.doc.dirty() ? " •" : ""), bx, this.y + 11, -1841682);
        bx += c.width(this.image.name() + " •") + 6;
        c.text(this.doc.w + "×" + this.doc.h, bx, this.y + 11, -9933435);
        bx += c.width(this.doc.w + "×" + this.doc.h) + 14;
        bx = this.barButton(c, mx, my, bx, "Сохранить", Icon.SAVE, this.doc.dirty(), this::save, "Ctrl+S");
        bx = this.barButton(c, mx, my, bx, null, Icon.UNDO, false, this::undo, "Отменить (Ctrl+Z)");
        bx = this.barButton(c, mx, my, bx, null, Icon.REDO, false, this::redo, "Вернуть (Ctrl+Y)");
        bx += 8;
        bx = this.barToggle(c, mx, my, bx, "Сетка", this.grid, () -> this.grid = !this.grid, "Сетка пикселей");
        if (this.isSkinSize()) {
            bx = this.barToggle(
                c, mx, my, bx, "Разметка", this.skinGuide, () -> this.skinGuide = !this.skinGuide, "Части скина: голова, тело, руки, ноги, глаза Spraute…"
            );
        }

        bx = this.barToggle(c, mx, my, bx, "Зеркало ↔", this.mirrorX, () -> this.mirrorX = !this.mirrorX, "Рисовать симметрично слева-справа (H)");
        bx = this.barToggle(c, mx, my, bx, "Зеркало ↕", this.mirrorY, () -> this.mirrorY = !this.mirrorY, "Рисовать симметрично сверху-снизу");
        bx += 8;
        bx = this.barButton(c, mx, my, bx, "Слой ▾", null, false, () -> this.layerMenu(mx, this.y + 30), "Отразить, повернуть, сдвинуть слой");
        bx = this.barButton(c, mx, my, bx, "Холст ▾", null, false, () -> this.canvasMenu(mx, this.y + 30), "Размер холста, вписать в окно");
        String z = this.zoom + "×";
        int zx = this.x + this.w - 8 - 18;
        this.barButtonAt(c, mx, my, zx, "+", () -> this.zoomBy(1, this.canvasX + this.canvasW / 2, this.canvasY + this.canvasH / 2), "Приблизить (колесо)");
        zx -= c.width(z) + 10;
        c.text(z, zx + 4, this.y + 11, -6577995);
        this.barButtonAt(c, mx, my, zx - 20, "−", () -> this.zoomBy(-1, this.canvasX + this.canvasW / 2, this.canvasY + this.canvasH / 2), "Отдалить");
    }

    private int barButton(Canvas c, int mx, int my, int bx, String text, Icon icon, boolean accent, Runnable run, String tip) {
        int bw = (text != null ? c.width(text) + 12 : 0) + (icon != null ? 18 : 0) + (text == null ? 2 : 0);
        boolean hov = mx >= bx && mx < bx + bw && my >= this.y + 5 && my < this.y + 25;
        c.round(bx, this.y + 5, bw, 20, 4, accent ? (hov ? -6255873 : -7639809) : (hov ? -13618102 : -14210506));
        int tx = bx + 6;
        if (icon != null) {
            c.icon(icon, bx + 5, this.y + 9, accent ? -1 : -6577995);
            tx += 16;
        }

        if (text != null) {
            c.text(text, icon != null ? tx - 2 : tx, this.y + 11, accent ? -1 : -1841682);
        }

        this.hits.add(new TextureEditor.Hit(bx, this.y + 5, bw, 20, run, null, tip));
        return bx + bw + 4;
    }

    private void barButtonAt(Canvas c, int mx, int my, int bx, String text, Runnable run, String tip) {
        boolean hov = mx >= bx && mx < bx + 18 && my >= this.y + 5 && my < this.y + 25;
        c.round(bx, this.y + 5, 18, 20, 4, hov ? -13618102 : -14210506);
        c.textCentered(text, bx + 9, this.y + 11, -1841682);
        this.hits.add(new TextureEditor.Hit(bx, this.y + 5, 18, 20, run, null, tip));
    }

    private int barToggle(Canvas c, int mx, int my, int bx, String text, boolean on, Runnable run, String tip) {
        int bw = c.width(text) + 14;
        boolean hov = mx >= bx && mx < bx + bw && my >= this.y + 5 && my < this.y + 25;
        c.round(bx, this.y + 5, bw, 20, 4, on ? (hov ? -11911540 : -12766347) : (hov ? -13618102 : -14210506));
        if (on) {
            c.fill(bx + 4, this.y + 22, bw - 8, 1, -7639809);
        }

        c.text(text, bx + 7, this.y + 11, on ? -1841682 : -6577995);
        this.hits.add(new TextureEditor.Hit(bx, this.y + 5, bw, 20, run, null, tip));
        return bx + bw + 4;
    }

    private void paintTools(Canvas c, int mx, int my) {
        c.fill(this.x, this.y + 30, 44, this.h - 30, -14934490);
        c.fill(this.x + 44 - 1, this.y + 30, 1, this.h - 30, -13749952);
        int ty = this.y + 30 + 6;

        for (TextureEditor.Tool t : TextureEditor.Tool.values()) {
            int tx = this.x + 6;
            boolean on = t == this.tool;
            boolean hov = mx >= tx && mx < tx + 32 && my >= ty && my < ty + 28;
            if (on) {
                c.round(tx, ty, 32, 28, 5, -12766347);
            } else if (hov) {
                c.round(tx, ty, 32, 28, 5, -14210506);
            }

            if (on) {
                c.fill(this.x, ty + 6, 2, 16, -7639809);
            }

            glyph(c, t.glyph, tx + 7, ty + 5, 2, on ? -1 : (hov ? -1841682 : -6577995));
            this.hits.add(new TextureEditor.Hit(tx, ty, 32, 28, () -> this.setTool(t), null, t.title + "  [" + t.key + "]"));
            ty += 30;
            if (t == TextureEditor.Tool.FILL || t == TextureEditor.Tool.ELLIPSE || t == TextureEditor.Tool.PICKER) {
                c.fill(this.x + 10, ty, 24, 1, -13749952);
                ty += 5;
            }
        }

        int cy = Math.max(ty + 8, this.y + this.h - 20 - 52);
        c.fill(this.x + 16, cy + 14, 20, 20, -16777216);
        swatch(c, this.x + 17, cy + 15, 18, 18, this.secondary);
        c.fill(this.x + 6, cy + 4, 20, 20, -16777216);
        swatch(c, this.x + 7, cy + 5, 18, 18, this.primary);
        this.hits
            .add(
                new TextureEditor.Hit(
                    this.x + 6, cy + 4, 20, 20, () -> this.pickColor(true, this.x + 30, cy), null, "Основной цвет (ЛКМ) — нажмите, чтобы выбрать"
                )
            );
        this.hits.add(new TextureEditor.Hit(this.x + 16, cy + 24, 20, 10, () -> this.pickColor(false, this.x + 30, cy), null, "Второй цвет (ПКМ)"));
        c.text("⇄", this.x + 28, cy - 2, -9933435);
        this.hits.add(new TextureEditor.Hit(this.x + 26, cy - 4, 14, 12, this::swapColors, null, "Поменять цвета (X)"));
    }

    private static void glyph(Canvas c, String[] g, int gx, int gy, int k, int color) {
        for (int r = 0; r < g.length; r++) {
            for (int q = 0; q < g[r].length(); q++) {
                if (g[r].charAt(q) == '#') {
                    c.fill(gx + q * k, gy + r * k, k, k, color);
                }
            }
        }
    }

    private static void swatch(Canvas c, int sx, int sy, int sw, int sh, int argb) {
        if (argb >>> 24 < 255) {
            for (int yy = 0; yy < sh; yy += 4) {
                for (int xx = 0; xx < sw; xx += 4) {
                    c.fill(sx + xx, sy + yy, Math.min(4, sw - xx), Math.min(4, sh - yy), ((xx + yy) / 4 & 1) == 0 ? -7697782 : -10855846);
                }
            }
        }

        if (argb >>> 24 > 0) {
            c.fill(sx, sy, sw, sh, argb);
        }
    }

    private void paintRight(Canvas c, int mx, int my) {
        int rx = this.x + this.w - 240;
        int ry = this.y + 30;
        c.fill(rx, ry, 240, this.h - 30, -14934490);
        c.fill(rx, ry, 1, this.h - 30, -13749952);
        int px = rx + 10;
        int pw = 220;
        int yy = ry + 8;
        yy = this.header(c, "ЦВЕТ", px, yy);
        swatch(c, px, yy, 40, 28, this.primary);
        c.outline(px - 1, yy - 1, 42, 30, -12762793);
        this.hits.add(new TextureEditor.Hit(px, yy, 40, 28, () -> this.pickColor(true, rx - 230, ry + 20), null, "Основной цвет — выбрать"));
        swatch(c, px + 46, yy + 8, 26, 20, this.secondary);
        c.outline(px + 45, yy + 7, 28, 22, -12762793);
        this.hits.add(new TextureEditor.Hit(px + 46, yy + 8, 26, 20, () -> this.pickColor(false, rx - 230, ry + 20), null, "Второй цвет — выбрать"));
        c.text(String.format("#%06X", this.primary & 16777215), px + 80, yy + 2, -1841682);
        int alpha = Math.round((this.primary >>> 24) / 2.55F);
        c.text("непрозр. " + alpha + "%", px + 80, yy + 16, -9933435);
        yy += 36;
        c.text("Шейды (тень → свет)", px, yy, -9933435);
        yy += 11;
        int[] ramp = PixelOps.ramp(this.primary, 7, this.hueShift);
        int cw = (pw - 6) / 7;

        for (int i = 0; i < 7; i++) {
            int col = ramp[i];
            int sxx = px + i * (cw + 1);
            swatch(c, sxx, yy, cw, 16, col);
            if (i == 3) {
                c.outline(sxx - 1, yy - 1, cw + 2, 18, -1);
            }

            this.hits
                .add(
                    new TextureEditor.Hit(
                        sxx,
                        yy,
                        cw,
                        16,
                        () -> this.setPrimary(col),
                        () -> this.secondary = col,
                        String.format("#%06X — ЛКМ основной, ПКМ второй", col & 16777215)
                    )
                );
        }

        yy += 22;
        String pal = this.paletteFromImage ? "Палитра картинки" : "Палитра Endesga 32";
        c.text(pal, px, yy, -9933435);
        String other = this.paletteFromImage ? "Endesga" : "картинки";
        int ow = c.width(other);
        c.text(other, px + pw - ow, yy, -7639809);
        this.hits
            .add(new TextureEditor.Hit(px + pw - ow - 2, yy - 2, ow + 4, 11, () -> this.paletteFromImage = !this.paletteFromImage, null, "Сменить палитру"));
        yy += 11;
        List<Integer> colors = this.paletteFromImage ? this.imageColors() : this.endesga();

        for (int r : this.recent) {
            if (!colors.contains(r)) {
                colors.add(0, r);
            }
        }

        int per = 12;
        int sw = (pw - (per - 1)) / per;
        int shown = Math.min(colors.size(), per * 4);

        for (int i = 0; i < shown; i++) {
            int col = colors.get(i);
            int sxx = px + i % per * (sw + 1);
            int syy = yy + i / per * (sw + 1);
            swatch(c, sxx, syy, sw, sw, col);
            if (col == this.primary) {
                c.outline(sxx - 1, syy - 1, sw + 2, sw + 2, -1);
            }

            this.hits.add(new TextureEditor.Hit(sxx, syy, sw, sw, () -> this.setPrimary(col), () -> this.secondary = col, String.format("#%08X", col)));
        }

        yy += (Math.max(1, shown) + per - 1) / per * (sw + 1) + 8;
        yy = this.header(c, this.tool.title.toUpperCase(Locale.ROOT).replaceAll(" \\(.*", "").replaceAll(":.*", ""), px, yy);
        yy = this.toolOptions(c, mx, my, px, yy, pw);
        yy += 6;
        this.paintLayers(c, mx, my, px, yy, pw, this.y + this.h - 20 - yy - 4);
    }

    private int header(Canvas c, String text, int px, int yy) {
        c.text(text, px, yy, -6577995);
        c.fill(px + c.width(text) + 6, yy + 4, 214 - c.width(text), 1, -13749952);
        return yy + 13;
    }

    private int toolOptions(Canvas c, int mx, int my, int px, int yy, int pw) {
        switch (this.tool) {
            case PENCIL:
            case ERASER:
                yy = this.stepper(
                    c,
                    mx,
                    my,
                    px,
                    yy,
                    pw,
                    "Размер",
                    this.size + " px",
                    () -> this.size = Math.max(1, this.size - 1),
                    () -> this.size = Math.min(16, this.size + 1)
                );
                if (this.tool == TextureEditor.Tool.PENCIL) {
                    yy = this.toggle(c, mx, my, px, yy, pw, "Pixel-perfect (без «лесенок»)", this.pixelPerfect, () -> this.pixelPerfect = !this.pixelPerfect);
                }
                break;
            case SHADE:
                yy = this.stepper(
                    c,
                    mx,
                    my,
                    px,
                    yy,
                    pw,
                    "Размер",
                    this.size + " px",
                    () -> this.size = Math.max(1, this.size - 1),
                    () -> this.size = Math.min(16, this.size + 1)
                );
                yy = this.stepper(
                    c,
                    mx,
                    my,
                    px,
                    yy,
                    pw,
                    "Сдвиг оттенка",
                    Math.round(this.hueShift) + "°",
                    () -> this.hueShift = Math.max(0.0F, this.hueShift - 2.0F),
                    () -> this.hueShift = Math.min(30.0F, this.hueShift + 2.0F)
                );
                c.wrapped("Тени холоднее и насыщеннее, свет теплее. Один проход — один шаг на пиксель.", px, yy, pw, -9933435, 1);
                yy += 22;
                break;
            case FILL:
            case WAND:
                yy = this.toggle(c, mx, my, px, yy, pw, "Только соседние пиксели", this.contiguous, () -> this.contiguous = !this.contiguous);
                yy = this.stepper(
                    c,
                    mx,
                    my,
                    px,
                    yy,
                    pw,
                    "Допуск",
                    String.valueOf(this.tolerance),
                    () -> this.tolerance = Math.max(0, this.tolerance - 4),
                    () -> this.tolerance = Math.min(128, this.tolerance + 4)
                );
                if (this.tool == TextureEditor.Tool.WAND) {
                    c.text("Shift — добавить к выделению", px, yy, -9933435);
                    yy += 12;
                }
                break;
            case RECT:
            case ELLIPSE:
                yy = this.toggle(c, mx, my, px, yy, pw, "Залитая фигура", this.filled, () -> this.filled = !this.filled);
                break;
            case LINE:
                c.text("Shift — ровно по 45°", px, yy, -9933435);
                yy += 12;
                break;
            case GRADIENT:
                yy = this.choice(c, mx, my, px, yy, pw, "Цвета", new String[]{"Шейды", "Осн.→втор.", "По цветам"}, this.gradColors, v -> this.gradColors = v);
                yy = this.stepper(
                    c,
                    mx,
                    my,
                    px,
                    yy,
                    pw,
                    "Ступеней",
                    String.valueOf(this.gradSteps),
                    () -> this.gradSteps = Math.max(2, this.gradSteps - 1),
                    () -> this.gradSteps = Math.min(12, this.gradSteps + 1)
                );
                yy = this.choice(c, mx, my, px, yy, pw, "Дизеринг", new String[]{"нет", "шахматка", "Байер 4×4"}, this.gradDither, v -> this.gradDither = v);
                yy = this.stepper(
                    c,
                    mx,
                    my,
                    px,
                    yy,
                    pw,
                    "Ширина перехода",
                    Math.round(this.gradSpread * 100.0F) + "%",
                    () -> this.gradSpread = Math.max(0.0F, this.gradSpread - 0.125F),
                    () -> this.gradSpread = Math.min(1.0F, this.gradSpread + 0.125F)
                );
                yy = this.choice(c, mx, my, px, yy, pw, "Форма", new String[]{"Линейный", "Круговой"}, this.gradRadial ? 1 : 0, v -> this.gradRadial = v == 1);
                yy = this.choice(
                    c, mx, my, px, yy, pw, "Где", new String[]{"Цвет под курс.", "Непрозрачное", "Весь слой"}, this.gradArea, v -> this.gradArea = v
                );
                yy = this.toggle(c, mx, my, px, yy, pw, "Обратно (начало — тень)", this.gradReverse, () -> this.gradReverse = !this.gradReverse);
                int[] cols = this.gradientColors(-8355712);
                int cw = Math.max(4, (pw - cols.length) / cols.length);

                for (int i = 0; i < cols.length; i++) {
                    swatch(c, px + i * (cw + 1), yy, cw, 8, cols[i]);
                }

                yy += 12;
                c.wrapped("Тяните от света к тени. Выделение ограничивает область.", px, yy, pw, -9933435, 1);
                yy += 22;
                break;
            case SELECT:
                c.wrapped("Shift — добавить. Ctrl+C / Ctrl+V, Delete — стереть, Ctrl+D — снять.", px, yy, pw, -9933435, 1);
                yy += 22;
                break;
            case MOVE:
                c.wrapped("Двигает выделенное или весь слой. Стрелки — на пиксель.", px, yy, pw, -9933435, 1);
                yy += 22;
                break;
            case PICKER:
                c.wrapped("ЛКМ — основной цвет, ПКМ — второй. В любом инструменте: Alt + клик.", px, yy, pw, -9933435, 1);
                yy += 22;
        }

        return yy;
    }

    private int stepper(Canvas c, int mx, int my, int px, int yy, int pw, String label, String value, Runnable minus, Runnable plus) {
        c.text(label, px, yy + 4, -6577995);
        int bx = px + pw - 70;
        this.smallButton(c, mx, my, bx, yy, 16, "−", minus);
        c.textCentered(value, bx + 35, yy + 4, -1841682);
        this.smallButton(c, mx, my, bx + 54, yy, 16, "+", plus);
        return yy + 18;
    }

    private int toggle(Canvas c, int mx, int my, int px, int yy, int pw, String label, boolean on, Runnable flip) {
        int bx = px + pw - 24;
        c.round(bx, yy + 2, 24, 12, 6, on ? -7639809 : -14210506);
        c.round(on ? bx + 13 : bx + 1, yy + 3, 10, 10, 5, -1);
        c.textEllipsized(label, px, yy + 4, pw - 30, -6577995);
        this.hits.add(new TextureEditor.Hit(px, yy, pw, 16, flip, null, null));
        return yy + 18;
    }

    private int choice(Canvas c, int mx, int my, int px, int yy, int pw, String label, String[] opts, int cur, IntConsumer set) {
        c.text(label, px, yy, -9933435);
        yy += 10;
        int bw = (pw - (opts.length - 1) * 2) / opts.length;

        for (int i = 0; i < opts.length; i++) {
            int bx = px + i * (bw + 2);
            int idx = i;
            boolean on = i == cur;
            boolean hov = mx >= bx && mx < bx + bw && my >= yy && my < yy + 15;
            c.round(bx, yy, bw, 15, 3, on ? -12766347 : (hov ? -13618102 : -14210506));
            c.textCentered(c.ellipsize(opts[i], bw - 4), bx + bw / 2, yy + 4, on ? -1841682 : -6577995);
            this.hits.add(new TextureEditor.Hit(bx, yy, bw, 15, () -> set.accept(idx), null, opts[i]));
        }

        return yy + 19;
    }

    private void smallButton(Canvas c, int mx, int my, int bx, int by, int bw, String text, Runnable run) {
        boolean hov = mx >= bx && mx < bx + bw && my >= by && my < by + 15;
        c.round(bx, by, bw, 15, 3, hov ? -13618102 : -14210506);
        c.textCentered(text, bx + bw / 2, by + 4, -1841682);
        this.hits.add(new TextureEditor.Hit(bx, by, bw, 15, run, null, null));
    }

    private void paintLayers(Canvas c, int mx, int my, int px, int yy, int pw, int avail) {
        yy = this.header(c, "СЛОИ", px, yy);
        String[][] btn = new String[][]{{"+", "Новый слой"}, {"⧉", "Копия слоя"}, {"▲", "Выше"}, {"▼", "Ниже"}, {"⤓", "Слить с нижним"}, {"✕", "Удалить слой"}};
        Runnable[] act = new Runnable[]{
            this::newLayer, this::duplicateLayer, () -> this.moveLayer(1), () -> this.moveLayer(-1), this::mergeDown, this::deleteLayer
        };
        int bw = (pw - 10) / 6;

        for (int i = 0; i < 6; i++) {
            int bx = px + i * (bw + 2);
            boolean hov = mx >= bx && mx < bx + bw && my >= yy && my < yy + 16;
            c.round(bx, yy, bw, 16, 3, hov ? -13618102 : -14210506);
            c.textCentered(btn[i][0], bx + bw / 2, yy + 4, i == 5 ? -1023878 : -1841682);
            this.hits.add(new TextureEditor.Hit(bx, yy, bw, 16, act[i], null, btn[i][1]));
        }

        yy += 20;
        PixelDoc.Layer cur = this.doc.layer();
        yy = this.stepper(
            c,
            mx,
            my,
            px,
            yy,
            pw,
            "Непрозрачность",
            cur.opacity + "%",
            () -> this.setOpacity(cur, cur.opacity - 10),
            () -> this.setOpacity(cur, cur.opacity + 10)
        );
        int rows = Math.max(1, (avail - 54) / 22);
        this.layerScroll = Math.max(0, Math.min(this.layerScroll, this.doc.layers.size() - rows));
        int shown = 0;

        for (int li = this.doc.layers.size() - 1 - this.layerScroll; li >= 0 && shown < rows; shown++) {
            PixelDoc.Layer l = this.doc.layers.get(li);
            int ly0 = yy + shown * 22;
            int idx = li;
            boolean on = li == this.doc.active;
            boolean hov = mx >= px && mx < px + pw && my >= ly0 && my < ly0 + 20;
            c.round(px, ly0, pw, 20, 4, on ? -13686180 : (hov ? -14210506 : -14605523));
            if (on) {
                c.fill(px, ly0 + 4, 2, 12, -7639809);
            }

            glyph(
                c,
                l.visible ? new String[]{"..###..", ".#...#.", "#..#..#", ".#...#."} : new String[]{".......", ".......", "#######", "......."},
                px + 6,
                ly0 + 6,
                1,
                l.visible ? -1841682 : -9933435
            );
            this.hits.add(new TextureEditor.Hit(px + 2, ly0, 16, 20, () -> {
                this.doc.checkpoint();
                l.visible = !l.visible;
            }, null, l.visible ? "Скрыть слой" : "Показать слой"));
            c.textEllipsized(l.name, px + 22, ly0 + 6, pw - 70, l.visible ? -1841682 : -9933435);
            if (l.opacity < 100) {
                c.text(l.opacity + "%", px + pw - 44, ly0 + 6, -9933435);
            }

            glyph(
                c,
                l.locked ? new String[]{".###.", "#...#", "#####", "#####", "#####"} : new String[]{".###.", "#...#", "....#", "#####", "#####"},
                px + pw - 14,
                ly0 + 7,
                1,
                l.locked ? -870325 : -9933435
            );
            this.hits
                .add(
                    new TextureEditor.Hit(
                        px + pw - 18, ly0, 18, 20, () -> l.locked = !l.locked, null, l.locked ? "Слой закрыт — открыть" : "Закрыть слой от рисования"
                    )
                );
            this.hits
                .add(
                    new TextureEditor.Hit(
                        px + 18,
                        ly0,
                        pw - 36,
                        20,
                        () -> this.doc.active = idx,
                        () -> this.renameLayer(l),
                        "ЛКМ — выбрать, ПКМ или двойной клик — переименовать"
                    )
                );
            li--;
        }

        if (this.doc.layers.size() > rows) {
            c.text("колесо — ещё слои", px, yy + rows * 22 + 2, -9933435);
        }
    }

    private void paintStatus(Canvas c, int mx, int my) {
        int sy0 = this.y + this.h - 20;
        c.fill(this.x + 44, sy0, this.w - 44 - 240, 20, -14934490);
        c.fill(this.x + 44, sy0, this.w - 44 - 240, 1, -13749952);
        StringBuilder s = new StringBuilder();
        if (this.doc.in(this.curX, this.curY) && this.inCanvas(mx, my)) {
            int col = this.doc.flatAt(this.curX, this.curY);
            s.append(this.curX).append(", ").append(this.curY).append("   ");
            s.append(
                col >>> 24 == 0
                    ? "прозрачный"
                    : String.format("#%06X", col & 16777215) + (col >>> 24 < 255 ? " (" + Math.round((col >>> 24) / 2.55F) + "%)" : "")
            );
            if (this.skinGuide && this.isSkinSize()) {
                for (TextureEditor.Part p : this.parts()) {
                    if (this.curX >= p.x && this.curX < p.x + p.w && this.curY >= p.y && this.curY < p.y + p.h) {
                        s.append("   ·  ").append(p.name);
                        break;
                    }
                }
            }
        } else {
            s.append("Колесо — масштаб, средняя кнопка или пробел — двигать, Alt — пипетка, [ ] — размер кисти, X — поменять цвета");
        }

        c.textEllipsized(s.toString(), this.x + 44 + 8, sy0 + 6, this.w - 44 - 240 - 16, -6577995);
    }

    private boolean isSkinSize() {
        return this.doc.w == 64 && (this.doc.h == 64 || this.doc.h == 32) || this.doc.w == 128 && this.doc.h == 64;
    }

    private List<TextureEditor.Part> parts() {
        List<TextureEditor.Part> p = new ArrayList<>();
        int head = -1996500390;
        int body = -2008038409;
        int arm = -2005147510;
        int leg = -1996517542;
        int over = 1437895935;
        p.add(new TextureEditor.Part("Голова · верх", 8, 0, 8, 8, head));
        p.add(new TextureEditor.Part("Голова · низ", 16, 0, 8, 8, head));
        p.add(new TextureEditor.Part("Голова · правая сторона", 0, 8, 8, 8, head));
        p.add(new TextureEditor.Part("Голова · лицо", 8, 8, 8, 8, head));
        p.add(new TextureEditor.Part("Голова · левая сторона", 16, 8, 8, 8, head));
        p.add(new TextureEditor.Part("Голова · затылок", 24, 8, 8, 8, head));
        p.add(new TextureEditor.Part("Шапка (второй слой головы)", 32, 0, 32, 16, over));
        p.add(new TextureEditor.Part("Правая нога", 0, 16, 16, 16, leg));
        p.add(new TextureEditor.Part("Тело", 16, 16, 24, 16, body));
        p.add(new TextureEditor.Part("Правая рука", 40, 16, 16, 16, arm));
        if (this.doc.h >= 64) {
            p.add(new TextureEditor.Part("Правая нога · второй слой", 0, 32, 16, 16, over));
            p.add(new TextureEditor.Part("Куртка (второй слой тела)", 16, 32, 24, 16, over));
            p.add(new TextureEditor.Part("Правый рукав", 40, 32, 16, 16, over));
            p.add(new TextureEditor.Part("Левая нога · второй слой", 0, 48, 16, 16, over));
            p.add(new TextureEditor.Part("Левая нога", 16, 48, 16, 16, leg));
            p.add(new TextureEditor.Part("Левая рука", 32, 48, 16, 16, arm));
            p.add(new TextureEditor.Part("Левый рукав", 48, 48, 16, 16, over));
        }

        if (this.doc.w == 128) {
            int st = -37928;
            p.add(0, new TextureEditor.Part("Spraute · зрачки", 0, 0, 2, 2, st));
            p.add(0, new TextureEditor.Part("Spraute · белок левого глаза", 0, 2, 2, 2, st));
            p.add(0, new TextureEditor.Part("Spraute · белок правого глаза", 4, 2, 2, 2, st));
            p.add(0, new TextureEditor.Part("Spraute · брови", 0, 5, 6, 2, st));
            p.add(0, new TextureEditor.Part("Spraute · зубы", 6, 6, 2, 1, st));
            p.add(0, new TextureEditor.Part("Spraute · рот", 6, 7, 2, 1, st));
        }

        return p;
    }

    private List<Integer> imageColors() {
        int ver = this.doc.version * 1000 + (this.drawing ? 0 : this.drawVersion);
        if (this.paletteCache == null || this.paletteVersion != ver && !this.drawing) {
            this.paletteVersion = ver;
            this.paletteCache = this.computeImageColors();
            return new ArrayList<>(this.paletteCache);
        } else {
            return new ArrayList<>(this.paletteCache);
        }
    }

    private List<Integer> computeImageColors() {
        LinkedHashMap<Integer, Integer> count = new LinkedHashMap<>();
        int[] flat = this.doc.flatten();

        for (int c : flat) {
            if (c >>> 24 > 0) {
                count.merge(c, 1, Integer::sum);
            }
        }

        List<Integer> cols = new ArrayList<>(count.keySet());
        if (cols.size() > 256) {
            cols.sort(Comparator.comparingInt(cx -> -count.get(cx)));
            cols = new ArrayList<>(cols.subList(0, 256));
        }

        Map<Integer, float[]> hsvs = new HashMap<>();

        for (int cx : cols) {
            hsvs.put(cx, PixelOps.hsv(cx));
        }

        cols.sort(Comparator.<Integer>comparingDouble(cx -> Math.round(hsvs.get(cx)[0] / 30.0F)).thenComparingDouble(cx -> hsvs.get(cx)[2]));
        return (List<Integer>)(cols.size() > 48 ? new ArrayList<>(cols.subList(0, 48)) : cols);
    }

    private List<Integer> endesga() {
        List<Integer> l = new ArrayList<>();

        for (int c : ENDESGA) {
            l.add(0xFF000000 | c);
        }

        return l;
    }

    private void setPrimary(int c) {
        this.primary = c;
        this.remember(c);
    }

    private void remember(int c) {
        this.recent.remove(Integer.valueOf(c));
        this.recent.add(0, c);

        while (this.recent.size() > 8) {
            this.recent.remove(this.recent.size() - 1);
        }
    }

    private void swapColors() {
        int t = this.primary;
        this.primary = this.secondary;
        this.secondary = t;
    }

    private void pickColor(boolean main, int px, int py) {
        ColorPicker.open(main ? this.primary : this.secondary, Math.max(4, px), Math.max(4, py), c -> {
            if (main) {
                this.primary = c;
            } else {
                this.secondary = c;
            }
        });
    }

    private int[] gradientColors(int under) {
        int n = this.gradSteps;

        int[] cols = switch (this.gradColors) {
            case 1 -> PixelOps.blend(this.primary, this.secondary, n);
            case 2 -> {
                int[] o = new int[n];

                for (int i = 0; i < n; i++) {
                    o[i] = PixelOps.shade(under, (n - 1) / 2 - i, this.hueShift);
                }

                yield o;
            }
            default -> {
                int[] r = PixelOps.ramp(this.primary, n, this.hueShift);
                int[] o = new int[n];

                for (int i = 0; i < n; i++) {
                    o[i] = r[n - 1 - i];
                }

                yield o;
            }
        };
        if (this.gradReverse) {
            int[] o = new int[n];

            for (int i = 0; i < n; i++) {
                o[i] = cols[n - 1 - i];
            }

            cols = o;
        }

        return cols;
    }

    private boolean inCanvas(double mx, double my) {
        return mx >= this.canvasX && mx < this.canvasX + this.canvasW && my >= this.canvasY && my < this.canvasY + this.canvasH;
    }

    private int toPx(double mx) {
        return (int)Math.floor((mx - this.originX()) / this.zoom);
    }

    private int toPy(double my) {
        return (int)Math.floor((my - this.originY()) / this.zoom);
    }

    @Override
    public String tooltipText(int mx, int my) {
        for (int i = this.hits.size() - 1; i >= 0; i--) {
            TextureEditor.Hit hh = this.hits.get(i);
            if (mx >= hh.x && mx < hh.x + hh.w && my >= hh.y && my < hh.y + hh.h) {
                return hh.tip;
            }
        }

        return null;
    }

    @Override
    public boolean mousePressed(double mx, double my, int b) {
        this.requestFocus();
        if (this.doc == null) {
            return true;
        } else {
            for (int i = this.hits.size() - 1; i >= 0; i--) {
                TextureEditor.Hit hh = this.hits.get(i);
                if (mx >= hh.x && mx < hh.x + hh.w && my >= hh.y && my < hh.y + hh.h) {
                    if (b == 1 && hh.right != null) {
                        hh.right.run();
                    } else if (b == 0 && hh.left != null) {
                        hh.left.run();
                    }

                    return true;
                }
            }

            if (!this.inCanvas(mx, my)) {
                return true;
            } else {
                int px = this.toPx(mx);
                int py = this.toPy(my);
                boolean space = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 32);
                if (b != 2 && !space) {
                    boolean alt = InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), 342);
                    if (!alt && this.tool != TextureEditor.Tool.PICKER) {
                        this.button = b;
                        this.sx = this.lx = px;
                        this.sy = this.ly = py;
                        this.selectAdd = UI.shift(this.modsNow());
                        switch (this.tool) {
                            case FILL: {
                                if (!this.editable() || !this.doc.in(px, py)) {
                                    return true;
                                }

                                this.doc.checkpoint();
                                int col = b == 1 ? this.secondary : this.primary;
                                boolean[] m = PixelOps.region(this.doc.layer().px, this.doc.w, this.doc.h, px, py, this.contiguous, this.tolerance);
                                int[] lp = this.doc.layer().px;

                                for (int ix = 0; ix < lp.length; ix++) {
                                    if (m[ix] && this.allowed(ix)) {
                                        lp[ix] = col;
                                    }
                                }

                                this.remember(col);
                                break;
                            }
                            case WAND:
                                if (!this.doc.in(px, py)) {
                                    if (!this.selectAdd) {
                                        this.selection = null;
                                    }

                                    return true;
                                }

                                boolean[] m = PixelOps.region(this.doc.layer().px, this.doc.w, this.doc.h, px, py, this.contiguous, this.tolerance);
                                if (this.selectAdd && this.selection != null) {
                                    for (int ix = 0; ix < m.length; ix++) {
                                        this.selection[ix] = this.selection[ix] | m[ix];
                                    }
                                } else {
                                    this.selection = m;
                                }
                                break;
                            case RECT:
                            case ELLIPSE:
                            case LINE:
                            case GRADIENT:
                            default:
                                if (!this.editable()) {
                                    return true;
                                }

                                this.doc.checkpoint();
                                this.drawing = true;
                                this.base = (int[])this.doc.layer().px.clone();
                                this.touched = new boolean[this.doc.w * this.doc.h];
                                this.strokePts.clear();
                                if (this.tool == TextureEditor.Tool.GRADIENT) {
                                    this.gradMask = this.gradientMask(px, py);
                                }

                                if (this.tool != TextureEditor.Tool.PENCIL && this.tool != TextureEditor.Tool.ERASER && this.tool != TextureEditor.Tool.SHADE) {
                                    this.shapeTo(px, py);
                                } else {
                                    this.brushAt(px, py);
                                    if (this.tool == TextureEditor.Tool.PENCIL) {
                                        this.remember(b == 1 ? this.secondary : this.primary);
                                    }
                                }
                                break;
                            case SELECT:
                                this.drawing = true;
                                if (!this.selectAdd) {
                                    this.selection = null;
                                }

                                this.base = null;
                                this.updateRectSelection(px, py);
                                break;
                            case MOVE:
                                if (!this.editable()) {
                                    return true;
                                }

                                this.doc.checkpoint();
                                this.drawing = this.moving = true;
                                this.base = (int[])this.doc.layer().px.clone();
                                this.lifted = this.selection != null ? (boolean[])this.selection.clone() : null;
                                this.floatPx = (int[])this.base.clone();
                        }

                        this.drawVersion++;
                        return true;
                    } else {
                        if (this.doc.in(px, py)) {
                            int col = this.doc.flatAt(px, py);
                            if (b == 1) {
                                this.secondary = col;
                            } else {
                                this.setPrimary(col);
                            }
                        }

                        return true;
                    }
                } else {
                    this.panning = true;
                    this.panFromX = mx - this.panX;
                    this.panFromY = my - this.panY;
                    return true;
                }
            }
        }
    }

    private int modsNow() {
        long win = Minecraft.getInstance().getWindow().getWindow();
        int m = 0;
        if (InputConstants.isKeyDown(win, 340) || InputConstants.isKeyDown(win, 344)) {
            m |= 1;
        }

        return m;
    }

    private boolean editable() {
        PixelDoc.Layer l = this.doc.layer();
        UI ui = UI.get();
        if (l.locked) {
            if (ui != null) {
                ui.toastInfo("Слой «" + l.name + "» закрыт от рисования (замок справа)");
            }

            return false;
        } else if (!l.visible) {
            if (ui != null) {
                ui.toastInfo("Слой «" + l.name + "» скрыт — покажите его, чтобы рисовать");
            }

            return false;
        } else {
            return true;
        }
    }

    private boolean allowed(int i) {
        return this.selection == null || this.selection[i];
    }

    @Override
    public boolean mouseDragged(double mx, double my, int b, double dx, double dy) {
        if (this.doc == null) {
            return true;
        } else {
            this.curX = this.toPx(mx);
            this.curY = this.toPy(my);
            if (this.panning) {
                this.panX = (int)(mx - this.panFromX);
                this.panY = (int)(my - this.panFromY);
                return true;
            } else if (!this.drawing) {
                return true;
            } else {
                int px = this.curX;
                int py = this.curY;
                switch (this.tool) {
                    case PENCIL:
                    case ERASER:
                    case SHADE:
                        int fx = this.lx;
                        int fy = this.ly;
                        PixelOps.line(fx, fy, px, py, (qx, qy) -> {
                            if (qx != this.lx || qy != this.ly) {
                                this.brushAt(qx, qy);
                            }

                            this.lx = qx;
                            this.ly = qy;
                        });
                        break;
                    case FILL:
                    case WAND:
                    case RECT:
                    case ELLIPSE:
                    case LINE:
                    case GRADIENT:
                    default:
                        this.shapeTo(px, py);
                        break;
                    case SELECT:
                        this.updateRectSelection(px, py);
                        break;
                    case MOVE:
                        this.moveTo(px - this.sx, py - this.sy);
                }

                this.drawVersion++;
                return true;
            }
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int b) {
        if (this.panning) {
            this.panning = false;
            return true;
        } else {
            if (this.drawing && this.tool == TextureEditor.Tool.MOVE && this.lifted != null) {
                int ddx = this.toPx(mx) - this.sx;
                int ddy = this.toPy(my) - this.sy;
                boolean[] n = new boolean[this.lifted.length];

                for (int i = 0; i < this.lifted.length; i++) {
                    if (this.lifted[i]) {
                        int tx = i % this.doc.w + ddx;
                        int ty = i / this.doc.w + ddy;
                        if (this.doc.in(tx, ty)) {
                            n[ty * this.doc.w + tx] = true;
                        }
                    }
                }

                this.selection = n;
            }

            if (this.drawing && this.tool == TextureEditor.Tool.SELECT && this.selection != null) {
                boolean any = false;

                for (boolean s : this.selection) {
                    any |= s;
                }

                if (!any) {
                    this.selection = null;
                }
            }

            this.drawing = this.moving = false;
            this.base = null;
            this.touched = null;
            this.lifted = null;
            this.gradMask = null;
            this.drawVersion++;
            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.doc == null) {
            return true;
        } else if (mx >= this.x + this.w - 240) {
            this.layerScroll = Math.max(0, this.layerScroll - (int)Math.signum(delta));
            return true;
        } else {
            if (this.inCanvas(mx, my)) {
                this.zoomBy(delta > 0.0 ? 1 : -1, mx, my);
            }

            return true;
        }
    }

    private void zoomBy(int dir, double mx, double my) {
        int i = 0;

        while (i < ZOOMS.length - 1 && ZOOMS[i] < this.zoom) {
            i++;
        }

        int nz = ZOOMS[Math.max(0, Math.min(ZOOMS.length - 1, i + dir))];
        if (nz != this.zoom) {
            double fx = (mx - this.originX()) / this.zoom;
            double fy = (my - this.originY()) / this.zoom;
            this.zoom = nz;
            this.panX = this.panX + (int)Math.round(mx - (this.originX() + fx * this.zoom));
            this.panY = this.panY + (int)Math.round(my - (this.originY() + fy * this.zoom));
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(Canvas c, int mx, int my, float dt) {
        if (this.doc != null && !this.drawing && !this.panning && this.inCanvas(mx, my)) {
            this.curX = this.toPx(mx);
            this.curY = this.toPy(my);
        }

        super.render(c, mx, my, dt);
    }

    private void plot(int px, int py, IntUnaryOperator f) {
        this.plotOne(px, py, f);
        if (this.mirrorX) {
            this.plotOne(this.doc.w - 1 - px, py, f);
        }

        if (this.mirrorY) {
            this.plotOne(px, this.doc.h - 1 - py, f);
        }

        if (this.mirrorX && this.mirrorY) {
            this.plotOne(this.doc.w - 1 - px, this.doc.h - 1 - py, f);
        }
    }

    private void plotOne(int px, int py, IntUnaryOperator f) {
        if (this.doc.in(px, py)) {
            int i = py * this.doc.w + px;
            if (this.allowed(i)) {
                this.doc.layer().px[i] = f.applyAsInt(this.doc.layer().px[i]);
            }
        }
    }

    private void brushAt(int px, int py) {
        int col = this.button == 1 ? this.secondary : this.primary;
        int half = (this.size - 1) / 2;
        if (this.tool == TextureEditor.Tool.PENCIL && this.size == 1 && this.pixelPerfect) {
            this.pixelPerfectAdd(px, py, col);
        } else {
            for (int yy = 0; yy < this.size; yy++) {
                for (int xx = 0; xx < this.size; xx++) {
                    int qx = px - half + xx;
                    int qy = py - half + yy;
                    switch (this.tool) {
                        case ERASER:
                            this.plot(qx, qy, old -> 0);
                            break;
                        case SHADE:
                            this.plotShade(qx, qy);
                            if (this.mirrorX) {
                                this.plotShade(this.doc.w - 1 - qx, qy);
                            }

                            if (this.mirrorY) {
                                this.plotShade(qx, this.doc.h - 1 - qy);
                            }
                            break;
                        default:
                            this.plot(qx, qy, old -> col);
                    }
                }
            }
        }
    }

    private void plotShade(int qx, int qy) {
        if (this.doc.in(qx, qy)) {
            int i = qy * this.doc.w + qx;
            if (!this.touched[i] && this.allowed(i)) {
                this.touched[i] = true;
                int[] lp = this.doc.layer().px;
                lp[i] = PixelOps.shade(this.base[i], this.button == 1 ? 1 : -1, this.hueShift);
            }
        }
    }

    private void pixelPerfectAdd(int px, int py, int col) {
        if (!this.strokePts.isEmpty()) {
            int[] last = this.strokePts.get(this.strokePts.size() - 1);
            if (last[0] == px && last[1] == py) {
                return;
            }
        }

        this.strokePts.add(new int[]{px, py});
        this.plot(px, py, old -> col);
        int n = this.strokePts.size();
        if (n >= 3) {
            int[] a = this.strokePts.get(n - 3);
            int[] m = this.strokePts.get(n - 2);
            int[] c = this.strokePts.get(n - 1);
            boolean corner = Math.abs(a[0] - c[0]) == 1 && Math.abs(a[1] - c[1]) == 1 && (m[0] == a[0] || m[1] == a[1]) && (m[0] == c[0] || m[1] == c[1]);
            if (corner) {
                this.restorePx(m[0], m[1]);
                if (this.mirrorX) {
                    this.restorePx(this.doc.w - 1 - m[0], m[1]);
                }

                if (this.mirrorY) {
                    this.restorePx(m[0], this.doc.h - 1 - m[1]);
                }

                this.strokePts.remove(n - 2);
            }
        }
    }

    private void restorePx(int px, int py) {
        if (this.doc.in(px, py)) {
            int var10001 = py * this.doc.w + px;
            int var10003 = py * this.doc.w;
            this.doc.layer().px[var10001] = this.base[var10003 + px];
        }
    }

    private void shapeTo(int px, int py) {
        int[] lp = this.doc.layer().px;
        System.arraycopy(this.base, 0, lp, 0, lp.length);
        int col = this.button == 1 ? this.secondary : this.primary;
        switch (this.tool) {
            case RECT:
                PixelOps.rect(this.sx, this.sy, px, py, this.filled, (qx, qy) -> this.plot(qx, qy, o -> col));
                break;
            case ELLIPSE:
                PixelOps.ellipse(this.sx, this.sy, px, py, this.filled, (qx, qy) -> this.plot(qx, qy, o -> col));
                break;
            case LINE:
                int ex = px;
                int ey = py;
                if (UI.shift(this.modsNow())) {
                    int dx = px - this.sx;
                    int dy = py - this.sy;
                    if (Math.abs(dx) > 2 * Math.abs(dy)) {
                        ey = this.sy;
                    } else if (Math.abs(dy) > 2 * Math.abs(dx)) {
                        ex = this.sx;
                    } else {
                        int d = Math.max(Math.abs(dx), Math.abs(dy));
                        ex = this.sx + Integer.signum(dx) * d;
                        ey = this.sy + Integer.signum(dy) * d;
                    }
                }

                PixelOps.line(this.sx, this.sy, ex, ey, (qx, qy) -> this.plot(qx, qy, o -> col));
                break;
            case GRADIENT:
                this.gradient(px, py);
        }
    }

    private boolean[] gradientMask(int px, int py) {
        int[] lp = this.doc.layer().px;
        boolean[] m;
        if (this.selection != null) {
            m = (boolean[])this.selection.clone();
        } else if (this.gradArea == 0 && this.doc.in(px, py)) {
            m = PixelOps.region(lp, this.doc.w, this.doc.h, px, py, true, this.tolerance);
        } else if (this.gradArea == 1) {
            m = new boolean[lp.length];

            for (int i = 0; i < lp.length; i++) {
                m[i] = lp[i] >>> 24 > 0;
            }
        } else {
            m = new boolean[lp.length];
            Arrays.fill(m, true);
        }

        return m;
    }

    private void gradient(int ex, int ey) {
        int[] lp = this.doc.layer().px;
        float dx = ex - this.sx;
        float dy = ey - this.sy;
        float len2 = dx * dx + dy * dy;
        if (!(len2 < 1.0F)) {
            float len = (float)Math.sqrt(len2);
            int[] fixed = this.gradColors == 2 ? null : this.gradientColors(this.primary);

            for (int i = 0; i < lp.length; i++) {
                if (this.gradMask == null || this.gradMask[i]) {
                    int px = i % this.doc.w;
                    int py = i / this.doc.w;
                    float t;
                    if (this.gradRadial) {
                        t = (float)Math.sqrt((px - this.sx) * (px - this.sx) + (py - this.sy) * (py - this.sy)) / len;
                    } else {
                        t = ((px - this.sx) * dx + (py - this.sy) * dy) / len2;
                    }

                    int[] cols = fixed != null ? fixed : this.gradientColors(this.base[i]);
                    if (fixed != null || this.base[i] >>> 24 != 0) {
                        int idx = PixelOps.gradientIndex(t, cols.length, px, py, this.gradDither, this.gradSpread);
                        lp[i] = cols[idx];
                    }
                }
            }
        }
    }

    private void updateRectSelection(int px, int py) {
        if (this.selectAdd && this.lifted == null) {
            this.lifted = this.selection == null ? new boolean[this.doc.w * this.doc.h] : (boolean[])this.selection.clone();
        }

        boolean[] n = this.selectAdd && this.lifted != null ? (boolean[])this.lifted.clone() : new boolean[this.doc.w * this.doc.h];
        int ax = Math.max(0, Math.min(this.sx, px));
        int bx = Math.min(this.doc.w - 1, Math.max(this.sx, px));
        int ay = Math.max(0, Math.min(this.sy, py));
        int by = Math.min(this.doc.h - 1, Math.max(this.sy, py));

        for (int yy = ay; yy <= by; yy++) {
            for (int xx = ax; xx <= bx; xx++) {
                n[yy * this.doc.w + xx] = true;
            }
        }

        this.selection = n;
    }

    private void moveTo(int ddx, int ddy) {
        int[] lp = this.doc.layer().px;
        int n = lp.length;

        for (int i = 0; i < n; i++) {
            lp[i] = this.lifted != null && !this.lifted[i] ? this.base[i] : 0;
        }

        for (int i = 0; i < n; i++) {
            if (this.lifted == null || this.lifted[i]) {
                int tx = i % this.doc.w + ddx;
                int ty = i / this.doc.w + ddy;
                if (this.doc.in(tx, ty) && this.base[i] >>> 24 > 0) {
                    lp[ty * this.doc.w + tx] = this.base[i];
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.doc == null) {
            return false;
        } else {
            boolean ctrl = UI.ctrl(mods);
            boolean shift = UI.shift(mods);
            if (ctrl) {
                switch (key) {
                    case 65:
                        this.selection = new boolean[this.doc.w * this.doc.h];
                        Arrays.fill(this.selection, true);
                        break;
                    case 66:
                    case 69:
                    case 70:
                    case 71:
                    case 72:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                    case 80:
                    case 81:
                    case 82:
                    case 84:
                    case 85:
                    case 87:
                    default:
                        return false;
                    case 67:
                        this.copy();
                        break;
                    case 68:
                        this.selection = null;
                        break;
                    case 73:
                        this.invertSelection();
                        break;
                    case 83:
                        this.save();
                        break;
                    case 86:
                        this.paste();
                        break;
                    case 88:
                        this.copy();
                        this.clearSelection();
                        break;
                    case 89:
                        this.redo();
                        break;
                    case 90:
                        if (shift) {
                            this.redo();
                        } else {
                            this.undo();
                        }
                }

                this.drawVersion++;
                return true;
            } else {
                for (TextureEditor.Tool t : TextureEditor.Tool.values()) {
                    if (key == InputConstants.getKey("key.keyboard." + t.key.toLowerCase(Locale.ROOT)).getValue()) {
                        this.setTool(t);
                        return true;
                    }
                }

                switch (key) {
                    case 45:
                    case 333:
                        this.zoomBy(-1, this.canvasX + this.canvasW / 2.0, this.canvasY + this.canvasH / 2.0);
                        break;
                    case 48:
                        this.fit();
                        break;
                    case 61:
                    case 334:
                        this.zoomBy(1, this.canvasX + this.canvasW / 2.0, this.canvasY + this.canvasH / 2.0);
                        break;
                    case 72:
                        this.mirrorX = !this.mirrorX;
                        break;
                    case 88:
                        this.swapColors();
                        break;
                    case 91:
                        this.size = Math.max(1, this.size - 1);
                        break;
                    case 93:
                        this.size = Math.min(16, this.size + 1);
                        break;
                    case 256:
                        if (this.selection == null) {
                            return false;
                        }

                        this.selection = null;
                        break;
                    case 259:
                    case 261:
                        this.clearSelection();
                        break;
                    case 262:
                    case 263:
                    case 264:
                    case 265:
                        if (this.tool != TextureEditor.Tool.MOVE || !this.editable()) {
                            return false;
                        }

                        int ddx = key == 263 ? -1 : (key == 262 ? 1 : 0);
                        int ddy = key == 265 ? -1 : (key == 264 ? 1 : 0);
                        this.doc.checkpoint();
                        this.base = (int[])this.doc.layer().px.clone();
                        this.lifted = this.selection != null ? (boolean[])this.selection.clone() : null;
                        this.moveTo(ddx, ddy);
                        if (this.lifted != null) {
                            boolean[] n = new boolean[this.lifted.length];

                            for (int i = 0; i < this.lifted.length; i++) {
                                if (this.lifted[i]) {
                                    int tx = i % this.doc.w + ddx;
                                    int ty = i / this.doc.w + ddy;
                                    if (this.doc.in(tx, ty)) {
                                        n[ty * this.doc.w + tx] = true;
                                    }
                                }
                            }

                            this.selection = n;
                        }

                        this.base = null;
                        this.lifted = null;
                        break;
                    default:
                        return false;
                }

                this.drawVersion++;
                return true;
            }
        }
    }

    private void setTool(TextureEditor.Tool t) {
        this.tool = t;
    }

    private void clearSelection() {
        if (this.selection != null && this.editable()) {
            this.doc.checkpoint();
            int[] lp = this.doc.layer().px;

            for (int i = 0; i < lp.length; i++) {
                if (this.selection[i]) {
                    lp[i] = 0;
                }
            }
        }
    }

    private void invertSelection() {
        if (this.selection == null) {
            this.selection = new boolean[this.doc.w * this.doc.h];
        } else {
            for (int i = 0; i < this.selection.length; i++) {
                this.selection[i] = !this.selection[i];
            }
        }
    }

    private void copy() {
        int ax = this.doc.w;
        int ay = this.doc.h;
        int bx = -1;
        int by = -1;

        for (int i = 0; i < this.doc.w * this.doc.h; i++) {
            if (this.selection == null || this.selection[i]) {
                int px = i % this.doc.w;
                int py = i / this.doc.w;
                ax = Math.min(ax, px);
                ay = Math.min(ay, py);
                bx = Math.max(bx, px);
                by = Math.max(by, py);
            }
        }

        if (bx >= 0) {
            this.clipX = ax;
            this.clipY = ay;
            this.clipW = bx - ax + 1;
            this.clipH = by - ay + 1;
            this.clipboard = new int[this.clipW * this.clipH];
            int[] lp = this.doc.layer().px;

            for (int yy = 0; yy < this.clipH; yy++) {
                for (int xx = 0; xx < this.clipW; xx++) {
                    int ix = (ay + yy) * this.doc.w + ax + xx;
                    this.clipboard[yy * this.clipW + xx] = this.selection != null && !this.selection[ix] ? 0 : lp[ix];
                }
            }

            UI ui = UI.get();
            if (ui != null) {
                ui.toastInfo("Скопировано " + this.clipW + "×" + this.clipH);
            }
        }
    }

    private void paste() {
        if (this.clipboard != null) {
            this.doc.checkpoint();
            PixelDoc.Layer l = this.doc.addLayer("Вставка");
            this.selection = new boolean[this.doc.w * this.doc.h];

            for (int yy = 0; yy < this.clipH; yy++) {
                for (int xx = 0; xx < this.clipW; xx++) {
                    int tx = this.clipX + xx;
                    int ty = this.clipY + yy;
                    if (this.doc.in(tx, ty)) {
                        int c = this.clipboard[yy * this.clipW + xx];
                        l.px[ty * this.doc.w + tx] = c;
                        if (c >>> 24 > 0) {
                            this.selection[ty * this.doc.w + tx] = true;
                        }
                    }
                }
            }

            this.tool = TextureEditor.Tool.MOVE;
        }
    }

    private void newLayer() {
        this.doc.checkpoint();
        this.doc.addLayer("Слой " + (this.doc.layers.size() + 1));
    }

    private void duplicateLayer() {
        this.doc.checkpoint();
        PixelDoc.Layer src = this.doc.layer();
        PixelDoc.Layer l = this.doc.addLayer(src.name + " копия");
        l.px = (int[])src.px.clone();
        l.opacity = src.opacity;
    }

    private void moveLayer(int dir) {
        int to = this.doc.active + dir;
        if (to >= 0 && to < this.doc.layers.size()) {
            this.doc.checkpoint();
            Collections.swap(this.doc.layers, this.doc.active, to);
            this.doc.active = to;
        }
    }

    private void mergeDown() {
        if (this.doc.active > 0) {
            this.doc.checkpoint();
            this.doc.mergeDown();
        }
    }

    private void deleteLayer() {
        if (this.doc.layers.size() <= 1) {
            UI ui = UI.get();
            if (ui != null) {
                ui.toastInfo("Последний слой не удалить");
            }
        } else {
            this.doc.checkpoint();
            this.doc.layers.remove(this.doc.active);
            this.doc.active = Math.max(0, this.doc.active - 1);
        }
    }

    private void setOpacity(PixelDoc.Layer l, int v) {
        this.doc.checkpoint();
        l.opacity = Math.max(0, Math.min(100, v));
    }

    private void renameLayer(PixelDoc.Layer l) {
        Dialog.prompt("Слой", "Имя слоя:", l.name, v -> v.isBlank() ? "Введите имя" : null, v -> {
            this.doc.checkpoint();
            l.name = v;
        });
    }

    @Override
    public boolean mouseDoubleClicked(double mx, double my, int b) {
        int rx = this.x + this.w - 240;
        if (mx >= rx) {
            for (TextureEditor.Hit hh : this.hits) {
                if (mx >= hh.x
                    && mx < hh.x + hh.w
                    && my >= hh.y
                    && my < hh.y + hh.h
                    && hh.right != null
                    && hh.tip != null
                    && hh.tip.startsWith("ЛКМ — выбрать")) {
                    hh.right.run();
                    return true;
                }
            }
        }

        return this.mousePressed(mx, my, b);
    }

    private void layerMenu(int mx, int my) {
        PopupMenu pm = new PopupMenu();
        pm.item("Отразить по горизонтали", Icon.REFRESH, null, () -> this.transform(0));
        pm.item("Отразить по вертикали", Icon.REFRESH, null, () -> this.transform(1));
        pm.item("Повернуть на 180°", Icon.REFRESH, null, () -> this.transform(2));
        if (this.doc.w == this.doc.h) {
            pm.item("Повернуть на 90° по часовой", Icon.REFRESH, null, () -> this.transform(3));
            pm.item("Повернуть на 90° против", Icon.REFRESH, null, () -> this.transform(4));
        }

        pm.separator();
        pm.item("Обводка выделенного (основным цветом)", Icon.EDIT, null, this::outline);
        pm.item("Очистить слой", Icon.TRASH, null, () -> {
            if (this.editable()) {
                this.doc.checkpoint();
                Arrays.fill(this.doc.layer().px, 0);
            }
        }).color(-1023878);
        pm.show(mx, my);
    }

    private void transform(int kind) {
        if (this.editable()) {
            this.doc.checkpoint();
            int[] src = (int[])this.doc.layer().px.clone();
            int[] dst = new int[src.length];
            int W = this.doc.w;
            int H = this.doc.h;

            for (int yy = 0; yy < H; yy++) {
                for (int xx = 0; xx < W; xx++) {
                    int v = src[yy * W + xx];
                    int tx;
                    int ty;
                    switch (kind) {
                        case 0:
                            tx = W - 1 - xx;
                            ty = yy;
                            break;
                        case 1:
                            tx = xx;
                            ty = H - 1 - yy;
                            break;
                        case 2:
                            tx = W - 1 - xx;
                            ty = H - 1 - yy;
                            break;
                        case 3:
                            tx = H - 1 - yy;
                            ty = xx;
                            break;
                        default:
                            tx = yy;
                            ty = W - 1 - xx;
                    }

                    dst[ty * W + tx] = v;
                }
            }

            if (this.selection != null) {
                for (int i = 0; i < src.length; i++) {
                    if (!this.selection[i]) {
                        dst[i] = src[i];
                    }
                }
            }

            this.doc.layer().px = dst;
        }
    }

    private void outline() {
        if (this.editable()) {
            this.doc.checkpoint();
            int[] lp = this.doc.layer().px;
            int[] src = (int[])lp.clone();

            for (int py = 0; py < this.doc.h; py++) {
                for (int px = 0; px < this.doc.w; px++) {
                    int i = py * this.doc.w + px;
                    if (src[i] >>> 24 <= 0) {
                        boolean near = false;
                        int[][] n = new int[][]{{px - 1, py}, {px + 1, py}, {px, py - 1}, {px, py + 1}};

                        for (int[] q : n) {
                            if (this.doc.in(q[0], q[1])
                                && src[q[1] * this.doc.w + q[0]] >>> 24 > 0
                                && (this.selection == null || this.selection[q[1] * this.doc.w + q[0]])) {
                                near = true;
                            }
                        }

                        if (near) {
                            lp[i] = this.primary;
                        }
                    }
                }
            }
        }
    }

    private void canvasMenu(int mx, int my) {
        PopupMenu pm = new PopupMenu();
        pm.item("Вписать в окно", Icon.SEARCH, "0", this::fit);
        pm.item("Размер холста…", Icon.EDIT, null, this::resizeDialog);
        pm.separator();

        for (int[] s : new int[][]{{16, 16}, {32, 32}, {64, 64}, {128, 64}}) {
            pm.item("Холст " + s[0] + "×" + s[1] + (s[0] == 64 && s[1] == 64 ? " (скин)" : (s[0] == 128 ? " (Spraute)" : "")), Icon.IMAGE, null, () -> {
                this.doc.checkpoint();
                this.doc.resize(s[0], s[1], 0, 0);
                this.selection = null;
                this.fit();
            });
        }

        pm.show(mx, my);
    }

    private void resizeDialog() {
        Dialog d = new Dialog("Размер холста", Icon.IMAGE);
        d.body.add(new Label("Ширина и высота в пикселях (картинка остаётся в левом верхнем углу или по выбору):").color(-6577995));
        TextField fw = new TextField();
        TextField fh = new TextField();
        fw.setText(String.valueOf(this.doc.w));
        fh.setText(String.valueOf(this.doc.h));
        d.body.add(fw);
        d.body.add(fh);
        int[] anchor = new int[]{0};
        d.body
            .add(
                new Dropdown<>(
                    () -> List.of(0, 1, 2),
                    () -> anchor[0],
                    v -> v == 0 ? "Привязка: левый верхний угол" : (v == 1 ? "Привязка: центр" : "Привязка: правый нижний угол"),
                    v -> anchor[0] = v
                )
            );
        d.addButton("Отмена", Button.Style.DEFAULT, d::close);
        d.addButton("OK", Button.Style.PRIMARY, () -> {
            try {
                int nw = Math.max(1, Math.min(1024, Integer.parseInt(fw.text().trim())));
                int nh = Math.max(1, Math.min(1024, Integer.parseInt(fh.text().trim())));
                d.close();
                this.doc.checkpoint();
                this.doc.resize(nw, nh, anchor[0], anchor[0]);
                this.selection = null;
                this.fit();
            } catch (NumberFormatException var7) {
                UI ui = UI.get();
                if (ui != null) {
                    ui.toastError("Нужны числа");
                }
            }
        });
        d.open();
    }

    @Override
    public void undo() {
        if (this.doc.undo()) {
            this.selection = null;
            this.drawVersion++;
        }
    }

    @Override
    public void redo() {
        if (this.doc.redo()) {
            this.drawVersion++;
        }
    }

    @Override
    public void save() {
        if (this.doc != null) {
            try {
                this.doc.save(this.image.path());
                ProjectImages.load(this.project, this.image);
                UI ui = UI.get();
                if (ui != null) {
                    ui.toastSuccess("Сохранено: " + this.image.name());
                }
            } catch (IOException var3) {
                UI uix = UI.get();
                if (uix != null) {
                    uix.toastError("Не удалось сохранить: " + var3.getMessage());
                }
            }
        }
    }

    public void closed() {
        if (this.tex != null) {
            Minecraft.getInstance().getTextureManager().release(this.texId);
            this.tex = null;
        }

        if (this.checker != null) {
            Minecraft.getInstance().getTextureManager().release(this.checkerId);
            this.checker = null;
        }
    }

    @Override
    public boolean isDirty() {
        return this.doc != null && this.doc.dirty();
    }

    @Override
    public Icon icon() {
        return Icon.IMAGE;
    }

    @Override
    public int iconColor() {
        return -10638858;
    }

    @Override
    public String source() {
        return null;
    }

    @Override
    public int scriptId() {
        return -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        return List.of();
    }

    @Override
    public Analyzer.Result analysis() {
        return null;
    }

    @Override
    public void reveal(int offset) {
    }

    @Override
    public void focusContent() {
        this.requestFocus();
    }

    private record Hit(int x, int y, int w, int h, Runnable left, Runnable right, String tip) {
    }

    private record Part(String name, int x, int y, int w, int h, int color) {
    }

    static enum Tool {
        PENCIL(
            "Карандаш", "B", new String[]{".......##", "......###", ".....###.", "....###..", "...###...", "..###....", ".###.....", "##.......", "#........"}
        ),
        ERASER("Ластик", "E", new String[]{"....####.", "...#...##", "..#...#.#", ".#...#..#", "#####..#.", "#...#.#..", "#...##...", "#####....", "........."}),
        FILL("Заливка", "G", new String[]{"...#.....", "..#.#....", ".#...#...", "#.....#..", ".#...#.#.", "..#.#..#.", "...#...#.", ".......##", "........."}),
        LINE("Линия", "L", new String[]{"........#", ".......#.", "......#..", ".....#...", "....#....", "...#.....", "..#......", ".#.......", "#........"}),
        RECT(
            "Прямоугольник",
            "R",
            new String[]{"#########", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", "#########"}
        ),
        ELLIPSE(
            "Эллипс", "O", new String[]{"...###...", ".##...##.", ".#.....#.", "#.......#", "#.......#", "#.......#", ".#.....#.", ".##...##.", "...###..."}
        ),
        GRADIENT(
            "Градиент (пиксельный, с шейдом)",
            "D",
            new String[]{"#########", "#########", "##.###.##", "#.#.#.#.#", ".#.#.#.#.", "#...#...#", "..#...#..", ".........", "........."}
        ),
        SHADE(
            "Шейд: ЛКМ — тень, ПКМ — свет",
            "S",
            new String[]{"..#####..", ".###...#.", "####....#", "####....#", "####....#", "####....#", "####....#", ".###...#.", "..#####.."}
        ),
        PICKER(
            "Пипетка (или Alt + клик)",
            "I",
            new String[]{"......##.", ".....####", "......##.", ".....#.#.", "....#....", "...#.....", "..#......", ".#.......", "#........"}
        ),
        SELECT(
            "Выделение", "M", new String[]{"##.##.##.", "........#", "#........", "#.......#", ".........", "#.......#", "#........", "........#", ".##.##.##"}
        ),
        WAND(
            "Волшебная палочка",
            "W",
            new String[]{"......#..", ".....###.", "......#..", ".....#...", "....#....", "...#.....", "..#......", ".#.......", "#........"}
        ),
        MOVE(
            "Перемещение (выделения или слоя)",
            "V",
            new String[]{"....#....", "...###...", "....#....", ".#..#..#.", "#########", ".#..#..#.", "....#....", "...###...", "....#...."}
        );

        final String title;
        final String key;
        final String[] glyph;

        private Tool(String title, String key, String[] glyph) {
            this.title = title;
            this.key = key;
            this.glyph = glyph;
        }
    }
}
