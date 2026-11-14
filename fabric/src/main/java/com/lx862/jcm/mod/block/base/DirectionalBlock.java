package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.HolderBase;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class DirectionalBlock extends JCMBlock {
    public static final DirectionProperty FACING = BlockProperties.FACING;

    public DirectionalBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        if(superState == null) return null;
        return superState.setValue(FACING, Direction.from2DDataValue(ctx.getNearestLookingDirection().get2DDataValue()));
    }
}
