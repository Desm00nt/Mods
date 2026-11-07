package org.sprautestudio.spraute_engine.lang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sprautestudio.spraute_engine.build.BuildStyles;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.general.GeneralLang;
import org.sprautestudio.spraute_engine.gui.PanelLook;
import org.sprautestudio.spraute_engine.hitbox.HitboxFlags;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.post.PostParams;
import org.sprautestudio.spraute_engine.runtime.Colors;
import org.sprautestudio.spraute_engine.stealth.StealthShape;

public final class Analyzer {
    private static final Pattern INTERPOLATION = Pattern.compile("(?<!\\\\)\\{([^{}]*)}");
    private static final Pattern RESOURCE_ID = Pattern.compile("([a-z0-9_.-]+:)?[a-z0-9_./-]+");
    private final String src;
    private final int[] lineStarts;
    private final Analyzer.Result result;
    private final Analyzer.Registries registries;
    private boolean clip;
    private Ast.Program story;
    private boolean general;
    private int inFunction;
    private int loopDepth;

    private Analyzer(String src, Parser.Result parse, Analyzer.Registries registries) {
        this.src = src;
        this.lineStarts = Parser.computeLineStarts(src);
        this.result = new Analyzer.Result(parse);
        this.registries = registries == null ? new Analyzer.Registries() : registries;
    }

    private static boolean isPlayerWord(String name) {
        return name.equals("player") || name.equals("actor");
    }

    public static Analyzer.Result analyze(String src) {
        return analyze(src, null);
    }

    public static Analyzer.Result analyze(String src, Analyzer.Registries registries) {
        Parser.Result parse = Parser.parse(src);
        Analyzer a = new Analyzer(src, parse, registries);
        a.result.diagnostics.addAll(parse.diagnostics);
        a.run();
        a.result.diagnostics.sort((x, y) -> Integer.compare(x.start, y.start));
        return a.result;
    }

    public static Analyzer.Result analyzeClip(String src, Analyzer.Registries registries, Ast.Program story) {
        Parser.Result parse = Parser.parse(src);
        Analyzer a = new Analyzer(src, parse, registries);
        a.clip = true;
        a.story = story;

        for (Diagnostic d : parse.diagnostics) {
            if (!d.message.startsWith("Нет заголовка")) {
                a.result.diagnostics.add(d);
            }
        }

        a.run();
        a.result.diagnostics.sort((x, y) -> Integer.compare(x.start, y.start));
        return a.result;
    }

    public static Analyzer.Result analyzeBody(String src, Analyzer.Registries registries) {
        Parser.Result parse = Parser.parse(src);
        Analyzer a = new Analyzer(src, parse, registries);

        for (Diagnostic d : parse.diagnostics) {
            if (!d.message.startsWith("Нет заголовка")) {
                a.result.diagnostics.add(d);
            }
        }

        a.run();
        a.result.diagnostics.sort((x, y) -> Integer.compare(x.start, y.start));
        return a.result;
    }

    private void clipScope(Ast.Program prog) {
        if (prog.header != null) {
            this.error("У скрипта катсцены нет заголовка: он выполняется внутри сюжета", prog.header);
        }

        String decl = "В скрипте катсцены ничего не объявляют: объявите это в сюжете";

        for (Ast.CharacterDecl c : prog.characters) {
            this.diag(Diagnostic.Severity.ERROR, decl, c.nameStart, c.nameEnd);
        }

        for (Ast.LocationDecl l : prog.locations) {
            this.diag(Diagnostic.Severity.ERROR, decl, l.nameStart, l.nameEnd);
        }

        for (Ast.VarDecl v : prog.vars) {
            this.diag(Diagnostic.Severity.ERROR, decl, v.nameStart, v.nameEnd);
        }

        for (Ast.FuncDecl f : prog.funcs) {
            this.diag(Diagnostic.Severity.ERROR, decl, f.nameStart, f.nameEnd);
        }

        if (this.story != null) {
            for (Ast.CharacterDecl c : this.story.characters) {
                this.result.characters.put(c.name, new Analyzer.Symbol(c.name, Analyzer.SymbolKind.CHARACTER, 0, 0, "из сюжета"));
            }

            for (Ast.LocationDecl l : this.story.locations) {
                this.result.locations.put(l.name, new Analyzer.Symbol(l.name, Analyzer.SymbolKind.LOCATION, 0, 0, "из сюжета"));
            }

            for (Ast.VarDecl v : this.story.vars) {
                this.result.vars.put(v.name, new Analyzer.Symbol(v.name, Analyzer.SymbolKind.VARIABLE, 0, 0, "из сюжета"));
            }

            for (Ast.FuncDecl f : this.story.funcs) {
                this.result.functions.put(f.name, new Analyzer.Symbol(f.name, Analyzer.SymbolKind.FUNCTION, 0, 0, "(" + String.join(", ", f.params) + ")"));
            }
        }
    }

    private void undeclared(String msg, int start, int end) {
        if (!this.clip || this.story != null) {
            this.diag(Diagnostic.Severity.ERROR, msg, start, end);
        }
    }

    private void diag(Diagnostic.Severity sev, String msg, Ast.Node at) {
        this.diag(sev, msg, at.start, at.end);
    }

    private void diag(Diagnostic.Severity sev, String msg, int start, int end) {
        int lo = 0;
        int hi = this.lineStarts.length - 1;

        while (lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (this.lineStarts[mid] <= start) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        this.result.diagnostics.add(new Diagnostic(sev, msg, start, end, lo, start - this.lineStarts[lo]));
    }

    private void error(String msg, Ast.Node at) {
        this.diag(Diagnostic.Severity.ERROR, msg, at);
    }

    private void warn(String msg, Ast.Node at) {
        this.diag(Diagnostic.Severity.WARNING, msg, at);
    }

    private void run() {
        Ast.Program prog = this.result.program();
        if (this.clip) {
            this.clipScope(prog);
        } else if (prog.header != null) {
            if (prog.header.id <= 0) {
                this.diag(Diagnostic.Severity.ERROR, "Id сюжета должен быть больше 0", prog.header.idStart, prog.header.idEnd);
            } else if (prog.header.id < 1000) {
                this.diag(
                    Diagnostic.Severity.WARNING,
                    "Id меньше 1000 может совпасть с сюжетами Spraute (1, 2, 3, 4, 7, 888...)",
                    prog.header.idStart,
                    prog.header.idEnd
                );
            }

            this.general = prog.header.general;
            if (this.general && (prog.header.autostart || prog.header.solo)) {
                this.error("Общий скрипт сам работает с запуска игры: autostart и solo ему не нужны", prog.header);
            }

            if (this.general) {
                for (Ast.CharacterDecl c : prog.characters) {
                    this.error("В общем скрипте нет мира: персонажей здесь быть не может", c);
                }

                for (Ast.LocationDecl l : prog.locations) {
                    this.error("В общем скрипте нет мира: точек здесь быть не может", l);
                }
            }
        }

        for (Ast.CharacterDecl c : prog.characters) {
            this.declare(
                this.result.characters,
                c.name,
                Analyzer.SymbolKind.CHARACTER,
                c.nameStart,
                c.nameEnd,
                c.existing != null ? "existing " + this.literal(c.existing) : this.literal(c.type)
            );
        }

        for (Ast.LocationDecl l : prog.locations) {
            this.declare(this.result.locations, l.name, Analyzer.SymbolKind.LOCATION, l.nameStart, l.nameEnd, this.literal(l.pos));
        }

        for (Ast.VarDecl v : prog.vars) {
            this.declare(this.result.vars, v.name, Analyzer.SymbolKind.VARIABLE, v.nameStart, v.nameEnd, this.literal(v.init));
        }

        for (Ast.FuncDecl f : prog.funcs) {
            this.declare(this.result.functions, f.name, Analyzer.SymbolKind.FUNCTION, f.nameStart, f.nameEnd, "(" + String.join(", ", f.params) + ")");

            for (String p : f.params) {
                Analyzer.Symbol existing = this.result.lookup(p);
                if (existing == null) {
                    Analyzer.Symbol s = new Analyzer.Symbol(p, Analyzer.SymbolKind.VARIABLE, f.nameStart, f.nameEnd, "параметр " + f.name);
                    this.result.vars.put(p, s);
                } else if (existing.kind() != Analyzer.SymbolKind.VARIABLE) {
                    this.diag(Diagnostic.Severity.ERROR, "Параметр '" + p + "' совпадает с именем: " + kindName(existing.kind()), f.nameStart, f.nameEnd);
                }
            }
        }

        this.collectLabels(prog.body);

        for (Ast.FuncDecl f : prog.funcs) {
            this.collectLabels(f.body);
        }

        for (Ast.CharacterDecl c : prog.characters) {
            if (c.type != null) {
                this.checkResource(c.type, ParamType.ENTITY);
            }

            if (c.existing != null && !(c.existing instanceof Ast.StringLit)) {
                this.error("Ожидался id персонажа в кавычках", c.existing);
            }

            if (c.displayName != null) {
                this.expr(c.displayName);
            }

            if (c.health != null) {
                this.expr(c.health);
            }

            if (c.hitbox != null) {
                this.expr(c.hitbox);
                if (c.hitbox instanceof Ast.StringLit s) {
                    try {
                        HitboxFlags.parse(0, List.of(s.value));
                    } catch (IllegalArgumentException var9) {
                        this.error(var9.getMessage(), c.hitbox);
                    }
                }
            }

            if (c.color != null) {
                this.expr(c.color);
                if (c.color instanceof Ast.StringLit s) {
                    try {
                        Colors.parse(s.value);
                    } catch (RuntimeException var8) {
                        this.error(var8.getMessage(), c.color);
                    }
                }
            }

            if (c.speed != null) {
                this.expr(c.speed);
            }

            if (c.skin != null && !(c.skin instanceof Ast.StringLit)) {
                this.error("Ожидался путь к текстуре в кавычках", c.skin);
            }

            this.checkPicture(c.icon);
            this.checkPicture(c.portrait);
        }

        for (Ast.LocationDecl l : prog.locations) {
            this.checkCoords(l.pos);
            if (l.yaw != null) {
                this.expr(l.yaw);
            }

            if (l.pitch != null) {
                this.expr(l.pitch);
            }

            if (l.dimension != null) {
                this.checkResource(l.dimension, null);
            }
        }

        for (Ast.VarDecl v : prog.vars) {
            this.expr(v.init);
        }

        this.statements(prog.body);

        for (Ast.FuncDecl f : prog.funcs) {
            this.inFunction++;
            this.statements(f.body);
            this.inFunction--;
        }
    }

    private void branch(String name, int start, int end) {
        if (name != null) {
            Analyzer.Symbol existing = this.result.branches.get(name);
            if (existing == null) {
                Analyzer.Symbol other = this.result.lookup(name);
                if (other != null) {
                    this.diag(Diagnostic.Severity.ERROR, "Имя ветки '" + name + "' уже занято (" + kindName(other.kind()) + ")", start, end);
                } else {
                    Analyzer.Symbol s = new Analyzer.Symbol(name, Analyzer.SymbolKind.PARALLEL, start, end, "");
                    this.result.branches.put(name, s);
                    this.result.outline.add(s);
                }
            }
        }
    }

    private void declare(Map<String, Analyzer.Symbol> map, String name, Analyzer.SymbolKind kind, int start, int end, String detail) {
        if (name != null) {
            Analyzer.Symbol existing = this.result.lookup(name);
            if (existing != null) {
                this.diag(Diagnostic.Severity.ERROR, "Имя '" + name + "' уже занято (" + kindName(existing.kind()) + ")", start, end);
            } else {
                if (Specs.function(name) != null) {
                    this.diag(Diagnostic.Severity.WARNING, "Имя '" + name + "' совпадает со встроенной функцией", start, end);
                }

                Analyzer.Symbol s = new Analyzer.Symbol(name, kind, start, end, detail);
                map.put(name, s);
                this.result.outline.add(s);
            }
        }
    }

    private static String kindName(Analyzer.SymbolKind k) {
        return switch (k) {
            case CHARACTER -> "персонаж";
            case LOCATION -> "точка";
            case VARIABLE -> "переменная";
            case LABEL -> "метка";
            case PARALLEL -> "асинхронная ветка";
            case FUNCTION -> "функция";
        };
    }

    private String literal(Ast.Expr e) {
        return e == null ? "" : this.src.substring(e.start, Math.min(e.end, this.src.length()));
    }

    private void collectLabels(List<Ast.Stmt> body) {
        for (Ast.Stmt s : body) {
            if (s instanceof Ast.LabelStmt l) {
                this.declare(this.result.labels, l.name, Analyzer.SymbolKind.LABEL, l.start, l.end, "");
            } else if (s instanceof Ast.IfStmt i) {
                this.collectLabels(i.then);
                if (i.otherwise != null) {
                    this.collectLabels(i.otherwise);
                }
            } else if (s instanceof Ast.WhileStmt w) {
                this.collectLabels(w.body);
            } else if (s instanceof Ast.RepeatStmt r) {
                this.collectLabels(r.body);
            } else if (s instanceof Ast.ForStmt f) {
                if (!this.result.vars.containsKey(f.var)) {
                    this.declare(this.result.vars, f.var, Analyzer.SymbolKind.VARIABLE, f.varStart, f.varEnd, "переменная цикла");
                }

                this.collectLabels(f.body);
            } else if (s instanceof Ast.EveryStmt e) {
                this.collectLabels(e.body);
            } else if (s instanceof Ast.ChoiceStmt c) {
                for (Ast.ChoiceOption o : c.options) {
                    this.collectLabels(o.body);
                }
            } else if (s instanceof Ast.AsStmt as) {
                this.collectLabels(as.body);
            } else if (s instanceof Ast.ParallelStmt ps) {
                this.branch(ps.name, ps.start, ps.end);
                this.collectLabels(ps.body);
            } else if (s instanceof Ast.CallStmt c && c.async) {
                this.branch(c.as, c.asStart, c.asEnd);
            } else if (s instanceof Ast.OnStmt on) {
                this.collectLabels(on.body);
            } else if (s instanceof Ast.SwitchStmt sw) {
                for (Ast.SwitchCase c : sw.cases) {
                    this.collectLabels(c.body);
                }

                if (sw.otherwise != null) {
                    this.collectLabels(sw.otherwise);
                }
            }
        }
    }

    private void statements(List<Ast.Stmt> body) {
        boolean afterJump = false;

        for (Ast.Stmt s : body) {
            if (afterJump && !(s instanceof Ast.LabelStmt)) {
                this.warn("Этот код никогда не выполнится (после goto/stop)", s);
                afterJump = false;
            }

            this.statement(s);
            if (s instanceof Ast.GotoStmt || s instanceof Ast.StopStmt || s instanceof Ast.ReturnStmt) {
                afterJump = true;
            }

            if (s instanceof Ast.LabelStmt) {
                afterJump = false;
            }
        }
    }

    private boolean isLabel(String name) {
        return this.result.labels.containsKey(name) || name.equals("начало") || name.equals("конец");
    }

    private void statement(Ast.Stmt s) {
        if (s instanceof Ast.CommandStmt c) {
            this.command(c);
        } else if (s instanceof Ast.GotoStmt g) {
            if (!this.isLabel(g.name)) {
                this.diag(Diagnostic.Severity.ERROR, "Метка '" + g.name + "' не найдена", g.nameStart, g.nameEnd);
            }
        } else if (s instanceof Ast.SetStmt set) {
            boolean entityProp = set.prop != null && (isPlayerWord(set.name) || this.result.characters.containsKey(set.name));
            if (!this.result.vars.containsKey(set.name) && !entityProp) {
                this.undeclared("Переменная '" + set.name + "' не объявлена (добавьте var " + set.name + " = 0)", set.nameStart, set.nameEnd);
            }

            if (set.index != null) {
                this.expr(set.index);
            }

            this.expr(set.value);
        } else if (s instanceof Ast.ForStmt f) {
            this.expr(f.iter);
            this.loopDepth++;
            this.statements(f.body);
            this.loopDepth--;
        } else if (s instanceof Ast.EveryStmt e) {
            this.expr(e.interval);
            int saved = this.loopDepth;
            this.loopDepth = 0;
            this.statements(e.body);
            this.loopDepth = saved;
        } else if (!(s instanceof Ast.BreakStmt) && !(s instanceof Ast.ContinueStmt)) {
            if (s instanceof Ast.MethodStmt ms) {
                if (!this.result.vars.containsKey(ms.target) && !isPlayerWord(ms.target) && !this.result.characters.containsKey(ms.target)) {
                    this.undeclared("Переменная '" + ms.target + "' не объявлена (добавьте var " + ms.target + " = [])", ms.targetStart, ms.targetEnd);
                }

                for (Ast.Expr a : ms.args) {
                    this.expr(a);
                }
            } else if (s instanceof Ast.IfStmt i) {
                this.expr(i.cond);
                this.statements(i.then);
                if (i.otherwise != null) {
                    this.statements(i.otherwise);
                }
            } else if (s instanceof Ast.WhileStmt w) {
                this.expr(w.cond);
                this.loopDepth++;
                this.statements(w.body);
                this.loopDepth--;
            } else if (s instanceof Ast.RepeatStmt r) {
                this.expr(r.count);
                this.loopDepth++;
                this.statements(r.body);
                this.loopDepth--;
            } else if (s instanceof Ast.ChoiceStmt c) {
                if (this.general) {
                    this.error("В общем скрипте нет диалогов: для выбора сделайте кнопки интерфейса", s);
                }

                if (!this.result.characters.containsKey(c.character)) {
                    this.undeclared("Персонаж '" + c.character + "' не объявлен", c.characterStart, c.characterEnd);
                }

                this.expr(c.text);
                if (c.emotion != null) {
                    this.checkEmotion(c.emotion);
                }

                for (Ast.ChoiceOption o : c.options) {
                    this.expr(o.text);
                    this.statements(o.body);
                }
            } else if (s instanceof Ast.AsStmt as) {
                if (this.general) {
                    this.error("В общем скрипте нет игроков", s);
                }

                this.expr(as.who);
                this.statements(as.body);
            } else if (s instanceof Ast.ParallelStmt ps) {
                this.statements(ps.body);
            } else if (s instanceof Ast.CallStmt c) {
                Ast.FuncDecl f = this.result.program().func(c.name);
                if (f == null && this.story != null) {
                    f = this.story.func(c.name);
                }

                if (f == null) {
                    this.undeclared("Функция '" + c.name + "' не объявлена (func " + c.name + " { ... })", c.nameStart, c.nameEnd);
                } else if (f.params.size() != c.args.size()) {
                    this.diag(
                        Diagnostic.Severity.ERROR,
                        "Функция " + c.name + "(" + String.join(", ", f.params) + ") принимает аргументов: " + f.params.size() + ", передано: " + c.args.size(),
                        c.nameStart,
                        c.nameEnd
                    );
                }

                for (Ast.Expr a : c.args) {
                    this.expr(a);
                }
            } else if (s instanceof Ast.OnStmt on) {
                if (this.clip) {
                    this.error("В скрипте катсцены нет on: он длится миг, обработчики пишите в сюжете", on);
                }

                this.command(on.event);
                int saved = this.loopDepth;
                this.loopDepth = 0;
                this.statements(on.body);
                this.loopDepth = saved;
            } else if (s instanceof Ast.SwitchStmt sw) {
                this.expr(sw.value);

                for (Ast.SwitchCase c : sw.cases) {
                    for (Ast.Expr v : c.values) {
                        this.expr(v);
                    }

                    this.statements(c.body);
                }

                if (sw.otherwise != null) {
                    this.statements(sw.otherwise);
                }
            } else if (s instanceof Ast.ReturnStmt) {
                if (this.inFunction == 0) {
                    this.error("return можно использовать только внутри func", s);
                }
            } else if (s instanceof Ast.AwaitStmt a) {
                for (int ix = 0; ix < a.names.size(); ix++) {
                    if (!this.result.branches.containsKey(a.names.get(ix))) {
                        this.diag(
                            Diagnostic.Severity.ERROR,
                            "Асинхронная ветка '"
                                + a.names.get(ix)
                                + "' не найдена (async "
                                + a.names.get(ix)
                                + " { } или async call ... as "
                                + a.names.get(ix)
                                + ")",
                            a.spans.get(ix)[0],
                            a.spans.get(ix)[1]
                        );
                    }
                }
            } else if (s instanceof Ast.CancelStmt c && !this.result.branches.containsKey(c.name)) {
                this.diag(Diagnostic.Severity.ERROR, "Асинхронная ветка '" + c.name + "' не найдена", c.nameStart, c.nameEnd);
            }
        } else if (this.loopDepth == 0) {
            this.error((s instanceof Ast.BreakStmt ? "break" : "continue") + " можно писать только внутри for, while или repeat", s);
        }
    }

    private void command(Ast.CommandStmt c) {
        CommandSpec spec = Specs.command(c.key);
        if (spec != null) {
            if (Specs.fullOnly(spec)) {
                this.diag(Diagnostic.Severity.ERROR, "Есть только в полной версии Spraute Engine: " + c.key, c.nameStart, c.nameEnd);
            }

            if (this.general && !GeneralLang.COMMANDS.contains(c.key)) {
                this.diag(Diagnostic.Severity.ERROR, "В общем скрипте нет мира и игрока: команда «" + c.key + "» недоступна", c.nameStart, c.nameEnd);
            } else if (!this.general && GeneralLang.ONLY_GENERAL.contains(c.key)) {
                this.diag(Diagnostic.Severity.ERROR, "Команда «" + c.key + "» работает только в общем скрипте (script ... general)", c.nameStart, c.nameEnd);
            }

            if (c.key.equals("spawn") && c.arg("count") != null) {
                if (!(c.arg("count") instanceof Ast.NumberLit n && n.value == Math.floor(n.value))) {
                    this.error("count у spawn — обычное целое число: сколько копий нужно, известно ещё до начала сюжета", c.arg("count"));
                } else if (n.value < 1.0 || n.value > 100.0) {
                    this.error("count у spawn — от 1 до 100", c.arg("count"));
                }
            }

            for (Ast.Arg a : c.args) {
                CommandSpec.Param p = spec.param(a.param);
                if (p != null
                    && (
                        !c.key.startsWith("time ")
                            || !(
                                a.value instanceof Ast.Ident id
                                    && Set.of("sunrise", "dawn", "day", "morning", "noon", "sunset", "evening", "night", "midnight").contains(id.name)
                            )
                    )) {
                    this.checkArg(p, a.value);
                }
            }

            if (c.key.equals("screen")) {
                for (Ast.Arg ax : c.args) {
                    if (ax.value instanceof Ast.NumberLit n && (n.value < 0.0 || n.value > 255.0)) {
                        this.warn("Значение канала цвета должно быть от 0 до 255", ax.value);
                    }
                }
            }
        }
    }

    private void checkArg(CommandSpec.Param p, Ast.Expr e) {
        switch (p.type) {
            case CHAR:
                this.checkCharacter(e);
                break;
            case LOC:
                this.checkLocation(e);
                break;
            case TARGET:
                if (e instanceof Ast.Ident idx) {
                    if (!isPlayerWord(idx.name)
                        && !this.result.characters.containsKey(idx.name)
                        && !this.result.locations.containsKey(idx.name)
                        && !this.result.vars.containsKey(idx.name)) {
                        this.undeclared("'" + idx.name + "' — не персонаж, не точка, не переменная и не player", e.start, e.end);
                    }
                } else if (e instanceof Ast.StringLit sxxxx) {
                    if (!sxxxx.value.equals("mob") && !sxxxx.value.equals("any")) {
                        this.checkResource(e, ParamType.ENTITY);
                    }
                } else {
                    this.checkCoords(e);
                }
                break;
            case SPEAKER:
                if (e instanceof Ast.Ident id) {
                    if (!isPlayerWord(id.name) && !this.result.characters.containsKey(id.name) && !this.result.vars.containsKey(id.name)) {
                        this.undeclared("Говорящий '" + id.name + "' не найден. Укажите слот (0/1), персонажа, player или имя в кавычках", e.start, e.end);
                    }
                } else if (e instanceof Ast.NumberLit n) {
                    if (n.value != 0.0 && n.value != 1.0) {
                        this.warn("Обычно слотов речи два: 0 и 1", e);
                    }
                } else {
                    this.expr(e);
                }
                break;
            case DURATION:
                if (e instanceof Ast.NumberLit) {
                    this.warn("Число без единицы считается тиками (20 тиков = 1 секунда). Добавьте t или s", e);
                } else if (!(e instanceof Ast.DurationLit)) {
                    this.expr(e);
                }
                break;
            case INT:
            case NUMBER:
            case SCRIPT:
                if (e instanceof Ast.StringLit) {
                    this.error("Ожидалось " + p.type.title + ", а не текст", e);
                } else {
                    this.expr(e);
                }

                if (p.type == ParamType.SCRIPT && e instanceof Ast.NumberLit n && n.value != Math.floor(n.value)) {
                    this.error("Id сюжета должен быть целым", e);
                }
                break;
            case TEXT:
                this.expr(e);
                break;
            case EXPR:
            case BOOL:
                this.expr(e);
                break;
            case LABEL:
                if (e instanceof Ast.Ident id && !this.isLabel(id.name)) {
                    this.error("Метка '" + id.name + "' не найдена", e);
                }
                break;
            case SOUND:
            case ITEM:
            case EFFECT:
            case ENTITY:
                boolean ingredient = p.type == ParamType.ITEM
                    && p.name.matches("[abc][123]|input")
                    && e instanceof Ast.StringLit lit
                    && (lit.value.startsWith("#") || lit.value.contains("|"));
                if (!ingredient) {
                    this.checkResource(e, p.type);
                }
                break;
            case EMOTION:
                this.checkEmotion(e);
                break;
            case KEY:
                String k = e instanceof Ast.Ident idxxxxxxxx ? idxxxxxxxx.name : (e instanceof Ast.StringLit sxxxxxxxxxxxx ? sxxxxxxxxxxxx.value : null);
                if (k == null || Specs.glfwKey(k) < 0) {
                    this.error("Неизвестная клавиша. Примеры: F8, G, SPACE", e);
                }
                break;
            case ANIM:
                if (e instanceof Ast.StringLit sxxx) {
                    if (sxxx.value.isBlank()) {
                        this.error("Пустой id анимации", e);
                    } else if (this.registries.animations != null && !this.registries.animations.test(sxxx.value)) {
                        this.warn("Анимация '" + sxxx.value + "' не найдена. Откройте «Ассеты → Анимации», чтобы выбрать", e);
                    }
                } else {
                    this.error("Ожидался id анимации в кавычках, например \"entity/humanoid!attack\"", e);
                }
                break;
            case GUI:
                if (e instanceof Ast.StringLit sxx) {
                    if (this.registries.guis != null && !this.registries.guis.test(sxx.value)) {
                        this.warn("Интерфейса '" + sxx.value + "' нет в проекте (папка «Интерфейсы»)", e);
                    }
                } else {
                    this.error("Ожидалось имя интерфейса в кавычках: gui open \"меню\"", e);
                }
                break;
            case GUIEL:
                if (e instanceof Ast.StringLit sx) {
                    if (this.registries.guiElements != null && !this.registries.guiElements.test(sx.value.substring(sx.value.indexOf(47) + 1))) {
                        this.warn("Элемента '" + sx.value + "' нет ни в одном интерфейсе проекта", e);
                    }
                } else {
                    this.error("Ожидался id элемента в кавычках, например \"yes\"", e);
                }
                break;
            case LAYER:
                String l = e instanceof Ast.Ident idxxxxxxx
                    ? idxxxxxxx.name
                    : (e instanceof Ast.NumberLit nx ? String.valueOf((int)nx.value) : (e instanceof Ast.StringLit sxxxxxxxxxxx ? sxxxxxxxxxxx.value : null));
                if (l == null || Specs.layerName(l) == null) {
                    this.error("Неизвестный слой. Варианты: action, ambient, emotion, eyes, 0–20", e);
                }
                break;
            case EASE:
                String mxxxx = e instanceof Ast.Ident idxxxxxx ? idxxxxxx.name : (e instanceof Ast.StringLit sxxxxxxxxxx ? sxxxxxxxxxx.value : null);
                if (mxxxx == null || !Ease.known(mxxxx)) {
                    this.error("Неизвестная плавность. Например: smooth, linear, ease_in_out_sine, ease_out_back", e);
                }
                break;
            case CUTSCENE:
                if (!(e instanceof Ast.StringLit) && !(e instanceof Ast.Ident)) {
                    this.error("Имя катсцены в кавычках", e);
                }
                break;
            case SHOP:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя магазина в кавычках — из папки «Магазины» проекта", e);
                }
                break;
            case FIGHT:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Стиль боя в кавычках: \"fists\" или свой из папки «Стили боя»", e);
                }
                break;
            case ZONE:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя зоны в кавычках — из папки «Зоны» проекта", e);
                }
                break;
            case PATH:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя пути в кавычках — из папки «Пути» проекта", e);
                }
                break;
            case QUEST:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя квеста в кавычках — из папки «Квесты» проекта", e);
                }
                break;
            case VFX:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя эффекта в кавычках: \"nuke\" или свой из папки «Эффекты»", e);
                }
                break;
            case POST:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя пост-эффекта в кавычках: \"noir\" или свой из папки «Пост-эффекты»", e);
                }
                break;
            case STRUCTURE:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя постройки в кавычках: своя из папки «Постройки» или id структуры", e);
                }
                break;
            case BUILDSTYLE:
                if (!(e instanceof Ast.StringLit sxxxxxxxxxx && BuildStyles.get(sxxxxxxxxxx.value) != null)) {
                    this.error(
                        "Нет такой анимации: up, down, left, right, front, back, rain, rise, center, edges, spiral, tornado, scatter, assemble, wave, pop, instant",
                        e
                    );
                }
                break;
            case SKILLTREE:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Система прокачки в кавычках: из папки «Прокачка»", e);
                }
                break;
            case SKILLMENU:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Меню прокачки в кавычках", e);
                }
                break;
            case SKILLNODE:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Навык в кавычках", e);
                }
                break;
            case STEALTH:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Стиль стелса в кавычках: \"guard\" или свой из папки «Стили стелса»", e);
                }
                break;
            case STEALTHSHAPE:
                if (!(e instanceof Ast.StringLit sxxxxxxxxxx && StealthShape.NAMES.contains(sxxxxxxxxxx.value))) {
                    this.error("Нет такой формы: fan, circle, rect, triangle", e);
                }
                break;
            case PANELTURN:
                if (!(e instanceof Ast.StringLit sxxxxxxxxxx && PanelLook.TURNS.contains(sxxxxxxxxxx.value))) {
                    this.error("Нет такого поворота: " + String.join(", ", PanelLook.TURNS), e);
                }
                break;
            case PANELLOOK:
                if (!(e instanceof Ast.StringLit sxxxxxxxxxx && PanelLook.isLook(sxxxxxxxxxx.value))) {
                    this.error("Нет такого вида: " + String.join(", ", PanelLook.looks()), e);
                }
                break;
            case SLOTORDER:
                if (!(e instanceof Ast.StringLit sxxxxxxxxxx && SlotPlan.ORDERS.contains(sxxxxxxxxxx.value))) {
                    this.error("Нет такого способа: " + String.join(", ", SlotPlan.ORDERS), e);
                }
                break;
            case SLOTSET:
                if (e instanceof Ast.StringLit s) {
                    String problem = SlotPlan.problem(s.value);
                    if (problem != null) {
                        this.error(problem, e);
                    }
                }
                break;
            case PLAYERCAN:
            case PLAYERSTAT:
            case PLAYERRULE:
                if (e instanceof Ast.StringLit sxxxxxxxxx) {
                    PlayerRuleDefs.Rule rule = PlayerRuleDefs.get(sxxxxxxxxx.value);
                    if (rule == null) {
                        this.error("Нет такого правила игрока: " + sxxxxxxxxx.value, e);
                    } else if (p.type == ParamType.PLAYERCAN && !rule.isSwitch()) {
                        this.error(sxxxxxxxxx.value + " — число, его задают через rule set", e);
                    } else if (p.type == ParamType.PLAYERSTAT && rule.isSwitch()) {
                        this.error(sxxxxxxxxx.value + " — разрешение, его меняют через rule allow и rule deny", e);
                    }
                }
                break;
            case CAMRIG:
                if (!(e instanceof Ast.StringLit)) {
                    this.error("Имя камеры в кавычках: \"side_x\" или своя из папки «Камеры»", e);
                }
                break;
            case CAMPARAM:
                String mxxx = e instanceof Ast.Ident idxxxxx ? idxxxxx.name : (e instanceof Ast.StringLit sxxxxxxxx ? sxxxxxxxx.value : null);
                if (mxxx == null || CamProfile.param(mxxx) == null) {
                    this.error("Нет такой настройки камеры: distance, yaw, pitch, lock_x, lock_y, lock_z, fov…", e);
                }
                break;
            case POSTPARAM:
                String mxx = e instanceof Ast.Ident idxxxx ? idxxxx.name : (e instanceof Ast.StringLit sxxxxxxx ? sxxxxxxx.value : null);
                if (mxx == null || PostParams.get(mxx) == null) {
                    this.error("Нет такой настройки пост-эффекта: bloom, vignette, fog, contrast, saturation, dof…", e);
                }
                break;
            case SYSTEM:
                String mx = e instanceof Ast.Ident idxxx ? idxxx.name : (e instanceof Ast.StringLit sxxxxxx ? sxxxxxx.value : null);
                if (mx == null || !Specs.ENGINE_SYSTEMS.contains(mx.toLowerCase())) {
                    this.error("Система движка: health, coins, time, inventory, slots или chat", e);
                }
                break;
            case MOVETYPE:
                String m = e instanceof Ast.Ident idxx ? idxx.name : (e instanceof Ast.StringLit sxxxxx ? sxxxxx.value : null);
                if (m == null || !Specs.MOVE_TYPES.contains(m.toLowerCase())) {
                    this.error("Тип движения: idle, walk, fastwalk или run", e);
                }
        }
    }

    private void checkCharacter(Ast.Expr e) {
        if (e instanceof Ast.Ident id) {
            if (!this.result.characters.containsKey(id.name)) {
                this.undeclared("Персонаж '" + id.name + "' не объявлен", e.start, e.end);
            }
        } else {
            this.error("Ожидался персонаж (имя из character ...)", e);
        }
    }

    private void checkLocation(Ast.Expr e) {
        if (e instanceof Ast.Ident id) {
            if (this.result.characters.containsKey(id.name)) {
                this.error("'" + id.name + "' — персонаж, а здесь нужна точка", e);
            } else if (!this.result.locations.containsKey(id.name) && !this.result.vars.containsKey(id.name)) {
                this.undeclared("Точка '" + id.name + "' не объявлена", e.start, e.end);
            }
        } else {
            this.checkCoords(e);
        }
    }

    private void checkCoords(Ast.Expr e) {
        if (e instanceof Ast.Tuple t) {
            if (t.items.size() != 3 && t.items.size() != 4) {
                this.error("Координаты должны быть из трёх чисел: (x, y, z), и ещё может быть поворот: (x, y, z, поворот)", e);
            }

            for (Ast.Expr item : t.items) {
                this.expr(item);
            }
        } else if (!(e instanceof Ast.Ident id && (this.result.locations.containsKey(id.name) || this.result.vars.containsKey(id.name)))) {
            if (!(e instanceof Ast.Member) && !(e instanceof Ast.Call) && !(e instanceof Ast.Index)) {
                this.error("Ожидались координаты (x, y, z) или имя точки", e);
            } else {
                this.expr(e);
            }
        }
    }

    private void checkPicture(Ast.Expr e) {
        if (e != null) {
            if (e instanceof Ast.StringLit s) {
                if (!Specs.SPEECH_ICONS.contains(s.value) && !s.value.equals("head")) {
                    if (!s.value.contains(":") && !s.value.equals("reset") && !s.value.equals("default") && !s.value.contains("{")) {
                        if (this.registries.images != null && !this.registries.images.test(s.value)) {
                            this.warn("Картинки '" + s.value + "' нет в проекте. Добавьте её в «Картинки» (можно перетащить файл в окно)", e);
                        }
                    }
                }
            } else {
                this.error("Ожидалось имя картинки в кавычках — из папки «Картинки» проекта", e);
            }
        }
    }

    private void checkEmotion(Ast.Expr e) {
        String name = e instanceof Ast.Ident id ? id.name : (e instanceof Ast.StringLit s ? s.value : null);
        if (name == null || !Specs.EMOTIONS.contains(name.toUpperCase())) {
            this.error("Неизвестная эмоция. Например: HAPPY_NEW, SAD1, ANGRY", e);
        }
    }

    private void checkResource(Ast.Expr e, ParamType type) {
        if (e instanceof Ast.StringLit s) {
            if (type == ParamType.SOUND && !s.value.contains(":")) {
                if (this.registries.projectSounds != null && this.registries.projectSounds.test(s.value)) {
                    return;
                }

                if (!RESOURCE_ID.matcher(s.value).matches()) {
                    if (this.registries.projectSounds != null) {
                        this.warn("Звука «" + s.value + "» нет в проекте. Добавьте файл .ogg или .wav в «Звуки» (можно перетащить в окно)", e);
                    }

                    return;
                }
            }

            if (type == ParamType.ENTITY && s.value.startsWith("spraute:")) {
                if (this.registries.models != null
                    && !s.value.equals("spraute:npc")
                    && !s.value.equals("spraute:npc_slim")
                    && !this.registries.models.test(s.value.substring(8))) {
                    this.warn("Сущности «" + s.value.substring(8) + "» нет в проекте (папка «Сущности»)", e);
                }
            } else if (type == ParamType.ITEM && !ItemText.nbt(s.value).isEmpty()) {
                String bad = ItemText.error(s.value);
                if (bad != null) {
                    this.error("Ошибка в NBT предмета: " + bad, e);
                }

                String item = ItemText.id(s.value);
                if (this.registries.items != null && !this.registries.items.test(item)) {
                    this.warn("'" + item + "' не найден в игре (" + type.title + ")", e);
                }
            } else if (!RESOURCE_ID.matcher(s.value).matches()) {
                this.error("Некорректный id '" + s.value + "' (только латиница в нижнем регистре, цифры, _ . / -)", e);
            } else if (type != null) {
                Predicate<String> known = switch (type) {
                    case SOUND -> this.registries.sounds;
                    case ITEM -> this.registries.items;
                    case EFFECT -> this.registries.effects;
                    case ENTITY -> this.registries.entities;
                    default -> null;
                };
                String full = s.value.contains(":") ? s.value : "minecraft:" + s.value;
                if (known != null && !known.test(full)) {
                    this.warn("'" + full + "' не найден в игре (" + type.title + ")", e);
                }
            }
        } else {
            this.error("Ожидался id в кавычках, например \"minecraft:stone\"", e);
        }
    }

    private void expr(Ast.Expr e) {
        if (e != null) {
            if (e instanceof Ast.Ident id) {
                String n = id.name;
                if (isPlayerWord(n)) {
                    if (this.general) {
                        this.error("В общем скрипте нет игрока", e);
                    }

                    return;
                }

                if (this.result.vars.containsKey(n) || this.result.characters.containsKey(n) || this.result.locations.containsKey(n)) {
                    return;
                }

                if (Specs.function(n) != null) {
                    this.error("'" + n + "' — функция, вызывайте так: " + Specs.function(n).signature(), e);
                    return;
                }

                this.undeclared("Неизвестное имя '" + n + "'", e.start, e.end);
            } else if (e instanceof Ast.StringLit s) {
                this.checkInterpolation(s);
            } else if (e instanceof Ast.Binary b) {
                this.expr(b.left);
                this.expr(b.right);
            } else if (e instanceof Ast.Unary u) {
                this.expr(u.operand);
            } else if (e instanceof Ast.Tuple t) {
                for (Ast.Expr x : t.items) {
                    this.expr(x);
                }
            } else if (e instanceof Ast.ListLit l) {
                for (Ast.Expr x : l.items) {
                    this.expr(x);
                }
            } else if (e instanceof Ast.MapLit m) {
                for (Ast.Expr x : m.keys) {
                    this.expr(x);
                }

                for (Ast.Expr x : m.values) {
                    this.expr(x);
                }
            } else if (e instanceof Ast.Index ix) {
                this.expr(ix.target);
                this.expr(ix.index);
            } else if (e instanceof Ast.Member m) {
                this.expr(m.target);
                if (m.args != null) {
                    for (Ast.Expr x : m.args) {
                        this.expr(x);
                    }
                }
            } else if (e instanceof Ast.Call c) {
                Specs.FunctionSpec fs = Specs.function(c.name);
                if (fs != null && this.general && !GeneralLang.FUNCTIONS.contains(c.name)) {
                    this.diag(Diagnostic.Severity.ERROR, "В общем скрипте нет мира и игрока: функция " + c.name + "() недоступна", c.nameStart, c.nameEnd);
                } else if (fs != null && !this.general && GeneralLang.ONLY_GENERAL_FUNCTIONS.contains(c.name)) {
                    this.diag(Diagnostic.Severity.ERROR, "Функция " + c.name + "() работает только в общем скрипте", c.nameStart, c.nameEnd);
                } else if (fs == null) {
                    this.diag(Diagnostic.Severity.ERROR, "Неизвестная функция '" + c.name + "'", c.nameStart, c.nameEnd);
                } else {
                    int expected = arity(fs.signature());
                    int optional = optionalArgs(fs.signature());
                    if (c.args.size() > expected && !fs.signature().contains("...") || c.args.size() < expected - optional) {
                        this.diag(Diagnostic.Severity.ERROR, "Функция " + fs.signature() + " принимает аргументов: " + expected, c.nameStart, c.nameEnd);
                    }
                }

                for (Ast.Expr x : c.args) {
                    this.expr(x);
                }
            }
        }
    }

    private static int optionalArgs(String signature) {
        int n = 0;

        for (char ch : signature.toCharArray()) {
            if (ch == '[') {
                n++;
            }
        }

        return signature.contains("...") ? 99 : n;
    }

    private static int arity(String signature) {
        String inner = signature.substring(signature.indexOf(40) + 1, signature.lastIndexOf(41)).trim();
        return inner.isEmpty() ? 0 : inner.split(",").length;
    }

    private void checkInterpolation(Ast.StringLit s) {
        Matcher m = INTERPOLATION.matcher(s.value);

        while (m.find()) {
            String name = m.group(1).trim();
            if (!name.isEmpty() && !name.startsWith("skill:")) {
                String base = name.split("[(.\\[ ]", 2)[0];
                if (!this.result.vars.containsKey(base) && Specs.function(base) == null && !isPlayerWord(base)) {
                    this.warn("В строке подставляется неизвестное имя '{" + name + "}'", s);
                }
            }
        }
    }

    public static final class Registries {
        public Predicate<String> sounds;
        public Predicate<String> items;
        public Predicate<String> effects;
        public Predicate<String> entities;
        public Predicate<String> images;
        public Predicate<String> projectSounds;
        public Predicate<String> guis;
        public Predicate<String> guiElements;
        public Predicate<String> animations;
        public Predicate<String> models;
    }

    public static final class Result {
        public final Parser.Result parse;
        public final List<Diagnostic> diagnostics = new ArrayList<>();
        public final Map<String, Analyzer.Symbol> characters = new LinkedHashMap<>();
        public final Map<String, Analyzer.Symbol> locations = new LinkedHashMap<>();
        public final Map<String, Analyzer.Symbol> vars = new LinkedHashMap<>();
        public final Map<String, Analyzer.Symbol> labels = new LinkedHashMap<>();
        public final Map<String, Analyzer.Symbol> functions = new LinkedHashMap<>();
        public final Map<String, Analyzer.Symbol> branches = new LinkedHashMap<>();
        public final List<Analyzer.Symbol> outline = new ArrayList<>();

        Result(Parser.Result parse) {
            this.parse = parse;
        }

        public Ast.Program program() {
            return this.parse.program;
        }

        public boolean hasErrors() {
            for (Diagnostic d : this.diagnostics) {
                if (d.isError()) {
                    return true;
                }
            }

            return false;
        }

        public int errorCount() {
            int n = 0;

            for (Diagnostic d : this.diagnostics) {
                if (d.isError()) {
                    n++;
                }
            }

            return n;
        }

        public int warningCount() {
            int n = 0;

            for (Diagnostic d : this.diagnostics) {
                if (d.severity == Diagnostic.Severity.WARNING) {
                    n++;
                }
            }

            return n;
        }

        public Analyzer.Symbol lookup(String name) {
            Analyzer.Symbol s = this.characters.get(name);
            if (s == null) {
                s = this.locations.get(name);
            }

            if (s == null) {
                s = this.vars.get(name);
            }

            if (s == null) {
                s = this.labels.get(name);
            }

            if (s == null) {
                s = this.functions.get(name);
            }

            if (s == null) {
                s = this.branches.get(name);
            }

            return s;
        }
    }

    public record Symbol(String name, Analyzer.SymbolKind kind, int start, int end, String detail) {
    }

    public static enum SymbolKind {
        CHARACTER,
        LOCATION,
        VARIABLE,
        LABEL,
        PARALLEL,
        FUNCTION;
    }
}
