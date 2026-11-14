package com.lx862.jcm.mod.compat;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.function.Predicate;
import com.lx862.jcm.mod.compat.PlayerHelper;
import net.minecraft.world.Container;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.compat.PlayerHelper}. Static call surface preserved.
 */
public final class PlayerHelper {

	public static Inventory getPlayerInventory(Player player) {
		return player.getInventory();
	}

	public static boolean isHolding(Player player, Predicate<Item> itemPredicate) {
		return player.getMainHandItem().getItem() instanceof Item item && itemPredicate.test(item)
				|| player.getOffhandItem().getItem() instanceof Item item2 && itemPredicate.test(item2);
	}
}
