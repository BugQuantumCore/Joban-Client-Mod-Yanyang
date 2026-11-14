package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.CeilingAttachedDirectionalBlock;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ExitSignOdd extends CeilingAttachedDirectionalBlock {

    public ExitSignOdd(BlockBehaviour.Properties settings) {
        super(settings, true);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(0, 9, 7.9, 16, 16, 8.1, IBlock.getStatePropertySafe(state, FACING));
    }
}
