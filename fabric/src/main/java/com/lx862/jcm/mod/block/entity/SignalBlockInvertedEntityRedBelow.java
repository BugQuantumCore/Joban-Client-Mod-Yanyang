package com.lx862.jcm.mod.block.entity;

import com.lx862.jcm.mod.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockSignalBase;

public class SignalBlockInvertedEntityRedBelow extends BlockSignalBase.BlockEntityBase {
    public SignalBlockInvertedEntityRedBelow(BlockPos blockPos, BlockState blockState) {
        super(BlockEntities.SIGNAL_LIGHT_INVERTED_RED_BELOW.get(), false, blockPos, blockState);
    }
}
