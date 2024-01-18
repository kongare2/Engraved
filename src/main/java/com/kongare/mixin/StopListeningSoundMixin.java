package com.kongare.mixin;

import com.kongare.StopListeningSound;
import com.kongare.WrappedSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.CompletableFuture;

@Mixin(StopListeningSound.class)
public abstract class StopListeningSoundMixin implements SoundInstance, WrappedSoundInstance {

    @Override
    public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, ResourceLocation id, boolean repeatInstantly) {
        return this.getParent().getAudioStream(loader, id, repeatInstantly);
    }
}
