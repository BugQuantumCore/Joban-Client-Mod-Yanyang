package com.lx862.jcm.mixin.mtrscripting;
import com.lx862.jcm.nativeapi.NativeMatrixApplier;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.spongepowered.asm.mixin.*;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(value=GraphicsHolder.class,remap=false)
public abstract class GraphicsHolderMatrixMixin implements NativeMatrixApplier {
    @Shadow @Final
    private PoseStack poseStack;

    /* 2.3.0-1.0.1: re-exposed (was yanyang.11-12) — the backing carrier
       borrows the holder's raw pose to rebuild a GraphicsHolder around a
       private immediate (the drawText discipline), keeping the flush-time
       camera matrices intact; see PSDScriptRenderer#drawBacking. */
    @Override
    public PoseStack jcm$getMatrixStack() {
        return poseStack;
    }

    /* Pooled JOML scratch (render thread): the old path allocated a fresh
       Matrix4f + Matrix3f per native pose application — per pose per car
       per frame. Both objects are fully overwritten by set() before every
       use and only read within this call, so reuse is safe. */
    private static final ThreadLocal<org.joml.Matrix4f> jcm$ScratchMatrix4f =
            ThreadLocal.withInitial(org.joml.Matrix4f::new);
    private static final ThreadLocal<org.joml.Matrix3f> jcm$ScratchMatrix3f =
            ThreadLocal.withInitial(org.joml.Matrix3f::new);

    /* Scratch for the offset pose variant: one small array per render
       thread, reused every call. */
    private static final ThreadLocal<float[]> jcm$NativePoseScratch =
            ThreadLocal.withInitial(() -> new float[16]);

    @Override public void jcm$multiplyNativeMatrix(float[] values, int floatOffset) {
        final float[] scratch = jcm$NativePoseScratch.get();
        System.arraycopy(values, floatOffset, scratch, 0, 16);
        jcm$multiplyNativeMatrix(scratch);
    }
    @Override public void jcm$multiplyNativeMatrix(float[] values) {
        org.joml.Matrix4f matrix=jcm$ScratchMatrix4f.get();
        matrix.set(values);
        org.joml.Matrix3f normal=jcm$ScratchMatrix3f.get();
        normal.set(matrix.m00(),matrix.m01(),matrix.m02(),
                   matrix.m10(),matrix.m11(),matrix.m12(),
                   matrix.m20(),matrix.m21(),matrix.m22());
        normal.invert().transpose();
        poseStack.last().pose().mul(matrix);
        poseStack.last().normal().mul(normal);
    }
}
