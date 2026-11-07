package org.sprautestudio.spraute_engine.cutscene;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Ast;

public final class ClipSim {
    public static final double WALK = 2.5902;
    private final Ast.Program story;
    private final double[] player;
    private final Function<String, List<double[]>> paths;
    private final ClipSim.Router router;
    private final ClipSim.Result out = new ClipSim.Result();
    private final Map<String, Object> vars = new HashMap<>();
    private boolean before;
    private boolean reached;
    private String cutscene;
    private String where = "";
    private int callDepth;

    public static double yawTo(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }

    private ClipSim(Ast.Program story, double[] player, Function<String, List<double[]>> paths, ClipSim.Router router) {
        this.story = story;
        this.player = player;
        this.paths = paths;
        this.router = router;
    }

    public static ClipSim.Result run(Ast.Program story, String cutscene, CutsceneDoc doc, double[] player, Function<String, List<double[]>> paths) {
        return run(story, cutscene, doc, player, paths, null);
    }

    public static ClipSim.Result run(
        Ast.Program story, String cutscene, CutsceneDoc doc, double[] player, Function<String, List<double[]>> paths, ClipSim.Router router
    ) {
        ClipSim sim = new ClipSim(story, player, paths, router);
        if (story != null) {
            sim.init();
            sim.before = true;
            sim.cutscene = cutscene;
            sim.statements(story.body, 0.0);
            sim.before = false;
            if (!sim.reached) {
                sim.out.actors.clear();
                sim.out.events.clear();
                sim.out.notes.clear();
                sim.vars.clear();
                sim.init();
            }

            sim.out.events.clear();
        }

        for (CutsceneDoc.Clip clip : doc.clips) {
            if (!clip.code.isBlank()) {
                sim.where = "Скрипт на " + fmt(clip.t) + " с";
                Analyzer.Result check = Analyzer.analyzeClip(clip.code, null, story);
                if (check.hasErrors()) {
                    sim.note("в нём ошибка, он не выполнится");
                } else {
                    sim.statements(check.program().body, clip.t);
                }
            }
        }

        sim.out.events.sort(Comparator.comparingDouble(e -> e.t));
        return sim.out;
    }

    private void init() {
        for (Ast.VarDecl v : this.story.vars) {
            Object value = this.value(v.init, 0.0);
            if (value != null) {
                this.vars.put(v.name, value);
            }
        }
    }

    private static String fmt(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        return r == (long)r ? String.valueOf((long)r) : String.valueOf(r);
    }

    private void note(String text) {
        if (!this.before) {
            String line = this.where.isEmpty() ? text : this.where + ": " + text;
            if (!this.out.notes.contains(line) && this.out.notes.size() < 40) {
                this.out.notes.add(line);
            }
        }
    }

    private double statements(List<Ast.Stmt> body, double clock) {
        for (Ast.Stmt s : body) {
            if (this.reached && this.before) {
                return clock;
            }

            clock = this.statement(s, clock);
            if (Double.isNaN(clock)) {
                return clock;
            }
        }

        return clock;
    }

    private double statement(Ast.Stmt s, double clock) {
        if (s instanceof Ast.CommandStmt c) {
            return this.command(c, clock);
        } else if (s instanceof Ast.SetStmt set) {
            if (set.prop == null && set.index == null) {
                this.assign(set, clock);
            }

            return clock;
        } else if (s instanceof Ast.IfStmt i) {
            Object cond = this.value(i.cond, clock);
            if (cond instanceof Boolean b && !b) {
                return i.otherwise != null ? this.statements(i.otherwise, clock) : clock;
            } else {
                if (!(cond instanceof Boolean)) {
                    this.note("условие if показано как выполненное");
                }

                return this.statements(i.then, clock);
            }
        } else if (s instanceof Ast.RepeatStmt r) {
            Double n = this.number(r.count, clock);
            if (n == null) {
                this.note("число повторов repeat неизвестно: показан один");
                return this.statements(r.body, clock);
            } else {
                for (int k = 0; k < Math.min(n, 64.0); k++) {
                    clock = this.statements(r.body, clock);
                    if (Double.isNaN(clock)) {
                        return clock;
                    }
                }

                return clock;
            }
        } else if (s instanceof Ast.ParallelStmt p) {
            this.statements(p.body, clock);
            return clock;
        } else if (s instanceof Ast.AsStmt a) {
            return this.statements(a.body, clock);
        } else if (s instanceof Ast.CallStmt c) {
            Ast.FuncDecl f = this.story != null ? this.story.func(c.name) : null;
            if (f == null) {
                this.note("функция " + c.name + " не найдена");
                return clock;
            } else if (this.callDepth > 16) {
                return clock;
            } else {
                this.callDepth++;

                for (int kx = 0; kx < Math.min(f.params.size(), c.args.size()); kx++) {
                    Object v = this.value(c.args.get(kx), clock);
                    if (v != null) {
                        this.vars.put(f.params.get(kx), v);
                    }
                }

                double end = this.statements(f.body, clock);
                this.callDepth--;
                return c.async ? clock : end;
            }
        } else if (s instanceof Ast.SwitchStmt sw) {
            this.note("switch показан первым вариантом");
            if (!sw.cases.isEmpty()) {
                return this.statements(sw.cases.get(0).body, clock);
            } else {
                return sw.otherwise != null ? this.statements(sw.otherwise, clock) : clock;
            }
        } else if (s instanceof Ast.WhileStmt || s instanceof Ast.ForStmt || s instanceof Ast.EveryStmt) {
            this.note("циклы while, for и every в просмотре не выполняются");
            return clock;
        } else if (s instanceof Ast.ChoiceStmt) {
            return this.stalled("выбор игрока");
        } else if (s instanceof Ast.AwaitStmt) {
            return this.stalled("await");
        } else if (s instanceof Ast.GotoStmt) {
            return this.stalled("goto");
        } else {
            return !(s instanceof Ast.StopStmt) && !(s instanceof Ast.ReturnStmt) ? clock : Double.NaN;
        }
    }

    private double stalled(String what) {
        if (this.before) {
            return 0.0;
        } else {
            this.note("дальше зависит от игры (" + what + "), просмотр здесь останавливается");
            return Double.NaN;
        }
    }

    private void assign(Ast.SetStmt set, double clock) {
        Object v = this.value(set.value, clock);
        if (v == null) {
            this.vars.remove(set.name);
        } else {
            if (!"=".equals(set.op) && this.vars.get(set.name) instanceof Double old && v instanceof Double d) {
                v = set.op.equals("+=") ? old + d : old - d;
            }

            this.vars.put(set.name, v);
        }
    }

    private double command(Ast.CommandStmt c, double clock) {
        double now = this.before ? 0.0 : clock;
        String var6 = c.key;
        switch (var6) {
            case "cutscene play":
                if (this.before && c.arg("name") instanceof Ast.StringLit s && s.value.equals(this.cutscene)) {
                    this.reached = true;
                }

                return clock;
            case "wait": {
                if (this.before) {
                    return clock;
                }

                Double sec = this.seconds(c.arg("time"), clock);
                if (sec == null) {
                    this.note("длина wait неизвестна: считается 1 с");
                    sec = 1.0;
                }

                return clock + sec;
            }
            case "spawn":
                this.spawn(c, now);
                return clock;
            case "despawn":
                for (ClipSim.Actor ax : this.bodies(c.arg("who"))) {
                    ax.cutAfter(now);
                    ClipSim.Key k = new ClipSim.Key();
                    double[] p = ax.position(now);
                    if (p != null) {
                        k.x = p[0];
                        k.y = p[1];
                        k.z = p[2];
                    }

                    k.t = now;
                    k.present = false;
                    ax.keys.add(k);
                }

                return clock;
            case "tp": {
                double[] to = this.point(c.arg("pos"), now);
                if (to != null) {
                    for (ClipSim.Actor ax : this.bodies(c.arg("who"))) {
                        place(ax, to, now);
                    }
                }

                return clock;
            }
            case "move":
            case "fly": {
                double[] to = this.point(c.arg("target"), now);
                if (to == null) {
                    return clock;
                }

                return this.walk(c, List.of(to), c.key.equals("fly"), clock, now);
            }
            case "path": {
                List<double[]> route = new ArrayList<>();

                for (Ast.Arg a : c.args) {
                    if ("points".equals(a.param)) {
                        double[] p = this.point(a.value, now);
                        if (p != null) {
                            route.add(p);
                        }
                    }
                }

                return route.isEmpty() ? clock : this.walk(c, route, false, clock, now);
            }
            case "walk": {
                List<double[]> route = c.arg("path") instanceof Ast.StringLit s && this.paths != null ? this.paths.apply(s.value) : null;
                if (route != null && !route.isEmpty()) {
                    double end = this.walk(c, route, false, clock, now);
                    return c.arg("nowait") == null && c.arg("loop") == null ? end : clock;
                }

                this.note("путь для walk не найден");
                return clock;
            }
            case "look": {
                Ast.Expr target = c.arg("target");
                String other = target instanceof Ast.Ident id && this.actorOf(id.name) != null ? this.actorOf(id.name).id : null;
                double[] p = other == null ? this.point(target, now) : null;

                for (ClipSim.Actor ax : this.bodies(c.arg("who"))) {
                    ax.looks.add(new ClipSim.Look(now, p, other));
                }

                return clock;
            }
            case "say":
                return this.say(c, clock, now);
            case "fx play":
                this.fx(c, now);
                return clock;
            case "fx stop": {
                ClipSim.Event e = new ClipSim.Event(now, ClipSim.Kind.FX_STOP);
                e.id = this.text(c.arg("id"));
                this.out.events.add(e);
                return clock;
            }
            case "fx clear":
                this.out.events.add(new ClipSim.Event(now, ClipSim.Kind.FX_CLEAR));
                return clock;
            case "explode":
            case "lightning":
            case "shockwave": {
                double[] p = this.point(c.arg("pos"), now);
                if (p == null) {
                    return clock;
                }

                ClipSim.Event e = new ClipSim.Event(
                    now, c.key.equals("explode") ? ClipSim.Kind.EXPLODE : (c.key.equals("lightning") ? ClipSim.Kind.LIGHTNING : ClipSim.Kind.SHOCKWAVE)
                );
                e.x = p[0];
                e.y = p[1];
                e.z = p[2];
                this.out.events.add(e);
                return clock;
            }
            case "anim":
                for (ClipSim.Actor ax : this.bodies(c.arg("who"))) {
                    ClipSim.Event ex = new ClipSim.Event(now, ClipSim.Kind.ANIM);
                    ex.actor = ax.id;
                    ex.name = this.text(c.arg("anim"));
                    ex.id = c.arg("layer") != null ? this.text(c.arg("layer")) : "action";
                    ex.loop = c.arg("loop") != null || c.arg("hold") != null;
                    this.out.events.add(ex);
                }

                return clock;
            case "stopanim":
            case "clearanims":
                for (ClipSim.Actor a : this.bodies(c.arg("who"))) {
                    ClipSim.Event ex = new ClipSim.Event(now, ClipSim.Kind.ANIM_STOP);
                    ex.actor = a.id;
                    ex.id = c.arg("layer") != null ? this.text(c.arg("layer")) : "action";
                    this.out.events.add(ex);
                }

                return clock;
            case "post play": {
                ClipSim.Event e = new ClipSim.Event(now, ClipSim.Kind.POST);
                e.name = this.text(c.arg("name"));
                Double sec = this.seconds(c.arg("time"), clock);
                e.scale = sec != null ? sec : 1.0;
                this.out.events.add(e);
                return clock;
            }
            case "post clear":
                this.out.events.add(new ClipSim.Event(now, ClipSim.Kind.POST_CLEAR));
                return clock;
            default:
                if (c.key.startsWith("wait ")) {
                    return this.stalled(c.key);
                } else {
                    if (c.key.startsWith("camera ")) {
                        this.note("команды camera показываются только в просмотре в игре");
                    }

                    return clock;
                }
        }
    }

    private double say(Ast.CommandStmt c, double clock, double now) {
        String text = this.text(c.arg("text"));
        double length = Math.max(2.0, text.length() * 0.06);
        ClipSim.Actor a = c.arg("who") instanceof Ast.Ident id ? this.actorOf(id.name) : null;
        if (a != null) {
            a.lines.add(new ClipSim.Line(now, now + length, text));
        }

        if (!this.before && c.arg("nowait") == null) {
            this.note("реплика ждёт игрока, в просмотре она идёт примерно " + fmt(length) + " с");
            return clock + length;
        } else {
            return clock;
        }
    }

    private void spawn(Ast.CommandStmt c, double now) {
        if (c.arg("who") instanceof Ast.Ident who) {
            double[] at = this.point(c.arg("pos"), now);
            if (at != null) {
                Double countE = this.number(c.arg("count"), now);
                Double spreadE = this.number(c.arg("spread"), now);
                Double scaleE = this.number(c.arg("scale"), now);
                int count = countE != null ? (int)Math.max(1.0, Math.min(100.0, countE)) : 1;
                double spread = spreadE != null ? spreadE : (count > 1 ? 3.0 : 0.0);
                Ast.CharacterDecl decl = this.declOf(who.name);

                for (int i = 1; i <= count; i++) {
                    String id = Ast.Program.copyName(who.name, i);
                    ClipSim.Actor a = this.out.actors.computeIfAbsent(id, k -> new ClipSim.Actor(k, who.name, decl));
                    if (scaleE != null) {
                        a.scale = scaleE;
                    }

                    if (spread > 0.0 && count > 1) {
                        double angle = i * 2.39996;
                        double r = spread * Math.sqrt((double)i / count);
                        a.offX = Math.cos(angle) * r;
                        a.offZ = Math.sin(angle) * r;
                    }

                    place(a, at, now);
                }
            }
        }
    }

    private Ast.CharacterDecl declOf(String name) {
        if (this.story != null) {
            for (Ast.CharacterDecl d : this.story.characters) {
                if (d.name.equals(name)) {
                    return d;
                }
            }
        }

        return null;
    }

    private static void place(ClipSim.Actor a, double[] to, double now) {
        a.cutAfter(now);
        ClipSim.Key k = new ClipSim.Key();
        k.t = now;
        k.x = to[0] + a.offX;
        k.y = to[1];
        k.z = to[2] + a.offZ;
        a.keys.add(k);
    }

    private double walk(Ast.CommandStmt c, List<double[]> route, boolean air, double clock, double now) {
        List<ClipSim.Actor> list = this.bodies(c.arg("who"));
        Double speedE = this.number(c.arg("speed"), now);
        double speed = Math.max(0.05, speedE != null ? speedE : 1.0) * 2.5902;
        double end = clock;

        for (ClipSim.Actor a : list) {
            double[] from = a.position(now);
            if (from != null && a.presentAt(now)) {
                if (this.before) {
                    double[] last = route.get(route.size() - 1);
                    place(a, last, now);
                } else {
                    a.cutAfter(now);
                    double t = now;
                    double x = from[0];
                    double y = from[1];
                    double z = from[2];
                    if (a.keys.isEmpty() || a.keys.get(a.keys.size() - 1).t < now) {
                        ClipSim.Key start = new ClipSim.Key();
                        start.t = now;
                        start.x = x;
                        start.y = y;
                        start.z = z;
                        a.keys.add(start);
                    }

                    for (double[] p : route) {
                        double[] target = new double[]{p[0] + a.offX, p[1], p[2] + a.offZ};
                        List<double[]> way = !air && this.router != null ? this.router.route(a, new double[]{x, y, z}, target) : null;
                        if (way != null && !way.isEmpty()) {
                            double[] last = way.get(way.size() - 1);
                            if (Math.abs(last[0] - target[0]) + Math.abs(last[2] - target[2]) > 2.0) {
                                this.note("для " + a.base + " нет пути до точки: он остановится раньше");
                            }
                        } else {
                            if (!air && this.router != null) {
                                this.note("путь для " + a.base + " не просчитан (нет модели или место не загружено): показана прямая");
                            }

                            way = List.of(target);
                        }

                        for (double[] step : way) {
                            double dist = Math.sqrt((step[0] - x) * (step[0] - x) + (step[1] - y) * (step[1] - y) + (step[2] - z) * (step[2] - z));
                            t += dist / speed;
                            ClipSim.Key k = new ClipSim.Key();
                            k.t = Math.max(t, a.keys.get(a.keys.size() - 1).t + 0.001);
                            k.x = step[0];
                            k.y = step[1];
                            k.z = step[2];
                            k.glide = true;
                            k.air = air;
                            a.keys.add(k);
                            x = step[0];
                            y = step[1];
                            z = step[2];
                        }
                    }

                    end = Math.max(end, t);
                }
            }
        }

        return this.before ? clock : end;
    }

    private void fx(Ast.CommandStmt c, double now) {
        Ast.Expr pos = c.arg("pos");
        double[] p = this.point(pos, now);
        if (p != null) {
            ClipSim.Event e = new ClipSim.Event(now, ClipSim.Kind.FX);
            e.name = this.text(c.arg("name")).trim();
            e.x = p[0];
            e.y = p[1];
            e.z = p[2];
            Double scale = this.number(c.arg("scale"), now);
            if (scale != null) {
                e.scale = scale;
            }

            if (c.arg("id") != null) {
                e.id = this.text(c.arg("id"));
            }

            if (c.arg("follow") != null && pos instanceof Ast.Ident id && this.actorOf(id.name) != null) {
                e.actor = this.actorOf(id.name).id;
            }

            this.out.events.add(e);
        }
    }

    private List<ClipSim.Actor> bodies(Ast.Expr who) {
        if (!(who instanceof Ast.Ident id)) {
            return List.of();
        } else {
            ArrayList list = new ArrayList();

            for (ClipSim.Actor a : this.out.actors.values()) {
                if (a.base.equals(id.name)) {
                    list.add(a);
                }
            }

            if (list.isEmpty()) {
                if (id.name.equals("player") || id.name.equals("actor")) {
                    return list;
                }

                this.note(id.name + " ещё не появился: добавьте spawn в сюжет перед катсценой или в скрипт раньше");
            }

            return list;
        }
    }

    private ClipSim.Actor actorOf(String name) {
        return this.out.actors.get(name);
    }

    private String text(Ast.Expr e) {
        if (e instanceof Ast.StringLit s) {
            return s.value;
        } else if (e instanceof Ast.Ident id) {
            return this.vars.get(id.name) instanceof String s ? s : id.name;
        } else {
            return "";
        }
    }

    private double[] point(Ast.Expr e, double now) {
        if (this.value(e, now) instanceof double[] p) {
            return p;
        } else {
            this.note("не удалось вычислить точку: она будет понятна только в игре");
            return null;
        }
    }

    private Double number(Ast.Expr e, double now) {
        return e != null && this.value(e, now) instanceof Double d ? d : null;
    }

    private Double seconds(Ast.Expr e, double now) {
        if (e instanceof Ast.DurationLit d) {
            return d.ticks / 20.0;
        } else {
            Double ticks = this.number(e, now);
            return ticks != null ? ticks / 20.0 : null;
        }
    }

    private Object value(Ast.Expr e, double now) {
        if (e == null) {
            return null;
        } else if (e instanceof Ast.NumberLit n) {
            return n.value;
        } else if (e instanceof Ast.DurationLit d) {
            return (double)d.ticks;
        } else if (e instanceof Ast.StringLit s) {
            return s.value;
        } else if (e instanceof Ast.BoolLit b) {
            return b.value;
        } else if (e instanceof Ast.Tuple t && t.items.size() >= 3) {
            Object x = this.value(t.items.get(0), now);
            Object y = this.value(t.items.get(1), now);
            Object z = this.value(t.items.get(2), now);
            return x instanceof Double a && y instanceof Double b && z instanceof Double c ? new double[]{a, b, c} : null;
        } else if (e instanceof Ast.Ident id) {
            if (!id.name.equals("player") && !id.name.equals("actor")) {
                ClipSim.Actor actor = this.actorOf(id.name);
                if (actor != null) {
                    double[] p = actor.position(now);
                    if (p != null) {
                        return p;
                    }
                }

                if (this.story != null) {
                    for (Ast.LocationDecl l : this.story.locations) {
                        if (l.name.equals(id.name)) {
                            return this.value(l.pos, now);
                        }
                    }
                }

                return this.vars.get(id.name);
            } else {
                return this.player;
            }
        } else if (e instanceof Ast.Unary u) {
            Object v = this.value(u.operand, now);
            if (v instanceof Double d && u.op.equals("-")) {
                return -d;
            } else {
                return v instanceof Boolean b && (u.op.equals("!") || u.op.equals("not")) ? !b : null;
            }
        } else if (e instanceof Ast.Binary b) {
            Object lx = this.value(b.left, now);
            Object r = this.value(b.right, now);
            if (lx instanceof Double x && r instanceof Double y) {
                return switch (b.op) {
                    case "+" -> x + y;
                    case "-" -> x - y;
                    case "*" -> x * y;
                    case "/" -> y == 0.0 ? null : x / y;
                    case ">" -> x > y;
                    case "<" -> x < y;
                    case ">=" -> x >= y;
                    case "<=" -> x <= y;
                    case "==" -> x.equals(y);
                    case "!=" -> !x.equals(y);
                    default -> null;
                };
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static final class Actor {
        public final String id;
        public final String base;
        public final Ast.CharacterDecl decl;
        public double scale = 1.0;
        public final List<ClipSim.Key> keys = new ArrayList<>();
        public final List<ClipSim.Look> looks = new ArrayList<>();
        public final List<ClipSim.Line> lines = new ArrayList<>();
        double offX;
        double offZ;

        Actor(String id, String base, Ast.CharacterDecl decl) {
            this.id = id;
            this.base = base;
            this.decl = decl;
        }

        private int index(double t) {
            int i = -1;
            int k = 0;

            while (k < this.keys.size() && this.keys.get(k).t <= t) {
                i = k++;
            }

            return i;
        }

        private double[] position(double t) {
            int i = this.index(t);
            if (i < 0) {
                return null;
            } else {
                ClipSim.Key a = this.keys.get(i);
                if (i + 1 < this.keys.size() && this.keys.get(i + 1).glide) {
                    ClipSim.Key b = this.keys.get(i + 1);
                    double u = (t - a.t) / Math.max(1.0E-6, b.t - a.t);
                    return new double[]{a.x + (b.x - a.x) * u, a.y + (b.y - a.y) * u, a.z + (b.z - a.z) * u};
                } else {
                    return new double[]{a.x, a.y, a.z};
                }
            }
        }

        boolean presentAt(double t) {
            int i = this.index(t);
            return i >= 0 && this.keys.get(i).present;
        }

        public ClipSim.State at(double t, ClipSim.Result world) {
            int i = this.index(t);
            if (i >= 0 && this.keys.get(i).present) {
                double[] p = this.position(t);
                ClipSim.Key a = this.keys.get(i);
                boolean moving = i + 1 < this.keys.size() && this.keys.get(i + 1).glide;
                boolean air = moving ? this.keys.get(i + 1).air : a.air;
                double yaw = 0.0;
                if (moving) {
                    yaw = ClipSim.yawTo(this.keys.get(i + 1).x - a.x, this.keys.get(i + 1).z - a.z);
                } else {
                    for (int k = i; k > 0; k--) {
                        if (this.keys.get(k).glide) {
                            yaw = ClipSim.yawTo(this.keys.get(k).x - this.keys.get(k - 1).x, this.keys.get(k).z - this.keys.get(k - 1).z);
                            break;
                        }
                    }

                    ClipSim.Look look = null;

                    for (ClipSim.Look l : this.looks) {
                        if (l.t <= t) {
                            look = l;
                        }
                    }

                    if (look != null) {
                        double[] to = look.point;
                        if (look.actor != null && world != null && world.actors.containsKey(look.actor)) {
                            to = world.actors.get(look.actor).position(t);
                        }

                        if (to != null) {
                            yaw = ClipSim.yawTo(to[0] - p[0], to[2] - p[2]);
                        }
                    }
                }

                return new ClipSim.State(true, p[0], p[1], p[2], yaw, moving, air);
            } else {
                return new ClipSim.State(false, 0.0, 0.0, 0.0, 0.0, false, false);
            }
        }

        public String speech(double t) {
            for (ClipSim.Line l : this.lines) {
                if (t >= l.t0 && t < l.t1) {
                    return l.text;
                }
            }

            return null;
        }

        void cutAfter(double t) {
            double[] p = this.position(t);
            int i = this.index(t);
            boolean walking = i >= 0 && i + 1 < this.keys.size() && this.keys.get(i + 1).glide;
            boolean air = walking ? this.keys.get(i + 1).air : i >= 0 && this.keys.get(i).air;

            while (this.keys.size() > i + 1) {
                this.keys.remove(this.keys.size() - 1);
            }

            if (i >= 0 && p != null && this.keys.get(i).t < t) {
                ClipSim.Key k = new ClipSim.Key();
                k.t = t;
                k.x = p[0];
                k.y = p[1];
                k.z = p[2];
                k.glide = walking;
                k.air = air;
                this.keys.add(k);
            }
        }
    }

    public static final class Event {
        public final double t;
        public final ClipSim.Kind kind;
        public String name = "";
        public double x;
        public double y;
        public double z;
        public double scale = 1.0;
        public String actor;
        public String id = "";
        public boolean loop;

        Event(double t, ClipSim.Kind kind) {
            this.t = t;
            this.kind = kind;
        }
    }

    public static final class Key {
        public double t;
        public double x;
        public double y;
        public double z;
        public boolean present = true;
        public boolean glide;
        public boolean air;
    }

    public static enum Kind {
        FX,
        FX_STOP,
        FX_CLEAR,
        EXPLODE,
        LIGHTNING,
        SHOCKWAVE,
        ANIM,
        ANIM_STOP,
        POST,
        POST_CLEAR;
    }

    public record Line(double t0, double t1, String text) {
    }

    public record Look(double t, double[] point, String actor) {
    }

    public static final class Result {
        public final Map<String, ClipSim.Actor> actors = new LinkedHashMap<>();
        public final List<ClipSim.Event> events = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
    }

    public interface Router {
        List<double[]> route(ClipSim.Actor var1, double[] var2, double[] var3);
    }

    public record State(boolean present, double x, double y, double z, double yaw, boolean moving, boolean air) {
    }
}
