package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.engine.EngineConfig;

public final class ChoiceLayout {
    public static final List<String> PARTS = List.of("answers", "panel", "name", "text", "portrait");
    private static final ResourceLocation SHADOW = st("gui/talk/fade");
    private static final ResourceLocation NAMEPLATE = st("gui/talk/divider");
    private static final ResourceLocation TOP = st("gui/talk/answer_top");
    private static final ResourceLocation MID = st("gui/talk/answer_mid");
    private static final ResourceLocation BOTTOM = st("gui/talk/answer_bottom");
    private static final ResourceLocation TOP_SEL = st("gui/talk/answer_top_hot");
    private static final ResourceLocation MID_SEL = st("gui/talk/answer_mid_hot");
    private static final ResourceLocation BOTTOM_SEL = st("gui/talk/answer_bottom_hot");
    private static final int BTN_W = 166;
    private static final int CAP = 16;
    private static final int TEXT_LEFT = 15;
    private static final int TEXT_RIGHT = 12;
    private static final int GAP = 5;

    private ChoiceLayout() {
    }

    public static String title(String part) {
        return switch (part) {
            case "answers" -> "Ответы";
            case "panel" -> "Панель";
            case "name" -> "Имя";
            case "text" -> "Вопрос";
            default -> "Портрет";
        };
    }

    private static ResourceLocation st(String path) {
        return new ResourceLocation("spraute_engine", "textures/" + path + ".png");
    }

    public static int[] offset(EngineConfig.Choice c, String part) {
        int[] o = c.offsets.get(part);
        return o != null && o.length == 2 ? o : new int[]{0, 0};
    }

    private static int answerW(EngineConfig.Choice c, Font font, String text) {
        if (!c.answerWidth.equals("fit")) {
            return 166;
        } else {
            int tw = (int)Math.ceil(font.width(text) * c.answerTextScale);
            return Math.max(c.answerMinW, Math.min(c.answerMaxW, tw + 15 + 12 + 2));
        }
    }

    private static List<FormattedCharSequence> answerLines(EngineConfig.Choice c, Font font, String text) {
        int bw = answerW(c, font, text);
        return font.split(Component.literal(text), Math.max(10, (int)((bw - 15 - 12) / c.answerTextScale)));
    }

    private static int answerHeight(EngineConfig.Choice c, Font font, String text) {
        return Math.round(answerLines(c, font, text).size() * 9 * c.answerTextScale) - 2 + 32;
    }

    private static void blit3(Gfx g, ResourceLocation tex, int x, int y, int w, int h, int srcH) {
        if (w == 166) {
            g.blit(tex, x, y, w, h, 0.0F, 0.0F, 166, srcH, 166, srcH);
        } else {
            int edge = Math.min(24, w / 2);
            g.blit(tex, x, y, edge, h, 0.0F, 0.0F, edge, srcH, 166, srcH);
            g.blit(tex, x + edge, y, Math.max(0, w - edge * 2), h, edge, 0.0F, 166 - edge * 2, srcH, 166, srcH);
            g.blit(tex, x + w - edge, y, edge, h, 166 - edge, 0.0F, edge, srcH, 166, srcH);
        }
    }

    public static Map<String, int[]> rects(EngineConfig.Choice c, Font font, int w, int h, List<String> answers) {
        Map<String, int[]> r = new LinkedHashMap<>();
        int[] o = offset(c, "panel");
        r.put("panel", new int[]{o[0], h - 270 + o[1], w, 270});
        o = offset(c, "name");
        r.put("name", new int[]{28 + o[0], h - 74 + o[1], 104, 15});
        o = offset(c, "text");
        r.put("text", new int[]{36 + o[0], h - 50 + o[1], Math.max(40, w - 50 - 115), 45});
        o = offset(c, "portrait");
        r.put("portrait", new int[]{Math.round(w * 0.8625F - 49.0F) + o[0], h - 62 - 85 + o[1], 98, 98});
        o = offset(c, "answers");
        int ay = 5 + o[1];
        int total = 0;
        int widest = 166;
        if (c.answerWidth.equals("fit")) {
            widest = 0;
        }

        for (int i = 0; i < answers.size(); i++) {
            int bh = answerHeight(c, font, answers.get(i));
            int bw = answerW(c, font, answers.get(i));
            r.put("answer" + i, new int[]{5 + o[0], ay, bw, bh});
            widest = Math.max(widest, bw);
            ay += bh + 5;
            total += bh + (i > 0 ? 5 : 0);
        }

        r.put("answers", new int[]{5 + o[0], 5 + o[1], Math.max(40, widest), Math.max(32, total)});
        return r;
    }

    public static int textOverflow(EngineConfig.Choice c, Font font, int w, int h, List<String> answers, String text) {
        int[] t = rects(c, font, w, h, answers).get("text");
        return Math.max(0, font.split(Component.literal(text), t[2]).size() * 9 - t[3]);
    }

    public static void draw(Gfx g, Font font, EngineConfig cfg, int w, int h, ChoiceLayout.Content ct) {
        EngineConfig.Choice c = cfg.choice;
        String project = cfg.project;
        Map<String, int[]> r = rects(c, font, w, h, ct.answers());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int[] p = r.get("panel");
        if (c.panel.isSet()) {
            Skins.draw(g, project, c.panel, p[0], p[1], p[2], p[3]);
        } else {
            g.blit(SHADOW, p[0], p[1], p[2], p[3], 0.0F, 0.0F, 480, 270, 480, 270);
        }

        int[] f = r.get("portrait");
        if (c.portrait.isSet()) {
            Skins.draw(g, project, c.portrait, f[0], f[1], f[2], f[3]);
        }

        LivingEntity e = ct.subject();
        if (ct.portrait() != null) {
            RenderSystem.enableBlend();
            g.blit(ct.portrait(), f[0], f[1], 0.0F, 0.0F, f[2], f[3], f[2], f[3]);
        } else if (e != null && !c.show.equals("none")) {
            int cl = Math.max(0, c.cropLeft);
            int cr = Math.max(0, c.cropRight);
            int ctp = Math.max(0, c.cropTop);
            int cb = Math.max(0, c.cropBottom);
            int[] clip = ChatHeads.screenRect(g, f[0] + cl, f[1] + ctp, Math.max(1, f[2] - cl - cr), Math.max(1, f[3] - ctp - cb));
            g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 400.0F);

            try {
                int ex = f[0] + c.entityX;
                int ey = f[1] + c.entityY;
                if (c.framing.equals("head")) {
                    ChatHeads.render(g, e, ex, ey, f[2], f[3], new ChatHeads.Params(c.entityScale, 0, 0, 0, "auto"));
                } else {
                    float eh = Math.max(0.5F, e.getBbHeight());
                    float ew = Math.max(0.5F, e.getBbWidth());
                    int size = (int)Math.max(4.0F, Math.min((f[3] - 14) / eh, (f[2] - 14) / (ew * 1.4F)) * c.entityScale);
                    Mc.inventoryEntity(g, ex + f[2] / 2, ey + f[3] - 8, size, ex + f[2] / 2.0F - ct.mouseX(), ey + f[3] / 3.0F - ct.mouseY(), e);
                }
            } catch (Throwable var33) {
            } finally {
                g.pose().popPose();
                g.disableScissor();
            }
        }

        int[] n = r.get("name");
        if (c.nameLine) {
            if (c.nameplate.isSet()) {
                Skins.draw(g, project, c.nameplate, n[0], n[1] + 12, 103, 3);
            } else {
                g.blit(NAMEPLATE, n[0], n[1] + 12, 103, 3, 0.0F, 0.0F, 80, 3, 80, 3);
            }
        }

        if (!ct.name().isEmpty()) {
            g.drawCenteredString(font, ct.name(), n[0] + 52, n[1] + 2, c.nameColor);
        }

        int[] t = r.get("text");
        String q = ct.question();
        int typed = Math.max(0, Math.min(q.length(), ct.typed()));
        MutableComponent shown = Component.literal(q.substring(0, typed));
        if (typed < q.length()) {
            shown.append(Component.literal("d").withStyle(ChatFormatting.OBFUSCATED));
        }

        List<FormattedCharSequence> lines = font.split(shown, t[2]);
        int most = Math.max(0, lines.size() * 9 - t[3]);
        int scroll = ct.textScroll() < 0 ? most : Math.min(most, ct.textScroll());
        int[] clip = ChatHeads.screenRect(g, t[0], t[1], t[2] + 4, t[3]);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);

        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), t[0], t[1] + 9 * i - scroll, c.textColor, false);
        }

        g.disableScissor();
        if (most > 0) {
            int bar = Math.max(6, t[3] * t[3] / (lines.size() * 9));
            int by = t[1] + (t[3] - bar) * scroll / most;
            g.fill(t[0] + t[2] + 2, by, t[0] + t[2] + 4, by + bar, -1593835521);
        }

        if (ct.selected() >= -1) {
            for (int i = 0; i < ct.answers().size(); i++) {
                int[] b = r.get("answer" + i);
                boolean off = i < ct.locked().size() && ct.locked().get(i);
                boolean hot = i == ct.selected() && !off;
                EngineConfig.Skin skin = hot && c.buttonHover.isSet() ? c.buttonHover : c.button;
                if (skin.isSet()) {
                    Skins.draw(g, project, skin, b[0], b[1], b[2], b[3]);
                } else {
                    int mid = Math.max(0, b[3] - 32);
                    blit3(g, hot ? TOP_SEL : TOP, b[0], b[1], b[2], 16, 16);
                    blit3(g, hot ? MID_SEL : MID, b[0], b[1] + 16, b[2], mid, 8);
                    blit3(g, hot ? BOTTOM_SEL : BOTTOM, b[0], b[1] + 16 + mid, b[2], 16, 16);
                }

                String answerText = ct.answers().get(i);
                List<FormattedCharSequence> al = off && Screen.hasShiftDown()
                    ? font.split(
                        Component.literal(answerText).withStyle(ChatFormatting.STRIKETHROUGH),
                        Math.max(10, (int)((answerW(c, font, answerText) - 15 - 12) / c.answerTextScale))
                    )
                    : answerLines(c, font, answerText);
                float ts = c.answerTextScale;

                for (int k = 0; k < al.size(); k++) {
                    g.pose().pushPose();
                    boolean middle = i < ct.centered().size() && ct.centered().get(i);
                    g.pose().translate(middle ? b[0] + (b[2] - font.width(al.get(k)) * ts) / 2.0F : b[0] + 15, b[1] + 16 - 1 + 9 * k * ts, 0.0F);
                    g.pose().scale(ts, ts, 1.0F);
                    g.drawString(font, al.get(k), 0, 0, off ? -8355712 : (hot ? c.answerHoverColor : c.answerColor), false);
                    g.pose().popPose();
                }
            }
        }
    }

    public static void drawButton(Gfx g, boolean hot, int x, int y, int w, int h) {
        int mid = Math.max(0, h - 32);
        blit3(g, hot ? TOP_SEL : TOP, x, y, w, 16, 16);
        blit3(g, hot ? MID_SEL : MID, x, y + 16, w, mid, 8);
        blit3(g, hot ? BOTTOM_SEL : BOTTOM, x, y + 16 + mid, w, 16, 16);
    }

    public static int answerAt(EngineConfig.Choice c, Font font, int w, int h, List<String> answers, double mx, double my) {
        Map<String, int[]> r = rects(c, font, w, h, answers);

        for (int i = 0; i < answers.size(); i++) {
            int[] b = r.get("answer" + i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                return i;
            }
        }

        return -1;
    }

    public static String partAt(EngineConfig.Choice c, Font font, int w, int h, List<String> answers, double mx, double my) {
        Map<String, int[]> r = rects(c, font, w, h, answers);

        for (String part : new ArrayList<>(List.of("answers", "name", "portrait", "text", "panel"))) {
            int[] b = r.get(part);
            if (part.equals("panel")) {
                if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] + 190 && my < b[1] + b[3]) {
                    return part;
                }
            } else if (mx >= b[0] - 2 && mx < b[0] + b[2] + 2 && my >= b[1] - 2 && my < b[1] + b[3] + 2) {
                return part;
            }
        }

        return null;
    }

    public record Content(
        String name,
        String question,
        int typed,
        List<String> answers,
        int selected,
        LivingEntity subject,
        float mouseX,
        float mouseY,
        List<Boolean> locked,
        int textScroll,
        List<Boolean> centered,
        ResourceLocation portrait
    ) {
        public Content(String name, String question, int typed, List<String> answers, int selected, LivingEntity subject, float mouseX, float mouseY) {
            this(name, question, typed, answers, selected, subject, mouseX, mouseY, List.of(), -1, List.of(), null);
        }
    }
}
