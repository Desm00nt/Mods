package org.sprautestudio.spraute_engine.editor.assets;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.tabs.EditorTab;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.ListView;

public class ModelTab extends EditorTab {
    public final Project.ModelRef model;
    private final ModelView view = new ModelView();
    private final ListView<String> anims;
    private final Button character;
    private final Button reload;
    private final Button folder;
    private String key = "";
    private final Consumer<String> createEntity;

    public ModelTab(Project project, Project.ModelRef model, Consumer<String> createEntity) {
        super(project, null);
        this.model = model;
        this.createEntity = createEntity;
        this.add(this.view);
        this.view.emptyText = "Войдите в мир, чтобы увидеть модель";
        this.anims = this.add(new ListView<>((c, a, x, y, w, h, sel, hov) -> {
            c.icon(Icon.FILM, x + 6, y + (h - 9) / 2, a.equals(this.view.playing()) ? -11746422 : -9933435);
            c.textEllipsized(a, x + 20, y + (h - 8) / 2, w - 24, sel ? -1841682 : -6577995);
        }));
        this.anims.rowHeight = 18;
        this.anims.background = -14934490;
        this.anims.onActivate = this::play;
        this.anims.onSelect = this::play;
        this.character = this.add(
            new Button("Создать сущность с этой моделью", () -> createEntity.accept(model.name())).icon(Icon.USER).style(Button.Style.PRIMARY)
        );
        this.reload = this.add(new Button("Перезагрузить файлы", this::refresh).icon(Icon.REFRESH).style(Button.Style.GHOST));
        this.reload.tooltip("Перечитать модель, текстуру и анимации после правки в Blockbench");
        this.folder = this.add(new Button("Папка модели", () -> Util.getPlatform().openFile(model.dir().toFile())).icon(Icon.FOLDER).style(Button.Style.GHOST));

        for (Button b : List.of(this.character, this.reload, this.folder)) {
            b.autoWidth();
        }

        this.refresh();
    }

    private void refresh() {
        ModelLibrary.scan();
        ModelLibrary.Model m = ModelLibrary.find(this.project.name(), this.model.name());
        this.key = m != null ? m.key() : "";
        String k = this.key;
        this.view.setType(Orbs.MODEL.get(), e -> {
            if (e instanceof SprauteModelEntity me) {
                me.setModel(k);
            }
        });
        this.anims.setItems(ModelLibrary.animations(m));
        UI ui = UI.get();
        if (ui != null && m == null) {
            ui.toastError("В папке модели нет файла .geo.json");
        }
    }

    private void play(String a) {
        if (a != null) {
            String err = this.view.play("spraute:" + this.project.name() + "/" + this.model.name() + "/" + a, "action", true, 1.0F);
            UI ui = UI.get();
            if (err != null && ui != null) {
                ui.toastError(err);
            }
        }
    }

    @Override
    public String title() {
        return this.model.name();
    }

    @Override
    public void layout() {
        int lw = Math.min(240, this.w / 3);
        this.view.bounds(this.x, this.y + 30, this.w - lw, this.h - 30 - 34);
        this.anims.bounds(this.x + this.w - lw, this.y + 30, lw, this.h - 30 - 34);
        int bx = this.x + 10;
        int by = this.y + this.h - 27;

        for (Button b : List.of(this.character, this.reload, this.folder)) {
            b.bounds(bx, by, b.prefW, 20);
            bx += b.prefW + 6;
        }

        super.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y, this.w, this.h, -15461093);
        ModelLibrary.Model m = ModelLibrary.byKey(this.key);
        c.text(this.model.name(), this.x + 10, this.y + 10, -1841682);
        String info = m == null
            ? "нет .geo.json"
            : "файлы модели · анимаций: " + this.anims.items().size() + (m.texture() == null ? "   ·   нет текстуры (.png)" : "");
        c.text(info, this.x + 20 + c.width(this.model.name()), this.y + 10, m == null ? -1023878 : -9933435);
        c.fill(this.x, this.y + 29, this.w, 1, -13749952);
        int lw = Math.min(240, this.w / 3);
        c.fill(this.x + this.w - lw, this.y + 30, 1, this.h - 64, -13749952);
        if (this.anims.items().isEmpty()) {
            c.textCentered("нет анимаций (.animation.json)", this.x + this.w - lw / 2, this.y + 50, -9933435);
        }

        c.fill(this.x, this.y + this.h - 34, this.w, 34, -14934490);
        c.fill(this.x, this.y + this.h - 34, this.w, 1, -13749952);
    }

    @Override
    public Icon icon() {
        return Icon.CUBE;
    }

    @Override
    public int iconColor() {
        return -12988235;
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
