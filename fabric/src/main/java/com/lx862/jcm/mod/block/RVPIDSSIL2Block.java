package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.entity.RVPIDSSIL2BlockEntity;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class RVPIDSSIL2Block extends JCMPIDSBlock {

    public RVPIDSSIL2Block(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        VoxelShape vx1 = IBlock.getVoxelShapeByDirection(0, -2, 0, 16, 9, 16, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape vx2 = IBlock.getVoxelShapeByDirection(7.5, 9, 8.5, 8.5, 16, 9.5, IBlock.getStatePropertySafe(state, FACING));
        return Shapes.or(vx1, vx2);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new RVPIDSSIL2BlockEntity(blockPos, blockState);
    }
}