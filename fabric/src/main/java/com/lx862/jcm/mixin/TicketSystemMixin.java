package com.lx862.jcm.mixin;

import com.lx862.jcm.mod.block.FareSaverBlock;
import com.lx862.jcm.mod.data.TransactionHistoryManager;
import com.lx862.jcm.mod.data.TransactionEntry;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.mtr.data.TicketSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = TicketSystem.class)
public abstract class TicketSystemMixin {
    @Shadow
    private static void incrementPlayerScore(Level world, Player player, String objective, String title, int value) {
    }

    @Inject(method = "onExit", at = @At(value = "INVOKE", target = "Lorg/mtr/data/TicketSystem;incrementPlayerScore(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Ljava/lang/String;Ljava/lang/String;I)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private static void onExit(Level world, Station station, Player player, boolean remindIfNoRecord, CallbackInfoReturnable<Boolean> cir, int entryZone1, int entryZone2, int entryZone3, boolean entered, long fare, long finalFare) {
        try {
            long finalDeductedAmount = -finalFare;

            if(entered && FareSaverBlock.discountList.containsKey(player.getUUID())) {
                long subsidyAmount = Math.min(finalFare, FareSaverBlock.discountList.get(player.getUUID()));
                incrementPlayerScore(world, player, "mtr_balance", "Balance", (int) subsidyAmount);
                finalDeductedAmount += subsidyAmount;

                if (subsidyAmount < 0 && finalFare > 0) {
                    player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "faresaver.saved_sarcasm", -subsidyAmount)), false);
                } else if (subsidyAmount > 0) {
                    player.displayClientMessage((TextUtil.translatable(TextCategory.HUD, "faresaver.saved", subsidyAmount)), false);
                }

                FareSaverBlock.discountList.remove(player.getUUID());
            }

            TransactionHistoryManager.appendEntry(player, TransactionEntry.createNow(station.getName(), finalDeductedAmount));
        } catch (Exception e) {
            JCMLogger.error("Unexpected error while saving JCM transaction logs!", e);
        }
    }

    @Inject(method = "addBalance", at = @At("TAIL"))
    private static void addBalance(Level world, Player player, int amount, CallbackInfo ci) {
        try {
            if (!world.isClientSide()) {
                TransactionHistoryManager.appendEntry(player, TransactionEntry.createNow("Add Value", amount));
            }
        } catch (Exception e) {
            JCMLogger.error("Unexpected error while saving JCM transaction logs!", e);
        }
    }
}