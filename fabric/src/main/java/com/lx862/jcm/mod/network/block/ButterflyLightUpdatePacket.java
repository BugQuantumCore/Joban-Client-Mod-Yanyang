package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.ButterflyLightBlockEntity;
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

public class ButterflyLightUpdatePacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int startBlinkingSeconds;

    public ButterflyLightUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        this.startBlinkingSeconds = packetBufferReceiver.readInt();
    }

    public ButterflyLightUpdatePacket(BlockPos blockPos, int startBlinkingSeconds) {
        this.blockPos = blockPos;
        this.startBlinkingSeconds = startBlinkingSeconds;
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayer serverPlayerEntity) {
        Level world = serverPlayerEntity.level();
        BlockState state = BlockUtil.getBlockState(world, blockPos);
        if(state == null || !(state.getBlock() instanceof JCMBlock)) return;
        if(!PacketValidator.canConfigureBlock(serverPlayerEntity)) {
            JCMLogger.warn("Player {} attempted to configure Butterfly Light at {} {} {} without permission.", serverPlayerEntity.getGameProfile().getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            return;
        }

        ((JCMBlock)state.getBlock()).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be instanceof ButterflyLightBlockEntity) {
                ((ButterflyLightBlockEntity)be).setData(startBlinkingSeconds);
            }
        });
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(startBlinkingSeconds);
    }
}
