import java.io.*; import java.lang.reflect.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*;
public class DumpEvents {
    public static void main(String[] a) throws Exception {
        List<URL> urls = new ArrayList<>(); for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
        ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        Class<?> ev = Class.forName("org.sprautestudio.spraute_engine.lang.EventDefs", true, l);
        Field f = ev.getDeclaredField("ALL"); f.setAccessible(true);
        PrintStream out = new PrintStream(new FileOutputStream(a[1]), true, StandardCharsets.UTF_8);
        for (Map.Entry<?, ?> e : ((Map<?, ?>) f.get(null)).entrySet()) {
            Object d = e.getValue(); StringBuilder sb = new StringBuilder("E\t" + e.getKey());
            for (RecordComponent rc : d.getClass().getRecordComponents()) { Object v = rc.getAccessor().invoke(d); if (v instanceof String s) sb.append("\t").append(rc.getName()).append("=").append(s.replace('\n',' ')); }
            out.println(sb);
        }
    }
}
