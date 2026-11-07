package org.sprautestudio.spraute_engine.runtime;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public final class Values {
    private static final Random RND = new Random();
    private static final Gson GSON = new Gson();
    public static final String JSON_MARK = "§json:";

    private Values() {
    }

    public static List<Object> iterable(Object v) {
        if (v instanceof List<?> l) {
            return new ArrayList<>((Collection<? extends Object>)l);
        } else if (v instanceof Map<?, ?> m) {
            return new ArrayList<>((Collection<? extends Object>)m.keySet());
        } else if (!(v instanceof Double d)) {
            if (v instanceof String s) {
                List<Object> out = new ArrayList<>();
                s.codePoints().forEach(cp -> out.add(new String(Character.toChars(cp))));
                return out;
            } else {
                throw new SprauteRuntimeException("for: ожидался список, словарь, число или текст");
            }
        } else {
            List<Object> out = new ArrayList<>();

            for (int i = 0; i < Math.min(100000, d.intValue()); i++) {
                out.add((double)i);
            }

            return out;
        }
    }

    private static int idx(List<?> l, Object i) {
        int n = (int)Math.floor(Eval.num(i));
        if (n < 0) {
            n += l.size();
        }

        return n;
    }

    public static Object index(Object c, Object i) {
        if (c instanceof List<?> l) {
            int n = idx(l, i);
            if (n >= 0 && n < l.size()) {
                return l.get(n);
            } else {
                throw new SprauteRuntimeException("Индекс " + Eval.str(i) + " вне списка (в нём " + l.size() + ")");
            }
        } else if (c instanceof Map<?, ?> m) {
            Object v = m.get(Eval.str(i));
            return v == null ? "" : v;
        } else if (c instanceof String s) {
            int n = (int)Math.floor(Eval.num(i));
            if (n < 0) {
                n += s.length();
            }

            return n >= 0 && n < s.length() ? String.valueOf(s.charAt(n)) : "";
        } else {
            throw new SprauteRuntimeException("[ ] работает только со списками, словарями и текстом");
        }
    }

    public static void setIndex(Object c, Object i, Object v) {
        if (c instanceof List<?> l) {
            int n = idx(l, i);
            if (n == l.size()) {
                ((List<Object>)l).add(v);
            } else if (n >= 0 && n <= l.size()) {
                ((List<Object>)l).set(n, v);
            } else {
                throw new SprauteRuntimeException("Индекс " + Eval.str(i) + " вне списка (в нём " + l.size() + ")");
            }
        } else if (c instanceof Map<?, ?> m) {
            ((Map<String, Object>)m).put(Eval.str(i), v);
        } else {
            throw new SprauteRuntimeException("set x[...]: переменная должна быть списком или словарём (var x = [] или var x = {})");
        }
    }

    public static Object property(Object c, String name) {
        switch (name) {
            case "len":
            case "size":
            case "count":
            case "length":
                if (c instanceof List<?> l) {
                    return (double)l.size();
                } else {
                    if (c instanceof Map<?, ?> mx) {
                        return (double)mx.size();
                    }

                    return (double)Eval.str(c).length();
                }
            case "empty":
                return !Eval.truthy(c);
            case "x":
            case "y":
            case "z":
                if (c instanceof List<?> l) {
                    int i = name.charAt(0) - 'x';
                    if (i < l.size()) {
                        return l.get(i);
                    }

                    throw new SprauteRuntimeException("." + name + ": в точке нет такой координаты");
                } else {
                    if (c instanceof Map<?, ?> mx && mx.containsKey(name)) {
                        return mx.get(name);
                    }

                    throw new SprauteRuntimeException("." + name + " — у точек (x, y, z)");
                }
            case "first": {
                return c instanceof List<?> l && !l.isEmpty() ? l.get(0) : "";
            }
            case "last": {
                return c instanceof List<?> l && !l.isEmpty() ? l.get(l.size() - 1) : "";
            }
            case "keys": {
                return c instanceof Map<?, ?> m ? new ArrayList(m.keySet()) : new ArrayList();
            }
            case "values": {
                return c instanceof Map<?, ?> m ? new ArrayList<>(m.values()) : new ArrayList();
            }
            default: {
                if (c instanceof Map<?, ?> m) {
                    Object v = m.get(name);
                    return v == null ? "" : v;
                } else {
                    throw new SprauteRuntimeException("Неизвестное свойство ." + name);
                }
            }
        }
    }

    public static Object method(Object c, String name, List<Object> a) {
        if (c instanceof Map<?, ?> mm) {
            switch (name) {
                case "put":
                case "set":
                    need(name, a, 2);
                    ((Map<String, Object>)mm).put(Eval.str(a.get(0)), a.get(1));
                    return c;
                case "get": {
                    need(name, a, 1);
                    Object v = mm.get(Eval.str(a.get(0)));
                    return v != null ? v : (a.size() > 1 ? a.get(1) : "");
                }
                case "has":
                case "contains":
                    need(name, a, 1);
                    return mm.containsKey(Eval.str(a.get(0)));
                case "remove": {
                    need(name, a, 1);
                    Object v = mm.remove(Eval.str(a.get(0)));
                    return v == null ? "" : v;
                }
                case "clear":
                    mm.clear();
                    return c;
                case "keys":
                case "values":
                case "len":
                case "size":
                case "count":
                    return property(c, name);
                default:
                    throw new SprauteRuntimeException("У словаря нет метода ." + name + "(). Есть: put, get, has, remove, clear, keys, values");
            }
        } else if (!(c instanceof List<?> list)) {
            if (name.equals("has") || name.equals("contains")) {
                return Eval.str(c).toLowerCase().contains(Eval.str(a.isEmpty() ? "" : a.get(0)).toLowerCase());
            } else if (name.equals("split")) {
                return split(Eval.str(c), a.isEmpty() ? " " : Eval.str(a.get(0)));
            } else if (!name.equals("len") && !name.equals("size")) {
                throw new SprauteRuntimeException("." + name + "() работает со списками (var x = [])");
            } else {
                return property(c, name);
            }
        } else {
            List<Object> l = (List<Object>)list;
            switch (name) {
                case "add":
                case "push":
                    l.addAll(a);
                    return c;
                case "insert": {
                    need(name, a, 2);
                    int i = Math.max(0, Math.min(l.size(), idx(l, a.get(0))));
                    l.add(i, a.get(1));
                    return c;
                }
                case "remove": {
                    need(name, a, 1);

                    for (int i = 0; i < l.size(); i++) {
                        if (Eval.same(l.get(i), a.get(0))) {
                            l.remove(i);
                            return true;
                        }
                    }

                    return false;
                }
                case "removeat":
                case "removeAt": {
                    need(name, a, 1);
                    int i = idx(l, a.get(0));
                    return i >= 0 && i < l.size() ? l.remove(i) : "";
                }
                case "pop":
                    return l.isEmpty() ? "" : l.remove(l.size() - 1);
                case "clear":
                    l.clear();
                    return c;
                case "get": {
                    need(name, a, 1);
                    int i = idx(l, a.get(0));
                    return i >= 0 && i < l.size() ? l.get(i) : (a.size() > 1 ? a.get(1) : "");
                }
                case "set":
                    need(name, a, 2);
                    setIndex(l, a.get(0), a.get(1));
                    return c;
                case "has":
                case "contains":
                    need(name, a, 1);

                    for (Object x : l) {
                        if (Eval.same(x, a.get(0))) {
                            return true;
                        }
                    }

                    return false;
                case "indexof":
                case "indexOf": {
                    need(name, a, 1);

                    for (int i = 0; i < l.size(); i++) {
                        if (Eval.same(l.get(i), a.get(0))) {
                            return (double)i;
                        }
                    }

                    return -1.0;
                }
                case "sort":
                    l.sort(Values::compare);
                    return c;
                case "reverse":
                    Collections.reverse(l);
                    return c;
                case "shuffle":
                    Collections.shuffle(l, RND);
                    return c;
                case "random":
                case "pick":
                    return l.isEmpty() ? "" : l.get(RND.nextInt(l.size()));
                case "join":
                    return join(l, a.isEmpty() ? ", " : Eval.str(a.get(0)));
                case "len":
                case "size":
                case "count":
                case "first":
                case "last":
                    return property(c, name);
                case "sum":
                    double s = 0.0;

                    for (Object xx : l) {
                        s += Eval.num(xx);
                    }

                    return s;
                case "min":
                case "max":
                    if (l.isEmpty()) {
                        return 0.0;
                    }

                    double best = Eval.num(l.get(0));

                    for (Object x : l) {
                        best = name.equals("min") ? Math.min(best, Eval.num(x)) : Math.max(best, Eval.num(x));
                    }

                    return best;
                case "copy":
                    return new ArrayList<>(l);
                default:
                    throw new SprauteRuntimeException(
                        "У списка нет метода ."
                            + name
                            + "(). Есть: add, insert, remove, removeAt, pop, clear, get, set, has, indexOf, sort, reverse, shuffle, random, join, sum, min, max, copy"
                    );
            }
        }
    }

    private static void need(String name, List<Object> a, int n) {
        if (a.size() < n) {
            throw new SprauteRuntimeException("." + name + "(): нужно аргументов: " + n);
        }
    }

    public static List<Object> split(String s, String sep) {
        List<Object> out = new ArrayList<>();
        if (sep.isEmpty()) {
            s.codePoints().forEach(cp -> out.add(new String(Character.toChars(cp))));
            return out;
        } else {
            for (String p : s.split(Pattern.quote(sep), -1)) {
                out.add(p);
            }

            return out;
        }
    }

    public static String join(List<?> l, String sep) {
        StringBuilder sb = new StringBuilder();

        for (Object x : l) {
            if (sb.length() > 0) {
                sb.append(sep);
            }

            sb.append(Eval.str(x));
        }

        return sb.toString();
    }

    public static int compare(Object a, Object b) {
        try {
            return Double.compare(Eval.num(a), Eval.num(b));
        } catch (SprauteRuntimeException var3) {
            return Eval.str(a).compareTo(Eval.str(b));
        }
    }

    public static List<Object> range(double from, double to, double step) {
        if (step == 0.0) {
            throw new SprauteRuntimeException("range: шаг не может быть 0");
        } else {
            List<Object> out = new ArrayList<>();

            for (double v = from; step > 0.0 ? v < to : v > to; v += step) {
                out.add(v);
                if (out.size() > 100000) {
                    throw new SprauteRuntimeException("range: слишком длинный список");
                }
            }

            return out;
        }
    }

    public static List<Double> vec(Object v) {
        if (v instanceof EntityRef r) {
            v = WorldApi.list(r.require().position());
        }

        if (v instanceof List<?> l && l.size() >= 2) {
            List<Double> out = new ArrayList<>();

            for (Object x : l) {
                out.add(Eval.num(x));
            }

            return out;
        } else {
            throw new SprauteRuntimeException("Ожидалась точка (x, y, z)");
        }
    }

    public static List<Object> vecOp(Object a, Object b, char op) {
        List<Double> x = vec(a);
        List<Object> out = new ArrayList<>();
        if (op != '*' && op != '/') {
            List<Double> y = vec(b);

            for (int i = 0; i < Math.min(x.size(), y.size()); i++) {
                out.add(op == '+' ? x.get(i) + y.get(i) : x.get(i) - y.get(i));
            }

            return out;
        } else {
            double k = Eval.num(b);

            for (double v : x) {
                out.add(op == '*' ? v * k : v / k);
            }

            return out;
        }
    }

    public static double length(Object a) {
        double s = 0.0;

        for (double v : vec(a)) {
            s += v * v;
        }

        return Math.sqrt(s);
    }

    public static String format(Object v) {
        if (v instanceof List<?> l) {
            StringBuilder sb = new StringBuilder("[");

            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                Object x = l.get(i);
                sb.append(x instanceof String s ? "\"" + s + "\"" : Eval.str(x));
            }

            return sb.append(']').toString();
        } else if (v instanceof Map<?, ?> m) {
            StringBuilder sb = new StringBuilder("{");

            for (Entry<?, ?> e : m.entrySet()) {
                if (sb.length() > 1) {
                    sb.append(", ");
                }

                Object x = e.getValue();
                sb.append(e.getKey()).append(": ").append(x instanceof String s ? "\"" + s + "\"" : Eval.str(x));
            }

            return sb.append('}').toString();
        } else {
            return String.valueOf(v);
        }
    }

    public static Map<String, Object> map() {
        return new LinkedHashMap<>();
    }

    public static String toJson(Object v) {
        return "§json:" + GSON.toJson(plain(v));
    }

    private static Object plain(Object v) {
        if (v instanceof EntityRef r) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("$entity", r.uuid.toString());
            m.put("type", r.type);
            return m;
        } else if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>();

            for (Object x : l) {
                out.add(plain(x));
            }

            return out;
        } else if (!(v instanceof Map<?, ?> m)) {
            return v;
        } else {
            Map<String, Object> out = new LinkedHashMap<>();

            for (Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), plain(e.getValue()));
            }

            return out;
        }
    }

    public static Object fromJson(String s) {
        try {
            return normalize(GSON.fromJson(s.substring("§json:".length()), Object.class));
        } catch (Exception var2) {
            return s;
        }
    }

    private static Object normalize(Object v) {
        if (v instanceof List<?> l) {
            List<Object> out = new ArrayList<>();

            for (Object x : l) {
                out.add(normalize(x));
            }

            return out;
        } else if (!(v instanceof Map<?, ?> m)) {
            return v;
        } else {
            if (m.get("$entity") instanceof String id) {
                try {
                    return new EntityRef(UUID.fromString(id), String.valueOf(m.get("type")));
                } catch (IllegalArgumentException var5) {
                }
            }

            Map<String, Object> out = new LinkedHashMap<>();

            for (Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), normalize(e.getValue()));
            }

            return out;
        }
    }
}
