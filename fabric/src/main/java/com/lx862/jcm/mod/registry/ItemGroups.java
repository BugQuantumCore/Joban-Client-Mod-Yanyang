package com.lx862.jcm.mod.registry;

import com.lx862.jcm.mod.Constants;
import net.minecraft.world.item.ItemStack;

public class ItemGroups {
    public static final String MAIN = JCMRegistry.registerItemGroup(
            Constants.id("main").getPath(),
            () -> new ItemStack(Blocks.MTR_STAIRS.get())
    );

    public static final String PIDS = JCMRegistry.registerItemGroup(
            Constants.id("pids").getPath(),
            () -> new ItemStack(Blocks.RV_PIDS.get())
    );
}
