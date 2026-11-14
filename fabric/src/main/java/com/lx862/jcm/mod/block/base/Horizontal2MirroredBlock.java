package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.util.BlockUtil;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cheap way for a horizontal multi block, with 2 same block facing each other.
 */
public abstract class Horizontal2MirroredBlock extends DirectionalBlock {
    public Horizontal2MirroredBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction placeDirection = ctx.getNearestLookingDirection().getOpposite();
        return BlockUtil.isReplacable(ctx.getLevel(), ctx.getClickedPos(), placeDirection, ctx, 2) && super.getStateForPlacement(ctx) != null ? super.getStateForPlacement(ctx).setValue(FACING, placeDirection) : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if(world.isClientSide()) return;

        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        world.setBlock(pos.relative(facing), state.setValue(FACING, facing.getOpposite()), 3, 3);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos breakPos, BlockState breakState, Player player) {
        breakWithoutDropIfCreative(world, breakPos, breakState, player, this, this::getLootDropPos);
        return super.playerWillDestroy(world, breakPos, breakState, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        if(IBlock.getStatePropertySafe(state, FACING) == direction && !(neighborState.is(this))) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, LevelAccessor world, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        BlockPos otherPos = pos.relative(facing);
        return new BlockPos[]{ pos, otherPos };
    }

    public BlockPos getLootDropPos(BlockState state, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        if(facing == Direction.EAST || facing == Direction.NORTH) return pos;
        return pos.relative(facing);
    }
}