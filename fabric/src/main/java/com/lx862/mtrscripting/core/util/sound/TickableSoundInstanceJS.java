package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.api.AbstractScriptContext;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.util.JSUtil;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import com.lx862.jcm.mod.compat.SoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class TickableSoundInstanceJS extends AbstractTickableSoundInstance implements AutoCloseable {
    private boolean instanceInUse = false;
    private boolean isRepeatable = false;
    private boolean useNewRepeatBehaviour = false;

    protected TickableSoundInstanceJS(ResourceLocation soundId, String category) {
        this(SoundHelper.createSoundEvent(soundId), SoundSource.valueOf(category));
    }

    protected TickableSoundInstanceJS(SoundEvent sound, SoundSource category) {
        super(sound, category, net.minecraft.util.RandomSource.create());
    }

    @ApiInternal
    public static TickableSoundInstanceJS create(ResourceLocation soundId, SoundSource category) {
        return new TickableSoundInstanceJS(SoundHelper.createSoundEvent(soundId), category);
    }

    public static TickableSoundInstanceJS create(ResourceLocation soundId, String category) {
        return new TickableSoundInstanceJS(soundId, category);
    }

    public static TickableSoundInstanceJS create(String soundId, String category) {
        return new TickableSoundInstanceJS(JSUtil.parseId(soundId), category);
    }

    // TODO: Getters

    public TickableSoundInstanceJS setSoundVolume(float f) {
        this.volume = f;
        return this;
    }

    public TickableSoundInstanceJS setSoundPitch(float f) {
        this.pitch = f;
        return this;
    }

    public TickableSoundInstanceJS setRepeatable(boolean canRepeat) {
        this.useNewRepeatBehaviour = true;
        this.isRepeatable = canRepeat;
        return this;
    }

    /** Legacy method that directly sets the sound instance. Not recommended due to manually managed */
    @Deprecated
    public TickableSoundInstanceJS setLoopable(boolean canLoop) {
        this.looping = canLoop;
        return this;
    }

    public TickableSoundInstanceJS setLoopDelay(int tick) {
        this.delay = tick;
        return this;
    }

    public TickableSoundInstanceJS setRelative(boolean isRelative) {
        this.relative = isRelative;
        return this;
    }

    public TickableSoundInstanceJS setPos(ScriptVector3f pos) {
        this.x = pos.x();
        this.y = pos.y();
        this.z = pos.z();
        return this;
    }

    public boolean isPlaying() {
        return Minecraft.getInstance().getSoundManager().isActive(this);
    }

    public void play(AbstractScriptContext abstractScriptContext) {
        play();
        abstractScriptContext.addDisposable(this);
    }

    public void play(AbstractScriptContext abstractScriptContext, int delay) {
        play(delay);
        abstractScriptContext.addDisposable(this);
    }

    public void play() {
        if(isInstanceInUse()) return; // Don't allow playing twice

        if(this.useNewRepeatBehaviour) this.looping = this.isRepeatable;
        setInstanceInUse(true);
        Minecraft.getInstance().submit(() -> {
            Minecraft.getInstance().getSoundManager().play(this);
        });
    }

    public void play(int delay) {
        if(isInstanceInUse()) return; // Don't allow playing twice

        setInstanceInUse(true);
        if(this.useNewRepeatBehaviour) this.looping = this.isRepeatable;
        Minecraft.getInstance().submit(() -> {
            Minecraft.getInstance().getSoundManager().playDelayed(this, delay);
        });
    }

    // N.B. stop() is already in mojmap and is final
    public void abort() {
        if(!isInstanceInUse()) return;

        setInstanceInUse(false);
        this.looping = false;
        Minecraft.getInstance().submit(() -> {
            Minecraft.getInstance().getSoundManager().stop(this);
        });
    }

    @ApiInternal
    protected void setInstanceInUse(boolean isPlaying) {
        this.instanceInUse = isPlaying;
    }

    @ApiInternal
    protected boolean isInstanceInUse() {
        return this.instanceInUse;
    }

    @ApiInternal
    @Override
    public boolean canPlaySound() {
        return true;
    }

    @ApiInternal
    @Override
    public void tick() {
    }

    @Override
    public void close() throws Exception {
        abort();
    }
}
