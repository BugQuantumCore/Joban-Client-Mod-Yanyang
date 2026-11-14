package com.lx862.jcm.mod.block;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;

public class MTRStairsBlock extends StairBlock {
    public MTRStairsBlock(BlockBehaviour.Properties settings) {
        super(Blocks.SMOOTH_STONE.defaultBlockState(), settings);
    }
}