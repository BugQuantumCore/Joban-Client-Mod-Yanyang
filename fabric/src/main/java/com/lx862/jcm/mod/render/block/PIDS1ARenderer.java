package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.LCDPIDSBlockEntity;
import com.lx862.jcm.mod.block.entity.PIDS1ABlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.render.StoredMatrixTransformations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PIDS1ARenderer extends PIDSRenderer<PIDS1ABlockEntity> {
    public PIDS1ARenderer(BlockEntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(PIDS1ABlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Level world, BlockState state, BlockPos pos, Direction facing, org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden, int light) {
        storedMatrixTransformations.add(graphicsHolderNew -> {
            graphicsHolderNew.translate(-0.47, -0.155, -0.130);
        });
        pidsPreset.render(blockEntity, graphicsHolder, storedMatrixTransformations, world, blockEntity.getBlockPos(), facing, targetPlatformIds, arrivals, rowHidden, tickDelta, 0, 0, 186, 60, light);
    }

    @Override
    public boolean shouldRenderOffScreen(PIDS1ABlockEntity blockEntity) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.shouldRenderOffScreen(blockEntity);
    }

    @Override
    public boolean shouldRender(PIDS1ABlockEntity blockEntity, Vec3 position) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.shouldRender(blockEntity, position);
    }
}
