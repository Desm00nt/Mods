import java.io.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.util.*;
public class Demo { public static void main(String[] a) throws Exception {
    List<URL> urls = new ArrayList<>(); for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
    ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    Object s = Class.forName("org.sprautestudio.spraute_engine.editor.project.ScriptTemplates", true, l).getMethod("demo", int.class).invoke(null, 5001);
    try (Writer w = new OutputStreamWriter(new FileOutputStream(a[1]), StandardCharsets.UTF_8)) { w.write((String) s); }
} }
