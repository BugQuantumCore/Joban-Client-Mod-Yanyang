package com.lx862.jcm.mod.util;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Contains utilities method for block checking/manipulation
 */
public class BlockUtil {
    public static boolean canSurvive(Block instance, LevelAccessor world, BlockPos pos, Direction facing, int part, int totalWidthHeight) {
        boolean checkLeftOrBottom = part != 0;
        boolean checkRightOrTop = part != totalWidthHeight - 1;
        boolean canSurvive = true;

        if (checkLeftOrBottom) {
            BlockPos posLeftOrDown = pos.relative(facing.getOpposite());
            BlockState blockLeftOrDown = world.getBlockState(posLeftOrDown);
            if (!blockLeftOrDown.getBlock().equals(instance)) {
                return false;
            }
        }

        if (checkRightOrTop) {
            BlockPos posRightOrUp = pos.relative(facing);
            BlockState blockRightOrUp = world.getBlockState(posRightOrUp);
            if (!blockRightOrUp.getBlock().equals(instance)) {
                return false;
            }
        }
        return canSurvive;
    }

    public static boolean isReplacable(Level world, BlockPos startPos, Direction direction, BlockPlaceContext ctx, int distance) {
        for (int i = 0; i < distance; i++) {
            BlockPos posUp = startPos.relative(direction, i);
            BlockState blockState = world.getBlockState(posUp);
            if (!blockState.canBeReplaced(ctx)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get block entity in world, but will return null if chunk is not loaded (And will not attempt to load the chunk).
     */
    public static @Nullable BlockEntity getBlockEntityOrNull(Level world, BlockPos pos) {
        if(!world.getChunkSource().hasChunk(getChunkCoords(pos.getX()), getChunkCoords(pos.getZ()))) return null;
        return world.getBlockEntity(pos);
    }

    /**
     * Get block state in world, but will return null if chunk is not loaded (And will not attempt to load the chunk).
     */
    public static @Nullable BlockState getBlockState(Level world, BlockPos pos) {
        if(!world.getChunkSource().hasChunk(getChunkCoords(pos.getX()), getChunkCoords(pos.getZ()))) return null;
        return world.getBlockState(pos);
    }

    private static int getChunkCoords(int pos) {
        return pos >> 4;
    }
}
