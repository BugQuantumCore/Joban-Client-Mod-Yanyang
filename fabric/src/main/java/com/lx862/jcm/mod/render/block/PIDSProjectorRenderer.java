package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.block.entity.PIDSProjectorBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMUtil;
import org.mtr.core.operation.ArrivalResponse;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.render.MainRenderer;
import org.mtr.client.IDrawing;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.StoredMatrixTransformations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class PIDSProjectorRenderer extends PIDSRenderer<PIDSProjectorBlockEntity> {
    public PIDSProjectorRenderer(BlockEntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderPIDS(PIDSProjectorBlockEntity blockEntity, PIDSPresetBase pidsPreset, GraphicsHolder graphicsHolder, StoredMatrixTransformations storedMatrixTransformations, Level world, BlockState state, BlockPos pos, Direction facing, LongImmutableList targetPlatformIds, ObjectArrayList<ArrivalResponse> arrivals, float tickDelta, boolean[] rowHidden, int light) {
        float appliedScale = (float)blockEntity.getScale();
        float offsetX = (float)(0.5 - blockEntity.getOffsetX());
        float offsetY = (float)(0.5 + blockEntity.getOffsetY());
        float offsetZ = (float)(-0.5 - blockEntity.getOffsetZ());
        /* Null player during disconnect/dimension change frames used to NPE
           here (this renderer runs at any distance via shouldRender). */
        LocalPlayer player = Minecraft.getInstance().player;
        boolean showOutline = player != null && JCMUtil.playerHoldingBrush((Player)player);

        storedMatrixTransformations.add(poseStack -> {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));

            poseStack.translate(-0.5 + blockEntity.getOffsetX(), -0.5 - blockEntity.getOffsetY(), 0.5 + blockEntity.getOffsetZ());
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees((float)blockEntity.getRotateX()));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)blockEntity.getRotateY()));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((float)blockEntity.getRotateZ()));
        });

        // Draw projection effect
        if(showOutline && blockEntity.getRotateX() == 0 && blockEntity.getRotateY() == 0 && blockEntity.getRotateZ() == 0) {
            MainRenderer.scheduleRender(QueuedRenderLayer.LINES, (poseStack, vertexConsumer, offset) -> {
                storedMatrixTransformations.transform(poseStack, offset);

                IDrawing.drawLineInWorld(poseStack, vertexConsumer, offsetX, offsetY, offsetZ, 0, 0, 0, 0xFFFF0000);
                IDrawing.drawLineInWorld(poseStack, vertexConsumer, offsetX, offsetY, offsetZ, 0 + (1.785f * appliedScale), 0, 0, 0xFFFF0000);

                IDrawing.drawLineInWorld(poseStack, vertexConsumer, offsetX, offsetY, offsetZ, 0, 0 + (1 * appliedScale), 0, 0xFFFF0000);
                IDrawing.drawLineInWorld(poseStack, vertexConsumer, offsetX, offsetY, offsetZ, 0 + (1.785f * appliedScale), 0 + (1 * appliedScale), 0, 0xFFFF0000);
                poseStack.popPose();
            });
        }

        StoredMatrixTransformations newMatrices = storedMatrixTransformations.copy();
        newMatrices.add(poseStack -> {
            poseStack.scale(1.26315f, 1.26315f, 1.26315f);
            poseStack.scale(appliedScale, appliedScale, appliedScale);
        });

        pidsPreset.render(blockEntity, graphicsHolder, newMatrices.copy(), world, blockEntity.getBlockPos(), facing, targetPlatformIds, arrivals, rowHidden, tickDelta, 0, 0, 136, 76, light);

        // Border
        if(showOutline) {
            MainRenderer.scheduleRender(Constants.id("textures/block/light_1.png"), false, QueuedRenderLayer.LIGHT, (poseStack, vertexConsumer, offset) -> {
                newMatrices.transform(poseStack, offset);
                poseStack.scale(PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE, PIDSPresetBase.BASE_SCALE);
                org.mtr.client.IDrawing.drawTexture(poseStack, vertexConsumer, -1, -1, 0.1f, 137, 77, 0.1f, facing.getOpposite(), 0xFFFF0000, MAX_RENDER_LIGHT);
                poseStack.popPose();
            });
        }
    }

    @Override
    public boolean shouldRenderOffScreen(PIDSProjectorBlockEntity blockEntity) {
        if(blockEntity.getScale() >= 4) return true;
        if(blockEntity.getOffsetX() >= 4) return true;
        if(blockEntity.getOffsetY() >= 4) return true;
        if(blockEntity.getOffsetZ() >= 4) return true;
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.shouldRenderOffScreen(blockEntity);
    }

    @Override
    public boolean shouldRender(PIDSProjectorBlockEntity blockEntity, Vec3 position) {
        PIDSPresetBase pidsPreset = PIDSManager.getPreset(blockEntity.getPresetId());
        if(pidsPreset != null && pidsPreset.outOfBoundAllowed()) return true;
        return super.shouldRender(blockEntity, position);
    }
}