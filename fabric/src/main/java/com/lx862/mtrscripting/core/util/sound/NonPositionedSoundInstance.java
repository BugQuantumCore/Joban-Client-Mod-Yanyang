package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;

@ApiInternal
public class NonPositionedSoundInstance extends AbstractSoundInstance {
    protected NonPositionedSoundInstance(SoundEvent sound, SoundSource category, float volume, float pitch) {
        super(sound, category, RandomSource.create());
        this.volume = volume;
        this.pitch = pitch;
        this.looping = false;
        this.relative = true;
    }
}
