package org.sprautestudio.spraute_engine.custom;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.SpawnPlacements.Type;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.mixin.SpawnPlacementsAccess;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.models.EntityLibrary;
import org.sprautestudio.spraute_engine.runtime.models.ModelLibrary;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

public final class CustomMobs {
    public static final String FOLDER = "mobs";
    private static final Map<String, CustomMobs.Registered> REGISTERED = new LinkedHashMap<>();
    private static Map<EntityType<?>, CustomMobs.Registered> byType;
    private static final Map<String, CustomMobs.Fresh> CURRENT = new ConcurrentHashMap<>();
    private static final long FRESH_FOR_MS = 3000L;

    private CustomMobs() {
    }

    public static Map<String, CustomMobs.Registered> registered() {
        return REGISTERED;
    }

    public static List<Supplier<EntityType<SprauteModelEntity>>> types() {
        List<Supplier<EntityType<SprauteModelEntity>>> out = new ArrayList<>();

        for (CustomMobs.Registered r : REGISTERED.values()) {
            out.add(r.type());
        }

        return out;
    }

    public static void register() {
        Path root = CustomItems.projectsDir();
        if (Files.isDirectory(root)) {
            try (Stream<Path> projects = Files.list(root)) {
                for (Path project : (Iterable<Path>) projects.filter(x$0 -> Files.isDirectory(x$0)).sorted()::iterator) {
                    registerProject(project);
                }
            } catch (IOException var6) {
                SprauteEngine.LOGGER.error("Spraute: cannot list the projects for their mobs", (Throwable)var6);
            }

            if (!REGISTERED.isEmpty()) {
                SprauteEngine.LOGGER.info("Spraute: {} mobs of projects are in the game", REGISTERED.size());
            }
        }
    }

    private static void registerProject(Path project) throws IOException {
        for (MobDef d : read(project)) {
            String problem = d.problem();
            if (problem != null) {
                SprauteEngine.LOGGER.error("Spraute: the mob {} of {} is not added: {}", d.id, project.getFileName(), problem);
            } else if (REGISTERED.containsKey(d.id)) {
                SprauteEngine.LOGGER.error("Spraute: the mob {} is in two projects: the first is used", d.id);
            } else {
                String name = "mob/" + d.id;
                MobCategory category = d.hostile() ? MobCategory.MONSTER : MobCategory.CREATURE;
                Supplier<EntityType<SprauteModelEntity>> type = Platform.get()
                    .entityType(name, () -> Builder.of(Platform.get()::modelEntity, category).sized(0.6F, 1.9F).clientTrackingRange(10).build(name));
                Platform.get()
                    .entityAttributes(
                        type,
                        () -> Mob.createMobAttributes()
                            .add(Attributes.MOVEMENT_SPEED, 0.25)
                            .add(Attributes.FOLLOW_RANGE, 48.0)
                            .add(Attributes.ATTACK_DAMAGE, 2.0)
                            .build()
                    );
                if (d.spawns) {
                    Platform.get().mobSpawn(type, category, d.weight, d.groupMin, d.groupMax, d.dimension, d.biomes);
                }

                REGISTERED.put(d.id, new CustomMobs.Registered(d, project, d.restartKey(), type));
            }
        }
    }

    public static List<MobDef> read(Path project) throws IOException {
        List<MobDef> out = new ArrayList<>();
        return out;
    }

    static void writeData(Path root) throws IOException {
        for (CustomMobs.Registered r : REGISTERED.values()) {
            for (Entry<String, String> file : r.def().forgeData().entrySet()) {
                Path to = root.resolve(file.getKey());
                Files.createDirectories(to.getParent());
                Files.writeString(to, file.getValue(), StandardCharsets.UTF_8);
            }
        }
    }

    public static void registerHooks() {
        Hooks.COMMON_SETUP
            .add(
                e -> {
                    for (CustomMobs.Registered r : REGISTERED.values()) {
                        if (r.def().spawns) {
                            boolean hostile = r.def().hostile();
                            SpawnPlacementsAccess.spraute$register(
                                r.type().get(),
                                Type.ON_GROUND,
                                Types.MOTION_BLOCKING_NO_LEAVES,
                                (type, level, reason, pos, random) -> hostile
                                    ? level.getDifficulty() != Difficulty.PEACEFUL
                                        && Monster.isDarkEnoughToSpawn(level, pos, random)
                                        && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                                    : level.getRawBrightness(pos, 0) > 8 && Mob.checkMobSpawnRules(type, level, reason, pos, random)
                            );
                        }
                    }
                }
            );
    }

    private static CustomMobs.Registered of(Entity e) {
        if (REGISTERED.isEmpty()) {
            return null;
        } else {
            if (byType == null) {
                Map<EntityType<?>, CustomMobs.Registered> made = new IdentityHashMap<>();

                for (CustomMobs.Registered r : REGISTERED.values()) {
                    made.put(r.type().get(), r);
                }

                byType = made;
            }

            return byType.get(e.getType());
        }
    }

    public static boolean isMob(Entity e) {
        return of(e) != null;
    }

    private static MobDef current(CustomMobs.Registered r) {
        String id = r.def().id;
        long now = System.currentTimeMillis();
        CustomMobs.Fresh known = CURRENT.get(id);
        if (known != null && now - known.at() < 3000L) {
            return known.def();
        } else {
            MobDef d = r.def();
            Path f = r.project().resolve("mobs").resolve(id + ".json");

            try {
                if (Files.isRegularFile(f)) {
                    d = MobDef.fromJson(Files.readString(f, StandardCharsets.UTF_8));
                    d.id = id;
                }
            } catch (RuntimeException | IOException var8) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read the mob {}", f, var8);
            }

            CURRENT.put(id, new CustomMobs.Fresh(d, now));
            return d;
        }
    }

    private static MobDef def(Entity e) {
        CustomMobs.Registered r = of(e);
        return r != null ? current(r) : null;
    }

    public static boolean hostile(Entity e) {
        CustomMobs.Registered r = of(e);
        return r != null && r.def().hostile();
    }

    public static void goals(SprauteModelEntity e, GoalSelector goals, GoalSelector targets) {
        CustomMobs.Registered r = of(e);
        if (r != null) {
            goals.addGoal(0, new FloatGoal(e));
            if (r.def().passive()) {
                goals.addGoal(1, new PanicGoal(e, 1.4));
            } else {
                goals.addGoal(2, new MeleeAttackGoal(e, 1.2, false));
                targets.addGoal(1, new HurtByTargetGoal(e));
            }

            if (r.def().hostile()) {
                targets.addGoal(2, new NearestAttackableTargetGoal<>(e, Player.class, true));
            }

            goals.addGoal(5, new WaterAvoidingRandomStrollGoal(e, 1.0));
            goals.addGoal(6, new LookAtPlayerGoal(e, Player.class, 8.0F));
            goals.addGoal(7, new RandomLookAroundGoal(e));
        }
    }

    public static void setup(SprauteModelEntity e) {
        CustomMobs.Registered r = of(e);
        if (r != null && !Mc.level(e).isClientSide) {
            MobDef d = current(r);
            e.canDie = true;
            e.setVulnerable(true);
            e.setModel(d.look.isBlank() ? "builtin/humanoid" : ModelLibrary.projectNameOf(r.project()) + "/" + d.look);
            stats(e);
        }
    }

    public static void stats(SprauteModelEntity e) {
        MobDef d = def(e);
        if (d != null) {
            set(e, Attributes.MAX_HEALTH, d.health);
            set(e, Attributes.ATTACK_DAMAGE, d.damage);
            set(e, Attributes.MOVEMENT_SPEED, 0.25 * d.speed);
            set(e, Attributes.FOLLOW_RANGE, d.followRange);
            set(e, Attributes.ARMOR, d.armor);
            set(e, Attributes.KNOCKBACK_RESISTANCE, d.knockbackResistance);
            e.setHealth(d.health);
            e.setXpReward(d.xp);
        }
    }

    private static void set(SprauteModelEntity e, Attribute a, double value) {
        AttributeInstance attr = e.getAttribute(a);
        if (attr != null) {
            attr.setBaseValue(value);
        }
    }

    public static Component name(Entity e) {
        MobDef d = def(e);
        return d != null ? Component.literal(d.name) : null;
    }

    public static ResourceLocation loot(Entity e) {
        MobDef d = def(e);
        return d != null && !d.loot.isBlank() ? ResourceLocation.tryParse(LootDef.ref(d.loot.trim())) : null;
    }

    public static SoundEvent ambient(Entity e) {
        MobDef d = def(e);
        return d != null ? CustomItems.soundOf(d.ambientSound) : null;
    }

    public static SoundEvent hurt(Entity e) {
        MobDef d = def(e);
        return d != null ? CustomItems.soundOf(d.hurtSound) : null;
    }

    public static SoundEvent death(Entity e) {
        MobDef d = def(e);
        return d != null ? CustomItems.soundOf(d.deathSound) : null;
    }

    public static void attacked(SprauteModelEntity e) {
        MobDef d = def(e);
        if (d != null && !d.attackAnim.isBlank()) {
            EntityLibrary.Look look = EntityLibrary.look(e.model());
            if (look != null) {
                look.animation(d.attackAnim).ifPresent(a -> e.startAnimation("action", a.configure().transitionTime(0.1F)));
            }
        }
    }

    public static void born(SprauteModelEntity e, String reason) {
        CustomMobs.Registered r = of(e);
        if (r != null) {
            MobDef d = current(r);
            run(e, d.spawnCommand);
            GameEvents.mobSpawn(e, d.id, reason);
        }
    }

    public static void died(SprauteModelEntity e) {
        MobDef d = def(e);
        if (d != null) {
            run(e, d.deathCommand);
        }
    }

    private static void run(SprauteModelEntity e, String command) {
        if (command != null && !command.isBlank()) {
            MinecraftServer server = Mc.level(e).getServer();
            if (server != null && Mc.level(e) instanceof ServerLevel level) {
                String line = command.startsWith("/") ? command.substring(1) : command;
                server.getCommands()
                    .performPrefixedCommand(
                        server.createCommandSourceStack()
                            .withEntity(e)
                            .withLevel(level)
                            .withPosition(new Vec3(e.getX(), e.getY(), e.getZ()))
                            .withSuppressedOutput(),
                        line
                    );
            }
        }
    }

    private record Fresh(MobDef def, long at) {
    }

    public record Registered(MobDef def, Path project, String restartKey, Supplier<EntityType<SprauteModelEntity>> type) {
    }
}
