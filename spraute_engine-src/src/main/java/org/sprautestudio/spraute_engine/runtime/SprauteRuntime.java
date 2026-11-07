package org.sprautestudio.spraute_engine.runtime;

import com.google.gson.Gson;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.addon.AddonContent;
import org.sprautestudio.spraute_engine.content.ProjectContent;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.core.script.ScriptEnvironment;
import org.sprautestudio.spraute_engine.core.script.ScriptInstance;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformer;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.lang.Analyzer;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Diagnostic;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.quest.Quests;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.zone.Zones;

public final class SprauteRuntime {
    private static final SprauteRuntime INSTANCE = new SprauteRuntime();
    private final Map<Integer, SprauteRuntime.Deployed> deployed = new TreeMap<>();
    final Map<String, Character<?>> characterCache = new HashMap<>();
    private Constructor<SprauteScriptInstance> constructor;
    private static final ThreadLocal<ScriptInstance> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<UUID> AS = new ThreadLocal<>();
    public static final String SOLO_TAG = "spraute_solo:";
    private ScriptEnvironment environment;
    private final Map<UUID, Integer> autostartAt = new HashMap<>();
    private final Map<String, Map<String, String>> guis = new LinkedHashMap<>();
    private static final int DEBUG_MAX = 80;

    public static SprauteRuntime get() {
        return INSTANCE;
    }

    private SprauteRuntime() {
    }

    public SprauteRuntime.Deployed deployed(int id) {
        return this.deployed.get(ScriptEnvironment.baseId(id));
    }

    public Map<Integer, SprauteRuntime.Deployed> all() {
        return Collections.unmodifiableMap(this.deployed);
    }

    public static MinecraftServer server() {
        return Platform.get().server();
    }

    public static List<ServerPlayer> players() {
        MinecraftServer s = server();
        return s == null ? List.of() : s.getPlayerList().getPlayers();
    }

    public static ScriptInstance enter(ScriptInstance s) {
        ScriptInstance before = CURRENT.get();
        if (s == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(s);
        }

        return before;
    }

    public static UUID getAs() {
        return AS.get();
    }

    public static void setAs(UUID player) {
        if (player == null) {
            AS.remove();
        } else {
            AS.set(player);
        }
    }

    private static ServerPlayer online(UUID id) {
        MinecraftServer s = server();
        return s != null && id != null ? s.getPlayerList().getPlayer(id) : null;
    }

    public static boolean isOnline(UUID id) {
        return online(id) != null;
    }

    public static ServerPlayer player() {
        ServerPlayer as = online(AS.get());
        if (as != null) {
            return as;
        } else {
            ScriptInstance s = CURRENT.get();
            if (s != null) {
                return s.protagonist();
            } else {
                List<ServerPlayer> list = players();
                return list.isEmpty() ? null : list.get(0);
            }
        }
    }

    public static ServerPlayer playerOf(int scriptId) {
        ScriptInstance s = env().map(e -> e.scriptInstances.get(scriptId)).orElse(null);
        if (s != null) {
            return s.protagonist();
        } else {
            List<ServerPlayer> list = players();
            return list.isEmpty() ? null : list.get(0);
        }
    }

    private static List<ServerPlayer> only(ServerPlayer p) {
        return p == null ? List.of() : List.of(p);
    }

    public static List<ServerPlayer> audience() {
        ServerPlayer as = online(AS.get());
        if (as != null) {
            return List.of(as);
        } else {
            ScriptInstance s = CURRENT.get();
            return s != null && s.solo() ? only(s.protagonist()) : players();
        }
    }

    public static List<ServerPlayer> audience(int scriptId) {
        ScriptInstance s = env().map(e -> e.scriptInstances.get(scriptId)).orElse(null);
        return s != null && s.solo() ? only(s.protagonist()) : players();
    }

    public static void markOwned(Entity e, ScriptInstance s) {
        if (s != null && s.solo() && s.owner != null) {
            e.addTag("spraute_solo:" + s.owner);
        }
    }

    public static void markOwned(Entity e) {
        markOwned(e, CURRENT.get());
    }

    public static String ownerOf(Entity e) {
        for (String t : e.getTags()) {
            if (t.startsWith("spraute_solo:")) {
                return t.substring("spraute_solo:".length());
            }
        }

        return null;
    }

    public static boolean sees(Entity e, ServerPlayer p) {
        String owner = ownerOf(e);
        return owner == null || owner.equals(p.getUUID().toString());
    }

    public static boolean canEdit(ServerPlayer player) {
        MinecraftServer s = player.getServer();
        return player.hasPermissions(2) || s != null && s.isSingleplayerOwner(player.getGameProfile());
    }

    public static Optional<ScriptEnvironment> env() {
        return Optional.ofNullable(INSTANCE.environment);
    }

    public void openEnvironment(MinecraftServer server) {
        if (!Platform.get().isClient()) {
            ModelLibrary.scan();
        }

        this.environment = new ScriptEnvironment(server);
        this.environment.load();
    }

    public void closeEnvironment() {
        this.environment = null;
    }

    public SprauteRuntime.DeployResult deploy(String project, String source) {
        Analyzer.Result analysis = Analyzer.analyze(source);
        List<String> messages = new ArrayList<>();

        for (Diagnostic d : analysis.diagnostics) {
            if (d.isError()) {
                messages.add("Строка " + (d.line + 1) + ": " + d.message);
            }
        }

        Ast.Header header = analysis.program().header;
        if (header == null) {
            messages.add("Нет заголовка script <id> \"Название\"");
        } else if (header.general) {
            messages.add("Это общий скрипт: он работает в самой игре, а не в мире. Загружать его в мир не нужно");
        }

        if (!messages.isEmpty()) {
            return new SprauteRuntime.DeployResult(false, header != null ? header.id : -1, messages);
        } else {
            int id = header.id;
            Optional<ScriptEnvironment> env = env();
            if (env.isPresent()) {
                Constructor<? extends ScriptInstance> existing = env.get().constructors.get(id);
                if (existing != null && existing.getDeclaringClass() != SprauteScriptInstance.class) {
                    messages.add("Id " + id + " уже занят (" + existing.getDeclaringClass().getSimpleName() + "). Выберите другой id.");
                    return new SprauteRuntime.DeployResult(false, id, messages);
                }
            }

            SprauteRuntime.Deployed old = this.deployed.get(id);
            if (old != null && !old.project.equals(project)) {
                messages.add("Id " + id + " уже используется скриптом «" + old.title + "» из проекта " + old.project);
                return new SprauteRuntime.DeployResult(false, id, messages);
            } else {
                List<String> labels = new ArrayList<>(analysis.labels.keySet());
                this.deployed.put(id, new SprauteRuntime.Deployed(id, header.title, project, source, analysis.program(), labels));
                this.save(id);
                env.ifPresent(this::registerInEnv);
                messages.add("Сюжет #" + id + " «" + header.title + "» загружен в игру");
                int warnings = analysis.warningCount();
                if (warnings > 0) {
                    messages.add("Предупреждений: " + warnings);
                }

                return new SprauteRuntime.DeployResult(true, id, messages);
            }
        }
    }

    public boolean undeploy(int id) {
        SprauteRuntime.Deployed d = this.deployed.remove(id);
        if (d == null) {
            return false;
        } else {
            env().ifPresent(env -> {
                if (env.scriptInstances.containsKey(id)) {
                    env.stop(id);
                }

                Constructor<? extends ScriptInstance> c = env.constructors.get(id);
                if (c != null && c.getDeclaringClass() == SprauteScriptInstance.class) {
                    env.constructors.remove(id);
                }
            });
            Path dir = this.storageDir();
            if (dir != null) {
                try {
                    Files.deleteIfExists(dir.resolve(id + ".sst"));
                    Files.deleteIfExists(dir.resolve(id + ".project"));
                } catch (IOException var5) {
                    SprauteEngine.LOGGER.warn("Failed to delete deployed script {}", id, var5);
                }
            }

            this.registerCategories();
            return true;
        }
    }

    public void keepOnly(String project, Set<Integer> ids) {
        for (SprauteRuntime.Deployed d : new ArrayList<>(this.deployed.values())) {
            if (d.project.equals(project) && !ids.contains(d.id)) {
                this.undeploy(d.id);
            }
        }
    }

    public void registerInEnv(ScriptEnvironment env) {
        if (this.constructor == null) {
            try {
                this.constructor = SprauteScriptInstance.class.getConstructor(ScriptEnvironment.class, int.class);
            } catch (NoSuchMethodException var5) {
                throw new IllegalStateException(var5);
            }
        }

        for (SprauteRuntime.Deployed d : this.deployed.values()) {
            Constructor<? extends ScriptInstance> existing = env.constructors.get(d.id);
            if (existing == null) {
                env.constructors.put(d.id, this.constructor);
            } else if (existing.getDeclaringClass() != SprauteScriptInstance.class) {
                this.report(d.id, "Id " + d.id + " занят — скрипт не зарегистрирован");
            }
        }

        this.registerCategories();
    }

    public boolean needsRegistration(ScriptEnvironment env) {
        for (Integer id : this.deployed.keySet()) {
            if (!env.constructors.containsKey(id)) {
                return true;
            }
        }

        return false;
    }

    private void registerCategories() {
        if (this.environment != null) {
            Map<String, List<Integer>> byProject = new LinkedHashMap<>();

            for (SprauteRuntime.Deployed d : this.deployed.values()) {
                byProject.computeIfAbsent(categoryName(d.project), k -> new ArrayList<>()).add(d.id);
            }

            this.environment.categories.clear();
            byProject.forEach((k, v) -> this.environment.categories.put(k, v.stream().mapToInt(Integer::intValue).toArray()));
        }
    }

    public static String categoryName(String project) {
        String s = project.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return "spraute_" + (s.isEmpty() ? "project" : s);
    }

    private Path storageDir() {
        MinecraftServer s = server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute");
    }

    private void save(int id) {
        Path dir = this.storageDir();
        SprauteRuntime.Deployed d = this.deployed.get(id);
        if (dir != null && d != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(dir.resolve(id + ".sst"), d.source, StandardCharsets.UTF_8);
                Files.writeString(dir.resolve(id + ".project"), d.project, StandardCharsets.UTF_8);
            } catch (IOException var5) {
                SprauteEngine.LOGGER.warn("Failed to save deployed script {}", id, var5);
            }
        }
    }

    public void loadWorld() {
        this.deployed.clear();
        Path dir = this.storageDir();
        if (dir != null && Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".sst"))::iterator) {
                    String source = Files.readString(f, StandardCharsets.UTF_8);
                    Path projectFile = Path.of(f.toString().replace(".sst", ".project"));
                    String project = Files.exists(projectFile) ? Files.readString(projectFile, StandardCharsets.UTF_8).trim() : "world";
                    Analyzer.Result a = Analyzer.analyze(source);
                    if (!a.hasErrors() && a.program().header != null) {
                        Ast.Header h = a.program().header;
                        this.deployed.put(h.id, new SprauteRuntime.Deployed(h.id, h.title, project, source, a.program(), new ArrayList<>(a.labels.keySet())));
                    } else {
                        SprauteEngine.LOGGER.warn("Skipping broken Spraute script {}", f);
                    }
                }
            } catch (IOException var12) {
                SprauteEngine.LOGGER.warn("Failed to load Spraute scripts", (Throwable)var12);
            }

            this.loadGuis(dir);
            SprauteEngine.LOGGER.info("Loaded {} Spraute scripts", this.deployed.size());
        }
    }

    public void clear() {
        this.deployed.clear();
    }

    public void playerJoined(ServerPlayer player) {
        MinecraftServer s = server();
        if (s != null) {
            this.autostartAt.put(player.getUUID(), s.getTickCount() + 40);
        }

        Quests.sync(player, false);
        Zones.syncTo(player);
        ProjectContent.sendIndex(player);
    }

    public void tickAutostart() {
        MinecraftServer s = server();
        if (s != null && !this.autostartAt.isEmpty()) {
            Iterator<Entry<UUID, Integer>> it = this.autostartAt.entrySet().iterator();

            while (it.hasNext()) {
                Entry<UUID, Integer> e = it.next();
                if (s.getTickCount() >= e.getValue()) {
                    it.remove();
                    ServerPlayer p = s.getPlayerList().getPlayer(e.getKey());
                    if (p != null) {
                        this.autostart(p);
                    }
                }
            }
        }
    }

    private void autostart(ServerPlayer player) {
        Optional<ScriptEnvironment> envOpt = env();
        if (!envOpt.isEmpty()) {
            Set<String> done = this.autostartDone();
            boolean changed = false;

            for (SprauteRuntime.Deployed d : this.deployed.values()) {
                Ast.Header h = d.program.header;
                if (h != null && h.autostart) {
                    String mark = h.solo ? d.id + ":" + player.getUUID() : String.valueOf(d.id);
                    if (envOpt.get().instanceFor(d.id, player) == null && (!h.once || !done.contains(mark))) {
                        MinecraftServer s = server();
                        s.getCommands()
                            .performPrefixedCommand(player.createCommandSourceStack().withPermission(4).withSuppressedOutput(), "script start " + d.id);
                        this.log(d.id, "Автозапуск: «" + d.title + "»");
                        if (done.add(mark)) {
                            changed = true;
                        }
                    }
                }
            }

            if (changed) {
                this.saveAutostartDone(done);
            }
        }
    }

    private Set<String> autostartDone() {
        Set<String> out = new TreeSet<>();
        Path dir = this.storageDir();
        if (dir == null) {
            return out;
        } else {
            try {
                Path f = dir.resolve("autostarted.txt");
                if (Files.exists(f)) {
                    for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                        if (!line.isBlank()) {
                            out.add(line.trim());
                        }
                    }
                }
            } catch (IOException var6) {
            }

            return out;
        }
    }

    private void saveAutostartDone(Set<String> done) {
        Path dir = this.storageDir();
        if (dir != null) {
            try {
                Files.createDirectories(dir);
                Files.writeString(dir.resolve("autostarted.txt"), String.join("\n", done) + "\n", StandardCharsets.UTF_8);
            } catch (IOException var4) {
            }
        }
    }

    public void log(int scriptId, String text) {
        this.broadcast(0, "#" + scriptId, text);
    }

    public static void installErrorHook() {
        ScriptEnvironment.onError = (id, error) -> {
            if (!(error instanceof SprauteRuntimeException)) {
                SprauteEngine.LOGGER.error("Story #{} failed", id, error);
            }

            Throwable root = error;

            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }

            StackTraceElement where = null;

            for (StackTraceElement e : root.getStackTrace()) {
                if (e.getClassName().startsWith("org.sprautestudio")) {
                    where = e;
                    break;
                }
            }

            if (root instanceof SprauteRuntimeException) {
                get().report(id, "Сюжет остановлен из-за ошибки: " + root.getMessage());
            } else {
                get()
                    .report(
                        id,
                        "Сюжет остановлен из-за ошибки: "
                            + root.getClass().getSimpleName()
                            + (root.getMessage() != null ? ": " + root.getMessage() : "")
                            + (
                                where != null
                                    ? " (" + where.getClassName().substring(where.getClassName().lastIndexOf(46) + 1) + "." + where.getMethodName() + ")"
                                    : ""
                            )
                            + ". Подробности в latest.log"
                    );
            }
        };
    }

    public void putGuis(String project, Map<String, String> map) {
        if (map != null) {
            this.guis.put(project, new LinkedHashMap<>(map));
            EngineConfig cfg = EngineServer.config();
            MinecraftServer server = Platform.get().server();
            if (server != null && project.equals(cfg.project)) {
                EngineConfig c = null;

                for (Entry<String, String> en : cfg.layouts.entrySet()) {
                    String json = en.getValue() != null && !en.getValue().isEmpty() ? map.get(en.getValue()) : null;
                    if (json != null && !json.equals(cfg.layoutJson.get(en.getKey()))) {
                        if (c == null) {
                            c = cfg.copy();
                        }

                        c.layoutJson.put(en.getKey(), json);
                    }
                }

                if (c != null) {
                    EngineServer.set(server, c);
                }
            }

            Path dir = this.storageDir();
            if (dir != null) {
                try {
                    Files.createDirectories(dir);
                    Files.writeString(dir.resolve(categoryName(project) + ".guis.json"), new Gson().toJson(map), StandardCharsets.UTF_8);
                    Files.writeString(dir.resolve(categoryName(project) + ".guis.project"), project, StandardCharsets.UTF_8);
                } catch (IOException var9) {
                    SprauteEngine.LOGGER.warn("Failed to save GUIs of {}", project, var9);
                }
            }
        }
    }

    public Map<String, Map<String, String>> allGuis() {
        return Collections.unmodifiableMap(this.guis);
    }

    public String gui(String project, String name) {
        Map<String, String> m = this.guis.get(project);
        if (!name.startsWith("fx:")
            && !name.startsWith("post:")
            && !name.startsWith("fight:")
            && !name.startsWith("build:")
            && !name.startsWith("stealth:")
            && !name.startsWith("skills:")) {
            return m == null ? null : m.get(name);
        } else {
            return null;
        }
    }

    private void loadGuis(Path dir) {
        this.guis.clear();

        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".guis.json"))::iterator) {
                Path pf = Path.of(f.toString().replace(".guis.json", ".guis.project"));
                String project = Files.exists(pf) ? Files.readString(pf, StandardCharsets.UTF_8).trim() : "world";
                Map<String, String> m = new Gson().fromJson(Files.readString(f, StandardCharsets.UTF_8), LinkedHashMap.class);
                if (m != null) {
                    this.guis.put(project, m);
                }
            }
        } catch (Exception var10) {
            SprauteEngine.LOGGER.warn("Failed to load Spraute GUIs", (Throwable)var10);
        }
    }

    private void panelEvent(SprauteScriptInstance s, ServerPlayer player, Packets.GuiEvent msg) {
        String[] parts = msg.kind().split(":", 3);
        String kind = parts[0];
        String panel = parts[parts.length - 1];
        switch (kind) {
            case "panel_close":
                s.guiCloseSeq++;
                WorldPanels.remove(s.getId(), panel);
                break;
            case "panel_click":
            case "panel_hover":
            case "panel_leave":
                Map<String, Object> data = new HashMap<>();
                data.put("element", msg.element());
                data.put("panel", panel);
                data.put("gui", msg.gui());
                if (kind.equals("panel_click")) {
                    data.put("button", parts.length == 3 ? parts[1] : "left");
                }

                GameEvents.post(kind.equals("panel_click") ? "panelclick" : (kind.equals("panel_hover") ? "panelhover" : "panelleave"), player, data);
        }
    }

    public void onGuiEvent(ServerPlayer player, Packets.GuiEvent msg) {
        env().ifPresent(env -> {
            ScriptInstance si = env.scriptInstances.get(msg.scriptId());
            if (si instanceof SprauteScriptInstance s) {
                if (!msg.gui().equals(s.openGui) || s.guiPlayer == null || s.guiPlayer.equals(player.getUUID())) {
                    if (msg.inputs() != null) {
                        s.guiInputs.putAll(msg.inputs());
                    }

                    if (msg.kind().startsWith("addon_event:")) {
                        String event = msg.kind().substring("addon_event:".length());
                        if (AddonContent.isPlayerEvent(event)) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("element", msg.element());
                            data.put("gui", msg.gui());
                            GameEvents.post(event, player, data);
                        }
                    } else if (msg.kind().equals("click")) {
                        s.lastButton = msg.element();
                        s.buttonSeq++;
                    } else if (msg.kind().equals("drag_start") || msg.kind().startsWith("drag_drop:")) {
                        Map<String, Object> data = new HashMap<>();
                        data.put("element", msg.element());
                        data.put("gui", msg.gui());
                        if (msg.kind().equals("drag_start")) {
                            GameEvents.post("guidrag", player, data);
                        } else {
                            String zone = msg.kind().substring("drag_drop:".length());
                            if (zone.isEmpty()) {
                                s.guiZones.remove(msg.element());
                            } else {
                                s.guiZones.put(msg.element(), zone);
                            }

                            data.put("zone", zone);
                            GameEvents.post("guidrop", player, data);
                        }
                    } else if (msg.kind().startsWith("slot_put:")) {
                        String[] p = msg.kind().split(":");

                        try {
                            GuiSlots.put(s, player, msg.element(), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                        } catch (RuntimeException var9) {
                        }
                    } else if (msg.kind().startsWith("slot_take:")) {
                        try {
                            GuiSlots.take(s, player, msg.element(), Integer.parseInt(msg.kind().substring(10)));
                        } catch (RuntimeException var8) {
                        }
                    } else if (msg.kind().equals("close")) {
                        if (msg.gui().equals(s.openGui)) {
                            s.openGui = null;
                        }

                        s.guiCloseSeq++;
                    } else if (msg.kind().startsWith("panel_")) {
                        this.panelEvent(s, player, msg);
                    }
                }
            }
        });
    }

    public void report(int scriptId, String text) {
        SprauteRuntime.Deployed d = this.deployed(scriptId);
        String who = d != null ? "«" + d.title + "» #" + scriptId + (d.project.isEmpty() ? "" : " (" + d.project + ")") : "#" + scriptId;
        SprauteEngine.LOGGER.warn("[Spraute {}] {}", who, text);
        this.broadcast(3, who, text);
    }

    public void broadcast(int level, String source, String text) {
        for (ServerPlayer p : players()) {
            if (canEdit(p)) {
                SprauteNetwork.sendTo(p, new Packets.Log(level, source, text));
            }
        }
    }

    public Packets.Status status() {
        return this.status(null);
    }

    public Packets.Status status(ServerPlayer viewer) {
        List<Packets.ScriptEntry> entries = new ArrayList<>();
        Optional<ScriptEnvironment> envOpt = env();
        List<String> categories = envOpt.<List<String>>map(ex -> new ArrayList<>(ex.categories.keySet())).orElseGet(ArrayList::new);
        if (envOpt.isPresent()) {
            ScriptEnvironment env = envOpt.get();
            List<Integer> ids = new ArrayList<>(env.constructors.keySet());
            Collections.sort(ids);

            for (int id : ids) {
                Packets.ScriptEntry e = new Packets.ScriptEntry();
                e.id = id;
                Constructor<? extends ScriptInstance> c = env.constructors.get(id);
                SprauteRuntime.Deployed d = this.deployed.get(id);
                if (d != null && c != null && c.getDeclaringClass() == SprauteScriptInstance.class) {
                    e.spraute = true;
                    e.title = d.title;
                    e.project = d.project;
                    e.labels.addAll(d.labels);
                } else if (c != null) {
                    e.title = c.getDeclaringClass().getSimpleName();
                    e.project = "";
                }

                ScriptInstance inst = viewer != null ? env.instanceFor(id, viewer) : null;
                if (inst == null) {
                    inst = env.anyInstance(id);
                }

                if (inst != null) {
                    debug(inst, e);
                    e.active = true;
                    e.paused = inst.isPaused();
                    ScriptPerformer main = inst.getMainPerformer();
                    if (main != null) {
                        e.stage = String.valueOf(main.getStage());
                        e.actionIndex = main.getActionIndex();
                        e.actionCount = main.getActionsAmount();
                        e.doing = main.describe();
                        if (!e.spraute) {
                            for (String l : main.labelPointsNames) {
                                if (!l.startsWith("__sp_")) {
                                    e.labels.add(l);
                                }
                            }
                        }
                    }
                }

                entries.add(e);
            }
        }

        List<Packets.Value> pvars = new ArrayList<>();
        int coins = -1;
        if (viewer != null) {
            PlayerVars.all(viewer).forEach((k, v) -> pvars.add(value(k, v)));
            if (EngineServer.config().currency) {
                coins = Coins.get(viewer);
            }
        }

        return new Packets.Status(envOpt.isPresent(), entries, categories, pvars, coins);
    }

    private static void debug(ScriptInstance inst, Packets.ScriptEntry e) {
        SprauteScriptInstance si = inst instanceof SprauteScriptInstance s ? s : null;

        for (Entry<String, ScriptPerformer> b : inst.runningBranches().entrySet()) {
            if (e.branches.size() >= 80) {
                break;
            }

            String name = b.getKey();
            SprauteScriptInstance.Handler h = si != null ? si.handlers.get(name) : null;
            String shown = h != null ? h.title : (name.startsWith("__async_") ? "фоновая ветка" : name);
            e.branches.add(new Packets.Branch(cut(shown, 250), cut(b.getValue().describe(), 500), h != null ? h.count : -1));
        }

        if (si != null) {
            for (Entry<String, Object> v : si.variables().entrySet()) {
                if (e.vars.size() >= 80) {
                    break;
                }

                if (!v.getKey().startsWith("__")) {
                    e.vars.add(value(v.getKey(), v.getValue()));
                }
            }
        }
    }

    private static Packets.Value value(String name, Object v) {
        String kind = !(v instanceof Double) && !(v instanceof Number)
            ? (v instanceof Boolean ? "bool" : (v instanceof List ? "list" : (v instanceof Map ? "map" : (v instanceof EntityRef ? "entity" : "text"))))
            : "number";
        String text = v instanceof String s ? "\"" + s + "\"" : Values.format(v instanceof Double d ? Eval.str(d) : v);
        return new Packets.Value(cut(name, 250), cut(text, 1000), kind);
    }

    private static String cut(String s, int max) {
        if (s == null) {
            return "";
        } else {
            return s.length() > max ? s.substring(0, max - 1) + "…" : s;
        }
    }

    public String debugSet(ServerPlayer p, Packets.DebugSet msg) {
        if (!canEdit(p)) {
            return "Менять значения могут только те, кто может редактировать";
        } else {
            try {
                String ex = msg.scope();
                switch (ex) {
                    case "coins":
                        Coins.set(p, (int)Math.round(Eval.num(literal(null, msg.value()))));
                        return null;
                    case "pvar":
                        if (msg.value().isBlank()) {
                            PlayerVars.clear(p, msg.name());
                        } else {
                            PlayerVars.set(p, msg.name(), literal(null, msg.value()));
                        }

                        return null;
                    default:
                        ScriptInstance inst = env().map(en -> {
                            ScriptInstance mine = en.instanceFor(msg.scriptId(), p);
                            return mine != null ? mine : en.anyInstance(msg.scriptId());
                        }).orElse(null);
                        if (inst instanceof SprauteScriptInstance si) {
                            si.setVariable(msg.name(), literal(si, msg.value()));
                            return null;
                        } else {
                            return "Сюжет #" + msg.scriptId() + " не запущен";
                        }
                }
            } catch (RuntimeException var7) {
                return "Не получилось: " + var7.getMessage();
            }
        }
    }

    private static Object literal(SprauteScriptInstance si, String text) {
        String t = text.trim();

        try {
            Ast.Expr expr = Parser.parseExpression(t);
            if (expr != null) {
                if (si != null) {
                    return si.eval().eval(expr);
                }

                if (expr instanceof Ast.NumberLit n) {
                    return n.value;
                }

                if (expr instanceof Ast.BoolLit b) {
                    return b.value;
                }

                if (expr instanceof Ast.StringLit s) {
                    return s.value;
                }
            }
        } catch (RuntimeException var5) {
        }

        return t;
    }

    public record DeployResult(boolean ok, int id, List<String> messages) {
    }

    public static final class Deployed {
        public final int id;
        public final String title;
        public final String project;
        public final String source;
        public final Ast.Program program;
        public final List<String> labels;

        Deployed(int id, String title, String project, String source, Ast.Program program, List<String> labels) {
            this.id = id;
            this.title = title;
            this.project = project;
            this.source = source;
            this.program = program;
            this.labels = labels;
        }
    }
}
