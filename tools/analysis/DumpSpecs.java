import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DumpSpecs {
    public static void main(String[] a) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
        ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        Class<?> specs = Class.forName("org.sprautestudio.spraute_engine.lang.Specs", true, l);
        PrintStream out = new PrintStream(new FileOutputStream(a[1]), true, StandardCharsets.UTF_8);
        Collection<?> cmds = (Collection<?>) specs.getMethod("commands").invoke(null);
        Method fullOnly = specs.getMethod("fullOnly", Class.forName("org.sprautestudio.spraute_engine.lang.CommandSpec", false, l));
        for (Object c : cmds) {
            Class<?> cc = c.getClass();
            String key = (String) cc.getField("key").get(c), cat = (String) cc.getField("category").get(c);
            String syn = (String) cc.getField("syntax").get(c), doc = (String) cc.getField("doc").get(c);
            boolean full = (Boolean) fullOnly.invoke(null, c);
            out.println("C\t" + cat + "\t" + key + "\t" + (full ? "FULL" : "") + "\t" + syn.replace('\n', ' ') + "\t" + doc.replace('\n', ' '));
        }
        Collection<?> fns = (Collection<?>) specs.getMethod("functions").invoke(null);
        for (Object f : fns) {
            Class<?> fc = f.getClass();
            out.println("F\t" + fc.getMethod("signature").invoke(f) + "\t" + fc.getMethod("returns").invoke(f) + "\t" + String.valueOf(fc.getMethod("doc").invoke(f)).replace('\n', ' '));
        }
        Map<?, ?> kw = (Map<?, ?>) specs.getMethod("keywordDocs").invoke(null);
        for (Map.Entry<?, ?> e : kw.entrySet()) out.println("K\t" + e.getKey() + "\t" + String.valueOf(e.getValue()).replace('\n', ' '));
        for (String list : new String[]{"EMOTIONS", "LAYERS", "MOVE_TYPES", "KEYS", "ENGINE_SYSTEMS"}) {
            out.println("L\t" + list + "\t" + String.join(", ", (List<String>) specs.getField(list).get(null)));
        }
        Class<?> ev = Class.forName("org.sprautestudio.spraute_engine.lang.EventDefs", true, l);
        for (Field f : ev.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) { f.setAccessible(true); Object v = f.get(null); out.println("EV-FIELD\t" + f.getName() + "\t" + (v == null ? "null" : v.getClass().getName() + " size=" + (v instanceof Collection<?> col ? col.size() : v instanceof Map<?,?> m ? m.size() : -1))); }
        }
    }
}
