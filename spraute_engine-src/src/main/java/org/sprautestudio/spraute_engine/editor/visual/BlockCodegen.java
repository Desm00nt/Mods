package org.sprautestudio.spraute_engine.editor.visual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.Diagnostic;
import org.sprautestudio.spraute_engine.lang.ParamType;

public final class BlockCodegen {
    private final StringBuilder out = new StringBuilder();
    private final List<VisualScript.Block> owners = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private static final Pattern OPTIONAL = Pattern.compile("\\[([^\\[\\]]*)]");
    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\w+)");

    private BlockCodegen() {
    }

    public static BlockCodegen.Result generate(VisualScript script) {
        BlockCodegen g = new BlockCodegen();
        g.run(script);
        BlockCodegen.Result r = new BlockCodegen.Result(g.out.toString(), g.owners, g.problems);
        g.markErrors(r);
        return r;
    }

    public static BlockCodegen.Result generateClip(VisualScript script, Ast.Program story) {
        BlockCodegen g = new BlockCodegen();
        g.clearErrors(script);
        List<VisualScript.Stack> stacks = new ArrayList<>(script.stacks);
        stacks.sort(Comparator.comparingDouble(s -> s.y));

        for (VisualScript.Stack s : stacks) {
            if (!s.blocks.isEmpty()) {
                VisualScript.Block head = s.blocks.get(0);
                if (head.type.equals("start") || head.type.equals("clip_start")) {
                    g.statements(s.blocks.subList(1, s.blocks.size()), 0);
                } else if (!head.type.equals("func") && !head.typeInfo().isEvent()) {
                    g.statements(s.blocks, 0);
                } else {
                    g.fail(head, "В скрипте катсцены нет функций и событий: они живут в сюжете");
                }
            }
        }

        String text = g.out.toString();
        BlockCodegen.Result r = new BlockCodegen.Result(text, g.owners, g.problems, Analyzer.analyzeClip(text, null, story));
        g.markErrors(r);
        return r;
    }

    public static BlockCodegen.Result generateBody(VisualScript script) {
        BlockCodegen g = new BlockCodegen();
        g.clearErrors(script);
        List<VisualScript.Stack> stacks = new ArrayList<>(script.stacks);
        stacks.sort(Comparator.comparingDouble(s -> s.y));

        for (VisualScript.Stack s : stacks) {
            if (!s.blocks.isEmpty()) {
                VisualScript.Block head = s.blocks.get(0);
                if (head.type.equals("start") || head.type.equals("clip_start")) {
                    g.declarations(s.blocks.subList(1, s.blocks.size()));
                }
            }
        }

        for (VisualScript.Stack sx : stacks) {
            if (!sx.blocks.isEmpty() && sx.blocks.get(0).typeInfo().isEvent()) {
                VisualScript.Block head = sx.blocks.get(0);
                g.line(0, g.command(head, head.typeInfo().spec()).replaceFirst("^wait ", "on ") + " {", head);
                if (!head.slots.isEmpty()) {
                    g.statements(head.slots.get(0), 1);
                }

                g.statements(sx.blocks.subList(1, sx.blocks.size()), 1);
                g.line(0, "}", head);
            }
        }

        for (VisualScript.Stack sxx : stacks) {
            if (!sxx.blocks.isEmpty()) {
                VisualScript.Block head = sxx.blocks.get(0);
                if (!head.typeInfo().isEvent() && !head.type.equals("func")) {
                    boolean hat = head.type.equals("start") || head.type.equals("clip_start");
                    g.statements(hat ? sxx.blocks.subList(1, sxx.blocks.size()) : sxx.blocks, 0);
                }
            }
        }

        String text = g.out.toString();
        BlockCodegen.Result r = new BlockCodegen.Result(text, g.owners, g.problems, Analyzer.analyzeBody(text, null));
        g.markErrors(r);
        return r;
    }

    private void line(int indent, String text, VisualScript.Block owner) {
        this.out.append("    ".repeat(indent)).append(text).append('\n');
        this.owners.add(owner);
    }

    private void run(VisualScript script) {
        this.clearErrors(script);
        VisualScript.Stack main = script.mainStack();
        if (main == null) {
            this.problems.add("Нет блока «когда начинается сюжет» — перетащите его из категории «Поток»");
        } else {
            VisualScript.Block start = main.blocks.get(0);
            String id = start.field("id").trim();
            if (!id.matches("\\d+")) {
                this.fail(start, "Id сюжета должен быть числом");
            }

            String auto = start.field("auto").trim();
            this.line(
                0,
                "script "
                    + (id.isEmpty() ? "0" : id)
                    + " "
                    + quote(start.field("title"))
                    + (auto.equals("при входе") ? " autostart" : (auto.equals("один раз") ? " autostart once" : ""))
                    + (
                        auto.equals("общий: с запуска игры") || !start.field("solo").trim().equals("только игроку") && !isYes(start.field("solo"))
                            ? ""
                            : " solo"
                    )
                    + (auto.equals("общий: с запуска игры") ? " general" : ""),
                start
            );
            this.line(0, "", null);
            List<VisualScript.Block> body = main.blocks.subList(1, main.blocks.size());
            List<VisualScript.Stack> funcs = new ArrayList<>();

            for (VisualScript.Stack s : script.stacks) {
                if (!s.blocks.isEmpty() && s.blocks.get(0).type.equals("func")) {
                    funcs.add(s);
                }
            }

            int before = this.owners.size();
            this.declarations(body);

            for (VisualScript.Stack sx : funcs) {
                this.declarations(sx.blocks.subList(1, sx.blocks.size()));
            }

            if (this.owners.size() > before) {
                this.line(0, "", null);
            }

            for (VisualScript.Stack sx : script.stacks) {
                if (!sx.blocks.isEmpty() && sx.blocks.get(0).typeInfo().isEvent()) {
                    VisualScript.Block head = sx.blocks.get(0);
                    this.line(0, this.command(head, head.typeInfo().spec()).replaceFirst("^wait ", "on ") + " {", head);
                    if (!head.slots.isEmpty()) {
                        this.statements(head.slots.get(0), 1);
                    }

                    this.statements(sx.blocks.subList(1, sx.blocks.size()), 1);
                    this.line(0, "}", head);
                }
            }

            this.statements(body, 0);
            Set<String> names = new HashSet<>();

            for (VisualScript.Stack sxx : funcs) {
                VisualScript.Block head = sxx.blocks.get(0);
                String name = this.required(head, "name");
                if (!names.add(name)) {
                    this.fail(head, "Функция «" + name + "» уже есть");
                }

                this.line(0, "", null);
                this.line(0, "func " + name + "(" + head.field("params").trim() + ") {", head);
                this.statements(sxx.blocks.subList(1, sxx.blocks.size()), 1);
                this.line(0, "}", head);
            }
        }
    }

    private void clearErrors(VisualScript script) {
        for (VisualScript.Stack s : script.stacks) {
            this.clearErrors(s.blocks);
        }
    }

    private void clearErrors(List<VisualScript.Block> blocks) {
        for (VisualScript.Block b : blocks) {
            b.error = false;
            b.errorText = null;

            for (List<VisualScript.Block> slot : b.slots) {
                this.clearErrors(slot);
            }

            this.clearErrors(new ArrayList<>(b.inputs.values()));
        }
    }

    private void fail(VisualScript.Block b, String message) {
        b.error = true;
        b.errorText = b.errorText == null ? message : b.errorText + "\n" + message;
        this.problems.add(message);
    }

    private void declarations(List<VisualScript.Block> blocks) {
        for (VisualScript.Block b : blocks) {
            label87: {
                String var4 = b.type;
                StringBuilder sb;
                switch (var4) {
                    case "character":
                        sb = new StringBuilder("character ").append(this.required(b, "name")).append(" = ").append(this.requiredQuoted(b, "type"));
                        if (has(b, "display")) {
                            sb.append(" name ").append(this.requiredQuoted(b, "display"));
                        }

                        if (has(b, "color")) {
                            sb.append(" color ").append(this.requiredQuoted(b, "color"));
                        }

                        if (has(b, "hitbox")) {
                            sb.append(" hitbox ").append(this.requiredQuoted(b, "hitbox"));
                        }

                        if (has(b, "speed")) {
                            sb.append(" speed ").append(paren(this.value(b, "speed")));
                        }

                        if (has(b, "icon")) {
                            sb.append(" icon ").append(this.requiredQuoted(b, "icon"));
                        }

                        if (has(b, "portrait")) {
                            sb.append(" portrait ").append(this.requiredQuoted(b, "portrait"));
                        }
                        break;
                    case "location":
                        sb = new StringBuilder("location ").append(this.required(b, "name")).append(" = ").append(this.required(b, "pos"));
                        if (!b.field("yaw").isBlank()) {
                            sb.append(" yaw ").append(b.field("yaw").trim());
                        }

                        this.line(0, sb.toString(), b);
                        break label87;
                    case "var":
                        this.line(0, "var " + this.required(b, "name") + " = " + this.required(b, "value"), b);
                    default:
                        break label87;
                }

                for (String hk : new String[]{"headscale", "headx", "heady", "headyaw"}) {
                    if (has(b, hk)) {
                        sb.append(" ").append(hk).append(" ").append(paren(this.value(b, hk)));
                    }
                }

                if (has(b, "headbone")) {
                    sb.append(" headbone ").append(this.requiredQuoted(b, "headbone"));
                }

                this.line(0, sb.toString(), b);
            }

            for (List<VisualScript.Block> slot : b.slots) {
                this.declarations(slot);
            }
        }
    }

    private static boolean has(VisualScript.Block b, String field) {
        return b.inputs.containsKey(field) || !b.field(field).isBlank();
    }

    private String value(VisualScript.Block b, String field) {
        VisualScript.Block in = b.inputs.get(field);
        return in != null ? this.expr(in) : b.field(field).trim();
    }

    private static String paren(String code) {
        return !code.matches("-?[\\w.]+") && (!code.startsWith("(") || !code.endsWith(")")) ? "(" + code + ")" : code;
    }

    private String requiredQuoted(VisualScript.Block b, String field) {
        VisualScript.Block in = b.inputs.get(field);
        return in != null ? paren(this.expr(in)) : quote(this.required(b, field));
    }

    String expr(VisualScript.Block r) {
        BlockTypes.Type t = r.typeInfo();
        if (t != null && t.code() != null) {
            return this.fill(r, template(r));
        } else {
            this.fail(r, "Этот блок нельзя вставить в поле");
            return "0";
        }
    }

    private static String template(VisualScript.Block b) {
        BlockTypes.Variant v = BlockTypes.variant(b.type);
        return v != null ? v.template(b.field(v.field()).trim()) : b.typeInfo().code();
    }

    private String fill(VisualScript.Block b, String template) {
        Matcher m = OPTIONAL.matcher(template);
        StringBuilder sb = new StringBuilder();

        while (m.find()) {
            String inner = m.group(1);
            boolean keep = true;
            Matcher p = PLACEHOLDER.matcher(inner);

            while (p.find()) {
                if (!has(b, p.group(1))) {
                    keep = false;
                }
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(keep ? inner : ""));
        }

        m.appendTail(sb);
        String code = sb.toString();
        List<BlockTypes.Field> fields = new ArrayList<>(b.typeInfo().fields());
        fields.sort((a, c) -> c.name().length() - a.name().length());

        for (BlockTypes.Field f : fields) {
            if (code.contains("%" + f.name())) {
                code = code.replace("%" + f.name(), this.argument(b, f));
            }
        }

        return code;
    }

    private String argument(VisualScript.Block r, BlockTypes.Field f) {
        VisualScript.Block in = r.inputs.get(f.name());
        if (in != null) {
            String e = paren(this.expr(in));
            return f.kind() == ParamType.DURATION ? "(" + e + " * 20)" : e;
        } else {
            String v = r.field(f.name()).trim();
            if (f.kind() != null && f.kind() != ParamType.VAR) {
                return switch (f.kind()) {
                    case CHAR, LABEL, TARGET, LOC, KEY, EMOTION, LAYER, MOVETYPE, FUNC, BRANCH, SPEAKER, DURATION, CHOICE, SYSTEM, EASE -> {
                        if (v.isEmpty()) {
                            this.fail(r, "Не заполнено поле «" + (f.label().isEmpty() ? f.name() : f.label()) + "»");
                            yield "_";
                        } else {
                            yield v;
                        }
                    }
                    case BOOL -> !v.equalsIgnoreCase("да") && !v.equalsIgnoreCase("true")
                        ? (!v.isEmpty() && !v.equalsIgnoreCase("нет") ? paren(v) : "false")
                        : "true";
                    case NUMBER, INT, SCRIPT -> v.isEmpty() ? "0" : (v.matches("-?\\d+(\\.\\d+)?") ? v : paren(v));
                    case EXPR -> {
                        if (v.isEmpty()) {
                            this.fail(r, "Пустое поле в блоке «" + r.typeInfo().key() + "»");
                            yield "0";
                        } else {
                            yield paren(v);
                        }
                    }
                    default -> quoteValue(r.field(f.name()));
                };
            } else {
                if (v.isEmpty()) {
                    this.fail(r, "Выберите переменную");
                }

                return v.isEmpty() ? "_" : v;
            }
        }
    }

    private static String quoteValue(String v) {
        return v.trim().matches("-?\\d+(\\.\\d+)?") ? v.trim() : quote(v);
    }

    private String paramCode(VisualScript.Block b, CommandSpec.Param p) {
        VisualScript.Block in = b.inputs.get(p.name);
        if (in != null) {
            String e = this.expr(in);
            return p.type == ParamType.DURATION ? "(" + paren(e) + " * 20)" : paren(e);
        } else {
            String v = b.field(p.name).trim();
            return v.isEmpty() ? "" : format(p.type, p.vararg, v);
        }
    }

    private String required(VisualScript.Block b, String field) {
        VisualScript.Block in = b.inputs.get(field);
        if (in != null) {
            return this.expr(in);
        } else {
            String v = b.field(field).trim();
            if (v.isEmpty()) {
                BlockTypes.Field f = b.typeInfo().field(field);
                this.fail(b, "Не заполнено поле «" + (f != null ? f.label() : field) + "»");
                return "_";
            } else {
                return v;
            }
        }
    }

    private void statements(List<VisualScript.Block> blocks, int indent) {
        for (VisualScript.Block b : blocks) {
            this.statement(b, indent);
        }
    }

    private void statement(VisualScript.Block b, int indent) {
        BlockTypes.Type t = b.typeInfo();
        String var4 = b.type;
        switch (var4) {
            case "start":
            case "clip_start":
                this.fail(b, "Второй блок начала сюжета внутри сюжета");
                break;
            case "func":
                this.fail(b, "Шапку функции нельзя вкладывать — оставьте её отдельной стопкой");
                break;
            case "call":
                this.line(indent, "call " + this.required(b, "name") + "(" + b.field("args").trim() + ")", b);
                break;
            case "asynccall":
                this.line(
                    indent,
                    "async call "
                        + this.required(b, "name")
                        + "("
                        + b.field("args").trim()
                        + ")"
                        + (b.field("as").isBlank() ? "" : " as " + b.field("as").trim()),
                    b
                );
                break;
            case "async":
                this.block(b, indent, b.field("name").isBlank() ? "async" : "async " + b.field("name").trim());
                break;
            case "await":
                this.line(indent, "await " + this.required(b, "name"), b);
                break;
            case "cancel":
                this.line(indent, "cancel " + this.required(b, "name"), b);
                break;
            case "return":
                this.line(indent, "return", b);
            case "character":
            case "location":
            case "var":
                break;
            case "comment":
                this.line(indent, "// " + b.field("text"), b);
                break;
            case "label":
                this.line(indent, "label " + this.required(b, "name"), b);
                break;
            case "goto":
                this.line(indent, "goto " + this.required(b, "label"), b);
                break;
            case "stop":
                this.line(indent, "stop", b);
                break;
            case "code":
                for (String l : this.required(b, "code").split(";|\n")) {
                    if (!l.isBlank()) {
                        this.line(indent, l.trim(), b);
                    }
                }
                break;
            case "set":
                this.line(
                    indent,
                    "set " + this.required(b, "var") + " " + (b.field("op").isBlank() ? "=" : b.field("op").trim()) + " " + this.required(b, "value"),
                    b
                );
                break;
            case "change":
                this.line(indent, "set " + this.required(b, "var") + " += " + this.required(b, "value"), b);
                break;
            case "switch":
                this.line(indent, "switch " + this.required(b, "value") + " {", b);
                if (b.options.isEmpty()) {
                    this.fail(b, "Добавьте хотя бы один вариант (ПКМ по блоку)");
                }

                for (int i = 0; i < b.options.size(); i++) {
                    String o = b.options.get(i).trim();
                    this.line(indent + 1, (o.isEmpty() ? "\"\"" : (!o.startsWith("\"") && !o.matches("-?\\d+(\\.\\d+)?") ? quote(o) : o)) + " {", b);
                    this.statements(b.slots.get(i), indent + 2);
                    this.line(indent + 1, "}", b);
                }

                List<VisualScript.Block> otherwise = b.slots.get(b.slots.size() - 1);
                if (!otherwise.isEmpty()) {
                    this.line(indent + 1, "else {", b);
                    this.statements(otherwise, indent + 2);
                    this.line(indent + 1, "}", b);
                }

                this.line(indent, "}", b);
                break;
            case "if":
            case "ifelse":
                this.line(indent, "if " + this.required(b, "cond") + " {", b);
                this.statements(b.slots.get(0), indent + 1);
                if (b.type.equals("ifelse")) {
                    this.line(indent, "} else {", b);
                    this.statements(b.slots.get(1), indent + 1);
                }

                this.line(indent, "}", b);
                break;
            case "while":
                this.block(b, indent, "while " + this.required(b, "cond"));
                break;
            case "for":
                this.block(b, indent, "for " + this.required(b, "var") + " in " + this.required(b, "list"));
                break;
            case "as":
                this.block(b, indent, "as " + this.required(b, "who"));
                break;
            case "asall":
                String each = "each_player_" + indent;
                this.line(indent, "for " + each + " in " + BlockTypes.eachPlayerCode(b.field("who").trim()) + " {", b);
                this.line(indent + 1, "as " + each + " {", b);
                this.statements(b.slots.get(0), indent + 2);
                this.line(indent + 1, "}", b);
                this.line(indent, "}", b);
                break;
            case "every":
                this.block(b, indent, "every " + this.required(b, "time"));
                break;
            case "break":
                this.line(indent, "break", b);
                break;
            case "continue":
                this.line(indent, "continue", b);
                break;
            case "propset":
                this.line(indent, "set " + this.required(b, "who") + "." + this.required(b, "prop") + " = " + this.required(b, "value"), b);
                break;
            case "method":
                this.line(indent, this.required(b, "who") + "." + this.required(b, "method") + "(" + b.field("args").trim() + ")", b);
                break;
            case "listadd":
                this.line(indent, this.required(b, "var") + ".add(" + this.required(b, "value") + ")", b);
                break;
            case "listremove":
                this.line(indent, this.required(b, "var") + ".remove(" + this.required(b, "value") + ")", b);
                break;
            case "listclear":
                this.line(indent, this.required(b, "var") + ".clear()", b);
                break;
            case "setat":
                this.line(indent, "set " + this.required(b, "var") + "[" + this.required(b, "index") + "] = " + this.required(b, "value"), b);
                break;
            case "repeat":
                this.block(b, indent, "repeat " + this.required(b, "count"));
                break;
            case "parallel":
                this.block(b, indent, "parallel " + this.required(b, "name"));
                break;
            case "choice":
                StringBuilder sb = new StringBuilder("choice ").append(this.required(b, "who")).append(' ').append(this.requiredQuoted(b, "text"));
                if (!b.field("emotion").isBlank()) {
                    sb.append(" emotion ").append(b.field("emotion").trim());
                }

                this.line(indent, sb + " {", b);
                if (b.options.isEmpty()) {
                    this.fail(b, "Добавьте хотя бы один вариант ответа (ПКМ по блоку)");
                }

                for (int i = 0; i < b.options.size(); i++) {
                    this.line(indent + 1, quote(b.options.get(i)) + " {", b);
                    this.statements(b.slots.get(i), indent + 2);
                    this.line(indent + 1, "}", b);
                }

                this.line(indent, "}", b);
                break;
            default:
                if (t.isValue()) {
                    this.fail(b, "Блок-значение нужно вставить в поле другого блока");
                } else if (BlockTypes.variant(b.type) != null) {
                    this.line(indent, this.fill(b, template(b)), b);
                } else if (t.isEvent()) {
                    this.line(indent, this.command(b, t.spec()).replaceFirst("^wait ", "on ") + " {", b);
                    this.statements(b.slots.isEmpty() ? List.of() : b.slots.get(0), indent + 1);
                    this.line(indent, "}", b);
                } else if (t.spec() != null) {
                    this.line(indent, this.command(b, t.spec()), b);
                }
        }
    }

    private static boolean isYes(String value) {
        String v = value.trim().toLowerCase();
        return v.equals("true") || v.equals("да");
    }

    private void block(VisualScript.Block b, int indent, String head) {
        this.line(indent, head + " {", b);
        this.statements(b.slots.get(0), indent + 1);
        this.line(indent, "}", b);
    }

    private String command(VisualScript.Block b, CommandSpec spec) {
        StringBuilder sb = new StringBuilder(spec.key);

        for (CommandSpec.Group g : spec.groups) {
            if (!g.isFlag()) {
                List<String> values = new ArrayList<>();
                boolean any = false;

                for (CommandSpec.Param p : g.params) {
                    String v = this.paramCode(b, p);
                    if (!v.isEmpty()) {
                        any = true;
                    }

                    values.add(v);
                }

                if (any || !g.optional) {
                    sb.append(' ');
                    if (g.keyword != null) {
                        sb.append(g.keyword).append(' ');
                    }

                    for (int i = 0; i < g.params.size(); i++) {
                        CommandSpec.Param p = g.params.get(i);
                        String v = values.get(i);
                        if (v.isEmpty()) {
                            BlockTypes.Field f = b.typeInfo().field(p.name);
                            this.fail(b, "Не заполнено поле «" + (f != null ? f.label() : p.name) + "»");
                            v = "_";
                        }

                        if (i > 0) {
                            sb.append(' ');
                        }

                        sb.append(v);
                    }
                }
            } else if (isYes(b.field(g.keyword))) {
                sb.append(' ').append(g.keyword);
            }
        }

        return sb.toString();
    }

    private static String format(ParamType type, boolean vararg, String v) {
        boolean quoted = type == ParamType.TEXT
            || type == ParamType.SOUND
            || type == ParamType.ITEM
            || type == ParamType.EFFECT
            || type == ParamType.ENTITY
            || type == ParamType.ANIM
            || type == ParamType.GUI
            || type == ParamType.GUIEL
            || type == ParamType.IMAGE
            || type == ParamType.CUTSCENE
            || type == ParamType.VFX
            || type == ParamType.POST
            || type == ParamType.CAMRIG
            || type == ParamType.STRUCTURE
            || type == ParamType.BUILDSTYLE
            || type == ParamType.STEALTHSHAPE
            || type == ParamType.STEALTH
            || type == ParamType.SKILLTREE
            || type == ParamType.SKILLNODE
            || type == ParamType.SKILLMENU
            || type == ParamType.PANELTURN
            || type == ParamType.PANELLOOK
            || type == ParamType.SLOTORDER
            || type == ParamType.SLOTSET
            || type == ParamType.PLAYERCAN
            || type == ParamType.PLAYERSTAT
            || type == ParamType.PLAYERRULE
            || type == ParamType.SHOP
            || type == ParamType.QUEST
            || type == ParamType.TASK
            || type == ParamType.ZONE
            || type == ParamType.FIGHT
            || type == ParamType.PATH;
        if (!quoted) {
            return v;
        } else if (vararg) {
            StringBuilder sb = new StringBuilder();

            for (String part : v.split(",")) {
                if (!part.isBlank()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }

                    sb.append(quote(part.trim()));
                }
            }

            return sb.toString();
        } else {
            return quote(v);
        }
    }

    static String quote(String s) {
        return s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"") ? s : "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void markErrors(BlockCodegen.Result r) {
        for (Diagnostic d : r.analysis.diagnostics) {
            if (d.severity == Diagnostic.Severity.ERROR) {
                VisualScript.Block owner = d.line < this.owners.size() ? this.owners.get(d.line) : null;
                if (owner != null) {
                    owner.error = true;
                    owner.errorText = owner.errorText == null ? d.message : owner.errorText + "\n" + d.message;
                }
            }
        }
    }

    public static final class Result {
        public final String text;
        public final List<VisualScript.Block> lineOwners;
        public final List<String> problems;
        public final Analyzer.Result analysis;

        Result(String text, List<VisualScript.Block> lineOwners, List<String> problems) {
            this(text, lineOwners, problems, Analyzer.analyze(text));
        }

        Result(String text, List<VisualScript.Block> lineOwners, List<String> problems, Analyzer.Result analysis) {
            this.text = text;
            this.lineOwners = lineOwners;
            this.problems = problems;
            this.analysis = analysis;
        }

        public boolean hasErrors() {
            return !this.problems.isEmpty() || this.analysis.hasErrors();
        }

        public int errorCount() {
            return this.problems.size() + this.analysis.errorCount();
        }
    }
}
