package org.sprautestudio.spraute_engine.core.script;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.addon.Addons;
import org.sprautestudio.spraute_engine.build.BlockMoves;
import org.sprautestudio.spraute_engine.build.Builds;
import org.sprautestudio.spraute_engine.camera.Cams;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.post.Posts;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.runtime.WorldPanels;
import org.sprautestudio.spraute_engine.stealth.Stealth;

public final class ScriptEnvironment {
    public static BiConsumer<Integer, Throwable> onError = (id, t) -> SprauteEngine.LOGGER.error("Story #{} failed", id, t);
    public final Map<Integer, Constructor<? extends ScriptInstance>> constructors = new TreeMap<>();
    public final Map<Integer, ScriptInstance> scriptInstances = new LinkedHashMap<>();
    public final Map<String, int[]> categories = new LinkedHashMap<>();
    private final MinecraftServer server;
    private final Map<String, Long> keys = new HashMap<>();
    private final Map<UUID, Map<String, Long>> playerKeys = new HashMap<>();
    public static final int COPY = 1000000;
    UUID startingFor;
    private CompoundTag pendingLoad;
    private static final boolean WITHOUT_PLAYERS = Boolean.getBoolean("spraute.tickWithoutPlayers");

    public ScriptEnvironment(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer server() {
        return this.server;
    }

    public static int baseId(int id) {
        return id % 1000000;
    }

    public void keyPressed(UUID player, String key) {
        this.keys.merge(key, 1L, Long::sum);
        this.playerKeys.computeIfAbsent(player, k -> new HashMap<>()).merge(key, 1L, Long::sum);
    }

    public long keyCount(String key) {
        return this.keys.getOrDefault(key, 0L);
    }

    public long keyCount(UUID player, String key) {
        Map<String, Long> m = this.playerKeys.get(player);
        return m == null ? 0L : m.getOrDefault(key, 0L);
    }

    private boolean solo(int base) {
        SprauteRuntime.Deployed d = SprauteRuntime.get().deployed(base);
        return d != null && d.program.header != null && d.program.header.solo;
    }

    public ScriptInstance instanceFor(int base, ServerPlayer p) {
        if (!this.solo(base)) {
            return this.scriptInstances.get(base);
        } else {
            for (ScriptInstance s : this.scriptInstances.values()) {
                if (baseId(s.getId()) == base && p.getUUID().equals(s.owner)) {
                    return s;
                }
            }

            return null;
        }
    }

    public ScriptInstance anyInstance(int id) {
        ScriptInstance s = this.scriptInstances.get(id);
        if (s != null) {
            return s;
        } else {
            for (ScriptInstance o : this.scriptInstances.values()) {
                if (baseId(o.getId()) == id) {
                    return o;
                }
            }

            return null;
        }
    }

    public ScriptInstance startFor(int base, ServerPlayer p) {
        ScriptInstance have = p != null ? this.instanceFor(base, p) : this.scriptInstances.get(base);
        if (have != null) {
            return have;
        } else {
            int id = base;
            if (this.solo(base)) {
                if (p == null) {
                    return null;
                }

                int n = 1;

                while (this.scriptInstances.containsKey(base + n * 1000000)) {
                    n++;
                }

                id = base + n * 1000000;
            }

            this.startingFor = p != null ? p.getUUID() : null;

            ScriptInstance var9;
            try {
                var9 = this.start(id);
            } finally {
                this.startingFor = null;
            }

            return var9;
        }
    }

    public ScriptInstance start(int id) {
        if (this.scriptInstances.containsKey(id)) {
            return this.scriptInstances.get(id);
        } else {
            Constructor<? extends ScriptInstance> c = this.constructors.get(baseId(id));
            if (c == null) {
                return null;
            } else {
                UUID outerAs = SprauteRuntime.getAs();
                SprauteRuntime.setAs(this.startingFor);
                ScriptInstance outer = SprauteRuntime.enter(null);

                try {
                    ScriptInstance s = c.newInstance(this, id);
                    this.scriptInstances.put(id, s);
                    SprauteRuntime.enter(s);
                    SprauteNetwork.sendToAll(new Packets.SpeechClear(id));
                    this.scriptEvent("scriptstart", id);
                    this.announce();
                    return s;
                } catch (InvocationTargetException var11) {
                    onError.accept(id, (Throwable)(var11.getCause() != null ? var11.getCause() : var11));
                } catch (ReflectiveOperationException var12) {
                    onError.accept(id, var12);
                } finally {
                    SprauteRuntime.setAs(outerAs);
                    SprauteRuntime.enter(outer);
                }

                return null;
            }
        }
    }

    public void stop(int id) {
        if (id < 1000000) {
            for (int other : new ArrayList<>(this.scriptInstances.keySet())) {
                if (other >= 1000000 && baseId(other) == id) {
                    this.stop(other);
                }
            }
        }

        ScriptInstance s = this.scriptInstances.remove(id);
        if (s != null) {
            ScriptInstance outer = SprauteRuntime.enter(s);

            try {
                s.stopPerformers();
                s.handleStop();
                Characters.release(id);
                Posts.storyStopped(id);
                Cams.storyStopped(id);
                Builds.storyStopped(id);
                BlockMoves.storyStopped(id);
                Addons.storyStopped(id);
                Stealth.storyStopped(id);
                WorldPanels.removeAll(id);
                this.scriptEvent("scriptstop", id);
            } finally {
                SprauteRuntime.enter(outer);
            }

            this.announce();
        }
    }

    private void scriptEvent(String name, int id) {
        Map<String, Object> d = new HashMap<>();
        d.put("id", String.valueOf(id));
        GameEvents.fire(name, null, d);
    }

    public int newest(ServerPlayer p) {
        int last = -1;

        for (ScriptInstance s : this.scriptInstances.values()) {
            if (!s.solo() || p.getUUID().equals(s.owner)) {
                last = s.getId();
            }
        }

        return last;
    }

    public void announce() {
        for (ServerPlayer p : this.server.getPlayerList().getPlayers()) {
            SprauteNetwork.sendTo(p, new Packets.SpeechNewest(this.newest(p)));
        }
    }

    public void sendTo(ServerPlayer p) {
        int id = this.newest(p);
        ScriptInstance s = this.scriptInstances.get(id);
        SprauteNetwork.sendTo(p, new Packets.SpeechNewest(id));
        if (s != null) {
            for (ScriptInstance.Line l : s.journal) {
                SprauteNetwork.sendTo(p, new Packets.Speech(id, l.name(), l.color(), l.icon(), l.text(), "", l.think(), false, true));
            }
        }

        for (ScriptInstance o : this.scriptInstances.values()) {
            if (!o.solo() || p.getUUID().equals(o.owner)) {
                o.catchUp(p);
            }
        }
    }

    public void stopAll() {
        for (int id : new ArrayList<>(this.scriptInstances.keySet())) {
            this.stop(id);
        }
    }

    public void tick() {
        this.restore();
        if (WITHOUT_PLAYERS || this.server.getPlayerCount() != 0) {
            for (ScriptInstance s : new ArrayList<>(this.scriptInstances.values())) {
                if (!s.solo() || s.protagonist() != null) {
                    SprauteRuntime.enter(s);

                    try {
                        s.tick();
                        ScriptPerformer main = s.getMainPerformer();
                        if (main == null || main.isFinished()) {
                            this.stop(s.getId());
                        }
                    } catch (Throwable var7) {
                        onError.accept(s.getId(), var7);
                        this.stop(s.getId());
                    } finally {
                        SprauteRuntime.setAs(null);
                        SprauteRuntime.enter(null);
                    }
                }
            }
        }
    }

    public List<Integer> category(String name) {
        List<Integer> out = new ArrayList<>();
        int[] ids = this.categories.get(name);
        if (ids != null) {
            for (int i : ids) {
                out.add(i);
            }
        }

        return out;
    }

    private Path file() {
        return this.server.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("running.dat");
    }

    public void save() {
        CompoundTag root = new CompoundTag();
        root.put("characters", Characters.save());
        CompoundTag list = new CompoundTag();
        this.scriptInstances.forEach((id, s) -> {
            try {
                list.put(String.valueOf(id), s.saveState());
            } catch (RuntimeException var4x) {
                SprauteEngine.LOGGER.warn("Failed to save story {}", id, var4x);
            }
        });
        root.put("scripts", list);

        try {
            Files.createDirectories(this.file().getParent());
            NbtIo.writeCompressed(root, this.file().toFile());
        } catch (IOException var4) {
            SprauteEngine.LOGGER.warn("Failed to save running stories", (Throwable)var4);
        }
    }

    public void load() {
        Characters.clear();
        if (Files.isRegularFile(this.file())) {
            try {
                this.pendingLoad = NbtIo.readCompressed(this.file().toFile());
                Characters.load(this.pendingLoad.getCompound("characters"));
            } catch (IOException var2) {
                SprauteEngine.LOGGER.warn("Failed to read running stories", (Throwable)var2);
            }
        }
    }

    private void restore() {
        if (this.pendingLoad != null) {
            CompoundTag list = this.pendingLoad.getCompound("scripts");
            this.pendingLoad = null;

            for (String k : list.getAllKeys()) {
                try {
                    CompoundTag state = list.getCompound(k);
                    this.startingFor = state.hasUUID("owner") ? state.getUUID("owner") : null;
                    ScriptInstance s = this.start(Integer.parseInt(k));
                    this.startingFor = null;
                    if (s != null) {
                        s.loadState(state);
                    }
                } catch (RuntimeException var6) {
                    SprauteEngine.LOGGER.warn("Failed to restore story {}", k, var6);
                }
            }

            Characters.afterRestore(this);
        }
    }
}
