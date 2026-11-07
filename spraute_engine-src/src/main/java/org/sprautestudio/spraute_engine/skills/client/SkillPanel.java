package org.sprautestudio.spraute_engine.skills.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.LiveDraw;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.skills.SkillLogic;
import org.sprautestudio.spraute_engine.skills.SkillLook;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.skills.Skills;

public final class SkillPanel {
    private static final int HEADER = 34;
    private static final int TABS = 20;
    private static final int DETAIL = 176;
    private static final int GAP = 6;
    private static final int LABEL_ROOM = 44;
    public Skills.View view;
    public String selected;
    private String section;
    private int panX;
    private int panY;
    private boolean fitted;
    private double scroll;
    private int x;
    private int y;
    private int w;
    private int h;
    private int ax;
    private int ay;
    private int aw;
    private int ah;
    private int[] learnButton;
    private int[] useButton;
    private int[] resetButton;
    private final List<int[]> tabRects = new ArrayList<>();
    private List<String> tabs = List.of();
    private SkillTree.Node hovered;
    private SkillTree.Node hoveredBefore;
    private float zoom = 1.0F;
    private boolean zoomedByHand;
    boolean customTooltip;

    public SkillPanel(Skills.View view) {
        this.view = view;
    }

    public void setView(Skills.View v) {
        this.view = v;
        if (this.selected != null && v.tree.node(this.selected) == null) {
            this.selected = null;
        }
    }

    private SkillTree tree() {
        return this.view.tree;
    }

    private SkillLogic.State state() {
        return this.view.state;
    }

    private SkillTree.Menu menu() {
        return this.tree().menu(this.view.menu);
    }

    private SkillLook look() {
        return this.menu().ownLook ? this.menu().look : EngineClient.config().skills;
    }

    private int accent() {
        return 0xFF000000 | this.look().accent;
    }

    private boolean side() {
        return this.menu().interact.equals("side");
    }

    private int bodyW() {
        return this.aw;
    }

    private int bodyTop() {
        return this.ay;
    }

    private static ItemStack stack(String id) {
        return id != null && !id.isBlank() ? ItemSpec.stack(id, 1) : ItemStack.EMPTY;
    }

    private List<SkillTree.Node> shown() {
        List<SkillTree.Node> out = new ArrayList<>();

        for (SkillTree.Node n : this.tree().nodes) {
            if ((this.menu().sections.isEmpty() || this.menu().sections.contains(n.section)) && (this.tabs.size() <= 1 || n.section.equals(this.section))) {
                out.add(n);
            }
        }

        return out;
    }

    private void findTabs() {
        List<String> found = new ArrayList<>();

        for (SkillTree.Node n : this.tree().nodes) {
            if ((this.menu().sections.isEmpty() || this.menu().sections.contains(n.section)) && !found.contains(n.section)) {
                found.add(n.section);
            }
        }

        this.tabs = found;
        if (this.section == null || !found.contains(this.section)) {
            this.section = found.isEmpty() ? "" : found.get(0);
            this.fitted = false;
        }
    }

    public void draw(Gfx g, Font font, int x, int y, int w, int h, double mx, double my) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.hovered = null;
        this.learnButton = this.useButton = this.resetButton = null;
        this.findTabs();
        this.ay = y + 34 + (this.tabs.size() > 1 ? 20 : 0);
        this.ax = x;
        this.aw = this.side() ? w - 176 : w;
        this.ah = y + h - this.ay;
        SkillLook l = this.look();
        g.fill(x, y, x + w, y + h, l.panel);
        if (l.panelImage.isSet()) {
            Skins.draw(g, this.view.project, l.panelImage, x, y, w, h);
        }

        if (!this.menu().background.isBlank()) {
            EngineConfig.Skin skin = new EngineConfig.Skin();
            skin.image = this.menu().background;
            Skins.draw(g, this.view.project, skin, x, y, w, h);
        }

        int[] whole = ChatHeads.screenRect(g, x, y, w, h);
        g.enableScissor(whole[0], whole[1], whole[2], whole[3]);
        this.layers(g, x, y, w, h, mx, my);
        g.disableScissor();
        this.header(g, font, l, mx, my);
        if (this.tabs.size() > 1) {
            this.tabRow(g, font, l, mx, my);
        }

        int[] clip = ChatHeads.screenRect(g, this.ax, this.ay, this.aw, this.ah);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        if (this.menu().view.equals("grid")) {
            this.grid(g, font, l, mx, my);
        } else {
            this.field(g, font, l, mx, my);
        }

        g.disableScissor();
        if (this.side()) {
            this.detail(g, font, l, mx, my);
        } else {
            if (this.tree().allowReset && !this.state().ranks.isEmpty()) {
                this.resetButton = this.button(g, font, l, "Сбросить всё", x + w - 96, y + h - 24, 88, true, mx, my);
            }

            if (this.hovered != null) {
                this.tooltip(g, font, l, this.hovered, mx, my);
            }
        }
    }

    private void header(Gfx g, Font font, SkillLook l, double mx, double my) {
        g.fill(this.x, this.y, this.x + this.w, this.y + 34, l.header);
        g.fill(this.x, this.y + 34 - 1, this.x + this.w, this.y + 34, l.border);
        String title = this.menu().title.isBlank() ? this.tree().title : this.menu().title;
        g.drawString(font, title, this.x + 10, this.y + 7, this.accent(), false);
        if (this.tree().levels && this.menu().showLevel) {
            boolean top = this.state().level >= this.tree().maxLevel;
            String level = "Уровень " + this.state().level;
            int lx = this.x + 10;
            g.drawString(font, level, lx, this.y + 19, l.text, false);
            int barX = lx + font.width(level) + 8;
            int barW = Math.min(150, Math.max(60, this.w / 5));
            g.fill(barX, this.y + 21, barX + barW, this.y + 26, l.tile);
            long need = SkillLogic.xpToNext(this.tree(), this.state().level);
            int[] bar = new int[]{barX, this.y + 21, barW, 5};
            if (top) {
                g.fill(barX, this.y + 21, barX + barW, this.y + 26, this.accent());
                g.drawString(font, "максимум", barX + barW + 6, this.y + 19, l.muted, false);
            } else {
                int[] part = fillRect(bar, Math.min(1.0, (double)this.state().xp / Math.max(1L, need)), this.menu().xpDirection);
                g.fill(part[0], part[1], part[0] + part[2], part[1] + part[3], this.accent());
                g.drawString(font, this.state().xp + " / " + need, barX + barW + 6, this.y + 19, l.muted, false);
            }
        }

        if (this.menu().showPoints) {
            int rx = this.x + this.w - 10;

            for (int i = this.tree().currencies.size() - 1; i >= 0; i--) {
                SkillTree.Currency c = this.tree().currencies.get(i);
                String n = String.valueOf(this.state().points(c.id));
                rx -= font.width(n);
                g.drawString(font, n, rx, this.y + 13, 0xFF000000 | c.color, true);
                rx -= 18;
                this.art(g, c.icon, c.image, rx, this.y + 9, 16);
                if (mx >= rx && mx < rx + 16 + font.width(n) && my >= this.y + 8 && my < this.y + 26) {
                    int nw = font.width(c.name);
                    g.drawString(font, c.name, rx - nw - 4, this.y + 13, l.muted, false);
                }

                rx -= 10;
            }
        }
    }

    private void tabRow(Gfx g, Font font, SkillLook l, double mx, double my) {
        this.tabRects.clear();
        int tx = this.x + 6;
        int ty = this.y + 34;
        g.fill(this.x, ty, this.x + this.w, ty + 20, l.header);

        for (String s : this.tabs) {
            String name = s.isEmpty() ? "Основное" : s;
            int tw = font.width(name) + 14;
            boolean on = s.equals(this.section);
            boolean hover = mx >= tx && mx < tx + tw && my >= ty && my < ty + 20;
            if (on) {
                g.fill(tx, ty + 20 - 2, tx + tw, ty + 20, this.accent());
            }

            g.drawString(font, name, tx + 7, ty + 6, on ? l.text : (hover ? l.text : l.muted), false);
            this.tabRects.add(new int[]{tx, ty, tw, 20});
            tx += tw + 2;
        }

        g.fill(this.x, ty + 20 - 1, this.x + this.w, ty + 20, l.border);
    }

    private void art(Gfx g, String item, String image, int ax, int ay, int size) {
        art(g, this.view.project, item, image, ax, ay, size);
    }

    static void art(Gfx g, String project, String item, String image, int ax, int ay, int size) {
        if (image != null && !image.isBlank()) {
            EngineConfig.Skin skin = new EngineConfig.Skin();
            skin.image = image;
            Skins.draw(g, project, skin, ax, ay, size, size);
        } else {
            ItemStack st = stack(item);
            if (!st.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate((float)ax, (float)ay, 0.0F);
                g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
                g.renderItem(st, 0, 0);
                g.pose().popPose();
            }
        }
    }

    private void plate(Gfx g, SkillLook l, SkillTree.Node n, int px, int py, int pw, int ph, boolean hover) {
        int rank = this.state().rank(n.id);
        boolean can = SkillLogic.refusal(this.tree(), this.state(), n) == null;
        int edge = n.id.equals(this.selected) ? -1 : (rank > 0 ? this.accent() : (can ? l.good : l.border));
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, edge);
        EngineConfig.Skin skin = rank > 0 && l.tileLearned.isSet() ? l.tileLearned : (rank == 0 && !can && l.tileLocked.isSet() ? l.tileLocked : l.tileImage);
        if (skin.isSet()) {
            Skins.draw(g, this.view.project, skin, px, py, pw, ph);
        } else {
            g.fill(px, py, px + pw, py + ph, rank > 0 ? blend(this.accent(), l.tile, 0.3F) : (hover ? blend(-1, l.tile, 0.06F) : l.tile));
        }
    }

    private int nodeSize() {
        String var1 = this.menu().size;

        return switch (var1) {
            case "compact" -> 20;
            case "big" -> 34;
            default -> 26;
        };
    }

    private int nodeX(SkillTree.Node n) {
        return n.x;
    }

    private int nodeY(SkillTree.Node n) {
        return n.y;
    }

    private void layers(Gfx g, int rx, int ry, int rw, int rh, double mx, double my) {
        for (SkillTree.Layer layer : this.menu().layers) {
            if (!layer.image.isBlank()) {
                float kx = (float)Math.max(-1.0, Math.min(1.0, (mx - (rx + rw / 2.0F)) / Math.max(1.0F, rw / 2.0F)));
                float ky = (float)Math.max(-1.0, Math.min(1.0, (my - (ry + rh / 2.0F)) / Math.max(1.0F, rh / 2.0F)));
                int ex = (int)Math.ceil(Math.abs(layer.depthX));
                int ey = (int)Math.ceil(Math.abs(layer.depthY));
                imageOr(
                    g,
                    this.view.project,
                    layer.image,
                    0,
                    rx - ex + Math.round(layer.depthX * kx),
                    ry - ey + Math.round(layer.depthY * ky),
                    rw + ex * 2,
                    rh + ey * 2
                );
            }
        }
    }

    static int[] fillRect(int[] r, double share, String direction) {
        int fw = (int)Math.round(r[2] * share);
        int fh = (int)Math.round(r[3] * share);
        String var6 = direction == null ? "" : direction;

        return switch (var6) {
            case "left" -> new int[]{r[0] + r[2] - fw, r[1], fw, r[3]};
            case "up" -> new int[]{r[0], r[1] + r[3] - fh, r[2], fh};
            case "down" -> new int[]{r[0], r[1], r[2], fh};
            default -> new int[]{r[0], r[1], fw, r[3]};
        };
    }

    private void fit() {
        if (!this.fitted) {
            this.fitted = true;
            int size = this.nodeSize();
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;

            for (SkillTree.Node n : this.shown()) {
                minX = Math.min(minX, n.x);
                minY = Math.min(minY, n.y);
                maxX = Math.max(maxX, n.x + size);
                maxY = Math.max(maxY, n.y + size);
            }

            if (minX != Integer.MAX_VALUE) {
                if (!this.zoomedByHand) {
                    float wanted = this.menu().zoom;
                    this.zoom = wanted > 0.0F
                        ? wanted
                        : Math.max(
                            0.2F,
                            Math.min(1.5F, Math.min((this.bodyW() - 16.0F) / Math.max(1, maxX - minX + 44), (this.ah - 16.0F) / Math.max(1, maxY - minY + 14)))
                        );
                }

                this.panX = Math.round((this.bodyW() - (maxX + minX) * this.zoom) / 2.0F);
                this.panY = Math.round((this.ah - (maxY + minY) * this.zoom) / 2.0F);
            }
        }
    }

    public void beginParts() {
        this.hoveredBefore = this.hovered;
        this.hovered = null;
        this.learnButton = this.useButton = this.resetButton = null;
        this.tabRects.clear();
        this.findTabs();
    }

    public String titleText() {
        return this.menu().title.isBlank() ? this.tree().title : this.menu().title;
    }

    public String selectedTitle() {
        SkillTree.Node n = this.selected != null ? this.tree().node(this.selected) : null;
        return n != null ? n.title : "";
    }

    static void imageOr(Gfx g, String project, String image, int color, int px, int py, int pw, int ph) {
        if (image != null && !image.isBlank()) {
            EngineConfig.Skin skin = new EngineConfig.Skin();
            skin.image = image;
            Skins.draw(g, project, skin, px, py, pw, ph);
        } else if (color >>> 24 != 0) {
            g.fill(px, py, px + pw, py + ph, color);
        }
    }

    static void imageOr(Gfx g, String project, EngineConfig.Skin skin, int color, int px, int py, int pw, int ph) {
        if (skin != null && skin.isSet()) {
            Skins.draw(g, project, skin, px, py, pw, ph);
        } else if (color >>> 24 != 0) {
            g.fill(px, py, px + pw, py + ph, color);
        }
    }

    private void imageOr(Gfx g, GuiDoc.Element e, String image, EngineConfig.Skin fallback, int color, int px, int py, int pw, int ph) {
        if (image != null && !image.isBlank()) {
            ResourceLocation rl = LiveDraw.tex(this.view.project, image, null, null);
            if (rl != null) {
                LiveDraw.image(g, e, rl, px, py, pw, ph, 1.0F);
            }
        } else {
            imageOr(g, this.view.project, fallback, color, px, py, pw, ph);
        }
    }

    public void partXp(Gfx g, int[] r, GuiDoc.Element e) {
        imageOr(g, this.view.project, e.get("back"), e.color("background"), r[0], r[1], r[2], r[3]);
        double share = this.state().level >= this.tree().maxLevel
            ? 1.0
            : Math.min(1.0, (double)this.state().xp / Math.max(1L, SkillLogic.xpToNext(this.tree(), this.state().level)));
        int[] part = fillRect(r, share, e.get("direction"));
        if (part[2] > 0 && part[3] > 0) {
            int own = e.color("color");
            String image = e.get("fill");
            if (image != null && !image.isBlank()) {
                int[] clip = ChatHeads.screenRect(g, part[0], part[1], part[2], part[3]);
                g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
                imageOr(g, this.view.project, image, 0, r[0], r[1], r[2], r[3]);
                g.disableScissor();
            } else {
                g.fill(part[0], part[1], part[0] + part[2], part[1] + part[3], own >>> 24 != 0 ? own : this.accent());
            }
        }
    }

    public void partPoints(Gfx g, Font font, int[] r, GuiDoc.Element e) {
        int icon = Math.min(16, r[3]);
        int ty = r[1] + (r[3] - 8) / 2;
        int iy = r[1] + (r[3] - icon) / 2;
        boolean right = !e.get("align").equals("left");
        int cx = right ? r[0] + r[2] : r[0];

        for (int i = 0; i < this.tree().currencies.size(); i++) {
            SkillTree.Currency c = this.tree().currencies.get(right ? this.tree().currencies.size() - 1 - i : i);
            String n = String.valueOf(this.state().points(c.id));
            int nw = font.width(n);
            if (right) {
                cx -= nw;
                g.drawString(font, n, cx, ty, 0xFF000000 | c.color, true);
                cx -= icon + 2;
                this.art(g, c.icon, c.image, cx, iy, icon);
                cx -= 8;
            } else {
                this.art(g, c.icon, c.image, cx, iy, icon);
                cx += icon + 2;
                g.drawString(font, n, cx, ty, 0xFF000000 | c.color, true);
                cx += nw + 8;
            }
        }
    }

    public void partTabs(Gfx g, Font font, int[] r, double mx, double my) {
        if (this.tabs.size() >= 2) {
            SkillLook l = this.look();
            int tx = r[0];

            for (String s : this.tabs) {
                String name = s.isEmpty() ? "Основное" : s;
                int tw = font.width(name) + 14;
                boolean on = s.equals(this.section);
                boolean hover = mx >= tx && mx < tx + tw && my >= r[1] && my < r[1] + r[3];
                if (on) {
                    g.fill(tx, r[1] + r[3] - 2, tx + tw, r[1] + r[3], this.accent());
                }

                g.drawString(font, name, tx + 7, r[1] + (r[3] - 8) / 2, !on && !hover ? l.muted : l.text, false);
                this.tabRects.add(new int[]{tx, r[1], tw, r[3]});
                tx += tw + 2;
            }
        }
    }

    public void partField(Gfx g, Font font, int[] r, double mx, double my) {
        boolean moved = r[0] != this.ax || r[1] != this.ay || r[2] != this.aw || r[3] != this.ah;
        this.x = this.ax = r[0];
        this.y = this.ay = r[1];
        this.w = this.aw = r[2];
        this.h = this.ah = r[3];
        if (moved) {
            this.fitted = false;
        }

        SkillLook l = this.look();
        int[] clip = ChatHeads.screenRect(g, this.ax, this.ay, this.aw, this.ah);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        imageOr(g, this.view.project, this.menu().background, 0, this.ax, this.ay, this.aw, this.ah);
        this.layers(g, this.ax, this.ay, this.aw, this.ah, mx, my);
        if (this.menu().view.equals("grid")) {
            this.grid(g, font, l, mx, my);
        } else {
            this.field(g, font, l, mx, my);
        }

        g.disableScissor();
        if (!this.side() && this.hovered != null && !this.customTooltip) {
            this.tooltip(g, font, l, this.hovered, mx, my);
        }
    }

    public void partTooltip(Gfx g, Font font, int[] r, GuiDoc.Element e, double mx, double my) {
        boolean sample = this.view.key.equals("sample");
        SkillTree.Node n = sample
            ? (this.selected != null ? this.tree().node(this.selected) : null)
            : (this.hovered != null ? this.hovered : this.hoveredBefore);
        if (n != null) {
            SkillLook l = this.look();
            int pad = e.i("pad");
            int tw = Math.max(20, r[2] - pad * 2);
            boolean canNow = e.b("hint") && !this.side() && this.state().rank(n.id) < n.maxRank && SkillLogic.refusal(this.tree(), this.state(), n) == null;
            int th = this.info(null, font, l, n, 0, 0, tw) + pad * 2 + (canNow ? 12 : 0);
            int bx = r[0];
            int by = r[1];
            if (e.b("follow") && !sample) {
                bx = (int)Math.min(mx + 12.0, (double)(this.ax + this.aw - r[2]));
                by = (int)Math.min(my + 8.0, (double)(this.ay + this.ah - th));
            }

            int edge = e.color("borderColor");
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 400.0F);
            g.fill(bx - 1, by - 1, bx + r[2] + 1, by + th + 1, edge >>> 24 != 0 ? edge : this.accent());
            this.imageOr(g, e, e.get("image"), l.tooltipImage, e.color("background"), bx, by, r[2], th);
            int end = this.info(g, font, l, n, bx + pad, by + pad, tw);
            if (canNow) {
                g.drawString(font, "Нажмите, чтобы изучить", bx + pad, end + 2, l.good, false);
            }

            g.pose().popPose();
        }
    }

    public void partInfo(Gfx g, Font font, int[] r) {
        SkillLook l = this.look();
        SkillTree.Node n = this.hovered != null && !this.side() ? this.hovered : (this.selected != null ? this.tree().node(this.selected) : null);
        int[] clip = ChatHeads.screenRect(g, r[0], r[1], r[2], r[3]);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        if (n == null) {
            this.wrapped(
                g,
                font,
                this.menu().view.equals("fog") ? "Новое откроется, когда изучите то, что уже видно." : "Нажмите на навык, чтобы узнать о нём.",
                r[0],
                r[1],
                r[2],
                l.muted
            );
        } else {
            this.info(g, font, l, n, r[0], r[1], r[2]);
        }

        g.disableScissor();
    }

    private int[] partButton(Gfx g, Font font, int[] r, GuiDoc.Element e, String text, boolean on, double mx, double my) {
        SkillLook l = this.look();
        boolean hover = on && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
        String image = !on && !e.get("offImage").isBlank()
            ? e.get("offImage")
            : (hover && !e.get("hoverImage").isBlank() ? e.get("hoverImage") : e.get("image"));
        this.imageOr(g, e, image, l.buttonImage, !on ? l.tile : (hover ? blend(-1, this.accent(), 0.2F) : this.accent()), r[0], r[1], r[2], r[3]);
        g.drawString(font, text, r[0] + (r[2] - font.width(text)) / 2, r[1] + (r[3] - 8) / 2, on ? -1 : l.muted, false);
        return on ? (int[])r.clone() : null;
    }

    public void partLearn(Gfx g, Font font, int[] r, GuiDoc.Element e, double mx, double my) {
        SkillTree.Node n = this.selected != null ? this.tree().node(this.selected) : null;
        if (n != null && this.state().rank(n.id) < n.maxRank) {
            this.learnButton = this.partButton(g, font, r, e, e.get("text"), SkillLogic.refusal(this.tree(), this.state(), n) == null, mx, my);
        }
    }

    public void partUse(Gfx g, Font font, int[] r, GuiDoc.Element e, double mx, double my) {
        SkillTree.Node n = this.selected != null ? this.tree().node(this.selected) : null;
        if (n != null && n.ability.on && this.state().rank(n.id) != 0) {
            Integer cd = this.view.cooldowns.get(n.id);
            boolean ready = cd == null || cd <= 0;
            this.useButton = this.partButton(g, font, r, e, ready ? e.get("text") : "Ждать " + (cd + 19) / 20 + " с", ready, mx, my);
        }
    }

    public void partReset(Gfx g, Font font, int[] r, GuiDoc.Element e, double mx, double my) {
        if (this.tree().allowReset && !this.state().ranks.isEmpty()) {
            this.resetButton = this.partButton(g, font, r, e, e.get("text"), true, mx, my);
        }
    }

    private void field(Gfx g, Font font, SkillLook l, double screenX, double screenY) {
        this.fit();
        int size = this.nodeSize();
        List<SkillTree.Node> ns = this.shown();
        double mx = (screenX - this.ax - this.panX) / this.zoom;
        double my = (screenY - this.ay - this.panY) / this.zoom;
        boolean inArea = screenX >= this.ax && screenX < this.ax + this.aw && screenY >= this.ay && screenY < this.ay + this.ah;
        g.pose().pushPose();
        g.pose().translate((float)(this.ax + this.panX), (float)(this.ay + this.panY), 0.0F);
        g.pose().scale(this.zoom, this.zoom, 1.0F);

        for (SkillTree.Node n : ns) {
            if (SkillLogic.visible(this.menu().view, this.state(), n)) {
                for (String pid : n.parents) {
                    SkillTree.Node p = this.tree().node(pid);
                    if (p != null && ns.contains(p) && SkillLogic.visible(this.menu().view, this.state(), p)) {
                        boolean learned = this.state().rank(pid) > 0;
                        this.linkStyled(
                            g,
                            l,
                            this.nodeX(p) + size / 2,
                            this.nodeY(p) + size / 2,
                            this.nodeX(n) + size / 2,
                            this.nodeY(n) + size / 2,
                            learned ? this.accent() : blend(l.border, -16777216, 0.9F),
                            this.menu().links,
                            learned,
                            size
                        );
                    }
                }
            }
        }

        for (SkillTree.Node nx : ns) {
            if (SkillLogic.visible(this.menu().view, this.state(), nx)) {
                int nxx = this.nodeX(nx);
                int ny = this.nodeY(nx);
                boolean hover = mx >= nxx && mx < nxx + size && my >= ny && my < ny + size && inArea;
                if (hover) {
                    this.hovered = nx;
                }

                this.plate(g, l, nx, nxx, ny, size, size, hover);
                int ic = size - 10;
                this.art(g, nx.icon, nx.image, nxx + 5, ny + 5, ic);
                int rank = this.state().rank(nx.id);
                if (rank == 0 && SkillLogic.refusal(this.tree(), this.state(), nx) != null) {
                    over(g, nxx, ny, size, size, -2146430440);
                }

                g.pose().pushPose();
                g.pose().translate(0.0F, 0.0F, 300.0F);
                if (nx.maxRank > 1) {
                    pips(g, nxx + 2, ny + size - 3, size - 4, rank, nx.maxRank, this.accent(), l.border);
                }

                Integer cd = this.view.cooldowns.get(nx.id);
                if (cd != null && cd > 0) {
                    g.drawString(font, String.valueOf((cd + 19) / 20), nxx + 2, ny + 2, l.bad, true);
                }

                String label = font.plainSubstrByWidth(nx.title, size + 44);
                g.drawString(font, label, nxx + (size - font.width(label)) / 2, ny + size + 3, rank > 0 ? l.text : l.muted, true);
                g.pose().popPose();
            }
        }

        g.pose().popPose();
    }

    private static void over(Gfx g, int ax, int ay, int aw, int ah, int color) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 250.0F);
        g.fill(ax, ay, ax + aw, ay + ah, color);
        g.pose().popPose();
    }

    private static void pips(Gfx g, int px, int py, int pw, int rank, int max, int on, int off) {
        if (max > 12) {
            g.fill(px, py, px + pw, py + 2, off);
            g.fill(px, py, px + pw * rank / max, py + 2, on);
        } else {
            int each = Math.max(2, (pw - (max - 1)) / max);

            for (int i = 0; i < max; i++) {
                g.fill(px + i * (each + 1), py, px + i * (each + 1) + each, py + 2, i < rank ? on : off);
            }
        }
    }

    public static void link(Gfx g, int ax, int ay, int bx, int by, int color, String style) {
        if (style.equals("elbow")) {
            int mid = (ax + bx) / 2;
            g.fill(Math.min(ax, mid), ay - 1, Math.max(ax, mid) + 1, ay + 1, color);
            g.fill(mid - 1, Math.min(ay, by), mid + 1, Math.max(ay, by) + 1, color);
            g.fill(Math.min(mid, bx), by - 1, Math.max(mid, bx) + 1, by + 1, color);
        } else {
            float dx = bx - ax;
            float dy = by - ay;
            int len = Math.round((float)Math.sqrt(dx * dx + dy * dy));
            g.pose().pushPose();
            g.pose().translate((float)ax, (float)ay, 0.0F);
            Mc.mulPose(g.pose(), Axes.ZP.rotationDegrees((float)Math.toDegrees(Math.atan2(dy, dx))));
            g.fill(0, -1, len, 1, color);
            g.pose().popPose();
        }
    }

    private void linkStyled(Gfx g, SkillLook l, int ax, int ay, int bx, int by, int color, String style, boolean learned, int nodeSize) {
        EngineConfig.Skin line = learned && l.linkLearned.isSet() ? l.linkLearned : l.linkImage;
        if (!line.isSet()) {
            link(g, ax, ay, bx, by, color, style);
        } else if (style.equals("elbow")) {
            int mid = (ax + bx) / 2;
            this.linkSegment(g, line, ax, ay, mid, ay, l.linkWidth);
            this.linkSegment(g, line, mid, ay, mid, by, l.linkWidth);
            this.linkSegment(g, line, mid, by, bx, by, l.linkWidth);
        } else {
            this.linkSegment(g, line, ax, ay, bx, by, l.linkWidth);
        }

        EngineConfig.Skin arrow = learned && l.arrowLearned.isSet() ? l.arrowLearned : l.arrowImage;
        if (arrow.isSet()) {
            float dx = style.equals("elbow") ? Math.signum((float)(bx - ax)) : bx - ax;
            float dy = style.equals("elbow") ? 0.0F : by - ay;
            if (dx != 0.0F || dy != 0.0F) {
                float angle = (float)Math.atan2(dy, dx);
                int tip = Math.max(6, Math.min(20, l.linkWidth * 4));
                int cx = Math.round(bx - (float)Math.cos(angle) * nodeSize / 2.0F);
                int cy = Math.round(by - (float)Math.sin(angle) * nodeSize / 2.0F);
                g.pose().pushPose();
                g.pose().translate((float)cx, (float)cy, 0.0F);
                Mc.mulPose(g.pose(), Axes.ZP.rotationDegrees((float)Math.toDegrees(angle)));
                Skins.draw(g, this.view.project, arrow, -tip / 2, -tip / 2, tip, tip);
                g.pose().popPose();
            }
        }
    }

    private void linkSegment(Gfx g, EngineConfig.Skin skin, int ax, int ay, int bx, int by, int width) {
        float dx = bx - ax;
        float dy = by - ay;
        int len = Math.round((float)Math.sqrt(dx * dx + dy * dy));
        if (len > 0) {
            g.pose().pushPose();
            g.pose().translate((float)ax, (float)ay, 0.0F);
            Mc.mulPose(g.pose(), Axes.ZP.rotationDegrees((float)Math.toDegrees(Math.atan2(dy, dx))));
            Skins.draw(g, this.view.project, skin, 0, -width / 2, len, width);
            g.pose().popPose();
        }
    }

    private static int blend(int a, int b, float k) {
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        return 0xFF000000 | Math.round(br + (ar - br) * k) << 16 | Math.round(bg + (ag - bg) * k) << 8 | Math.round(bb + (ab - bb) * k);
    }

    private int cardW() {
        String var1 = this.menu().size;

        return switch (var1) {
            case "compact" -> 120;
            case "big" -> 190;
            default -> 150;
        };
    }

    private int cardH() {
        String var1 = this.menu().size;

        return switch (var1) {
            case "compact" -> 30;
            case "big" -> 76;
            default -> 56;
        };
    }

    private int columns() {
        return this.menu().columns > 0 ? this.menu().columns : Math.max(1, (this.bodyW() - 6) / (this.cardW() + 6));
    }

    private int[] card(int i) {
        int cols = this.columns();
        int total = cols * this.cardW() + (cols - 1) * 6;
        int left = this.ax + Math.max(6, (this.bodyW() - total) / 2);
        return new int[]{left + i % cols * (this.cardW() + 6), (int)Math.round(this.bodyTop() + 6 + i / cols * (this.cardH() + 6) - this.scroll)};
    }

    private void grid(Gfx g, Font font, SkillLook l, double mx, double my) {
        List<SkillTree.Node> ns = this.shown();
        int cw = this.cardW();
        int ch = this.cardH();
        boolean compact = this.menu().size.equals("compact");
        int icon = compact ? 18 : (this.menu().size.equals("big") ? 32 : 24);

        for (int i = 0; i < ns.size(); i++) {
            SkillTree.Node n = ns.get(i);
            int[] c = this.card(i);
            boolean hover = mx >= c[0]
                && mx < c[0] + cw
                && my >= c[1]
                && my < c[1] + ch
                && mx >= this.ax
                && mx < this.ax + this.aw
                && my >= this.ay
                && my < this.ay + this.ah;
            if (hover) {
                this.hovered = n;
            }

            this.plate(g, l, n, c[0], c[1], cw, ch, hover);
            int rank = this.state().rank(n.id);
            String why = SkillLogic.refusal(this.tree(), this.state(), n);
            int iy = c[1] + (compact ? (ch - icon) / 2 : 6);
            this.art(g, n.icon, n.image, c[0] + 6, iy, icon);
            if (rank == 0 && why != null) {
                over(g, c[0] + 6, iy, icon, icon, -2146430440);
            }

            int tx = c[0] + icon + 12;
            int tw = cw - icon - 18;
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            g.drawString(font, font.plainSubstrByWidth(n.title, tw), tx, c[1] + (compact ? 6 : 7), rank > 0 ? l.text : (why == null ? l.text : l.muted), false);
            String status = rank >= n.maxRank
                ? "Изучено"
                : (this.tree().levels && this.state().level < n.level ? "С " + n.level + " уровня" : this.priceLine(n));
            int color = rank >= n.maxRank ? this.accent() : (why == null ? l.good : l.muted);
            if (!compact) {
                if (!n.description.isBlank()) {
                    g.drawString(font, font.plainSubstrByWidth(n.description, tw), tx, c[1] + 18, l.muted, false);
                }

                g.drawString(font, font.plainSubstrByWidth(status, tw), tx, c[1] + ch - (n.maxRank > 1 ? 18 : 13), color, false);
                if (n.maxRank > 1) {
                    pips(g, tx, c[1] + ch - 7, Math.min(tw, 60), rank, n.maxRank, this.accent(), l.border);
                }
            } else {
                g.drawString(font, font.plainSubstrByWidth(status, tw), tx, c[1] + 17, color, false);
            }

            g.pose().popPose();
        }
    }

    private String priceLine(SkillTree.Node n) {
        List<String> parts = new ArrayList<>();

        for (SkillTree.Cost c : n.costs) {
            SkillTree.Currency cur = this.tree().currency(c.currency);
            parts.add(SkillLogic.costOf(n, c, this.state().rank(n.id)) + " " + (cur != null ? cur.name : c.currency));
        }

        return parts.isEmpty() ? "Бесплатно" : String.join(", ", parts);
    }

    private void detail(Gfx g, Font font, SkillLook l, double mx, double my) {
        int dx = this.x + this.w - 176;
        int dy = this.y + 34;
        g.fill(dx, dy, this.x + this.w, this.y + this.h, blend(l.header, l.panel, 0.5F));
        g.fill(dx, dy, dx + 1, this.y + this.h, l.border);
        int tx = dx + 8;
        int tw = 160;
        int ty = dy + 8;
        SkillTree.Node n = this.selected != null ? this.tree().node(this.selected) : null;
        if (n == null) {
            this.wrapped(
                g,
                font,
                this.menu().view.equals("fog") ? "Новое откроется, когда изучите то, что уже видно." : "Нажмите на навык, чтобы узнать о нём.",
                tx,
                ty,
                tw,
                l.muted
            );
        } else {
            ty = this.info(g, font, l, n, tx, ty, tw);
            ty += 4;
            int rank = this.state().rank(n.id);
            if (rank < n.maxRank) {
                String why = SkillLogic.refusal(this.tree(), this.state(), n);
                this.learnButton = this.button(g, font, l, "Изучить", tx, ty, tw, why == null, mx, my);
                ty += 22;
            }

            if (n.ability.on && rank > 0) {
                Integer cd = this.view.cooldowns.get(n.id);
                boolean ready = cd == null || cd <= 0;
                this.useButton = this.button(g, font, l, ready ? "Применить" : "Ждать " + (cd + 19) / 20 + " с", tx, ty, tw, ready, mx, my);
            }
        }

        if (this.tree().allowReset && !this.state().ranks.isEmpty()) {
            this.resetButton = this.button(g, font, l, "Сбросить всё", tx, this.y + this.h - 24, tw, true, mx, my);
        }
    }

    private int info(Gfx g, Font font, SkillLook l, SkillTree.Node n, int tx, int ty, int tw) {
        if (g != null) {
            this.art(g, n.icon, n.image, tx, ty, 16);
        }

        int top = ty;

        for (FormattedCharSequence line : font.split(Component.literal(n.title), tw - 20)) {
            if (g != null) {
                g.drawString(font, line, tx + 20, ty + 4, l.text, false);
            }

            ty += 10;
        }

        ty = Math.max(ty + 4, top + 20);
        int rank = this.state().rank(n.id);
        if (n.maxRank > 1) {
            if (g != null) {
                g.drawString(font, "Ранг " + rank + " из " + n.maxRank, tx, ty, this.accent(), false);
            }

            ty += 11;
        }

        ty = this.wrapped(g, font, n.description, tx, ty, tw, l.muted);
        if (!n.description.isBlank()) {
            ty += 3;
        }

        for (SkillTree.Effect e : n.effects) {
            if (!e.kind.equals("command")) {
                ty = this.wrapped(g, font, "• " + describe(e), tx, ty, tw, l.good);
            }
        }

        if (n.ability.on) {
            ty = this.wrapped(
                g,
                font,
                "• Клавиша " + n.ability.key.toUpperCase() + (n.ability.cooldown > 0.0F ? ", перезарядка " + fmt(n.ability.cooldown) + " с" : ""),
                tx,
                ty,
                tw,
                l.good
            );
        }

        ty += 3;
        if (rank < n.maxRank) {
            ty = this.wrapped(g, font, "Цена: " + this.priceLine(n), tx, ty, tw, l.text);
            String why = SkillLogic.refusal(this.tree(), this.state(), n);
            if (this.tree().levels && n.level > 1) {
                ty = this.wrapped(g, font, "Нужен уровень: " + n.level, tx, ty, tw, this.state().level >= n.level ? l.text : l.bad);
            }

            if (why != null && !why.startsWith("Нужен уровень")) {
                ty = this.wrapped(g, font, why, tx, ty, tw, l.bad);
            }
        } else {
            ty = this.wrapped(g, font, "Изучено", tx, ty, tw, this.accent());
        }

        return ty;
    }

    private void tooltip(Gfx g, Font font, SkillLook l, SkillTree.Node n, double mx, double my) {
        int tw = 170;
        boolean canNow = this.state().rank(n.id) < n.maxRank && SkillLogic.refusal(this.tree(), this.state(), n) == null;
        int th = this.info(null, font, l, n, 0, 0, tw) + (canNow ? 24 : 12);
        int bx = (int)Math.min(mx + 12.0, (double)(this.x + this.w - tw - 18));
        int by = (int)Math.min(my + 8.0, (double)(this.y + this.h - th - 4));
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 400.0F);
        g.fill(bx - 1, by - 1, bx + tw + 17, by + th + 1, this.accent());
        imageOr(g, this.view.project, l.tooltipImage, l.header, bx, by, tw + 16, th);
        int end = this.info(g, font, l, n, bx + 8, by + 6, tw);
        if (canNow) {
            g.drawString(font, "Нажмите, чтобы изучить", bx + 8, end + 2, l.good, false);
        }

        g.pose().popPose();
    }

    private int wrapped(Gfx g, Font font, String text, int tx, int ty, int tw, int color) {
        if (text != null && !text.isBlank()) {
            for (FormattedCharSequence line : font.split(Component.literal(text), tw)) {
                if (g != null) {
                    g.drawString(font, line, tx, ty, color, false);
                }

                ty += 10;
            }

            return ty;
        } else {
            return ty;
        }
    }

    private int[] button(Gfx g, Font font, SkillLook l, String text, int bx, int by, int bw, boolean on, double mx, double my) {
        boolean hover = on && mx >= bx && mx < bx + bw && my >= by && my < by + 18;
        if (l.buttonImage.isSet()) {
            Skins.draw(g, this.view.project, l.buttonImage, bx, by, bw, 18);
        } else {
            g.fill(bx, by, bx + bw, by + 18, !on ? l.tile : (hover ? blend(-1, this.accent(), 0.2F) : this.accent()));
        }

        g.drawString(font, text, bx + (bw - font.width(text)) / 2, by + 5, on ? -1 : l.muted, false);
        return on ? new int[]{bx, by, bw, 18} : null;
    }

    private static String fmt(double v) {
        return v == Math.rint(v) ? String.valueOf((long)v) : String.valueOf(Math.round(v * 100.0) / 100.0);
    }

    public static String describe(SkillTree.Effect e) {
        String var1 = e.kind;

        return switch (var1) {
            case "stat" -> {
                PlayerRuleDefs.Rule r = PlayerRuleDefs.get(e.rule);
                yield (r != null ? r.title() : e.rule) + (e.amount >= 0.0F ? " +" : " ") + fmt(e.amount);
            }
            case "unlock" -> {
                PlayerRuleDefs.Rule r = PlayerRuleDefs.get(e.rule);
                yield "Открывает: " + (r != null ? r.title() : e.rule);
            }
            case "item" -> "Открывает предмет: " + stack(e.item).getHoverName().getString();
            case "potion" -> {
                ResourceLocation rl = ResourceLocation.tryParse(e.potion);
                MobEffect m = rl != null ? Regs.MOB_EFFECT.get(rl) : null;
                yield "Постоянно: " + (m != null ? m.getDisplayName().getString() : e.potion) + (e.level > 1 ? " " + e.level : "");
            }
            default -> "Особое действие";
        };
    }

    private static boolean in(int[] r, double mx, double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    public String click(double mx, double my) {
        if (in(this.learnButton, mx, my)) {
            return "learn";
        } else if (in(this.useButton, mx, my)) {
            return "use";
        } else if (in(this.resetButton, mx, my)) {
            return "reset";
        } else {
            for (int i = 0; i < this.tabRects.size() && i < this.tabs.size(); i++) {
                if (in(this.tabRects.get(i), mx, my)) {
                    this.section = this.tabs.get(i);
                    this.fitted = false;
                    this.scroll = 0.0;
                    return null;
                }
            }

            if (!(mx < this.ax) && !(mx >= this.ax + this.aw) && !(my < this.ay) && !(my >= this.ay + this.ah)) {
                SkillTree.Node n = this.at(mx, my);
                if (n == null) {
                    return null;
                } else {
                    this.selected = n.id;
                    if (this.side()) {
                        return null;
                    } else if (this.state().rank(n.id) < n.maxRank) {
                        return SkillLogic.refusal(this.tree(), this.state(), n) == null ? "learn" : null;
                    } else {
                        return n.ability.on ? "use" : null;
                    }
                }
            } else {
                return null;
            }
        }
    }

    private SkillTree.Node at(double mx, double my) {
        List<SkillTree.Node> ns = this.shown();
        if (this.menu().view.equals("grid")) {
            for (int i = 0; i < ns.size(); i++) {
                int[] c = this.card(i);
                if (mx >= c[0] && mx < c[0] + this.cardW() && my >= c[1] && my < c[1] + this.cardH()) {
                    return ns.get(i);
                }
            }

            return null;
        } else {
            int size = this.nodeSize();
            double wx = (mx - this.ax - this.panX) / this.zoom;
            double wy = (my - this.ay - this.panY) / this.zoom;

            for (SkillTree.Node n : ns) {
                if (SkillLogic.visible(this.menu().view, this.state(), n)) {
                    int nx = this.nodeX(n);
                    int ny = this.nodeY(n);
                    if (wx >= nx && wx < nx + size && wy >= ny && wy < ny + size) {
                        return n;
                    }
                }
            }

            return null;
        }
    }

    public void drag(double dx, double dy) {
        if (this.menu().view.equals("grid")) {
            this.scrollBy(-dy);
        } else {
            this.panX = this.panX + (int)Math.round(dx);
            this.panY = this.panY + (int)Math.round(dy);
        }
    }

    public void scroll(double delta, double mx, double my) {
        if (this.menu().view.equals("grid")) {
            this.scrollBy(-delta * 20.0);
        } else if (this.menu().userZoom) {
            float next = Math.max(this.menu().zoomMin, Math.min(this.menu().zoomMax, this.zoom * (delta > 0.0 ? 1.12F : 0.89285713F)));
            double wx = (mx - this.ax - this.panX) / this.zoom;
            double wy = (my - this.ay - this.panY) / this.zoom;
            this.panX = (int)Math.round(mx - this.ax - wx * next);
            this.panY = (int)Math.round(my - this.ay - wy * next);
            this.zoom = next;
            this.zoomedByHand = true;
        } else {
            this.panY = this.panY + (int)Math.round(delta * 20.0);
        }
    }

    private void scrollBy(double d) {
        int rows = (this.shown().size() + this.columns() - 1) / this.columns();
        double most = Math.max(0, rows * (this.cardH() + 6) + 6 - this.ah);
        this.scroll = Math.max(0.0, Math.min(most, this.scroll + d));
    }
}
