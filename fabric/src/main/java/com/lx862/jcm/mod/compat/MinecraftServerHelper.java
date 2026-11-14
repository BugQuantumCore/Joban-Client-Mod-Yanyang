package com.lx862.jcm.mod.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Consumer;
import com.lx862.jcm.mod.compat.MinecraftServerHelper;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.compat.MinecraftServerHelper}. Static call surface preserved.
 */
public final class MinecraftServerHelper {

	public static void iterateWorlds(MinecraftServer minecraftServer, Consumer<ServerLevel> consumer) {
		minecraftServer.getAllLevels().forEach(consumer);
	}

	public static void iteratePlayers(MinecraftServer minecraftServer, Consumer<ServerPlayer> consumer) {
		minecraftServer.getPlayerList().getPlayers().forEach(consumer);
	}

	public static void iteratePlayers(ServerLevel serverWorld, Consumer<ServerPlayer> consumer) {
		serverWorld.players().forEach(consumer);
	}

	public static ResourceLocation getWorldId(net.minecraft.world.level.Level world) {
		return world.dimension().location();
	}
}
