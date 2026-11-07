package org.sprautestudio.spraute_engine.runtime.models;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.sprautestudio.spraute_engine.net.Packets;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.platform.ClientHooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;

public final class PartPoseSync {
    private static int tick;

    public static void registerHooks() {
        ClientHooks.CLIENT_TICK.add(PartPoseSync::onTick);
    }

    private PartPoseSync() {
    }

    public static void onTick(ClientHooks.ClientTick e) {
        if (e.phase == TickPhase.END && ++tick % 2 == 0) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null && mc.player != null && mc.getConnection() != null) {
                long now = System.currentTimeMillis();

                for (Entity en : mc.level.entitiesForRendering()) {
                    if (en instanceof SprauteModelEntity me && !(en.distanceToSqr(mc.player) > 4096.0) && !me.bonePoses.isEmpty()) {
                        List<String> bones = new ArrayList<>();
                        List<float[]> rows = new ArrayList<>();
                        me.bonePoses.forEach((name, p) -> {
                            if (now - p.at() <= 500L) {
                                bones.add(name);
                                rows.add(new float[]{p.pos().x, p.pos().y, p.pos().z, p.rot().x, p.rot().y, p.rot().z, p.rot().w});
                            }
                        });
                        if (!bones.isEmpty()) {
                            float[] data = new float[bones.size() * 7];

                            for (int i = 0; i < rows.size(); i++) {
                                System.arraycopy(rows.get(i), 0, data, i * 7, 7);
                            }

                            SprauteNetwork.sendToServer(new Packets.PartPoses(en.getId(), bones, data));
                        }
                    }
                }
            }
        }
    }
}
