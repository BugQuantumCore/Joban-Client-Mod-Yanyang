package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import org.mtr.client.VehicleRidingMovement;
import org.mtr.data.VehicleExtension;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VehicleResource.class, remap = false, priority = 900)
public abstract class VehicleResourceMixin {
    @Inject(method = "queue(Lorg/mtr/mod/render/StoredMatrixTransformations;Lorg/mtr/mod/data/VehicleExtension;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void jsblock$renderScriptResult(StoredMatrixTransformations storedMatrixTransformations, VehicleExtension vehicle, int carNumber, int totalCars, int light, boolean noOpenDoorways, CallbackInfo ci) {
        // If hide riding vehicle and is current vehicle, cancel the rendering
        if(JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) {
            ci.cancel();
        }
    }
}
