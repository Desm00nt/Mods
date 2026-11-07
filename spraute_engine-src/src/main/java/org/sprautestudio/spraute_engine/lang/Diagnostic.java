package org.sprautestudio.spraute_engine.lang;

public final class Diagnostic {
    public final Diagnostic.Severity severity;
    public final String message;
    public final int start;
    public final int end;
    public final int line;
    public final int col;

    public Diagnostic(Diagnostic.Severity severity, String message, int start, int end, int line, int col) {
        this.severity = severity;
        this.message = message;
        this.start = start;
        this.end = Math.max(end, start);
        this.line = line;
        this.col = col;
    }

    public boolean isError() {
        return this.severity == Diagnostic.Severity.ERROR;
    }

    @Override
    public String toString() {
        return this.line + 1 + ":" + (this.col + 1) + " " + this.severity + ": " + this.message;
    }

    public static enum Severity {
        ERROR,
        WARNING,
        INFO;
    }
}
