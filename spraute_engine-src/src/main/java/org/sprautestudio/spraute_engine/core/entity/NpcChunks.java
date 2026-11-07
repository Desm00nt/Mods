package org.sprautestudio.spraute_engine.core.entity;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.sprautestudio.spraute_engine.compat.Mc;

public final class NpcChunks {
    private static final int LIFE = 100;
    private static final TicketType<ChunkPos> TICKET = TicketType.create("spraute_npc", Comparator.comparingLong(ChunkPos::toLong), 100);
    private static final int AROUND_WALKER = 3;
    private static final int TO_WAKE = 2;
    public static final int RENEW_EVERY = 40;
    private static final int ALONG_MAX = 16;

    private NpcChunks() {
    }

    public static void around(ServerLevel level, ChunkPos at) {
        level.getChunkSource().addRegionTicket(TICKET, at, 3, at);
    }

    public static void wake(ServerLevel level, ChunkPos at) {
        level.getChunkSource().addRegionTicket(TICKET, at, 2, at);
    }

    public static void load(ServerLevel level, Vec3 at) {
        ChunkPos c = new ChunkPos(Mc.blockPos(at.x, at.y, at.z));
        level.getChunk(c.x, c.z);
        around(level, c);
    }

    public static void along(ServerLevel level, Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        int steps = Math.min(16, (int)Math.ceil(len / 16.0));
        long last = Long.MIN_VALUE;

        for (int i = 1; i <= steps; i++) {
            double t = (double)i / steps;
            ChunkPos c = new ChunkPos(Mth.floor(from.x + dx * t) >> 4, Mth.floor(from.z + dz * t) >> 4);
            if (c.toLong() != last) {
                last = c.toLong();
                level.getChunkSource().addRegionTicket(TICKET, c, 2, c);
            }
        }
    }
}
