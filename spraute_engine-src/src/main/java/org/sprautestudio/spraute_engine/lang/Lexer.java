package org.sprautestudio.spraute_engine.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class Lexer {
    public static final Set<String> KEYWORDS = Set.of(
        "script",
        "character",
        "location",
        "var",
        "label",
        "goto",
        "if",
        "else",
        "while",
        "repeat",
        "set",
        "choice",
        "parallel",
        "true",
        "false",
        "and",
        "or",
        "not",
        "player",
        "stop",
        "func",
        "call",
        "return",
        "async",
        "await",
        "cancel",
        "on",
        "switch",
        "for",
        "in",
        "every",
        "break",
        "continue"
    );
    private static final String[] OPERATORS = new String[]{
        "==", "!=", "<=", ">=", "&&", "||", "->", "+=", "-=", "=", "<", ">", "+", "-", "*", "/", "%", "!", ":", "."
    };
    private final String src;
    private final boolean keepComments;
    private int pos;
    private int line;
    private int col;
    private final List<Token> out = new ArrayList<>();

    private Lexer(String src, boolean keepComments) {
        this.src = src;
        this.keepComments = keepComments;
    }

    public static List<Token> tokenize(String src) {
        return new Lexer(src, false).run();
    }

    public static List<Token> tokenizeAll(String src) {
        return new Lexer(src, true).run();
    }

    private List<Token> run() {
        while (this.pos < this.src.length()) {
            char c = this.src.charAt(this.pos);
            if (c == '\n') {
                this.add(TokenType.NEWLINE, this.pos, this.pos + 1, null);
                this.advance(1);
            } else if (c != ' ' && c != '\t' && c != '\r') {
                if (c == '/' && this.peek(1) == '/') {
                    int s = this.pos;

                    while (this.pos < this.src.length() && this.src.charAt(this.pos) != '\n') {
                        this.pos++;
                    }

                    this.comment(s);
                } else if (c == '#') {
                    int s = this.pos;

                    while (this.pos < this.src.length() && this.src.charAt(this.pos) != '\n') {
                        this.pos++;
                    }

                    this.comment(s);
                } else if (c == '/' && this.peek(1) == '*') {
                    this.blockComment();
                } else if (c == '"') {
                    this.string();
                } else if (!Character.isDigit(c) && (c != '.' || !Character.isDigit(this.peek(1)))) {
                    if (Character.isLetter(c) || c == '_') {
                        this.ident();
                    } else if (c == '{') {
                        this.single(TokenType.LBRACE);
                    } else if (c == '}') {
                        this.single(TokenType.RBRACE);
                    } else if (c == '(') {
                        this.single(TokenType.LPAREN);
                    } else if (c == ')') {
                        this.single(TokenType.RPAREN);
                    } else if (c == '[') {
                        this.single(TokenType.LBRACKET);
                    } else if (c == ']') {
                        this.single(TokenType.RBRACKET);
                    } else if (c == ',') {
                        this.single(TokenType.COMMA);
                    } else if (c == ';') {
                        this.add(TokenType.NEWLINE, this.pos, this.pos + 1, null);
                        this.advance(1);
                    } else {
                        this.operator();
                    }
                } else {
                    this.number();
                }
            } else {
                this.advance(1);
            }
        }

        this.out.add(new Token(TokenType.EOF, "", this.src.length(), this.src.length(), this.line, this.col, null));
        return this.out;
    }

    private char peek(int off) {
        int p = this.pos + off;
        return p < this.src.length() ? this.src.charAt(p) : '\u0000';
    }

    private void advance(int n) {
        for (int i = 0; i < n; i++) {
            if (this.src.charAt(this.pos) == '\n') {
                this.line++;
                this.col = 0;
            } else {
                this.col++;
            }

            this.pos++;
        }
    }

    private void add(TokenType type, int start, int end, Object value) {
        this.out.add(new Token(type, this.src.substring(start, end), start, end, this.line, this.col, value));
    }

    private void single(TokenType type) {
        this.add(type, this.pos, this.pos + 1, null);
        this.advance(1);
    }

    private void comment(int start) {
        int len = this.pos - start;
        this.pos = start;
        if (this.keepComments) {
            this.add(TokenType.COMMENT, start, start + len, null);
        }

        this.advance(len);
    }

    private void blockComment() {
        int start = this.pos;
        int sl = this.line;
        int sc = this.col;
        int endIdx = this.src.indexOf("*/", this.pos + 2);
        int end = endIdx < 0 ? this.src.length() : endIdx + 2;
        if (this.keepComments) {
            this.out.add(new Token(TokenType.COMMENT, this.src.substring(start, end), start, end, sl, sc, null));
        }

        this.advance(end - this.pos);
    }

    private void string() {
        int start = this.pos;
        int sl = this.line;
        int sc = this.col;
        StringBuilder sb = new StringBuilder();
        int p = this.pos + 1;
        boolean closed = false;

        while (p < this.src.length()) {
            char c = this.src.charAt(p);
            if (c == '"') {
                closed = true;
                p++;
                break;
            }

            if (c == '\n') {
                break;
            }

            if (c == '\\' && p + 1 < this.src.length()) {
                char n = this.src.charAt(p + 1);
                switch (n) {
                    case '"':
                        sb.append('"');
                        break;
                    case '\\':
                        sb.append('\\');
                        break;
                    case 'n':
                        sb.append('\n');
                        break;
                    case 't':
                        sb.append('\t');
                        break;
                    case '{':
                        sb.append("\\{");
                        break;
                    default:
                        sb.append('\\').append(n);
                }

                p += 2;
            } else {
                sb.append(c);
                p++;
            }
        }

        this.out
            .add(
                new Token(
                    closed ? TokenType.STRING : TokenType.ERROR, this.src.substring(start, p), start, p, sl, sc, closed ? sb.toString() : "Незакрытая строка"
                )
            );
        this.advance(p - this.pos);
    }

    private void number() {
        int start = this.pos;
        int p = this.pos;

        while (p < this.src.length() && Character.isDigit(this.src.charAt(p))) {
            p++;
        }

        if (p < this.src.length() && this.src.charAt(p) == '.' && p + 1 < this.src.length() && Character.isDigit(this.src.charAt(p + 1))) {
            p++;

            while (p < this.src.length() && Character.isDigit(this.src.charAt(p))) {
                p++;
            }
        }

        double value = Double.parseDouble(this.src.substring(start, p));
        int q = p;

        while (q < this.src.length() && Character.isLetter(this.src.charAt(q))) {
            q++;
        }

        String suffix = this.src.substring(p, q);
        if (!suffix.isEmpty()) {
            Integer ticks = switch (suffix) {
                case "t" -> (int)Math.round(value);
                case "s" -> (int)Math.round(value * 20.0);
                case "m" -> (int)Math.round(value * 20.0 * 60.0);
                case "ms" -> (int)Math.round(value / 50.0);
                default -> null;
            };
            if (ticks != null) {
                this.add(TokenType.DURATION, start, q, ticks);
            } else {
                this.out
                    .add(
                        new Token(
                            TokenType.ERROR,
                            this.src.substring(start, q),
                            start,
                            q,
                            this.line,
                            this.col,
                            "Неизвестная единица времени '" + suffix + "' (используйте t, s, m или ms)"
                        )
                    );
            }

            this.advance(q - this.pos);
        } else {
            this.add(TokenType.NUMBER, start, p, value);
            this.advance(p - this.pos);
        }
    }

    private void ident() {
        int start = this.pos;
        int p = this.pos;

        while (p < this.src.length() && (Character.isLetterOrDigit(this.src.charAt(p)) || this.src.charAt(p) == '_')) {
            p++;
        }

        String word = this.src.substring(start, p);
        this.add(KEYWORDS.contains(word) ? TokenType.KEYWORD : TokenType.IDENT, start, p, null);
        this.advance(p - this.pos);
        if (word.equals("js")) {
            this.jsBlock();
        }
    }

    private void jsBlock() {
        int p = this.pos;

        while (p < this.src.length() && (this.src.charAt(p) == ' ' || this.src.charAt(p) == '\t')) {
            p++;
        }

        if (p < this.src.length() && this.src.charAt(p) == '{') {
            int start = p;
            int depth = 0;
            int sl = this.line;
            int sc = this.col + (p - this.pos);
            boolean closed = false;

            while (p < this.src.length()) {
                char c = this.src.charAt(p);
                if (c != '"' && c != '\'' && c != '`') {
                    if (c == '/' && p + 1 < this.src.length() && this.src.charAt(p + 1) == '/') {
                        while (p < this.src.length() && this.src.charAt(p) != '\n') {
                            p++;
                        }
                    } else if (c == '/' && p + 1 < this.src.length() && this.src.charAt(p + 1) == '*') {
                        int e = this.src.indexOf("*/", p + 2);
                        p = e < 0 ? this.src.length() : e + 2;
                    } else {
                        if (c == '{') {
                            depth++;
                        } else if (c == '}') {
                            if (--depth == 0) {
                                p++;
                                closed = true;
                                break;
                            }
                        }

                        p++;
                    }
                } else {
                    char q = c;
                    p++;

                    for (; p < this.src.length() && this.src.charAt(p) != q; p++) {
                        if (this.src.charAt(p) == '\\') {
                            p++;
                        }
                    }

                    p++;
                }
            }

            p = Math.min(p, this.src.length());
            String code = closed ? this.src.substring(start + 1, p - 1) : this.src.substring(start + 1);
            this.advance(start - this.pos);
            this.out
                .add(
                    new Token(
                        closed ? TokenType.STRING : TokenType.ERROR,
                        this.src.substring(start, p),
                        start,
                        p,
                        this.line,
                        this.col,
                        closed ? code.replace("{", "\\{") : "Незакрытый блок js { … }"
                    )
                );
            this.advance(p - this.pos);
        }
    }

    private void operator() {
        for (String op : OPERATORS) {
            if (this.src.startsWith(op, this.pos)) {
                this.add(TokenType.OPERATOR, this.pos, this.pos + op.length(), null);
                this.advance(op.length());
                return;
            }
        }

        this.out
            .add(
                new Token(
                    TokenType.ERROR,
                    this.src.substring(this.pos, this.pos + 1),
                    this.pos,
                    this.pos + 1,
                    this.line,
                    this.col,
                    "Неожиданный символ '" + this.src.charAt(this.pos) + "'"
                )
            );
        this.advance(1);
    }
}
