import java.io.*; import java.lang.reflect.*; import java.net.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
public class Ana { public static void main(String[] a) throws Exception {
    List<URL> urls = new ArrayList<>(); for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
    ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    Method an = Class.forName("org.sprautestudio.spraute_engine.lang.Analyzer", true, l).getMethod("analyze", String.class);
    List<Path> fs = new ArrayList<>(); try (DirectoryStream<Path> ds = Files.newDirectoryStream(Paths.get(a[1]))) { for (Path p : ds) fs.add(p); }
    Collections.sort(fs);
    for (Path p : fs) {
        String src = Files.readString(p, StandardCharsets.UTF_8);
        Object r = an.invoke(null, src);
        List<?> diags = (List<?>) r.getClass().getField("diagnostics").get(r);
        String[] lines = src.split("\n", -1);
        for (Object d : diags) {
            Class<?> dc = d.getClass();
            int line = dc.getField("line").getInt(d);
            System.out.println(p.getFileName() + "\t" + dc.getField("severity").get(d) + "\t" + lines[Math.min(line, lines.length - 1)] + "\t" + dc.getField("message").get(d));
        }
    }
} }
