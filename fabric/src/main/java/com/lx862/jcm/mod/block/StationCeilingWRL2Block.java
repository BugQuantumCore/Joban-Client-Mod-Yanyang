package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectional2Block;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class StationCeilingWRL2Block extends CeilingAttachedDirectional2Block {

    public StationCeilingWRL2Block(BlockBehaviour.Properties settings) {
        super(settings, true, false, false);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            VoxelShape ceiling = IBlock.getVoxelShapeByDirection(0.5, 8, 1, 16, 9, 15, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape pole = IBlock.getVoxelShapeByDirection(5.5, 9, 7.5, 6.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return Shapes.or(ceiling, pole);
        } else {
            VoxelShape ceilingR = IBlock.getVoxelShapeByDirection(0, 8, 1, 15.5, 9, 15, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape poleR = IBlock.getVoxelShapeByDirection(10.5, 9, 7.5, 11.5, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return Shapes.or(ceilingR, poleR);
        }
    }
}
