package org.sprautestudio.spraute_engine.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import org.sprautestudio.spraute_engine.build.BuildStyles;
import org.sprautestudio.spraute_engine.camera.CamProfile;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.editor.assets.AssetIndex;
import org.sprautestudio.spraute_engine.fight.FightDef;
import org.sprautestudio.spraute_engine.fx.FxPresets;
import org.sprautestudio.spraute_engine.gui.PanelLook;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.Diagnostic;
import org.sprautestudio.spraute_engine.lang.Lexer;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.lang.Token;
import org.sprautestudio.spraute_engine.lang.TokenType;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;
import org.sprautestudio.spraute_engine.player.SlotPlan;
import org.sprautestudio.spraute_engine.post.PostParams;
import org.sprautestudio.spraute_engine.post.PostPresets;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.stealth.StealthDef;
import org.sprautestudio.spraute_engine.stealth.StealthShape;
import org.sprautestudio.spraute_engine.ui.code.LanguageSupport;

public final class SstLanguage implements LanguageSupport {
    public Supplier<String> positionProvider;
    public boolean clip;
    public boolean body;
    public Ast.Program clipStory;
    public Supplier<List<Integer>> knownScripts = List::of;
    private static List<String> sounds;
    private static List<String> items;
    private static List<String> effects;
    private static List<String> entities;
    private static Analyzer.Registries registries;

    private static void loadRegistries() {
        if (sounds == null) {
            sounds = Regs.SOUND_EVENT.keySet().stream().<String>map(ResourceLocation::toString).sorted().collect(Collectors.toList());
            items = Regs.ITEM.keySet().stream().<String>map(ResourceLocation::toString).sorted().collect(Collectors.toList());
            effects = Regs.MOB_EFFECT.keySet().stream().<String>map(ResourceLocation::toString).sorted().collect(Collectors.toList());
            entities = Regs.ENTITY_TYPE.keySet().stream().<String>map(ResourceLocation::toString).sorted().collect(Collectors.toList());
            Set<String> s = new HashSet<>(sounds);
            Set<String> i = new HashSet<>(items);
            Set<String> e = new HashSet<>(effects);
            Set<String> en = new HashSet<>(entities);
            registries = new Analyzer.Registries();
            registries.sounds = s::contains;
            registries.items = i::contains;
            registries.effects = e::contains;
            registries.entities = en::contains;
            registries.models = name -> EntityLibrary.find(null, name) != null || ModelLibrary.find(null, name) != null;
            registries.guis = name -> AssetIndex.projectGuis.get().containsKey(name);
            registries.guiElements = id -> guiElementIds().contains(id);
            registries.images = name -> AssetIndex.projectImages.get().contains(name);
            registries.projectSounds = name -> AssetIndex.projectSounds.get().contains(name);
            registries.animations = id -> customAnimation(id) || Anims.all().isEmpty() || Anims.exists(id);
        }
    }

    private static List<String> animationIds() {
        List<String> ids = new ArrayList<>();

        for (ModelLibrary.ProjectAnim a : ModelLibrary.projectAnimations(null)) {
            ids.add(a.shortId());
        }

        for (Anims.Entry e : Anims.all()) {
            ids.add(e.id());
        }

        return ids;
    }

    @Override
    public LanguageSupport.Analysis analyze(String text) {
        return new SstLanguage.SstAnalysis(text, this.clip, this.body, this.clipStory);
    }

    private static Analyzer.Result result(LanguageSupport.Analysis a) {
        return a instanceof SstLanguage.SstAnalysis s ? s.result : Analyzer.analyze("");
    }

    static int[] highlight(String text, Analyzer.Result r) {
        int[] colors = new int[text.length()];
        Arrays.fill(colors, -2696470);
        List<Token> tokens = Lexer.tokenizeAll(text);
        boolean statementStart = true;
        String head = null;
        CommandSpec spec = null;
        int tokenInStatement = 0;
        boolean scriptLine = false;

        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            int color;
            switch (t.type) {
                case COMMENT:
                    color = -9998699;
                    break;
                case STRING:
                    color = -3938163;
                    break;
                case NUMBER:
                case DURATION:
                    color = -553876;
                    break;
                case KEYWORD:
                    color = tokenInStatement == 1 && head != null && Specs.command(head + " " + t.text) != null
                        ? -8213761
                        : (
                            !t.text.equals("true") && !t.text.equals("false") && !t.text.equals("player")
                                ? (!t.text.equals("and") && !t.text.equals("or") && !t.text.equals("not") ? -3697942 : -7741953)
                                : -553876
                        );
                    break;
                case OPERATOR:
                case LPAREN:
                case RPAREN:
                case LBRACKET:
                case RBRACKET:
                case COMMA:
                case LBRACE:
                case RBRACE:
                    color = -7741953;
                    break;
                case ERROR:
                    color = -44176;
                    break;
                case IDENT:
                    color = -2696470;
                    Token next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
                    if (statementStart && Specs.isCommandHead(t.text)) {
                        color = -8213761;
                        head = t.text;
                        spec = Specs.command(t.text);
                    } else if (tokenInStatement == 1 && head != null && Specs.command(head + " " + t.text) != null) {
                        color = -8213761;
                        spec = Specs.command(head + " " + t.text);
                    } else if (next != null && next.type == TokenType.LPAREN && next.start == t.end && Specs.function(t.text) != null) {
                        color = -10880;
                    } else if (r.characters.containsKey(t.text)) {
                        color = -11544898;
                    } else if (r.locations.containsKey(t.text)) {
                        color = -13461;
                    } else {
                        if (!r.labels.containsKey(t.text) && !r.branches.containsKey(t.text)) {
                            if (r.functions.containsKey(t.text)) {
                                color = -10880;
                                break;
                            }

                            if (r.vars.containsKey(t.text)) {
                                color = -8531969;
                                break;
                            }

                            if (spec != null && isGroupKeyword(spec, t.text)) {
                                color = -6642226;
                                break;
                            }

                            if (!t.text.equals("as") || tokenInStatement < 2 && !statementStart) {
                                if (!scriptLine || !t.text.equals("solo") && !t.text.equals("autostart") && !t.text.equals("once")) {
                                    if (t.text.equals("actor")) {
                                        color = -553876;
                                        break;
                                    }

                                    if (t.text.equals("name")
                                        || t.text.equals("speed")
                                        || t.text.equals("skin")
                                        || t.text.equals("existing")
                                        || t.text.equals("yaw")
                                        || t.text.equals("pitch")
                                        || t.text.equals("dim")
                                        || t.text.equals("emotion")) {
                                        color = -6642226;
                                    }
                                    break;
                                }

                                color = -3697942;
                                break;
                            }

                            color = -3697942;
                            break;
                        }

                        color = -33098;
                    }
                    break;
                default:
                    color = -2696470;
            }

            if (t.type != TokenType.NEWLINE && t.type != TokenType.EOF) {
                for (int k = t.start; k < t.end && k < colors.length; k++) {
                    colors[k] = color;
                }

                if (t.type == TokenType.STRING) {
                    highlightInterpolation(text, t, colors);
                }
            }

            if (t.type != TokenType.COMMENT) {
                if (t.type != TokenType.NEWLINE && t.type != TokenType.LBRACE && t.type != TokenType.RBRACE) {
                    if (statementStart && t.text.equals("script")) {
                        scriptLine = true;
                    }

                    statementStart = false;
                    tokenInStatement++;
                } else {
                    statementStart = true;
                    head = null;
                    spec = null;
                    tokenInStatement = 0;
                    scriptLine = false;
                }
            }
        }

        return colors;
    }

    private static void highlightInterpolation(String text, Token t, int[] colors) {
        int depth = -1;

        for (int k = t.start + 1; k < t.end - 1; k++) {
            char ch = text.charAt(k);
            if (ch == '\\') {
                k++;
            } else {
                if (ch == '{') {
                    depth = k;
                }

                if (depth >= 0) {
                    colors[k] = -13461;
                }

                if (ch == '}') {
                    depth = -1;
                }
            }
        }
    }

    private static boolean isGroupKeyword(CommandSpec spec, String word) {
        for (CommandSpec.Group g : spec.groups) {
            if (word.equals(g.keyword)) {
                return true;
            }
        }

        return false;
    }

    private static SstLanguage.Context context(String text, int caret) {
        SstLanguage.Context ctx = new SstLanguage.Context();
        int ls = text.lastIndexOf(10, Math.max(0, caret - 1)) + 1;
        String line = text.substring(ls, caret);
        List<Token> lineTokens = Lexer.tokenizeAll(line);
        List<Token> stmt = new ArrayList<>();

        for (Token t : lineTokens) {
            if (t.type != TokenType.EOF && t.type != TokenType.NEWLINE) {
                if (t.type != TokenType.LBRACE && t.type != TokenType.RBRACE) {
                    stmt.add(t);
                } else {
                    stmt.clear();
                }
            }
        }

        if (!stmt.isEmpty()) {
            Token last = stmt.get(stmt.size() - 1);
            if (last.type == TokenType.ERROR && last.text.startsWith("\"")) {
                ctx.inString = true;
                ctx.stringContentStart = ls + last.start + 1;
                ctx.typed = line.substring(last.start + 1);
                ctx.wordStart = ctx.stringContentStart;
                stmt.remove(stmt.size() - 1);
                ctx.tokens = stmt;
                resolveSpec(ctx);
                return ctx;
            }
        }

        int ws = caret;

        while (ws > ls && isWord(text.charAt(ws - 1))) {
            ws--;
        }

        ctx.wordStart = ws;
        ctx.typed = text.substring(ws, caret);
        if (!stmt.isEmpty() && !ctx.typed.isEmpty()) {
            Token last = stmt.get(stmt.size() - 1);
            if (ls + last.end == caret) {
                stmt.remove(stmt.size() - 1);
            }
        }

        ctx.tokens = stmt;
        resolveSpec(ctx);
        return ctx;
    }

    private static void resolveSpec(SstLanguage.Context ctx) {
        List<Token> t = ctx.tokens;
        if (!t.isEmpty() && t.get(0).type == TokenType.IDENT) {
            if (t.size() >= 2 && (t.get(1).type == TokenType.IDENT || t.get(1).type == TokenType.KEYWORD)) {
                CommandSpec s = Specs.command(t.get(0).text + " " + t.get(1).text);
                if (s != null) {
                    ctx.spec = s;
                    ctx.specTokens = 2;
                    return;
                }
            }

            ctx.spec = Specs.command(t.get(0).text);
            ctx.specTokens = 1;
        }
    }

    private static boolean isWord(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private static SstLanguage.ArgState argState(CommandSpec spec, List<Token> args) {
        List<Token> values = new ArrayList<>();
        int depth = 0;

        for (Token t : args) {
            if (t.type != TokenType.LPAREN && t.type != TokenType.LBRACKET) {
                if (t.type == TokenType.RPAREN || t.type == TokenType.RBRACKET) {
                    depth--;
                } else if (depth == 0 && (t.type != TokenType.OPERATOR || !t.text.equals("-"))) {
                    values.add(t);
                }
            } else {
                if (depth == 0) {
                    values.add(t);
                }

                depth++;
            }
        }

        List<CommandSpec.Group> groups = spec.groups;
        boolean[] used = new boolean[groups.size()];
        CommandSpec.Group current = null;
        int paramIdx = 0;
        int currentGroupIndex = -1;

        for (Token v : values) {
            int kw = -1;
            if (v.type == TokenType.IDENT || v.type == TokenType.KEYWORD) {
                for (int i = 0; i < groups.size(); i++) {
                    if (!used[i] && v.text.equals(groups.get(i).keyword)) {
                        kw = i;
                        break;
                    }
                }
            }

            if (kw >= 0) {
                used[kw] = true;
                current = groups.get(kw);
                currentGroupIndex = kw;
                paramIdx = 0;
            } else if (current != null && paramIdx < current.params.size()) {
                if (!current.params.get(paramIdx).vararg) {
                    if (++paramIdx >= current.params.size()) {
                        current = null;
                    }
                }
            } else {
                current = null;

                for (int ix = 0; ix < groups.size(); ix++) {
                    if (!used[ix] && groups.get(ix).keyword == null) {
                        used[ix] = true;
                        currentGroupIndex = ix;
                        if (groups.get(ix).params.get(0).vararg) {
                            current = groups.get(ix);
                            paramIdx = 0;
                        }
                        break;
                    }
                }
            }
        }

        CommandSpec.Param next = null;
        String needKw = null;
        int active = -1;
        if (current != null && paramIdx < current.params.size()) {
            next = current.params.get(paramIdx);
            active = currentGroupIndex;
        } else {
            for (int ixx = 0; ixx < groups.size(); ixx++) {
                if (!used[ixx]) {
                    CommandSpec.Group g = groups.get(ixx);
                    if (g.keyword == null) {
                        next = g.params.get(0);
                        active = ixx;
                        break;
                    }

                    if (!g.optional) {
                        needKw = g.keyword;
                        active = ixx;
                        break;
                    }
                }
            }
        }

        List<String> free = new ArrayList<>();
        boolean requiredLeft = false;

        for (int ixxx = 0; ixxx < groups.size(); ixxx++) {
            if (!used[ixxx] && !groups.get(ixxx).optional) {
                requiredLeft = true;
            }
        }

        for (int ixxxx = 0; ixxxx < groups.size(); ixxxx++) {
            if (!used[ixxxx] && groups.get(ixxxx).keyword != null && (!groups.get(ixxxx).optional || !requiredLeft || next == null)) {
                free.add(groups.get(ixxxx).keyword);
            }
        }

        return new SstLanguage.ArgState(next, needKw, free, active);
    }

    @Override
    public List<LanguageSupport.Completion> complete(String text, int caret, LanguageSupport.Analysis analysis, boolean explicit) {
        Analyzer.Result r = result(analysis);
        SstLanguage.Context ctx = context(text, caret);
        List<LanguageSupport.Completion> out = new ArrayList<>();
        int rs = ctx.wordStart;
        String typed = ctx.typed;
        if (ctx.inString) {
            ParamType type = stringParamType(ctx);
            if (type == null) {
                return List.of();
            } else {
                loadRegistries();

                List<String> ids = switch (type) {
                    case SOUND -> sounds;
                    case ITEM -> items;
                    case EFFECT -> effects;
                    case ENTITY -> {
                        List<String> l = new ArrayList<>();

                        for (EntityLibrary.Type t : EntityLibrary.all()) {
                            l.add("spraute:" + t.name());
                        }

                        l.addAll(merge(Specs.ST_ENTITIES, entities));
                        yield l;
                    }
                    case ANIM -> animationIds();
                    case GUI -> new ArrayList<>(AssetIndex.projectGuis.get().keySet());
                    case GUIEL -> guiElementIds();
                    case IMAGE -> AssetIndex.imageChoices();
                    default -> List.of();
                };

                for (String id : ids) {
                    out.add(new LanguageSupport.Completion(id, LanguageSupport.Kind.VALUE, type.title, id, null, rs, 1));
                }

                return filter(out, typed, 200);
            }
        } else {
            List<Token> toks = ctx.tokens;
            if (toks.isEmpty()) {
                if (typed.isEmpty() && !explicit) {
                    return List.of();
                } else {
                    this.statementCompletions(out, rs, r);
                    return filter(out, typed, 80);
                }
            } else {
                Token first = toks.get(0);
                if (first.type == TokenType.KEYWORD) {
                    String var22 = first.text;
                    switch (var22) {
                        case "script":
                            if (toks.size() >= 2) {
                                Set<String> written = new HashSet<>();

                                for (Token w : toks) {
                                    written.add(w.text);
                                }

                                if (!written.contains("solo")) {
                                    out.add(
                                        new LanguageSupport.Completion(
                                            "solo", LanguageSupport.Kind.KEYWORD, "у каждого игрока свой", "solo", Specs.keywordDocs().get("solo"), rs, 0
                                        )
                                    );
                                }

                                if (!written.contains("autostart")) {
                                    out.add(
                                        new LanguageSupport.Completion(
                                            "autostart",
                                            LanguageSupport.Kind.KEYWORD,
                                            "сам при входе",
                                            "autostart",
                                            Specs.keywordDocs().get("autostart"),
                                            rs,
                                            0
                                        )
                                    );
                                    out.add(
                                        new LanguageSupport.Completion(
                                            "autostart once",
                                            LanguageSupport.Kind.KEYWORD,
                                            "сам один раз",
                                            "autostart once",
                                            Specs.keywordDocs().get("once"),
                                            rs,
                                            1
                                        )
                                    );
                                } else if (!written.contains("once") && toks.get(toks.size() - 1).text.equals("autostart")) {
                                    out.add(
                                        new LanguageSupport.Completion(
                                            "once", LanguageSupport.Kind.KEYWORD, "только первый вход", "once", Specs.keywordDocs().get("once"), rs, 0
                                        )
                                    );
                                }
                            }
                            break;
                        case "goto":
                            if (toks.size() == 1) {
                                for (Analyzer.Symbol sx : r.labels.values()) {
                                    out.add(this.sym(sx, rs));
                                }
                            }
                            break;
                        case "call":
                            if (toks.size() == 1) {
                                this.functionCompletions(out, rs, r);
                            } else {
                                this.exprCompletions(out, rs, r);
                            }
                            break;
                        case "async":
                            if (toks.size() == 1) {
                                out.add(
                                    new LanguageSupport.Completion(
                                        "call", LanguageSupport.Kind.KEYWORD, "функция в фоне", "call ", Specs.keywordDocs().get("async"), rs, 0
                                    )
                                );
                                out.add(
                                    new LanguageSupport.Completion(
                                        "{ }", LanguageSupport.Kind.KEYWORD, "фоновая ветка", "{\n    $0\n}", "Анонимная асинхронная ветка", rs, 1
                                    )
                                );
                                out.add(
                                    new LanguageSupport.Completion(
                                        "имя { }",
                                        LanguageSupport.Kind.KEYWORD,
                                        "ветка с именем",
                                        "${1:ветка} {\n    $0\n}",
                                        "Ветка с именем — её можно ждать (await) и отменять (cancel)",
                                        rs,
                                        1
                                    )
                                );
                            } else if (toks.size() == 2 && toks.get(1).text.equals("call")) {
                                this.functionCompletions(out, rs, r);
                            } else if (toks.size() >= 3 && toks.get(1).text.equals("call")) {
                                out.add(
                                    new LanguageSupport.Completion(
                                        "as", LanguageSupport.Kind.KEYWORD, "имя ветки", "as ${1:job}", "Дать фоновому вызову имя для await/cancel", rs, 0
                                    )
                                );
                                this.exprCompletions(out, rs, r);
                            }
                            break;
                        case "on":
                            if (toks.size() == 1) {
                                for (CommandSpec s : Specs.withHead("wait")) {
                                    if (s.sub != null && !s.sub.equals("random") && !s.sub.equals("until")) {
                                        String rest = s.snippet().substring(s.head.length() + 1);
                                        out.add(
                                            new LanguageSupport.Completion(
                                                s.sub,
                                                LanguageSupport.Kind.KEYWORD,
                                                "событие",
                                                rest + " {\n    $0\n}",
                                                "on " + s.signature().substring(5) + "\n" + s.doc,
                                                rs,
                                                0
                                            )
                                        );
                                    }
                                }
                            }
                            break;
                        case "switch":
                            if (toks.size() == 1) {
                                this.exprCompletions(out, rs, r);
                            }
                            break;
                        case "await":
                        case "cancel":
                            for (Analyzer.Symbol s : r.branches.values()) {
                                out.add(this.sym(s, rs));
                            }
                            break;
                        case "set":
                            if (toks.size() == 1) {
                                for (Analyzer.Symbol s : r.vars.values()) {
                                    out.add(this.sym(s, rs));
                                }
                            } else if (toks.size() >= 3) {
                                this.exprCompletions(out, rs, r);
                            } else if (toks.size() == 2) {
                                out.add(new LanguageSupport.Completion("=", LanguageSupport.Kind.KEYWORD, null, "= ", null, rs, 1));
                                out.add(new LanguageSupport.Completion("+=", LanguageSupport.Kind.KEYWORD, null, "+= ", null, rs, 1));
                                out.add(new LanguageSupport.Completion("-=", LanguageSupport.Kind.KEYWORD, null, "-= ", null, rs, 1));
                            }
                            break;
                        case "if":
                        case "while":
                        case "repeat":
                            this.exprCompletions(out, rs, r);
                            break;
                        case "choice":
                            if (toks.size() == 1) {
                                for (Analyzer.Symbol s : r.characters.values()) {
                                    out.add(this.sym(s, rs));
                                }
                            } else if (toks.size() == 3) {
                                out.add(
                                    new LanguageSupport.Completion(
                                        "emotion", LanguageSupport.Kind.KEYWORD, "эмоция", "emotion ", "Эмоция говорящего в диалоге", rs, 1
                                    )
                                );
                            } else if (toks.size() == 4 && toks.get(3).text.equals("emotion")) {
                                emotions(out, rs);
                            }
                            break;
                        case "character":
                            if (toks.size() == 3) {
                                out.add(
                                    new LanguageSupport.Completion(
                                        "existing",
                                        LanguageSupport.Kind.KEYWORD,
                                        "готовый персонаж ST",
                                        "existing \"${1:id}\"",
                                        "Использовать персонажа, уже описанного в Spraute",
                                        rs,
                                        0
                                    )
                                );

                                for (String e : Specs.ST_ENTITIES) {
                                    out.add(
                                        new LanguageSupport.Completion("\"" + e + "\"", LanguageSupport.Kind.VALUE, "сущность", "\"" + e + "\"", null, rs, 1)
                                    );
                                }
                            } else if (toks.size() >= 4) {
                                out.add(
                                    new LanguageSupport.Completion(
                                        "name", LanguageSupport.Kind.KEYWORD, "имя", "name \"${1:Имя}\"", "Отображаемое имя персонажа", rs, 0
                                    )
                                );
                                out.add(
                                    new LanguageSupport.Completion(
                                        "speed", LanguageSupport.Kind.KEYWORD, "скорость", "speed ${1:0.3}", "Стандартная скорость ходьбы", rs, 0
                                    )
                                );
                                out.add(
                                    new LanguageSupport.Completion(
                                        "skin",
                                        LanguageSupport.Kind.KEYWORD,
                                        "текстура",
                                        "skin \"${1:spraute_engine:textures/entity/humanoid.png}\"",
                                        "Текстура персонажа",
                                        rs,
                                        0
                                    )
                                );

                                for (String ic : AssetIndex.imageChoices()) {
                                    out.add(
                                        new LanguageSupport.Completion(
                                            "icon \"" + ic + "\"",
                                            LanguageSupport.Kind.KEYWORD,
                                            "иконка речи",
                                            "icon \"" + ic + "\"",
                                            "Картинка в окне речи (say)",
                                            rs,
                                            1
                                        )
                                    );
                                    out.add(
                                        new LanguageSupport.Completion(
                                            "portrait \"" + ic + "\"",
                                            LanguageSupport.Kind.KEYWORD,
                                            "лицо в диалоге",
                                            "portrait \"" + ic + "\"",
                                            "Лицо в диалоге с выбором (choice)",
                                            rs,
                                            1
                                        )
                                    );
                                }
                            }
                            break;
                        case "location":
                            if (toks.size() == 3) {
                                this.locationLiterals(out, rs);
                            } else if (toks.size() >= 4) {
                                out.add(
                                    new LanguageSupport.Completion(
                                        "yaw", LanguageSupport.Kind.KEYWORD, "поворот", "yaw ${1:0}", "Поворот по горизонтали (градусы)", rs, 0
                                    )
                                );
                                out.add(
                                    new LanguageSupport.Completion(
                                        "pitch", LanguageSupport.Kind.KEYWORD, "наклон", "pitch ${1:0}", "Наклон головы (градусы)", rs, 0
                                    )
                                );
                                out.add(
                                    new LanguageSupport.Completion(
                                        "dim", LanguageSupport.Kind.KEYWORD, "измерение", "dim \"${1:minecraft:overworld}\"", "Измерение мира", rs, 0
                                    )
                                );
                            }
                    }

                    return filter(out, typed, 80);
                } else if (first.type != TokenType.IDENT) {
                    return List.of();
                } else if (first.text.equals("as") && toks.size() == 1) {
                    this.exprCompletions(out, rs, r);
                    return filter(out, typed, 80);
                } else {
                    if (toks.size() == 1 && Specs.withHead(first.text).size() > 1) {
                        for (CommandSpec sx : Specs.withHead(first.text)) {
                            if (sx.sub != null) {
                                String rest = sx.snippet().substring(sx.head.length() + 1);
                                out.add(
                                    new LanguageSupport.Completion(
                                        sx.sub, LanguageSupport.Kind.COMMAND, sx.signature().substring(sx.head.length() + 1), rest, sx.doc, rs, 0
                                    )
                                );
                            }
                        }
                    }

                    CommandSpec spec = ctx.spec;
                    if (spec == null) {
                        return filter(out, typed, 80);
                    } else if (ctx.specTokens == 1
                        && spec.sub == null
                        && toks.size() == 1
                        && Specs.withHead(first.text).size() > 1
                        && typed.isEmpty()
                        && !explicit) {
                        durations(out, rs);
                        return filter(out, typed, 80);
                    } else {
                        SstLanguage.ArgState st = argState(spec, toks.subList(ctx.specTokens, toks.size()));
                        if (st.keywordNeeded != null) {
                            out.add(
                                new LanguageSupport.Completion(
                                    st.keywordNeeded, LanguageSupport.Kind.KEYWORD, "обязательно", st.keywordNeeded + " ", null, rs, 0
                                )
                            );
                        }

                        if (st.next != null) {
                            this.valueCompletions(out, st.next.type, rs, r);
                        }

                        for (String kw : st.freeKeywords) {
                            if (!kw.equals(st.keywordNeeded)) {
                                CommandSpec.Group g = null;

                                for (CommandSpec.Group gg : spec.groups) {
                                    if (kw.equals(gg.keyword)) {
                                        g = gg;
                                    }
                                }

                                String detail = g != null ? g.params.stream().map(p -> p.type.title).collect(Collectors.joining(" ")) : null;
                                out.add(new LanguageSupport.Completion(kw, LanguageSupport.Kind.KEYWORD, detail, kw + " ", null, rs, 2));
                            }
                        }

                        return filter(out, typed, 120);
                    }
                }
            }
        }
    }

    private static List<String> merge(List<String> a, List<String> b) {
        List<String> list = new ArrayList<>(a);

        for (String s : b) {
            if (!list.contains(s)) {
                list.add(s);
            }
        }

        return list;
    }

    public static String valueKindAt(String text, int quote) {
        SstLanguage.Context ctx = context(text, quote);
        List<Token> t = ctx.tokens;
        if (t.isEmpty()) {
            return null;
        } else if (t.get(0).isWord("character")) {
            if (t.size() == 3) {
                return "entity";
            } else {
                String var6 = t.get(t.size() - 1).text;

                return switch (var6) {
                    case "icon", "portrait", "skin" -> "image";
                    case "color" -> "color";
                    case "hitbox" -> "hitbox";
                    default -> null;
                };
            }
        } else if (ctx.spec == null) {
            return null;
        } else if (ctx.spec.key.equals("hitbox")) {
            return "hitbox";
        } else {
            SstLanguage.ArgState st = argState(ctx.spec, t.subList(ctx.specTokens, t.size()));
            if (st.next == null) {
                return null;
            } else {
                return st.next.name.equals("color") ? "color" : st.next.type.name().toLowerCase(Locale.ROOT);
            }
        }
    }

    private static ParamType stringParamType(SstLanguage.Context ctx) {
        List<Token> t = ctx.tokens;
        if (t.isEmpty()) {
            return null;
        } else if (t.get(0).isWord("character") && t.size() == 3) {
            return ParamType.ENTITY;
        } else if (ctx.spec == null) {
            return null;
        } else {
            SstLanguage.ArgState st = argState(ctx.spec, t.subList(ctx.specTokens, t.size()));
            if (st.next == null) {
                return null;
            } else {
                return switch (st.next.type) {
                    case SOUND, ITEM, EFFECT, ENTITY, ANIM, GUI, GUIEL, IMAGE -> st.next.type;
                    default -> null;
                };
            }
        }
    }

    private void statementCompletions(List<LanguageSupport.Completion> out, int rs, Analyzer.Result r) {
        Set<String> heads = new HashSet<>();

        for (CommandSpec s : Specs.commands()) {
            if (s.sub != null && Specs.command(s.head) == null && heads.add(s.head)) {
                out.add(
                    new LanguageSupport.Completion(
                        s.head,
                        LanguageSupport.Kind.COMMAND,
                        "…",
                        s.head + " ",
                        "Варианты: " + Specs.withHead(s.head).stream().map(x -> x.sub).collect(Collectors.joining(", ")),
                        rs,
                        1
                    )
                );
            }

            out.add(
                new LanguageSupport.Completion(
                    s.key, LanguageSupport.Kind.COMMAND, s.category, s.snippet(), s.signature() + "\n\n" + s.doc + paramDocs(s), rs, s.sub == null ? 1 : 2
                )
            );
        }

        out.add(snippet("label", "label ${1:имя}", rs));
        out.add(snippet("goto", "goto ${1:метка}", rs));
        out.add(snippet("if", "if ${1:условие} {\n    $0\n}", rs));
        out.add(snippet("if else", "if ${1:условие} {\n    $2\n} else {\n    $0\n}", rs));
        out.add(snippet("while", "while ${1:условие} {\n    $0\n}", rs));
        out.add(snippet("repeat", "repeat ${1:3} {\n    $0\n}", rs));
        out.add(snippet("for", "for ${1:x} in ${2:список} {\n    $0\n}", rs));
        out.add(snippet("script autostart", "script ${1:5001} \"${2:Название}\" autostart", rs));
        out.add(snippet("script solo", "script ${1:5001} \"${2:Название}\" solo", rs));
        out.add(snippet("as", "as ${1:actor} {\n    $0\n}", rs));
        out.add(snippet("for players", "for p in players() {\n    as p {\n        $0\n    }\n}", rs));
        out.add(snippet("for range", "for ${1:i} in range(${2:10}) {\n    $0\n}", rs));
        out.add(snippet("every", "every ${1:20t} {\n    $0\n}", rs));
        out.add(snippet("on tick", "on tick {\n    $0\n}", rs));
        out.add(snippet("on signal", "on signal \"${1:имя}\" {\n    $0\n}", rs));
        out.add(snippet("emit", "emit \"${1:имя}\" with ${2:значение}", rs));
        out.add(snippet("var list", "var ${1:items} = []", rs));
        out.add(snippet("var dict", "var ${1:data} = {${2:ключ}: ${3:значение}}", rs));
        out.add(snippet("set", "set ${1:переменная} = ${2:значение}", rs));
        out.add(snippet("choice", "choice ${1:персонаж} \"${2:Вопрос?}\" {\n    \"${3:Да}\" {\n        $0\n    }\n    \"${4:Нет}\" {\n    }\n}", rs));
        out.add(snippet("parallel", "parallel ${1:ветка} {\n    $0\n}", rs));
        out.add(snippet("func", "func ${1:имя}(${2}) {\n    $0\n}", rs));
        out.add(snippet("call", "call ${1:функция}()", rs));
        out.add(snippet("async", "async {\n    $0\n}", rs));
        out.add(snippet("async call", "async call ${1:функция}() as ${2:job}", rs));
        out.add(snippet("await", "await ${1:ветка}", rs));
        out.add(snippet("cancel", "cancel ${1:ветка}", rs));
        out.add(snippet("return", "return", rs));
        out.add(snippet("on button", "on button \"${1:yes}\" {\n    $0\n}", rs));
        out.add(snippet("on key", "on key ${1:H} {\n    $0\n}", rs));
        out.add(snippet("on click", "on click ${1:персонаж} {\n    $0\n}", rs));
        out.add(snippet("switch", "switch ${1:button()} {\n    \"${2:yes}\" {\n        $0\n    }\n    else {\n    }\n}", rs));

        for (Analyzer.Symbol f : r.functions.values()) {
            out.add(
                new LanguageSupport.Completion(
                    "call " + f.name(),
                    LanguageSupport.Kind.FUNCTION,
                    "функция" + f.detail(),
                    callSnippet(f),
                    "Вызвать функцию " + f.name() + f.detail(),
                    rs,
                    1
                )
            );
        }

        out.add(snippet("stop", "stop", rs));
        out.add(snippet("character", "character ${1:имя} = \"${2:spraute:npc}\" name \"${3:Имя}\"", rs));
        out.add(snippet("location", "location ${1:точка} = ${2:(0, 64, 0)}", rs));
        out.add(snippet("var", "var ${1:имя} = ${2:0}", rs));
        out.add(snippet("script", "script ${1:5001} \"${2:Название}\"", rs));
    }

    private void functionCompletions(List<LanguageSupport.Completion> out, int rs, Analyzer.Result r) {
        for (Analyzer.Symbol f : r.functions.values()) {
            String insert = callSnippet(f).substring("call ".length());
            out.add(new LanguageSupport.Completion(f.name(), LanguageSupport.Kind.FUNCTION, f.detail(), insert, "Функция " + f.name() + f.detail(), rs, 0));
        }
    }

    private static String callSnippet(Analyzer.Symbol f) {
        String inner = f.detail() == null ? "" : f.detail().replace("(", "").replace(")", "").trim();
        if (inner.isEmpty()) {
            return "call " + f.name() + "()";
        } else {
            StringBuilder sb = new StringBuilder("call ").append(f.name()).append('(');
            String[] ps = inner.split(",\\s*");

            for (int i = 0; i < ps.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                sb.append("${").append(i + 1).append(':').append(ps[i]).append('}');
            }

            return sb.append(')').toString();
        }
    }

    private static LanguageSupport.Completion snippet(String keyword, String body, int rs) {
        return new LanguageSupport.Completion(
            keyword, LanguageSupport.Kind.KEYWORD, "ключевое слово", body, Specs.keywordDocs().getOrDefault(keyword.split(" ")[0], ""), rs, 0
        );
    }

    private static String paramDocs(CommandSpec s) {
        StringBuilder sb = new StringBuilder();

        for (CommandSpec.Param p : s.allParams()) {
            sb.append("\n• ").append(p.name).append(" — ").append(p.type.title);
        }

        return sb.length() == 0 ? "" : "\n" + sb;
    }

    private LanguageSupport.Completion sym(Analyzer.Symbol s, int rs) {
        LanguageSupport.Kind k = switch (s.kind()) {
            case CHARACTER -> LanguageSupport.Kind.CHARACTER;
            case LOCATION -> LanguageSupport.Kind.LOCATION;
            case LABEL -> LanguageSupport.Kind.LABEL;
            case VARIABLE -> LanguageSupport.Kind.VARIABLE;
            case PARALLEL -> LanguageSupport.Kind.LABEL;
            case FUNCTION -> LanguageSupport.Kind.FUNCTION;
        };
        return new LanguageSupport.Completion(s.name(), k, s.detail(), s.name(), null, rs, 0);
    }

    private static void placeSnippets(List<LanguageSupport.Completion> out, int rs) {
        out.add(
            new LanguageSupport.Completion(
                "infront(player, 3)",
                LanguageSupport.Kind.FUNCTION,
                "перед игроком, на земле",
                "infront(${1:player}, ${2:3})",
                "Точка в 3 блоках перед игроком, стоящая на полу.",
                rs,
                1
            )
        );
        out.add(
            new LanguageSupport.Completion(
                "(player.x + 5, player.y, player.z)",
                LanguageSupport.Kind.VALUE,
                "сдвиг от игрока",
                "(player.x + ${1:5}, player.y, player.z${2})",
                "Координаты, посчитанные от игрока.",
                rs,
                1
            )
        );
        out.add(
            new LanguageSupport.Completion(
                "aimpoint(player, 20)", LanguageSupport.Kind.FUNCTION, "куда смотрит игрок", "aimpoint(player, ${1:20})", null, rs, 1
            )
        );
        out.add(
            new LanguageSupport.Completion(
                "ground((x, y, z))", LanguageSupport.Kind.FUNCTION, "точку — на пол", "ground(${1:(player.x + 5, player.y, player.z)})", null, rs, 2
            )
        );
        out.add(
            new LanguageSupport.Completion(
                "vadd(player.pos, vmul(player.look, 3))",
                LanguageSupport.Kind.FUNCTION,
                "вдоль взгляда (может быть в воздухе)",
                "vadd(player.pos, vmul(player.look, ${1:3}))",
                null,
                rs,
                2
            )
        );
    }

    private void valueCompletions(List<LanguageSupport.Completion> out, ParamType type, int rs, Analyzer.Result r) {
        switch (type) {
            case SOUND:
            case ITEM:
            case EFFECT:
            case ENTITY:
                out.add(new LanguageSupport.Completion("\"…\"", LanguageSupport.Kind.VALUE, type.title, "\"${1:minecraft:}\"", type.doc, rs, 1));
                break;
            case ANIM:
                out.add(new LanguageSupport.Completion("\"…\"", LanguageSupport.Kind.VALUE, type.title, "\"${1:st2/}\"", type.doc, rs, 1));
                break;
            case GUI:
                for (String g : AssetIndex.projectGuis.get().keySet()) {
                    out.add(new LanguageSupport.Completion("\"" + g + "\"", LanguageSupport.Kind.VALUE, "интерфейс", "\"" + g + "\"", null, rs, 0));
                }
                break;
            case GUIEL:
                for (String id : guiElementIds()) {
                    out.add(new LanguageSupport.Completion("\"" + id + "\"", LanguageSupport.Kind.VALUE, "элемент", "\"" + id + "\"", null, rs, 0));
                }
                break;
            case IMAGE:
                for (String im : AssetIndex.imageChoices()) {
                    out.add(new LanguageSupport.Completion("\"" + im + "\"", LanguageSupport.Kind.VALUE, "картинка", "\"" + im + "\"", null, rs, 1));
                }
                break;
            case CHAR:
                r.characters.values().forEach(s -> out.add(this.sym(s, rs)));
                break;
            case LOC:
                r.locations.values().forEach(s -> out.add(this.sym(s, rs)));
                placeSnippets(out, rs);
                this.locationLiterals(out, rs);
                break;
            case TARGET:
                r.characters.values().forEach(s -> out.add(this.sym(s, rs)));
                r.locations.values().forEach(s -> out.add(this.sym(s, rs)));
                out.add(new LanguageSupport.Completion("player", LanguageSupport.Kind.KEYWORD, "игрок", "player", "Игрок", rs, 1));
                out.add(
                    new LanguageSupport.Completion("actor", LanguageSupport.Kind.KEYWORD, "игрок события", "actor", Specs.keywordDocs().get("actor"), rs, 1)
                );
                placeSnippets(out, rs);
                this.locationLiterals(out, rs);
                break;
            case SPEAKER:
                r.characters.values().forEach(s -> out.add(this.sym(s, rs)));
                out.add(new LanguageSupport.Completion("0", LanguageSupport.Kind.VALUE, "слот слева", "0", null, rs, 1));
                out.add(new LanguageSupport.Completion("1", LanguageSupport.Kind.VALUE, "слот справа", "1", null, rs, 1));
                out.add(new LanguageSupport.Completion("\"Имя\"", LanguageSupport.Kind.VALUE, "имя текстом", "\"${1:Имя}\"", null, rs, 2));
                break;
            case LABEL:
                r.labels.values().forEach(s -> out.add(this.sym(s, rs)));
                break;
            case DURATION:
                durations(out, rs);
                break;
            case BOOL:
                out.add(new LanguageSupport.Completion("true", LanguageSupport.Kind.VALUE, null, "true", null, rs, 1));
                out.add(new LanguageSupport.Completion("false", LanguageSupport.Kind.VALUE, null, "false", null, rs, 1));
                break;
            case EXPR:
                this.exprCompletions(out, rs, r);
                break;
            case EMOTION:
                emotions(out, rs);
                break;
            case KEY:
                for (String k : Specs.KEYS) {
                    out.add(new LanguageSupport.Completion(k, LanguageSupport.Kind.VALUE, "клавиша", k, null, rs, 1));
                }
                break;
            case SCRIPT:
                for (Integer id : this.knownScripts.get()) {
                    out.add(new LanguageSupport.Completion(String.valueOf(id), LanguageSupport.Kind.VALUE, "сюжет", String.valueOf(id), null, rs, 1));
                }
                break;
            case EASE:
                for (String e : Ease.names()) {
                    out.add(new LanguageSupport.Completion(e, LanguageSupport.Kind.VALUE, Ease.TITLES.get(e), e, type.doc, rs, 1));
                }
                break;
            case CUTSCENE:
                for (String n : AssetIndex.projectCutscenes.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "катсцена", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case SHOP:
                for (String n : AssetIndex.projectShops.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "магазин", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case FIGHT:
                for (String n : AssetIndex.projectFights.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "стиль боя", "\"" + n + "\"", null, rs, 0));
                }

                for (Entry<String, String> e : FightDef.PRESET_TITLES.entrySet()) {
                    out.add(
                        new LanguageSupport.Completion(
                            "\"" + e.getKey() + "\"", LanguageSupport.Kind.VALUE, e.getValue(), "\"" + e.getKey() + "\"", null, rs, 1
                        )
                    );
                }
                break;
            case ZONE:
                for (String n : AssetIndex.projectZones.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "зона", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case PATH:
                for (String n : AssetIndex.projectPaths.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "путь", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case QUEST:
                for (String n : AssetIndex.projectQuests.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "квест", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case TASK:
                for (String q : AssetIndex.projectQuests.get()) {
                    AssetIndex.questTasks(q)
                        .forEach(
                            (id, line) -> out.add(
                                new LanguageSupport.Completion("\"" + id + "\"", LanguageSupport.Kind.VALUE, q + ": " + line, "\"" + id + "\"", null, rs, 0)
                            )
                        );
                }
                break;
            case POST:
                for (String n : AssetIndex.projectPosts.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "пост-эффект проекта", "\"" + n + "\"", null, rs, 0));
                }

                for (PostPresets.Preset p : PostPresets.all()) {
                    out.add(
                        new LanguageSupport.Completion("\"" + p.name() + "\"", LanguageSupport.Kind.VALUE, p.title(), "\"" + p.name() + "\"", p.group(), rs, 1)
                    );
                }
                break;
            case STRUCTURE:
                for (String n : AssetIndex.structureChoices()) {
                    out.add(
                        new LanguageSupport.Completion(
                            "\"" + n + "\"",
                            LanguageSupport.Kind.VALUE,
                            n.contains(":") ? "структура игры или мода" : "постройка",
                            "\"" + n + "\"",
                            null,
                            rs,
                            0
                        )
                    );
                }
                break;
            case PLAYERCAN:
            case PLAYERSTAT:
            case PLAYERRULE:
                for (PlayerRuleDefs.Rule rule : PlayerRuleDefs.all()) {
                    if ((type != ParamType.PLAYERCAN || rule.isSwitch()) && (type != ParamType.PLAYERSTAT || !rule.isSwitch())) {
                        out.add(
                            new LanguageSupport.Completion(
                                "\"" + rule.name() + "\"", LanguageSupport.Kind.VALUE, rule.title(), "\"" + rule.name() + "\"", rule.doc(), rs, 0
                            )
                        );
                    }
                }
                break;
            case SLOTORDER:
                for (String o : SlotPlan.ORDERS) {
                    out.add(new LanguageSupport.Completion("\"" + o + "\"", LanguageSupport.Kind.VALUE, SlotPlan.orderTitle(o), "\"" + o + "\"", null, rs, 0));
                }
                break;
            case PANELTURN:
                for (String t : PanelLook.TURNS) {
                    out.add(new LanguageSupport.Completion("\"" + t + "\"", LanguageSupport.Kind.VALUE, PanelLook.turnTitle(t), "\"" + t + "\"", null, rs, 0));
                }
                break;
            case PANELLOOK:
                for (String l : PanelLook.looks()) {
                    out.add(new LanguageSupport.Completion("\"" + l + "\"", LanguageSupport.Kind.VALUE, PanelLook.lookTitle(l), "\"" + l + "\"", null, rs, 0));
                }
                break;
            case BUILDSTYLE:
                for (BuildStyles.Style s : BuildStyles.all()) {
                    out.add(
                        new LanguageSupport.Completion("\"" + s.name() + "\"", LanguageSupport.Kind.VALUE, s.title(), "\"" + s.name() + "\"", s.doc(), rs, 0)
                    );
                }
                break;
            case SKILLTREE:
                for (String n : AssetIndex.projectSkills.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "система прокачки", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case SKILLMENU:
                for (String n : AssetIndex.projectSkillMenus.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "меню прокачки", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case SKILLNODE:
                for (String n : AssetIndex.projectSkillNodes.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "навык", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case STEALTH:
                for (String n : AssetIndex.projectStealths.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "стиль стелса", "\"" + n + "\"", null, rs, 0));
                }

                for (Entry<String, String> e : StealthDef.PRESET_TITLES.entrySet()) {
                    out.add(
                        new LanguageSupport.Completion(
                            "\"" + e.getKey() + "\"", LanguageSupport.Kind.VALUE, e.getValue(), "\"" + e.getKey() + "\"", null, rs, 1
                        )
                    );
                }
                break;
            case STEALTHSHAPE:
                for (String n : StealthShape.NAMES) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "форма", "\"" + n + "\"", null, rs, 0));
                }
                break;
            case CAMRIG:
                for (String n : AssetIndex.projectCams.get()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "камера проекта", "\"" + n + "\"", null, rs, 0));
                }

                for (CamProfile.Preset p : CamProfile.presets()) {
                    out.add(
                        new LanguageSupport.Completion("\"" + p.name() + "\"", LanguageSupport.Kind.VALUE, p.title(), "\"" + p.name() + "\"", p.doc(), rs, 1)
                    );
                }
                break;
            case CAMPARAM:
                for (CamProfile.Param p : CamProfile.params()) {
                    out.add(new LanguageSupport.Completion(p.id(), LanguageSupport.Kind.VALUE, p.title(), p.id(), p.doc(), rs, 0));
                }
                break;
            case POSTPARAM:
                for (PostParams.Param p : PostParams.all()) {
                    out.add(new LanguageSupport.Completion(p.id(), LanguageSupport.Kind.VALUE, p.title(), p.id(), p.doc(), rs, 0));
                }
                break;
            case VFX:
                for (String n : List.<String>of()) {
                    out.add(new LanguageSupport.Completion("\"" + n + "\"", LanguageSupport.Kind.VALUE, "эффект проекта", "\"" + n + "\"", null, rs, 0));
                }

                for (FxPresets.Preset p : FxPresets.ALL.values()) {
                    out.add(
                        new LanguageSupport.Completion("\"" + p.name() + "\"", LanguageSupport.Kind.VALUE, p.title(), "\"" + p.name() + "\"", p.doc(), rs, 1)
                    );
                }
                break;
            case LAYER:
                for (String l : Specs.LAYERS) {
                    out.add(new LanguageSupport.Completion(l, LanguageSupport.Kind.VALUE, "слой", l, layerDoc(l), rs, 1));
                }
                break;
            case SYSTEM:
                for (String m : Specs.ENGINE_SYSTEMS) {
                    out.add(new LanguageSupport.Completion(m, LanguageSupport.Kind.VALUE, Specs.systemTitle(m), m, ParamType.SYSTEM.doc, rs, 1));
                }
                break;
            case MOVETYPE:
                for (String m : Specs.MOVE_TYPES) {
                    out.add(new LanguageSupport.Completion(m, LanguageSupport.Kind.VALUE, "тип движения", m, ParamType.MOVETYPE.doc, rs, 1));
                }
                break;
            case TEXT:
                out.add(new LanguageSupport.Completion("\"…\"", LanguageSupport.Kind.VALUE, "текст", "\"${1:текст}\"", ParamType.TEXT.doc, rs, 3));
        }
    }

    private static List<String> guiElementIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();

        for (List<String> l : AssetIndex.projectGuis.get().values()) {
            ids.addAll(l);
        }

        return new ArrayList<>(ids);
    }

    private static String layerDoc(String l) {
        return switch (l) {
            case "action" -> "Основной слой: анимация всего тела, перекрывает стойку и ходьбу";
            case "ambient" -> "Фоновый слой, накладывается поверх";
            case "emotion" -> "Мимика лица";
            case "eyes" -> "Глаза";
            default -> "Накладываемый слой ANIMATION_" + l + ": складывается с остальными анимациями";
        };
    }

    private void locationLiterals(List<LanguageSupport.Completion> out, int rs) {
        if (this.positionProvider != null) {
            String pos = this.positionProvider.get();
            if (pos != null) {
                out.add(
                    new LanguageSupport.Completion(
                        "моя позиция " + pos, LanguageSupport.Kind.VALUE, "координаты игрока", pos, "Текущие координаты вашего персонажа", rs, 0
                    )
                );
            }
        }

        out.add(new LanguageSupport.Completion("(x, y, z)", LanguageSupport.Kind.VALUE, "координаты", "(${1:0}, ${2:64}, ${3:0})", null, rs, 2));
    }

    private static void durations(List<LanguageSupport.Completion> out, int rs) {
        for (String d : List.of("1s", "2s", "3s", "5s", "10s", "20t", "0.5s")) {
            out.add(new LanguageSupport.Completion(d, LanguageSupport.Kind.VALUE, "время", d, ParamType.DURATION.doc, rs, 1));
        }
    }

    private static void emotions(List<LanguageSupport.Completion> out, int rs) {
        for (String e : Specs.EMOTIONS) {
            out.add(new LanguageSupport.Completion(e, LanguageSupport.Kind.VALUE, "эмоция", e, null, rs, 1));
        }
    }

    private void exprCompletions(List<LanguageSupport.Completion> out, int rs, Analyzer.Result r) {
        r.vars.values().forEach(s -> out.add(this.sym(s, rs)));
        r.characters.values().forEach(s -> out.add(this.sym(s, rs)));
        r.locations.values().forEach(s -> out.add(this.sym(s, rs)));

        for (Specs.FunctionSpec f : Specs.functions()) {
            String insert = f.signature().endsWith("()") ? f.name() + "()" : f.name() + "(${1})";
            out.add(new LanguageSupport.Completion(f.name(), LanguageSupport.Kind.FUNCTION, f.returns(), insert, f.signature() + "\n\n" + f.doc(), rs, 1));
        }

        for (String k : List.of("and", "or", "not", "true", "false", "player", "actor")) {
            out.add(new LanguageSupport.Completion(k, LanguageSupport.Kind.KEYWORD, null, k + (k.equals("not") ? " " : ""), Specs.keywordDocs().get(k), rs, 3));
        }
    }

    private static List<LanguageSupport.Completion> filter(List<LanguageSupport.Completion> list, String typed, int limit) {
        String t = typed.toLowerCase(Locale.ROOT);
        List<Object[]> scored = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (LanguageSupport.Completion c : list) {
            if (seen.add(c.label() + "|" + c.insert())) {
                String l = c.label().toLowerCase(Locale.ROOT);
                int score;
                if (t.isEmpty()) {
                    score = 0;
                } else if (l.equals(t)) {
                    score = -3;
                } else if (l.startsWith(t)) {
                    score = -2;
                } else if (l.contains(t)) {
                    score = 1;
                } else {
                    if (!subsequence(t, l)) {
                        continue;
                    }

                    score = 2;
                }

                scored.add(new Object[]{c, score});
            }
        }

        scored.sort(
            Comparator.<Object[]>comparingInt(ox -> (Integer)ox[1])
                .thenComparingInt(ox -> ((LanguageSupport.Completion)ox[0]).priority())
                .thenComparing(ox -> ((LanguageSupport.Completion)ox[0]).label().length())
                .thenComparing(ox -> ((LanguageSupport.Completion)ox[0]).label())
        );
        List<LanguageSupport.Completion> result = new ArrayList<>();

        for (Object[] o : scored) {
            result.add((LanguageSupport.Completion)o[0]);
            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    private static boolean subsequence(String needle, String hay) {
        int j = 0;

        for (int i = 0; i < hay.length() && j < needle.length(); i++) {
            if (hay.charAt(i) == needle.charAt(j)) {
                j++;
            }
        }

        return j == needle.length();
    }

    @Override
    public String hover(String text, int offset, LanguageSupport.Analysis analysis) {
        Analyzer.Result r = result(analysis);
        int s = offset;
        int e = offset;

        while (s > 0 && isWord(text.charAt(s - 1))) {
            s--;
        }

        while (e < text.length() && isWord(text.charAt(e))) {
            e++;
        }

        if (s == e) {
            return null;
        } else {
            String word = text.substring(s, e);
            int ls = text.lastIndexOf(10, Math.max(0, s - 1)) + 1;
            String before = text.substring(ls, s).trim();
            if (before.endsWith("{")) {
                before = "";
            }

            if (before.isEmpty() && Specs.isCommandHead(word)) {
                int ne = e;

                while (ne < text.length() && text.charAt(ne) == ' ') {
                    ne++;
                }

                int we = ne;

                while (we < text.length() && isWord(text.charAt(we))) {
                    we++;
                }

                CommandSpec spec = Specs.command(word + " " + text.substring(ne, we));
                if (spec == null) {
                    spec = Specs.command(word);
                }

                return spec != null
                    ? spec.signature() + "\n" + spec.doc + paramDocs(spec)
                    : "Команда " + word + ": " + Specs.withHead(word).stream().map(x -> x.key).collect(Collectors.joining(", "));
            } else if (Specs.keywordDocs().containsKey(word) && Lexer.KEYWORDS.contains(word)) {
                return word + "\n" + Specs.keywordDocs().get(word);
            } else {
                Specs.FunctionSpec f = Specs.function(word);
                if (f != null && e < text.length() && text.charAt(e) == '(') {
                    return f.signature() + " → " + f.returns() + "\n" + f.doc();
                } else {
                    Analyzer.Symbol sym = r.lookup(word);
                    if (sym != null) {
                        String kind = switch (sym.kind()) {
                            case CHARACTER -> "Персонаж";
                            case LOCATION -> "Точка";
                            case LABEL -> "Метка";
                            case VARIABLE -> "Переменная";
                            case PARALLEL -> "Асинхронная ветка";
                            case FUNCTION -> "Функция";
                        };
                        String detail = sym.detail() != null && !sym.detail().isEmpty() ? " = " + sym.detail() : "";
                        return kind + " " + word + detail + "\nCtrl+клик — перейти к объявлению";
                    } else {
                        return Specs.keywordDocs().containsKey(word) ? word + "\n" + Specs.keywordDocs().get(word) : null;
                    }
                }
            }
        }
    }

    @Override
    public LanguageSupport.Signature signature(String text, int caret, LanguageSupport.Analysis analysis) {
        SstLanguage.Context ctx = context(text, caret);
        if (ctx.spec != null && !ctx.tokens.isEmpty()) {
            CommandSpec spec = ctx.spec;
            if (spec.groups.isEmpty()) {
                return null;
            } else {
                SstLanguage.ArgState st = argState(spec, ctx.tokens.subList(ctx.specTokens, ctx.tokens.size()));
                List<String> parts = new ArrayList<>();
                parts.add(spec.key);

                for (CommandSpec.Group g : spec.groups) {
                    StringBuilder sb = new StringBuilder();
                    if (g.optional) {
                        sb.append('[');
                    }

                    if (g.keyword != null) {
                        sb.append(g.keyword);
                    }

                    for (int i = 0; i < g.params.size(); i++) {
                        CommandSpec.Param p = g.params.get(i);
                        if (g.keyword != null || i > 0) {
                            sb.append(' ');
                        }

                        sb.append('<').append(p.name).append(':').append(p.type.title).append(p.vararg ? "..." : "").append('>');
                    }

                    if (g.optional) {
                        sb.append(']');
                    }

                    parts.add(sb.toString());
                }

                String doc = spec.doc;
                if (st.next != null) {
                    doc = st.next.name + " — " + st.next.type.doc;
                }

                return new LanguageSupport.Signature(parts, st.activePart >= 0 ? st.activePart + 1 : -1, doc);
            }
        } else {
            return null;
        }
    }

    @Override
    public int definition(String text, int offset, LanguageSupport.Analysis analysis) {
        int s = offset;
        int e = offset;

        while (s > 0 && isWord(text.charAt(s - 1))) {
            s--;
        }

        while (e < text.length() && isWord(text.charAt(e))) {
            e++;
        }

        if (s == e) {
            return -1;
        } else {
            Analyzer.Symbol sym = result(analysis).lookup(text.substring(s, e));
            return sym == null ? -1 : sym.start();
        }
    }

    @Override
    public String lineComment() {
        return "//";
    }

    @Override
    public String format(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        int depth = 0;

        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].strip();
            int d = depth;
            if (t.startsWith("}")) {
                d = Math.max(0, depth - 1);
            }

            if (!t.isEmpty()) {
                sb.append("    ".repeat(d)).append(t);
            }

            if (i < lines.length - 1) {
                sb.append('\n');
            }

            boolean inStr = false;

            for (int k = 0; k < t.length(); k++) {
                char ch = t.charAt(k);
                if (ch == '\\' && inStr) {
                    k++;
                } else {
                    if (ch == '"') {
                        inStr = !inStr;
                    }

                    if (!inStr) {
                        if (ch == '/' && k + 1 < t.length() && t.charAt(k + 1) == '/' || ch == '#') {
                            break;
                        }

                        if (ch == '{') {
                            depth++;
                        }

                        if (ch == '}') {
                            depth = Math.max(0, depth - 1);
                        }
                    }
                }
            }
        }

        return sb.toString();
    }

    private static boolean customAnimation(String id) {
        if (ModelLibrary.projectAnimation(null, id).isPresent()) {
            return true;
        } else {
            for (ModelLibrary.Model m : ModelLibrary.all()) {
                if (ModelLibrary.animation(m, id).isPresent()) {
                    return true;
                }
            }

            return false;
        }
    }

    private record ArgState(CommandSpec.Param next, String keywordNeeded, List<String> freeKeywords, int activePart) {
    }

    private static final class Context {
        List<Token> tokens = new ArrayList<>();
        int wordStart;
        String typed = "";
        boolean inString;
        int stringContentStart;
        CommandSpec spec;
        int specTokens;
    }

    public static final class SstAnalysis implements LanguageSupport.Analysis {
        public final Analyzer.Result result;
        private final List<LanguageSupport.Problem> problems = new ArrayList<>();
        private final int[] colors;

        SstAnalysis(String text, boolean clip, boolean body, Ast.Program story) {
            SstLanguage.loadRegistries();
            this.result = body
                ? Analyzer.analyzeBody(text, SstLanguage.registries)
                : (clip ? Analyzer.analyzeClip(text, SstLanguage.registries, story) : Analyzer.analyze(text, SstLanguage.registries));

            for (Diagnostic d : this.result.diagnostics) {
                LanguageSupport.Severity sev = d.severity == Diagnostic.Severity.ERROR
                    ? LanguageSupport.Severity.ERROR
                    : (d.severity == Diagnostic.Severity.WARNING ? LanguageSupport.Severity.WARNING : LanguageSupport.Severity.INFO);
                this.problems.add(new LanguageSupport.Problem(d.start, d.end, sev, d.message));
            }

            this.colors = SstLanguage.highlight(text, this.result);
        }

        @Override
        public List<LanguageSupport.Problem> problems() {
            return this.problems;
        }

        @Override
        public int[] colors() {
            return this.colors;
        }
    }
}
