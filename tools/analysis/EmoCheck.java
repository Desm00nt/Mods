import java.io.*; import java.lang.reflect.*; import java.net.*; import java.util.*;
public class EmoCheck { public static void main(String[] a) throws Exception {
    List<URL> urls = new ArrayList<>(); for (String s : a[0].split(":")) urls.add(new File(s).toURI().toURL());
    ClassLoader l = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    Class<?> e = Class.forName("org.sprautestudio.spraute_engine.core.anim.Emotions", true, l);
    Field clips = e.getDeclaredField("CLIPS"); clips.setAccessible(true);
    Map<?,?> m = (Map<?,?>) clips.get(null);
    System.out.println("NAMES = " + e.getField("NAMES").get(null));
    System.out.println("CLIPS (" + m.size() + ") = " + m.keySet());
    Class<?> sp = Class.forName("org.sprautestudio.spraute_engine.lang.Specs", true, l);
    System.out.println("Specs.EMOTIONS = " + sp.getField("EMOTIONS").get(null));
} }
