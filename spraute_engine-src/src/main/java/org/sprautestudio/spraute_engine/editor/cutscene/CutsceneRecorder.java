package org.sprautestudio.spraute_engine.editor.cutscene;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.editor.ClientEvents;
import org.sprautestudio.spraute_engine.editor.SprauteScreen;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.ClientPlatform;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class CutsceneRecorder {
    public static final KeyMapping FRAME = ClientPlatform.get().gameKey("key.spraute_engine.cutscene_frame", 75, "key.categories.spraute_engine");
    private static CutsceneTab tab;

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(CutsceneRecorder::onTick);
        ClientHooks.RENDER_GUI.add(CutsceneRecorder::onGui);
    }

    private CutsceneRecorder() {
    }

    static void start(CutsceneTab t) {
        tab = t;
    }

    public static boolean recording() {
        return tab != null;
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END) {
            Minecraft mc = Minecraft.getInstance();
            if (tab != null) {
                if (!(mc.screen instanceof SprauteScreen) && mc.player != null) {
                    while (FRAME.consumeClick()) {
                        if (mc.screen == null) {
                            tab.addFromPlayer();
                            mc.player.playSound(Mc.sound(SoundEvents.UI_BUTTON_CLICK), 0.4F, 1.6F);
                        }
                    }
                } else {
                    tab = null;
                }
            } else {
                while (FRAME.consumeClick()) {
                }
            }
        }
    }

    public static void onGui(ClientHooks.RenderGui e) {
        if (tab != null) {
            Minecraft mc = Minecraft.getInstance();
            Gfx g = e.getGuiGraphics();
            String key = FRAME.getTranslatedKeyMessage().getString();
            String back = ClientEvents.OPEN_EDITOR.getTranslatedKeyMessage().getString();
            String[] lines = new String[]{
                "● Запись катсцены — кадров: " + tab.frames(),
                "[" + key + "] — кадр там, где вы стоите и куда смотрите",
                "[" + back + "] — вернуться в редактор (в режиме наблюдателя можно летать сквозь стены)"
            };
            int y = 6;

            for (int i = 0; i < lines.length; i++) {
                int w = mc.font.width(lines[i]);
                g.fill(4, y - 2, 10 + w, y + 9, -2013265920);
                g.drawString(mc.font, lines[i], 7, y, i == 0 ? -43691 : -1, false);
                y += 12;
            }
        }
    }
}
