package com.lx862.jcm.mod.compat;

import net.minecraft.client.Minecraft;
import com.lx862.jcm.mod.compat.MinecraftClientHelper;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.compat.MinecraftClientHelper}. Static call surface preserved.
 */
public final class MinecraftClientHelper {

	public static int getRenderDistance() {
		return Minecraft.getInstance().options.renderDistance().get();
	}
}
