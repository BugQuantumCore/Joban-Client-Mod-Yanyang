package com.lx862.jcm.mod.block.base;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;

public abstract class PoleBlock extends SlabExtendableBlock {
    public PoleBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    public boolean canPlace(Block block) {
        return (block instanceof PoleBlock) || blockIsAllowed(block);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        if(superState == null) return null;

        BlockState blockBelow = ctx.getLevel().getBlockState(ctx.getClickedPos().below());
        if (this.canPlace(blockBelow.getBlock()) && blockIsAllowed(blockBelow.getBlock())) {
            return superState;
        } else {
            return null;
        }
    }

    public abstract boolean blockIsAllowed(Block block);
}
