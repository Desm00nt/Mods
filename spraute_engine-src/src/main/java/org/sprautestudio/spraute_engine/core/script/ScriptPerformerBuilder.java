package org.sprautestudio.spraute_engine.core.script;

import java.util.Locale;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.core.anim.MovementActionType;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.system.PlayerTexture;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class ScriptPerformerBuilder {
    private final ScriptInstance script;
    private final ScriptPerformer performer;
    private boolean onBecoming;

    private ScriptPerformerBuilder(ScriptInstance script) {
        this.script = script;
        this.performer = new ScriptPerformer(script);
    }

    public static ScriptPerformerBuilder builder(ScriptInstance script) {
        return new ScriptPerformerBuilder(script);
    }

    public ScriptPerformer build() {
        return this.performer;
    }

    public void build(Consumer<ScriptPerformer> to) {
        to.accept(this.performer);
    }

    public int mark() {
        return this.performer.actions.size();
    }

    public void words(int from, String line) {
        for (int i = from; i < this.performer.actions.size(); i++) {
            this.performer.words.putIfAbsent(i, line);
        }
    }

    public ScriptPerformerBuilder append(Function<ScriptPerformer, ScriptPerformerAction> make) {
        this.performer.actions.add(make.apply(this.performer));
        return this;
    }

    public void thenRun(Runnable r) {
        this.append(p -> new ScriptPerformerBuilder.Run(p, r));
    }

    public void thenAs(Supplier<ServerPlayer> who, String end) {
        this.append(p -> new ScriptPerformerBuilder.Run(p, () -> {
            ServerPlayer player = who.get();
            if (player != null) {
                p.enterAs(player.getUUID(), end);
            } else {
                p.jumpTo(end);
            }
        }));
    }

    public void thenAsLeave(int blocks) {
        this.append(p -> new ScriptPerformerBuilder.Run(p, () -> p.leaveAs(blocks)));
    }

    public void thenAsReset() {
        this.append(p -> new ScriptPerformerBuilder.Run(p, p::resetAs));
    }

    public void onPlayerGone(String label) {
        this.performer.goneLabel = label;
    }

    public void label(String name) {
        if (name.equals("начало") || name.equals("конец")) {
            throw new IllegalArgumentException("Имя метки «" + name + "» зарезервировано");
        } else if (this.performer.labels.containsKey(name)) {
            throw new IllegalArgumentException("Метка «" + name + "» объявлена дважды");
        } else {
            this.performer.labels.put(name, this.performer.actions.size());
            this.performer.labelPointsNames.add(name);
        }
    }

    public void gotoLabel(String label, boolean ignored) {
        this.thenRun(() -> this.performer.jumpTo(label));
    }

    public void gotoLabel(Supplier<String> label, boolean ignored) {
        this.thenRun(() -> this.performer.jumpTo(label.get()));
    }

    public void onBecoming(boolean on) {
        this.onBecoming = on;
    }

    private BooleanSupplier cond(BooleanSupplier done) {
        if (!this.onBecoming) {
            return done;
        } else {
            boolean[] was = new boolean[]{false};
            return () -> {
                boolean now = done.getAsBoolean();
                boolean became = now && !was[0];
                was[0] = now;
                return became;
            };
        }
    }

    public void thenWaitUntil(BooleanSupplier done) {
        BooleanSupplier c = this.cond(done);
        this.append(p -> new ScriptPerformerBuilder.Wait(p, null, c, "Ждёт условие"));
    }

    private void waitWith(String words, Runnable begin, BooleanSupplier done) {
        BooleanSupplier c = this.cond(done);
        this.append(p -> new ScriptPerformerBuilder.Wait(p, begin, c, words));
    }

    public void thenPassing(Runnable begin, BooleanSupplier done) {
        this.append(p -> new ScriptPerformerBuilder.Wait(p, begin, done, "Идёт параллельно") {
            @Override
            public boolean blocking() {
                return false;
            }
        });
    }

    public void thenDelay(int ticks) {
        this.thenDelay(() -> ticks);
    }

    public void thenDelay(IntSupplier ticks) {
        int[] left = new int[]{0};
        this.waitWith("Ждёт время", () -> left[0] = ticks.getAsInt(), () -> left[0]-- <= 0);
    }

    public void thenDelayRandom(int min, int max, Random random) {
        this.thenDelay(() -> max <= min ? min : min + random.nextInt(max - min + 1));
    }

    public void finish(boolean ignored) {
        this.thenRun(() -> this.script.env.stop(this.script.getId()));
        this.thenWaitUntil(() -> false);
    }

    public ScriptPerformer performer() {
        return this.performer;
    }

    public void createParallelPerformer(String name, ScriptPerformer sub) {
        this.script.known.put(name, sub);
        this.thenRun(() -> {
            sub.restart();
            sub.inheritAs(this.performer.asPlayer);
            this.script.running.put(name, sub);
        });
    }

    public void removeParallelPerformer(String name) {
        this.thenRun(() -> {
            ScriptPerformer p = this.script.running.remove(name);
            if (p != null) {
                p.stop();
            }
        });
    }

    public void saveState(String name) {
        this.label("__state_" + name);
        this.thenRun(() -> this.script.passCheckpoint(name, this.performer));
    }

    public void gotoSaveState(String name) {
        this.thenRun(() -> Characters.rollback(this.script.getId()));
        this.gotoLabel("__state_" + name, true);
    }

    public void ensureScriptActive(int[] ids) {
        this.thenRun(() -> {
            for (int id : ids) {
                this.script.env.start(id);
            }
        });
    }

    public void ensureScriptInactive(int[] ids) {
        this.thenRun(() -> {
            for (int id : ids) {
                this.script.env.stop(id);
            }
        });
    }

    public void thenWaitForFlag(String name) {
        BooleanSupplier c = this.cond(() -> this.script.getFlags().has(name));
        this.append(p -> new ScriptPerformerBuilder.Wait(p, null, c, "Ждёт флаг " + name));
    }

    public void thenWaitForKey(String key) {
        long[] from = new long[]{0L};
        this.waitWith("Ждёт нажатия " + key.toUpperCase(Locale.ROOT), () -> from[0] = this.script.keyCount(key), () -> this.script.keyCount(key) > from[0]);
    }

    public void thenWaitForEachKey(String key) {
        long[] seen = new long[]{-1L};
        this.waitWith("Ждёт нажатия " + key.toUpperCase(Locale.ROOT), () -> {
            if (seen[0] < 0L) {
                seen[0] = this.script.keyCount(key);
            }
        }, () -> {
            if (this.script.keyCount(key) <= seen[0]) {
                return false;
            } else {
                seen[0]++;
                return true;
            }
        });
    }

    public void thenWaitForTrigger() {
        int[] from = new int[]{0};
        this.waitWith("Ждёт триггер", () -> from[0] = this.script.triggerSeq, () -> this.script.triggerSeq > from[0]);
    }

    public void thenAwaitEntityClick(Character<?> c) {
        long[] from = new long[]{0L};
        this.waitWith(
            "Ждёт клика по персонажу " + c.getName(),
            () -> from[0] = Behaviours.clicks((Entity)this.script.optional(c).orElse(null)),
            () -> Behaviours.clicks((Entity)this.script.optional(c).orElse(null)) > from[0]
        );
    }

    public void thenAwaitBlockClick(BlockPos pos) {
        long[] from = new long[]{0L};
        this.waitWith("Ждёт клика по блоку", () -> from[0] = Behaviours.clicks(pos), () -> Behaviours.clicks(pos) > from[0]);
    }

    public void thenWhenPlayerCloseTo(Character<?> c, double radius) {
        this.thenWaitUntil(() -> {
            ServerPlayer p = SprauteRuntime.player();
            Entity e = (Entity)this.script.optional(c).orElse(null);
            return p != null && e != null && Mc.level(p) == Mc.level(e) && p.distanceToSqr(e) <= radius * radius;
        });
    }

    public void thenWhenPlayerCloseTo(Supplier<Location> at, double radius) {
        this.thenWaitUntil(() -> {
            ServerPlayer p = SprauteRuntime.player();
            Location l = at.get();
            return p != null && l != null && p.distanceToSqr(l.x, l.y, l.z) <= radius * radius;
        });
    }

    public void thenWhenPlayerEnterBox(AABB box) {
        this.thenWaitUntil(() -> {
            ServerPlayer p = SprauteRuntime.player();
            return p != null && box.contains(p.position());
        });
    }

    public void thenWhenPlayerInDimension(ResourceLocation dim) {
        this.thenWaitUntil(() -> {
            ServerPlayer p = SprauteRuntime.player();
            return p != null && Mc.level(p).dimension().location().equals(dim);
        });
    }

    public void thenWhenPlayerExitBox(AABB box) {
        this.thenWaitUntil(() -> {
            ServerPlayer p = SprauteRuntime.player();
            return p != null && !box.contains(p.position());
        });
    }

    public void thenWaitCharacterVisible(Character<?> c) {
        this.thenWaitCharacterVisible(c, true);
    }

    public void thenWaitCharacterVisible(Character<?> c, boolean lineOfSight) {
        this.thenWaitUntil(() -> {
            ServerPlayer p = SprauteRuntime.player();
            Entity e = (Entity)this.script.optional(c).orElse(null);
            if (p != null && e != null && Mc.level(p) == Mc.level(e) && (!lineOfSight || p.hasLineOfSight(e))) {
                Vec3 to = e.getEyePosition().subtract(p.getEyePosition()).normalize();
                return p.getViewVector(1.0F).dot(to) > 0.5;
            } else {
                return false;
            }
        });
    }

    private void sendSpeech(String name, Integer color, String icon, String text, String hint, boolean think, boolean wait) {
        this.script.journal.add(new ScriptInstance.Line(name, color != null ? color : -1, icon, text, think));
        SprauteNetwork.sendToAll(new Packets.Speech(this.script.getId(), name, color != null ? color : -1, icon, text, hint, think, wait, false));
    }

    public void thenSetSpeechEntity(int slot, String name) {
        this.thenRun(() -> this.performer.speech().setEntityName(slot, name, "generic_man"));
    }

    public void thenSpeechMessage(int slot, Supplier<Component> hint, Supplier<Component> text, boolean wait) {
        this.thenRun(() -> {
            SpeechState.Speaker s = this.performer.speech().get(slot);
            this.sendSpeech(s.name(), s.color(), s.icon(), text.get().getString(), hint != null ? hint.get().getString() : "", false, wait);
        });
        if (wait) {
            this.thenWaitForKey("f8");
        }
    }

    public void thenThink(Supplier<Component> text, boolean wait) {
        this.thenRun(() -> {
            ServerPlayer p = SprauteRuntime.player();
            this.sendSpeech(p != null ? p.getGameProfile().getName() : "", null, "spraute/head/player/100/0/0", text.get().getString(), "", true, wait);
        });
        if (wait) {
            this.thenWaitForKey("f8");
        }
    }

    public void clearChat() {
        this.thenRun(() -> {
            this.script.journal.clear();
            SprauteNetwork.sendToAll(new Packets.SpeechClear(this.script.getId()));
        });
    }

    public void thenTitle(Title t) {
        this.thenRun(() -> {
            for (ServerPlayer p : SprauteRuntime.audience()) {
                p.connection.send(new ClientboundSetTitlesAnimationPacket(t.fadeIn(), t.stay(), t.fadeOut()));
                if (!t.subtitle().isEmpty()) {
                    p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(t.subtitle())));
                }

                p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(t.text())));
            }
        });
    }

    public void sendWebMessage(String text, boolean ignored) {
        this.thenRun(() -> {
            for (ServerPlayer p : SprauteRuntime.audience()) {
                p.sendSystemMessage(Component.literal(text));
            }
        });
    }

    public void sendWebWarning(String text) {
        this.thenRun(() -> {
            for (ServerPlayer p : SprauteRuntime.audience()) {
                p.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW));
            }
        });
    }

    private void state(Character<?> c, String key, Consumer<Entity> action) {
        this.thenRun(() -> this.keep(c, key, action));
    }

    private void keep(Character<?> c, String key, Consumer<Entity> action) {
        this.script.note(c.getId() + "|" + key, this.performer);
        Characters.state(this.script, c, key, action);
    }

    private void stateAnimated(Character<?> c, String key, Consumer<AnimatedEntity> action) {
        this.state(c, key, e -> {
            if (e instanceof AnimatedEntity ae) {
                action.accept(ae);
            }
        });
    }

    private void act(Character<?> c, Consumer<Entity> action) {
        this.thenRun(() -> Characters.act(this.script, c, action));
    }

    private void actAnimated(Character<?> c, Consumer<AnimatedEntity> action) {
        this.act(c, e -> {
            if (e instanceof AnimatedEntity ae) {
                action.accept(ae);
            }
        });
    }

    public void positionEntity(Character<?> c, Supplier<Location> at) {
        this.thenRun(() -> Characters.place(this.script, c, at.get()));
    }

    public void removeEntity(Character<?> c) {
        this.thenRun(() -> Characters.remove(this.script, c));
    }

    public void freeEntity(Character<?> c) {
        this.thenRun(() -> Characters.free(c));
    }

    public void thenClearMovement(Character<?> c) {
        this.thenRun(() -> {
            Characters.forget(this.script, c, "move");
            Characters.act(this.script, c, e -> Behaviours.stop(e, Behaviours.Type.MOVEMENT));
        });
    }

    public void thenClearBehaviour(Character<?> c, Behaviours.Type type) {
        this.thenRun(() -> {
            if (type == Behaviours.Type.MOVEMENT) {
                Characters.forget(this.script, c, "move");
            }

            if (type == Behaviours.Type.LOOK) {
                Characters.forget(this.script, c, "look");
            }

            Characters.act(this.script, c, e -> Behaviours.stop(e, type));
        });
    }

    public void entityLookAt(Character<?> c, Supplier<Location> at, boolean instant) {
        this.state(c, "look", e -> {
            Supplier<Vec3> target = () -> {
                Location l = at.get();
                return l == null ? null : new Vec3(l.x, l.y, l.z);
            };
            if (e instanceof AnimatedEntity ae) {
                ae.setLookTarget(target, instant);
            } else {
                Vec3 v = target.get();
                if (v != null) {
                    Behaviours.lookOnce(e, v);
                }
            }

            Walkers.lookAt(e, target);
        });
    }

    public void thenWaitLook(Character<?> c) {
        int[] waited = new int[]{0};
        this.waitWith(
            "Поворачивается к цели",
            () -> waited[0] = 0,
            () -> ++waited[0] > 60 || !(this.script.optional(c).orElse(null) instanceof AnimatedEntity ae && !ae.lookSettled())
        );
    }

    public void entityStopLook(Character<?> c) {
        this.thenRun(() -> {
            Characters.forget(this.script, c, "look");
            Characters.act(this.script, c, e -> {
                if (e instanceof AnimatedEntity ae) {
                    ae.setLookTarget(null, false);
                }

                Walkers.lookAt(e, null);
            });
        });
    }

    public void thenEntityFollowEntity(Character<?> who, Character<?> target, double speed, double min) {
        this.state(who, "move", e -> Behaviours.follow(e, () -> (Entity)this.script.optional(target).orElse(null), speed, min, -1.0));
    }

    public void thenEntityFollowPlayer(Character<?> who, double speed, double min, double max) {
        this.state(who, "move", e -> Behaviours.follow(e, SprauteRuntime::player, speed, min, max));
    }

    public void thenEntityRandomWalk(Character<?> who, double speed, int ignored, double radius) {
        this.state(who, "move", e -> Behaviours.wander(e, speed, radius));
    }

    public void entityMoveSpeed(Character<?> who, double speed) {
        this.state(who, "speed", e -> {
            if (e instanceof LivingEntity le) {
                AttributeInstance a = le.getAttribute(Attributes.MOVEMENT_SPEED);
                if (a != null) {
                    a.setBaseValue(0.25 * speed);
                }
            }
        });
    }

    public void entitySit(Character<?> who, Supplier<Location> at) {
        this.state(who, "sit", e -> Seats.sit(e, at != null ? at.get() : null));
    }

    public void entityStopSit(Character<?> who) {
        this.thenRun(() -> {
            Characters.forget(this.script, who, "sit");
            Characters.act(this.script, who, Seats::stand);
        });
    }

    public void entityNameOverride(Character<?> who, Supplier<String> name) {
        this.state(who, "name", e -> e.setCustomName(Component.literal(name.get())));
    }

    public void thenSetHeldItem(Character<?> who, ItemStack stack) {
        this.state(who, "hand", e -> {
            if (e instanceof LivingEntity le) {
                le.setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
            }
        });
    }

    public void keepHeldItem(Character<?> who, ItemStack stack) {
        ItemStack held = stack.copy();
        this.keep(who, "hand", e -> {
            if (e instanceof LivingEntity le) {
                le.setItemSlot(EquipmentSlot.MAINHAND, held.copy());
            }
        });
    }

    public void setMaxHealth(Character<?> who, float hp) {
        this.state(who, "health", e -> {
            if (e instanceof AnimatedEntity ae) {
                ae.setMaxHealth(hp);
            } else if (e instanceof LivingEntity le) {
                AttributeInstance a = le.getAttribute(Attributes.MAX_HEALTH);
                if (a != null) {
                    a.setBaseValue(hp);
                }

                le.setHealth(hp);
            }
        });
    }

    public void entityTextureOverride(Character<?> who, ResourceLocation texture) {
        this.stateAnimated(who, "texture", ae -> ae.setTextureOverride(texture));
    }

    public void setAnimation(Character<?> who, String layer, ConfiguredAnimation ca) {
        boolean stays = ca.loopMode() != LoopMode.ONCE;
        this.thenRun(() -> {
            Characters.forget(this.script, who, "anim:" + layer);
            Characters.act(this.script, who, e -> {
                if (e instanceof AnimatedEntity ae) {
                    ae.startAnimation(layer, ca);
                }
            });
            if (stays && !ca.animation().isNull()) {
                this.keep(who, "anim:" + layer, e -> {
                    if (e instanceof AnimatedEntity ae && !ae.getAnimationSystem().getLayer(layer).activeAnimation().id().equals(ca.animation().id())) {
                        ae.startAnimation(layer, ca);
                    }
                });
            }
        });
    }

    public void clearAnimations(Character<?> who, float fade) {
        this.thenRun(() -> {
            Characters.forget(this.script, who, "anim:");
            Characters.act(this.script, who, e -> {
                if (e instanceof AnimatedEntity ae) {
                    for (String l : ae.getAnimationSystem().getLayerNames()) {
                        if (!l.equals("movement")) {
                            ae.getAnimationSystem().stopAnimation(l, fade);
                        }
                    }
                }
            });
        });
    }

    public void overridePredefinedAnimation(Character<?> who, MovementActionType type, ConfiguredAnimation ca) {
        this.stateAnimated(who, "gait:" + type, ae -> ae.setAnimationOverride(type, ca));
    }

    public void removeOverridePredefined(Character<?> who) {
        this.thenRun(() -> {
            Characters.forget(this.script, who, "gait:");
            Characters.act(this.script, who, e -> {
                if (e instanceof AnimatedEntity ae) {
                    ae.clearAnimationOverrides();
                }
            });
        });
    }

    private void gesture(Character<?> who, String name, int times, float speed) {
        this.actAnimated(
            who,
            ae -> {
                Animation a = Anims.gesture(ae, name);
                if (a != null) {
                    ConfiguredAnimation ca = a.configure().transitionTime(0.1F).speed(speed);
                    if (times > 1) {
                        ca.loopMode(LoopMode.LOOP);
                    }

                    ae.startAnimation("action", ca);
                    if (times > 1) {
                        Behaviours.afterTicks(
                            (int)Math.ceil(a.lengthSeconds() * 20.0 * times / speed), () -> ae.getAnimationSystem().stopAnimation("action", 0.2F)
                        );
                    }
                }
            }
        );
    }

    public void nod(Character<?> who, int times) {
        this.gesture(who, "nod", times, 1.0F);
    }

    public void nope(Character<?> who, int times) {
        this.gesture(who, "nope", times, 1.0F);
    }

    public void refuse(Character<?> who, int times) {
        this.gesture(who, "refuse", times, 1.0F);
    }

    public void laugh(Character<?> who) {
        this.gesture(who, "laugh", 1, 1.0F);
    }

    public void laugh(Character<?> who, int times, float speed) {
        this.gesture(who, "laugh", times, speed);
    }

    public void sigh(Character<?> who) {
        this.gesture(who, "sigh", 1, 1.0F);
    }

    public void shake(Character<?> who, int ticks) {
        this.actAnimated(who, ae -> {
            Animation a = Anims.gesture(ae, "shake");
            if (a != null) {
                ae.startAnimation("action", a.configure().transitionTime(0.1F).loopMode(LoopMode.LOOP));
                Behaviours.afterTicks(ticks, () -> ae.getAnimationSystem().stopAnimation("action", 0.2F));
            }
        });
    }

    public void teleportPlayer(Supplier<Location> at) {
        this.thenRun(() -> {
            ServerPlayer p = SprauteRuntime.player();
            Location l = at.get();
            if (p != null && l != null) {
                ServerLevel level = p.server.getLevel(l.dimension);
                p.teleportTo(level != null ? level : Mc.serverLevel(p), l.x, l.y, l.z, l.yaw, l.pitch);
            }
        });
    }

    public void thenAddEffect(MobEffectInstance effect) {
        this.thenRun(() -> {
            ServerPlayer p = SprauteRuntime.player();
            if (p != null) {
                p.addEffect(new MobEffectInstance(effect));
            }
        });
    }

    public void playSound(Supplier<BlockPos> at, SoundEvent sound) {
        this.thenRun(() -> {
            ServerPlayer p = SprauteRuntime.player();
            if (p != null) {
                Mc.level(p).playSound(null, at.get(), sound, SoundSource.MASTER, 1.0F, 1.0F);
            }
        });
    }

    public void playerTextureOverride(ResourceLocation texture) {
        this.thenRun(() -> PlayerTexture.set(SprauteRuntime.player(), texture));
    }

    public void playerTextureReset() {
        this.thenRun(() -> PlayerTexture.set(SprauteRuntime.player(), null));
    }

    private void screen(String kind, int a, int b, int c, int argb) {
        this.thenRun(() -> SprauteNetwork.sendToAll(new Packets.ScreenFx(kind, a, b, c, argb)));
    }

    public void blackout(int ticks) {
        this.screen("pulse", 0, ticks, 20, -16777216);
    }

    public void flash(int ticks) {
        this.screen("pulse", ticks / 2, 0, ticks - ticks / 2, -1);
    }

    public void screenFill(int argb, int in, int hold, int out, boolean underChat) {
        this.screen(underChat ? "pulse_under" : "pulse", in, hold, out, argb);
    }

    public void screenColor(int r, int g, int b, int a) {
        this.screen("color", 0, 0, 0, (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | b & 0xFF);
    }

    public void close_eyes(int close, int hold, int open) {
        this.screen("eyes", close, hold, open, -16777216);
    }

    private static final class Run extends ScriptPerformerAction {
        private final Runnable r;

        Run(ScriptPerformer p, Runnable r) {
            super(p);
            this.r = r;
        }

        @Override
        public boolean update() {
            this.r.run();
            return true;
        }
    }

    private static class Wait extends ScriptPerformerAction {
        private final Runnable begin;
        private final BooleanSupplier done;
        private final String words;

        Wait(ScriptPerformer p, Runnable begin, BooleanSupplier done, String words) {
            super(p);
            this.begin = begin;
            this.done = done;
            this.words = words;
        }

        @Override
        public String describe() {
            return this.words;
        }

        @Override
        public void start() {
            if (this.begin != null) {
                this.begin.run();
            }
        }

        @Override
        public boolean update() {
            return this.done.getAsBoolean();
        }
    }
}
