package org.sprautestudio.spraute_engine.editor.tabs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.visual.DialogueImport;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.ui.Box;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.UI;
import org.sprautestudio.spraute_engine.ui.Widget;
import org.sprautestudio.spraute_engine.ui.code.CodeEditor;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;
import org.sprautestudio.spraute_engine.ui.widgets.Button;
import org.sprautestudio.spraute_engine.ui.widgets.Dropdown;
import org.sprautestudio.spraute_engine.ui.widgets.Label;
import org.sprautestudio.spraute_engine.ui.widgets.ScrollView;
import org.sprautestudio.spraute_engine.ui.widgets.TextField;

public final class FromTextTab extends EditorTab {
    private static final LanguageSupport PLAIN = new LanguageSupport() {
        @Override
        public LanguageSupport.Analysis analyze(String text) {
            final int[] colors = new int[text.length()];
            Arrays.fill(colors, -1841682);
            return new LanguageSupport.Analysis() {
                @Override
                public List<LanguageSupport.Problem> problems() {
                    return List.of();
                }

                @Override
                public int[] colors() {
                    return colors;
                }
            };
        }

        @Override
        public List<LanguageSupport.Completion> complete(String text, int caret, LanguageSupport.Analysis analysis, boolean explicit) {
            return List.of();
        }

        @Override
        public String hover(String text, int offset, LanguageSupport.Analysis analysis) {
            return null;
        }

        @Override
        public LanguageSupport.Signature signature(String text, int caret, LanguageSupport.Analysis analysis) {
            return null;
        }

        @Override
        public int definition(String text, int offset, LanguageSupport.Analysis analysis) {
            return -1;
        }

        @Override
        public String lineComment() {
            return "#";
        }
    };
    private EditorTab target;
    private final BiConsumer<EditorTab, DialogueImport.Plan> onApply;
    private final CodeEditor input = this.add(new CodeEditor("", PLAIN));
    private final Label heading = this.add(new Label("Имя персонажа: реплика — одна реплика на строку").color(-6577995));
    private final Label transitionsTitle = this.add(new Label("Переходы между репликами").color(-6577995));
    private final Box allRow = this.add(Box.row().gap(6));
    private final FromTextTab.Gap all = new FromTextTab.Gap();
    private List<String> speakers = List.of();
    private final Box rows = Box.column().gap(4).pad(4);
    private final ScrollView transitions = this.add(new ScrollView(this.rows));
    private final Box toolbar = this.add(Box.row().gap(8).pad(8, 4).background(-14934490));
    private final Label status = new Label("Выберите переход для всех реплик или для каждой отдельно").color(-9933435);
    private final List<FromTextTab.Gap> gaps = new ArrayList<>();

    public FromTextTab(Project project, EditorTab target, BiConsumer<EditorTab, DialogueImport.Plan> onApply) {
        super(project, null);
        this.target = target;
        this.onApply = onApply;
        this.input.showMinimap = false;
        this.input.emptyHint = "Вставьте реплики: Алиса: Привет!";
        this.input.onChange = editor -> this.rebuild();
        Button apply = new Button("Добавить в скрипт", this::apply).icon(Icon.PLUS).style(Button.Style.PRIMARY);
        apply.autoWidth();
        this.toolbar.add(apply);
        this.status.flex = 1.0F;
        this.toolbar.add(this.status);
        this.buildAllRow();
        this.rebuild();
    }

    public void target(EditorTab target) {
        this.target = target;
    }

    private void buildAllRow() {
        this.allRow.clear();
        Label label = new Label("Для всех реплик").color(-6577995);
        label.prefW = 150;
        this.allRow.add(label);
        this.allRow.add(this.triggerChoice(this.all, () -> {
            for (FromTextTab.Gap gap : this.gaps) {
                this.copyAll(gap);
            }

            this.buildAllRow();
            this.rebuild();
        }));
        this.allRow.add(this.valueEditor(this.all, text -> {
            this.all.value = text;

            for (FromTextTab.Gap gap : this.gaps) {
                this.copyAll(gap);
            }

            this.rebuild();
        }));
    }

    private void copyAll(FromTextTab.Gap gap) {
        gap.trigger = this.all.trigger;
        gap.value = this.all.value;
    }

    private Dropdown<DialogueImport.Trigger> triggerChoice(FromTextTab.Gap gap, Runnable changed) {
        Dropdown<DialogueImport.Trigger> trigger = new Dropdown<>(
            () -> List.of(DialogueImport.Trigger.F8, DialogueImport.Trigger.KEY, DialogueImport.Trigger.NPC),
            () -> gap.trigger,
            FromTextTab::triggerName,
            choice -> {
                if (choice != gap.trigger) {
                    gap.value = "";
                }

                gap.trigger = choice;
                changed.run();
            }
        );
        trigger.prefW = 145;
        return trigger;
    }

    private Widget valueEditor(FromTextTab.Gap gap, Consumer<String> set) {
        Widget editor;
        if (gap.trigger == DialogueImport.Trigger.NPC) {
            editor = new Dropdown<>(() -> this.npcChoices(gap), () -> gap.value, v -> v.isEmpty() ? "Выберите NPC" : v, set);
        } else {
            TextField key = new TextField().placeholder("Клавиша, например H");
            key.setText(gap.value);
            key.onChange = text -> set.accept(text);
            editor = key;
        }

        editor.visible = gap.trigger != DialogueImport.Trigger.F8;
        editor.flex = 1.0F;
        return editor;
    }

    private List<String> npcChoices(FromTextTab.Gap gap) {
        Set<String> names = new LinkedHashSet<>();

        for (String speaker : this.speakers) {
            if (!speaker.equalsIgnoreCase("player") && !speaker.equalsIgnoreCase("игрок")) {
                names.add(speaker);
            }
        }

        names.addAll(AssetIndex.characters());
        if (!gap.value.isEmpty()) {
            names.add(gap.value);
        }

        return new ArrayList<>(names);
    }

    private void rebuild() {
        List<String> speakers = new ArrayList<>();

        for (String raw : this.input.text().split("\\R")) {
            if (!raw.isBlank()) {
                int colon = raw.indexOf(58);
                speakers.add((colon >= 0 ? raw.substring(0, colon) : raw).trim());
            }
        }

        this.speakers = speakers;
        int count = Math.max(0, speakers.size() - 1);

        while (this.gaps.size() < count) {
            FromTextTab.Gap gap = new FromTextTab.Gap();
            this.copyAll(gap);
            this.gaps.add(gap);
        }

        while (this.gaps.size() > count) {
            this.gaps.remove(this.gaps.size() - 1);
        }

        this.allRow.visible = count > 0;
        this.rows.clear();

        for (int i = 0; i < count; i++) {
            FromTextTab.Gap gap = this.gaps.get(i);
            Box row = Box.row().gap(6);
            row.prefH = 20;
            Label label = new Label("После " + (i + 1) + ": " + speakers.get(i)).color(-6577995);
            label.prefW = 150;
            row.add(label);
            row.add(this.triggerChoice(gap, this::rebuild));
            row.add(this.valueEditor(gap, text -> gap.value = text));
            this.rows.add(row);
        }

        this.rows.prefH = 8 + count * 24;
        UI ui = UI.get();
        if (ui != null) {
            ui.relayout();
        }
    }

    private static String triggerName(DialogueImport.Trigger trigger) {
        return switch (trigger) {
            case F8 -> "F8";
            case KEY -> "Клавиша";
            case NPC -> "Нажатие по NPC";
        };
    }

    private void apply() {
        try {
            List<DialogueImport.Line> lines = DialogueImport.parse(this.input.text());
            if (this.gaps.size() != lines.size() - 1) {
                this.rebuild();
            }

            List<DialogueImport.Transition> choices = new ArrayList<>();

            for (FromTextTab.Gap gap : this.gaps) {
                choices.add(new DialogueImport.Transition(gap.trigger, gap.value));
            }

            DialogueImport.Plan plan = DialogueImport.plan(this.target.source(), lines, choices);
            this.onApply.accept(this.target, plan);
            this.status.setText("Добавлено: " + lines.size() + " реплик");
        } catch (IllegalStateException | IllegalArgumentException var5) {
            this.status.setText(var5.getMessage());
        }
    }

    @Override
    public void layout() {
        this.heading.bounds(this.x + 8, this.y + 8, this.w - 16, 16);
        int editorH = Math.max(90, (this.h - 94) / 2);
        this.input.bounds(this.x + 8, this.y + 26, this.w - 16, editorH);
        this.input.layout();
        this.transitionsTitle.bounds(this.x + 8, this.y + 32 + editorH, this.w - 16, 16);
        this.allRow.bounds(this.x + 12, this.y + 50 + editorH, this.w - 24, 20);
        this.allRow.layout();
        int scrollY = this.y + 74 + editorH;
        this.transitions.bounds(this.x + 8, scrollY, this.w - 16, Math.max(0, this.y + this.h - 32 - scrollY));
        this.transitions.layout();
        this.toolbar.bounds(this.x, this.y + this.h - 26, this.w, 26);
        this.toolbar.layout();
    }

    @Override
    public String title() {
        return "Из текста";
    }

    @Override
    public Icon icon() {
        return Icon.EDIT;
    }

    @Override
    public int iconColor() {
        return -7639809;
    }

    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public void save() throws IOException {
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
        this.input.gotoOffset(offset, 0);
    }

    @Override
    public void focusContent() {
        this.input.requestFocus();
    }

    private static final class Gap {
        DialogueImport.Trigger trigger = DialogueImport.Trigger.F8;
        String value = "";
    }
}
