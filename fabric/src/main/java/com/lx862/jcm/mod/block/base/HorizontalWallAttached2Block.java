package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalDoubleBlockBehavior;
import com.lx862.jcm.mod.block.behavior.WallAttachedBlockBehavior;
import net.minecraft.world.level.block.state.properties.Property;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class HorizontalWallAttached2Block extends DirectionalBlock implements HorizontalDoubleBlockBehavior {
    public HorizontalWallAttached2Block(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        if(superState == null) return null;

        boolean firstBlockAttachable = WallAttachedBlockBehavior.canBePlaced(ctx.getClickedPos(), ctx.getLevel(), IBlock.getStatePropertySafe(superState, FACING));
        boolean secondBlockAttachable = WallAttachedBlockBehavior.canBePlaced(ctx.getClickedPos().relative(IBlock.getStatePropertySafe(superState, FACING).getClockWise()), ctx.getLevel(), IBlock.getStatePropertySafe(superState, FACING));

        return firstBlockAttachable && secondBlockAttachable && HorizontalDoubleBlockBehavior.canBePlaced(ctx) ? super.getStateForPlacement(ctx) : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalDoubleBlockBehavior.onPlaced(world, state, pos);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);

        boolean blockAttachable = WallAttachedBlockBehavior.canBePlaced(pos, (Level)world, IBlock.getStatePropertySafe(state, FACING));

        if(!HorizontalDoubleBlockBehavior.blockIsValid(pos, state, direction, world, isLeft) || !blockAttachable) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, LevelAccessor world, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        BlockPos otherPos = pos.relative(facing);
        return new BlockPos[]{ pos, otherPos };
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        HorizontalDoubleBlockBehavior.addProperties(properties);
    }
}
