import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Initialise every class of a build and dump all its static fields; used to diff original vs recompiled builds. */
public class StaticDiff {
    static String dump(Object o, IdentityHashMap<Object, Boolean> seen, int depth) {
        if (o == null) return "null";
        if (depth > 60) return "<deep>";
        Class<?> c = o.getClass();
        if (o instanceof String s) return '"' + s.replace("\n", "\\n") + '"';
        if (o instanceof Number || o instanceof Boolean || o instanceof Character) return c.getSimpleName() + ":" + o;
        if (o instanceof Enum<?> e) return e.getDeclaringClass().getSimpleName() + "." + e.name();
        if (c.isSynthetic() || c.getName().contains("$$Lambda") || c.isAnonymousClass()) return "<fn>";
        if (seen.containsKey(o)) return "<cycle>";
        if (c.isArray()) {
            seen.put(o, true);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < Array.getLength(o); i++) sb.append(i > 0 ? "," : "").append(dump(Array.get(o, i), seen, depth + 1));
            seen.remove(o);
            return sb.append("]").toString();
        }
        if (o instanceof Collection<?> col) {
            seen.put(o, true);
            List<String> parts = new ArrayList<>();
            for (Object x : col) parts.add(dump(x, seen, depth + 1));
            if (o instanceof Set<?>) Collections.sort(parts);
            seen.remove(o);
            return "[" + String.join(",", parts) + "]";
        }
        if (o instanceof Map<?, ?> m) {
            seen.put(o, true);
            List<String> parts = new ArrayList<>();
            for (Map.Entry<?, ?> e : m.entrySet()) parts.add(dump(e.getKey(), seen, depth + 1) + "=" + dump(e.getValue(), seen, depth + 1));
            if (!(o instanceof LinkedHashMap<?, ?>) && !(o instanceof SortedMap<?, ?>)) Collections.sort(parts);
            seen.remove(o);
            return "{" + String.join(",", parts) + "}";
        }
        if (!c.getName().startsWith("org.sprautestudio")) return c.getSimpleName() + "(" + o + ")";
        seen.put(o, true);
        List<Field> fields = new ArrayList<>();
        for (Class<?> k = c; k != null && k != Object.class; k = k.getSuperclass())
            for (Field f : k.getDeclaredFields()) if (!Modifier.isStatic(f.getModifiers()) && !f.isSynthetic()) fields.add(f);
        fields.sort(Comparator.comparing(f -> f.getDeclaringClass().getName() + "#" + f.getName()));
        StringBuilder sb = new StringBuilder(c.getName().substring(c.getName().lastIndexOf('.') + 1)).append("{");
        boolean first = true;
        for (Field f : fields) {
            try { f.setAccessible(true); } catch (RuntimeException ex) { continue; }
            Object v;
            try { v = f.get(o); } catch (IllegalAccessException ex) { v = "<noaccess>"; }
            sb.append(first ? "" : ",").append(f.getName()).append("=").append(dump(v, seen, depth + 1));
            first = false;
        }
        seen.remove(o);
        return sb.append("}").toString();
    }


    static String err(Throwable t) {
        while ((t instanceof InvocationTargetException || t instanceof ExceptionInInitializerError) && t.getCause() != null) t = t.getCause();
        return "THROW " + t.getClass().getName() + ": " + t.getMessage();
    }

    public static void main(String[] a) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
        URL[] cp = urls.toArray(new URL[0]);
        List<String> names = new ArrayList<>();
        try (BufferedReader r = new BufferedReader(new FileReader(a[1]))) { for (String l; (l = r.readLine()) != null;) names.add(l.trim()); }
        PrintStream out = new PrintStream(new FileOutputStream(a[2]), true, StandardCharsets.UTF_8);
        PrintStream realErr = System.err, realOut = System.out;
        PrintStream sink = new PrintStream(OutputStream.nullOutputStream());
        for (String n : names) {
            StringBuilder sb = new StringBuilder("== " + n + "\n");
            try (URLClassLoader l = new URLClassLoader(cp, ClassLoader.getPlatformClassLoader())) {
                System.setOut(sink); System.setErr(sink);
                try {
                    Class<?> c = Class.forName(n, true, l);
                    List<Field> fs = new ArrayList<>(Arrays.asList(c.getDeclaredFields()));
                    fs.sort(Comparator.comparing(Field::getName));
                    for (Field f : fs) {
                        if (!Modifier.isStatic(f.getModifiers()) || f.isSynthetic() || f.getName().startsWith("$")) continue;
                        String v;
                        try { f.setAccessible(true); v = dump(f.get(null), new IdentityHashMap<>(), 0); } catch (Throwable t) { v = err(t); }
                        sb.append("  ").append(f.getName()).append(" = ").append(v.replaceAll("@[0-9a-f]{4,}", "@H")).append("\n");
                    }
                } catch (Throwable t) {
                    sb.append("  ").append(err(t).replaceAll("@[0-9a-f]{4,}", "@H")).append("\n");
                } finally { System.setOut(realOut); System.setErr(realErr); }
            }
            out.print(sb);
        }
    }
}
