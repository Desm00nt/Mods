package org.sprautestudio.spraute_engine.ui.code;

import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class FindBar extends Widget {
    final TextField find = new TextField().placeholder("Найти");
    final TextField replace = new TextField().placeholder("Заменить");
    private final Button caseBtn;
    private final Button wordBtn;
    private final Button prevBtn;
    private final Button nextBtn;
    private final Button replaceBtn;
    private final Button replaceAllBtn;
    private final Button closeBtn;
    boolean matchCase;
    boolean wholeWord;
    boolean showReplace;
    private final CodeEditor editor;
    static final int W = 280;

    FindBar(CodeEditor editor) {
        this.editor = editor;
        this.visible = false;
        this.add(this.find);
        this.add(this.replace);
        this.caseBtn = this.add(new Button("Aa", () -> {
            this.matchCase = !this.matchCase;
            editor.updateMatches(true);
        }).style(Button.Style.GHOST).toggle(() -> this.matchCase));
        this.caseBtn.tooltip("Учитывать регистр");
        this.wordBtn = this.add(new Button("W", () -> {
            this.wholeWord = !this.wholeWord;
            editor.updateMatches(true);
        }).style(Button.Style.GHOST).toggle(() -> this.wholeWord));
        this.wordBtn.tooltip("Слово целиком");
        this.prevBtn = this.add(Button.icon(Icon.CHEVRON_UP, "Предыдущее (Shift+Enter)", () -> editor.findNext(false)));
        this.nextBtn = this.add(Button.icon(Icon.CHEVRON_DOWN, "Следующее (Enter)", () -> editor.findNext(true)));
        this.closeBtn = this.add(Button.icon(Icon.CLOSE, "Закрыть (Esc)", this::hide));
        this.replaceBtn = this.add(new Button("Заменить", editor::replaceCurrent).style(Button.Style.GHOST));
        this.replaceAllBtn = this.add(new Button("Все", editor::replaceAll).style(Button.Style.GHOST));
        this.replaceAllBtn.tooltip("Заменить все");
        this.find.onChange = s -> editor.updateMatches(true);
        this.find.keyHook = (key, mods) -> {
            if (key == 257 || key == 335) {
                editor.findNext(!UI.shift(mods));
                return true;
            } else if (key == 256) {
                this.hide();
                return true;
            } else {
                return false;
            }
        };
        this.replace.keyHook = (key, mods) -> {
            if (key != 257 && key != 335) {
                if (key == 256) {
                    this.hide();
                    return true;
                } else {
                    return false;
                }
            } else {
                if (UI.ctrl(mods)) {
                    editor.replaceAll();
                } else {
                    editor.replaceCurrent();
                }

                return true;
            }
        };
    }

    void show(boolean withReplace, String initial) {
        this.visible = true;
        this.showReplace = withReplace;
        if (initial != null && !initial.isEmpty() && !initial.contains("\n")) {
            this.find.setText(initial);
        }

        this.find.selectAll();
        this.find.requestFocus();
        this.editor.layout();
        this.editor.updateMatches(true);
    }

    void hide() {
        this.visible = false;
        this.editor.clearMatches();
        this.editor.requestFocus();
    }

    int barHeight() {
        return this.showReplace ? 48 : 26;
    }

    @Override
    public void layout() {
        int fx = this.x + 6;
        this.find.bounds(fx, this.y + 4, 168, 18);
        this.caseBtn.bounds(fx + 280 - 106, this.y + 4, 18, 18);
        this.wordBtn.bounds(fx + 280 - 86, this.y + 4, 18, 18);
        this.prevBtn.bounds(fx + 280 - 66, this.y + 4, 18, 18);
        this.nextBtn.bounds(fx + 280 - 46, this.y + 4, 18, 18);
        this.closeBtn.bounds(fx + 280 - 26, this.y + 4, 18, 18);
        this.replace.visible = this.replaceBtn.visible = this.replaceAllBtn.visible = this.showReplace;
        this.replace.bounds(fx, this.y + 26, 168, 18);
        this.replaceBtn.bounds(fx + 280 - 106, this.y + 26, 58, 18);
        this.replaceAllBtn.bounds(fx + 280 - 46, this.y + 26, 38, 18);
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.shadow(this.x, this.y, this.w, this.h, 4, 5);
        c.roundBorder(this.x, this.y, this.w, this.h, 4, -14210506, -12762793);
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        String count = this.editor.matchSummary();
        if (!count.isEmpty()) {
            int cw = c.width(count);
            c.text(count, this.find.x + this.find.w - cw - 5, this.find.y + 5, -9933435);
        }
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        return true;
    }
}
