package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.SignalBlockInvertedEntityRedBelow;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockSignalLightBase;

public class InvertedSignalBlockRedBelow extends BlockSignalLightBase {
    public InvertedSignalBlockRedBelow(BlockBehaviour.Properties settings) {
        super(settings, 2, 14);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SignalBlockInvertedEntityRedBelow(blockPos, blockState);
    }
}
