package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.SprauteEngine;

final class SmoothText {
    static final String MINECRAFT = "minecraft";
    private static final int KEEP = 256;
    private static final int LEAST_PX = 6;
    private static final int MOST_PX = 256;
    private static boolean broken;
    private static long names;
    private static final Map<SmoothText.Key, SmoothText.Line> LINES = new LinkedHashMap<>(64, 0.75F, true);
    private static final Map<SmoothText.Style, Font> FONTS = new HashMap<>();
    private static Graphics2D measure;

    private SmoothText() {
    }

    static int px(float size) {
        return Math.max(6, Math.min(256, Math.round(size)));
    }

    static boolean usable(String font) {
        return !broken && font != null && !font.isEmpty() && !font.equals("minecraft");
    }

    private static void fail(Throwable t) {
        broken = true;
        SprauteEngine.LOGGER.warn("Spraute: smooth fonts are not available here, using the game's font", t);
    }

    static String plain(String text) {
        return text.replaceAll("§.", "");
    }

    private static Font font(SmoothText.Style s) {
        return FONTS.computeIfAbsent(s, k -> {
            String family = switch (s.font) {
                case "serif" -> "Serif";
                case "mono" -> "Monospaced";
                default -> "SansSerif";
            };
            Font f = new Font(family, (s.bold ? 1 : 0) | (s.italic ? 2 : 0), s.px);
            Map<TextAttribute, Object> extra = new HashMap<>();
            if (s.underline) {
                extra.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
            }

            if (s.strike) {
                extra.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
            }

            return extra.isEmpty() ? f : f.deriveFont(extra);
        });
    }

    private static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private static FontMetrics metrics(SmoothText.Style s) {
        if (measure == null) {
            measure = new BufferedImage(1, 1, 2).createGraphics();
            smooth(measure);
        }

        return measure.getFontMetrics(font(s));
    }

    static int lineHeight(SmoothText.Style s) {
        try {
            return metrics(s).getHeight();
        } catch (Throwable var2) {
            fail(var2);
            return 0;
        }
    }

    static int width(String text, SmoothText.Style s) {
        try {
            return metrics(s).stringWidth(text);
        } catch (Throwable var3) {
            fail(var3);
            return -1;
        }
    }

    static List<String> wrap(String paragraph, int widthPx, SmoothText.Style s) {
        List<String> out = new ArrayList<>();
        StringBuilder row = new StringBuilder();

        for (String word : paragraph.split(" ", -1)) {
            String next = row.length() == 0 ? word : row + " " + word;
            if (width(next, s) <= widthPx) {
                row.setLength(0);
                row.append(next);
            } else {
                if (row.length() > 0) {
                    out.add(row.toString());
                }

                row.setLength(0);
                String rest = word;

                while (rest.length() > 1 && width(rest, s) > widthPx) {
                    int n = rest.length() - 1;

                    while (n > 1 && width(rest.substring(0, n), s) > widthPx) {
                        n--;
                    }

                    out.add(rest.substring(0, n));
                    rest = rest.substring(n);
                }

                row.append(rest);
            }
        }

        out.add(row.toString());
        return out;
    }

    static SmoothText.Line line(String text, SmoothText.Style s) {
        if (!text.isEmpty() && !broken) {
            SmoothText.Key key = new SmoothText.Key(text, s);
            SmoothText.Line known = LINES.get(key);
            if (known != null) {
                return known;
            } else {
                try {
                    SmoothText.Line made = draw(text, s);
                    LINES.put(key, made);
                    forgetOld();
                    return made;
                } catch (Throwable var5) {
                    fail(var5);
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private static SmoothText.Line draw(String text, SmoothText.Style s) {
        FontMetrics m = metrics(s);
        int pad = Math.max(1, s.px / 8);
        int w = Math.max(1, m.stringWidth(text)) + 2 * pad;
        int h = Math.max(1, m.getHeight()) + 2 * pad;
        BufferedImage picture = new BufferedImage(w, h, 2);
        Graphics2D g = picture.createGraphics();
        smooth(g);
        g.setFont(font(s));
        g.setColor(Color.WHITE);
        g.drawString(text, pad, pad + m.getAscent());
        g.dispose();
        NativeImage image = new NativeImage(w, h, false);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                image.setPixelRGBA(x, y, picture.getRGB(x, y) & 0xFF000000 | 16777215);
            }
        }

        DynamicTexture texture = new DynamicTexture(image);
        ResourceLocation id = new ResourceLocation("spraute_engine", "gui/text/" + names++);
        Minecraft.getInstance().getTextureManager().register(id, texture);
        texture.setFilter(true, false);
        return new SmoothText.Line(id, w, h);
    }

    private static void forgetOld() {
        if (LINES.size() > 256) {
            Iterator<SmoothText.Line> it = LINES.values().iterator();
            SmoothText.Line eldest = it.next();
            it.remove();
            Minecraft.getInstance().getTextureManager().release(eldest.texture());
        }
    }

    private record Key(String text, SmoothText.Style style) {
    }

    record Line(ResourceLocation texture, int w, int h) {
    }

    record Style(String font, boolean bold, boolean italic, boolean underline, boolean strike, int px) {
    }
}
