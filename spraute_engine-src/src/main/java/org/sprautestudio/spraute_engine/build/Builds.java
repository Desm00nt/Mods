package org.sprautestudio.spraute_engine.build;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.net.SprauteNetwork;
import org.sprautestudio.spraute_engine.net.Packets.BuildFly;
import org.sprautestudio.spraute_engine.platform.Hooks;
import org.sprautestudio.spraute_engine.platform.Platform;
import org.sprautestudio.spraute_engine.platform.TickPhase;
import org.sprautestudio.spraute_engine.runtime.GameEvents;
import org.sprautestudio.spraute_engine.runtime.SprauteRuntime;

public final class Builds {
    private static final List<Builds.Job> JOBS = new ArrayList<>();
    private static final Map<String, List<Builds.Saved>> HISTORY = new HashMap<>();
    private static long seeds = 1L;
    private static final int PER_PACKET = 30000;

    private Builds() {
    }

    public static void registerHooks() {
        Hooks.SERVER_TICK.add(e -> {
            if (e.phase == TickPhase.END) {
                tick();
            }
        });
        Hooks.SERVER_STOPPING.add(e -> {
            finishAll();
            HISTORY.clear();
        });
    }

    public static StructureDef load(String project, String name, MinecraftServer server) {
        String own = SprauteRuntime.get().gui(project, "build:" + name);
        if (own != null) {
            return StructureDef.fromJson(own);
        } else if (name.contains(":")) {
            ResourceLocation id = ResourceLocation.tryParse(name);
            if (id == null) {
                return null;
            } else {
                Optional<StructureTemplate> template = server.getStructureManager().get(id);
                return template.<StructureDef>map(t -> StructureImport.template(t.save(new CompoundTag())).trimmed()).orElse(null);
            }
        } else {
            for (Path file : schematicFiles()) {
                String base = file.getFileName().toString();
                if (base.substring(0, base.lastIndexOf(46)).equalsIgnoreCase(name)) {
                    try {
                        return StructureImport.read(file);
                    } catch (RuntimeException | IOException var8) {
                        SprauteEngine.LOGGER.warn("Spraute: cannot read {}", file, var8);
                    }
                }
            }

            return null;
        }
    }

    public static List<Path> schematicFiles() {
        List<Path> out = new ArrayList<>();
        Path game = Platform.get().gameDir();

        for (Path dir : List.of(game.resolve("schematics"), game.resolve("config/worldedit/schematics"), game.resolve("worldedit/schematics"))) {
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    files.filter(f -> StructureImport.canRead(f.getFileName().toString())).sorted().forEach(out::add);
                } catch (IOException var9) {
                    SprauteEngine.LOGGER.warn("Spraute: cannot list {}", dir, var9);
                }
            }
        }

        return out;
    }

    public static Builds.Job start(
        int script,
        StructureDef def,
        String name,
        BlockPos at,
        ServerLevel level,
        String styleName,
        double seconds,
        double flightSeconds,
        int rotate,
        String mirror,
        String air,
        String id,
        float viewerYaw
    ) {
        BuildStyles.Style style = BuildStyles.get(styleName);
        if (style == null) {
            style = BuildStyles.get("up");
        }

        if (air != null && !air.isBlank()) {
            def.air = air.trim();
        }

        List<BuildLayout.Cell> cells = BuildLayout.of(def, style.name(), at, rotate, mirror, viewerYaw, level, seeds++);
        double time = seconds >= 0.0 ? seconds : Math.max(2.0, Math.min(30.0, cells.size() / 120.0));
        int flight = (int)Math.round((flightSeconds >= 0.0 ? flightSeconds : style.flight()) * 20.0);
        Builds.Job job = new Builds.Job(id, name, script, level, cells, style, flight, (int)Math.round(time * 20.0), false, at);
        if (id != null && !id.isEmpty()) {
            stop(id);
            HISTORY.put(id, job.saved);
        }

        JOBS.add(job);
        return job;
    }

    public static Builds.Job undo(int script, String id, ServerLevel level, String styleName, double seconds, float viewerYaw) {
        stop(id);
        List<Builds.Saved> saved = HISTORY.remove(id);
        if (saved != null && !saved.isEmpty()) {
            BuildStyles.Style style = BuildStyles.get(styleName);
            if (style == null) {
                style = BuildStyles.get("scatter");
            }

            double cx = 0.0;
            double cz = 0.0;
            int floor = Integer.MAX_VALUE;
            int top = Integer.MIN_VALUE;

            for (Builds.Saved s : saved) {
                cx += s.pos.getX();
                cz += s.pos.getZ();
                floor = Math.min(floor, s.pos.getY());
                top = Math.max(top, s.pos.getY());
            }

            cx /= saved.size();
            cz /= saved.size();
            double yaw = Math.toRadians(viewerYaw);
            double fx = -Math.sin(yaw);
            double fz = Math.cos(yaw);
            int awayX = Math.abs(fx) >= Math.abs(fz) ? (fx > 0.0 ? 1 : -1) : 0;
            int awayZ = awayX != 0 ? 0 : (fz > 0.0 ? 1 : -1);
            int rightX = -awayZ;
            int rightZ = awayX;
            List<BuildLayout.Cell> cells = new ArrayList<>();
            long seed = seeds++;

            for (Builds.Saved s : saved) {
                double dx = s.pos.getX() - cx;
                double dz = s.pos.getZ() - cz;
                BuildStyles.Plan plan = BuildStyles.plan(
                    style.name(), dx * rightX + dz * rightZ, s.pos.getY() - floor, dx * awayX + dz * awayZ, top - floor + 1, 16.0, seed
                );
                cells.add(
                    new BuildLayout.Cell(
                        s.pos,
                        s.state,
                        s.nbt,
                        (float)(rightX * plan.fromX() + awayX * plan.fromZ()),
                        (float)plan.fromY(),
                        (float)(rightZ * plan.fromX() + awayZ * plan.fromZ()),
                        -plan.key()
                    )
                );
            }

            cells.sort(Comparator.comparingDouble(BuildLayout.Cell::key));
            double time = seconds >= 0.0 ? seconds : Math.max(2.0, Math.min(20.0, cells.size() / 150.0));
            Builds.Job job = new Builds.Job(
                id,
                id,
                script,
                level,
                cells,
                style,
                (int)Math.round(style.flight() * 20.0),
                (int)Math.round(time * 20.0),
                true,
                new BlockPos((int)cx, floor, (int)cz)
            );
            JOBS.add(job);
            return job;
        } else {
            return null;
        }
    }

    public static boolean known(String id) {
        return HISTORY.containsKey(id);
    }

    public static void stop(String id) {
        Iterator<Builds.Job> it = JOBS.iterator();

        while (it.hasNext()) {
            Builds.Job job = it.next();
            if (id.equals(job.id)) {
                finish(job);
                it.remove();
            }
        }
    }

    public static void storyStopped(int script) {
        Iterator<Builds.Job> it = JOBS.iterator();

        while (it.hasNext()) {
            Builds.Job job = it.next();
            if (job.script == script) {
                finish(job);
                it.remove();
            }
        }
    }

    private static void finishAll() {
        for (Builds.Job job : JOBS) {
            finish(job);
        }

        JOBS.clear();
    }

    private static void finish(Builds.Job job) {
        while (!job.landing.isEmpty()) {
            put(job, job.landing.poll().cell);
        }

        while (job.next < job.cells.size()) {
            put(job, job.cells.get(job.next++));
        }

        done(job);
    }

    private static void tick() {
        if (!JOBS.isEmpty()) {
            for (Builds.Job job : new ArrayList<>(JOBS)) {
                step(job);
                if (job.next >= job.cells.size() && job.landing.isEmpty()) {
                    JOBS.remove(job);
                    done(job);
                }
            }
        }
    }

    private static void step(Builds.Job job) {
        job.tick++;

        while (!job.landing.isEmpty() && job.landing.peek().at <= job.tick) {
            BuildLayout.Cell c = job.landing.poll().cell;
            put(job, c);
            landSound(job, c);
        }

        job.owed = job.owed + job.perTick;
        int count = (int)job.owed;
        job.owed -= count;
        if (count > 0) {
            List<BuildLayout.Cell> flying = new ArrayList<>();
            List<BlockState> ghosts = new ArrayList<>();

            for (int i = 0; i < count && job.next < job.cells.size(); i++) {
                BuildLayout.Cell c = job.cells.get(job.next++);
                if (job.undo) {
                    BlockState was = job.level.getBlockState(c.pos());
                    put(job, c);
                    if (!was.isAir() && job.flight > 0) {
                        flying.add(c);
                        ghosts.add(was);
                    }
                } else if (!c.state().isAir() && job.flight > 0) {
                    flying.add(c);
                    ghosts.add(c.state());
                    job.landing.add(new Builds.Landing(job.tick + job.flight, c));
                } else {
                    put(job, c);
                    landSound(job, c);
                }
            }

            if (!flying.isEmpty()) {
                send(job, flying, ghosts);
            }
        }
    }

    private static void send(Builds.Job job, List<BuildLayout.Cell> cells, List<BlockState> ghosts) {
        for (int i = 0; i < cells.size(); i += 30000) {
            int end = Math.min(cells.size(), i + 30000);
            sendPart(job, cells.subList(i, end), ghosts.subList(i, end));
        }
    }

    private static void sendPart(Builds.Job job, List<BuildLayout.Cell> cells, List<BlockState> ghosts) {
        long[] pos = new long[cells.size()];
        int[] states = new int[cells.size()];
        float[] from = new float[cells.size() * 3];

        for (int i = 0; i < pos.length; i++) {
            BuildLayout.Cell c = cells.get(i);
            pos[i] = c.pos().asLong();
            states[i] = Block.getId(ghosts.get(i));
            from[i * 3] = c.fromX();
            from[i * 3 + 1] = c.fromY();
            from[i * 3 + 2] = c.fromZ();
        }

        BuildFly msg = new BuildFly(job.flight, job.style.ease().ordinal(), job.undo, job.style.spinning(), (float)job.style.swirl(), pos, states, from);

        for (ServerPlayer p : job.level.players()) {
            if (p.blockPosition().distSqr(job.center) < 262144.0) {
                SprauteNetwork.sendTo(p, msg);
            }
        }
    }

    private static void put(Builds.Job job, BuildLayout.Cell c) {
        ServerLevel level = job.level;
        BlockPos pos = c.pos();
        if (!job.undo) {
            BlockEntity old = level.getBlockEntity(pos);
            job.saved.add(new Builds.Saved(pos, level.getBlockState(pos), old != null ? old.saveWithoutMetadata() : null));
        }

        level.setBlock(pos, c.state(), 18);
        if (c.nbt() != null) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag tag = c.nbt().copy();
                tag.putInt("x", pos.getX());
                tag.putInt("y", pos.getY());
                tag.putInt("z", pos.getZ());
                be.load(tag);
                be.setChanged();
            }
        }
    }

    private static void landSound(Builds.Job job, BuildLayout.Cell c) {
        if (!c.state().isAir() && job.sounds++ % 4 == 0) {
            SoundEvent sound = c.state().getSoundType().getPlaceSound();
            job.level.playSound(null, c.pos(), sound, SoundSource.BLOCKS, 0.5F, 0.8F + job.level.random.nextFloat() * 0.4F);
        }
    }

    private static void done(Builds.Job job) {
        if (!job.done) {
            job.done = true;
            Map<String, Object> data = new HashMap<>();
            data.put("id", job.id == null ? "" : job.id);
            data.put("name", job.name);
            data.put("x", (double)job.center.getX());
            data.put("y", (double)job.center.getY());
            data.put("z", (double)job.center.getZ());
            GameEvents.post(job.undo ? "buildremoved" : "buildend", null, data);
        }
    }

    public static final class Job {
        final String id;
        final String name;
        final int script;
        final ServerLevel level;
        final List<BuildLayout.Cell> cells;
        final BuildStyles.Style style;
        final int flight;
        final boolean undo;
        final double perTick;
        final Deque<Builds.Landing> landing = new ArrayDeque<>();
        final List<Builds.Saved> saved = new ArrayList<>();
        final BlockPos center;
        int next;
        int tick;
        int sounds;
        double owed;
        boolean done;

        Job(
            String id,
            String name,
            int script,
            ServerLevel level,
            List<BuildLayout.Cell> cells,
            BuildStyles.Style style,
            int flight,
            int ticks,
            boolean undo,
            BlockPos center
        ) {
            this.id = id;
            this.name = name;
            this.script = script;
            this.level = level;
            this.cells = cells;
            this.style = style;
            this.flight = flight;
            this.undo = undo;
            this.center = center;
            this.perTick = (double)cells.size() / Math.max(1, ticks);
        }

        public boolean finished() {
            return this.done;
        }
    }

    private record Landing(int at, BuildLayout.Cell cell) {
    }

    private record Saved(BlockPos pos, BlockState state, CompoundTag nbt) {
    }
}
