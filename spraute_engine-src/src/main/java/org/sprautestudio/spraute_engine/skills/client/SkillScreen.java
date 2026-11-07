package org.sprautestudio.spraute_engine.skills.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;
import org.sprautestudio.spraute_engine.engine.client.EngineClient;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.client.GuiView;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.skills.Skills;

public final class SkillScreen extends GfxScreen {
    private static final int MARGIN = 16;
    final SkillPanel panel;
    private boolean pressed;
    private boolean moved;

    public SkillScreen(Skills.View view) {
        super(Component.literal(view.tree.title));
        this.panel = new SkillPanel(view);
    }

    private GuiView layout() {
        return EngineClient.window(this.panel.view.tree.menu(this.panel.view.menu).layout);
    }

    @Override
    public void draw(Gfx g, int mx, int my, float partial) {
        GuiView lay = this.layout();
        if (lay != null) {
            g.fill(0, 0, this.width, this.height, GuiDoc.toColor(lay.doc.get("back")));
            lay.liveState = this.panel;
            SkillParts.begin(lay);
            EngineClient.drawWindow(g, lay, mx, my);
        } else {
            g.fill(0, 0, this.width, this.height, -1879048192);
            this.panel.draw(g, this.font, 16, 16, this.width - 32, this.height - 32, mx, my);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return true;
        } else {
            this.pressed = true;
            this.moved = false;
            GuiView lay = this.layout();
            String action = lay != null ? this.panel.click(lay.toDesignX(mx), lay.toDesignY(my)) : this.panel.click(mx, my);
            if (action != null) {
                String node = this.panel.selected != null ? this.panel.selected : "";
                SprauteNetwork.sendToServer(new Packets.SkillAction(this.panel.view.key, node, action));
            }

            return true;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        GuiView lay = this.layout();
        float k = lay != null ? Math.max(0.01F, lay.areaScale()) : 1.0F;
        if (this.pressed) {
            this.panel.drag(dx / k, dy / k);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.pressed = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        GuiView lay = this.layout();
        this.panel.scroll(delta, lay != null ? lay.toDesignX(mx) : mx, lay != null ? lay.toDesignY(my) : my);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        String full = InputConstants.getKey(key, scan).getName();
        String name = full.startsWith("key.keyboard.") ? full.substring(13) : full;
        if (!Minecraft.getInstance().options.keyInventory.matches(key, scan) && !name.equals(this.panel.view.tree.menu(this.panel.view.menu).openKey)) {
            return super.keyPressed(key, scan, mods);
        } else {
            this.onClose();
            return true;
        }
    }

    @Override
    public void removed() {
        super.removed();
        SprauteNetwork.sendToServer(new Packets.SkillAction(this.panel.view.key, "", "closed"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
