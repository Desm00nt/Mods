package org.sprautestudio.spraute_engine.skills;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlot.Type;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.script.ScriptEnvironment;
import org.sprautestudio.spraute_engine.core.script.ScriptInstance;
import org.sprautestudio.spraute_engine.lang.EventDefs;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.player.PlayerRuleDefs;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.ItemSpec;
import org.sprautestudio.spraute_engine.runtime.PlayerRules;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Skills {
    private static final String DATA = "spraute_skills";
    private static final Gson GSON = new Gson();
    private static final int FIRST_CODE_ID = 800000;
    private static final int CODE_IDS = 190000;
    private static final int LASTING_TICKS = 400;
    private static final int RENEW_BELOW = 220;
    private static final Map<String, String> PARSED_FROM = new HashMap<>();
    private static final Map<String, Skills.Tree> PARSED = new LinkedHashMap<>();
    private static Map<String, List<Skills.Lock>> locks = Map.of();
    private static Map<String, List<Skills.Opener>> keysOpen = Map.of();
    private static final Map<UUID, Set<String>> RULES_SET = new HashMap<>();
    private static final Map<UUID, Set<String>> POTIONS_SET = new HashMap<>();
    private static final Map<UUID, Map<String, Integer>> READY_AT = new HashMap<>();
    private static final Map<UUID, String> OPEN = new HashMap<>();
    private static final Map<UUID, Map<String, SkillTree>> TREE_SENT = new HashMap<>();
    private static final Map<Integer, Skills.CodeOwner> CODE_OWNERS = new HashMap<>();
    private static final Map<UUID, Set<String>> PASSIVE_STARTED = new HashMap<>();
    private static final Set<String> BROKEN = new HashSet<>();
    private static final Map<UUID, Integer> LOCK_SAID = new HashMap<>();
    private static int ticks;

    private Skills() {
    }

    private static Skills.Saved saved(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(Skills.Saved::load, Skills.Saved::new, "spraute_skills");
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END) {
                ticks++;
                if (ticks % 40 == 0) {
                    keepUp();
                }

                if (ticks % 20 == 0) {
                    tellCooldowns();
                }
            }
        });
        Hooks.PLAYER_LOGGED_IN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                apply(p);
                syncAll(p);
            }
        });
        Hooks.PLAYER_RESPAWN.add(e -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                apply(p);
            }
        });
        Hooks.PLAYER_LOGGED_OUT.add(e -> {
            UUID id = e.getEntity().getUUID();
            TREE_SENT.remove(id);
            PASSIVE_STARTED.remove(id);
            OPEN.remove(id);
            LOCK_SAID.remove(id);
        });
        Hooks.BLOCK_BREAK.add(e -> {
            if (e.getPlayer() instanceof ServerPlayer p && blocked(p, p.getMainHandItem())) {
                e.setCanceled(true);
            }
        });
    }

    public static void unload() {
        PARSED.clear();
        PARSED_FROM.clear();
        locks = Map.of();
        keysOpen = Map.of();
        RULES_SET.clear();
        POTIONS_SET.clear();
        READY_AT.clear();
        TREE_SENT.clear();
        CODE_OWNERS.clear();
        PASSIVE_STARTED.clear();
        BROKEN.clear();
        OPEN.clear();
        LOCK_SAID.clear();
    }

    public static List<Skills.Tree> trees() {
        return List.of();
    }

    private static void index() {
        Map<String, List<Skills.Lock>> newLocks = new HashMap<>();
        Map<String, List<Skills.Opener>> newKeys = new HashMap<>();

        for (Skills.Tree t : PARSED.values()) {
            for (SkillTree.Menu m : t.def.menus) {
                if (!m.openKey.isBlank()) {
                    newKeys.computeIfAbsent(m.openKey, k -> new ArrayList<>()).add(new Skills.Opener(t, m.id));
                }
            }

            for (SkillTree.Node n : t.def.nodes) {
                for (SkillTree.Effect e : n.effects) {
                    if (e.kind.equals("item") && !e.item.isBlank()) {
                        newLocks.computeIfAbsent(ItemSpec.id(e.item), k -> new ArrayList<>()).add(new Skills.Lock(t, n));
                    }
                }
            }
        }

        locks = newLocks;
        keysOpen = newKeys;
    }

    public static Skills.Tree tree(String project, String name) {
        Skills.Tree any = null;

        for (Skills.Tree t : trees()) {
            if (t.name.equals(name)) {
                if (t.project.equals(project)) {
                    return t;
                }

                any = t;
            }
        }

        return any;
    }

    public static SkillLogic.State state(ServerPlayer p, Skills.Tree t) {
        Skills.Saved s = saved(p.server);
        return s.players.computeIfAbsent(p.getUUID().toString(), k -> new HashMap<>()).computeIfAbsent(t.key(), k -> new SkillLogic.State());
    }

    private static void dirty(ServerPlayer p) {
        saved(p.server).setDirty();
    }

    public static void onEvent(String type, Player player, Map<String, Object> data) {
        if (player instanceof ServerPlayer p) {
            List<Skills.Tree> all = trees();
            if (!all.isEmpty()) {
                if (type.equals("keypress") && data.get("key") instanceof String key) {
                    pressed(p, key);
                }

                for (Skills.Tree t : all) {
                    SkillLogic.State s = state(p, t);
                    if (!s.off) {
                        for (SkillTree.Source src : t.def.sources) {
                            if (src.event.equals(type) && matches(type, src.filter, data)) {
                                if (src.currency != null && !src.currency.isEmpty() && src.points != 0) {
                                    points(p, t, src.currency, src.points);
                                }

                                if (src.xp > 0.0F) {
                                    xp(p, t, Math.round(src.xp));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean matches(String type, String filter, Map<String, Object> data) {
        if (filter != null && !filter.isBlank()) {
            EventDefs.Def def = EventDefs.ALL.get(type);
            if (def == null) {
                return false;
            } else {
                String want = ItemSpec.id(filter.trim());

                for (String field : def.matches()) {
                    Object have = data.get(field);
                    if (have != null && (want.equals(String.valueOf(have)) || filter.trim().equals(String.valueOf(have)))) {
                        return true;
                    }
                }

                return false;
            }
        } else {
            return true;
        }
    }

    public static void xp(ServerPlayer p, Skills.Tree t, long amount) {
        SkillLogic.State s = state(p, t);
        List<Integer> reached = SkillLogic.addXp(t.def, s, amount);
        dirty(p);

        for (int level : reached) {
            p.displayClientMessage(Component.literal(t.def.title + ": уровень " + level), true);
            Mc.level(p).playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.0F);
            if (!t.def.levelCommand.isBlank()) {
                command(p, t.def.levelCommand);
            }

            Map<String, Object> d = new LinkedHashMap<>();
            d.put("tree", t.name);
            d.put("level", level);
            GameEvents.post("skilllevel", p, d);
        }

        sync(p, t);
    }

    public static void points(ServerPlayer p, Skills.Tree t, String currency, int amount) {
        SkillLogic.State s = state(p, t);
        s.points.put(currency, Math.max(0, s.points(currency) + amount));
        dirty(p);
        sync(p, t);
    }

    public static void learn(ServerPlayer p, Skills.Tree t, SkillTree.Node n) {
        SkillLogic.State s = state(p, t);
        if (!SkillLogic.learn(t.def, s, n)) {
            sync(p, t);
        } else {
            learned(p, t, n, s);
        }
    }

    public static void grant(ServerPlayer p, Skills.Tree t, SkillTree.Node n) {
        SkillLogic.State s = state(p, t);
        if (s.rank(n.id) < n.maxRank) {
            SkillLogic.grant(n, s);
            learned(p, t, n, s);
        }
    }

    private static void learned(ServerPlayer p, Skills.Tree t, SkillTree.Node n, SkillLogic.State s) {
        dirty(p);

        for (SkillTree.Effect e : n.effects) {
            if (e.kind.equals("command") && !e.command.isBlank()) {
                command(p, e.command);
            }
        }

        apply(p);
        if (!n.learnCode.isBlank()) {
            runCode(p, t, n, "изучен", n.learnCode);
        }

        Mc.level(p).playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.4F, 1.5F);
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("tree", t.name);
        d.put("node", n.id);
        d.put("level", s.rank(n.id));
        GameEvents.post("skilllearn", p, d);
        sync(p, t);
    }

    public static void forget(ServerPlayer p, Skills.Tree t, SkillTree.Node n) {
        state(p, t).ranks.remove(n.id);
        dirty(p);
        apply(p);
        sync(p, t);
    }

    public static void reset(ServerPlayer p, Skills.Tree t) {
        SkillLogic.reset(t.def, state(p, t));
        dirty(p);
        apply(p);
        sync(p, t);
    }

    public static void setLevel(ServerPlayer p, Skills.Tree t, int level) {
        SkillLogic.State s = state(p, t);
        s.level = Math.max(1, Math.min(t.def.maxLevel, level));
        s.xp = 0L;
        dirty(p);
        sync(p, t);
    }

    public static void setOff(ServerPlayer p, Skills.Tree t, boolean off) {
        state(p, t).off = off;
        dirty(p);
        sync(p, t);
    }

    public static void action(ServerPlayer p, Packets.SkillAction msg) {
        trees();
        Skills.Tree t = PARSED.get(msg.key());
        if (t != null) {
            SkillLogic.State s = state(p, t);
            String var4 = msg.action();
            switch (var4) {
                case "learn":
                    SkillTree.Node nx = t.def.node(msg.node());
                    if (nx != null && !s.off) {
                        learn(p, t, nx);
                    }
                    break;
                case "use":
                    SkillTree.Node n = t.def.node(msg.node());
                    if (n != null && !s.off) {
                        use(p, t, n);
                    }
                    break;
                case "reset":
                    if (t.def.allowReset) {
                        reset(p, t);
                    }
                    break;
                case "closed":
                    OPEN.remove(p.getUUID());
            }
        }
    }

    private static void pressed(ServerPlayer p, String key) {
        List<Skills.Opener> opens = keysOpen.get(key);
        if (opens != null) {
            for (Skills.Opener o : opens) {
                if (!state(p, o.tree).off) {
                    open(p, o.tree, o.menu);
                    return;
                }
            }
        }

        for (Skills.Tree t : trees()) {
            SkillLogic.State s = state(p, t);
            if (!s.off) {
                for (SkillTree.Node n : t.def.nodes) {
                    if (n.ability.on && key.equals(n.ability.key) && s.rank(n.id) > 0) {
                        use(p, t, n);
                    }
                }
            }
        }
    }

    private static int cooldownLeft(ServerPlayer p, Skills.Tree t, SkillTree.Node n) {
        Map<String, Integer> m = READY_AT.get(p.getUUID());
        Integer at = m != null ? m.get(t.key() + "/" + n.id) : null;
        return at == null ? 0 : Math.max(0, at - ticks);
    }

    public static void use(ServerPlayer p, Skills.Tree t, SkillTree.Node n) {
        if (n.ability.on && state(p, t).rank(n.id) != 0) {
            int left = cooldownLeft(p, t, n);
            if (left > 0) {
                p.displayClientMessage(Component.literal(n.title + ": ещё " + (left + 19) / 20 + " с"), true);
            } else {
                SkillTree.Ability a = n.ability;
                switch (a.action) {
                    case "heal":
                        p.heal(a.power);
                        break;
                    case "dash":
                        Vec3 look = p.getLookAngle();
                        p.setDeltaMovement(look.x * a.power, Math.max(0.2, look.y * a.power * 0.5), look.z * a.power);
                        p.hurtMarked = true;
                        p.fallDistance = 0.0F;
                        break;
                    case "potion":
                        MobEffect effect = effect(a.potion);
                        if (effect != null) {
                            p.addEffect(new MobEffectInstance(effect, Math.round(a.seconds * 20.0F), Math.max(0, a.level - 1)));
                        }
                        break;
                    case "command":
                        command(p, a.command);
                        break;
                    case "code":
                        runCode(p, t, n, "способность", a.code);
                }

                if (a.cooldown > 0.0F) {
                    READY_AT.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(t.key() + "/" + n.id, ticks + Math.round(a.cooldown * 20.0F));
                }

                Map<String, Object> d = new LinkedHashMap<>();
                d.put("tree", t.name);
                d.put("node", n.id);
                GameEvents.post("skilluse", p, d);
                sync(p, t);
            }
        }
    }

    private static void runCode(ServerPlayer p, Skills.Tree t, SkillTree.Node n, String what, String code) {
        if (code != null && !code.isBlank()) {
            int id = codeId(t, n, what);
            String source = codeSource(id, n, what, code);
            if (!BROKEN.contains(source)) {
                SprauteRuntime rt = SprauteRuntime.get();
                CODE_OWNERS.put(id, new Skills.CodeOwner(t, n));
                SprauteRuntime.Deployed have = rt.deployed(id);
                if (have == null || !source.equals(have.source)) {
                    SprauteRuntime.DeployResult result = rt.deploy(t.project, source);
                    if (!result.ok()) {
                        BROKEN.add(source);
                        SprauteEngine.LOGGER.warn("Spraute: the code of skill {} does not compile: {}", n.id, result.messages());
                        p.displayClientMessage(Component.literal(n.title + ": ошибка в коде навыка"), true);
                        return;
                    }
                }

                SprauteRuntime.env().ifPresent(env -> {
                    ScriptInstance running = env.instanceFor(id, p);
                    if (running != null) {
                        env.stop(running.getId());
                    }

                    env.startFor(id, p);
                });
            }
        }
    }

    private static int codeId(Skills.Tree t, SkillTree.Node n, String what) {
        return 800000 + Math.floorMod((t.key() + "/" + n.id + "/" + what).hashCode(), 190000);
    }

    private static String codeSource(int id, SkillTree.Node n, String what, String code) {
        return "script " + id + " \"Навык " + n.title.replace("\"", "") + ": " + what + "\" solo\n" + code;
    }

    public static int powerOf(int scriptId, ServerPlayer p) {
        Skills.CodeOwner owner = CODE_OWNERS.get(scriptId);
        return owner != null && p != null ? state(p, owner.tree).rank(owner.node.id) : 0;
    }

    private static void keepPassive(ServerPlayer p, Skills.Tree t, SkillTree.Node n, boolean on) {
        if (!n.passiveCode.isBlank()) {
            ScriptEnvironment env = SprauteRuntime.env().orElse(null);
            if (env != null) {
                String what = "пока изучен";
                String key = t.key() + "/" + n.id;
                int id = codeId(t, n, what);
                ScriptInstance running = env.instanceFor(id, p);
                Set<String> started = PASSIVE_STARTED.computeIfAbsent(p.getUUID(), k -> new HashSet<>());
                if (!on) {
                    if (running != null) {
                        env.stop(running.getId());
                    }

                    started.remove(key);
                } else {
                    SprauteRuntime.Deployed have = SprauteRuntime.get().deployed(id);
                    boolean same = have != null && codeSource(id, n, what, n.passiveCode).equals(have.source);
                    if (!same || running == null) {
                        if (!same || !started.contains(key) || n.passiveCode.matches("(?s)(.*\\n)?\\s*on\\s.*")) {
                            started.add(key);
                            runCode(p, t, n, what, n.passiveCode);
                        }
                    }
                }
            }
        }
    }

    private static void tellCooldowns() {
        if (!READY_AT.isEmpty()) {
            MinecraftServer server = Platform.get().server();
            if (server != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    Map<String, Integer> ready = READY_AT.get(p.getUUID());
                    if (ready != null && !ready.isEmpty()) {
                        for (Skills.Tree t : trees()) {
                            String prefix = t.key() + "/";
                            boolean news = false;

                            for (Entry<String, Integer> en : ready.entrySet()) {
                                if (en.getKey().startsWith(prefix)) {
                                    news = true;
                                }
                            }

                            ready.entrySet().removeIf(enx -> ((String)enx.getKey()).startsWith(prefix) && (Integer)enx.getValue() <= ticks);
                            if (news) {
                                sync(p, t);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void command(ServerPlayer p, String line) {
        MinecraftServer server = p.server;
        String c = line.startsWith("/") ? line.substring(1) : line;
        server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withSuppressedOutput().withPermission(4), c);
    }

    private static MobEffect effect(String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        return rl == null ? null : Regs.MOB_EFFECT.get(rl);
    }

    private static void keepUp() {
        if (!trees().isEmpty() || !RULES_SET.isEmpty() || !POTIONS_SET.isEmpty()) {
            MinecraftServer server = Platform.get().server();
            if (server != null) {
                for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                    apply(p);
                    takeOffLocked(p);
                    Map<String, SkillTree> sent = TREE_SENT.get(p.getUUID());

                    for (Skills.Tree t : trees()) {
                        if (sent == null || sent.get(t.key()) != t.def) {
                            sync(p, t);
                        }
                    }
                }
            }
        }
    }

    public static void apply(ServerPlayer p) {
        Map<String, Float> stats = new HashMap<>();
        Map<String, Boolean> switches = new HashMap<>();
        Map<String, Integer> potions = new HashMap<>();

        for (Skills.Tree t : trees()) {
            SkillLogic.State s = state(p, t);
            SkillLogic.stats(t.def, s).forEach((rule, amount) -> stats.merge(rule, amount, Float::sum));

            for (SkillTree.Node n : t.def.nodes) {
                int rank = s.rank(n.id);
                keepPassive(p, t, n, rank > 0 && !s.off);

                for (SkillTree.Effect e : n.effects) {
                    if (e.kind.equals("unlock")) {
                        switches.merge(e.rule, rank > 0, Boolean::logicalOr);
                    }

                    if (e.kind.equals("potion") && rank > 0 && !e.potion.isBlank()) {
                        potions.merge(e.potion, Math.max(1, e.level) + rank - 1, Math::max);
                    }
                }
            }
        }

        Set<String> rulesNow = new HashSet<>();
        stats.forEach((name, amount) -> {
            PlayerRuleDefs.Rule rule = PlayerRuleDefs.get(name);
            if (rule != null && !rule.isSwitch()) {
                rulesNow.add(name);
                if (!PlayerRules.timed(p, name)) {
                    double value = PlayerRuleDefs.clamp(rule, rule.def() + amount.floatValue());
                    if (PlayerRules.number(p, name, Double.NaN) != value) {
                        PlayerRules.set(p, name, value, 0.0);
                    }
                }
            }
        });
        switches.forEach((name, learned) -> {
            PlayerRuleDefs.Rule rule = PlayerRuleDefs.get(name);
            if (rule != null && rule.isSwitch()) {
                rulesNow.add(name);
                double value = learned ? 1.0 : (rule.def() >= 1.0 ? 0.0 : rule.def());
                if (PlayerRules.number(p, name, Double.NaN) != value) {
                    PlayerRules.set(p, name, value, 0.0);
                }
            }
        });
        Set<String> had = RULES_SET.put(p.getUUID(), rulesNow);
        if (had != null) {
            for (String name : had) {
                if (!rulesNow.contains(name)) {
                    PlayerRules.reset(p, name);
                }
            }
        }

        Set<String> potionsNow = new HashSet<>(potions.keySet());
        potions.forEach((idx, level) -> {
            MobEffect effect = effect(idx);
            if (effect != null) {
                MobEffectInstance on = p.getEffect(effect);
                if (on == null || on.getAmplifier() != level - 1 || on.getDuration() < 220) {
                    p.addEffect(new MobEffectInstance(effect, 400, level - 1, true, false, true));
                }
            }
        });
        Set<String> hadPotions = POTIONS_SET.put(p.getUUID(), potionsNow);
        if (hadPotions != null) {
            for (String id : hadPotions) {
                MobEffect effect = effect(id);
                if (effect != null && !potionsNow.contains(id)) {
                    p.removeEffect(effect);
                }
            }
        }
    }

    public static boolean blocked(Player player, ItemStack stack) {
        if (player instanceof ServerPlayer p && !locks.isEmpty() && stack != null && !stack.isEmpty() && !p.isCreative() && !p.isSpectator()) {
            ResourceLocation key = Regs.ITEM.getKey(stack.getItem());
            List<Skills.Lock> by = key != null ? locks.get(key.toString()) : null;
            if (by == null) {
                return false;
            } else {
                for (Skills.Lock lock : by) {
                    SkillLogic.State s = state(p, lock.tree);
                    if (!s.off && s.rank(lock.node.id) > 0) {
                        return false;
                    }
                }

                for (Skills.Lock lockx : by) {
                    if (!state(p, lockx.tree).off) {
                        Integer said = LOCK_SAID.get(p.getUUID());
                        if (said == null || ticks - said > 40) {
                            LOCK_SAID.put(p.getUUID(), ticks);
                            p.displayClientMessage(Component.literal("Нужен навык: " + lockx.node.title), true);
                        }

                        return true;
                    }
                }

                return false;
            }
        } else {
            return false;
        }
    }

    private static void takeOffLocked(ServerPlayer p) {
        if (!locks.isEmpty() && !p.isCreative() && !p.isSpectator()) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == Type.ARMOR) {
                    ItemStack worn = p.getItemBySlot(slot);
                    if (blocked(p, worn)) {
                        p.setItemSlot(slot, ItemStack.EMPTY);
                        if (!p.addItem(worn)) {
                            p.drop(worn, false);
                        }
                    }
                }
            }
        }
    }

    public static void open(ServerPlayer p, Skills.Tree t, String menu) {
        OPEN.put(p.getUUID(), t.key());
        TREE_SENT.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(t.key(), t.def);
        Skills.View v = view(p, t);
        v.menu = menu == null ? "" : menu;
        SprauteNetwork.sendTo(p, new Packets.SkillsView(GSON.toJson(v), 1));
    }

    public static void close(ServerPlayer p) {
        OPEN.remove(p.getUUID());
        SprauteNetwork.sendTo(p, new Packets.SkillsView("", 2));
    }

    private static Skills.View view(ServerPlayer p, Skills.Tree t) {
        Skills.View v = new Skills.View();
        v.key = t.key();
        v.project = t.project;
        v.name = t.name;
        v.tree = t.def;
        v.state = state(p, t);

        for (SkillTree.Node n : t.def.nodes) {
            int left = n.ability.on ? cooldownLeft(p, t, n) : 0;
            if (left > 0) {
                v.cooldowns.put(n.id, left);
            }
        }

        return v;
    }

    private static void sync(ServerPlayer p, Skills.Tree t) {
        Skills.View v = view(p, t);
        Map<String, SkillTree> sent = TREE_SENT.computeIfAbsent(p.getUUID(), k -> new HashMap<>());
        if (sent.get(t.key()) == t.def) {
            v.tree = null;
        } else {
            sent.put(t.key(), t.def);
        }

        SprauteNetwork.sendTo(p, new Packets.SkillsView(GSON.toJson(v), 0));
    }

    private static void syncAll(ServerPlayer p) {
        for (Skills.Tree t : trees()) {
            sync(p, t);
        }
    }

    private record CodeOwner(Skills.Tree tree, SkillTree.Node node) {
    }

    private record Lock(Skills.Tree tree, SkillTree.Node node) {
    }

    private record Opener(Skills.Tree tree, String menu) {
    }

    private static final class Saved extends SavedData {
        Map<String, Map<String, SkillLogic.State>> players = new HashMap<>();

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putString("json", Skills.GSON.toJson(this.players));
            return tag;
        }

        static Skills.Saved load(CompoundTag tag) {
            Skills.Saved s = new Skills.Saved();

            try {
                Map<String, Map<String, SkillLogic.State>> read = Skills.GSON
                    .fromJson(tag.getString("json"), (new TypeToken<Map<String, Map<String, SkillLogic.State>>>() {}).getType());
                if (read != null) {
                    s.players = read;
                }
            } catch (RuntimeException var3) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read the players' skills", (Throwable)var3);
            }

            return s;
        }
    }

    public record Tree(String project, String name, SkillTree def) {
        public String key() {
            return this.project + "/" + this.name;
        }
    }

    public static final class View {
        public String key;
        public String project;
        public String name;
        public String menu = "";
        public SkillTree tree;
        public SkillLogic.State state;
        public Map<String, Integer> cooldowns = new HashMap<>();
    }
}
