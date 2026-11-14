package com.lx862.jcm.mod.block.behavior;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface WallAttachedBlockBehavior {
    static boolean canBePlaced(BlockPos pos, Level world, Direction directionTowardsWall) {
        BlockPos wallBlockPos = pos.relative(directionTowardsWall);
        BlockState wallBlock = world.getBlockState(wallBlockPos);

        return wallBlock.isFaceSturdy((BlockGetter)world, pos, directionTowardsWall.getOpposite());
    }

    default Direction getWallDirection(Direction defaultDirection) {
        return defaultDirection;
    }
}
