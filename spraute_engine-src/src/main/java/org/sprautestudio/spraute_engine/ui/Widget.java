package org.sprautestudio.spraute_engine.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class Widget {
    public int x;
    public int y;
    public int w;
    public int h;
    public int prefW = -1;
    public int prefH = -1;
    public float flex;
    public boolean visible = true;
    public boolean enabled = true;
    public boolean focusable;
    public boolean clipChildren = true;
    protected Widget parent;
    protected final List<Widget> children = new ArrayList<>();
    private Supplier<String> tooltip;
    protected float hoverAnim;

    public <T extends Widget> T add(T child) {
        if (child.parent != null) {
            child.parent.remove(child);
        }

        child.parent = this;
        this.children.add(child);
        return child;
    }

    public <T extends Widget> T add(int index, T child) {
        if (child.parent != null) {
            child.parent.remove(child);
        }

        child.parent = this;
        this.children.add(Math.max(0, Math.min(index, this.children.size())), child);
        return child;
    }

    public void remove(Widget child) {
        if (this.children.remove(child)) {
            child.parent = null;
            UI ui = UI.get();
            if (ui != null) {
                ui.onRemoved(child);
            }
        }
    }

    public void clear() {
        for (Widget c : new ArrayList<>(this.children)) {
            this.remove(c);
        }
    }

    public List<Widget> children() {
        return Collections.unmodifiableList(this.children);
    }

    public Widget parent() {
        return this.parent;
    }

    public boolean isAncestorOf(Widget w) {
        for (Widget p = w; p != null; p = p.parent) {
            if (p == this) {
                return true;
            }
        }

        return false;
    }

    public boolean isShown() {
        for (Widget p = this; p != null; p = p.parent) {
            if (!p.visible) {
                return false;
            }
        }

        return true;
    }

    public Widget bounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = Math.max(0, w);
        this.h = Math.max(0, h);
        return this;
    }

    public Widget size(int w, int h) {
        this.prefW = w;
        this.prefH = h;
        return this;
    }

    public Widget flex(float f) {
        this.flex = f;
        return this;
    }

    public Widget tooltip(String text) {
        this.tooltip = text == null ? null : () -> text;
        return this;
    }

    public Widget tooltip(Supplier<String> text) {
        this.tooltip = text;
        return this;
    }

    public String tooltipText(int mx, int my) {
        return this.tooltip == null ? null : this.tooltip.get();
    }

    public boolean contains(double mx, double my) {
        return mx >= this.x && my >= this.y && mx < this.x + this.w && my < this.y + this.h;
    }

    public Widget hitTest(double mx, double my) {
        if (this.visible && this.contains(mx, my)) {
            for (int i = this.children.size() - 1; i >= 0; i--) {
                Widget hit = this.children.get(i).hitTest(mx, my);
                if (hit != null) {
                    return hit;
                }
            }

            return this;
        } else {
            return null;
        }
    }

    public void layout() {
        for (Widget c : this.children) {
            c.layout();
        }
    }

    public void render(Canvas c, int mx, int my, float dt) {
        if (this.visible) {
            boolean hovered = this.isHovered();
            this.hoverAnim += (hovered ? 1 : -1) * dt * 8.0F;
            this.hoverAnim = Math.max(0.0F, Math.min(1.0F, this.hoverAnim));
            this.paint(c, mx, my, dt);
            if (!this.children.isEmpty()) {
                if (this.clipChildren) {
                    c.pushClip(this.x, this.y, this.w, this.h);
                }

                for (Widget child : new ArrayList<>(this.children)) {
                    child.render(c, mx, my, dt);
                }

                if (this.clipChildren) {
                    c.popClip();
                }
            }

            this.paintOver(c, mx, my, dt);
        }
    }

    protected void paint(Canvas c, int mx, int my, float dt) {
    }

    protected void paintOver(Canvas c, int mx, int my, float dt) {
    }

    public boolean isHovered() {
        UI ui = UI.get();
        return ui != null && ui.hovered() == this;
    }

    public boolean isHoveredDeep() {
        UI ui = UI.get();
        return ui != null && ui.hovered() != null && this.isAncestorOf(ui.hovered());
    }

    public boolean isFocused() {
        UI ui = UI.get();
        return ui != null && ui.focused() == this;
    }

    public boolean hasFocusWithin() {
        UI ui = UI.get();
        return ui != null && ui.focused() != null && this.isAncestorOf(ui.focused());
    }

    public void requestFocus() {
        UI ui = UI.get();
        if (ui != null) {
            ui.setFocus(this);
        }
    }

    public boolean isPressed() {
        UI ui = UI.get();
        return ui != null && ui.pressed() == this;
    }

    public boolean mousePressed(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return false;
    }

    public boolean mouseDoubleClicked(double mx, double my, int button) {
        return false;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        return false;
    }

    public boolean charTyped(char ch, int mods) {
        return false;
    }

    public void focusGained() {
    }

    public void focusLost() {
    }

    public void tick() {
        for (Widget c : new ArrayList<>(this.children)) {
            c.tick();
        }
    }
}
