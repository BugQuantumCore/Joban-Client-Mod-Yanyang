package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.HorizontalWallAttached2Block;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KCRTrespassSignageBlock extends HorizontalWallAttached2Block {

    public KCRTrespassSignageBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            return IBlock.getVoxelShapeByDirection(4, 2, 0, 16, 14, 0.1, IBlock.getStatePropertySafe(state, FACING));
        } else {
            return IBlock.getVoxelShapeByDirection(0, 2, 0, 12, 14, 0.1, IBlock.getStatePropertySafe(state, FACING));
        }
    }
}
