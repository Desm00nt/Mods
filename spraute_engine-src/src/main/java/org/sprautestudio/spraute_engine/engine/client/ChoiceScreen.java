package org.sprautestudio.spraute_engine.engine.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.client.SpeechClient;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.runtime.Anims;

public class ChoiceScreen extends GfxScreen {
    private final int scriptId;
    private final int choiceId;
    private final int entityId;
    private final String speaker;
    private final List<String> paragraphs;
    private final List<String> answers;
    private final List<Boolean> locked;
    private final List<String> emotions;
    private final String emotion;
    private final List<Boolean> centered;
    private final ResourceLocation portrait;
    private int selected = -1;
    private int typed;
    private int paragraph;
    private int lastHover = -1;
    private int textScroll = -1;
    private boolean answered;
    private static LivingEntity mobCache;
    private static String mobCacheId = "";

    public ChoiceScreen(Packets.ChoiceOpen msg) {
        super(Component.literal(msg.question()));
        this.scriptId = msg.scriptId();
        this.choiceId = msg.choiceId();
        this.entityId = msg.entityId();
        this.speaker = msg.speaker();
        this.paragraphs = List.of(msg.question().split("\\n\\s*\\n"));
        this.answers = msg.answers();
        this.locked = msg.locked();
        this.emotions = msg.emotions();
        this.emotion = msg.emotion();
        this.centered = msg.centered();
        this.portrait = msg.portrait().isEmpty() ? null : ResourceLocation.tryParse(msg.portrait());
    }

    public static void open(Packets.ChoiceOpen msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ChoiceScreen(msg));
        ProjectSounds.playBuiltin("dialog_open");
        if (cfg().camera && msg.entityId() >= 0) {
            CameraDirector.dialogFlyTo(msg.entityId());
        }
    }

    private static EngineConfig.Choice cfg() {
        return EngineClient.config().choice;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private String shownText() {
        return String.join("\n\n", this.paragraphs.subList(0, this.paragraph + 1));
    }

    private boolean typedOut() {
        return this.paragraph >= this.paragraphs.size() - 1 && this.typed >= this.shownText().length();
    }

    private boolean paragraphTyped() {
        return this.typed >= this.shownText().length();
    }

    private List<String> shownAnswers() {
        return this.typedOut() ? this.answers : List.of();
    }

    private boolean isLocked(int i) {
        return i >= 0 && i < this.locked.size() && this.locked.get(i);
    }

    private void hoverFace(int hover) {
        if (hover != this.lastHover) {
            this.lastHover = hover;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.level.getEntity(this.entityId) instanceof AnimatedEntity ae) {
                String face = hover >= 0 && hover < this.emotions.size() && !this.emotions.get(hover).isEmpty() ? this.emotions.get(hover) : this.emotion;
                Anims.showEmotion(ae, face);
            }
        }
    }

    @Override
    public void tick() {
        if (CameraDirector.dialogArrived() && !this.paragraphTyped()) {
            this.typed = Math.min(this.shownText().length(), this.typed + Math.round(2.0F + Minecraft.getInstance().getFrameTime()));
        }
    }

    static LivingEntity subject(EngineConfig.Choice c, int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        } else {
            String var3 = c.show;
            switch (var3) {
                case "player":
                    return mc.player;
                case "mob":
                    if (!c.mob.equals(mobCacheId) || mobCache == null || Mc.level(mobCache) != mc.level) {
                        mobCacheId = c.mob;
                        mobCache = null;
                        ResourceLocation rl = ResourceLocation.tryParse(c.mob.contains(":") ? c.mob : "minecraft:" + c.mob);
                        EntityType<?> type = rl == null ? null : Regs.ENTITY_TYPE.getOptional(rl).orElse(null);
                        if (type != null) {
                            try {
                                if (type.create(mc.level) instanceof LivingEntity lex) {
                                    mobCache = lex;
                                }
                            } catch (Throwable var9) {
                            }
                        }
                    }

                    return mobCache;
                case "none":
                    return null;
                default:
                    return (entityId >= 0 ? mc.level.getEntity(entityId) : null) instanceof LivingEntity le ? le : null;
            }
        }
    }

    public static LivingEntity previewSubject(EngineConfig.Choice c) {
        return (LivingEntity)(c.show.equals("mob") ? subject(c, -1) : Minecraft.getInstance().player);
    }

    @Override
    public void draw(Gfx g, int mx, int my, float partial) {
        if (CameraDirector.dialogArrived()) {
            EngineConfig cfg = EngineClient.config();
            GuiView lay = EngineClient.window("choice");
            if (lay != null) {
                lay.vars.put("name", this.speaker);
                lay.vars.put("question", this.shownText().substring(0, Math.min(this.shownText().length(), this.typed)));
                lay.answers = this.typedOut() ? this.answers : List.of();
                lay.noSpeaker = true;
                int h = this.typedOut() ? lay.answerAt(mx, my) : -1;
                if (h >= 0) {
                    this.selected = h;
                }

                lay.answerHover = this.selected;
                EngineClient.drawWindow(g, lay, mx, my);
            } else {
                float k = this.zoom(cfg.choice);
                int vw = Math.round(this.width / k);
                int vh = Math.round(this.height / k);
                double vx = mx / k;
                double vy = my / k;
                int hover = this.typedOut() ? ChoiceLayout.answerAt(cfg.choice, this.font, vw, vh, this.answers, vx, vy) : -1;
                this.hoverFace(hover);
                if (hover >= 0) {
                    this.selected = hover;
                }

                g.pose().pushPose();
                g.pose().scale(k, k, 1.0F);
                ChoiceLayout.draw(
                    g,
                    this.font,
                    cfg,
                    vw,
                    vh,
                    new ChoiceLayout.Content(
                        this.speaker,
                        this.shownText(),
                        this.typed,
                        this.shownAnswers(),
                        this.typedOut() ? this.selected : -2,
                        subject(cfg.choice, this.entityId),
                        (float)vx,
                        (float)vy,
                        this.locked,
                        this.textScroll,
                        this.centered,
                        this.portrait
                    )
                );
                g.pose().popPose();
            }
        }
    }

    private float zoom(EngineConfig.Choice c) {
        return Math.max(0.25F, this.height / 270.0F * c.uiScale);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || !CameraDirector.dialogArrived()) {
            return true;
        } else if (!this.typedOut()) {
            this.typed = this.shownText().length();
            return true;
        } else {
            GuiView lay = EngineClient.window("choice");
            int i;
            if (lay != null) {
                i = lay.answerAt(mx, my);
            } else {
                float k = this.zoom(cfg());
                i = ChoiceLayout.answerAt(cfg(), this.font, Math.round(this.width / k), Math.round(this.height / k), this.answers, mx / k, my / k);
            }

            if (i >= 0 && !this.isLocked(i)) {
                this.answer(i);
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        float k = this.zoom(cfg());
        int most = ChoiceLayout.textOverflow(cfg(), this.font, Math.round(this.width / k), Math.round(this.height / k), this.shownAnswers(), this.shownText());
        int from = this.textScroll < 0 ? most : this.textScroll;
        int to = from - (int)(delta * 9.0);
        this.textScroll = to >= most ? -1 : Math.max(0, to);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            this.onClose();
            return true;
        } else if (!CameraDirector.dialogArrived()) {
            return true;
        } else if (SpeechClient.ADVANCE.matches(key, scan) && this.paragraphTyped() && this.paragraph < this.paragraphs.size() - 1) {
            this.paragraph++;
            this.textScroll = -1;
            if (this.scriptId >= 0) {
                SprauteNetwork.sendToServer(new Packets.ChoiceParagraph(this.scriptId, this.choiceId, this.paragraph));
            }

            return true;
        } else if (!this.typedOut()) {
            if (key == 32 || key == 257 || key == 335) {
                this.typed = this.shownText().length();
            }

            return true;
        } else if (key >= 49 && key <= 57 && key - 49 < this.answers.size()) {
            if (!this.isLocked(key - 49)) {
                this.answer(key - 49);
            }

            return true;
        } else {
            if (key == 264 || key == 83) {
                this.selected = (this.selected + 1 + this.answers.size()) % this.answers.size();
            } else if (key == 265 || key == 87) {
                this.selected = (Math.max(0, this.selected) - 1 + this.answers.size()) % this.answers.size();
            } else if ((key == 257 || key == 335 || key == 32) && this.selected >= 0 && !this.isLocked(this.selected)) {
                this.answer(this.selected);
            }

            return true;
        }
    }

    private void answer(int index) {
        if (!this.answered) {
            this.answered = true;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (this.scriptId >= 0) {
                SprauteNetwork.sendToServer(new Packets.ChoiceAnswer(this.scriptId, this.choiceId, index + 1));
            }

            CameraDirector.dialogFlyBack();
            this.onClose();
        }
    }

    @Override
    public void onClose() {
        if (!this.answered) {
            this.answered = true;
            if (this.scriptId >= 0) {
                SprauteNetwork.sendToServer(new Packets.ChoiceAnswer(this.scriptId, this.choiceId, -1));
            }

            CameraDirector.dialogFlyBack();
        }

        Minecraft.getInstance().setScreen(null);
    }
}
