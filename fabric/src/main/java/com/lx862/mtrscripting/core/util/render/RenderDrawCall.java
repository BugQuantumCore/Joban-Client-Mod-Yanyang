package com.lx862.mtrscripting.core.util.render;

import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.api.ScriptResultCall;
import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.Matrices;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.mtr.render.StoredMatrixTransformations;

public abstract class RenderDrawCall<T extends RenderDrawCall<?>> implements ScriptResultCall {
    protected StoredMatrixTransformations storedMatrixTransformations;

    /**
     * Called when this draw call leaves a render manager's queue
     * (reset, or a capture taking over the list). Default: no-op.
     *
     * <p>Implementations that pool their instances (e.g. the native model
     * draw call) recycle themselves here; releasing twice must be harmless
     * (guard with a flag). This is what lets the per-frame draw-call churn
     * drop to zero without changing the capture/replay semantics.
     */
    @ApiInternal
    public void release() {
    }

    public T matrices(@ValueNullable Matrices matrices) {
        if(matrices != null) {
            this.storedMatrixTransformations = matrices.compileStoredMatrixTransformations();
        } else {
            this.storedMatrixTransformations = null;
        }
        return (T)this;
    }

    @ApiInternal
    protected T setMatrices(StoredMatrixTransformations m) {
        this.storedMatrixTransformations = m;
        return (T)this;
    }

    @ApiInternal
    public void run(Level world, StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        if (this.storedMatrixTransformations != null) {
            storedMatrixTransformations.add(poseStack -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180)));
            storedMatrixTransformations.add(this.storedMatrixTransformations);
            storedMatrixTransformations.add(poseStack -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-180)));
        }
    }
}
