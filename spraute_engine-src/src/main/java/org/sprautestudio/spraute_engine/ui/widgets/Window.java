package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class Window extends Widget implements UI.OverlayLayout, UI.CursorProvider, UI.RaiseOnClick {
    public static final int HEADER = 22;
    private static final int EDGE = 5;
    private static final int MIN_W = 320;
    private static final int MIN_H = 200;
    private final Supplier<String> title;
    private final Icon icon;
    private final int iconColor;
    private final Widget content;
    private int wx;
    private int wy;
    private int ww;
    private int wh;
    private boolean maximized;
    private boolean rolledUp;
    private boolean placed;
    public int insetTop;
    public int insetBottom;
    public Runnable onClose;
    private int mode;
    private int edges;
    private double grabX;
    private double grabY;
    private int[] start;

    public Window(Supplier<String> title, Icon icon, int iconColor, Widget content) {
        this.title = title;
        this.icon = icon;
        this.iconColor = iconColor;
        this.content = this.add(content);
        this.focusable = true;
    }

    public Widget content() {
        return this.content;
    }

    public void open(int vw, int vh) {
        UI ui = UI.get();
        if (ui != null) {
            ui.openOverlay(this, false, false, null);
        }
    }

    public void close() {
        UI ui = UI.get();
        if (ui != null) {
            ui.closeOverlay(this);
        }

        if (this.onClose != null) {
            this.onClose.run();
        }
    }

    public void toFront() {
        UI ui = UI.get();
        if (ui != null) {
            ui.raiseOverlay(this);
        }
    }

    public void toggleMaximize() {
        this.maximized = !this.maximized;
        this.rolledUp = false;
        this.relayout();
    }

    private void relayout() {
        UI ui = UI.get();
        if (ui != null) {
            this.layoutOverlay(ui.vw, ui.vh);
        }
    }

    @Override
    public void layoutOverlay(int vw, int vh) {
        if (!this.placed) {
            this.ww = Math.max(320, vw * 86 / 100);
            this.wh = Math.max(200, (vh - this.insetTop - this.insetBottom) * 88 / 100);
            this.wx = (vw - this.ww) / 2;
            this.wy = this.insetTop + Math.max(0, (vh - this.insetTop - this.insetBottom - this.wh) / 2);
            this.placed = true;
        }

        int avail = vh - this.insetTop - this.insetBottom;
        if (this.maximized) {
            this.bounds(0, this.insetTop, vw, avail);
        } else {
            this.ww = Math.min(this.ww, vw);
            this.wh = Math.min(this.wh, avail);
            this.wx = Math.max(-this.ww + 80, Math.min(vw - 80, this.wx));
            this.wy = Math.max(this.insetTop, Math.min(this.insetTop + avail - 22, this.wy));
            this.bounds(this.wx, this.wy, this.ww, this.rolledUp ? 22 : this.wh);
        }

        this.content.visible = !this.rolledUp;
        this.content.bounds(this.x + 1, this.y + 22, this.w - 2, Math.max(0, this.h - 22 - 1));
        this.content.layout();
    }

    @Override
    public void layout() {
        this.relayout();
    }

    private int buttonX(int i) {
        return this.x + this.w - 22 * (i + 1);
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (!this.maximized) {
            c.shadow(this.x, this.y, this.w, this.h, 6, 12);
        }

        boolean front = this.hasFocusWithin();
        c.roundBorder(this.x, this.y, this.w, this.h, this.maximized ? 0 : 6, -14934490, front ? -11907216 : -12762793);
        c.round(this.x + 1, this.y + 1, this.w - 2, 21, this.maximized ? 0 : 5, front ? -14539462 : -15724266);
        c.fill(this.x + 1, this.y + 22 - 1, this.w - 2, 1, -13749952);
        int tx = this.x + 9;
        if (this.icon != null) {
            c.icon(this.icon, tx, this.y + 6, this.iconColor);
            tx += 14;
        }

        c.textEllipsized(this.title.get(), tx, this.y + 7, this.w - (tx - this.x) - 74, front ? -1841682 : -6577995);
        String[] tips = new String[]{"close", "max", "roll"};

        for (int i = 0; i < tips.length; i++) {
            int bx = this.buttonX(i);
            int by = this.y + 3;
            boolean hov = mx >= bx && mx < bx + 20 && my >= by && my < by + 16 && this.isHoveredDeep();
            if (hov) {
                c.round(bx, by, 20, 16, 3, i == 0 ? -856661894 : 587202559);
            }

            int cx = bx + 6;
            int cy = by + 4;
            int col = hov ? -1 : -6577995;
            switch (i) {
                case 0:
                    c.icon(Icon.CLOSE, cx, cy, col);
                    break;
                case 1:
                    c.outline(cx, cy, 9, 8, col);
                    c.fill(cx, cy, 9, 2, col);
                    break;
                default:
                    c.fill(cx, cy + 6, 9, 2, col);
            }
        }
    }

    @Override
    public Widget hitTest(double mx, double my) {
        if (!this.visible || !this.contains(mx, my)) {
            return null;
        } else {
            return (Widget)(!(my < this.y + 22) && this.edgesAt(mx, my) == 0 ? super.hitTest(mx, my) : this);
        }
    }

    private int edgesAt(double mx, double my) {
        if (!this.maximized && !this.rolledUp) {
            int e = 0;
            if (mx < this.x + 5) {
                e |= 1;
            }

            if (mx >= this.x + this.w - 5) {
                e |= 2;
            }

            if (my < this.y + 5) {
                e |= 4;
            }

            if (my >= this.y + this.h - 5) {
                e |= 8;
            }

            return e;
        } else {
            return 0;
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        this.toFront();
        this.requestFocus();
        if (button != 0) {
            return true;
        } else {
            if (my < this.y + 22) {
                for (int i = 0; i < 3; i++) {
                    int bx = this.buttonX(i);
                    if (mx >= bx && mx < bx + 20 && my >= this.y + 3 && my < this.y + 19) {
                        switch (i) {
                            case 0:
                                this.close();
                                break;
                            case 1:
                                this.toggleMaximize();
                                break;
                            default:
                                this.rolledUp = !this.rolledUp;
                                this.relayout();
                        }

                        return true;
                    }
                }
            }

            int e = this.edgesAt(mx, my);
            this.start = new int[]{this.x, this.y, this.w, this.h};
            this.grabX = mx;
            this.grabY = my;
            if (e != 0) {
                this.mode = 2;
                this.edges = e;
            } else if (my < this.y + 22) {
                this.mode = 1;
            }

            return true;
        }
    }

    @Override
    public boolean mouseDoubleClicked(double mx, double my, int button) {
        if (my < this.y + 22 && mx < this.buttonX(2)) {
            this.toggleMaximize();
            return true;
        } else {
            return this.mousePressed(mx, my, button);
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.mode != 0 && this.start != null) {
            int ddx = (int)Math.round(mx - this.grabX);
            int ddy = (int)Math.round(my - this.grabY);
            if (this.mode == 1) {
                if (this.maximized) {
                    this.maximized = false;
                    double rel = (this.grabX - this.start[0]) / Math.max(1, this.start[2]);
                    this.wx = (int)(mx - this.ww * rel);
                    this.wy = (int)my - 11;
                    this.start = new int[]{this.wx, this.wy, this.ww, this.wh};
                    this.grabX = mx;
                    this.grabY = my;
                } else {
                    this.wx = this.start[0] + ddx;
                    this.wy = this.start[1] + ddy;
                }
            } else {
                int x0 = this.start[0];
                int y0 = this.start[1];
                int x1 = this.start[0] + this.start[2];
                int y1 = this.start[1] + this.start[3];
                if ((this.edges & 1) != 0) {
                    x0 = Math.min(x1 - 320, x0 + ddx);
                }

                if ((this.edges & 2) != 0) {
                    x1 = Math.max(x0 + 320, x1 + ddx);
                }

                if ((this.edges & 4) != 0) {
                    y0 = Math.min(y1 - 200, y0 + ddy);
                }

                if ((this.edges & 8) != 0) {
                    y1 = Math.max(y0 + 200, y1 + ddy);
                }

                this.wx = x0;
                this.wy = y0;
                this.ww = x1 - x0;
                this.wh = y1 - y0;
            }

            this.relayout();
            return true;
        } else {
            return true;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.mode = 0;
        this.start = null;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        return key == 256;
    }

    @Override
    public int cursor(int mx, int my) {
        int e = this.mode == 2 ? this.edges : this.edgesAt(mx, my);
        if (e == 1 || e == 2) {
            return 221189;
        } else if (e == 4 || e == 8) {
            return 221190;
        } else {
            return e != 0 ? 221187 : 221185;
        }
    }
}
