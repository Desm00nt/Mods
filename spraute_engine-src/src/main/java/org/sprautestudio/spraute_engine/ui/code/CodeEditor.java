package org.sprautestudio.spraute_engine.ui.code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Theme;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Dialog;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;

public class CodeEditor extends Widget implements UI.CursorProvider {
    public String emptyHint = "Начните писать сюжет… (Ctrl+Space — подсказки)";
    public static final int LINE_H = 12;
    private static final int PAD_TOP = 4;
    private static final int MINIMAP_W = 64;
    private TextDocument doc;
    private LanguageSupport lang;
    private LanguageSupport.Analysis analysis;
    private int analyzedVersion = -1;
    private int caret;
    private int anchor;
    private int preferredX = -1;
    private double scrollX;
    private double scrollY;
    private double targetScrollY;
    private long blinkStart = System.currentTimeMillis();
    public boolean readOnly;
    public boolean showMinimap = true;
    public Consumer<CodeEditor> onChange;
    public Consumer<CodeEditor> onCaretMove;
    public Runnable onSave;
    public Supplier<String> positionProvider;
    public Consumer<PopupMenu> contextExtras;
    private final CompletionPopup popup = new CompletionPopup(this::acceptCompletion);
    private boolean popupOpen;
    private boolean popupExplicit;
    private LanguageSupport.Signature signature;
    private final FindBar findBar = new FindBar(this);
    private final List<int[]> matches = new ArrayList<>();
    private int currentMatch = -1;
    private List<int[]> occurrences = new ArrayList<>();
    private int occurrenceVersion = -1;
    private int occurrenceCaret = -1;
    private boolean draggingSelection;
    private boolean draggingMinimap;
    private int dragMode;
    private int dragOriginStart;
    private int dragOriginEnd;
    private List<int[]> snippetStops;
    private int snippetIndex;
    public int menuX;
    public int menuY;
    public BiFunction<String, Integer, List<CodeEditor.Chip>> chipProvider;
    private final Map<Integer, List<CodeEditor.Chip>> chipCache = new HashMap<>();
    private int chipCacheFor;
    private final List<Object[]> chipHits = new ArrayList<>();
    private static final String OPEN = "([{\"";
    private static final String CLOSE = ")]}\"";
    private int popupStartOffset;
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{(\\d+):([^}]*)}|\\$(\\d+)");

    public CodeEditor(String text, LanguageSupport lang) {
        this.doc = new TextDocument(text);
        this.lang = lang;
        this.focusable = true;
        this.add(this.findBar);
        this.reanalyze();
    }

    public String text() {
        return this.doc.text();
    }

    public TextDocument document() {
        return this.doc;
    }

    public void setText(String text) {
        this.doc = new TextDocument(text);
        this.caret = this.anchor = 0;
        this.scrollY = this.targetScrollY = this.scrollX = 0.0;
        this.reanalyze();
    }

    public void replaceAllText(String text) {
        int c = this.caret;
        this.doc.setText(text, c);
        this.caret = this.anchor = Math.min(c, this.doc.length());
        this.changed();
    }

    public boolean isDirty() {
        return this.doc.isDirty();
    }

    public void markSaved() {
        this.doc.markSaved();
    }

    public LanguageSupport.Analysis analysis() {
        this.ensureAnalysis();
        return this.analysis;
    }

    public List<LanguageSupport.Problem> problems() {
        this.ensureAnalysis();
        return this.analysis == null ? List.of() : this.analysis.problems();
    }

    public int caret() {
        return this.caret;
    }

    public int caretLine() {
        return this.doc.lineOf(this.caret);
    }

    public int caretCol() {
        return this.doc.colOf(this.caret);
    }

    public int selectionLength() {
        return Math.abs(this.caret - this.anchor);
    }

    public String selectedText() {
        return this.doc.substring(Math.min(this.caret, this.anchor), Math.max(this.caret, this.anchor));
    }

    public void gotoOffset(int offset, int selectLength) {
        offset = this.clamp(offset);
        this.anchor = offset;
        this.caret = this.clamp(offset + selectLength);
        this.preferredX = -1;
        this.revealCaret(true);
        this.requestFocus();
        this.caretMoved();
    }

    public void gotoLine(int line) {
        this.gotoOffset(this.doc.lineStart(Math.max(0, Math.min(this.doc.lineCount() - 1, line))), 0);
    }

    public String lineText(int line) {
        return this.doc.line(line);
    }

    public void replaceLine(int line, String text) {
        if (!this.readOnly) {
            this.anchor = this.doc.lineStart(line);
            this.caret = this.doc.lineEnd(line);
            this.replaceSelection(text, 0);
        }
    }

    public void insertAtCaret(String s) {
        if (!this.readOnly) {
            if (s.contains("${")) {
                this.insertSnippet(Math.min(this.caret, this.anchor), Math.max(this.caret, this.anchor), s);
            } else {
                this.replaceSelection(s, 0);
            }

            this.requestFocus();
        }
    }

    public void insertLine(String statement) {
        if (!this.readOnly) {
            int ln = this.doc.lineOf(this.caret);
            String cur = this.doc.line(ln);
            int ws = 0;

            while (ws < cur.length() && (cur.charAt(ws) == ' ' || cur.charAt(ws) == '\t')) {
                ws++;
            }

            String indent = cur.substring(0, ws);
            if (cur.endsWith("{")) {
                indent = indent + "    ";
            }

            if (cur.isBlank()) {
                this.gotoOffset(this.doc.lineStart(ln), cur.length());
                this.replaceSelection(indent + statement, 0);
            } else {
                this.gotoOffset(this.doc.lineEnd(ln), 0);
                this.replaceSelection("\n" + indent + statement, 0);
            }

            this.doc.breakUndoGroup();
            this.requestFocus();
        }
    }

    public boolean canUndo() {
        return this.doc.canUndo();
    }

    public boolean canRedo() {
        return this.doc.canRedo();
    }

    public void undo() {
        int[] sel = this.doc.undo();
        if (sel != null) {
            this.caret = this.clamp(sel[0]);
            this.anchor = this.clamp(sel[1]);
            this.afterExternalEdit();
        }
    }

    public void redo() {
        int[] sel = this.doc.redo();
        if (sel != null) {
            this.caret = this.clamp(sel[0]);
            this.anchor = this.clamp(sel[1]);
            this.afterExternalEdit();
        }
    }

    private void afterExternalEdit() {
        this.snippetStops = null;
        this.closePopup();
        this.reanalyze();
        this.revealCaret(true);
        if (this.onChange != null) {
            this.onChange.accept(this);
        }

        this.caretMoved();
    }

    public void openFind(boolean replace) {
        this.findBar.show(replace, this.selectionLength() > 0 ? this.selectedText() : this.findBar.find.text());
    }

    public void promptGotoLine() {
        Dialog.prompt("Перейти к строке", "Номер строки (1–" + this.doc.lineCount() + "):", String.valueOf(this.caretLine() + 1), v -> {
            try {
                int n = Integer.parseInt(v);
                return n >= 1 && n <= this.doc.lineCount() ? null : "Нет такой строки";
            } catch (NumberFormatException var3) {
                return "Введите число";
            }
        }, v -> this.gotoLine(Integer.parseInt(v) - 1));
    }

    public void format() {
        if (!this.readOnly) {
            String formatted = this.lang.format(this.doc.text());
            if (formatted != null && !formatted.equals(this.doc.text())) {
                int line = this.caretLine();
                int col = this.caretCol();
                this.doc.setText(formatted, this.caret);
                this.caret = this.anchor = this.doc.offsetOf(line, col);
                this.changed();
            }
        }
    }

    private void reanalyze() {
        this.analysis = this.lang.analyze(this.doc.text());
        this.analyzedVersion = this.doc.version();
    }

    private void ensureAnalysis() {
        if (this.analyzedVersion != this.doc.version()) {
            this.reanalyze();
        }
    }

    private void changed() {
        this.reanalyze();
        this.blinkStart = System.currentTimeMillis();
        this.updateSnippetAfterEdit();
        if (this.findBar.visible) {
            this.updateMatches(false);
        }

        if (this.onChange != null) {
            this.onChange.accept(this);
        }

        this.caretMoved();
    }

    private void caretMoved() {
        this.blinkStart = System.currentTimeMillis();
        this.ensureAnalysis();
        this.signature = !this.hasFocusWithin() && !this.isFocused() ? null : this.lang.signature(this.doc.text(), this.caret, this.analysis);
        if (this.snippetStops != null && !this.insideSnippet()) {
            this.snippetStops = null;
        }

        if (this.onCaretMove != null) {
            this.onCaretMove.accept(this);
        }
    }

    private Font font() {
        return Minecraft.getInstance().font;
    }

    private int gutterW() {
        int digits = Math.max(2, String.valueOf(this.doc.lineCount()).length());
        return digits * 6 + 22;
    }

    private boolean minimapShown() {
        return this.showMinimap && this.w > 360;
    }

    private int textLeft() {
        return this.x + this.gutterW() + 6;
    }

    private int textAreaW() {
        return this.w - this.gutterW() - 6 - (this.minimapShown() ? 64 : 0) - 6;
    }

    private int visibleLines() {
        return Math.max(1, (this.h - 4) / 12);
    }

    private int maxScrollY() {
        return Math.max(0, this.doc.lineCount() * 12 - this.h + 4 + 36);
    }

    private int lineY(int line) {
        return this.y + 4 + line * 12 - (int)Math.round(this.scrollY);
    }

    private int colX(int line, int col) {
        String l = this.doc.line(line);
        col = Math.min(col, l.length());
        return this.textLeft() - (int)Math.round(this.scrollX) + this.font().width(l.substring(0, col));
    }

    private int offsetAt(double mx, double my) {
        int line = (int)Math.floor((my - this.y - 4.0 + this.scrollY) / 12.0);
        line = Math.max(0, Math.min(this.doc.lineCount() - 1, line));
        String l = this.doc.line(line);
        double rel = mx - this.textLeft() + this.scrollX;
        int acc = 0;

        for (int i = 0; i < l.length(); i++) {
            int cw = this.font().width(String.valueOf(l.charAt(i)));
            if (rel < acc + cw / 2.0) {
                return this.doc.lineStart(line) + i;
            }

            acc += cw;
        }

        return this.doc.lineEnd(line);
    }

    private int clamp(int o) {
        return Math.max(0, Math.min(this.doc.length(), o));
    }

    public void revealCaret(boolean center) {
        int line = this.doc.lineOf(this.caret);
        int top = line * 12;
        if (!center || !(top < this.targetScrollY) && !(top + 12 > this.targetScrollY + this.h - 4.0)) {
            if (top < this.targetScrollY) {
                this.targetScrollY = top;
            } else if (top + 24 > this.targetScrollY + this.h - 4.0) {
                this.targetScrollY = top + 24 - this.h + 4;
            }
        } else {
            this.targetScrollY = top - this.h / 3.0;
        }

        this.targetScrollY = Math.max(0.0, Math.min((double)this.maxScrollY(), this.targetScrollY));
        int cx = this.font().width(this.doc.line(line).substring(0, this.doc.colOf(this.caret)));
        int area = this.textAreaW();
        if (cx - this.scrollX > area - 10) {
            this.scrollX = cx - area + 30;
        }

        if (cx - this.scrollX < 0.0) {
            this.scrollX = Math.max(0, cx - 30);
        }
    }

    @Override
    public int cursor(int mx, int my) {
        if (this.minimapShown() && mx >= this.x + this.w - 64) {
            return 221188;
        } else {
            for (Object[] hit : this.chipHits) {
                int hx = (Integer)hit[0];
                int hy = (Integer)hit[1];
                int hw = (Integer)hit[2];
                int hh = (Integer)hit[3];
                if (mx >= hx && mx < hx + hw && my >= hy && my < hy + hh) {
                    return 221188;
                }
            }

            return mx < this.textLeft() - 4 ? 221185 : 221186;
        }
    }

    @Override
    public void layout() {
        int bh = this.findBar.barHeight();
        this.findBar.bounds(this.x + this.w - 280 - 12 - (this.minimapShown() ? 64 : 0), this.y + 2, 280, bh);
        this.findBar.layout();
    }

    @Override
    public void render(Canvas c, int mx, int my, float dt) {
        if (Math.abs(this.targetScrollY - this.scrollY) > 0.5) {
            this.scrollY = this.scrollY + (this.targetScrollY - this.scrollY) * Math.min(1.0F, dt * 20.0F);
        } else {
            this.scrollY = this.targetScrollY;
        }

        super.render(c, mx, my, dt);
        UI ui = UI.get();
        if (this.popupOpen && ui != null) {
            if (!this.hasFocusWithin() && !this.isFocused()) {
                this.closePopup();
            } else {
                int line = this.doc.lineOf(this.caret);
                this.popup.anchor(this.colX(line, this.doc.colOf(this.caret)), this.lineY(line), 12);
            }
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        this.ensureAnalysis();
        this.chipHits.clear();
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        int gw = this.gutterW();
        c.fill(this.x, this.y, gw, this.h, -15461093);
        String text = this.doc.text();
        int[] colors = this.analysis != null ? this.analysis.colors() : new int[0];
        int first = Math.max(0, (int)(this.scrollY / 12.0));
        int last = Math.min(this.doc.lineCount() - 1, first + this.visibleLines() + 1);
        int selFrom = Math.min(this.caret, this.anchor);
        int selTo = Math.max(this.caret, this.anchor);
        int caretLine = this.doc.lineOf(this.caret);
        boolean focused = this.isFocused();
        this.updateOccurrences();
        int[] bracket = this.matchingBracket();
        List<LanguageSupport.Problem> problems = this.analysis != null ? this.analysis.problems() : List.of();
        c.pushClip(this.x, this.y, gw, this.h);

        for (int line = first; line <= last; line++) {
            int ly = this.lineY(line);
            String num = String.valueOf(line + 1);
            c.text(num, this.x + gw - 8 - c.width(num), ly + 2, line == caretLine ? -4933428 : -11775898);
            LanguageSupport.Severity worst = null;
            int ls = this.doc.lineStart(line);
            int le = this.doc.lineEnd(line);

            for (LanguageSupport.Problem p : problems) {
                if (p.start() <= le && Math.max(p.end(), p.start() + 1) > ls && (worst == null || p.severity().ordinal() < worst.ordinal())) {
                    worst = p.severity();
                }
            }

            if (worst != null) {
                int col = worst == LanguageSupport.Severity.ERROR ? -1023878 : (worst == LanguageSupport.Severity.WARNING ? -870325 : -10638858);
                c.round(this.x + 4, ly + 3, 5, 5, 2, col);
            }
        }

        c.popClip();
        c.fill(this.x + gw - 1, this.y, 1, this.h, -13749952);
        int areaRight = this.x + this.w - (this.minimapShown() ? 64 : 0);
        c.pushClip(this.x + gw, this.y, areaRight - this.x - gw, this.h);
        int left = this.textLeft() - (int)Math.round(this.scrollX);

        for (int line = first; line <= last; line++) {
            int ly = this.lineY(line);
            int ls = this.doc.lineStart(line);
            int le = this.doc.lineEnd(line);
            String l = this.doc.line(line);
            if (line == caretLine && selFrom == selTo) {
                c.fill(this.x + gw, ly, areaRight - this.x - gw, 12, -15000282);
            }

            int indent = 0;

            while (indent < l.length() && l.charAt(indent) == ' ') {
                indent++;
            }

            if (l.trim().isEmpty()) {
                indent = this.guessIndent(line);
            }

            for (int g = 4; g <= indent; g += 4) {
                int gx = left + this.font().width(" ".repeat(g - 4));
                c.fill(gx, ly, 1, 12, 419430399);
            }

            for (int[] m : this.matches) {
                this.paintRange(c, line, ls, le, m[0], m[1], m == this.currentMatchRange() ? 1728039787 : 872401771);
            }

            for (int[] o : this.occurrences) {
                this.paintRange(c, line, ls, le, o[0], o[1], 712888319);
            }

            if (selFrom != selTo && selFrom <= le && selTo >= ls) {
                int a = Math.max(selFrom, ls) - ls;
                int b = Math.min(selTo, le) - ls;
                int sx = this.colX(line, a);
                int ex = this.colX(line, b);
                if (selTo > le) {
                    ex += 4;
                }

                c.fill(sx, ly, Math.max(1, ex - sx), 12, focused ? 1430088664 : 859663320);
            }

            if (bracket != null) {
                for (int bo : bracket) {
                    if (bo >= ls && bo < le) {
                        int bx = this.colX(line, bo - ls);
                        int bw = this.font().width(String.valueOf(this.doc.charAt(bo)));
                        c.fill(bx, ly, bw + 1, 12, 1435098623);
                    }
                }
            }

            int runStart = 0;
            int xx = left;

            for (int i = 1; i <= l.length(); i++) {
                int ci = ls + i - 1;
                int prevColor = ci < colors.length ? colors[ci] : -2696470;
                int nextColor = i < l.length() && ls + i < colors.length ? colors[ls + i] : -2696470;
                if (i == l.length() || nextColor != prevColor) {
                    String run = l.substring(runStart, i);
                    if (xx < areaRight && xx + 2000 > this.x) {
                        c.text(run, xx, ly + 2, prevColor);
                    }

                    xx += this.font().width(run);
                    runStart = i;
                }
            }

            if (this.chipProvider != null) {
                int cx = xx + 10;

                for (CodeEditor.Chip ch : this.chips(text, line)) {
                    boolean sw = ch.swatch() != 0;
                    int cw = (ch.label().isEmpty() ? 0 : c.width(ch.label()) + 8) + (sw ? 12 : 0) + (ch.label().isEmpty() ? 2 : 0);
                    if (cx + cw > areaRight) {
                        break;
                    }

                    boolean hov = mx >= cx && mx < cx + cw && my >= ly + 1 && my < ly + 12 - 1;
                    c.round(cx, ly + 1, cw, 10, 3, hov ? 1716481023 : 858139450);
                    int tx = cx + 4;
                    if (sw) {
                        c.round(cx + 3, ly + 3, 8, 6, 2, -1);
                        c.round(cx + 4, ly + 4, 6, 4, 1, ch.swatch());
                        tx = cx + 14;
                    }

                    if (!ch.label().isEmpty()) {
                        c.text(ch.label(), tx, ly + 2, hov ? -1 : ch.color());
                    }

                    this.chipHits.add(new Object[]{cx, ly + 1, cw, 10, ch});
                    cx += cw + 4;
                }
            }

            for (LanguageSupport.Problem px : problems) {
                int ps = px.start();
                int pe = Math.max(px.end(), px.start() + 1);
                if (ps <= le && pe > ls) {
                    int a = Math.max(ps, ls) - ls;
                    int b = Math.min(pe, le) - ls;
                    int sx = this.colX(line, a);
                    int ex = b > a ? this.colX(line, b) : sx + 6;
                    int col = px.severity() == LanguageSupport.Severity.ERROR
                        ? -44176
                        : (px.severity() == LanguageSupport.Severity.WARNING ? -870325 : -10638858);
                    c.squiggle(sx, ly + 12 - 2, Math.max(4, ex - sx), col);
                }
            }
        }

        if (focused && !this.readOnly && (System.currentTimeMillis() - this.blinkStart) / 530L % 2L == 0L) {
            int cx = this.colX(caretLine, this.doc.colOf(this.caret));
            c.fill(cx, this.lineY(caretLine) + 1, 1, 11, -1);
        }

        if (this.snippetStops != null) {
            for (int ix = 0; ix < this.snippetStops.size(); ix++) {
                int[] s = this.snippetStops.get(ix);
                int line = this.doc.lineOf(s[0]);
                int sx = this.colX(line, s[0] - this.doc.lineStart(line));
                int ex = this.colX(line, s[1] - this.doc.lineStart(line));
                c.outline(sx - 1, this.lineY(line), Math.max(3, ex - sx + 2), 12, ix == this.snippetIndex ? -1433703169 : 1149988095);
            }
        }

        c.popClip();
        if (this.minimapShown()) {
            this.paintMinimap(c, colors, problems);
        }

        if (this.doc.length() == 0 && !this.readOnly) {
            c.text(this.emptyHint, this.textLeft(), this.y + 4 + 2, -9933435);
        }
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        if (this.signature != null && this.isFocused() && !this.signature.parts().isEmpty()) {
            int line = this.doc.lineOf(this.caret);
            int ly = this.lineY(line);
            StringBuilder sb = new StringBuilder();
            int activeStart = 0;
            int activeEnd = 0;

            for (int i = 0; i < this.signature.parts().size(); i++) {
                if (i > 0) {
                    sb.append(' ');
                }

                if (i == this.signature.active()) {
                    activeStart = c.width(sb.toString());
                }

                sb.append(this.signature.parts().get(i));
                if (i == this.signature.active()) {
                    activeEnd = c.width(sb.toString());
                }
            }

            String sig = sb.toString();
            String doc = this.signature.doc();
            int sw = Math.max(c.width(sig), doc != null ? Math.min(300, c.width(doc)) : 0) + 12;
            int docH = doc != null && !doc.isEmpty() ? c.wrappedHeight(doc, sw - 12, 1) + 3 : 0;
            int sh = 14 + docH;
            int sx = Math.max(this.x + this.gutterW() + 2, Math.min(this.colX(line, 0), this.x + this.w - sw - 4));
            int sy = ly - sh - 3;
            if (sy < this.y) {
                sy = ly + 12 + 3;
            }

            if (this.popupOpen) {
                sy = ly - sh - 3 < this.y ? sy : ly - sh - 3;
            }

            c.shadow(sx, sy, sw, sh, 3, 4);
            c.roundBorder(sx, sy, sw, sh, 3, -14210506, -12762793);
            c.text(sig, sx + 6, sy + 3, -6577995);
            if (activeEnd > activeStart) {
                String active = this.signature.parts().get(this.signature.active());
                c.text(active, sx + 6 + activeStart, sy + 3, -12988235);
                c.fill(sx + 6 + activeStart, sy + 12, activeEnd - activeStart, 1, -12988235);
            }

            if (docH > 0) {
                c.wrapped(doc, sx + 6, sy + 16, sw - 12, -9933435, 1);
            }
        }

        int total = this.doc.lineCount() * 12 + 36;
        if (total > this.h && !this.minimapShown()) {
            int barH = Math.max(16, this.h * this.h / total);
            int barY = this.y + (int)((this.h - barH) * (this.scrollY / Math.max(1, this.maxScrollY())));
            c.round(this.x + this.w - 5, barY, 4, barH, 2, 1157627903);
        }
    }

    private int[] currentMatchRange() {
        return this.currentMatch >= 0 && this.currentMatch < this.matches.size() ? this.matches.get(this.currentMatch) : null;
    }

    private void paintRange(Canvas c, int line, int ls, int le, int from, int to, int color) {
        if (to > ls && from <= le) {
            int a = Math.max(from, ls) - ls;
            int b = Math.min(to, le) - ls;
            int sx = this.colX(line, a);
            int ex = this.colX(line, b);
            c.fill(sx, this.lineY(line), Math.max(1, ex - sx), 12, color);
        }
    }

    private int guessIndent(int line) {
        for (int l = line - 1; l >= 0 && l >= line - 50; l--) {
            String s = this.doc.line(l);
            if (!s.trim().isEmpty()) {
                int i = 0;

                while (i < s.length() && s.charAt(i) == ' ') {
                    i++;
                }

                return s.trim().endsWith("{") ? i + 4 : i;
            }
        }

        return 0;
    }

    private void paintMinimap(Canvas c, int[] colors, List<LanguageSupport.Problem> problems) {
        int mx0 = this.x + this.w - 64;
        c.fill(mx0, this.y, 64, this.h, -15724266);
        c.fill(mx0, this.y, 1, this.h, -13749952);
        int lines = this.doc.lineCount();
        float lineScale = Math.min(2.0F, (float)(this.h - 4) / Math.max(1, lines));
        int px = mx0 + 4;

        for (int line = 0; line < lines; line++) {
            int ly = this.y + 2 + (int)(line * lineScale);
            String l = this.doc.line(line);
            int ls = this.doc.lineStart(line);
            int i = 0;

            while (i < l.length()) {
                if (l.charAt(i) == ' ') {
                    i++;
                } else {
                    int j = i;
                    int col = ls + i < colors.length ? colors[ls + i] : -2696470;

                    while (j < l.length() && l.charAt(j) != ' ' && (ls + j >= colors.length || colors[ls + j] == col)) {
                        j++;
                    }

                    int bx = px + i / 2;
                    int bw = Math.max(1, (j - i) / 2);
                    if (bx < this.x + this.w - 2) {
                        c.fill(bx, ly, Math.min(bw, this.x + this.w - 2 - bx), Math.max(1, (int)lineScale), Theme.alpha(col, 0.55F));
                    }

                    i = j;
                }
            }
        }

        for (LanguageSupport.Problem p : problems) {
            int line = this.doc.lineOf(Math.min(p.start(), this.doc.length()));
            int ly = this.y + 2 + (int)(line * lineScale);
            c.fill(this.x + this.w - 4, ly, 3, 2, p.severity() == LanguageSupport.Severity.ERROR ? -1023878 : -870325);
        }

        int vy = this.y + 2 + (int)(this.scrollY / 12.0 * lineScale);
        int vh = Math.max(4, (int)(this.visibleLines() * lineScale));
        c.fill(mx0 + 1, vy, 63, vh, this.draggingMinimap ? 587202559 : 352321535);
    }

    private void minimapScroll(double my) {
        int lines = this.doc.lineCount();
        float lineScale = Math.min(2.0F, (float)(this.h - 4) / Math.max(1, lines));
        double line = (my - this.y - 2.0) / lineScale - this.visibleLines() / 2.0;
        this.targetScrollY = this.scrollY = Math.max(0.0, Math.min((double)this.maxScrollY(), line * 12.0));
    }

    private void updateOccurrences() {
        if (this.occurrenceVersion != this.doc.version() || this.occurrenceCaret != this.caret) {
            this.occurrenceVersion = this.doc.version();
            this.occurrenceCaret = this.caret;
            this.occurrences = new ArrayList<>();
            if (this.caret == this.anchor) {
                int[] w = this.wordAt(this.caret);
                if (w != null && w[1] - w[0] >= 2) {
                    String word = this.doc.substring(w[0], w[1]);
                    String text = this.doc.text();
                    int idx = 0;

                    while ((idx = text.indexOf(word, idx)) >= 0) {
                        int end = idx + word.length();
                        boolean leftOk = idx == 0 || !isWordChar(text.charAt(idx - 1));
                        boolean rightOk = end >= text.length() || !isWordChar(text.charAt(end));
                        if (leftOk && rightOk) {
                            this.occurrences.add(new int[]{idx, end});
                        }

                        idx = end;
                        if (this.occurrences.size() > 500) {
                            break;
                        }
                    }

                    if (this.occurrences.size() < 2) {
                        this.occurrences.clear();
                    }
                }
            }
        }
    }

    private int[] matchingBracket() {
        String open = "({[";
        String close = ")}]";
        int[] cand = new int[]{this.caret - 1, this.caret};

        for (int pos : cand) {
            if (pos >= 0 && pos < this.doc.length()) {
                char ch = this.doc.charAt(pos);
                int oi = open.indexOf(ch);
                int ci = close.indexOf(ch);
                if (oi >= 0) {
                    int depth = 0;

                    for (int i = pos; i < this.doc.length() && i < pos + 20000; i++) {
                        char d = this.doc.charAt(i);
                        if (d == ch) {
                            depth++;
                        } else if (d == close.charAt(oi)) {
                            if (--depth == 0) {
                                return new int[]{pos, i};
                            }
                        }
                    }
                } else if (ci >= 0) {
                    int depth = 0;

                    for (int ix = pos; ix >= 0 && ix > pos - 20000; ix--) {
                        char d = this.doc.charAt(ix);
                        if (d == ch) {
                            depth++;
                        } else if (d == open.charAt(ci)) {
                            if (--depth == 0) {
                                return new int[]{ix, pos};
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private static boolean isWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private int[] wordAt(int offset) {
        int s = offset;
        int e = offset;

        while (s > 0 && isWordChar(this.doc.charAt(s - 1))) {
            s--;
        }

        while (e < this.doc.length() && isWordChar(this.doc.charAt(e))) {
            e++;
        }

        return s == e ? null : new int[]{s, e};
    }

    @Override
    public String tooltipText(int mx, int my) {
        if (mx >= this.textLeft() && (!this.minimapShown() || mx < this.x + this.w - 64)) {
            int line = (int)Math.floor((my - this.y - 4 + this.scrollY) / 12.0);
            if (line >= 0 && line < this.doc.lineCount()) {
                int off = this.offsetAt(mx, my);
                if (off >= this.doc.lineEnd(line) && mx > this.colX(line, this.doc.line(line).length()) + 4) {
                    return null;
                } else {
                    StringBuilder sb = new StringBuilder();

                    for (LanguageSupport.Problem p : this.problems()) {
                        int pe = Math.max(p.end(), p.start() + 1);
                        if (off >= p.start() && off <= pe) {
                            if (sb.length() > 0) {
                                sb.append('\n');
                            }

                            sb.append(p.severity() == LanguageSupport.Severity.ERROR ? "Ошибка: " : "Внимание: ").append(p.message());
                        }
                    }

                    String hover = this.lang.hover(this.doc.text(), off, this.analysis);
                    if (hover != null && !hover.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n\n");
                        }

                        sb.append(hover);
                    }

                    return sb.length() == 0 ? null : sb.toString();
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (this.minimapShown() && mx >= this.x + this.w - 64) {
            this.draggingMinimap = true;
            this.minimapScroll(my);
            return true;
        } else if (button != 1) {
            if (button != 0) {
                return false;
            } else {
                for (Object[] hit : this.chipHits) {
                    int hx = (Integer)hit[0];
                    int hy = (Integer)hit[1];
                    int hw = (Integer)hit[2];
                    int hh = (Integer)hit[3];
                    if (mx >= hx && mx < hx + hw && my >= hy && my < hy + hh) {
                        this.closePopup();
                        this.menuX = hx;
                        this.menuY = hy + hh + 2;
                        ((CodeEditor.Chip)hit[4]).action().accept(hx, hy + hh + 2);
                        return true;
                    }
                }

                this.closePopup();
                this.doc.breakUndoGroup();
                int off = this.offsetAt(mx, my);
                if (UI.ctrl(modsNow())) {
                    int def = this.lang.definition(this.doc.text(), off, this.analysis);
                    if (def >= 0) {
                        this.gotoOffset(def, 0);
                        return true;
                    }
                }

                int clicks = UI.get() != null ? UI.get().clickCount() : 1;
                if (clicks >= 3) {
                    int line = this.doc.lineOf(off);
                    this.anchor = this.doc.lineStart(line);
                    this.caret = line + 1 < this.doc.lineCount() ? this.doc.lineStart(line + 1) : this.doc.lineEnd(line);
                    this.dragMode = 3;
                } else if (clicks == 2) {
                    int[] w = this.wordAt(off);
                    if (w != null) {
                        this.anchor = w[0];
                        this.caret = w[1];
                    }

                    this.dragMode = 2;
                } else {
                    if (Screen.hasShiftDown()) {
                        this.caret = off;
                    } else {
                        this.caret = this.anchor = off;
                    }

                    this.dragMode = 1;
                }

                this.dragOriginStart = Math.min(this.caret, this.anchor);
                this.dragOriginEnd = Math.max(this.caret, this.anchor);
                this.draggingSelection = true;
                this.preferredX = -1;
                this.caretMoved();
                return true;
            }
        } else {
            int offx = this.offsetAt(mx, my);
            if (offx < Math.min(this.caret, this.anchor) || offx > Math.max(this.caret, this.anchor)) {
                this.caret = this.anchor = offx;
            }

            this.openContextMenu((int)mx, (int)my);
            return true;
        }
    }

    private static int modsNow() {
        long win = Minecraft.getInstance().getWindow().getWindow();
        int mods = 0;
        if (GLFW.glfwGetKey(win, 341) == 1 || GLFW.glfwGetKey(win, 345) == 1) {
            mods |= 2;
        }

        return mods;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (this.draggingMinimap) {
            this.minimapScroll(my);
            return true;
        } else if (!this.draggingSelection) {
            return false;
        } else {
            int off = this.offsetAt(mx, my);
            if (this.dragMode == 1) {
                this.caret = off;
            } else if (this.dragMode == 2) {
                int[] w = this.wordAt(off);
                int ws = w != null ? w[0] : off;
                int we = w != null ? w[1] : off;
                if (ws < this.dragOriginStart) {
                    this.anchor = this.dragOriginEnd;
                    this.caret = ws;
                } else {
                    this.anchor = this.dragOriginStart;
                    this.caret = Math.max(we, this.dragOriginEnd);
                }
            } else {
                int line = this.doc.lineOf(off);
                int ls = this.doc.lineStart(line);
                int le = line + 1 < this.doc.lineCount() ? this.doc.lineStart(line + 1) : this.doc.lineEnd(line);
                if (ls < this.dragOriginStart) {
                    this.anchor = this.dragOriginEnd;
                    this.caret = ls;
                } else {
                    this.anchor = this.dragOriginStart;
                    this.caret = le;
                }
            }

            if (my < this.y + 6) {
                this.targetScrollY = this.scrollY = Math.max(0.0, this.scrollY - 12.0);
            }

            if (my > this.y + this.h - 6) {
                this.targetScrollY = this.scrollY = Math.min((double)this.maxScrollY(), this.scrollY + 12.0);
            }

            this.caretMoved();
            return true;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        this.draggingSelection = false;
        this.draggingMinimap = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (Screen.hasShiftDown()) {
            this.scrollX = Math.max(0.0, this.scrollX - delta * 40.0);
        } else {
            this.targetScrollY = Math.max(0.0, Math.min((double)this.maxScrollY(), this.targetScrollY - delta * 12.0 * 3.0));
        }

        return true;
    }

    private List<CodeEditor.Chip> chips(String text, int line) {
        int key = text.hashCode() * 31 + text.length();
        if (key != this.chipCacheFor) {
            this.chipCache.clear();
            this.chipCacheFor = key;
        }

        return this.chipCache.computeIfAbsent(line, l -> {
            try {
                List<CodeEditor.Chip> r = this.chipProvider.apply(text, l);
                return r != null ? r : List.of();
            } catch (RuntimeException var4) {
                return List.of();
            }
        });
    }

    public void replaceText(int from, int to, String s) {
        if (!this.readOnly) {
            this.anchor = this.clamp(from);
            this.caret = this.clamp(to);
            this.replaceSelection(s, 0);
        }
    }

    private void openContextMenu(int mx, int my) {
        this.menuX = mx;
        this.menuY = my;
        PopupMenu pm = new PopupMenu();
        pm.item("Вырезать", Icon.EDIT, "Ctrl+X", this::cut).enabled(() -> !this.readOnly);
        pm.item("Копировать", Icon.COPY, "Ctrl+C", this::copy);
        pm.item("Вставить", Icon.FILE, "Ctrl+V", this::paste).enabled(() -> !this.readOnly);
        pm.separator();
        pm.item("Перейти к объявлению", Icon.ARROW_RIGHT, "F12", () -> {
            int def = this.lang.definition(this.doc.text(), this.caret, this.analysis);
            if (def >= 0) {
                this.gotoOffset(def, 0);
            }
        });
        pm.item("Найти", Icon.SEARCH, "Ctrl+F", () -> this.openFind(false));
        pm.item("Заменить", Icon.SEARCH, "Ctrl+H", () -> this.openFind(true)).enabled(() -> !this.readOnly);
        pm.separator();
        pm.item("Закомментировать", Icon.CODE, "Ctrl+/", this::toggleComment).enabled(() -> !this.readOnly);
        pm.item("Форматировать", Icon.LIST, "Ctrl+Shift+F", this::format).enabled(() -> !this.readOnly);
        if (this.positionProvider != null) {
            pm.item("Вставить мою позицию", Icon.PIN, "Alt+P", this::insertPosition).enabled(() -> !this.readOnly);
        }

        if (this.contextExtras != null) {
            this.contextExtras.accept(pm);
        }

        pm.show(mx, my);
    }

    private void replaceRange(int from, int to, String s, int newCaret, int kind) {
        int cb = this.caret;
        int ab = this.anchor;
        this.doc.replace(from, to, s, cb, ab, newCaret, newCaret, kind);
        this.caret = this.anchor = this.clamp(newCaret);
        this.adjustSnippet(from, to - from, TextDocument.normalize(s).length());
        this.changed();
        this.revealCaret(false);
    }

    private void replaceSelection(String s, int kind) {
        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        String norm = TextDocument.normalize(s);
        this.replaceRange(from, to, norm, from + norm.length(), kind);
    }

    public void copy() {
        if (this.caret != this.anchor) {
            UI.setClipboard(this.selectedText());
        } else {
            int line = this.doc.lineOf(this.caret);
            UI.setClipboard(this.doc.line(line) + "\n");
        }
    }

    public void cut() {
        if (!this.readOnly) {
            this.copy();
            if (this.caret != this.anchor) {
                this.replaceSelection("", 0);
            } else {
                int line = this.doc.lineOf(this.caret);
                int ls = this.doc.lineStart(line);
                int le = line + 1 < this.doc.lineCount() ? this.doc.lineStart(line + 1) : this.doc.lineEnd(line);
                this.replaceRange(ls, le, "", ls, 0);
            }
        }
    }

    public void paste() {
        if (!this.readOnly) {
            String clip = UI.clipboard();
            if (clip != null && !clip.isEmpty()) {
                this.doc.breakUndoGroup();
                this.replaceSelection(clip, 0);
                this.doc.breakUndoGroup();
            }
        }
    }

    public void selectAll() {
        this.anchor = 0;
        this.caret = this.doc.length();
        this.caretMoved();
    }

    private String indentOf(int line) {
        String l = this.doc.line(line);
        int i = 0;

        while (i < l.length() && l.charAt(i) == ' ') {
            i++;
        }

        return l.substring(0, i);
    }

    private void newline() {
        int line = this.doc.lineOf(this.caret);
        String indent = this.indentOf(line);
        int from = Math.min(this.caret, this.anchor);
        char before = from > 0 ? this.doc.charAt(from - 1) : 0;
        char after = Math.max(this.caret, this.anchor) < this.doc.length() ? this.doc.charAt(Math.max(this.caret, this.anchor)) : 0;
        if (this.doc.colOf(from) < indent.length()) {
            indent = indent.substring(0, this.doc.colOf(from));
        }

        if (before == '{' && after == '}') {
            String ins = "\n" + indent + "    \n" + indent;
            this.replaceSelection(ins, 0);
            this.caret = this.anchor = from + 1 + indent.length() + 4;
            this.caretMoved();
        } else if (before == '{') {
            this.replaceSelection("\n" + indent + "    ", 0);
        } else {
            this.replaceSelection("\n" + indent, 0);
        }

        this.doc.breakUndoGroup();
    }

    private void typeChar(char ch) {
        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        char next = to < this.doc.length() ? this.doc.charAt(to) : 0;
        char prev = from > 0 ? this.doc.charAt(from - 1) : 0;
        boolean inString = this.inStringAt(from);
        if (from != to || ")]}\"".indexOf(ch) < 0 || next != ch || ch == '"' && !inString) {
            int oi = "([{\"".indexOf(ch);
            if (oi >= 0 && (ch != '"' || !inString && !isWordChar(prev))) {
                char closer = ")]}\"".charAt(oi);
                if (from != to) {
                    String sel = this.doc.substring(from, to);
                    this.replaceRange(from, to, ch + sel + closer, from + 1 + sel.length() + 1, 0);
                    this.anchor = from + 1;
                    this.caret = from + 1 + sel.length();
                    this.caretMoved();
                    return;
                }

                boolean nextFree = next == 0 || Character.isWhitespace(next) || ")]}\"".indexOf(next) >= 0;
                if (nextFree && (ch != '"' || !inString)) {
                    this.replaceRange(from, to, "" + ch + closer, from + 1, 1);
                    return;
                }
            }

            if (ch == '}' && from == to) {
                int line = this.doc.lineOf(from);
                String before = this.doc.substring(this.doc.lineStart(line), from);
                if (before.trim().isEmpty() && before.length() >= 4) {
                    int ls = this.doc.lineStart(line);
                    this.replaceRange(ls, from, before.substring(4) + "}", ls + before.length() - 4 + 1, 1);
                    return;
                }
            }

            this.replaceSelection(String.valueOf(ch), 1);
        } else {
            this.caret = this.anchor = from + 1;
            this.caretMoved();
        }
    }

    private boolean inStringAt(int offset) {
        int line = this.doc.lineOf(offset);
        String l = this.doc.substring(this.doc.lineStart(line), offset);
        boolean in = false;

        for (int i = 0; i < l.length(); i++) {
            char ch = l.charAt(i);
            if (ch == '\\' && in) {
                i++;
            } else {
                if (ch == '"') {
                    in = !in;
                }

                if (!in && ch == '/' && i + 1 < l.length() && l.charAt(i + 1) == '/') {
                    return false;
                }
            }
        }

        return in;
    }

    private boolean inCommentAt(int offset) {
        int line = this.doc.lineOf(offset);
        String l = this.doc.substring(this.doc.lineStart(line), offset);
        boolean in = false;

        for (int i = 0; i < l.length(); i++) {
            char ch = l.charAt(i);
            if (ch == '"') {
                in = !in;
            }

            if (!in && (ch == '/' && i + 1 < l.length() && l.charAt(i + 1) == '/' || ch == '#')) {
                return true;
            }
        }

        return false;
    }

    private void backspace(boolean word) {
        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        if (from != to) {
            this.replaceRange(from, to, "", from, 2);
        } else if (from != 0) {
            if (!word) {
                char prev = this.doc.charAt(from - 1);
                char next = from < this.doc.length() ? this.doc.charAt(from) : 0;
                int oi = "([{\"".indexOf(prev);
                if (oi >= 0 && next == ")]}\"".charAt(oi)) {
                    this.replaceRange(from - 1, from + 1, "", from - 1, 2);
                } else {
                    int line = this.doc.lineOf(from);
                    int ls = this.doc.lineStart(line);
                    String before = this.doc.substring(ls, from);
                    if (!before.isEmpty() && before.trim().isEmpty()) {
                        int remove = before.length() % 4 == 0 ? 4 : before.length() % 4;
                        this.replaceRange(from - remove, from, "", from - remove, 2);
                    } else {
                        this.replaceRange(from - 1, from, "", from - 1, 2);
                    }
                }
            } else {
                int s = from;

                while (s > 0 && Character.isWhitespace(this.doc.charAt(s - 1)) && this.doc.charAt(s - 1) != '\n') {
                    s--;
                }

                if (s == from) {
                    if (isWordChar(this.doc.charAt(s - 1))) {
                        while (s > 0 && isWordChar(this.doc.charAt(s - 1))) {
                            s--;
                        }
                    } else {
                        s--;
                    }
                }

                this.replaceRange(s, from, "", s, 2);
            }
        }
    }

    private void deleteForward(boolean word) {
        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        if (from != to) {
            this.replaceRange(from, to, "", from, 2);
        } else if (from < this.doc.length()) {
            int e = from + 1;
            if (word) {
                e = from;
                if (isWordChar(this.doc.charAt(from))) {
                    while (e < this.doc.length() && isWordChar(this.doc.charAt(e))) {
                        e++;
                    }
                } else {
                    e = from + 1;
                }
            }

            this.replaceRange(from, e, "", from, 2);
        }
    }

    private void indentLines(boolean outdent) {
        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        int l1 = this.doc.lineOf(from);
        int l2 = this.doc.lineOf(to);
        if (to > from && this.doc.colOf(to) == 0 && l2 > l1) {
            l2--;
        }

        if (!outdent && from == to) {
            int col = this.doc.colOf(this.caret);
            int spaces = 4 - col % 4;
            this.replaceSelection(" ".repeat(spaces), 1);
        } else {
            StringBuilder sb = new StringBuilder();
            int start = this.doc.lineStart(l1);
            int end = this.doc.lineEnd(l2);
            int firstDelta = 0;
            int totalDelta = 0;

            for (int l = l1; l <= l2; l++) {
                String line = this.doc.line(l);
                String nl;
                if (!outdent) {
                    nl = line.trim().isEmpty() ? line : "    " + line;
                    int add = nl.length() - line.length();
                    if (l == l1) {
                        firstDelta = add;
                    }

                    totalDelta += add;
                } else {
                    int rm = 0;

                    while (rm < 4 && rm < line.length() && line.charAt(rm) == ' ') {
                        rm++;
                    }

                    nl = line.substring(rm);
                    if (l == l1) {
                        firstDelta = -rm;
                    }

                    totalDelta -= rm;
                }

                sb.append(nl);
                if (l < l2) {
                    sb.append('\n');
                }
            }

            int newAnchor = Math.max(start, from + firstDelta);
            int newCaret = Math.max(start, to + totalDelta);
            int cb = this.caret;
            int ab = this.anchor;
            this.doc.replace(start, end, sb.toString(), cb, ab, newCaret, newAnchor, 0);
            this.anchor = this.clamp(newAnchor);
            this.caret = this.clamp(newCaret);
            this.changed();
        }
    }

    public void toggleComment() {
        if (!this.readOnly) {
            String marker = this.lang.lineComment();
            int from = Math.min(this.caret, this.anchor);
            int to = Math.max(this.caret, this.anchor);
            int l1 = this.doc.lineOf(from);
            int l2 = this.doc.lineOf(to);
            if (to > from && this.doc.colOf(to) == 0 && l2 > l1) {
                l2--;
            }

            boolean allCommented = true;
            int minIndent = Integer.MAX_VALUE;

            for (int l = l1; l <= l2; l++) {
                String s = this.doc.line(l);
                if (!s.trim().isEmpty()) {
                    if (!s.trim().startsWith(marker)) {
                        allCommented = false;
                    }

                    minIndent = Math.min(minIndent, this.indentOf(l).length());
                }
            }

            if (minIndent == Integer.MAX_VALUE) {
                minIndent = 0;
            }

            StringBuilder sb = new StringBuilder();

            for (int lx = l1; lx <= l2; lx++) {
                String s = this.doc.line(lx);
                if (s.trim().isEmpty()) {
                    sb.append(s);
                } else if (allCommented) {
                    int i = s.indexOf(marker);
                    String rest = s.substring(i + marker.length());
                    if (rest.startsWith(" ")) {
                        rest = rest.substring(1);
                    }

                    sb.append(s, 0, i).append(rest);
                } else {
                    sb.append(s, 0, minIndent).append(marker).append(' ').append(s.substring(minIndent));
                }

                if (lx < l2) {
                    sb.append('\n');
                }
            }

            int start = this.doc.lineStart(l1);
            int end = this.doc.lineEnd(l2);
            int cb = this.caret;
            int ab = this.anchor;
            String repl = sb.toString();
            int delta = repl.length() - (end - start);
            this.doc.replace(start, end, repl, cb, ab, cb, ab, 0);
            if (from == to) {
                this.caret = this.anchor = this.clamp(this.caret + (this.doc.lineOf(Math.max(0, this.caret)) == l1 ? delta : 0));
            } else {
                this.anchor = this.clamp(start);
                this.caret = this.clamp(end + delta);
            }

            this.changed();
        }
    }

    private void duplicateLine() {
        int line = this.doc.lineOf(this.caret);
        int col = this.doc.colOf(this.caret);
        String l = this.doc.line(line);
        int le = this.doc.lineEnd(line);
        this.replaceRange(le, le, "\n" + l, le + 1 + col, 0);
    }

    private void deleteLine() {
        int line = this.doc.lineOf(this.caret);
        int ls = this.doc.lineStart(line);
        int le = line + 1 < this.doc.lineCount() ? this.doc.lineStart(line + 1) : this.doc.lineEnd(line);
        if (line + 1 >= this.doc.lineCount() && ls > 0) {
            ls--;
        }

        this.replaceRange(ls, le, "", Math.min(ls, this.doc.length()), 0);
    }

    private void moveLines(int dir) {
        int from = Math.min(this.caret, this.anchor);
        int to = Math.max(this.caret, this.anchor);
        int l1 = this.doc.lineOf(from);
        int l2 = this.doc.lineOf(to);
        if (dir >= 0 || l1 != 0) {
            if (dir <= 0 || l2 < this.doc.lineCount() - 1) {
                int start = this.doc.lineStart(dir < 0 ? l1 - 1 : l1);
                int end = this.doc.lineEnd(dir < 0 ? l2 : l2 + 1);
                String block = this.doc.substring(this.doc.lineStart(l1), this.doc.lineEnd(l2));
                String other = dir < 0 ? this.doc.line(l1 - 1) : this.doc.line(l2 + 1);
                String repl = dir < 0 ? block + "\n" + other : other + "\n" + block;
                int shift = dir < 0 ? -(other.length() + 1) : other.length() + 1;
                int cb = this.caret;
                int ab = this.anchor;
                this.doc.replace(start, end, repl, cb, ab, cb + shift, ab + shift, 0);
                this.caret = this.clamp(cb + shift);
                this.anchor = this.clamp(ab + shift);
                this.changed();
                this.revealCaret(false);
            }
        }
    }

    private void insertPosition() {
        if (this.positionProvider != null && !this.readOnly) {
            String pos = this.positionProvider.get();
            if (pos != null) {
                this.replaceSelection(pos, 0);
            }
        }
    }

    private void moveCaret(int newPos, boolean select) {
        this.caret = this.clamp(newPos);
        if (!select) {
            this.anchor = this.caret;
        }

        this.revealCaret(false);
        this.caretMoved();
    }

    private void verticalMove(int lines, boolean select) {
        int line = this.doc.lineOf(this.caret);
        if (this.preferredX < 0) {
            this.preferredX = this.font().width(this.doc.line(line).substring(0, this.doc.colOf(this.caret)));
        }

        int target = Math.max(0, Math.min(this.doc.lineCount() - 1, line + lines));
        if (target == line) {
            this.moveCaret(lines < 0 ? 0 : this.doc.length(), select);
        } else {
            String l = this.doc.line(target);
            int acc = 0;
            int col = l.length();

            for (int i = 0; i < l.length(); i++) {
                int cw = this.font().width(String.valueOf(l.charAt(i)));
                if (this.preferredX < acc + cw / 2.0) {
                    col = i;
                    break;
                }

                acc += cw;
            }

            int keep = this.preferredX;
            this.moveCaret(this.doc.lineStart(target) + col, select);
            this.preferredX = keep;
        }
    }

    private int wordLeft(int i) {
        while (i > 0 && Character.isWhitespace(this.doc.charAt(i - 1))) {
            i--;
        }

        if (i > 0 && isWordChar(this.doc.charAt(i - 1))) {
            while (i > 0 && isWordChar(this.doc.charAt(i - 1))) {
                i--;
            }
        } else if (i > 0) {
            i--;
        }

        return i;
    }

    private int wordRight(int i) {
        int n = this.doc.length();

        while (i < n && Character.isWhitespace(this.doc.charAt(i))) {
            i++;
        }

        if (i < n && isWordChar(this.doc.charAt(i))) {
            while (i < n && isWordChar(this.doc.charAt(i))) {
                i++;
            }
        } else if (i < n) {
            i++;
        }

        return i;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        boolean ctrl = UI.ctrl(mods);
        boolean shift = UI.shift(mods);
        boolean alt = UI.alt(mods);
        if (this.popupOpen) {
            switch (key) {
                case 256:
                    this.closePopup();
                    return true;
                case 257:
                case 258:
                case 335:
                    LanguageSupport.Completion sel = this.popup.selected();
                    if (sel != null) {
                        this.acceptCompletion(sel);
                        return true;
                    }

                    this.closePopup();
                    break;
                case 264:
                    this.popup.move(1);
                    return true;
                case 265:
                    this.popup.move(-1);
                    return true;
                case 266:
                    this.popup.move(-8);
                    return true;
                case 267:
                    this.popup.move(8);
                    return true;
            }
        }

        if (this.snippetStops != null) {
            if (key == 258) {
                this.nextSnippetStop(shift ? -1 : 1);
                return true;
            }

            if (key == 256) {
                this.snippetStops = null;
                return true;
            }
        }

        if (ctrl) {
            switch (key) {
                case 32:
                    if (!this.readOnly) {
                        this.requestCompletion(true);
                    }
                    break;
                case 47:
                case 331:
                    this.toggleComment();
                    break;
                case 65:
                    this.selectAll();
                    break;
                case 67:
                    this.copy();
                    break;
                case 68:
                    if (!this.readOnly) {
                        this.duplicateLine();
                    }
                    break;
                case 70:
                    if (shift) {
                        this.format();
                    } else {
                        this.openFind(false);
                    }
                    break;
                case 71:
                    this.promptGotoLine();
                    break;
                case 72:
                    if (!this.readOnly) {
                        this.openFind(true);
                    }
                    break;
                case 75:
                    if (!shift || this.readOnly) {
                        return false;
                    }

                    this.deleteLine();
                    break;
                case 83:
                    if (this.onSave == null) {
                        return false;
                    }

                    this.onSave.run();
                    break;
                case 86:
                    this.paste();
                    break;
                case 88:
                    this.cut();
                    break;
                case 89:
                    if (!this.readOnly) {
                        this.redo();
                    }
                    break;
                case 90:
                    if (this.readOnly) {
                        return true;
                    }

                    if (shift) {
                        this.redo();
                    } else {
                        this.undo();
                    }
                    break;
                case 257:
                case 335:
                    if (this.readOnly) {
                        return true;
                    }

                    int line = this.doc.lineOf(this.caret);
                    this.caret = this.anchor = this.doc.lineEnd(line);
                    this.newline();
                    break;
                case 259:
                    if (!this.readOnly) {
                        this.backspace(true);
                    }
                    break;
                case 261:
                    if (!this.readOnly) {
                        this.deleteForward(true);
                    }
                    break;
                case 262:
                    this.moveCaret(this.wordRight(this.caret), shift);
                    break;
                case 263:
                    this.moveCaret(this.wordLeft(this.caret), shift);
                    break;
                case 264:
                    this.targetScrollY = Math.min((double)this.maxScrollY(), this.targetScrollY + 12.0);
                    break;
                case 265:
                    this.targetScrollY = Math.max(0.0, this.targetScrollY - 12.0);
                    break;
                case 268:
                    this.moveCaret(0, shift);
                    break;
                case 269:
                    this.moveCaret(this.doc.length(), shift);
                    break;
                default:
                    return false;
            }

            return true;
        } else {
            if (alt) {
                switch (key) {
                    case 80:
                        this.insertPosition();
                        return true;
                    case 264:
                        if (!this.readOnly) {
                            this.moveLines(1);
                        }

                        return true;
                    case 265:
                        if (!this.readOnly) {
                            this.moveLines(-1);
                        }

                        return true;
                }
            }

            switch (key) {
                case 256:
                    if (this.findBar.visible) {
                        this.findBar.hide();
                        return true;
                    }

                    if (this.caret != this.anchor) {
                        this.anchor = this.caret;
                        return true;
                    }

                    if (this.signature != null) {
                        this.signature = null;
                        return true;
                    }

                    return false;
                case 257:
                case 335:
                    if (!this.readOnly) {
                        this.newline();
                    }
                    break;
                case 258:
                    if (!this.readOnly) {
                        this.indentLines(shift);
                    }
                    break;
                case 259:
                    if (this.readOnly) {
                        return true;
                    }

                    this.backspace(false);
                    if (this.popupOpen) {
                        if (this.caret < this.popupStart()) {
                            this.closePopup();
                        } else {
                            this.requestCompletion(this.popupExplicit);
                        }
                    }
                    break;
                case 261:
                    if (!this.readOnly) {
                        this.deleteForward(false);
                    }
                    break;
                case 262:
                    int to = Math.max(this.caret, this.anchor);
                    this.moveCaret(!shift && this.caret != this.anchor ? to : this.caret + 1, shift);
                    this.preferredX = -1;
                    break;
                case 263:
                    int from = Math.min(this.caret, this.anchor);
                    this.moveCaret(!shift && this.caret != this.anchor ? from : this.caret - 1, shift);
                    this.preferredX = -1;
                    break;
                case 264:
                    this.verticalMove(1, shift);
                    break;
                case 265:
                    this.verticalMove(-1, shift);
                    break;
                case 266:
                    this.verticalMove(-this.visibleLines(), shift);
                    break;
                case 267:
                    this.verticalMove(this.visibleLines(), shift);
                    break;
                case 268:
                    int line = this.doc.lineOf(this.caret);
                    int ls = this.doc.lineStart(line);
                    int first = ls + this.indentOf(line).length();
                    this.moveCaret(this.caret == first ? ls : first, shift);
                    this.preferredX = -1;
                    break;
                case 269:
                    this.moveCaret(this.doc.lineEnd(this.doc.lineOf(this.caret)), shift);
                    this.preferredX = -1;
                    break;
                case 292:
                    this.findNext(!shift);
                    break;
                case 301:
                    int def = this.lang.definition(this.doc.text(), this.caret, this.analysis);
                    if (def >= 0) {
                        this.gotoOffset(def, 0);
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (this.readOnly || ch < ' ' || ch == 127) {
            return false;
        } else if (UI.ctrl(mods) && !UI.alt(mods)) {
            return false;
        } else {
            this.typeChar(ch);
            if (this.inCommentAt(this.caret)) {
                this.closePopup();
                return true;
            } else {
                boolean inStr = this.inStringAt(this.caret);
                if (this.popupOpen) {
                    if (isWordChar(ch)) {
                        this.requestCompletion(this.popupExplicit);
                    } else if (ch != ' ' && ch != '(' && ch != ',') {
                        this.closePopup();
                    } else {
                        this.requestCompletion(false);
                    }
                } else if (inStr || !this.lang.isCompletionTrigger(ch) && ch != ' ') {
                    if (inStr && ch == ':') {
                        this.requestCompletion(false);
                    }
                } else {
                    this.requestCompletion(false);
                }

                this.caretMoved();
                return true;
            }
        }
    }

    private int popupStart() {
        return this.popupStartOffset;
    }

    public void requestCompletion(boolean explicit) {
        this.ensureAnalysis();
        List<LanguageSupport.Completion> items = this.lang.complete(this.doc.text(), this.caret, this.analysis, explicit);
        UI ui = UI.get();
        if (!items.isEmpty() && ui != null) {
            if (!explicit && items.size() == 1) {
                LanguageSupport.Completion only = items.get(0);
                String typed = this.doc.substring(only.replaceStart(), this.caret);
                if (typed.equals(only.label()) || typed.equals(only.insert())) {
                    this.closePopup();
                    return;
                }
            }

            this.popup.setItems(items);
            this.popupStartOffset = items.get(0).replaceStart();
            this.popupExplicit = explicit;
            int line = this.doc.lineOf(this.caret);
            this.popup.anchor(this.colX(line, this.doc.colOf(this.caret)), this.lineY(line), 12);
            if (!this.popupOpen) {
                ui.openOverlay(this.popup, false, true, () -> this.popupOpen = false);
                this.popupOpen = true;
            }
        } else {
            this.closePopup();
        }
    }

    public void closePopup() {
        if (this.popupOpen) {
            this.popupOpen = false;
            UI ui = UI.get();
            if (ui != null) {
                ui.closeOverlay(this.popup);
            }
        }
    }

    private void acceptCompletion(LanguageSupport.Completion item) {
        this.closePopup();
        int start = Math.max(0, Math.min(item.replaceStart(), this.caret));
        int end = this.caret;

        while (end < this.doc.length() && isWordChar(this.doc.charAt(end))) {
            end++;
        }

        this.doc.breakUndoGroup();
        this.insertSnippet(start, end, item.insert());
        this.doc.breakUndoGroup();
        this.requestFocus();
        if (item.insert().endsWith(" ") && this.snippetStops == null) {
            this.requestCompletion(false);
        }
    }

    private void insertSnippet(int from, int to, String snippet) {
        StringBuilder out = new StringBuilder();
        List<int[]> stops = new ArrayList<>();
        Matcher m = PLACEHOLDER.matcher(snippet);

        int last;
        for (last = 0; m.find(); last = m.end()) {
            out.append(snippet, last, m.start());
            int idx = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(3));
            String ph = m.group(2) != null ? m.group(2) : "";
            int s = out.length();
            out.append(ph);
            stops.add(new int[]{idx == 0 ? Integer.MAX_VALUE : idx, s, out.length()});
        }

        out.append(snippet.substring(last));
        String indent = this.indentOf(this.doc.lineOf(from));
        String text = out.toString().replace("\n", "\n" + indent);
        if (!indent.isEmpty()) {
            for (int[] s : stops) {
                int added = countNewlines(out.substring(0, s[1])) * indent.length();
                int addedEnd = countNewlines(out.substring(0, s[2])) * indent.length();
                s[1] += added;
                s[2] += addedEnd;
            }
        }

        stops.sort((a, b) -> Integer.compare(a[0], b[0]));
        int cb = this.caret;
        int ab = this.anchor;
        this.doc.replace(from, to, text, cb, ab, from + text.length(), from + text.length(), 0);
        if (stops.isEmpty()) {
            this.caret = this.anchor = from + text.length();
            this.snippetStops = null;
        } else {
            this.snippetStops = new ArrayList<>();

            for (int[] s : stops) {
                this.snippetStops.add(new int[]{from + s[1], from + s[2]});
            }

            this.snippetIndex = 0;
            int[] first = this.snippetStops.get(0);
            this.anchor = first[0];
            this.caret = first[1];
            if (this.snippetStops.size() == 1 && first[0] == first[1]) {
                this.snippetStops = null;
            }
        }

        this.reanalyze();
        if (this.findBar.visible) {
            this.updateMatches(false);
        }

        if (this.onChange != null) {
            this.onChange.accept(this);
        }

        this.revealCaret(false);
        this.caretMoved();
    }

    private static int countNewlines(String s) {
        int n = 0;

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                n++;
            }
        }

        return n;
    }

    private void nextSnippetStop(int dir) {
        if (this.snippetStops != null) {
            this.snippetIndex += dir;
            if (this.snippetIndex < 0) {
                this.snippetIndex = 0;
            }

            if (this.snippetIndex >= this.snippetStops.size()) {
                int[] last = this.snippetStops.get(this.snippetStops.size() - 1);
                this.caret = this.anchor = last[1];
                this.snippetStops = null;
                this.caretMoved();
            } else {
                int[] s = this.snippetStops.get(this.snippetIndex);
                this.anchor = s[0];
                this.caret = s[1];
                this.caretMoved();
                if (s[0] == s[1]) {
                    this.requestCompletion(false);
                }
            }
        }
    }

    private boolean insideSnippet() {
        if (this.snippetStops == null) {
            return false;
        } else {
            int min = Integer.MAX_VALUE;
            int max = -1;

            for (int[] s : this.snippetStops) {
                min = Math.min(min, s[0]);
                max = Math.max(max, s[1]);
            }

            return this.caret >= min
                && this.caret <= max
                && this.doc.lineOf(this.caret) >= this.doc.lineOf(min)
                && this.doc.lineOf(this.caret) <= this.doc.lineOf(max);
        }
    }

    private void adjustSnippet(int offset, int removed, int inserted) {
        if (this.snippetStops != null) {
            int delta = inserted - removed;

            for (int i = 0; i < this.snippetStops.size(); i++) {
                int[] s = this.snippetStops.get(i);
                if (offset >= s[0] && offset + removed <= s[1]) {
                    s[1] += delta;
                } else if (offset < s[0]) {
                    s[0] += delta;
                    s[1] += delta;
                }
            }
        }
    }

    private void updateSnippetAfterEdit() {
        if (this.snippetStops != null && !this.insideSnippet()) {
            this.snippetStops = null;
        }
    }

    void updateMatches(boolean jump) {
        this.matches.clear();
        this.currentMatch = -1;
        String q = this.findBar.find.text();
        if (!q.isEmpty()) {
            String text = this.doc.text();
            String hay = this.findBar.matchCase ? text : text.toLowerCase();
            String needle = this.findBar.matchCase ? q : q.toLowerCase();
            int idx = 0;

            while ((idx = hay.indexOf(needle, idx)) >= 0) {
                int end = idx + needle.length();
                boolean ok = !this.findBar.wholeWord
                    || (idx == 0 || !isWordChar(text.charAt(idx - 1))) && (end >= text.length() || !isWordChar(text.charAt(end)));
                if (ok) {
                    this.matches.add(new int[]{idx, end});
                }

                idx = Math.max(end, idx + 1);
                if (this.matches.size() > 5000) {
                    break;
                }
            }

            if (!this.matches.isEmpty()) {
                this.currentMatch = 0;

                for (int i = 0; i < this.matches.size(); i++) {
                    if (this.matches.get(i)[0] >= Math.min(this.caret, this.anchor)) {
                        this.currentMatch = i;
                        break;
                    }
                }

                if (jump) {
                    this.selectMatch();
                }
            }
        }
    }

    void clearMatches() {
        this.matches.clear();
        this.currentMatch = -1;
    }

    String matchSummary() {
        if (this.findBar.find.text().isEmpty()) {
            return "";
        } else {
            return this.matches.isEmpty() ? "нет" : this.currentMatch + 1 + "/" + this.matches.size();
        }
    }

    private void selectMatch() {
        int[] m = this.currentMatchRange();
        if (m != null) {
            this.anchor = m[0];
            this.caret = m[1];
            this.revealCaret(true);
        }
    }

    public void findNext(boolean forward) {
        if (this.matches.isEmpty()) {
            this.updateMatches(false);
            if (this.matches.isEmpty()) {
                return;
            }
        }

        this.currentMatch = Math.floorMod(this.currentMatch + (forward ? 1 : -1), this.matches.size());
        this.selectMatch();
    }

    void replaceCurrent() {
        if (!this.readOnly) {
            int[] m = this.currentMatchRange();
            if (m != null) {
                String repl = this.findBar.replace.text();
                this.replaceRange(m[0], m[1], repl, m[0] + repl.length(), 0);
                this.updateMatches(false);
                if (!this.matches.isEmpty()) {
                    for (int i = 0; i < this.matches.size(); i++) {
                        if (this.matches.get(i)[0] >= m[0] + repl.length()) {
                            this.currentMatch = i;
                            break;
                        }
                    }

                    this.selectMatch();
                }
            }
        }
    }

    void replaceAll() {
        if (!this.readOnly && !this.matches.isEmpty()) {
            String repl = this.findBar.replace.text();
            StringBuilder sb = new StringBuilder(this.doc.text());

            for (int i = this.matches.size() - 1; i >= 0; i--) {
                int[] m = this.matches.get(i);
                sb.replace(m[0], m[1], repl);
            }

            int count = this.matches.size();
            this.replaceAllText(sb.toString());
            this.updateMatches(false);
            UI ui = UI.get();
            if (ui != null) {
                ui.toastInfo("Заменено: " + count);
            }
        }
    }

    @Override
    public void focusLost() {
        this.closePopup();
    }

    @Override
    public void focusGained() {
        this.blinkStart = System.currentTimeMillis();
    }

    public record Chip(String label, int swatch, int color, BiConsumer<Integer, Integer> action) {
    }
}
