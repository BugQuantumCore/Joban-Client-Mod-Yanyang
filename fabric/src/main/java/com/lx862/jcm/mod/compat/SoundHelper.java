package com.lx862.jcm.mod.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import com.lx862.jcm.mod.compat.SoundHelper;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.compat.SoundHelper}. Static call surface preserved.
 */
public final class SoundHelper {

	public static SoundEvent createSoundEvent(ResourceLocation identifier) {
		return SoundEvent.createVariableRangeEvent(identifier);
	}
}
