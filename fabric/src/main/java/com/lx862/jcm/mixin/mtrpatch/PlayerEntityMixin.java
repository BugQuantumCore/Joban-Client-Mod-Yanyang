package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lowers the local player's eye height while "sitting" on a MTR vehicle
 * (sneak toggle while riding, see {@link JCMPatchForMTR#isSitting}).
 *
 * <p>4.0-era JCM hooked {@code PlayerEntity#getStandingEyeHeight(Pose,
 * EntityDimensions)}, which 1.21 removed: eye height now lives in the cached
 * {@code Entity#eyeHeight} field, read through two final query methods —
 * {@code Entity#getEyeHeight()} (drives {@code Camera#tick}'s smooth camera
 * height) and {@code Entity#getEyeY()} (the pick/raycast eye origin).
 * Injecting both query points on {@link Entity} with a local-player guard
 * reproduces the old behaviour on 1.21.1 and 1.21.4, whose eye-height APIs
 * are identical. Guarding on {@code (Object) this == Minecraft#player} keeps
 * every other entity (and the integrated server's player copy) untouched.</p>
 */
@Mixin(Entity.class)
public class PlayerEntityMixin {

    @Inject(method = "getEyeHeight()F", at = @At("HEAD"), cancellable = true)
    private void jsblock$sitPlayerOnVehicle(CallbackInfoReturnable<Float> cir) {
        /* Local player only: isSitting is the LOCAL player's state, but this
        inject applies to every Entity — remote players' and mobs' eye heights
        must not move. */
        if (JCMPatchForMTR.isSitting && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(1.27F);
        }
    }

    @Inject(method = "getEyeY()D", at = @At("HEAD"), cancellable = true)
    private void jsblock$sitPlayerEyeY(CallbackInfoReturnable<Double> cir) {
        if (JCMPatchForMTR.isSitting && (Object) this == Minecraft.getInstance().player) {
            cir.setReturnValue(((Entity) (Object) this).getY() + 1.27D);
        }
    }
}
