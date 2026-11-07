package org.sprautestudio.spraute_engine.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.addon.AddonContext;
import org.sprautestudio.spraute_engine.addon.Addons;
import org.sprautestudio.spraute_engine.addon.StoryCall;
import org.sprautestudio.spraute_engine.build.BlockMoves;
import org.sprautestudio.spraute_engine.build.BlockQuake;
import org.sprautestudio.spraute_engine.build.Builds;
import org.sprautestudio.spraute_engine.build.StructureDef;
import org.sprautestudio.spraute_engine.camera.Cams;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.core.anim.MovementActionType;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.script.Behaviours;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.core.script.Characters;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformer;
import org.sprautestudio.spraute_engine.core.script.ScriptPerformerBuilder;
import org.sprautestudio.spraute_engine.core.script.Title;
import org.sprautestudio.spraute_engine.core.system.CameraState;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.core.system.PlayerTexture;
import org.sprautestudio.spraute_engine.core.system.WorldClock;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.custom.Dungeons;
import org.sprautestudio.spraute_engine.custom.ItemApi;
import org.sprautestudio.spraute_engine.custom.LootDef;
import org.sprautestudio.spraute_engine.custom.MobDef;
import org.sprautestudio.spraute_engine.cutscene.CutsceneDoc;
import org.sprautestudio.spraute_engine.engine.EngineConfig;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.fight.FightDef;
import org.sprautestudio.spraute_engine.fight.Fights;
import org.sprautestudio.spraute_engine.fx.Casts;
import org.sprautestudio.spraute_engine.fx.FxDef;
import org.sprautestudio.spraute_engine.fx.FxPresets;
import org.sprautestudio.spraute_engine.gui.GuiDoc;
import org.sprautestudio.spraute_engine.gui.PanelLook;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.lang.Ast;
import org.sprautestudio.spraute_engine.lang.CommandSpec;
import org.sprautestudio.spraute_engine.lang.EventDefs;
import org.sprautestudio.spraute_engine.lang.ParamType;
import org.sprautestudio.spraute_engine.lang.Specs;
import org.sprautestudio.spraute_engine.nav.Navigation;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.path.PathDef;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.post.Posts;
import org.sprautestudio.spraute_engine.quest.Quests;
import org.sprautestudio.spraute_engine.recipe.RecipeApi;
import org.sprautestudio.spraute_engine.runtime.models.BoneWorld;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.orb.Orbs;
import org.sprautestudio.spraute_engine.runtime.orb.SprauteOrb;
import org.sprautestudio.spraute_engine.shop.Shops;
import org.sprautestudio.spraute_engine.skills.SkillTree;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.stealth.Stealth;
import org.sprautestudio.spraute_engine.stealth.StealthDef;
import org.sprautestudio.spraute_engine.stealth.StealthShape;
import org.sprautestudio.spraute_engine.zone.ZoneDef;
import org.sprautestudio.spraute_engine.zone.Zones;

final class Emitter {
    private final SprauteScriptInstance script;
    private final Eval eval;
    private int labelCounter;
    private final Map<String, Integer> speakerSlots = new HashMap<>();
    private int nextSlot;
    private final Map<ScriptPerformerBuilder, Emitter.Ctx> contexts = new IdentityHashMap<>();
    private final ArrayDeque<String[]> loops = new ArrayDeque<>();
    private int asDepth;
    private int[] lineStarts;
    private static final Set<String> NOT_FOR_COPIES = Set.of("wait click", "wait visible", "name");
    private static final double DEFAULT_SPEED = 0.85;
    private static final double WALK_SPEED = 1.0;
    private boolean handlerWait;
    private static final Set<String> LASTING = Set.of(
        "wait until", "wait flag", "wait near", "wait lookat", "wait enter", "wait exit", "wait visible", "wait dimension", "wait zone", "wait outside"
    );
    private boolean keyHandler;
    private int gizmoSeq;
    private static final Map<String, Boolean> ANIMATED_TYPES = new HashMap<>();

    Emitter(SprauteScriptInstance script) {
        this.script = script;
        this.eval = script.eval;
    }

    void emitProgram(ScriptPerformerBuilder b, List<Ast.Stmt> body) {
        this.statements(b, body);
        if (hasHandlers(body)) {
            b.thenWaitUntil(() -> false);
        }

        this.finish(b);
    }

    private static boolean hasHandlers(List<?> stmts) {
        for (Object s : stmts) {
            if (s instanceof Ast.OnStmt) {
                return true;
            }

            if (s != null) {
                for (Field f : s.getClass().getFields()) {
                    try {
                        if (f.get(s) instanceof List<?> l && !l.isEmpty() && hasHandlers(l)) {
                            return true;
                        }
                    } catch (IllegalAccessException var9) {
                    }
                }
            }
        }

        return false;
    }

    private Emitter.Ctx ctx(ScriptPerformerBuilder b) {
        return this.contexts.computeIfAbsent(b, k -> new Emitter.Ctx("p" + this.contexts.size()));
    }

    private static String fnLabel(String name) {
        return "__fn_" + name;
    }

    private void finish(ScriptPerformerBuilder b) {
        Emitter.Ctx ctx = this.ctx(b);
        if (!ctx.used.isEmpty()) {
            b.gotoLabel(ctx.end, true);
            Set<String> emitted = new HashSet<>();

            while (true) {
                String next = null;

                for (String f : ctx.used) {
                    if (!emitted.contains(f)) {
                        next = f;
                        break;
                    }
                }

                if (next == null) {
                    b.label(ctx.end);
                    return;
                }

                emitted.add(next);
                Ast.FuncDecl fx = this.script.deployed.program.func(next);
                b.label(fnLabel(next));
                ArrayDeque<String[]> outer = new ArrayDeque<>(this.loops);
                int outerAs = this.asDepth;
                this.loops.clear();
                this.asDepth = 0;
                if (fx != null) {
                    this.statements(b, fx.body);
                }

                this.loops.addAll(outer);
                this.asDepth = outerAs;
                this.returnJump(b, ctx);
            }
        }
    }

    private void returnJump(ScriptPerformerBuilder b, Emitter.Ctx ctx) {
        b.gotoLabel(() -> {
            Deque<String> d = this.script.callStacks.get(ctx.id);
            return d != null && !d.isEmpty() ? d.pop() : ctx.end;
        }, true);
    }

    private void call(ScriptPerformerBuilder b, Ast.CallStmt c) {
        Ast.FuncDecl f = this.script.deployed.program.func(c.name);
        if (f == null) {
            b.thenRun(() -> SprauteRuntime.get().report(this.script.getId(), "Функция '" + c.name + "' не объявлена"));
        } else {
            Emitter.Ctx ctx = this.ctx(b);
            ctx.used.add(f.name);
            String ret = this.freshLabel("ret");
            List<Ast.Expr> args = c.args;
            b.thenRun(() -> {
                Deque<String> d = this.script.callStacks.computeIfAbsent(ctx.id, k -> new ArrayDeque<>());
                if (d.size() >= 256) {
                    SprauteRuntime.get().report(this.script.getId(), "Слишком глубокая рекурсия в функции " + f.name);
                    d.clear();
                }

                d.push(ret);
                Object[] values = new Object[f.params.size()];

                for (int i = 0; i < values.length && i < args.size(); i++) {
                    try {
                        values[i] = this.eval.eval(args.get(i));
                    } catch (SprauteRuntimeException var9) {
                        SprauteRuntime.get().report(this.script.getId(), "Аргумент " + f.params.get(i) + ": " + var9.getMessage());
                        values[i] = 0.0;
                    }
                }

                for (int i = 0; i < values.length; i++) {
                    if (values[i] != null) {
                        this.script.vars.put(f.params.get(i), values[i]);
                    }
                }
            });
            b.gotoLabel(fnLabel(f.name), true);
            b.label(ret);
        }
    }

    static String runningFlag(String branch) {
        return "__run_" + branch;
    }

    private void branch(ScriptPerformerBuilder b, String name, Consumer<ScriptPerformerBuilder> body) {
        ScriptPerformer performer = this.branchPerformer(name, body);
        b.thenRun(() -> this.script.vars.put(runningFlag(name), true));
        b.createParallelPerformer(name, performer);
    }

    private ScriptPerformer branchPerformer(String name, Consumer<ScriptPerformerBuilder> body) {
        ScriptPerformerBuilder sub = ScriptPerformerBuilder.builder(this.script);
        ArrayDeque<String[]> outer = new ArrayDeque<>(this.loops);
        int outerAs = this.asDepth;
        this.loops.clear();
        this.asDepth = 0;

        try {
            body.accept(sub);
        } finally {
            this.loops.clear();
            this.loops.addAll(outer);
            this.asDepth = outerAs;
        }

        this.finish(sub);
        String gone = this.freshLabel("gone");
        sub.label(gone);
        sub.onPlayerGone(gone);
        String flag = runningFlag(name);
        sub.thenRun(() -> this.script.vars.put(flag, false));
        return sub.build();
    }

    private CutsceneClips compileClips(String cutscene, CutsceneDoc doc, ScriptPerformer by) {
        CutsceneClips out = new CutsceneClips(this.script, by);
        if (!doc.clips.isEmpty()) {
            SprauteRuntime.get().report(this.script.getId(), "Скрипты катсцены «" + cutscene + "» пропущены. Есть только в полной версии Spraute Engine");
        }

        return out;
    }

    private void pushLoop(String start, String end) {
        this.loops.push(new String[]{start, end, String.valueOf(this.asDepth)});
    }

    private void leaveAsBlocks(ScriptPerformerBuilder b, int downTo) {
        if (this.asDepth > downTo) {
            b.thenAsLeave(this.asDepth - downTo);
        }
    }

    private String freshLabel(String hint) {
        return "__sp_" + hint + "_" + this.labelCounter++;
    }

    private void statements(ScriptPerformerBuilder b, List<Ast.Stmt> body) {
        for (Ast.Stmt s : body) {
            this.statement(b, s);
        }
    }

    private void statement(ScriptPerformerBuilder b, Ast.Stmt s) {
        int from = b.mark();
        this.statementSteps(b, s);
        b.words(from, this.lineOf(s));
    }

    private String lineOf(Ast.Node s) {
        String src = this.script.deployed.source;
        if (src != null && s.start >= 0 && s.start < src.length()) {
            if (this.lineStarts == null) {
                List<Integer> starts = new ArrayList<>();
                starts.add(0);

                for (int i = 0; i < src.length(); i++) {
                    if (src.charAt(i) == '\n') {
                        starts.add(i + 1);
                    }
                }

                this.lineStarts = starts.stream().mapToInt(Integer::intValue).toArray();
            }

            int found = Arrays.binarySearch(this.lineStarts, s.start);
            int line = found >= 0 ? found + 1 : -found - 1;
            int lineEnd = src.indexOf(10, s.start);
            String text = src.substring(s.start, lineEnd < 0 ? src.length() : lineEnd).trim();
            if (text.endsWith("{")) {
                text = text.substring(0, text.length() - 1).trim();
            }

            if (text.length() > 80) {
                text = text.substring(0, 79) + "…";
            }

            return line + ": " + text;
        } else {
            return "";
        }
    }

    private void statementSteps(ScriptPerformerBuilder b, Ast.Stmt s) {
        if (s instanceof Ast.LabelStmt l) {
            b.label(l.name);
        } else if (s instanceof Ast.GotoStmt g) {
            b.gotoLabel(g.name, true);
        } else if (s instanceof Ast.StopStmt) {
            b.finish(true);
        } else if (s instanceof Ast.SetStmt set && set.prop != null) {
            Ast.Ident targetId = new Ast.Ident();
            targetId.name = set.name;
            b.thenRun(
                this.guard(
                    () -> {
                        Object target = this.eval.ref(targetId);
                        Object value = this.eval.ref(set.value);
                        if (target instanceof EntityRef er) {
                            if (!set.op.equals("=")) {
                                Object old = WorldApi.get(er, set.prop);
                                value = set.op.equals("-=") ? Eval.num(old) - Eval.num(value) : Eval.num(old) + Eval.num(value);
                            }

                            WorldApi.set(er, set.prop, value);
                        } else if (target instanceof Map<?, ?> m) {
                            if (!set.op.equals("=")) {
                                Object old = ((Map<?, Double>)m).getOrDefault(set.prop, 0.0);
                                value = set.op.equals("-=")
                                    ? Eval.num(old) - Eval.num(value)
                                    : (
                                        !(old instanceof String) && !(value instanceof String)
                                            ? Eval.num(old) + Eval.num(value)
                                            : Eval.str(old) + Eval.str(value)
                                    );
                            }

                            ((Map<String, Object>)m).put(set.prop, value);
                        } else {
                            if (!(target instanceof JavaBridge.JavaObj jo)) {
                                throw new SprauteRuntimeException("set " + set.name + "." + set.prop + ": " + set.name + " — не сущность и не словарь");
                            }

                            if (!set.op.equals("=")) {
                                Object old = JavaBridge.get(jo, set.prop);
                                value = set.op.equals("-=")
                                    ? Eval.num(old) - Eval.num(value)
                                    : (
                                        !(old instanceof String) && !(value instanceof String)
                                            ? Eval.num(old) + Eval.num(value)
                                            : Eval.str(old) + Eval.str(value)
                                    );
                            }

                            JavaBridge.set(jo, set.prop, value);
                        }
                    }
                )
            );
        } else if (s instanceof Ast.SetStmt set && set.index != null) {
            b.thenRun(
                this.guard(
                    () -> {
                        Object container = this.script.vars.get(set.name);
                        Object key = this.eval.eval(set.index);
                        Object value = this.eval.eval(set.value);
                        if (!set.op.equals("=")) {
                            Object old = Values.index(container, key);
                            value = set.op.equals("-=")
                                ? Eval.num(old) - Eval.num(value)
                                : (!(old instanceof String) && !(value instanceof String) ? Eval.num(old) + Eval.num(value) : Eval.str(old) + Eval.str(value));
                        }

                        Values.setIndex(container, key, value);
                    }
                )
            );
        } else if (s instanceof Ast.SetStmt set) {
            b.thenRun(this.guard(() -> {
                Object value = this.eval.eval(set.value);
                String var3x = set.op;
                switch (var3x) {
                    case "+=":
                        Object old = this.script.vars.get(set.name);
                        value = !(old instanceof String) && !(value instanceof String) ? Eval.num(old) + Eval.num(value) : Eval.str(old) + Eval.str(value);
                        break;
                    case "-=":
                        value = Eval.num(this.script.vars.get(set.name)) - Eval.num(value);
                }

                this.script.vars.put(set.name, value);
            }));
        } else if (s instanceof Ast.IfStmt i) {
            this.ifStmt(b, i);
        } else if (s instanceof Ast.WhileStmt w) {
            String start = this.freshLabel("while");
            String body = this.freshLabel("do");
            String end = this.freshLabel("wend");
            b.label(start);
            b.gotoLabel(this.cond(w.cond, body, end), true);
            b.label(body);
            this.pushLoop(start, end);
            this.statements(b, w.body);
            this.loops.pop();
            b.gotoLabel(start, true);
            b.label(end);
        } else if (s instanceof Ast.RepeatStmt r) {
            String counter = "__repeat_" + this.labelCounter++;
            String start = this.freshLabel("rep");
            String body = this.freshLabel("rdo");
            String end = this.freshLabel("rend");
            b.thenRun(this.guard(() -> this.script.vars.put(counter, Math.floor(Eval.num(this.eval.eval(r.count))))));
            b.label(start);
            b.gotoLabel(() -> Eval.num(this.script.vars.getOrDefault(counter, 0.0)) > 0.0 ? body : end, true);
            b.label(body);
            b.thenRun(() -> this.script.vars.put(counter, Eval.num(this.script.vars.getOrDefault(counter, 0.0)) - 1.0));
            this.pushLoop(start, end);
            this.statements(b, r.body);
            this.loops.pop();
            b.gotoLabel(start, true);
            b.label(end);
        } else if (s instanceof Ast.AsStmt as) {
            String end = this.freshLabel("asend");
            b.thenAs(() -> {
                try {
                    return this.eval.playerOf(as.who);
                } catch (SprauteRuntimeException var3x) {
                    SprauteRuntime.get().report(this.script.getId(), "as: блок пропущен. " + var3x.getMessage());
                    return null;
                }
            }, end);
            this.asDepth++;
            this.statements(b, as.body);
            this.asDepth--;
            b.thenAsLeave(1);
            b.label(end);
        } else if (s instanceof Ast.ForStmt f) {
            int n = this.labelCounter++;
            String items = "__for_" + n;
            String idx = "__fori_" + n;
            String start = this.freshLabel("for");
            String body = this.freshLabel("fdo");
            String end = this.freshLabel("fend");
            b.thenRun(this.guard(() -> {
                this.script.vars.put(items, Values.iterable(this.eval.eval(f.iter)));
                this.script.vars.put(idx, 0.0);
            }));
            b.label(start);
            b.gotoLabel(() -> {
                Object lx = this.script.vars.get(items);
                int ix = (int)Eval.num(this.script.vars.getOrDefault(idx, 0.0));
                if (lx instanceof List<?> list && ix < list.size()) {
                    this.script.vars.put(f.var, list.get(ix));
                    this.script.vars.put(idx, ix + 1.0);
                    return body;
                } else {
                    return end;
                }
            }, true);
            b.label(body);
            this.pushLoop(start, end);
            this.statements(b, f.body);
            this.loops.pop();
            b.gotoLabel(start, true);
            b.label(end);
            b.thenRun(() -> {
                this.script.vars.remove(items);
                this.script.vars.remove(idx);
            });
        } else if (s instanceof Ast.EveryStmt ev) {
            this.branch(b, "__every_" + this.labelCounter++, sub -> {
                String loop = this.freshLabel("every");
                int[] left = new int[]{0};
                sub.label(loop);
                this.statements(sub, ev.body);
                sub.thenRun(() -> {
                    try {
                        left[0] = Math.max(1, (int)Math.round(Eval.num(this.eval.eval(ev.interval))));
                    } catch (SprauteRuntimeException var4x) {
                        SprauteRuntime.get().report(this.script.getId(), "every: " + var4x.getMessage());
                        left[0] = 20;
                    }
                });
                sub.thenWaitUntil(() -> --left[0] <= 0);
                sub.gotoLabel(loop, true);
            });
        } else if (s instanceof Ast.BreakStmt) {
            if (!this.loops.isEmpty()) {
                this.leaveAsBlocks(b, Integer.parseInt(this.loops.peek()[2]));
                b.gotoLabel(this.loops.peek()[1], true);
            }
        } else if (s instanceof Ast.ContinueStmt) {
            if (!this.loops.isEmpty()) {
                this.leaveAsBlocks(b, Integer.parseInt(this.loops.peek()[2]));
                b.gotoLabel(this.loops.peek()[0], true);
            }
        } else if (s instanceof Ast.MethodStmt ms) {
            Ast.Ident targetId = new Ast.Ident();
            targetId.name = ms.target;
            b.thenRun(
                this.guard(
                    () -> {
                        boolean entity = ms.target.equals("player")
                            || this.script.characters.containsKey(ms.target)
                            || this.script.vars.get(ms.target) instanceof EntityRef;
                        if (entity) {
                            List<Object> args = new ArrayList<>();

                            for (Ast.Expr x : ms.args) {
                                args.add(this.eval.ref(x));
                            }

                            if (this.eval.ref(targetId) instanceof EntityRef er) {
                                WorldApi.call(er, ms.method, args);
                            }
                        } else {
                            Object target = this.script.vars.get(ms.target);
                            if (!(target instanceof List)
                                && !(target instanceof Map)
                                && (ms.method.equals("add") || ms.method.equals("push") || ms.method.equals("insert"))) {
                                target = new ArrayList();
                                this.script.vars.put(ms.target, target);
                            }

                            List<Object> args = new ArrayList<>();

                            for (Ast.Expr x : ms.args) {
                                args.add(this.eval.ref(x));
                            }

                            Values.method(target, ms.method, args);
                        }
                    }
                )
            );
        } else if (s instanceof Ast.ChoiceStmt c) {
            this.choice(b, c);
        } else if (s instanceof Ast.ParallelStmt ps) {
            String name = ps.name != null ? ps.name : "__async_" + this.labelCounter++;
            this.branch(b, name, sub -> this.statements(sub, ps.body));
        } else if (s instanceof Ast.CallStmt c) {
            if (c.async) {
                this.branch(b, c.as != null ? c.as : "__async_" + this.labelCounter++, sub -> this.call(sub, c));
            } else {
                this.call(b, c);
            }
        } else if (s instanceof Ast.OnStmt on) {
            String handlerName = "__on_" + this.labelCounter++;
            SprauteScriptInstance.Handler handler = new SprauteScriptInstance.Handler(this.lineOf(on));
            this.script.handlers.put(handlerName, handler);
            this.branch(b, handlerName, sub -> {
                String loop = this.freshLabel("on");
                sub.label(loop);
                int waitFrom = sub.mark();
                this.handlerWait = true;
                sub.onBecoming(LASTING.contains(on.event.key));

                try {
                    this.command(sub, on.event);
                } finally {
                    this.handlerWait = false;
                    sub.onBecoming(false);
                }

                sub.words(waitFrom, "ждёт: " + handler.title);
                sub.thenRun(() -> handler.count++);
                boolean outer = this.keyHandler;
                this.keyHandler = on.event.key.equals("wait key");

                try {
                    this.statements(sub, on.body);
                } finally {
                    this.keyHandler = outer;
                }

                sub.thenAsReset();
                sub.thenDelay(1);
                sub.gotoLabel(loop, true);
            });
        } else if (s instanceof Ast.SwitchStmt sw) {
            this.switchStmt(b, sw);
        } else if (s instanceof Ast.ReturnStmt) {
            this.leaveAsBlocks(b, 0);
            this.returnJump(b, this.ctx(b));
        } else if (s instanceof Ast.AwaitStmt a) {
            List<String> names = new ArrayList<>(a.names);
            b.thenWaitUntil(() -> {
                for (String n : names) {
                    if (Eval.truthy(this.script.vars.get(runningFlag(n)))) {
                        return false;
                    }
                }

                return true;
            });
        } else if (s instanceof Ast.CancelStmt cs) {
            b.removeParallelPerformer(cs.name);
            b.thenRun(() -> this.script.vars.put(runningFlag(cs.name), false));
        } else if (s instanceof Ast.CommandStmt cmd && !this.copiesCommand(b, cmd)) {
            this.command(b, cmd);
        }
    }

    private String copiesIn(Ast.CommandStmt c) {
        CommandSpec spec = Specs.command(c.key);
        if (spec != null && !NOT_FOR_COPIES.contains(c.key)) {
            for (CommandSpec.Group group : spec.groups) {
                for (CommandSpec.Param param : group.params) {
                    if (param.type == ParamType.CHAR
                        && c.arg(param.name) instanceof Ast.Ident id
                        && this.script.deployed.program.copies.getOrDefault(id.name, 1) > 1) {
                        return id.name;
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private Ast.CommandStmt forCopy(Ast.CommandStmt c, String character, String copy) {
        Ast.CommandStmt one = new Ast.CommandStmt();
        one.key = c.key;
        one.nameStart = c.nameStart;
        one.nameEnd = c.nameEnd;
        one.start = c.start;
        one.end = c.end;

        for (Ast.Arg a : c.args) {
            if (!"count".equals(a.param) || !c.key.equals("spawn")) {
                Ast.Arg arg = new Ast.Arg();
                arg.keyword = a.keyword;
                arg.param = a.param;
                arg.value = a.value;
                if (a.value instanceof Ast.Ident id && id.name.equals(character)) {
                    Ast.Ident named = new Ast.Ident();
                    named.name = copy;
                    named.start = id.start;
                    named.end = id.end;
                    arg.value = named;
                }

                one.args.add(arg);
            }
        }

        return one;
    }

    private boolean copiesCommand(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        String character = this.copiesIn(c);
        if (character == null) {
            return false;
        } else {
            Ast.Program program = this.script.deployed.program;
            if (c.key.equals("spawn")) {
                int n = c.arg("count") instanceof Ast.NumberLit lit ? (int)lit.value : 1;

                for (int i = 1; i <= Math.min(n, program.copies.get(character)); i++) {
                    this.command(b, this.forCopy(c, character, Ast.Program.copyName(character, i)));
                }

                return true;
            } else {
                boolean always = c.key.equals("despawn") || c.key.equals("free");
                List<String> flags = new ArrayList<>();

                for (int i = 1; i <= program.copies.get(character); i++) {
                    String copy = Ast.Program.copyName(character, i);
                    Character<?> ch = this.script.characters.get(copy);
                    if (ch != null) {
                        Ast.CommandStmt one = this.forCopy(c, character, copy);
                        String name = "__copy_" + this.labelCounter++;
                        flags.add(runningFlag(name));
                        this.branch(b, name, sub -> {
                            if (always) {
                                this.command(sub, one);
                            } else {
                                String go = this.freshLabel("copy");
                                String past = this.freshLabel("nocopy");
                                sub.gotoLabel(() -> this.script.optional(ch).isPresent() ? go : past, true);
                                sub.label(go);
                                this.command(sub, one);
                                sub.label(past);
                            }
                        });
                    }
                }

                b.thenWaitUntil(() -> {
                    for (String f : flags) {
                        if (Eval.truthy(this.script.vars.get(f))) {
                            return false;
                        }
                    }

                    return true;
                });
                return true;
            }
        }
    }

    private Location scattered(Location at, double radius) {
        MinecraftServer server = Platform.get().server();
        ServerLevel level = at != null && server != null ? server.getLevel(at.dimension) : null;
        if (level != null && !(radius <= 0.0)) {
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = random.nextDouble(Math.PI * 2);
                double far = radius * Math.sqrt(random.nextDouble());
                double x = at.x + Math.cos(angle) * far;
                double z = at.z + Math.sin(angle) * far;

                for (int up = 3; up >= -6; up--) {
                    Location here = new Location(at.dimension, x, Math.floor(at.y) + up, z, random.nextFloat() * 360.0F, 0.0F);
                    BlockPos pos = Mc.blockPos(here);
                    if (level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                        && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                        && !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                        return here;
                    }
                }
            }

            return at;
        } else {
            return at;
        }
    }

    private void switchStmt(ScriptPerformerBuilder b, Ast.SwitchStmt sw) {
        String tmp = "__switch_" + this.labelCounter++;
        List<String> labels = new ArrayList<>();

        for (int i = 0; i < sw.cases.size(); i++) {
            labels.add(this.freshLabel("case"));
        }

        String otherwise = this.freshLabel("default");
        String end = this.freshLabel("esac");
        b.thenRun(this.guard(() -> this.script.vars.put(tmp, this.eval.eval(sw.value))));
        b.gotoLabel(() -> {
            Object v = this.script.vars.get(tmp);

            for (int ix = 0; ix < sw.cases.size(); ix++) {
                for (Ast.Expr e : sw.cases.get(ix).values) {
                    try {
                        if (Eval.same(v, this.eval.eval(e))) {
                            return labels.get(ix);
                        }
                    } catch (SprauteRuntimeException var10) {
                        SprauteRuntime.get().report(this.script.getId(), "switch: " + var10.getMessage());
                    }
                }
            }

            return otherwise;
        }, true);

        for (int i = 0; i < sw.cases.size(); i++) {
            b.label(labels.get(i));
            this.statements(b, sw.cases.get(i).body);
            b.gotoLabel(end, true);
        }

        b.label(otherwise);
        if (sw.otherwise != null) {
            this.statements(b, sw.otherwise);
        }

        b.label(end);
    }

    private void ifStmt(ScriptPerformerBuilder b, Ast.IfStmt i) {
        String then = this.freshLabel("then");
        String otherwise = this.freshLabel("else");
        String end = this.freshLabel("fi");
        b.gotoLabel(this.cond(i.cond, then, otherwise), true);
        b.label(then);
        this.statements(b, i.then);
        b.gotoLabel(end, true);
        b.label(otherwise);
        if (i.otherwise != null) {
            this.statements(b, i.otherwise);
        }

        b.label(end);
    }

    private Supplier<String> cond(Ast.Expr e, String ifTrue, String ifFalse) {
        return () -> {
            try {
                return Eval.truthy(this.eval.eval(e)) ? ifTrue : ifFalse;
            } catch (SprauteRuntimeException var5) {
                SprauteRuntime.get().report(this.script.getId(), "Ошибка в условии: " + var5.getMessage());
                return ifFalse;
            }
        };
    }

    private void walkRoute(ScriptPerformerBuilder b, Character ch, Supplier<List<Location>> route, Ast.Expr speedE) {
        b.thenClearMovement(ch);
        b.thenRun(this.guard(() -> {
            Entity e = this.script.<Entity>optional(ch).orElse(null);
            if (e != null && this.script.controls(ch)) {
                PathDef d = new PathDef();

                for (Location l : route.get()) {
                    if (l != null) {
                        d.points.add(new PathDef.Point(l.x, l.y, l.z));
                    }
                }

                d.speed = Math.max(0.05, this.number(speedE, 1.0));
                Walkers.start(e, "", d, 0.0, "once", 0);
            }
        }));
        b.thenWaitUntil(() -> !Walkers.walking(this.script.<Entity>optional(ch).orElse(null)));
    }

    private void choice(ScriptPerformerBuilder b, Ast.ChoiceStmt c) {
        this.ownChoice(b, c);
    }

    private void ownChoice(ScriptPerformerBuilder b, Ast.ChoiceStmt c) {
        Character<?> owner = this.character(c.character);
        String name = c.character;
        List<Ast.Expr> texts = new ArrayList<>();

        for (Ast.ChoiceOption o : c.options) {
            texts.add(o.text);
        }

        Ast.Expr nodeEmotion = c.emotion;
        List<String> branches = new ArrayList<>();

        for (int i = 0; i < c.options.size(); i++) {
            branches.add(this.freshLabel("opt" + (i + 1)));
        }

        String end = this.freshLabel("choice_end");
        b.thenRun(
            () -> {
                this.script.lastAnswer = 0;
                this.script.choiceSeq++;
                ServerPlayer p = SprauteRuntime.player();
                if (p != null) {
                    this.script.choicePlayer = p.getUUID();
                    Entity en = (Entity)this.script.optional(owner).orElse(null);
                    String modelKey = this.script.characterModels.get(name);
                    if (modelKey != null && en instanceof SprauteModelEntity me && !modelKey.equals(me.model())) {
                        me.setModel(modelKey);
                    }

                    List<String> answers = new ArrayList<>();
                    List<String> emotions = new ArrayList<>();
                    List<Boolean> locked = new ArrayList<>();
                    List<Boolean> centered = new ArrayList<>();

                    for (Ast.Expr t : texts) {
                        answers.add(this.text(t));
                    }

                    for (Ast.ChoiceOption o : c.options) {
                        locked.add(o.locked != null && Eval.truthy(this.eval.eval(o.locked)));
                        emotions.add(o.emotion != null ? this.emotionName(o.emotion) : "");
                        centered.add(o.center);
                    }

                    ResourceLocation picture = this.script.portraits.get(name);
                    if (en != null) {
                        this.script.openDialog(en, c.anims != null ? List.of(this.text(c.anims).split(";")) : List.of());
                    }

                    String face = nodeEmotion != null ? this.emotionName(nodeEmotion) : "NEUTRAL";
                    if (en instanceof AnimatedEntity ae) {
                        Anims.showEmotion(ae, face);
                    }

                    SprauteNetwork.sendTo(
                        p,
                        new Packets.ChoiceOpen(
                            this.script.getId(),
                            this.script.choiceSeq,
                            owner.getName(),
                            this.text(c.text),
                            answers,
                            en != null ? en.getId() : -1,
                            locked,
                            emotions,
                            face,
                            centered,
                            picture != null ? picture.toString() : ""
                        )
                    );
                }
            }
        );
        b.thenWaitUntil(() -> this.script.lastAnswer != 0);
        b.gotoLabel(() -> {
            int a = this.script.lastAnswer;
            return a >= 1 && a <= branches.size() ? branches.get(a - 1) : end;
        }, true);

        for (int i = 0; i < c.options.size(); i++) {
            b.label(branches.get(i));
            this.statements(b, c.options.get(i).body);
            b.gotoLabel(end, true);
        }

        b.label(end);
    }

    private void command(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        CommandSpec spec = Specs.command(c.key);
        if (spec != null && Specs.fullOnly(spec)) {
            throw new SprauteRuntimeException("Есть только в полной версии Spraute Engine: " + c.key);
        } else {
            String var23 = c.key;
            switch (var23) {
                case "speaker": {
                    int slotx = (int)Eval.num(this.eval.eval(c.arg("slot")));
                    Ast.Expr who = c.arg("who");
                    if (who instanceof Ast.Ident id && this.script.characters.containsKey(id.name)) {
                        this.speakerSlots.put("char:" + id.name, slotx);
                        this.speechEntity(b, slotx, id.name);
                    } else {
                        if (who instanceof Ast.Ident idx && idx.name.equals("player")) {
                            this.speakerSlots.put("player", slotx);
                            this.speechPlayer(b, slotx);
                            break;
                        }

                        String namex = this.text(who);
                        this.speakerSlots.put("name:" + namex, slotx);
                        b.thenSetSpeechEntity(slotx, namex);
                    }
                    break;
                }
                case "say": {
                    if (c.arg("who") instanceof Ast.Ident sid && this.script.characters.containsKey(sid.name)) {
                        this.headSync(b, sid.name);
                    }

                    int slotx = this.speakerSlot(b, c.arg("who"));
                    Ast.Expr text = c.arg("text");
                    boolean waitxx = c.arg("nowait") == null && !this.keyHandler;
                    Supplier<Component> hint = waitxx && EngineServer.config().chatLog.f8Hint ? () -> Component.literal("▸ F8") : null;
                    b.thenSpeechMessage(slotx, hint, () -> Component.literal(this.safeText(text)), waitxx);
                    break;
                }
                case "think": {
                    Ast.Expr text = c.arg("text");
                    b.thenThink(() -> Component.literal(this.safeText(text)), c.arg("nowait") == null && !this.keyHandler);
                    break;
                }
                case "clearchat":
                    b.clearChat();
                    break;
                case "title":
                    Title.Builder tb = Title.builder(this.text(c.arg("text")));
                    if (c.arg("subtitle") != null) {
                        tb.subtitle(this.text(c.arg("subtitle")));
                    }

                    if (c.arg("fadeIn") != null) {
                        tb.times(this.ticks(c.arg("fadeIn")), this.ticks(c.arg("stay")), this.ticks(c.arg("fadeOut")));
                    }

                    b.thenTitle(tb.build());
                    break;
                case "message": {
                    Ast.Expr text = c.arg("text");
                    b.thenRun(this.guard(() -> {
                        Component msg = Component.literal(this.safeText(text));

                        for (ServerPlayer p : SprauteRuntime.audience()) {
                            p.sendSystemMessage(msg);
                        }
                    }));
                    break;
                }
                case "web":
                    b.sendWebMessage(this.text(c.arg("text")), false);
                    break;
                case "warn":
                    b.sendWebWarning(this.text(c.arg("text")));
                    break;
                case "wait": {
                    Ast.Expr t = c.arg("time");
                    if (!(t instanceof Ast.DurationLit) && !(t instanceof Ast.NumberLit)) {
                        b.thenDelay(() -> this.ticksSafe(t));
                    } else {
                        b.thenDelay(this.ticks(t));
                    }
                    break;
                }
                case "wait random":
                    b.thenDelayRandom(this.ticks(c.arg("min")), this.ticks(c.arg("max")), this.script.getRandom());
                    break;
                case "wait until":
                    Ast.Expr cond = c.arg("condition");
                    b.thenWaitUntil(() -> {
                        try {
                            return Eval.truthy(this.eval.eval(cond));
                        } catch (SprauteRuntimeException var3x) {
                            SprauteRuntime.get().report(this.script.getId(), "Ошибка в wait until: " + var3x.getMessage());
                            return true;
                        }
                    });
                    break;
                case "wait flag":
                    b.thenWaitForFlag(this.text(c.arg("name")));
                    break;
                case "wait key": {
                    Ast.Expr k = c.arg("key");
                    String name = k instanceof Ast.Ident id ? id.name : this.text(k);
                    if (this.handlerWait) {
                        b.thenWaitForEachKey(name.toLowerCase(Locale.ROOT));
                    } else {
                        b.thenWaitForKey(name.toLowerCase(Locale.ROOT));
                    }
                    break;
                }
                case "wait trigger":
                    b.thenWaitForTrigger();
                    break;
                case "wait chat": {
                    List<Ast.Expr> words = new ArrayList<>();

                    for (Ast.Arg a : c.args) {
                        if ("words".equals(a.param)) {
                            words.add(a.value);
                        }
                    }

                    this.waitLog(b, "chat", () -> {
                        List<String> ws = new ArrayList<>();

                        for (Ast.Expr w : words) {
                            ws.add(this.text(w).trim().toLowerCase(Locale.ROOT));
                        }

                        return dxx -> {
                            String msg = String.valueOf(dxx.get("text")).trim().toLowerCase(Locale.ROOT);
                            if (ws.isEmpty()) {
                                return true;
                            } else {
                                for (String wx : ws) {
                                    if (msg.equals(wx) || (" " + msg.replaceAll("[^\\p{L}\\p{N}]+", " ") + " ").contains(" " + wx + " ")) {
                                        return true;
                                    }
                                }

                                return false;
                            }
                        };
                    });
                    break;
                }
                case "wait click":
                    b.thenAwaitEntityClick(this.character(c.arg("who")));
                    break;
                case "wait clickblock": {
                    Ast.Expr posE = c.arg("pos");
                    Ast.Expr blockE = c.arg("blocktype");
                    this.waitLog(
                        b,
                        "useblock",
                        () -> {
                            BlockPos atx = posE != null ? this.blockPos(posE) : null;
                            String typex = blockE != null ? this.text(blockE).trim() : null;
                            String full = typex != null && !typex.contains(":") ? "minecraft:" + typex : typex;
                            return d -> (
                                    atx == null
                                        || Double.valueOf((double)atx.getX()).equals(d.get("x"))
                                            && Double.valueOf((double)atx.getY()).equals(d.get("y"))
                                            && Double.valueOf((double)atx.getZ()).equals(d.get("z"))
                                )
                                && (full == null || full.equals(d.get("block")));
                        }
                    );
                    break;
                }
                case "wait near": {
                    Ast.Expr target = c.arg("target");
                    double radius = c.arg("radius") != null ? Eval.num(this.eval.eval(c.arg("radius"))) : 3.0;
                    if (target instanceof Ast.Ident id && this.script.characters.containsKey(id.name)) {
                        b.thenWhenPlayerCloseTo(this.character(id.name), radius);
                        break;
                    }

                    b.thenWhenPlayerCloseTo(this.locationSupplier(target), radius);
                    break;
                }
                case "wait lookat": {
                    Ast.Expr target = c.arg("target");
                    b.thenWaitUntil(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p == null) {
                            return false;
                        } else {
                            try {
                                return Raycast.looksAt(new EntityRef(p), this.eval.ref(target), 64.0);
                            } catch (SprauteRuntimeException var4) {
                                return false;
                            }
                        }
                    });
                    break;
                }
                case "wait enter":
                    b.thenWhenPlayerEnterBox(this.box(c.arg("from"), c.arg("to")));
                    break;
                case "wait exit":
                    b.thenWhenPlayerExitBox(this.box(c.arg("from"), c.arg("to")));
                    break;
                case "wait visible":
                    b.thenWaitCharacterVisible(this.character(c.arg("who")), c.arg("nosight") == null);
                    break;
                case "spawn": {
                    Character ch = this.character(c.arg("who"));
                    Supplier<Location> place = this.locationSupplier(c.arg("pos"));
                    Ast.Expr spreadE = c.arg("spread");
                    b.positionEntity(ch, spreadE == null ? place : () -> this.scattered(place.get(), this.number(spreadE, 0.0)));
                    int[] waited = new int[]{0};
                    b.thenRun(() -> waited[0] = 0);
                    b.thenWaitUntil(() -> this.script.optional(ch).isPresent() || ++waited[0] > 40);
                    Ast.Expr sizeE = c.arg("scale");
                    if (sizeE != null) {
                        b.thenRun(this.guard(() -> {
                            float size = (float)this.number(sizeE, 1.0);
                            Characters.state(this.script, ch, "scale", en -> {
                                if (en instanceof SprauteModelEntity me) {
                                    me.setBodyScale(size);
                                }
                            });
                        }));
                    }

                    String hname = c.arg("who") instanceof Ast.Ident hid ? Ast.Program.copyOf(hid.name) : "";

                    for (Ast.CharacterDecl d : this.script.deployed.program.characters) {
                        if (d.name.equals(hname) && d.health != null) {
                            Ast.Expr hpE = d.health;
                            b.thenRun(this.guard(() -> {
                                Object en = this.script.optional(ch).orElse(null);
                                float hp = (float)Math.max(1.0, this.number(hpE, 20.0));
                                if (en instanceof AnimatedEntity ae) {
                                    ae.setMaxHealth(hp);
                                } else if (en instanceof LivingEntity le) {
                                    AttributeInstance atx = le.getAttribute(Attributes.MAX_HEALTH);
                                    if (atx != null) {
                                        atx.setBaseValue(hp);
                                    }

                                    le.setHealth(hp);
                                }
                            }));
                        }
                    }

                    String cname = c.arg("who") instanceof Ast.Ident id ? Ast.Program.copyOf(id.name) : "";

                    for (Ast.CharacterDecl dx : this.script.deployed.program.characters) {
                        if (dx.name.equals(cname) && dx.hitbox != null) {
                            Ast.Expr hb = dx.hitbox;
                            b.thenRun(this.guard(() -> Hitboxes.applyWhenReady(() -> this.script.<Entity>optional(ch).orElse(null), List.of(this.text(hb)))));
                        }
                    }
                    break;
                }
                case "tp":
                    b.positionEntity(this.character(c.arg("who")), this.locationSupplier(c.arg("pos")));
                    break;
                case "despawn":
                    b.removeEntity(this.character(c.arg("who")));
                    break;
                case "move": {
                    Supplier<Location> to = this.locationSupplier(c.arg("target"));
                    this.walkRoute(b, this.character(c.arg("who")), () -> List.of(to.get()), c.arg("speed"));
                    break;
                }
                case "walk": {
                    Character ch = this.character(c.arg("who"));
                    Ast.Expr pathE = c.arg("path");
                    Ast.Expr speedE = c.arg("speed");
                    Ast.Expr fromE = c.arg("from");
                    boolean loop = c.arg("loop") != null;
                    boolean ping = c.arg("pingpong") != null;
                    boolean nowaitxxxxxxxx = c.arg("nowait") != null;
                    b.thenClearMovement(ch);
                    boolean[] endless = new boolean[]{false};
                    b.thenRun(this.guard(() -> {
                        Entity e = this.script.<Entity>optional(ch).orElse(null);
                        if (e == null) {
                            throw new SprauteRuntimeException("Персонажа нет в мире — сначала spawn");
                        } else if (this.script.controls(ch)) {
                            String nx = this.text(pathE);
                            PathDef d = this.pathNamed(nx);
                            String mode = loop ? "loop" : (ping ? "pingpong" : "");
                            endless[0] = !(mode.isEmpty() ? d.mode : mode).equals("once");
                            int from = fromE == null ? 0 : (int)Math.round(this.number(fromE, 1.0)) - 1;
                            Walkers.start(e, nx, d, speedE == null ? 0.0 : this.number(speedE, 0.0), mode, from);
                        }
                    }));
                    if (!nowaitxxxxxxxx) {
                        b.thenWaitUntil(() -> endless[0] || !Walkers.walking(this.script.<Entity>optional(ch).orElse(null)));
                    }
                    break;
                }
                case "stopwalk": {
                    Character ch = this.character(c.arg("who"));
                    b.thenClearMovement(ch);
                    break;
                }
                case "path": {
                    List<Ast.Expr> points = new ArrayList<>();

                    for (Ast.Arg a : c.args) {
                        if ("points".equals(a.param)) {
                            points.add(a.value);
                        }
                    }

                    Supplier<List<Location>> route = () -> {
                        List<Location> list = new ArrayList<>();

                        for (Ast.Expr px : points) {
                            list.add(this.positionSafe(px));
                        }

                        return list;
                    };
                    this.walkRoute(b, this.character(c.arg("who")), route, c.arg("speed"));
                    break;
                }
                case "look": {
                    Character who = this.character(c.arg("who"));
                    b.entityLookAt(who, this.eyesSupplier(c.arg("target")), c.arg("instant") != null);
                    if (c.arg("wait") != null) {
                        b.thenWaitLook(who);
                    }
                    break;
                }
                case "stoplook":
                    b.entityStopLook(this.character(c.arg("who")));
                    break;
                case "follow": {
                    if (c.arg("target") instanceof Ast.Ident id && this.script.characters.containsKey(id.name)) {
                        b.thenEntityFollowEntity(
                            this.character(c.arg("who")), this.character(id.name), this.number(c.arg("speed"), 1.0), this.number(c.arg("min"), 2.0)
                        );
                        break;
                    }

                    double max = c.arg("noteleport") != null ? 1.0E9 : this.number(c.arg("max"), 6.0);
                    b.thenEntityFollowPlayer(this.character(c.arg("who")), this.number(c.arg("speed"), 1.0), this.number(c.arg("min"), 2.0), max);
                    break;
                }
                case "enable":
                case "disable": {
                    Ast.Expr sys = c.arg("sys");
                    String name = sys instanceof Ast.Ident id ? id.name : (sys instanceof Ast.StringLit s ? s.value : "");
                    boolean on = c.key.equals("enable");
                    b.thenRun(() -> EngineServer.setSystem(name, on));
                    break;
                }
                case "walkspeed":
                    b.entityMoveSpeed(this.character(c.arg("who")), this.number(c.arg("speed"), 1.0));
                    break;
                case "wander":
                    b.thenEntityRandomWalk(this.character(c.arg("who")), this.number(c.arg("speed"), 1.0), 10, this.number(c.arg("radius"), 8.0));
                    break;
                case "sit":
                    b.entitySit(this.character(c.arg("who")), c.arg("pos") != null ? this.locationSupplier(c.arg("pos")) : null);
                    break;
                case "clearbehavior": {
                    String t = this.text(c.arg("type")).toUpperCase(Locale.ROOT);

                    try {
                        b.thenClearBehaviour(this.character(c.arg("who")), Behaviours.Type.valueOf(t));
                        break;
                    } catch (IllegalArgumentException var22) {
                        throw new SprauteRuntimeException(
                            "Нет такого типа поведения: " + t + " (movement, look, combat, speech, action, interaction, emotion, complex)"
                        );
                    }
                }
                case "fly": {
                    Supplier<Location> to = this.locationSupplier(c.arg("target"));
                    Character ch = this.character(c.arg("who"));
                    b.thenRun(this.guard(() -> {
                        Entity e = this.script.<Entity>optional(ch).orElse(null);
                        if (e != null && this.script.controls(ch)) {
                            Walkers.fly(e, to.get(), this.number(c.arg("speed"), 1.0));
                        }
                    }));
                    b.thenWaitUntil(() -> !Walkers.walking(this.script.<Entity>optional(ch).orElse(null)));
                    break;
                }
                case "refuse":
                    b.refuse(this.character(c.arg("who")), (int)this.number(c.arg("times"), 1.0));
                    break;
                case "wait dimension":
                    b.thenWhenPlayerInDimension(this.id(c.arg("dim")));
                    break;
                case "stand":
                    b.entityStopSit(this.character(c.arg("who")));
                    break;
                case "name": {
                    Ast.Expr name = c.arg("name");
                    Ast.Expr who = c.arg("who");
                    b.entityNameOverride(this.character(who), () -> this.safeText(name));
                    String key = who instanceof Ast.Ident id ? id.name : "";
                    b.thenRun(() -> this.script.speechNames.put(key, this.safeText(name)));
                    Integer slot = this.speakerSlots.get("char:" + key);
                    if (slot != null) {
                        this.speechEntity(b, slot, key);
                    }
                    break;
                }
                case "hold":
                    b.thenSetHeldItem(this.character(c.arg("who")), this.itemStack(c.arg("item"), 1));
                    break;
                case "pickup": {
                    Character ch = this.character(c.arg("who"));
                    boolean off = c.arg("off") != null;
                    List<Ast.Expr> items = new ArrayList<>();

                    for (Ast.Arg a : c.args) {
                        if ("items".equals(a.param)) {
                            items.add(a.value);
                        }
                    }

                    Ast.Expr count = c.arg("count");
                    b.thenRun(this.guard(() -> {
                        Entity e = this.script.<Entity>optional(ch).orElse(null);
                        if (e instanceof AnimatedEntity npc && this.script.controls(ch)) {
                            if (off) {
                                npc.itemPickup().stop();
                            } else {
                                List<String> filters = new ArrayList<>();

                                for (Ast.Expr itemx : items) {
                                    String specx = this.text(itemx);
                                    if (ItemSpec.error(specx) != null || ItemSpec.item(specx) == Items.AIR) {
                                        throw new SprauteRuntimeException("pickup: предмет " + specx + " не найден или имеет неверный NBT");
                                    }

                                    filters.add(specx);
                                }

                                int limit = count == null ? -1 : (int)this.number(count, -1.0);
                                if (count != null && limit < 0) {
                                    throw new SprauteRuntimeException("pickup: count должен быть неотрицательным");
                                } else {
                                    npc.itemPickup().configure(filters, limit);
                                }
                            }
                        }
                    }));
                    break;
                }
                case "wait npcpickup": {
                    Ast.Expr what = c.arg("what");
                    EventDefs.Def ev = EventDefs.get("npcpickup");
                    if (!this.handlerWait && what instanceof Ast.Ident who && this.script.characters.containsKey(who.name)) {
                        Character<?> ch = this.character(who.name);
                        b.thenRun(
                            () -> {
                                Entity e = (Entity)this.script.optional(ch).orElse(null);
                                if (e != null && !(e instanceof AnimatedEntity npc && npc.itemPickup().active())) {
                                    SprauteRuntime.get()
                                        .report(
                                            this.script.getId(), "wait npcpickup: " + who.name + " сейчас ничего не подбирает — сначала нужна команда pickup"
                                        );
                                }
                            }
                        );
                        b.thenWaitUntil(() -> {
                            Entity e = (Entity)this.script.optional(ch).orElse(null);
                            if (e == null) {
                                return false;
                            } else if (e instanceof AnimatedEntity npc) {
                                NpcItemPickup pickup = npc.itemPickup();
                                if (!pickup.finished()) {
                                    return false;
                                } else {
                                    pickup.markSeen();
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("character", who.name);
                                    data.put("name", e.getName().getString());
                                    data.put("item", pickup.lastItem());
                                    data.put("count", (double)pickup.collected());
                                    this.script.event = data;
                                    return true;
                                }
                            } else {
                                return true;
                            }
                        });
                        break;
                    }

                    this.waitEvent(b, ev, what);
                    break;
                }
                case "drop": {
                    Character ch = this.character(c.arg("who"));
                    Ast.Expr itemE = c.arg("item");
                    Ast.Expr countE = c.arg("count");
                    Ast.Expr targetE = c.arg("target");
                    Ast.Expr speedE = c.arg("speed");
                    Ast.Expr motionE = c.arg("motion");
                    Ast.Expr into = c.arg("var");
                    b.thenRun(this.guard(() -> {
                        if (this.script.controls(ch) && this.script.optional(ch).orElse(null) instanceof LivingEntity who) {
                            ItemStack stack;
                            if (itemE != null) {
                                stack = this.itemToSpawn("drop", this.text(itemE), countE);
                            } else {
                                ItemStack held = who.getMainHandItem();
                                if (held.isEmpty()) {
                                    return;
                                }

                                int n = countE != null ? Math.max(1, Math.min(held.getCount(), (int)this.number(countE, 1.0))) : held.getCount();
                                stack = Mc.copyWithCount(held, n);
                                b.keepHeldItem(ch, n < held.getCount() ? Mc.copyWithCount(held, held.getCount() - n) : ItemStack.EMPTY);
                            }

                            Vec3 from = new Vec3(who.getX(), who.getEyeY() - 0.3, who.getZ());
                            Vec3 motion;
                            if (motionE != null) {
                                motion = WorldApi.vec(this.eval.ref(motionE));
                            } else {
                                double speed = speedE != null ? this.number(speedE, 0.3) : 0.3;
                                Vec3 way = targetE != null ? this.positionSafe(targetE).subtract(from) : who.getLookAngle();
                                if (way.lengthSqr() < 1.0E-6) {
                                    way = who.getLookAngle();
                                }

                                motion = way.normalize().scale(speed).add(0.0, 0.1, 0.0);
                            }

                            this.keepInVariable(into, WorldApi.spawnItem((ServerLevel)Mc.level(who), from, stack, motion, 40, false));
                        } else {
                            throw new SprauteRuntimeException("drop: персонаж не в мире или сюжет им не управляет");
                        }
                    }));
                    break;
                }
                case "spawnitem": {
                    Ast.Expr itemE = c.arg("item");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr countE = c.arg("count");
                    Ast.Expr motionE = c.arg("motion");
                    Ast.Expr delayE = c.arg("delay");
                    Ast.Expr into = c.arg("var");
                    boolean never = c.arg("nopickup") != null;
                    b.thenRun(this.guard(() -> {
                        ItemStack stackx = this.itemToSpawn("spawnitem", this.text(itemE), countE);
                        Vec3 motion = motionE != null ? WorldApi.vec(this.eval.ref(motionE)) : Vec3.ZERO;
                        int delay = delayE != null ? Math.max(0, this.ticks(delayE)) : 10;
                        this.keepInVariable(into, WorldApi.spawnItem(WorldApi.level(), this.positionSafe(pos), stackx, motion, delay, never));
                    }));
                    break;
                }
                case "health":
                    b.setMaxHealth(this.character(c.arg("who")), (float)this.number(c.arg("hp"), 20.0));
                    break;
                case "free":
                    b.freeEntity(this.character(c.arg("who")));
                    break;
                case "nod":
                    b.nod(this.character(c.arg("who")), (int)this.number(c.arg("times"), 1.0));
                    break;
                case "nope":
                    b.nope(this.character(c.arg("who")), (int)this.number(c.arg("times"), 1.0));
                    break;
                case "laugh":
                    if (c.arg("times") == null && c.arg("speed") == null) {
                        b.laugh(this.character(c.arg("who")));
                    } else {
                        b.laugh(this.character(c.arg("who")), (int)this.number(c.arg("times"), 1.0), (float)this.number(c.arg("speed"), 1.0));
                    }
                    break;
                case "anim":
                case "face":
                    this.animation(b, c);
                    break;
                case "playeranim":
                case "playeranim stop":
                    this.playerAnimation(b, c);
                    break;
                case "gui open": {
                    Ast.Expr name = c.arg("name");
                    b.thenRun(
                        this.guard(
                            () -> {
                                String n = this.text(name);
                                String json = SprauteRuntime.get().gui(this.script.deployed.project, n);
                                if (json == null) {
                                    SprauteRuntime.get()
                                        .report(
                                            this.script.getId(),
                                            "Интерфейс «"
                                                + n
                                                + "» не найден в проекте "
                                                + this.script.deployed.project
                                                + ". Загрузите сюжет из редактора ещё раз (F5)"
                                        );
                                } else {
                                    this.script.openGui = n;
                                    this.script.guiInputs.clear();
                                    this.script.guiZones.clear();
                                    ServerPlayer p = SprauteRuntime.player();
                                    this.script.guiPlayer = p != null ? p.getUUID() : null;
                                    if (p != null) {
                                        SprauteNetwork.sendTo(p, new Packets.GuiOpen(this.script.getId(), this.script.deployed.project, n, json));
                                    }

                                    GuiSlots.opened(this.script, GuiDoc.fromJson(json), p);
                                }
                            }
                        )
                    );
                    break;
                }
                case "gui close":
                    b.thenRun(() -> {
                        this.script.openGui = null;
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            SprauteNetwork.sendTo(p, new Packets.GuiClose(this.script.getId()));
                        }
                    });
                    break;
                case "gui text":
                    this.guiUpdate(b, c.arg("el"), "text", c.arg("text"));
                    break;
                case "gui show":
                    this.guiUpdateConst(b, c.arg("el"), "visible", "true");
                    break;
                case "gui undrag": {
                    Ast.Expr el = c.arg("el");
                    this.guiUpdateConst(b, el, "undrag", "1");
                    b.thenRun(this.guard(() -> this.script.guiZones.remove(this.text(el))));
                    break;
                }
                case "gui hide":
                    this.guiUpdateConst(b, c.arg("el"), "visible", "false");
                    break;
                case "gui progress":
                    this.guiUpdate(b, c.arg("el"), "value", c.arg("value"));
                    break;
                case "gui image":
                    this.guiUpdate(b, c.arg("el"), "image", c.arg("image"));
                    break;
                case "gui scroll":
                    this.guiUpdate(b, c.arg("el"), "scrollY", c.arg("y"));
                    break;
                case "overlay show": {
                    Ast.Expr name = c.arg("name");
                    b.thenRun(this.guard(() -> {
                        String n = this.text(name);
                        String json = SprauteRuntime.get().gui(this.script.deployed.project, n);
                        if (json == null) {
                            throw new SprauteRuntimeException("Интерфейс «" + n + "» не найден в проекте. Загрузите сюжет ещё раз (F5)");
                        } else {
                            this.script.overlayShown(n, json);
                            SprauteNetwork.sendToAll(new Packets.OverlayShow(this.script.getId(), this.script.deployed.project, n, json));
                        }
                    }));
                    break;
                }
                case "overlay hide": {
                    Ast.Expr name = c.arg("name");
                    b.thenRun(this.guard(() -> {
                        String n = name == null ? "" : this.text(name);
                        this.script.overlayHidden(n);
                        SprauteNetwork.sendToAll(new Packets.OverlayHide(this.script.getId(), n));
                    }));
                    break;
                }
                case "gui move":
                case "gui resize": {
                    boolean move = c.key.equals("gui move");
                    Ast.Expr el = c.arg("el");
                    Ast.Expr a1 = c.arg(move ? "x" : "w");
                    Ast.Expr a2 = c.arg(move ? "y" : "h");
                    Ast.Expr time = c.arg("time");
                    b.thenRun(this.guard(() -> {
                        String suffix = time != null ? "@" + this.ticks(time) / 20.0F : "";
                        String idx = this.text(el);
                        this.sendGuiUpdate(idx, (move ? "x" : "w") + suffix, this.safeText(a1));
                        this.sendGuiUpdate(idx, (move ? "y" : "h") + suffix, this.safeText(a2));
                    }));
                    break;
                }
                case "gui animate": {
                    Ast.Expr el = c.arg("el");
                    Ast.Expr prop = c.arg("prop");
                    Ast.Expr value = c.arg("value");
                    Ast.Expr time = c.arg("time");
                    b.thenRun(
                        this.guard(
                            () -> this.sendGuiUpdate(
                                this.text(el), this.text(prop) + (time != null ? "@" + this.ticks(time) / 20.0F : ""), this.safeText(value)
                            )
                        )
                    );
                    break;
                }
                case "cutscene play": {
                    Ast.Expr name = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    boolean waitx = c.arg("nowait") == null;
                    String[] playing = new String[]{""};
                    ScriptPerformer player = b.performer();
                    b.thenRun(this.guard(() -> {
                        String nx = this.text(name);
                        playing[0] = nx;
                        String json = SprauteRuntime.get().gui(this.script.deployed.project, "cutscene:" + nx);
                        if (json == null) {
                            throw new SprauteRuntimeException("Катсцена «" + nx + "» не найдена в проекте. Загрузите сюжет ещё раз (F5)");
                        } else {
                            ServerPlayer px = SprauteRuntime.player();
                            if (px != null) {
                                CutsceneDoc doc = CutsceneDoc.fromJson(json);
                                Map<String, Integer> looks = new HashMap<>();

                                for (CutsceneDoc.Key kx : doc.keys) {
                                    if (!kx.look.isEmpty() && !kx.look.equals("player") && !looks.containsKey(kx.look)) {
                                        Character<?> chx = this.script.characters.get(kx.look);
                                        Entity en = chx != null ? (Entity)this.script.optional(chx).orElse(null) : this.eval.nearestMob(kx.look);
                                        if (en != null) {
                                            looks.put(kx.look, en.getId());
                                        }
                                    }
                                }

                                Location atx = pos != null ? this.positionSafe(pos) : new Location(px);
                                this.script.cutscene = nx;
                                if (this.script.clips != null) {
                                    this.script.clips.ended();
                                }

                                this.script.clips = this.compileClips(nx, doc, player);
                                SprauteNetwork.sendToAll(new Packets.CameraPlay(this.script.getId(), nx, doc.clientJson(), atx.x, atx.y, atx.z, looks));
                                CutsceneDoc sceneDoc = CutsceneDoc.fromJson(json);
                                if (sceneDoc.post != null && !sceneDoc.post.isBlank()) {
                                    String lookx = Posts.json(this.script.deployed.project, sceneDoc.post.trim());
                                    if (lookx != null) {
                                        Posts.cutscene(lookx, (int)(sceneDoc.blendIn * 20.0F));
                                    }
                                }
                            }
                        }
                    }));
                    if (waitx) {
                        this.waitLog(b, "cutscene", () -> d -> playing[0].equals(d.get("id")));
                    }
                    break;
                }
                case "cutscene stop":
                    b.thenRun(() -> this.camera(op("stop")));
                    break;
                case "cutscene continue":
                    b.thenRun(() -> this.camera(op("continue")));
                    break;
                case "camera at": {
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr look = c.arg("look");
                    Ast.Expr yaw = c.arg("yaw");
                    Ast.Expr pitch = c.arg("pitch");
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op("at");
                        Location l = this.eval.eyes(pos);
                        o.addProperty("x", l.x);
                        o.addProperty("y", l.y);
                        o.addProperty("z", l.z);
                        if (yaw != null) {
                            o.addProperty("yaw", this.number(yaw, 0.0));
                        }

                        if (pitch != null) {
                            o.addProperty("pitch", this.number(pitch, 0.0));
                        }

                        if (look != null) {
                            this.lookTarget(o, look);
                        }

                        this.timing(o, c);
                        this.camera(o);
                    }));
                    break;
                }
                case "camera look": {
                    Ast.Expr target = c.arg("target");
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op("look");
                        this.lookTarget(o, target);
                        this.timing(o, c);
                        this.camera(o);
                    }));
                    break;
                }
                case "camera turn": {
                    Ast.Expr yaw = c.arg("yaw");
                    Ast.Expr pitch = c.arg("pitch");
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op("turn");
                        o.addProperty("yaw", Eval.num(this.eval.eval(yaw)));
                        o.addProperty("pitch", Eval.num(this.eval.eval(pitch)));
                        this.timing(o, c);
                        this.camera(o);
                    }));
                    break;
                }
                case "camera follow":
                case "camera orbit": {
                    Ast.Expr who = c.arg("who");
                    boolean follow = c.key.equals("camera follow");
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op(follow ? "follow" : "orbit");
                        Entity en = this.entityOf(who);
                        if (en != null) {
                            o.addProperty("entity", en.getId());
                        }

                        for (String k : new String[]{"distance", "height", "angle", "radius", "speed"}) {
                            Ast.Expr v = c.arg(k);
                            if (v != null) {
                                o.addProperty(k, this.number(v, 0.0));
                            }
                        }

                        this.camera(o);
                    }));
                    break;
                }
                case "camera rig": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr timeE = c.arg("time");
                    b.thenRun(this.guard(() -> {
                        String n = this.text(nameE).trim();
                        String json = Cams.json(this.script.deployed.project, n);
                        if (json == null) {
                            throw new SprauteRuntimeException("Камера «" + n + "» не найдена: её нет в папке «Камеры» проекта и среди готовых");
                        } else {
                            Cams.rig(this.script.getId(), json, timeE != null ? this.ticks(timeE) / 20.0 : 0.6);
                        }
                    }));
                    break;
                }
                case "camera rigset": {
                    Ast.Expr paramE = c.arg("param");
                    Ast.Expr valueE = c.arg("value");
                    Ast.Expr timeE = c.arg("time");
                    String param = paramE instanceof Ast.Ident id ? id.name : (paramE instanceof Ast.StringLit s ? s.value : "");
                    b.thenRun(this.guard(() -> Cams.set(this.script.getId(), param, this.number(valueE, 0.0), timeE != null ? this.ticks(timeE) / 20.0 : 0.3)));
                    break;
                }
                case "camera rigoff": {
                    Ast.Expr timeE = c.arg("time");
                    b.thenRun(this.guard(() -> Cams.off(this.script.getId(), timeE != null ? this.ticks(timeE) / 20.0 : 0.6)));
                    break;
                }
                case "camera reset":
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op("reset");
                        o.addProperty("time", c.arg("time") != null ? this.ticks(c.arg("time")) / 20.0 : 0.8);
                        this.camera(o);
                    }));
                    break;
                case "camera fov":
                case "camera roll":
                case "camera shake":
                case "camera bars":
                case "camera fade": {
                    Ast.Expr value = c.arg("value");
                    Ast.Expr color = c.arg("color");
                    String kind = c.key.substring(7);
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op(kind);
                        o.addProperty("value", Eval.num(this.eval.eval(value)));
                        if (color != null) {
                            o.addProperty("color", this.text(color));
                        }

                        this.timing(o, c);
                        this.camera(o);
                    }));
                    break;
                }
                case "slots": {
                    Ast.Expr countE = c.arg("count");
                    Ast.Expr offE = c.arg("offhand");
                    Ast.Expr orderE = c.arg("order");
                    Ast.Expr cellsE = c.arg("cells");
                    b.thenRun(this.guard(() -> {
                        MinecraftServer server = Platform.get().server();
                        if (server != null) {
                            EngineConfig cfg = EngineServer.config().copy();
                            cfg.slots = true;
                            cfg.openCells = (int)Math.max(0.0, Math.min(36.0, this.number(countE, 36.0)));
                            if (orderE != null) {
                                cfg.slotOrder = this.text(orderE).trim();
                            }

                            if (cellsE != null) {
                                cfg.closedCells = this.text(cellsE).trim();
                                cfg.slotOrder = "list";
                            }

                            if (offE != null) {
                                cfg.offhand = offE instanceof Ast.Ident oid ? oid.name : this.text(offE);
                            }

                            EngineServer.set(server, cfg);
                        }
                    }));
                    break;
                }
                case "slot icon":
                    Ast.Expr imageE = c.arg("image");
                    b.thenRun(this.guard(() -> {
                        MinecraftServer server = Platform.get().server();
                        if (server != null) {
                            EngineConfig cfg = EngineServer.config().copy();
                            cfg.lock.image = this.text(imageE).trim();
                            EngineServer.set(server, cfg);
                        }
                    }));
                    break;
                case "time task":
                    this.timeTask(b, c);
                    break;
                case "time taskstop": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> WorldClock.removeTask(this.text(idE))));
                    break;
                }
                case "camera carry": {
                    Ast.Expr value = c.arg("value");
                    b.thenRun(this.guard(() -> CameraState.carry(SprauteRuntime.player(), Eval.truthy(this.eval.eval(value)))));
                    break;
                }
                case "fight tune": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr[] vals = new Ast.Expr[]{c.arg("armor"), c.arg("speed"), c.arg("power"), c.arg("stagger"), c.arg("block"), c.arg("dodge")};
                    b.thenRun(this.guard(() -> {
                        Entity e = this.bossBody(who, "fight tune");
                        Double[] v = new Double[vals.length];

                        for (int i = 0; i < vals.length; i++) {
                            v[i] = vals[i] != null ? this.number(vals[i], 0.0) : null;
                        }

                        if (!Fights.tune(e, v[0], v[1], v[2], v[3], v[4], v[5])) {
                            throw new SprauteRuntimeException("fight tune: " + this.text(who) + " сейчас не дерётся (сначала fight)");
                        }
                    }));
                    break;
                }
                case "bossbar show": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr tE = c.arg("title");
                    Ast.Expr colE = c.arg("color");
                    Ast.Expr stE = c.arg("style");
                    b.thenRun(
                        this.guard(
                            () -> BossMoves.bar(
                                this.bossBody(who, "bossbar"),
                                tE != null ? this.safeText(tE) : null,
                                colE != null ? this.text(colE) : null,
                                stE != null ? this.text(stE) : null,
                                this.script.getId()
                            )
                        )
                    );
                    break;
                }
                case "bossbar hide": {
                    Ast.Expr who = c.arg("who");
                    b.thenRun(this.guard(() -> BossMoves.hideBar(this.bossBody(who, "bossbar"))));
                    break;
                }
                case "leap": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr hE = c.arg("height");
                    Ast.Expr tE = c.arg("time");
                    Ast.Expr imE = c.arg("impact");
                    Ast.Expr rE = c.arg("radius");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr pE = c.arg("push");
                    boolean nowait = c.arg("nowait") != null;
                    Entity[] body = new Entity[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                body[0] = this.bossBody(who, "leap");
                                int ticks = tE != null ? Math.max(4, this.ticksSafe(tE)) : 16;
                                BossMoves.leap(
                                    body[0],
                                    this.script.getId(),
                                    WorldApi.vec(this.eval.ref(toE)),
                                    hE != null ? this.number(hE, 3.0) : 3.0,
                                    ticks,
                                    this.bossFx(imE),
                                    rE != null ? this.number(rE, 0.0) : 0.0,
                                    (float)(dE != null ? this.number(dE, 4.0) : 4.0),
                                    pE != null ? this.number(pE, 1.2) : 1.2
                                );
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> !BossMoves.moving(body[0]));
                    }
                    break;
                }
                case "charge": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr sE = c.arg("speed");
                    Ast.Expr distE = c.arg("distance");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr pE = c.arg("push");
                    Ast.Expr trE = c.arg("trail");
                    boolean nowait = c.arg("nowait") != null;
                    Entity[] body = new Entity[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                body[0] = this.bossBody(who, "charge");
                                BossMoves.charge(
                                    body[0],
                                    this.script.getId(),
                                    WorldApi.vec(this.eval.ref(toE)),
                                    sE != null ? this.number(sE, 14.0) : 14.0,
                                    distE != null ? this.number(distE, 0.0) : 0.0,
                                    (float)(dE != null ? this.number(dE, 4.0) : 4.0),
                                    pE != null ? this.number(pE, 1.0) : 1.0,
                                    this.bossFx(trE)
                                );
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> !BossMoves.moving(body[0]));
                    }
                    break;
                }
                case "circle": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr cE = c.arg("center");
                    Ast.Expr rE = c.arg("radius");
                    Ast.Expr sE = c.arg("speed");
                    Ast.Expr tE = c.arg("time");
                    boolean nowait = c.arg("nowait") != null;
                    Entity[] body = new Entity[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                body[0] = this.bossBody(who, "circle");
                                Object center = this.eval.ref(cE);
                                Entity ce = center instanceof EntityRef r ? r.require() : null;
                                BossMoves.circle(
                                    body[0],
                                    this.script.getId(),
                                    ce,
                                    ce == null ? WorldApi.vec(center) : null,
                                    rE != null ? this.number(rE, 5.0) : 5.0,
                                    sE != null ? this.number(sE, 90.0) : 90.0,
                                    tE != null ? Math.max(1, this.ticksSafe(tE)) : 60
                                );
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> !BossMoves.moving(body[0]));
                    }
                    break;
                }
                case "part": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr nE = c.arg("name");
                    Ast.Expr onE = c.arg("enabled");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr aE = c.arg("attack");
                    b.thenRun(
                        this.guard(
                            () -> {
                                if (this.bossBody(who, "part") instanceof SprauteModelEntity me) {
                                    String var8x = this.text(nE);
                                    if (!me.partNames().contains(var8x)) {
                                        throw new SprauteRuntimeException(
                                            "part: у " + this.text(who) + " нет части «" + var8x + "». Есть: " + String.join(", ", me.partNames())
                                        );
                                    } else {
                                        if (onE != null) {
                                            me.setPartEnabled(var8x, Eval.truthy(this.eval.eval(onE)));
                                        }

                                        if (dE != null) {
                                            me.setPartDamage(var8x, (float)this.number(dE, 1.0));
                                        }

                                        if (aE != null) {
                                            me.setPartAttack(var8x, (float)this.number(aE, 0.0));
                                        }
                                    }
                                } else {
                                    throw new SprauteRuntimeException("part: хитбоксы-части есть только у сущностей spraute:… (редактор сущности)");
                                }
                            }
                        )
                    );
                    break;
                }
                case "attack": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr partE = c.arg("part");
                    Ast.Expr wE = c.arg("w");
                    Ast.Expr hE = c.arg("h");
                    Ast.Expr dpE = c.arg("d");
                    Ast.Expr rE = c.arg("right");
                    Ast.Expr uE = c.arg("up");
                    Ast.Expr fE = c.arg("forward");
                    Ast.Expr dlE = c.arg("delay");
                    Ast.Expr tE = c.arg("time");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr pE = c.arg("push");
                    Ast.Expr imE = c.arg("impact");
                    Ast.Expr exE = c.arg("except");
                    boolean show = c.arg("show") != null;
                    boolean nowait = c.arg("nowait") != null;
                    Entity[] body = new Entity[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                body[0] = this.bossBody(who, "attack");
                                Entity ex = exE != null && this.eval.ref(exE) instanceof EntityRef xr ? xr.require() : null;
                                AttackBoxes.attack(
                                    body[0],
                                    partE != null ? this.text(partE) : null,
                                    new Vector3f((float)this.number(wE, 2.0), (float)this.number(hE, 2.0), (float)this.number(dpE, 2.0)),
                                    new Vector3f((float)this.number(rE, 0.0), (float)this.number(uE, 1.0), (float)this.number(fE, 1.5)),
                                    (float)(dE != null ? this.number(dE, 4.0) : 4.0),
                                    pE != null ? this.number(pE, 0.5) : 0.5,
                                    dlE != null ? this.ticksSafe(dlE) : 0,
                                    tE != null ? this.ticksSafe(tE) : 6,
                                    ex,
                                    this.bossFx(imE),
                                    show,
                                    this.script.getId()
                                );
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> body[0] == null || !AttackBoxes.active(body[0]));
                    }
                    break;
                }
                case "stopmove": {
                    Ast.Expr who = c.arg("who");
                    b.thenRun(this.guard(() -> {
                        Entity e = this.bossBody(who, "stopmove");
                        BossMoves.stop(e);
                        AttackBoxes.stop(e);
                    }));
                    break;
                }
                case "strike": {
                    Ast.Expr atE = c.arg("pos");
                    Ast.Expr rE = c.arg("radius");
                    Ast.Expr dlE = c.arg("delay");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr pE = c.arg("push");
                    Ast.Expr wE = c.arg("warn");
                    Ast.Expr imE = c.arg("impact");
                    Ast.Expr byE = c.arg("by");
                    Ast.Expr exE = c.arg("except");
                    b.thenRun(
                        this.guard(
                            () -> {
                                Entity by = byE != null && this.eval.ref(byE) instanceof EntityRef br ? br.require() : null;
                                Entity ex = exE != null && this.eval.ref(exE) instanceof EntityRef xr ? xr.require() : null;
                                BossMoves.strike(
                                    WorldApi.level(),
                                    WorldApi.vec(this.eval.ref(atE)),
                                    rE != null ? this.number(rE, 2.5) : 2.5,
                                    dlE != null ? this.ticksSafe(dlE) : 20,
                                    (float)(dE != null ? this.number(dE, 4.0) : 4.0),
                                    pE != null ? this.number(pE, 0.8) : 0.8,
                                    this.bossFx(wE),
                                    this.bossFx(imE),
                                    by,
                                    ex,
                                    this.script.getId()
                                );
                            }
                        )
                    );
                    break;
                }
                case "beam": {
                    Ast.Expr fromE = c.arg("from");
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr tE = c.arg("time");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr rE = c.arg("range");
                    Ast.Expr wE = c.arg("width");
                    Ast.Expr trE = c.arg("track");
                    Ast.Expr paE = c.arg("particle");
                    Ast.Expr imE = c.arg("impact");
                    boolean wait = c.arg("wait") != null;
                    Entity[] body = new Entity[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                body[0] = this.bossBody(fromE, "beam");
                                Object tox = this.eval.ref(toE);
                                Entity te = tox instanceof EntityRef r ? r.require() : null;
                                BossMoves.beam(
                                    body[0],
                                    te,
                                    te == null ? WorldApi.vec(tox) : null,
                                    tE != null ? this.ticksSafe(tE) : 40,
                                    (float)(dE != null ? this.number(dE, 6.0) : 6.0),
                                    rE != null ? this.number(rE, 24.0) : 24.0,
                                    wE != null ? this.number(wE, 0.3) : 0.3,
                                    trE != null ? this.number(trE, 0.0) : 0.0,
                                    paE != null ? this.text(paE) : null,
                                    this.bossFx(imE),
                                    this.script.getId()
                                );
                            }
                        )
                    );
                    if (wait) {
                        b.thenWaitUntil(() -> body[0] == null || !BossMoves.beaming(body[0]));
                    }
                    break;
                }
                case "aoe": {
                    Ast.Expr atE = c.arg("pos");
                    Ast.Expr rE = c.arg("radius");
                    Ast.Expr coneE = c.arg("cone");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr pushE = c.arg("push");
                    Ast.Expr pullE = c.arg("pull");
                    Ast.Expr fireE = c.arg("fire");
                    Ast.Expr effE = c.arg("effect");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr lvlE = c.arg("level");
                    Ast.Expr healE = c.arg("heal");
                    Ast.Expr byE = c.arg("by");
                    Ast.Expr onlyE = c.arg("only");
                    Ast.Expr exE = c.arg("except");
                    boolean allies = c.arg("allies") != null;
                    boolean everyone = c.arg("everyone") != null;
                    boolean self = c.arg("self") != null;
                    b.thenRun(
                        this.guard(
                            () -> {
                                Object atx = this.eval.ref(atE);
                                Vec3 posx = atx instanceof EntityRef er ? er.require().position() : WorldApi.vec(atx);
                                Entity by = (Entity)(byE != null && this.eval.ref(byE) instanceof EntityRef br ? br.require() : SprauteRuntime.player());
                                MobEffect effect = null;
                                if (effE != null) {
                                    ResourceLocation rl = ResourceLocation.tryParse(this.text(effE));
                                    effect = rl != null ? Regs.MOB_EFFECT.get(rl) : null;
                                    if (effect == null) {
                                        throw new SprauteRuntimeException("aoe: эффект " + this.text(effE) + " не найден");
                                    }
                                }

                                Mechanics.aoe(
                                    WorldApi.level(),
                                    posx,
                                    new Mechanics.Area(
                                        rE != null ? this.number(rE, 5.0) : 5.0,
                                        coneE != null ? this.number(coneE, 0.0) : 0.0,
                                        (float)(dE != null ? this.number(dE, 0.0) : 0.0),
                                        pushE != null ? this.number(pushE, 0.0) : 0.0,
                                        pullE != null ? this.number(pullE, 0.0) : 0.0,
                                        fireE != null ? this.ticks(fireE) / 20 : 0,
                                        effect,
                                        timeE != null ? this.ticks(timeE) : 100,
                                        lvlE != null ? (int)this.number(lvlE, 1.0) : 1,
                                        (float)(healE != null ? this.number(healE, 0.0) : 0.0),
                                        by,
                                        exE != null && this.eval.ref(exE) instanceof EntityRef xr ? xr.require() : null,
                                        onlyE != null ? this.text(onlyE) : "",
                                        allies,
                                        everyone,
                                        self
                                    )
                                );
                            }
                        )
                    );
                    break;
                }
                case "motion add":
                case "motion set":
                case "motion jump":
                case "motion forward":
                case "motion toward":
                case "motion stop": {
                    Ast.Expr whoE = c.arg("who");
                    Ast.Expr vecE = c.arg("vec");
                    Ast.Expr powerE = c.arg("power");
                    Ast.Expr upE = c.arg("up");
                    Ast.Expr toE = c.arg("to");
                    boolean safe = c.arg("safe") != null;
                    String kind = c.key;
                    b.thenRun(
                        this.guard(
                            () -> {
                                if (this.eval.ref(whoE) instanceof EntityRef whox && whox.get() != null) {
                                    Entity e = whox.get();
                                    Vec3 now = e.getDeltaMovement();
                                    double power = powerE != null ? this.number(powerE, 0.0) : 0.0;
                                    double up = upE != null ? this.number(upE, 0.0) : 0.0;

                                    Vec3 next = switch (kind) {
                                        case "motion add" -> now.add(WorldApi.vec(this.eval.ref(vecE)));
                                        case "motion set" -> WorldApi.vec(this.eval.ref(vecE));
                                        case "motion jump" -> new Vec3(now.x, power, now.z);
                                        case "motion forward" -> now.add(Mechanics.flat(e.getLookAngle()).scale(power)).add(0.0, up, 0.0);
                                        case "motion toward" -> now.add(Mechanics.flat(WorldApi.vec(this.eval.ref(toE)).subtract(e.position())).scale(power))
                                            .add(0.0, up, 0.0);
                                        default -> Vec3.ZERO;
                                    };
                                    Mechanics.motion(e, next, safe);
                                }
                            }
                        )
                    );
                    break;
                }
                case "ignite": {
                    Ast.Expr whoE = c.arg("who");
                    Ast.Expr timeE = c.arg("time");
                    b.thenRun(this.guard(() -> {
                        if (this.eval.ref(whoE) instanceof EntityRef whox && whox.get() != null) {
                            whox.get().setSecondsOnFire(this.ticks(timeE) / 20);
                        }
                    }));
                    break;
                }
                case "breakblock": {
                    Ast.Expr posE = c.arg("pos");
                    boolean drops = c.arg("nodrops") == null;
                    boolean wear = c.arg("nowear") == null;
                    boolean force = c.arg("force") != null;
                    b.thenRun(this.guard(() -> {
                        ServerPlayer px = SprauteRuntime.player();
                        if (px != null) {
                            Mechanics.breakBlock(px, WorldApi.vec(this.eval.ref(posE)), drops, wear, force);
                        }
                    }));
                    break;
                }
                case "shockwave": {
                    Ast.Expr atE = c.arg("pos");
                    Ast.Expr rE = c.arg("radius");
                    Ast.Expr pE = c.arg("power");
                    Ast.Expr dE = c.arg("damage");
                    Ast.Expr exE = c.arg("except");
                    b.thenRun(
                        this.guard(
                            () -> {
                                Object atx = this.eval.ref(atE);
                                Entity center = atx instanceof EntityRef er ? er.require() : null;
                                Vec3 posx = center != null ? center.position() : WorldApi.vec(atx);
                                Entity except = exE != null && this.eval.ref(exE) instanceof EntityRef xr ? xr.require() : center;
                                Mechanics.shockwave(
                                    WorldApi.level(),
                                    posx,
                                    rE != null ? this.number(rE, 6.0) : 6.0,
                                    pE != null ? this.number(pE, 1.2) : 1.2,
                                    (float)(dE != null ? this.number(dE, 0.0) : 0.0),
                                    except,
                                    center
                                );
                            }
                        )
                    );
                    break;
                }
                case "explode": {
                    Ast.Expr atE = c.arg("pos");
                    Ast.Expr pE = c.arg("power");
                    boolean fire = c.arg("fire") != null;
                    boolean breaks = c.arg("break") != null;
                    b.thenRun(
                        this.guard(
                            () -> Mechanics.explode(
                                WorldApi.level(), WorldApi.vec(this.eval.ref(atE)), (float)(pE != null ? this.number(pE, 3.0) : 3.0), fire, breaks, null
                            )
                        )
                    );
                    break;
                }
                case "lightning": {
                    Ast.Expr atE = c.arg("pos");
                    boolean harmless = c.arg("harmless") != null;
                    b.thenRun(this.guard(() -> Mechanics.lightning(WorldApi.level(), WorldApi.vec(this.eval.ref(atE)), harmless)));
                    break;
                }
                case "bar set": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr vE = c.arg("value");
                    Ast.Expr mE = c.arg("max");
                    Ast.Expr tE = c.arg("title");
                    Ast.Expr colE = c.arg("color");
                    b.thenRun(
                        this.guard(
                            () -> Mechanics.bar(
                                this.text(idE),
                                this.number(vE, 0.0),
                                mE != null ? this.number(mE, 100.0) : 100.0,
                                tE != null ? this.text(tE) : null,
                                colE != null ? this.text(colE) : null
                            )
                        )
                    );
                    break;
                }
                case "bar hide": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> Mechanics.hideBar(this.text(idE))));
                    break;
                }
                case "notify": {
                    Ast.Expr tE = c.arg("text");
                    Ast.Expr iconE = c.arg("icon");
                    Ast.Expr itemE = c.arg("item");
                    boolean silent = c.arg("silent") != null;
                    b.thenRun(
                        this.guard(
                            () -> {
                                ServerPlayer px = SprauteRuntime.player();
                                if (px != null) {
                                    SprauteNetwork.sendTo(
                                        px,
                                        new Packets.Notify(
                                            this.safeText(tE),
                                            iconE != null ? this.text(iconE) : "",
                                            itemE != null ? this.text(itemE) : "",
                                            this.script.deployed.project == null ? "" : this.script.deployed.project,
                                            !silent
                                        )
                                    );
                                }
                            }
                        )
                    );
                    break;
                }
                case "actionbar": {
                    Ast.Expr tE = c.arg("text");
                    b.thenRun(this.guard(() -> Mechanics.actionbar(this.safeText(tE))));
                    break;
                }
                case "cast": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr fromE = c.arg("from");
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr speedE = c.arg("speed");
                    Ast.Expr dmgE = c.arg("damage");
                    Ast.Expr rangeE = c.arg("range");
                    Ast.Expr gravE = c.arg("gravity");
                    Ast.Expr sizeE = c.arg("size");
                    Ast.Expr pushE = c.arg("push");
                    Ast.Expr impactE = c.arg("impact");
                    Ast.Expr scaleE = c.arg("scale");
                    Ast.Expr idE = c.arg("id");
                    boolean pierce = c.arg("pierce") != null;
                    boolean ring = c.arg("ring") != null;
                    Ast.Expr countE = c.arg("count");
                    Ast.Expr spreadE = c.arg("spread");
                    b.thenRun(this.guard(() -> {
                        String nx = this.text(nameE).trim();
                        int countx = (int)Math.max(1.0, Math.min(64.0, countE != null ? this.number(countE, 1.0) : (ring ? 8.0 : 1.0)));
                        double spreadx = spreadE != null ? this.number(spreadE, 30.0) : 30.0;

                        for (int shot = 0; shot < countx; shot++) {
                            Casts.Cast cast = new Casts.Cast();
                            cast.fx = nx;
                            cast.id = idE != null ? this.text(idE) : nx;
                            cast.project = this.script.deployed.project;
                            cast.scriptId = this.script.getId();
                            Object from;
                            if (fromE != null) {
                                from = this.eval.ref(fromE);
                            } else {
                                ServerPlayer pl = SprauteRuntime.player();
                                if (pl == null) {
                                    throw new SprauteRuntimeException("cast: игрока нет в мире");
                                }

                                from = new EntityRef(pl);
                            }

                            Entity owner = from instanceof EntityRef er ? er.require() : null;
                            Vec3 dir;
                            Vec3 startx;
                            if (owner != null) {
                                Vec3 lookx = owner.getViewVector(1.0F);
                                startx = owner.getEyePosition().add(lookx.scale(0.8)).add(0.0, -0.25, 0.0);
                                dir = lookx;
                            } else {
                                startx = WorldApi.vec(from);
                                dir = new Vec3(0.0, 0.0, 1.0);
                            }

                            if (toE != null) {
                                Object tox = this.eval.ref(toE);
                                Vec3 aim = tox instanceof EntityRef tr ? tr.require().getBoundingBox().getCenter() : WorldApi.vec(tox);
                                dir = aim.subtract(startx);
                            }

                            if (dir.lengthSqr() < 1.0E-6) {
                                dir = new Vec3(0.0, 0.0, 1.0);
                            }

                            if (ring) {
                                dir = new Vec3(dir.x, 0.0, dir.z).yRot((float)((Math.PI * 2) * shot / countx));
                            } else if (countx > 1) {
                                dir = dir.yRot((float)Math.toRadians(-spreadx / 2.0 + spreadx * shot / (countx - 1)));
                            }

                            if (dir.lengthSqr() < 1.0E-6) {
                                dir = new Vec3(0.0, 0.0, 1.0);
                            }

                            double speedx = Math.max(0.5, speedE != null ? this.number(speedE, 20.0) : 20.0) / 20.0;
                            cast.pos = startx;
                            cast.vel = dir.normalize().scale(speedx);
                            cast.owner = owner;
                            cast.level = owner != null && Mc.level(owner) instanceof ServerLevel sl ? sl : WorldApi.level();
                            cast.damage = (float)(dmgE != null ? this.number(dmgE, 4.0) : 4.0);
                            cast.life = (int)Math.ceil(Math.max(1.0, rangeE != null ? this.number(rangeE, 48.0) : 48.0) / speedx);
                            cast.gravity = gravE != null ? this.number(gravE, 0.0) * 0.04 : 0.0;
                            cast.radius = sizeE != null ? Math.max(0.05, this.number(sizeE, 0.4)) : 0.4;
                            cast.knockback = (float)(pushE != null ? this.number(pushE, 0.4) : 0.4);
                            cast.scale = scaleE != null ? (float)this.number(scaleE, 1.0) : 1.0F;
                            cast.pierce = pierce;
                            if (impactE != null) {
                                cast.impactJson = this.fxJson(this.text(impactE).trim());
                            }

                            Casts.launch(cast, this.fxJson(nx));
                        }
                    }));
                    break;
                }
                case "fx play": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr scaleE = c.arg("scale");
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr boneE = c.arg("bone");
                    boolean follow = c.arg("follow") != null;
                    b.thenRun(
                        this.guard(
                            () -> {
                                String nx = this.text(nameE).trim();
                                String json = SprauteRuntime.get().gui(this.script.deployed.project, "fx:" + nx);
                                if (json == null) {
                                    FxDef preset = FxPresets.get(nx);
                                    if (preset == null) {
                                        throw new SprauteRuntimeException(
                                            "Эффект «"
                                                + nx
                                                + "» не найден: его нет в папке «Эффекты» проекта и среди встроенных ("
                                                + String.join(", ", FxPresets.names())
                                                + ")"
                                        );
                                    }

                                    json = preset.toJson();
                                }

                                Location atx = this.positionSafe(pos);
                                Entity en = follow ? this.entityOf(pos) : null;
                                float sc = scaleE != null ? (float)this.number(scaleE, 1.0) : 1.0F;
                                String idx = idE != null ? this.text(idE) : "";
                                String bone = boneE != null ? this.text(boneE).trim() : "";
                                double fxX = atx.x;
                                double fxY = atx.y;
                                double fxZ = atx.z;
                                if (!bone.isEmpty()) {
                                    Entity carrier = this.entityOf(pos);
                                    if (carrier == null) {
                                        throw new SprauteRuntimeException("fx play … bone: цель должна быть персонажем с моделью");
                                    }

                                    Vec3 point = BoneWorld.position(carrier, bone, 0.0, 0.0, 0.0);
                                    fxX = point.x;
                                    fxY = point.y;
                                    fxZ = point.z;
                                }

                                SprauteNetwork.sendToAll(
                                    new Packets.FxPlay(
                                        idx,
                                        this.script.deployed.project,
                                        json,
                                        fxX,
                                        fxY,
                                        fxZ,
                                        en != null ? en.getId() : -1,
                                        sc,
                                        this.script.getId(),
                                        en != null ? bone : ""
                                    )
                                );
                            }
                        )
                    );
                    break;
                }
                case "skin":
                    this.skin(b, c.arg("who"), c.arg("skin"));
                    break;
                case "namecolor": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr colE = c.arg("color");
                    String key;
                    if (who instanceof Ast.Ident id && this.script.characters.containsKey(id.name)) {
                        key = id.name;
                    } else {
                        if (!(who instanceof Ast.Ident idx) || !idx.name.equals("player")) {
                            throw new SprauteRuntimeException("namecolor: нужен персонаж или player");
                        }

                        key = "player";
                    }

                    if (!(colE instanceof Ast.StringLit sl)) {
                        throw new SprauteRuntimeException("namecolor: цвет в кавычках, например \"#FF8800\" или \"gold\"");
                    }

                    Integer color = Colors.parse(sl.value);
                    b.thenRun(() -> {
                        if (color != null) {
                            this.script.nameColors.put(key, color);
                        } else {
                            this.script.nameColors.remove(key);
                        }
                    });
                    Integer slotxx = this.speakerSlots.get(key.equals("player") ? "player" : "char:" + key);
                    if (slotxx != null) {
                        if (key.equals("player")) {
                            this.speechPlayer(b, slotxx);
                        } else {
                            this.speechEntity(b, slotxx, key);
                        }
                    }
                    break;
                }
                case "shop open": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr who = c.arg("who");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            String n = this.text(nameE);
                            if (!Shops.open(p, this.script.getId(), this.script.deployed.project, n, who != null ? this.entityOf(who) : null)) {
                                throw new SprauteRuntimeException("Магазин «" + n + "» не найден в папке «Магазины» проекта");
                            }
                        }
                    }));
                    break;
                }
                case "shop restock": {
                    Ast.Expr nameE = c.arg("name");
                    b.thenRun(this.guard(() -> Shops.restock(this.script.deployed.project, this.text(nameE))));
                    break;
                }
                case "shop stock": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr offerE = c.arg("offer");
                    Ast.Expr value = c.arg("value");
                    b.thenRun(this.guard(() -> Shops.setStock(this.script.deployed.project, this.text(nameE), this.text(offerE), (int)this.number(value, 0.0))));
                    break;
                }
                case "shop close":
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            SprauteNetwork.sendTo(p, new Packets.ShopOpen("", false));
                        }
                    }));
                    break;
                case "hitbox": {
                    Ast.Expr who = c.arg("who");
                    List<Ast.Expr> modes = new ArrayList<>();

                    for (Ast.Arg a : c.args) {
                        if ("modes".equals(a.param)) {
                            modes.add(a.value);
                        }
                    }

                    b.thenRun(this.guard(() -> {
                        if (!(this.eval.ref(who) instanceof EntityRef er)) {
                            throw new SprauteRuntimeException("hitbox: нужен персонаж, игрок или существо");
                        } else {
                            Entity e = er.require();
                            ArrayList words = new ArrayList();

                            for (Ast.Expr m : modes) {
                                words.add(this.text(m));
                            }

                            try {
                                Hitboxes.set(e, Hitboxes.parse(Hitboxes.flags(e), words));
                            } catch (IllegalArgumentException var9x) {
                                throw new SprauteRuntimeException(var9x.getMessage());
                            }
                        }
                    }));
                    break;
                }
                case "skills open":
                case "skills level":
                case "skills xp":
                case "skills points":
                case "skills learn":
                case "skills forget":
                case "skills reset":
                case "skills enable":
                case "skills disable": {
                    Ast.Expr treeE = c.arg("tree");
                    Ast.Expr amountE = c.arg("amount");
                    Ast.Expr nodeE = c.arg("node");
                    Ast.Expr currencyE = c.arg("currency");
                    Ast.Expr menuE = c.arg("menu");
                    String kind = c.key;
                    b.thenRun(
                        this.guard(
                            () -> {
                                ServerPlayer p = SprauteRuntime.player();
                                if (p != null) {
                                    String n = this.text(treeE);
                                    Skills.Tree tree = Skills.tree(this.script.deployed.project, n);
                                    if (tree == null) {
                                        throw new SprauteRuntimeException(
                                            "Система прокачки «" + n + "» не найдена в папке «Прокачка». Загрузите сюжет ещё раз (F5)"
                                        );
                                    } else {
                                        SkillTree.Node node = null;
                                        if (nodeE != null) {
                                            node = tree.def().node(this.text(nodeE));
                                            if (node == null) {
                                                throw new SprauteRuntimeException("В системе «" + n + "» нет навыка «" + this.text(nodeE) + "»");
                                            }
                                        }

                                        switch (kind) {
                                            case "skills open":
                                                Skills.open(p, tree, menuE != null ? this.text(menuE) : "");
                                                break;
                                            case "skills level":
                                                Skills.setLevel(p, tree, (int)Math.round(this.number(amountE, 1.0)));
                                                break;
                                            case "skills xp":
                                                Skills.xp(p, tree, Math.round(this.number(amountE, 0.0)));
                                                break;
                                            case "skills points":
                                                Skills.points(
                                                    p,
                                                    tree,
                                                    currencyE != null ? this.text(currencyE) : tree.def().currencies.get(0).id,
                                                    (int)Math.round(this.number(amountE, 0.0))
                                                );
                                                break;
                                            case "skills learn":
                                                Skills.grant(p, tree, node);
                                                break;
                                            case "skills forget":
                                                Skills.forget(p, tree, node);
                                                break;
                                            case "skills reset":
                                                Skills.reset(p, tree);
                                                break;
                                            default:
                                                Skills.setOff(p, tree, kind.equals("skills disable"));
                                        }
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "skills close":
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            Skills.close(p);
                        }
                    }));
                    break;
                case "stealth start":
                    Ast.Expr backE = c.arg("back");
                    Ast.Expr restoreE = c.arg("restore");
                    b.thenRun(
                        this.guard(
                            () -> {
                                try {
                                    Stealth.start(
                                        this.script.getId(),
                                        backE != null ? this.positionSafe(backE) : null,
                                        StealthShape.restores(restoreE != null ? this.text(restoreE) : "")
                                    );
                                } catch (IllegalArgumentException var4) {
                                    throw new SprauteRuntimeException(var4.getMessage());
                                }
                            }
                        )
                    );
                    break;
                case "stealth guard": {
                    Character ch = this.character(c.arg("who"));
                    Ast.Expr styleE = c.arg("style");
                    Ast.Expr patrolE = c.arg("patrol");
                    Ast.Expr speedE = c.arg("speed");
                    Ast.Expr shapeE = c.arg("shape");
                    Ast.Expr rangeE = c.arg("range");
                    Ast.Expr angleE = c.arg("angle");
                    Ast.Expr widthE = c.arg("width");
                    Ast.Expr heightE = c.arg("height");
                    Ast.Expr sneakE = c.arg("sneak");
                    Ast.Expr noticeE = c.arg("notice");
                    Ast.Expr colorE = c.arg("color");
                    boolean through = c.arg("through") != null;
                    boolean ping = c.arg("pingpong") != null;
                    if (patrolE != null) {
                        b.thenClearMovement(ch);
                    }

                    b.thenRun(
                        this.guard(
                            () -> {
                                Entity e = this.script.<Entity>optional(ch).orElse(null);
                                if (e == null) {
                                    throw new SprauteRuntimeException("Персонажа нет в мире — сначала spawn");
                                } else {
                                    StealthDef style = styleE != null ? Stealth.style(this.script.deployed.project, this.text(styleE)) : null;
                                    if (styleE != null && style == null) {
                                        throw new SprauteRuntimeException(
                                            "Стиль стелса «"
                                                + this.text(styleE)
                                                + "» не найден: его нет в папке «Стили стелса» и среди готовых ("
                                                + String.join(", ", StealthDef.PRESET_TITLES.keySet())
                                                + ")"
                                        );
                                    } else {
                                        Stealth.Look lookx = style != null ? Stealth.look(style) : new Stealth.Look();
                                        if (style != null) {
                                            Stealth.begin(this.script.getId(), style);
                                        }

                                        if (shapeE != null) {
                                            lookx.shape = this.text(shapeE);
                                        }

                                        lookx.range = Math.max(0.5, this.number(rangeE, lookx.range));
                                        lookx.angle = Math.max(1.0, Math.min(360.0, this.number(angleE, lookx.angle)));
                                        lookx.width = Math.max(0.5, this.number(widthE, lookx.width));
                                        lookx.height = Math.max(0.1, this.number(heightE, lookx.height));
                                        lookx.sneak = Math.max(0.0, this.number(sneakE, lookx.sneak));
                                        if (noticeE != null) {
                                            lookx.notice = this.ticks(noticeE);
                                        }

                                        lookx.through = lookx.through || through;
                                        if (colorE != null) {
                                            try {
                                                lookx.color = Integer.parseInt(this.text(colorE).trim().replace("#", ""), 16) & 16777215;
                                            } catch (NumberFormatException var21x) {
                                                throw new SprauteRuntimeException("Цвет стража — как \"#FF3030\", а не «" + this.text(colorE) + "»");
                                            }
                                        }

                                        String nx = patrolE != null ? this.text(patrolE) : "";
                                        PathDef d = patrolE != null ? this.pathNamed(nx) : null;
                                        String mode = ping ? "pingpong" : (d != null && d.mode.equals("once") ? "loop" : "");
                                        Stealth.guard(
                                            this.script.getId(),
                                            e,
                                            lookx,
                                            nx,
                                            d,
                                            speedE != null ? this.number(speedE, 0.0) : (style != null ? style.speed : 0.0),
                                            mode
                                        );
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "stealth remove": {
                    Character ch = this.character(c.arg("who"));
                    b.thenRun(this.guard(() -> {
                        Entity e = this.script.<Entity>optional(ch).orElse(null);
                        if (e != null) {
                            Stealth.remove(e);
                        }
                    }));
                    break;
                }
                case "stealth stop":
                    b.thenRun(this.guard(() -> Stealth.stop(this.script.getId())));
                    break;
                case "fight":
                case "fight stop":
                case "fight heal": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr styleE = c.arg("style");
                    Ast.Expr targetE = c.arg("target");
                    Ast.Expr value = c.arg("value");
                    String name = who instanceof Ast.Ident id ? id.name : "?";
                    Character ch = this.character(who);
                    String kind = c.key;
                    b.thenRun(
                        this.guard(
                            () -> {
                                Entity e = this.script.<Entity>optional(ch).orElse(null);
                                if (e == null) {
                                    throw new SprauteRuntimeException("Персонаж " + name + " сейчас не в мире");
                                } else {
                                    switch (kind) {
                                        case "fight stop":
                                            Fights.stop(e);
                                            break;
                                        case "fight heal":
                                            Fights.heal(e, value != null ? (float)this.number(value, -1.0) : -1.0F);
                                            break;
                                        default:
                                            String st = styleE != null ? this.text(styleE) : "";
                                            FightDef def = Fights.style(this.script.deployed.project, st);
                                            if (def == null) {
                                                throw new SprauteRuntimeException(
                                                    "Стиль боя «" + st + "» не найден: его нет в папке «Стили боя» и среди готовых (fists, sword, brute, weak)"
                                                );
                                            }

                                            Entity tx = null;
                                            if (targetE != null) {
                                                if (!(this.eval.ref(targetE) instanceof EntityRef er)) {
                                                    throw new SprauteRuntimeException("Цель боя — игрок, персонаж или существо");
                                                }

                                                tx = er.require();
                                            }

                                            try {
                                                Fights.start(e, name, this.script.getId(), def, tx);
                                            } catch (IllegalArgumentException var15x) {
                                                throw new SprauteRuntimeException(var15x.getMessage());
                                            }
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "wait defeated": {
                    Character ch = this.character(c.arg("who"));
                    UUID[] seen = new UUID[1];
                    b.thenWaitUntil(() -> {
                        Entity e = this.script.<Entity>optional(ch).orElse(null);
                        if (e != null) {
                            seen[0] = e.getUUID();
                            return !e.isAlive() || Fights.defeated(e);
                        } else {
                            return seen[0] != null && Fights.gone(seen[0]);
                        }
                    });
                    break;
                }
                case "zone on":
                case "zone off":
                case "zone wall": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr modeE = c.arg("mode");
                    String kind = c.key.substring(5);
                    b.thenRun(this.guard(() -> {
                        String n = this.text(nameE);
                        String proj = this.script.deployed.project;
                        boolean ok = kind.equals("wall") ? Zones.setWall(proj, n, this.text(modeE)) : Zones.setEnabled(proj, n, kind.equals("on"));
                        if (!ok) {
                            throw new SprauteRuntimeException("Зона «" + n + "» не найдена в папке «Зоны» проекта");
                        }
                    }));
                    break;
                }
                case "wait zone":
                case "wait outside": {
                    Ast.Expr nameE = c.arg("name");
                    boolean in = c.key.equals("wait zone");
                    String[] n = new String[]{null};
                    b.thenRun(this.guard(() -> {
                        n[0] = this.text(nameE);
                        Zones.require(this.script.deployed.project, n[0]);
                    }));
                    b.thenWaitUntil(() -> n[0] != null && Zones.playerIn(SprauteRuntime.player(), this.script.deployed.project, n[0]) == in);
                    break;
                }
                case "quest start":
                case "quest progress":
                case "quest set":
                case "quest reveal":
                case "quest done":
                case "quest fail":
                case "quest reset":
                case "quest track": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr taskE = c.arg("task");
                    Ast.Expr value = c.arg("value");
                    String kind = c.key.substring(6);
                    b.thenRun(
                        this.guard(
                            () -> {
                                ServerPlayer p = SprauteRuntime.player();
                                if (p != null) {
                                    String asked = this.text(nameE);
                                    String[] rq = Quests.resolve(this.script.deployed.project, asked);
                                    String nx = rq[1];
                                    String proj = rq[0];

                                    boolean ok;
                                    try {
                                        ok = switch (kind) {
                                            case "start" -> Quests.start(p, proj, nx);
                                            case "progress" -> Quests.progress(
                                                p, proj, nx, this.text(taskE), value != null ? (int)Math.round(this.number(value, 1.0)) : 1, false
                                            );
                                            case "set" -> Quests.progress(p, proj, nx, this.text(taskE), (int)Math.round(this.number(value, 0.0)), true);
                                            case "reveal" -> Quests.reveal(p, proj, nx, this.text(taskE));
                                            case "done" -> Quests.end(p, proj, nx, true);
                                            case "fail" -> Quests.end(p, proj, nx, false);
                                            case "reset" -> {
                                                Quests.reset(p, proj, nx);
                                                yield true;
                                            }
                                            default -> {
                                                Quests.track(p, Quests.key(proj, nx));
                                                yield true;
                                            }
                                        };
                                    } catch (IllegalArgumentException var13x) {
                                        throw new SprauteRuntimeException(var13x.getMessage());
                                    }

                                    if (!ok) {
                                        List<String> have = Quests.names(this.script.deployed.project);
                                        String pn = this.script.deployed.project != null && !this.script.deployed.project.isEmpty()
                                            ? " «" + this.script.deployed.project + "»"
                                            : "";
                                        throw new SprauteRuntimeException(
                                            "Квест «"
                                                + asked
                                                + "» не найден в папке «Квесты» проекта"
                                                + pn
                                                + ". "
                                                + (
                                                    have.isEmpty()
                                                        ? "Там нет квестов (или проект не отправлен в мир — сохраните квест или запустите сюжет из редактора)."
                                                        : "Есть: " + String.join(", ", have) + " (пишите имя файла квеста)."
                                                )
                                        );
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "wait goal": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr taskE = c.arg("task");
                    if (this.handlerWait) {
                        this.waitLog(b, "questtask", () -> {
                            String quest = Quests.resolve(this.script.deployed.project, this.text(nameE))[1];
                            String task = this.text(taskE);
                            return d -> quest.equals(d.get("quest")) && task.equals(d.get("task"));
                        });
                    } else {
                        String[] goal = new String[3];
                        b.thenRun(this.guard(() -> {
                            String[] rq = Quests.resolve(this.script.deployed.project, this.text(nameE));
                            goal[0] = rq[0];
                            goal[1] = rq[1];
                            goal[2] = this.text(taskE);
                            String problem = Quests.goalProblem(goal[0], goal[1], goal[2]);
                            if (problem != null) {
                                throw new SprauteRuntimeException(problem);
                            }
                        }));
                        b.thenWaitUntil(() -> {
                            if (!Quests.goalDone(SprauteRuntime.player(), goal[0], goal[1], goal[2])) {
                                return false;
                            } else {
                                Map<String, Object> data = new HashMap<>();
                                data.put("quest", goal[1]);
                                data.put("task", goal[2]);
                                this.script.event = data;
                                return true;
                            }
                        });
                    }
                    break;
                }
                case "quest journal":
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            Quests.sync(p, true);
                        }
                    }));
                    break;
                case "time set":
                case "time add":
                case "time speed":
                case "time freeze":
                case "time unfreeze": {
                    Ast.Expr value = c.arg("value");
                    String kind = c.key.substring(5);
                    b.thenRun(this.guard(() -> {
                        MinecraftServer server = Platform.get().server();
                        if (server != null) {
                            if (EngineServer.config().time) {
                                switch (kind) {
                                    case "set":
                                        if (c.arg("day") != null) {
                                            WorldClock.set(server, (long)this.number(c.arg("day"), 0.0), this.timeOfDay(value));
                                        } else {
                                            WorldClock.setTimeOfDay(server, this.timeOfDay(value));
                                        }
                                        break;
                                    case "add":
                                        WorldClock.add(server, (long)this.number(value, 0.0));
                                        break;
                                    case "speed":
                                        WorldClock.setFactor(server, this.number(value, 1.0));
                                        break;
                                    case "freeze":
                                        if (value != null) {
                                            WorldClock.setTimeOfDay(server, this.timeOfDay(value));
                                        }

                                        WorldClock.freeze(server);
                                        break;
                                    default:
                                        WorldClock.unfreeze(server, c.arg("back") != null);
                                }
                            } else {
                                switch (kind) {
                                    case "set":
                                        EngineServer.setTimeOfDay(server, this.timeOfDay(value));
                                        break;
                                    case "add":
                                        EngineServer.addTime(server, (long)this.number(value, 0.0));
                                        break;
                                    case "speed": {
                                        EngineConfig cfg = EngineServer.config().copy();
                                        cfg.clock.speed = (float)this.number(value, 1.0);
                                        EngineServer.set(server, cfg);
                                        break;
                                    }
                                    case "freeze": {
                                        EngineConfig cfg = EngineServer.config().copy();
                                        cfg.clock.manage = true;
                                        cfg.clock.fixed = (int)Math.floorMod(value != null ? this.timeOfDay(value) : server.overworld().getDayTime(), 24000L);
                                        EngineServer.set(server, cfg);
                                        break;
                                    }
                                    default: {
                                        EngineConfig cfg = EngineServer.config().copy();
                                        cfg.clock.manage = true;
                                        cfg.clock.fixed = -1;
                                        cfg.clock.cycle = true;
                                        EngineServer.set(server, cfg);
                                    }
                                }
                            }
                        }
                    }));
                    break;
                }
                case "summon": {
                    Ast.Expr typeE = c.arg("type");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr into = c.arg("var");
                    b.thenRun(this.guard(() -> {
                        EntityRef r = WorldApi.summon(this.text(typeE), this.positionSafe(pos));
                        if (into instanceof Ast.Ident v) {
                            if (this.script.vars.get(v.name) instanceof List<?> l) {
                                ((List<EntityRef>)l).add(r);
                            } else {
                                this.script.vars.put(v.name, r);
                            }
                        }
                    }));
                    break;
                }
                case "gui place": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr scaleE = c.arg("scale");
                    Ast.Expr pitchE = c.arg("pitch");
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr reachE = c.arg("reach");
                    Ast.Expr rangeE = c.arg("range");
                    boolean face = c.arg("face") != null;
                    b.thenRun(
                        this.guard(
                            () -> {
                                String nx = this.text(nameE);
                                String json = SprauteRuntime.get().gui(this.script.deployed.project, nx);
                                if (json == null) {
                                    throw new SprauteRuntimeException("Интерфейс «" + nx + "» не найден в проекте. Загрузите сюжет ещё раз (F5)");
                                } else {
                                    Location atx = this.positionSafe(pos);
                                    ServerPlayer px = SprauteRuntime.player();
                                    MinecraftServer server = Platform.get().server();
                                    String dimension = (px != null ? Mc.serverLevel(px) : server.overworld()).dimension().location().toString();
                                    String idx = idE != null ? this.text(idE).trim() : "";
                                    WorldPanels.place(
                                        new Packets.WorldPanelShow(
                                            idx.isEmpty() ? nx : idx,
                                            this.script.getId(),
                                            this.script.deployed.project,
                                            nx,
                                            json,
                                            dimension,
                                            atx.x,
                                            atx.y,
                                            atx.z,
                                            atx.yaw,
                                            pitchE != null ? (float)this.number(pitchE, 0.0) : 0.0F,
                                            Math.max(0.05F, (float)this.number(scaleE, 3.0)),
                                            (float)this.number(reachE, 6.0),
                                            (float)this.number(rangeE, 64.0),
                                            face,
                                            this.panelLook(c),
                                            -1,
                                            "",
                                            0.0F,
                                            0.0F,
                                            0.0F,
                                            false
                                        )
                                    );
                                }
                            }
                        )
                    );
                    break;
                }
                case "gui attach": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr whoE = c.arg("who");
                    Ast.Expr boneE = c.arg("bone");
                    Ast.Expr offsetE = c.arg("offset");
                    Ast.Expr scaleE = c.arg("scale");
                    Ast.Expr pitchE = c.arg("pitch");
                    Ast.Expr yawE = c.arg("yaw");
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr reachE = c.arg("reach");
                    Ast.Expr rangeE = c.arg("range");
                    boolean face = c.arg("face") != null;
                    boolean self = c.arg("self") != null;
                    b.thenRun(
                        this.guard(
                            () -> {
                                String nx = this.text(nameE);
                                String json = SprauteRuntime.get().gui(this.script.deployed.project, nx);
                                if (json == null) {
                                    throw new SprauteRuntimeException("Интерфейс «" + nx + "» не найден в проекте. Загрузите сюжет ещё раз (F5)");
                                } else {
                                    Entity wearer = this.entityOf(whoE);
                                    if (wearer == null) {
                                        throw new SprauteRuntimeException("gui attach: цепляться не к кому, этого персонажа или игрока нет в мире");
                                    } else {
                                        String bone = boneE != null ? this.text(boneE).trim() : "";
                                        if (!bone.isEmpty()) {
                                            BoneWorld.position(wearer, bone, 0.0, 0.0, 0.0);
                                        }

                                        Location offx = offsetE != null ? this.eval.position(offsetE) : Location.ZERO;
                                        String idx = idE != null ? this.text(idE).trim() : "";
                                        WorldPanels.place(
                                            new Packets.WorldPanelShow(
                                                idx.isEmpty() ? nx : idx,
                                                this.script.getId(),
                                                this.script.deployed.project,
                                                nx,
                                                json,
                                                Mc.level(wearer).dimension().location().toString(),
                                                wearer.getX(),
                                                wearer.getY(),
                                                wearer.getZ(),
                                                yawE != null ? (float)this.number(yawE, 0.0) : 0.0F,
                                                pitchE != null ? (float)this.number(pitchE, 0.0) : 0.0F,
                                                Math.max(0.05F, (float)this.number(scaleE, 1.5)),
                                                (float)this.number(reachE, 6.0),
                                                (float)this.number(rangeE, 64.0),
                                                face,
                                                this.panelLook(c),
                                                wearer.getId(),
                                                bone,
                                                (float)offx.x,
                                                (float)offx.y,
                                                (float)offx.z,
                                                self
                                            ),
                                            wearer
                                        );
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "gui remove": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> {
                        if (idE == null) {
                            WorldPanels.removeAll(this.script.getId());
                        } else {
                            WorldPanels.remove(this.script.getId(), this.text(idE).trim());
                        }
                    }));
                    break;
                }
                case "slot lock":
                case "slot unlock":
                case "slot size":
                case "slot order":
                case "slot reset": {
                    Ast.Expr whichE = c.arg("which");
                    Ast.Expr countE = c.arg("count");
                    Ast.Expr orderE = c.arg("order");
                    Ast.Expr whoE = c.arg("player");
                    String kind = c.key.substring(5);
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = whoE != null ? this.eval.playerOf(whoE) : SprauteRuntime.player();
                        if (p != null) {
                            switch (kind) {
                                case "lock":
                                    PlayerSlots.lock(p, this.text(whichE));
                                    break;
                                case "unlock":
                                    PlayerSlots.unlock(p, this.text(whichE));
                                    break;
                                case "size":
                                    PlayerSlots.size(p, Eval.num(this.eval.eval(countE)));
                                    break;
                                case "order":
                                    PlayerSlots.order(p, this.text(orderE).trim());
                                    break;
                                default:
                                    PlayerSlots.reset(p);
                            }
                        }
                    }));
                    break;
                }
                case "rule deny":
                case "rule allow":
                case "rule set": {
                    Ast.Expr ruleE = c.arg("rule");
                    Ast.Expr valueE = c.arg("value");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr whoE = c.arg("player");
                    String kind = c.key.substring(5);
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = whoE != null ? this.eval.playerOf(whoE) : SprauteRuntime.player();
                        if (p != null) {
                            double valuex = kind.equals("set") ? Eval.num(this.eval.eval(valueE)) : (kind.equals("allow") ? 1.0 : 0.0);
                            PlayerRules.set(p, this.text(ruleE).trim(), valuex, timeE != null ? this.ticks(timeE) / 20.0 : 0.0);
                        }
                    }));
                    break;
                }
                case "rule reset": {
                    Ast.Expr ruleE = c.arg("rule");
                    Ast.Expr whoE = c.arg("player");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = whoE != null ? this.eval.playerOf(whoE) : SprauteRuntime.player();
                        if (p != null) {
                            if (ruleE != null) {
                                PlayerRules.reset(p, this.text(ruleE).trim());
                            } else {
                                PlayerRules.resetAll(p);
                            }
                        }
                    }));
                    break;
                }
                case "build place": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr styleE = c.arg("style");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr flightE = c.arg("flight");
                    Ast.Expr rotateE = c.arg("rotate");
                    Ast.Expr mirrorE = c.arg("mirror");
                    Ast.Expr airE = c.arg("air");
                    Ast.Expr idE = c.arg("id");
                    boolean nowait = c.arg("nowait") != null;
                    Builds.Job[] job = new Builds.Job[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                job[0] = null;
                                ServerPlayer px = SprauteRuntime.player();
                                MinecraftServer server = Platform.get().server();
                                String nx = this.text(nameE).trim();
                                StructureDef def = Builds.load(this.script.deployed.project, nx, server);
                                if (def == null) {
                                    throw new SprauteRuntimeException(
                                        "Постройка «" + nx + "» не найдена: её нет в папке «Постройки» проекта, среди структур игры и модов и в папках схем"
                                    );
                                } else {
                                    Location atx = this.positionSafe(pos);
                                    ServerLevel level = px != null ? Mc.serverLevel(px) : server.overworld();
                                    BlockPos spot = new BlockPos(Mth.floor(atx.x), Mth.floor(atx.y), Mth.floor(atx.z));
                                    job[0] = Builds.start(
                                        this.script.getId(),
                                        def,
                                        nx,
                                        spot,
                                        level,
                                        styleE != null ? this.text(styleE) : "up",
                                        timeE != null ? this.ticks(timeE) / 20.0 : -1.0,
                                        flightE != null ? this.ticks(flightE) / 20.0 : -1.0,
                                        rotateE != null ? (int)this.number(rotateE, 0.0) : 0,
                                        mirrorE != null ? this.text(mirrorE) : "",
                                        airE != null ? this.text(airE) : "",
                                        idE != null ? this.text(idE) : "",
                                        px != null ? px.getYRot() : 0.0F
                                    );
                                }
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> job[0] == null || job[0].finished());
                    }
                    break;
                }
                case "build undo": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr styleE = c.arg("style");
                    Ast.Expr timeE = c.arg("time");
                    boolean nowait = c.arg("nowait") != null;
                    Builds.Job[] job = new Builds.Job[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                ServerPlayer px = SprauteRuntime.player();
                                MinecraftServer server = Platform.get().server();
                                job[0] = Builds.undo(
                                    this.script.getId(),
                                    this.text(idE),
                                    px != null ? Mc.serverLevel(px) : server.overworld(),
                                    styleE != null ? this.text(styleE) : "scatter",
                                    timeE != null ? this.ticks(timeE) / 20.0 : -1.0,
                                    px != null ? px.getYRot() : 0.0F
                                );
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> job[0] == null || job[0].finished());
                    }
                    break;
                }
                case "build stop": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> Builds.stop(this.text(idE))));
                    break;
                }
                case "blocks move": {
                    Ast.Expr fromE = c.arg("from");
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr byE = c.arg("by");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr easeE = c.arg("ease");
                    Ast.Expr idE = c.arg("id");
                    boolean nowait = c.arg("nowait") != null;
                    BlockMoves.Job[] job = new BlockMoves.Job[1];
                    b.thenRun(
                        this.guard(
                            () -> {
                                job[0] = null;
                                Location by = this.eval.position(byE);
                                job[0] = BlockMoves.move(
                                    this.script.getId(),
                                    this.storyLevel(),
                                    this.blockPos(fromE),
                                    this.blockPos(toE),
                                    (int)Math.round(by.x),
                                    (int)Math.round(by.y),
                                    (int)Math.round(by.z),
                                    timeE != null ? this.ticks(timeE) : 20,
                                    easeE != null ? this.text(easeE) : "smooth",
                                    idE != null ? this.text(idE).trim() : ""
                                );
                            }
                        )
                    );
                    if (!nowait) {
                        b.thenWaitUntil(() -> job[0] == null || job[0].finished());
                    }
                    break;
                }
                case "blocks back": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr easeE = c.arg("ease");
                    boolean nowait = c.arg("nowait") != null;
                    BlockMoves.Job[] job = new BlockMoves.Job[1];
                    b.thenRun(this.guard(() -> {
                        job[0] = null;
                        String idx = this.text(idE).trim();
                        job[0] = BlockMoves.back(this.script.getId(), idx, timeE != null ? this.ticks(timeE) : 20, easeE != null ? this.text(easeE) : "smooth");
                        if (job[0] == null) {
                            throw new SprauteRuntimeException("blocks back: нет сдвига с id «" + idx + "»");
                        }
                    }));
                    if (!nowait) {
                        b.thenWaitUntil(() -> job[0] == null || job[0].finished());
                    }
                    break;
                }
                case "blocks stop": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> BlockMoves.stop(this.text(idE).trim())));
                    break;
                }
                case "blocks shake": {
                    Ast.Expr atE = c.arg("at");
                    Ast.Expr radiusE = c.arg("radius");
                    Ast.Expr fromE = c.arg("from");
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr powerE = c.arg("power");
                    Ast.Expr timeE = c.arg("time");
                    boolean together = c.arg("together") != null;
                    b.thenRun(this.guard(() -> {
                        float power = (float)this.number(powerE, 0.08);
                        float seconds = timeE != null ? this.ticks(timeE) / 20.0F : 1.0F;
                        if (fromE != null && toE != null) {
                            BlockMoves.shake(this.storyLevel(), this.blockPos(fromE), this.blockPos(toE), false, power, seconds, together);
                        } else if (atE == null) {
                            throw new SprauteRuntimeException("blocks shake: укажите at и radius или from и to");
                        } else {
                            int r = (int)Math.ceil(Math.max(0.0, this.number(radiusE, 6.0)));
                            BlockPos atx = this.blockPos(atE);
                            BlockMoves.shake(this.storyLevel(), atx.offset(-r, -r, -r), atx.offset(r, r, r), true, power, seconds, together);
                        }
                    }));
                    break;
                }
                case "blocks wave": {
                    Ast.Expr atE = c.arg("at");
                    Ast.Expr radiusE = c.arg("radius");
                    Ast.Expr heightE = c.arg("height");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr stepE = c.arg("step");
                    Ast.Expr riseE = c.arg("rise");
                    Ast.Expr tiltE = c.arg("tilt");
                    b.thenRun(
                        this.guard(
                            () -> {
                                Location atx = this.eval.position(atE);
                                BlockMoves.wave(
                                    this.storyLevel(),
                                    atx.x,
                                    atx.y,
                                    atx.z,
                                    BlockQuake.Wave.of(
                                        this.number(radiusE, 8.0),
                                        this.number(heightE, 0.6),
                                        timeE != null ? this.ticks(timeE) / 20.0 : 0.8,
                                        this.number(stepE, 0.0),
                                        this.number(riseE, 0.0),
                                        this.number(tiltE, 10.0)
                                    )
                                );
                            }
                        )
                    );
                    break;
                }
                case "setblock": {
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr block = c.arg("block");
                    b.thenRun(this.guard(() -> WorldApi.setBlock(this.positionSafe(pos), this.text(block))));
                    break;
                }
                case "use": {
                    Ast.Expr who = c.arg("who");
                    Ast.Expr pos = c.arg("pos");
                    b.thenRun(this.guard(() -> {
                        Entity e = this.entityOf(who);
                        if (e == null) {
                            throw new SprauteRuntimeException("use: некому использовать блок, персонажа нет в мире");
                        } else {
                            WorldApi.useBlock(e, this.positionSafe(pos));
                        }
                    }));
                    break;
                }
                case "setprop": {
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr prop = c.arg("prop");
                    Ast.Expr value = c.arg("value");
                    b.thenRun(this.guard(() -> WorldApi.setProp(this.positionSafe(pos), this.text(prop), this.eval.eval(value))));
                    break;
                }
                case "broadcast": {
                    Ast.Expr text = c.arg("text");
                    b.thenRun(this.guard(() -> {
                        Component msg = Component.literal(this.safeText(text));

                        for (ServerPlayer p : SprauteRuntime.players()) {
                            p.sendSystemMessage(msg);
                        }
                    }));
                    break;
                }
                case "setowner": {
                    Ast.Expr who = c.arg("player");
                    b.thenRun(this.guard(() -> {
                        if (this.script.solo()) {
                            throw new SprauteRuntimeException("setowner: у сюжета «только игроку» главный игрок не меняется");
                        } else {
                            this.script.owner = this.eval.playerOf(who).getUUID();
                        }
                    }));
                    break;
                }
                case "pvar set":
                case "pvar add":
                case "pvar clear": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr valueE = c.arg("value");
                    Ast.Expr whoE = c.arg("player");
                    String kind = c.key.substring(5);
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = whoE != null ? this.eval.playerOf(whoE) : SprauteRuntime.player();
                        if (p != null) {
                            String namex = this.text(nameE).trim();
                            switch (kind) {
                                case "set":
                                    PlayerVars.set(p, namex, this.eval.eval(valueE));
                                    break;
                                case "add":
                                    PlayerVars.add(p, namex, Eval.num(this.eval.eval(valueE)));
                                    break;
                                default:
                                    PlayerVars.clear(p, namex);
                            }
                        }
                    }));
                    break;
                }
                case "team join":
                case "team leave":
                case "team color":
                case "team pvp": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr whoE = c.arg("player");
                    Ast.Expr colorE = c.arg("color");
                    boolean off = c.arg("off") != null;
                    String kind = c.key.substring(5);
                    b.thenRun(this.guard(() -> {
                        if (kind.equals("color")) {
                            PlayerTeams.color(this.text(nameE).trim(), this.text(colorE).trim());
                        } else if (kind.equals("pvp")) {
                            PlayerTeams.pvp(this.text(nameE).trim(), !off);
                        } else {
                            ServerPlayer px = whoE != null ? this.eval.playerOf(whoE) : SprauteRuntime.player();
                            if (px == null) {
                                return;
                            }

                            if (kind.equals("join")) {
                                PlayerTeams.join(px, this.text(nameE).trim());
                            } else {
                                PlayerTeams.leave(px);
                            }
                        }
                    }));
                    break;
                }
                case "tag add":
                case "tag remove": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr whoE = c.arg("player");
                    boolean add = c.key.equals("tag add");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = whoE != null ? this.eval.playerOf(whoE) : SprauteRuntime.player();
                        if (p != null) {
                            String tag = this.text(nameE).trim();
                            if (tag.isEmpty()) {
                                throw new SprauteRuntimeException("Пустое имя тега");
                            } else if (add && !p.getTags().contains(tag) && p.getTags().size() >= 1024) {
                                throw new SprauteRuntimeException("У игрока уже 1024 тега: больше игра не хранит");
                            } else if (add ? p.addTag(tag) : p.removeTag(tag)) {
                                Map<String, Object> data = new LinkedHashMap<>();
                                data.put("tag", tag);
                                GameEvents.post(add ? "tagadd" : "tagremove", p, data);
                            }
                        }
                    }));
                    break;
                }
                case "emit": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr valueE = c.arg("value");
                    b.thenRun(this.guard(() -> {
                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("name", this.text(nameE));
                        data.put("value", valueE != null ? this.eval.eval(valueE) : "");
                        data.put("from", (double)this.script.getId());
                        GameEvents.fire("signal", null, data);
                    }));
                    break;
                }
                case "post play": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr powerE = c.arg("power");
                    b.thenRun(this.guard(() -> {
                        String nx = this.text(nameE).trim();
                        String json = Posts.json(this.script.deployed.project, nx);
                        if (json == null) {
                            throw new SprauteRuntimeException("Пост-эффект «" + nx + "» не найден: его нет в папке «Пост-эффекты» проекта и среди готовых");
                        } else {
                            Posts.play(this.script.getId(), json, timeE != null ? this.ticks(timeE) : 20, this.number(powerE, 1.0));
                        }
                    }));
                    break;
                }
                case "post set": {
                    Ast.Expr paramE = c.arg("param");
                    Ast.Expr valueE = c.arg("value");
                    Ast.Expr timeE = c.arg("time");
                    String param = paramE instanceof Ast.Ident id ? id.name : (paramE instanceof Ast.StringLit s ? s.value : "");
                    b.thenRun(this.guard(() -> Posts.set(this.script.getId(), param, this.text(valueE).trim(), timeE != null ? this.ticks(timeE) : 0)));
                    break;
                }
                case "post pulse": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr inE = c.arg("in");
                    Ast.Expr holdE = c.arg("hold");
                    Ast.Expr outE = c.arg("out");
                    Ast.Expr powerE = c.arg("power");
                    b.thenRun(
                        this.guard(
                            () -> {
                                String nx = this.text(nameE).trim();
                                String json = Posts.json(this.script.deployed.project, nx);
                                if (json == null) {
                                    throw new SprauteRuntimeException(
                                        "Пост-эффект «" + nx + "» не найден: его нет в папке «Пост-эффекты» проекта и среди готовых"
                                    );
                                } else {
                                    Posts.pulse(
                                        this.script.getId(),
                                        json,
                                        inE != null ? this.ticks(inE) : 2,
                                        holdE != null ? this.ticks(holdE) : 4,
                                        outE != null ? this.ticks(outE) : 14,
                                        this.number(powerE, 1.0)
                                    );
                                }
                            }
                        )
                    );
                    break;
                }
                case "post clear": {
                    Ast.Expr timeE = c.arg("time");
                    b.thenRun(this.guard(() -> Posts.clear(this.script.getId(), timeE != null ? this.ticks(timeE) : 20)));
                    break;
                }
                case "fx stop": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> SprauteNetwork.sendToAll(new Packets.FxStop(this.text(idE), -1))));
                    break;
                }
                case "fx clear":
                    b.thenRun(this.guard(() -> SprauteNetwork.sendToAll(new Packets.FxStop("", -1))));
                    break;
                case "camera hide": {
                    Ast.Expr what = c.arg("what");
                    b.thenRun(this.guard(() -> {
                        String w = this.text(what).trim().toLowerCase(Locale.ROOT);
                        if (!CutsceneDoc.HIDE_MODES.contains(w)) {
                            throw new SprauteRuntimeException("camera hide: ожидалось all, minecraft, classic, custom или none, а не «" + w + "»");
                        } else {
                            JsonObject o = op("hud");
                            o.addProperty("value", w);
                            this.camera(o);
                        }
                    }));
                    break;
                }
                case "camera hud":
                case "camera lock": {
                    Ast.Expr value = c.arg("value");
                    String kind = c.key.substring(7);
                    b.thenRun(this.guard(() -> {
                        JsonObject o = op(kind);
                        o.addProperty("value", Eval.truthy(this.eval.eval(value)));
                        this.camera(o);
                    }));
                    break;
                }
                case "orb spawn": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr count = c.arg("count");
                    Ast.Expr value = c.arg("value");
                    Ast.Expr coins = c.arg("coins");
                    Ast.Expr xp = c.arg("xp");
                    Ast.Expr image = c.arg("image");
                    Ast.Expr item = c.arg("item");
                    Ast.Expr color = c.arg("color");
                    Ast.Expr size = c.arg("size");
                    Ast.Expr radius = c.arg("radius");
                    Ast.Expr life = c.arg("life");
                    Ast.Expr spread = c.arg("spread");
                    boolean nopickup = c.arg("nopickup") != null;
                    boolean floating = c.arg("float") != null;
                    b.thenRun(
                        this.guard(
                            () -> {
                                ServerPlayer px = SprauteRuntime.player();
                                if (px != null) {
                                    Location atx = this.positionSafe(pos);
                                    int nx = count != null ? Math.max(1, Math.min(500, (int)Eval.num(this.eval.eval(count)))) : 1;
                                    double sp = spread != null ? this.number(spread, 0.5) : 0.5;
                                    String lookx = "";
                                    if (image != null) {
                                        lookx = Images.texture(this.script.deployed.project, this.text(image)).toString();
                                    } else if (item != null) {
                                        String iid = this.text(item);
                                        lookx = "item:" + ItemSpec.with(iid, ItemSpec.nbt(iid));
                                    }

                                    int col = color != null ? GuiDoc.toColor(this.text(color)) : -1;
                                    if (col >>> 24 == 0) {
                                        col |= -16777216;
                                    }

                                    for (int i = 0; i < nx; i++) {
                                        SprauteOrb orb = new SprauteOrb(Orbs.ORB.get(), Mc.serverLevel(px));
                                        orb.setKind(this.text(idE));
                                        orb.setLook(lookx);
                                        orb.setColor(col);
                                        orb.setSize((float)(size != null ? this.number(size, 0.25) : 0.25));
                                        orb.setLife(life != null ? this.ticks(life) : 1200);
                                        orb.value = value != null ? (int)Eval.num(this.eval.eval(value)) : 1;
                                        orb.coins = coins != null ? (int)Eval.num(this.eval.eval(coins)) : 0;
                                        orb.xp = xp != null ? (int)Eval.num(this.eval.eval(xp)) : 0;
                                        orb.radius = radius != null ? this.number(radius, 6.0) : 6.0;
                                        orb.pickup = !nopickup;
                                        orb.floating = floating;
                                        RandomSource rnd = px.getRandom();
                                        orb.setPos(
                                            atx.x + (rnd.nextDouble() - 0.5) * sp,
                                            atx.y + 0.5 + rnd.nextDouble() * sp * 0.5,
                                            atx.z + (rnd.nextDouble() - 0.5) * sp
                                        );
                                        orb.setDeltaMovement(
                                            (rnd.nextDouble() - 0.5) * 0.2 * sp, 0.1 + rnd.nextDouble() * 0.15 * sp, (rnd.nextDouble() - 0.5) * 0.2 * sp
                                        );
                                        Mc.serverLevel(px).addFreshEntity(orb);
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "orb clear": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            String only = idE != null ? this.text(idE) : null;

                            for (Entity en : Mc.serverLevel(p).getAllEntities()) {
                                if (en instanceof SprauteOrb o && (only == null || o.kind().equals(only))) {
                                    o.discard();
                                }
                            }
                        }
                    }));
                    break;
                }
                case "particles": {
                    Ast.Expr type = c.arg("type");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr count = c.arg("count");
                    Ast.Expr spread = c.arg("spread");
                    Ast.Expr speed = c.arg("speed");
                    Ast.Expr boneE = c.arg("bone");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            String t = this.text(type);
                            ResourceLocation rl = ResourceLocation.tryParse(t.contains(":") ? t : "minecraft:" + t);
                            if ((rl == null ? null : Regs.PARTICLE_TYPE.get(rl)) instanceof SimpleParticleType simple) {
                                Location atx = this.positionSafe(pos);
                                int nx = count != null ? (int)Eval.num(this.eval.eval(count)) : 10;
                                double sp = spread != null ? this.number(spread, 0.5) : 0.5;
                                double px = atx.x;
                                double py = atx.y + 1.0;
                                double pz = atx.z;
                                if (pos instanceof Ast.Call boneCall && boneCall.name.equals("bone")) {
                                    py = atx.y;
                                }

                                if (boneE != null) {
                                    Entity carrier = this.entityOf(pos);
                                    if (carrier == null) {
                                        throw new SprauteRuntimeException("particles … bone: цель должна быть персонажем с моделью");
                                    }

                                    Vec3 point = BoneWorld.position(carrier, this.text(boneE).trim(), 0.0, 0.0, 0.0);
                                    px = point.x;
                                    py = point.y;
                                    pz = point.z;
                                }

                                Mc.serverLevel(p).sendParticles(simple, px, py, pz, nx, sp, sp, sp, speed != null ? this.number(speed, 0.02) : 0.02);
                            } else {
                                throw new SprauteRuntimeException("Частицы " + t + " не найдены (нужен простой тип: flame, heart, cloud, end_rod...)");
                            }
                        }
                    }));
                    break;
                }
                case "coins give":
                case "coins take":
                case "coins set":
                case "coins need": {
                    Ast.Expr amount = c.arg("amount");
                    String k = c.key;
                    b.thenRun(this.guard(() -> {
                        int nx = (int)Eval.num(this.eval.eval(amount));
                        switch (k) {
                            case "coins give":
                                Coins.add(SprauteRuntime.player(), nx);
                                break;
                            case "coins take":
                                Coins.add(SprauteRuntime.player(), -Math.min(nx, Coins.get(SprauteRuntime.player())));
                                break;
                            case "coins need":
                                Coins.need(SprauteRuntime.player(), nx);
                                break;
                            default:
                                Coins.set(SprauteRuntime.player(), nx);
                        }
                    }));
                    break;
                }
                case "coins drop": {
                    Ast.Expr amount = c.arg("amount");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr count = c.arg("count");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            Location atx = this.positionSafe(pos);
                            int total = (int)Eval.num(this.eval.eval(amount));
                            int nx = count != null ? Math.max(1, (int)Eval.num(this.eval.eval(count))) : 1;
                            RandomSource rnd = p.getRandom();

                            for (int i = 0; i < nx; i++) {
                                int part = total / nx + (i < total % nx ? 1 : 0);
                                if (part > 0) {
                                    String coinItem = Coins.item();
                                    if (coinItem != null) {
                                        double dropX = atx.x + (rnd.nextDouble() - 0.5);
                                        double dropZ = atx.z + (rnd.nextDouble() - 0.5);
                                        int left = part;

                                        while (left > 0) {
                                            ItemStack stack = ItemSpec.stack(coinItem, 1);
                                            int size = Math.min(left, stack.getMaxStackSize());
                                            stack.setCount(size);
                                            ItemEntity drop = new ItemEntity(Mc.serverLevel(p), dropX, atx.y + 0.5, dropZ, stack);
                                            drop.setDefaultPickUpDelay();
                                            Mc.serverLevel(p).addFreshEntity(drop);
                                            left -= size;
                                        }
                                    } else {
                                        SprauteOrb e = new SprauteOrb(Orbs.ORB.get(), Mc.serverLevel(p));
                                        e.setKind("coins");
                                        e.setLook("spraute_engine:textures/gui/" + (part < 10 ? "coin.png" : (part < 100 ? "coin_2.png" : "coin_3.png")));
                                        e.setColor(-1);
                                        e.setSize(part < 10 ? 0.25F : (part < 100 ? 0.32F : 0.4F));
                                        e.setLife(1000);
                                        e.coins = part;
                                        e.radius = 10.0;
                                        e.pickup = true;
                                        e.setPos(atx.x + (rnd.nextDouble() - 0.5), atx.y + 0.5, atx.z + (rnd.nextDouble() - 0.5));
                                        Mc.serverLevel(p).addFreshEntity(e);
                                    }
                                }
                            }
                        }
                    }));
                    break;
                }
                case "gui slot": {
                    Ast.Expr el = c.arg("el");
                    Ast.Expr item = c.arg("item");
                    Ast.Expr count = c.arg("count");
                    b.thenRun(this.guard(() -> {
                        int nx = count != null ? (int)Eval.num(this.eval.eval(count)) : 1;
                        String id = this.text(item);
                        ItemStack st = nx <= 0 ? ItemStack.EMPTY : GuiSlots.stack(id, nx);
                        if (st.isEmpty() && nx > 0 && !id.isBlank()) {
                            throw new SprauteRuntimeException("Предмет " + id + " не найден");
                        } else {
                            GuiSlots.set(this.script, this.text(el), st);
                        }
                    }));
                    break;
                }
                case "gui clearslot": {
                    Ast.Expr el = c.arg("el");
                    b.thenRun(this.guard(() -> GuiSlots.set(this.script, this.text(el), ItemStack.EMPTY)));
                    break;
                }
                case "wait slot": {
                    List<String> ids = new ArrayList<>();

                    for (Ast.Arg a : c.args) {
                        if ("slots".equals(a.param)) {
                            ids.add(this.text(a.value));
                        }
                    }

                    int[] start = new int[]{0};
                    b.thenRun(() -> start[0] = this.script.slotSeq);
                    b.thenWaitUntil(() -> this.script.slotSeq > start[0] && (ids.isEmpty() || ids.contains(this.script.lastSlot)));
                    break;
                }
                case "gui set": {
                    Ast.Expr el = c.arg("el");
                    Ast.Expr prop = c.arg("prop");
                    Ast.Expr value = c.arg("value");
                    b.thenRun(this.guard(() -> this.sendGuiUpdate(this.text(el), this.text(prop), this.safeText(value))));
                    break;
                }
                case "wait button": {
                    List<String> ids = new ArrayList<>();

                    for (Ast.Arg a : c.args) {
                        if ("buttons".equals(a.param)) {
                            ids.add(this.text(a.value));
                        }
                    }

                    int[] start = new int[]{0};
                    b.thenRun(() -> start[0] = this.script.buttonSeq);
                    b.thenWaitUntil(() -> this.script.buttonSeq > start[0] && (ids.isEmpty() || ids.contains(this.script.lastButton)));
                    break;
                }
                case "wait guiclose": {
                    int[] start = new int[]{0};
                    b.thenRun(() -> start[0] = this.script.guiCloseSeq);
                    b.thenWaitUntil(() -> this.script.guiCloseSeq > start[0] || this.script.openGui == null);
                    break;
                }
                case "stopanim": {
                    Character ch = this.animated(b, c);
                    if (ch != null) {
                        String layer = this.layer(c.arg("layer"), "action");
                        ConfiguredAnimation none = Animation.NULL.configure().transitionTime(this.fadeSeconds(c, 0.25F));
                        b.setAnimation(ch, layer, none);
                    }
                    break;
                }
                case "clearanims": {
                    Character ch = this.animated(b, c);
                    if (ch != null) {
                        b.clearAnimations(ch, this.fadeSeconds(c, 0.25F));
                    }
                    break;
                }
                case "moveanim": {
                    Character ch = this.animated(b, c);
                    if (ch != null) {
                        String var312 = this.text(c.arg("state")).toLowerCase();

                        MovementActionType typex = switch (var312) {
                            case "walk" -> MovementActionType.WALKING;
                            case "fastwalk" -> MovementActionType.FAST_WALKING;
                            case "run" -> MovementActionType.RUNNING;
                            default -> MovementActionType.IDLE;
                        };
                        Animation ax = this.anim(b, c.arg("anim"));
                        if (ax != null) {
                            b.overridePredefinedAnimation(ch, typex, ax.configure());
                        }
                    }
                    break;
                }
                case "resetanim": {
                    Character ch = this.animated(b, c);
                    if (ch != null) {
                        b.removeOverridePredefined(ch);
                    }
                    break;
                }
                case "sigh":
                    b.sigh(this.character(c.arg("who")));
                    break;
                case "shake":
                    b.shake(this.character(c.arg("who")), this.ticks(c.arg("time")));
                    break;
                case "teleport":
                    b.teleportPlayer(this.locationSupplier(c.arg("pos")));
                    break;
                case "effect":
                    MobEffect effect = Regs.MOB_EFFECT.get(this.id(c.arg("effect")));
                    if (effect == null) {
                        throw new SprauteRuntimeException("Эффект " + this.text(c.arg("effect")) + " не найден");
                    }

                    b.thenAddEffect(new MobEffectInstance(effect, this.ticks(c.arg("time")), (int)this.number(c.arg("level"), 0.0)));
                    break;
                case "sound": {
                    String sname = this.text(c.arg("sound"));
                    Ast.Expr at0 = c.arg("pos");
                    Ast.Expr volE = c.arg("volume");
                    Ast.Expr pitchE = c.arg("pitch");
                    Ast.Expr sidE = c.arg("id");
                    Ast.Expr fadeE = c.arg("fade");
                    boolean loopS = c.arg("loop") != null;
                    float fadeIn = fadeE != null ? this.ticks(fadeE) / 20.0F : 0.0F;
                    String project = this.script.deployed.project == null ? "" : this.script.deployed.project;
                    Path own = project.isEmpty() ? null : Platform.get().gameDir().resolve("spraute").resolve("projects").resolve(project).resolve("sounds");
                    boolean projectSound = !sname.contains(":")
                        && own != null
                        && (Files.isRegularFile(own.resolve(sname + ".ogg")) || Files.isRegularFile(own.resolve(sname + ".wav")));
                    ResourceLocation srl = projectSound ? null : ResourceLocation.tryParse(sname.contains(":") ? sname : "minecraft:" + sname);
                    SoundEvent sound = srl == null ? null : Regs.SOUND_EVENT.get(srl);
                    if (sound != null && !projectSound && sidE == null && fadeE == null && !loopS) {
                        if (volE == null && pitchE == null) {
                            Ast.Expr at = c.arg("pos");
                            Supplier<BlockPos> posx = at != null ? () -> Mc.blockPos(this.positionSafe(at)) : () -> {
                                ServerPlayer px = SprauteRuntime.player();
                                return px != null ? px.blockPosition() : BlockPos.ZERO;
                            };
                            b.playSound(posx, sound);
                        } else {
                            b.thenRun(
                                this.guard(
                                    () -> {
                                        ServerPlayer px = SprauteRuntime.player();
                                        if (px != null) {
                                            BlockPos bp = at0 != null ? Mc.blockPos(this.positionSafe(at0)) : px.blockPosition();
                                            Mc.level(px)
                                                .playSound(
                                                    null,
                                                    bp,
                                                    sound,
                                                    SoundSource.MASTER,
                                                    volE != null ? (float)this.number(volE, 1.0) : 1.0F,
                                                    pitchE != null ? (float)this.number(pitchE, 1.0) : 1.0F
                                                );
                                        }
                                    }
                                )
                            );
                        }
                    } else {
                        String played = !projectSound && sound != null ? srl.toString() : sname;
                        b.thenRun(this.guard(() -> {
                            float vol = volE != null ? (float)this.number(volE, 1.0) : 1.0F;
                            float pt = pitchE != null ? (float)this.number(pitchE, 1.0) : 1.0F;
                            String sid = sidE != null ? this.safeText(sidE) : "";
                            double sx = 0.0;
                            double sy = 0.0;
                            double sz = 0.0;
                            if (at0 != null) {
                                Location l = this.positionSafe(at0);
                                sx = l.x;
                                sy = l.y;
                                sz = l.z;
                            }

                            SprauteNetwork.sendToAll(new Packets.SoundPlay(project, played, sx, sy, sz, at0 != null, vol, pt, loopS, sid, fadeIn));
                        }));
                        if (!projectSound && sound == null) {
                            SprauteRuntime.get().report(this.script.getId(), "Звук «" + sname + "» не найден ни в игре, ни в папке «Звуки» проекта " + project);
                        }
                    }
                    break;
                }
                case "highlight":
                case "highlight off":
                case "highlight clear":
                case "gizmo line":
                case "gizmo box":
                case "gizmo sphere":
                case "gizmo text":
                case "gizmo screen text":
                case "gizmo screen rect":
                case "gizmo screen line":
                case "gizmo clear":
                    this.gizmo(b, c);
                    break;
                case "navigate to":
                case "navigate zone": {
                    Ast.Expr toE = c.arg("to");
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr titleE = c.arg("title");
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr colorE = c.arg("color");
                    Ast.Expr radiusE = c.arg("radius");
                    boolean zone = c.key.equals("navigate zone");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            String id = idE != null ? this.safeText(idE) : "";
                            String title = titleE != null ? this.safeText(titleE) : "";
                            Integer parsed = colorE != null ? Colors.parse(this.safeText(colorE)) : null;
                            int colorx = parsed != null ? parsed & 16777215 : -1;
                            Navigation.Target t;
                            if (zone) {
                                String n = this.text(nameE);
                                String proj = this.script.deployed.project;
                                ZoneDef def = Zones.require(proj, n);
                                Vec3 center = def.center();
                                t = new Navigation.Target(id, title.isEmpty() ? n : title, colorx, () -> center, pl -> Zones.playerIn(pl, proj, n));
                            } else {
                                Object tox = this.eval.ref(toE);
                                double radius = radiusE != null ? this.number(radiusE, 3.0) : 3.0;
                                Supplier<Vec3> where;
                                if (tox instanceof EntityRef er) {
                                    where = () -> {
                                        Entity en = er.get();
                                        return en != null && Mc.level(en) == Mc.level(p) ? en.position() : null;
                                    };
                                } else {
                                    Location l = this.positionSafe(toE);
                                    Vec3 atx = new Vec3(l.x, l.y, l.z);
                                    where = () -> atx;
                                }

                                t = new Navigation.Target(id, title, colorx, where, pl -> {
                                    Vec3 w = where.get();
                                    return w != null && pl.position().distanceTo(w) <= radius;
                                });
                            }

                            Navigation.start(p, t);
                        }
                    }));
                    break;
                }
                case "navigate stop": {
                    Ast.Expr idE = c.arg("id");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            Navigation.stop(p, idE != null ? this.safeText(idE) : "");
                        }
                    }));
                    break;
                }
                case "stopsound": {
                    Ast.Expr sidE = c.arg("id");
                    Ast.Expr fadeE = c.arg("fade");
                    float fadeOut = fadeE != null ? this.ticks(fadeE) / 20.0F : 0.0F;
                    b.thenRun(this.guard(() -> SprauteNetwork.sendToAll(new Packets.SoundStop(sidE != null ? this.safeText(sidE) : "", fadeOut))));
                    break;
                }
                case "recipe shaped":
                case "recipe shapeless": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr resultE = c.arg("result");
                    Ast.Expr countE = c.arg("count");
                    String[] names = new String[]{"a1", "a2", "a3", "b1", "b2", "b3", "c1", "c2", "c3"};
                    Ast.Expr[] slots = new Ast.Expr[names.length];

                    for (int i = 0; i < names.length; i++) {
                        slots[i] = c.arg(names[i]);
                    }

                    boolean shaped = c.key.equals("recipe shaped");
                    b.thenRun(this.guard(() -> {
                        String[] texts = new String[slots.length];

                        for (int i = 0; i < slots.length; i++) {
                            texts[i] = slots[i] != null ? this.text(slots[i]) : "";
                        }

                        RecipeApi.crafting(shaped, this.text(idE), this.text(resultE), (int)this.number(countE, 1.0), texts);
                    }));
                    break;
                }
                case "recipe smelting":
                case "recipe blasting":
                case "recipe smoking":
                case "recipe campfire": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr inE = c.arg("input");
                    Ast.Expr resultE = c.arg("result");
                    Ast.Expr timeE = c.arg("time");
                    Ast.Expr xpE = c.arg("xp");
                    String type = c.key.equals("recipe campfire") ? "minecraft:campfire_cooking" : "minecraft:" + c.key.substring(7);
                    b.thenRun(
                        this.guard(
                            () -> RecipeApi.cooking(
                                type, this.text(idE), this.text(inE), this.text(resultE), timeE != null ? this.ticks(timeE) : -1, this.number(xpE, 0.1)
                            )
                        )
                    );
                    break;
                }
                case "recipe stonecutting": {
                    Ast.Expr idE = c.arg("id");
                    Ast.Expr inE = c.arg("input");
                    Ast.Expr resultE = c.arg("result");
                    Ast.Expr countE = c.arg("count");
                    b.thenRun(this.guard(() -> RecipeApi.stonecutting(this.text(idE), this.text(inE), this.text(resultE), (int)this.number(countE, 1.0))));
                    break;
                }
                case "recipe disable":
                case "recipe enable":
                case "recipe reset": {
                    Ast.Expr idE = c.arg("id");
                    String what = c.key.substring(7);
                    b.thenRun(this.guard(() -> {
                        if (what.equals("disable")) {
                            RecipeApi.disable(this.text(idE));
                        } else if (what.equals("enable")) {
                            RecipeApi.enable(this.text(idE));
                        } else {
                            RecipeApi.reset(this.text(idE));
                        }
                    }));
                    break;
                }
                case "mob spawn": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    Ast.Expr countE = c.arg("count");
                    Ast.Expr into = c.arg("var");
                    b.thenRun(this.guard(() -> {
                        String typex = MobDef.ref(this.text(nameE).trim());
                        Location atx = this.positionSafe(pos);
                        int countx = countE != null ? (int)Math.max(1.0, Math.min(100.0, this.number(countE, 1.0))) : 1;
                        List<Object> made = new ArrayList<>();

                        for (int i = 0; i < countx; i++) {
                            double angle = i * 2.39996;
                            double far = countx > 1 ? 1.5 * Math.sqrt(i) : 0.0;
                            made.add(WorldApi.summon(typex, new Vec3(atx.x + Math.cos(angle) * far, atx.y, atx.z + Math.sin(angle) * far)));
                        }

                        if (into instanceof Ast.Ident v) {
                            if (this.script.vars.get(v.name) instanceof List<?> l) {
                                ((List<Object>)l).addAll(made);
                            } else {
                                this.script.vars.put(v.name, countx == 1 ? made.get(0) : made);
                            }
                        }
                    }));
                    break;
                }
                case "dungeon place": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    b.thenRun(
                        this.guard(
                            () -> {
                                MinecraftServer server = Platform.get().server();
                                if (server != null) {
                                    ServerPlayer p = SprauteRuntime.player();
                                    Location atx = this.positionSafe(pos);
                                    String problem = Dungeons.placeNow(
                                        this.text(nameE).trim(),
                                        p != null ? Mc.serverLevel(p) : server.overworld(),
                                        new BlockPos((int)Math.floor(atx.x), (int)Math.floor(atx.y), (int)Math.floor(atx.z))
                                    );
                                    if (problem != null) {
                                        throw new SprauteRuntimeException(problem);
                                    }
                                }
                            }
                        )
                    );
                    break;
                }
                case "loot give":
                case "loot chest": {
                    Ast.Expr nameE = c.arg("name");
                    Ast.Expr pos = c.arg("pos");
                    boolean give = c.key.equals("loot give");
                    b.thenRun(this.guard(() -> {
                        MinecraftServer server = Platform.get().server();
                        if (server != null) {
                            String table = LootDef.ref(this.text(nameE).trim());
                            String line;
                            if (give) {
                                ServerPlayer p = SprauteRuntime.player();
                                if (p == null) {
                                    return;
                                }

                                line = "loot give " + p.getGameProfile().getName() + " loot " + table;
                            } else {
                                Location atx = this.positionSafe(pos);
                                line = "loot insert " + (int)Math.floor(atx.x) + " " + (int)Math.floor(atx.y) + " " + (int)Math.floor(atx.z) + " loot " + table;
                            }

                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), line);
                        }
                    }));
                    break;
                }
                case "recipe learn":
                case "recipe forget": {
                    Ast.Expr idE = c.arg("id");
                    boolean learn = c.key.equals("recipe learn");
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null) {
                            if (learn) {
                                RecipeApi.learn(p, this.text(idE));
                            } else {
                                RecipeApi.forget(p, this.text(idE));
                            }
                        }
                    }));
                    break;
                }
                case "item cooldown": {
                    Ast.Expr itemE = c.arg("item");
                    Ast.Expr timeE = c.arg("time");
                    b.thenRun(this.guard(() -> ItemApi.cooldown(SprauteRuntime.player(), this.text(itemE), this.ticks(timeE))));
                    break;
                }
                case "item repair": {
                    Ast.Expr amountE = c.arg("amount");
                    b.thenRun(this.guard(() -> ItemApi.repair(SprauteRuntime.player(), amountE != null ? (int)this.number(amountE, 0.0) : -1)));
                    break;
                }
                case "item damage": {
                    Ast.Expr amountE = c.arg("amount");
                    b.thenRun(this.guard(() -> ItemApi.damage(SprauteRuntime.player(), (int)this.number(amountE, 0.0))));
                    break;
                }
                case "item rename": {
                    Ast.Expr nameE = c.arg("name");
                    b.thenRun(this.guard(() -> ItemApi.rename(SprauteRuntime.player(), this.text(nameE))));
                    break;
                }
                case "item enchant":
                    Ast.Expr enchantE = c.arg("enchant");
                    Ast.Expr levelE = c.arg("level");
                    b.thenRun(this.guard(() -> ItemApi.enchant(SprauteRuntime.player(), this.text(enchantE), (int)this.number(levelE, 1.0))));
                    break;
                case "give":
                    ItemStack stack = this.itemStack(c.arg("item"), (int)this.number(c.arg("count"), 1.0));
                    b.thenRun(this.guard(() -> {
                        ServerPlayer p = SprauteRuntime.player();
                        if (p != null && !p.getInventory().add(stack.copy())) {
                            p.drop(stack.copy(), false);
                        }
                    }));
                    break;
                case "command":
                    Ast.Expr cmd = c.arg("command");
                    b.thenRun(this.guard(() -> {
                        MinecraftServer server = Platform.get().server();
                        if (server != null) {
                            String line = this.safeText(cmd);
                            if (line.startsWith("/")) {
                                line = line.substring(1);
                            }

                            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), line);
                        }
                    }));
                    break;
                case "blackout":
                    b.blackout(this.ticks(c.arg("time")));
                    break;
                case "flash":
                    b.flash(this.ticks(c.arg("time")));
                    break;
                case "screen":
                    b.screenColor(
                        (int)this.number(c.arg("r"), 0.0),
                        (int)this.number(c.arg("g"), 0.0),
                        (int)this.number(c.arg("b"), 0.0),
                        (int)this.number(c.arg("a"), 255.0)
                    );
                    break;
                case "eyes":
                    b.close_eyes(this.ticks(c.arg("close")), this.ticks(c.arg("hold")), this.ticks(c.arg("open")));
                    break;
                case "flag set": {
                    Ast.Expr name = c.arg("name");
                    Ast.Expr value = c.arg("value");
                    b.thenRun(this.guard(() -> {
                        if (value != null) {
                            this.script.getFlags().set(this.safeText(name), this.safeText(value));
                        } else {
                            this.script.getFlags().set(this.safeText(name));
                        }
                    }));
                    break;
                }
                case "js":
                    Ast.Expr codeE = c.arg("code");
                    b.thenRun(this.guard(() -> {
                        String code = codeE instanceof Ast.StringLit sl ? sl.value.replace("\\{", "{") : this.safeText(codeE);
                        JsEngine.run(this.script, code);
                    }));
                    break;
                case "setnbt": {
                    Ast.Expr t = c.arg("target");
                    Ast.Expr p = c.arg("path");
                    Ast.Expr v = c.arg("value");
                    b.thenRun(this.guard(() -> Bridges.setNbt(this.eval.ref(t), this.text(p), this.eval.eval(v))));
                    break;
                }
                case "setattr": {
                    Ast.Expr t = c.arg("target");
                    Ast.Expr a = c.arg("attr");
                    Ast.Expr v = c.arg("value");
                    b.thenRun(this.guard(() -> Bridges.setAttr(this.eval.ref(t), this.text(a), this.number(v, 0.0))));
                    break;
                }
                case "setscore": {
                    Ast.Expr t = c.arg("target");
                    Ast.Expr o = c.arg("objective");
                    Ast.Expr v = c.arg("value");
                    boolean add = c.arg("add") != null;
                    b.thenRun(this.guard(() -> Bridges.setScore(this.eval.ref(t), this.text(o), this.number(v, 0.0), add)));
                    break;
                }
                case "global set":
                case "global add": {
                    Ast.Expr name = c.arg("name");
                    Ast.Expr value = c.arg("value");
                    boolean add = c.key.equals("global add");
                    b.thenRun(this.guard(() -> {
                        Object vx = this.eval.eval(value);
                        if (add) {
                            Globals.add(this.safeText(name), Eval.num(vx));
                        } else {
                            Globals.set(this.safeText(name), vx);
                        }
                    }));
                    break;
                }
                case "global clear": {
                    Ast.Expr name = c.arg("name");
                    b.thenRun(this.guard(() -> Globals.clear(this.safeText(name))));
                    break;
                }
                case "flag clear": {
                    Ast.Expr name = c.arg("name");
                    b.thenRun(this.guard(() -> this.script.getFlags().remove(this.safeText(name))));
                    break;
                }
                case "checkpoint":
                    b.saveState(this.text(c.arg("name")));
                    break;
                case "restore":
                    b.gotoSaveState(this.text(c.arg("name")));
                    break;
                case "ensure active":
                    b.ensureScriptActive(this.scriptIds(c));
                    break;
                case "ensure inactive":
                    b.ensureScriptInactive(this.scriptIds(c));
                    break;
                case "log": {
                    Ast.Expr text = c.arg("text");
                    b.thenRun(() -> SprauteRuntime.get().log(this.script.getId(), this.safeText(text)));
                    break;
                }
                default:
                    AddonContext.Command addon = Addons.command(c.key);
                    if (addon != null) {
                        this.addonCommand(b, c, addon);
                        return;
                    }

                    EventDefs.Def ev = c.key.startsWith("wait ") ? EventDefs.get(c.key.substring(5)) : null;
                    if (ev == null) {
                        throw new SprauteRuntimeException("Команда '" + c.key + "' не поддерживается рантаймом");
                    }

                    this.waitEvent(b, ev, c.arg("what"));
            }
        }
    }

    private static JsonObject op(String name) {
        JsonObject o = new JsonObject();
        o.addProperty("op", name);
        return o;
    }

    private void camera(JsonObject o) {
        this.script.cameraOp(o);
        SprauteNetwork.sendToAll(new Packets.CameraOp(this.script.getId(), o.toString()));
    }

    private void timing(JsonObject o, Ast.CommandStmt c) {
        if (c.arg("time") != null) {
            o.addProperty("time", this.ticks(c.arg("time")) / 20.0);
        }

        Ast.Expr ease = c.arg("ease");
        if (ease != null) {
            o.addProperty("ease", ease instanceof Ast.Ident id ? id.name : this.text(ease));
        }
    }

    private void lookTarget(JsonObject o, Ast.Expr target) {
        Entity en = this.entityOf(target);
        if (en != null) {
            o.addProperty("lookEntity", en.getId());
        } else {
            Location l = this.positionSafe(target);
            o.addProperty("lookX", l.x);
            o.addProperty("lookY", l.y);
            o.addProperty("lookZ", l.z);
        }
    }

    private ResourceLocation skinTexture(String value) {
        String v = value.trim();
        if (v.isEmpty() || v.equals("reset") || v.equals("default") || v.equals("none")) {
            return null;
        } else if (v.contains(":")) {
            ResourceLocation rl = ResourceLocation.tryParse(v);
            if (rl == null) {
                throw new SprauteRuntimeException("Неверный id текстуры '" + v + "'");
            } else {
                return rl;
            }
        } else {
            return Images.texture(this.script.deployed.project, v);
        }
    }

    private void skin(ScriptPerformerBuilder b, Ast.Expr who, Ast.Expr skinE) {
        boolean fixed = skinE instanceof Ast.StringLit s && !s.value.contains("{");
        if (who instanceof Ast.Ident id && id.name.equals("player")) {
            if (fixed) {
                ResourceLocation rl = this.skinTexture(((Ast.StringLit)skinE).value);
                if (rl == null) {
                    b.playerTextureReset();
                } else {
                    b.playerTextureOverride(rl);
                }
            } else {
                b.thenRun(this.guard(() -> {
                    ResourceLocation rl = this.skinTexture(this.text(skinE));
                    PlayerTexture.set(SprauteRuntime.player(), rl);
                }));
            }
        } else if (who instanceof Ast.Ident id && this.script.characters.containsKey(id.name)) {
            Character ch = this.script.characters.get(id.name);
            if (fixed) {
                b.entityTextureOverride(ch, this.skinTexture(((Ast.StringLit)skinE).value));
            } else {
                b.thenRun(this.guard(() -> {
                    ResourceLocation rl = this.skinTexture(this.text(skinE));
                    if (this.script.optional(ch).isPresent()) {
                        Characters.state(this.script, ch, "texture", en -> {
                            if (en instanceof AnimatedEntity ae) {
                                ae.setTextureOverride(rl);
                            }
                        });
                    } else {
                        throw new SprauteRuntimeException("skin: персонаж " + id.name + " ещё не появился (spawn)");
                    }
                }));
            }
        } else {
            throw new SprauteRuntimeException("skin: укажите персонажа (character) или player");
        }
    }

    private long timeOfDay(Ast.Expr e) {
        Object v = e instanceof Ast.Ident id ? id.name : this.eval.eval(e);
        if (v instanceof String s) {
            if (s.trim().matches("\\d{1,2}:\\d{2}")) {
                String[] hm = s.trim().split(":");
                return Math.floorMod((Integer.parseInt(hm[0]) - 6) * 1000L + Integer.parseInt(hm[1]) * 1000L / 60L, 24000L);
            } else {
                String hm = s.trim().toLowerCase();

                return switch (hm) {
                    case "sunrise", "dawn" -> 0L;
                    case "day", "morning" -> 1000L;
                    case "noon" -> 6000L;
                    case "sunset", "evening" -> 12000L;
                    case "night" -> 13000L;
                    case "midnight" -> 18000L;
                    default -> (long)Eval.num(s);
                };
            }
        } else {
            return (long)Eval.num(v);
        }
    }

    private Entity entityOf(Ast.Expr e) {
        if (e instanceof Ast.Ident id) {
            if (id.name.equals("player")) {
                return SprauteRuntime.player();
            } else if (id.name.equals("actor") && !this.script.vars.containsKey("actor")) {
                return this.eval.actor();
            } else {
                Character<?> ch = this.script.characters.get(id.name);
                if (ch != null) {
                    return (Entity)this.script.optional(ch).orElse(null);
                } else {
                    return this.script.vars.get(id.name) instanceof EntityRef r ? r.get() : null;
                }
            }
        } else if (e instanceof Ast.StringLit s) {
            return this.eval.nearestMob(s.value);
        } else {
            if (e instanceof Ast.Member || e instanceof Ast.Call || e instanceof Ast.Index) {
                try {
                    if (this.eval.ref(e) instanceof EntityRef r) {
                        return r.get();
                    }
                } catch (SprauteRuntimeException var6) {
                }
            }

            return null;
        }
    }

    private void waitEvent(ScriptPerformerBuilder b, EventDefs.Def ev, Ast.Expr what) {
        if (ev.name().equals("forge") && what != null) {
            b.thenRun(this.guard(() -> Platform.get().listenLoaderEvent(this.text(what))));
        }

        this.waitLog(b, ev.name(), () -> this.eventFilter(ev, what));
    }

    private void waitLog(ScriptPerformerBuilder b, String type, Supplier<Predicate<Map<String, Object>>> makeFilter) {
        boolean persistent = this.handlerWait;
        long[] cursor = new long[]{-1L};
        Predicate<Map<String, Object>>[] filter = new Predicate[1];
        b.thenRun(() -> {
            if (!persistent || cursor[0] < 0L) {
                cursor[0] = this.script.events.seq();
            }

            filter[0] = makeFilter.get();
        });
        b.thenWaitUntil(() -> {
            EventLog.Entry e = this.script.events.find(type, cursor[0], filter[0] == null ? d -> true : filter[0]);
            if (e == null) {
                return false;
            } else {
                cursor[0] = e.seq();
                this.script.event = e.data();
                return true;
            }
        });
    }

    private Predicate<Map<String, Object>> eventFilter(EventDefs.Def ev, Ast.Expr what) {
        if (what != null && ev.filter() != null) {
            if (ev.filter() == ParamType.TARGET && what instanceof Ast.Ident id) {
                if (id.name.equals("player")) {
                    return d -> "player".equals(d.get("entity"));
                } else {
                    Object held = this.script.vars.get(id.name);
                    if (held instanceof EntityRef er) {
                        return d -> er.equals(d.get("ref"));
                    } else {
                        return held instanceof List<?> list ? d -> list.contains(d.get("ref")) : d -> id.name.equals(d.get("character"));
                    }
                }
            } else {
                String v;
                try {
                    v = this.text(what).trim();
                } catch (SprauteRuntimeException var6) {
                    SprauteRuntime.get().report(this.script.getId(), var6.getMessage());
                    return d -> false;
                }

                if (ev.filter() != ParamType.TARGET || !v.equals("mob") && !v.equals("any")) {
                    String full = v.contains(":") ? v : "minecraft:" + v;
                    List<String> keys = ev.matches();
                    return d -> {
                        for (String k : keys) {
                            Object x = d.get(k);
                            if (x != null && (x.toString().equals(v) || x.toString().equals(full))) {
                                return true;
                            }
                        }

                        return false;
                    };
                } else {
                    return d -> !"player".equals(d.get("entity")) && !"".equals(d.get("entity"));
                }
            }
        } else {
            return d -> true;
        }
    }

    private void timeTask(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        Ast.Expr idE = c.arg("id");
        Ast.Expr prE = c.arg("priority");
        Ast.Expr spE = c.arg("speed");
        Ast.Expr unE = c.arg("until");
        b.thenRun(this.guard(() -> {
            BooleanSupplier until = unE == null ? null : () -> {
                try {
                    return Eval.truthy(this.eval.eval(unE));
                } catch (SprauteRuntimeException var3x) {
                    return true;
                }
            };
            WorldClock.addTask(this.text(idE), (int)this.number(prE, 1.0), this.number(spE, 1.0), until);
        }));
    }

    private void addonCommand(ScriptPerformerBuilder b, final Ast.CommandStmt c, AddonContext.Command run) {
        BooleanSupplier[] waiting = new BooleanSupplier[1];
        var call = new StoryCall() {
            private Ast.Expr arg(String name) {
                return c.arg(name);
            }

            @Override
            public boolean has(String name) {
                return this.arg(name) != null;
            }

            @Override
            public String text(String name) {
                return this.arg(name) != null ? Emitter.this.text(this.arg(name)) : "";
            }

            @Override
            public double number(String name, double fallback) {
                return Emitter.this.number(this.arg(name), fallback);
            }

            @Override
            public int ticks(String name, int fallback) {
                return this.arg(name) != null ? Emitter.this.ticks(this.arg(name)) : fallback;
            }

            @Override
            public Object value(String name) {
                return this.arg(name) != null ? Emitter.this.eval.eval(this.arg(name)) : null;
            }

            @Override
            public Location position(String name) {
                return Emitter.this.eval.position(this.arg(name));
            }

            @Override
            public Entity entity(String name) {
                return this.arg(name) != null ? Emitter.this.entityOf(this.arg(name)) : null;
            }

            @Override
            public ServerPlayer player() {
                return SprauteRuntime.player();
            }

            @Override
            public int scriptId() {
                return Emitter.this.script.getId();
            }

            @Override
            public String project() {
                return Emitter.this.script.deployed.project;
            }
        };
        b.thenRun(this.guard(() -> {
            waiting[0] = null;

            try {
                waiting[0] = run.run(call);
            } catch (SprauteRuntimeException var5x) {
                throw var5x;
            } catch (RuntimeException var6) {
                SprauteEngine.LOGGER.error("Spraute: an addon's command {} failed", c.key, var6);
                throw new SprauteRuntimeException(c.key + ": ошибка в аддоне: " + var6.getMessage());
            }
        }));
        b.thenWaitUntil(() -> waiting[0] == null || waiting[0].getAsBoolean());
    }

    private ServerLevel storyLevel() {
        ServerPlayer p = SprauteRuntime.player();
        return p != null ? Mc.serverLevel(p) : Platform.get().server().overworld();
    }

    private PanelLook panelLook(Ast.CommandStmt c) {
        Map<String, String> written = new HashMap<>();

        for (String key : PanelLook.VALUES) {
            Ast.Expr e = c.arg(key);
            if (e != null) {
                written.put(key, this.safeText(e));
            }
        }

        for (String keyx : PanelLook.FLAGS) {
            if (c.arg(keyx) != null) {
                written.put(keyx, "да");
            }
        }

        return PanelLook.of(written::get);
    }

    private void guiUpdate(ScriptPerformerBuilder b, Ast.Expr el, String prop, Ast.Expr value) {
        b.thenRun(this.guard(() -> this.sendGuiUpdate(this.text(el), prop, this.safeText(value))));
    }

    private void guiUpdateConst(ScriptPerformerBuilder b, Ast.Expr el, String prop, String value) {
        b.thenRun(this.guard(() -> this.sendGuiUpdate(this.text(el), prop, value)));
    }

    private void sendGuiUpdate(String element, String prop, String value) {
        Packets.GuiUpdate update = new Packets.GuiUpdate(this.script.getId(), element, prop, value);
        if (WorldPanels.has(this.script.getId())) {
            SprauteNetwork.sendToAll(update);
        } else {
            ServerPlayer p = SprauteRuntime.player();
            if (p != null) {
                SprauteNetwork.sendTo(p, update);
            }
        }
    }

    private void animation(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        Character ch = this.animated(b, c);
        if (ch != null) {
            Animation a = null;
            if (c.arg("who") instanceof Ast.Ident wid && this.script.characterModels.containsKey(wid.name)) {
                EntityLibrary.Look look = EntityLibrary.look(this.script.characterModels.get(wid.name));
                if (look != null) {
                    a = look.animation(this.text(c.arg("anim"))).orElse(null);
                }
            }

            if (a == null) {
                a = this.anim(b, c.arg("anim"));
            }

            if (a != null) {
                boolean face = c.key.equals("face");
                String layer = face ? "emotion" : this.layer(c.arg("layer"), "action");
                double speed = this.number(c.arg("speed"), 1.0);
                int times = c.arg("times") != null ? Math.max(1, (int)this.number(c.arg("times"), 1.0)) : 0;
                ConfiguredAnimation ca = a.configure().transitionTime(this.fadeSeconds(c, 0.25F));
                if (c.arg("from") != null) {
                    ca.startTime(this.ticks(c.arg("from")) / 20.0F);
                }

                if (speed != 1.0) {
                    ca.speed((float)speed);
                }

                if (c.arg("loop") != null || times > 1) {
                    ca.loopMode(LoopMode.LOOP);
                } else if (c.arg("hold") != null) {
                    ca.loopMode(LoopMode.HOLD_ON_LAST_FRAME);
                }

                b.setAnimation(ch, layer, ca);
                int length = Math.max(1, Anims.lengthTicks(a, speed));
                if (times > 0) {
                    b.thenDelay(length * times);
                    ConfiguredAnimation none = Animation.NULL.configure().transitionTime(0.2F);
                    b.setAnimation(ch, layer, none);
                } else if (c.arg("wait") != null && c.arg("loop") == null) {
                    b.thenDelay(length);
                }
            }
        }
    }

    private int argb(Ast.Expr colorE, Ast.Expr alphaE, int rgb, float alpha) {
        Integer c = colorE != null ? Colors.parse(this.safeText(colorE)) : null;
        float a = alphaE != null ? (float)Math.max(0.0, Math.min(1.0, this.number(alphaE, alpha))) : alpha;
        return Math.round(a * 255.0F) << 24 | (c != null ? c : rgb) & 16777215;
    }

    private JsonElement gizmoPoint(Ast.Expr e) {
        if (this.eval.ref(e) instanceof EntityRef er) {
            JsonObject o = new JsonObject();
            o.addProperty("e", er.uuid.toString());
            return o;
        } else {
            Location l = this.positionSafe(e);
            JsonArray a = new JsonArray();
            a.add(l.x);
            a.add(l.y);
            a.add(l.z);
            return a;
        }
    }

    private String highlightKey(Ast.Expr who) {
        if (this.eval.ref(who) instanceof EntityRef er) {
            return "hl:" + er.uuid;
        } else {
            Location l = this.positionSafe(who);
            return "hl:" + (int)Math.floor(l.x) + "," + (int)Math.floor(l.y) + "," + (int)Math.floor(l.z);
        }
    }

    private void gizmo(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        String key = c.key;
        b.thenRun(this.guard(() -> {
            int sid = this.script.getId();
            Ast.Expr idE = c.arg("id");
            switch (key) {
                case "highlight off":
                    SprauteNetwork.sendToAll(new Packets.GizmoRemove(sid, this.highlightKey(c.arg("who"))));
                    return;
                case "highlight clear":
                case "gizmo clear":
                    String what = idE != null ? this.safeText(idE) : (key.startsWith("highlight") ? "#highlight" : "#gizmo");
                    SprauteNetwork.sendToAll(new Packets.GizmoRemove(sid, what));
                    return;
                default:
                    JsonObject d = new JsonObject();
                    String id;
                    if (key.equals("highlight")) {
                        Ast.Expr who = c.arg("who");
                        d.addProperty("kind", "highlight");
                        if (this.eval.ref(who) instanceof EntityRef er) {
                            d.addProperty("entity", er.uuid.toString());
                        } else {
                            Location l = this.positionSafe(who);
                            JsonArray bp = new JsonArray();
                            bp.add((int)Math.floor(l.x));
                            bp.add((int)Math.floor(l.y));
                            bp.add((int)Math.floor(l.z));
                            d.add("block", bp);
                        }

                        d.addProperty("fill", this.argb(c.arg("fill"), c.arg("fillalpha"), 16777215, 0.4F));
                        d.addProperty("outline", this.argb(c.arg("outline"), c.arg("outlinealpha"), 16777215, 1.0F));
                        d.addProperty("width", (float)Math.max(0.005, Math.min(1.0, this.number(c.arg("width"), 0.05))));
                        d.addProperty("through", c.arg("occluded") == null);
                        id = idE != null ? this.safeText(idE) : this.highlightKey(who);
                    } else {
                        d.addProperty("kind", key.substring("gizmo ".length()).replace(' ', '_'));
                        d.addProperty("color", this.argb(c.arg("color"), c.arg("alpha"), 16777215, 1.0F));
                        if (c.arg("fill") != null) {
                            d.addProperty("fill", this.argb(c.arg("fill"), c.arg("fillalpha"), 16777215, 0.25F));
                        }

                        if (c.arg("width") != null) {
                            d.addProperty("width", (float)this.number(c.arg("width"), 2.0));
                        }

                        if (c.arg("scale") != null) {
                            d.addProperty("scale", (float)this.number(c.arg("scale"), 1.0));
                        }

                        if (c.arg("radius") != null) {
                            d.addProperty("radius", (float)this.number(c.arg("radius"), 1.0));
                        }

                        if (c.arg("text") != null) {
                            d.addProperty("text", this.safeText(c.arg("text")));
                        }

                        d.addProperty("through", c.arg("through") != null);
                        if (c.arg("from") != null) {
                            d.add("a", this.gizmoPoint(c.arg("from")));
                        }

                        if (c.arg("to") != null) {
                            d.add("b", this.gizmoPoint(c.arg("to")));
                        }

                        if (c.arg("at") != null) {
                            d.add("a", this.gizmoPoint(c.arg("at")));
                        }

                        for (String n : new String[]{"x", "y", "w", "h", "x2", "y2"}) {
                            if (c.arg(n) != null) {
                                d.addProperty(n, (float)this.number(c.arg(n), 0.0));
                            }
                        }

                        id = idE != null ? this.safeText(idE) : "~" + this.gizmoSeq++;
                    }

                    if (c.arg("time") != null) {
                        d.addProperty("time", this.ticks(c.arg("time")));
                    }

                    SprauteNetwork.sendToAll(new Packets.GizmoSet(sid, id, d.toString()));
            }
        }));
    }

    private void playerAnimation(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        Ast.Expr who = c.arg("who");
        if (c.key.equals("playeranim stop")) {
            float fade = this.fadeSeconds(c, 0.25F);
            b.thenRun(this.guard(() -> sendPlayerAnim(this.eval.ref(who), "", 1.0F, fade, 1, 0)));
        } else {
            Animation a = this.anim(b, c.arg("anim"));
            if (a != null) {
                String id = this.text(c.arg("anim"));
                double speed = this.number(c.arg("speed"), 1.0);
                float fade = this.fadeSeconds(c, 0.15F);
                int times = c.arg("times") != null ? Math.max(1, (int)this.number(c.arg("times"), 1.0)) : 1;
                int mode = c.arg("loop") != null ? 1 : (c.arg("hold") != null ? 2 : 0);
                b.thenRun(this.guard(() -> sendPlayerAnim(this.eval.ref(who), id, (float)speed, fade, times, mode)));
                if (mode == 0 && c.arg("wait") != null) {
                    b.thenDelay(Math.max(1, Anims.lengthTicks(a, speed)) * times);
                }
            }
        }
    }

    private static void sendPlayerAnim(Object who, String anim, float speed, float fade, int times, int mode) {
        if ((who instanceof EntityRef r ? r.get() : null) instanceof ServerPlayer p) {
            SprauteNetwork.sendToEveryone(new Packets.PlayerAnim(p.getUUID(), anim, speed, fade, times, mode));
        } else {
            throw new SprauteRuntimeException("playeranim: нужен игрок, а не " + who);
        }
    }

    private Character animated(ScriptPerformerBuilder b, Ast.CommandStmt c) {
        Ast.Expr who = c.arg("who");
        Character ch = this.character(who);
        EntityType<?> type = ch.getType();
        String key = String.valueOf(Regs.ENTITY_TYPE.getKey(type));
        Boolean ok = ANIMATED_TYPES.get(key);
        if (ok == null) {
            ok = true;
            MinecraftServer server = Platform.get().server();
            if (server != null) {
                try {
                    Entity probe = type.create(server.overworld());
                    ok = probe instanceof AnimatedEntity;
                    if (probe != null) {
                        probe.discard();
                    }
                } catch (RuntimeException var10) {
                }
            }

            ANIMATED_TYPES.put(key, ok);
        }

        if (!ok) {
            String name = who instanceof Ast.Ident id ? id.name : "?";
            b.thenRun(
                () -> SprauteRuntime.get()
                    .report(this.script.getId(), "Сущность персонажа " + name + " (" + key + ") не поддерживает анимации — команда " + c.key + " пропущена")
            );
            return null;
        } else {
            return ch;
        }
    }

    private Animation anim(ScriptPerformerBuilder b, Ast.Expr e) {
        String id = this.text(e);
        Animation a = Anims.resolve(id).orElse(null);
        if (a == null) {
            b.thenRun(() -> SprauteRuntime.get().report(this.script.getId(), "Анимация '" + id + "' не найдена — команда пропущена"));
        }

        return a;
    }

    private String layer(Ast.Expr e, String fallback) {
        if (e == null) {
            return fallback;
        } else {
            String raw = e instanceof Ast.NumberLit n ? String.valueOf((int)n.value) : this.text(e);
            String name = Specs.layerName(raw);
            if (name == null) {
                throw new SprauteRuntimeException("Неизвестный слой анимации '" + raw + "'");
            } else {
                return name;
            }
        }
    }

    private float fadeSeconds(Ast.CommandStmt c, float fallback) {
        Ast.Expr f = c.arg("fade");
        return f == null ? fallback : this.ticks(f) / 20.0F;
    }

    private Runnable guard(Runnable r) {
        return r;
    }

    private int speakerSlot(ScriptPerformerBuilder b, Ast.Expr who) {
        if (who instanceof Ast.NumberLit n) {
            return (int)n.value;
        } else {
            String key;
            if (who instanceof Ast.Ident id && this.script.characters.containsKey(id.name)) {
                key = "char:" + id.name;
            } else if (who instanceof Ast.Ident id && id.name.equals("player")) {
                key = "player";
            } else {
                key = "name:" + this.text(who);
            }

            Integer slot = this.speakerSlots.get(key);
            int s;
            if (slot != null) {
                s = slot;
            } else {
                s = this.nextSlot;
                this.nextSlot = (this.nextSlot + 1) % 2;
                this.speakerSlots.values().removeIf(v -> v == s);
                this.speakerSlots.put(key, s);
            }

            if (key.startsWith("char:")) {
                this.speechEntity(b, s, key.substring(5));
            } else if (key.equals("player")) {
                this.speechPlayer(b, s);
            } else {
                b.thenSetSpeechEntity(s, key.substring(5));
            }

            return s;
        }
    }

    private void speechEntity(ScriptPerformerBuilder b, int slot, String name) {
        Character ch = this.character(name);
        this.headSync(b, name);
        String icon = this.script.speechIcons.getOrDefault(name, "generic_man");
        b.append(p -> new SpeechNameAction(p, slot, () -> this.script.speechNames.getOrDefault(name, ch.getName()), () -> this.nameColor(name), icon));
    }

    private void headSync(ScriptPerformerBuilder b, String name) {
        if (this.script.headSpeaker(name)) {
            b.thenRun(
                () -> {
                    ServerPlayer p = SprauteRuntime.player();
                    Character<?> ch = this.script.characters.get(name);
                    Entity en = ch != null ? (Entity)this.script.optional(ch).orElse(null) : null;
                    String modelKey = this.script.characterModels.get(name);
                    if (modelKey != null && en instanceof SprauteModelEntity me && !modelKey.equals(me.model())) {
                        me.setModel(modelKey);
                    }

                    if (en != null || modelKey != null) {
                        SprauteNetwork.sendToAll(
                            new Packets.SpeechHead(this.script.getId(), Images.slug(name), en != null ? en.getId() : -1, modelKey != null ? modelKey : "")
                        );
                    }
                }
            );
        }
    }

    private Integer nameColor(String key) {
        Integer own = this.script.nameColors.get(key);
        if (own != null) {
            return own;
        } else {
            int all = EngineServer.config().chatLog.nameColor;
            return all < 0 ? null : all & 16777215;
        }
    }

    private void speechPlayer(ScriptPerformerBuilder b, int slot) {
        b.append(pf -> new SpeechNameAction(pf, slot, () -> {
            String own = this.script.speechNames.get("player");
            if (own != null) {
                return own;
            } else {
                ServerPlayer p = SprauteRuntime.player();
                return p != null ? p.getGameProfile().getName() : "Игрок";
            }
        }, () -> this.nameColor("player"), playerHead()));
    }

    private static String playerHead() {
        EngineConfig.ChatLog c = EngineServer.config().chatLog;
        return "spraute/head/player/" + Math.round(Math.max(0.2F, c.playerHeadScale) * 100.0F) + "/" + c.playerHeadX + "/" + c.playerHeadY;
    }

    private Character character(Ast.Expr e) {
        if (e instanceof Ast.Ident id) {
            return this.character(id.name);
        } else {
            throw new SprauteRuntimeException("Ожидался персонаж");
        }
    }

    private Character character(String name) {
        Character<?> c = this.script.characters.get(name);
        if (c == null) {
            throw new SprauteRuntimeException("Персонаж '" + name + "' не объявлен");
        } else {
            return c;
        }
    }

    private Supplier<Location> eyesSupplier(Ast.Expr e) {
        return () -> {
            try {
                Location l = this.eval.eyes(e);
                if (l != null) {
                    return l;
                }
            } catch (SprauteRuntimeException var3) {
                SprauteRuntime.get().report(this.script.getId(), var3.getMessage());
            }

            return this.positionSafe(e);
        };
    }

    private Supplier<Location> locationSupplier(Ast.Expr e) {
        return () -> this.positionSafe(e);
    }

    private Entity bossBody(Ast.Expr who, String cmd) {
        if (this.eval.ref(who) instanceof EntityRef r) {
            return r.require();
        } else {
            throw new SprauteRuntimeException(cmd + ": нужен персонаж, игрок или существо, а не точка");
        }
    }

    private BossMoves.Fx bossFx(Ast.Expr e) {
        return e == null ? null : new BossMoves.Fx(this.fxJson(this.text(e).trim()), this.script.deployed.project, this.script.getId(), 1.0F);
    }

    private String fxJson(String n) {
        String json = SprauteRuntime.get().gui(this.script.deployed.project, "fx:" + n);
        if (json != null) {
            return json;
        } else {
            FxDef preset = FxPresets.get(n);
            if (preset == null) {
                throw new SprauteRuntimeException(
                    "Эффект «" + n + "» не найден: его нет в папке «Эффекты» проекта и среди встроенных (" + String.join(", ", FxPresets.names()) + ")"
                );
            } else {
                return preset.toJson();
            }
        }
    }

    private PathDef pathNamed(String n) {
        String json = SprauteRuntime.get().gui(this.script.deployed.project, "path:" + n);
        if (json == null) {
            Map<String, String> m = SprauteRuntime.get().allGuis().get(this.script.deployed.project);
            if (m != null) {
                for (Entry<String, String> en : m.entrySet()) {
                    if (en.getKey().equalsIgnoreCase("path:" + n)) {
                        json = en.getValue();
                    }
                }
            }
        }

        if (json == null) {
            throw new SprauteRuntimeException("Путь «" + n + "» не найден в папке «Пути» проекта");
        } else {
            PathDef d = PathDef.fromJson(json);
            if (d.points.isEmpty()) {
                throw new SprauteRuntimeException("В пути «" + n + "» нет точек");
            } else {
                return d;
            }
        }
    }

    private Location positionSafe(Ast.Expr e) {
        try {
            Location l = this.eval.position(e);
            if (l != null) {
                return l;
            }
        } catch (SprauteRuntimeException var3) {
            SprauteRuntime.get().report(this.script.getId(), var3.getMessage());
        }

        ServerPlayer p = SprauteRuntime.player();
        return p != null ? new Location(p) : Location.ZERO;
    }

    private BlockPos blockPos(Ast.Expr e) {
        return Mc.blockPos(this.eval.position(e));
    }

    private AABB box(Ast.Expr from, Ast.Expr to) {
        Location a = this.eval.position(from);
        Location b = this.eval.position(to);
        return new AABB(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    private String text(Ast.Expr e) {
        return e instanceof Ast.Ident id && !this.script.vars.containsKey(id.name) ? id.name : Eval.str(this.eval.eval(e));
    }

    private String safeText(Ast.Expr e) {
        try {
            return Eval.str(this.eval.eval(e));
        } catch (SprauteRuntimeException var3) {
            SprauteRuntime.get().report(this.script.getId(), var3.getMessage());
            return "?";
        }
    }

    private int ticks(Ast.Expr e) {
        return e instanceof Ast.DurationLit d ? d.ticks : (int)Math.round(Eval.num(this.eval.eval(e)));
    }

    private int ticksSafe(Ast.Expr e) {
        try {
            return this.ticks(e);
        } catch (SprauteRuntimeException var3) {
            SprauteRuntime.get().report(this.script.getId(), var3.getMessage());
            return 0;
        }
    }

    private double number(Ast.Expr e, double fallback) {
        return e == null ? fallback : Eval.num(this.eval.eval(e));
    }

    private ResourceLocation id(Ast.Expr e) {
        String s = this.text(e);
        return new ResourceLocation(s.contains(":") ? s : "minecraft:" + s);
    }

    private ItemStack itemToSpawn(String command, String spec, Ast.Expr countE) {
        if (ItemSpec.error(spec) == null && ItemSpec.item(spec) != Items.AIR) {
            int count = countE != null ? (int)this.number(countE, 1.0) : 1;
            if (count < 1) {
                throw new SprauteRuntimeException(command + ": count должен быть больше нуля");
            } else {
                return ItemSpec.stack(spec, count);
            }
        } else {
            throw new SprauteRuntimeException(command + ": предмет " + spec + " не найден или имеет неверный NBT");
        }
    }

    private void keepInVariable(Ast.Expr into, List<EntityRef> made) {
        if (into instanceof Ast.Ident v && !made.isEmpty()) {
            if (this.script.vars.get(v.name) instanceof List<?> l) {
                ((List<Object>)l).addAll(made);
            } else {
                this.script.vars.put(v.name, made.get(made.size() - 1));
            }
        }
    }

    private ItemStack itemStack(Ast.Expr e, int count) {
        String spec = this.text(e);
        if (ItemSpec.error(spec) != null) {
            throw new SprauteRuntimeException("Предмет " + spec + ": ошибка в NBT: " + ItemSpec.error(spec));
        } else {
            ItemStack stack = ItemSpec.stack(spec, count);
            if (stack.isEmpty()) {
                throw new SprauteRuntimeException("Предмет " + spec + " не найден");
            } else {
                return stack;
            }
        }
    }

    private String emotionName(Ast.Expr e) {
        return (e instanceof Ast.Ident id ? id.name : this.text(e)).toUpperCase(Locale.ROOT);
    }

    private int[] scriptIds(Ast.CommandStmt c) {
        List<Integer> ids = new ArrayList<>();

        for (Ast.Arg a : c.args) {
            if ("ids".equals(a.param)) {
                ids.add((int)Eval.num(this.eval.eval(a.value)));
            }
        }

        return ids.stream().mapToInt(Integer::intValue).toArray();
    }

    private final class Ctx {
        final String id;
        final String end = Emitter.this.freshLabel("end");
        final LinkedHashSet<String> used = new LinkedHashSet<>();

        Ctx(String id) {
            this.id = id;
        }
    }
}
