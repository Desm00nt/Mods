package org.sprautestudio.spraute_engine.core.entity;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.anim.Animation;
import org.sprautestudio.spraute_engine.core.anim.AnimationSystem;
import org.sprautestudio.spraute_engine.core.anim.ConfiguredAnimation;
import org.sprautestudio.spraute_engine.core.anim.IAnimatedObject;
import org.sprautestudio.spraute_engine.core.anim.LoopMode;
import org.sprautestudio.spraute_engine.core.anim.MovementActionType;
import org.sprautestudio.spraute_engine.path.Walkers;
import org.sprautestudio.spraute_engine.runtime.Anims;
import org.sprautestudio.spraute_engine.runtime.NpcItemPickup;

public abstract class AnimatedEntity extends PathfinderMob implements IAnimatedObject {
    private static final EntityDataAccessor<String> ANIMS = SynchedEntityData.defineId(AnimatedEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> MOVES = SynchedEntityData.defineId(AnimatedEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(AnimatedEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(AnimatedEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> LOOK = SynchedEntityData.defineId(AnimatedEntity.class, EntityDataSerializers.STRING);
    private static final float SPEED_1 = 0.12951F;
    private static final float FAST_WALK = 0.20721601F;
    private static final float RUN = 0.25902F;
    private static final float STILL = 0.03F;
    private static final int BLINK_MIN = 50;
    private static final int BLINK_MAX = 110;
    private static final float LOOK_SETTLED = 10.0F;
    private static final float TURN_MIN = 2.5F;
    private static final float LOOK_DEGREES_PER_TICK = 10.0F;
    private static final float BODY_TURN_START = 50.0F;
    private static final float BODY_TURN_STOP = 15.0F;
    private static final float HEAD_MAX_YAW = 75.0F;
    private static final float HEAD_MAX_PITCH = 60.0F;
    private static final int TURN_HOLD = 2;
    private boolean bodyFollowing;
    private int turningShown;
    private int turnFor;
    private boolean turnLeft;
    private boolean clientBodyTurning;
    private float clientBodyYaw;
    private float clientBodyYawO;
    private boolean holdsChunk;
    private long heldChunk = Long.MIN_VALUE;
    private final NpcItemPickup itemPickup = new NpcItemPickup(this);
    private final AnimationSystem animations = new AnimationSystem();
    private final Map<MovementActionType, ConfiguredAnimation> overrides = new EnumMap<>(MovementActionType.class);
    public boolean canDie;
    private boolean vulnerable;
    private MovementActionType shownType;
    private String shownMoves;
    private String shownKey;
    private boolean shownSitting;
    private Supplier<Vec3> lookTarget;
    private boolean lookInstant;
    private boolean bodyLocked;
    private boolean headLocked;
    private boolean noTurn;
    private float lockedBody;
    private float lockedHead;
    public boolean blinks = true;
    private int blinkIn = 50;
    private static final double LOOK_SYNC_STEP_SQR = 0.04000000000000001;
    private Vec3 lookSynced;
    private String lookRead;
    private Vec3 lookParsed;
    private String textureRead;
    private ResourceLocation textureParsed;
    private float gaitRate = 1.0F;
    private float smoothSpeed;
    private static final int REST_CHECK_EVERY = 5;
    private Vec3 restsAt;

    protected AnimatedEntity(EntityType<? extends AnimatedEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.lookControl = new LookControl(this) {
            @Override
            public void tick() {
                if (AnimatedEntity.this.lookTarget == null) {
                    super.tick();
                }
            }
        };
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIMS, "");
        this.entityData.define(MOVES, "");
        this.entityData.define(TEXTURE, "");
        this.entityData.define(SITTING, false);
        this.entityData.define(LOOK, "");
    }

    @Override
    public AnimationSystem getAnimationSystem() {
        return this.animations;
    }

    public void startAnimation(String layer, ConfiguredAnimation ca) {
        this.animations.startAnimationAt(layer, ca);
    }

    public void setMaxHealth(float hp) {
        AttributeInstance a = this.getAttribute(Attributes.MAX_HEALTH);
        if (a != null) {
            a.setBaseValue(hp);
        }

        this.setHealth(hp);
    }

    public void setVulnerable(boolean v) {
        this.vulnerable = v;
    }

    public void setLookTarget(Supplier<Vec3> target, boolean instant) {
        if (target == null && this.lookTarget != null) {
            this.yBodyRot = this.yHeadRot;
            this.setYRot(this.yHeadRot);
            this.entityData.set(LOOK, "");
            this.lookSynced = null;
        }

        this.lookTarget = target;
        this.lookInstant = instant;
        if (target != null && !Mc.level(this).isClientSide) {
            try {
                Vec3 at = target.get();
                if (at != null) {
                    this.syncLookPoint(at);
                }
            } catch (RuntimeException var4) {
            }
        }
    }

    private void syncLookPoint(Vec3 at) {
        if (this.lookSynced == null || !(this.lookSynced.distanceToSqr(at) < 0.04000000000000001)) {
            this.lookSynced = at;
            this.entityData.set(LOOK, at.x + "," + at.y + "," + at.z);
        }
    }

    public Vec3 lookPoint() {
        String raw = this.entityData.get(LOOK);
        if (raw == this.lookRead) {
            return this.lookParsed;
        } else {
            this.lookRead = raw;
            this.lookParsed = null;
            String[] p = raw.split(",");
            if (p.length != 3) {
                return null;
            } else {
                try {
                    this.lookParsed = new Vec3(Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]));
                } catch (NumberFormatException var4) {
                    this.lookParsed = null;
                }

                return this.lookParsed;
            }
        }
    }

    public boolean lookSettled() {
        Vec3 at = this.lookTarget != null ? this.lookTarget.get() : null;
        if (at == null) {
            return true;
        } else {
            Vec3 d = at.subtract(this.getEyePosition());
            float want = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
            return Math.abs(Mth.wrapDegrees(want - this.yHeadRot)) <= 10.0F;
        }
    }

    public void setBodyLocked(boolean locked) {
        this.bodyLocked = locked;
        this.lockedBody = this.yBodyRot;
    }

    public void setHeadLocked(boolean locked) {
        this.headLocked = locked;
        this.lockedHead = this.yHeadRot;
    }

    public void setHoldsChunk(boolean v) {
        this.holdsChunk = v;
        if (!v) {
            this.releaseChunk();
        }
    }

    public NpcItemPickup itemPickup() {
        return this.itemPickup;
    }

    private void releaseChunk() {
        if (this.heldChunk != Long.MIN_VALUE && Mc.level(this) instanceof ServerLevel l) {
            ChunkPos at = new ChunkPos(this.heldChunk);
            l.setChunkForced(at.x, at.z, false);
        }

        this.heldChunk = Long.MIN_VALUE;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason != RemovalReason.UNLOADED_TO_CHUNK && reason != RemovalReason.UNLOADED_WITH_PLAYER) {
            this.releaseChunk();
        }

        super.remove(reason);
    }

    public void setNoTurn(boolean v) {
        this.noTurn = v;
    }

    public boolean noTurn() {
        return this.noTurn;
    }

    protected ConfiguredAnimation blinkAnimation() {
        return null;
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setSitting(boolean sit) {
        this.entityData.set(SITTING, sit);
    }

    public void setTextureOverride(ResourceLocation texture) {
        this.entityData.set(TEXTURE, texture == null ? "" : texture.toString());
    }

    public ResourceLocation textureOverride() {
        String t = this.entityData.get(TEXTURE);
        if (t != this.textureRead) {
            this.textureRead = t;
            this.textureParsed = t.isEmpty() ? null : ResourceLocation.tryParse(t);
        }

        return this.textureParsed;
    }

    public void setAnimationOverride(MovementActionType type, ConfiguredAnimation ca) {
        this.overrides.put(type, ca);
        this.syncOverrides();
    }

    public void clearAnimationOverrides() {
        this.overrides.clear();
        this.syncOverrides();
    }

    private void syncOverrides() {
        StringBuilder sb = new StringBuilder();
        this.overrides.forEach((k, v) -> sb.append(k.name()).append('=').append(v.animation().id()).append(';'));
        this.entityData.set(MOVES, sb.toString());
    }

    protected ConfiguredAnimation movementAnimation(MovementActionType type) {
        return null;
    }

    private ConfiguredAnimation animationFor(MovementActionType type) {
        for (String row : this.entityData.get(MOVES).split(";")) {
            int eq = row.indexOf(61);
            if (eq > 0 && row.substring(0, eq).equals(type.name())) {
                Optional<Animation> a = Anims.resolve(row.substring(eq + 1));
                if (a.isPresent()) {
                    return a.get().configure();
                }
            }
        }

        return this.movementAnimation(type);
    }

    public float getWalkingSpeed() {
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        return (float)Math.sqrt(dx * dx + dz * dz);
    }

    protected String movementKey() {
        return "";
    }

    protected ConfiguredAnimation turnAnimation(boolean left) {
        return null;
    }

    private static float gaitPace(MovementActionType type) {
        return switch (type) {
            case WALKING -> 0.12951F;
            case FAST_WALKING -> 0.2266425F;
            case RUNNING -> 0.310824F;
            default -> 0.0F;
        };
    }

    public float renderedBodyYaw(float partial) {
        return this.clientBodyTurning ? Mth.rotLerp(partial, this.clientBodyYawO, this.clientBodyYaw) : Mth.rotLerp(partial, this.yBodyRotO, this.yBodyRot);
    }

    public float renderedHeadYaw(float partial) {
        return Mth.rotLerp(partial, this.yHeadRotO, this.yHeadRot);
    }

    private void updateClientBodyYaw(Vec3 target, boolean idle) {
        if (target != null && idle) {
            if (!this.clientBodyTurning) {
                this.clientBodyYaw = this.yBodyRot;
                this.clientBodyYawO = this.clientBodyYaw;
                this.clientBodyTurning = true;
            } else {
                this.clientBodyYawO = this.clientBodyYaw;
            }

            this.clientBodyYaw = this.clientBodyYaw + Mth.clamp(Mth.wrapDegrees(this.getYRot() - this.clientBodyYaw), -10.0F, 10.0F);
        } else {
            if (this.clientBodyTurning) {
                this.clientBodyYawO = this.clientBodyYaw;
                this.clientBodyYaw = this.clientBodyYaw + Mth.clamp(Mth.wrapDegrees(this.yBodyRot - this.clientBodyYaw), -10.0F, 10.0F);
                if (Math.abs(Mth.wrapDegrees(this.yBodyRot - this.clientBodyYaw)) < 1.0F) {
                    this.clientBodyTurning = false;
                }
            }
        }
    }

    private void pickMovement() {
        float v = this.smoothSpeed = this.smoothSpeed + (this.getWalkingSpeed() - this.smoothSpeed) * 0.4F;
        MovementActionType type = v < 0.03F
            ? MovementActionType.IDLE
            : (v < 0.20721601F ? MovementActionType.WALKING : (v < 0.25902F ? MovementActionType.FAST_WALKING : MovementActionType.RUNNING));
        String moves = this.entityData.get(MOVES);
        String key = this.movementKey();
        boolean sitting = this.isSitting();
        if (type != this.shownType || sitting != this.shownSitting || !moves.equals(this.shownMoves) || !key.equals(this.shownKey)) {
            this.shownType = type;
            this.shownSitting = sitting;
            this.shownMoves = moves;
            this.shownKey = key;
            ConfiguredAnimation ca = this.animationFor(type);
            if (ca != null) {
                this.startAnimation("movement", ca.loopMode(LoopMode.LOOP).transitionTime(0.2F).speed(ca.speed() * (0.95F + this.random.nextFloat() * 0.1F)));
            } else {
                this.animations.stopAnimation("movement", 0.2F);
            }
        }

        float pace = gaitPace(type);
        float wanted = pace > 0.0F ? Mth.clamp(v / pace, 0.55F, 1.7F) : 1.0F;
        this.gaitRate = this.gaitRate + (wanted - this.gaitRate) * 0.25F;
        this.animations.setRate("movement", this.gaitRate);
        Vec3 target = this.lookPoint();
        this.updateClientBodyYaw(target, type == MovementActionType.IDLE);
        float delta = this.clientBodyTurning ? Mth.wrapDegrees(this.clientBodyYaw - this.clientBodyYawO) : Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO);
        if (type == MovementActionType.IDLE && Math.abs(delta) > 2.5F) {
            this.turnLeft = delta < 0.0F;
            this.turnFor = 2;
        } else if (this.turnFor > 0) {
            this.turnFor--;
        }

        int turning = type == MovementActionType.IDLE && this.turnFor > 0 ? (this.turnLeft ? 1 : 2) : 0;
        if (turning != this.turningShown) {
            this.turningShown = turning;
            ConfiguredAnimation turn = turning != 0 ? this.turnAnimation(turning == 1) : null;
            if (turn != null) {
                this.startAnimation("turn", turn.loopMode(LoopMode.LOOP).transitionTime(0.2F).speed(turn.speed() * 0.75F));
            } else {
                this.animations.stopAnimation("turn", 0.2F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (Mc.level(this).isClientSide) {
            this.pickMovement();
        } else {
            if (this.blinks && --this.blinkIn <= 0) {
                this.blinkIn = 50 + this.random.nextInt(61);
                ConfiguredAnimation blink = this.blinkAnimation();
                if (blink != null) {
                    this.startAnimation("EYES", blink.transitionTime(0.0F).loopMode(LoopMode.ONCE));
                }
            }

            this.itemPickup.tick();
            if (this.animations.takeDirty()) {
                this.entityData.set(ANIMS, this.animations.encode());
            }

            if (this.lookTarget != null) {
                Vec3 at = null;

                try {
                    at = this.lookTarget.get();
                } catch (RuntimeException var8) {
                }

                if (at != null) {
                    Vec3 d = at.subtract(this.getEyePosition());
                    float yaw = (float)Math.toDegrees(Math.atan2(-d.x, d.z));
                    float step = this.lookInstant ? 360.0F : 10.0F;
                    boolean walking = this.getNavigation().isInProgress() || Walkers.speedOf(this) >= 0.0;
                    if (!this.bodyLocked && !walking) {
                        float aside = Mth.wrapDegrees(yaw - this.getYRot());
                        if (this.lookInstant || Math.abs(aside) > 50.0F) {
                            this.bodyFollowing = true;
                        } else if (Math.abs(aside) < 15.0F) {
                            this.bodyFollowing = false;
                        }

                        float next = this.getYRot() + (this.bodyFollowing ? Mth.clamp(aside, -step, step) : 0.0F);
                        this.setYRot(next);
                        this.yBodyRot = next;
                    } else {
                        this.bodyFollowing = false;
                    }

                    if (!this.headLocked) {
                        float headTarget = this.yBodyRot + Mth.clamp(Mth.wrapDegrees(yaw - this.yBodyRot), -75.0F, 75.0F);
                        this.yHeadRot = this.yHeadRot + Mth.clamp(Mth.wrapDegrees(headTarget - this.yHeadRot), -step, step);
                        float pitch = Mth.clamp((float)(-Math.toDegrees(Math.atan2(d.y, d.horizontalDistance()))), -60.0F, 60.0F);
                        this.setXRot(this.getXRot() + Mth.clamp(pitch - this.getXRot(), -step, step));
                    }

                    if (this.tickCount % 5 == 0) {
                        this.syncLookPoint(at);
                    }
                }
            }

            if (this.holdsChunk && Mc.level(this) instanceof ServerLevel l) {
                boolean moved = this.chunkPosition().toLong() != this.heldChunk;
                if (moved) {
                    this.releaseChunk();
                    this.heldChunk = this.chunkPosition().toLong();
                    l.setChunkForced(this.chunkPosition().x, this.chunkPosition().z, true);
                }

                if (moved || this.tickCount % 40 == 0) {
                    NpcChunks.around(l, this.chunkPosition());
                }
            }

            if (this.bodyLocked) {
                this.yBodyRot = this.yBodyRotO = this.lockedBody;
            }

            if (this.headLocked) {
                this.yHeadRot = this.yHeadRotO = this.lockedHead;
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (ANIMS.equals(key) && Mc.level(this).isClientSide) {
            this.animations.decode(this.entityData.get(ANIMS));
        }
    }

    private boolean atRest(Vec3 input) {
        if (!Mc.level(this).isClientSide && (this.tickCount + this.getId()) % 5 != 0) {
            Vec3 v = this.getDeltaMovement();
            return input.lengthSqr() == 0.0
                && v.y <= 0.0
                && v.x * v.x + v.z * v.z < 1.0E-6
                && this.position().equals(this.restsAt)
                && Mc.onGround(this)
                && !this.isInWater()
                && !this.isInLava()
                && this.getNavigation().isDone()
                && Walkers.speedOf(this) < 0.0;
        } else {
            return false;
        }
    }

    @Override
    public void travel(Vec3 input) {
        if (!this.atRest(input)) {
            super.travel(input);
            this.restsAt = this.position();
        }
    }

    @Override
    protected void pushEntities() {
        if (Mc.level(this).isClientSide
            || (this.tickCount + this.getId()) % 4 == 0
            || Mc.level(this).hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(), 32.0)) {
            super.pushEntities();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return !this.vulnerable && !Mc.bypassesInvulnerability(source) ? false : super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.canDie && !Mc.bypassesInvulnerability(source)) {
            this.setHealth(1.0F);
        } else {
            super.die(source);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SprauteVulnerable", this.vulnerable);
        tag.putBoolean("SprauteCanDie", this.canDie);
        tag.putBoolean("SprauteSitting", this.isSitting());
        tag.putString("SprauteTexture", this.entityData.get(TEXTURE));
        tag.putString("SprauteAnims", this.animations.encodeSaved());
        tag.putBoolean("SprauteBodyLock", this.bodyLocked);
        tag.putBoolean("SprauteHeadLock", this.headLocked);
        tag.putBoolean("SprauteNoTurn", this.noTurn);
        tag.putBoolean("SprauteBlinks", this.blinks);
        tag.putString("SprauteGaits", this.entityData.get(MOVES));
        tag.putBoolean("SprauteChunk", this.holdsChunk);
        CompoundTag pickup = new CompoundTag();
        this.itemPickup.save(pickup);
        tag.put("SprauteItemPickup", pickup);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.vulnerable = tag.getBoolean("SprauteVulnerable");
        this.canDie = tag.getBoolean("SprauteCanDie");
        this.setSitting(tag.getBoolean("SprauteSitting"));
        this.entityData.set(TEXTURE, tag.getString("SprauteTexture"));
        this.animations.restore(tag.getString("SprauteAnims"));
        this.bodyLocked = tag.getBoolean("SprauteBodyLock");
        this.headLocked = tag.getBoolean("SprauteHeadLock");
        this.noTurn = tag.getBoolean("SprauteNoTurn");
        if (tag.contains("SprauteBlinks")) {
            this.blinks = tag.getBoolean("SprauteBlinks");
        }

        this.entityData.set(MOVES, tag.getString("SprauteGaits"));
        this.holdsChunk = tag.getBoolean("SprauteChunk");
        if (tag.contains("SprauteItemPickup", 10)) {
            this.itemPickup.load(tag.getCompound("SprauteItemPickup"));
        }

        this.lockedBody = this.yBodyRot;
        this.lockedHead = this.yHeadRot;
    }
}
