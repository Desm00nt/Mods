import java.io.*; import java.lang.reflect.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*;
public class BlockTest {
    public static void main(String[] a) throws Exception {
        List<URL> urls = new ArrayList<>(); for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
        ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        String P = "org.sprautestudio.spraute_engine.";
        Class<?> types = Class.forName(P + "editor.visual.BlockTypes", true, l);
        Class<?> vsC = Class.forName(P + "editor.visual.VisualScript", true, l);
        Class<?> blockC = Class.forName(P + "editor.visual.VisualScript$Block", true, l);
        Class<?> stackC = Class.forName(P + "editor.visual.VisualScript$Stack", true, l);
        Class<?> gen = Class.forName(P + "editor.visual.BlockCodegen", true, l);
        Method all = types.getMethod("all"), hidden = types.getMethod("hidden", String.class);
        Method create = vsC.getMethod("create", int.class, String.class), bcreate = blockC.getMethod("create", String.class);
        Method generate = gen.getMethod("generate", vsC);
        PrintStream out = new PrintStream(new FileOutputStream(a[1]), true, StandardCharsets.UTF_8);
        int n = 0, bad = 0;
        for (Object t : (Collection<?>) all.invoke(null)) {
            Class<?> tc = t.getClass();
            String key = (String) tc.getMethod("key").invoke(t);
            if ((Boolean) hidden.invoke(null, key) || (Boolean) tc.getMethod("isValue").invoke(t) || key.equals("start")) continue;
            n++;
            Object vs = create.invoke(null, 9400, "t");
            Object block = bcreate.invoke(null, key);
            List<Object> stacks = (List<Object>) vsC.getField("stacks").get(vs);
            if ((Boolean) tc.getMethod("isEvent").invoke(t)) {
                Object st = stackC.getDeclaredConstructor().newInstance();
                ((List<Object>) stackC.getField("blocks").get(st)).add(block);
                stacks.add(st);
            } else {
                ((List<Object>) stackC.getField("blocks").get(stacks.get(0))).add(block);
            }
            String text; List<String> msgs = new ArrayList<>();
            try {
                Object r = generate.invoke(null, vs);
                text = (String) r.getClass().getField("text").get(r);
                for (Object p : (List<?>) r.getClass().getField("problems").get(r)) msgs.add("PROBLEM " + p);
                Object an = r.getClass().getField("analysis").get(r);
                String[] lines = text.split("\n", -1);
                for (Object d : (List<?>) an.getClass().getField("diagnostics").get(an)) {
                    if (!String.valueOf(d.getClass().getField("severity").get(d)).equals("ERROR")) continue;
                    int line = d.getClass().getField("line").getInt(d);
                    msgs.add("ERROR [" + lines[Math.min(line, lines.length - 1)].trim() + "] " + d.getClass().getField("message").get(d));
                }
            } catch (Throwable e) {
                while (e.getCause() != null) e = e.getCause();
                msgs.add("THROW " + e);
            }
            if (!msgs.isEmpty()) bad++;
            out.println("== " + key + "\t" + String.join(" | ", msgs));
        }
        out.println("blocks=" + n + " withErrors=" + bad);
    }
}
