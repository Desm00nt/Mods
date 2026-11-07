import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Runs Lexer/Parser/Analyzer (+ Eval on expressions) of a given build of the mod and dumps everything canonically. */
public class DiffTest {
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
        ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        Class<?> analyzer = Class.forName("org.sprautestudio.spraute_engine.lang.Analyzer", true, l);
        Method analyze = analyzer.getMethod("analyze", String.class);
        Class<?> parser = Class.forName("org.sprautestudio.spraute_engine.lang.Parser", true, l);
        Method parseExpr = parser.getMethod("parseExpression", String.class);
        Class<?> evalC = Class.forName("org.sprautestudio.spraute_engine.runtime.Eval", true, l);
        Constructor<?> evalCtor = evalC.getDeclaredConstructors()[0];
        evalCtor.setAccessible(true);
        Method eval = null;
        for (Method m : evalC.getMethods()) if (m.getName().equals("eval") && m.getParameterCount() == 1) eval = m;
        PrintStream out = new PrintStream(new FileOutputStream(a[3]), true, StandardCharsets.UTF_8);
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(a[1]))) { for (Path p : ds) files.add(p); }
        Collections.sort(files);
        for (Path p : files) {
            String src = Files.readString(p, StandardCharsets.UTF_8);
            String res;
            try { res = dump(analyze.invoke(null, src), new IdentityHashMap<>(), 0); } catch (Throwable t) { res = err(t); }
            out.println("== " + p.getFileName() + "\n" + res);
        }
        for (String ex : Files.readAllLines(Paths.get(a[2]), StandardCharsets.UTF_8)) {
            String ast, val;
            Object e = null;
            try { e = parseExpr.invoke(null, ex); ast = dump(e, new IdentityHashMap<>(), 0); } catch (Throwable t) { ast = err(t); }
            try { Object ev = evalCtor.newInstance(new Object[]{null}); val = dump(eval.invoke(ev, e), new IdentityHashMap<>(), 0); } catch (Throwable t) { val = err(t); }
            out.println("== expr " + ex + "\nAST " + ast + "\nVAL " + val);
        }
    }
}
