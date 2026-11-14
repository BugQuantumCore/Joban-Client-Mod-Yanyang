package com.lx862.mtrscripting.core.integration;

import com.lx862.jcm.mapping.LoaderImpl;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemStackWrapper {
    private final ItemStack itemStack;

    @ApiInternal
    public ItemStackWrapper(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public String itemId() {
        ResourceLocation id = LoaderImpl.getIdFromItem(this.itemStack.getItem());
        return id.getNamespace() + ":" + id.getPath();
    }

    public String translationId() {
        return this.itemStack.getDescriptionId();
    }

    public boolean empty() {
        return this.itemStack.isEmpty();
    }

    public int count() {
        return this.itemStack.getCount();
    }
}
