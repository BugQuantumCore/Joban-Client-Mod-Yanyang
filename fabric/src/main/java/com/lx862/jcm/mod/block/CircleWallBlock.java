package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.registry.Blocks;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CircleWallBlock extends DirectionalBlock {
    public CircleWallBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState superState = super.getStateForPlacement(ctx);

        if(superState != null && ctx.getPlayer() != null && autoPlace(ctx.getLevel(), ctx.getClickedPos(), superState, ctx.getNearestLookingDirection())) {
            ctx.getPlayer().swing(ctx.getHand(), !ctx.getLevel().isClientSide());
            return null;
        } else {
            return superState;
        }
    }

    // Why does light still pass through aaaaa
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 15, IBlock.getStatePropertySafe(state, FACING));
    }

    /**
     * Automatically place tunnel walls in a shifted block position to provide easier placement
     * @return True if the block is successfully placed in the shifted position, False otherwise
     */
    private boolean autoPlace(Level world, BlockPos pos, BlockState state, Direction playerFacing) {
        Block thisBlock = this.asBlock();
        Block blockBelow = world.getBlockState(pos.below()).getBlock();
        BlockPos shiftedBlockPos = null;

        if(blockBelow.equals(com.lx862.jcm.mod.registry.Blocks.CIRCLE_WALL_1.get()) && thisBlock.equals(com.lx862.jcm.mod.registry.Blocks.CIRCLE_WALL_2.get())) {
            shiftedBlockPos = pos.relative(playerFacing);
        }

        if(blockBelow.equals(com.lx862.jcm.mod.registry.Blocks.CIRCLE_WALL_4.get()) && thisBlock.equals(com.lx862.jcm.mod.registry.Blocks.CIRCLE_WALL_5.get()) ||
           blockBelow.equals(com.lx862.jcm.mod.registry.Blocks.CIRCLE_WALL_5.get()) && thisBlock.equals(com.lx862.jcm.mod.registry.Blocks.CIRCLE_WALL_6.get())
        ) {
            shiftedBlockPos = pos.relative(playerFacing.getOpposite());
        }

        if(shiftedBlockPos != null && world.getBlockState(shiftedBlockPos).isAir()) {
            world.setBlock(shiftedBlockPos, state, 3);
            return true;
        } else {
            return false;
        }
    }
}
