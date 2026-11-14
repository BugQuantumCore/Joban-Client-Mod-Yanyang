package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.OperatorButtonBlock;
import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.OperatorButtonBlockEntity;
import com.lx862.jcm.mod.network.PacketValidator;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class OperatorButtonUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final boolean[] keyRequirements;

    public OperatorButtonUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        this.keyRequirements = new boolean[OperatorButtonBlock.ACCEPTED_KEYS.length];
        for (int i = 0; i < OperatorButtonBlock.ACCEPTED_KEYS.length; i++) {
            keyRequirements[i] = packetBufferReceiver.readBoolean();
        }
    }

    public OperatorButtonUpdatePacket(BlockPos blockPos, boolean[] keyRequirements) {
        this.blockPos = blockPos;
        this.keyRequirements = keyRequirements;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        for (boolean keyAccepted : keyRequirements) {
            packetBufferSender.writeBoolean(keyAccepted);
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayer serverPlayerEntity) {
        Level world = serverPlayerEntity.level();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock() instanceof JCMBlock)) return;
        if(!PacketValidator.canConfigureBlock(serverPlayerEntity)) {
            JCMLogger.warn("Player {} attempted to configure Operator Button at {} {} {} without permission.", serverPlayerEntity.getGameProfile().getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            return;
        }

        ((JCMBlock)state.getBlock()).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be instanceof OperatorButtonBlockEntity) {
                ((OperatorButtonBlockEntity)be).setData(keyRequirements);
            }
        });
    }
}
