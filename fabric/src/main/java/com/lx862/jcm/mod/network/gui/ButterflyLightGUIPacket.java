package com.lx862.jcm.mod.network.gui;

import net.minecraft.core.BlockPos;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;

public class ButterflyLightGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final int secondsToBlink;

    public ButterflyLightGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        this.secondsToBlink = packetBufferReceiver.readInt();
    }

    public ButterflyLightGUIPacket(BlockPos blockPos, int secondsToBlink) {
        this.blockPos = blockPos;
        this.secondsToBlink = secondsToBlink;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(secondsToBlink);
    }

    @Override
    public void runClient() {
        ClientHelper.openButterflyLightScreen(blockPos, secondsToBlink);
    }
}
