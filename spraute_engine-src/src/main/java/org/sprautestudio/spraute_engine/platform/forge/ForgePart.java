package org.sprautestudio.spraute_engine.platform.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;
import org.sprautestudio.spraute_engine.runtime.models.BodyPart;
import org.sprautestudio.spraute_engine.runtime.models.SprauteModelEntity;

final class ForgePart extends PartEntity<SprauteModelEntity> implements BodyPart {
    private final int index;
    private EntityDimensions size = EntityDimensions.scalable(0.01F, 0.01F);
    private boolean active;

    ForgePart(SprauteModelEntity body, int index) {
        super(body);
        this.index = index;
        this.noPhysics = true;
        this.refreshDimensions();
    }

    @Override
    public SprauteModelEntity body() {
        return this.getParent();
    }

    @Override
    public int index() {
        return this.index;
    }

    @Override
    public boolean active() {
        return this.active;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public void resize(float w, float h) {
        if (!(Math.abs(this.size.width - w) < 1.0E-4) || !(Math.abs(this.size.height - h) < 1.0E-4)) {
            this.size = EntityDimensions.scalable(w, h);
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return this.size;
    }

    @Override
    public boolean isPickable() {
        return this.active && this.getParent().isPickable();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return this.active && !this.isInvulnerableTo(source) ? this.getParent().hurtPart(this, source, amount) : false;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        return this.getParent().interact(player, hand);
    }

    @Override
    public boolean is(Entity e) {
        return this == e || this.getParent() == e;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        throw new UnsupportedOperationException();
    }
}
