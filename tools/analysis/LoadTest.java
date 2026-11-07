import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class LoadTest {
    public static void main(String[] args) throws Exception {
        String modJar = args[0];
        List<URL> urls = new ArrayList<>();
        urls.add(new File(modJar).toURI().toURL());
        for (int i = 1; i < args.length; i++) urls.add(new File(args[i]).toURI().toURL());
        URL[] cp = urls.toArray(new URL[0]);
        List<String> names = new ArrayList<>();
        try (ZipFile z = new ZipFile(modJar)) {
            for (Enumeration<? extends ZipEntry> e = z.entries(); e.hasMoreElements();) {
                String n = e.nextElement().getName();
                if (!n.endsWith(".class") || !n.startsWith("org/sprautestudio")) continue;
                String c = n.substring(0, n.length() - 6);
                String[] parts = c.split("/");
                boolean clientPkg = Arrays.asList(parts).contains("client") || parts[3].equals("editor") || parts[3].equals("ui") || parts[3].equals("mixin");
                if (clientPkg) continue;
                names.add(c.replace('/', '.'));
            }
        }
        Collections.sort(names);
        int ok = 0, init = 0, bad = 0;
        for (String n : names) {
            try (URLClassLoader l = new URLClassLoader(cp, ClassLoader.getPlatformClassLoader())) {
                try {
                    Class.forName(n, true, l);
                    ok++;
                } catch (ExceptionInInitializerError e) {
                    init++; // static init hit a stub — verification itself passed
                } catch (Throwable t) {
                    String msg = String.valueOf(t.getMessage());
                    boolean client = msg.contains("net/minecraft/client") || msg.contains("blaze3d") || msg.contains("minecraftforge/client") || msg.contains("lwjgl");
                    if (client) {
                        bad++;
                        System.out.println("FAIL " + n + " : " + t.getClass().getSimpleName() + ": " + msg);
                    } else {
                        System.out.println("OTHER " + n + " : " + t.getClass().getSimpleName() + ": " + msg);
                    }
                }
            }
        }
        System.out.println("classes=" + names.size() + " ok=" + ok + " initFail(stub)=" + init + " clientFail=" + bad);
    }
}
