package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.data.VehicleExtension;
import org.mtr.client.VehicleRidingMovement;
import org.mtr.model.BuiltVehicleModelHolder;
import org.mtr.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code mtrPatch.hideRidingVehicle}: skips drawing the car model of the
 * vehicle the local player is riding. Ported from the 4.0-era
 * RenderVehiclesMixin lambda hooks onto MTR 4.1's per-car model render entry
 * point {@link BuiltVehicleModelHolder#render}.
 */
@Mixin(value = BuiltVehicleModelHolder.class, remap = false)
public class HideRidingVehicleMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jsblock$hideRidingTrainModel(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, boolean isWithinHalfRenderDistance, int[] scrollingDisplayIndexTracker, int light, Object openDoorways, boolean fromResourcePackCreator, CallbackInfo ci) {
        if (JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) {
            ci.cancel();
        }
    }
}
