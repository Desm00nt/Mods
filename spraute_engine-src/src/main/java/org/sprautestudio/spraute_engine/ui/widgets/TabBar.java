package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class TabBar extends Widget implements UI.CursorProvider {
    private final List<TabBar.Tab> tabs = new ArrayList<>();
    private int selected = -1;
    private double scrollX;
    public boolean editorStyle = true;
    public Consumer<TabBar.Tab> onSelect;
    public Consumer<TabBar.Tab> onClose;
    public TabBar.TabContext onContext;

    public List<TabBar.Tab> tabs() {
        return this.tabs;
    }

    public TabBar.Tab add(TabBar.Tab t) {
        this.tabs.add(t);
        return t;
    }

    public void removeTab(TabBar.Tab t) {
        int idx = this.tabs.indexOf(t);
        if (idx >= 0) {
            this.tabs.remove(idx);
            if (this.selected >= this.tabs.size()) {
                this.selected = this.tabs.size() - 1;
            } else if (idx < this.selected) {
                this.selected--;
            }
        }
    }

    public TabBar.Tab selected() {
        return this.selected >= 0 && this.selected < this.tabs.size() ? this.tabs.get(this.selected) : null;
    }

    public void select(TabBar.Tab t) {
        this.selected = this.tabs.indexOf(t);
        if (t != null && this.onSelect != null) {
            this.onSelect.accept(t);
        }
    }

    public TabBar.Tab find(Object data) {
        for (TabBar.Tab t : this.tabs) {
            if (t.data == data || t.data != null && t.data.equals(data)) {
                return t;
            }
        }

        return null;
    }

    private int tabWidth(Canvas c, TabBar.Tab t) {
        int tw = c != null ? c.width(t.title.get()) : Minecraft.getInstance().font.width(t.title.get());
        int wsum = tw + 16 + (t.icon != null ? 13 : 0) + (t.closable ? 14 : 0);
        if (t.badge != null && t.badge.get() != null && !t.badge.get().isEmpty()) {
            wsum += Minecraft.getInstance().font.width(t.badge.get()) + 8;
        }

        return Math.min(this.editorStyle ? 180 : 160, wsum);
    }

    private int tabX(int index) {
        int xx = this.x - (int)this.scrollX;

        for (int i = 0; i < index; i++) {
            xx += this.tabWidth(null, this.tabs.get(i));
        }

        return xx;
    }

    private int indexAt(double mx) {
        int xx = this.x - (int)this.scrollX;

        for (int i = 0; i < this.tabs.size(); i++) {
            int tw = this.tabWidth(null, this.tabs.get(i));
            if (mx >= xx && mx < xx + tw) {
                return i;
            }

            xx += tw;
        }

        return -1;
    }

    private boolean onCloseButton(int index, double mx) {
        TabBar.Tab t = this.tabs.get(index);
        if (!t.closable) {
            return false;
        } else {
            int right = this.tabX(index) + this.tabWidth(null, t);
            return mx >= right - 16 && mx < right - 3;
        }
    }

    @Override
    public int cursor(int mx, int my) {
        return this.indexAt(mx) >= 0 ? 221188 : 221185;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, this.editorStyle ? -15724266 : -14934490);
        c.fill(this.x, this.y + this.h - 1, this.w, 1, -13749952);
        int hov = this.isHovered() ? this.indexAt(mx) : -1;
        c.pushClip(this.x, this.y, this.w, this.h);

        for (int i = 0; i < this.tabs.size(); i++) {
            TabBar.Tab t = this.tabs.get(i);
            int tx = this.tabX(i);
            int tw = this.tabWidth(c, t);
            if (tx <= this.x + this.w && tx + tw >= this.x) {
                boolean sel = i == this.selected;
                if (this.editorStyle) {
                    if (sel) {
                        c.fill(tx, this.y, tw, this.h, -15461093);
                        c.fill(tx, this.y, tw, 2, -7639809);
                    } else if (i == hov) {
                        c.fill(tx, this.y, tw, this.h - 1, -14934490);
                    }

                    c.fill(tx + tw - 1, this.y + 4, 1, this.h - 8, -13749952);
                } else if (sel) {
                    c.fill(tx + 4, this.y + this.h - 2, tw - 8, 2, -7639809);
                } else if (i == hov) {
                    c.fill(tx + 4, this.y + this.h - 2, tw - 8, 1, -12762793);
                }

                int ix = tx + 8;
                int ty = this.y + (this.h - 8) / 2;
                if (t.icon != null) {
                    c.icon(t.icon, ix, this.y + (this.h - 9) / 2, t.iconColor);
                    ix += 13;
                }

                String badge = t.badge != null ? t.badge.get() : null;
                int badgeW = badge != null && !badge.isEmpty() ? c.width(badge) + 8 : 0;
                int titleW = tw - (ix - tx) - (t.closable ? 16 : 6) - badgeW;
                c.textEllipsized(t.title.get(), ix, ty, titleW, sel ? -1841682 : -6577995);
                if (badgeW > 0) {
                    int bx = ix + Math.min(titleW, c.width(t.title.get())) + 4;
                    c.round(bx, ty - 2, badgeW - 2, 11, 4, Theme.alpha(t.badgeColor, 0.25F));
                    c.text(badge, bx + 3, ty, t.badgeColor);
                }

                if (t.closable) {
                    int cx = tx + tw - 14;
                    boolean dirty = t.dirty != null && t.dirty.getAsBoolean();
                    boolean hotClose = i == hov && this.onCloseButton(i, mx);
                    if (hotClose) {
                        c.round(cx - 2, this.y + (this.h - 13) / 2, 13, 13, 3, 352321535);
                    }

                    if (dirty && !hotClose) {
                        c.icon(Icon.DOT, cx, this.y + (this.h - 9) / 2, -1841682);
                    } else if (sel || i == hov) {
                        c.icon(Icon.CLOSE, cx, this.y + (this.h - 9) / 2, hotClose ? -1841682 : -9933435);
                    }
                }
            }
        }

        c.popClip();
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        int idx = this.indexAt(mx);
        if (idx < 0) {
            return false;
        } else {
            TabBar.Tab t = this.tabs.get(idx);
            if (button == 2 && t.closable) {
                if (this.onClose != null) {
                    this.onClose.accept(t);
                }

                return true;
            } else if (button == 1) {
                this.select(t);
                if (this.onContext != null) {
                    this.onContext.open(t, (int)mx, (int)my);
                }

                return true;
            } else if (button == 0 && this.onCloseButton(idx, mx)) {
                if (this.onClose != null) {
                    this.onClose.accept(t);
                }

                return true;
            } else {
                this.select(t);
                return true;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int total = 0;

        for (TabBar.Tab t : this.tabs) {
            total += this.tabWidth(null, t);
        }

        this.scrollX = Math.max(0.0, Math.min((double)Math.max(0, total - this.w), this.scrollX - delta * 30.0));
        return true;
    }

    public static final class Tab {
        public Supplier<String> title;
        public Icon icon;
        public int iconColor = -6577995;
        public Object data;
        public boolean closable;
        public BooleanSupplier dirty;
        public Supplier<String> badge;
        public int badgeColor = -9933435;

        public Tab(Supplier<String> title, Icon icon, Object data) {
            this.title = title;
            this.icon = icon;
            this.data = data;
        }
    }

    public interface TabContext {
        void open(TabBar.Tab var1, int var2, int var3);
    }
}
