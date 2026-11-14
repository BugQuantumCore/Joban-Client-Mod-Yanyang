package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import net.minecraft.world.level.block.SlabBlock;
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
import net.minecraft.world.level.block.state.properties.SlabType;

public abstract class SlabExtendableBlock extends DirectionalBlock {
    public static final BooleanProperty IS_SLAB = BlockProperties.IS_SLAB;

    public SlabExtendableBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(IS_SLAB, false));
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(IS_SLAB);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if(state == null) return null;

        return state.setValue(IS_SLAB, shouldExtendForSlab((LevelAccessor)ctx.getLevel(), ctx.getClickedPos().above(), SlabType.TOP));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos).setValue(IS_SLAB, shouldExtendForSlab(world, pos.above(), SlabType.TOP));
    }

    public static boolean shouldExtendForSlab(LevelAccessor world, BlockPos pos, SlabType slabType) {
        BlockState blockTop = world.getBlockState(pos);

        try {
            return blockTop.getValue(SlabBlock.TYPE) == slabType;
        } catch (IllegalArgumentException ignored) { // Likely no slab property
            return false;
        }
    }
}
