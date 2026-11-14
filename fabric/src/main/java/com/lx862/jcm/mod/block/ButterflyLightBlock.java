package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
import com.lx862.jcm.mod.network.gui.ButterflyLightGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import net.minecraft.world.level.block.EntityBlock;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ButterflyLightBlock extends DirectionalBlock implements EntityBlock {

    public ButterflyLightBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return IBlock.getVoxelShapeByDirection(2, 0, 0, 14, 5.85, 10, IBlock.getStatePropertySafe(state, FACING));
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            BlockEntity be = world.getBlockEntity(pos);

            ButterflyLightBlockEntity thisEntity = (ButterflyLightBlockEntity)be;
            Networking.sendPacketToClient(player, new ButterflyLightGUIPacket(pos, thisEntity.getStartBlinkingSeconds()));
        });
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new ButterflyLightBlockEntity(blockPos, blockState);
    }
}
