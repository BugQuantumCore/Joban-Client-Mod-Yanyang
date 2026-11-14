package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectDoubleImmutablePair;
import net.minecraft.world.phys.AABB;
import net.minecraft.resources.ResourceLocation;
import org.mtr.data.VehicleExtension;
import org.mtr.generated.resource.ModelPropertiesPartSchema;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.resource.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.mtr.resource.ModelDisplayPart;

@Mixin(value = ModelPropertiesPart.class, remap = false)
public class ModelPropertiesPartMixin extends ModelPropertiesPartSchema {
    protected ModelPropertiesPartMixin(PartCondition condition, RenderStage renderStage, PartType type, double displayXPadding, double displayYPadding, String displayColorCjk, String displayColor, double displayMaxLineHeight, double displayCjkSizeRatio, double displayPadZeros, DisplayType displayType, String displayDefaultText, double doorXMultiplier, double doorZMultiplier, DoorAnimationType doorAnimationType, long renderFromOpeningDoorTime, long renderUntilOpeningDoorTime, long renderFromClosingDoorTime, long renderUntilClosingDoorTime, long flashOffTime, long flashOnTime) {
        super(condition, renderStage, type, displayXPadding, displayYPadding, displayColorCjk, displayColor, displayMaxLineHeight, displayCjkSizeRatio, displayPadZeros, displayType, displayDefaultText, doorXMultiplier, doorZMultiplier, doorAnimationType, renderFromOpeningDoorTime, renderUntilOpeningDoorTime, renderFromClosingDoorTime, renderUntilClosingDoorTime, flashOffTime, flashOnTime);
    }

    @Inject(method = "renderDisplay", at = @At("HEAD"), cancellable = true)
    private void jsblock$hideDisplayParts(StoredMatrixTransformations storedMatrixTransformations, ModelDisplayPart modelDisplayPart, org.mtr.data.VehicleExtension vehicle, int carNumber, int[] scrollingDisplayIndexTracker, boolean fromResourcePackCreator, CallbackInfo ci) {
        /* Only DISPLAY parts ever consult the script registry, and the car's
           vehicle id is available O(1) from the immutable car list. The old
           body ran vehicle.getVehicleCarsAndPositions() here — a FULL
           position-tree rebuild per model part per car per frame (P x N
           builds where 1 already happens elsewhere each frame) — and NPE'd
           when MTR passes a null vehicle (stored-model render path with
           optimized rendering off). (Upstream prerelease.3 makes the same
           DISPLAY-only + immutableVehicleCars change in a simpler form.) */
        if(type != PartType.DISPLAY || vehicle == null) return;
        final int carCount = vehicle.vehicleExtraData.immutableVehicleCars.size();
        if(carNumber < 0 || carNumber >= carCount) return;
        final String vehicleId = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber).getVehicleId();
        if(MtrScriptingResourceManager.vehicle.shouldHideDisplayParts(vehicleId)) {
            ci.cancel();
        }
    }
}