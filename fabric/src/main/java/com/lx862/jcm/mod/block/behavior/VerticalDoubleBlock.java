package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public interface VerticalDoubleBlock {
    EnumProperty<IBlock.DoubleBlockHalf> HALF = BlockProperties.VERTICAL_2;
    static boolean canBePlaced(Level world, BlockPos pos, BlockPlaceContext ctx) {
        return BlockUtil.isReplacable(world, pos, Direction.UP, ctx, 2);
    }

    static void placeBlock(Level world, BlockPos startPos, BlockState state) {
        world.setBlock(startPos.above(), state.setValue(HALF, IBlock.DoubleBlockHalf.UPPER), 3);
    }

    static BlockPos getLootDropPos(BlockState state, BlockPos pos) {
        switch(IBlock.getStatePropertySafe(state, HALF)) {
            case LOWER: return pos;
            case UPPER: return pos.below();
            default: return pos;
        }
    }

    static boolean blockNotValid(Block blockInstance, LevelAccessor world, BlockPos pos, BlockState state) {
        IBlock.DoubleBlockHalf thisPart = IBlock.getStatePropertySafe(state, HALF);
        int offset = thisPart == IBlock.DoubleBlockHalf.UPPER ? -1 : 1;
        return !world.getBlockState(pos.relative(Axis.Y, offset)).is(blockInstance);
    }
}
