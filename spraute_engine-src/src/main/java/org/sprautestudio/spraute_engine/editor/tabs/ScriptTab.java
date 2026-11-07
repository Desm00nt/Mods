package org.sprautestudio.spraute_engine.editor.tabs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.sprautestudio.spraute_engine.editor.ClientState;
import org.sprautestudio.spraute_engine.editor.SstLanguage;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.editor.assets.AssetPicker;
import org.sprautestudio.spraute_engine.editor.assets.HeadEditor;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.engine.client.ChatHeads;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.runtime.Colors;
import org.sprautestudio.spraute_engine.ui.Icon;
import org.sprautestudio.spraute_engine.ui.code.CodeEditor;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;
import org.sprautestudio.spraute_engine.ui.code.TextDocument;
import org.sprautestudio.spraute_engine.ui.widgets.ColorPicker;
import org.sprautestudio.spraute_engine.ui.widgets.PopupMenu;

public class ScriptTab extends EditorTab {
    public final CodeEditor editor;
    private final SstLanguage language = new SstLanguage();
    private boolean lastDirty;
    private static final Map<String, String> CHIP_WORDS = Map.ofEntries(
        Map.entry("item", "предмет…"),
        Map.entry("entity", "модель…"),
        Map.entry("anim", "анимация…"),
        Map.entry("sound", "звук…"),
        Map.entry("image", "картинка…"),
        Map.entry("cutscene", "катсцена…"),
        Map.entry("gui", "интерфейс…"),
        Map.entry("vfx", "эффект…"),
        Map.entry("shop", "магазин…"),
        Map.entry("quest", "квест…"),
        Map.entry("zone", "зона…"),
        Map.entry("fight", "стиль…"),
        Map.entry("task", "задача…"),
        Map.entry("hitbox", "хитбокс…")
    );
    private static final LinkedHashMap<String, String> KINDS = new LinkedHashMap<>();
    private static final Pattern COLOR_OPT = Pattern.compile("\\s+color\\s+\"[^\"]*\"");
    private static final Pattern LAST_STRING = Pattern.compile("\"([^\"]*)\"\\s*$");
    private static final Pattern HEAD_OPT = Pattern.compile(
        "\\s+(headscale|headx|heady|headyaw)\\s+\\(?\\s*-?[\\d.]+\\s*\\)?|\\s+headbone\\s+\\(?\\s*\"[^\"]*\"\\s*\\)?"
    );

    public ScriptTab(Project project, Project.FileRef file, String text, Supplier<String> position, Runnable onSave) {
        super(project, file);
        this.language.positionProvider = position;
        this.language.knownScripts = () -> ClientState.get().scriptIds();
        this.editor = this.add(new CodeEditor(text, this.language));
        this.editor.positionProvider = position;
        this.editor.chipProvider = this::lineChips;
        this.editor.contextExtras = pm -> {
            this.valueMenu(pm);
            int ln = this.editor.caretLine();
            String line = this.editor.lineText(ln);
            String t = line.trim();
            if (t.startsWith("namecolor ")) {
                pm.separator();
                pm.item("Выбрать цвет…", Icon.EDIT, null, () -> this.colorEditor(ln, line, false));
            } else if (t.startsWith("character ")) {
                pm.separator();
                pm.item("Голова в чате…", Icon.USER, null, () -> this.headEditor(ln, line));
                pm.item("Цвет имени…", Icon.EDIT, null, () -> this.colorEditor(ln, line, true));
            }
        };
        this.editor.showMinimap = Workspace.settings().minimap;
        this.editor.onSave = onSave;
        this.editor.onChange = e -> {
            if (e.isDirty() != this.lastDirty) {
                this.lastDirty = e.isDirty();
                if (this.onDirtyChanged != null) {
                    this.onDirtyChanged.run();
                }
            }
        };
    }

    @Override
    public void layout() {
        this.editor.bounds(this.x, this.y, this.w, this.h);
        this.editor.layout();
    }

    @Override
    public Icon icon() {
        return Icon.CODE;
    }

    @Override
    public int iconColor() {
        return -8213761;
    }

    @Override
    public boolean isDirty() {
        return this.editor.isDirty();
    }

    @Override
    public void save() throws IOException {
        this.project.write(this.file, this.editor.text());
        this.editor.markSaved();
        this.lastDirty = false;
        if (this.onDirtyChanged != null) {
            this.onDirtyChanged.run();
        }
    }

    @Override
    public String source() {
        return this.editor.text();
    }

    @Override
    public int scriptId() {
        Analyzer.Result r = this.analysis();
        return r != null && r.program().header != null ? r.program().header.id : -1;
    }

    @Override
    public List<EditorTab.Problem> problems() {
        List<EditorTab.Problem> list = new ArrayList<>();
        TextDocument doc = this.editor.document();

        for (LanguageSupport.Problem p : this.editor.problems()) {
            int line = doc.lineOf(Math.min(p.start(), doc.length()));
            int col = Math.min(p.start(), doc.length()) - doc.lineStart(line);
            list.add(
                new EditorTab.Problem(
                    p.severity(),
                    p.message(),
                    this.title() + ":" + (line + 1) + ":" + (col + 1),
                    () -> this.editor.gotoOffset(p.start(), Math.max(0, p.end() - p.start()))
                )
            );
        }

        return list;
    }

    @Override
    public Analyzer.Result analysis() {
        return this.editor.analysis() instanceof SstLanguage.SstAnalysis a ? a.result : null;
    }

    @Override
    public void reveal(int offset) {
        this.editor.gotoOffset(offset, 0);
    }

    @Override
    public void focusContent() {
        this.editor.requestFocus();
    }

    @Override
    public void undo() {
        this.editor.undo();
    }

    @Override
    public void redo() {
        this.editor.redo();
    }

    private List<CodeEditor.Chip> lineChips(String text, int ln) {
        List<CodeEditor.Chip> out = new ArrayList<>();
        int ls = 0;

        for (int i = 0; i < ln; i++) {
            int n = text.indexOf(10, ls);
            if (n < 0) {
                return out;
            }

            ls = n + 1;
        }

        int le = text.indexOf(10, ls);
        if (le < 0) {
            le = text.length();
        }

        String line = text.substring(ls, le);
        String t = line.trim();
        if (t.startsWith("//")) {
            return out;
        } else {
            int open = -1;

            for (int i = ls; i < le; i++) {
                if (text.charAt(i) == '"') {
                    if (open < 0) {
                        open = i;
                    } else {
                        int q = open;
                        int qcol = open - ls;
                        open = -1;
                        String kind = SstLanguage.valueKindAt(text, q);
                        if (kind != null && KINDS.containsKey(kind)) {
                            String cur = text.substring(q + 1, i);
                            BiConsumer<Integer, Integer> act = (cx, cy) -> {
                                String now = this.editor.text();
                                int[] r = quoteAt(now, ln, qcol);
                                if (r != null) {
                                    String value = now.substring(r[0] + 1, r[1]);
                                    int[] range = new int[]{r[0] + 1, r[1]};
                                    this.pick(kind, value, v -> {
                                        this.editor.replaceText(range[0], range[1], v);
                                        range[1] = range[0] + v.length();
                                    });
                                }
                            };
                            if (kind.equals("color")) {
                                int sw = -8355712;

                                try {
                                    Integer col = Colors.parse(cur);
                                    if (col != null) {
                                        sw = 0xFF000000 | col;
                                    }
                                } catch (RuntimeException var18) {
                                }

                                out.add(new CodeEditor.Chip("", sw, 0, act));
                            } else {
                                out.add(new CodeEditor.Chip(CHIP_WORDS.getOrDefault(kind, "выбрать…"), 0, -7364408, act));
                            }
                        }
                    }
                }
            }

            if (t.startsWith("character ")) {
                out.add(new CodeEditor.Chip("голова…", 0, -8658806, (cx, cy) -> this.headEditor(ln, this.editor.lineText(ln))));
                if (!line.matches(".*\\scolor\\s+\".*")) {
                    out.add(new CodeEditor.Chip("+ цвет имени", 0, -7364408, (cx, cy) -> this.colorEditor(ln, this.editor.lineText(ln), true)));
                }

                if (!line.matches(".*\\shitbox\\s+\".*")) {
                    out.add(new CodeEditor.Chip("+ хитбокс", 0, -7364408, (cx, cy) -> {
                        PopupMenu m = new PopupMenu();

                        for (String w : List.of("solid", "fixed", "ghost", "damage", "mortal", "nopick")) {
                            m.item(w, () -> {
                                String l = this.editor.lineText(ln);
                                this.editor.replaceLine(ln, l.replaceAll("\\s+$", "") + " hitbox \"" + w + "\"");
                            });
                        }

                        m.show(cx, cy);
                    }));
                }
            }

            return out;
        }
    }

    private static int[] quoteAt(String text, int ln, int col) {
        int ls = 0;

        for (int i = 0; i < ln; i++) {
            int n = text.indexOf(10, ls);
            if (n < 0) {
                return null;
            }

            ls = n + 1;
        }

        int le = text.indexOf(10, ls);
        if (le < 0) {
            le = text.length();
        }

        int q = ls + col;
        if (q < le && text.charAt(q) == '"') {
            int close = text.indexOf(34, q + 1);
            return close >= 0 && close <= le ? new int[]{q, close} : null;
        } else {
            return null;
        }
    }

    private int[] stringAtCaret() {
        String text = this.editor.text();
        int caret = this.editor.caret();
        int ls = text.lastIndexOf(10, Math.max(0, caret - 1)) + 1;
        int le = text.indexOf(10, caret);
        if (le < 0) {
            le = text.length();
        }

        int open = -1;

        for (int i = ls; i < le; i++) {
            if (text.charAt(i) == '"') {
                if (open < 0) {
                    open = i;
                } else {
                    if (caret > open && caret <= i) {
                        return new int[]{open, i};
                    }

                    open = -1;
                }
            }
        }

        return null;
    }

    private void valueMenu(PopupMenu pm) {
        int[] str = this.stringAtCaret();
        pm.separator();
        if (str != null) {
            String kind = SstLanguage.valueKindAt(this.editor.text(), str[0]);
            if (kind != null && KINDS.containsKey(kind)) {
                String cur = this.editor.text().substring(str[0] + 1, str[1]);
                int[] range = new int[]{str[0] + 1, str[1]};
                pm.item("Выбрать: " + KINDS.get(kind).toLowerCase() + "…", Icon.EDIT, null, () -> this.pick(kind, cur, v -> {
                    this.editor.replaceText(range[0], range[1], v);
                    range[1] = range[0] + v.length();
                })).color(-11549705);
            }
        }

        pm.item("Вставить значение…", Icon.PLUS, null, () -> {
            PopupMenu m = new PopupMenu();

            for (Entry<String, String> e : KINDS.entrySet()) {
                m.item(e.getValue(), () -> {
                    int at = this.editor.caret();
                    this.editor.insertAtCaret("\"\"");
                    int[] rangex = new int[]{at + 1, at + 1};
                    this.pick(e.getKey(), "", v -> {
                        this.editor.replaceText(rangex[0], rangex[1], v);
                        rangex[1] = rangex[0] + v.length();
                    });
                });
            }

            m.separator();
            m.item("Блок под прицелом (x, y, z)", Icon.PIN, null, () -> {
                HitResult h = Minecraft.getInstance().hitResult;
                if (h instanceof BlockHitResult b && h.getType() == Type.BLOCK) {
                    BlockPos p = b.getBlockPos();
                    this.editor.insertAtCaret("(" + p.getX() + ", " + p.getY() + ", " + p.getZ() + ")");
                }
            });
            m.show(this.editor.menuX, this.editor.menuY);
        });
    }

    private String questOnLine() {
        String[] lines = this.editor.text().split("\n", -1);
        int line = this.editor.caretLine();
        if (line >= 0 && line < lines.length) {
            Matcher m = Pattern.compile("\"([^\"]*)\"").matcher(lines[line]);
            return m.find() ? m.group(1) : "";
        } else {
            return "";
        }
    }

    private String characterTypeOnLine() {
        String text = this.editor.text();
        Map<String, String> types = new HashMap<>();
        Matcher m = Pattern.compile("(?m)^\\s*character\\s+([\\p{L}\\w]+)\\s*=\\s*\"([^\"]+)\"").matcher(text);

        while (m.find()) {
            types.put(m.group(1), m.group(2));
        }

        if (types.isEmpty()) {
            return null;
        } else {
            int line = this.editor.caretLine();
            String[] lines = text.split("\n", -1);
            if (line >= 0 && line < lines.length) {
                String l = lines[line];
                Matcher w = Pattern.compile("[\\p{L}\\w]+").matcher(l.replaceAll("\"[^\"]*\"", " "));

                while (w.find()) {
                    if (types.containsKey(w.group())) {
                        return types.get(w.group());
                    }
                }

                return types.size() == 1 ? types.values().iterator().next() : null;
            } else {
                return null;
            }
        }
    }

    private void pick(String kind, String cur, Consumer<String> set) {
        int mx = this.editor.menuX;
        int my = this.editor.menuY;
        switch (kind) {
            case "color":
                int argb = -22016;

                try {
                    Integer c = Colors.parse(cur);
                    if (c != null) {
                        argb = 0xFF000000 | c;
                    }
                } catch (RuntimeException var12) {
                }

                ColorPicker.open(argb, mx, my, vx -> set.accept(String.format("#%06X", vx & 16777215)));
                break;
            case "item":
                AssetPicker.open(ParamType.ITEM, cur, set);
                break;
            case "entity":
                AssetPicker.open(ParamType.ENTITY, cur, set);
                break;
            case "anim":
                AssetPicker.open(ParamType.ANIM, cur, set, this.characterTypeOnLine());
                break;
            case "sound":
                AssetPicker.open(ParamType.SOUND, cur, set);
                break;
            default:
                List<String> list = (List<String>)(switch (kind) {
                    case "image" -> AssetIndex.imageChoices();
                    case "cutscene" -> (List)AssetIndex.projectCutscenes.get();
                    case "gui" -> new ArrayList(AssetIndex.projectGuis.get().keySet());
                    case "vfx" -> AssetIndex.vfxChoices();
                    case "shop" -> (List)AssetIndex.projectShops.get();
                    case "quest" -> (List)AssetIndex.projectQuests.get();
                    case "task" -> new ArrayList(AssetIndex.questTasks(this.questOnLine()).keySet());
                    case "zone" -> (List)AssetIndex.projectZones.get();
                    case "path" -> (List)AssetIndex.projectPaths.get();
                    case "fight" -> AssetIndex.fightChoices();
                    case "hitbox" -> List.of("normal", "solid", "fixed", "ghost", "damage", "nodamage", "mortal", "immortal", "nopick", "pick");
                    default -> List.of();
                });
                PopupMenu m = new PopupMenu();
                if (list.isEmpty()) {
                    m.item("(в проекте пока нет)", () -> {});
                }

                for (String v : list) {
                    m.item(v, () -> set.accept(v)).checked(() -> v.equals(cur));
                }

                m.show(mx, my);
        }
    }

    private void colorEditor(int ln, String line, boolean declaration) {
        String cur = "";
        Matcher m = declaration ? Pattern.compile("color\\s+\"([^\"]*)\"").matcher(line) : LAST_STRING.matcher(line);
        if (m.find()) {
            cur = m.group(1);
        }

        int argb = -22016;

        try {
            Integer c = Colors.parse(cur);
            if (c != null) {
                argb = 0xFF000000 | c;
            }
        } catch (RuntimeException var8) {
        }

        ColorPicker.open(argb, this.editor.x + this.editor.w / 2 - 90, this.editor.y + 40, v -> {
            String hex = String.format("\"#%06X\"", v & 16777215);
            String nl;
            if (declaration) {
                String base = COLOR_OPT.matcher(line).replaceAll("");
                Matcher nm = Pattern.compile("\\sname\\s+\"[^\"]*\"").matcher(base);
                nl = nm.find() ? base.substring(0, nm.end()) + " color " + hex + base.substring(nm.end()) : base.replaceAll("\\s+$", "") + " color " + hex;
            } else {
                Matcher lm = LAST_STRING.matcher(line);
                nl = lm.find() ? line.substring(0, lm.start()) + hex : line.replaceAll("\\s+$", "") + " " + hex;
            }

            this.editor.replaceLine(ln, nl);
        });
    }

    private void headEditor(int ln, String line) {
        Matcher type = Pattern.compile("=\\s*\"([^\"]+)\"").matcher(line);
        String entity = type.find() ? type.group(1) : "";
        float scale = 1.0F;
        int dx = 0;
        int dy = 0;
        int yaw = 0;
        String bone = "auto";
        Matcher m = Pattern.compile("(headscale|headx|heady|headyaw)\\s+\\(?\\s*(-?[\\d.]+)").matcher(line);

        while (m.find()) {
            float v;
            try {
                v = Float.parseFloat(m.group(2));
            } catch (NumberFormatException var14) {
                continue;
            }

            String e = m.group(1);
            switch (e) {
                case "headscale":
                    scale = v;
                    break;
                case "headx":
                    dx = Math.round(v);
                    break;
                case "heady":
                    dy = Math.round(v);
                    break;
                default:
                    yaw = Math.round(v);
            }
        }

        Matcher bm = Pattern.compile("headbone\\s+\"([^\"]*)\"").matcher(line);
        if (bm.find()) {
            bone = bm.group(1);
        }

        HeadEditor.open(entity, new ChatHeads.Params(scale, dx, dy, yaw, bone), p -> {
            String base = HEAD_OPT.matcher(line).replaceAll("").replaceAll("\\s+$", "");
            StringBuilder sb = new StringBuilder(base);
            if (p.scale() != 1.0F) {
                sb.append(" headscale ").append(p.scale());
            }

            if (p.dx() != 0) {
                sb.append(" headx ").append(p.dx());
            }

            if (p.dy() != 0) {
                sb.append(" heady ").append(p.dy());
            }

            if (p.yaw() != 0) {
                sb.append(" headyaw ").append(p.yaw());
            }

            if (!p.bone().equals("auto")) {
                sb.append(" headbone \"").append(p.bone()).append('"');
            }

            this.editor.replaceLine(ln, sb.toString());
        });
    }

    static {
        KINDS.put("color", "Цвет");
        KINDS.put("item", "Предмет");
        KINDS.put("entity", "Сущность / модель");
        KINDS.put("anim", "Анимация");
        KINDS.put("sound", "Звук");
        KINDS.put("image", "Картинка проекта");
        KINDS.put("cutscene", "Катсцена");
        KINDS.put("gui", "Интерфейс");
        KINDS.put("vfx", "Эффект частиц");
        KINDS.put("shop", "Магазин");
        KINDS.put("quest", "Квест");
        KINDS.put("task", "Задача квеста");
        KINDS.put("zone", "Зона");
        KINDS.put("fight", "Стиль боя");
        KINDS.put("hitbox", "Хитбокс");
    }
}
