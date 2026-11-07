package org.sprautestudio.spraute_engine.editor.assets;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.widgets.Button;

public class ImageTab extends EditorTab {
    public final Project.ImageRef image;
    private final Button copyIcon;
    private final Button copyPortrait;
    private final Button insertCharacter;

    public ImageTab(Project project, Project.ImageRef image, Consumer<String> insertCode) {
        super(project, null);
        this.image = image;
        this.copyIcon = this.add(
            new Button("icon \"" + image.name() + "\"", () -> this.copy("icon \"" + image.name() + "\"")).icon(Icon.COPY).style(Button.Style.GHOST)
        );
        this.copyIcon.tooltip("Иконка в окне речи (say). Допишите в объявление персонажа");
        this.copyPortrait = this.add(
            new Button("portrait \"" + image.name() + "\"", () -> this.copy("portrait \"" + image.name() + "\"")).icon(Icon.COPY).style(Button.Style.GHOST)
        );
        this.copyPortrait.tooltip("Лицо в диалоге с выбором (choice). Допишите в объявление персонажа");
        this.insertCharacter = this.add(
            new Button(
                    "Персонаж с этой картинкой",
                    () -> {
                        String var = Images.slug(image.name());
                        insertCode.accept(
                            "character "
                                + var
                                + " = \"spraute:npc\" name \""
                                + image.name().replace("\"", "'")
                                + "\" icon \""
                                + image.name()
                                + "\" portrait \""
                                + image.name()
                                + "\""
                        );
                    }
                )
                .icon(Icon.USER)
                .style(Button.Style.PRIMARY)
        );

        for (Button b : List.of(this.copyIcon, this.copyPortrait, this.insertCharacter)) {
            b.autoWidth();
        }
    }

    private void copy(String s) {
        Minecraft.getInstance().keyboardHandler.setClipboard(s);
        UI ui = UI.get();
        if (ui != null) {
            ui.toastInfo("Скопировано: " + s);
        }
    }

    @Override
    public String title() {
        return this.image.name();
    }

    @Override
    public void layout() {
        int bx = this.x + 12;
        int by = this.y + this.h - 30;

        for (Button b : List.of(this.copyIcon, this.copyPortrait, this.insertCharacter)) {
            b.bounds(bx, by, b.prefW, 20);
            bx += b.prefW + 6;
        }

        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        ProjectImages.Loaded tex = ProjectImages.load(this.project, this.image);
        int areaY = this.y + 34;
        int areaH = this.h - 34 - 40;
        c.text(this.image.name(), this.x + 12, this.y + 10, -1841682);
        String info = tex == null ? "не удалось прочитать файл" : tex.width() + " × " + tex.height() + "   " + this.image.path().getFileName();
        c.text(info, this.x + 18 + c.width(this.image.name()), this.y + 10, -9933435);
        c.fill(this.x, areaY - 4, this.w, 1, -13749952);
        if (tex == null) {
            c.textCentered("Картинка не читается. Поддерживаются PNG, JPG, BMP и GIF", this.x + this.w / 2, areaY + areaH / 2, -1023878);
        } else {
            double k = Math.min(Math.min((double)(this.w - 40) / tex.width(), (double)(areaH - 20) / tex.height()), 4.0);
            int iw = Math.max(1, (int)(tex.width() * k));
            int ih = Math.max(1, (int)(tex.height() * k));
            int ix = this.x + (this.w - iw) / 2;
            int iy = areaY + (areaH - ih) / 2;
            int cell = 8;
            c.pushClip(ix, iy, iw, ih);

            for (int yy = 0; yy < ih; yy += cell) {
                for (int xx = 0; xx < iw; xx += cell) {
                    c.fill(ix + xx, iy + yy, cell, cell, (xx + yy) / cell % 2 == 0 ? -14013384 : -14539730);
                }
            }

            c.popClip();
            c.g.blit(tex.texture(), ix, iy, iw, ih, 0.0F, 0.0F, tex.width(), tex.height(), tex.width(), tex.height());
            c.outline(ix - 1, iy - 1, iw + 2, ih + 2, -12762793);
            c.fill(this.x, this.y + this.h - 40, this.w, 40, -14934490);
            c.fill(this.x, this.y + this.h - 40, this.w, 1, -13749952);
        }
    }

    @Override
    public Icon icon() {
        return Icon.IMAGE;
    }

    @Override
    public int iconColor() {
        return -10638858;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void save() {
    }

    @Override
    public String source() {
        return null;
    }

    @Override
    public int scriptId() {
        return -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        return List.of();
    }

    @Override
    public Analyzer.Result analysis() {
        return null;
    }

    @Override
    public void reveal(int offset) {
    }

    @Override
    public void focusContent() {
    }
}
