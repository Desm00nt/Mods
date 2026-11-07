package org.sprautestudio.spraute_engine.quest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.quest.QuestDef;
import org.sprautestudio.spraute_engine.quest.QuestJournal;
import org.sprautestudio.spraute_engine.ui.GuiText;

public class QuestScreen extends GfxScreen {
    public static final int W = 392;
    public static final int H = 230;
    private static final ResourceLocation BG = tex("card");
    private static final ResourceLocation BG_2 = tex("list");
    private static final ResourceLocation BUTTON = tex("entry");
    private static final ResourceLocation CATEGORIES = tex("icons");
    private static final ResourceLocation LINE = tex("divider");
    private static final ResourceLocation TASK_LINE = tex("task_strip");
    private static final ResourceLocation[] TABS = new ResourceLocation[]{tex("tab_current"), tex("tab_completed"), tex("tab_failed")};
    private static final String[] STATES = new String[]{"active", "completed", "failed"};
    private static final String[] TAB_NAMES = new String[]{"Текущие", "Выполненные", "Проваленные"};
    private int WHITE = -1;
    private int MUTED = -6642508;
    private int GOLD = -11686;
    private int GREEN = -8658806;
    private int RED = -38037;
    private int BLUE = -9518081;
    public EngineConfig styleFrom;
    private EngineConfig.Quests st;
    private String project = "";
    private QuestJournal journal;
    private int tab;
    private String selected;
    private double listScroll;
    private double textScroll;
    private float scale = 1.0F;
    private int x;
    private int y;
    private final long openedAt = System.currentTimeMillis();
    private boolean hidGui;
    private boolean preview;
    private final JournalParts.State live;
    private static final int LIST_TOP = 31;
    private static final int LIST_H = 195;
    private double maxText;

    private static ResourceLocation tex(String n) {
        return new ResourceLocation("spraute_engine", "textures/gui/journal/" + n + ".png");
    }

    private EngineConfig cfg() {
        return this.styleFrom != null ? this.styleFrom : EngineClient.config();
    }

    private void skin(Gfx g, EngineConfig.Skin s, ResourceLocation def, int x, int y, int w, int h, int u, int v, int tw, int th) {
        if (s != null && s.isSet()) {
            Skins.draw(g, this.project, s, x, y, w, h);
        } else {
            g.blit(def, x, y, u, v, w, h, tw, th);
        }
    }

    private GuiView studioLayout() {
        return this.preview ? null : EngineClient.window("quest");
    }

    public QuestScreen(QuestJournal j) {
        super(Component.literal("Журнал квестов"));
        this.font = Minecraft.getInstance().font;
        this.journal = j;
        this.live = new JournalParts.State(j);
        if (!j.tracked.isEmpty() && j.entry(j.tracked) != null) {
            this.select(j.tracked);
        }
    }

    public void update(QuestJournal j) {
        this.journal = j;
        this.live.journal = j;
        if (this.live.selected != null && j.entry(this.live.selected) == null) {
            this.live.selected = null;
        }

        if (this.selected != null && j.entry(this.selected) == null) {
            this.selected = null;
        }
    }

    private void select(String key) {
        QuestJournal.Entry e = this.journal.entry(key);
        if (e != null) {
            this.selected = key;

            for (int i = 0; i < STATES.length; i++) {
                if (STATES[i].equals(e.state)) {
                    this.tab = i;
                }
            }

            this.textScroll = 0.0;
        }
    }

    private List<QuestJournal.Entry> list() {
        List<QuestJournal.Entry> out = new ArrayList<>();

        for (QuestJournal.Entry e : this.journal.quests) {
            if (e.state.equals(STATES[this.tab])) {
                out.add(e);
            }
        }

        return out;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui) {
            mc.options.hideGui = true;
            this.hidGui = true;
        }
    }

    @Override
    public void removed() {
        if (this.hidGui) {
            Minecraft.getInstance().options.hideGui = false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void paintPreview(Gfx g, float k, double mx, double my) {
        this.preview = true;
        this.scale = k;
        this.x = 0;
        this.y = 0;
        this.width = Math.round(392.0F * k);
        this.height = Math.round(230.0F * k);
        this.draw(g, (int)Math.round(mx), (int)Math.round(my), 0.0F);
    }

    public boolean clickPreview(double mx, double my) {
        this.preview = true;
        return this.mouseClicked(mx, my, 0);
    }

    public boolean scrollPreview(double mx, double my, double delta) {
        return this.mouseScrolled(mx, my, delta);
    }

    public String selected() {
        return this.selected;
    }

    private void layout() {
        if (!this.preview) {
            this.scale = (float)(Math.min((this.width - 16) / 392.0, (this.height - 16) / 230.0) * this.st.scale);
            this.x = Math.round((this.width / this.scale - 392.0F) / 2.0F);
            this.y = Math.round((this.height / this.scale - 230.0F) / 2.0F);
        }
    }

    private int[] tabRect(int i) {
        return new int[]{this.x + 7 + i * 26, this.y + 5, 23, 23};
    }

    private int[] entryRect(int i) {
        return new int[]{this.x + 6, (int)Math.round(this.y + 33 + i * 37 - this.listScroll), 131, 35};
    }

    private int[] trackRect() {
        return new int[]{this.x + 158, this.y + 18, 224, 12};
    }

    private static boolean in(int[] r, double mx, double my) {
        return mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    private void text(Gfx g, String s, float tx, float ty, float sc, int color, boolean shadow) {
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        GuiText.draw(g, this.font, s, tx / sc, ty / sc, color, shadow);
        g.pose().popPose();
    }

    private void text(Gfx g, FormattedCharSequence s, float tx, float ty, float sc, int color) {
        g.pose().pushPose();
        g.pose().scale(sc, sc, 1.0F);
        GuiText.draw(g, this.font, s, tx / sc, ty / sc, color, false);
        g.pose().popPose();
    }

    @Override
    public void draw(Gfx g, int smx, int smy, float pt) {
        GuiView lay = this.studioLayout();
        if (lay != null) {
            g.fill(0, 0, this.width, this.height, GuiDoc.toColor(lay.doc.get("background")));
            lay.liveState = this.live;
            JournalParts.vars(lay);
            EngineClient.drawWindow(g, lay, smx, smy);
        } else {
            EngineConfig cfg = this.cfg();
            this.st = cfg.quests;
            this.project = cfg.project;
            this.WHITE = this.st.textColor;
            this.MUTED = this.st.mutedColor;
            this.GOLD = this.st.accentColor;
            this.GREEN = this.st.doneColor;
            this.RED = this.st.failColor;
            this.BLUE = this.st.headerColor;
            if (!this.preview) {
                g.fill(0, 0, this.width, this.height, this.st.shadeColor);
            }

            this.layout();
            double wmx = smx / this.scale;
            double wmy = smy / this.scale;
            int[] ol = this.st.off("list");
            int[] oc = this.st.off("card");
            double mx = wmx - ol[0];
            double my = wmy - ol[1];
            double cmx = wmx - oc[0];
            double cmy = wmy - oc[1];
            float k = Math.min(1.0F, (float)(System.currentTimeMillis() - this.openedAt) / 400.0F);
            float ease = 1.0F - (1.0F - k) * (1.0F - k);
            g.pose().pushPose();
            g.pose().scale(this.scale, this.scale, 1.0F);
            g.pose().translate(0.0F, -(1.0F - ease) * 30.0F, 0.0F);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ease);
            this.skin(g, this.st.list, BG_2, this.x + ol[0], this.y + ol[1], 144, 230, 0, 0, 144, 230);
            this.skin(g, this.st.card, BG, this.x + 150 + oc[0], this.y + 2 + oc[1], 240, 225, 0, 0, 240, 225);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            g.pose().pushPose();
            g.pose().translate((float)ol[0], (float)ol[1], 0.0F);

            for (int i = 0; i < 3; i++) {
                int[] r = this.tabRect(i);
                int v = this.tab == i ? 46 : (in(r, mx, my) ? 23 : 0);
                EngineConfig.Skin ts = i == 0 ? this.st.tabCurrent : (i == 1 ? this.st.tabDone : this.st.tabFailed);
                Skins.drawSheet(g, this.project, ts, TABS[i], r[0], r[1], 23, 23, 0, v, 23, 69);
            }

            long n = this.journal.quests.stream().filter(ex -> ex.state.equals(STATES[this.tab])).count();
            this.text(g, TAB_NAMES[this.tab] + " · " + n, this.x + 88, this.y + 13, 0.75F, this.MUTED, false);
            List<QuestJournal.Entry> ls = this.list();
            int[] clip = ChatHeads.screenRect(g, this.x + 4, this.y + 31, 136, 195);
            g.enableScissor(clip[0], clip[1], clip[2], clip[3]);

            for (int i = 0; i < ls.size(); i++) {
                QuestJournal.Entry e = ls.get(i);
                int[] r = this.entryRect(i);
                if (r[1] + r[3] >= this.y + 31 && r[1] <= this.y + 31 + 195) {
                    boolean hov = in(r, mx, my) && my >= this.y + 31 && my < this.y + 31 + 195;
                    int v = e.key.equals(this.selected) ? 70 : (hov ? 35 : 0);
                    EngineConfig.Skin bs = e.key.equals(this.selected) ? this.st.buttonOn : (hov ? this.st.buttonHover : this.st.button);
                    if (!bs.isSet() && (e.key.equals(this.selected) || hov)) {
                        bs = this.st.button.isSet() ? this.st.button : bs;
                    }

                    this.skin(g, bs, BUTTON, r[0], r[1], 131, 35, 0, v, 131, 105);
                    Skins.drawSheet(g, this.project, this.st.icons, CATEGORIES, r[0] + 5, r[1] + 5, 21, 23, this.tab * 21, hov ? 23 : 0, 63, 46);
                    String t = this.font.plainSubstrByWidth(e.title, 128);
                    this.text(g, t, r[0] + 30, r[1] + 8, 0.75F, e.key.equals(this.journal.tracked) ? this.GOLD : this.WHITE, true);
                    String sub;
                    if (e.state.equals("active")) {
                        int done = 0;
                        int all = 0;

                        for (QuestJournal.Task x2 : e.tasks) {
                            if (!x2.optional) {
                                all++;
                                if (x2.done()) {
                                    done++;
                                }
                            }
                        }

                        sub = (e.giver.isEmpty() ? "" : e.giver + " · ") + done + "/" + all;
                    } else {
                        sub = e.giver;
                    }

                    this.text(g, this.font.plainSubstrByWidth(sub, 160), r[0] + 30, r[1] + 19, 0.6F, this.MUTED, false);
                }
            }

            g.disableScissor();
            if (ls.isEmpty()) {
                this.text(
                    g,
                    this.tab == 0 ? "Нет текущих квестов" : (this.tab == 1 ? "Пока ничего не выполнено" : "Ничего не провалено"),
                    this.x + 12,
                    this.y + 40,
                    0.75F,
                    this.MUTED,
                    false
                );
            }

            g.pose().popPose();
            g.pose().pushPose();
            g.pose().translate((float)oc[0], (float)oc[1], 0.0F);
            QuestJournal.Entry e = this.selected != null ? this.journal.entry(this.selected) : null;
            if (e == null || !e.state.equals(STATES[this.tab])) {
                e = ls.isEmpty() ? null : ls.get(0);
            }

            if (e != null) {
                this.selected = e.key;
                this.drawQuest(g, e, cmx, cmy);
            } else {
                this.text(g, "Выберите квест слева", this.x + 162, this.y + 24, 0.75F, this.MUTED, false);
            }

            if (!this.preview) {
                this.text(g, QuestClient.JOURNAL.getTranslatedKeyMessage().getString() + " — закрыть", this.x + 155, this.y + 229, 0.5F, -2130706433, false);
            }

            g.pose().popPose();
            g.pose().popPose();
        }
    }

    private void drawQuest(Gfx g, QuestJournal.Entry e, double mx, double my) {
        int lx = this.x + 160;
        int top = this.y + 17;
        boolean tracked = e.key.equals(this.journal.tracked);
        int[] tr = this.trackRect();
        boolean hov = e.state.equals("active") && in(tr, mx, my);
        Component title = Component.literal(e.title).withStyle(ChatFormatting.BOLD);
        float ts = Math.min(1.0F, 220.0F / Math.max(1, this.font.width(title)));
        g.pose().pushPose();
        g.pose().scale(ts, ts, 1.0F);
        g.drawString(this.font, title, (int)(lx / ts), (int)((top + 2) / ts), tracked ? this.GOLD : (hov ? this.BLUE : this.WHITE), true);
        g.pose().popPose();
        if (e.state.equals("active")) {
            this.text(
                g,
                tracked ? "★ отслеживается" : (hov ? "☆ отслеживать" : ""),
                this.x + 382 - this.font.width(tracked ? "★ отслеживается" : "☆ отслеживать") * 0.6F,
                top - 8,
                0.6F,
                tracked ? this.GOLD : this.BLUE,
                false
            );
        }

        float cy = top + 13;
        if (!e.giver.isEmpty()) {
            this.text(g, "Выдал: " + e.giver, lx, cy, 0.6F, this.MUTED, false);
            cy += 7.0F;
        }

        Skins.drawSheet(g, this.project, this.st.divider, LINE, lx, (int)cy + 1, 129, 2, 0, 0, 129, 2);
        cy += 6.0F;
        int clipTop = (int)cy;
        int clipBottom = this.y + 207;
        int[] clip = ChatHeads.screenRect(g, lx - 4, clipTop, 230, clipBottom - clipTop);
        g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
        float sy = (float)(cy - this.textScroll);
        List<QuestDef.Section> parts = e.layout != null ? e.layout : QuestDef.defaultLayout();
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
                        this.text(g, part.header.toUpperCase(), lx, sy, 0.6F, this.BLUE, false);
                        sy += 8.0F;
                    }

                    sy = this.paragraph(g, e.description, lx, sy, -2564376);
                    break;
                case "tasks":
                    sy = this.drawTasks(g, e, part.header.isEmpty() ? "ЗАДАЧИ" : part.header.toUpperCase(), lx, sy);
                    break;
                case "reward":
                    if (e.reward != null && !e.reward.isEmpty()) {
                        this.text(g, part.header.isEmpty() ? "НАГРАДА" : part.header.toUpperCase(), lx, sy, 0.6F, this.GOLD, false);
                        sy += 8.0F;
                        sy = this.paragraph(g, e.reward, lx, sy, e.state.equals("completed") ? this.GREEN : -6237);
                    }
                    break;
                default:
                    if (!part.header.isEmpty()) {
                        this.text(g, part.header.toUpperCase(), lx, sy, 0.6F, this.BLUE, false);
                        sy += 8.0F;
                    }

                    int col = -2564376;

                    try {
                        if (!part.color.isEmpty()) {
                            col = 0xFF000000 | Integer.parseInt(part.color.replace("#", ""), 16);
                        }
                    } catch (NumberFormatException var27) {
                    }

                    sy = this.paragraph(g, part.text, lx, sy, col);
            }
        }

        g.disableScissor();
        this.maxText = Math.max(0.0, sy + this.textScroll - clipBottom + 4.0);
        this.textScroll = Math.min(this.textScroll, this.maxText);
        if (this.maxText > 0.0) {
            float viewH = clipBottom - clipTop;
            float all = (float)(viewH + this.maxText);
            int barH = Math.max(12, (int)(viewH * viewH / all));
            int barY = clipTop + (int)((viewH - barH) * (this.textScroll / this.maxText));
            g.fill(this.x + 384, clipTop, this.x + 386, clipBottom, 822083583);
            g.fill(this.x + 384, barY, this.x + 386, barY + barH, -1325400065);
        }

        String var33 = e.state;

        String stt = switch (var33) {
            case "completed" -> "Выполнен";
            case "failed" -> "Провален";
            default -> "";
        };
        if (!stt.isEmpty()) {
            this.text(g, stt, this.x + 382 - this.font.width(stt) * 0.75F, this.y + 213, 0.75F, e.state.equals("completed") ? this.GREEN : this.RED, true);
        }
    }

    private float paragraph(Gfx g, String s, int lx, float sy, int color) {
        if (s != null && !s.isEmpty()) {
            for (FormattedCharSequence line : this.font.split(Component.literal(s), 314)) {
                this.text(g, line, lx, sy, 0.7F, color);
                sy += 7.5F;
            }

            return sy;
        } else {
            return sy;
        }
    }

    private float drawTasks(Gfx g, QuestJournal.Entry e, String header, int lx, float sy) {
        this.text(g, header, lx, sy, 0.6F, this.BLUE, false);
        sy += 8.0F;

        for (QuestJournal.Task t : e.tasks) {
            boolean done = t.done();
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, done ? 0.35F : 0.8F);
            this.skin(g, this.st.taskLine, TASK_LINE, lx - 3, (int)sy, 225, 16, 0, 0, 225, 16);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            String mark = done ? "✔" : (e.state.equals("failed") ? "✘" : "◇");
            this.text(g, mark, lx + 1, sy + 4.5F, 0.75F, done ? this.GREEN : (e.state.equals("failed") ? this.RED : this.WHITE), true);
            String txt = t.text + (t.optional ? " (необязательно)" : "");
            String prog = t.count > 1 ? t.progress + "/" + t.count : "";
            float pw = this.font.width(prog) * 0.75F;
            this.text(g, this.font.plainSubstrByWidth(txt, (int)((205.0F - pw) / 0.75F)), lx + 10, sy + 4.5F, 0.75F, done ? -4669236 : this.WHITE, true);
            if (!prog.isEmpty()) {
                this.text(g, prog, lx + 218 - pw, sy + 4.5F, 0.75F, done ? this.GREEN : this.GOLD, true);
            }

            if (done) {
                g.fill(lx + 10, (int)sy + 8, (int)(lx + 10 + Math.min(205.0F - pw, this.font.width(txt) * 0.75F)), (int)sy + 9, -1715945268);
            }

            sy += 18.0F;
        }

        return sy;
    }

    @Override
    public boolean mouseClicked(double smx, double smy, int button) {
        if (button != 0) {
            return false;
        } else {
            GuiView lay = this.studioLayout();
            if (lay != null) {
                return JournalParts.click(this.live, lay.toDesignX(smx), lay.toDesignY(smy));
            } else {
                if (this.st == null) {
                    this.st = this.cfg().quests;
                }

                int[] ol = this.st.off("list");
                int[] oc = this.st.off("card");
                double mx = smx / this.scale - ol[0];
                double my = smy / this.scale - ol[1];
                double cmx = smx / this.scale - oc[0];
                double cmy = smy / this.scale - oc[1];

                for (int i = 0; i < 3; i++) {
                    if (in(this.tabRect(i), mx, my)) {
                        if (this.tab != i) {
                            this.tab = i;
                            this.selected = null;
                            this.listScroll = 0.0;
                            this.textScroll = 0.0;
                            click();
                        }

                        return true;
                    }
                }

                List<QuestJournal.Entry> ls = this.list();
                if (my >= this.y + 31 && my < this.y + 31 + 195) {
                    for (int ix = 0; ix < ls.size(); ix++) {
                        if (in(this.entryRect(ix), mx, my)) {
                            if (!ls.get(ix).key.equals(this.selected)) {
                                this.selected = ls.get(ix).key;
                                this.textScroll = 0.0;
                                click();
                            }

                            return true;
                        }
                    }
                }

                QuestJournal.Entry e = this.selected != null ? this.journal.entry(this.selected) : null;
                if (e != null && e.state.equals("active") && in(this.trackRect(), cmx, cmy)) {
                    if (this.preview) {
                        this.journal.tracked = this.journal.tracked.equals(e.key) ? "" : e.key;
                    } else {
                        QuestClient.track(e.key);
                    }

                    click();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double smx, double smy, double delta) {
        GuiView lay = this.studioLayout();
        if (lay != null) {
            JournalParts.scroll(this.live, lay.toDesignX(smx), lay.toDesignY(smy), delta);
            return true;
        } else {
            if (this.st == null) {
                this.st = this.cfg().quests;
            }

            double mx = smx / this.scale - this.st.off("list")[0];
            if (mx < this.x + 145) {
                double max = Math.max(0, this.list().size() * 37 - 195 + 4);
                this.listScroll = Math.max(0.0, Math.min(max, this.listScroll - delta * 20.0));
            } else {
                this.textScroll = Math.max(0.0, Math.min(this.maxText, this.textScroll - delta * 15.0));
            }

            return true;
        }
    }

    private static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Mc.sound(SoundEvents.UI_BUTTON_CLICK), 1.0F));
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!QuestClient.JOURNAL.matches(key, scan) && !Minecraft.getInstance().options.keyInventory.matches(key, scan)) {
            return super.keyPressed(key, scan, mods);
        } else {
            this.onClose();
            return true;
        }
    }
}
