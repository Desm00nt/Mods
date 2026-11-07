package org.sprautestudio.spraute_engine.editor.tabs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.sprautestudio.spraute_engine.editor.SstLanguage;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.visual.BlockCodegen;
import org.sprautestudio.spraute_engine.editor.visual.BlockEditor;
import org.sprautestudio.spraute_engine.editor.visual.VisualScript;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Diagnostic;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Canvas;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.code.CodeEditor;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Label;
import org.sprautestudio.spraute_engine.ui.widgets.SplitPane;

public class BlocksTab extends EditorTab {
    public final BlockEditor blocks;
    private final CodeEditor preview;
    private final SplitPane split;
    private final Box toolbar;
    private boolean dirty;
    private int previewVersion = -1;
    private int changeCounter;

    public BlocksTab(Project project, Project.FileRef file, String json, Supplier<String> position, Consumer<String> convertToText) {
        super(project, file);
        this.blocks = new BlockEditor(VisualScript.fromJson(json));
        this.blocks.positionProvider = position;
        this.blocks.assetPicker = AssetPicker::open;
        this.blocks.onChange = b -> {
            this.changeCounter++;
            if (!this.dirty) {
                this.dirty = true;
                if (this.onDirtyChanged != null) {
                    this.onDirtyChanged.run();
                }
            }
        };
        this.preview = new CodeEditor("", new SstLanguage());
        this.preview.readOnly = true;
        this.preview.showMinimap = false;
        this.preview.visible = false;
        this.split = this.add(new SplitPane(true, this.blocks, this.preview, false, 300));
        this.toolbar = this.add(Box.row().gap(4).pad(6, 3).background(-14934490));
        Button code = new Button("Код", () -> {
            this.preview.visible = !this.preview.visible;
            this.split.layout();
        }).icon(Icon.CODE).style(Button.Style.GHOST).toggle(() -> this.preview.visible);
        code.autoWidth();
        code.tooltip("Показать сгенерированный код SST");
        this.toolbar.add(code);
        Button toText = new Button("В текстовый скрипт", () -> convertToText.accept(this.blocks.generated().text)).icon(Icon.FILE).style(Button.Style.GHOST);
        toText.autoWidth();
        toText.tooltip("Создать .sst файл из этих блоков");
        this.toolbar.add(toText);
        Button home = new Button("К началу", this.blocks::goHome).icon(Icon.HOME).style(Button.Style.GHOST);
        home.autoWidth();
        this.toolbar.add(home);
        Label hint = new Label("ЛКМ — перетащить, ПКМ — меню, Ctrl+колесо — масштаб, Ctrl — тащить один блок").color(-9933435);
        hint.flex = 1.0F;
        hint.align = Label.Align.RIGHT;
        this.toolbar.add(hint);
    }

    @Override
    public void layout() {
        this.toolbar.bounds(this.x, this.y, this.w, 24);
        this.toolbar.layout();
        this.split.bounds(this.x, this.y + 24, this.w, this.h - 24);
        this.split.layout();
        this.blocks.layout();
    }

    @Override
    protected void paint(Canvas c, int mx, int my, float dt) {
        if (this.preview.visible && this.previewVersion != this.changeCounter) {
            this.previewVersion = this.changeCounter;
            this.preview.setText(this.blocks.generated().text);
        }
    }

    @Override
    protected void paintOver(Canvas c, int mx, int my, float dt) {
        c.fill(this.x, this.y + 23, this.w, 1, -13749952);
    }

    @Override
    public Icon icon() {
        return Icon.GRAPH;
    }

    @Override
    public int iconColor() {
        return -870325;
    }

    @Override
    public boolean isDirty() {
        return this.dirty;
    }

    @Override
    public void save() throws IOException {
        this.project.write(this.file, this.blocks.script().toJson());
        this.dirty = false;
        if (this.onDirtyChanged != null) {
            this.onDirtyChanged.run();
        }
    }

    @Override
    public String source() {
        return this.blocks.generated().text;
    }

    @Override
    public int scriptId() {
        BlockCodegen.Result r = this.blocks.generated();
        return r.analysis.program().header != null ? r.analysis.program().header.id : -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        BlockCodegen.Result r = this.blocks.generated();
        List<EditorTab.Problem> list = new ArrayList<>();

        for (String p : r.problems) {
            list.add(new EditorTab.Problem(LanguageSupport.Severity.ERROR, p, this.title(), null));
        }

        for (Diagnostic d : r.analysis.diagnostics) {
            LanguageSupport.Severity sev = d.isError()
                ? LanguageSupport.Severity.ERROR
                : (d.severity == Diagnostic.Severity.WARNING ? LanguageSupport.Severity.WARNING : LanguageSupport.Severity.INFO);
            list.add(new EditorTab.Problem(sev, d.message, this.title() + " (блоки)", null));
        }

        return list;
    }

    @Override
    public Analyzer.Result analysis() {
        return this.blocks.generated().analysis;
    }

    @Override
    public void reveal(int offset) {
        this.blocks.requestFocus();
    }

    @Override
    public void focusContent() {
        this.blocks.requestFocus();
        UI ui = UI.get();
        if (ui != null) {
            ui.relayout();
        }
    }

    @Override
    public void undo() {
        this.blocks.undo();
    }

    @Override
    public void redo() {
        this.blocks.redo();
    }
}
