package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.render.HolderBase;
import org.mtr.block.IBlock;

import java.util.List;

import static com.lx862.jcm.mod.block.base.DirectionalBlock.FACING;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public interface HorizontalDoubleBlockBehavior {
    BooleanProperty IS_LEFT = BlockProperties.HORIZONTAL_IS_LEFT;
    static boolean canBePlaced(BlockPlaceContext ctx) {
        return BlockUtil.isReplacable(ctx.getLevel(), ctx.getClickedPos(), ctx.getNearestLookingDirection().getClockWise(), ctx, 2);
    }

    static void placeBlock(Level world, BlockPos pos, BlockState state, BooleanProperty isLeft, Direction directionToPlace, int length) {
        world.setBlock(pos.relative(directionToPlace), state.setValue(isLeft, false), 3);
    }

    static boolean blockIsValid(BlockPos pos, BlockState state, Direction updateFrom, LevelAccessor world, boolean thisBlockIsLeftPart) {
        Direction dir = IBlock.getStatePropertySafe(state, FACING);
        Direction checkDir = thisBlockIsLeftPart ? dir.getClockWise() : dir.getCounterClockWise();
        BlockPos neighbourPos = pos.relative(checkDir);
        BlockState neighbourState = world.getBlockState(neighbourPos);
        return !checkDir.equals(updateFrom) || neighbourState.is(state.getBlock());
    }

    static void onPlaced(Level world, BlockState state, BlockPos placedPos) {
        world.setBlock(placedPos.relative(IBlock.getStatePropertySafe(state, FACING).getClockWise()), state.setValue(IS_LEFT, false), 3);
    }

    static BlockPos getLootDropPos(BlockState state, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);
        return isLeft ? pos : pos.relative(facing.getCounterClockWise());
    }

    static void addProperties(List<Property<?>> properties) {
        properties.add(IS_LEFT);
    }
}
