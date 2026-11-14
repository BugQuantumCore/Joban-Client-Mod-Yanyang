package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.MTRClient;
import org.mtr.block.IBlock;
import org.mtr.data.ArrivalsCacheClient;
import org.mtr.render.StoredMatrixTransformations;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public abstract class PIDSRenderer<T extends PIDSBlockEntity> extends JCMBlockEntityRenderer<T> {
    public PIDSRenderer(BlockEntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(T blockEntity, GraphicsHolder graphicsHolder, Level world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        PIDSPresetBase pidsPreset = getPreset(blockEntity);
        if(pidsPreset == null) return;

        Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);

        boolean[] rowHidden = new boolean[blockEntity.getRowAmount()];
        boolean[] beRowHidden = blockEntity.getRowHidden();
        for(int i = 0; i < rowHidden.length; i++) {
            rowHidden[i] = pidsPreset.isRowHidden(i) || beRowHidden[i];
        }

        LongImmutableList platforms;
        if(!blockEntity.getPlatformIds().isEmpty()) {
            platforms = new LongImmutableList(blockEntity.getPlatformIds());
        } else {
            LongArrayList closestPlatforms = new LongArrayList();
            MTRClient.findClosePlatform(pos.below(4), 5, e -> closestPlatforms.add(e.getId()));
            platforms = new LongImmutableList(closestPlatforms);
        }

        ObjectArrayList<ArrivalResponse> arrivals = ArrivalsCacheClient.INSTANCE.requestArrivals(platforms);

        StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(0.5 + blockEntity.getBlockPos().getX(), 0.5 + blockEntity.getBlockPos().getY(), 0.5 + blockEntity.getBlockPos().getZ());
        storedMatrixTransformations.add(poseStack -> {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90 - facing.toYRot()));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180));
        });

        renderPIDS(blockEntity, pidsPreset, graphicsHolder, storedMatrixTransformations, world, state, pos, facing, platforms, arrivals, tickDelta, rowHidden, light);
    }

    public abstract void renderPIDS(T blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Level world, BlockState state, BlockPos pos, Direction facing, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden, int light);

    private PIDSPresetBase getPreset(PIDSBlockEntity blockEntity) {
        return PIDSManager.getPreset(blockEntity.getPresetId(), PIDSManager.getPreset(blockEntity.getDefaultPresetId()));
    }
}
