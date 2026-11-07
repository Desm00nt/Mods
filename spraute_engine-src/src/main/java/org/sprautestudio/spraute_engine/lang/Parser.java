package org.sprautestudio.spraute_engine.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Parser {
    private final String src;
    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final int[] lineStarts;
    private int p;
    private Map<String, Integer> copies = new HashMap<>();
    public static final int MAX_COPIES = 100;

    private Parser(String src) {
        this.src = src;
        this.tokens = Lexer.tokenize(src);
        this.lineStarts = computeLineStarts(src);
    }

    public static Parser.Result parse(String src) {
        Parser parser = new Parser(src);
        Ast.Program program = parser.program();
        return new Parser.Result(program, parser.diagnostics, parser.tokens);
    }

    public static Ast.Expr parseExpression(String src) {
        Parser parser = new Parser(src);

        try {
            Ast.Expr e = parser.expression();
            return parser.peek().type == TokenType.EOF && parser.diagnostics.isEmpty() ? e : null;
        } catch (Parser.ParseError var3) {
            return null;
        }
    }

    static int[] computeLineStarts(String src) {
        List<Integer> starts = new ArrayList<>();
        starts.add(0);

        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                starts.add(i + 1);
            }
        }

        int[] arr = new int[starts.size()];

        for (int ix = 0; ix < arr.length; ix++) {
            arr[ix] = starts.get(ix);
        }

        return arr;
    }

    private Token peek() {
        return this.tokens.get(this.p);
    }

    private Token peek(int off) {
        return this.tokens.get(Math.min(this.p + off, this.tokens.size() - 1));
    }

    private Token next() {
        Token t = this.tokens.get(this.p);
        if (t.type != TokenType.EOF) {
            this.p++;
        }

        return t;
    }

    private Token prev() {
        return this.tokens.get(Math.max(0, this.p - 1));
    }

    private boolean atStatementEnd() {
        TokenType t = this.peek().type;
        return t == TokenType.NEWLINE || t == TokenType.EOF || t == TokenType.RBRACE || t == TokenType.LBRACE;
    }

    void error(String msg, int start, int end) {
        this.add(Diagnostic.Severity.ERROR, msg, start, end);
    }

    void warn(String msg, int start, int end) {
        this.add(Diagnostic.Severity.WARNING, msg, start, end);
    }

    private void add(Diagnostic.Severity sev, String msg, int start, int end) {
        int line = this.lineOf(start);
        this.diagnostics.add(new Diagnostic(sev, msg, start, end, line, start - this.lineStarts[line]));
    }

    private int lineOf(int offset) {
        int lo = 0;
        int hi = this.lineStarts.length - 1;

        while (lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (this.lineStarts[mid] <= offset) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }

        return lo;
    }

    private Parser.ParseError fail(String msg, Token at) {
        int end = at.type != TokenType.NEWLINE && at.type != TokenType.EOF ? at.end : at.start + 1;
        this.error(msg, at.start, Math.min(end, Math.max(at.start + 1, this.src.length())));
        return new Parser.ParseError();
    }

    private Token expect(TokenType type, String what) {
        if (this.peek().type != type) {
            throw this.fail("Ожидалось " + what, this.peek());
        } else {
            return this.next();
        }
    }

    private Token expectIdent(String what) {
        Token t = this.peek();
        if (t.type != TokenType.IDENT) {
            throw this.fail("Ожидалось " + what, t);
        } else {
            return this.next();
        }
    }

    private void expectEnd() {
        if (!this.atStatementEnd()) {
            throw this.fail("Ожидался конец строки", this.peek());
        }
    }

    private void skipNewlines() {
        while (this.peek().type == TokenType.NEWLINE) {
            this.next();
        }
    }

    private void recover() {
        int depth = 0;

        while (this.peek().type != TokenType.EOF) {
            TokenType t = this.peek().type;
            if (t == TokenType.NEWLINE && depth <= 0) {
                return;
            }

            if (t == TokenType.LBRACE) {
                depth++;
            }

            if (t == TokenType.RBRACE) {
                if (depth <= 0) {
                    return;
                }

                depth--;
            }

            this.next();
        }
    }

    private Ast.Program program() {
        Ast.Program program = new Ast.Program();
        this.copies = program.copies;

        for (Token t : this.tokens) {
            if (t.type == TokenType.ERROR) {
                this.error(String.valueOf(t.value), t.start, t.end);
            }
        }

        this.skipNewlines();

        for (; this.peek().type != TokenType.EOF; this.skipNewlines()) {
            try {
                Token tx = this.peek();
                if (tx.isWord("script")) {
                    this.header(program);
                } else if (tx.isWord("character")) {
                    program.characters.add(this.characterDecl());
                } else if (tx.isWord("location")) {
                    program.locations.add(this.locationDecl());
                } else if (tx.isWord("var")) {
                    program.vars.add(this.varDecl());
                } else if (tx.isWord("func")) {
                    Ast.FuncDecl f = this.funcDecl();
                    if (program.func(f.name) != null) {
                        this.error("Функция '" + f.name + "' уже объявлена", f.nameStart, f.nameEnd);
                    }

                    program.funcs.add(f);
                } else if (tx.type == TokenType.RBRACE) {
                    this.error("Лишняя закрывающая скобка }", tx.start, tx.end);
                    this.next();
                } else {
                    Ast.Stmt s = this.statement();
                    if (s != null) {
                        program.body.add(s);
                    }
                }
            } catch (Parser.ParseError var4) {
                this.recover();
                if (this.peek().type == TokenType.RBRACE) {
                    this.next();
                }
            }
        }

        program.at(0, this.src.length());
        if (program.header == null) {
            this.warn("Нет заголовка: добавьте строку script <id> \"Название\"", 0, Math.min(1, this.src.length()));
        }

        return program;
    }

    private void header(Ast.Program program) {
        Token kw = this.next();
        Ast.Header h = new Ast.Header();
        Token id = this.peek();
        if (id.type != TokenType.NUMBER) {
            throw this.fail("Ожидался числовой id сюжета", id);
        } else {
            this.next();
            double v = (Double)id.value;
            if (v != Math.floor(v)) {
                this.error("Id сюжета должен быть целым", id.start, id.end);
            }

            h.id = (int)v;
            h.idStart = id.start;
            h.idEnd = id.end;
            if (this.peek().type == TokenType.STRING) {
                h.title = (String)this.next().value;
            }

            if (this.peek().isWord("solo")) {
                this.next();
                h.solo = true;
            }

            if (this.peek().isWord("autostart")) {
                this.next();
                h.autostart = true;
                if (this.peek().isWord("once")) {
                    this.next();
                    h.once = true;
                }
            }

            if (this.peek().isWord("solo")) {
                this.next();
                h.solo = true;
            }

            if (this.peek().isWord("general")) {
                this.next();
                h.general = true;
            }

            this.expectEnd();
            if (program.header != null) {
                this.error("Заголовок script уже объявлен", kw.start, kw.end);
            }

            program.header = h.at(kw.start, this.prev().end);
        }
    }

    private Ast.CharacterDecl characterDecl() {
        Token kw = this.next();
        Ast.CharacterDecl d = new Ast.CharacterDecl();
        Token name = this.expectIdent("имя персонажа");
        d.name = name.text;
        d.nameStart = name.start;
        d.nameEnd = name.end;
        if (!this.peek().isOp("=")) {
            throw this.fail("Ожидалось = после имени персонажа", this.peek());
        } else {
            this.next();
            if (this.peek().isWord("existing")) {
                this.next();
                d.existing = this.primary();
            } else {
                d.type = this.primary();
            }

            Set<String> seen = new HashSet<>();

            while (!this.atStatementEnd()) {
                Token opt = this.peek();
                if (opt.type != TokenType.IDENT || !seen.add(opt.text)) {
                    throw this.fail("Неожиданный параметр персонажа", opt);
                }

                this.next();
                String var6 = opt.text;
                switch (var6) {
                    case "name":
                        d.displayName = this.primary();
                        break;
                    case "speed":
                        d.speed = this.unary();
                        break;
                    case "skin":
                        d.skin = this.primary();
                        break;
                    case "icon":
                        d.icon = this.primary();
                        break;
                    case "portrait":
                        d.portrait = this.primary();
                        break;
                    case "headscale":
                        d.headScale = this.unary();
                        break;
                    case "headx":
                        d.headX = this.unary();
                        break;
                    case "heady":
                        d.headY = this.unary();
                        break;
                    case "headyaw":
                        d.headYaw = this.unary();
                        break;
                    case "headbone":
                        d.headBone = this.primary();
                        break;
                    case "color":
                        d.color = this.primary();
                        break;
                    case "hitbox":
                        d.hitbox = this.primary();
                        break;
                    case "health":
                    case "hp":
                        d.health = this.unary();
                        break;
                    case "priority":
                        d.priority = this.unary();
                        break;
                    case "id":
                        d.sharedId = this.primary();
                        break;
                    default:
                        throw this.fail(
                            "Неизвестный параметр персонажа '"
                                + opt.text
                                + "' (есть: name, color, hitbox, health, speed, skin, icon, portrait, headscale, headx, heady, headyaw, headbone)",
                            opt
                        );
                }
            }

            this.expectEnd();
            return d.at(kw.start, this.prev().end);
        }
    }

    private Ast.LocationDecl locationDecl() {
        Token kw = this.next();
        Ast.LocationDecl d = new Ast.LocationDecl();
        Token name = this.expectIdent("имя точки");
        d.name = name.text;
        d.nameStart = name.start;
        d.nameEnd = name.end;
        if (!this.peek().isOp("=")) {
            throw this.fail("Ожидалось = после имени точки", this.peek());
        } else {
            this.next();
            d.pos = this.primary();
            Set<String> seen = new HashSet<>();

            while (!this.atStatementEnd()) {
                Token opt = this.peek();
                if (opt.type != TokenType.IDENT || !seen.add(opt.text)) {
                    throw this.fail("Неожиданный параметр точки", opt);
                }

                this.next();
                String var6 = opt.text;
                switch (var6) {
                    case "yaw":
                        d.yaw = this.unary();
                        break;
                    case "pitch":
                        d.pitch = this.unary();
                        break;
                    case "dim":
                        d.dimension = this.primary();
                        break;
                    default:
                        throw this.fail("Неизвестный параметр точки '" + opt.text + "' (есть: yaw, pitch, dim)", opt);
                }
            }

            this.expectEnd();
            return d.at(kw.start, this.prev().end);
        }
    }

    private Ast.VarDecl varDecl() {
        Token kw = this.next();
        Ast.VarDecl d = new Ast.VarDecl();
        Token name = this.expectIdent("имя переменной");
        d.name = name.text;
        d.nameStart = name.start;
        d.nameEnd = name.end;
        if (!this.peek().isOp("=")) {
            throw this.fail("Ожидалось =", this.peek());
        } else {
            this.next();
            d.init = this.expression();
            this.expectEnd();
            return d.at(kw.start, this.prev().end);
        }
    }

    private List<Ast.Stmt> block() {
        this.expect(TokenType.LBRACE, "{");
        List<Ast.Stmt> list = new ArrayList<>();
        this.skipNewlines();

        for (; this.peek().type != TokenType.RBRACE; this.skipNewlines()) {
            if (this.peek().type == TokenType.EOF) {
                throw this.fail("Блок не закрыт: не хватает }", this.peek());
            }

            try {
                Token t = this.peek();
                if (t.isWord("character") || t.isWord("location") || t.isWord("var") || t.isWord("script") || t.isWord("func")) {
                    throw this.fail("'" + t.text + "' можно объявлять только на верхнем уровне файла", t);
                }

                Ast.Stmt s = this.statement();
                if (s != null) {
                    list.add(s);
                }
            } catch (Parser.ParseError var4) {
                this.recover();
            }
        }

        this.next();
        return list;
    }

    private Ast.Stmt statement() {
        Token t = this.peek();
        if (t.isWord("label")) {
            return this.label();
        } else if (t.isWord("goto")) {
            return this.gotoStmt();
        } else if (t.isWord("stop")) {
            this.next();
            this.expectEnd();
            return new Ast.StopStmt().at(t.start, t.end);
        } else if (t.isWord("set")) {
            return this.setStmt();
        } else if (t.isWord("if")) {
            return this.ifStmt();
        } else if (t.isWord("while")) {
            this.next();
            Ast.WhileStmt w = new Ast.WhileStmt();
            w.cond = this.expression();
            w.body.addAll(this.block());
            return w.at(t.start, this.prev().end);
        } else if (t.isWord("repeat")) {
            this.next();
            Ast.RepeatStmt r = new Ast.RepeatStmt();
            r.count = this.expression();
            r.body.addAll(this.block());
            return r.at(t.start, this.prev().end);
        } else if (t.isWord("as")) {
            this.next();
            Ast.AsStmt a = new Ast.AsStmt();
            a.who = this.expression();
            a.body.addAll(this.block());
            return a.at(t.start, this.prev().end);
        } else if (t.isWord("for")) {
            this.next();
            Ast.ForStmt f = new Ast.ForStmt();
            Token v = this.expectIdent("имя переменной цикла");
            f.var = v.text;
            f.varStart = v.start;
            f.varEnd = v.end;
            if (!this.peek().isWord("in")) {
                throw this.fail("Ожидалось in: for x in список { }", this.peek());
            } else {
                this.next();
                f.iter = this.expression();
                f.body.addAll(this.block());
                return f.at(t.start, this.prev().end);
            }
        } else if (t.isWord("every")) {
            this.next();
            Ast.EveryStmt e = new Ast.EveryStmt();
            e.interval = this.expression();
            e.body.addAll(this.block());
            return e.at(t.start, this.prev().end);
        } else if (t.isWord("on") && this.peek(1).isWord("tick")) {
            this.next();
            Token tick = this.next();
            Ast.EveryStmt e = new Ast.EveryStmt();
            Ast.DurationLit one = new Ast.DurationLit();
            one.ticks = 1;
            e.interval = one.at(tick.start, tick.end);
            e.body.addAll(this.block());
            return e.at(t.start, this.prev().end);
        } else {
            if (t.isWord("break") || t.isWord("continue")) {
                TokenType after = this.peek(1).type;
                if (after == TokenType.NEWLINE || after == TokenType.RBRACE || after == TokenType.EOF) {
                    this.next();
                    return ((Ast.Stmt)(t.isWord("break") ? new Ast.BreakStmt() : new Ast.ContinueStmt())).at(t.start, t.end);
                }
            }

            if ((t.type == TokenType.IDENT || t.isWord("player"))
                && this.peek(1).isOp(".")
                && this.peek(1).start == t.end
                && this.peek(2).type == TokenType.IDENT
                && this.peek(3).type == TokenType.LPAREN) {
                this.next();
                this.next();
                Token m = this.next();
                this.next();
                Ast.MethodStmt ms = new Ast.MethodStmt();
                ms.target = t.text;
                ms.targetStart = t.start;
                ms.targetEnd = t.end;
                ms.method = m.text;
                this.skipNewlines();
                if (this.peek().type != TokenType.RPAREN) {
                    do {
                        this.skipNewlines();
                        ms.args.add(this.expression());
                        this.skipNewlines();
                    } while (this.peek().type == TokenType.COMMA && this.next() != null);
                }

                this.expect(TokenType.RPAREN, ")");
                this.expectEnd();
                return ms.at(t.start, this.prev().end);
            } else if (t.isWord("choice")) {
                return this.choice();
            } else if (t.isWord("parallel")) {
                this.next();
                Ast.ParallelStmt ps = new Ast.ParallelStmt();
                ps.name = this.expectIdent("имя параллельной ветки").text;
                ps.body.addAll(this.block());
                return ps.at(t.start, this.prev().end);
            } else if (t.isWord("call")) {
                this.next();
                return this.callRest(t, false);
            } else if (t.isWord("on")) {
                return this.onStmt();
            } else if (t.isWord("switch")) {
                return this.switchStmt();
            } else if (t.isWord("async")) {
                this.next();
                if (this.peek().isWord("call")) {
                    this.next();
                    return this.callRest(t, true);
                } else {
                    Ast.ParallelStmt ps = new Ast.ParallelStmt();
                    if (this.peek().type == TokenType.IDENT) {
                        ps.name = this.next().text;
                    }

                    if (this.peek().type != TokenType.LBRACE) {
                        throw this.fail("После async ожидалось: call функция(...), имя ветки или {", this.peek());
                    } else {
                        ps.body.addAll(this.block());
                        return ps.at(t.start, this.prev().end);
                    }
                }
            } else if (t.isWord("return")) {
                this.next();
                this.expectEnd();
                return new Ast.ReturnStmt().at(t.start, t.end);
            } else if (!t.isWord("await")) {
                if (t.isWord("cancel")) {
                    this.next();
                    Ast.CancelStmt cs = new Ast.CancelStmt();
                    Token n = this.expectIdent("имя асинхронной ветки");
                    cs.name = n.text;
                    cs.nameStart = n.start;
                    cs.nameEnd = n.end;
                    this.expectEnd();
                    return cs.at(t.start, n.end);
                } else if (t.type == TokenType.IDENT) {
                    return this.command();
                } else {
                    throw this.fail(t.type == TokenType.KEYWORD ? "Здесь нельзя использовать '" + t.text + "'" : "Ожидалась команда", t);
                }
            } else {
                this.next();
                Ast.AwaitStmt a = new Ast.AwaitStmt();

                while (true) {
                    Token n = this.expectIdent("имя асинхронной ветки");
                    a.names.add(n.text);
                    a.spans.add(new int[]{n.start, n.end});
                    if (this.peek().type != TokenType.COMMA) {
                        this.expectEnd();
                        return a.at(t.start, this.prev().end);
                    }

                    this.next();
                }
            }
        }
    }

    private Ast.OnStmt onStmt() {
        Token kw = this.next();
        Token ev = this.peek();
        if (ev.type != TokenType.IDENT && ev.type != TokenType.KEYWORD) {
            throw this.fail("После on ожидалось событие: button, key, click, guiclose, chat, near...", ev);
        } else {
            CommandSpec spec = Specs.command("wait " + ev.text);
            if (spec == null) {
                StringBuilder subs = new StringBuilder();

                for (CommandSpec s : Specs.withHead("wait")) {
                    if (s.sub != null) {
                        subs.append(subs.length() > 0 ? ", " : "").append(s.sub);
                    }
                }

                throw this.fail("Неизвестное событие '" + ev.text + "'. Доступно: " + subs, ev);
            } else {
                this.next();
                Ast.OnStmt on = new Ast.OnStmt();
                Ast.CommandStmt cs = new Ast.CommandStmt();
                cs.key = spec.key;
                cs.nameStart = ev.start;
                cs.nameEnd = ev.end;
                this.commandArgs(cs, spec);
                on.event = cs.at(ev.start, this.prev().end);
                on.eventStart = ev.start;
                on.eventEnd = ev.end;
                on.body.addAll(this.block());
                return on.at(kw.start, this.prev().end);
            }
        }
    }

    private Ast.SwitchStmt switchStmt() {
        Token kw = this.next();
        Ast.SwitchStmt sw = new Ast.SwitchStmt();
        sw.value = this.expression();
        this.expect(TokenType.LBRACE, "{");
        this.skipNewlines();

        for (; this.peek().type != TokenType.RBRACE; this.skipNewlines()) {
            if (this.peek().type == TokenType.EOF) {
                throw this.fail("switch не закрыт: не хватает }", this.peek());
            }

            try {
                if (this.peek().isWord("else")) {
                    Token e = this.next();
                    if (sw.otherwise != null) {
                        this.error("else уже есть в этом switch", e.start, e.end);
                    }

                    sw.otherwise = new ArrayList<>(this.block());
                } else {
                    Ast.SwitchCase c = new Ast.SwitchCase();
                    int s = this.peek().start;

                    while (true) {
                        c.values.add(this.expression());
                        if (this.peek().type != TokenType.COMMA) {
                            c.body.addAll(this.block());
                            sw.cases.add(c.at(s, this.prev().end));
                            break;
                        }

                        this.next();
                    }
                }
            } catch (Parser.ParseError var5) {
                this.recover();
            }
        }

        this.next();
        if (sw.cases.isEmpty()) {
            this.error("В switch нужен хотя бы один вариант: \"значение\" { ... }", kw.start, kw.end);
        }

        return sw.at(kw.start, this.prev().end);
    }

    private Ast.FuncDecl funcDecl() {
        Token kw = this.next();
        Ast.FuncDecl f = new Ast.FuncDecl();
        Token name = this.expectIdent("имя функции");
        f.name = name.text;
        f.nameStart = name.start;
        f.nameEnd = name.end;
        if (this.peek().type == TokenType.LPAREN) {
            this.next();

            while (this.peek().type != TokenType.RPAREN) {
                if (!f.params.isEmpty()) {
                    this.expect(TokenType.COMMA, ", или )");
                }

                Token p = this.expectIdent("имя параметра");
                if (f.params.contains(p.text)) {
                    this.error("Параметр '" + p.text + "' повторяется", p.start, p.end);
                }

                f.params.add(p.text);
            }

            this.next();
        }

        f.body.addAll(this.block());
        return f.at(kw.start, this.prev().end);
    }

    private Ast.CallStmt callRest(Token kw, boolean async) {
        Ast.CallStmt c = new Ast.CallStmt();
        c.async = async;
        Token name = this.expectIdent("имя функции");
        c.name = name.text;
        c.nameStart = name.start;
        c.nameEnd = name.end;
        if (this.peek().type == TokenType.LPAREN) {
            this.next();

            while (this.peek().type != TokenType.RPAREN) {
                if (!c.args.isEmpty()) {
                    this.expect(TokenType.COMMA, ", или )");
                }

                if (this.atStatementEnd()) {
                    throw this.fail("Не закрыта скобка )", this.peek());
                }

                c.args.add(this.expression());
            }

            this.next();
        }

        if (this.peek().isWord("as")) {
            if (!async) {
                throw this.fail("'as' — только для async call", this.peek());
            }

            this.next();
            Token n = this.expectIdent("имя ветки");
            c.as = n.text;
            c.asStart = n.start;
            c.asEnd = n.end;
        }

        this.expectEnd();
        return c.at(kw.start, this.prev().end);
    }

    private Ast.LabelStmt label() {
        Token kw = this.next();
        Token name = this.expectIdent("имя метки");
        if (this.peek().isOp(":")) {
            this.next();
        }

        this.expectEnd();
        Ast.LabelStmt l = new Ast.LabelStmt();
        l.name = name.text;
        return l.at(kw.start, name.end);
    }

    private Ast.GotoStmt gotoStmt() {
        Token kw = this.next();
        Token name = this.expectIdent("имя метки");
        this.expectEnd();
        Ast.GotoStmt g = new Ast.GotoStmt();
        g.name = name.text;
        g.nameStart = name.start;
        g.nameEnd = name.end;
        return g.at(kw.start, name.end);
    }

    private Ast.SetStmt setStmt() {
        Token kw = this.next();
        Token name = this.peek().isWord("player") ? this.next() : this.expectIdent("имя переменной");
        String prop = null;
        int propStart = 0;
        int propEnd = 0;
        if (this.peek().isOp(".") && this.peek().start == name.end && (this.peek(1).type == TokenType.IDENT || this.peek(1).type == TokenType.KEYWORD)) {
            this.next();
            Token p = this.next();
            prop = p.text;
            propStart = p.start;
            propEnd = p.end;
        }

        Ast.Expr index = null;
        if (this.peek().type == TokenType.LBRACKET && this.peek().start == name.end) {
            this.next();
            index = this.expression();
            this.expect(TokenType.RBRACKET, "]");
        }

        Token op = this.peek();
        if (!op.isOp("=") && !op.isOp("+=") && !op.isOp("-=")) {
            throw this.fail("Ожидалось =, += или -=", op);
        } else {
            this.next();
            Ast.SetStmt s = new Ast.SetStmt();
            s.index = index;
            s.prop = prop;
            s.propStart = propStart;
            s.propEnd = propEnd;
            s.name = name.text;
            s.nameStart = name.start;
            s.nameEnd = name.end;
            s.op = op.text;
            s.value = this.expression();
            this.expectEnd();
            return s.at(kw.start, this.prev().end);
        }
    }

    private Ast.IfStmt ifStmt() {
        Token kw = this.next();
        Ast.IfStmt s = new Ast.IfStmt();
        s.cond = this.expression();
        s.then.addAll(this.block());
        int save = this.p;
        this.skipNewlines();
        if (this.peek().isWord("else")) {
            this.next();
            if (this.peek().isWord("if")) {
                List<Ast.Stmt> nested = new ArrayList<>();
                nested.add(this.ifStmt());
                s.otherwise = nested;
            } else {
                s.otherwise = this.block();
            }
        } else {
            this.p = save;
        }

        return s.at(kw.start, this.prev().end);
    }

    private Ast.ChoiceStmt choice() {
        Token kw = this.next();
        Ast.ChoiceStmt c = new Ast.ChoiceStmt();
        Token who = this.expectIdent("персонаж, который задаёт вопрос");
        c.character = who.text;
        c.characterStart = who.start;
        c.characterEnd = who.end;
        c.text = this.primary();
        if (this.peek().isWord("emotion")) {
            this.next();
            c.emotion = this.primary();
        }

        if (this.peek().isWord("anims")) {
            this.next();
            c.anims = this.primary();
        }

        this.expect(TokenType.LBRACE, "{ с вариантами ответа");
        this.skipNewlines();

        for (; this.peek().type != TokenType.RBRACE; this.skipNewlines()) {
            if (this.peek().type == TokenType.EOF) {
                throw this.fail("choice не закрыт: не хватает }", this.peek());
            }

            try {
                Ast.ChoiceOption o = new Ast.ChoiceOption();
                int s = this.peek().start;
                o.text = this.primary();
                if (this.peek().isWord("locked")) {
                    this.next();
                    o.locked = this.primary();
                }

                if (this.peek().isWord("emotion")) {
                    this.next();
                    o.emotion = this.primary();
                }

                if (this.peek().isWord("center")) {
                    this.next();
                    o.center = true;
                }

                if (this.peek().isOp("->")) {
                    this.next();
                    Token target = this.expectIdent("метка");
                    Ast.GotoStmt g = new Ast.GotoStmt();
                    g.name = target.text;
                    g.nameStart = target.start;
                    g.nameEnd = target.end;
                    o.body.add(g.at(target.start, target.end));
                    this.expectEnd();
                } else if (this.peek().type == TokenType.LBRACE) {
                    o.body.addAll(this.block());
                } else {
                    this.expectEnd();
                }

                c.options.add(o.at(s, this.prev().end));
            } catch (Parser.ParseError var8) {
                this.recover();
            }
        }

        this.next();
        if (c.options.isEmpty()) {
            this.error("В choice нужен хотя бы один вариант ответа", kw.start, kw.end);
        }

        return c.at(kw.start, this.prev().end);
    }

    private void noteCopies(Ast.CommandStmt cs) {
        if (cs.key.equals("spawn") && cs.arg("who") instanceof Ast.Ident who && cs.arg("count") instanceof Ast.NumberLit n && n.value > 1.0) {
            this.copies.merge(who.name, Math.min(100, (int)n.value), Math::max);
        }
    }

    private Ast.CommandStmt command() {
        Token head = this.next();
        Ast.CommandStmt cs = new Ast.CommandStmt();
        cs.nameStart = head.start;
        cs.nameEnd = head.end;
        CommandSpec spec = null;
        if (this.peek().type == TokenType.IDENT || this.peek().type == TokenType.KEYWORD) {
            Token second = this.peek();
            Token third = this.peek(1);
            if (third.type == TokenType.IDENT || third.type == TokenType.KEYWORD) {
                spec = Specs.command(head.text + " " + second.text + " " + third.text);
                if (spec != null) {
                    this.next();
                    cs.nameEnd = this.next().end;
                }
            }

            if (spec == null) {
                spec = Specs.command(head.text + " " + second.text);
                if (spec != null) {
                    cs.nameEnd = this.next().end;
                }
            }
        }

        if (spec == null) {
            spec = Specs.command(head.text);
        }

        if (spec == null) {
            if (Specs.isCommandHead(head.text)) {
                StringBuilder subs = new StringBuilder();

                for (CommandSpec s : Specs.withHead(head.text)) {
                    if (s.sub != null) {
                        subs.append(subs.length() > 0 ? ", " : "").append(s.sub);
                    }
                }

                throw this.fail("Неизвестный вариант '" + head.text + "'. Доступно: " + subs, head);
            } else {
                throw this.fail("Неизвестная команда '" + head.text + "'", head);
            }
        } else {
            cs.key = spec.key;
            if (spec.key.equals(head.text) && this.peek().type == TokenType.LPAREN && this.peek().start == head.end) {
                int save = this.p;
                int diags = this.diagnostics.size();

                try {
                    if (this.callArgs(cs, spec)) {
                        return cs.at(head.start, this.prev().end);
                    }
                } catch (Parser.ParseError var7) {
                }

                this.p = save;

                while (this.diagnostics.size() > diags) {
                    this.diagnostics.remove(this.diagnostics.size() - 1);
                }

                cs.args.clear();
            }

            this.commandArgs(cs, spec);
            this.noteCopies(cs);
            return cs.at(head.start, this.prev().end);
        }
    }

    private boolean callArgs(Ast.CommandStmt cs, CommandSpec spec) {
        this.next();
        List<Ast.Expr> values = new ArrayList<>();
        this.skipNewlines();
        if (this.peek().type != TokenType.RPAREN) {
            do {
                this.skipNewlines();
                values.add(this.expression());
                this.skipNewlines();
            } while (this.peek().type == TokenType.COMMA && this.next() != null);
        }

        if (this.peek().type != TokenType.RPAREN) {
            return false;
        } else {
            Token close = this.next();
            if (!this.atStatementEnd()) {
                return false;
            } else {
                Iterator<Ast.Expr> it = values.iterator();

                while (it.hasNext()) {
                    Ast.Expr v = it.next();
                    if (v instanceof Ast.Ident id) {
                        for (CommandSpec.Group g : spec.groups) {
                            if (g.isFlag() && g.keyword.equals(id.name)) {
                                Ast.Arg flag = new Ast.Arg();
                                flag.keyword = g.keyword;
                                flag.param = g.keyword;
                                Ast.BoolLit on = new Ast.BoolLit();
                                on.value = true;
                                flag.value = on.at(v.start, v.end);
                                cs.args.add(flag.at(v.start, v.end));
                                it.remove();
                                break;
                            }
                        }
                    }
                }

                int i = 0;

                for (CommandSpec.Group gx : spec.groups) {
                    if (!gx.isFlag()) {
                        if (i >= values.size()) {
                            if (!gx.optional) {
                                this.error("Не хватает <" + gx.params.get(0).name + ">. Синтаксис: " + spec.signature(), close.start, close.end);
                            }
                        } else {
                            for (CommandSpec.Param param : gx.params) {
                                if (param.vararg) {
                                    while (i < values.size()) {
                                        cs.args.add(callArg(gx.keyword, param, values.get(i++)));
                                    }
                                } else if (i < values.size()) {
                                    cs.args.add(callArg(gx.keyword, param, values.get(i++)));
                                } else {
                                    this.error("Не хватает <" + param.name + ">. Синтаксис: " + spec.signature(), close.start, close.end);
                                }
                            }
                        }
                    }
                }

                return i == values.size();
            }
        }
    }

    private static Ast.Arg callArg(String keyword, CommandSpec.Param param, Ast.Expr value) {
        Ast.Arg a = new Ast.Arg();
        a.keyword = keyword;
        a.param = param.name;
        a.value = value;
        return a.at(value.start, value.end);
    }

    private void commandArgs(Ast.CommandStmt cs, CommandSpec spec) {
        List<CommandSpec.Group> groups = spec.groups;
        boolean[] used = new boolean[groups.size()];

        while (!this.atStatementEnd()) {
            Token t = this.peek();
            int gi = -1;
            if (t.type == TokenType.IDENT || t.type == TokenType.KEYWORD) {
                for (int i = 0; i < groups.size(); i++) {
                    if (!used[i] && t.text.equals(groups.get(i).keyword)) {
                        gi = i;
                        break;
                    }
                }
            }

            if (gi >= 0) {
                this.next();
            } else {
                for (int ix = 0; ix < groups.size(); ix++) {
                    if (!used[ix] && groups.get(ix).keyword == null) {
                        gi = ix;
                        break;
                    }
                }

                if (gi < 0) {
                    throw this.fail("Лишний аргумент для '" + spec.key + "'. Синтаксис: " + spec.signature(), t);
                }
            }

            used[gi] = true;
            CommandSpec.Group g = groups.get(gi);
            if (g.isFlag()) {
                Ast.Arg flag = new Ast.Arg();
                flag.keyword = g.keyword;
                flag.param = g.keyword;
                Ast.BoolLit on = new Ast.BoolLit();
                on.value = true;
                flag.value = on.at(this.prev().start, this.prev().end);
                cs.args.add(flag.at(this.prev().start, this.prev().end));
            } else {
                for (CommandSpec.Param param : g.params) {
                    if (param.vararg) {
                        int count;
                        for (count = 0; !this.atStatementEnd() && !this.isUnusedKeyword(groups, used); count++) {
                            cs.args.add(this.arg(g.keyword, param));
                        }

                        if (count == 0) {
                            throw this.fail("Ожидалось значение <" + param.name + ">", this.peek());
                        }
                    } else {
                        if (this.atStatementEnd()) {
                            throw this.fail("Не хватает аргумента <" + param.name + "> (" + param.type.title + ")", this.peek());
                        }

                        cs.args.add(this.arg(g.keyword, param));
                    }
                }
            }
        }

        for (int ixx = 0; ixx < groups.size(); ixx++) {
            CommandSpec.Group g = groups.get(ixx);
            if (!used[ixx] && !g.optional) {
                String what = g.keyword != null ? "'" + g.keyword + " <" + g.params.get(0).name + ">'" : "<" + g.params.get(0).name + ">";
                Token at = this.peek();
                this.error("Не хватает " + what + ". Синтаксис: " + spec.signature(), Math.max(0, at.start - 1), at.start + 1);
            }
        }
    }

    private boolean isUnusedKeyword(List<CommandSpec.Group> groups, boolean[] used) {
        Token t = this.peek();
        if (t.type != TokenType.IDENT && t.type != TokenType.KEYWORD) {
            return false;
        } else {
            for (int i = 0; i < groups.size(); i++) {
                if (!used[i] && t.text.equals(groups.get(i).keyword)) {
                    return true;
                }
            }

            return false;
        }
    }

    private Ast.Arg arg(String keyword, CommandSpec.Param param) {
        Ast.Arg a = new Ast.Arg();
        a.keyword = keyword;
        a.param = param.name;
        int s = this.peek().start;
        a.value = param.type == ParamType.EXPR ? this.expression() : this.unary();
        return a.at(s, this.prev().end);
    }

    public Ast.Expr expression() {
        return this.or();
    }

    private Ast.Expr or() {
        Ast.Expr left;
        for (left = this.and(); this.peek().isOp("||") || this.peek().isWord("or"); left = this.bin("or", left, this.and())) {
            this.next();
        }

        return left;
    }

    private Ast.Expr and() {
        Ast.Expr left;
        for (left = this.not(); this.peek().isOp("&&") || this.peek().isWord("and"); left = this.bin("and", left, this.not())) {
            this.next();
        }

        return left;
    }

    private Ast.Expr not() {
        if (this.peek().isWord("not")) {
            Token t = this.next();
            Ast.Unary u = new Ast.Unary();
            u.op = "!";
            u.operand = this.not();
            return u.at(t.start, u.operand.end);
        } else {
            return this.comparison();
        }
    }

    private Ast.Expr comparison() {
        Ast.Expr left = this.additive();

        while (true) {
            Token t = this.peek();
            if (!t.isOp("==") && !t.isOp("!=") && !t.isOp("<") && !t.isOp("<=") && !t.isOp(">") && !t.isOp(">=")) {
                return left;
            }

            this.next();
            left = this.bin(t.text, left, this.additive());
        }
    }

    private Ast.Expr additive() {
        Ast.Expr left = this.multiplicative();

        while (this.peek().isOp("+") || this.peek().isOp("-")) {
            String op = this.next().text;
            left = this.bin(op, left, this.multiplicative());
        }

        return left;
    }

    private Ast.Expr multiplicative() {
        Ast.Expr left = this.unary();

        while (this.peek().isOp("*") || this.peek().isOp("/") || this.peek().isOp("%")) {
            String op = this.next().text;
            left = this.bin(op, left, this.unary());
        }

        return left;
    }

    private Ast.Expr bin(String op, Ast.Expr l, Ast.Expr r) {
        Ast.Binary b = new Ast.Binary();
        b.op = op;
        b.left = l;
        b.right = r;
        return b.at(l.start, r.end);
    }

    private Ast.Expr unary() {
        Token t = this.peek();
        if (!t.isOp("-") && !t.isOp("!")) {
            return this.primary();
        } else {
            this.next();
            Ast.Expr operand = this.unary();
            if (t.isOp("-") && operand instanceof Ast.NumberLit n) {
                n.value = -n.value;
                return n.at(t.start, n.end);
            } else {
                Ast.Unary u = new Ast.Unary();
                u.op = t.text;
                u.operand = operand;
                return u.at(t.start, operand.end);
            }
        }
    }

    private Ast.Expr primary() {
        Ast.Expr e = this.atom();

        while (true) {
            Token t = this.peek();
            if (t.type != TokenType.LBRACKET || t.start != this.prev().end) {
                if (!t.isOp(".") || t.start != this.prev().end || this.peek(1).type != TokenType.IDENT && this.peek(1).type != TokenType.KEYWORD) {
                    return e;
                }

                this.next();
                Token n = this.next();
                Ast.Member m = new Ast.Member();
                m.target = e;
                m.name = n.text;
                m.nameStart = n.start;
                m.nameEnd = n.end;
                if (this.peek().type == TokenType.LPAREN && this.peek().start == n.end) {
                    this.next();
                    m.args = new ArrayList<>();
                    if (this.peek().type != TokenType.RPAREN) {
                        do {
                            m.args.add(this.expression());
                        } while (this.peek().type == TokenType.COMMA && this.next() != null);
                    }

                    this.expect(TokenType.RPAREN, ")");
                }

                e = m.at(e.start, this.prev().end);
            } else {
                this.next();
                Ast.Index ix = new Ast.Index();
                ix.target = e;
                ix.index = this.expression();
                Token close = this.expect(TokenType.RBRACKET, "]");
                e = ix.at(e.start, close.end);
            }
        }
    }

    private Ast.Expr atom() {
        Token t = this.peek();
        switch (t.type) {
            case NUMBER:
                this.next();
                Ast.NumberLit n = new Ast.NumberLit();
                n.value = (Double)t.value;
                return n.at(t.start, t.end);
            case DURATION:
                this.next();
                Ast.DurationLit d = new Ast.DurationLit();
                d.ticks = (Integer)t.value;
                return d.at(t.start, t.end);
            case STRING:
                this.next();
                Ast.StringLit s = new Ast.StringLit();
                s.value = (String)t.value;
                return s.at(t.start, t.end);
            case KEYWORD:
                if (!t.text.equals("true") && !t.text.equals("false")) {
                    if (t.text.equals("player")) {
                        this.next();
                        Ast.Ident id = new Ast.Ident();
                        id.name = "player";
                        return id.at(t.start, t.end);
                    }

                    throw this.fail("Ожидалось значение, а не '" + t.text + "'", t);
                }

                this.next();
                Ast.BoolLit b = new Ast.BoolLit();
                b.value = t.text.equals("true");
                return b.at(t.start, t.end);
            case IDENT:
                this.next();
                if (this.peek().type == TokenType.LPAREN && this.peek().start == t.end) {
                    this.next();
                    Ast.Call c = new Ast.Call();
                    c.name = t.text;
                    c.nameStart = t.start;
                    c.nameEnd = t.end;
                    if (this.peek().type != TokenType.RPAREN) {
                        do {
                            c.args.add(this.expression());
                        } while (this.peek().type == TokenType.COMMA && this.next() != null);
                    }

                    Token close = this.expect(TokenType.RPAREN, ")");
                    return c.at(t.start, close.end);
                } else {
                    Ast.Ident id = new Ast.Ident();
                    id.name = t.text;
                    return id.at(t.start, t.end);
                }
            case LPAREN: {
                this.next();
                Ast.Expr first = this.expression();
                if (this.peek().type != TokenType.COMMA) {
                    Token close = this.expect(TokenType.RPAREN, ")");
                    return first.at(t.start, close.end);
                }

                Ast.Tuple tuple = new Ast.Tuple();
                tuple.items.add(first);

                while (this.peek().type == TokenType.COMMA) {
                    this.next();
                    tuple.items.add(this.expression());
                }

                Token close = this.expect(TokenType.RPAREN, ")");
                return tuple.at(t.start, close.end);
            }
            case LBRACKET: {
                this.next();
                Ast.ListLit list = new Ast.ListLit();
                this.skipNewlines();
                if (this.peek().type != TokenType.RBRACKET) {
                    do {
                        this.skipNewlines();
                        list.items.add(this.expression());
                        this.skipNewlines();
                    } while (this.peek().type == TokenType.COMMA && this.next() != null);
                }

                this.skipNewlines();
                Token close = this.expect(TokenType.RBRACKET, "]");
                return list.at(t.start, close.end);
            }
            case LBRACE: {
                this.next();
                Ast.MapLit map = new Ast.MapLit();
                this.skipNewlines();
                if (this.peek().type != TokenType.RBRACE) {
                    do {
                        this.skipNewlines();
                        Token k = this.peek();
                        Ast.Expr key;
                        if (k.type != TokenType.IDENT && k.type != TokenType.KEYWORD) {
                            key = this.atom();
                        } else {
                            this.next();
                            Ast.StringLit sx = new Ast.StringLit();
                            sx.value = k.text;
                            key = sx.at(k.start, k.end);
                        }

                        if (!this.peek().isOp(":")) {
                            throw this.fail("Ожидалось ':' — {ключ: значение}", this.peek());
                        }

                        this.next();
                        this.skipNewlines();
                        map.keys.add(key);
                        map.values.add(this.expression());
                        this.skipNewlines();
                    } while (this.peek().type == TokenType.COMMA && this.next() != null);
                }

                this.skipNewlines();
                Token close = this.expect(TokenType.RBRACE, "}");
                return map.at(t.start, close.end);
            }
            default:
                throw this.fail("Ожидалось значение", t);
        }
    }

    private static final class ParseError extends RuntimeException {
        ParseError() {
            super(null, null, false, false);
        }
    }

    public static final class Result {
        public final Ast.Program program;
        public final List<Diagnostic> diagnostics;
        public final List<Token> tokens;

        Result(Ast.Program program, List<Diagnostic> diagnostics, List<Token> tokens) {
            this.program = program;
            this.diagnostics = diagnostics;
            this.tokens = tokens;
        }

        public boolean hasErrors() {
            for (Diagnostic d : this.diagnostics) {
                if (d.isError()) {
                    return true;
                }
            }

            return false;
        }
    }
}
