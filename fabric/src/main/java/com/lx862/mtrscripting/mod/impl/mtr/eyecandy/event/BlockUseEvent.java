package com.lx862.mtrscripting.mod.impl.mtr.eyecandy.event;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.integration.ItemStackWrapper;
import com.lx862.mtrscripting.core.integration.PlayerEntityWrapper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

public class BlockUseEvent {
    private final PlayerEntityWrapper entity;
    private final ItemStackWrapper itemUsed;

    @ApiInternal
    public BlockUseEvent(Player playerEntity, ItemStack itemUsed) {
        this.entity = new PlayerEntityWrapper(playerEntity);
        this.itemUsed = new ItemStackWrapper(itemUsed);
    }

    public PlayerEntityWrapper player() {
        return entity;
    }

    public ItemStackWrapper item() {
        return itemUsed;
    }
}
