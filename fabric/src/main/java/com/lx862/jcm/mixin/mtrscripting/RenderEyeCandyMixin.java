package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.resource.EyecandyResourceProvider;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptInstance;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyecandyBlockEntityWrapper;
import com.lx862.mtrscripting.core.primitive.ParsedScript;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.block.BlockEyeCandy;
import org.mtr.block.IBlock;
import org.mtr.render.RenderEyeCandy;
import org.mtr.render.StoredMatrixTransformations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.lx862.jcm.mod.extra.JCMPatchForMTR;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

@Mixin(value = RenderEyeCandy.class, remap = false)
public class RenderEyeCandyMixin {
    /**
     * Per-block-entity transform cache: the block's pos/translate/rotate
     * values change only when its NBT changes, but the old path rebuilt a
     * fresh base {@code StoredMatrixTransformations} (position entry plus a
     * rotation lambda) per eyecandy per frame — and the JS path built a
     * base+copy pair on top. Entries die with the block entity (weak keys);
     * field comparison catches NBT edits exactly.
     *
     * <p><b>Capture discipline:</b> the cached consumer must capture
     * PRIMITIVES only. The map is a {@link WeakHashMap} keyed by the block
     * entity — a lambda capturing {@code be} would be stored in the value
     * and strongly reference the key, making the entry permanently
     * reachable (the classic WeakHashMap self-reference leak).
     *
     * <p><b>World anchor:</b> the cached holder is built with MTR's own
     * {@code RenderEyeCandy} base pattern — the 4-arg constructor carrying
     * the block's world position, with ONLY local transforms (NBT translate
     * + rotations) as consumers. The anchor participates in the flush-time
     * {@code translate(initialTranslate - offset)} camera compensation; a
     * world translate stuffed into a consumer is NOT compensated and
     * displaces the geometry by the rendering entity's position (the
     * pre-fix cache rendered eyecandy at entityPos + blockPos).
     *
     * <p>The cached transform is used directly as the replay root:
     * {@code ScriptRenderManager.invoke} replays every draw call against
     * its own pooled snapshot (see {@link com.lx862.mtrscripting.core.util.render.TransformCapturePool}),
     * so a shared base is neither mutated nor cross-contaminated and needs
     * no per-frame {@code copy()}.
     */
    private static final class TransformCache {
        StoredMatrixTransformations transform;
        int px, py, pz;
        float tx, ty, tz, rx, ry, rz;
        Direction facing;
        boolean initialized;

        boolean matches(BlockEyeCandy.EyeCandyBlockEntity be, Direction facing) {
            BlockPos pos = be.getBlockPos();
            return this.initialized
                    && this.px == pos.getX() && this.py == pos.getY() && this.pz == pos.getZ()
                    && this.tx == be.getTranslateX() && this.ty == be.getTranslateY() && this.tz == be.getTranslateZ()
                    && this.rx == be.getRotateX() && this.ry == be.getRotateY() && this.rz == be.getRotateZ()
                    && this.facing == facing;
        }

        void rebuild(BlockEyeCandy.EyeCandyBlockEntity be, Direction facing) {
            final BlockPos pos = be.getBlockPos();
            /* Primitives only — see the capture discipline note above. */
            final double bx = 0.5 + pos.getX(), by = pos.getY(), bz = 0.5 + pos.getZ();
            final float ltx = be.getTranslateX(), lty = be.getTranslateY(), ltz = be.getTranslateZ();
            final float lrx = be.getRotateX(), lry = be.getRotateY(), lrz = be.getRotateZ();
            final float facingRot = -(float) Math.toRadians(facing.toYRot()) + (float) Math.PI;
            /* 4-arg world anchor (MTR's RenderEyeCandy pattern): consumers
               carry ONLY the local NBT translate + rotations. */
            final StoredMatrixTransformations transform = new StoredMatrixTransformations(bx, by, bz);
            transform.add(poseStack -> {
                poseStack.translate(ltx, lty, ltz);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(facingRot));
                poseStack.mulPose(com.mojang.math.Axis.XP.rotation(lrx + (float) Math.PI));
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(lry));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(lrz));
            });
            this.transform = transform;
            this.px = pos.getX();
            this.py = pos.getY();
            this.pz = pos.getZ();
            this.tx = ltx;
            this.ty = lty;
            this.tz = ltz;
            this.rx = lrx;
            this.ry = lry;
            this.rz = lrz;
            this.facing = facing;
            this.initialized = true;
        }
    }

    private static final Map<BlockEyeCandy.EyeCandyBlockEntity, TransformCache> TRANSFORMS = new WeakHashMap<>();

    private static StoredMatrixTransformations transformOf(BlockEyeCandy.EyeCandyBlockEntity blockEntity, Direction facing) {
        TransformCache cache = TRANSFORMS.get(blockEntity);
        if (cache == null) {
            cache = new TransformCache();
            TRANSFORMS.put(blockEntity, cache);
        }
        if (!cache.matches(blockEntity, facing)) {
            /* Rebuild swaps in a fresh anchored holder — safe even mid-frame:
               in-flight pooled snapshots copied the old consumers at acquire
               time and never alias this list. */
            cache.rebuild(blockEntity, facing);
        }
        return cache.transform;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lorg/mtr/mod/block/BlockEyeCandy$BlockEntity;getModelId()Ljava/lang/String;"), cancellable = true)
    public void jsblock$executeAndRenderScript(BlockEyeCandy.EyeCandyBlockEntity blockEntity, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, ClientLevel clientWorld, LocalPlayer clientPlayer, float tickDelta, int light, int overlay, CallbackInfo ci) {
        Level world = blockEntity.getLevel();
        if(world == null) return;
        if(world.getBlockEntity(blockEntity.getBlockPos()) == null) return; // Cached BE can continue to render outside the render distance

        String eyecandyId = blockEntity.getModelId();
        EyecandyResourceProvider.EyecandyScriptConfiguration eyecandyScriptEntry = MtrScriptingResourceManager.eyecandy.getScriptEntry(eyecandyId);
        if(eyecandyScriptEntry == null) return;
        String nativeId=MtrScriptingResourceManager.eyecandy.getScriptEntryId(eyecandyId);
        if(com.lx862.jcm.nativeapi.NativeScriptManager.isLoaded(nativeId)) {
            Direction facing=IBlock.getStatePropertySafe(world,blockEntity.getBlockPos(),com.lx862.jcm.mod.data.BlockProperties.FACING);
            /* Cached base (rebuilt only on NBT change): invoke replays every
               draw call against pooled snapshots seeded from this base, so
               the cached instance serves directly as the replay root. */
            StoredMatrixTransformations transforms=transformOf(blockEntity, facing);
            /* Shadow-pass guard, the same discipline the JS branch below has:
               during the shader shadow phase the native script must NOT run
               again (no re-render, no context reset, no event consumption, no
               sounds, no texture uploads) — only the draw calls captured by
               the last main pass replay, so shadow-casting models keep their
               silhouette. */
            com.lx862.jcm.nativeapi.NativeContentDriver.eyecandy(nativeId,blockEntity,world,transforms,facing,light,JCMPatchForMTR.isRenderingShadow());
            if(MtrScriptingResourceManager.eyecandy.isScriptControlledRendering(eyecandyId)) ci.cancel();
            /* Yanyang: mixed entries (JS + C++ modules under one script id)
               keep running — the JS part below executes in addition to the
               native modules above; ci.cancel() only suppresses MTR's
               built-in model, not the rest of this injector. */
        }
        ParsedScript parsedScript = eyecandyScriptEntry.parsedScript();
        if(parsedScript == null) return; /* native-only entry: nothing left to drive */

        ScriptInstance<EyecandyBlockEntityWrapper> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", eyecandyId, blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), blockEntity.getBlockPos().getZ()), () -> new EyeCandyScriptInstance(blockEntity, new EyeCandyScriptContext(blockEntity), parsedScript));
        if(!(scriptInstance instanceof EyeCandyScriptInstance)) return;

        if(MtrScriptingResourceManager.eyecandy.isScriptControlledRendering(eyecandyId)) {
            ci.cancel();
        }

        EyeCandyScriptInstance eyeCandyScriptInstance = (EyeCandyScriptInstance) scriptInstance;
        EyeCandyScriptContext eyeCandyScriptContext = (EyeCandyScriptContext)scriptInstance.getContextObject();

        if(!JCMPatchForMTR.isRenderingShadow()) {
            EyecandyBlockEntityWrapper eyecandyWrapper = new EyecandyBlockEntityWrapper(blockEntity);
            scriptInstance.setWrapperObject(eyecandyWrapper);
            scriptInstance.getScript().invokeRenderFunctions(scriptInstance, () -> {
                eyeCandyScriptInstance.updateRenderer(eyeCandyScriptContext.getRenderManager());
                eyeCandyScriptInstance.updateSound(eyeCandyScriptContext.getSoundManager());
                eyeCandyScriptContext.resetForNextRun();
            });
        }

        Direction facing = IBlock.getStatePropertySafe(blockEntity.getLevel(), blockEntity.getBlockPos(), com.lx862.jcm.mod.data.BlockProperties.FACING);

        /* Same cached base as the native path (see above). */
        StoredMatrixTransformations storedMatrixTransformations = transformOf(blockEntity, facing);

        eyeCandyScriptInstance.getRenderManager().invoke(world, storedMatrixTransformations, facing, light);
        eyeCandyScriptInstance.getSoundManager().invoke(world, new ScriptVector3f(blockEntity.getBlockPos()));
    }
}
