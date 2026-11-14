package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LRTInterCarBarrierBlock extends DirectionalBlock {

    public LRTInterCarBarrierBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 16, 16, Direction.NORTH);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(0, 0, 0, 16, 24, 16, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter view, BlockPos pos) {
        return Shapes.empty();
    }
}
