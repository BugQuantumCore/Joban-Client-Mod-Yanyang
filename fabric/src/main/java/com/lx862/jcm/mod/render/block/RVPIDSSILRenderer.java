package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.RVPIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.render.StoredMatrixTransformations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class RVPIDSSILRenderer<T extends PIDSBlockEntity> extends PIDSRenderer<T> {
    public RVPIDSSILRenderer(BlockEntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(T blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Level world, BlockState state, BlockPos pos, Direction facing, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden, int light) {
        storedMatrixTransformations.add(poseStack -> {
            poseStack.translate(-0.21, -0.155, -0.650);
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(22.5f));
        });

        pidsPreset.render(blockEntity, graphicsHolder, storedMatrixTransformations, world, blockEntity.getBlockPos(), facing, targetPlatformIds, arrivals, rowHidden, tickDelta, 0, 0, 136, 76, light);
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.shouldRenderOffScreen(blockEntity);
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 position) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.shouldRender(blockEntity, position);
    }
}
