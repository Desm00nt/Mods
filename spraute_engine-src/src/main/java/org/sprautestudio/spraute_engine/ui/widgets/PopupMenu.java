package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class PopupMenu extends Widget implements UI.OverlayLayout, UI.CursorProvider {
    private final List<PopupMenu.Item> items = new ArrayList<>();
    private int anchorX;
    private int anchorY;
    private int hoverIndex = -1;
    private static final int ROW = 17;
    private static final int SEP = 7;
    private static final int PAD = 4;
    private int contentH;
    private int scroll;
    private String typed = "";
    private long typedAt;
    public Runnable onClosed;
    private PopupMenu parent;
    private PopupMenu child;
    private int childIndex = -1;
    private int settling = -1;
    private long settledAt;
    private static final long OPEN_DELAY_MS = 130L;
    private int lastMx = Integer.MIN_VALUE;
    private int lastMy;

    public PopupMenu.Item submenu(String text, Icon icon, Consumer<PopupMenu> rows) {
        PopupMenu.Item it = this.item(text, icon, null, null);
        it.submenu = rows;
        return it;
    }

    public PopupMenu.Item item(String text, Icon icon, String shortcut, Runnable action) {
        PopupMenu.Item it = new PopupMenu.Item(text, icon, shortcut, action, false);
        this.items.add(it);
        return it;
    }

    public PopupMenu.Item item(String text, Runnable action) {
        return this.item(text, null, null, action);
    }

    public void separator() {
        if (!this.items.isEmpty() && !this.items.get(this.items.size() - 1).separator) {
            this.items.add(new PopupMenu.Item("", null, null, null, true));
        }
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public PopupMenu show(int x, int y) {
        return this.show(x, y, null);
    }

    public PopupMenu show(int x, int y, Widget owner) {
        UI ui = UI.get();
        if (ui == null) {
            return this;
        } else {
            this.anchorX = x;
            this.anchorY = y;
            UI.Overlay o = ui.openOverlay(this, false, true, this.onClosed);
            o.owner = owner;
            return this;
        }
    }

    public void close() {
        this.closeChild();
        UI ui = UI.get();
        if (ui != null) {
            ui.closeOverlay(this);
        }
    }

    private void closeChild() {
        if (this.child != null) {
            this.child.close();
        }

        this.child = null;
        this.childIndex = -1;
    }

    private void openChild(int idx) {
        PopupMenu.Item it = this.items.get(idx);
        this.closeChild();
        PopupMenu menu = new PopupMenu();
        menu.parent = this;
        menu.onClosed = () -> {
            if (this.child == menu) {
                this.child = null;
                this.childIndex = -1;
                this.settling = -1;
            }
        };
        it.submenu.accept(menu);
        if (!menu.isEmpty()) {
            this.child = menu;
            this.childIndex = idx;
            menu.show(this.x + this.w - 3, this.y + this.rowTop(idx) - this.scroll - 4);
        }
    }

    private void followHover() {
        if (this.hoverIndex != this.settling) {
            this.settling = this.hoverIndex;
            this.settledAt = System.currentTimeMillis();
        }

        if (this.settling != this.childIndex && System.currentTimeMillis() - this.settledAt >= 130L) {
            if (this.settling >= 0 && this.items.get(this.settling).submenu != null && this.items.get(this.settling).isEnabled()) {
                this.openChild(this.settling);
            } else {
                this.closeChild();
            }
        }
    }

    @Override
    public void layoutOverlay(int vw, int vh) {
        Font font = Minecraft.getInstance().font;
        int width = 120;

        for (PopupMenu.Item it : this.items) {
            if (!it.separator) {
                int iw = 26 + font.width(it.text) + (it.shortcut != null ? font.width(it.shortcut) + 20 : 0) + (it.submenu != null ? 14 : 0) + 10;
                width = Math.max(width, iw);
            }
        }

        int height = 8;

        for (PopupMenu.Item itx : this.items) {
            height += itx.separator ? 7 : 17;
        }

        this.contentH = height;
        boolean scrolls = height > vh - 4;
        if (scrolls) {
            width += 6;
        }

        int below = vh - 2 - this.anchorY;
        int above = this.anchorY - 2;
        int ph = Math.min(height, vh - 4);
        int py;
        if (height <= below) {
            py = this.anchorY;
        } else if (height <= above) {
            py = this.anchorY - height;
        } else if (below >= above) {
            ph = Math.min(height, Math.max(below, 60));
            py = Math.min(this.anchorY, vh - 2 - ph);
        } else {
            ph = Math.min(height, above);
            py = this.anchorY - ph;
        }

        int px = this.parent != null && this.anchorX + width > vw - 2 ? this.parent.x - width + 3 : Math.min(this.anchorX, vw - width - 2);
        this.bounds(Math.max(2, px), Math.max(2, py), width, ph);
        this.clampScroll();
    }

    private void clampScroll() {
        this.scroll = Math.max(0, Math.min(this.scroll, this.contentH - this.h));
    }

    private int rowTop(int idx) {
        int yy = 4;

        for (int i = 0; i < idx; i++) {
            yy += this.items.get(i).separator ? 7 : 17;
        }

        return yy;
    }

    private void reveal(int idx) {
        if (idx >= 0) {
            int top = this.rowTop(idx);
            if (top - 4 < this.scroll) {
                this.scroll = top - 4;
            }

            if (top + 17 + 4 > this.scroll + this.h) {
                this.scroll = top + 17 + 4 - this.h;
            }

            this.clampScroll();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (this.contentH <= this.h) {
            return true;
        } else {
            this.scroll -= (int)(delta * 17.0 * 2.0);
            this.clampScroll();
            return true;
        }
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (Character.isISOControl(ch)) {
            return false;
        } else {
            long now = System.currentTimeMillis();
            this.typed = (now - this.typedAt > 900L ? "" : this.typed) + Character.toLowerCase(ch);
            this.typedAt = now;

            for (int i = 0; i < this.items.size(); i++) {
                PopupMenu.Item it = this.items.get(i);
                if (!it.separator && it.text.toLowerCase().contains(this.typed)) {
                    this.hoverIndex = i;
                    this.reveal(i);
                    break;
                }
            }

            return true;
        }
    }

    private int indexAt(double my) {
        if (!(my < this.y) && !(my >= this.y + this.h)) {
            int yy = this.y + 4 - this.scroll;

            for (int i = 0; i < this.items.size(); i++) {
                int hgt = this.items.get(i).separator ? 7 : 17;
                if (my >= yy && my < yy + hgt) {
                    return this.items.get(i).separator ? -1 : i;
                }

                yy += hgt;
            }

            return -1;
        } else {
            return -1;
        }
    }

    @Override
    public int cursor(int mx, int my) {
        return this.indexAt(my) >= 0 ? 221188 : 221185;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.shadow(this.x, this.y, this.w, this.h, 5, 6);
        c.roundBorder(this.x, this.y, this.w, this.h, 5, -14210506, -12762793);
        if (this.isHovered() && (mx != this.lastMx || my != this.lastMy)) {
            this.hoverIndex = this.indexAt(my);
        }

        this.lastMx = mx;
        this.lastMy = my;
        if (this.isHovered()) {
            this.followHover();
        }

        c.pushClip(this.x, this.y + 1, this.w, this.h - 2);
        int yy = this.y + 4 - this.scroll;

        for (int i = 0; i < this.items.size(); i++) {
            PopupMenu.Item it = this.items.get(i);
            if (it.separator) {
                c.fill(this.x + 6, yy + 3, this.w - 12, 1, -13749952);
                yy += 7;
            } else {
                boolean en = it.isEnabled();
                if ((i == this.hoverIndex || i == this.childIndex) && en) {
                    c.round(this.x + 3, yy, this.w - 6, 17, 3, 1435200767);
                }

                boolean checked = it.checked != null && it.checked.getAsBoolean();
                Icon ic = checked ? Icon.CHECK : it.icon;
                if (ic != null) {
                    c.icon(ic, this.x + 9, yy + 4, en ? (checked ? -12988235 : it.iconColor) : -9933435);
                }

                c.text(it.text, this.x + 24, yy + 5, en ? -1841682 : -9933435);
                if (it.shortcut != null) {
                    c.text(it.shortcut, this.x + this.w - 10 - c.width(it.shortcut), yy + 5, -9933435);
                }

                if (it.submenu != null) {
                    c.icon(Icon.CHEVRON_RIGHT, this.x + this.w - 15, yy + 4, -6577995);
                }

                yy += 17;
            }
        }

        c.popClip();
        if (this.contentH > this.h) {
            int th = Math.max(16, this.h * this.h / this.contentH);
            int ty = this.y + (int)((long)(this.h - th) * this.scroll / Math.max(1, this.contentH - this.h));
            c.round(this.x + this.w - 5, ty + 2, 3, th - 4, 1, -12762793);
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        int idx = this.indexAt(my);
        if (idx >= 0 && this.contains(mx, my)) {
            this.activate(idx);
        }

        return true;
    }

    private void activate(int idx) {
        PopupMenu.Item it = this.items.get(idx);
        if (it.isEnabled()) {
            if (it.submenu != null) {
                if (this.childIndex != idx) {
                    this.openChild(idx);
                }
            } else {
                this.close();

                for (PopupMenu p = this.parent; p != null; p = p.parent) {
                    p.close();
                }

                if (it.action != null) {
                    it.action.run();
                }
            }
        }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 264 || key == 265) {
            int dir = key == 264 ? 1 : -1;
            int i = this.hoverIndex;

            for (int n = 0; n < this.items.size(); n++) {
                i = (i + dir + this.items.size()) % this.items.size();
                if (!this.items.get(i).separator) {
                    break;
                }
            }

            this.hoverIndex = i;
            this.reveal(i);
            return true;
        } else if (key == 262 && this.hoverIndex >= 0 && this.items.get(this.hoverIndex).submenu != null) {
            this.activate(this.hoverIndex);
            return true;
        } else if (key == 263 && this.parent != null) {
            this.parent.closeChild();
            return true;
        } else if ((key == 257 || key == 335) && this.hoverIndex >= 0) {
            this.activate(this.hoverIndex);
            return true;
        } else {
            return false;
        }
    }

    public static final class Item {
        public final String text;
        public final Icon icon;
        public final String shortcut;
        public final Runnable action;
        public final boolean separator;
        public int iconColor = -6577995;
        public BooleanSupplier enabled;
        public BooleanSupplier checked;
        Consumer<PopupMenu> submenu;

        Item(String text, Icon icon, String shortcut, Runnable action, boolean separator) {
            this.text = text;
            this.icon = icon;
            this.shortcut = shortcut;
            this.action = action;
            this.separator = separator;
        }

        public PopupMenu.Item enabled(BooleanSupplier e) {
            this.enabled = e;
            return this;
        }

        public PopupMenu.Item checked(BooleanSupplier c) {
            this.checked = c;
            return this;
        }

        public PopupMenu.Item color(int c) {
            this.iconColor = c;
            return this;
        }

        boolean isEnabled() {
            return this.enabled == null || this.enabled.getAsBoolean();
        }
    }
}
