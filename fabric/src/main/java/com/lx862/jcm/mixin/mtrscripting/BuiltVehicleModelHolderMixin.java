package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.nativeapi.NativeVehicleDriver;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.mtr.client.VehicleRidingMovement;
import org.mtr.core.data.VehicleCar;
import org.mtr.model.BuiltVehicleModelHolder;
import org.mtr.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replays captured vehicle-script draw/sound calls for each car, aligned to
 * the exact model transform MTR 4.1 uses. Ported from the 4.0-era
 * VehicleResourceMixin (which hooked the removed
 * {@code VehicleResource#queue}) onto the 4.1 per-car render entry
 * {@link BuiltVehicleModelHolder#render}.
 */
@Mixin(value = BuiltVehicleModelHolder.class, remap = false)
public abstract class BuiltVehicleModelHolderMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void jsblock$drawCarScript(StoredMatrixTransformations storedMatrixTransformations, org.mtr.data.VehicleExtension vehicle, int carNumber, boolean isWithinHalfRenderDistance, int[] scrollingDisplayIndexTracker, int light, Object openDoorways, boolean fromResourcePackCreator, CallbackInfo ci) {
        /* The mtrpatch HideRidingVehicleMixin cancels the original render for
           the ridden car when hideRidingVehicle is on, but this handler still
           runs and would replay the scripted PIDS/LCD content floating in
           front of the camera. Guard here too. */
        if (JCMClientConfig.INSTANCE.mtrPatch.hideRidingVehicle.value() && VehicleRidingMovement.isRiding(vehicle.getId())) return;

        VehicleCar vehicleCar = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber);
        String scriptGroupId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleCar.getVehicleId());
        if (scriptGroupId == null) return;

        /* Native (language=cpp) scripts never create a VehicleScriptInstance;
           their captured draw calls live in NativeDrawRegistry instead.
           Replaying them HERE puts the LCD quads through the exact same
           StoredMatrixTransformations / light as the JS path. */
        NativeVehicleDriver.invokeCar(vehicle, scriptGroupId, carNumber, storedMatrixTransformations, light);

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if (!(scriptInstance instanceof VehicleScriptInstance)) return;

        ScriptRenderManager carRenderManager = ((VehicleScriptInstance) scriptInstance).capturedScriptCalls.carRenderManagers.get(carNumber);
        ScriptSoundManager carSoundManager = ((VehicleScriptInstance) scriptInstance).capturedScriptCalls.carSoundManagers.get(carNumber);

        StoredMatrixTransformations newTransform = storedMatrixTransformations.copy();
        newTransform.add(gh -> gh.translate(0, -1, 0)); // Replicate behaviour from MTR 3

        Level world = Minecraft.getInstance().level;

        /* Per-car average bogie position, computed once per frame by the
           RenderVehiclesMixin script driver (shared with the native path). */
        ScriptVector3f soundPos = NativeVehicleDriver.carSoundPosition(vehicle.getHexId(), carNumber);

        if (carRenderManager != null) {
            carRenderManager.invoke(world, newTransform, Direction.NORTH, light);
        }

        if (carSoundManager != null && soundPos != null) {
            carSoundManager.invoke(world, soundPos);
        }
    }
}
