package com.lx862.jcm.mod.block.base;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class CeilingAttachedBlock extends JCMBlock {
    protected final boolean enforceLogicalPattern;

    public CeilingAttachedBlock(BlockBehaviour.Properties settings, boolean enforceLogicalPattern) {
        super(settings);
        this.enforceLogicalPattern = enforceLogicalPattern;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);

        if(enforceLogicalPattern) {
            BlockState blockAbove = ctx.getLevel().getBlockState(ctx.getClickedPos().above());
            if(superState == null) return null;
            if(!com.lx862.jcm.mod.block.behavior.VerticallyAttachedBlock.canPlace(true, false, ctx)) return null;
        }

        return superState;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        BlockState attachedBlock = world.getBlockState(pos.above());

        if (enforceLogicalPattern && attachedBlock.isAir()) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }
}
