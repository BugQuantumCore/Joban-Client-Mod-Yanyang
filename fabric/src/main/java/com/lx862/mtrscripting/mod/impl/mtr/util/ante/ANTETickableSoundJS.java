package com.lx862.mtrscripting.mod.impl.mtr.util.ante;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.sound.TickableSoundInstanceJS;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

@Deprecated
public class ANTETickableSoundJS {

    public final TickableSoundInstanceJS tickableSoundInstanceJS;

    public ANTETickableSoundJS(ResourceLocation sound) {
        this.tickableSoundInstanceJS = TickableSoundInstanceJS.create(sound, SoundSource.BLOCKS);
    }

    public ANTETickableSoundJS(ResourceLocation sound, SoundSource category) {
        this.tickableSoundInstanceJS = TickableSoundInstanceJS.create(sound, category);
    }

    public void setData(float volume, float pitch, ScriptVector3f pos) {
        this.tickableSoundInstanceJS.setSoundVolume(volume);
        this.tickableSoundInstanceJS.setSoundPitch(pitch);
        this.tickableSoundInstanceJS.setPos(pos);
    }

    public void setLooping(boolean bl) {
        this.tickableSoundInstanceJS.setLoopable(bl);
    }

    public void setDelay(int tick) {
        this.tickableSoundInstanceJS.setLoopDelay(tick);
    }

    public void setAttenuation(Object type) {
        throw new IllegalArgumentException("Not implemented.");
    }

    public void play() {
        ANTESoundHelperJS.play(this);
    }

    public void quit() {
        ANTESoundHelperJS.stop(this);
    }

    public void pause() {
        ANTESoundHelperJS.stop(this);
    }

    public void setRelative(boolean relative) {
        this.tickableSoundInstanceJS.setRelative(relative);
    }

    public Object getSound() {
        throw new IllegalArgumentException("Not implemented.");
    }

    public ResourceLocation getLocation() {
        return this.tickableSoundInstanceJS.getLocation();
    }

    public SoundSource getSource() {
        return this.tickableSoundInstanceJS.getSource();
    }

    public boolean isLooping() {
        return this.tickableSoundInstanceJS.isPlaying();
    }

    public float getVolume() {
        return this.tickableSoundInstanceJS.getVolume();
    }

    public float getPitch() {
        return this.tickableSoundInstanceJS.getPitch();
    }

    public Object getAttenuation() {
        throw new IllegalArgumentException("Not implemented.");
    }

    public int getDelay() {
        return this.tickableSoundInstanceJS.getDelay();
    }

    public boolean isRelative() {
        return this.tickableSoundInstanceJS.isRelative();
    }
}
