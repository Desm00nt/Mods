package org.sprautestudio.spraute_engine.gui.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.addon.AddonClient;
import org.sprautestudio.spraute_engine.compat.Axes;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.content.ContentClient;
import org.sprautestudio.spraute_engine.core.client.ChatLogScreen;
import org.sprautestudio.spraute_engine.core.client.SlotOverlay;
import org.sprautestudio.spraute_engine.core.client.SpeechClient;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.PlateLayout;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.engine.client.ChoiceLayout;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.HudPainter;
import org.sprautestudio.spraute_engine.engine.client.ProjectSounds;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.GuiDrag;
import org.sprautestudio.spraute_engine.gui.GuiMotion;
import org.sprautestudio.spraute_engine.gui.GuiSchema;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.nav.NavClient;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.quest.QuestJournal;
import org.sprautestudio.spraute_engine.quest.client.JournalParts;
import org.sprautestudio.spraute_engine.quest.client.QuestClient;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.PlayerSlots;
import org.sprautestudio.spraute_engine.shop.client.ShopParts;
import org.sprautestudio.spraute_engine.skills.client.SkillParts;
import org.sprautestudio.spraute_engine.skills.client.SkillsClient;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class GuiView extends Widget implements UI.CursorProvider {
    public GuiDoc doc;
    public String project;
    public Project localProject;
    public GuiView.Listener listener;
    public boolean interactive = true;
    public boolean freeMouse;
    protected long openedAt = System.nanoTime();
    public boolean playAppear = true;
    public boolean drawBackground = true;
    protected float areaX;
    protected float areaY;
    protected float areaScale = 1.0F;
    protected float areaW;
    protected float areaH;
    private final Map<String, EntityPreview> entities = new HashMap<>();
    private final Map<String, Float> hoverAnim = new HashMap<>();
    private final Map<String, Float> pressAnim = new HashMap<>();
    private final Set<String> hoveredChain = new HashSet<>();
    private float drawnHover;
    private final Map<String, Float> shownValue = new HashMap<>();
    private final Map<String, Long> textChangedAt = new HashMap<>();
    private final Map<String, String> inputs = new LinkedHashMap<>();
    private String focusedInput;
    private String pressedId;
    private String hoveredId;
    private final Set<String> hoverOpen = new HashSet<>();
    private final Set<String> hoverFollowing = new HashSet<>();
    private int mouseX;
    private int mouseY;
    private final Map<GuiDoc.Element, int[]> rects = new IdentityHashMap<>();
    private final Map<GuiDoc.Element, float[]> areas = new IdentityHashMap<>();
    private final Map<GuiDoc.Element, int[]> contents = new IdentityHashMap<>();
    private final Map<String, float[]> scrolls = new HashMap<>();
    private final Map<String, float[]> dragOffsets = new HashMap<>();
    private final Map<String, String> dragZones = new HashMap<>();
    private String dragPress;
    private String dragHeld;
    private double dragPressX;
    private double dragPressY;
    private float dragFromX;
    private float dragFromY;
    private long dragStep;
    private String draggingBar;
    private boolean barVertical;
    private float viewX0;
    private float viewY0;
    private float viewX1;
    private float viewY1;
    public Supplier<HudPainter.State> hudState;
    private static HudPainter.State sample;
    public final Map<String, String> vars = new HashMap<>();
    public List<String> answers;
    public int answerHover = -1;
    private final List<int[]> answerRects = new ArrayList<>();
    public LivingEntity speaker;
    public boolean noSpeaker;
    public String speechIcon;
    public SpeechClient.Line speechLine;
    public List<SpeechClient.Line> chatLines;
    public int chatScroll;
    public float chatSince = 1000000.0F;
    public int speakerColor = -1;
    public float opacity = 1.0F;
    public Object liveState;
    private static final Map<String, String> SAMPLE_VARS = Map.of(
        "name",
        "Слепая Агата",
        "question",
        "Ты ведь слышал все тринадцать ударов?",
        "title",
        "Задача выполнена",
        "text",
        "Найти того, кто напевает на северной улице",
        "kind",
        "task",
        "zone",
        "Деревня Ольховка",
        "subtitle",
        "Тихое место у реки",
        "hint",
        "▸ F8"
    );
    private static final Pattern SKILL_VALUE = Pattern.compile("\\{skill:(?:([^:}]+):)?(level|nextlevel|xp|xpnext|maxlevel|title|points|rank)(?::([^}]+))?\\}");
    private static final List<String> SAMPLE_ANSWERS = List.of("Слышал.", "Их было двенадцать.", "Расскажи подробнее…");
    private static final SpeechClient.Line SAMPLE_LINE = new SpeechClient.Line(
        "Слепая Агата", -1, "generic_man", "Ты ведь слышал все тринадцать ударов?", "▸ F8", false
    );
    private static final List<SpeechClient.Line> SAMPLE_CHAT = List.of(
        new SpeechClient.Line("Слепая Агата", -1, "generic_man", "Ты пришёл слишком поздно.", "", false),
        new SpeechClient.Line("", -1, "generic_man", "Колокол ещё гудел над площадью.", "", true),
        SAMPLE_LINE
    );
    private static final String PLATE = "speech_plate";
    private SpeechClient.PlateBox plateBox;
    private static final ResourceLocation QUEST_ICONS = new ResourceLocation("spraute_engine", "textures/gui/journal/icons.png");
    private final Map<String, double[]> tweens = new LinkedHashMap<>();
    private final Map<String, String> tweenEnd = new HashMap<>();
    private boolean paintAfter;
    private static final float PRESS_SPEED = 24.0F;
    private static final float MIN_PERSPECTIVE = 50.0F;
    private static final int SHADOW_COLOR = 1610612736;
    private static final int SHADOW_DOWN = 2;
    private static final ResourceLocation MC_WIDGETS = new ResourceLocation("minecraft", "textures/gui/widgets.png");
    private static final ResourceLocation MC_WINDOW = new ResourceLocation("minecraft", "textures/gui/demo_background.png");
    private static final ResourceLocation MC_STONE = new ResourceLocation("minecraft", "textures/gui/options_background.png");
    private static final int MC_BUTTON_OFF = 46;
    private static final int MC_BUTTON = 66;
    private static final int MC_BUTTON_HOVER = 86;
    private static final int MC_SHEET = 256;
    private static final int MC_BUTTON_W = 200;
    private static final int MC_BUTTON_H = 20;
    private static final int MC_EDGE = 3;
    private static final int MC_WINDOW_W = 248;
    private static final int MC_WINDOW_H = 166;
    private static final int MC_WINDOW_EDGE = 4;
    private static final int MC_STONE_SIZE = 16;
    private static final int MC_STONE_TINT = -12566464;
    private static final int MC_DARK_EDGE = -16777216;
    private static final float FONT_UNITS = 9.0F;
    private static final float SHADOW_ALPHA = 0.35F;
    private static final float SHADOW_SHIFT = 0.6F;
    private static final int MC_HOVER_TEXT = -96;
    public BiConsumer<Integer, Integer> cellClick;
    private int carried = -1;

    public float mouseDX() {
        return this.toDesignX(this.mouseX);
    }

    public float mouseDY() {
        return this.toDesignY(this.mouseY);
    }

    public int answerAt(double sx, double sy) {
        float dx = this.toDesignX(sx);
        float dy = this.toDesignY(sy);

        for (int i = 0; i < this.answerRects.size(); i++) {
            int[] r = this.answerRects.get(i);
            if (dx >= r[0] && dx < r[0] + r[2] && dy >= r[1] && dy < r[1] + r[3]) {
                return i;
            }
        }

        return -1;
    }

    private HudPainter.State hud() {
        if (this.hudState != null) {
            HudPainter.State s = this.hudState.get();
            if (s != null) {
                return s;
            }
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && !inEditor()) {
            return EngineClient.state(player);
        } else {
            if (sample == null) {
                HudPainter.State s = new HudPainter.State();
                s.hp = 14.0F;
                s.armor = 8;
                s.food = 17;
                s.air = 180;
                s.exp = 0.45F;
                s.level = 7;
                s.coins = 1250;
                s.selected = 1;
                ItemStack[] items = new ItemStack[]{
                    new ItemStack(Items.IRON_SWORD),
                    new ItemStack(Items.BREAD, 12),
                    new ItemStack(Items.TORCH, 48),
                    ItemStack.EMPTY,
                    new ItemStack(Items.MAP),
                    ItemStack.EMPTY,
                    ItemStack.EMPTY,
                    new ItemStack(Items.COMPASS),
                    new ItemStack(Items.APPLE, 3)
                };
                System.arraycopy(items, 0, s.hotbar, 0, 9);
                sample = s;
            }

            sample.stHealth = EngineClient.config().health;
            return sample;
        }
    }

    private static boolean inEditor() {
        return Minecraft.getInstance().screen instanceof SprauteScreen;
    }

    private String fillSkills(String text) {
        if (!text.contains("{skill:")) {
            return text;
        } else {
            boolean sampleValues = inEditor();
            Matcher m = SKILL_VALUE.matcher(text);
            StringBuilder out = new StringBuilder();

            while (m.find()) {
                String v = SkillsClient.value(this.project, m.group(1), m.group(2), m.group(3));
                if (v == null) {
                    v = sampleValues ? "7" : "0";
                }

                m.appendReplacement(out, Matcher.quoteReplacement(v));
            }

            m.appendTail(out);
            return out.toString();
        }
    }

    private String fillHud(String text) {
        if (text.indexOf(123) < 0) {
            return text;
        } else {
            text = this.fillSkills(text);
            HudPainter.State s = this.hud();
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer p = this.hudState == null && inEditor() ? null : mc.player;
            long day = mc.level != null ? mc.level.getDayTime() % 24000L : 6000L;
            int hh = (int)((day / 1000L + 6L) % 24L);
            int mm = (int)(day % 1000L * 60L / 1000L);

            for (String k : SAMPLE_VARS.keySet()) {
                String v = this.vars.get(k);
                if (v == null && this.hudState == null && inEditor()) {
                    v = SAMPLE_VARS.get(k);
                }

                if (v != null) {
                    text = text.replace("{" + k + "}", v);
                }
            }

            for (Entry<String, String> en : this.vars.entrySet()) {
                if (!SAMPLE_VARS.containsKey(en.getKey())) {
                    text = text.replace("{" + en.getKey() + "}", en.getValue());
                }
            }

            return text.replace("{hp}", String.valueOf(Math.round(s.hp)))
                .replace("{maxhp}", String.valueOf(Math.round(s.maxHp)))
                .replace("{food}", String.valueOf(s.food))
                .replace("{armor}", String.valueOf(s.armor))
                .replace("{air}", String.valueOf(Math.max(0, s.air) * 10 / Math.max(1, s.maxAir)))
                .replace("{level}", String.valueOf(s.level))
                .replace("{exp}", String.valueOf(Math.round(s.exp * 100.0F)))
                .replace("{coins}", String.valueOf(s.coins))
                .replace("{x}", p != null ? String.valueOf(p.getBlockX()) : "120")
                .replace("{y}", p != null ? String.valueOf(p.getBlockY()) : "64")
                .replace("{z}", p != null ? String.valueOf(p.getBlockZ()) : "-35")
                .replace("{player}", p != null ? p.getGameProfile().getName() : "Игрок")
                .replace("{time}", String.format("%02d:%02d", hh, mm));
        }
    }

    private float hudFraction(String bind) {
        HudPainter.State s = this.hud();

        return switch (bind) {
            case "health" -> s.maxHp <= 0.0F ? 0.0F : s.hp / s.maxHp;
            case "food" -> s.food / 20.0F;
            case "armor" -> s.armor / 20.0F;
            case "air" -> s.maxAir <= 0 ? 0.0F : (float)Math.max(0, s.air) / s.maxAir;
            case "exp" -> s.exp;
            default -> 0.0F;
        };
    }

    private void drawQuests(Canvas c, GuiDoc.Element e, int[] r, float op) {
        List<String[]> rows = new ArrayList<>();
        String title;
        if (this.hudState != null) {
            QuestJournal j = QuestClient.journal();
            QuestJournal.Entry q = j.tracked.isEmpty() ? null : j.entry(j.tracked);
            if (q == null || !q.state.equals("active")) {
                return;
            }

            title = q.title;

            for (QuestJournal.Task t : q.tasks) {
                rows.add(
                    new String[]{
                        (t.done() ? "✔ " : "◇ ") + t.text + (t.count > 1 ? "  " + t.progress + "/" + t.count : ""),
                        t.done() ? "done" : (t.optional ? "opt" : "")
                    }
                );
            }
        } else {
            title = "Тринадцатый удар";
            rows.add(new String[]{"✔ Найти хоть кого-нибудь живого", "done"});
            rows.add(new String[]{"◇ Собрать души-огоньки  1/3", ""});
            rows.add(new String[]{"◇ Заметить, что у монаха нет тени", "opt"});
        }

        c.flush();
        String al = e.get("align");
        boolean shadow = e.b("shadow");
        Component tc = Component.literal(c.font.plainSubstrByWidth(title, r[2])).withStyle(ChatFormatting.BOLD);
        int tw = c.font.width(tc);
        int tx = al.equals("right") ? r[0] + r[2] - tw : (al.equals("center") ? r[0] + (r[2] - tw) / 2 : r[0]);
        c.g.drawString(c.font, tc, tx, r[1], a(e.color("titleColor"), op), shadow);
        float ts = Math.max(0.3F, e.f("textScale"));
        float y = r[1] + 11;
        int shown = 0;

        for (String[] row : rows) {
            if (shown++ >= Math.max(1, e.i("maxTasks")) || y > r[1] + r[3]) {
                break;
            }

            String s = c.font.plainSubstrByWidth(row[0], (int)(r[2] / ts));
            int col = row[1].equals("done") ? e.color("doneColor") : (row[1].equals("opt") ? e.color("optionalColor") : e.color("textColor"));
            float w = c.font.width(s) * ts;
            float x = al.equals("right") ? r[0] + r[2] - w : (al.equals("center") ? r[0] + (r[2] - w) / 2.0F : r[0]);
            c.g.pose().pushPose();
            c.g.pose().translate(x, y, 0.0F);
            c.g.pose().scale(ts, ts, 1.0F);
            c.g.drawString(c.font, s, 0, 0, a(col, op), shadow);
            c.g.pose().popPose();
            y += 10.0F * ts + 1.0F;
        }
    }

    private void drawAnswers(Canvas c, GuiDoc.Element e, int[] r, float op) {
        List<String> list = this.answers != null ? this.answers : SAMPLE_ANSWERS;
        String style = e.get("style");
        float ts = Math.max(0.3F, e.f("textScale"));
        int pad = e.i("padding");
        int gap = e.i("gap");
        int minH = Math.max(6, e.i("minHeight"));
        int innerW = Math.max(10, Math.round((r[2] - 2 * pad) / ts));
        int y = r[1];
        c.flush();

        for (int i = 0; i < list.size(); i++) {
            String label = (e.b("numbers") ? i + 1 + ". " : "") + list.get(i);
            List<FormattedCharSequence> lines = c.font.split(Component.literal(label), innerW);
            int h = Math.max(minH, Math.round(lines.size() * 9 * ts) + 2 * pad);
            boolean hot = this.answers != null ? i == this.answerHover : i == 0;
            switch (style) {
                case "flat":
                    this.box(
                        c,
                        r[0],
                        y,
                        r[2],
                        h,
                        e.i("radius"),
                        a(hot ? e.color("hoverColor") : e.color("color"), op),
                        a(hot ? e.color("hoverBorderColor") : e.color("borderColor"), op),
                        1
                    );
                    c.flush();
                    break;
                case "image":
                    String img = hot && !e.get("hoverImage").isEmpty() ? e.get("hoverImage") : e.get("image");
                    if (!img.isEmpty()) {
                        this.background(c, e, new int[]{r[0], y, r[2], h}, img, op);
                    }
                    break;
                default:
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);
                    ChoiceLayout.drawButton(c.g, hot, r[0], y, r[2], h);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }

            int col = a(hot ? e.color("hoverTextColor") : e.color("textColor"), op);
            float ty = y + (h - lines.size() * 9 * ts) / 2.0F;

            for (int k = 0; k < lines.size(); k++) {
                c.g.pose().pushPose();
                c.g.pose().translate((float)(r[0] + pad), ty + 9.0F * ts * k, 0.0F);
                c.g.pose().scale(ts, ts, 1.0F);
                c.g.drawString(c.font, lines.get(k), 0, 0, col, false);
                c.g.pose().popPose();
            }

            this.answerRects.add(new int[]{r[0], y, r[2], h});
            y += h + gap;
        }
    }

    private static SpeechClient.PlateStyle plateStyle(GuiDoc.Element plate) {
        GuiDoc.Element icon = PlateLayout.icon(plate);
        GuiDoc.Element text = PlateLayout.text(plate);
        boolean shown = icon != null && icon.b("visible");
        return new SpeechClient.PlateStyle(
            shown,
            icon != null ? Math.max(1, icon.i("w")) : 20,
            icon != null ? icon.i("x") : 0,
            icon != null ? icon.i("y") : 0,
            text != null ? text.i("x") : 0,
            text != null ? text.i("y") : 0
        );
    }

    private void placePlate(GuiDoc.Element e, int fx, int fy, int fw, int fh) {
        Font font = Minecraft.getInstance().font;
        SpeechClient.Line line = this.speechLine != null ? this.speechLine : SAMPLE_LINE;
        SpeechClient.PlateBox box = SpeechClient.plateBox(font, line, fw, plateStyle(e), true);
        String va = e.get("valign");
        int px = SpeechClient.plateCenter(e.get("halign"), fx, fw, box.w()) - box.w() / 2;
        int py = va.equals("top") ? fy : (va.equals("middle") ? fy + (fh - box.h()) / 2 : fy + fh - box.h());
        this.rects.put(e, new int[]{px, py, box.w(), box.h()});
        this.plateBox = box;
        GuiDoc.Element icon = PlateLayout.icon(e);
        GuiDoc.Element text = PlateLayout.text(e);
        float[] area = new float[]{px, py, box.w(), box.h()};

        for (GuiDoc.Element part : e.children) {
            this.rects
                .put(
                    part,
                    part == icon
                        ? new int[]{px + box.iconX(), py + box.iconY(), box.iconSize(), box.iconSize()}
                        : (
                            part == text
                                ? new int[]{px + box.textX(), py + box.textY(), Math.max(1, box.textW()), Math.max(1, box.textH())}
                                : new int[]{px, py, 1, 1}
                        )
                );
            this.areas.put(part, area);
        }
    }

    private void drawSpeechText(Canvas c, GuiDoc.Element e, int[] r, float op) {
        if (this.plateBox != null) {
            GuiDoc.Element owner = null;

            for (GuiDoc.Element p : this.doc.all()) {
                if (p.children.contains(e)) {
                    owner = p;
                }
            }

            c.flush();
            SpeechClient.drawPlateText(c.g, c.font, this.plateBox, r[0], r[1], owner != null ? owner.color("textColor") : -1, op);
        }
    }

    private void drawSpeechPlate(Canvas c, GuiDoc.Element e, int[] r, float op) {
        if (!e.children.isEmpty()) {
            c.fill(r[0], r[1], r[2], r[3], a(e.color("background"), op));
        } else {
            SpeechClient.Line line = this.speechLine != null ? this.speechLine : SAMPLE_LINE;
            int h = SpeechClient.plateHeight(c.font, line, r[2]);
            String va = e.get("valign");
            int y = va.equals("top") ? r[1] : (va.equals("middle") ? r[1] + (r[3] - h) / 2 : r[1] + r[3] - h);
            c.flush();
            int center = SpeechClient.plateCenter(e.get("halign"), r[0], r[2], SpeechClient.plateWidth(c.font, line, r[2]));
            SpeechClient.plate(c.g, c.font, line, center, y, r[2], e.color("background"), e.color("textColor"), op, true);
        }
    }

    private void drawChatLines(Canvas c, GuiDoc.Element e, int[] r, float op) {
        List<SpeechClient.Line> lines = this.chatLines != null ? this.chatLines : SAMPLE_CHAT;
        float k = Math.max(0.01F, this.areaScale);
        c.flush();
        c.pushClip(r[0], r[1], r[2], r[3]);
        c.g.pose().pushPose();
        c.g.pose().translate(r[0] + r[2] / 2.0F, (float)r[1], 0.0F);
        c.g.pose().scale(1.0F / k, 1.0F / k, 1.0F);
        ChatLogScreen.drawLines(
            c.g,
            c.font,
            lines,
            0,
            Math.round(r[2] * k),
            0,
            Math.round(r[3] * k),
            this.chatScroll,
            this.chatSince,
            Math.round((e.color("background") >>> 24) * op) << 24 | e.color("background") & 16777215,
            e.color("textColor")
        );
        c.g.pose().popPose();
        c.flush();
        c.popClip();
    }

    private void drawSpeechIcon(Canvas c, GuiDoc.Element e, int[] r, float op) {
        int bg = e.color("background");
        if (bg >>> 24 != 0) {
            c.fill(r[0], r[1], r[2], r[3], a(bg, op));
        }

        c.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);
        ChatHeads.drawIcon(c.g, SpeechClient.iconTexture(this.speechIcon), r[0], r[1], r[2], r[3]);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawSpeaker(Canvas c, GuiDoc.Element e, int[] r, float op) {
        int bg = e.color("background");
        if (bg >>> 24 != 0) {
            c.fill(r[0], r[1], r[2], r[3], a(bg, op));
        }

        LivingEntity who = (LivingEntity)(this.speaker != null ? this.speaker : (this.hudState == null ? Minecraft.getInstance().player : null));
        if (who != null) {
            c.flush();
            int[] clip = ChatHeads.screenRect(c.g, r[0], r[1], r[2], r[3]);
            c.g.enableScissor(clip[0], clip[1], clip[2], clip[3]);
            c.g.pose().pushPose();
            c.g.pose().translate(0.0F, 0.0F, 200.0F);

            try {
                float sc = Math.max(0.1F, e.f("scale"));
                if (e.get("framing").equals("head")) {
                    ChatHeads.render(c.g, who, r[0], r[1], r[2], r[3], new ChatHeads.Params(sc, 0, 0, 0, "auto"));
                } else {
                    float eh = Math.max(0.5F, who.getBbHeight());
                    float ew = Math.max(0.5F, who.getBbWidth());
                    int size = (int)Math.max(4.0F, Math.min((r[3] - 8) / eh, (r[2] - 8) / (ew * 1.4F)) * sc);
                    float mx = this.toDesignX(this.mouseX);
                    float my = this.toDesignY(this.mouseY);
                    Mc.inventoryEntity(c.g, r[0] + r[2] / 2, r[1] + r[3] - 4, size, r[0] + r[2] / 2.0F - mx, r[1] + r[3] / 3.0F - my, who);
                }
            } catch (Throwable var17) {
            } finally {
                c.g.pose().popPose();
                c.g.disableScissor();
            }
        }
    }

    private void drawQuestIcon(Canvas c, GuiDoc.Element e, int[] r, float op) {
        String kind = this.vars.getOrDefault("kind", this.hudState == null ? "task" : "");
        int i = kind.equals("complete") ? 1 : (kind.equals("fail") ? 2 : 0);
        c.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);
        String own = e.get("image");
        if (own.isEmpty()) {
            c.g.blit(QUEST_ICONS, r[0], r[1], r[2], r[3], i * 21, 0.0F, 21, 23, 63, 46);
        } else {
            EngineConfig.Skin sk = new EngineConfig.Skin();
            sk.image = own;
            Skins.drawSheet(c.g, this.project, sk, QUEST_ICONS, r[0], r[1], r[2], r[3], i * 21, 0, 63, 46);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean replaced(GuiDoc.Element e, String key) {
        if (!EngineConfig.gameDrawn(e.get("style"))) {
            return false;
        } else {
            for (GuiDoc.Element o : this.doc.all()) {
                if (o != e && o.b("visible")) {
                    if (o.type.equals(e.type) && !EngineConfig.gameDrawn(o.get("style"))) {
                        return true;
                    }

                    if (o.type.equals("progress") && o.get("bind").equals(key)) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    private void drawHud(Canvas c, GuiDoc.Element e, int[] r, float op) {
        String key = e.type.substring(4);
        if (!this.replaced(e, key)) {
            if (key.equals("nav")) {
                c.flush();
                if (this.hudState != null && !inEditor()) {
                    NavClient.drawAt(c.g, r, Minecraft.getInstance().getFrameTime());
                } else {
                    HudPainter.standard(c.g, Minecraft.getInstance().font, "nav", r, this.hud());
                }
            } else {
                HudPainter.State s = this.hud();
                EngineConfig cfg = EngineClient.config();
                Minecraft mc = Minecraft.getInstance();
                if (this.hudState == null || EngineClient.shown(mc, key, s) && (!key.equals("coins") || cfg.currency)) {
                    EngineConfig.Element el = new EngineConfig.Element();
                    el.style = e.get("style");
                    el.count = Math.max(1, e.i("count"));
                    el.pad = e.i("pad");
                    el.fillDir = e.get("direction");
                    el.gap = e.i("gap");
                    el.reverse = e.b("reverse");
                    el.text = e.b("showText");
                    el.textColor = e.color("textColor");
                    el.fillColor = e.color("fillColor");
                    el.backColor = e.color("backColor");
                    el.waves = !"false".equals(e.get("waves"));
                    el.width = r[2];
                    el.height = r[3];
                    el.back.image = e.get("back");
                    el.fill.image = e.get("fill");
                    el.frame.image = e.get("frame");
                    el.full.image = e.get("full");
                    el.half.image = e.get("half");
                    el.empty.image = e.get("empty");
                    c.flush();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, op);

                    try {
                        if (EngineConfig.gameDrawn(el.style)) {
                            int nw;
                            int nh;
                            switch (key) {
                                case "health":
                                    nw = s.stHealth ? 68 : 81;
                                    nh = s.stHealth ? 67 : 9;
                                    break;
                                case "exp":
                                    nw = 182;
                                    nh = 5;
                                    break;
                                case "hotbar":
                                    nw = 182;
                                    nh = 22;
                                    break;
                                case "coins":
                                    nw = 56;
                                    nh = 18;
                                    break;
                                default:
                                    nw = 81;
                                    nh = 9;
                            }

                            float k = Math.min((float)r[2] / nw, (float)r[3] / nh);
                            c.g.pose().pushPose();
                            c.g.pose().translate(r[0] + (r[2] - nw * k) / 2.0F, r[1] + (r[3] - nh * k) / 2.0F, 0.0F);
                            c.g.pose().scale(k, k, 1.0F);
                            HudPainter.standard(c.g, c.font, key, el.style, new int[]{0, 0, nw, nh}, s);
                            c.g.pose().popPose();
                        } else {
                            EngineConfig tmp = new EngineConfig();
                            tmp.project = this.project != null && !this.project.isEmpty() ? this.project : cfg.project;
                            tmp.hud.put(key, el);
                            HudPainter.custom(c.g, c.font, tmp, key, r, s);
                        }
                    } finally {
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    }
                }
            }
        }
    }

    public GuiView(GuiDoc doc, String project) {
        this.doc = doc;
        this.project = project;
        this.focusable = true;

        for (GuiDoc.Element e : doc.all()) {
            if (e.type.equals("input")) {
                this.inputs.put(e.id, e.get("text"));
            }
        }
    }

    public void restartAppear() {
        this.openedAt = System.nanoTime();
        this.textChangedAt.clear();
    }

    public void fire(String element, String kind) {
        if (this.listener != null) {
            this.listener.event(element, kind);
        }
    }

    public Path videoFile(String name) {
        if (!name.isBlank() && !name.contains("/") && !name.contains("\\") && !name.contains("..")) {
            if (this.localProject != null) {
                Path own = Project.findVideo(this.localProject.videosDir(), name);
                if (own != null) {
                    return own;
                }
            }

            for (Workspace.ProjectInfo info : Workspace.projects()) {
                if (info.name().equals(this.project)) {
                    Path own = Project.findVideo(info.dir().resolve("videos"), name);
                    if (own != null) {
                        return own;
                    }
                }
            }

            Path server = ContentClient.dir(this.project);
            Path copy = server != null ? Project.findVideo(server.resolve("videos"), name) : null;
            return copy != null ? copy : Project.findVideo(Workspace.root().resolve("videos").resolve(this.project != null ? this.project : ""), name);
        } else {
            return null;
        }
    }

    public static void drawTextureId(Canvas c, int texture, int x, int y, int w, int h, float u0, float v0, float u1, float v1, float opacity) {
        c.flush();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        Matrix4f matrix = c.g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.vertex(matrix, x, y + h, 0.0F).uv(u0, v1).endVertex();
        b.vertex(matrix, x + w, y + h, 0.0F).uv(u1, v1).endVertex();
        b.vertex(matrix, x + w, y, 0.0F).uv(u1, v0).endVertex();
        b.vertex(matrix, x, y, 0.0F).uv(u0, v0).endVertex();
        BufferUploader.drawWithShader(b.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    protected String forcedHover() {
        return null;
    }

    protected float time() {
        return this.playAppear ? (float)(System.nanoTime() - this.openedAt) / 1.0E9F : 1000000.0F;
    }

    public Map<String, String> inputs() {
        return this.inputs;
    }

    public void update(String id, String prop, String value) {
        GuiDoc.Element e = this.doc.element(id);
        if (e != null) {
            if (prop.equals("undrag")) {
                float[] o = this.dragOffsets.get(id);
                if (o != null) {
                    o[2] = o[3] = 0.0F;
                }

                this.dragZones.remove(id);
            } else {
                if (prop.startsWith("x") || prop.startsWith("y")) {
                    int mark = prop.indexOf(64);
                    if ((mark < 0 ? prop : prop.substring(0, mark)).length() == 1) {
                        this.dragOffsets.remove(id);
                    }
                }

                int at = prop.indexOf(64);
                if (at > 0) {
                    float secs = GuiDoc.toFloat(prop.substring(at + 1));
                    prop = prop.substring(0, at);
                    String cur = e.get(prop);
                    boolean numeric = isNumber(cur) && isNumber(value);
                    if (secs > 0.0F && numeric) {
                        this.tweens
                            .put(id + "\u0000" + prop, new double[]{Double.parseDouble(cur.trim()), Double.parseDouble(value.trim()), System.nanoTime(), secs});
                        this.tweenEnd.put(id + "\u0000" + prop, value);
                        return;
                    }
                }

                this.tweens.remove(id + "\u0000" + prop);
                e.set(prop, value);
                if (prop.equals("text")) {
                    this.textChangedAt.put(id, System.nanoTime());
                    if (e.type.equals("input")) {
                        this.inputs.put(id, value);
                    }
                }

                if (prop.equals("scrollY") || prop.equals("scrollX")) {
                    float[] s = this.scrolls.computeIfAbsent(id, k -> new float[4]);
                    float v = GuiDoc.toFloat(value);
                    if (prop.equals("scrollY")) {
                        s[1] = s[3] = v;
                    } else {
                        s[0] = s[2] = v;
                    }
                }
            }
        }
    }

    private static boolean isNumber(String s) {
        try {
            Double.parseDouble(s.trim());
            return true;
        } catch (RuntimeException var2) {
            return false;
        }
    }

    private void stepTweens() {
        if (!this.tweens.isEmpty()) {
            long now = System.nanoTime();
            Iterator<Entry<String, double[]>> it = this.tweens.entrySet().iterator();

            while (it.hasNext()) {
                Entry<String, double[]> en = it.next();
                String[] k = en.getKey().split("\u0000", 2);
                GuiDoc.Element e = this.doc.element(k[0]);
                double[] t = en.getValue();
                if (e == null) {
                    it.remove();
                } else {
                    double p = (now - t[2]) / 1.0E9 / t[3];
                    if (p >= 1.0) {
                        e.set(k[1], this.tweenEnd.getOrDefault(en.getKey(), String.valueOf(t[1])));
                        it.remove();
                    } else {
                        double q = p < 0.5 ? 2.0 * p * p : 1.0 - Math.pow(-2.0 * p + 2.0, 2.0) / 2.0;
                        double v = t[0] + (t[1] - t[0]) * q;
                        e.set(k[1], String.valueOf(Math.round(v * 100.0) / 100.0));
                    }
                }
            }
        }
    }

    protected final void computeArea() {
        this.stepTweens();
        this.stepDrags();
        this.placeArea();
        this.layoutTree();
    }

    protected void placeArea() {
        if (this.doc.get("layout").equals("screen")) {
            this.areaScale = 1.0F;
            this.areaX = this.x;
            this.areaY = this.y;
            this.areaW = this.w;
            this.areaH = this.h;
        } else if (this.doc.get("layout").equals("height")) {
            this.areaH = this.doc.height();
            this.areaScale = this.h / this.areaH;
            this.areaW = this.w / this.areaScale;
            this.areaX = this.x;
            this.areaY = this.y;
        } else {
            this.areaW = this.doc.width();
            this.areaH = this.doc.height();
            this.areaScale = Math.min(this.w / this.areaW, this.h / this.areaH);
            this.areaX = this.x + (this.w - this.areaW * this.areaScale) / 2.0F;
            this.areaY = this.y + (this.h - this.areaH * this.areaScale) / 2.0F;
        }
    }

    private static float ax(String anchor) {
        return anchor.endsWith("left") ? 0.0F : (anchor.endsWith("right") ? 1.0F : 0.5F);
    }

    private static float ay(String anchor) {
        return anchor.startsWith("top") ? 0.0F : (anchor.startsWith("bottom") ? 1.0F : 0.5F);
    }

    public void layoutTree() {
        this.rects.clear();
        this.areas.clear();
        this.contents.clear();
        this.place(this.doc.elements, 0.0F, 0.0F, this.areaW, this.areaH);
    }

    private void place(List<GuiDoc.Element> list, float ax, float ay, float aw, float ah) {
        for (GuiDoc.Element e : list) {
            String a = e.get("anchor");
            float fx = ax(a);
            float fy = ay(a);
            int ew = Math.max(1, Math.round(dim(e.get("w"), aw)));
            int eh = Math.max(1, Math.round(dim(e.get("h"), ah)));
            int ex = Math.round(ax + fx * aw + dim(e.get("x"), aw) - fx * ew);
            int ey = Math.round(ay + fy * ah + dim(e.get("y"), ah) - fy * eh);
            float[] dragged = this.dragOffsets.get(e.id);
            if (dragged != null) {
                ex += Math.round(dragged[0]);
                ey += Math.round(dragged[1]);
            }

            String stretch = e.get("stretch");
            if (this.areaScale > 0.0F && (stretch.equals("width") || stretch.equals("both"))) {
                ex = (int)Math.floor((this.x - this.areaX) / this.areaScale);
                ew = Math.max(1, (int)Math.ceil(this.w / this.areaScale) + 1);
            }

            if (this.areaScale > 0.0F && (stretch.equals("height") || stretch.equals("both"))) {
                ey = (int)Math.floor((this.y - this.areaY) / this.areaScale);
                eh = Math.max(1, (int)Math.ceil(this.h / this.areaScale) + 1);
            }

            if (e.type.equals("speech_plate") && !e.children.isEmpty()) {
                this.placePlate(e, ex, ey, ew, eh);
                this.areas.put(e, new float[]{ax, ay, aw, ah});
            } else {
                this.rects.put(e, new int[]{ex, ey, ew, eh});
                this.areas.put(e, new float[]{ax, ay, aw, ah});
                if (!e.children.isEmpty() && e.isContainer()) {
                    if (!e.type.equals("scroll")) {
                        this.place(e.children, ex, ey, ew, eh);
                    } else {
                        String dir = e.get("direction");
                        boolean v = !dir.equals("horizontal");
                        boolean hz = !dir.equals("vertical");
                        int cw = e.i("contentWidth");
                        int ch = e.i("contentHeight");
                        if (cw <= 0 || ch <= 0) {
                            this.place(e.children, ex, ey, ew, eh);
                            int maxR = ew;
                            int maxB = eh;
                            int pad = e.i("padding");

                            for (GuiDoc.Element c : e.children) {
                                int[] r = this.rects.get(c);
                                maxR = Math.max(maxR, r[0] + r[2] - ex + pad);
                                maxB = Math.max(maxB, r[1] + r[3] - ey + pad);
                            }

                            if (cw <= 0) {
                                cw = maxR;
                            }

                            if (ch <= 0) {
                                ch = maxB;
                            }
                        }

                        if (!hz) {
                            cw = ew;
                        }

                        if (!v) {
                            ch = eh;
                        }

                        cw = Math.max(cw, ew);
                        ch = Math.max(ch, eh);
                        this.contents.put(e, new int[]{cw, ch});
                        float[] s = this.scrolls.computeIfAbsent(e.id, k -> new float[4]);
                        s[2] = Math.max(0.0F, Math.min((float)(cw - ew), s[2]));
                        s[3] = Math.max(0.0F, Math.min((float)(ch - eh), s[3]));
                        s[0] = Math.max(0.0F, Math.min((float)(cw - ew), s[0]));
                        s[1] = Math.max(0.0F, Math.min((float)(ch - eh), s[1]));
                        this.place(e.children, ex - Math.round(s[0]), ey - Math.round(s[1]), cw, ch);
                    }
                }
            }
        }
    }

    public static float dim(String v, float base) {
        String s = v == null ? "" : v.trim();
        return s.endsWith("%") ? GuiDoc.toFloat(s.substring(0, s.length() - 1)) * base / 100.0F : GuiDoc.toFloat(s);
    }

    public int[] rect(GuiDoc.Element e) {
        int[] r = this.rects.get(e);
        if (r == null) {
            this.layoutTree();
            r = this.rects.get(e);
        }

        return r != null ? r : new int[]{0, 0, 1, 1};
    }

    public int[] offsetFor(GuiDoc.Element e, int rx, int ry, int rw, int rh) {
        String a = e.get("anchor");
        float fx = ax(a);
        float fy = ay(a);
        float[] ar = this.areas.get(e);
        if (ar == null) {
            ar = new float[]{0.0F, 0.0F, this.areaW, this.areaH};
        }

        return new int[]{Math.round(rx - ar[0] - fx * ar[2] + fx * rw), Math.round(ry - ar[1] - fy * ar[3] + fy * rh)};
    }

    public float[] areaOf(GuiDoc.Element e) {
        float[] ar = this.areas.get(e);
        return ar != null ? ar : new float[]{0.0F, 0.0F, this.areaW, this.areaH};
    }

    public float toDesignX(double px) {
        return (float)((px - this.areaX) / this.areaScale);
    }

    public float toDesignY(double py) {
        return (float)((py - this.areaY) / this.areaScale);
    }

    public float areaX() {
        return this.areaX;
    }

    public float areaY() {
        return this.areaY;
    }

    public float areaScale() {
        return this.areaScale;
    }

    public float areaW() {
        return this.areaW;
    }

    public float areaH() {
        return this.areaH;
    }

    public GuiDoc.Element elementAt(float dx, float dy, boolean interactiveOnly) {
        return this.pick(
            dx,
            dy,
            e -> !interactiveOnly
                || e.type.equals("button")
                || e.type.equals("input")
                || e.type.equals("slot")
                || e.type.equals("inventory")
                || !e.get("tooltip").isEmpty()
                || !e.get("hoverShow").isEmpty()
                || this.hoverOpen.contains(e.id)
                || GuiSchema.reactsToMouse(e.props.keySet())
        );
    }

    public GuiDoc.Element pick(float dx, float dy, Predicate<GuiDoc.Element> accept) {
        return this.pickIn(this.doc.elements, dx, dy, accept);
    }

    private GuiDoc.Element pickIn(List<GuiDoc.Element> list, float dx, float dy, Predicate<GuiDoc.Element> accept) {
        for (int i = list.size() - 1; i >= 0; i--) {
            GuiDoc.Element e = list.get(i);
            if (e.b("visible") || this.hoverOpen.contains(e.id) || this.showHidden()) {
                int[] r = this.rect(e);
                float px = dx;
                float py = dy;
                float turn = e.f("rotation");
                if (turn != 0.0F) {
                    float cx = r[0] + r[2] * e.f("pivotX");
                    float cy = r[1] + r[3] * e.f("pivotY");
                    double a = Math.toRadians(-turn);
                    float co = (float)Math.cos(a);
                    float si = (float)Math.sin(a);
                    px = cx + (dx - cx) * co - (dy - cy) * si;
                    py = cy + (dx - cx) * si + (dy - cy) * co;
                }

                boolean inside = px >= r[0] && py >= r[1] && px < r[0] + r[2] && py < r[1] + r[3];
                if (e.isContainer() && !e.children.isEmpty() && (inside || GuiSchema.isLoose(e.type))) {
                    GuiDoc.Element c = this.pickIn(e.children, px, py, accept);
                    if (c != null) {
                        return c;
                    }
                }

                if (inside && accept.test(e)) {
                    return e;
                }
            }
        }

        return null;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        this.mouseX = mx;
        this.mouseY = my;
        this.computeArea();
        if (this.liveState == null) {
            for (GuiDoc.Element e : this.doc.all()) {
                if (e.type.startsWith("skill_")) {
                    SkillParts.begin(this);
                    break;
                }
            }
        }

        int bg = GuiDoc.toColor(this.doc.get("background"));
        if (bg >>> 24 != 0 && this.interactive && this.drawBackground) {
            c.fill(this.x, this.y, this.w, this.h, bg);
        }

        this.paintBackdrop(c);
        if (this.interactive) {
            GuiDoc.Element hov = this.elementAt(this.toDesignX(mx), this.toDesignY(my), true);
            this.hoveredId = hov == null || !this.freeMouse && !this.isHoveredDeep() ? null : hov.id;
        }

        this.hoveredChain.clear();
        if (this.hoveredId != null) {
            this.chain(this.doc.elements, this.hoveredId);
        }

        this.hoverMenus();
        this.paintAfter = this.interactive;
        this.viewX0 = this.toDesignX(this.x);
        this.viewY0 = this.toDesignY(this.y);
        this.viewX1 = this.toDesignX(this.x + this.w);
        this.viewY1 = this.toDesignY(this.y + this.h);
        c.pushTransform(this.areaX, this.areaY, this.areaScale);
        this.answerRects.clear();
        this.drawList(c, this.doc.elements, Math.max(0.0F, Math.min(1.0F, this.opacity)), dt);
        GuiDoc.Element top = this.dragHeld != null ? this.doc.element(this.dragHeld) : null;
        if (top != null) {
            this.drawElement(c, top, 0.0F, dt, Math.max(0.0F, Math.min(1.0F, this.opacity)));
        }

        c.popTransform();
        if (this.interactive && this.hoveredId != null) {
            GuiDoc.Element ex = this.doc.element(this.hoveredId);
            if (ex != null && !ex.get("tooltip").isEmpty()) {
                this.paintTooltip(c, this.fillHud(unescape(ex.get("tooltip"))), mx, my);
            }
        }
    }

    private boolean chain(List<GuiDoc.Element> list, String id) {
        for (GuiDoc.Element e : list) {
            if (e.id.equals(id) || this.chain(e.children, id)) {
                this.hoveredChain.add(e.id);
                return true;
            }
        }

        return false;
    }

    protected void paintBackdrop(Canvas c) {
    }

    protected boolean showHidden() {
        return false;
    }

    private void hoverMenus() {
        this.hoverOpen.clear();
        this.hoverFollowing.clear();
        if (this.interactive && this.hoveredId != null) {
            for (GuiDoc.Element e : this.doc.all()) {
                String menu = e.get("hoverShow");
                if (!menu.isEmpty() && (this.hoveredChain.contains(e.id) || this.hoveredChain.contains(menu))) {
                    this.hoverOpen.add(menu);
                    if (e.b("hoverFollow")) {
                        this.hoverFollowing.add(menu);
                    }
                }
            }
        }
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n");
    }

    private void drawList(Canvas c, List<GuiDoc.Element> list, float parentOpacity, float dt) {
        for (GuiDoc.Element e : list) {
            if ((e.b("visible") || this.hoverOpen.contains(e.id) || this.showHidden()) && !e.id.equals(this.dragHeld)) {
                int[] r = this.rect(e);
                boolean plain = !this.hoverFollowing.contains(e.id)
                    && e.f("rotation") == 0.0F
                    && e.f("rotX") == 0.0F
                    && e.f("rotY") == 0.0F
                    && e.get("appear").equals("none")
                    && e.get("loop").equals("none")
                    && !GuiSchema.reactsToMouse(e.props.keySet());
                if (!plain
                    || GuiSchema.isLoose(e.type)
                    || !(r[0] > this.viewX1) && !(r[1] > this.viewY1) && !(r[0] + r[2] < this.viewX0) && !(r[1] + r[3] < this.viewY0)) {
                    float target = !this.hoveredChain.contains(e.id) && !e.id.equals(this.forcedHover()) ? 0.0F : 1.0F;
                    float hv = this.hoverAnim.getOrDefault(e.id, 0.0F);
                    if (hv != target) {
                        if (hv == 0.0F && this.interactive) {
                            ProjectSounds.ui(this.project, e.get("hoverSound"));
                        }

                        hv += (target - hv) * GuiMotion.hoverStep(dt, e.f("hoverTime"));
                        if (Math.abs(hv - target) < 0.01F) {
                            hv = target;
                        }

                        this.hoverAnim.put(e.id, hv);
                    }

                    try {
                        this.drawElement(c, e, hv, dt, parentOpacity);
                    } catch (RuntimeException var12) {
                        c.outline(r[0], r[1], r[2], r[3], -1023878);
                    }
                }
            }
        }
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        super.paintOver(c, mx, my, dt);
        if (this.paintAfter) {
            this.drawCarried(c);
        }
    }

    private void drawElement(Canvas c, GuiDoc.Element e, float hover, float dt, float parentOpacity) {
        int[] r = this.rect(e);
        float opacity = Math.max(0.0F, Math.min(1.0F, e.f("opacity"))) * parentOpacity;
        if (!e.b("visible") && !this.hoverOpen.contains(e.id)) {
            opacity *= 0.3F;
        }

        String appear = e.get("appear");
        float dx = 0.0F;
        float dy = 0.0F;
        float scale = 1.0F;
        float rot = e.f("rotation");
        float rotX = e.f("rotX");
        float rotY = e.f("rotY");
        if (!appear.equals("none")) {
            float dur = Math.max(0.01F, e.f("appearTime"));
            float t = (this.time() - e.f("appearDelay")) / dur;
            if (t <= 0.0F && this.playAppear) {
                return;
            }

            GuiMotion.Frame m = GuiMotion.appear(appear, t, e.get("appearEase"));
            dx += m.dx;
            dy += m.dy;
            scale *= m.scale;
            rot += m.rotZ;
            rotX += m.rotX;
            rotY += m.rotY;
            opacity *= m.opacity;
        }

        String loop = e.get("loop");
        if (!loop.equals("none") && this.playAppear) {
            GuiMotion.Frame m = GuiMotion.loop(loop, this.time(), e.f("loopSpeed"), e.f("loopPower"));
            dx += m.dx;
            dy += m.dy;
            scale *= m.scale;
            rot += m.rotZ;
            rotX += m.rotX;
            rotY += m.rotY;
            opacity *= m.opacity;
        }

        if (this.hoverFollowing.contains(e.id)) {
            dx += Math.min(this.viewX1 - r[2] - 2.0F, this.toDesignX(this.mouseX) + 10.0F) - r[0];
            dy += Math.min(this.viewY1 - r[3] - 2.0F, this.toDesignY(this.mouseY) + 8.0F) - r[1];
        }

        float depthX = e.f("parallax");
        float depthY = e.f("parallaxY");
        if ((depthX != 0.0F || depthY != 0.0F) && this.w > 0 && this.h > 0) {
            dx += depthX * Math.max(-1.0F, Math.min(1.0F, (this.mouseX - (this.x + this.w / 2.0F)) / (this.w / 2.0F)));
            dy += depthY * Math.max(-1.0F, Math.min(1.0F, (this.mouseY - (this.y + this.h / 2.0F)) / (this.h / 2.0F)));
        }

        if (hover > 0.0F) {
            float hs = e.f("hoverScale");
            if (hs > 0.0F) {
                scale *= 1.0F + (hs - 1.0F) * hover;
            }

            opacity *= 1.0F + (e.f("hoverOpacity") - 1.0F) * hover;
            dx += e.f("hoverX") * hover;
            dy += e.f("hoverY") * hover;
            rot += e.f("hoverRotation") * hover;
            rotX += e.f("hoverRotX") * hover;
            rotY += e.f("hoverRotY") * hover;
            float tilt = e.f("hoverTilt");
            if (tilt != 0.0F && r[2] > 0 && r[3] > 0) {
                float[] lean = GuiMotion.tilt(
                    (this.toDesignX(this.mouseX) - r[0] - r[2] / 2.0F) / (r[2] / 2.0F),
                    (this.toDesignY(this.mouseY) - r[1] - r[3] / 2.0F) / (r[3] / 2.0F),
                    tilt
                );
                rotX += lean[0] * hover;
                rotY += lean[1] * hover;
            }
        }

        float pressTarget = this.interactive && e.id.equals(this.pressedId) && e.id.equals(this.hoveredId) ? 1.0F : 0.0F;
        float pressed = this.pressAnim.getOrDefault(e.id, 0.0F);
        if (pressed != pressTarget) {
            pressed += (pressTarget - pressed) * Math.min(1.0F, dt * 24.0F);
            if (Math.abs(pressed - pressTarget) < 0.01F) {
                pressed = pressTarget;
            }

            this.pressAnim.put(e.id, pressed);
        }

        if (pressed > 0.0F) {
            scale *= 1.0F + (e.f("pressScale") - 1.0F) * pressed;
        }

        if (e.id.equals(this.dragHeld)) {
            scale *= e.f("carryScale");
            opacity *= Math.max(0.0F, Math.min(1.0F, e.f("carryOpacity")));
        }

        if (!(opacity <= 0.01F)) {
            float glow = e.f("glow") + (e.f("hoverGlow") - e.f("glow")) * (e.f("hoverGlow") > 0.0F ? hover : 0.0F);
            this.drawnHover = hover;
            boolean turned3d = rotX != 0.0F || rotY != 0.0F;
            boolean transformed = dx != 0.0F || dy != 0.0F || scale != 1.0F || rot != 0.0F || turned3d;
            float cx = r[0] + r[2] * e.f("pivotX");
            float cy = r[1] + r[3] * e.f("pivotY");
            if (turned3d) {
                begin3d(c, cx + dx, cy + dy, e.f("perspective"));
            }

            try {
                if (transformed) {
                    c.g.pose().pushPose();
                    c.g.pose().translate(cx + dx, cy + dy, 0.0F);
                    if (rot != 0.0F) {
                        Mc.mulPose(c.g.pose(), Axes.ZP.rotationDegrees(rot));
                    }

                    if (rotY != 0.0F) {
                        Mc.mulPose(c.g.pose(), Axes.YP.rotationDegrees(rotY));
                    }

                    if (rotX != 0.0F) {
                        Mc.mulPose(c.g.pose(), Axes.XP.rotationDegrees(rotX));
                    }

                    c.g.pose().scale(scale, scale, 1.0F);
                    c.g.pose().translate(-cx, -cy, 0.0F);
                }

                try {
                    if (glow > 0.0F) {
                        SmoothShapes.halo(c, r[0], r[1], r[2], r[3], e.i("radius"), Math.round(glow), e.color("glowColor"), opacity, 0);
                    }

                    this.drawBody(c, e, r, opacity, hover, dt);
                } finally {
                    if (transformed) {
                        c.g.pose().popPose();
                    }
                }
            } finally {
                if (turned3d) {
                    end3d(c);
                }
            }
        }
    }

    private static void begin3d(Canvas c, float cx, float cy, float perspective) {
        c.flush();
        RenderSystem.clear(256, Minecraft.ON_OSX);
        Vector3f at = Mc.pose(c.g.pose().last()).transformPosition(cx, cy, 0.0F, new Vector3f());
        PoseStack stack = RenderSystem.getModelViewStack();
        stack.pushPose();
        Mc.mulPoseMatrix(stack, new Matrix4f().translate(at.x, at.y, 0.0F).m23(-1.0F / Math.max(50.0F, perspective)).translate(-at.x, -at.y, 0.0F));
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableCull();
    }

    private static void end3d(Canvas c) {
        c.flush();
        RenderSystem.enableCull();
        RenderSystem.getModelViewStack().popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(256, Minecraft.ON_OSX);
    }

    private void drawBody(Canvas c, GuiDoc.Element e, int[] r, float opacity, float hover, float dt) {
        if (!e.type.equals("button")) {
            this.background(c, e, r, e.get("backgroundImage"), opacity);
        }

        switch (e.type) {
            case "panel":
                this.drawPanel(c, e, r, opacity);
                break;
            case "image":
                this.drawImage(c, e, r, opacity);
                break;
            case "text":
                this.drawText(c, e, r, opacity);
                break;
            case "button":
                this.drawButton(c, e, r, opacity, hover);
                break;
            case "entity":
                this.drawEntity(c, e, r, opacity);
                break;
            case "item":
                this.drawItem(c, e, r, opacity);
                break;
            case "slot":
                this.drawSlot(c, e, r, opacity, hover);
                break;
            case "inventory":
                this.drawInventory(c, e, r, opacity);
                break;
            case "progress":
                this.drawProgress(c, e, r, opacity, dt);
                break;
            case "input":
                this.drawInput(c, e, r, opacity);
                break;
            case "clip":
            case "scroll":
                this.drawZone(c, e, r, opacity);
                break;
            case "drop_zone":
                this.drawZone(c, e, r, opacity);
                this.dropHint(c, e, r, opacity);
                if (!this.interactive) {
                    c.outline(r[0], r[1], r[2], r[3], 1430439935);
                }
                break;
            case "drag":
                if (!this.interactive) {
                    c.outline(r[0], r[1], r[2], r[3], 1432214666);
                }
                break;
            case "hud_quests":
                this.drawQuests(c, e, r, opacity);
                break;
            case "hud_skill_abilities":
            case "hud_skill_level":
                SkillsClient.part(c, e, r);
                break;
            case "choice_answers":
                this.drawAnswers(c, e, r, opacity);
                break;
            case "speaker":
                if (!this.noSpeaker) {
                    this.drawSpeaker(c, e, r, opacity);
                }
                break;
            case "speech_icon":
                this.drawSpeechIcon(c, e, r, opacity);
                break;
            case "speech_plate":
                this.drawSpeechPlate(c, e, r, opacity);
                break;
            case "speech_text":
                this.drawSpeechText(c, e, r, opacity);
                break;
            case "menu_slots":
                c.flush();
                MenuLook.drawSlots(c.g, e.color("color"), e.color("borderColor"), opacity);
                c.flush();
                break;
            case "chat_lines":
                this.drawChatLines(c, e, r, opacity);
                break;
            case "menu_widget":
                if (!this.interactive && e.get("baseText").startsWith("Copyright")) {
                    c.g.drawString(c.font, e.get("text"), r[0], r[1] + 1, a(-1, opacity));
                } else if (!this.interactive) {
                    gameButton(c, r, e.b("enabled") ? 66 : 46, opacity);
                    c.g.drawCenteredString(c.font, e.get("text"), r[0] + r[2] / 2, r[1] + (r[3] - 9) / 2 + 1, a(-2039584, opacity));
                }
                break;
            case "quest_icon":
                this.drawQuestIcon(c, e, r, opacity);
                break;
            default:
                if (GuiSchema.isHud(e.type)) {
                    this.drawHud(c, e, r, opacity);
                } else if (e.type.startsWith("journal_")) {
                    JournalParts.draw(this, c, e, r, opacity);
                } else if (e.type.startsWith("shop_")) {
                    ShopParts.draw(this, c, e, r, opacity);
                } else if (e.type.startsWith("skill_")) {
                    SkillParts.draw(this, c, e, r);
                } else {
                    AddonClient.GuiPart addon = AddonClient.part(e.type);
                    if (addon != null) {
                        addon.draw(c, e, r, opacity, hover, this);
                    }
                }
        }

        if (e.isContainer() && !e.children.isEmpty()) {
            boolean clips = !GuiSchema.isLoose(e.type);
            if (clips) {
                c.pushClip(r[0], r[1], r[2], r[3]);
            }

            if (e.type.equals("scroll")) {
                this.animateScroll(e, dt);
            }

            this.drawList(c, e.children, opacity, dt);
            if (clips) {
                c.popClip();
            }
        }

        if (e.type.equals("scroll")) {
            this.drawScrollbars(c, e, r, opacity);
        }
    }

    private void drawZone(Canvas c, GuiDoc.Element e, int[] r, float op) {
        if (e.get("backgroundImage").isBlank()) {
            int bg = e.color("background");
            int bw = e.i("borderWidth");
            if (bg >>> 24 != 0 || bw > 0) {
                this.box(c, r[0], r[1], r[2], r[3], e.i("radius"), a(bg, op), a(e.color("borderColor"), op), bw);
            }

            if (!this.interactive && e.type.equals("clip")) {
                c.outline(r[0], r[1], r[2], r[3], 872401771);
            }
        }
    }

    private void animateScroll(GuiDoc.Element e, float dt) {
        float[] s = this.scrolls.get(e.id);
        if (s != null) {
            if (!e.b("smooth")) {
                s[0] = s[2];
                s[1] = s[3];
            } else {
                float k = Math.min(1.0F, dt * 16.0F);
                s[0] += (s[2] - s[0]) * k;
                s[1] += (s[3] - s[1]) * k;
                if (Math.abs(s[2] - s[0]) < 0.3F) {
                    s[0] = s[2];
                }

                if (Math.abs(s[3] - s[1]) < 0.3F) {
                    s[1] = s[3];
                }
            }
        }
    }

    private int[] bars(GuiDoc.Element e, int[] r) {
        int[] content = this.contents.get(e);
        int[] out = new int[8];
        if (content != null && e.b("scrollbar")) {
            float[] s = this.scrolls.getOrDefault(e.id, new float[4]);
            int bw = Math.max(2, e.i("barWidth"));
            if (content[1] > r[3]) {
                int track = r[3] - 4;
                int thumb = Math.max(12, track * r[3] / content[1]);
                int pos = Math.round((track - thumb) * s[1] / (content[1] - r[3]));
                out[0] = r[0] + r[2] - bw - 2;
                out[1] = r[1] + 2 + pos;
                out[2] = bw;
                out[3] = thumb;
            }

            if (content[0] > r[2]) {
                int track = r[2] - 4;
                int thumb = Math.max(12, track * r[2] / content[0]);
                int pos = Math.round((track - thumb) * s[0] / (content[0] - r[2]));
                out[4] = r[0] + 2 + pos;
                out[5] = r[1] + r[3] - bw - 2;
                out[6] = thumb;
                out[7] = bw;
            }

            return out;
        } else {
            return out;
        }
    }

    private void drawScrollbars(Canvas c, GuiDoc.Element e, int[] r, float op) {
        int[] b = this.bars(e, r);
        int col = a(e.color("barColor"), op);
        if (b[2] > 0) {
            c.round(b[0], b[1], b[2], b[3], b[2] / 2, col);
        }

        if (b[6] > 0) {
            c.round(b[4], b[5], b[6], b[7], b[7] / 2, col);
        }
    }

    public boolean scrollAt(float dx, float dy, double delta, boolean horizontal) {
        GuiDoc.Element e = this.pick(dx, dy, el -> el.type.equals("scroll") && this.contents.containsKey(el));
        if (e == null) {
            return false;
        } else {
            int[] content = this.contents.get(e);
            int[] r = this.rect(e);
            float[] s = this.scrolls.computeIfAbsent(e.id, k -> new float[4]);
            float step = (float)(delta * Math.max(4, e.i("speed")));
            boolean canV = content[1] > r[3];
            boolean canH = content[0] > r[2];
            if ((horizontal || !canV) && canH) {
                s[2] = Math.max(0.0F, Math.min((float)(content[0] - r[2]), s[2] - step));
            } else {
                if (!canV) {
                    return false;
                }

                s[3] = Math.max(0.0F, Math.min((float)(content[1] - r[3]), s[3] - step));
            }

            return true;
        }
    }

    private static int a(int color, float opacity) {
        return Theme.alpha(color, opacity);
    }

    private boolean smoothCorners() {
        return !this.doc.get("corners").equals("pixel");
    }

    private void round(Canvas c, int x, int y, int w, int h, int radius, int color) {
        if (this.smoothCorners()) {
            SmoothShapes.round(c, x, y, w, h, radius, color);
        } else {
            c.round(x, y, w, h, radius, color);
        }
    }

    private void box(Canvas c, int x, int y, int w, int h, int radius, int fill, int border, int bw) {
        if (bw > 0 && border >>> 24 != 0) {
            this.round(c, x, y, w, h, radius, border);
            this.round(c, x + bw, y + bw, w - 2 * bw, h - 2 * bw, Math.max(0, radius - bw), fill);
        } else {
            this.round(c, x, y, w, h, radius, fill);
        }
    }

    private int hovered(GuiDoc.Element e, String colorKey, String hoverKey) {
        int base = e.color(colorKey);
        int other = e.color(hoverKey);
        return other >>> 24 == 0 ? base : Theme.lerp(base, other, this.drawnHover);
    }

    private static void nineParts(Canvas c, ResourceLocation rl, int sheetW, int sheetH, int u, int v, int uw, int vh, int edge, int[] r, float op) {
        int e = Math.min(edge, Math.min(r[2], r[3]) / 2);
        if (e > 0) {
            int[] xs = new int[]{r[0], r[0] + e, r[0] + r[2] - e};
            int[] ws = new int[]{e, r[2] - 2 * e, e};
            int[] ys = new int[]{r[1], r[1] + e, r[1] + r[3] - e};
            int[] hs = new int[]{e, r[3] - 2 * e, e};
            float[] us = new float[]{u, u + edge, u + uw - edge, u + uw};
            float[] vs = new float[]{v, v + edge, v + vh - edge, v + vh};

            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (ws[col] > 0 && hs[row] > 0) {
                        blit(c, rl, xs[col], ys[row], ws[col], hs[row], us[col] / sheetW, vs[row] / sheetH, us[col + 1] / sheetW, vs[row + 1] / sheetH, -1, op);
                    }
                }
            }
        }
    }

    private static void gameButton(Canvas c, int[] r, int row, float op) {
        nineParts(c, MC_WIDGETS, 256, 256, 0, row, 200, 20, 3, r, op);
    }

    private static void gameStone(Canvas c, int[] r, float op) {
        c.pushClip(r[0], r[1], r[2], r[3]);

        for (int yy = r[1]; yy < r[1] + r[3]; yy += 16) {
            for (int xx = r[0]; xx < r[0] + r[2]; xx += 16) {
                blit(c, MC_STONE, xx, yy, 16, 16, 0.0F, 0.0F, 1.0F, 1.0F, -12566464, op);
            }
        }

        c.flush();
        c.popClip();
        c.outline(r[0], r[1], r[2], r[3], a(-16777216, op));
    }

    private void drawPanel(Canvas c, GuiDoc.Element e, int[] r, float op) {
        if (e.get("backgroundImage").isBlank()) {
            String look = e.get("look");
            if (look.equals("minecraft")) {
                nineParts(c, MC_WINDOW, 256, 256, 0, 0, 248, 166, 4, r, op);
            } else if (look.equals("minecraft_dark")) {
                gameStone(c, r, op);
            } else {
                int radius = e.i("radius");
                int bw = e.i("borderWidth");
                int shadow = e.i("shadow");
                if (shadow > 0 && op > 0.4F) {
                    if (this.smoothCorners()) {
                        SmoothShapes.halo(c, r[0], r[1], r[2], r[3], radius, shadow, 1610612736, op, 2);
                    } else {
                        c.shadow(r[0], r[1], r[2], r[3], radius, shadow);
                    }
                }

                int col = a(this.hovered(e, "color", "hoverColor"), op);
                int col2 = a(e.color("color2"), op);
                String grad = e.get("gradient");
                this.box(c, r[0], r[1], r[2], r[3], radius, col, a(this.hovered(e, "borderColor", "hoverBorderColor"), op), bw);
                if (!grad.equals("none")) {
                    int inset = Math.max(bw, 0);
                    int gx = r[0] + inset;
                    int gy = r[1] + inset;
                    int gw = r[2] - 2 * inset;
                    int gh = r[3] - 2 * inset;
                    if (grad.equals("vertical")) {
                        int ry = Math.min(radius, gh / 2);
                        c.gradient(gx, gy + ry, gw, gh - 2 * ry, col, col2);
                        this.round(c, gx, gy + gh - 2 * ry, gw, 2 * ry, ry, col2);
                        c.fill(gx, gy + gh - 2 * ry, gw, ry, col2);
                    } else {
                        int steps = Math.max(1, Math.min(64, gw));
                        int rx = Math.min(radius, gw / 2);

                        for (int i = 0; i < steps; i++) {
                            int sx = gx + rx + (gw - 2 * rx) * i / steps;
                            int ex = gx + rx + (gw - 2 * rx) * (i + 1) / steps;
                            c.fill(sx, gy, ex - sx, gh, Theme.lerp(col, col2, (float)i / steps));
                        }

                        this.round(c, gx + gw - 2 * rx, gy, 2 * rx, gh, rx, col2);
                        c.fill(gx + gw - 2 * rx, gy, rx, gh, col2);
                    }
                }
            }
        }
    }

    private boolean background(Canvas c, GuiDoc.Element e, int[] r, String image, float opacity) {
        GuiView.Tex t = this.texture(image);
        if (t == null) {
            return false;
        } else {
            this.drawSkin(c, e, r, t, opacity);
            return true;
        }
    }

    private void drawSkin(Canvas c, GuiDoc.Element e, int[] r, GuiView.Tex t, float opacity) {
        EngineConfig.Skin skin = new EngineConfig.Skin();
        skin.nine = e.b("nineSlice");
        skin.left = e.i("sliceLeft");
        skin.top = e.i("sliceTop");
        skin.right = e.i("sliceRight");
        skin.bottom = e.i("sliceBottom");
        skin.scale = e.f("sliceScale");
        skin.tileCenter = e.b("sliceTileCenter");
        skin.tileEdges = e.b("sliceTileEdges");
        c.flush();
        int tint = e.type.equals("image") ? this.hovered(e, "tint", "hoverTint") : -1;
        RenderSystem.setShaderColor((tint >> 16 & 0xFF) / 255.0F, (tint >> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, (tint >>> 24) / 255.0F * opacity);

        try {
            Skins.draw(c.g, t.rl, skin, r[0], r[1], r[2], r[3]);
            c.flush();
        } finally {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private GuiView.Tex texture(String value) {
        if (value != null && !value.isBlank()) {
            String v = value.trim();
            if (v.contains(":")) {
                ResourceLocation rl = ResourceLocation.tryParse(v);
                if (rl == null) {
                    return null;
                } else {
                    int[] size = Skins.size(rl);
                    return new GuiView.Tex(rl, size[0], size[1]);
                }
            } else {
                if (this.localProject != null) {
                    Project.ImageRef ref = this.localProject.image(v);
                    if (ref != null) {
                        ProjectImages.Loaded l = ProjectImages.load(this.localProject, ref);
                        if (l != null) {
                            return new GuiView.Tex(l.texture(), l.width(), l.height());
                        }
                    }
                }

                int[] size = ProjectImages.size(Images.texture(this.project, v));
                return new GuiView.Tex(Images.texture(this.project, v), size[0], size[1]);
            }
        } else {
            return null;
        }
    }

    static void blit(Canvas c, ResourceLocation rl, int x, int y, int w, int h, float u0, float v0, float u1, float v1, int tint, float op) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor((tint >> 16 & 0xFF) / 255.0F, (tint >> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, (tint >>> 24) / 255.0F * op);
        int T = 4096;
        int uu = Math.round(u0 * 4096.0F);
        int vv = Math.round(v0 * 4096.0F);
        int uw = Math.round((u1 - u0) * 4096.0F);
        int vh = Math.round((v1 - v0) * 4096.0F);
        c.g.blit(rl, x, y, w, h, uu, vv, uw, vh, 4096, 4096);
        c.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawImage(Canvas c, GuiDoc.Element e, int[] r, float op) {
        GuiView.Tex t = this.texture(e.get("image"));
        GuiView.Tex other = this.drawnHover > 0.0F ? this.texture(e.get("hoverImage")) : null;
        if (other != null) {
            float[] mix = GuiMotion.crossfade(this.drawnHover);
            if (t != null && mix[0] > 0.0F) {
                this.picture(c, e, r, t, op * mix[0]);
            }

            if (mix[1] > 0.0F) {
                this.picture(c, e, r, other, op * mix[1]);
            }
        } else if (t == null) {
            if (!this.interactive && e.get("backgroundImage").isBlank()) {
                c.outline(r[0], r[1], r[2], r[3], 1442840575);
                c.textCentered("картинка", r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, -1996488705);
            }
        } else {
            this.picture(c, e, r, t, op);
        }
    }

    private void picture(Canvas c, GuiDoc.Element e, int[] r, GuiView.Tex t, float op) {
        boolean smooth = e.b("smooth");
        AbstractTexture texture = smooth ? Minecraft.getInstance().getTextureManager().getTexture(t.rl) : null;
        if (texture != null) {
            texture.setFilter(true, false);
        }

        try {
            this.placed(c, e, r, t, op);
        } finally {
            if (texture != null) {
                texture.setFilter(false, false);
            }
        }
    }

    private void placed(Canvas c, GuiDoc.Element e, int[] r, GuiView.Tex t, float op) {
        if (e.b("nineSlice")) {
            this.drawSkin(c, e, r, t, op);
        } else {
            float u0 = e.f("u0");
            float v0 = e.f("v0");
            float u1 = e.f("u1");
            float v1 = e.f("v1");
            if (e.b("flipX")) {
                float tmp = u0;
                u0 = u1;
                u1 = tmp;
            }

            if (e.b("flipY")) {
                float tmp = v0;
                v0 = v1;
                v1 = tmp;
            }

            int tint = this.hovered(e, "tint", "hoverTint");
            String fit = e.get("fit");
            int x = r[0];
            int y = r[1];
            int w = r[2];
            int h = r[3];
            if (t.w <= 0 || t.h <= 0 || !fit.equals("contain") && !fit.equals("cover")) {
                if (fit.equals("tile")) {
                    int tw = t.w > 0 ? t.w : 16;
                    int th = t.h > 0 ? t.h : 16;
                    c.pushClip(x, y, w, h);

                    for (int yy = y; yy < y + h; yy += th) {
                        for (int xx = x; xx < x + w; xx += tw) {
                            blit(c, t.rl, xx, yy, tw, th, u0, v0, u1, v1, tint, op);
                        }
                    }

                    c.popClip();
                } else {
                    blit(c, t.rl, x, y, w, h, u0, v0, u1, v1, tint, op);
                }
            } else {
                float iw = t.w * Math.abs(u1 - u0);
                float ih = t.h * Math.abs(v1 - v0);
                float k = fit.equals("contain") ? Math.min(w / iw, h / ih) : Math.max(w / iw, h / ih);
                int dw = Math.round(iw * k);
                int dh = Math.round(ih * k);
                if (fit.equals("cover")) {
                    c.pushClip(x, y, w, h);
                }

                blit(c, t.rl, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, u0, v0, u1, v1, tint, op);
                if (fit.equals("cover")) {
                    c.popClip();
                }
            }
        }
    }

    private MutableComponent styled(GuiDoc.Element e, String text) {
        MutableComponent comp = Component.literal(text);
        Style s = Style.EMPTY.withBold(e.b("bold")).withItalic(e.b("italic")).withUnderlined(e.b("underline")).withStrikethrough(e.b("strike"));
        return comp.withStyle(s);
    }

    private String visibleText(GuiDoc.Element e, String full) {
        full = this.fillHud(full);
        float speed = e.f("typewriter");
        if (!(speed <= 0.0F) && this.playAppear) {
            long from = this.textChangedAt.getOrDefault(e.id, this.openedAt);
            float secs = (float)(System.nanoTime() - from) / 1.0E9F - (this.textChangedAt.containsKey(e.id) ? 0.0F : e.f("appearDelay"));
            int n = (int)Math.max(0.0F, secs * speed);
            return n >= full.length() ? full : full.substring(0, n);
        } else {
            return full;
        }
    }

    private float shrunk(Canvas c, GuiDoc.Element e, int[] r, int pad, float scale) {
        String whole = this.fillHud(unescape(e.get("text")));
        int lh = 9 + e.i("lineSpacing");

        for (float s = scale; s > scale / 2.0F; s -= scale / 20.0F) {
            int width = Math.max(1, Math.round((r[2] - 2 * pad) / s));
            int lines = 0;

            for (String para : whole.split("\n", -1)) {
                lines += Math.max(1, c.font.split(this.styled(e, para), width).size());
            }

            if (lines * lh * s <= r[3] - 2 * pad) {
                return s;
            }
        }

        return scale / 2.0F;
    }

    private void drawText(Canvas c, GuiDoc.Element e, int[] r, float op) {
        int bgc = e.color("background");
        if (bgc >>> 24 != 0) {
            c.round(r[0], r[1], r[2], r[3], 3, a(bgc, op));
        }

        int pad = e.i("padding");
        float scale = Math.max(0.1F, e.f("scale"));
        if (e.b("shrink")) {
            scale = this.shrunk(c, e, r, pad, scale);
        }

        String text = this.visibleText(e, unescape(e.get("text")));
        if (!SmoothText.usable(e.get("font")) || !this.smoothText(c, e, r, op, text, pad, scale)) {
            int innerW = Math.max(1, Math.round((r[2] - 2 * pad) / scale));
            List<FormattedCharSequence> lines = new ArrayList<>();

            for (String para : text.split("\n", -1)) {
                if (e.b("wrap")) {
                    List<FormattedCharSequence> split = c.font.split(this.styled(e, para), innerW);
                    if (split.isEmpty()) {
                        lines.add(FormattedCharSequence.EMPTY);
                    } else {
                        lines.addAll(split);
                    }
                } else {
                    lines.add(this.styled(e, para).getVisualOrderText());
                }
            }

            int lh = 9 + e.i("lineSpacing");
            float innerH = (r[3] - 2 * pad) / scale;
            String overflow = e.get("overflow");
            if (overflow.equals("ellipsis")) {
                int fit = Math.max(1, (int)Math.floor((innerH + e.i("lineSpacing")) / lh));
                boolean cut = lines.size() > fit;

                while (lines.size() > fit) {
                    lines.remove(lines.size() - 1);
                }

                for (int i = 0; i < lines.size(); i++) {
                    boolean last = i == lines.size() - 1;
                    if (c.font.width(lines.get(i)) > innerW || last && cut) {
                        String plain = plain(lines.get(i));
                        String shortened = c.font.plainSubstrByWidth(plain, Math.max(0, innerW - c.font.width("…")));
                        lines.set(i, this.styled(e, shortened.stripTrailing() + "…").getVisualOrderText());
                    }
                }
            }

            int total = lines.size() * lh;
            String va = e.get("valign");
            String al = e.get("align");
            float top = va.equals("middle") ? (innerH - total) / 2.0F : (va.equals("bottom") ? innerH - total : 0.0F);
            int color = a(this.textColor(e), op);
            if (color >>> 24 >= 5) {
                boolean clip = !overflow.equals("visible");
                if (clip) {
                    c.pushClip(r[0], r[1], r[2], r[3]);
                }

                c.g.pose().pushPose();
                c.g.pose().translate((float)(r[0] + pad), (float)(r[1] + pad), 0.0F);
                c.g.pose().scale(scale, scale, 1.0F);
                int yy = Math.round(top);

                for (FormattedCharSequence line : lines) {
                    int lw = c.font.width(line);
                    int lx = al.equals("center") ? (innerW - lw) / 2 : (al.equals("right") ? innerW - lw : 0);
                    c.g.drawString(c.font, line, lx, yy, color, e.b("shadow"));
                    yy += lh;
                }

                c.g.pose().popPose();
                if (clip) {
                    c.flush();
                    c.popClip();
                }
            }
        }
    }

    private int textColor(GuiDoc.Element e) {
        return e.b("speakerColor") && this.speakerColor >= 0 ? 0xFF000000 | this.speakerColor : this.hovered(e, "color", "hoverColor");
    }

    private static SmoothText.Style fontStyle(GuiDoc.Element e, String text, int px) {
        return new SmoothText.Style(e.get("font"), e.b("bold"), e.b("italic") || text.startsWith("§o"), e.b("underline"), e.b("strike"), px);
    }

    private float realPixels() {
        return this.areaScale * (float)Minecraft.getInstance().getWindow().getGuiScale();
    }

    private static void smoothLine(Canvas c, SmoothText.Line line, float x, float y, float perUnit, int color, boolean shadow) {
        if (line != null) {
            c.g.pose().pushPose();
            c.g.pose().translate(x, y, 0.0F);
            c.g.pose().scale(1.0F / perUnit, 1.0F / perUnit, 1.0F);
            if (shadow) {
                int shift = Math.max(1, Math.round(perUnit * 0.6F));
                blit(c, line.texture(), shift, shift, line.w(), line.h(), 0.0F, 0.0F, 1.0F, 1.0F, Math.round((color >>> 24) * 0.35F) << 24, 1.0F);
            }

            blit(c, line.texture(), 0, 0, line.w(), line.h(), 0.0F, 0.0F, 1.0F, 1.0F, color, 1.0F);
            c.g.pose().popPose();
        }
    }

    private boolean smoothText(Canvas c, GuiDoc.Element e, int[] r, float op, String text, int pad, float scale) {
        int px = SmoothText.px(9.0F * scale * this.realPixels());
        float perUnit = px / (9.0F * scale);
        SmoothText.Style style = fontStyle(e, text, px);
        String plain = SmoothText.plain(text);
        int widthPx = Math.round((r[2] - 2 * pad) * perUnit);
        int lineHeight = SmoothText.lineHeight(style);
        if (lineHeight <= 0) {
            return false;
        } else {
            List<String> lines = new ArrayList<>();

            for (String para : plain.split("\n", -1)) {
                if (e.b("wrap")) {
                    lines.addAll(SmoothText.wrap(para, widthPx, style));
                } else {
                    lines.add(para);
                }
            }

            if (!SmoothText.usable(e.get("font"))) {
                return false;
            } else {
                float lh = lineHeight / perUnit + e.i("lineSpacing") * scale;
                float total = lines.size() * lh;
                float innerH = r[3] - 2 * pad;
                float innerW = r[2] - 2 * pad;
                String va = e.get("valign");
                String al = e.get("align");
                float top = va.equals("middle") ? (innerH - total) / 2.0F : (va.equals("bottom") ? innerH - total : 0.0F);
                int color = a(this.textColor(e), op);
                if (color >>> 24 < 5) {
                    return true;
                } else {
                    boolean clip = !e.get("overflow").equals("visible");
                    if (clip) {
                        c.pushClip(r[0], r[1], r[2], r[3]);
                    }

                    float yy = r[1] + pad + top;

                    for (String line : lines) {
                        SmoothText.Line tex = SmoothText.line(line, style);
                        float lw = tex != null ? tex.w() / perUnit : 0.0F;
                        float lx = al.equals("center") ? (innerW - lw) / 2.0F : (al.equals("right") ? innerW - lw : 0.0F);
                        smoothLine(c, tex, r[0] + pad + lx, yy, perUnit, color, e.b("shadow"));
                        yy += lh;
                    }

                    if (clip) {
                        c.flush();
                        c.popClip();
                    }

                    return true;
                }
            }
        }
    }

    private static String plain(FormattedCharSequence line) {
        StringBuilder sb = new StringBuilder();
        line.accept((i, style, cp) -> {
            sb.appendCodePoint(cp);
            return true;
        });
        return sb.toString();
    }

    private void drawButton(Canvas c, GuiDoc.Element e, int[] r, float op, float hover) {
        boolean enabled = e.b("enabled");
        boolean pressed = this.interactive && e.id.equals(this.pressedId) && e.id.equals(this.hoveredId);
        boolean game = e.get("look").equals("minecraft");
        String bgImage = e.get("backgroundImage");
        String stateImage = e.get(!enabled ? "buttonDisabledImage" : (pressed ? "buttonPressImage" : "buttonHoverImage"));
        if ((!enabled || pressed || hover > 0.5F) && !stateImage.isBlank()) {
            bgImage = stateImage;
        }

        if (!this.background(c, e, r, bgImage, op)) {
            if (game) {
                gameButton(c, r, !enabled ? 46 : (hover > 0.5F ? 86 : 66), op);
            } else {
                int fill = !enabled ? e.color("disabledColor") : (pressed ? e.color("pressColor") : Theme.lerp(e.color("color"), e.color("hoverColor"), hover));
                int border = enabled ? Theme.lerp(e.color("borderColor"), e.color("hoverBorderColor"), hover) : e.color("borderColor");
                this.box(c, r[0], r[1], r[2], r[3], e.i("radius"), a(fill, op), a(border, op), e.i("borderWidth"));
            }
        }

        float ts = Math.max(0.1F, e.f("textScale"));
        String text = this.fillHud(unescape(e.get("text")));
        GuiView.Tex icon = this.texture(e.get("icon"));
        int is = icon != null ? e.i("iconSize") : 0;
        boolean soft = SmoothText.usable(e.get("font"));
        int softPx = soft ? SmoothText.px(9.0F * ts * this.realPixels()) : 0;
        float softUnit = soft ? softPx / (9.0F * ts) : 1.0F;
        SmoothText.Style softStyle = soft ? new SmoothText.Style(e.get("font"), false, false, false, false, softPx) : null;
        int softW = soft ? SmoothText.width(text, softStyle) : -1;
        soft = soft && softW >= 0;
        int tw = soft ? Math.round(softW / softUnit) : Math.round(c.font.width(text) * ts);
        int gap = is > 0 && !text.isEmpty() ? 4 : 0;
        int total = is + gap + tw;
        int sx = r[0] + (r[2] - total) / 2;
        if (icon != null) {
            blit(c, icon.rl, sx, r[1] + (r[3] - is) / 2, is, is, 0.0F, 0.0F, 1.0F, 1.0F, -1, enabled ? op : op * 0.5F);
        }

        int tc = a(game && enabled ? Theme.lerp(e.color("textColor"), -96, hover) : e.color("textColor"), enabled ? op : op * 0.5F);
        if (tc >>> 24 >= 5 && !text.isEmpty() && soft) {
            float tall = SmoothText.lineHeight(softStyle) / softUnit;
            smoothLine(c, SmoothText.line(text, softStyle), sx + is + gap, r[1] + (r[3] - tall) / 2.0F + (pressed ? 1 : 0), softUnit, tc, true);
        } else if (tc >>> 24 >= 5 && !text.isEmpty()) {
            c.g.pose().pushPose();
            c.g.pose().translate((float)(sx + is + gap), r[1] + (r[3] - 9.0F * ts) / 2.0F + (pressed ? 1 : 0), 0.0F);
            c.g.pose().scale(ts, ts, 1.0F);
            c.g.drawString(c.font, text, 0, 0, tc, true);
            c.g.pose().popPose();
        }
    }

    private void drawEntity(Canvas c, GuiDoc.Element e, int[] r, float op) {
        int bgc = e.color("background");
        if (bgc >>> 24 != 0) {
            c.round(r[0], r[1], r[2], r[3], 4, a(bgc, op));
        }

        String type = e.get("entity");
        EntityPreview p = this.entities.get(e.id);
        if (p == null || !p.typeId().equals(type)) {
            p = new EntityPreview(type);
            this.entities.put(e.id, p);
        }

        p.animate(e.get("anim"), e.get("animLayer"), e.b("animLoop"), e.f("animSpeed"));
        float lookX = 0.0F;
        float lookY = 0.0F;
        if (e.b("followMouse")) {
            float mxD = this.toDesignX(this.mouseX);
            float myD = this.toDesignY(this.mouseY);
            lookX = (float)Math.atan((r[0] + r[2] / 2.0F - mxD) / 40.0F);
            lookY = (float)Math.atan((r[1] + r[3] * 0.3F - myD) / 40.0F);
        }

        float yaw = e.f("yaw") + e.f("spin") * this.time();
        c.pushClip(r[0], r[1], r[2], r[3]);
        p.render(c, r[0] + r[2] / 2.0F, r[1] + r[3] - 2, r[2], r[3], Math.max(0.05F, e.f("scale")), yaw, e.f("pitch"), lookX, lookY);
        c.popClip();
        if (p.entity() == null && !this.interactive) {
            c.outline(r[0], r[1], r[2], r[3], 1442840575);
            c.textCentered(p.error != null ? p.error : "войдите в мир", r[0] + r[2] / 2, r[1] + r[3] / 2 - 4, -1996488705);
        }
    }

    private void drawItem(Canvas c, GuiDoc.Element e, int[] r, float op) {
        int bgc = e.color("background");
        if (bgc >>> 24 != 0) {
            c.round(r[0], r[1], r[2], r[3], 3, a(bgc, op));
        }

        ItemStack stack = stackOf(e.get("item"), e.i("count"));
        if (!stack.isEmpty()) {
            c.g.pose().pushPose();
            int size = e.i("itemSize") > 0 ? e.i("itemSize") : Math.min(r[2], r[3]);
            c.g.pose().translate(r[0] + (r[2] - size) / 2.0F, r[1] + (r[3] - size) / 2.0F, 0.0F);
            c.g.pose().scale(size / 16.0F, size / 16.0F, 1.0F);
            c.g.renderItem(stack, 0, 0);
            if (e.b("showCount") && stack.getCount() > 1) {
                c.g.renderItemDecorations(c.font, stack, 0, 0);
            }

            c.g.pose().popPose();
        }
    }

    private static ItemStack stackOf(String id, int count) {
        return id != null && !id.isBlank() ? ItemSpec.stack(id, count) : ItemStack.EMPTY;
    }

    private void cell(Canvas c, GuiDoc.Element e, int x, int y, int s, float op, boolean hover) {
        GuiView.Tex t = this.texture(e.get("texture"));
        if (t == null) {
            t = this.texture(e.get("backgroundImage"));
        }

        if (t != null) {
            this.drawSkin(c, e, new int[]{x, y, s, s}, t, op);
        } else {
            String var9 = e.get("style");
            switch (var9) {
                case "flat":
                    c.fill(x, y, s, s, a(e.color("background"), op));
                case "none":
                    break;
                default:
                    c.fill(x, y, s, s, a(-7631989, op));
                    c.fill(x, y, s - 1, 1, a(-13158601, op));
                    c.fill(x, y, 1, s - 1, a(-13158601, op));
                    c.fill(x + 1, y + s - 1, s - 1, 1, a(-1, op));
                    c.fill(x + s - 1, y + 1, 1, s - 1, a(-1, op));
            }
        }

        if (hover) {
            c.fill(x + 1, y + 1, s - 2, s - 2, e.color("hoverColor"));
        }
    }

    private void itemIn(Canvas c, ItemStack st, int x, int y, int s, boolean decorations) {
        this.itemIn(c, st, x, y, s, decorations, 0);
    }

    private void itemIn(Canvas c, ItemStack st, int x, int y, int s, boolean decorations, int itemSize) {
        if (!st.isEmpty()) {
            c.flush();
            float k = Math.max(0.0625F, (itemSize > 0 ? itemSize : s - 2) / 16.0F);
            c.g.pose().pushPose();
            c.g.pose().translate(x + (s - 16.0F * k) / 2.0F, y + (s - 16.0F * k) / 2.0F, 0.0F);
            c.g.pose().scale(k, k, 1.0F);
            c.g.renderItem(st, 0, 0);
            if (decorations) {
                c.g.renderItemDecorations(c.font, st, 0, 0);
            }

            c.g.pose().popPose();
        }
    }

    private void drawSlot(Canvas c, GuiDoc.Element e, int[] r, float op, float hover) {
        int s = Math.min(r[2], r[3]);
        int x = r[0] + (r[2] - s) / 2;
        int y = r[1] + (r[3] - s) / 2;
        this.cell(c, e, x, y, s, op, this.interactive && hover > 0.5F && !e.get("mode").equals("display"));
        ItemStack st = stackOf(e.get("item"), e.i("count"));
        if (st.isEmpty()) {
            ItemStack ghost = stackOf(e.get("ghost"), 1);
            if (!ghost.isEmpty()) {
                this.itemIn(c, ghost, x, y, s, false, e.i("itemSize"));
                c.fill(x + 1, y + 1, s - 2, s - 2, -1718908021);
            }
        } else {
            this.itemIn(c, st, x, y, s, true, e.i("itemSize"));
        }
    }

    private int[][] inventoryRows(GuiDoc.Element e) {
        String rows = e.get("rows");
        if (rows.equals("single")) {
            return new int[][]{{singleCell(e.get("slot"))}};
        } else if (rows.equals("armor")) {
            return new int[][]{{39}, {38}, {37}, {36}};
        } else if (rows.equals("offhand")) {
            return new int[][]{{40}};
        } else if (rows.equals("craft")) {
            int f = 2000;
            return new int[][]{{f, f + 1}, {f + 2, f + 3}};
        } else if (rows.equals("craft_result")) {
            return new int[][]{{2004}};
        } else {
            List<int[]> out = new ArrayList<>();
            if (!rows.equals("hotbar")) {
                for (int r = 0; r < 3; r++) {
                    int[] row = new int[9];

                    for (int i = 0; i < 9; i++) {
                        row[i] = 9 + r * 9 + i;
                    }

                    out.add(row);
                }
            }

            if (!rows.equals("main")) {
                int[] row = new int[9];
                int i = 0;

                while (i < 9) {
                    row[i] = i++;
                }

                out.add(row);
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (this.cellClick != null && rows.equals("all") && player != null) {
                int extra = PlayerSlots.extra(player);

                for (int at = 0; at < extra; at += 9) {
                    int[] row = new int[Math.min(9, extra - at)];

                    for (int i = 0; i < row.length; i++) {
                        row[i] = 1000 + at + i;
                    }

                    out.add(row);
                }
            }

            return out.toArray(new int[0][]);
        }
    }

    private static int singleCell(String slot) {
        String s = slot == null ? "" : slot.trim().toLowerCase(Locale.ROOT);

        return switch (s) {
            case "head", "голова" -> {
                byte var12 = 39;
                yield var12;
            }
            case "chest", "грудь" -> {
                byte var11 = 38;
                yield var11;
            }
            case "legs", "ноги" -> {
                byte var10 = 37;
                yield var10;
            }
            case "feet", "ступни" -> {
                byte var9 = 36;
                yield var9;
            }
            case "offhand", "вторая рука" -> {
                byte var8 = 40;
                yield var8;
            }
            default -> {
                int n;
                try {
                    n = Integer.parseInt(s);
                } catch (NumberFormatException var7) {
                    n = 1;
                }

                int var4 = Math.max(1, Math.min(36, n)) - 1;
                yield var4;
            }
        };
    }

    private static int rowGap(GuiDoc.Element e, int row) {
        return row >= 3 && e.get("rows").equals("all") ? 4 : 0;
    }

    private static ItemStack cellStack(Player player, int cell) {
        return cell < 1000 ? player.getInventory().getItem(cell) : player.inventoryMenu.getSlot(SlotPlan.menuSlot(cell)).getItem();
    }

    public ItemStack hoveredCellStack() {
        LocalPlayer player = Minecraft.getInstance().player;
        float dx = this.toDesignX(this.mouseX);
        float dy = this.toDesignY(this.mouseY);
        GuiDoc.Element e = this.pick(dx, dy, el -> el.type.equals("inventory"));
        if (e != null && player != null) {
            int cell = this.inventoryCell(e, dx, dy);
            return cell >= 0 && !cellLocked(cell) ? cellStack(player, cell) : ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
    }

    private int inventoryCell(GuiDoc.Element e, float dx, float dy) {
        int[] r = this.rect(e);
        int s = Math.max(4, e.i("cell"));
        int[][] rows = this.inventoryRows(e);

        for (int ri = 0; ri < rows.length; ri++) {
            int cy = r[1] + ri * s + rowGap(e, ri);

            for (int ci = 0; ci < rows[ri].length; ci++) {
                int cx = r[0] + ci * s;
                if (dx >= cx && dx < cx + s && dy >= cy && dy < cy + s) {
                    return rows[ri][ci];
                }
            }
        }

        return -1;
    }

    private static boolean cellLocked(int cell) {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && cell < 1000 && PlayerSlots.locked(player, cell);
    }

    private void drawInventory(Canvas c, GuiDoc.Element e, int[] r, float op) {
        LocalPlayer player = Minecraft.getInstance().player;
        int s = Math.max(4, e.i("cell"));
        int[][] rows = this.inventoryRows(e);
        int hover = !this.interactive || this.cellClick == null && !this.isHoveredDeep()
            ? -1
            : this.inventoryCell(e, this.toDesignX(this.mouseX), this.toDesignY(this.mouseY));

        for (int ri = 0; ri < rows.length; ri++) {
            int cy = r[1] + ri * s + rowGap(e, ri);

            for (int ci = 0; ci < rows[ri].length; ci++) {
                int idx = rows[ri][ci];
                int cx = r[0] + ci * s;
                boolean locked = cellLocked(idx);
                this.cell(c, e, cx, cy, s, op, idx == hover && !locked);
                if (player != null && idx != this.carried) {
                    this.itemIn(c, cellStack(player, idx), cx, cy, s, true, e.i("itemSize"));
                }

                if (locked) {
                    c.flush();
                    c.g.pose().pushPose();
                    c.g.pose().translate(0.0F, 0.0F, 200.0F);
                    SlotOverlay.lock(c.g, cx, cy, s);
                    c.g.pose().popPose();
                }
            }
        }
    }

    private void drawCarried(Canvas c) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (this.cellClick != null && player != null) {
            ItemStack held = player.inventoryMenu.getCarried();
            if (!held.isEmpty()) {
                c.pushLayer(400.0F);
                this.itemIn(c, held, this.mouseX - 9, this.mouseY - 9, 18, true);
                c.popLayer();
            }
        } else if (this.carried >= 0 && player != null) {
            ItemStack st = player.getInventory().getItem(this.carried);
            if (st.isEmpty()) {
                this.carried = -1;
            } else {
                c.pushLayer(400.0F);
                this.itemIn(c, st, this.mouseX - 9, this.mouseY - 9, 18, true);
                c.popLayer();
            }
        }
    }

    private boolean clickSlots(float dx, float dy, int button) {
        GuiDoc.Element e = this.pick(dx, dy, el -> el.type.equals("slot") || el.type.equals("inventory"));
        if (this.cellClick != null && e != null && e.type.equals("inventory")) {
            int idx = this.inventoryCell(e, dx, dy);
            if (idx >= 0 && !cellLocked(idx)) {
                this.cellClick.accept(idx, button);
            }

            return true;
        } else if (this.cellClick != null && e == null) {
            LocalPlayer holder = Minecraft.getInstance().player;
            if (holder != null && !holder.inventoryMenu.getCarried().isEmpty() && this.elementAt(dx, dy, false) == null) {
                this.cellClick.accept(-1, button);
                return true;
            } else {
                return false;
            }
        } else if (e == null) {
            if (this.carried >= 0) {
                this.carried = -1;
                return true;
            } else {
                return false;
            }
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (e.type.equals("inventory")) {
                int idx = this.inventoryCell(e, dx, dy);
                if (idx >= 0 && !cellLocked(idx) && idx < 1000) {
                    if (player != null && !player.getInventory().getItem(idx).isEmpty()) {
                        this.carried = this.carried == idx ? -1 : idx;
                    } else {
                        this.carried = -1;
                    }

                    return true;
                } else {
                    return true;
                }
            } else {
                String mode = e.get("mode");
                if (!mode.equals("display") && this.listener != null) {
                    int one = button == 1 ? 1 : 64;
                    if (this.carried >= 0) {
                        if (mode.equals("storage") || mode.equals("put")) {
                            this.listener.event(e.id, "slot_put:" + this.carried + ":" + one);
                        }

                        this.carried = -1;
                    } else if (!e.get("item").isBlank() && (mode.equals("storage") || mode.equals("take"))) {
                        this.listener.event(e.id, "slot_take:" + one);
                    }

                    return true;
                } else {
                    return true;
                }
            }
        }
    }

    private void drawProgress(Canvas c, GuiDoc.Element e, int[] r, float op, float dt) {
        String bind = e.get("bind");
        float target = Math.max(0.0F, Math.min(1.0F, !bind.isEmpty() && !bind.equals("none") ? this.hudFraction(bind) : e.f("value")));
        float v = this.shownValue.getOrDefault(e.id, target);
        float smooth = e.f("smooth");
        v = !(smooth <= 0.0F) && this.interactive ? v + (target - v) * Math.min(1.0F, dt * smooth) : target;
        this.shownValue.put(e.id, v);
        int radius = e.i("radius");
        int bw = e.i("borderWidth");
        if (e.get("backgroundImage").isBlank()) {
            this.box(c, r[0], r[1], r[2], r[3], radius, a(e.color("background"), op), a(e.color("borderColor"), op), bw);
        }

        int ix = r[0] + bw;
        int iy = r[1] + bw;
        int iw = r[2] - 2 * bw;
        int ih = r[3] - 2 * bw;
        String dir = e.get("direction");
        int fx = ix;
        int fy = iy;
        int fw = iw;
        int fh = ih;
        switch (dir) {
            case "left":
                fw = Math.round(iw * v);
                fx = ix + iw - fw;
                break;
            case "up":
                fh = Math.round(ih * v);
                fy = iy + ih - fh;
                break;
            case "down":
                fh = Math.round(ih * v);
                break;
            default:
                fw = Math.round(iw * v);
        }

        if (fw > 0 && fh > 0) {
            int col = a(e.color("color"), op);
            String c2 = e.get("color2");
            c.pushClip(fx, fy, fw, fh);
            if (c2.isBlank()) {
                c.round(ix, iy, iw, ih, Math.max(0, radius - bw), col);
            } else {
                int col2 = a(GuiDoc.toColor(c2), op);
                boolean vertical = dir.equals("up") || dir.equals("down");
                if (vertical) {
                    c.gradient(ix, iy, iw, ih, col2, col);
                } else {
                    int steps = Math.max(1, Math.min(48, iw));

                    for (int i = 0; i < steps; i++) {
                        int sx = ix + iw * i / steps;
                        int ex = ix + iw * (i + 1) / steps;
                        c.fill(sx, iy, ex - sx, ih, Theme.lerp(col, col2, (float)i / steps));
                    }
                }
            }

            c.popClip();
        }

        String label = e.get("label");
        if (!label.isBlank()) {
            String s = this.fillHud(unescape(label).replace("{p}", String.valueOf(Math.round(v * 100.0F))));
            c.g.drawString(c.font, s, r[0] + (r[2] - c.font.width(s)) / 2, r[1] + (r[3] - 8) / 2, a(e.color("textColor"), op), true);
        }
    }

    private void drawInput(Canvas c, GuiDoc.Element e, int[] r, float op) {
        boolean focused = this.interactive && e.id.equals(this.focusedInput);
        if (e.get("backgroundImage").isBlank()) {
            this.box(c, r[0], r[1], r[2], r[3], e.i("radius"), a(e.color("background"), op), a(focused ? e.color("focusColor") : e.color("borderColor"), op), 1);
        }

        String value = this.interactive ? this.inputs.getOrDefault(e.id, "") : e.get("text");
        c.pushClip(r[0] + 2, r[1], r[2] - 4, r[3]);
        int ty = r[1] + (r[3] - 8) / 2;
        if (value.isEmpty() && !focused) {
            c.text(e.get("placeholder"), r[0] + 5, ty, a(-9933435, op));
        } else {
            int tw = c.width(value);
            int shift = Math.max(0, tw - (r[2] - 12));
            c.text(value, r[0] + 5 - shift, ty, a(e.color("textColor"), op));
            if (focused && System.currentTimeMillis() / 500L % 2L == 0L) {
                c.fill(r[0] + 5 - shift + tw + 1, ty - 1, 1, 10, a(-1, op));
            }
        }

        c.popClip();
    }

    private void paintTooltip(Canvas c, String text, int mx, int my) {
        int w = 0;
        String[] lines = text.split("\n");

        for (String l : lines) {
            w = Math.max(w, c.width(l));
        }

        int h = lines.length * 10 + 6;
        int tx = Math.min(mx + 10, this.x + this.w - w - 12);
        int ty = Math.min(my + 12, this.y + this.h - h - 4);
        c.pushLayer(300.0F);
        c.roundBorder(tx, ty, w + 10, h, 3, -267382506, -10858545);

        for (int i = 0; i < lines.length; i++) {
            c.text(lines[i], tx + 5, ty + 4 + i * 10, -1841682);
        }

        c.popLayer();
    }

    @Override
    public void tick() {
        super.tick();

        for (EntityPreview p : this.entities.values()) {
            p.tick();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!this.interactive) {
            return false;
        } else {
            this.computeArea();
            return this.scrollAt(this.toDesignX(mx), this.toDesignY(my), delta, Screen.hasShiftDown());
        }
    }

    private void stepDrags() {
        long now = System.nanoTime();
        float step = this.dragStep == 0L ? 0.0F : Math.min(0.1F, (float)(now - this.dragStep) / 1.0E9F);
        this.dragStep = now;
        if (!this.dragOffsets.isEmpty()) {
            float k = 1.0F - (float)Math.exp(-step * 18.0F);
            Iterator<Entry<String, float[]>> it = this.dragOffsets.entrySet().iterator();

            while (it.hasNext()) {
                Entry<String, float[]> en = it.next();
                float[] o = en.getValue();
                if (!en.getKey().equals(this.dragHeld)) {
                    o[0] += (o[2] - o[0]) * k;
                    o[1] += (o[3] - o[1]) * k;
                    if (Math.abs(o[2] - o[0]) < 0.3F && Math.abs(o[3] - o[1]) < 0.3F) {
                        o[0] = o[2];
                        o[1] = o[3];
                        if (o[2] == 0.0F && o[3] == 0.0F) {
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    private void pressDrag(double mx, double my) {
        GuiDoc.Element e = this.pick(this.toDesignX(mx), this.toDesignY(my), c -> c.type.equals("drag") && c.b("enabled"));
        this.dragPress = e != null ? e.id : null;
        if (e != null) {
            float[] o = this.dragOffsets.get(e.id);
            this.dragPressX = mx;
            this.dragPressY = my;
            this.dragFromX = o != null ? o[2] : 0.0F;
            this.dragFromY = o != null ? o[3] : 0.0F;
        }
    }

    private GuiDoc.Element parentOf(List<GuiDoc.Element> list, GuiDoc.Element child) {
        for (GuiDoc.Element e : list) {
            if (e.children.contains(child)) {
                return e;
            }

            GuiDoc.Element deeper = this.parentOf(e.children, child);
            if (deeper != null) {
                return deeper;
            }
        }

        return null;
    }

    private void carry(double mx, double my) {
        GuiDoc.Element e = this.doc.element(this.dragPress);
        if (e != null) {
            if (this.dragHeld == null) {
                if (Math.abs(mx - this.dragPressX) + Math.abs(my - this.dragPressY) < 3.0) {
                    return;
                }

                this.dragHeld = e.id;
                this.pressedId = null;
                ProjectSounds.ui(this.project, e.get("takeSound"));
                if (this.listener != null) {
                    this.listener.event(e.id, "drag_start");
                }
            }

            float nx = this.dragFromX + (float)((mx - this.dragPressX) / this.areaScale);
            float ny = this.dragFromY + (float)((my - this.dragPressY) / this.areaScale);
            String axis = e.get("axis");
            if (axis.equals("y")) {
                nx = this.dragFromX;
            }

            if (axis.equals("x")) {
                ny = this.dragFromY;
            }

            int grid = e.i("grid");
            nx = GuiDrag.snap(nx, grid);
            ny = GuiDrag.snap(ny, grid);
            float[] o = this.dragOffsets.computeIfAbsent(e.id, k -> new float[4]);
            int[] r = this.rect(e);
            float homeX = r[0] - Math.round(o[0]);
            float homeY = r[1] - Math.round(o[1]);
            String bounds = e.get("bounds");
            GuiDoc.Element parent = bounds.equals("parent") ? this.parentOf(this.doc.elements, e) : null;
            if (parent != null) {
                int[] p = this.rect(parent);
                nx = GuiDrag.inside(homeX + nx, r[2], p[0], p[0] + p[2]) - homeX;
                ny = GuiDrag.inside(homeY + ny, r[3], p[1], p[1] + p[3]) - homeY;
            } else if (!bounds.equals("none")) {
                nx = GuiDrag.inside(homeX + nx, r[2], this.toDesignX(this.x), this.toDesignX(this.x + this.w)) - homeX;
                ny = GuiDrag.inside(homeY + ny, r[3], this.toDesignY(this.y), this.toDesignY(this.y + this.h)) - homeY;
            }

            o[0] = o[2] = nx;
            o[1] = o[3] = ny;
        }
    }

    private boolean zoneFull(GuiDoc.Element zone, GuiDoc.Element by) {
        int limit = zone.i("limit");
        if (limit > 0 && !zone.get("full").equals("swap")) {
            int there = 0;

            for (Entry<String, String> en : this.dragZones.entrySet()) {
                if (en.getValue().equals(zone.id) && !en.getKey().equals(by.id)) {
                    there++;
                }
            }

            return there >= limit;
        } else {
            return false;
        }
    }

    private void dropAt(GuiDoc.Element e, double mx, double my) {
        float dx = this.toDesignX(mx);
        float dy = this.toDesignY(my);
        GuiDoc.Element over = this.pick(dx, dy, z -> z.type.equals("drop_zone") && !GuiDoc.contains(e, z));
        boolean taken = over != null && GuiDrag.accepts(over.get("accept"), e.get("tag")) && !this.zoneFull(over, e);
        float[] o = this.dragOffsets.computeIfAbsent(e.id, k -> new float[4]);
        String from = this.dragZones.get(e.id);
        if (taken) {
            int limit = over.i("limit");
            if (limit > 0 && over.get("full").equals("swap")) {
                this.swapOut(over, e, from, limit);
            }

            if (over.get("snap").equals("center")) {
                this.putInto(e, over, o);
            }

            this.dragZones.put(e.id, over.id);
            ProjectSounds.ui(this.project, e.get("dropSound"));
            if (this.listener != null) {
                this.listener.event(e.id, "drag_drop:" + over.id);
            }
        } else if (over == null && !e.get("miss").equals("return")) {
            this.dragZones.remove(e.id);
            if (this.listener != null) {
                this.listener.event(e.id, "drag_drop:");
            }
        } else {
            o[2] = this.dragFromX;
            o[3] = this.dragFromY;
            if (this.listener != null) {
                this.listener.event(e.id, "drag_drop:" + (from != null ? from : ""));
            }
        }
    }

    private void putInto(GuiDoc.Element e, GuiDoc.Element zone, float[] o) {
        int[] r = this.rect(e);
        int[] z = this.rect(zone);
        o[2] = o[0] + (z[0] + z[2] / 2.0F) - (r[0] + r[2] / 2.0F);
        o[3] = o[1] + (z[1] + z[3] / 2.0F) - (r[1] + r[3] / 2.0F);
    }

    private void swapOut(GuiDoc.Element zone, GuiDoc.Element by, String from, int limit) {
        List<String> there = new ArrayList<>();

        for (Entry<String, String> en : this.dragZones.entrySet()) {
            if (en.getValue().equals(zone.id) && !en.getKey().equals(by.id)) {
                there.add(en.getKey());
            }
        }

        if (there.size() >= limit) {
            GuiDoc.Element other = this.doc.element(there.get(0));
            if (other != null) {
                float[] o = this.dragOffsets.computeIfAbsent(other.id, k -> new float[4]);
                GuiDoc.Element back = from != null ? this.doc.element(from) : null;
                if (back != null) {
                    this.putInto(other, back, o);
                    this.dragZones.put(other.id, from);
                } else {
                    int[] r = this.rect(other);
                    int[] m = this.rect(by);
                    float[] mine = this.dragOffsets.get(by.id);
                    o[2] = o[0] + (m[0] - Math.round(mine[0]) + this.dragFromX) - r[0];
                    o[3] = o[1] + (m[1] - Math.round(mine[1]) + this.dragFromY) - r[1];
                    this.dragZones.remove(other.id);
                }

                if (this.listener != null) {
                    this.listener.event(other.id, "drag_drop:" + (back != null ? from : ""));
                }
            }
        }
    }

    private void dropHint(Canvas c, GuiDoc.Element z, int[] r, float op) {
        GuiDoc.Element e = this.dragHeld != null ? this.doc.element(this.dragHeld) : null;
        if (e != null && !GuiDoc.contains(e, z)) {
            boolean fits = GuiDrag.accepts(z.get("accept"), e.get("tag")) && !this.zoneFull(z, e);
            float mx = this.toDesignX(this.mouseX);
            float my = this.toDesignY(this.mouseY);
            boolean under = mx >= r[0] && my >= r[1] && mx < r[0] + r[2] && my < r[1] + r[3];
            int color = under ? (fits ? z.color("hoverFill") : z.color("denyFill")) : (fits && z.b("showFree") ? a(z.color("hoverFill"), 0.4F) : 0);
            if (color >>> 24 != 0) {
                c.round(r[0], r[1], r[2], r[3], z.i("radius"), a(color, op));
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double ddx, double ddy) {
        if (this.interactive && this.dragPress != null) {
            this.computeArea();
            this.carry(mx, my);
            return true;
        } else if (this.interactive && this.draggingBar != null) {
            GuiDoc.Element e = this.doc.element(this.draggingBar);
            int[] content = e != null ? this.contents.get(e) : null;
            if (content == null) {
                return true;
            } else {
                int[] r = this.rect(e);
                float[] s = this.scrolls.computeIfAbsent(e.id, k -> new float[4]);
                float d = (float)((this.barVertical ? ddy : ddx) / this.areaScale);
                if (this.barVertical) {
                    s[3] = s[1] = Math.max(0.0F, Math.min((float)(content[1] - r[3]), s[3] + d * content[1] / r[3]));
                } else {
                    s[2] = s[0] = Math.max(0.0F, Math.min((float)(content[0] - r[2]), s[2] + d * content[0] / r[2]));
                }

                return true;
            }
        } else {
            return this.interactive;
        }
    }

    private boolean pressBar(float dx, float dy) {
        for (GuiDoc.Element e : this.doc.all()) {
            if (e.type.equals("scroll") && this.contents.containsKey(e) && e.b("visible")) {
                int[] b = this.bars(e, this.rect(e));
                if (b[2] > 0 && dx >= b[0] - 2 && dx < b[0] + b[2] + 2 && dy >= b[1] && dy < b[1] + b[3]) {
                    this.draggingBar = e.id;
                    this.barVertical = true;
                    return true;
                }

                if (b[6] > 0 && dx >= b[4] && dx < b[4] + b[6] && dy >= b[5] - 2 && dy < b[5] + b[7] + 2) {
                    this.draggingBar = e.id;
                    this.barVertical = false;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (!this.interactive) {
            return false;
        } else {
            this.computeArea();
            if (this.clickSlots(this.toDesignX(mx), this.toDesignY(my), button)) {
                return true;
            } else if (button != 0) {
                return true;
            } else {
                this.draggingBar = null;
                if (this.pressBar(this.toDesignX(mx), this.toDesignY(my))) {
                    return true;
                } else {
                    this.pressDrag(mx, my);
                    GuiDoc.Element e = this.elementAt(this.toDesignX(mx), this.toDesignY(my), true);
                    this.focusedInput = null;
                    this.pressedId = null;
                    if (e == null) {
                        return true;
                    } else {
                        if (e.type.equals("input")) {
                            this.focusedInput = e.id;
                            this.requestFocus();
                        } else if (e.type.equals("button") && e.b("enabled")) {
                            this.pressedId = e.id;
                        }

                        return true;
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!this.interactive) {
            return false;
        } else {
            this.draggingBar = null;
            this.computeArea();
            if (this.dragHeld != null) {
                GuiDoc.Element held = this.doc.element(this.dragHeld);
                if (held != null) {
                    this.dropAt(held, mx, my);
                }

                this.dragHeld = null;
            }

            this.dragPress = null;
            String p = this.pressedId;
            this.pressedId = null;
            if (p == null) {
                return true;
            } else {
                GuiDoc.Element e = this.elementAt(this.toDesignX(mx), this.toDesignY(my), true);
                if (e != null && e.id.equals(p)) {
                    this.click(e);
                }

                return true;
            }
        }
    }

    public void click(GuiDoc.Element e) {
        if (e.b("enabled")) {
            String sound = e.get("sound");
            if (!sound.isBlank()) {
                ProjectSounds.ui(this.project, sound);
            }

            String action = e.get("action");
            if (this.listener != null) {
                if (!action.equals("close")) {
                    this.listener.event(e.id, "click");
                }

                if (action.equals("close") || action.equals("event_close")) {
                    this.listener.event(e.id, "close");
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (!this.interactive) {
            return false;
        } else if (this.focusedInput != null) {
            String v = this.inputs.getOrDefault(this.focusedInput, "");
            switch (key) {
                case 86:
                    if ((mods & 2) != 0) {
                        GuiDoc.Element e = this.doc.element(this.focusedInput);
                        int max = e != null ? e.i("maxLength") : 64;
                        String add = Minecraft.getInstance().keyboardHandler.getClipboard().replace("\n", " ");
                        this.inputs.put(this.focusedInput, (v + add).substring(0, Math.min(max, v.length() + add.length())));
                        return true;
                    }

                    return true;
                case 256:
                case 257:
                case 258:
                case 335:
                    this.focusedInput = null;
                    return true;
                case 259:
                    if (!v.isEmpty()) {
                        this.inputs.put(this.focusedInput, v.substring(0, v.length() - 1));
                    }

                    return true;
                default:
                    return true;
            }
        } else {
            for (GuiDoc.Element e : this.doc.all()) {
                if (e.type.equals("button") && e.b("visible") && !e.get("key").isBlank()) {
                    int k = Specs.glfwKey(e.get("key").trim());
                    if (k == key || k >= 48 && k <= 57 && key == 320 + (k - 48)) {
                        this.click(e);
                        return true;
                    }
                }
            }

            return false;
        }
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (this.interactive && this.focusedInput != null && ch >= ' ') {
            GuiDoc.Element e = this.doc.element(this.focusedInput);
            int max = e != null ? Math.max(1, e.i("maxLength")) : 64;
            String v = this.inputs.getOrDefault(this.focusedInput, "");
            if (v.length() < max) {
                this.inputs.put(this.focusedInput, v + ch);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public int cursor(int mx, int my) {
        if (!this.interactive) {
            return 221185;
        } else {
            GuiDoc.Element e = this.elementAt(this.toDesignX(mx), this.toDesignY(my), true);
            if (e == null) {
                return 221185;
            } else if (!e.type.equals("slot") && !e.type.equals("inventory")) {
                return e.type.equals("input") ? 221186 : (e.type.equals("button") && e.b("enabled") ? 221188 : 221185);
            } else {
                return 221188;
            }
        }
    }

    public boolean hasFocusedInput() {
        return this.focusedInput != null;
    }

    public static List<GuiSchema.Type> palette() {
        return GuiSchema.types();
    }

    public interface Listener {
        void event(String var1, String var2);
    }

    private record Tex(ResourceLocation rl, int w, int h) {
    }
}
