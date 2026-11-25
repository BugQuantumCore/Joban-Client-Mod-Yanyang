package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.nativeapi.NativeVehicleDriver;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.NTETrainWrapper;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.mod.resource.VehicleResourceProvider;
import org.mtr.core.data.VehicleCar;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.client.MinecraftClientData;
import org.mtr.data.VehicleExtension;
import org.mtr.render.*;
import org.mtr.resource.VehicleResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import com.lx862.jcm.mod.util.JCMLogger;

@Mixin(value = RenderVehicles.class, remap = false)
public class RenderVehiclesMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectArraySet;forEach(Ljava/util/function/Consumer;)V"))
    private static void jsblock$executeScript(long millisElapsed, Vec3 cameraShakeOffset, CallbackInfo ci) {
        /* Yanyang: Iris/OptiFine shadow passes re-run RenderVehicles.render —
           drive the scripts exactly once per frame (check cached via
           OptimizedRendererMixin redirect). */
        if(JCMPatchForMTR.isRenderingShadow()) return;
        NativeVehicleDriver.beginFrame();
        /* Per-frame live-vehicle id set for the JS instances' shouldInvalidate
           (see VehicleScriptInstance.beginFrame) — one O(V) pass here replaces
           one O(V) stream scan per scripted vehicle per frame. */
        VehicleScriptInstance.beginFrame();

        for(VehicleExtension vehicle : MinecraftClientData.getInstance().vehicles) {
            /* Yanyang: C++ (language=cpp) scripts take the native route and
               never build a VehicleScriptInstance — drive them first so their
               per-car draw calls are captured before MTR starts queueing cars.
               Idle for every vehicle whose script id has no native modules. */
            NativeVehicleDriver.render(vehicle);

            ObjectImmutableList<VehicleCar> cars = vehicle.vehicleExtraData.immutableVehicleCars;

            /* Lazily allocated: most trains carry no script at all, and a fresh
               map per vehicle per frame was pure allocation churn. */
            Object2ObjectOpenHashMap<String, VehicleResourceProvider.VehicleScriptConfiguration> scriptsInVehicle = null;
            for (VehicleCar vehicleCar : cars) {
                String scriptEntryId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleCar.getVehicleId());
                VehicleResourceProvider.VehicleScriptConfiguration script = MtrScriptingResourceManager.vehicle.getVehicleScript(scriptEntryId);
                if (script == null) continue;
                if (scriptsInVehicle == null) scriptsInVehicle = new Object2ObjectOpenHashMap<>();
                if (scriptsInVehicle.containsKey(scriptEntryId)) continue;
                scriptsInVehicle.put(scriptEntryId, script);
            }
            if (scriptsInVehicle == null) continue;
            /* Latch (never resets): this build actually runs scripted
               vehicles — feeds the degradation warning at the tail of this
               handler (see jsblock$bogieHookWarned). */
            jsblock$anyScriptedVehicle = true;
            /* Per-frame car sound positions for BOTH script sides (native
               replay in invokeCar, JS replay in VehicleResourceMixin). */
            NativeVehicleDriver.computeCarPositions(vehicle);

            for(Map.Entry<String, VehicleResourceProvider.VehicleScriptConfiguration> scriptEntry : scriptsInVehicle.entrySet()) {
                String scriptEntryId = scriptEntry.getKey();
                /* Yanyang: a native-only entry is also registered here so
                   NativeVehicleDriver can read its dataFetchMode, but it
                   carries no ParsedScript — driving it through the JS instance
                   pipeline would NPE on .parsedScript(); it is rendered above
                   by NativeVehicleDriver.render(). Mixed entries (a .js part
                   plus C++ modules under the same id) run BOTH: the native
                   part above, the JS part here — each side captures into its
                   own replay registry, so draw calls never collide. */
                if(scriptEntry.getValue().parsedScript() == null) continue;
                /* Look the instance up first and only build the car list
                   when creating one: on every steady-state frame the
                   instance already exists, and the old count-then-fill ran
                   the script-entry-id lookup TWICE per car before the
                   lookup even happened. Single-pass collection into a
                   fastutil IntArrayList (relocated package), snapshotted
                   to the exact-size array the context stores. */
                UniqueKey instanceKey = new UniqueKey("vehicle", vehicle.getHexId(), scriptEntryId);
                /* The <VehicleWrapper> witness is load-bearing: without it
                   the one-arg getInstance infers Object, and the cast to
                   VehicleScriptInstance no longer compiles. */
                VehicleScriptInstance scriptInstance = (VehicleScriptInstance) MTRContentScripting.getScriptManager().getInstanceManager().<VehicleWrapper>getInstance(instanceKey);
                if(scriptInstance == null) {
                    IntArrayList scriptCars = new IntArrayList();
                    for(int i = 0; i < cars.size(); i++) {
                        if(scriptEntryId.equals(MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(cars.get(i).getVehicleId()))) scriptCars.add(i);
                    }
                    int[] carsArray = scriptCars.toIntArray();
                    scriptInstance = (VehicleScriptInstance) MTRContentScripting.getScriptManager().getInstanceManager().<VehicleWrapper>getInstance(instanceKey, () -> new VehicleScriptInstance(new VehicleScriptContext(vehicle, scriptEntryId, carsArray), vehicle, scriptEntry.getValue().parsedScript()));
                    if(scriptInstance == null) continue;
                }

                VehicleScriptContext.DataFetchMode dataFetchMode = scriptEntry.getValue().dataFetchMode();

                /* Legacy stuff */
                if(((VehicleScriptContext)scriptInstance.getContextObject()).getFetchModeOverride() != null) {
                    dataFetchMode = ((VehicleScriptContext)scriptInstance.getContextObject()).getFetchModeOverride();
                }

                /* Build the wrapper (a full position tree + per-car door
                   world scans) ONLY on frames that actually run the script —
                   while a slow script's task is in flight that is every
                   frame, and the wrapper was built and discarded each time,
                   plus once more per frame for mixed JS+native entries. */
                if(!scriptInstance.getScript().willInvokeRender(scriptInstance)) {
                    continue;
                }

                VehicleWrapper wrapperObject = new NTETrainWrapper(dataFetchMode, vehicle);
                scriptInstance.setWrapperObject(wrapperObject);

                if(dataFetchMode == VehicleScriptContext.DataFetchMode.MANDATORY && !wrapperObject.isStopsDataFullyFetched()) {
                    continue;
                }

                /* scriptInstance is reassigned above (lookup, then
                   create), so it is not effectively final — pin it in a
                   final local for the lambda below. */
                final VehicleScriptInstance instance = scriptInstance;
                instance.getScript().invokeRenderFunctions(instance, () -> {
                    VehicleScriptContext ctx = (VehicleScriptContext) instance.getContextObject();
                    instance.capturedScriptCalls.capture(ctx.getScriptCallsHolder());
                    ctx.resetForNextRun();
                });
            }
        }

        /* Self-contained degradation warning: scripted vehicles ran this
           frame but the car-level lambda hook never applied — a future MTR
           build renumbered the lambdas and require = 0 kept us booting
           instead of crashing. Two missed frames before warning, then
           latch so it logs exactly once per game session. */
        if(jsblock$anyScriptedVehicle && !jsblock$carHookEverApplied && !jsblock$bogieHookWarned && ++jsblock$bogieHookMissStreak >= 2) {
            jsblock$bogieHookWarned = true;
            JCMLogger.warn("Scripted vehicles are running but the RenderVehicles car-level hook never applied; the lambda$render$3/lambda$render$5 injections no longer match this MTR build and bogie script rendering will not work.");
        }
    }

    /**
     * MTR 4.1's bogie lambda (lambda$render$5) no longer receives the car
     * number — it was a parameter of the 4.0-era lambda the 4.0 mixin hooked.
     * The enclosing car-level lambda (lambda$render$3) still carries it as
     * its 17th argument, so stash it here (HEAD runs right before that car's
     * bogie loop) and read it back from jsblock$drawBogieScript. The two
     * int locals in scope at HEAD are ridingCarNumber (ordinal 0) and
     * carNumber (ordinal 1).
     */
    @Unique
    private static int jsblock$currentCarNumber = -1;

    /* Degradation-warning state for the fragile lambda hooks below (see
       the tail of jsblock$executeScript). */
    @Unique
    private static boolean jsblock$anyScriptedVehicle = false;
    @Unique
    private static boolean jsblock$carHookEverApplied = false;
    @Unique
    private static boolean jsblock$bogieHookWarned = false;
    @Unique
    private static int jsblock$bogieHookMissStreak = 0;

    /**
     * MTR 4.1's bogie lambda (lambda$render$5) no longer receives the car
     * number — it was a parameter of the 4.0-era lambda the 4.0 mixin hooked.
     * The enclosing car-level lambda (lambda$render$3) still carries it as
     * its 17th argument, so stash it here (HEAD runs right before that car's
     * bogie loop) and read it back from jsblock$drawBogieScript. The two
     * int locals in scope at HEAD are ridingCarNumber (ordinal 0) and
     * carNumber (ordinal 1).
     *
     * <p><b>Fragile hook:</b> {@code lambda$render$3} is a synthetic
     * compiler-generated name that MTR may renumber in any future build.
     * Injected with {@code require = 0} so a mismatch degrades silently
     * (bogie script rendering stops working; the warning at the tail of
     * {@code jsblock$executeScript} fires) instead of crashing the game at
     * startup.</p>
     */
    @Inject(method = "lambda$render$3", at = @At("HEAD"), require = 0)
    private static void jsblock$trackCurrentCar(CallbackInfo ci, @Local(ordinal = 1) int carNumber) {
        jsblock$carHookEverApplied = true;
        RenderVehiclesMixin.jsblock$currentCarNumber = carNumber;
    }

    /**
     * Replays captured per-bogie script draw calls at the exact transform
     * MTR 4.1 uses for the bogie. The 4.1 bogie lambda's real signature is
     * (Vec3 offsetVector, Double offsetRotation, PositionAndRotation
     * ridingCarPositionAndRotation, Vec3 cameraShakeOffset, VehicleResource
     * vehicleResource, VehicleExtension vehicle, boolean
     * isWithinHalfRenderDistance, PositionAndRotation
     * absoluteVehicleCarPositionAndRotation, int bogieIndex,
     * PositionAndRotation absoluteBogiePositionAndRotation).
     *
     * <p>Injected just before the queueBogie call so the
     * StoredMatrixTransformations local (built from
     * RenderVehicles#getStoredMatrixTransformations plus the 180°-X bogie
     * rotation) is guaranteed live for @Local capture.</p>
     *
     * <p><b>Fragile hook:</b> {@code lambda$render$5} is a synthetic
     * compiler-generated name that MTR may renumber in any future build.
     * Injected with {@code require = 0} so a mismatch degrades silently
     * (bogie script rendering stops working; the warning at the tail of
     * {@code jsblock$executeScript} fires) instead of crashing the game at
     * startup.</p>
     */
    @Inject(method = "lambda$render$5", at = @At(value = "INVOKE", target = "Lorg/mtr/resource/VehicleResource;queueBogie(ILorg/mtr/render/StoredMatrixTransformations;Lorg/mtr/data/VehicleExtension;ZI)V"), require = 0)
    private static void jsblock$drawBogieScript(Vec3 offsetVector, Double offsetRotation, PositionAndRotation ridingCarPositionAndRotation, Vec3 cameraShakeOffset, VehicleResource vehicleResource, VehicleExtension vehicle, boolean isWithinHalfRenderDistance, PositionAndRotation absoluteVehicleCarPositionAndRotation, int bogieIndex, PositionAndRotation absoluteBogiePositionAndRotation, CallbackInfo ci, @Local(ordinal = 0) StoredMatrixTransformations storedMatrixTransformations) {
        int carNumber = jsblock$currentCarNumber;
        if (carNumber < 0) return;

        VehicleCar vehicleCar = vehicle.vehicleExtraData.immutableVehicleCars.get(carNumber);
        String scriptGroupId = MtrScriptingResourceManager.vehicle.getVehicleScriptEntryId(vehicleCar.getVehicleId());
        if(scriptGroupId == null) return;
        com.lx862.jcm.nativeapi.NativeVehicleDriver.invokeBogie(vehicle,scriptGroupId,carNumber,bogieIndex,storedMatrixTransformations,absoluteVehicleCarPositionAndRotation.light);

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("vehicle", vehicle.getHexId(), scriptGroupId));
        if(!(scriptInstance instanceof VehicleScriptInstance)) return;

        Level world = (Level)Minecraft.getInstance().level;
        List<ScriptRenderManager> bogieRenderManagers = ((VehicleScriptInstance)scriptInstance).capturedScriptCalls.carBogieRenderers.get(carNumber);
        if(bogieRenderManagers != null && bogieIndex < bogieRenderManagers.size()) {
            bogieRenderManagers.get(bogieIndex).invoke(world, storedMatrixTransformations.copy(), Direction.NORTH, absoluteVehicleCarPositionAndRotation.light);
        }
    }
}
