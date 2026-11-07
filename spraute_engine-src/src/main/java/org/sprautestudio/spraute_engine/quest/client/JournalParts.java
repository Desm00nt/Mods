package org.sprautestudio.spraute_engine.quest.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.LiveDraw;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.quest.QuestDef;
import org.sprautestudio.spraute_engine.quest.QuestJournal;
import org.sprautestudio.spraute_engine.ui.Canvas;

public final class JournalParts {
    static final ResourceLocation BUTTON = tex("entry");
    static final ResourceLocation CATEGORIES = tex("icons");
    static final ResourceLocation TASK_LINE = tex("task_strip");
    static final ResourceLocation[] TABS = new ResourceLocation[]{tex("tab_current"), tex("tab_completed"), tex("tab_failed")};
    static final String[] STATES = new String[]{"active", "completed", "failed"};
    static final String[] TAB_NAMES = new String[]{"Текущие", "Выполненные", "Проваленные"};
    private static JournalParts.State sample;

    private JournalParts() {
    }

    private static ResourceLocation tex(String n) {
        return new ResourceLocation("spraute_engine", "textures/gui/journal/" + n + ".png");
    }

    public static JournalParts.State sample() {
        if (sample == null) {
            QuestJournal j = QuestJournal.preview(QuestDef.example("Ворота деревни"), "example/gate");
            String[][] more = new String[][]{
                {"example/bell", "Тринадцатый удар", "active", "Слепая Агата"},
                {"example/well", "Колодец на площади", "active", ""},
                {"example/bread", "Хлеб для пекаря", "completed", "Пекарь Ансельм"},
                {"example/wolf", "Волчий след", "failed", "Охотник"}
            };

            for (String[] m : more) {
                QuestJournal.Entry e = new QuestJournal.Entry();
                e.key = m[0];
                e.title = m[1];
                e.state = m[2];
                e.giver = m[3];
                e.description = "Пример квеста: так он будет выглядеть в журнале.";
                e.reward = "";
                QuestJournal.Task t = new QuestJournal.Task();
                t.id = "t";
                t.text = "Пример задачи";
                t.progress = m[2].equals("completed") ? 1 : 0;
                e.tasks.add(t);
                j.quests.add(e);
            }

            if (!j.quests.isEmpty()) {
                j.tracked = j.quests.get(0).key;
            }

            sample = new JournalParts.State(j);
            sample.preview = true;
        }

        return sample;
    }

    static JournalParts.State state(GuiView v) {
        return v.liveState instanceof JournalParts.State s ? s : sample();
    }

    public static void vars(GuiView v) {
        JournalParts.State s = state(v);
        long n = s.journal.quests.stream().filter(ex -> ex.state.equals(STATES[s.tab])).count();
        v.vars.put("tab", TAB_NAMES[s.tab]);
        v.vars.put("count", String.valueOf(n));
        QuestJournal.Entry e = s.current();
        v.vars.put("quest", e != null ? e.title : "");
        v.vars.put("giver", e != null ? e.giver : "");
        v.vars.put("state", e == null ? "" : (e.state.equals("completed") ? "Выполнен" : (e.state.equals("failed") ? "Провален" : "")));
        v.vars.put("close", QuestClient.JOURNAL.getTranslatedKeyMessage().getString() + " — закрыть");
    }

    private static EngineConfig.Quests st() {
        return EngineClient.config().quests;
    }

    public static void draw(GuiView v, Canvas c, GuiDoc.Element e, int[] r, float op) {
        JournalParts.State s = state(v);
        c.flush();
        Gfx g = c.g;
        Font font = c.font;
        float mx = v.mouseDX();
        float my = v.mouseDY();
        String var10 = e.type;
        switch (var10) {
            case "journal_tabs":
                tabs(v, g, s, e, r, op, mx, my);
                break;
            case "journal_list":
                list(v, g, font, s, e, r, op, mx, my);
                break;
            case "journal_title":
                title(g, font, s, e, r, op, mx, my);
                break;
            case "journal_page":
                page(v, g, font, s, e, r, op);
                break;
            case "journal_state":
                QuestJournal.Entry q = s.current();
                if (q == null || q.state.equals("active")) {
                    return;
                }

                boolean done = q.state.equals("completed");
                String t = done ? "Выполнен" : "Провален";
                float sc = Math.max(0.3F, e.f("textScale"));
                LiveDraw.text(g, font, t, r[0] + r[2] - font.width(t) * sc, (float)r[1], sc, LiveDraw.a(e.color(done ? "doneColor" : "failColor"), op), true);
        }

        c.flush();
    }

    private static void tabs(GuiView v, Gfx g, JournalParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        s.tabRects.clear();
        EngineConfig.Quests st = st();
        int gap = e.i("gap");
        int size = Math.max(4, Math.min(r[3], (r[2] - 2 * gap) / 3));

        for (int i = 0; i < 3; i++) {
            int[] tr = new int[]{r[0] + i * (size + gap), r[1], size, size};
            s.tabRects.add(tr);
            int vv = s.tab == i ? 46 : (LiveDraw.in(tr, mx, my) ? 23 : 0);
            EngineConfig.Skin skin = i == 0 ? st.tabCurrent : (i == 1 ? st.tabDone : st.tabFailed);
            String prop = i == 0 ? "current" : (i == 1 ? "done" : "failed");
            LiveDraw.sheet(g, LiveDraw.tex(v.project, e, prop, skin, TABS[i]), tr[0], tr[1], size, size, 0, vv, 23, 23, 23, 69, op);
        }
    }

    private static void list(GuiView v, Gfx g, Font font, JournalParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        s.listRect = (int[])r.clone();
        s.entryRects.clear();
        s.entryKeys.clear();
        EngineConfig.Quests st = st();
        List<QuestJournal.Entry> ls = s.list();
        int eh = Math.max(12, e.i("entryHeight"));
        int gap = e.i("gap");
        int pad = e.i("padding");
        int ew = r[2] - pad * 2;
        s.maxList = Math.max(0, ls.size() * (eh + gap) + pad * 2 - r[3]);
        s.listScroll = Math.max(0.0, Math.min(s.maxList, s.listScroll));
        int text = e.color("textColor");
        int muted = e.color("mutedColor");
        int accent = e.color("accentColor");
        LiveDraw.clip(g, r[0], r[1], r[2], r[3]);
        boolean inList = LiveDraw.in(r, mx, my);
        QuestJournal.Entry cur = s.current();

        for (int i = 0; i < ls.size(); i++) {
            QuestJournal.Entry q = ls.get(i);
            int[] er = new int[]{r[0] + pad, (int)Math.round(r[1] + pad + i * (eh + gap) - s.listScroll), ew, eh};
            s.entryRects.add(er);
            s.entryKeys.add(q.key);
            if (er[1] + eh >= r[1] && er[1] <= r[1] + r[3]) {
                boolean sel = q == cur;
                boolean hov = inList && LiveDraw.in(er, mx, my);
                int vv = sel ? 70 : (hov ? 35 : 0);
                String prop = sel ? "buttonOn" : (hov ? "buttonHover" : "entry");
                EngineConfig.Skin skin = sel ? st.buttonOn : (hov ? st.buttonHover : st.button);
                if (e.get(prop).isEmpty() && !skin.isSet() && (sel || hov)) {
                    if (!e.get("entry").isEmpty()) {
                        prop = "entry";
                    } else {
                        skin = st.button;
                    }
                }

                ResourceLocation rl = LiveDraw.tex(v.project, e, prop, skin, BUTTON);
                if (rl == BUTTON) {
                    LiveDraw.sheet(g, rl, er[0], er[1], ew, eh, 0, vv, 131, 35, 131, 105, op);
                } else {
                    LiveDraw.image(g, e, rl, er[0], er[1], ew, eh, op);
                }

                int icon = Math.round(eh * 0.62F);
                if (e.b("icons")) {
                    LiveDraw.sheet(
                        g,
                        LiveDraw.tex(v.project, e, "iconSheet", st.icons, CATEGORIES),
                        er[0] + 5,
                        er[1] + (eh - icon) / 2,
                        Math.round(icon * 21 / 23.0F),
                        icon,
                        s.tab * 21,
                        hov ? 23 : 0,
                        21,
                        23,
                        63,
                        46,
                        op
                    );
                }

                int tx = er[0] + (e.b("icons") ? 30 : 8);
                int tw = er[0] + ew - 5 - tx;
                String t = font.plainSubstrByWidth(q.title, (int)(tw / 0.75F));
                boolean tracked = q.key.equals(s.journal.tracked);
                LiveDraw.text(g, font, t, (float)tx, er[1] + eh / 2.0F - 9.5F, 0.75F, LiveDraw.a(tracked ? accent : text, op), true);
                String sub;
                if (q.state.equals("active")) {
                    int done = 0;
                    int all = 0;

                    for (QuestJournal.Task x : q.tasks) {
                        if (!x.optional) {
                            all++;
                            if (x.done()) {
                                done++;
                            }
                        }
                    }

                    sub = (q.giver.isEmpty() ? "" : q.giver + " · ") + done + "/" + all;
                } else {
                    sub = q.giver;
                }

                LiveDraw.text(g, font, font.plainSubstrByWidth(sub, (int)(tw / 0.6F)), (float)tx, er[1] + eh / 2.0F + 1.5F, 0.6F, LiveDraw.a(muted, op), false);
            }
        }

        g.disableScissor();
        if (ls.isEmpty()) {
            LiveDraw.text(
                g,
                font,
                s.tab == 0 ? "Нет текущих квестов" : (s.tab == 1 ? "Пока ничего не выполнено" : "Ничего не провалено"),
                (float)(r[0] + 8),
                (float)(r[1] + 9),
                0.75F,
                LiveDraw.a(muted, op),
                false
            );
        }

        if (s.maxList > 0.0 && e.b("scrollbar")) {
            int barH = Math.max(10, (int)(r[3] * r[3] / (float)(r[3] + s.maxList)));
            int barY = r[1] + (int)((r[3] - barH) * (s.listScroll / s.maxList));
            g.fill(r[0] + r[2] - 2, r[1], r[0] + r[2], r[1] + r[3], LiveDraw.a(822083583, op));
            g.fill(r[0] + r[2] - 2, barY, r[0] + r[2], barY + barH, LiveDraw.a(-1325400065, op));
        }
    }

    private static void title(Gfx g, Font font, JournalParts.State s, GuiDoc.Element e, int[] r, float op, float mx, float my) {
        QuestJournal.Entry q = s.current();
        s.trackRect = null;
        int muted = e.color("mutedColor");
        if (q == null) {
            LiveDraw.text(g, font, "Выберите квест слева", (float)r[0], (float)(r[1] + 8), 0.75F, LiveDraw.a(muted, op), false);
        } else {
            boolean active = q.state.equals("active");
            boolean tracked = q.key.equals(s.journal.tracked);
            int[] tr = new int[]{r[0], r[1] + 8, r[2], 12};
            boolean hov = active && LiveDraw.in(tr, mx, my);
            if (active) {
                s.trackRect = tr;
            }

            if (active && e.b("trackHint")) {
                String hint = tracked ? "★ отслеживается" : "☆ отслеживать";
                if (tracked || hov) {
                    LiveDraw.text(
                        g,
                        font,
                        hint,
                        r[0] + r[2] - font.width(hint) * 0.6F,
                        (float)r[1],
                        0.6F,
                        LiveDraw.a(tracked ? e.color("accentColor") : e.color("hoverColor"), op),
                        false
                    );
                }
            }

            Component title = Component.literal(q.title).withStyle(ChatFormatting.BOLD);
            float ts = Math.min(1.0F, (float)r[2] / Math.max(1, font.width(title)));
            LiveDraw.text(
                g,
                font,
                title,
                (float)r[0],
                (float)(r[1] + 10),
                ts,
                LiveDraw.a(tracked ? e.color("accentColor") : (hov ? e.color("hoverColor") : e.color("textColor")), op),
                true
            );
            if (!q.giver.isEmpty() && e.b("giver")) {
                LiveDraw.text(g, font, "Выдал: " + q.giver, (float)r[0], (float)(r[1] + 22), 0.6F, LiveDraw.a(muted, op), false);
            }
        }
    }

    private static void page(GuiView v, Gfx g, Font font, JournalParts.State s, GuiDoc.Element e, int[] r, float op) {
        s.bodyRect = (int[])r.clone();
        QuestJournal.Entry q = s.current();
        if (q != null) {
            EngineConfig.Quests st = st();
            int lx = r[0] + 4;
            int width = r[2] - 12;
            int text = e.color("textColor");
            int body = e.color("bodyColor");
            int header = e.color("headerColor");
            int accent = e.color("accentColor");
            int done = e.color("doneColor");
            int fail = e.color("failColor");
            float ps = Math.max(0.3F, e.f("textScale"));
            LiveDraw.clip(g, r[0], r[1], r[2], r[3]);
            float sy = (float)(r[1] + 2 - s.textScroll);
            List<QuestDef.Section> parts = q.layout != null ? q.layout : QuestDef.defaultLayout();
            boolean first = true;

            for (QuestDef.Section part : parts) {
                if (!first) {
                    sy += 4.0F;
                }

                first = false;
                String barH = part.kind;
                switch (barH) {
                    case "description":
                        if (!part.header.isEmpty()) {
                            LiveDraw.text(g, font, part.header.toUpperCase(), (float)lx, sy, 0.6F, LiveDraw.a(header, op), false);
                            sy += 8.0F;
                        }

                        sy = paragraph(g, font, q.description, lx, sy, width, ps, LiveDraw.a(body, op));
                        break;
                    case "tasks":
                        LiveDraw.text(g, font, part.header.isEmpty() ? "ЗАДАЧИ" : part.header.toUpperCase(), (float)lx, sy, 0.6F, LiveDraw.a(header, op), false);
                        sy += 8.0F;
                        ResourceLocation line = LiveDraw.tex(v.project, e, "taskLine", st.taskLine, TASK_LINE);

                        for (QuestJournal.Task t : q.tasks) {
                            boolean d = t.done();
                            LiveDraw.image(g, e, line, lx - 3, (int)sy, width + 5, 16, op * (d ? 0.35F : 0.8F));
                            String mark = d ? "✔" : (q.state.equals("failed") ? "✘" : "◇");
                            LiveDraw.text(
                                g, font, mark, (float)(lx + 1), sy + 4.5F, 0.75F, LiveDraw.a(d ? done : (q.state.equals("failed") ? fail : text), op), true
                            );
                            String txt = t.text + (t.optional ? " (необязательно)" : "");
                            String prog = t.count > 1 ? t.progress + "/" + t.count : "";
                            float pw = font.width(prog) * 0.75F;
                            LiveDraw.text(
                                g,
                                font,
                                font.plainSubstrByWidth(txt, (int)((width - 15 - pw) / 0.75F)),
                                (float)(lx + 10),
                                sy + 4.5F,
                                0.75F,
                                LiveDraw.a(d ? -4669236 : text, op),
                                true
                            );
                            if (!prog.isEmpty()) {
                                LiveDraw.text(g, font, prog, lx + width - 2 - pw, sy + 4.5F, 0.75F, LiveDraw.a(d ? done : accent, op), true);
                            }

                            if (d) {
                                g.fill(
                                    lx + 10,
                                    (int)sy + 8,
                                    (int)(lx + 10 + Math.min(width - 15 - pw, font.width(txt) * 0.75F)),
                                    (int)sy + 9,
                                    LiveDraw.a(-1715945268, op)
                                );
                            }

                            sy += 18.0F;
                        }
                        break;
                    case "reward":
                        if (q.reward != null && !q.reward.isEmpty()) {
                            LiveDraw.text(
                                g, font, part.header.isEmpty() ? "НАГРАДА" : part.header.toUpperCase(), (float)lx, sy, 0.6F, LiveDraw.a(accent, op), false
                            );
                            sy += 8.0F;
                            sy = paragraph(g, font, q.reward, lx, sy, width, ps, LiveDraw.a(q.state.equals("completed") ? done : e.color("rewardColor"), op));
                        }
                        break;
                    default:
                        if (!part.header.isEmpty()) {
                            LiveDraw.text(g, font, part.header.toUpperCase(), (float)lx, sy, 0.6F, LiveDraw.a(header, op), false);
                            sy += 8.0F;
                        }

                        int col = body;

                        try {
                            if (!part.color.isEmpty()) {
                                col = 0xFF000000 | Integer.parseInt(part.color.replace("#", ""), 16);
                            }
                        } catch (NumberFormatException var33) {
                        }

                        sy = paragraph(g, font, part.text, lx, sy, width, ps, LiveDraw.a(col, op));
                }
            }

            g.disableScissor();
            s.maxText = Math.max(0.0, sy + s.textScroll - (r[1] + r[3]) + 4.0);
            s.textScroll = Math.min(s.textScroll, s.maxText);
            if (s.maxText > 0.0) {
                float viewH = r[3];
                float all = (float)(viewH + s.maxText);
                int barH = Math.max(12, (int)(viewH * viewH / all));
                int barY = r[1] + (int)((viewH - barH) * (s.textScroll / s.maxText));
                g.fill(r[0] + r[2] - 2, r[1], r[0] + r[2], r[1] + r[3], LiveDraw.a(822083583, op));
                g.fill(r[0] + r[2] - 2, barY, r[0] + r[2], barY + barH, LiveDraw.a(-1325400065, op));
            }
        }
    }

    private static float paragraph(Gfx g, Font font, String s, int lx, float sy, int width, float scale, int color) {
        if (s != null && !s.isEmpty()) {
            for (FormattedCharSequence line : font.split(Component.literal(s), (int)(width / scale))) {
                LiveDraw.text(g, font, line, (float)lx, sy, scale, color, false);
                sy += 10.7F * scale;
            }

            return sy;
        } else {
            return sy;
        }
    }

    public static boolean click(JournalParts.State s, float mx, float my) {
        for (int i = 0; i < s.tabRects.size(); i++) {
            if (LiveDraw.in(s.tabRects.get(i), mx, my)) {
                if (s.tab != i) {
                    s.tab = i;
                    s.selected = null;
                    s.listScroll = 0.0;
                    s.textScroll = 0.0;
                    sound();
                }

                return true;
            }
        }

        if (LiveDraw.in(s.listRect, mx, my)) {
            for (int ix = 0; ix < s.entryRects.size(); ix++) {
                if (LiveDraw.in(s.entryRects.get(ix), mx, my)) {
                    String key = s.entryKeys.get(ix);
                    if (!key.equals(s.selected)) {
                        s.selected = key;
                        s.textScroll = 0.0;
                        sound();
                    }

                    return true;
                }
            }
        }

        QuestJournal.Entry q = s.current();
        if (q != null && q.state.equals("active") && LiveDraw.in(s.trackRect, mx, my)) {
            if (s.preview) {
                s.journal.tracked = s.journal.tracked.equals(q.key) ? "" : q.key;
            } else {
                QuestClient.track(q.key);
            }

            sound();
            return true;
        } else {
            return false;
        }
    }

    public static void scroll(JournalParts.State s, float mx, float my, double delta) {
        if (LiveDraw.in(s.listRect, mx, my)) {
            s.listScroll = Math.max(0.0, Math.min(s.maxList, s.listScroll - delta * 20.0));
        } else {
            s.textScroll = Math.max(0.0, Math.min(s.maxText, s.textScroll - delta * 15.0));
        }
    }

    private static void sound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Mc.sound(SoundEvents.UI_BUTTON_CLICK), 1.0F));
    }

    public static final class State {
        public QuestJournal journal;
        public int tab;
        public String selected;
        public double listScroll;
        public double textScroll;
        public double maxText;
        public double maxList;
        public boolean preview;
        final List<int[]> tabRects = new ArrayList<>();
        final List<int[]> entryRects = new ArrayList<>();
        final List<String> entryKeys = new ArrayList<>();
        int[] listRect;
        int[] bodyRect;
        int[] trackRect;

        public State(QuestJournal j) {
            this.journal = j;
            if (!j.tracked.isEmpty() && j.entry(j.tracked) != null) {
                this.select(j.tracked);
            }
        }

        public void select(String key) {
            QuestJournal.Entry e = this.journal.entry(key);
            if (e != null) {
                this.selected = key;

                for (int i = 0; i < JournalParts.STATES.length; i++) {
                    if (JournalParts.STATES[i].equals(e.state)) {
                        this.tab = i;
                    }
                }

                this.textScroll = 0.0;
            }
        }

        public List<QuestJournal.Entry> list() {
            List<QuestJournal.Entry> out = new ArrayList<>();

            for (QuestJournal.Entry e : this.journal.quests) {
                if (e.state.equals(JournalParts.STATES[this.tab])) {
                    out.add(e);
                }
            }

            return out;
        }

        public QuestJournal.Entry current() {
            QuestJournal.Entry e = this.selected != null ? this.journal.entry(this.selected) : null;
            if (e == null || !e.state.equals(JournalParts.STATES[this.tab])) {
                List<QuestJournal.Entry> ls = this.list();
                e = ls.isEmpty() ? null : ls.get(0);
            }

            return e;
        }
    }
}
