package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class TextField extends Widget implements UI.CursorProvider {
    private String text = "";
    private int caret;
    private int anchor;
    private int scroll;
    public String placeholder = "";
    public Icon icon;
    public Consumer<String> onChange;
    public Consumer<String> onSubmit;
    public Runnable onEscape;
    public TextField.KeyHook keyHook;
    public Predicate<Character> filter;
    public Consumer<PopupMenu> contextMenu;
    public int maxLength = 1000;
    public boolean borderless;
    public int textColor = -1841682;
    private long blinkStart = System.currentTimeMillis();

    public TextField() {
        this.focusable = true;
        this.prefH = 18;
    }

    public TextField placeholder(String p) {
        this.placeholder = p;
        return this;
    }

    public TextField icon(Icon i) {
        this.icon = i;
        return this;
    }

    public String text() {
        return this.text;
    }

    public void setText(String s) {
        this.text = s == null ? "" : s;
        this.caret = this.anchor = this.text.length();
        this.scroll = 0;
    }

    public void selectAll() {
        this.anchor = 0;
        this.caret = this.text.length();
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    private int textX() {
        return this.x + (this.borderless ? 2 : 5) + (this.icon != null ? 13 : 0);
    }

    private int innerW() {
        return this.w - (this.textX() - this.x) - (this.borderless ? 2 : 5);
    }

    @Override
    public int cursor(int mx, int my) {
        return 221186;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        boolean focused = this.isFocused();
        if (!this.borderless) {
            int border = focused ? -7639809 : Theme.lerp(-13749952, -12762793, this.hoverAnim);
            c.roundBorder(this.x, this.y, this.w, this.h, 3, -15592678, border);
        }

        if (this.icon != null) {
            c.icon(this.icon, this.x + 5, this.y + (this.h - 9) / 2, -9933435);
        }

        int tx = this.textX();
        int ty = this.y + (this.h - 8) / 2;
        this.ensureCaretVisible();
        c.pushClip(tx, this.y, this.innerW() + 1, this.h);
        if (this.text.isEmpty() && !this.placeholder.isEmpty()) {
            c.text(this.placeholder, tx, ty, -9933435);
        }

        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        if (from != to) {
            int sx = tx - this.scroll + this.font().width(this.text.substring(0, from));
            int ex = tx - this.scroll + this.font().width(this.text.substring(0, to));
            c.fill(sx, ty - 1, ex - sx, 10, focused ? 1430088664 : 859663320);
        }

        c.text(this.text, tx - this.scroll, ty, this.enabled ? this.textColor : -9933435);
        if (focused && (System.currentTimeMillis() - this.blinkStart) / 530L % 2L == 0L) {
            int cx = tx - this.scroll + this.font().width(this.text.substring(0, this.caret));
            c.fill(cx, ty - 1, 1, 10, -1);
        }

        c.popClip();
    }

    private void ensureCaretVisible() {
        int cx = this.font().width(this.text.substring(0, this.caret));
        int iw = this.innerW();
        if (cx - this.scroll > iw) {
            this.scroll = cx - iw;
        }

        if (cx - this.scroll < 0) {
            this.scroll = cx;
        }

        int total = this.font().width(this.text);
        if (total - this.scroll < iw) {
            this.scroll = Math.max(0, total - iw);
        }
    }

    private int indexAt(double mx) {
        double rel = mx - this.textX() + this.scroll;
        int best = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i <= this.text.length(); i++) {
            double d = Math.abs(this.font().width(this.text.substring(0, i)) - rel);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }

        return best;
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (button == 1 && this.contextMenu != null) {
            PopupMenu pm = new PopupMenu();
            this.contextMenu.accept(pm);
            pm.show((int)mx, (int)my);
            return true;
        } else if (button != 0) {
            return false;
        } else {
            int idx = this.indexAt(mx);
            if (Minecraft.getInstance().screen != null && Screen.hasShiftDown()) {
                this.caret = idx;
            } else {
                this.caret = this.anchor = idx;
            }

            this.blinkStart = System.currentTimeMillis();
            return true;
        }
    }

    @Override
    public boolean mouseDoubleClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        } else {
            int idx = this.indexAt(mx);
            this.anchor = this.wordLeft(idx);
            this.caret = this.wordRight(idx);
            return true;
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        this.caret = this.indexAt(mx);
        return true;
    }

    private void changed() {
        this.blinkStart = System.currentTimeMillis();
        if (this.onChange != null) {
            this.onChange.accept(this.text);
        }
    }

    public void insert(String s) {
        StringBuilder clean = new StringBuilder();

        for (char ch : s.toCharArray()) {
            if (ch != '\n' && ch != '\r' && (this.filter == null || this.filter.test(ch))) {
                clean.append(ch);
            }
        }

        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        String next = this.text.substring(0, from) + clean + this.text.substring(to);
        if (next.length() <= this.maxLength) {
            this.text = next;
            this.caret = this.anchor = from + clean.length();
            this.changed();
        }
    }

    private void deleteRange(int from, int to) {
        if (from != to) {
            this.text = this.text.substring(0, from) + this.text.substring(to);
            this.caret = this.anchor = from;
            this.changed();
        }
    }

    private int wordLeft(int i) {
        while (i > 0 && !Character.isLetterOrDigit(this.text.charAt(i - 1))) {
            i--;
        }

        while (i > 0 && Character.isLetterOrDigit(this.text.charAt(i - 1))) {
            i--;
        }

        return i;
    }

    private int wordRight(int i) {
        while (i < this.text.length() && !Character.isLetterOrDigit(this.text.charAt(i))) {
            i++;
        }

        while (i < this.text.length() && Character.isLetterOrDigit(this.text.charAt(i))) {
            i++;
        }

        return i;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (this.keyHook != null && this.keyHook.onKey(key, mods)) {
            return true;
        } else {
            boolean shift = UI.shift(mods);
            boolean ctrl = UI.ctrl(mods);
            int from = Math.min(this.caret, this.anchor);
            int to = Math.max(this.caret, this.anchor);
            switch (key) {
                case 65:
                    if (!ctrl) {
                        return false;
                    }

                    this.selectAll();
                    break;
                case 67:
                    if (!ctrl) {
                        return false;
                    }

                    if (from != to) {
                        UI.setClipboard(this.text.substring(from, to));
                    }
                    break;
                case 86:
                    if (!ctrl) {
                        return false;
                    }

                    this.insert(UI.clipboard());
                    break;
                case 88:
                    if (!ctrl) {
                        return false;
                    }

                    if (from != to) {
                        UI.setClipboard(this.text.substring(from, to));
                        this.deleteRange(from, to);
                    }
                    break;
                case 256:
                    if (this.onEscape != null) {
                        this.onEscape.run();
                        return true;
                    }

                    return false;
                case 257:
                case 335:
                    if (this.onSubmit != null) {
                        this.onSubmit.accept(this.text);
                        return true;
                    }

                    return false;
                case 259:
                    if (from != to) {
                        this.deleteRange(from, to);
                    } else if (this.caret > 0) {
                        this.deleteRange(ctrl ? this.wordLeft(this.caret) : this.caret - 1, this.caret);
                    }
                    break;
                case 261:
                    if (from != to) {
                        this.deleteRange(from, to);
                    } else if (this.caret < this.text.length()) {
                        this.deleteRange(this.caret, ctrl ? this.wordRight(this.caret) : this.caret + 1);
                    }
                    break;
                case 262:
                    int nx = ctrl ? this.wordRight(this.caret) : (!shift && from != to ? to : Math.min(this.text.length(), this.caret + 1));
                    this.caret = nx;
                    if (!shift) {
                        this.anchor = this.caret;
                    }
                    break;
                case 263:
                    int n = ctrl ? this.wordLeft(this.caret) : (!shift && from != to ? from : Math.max(0, this.caret - 1));
                    this.caret = n;
                    if (!shift) {
                        this.anchor = this.caret;
                    }
                    break;
                case 268:
                    this.caret = 0;
                    if (!shift) {
                        this.anchor = this.caret;
                    }
                    break;
                case 269:
                    this.caret = this.text.length();
                    if (!shift) {
                        this.anchor = this.caret;
                    }
                    break;
                default:
                    return false;
            }

            this.blinkStart = System.currentTimeMillis();
            return true;
        }
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (ch >= ' ' && ch != 127) {
            this.insert(String.valueOf(ch));
            return true;
        } else {
            return false;
        }
    }

    public interface KeyHook {
        boolean onKey(int var1, int var2);
    }
}
