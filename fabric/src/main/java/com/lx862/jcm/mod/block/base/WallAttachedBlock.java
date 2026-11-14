package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.WallAttachedBlockBehavior;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WallAttachedBlock extends DirectionalBlock implements WallAttachedBlockBehavior {
    public WallAttachedBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        if (superState == null) return null;

        Direction facing = IBlock.getStatePropertySafe(superState, FACING);
        boolean isAttached = WallAttachedBlockBehavior.canBePlaced(ctx.getClickedPos(), (Level)ctx.getLevel(), Direction.from2DDataValue(ctx.getClickedFace().get2DDataValue()).getOpposite());
        if(!isAttached) return null;

        return superState.setValue(FACING, Direction.from2DDataValue(ctx.getClickedFace().get2DDataValue()).getOpposite());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        if (!WallAttachedBlockBehavior.canBePlaced(pos, (Level)world, getWallDirection(facing))) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }
}
