package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.api.ScriptResultCall;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;

public abstract class SoundCall implements ScriptResultCall {
    protected final SoundEvent soundEvent;
    protected final SoundSource soundCategory;
    protected final float volume;
    protected final float pitch;

    public SoundCall(SoundEvent soundEvent, SoundSource soundCategory, float volume, float pitch) {
        this.soundEvent = soundEvent;
        this.volume = volume;
        this.pitch = pitch;
        this.soundCategory = soundCategory;
    }

    @ApiInternal
    public abstract void run(Level world, ScriptVector3f basePos);

    @Override
    public void validate() {
        if(this.soundEvent == null) throw new IllegalStateException("SoundEvent must not be null!");
        if(this.volume < 0) throw new IllegalStateException("Volume must not be negative!");
        if(this.pitch < 0) throw new IllegalStateException("Pitch must not be negative!");
    }
}
