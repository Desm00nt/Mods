package org.sprautestudio.spraute_engine.content;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.editor.assets.ProjectImages;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.Images;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;

public final class ContentClient {
    private static final int PIECES_PER_TICK = 2;
    private static final int UPLOAD_TIMEOUT_TICKS = 2400;
    private static final Map<String, Path> ACTIVE = new LinkedHashMap<>();
    private static final Map<String, Map<String, ProjectContent.Entry>> MISSING = new HashMap<>();
    private static final Map<String, ByteArrayOutputStream> PARTS = new HashMap<>();
    private static final Deque<Packets.ContentData> UP = new ArrayDeque<>();
    private static final Map<String, Runnable> AFTER_UPLOAD = new HashMap<>();
    private static final Map<String, Integer> UPLOAD_AGE = new HashMap<>();

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(ContentClient::onTick);
        ClientHooks.LOGGING_OUT.add(ContentClient::onLogout);
    }

    private ContentClient() {
    }

    private static Path cache() {
        return Workspace.root().resolve("server_content");
    }

    public static Collection<Path> activeDirs() {
        return ACTIVE.values();
    }

    public static Path dir(String project) {
        return ACTIVE.get(project);
    }

    public static void upload(String project, Runnable then) {
        Path dir = null;

        for (Workspace.ProjectInfo i : Workspace.projects()) {
            if (i.name().equals(project)) {
                dir = i.dir();
            }
        }

        if (dir == null) {
            if (then != null) {
                then.run();
            }
        } else {
            Minecraft mc = Minecraft.getInstance();
            IntegratedServer integrated = mc.getSingleplayerServer();
            if (integrated != null) {
                ProjectContent.copyLocal(integrated, project, dir, () -> {
                    if (then != null) {
                        mc.execute(then);
                    }
                });
            } else {
                try {
                    List<String> lines = ProjectContent.lines(ProjectContent.manifest(dir));
                    if (then != null) {
                        AFTER_UPLOAD.put(project, then);
                        UPLOAD_AGE.put(project, 0);
                    }

                    SprauteNetwork.sendToServer(new Packets.ContentOffer(project, lines));
                } catch (IOException var6) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read the project {}", project, var6);
                    if (then != null) {
                        then.run();
                    }
                }
            }
        }
    }

    public static void wanted(Packets.ContentWant msg) {
        if (msg.paths().isEmpty()) {
            UPLOAD_AGE.remove(msg.project());
            Runnable then = AFTER_UPLOAD.remove(msg.project());
            if (then != null) {
                then.run();
            }
        } else {
            Path dir = null;

            for (Workspace.ProjectInfo i : Workspace.projects()) {
                if (i.name().equals(msg.project())) {
                    dir = i.dir();
                }
            }

            if (dir != null) {
                for (String path : msg.paths()) {
                    try {
                        ProjectContent.pieces(msg.project(), path, Files.readAllBytes(dir.resolve(path)), 24576, UP);
                    } catch (IOException var5) {
                        SprauteEngine.LOGGER.warn("Spraute: cannot read {}", path, var5);
                    }
                }
            }
        }
    }

    public static void index(Packets.ContentIndex msg) {
        if (!Minecraft.getInstance().hasSingleplayerServer()) {
            try {
                Path d = cache().resolve(Images.slug(msg.project()));
                Files.createDirectories(d);
                Map<String, ProjectContent.Entry> have = ProjectContent.manifest(d);
                Map<String, ProjectContent.Entry> wanted = new LinkedHashMap<>();
                Map<String, ProjectContent.Entry> theirs = new LinkedHashMap<>();

                for (String line : msg.entries()) {
                    ProjectContent.Entry e = ProjectContent.Entry.of(line);
                    if (ProjectContent.allowed(e.path())) {
                        theirs.put(e.path(), e);
                    }
                }

                for (String old : have.keySet()) {
                    if (!theirs.containsKey(old)) {
                        Files.deleteIfExists(d.resolve(old));
                    }
                }

                ProjectContent.removeEmpty(d);

                for (ProjectContent.Entry e : theirs.values()) {
                    ProjectContent.Entry mine = have.get(e.path());
                    if (mine == null || !mine.hash().equals(e.hash())) {
                        wanted.put(e.path(), e);
                    }
                }

                ACTIVE.put(msg.project(), d);
                if (wanted.isEmpty()) {
                    MISSING.remove(msg.project());
                    ready(msg.project());
                } else {
                    MISSING.put(msg.project(), wanted);
                    List<String> batch = new ArrayList<>();
                    int size = 0;

                    for (String path : wanted.keySet()) {
                        batch.add(path);
                        size += path.length() * 3 + 4;
                        if (size > 20000) {
                            SprauteNetwork.sendToServer(new Packets.ContentRequest(msg.project(), batch));
                            batch = new ArrayList<>();
                            size = 0;
                        }
                    }

                    if (!batch.isEmpty()) {
                        SprauteNetwork.sendToServer(new Packets.ContentRequest(msg.project(), batch));
                    }
                }
            } catch (RuntimeException | IOException var9) {
                SprauteEngine.LOGGER.warn("Spraute: cannot take the project {} from the server", msg.project(), var9);
            }
        }
    }

    public static void received(Packets.ContentData msg) {
        Map<String, ProjectContent.Entry> missing = MISSING.get(msg.project());
        ProjectContent.Entry e = missing == null ? null : missing.get(msg.path());
        Path d = ACTIVE.get(msg.project());
        if (e != null && d != null) {
            String key = msg.project() + "/" + msg.path();
            ByteArrayOutputStream part = PARTS.computeIfAbsent(key, k -> new ByteArrayOutputStream());
            part.writeBytes(msg.data());
            if (msg.index() + 1 >= msg.total()) {
                PARTS.remove(key);
                missing.remove(msg.path());
                byte[] all = part.toByteArray();
                if (ProjectContent.hash(all).equals(e.hash())) {
                    try {
                        Path f = d.resolve(msg.path()).normalize();
                        if (f.startsWith(d)) {
                            Files.createDirectories(f.getParent());
                            Files.write(f, all);
                        }
                    } catch (IOException var8) {
                        SprauteEngine.LOGGER.warn("Spraute: cannot save {}", msg.path(), var8);
                    }
                } else {
                    SprauteEngine.LOGGER.warn("Spraute: {} of {} came damaged", msg.path(), msg.project());
                }

                if (missing.isEmpty()) {
                    MISSING.remove(msg.project());
                    ready(msg.project());
                }
            }
        }
    }

    private static void ready(String project) {
        ModelLibrary.scan();

        try {
            ProjectImages.register(Project.openReadOnly(ACTIVE.get(project)));
        } catch (Exception var2) {
            SprauteEngine.LOGGER.warn("Spraute: cannot load the pictures of {}", project, var2);
        }
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END) {
            if (Minecraft.getInstance().getConnection() != null) {
                for (int i = 0; i < 2 && !UP.isEmpty(); i++) {
                    SprauteNetwork.sendToServer(UP.poll());
                }

                if (!UPLOAD_AGE.isEmpty()) {
                    for (String project : new ArrayList<>(UPLOAD_AGE.keySet())) {
                        int age = UPLOAD_AGE.merge(project, 1, Integer::sum);
                        if (age >= 2400) {
                            UPLOAD_AGE.remove(project);
                            Runnable then = AFTER_UPLOAD.remove(project);
                            if (then != null) {
                                then.run();
                            }
                        }
                    }
                }
            }
        }
    }

    public static void onLogout(ClientHooks.LoggingOut e) {
        boolean had = !ACTIVE.isEmpty();
        ACTIVE.clear();
        MISSING.clear();
        PARTS.clear();
        UP.clear();
        AFTER_UPLOAD.clear();
        UPLOAD_AGE.clear();
        if (had) {
            ModelLibrary.scan();
        }
    }
}
