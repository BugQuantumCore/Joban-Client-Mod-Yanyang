package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.mtr.block.BlockEntityExtension;
import org.mtr.block.IBlock;
import org.mtr.render.BlockEntityRendererExtension;
import com.lx862.jcm.mod.render.GraphicsHolder;

public abstract class JCMBlockEntityRenderer<T extends BlockEntityExtension> extends BlockEntityRendererExtension<T> implements RenderHelper {
    public JCMBlockEntityRenderer(BlockEntityRendererProvider.Context dispatcher) {
    }

    /**
     * MTR 4.1 render entry point. Bridges into the legacy JCM
     * {@link GraphicsHolder} surface so subclass renderers keep working
     * unmodified.
     */
    @Override
    public final void render(T blockEntity, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, ClientLevel world, LocalPlayer player, float tickDelta, int light, int overlay) {
        render(blockEntity, tickDelta, new GraphicsHolder(matrixStack, vertexConsumerProvider), light, overlay);
    }

    /**
     * Legacy JCM render hook: same parameter order the 4.0-era mapping layer
     * provided, minus the world/player arguments the curated hook re-derives.
     */
    public void render(T blockEntity, float tickDelta, GraphicsHolder graphicsHolder, int light, int i1) {
        try {
            Level world = blockEntity.getLevel();
            BlockPos pos = blockEntity.getBlockPos();
            if(JCMClientConfig.INSTANCE.disableRendering.value() || world == null) return;

            BlockState state = world.getBlockState(pos); /* one lookup: this fetched the same state twice per BE per frame */
            if(state.isAir()) return;

            renderCurated(blockEntity, graphicsHolder, world, state, pos, tickDelta, light, i1);
        } catch (Exception e) {
            JCMLogger.error("An exception occurred while rendering Block Entity!");
            JCMLogger.error("This is a bug, please report to the developers of Joban Client Mod!");
            throw new Error("An exception occurred while rendering Block Entity.", e);
        }
    }

    /**
     * Same as the default block entity render method, but only called in safe condition and when rendering are not disabled
     */
    public abstract void renderCurated(T blockEntity, GraphicsHolder graphicsHolder, Level world, BlockState state, BlockPos pos, float tickDelta, int light, int i1);

    public void rotateToBlockDirection(GraphicsHolder graphicsHolder, T blockEntity) {
        Level world = blockEntity.getLevel();
        if(world != null) {
            BlockState state = world.getBlockState(blockEntity.getBlockPos());
            Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);
            graphicsHolder.rotateYDegrees(-facing.toYRot());
        }
    }
}
