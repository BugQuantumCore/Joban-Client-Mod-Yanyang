package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.WallAttachedBlock;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class KCREnquiryMachineWall extends WallAttachedBlock implements EnquiryMachineBehavior {
    public KCREnquiryMachineWall(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(3.5, 2.5, 0, 12.5, 13.5, 1, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(!world.isClientSide()) enquiry(EnquiryScreenType.NONE, pos, world, player);
        return InteractionResult.SUCCESS;
    }
}
