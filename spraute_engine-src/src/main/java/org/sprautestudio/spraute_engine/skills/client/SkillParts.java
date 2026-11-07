package org.sprautestudio.spraute_engine.skills.client;

import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.skills.SkillLogic;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class SkillParts {
    private static SkillPanel sample;

    private SkillParts() {
    }

    public static SkillPanel sample() {
        if (sample == null) {
            Skills.View v = new Skills.View();
            v.key = "sample";
            v.project = "";
            v.name = "sample";
            v.tree = SkillTree.preset("classic");
            v.state = new SkillLogic.State();
            v.state.level = 6;
            v.state.xp = SkillLogic.xpToNext(v.tree, 6) / 2L;
            v.state.points.put("очки", 3);
            v.state.ranks.put("начало", 1);
            v.state.ranks.put("сила", 2);
            sample = new SkillPanel(v);
            sample.selected = "сила";
        }

        return sample;
    }

    static SkillPanel panel(GuiView v) {
        return v.liveState instanceof SkillPanel p ? p : sample();
    }

    public static void begin(GuiView v) {
        SkillPanel p = panel(v);
        p.beginParts();
        p.customTooltip = false;

        for (GuiDoc.Element e : v.doc.all()) {
            if (e.type.equals("skill_tooltip")) {
                p.customTooltip = true;
            }
        }

        v.vars.put("title", p.titleText());
        v.vars.put("level", String.valueOf(p.view.state.level));
        v.vars.put("nextlevel", String.valueOf(Math.min(p.view.state.level + 1, p.view.tree.maxLevel)));
        v.vars.put("xp", String.valueOf(p.view.state.xp));
        v.vars.put("xpnext", String.valueOf(SkillLogic.xpToNext(p.view.tree, p.view.state.level)));
        v.vars.put("points", String.valueOf(p.view.state.points(p.view.tree.currencies.get(0).id)));
        v.vars.put("node", p.selectedTitle());
    }

    public static void draw(GuiView v, Canvas c, GuiDoc.Element e, int[] r) {
        SkillPanel p = panel(v);
        c.flush();
        float mx = v.mouseDX();
        float my = v.mouseDY();
        String var7 = e.type;
        switch (var7) {
            case "skill_xp":
                p.partXp(c.g, r, e);
                break;
            case "skill_points":
                p.partPoints(c.g, c.font, r, e);
                break;
            case "skill_tabs":
                p.partTabs(c.g, c.font, r, mx, my);
                break;
            case "skill_field":
                p.partField(c.g, c.font, r, mx, my);
                break;
            case "skill_info":
                p.partInfo(c.g, c.font, r);
                break;
            case "skill_tooltip":
                p.partTooltip(c.g, c.font, r, e, mx, my);
                break;
            case "skill_learn":
                p.partLearn(c.g, c.font, r, e, mx, my);
                break;
            case "skill_use":
                p.partUse(c.g, c.font, r, e, mx, my);
                break;
            case "skill_reset":
                p.partReset(c.g, c.font, r, e, mx, my);
        }

        c.flush();
    }
}
