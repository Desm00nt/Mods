package org.sprautestudio.spraute_engine.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class Hooks {
    public static final Hook<Hooks.CommonSetup> COMMON_SETUP = new Hook<>();
    public static final Hook<Hooks.ServerAboutToStart> SERVER_ABOUT_TO_START = new Hook<>();
    public static final Hook<Hooks.ServerStopping> SERVER_STOPPING = new Hook<>();
    public static final Hook<Hooks.ServerStopped> SERVER_STOPPED = new Hook<>();
    public static final Hook<Hooks.LevelSave> LEVEL_SAVE = new Hook<>();
    public static final Hook<Hooks.RegisterCommands> REGISTER_COMMANDS = new Hook<>();
    public static final Hook<Hooks.ServerTick> SERVER_TICK = new Hook<>();
    public static final Hook<Hooks.PlayerTick> PLAYER_TICK = new Hook<>();
    public static final Hook<Hooks.AdvancementEarn> ADVANCEMENT_EARN = new Hook<>();
    public static final Hook<Hooks.AnimalTame> ANIMAL_TAME = new Hook<>();
    public static final Hook<Hooks.ArrowLoose> ARROW_LOOSE = new Hook<>();
    public static final Hook<Hooks.AttackEntity> ATTACK_ENTITY = new Hook<>();
    public static final Hook<Hooks.BabySpawn> BABY_SPAWN = new Hook<>();
    public static final Hook<Hooks.BlockBreak> BLOCK_BREAK = new Hook<>();
    public static final Hook<Hooks.BlockPlace> BLOCK_PLACE = new Hook<>();
    public static final Hook<Hooks.Bonemeal> BONEMEAL = new Hook<>();
    public static final Hook<Hooks.EntityJoin> ENTITY_JOIN = new Hook<>();
    public static final Hook<Hooks.EntityLeave> ENTITY_LEAVE = new Hook<>();
    public static final Hook<Hooks.EntityMount> ENTITY_MOUNT = new Hook<>();
    public static final Hook<Hooks.ExplosionDetonate> EXPLOSION_DETONATE = new Hook<>();
    public static final Hook<Hooks.FillBucket> FILL_BUCKET = new Hook<>();
    public static final Hook<Hooks.ItemFished> ITEM_FISHED = new Hook<>();
    public static final Hook<Hooks.ItemToss> ITEM_TOSS = new Hook<>();
    public static final Hook<Hooks.LivingAttack> LIVING_ATTACK = new Hook<>();
    public static final Hook<Hooks.LivingDeath> LIVING_DEATH = new Hook<>();
    public static final Hook<Hooks.UseItemFinish> USE_ITEM_FINISH = new Hook<>();
    public static final Hook<Hooks.EquipmentChange> EQUIPMENT_CHANGE = new Hook<>();
    public static final Hook<Hooks.LivingJump> LIVING_JUMP = new Hook<>();
    public static final Hook<Hooks.LivingFall> LIVING_FALL = new Hook<>();
    public static final Hook<Hooks.LivingHurt> LIVING_HURT = new Hook<>();
    public static final Hook<Hooks.EffectAdded> EFFECT_ADDED = new Hook<>();
    public static final Hook<Hooks.ContainerClose> CONTAINER_CLOSE = new Hook<>();
    public static final Hook<Hooks.ContainerOpen> CONTAINER_OPEN = new Hook<>();
    public static final Hook<Hooks.ItemCrafted> ITEM_CRAFTED = new Hook<>();
    public static final Hook<Hooks.ItemPickup> ITEM_PICKUP = new Hook<>();
    public static final Hook<Hooks.ItemSmelted> ITEM_SMELTED = new Hook<>();
    public static final Hook<Hooks.ChangedDimension> CHANGED_DIMENSION = new Hook<>();
    public static final Hook<Hooks.PlayerLoggedIn> PLAYER_LOGGED_IN = new Hook<>();
    public static final Hook<Hooks.PlayerLoggedOut> PLAYER_LOGGED_OUT = new Hook<>();
    public static final Hook<Hooks.PlayerRespawn> PLAYER_RESPAWN = new Hook<>();
    public static final Hook<Hooks.StartTracking> START_TRACKING = new Hook<>();
    public static final Hook<Hooks.EntityInteract> ENTITY_INTERACT = new Hook<>();
    public static final Hook<Hooks.LeftClickBlock> LEFT_CLICK_BLOCK = new Hook<>();
    public static final Hook<Hooks.RightClickBlock> RIGHT_CLICK_BLOCK = new Hook<>();
    public static final Hook<Hooks.RightClickItem> RIGHT_CLICK_ITEM = new Hook<>();
    public static final Hook<Hooks.SleepInBed> SLEEP_IN_BED = new Hook<>();
    public static final Hook<Hooks.WakeUp> WAKE_UP = new Hook<>();
    public static final Hook<Hooks.XpLevelChange> XP_LEVEL_CHANGE = new Hook<>();
    public static final Hook<Hooks.XpPickup> XP_PICKUP = new Hook<>();
    public static final Hook<Hooks.ServerChat> SERVER_CHAT = new Hook<>(true);
    public static final Hook<Hooks.Trade> TRADE = new Hook<>();

    private Hooks() {
    }

    public static final class AdvancementEarn {
        private final Player entity;
        private final Advancement advancement;

        public AdvancementEarn(Player entity, Advancement advancement) {
            this.entity = entity;
            this.advancement = advancement;
        }

        public Player getEntity() {
            return this.entity;
        }

        public Advancement getAdvancement() {
            return this.advancement;
        }
    }

    public static final class AnimalTame extends Cancellable {
        private final Animal animal;
        private final Player tamer;

        public AnimalTame(Animal animal, Player tamer) {
            this.animal = animal;
            this.tamer = tamer;
        }

        public Animal getAnimal() {
            return this.animal;
        }

        public Player getTamer() {
            return this.tamer;
        }
    }

    public static final class ArrowLoose {
        private final Player entity;
        private final ItemStack bow;
        private final int charge;

        public ArrowLoose(Player entity, ItemStack bow, int charge) {
            this.entity = entity;
            this.bow = bow;
            this.charge = charge;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getBow() {
            return this.bow;
        }

        public int getCharge() {
            return this.charge;
        }
    }

    public static final class AttackEntity extends Cancellable {
        private final Player entity;
        private final Entity target;

        public AttackEntity(Player entity, Entity target) {
            this.entity = entity;
            this.target = target;
        }

        public Player getEntity() {
            return this.entity;
        }

        public Entity getTarget() {
            return this.target;
        }
    }

    public static final class BabySpawn extends Cancellable {
        private final AgeableMob child;
        private final Player causedByPlayer;

        public BabySpawn(AgeableMob child, Player causedByPlayer) {
            this.child = child;
            this.causedByPlayer = causedByPlayer;
        }

        public AgeableMob getChild() {
            return this.child;
        }

        public Player getCausedByPlayer() {
            return this.causedByPlayer;
        }
    }

    public static final class BlockBreak extends Cancellable {
        private final BlockPos pos;
        private final BlockState state;
        private final Player player;

        public BlockBreak(BlockPos pos, BlockState state, Player player) {
            this.pos = pos;
            this.state = state;
            this.player = player;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public BlockState getState() {
            return this.state;
        }

        public Player getPlayer() {
            return this.player;
        }
    }

    public static final class BlockPlace extends Cancellable {
        private final Entity entity;
        private final BlockPos pos;
        private final BlockState placedBlock;

        public BlockPlace(Entity entity, BlockPos pos, BlockState placedBlock) {
            this.entity = entity;
            this.pos = pos;
            this.placedBlock = placedBlock;
        }

        public Entity getEntity() {
            return this.entity;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public BlockState getPlacedBlock() {
            return this.placedBlock;
        }
    }

    public static final class Bonemeal {
        private final Player entity;
        private final BlockPos pos;
        private final BlockState block;

        public Bonemeal(Player entity, BlockPos pos, BlockState block) {
            this.entity = entity;
            this.pos = pos;
            this.block = block;
        }

        public Player getEntity() {
            return this.entity;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public BlockState getBlock() {
            return this.block;
        }
    }

    public static final class ChangedDimension {
        private final Player entity;
        private final ResourceKey<Level> from;
        private final ResourceKey<Level> to;

        public ChangedDimension(Player entity, ResourceKey<Level> from, ResourceKey<Level> to) {
            this.entity = entity;
            this.from = from;
            this.to = to;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ResourceKey<Level> getFrom() {
            return this.from;
        }

        public ResourceKey<Level> getTo() {
            return this.to;
        }
    }

    public static final class CommonSetup {
    }

    public static final class ContainerClose {
        private final Player entity;
        private final AbstractContainerMenu container;

        public ContainerClose(Player entity, AbstractContainerMenu container) {
            this.entity = entity;
            this.container = container;
        }

        public Player getEntity() {
            return this.entity;
        }

        public AbstractContainerMenu getContainer() {
            return this.container;
        }
    }

    public static final class ContainerOpen {
        private final Player entity;
        private final AbstractContainerMenu container;

        public ContainerOpen(Player entity, AbstractContainerMenu container) {
            this.entity = entity;
            this.container = container;
        }

        public Player getEntity() {
            return this.entity;
        }

        public AbstractContainerMenu getContainer() {
            return this.container;
        }
    }

    public static final class EffectAdded {
        private final LivingEntity entity;
        private final MobEffectInstance effectInstance;

        public EffectAdded(LivingEntity entity, MobEffectInstance effectInstance) {
            this.entity = entity;
            this.effectInstance = effectInstance;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public MobEffectInstance getEffectInstance() {
            return this.effectInstance;
        }
    }

    public static final class EntityInteract {
        private final Player entity;
        private final Level level;
        private final InteractionHand hand;
        private final Entity target;
        private final ItemStack itemStack;

        public EntityInteract(Player entity, Level level, InteractionHand hand, Entity target, ItemStack itemStack) {
            this.entity = entity;
            this.level = level;
            this.hand = hand;
            this.target = target;
            this.itemStack = itemStack;
        }

        public Player getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }

        public InteractionHand getHand() {
            return this.hand;
        }

        public Entity getTarget() {
            return this.target;
        }

        public ItemStack getItemStack() {
            return this.itemStack;
        }
    }

    public static final class EntityJoin extends Cancellable {
        private final Entity entity;
        private final Level level;

        public EntityJoin(Entity entity, Level level) {
            this.entity = entity;
            this.level = level;
        }

        public Entity getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }
    }

    public static final class EntityLeave {
        private final Entity entity;
        private final Level level;

        public EntityLeave(Entity entity, Level level) {
            this.entity = entity;
            this.level = level;
        }

        public Entity getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }
    }

    public static final class EntityMount extends Cancellable {
        private final Entity entityMounting;
        private final Entity entityBeingMounted;
        private final boolean mounting;

        public EntityMount(Entity entityMounting, Entity entityBeingMounted, boolean mounting) {
            this.entityMounting = entityMounting;
            this.entityBeingMounted = entityBeingMounted;
            this.mounting = mounting;
        }

        public Entity getEntityMounting() {
            return this.entityMounting;
        }

        public Entity getEntityBeingMounted() {
            return this.entityBeingMounted;
        }

        public boolean isMounting() {
            return this.mounting;
        }
    }

    public static final class EquipmentChange {
        private final LivingEntity entity;
        private final EquipmentSlot slot;
        private final ItemStack to;

        public EquipmentChange(LivingEntity entity, EquipmentSlot slot, ItemStack to) {
            this.entity = entity;
            this.slot = slot;
            this.to = to;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public EquipmentSlot getSlot() {
            return this.slot;
        }

        public ItemStack getTo() {
            return this.to;
        }
    }

    public static final class ExplosionDetonate {
        private final Explosion explosion;
        private final Level level;
        private final Vec3 position;

        public ExplosionDetonate(Explosion explosion, Level level, Vec3 position) {
            this.explosion = explosion;
            this.level = level;
            this.position = position;
        }

        public Vec3 getPosition() {
            return this.position;
        }

        public Explosion getExplosion() {
            return this.explosion;
        }

        public Level getLevel() {
            return this.level;
        }
    }

    public static final class FillBucket {
        private final Player entity;
        private final ItemStack emptyBucket;

        public FillBucket(Player entity, ItemStack emptyBucket) {
            this.entity = entity;
            this.emptyBucket = emptyBucket;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getEmptyBucket() {
            return this.emptyBucket;
        }
    }

    public static final class ItemCrafted {
        private final Player entity;
        private final ItemStack crafting;

        public ItemCrafted(Player entity, ItemStack crafting) {
            this.entity = entity;
            this.crafting = crafting;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getCrafting() {
            return this.crafting;
        }
    }

    public static final class ItemFished {
        private final Player entity;
        private final NonNullList<ItemStack> drops;

        public ItemFished(Player entity, NonNullList<ItemStack> drops) {
            this.entity = entity;
            this.drops = drops;
        }

        public Player getEntity() {
            return this.entity;
        }

        public NonNullList<ItemStack> getDrops() {
            return this.drops;
        }
    }

    public static final class ItemPickup {
        private final Player entity;
        private final ItemStack stack;

        public ItemPickup(Player entity, ItemStack stack) {
            this.entity = entity;
            this.stack = stack;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getStack() {
            return this.stack;
        }
    }

    public static final class ItemSmelted {
        private final Player entity;
        private final ItemStack smelting;

        public ItemSmelted(Player entity, ItemStack smelting) {
            this.entity = entity;
            this.smelting = smelting;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getSmelting() {
            return this.smelting;
        }
    }

    public static final class ItemToss extends Cancellable {
        private final ItemEntity entity;
        private final Player player;

        public ItemToss(ItemEntity entity, Player player) {
            this.entity = entity;
            this.player = player;
        }

        public ItemEntity getEntity() {
            return this.entity;
        }

        public Player getPlayer() {
            return this.player;
        }
    }

    public static final class LeftClickBlock {
        private final Player entity;
        private final Level level;
        private final BlockPos pos;
        private final ItemStack itemStack;
        private final boolean start;

        public LeftClickBlock(Player entity, Level level, BlockPos pos, ItemStack itemStack, boolean start) {
            this.entity = entity;
            this.level = level;
            this.pos = pos;
            this.itemStack = itemStack;
            this.start = start;
        }

        public Player getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public ItemStack getItemStack() {
            return this.itemStack;
        }

        public boolean isStart() {
            return this.start;
        }
    }

    public static final class LevelSave {
        private final LevelAccessor level;

        public LevelSave(LevelAccessor level) {
            this.level = level;
        }

        public LevelAccessor getLevel() {
            return this.level;
        }
    }

    public static final class LivingAttack extends Cancellable {
        private final LivingEntity entity;
        private final DamageSource source;
        private final float amount;

        public LivingAttack(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }

        public float getAmount() {
            return this.amount;
        }
    }

    public static final class LivingDeath extends Cancellable {
        private final LivingEntity entity;
        private final DamageSource source;

        public LivingDeath(LivingEntity entity, DamageSource source) {
            this.entity = entity;
            this.source = source;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }
    }

    public static final class LivingFall {
        private final LivingEntity entity;
        private float distance;
        private float damageMultiplier;

        public LivingFall(LivingEntity entity, float distance, float damageMultiplier) {
            this.entity = entity;
            this.distance = distance;
            this.damageMultiplier = damageMultiplier;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public float getDistance() {
            return this.distance;
        }

        public void setDistance(float value) {
            this.distance = value;
        }

        public float getDamageMultiplier() {
            return this.damageMultiplier;
        }

        public void setDamageMultiplier(float value) {
            this.damageMultiplier = value;
        }
    }

    public static final class LivingHurt extends Cancellable {
        private final LivingEntity entity;
        private final DamageSource source;
        private final float amount;

        public LivingHurt(LivingEntity entity, DamageSource source, float amount) {
            this.entity = entity;
            this.source = source;
            this.amount = amount;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public DamageSource getSource() {
            return this.source;
        }

        public float getAmount() {
            return this.amount;
        }
    }

    public static final class LivingJump {
        private final LivingEntity entity;

        public LivingJump(LivingEntity entity) {
            this.entity = entity;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }
    }

    public static final class PlayerLoggedIn {
        private final Player entity;

        public PlayerLoggedIn(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return this.entity;
        }
    }

    public static final class PlayerLoggedOut {
        private final Player entity;

        public PlayerLoggedOut(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return this.entity;
        }
    }

    public static final class PlayerRespawn {
        private final Player entity;

        public PlayerRespawn(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return this.entity;
        }
    }

    public static final class PlayerTick {
        public final TickPhase phase;
        public final Player player;

        public PlayerTick(TickPhase phase, Player player) {
            this.phase = phase;
            this.player = player;
        }
    }

    public static final class RegisterCommands {
        private final CommandDispatcher<CommandSourceStack> dispatcher;

        public RegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
            this.dispatcher = dispatcher;
        }

        public CommandDispatcher<CommandSourceStack> getDispatcher() {
            return this.dispatcher;
        }
    }

    public static final class RightClickBlock {
        private final Player entity;
        private final Level level;
        private final InteractionHand hand;
        private final BlockPos pos;
        private final ItemStack itemStack;

        public RightClickBlock(Player entity, Level level, InteractionHand hand, BlockPos pos, ItemStack itemStack) {
            this.entity = entity;
            this.level = level;
            this.hand = hand;
            this.pos = pos;
            this.itemStack = itemStack;
        }

        public Player getEntity() {
            return this.entity;
        }

        public Level getLevel() {
            return this.level;
        }

        public InteractionHand getHand() {
            return this.hand;
        }

        public BlockPos getPos() {
            return this.pos;
        }

        public ItemStack getItemStack() {
            return this.itemStack;
        }
    }

    public static final class RightClickItem {
        private final Player entity;
        private final ItemStack itemStack;

        public RightClickItem(Player entity, ItemStack itemStack) {
            this.entity = entity;
            this.itemStack = itemStack;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ItemStack getItemStack() {
            return this.itemStack;
        }
    }

    public static final class ServerAboutToStart {
        private final MinecraftServer server;

        public ServerAboutToStart(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static final class ServerChat {
        private final ServerPlayer player;
        private final String rawText;

        public ServerChat(ServerPlayer player, String rawText) {
            this.player = player;
            this.rawText = rawText;
        }

        public ServerPlayer getPlayer() {
            return this.player;
        }

        public String getRawText() {
            return this.rawText;
        }
    }

    public static final class ServerStopped {
        private final MinecraftServer server;

        public ServerStopped(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static final class ServerStopping {
        private final MinecraftServer server;

        public ServerStopping(MinecraftServer server) {
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static final class ServerTick {
        public final TickPhase phase;
        private final MinecraftServer server;

        public ServerTick(TickPhase phase, MinecraftServer server) {
            this.phase = phase;
            this.server = server;
        }

        public MinecraftServer getServer() {
            return this.server;
        }
    }

    public static final class SleepInBed {
        private final Player entity;
        private final BlockPos pos;

        public SleepInBed(Player entity, BlockPos pos) {
            this.entity = entity;
            this.pos = pos;
        }

        public Player getEntity() {
            return this.entity;
        }

        public BlockPos getPos() {
            return this.pos;
        }
    }

    public static final class StartTracking {
        private final Player entity;
        private final Entity target;

        public StartTracking(Player entity, Entity target) {
            this.entity = entity;
            this.target = target;
        }

        public Player getEntity() {
            return this.entity;
        }

        public Entity getTarget() {
            return this.target;
        }
    }

    public static final class Trade {
        private final Player entity;
        private final AbstractVillager abstractVillager;
        private final MerchantOffer merchantOffer;

        public Trade(Player entity, AbstractVillager abstractVillager, MerchantOffer merchantOffer) {
            this.entity = entity;
            this.abstractVillager = abstractVillager;
            this.merchantOffer = merchantOffer;
        }

        public Player getEntity() {
            return this.entity;
        }

        public AbstractVillager getAbstractVillager() {
            return this.abstractVillager;
        }

        public MerchantOffer getMerchantOffer() {
            return this.merchantOffer;
        }
    }

    public static final class UseItemFinish {
        private final LivingEntity entity;
        private final ItemStack item;

        public UseItemFinish(LivingEntity entity, ItemStack item) {
            this.entity = entity;
            this.item = item;
        }

        public LivingEntity getEntity() {
            return this.entity;
        }

        public ItemStack getItem() {
            return this.item;
        }
    }

    public static final class WakeUp {
        private final Player entity;

        public WakeUp(Player entity) {
            this.entity = entity;
        }

        public Player getEntity() {
            return this.entity;
        }
    }

    public static final class XpLevelChange {
        private final Player entity;
        private final int levels;

        public XpLevelChange(Player entity, int levels) {
            this.entity = entity;
            this.levels = levels;
        }

        public Player getEntity() {
            return this.entity;
        }

        public int getLevels() {
            return this.levels;
        }
    }

    public static final class XpPickup {
        private final Player entity;
        private final ExperienceOrb orb;

        public XpPickup(Player entity, ExperienceOrb orb) {
            this.entity = entity;
            this.orb = orb;
        }

        public Player getEntity() {
            return this.entity;
        }

        public ExperienceOrb getOrb() {
            return this.orb;
        }
    }
}
