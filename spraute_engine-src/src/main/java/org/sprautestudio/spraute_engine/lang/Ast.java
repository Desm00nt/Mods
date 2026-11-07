package org.sprautestudio.spraute_engine.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Ast {
    private Ast() {
    }

    public static final class Arg extends Ast.Node {
        public String keyword;
        public Ast.Expr value;
        public String param;
    }

    public static final class AsStmt extends Ast.Stmt {
        public Ast.Expr who;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class AwaitStmt extends Ast.Stmt {
        public final List<String> names = new ArrayList<>();
        public final List<int[]> spans = new ArrayList<>();
    }

    public static final class Binary extends Ast.Expr {
        public String op;
        public Ast.Expr left;
        public Ast.Expr right;
    }

    public static final class BoolLit extends Ast.Expr {
        public boolean value;
    }

    public static final class BreakStmt extends Ast.Stmt {
    }

    public static final class Call extends Ast.Expr {
        public String name;
        public int nameStart;
        public int nameEnd;
        public final List<Ast.Expr> args = new ArrayList<>();
    }

    public static final class CallStmt extends Ast.Stmt {
        public String name;
        public int nameStart;
        public int nameEnd;
        public final List<Ast.Expr> args = new ArrayList<>();
        public boolean async;
        public String as;
        public int asStart;
        public int asEnd;
    }

    public static final class CancelStmt extends Ast.Stmt {
        public String name;
        public int nameStart;
        public int nameEnd;
    }

    public static final class CharacterDecl extends Ast.Node {
        public String name;
        public int nameStart;
        public int nameEnd;
        public Ast.Expr type;
        public Ast.Expr existing;
        public Ast.Expr displayName;
        public Ast.Expr speed;
        public Ast.Expr skin;
        public Ast.Expr color;
        public Ast.Expr hitbox;
        public Ast.Expr health;
        public Ast.Expr icon;
        public Ast.Expr portrait;
        public Ast.Expr headScale;
        public Ast.Expr headX;
        public Ast.Expr headY;
        public Ast.Expr headYaw;
        public Ast.Expr priority;
        public Ast.Expr sharedId;
        public Ast.Expr headBone;
    }

    public static final class ChoiceOption extends Ast.Node {
        public Ast.Expr text;
        public Ast.Expr locked;
        public Ast.Expr emotion;
        public boolean center;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class ChoiceStmt extends Ast.Stmt {
        public String character;
        public int characterStart;
        public int characterEnd;
        public Ast.Expr text;
        public Ast.Expr emotion;
        public Ast.Expr anims;
        public final List<Ast.ChoiceOption> options = new ArrayList<>();
    }

    public static final class CommandStmt extends Ast.Stmt {
        public String key;
        public int nameStart;
        public int nameEnd;
        public final List<Ast.Arg> args = new ArrayList<>();

        public Ast.Expr arg(String param) {
            for (Ast.Arg a : this.args) {
                if (param.equals(a.param)) {
                    return a.value;
                }
            }

            return null;
        }
    }

    public static final class ContinueStmt extends Ast.Stmt {
    }

    public static final class DurationLit extends Ast.Expr {
        public int ticks;
    }

    public static final class EveryStmt extends Ast.Stmt {
        public Ast.Expr interval;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public abstract static class Expr extends Ast.Node {
    }

    public static final class ForStmt extends Ast.Stmt {
        public String var;
        public int varStart;
        public int varEnd;
        public Ast.Expr iter;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class FuncDecl extends Ast.Node {
        public String name;
        public int nameStart;
        public int nameEnd;
        public final List<String> params = new ArrayList<>();
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class GotoStmt extends Ast.Stmt {
        public String name;
        public int nameStart;
        public int nameEnd;
    }

    public static final class Header extends Ast.Node {
        public int id;
        public String title = "";
        public boolean autostart;
        public boolean once;
        public boolean solo;
        public boolean general;
        public int idStart;
        public int idEnd;
    }

    public static final class Ident extends Ast.Expr {
        public String name;
    }

    public static final class IfStmt extends Ast.Stmt {
        public Ast.Expr cond;
        public final List<Ast.Stmt> then = new ArrayList<>();
        public List<Ast.Stmt> otherwise;
    }

    public static final class Index extends Ast.Expr {
        public Ast.Expr target;
        public Ast.Expr index;
    }

    public static final class LabelStmt extends Ast.Stmt {
        public String name;
    }

    public static final class ListLit extends Ast.Expr {
        public final List<Ast.Expr> items = new ArrayList<>();
    }

    public static final class LocationDecl extends Ast.Node {
        public String name;
        public int nameStart;
        public int nameEnd;
        public Ast.Expr pos;
        public Ast.Expr yaw;
        public Ast.Expr pitch;
        public Ast.Expr dimension;
    }

    public static final class MapLit extends Ast.Expr {
        public final List<Ast.Expr> keys = new ArrayList<>();
        public final List<Ast.Expr> values = new ArrayList<>();
    }

    public static final class Member extends Ast.Expr {
        public Ast.Expr target;
        public String name;
        public int nameStart;
        public int nameEnd;
        public List<Ast.Expr> args;
    }

    public static final class MethodStmt extends Ast.Stmt {
        public String target;
        public int targetStart;
        public int targetEnd;
        public String method;
        public final List<Ast.Expr> args = new ArrayList<>();
    }

    public abstract static class Node {
        public int start;
        public int end;

        public <T extends Ast.Node> T at(int start, int end) {
            this.start = start;
            this.end = end;
            return (T)this;
        }
    }

    public static final class NumberLit extends Ast.Expr {
        public double value;
    }

    public static final class OnStmt extends Ast.Stmt {
        public Ast.CommandStmt event;
        public int eventStart;
        public int eventEnd;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class ParallelStmt extends Ast.Stmt {
        public String name;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class Program extends Ast.Node {
        public Ast.Header header;
        public final List<Ast.CharacterDecl> characters = new ArrayList<>();
        public final List<Ast.LocationDecl> locations = new ArrayList<>();
        public final List<Ast.VarDecl> vars = new ArrayList<>();
        public final List<Ast.FuncDecl> funcs = new ArrayList<>();
        public final List<Ast.Stmt> body = new ArrayList<>();
        public final Map<String, Integer> copies = new HashMap<>();

        public static String copyName(String character, int number) {
            return number <= 1 ? character : character + "#" + number;
        }

        public static String copyOf(String name) {
            int mark = name.indexOf(35);
            return mark < 0 ? name : name.substring(0, mark);
        }

        public Ast.FuncDecl func(String name) {
            for (Ast.FuncDecl f : this.funcs) {
                if (f.name.equals(name)) {
                    return f;
                }
            }

            return null;
        }
    }

    public static final class RepeatStmt extends Ast.Stmt {
        public Ast.Expr count;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class ReturnStmt extends Ast.Stmt {
    }

    public static final class SetStmt extends Ast.Stmt {
        public String name;
        public int nameStart;
        public int nameEnd;
        public Ast.Expr index;
        public String prop;
        public int propStart;
        public int propEnd;
        public String op;
        public Ast.Expr value;
    }

    public abstract static class Stmt extends Ast.Node {
    }

    public static final class StopStmt extends Ast.Stmt {
    }

    public static final class StringLit extends Ast.Expr {
        public String value;
    }

    public static final class SwitchCase extends Ast.Node {
        public final List<Ast.Expr> values = new ArrayList<>();
        public final List<Ast.Stmt> body = new ArrayList<>();
    }

    public static final class SwitchStmt extends Ast.Stmt {
        public Ast.Expr value;
        public final List<Ast.SwitchCase> cases = new ArrayList<>();
        public List<Ast.Stmt> otherwise;
    }

    public static final class Tuple extends Ast.Expr {
        public final List<Ast.Expr> items = new ArrayList<>();
    }

    public static final class Unary extends Ast.Expr {
        public String op;
        public Ast.Expr operand;
    }

    public static final class VarDecl extends Ast.Node {
        public String name;
        public int nameStart;
        public int nameEnd;
        public Ast.Expr init;
    }

    public static final class WhileStmt extends Ast.Stmt {
        public Ast.Expr cond;
        public final List<Ast.Stmt> body = new ArrayList<>();
    }
}
