package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Vertical2Block;
import com.lx862.jcm.mod.block.behavior.EnquiryMachineBehavior;
import com.lx862.jcm.mod.data.EnquiryScreenType;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MTREnquiryMachine extends Vertical2Block implements EnquiryMachineBehavior {
    public MTREnquiryMachine(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        switch(IBlock.getStatePropertySafe(state, HALF)) {
            case LOWER:
                return IBlock.getVoxelShapeByDirection(4, 0, 4.5, 12, 15, 11.5, IBlock.getStatePropertySafe(state, FACING));
            case UPPER:
                VoxelShape vx1 = IBlock.getVoxelShapeByDirection(4, 0, 4.5, 12, 5.6, 11.5, IBlock.getStatePropertySafe(state, FACING));
                VoxelShape vx2 = IBlock.getVoxelShapeByDirection(4, 5.6569, 5.85, 12, 10.1519, 5.95, IBlock.getStatePropertySafe(state, FACING));
                return Shapes.or(vx1, vx2);
            default:
                return Shapes.empty();
        }
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(!world.isClientSide()) enquiry(EnquiryScreenType.CLASSIC, pos, world, player);
        return InteractionResult.SUCCESS;
    }
}
