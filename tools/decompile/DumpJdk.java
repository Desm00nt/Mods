import java.lang.reflect.*;
import java.io.*;
import java.util.*;
public class DumpJdk {
    static String d(Class<?> c) {
        if (c.isArray()) return "[" + d(c.getComponentType());
        if (c == int.class) return "I"; if (c == long.class) return "J"; if (c == boolean.class) return "Z";
        if (c == byte.class) return "B"; if (c == char.class) return "C"; if (c == short.class) return "S";
        if (c == float.class) return "F"; if (c == double.class) return "D"; if (c == void.class) return "V";
        return "L" + c.getName().replace('.', '/') + ";";
    }
    public static void main(String[] a) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim(); if (line.isEmpty()) continue;
            Class<?> c;
            try { c = Class.forName(line, false, DumpJdk.class.getClassLoader()); } catch (Throwable t) { System.out.println("!" + line); continue; }
            System.out.println("#" + line + " " + (c.isInterface() ? "interface" : "class"));
            Set<String> out = new TreeSet<>();
            List<Class<?>> todo = new ArrayList<>(); todo.add(c); Set<Class<?>> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                Class<?> k = todo.remove(0); if (k == null || !seen.add(k)) continue;
                for (Method m : k.getDeclaredMethods()) {
                    int mod = m.getModifiers();
                    if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) continue;
                    StringBuilder sb = new StringBuilder(m.getName()).append('(');
                    for (Class<?> p : m.getParameterTypes()) sb.append(d(p));
                    sb.append(')').append(d(m.getReturnType()));
                    out.add((Modifier.isFinal(mod) ? "F " : "  ") + sb);
                }
                for (Field f : k.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (Modifier.isPublic(mod) || Modifier.isProtected(mod)) out.add("= " + f.getName() + ":" + d(f.getType()));
                }
                todo.add(k.getSuperclass()); todo.addAll(Arrays.asList(k.getInterfaces()));
                if (k.isInterface()) todo.add(Object.class);
            }
            for (String s : out) System.out.println(s);
            // constructors
            for (Constructor<?> ct : c.getDeclaredConstructors()) {
                int mod = ct.getModifiers();
                if (!Modifier.isPublic(mod) && !Modifier.isProtected(mod)) continue;
                StringBuilder sb = new StringBuilder("<init>(");
                for (Class<?> p : ct.getParameterTypes()) sb.append(d(p));
                System.out.println("C " + sb.append(")V"));
            }
        }
    }
}
