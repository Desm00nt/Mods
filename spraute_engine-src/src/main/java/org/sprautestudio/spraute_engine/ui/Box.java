package org.sprautestudio.spraute_engine.ui;

public class Box extends Widget {
    public Box.Dir dir;
    public int gap;
    public int padL;
    public int padT;
    public int padR;
    public int padB;
    public Box.Align align = Box.Align.STRETCH;
    public Box.Align justify = Box.Align.START;
    public int background;
    public int border;
    public int radius;

    public Box(Box.Dir dir) {
        this.dir = dir;
    }

    public static Box row() {
        return new Box(Box.Dir.ROW);
    }

    public static Box column() {
        return new Box(Box.Dir.COLUMN);
    }

    public Box gap(int gap) {
        this.gap = gap;
        return this;
    }

    public Box pad(int all) {
        return this.pad(all, all, all, all);
    }

    public Box pad(int horizontal, int vertical) {
        return this.pad(horizontal, vertical, horizontal, vertical);
    }

    public Box pad(int l, int t, int r, int b) {
        this.padL = l;
        this.padT = t;
        this.padR = r;
        this.padB = b;
        return this;
    }

    public Box align(Box.Align a) {
        this.align = a;
        return this;
    }

    public Box justify(Box.Align a) {
        this.justify = a;
        return this;
    }

    public Box background(int color) {
        this.background = color;
        return this;
    }

    public Box border(int color) {
        this.border = color;
        return this;
    }

    public Box radius(int r) {
        this.radius = r;
        return this;
    }

    @Override
    public void layout() {
        boolean row = this.dir == Box.Dir.ROW;
        int innerX = this.x + this.padL;
        int innerY = this.y + this.padT;
        int innerW = this.w - this.padL - this.padR;
        int innerH = this.h - this.padT - this.padB;
        int main = row ? innerW : innerH;
        int fixed = 0;
        float flexSum = 0.0F;
        int count = 0;

        for (Widget c : this.children) {
            if (c.visible) {
                count++;
                if (c.flex > 0.0F) {
                    flexSum += c.flex;
                } else {
                    fixed += Math.max(0, row ? c.prefW : c.prefH);
                }
            }
        }

        if (count != 0) {
            int gaps = this.gap * (count - 1);
            int remaining = Math.max(0, main - fixed - gaps);
            int pos = row ? innerX : innerY;
            if (flexSum == 0.0F) {
                int used = fixed + gaps;
                if (this.justify == Box.Align.CENTER) {
                    pos += (main - used) / 2;
                } else if (this.justify == Box.Align.END) {
                    pos += main - used;
                }
            }

            float flexAcc = 0.0F;
            int flexUsed = 0;

            for (Widget cx : this.children) {
                if (cx.visible) {
                    int size;
                    if (cx.flex > 0.0F) {
                        flexAcc += cx.flex;
                        int target = Math.round(remaining * flexAcc / flexSum);
                        size = target - flexUsed;
                        flexUsed = target;
                    } else {
                        size = Math.max(0, row ? cx.prefW : cx.prefH);
                    }

                    int cross = row ? innerH : innerW;
                    int pref = row ? cx.prefH : cx.prefW;
                    int cs = this.align != Box.Align.STRETCH && pref >= 0 ? Math.min(cross, pref) : cross;

                    int co = switch (this.align) {
                        case CENTER -> (cross - cs) / 2;
                        case END -> cross - cs;
                        default -> 0;
                    };
                    if (row) {
                        cx.bounds(pos, innerY + co, size, cs);
                    } else {
                        cx.bounds(innerX + co, pos, cs, size);
                    }

                    pos += size + this.gap;
                    cx.layout();
                }
            }
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (this.background != 0 || this.border != 0) {
            if (this.border != 0) {
                c.roundBorder(this.x, this.y, this.w, this.h, this.radius, this.background, this.border);
            } else {
                c.round(this.x, this.y, this.w, this.h, this.radius, this.background);
            }
        }
    }

    public static enum Align {
        STRETCH,
        START,
        CENTER,
        END;
    }

    public static enum Dir {
        ROW,
        COLUMN;
    }
}
