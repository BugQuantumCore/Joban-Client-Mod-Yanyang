package com.lx862.jcm.mod.network.block;

import com.lx862.jcm.mod.block.base.JCMBlock;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.network.JCMPacketHandlerHelper;
import com.lx862.jcm.mod.network.PacketValidator;
import com.lx862.jcm.mod.util.BlockUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.packet.PacketHandler;
import org.mtr.packet.PacketBufferReceiver;
import org.mtr.packet.PacketBufferSender;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PIDSUpdatePacket extends PacketHandler {
    /**
     * Upper bound for the client-controlled row count. The biggest PIDS in the
     * mod has far fewer rows; anything beyond this is a corrupted or hostile
     * packet (a huge count would allocate ~rows*16 bytes and crash the server
     * with OutOfMemoryError, a negative one NegativeArraySizeException).
     */
    private static final int MAX_ROWS = 32;
    protected final BlockPos blockPos;
    protected final LongAVLTreeSet filteredPlatforms;
    protected final String[] customMessages;
    protected final boolean[] rowHidden;
    protected final boolean hidePlatformNumber;
    protected final String presetId;

    public PIDSUpdatePacket(PacketBufferReceiver packetBufferReceiver) {
        this.blockPos = BlockPos.of(packetBufferReceiver.readLong());
        int rowsRead = packetBufferReceiver.readInt();
        if(rowsRead < 0 || rowsRead > MAX_ROWS) rowsRead = 0;
        /* Effectively final for the lambda captures below. */
        final int rows = rowsRead;
        this.customMessages = new String[rows];
        this.rowHidden = new boolean[rows];
        this.filteredPlatforms = new LongAVLTreeSet();

        /* Guarded callbacks: the per-array size headers are read from the same
           untrusted buffer and may disagree with the clamped row count —
           out-of-range indices are consumed and dropped instead of throwing
           AIOOBE inside the server packet pipeline. */
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> {
            final String message = packetBufferReceiver.readString();
            if(i >= 0 && i < rows) this.customMessages[i] = message;
        });
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> {
            final boolean hidden = packetBufferReceiver.readBoolean();
            if(i >= 0 && i < rows) this.rowHidden[i] = hidden;
        });
        JCMPacketHandlerHelper.readArray(packetBufferReceiver, i -> filteredPlatforms.add(packetBufferReceiver.readLong()));

        this.hidePlatformNumber = packetBufferReceiver.readBoolean();
        this.presetId = packetBufferReceiver.readString();
    }

    public PIDSUpdatePacket(BlockPos blockPos, LongAVLTreeSet filteredPlatforms, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String pidsPreset) {
        this.blockPos = blockPos;
        this.customMessages = customMessages;
        this.rowHidden = rowHidden;
        this.presetId = pidsPreset;
        this.hidePlatformNumber = hidePlatformNumber;
        this.filteredPlatforms = filteredPlatforms;
    }

    @Override
    public void write(PacketBufferSender packetBufferSender) {
        packetBufferSender.writeLong(blockPos.asLong());
        packetBufferSender.writeInt(customMessages.length);
        JCMPacketHandlerHelper.writeArray(packetBufferSender, customMessages, i -> packetBufferSender.writeString(customMessages[i]));
        JCMPacketHandlerHelper.writeArray(packetBufferSender, rowHidden, i -> packetBufferSender.writeBoolean(rowHidden[i]));
        JCMPacketHandlerHelper.writeArray(packetBufferSender, filteredPlatforms, packetBufferSender::writeLong);
        packetBufferSender.writeBoolean(hidePlatformNumber);
        packetBufferSender.writeString(presetId);
    }

    @Override
    public void runServer(MinecraftServer minecraftServer, ServerPlayer serverPlayerEntity) {
        Level world = serverPlayerEntity.level();
        BlockState state = BlockUtil.getBlockState(world, blockPos);

        if(state == null || !(state.getBlock() instanceof JCMBlock)) return;
        if(!PacketValidator.canConfigureBlock(serverPlayerEntity)) {
            JCMLogger.warn("Player {} attempted to configure PIDS at {} {} {} without permission.", serverPlayerEntity.getGameProfile().getName(), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            return;
        }

        ((JCMBlock)state.getBlock()).loopStructure(state, world, blockPos, (bs, be) -> {
            if(be instanceof PIDSBlockEntity) {
                ((PIDSBlockEntity)be).setData(customMessages, filteredPlatforms, rowHidden, hidePlatformNumber, presetId);
            }
        });
    }
}