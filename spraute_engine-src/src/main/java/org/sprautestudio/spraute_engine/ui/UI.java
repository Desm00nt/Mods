package org.sprautestudio.spraute_engine.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.sprautestudio.spraute_engine.compat.Gfx;
import org.sprautestudio.spraute_engine.compat.GfxScreen;

public abstract class UI extends GfxScreen {
    private static UI current;
    protected final Widget root = new Widget();
    private final List<UI.Overlay> overlays = new ArrayList<>();
    private final List<UI.Toast> toasts = new ArrayList<>();
    private Widget hovered;
    private Widget focused;
    private Widget pressed;
    private int pressedButton = -1;
    private long lastClickTime;
    private double lastClickX;
    private double lastClickY;
    private int clickCount;
    private long hoverStart;
    private Widget tooltipFor;
    private long lastFrame = System.nanoTime();
    private double uiScale;
    private float factor = 1.0F;
    public int vw;
    public int vh;
    private int mouseVX;
    private int mouseVY;
    private long cursorShape = 0L;
    private int currentCursor = 221185;
    private boolean needsLayout = true;
    public static final int CURSOR_ARROW = 221185;
    public static final int CURSOR_IBEAM = 221186;
    public static final int CURSOR_HAND = 221188;
    public static final int CURSOR_HRESIZE = 221189;
    public static final int CURSOR_VRESIZE = 221190;
    public static final int CURSOR_MOVE = 221187;

    public static UI get() {
        return current;
    }

    protected UI(Component title) {
        super(title);
    }

    public void setUiScale(double physicalScale) {
        this.uiScale = physicalScale;
        this.needsLayout = true;
    }

    public double uiScale() {
        return this.uiScale;
    }

    private void updateScale() {
        double gui = Minecraft.getInstance().getWindow().getGuiScale();
        double phys = this.uiScale <= 0.0 ? gui : this.uiScale;
        this.factor = (float)(phys / gui);
        int nvw = (int)Math.floor(this.width / this.factor);
        int nvh = (int)Math.floor(this.height / this.factor);
        if (nvw != this.vw || nvh != this.vh) {
            this.vw = nvw;
            this.vh = nvh;
            this.needsLayout = true;
        }
    }

    public float factor() {
        return this.factor;
    }

    public void relayout() {
        this.needsLayout = true;
    }

    private void doLayout() {
        this.root.bounds(0, 0, this.vw, this.vh);
        this.root.layout();

        for (UI.Overlay o : this.overlays) {
            if (o.widget instanceof UI.OverlayLayout ol) {
                ol.layoutOverlay(this.vw, this.vh);
            } else {
                o.widget.layout();
            }
        }

        this.needsLayout = false;
    }

    public void raiseOverlay(Widget widget) {
        for (int i = 0; i < this.overlays.size(); i++) {
            UI.Overlay o = this.overlays.get(i);
            if (o.widget == widget && i != this.overlays.size() - 1) {
                this.overlays.remove(i);
                this.overlays.add(o);
                return;
            }
        }
    }

    @Override
    protected void init() {
        current = this;
        this.updateScale();
        this.needsLayout = true;
    }

    @Override
    public void removed() {
        this.setCursor(221185);
        if (current == this) {
            current = null;
        }

        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        current = this;
        this.root.tick();

        for (UI.Overlay o : new ArrayList<>(this.overlays)) {
            o.widget.tick();
        }
    }

    @Override
    public void draw(Gfx g, int mouseX, int mouseY, float partialTick) {
        current = this;
        this.updateScale();
        long now = System.nanoTime();
        float dt = Math.min(0.1F, (float)(now - this.lastFrame) / 1.0E9F);
        this.lastFrame = now;
        if (this.needsLayout) {
            this.doLayout();
        }

        double rawX = Minecraft.getInstance().mouseHandler.xpos() * this.width / Minecraft.getInstance().getWindow().getScreenWidth();
        double rawY = Minecraft.getInstance().mouseHandler.ypos() * this.height / Minecraft.getInstance().getWindow().getScreenHeight();
        this.mouseVX = (int)Math.floor(rawX / this.factor);
        this.mouseVY = (int)Math.floor(rawY / this.factor);
        this.updateHover();
        g.pose().pushPose();
        g.pose().scale(this.factor, this.factor, 1.0F);
        Canvas c = new Canvas(g, this.font, this.factor);
        this.root.render(c, this.mouseVX, this.mouseVY, dt);
        float z = 200.0F;

        for (UI.Overlay o : new ArrayList<>(this.overlays)) {
            clearDepth(c);
            c.pushLayer(z);
            if (o.modal) {
                c.fill(0, 0, this.vw, this.vh, -1728053248);
            }

            o.widget.render(c, this.mouseVX, this.mouseVY, dt);
            c.popLayer();
            z += 50.0F;
        }

        clearDepth(c);
        c.pushLayer(z + 100.0F);
        this.renderToasts(c);
        this.renderTooltip(c);
        c.popLayer();
        g.pose().popPose();
        Widget cursorSource = this.pressed != null ? this.pressed : this.hovered;
        this.setCursor(cursorSource != null && cursorSource instanceof UI.CursorProvider cp ? cp.cursor(this.mouseVX, this.mouseVY) : 221185);
    }

    private static void clearDepth(Canvas c) {
        c.flush();
        RenderSystem.clear(256, Minecraft.ON_OSX);
    }

    private void setCursor(int shape) {
        if (shape != this.currentCursor) {
            this.currentCursor = shape;
            long window = Minecraft.getInstance().getWindow().getWindow();
            if (this.cursorShape != 0L) {
                GLFW.glfwDestroyCursor(this.cursorShape);
                this.cursorShape = 0L;
            }

            if (shape == 221185) {
                GLFW.glfwSetCursor(window, 0L);
            } else {
                this.cursorShape = GLFW.glfwCreateStandardCursor(shape);
                GLFW.glfwSetCursor(window, this.cursorShape);
            }
        }
    }

    private void updateHover() {
        Widget h = this.hitTestAll(this.mouseVX, this.mouseVY);
        if (h != this.hovered) {
            this.hovered = h;
            this.hoverStart = System.currentTimeMillis();
            this.tooltipFor = null;
        }
    }

    private Widget hitTestAll(double mx, double my) {
        for (int i = this.overlays.size() - 1; i >= 0; i--) {
            UI.Overlay o = this.overlays.get(i);
            Widget hit = o.widget.hitTest(mx, my);
            if (hit != null) {
                return hit;
            }

            if (o.modal) {
                return null;
            }
        }

        return this.root.hitTest(mx, my);
    }

    private void renderTooltip(Canvas c) {
        if (this.hovered != null && this.pressed == null) {
            if (System.currentTimeMillis() - this.hoverStart >= 550L) {
                String text = this.hovered.tooltipText(this.mouseVX, this.mouseVY);
                if (text != null && !text.isEmpty()) {
                    int maxW = 260;
                    int tw = 0;

                    for (String line : text.split("\n")) {
                        tw = Math.max(tw, c.width(line));
                    }

                    int w = Math.min(maxW, tw) + 12;
                    int h = c.wrappedHeight(text, w - 12, 2) + 8;
                    int tx = Math.min(this.mouseVX + 10, this.vw - w - 4);
                    int ty = this.mouseVY + 16;
                    if (ty + h > this.vh - 4) {
                        ty = this.mouseVY - h - 6;
                    }

                    c.shadow(tx, ty, w, h, 4, 4);
                    c.roundBorder(tx, ty, w, h, 4, -14210506, -12762793);
                    c.wrapped(text, tx + 6, ty + 5, w - 12, -1841682, 2);
                }
            }
        }
    }

    public void toast(String text, Icon icon, int color) {
        this.toasts.add(new UI.Toast(text, color, icon, System.currentTimeMillis(), 3500L));
        if (this.toasts.size() > 5) {
            this.toasts.remove(0);
        }
    }

    public void toastInfo(String text) {
        this.toast(text, Icon.INFO, -10638858);
    }

    public void toastSuccess(String text) {
        this.toast(text, Icon.CHECK, -11746422);
    }

    public void toastError(String text) {
        this.toast(text, Icon.ERROR, -1023878);
    }

    private void renderToasts(Canvas c) {
        long now = System.currentTimeMillis();
        this.toasts.removeIf(tx -> now - tx.born > tx.life);
        int y = this.vh - 34;

        for (int i = this.toasts.size() - 1; i >= 0; i--) {
            UI.Toast t = this.toasts.get(i);
            float age = (float)(now - t.born) / (float)t.life;
            float alpha = age < 0.06F ? age / 0.06F : (age > 0.85F ? (1.0F - age) / 0.15F : 1.0F);
            int w = Math.min(320, c.width(t.text) + 34);
            int h = 22;
            int x = this.vw - w - 12 + (int)((1.0F - Math.min(1.0F, alpha * 1.5F)) * 30.0F);
            c.shadow(x, y - h, w, h, 5, 4);
            c.round(x, y - h, w, h, 5, Theme.alpha(-14210506, alpha));
            c.fill(x, y - h + 4, 2, h - 8, Theme.alpha(t.color, alpha));
            c.icon(t.icon, x + 9, y - h + 7, Theme.alpha(t.color, alpha));
            c.textEllipsized(t.text, x + 24, y - h + 7, w - 30, Theme.alpha(-1841682, alpha));
            y -= h + 6;
        }
    }

    public UI.Overlay openOverlay(Widget widget, boolean modal, boolean closeOnOutside, Runnable onClose) {
        UI.Overlay o = new UI.Overlay(widget, modal, closeOnOutside, onClose);
        this.overlays.add(o);
        if (widget instanceof UI.OverlayLayout ol) {
            ol.layoutOverlay(this.vw, this.vh);
        } else {
            widget.layout();
        }

        return o;
    }

    public void closeOverlay(Widget widget) {
        Iterator<UI.Overlay> it = this.overlays.iterator();

        while (it.hasNext()) {
            UI.Overlay o = it.next();
            if (o.widget == widget) {
                it.remove();
                if (this.focused != null && widget.isAncestorOf(this.focused)) {
                    this.focused = null;
                }

                if (o.onClose != null) {
                    o.onClose.run();
                }

                return;
            }
        }
    }

    public boolean isOverlayOpen(Widget widget) {
        for (UI.Overlay o : this.overlays) {
            if (o.widget == widget) {
                return true;
            }
        }

        return false;
    }

    public boolean hasModal() {
        for (UI.Overlay o : this.overlays) {
            if (o.modal) {
                return true;
            }
        }

        return false;
    }

    public void closeAllPopups() {
        for (UI.Overlay o : new ArrayList<>(this.overlays)) {
            if (!o.modal) {
                this.closeOverlay(o.widget);
            }
        }
    }

    void onRemoved(Widget w) {
        if (this.focused != null && w.isAncestorOf(this.focused)) {
            this.setFocus(null);
        }

        if (this.hovered != null && w.isAncestorOf(this.hovered)) {
            this.hovered = null;
        }

        if (this.pressed != null && w.isAncestorOf(this.pressed)) {
            this.pressed = null;
        }
    }

    public Widget hovered() {
        return this.hovered;
    }

    public Widget focused() {
        return this.focused;
    }

    public Widget pressed() {
        return this.pressed;
    }

    public int mouseX() {
        return this.mouseVX;
    }

    public int mouseY() {
        return this.mouseVY;
    }

    public void setFocus(Widget w) {
        if (w != this.focused) {
            Widget old = this.focused;
            this.focused = w;
            if (old != null) {
                old.focusLost();
            }

            if (w != null) {
                w.focusGained();
            }
        }
    }

    private double vx(double mx) {
        return mx / this.factor;
    }

    private double vy(double my) {
        return my / this.factor;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = this.vx(mouseX);
        double my = this.vy(mouseY);
        long now = System.currentTimeMillis();
        if (now - this.lastClickTime < 350L && Math.abs(mx - this.lastClickX) < 4.0 && Math.abs(my - this.lastClickY) < 4.0) {
            this.clickCount++;
        } else {
            this.clickCount = 1;
        }

        this.lastClickTime = now;
        this.lastClickX = mx;
        this.lastClickY = my;

        for (int i = this.overlays.size() - 1; i >= 0; i--) {
            UI.Overlay o = this.overlays.get(i);
            Widget hit = o.widget.hitTest(mx, my);
            if (hit != null) {
                if (o.widget instanceof UI.RaiseOnClick) {
                    this.raiseOverlay(o.widget);
                }

                return this.dispatchPress(hit, mx, my, button);
            }

            if (o.owner != null && o.owner.contains(mx, my)) {
                this.closeOverlay(o.widget);
                return true;
            }

            if (o.closeOnOutside) {
                this.closeOverlay(o.widget);
                if (o.modal) {
                    return true;
                }
            } else if (o.modal) {
                return true;
            }
        }

        Widget hitx = this.root.hitTest(mx, my);
        if (hitx == null) {
            this.setFocus(null);
            return false;
        } else {
            return this.dispatchPress(hitx, mx, my, button);
        }
    }

    public int clickCount() {
        return this.clickCount;
    }

    private boolean dispatchPress(Widget hit, double mx, double my, int button) {
        Widget focusTarget = hit;

        while (focusTarget != null && !focusTarget.focusable) {
            focusTarget = focusTarget.parent;
        }

        if (focusTarget != null) {
            this.setFocus(focusTarget);
        } else if (this.focused != null && !this.focused.isAncestorOf(hit) && this.root.isAncestorOf(hit)) {
            this.setFocus(null);
        }

        for (Widget w = hit; w != null; w = w.parent) {
            if (w.enabled) {
                boolean consumed = this.clickCount >= 2 && w.mouseDoubleClicked(mx, my, button);
                if (!consumed) {
                    consumed = w.mousePressed(mx, my, button);
                }

                if (consumed) {
                    this.pressed = w;
                    this.pressedButton = button;
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double mx = this.vx(mouseX);
        double my = this.vy(mouseY);
        Widget p = this.pressed;
        if (p != null && button == this.pressedButton) {
            this.pressed = null;
            this.pressedButton = -1;
            p.mouseReleased(mx, my, button);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return this.pressed != null && button == this.pressedButton
            ? this.pressed.mouseDragged(this.vx(mouseX), this.vy(mouseY), button, dragX / this.factor, dragY / this.factor)
            : false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double mx = this.vx(mouseX);
        double my = this.vy(mouseY);
        Widget hit = this.hitTestAll(mx, my);

        for (Widget w = hit; w != null; w = w.parent) {
            if (w.mouseScrolled(mx, my, delta)) {
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        for (Widget w = this.focused; w != null; w = w.parent) {
            if (w.enabled && w.keyPressed(key, scan, mods)) {
                return true;
            }
        }

        if (key == 256 && !this.overlays.isEmpty()) {
            UI.Overlay top = this.overlays.get(this.overlays.size() - 1);
            this.closeOverlay(top.widget);
            return true;
        } else {
            return this.onShortcut(key, mods);
        }
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        for (Widget w = this.focused; w != null; w = w.parent) {
            if (w.enabled && w.charTyped(ch, mods)) {
                return true;
            }
        }

        return false;
    }

    protected abstract boolean onShortcut(int var1, int var2);

    public static boolean ctrl(int mods) {
        return (mods & 2) != 0;
    }

    public static boolean shift(int mods) {
        return (mods & 1) != 0;
    }

    public static boolean alt(int mods) {
        return (mods & 4) != 0;
    }

    public static String clipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void setClipboard(String s) {
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
    }

    public interface CursorProvider {
        int cursor(int var1, int var2);
    }

    public static final class Overlay {
        public final Widget widget;
        public final boolean modal;
        public final boolean closeOnOutside;
        public final Runnable onClose;
        public Widget owner;

        Overlay(Widget widget, boolean modal, boolean closeOnOutside, Runnable onClose) {
            this.widget = widget;
            this.modal = modal;
            this.closeOnOutside = closeOnOutside;
            this.onClose = onClose;
        }
    }

    public interface OverlayLayout {
        void layoutOverlay(int var1, int var2);
    }

    public interface RaiseOnClick {
    }

    private record Toast(String text, int color, Icon icon, long born, long life) {
    }
}
