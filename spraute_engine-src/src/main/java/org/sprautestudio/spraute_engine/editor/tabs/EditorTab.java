package org.sprautestudio.spraute_engine.editor.tabs;

import java.io.IOException;
import java.util.List;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;

public abstract class EditorTab extends Widget {
    public final Project.FileRef file;
    protected final Project project;
    public Runnable onDirtyChanged;

    protected EditorTab(Project project, Project.FileRef file) {
        this.project = project;
        this.file = file;
    }

    public String title() {
        return this.file != null ? this.file.name() : "";
    }

    public abstract Icon icon();

    public abstract int iconColor();

    public abstract boolean isDirty();

    public abstract void save() throws IOException;

    public abstract String source();

    public abstract int scriptId();

    public abstract List<EditorTab.Problem> problems();

    public abstract Analyzer.Result analysis();

    public abstract void reveal(int var1);

    public abstract void focusContent();

    public void setShown(boolean shown) {
    }

    public boolean showsWorld() {
        return false;
    }

    public boolean wantsRoom() {
        return false;
    }

    public Widget inspector() {
        return null;
    }

    public Widget hierarchy() {
        return null;
    }

    public void undo() {
    }

    public void redo() {
    }

    public record Problem(LanguageSupport.Severity severity, String message, String where, Runnable reveal) {
    }
}
