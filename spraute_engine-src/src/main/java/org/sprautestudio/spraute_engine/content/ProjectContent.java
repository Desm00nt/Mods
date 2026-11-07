package org.sprautestudio.spraute_engine.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.recipe.RecipeOverrides;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public final class ProjectContent {
    private static final List<String> FOLDERS = List.of("entities/", "models/", "animations/", "images/", "sounds/", "recipes/");
    private static final Set<String> EXTENSIONS = Set.of(".sent", ".json", ".png", ".jpg", ".jpeg", ".ogg", ".wav");
    private static final long MAX_FILE = 33554432L;
    public static final int UP_PIECE = 24576;
    public static final int DOWN_PIECE = 49152;
    private static final int PIECES_PER_TICK = 3;
    private static final Map<String, ProjectContent.Upload> UPLOADS = new HashMap<>();
    private static final Map<UUID, Deque<Packets.ContentData>> OUTGOING = new HashMap<>();
    private static final ExecutorService LOCAL_COPIES = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "Spraute local content sync");
        thread.setDaemon(true);
        return thread;
    });

    private ProjectContent() {
    }

    public static boolean allowed(String path) {
        if (path == null || path.isEmpty() || path.contains("..") || path.contains(":") || path.startsWith("/") || path.contains("\\")) {
            return false;
        } else if (path.equals("project.json")) {
            return true;
        } else if (FOLDERS.stream().noneMatch(path::startsWith)) {
            return false;
        } else {
            int dot = path.lastIndexOf(46);
            return dot > 0 && EXTENSIONS.contains(path.substring(dot).toLowerCase(Locale.ROOT));
        }
    }

    public static Map<String, ProjectContent.Entry> manifest(Path dir) throws IOException {
        Map<String, ProjectContent.Entry> out = new LinkedHashMap<>();
        if (!Files.isDirectory(dir)) {
            return out;
        } else {
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path f : (Iterable<Path>) files.filter(x$0 -> Files.isRegularFile(x$0)).sorted()::iterator) {
                    String rel = dir.relativize(f).toString().replace('\\', '/');
                    if (allowed(rel) && Files.size(f) <= 33554432L) {
                        out.put(rel, new ProjectContent.Entry(rel, Files.size(f), hash(Files.readAllBytes(f))));
                    }
                }
            }

            return out;
        }
    }

    public static String hash(byte[] data) {
        try {
            StringBuilder sb = new StringBuilder();

            for (byte b : MessageDigest.getInstance("SHA-1").digest(data)) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException var6) {
            throw new IllegalStateException(var6);
        }
    }

    public static void removeEmpty(Path dir) throws IOException {
        if (Files.isDirectory(dir)) {
            try (Stream<Path> all = Files.walk(dir)) {
                for (Path p : (Iterable<Path>) all.sorted(Comparator.reverseOrder())::iterator) {
                    if (!p.equals(dir) && Files.isDirectory(p)) {
                        try (Stream<Path> inside = Files.list(p)) {
                            if (inside.findAny().isEmpty()) {
                                Files.delete(p);
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<String> lines(Map<String, ProjectContent.Entry> m) {
        List<String> out = new ArrayList<>();

        for (ProjectContent.Entry e : m.values()) {
            out.add(e.line());
        }

        return out;
    }

    public static Path root() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("content");
    }

    private static Path dir(String project) {
        return root().resolve(Images.slug(project));
    }

    public static void copyLocal(MinecraftServer server, String project, Path source, Runnable done) {
        Path target = server.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("content").resolve(Images.slug(project));
        CompletableFuture.<Boolean>supplyAsync(() -> {
            try {
                Files.createDirectories(target);
                Map<String, ProjectContent.Entry> have = manifest(target);
                Map<String, ProjectContent.Entry> offered = manifest(source);
                boolean changed = false;

                for (String old : have.keySet()) {
                    if (!offered.containsKey(old)) {
                        Files.deleteIfExists(target.resolve(old));
                        changed = true;
                    }
                }

                for (ProjectContent.Entry entry : offered.values()) {
                    ProjectContent.Entry current = have.get(entry.path());
                    if (current == null || !current.hash().equals(entry.hash())) {
                        Path destination = target.resolve(entry.path());
                        Files.createDirectories(destination.getParent());
                        Files.copy(source.resolve(entry.path()), destination, StandardCopyOption.REPLACE_EXISTING);
                        changed = true;
                    }
                }

                removeEmpty(target);
                return changed;
            } catch (IOException var9) {
                throw new CompletionException(var9);
            }
        }, LOCAL_COPIES).whenComplete((changed, error) -> server.execute(() -> {
            if (Platform.get().server() == server) {
                if (error != null) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot copy project {} locally", project, error);
                } else if (changed) {
                    publish();
                }

                done.run();
            }
        }));
    }

    public static void offer(ServerPlayer p, Packets.ContentOffer msg) {
        try {
            Path d = dir(msg.project());
            Files.createDirectories(d);
            Map<String, ProjectContent.Entry> have = manifest(d);
            Map<String, ProjectContent.Entry> offered = new LinkedHashMap<>();

            for (String line : msg.entries()) {
                ProjectContent.Entry e = ProjectContent.Entry.of(line);
                if (allowed(e.path()) && e.size() <= 33554432L) {
                    offered.put(e.path(), e);
                }
            }

            ProjectContent.Upload u = new ProjectContent.Upload(p.getUUID());

            for (String old : have.keySet()) {
                if (!offered.containsKey(old)) {
                    Files.deleteIfExists(d.resolve(old));
                    u.changed = true;
                }
            }

            removeEmpty(d);

            for (ProjectContent.Entry e : offered.values()) {
                ProjectContent.Entry mine = have.get(e.path());
                if (mine == null || !mine.hash().equals(e.hash())) {
                    u.want.put(e.path(), e);
                }
            }

            if (u.want.isEmpty()) {
                if (u.changed) {
                    publish();
                }

                SprauteNetwork.sendTo(p, new Packets.ContentWant(msg.project(), List.of()));
                return;
            }

            UPLOADS.put(msg.project(), u);
            SprauteNetwork.sendTo(p, new Packets.ContentWant(msg.project(), new ArrayList<>(u.want.keySet())));
        } catch (RuntimeException | IOException var9) {
            SprauteEngine.LOGGER.warn("Spraute: cannot take the project {}", msg.project(), var9);
            SprauteNetwork.sendTo(p, new Packets.ContentWant(msg.project(), List.of()));
        }
    }

    public static void received(ServerPlayer p, Packets.ContentData msg) {
        ProjectContent.Upload u = UPLOADS.get(msg.project());
        if (u != null && u.from.equals(p.getUUID())) {
            ProjectContent.Entry e = u.want.get(msg.path());
            if (e != null) {
                ByteArrayOutputStream part = u.parts.computeIfAbsent(msg.path(), k -> new ByteArrayOutputStream());
                part.writeBytes(msg.data());
                if (part.size() > e.size()) {
                    u.parts.remove(msg.path());
                    u.want.remove(msg.path());
                } else if (msg.index() + 1 >= msg.total()) {
                    byte[] all = part.toByteArray();
                    u.parts.remove(msg.path());
                    u.want.remove(msg.path());
                    if (hash(all).equals(e.hash())) {
                        try {
                            Path f = dir(msg.project()).resolve(msg.path()).normalize();
                            if (f.startsWith(dir(msg.project()))) {
                                Files.createDirectories(f.getParent());
                                Files.write(f, all);
                                u.changed = true;
                            }
                        } catch (IOException var7) {
                            SprauteEngine.LOGGER.warn("Spraute: cannot save {} of {}", msg.path(), msg.project(), var7);
                        }
                    } else {
                        SprauteEngine.LOGGER.warn("Spraute: {} of {} came damaged", msg.path(), msg.project());
                    }
                }

                if (u.want.isEmpty()) {
                    UPLOADS.remove(msg.project());
                    if (u.changed) {
                        publish();
                    }

                    SprauteNetwork.sendTo(p, new Packets.ContentWant(msg.project(), List.of()));
                }
            }
        }
    }

    private static void publish() {
        if (!Platform.get().isClient()) {
            ModelLibrary.scan();
        }

        for (ServerPlayer p : SprauteRuntime.players()) {
            sendIndex(p);
        }

        RecipeOverrides.reload(Platform.get().server());
    }

    public static void sendIndex(ServerPlayer p) {
        Path root = root();
        if (root != null && Files.isDirectory(root)) {
            try (Stream<Path> dirs = Files.list(root)) {
                for (Path d : (Iterable<Path>) dirs.filter(x$0 -> Files.isDirectory(x$0))::iterator) {
                    String name = ModelLibrary.projectNameOf(d);
                    SprauteNetwork.sendTo(p, new Packets.ContentIndex(name, lines(manifest(d))));
                }
            } catch (IOException var8) {
                SprauteEngine.LOGGER.warn("Spraute: cannot list the projects", (Throwable)var8);
            }
        }
    }

    public static void requested(ServerPlayer p, Packets.ContentRequest msg) {
        Deque<Packets.ContentData> queue = OUTGOING.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());

        for (String path : msg.paths()) {
            if (allowed(path)) {
                try {
                    Path f = dir(msg.project()).resolve(path).normalize();
                    if (f.startsWith(dir(msg.project())) && Files.isRegularFile(f)) {
                        pieces(msg.project(), path, Files.readAllBytes(f), 49152, queue);
                    }
                } catch (IOException var6) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read {} of {}", path, msg.project(), var6);
                }
            }
        }
    }

    public static void pieces(String project, String path, byte[] all, int size, Deque<Packets.ContentData> queue) {
        int total = Math.max(1, (all.length + size - 1) / size);

        for (int i = 0; i < total; i++) {
            queue.add(new Packets.ContentData(project, path, i, total, Arrays.copyOfRange(all, i * size, Math.min(all.length, (i + 1) * size))));
        }
    }

    public static void tick(MinecraftServer server) {
        OUTGOING.entrySet().removeIf(en -> {
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null) {
                return true;
            } else {
                for (int i = 0; i < 3 && !en.getValue().isEmpty(); i++) {
                    SprauteNetwork.sendTo(p, en.getValue().poll());
                }

                return en.getValue().isEmpty();
            }
        });
    }

    public static void unload() {
        UPLOADS.clear();
        OUTGOING.clear();
    }

    public record Entry(String path, long size, String hash) {
        public String line() {
            return this.path + "\t" + this.size + "\t" + this.hash;
        }

        public static ProjectContent.Entry of(String line) {
            String[] p = line.split("\t");
            return new ProjectContent.Entry(p[0], Long.parseLong(p[1]), p[2]);
        }
    }

    private static final class Upload {
        final UUID from;
        final Map<String, ProjectContent.Entry> want = new HashMap<>();
        final Map<String, ByteArrayOutputStream> parts = new HashMap<>();
        boolean changed;

        Upload(UUID from) {
            this.from = from;
        }
    }
}
