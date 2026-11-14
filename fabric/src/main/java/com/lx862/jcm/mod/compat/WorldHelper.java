package com.lx862.jcm.mod.compat;

import net.minecraft.world.level.LevelAccessor;
import com.lx862.jcm.mod.compat.WorldHelper;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.compat.WorldHelper}. Static call surface preserved.
 */
public final class WorldHelper {

	public static long getTimeOfDay(LevelAccessor world) {
		return ((net.minecraft.world.level.Level)world).getDayTime();
	}
}
