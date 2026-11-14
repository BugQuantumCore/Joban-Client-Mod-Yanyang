package com.lx862.jcm.mod.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockEntityExtension;

public abstract class JCMBlockEntityBase extends BlockEntityExtension {

    public JCMBlockEntityBase(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState) {
        super(type, blockPos, blockState);
    }

    /**
     * Server-side tick, driven by the owning block's
     * {@code EntityBlock#getTicker}. Kept from the 4.0-era mapping-layer
     * {@code blockEntityTick} hook.
     */
    public void tick() {
    }
}
