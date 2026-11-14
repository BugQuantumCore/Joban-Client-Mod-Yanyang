package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.SoundLooperBlockEntity;
import com.lx862.jcm.mod.network.gui.SoundLooperGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import net.minecraft.world.level.block.EntityBlock;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class SoundLooperBlock extends JCMBlock implements EntityBlock {
    public SoundLooperBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new SoundLooperBlockEntity(blockPos, blockState);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            SoundLooperBlockEntity be = (SoundLooperBlockEntity) world.getBlockEntity(pos);
            Networking.sendPacketToClient(player, new SoundLooperGUIPacket(pos, be.getCorner1(), be.getCorner2(), be.getSoundId(), be.getSoundCategory(), be.getLoopInterval(), be.getSoundVolume(), be.needRedstone(), be.rangeLimited()));
        });
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return type == com.lx862.jcm.mod.registry.BlockEntities.SOUND_LOOPER.get() ? (level1, pos, state1, blockEntity) -> ((com.lx862.jcm.mod.block.entity.JCMBlockEntityBase) blockEntity).tick() : null;
    }
}
