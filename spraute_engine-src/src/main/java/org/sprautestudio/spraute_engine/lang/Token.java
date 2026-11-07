package org.sprautestudio.spraute_engine.lang;

public final class Token {
    public final TokenType type;
    public final String text;
    public final int start;
    public final int end;
    public final int line;
    public final int col;
    public final Object value;

    public Token(TokenType type, String text, int start, int end, int line, int col, Object value) {
        this.type = type;
        this.text = text;
        this.start = start;
        this.end = end;
        this.line = line;
        this.col = col;
        this.value = value;
    }

    public boolean is(TokenType t) {
        return this.type == t;
    }

    public boolean is(TokenType t, String s) {
        return this.type == t && this.text.equals(s);
    }

    public boolean isWord(String s) {
        return (this.type == TokenType.IDENT || this.type == TokenType.KEYWORD) && this.text.equals(s);
    }

    public boolean isOp(String s) {
        return this.type == TokenType.OPERATOR && this.text.equals(s);
    }

    @Override
    public String toString() {
        return this.type + "'" + this.text + "'@" + this.line + ":" + this.col;
    }
}
