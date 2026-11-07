package org.sprautestudio.spraute_engine.platform;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;

public interface CustomSoundStream {
    CompletableFuture<AudioStream> getStream(SoundBufferLibrary var1, Sound var2, boolean var3);
}
