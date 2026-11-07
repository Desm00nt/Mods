package org.sprautestudio.spraute_engine.core.script;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.core.entity.AnimatedEntity;
import org.sprautestudio.spraute_engine.core.world.Location;
import org.sprautestudio.spraute_engine.platform.Platform;

public final class Seats {
    private static final String TAG = "SpraputeSeat";

    private Seats() {
    }

    public static void sit(Entity e, Location at) {
        stand(e);
        if (Mc.level(e) instanceof ServerLevel level) {
            ArmorStand seat = EntityType.ARMOR_STAND.create(level);
            if (seat != null) {
                double x = at != null ? at.x : e.getX();
                double y = at != null ? at.y : e.getY();
                double z = at != null ? at.z : e.getZ();
                seat.moveTo(x, y - 0.5, z, at != null ? at.yaw : e.getYRot(), 0.0F);
                seat.setInvisible(true);
                seat.setNoGravity(true);
                seat.setInvulnerable(true);
                Platform.get().persistentData(seat).putBoolean("SpraputeSeat", true);
                level.addFreshEntity(seat);
                e.startRiding(seat, true);
                if (e instanceof AnimatedEntity ae) {
                    ae.setSitting(true);
                    if (at != null) {
                        ae.setBodyLocked(true);
                    }
                }
            }
        }
    }

    public static void stand(Entity e) {
        Entity seat = e.getVehicle();
        if (seat != null && Platform.get().persistentData(seat).getBoolean("SpraputeSeat")) {
            e.stopRiding();
            seat.discard();
        }

        if (e instanceof AnimatedEntity ae) {
            ae.setSitting(false);
            ae.setBodyLocked(false);
        }
    }
}
