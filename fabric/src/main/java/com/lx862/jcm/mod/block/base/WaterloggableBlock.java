/*package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.block.behavior.WaterloggableBehavior;
import com.lx862.jcm.mod.render.HolderBase;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockBehaviour;
*/
/**
 * A waterlogged block
 * Cannot find ways to migrate non-waterlogged block yet, so this is not in use.
 * (WATERLOGGED is a BooleanProperty, by default Minecraft will use the first value as it's default value, and somehow "true" comes first)
 */

/*
public abstract class WaterloggableBlock extends JCMBlock implements WaterloggableBehavior {
    public WaterloggableBlock(BlockSettings settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(WATERLOGGED, false));
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(WATERLOGGED);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return getFluidState(state);
    }

    @Override
    public BlockState getStateForPlacement(ItemPlacementContext ctx) {
        return getWaterloggedState(super.getPlacementState2(ctx), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        updateWaterState(state, world, pos);
        return super.getStateForNeighborUpdate2(state, direction, neighborState, world, pos, neighborPos);
    }
}
*/