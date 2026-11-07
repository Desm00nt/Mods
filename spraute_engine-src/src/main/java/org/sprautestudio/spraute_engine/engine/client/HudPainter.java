package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.core.client.SlotOverlay;
import org.sprautestudio.spraute_engine.engine.EngineConfig;

public final class HudPainter {
    public static final ResourceLocation CHAT_BACK = new ResourceLocation("spraute_engine", "textures/gui/log/back.png");
    public static final ResourceLocation CHAT_FRAME = new ResourceLocation("spraute_engine", "textures/gui/log/frame.png");
    private static final ResourceLocation COIN = new ResourceLocation("spraute_engine", "textures/gui/coin.png");
    private static final ResourceLocation ICONS = new ResourceLocation("minecraft", "textures/gui/icons.png");
    private static final ResourceLocation WIDGETS = new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final ResourceLocation WIDE_HOTBAR = new ResourceLocation("spraute_engine", "textures/gui/slots/hotbar.png");
    private static final ResourceLocation WIDE_SELECTION = new ResourceLocation("spraute_engine", "textures/gui/slots/selection.png");

    private HudPainter() {
    }

    public static int[] anchorPoint(String anchor, int sw, int sh) {
        int ax = switch (anchor) {
            case "T", "C", "B" -> sw / 2;
            case "TR", "R", "BR" -> sw;
            default -> 0;
        };

        int ay = switch (anchor) {
            case "L", "C", "R" -> sh / 2;
            case "BL", "B", "BR" -> sh;
            default -> 0;
        };
        return new int[]{ax, ay};
    }

    private static boolean vanillaDrawn(EngineConfig cfg, String key, HudPainter.State s) {
        EngineConfig.Element e = cfg.element(key);
        return key.equals("health") && s.stHealth ? false : e.visible && EngineConfig.gameDrawn(e.style);
    }

    public static int[] defaultRect(EngineConfig cfg, String key, int sw, int sh, HudPainter.State s) {
        int cx = sw / 2;

        return switch (key) {
            case "health" -> s.stHealth ? new int[]{10, sh - 90, 68, 67} : new int[]{cx - 91, sh - 39, 81, 9};
            case "armor" -> new int[]{cx - 91, sh - 39 - (vanillaDrawn(cfg, "health", s) ? 10 : 0), 81, 9};
            case "food" -> new int[]{cx + 10, sh - 39, 81, 9};
            case "air" -> new int[]{cx + 10, sh - 39 - (vanillaDrawn(cfg, "food", s) ? 10 : 0), 81, 9};
            case "exp" -> new int[]{cx - 91, sh - 29, 182, 5};
            case "hotbar" -> new int[]{cx - 91, sh - 22, 182, 22};
            case "coins" -> new int[]{sw - 62, sh - 24, 56, 18};
            case "nav" -> {
                EngineConfig.Nav n = s.nav != null ? s.nav : new EngineConfig.Nav();
                yield new int[]{cx - 80, "bottom".equals(n.place) ? sh - n.offset - 14 : n.offset, 160, 14};
            }
            default -> new int[]{0, 0, 10, 10};
        };
    }

    public static int[] rect(EngineConfig cfg, String key, int sw, int sh, HudPainter.State s) {
        EngineConfig.Element e = cfg.element(key);
        int[] d = defaultRect(cfg, key, sw, sh, s);
        int w = d[2];
        int h = d[3];
        if (!EngineConfig.gameDrawn(e.style)) {
            int[] cs = customSize(e, key);
            w = cs[0];
            h = cs[1];
        }

        if (!e.customPos) {
            int y = d[1] + (d[1] + d[3] > sh * 2 / 3 ? d[3] - h : 0);
            return new int[]{d[0], y, w, h};
        } else {
            int[] a = anchorPoint(e.anchor, sw, sh);
            int[] pv = anchorPoint(e.anchor, w, h);
            float ox = e.percent ? e.px * sw / 100.0F : e.x;
            float oy = e.percent ? e.py * sh / 100.0F : e.y;
            return new int[]{Math.round(a[0] + ox - pv[0]), Math.round(a[1] + oy - pv[1]), w, h};
        }
    }

    public static void place(EngineConfig.Element e, int[] r, int sw, int sh) {
        int[] a = anchorPoint(e.anchor, sw, sh);
        int[] pv = anchorPoint(e.anchor, r[2], r[3]);
        int ox = r[0] + pv[0] - a[0];
        int oy = r[1] + pv[1] - a[1];
        e.x = ox;
        e.y = oy;
        e.px = Math.round(ox * 1000.0F / sw) / 10.0F;
        e.py = Math.round(oy * 1000.0F / sh) / 10.0F;
    }

    public static int[] customSize(EngineConfig.Element e, String key) {
        String var2 = e.style;

        return switch (var2) {
            case "icons" -> {
                int n = Math.max(1, e.count);
                int s = Math.max(2, e.height);
                yield new int[]{n * s + (n - 1) * e.gap, s};
            }
            case "slots" -> {
                int n = 9;
                int s = Math.max(4, e.height);
                yield new int[]{n * s + (n - 1) * e.gap, s};
            }
            case "icon" -> new int[]{Math.max(4, e.height) + 4 + 30, Math.max(4, e.height)};
            case "orb" -> new int[]{Math.max(8, e.height), Math.max(8, e.height) + (e.text ? 10 : 0)};
            default -> new int[]{Math.max(2, e.width), Math.max(2, e.height)};
        };
    }

    public static int[] shift(EngineConfig cfg, String key, int sw, int sh, HudPainter.State s) {
        int[] d = defaultRect(cfg, key, sw, sh, s);
        int[] r = rect(cfg, key, sw, sh, s);
        return new int[]{r[0] - d[0], r[1] - d[1]};
    }

    private static float fraction(String key, HudPainter.State s) {
        return switch (key) {
            case "health" -> s.maxHp <= 0.0F ? 0.0F : s.hp / s.maxHp;
            case "armor" -> s.armor / 20.0F;
            case "food" -> s.food / 20.0F;
            case "air" -> s.maxAir <= 0 ? 0.0F : (float)Math.max(0, s.air) / s.maxAir;
            case "exp" -> s.exp;
            default -> 0.0F;
        };
    }

    private static String label(String key, HudPainter.State s) {
        return switch (key) {
            case "health" -> Math.round(s.hp) + " / " + Math.round(s.maxHp);
            case "armor" -> String.valueOf(s.armor);
            case "food" -> String.valueOf(s.food);
            case "air" -> String.valueOf(Math.max(0, s.air / 15));
            case "exp" -> String.valueOf(s.level);
            default -> "";
        };
    }

    public static void custom(Gfx g, Font font, EngineConfig cfg, String key, int[] r, HudPainter.State s) {
        EngineConfig.Element e = cfg.element(key);
        String p = cfg.project;
        int x = r[0];
        int y = r[1];
        int w = r[2];
        int h = r[3];
        String var12 = e.style;
        switch (var12) {
            case "orb":
                int size = Math.max(8, e.height);
                int fillC = e.fillColor != 0 ? e.fillColor : 0xFF000000 | barColor(key);
                int backC = e.backColor != 0 ? e.backColor : -1072688616;
                orb(g, x, y, size, Math.max(0.0F, Math.min(1.0F, fraction(key, s))), fillC, backC, e.waves);
                if (e.text) {
                    String t = label(key, s);
                    g.drawString(font, t, x + (size - font.width(t)) / 2, y + size + 2, e.textColor, true);
                }
                break;
            case "bar":
                float frac = Math.max(0.0F, Math.min(1.0F, fraction(key, s)));
                Skins.draw(g, p, e.back, x, y, w, h);
                if (!e.back.isSet() && !e.frame.isSet()) {
                    g.fill(x, y, x + w, y + h, e.backColor != 0 ? e.backColor : -2013265920);
                }

                int pd = Math.max(0, Math.min(e.pad, Math.min(w, h) / 2));
                if (e.fill.isSet()) {
                    Skins.drawFill(g, p, e.fill, x + pd, y + pd, w - 2 * pd, h - 2 * pd, frac, e.fillDir);
                } else {
                    int[] part = Skins.filledPart(x + pd, y + pd, w - 2 * pd, h - 2 * pd, frac, e.fillDir);
                    g.fill(part[0], part[1], part[0] + part[2], part[1] + part[3], e.fillColor != 0 ? e.fillColor : barColor(key));
                }

                Skins.draw(g, p, e.frame, x, y, w, h);
                if (e.text) {
                    String t = label(key, s);
                    if (key.equals("exp")) {
                        g.drawString(font, t, x + (w - font.width(t)) / 2, y - 9, e.textColor, true);
                    } else if (h >= 8) {
                        g.drawString(font, t, x + (w - font.width(t)) / 2, y + (h - 8) / 2 + 1, e.textColor, true);
                    }
                }
                break;
            case "icons": {
                int n = Math.max(1, e.count);
                int sizex = h;
                int step = h + e.gap;
                int halves = Math.round(Math.max(0.0F, Math.min(1.0F, fraction(key, s))) * n * 2.0F);

                for (int i = 0; i < n; i++) {
                    int idx = e.reverse ? n - 1 - i : i;
                    int ix = x + idx * step;
                    int v = halves - i * 2;
                    boolean skinned = e.full.isSet() || e.half.isSet() || e.empty.isSet();
                    if (!skinned) {
                        g.fill(ix, y, ix + sizex, y + sizex, e.backColor != 0 ? e.backColor : 1711276032);
                        if (v >= 1) {
                            g.fill(ix + 1, y + 1, ix + (v >= 2 ? sizex - 1 : sizex / 2), y + sizex - 1, e.fillColor != 0 ? e.fillColor : barColor(key));
                        }
                    } else {
                        Skins.draw(g, p, e.empty, ix, y, sizex, sizex);
                        if (v >= 2) {
                            Skins.draw(g, p, e.full, ix, y, sizex, sizex);
                        } else if (v == 1) {
                            if (e.half.isSet()) {
                                Skins.draw(g, p, e.half, ix, y, sizex, sizex);
                            } else {
                                Skins.drawFill(g, p, e.full, ix, y, sizex, sizex, 0.5F);
                            }
                        }
                    }
                }

                if (e.text) {
                    g.drawString(font, label(key, s), x + w + 3, y + (h - 8) / 2 + 1, e.textColor, true);
                }
                break;
            }
            case "slots": {
                int sizex = h;
                int step = h + e.gap;

                for (int ix = 0; ix < 9; ix++) {
                    int sx = x + ix * step;
                    if (ix < s.hotbarSize) {
                        EngineConfig.Skin sk = e.back;
                        if (sk.isSet()) {
                            Skins.draw(g, p, sk, sx, y, sizex, sizex);
                        } else {
                            g.fill(sx, y, sx + sizex, y + sizex, -1442840576);
                            g.renderOutline(sx, y, sizex, sizex, 1728053247);
                        }

                        ItemStack st = s.hotbar[ix];
                        if (st != null && !st.isEmpty()) {
                            int ixx = sx + (sizex - 16) / 2;
                            int iy = y + (sizex - 16) / 2;
                            g.renderItem(st, ixx, iy);
                            g.renderItemDecorations(font, st, ixx, iy);
                        }

                        if (s.locked[ix]) {
                            if (e.fill.isSet()) {
                                Skins.draw(g, p, e.fill, sx, y, sizex, sizex);
                            } else {
                                lock(g, sx, y, sizex);
                            }
                        }
                    }
                }

                int sel = Math.max(0, Math.min(8, s.selected));
                int sx = x + sel * step;
                if (e.frame.isSet()) {
                    Skins.draw(g, p, e.frame, sx - 1, y - 1, sizex + 2, sizex + 2);
                } else {
                    g.renderOutline(sx - 1, y - 1, sizex + 2, sizex + 2, -1);
                }

                if (s.offhand != null && !s.offhand.isEmpty()) {
                    int ox = x - sizex - 6;
                    if (e.back.isSet()) {
                        Skins.draw(g, p, e.back, ox, y, sizex, sizex);
                    } else {
                        g.fill(ox, y, ox + sizex, y + sizex, -1442840576);
                    }

                    g.renderItem(s.offhand, ox + (sizex - 16) / 2, y + (sizex - 16) / 2);
                    g.renderItemDecorations(font, s.offhand, ox + (sizex - 16) / 2, y + (sizex - 16) / 2);
                }
                break;
            }
            case "icon":
                String t = coinsText(s.coins);
                int tw = font.width(t);
                int bw = h + 4 + tw + (e.back.isSet() ? 8 : 0);
                int bx = x + w - bw;
                if (e.back.isSet()) {
                    Skins.draw(g, p, e.back, bx, y - 3, bw, h + 6);
                }

                int pdx = e.back.isSet() ? 4 : 0;
                ItemStack coinItem = EngineClient.coinItem();
                if (e.fill.isSet()) {
                    Skins.draw(g, p, e.fill, bx + pdx, y, h, h);
                } else if (!coinItem.isEmpty()) {
                    g.renderItem(coinItem, bx + pdx + (h - 16) / 2, y + (h - 16) / 2);
                } else {
                    g.fill(bx + pdx + 2, y + 2, bx + pdx + h - 2, y + h - 2, -8396545);
                }

                g.drawString(
                    font,
                    t,
                    bx + pdx + h + 4,
                    y + (h - 8) / 2 + 1,
                    s.coinsNeed >= 0 && s.coins < s.coinsNeed ? EngineClient.config().coinText.lackColor : e.textColor,
                    true
                );
        }
    }

    private static void lock(Gfx g, int x, int y, int size) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, 200.0F);
        SlotOverlay.lock(g, x, y, size);
        g.pose().popPose();
    }

    private static int barColor(String key) {
        return switch (key) {
            case "armor" -> -4668208;
            case "food" -> -3634628;
            case "air" -> -10638858;
            case "exp" -> -8323296;
            default -> -2537400;
        };
    }

    public static void standard(Gfx g, Font font, String key, String style, int[] r, HudPainter.State s) {
        if (key.equals("hotbar") && style.equals("classic")) {
            int x = r[0];
            int y = r[1];
            RenderSystem.enableBlend();
            g.blit(WIDE_HOTBAR, x - 2, y - 4, 0.0F, 0.0F, 186, 26, 186, 26);
            g.blit(WIDE_SELECTION, x - 1 + s.selected * 20 + 2, y - 1, 0.0F, 0.0F, 24, 23, 24, 23);

            for (int i = 0; i < 9; i++) {
                ItemStack st = s.hotbar[i];
                if (i < s.hotbarSize && st != null && !st.isEmpty()) {
                    g.renderItem(st, x + 5 + i * 20, y + 3);
                    g.renderItemDecorations(font, st, x + 5 + i * 20, y + 3);
                }
            }

            for (int ix = 0; ix < 9; ix++) {
                if (ix < s.hotbarSize && s.locked[ix]) {
                    lock(g, x + 5 + ix * 20, y + 3, 16);
                }
            }
        } else {
            standard(g, font, key, r, s);
        }
    }

    public static void standard(Gfx g, Font font, String key, int[] r, HudPainter.State s) {
        RenderSystem.enableBlend();
        int x = r[0];
        int y = r[1];
        switch (key) {
            case "health":
                if (s.stHealth) {
                    vessel(g, font, x, y, s);
                } else {
                    icons(g, x, y, Math.round(s.hp), 16, 52, 61, 0, false);
                }
                break;
            case "nav":
                EngineConfig.Nav n = s.nav != null ? s.nav : new EngineConfig.Nav();
                String label = "К цели" + (n.distance ? " · 12" + n.unit : "");
                int w = font.width(label);
                int cx = x + r[2] / 2;
                if (n.back >>> 24 != 0) {
                    g.fill(cx - w / 2 - 16, y, cx + w / 2 + 6, y + 14, n.back);
                }

                g.drawString(font, n.arrow != null && !n.arrow.isBlank() ? n.arrow : "▲", cx - w / 2 - 12, y + 3, n.arrowColor, false);
                g.drawString(font, label, cx - w / 2 + 2, y + 3, n.textColor, true);
                break;
            case "armor":
                icons(g, x, y, s.armor, 16, 34, 25, 9, false);
                break;
            case "food":
                icons(g, x, y, s.food, 16, 52, 61, 27, true);
                break;
            case "air":
                int bubbles = (int)Math.ceil(Math.max(0, s.air) * 10.0 / Math.max(1, s.maxAir));

                for (int ix = 0; ix < 10; ix++) {
                    if (ix < bubbles) {
                        g.blit(ICONS, x + 72 - ix * 8, y, 16, 18, 9, 9);
                    }
                }
                break;
            case "exp": {
                g.blit(ICONS, x, y, 0, 64, 182, 5);
                int fw = Math.round(182.0F * s.exp);
                if (fw > 0) {
                    g.blit(ICONS, x, y, 0, 69, fw, 5);
                }

                String t = String.valueOf(s.level);
                int tx = x + (182 - font.width(t)) / 2;
                int ty = y - 6;
                g.drawString(font, t, tx + 1, ty, -16777216, false);
                g.drawString(font, t, tx - 1, ty, -16777216, false);
                g.drawString(font, t, tx, ty + 1, -16777216, false);
                g.drawString(font, t, tx, ty - 1, -16777216, false);
                g.drawString(font, t, tx, ty, -8323296, false);
                break;
            }
            case "hotbar":
                int barW = s.hotbarSize * 20 + 1;
                g.blit(WIDGETS, x, y, 0, 0, barW, 22);
                g.blit(WIDGETS, x + barW, y, 181, 0, 1, 22);
                g.blit(WIDGETS, x - 1 + s.selected * 20, y - 1, 0, 22, 24, 24);

                for (int i = 0; i < 9; i++) {
                    ItemStack st = s.hotbar[i];
                    if (i < s.hotbarSize) {
                        if (st != null && !st.isEmpty()) {
                            g.renderItem(st, x + 3 + i * 20, y + 3);
                            g.renderItemDecorations(font, st, x + 3 + i * 20, y + 3);
                        }

                        if (s.locked[i]) {
                            lock(g, x + 3 + i * 20, y + 3, 16);
                        }
                    }
                }
                break;
            case "coins": {
                EngineConfig.CoinText ct = EngineClient.config().coinText;
                String have = coinsText(s.coins);
                ItemStack coinItem = EngineClient.coinItem();
                if (coinItem.isEmpty()) {
                    g.blit(COIN, x + r[2] - 18, y + 1, 0.0F, 0.0F, 16, 16, 16, 16);
                } else {
                    g.renderItem(coinItem, x + r[2] - 18, y + 1);
                }

                int tx = x + r[2] - 22;
                if (s.coinsNeed >= 0) {
                    String need = "/" + shortNumber(s.coinsNeed);
                    tx -= font.width(need);
                    g.drawString(font, need, tx, y + 5, -1, true);
                }

                g.drawString(font, have, tx - font.width(have), y + 5, s.coinsNeed >= 0 && s.coins < s.coinsNeed ? ct.lackColor : ct.color, true);
            }
        }
    }

    static String coinsText(int n) {
        EngineConfig.CoinText ct = EngineClient.config().coinText;
        return (ct.shorten ? shortNumber(n) : String.valueOf(n)) + ct.suffix;
    }

    private static String shortNumber(int n) {
        if (n < 10000) {
            return String.valueOf(n);
        } else {
            return n < 1000000 ? Math.round(n / 100.0F) / 10.0F + "k" : Math.round(n / 100000.0F) / 10.0F + "M";
        }
    }

    public static void orb(Gfx g, int ox, int oy, int size, float share, int fill, int back, boolean waves) {
        float t = (float)System.nanoTime() / 1.0E9F;
        float r = size / 2.0F;
        int behind = fill & 16777215 | Math.min(128, (fill >>> 24) / 2) << 24;
        float k = size / 40.0F;

        for (int cx = 0; cx < size; cx++) {
            float dx = cx + 0.5F - r;
            int half = (int)Math.sqrt(Math.max(0.0F, r * r - dx * dx));
            int top = oy + Math.round(r) - half;
            int bottom = oy + Math.round(r) + half;
            g.fill(ox + cx, top, ox + cx + 1, bottom, back);
            if (!(share <= 0.0F)) {
                float level = oy + size * (1.0F - share);
                float wave = waves ? 1.5F * k : 0.0F;
                int front = Math.round(level + (float)Math.sin(cx * 0.35F / k + t * 3.0F) * wave);
                int rear = Math.round(level + (float)Math.sin(-cx * 0.28F / k + t * 2.2F) * wave);
                if (waves && Math.max(rear, top) < bottom) {
                    g.fill(ox + cx, Math.max(rear, top), ox + cx + 1, bottom, behind);
                }

                if (Math.max(front, top) < bottom) {
                    g.fill(ox + cx, Math.max(front, top), ox + cx + 1, bottom, fill);
                }
            }
        }
    }

    private static void vessel(Gfx g, Font font, int x, int y, HudPainter.State s) {
        int size = 40;
        int ox = x + 14;
        int oy = y + 6;
        float share = s.maxHp <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, s.hp / s.maxHp));
        float t = (float)System.nanoTime() / 1.0E9F;
        float r = size / 2.0F;

        for (int cx = 0; cx < size; cx++) {
            float dx = cx + 0.5F - r;
            int half = (int)Math.sqrt(Math.max(0.0F, r * r - dx * dx));
            int top = oy + Math.round(r) - half;
            int bottom = oy + Math.round(r) + half;
            g.fill(ox + cx, top, ox + cx + 1, bottom, -1072688616);
            float level = oy + size * (1.0F - share);
            int front = Math.round(level + (float)Math.sin(cx * 0.35F + t * 3.0F) * 1.5F);
            int back = Math.round(level + (float)Math.sin(-cx * 0.28F + t * 2.2F) * 1.5F);
            if (share > 0.0F && Math.max(back, top) < bottom) {
                g.fill(ox + cx, Math.max(back, top), ox + cx + 1, bottom, -2132781739);
            }

            if (share > 0.0F && Math.max(front, top) < bottom) {
                g.fill(ox + cx, Math.max(front, top), ox + cx + 1, bottom, -4182225);
            }
        }

        String max = String.valueOf(Math.round(s.maxHp));
        g.drawString(font, max, ox + (size - font.width(max)) / 2, oy + size + 2, -1, true);
    }

    private static void icons(Gfx g, int x, int y, int halves, int uBack, int uFull, int uHalf, int v, boolean fromRight) {
        for (int i = 0; i < 10; i++) {
            int ix = fromRight ? x + 72 - i * 8 : x + i * 8;
            g.blit(ICONS, ix, y, uBack, v, 9, 9);
            if (halves >= i * 2 + 2) {
                g.blit(ICONS, ix, y, uFull, v, 9, 9);
            } else if (halves == i * 2 + 1) {
                g.blit(ICONS, ix, y, uHalf, v, 9, 9);
            }
        }
    }

    public static int[] chatRect(int sw, int sh) {
        float k = 1.1043478F;
        int w = Math.round(sh * k);
        return new int[]{Math.round(sw / 2.0F - sh / 2.0F * k), 0, w, sh};
    }

    public static void chatBack(Gfx g, EngineConfig cfg, int x, int y, int w, int h) {
        if (cfg.chatLog.back.isSet()) {
            Skins.draw(g, cfg.project, cfg.chatLog.back, x, y, w, h);
        } else {
            Skins.draw(g, CHAT_BACK, null, x, y, w, h);
        }
    }

    public static void chatFrame(Gfx g, EngineConfig cfg, int x, int y, int w, int h) {
        if (cfg.chatLog.frame.isSet()) {
            Skins.draw(g, cfg.project, cfg.chatLog.frame, x, y, w, h);
        } else {
            Skins.draw(g, CHAT_FRAME, null, x, y, w, h);
        }
    }

    public static final class State {
        public EngineConfig.Nav nav;
        public float hp = 20.0F;
        public float maxHp = 20.0F;
        public int armor;
        public int food = 20;
        public int air = 300;
        public int maxAir = 300;
        public float exp;
        public int level;
        public int coins;
        public int coinsNeed = -1;
        public int selected;
        public ItemStack[] hotbar = new ItemStack[9];
        public ItemStack offhand = ItemStack.EMPTY;
        public int hotbarSize = 9;
        public boolean[] locked = new boolean[9];
        public boolean stHealth;
        public boolean showAir = true;

        public State() {
            Arrays.fill(this.hotbar, ItemStack.EMPTY);
        }
    }
}
