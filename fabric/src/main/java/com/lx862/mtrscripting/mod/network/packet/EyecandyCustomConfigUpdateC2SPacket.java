package com.lx862.mtrscripting.mod.network.packet;

import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.JCMBlockEyecandyExtra;
import com.lx862.jcm.mod.network.PacketValidator;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import org.mtr.block.BlockEyeCandy;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EyecandyCustomConfigUpdateC2SPacket extends PacketHandler {
    private final Map<String, String> entries;
    private final BlockPos blockPos;
    private final String modelId;

    public EyecandyCustomConfigUpdateC2SPacket(PacketBufferReceiver packetBufferReceiver) {
        this.entries = new HashMap<>();
        this.modelId = packetBufferReceiver.readString();
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        int size = packetBufferReceiver.readInt();

        for(int i = 0; i < size; i++) {
            String key = packetBufferReceiver.readString();
            String value = packetBufferReceiver.readString();
            entries.put(key, value);
        }
    }

    public EyecandyCustomConfigUpdateC2SPacket(String modelId, BlockPos blockPos, Map<String, String> entries) {
        this.modelId = modelId;
        this.blockPos = blockPos;
        this.entries = new HashMap<>(entries);
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeString(modelId);
        packetBufferSender.writeLong(blockPos.asLong());

        int size = entries.size();
        packetBufferSender.writeInt(size);

        for(Map.Entry<String, String> entry : entries.entrySet()) {
            packetBufferSender.writeString(entry.getKey());
            packetBufferSender.writeString(entry.getValue());
        }
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayer serverPlayerEntity) {
        Level world = serverPlayerEntity.level();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);
        if(be == null || !(be instanceof BlockEyeCandy.EyeCandyBlockEntity)) return;

        if(!PacketValidator.canConfigureBlock(serverPlayerEntity)) {
            JCMLogger.warn("Player {} attempted to write Eyecandy custom config at {} {} {} without permission.", serverPlayerEntity.getGameProfile().getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            return;
        }

        BlockEyeCandy.EyeCandyBlockEntity eyecandyBE = (BlockEyeCandy.EyeCandyBlockEntity)be;
        ((JCMBlockEyecandyExtra)eyecandyBE).jsblock$updateCustomConfig(this.entries);
    }
}
