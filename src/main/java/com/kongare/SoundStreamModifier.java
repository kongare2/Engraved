package com.kongare;

import net.minecraft.client.sounds.AudioStream;

public interface SoundStreamModifier {

    AudioStream modifyStream(AudioStream stream);
}
