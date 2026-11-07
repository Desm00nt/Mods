package org.sprautestudio.spraute_engine.engine.client;

import com.mojang.blaze3d.audio.OggAudioStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioFormat.Encoding;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound.Type;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import org.lwjgl.BufferUtils;
import org.sprautestudio.spraute_engine.SprauteEngine;
import org.sprautestudio.spraute_engine.compat.Mc;
import org.sprautestudio.spraute_engine.compat.Regs;
import org.sprautestudio.spraute_engine.content.ContentClient;
import org.sprautestudio.spraute_engine.editor.project.Project;
import org.sprautestudio.spraute_engine.editor.project.Workspace;
import org.sprautestudio.spraute_engine.platform.CustomSoundStream;
import org.sprautestudio.spraute_engine.runtime.Images;

public final class ProjectSounds {
    private static final Map<String, SoundInstance> PLAYING = new HashMap<>();
    private static final Map<String, Path> DIRS = new HashMap<>();

    private ProjectSounds() {
    }

    private static Path projectDir(String project) {
        if (project != null && !project.isEmpty()) {
            Path fromServer = ContentClient.dir(project);
            if (fromServer != null) {
                return fromServer;
            } else {
                Path direct = Workspace.projectsDir().resolve(project);
                return Files.isDirectory(direct) ? direct : DIRS.computeIfAbsent(project, n -> {
                    for (Workspace.ProjectInfo i : Workspace.projects()) {
                        if (i.name().equals(n)) {
                            return i.dir();
                        }
                    }

                    return null;
                });
            }
        } else {
            return null;
        }
    }

    public static Path file(String project, String name) {
        Path dir = projectDir(project);
        if (dir != null && name != null && !name.isBlank() && !name.contains(":")) {
            for (String ext : Project.SOUND_EXTENSIONS) {
                Path p = dir.resolve("sounds").resolve(name + ext);
                if (Files.isRegularFile(p)) {
                    return p;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public static boolean exists(String project, String name) {
        return file(project, name) != null;
    }

    public static void ui(String project, String value) {
        if (value != null && !value.isBlank()) {
            String v = value.trim();
            if (exists(project, v)) {
                play(project, v, 0.0, 0.0, 0.0, false, 1.0F, 1.0F, false, "");
            } else {
                ResourceLocation rl = ResourceLocation.tryParse(v.contains(":") ? v : "minecraft:" + v);
                if (rl != null) {
                    Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(Mc.soundEvent(rl), 1.0F));
                }
            }
        }
    }

    public static void playBuiltin(String name) {
        ResourceLocation file = new ResourceLocation("spraute_engine", "sounds/ui/" + name + ".wav");
        Minecraft mc = Minecraft.getInstance();

        try (InputStream in = mc.getResourceManager().open(file)) {
            mc.getSoundManager()
                .play(
                    new ProjectSounds.Instance(
                        new ResourceLocation("spraute_engine", "project_sound/builtin/" + name),
                        in.readAllBytes(),
                        true,
                        1.0F,
                        1.0F,
                        false,
                        false,
                        0.0,
                        0.0,
                        0.0
                    )
                );
        } catch (IOException var8) {
            SprauteEngine.LOGGER.warn("Spraute: no sound {}", file, var8);
        }
    }

    public static void play(String project, String name, double x, double y, double z, boolean positional, float volume, float pitch, boolean loop, String id) {
        play(project, name, x, y, z, positional, volume, pitch, loop, id, 0.0F);
    }

    public static void play(
        String project, String name, double x, double y, double z, boolean positional, float volume, float pitch, boolean loop, String id, float fade
    ) {
        PLAYING.values().removeIf(s -> !Minecraft.getInstance().getSoundManager().isActive(s) && !(s instanceof ProjectSounds.Fading fd && fd.fade().fresh()));
        Path f = file(project, name);
        if (f == null) {
            ResourceLocation rl = name.contains(":") ? ResourceLocation.tryParse(name) : null;
            SoundEvent ev = rl == null ? null : Regs.SOUND_EVENT.get(rl);
            if (ev == null) {
                SprauteEngine.LOGGER.warn("Spraute: sound «{}» not found in project {}", name, project);
            } else {
                if (id != null && !id.isEmpty()) {
                    stop(id, 0.0F);
                }

                ProjectSounds.GameSound g = new ProjectSounds.GameSound(ev, volume, pitch, loop, positional, x, y, z, fade);
                Minecraft.getInstance().getSoundManager().play(g);
                PLAYING.put(id != null && !id.isEmpty() ? id : "~" + System.nanoTime(), g);
            }
        } else {
            byte[] data;
            try {
                data = Files.readAllBytes(f);
            } catch (IOException var18) {
                SprauteEngine.LOGGER.warn("Spraute: cannot read {}", f, var18);
                return;
            }

            if (id != null && !id.isEmpty()) {
                stop(id, 0.0F);
            }

            ProjectSounds.Instance in = new ProjectSounds.Instance(
                new ResourceLocation("spraute_engine", "project_sound/" + Images.slug(project) + "/" + Images.slug(name)),
                data,
                f.getFileName().toString().toLowerCase().endsWith(".wav"),
                volume,
                pitch,
                loop,
                positional,
                x,
                y,
                z,
                fade
            );
            Minecraft.getInstance().getSoundManager().play(in);
            PLAYING.put(id != null && !id.isEmpty() ? id : "~" + System.nanoTime(), in);
        }
    }

    public static void stop(String id) {
        stop(id, 0.0F);
    }

    public static void stop(String id, float fade) {
        if (id != null && !id.isEmpty()) {
            SoundInstance s = PLAYING.remove(id);
            if (s != null) {
                stopOne(s, fade);
            }
        } else {
            for (SoundInstance s : PLAYING.values()) {
                stopOne(s, fade);
            }

            PLAYING.clear();
        }
    }

    private static void stopOne(SoundInstance s, float fade) {
        if (fade > 0.0F && s instanceof ProjectSounds.Fading f) {
            f.fade().out(fade);
        } else {
            Minecraft.getInstance().getSoundManager().stop(s);
        }
    }

    static final class Fade {
        private final float base;
        private final int inTicks;
        private int age;
        private int outTicks;
        private int outLeft = -1;

        Fade(float base, float inSeconds) {
            this.base = base;
            this.inTicks = Math.max(0, Math.round(inSeconds * 20.0F));
        }

        boolean fresh() {
            return this.age < 2;
        }

        void out(float seconds) {
            this.outTicks = Math.max(1, Math.round(seconds * 20.0F));
            this.outLeft = this.outTicks;
        }

        float tick() {
            this.age++;
            float v = this.inTicks > 0 ? this.base * Math.min(1.0F, (float)this.age / this.inTicks) : this.base;
            if (this.outLeft >= 0) {
                v *= (float)Math.max(0, this.outLeft) / this.outTicks;
                this.outLeft--;
            }

            return v;
        }

        float start() {
            return this.inTicks > 0 ? 1.0E-4F : this.base;
        }

        boolean done() {
            return this.outTicks > 0 && this.outLeft < 0;
        }
    }

    interface Fading {
        ProjectSounds.Fade fade();
    }

    private static final class GameSound extends AbstractTickableSoundInstance implements ProjectSounds.Fading {
        private final ProjectSounds.Fade fade;

        GameSound(SoundEvent ev, float volume, float pitch, boolean loop, boolean positional, double x, double y, double z, float fadeIn) {
            super(ev, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.fade = new ProjectSounds.Fade(volume, fadeIn);
            this.volume = this.fade.start();
            this.pitch = pitch;
            this.looping = loop;
            this.x = x;
            this.y = y;
            this.z = z;
            this.relative = !positional;
            this.attenuation = positional ? Attenuation.LINEAR : Attenuation.NONE;
        }

        @Override
        public ProjectSounds.Fade fade() {
            return this.fade;
        }

        @Override
        public void tick() {
            this.volume = this.fade.tick();
            if (this.fade.done()) {
                this.stop();
            }
        }
    }

    private static final class Instance extends AbstractSoundInstance implements CustomSoundStream, TickableSoundInstance, ProjectSounds.Fading {
        private final byte[] data;
        private final boolean wav;
        private final ProjectSounds.Fade fade;
        private boolean stopped;

        Instance(ResourceLocation rl, byte[] data, boolean wav, float volume, float pitch, boolean loop, boolean positional, double x, double y, double z) {
            this(rl, data, wav, volume, pitch, loop, positional, x, y, z, 0.0F);
        }

        Instance(
            ResourceLocation rl,
            byte[] data,
            boolean wav,
            float volume,
            float pitch,
            boolean loop,
            boolean positional,
            double x,
            double y,
            double z,
            float fadeIn
        ) {
            super(rl, SoundSource.MASTER, SoundInstance.createUnseededRandom());
            this.data = data;
            this.wav = wav;
            this.fade = new ProjectSounds.Fade(volume, fadeIn);
            this.volume = this.fade.start();
            this.pitch = pitch;
            this.looping = loop;
            this.x = x;
            this.y = y;
            this.z = z;
            this.relative = !positional;
            this.attenuation = positional ? Attenuation.LINEAR : Attenuation.NONE;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            this.sound = new Sound(this.getLocation().toString(), ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Type.FILE, true, false, 24);
            WeighedSoundEvents w = new WeighedSoundEvents(this.getLocation(), null);
            w.addSound(this.sound);
            return w;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary lib, Sound s, boolean looping) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    InputStream in = new ByteArrayInputStream(this.data);
                    AudioStream st = (AudioStream)(this.wav ? new ProjectSounds.WavStream(in) : new OggAudioStream(in));
                    return (AudioStream)(looping ? new ProjectSounds.Looping(st, this) : st);
                } catch (Exception var4) {
                    throw new CompletionException(var4);
                }
            }, Util.backgroundExecutor());
        }

        @Override
        public ProjectSounds.Fade fade() {
            return this.fade;
        }

        @Override
        public boolean isStopped() {
            return this.stopped;
        }

        @Override
        public void tick() {
            this.volume = this.fade.tick();
            if (this.fade.done()) {
                this.stopped = true;
            }
        }

        AudioStream reopen() throws IOException {
            InputStream in = new ByteArrayInputStream(this.data);
            return (AudioStream)(this.wav ? new ProjectSounds.WavStream(in) : new OggAudioStream(in));
        }
    }

    private static final class Looping implements AudioStream {
        private AudioStream cur;
        private final ProjectSounds.Instance owner;

        Looping(AudioStream first, ProjectSounds.Instance owner) {
            this.cur = first;
            this.owner = owner;
        }

        @Override
        public AudioFormat getFormat() {
            return this.cur.getFormat();
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            ByteBuffer b = this.cur.read(size);
            if (b == null || !b.hasRemaining()) {
                this.cur.close();
                this.cur = this.owner.reopen();
                b = this.cur.read(size);
            }

            return b;
        }

        @Override
        public void close() throws IOException {
            this.cur.close();
        }
    }

    private static final class WavStream implements AudioStream {
        private final AudioInputStream in;
        private final AudioFormat format;

        WavStream(InputStream raw) throws IOException {
            try {
                AudioInputStream src = AudioSystem.getAudioInputStream(new BufferedInputStream(raw));
                AudioFormat f = src.getFormat();
                AudioFormat pcm = new AudioFormat(
                    Encoding.PCM_SIGNED, f.getSampleRate(), 16, Math.min(2, f.getChannels()), Math.min(2, f.getChannels()) * 2, f.getSampleRate(), false
                );
                this.in = AudioSystem.getAudioInputStream(pcm, src);
                this.format = pcm;
            } catch (Exception var5) {
                throw new IOException("WAV не читается: " + var5.getMessage(), var5);
            }
        }

        @Override
        public AudioFormat getFormat() {
            return this.format;
        }

        @Override
        public ByteBuffer read(int size) throws IOException {
            int frame = this.format.getFrameSize();
            byte[] buf = new byte[Math.max(frame, size - size % frame)];
            int n = 0;

            while (n < buf.length) {
                int r = this.in.read(buf, n, buf.length - n);
                if (r < 0) {
                    break;
                }

                n += r;
            }

            n -= n % frame;
            ByteBuffer out = BufferUtils.createByteBuffer(n);
            out.put(buf, 0, n).flip();
            return out;
        }

        @Override
        public void close() throws IOException {
            this.in.close();
        }
    }
}
