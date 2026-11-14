package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.base.DirectionalBlock;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.mesh.OptimizedModelWrapper;
import com.lx862.jcm.mod.resource.ComplexModelStorage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.mtr.block.BlockEntityExtension;
import org.mtr.block.IBlock;
import org.mtr.render.BlockEntityRendererExtension;
import org.mtr.render.StoredMatrixTransformations;

/**
 * Renders a block's complex OBJ model (loaded via {@link ComplexModelStorage})
 * through the MTR 4.1 batched-model pipeline.
 */
public class ComplexModelRenderer<T extends BlockEntityExtension> extends BlockEntityRendererExtension<T> {
    private final ResourceLocation modelLocation;

    public ComplexModelRenderer(ResourceLocation modelLocation, BlockEntityRendererProvider.Context dispatcher) {
        this.modelLocation = modelLocation;
        ComplexModelStorage.requireModel(modelLocation);
    }

    @Override
    public void render(T blockEntity, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, ClientLevel world, LocalPlayer player, float tickDelta, int light, int overlay) {
        StoredMatrixTransformations storedMatrixTransformations = new StoredMatrixTransformations(0.5 + blockEntity.getBlockPos().getX(), blockEntity.getBlockPos().getY(), 0.5 + blockEntity.getBlockPos().getZ());

        final BlockState state = world.getBlockState(blockEntity.getBlockPos());
        final Direction facing = IBlock.getStatePropertySafe(state, DirectionalBlock.FACING);
        final OptimizedModelWrapper model = ComplexModelStorage.getModel(modelLocation);

        if (model != null) {
            StoredMatrixTransformations storedMatrixTransformationsNew = storedMatrixTransformations.copy();
            storedMatrixTransformationsNew.add(matrixStackNew -> {
                org.mtr.tool.Drawing.rotateYDegrees(matrixStackNew, 180.0F - facing.toYRot());
                org.mtr.tool.Drawing.rotateZDegrees(matrixStackNew, 180F);
            });
            model.queue(storedMatrixTransformationsNew, light);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    public boolean shouldRender(T blockEntity, Vec3 position) {
        return true;
    }
}
