package org.sprautestudio.spraute_engine.quest.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.cutscene.client.CameraDirector;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.engine.client.Skins;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.quest.QuestJournal;
import org.sprautestudio.spraute_engine.ui.GuiText;

public final class QuestClient {
    public static final KeyMapping JOURNAL = ClientPlatform.get().gameKey("key.spraute_engine.journal", 74, "key.categories.spraute_engine");
    private static final ResourceLocation TASK_LINE = new ResourceLocation("spraute_engine", "textures/gui/journal/task_strip.png");
    private static final ResourceLocation CATEGORIES = new ResourceLocation("spraute_engine", "textures/gui/journal/icons.png");
    private static QuestJournal journal = new QuestJournal();
    private static String toastTitle = "";
    private static String toastText = "";
    private static String toastKind = "";
    private static long toastAt;

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(QuestClient::tick);
        ClientHooks.LOGGING_OUT.add(QuestClient::logout);
        ClientHooks.RENDER_GUI.add(QuestClient::render);
    }

    private QuestClient() {
    }

    public static QuestJournal journal() {
        return journal;
    }

    public static void receive(String json, boolean open) {
        journal = QuestJournal.fromJson(json);
        if (!journal.toast.isEmpty()) {
            String[] p = journal.toast.split("\n", 2);
            showToast(journal.toastKind, p[0], p.length > 1 ? p[1] : "");
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof QuestScreen s) {
            s.update(journal);
        } else if (open) {
            mc.setScreen(new QuestScreen(journal));
        }
    }

    private static EngineConfig.Quests.Toast toastCfg() {
        return EngineClient.config().quests.toast;
    }

    public static void showToast(String kind, String title, String text) {
        EngineConfig.Quests.Toast c;
        boolean var10000;
        label125: {
            label124: {
                c = toastCfg();
                if (c.enabled) {
                    switch (kind) {
                        case "start":
                            if (c.onStart) {
                                break label124;
                            }
                            break;
                        case "task":
                            if (c.onTask) {
                                break label124;
                            }
                            break;
                        case "reveal":
                            if (c.onReveal) {
                                break label124;
                            }
                            break;
                        case "complete":
                            if (c.onComplete) {
                                break label124;
                            }
                            break;
                        case "fail":
                            if (c.onFail) {
                                break label124;
                            }
                            break;
                        default:
                            break label124;
                    }
                }

                var10000 = false;
                break label125;
            }

            var10000 = true;
        }

        boolean on = var10000;
        if (on) {
            String own = switch (kind) {
                case "start" -> c.startTitle;
                case "task" -> c.taskTitle;
                case "reveal" -> c.revealTitle;
                case "complete" -> c.completeTitle;
                case "fail" -> c.failTitle;
                default -> "";
            };
            toastTitle = own != null && !own.isBlank() ? own : title;
            toastText = text;
            toastKind = kind;
            toastAt = System.currentTimeMillis();
            GuiView lay = EngineClient.window("toast");
            if (lay != null) {
                lay.restartAppear();
            }

            if (c.sound) {
                Minecraft mc = Minecraft.getInstance();
                SoundManager var13 = mc.getSoundManager();
                String var8 = toastKind;

                var13.play(SimpleSoundInstance.forUI(switch (var8) {
                    case "complete" -> SoundEvents.PLAYER_LEVELUP;
                    case "fail" -> Mc.sound(SoundEvents.NOTE_BLOCK_BASS);
                    default -> SoundEvents.EXPERIENCE_ORB_PICKUP;
                }, toastKind.equals("complete") ? 1.0F : 1.2F, toastKind.equals("complete") ? 0.6F : 0.8F));
            }
        }
    }

    public static void track(String key) {
        SprauteNetwork.sendToServer(new Packets.QuestTrack(key));
    }

    public static void tick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END) {
            Minecraft mc = Minecraft.getInstance();

            while (JOURNAL.consumeClick()) {
                if (mc.screen == null && mc.player != null) {
                    mc.setScreen(new QuestScreen(journal));
                }
            }
        }
    }

    public static void logout(ClientHooks.LoggingOut e) {
        journal = new QuestJournal();
        toastAt = 0L;
    }

    public static void render(ClientHooks.RenderGui e) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui && !(mc.screen instanceof QuestScreen) && !CameraDirector.hidesCustom()) {
            Gfx g = e.getGuiGraphics();
            Font font = mc.font;
            int sw = g.guiWidth();
            tracker(g, font, sw);
            toast(g, font, sw);
        }
    }

    private static void tracker(Gfx g, Font font, int sw) {
        if (!journal.tracked.isEmpty() && EngineClient.config().quests.tracker && EngineClient.hudLayout() == null) {
            QuestJournal.Entry q = journal.entry(journal.tracked);
            if (q != null && q.state.equals("active")) {
                int w = 150;
                int x = sw - w - 6;
                float y = 46.0F;
                String title = font.plainSubstrByWidth(q.title, w);
                g.drawString(
                    font,
                    Component.literal(title).withStyle(ChatFormatting.BOLD),
                    sw - 6 - font.width(Component.literal(title).withStyle(ChatFormatting.BOLD)),
                    (int)y,
                    -11686,
                    true
                );
                y += 11.0F;
                int shown = 0;

                for (QuestJournal.Task t : q.tasks) {
                    if (shown >= 5) {
                        break;
                    }

                    boolean done = t.done();
                    String s = (done ? "✔ " : "◇ ") + t.text + (t.count > 1 ? "  " + t.progress + "/" + t.count : "");
                    s = font.plainSubstrByWidth(s, (int)(w / 0.8F));
                    g.pose().pushPose();
                    g.pose().scale(0.8F, 0.8F, 1.0F);
                    GuiText.draw(g, font, s, (sw - 6 - font.width(s) * 0.8F) / 0.8F, y / 0.8F, done ? -8658806 : (t.optional ? -4669236 : -1), true);
                    g.pose().popPose();
                    y += 9.0F;
                    shown++;
                }
            }
        }
    }

    private static void toast(Gfx g, Font font, int sw) {
        EngineConfig.Quests.Toast c = toastCfg();
        EngineConfig.Quests q = EngineClient.config().quests;
        long life = (long)(c.seconds * 1000.0F);
        long t = System.currentTimeMillis() - toastAt;
        if (toastAt != 0L && t <= life) {
            float in = Math.min(1.0F, (float)t / 300.0F);
            float out = Math.min(1.0F, (float)(life - t) / 400.0F);
            float a = Math.min(in, out);
            GuiView lay = EngineClient.window("toast");
            if (lay != null) {
                lay.vars.put("title", toastTitle);
                lay.vars.put("text", toastText);
                lay.vars.put("kind", toastKind);
                lay.opacity = out;
                EngineClient.drawWindow(g, lay, -10000, -10000);
            } else {
                int w = c.width;
                int h = 16;
                float k = c.scale;
                float x = (sw - w * k) / 2.0F;
                float y = c.y - (1.0F - in) * 16.0F;
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                g.pose().pushPose();
                g.pose().translate(x, y, 0.0F);
                g.pose().scale(k, k, 1.0F);
                int bgA = c.background >>> 24 & 0xFF;
                g.fill(0, -12, w, h + 2, (int)(a * bgA) << 24 | c.background & 16777215);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
                String project = EngineClient.config().project;
                if (q.taskLine.isSet()) {
                    Skins.draw(g, project, q.taskLine, 0, 2, w, h);
                } else {
                    g.blit(TASK_LINE, 0, 2, 0.0F, 0.0F, w, h, w, h);
                }

                int tx = 6;
                if (c.icon) {
                    int icon = toastKind.equals("complete") ? 1 : (toastKind.equals("fail") ? 2 : 0);
                    Skins.drawSheet(g, project, q.icons, CATEGORIES, 3, -10, 21, 23, icon * 21, 0, 63, 46);
                    tx = 28;
                }

                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                int alpha = Math.max(4, (int)(a * 255.0F)) << 24;
                int col = (toastKind.equals("complete") ? q.doneColor : (toastKind.equals("fail") ? q.failColor : q.accentColor)) & 16777215;
                g.drawString(font, Component.literal(toastTitle.toUpperCase()).withStyle(ChatFormatting.BOLD), tx, -8, alpha | col, true);
                int room = w - tx - 4;
                String line = font.width(toastText) <= room ? toastText : font.plainSubstrByWidth(toastText, room - font.width("…")).stripTrailing() + "…";
                g.drawString(font, line, tx, 6, alpha | q.textColor & 16777215, true);
                g.pose().popPose();
            }
        }
    }

    public static final class ModBus {
        public static void registerHooks() {
            ClientHooks.REGISTER_KEYS.add(QuestClient.ModBus::keys);
        }

        public static void keys(ClientHooks.RegisterKeys e) {
            e.register(QuestClient.JOURNAL);
        }
    }
}
