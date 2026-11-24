package com.lx862.mtrscripting.mod.impl.mtr.util.ante;

import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import net.minecraft.sounds.SoundSource;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Deprecated
public class ANTESoundHelperJS {
    private static final ScriptSoundManager dummySoundManager = new ScriptSoundManager();

    /* Legacy ANTE sounds are played context-less — nothing attaches them to
       a script instance, so a looping sound used to keep playing forever
       after its script instance was disposed (block broken, resource reload).
       Track them here so resetAll() can stop them on script reload. */
    private static final Set<ANTETickableSoundJS> PLAYING = ConcurrentHashMap.newKeySet();

    public static void play(ANTETickableSoundJS tickableSoundInstance) {
        PLAYING.add(tickableSoundInstance);
        dummySoundManager.play(tickableSoundInstance.tickableSoundInstanceJS);
    }

    public static void play(ANTETickableSoundJS tickableSoundInstance, int delay) {
        PLAYING.add(tickableSoundInstance);
        dummySoundManager.play(tickableSoundInstance.tickableSoundInstanceJS, delay);
    }

    public static void stop(ANTETickableSoundJS tickableSoundInstance) {
        PLAYING.remove(tickableSoundInstance);
        dummySoundManager.stop(tickableSoundInstance.tickableSoundInstanceJS);
    }

    /** Stops every ANTE sound started through this helper (script reload /
     *  world leave). */
    public static void resetAll() {
        for(ANTETickableSoundJS sound : PLAYING) {
            sound.tickableSoundInstanceJS.abort();
        }
        PLAYING.clear();
    }

    public static SoundSource getSoundSource(String str) {
        return SoundSource.valueOf(str);
    }
}
