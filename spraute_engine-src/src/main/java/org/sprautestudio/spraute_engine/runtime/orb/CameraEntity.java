package org.sprautestudio.spraute_engine.runtime.orb;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.sprautestudio.spraute_engine.compat.Mc;

public class CameraEntity extends Entity {
    @Override
    public Packet getAddEntityPacket() {
        return Mc.addEntityPacket(this);
    }

    public CameraEntity(EntityType<? extends CameraEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
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
    public boolean isSpectator() {
        return true;
    }
}
