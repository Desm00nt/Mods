package org.sprautestudio.spraute_engine.skills.client;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.skills.SkillHud;
import org.sprautestudio.spraute_engine.skills.SkillLogic;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class SkillsClient {
    private static final Gson GSON = new Gson();
    private static final Map<String, Skills.View> VIEWS = new LinkedHashMap<>();
    private static Object connection;
    private static Skills.View sampleView;

    private SkillsClient() {
    }

    public static void registerHooks() {
        ClientHooks.RENDER_GUI.add(e -> hud(e.getGuiGraphics()));
    }

    private static void sameWorld() {
        Object now = Minecraft.getInstance().getConnection();
        if (now != connection) {
            connection = now;
            VIEWS.clear();
        }
    }

    public static void receive(String json, int mode) {
        Minecraft mc = Minecraft.getInstance();
        sameWorld();
        if (mode == 2) {
            if (mc.screen instanceof SkillScreen) {
                mc.setScreen(null);
            }
        } else {
            Skills.View v = GSON.fromJson(json, Skills.View.class);
            if (v != null) {
                if (v.tree == null) {
                    Skills.View known = VIEWS.get(v.key);
                    if (known == null) {
                        return;
                    }

                    v.tree = known.tree;
                } else {
                    v.tree.fix();
                }

                VIEWS.put(v.key, v);
                if (mc.screen instanceof SkillScreen s && s.panel.view.key.equals(v.key)) {
                    if (mode != 1) {
                        v.menu = s.panel.view.menu;
                    }

                    s.panel.setView(v);
                } else if (mode == 1) {
                    mc.setScreen(new SkillScreen(v));
                }
            }
        }
    }

    public static String value(String project, String system, String field, String arg) {
        sameWorld();
        Skills.View found = null;

        for (Skills.View v : VIEWS.values()) {
            if ((system == null || v.name.equals(system))
                && (system != null || found == null || !found.project.equals(project))
                && (found == null || v.project.equals(project))) {
                found = v;
            }
        }

        if (found == null) {
            return null;
        } else {
            SkillTree t = found.tree;
            SkillLogic.State s = found.state;

            return switch (field) {
                case "level" -> String.valueOf(s.level);
                case "nextlevel" -> String.valueOf(Math.min(s.level + 1, t.maxLevel));
                case "xp" -> String.valueOf(s.xp);
                case "xpnext" -> String.valueOf(SkillLogic.xpToNext(t, s.level));
                case "maxlevel" -> String.valueOf(t.maxLevel);
                case "title" -> t.title;
                case "points" -> String.valueOf(s.points(arg != null ? arg : t.currencies.get(0).id));
                case "rank" -> String.valueOf(arg != null ? s.rank(arg) : 0);
                default -> null;
            };
        }
    }

    private static void hud(Gfx g) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui && mc.screen == null && mc.player != null) {
            sameWorld();
            if (!VIEWS.isEmpty()) {
                SkillHud h = EngineClient.config().skillHud;
                GuiView layout = EngineClient.hudLayout();
                int sw = g.guiWidth();
                int sh = g.guiHeight();
                if (h.level.visible && !onCanvas(layout, "hud_skill_level")) {
                    int rowH = h.level.height + (h.level.text ? 10 : 0) + h.level.gap;
                    int[] r = SkillHud.place(
                        h.level.anchor, h.level.x, h.level.y, h.level.width, Math.max(0, levelCount(VIEWS.values()) * rowH - h.level.gap), sw, sh
                    );
                    levels(g, mc.font, VIEWS.values(), r, h.level.height, h.level.gap, h.level.text, h.level.back, 0, h.level.textColor, h.level.direction);
                }

                if (h.abilities.visible && !onCanvas(layout, "hud_skill_abilities")) {
                    SkillsClient.Icons style = new SkillsClient.Icons(
                        h.abilities.size,
                        h.abilities.gap,
                        h.abilities.pad,
                        0,
                        h.abilities.vertical,
                        h.abilities.keys,
                        h.abilities.seconds,
                        h.abilities.plate,
                        h.abilities.plateImage,
                        h.abilities.frameImage,
                        h.abilities.cooldownImage,
                        h.abilities.keyColor,
                        h.abilities.textColor,
                        h.abilities.cooldown
                    );
                    List<SkillsClient.Slot> slots = slots(VIEWS.values());
                    int run = slots.size() * (h.abilities.size + h.abilities.gap) - h.abilities.gap;
                    int[] r = SkillHud.place(
                        h.abilities.anchor,
                        h.abilities.x,
                        h.abilities.y,
                        h.abilities.vertical ? h.abilities.size : run,
                        h.abilities.vertical ? run : h.abilities.size,
                        sw,
                        sh
                    );
                    abilities(g, mc.font, slots, r, style);
                }
            }
        }
    }

    private static boolean onCanvas(GuiView layout, String type) {
        if (layout == null) {
            return false;
        } else {
            for (GuiDoc.Element e : layout.doc.all()) {
                if (type.equals(e.type)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static void part(Canvas c, GuiDoc.Element e, int[] r) {
        Minecraft mc = Minecraft.getInstance();
        Collection<Skills.View> views = VIEWS.values();
        if (mc.screen instanceof SprauteScreen) {
            views = List.of(sample());
        } else {
            if (mc.screen != null) {
                return;
            }

            SkillHud h = EngineClient.config().skillHud;
            if (e.type.equals("hud_skill_abilities") ? !h.abilities.visible : !h.level.visible) {
                return;
            }
        }

        c.flush();
        if (e.type.equals("hud_skill_abilities")) {
            boolean vertical = e.b("vertical");
            abilities(
                c.g,
                mc.font,
                slots(views),
                r,
                new SkillsClient.Icons(
                    vertical ? r[2] : r[3],
                    e.i("gap"),
                    e.i("pad"),
                    e.i("itemSize"),
                    vertical,
                    e.b("keys"),
                    e.b("seconds"),
                    e.color("plate"),
                    skin(e, "plateImage"),
                    skin(e, "frameImage"),
                    skin(e, "cooldownImage"),
                    e.color("keyColor"),
                    e.color("textColor"),
                    e.color("cooldown")
                )
            );
        } else {
            levels(c.g, mc.font, views, r, r[3], e.i("gap"), e.b("showText"), e.color("background"), e.color("color"), e.color("textColor"), e.get("direction"));
        }

        c.flush();
    }

    private static Skills.View sample() {
        if (sampleView == null) {
            Skills.View v = new Skills.View();
            v.key = "sample";
            v.project = "";
            v.name = "sample";
            v.tree = SkillTree.preset("star");
            v.state = new SkillLogic.State();
            v.state.level = 6;

            for (SkillTree.Node n : v.tree.nodes) {
                if (n.ability.on) {
                    v.state.ranks.put(n.id, 1);
                }
            }

            sampleView = v;
        }

        return sampleView;
    }

    private static EngineConfig.Skin skin(GuiDoc.Element e, String key) {
        EngineConfig.Skin s = new EngineConfig.Skin();
        s.image = e.get(key);
        s.nine = e.b("nineSlice");
        s.left = e.i("sliceLeft");
        s.top = e.i("sliceTop");
        s.right = e.i("sliceRight");
        s.bottom = e.i("sliceBottom");
        s.scale = e.f("sliceScale");
        return s;
    }

    private static int levelCount(Collection<Skills.View> views) {
        int n = 0;

        for (Skills.View v : views) {
            if (v.tree.hud && v.tree.levels && !v.state.off) {
                n++;
            }
        }

        return n;
    }

    private static void levels(
        Gfx g, Font font, Collection<Skills.View> views, int[] r, int height, int gap, boolean text, int back, int fill, int textColor, String direction
    ) {
        int y = r[1];

        for (Skills.View v : views) {
            if (v.tree.hud && v.tree.levels && !v.state.off) {
                if (text) {
                    g.drawString(font, v.tree.title + " · " + v.state.level, r[0], y, textColor, true);
                    y += 10;
                }

                g.fill(r[0], y, r[0] + r[2], y + height, back);
                if (v.state.level < v.tree.maxLevel) {
                    long need = SkillLogic.xpToNext(v.tree, v.state.level);
                    int color = fill >>> 24 != 0 ? fill : 0xFF000000 | v.tree.accent;
                    int[] part = SkillPanel.fillRect(new int[]{r[0], y, r[2], height}, Math.min(1.0, (double)v.state.xp / Math.max(1L, need)), direction);
                    g.fill(part[0], part[1], part[0] + part[2], part[1] + part[3], color);
                }

                y += height + gap;
            }
        }
    }

    private static List<SkillsClient.Slot> slots(Collection<Skills.View> views) {
        List<SkillsClient.Slot> out = new ArrayList<>();

        for (Skills.View v : views) {
            if (v.tree.hudAbilities && !v.state.off) {
                for (SkillTree.Node n : v.tree.nodes) {
                    if (n.ability.on && v.state.rank(n.id) > 0) {
                        out.add(new SkillsClient.Slot(v, n));
                    }
                }
            }
        }

        return out;
    }

    private static void abilities(Gfx g, Font font, List<SkillsClient.Slot> slots, int[] r, SkillsClient.Icons s) {
        int step = s.size + s.gap;

        for (int i = 0; i < slots.size(); i++) {
            SkillsClient.Slot slot = slots.get(i);
            SkillTree.Node n = slot.node;
            int bx = s.vertical ? r[0] : r[0] + i * step;
            int by = s.vertical ? r[1] + i * step : r[1];
            SkillPanel.imageOr(g, slot.view.project, s.plateImage, s.plate, bx, by, s.size, s.size);
            int iconSize = s.itemSize > 0 ? s.itemSize : Math.max(1, s.size - s.pad * 2);
            SkillPanel.art(g, slot.view.project, n.icon, n.image, bx + (s.size - iconSize) / 2, by + (s.size - iconSize) / 2, iconSize);
            Integer cd = slot.view.cooldowns.get(n.id);
            g.pose().pushPose();
            g.pose().translate(0.0F, 0.0F, 300.0F);
            if (s.frameImage != null && s.frameImage.isSet()) {
                SkillPanel.imageOr(g, slot.view.project, s.frameImage, 0, bx, by, s.size, s.size);
            }

            if (cd != null && cd > 0) {
                SkillPanel.imageOr(g, slot.view.project, s.cooldownImage, s.cooldown, bx, by, s.size, s.size);
                if (s.seconds) {
                    String left = String.valueOf((cd + 19) / 20);
                    g.drawString(font, left, bx + (s.size - font.width(left)) / 2, by + (s.size - 8) / 2, s.textColor, true);
                }
            }

            if (s.keys) {
                String key = n.ability.key.toUpperCase();
                g.drawString(font, key, bx + s.size - font.width(key) - 1, by + s.size - 8, s.keyColor, true);
            }

            g.pose().popPose();
        }
    }

    private record Icons(
        int size,
        int gap,
        int pad,
        int itemSize,
        boolean vertical,
        boolean keys,
        boolean seconds,
        int plate,
        EngineConfig.Skin plateImage,
        EngineConfig.Skin frameImage,
        EngineConfig.Skin cooldownImage,
        int keyColor,
        int textColor,
        int cooldown
    ) {
    }

    private record Slot(Skills.View view, SkillTree.Node node) {
    }
}
