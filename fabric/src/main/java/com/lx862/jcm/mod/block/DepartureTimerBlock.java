package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.block.entity.DepartureTimerBlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DepartureTimerBlock extends DirectionalBlock implements EntityBlock {

    public DepartureTimerBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(2.7, 0, 0, 13.3, 10.7, 13, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new DepartureTimerBlockEntity(blockPos, blockState);
    }
}
