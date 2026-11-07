package org.sprautestudio.spraute_engine.platform.forge;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent.Finish;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent.Added;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent.AdvancementEarnEvent;
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Close;
import net.minecraftforge.event.entity.player.PlayerContainerEvent.Open;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemCraftedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.ItemSmeltedEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem;
import net.minecraftforge.event.entity.player.PlayerXpEvent.LevelChange;
import net.minecraftforge.event.entity.player.PlayerXpEvent.PickupXp;
import net.minecraftforge.event.level.BlockEvent.BreakEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.level.ExplosionEvent.Detonate;
import net.minecraftforge.event.level.LevelEvent.Save;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import org.sprautestudio.spraute_engine.platform.Cancellable;
import org.sprautestudio.spraute_engine.platform.Hook;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Priority;
import org.sprautestudio.spraute_engine.platform.TickPhase;

final class ForgeEvents {
    private ForgeEvents() {
    }

    static TickPhase phase(Phase phase) {
        return phase == Phase.START ? TickPhase.START : TickPhase.END;
    }

    static EventPriority priority(Priority p) {
        return EventPriority.valueOf(p.name());
    }

    static <F extends Event, E> void bridge(Class<F> type, Hook<E> hook, Function<F, E> wrap) {
        bridge(type, hook, wrap, null);
    }

    static <F extends Event, E> void bridge(Class<F> type, Hook<E> hook, Function<F, E> wrap, BiConsumer<E, F> back) {
        for (Priority p : hook.priorities()) {
            MinecraftForge.EVENT_BUS.addListener(priority(p), hook.receivesCanceled(), type, f -> fire(p, hook, wrap, back, f));
        }
    }

    static <F extends Event, E> void fire(Priority p, Hook<E> hook, Function<F, E> wrap, BiConsumer<E, F> back, F f) {
        E e = wrap.apply(f);
        hook.fire(p, e);
        if (e instanceof Cancellable c && c.isCanceled()) {
            f.setCanceled(true);
        }

        if (back != null) {
            back.accept(e, f);
        }
    }

    static void install() {
        bridge(ServerAboutToStartEvent.class, Hooks.SERVER_ABOUT_TO_START, f -> new Hooks.ServerAboutToStart(f.getServer()));
        bridge(ServerStoppingEvent.class, Hooks.SERVER_STOPPING, f -> new Hooks.ServerStopping(f.getServer()));
        bridge(ServerStoppedEvent.class, Hooks.SERVER_STOPPED, f -> new Hooks.ServerStopped(f.getServer()));
        bridge(Save.class, Hooks.LEVEL_SAVE, f -> new Hooks.LevelSave(f.getLevel()));
        bridge(RegisterCommandsEvent.class, Hooks.REGISTER_COMMANDS, f -> new Hooks.RegisterCommands(f.getDispatcher()));
        bridge(ServerTickEvent.class, Hooks.SERVER_TICK, f -> new Hooks.ServerTick(phase(f.phase), f.getServer()));
        bridge(PlayerTickEvent.class, Hooks.PLAYER_TICK, f -> new Hooks.PlayerTick(phase(f.phase), f.player));
        bridge(AdvancementEarnEvent.class, Hooks.ADVANCEMENT_EARN, f -> new Hooks.AdvancementEarn(f.getEntity(), f.getAdvancement()));
        bridge(AnimalTameEvent.class, Hooks.ANIMAL_TAME, f -> new Hooks.AnimalTame(f.getAnimal(), f.getTamer()));
        bridge(ArrowLooseEvent.class, Hooks.ARROW_LOOSE, f -> new Hooks.ArrowLoose(f.getEntity(), f.getBow(), f.getCharge()));
        bridge(AttackEntityEvent.class, Hooks.ATTACK_ENTITY, f -> new Hooks.AttackEntity(f.getEntity(), f.getTarget()));
        bridge(BabyEntitySpawnEvent.class, Hooks.BABY_SPAWN, f -> new Hooks.BabySpawn(f.getChild(), f.getCausedByPlayer()));
        bridge(BreakEvent.class, Hooks.BLOCK_BREAK, f -> new Hooks.BlockBreak(f.getPos(), f.getState(), f.getPlayer()));
        bridge(EntityPlaceEvent.class, Hooks.BLOCK_PLACE, f -> new Hooks.BlockPlace(f.getEntity(), f.getPos(), f.getPlacedBlock()));
        bridge(BonemealEvent.class, Hooks.BONEMEAL, f -> new Hooks.Bonemeal(f.getEntity(), f.getPos(), f.getBlock()));
        bridge(EntityJoinLevelEvent.class, Hooks.ENTITY_JOIN, f -> new Hooks.EntityJoin(f.getEntity(), f.getLevel()));
        bridge(EntityLeaveLevelEvent.class, Hooks.ENTITY_LEAVE, f -> new Hooks.EntityLeave(f.getEntity(), f.getLevel()));
        bridge(EntityMountEvent.class, Hooks.ENTITY_MOUNT, f -> new Hooks.EntityMount(f.getEntityMounting(), f.getEntityBeingMounted(), f.isMounting()));
        bridge(Detonate.class, Hooks.EXPLOSION_DETONATE, f -> new Hooks.ExplosionDetonate(f.getExplosion(), f.getLevel(), f.getExplosion().getPosition()));
        bridge(FillBucketEvent.class, Hooks.FILL_BUCKET, f -> new Hooks.FillBucket(f.getEntity(), f.getEmptyBucket()));
        bridge(ItemFishedEvent.class, Hooks.ITEM_FISHED, f -> new Hooks.ItemFished(f.getEntity(), f.getDrops()));
        bridge(ItemTossEvent.class, Hooks.ITEM_TOSS, f -> new Hooks.ItemToss(f.getEntity(), f.getPlayer()));
        bridge(LivingAttackEvent.class, Hooks.LIVING_ATTACK, f -> new Hooks.LivingAttack(f.getEntity(), f.getSource(), f.getAmount()));
        bridge(LivingDeathEvent.class, Hooks.LIVING_DEATH, f -> new Hooks.LivingDeath(f.getEntity(), f.getSource()));
        bridge(Finish.class, Hooks.USE_ITEM_FINISH, f -> new Hooks.UseItemFinish(f.getEntity(), f.getItem()));
        bridge(LivingEquipmentChangeEvent.class, Hooks.EQUIPMENT_CHANGE, f -> new Hooks.EquipmentChange(f.getEntity(), f.getSlot(), f.getTo()));
        bridge(LivingJumpEvent.class, Hooks.LIVING_JUMP, f -> new Hooks.LivingJump(f.getEntity()));
        bridge(LivingFallEvent.class, Hooks.LIVING_FALL, f -> new Hooks.LivingFall(f.getEntity(), f.getDistance(), f.getDamageMultiplier()), (e, f) -> {
            f.setDistance(e.getDistance());
            f.setDamageMultiplier(e.getDamageMultiplier());
        });
        bridge(LivingHurtEvent.class, Hooks.LIVING_HURT, f -> new Hooks.LivingHurt(f.getEntity(), f.getSource(), f.getAmount()));
        bridge(Added.class, Hooks.EFFECT_ADDED, f -> new Hooks.EffectAdded(f.getEntity(), f.getEffectInstance()));
        bridge(Close.class, Hooks.CONTAINER_CLOSE, f -> new Hooks.ContainerClose(f.getEntity(), f.getContainer()));
        bridge(Open.class, Hooks.CONTAINER_OPEN, f -> new Hooks.ContainerOpen(f.getEntity(), f.getContainer()));
        bridge(ItemCraftedEvent.class, Hooks.ITEM_CRAFTED, f -> new Hooks.ItemCrafted(f.getEntity(), f.getCrafting()));
        bridge(ItemPickupEvent.class, Hooks.ITEM_PICKUP, f -> new Hooks.ItemPickup(f.getEntity(), f.getStack()));
        bridge(ItemSmeltedEvent.class, Hooks.ITEM_SMELTED, f -> new Hooks.ItemSmelted(f.getEntity(), f.getSmelting()));
        bridge(PlayerChangedDimensionEvent.class, Hooks.CHANGED_DIMENSION, f -> new Hooks.ChangedDimension(f.getEntity(), f.getFrom(), f.getTo()));
        bridge(PlayerLoggedInEvent.class, Hooks.PLAYER_LOGGED_IN, f -> new Hooks.PlayerLoggedIn(f.getEntity()));
        bridge(PlayerLoggedOutEvent.class, Hooks.PLAYER_LOGGED_OUT, f -> new Hooks.PlayerLoggedOut(f.getEntity()));
        bridge(PlayerRespawnEvent.class, Hooks.PLAYER_RESPAWN, f -> new Hooks.PlayerRespawn(f.getEntity()));
        bridge(StartTracking.class, Hooks.START_TRACKING, f -> new Hooks.StartTracking(f.getEntity(), f.getTarget()));
        bridge(
            EntityInteract.class,
            Hooks.ENTITY_INTERACT,
            f -> new Hooks.EntityInteract(f.getEntity(), f.getLevel(), f.getHand(), f.getTarget(), f.getItemStack())
        );
        bridge(
            LeftClickBlock.class,
            Hooks.LEFT_CLICK_BLOCK,
            f -> new Hooks.LeftClickBlock(f.getEntity(), f.getLevel(), f.getPos(), f.getItemStack(), ForgeVersion.startsBreaking(f))
        );
        bridge(
            RightClickBlock.class,
            Hooks.RIGHT_CLICK_BLOCK,
            f -> new Hooks.RightClickBlock(f.getEntity(), f.getLevel(), f.getHand(), f.getPos(), f.getItemStack())
        );
        bridge(RightClickItem.class, Hooks.RIGHT_CLICK_ITEM, f -> new Hooks.RightClickItem(f.getEntity(), f.getItemStack()));
        bridge(PlayerSleepInBedEvent.class, Hooks.SLEEP_IN_BED, f -> new Hooks.SleepInBed(f.getEntity(), f.getPos()));
        bridge(PlayerWakeUpEvent.class, Hooks.WAKE_UP, f -> new Hooks.WakeUp(f.getEntity()));
        bridge(LevelChange.class, Hooks.XP_LEVEL_CHANGE, f -> new Hooks.XpLevelChange(f.getEntity(), f.getLevels()));
        bridge(PickupXp.class, Hooks.XP_PICKUP, f -> new Hooks.XpPickup(f.getEntity(), f.getOrb()));
        bridge(ServerChatEvent.class, Hooks.SERVER_CHAT, f -> new Hooks.ServerChat(f.getPlayer(), f.getRawText()));
        ForgeVersion.registerTrade();
    }
}
