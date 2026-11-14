package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2Block;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MTRTrespassSignageBlock extends Horizontal2Block {

    public MTRTrespassSignageBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        if(IBlock.getStatePropertySafe(state, IS_LEFT)) {
            VoxelShape vx1 = IBlock.getVoxelShapeByDirection(2, 4, 7.5, 16, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape vx2 = IBlock.getVoxelShapeByDirection(7, 0, 7.5, 8, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return Shapes.or(vx1, vx2);
        } else {
            VoxelShape vx1R = IBlock.getVoxelShapeByDirection(0, 4, 7.5, 14, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            VoxelShape vx2R = IBlock.getVoxelShapeByDirection(8, 0, 7.5, 9, 16, 8.5, IBlock.getStatePropertySafe(state, FACING));
            return Shapes.or(vx1R, vx2R);
        }
    }
}
