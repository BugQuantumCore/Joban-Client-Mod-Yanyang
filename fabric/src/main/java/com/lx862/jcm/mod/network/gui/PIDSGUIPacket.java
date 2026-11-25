package com.lx862.jcm.mod.network.gui;

import com.lx862.jcm.mod.network.JCMPacketHandlerHelper;
import net.minecraft.core.BlockPos;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;

public class PIDSGUIPacket extends PacketHandler {
    /**
     * Upper bound for the client-controlled row count, mirroring
     * PIDSUpdatePacket's MAX_ROWS clamp: an untrusted count would allocate
     * {@code rows * 16} bytes (OutOfMemoryError) or a negative one throw
     * NegativeArraySizeException inside the client packet pipeline.
     */
    private static final int MAX_ROWS = 32;
    protected final BlockPos blockPos;
    protected final String[] customMessages;
    protected final boolean[] rowHidden;
    protected final boolean hidePlatformNumber;
    protected final String presetId;

    public PIDSGUIPacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        int rowsRead = packetBufferReceiver.readInt();
        if(rowsRead < 0 || rowsRead > MAX_ROWS) rowsRead = 0;
        /* Effectively final for the lambda captures below. */
        final int rows = rowsRead;
        this.customMessages = new String[rows];
        this.rowHidden = new boolean[rows];
        /* Guarded callbacks: the per-array size headers are read from the same
           untrusted buffer and may disagree with the clamped row count —
           out-of-range indices are consumed and dropped instead of throwing
           AIOOBE. */
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> {
            final String message = packetBufferReceiver.readString();
            if(i >= 0 && i < rows) this.customMessages[i] = message;
        });
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> {
            final boolean hidden = packetBufferReceiver.readBoolean();
            if(i >= 0 && i < rows) this.rowHidden[i] = hidden;
        });
        this.hidePlatformNumber = packetBufferReceiver.readBoolean();
        this.presetId = packetBufferReceiver.readString();
    }

    public PIDSGUIPacket(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        this.blockPos = blockPos;
        this.customMessages = customMessages;
        this.rowHidden = rowHidden;
        this.hidePlatformNumber = hidePlatformNumber;
        this.presetId = presetId;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(customMessages.length);
        JCMPacketHandlerHelper.writeArray(packetBufferSender, customMessages, i -> {
            String str = customMessages[i] == null ? "" : customMessages[i];
            packetBufferSender.writeString(str);
        });
        JCMPacketHandlerHelper.writeArray(packetBufferSender, rowHidden, i -> packetBufferSender.writeBoolean(rowHidden[i]));
        packetBufferSender.writeBoolean(hidePlatformNumber);
        packetBufferSender.writeString(presetId);
    }

    @Override
    public void runClient() {
        ClientHelper.openPIDSGUIScreen(blockPos, customMessages, rowHidden, hidePlatformNumber, presetId);
    }
}
