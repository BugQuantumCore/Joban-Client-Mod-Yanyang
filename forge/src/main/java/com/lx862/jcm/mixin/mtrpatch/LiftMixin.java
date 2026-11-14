package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import org.mtr.core.data.Data;
import org.mtr.core.data.Lift;
import org.mtr.core.data.LiftFloor;
import org.mtr.core.data.TransportMode;
import org.mtr.core.generated.data.LiftSchema;
import com.lx862.jcm.mod.compat.SoundHelper;
import org.mtr.MTR;
import org.mtr.block.BlockLiftTrackFloor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = Lift.class, remap = false)
public abstract class LiftMixin extends LiftSchema {
    @Shadow
    @Final
    private boolean isClientside;

    @Shadow
    public abstract LiftFloor getCurrentFloor();

    protected LiftMixin(TransportMode transportMode, Data data) {
        super(transportMode, data);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void jsblock$playArrivalDingSound(long millisElapsed, CallbackInfo ci) {
        if(isClientside && JCMPatchForMTR.liftJustArrived(id, instructions.size())) {
            LiftFloor currentFloor = getCurrentFloor();
            BlockPos pos = Init.positionToBlockPos(currentFloor.getPosition());
            ClientLevel mcWorld = Minecraft.getInstance().level;
            LocalPlayer mcPlayer = Minecraft.getInstance().player;

            if(mcWorld != null && mcPlayer != null) {
                BlockEntity be = mcWorld.getBlockEntity(pos);
                if(be != null && be instanceof BlockLiftTrackFloor.BlockEntity) {
                    BlockLiftTrackFloor.BlockEntity floorBE = (BlockLiftTrackFloor.BlockEntity)be;
                    if(floorBE.getShouldDing()) {
                        mcWorld.playSound(Player.cast(mcPlayer), pos, SoundHelper.createSoundEvent(JCMPatchForMTR.LIFT_DING_SOUND), SoundSource.BLOCKS, 1, 2);
                    }
                }
            }
        }
    }
}
