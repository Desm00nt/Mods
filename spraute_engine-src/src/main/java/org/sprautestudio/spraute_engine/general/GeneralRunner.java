package org.sprautestudio.spraute_engine.general;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class GeneralRunner {
    private static final int STEPS_PER_TICK = 2000;
    private static final Pattern INTERPOLATION = Pattern.compile("(?<!\\\\)\\{([^{}]*)}");
    private final Ast.Program program;
    private final String source;
    private final GeneralRunner.Host host;
    private final Map<String, Object> vars = new HashMap<>();
    private final List<GeneralRunner.Branch> branches = new ArrayList<>();
    private final Map<String, GeneralRunner.Branch> named = new LinkedHashMap<>();
    private final List<GeneralRunner.Handler> handlers = new ArrayList<>();
    private final Set<Ast.OnStmt> registered = new HashSet<>();
    private final Random random = new Random();
    private boolean stopped;
    private String lastButton = "";
    private String lastMenu = "";
    private int buttonSeq;
    private int menuSeq;

    public GeneralRunner(Ast.Program program, String source, GeneralRunner.Host host) {
        this.program = program;
        this.source = source;
        this.host = host;
    }

    public void start() {
        try {
            for (Ast.VarDecl v : this.program.vars) {
                this.vars.put(v.name, this.eval(v.init));
            }

            for (Ast.Stmt s : this.program.body) {
                if (s instanceof Ast.OnStmt on) {
                    this.register(on);
                }
            }

            this.branches.add(new GeneralRunner.Branch(new GeneralRunner.Frame(this.program.body, GeneralRunner.FrameKind.ROOT)));
        } catch (RuntimeException var4) {
            this.fail(this.program, var4);
            this.stop();
        }
    }

    public void stop() {
        this.stopped = true;
        this.branches.clear();
        this.handlers.clear();
    }

    public boolean stopped() {
        return this.stopped;
    }

    public boolean listens(String button) {
        for (GeneralRunner.Handler h : this.handlers) {
            if (h.event.equals("wait button") && h.filter.contains(button)) {
                return true;
            }
        }

        return false;
    }

    public void button(String id) {
        if (!this.stopped) {
            this.lastButton = id;
            this.buttonSeq++;
            this.fire("wait button", id);
        }
    }

    public void menu(String name) {
        if (!this.stopped) {
            this.lastMenu = name;
            this.menuSeq++;
            this.fire("wait menu", name);
        }
    }

    private void fire(String event, String value) {
        for (GeneralRunner.Handler h : new ArrayList<>(this.handlers)) {
            if (h.event.equals(event) && (h.filter.isEmpty() || h.filter.contains(value))) {
                this.branches.add(new GeneralRunner.Branch(new GeneralRunner.Frame(h.on.body, GeneralRunner.FrameKind.ROOT)));
            }
        }
    }

    public void tick() {
        if (!this.stopped) {
            for (int i = 0; i < this.branches.size() && !this.stopped; i++) {
                this.step(this.branches.get(i));
            }

            this.branches.removeIf(b -> b.done);
        }
    }

    private void step(GeneralRunner.Branch b) {
        if (!b.done) {
            if (b.sleep <= 0 || --b.sleep <= 0) {
                Ast.Stmt at = null;

                try {
                    if (b.waiting != null) {
                        if (!b.waiting.getAsBoolean()) {
                            return;
                        }

                        b.waiting = null;
                    }

                    for (int steps = 0; steps < 2000; steps++) {
                        if (b.frames.isEmpty()) {
                            b.done = true;
                            return;
                        }

                        GeneralRunner.Frame f = b.frames.get(b.frames.size() - 1);
                        if (f.pc >= f.body.size()) {
                            this.finish(b, f);
                            if (b.sleep > 0) {
                                return;
                            }
                        } else {
                            at = f.body.get(f.pc++);
                            this.run(b, at);
                            if (b.done || b.sleep > 0 || b.waiting != null || this.stopped) {
                                return;
                            }
                        }
                    }
                } catch (RuntimeException var5) {
                    this.fail((Ast.Node)(at != null ? at : this.program), var5);
                    b.done = true;
                }
            }
        }
    }

    private void fail(Ast.Node at, RuntimeException e) {
        int line = 1;

        for (int i = 0; i < Math.min(at.start, this.source.length()); i++) {
            if (this.source.charAt(i) == '\n') {
                line++;
            }
        }

        this.host.error("Строка " + line + ": " + (e instanceof SprauteRuntimeException ? e.getMessage() : e.toString()));
    }

    private void finish(GeneralRunner.Branch b, GeneralRunner.Frame f) {
        switch (f.kind) {
            case WHILE:
                if (this.host.truthy(this.eval(f.cond))) {
                    f.pc = 0;
                } else {
                    pop(b);
                }
                break;
            case REPEAT:
                if (--f.left > 0) {
                    f.pc = 0;
                } else {
                    pop(b);
                }
                break;
            case FOR:
                if (f.items.hasNext()) {
                    this.vars.put(f.var, f.items.next());
                    f.pc = 0;
                } else {
                    pop(b);
                }
                break;
            case EVERY:
                f.pc = 0;
                b.sleep = Math.max(1, (int)Math.round(this.host.num(this.eval(f.cond))));
                break;
            default:
                pop(b);
        }
    }

    private static void pop(GeneralRunner.Branch b) {
        b.frames.remove(b.frames.size() - 1);
    }

    private void run(GeneralRunner.Branch b, Ast.Stmt s) {
        if (s instanceof Ast.CommandStmt c) {
            this.command(b, c);
        } else if (s instanceof Ast.SetStmt set) {
            this.set(set);
        } else if (s instanceof Ast.IfStmt i) {
            if (this.host.truthy(this.eval(i.cond))) {
                push(b, i.then);
            } else if (i.otherwise != null) {
                push(b, i.otherwise);
            }
        } else if (s instanceof Ast.WhileStmt w) {
            if (this.host.truthy(this.eval(w.cond))) {
                GeneralRunner.Frame f = new GeneralRunner.Frame(w.body, GeneralRunner.FrameKind.WHILE);
                f.cond = w.cond;
                b.frames.add(f);
            }
        } else if (s instanceof Ast.RepeatStmt r) {
            int n = (int)Math.floor(this.host.num(this.eval(r.count)));
            if (n > 0) {
                GeneralRunner.Frame f = new GeneralRunner.Frame(r.body, GeneralRunner.FrameKind.REPEAT);
                f.left = n;
                b.frames.add(f);
            }
        } else if (s instanceof Ast.ForStmt fs) {
            Iterator<Object> items = this.host.iterable(this.eval(fs.iter)).iterator();
            if (items.hasNext()) {
                this.vars.put(fs.var, items.next());
                GeneralRunner.Frame f = new GeneralRunner.Frame(fs.body, GeneralRunner.FrameKind.FOR);
                f.var = fs.var;
                f.items = items;
                b.frames.add(f);
            }
        } else if (s instanceof Ast.BreakStmt) {
            unwindLoop(b);
            pop(b);
        } else if (s instanceof Ast.ContinueStmt) {
            unwindLoop(b);
            GeneralRunner.Frame loop = b.frames.get(b.frames.size() - 1);
            loop.pc = loop.body.size();
        } else if (s instanceof Ast.ReturnStmt) {
            while (!b.frames.isEmpty()) {
                GeneralRunner.Frame f = b.frames.remove(b.frames.size() - 1);
                if (f.kind == GeneralRunner.FrameKind.FUNC) {
                    return;
                }
            }
        } else if (s instanceof Ast.StopStmt) {
            this.stop();
        } else if (s instanceof Ast.SwitchStmt sw) {
            Object v = this.eval(sw.value);

            for (Ast.SwitchCase c : sw.cases) {
                for (Ast.Expr x : c.values) {
                    if (this.host.same(v, this.eval(x))) {
                        push(b, c.body);
                        return;
                    }
                }
            }

            if (sw.otherwise != null) {
                push(b, sw.otherwise);
            }
        } else if (!(s instanceof Ast.LabelStmt)) {
            if (s instanceof Ast.GotoStmt g) {
                this.jump(b, g.name);
            } else if (s instanceof Ast.CallStmt c) {
                this.call(b, c);
            } else if (s instanceof Ast.ParallelStmt p) {
                GeneralRunner.Branch sub = new GeneralRunner.Branch(new GeneralRunner.Frame(p.body, GeneralRunner.FrameKind.ROOT));
                if (p.name != null) {
                    this.named.put(p.name, sub);
                }

                this.branches.add(sub);
            } else if (s instanceof Ast.AwaitStmt a) {
                List<GeneralRunner.Branch> all = new ArrayList<>();

                for (String n : a.names) {
                    GeneralRunner.Branch wx = this.named.get(n);
                    if (wx != null) {
                        all.add(wx);
                    }
                }

                b.waiting = () -> {
                    for (GeneralRunner.Branch wx : all) {
                        if (!wx.done) {
                            return false;
                        }
                    }

                    return true;
                };
            } else if (s instanceof Ast.CancelStmt c) {
                GeneralRunner.Branch wx = this.named.get(c.name);
                if (wx != null) {
                    wx.done = true;
                }
            } else if (s instanceof Ast.EveryStmt ev) {
                GeneralRunner.Frame f = new GeneralRunner.Frame(ev.body, GeneralRunner.FrameKind.EVERY);
                f.cond = ev.interval;
                this.branches.add(new GeneralRunner.Branch(f));
            } else if (s instanceof Ast.OnStmt on) {
                this.register(on);
            } else {
                if (!(s instanceof Ast.MethodStmt m)) {
                    throw new SprauteRuntimeException("В общем скрипте это недоступно");
                }

                List<Object> args = new ArrayList<>();

                for (Ast.Expr xx : m.args) {
                    args.add(this.eval(xx));
                }

                this.host.method(this.variable(m.target), m.method, args);
            }
        }
    }

    private static void push(GeneralRunner.Branch b, List<Ast.Stmt> body) {
        b.frames.add(new GeneralRunner.Frame(body, GeneralRunner.FrameKind.BLOCK));
    }

    private static void unwindLoop(GeneralRunner.Branch b) {
        while (!b.frames.isEmpty() && !b.frames.get(b.frames.size() - 1).isLoop()) {
            pop(b);
        }

        if (b.frames.isEmpty()) {
            throw new SprauteRuntimeException("break и continue работают только в цикле");
        }
    }

    private void jump(GeneralRunner.Branch b, String label) {
        for (int i = b.frames.size() - 1; i >= 0; i--) {
            GeneralRunner.Frame f = b.frames.get(i);

            for (int at = 0; at < f.body.size(); at++) {
                if (f.body.get(at) instanceof Ast.LabelStmt l && l.name.equals(label)) {
                    while (b.frames.size() > i + 1) {
                        pop(b);
                    }

                    f.pc = at + 1;
                    return;
                }
            }

            if (f.kind == GeneralRunner.FrameKind.FUNC) {
                break;
            }
        }

        throw new SprauteRuntimeException("Метка «" + label + "» не найдена рядом: в общем скрипте переходить можно только наружу из блока");
    }

    private void call(GeneralRunner.Branch b, Ast.CallStmt c) {
        Ast.FuncDecl f = this.program.func(c.name);
        if (f == null) {
            throw new SprauteRuntimeException("Нет функции " + c.name);
        } else {
            for (int i = 0; i < f.params.size() && i < c.args.size(); i++) {
                this.vars.put(f.params.get(i), this.eval(c.args.get(i)));
            }

            GeneralRunner.Frame frame = new GeneralRunner.Frame(f.body, GeneralRunner.FrameKind.FUNC);
            if (c.async) {
                GeneralRunner.Branch sub = new GeneralRunner.Branch(frame);
                if (c.as != null) {
                    this.named.put(c.as, sub);
                }

                this.branches.add(sub);
            } else {
                b.frames.add(frame);
            }
        }
    }

    private void register(Ast.OnStmt on) {
        if (this.registered.add(on)) {
            Ast.CommandStmt e = on.event;
            Set<String> filter = new HashSet<>();

            for (Ast.Arg a : e.args) {
                filter.add(this.host.str(this.eval(a.value)));
            }

            this.handlers.add(new GeneralRunner.Handler(on, filter, e.key));
        }
    }

    private void command(GeneralRunner.Branch b, Ast.CommandStmt c) {
        String var3 = c.key;
        switch (var3) {
            case "wait":
                b.sleep = Math.max(1, this.ticks(c.arg("time")));
                break;
            case "wait random":
                int lo = this.ticks(c.arg("min"));
                int hi = this.ticks(c.arg("max"));
                b.sleep = Math.max(1, hi > lo ? lo + this.random.nextInt(hi - lo + 1) : lo);
                break;
            case "wait until":
                Ast.Expr cond = c.arg("condition");
                b.waiting = () -> this.host.truthy(this.eval(cond));
                break;
            case "wait button": {
                Set<String> ids = new HashSet<>();

                for (Ast.Arg ax : c.args) {
                    if ("buttons".equals(ax.param)) {
                        ids.add(this.host.str(this.eval(ax.value)));
                    }
                }

                int from = this.buttonSeq;
                b.waiting = () -> this.buttonSeq > from && (ids.isEmpty() || ids.contains(this.lastButton));
                break;
            }
            case "wait menu": {
                Ast.Expr name = c.arg("name");
                String want = name != null ? this.host.str(this.eval(name)) : "";
                int from = this.menuSeq;
                b.waiting = () -> this.menuSeq > from && (want.isEmpty() || want.equals(this.lastMenu));
                break;
            }
            default:
                Map<String, Object> args = new HashMap<>();

                for (Ast.Arg a : c.args) {
                    if (a.param != null && a.value != null) {
                        args.put(a.param, this.eval(a.value));
                    }
                }

                this.host.command(c.key, args);
        }
    }

    private int ticks(Ast.Expr e) {
        return e instanceof Ast.DurationLit d ? d.ticks : (int)Math.round(this.host.num(this.eval(e)));
    }

    private void set(Ast.SetStmt s) {
        Object value = this.eval(s.value);
        if (s.index != null) {
            Object target = this.variable(s.name);
            Object at = this.eval(s.index);
            this.host.setIndex(target, at, this.combine(s.op, this.host.index(target, at), value, s.op.equals("=")));
        } else if (s.prop != null) {
            throw new SprauteRuntimeException("В общем скрипте нет ни игрока, ни персонажей");
        } else {
            this.vars.put(s.name, this.combine(s.op, s.op.equals("=") ? null : this.variable(s.name), value, s.op.equals("=")));
        }
    }

    private Object combine(String op, Object old, Object value, boolean replace) {
        if (replace) {
            return value;
        } else {
            return op.equals("+=") ? this.plus(old, value) : this.host.num(old) - this.host.num(value);
        }
    }

    private Object variable(String name) {
        if (!this.vars.containsKey(name)) {
            throw new SprauteRuntimeException("Неизвестное имя '" + name + "'");
        } else {
            return this.vars.get(name);
        }
    }

    private Object eval(Ast.Expr e) {
        if (e instanceof Ast.NumberLit n) {
            return n.value;
        } else if (e instanceof Ast.DurationLit d) {
            return (double)d.ticks;
        } else if (e instanceof Ast.StringLit s) {
            return this.interpolate(s.value);
        } else if (e instanceof Ast.BoolLit b) {
            return b.value;
        } else if (e instanceof Ast.Ident id) {
            return this.variable(id.name);
        } else if (e instanceof Ast.Unary u) {
            Object v = this.eval(u.operand);
            return u.op.equals("-") ? -this.host.num(v) : !this.host.truthy(v);
        } else if (e instanceof Ast.Binary b) {
            return this.binary(b);
        } else if (e instanceof Ast.Call c) {
            return this.call(c);
        } else if (e instanceof Ast.Tuple t) {
            return this.list(t.items);
        } else if (e instanceof Ast.ListLit l) {
            return this.list(l.items);
        } else if (!(e instanceof Ast.MapLit m)) {
            if (e instanceof Ast.Index ix) {
                return this.host.index(this.eval(ix.target), this.eval(ix.index));
            } else if (e instanceof Ast.Member mx) {
                Object target = this.eval(mx.target);
                return mx.args == null ? this.host.property(target, mx.name) : this.host.method(target, mx.name, this.list(mx.args));
            } else {
                throw new SprauteRuntimeException("Неподдерживаемое выражение");
            }
        } else {
            Map<String, Object> out = this.host.map();

            for (int i = 0; i < m.keys.size(); i++) {
                out.put(this.host.str(this.eval(m.keys.get(i))), this.eval(m.values.get(i)));
            }

            return out;
        }
    }

    private List<Object> list(List<Ast.Expr> items) {
        List<Object> out = new ArrayList<>();

        for (Ast.Expr x : items) {
            out.add(this.eval(x));
        }

        return out;
    }

    private Object call(Ast.Call c) {
        String var2 = c.name;

        return switch (var2) {
            case "button" -> this.lastButton;
            case "menu" -> this.lastMenu;
            case "input" -> this.host.input(this.host.str(this.eval(c.args.get(0))));
            case "guiopen" -> this.host.guiOpen();
            default -> this.host.function(c.name, this.list(c.args));
        };
    }

    private Object plus(Object l, Object r) {
        if (!(l instanceof String) && !(r instanceof String)) {
            if (l instanceof List<?> a && r instanceof List<?> b) {
                List<Object> out = new ArrayList<>((Collection<? extends Object>)a);
                out.addAll((Collection<? extends Object>)b);
                return out;
            } else {
                return this.host.num(l) + this.host.num(r);
            }
        } else {
            return this.host.str(l) + this.host.str(r);
        }
    }

    private Object binary(Ast.Binary b) {
        if (b.op.equals("and")) {
            return this.host.truthy(this.eval(b.left)) && this.host.truthy(this.eval(b.right));
        } else if (!b.op.equals("or")) {
            Object l = this.eval(b.left);
            Object r = this.eval(b.right);
            String var4 = b.op;

            return switch (var4) {
                case "+" -> this.plus(l, r);
                case "-" -> this.host.num(l) - this.host.num(r);
                case "*" -> this.host.num(l) * this.host.num(r);
                case "/", "%" -> {
                    double d = this.host.num(r);
                    if (d == 0.0) {
                        throw new SprauteRuntimeException("Деление на ноль");
                    }

                    yield b.op.equals("/") ? this.host.num(l) / d : this.host.num(l) % d;
                }
                case "==" -> this.host.same(l, r);
                case "!=" -> !this.host.same(l, r);
                case "<" -> this.host.compare(l, r) < 0;
                case "<=" -> this.host.compare(l, r) <= 0;
                case ">" -> this.host.compare(l, r) > 0;
                case ">=" -> this.host.compare(l, r) >= 0;
                default -> throw new SprauteRuntimeException("Неизвестный оператор " + b.op);
            };
        } else {
            return this.host.truthy(this.eval(b.left)) || this.host.truthy(this.eval(b.right));
        }
    }

    private String interpolate(String text) {
        if (text.indexOf(123) < 0) {
            return text.replace("\\{", "{");
        } else {
            Matcher m = INTERPOLATION.matcher(text);
            StringBuilder sb = new StringBuilder();

            while (m.find()) {
                String inner = m.group(1).trim();
                Ast.Expr expr = !inner.isEmpty() && !inner.matches("(?s)[\\w.+-]+\\s*:.*") ? Parser.parseExpression(inner) : null;
                String value;
                if (expr == null) {
                    value = m.group(0);
                } else {
                    try {
                        value = this.host.str(this.eval(expr));
                    } catch (SprauteRuntimeException var8) {
                        value = "{" + inner + "?}";
                    }
                }

                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }

            m.appendTail(sb);
            return sb.toString().replace("\\{", "{");
        }
    }

    private static final class Branch {
        final List<GeneralRunner.Frame> frames = new ArrayList<>();
        BooleanSupplier waiting;
        int sleep;
        boolean done;

        Branch(GeneralRunner.Frame first) {
            this.frames.add(first);
        }
    }

    private static final class Frame {
        final List<Ast.Stmt> body;
        final GeneralRunner.FrameKind kind;
        int pc;
        Ast.Expr cond;
        int left;
        String var;
        Iterator<Object> items;

        Frame(List<Ast.Stmt> body, GeneralRunner.FrameKind kind) {
            this.body = body;
            this.kind = kind;
        }

        boolean isLoop() {
            return this.kind == GeneralRunner.FrameKind.WHILE
                || this.kind == GeneralRunner.FrameKind.REPEAT
                || this.kind == GeneralRunner.FrameKind.FOR
                || this.kind == GeneralRunner.FrameKind.EVERY;
        }
    }

    private static enum FrameKind {
        BLOCK,
        ROOT,
        FUNC,
        WHILE,
        REPEAT,
        FOR,
        EVERY;
    }

    private record Handler(Ast.OnStmt on, Set<String> filter, String event) {
    }

    public interface Host {
        void command(String var1, Map<String, Object> var2);

        Object function(String var1, List<Object> var2);

        String input(String var1);

        boolean guiOpen();

        void error(String var1);

        boolean truthy(Object var1);

        double num(Object var1);

        String str(Object var1);

        boolean same(Object var1, Object var2);

        int compare(Object var1, Object var2);

        Object index(Object var1, Object var2);

        void setIndex(Object var1, Object var2, Object var3);

        Object property(Object var1, String var2);

        Object method(Object var1, String var2, List<Object> var3);

        List<Object> iterable(Object var1);

        Map<String, Object> map();
    }
}
