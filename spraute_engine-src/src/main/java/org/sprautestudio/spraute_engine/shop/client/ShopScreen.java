package org.sprautestudio.spraute_engine.shop.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.shop.ShopView;

public class ShopScreen extends GfxScreen {
    private final ShopPanel panel;
    private final ShopParts.State live;
    private boolean hidGui;

    private static GuiView layout() {
        return EngineClient.window("shop");
    }

    public ShopScreen(ShopView view) {
        super(Component.literal(view.title));
        this.panel = new ShopPanel(view);
        this.live = new ShopParts.State(this.panel);
    }

    public static void show(String json, boolean refresh) {
        Minecraft mc = Minecraft.getInstance();
        if (json != null && !json.isEmpty()) {
            ShopView v = ShopView.fromJson(json);
            if (mc.screen instanceof ShopScreen s && s.panel.view.key.equals(v.key)) {
                s.panel.setView(v);
                if (refresh) {
                    mc.getSoundManager()
                        .play(SimpleSoundInstance.forUI(v.ok ? SoundEvents.EXPERIENCE_ORB_PICKUP : SoundEvents.VILLAGER_NO, v.ok ? 1.2F : 1.0F, 0.6F));
                }
            } else {
                if (!refresh) {
                    mc.setScreen(new ShopScreen(v));
                }
            }
        } else {
            if (mc.screen instanceof ShopScreen) {
                mc.setScreen(null);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.hideGui) {
            mc.options.hideGui = true;
            this.hidGui = true;
        }
    }

    @Override
    public void removed() {
        super.removed();
        if (this.hidGui) {
            Minecraft.getInstance().options.hideGui = false;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void draw(Gfx g, int mx, int my, float partial) {
        GuiView lay = layout();
        if (lay != null) {
            g.fill(0, 0, this.width, this.height, GuiDoc.toColor(lay.doc.get("background")));
            lay.liveState = this.live;
            ShopParts.vars(lay);
            EngineClient.drawWindow(g, lay, mx, my);
            if (this.live.hovered != null && !this.live.hovered.isEmpty()) {
                g.renderTooltip(this.font, this.live.hovered, mx, my);
            }
        } else {
            g.fill(0, 0, this.width, this.height, -1508305645);
            this.panel.layout(this.width, this.height, Minecraft.getInstance().getWindow().getGuiScale());
            this.panel.render(g, this.font, EngineClient.config(), this.panel.wx(mx), this.panel.wy(my));
            if (this.panel.hoveredStack != null && !this.panel.hoveredStack.isEmpty()) {
                g.renderTooltip(this.font, this.panel.hoveredStack, mx, my);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return super.mouseClicked(mx, my, button);
        } else {
            GuiView lay = layout();
            float dx = lay != null ? lay.toDesignX(mx) : 0.0F;
            float dy = lay != null ? lay.toDesignY(my) : 0.0F;
            boolean deal = lay != null ? ShopParts.click(this.live, dx, dy) : this.panel.click(this.panel.wx(mx), this.panel.wy(my));
            if ((lay != null ? ShopParts.hit(this.live, dx, dy) : this.panel.hit(this.panel.wx(mx), this.panel.wy(my))) != null) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Mc.sound(SoundEvents.UI_BUTTON_CLICK), 1.0F, 0.4F));
            }

            if (deal && this.panel.selected != null) {
                ShopView v = this.panel.view;
                SprauteNetwork.sendToServer(new Packets.ShopDeal(v.scriptId, v.key, this.panel.selected, Math.max(1, this.panel.qty), v.entityId));
                this.panel.qty = 1;
            }

            return true;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (layout() != null) {
            ShopParts.scroll(this.live, delta);
        } else {
            this.panel.scroll(delta);
        }

        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (Minecraft.getInstance().options.keyInventory.matches(key, scan)) {
            this.onClose();
            return true;
        } else {
            return super.keyPressed(key, scan, mods);
        }
    }
}
