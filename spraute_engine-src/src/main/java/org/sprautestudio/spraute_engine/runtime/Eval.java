package org.sprautestudio.spraute_engine.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.addon.AddonContext;
import org.sprautestudio.spraute_engine.addon.Addons;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.core.script.Characters;
import org.sprautestudio.spraute_engine.core.system.CameraState;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.core.system.WorldClock;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.custom.ItemApi;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.Parser;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.quest.Quests;
import org.sprautestudio.spraute_engine.recipe.RecipeApi;
import org.sprautestudio.spraute_engine.runtime.models.BoneWorld;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.shop.Shops;
import org.sprautestudio.spraute_engine.skills.BlockArea;
import org.sprautestudio.spraute_engine.skills.SkillLogic;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.zone.Zones;

public final class Eval {
    private static final Pattern INTERPOLATION = Pattern.compile("(?<!\\\\)\\{([^{}]*)}");
    private final SprauteScriptInstance script;
    private static final Set<String> SKILL_FIELDS = Set.of("level", "nextlevel", "xp", "xpnext", "maxlevel", "title", "points", "rank");

    Eval(SprauteScriptInstance script) {
        this.script = script;
    }

    public Object eval(Ast.Expr e) {
        if (e instanceof Ast.NumberLit n) {
            return n.value;
        } else if (e instanceof Ast.DurationLit d) {
            return (double)d.ticks;
        } else if (e instanceof Ast.StringLit s) {
            return this.interpolate(s.value);
        } else if (e instanceof Ast.BoolLit b) {
            return b.value;
        } else if (e instanceof Ast.Ident id) {
            return this.ident(id.name);
        } else if (e instanceof Ast.Unary u) {
            Object v = this.eval(u.operand);
            return u.op.equals("-") ? -num(v) : !truthy(v);
        } else if (e instanceof Ast.Binary b) {
            return this.binary(b);
        } else if (e instanceof Ast.Call c) {
            return this.call(c);
        } else if (e instanceof Ast.Tuple t) {
            List<Object> items = new ArrayList<>();

            for (Ast.Expr x : t.items) {
                items.add(this.eval(x));
            }

            return items;
        } else if (e instanceof Ast.ListLit l) {
            List<Object> items = new ArrayList<>();

            for (Ast.Expr x : l.items) {
                items.add(this.eval(x));
            }

            return items;
        } else if (e instanceof Ast.MapLit ml) {
            Map<String, Object> map = Values.map();

            for (int i = 0; i < ml.keys.size(); i++) {
                map.put(str(this.eval(ml.keys.get(i))), this.eval(ml.values.get(i)));
            }

            return map;
        } else if (e instanceof Ast.Index ix) {
            return Values.index(this.eval(ix.target), this.eval(ix.index));
        } else if (!(e instanceof Ast.Member m)) {
            throw new SprauteRuntimeException("Неподдерживаемое выражение");
        } else {
            Object target = this.ref(m.target);
            List<Object> args = null;
            if (m.args != null) {
                args = new ArrayList<>();

                for (Ast.Expr x : m.args) {
                    args.add(this.ref(x));
                }
            }

            if (target instanceof EntityRef er) {
                return args == null ? WorldApi.get(er, m.name) : WorldApi.call(er, m.name, args);
            } else if (target instanceof JavaBridge.JavaObj jo) {
                return args == null ? JavaBridge.get(jo, m.name) : JavaBridge.call(jo, m.name, args);
            } else {
                return args == null ? Values.property(target, m.name) : Values.method(target, m.name, args);
            }
        }
    }

    public Object ref(Ast.Expr e) {
        if (e instanceof Ast.Ident id) {
            if (id.name.equals("player") || id.name.equals("actor") && !this.script.vars.containsKey("actor")) {
                ServerPlayer p = id.name.equals("actor") ? this.actor() : SprauteRuntime.player();
                if (p == null) {
                    throw new SprauteRuntimeException("Игрока нет в мире");
                }

                return new EntityRef(p);
            }

            Character<?> ch = this.script.characters.get(id.name);
            if (ch != null) {
                Entity en = (Entity)this.script.optional(ch).orElse(null);
                if (en == null) {
                    throw new SprauteRuntimeException("Персонаж " + id.name + " сейчас не в мире (spawn " + id.name + " at ...)");
                }

                return new EntityRef(en);
            }

            if (!this.script.vars.containsKey(id.name) && this.script.locationDecls.containsKey(id.name)) {
                return WorldApi.list(this.script.location(id.name));
            }
        }

        return this.eval(e);
    }

    private List<Object> refsFrom(List<Ast.Expr> args, int from) {
        List<Object> out = new ArrayList<>();

        for (int i = from; i < args.size(); i++) {
            out.add(this.ref(args.get(i)));
        }

        return out;
    }

    private static List<Object> refs(List<ServerPlayer> players) {
        List<Object> list = new ArrayList<>();

        for (ServerPlayer p : players) {
            list.add(new EntityRef(p));
        }

        return list;
    }

    private List<ServerPlayer> playersAround(Ast.Expr target, double radius) {
        Entity self = this.ref(target) instanceof EntityRef er ? er.get() : null;
        Location at = this.position(target);
        List<ServerPlayer> out = new ArrayList<>();
        if (at == null) {
            return out;
        } else {
            for (ServerPlayer p : SprauteRuntime.players()) {
                if (p != self && (self == null || Mc.level(p) == Mc.level(self)) && p.position().distanceTo(at) <= radius) {
                    out.add(p);
                }
            }

            out.sort(Comparator.comparingDouble(px -> px.position().distanceTo(at)));
            return out;
        }
    }

    public ServerPlayer playerOf(Ast.Expr e) {
        Object who = this.ref(e);
        ServerPlayer p = (who instanceof EntityRef r ? r.get() : null) instanceof ServerPlayer sp
            ? sp
            : (who instanceof String name ? this.playerNamed(name) : null);
        if (p == null) {
            throw new SprauteRuntimeException("Нужен игрок в сети, а не " + who);
        } else {
            return p;
        }
    }

    public ServerPlayer playerNamed(String name) {
        MinecraftServer server = SprauteRuntime.server();
        return server == null ? null : server.getPlayerList().getPlayerByName(name);
    }

    public ServerPlayer actor() {
        if (this.script.event.get("__player") instanceof ServerPlayer p) {
            return p.hasDisconnected() ? null : p;
        } else {
            return SprauteRuntime.player();
        }
    }

    private Object ident(String name) {
        if (name.equals("player")) {
            ServerPlayer p = SprauteRuntime.player();
            return p != null ? p.getGameProfile().getName() : "";
        } else if (this.script.vars.containsKey(name)) {
            return this.script.vars.get(name);
        } else if (name.equals("actor")) {
            ServerPlayer p = this.actor();
            return p != null ? p.getGameProfile().getName() : "";
        } else if (this.script.characters.containsKey(name)) {
            return this.script.characters.get(name).getName();
        } else if (this.script.locationDecls.containsKey(name)) {
            return this.script.location(name).toString();
        } else {
            throw new SprauteRuntimeException("Неизвестное имя '" + name + "'");
        }
    }

    private Object binary(Ast.Binary b) {
        switch (b.op) {
            case "and":
                return truthy(this.eval(b.left)) && truthy(this.eval(b.right));
            case "or":
                return truthy(this.eval(b.left)) || truthy(this.eval(b.right));
            default: {
                Object l = this.eval(b.left);
                Object r = this.eval(b.right);
                switch (b.op) {
                    case "+":
                        if (l instanceof String || r instanceof String) {
                            return str(l) + str(r);
                        }

                        if (l instanceof List<?> a && r instanceof List<?> c) {
                            List<Object> out = new ArrayList<>(a);
                            out.addAll(c);
                            return out;
                        }

                        return num(l) + num(r);
                    case "-":
                        return num(l) - num(r);
                    case "*":
                        return num(l) * num(r);
                    case "/": {
                        double d = num(r);
                        if (d == 0.0) {
                            throw new SprauteRuntimeException("Деление на ноль");
                        }

                        return num(l) / d;
                    }
                    case "%": {
                        double d = num(r);
                        if (d == 0.0) {
                            throw new SprauteRuntimeException("Деление на ноль");
                        }

                        return num(l) % d;
                    }
                    case "==":
                        return equal(l, r);
                    case "!=":
                        return !equal(l, r);
                    case "<":
                        return compare(l, r) < 0;
                    case "<=":
                        return compare(l, r) <= 0;
                    case ">":
                        return compare(l, r) > 0;
                    case ">=":
                        return compare(l, r) >= 0;
                    default:
                        throw new SprauteRuntimeException("Неизвестный оператор " + b.op);
                }
            }
        }
    }

    private Object refOrNull(Ast.Expr e) {
        try {
            return this.ref(e);
        } catch (SprauteRuntimeException var3) {
            return null;
        }
    }

    private Object call(Ast.Call c) {
        List<Ast.Expr> a = c.args;
        String var3 = c.name;
        switch (var3) {
            case "random":
                int n = (int)num(this.eval(a.get(0)));
                return n <= 0 ? 0.0 : this.script.getRandom().nextInt(n);
            case "chance":
                return this.script.getRandom().nextDouble() * 100.0 < num(this.eval(a.get(0)));
            case "flag":
                return this.script.getFlags().has(str(this.eval(a.get(0))));
            case "consumeflag":
                return this.script.getFlags().consume(str(this.eval(a.get(0))));
            case "js":
                return JsEngine.run(this.script, str(this.eval(a.get(0))));
            case "nbt":
                return Bridges.nbt(this.ref(a.get(0)), a.size() > 1 ? str(this.eval(a.get(1))) : "");
            case "attr":
                return Bridges.attr(this.ref(a.get(0)), str(this.eval(a.get(1))));
            case "score":
                return Bridges.score(this.ref(a.get(0)), str(this.eval(a.get(1))));
            case "cmdresult":
                return Bridges.run(str(this.eval(a.get(0)))).output();
            case "cmdvalue":
                return (double)Bridges.run(str(this.eval(a.get(0)))).value();
            case "hasmod":
                return Bridges.hasMod(str(this.eval(a.get(0))));
            case "loader":
                return Platform.get().loader();
            case "isforge":
                return Platform.get().loader().equals("forge");
            case "isfabric":
                return Platform.get().loader().equals("fabric");
            case "hasclass":
                return JavaBridge.findClass(str(this.eval(a.get(0)))) != null;
            case "java": {
                Object v = this.ref(a.get(0));
                return v instanceof String s ? JavaBridge.type(s) : JavaBridge.wrap(v);
            }
            case "jnew":
                return JavaBridge.create(str(this.eval(a.get(0))), this.refsFrom(a, 1));
            case "jcall":
                return JavaBridge.call(JavaBridge.target(this.ref(a.get(0))), str(this.eval(a.get(1))), this.refsFrom(a, 2));
            case "jget":
                return JavaBridge.get(JavaBridge.target(this.ref(a.get(0))), str(this.eval(a.get(1))));
            case "jset":
                return JavaBridge.set(JavaBridge.target(this.ref(a.get(0))), str(this.eval(a.get(1))), this.ref(a.get(2)));
            case "jclass":
                return JavaBridge.className(this.ref(a.get(0)));
            case "jmembers":
                return JavaBridge.members(this.ref(a.get(0)));
            case "walking": {
                return this.ref(a.get(0)) instanceof EntityRef r && Walkers.walking(r.get());
            }
            case "global":
                return Globals.get(str(this.eval(a.get(0))), a.size() > 1 ? this.eval(a.get(1)) : 0.0);
            case "hasglobal":
                return Globals.has(str(this.eval(a.get(0))));
            case "flagvalue": {
                String v = this.script.getFlags().get(str(this.eval(a.get(0))));
                return v == null ? "" : v;
            }
            case "answer":
                return (double)this.script.lastAnswer;
            case "near": {
                ServerPlayer p = SprauteRuntime.player();
                Location locxx = this.position(a.get(0));
                return p != null && locxx != null && p.position().distanceTo(locxx) <= num(this.eval(a.get(1)));
            }
            case "dist": {
                ServerPlayer p = SprauteRuntime.player();
                Location locx = this.position(a.get(0));
                return p != null && locx != null ? p.position().distanceTo(locx) : -1.0;
            }
            case "health": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? p.getHealth() : 0.0;
            }
            case "daytime": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? Mc.level(p).getDayTime() % 24000L : 0.0;
            }
            case "button":
                return this.script.lastButton;
            case "input":
                return this.script.guiInputs.getOrDefault(str(this.eval(a.get(0))), "");
            case "skillpower":
                return (double)Skills.powerOf(this.script.deployed.id, SprauteRuntime.player());
            case "evententity":
                return this.script.event.get("ref");
            case "velocity": {
                return this.ref(a.get(0)) instanceof EntityRef er && er.get() != null ? WorldApi.list(er.get().getDeltaMovement()) : WorldApi.list(Vec3.ZERO);
            }
            case "onground": {
                return this.ref(a.get(0)) instanceof EntityRef er && er.get() != null && Mc.onGround(er.get());
            }
            case "blockarea": {
                Vec3 at = WorldApi.vec(this.ref(a.get(0)));
                Vec3 look = this.ref(a.get(1)) instanceof EntityRef er && er.get() != null ? er.get().getLookAngle() : new Vec3(0.0, 0.0, 1.0);
                List<Object> out = new ArrayList<>();

                for (int[] b : BlockArea.of(
                    (int)Math.floor(at.x),
                    (int)Math.floor(at.y),
                    (int)Math.floor(at.z),
                    look.x,
                    look.y,
                    look.z,
                    (int)num(this.eval(a.get(2))),
                    (int)num(this.eval(a.get(3))),
                    a.size() > 4 ? (int)num(this.eval(a.get(4))) : 1
                )) {
                    List<Object> point = new ArrayList<>();
                    point.add((double)b[0]);
                    point.add((double)b[1]);
                    point.add((double)b[2]);
                    out.add(point);
                }

                return out;
            }
            case "skilllevel":
            case "skillrank":
            case "skillpoints":
            case "skillxp":
            case "skillxpnext":
            case "skillmaxlevel":
            case "skillknows":
            case "skillcan": {
                ServerPlayer p = SprauteRuntime.player();
                Skills.Tree tree = Skills.tree(this.script.deployed.project, str(this.eval(a.get(0))));
                if (!c.name.equals("skillknows") && !c.name.equals("skillcan")) {
                    if (p != null && tree != null) {
                        SkillLogic.State stx = Skills.state(p, tree);
                        if (c.name.equals("skillxp")) {
                            return (double)stx.xp;
                        }

                        if (c.name.equals("skillxpnext")) {
                            return (double)SkillLogic.xpToNext(tree.def(), stx.level);
                        }

                        if (c.name.equals("skillmaxlevel")) {
                            return (double)tree.def().maxLevel;
                        }

                        if (c.name.equals("skilllevel")) {
                            return (double)stx.level;
                        }

                        if (c.name.equals("skillrank")) {
                            return (double)stx.rank(a.size() > 1 ? str(this.eval(a.get(1))) : "");
                        }

                        return (double)stx.points(a.size() > 1 ? str(this.eval(a.get(1))) : tree.def().currencies.get(0).id);
                    }

                    return 0.0;
                } else {
                    if (p != null && tree != null && a.size() >= 2) {
                        SkillLogic.State known = Skills.state(p, tree);
                        SkillTree.Node node = tree.def().node(str(this.eval(a.get(1))));
                        if (node == null) {
                            return false;
                        }

                        return c.name.equals("skillknows") ? known.rank(node.id) > 0 : SkillLogic.refusal(tree.def(), known, node) == null;
                    }

                    return false;
                }
            }
            case "guizone":
                return this.script.guiZones.getOrDefault(str(this.eval(a.get(0))), "");
            case "guiopen":
                return this.script.openGui != null;
            case "event": {
                Object v = this.script.event.get(str(this.eval(a.get(0))));
                return v == null ? "" : v;
            }
            case "cameraactive":
                return CameraState.active(SprauteRuntime.player());
            case "camerapos":
                return WorldApi.list(CameraState.position(SprauteRuntime.player()));
            case "timeday":
                MinecraftServer server = SprauteRuntime.server();
                return server == null ? 0.0 : WorldClock.day(server);
            case "incutscene":
                return !this.script.cutscene.isEmpty();
            case "coins":
                try {
                    return (double)Coins.get(SprauteRuntime.player());
                } catch (Throwable var13) {
                    return 0.0;
                }
            case "slotitem":
                return GuiSlots.id(this.script.guiSlots.get(str(this.eval(a.get(0)))));
            case "slotcount":
                ItemStack st = this.script.guiSlots.get(str(this.eval(a.get(0))));
                return st == null ? 0.0 : st.getCount();
            case "lastslot":
                return this.script.lastSlot;
            case "system":
                return EngineServer.enabled(str(this.eval(a.get(0))));
            case "round":
                return (double)Math.round(num(this.eval(a.get(0))));
            case "floor":
                return Math.floor(num(this.eval(a.get(0))));
            case "randint":
                int lo = (int)Math.round(num(this.eval(a.get(0))));
                int hi = (int)Math.round(num(this.eval(a.get(1))));
                if (hi < lo) {
                    int tx = lo;
                    lo = hi;
                    hi = tx;
                }

                return (double)(lo + this.script.getRandom().nextInt(hi - lo + 1));
            case "len":
                return Values.property(this.eval(a.get(0)), "len");
            case "players": {
                List<Object> list = new ArrayList<>();

                for (ServerPlayer p : SprauteRuntime.audience()) {
                    list.add(new EntityRef(p));
                }

                return list;
            }
            case "playercount":
                return (double)SprauteRuntime.audience().size();
            case "playerbyname": {
                ServerPlayer p = this.playerNamed(str(this.eval(a.get(0))));
                return p != null ? new EntityRef(p) : "";
            }
            case "online":
                return this.playerNamed(str(this.eval(a.get(0)))) != null;
            case "allplayers":
                return refs(SprauteRuntime.players());
            case "otherplayers":
                ServerPlayer me = SprauteRuntime.player();
                List<ServerPlayer> others = new ArrayList<>(SprauteRuntime.players());
                others.remove(me);
                return refs(others);
            case "randomplayer":
                List<ServerPlayer> all = SprauteRuntime.players();
                return all.isEmpty() ? "" : new EntityRef(all.get(this.script.getRandom().nextInt(all.size())));
            case "nearestplayer":
                List<ServerPlayer> near = this.playersAround(a.get(0), Double.MAX_VALUE);
                return near.isEmpty() ? "" : new EntityRef(near.get(0));
            case "playersnear":
                return refs(this.playersAround(a.get(0), num(this.eval(a.get(1)))));
            case "playersinzone": {
                String zone = str(this.eval(a.get(0)));
                List<ServerPlayer> in = new ArrayList<>();

                for (ServerPlayer p : SprauteRuntime.players()) {
                    if (Zones.playerIn(p, this.script.deployed.project, zone)) {
                        in.add(p);
                    }
                }

                return refs(in);
            }
            case "owner": {
                ServerPlayer p = this.script.protagonist();
                return p != null ? new EntityRef(p) : "";
            }
            case "isowner": {
                ServerPlayer p = a.isEmpty() ? SprauteRuntime.player() : this.playerOf(a.get(0));
                ServerPlayer main = this.script.protagonist();
                return p != null && main != null && p.getUUID().equals(main.getUUID());
            }
            case "isafk": {
                ServerPlayer p = a.isEmpty() ? SprauteRuntime.player() : this.playerOf(a.get(0));
                return p != null && GameEvents.isAfk(p);
            }
            case "pvar": {
                ServerPlayer p = a.size() > 1 ? this.playerOf(a.get(1)) : SprauteRuntime.player();
                Object fallback = a.size() > 2 ? this.eval(a.get(2)) : 0.0;
                return p != null ? PlayerVars.get(p, str(this.eval(a.get(0))).trim(), fallback) : fallback;
            }
            case "haspvar": {
                ServerPlayer p = a.size() > 1 ? this.playerOf(a.get(1)) : SprauteRuntime.player();
                return p != null && PlayerVars.has(p, str(this.eval(a.get(0))).trim());
            }
            case "teamof": {
                ServerPlayer p = a.isEmpty() ? SprauteRuntime.player() : this.playerOf(a.get(0));
                return p != null ? PlayerTeams.teamOf(p) : "";
            }
            case "teamplayers":
                return refs(PlayerTeams.online(str(this.eval(a.get(0))).trim()));
            case "sameteam":
                String first = PlayerTeams.teamOf(this.playerOf(a.get(0)));
                return !first.isEmpty() && first.equals(PlayerTeams.teamOf(this.playerOf(a.get(1))));
            case "hastag": {
                ServerPlayer p = a.size() > 1 ? this.playerOf(a.get(1)) : SprauteRuntime.player();
                return p != null && p.getTags().contains(str(this.eval(a.get(0))).trim());
            }
            case "tags": {
                ServerPlayer p = a.isEmpty() ? SprauteRuntime.player() : this.playerOf(a.get(0));
                return p != null ? new ArrayList<>(new TreeSet<>(p.getTags())) : new ArrayList();
            }
            case "playerswithtag": {
                String tag = str(this.eval(a.get(0))).trim();
                List<Object> list = new ArrayList<>();

                for (ServerPlayer p : SprauteRuntime.players()) {
                    if (p.getTags().contains(tag)) {
                        list.add(new EntityRef(p));
                    }
                }

                return list;
            }
            case "day": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? Math.floorDiv(Mc.level(p).getDayTime(), 24000L) : 0.0;
            }
            case "bought":
                return (double)Shops.bought(this.script.deployed.project, str(this.eval(a.get(0))), str(this.eval(a.get(1))));
            case "stock":
                return (double)Shops.stock(this.script.deployed.project, str(this.eval(a.get(0))), str(this.eval(a.get(1))));
            case "hitbox": {
                return this.ref(a.get(0)) instanceof EntityRef r ? Hitboxes.describe(Hitboxes.flags(r.require())) : "";
            }
            case "fighthp": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r ? Fights.hp(r.require()) : 0.0;
            }
            case "fighting": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && Fights.fighting(r.require());
            }
            case "defeated": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && Fights.defeated(r.require());
            }
            case "cooldown": {
                String key = str(this.eval(a.get(0)));
                int now = Mechanics.now();
                Integer until = this.script.cooldowns.get(key);
                if (until != null && until > now) {
                    return false;
                }

                this.script.cooldowns.put(key, now + (int)Math.round(num(this.eval(a.get(1))) * 20.0));
                return true;
            }
            case "cooldownleft": {
                Integer until = this.script.cooldowns.get(str(this.eval(a.get(0))));
                return until == null ? 0.0 : Math.max(0.0, (until - Mechanics.now()) / 20.0);
            }
            case "ticks":
                return (double)Mechanics.now();
            case "infront": {
                if (this.ref(a.get(0)) instanceof EntityRef r) {
                    Entity var154 = r.require();
                    double dx = a.size() > 1 ? num(this.eval(a.get(1))) : 3.0;
                    Vec3 look = var154.getViewVector(1.0F);
                    Vec3 flat = new Vec3(look.x, 0.0, look.z);
                    flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
                    return WorldApi.list(WorldApi.ground(var154.position().add(flat.scale(dx))));
                }

                throw new SprauteRuntimeException("infront: нужен игрок или персонаж");
            }
            case "hp":
            case "maxhp":
            case "hpfrac": {
                Entity en = this.ref(a.get(0)) instanceof EntityRef r ? r.get() : null;
                return c.name.equals("hp") ? BossMoves.hp(en) : (c.name.equals("maxhp") ? BossMoves.maxHp(en) : BossMoves.frac(en));
            }
            case "parts":
            case "lastpart":
            case "parton":
            case "partpos":
                SprauteModelEntity mex = (this.ref(a.get(0)) instanceof EntityRef rx ? rx.get() : null) instanceof SprauteModelEntity mx ? mx : null;
                String var169 = c.name;
                switch (var169) {
                    case "parts":
                        return mex == null ? new ArrayList() : new ArrayList<>(mex.partNames());
                    case "lastpart":
                        return mex == null ? "" : mex.lastPart();
                    case "parton":
                        return mex != null && mex.partEnabled(str(this.eval(a.get(1))));
                    default:
                        String nx = str(this.eval(a.get(1)));
                        AABB box = mex != null ? mex.partBox(nx) : null;
                        if (box == null) {
                            throw new SprauteRuntimeException("partpos: нет части «" + nx + "»");
                        }

                        return WorldApi.list(box.getCenter());
                }
            case "moving": {
                return this.ref(a.get(0)) instanceof EntityRef r && BossMoves.moving(r.get());
            }
            case "attacking": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && r.get() != null && AttackBoxes.active(r.get());
            }
            case "attackhits": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && r.get() != null ? AttackBoxes.hits(r.get()) : 0.0;
            }
            case "beaming": {
                return this.ref(a.get(0)) instanceof EntityRef r && r.get() != null && BossMoves.beaming(r.get());
            }
            case "behind": {
                if (this.ref(a.get(0)) instanceof EntityRef r) {
                    Entity var150 = r.require();
                    double dx = a.size() > 1 ? num(this.eval(a.get(1))) : 3.0;
                    Vec3 look = var150.getViewVector(1.0F);
                    Vec3 flat = new Vec3(look.x, 0.0, look.z);
                    flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
                    return WorldApi.list(WorldApi.ground(var150.position().subtract(flat.scale(dx))));
                }

                throw new SprauteRuntimeException("behind: нужен игрок или персонаж");
            }
            case "around": {
                Vec3 ctr = WorldApi.vec(this.ref(a.get(0)));
                double r = num(this.eval(a.get(1)));
                double ang = Math.toRadians(a.size() > 2 ? num(this.eval(a.get(2))) : 0.0);
                return WorldApi.list(WorldApi.ground(ctr.add(Math.cos(ang) * r, 0.0, Math.sin(ang) * r)));
            }
            case "randaround": {
                Vec3 ctr = WorldApi.vec(this.ref(a.get(0)));
                double r = num(this.eval(a.get(1)));
                double ang = this.script.getRandom().nextDouble() * Math.PI * 2.0;
                double rr = r * Math.sqrt(this.script.getRandom().nextDouble());
                return WorldApi.list(WorldApi.ground(ctr.add(Math.cos(ang) * rr, 0.0, Math.sin(ang) * rr)));
            }
            case "dirto":
                Vec3 f = WorldApi.vec(this.ref(a.get(0)));
                Vec3 t = WorldApi.vec(this.ref(a.get(1)));
                Vec3 d = new Vec3(t.x - f.x, 0.0, t.z - f.z);
                d = d.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : d.normalize();
                return WorldApi.list(d);
            case "ground":
                return WorldApi.list(WorldApi.ground(WorldApi.vec(this.ref(a.get(0)))));
            case "aimpoint": {
                if (this.ref(a.get(0)) instanceof EntityRef r) {
                    return WorldApi.list(Mechanics.aimPoint(r.require(), a.size() > 1 ? num(this.eval(a.get(1))) : 48.0));
                }

                throw new SprauteRuntimeException("aimpoint: нужен игрок или персонаж");
            }
            case "raycast":
                Object from = this.ref(a.get(0));
                Object to = a.size() > 1 ? this.ref(a.get(1)) : null;
                return Raycast.raycast(from, to, a.size() > 2 ? num(this.eval(a.get(2))) : 64.0);
            case "bone": {
                if (this.ref(a.get(0)) instanceof EntityRef r) {
                    Vec3 off = a.size() > 2 ? WorldApi.vec(this.ref(a.get(2))) : Vec3.ZERO;
                    return WorldApi.list(BoneWorld.position(r.require(), str(this.eval(a.get(1))), off.x, off.y, off.z));
                }

                throw new SprauteRuntimeException("bone: нужен персонаж с моделью");
            }
            case "bonedir": {
                if (this.ref(a.get(0)) instanceof EntityRef r) {
                    return WorldApi.list(BoneWorld.forward(r.require(), str(this.eval(a.get(1)))));
                }

                throw new SprauteRuntimeException("bonedir: нужен персонаж с моделью");
            }
            case "bones": {
                if (this.ref(a.get(0)) instanceof EntityRef r) {
                    return new ArrayList<>(BoneWorld.names(r.require()));
                }

                throw new SprauteRuntimeException("bones: нужен персонаж с моделью");
            }
            case "lookbone":
                return Raycast.lookBone(this.ref(a.get(0)), a.size() > 1 ? num(this.eval(a.get(1))) : 64.0);
            case "lookblock":
                return Raycast.lookBlock(this.ref(a.get(0)), a.size() > 1 ? num(this.eval(a.get(1))) : 64.0);
            case "lookentity":
                return Raycast.lookEntity(this.ref(a.get(0)), a.size() > 1 ? num(this.eval(a.get(1))) : 64.0);
            case "cansee":
                return Raycast.canSee(this.ref(a.get(0)), this.ref(a.get(1)));
            case "lookingat":
                return Raycast.looksAt(this.ref(a.get(0)), this.ref(a.get(1)), a.size() > 2 ? num(this.eval(a.get(2))) : 64.0);
            case "inzone":
                String zn = str(this.eval(a.get(0)));
                if (a.size() < 2) {
                    return Zones.playerIn(SprauteRuntime.player(), this.script.deployed.project, zn);
                }

                return Zones.contains(this.script.deployed.project, zn, WorldApi.vec(this.ref(a.get(1))));
            case "zonepoint":
                return WorldApi.list(Zones.randomPoint(this.script.deployed.project, str(this.eval(a.get(0)))));
            case "zonecenter":
                return WorldApi.list(Zones.require(this.script.deployed.project, str(this.eval(a.get(0)))).center());
            case "zones":
                return new ArrayList<>(Zones.zonesOf(SprauteRuntime.player(), this.script.deployed.project));
            case "queststate":
                return Quests.state(SprauteRuntime.player(), this.script.deployed.project, str(this.eval(a.get(0))));
            case "questprogress":
                return (double)Quests.progressOf(SprauteRuntime.player(), this.script.deployed.project, str(this.eval(a.get(0))), str(this.eval(a.get(1))));
            case "summon":
                return WorldApi.summon(str(this.eval(a.get(0))), this.ref(a.get(1)));
            case "entities":
                return WorldApi.entities(str(this.eval(a.get(0))), this.ref(a.get(1)), a.size() > 2 ? num(this.eval(a.get(2))) : 16.0);
            case "nearest":
                return WorldApi.nearest(str(this.eval(a.get(0))), this.ref(a.get(1)), a.size() > 2 ? num(this.eval(a.get(2))) : 32.0);
            case "copies":
            case "copycount":
                if (a.get(0) instanceof Ast.Ident who && this.script.characters.containsKey(who.name)) {
                    List<Entity> bodies = this.script.copiesOf(who.name);
                    if (c.name.equals("copycount")) {
                        return (double)bodies.size();
                    }

                    List<Object> refs = new ArrayList<>();

                    for (Entity body : bodies) {
                        refs.add(new EntityRef(body));
                    }

                    return refs;
                }

                throw new SprauteRuntimeException(c.name + "(): нужен персонаж, объявленный через character");
            case "alive": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && r.alive();
            }
            case "block":
                return WorldApi.block(this.ref(a.get(0)));
            case "blockprop":
                return WorldApi.blockProp(this.ref(a.get(0)), str(this.eval(a.get(1))));
            case "inbox":
                return WorldApi.inBox(this.ref(a.get(0)), this.ref(a.get(1)), this.ref(a.get(2)));
            case "range": {
                double x = num(this.eval(a.get(0)));
                if (a.size() == 1) {
                    return Values.range(0.0, x, 1.0);
                }

                return Values.range(x, num(this.eval(a.get(1))), a.size() > 2 ? num(this.eval(a.get(2))) : 1.0);
            }
            case "list": {
                List<Object> out = new ArrayList<>();

                for (Ast.Expr x : a) {
                    out.add(this.eval(x));
                }

                return out;
            }
            case "has":
                return Values.method(this.eval(a.get(0)), "has", List.of(this.eval(a.get(1))));
            case "indexof":
                return Values.method(this.eval(a.get(0)), "indexOf", List.of(this.eval(a.get(1))));
            case "keys":
                return Values.property(this.eval(a.get(0)), "keys");
            case "values":
                return Values.property(this.eval(a.get(0)), "values");
            case "sum":
                return Values.method(this.eval(a.get(0)), "sum", List.of());
            case "join":
                return Values.join(Values.iterable(this.eval(a.get(0))), a.size() > 1 ? str(this.eval(a.get(1))) : ", ");
            case "split":
                return Values.split(str(this.eval(a.get(0))), a.size() > 1 ? str(this.eval(a.get(1))) : " ");
            case "sorted": {
                List<Object> out = Values.iterable(this.eval(a.get(0)));
                out.sort(Values::compare);
                return out;
            }
            case "pick":
                List<Object> l = Values.iterable(this.eval(a.get(0)));
                return l.isEmpty() ? "" : l.get(this.script.getRandom().nextInt(l.size()));
            case "vadd":
                return Values.vecOp(this.ref(a.get(0)), this.ref(a.get(1)), '+');
            case "vsub":
                return Values.vecOp(this.ref(a.get(0)), this.ref(a.get(1)), '-');
            case "vmul":
                return Values.vecOp(this.ref(a.get(0)), this.eval(a.get(1)), '*');
            case "vlen":
                return Values.length(this.ref(a.get(0)));
            case "vdist":
                return Values.length(Values.vecOp(this.ref(a.get(0)), this.ref(a.get(1)), '-'));
            case "vnorm": {
                Object v = this.ref(a.get(0));
                double len = Values.length(v);
                return len == 0.0 ? v : Values.vecOp(v, 1.0 / len, '*');
            }
            case "spawn":
            case "tp": {
                Character<?> ch = this.characterArg(a.get(0), c.name);
                Location loc = this.position(a.get(1));
                if (loc == null) {
                    throw new SprauteRuntimeException(c.name + "(): точка не найдена");
                }

                Characters.place(this.script, ch, loc);
                if (c.name.equals("spawn")) {
                    this.afterSpawn(((Ast.Ident)a.get(0)).name, ch);
                }

                return true;
            }
            case "despawn": {
                Character<?> ch = this.characterArg(a.get(0), c.name);
                Characters.remove(this.script, ch);
                return true;
            }
            case "give":
                return WorldApi.give(str(this.eval(a.get(0))), a.size() > 1 ? (int)num(this.eval(a.get(1))) : 1);
            case "take":
                return WorldApi.take(str(this.eval(a.get(0))), a.size() > 1 ? (int)num(this.eval(a.get(1))) : 1);
            case "teleport": {
                ServerPlayer p = SprauteRuntime.player();
                Location loc = this.position(a.get(0));
                if (p != null && loc != null) {
                    p.teleportTo(loc.x, loc.y, loc.z);
                    return true;
                }

                return false;
            }
            case "effect": {
                ServerPlayer p = SprauteRuntime.player();
                if (p == null) {
                    return false;
                }

                List<Object> args = new ArrayList<>();

                for (Ast.Expr x : a) {
                    args.add(this.eval(x));
                }

                WorldApi.call(new EntityRef(p), "effect", args);
                return true;
            }
            case "setblock": {
                Object at = this.ref(a.get(0));
                String was = WorldApi.block(at);
                WorldApi.setBlock(at, str(this.eval(a.get(1))));
                return was;
            }
            case "explode":
                Mechanics.explode(
                    WorldApi.level(), WorldApi.vec(this.ref(a.get(0))), (float)(a.size() > 1 ? num(this.eval(a.get(1))) : 3.0), false, false, null
                );
                return true;
            case "lightning":
                Mechanics.lightning(WorldApi.level(), WorldApi.vec(this.ref(a.get(0))), false);
                return true;
            case "message": {
                Component msg = Component.literal(str(this.eval(a.get(0))));

                for (ServerPlayer p : SprauteRuntime.audience()) {
                    p.sendSystemMessage(msg);
                }

                return true;
            }
            case "actionbar":
                Mechanics.actionbar(str(this.eval(a.get(0))));
                return true;
            case "command":
                return Bridges.run(str(this.eval(a.get(0)))).output();
            case "hasrecipe":
                return RecipeApi.exists(str(this.eval(a.get(0))));
            case "recipeknown":
                return RecipeApi.known(SprauteRuntime.player(), str(this.eval(a.get(0))));
            case "cancraft":
                return RecipeApi.canCraft(SprauteRuntime.player(), str(this.eval(a.get(0))));
            case "recipes":
                return RecipeApi.all();
            case "recipesfor":
                return RecipeApi.making(str(this.eval(a.get(0))));
            case "recipesusing":
                return RecipeApi.using(str(this.eval(a.get(0))));
            case "recipeoutput":
                return RecipeApi.output(str(this.eval(a.get(0))));
            case "recipeoutputcount":
                return RecipeApi.outputCount(str(this.eval(a.get(0))));
            case "recipetype":
                return RecipeApi.type(str(this.eval(a.get(0))));
            case "recipeingredients":
                return RecipeApi.ingredients(str(this.eval(a.get(0))));
            case "customitems":
                return ItemApi.customItems();
            case "iscustomitem":
                return ItemApi.isCustom(str(this.eval(a.get(0))));
            case "itemname":
                return ItemApi.name(str(this.eval(a.get(0))));
            case "itemkind":
                return ItemApi.kind(str(this.eval(a.get(0))));
            case "itemmaxstack":
                return ItemApi.maxStack(str(this.eval(a.get(0))));
            case "itemdurability":
                return ItemApi.durability(str(this.eval(a.get(0))));
            case "itemoncooldown":
                return ItemApi.onCooldown(SprauteRuntime.player(), str(this.eval(a.get(0))));
            case "helddamage":
                return ItemApi.heldDamage(SprauteRuntime.player());
            case "heldleft":
                return ItemApi.heldLeft(SprauteRuntime.player());
            case "itemcount":
                return WorldApi.itemCount(str(this.eval(a.get(0))));
            case "hasitem":
                return WorldApi.itemCount(str(this.eval(a.get(0)))) > 0.0;
            case "helditem":
                return WorldApi.heldItem(this.ref(a.get(0)));
            case "haseffect":
                return WorldApi.effect(this.ref(a.get(0)), str(this.eval(a.get(1)))) != null;
            case "effectlevel": {
                MobEffectInstance m = WorldApi.effect(this.ref(a.get(0)), str(this.eval(a.get(1))));
                return m == null ? 0.0 : m.getAmplifier() + 1;
            }
            case "effecttime": {
                MobEffectInstance m = WorldApi.effect(this.ref(a.get(0)), str(this.eval(a.get(1))));
                return m == null ? 0.0 : (Mc.infinite(m) ? 999999.0 : m.getDuration() / 20.0);
            }
            case "xplevel": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? p.experienceLevel : 0.0;
            }
            case "food": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? p.getFoodData().getFoodLevel() : 0.0;
            }
            case "gamemode": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? p.gameMode.getGameModeForPlayer().getName() : "";
            }
            case "dimension": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && r.alive() ? Mc.level(r.require()).dimension().location().toString() : "";
            }
            case "weather":
                return WorldApi.weather();
            case "biome":
                return WorldApi.biome(this.ref(a.get(0)));
            case "light":
                return WorldApi.light(this.ref(a.get(0)));
            case "sky":
                return WorldApi.sky(this.ref(a.get(0)));
            case "posof":
                return WorldApi.list(WorldApi.vec(this.ref(a.get(0))));
            case "charname": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && r.alive() ? r.require().getName().getString() : "";
            }
            case "targetof": {
                return this.refOrNull(a.get(0)) instanceof EntityRef r && r.alive() ? WorldApi.get(r, "target") : "";
            }
            case "contains":
                return str(this.eval(a.get(0))).toLowerCase().contains(str(this.eval(a.get(1))).toLowerCase());
            case "upper":
                return str(this.eval(a.get(0))).toUpperCase();
            case "lower":
                return str(this.eval(a.get(0))).toLowerCase();
            case "num":
                try {
                    return Double.parseDouble(str(this.eval(a.get(0))).trim().replace(',', '.'));
                } catch (NumberFormatException var12) {
                    return 0.0;
                }
            case "running":
                return truthy(this.script.vars.get(Emitter.runningFlag(str(this.eval(a.get(0))))));
            case "active":
                return this.script.env.anyInstance((int)num(this.eval(a.get(0)))) != null;
            case "playername": {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? p.getGameProfile().getName() : "";
            }
            case "int":
                return Math.floor(num(this.eval(a.get(0))));
            case "str":
                return str(this.eval(a.get(0)));
            case "min":
                if (a.size() == 1) {
                    return Values.method(this.eval(a.get(0)), "min", List.of());
                }

                return Math.min(num(this.eval(a.get(0))), num(this.eval(a.get(1))));
            case "max":
                if (a.size() == 1) {
                    return Values.method(this.eval(a.get(0)), "max", List.of());
                }

                return Math.max(num(this.eval(a.get(0))), num(this.eval(a.get(1))));
            case "abs":
                return Math.abs(num(this.eval(a.get(0))));
            default:
                AddonContext.StoryFunction addon = Addons.function(c.name);
                if (addon == null) {
                    throw new SprauteRuntimeException("Неизвестная функция " + c.name);
                } else {
                    List<Object> values = new ArrayList<>();

                    for (Ast.Expr arg : a) {
                        values.add(this.eval(arg));
                    }

                    try {
                        return addon.call(values);
                    } catch (SprauteRuntimeException var14) {
                        throw var14;
                    } catch (RuntimeException var15) {
                        SprauteEngine.LOGGER.error("Spraute: an addon's function {} failed", c.name, var15);
                        throw new SprauteRuntimeException(c.name + ": ошибка в аддоне: " + var15.getMessage());
                    }
                }
        }
    }

    private Character<?> characterArg(Ast.Expr e, String fn) {
        Character<?> ch = e instanceof Ast.Ident id ? this.script.characters.get(id.name) : null;
        if (ch == null) {
            throw new SprauteRuntimeException(fn + "(): первым нужен персонаж, объявленный через character");
        } else {
            return ch;
        }
    }

    private void afterSpawn(String name, Character<?> ch) {
        for (Ast.CharacterDecl d : this.script.deployed.program.characters) {
            if (d.name.equals(name)) {
                if (d.health != null) {
                    float hp = (float)Math.max(1.0, num(this.eval(d.health)));
                    WhenReady.run(() -> (Entity)this.script.optional(ch).orElse(null), en -> {
                        if (en instanceof AnimatedEntity ae) {
                            ae.setMaxHealth(hp);
                        } else if (en instanceof LivingEntity le) {
                            AttributeInstance at = le.getAttribute(Attributes.MAX_HEALTH);
                            if (at != null) {
                                at.setBaseValue(hp);
                            }

                            le.setHealth(hp);
                        }
                    });
                }

                if (d.hitbox != null) {
                    String hb = str(this.eval(d.hitbox));
                    Hitboxes.applyWhenReady(() -> (Entity)this.script.optional(ch).orElse(null), List.of(hb));
                }
            }
        }
    }

    public Location position(Ast.Expr e) {
        if (e instanceof Ast.Ident id) {
            if (id.name.equals("player") || id.name.equals("actor") && !this.script.vars.containsKey("actor")) {
                ServerPlayer p = id.name.equals("actor") ? this.actor() : SprauteRuntime.player();
                return p != null ? new Location(p) : null;
            } else if (this.script.locationDecls.containsKey(id.name)) {
                return this.script.location(id.name);
            } else {
                Character<?> ch = this.script.characters.get(id.name);
                if (ch != null) {
                    Entity entity = (Entity)this.script.optional(ch).orElse(null);
                    return entity != null ? new Location(entity) : null;
                } else {
                    if (this.script.vars.containsKey(id.name)) {
                        Location l = toLocation(this.script.vars.get(id.name));
                        if (l != null) {
                            return l;
                        }
                    }

                    throw new SprauteRuntimeException("'" + id.name + "' — не точка и не персонаж");
                }
            }
        } else {
            if (e instanceof Ast.Member || e instanceof Ast.Call || e instanceof Ast.Index) {
                Location l = toLocation(this.ref(e));
                if (l != null) {
                    return l;
                }
            }

            if (e instanceof Ast.Tuple t && t.items.size() == 3) {
                return new Location(num(this.eval(t.items.get(0))), num(this.eval(t.items.get(1))), num(this.eval(t.items.get(2))));
            } else if (e instanceof Ast.Tuple t && t.items.size() == 4) {
                return new Location(
                    Level.OVERWORLD,
                    num(this.eval(t.items.get(0))),
                    num(this.eval(t.items.get(1))),
                    num(this.eval(t.items.get(2))),
                    (float)num(this.eval(t.items.get(3))),
                    0.0F
                );
            } else if (e instanceof Ast.StringLit s) {
                Entity mob = this.nearestMob(s.value);
                if (mob == null) {
                    throw new SprauteRuntimeException("Рядом с игроком нет моба " + s.value);
                } else {
                    return new Location(mob);
                }
            } else {
                throw new SprauteRuntimeException("Ожидались координаты");
            }
        }
    }

    public static Location toLocation(Object v) {
        if (v instanceof EntityRef r) {
            Entity en = r.get();
            return en != null ? new Location(en) : null;
        } else if (v instanceof List<?> l && l.size() == 3) {
            try {
                return new Location(num(l.get(0)), num(l.get(1)), num(l.get(2)));
            } catch (SprauteRuntimeException var3) {
                return null;
            }
        } else {
            return null;
        }
    }

    public Location eyes(Ast.Expr e) {
        Entity en = null;
        if (e instanceof Ast.Ident id) {
            if (id.name.equals("player")) {
                en = SprauteRuntime.player();
            } else if (id.name.equals("actor") && !this.script.vars.containsKey("actor")) {
                en = this.actor();
            } else if (this.script.characters.containsKey(id.name)) {
                en = (Entity)this.script.optional(this.script.characters.get(id.name)).orElse(null);
            } else if (this.script.vars.get(id.name) instanceof EntityRef r) {
                en = r.get();
            }
        } else if (e instanceof Ast.StringLit s) {
            en = this.nearestMob(s.value);
        }

        return en != null ? new Location(Mc.level(en), en.getEyePosition()) : this.position(e);
    }

    public Entity nearestMob(String type) {
        ServerPlayer p = SprauteRuntime.player();
        if (p == null) {
            return null;
        } else {
            String id = type.contains(":") ? type : "minecraft:" + type;
            boolean any = type.equals("mob") || type.equals("any");
            Entity best = null;
            double bestD = Double.MAX_VALUE;

            for (Entity en : Mc.serverLevel(p)
                .getEntities((Entity)null, p.getBoundingBox().inflate(64.0), x -> x instanceof LivingEntity && !(x instanceof Player))) {
                if (any || String.valueOf(Regs.ENTITY_TYPE.getKey(en.getType())).equals(id)) {
                    double d = en.distanceToSqr(p);
                    if (d < bestD) {
                        bestD = d;
                        best = en;
                    }
                }
            }

            return best;
        }
    }

    public String interpolate(String text) {
        if (text.indexOf(123) < 0) {
            return text.replace("\\{", "{");
        } else {
            Matcher m = INTERPOLATION.matcher(text);
            StringBuilder sb = new StringBuilder();

            while (m.find()) {
                String inner = m.group(1).trim();
                String skill = inner.startsWith("skill:") ? this.skillText(inner.substring("skill:".length())) : null;
                Ast.Expr expr = skill == null && !inner.isEmpty() && !inner.matches("(?s)[\\w.+-]+\\s*:.*") ? Parser.parseExpression(inner) : null;
                String value;
                if (skill != null) {
                    value = skill;
                } else if (expr == null) {
                    value = m.group(0);
                } else {
                    try {
                        value = str(this.eval(expr));
                    } catch (SprauteRuntimeException var9) {
                        value = "{" + inner + "?}";
                    }
                }

                m.appendReplacement(sb, Matcher.quoteReplacement(value));
            }

            m.appendTail(sb);
            return sb.toString().replace("\\{", "{");
        }
    }

    private String skillText(String spec) {
        String[] parts = spec.split(":", -1);
        int at = SKILL_FIELDS.contains(parts[0].trim()) ? 0 : 1;
        if (parts.length > at && SKILL_FIELDS.contains(parts[at].trim())) {
            String field = parts[at].trim();
            String arg = parts.length > at + 1 ? parts[at + 1].trim() : "";
            Skills.Tree tree = null;
            if (at == 1) {
                tree = Skills.tree(this.script.deployed.project, parts[0].trim());
            } else {
                for (Skills.Tree t : Skills.trees()) {
                    if (t.project().equals(this.script.deployed.project)) {
                        tree = t;
                        break;
                    }

                    if (tree == null) {
                        tree = t;
                    }
                }
            }

            ServerPlayer p = SprauteRuntime.player();
            if (tree != null && p != null) {
                SkillTree def = tree.def();
                SkillLogic.State st = Skills.state(p, tree);

                return switch (field) {
                    case "level" -> String.valueOf(st.level);
                    case "nextlevel" -> String.valueOf(Math.min(st.level + 1, def.maxLevel));
                    case "xp" -> String.valueOf(st.xp);
                    case "xpnext" -> String.valueOf(SkillLogic.xpToNext(def, st.level));
                    case "maxlevel" -> String.valueOf(def.maxLevel);
                    case "title" -> def.title;
                    case "points" -> String.valueOf(st.points(!arg.isEmpty() ? arg : def.currencies.get(0).id));
                    default -> String.valueOf(arg.isEmpty() ? 0 : st.rank(arg));
                };
            } else {
                return "0";
            }
        } else {
            return null;
        }
    }

    public static boolean truthy(Object v) {
        if (v instanceof Boolean b) {
            return b;
        } else if (v instanceof Collection<?> c) {
            return !c.isEmpty();
        } else if (v instanceof EntityRef r) {
            return r.alive();
        } else if (v instanceof Map<?, ?> m) {
            return !m.isEmpty();
        } else if (v instanceof Double d) {
            return d != 0.0;
        } else {
            return v instanceof String s ? !s.isEmpty() : v != null;
        }
    }

    public static double num(Object v) {
        if (v instanceof Double d) {
            return d;
        } else if (v instanceof Boolean b) {
            return b ? 1.0 : 0.0;
        } else if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim().replace(',', '.'));
            } catch (NumberFormatException var3) {
                throw new SprauteRuntimeException("'" + s + "' — не число");
            }
        } else {
            throw new SprauteRuntimeException("Ожидалось число");
        }
    }

    public static String str(Object v) {
        if (v instanceof Double d) {
            return d == Math.rint(d) && !Double.isInfinite(d) ? String.valueOf(d.longValue()) : String.format(Locale.ROOT, "%.2f", d);
        } else {
            return !(v instanceof List) && !(v instanceof Map) ? String.valueOf(v) : Values.format(v);
        }
    }

    public static boolean same(Object l, Object r) {
        return equal(l, r);
    }

    private static boolean equal(Object l, Object r) {
        if (l instanceof Double || r instanceof Double) {
            try {
                return num(l) == num(r);
            } catch (SprauteRuntimeException var3) {
                return false;
            }
        } else {
            return !(l instanceof EntityRef) && !(r instanceof EntityRef) ? str(l).equals(str(r)) : Objects.equals(l, r);
        }
    }

    public static int order(Object l, Object r) {
        return compare(l, r);
    }

    private static int compare(Object l, Object r) {
        return l instanceof String && r instanceof String ? ((String)l).compareTo((String)r) : Double.compare(num(l), num(r));
    }
}
