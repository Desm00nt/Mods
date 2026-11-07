package org.sprautestudio.spraute_engine.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.LevelResource;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.NpcQuestGoals;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;
import org.sprautestudio.spraute_engine.zone.Zones;

public final class Quests {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<String, Quests.PlayerState> players;
    private static long counter;

    private Quests() {
    }

    public static String key(String project, String name) {
        return project + "/" + name;
    }

    public static QuestDef def(String project, String name) {
        String json = SprauteRuntime.get().gui(project, "quest:" + name);
        return json == null ? null : QuestDef.fromJson(json);
    }

    public static String[] resolve(String project, String name) {
        String n = name == null ? "" : name.trim();
        if (def(project, n) != null) {
            return new String[]{project, n};
        } else {
            Map<String, Map<String, String>> all = SprauteRuntime.get().allGuis();
            List<String[]> own = new ArrayList<>();
            List<String[]> other = new ArrayList<>();

            for (Entry<String, Map<String, String>> pe : all.entrySet()) {
                for (Entry<String, String> ge : pe.getValue().entrySet()) {
                    if (ge.getKey().startsWith("quest:")) {
                        String file = ge.getKey().substring(6);
                        boolean match = file.equalsIgnoreCase(n);
                        if (!match) {
                            QuestDef d = QuestDef.fromJson(ge.getValue());
                            match = d.title.trim().equalsIgnoreCase(n);
                        }

                        if (match) {
                            (pe.getKey().equals(project) ? own : other).add(new String[]{pe.getKey(), file});
                        }
                    }
                }
            }

            if (!own.isEmpty()) {
                return own.get(0);
            } else {
                return other.size() == 1 ? other.get(0) : new String[]{project, n};
            }
        }
    }

    public static List<String> names(String project) {
        List<String> out = new ArrayList<>();
        Map<String, String> m = SprauteRuntime.get().allGuis().get(project);
        if (m != null) {
            for (String k : m.keySet()) {
                if (k.startsWith("quest:")) {
                    out.add(k.substring(6));
                }
            }
        }

        return out;
    }

    private static QuestDef def(String key) {
        int i = key.indexOf(47);
        return i < 0 ? null : def(key.substring(0, i), key.substring(i + 1));
    }

    private static Path file() {
        MinecraftServer s = Platform.get().server();
        return s == null ? null : s.getWorldPath(LevelResource.ROOT).resolve("spraute").resolve("quests.json");
    }

    private static Map<String, Quests.PlayerState> players() {
        if (players != null) {
            return players;
        } else {
            players = new LinkedHashMap<>();
            Path f = file();
            if (f != null && Files.exists(f)) {
                try {
                    Map<String, Quests.PlayerState> m = GSON.fromJson(
                        Files.readString(f, StandardCharsets.UTF_8), (new TypeToken<Map<String, Quests.PlayerState>>() {}).getType()
                    );
                    if (m != null) {
                        players.putAll(m);
                    }
                } catch (Exception var5) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot read quests", (Throwable)var5);
                }
            }

            for (Quests.PlayerState ps : players.values()) {
                for (Quests.QState q : ps.quests.values()) {
                    counter = Math.max(counter, q.order);
                }
            }

            return players;
        }
    }

    private static void save() {
        Path f = file();
        if (f != null && players != null) {
            try {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson(players), StandardCharsets.UTF_8);
            } catch (IOException var2) {
                SprauteEngine.LOGGER.warn("Spraute: cannot save quests", (Throwable)var2);
            }
        }
    }

    public static void unload() {
        players = null;
        counter = 0L;
    }

    private static Quests.PlayerState of(ServerPlayer p) {
        return players().computeIfAbsent(p.getUUID().toString(), k -> new Quests.PlayerState());
    }

    public static boolean start(ServerPlayer p, String project, String name) {
        QuestDef d = def(project, name);
        if (d == null) {
            return false;
        } else {
            String key = key(project, name);
            Quests.PlayerState ps = of(p);
            Quests.QState q = ps.quests.get(key);
            if (q != null && q.state.equals("active")) {
                return true;
            } else {
                q = new Quests.QState();
                q.order = ++counter;
                ps.quests.put(key, q);
                if (ps.tracked.isEmpty() || !"active".equals(stateOf(ps, ps.tracked))) {
                    ps.tracked = key;
                }

                changed(p, "Новый квест", d.title, "start");
                post(p, "queststart", name, "");
                return true;
            }
        }
    }

    private static String stateOf(Quests.PlayerState ps, String key) {
        Quests.QState q = ps.quests.get(key);
        return q == null ? "none" : q.state;
    }

    public static boolean progress(ServerPlayer p, String project, String name, String task, int amount, boolean set) {
        QuestDef d = def(project, name);
        if (d == null) {
            return false;
        } else {
            QuestDef.Task t = d.task(task);
            if (t == null) {
                throw new IllegalArgumentException(
                    "В квесте «" + name + "» нет задачи «" + task + "» (есть: " + String.join(", ", d.tasks.stream().map(x -> x.id).toList()) + ")"
                );
            } else {
                Quests.QState q = of(p).quests.get(key(project, name));
                if (q != null && q.state.equals("active")) {
                    int before = q.progress.getOrDefault(task, 0);
                    int now = Math.max(0, Math.min(t.count, set ? amount : before + amount));
                    if (now == before) {
                        return true;
                    } else {
                        q.progress.put(task, now);
                        q.revealed.add(task);
                        if (now >= t.count && before < t.count) {
                            post(p, "questtask", name, task);
                            if (d.autoComplete && allDone(d, q)) {
                                finish(p, project, name, d, q, true);
                                return true;
                            }

                            changed(p, "Задача выполнена", taskText(t), "task");
                        } else {
                            changed(p, "", "", "");
                        }

                        return true;
                    }
                } else {
                    return true;
                }
            }
        }
    }

    private static boolean allDone(QuestDef d, Quests.QState q) {
        for (QuestDef.Task t : d.tasks) {
            if (!t.optional && q.progress.getOrDefault(t.id, 0) < t.count) {
                return false;
            }
        }

        return true;
    }

    public static boolean reveal(ServerPlayer p, String project, String name, String task) {
        QuestDef d = def(project, name);
        if (d == null) {
            return false;
        } else {
            Quests.QState q = of(p).quests.get(key(project, name));
            if (q != null && q.revealed.add(task)) {
                changed(p, "Новая задача", d.task(task) != null ? taskText(d.task(task)) : task, "reveal");
            }

            return true;
        }
    }

    public static boolean end(ServerPlayer p, String project, String name, boolean done) {
        QuestDef d = def(project, name);
        if (d == null) {
            return false;
        } else {
            Quests.QState q = of(p).quests.get(key(project, name));
            if (q == null) {
                q = new Quests.QState();
                q.order = ++counter;
                of(p).quests.put(key(project, name), q);
            }

            if (!q.state.equals("active")) {
                return true;
            } else {
                finish(p, project, name, d, q, done);
                return true;
            }
        }
    }

    private static void finish(ServerPlayer p, String project, String name, QuestDef d, Quests.QState q, boolean done) {
        q.state = done ? "completed" : "failed";
        if (done) {
            for (QuestDef.Task t : d.tasks) {
                if (!t.optional) {
                    q.progress.put(t.id, t.count);
                }
            }

            reward(p, d.reward);
        }

        Quests.PlayerState ps = of(p);
        if (ps.tracked.equals(key(project, name))) {
            ps.tracked = "";

            for (Entry<String, Quests.QState> e : ps.quests.entrySet()) {
                if (e.getValue().state.equals("active")) {
                    ps.tracked = e.getKey();
                }
            }
        }

        changed(p, done ? "Квест выполнен" : "Квест провален", d.title, done ? "complete" : "fail");
        post(p, done ? "questdone" : "questfail", name, "");
    }

    public static void reset(ServerPlayer p, String project, String name) {
        Quests.PlayerState ps = of(p);
        if (ps.quests.remove(key(project, name)) != null) {
            if (ps.tracked.equals(key(project, name))) {
                ps.tracked = "";
            }

            changed(p, "", "", "");
        }
    }

    public static void track(ServerPlayer p, String key) {
        Quests.PlayerState ps = of(p);
        if (key.isEmpty() || ps.quests.containsKey(key)) {
            ps.tracked = ps.tracked.equals(key) ? "" : key;
            changed(p, "", "", "");
        }
    }

    private static boolean current(QuestDef d, Quests.QState q, QuestDef.Task t) {
        if (!d.sequential) {
            return true;
        } else {
            for (QuestDef.Task x : d.tasks) {
                if (x == t) {
                    return true;
                }

                if (!x.optional && q.progress.getOrDefault(x.id, 0) < x.count) {
                    return false;
                }
            }

            return false;
        }
    }

    public static void onEvent(String type, ServerPlayer player, String ownerOfEntity, Map<String, Object> data) {
        MinecraftServer server = Platform.get().server();
        if (player != null || server != null) {
            for (ServerPlayer p : player != null ? List.of(player) : new ArrayList<>(server.getPlayerList().getPlayers())) {
                if (player != null || ownerOfEntity == null || ownerOfEntity.equals(p.getUUID().toString())) {
                    Quests.PlayerState ps = players().get(p.getUUID().toString());
                    if (ps != null) {
                        for (Entry<String, Quests.QState> e : new ArrayList<>(ps.quests.entrySet())) {
                            Quests.QState q = e.getValue();
                            QuestDef d = q.state.equals("active") ? def(e.getKey()) : null;
                            if (d != null) {
                                String[] where = e.getKey().split("/", 2);

                                for (QuestDef.Task t : d.tasks) {
                                    int amount = current(d, q, t) ? QuestGoals.amount(t, type, data) : 0;
                                    if (amount > 0) {
                                        progress(p, where[0], where[1], t.id, amount, false);
                                        if (d.sequential || !q.state.equals("active")) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            Map<String, NpcQuestGoals.Wanted> wanted = new HashMap<>();

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                Quests.PlayerState ps = players().get(p.getUUID().toString());
                if (ps != null) {
                    for (Entry<String, Quests.QState> e : new ArrayList<>(ps.quests.entrySet())) {
                        Quests.QState q = e.getValue();
                        QuestDef d = q.state.equals("active") ? def(e.getKey()) : null;
                        if (d != null) {
                            String[] where = e.getKey().split("/", 2);

                            for (QuestDef.Task t : d.tasks) {
                                int left = t.count - q.progress.getOrDefault(t.id, 0);
                                if (left > 0 && current(d, q, t)) {
                                    if (t.goal.equals("npcpickup") && !t.who.isBlank()) {
                                        want(wanted, where[0], t, left, p);
                                    }

                                    if (t.goal.equals("zone") && !t.target.isBlank()) {
                                        if (Zones.playerIn(p, where[0], t.target)) {
                                            progress(p, where[0], where[1], t.id, t.count, true);
                                        }

                                        if (!q.state.equals("active")) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NpcQuestGoals.sync(wanted);
        }
    }

    private static void want(Map<String, NpcQuestGoals.Wanted> wanted, String project, QuestDef.Task t, int left, ServerPlayer p) {
        List<String> filters = t.target.isBlank() ? List.of() : List.of(t.target.contains(":") ? t.target : "minecraft:" + t.target);
        wanted.merge(NpcQuestGoals.key(project, t.who), new NpcQuestGoals.Wanted(filters, left, Set.of(p.getUUID())), (a, b) -> {
            boolean same = a.filters().equals(b.filters());
            List<String> all = new ArrayList<>(a.filters());

            for (String f : b.filters()) {
                if (!all.contains(f)) {
                    all.add(f);
                }
            }

            if (a.filters().isEmpty() || b.filters().isEmpty()) {
                all.clear();
            }

            Set<UUID> players = new HashSet<>(a.players());
            players.addAll(b.players());
            return new NpcQuestGoals.Wanted(all, same ? Math.max(a.left(), b.left()) : a.left() + b.left(), players);
        });
    }

    public static String goalProblem(String project, String name, String task) {
        QuestDef d = def(project, name);
        if (d == null) {
            return "Квест «" + name + "» не найден в папке «Квесты» проекта";
        } else {
            return d.task(task) != null
                ? null
                : "В квесте «" + name + "» нет задачи «" + task + "» (есть: " + String.join(", ", d.tasks.stream().map(x -> x.id).toList()) + ")";
        }
    }

    public static boolean goalDone(ServerPlayer p, String project, String name, String task) {
        if (p == null) {
            return false;
        } else {
            QuestDef d = def(project, name);
            QuestDef.Task t = d != null ? d.task(task) : null;
            Quests.QState q = of(p).quests.get(key(project, name));
            return t != null && q != null && (q.state.equals("completed") || q.progress.getOrDefault(task, 0) >= t.count);
        }
    }

    private static String taskText(QuestDef.Task t) {
        return QuestGoals.text(t, id -> targetName(QuestGoals.get(t.goal), id));
    }

    private static String targetName(QuestGoals.Goal g, String id) {
        if (g.target() == ParamType.ITEM) {
            return ItemSpec.error(id) == null && item(id) != Items.AIR ? ItemSpec.stack(id, 1).getHoverName().getString() : QuestGoals.shortId(id);
        } else {
            if (g.target() == ParamType.TARGET) {
                ResourceLocation type = ResourceLocation.tryParse(id.contains(":") ? id : "minecraft:" + id);
                if (type != null && Regs.ENTITY_TYPE.keySet().contains(type)) {
                    return Regs.ENTITY_TYPE.get(type).getDescription().getString();
                }
            }

            return QuestGoals.shortId(id);
        }
    }

    public static String state(ServerPlayer p, String project, String name) {
        String[] r = resolve(project, name);
        project = r[0];
        name = r[1];
        return p == null ? "none" : stateOf(of(p), key(project, name));
    }

    public static int progressOf(ServerPlayer p, String project, String name, String task) {
        String[] r = resolve(project, name);
        project = r[0];
        name = r[1];
        if (p == null) {
            return 0;
        } else {
            Quests.QState q = of(p).quests.get(key(project, name));
            return q == null ? 0 : q.progress.getOrDefault(task, 0);
        }
    }

    private static Item item(String id) {
        return ItemSpec.item(id);
    }

    private static void reward(ServerPlayer p, QuestDef.Reward r) {
        if (r.coins > 0) {
            Coins.add(p, r.coins);
        }

        if (r.xp > 0) {
            p.giveExperiencePoints(r.xp);
        }

        for (QuestDef.Item i : r.items) {
            Item it = item(i.item);
            if (it != Items.AIR) {
                int left = Math.max(1, i.count);

                while (left > 0) {
                    ItemStack s = ItemSpec.stack(i.item, Math.min(left, it.getMaxStackSize()));
                    left -= s.getCount();
                    if (!p.addItem(s)) {
                        p.drop(s, false);
                    }
                }
            }
        }
    }

    private static void post(ServerPlayer p, String type, String quest, String task) {
        Map<String, Object> data = new HashMap<>();
        data.put("quest", quest);
        data.put("task", task);
        GameEvents.post(type, p, data);
    }

    private static void changed(ServerPlayer p, String toastTitle, String toastText, String kind) {
        save();
        QuestJournal j = journal(p);
        if (!kind.isEmpty()) {
            j.toast = toastTitle + "\n" + toastText;
            j.toastKind = kind;
        }

        SprauteNetwork.sendTo(p, new Packets.QuestSync(j.toJson(), false));
    }

    public static void sync(ServerPlayer p, boolean open) {
        SprauteNetwork.sendTo(p, new Packets.QuestSync(journal(p).toJson(), open));
    }

    public static QuestJournal journal(ServerPlayer p) {
        Quests.PlayerState ps = of(p);
        QuestJournal j = new QuestJournal();
        j.tracked = ps.tracked;

        for (Entry<String, Quests.QState> e : ps.quests.entrySet()) {
            QuestDef d = def(e.getKey());
            if (d != null) {
                Quests.QState q = e.getValue();
                QuestJournal.Entry en = new QuestJournal.Entry();
                en.key = e.getKey();
                en.title = d.title;
                en.giver = d.giver;
                en.description = d.description;
                en.layout = d.layout;
                en.state = q.state;
                en.order = q.order;
                en.reward = QuestJournal.rewardText(d.reward, i -> ItemSpec.stack(i.item, 1).getHoverName().getString());

                for (QuestDef.Task t : d.tasks) {
                    int prog = q.progress.getOrDefault(t.id, 0);
                    if (!t.hidden || q.revealed.contains(t.id) || prog != 0) {
                        QuestJournal.Task x = new QuestJournal.Task();
                        x.id = t.id;
                        x.text = taskText(t);
                        x.count = t.count;
                        x.optional = t.optional;
                        x.progress = prog;
                        en.tasks.add(x);
                        if (d.sequential && prog < t.count && !t.optional && q.state.equals("active")) {
                            break;
                        }
                    }
                }

                j.quests.add(en);
            }
        }

        j.quests.sort(Comparator.comparingLong(xx -> -xx.order));
        return j;
    }

    static final class PlayerState {
        Map<String, Quests.QState> quests = new LinkedHashMap<>();
        String tracked = "";
    }

    static final class QState {
        String state = "active";
        long order;
        Map<String, Integer> progress = new LinkedHashMap<>();
        Set<String> revealed = new LinkedHashSet<>();
    }
}
