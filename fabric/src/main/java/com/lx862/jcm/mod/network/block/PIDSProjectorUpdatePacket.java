package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.network.PacketValidator;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PIDSProjectorUpdatePacket extends PIDSUpdatePacket {
    private final double x;
    private final double y;
    private final double z;
    private final double rotateX;
    private final double rotateY;
    private final double rotateZ;
    private final double scale;

    public PIDSProjectorUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        super(packetBufferReceiver);
        this.x = packetBufferReceiver.readDouble();
        this.y = packetBufferReceiver.readDouble();
        this.z = packetBufferReceiver.readDouble();
        this.rotateX = packetBufferReceiver.readDouble();
        this.rotateY = packetBufferReceiver.readDouble();
        this.rotateZ = packetBufferReceiver.readDouble();
        this.scale = packetBufferReceiver.readDouble();
    }

    public PIDSProjectorUpdatePacket(BlockPos blockPos, LongAVLTreeSet filteredPlatforms, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPreset, double x, double y, double z, double rotateX, double rotateY, double rotateZ, double scale) {
        super(blockPos, filteredPlatforms, customMessages, rowHidden, hidePlatformNumber, pidsPreset);
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotateX = rotateX;
        this.rotateY = rotateY;
        this.rotateZ = rotateZ;
        this.scale = scale;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        super.write(packetBufferSender);
        packetBufferSender.writeDouble(x);
        packetBufferSender.writeDouble(y);
        packetBufferSender.writeDouble(z);
        packetBufferSender.writeDouble(rotateX);
        packetBufferSender.writeDouble(rotateY);
        packetBufferSender.writeDouble(rotateZ);
        packetBufferSender.writeDouble(scale);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayer serverPlayerEntity) {
        Level world = serverPlayerEntity.level();
        BlockEntity be = BlockUtil.getBlockEntityOrNull(world, blockPos);

        if(be != null && be instanceof PIDSProjectorBlockEntity) {
            if(!PacketValidator.canConfigureBlock(serverPlayerEntity)) {
                JCMLogger.warn("Player {} attempted to configure PIDS Projector at {} {} {} without permission.", serverPlayerEntity.getGameProfile().getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
                return;
            }

            ((PIDSProjectorBlockEntity)be).setData(customMessages, filteredPlatforms, rowHidden, hidePlatformNumber, presetId, x, y, z, rotateX, rotateY, rotateZ, scale);
        }
    }
}