package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2Block;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MTRTrainModelBlock extends Horizontal2Block {
    public MTRTrainModelBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 3.5, 16, 9, 12.5, IBlock.getStatePropertySafe(state, FACING));
    }
}
