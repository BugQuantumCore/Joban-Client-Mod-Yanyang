package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.jcm.mod.compat.SoundHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public class NonPositionedSoundCall extends SoundCall {
    public NonPositionedSoundCall(ResourceLocation id, SoundSource soundCategory, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), soundCategory, volume, pitch);
    }

    @Override
    public void run(Level world, ScriptVector3f basePos) {
        Minecraft.getInstance().getSoundManager().play(new NonPositionedSoundInstance(soundEvent, soundCategory, volume, pitch));
    }
}
