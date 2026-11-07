package org.sprautestudio.spraute_engine.runtime.orb;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.system.Coins;
import org.sprautestudio.spraute_engine.runtime.GameEvents;

public class SprauteOrb extends Entity {
    private static final EntityDataAccessor<String> KIND = SynchedEntityData.defineId(SprauteOrb.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> LOOK = SynchedEntityData.defineId(SprauteOrb.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SprauteOrb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SIZE = SynchedEntityData.defineId(SprauteOrb.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(SprauteOrb.class, EntityDataSerializers.INT);
    public int value = 1;
    public int coins;
    public int xp;
    public double radius = 6.0;
    public boolean pickup = true;
    public boolean floating;
    public int age;

    @Override
    public Packet getAddEntityPacket() {
        return Mc.addEntityPacket(this);
    }

    public SprauteOrb(EntityType<? extends SprauteOrb> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(KIND, "orb");
        this.entityData.define(LOOK, "");
        this.entityData.define(COLOR, -1);
        this.entityData.define(SIZE, 0.25F);
        this.entityData.define(LIFE, 1200);
    }

    public String kind() {
        return this.entityData.get(KIND);
    }

    public void setKind(String v) {
        this.entityData.set(KIND, v);
    }

    public String look() {
        return this.entityData.get(LOOK);
    }

    public void setLook(String v) {
        this.entityData.set(LOOK, v);
    }

    public int color() {
        return this.entityData.get(COLOR);
    }

    public void setColor(int v) {
        this.entityData.set(COLOR, v);
    }

    public float size() {
        return this.entityData.get(SIZE);
    }

    public void setSize(float v) {
        this.entityData.set(SIZE, v);
    }

    public int life() {
        return this.entityData.get(LIFE);
    }

    public void setLife(int v) {
        this.entityData.set(LIFE, v);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        Vec3 v = this.getDeltaMovement();
        if (!this.floating && !this.isNoGravity()) {
            v = v.add(0.0, -0.02, 0.0);
        }

        if (this.isInWater()) {
            v = new Vec3(v.x * 0.9, v.y + 0.01, v.z * 0.9);
        }

        if (!Mc.level(this).isClientSide) {
            int life = this.life();
            if (life > 0 && this.age > life) {
                if (this.pickup) {
                    GameEvents.orb("orbexpire", null, this);
                }

                this.discard();
                return;
            }

            if (this.pickup && this.age > 10) {
                Player p = Mc.level(this).getNearestPlayer(this, this.radius);
                if (p != null && !p.isSpectator()) {
                    Vec3 to = new Vec3(p.getX() - this.getX(), p.getY() + p.getEyeHeight() / 2.0F - this.getY(), p.getZ() - this.getZ());
                    double d = to.length();
                    if (d < 0.7) {
                        this.collect(p);
                        return;
                    }

                    double pull = 1.0 - d / this.radius;
                    v = v.add(to.normalize().scale(pull * pull * 0.12));
                }
            }
        }

        this.setDeltaMovement(v);
        this.move(MoverType.SELF, this.getDeltaMovement());
        float drag = Mc.onGround(this) ? 0.7F : (this.floating ? 0.9F : 0.98F);
        this.setDeltaMovement(this.getDeltaMovement().multiply(drag, this.floating ? 0.9 : 0.98, drag));
        if (Mc.onGround(this)) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0, -0.5, 1.0));
        }
    }

    private void collect(Player p) {
        if (this.coins > 0) {
            try {
                if (p instanceof ServerPlayer owner) {
                    Coins.add(owner, this.coins);
                }
            } catch (Throwable var3) {
            }
        }

        if (this.xp > 0) {
            p.giveExperiencePoints(this.xp);
        }

        Mc.level(this)
            .playSound(
                null,
                this.getX(),
                this.getY(),
                this.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.1F,
                0.5F * ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.8F)
            );
        if (p instanceof ServerPlayer sp) {
            GameEvents.orb("orb", sp, this);
        }

        this.discard();
    }

    public Map<String, Object> eventData() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("id", this.kind());
        d.put("amount", (double)this.value);
        d.put("count", (double)this.coins);
        d.put("level", (double)this.xp);
        d.put("x", Math.floor(this.getX()));
        d.put("y", Math.floor(this.getY()));
        d.put("z", Math.floor(this.getZ()));
        return d;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 4096.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        this.setKind(t.getString("kind"));
        this.setLook(t.getString("look"));
        this.setColor(t.getInt("color"));
        this.setSize(t.getFloat("size"));
        this.setLife(t.getInt("life"));
        this.value = t.getInt("value");
        this.coins = t.getInt("coins");
        this.xp = t.getInt("xp");
        this.radius = t.getDouble("radius");
        this.pickup = t.getBoolean("pickup");
        this.floating = t.getBoolean("floating");
        this.age = t.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putString("kind", this.kind());
        t.putString("look", this.look());
        t.putInt("color", this.color());
        t.putFloat("size", this.size());
        t.putInt("life", this.life());
        t.putInt("value", this.value);
        t.putInt("coins", this.coins);
        t.putInt("xp", this.xp);
        t.putDouble("radius", this.radius);
        t.putBoolean("pickup", this.pickup);
        t.putBoolean("floating", this.floating);
        t.putInt("age", this.age);
    }
}
