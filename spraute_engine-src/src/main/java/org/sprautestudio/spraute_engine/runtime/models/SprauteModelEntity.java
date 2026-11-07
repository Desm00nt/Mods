package org.sprautestudio.spraute_engine.runtime.models;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.MovementActionType;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.custom.CustomMobs;
import org.sprautestudio.spraute_engine.hitbox.Hitboxes;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.AttackBoxes;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public class SprauteModelEntity extends AnimatedEntity {
    private static final EntityDataAccessor<String> MODEL = SynchedEntityData.defineId(SprauteModelEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PARTS_OFF = SynchedEntityData.defineId(SprauteModelEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> BODY_SCALE = SynchedEntityData.defineId(SprauteModelEntity.class, EntityDataSerializers.FLOAT);
    private String appliedFor = "";
    public static final int MAX_PARTS = 8;
    private final BodyPart[] parts = new BodyPart[8];
    private boolean wild;
    private boolean announced;
    private String bornHow = "command";
    private static AtomicInteger counter;
    private final Map<String, Float> damageOverride = new HashMap<>();
    private String lastPart = "";
    private boolean inPartHit;
    private final Map<String, Float> partAttack = new HashMap<>();
    public final Map<String, PartShape.BonePose> bonePoses = new ConcurrentHashMap<>();
    public volatile long boneWatchUntil;
    public long boneAskedAt;
    public Matrix4f renderBase;
    private final PartShape[] shapes = new PartShape[8];
    private boolean partsParked;
    private float sizedScale = Float.NaN;
    private float sizedWidth;
    private float sizedHeight;

    public SprauteModelEntity(EntityType<? extends AnimatedEntity> type, Level level) {
        super(type, level);

        for (int i = 0; i < 8; i++) {
            this.parts[i] = Platform.get().bodyPart(this, i);
        }

        this.setId(idCounter().getAndAdd(9) + 1);
        CustomMobs.setup(this);
    }

    @Override
    protected void registerGoals() {
        CustomMobs.goals(this, this.goalSelector, this.targetSelector);
    }

    public void setXpReward(int xp) {
        this.xpReward = xp;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type, SpawnGroupData data, CompoundTag tag) {
        if (type == MobSpawnType.NATURAL || type == MobSpawnType.CHUNK_GENERATION) {
            this.wild = true;
            this.bornHow = "natural";
        }

        return super.finalizeSpawn(level, difficulty, type, data, tag);
    }

    @Override
    public boolean isPersistenceRequired() {
        return !this.wild && super.isPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.wild && CustomMobs.hostile(this);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            CustomMobs.attacked(this);
        }

        return hit;
    }

    @Override
    public void die(DamageSource source) {
        boolean wasDead = this.isRemoved() || this.dead;
        super.die(source);
        if (!wasDead && this.dead && !Mc.level(this).isClientSide) {
            CustomMobs.died(this);
        }
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        ResourceLocation own = CustomMobs.loot(this);
        return own != null ? own : super.getDefaultLootTable();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        SoundEvent own = CustomMobs.ambient(this);
        return own != null ? own : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        SoundEvent own = CustomMobs.hurt(this);
        return own != null ? own : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        SoundEvent own = CustomMobs.death(this);
        return own != null ? own : super.getDeathSound();
    }

    @Override
    protected Component getTypeName() {
        Component own = CustomMobs.name(this);
        return own != null ? own : super.getTypeName();
    }

    private static AtomicInteger idCounter() {
        if (counter == null) {
            for (Field f : Entity.class.getDeclaredFields()) {
                if (f.getType() == AtomicInteger.class && Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        counter = (AtomicInteger)f.get(null);
                    } catch (ReflectiveOperationException var5) {
                        throw new IllegalStateException(var5);
                    }
                }
            }
        }

        return counter;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer p) {
        return SprauteRuntime.sees(this, p) && super.broadcastToPlayer(p);
    }

    @Override
    public void setId(int id) {
        super.setId(id);

        for (int i = 0; i < 8; i++) {
            if (this.parts[i] != null) {
                this.parts[i].entity().setId(id + i + 1);
            }
        }
    }

    public BodyPart[] parts() {
        return this.parts;
    }

    private List<EntityLibrary.Def.Part> partDefs() {
        EntityLibrary.Look l = this.look();
        return l != null && l.def() != null ? l.def().parts : List.of();
    }

    private boolean off(String name) {
        String o = this.entityData.get(PARTS_OFF);
        if (o.isEmpty()) {
            return false;
        } else {
            for (String s : o.split(",")) {
                if (s.equals(name)) {
                    return true;
                }
            }

            return false;
        }
    }

    public List<String> partNames() {
        List<String> out = new ArrayList<>();

        for (EntityLibrary.Def.Part p : this.partDefs()) {
            out.add(p.name);
        }

        return out;
    }

    public AABB partBox(String name) {
        List<EntityLibrary.Def.Part> defs = this.partDefs();

        for (int i = 0; i < defs.size() && i < 8; i++) {
            if (defs.get(i).name.equals(name)) {
                return this.parts[i].entity().getBoundingBox();
            }
        }

        return null;
    }

    public boolean partEnabled(String name) {
        for (EntityLibrary.Def.Part p : this.partDefs()) {
            if (p.name.equals(name)) {
                return p.enabled && !this.off(name);
            }
        }

        return false;
    }

    public void setPartEnabled(String name, boolean on) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String o = this.entityData.get(PARTS_OFF);
        if (!o.isEmpty()) {
            set.addAll(Arrays.asList(o.split(",")));
        }

        if (on) {
            set.remove(name);
        } else {
            set.add(name);
        }

        this.entityData.set(PARTS_OFF, String.join(",", set));
    }

    public void setPartAttack(String name, float damage) {
        if (damage <= 0.0F) {
            this.partAttack.remove(name);
        } else {
            this.partAttack.put(name, damage);
        }
    }

    public float partAttack(String name) {
        return this.partAttack.getOrDefault(name, 0.0F);
    }

    public void setPartDamage(String name, float mult) {
        this.damageOverride.put(name, Math.max(0.0F, mult));
    }

    public float partDamage(String name) {
        Float o = this.damageOverride.get(name);
        if (o != null) {
            return o;
        } else {
            for (EntityLibrary.Def.Part p : this.partDefs()) {
                if (p.name.equals(name)) {
                    return p.damage;
                }
            }

            return 1.0F;
        }
    }

    public String lastPart() {
        return this.lastPart;
    }

    public PartShape shape(int i) {
        return i >= 0 && i < 8 ? this.shapes[i] : null;
    }

    public List<EntityLibrary.Def.Part> partList() {
        return this.partDefs();
    }

    public PartShape.BonePose freshPose(String bone) {
        if (bone != null && !bone.isEmpty()) {
            PartShape.BonePose bp = this.bonePoses.get(bone);
            return bp != null && System.currentTimeMillis() - bp.at() < 2000L ? bp : null;
        } else {
            return null;
        }
    }

    private void placeParts() {
        List<EntityLibrary.Def.Part> defs = this.partDefs();
        if (!defs.isEmpty() || !this.partsParked || this.tickCount % 20 == 0) {
            this.partsParked = defs.isEmpty();
            float s = this.lookScale();

            for (int i = 0; i < 8; i++) {
                BodyPart bodyPart = this.parts[i];
                Entity part = bodyPart.entity();
                EntityLibrary.Def.Part d = i < defs.size() ? defs.get(i) : null;
                part.xo = part.getX();
                part.yo = part.getY();
                part.zo = part.getZ();
                part.xOld = part.getX();
                part.yOld = part.getY();
                part.zOld = part.getZ();
                if (d != null && d.enabled && !this.off(d.name)) {
                    bodyPart.setActive(true);
                    PartShape sh = PartShape.of(d, s, this.yBodyRot, this.freshPose(d.bone));
                    this.shapes[i] = sh;
                    AABB b = sh.bounds(this.getX(), this.getY(), this.getZ());
                    bodyPart.resize((float)Math.max(0.02, Math.max(b.getXsize(), b.getZsize())), (float)Math.max(0.02, b.getYsize()));
                    part.setPos((b.minX + b.maxX) / 2.0, b.minY, (b.minZ + b.maxZ) / 2.0);
                } else {
                    bodyPart.setActive(false);
                    this.shapes[i] = null;
                    bodyPart.resize(0.01F, 0.01F);
                    part.setPos(this.getX(), this.getY(), this.getZ());
                }
            }
        }
    }

    private Vec3[] blowLine(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct == null) {
            return null;
        } else {
            Vec3 me = this.position();
            Vec3 from;
            Vec3 to;
            if (direct instanceof LivingEntity le) {
                from = le.getEyePosition();
                to = from.add(le.getViewVector(1.0F).scale(8.0));
            } else {
                Vec3 v = direct.getDeltaMovement();
                if (v.lengthSqr() < 1.0E-6) {
                    return null;
                }

                from = direct.position().subtract(v.scale(2.0));
                to = direct.position().add(v.scale(2.0));
            }

            return new Vec3[]{from.subtract(me), to.subtract(me)};
        }
    }

    private int firstHit(DamageSource source) {
        Vec3[] line = this.blowLine(source);
        if (line == null) {
            return -2;
        } else {
            int best = -2;
            double bestT = Double.MAX_VALUE;

            for (int i = 0; i < 8; i++) {
                if (this.shapes[i] != null && this.parts[i].active()) {
                    double t = this.shapes[i].hit(line[0], line[1]);
                    if (t >= 0.0 && t < bestT) {
                        bestT = t;
                        best = i;
                    }
                }
            }

            AABB body = this.getBoundingBox().move(-this.getX(), -this.getY(), -this.getZ());
            Optional<Vec3> bh = body.clip(line[0], line[1]);
            if (body.contains(line[0])) {
                bh = Optional.of(line[0]);
            }

            if (bh.isPresent()) {
                double t = bh.get().distanceTo(line[0]) / Math.max(1.0E-6, line[1].distanceTo(line[0]));
                if (t < bestT) {
                    best = -1;
                }
            }

            return best;
        }
    }

    public boolean hurtPart(BodyPart part, DamageSource source, float amount) {
        List<EntityLibrary.Def.Part> defs = this.partDefs();
        if (part.index() >= defs.size()) {
            return false;
        } else {
            int real = Mc.level(this).isClientSide ? part.index() : this.firstHit(source);
            if (real == -1) {
                return this.hurt(source, amount);
            } else {
                if (real >= 0 && real < defs.size()) {
                    part = this.parts[real];
                }

                String name = defs.get(part.index()).name;
                float dmg = amount * this.partDamage(name);
                this.report(name, source, dmg, part.entity().getBoundingBox().getCenter());
                if (dmg <= 0.0F) {
                    Mc.level(this)
                        .playSound(
                            null, part.entity().getX(), part.entity().getY(), part.entity().getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 0.8F, 1.2F
                        );
                    return false;
                } else {
                    this.inPartHit = true;

                    boolean var8;
                    try {
                        var8 = this.hurt(source, dmg);
                    } finally {
                        this.inPartHit = false;
                    }

                    return var8;
                }
            }
        }
    }

    private void report(String name, DamageSource source, float dmg, Vec3 at) {
        this.lastPart = name;
        if (!Mc.level(this).isClientSide) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("part", name);
            d.put("damage", Math.round(dmg * 10.0F) / 10.0);
            Entity by = source.getEntity();
            d.put("by", by instanceof Player ? "player" : (by != null ? by.getName().getString() : ""));
            d.put("entity", "spraute:" + this.model());
            d.put("name", this.getName().getString());
            d.put("x", Math.floor(at.x));
            d.put("y", Math.floor(at.y));
            d.put("z", Math.floor(at.z));
            d.put("__entity", this);
            GameEvents.post("parthit", by instanceof Player byPlayer ? byPlayer : null, d);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.inPartHit && !Mc.level(this).isClientSide && !this.partDefs().isEmpty() && !this.isInvulnerableTo(source)) {
            this.report("body", source, amount, this.getBoundingBox().getCenter());
        }

        return super.hurt(source, amount);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MODEL, "");
        this.entityData.define(PARTS_OFF, "");
        this.entityData.define(BODY_SCALE, 1.0F);
    }

    public String model() {
        return this.entityData.get(MODEL);
    }

    public void setModel(String key) {
        if (key == null) {
            key = "";
        }

        if (!key.equals(this.model())) {
            this.entityData.set(MODEL, key);
        }

        this.applyMovement();
        this.refreshDimensions();
    }

    private float lookScale() {
        EntityLibrary.Look l = this.look();
        float s = l != null ? l.scale() : 1.0F;
        return (s > 0.0F ? s : 1.0F) * this.bodyScale();
    }

    public float bodyScale() {
        return this.entityData.get(BODY_SCALE);
    }

    public void setBodyScale(float scale) {
        this.entityData.set(BODY_SCALE, Math.max(0.05F, scale));
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions d = super.getDimensions(pose);
        EntityLibrary.Look l = this.look();
        if (l != null && l.def() != null && (l.def().width > 0.0F || l.def().height > 0.0F)) {
            d = EntityDimensions.scalable(l.def().width > 0.0F ? l.def().width : d.width, l.def().height > 0.0F ? l.def().height : d.height);
        }

        float s = this.lookScale();
        return s != 1.0F ? d.scale(s) : d;
    }

    private boolean sizeChanged() {
        EntityLibrary.Look l = this.look();
        float s = this.lookScale();
        float w = l != null && l.def() != null ? l.def().width : -1.0F;
        float h = l != null && l.def() != null ? l.def().height : -1.0F;
        if (s == this.sizedScale && w == this.sizedWidth && h == this.sizedHeight) {
            return false;
        } else {
            this.sizedScale = s;
            this.sizedWidth = w;
            this.sizedHeight = h;
            return true;
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MODEL.equals(key)) {
            this.refreshDimensions();
        }
    }

    public EntityLibrary.Look look() {
        return EntityLibrary.look(this.model());
    }

    public ModelLibrary.Model modelInfo() {
        EntityLibrary.Look l = this.look();
        return l != null ? l.model() : null;
    }

    private Optional<Animation> movement(EntityLibrary.Look l, String setting, String fallbackName) {
        if (l.def() != null && setting != null && !setting.isBlank()) {
            List<Animation> found = new ArrayList<>();

            for (String one : setting.split("[;,]")) {
                l.animation(one.trim()).ifPresent(found::add);
            }

            if (!found.isEmpty()) {
                return Optional.of(found.get(this.random.nextInt(found.size())));
            }
        }

        return l.animation(fallbackName);
    }

    private void applyMovement() {
        EntityLibrary.Look l = this.look();
        if (l != null && !Mc.level(this).isClientSide && !this.appliedFor.equals(this.model())) {
            this.appliedFor = this.model();
            EntityLibrary.Def d = l.def();
            boolean mob = CustomMobs.isMob(this);
            if (d != null && d.speed > 0.0 && !mob) {
                try {
                    AttributeInstance attr = this.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (attr != null) {
                        attr.setBaseValue(0.25 * d.speed);
                    }
                } catch (Throwable var7) {
                }
            }

            if (d != null && d.health > 0.0F && !mob) {
                try {
                    AttributeInstance hp = this.getAttribute(Attributes.MAX_HEALTH);
                    if (hp != null) {
                        hp.setBaseValue(d.health);
                    }

                    this.setHealth(d.health);
                } catch (Throwable var6) {
                }
            }

            if (d != null && !d.hitbox.isBlank()) {
                try {
                    Hitboxes.set(this, Hitboxes.parse(0, List.of(d.hitbox)));
                } catch (RuntimeException var5) {
                }
            }
        }
    }

    @Override
    protected String movementKey() {
        return this.model();
    }

    @Override
    protected ConfiguredAnimation turnAnimation(boolean left) {
        Animation a = Anims.turn(this, left);
        return a != null ? a.configure() : null;
    }

    @Override
    protected ConfiguredAnimation blinkAnimation() {
        EntityLibrary.Look l = this.look();
        if (l == null) {
            return null;
        } else {
            Optional<Animation> a = l.animation("blinking");
            if (a.isEmpty()) {
                a = l.animation("blink");
            }

            return a.map(Animation::configure).orElse(null);
        }
    }

    @Override
    protected ConfiguredAnimation movementAnimation(MovementActionType type) {
        EntityLibrary.Look l = this.look();
        if (l == null) {
            return null;
        } else {
            EntityLibrary.Def d = l.def();
            if (this.isSitting() && type == MovementActionType.IDLE) {
                Optional<Animation> a = l.animation("sit");
                if (a.isPresent()) {
                    return a.get().configure();
                }
            }

            Optional<Animation> a;
            switch (type) {
                case WALKING:
                    a = this.movement(l, d != null ? d.walk : "", "walk");
                    if (a.isPresent() && l.model() == null && (d == null || d.walk.isBlank())) {
                        return a.get().configure().speed(0.75F);
                    }
                    break;
                case FAST_WALKING:
                    a = l.animation("fast_walk");
                    if (a.isPresent() && (d == null || d.walk.isBlank())) {
                        return a.get().configure().speed(l.model() == null ? 0.75F : 1.0F);
                    }

                    a = this.movement(l, d != null ? d.walk : "", "walk");
                    if (a.isPresent()) {
                        return a.get().configure().speed(1.3F);
                    }
                    break;
                case RUNNING:
                    a = this.movement(l, d != null ? d.run : "", "run");
                    if (a.isEmpty()) {
                        a = this.movement(l, d != null ? d.walk : "", "walk");
                    }
                    break;
                default:
                    a = this.movement(l, d != null ? d.idle : "", "idle");
            }

            return a.map(Animation::configure).orElse(null);
        }
    }

    @Override
    public void tick() {
        if (this.sizeChanged()) {
            this.refreshDimensions();
        }

        super.tick();
        this.placeParts();
        if (!Mc.level(this).isClientSide && !this.partAttack.isEmpty()) {
            AttackBoxes.contact(this);
        }

        if (!Mc.level(this).isClientSide && !this.appliedFor.equals(this.model()) && this.tickCount % 20 == 0) {
            this.applyMovement();
        }

        if (!this.announced && !Mc.level(this).isClientSide) {
            this.announced = true;
            CustomMobs.born(this, this.bornHow);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("SprauteModel", this.model());
        tag.putFloat("SprauteScale", this.bodyScale());
        tag.putBoolean("SprauteWild", this.wild);
        tag.putBoolean("SprauteAnnounced", this.announced);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("SprauteModel")) {
            this.setModel(tag.getString("SprauteModel"));
        }

        if (tag.contains("SprauteScale")) {
            this.setBodyScale(tag.getFloat("SprauteScale"));
        }

        this.wild = tag.getBoolean("SprauteWild");
        this.announced = tag.getBoolean("SprauteAnnounced");
        if (CustomMobs.isMob(this)) {
            this.canDie = true;
            this.setVulnerable(true);
        }
    }
}
