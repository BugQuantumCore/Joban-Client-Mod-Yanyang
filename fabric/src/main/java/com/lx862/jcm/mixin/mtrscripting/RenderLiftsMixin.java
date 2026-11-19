package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.core.util.render.ScriptRenderManager;
import com.lx862.mtrscripting.core.util.sound.ScriptSoundManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.lift.LiftWrapper;
import com.lx862.mtrscripting.mod.resource.LiftResourceProvider;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.core.data.Lift;
import org.mtr.libraries.com.logisticscraft.occlusionculling.util.Vec3d;
import org.mtr.libraries.it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.client.MinecraftClientData;
import org.mtr.client.VehicleRidingMovement;
import org.mtr.render.PositionAndRotation;
import org.mtr.render.RenderLifts;
import org.mtr.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.lx862.jcm.mod.extra.JCMPatchForMTR;

@Mixin(value = RenderLifts.class, remap = false)
public class RenderLiftsMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/libraries/it/unimi/dsi/fastutil/objects/ObjectCollection;forEach(Ljava/util/function/Consumer;)V"))
    private static void jsblock$executeScript(long millisElapsed, Vec3 cameraShakeOffset, CallbackInfo ci) {
        if(JCMPatchForMTR.isRenderingShadow()) return; /* Yanyang: cached via OptimizedRendererMixin redirect */
        /* One frame id per main-pass RenderLifts.render(): jsblock$drawScript
           below can be reached from several injection anchors inside the same
           per-lift lambda (and the display loop runs once per display side), so
           its script work is gated to once per lift per frame via
           tryBeginLiftScriptDraw. Because this method early-returns during
           Iris/OptiFine shadow passes, the id stays constant for the whole
           frame there, which also keeps script lifts from drawing a second
           time in the shadow pass. */
        JCMPatchForMTR.beginLiftRenderFrame();
        boolean jsblock$anyScriptedLift = false;
        for(MinecraftClientData.LiftWrapper liftWrapper : MinecraftClientData.getInstance().liftWrapperList.values()) {
            String style = liftWrapper.getLift().getStyle();

            LiftResourceProvider.LiftScriptConfiguration scriptEntry = MtrScriptingResourceManager.lift.getScriptEntry(style);
            if(scriptEntry == null) continue;
            jsblock$anyScriptedLift = true;

            LiftWrapper wrapperObject = new LiftWrapper(liftWrapper, (Level)Minecraft.getInstance().level);
            String nativeId=MtrScriptingResourceManager.lift.getScriptEntryId(style);
            if(com.lx862.jcm.nativeapi.NativeScriptManager.isLoaded(nativeId)) {
                com.lx862.jcm.nativeapi.NativeContentDriver.lift(nativeId,wrapperObject,liftWrapper.getLift());
                /* Yanyang: mixed entries (JS + C++ under one script id) fall
                   through to the JS instance pipeline below; native-only
                   entries bail out at the parsedScript check. */
            }
            if(scriptEntry.parsedScript() == null) continue;
            LiftScriptInstance scriptInstance = (LiftScriptInstance) MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("lift", liftWrapper.getLift().getHexId()), () -> new LiftScriptInstance(new LiftScriptContext(liftWrapper.getLift()), wrapperObject, scriptEntry.parsedScript()));
            if(scriptInstance == null) continue;

            scriptInstance.setWrapperObject(wrapperObject);

            scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                LiftScriptContext ctx = (LiftScriptContext) scriptInstance.getContextObject();
                scriptInstance.updateRenderer(ctx.getRenderManager());
                scriptInstance.updateSound(ctx.getSoundManager());
                ctx.resetForNextRun();
            });
        }
        /* jsblock$drawScript is require = 0 (see below); anchor drift on a
           future MTR build would otherwise fail silently — surface it once. */
        JCMPatchForMTR.noteLiftScriptVisibility(jsblock$anyScriptedLift);
    }

    /**
     * Per-lift draw hook, ported to MTR 4.1's RenderLifts: the per-lift body
     * lives in {@code lambda$render$0} — bytecode-verified against the pinned
     * yanyang.18 jar, and matching upstream 4.1.0-beta.2 as well (both MTR
     * builds compile render()'s lambdas to the same source-order numbering;
     * note javac only settled on that numbering on recent JDKs, an MTR
     * rebuilt with an older JDK may renumber them).
     *
     * <p>The injection cannot sit at the lambda's tail RETURN: every local
     * past {@code absolutePositionAndRotation} is declared inside the
     * {@code if (liftWrapper.shouldRender)} block, and javac ends their
     * LocalVariableTable ranges at the block close — one instruction before
     * the RETURN. Old-style LocalCapture therefore sees them as dead at
     * RETURN, CAPTURE_FAILSOFT silently skips the injection and the
     * {@code defaultRequire=1} check then aborts the game with
     * "failed injection check, (0/1) succeeded" (builds 109/110).</p>
     *
     * <p>Two {@code @At} anchors, because the two supported MTR builds render
     * the built-in lift car differently. yanyang.18 calls
     * {@code RenderLifts.getLiftModel(Lift)} (verified at pc 554 of the
     * pinned jar) right before the display loop; upstream 4.1.0-beta.2 has
     * the {@code ModelLift1} render commented out entirely — no
     * {@code getLiftModel} method at all — so the earliest per-lift call site
     * that exists in BOTH builds after every captured local is live is the
     * display loop's {@code StoredMatrixTransformations.copy()} (pc 647).
     * An anchor a build doesn't have simply matches nothing; on builds with
     * both, the earlier one wins because {@code ci.cancel()} there skips the
     * rest of the lambda. The display loop runs once per display side, so the
     * handler is frame-gated ({@code JCMPatchForMTR#tryBeginLiftScriptDraw})
     * to exactly one run per lift per frame.</p>
     *
     * <p>{@code require = 0}: if a future MTR restructures render() and both
     * anchors vanish, lift scripts stop drawing and
     * {@code JCMPatchForMTR#noteLiftScriptVisibility} logs a one-time warning
     * — instead of hard-crashing at world entry the way build 112 did on
     * upstream 4.1.0-beta.2 (no {@code getLiftModel} → 0 injection points →
     * "failed injection check, (0/1) succeeded" on the first lift render;
     * the "Scanned 0 target(s)" note in those reports is a Fabric Mixin
     * 0.17.5 reporting artifact, the counter it prints is never assigned).</p>
     */
    @Inject(method = "lambda$render$0", at = {
            @At(value = "INVOKE", target = "Lorg/mtr/render/RenderLifts;getLiftModel(Lorg/mtr/core/data/Lift;)Lorg/mtr/model/ModelLift1;"),
            @At(value = "INVOKE", target = "Lorg/mtr/render/StoredMatrixTransformations;copy()Lorg/mtr/render/StoredMatrixTransformations;")
    }, locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true, require = 0)
    private static void jsblock$drawScript(boolean isHoldingRefresher, ClientLevel clientWorld, ObjectArrayList cullingTasks, Vec3d camera, Vec3 cameraShakeOffset, boolean canRide, LocalPlayer clientPlayerEntity, long millisElapsed, MinecraftClientData.LiftWrapper liftWrapper, CallbackInfo ci, Lift lift, PositionAndRotation absolutePositionAndRotation, IntObjectImmutablePair ridingVehicleCarNumberAndOffset, PositionAndRotation ridingCarPositionAndRotation, Vec3 offsetVector, Double offsetRotation, PositionAndRotation renderingPositionAndRotation, ObjectArrayList floorsAndDoorways, ObjectArrayList openDoorways, AABB doorway1, AABB doorway2, boolean doorway1Open, boolean doorway2Open, StoredMatrixTransformations storedMatrixTransformations) {
        /* Frame gate: once per lift per frame, whichever anchor fired. */
        if(!JCMPatchForMTR.tryBeginLiftScriptDraw(lift.getId())) return;

        /* Yanyang: C++ lift scripts share the same script-controlled + riding
           semantics as JS. Mixed entries (JS + C++ under one script id) run
           the native modules AND the JS script: the built-in renderer is
           suppressed and the riding movement applied exactly once, no matter
           how many script parts contributed this frame. */
        String nativeId=MtrScriptingResourceManager.lift.getScriptEntryId(lift.getStyle());
        boolean nativeLoaded = com.lx862.jcm.nativeapi.NativeScriptManager.isLoaded(nativeId);
        if(nativeLoaded) {
            com.lx862.jcm.nativeapi.NativeContentDriver.invokeLift(nativeId,lift.getId(),(Level)clientWorld,storedMatrixTransformations.copy(),absolutePositionAndRotation.light,
                new ScriptVector3f((float)absolutePositionAndRotation.position.x(),(float)absolutePositionAndRotation.position.y(),(float)absolutePositionAndRotation.position.z()));
        }

        ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("lift", lift.getHexId()));
        if(scriptInstance instanceof LiftScriptInstance) {
            Level world = (Level)Minecraft.getInstance().level;
            ScriptRenderManager scriptRenderManager = ((LiftScriptInstance)scriptInstance).getRenderManager();
            ScriptSoundManager scriptSoundManager = ((LiftScriptInstance)scriptInstance).getSoundManager();
            scriptRenderManager.invoke(world, storedMatrixTransformations.copy(), Direction.NORTH, absolutePositionAndRotation.light);
            /* Float, not int: the int casts snapped JS lift sound origins to
               block corners (up to half a block off); the native path already
               used floats. */
            scriptSoundManager.invoke(world, new ScriptVector3f((float)absolutePositionAndRotation.position.x(),(float)absolutePositionAndRotation.position.y(),(float)absolutePositionAndRotation.position.z()));
        }

        boolean skipBuiltinRenderer = MtrScriptingResourceManager.lift.isScriptControlledRendering(lift.getStyle());
        if(skipBuiltinRenderer && (nativeLoaded || scriptInstance instanceof LiftScriptInstance)) {
            ci.cancel();
            if(canRide) {
                VehicleRidingMovement.movePlayer(millisElapsed, lift.getId(), 0, floorsAndDoorways, null, null, null, absolutePositionAndRotation);
            }
        }
    }
}
