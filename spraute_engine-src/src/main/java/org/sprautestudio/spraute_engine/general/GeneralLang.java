package org.sprautestudio.spraute_engine.general;

import java.util.Set;
import java.util.regex.Pattern;

public final class GeneralLang {
    public static final Set<String> ONLY_GENERAL = Set.of("menu open", "wait menu", "game quit", "world open", "server join", "link open");
    public static final Set<String> COMMANDS = Set.of(
        "menu open",
        "wait menu",
        "game quit",
        "world open",
        "server join",
        "link open",
        "wait",
        "wait random",
        "wait until",
        "wait button",
        "log",
        "sound",
        "gui open",
        "gui close",
        "gui text",
        "gui show",
        "gui hide",
        "gui progress",
        "gui image",
        "gui set"
    );
    public static final Set<String> ONLY_GENERAL_FUNCTIONS = Set.of("menu");
    public static final Set<String> FUNCTIONS = Set.of(
        "menu",
        "button",
        "input",
        "guiopen",
        "random",
        "chance",
        "randint",
        "round",
        "floor",
        "int",
        "abs",
        "min",
        "max",
        "num",
        "str",
        "len",
        "range",
        "list",
        "has",
        "indexof",
        "keys",
        "values",
        "sum",
        "join",
        "split",
        "sorted",
        "pick",
        "contains",
        "upper",
        "lower"
    );
    private static final Pattern HEADER = Pattern.compile("(?m)^\\s*script\\s+\\d+(\\s+\"(?:[^\"\\\\\\n]|\\\\.)*\")?+(\\s+\\w+)*?\\s+general\\b");

    private GeneralLang() {
    }

    public static boolean isGeneral(String source) {
        return source != null && HEADER.matcher(source).find();
    }
}
