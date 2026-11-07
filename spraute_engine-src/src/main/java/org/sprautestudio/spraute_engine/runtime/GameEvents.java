package org.sprautestudio.spraute_engine.runtime;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.build.Builds;
import org.sprautestudio.spraute_engine.camera.Cams;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.core.script.Character;
import org.sprautestudio.spraute_engine.core.script.ScriptInstance;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.engine.EngineServer;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.post.Posts;
import org.sprautestudio.spraute_engine.quest.QuestGoals;
import org.sprautestudio.spraute_engine.quest.Quests;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;
import org.sprautestudio.spraute_engine.runtime.orb.SprauteOrb;
import org.sprautestudio.spraute_engine.skills.Skills;
import org.sprautestudio.spraute_engine.zone.Zones;

public final class GameEvents {
    private static final Map<UUID, Integer> DEATHS = new HashMap<>();
    private static final int AFK_TICKS = 3600;
    private static final Map<UUID, double[]> IDLE = new HashMap<>();
    private static final Map<UUID, Integer> LAST_COINS = new HashMap<>();
    private static final Map<UUID, Object[]> STATE = new HashMap<>();
    private static long lastDayTime = -1L;
    private static Boolean lastRain;

    public static void registerHooks() {
        Hooks.SERVER_CHAT.add(Priority.HIGHEST, GameEvents::onChat);
        Hooks.BLOCK_BREAK.add(Priority.LOWEST, GameEvents::onBreak);
        Hooks.BLOCK_PLACE.add(Priority.LOWEST, GameEvents::onPlace);
        Hooks.LEFT_CLICK_BLOCK.add(Priority.LOWEST, GameEvents::onPunch);
        Hooks.RIGHT_CLICK_BLOCK.add(Priority.LOWEST, GameEvents::onUseBlock);
        Hooks.BONEMEAL.add(Priority.LOWEST, GameEvents::onBonemeal);
        Hooks.FILL_BUCKET.add(Priority.LOWEST, GameEvents::onBucket);
        Hooks.ENTITY_INTERACT.add(Priority.LOWEST, GameEvents::onInteract);
        Hooks.ATTACK_ENTITY.add(Priority.LOWEST, GameEvents::onAttack);
        Hooks.LIVING_DEATH.add(Priority.LOWEST, GameEvents::onDeath);
        Hooks.LIVING_HURT.add(Priority.LOWEST, GameEvents::onHurt);
        Hooks.PLAYER_RESPAWN.add(GameEvents::onRespawn);
        Hooks.ANIMAL_TAME.add(Priority.LOWEST, GameEvents::onTame);
        Hooks.BABY_SPAWN.add(Priority.LOWEST, GameEvents::onBreed);
        Hooks.ENTITY_MOUNT.add(Priority.LOWEST, GameEvents::onMount);
        Hooks.TRADE.add(GameEvents::onTrade);
        Hooks.ITEM_PICKUP.add(GameEvents::onPickup);
        Hooks.ITEM_TOSS.add(Priority.LOWEST, GameEvents::onToss);
        Hooks.RIGHT_CLICK_ITEM.add(Priority.LOWEST, GameEvents::onUse);
        Hooks.USE_ITEM_FINISH.add(GameEvents::onEat);
        Hooks.ITEM_CRAFTED.add(GameEvents::onCraft);
        Hooks.ITEM_SMELTED.add(GameEvents::onSmelt);
        Hooks.EQUIPMENT_CHANGE.add(GameEvents::onEquip);
        Hooks.ARROW_LOOSE.add(GameEvents::onShoot);
        Hooks.ITEM_FISHED.add(GameEvents::onFish);
        Hooks.CONTAINER_OPEN.add(GameEvents::onOpen);
        Hooks.CONTAINER_CLOSE.add(GameEvents::onClose);
        Hooks.LIVING_JUMP.add(GameEvents::onJump);
        Hooks.LIVING_FALL.add(GameEvents::onFall);
        Hooks.SLEEP_IN_BED.add(GameEvents::onSleep);
        Hooks.WAKE_UP.add(GameEvents::onWake);
        Hooks.XP_LEVEL_CHANGE.add(GameEvents::onLevel);
        Hooks.XP_PICKUP.add(GameEvents::onXp);
        Hooks.EFFECT_ADDED.add(GameEvents::onEffect);
        Hooks.ADVANCEMENT_EARN.add(GameEvents::onAdvancement);
        Hooks.PLAYER_LOGGED_IN.add(GameEvents::onJoin);
        Posts.registerHooks();
        Cams.registerHooks();
        Builds.registerHooks();
        Hooks.PLAYER_LOGGED_OUT.add(GameEvents::onLeave);
        Hooks.CHANGED_DIMENSION.add(GameEvents::onDimension);
        Hooks.EXPLOSION_DETONATE.add(GameEvents::onExplosion);
        Hooks.PLAYER_TICK.add(GameEvents::onPlayerTick);
        Hooks.SERVER_TICK.add(GameEvents::onServerTick);
    }

    private GameEvents() {
    }

    public static void post(String type, Player player, Map<String, Object> data) {
        fire(type, player, data);
    }

    public static void fire(String type, Player player, Map<String, Object> data) {
        if (player == null || player instanceof ServerPlayer) {
            if (player != null) {
                data.putIfAbsent("player", player.getGameProfile().getName());
                data.putIfAbsent("__player", player);
                Skills.onEvent(type, player, data);
            }

            String ownerOfEntity = data.get("__entity") instanceof Entity e ? SprauteRuntime.ownerOf(e) : null;
            if (QuestGoals.listens(type)) {
                questGoals(type, player, ownerOfEntity, data);
            }

            SprauteRuntime.env()
                .ifPresent(
                    env -> {
                        for (ScriptInstance si : env.scriptInstances.values()) {
                            if (si instanceof SprauteScriptInstance s
                                && (!s.solo() || player == null || player.getUUID().equals(s.owner))
                                && (ownerOfEntity == null || s.solo() && ownerOfEntity.equals(String.valueOf(s.owner)))) {
                                s.events.add(type, data, s);
                            }
                        }
                    }
                );
        }
    }

    private static void questGoals(String type, Player player, String ownerOfEntity, Map<String, Object> data) {
        Map<String, Object> d = new LinkedHashMap<>(data);
        Set<String> names = new HashSet<>();
        if (data.get("__entity") instanceof Entity e) {
            SprauteRuntime.env().ifPresent(env -> {
                for (ScriptInstance si : env.scriptInstances.values()) {
                    if (si instanceof SprauteScriptInstance s) {
                        s.characters.forEach((name, ch) -> {
                            if (s.optional((Character<?>)ch).orElse(null) == e) {
                                names.add(name);
                            }
                        });
                    }
                }
            });
        }

        d.put("characters", names);
        Quests.onEvent(type, (ServerPlayer)player, ownerOfEntity, d);
    }

    private static Map<String, Object> data() {
        return new LinkedHashMap<>();
    }

    private static String id(BlockState st) {
        return Regs.BLOCK.getKey(st.getBlock()).toString();
    }

    private static String id(ItemStack st) {
        return st != null && !st.isEmpty() ? Regs.ITEM.getKey(st.getItem()).toString() : "";
    }

    private static Map<String, Object> at(Map<String, Object> d, BlockPos p) {
        d.put("x", (double)p.getX());
        d.put("y", (double)p.getY());
        d.put("z", (double)p.getZ());
        return d;
    }

    private static Map<String, Object> at(Map<String, Object> d, Entity e) {
        d.put("x", Math.floor(e.getX()));
        d.put("y", Math.floor(e.getY()));
        d.put("z", Math.floor(e.getZ()));
        return d;
    }

    private static Map<String, Object> entity(Map<String, Object> d, Entity e) {
        if (e == null) {
            return d;
        } else {
            d.put("entity", e instanceof Player ? "player" : Regs.ENTITY_TYPE.getKey(e.getType()).toString());
            d.put("name", e.getName().getString());
            d.put("__entity", e);
            return d;
        }
    }

    private static Player player(Entity e) {
        return e instanceof Player p ? p : null;
    }

    public static void onChat(Hooks.ServerChat e) {
        Map<String, Object> d = data();
        d.put("text", e.getRawText());
        fire("chat", e.getPlayer(), d);
    }

    public static void onBreak(Hooks.BlockBreak e) {
        if (!e.isCanceled()) {
            Map<String, Object> d = at(data(), e.getPos());
            d.put("block", id(e.getState()));
            d.put("item", id(e.getPlayer().getMainHandItem()));
            fire("break", e.getPlayer(), d);
        }
    }

    public static void onPlace(Hooks.BlockPlace e) {
        if (!e.isCanceled() && e.getEntity() instanceof Player p) {
            Map<String, Object> d = at(data(), e.getPos());
            d.put("block", id(e.getPlacedBlock()));
            fire("place", p, d);
        }
    }

    public static void onPunch(Hooks.LeftClickBlock e) {
        if (e.isStart()) {
            Map<String, Object> d = at(data(), e.getPos());
            d.put("block", id(e.getLevel().getBlockState(e.getPos())));
            d.put("item", id(e.getItemStack()));
            fire("punch", e.getEntity(), d);
        }
    }

    public static void onUseBlock(Hooks.RightClickBlock e) {
        if (e.getHand() == InteractionHand.MAIN_HAND) {
            Map<String, Object> d = at(data(), e.getPos());
            d.put("block", id(e.getLevel().getBlockState(e.getPos())));
            d.put("item", id(e.getItemStack()));
            fire("useblock", e.getEntity(), d);
        }
    }

    public static void onBonemeal(Hooks.Bonemeal e) {
        Map<String, Object> d = at(data(), e.getPos());
        d.put("block", id(e.getBlock()));
        fire("bonemeal", e.getEntity(), d);
    }

    public static void onBucket(Hooks.FillBucket e) {
        Map<String, Object> d = at(data(), e.getEntity().blockPosition());
        d.put("item", id(e.getEmptyBucket()));
        fire("bucket", e.getEntity(), d);
    }

    public static void onInteract(Hooks.EntityInteract e) {
        if (e.getHand() == InteractionHand.MAIN_HAND) {
            Map<String, Object> d = at(entity(data(), e.getTarget()), e.getTarget());
            d.put("item", id(e.getItemStack()));
            fire("interact", e.getEntity(), d);
        }
    }

    public static void onAttack(Hooks.AttackEntity e) {
        if (!e.isCanceled()) {
            Map<String, Object> d = at(entity(data(), e.getTarget()), e.getTarget());
            d.put("item", id(e.getEntity().getMainHandItem()));
            fire("attack", e.getEntity(), d);
        }
    }

    public static void onDeath(Hooks.LivingDeath e) {
        if (!e.isCanceled() && !Mc.level(e.getEntity()).isClientSide) {
            int now = Mechanics.now();
            Integer told = DEATHS.get(e.getEntity().getUUID());
            if (told == null || now - told >= 100) {
                DEATHS.put(e.getEntity().getUUID(), now);
                if (DEATHS.size() > 512) {
                    DEATHS.values().removeIf(t -> now - t > 100);
                }

                Entity killer = e.getSource().getEntity();
                if (e.getEntity() instanceof Player victim) {
                    Map<String, Object> d = at(entity(data(), killer), victim);
                    d.put("source", e.getSource().getMsgId());
                    fire("death", victim, d);
                    if (killer instanceof Player by && by != victim) {
                        Map<String, Object> k = at(data(), victim);
                        k.put("target", victim.getGameProfile().getName());
                        k.put("item", id(by.getMainHandItem()));
                        fire("pvpkill", by, k);
                    }
                } else {
                    if (killer instanceof Player p) {
                        Map<String, Object> d = at(entity(data(), e.getEntity()), e.getEntity());
                        d.put("item", id(p.getMainHandItem()));
                        fire("kill", p, d);
                    }

                    Map<String, Object> d = at(entity(data(), e.getEntity()), e.getEntity());
                    d.put("source", e.getSource().getMsgId());
                    putKiller(d, killer);
                    fire("npcdeath", player(killer), d);
                }
            }
        }
    }

    static void putKiller(Map<String, Object> d, Entity killer) {
        if (killer == null) {
            d.put("by", "");
            d.put("killer", "");
        } else {
            String type = killer instanceof Player ? "player" : Regs.ENTITY_TYPE.getKey(killer.getType()).toString();
            d.put("by", type);
            d.put("killer", type);
            d.put("__killer", killer);
        }
    }

    public static void onHurt(Hooks.LivingHurt e) {
        if (!e.isCanceled() && e.getEntity() instanceof Player p) {
            Map<String, Object> d = at(entity(data(), e.getSource().getEntity()), p);
            d.put("amount", (double)e.getAmount());
            d.put("source", e.getSource().getMsgId());
            fire("hurt", p, d);
            if (e.getSource().getEntity() instanceof Player by && by != p) {
                Map<String, Object> hit = at(data(), p);
                hit.put("target", p.getGameProfile().getName());
                hit.put("amount", (double)e.getAmount());
                hit.put("item", id(by.getMainHandItem()));
                fire("pvp", by, hit);
            }
        }
    }

    public static void onRespawn(Hooks.PlayerRespawn e) {
        fire("respawn", e.getEntity(), at(data(), e.getEntity()));
    }

    public static void onTame(Hooks.AnimalTame e) {
        if (!e.isCanceled()) {
            fire("tame", e.getTamer(), at(entity(data(), e.getAnimal()), e.getAnimal()));
        }
    }

    public static void onBreed(Hooks.BabySpawn e) {
        if (!e.isCanceled() && e.getChild() != null) {
            fire("breed", e.getCausedByPlayer(), at(entity(data(), e.getChild()), e.getChild()));
        }
    }

    public static void onMount(Hooks.EntityMount e) {
        if (!e.isCanceled() && e.getEntityMounting() instanceof Player p) {
            fire(e.isMounting() ? "mount" : "dismount", p, entity(data(), e.getEntityBeingMounted()));
        }
    }

    public static void onTrade(Hooks.Trade e) {
        Map<String, Object> d = entity(data(), e.getAbstractVillager());
        d.put("item", id(e.getMerchantOffer().getResult()));
        d.put("count", (double)e.getMerchantOffer().getResult().getCount());
        fire("trade", e.getEntity(), d);
    }

    public static void onPickup(Hooks.ItemPickup e) {
        Map<String, Object> d = data();
        d.put("item", id(e.getStack()));
        d.put("count", (double)e.getStack().getCount());
        fire("pickup", e.getEntity(), d);
    }

    public static void npcPickup(Entity npc, ItemStack taken) {
        Map<String, Object> d = at(entity(data(), npc), npc);
        d.put("item", id(taken));
        d.put("count", (double)taken.getCount());
        fire("npcpickup", null, d);
    }

    public static void onToss(Hooks.ItemToss e) {
        if (!e.isCanceled()) {
            ItemStack st = e.getEntity().getItem();
            Map<String, Object> d = data();
            d.put("item", id(st));
            d.put("count", (double)st.getCount());
            fire("drop", e.getPlayer(), d);
        }
    }

    public static void onUse(Hooks.RightClickItem e) {
        Map<String, Object> d = data();
        d.put("item", id(e.getItemStack()));
        fire("use", e.getEntity(), d);
    }

    public static void onEat(Hooks.UseItemFinish e) {
        if (e.getEntity() instanceof Player p) {
            Map<String, Object> d = data();
            d.put("item", id(e.getItem()));
            fire("eat", p, d);
        }
    }

    public static void onCraft(Hooks.ItemCrafted e) {
        Map<String, Object> d = data();
        d.put("item", id(e.getCrafting()));
        d.put("count", (double)e.getCrafting().getCount());
        fire("craft", e.getEntity(), d);
    }

    public static void onSmelt(Hooks.ItemSmelted e) {
        Map<String, Object> d = data();
        d.put("item", id(e.getSmelting()));
        d.put("count", (double)e.getSmelting().getCount());
        fire("smelt", e.getEntity(), d);
    }

    public static void onEquip(Hooks.EquipmentChange e) {
        if (e.getEntity() instanceof Player p && !e.getTo().isEmpty()) {
            if (e.getSlot() != EquipmentSlot.MAINHAND) {
                Map<String, Object> d = data();
                d.put("item", id(e.getTo()));
                d.put("slot", e.getSlot().getName());
                fire("equip", p, d);
            }
        }
    }

    public static void onShoot(Hooks.ArrowLoose e) {
        Map<String, Object> d = data();
        d.put("item", id(e.getBow()));
        d.put("amount", (double)e.getCharge());
        fire("shoot", e.getEntity(), d);
    }

    public static void onFish(Hooks.ItemFished e) {
        for (ItemStack st : e.getDrops()) {
            Map<String, Object> d = data();
            d.put("item", id(st));
            d.put("count", (double)st.getCount());
            fire("fish", e.getEntity(), d);
        }
    }

    public static void onOpen(Hooks.ContainerOpen e) {
        Map<String, Object> d = data();
        d.put("id", menuId(e.getContainer()));
        fire("opencontainer", e.getEntity(), d);
    }

    public static void onClose(Hooks.ContainerClose e) {
        Map<String, Object> d = data();
        d.put("id", menuId(e.getContainer()));
        fire("closecontainer", e.getEntity(), d);
    }

    private static String menuId(AbstractContainerMenu m) {
        try {
            ResourceLocation key = Regs.MENU.getKey(m.getType());
            return key != null ? key.toString() : "inventory";
        } catch (RuntimeException var2) {
            return "inventory";
        }
    }

    public static void onJump(Hooks.LivingJump e) {
        if (e.getEntity() instanceof Player p) {
            fire("jump", p, at(data(), p));
        }
    }

    public static void onFall(Hooks.LivingFall e) {
        if (e.getEntity() instanceof Player p && !(e.getDistance() <= 3.0F)) {
            Map<String, Object> d = at(data(), p);
            d.put("amount", Math.round(e.getDistance() * 10.0F) / 10.0);
            fire("fall", p, d);
        }
    }

    public static void onSleep(Hooks.SleepInBed e) {
        fire("sleep", e.getEntity(), at(data(), e.getPos()));
    }

    public static void onWake(Hooks.WakeUp e) {
        fire("wake", e.getEntity(), data());
    }

    public static void onLevel(Hooks.XpLevelChange e) {
        Map<String, Object> d = data();
        d.put("level", (double)(e.getEntity().experienceLevel + e.getLevels()));
        fire("levelup", e.getEntity(), d);
    }

    public static void onXp(Hooks.XpPickup e) {
        Map<String, Object> d = data();
        d.put("amount", (double)e.getOrb().getValue());
        fire("xp", e.getEntity(), d);
    }

    public static void onEffect(Hooks.EffectAdded e) {
        if (e.getEntity() instanceof Player p) {
            Map var4 = data();
            ResourceLocation key = Regs.MOB_EFFECT.getKey(e.getEffectInstance().getEffect());
            var4.put("id", key != null ? key.toString() : "");
            var4.put("level", (double)(e.getEffectInstance().getAmplifier() + 1));
            fire("effect", p, var4);
        }
    }

    public static void onAdvancement(Hooks.AdvancementEarn e) {
        Map<String, Object> d = data();
        d.put("id", e.getAdvancement().getId().toString());
        fire("advancement", e.getEntity(), d);
    }

    public static void onJoin(Hooks.PlayerLoggedIn e) {
        Map<String, Object> d = data();
        d.put("count", (double)SprauteRuntime.players().size());
        fire("join", e.getEntity(), d);
        if (e.getEntity() instanceof ServerPlayer sp) {
            if (sp.getStats().getValue(Stats.CUSTOM.get(Stats.LEAVE_GAME)) == 0) {
                Map<String, Object> first = data();
                first.put("count", (double)SprauteRuntime.players().size());
                fire("firstjoin", sp, first);
            }

            SprauteRuntime.get().playerJoined(sp);
        }
    }

    public static void onLeave(Hooks.PlayerLoggedOut e) {
        Player left = e.getEntity();
        int stay = 0;

        for (ServerPlayer p : SprauteRuntime.players()) {
            if (p != left) {
                stay++;
            }
        }

        Map<String, Object> d = data();
        d.put("count", (double)stay);
        fire("leave", left, d);
        IDLE.remove(left.getUUID());
    }

    public static boolean isAfk(ServerPlayer p) {
        double[] s = IDLE.get(p.getUUID());
        return s != null && s[6] == 1.0;
    }

    private static void watchIdle(ServerPlayer p) {
        double[] now = new double[]{p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()};
        double[] s = IDLE.get(p.getUUID());
        if (s == null) {
            IDLE.put(p.getUUID(), new double[]{now[0], now[1], now[2], now[3], now[4], 0.0, 0.0});
        } else {
            boolean moved = false;

            for (int i = 0; i < 5; i++) {
                if (Math.abs(now[i] - s[i]) > 0.01) {
                    moved = true;
                }
            }

            if (moved) {
                System.arraycopy(now, 0, s, 0, 5);
                s[5] = 0.0;
                if (s[6] == 1.0) {
                    s[6] = 0.0;
                    fire("afkback", p, data());
                }
            } else if (s[6] == 0.0 && (s[5] += 20.0) >= 3600.0) {
                s[6] = 1.0;
                fire("afk", p, data());
            }
        }
    }

    public static void onDimension(Hooks.ChangedDimension e) {
        Map<String, Object> d = data();
        d.put("from", e.getFrom().location().toString());
        d.put("to", e.getTo().location().toString());
        fire("dimension", e.getEntity(), d);
    }

    public static void onExplosion(Hooks.ExplosionDetonate e) {
        Vec3 pos = e.getPosition();

        for (Player p : e.getLevel().players()) {
            if (!(p.position().distanceTo(pos) > 24.0)) {
                Map<String, Object> d = data();
                d.put("x", Math.floor(pos.x));
                d.put("y", Math.floor(pos.y));
                d.put("z", Math.floor(pos.z));
                fire("explosion", p, d);
            }
        }
    }

    public static void stealthCaught(Player p, Entity guard) {
        fire("stealthcaught", p, at(entity(data(), guard), guard));
    }

    public static void mobSpawn(Entity e, String id, String reason) {
        Map<String, Object> d = at(entity(data(), e), e);
        d.put("id", id);
        d.put("reason", reason);
        fire("mobspawn", null, d);
    }

    public static void camera(ServerPlayer player, int scriptId, String kind, String name) {
        Map<String, Object> d = data();
        d.put("id", name);

        String type = switch (kind) {
            case "skip" -> "cutsceneskip";
            case "mark" -> "cutscenemark";
            case "hold" -> "cutscenehold";
            default -> "cutscene";
        };
        ScriptInstance story = SprauteRuntime.env().map(env -> env.scriptInstances.get(scriptId)).orElse(null);
        if (story == null || story.protagonist() == player) {
            if (story instanceof SprauteScriptInstance s && s.clips != null) {
                if (kind.equals("clip")) {
                    s.clips.reached(name);
                    return;
                }

                if (type.equals("cutsceneskip")) {
                    s.clips.skipped();
                } else if (type.equals("cutscene")) {
                    s.clips.ended();
                    s.clips = null;
                }
            }

            if (!kind.equals("clip")) {
                if (type.equals("cutscene") && story instanceof SprauteScriptInstance s && s.cutscene.equals(name)) {
                    s.cutscene = "";
                }

                fire(type, player, d);
            }
        }
    }

    public static void orb(String type, Player player, SprauteOrb orb) {
        fire(type, player, orb.eventData());
    }

    public static void onPlayerTick(Hooks.PlayerTick e) {
        if (e.phase == TickPhase.END && e.player instanceof ServerPlayer p) {
            if (p.tickCount % 20 == 0) {
                watchIdle(p);
            }

            String biome = Mc.level(p).getBiome(p.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse("");
            boolean low = p.getHealth() < p.getMaxHealth() / 3.0F;
            Object[] now = new Object[]{p.isShiftKeyDown(), p.isSprinting(), p.isInWater(), p.isOnFire(), low, p.getInventory().selected, biome};
            Object[] was = STATE.put(p.getUUID(), now);
            if (was != null) {
                if (!(Boolean)was[0] && (Boolean)now[0]) {
                    fire("sneak", p, at(data(), p));
                }

                if (!(Boolean)was[1] && (Boolean)now[1]) {
                    fire("sprint", p, at(data(), p));
                }

                if (!(Boolean)was[2] && (Boolean)now[2]) {
                    fire("swim", p, at(data(), p));
                }

                if (!(Boolean)was[3] && (Boolean)now[3]) {
                    fire("burn", p, at(data(), p));
                }

                if (!(Boolean)was[4] && (Boolean)now[4]) {
                    Map<String, Object> d = data();
                    d.put("amount", (double)p.getHealth());
                    fire("lowhealth", p, d);
                }

                if (!was[5].equals(now[5])) {
                    Map<String, Object> d = data();
                    d.put("slot", (double)((Integer)now[5] + 1));
                    d.put("item", id(p.getInventory().getSelected()));
                    fire("select", p, d);
                }

                if (!was[6].equals(now[6])) {
                    Map<String, Object> d = data();
                    d.put("biome", now[6]);
                    d.put("from", was[6]);
                    fire("biome", p, d);
                }
            }
        }
    }

    public static void onServerTick(Hooks.ServerTick e) {
        if (e.phase == TickPhase.END) {
            MinecraftServer server = Platform.get().server();
            if (server != null) {
                EngineServer.tick(server);
                SprauteRuntime.get().tickAutostart();
                Zones.tick(server);
                Quests.tick(server);
                ServerLevel level = server.overworld();
                long t = level.getDayTime() % 24000L;
                if (lastDayTime >= 0L) {
                    if (crossed(lastDayTime, t, 0L)) {
                        fire("day", null, data());
                    }

                    if (crossed(lastDayTime, t, 13000L)) {
                        fire("night", null, data());
                    }
                }

                lastDayTime = t;

                try {
                    for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                        int coins = Coins.get(p);
                        Integer last = LAST_COINS.put(p.getUUID(), coins);
                        if (last == null || coins != last) {
                            Coins.sync(p, -2);
                        }

                        if (last != null && coins != last) {
                            Map<String, Object> d = data();
                            d.put("amount", (double)(coins - last));
                            d.put("level", (double)coins);
                            fire("coins", p, d);
                        }
                    }
                } catch (Throwable var10) {
                }

                SprauteRuntime.env().ifPresent(env -> {
                    for (ScriptInstance si : env.scriptInstances.values()) {
                        if (si instanceof SprauteScriptInstance s && !s.characterModels.isEmpty()) {
                            s.characterModels.forEach((name, key) -> {
                                Character<?> ch = s.characters.get(name);
                                if (ch != null && s.optional(ch).orElse(null) instanceof SprauteModelEntity me && !key.equals(me.model())) {
                                    me.setModel(key);
                                }
                            });
                        }
                    }
                });
                boolean rain = level.isRaining();
                if (lastRain != null && lastRain != rain) {
                    fire(rain ? "rain" : "clear", null, data());
                }

                lastRain = rain;
            }
        }
    }

    private static boolean crossed(long from, long to, long mark) {
        if (from == to) {
            return false;
        } else {
            return from < to ? from < mark && mark <= to : mark > from || mark <= to;
        }
    }

    public static void reset() {
        STATE.clear();
        lastDayTime = -1L;
        lastRain = null;
        LAST_COINS.clear();
    }
}
