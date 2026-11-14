package com.lx862.mtrscripting.core.util.sound;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.jcm.mod.compat.SoundHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

public class PositionedSoundCall extends SoundCall {
    protected final double x;
    protected final double y;
    protected final double z;

    public PositionedSoundCall(ResourceLocation id, SoundSource soundCategory, double x, double y, double z, float volume, float pitch) {
        super(SoundHelper.createSoundEvent(id), soundCategory, volume, pitch);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void run(Level world, ScriptVector3f basePos) {
        ScriptVector3f finalPos = basePos.copy();
        finalPos.add((float)x, (float)y, (float)z);
        world.playSound(null, finalPos.x(), finalPos.y(), finalPos.z(), soundEvent, soundCategory, volume, pitch);
    }
}
