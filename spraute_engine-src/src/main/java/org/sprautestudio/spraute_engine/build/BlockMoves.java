package org.sprautestudio.spraute_engine.build;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.sprautestudio.spraute_engine.cutscene.Ease;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.net.Packets.BlocksShake;
import org.sprautestudio.spraute_engine.net.Packets.BlocksWave;
import org.sprautestudio.spraute_engine.net.Packets.BuildFly;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntimeException;

public final class BlockMoves {
    private static final int MAX_BLOCKS = 20000;
    private static final long MAX_BOX = 2000000L;
    private static final int PER_PACKET = 30000;
    private static final double TOLD_WITHIN = 512.0;
    private static final int QUIET = 18;
    private static final List<BlockMoves.Job> JOBS = new ArrayList<>();
    private static final Map<String, BlockMoves.Job> HISTORY = new HashMap<>();

    private BlockMoves() {
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END) {
                tick();
            }
        });
        Hooks.SERVER_STOPPING.add(e -> {
            for (BlockMoves.Job job : JOBS) {
                land(job);
            }

            JOBS.clear();
            HISTORY.clear();
        });
    }

    public static BlockMoves.Job move(int script, ServerLevel level, BlockPos a, BlockPos b, int dx, int dy, int dz, int ticks, String ease, String id) {
        if (!id.isEmpty()) {
            stop(id);
        }

        int x0 = Math.min(a.getX(), b.getX());
        int y0 = Math.min(a.getY(), b.getY());
        int z0 = Math.min(a.getZ(), b.getZ());
        int x1 = Math.max(a.getX(), b.getX());
        int y1 = Math.max(a.getY(), b.getY());
        int z1 = Math.max(a.getZ(), b.getZ());
        long box = (long)(x1 - x0 + 1) * (y1 - y0 + 1) * (z1 - z0 + 1);
        if (box > 2000000L) {
            throw new SprauteRuntimeException("blocks move: слишком большая область, " + box + " клеток");
        } else {
            List<BlockMoves.Cell> cells = new ArrayList<>();

            for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
                BlockState state = level.getBlockState(p);
                if (!state.isAir()) {
                    if (cells.size() >= 20000) {
                        throw new SprauteRuntimeException("blocks move: больше 20000 блоков за раз");
                    }

                    BlockEntity be = level.getBlockEntity(p);
                    BlockPos from = p.immutable();
                    cells.add(new BlockMoves.Cell(from, from.offset(dx, dy, dz), state, be != null ? be.saveWithoutMetadata() : null));
                }
            }

            for (BlockMoves.Cell c : cells) {
                clear(level, c.from);
            }

            return begin(script, level, cells, ticks, ease, id, !id.isEmpty());
        }
    }

    public static BlockMoves.Job back(int script, String id, int ticks, String ease) {
        stop(id);
        BlockMoves.Job was = HISTORY.remove(id);
        if (was == null) {
            return null;
        } else {
            ServerLevel level = was.level;
            List<BlockMoves.Cell> cells = new ArrayList<>();

            for (BlockMoves.Cell c : was.cells) {
                BlockState now = level.getBlockState(c.to);
                if (!now.isAir()) {
                    BlockEntity be = level.getBlockEntity(c.to);
                    cells.add(new BlockMoves.Cell(c.to, c.from, now, be != null ? be.saveWithoutMetadata() : null));
                }
            }

            for (BlockMoves.Cell cx : cells) {
                clear(level, cx.from);
            }

            for (BlockMoves.Stood s : was.replaced) {
                put(level, s.pos, s.state, s.nbt);
            }

            return begin(script, level, cells, ticks, ease, id, false);
        }
    }

    private static BlockMoves.Job begin(int script, ServerLevel level, List<BlockMoves.Cell> cells, int ticks, String ease, String id, boolean keep) {
        BlockMoves.Job job = new BlockMoves.Job(id, script, level, cells, Math.max(0, ticks), keep);
        if (job.left == 0) {
            land(job);
            return job;
        } else {
            BlockPos at = cells.isEmpty() ? BlockPos.ZERO : cells.get(0).to;
            int kind = 100 + Math.max(0, Ease.names().indexOf(ease));

            for (int i = 0; i < cells.size(); i += 30000) {
                send(level, at, cells.subList(i, Math.min(cells.size(), i + 30000)), job.left, kind);
            }

            JOBS.add(job);
            return job;
        }
    }

    private static void send(ServerLevel level, BlockPos at, List<BlockMoves.Cell> cells, int ticks, int ease) {
        long[] pos = new long[cells.size()];
        int[] states = new int[cells.size()];
        float[] from = new float[cells.size() * 3];

        for (int i = 0; i < pos.length; i++) {
            BlockMoves.Cell c = cells.get(i);
            pos[i] = c.to.asLong();
            states[i] = Block.getId(c.state);
            from[i * 3] = c.from.getX() - c.to.getX();
            from[i * 3 + 1] = c.from.getY() - c.to.getY();
            from[i * 3 + 2] = c.from.getZ() - c.to.getZ();
        }

        tell(level, at, new BuildFly(ticks, ease, false, false, 0.0F, pos, states, from));
    }

    private static void tell(ServerLevel level, BlockPos at, Object msg) {
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(at) < 262144.0) {
                SprauteNetwork.sendTo(p, msg);
            }
        }
    }

    public static void shake(ServerLevel level, BlockPos from, BlockPos to, boolean round, float power, float seconds, boolean together) {
        tell(level, from, new BlocksShake(from.asLong(), to.asLong(), round, power, seconds, together));
    }

    public static void wave(ServerLevel level, double x, double y, double z, BlockQuake.Wave w) {
        tell(
            level,
            new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z)),
            new BlocksWave(x, y, z, (float)w.radius(), (float)w.height(), (float)w.step(), (float)w.rise(), (float)w.tilt())
        );
    }

    private static void tick() {
        forEachJob(job -> {
            if (--job.left <= 0) {
                land(job);
            }
        });
    }

    public static void stop(String id) {
        forEachJob(job -> {
            if (id.equals(job.id)) {
                land(job);
            }
        });
    }

    public static void storyStopped(int script) {
        forEachJob(job -> {
            if (job.script == script) {
                land(job);
            }
        });
    }

    private static void forEachJob(Consumer<BlockMoves.Job> action) {
        if (!JOBS.isEmpty()) {
            for (BlockMoves.Job job : new ArrayList<>(JOBS)) {
                action.accept(job);
            }

            Iterator<BlockMoves.Job> it = JOBS.iterator();

            while (it.hasNext()) {
                if (it.next().done) {
                    it.remove();
                }
            }
        }
    }

    private static void land(BlockMoves.Job job) {
        if (!job.done) {
            job.done = true;
            ServerLevel level = job.level;

            for (BlockMoves.Cell c : job.cells) {
                BlockState was = level.getBlockState(c.to);
                if (job.keep && !was.isAir()) {
                    BlockEntity be = level.getBlockEntity(c.to);
                    job.replaced.add(new BlockMoves.Stood(c.to, was, be != null ? be.saveWithoutMetadata() : null));
                }

                clear(level, c.to);
                put(level, c.to, c.state, c.nbt);
            }

            if (!job.cells.isEmpty()) {
                BlockMoves.Cell first = job.cells.get(0);
                level.playSound(null, first.to, first.state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 0.8F);
            }

            if (job.keep) {
                HISTORY.put(job.id, job);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("id", job.id);
            GameEvents.post("blocksmoved", null, data);
        }
    }

    private static void clear(ServerLevel level, BlockPos pos) {
        Clearable.tryClear(level.getBlockEntity(pos));
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
    }

    private static void put(ServerLevel level, BlockPos pos, BlockState state, CompoundTag nbt) {
        level.setBlock(pos, state, 18);
        if (nbt != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag tag = nbt.copy();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                be.load(tag);
                be.setChanged();
            }
        }
    }

    private record Cell(BlockPos from, BlockPos to, BlockState state, CompoundTag nbt) {
    }

    public static final class Job {
        final String id;
        final int script;
        final ServerLevel level;
        final List<BlockMoves.Cell> cells;
        final List<BlockMoves.Stood> replaced = new ArrayList<>();
        final boolean keep;
        int left;
        boolean done;

        Job(String id, int script, ServerLevel level, List<BlockMoves.Cell> cells, int ticks, boolean keep) {
            this.id = id;
            this.script = script;
            this.level = level;
            this.cells = cells;
            this.left = ticks;
            this.keep = keep;
        }

        public boolean finished() {
            return this.done;
        }
    }

    private record Stood(BlockPos pos, BlockState state, CompoundTag nbt) {
    }
}
