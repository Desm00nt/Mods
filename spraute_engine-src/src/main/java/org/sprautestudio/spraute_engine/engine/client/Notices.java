package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;

public final class Notices {
    private static final int LIFE = 100;
    private static final int FADE = 20;
    private static final int EVERY = 12;
    private static final int ICON = 16;
    private static final float TRAVEL = 150.0F;
    private static final float START_Y = -16.0F;
    private static final Deque<Notices.Notice> WAITING = new ArrayDeque<>();
    private static final List<Notices.Notice> SHOWN = new ArrayList<>();
    private static int cooldown;

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(Notices::onTick);
        ClientHooks.RENDER_GUI.add(Notices::render);
        ClientHooks.LOGGING_OUT.add(Notices::onLogout);
    }

    private Notices() {
    }

    public static void show(String text, String icon, String item, String project, boolean sound) {
        ItemStack stack = ItemStack.EMPTY;
        ResourceLocation tex = null;
        if (!item.isEmpty()) {
            stack = ItemSpec.stack(item, 1);
        } else if (!icon.isEmpty()) {
            tex = texture(icon, project);
        }

        WAITING.addLast(new Notices.Notice(text, tex, stack, sound));
    }

    private static ResourceLocation texture(String icon, String project) {
        if (!icon.contains(":") && !icon.contains("/")) {
            return Images.texture(project, icon);
        } else {
            String id = icon.contains(":") ? icon : "minecraft:" + icon;
            if (!id.endsWith(".png")) {
                id = id + ".png";
            }

            return ResourceLocation.tryParse(id);
        }
    }

    public static void onTick(ClientHooks.ClientTick e) {
        Minecraft mc = Minecraft.getInstance();
        if (e.phase == TickPhase.END && !mc.isPaused()) {
            SHOWN.removeIf(nx -> ++nx.age > 100);
            if (cooldown > 0) {
                cooldown--;
            }

            if (cooldown == 0 && mc.screen == null && !WAITING.isEmpty()) {
                Notices.Notice n = WAITING.pollFirst();
                SHOWN.add(n);
                cooldown = 12;
                if (n.sound) {
                    ProjectSounds.playBuiltin("notice");
                }
            }
        }
    }

    public static void render(ClientHooks.RenderGui e) {
        if (!SHOWN.isEmpty()) {
            Minecraft mc = Minecraft.getInstance();
            if (!mc.options.hideGui) {
                Gfx g = e.getGuiGraphics();

                for (Notices.Notice n : SHOWN) {
                    float t = n.age + (mc.isPaused() ? 0.0F : e.getPartialTick());
                    float y = -16.0F + 150.0F * (1.0F - (1.0F - t / 100.0F) * (1.0F - t / 100.0F));
                    float alpha = Math.max(0.0F, Math.min(1.0F, (100.0F - t) / 20.0F));
                    if (!(alpha <= 0.0F)) {
                        g.pose().pushPose();
                        g.pose().translate(4.0F, y, 0.0F);
                        int tx = 0;
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
                        if (!n.item.isEmpty()) {
                            g.renderItem(n.item, 0, 0);
                            tx = 20;
                        } else if (n.icon != null) {
                            g.blit(n.icon, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
                            tx = 20;
                        }

                        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                        g.drawString(mc.font, n.text, tx, (16 - 9) / 2 + 1, Math.max(4, Math.round(alpha * 255.0F)) << 24 | 16777215, true);
                        g.pose().popPose();
                    }
                }
            }
        }
    }

    public static void onLogout(ClientHooks.LoggingOut e) {
        SHOWN.clear();
        WAITING.clear();
    }

    private static final class Notice {
        final String text;
        final ResourceLocation icon;
        final ItemStack item;
        final boolean sound;
        int age;

        Notice(String text, ResourceLocation icon, ItemStack item, boolean sound) {
            this.text = text;
            this.icon = icon;
            this.item = item;
            this.sound = sound;
        }
    }
}
