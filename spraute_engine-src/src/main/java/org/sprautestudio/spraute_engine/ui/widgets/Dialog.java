package org.sprautestudio.spraute_engine.ui.widgets;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;

public class Dialog extends Widget implements UI.OverlayLayout {
    private final String title;
    private final Icon icon;
    public final Box body = Box.column().gap(6);
    public final Box buttons = Box.row().gap(6).justify(Box.Align.END);
    public int dialogW = 300;
    public Runnable onEnter;
    private static final int HEADER = 24;

    public Dialog(String title, Icon icon) {
        this.title = title;
        this.icon = icon;
        this.add(this.body);
        this.add(this.buttons);
        this.focusable = true;
    }

    public Dialog width(int w) {
        this.dialogW = w;
        return this;
    }

    public Button addButton(String text, Button.Style style, Runnable action) {
        Button b = new Button(text, action).style(style);
        b.autoWidth();
        b.prefW = Math.max(70, b.prefW);
        this.buttons.add(b);
        return b;
    }

    public void open() {
        UI ui = UI.get();
        if (ui != null) {
            ui.openOverlay(this, true, false, null);
            Widget field = findField(this.body);
            if (field != null) {
                field.requestFocus();
            } else {
                this.requestFocus();
            }
        }
    }

    private static Widget findField(Widget w) {
        if (w instanceof TextField) {
            return w;
        } else {
            for (Widget c : w.children()) {
                Widget f = findField(c);
                if (f != null) {
                    return f;
                }
            }

            return null;
        }
    }

    public void close() {
        UI ui = UI.get();
        if (ui != null) {
            ui.closeOverlay(this);
        }
    }

    private int bodyHeight() {
        int hsum = 0;
        int n = 0;

        for (Widget c : this.body.children()) {
            if (c.visible) {
                hsum += Math.max(0, c.prefH);
                n++;
            }
        }

        return hsum + Math.max(0, n - 1) * this.body.gap;
    }

    @Override
    public void layoutOverlay(int vw, int vh) {
        int dw = Math.min(this.dialogW, vw - 20);
        int dh = 34 + this.bodyHeight() + 12 + 20 + 12;
        this.bounds((vw - dw) / 2, Math.max(10, (vh - dh) / 2 - 20), dw, dh);
        this.body.bounds(this.x + 12, this.y + 24 + 10, this.w - 24, this.bodyHeight());
        this.body.layout();
        this.buttons.bounds(this.x + 12, this.y + this.h - 32, this.w - 24, 20);
        this.buttons.layout();
    }

    @Override
    public void layout() {
        UI ui = UI.get();
        if (ui != null) {
            this.layoutOverlay(ui.vw, ui.vh);
        }
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.shadow(this.x, this.y, this.w, this.h, 6, 10);
        c.roundBorder(this.x, this.y, this.w, this.h, 6, -14934490, -12762793);
        c.fill(this.x + 1, this.y + 24, this.w - 2, 1, -13749952);
        int tx = this.x + 12;
        if (this.icon != null) {
            c.icon(this.icon, tx, this.y + 7, -7639809);
            tx += 14;
        }

        c.text(this.title, tx, this.y + 8, -1841682);
    }

    @Override
    public boolean mousePressed(double mx, double my, int button) {
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            this.close();
            return true;
        } else if ((key == 257 || key == 335) && this.onEnter != null) {
            this.onEnter.run();
            return true;
        } else {
            return false;
        }
    }

    public static void confirm(String title, String message, String okText, boolean danger, Runnable onOk) {
        Dialog d = new Dialog(title, danger ? Icon.WARNING : Icon.INFO);
        Label l = new Label(message).wrap().color(-6577995);
        l.prefH = Minecraft.getInstance().font.split(Component.literal(message), 276).size() * 11 + 2;
        d.body.add(l);
        d.addButton("Отмена", Button.Style.DEFAULT, d::close);
        Runnable ok = () -> {
            d.close();
            onOk.run();
        };
        d.addButton(okText, danger ? Button.Style.DANGER : Button.Style.PRIMARY, ok);
        d.onEnter = ok;
        d.open();
    }

    public static void info(String title, String message) {
        Dialog d = new Dialog(title, Icon.INFO).width(340);
        Label l = new Label(message).wrap().color(-6577995);
        l.prefH = Minecraft.getInstance().font.split(Component.literal(message), 316).size() * 11 + 2;
        d.body.add(l);
        d.addButton("OK", Button.Style.PRIMARY, d::close);
        d.onEnter = d::close;
        d.open();
    }

    public static void prompt(String title, String label, String initial, Function<String, String> validator, Consumer<String> onOk) {
        Dialog d = new Dialog(title, Icon.EDIT);
        Label l = new Label(label).color(-6577995);
        d.body.add(l);
        TextField field = new TextField();
        field.setText(initial);
        field.selectAll();
        d.body.add(field);
        Label error = new Label("").color(-1023878);
        error.prefH = 10;
        d.body.add(error);
        d.addButton("Отмена", Button.Style.DEFAULT, d::close);
        Runnable ok = () -> {
            String v = field.text().trim();
            String err = validator != null ? validator.apply(v) : null;
            if (err != null) {
                error.setText(err);
            } else {
                d.close();
                onOk.accept(v);
            }
        };
        field.onSubmit = s -> ok.run();
        field.onChange = s -> error.setText("");
        d.addButton("OK", Button.Style.PRIMARY, ok);
        d.onEnter = ok;
        d.open();
    }
}
