package com.lx862.mtrscripting.core.integration;

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
public class PlayerEntityWrapper extends EntityWrapper {
    private final Player playerEntity;

    @ApiInternal
    public PlayerEntityWrapper(Player player) {
        super((Entity)player);
        this.playerEntity = player;
    }

    public boolean isSpectator() {
        return playerEntity.isSpectator();
    }

    public boolean isCreative() {
        return playerEntity.isCreative();
    }

    public String playerName() {
        return playerEntity.getGameProfile().getName();
    }

    public String displayName() {
        return playerEntity.getDisplayName().getString();
    }

    public boolean isSneaking() {
        return playerEntity.isShiftKeyDown();
    }

    public boolean isSprinting() {
        return playerEntity.isSprinting();
    }

    public boolean isSwimming() {
        return playerEntity.isSwimming();
    }

    public boolean isHoldingItem(ResourceLocation id) {
        Item itm = LoaderImpl.getItemFromId(id);
        if(itm == null) return false;
        return playerEntity.isHolding(itm);
    }

    public ItemStackWrapper mainHandItem() {
        return new ItemStackWrapper(playerEntity.getMainHandItem());
    }

    public ItemStackWrapper offHandItem() {
        return new ItemStackWrapper(playerEntity.getOffhandItem());
    }

    public ItemStackWrapper activeItem() {
        ItemStackWrapper mainHandItem = mainHandItem();
        if(!mainHandItem.empty()) return mainHandItem;

        return offHandItem();
    }
}
