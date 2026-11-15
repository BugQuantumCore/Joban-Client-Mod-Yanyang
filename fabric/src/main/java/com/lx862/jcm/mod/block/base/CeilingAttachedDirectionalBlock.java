package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.HolderBase;

import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class CeilingAttachedDirectionalBlock extends CeilingAttachedBlock {

    // EnumProperty<Direction> works on 1.21.1 (DirectionProperty subclass)
    // and 1.21.4 (DirectionProperty removed) alike.
    public static final EnumProperty<Direction> FACING = BlockProperties.FACING;

    public CeilingAttachedDirectionalBlock(BlockBehaviour.Properties settings, boolean enforceLogicalPattern) {
        super(settings, enforceLogicalPattern);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        return superState == null ? null : superState.setValue(FACING, ctx.getNearestLookingDirection());
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(FACING);
    }
}
