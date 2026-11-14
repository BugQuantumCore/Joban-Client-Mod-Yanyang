package com.lx862.jcm.mod.block;

import com.lx862.jcm.mod.block.base.Horizontal2MirroredBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.network.gui.PIDSGUIPacket;
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

public abstract class JCMPIDSBlock extends Horizontal2MirroredBlock implements EntityBlock {
    public JCMPIDSBlock(BlockBehaviour.Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            PIDSBlockEntity be = (PIDSBlockEntity) world.getBlockEntity(pos);
            Networking.sendPacketToClient(player, new PIDSGUIPacket(pos, be.getCustomMessages(), be.getRowHidden(), be.platformNumberHidden(), be.getPresetId()));
        });
    }
}
