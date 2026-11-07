package org.sprautestudio.spraute_engine.core.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public abstract class ScriptInstance {
    public final ScriptEnvironment env;
    private final int id;
    private final Random random = new Random();
    private final ScriptFlags flags = new ScriptFlags();
    private ScriptPerformer main;
    final Map<String, ScriptPerformer> known = new HashMap<>();
    final Map<String, ScriptPerformer> running = new LinkedHashMap<>();
    private boolean paused;
    int triggerSeq;
    private CompoundTag checkpoint;
    private final Map<String, String> notes = new LinkedHashMap<>();
    private boolean replaying;
    final List<ScriptInstance.Line> journal = new ArrayList<>();
    public UUID owner;

    protected ScriptInstance(ScriptEnvironment env, int id) {
        this.env = env;
        this.id = id;
        this.owner = env.startingFor;
    }

    public boolean solo() {
        return false;
    }

    public ServerPlayer protagonist() {
        PlayerList players = this.env.server().getPlayerList();
        ServerPlayer p = this.owner != null ? players.getPlayer(this.owner) : null;
        if (p == null && !this.solo()) {
            return players.getPlayers().isEmpty() ? null : players.getPlayers().get(0);
        } else {
            return p;
        }
    }

    long keyCount(String key) {
        UUID as = SprauteRuntime.getAs();
        if (as != null) {
            return this.env.keyCount(as, key);
        } else {
            return this.solo() && this.owner != null ? this.env.keyCount(this.owner, key) : this.env.keyCount(key);
        }
    }

    public void catchUp(ServerPlayer p) {
    }

    public int getId() {
        return this.id;
    }

    public Random getRandom() {
        return this.random;
    }

    public ScriptFlags getFlags() {
        return this.flags;
    }

    public ScriptPerformer getMainPerformer() {
        return this.main;
    }

    protected void registerMainPerformer(ScriptPerformer p) {
        this.main = p;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setPaused(boolean p) {
        this.paused = p;
    }

    public <T extends Entity> Optional<T> optional(Character<T> c) {
        return Characters.find(c);
    }

    void note(String key, ScriptPerformer by) {
        if (!this.replaying) {
            String name = by == this.main ? "" : null;
            if (name == null) {
                for (Entry<String, ScriptPerformer> en : this.known.entrySet()) {
                    if (en.getValue() == by) {
                        name = en.getKey();
                    }
                }
            }

            if (name != null) {
                this.notes.remove(key);
                this.notes.put(key, by.getActionIndex() + "/" + name);
            }
        }
    }

    void forgetNotes(String prefix) {
        this.notes.keySet().removeIf(k -> k.startsWith(prefix));
    }

    boolean replaying() {
        return this.replaying;
    }

    private void replay() {
        this.replaying = true;

        try {
            for (String where : new ArrayList<>(this.notes.values())) {
                int cut = where.indexOf(47);
                int index = Integer.parseInt(where.substring(0, cut));
                String name = where.substring(cut + 1);
                ScriptPerformer p = name.isEmpty() ? this.main : this.known.get(name);
                if (p != null && index < p.actions.size()) {
                    p.actions.get(index).update();
                }
            }
        } finally {
            this.replaying = false;
        }
    }

    public int priorityOf(Character<?> c) {
        return 0;
    }

    public List<ScriptInstance.Line> journal() {
        return this.journal;
    }

    public void handleStop() {
    }

    public void startParallel(String name, ScriptPerformer sub, ScriptPerformer by) {
        this.stopParallel(name);
        this.known.put(name, sub);
        sub.restart();
        if (by != null) {
            sub.inheritAs(by.asPlayer);
        }

        this.running.put(name, sub);
    }

    public Map<String, ScriptPerformer> runningBranches() {
        return Collections.unmodifiableMap(this.running);
    }

    public void stopParallel(String name) {
        ScriptPerformer p = this.running.remove(name);
        if (p != null) {
            p.stop();
        }
    }

    void stopPerformers() {
        if (this.main != null) {
            this.main.stop();
        }

        for (ScriptPerformer p : this.running.values()) {
            p.stop();
        }
    }

    protected Map<String, Consumer<Player>> getTriggers() {
        return new HashMap<>();
    }

    public void trigger(String name, Player p) {
        Consumer<Player> t = this.getTriggers().get(name);
        if (t != null) {
            t.accept(p);
        }

        this.triggerSeq++;
    }

    public boolean goTo(String label) {
        if (this.main != null && this.main.goTo(label)) {
            return true;
        } else {
            for (ScriptPerformer p : this.running.values()) {
                if (p.goTo(label)) {
                    return true;
                }
            }

            return false;
        }
    }

    void tick() {
        if (!this.paused && this.main != null) {
            this.main.tick();

            for (ScriptPerformer p : new ArrayList<>(this.running.values())) {
                p.tick();
            }
        }
    }

    void passCheckpoint(String name, ScriptPerformer by) {
        by.setStage(name);
        CompoundTag tag = this.serializeNBT();
        if (by == this.main) {
            tag.putInt("main", this.main.getActionIndex() + 1);
        } else {
            this.running.forEach((k, p) -> {
                if (p == by) {
                    tag.getCompound("parallel").putInt(k, p.getActionIndex() + 1);
                }
            });
        }

        tag.putString("stage", name);
        this.checkpoint = tag;
        Map<String, Object> d = new HashMap<>();
        d.put("id", name);
        GameEvents.fire("checkpoint", null, d);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("flags", this.flags.save());
        tag.putBoolean("paused", this.paused);
        if (this.main != null) {
            tag.putInt("main", this.main.getActionIndex());
        }

        CompoundTag par = new CompoundTag();
        this.running.forEach((k, p) -> par.putInt(k, p.getActionIndex()));
        tag.put("parallel", par);
        ListTag kept = new ListTag();
        this.notes.forEach((k, v) -> {
            CompoundTag t = new CompoundTag();
            t.putString("key", k);
            t.putString("step", v);
            kept.add(t);
        });
        tag.put("notes", kept);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.flags.load(tag.getCompound("flags"));
        this.paused = tag.getBoolean("paused");
        if (this.main != null) {
            this.main.jumpToIndex(tag.getInt("main"));
            this.main.setStage(tag.getString("stage"));
        }

        CompoundTag par = tag.getCompound("parallel");
        this.running.clear();

        for (String k : par.getAllKeys()) {
            ScriptPerformer p = this.known.get(k);
            if (p != null) {
                p.jumpToIndex(par.getInt(k));
                this.running.put(k, p);
            }
        }

        this.notes.clear();
        ListTag kept = tag.getList("notes", 10);

        for (int i = 0; i < kept.size(); i++) {
            this.notes.put(kept.getCompound(i).getString("key"), kept.getCompound(i).getString("step"));
        }

        this.replay();
    }

    CompoundTag saveState() {
        CompoundTag tag = new CompoundTag();
        if (this.owner != null) {
            tag.putUUID("owner", this.owner);
        }

        if (this.checkpoint != null) {
            tag.put("checkpoint", this.checkpoint);
        }

        ListTag lines = new ListTag();

        for (ScriptInstance.Line l : this.journal) {
            CompoundTag t = new CompoundTag();
            t.putString("name", l.name());
            t.putInt("color", l.color());
            t.putString("icon", l.icon());
            t.putString("text", l.text());
            t.putBoolean("think", l.think());
            lines.add(t);
        }

        tag.put("journal", lines);
        return tag;
    }

    void loadState(CompoundTag tag) {
        this.journal.clear();
        ListTag lines = tag.getList("journal", 10);

        for (int i = 0; i < lines.size(); i++) {
            CompoundTag t = lines.getCompound(i);
            this.journal.add(new ScriptInstance.Line(t.getString("name"), t.getInt("color"), t.getString("icon"), t.getString("text"), t.getBoolean("think")));
        }

        if (tag.contains("checkpoint")) {
            this.checkpoint = tag.getCompound("checkpoint");
            this.deserializeNBT(this.checkpoint);
        }
    }

    public record Line(String name, int color, String icon, String text, boolean think) {
    }
}
