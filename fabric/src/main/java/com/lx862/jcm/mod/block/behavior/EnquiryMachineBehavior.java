package com.lx862.jcm.mod.block.behavior;

import com.lx862.jcm.mod.data.EnquiryScreenType;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.data.TransactionHistoryManager;
import com.lx862.jcm.mod.network.gui.EnquiryUpdateGUIPacket;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.registry.SoundEvents;
import org.mtr.data.TicketSystem;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public interface EnquiryMachineBehavior {
    default void enquiry(EnquiryScreenType type, BlockPos pos, Level world, Player player) {
        world.playSound(null, player.blockPosition(), SoundEvents.TICKET_PROCESSOR_ENTRY.get(), SoundSource.BLOCKS, 1, 1);

        if(type == EnquiryScreenType.NONE) {
            int score = TicketSystem.getBalance(world, player);
            player.displayClientMessage((TextUtil.translatable("gui.mtr.balance", String.valueOf(score))), true);
        } else {
            List<TransactionEntry> entries = TransactionHistoryManager.getLogs(player);
            Networking.sendPacketToClient(player, new EnquiryUpdateGUIPacket(type, pos, entries, TicketSystem.getBalance(world, player)));
        }
    }
}
