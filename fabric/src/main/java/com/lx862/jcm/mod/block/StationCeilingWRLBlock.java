package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectionalBlock;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StationCeilingWRLBlock extends CeilingAttachedDirectionalBlock {

    public StationCeilingWRLBlock(BlockBehaviour.Properties settings) {
        super(settings, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        VoxelShape ceiling = IBlock.getVoxelShapeByDirection(0.5, 8, 1, 15.5, 9, 15, IBlock.getStatePropertySafe(state, FACING));
        VoxelShape pole = IBlock.getVoxelShapeByDirection(7.5, 9, 7.5, 8.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
        return Shapes.or(ceiling, pole);
    }
}
