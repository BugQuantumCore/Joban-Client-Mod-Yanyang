package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LRTTrespassSignageBlock extends Vertical2Block {

    public LRTTrespassSignageBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        switch (IBlock.getStatePropertySafe(state, HALF)) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(7.5, 0, 7, 8.5, 16, 8, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                VoxelShape vx1R = IBlock.getVoxelShapeByDirection(7.5, 0, 7, 8.5, 11, 8, IBlock.getStatePropertySafe(state, FACING));
                VoxelShape vx2R = IBlock.getVoxelShapeByDirection(5.5, 2, 7, 10.5, 10, 8, IBlock.getStatePropertySafe(state, FACING));
                return Shapes.or(vx1R, vx2R);
            default:
                return Shapes.empty();
        }
    }
}