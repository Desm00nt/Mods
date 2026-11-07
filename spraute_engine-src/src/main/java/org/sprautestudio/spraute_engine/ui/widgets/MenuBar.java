package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class MenuBar extends Widget implements UI.CursorProvider {
    private final List<MenuBar.Menu> menus = new ArrayList<>();
    private PopupMenu open;
    private int openIndex = -1;

    public MenuBar menu(String title, Consumer<PopupMenu> builder) {
        this.menus.add(new MenuBar.Menu(title, builder));
        return this;
    }

    public int preferredWidth() {
        int wsum = 0;
        Font f = Minecraft.getInstance().font;

        for (MenuBar.Menu m : this.menus) {
            wsum += f.width(m.title) + 14;
        }

        return wsum;
    }

    private int indexAt(double mx) {
        Font f = Minecraft.getInstance().font;
        int xx = this.x;

        for (int i = 0; i < this.menus.size(); i++) {
            int mw = f.width(this.menus.get(i).title) + 14;
            if (mx >= xx && mx < xx + mw) {
                return i;
            }

            xx += mw;
        }

        return -1;
    }

    private int menuX(int index) {
        Font f = Minecraft.getInstance().font;
        int xx = this.x;

        for (int i = 0; i < index; i++) {
            xx += f.width(this.menus.get(i).title) + 14;
        }

        return xx;
    }

    @Override
    public int cursor(int mx, int my) {
        return 221188;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        UI ui = UI.get();
        if (this.open != null && ui != null && !ui.isOverlayOpen(this.open)) {
            this.open = null;
            this.openIndex = -1;
        }

        int hov = this.isHovered() ? this.indexAt(mx) : -1;
        if (this.open != null && hov >= 0 && hov != this.openIndex) {
            this.openMenu(hov);
        }

        int xx = this.x;

        for (int i = 0; i < this.menus.size(); i++) {
            MenuBar.Menu m = this.menus.get(i);
            int mw = c.width(m.title) + 14;
            if (i == this.openIndex) {
                c.round(xx + 1, this.y + 2, mw - 2, this.h - 4, 3, 1435200767);
            } else if (i == hov) {
                c.round(xx + 1, this.y + 2, mw - 2, this.h - 4, 3, 352321535);
            }

            c.text(m.title, xx + 7, this.y + (this.h - 8) / 2, i != this.openIndex && i != hov ? -6577995 : -1841682);
            xx += mw;
        }
    }

    private void openMenu(int index) {
        if (this.open != null) {
            this.open.close();
        }

        PopupMenu pm = new PopupMenu();
        this.menus.get(index).builder.accept(pm);
        this.open = pm;
        this.openIndex = index;
        pm.show(this.menuX(index), this.y + this.h, this);
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        int idx = this.indexAt(mx);
        if (idx < 0) {
            return false;
        } else {
            if (idx == this.openIndex && this.open != null) {
                this.open.close();
                this.open = null;
                this.openIndex = -1;
            } else {
                this.openMenu(idx);
            }

            return true;
        }
    }

    private record Menu(String title, Consumer<PopupMenu> builder) {
    }
}
