package org.sprautestudio.spraute_engine.ui.code;

import java.util.List;
import org.sprautestudio.spraute_engine.ui.Icon;

public interface LanguageSupport {
    LanguageSupport.Analysis analyze(String var1);

    List<LanguageSupport.Completion> complete(String var1, int var2, LanguageSupport.Analysis var3, boolean var4);

    default boolean isCompletionTrigger(char typed) {
        return Character.isLetter(typed) || typed == '_';
    }

    String hover(String var1, int var2, LanguageSupport.Analysis var3);

    LanguageSupport.Signature signature(String var1, int var2, LanguageSupport.Analysis var3);

    int definition(String var1, int var2, LanguageSupport.Analysis var3);

    String lineComment();

    default String format(String text) {
        return null;
    }

    public interface Analysis {
        List<LanguageSupport.Problem> problems();

        int[] colors();
    }

    public record Completion(String label, LanguageSupport.Kind kind, String detail, String insert, String doc, int replaceStart, int priority) {
    }

    public static enum Kind {
        KEYWORD(Icon.CODE, -3697942),
        COMMAND(Icon.BOLT, -8213761),
        SNIPPET(Icon.EDIT, -5195831),
        CHARACTER(Icon.USER, -11544898),
        LOCATION(Icon.PIN, -13461),
        LABEL(Icon.TAG, -33098),
        VARIABLE(Icon.DOT, -8531969),
        FUNCTION(Icon.CODE, -10880),
        VALUE(Icon.LIST, -553876);

        public final Icon icon;
        public final int color;

        private Kind(Icon icon, int color) {
            this.icon = icon;
            this.color = color;
        }
    }

    public record Problem(int start, int end, LanguageSupport.Severity severity, String message) {
    }

    public static enum Severity {
        ERROR,
        WARNING,
        INFO;
    }

    public record Signature(List<String> parts, int active, String doc) {
    }
}
