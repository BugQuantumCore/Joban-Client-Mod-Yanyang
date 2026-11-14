package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.network.gui.PIDSProjectorGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import net.minecraft.world.level.block.EntityBlock;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PIDSProjectorBlock extends DirectionalBlock implements EntityBlock {

    public PIDSProjectorBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return Shapes.box(0, 0, 0, 1, 1, 1);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter view, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }


    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PIDSProjectorBlockEntity(blockPos, blockState);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            PIDSProjectorBlockEntity be = (PIDSProjectorBlockEntity) world.getBlockEntity(pos);
            Networking.sendPacketToClient(player, new PIDSProjectorGUIPacket(pos, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden(), be.getPresetId(), be.getOffsetX(), be.getOffsetY(), be.getOffsetZ(), be.getRotateX(), be.getRotateY(), be.getRotateZ(), be.getScale()));
        });
    }
}