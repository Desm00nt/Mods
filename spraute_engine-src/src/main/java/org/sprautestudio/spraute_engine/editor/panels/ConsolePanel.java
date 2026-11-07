package org.sprautestudio.spraute_engine.editor.panels;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public class ConsolePanel extends Widget {
    private final TextField input = new TextField().placeholder("Команда, например: script start 5001   (↑↓ — история)");
    private final Button clear = Button.icon(Icon.TRASH, "Очистить", () -> ClientState.get().clearLog());
    private final List<String> history = new ArrayList<>();
    private int historyIndex = -1;
    private double scroll;
    private boolean stickToBottom = true;
    private int seenVersion = -1;
    private static final int LINE = 11;
    private final SimpleDateFormat time = new SimpleDateFormat("HH:mm:ss");

    public ConsolePanel() {
        this.add(this.input);
        this.add(this.clear);
        this.input.icon = Icon.TERMINAL;
        this.input.onSubmit = s -> {
            String cmd = s.trim();
            if (!cmd.isEmpty()) {
                if (this.history.isEmpty() || !this.history.get(this.history.size() - 1).equals(cmd)) {
                    this.history.add(cmd);
                }

                this.historyIndex = -1;
                ClientState.get().command(cmd);
                this.input.setText("");
                this.stickToBottom = true;
            }
        };
        this.input.keyHook = (key, mods) -> {
            if (key == 265 && !this.history.isEmpty()) {
                this.historyIndex = this.historyIndex < 0 ? this.history.size() - 1 : Math.max(0, this.historyIndex - 1);
                this.input.setText(this.history.get(this.historyIndex));
                return true;
            } else if (key == 264 && this.historyIndex >= 0) {
                this.historyIndex++;
                this.input.setText(this.historyIndex >= this.history.size() ? "" : this.history.get(this.historyIndex));
                if (this.historyIndex >= this.history.size()) {
                    this.historyIndex = -1;
                }

                return true;
            } else {
                return false;
            }
        };
    }

    public void focusInput() {
        this.input.requestFocus();
    }

    @Override
    public void layout() {
        this.input.bounds(this.x + 6, this.y + this.h - 22, this.w - 34, 18);
        this.clear.bounds(this.x + this.w - 24, this.y + this.h - 22, 18, 18);
    }

    private int logH() {
        return this.h - 26;
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        List<ClientState.LogLine> lines = ClientState.get().log();
        int total = lines.size() * 11 + 6;
        int max = Math.max(0, total - this.logH());
        if (this.seenVersion != ClientState.get().logVersion()) {
            this.seenVersion = ClientState.get().logVersion();
            if (this.stickToBottom) {
                this.scroll = max;
            }
        }

        this.scroll = Math.max(0.0, Math.min((double)max, this.scroll));
        c.pushClip(this.x, this.y, this.w, this.logH());
        if (lines.isEmpty()) {
            c.text("Здесь появятся сообщения сюжетов, ошибки и вывод команд /script.", this.x + 8, this.y + 8, -9933435);
        }

        int first = (int)(this.scroll / 11.0);

        for (int i = first; i < lines.size(); i++) {
            int ly = this.y + 4 + i * 11 - (int)this.scroll;
            if (ly > this.y + this.logH()) {
                break;
            }

            ClientState.LogLine l = lines.get(i);

            int color = switch (l.level()) {
                case 1 -> -11746422;
                case 2 -> -870325;
                case 3 -> -1023878;
                case 4 -> -1841682;
                default -> -6577995;
            };
            String ts = this.time.format(new Date(l.time()));
            c.text(ts, this.x + 8, ly, -9933435);
            int sx = this.x + 8 + c.width(ts) + 6;
            String src = l.source();
            c.text(src, sx, ly, -6255873);
            sx += c.width(src) + 6;
            c.text(c.ellipsize(l.text(), this.x + this.w - sx - 6), sx, ly, color);
        }

        c.popClip();
        c.fill(this.x, this.y + this.h - 26, this.w, 1, -13749952);
    }

    @Override
    public String tooltipText(int mx, int my) {
        List<ClientState.LogLine> lines = ClientState.get().log();
        int idx = (int)((my - this.y - 4 + this.scroll) / 11.0);
        if (my <= this.y + this.logH() && idx >= 0 && idx < lines.size()) {
            String t = lines.get(idx).text();
            return t.length() > 60 ? t : null;
        } else {
            return null;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        List<ClientState.LogLine> lines = ClientState.get().log();
        int max = Math.max(0, lines.size() * 11 + 6 - this.logH());
        this.scroll = Math.max(0.0, Math.min((double)max, this.scroll - delta * 11.0 * 3.0));
        this.stickToBottom = this.scroll >= max - 1;
        return true;
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        if (button == 1 && my < this.y + this.logH()) {
            List<ClientState.LogLine> lines = ClientState.get().log();
            int idx = (int)((my - this.y - 4.0 + this.scroll) / 11.0);
            if (idx >= 0 && idx < lines.size()) {
                UI.setClipboard(lines.get(idx).text());
                UI ui = UI.get();
                if (ui != null) {
                    ui.toastInfo("Строка скопирована");
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
