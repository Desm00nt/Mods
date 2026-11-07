package org.sprautestudio.spraute_engine.export;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public final class ModExport {
    public static final String MANIFEST_FOLDER = "META-INF";
    public static final String MANIFEST_FILE = "spraute_project.json";
    public static final String FILES_ROOT = "spraute_project";
    public static final int FORMAT = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String ENGINE_ID = "spraute_engine";
    private static final String FORGE_MOD = "Lnet/minecraftforge/fml/common/Mod;";
    private static final String FORGE_LOADER_RANGE = "[43,)";
    private static final String[][] LATIN = new String[][]{
        {"а", "a"},
        {"б", "b"},
        {"в", "v"},
        {"г", "g"},
        {"д", "d"},
        {"е", "e"},
        {"ё", "e"},
        {"ж", "zh"},
        {"з", "z"},
        {"и", "i"},
        {"й", "y"},
        {"к", "k"},
        {"л", "l"},
        {"м", "m"},
        {"н", "n"},
        {"о", "o"},
        {"п", "p"},
        {"р", "r"},
        {"с", "s"},
        {"т", "t"},
        {"у", "u"},
        {"ф", "f"},
        {"х", "h"},
        {"ц", "ts"},
        {"ч", "ch"},
        {"ш", "sh"},
        {"щ", "sch"},
        {"ъ", ""},
        {"ы", "y"},
        {"ь", ""},
        {"э", "e"},
        {"ю", "yu"},
        {"я", "ya"}
    };

    private ModExport() {
    }

    public static String suggestId(String name) {
        String s = name.toLowerCase(Locale.ROOT);

        for (String[] pair : LATIN) {
            s = s.replace(pair[0], pair[1]);
        }

        s = s.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (s.length() > 40) {
            s = s.substring(0, 40).replaceAll("_+$", "");
        }

        if (s.isEmpty() || !Character.isLetter(s.charAt(0))) {
            s = "story_" + s;
        }

        return s.length() < 2 ? "story" : s;
    }

    public static String checkId(String id) {
        if (id == null || !id.matches("[a-z][a-z0-9_]{1,63}")) {
            return "Id: от 2 до 64 символов, латинские буквы, цифры и _, начинается с буквы";
        } else {
            return !id.equals("spraute_engine") && !id.equals("minecraft") && !id.equals("forge") && !id.equals("fabric") && !id.equals("java")
                ? null
                : "Этот id занят";
        }
    }

    public static String checkVersion(String version) {
        return version != null && version.matches("[0-9]+(\\.[0-9]+)*([-+][0-9A-Za-z.-]+)?") ? null : "Версия: числа через точку, например 1.0.0";
    }

    public static ModExport.Result write(ModExport.Spec spec, Path out) throws IOException {
        String bad = checkId(spec.id);
        if (bad == null) {
            bad = checkVersion(spec.version);
        }

        if (bad != null) {
            throw new IOException(bad);
        } else if (!Files.isDirectory(spec.projectDir)) {
            throw new IOException("Нет папки проекта " + spec.projectDir);
        } else if (!spec.nested.isEmpty() && spec.loader == null) {
            throw new IOException("Движок можно вложить только для определённого загрузчика");
        } else {
            List<Path> files = projectFiles(spec.projectDir);
            Path temp = out.resolveSibling(out.getFileName() + ".tmp");
            Files.createDirectories(out.toAbsolutePath().getParent());
            long bytes = 0L;

            try (
                OutputStream raw = Files.newOutputStream(temp);
                JarOutputStream jar = new JarOutputStream(raw, manifest(spec));
            ) {
                String hash = hash(spec.projectDir, files);
                text(jar, "META-INF/spraute_project.json", projectManifest(spec, hash));
                text(jar, "pack.mcmeta", packMeta(spec));
                bytes(jar, entryClassPath(spec.id), entryClass(spec.id));
                if (spec.loader != ModExport.Loader.FABRIC) {
                    text(jar, "META-INF/mods.toml", modsToml(spec));
                }

                if (spec.loader != ModExport.Loader.FORGE) {
                    text(jar, "fabric.mod.json", fabricJson(spec));
                }

                if (spec.loader == ModExport.Loader.FORGE && !spec.nested.isEmpty()) {
                    text(jar, "META-INF/jarjar/metadata.json", jarJarMetadata(spec));

                    for (ModExport.Nested n : spec.nested) {
                        file(jar, "META-INF/jarjar/" + nestedName(n), n.jar());
                    }
                }

                if (spec.loader == ModExport.Loader.FABRIC) {
                    for (ModExport.Nested n : spec.nested) {
                        file(jar, "META-INF/jars/" + nestedName(n), n.jar());
                    }
                }

                for (Path f : files) {
                    bytes += Files.size(f);
                    file(jar, "spraute_project/" + relative(spec.projectDir, f), f);
                }
            } catch (RuntimeException | IOException var16) {
                Files.deleteIfExists(temp);
                throw var16;
            }

            Files.move(temp, out, StandardCopyOption.REPLACE_EXISTING);
            return new ModExport.Result(out, files.size(), bytes);
        }
    }

    static List<Path> projectFiles(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();

        try (Stream<Path> all = Files.walk(dir)) {
            for (Path f : (Iterable<Path>) all.filter(x$0 -> Files.isRegularFile(x$0)).sorted()::iterator) {
                boolean hidden = false;

                for (Path part : dir.relativize(f)) {
                    hidden |= part.toString().startsWith(".");
                }

                if (!hidden) {
                    out.add(f);
                }
            }
        }

        return out;
    }

    private static String relative(Path dir, Path f) {
        return dir.relativize(f).toString().replace('\\', '/');
    }

    static String hash(Path dir, List<Path> files) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException var10) {
            throw new IllegalStateException(var10);
        }

        byte[] buffer = new byte[65536];

        for (Path f : files) {
            digest.update(relative(dir, f).getBytes(StandardCharsets.UTF_8));

            try (InputStream in = new DigestInputStream(Files.newInputStream(f), digest)) {
                while (in.read(buffer) > 0) {
                }
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private static void text(JarOutputStream jar, String path, String content) throws IOException {
        bytes(jar, path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static void bytes(JarOutputStream jar, String path, byte[] content) throws IOException {
        jar.putNextEntry(new ZipEntry(path));
        jar.write(content);
        jar.closeEntry();
    }

    private static void file(JarOutputStream jar, String path, Path source) throws IOException {
        jar.putNextEntry(new ZipEntry(path));
        Files.copy(source, jar);
        jar.closeEntry();
    }

    private static Manifest manifest(ModExport.Spec spec) {
        Manifest m = new Manifest();
        Attributes a = m.getMainAttributes();
        a.put(Name.MANIFEST_VERSION, "1.0");
        a.putValue("Automatic-Module-Name", "spraute.exported.mod_" + spec.id);
        a.putValue("Implementation-Title", spec.id);
        a.putValue("Implementation-Version", spec.version);
        return m;
    }

    private static String nestedName(ModExport.Nested n) {
        return n.modId() + "-" + n.version() + ".jar";
    }

    static String projectManifest(ModExport.Spec spec, String hash) {
        JsonObject o = new JsonObject();
        o.addProperty("format", 1);
        o.addProperty("id", spec.id);
        o.addProperty("project", spec.project);
        o.addProperty("folder", spec.projectDir.getFileName().toString());
        o.addProperty("version", spec.version);
        o.addProperty("hash", hash);
        o.addProperty("editor", spec.editor);
        return GSON.toJson((JsonElement)o);
    }

    private static String packMeta(ModExport.Spec spec) {
        JsonObject pack = new JsonObject();
        pack.addProperty("description", spec.name);
        pack.addProperty("pack_format", 15);
        JsonObject o = new JsonObject();
        o.add("pack", pack);
        return GSON.toJson((JsonElement)o);
    }

    private static String toml(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "") + "\"";
    }

    static String modsToml(ModExport.Spec s) {
        StringBuilder t = new StringBuilder();
        t.append("modLoader = \"javafml\"\nloaderVersion = ").append(toml("[43,)")).append("\nlicense = \"All Rights Reserved\"\n\n[[mods]]\n");
        t.append("modId = ").append(toml(s.id)).append("\nversion = ").append(toml(s.version)).append("\ndisplayName = ").append(toml(s.name)).append('\n');
        if (!s.author.isBlank()) {
            t.append("authors = ").append(toml(s.author)).append('\n');
        }

        t.append("description = ").append(toml(s.description.isBlank() ? s.name : s.description)).append("\n\n");
        t.append("[[dependencies.")
            .append(s.id)
            .append("]]\nmodId = ")
            .append(toml("spraute_engine"))
            .append("\nmandatory = true\nversionRange = ")
            .append(toml("[" + s.engineVersion + ",)"))
            .append("\nordering = \"AFTER\"\nside = \"BOTH\"\n");
        if (s.minecraft != null) {
            t.append("\n[[dependencies.")
                .append(s.id)
                .append("]]\nmodId = \"minecraft\"\nmandatory = true\nversionRange = ")
                .append(toml("[" + s.minecraft + "]"))
                .append("\nordering = \"NONE\"\nside = \"BOTH\"\n");
        }

        return t.toString();
    }

    static String fabricJson(ModExport.Spec s) {
        JsonObject o = new JsonObject();
        o.addProperty("schemaVersion", 1);
        o.addProperty("id", s.id);
        o.addProperty("version", s.version);
        o.addProperty("name", s.name);
        o.addProperty("description", s.description.isBlank() ? s.name : s.description);
        if (!s.author.isBlank()) {
            JsonArray authors = new JsonArray();
            authors.add(s.author);
            o.add("authors", authors);
        }

        o.addProperty("environment", "*");
        JsonObject depends = new JsonObject();
        depends.addProperty("spraute_engine", ">=" + s.engineVersion);
        if (s.minecraft != null) {
            depends.addProperty("minecraft", s.minecraft);
        }

        o.add("depends", depends);
        if (s.loader == ModExport.Loader.FABRIC && !s.nested.isEmpty()) {
            JsonArray jars = new JsonArray();

            for (ModExport.Nested n : s.nested) {
                JsonObject j = new JsonObject();
                j.addProperty("file", "META-INF/jars/" + nestedName(n));
                jars.add(j);
            }

            o.add("jars", jars);
        }

        return GSON.toJson((JsonElement)o);
    }

    static String jarJarMetadata(ModExport.Spec s) {
        JsonArray jars = new JsonArray();

        for (ModExport.Nested n : s.nested) {
            JsonObject identifier = new JsonObject();
            identifier.addProperty("group", "org.sprautestudio");
            identifier.addProperty("artifact", n.modId());
            JsonObject version = new JsonObject();
            version.addProperty("range", "[" + n.version() + ",)");
            version.addProperty("artifactVersion", n.version());
            JsonObject j = new JsonObject();
            j.add("identifier", identifier);
            j.add("version", version);
            j.addProperty("path", "META-INF/jarjar/" + nestedName(n));
            j.addProperty("isObfuscated", false);
            jars.add(j);
        }

        JsonObject o = new JsonObject();
        o.add("jars", jars);
        return GSON.toJson((JsonElement)o);
    }

    static String entryClassName(String id) {
        return "org/sprautestudio/exported/mod_" + id + "/Entry";
    }

    static String entryClassPath(String id) {
        return entryClassName(id) + ".class";
    }

    static byte[] entryClass(String id) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(-889275714);
            out.writeShort(0);
            out.writeShort(52);
            out.writeShort(15);
            utf(out, entryClassName(id));
            classRef(out, 1);
            utf(out, "java/lang/Object");
            classRef(out, 3);
            utf(out, "<init>");
            utf(out, "()V");
            out.writeByte(12);
            out.writeShort(5);
            out.writeShort(6);
            out.writeByte(10);
            out.writeShort(4);
            out.writeShort(7);
            utf(out, "Code");
            utf(out, "RuntimeVisibleAnnotations");
            utf(out, "Lnet/minecraftforge/fml/common/Mod;");
            utf(out, "value");
            utf(out, id);
            utf(out, "Entry.java");
            out.writeShort(49);
            out.writeShort(2);
            out.writeShort(4);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(1);
            out.writeShort(1);
            out.writeShort(5);
            out.writeShort(6);
            out.writeShort(1);
            out.writeShort(9);
            byte[] code = new byte[]{42, -73, 0, 8, -79};
            out.writeInt(12 + code.length);
            out.writeShort(1);
            out.writeShort(1);
            out.writeInt(code.length);
            out.write(code);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(1);
            out.writeShort(10);
            out.writeInt(11);
            out.writeShort(1);
            out.writeShort(11);
            out.writeShort(1);
            out.writeShort(12);
            out.writeByte(115);
            out.writeShort(13);
            return bytes.toByteArray();
        } catch (IOException var4) {
            throw new IllegalStateException(var4);
        }
    }

    private static void utf(DataOutputStream out, String s) throws IOException {
        out.writeByte(1);
        out.writeUTF(s);
    }

    private static void classRef(DataOutputStream out, int name) throws IOException {
        out.writeByte(7);
        out.writeShort(name);
    }

    public static Map<String, String> readManifest(String json) {
        JsonObject o = GSON.fromJson(json, JsonObject.class);
        return Map.of(
            "id",
            o.get("id").getAsString(),
            "project",
            o.get("project").getAsString(),
            "folder",
            o.get("folder").getAsString(),
            "version",
            o.get("version").getAsString(),
            "hash",
            o.get("hash").getAsString(),
            "editor",
            o.has("editor") ? o.get("editor").getAsString() : "true"
        );
    }

    public static enum Loader {
        FORGE,
        FABRIC;
    }

    public record Nested(String modId, String version, Path jar) {
    }

    public record Result(Path jar, int files, long bytes) {
    }

    public static final class Spec {
        public String id;
        public String name;
        public String version = "1.0.0";
        public String author = "";
        public String description = "";
        public String project;
        public Path projectDir;
        public String engineVersion;
        public boolean editor = true;
        public ModExport.Loader loader;
        public String minecraft;
        public final List<ModExport.Nested> nested = new ArrayList<>();
    }
}
