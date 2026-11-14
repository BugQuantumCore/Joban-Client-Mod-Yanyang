package com.lx862.jcm.mod.block.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;

public interface VerticallyAttachedBlock {
    static boolean canPlace(boolean canAttachTop, boolean canAttachBottom, BlockPos pos, BlockPlaceContext ctx) {
        Level world = ctx.getLevel();

        boolean blockAboveSolid = !world.getBlockState(pos.above()).isAir();
        boolean blockBelowSolid = !world.getBlockState(pos.below()).isAir();

        if (!blockAboveSolid && !blockBelowSolid) return false;
        if (!canAttachTop && !blockBelowSolid) return false;
        return canAttachBottom || blockAboveSolid;
    }

    static boolean canPlace(boolean canAttachTop, boolean canAttachBottom, BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        return canPlace(canAttachTop, canAttachBottom, pos, ctx);
    }
}
