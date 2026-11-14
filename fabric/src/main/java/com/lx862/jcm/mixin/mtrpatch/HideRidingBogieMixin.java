package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.data.VehicleExtension;
import org.mtr.client.VehicleRidingMovement;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code mtrPatch.hideRidingVehicle}: also skips the bogies of the riding
 * vehicle (the wheel assemblies are queued separately from the car model in
 * MTR 4.1 via {@link VehicleResource#queueBogie}).
 */
@Mixin(value = VehicleResource.class, remap = false)
public class HideRidingBogieMixin {
    @Inject(method = "queueBogie", at = @At("HEAD"), cancellable = true)
    private void jsblock$hideRidingTrainBogie(int bogieIndex, StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, boolean isWithinHalfRenderDistance, int light, CallbackInfo ci) {
        if (JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) {
            ci.cancel();
        }
    }
}
