package com.lx862.jcm.mixin;

import com.lx862.mtrscripting.mod.impl.mtr.server.DoorDirectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockPSDAPGDoorBase;
import org.mtr.block.PlatformHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public class DoorBlockChangeMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
    private void jsblock$trackDoorBlockChange(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerLevel)) return;
        final Object newBlock = state.getBlock();
        if (newBlock instanceof PlatformHelper || newBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
            return;
        }
        if (!DoorDirectionManager.chunkMayContainPlatforms(this, pos.getX() >> 4, pos.getZ() >> 4)) return;
        final Object replacedBlock = ((Level) (Object) this).getBlockState(pos).getBlock();
        if (replacedBlock instanceof PlatformHelper || replacedBlock instanceof BlockPSDAPGDoorBase) {
            DoorDirectionManager.onDoorBlockChange(this, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
