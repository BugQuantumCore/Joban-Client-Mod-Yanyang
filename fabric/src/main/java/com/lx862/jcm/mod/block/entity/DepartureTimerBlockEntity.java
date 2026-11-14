package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockEntityExtension;

public class DepartureTimerBlockEntity extends BlockEntityExtension {
    public DepartureTimerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.DEPARTURE_TIMER.get(), blockPos, blockState);
    }
}
