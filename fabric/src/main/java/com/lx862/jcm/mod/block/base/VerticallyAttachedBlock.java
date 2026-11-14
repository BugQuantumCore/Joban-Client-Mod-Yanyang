package com.lx862.jcm.mod.block.base;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.HolderBase;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class VerticallyAttachedBlock extends JCMBlock {
    public static final BooleanProperty TOP = BlockProperties.TOP;

    public VerticallyAttachedBlock(BlockBehaviour.Properties settings) {
        super(settings);
        registerDefaultState(defaultBlockState().setValue(TOP, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);
        BlockState blockAbove = ctx.getLevel().getBlockState(ctx.getClickedPos().above());
        if(superState == null) return null;
        boolean canAttachTop = com.lx862.jcm.mod.block.behavior.VerticallyAttachedBlock.canPlace(true, false, ctx);
        boolean canAttachBottom = com.lx862.jcm.mod.block.behavior.VerticallyAttachedBlock.canPlace(false, true, ctx);
        if(!canAttachTop && !canAttachBottom) return null;

        boolean shouldAttachTop = !blockAbove.isAir() && !(ctx.getClickedFace().equals(Direction.UP) && canAttachBottom);
        return superState.setValue(TOP, shouldAttachTop);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        BlockState attachedBlock = state.getValue(TOP) ? world.getBlockState(pos.above()) : world.getBlockState(pos.below());

        if (shouldBreakOnBlockUpdate() && attachedBlock.isAir()) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void addBlockProperties(List<Property<?>> properties) {
        super.addBlockProperties(properties);
        properties.add(TOP);
    }

    protected boolean shouldBreakOnBlockUpdate() {
        return true;
    }
}
