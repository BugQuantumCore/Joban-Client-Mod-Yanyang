package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectional2Block;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ExitSignEven extends CeilingAttachedDirectional2Block {

    public ExitSignEven(BlockBehaviour.Properties settings) {
        super(settings, true, false, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            return IBlock.getVoxelShapeByDirection(8, 9, 7.9, 16, 16, 8.1, IBlock.getStatePropertySafe(state, FACING));
        } else {
            return IBlock.getVoxelShapeByDirection(0, 9, 7.9, 8, 16, 8.1, IBlock.getStatePropertySafe(state, FACING));
        }
    }
}
