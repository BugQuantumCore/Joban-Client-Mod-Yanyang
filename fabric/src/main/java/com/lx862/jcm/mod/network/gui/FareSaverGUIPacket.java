package com.lx862.jcm.mod.network.gui;

import net.minecraft.core.BlockPos;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;

public class FareSaverGUIPacket extends PacketHandler {
    private final BlockPos blockPos;
    private final String prefix;
    private final int discount;

    public FareSaverGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        this.prefix = packetBufferReceiver.readString();
        this.discount = packetBufferReceiver.readInt();
    }

    public FareSaverGUIPacket(BlockPos blockPos, String prefix, int discount) {
        this.blockPos = blockPos;
        this.prefix = prefix;
        this.discount = discount;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeString(prefix);
        packetBufferSender.writeInt(discount);
    }

    @Override
    public void runClient() {
        ClientHelper.openFareSaverGUIScreen(blockPos, prefix, discount);
    }
}
