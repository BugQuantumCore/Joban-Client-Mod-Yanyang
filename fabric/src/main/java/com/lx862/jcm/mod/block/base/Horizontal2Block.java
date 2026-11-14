package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.HorizontalDoubleBlockBehavior;
import net.minecraft.world.level.block.state.properties.Property;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class Horizontal2Block extends DirectionalBlock implements HorizontalDoubleBlockBehavior {
    public Horizontal2Block(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return HorizontalDoubleBlockBehavior.canBePlaced(ctx) ? super.getStateForPlacement(ctx) : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        HorizontalDoubleBlockBehavior.onPlaced(world, state, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos breakPos, BlockState breakState, Player player) {
        breakWithoutDropIfCreative(world, breakPos, breakState, player, this, HorizontalDoubleBlockBehavior::getLootDropPos);
        return super.playerWillDestroy(world, breakPos, breakState, player);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);
        if(!HorizontalDoubleBlockBehavior.blockIsValid(pos, state, direction, world, isLeft)) return Blocks.AIR.defaultBlockState();

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        HorizontalDoubleBlockBehavior.addProperties(properties);
    }

    @Override
    public BlockPos[] getAllPos(BlockState state, LevelAccessor world, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        boolean isLeft = IBlock.getStatePropertySafe(state, IS_LEFT);

        BlockPos secondaryPos = isLeft ? pos.relative(facing.getClockWise()) : pos.relative(facing.getCounterClockWise());
        return new BlockPos[]{ pos, secondaryPos };
    }
}
