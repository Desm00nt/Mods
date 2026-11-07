package org.sprautestudio.spraute_engine.runtime;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class EntityRef {
    public final UUID uuid;
    public final String type;
    private Entity cache;

    public EntityRef(Entity e) {
        this.uuid = e.getUUID();
        this.type = e instanceof Player ? "player" : String.valueOf(Regs.ENTITY_TYPE.getKey(e.getType()));
        this.cache = e;
    }

    public EntityRef(UUID uuid, String type) {
        this.uuid = uuid;
        this.type = type;
    }

    public Entity get() {
        if (this.cache != null && !this.cache.isRemoved()) {
            return this.cache;
        } else {
            this.cache = null;
            MinecraftServer server = Platform.get().server();
            if (server == null) {
                return null;
            } else {
                for (ServerLevel level : server.getAllLevels()) {
                    Entity e = level.getEntity(this.uuid);
                    if (e != null) {
                        this.cache = e;
                        return e;
                    }
                }

                return null;
            }
        }
    }

    public Entity require() {
        Entity e = this.get();
        if (e == null) {
            throw new SprauteRuntimeException("Сущность " + this.type + " пропала (убита или далеко)");
        } else {
            return e;
        }
    }

    public boolean alive() {
        Entity e = this.get();
        return e != null && e.isAlive();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityRef r && r.uuid.equals(this.uuid);
    }

    @Override
    public int hashCode() {
        return this.uuid.hashCode();
    }

    @Override
    public String toString() {
        Entity e = this.get();
        return e != null ? e.getName().getString() : this.type;
    }
}
