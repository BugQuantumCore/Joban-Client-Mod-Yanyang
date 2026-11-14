package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.JCMClient;
import com.lx862.jcm.mod.config.JCMClientConfig;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.block.BlockSignalBase;
import org.mtr.client.IDrawing;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.RenderSignalLight2Aspect;
import org.mtr.render.StoredMatrixTransformations;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class SignalBlockInvertedRenderer<T extends BlockSignalBase.BlockEntityBase> extends RenderSignalLight2Aspect<T> {
    private final int proceedColor;
    private final boolean redOnTop;
    public SignalBlockInvertedRenderer(BlockEntityRendererProvider.Context dispatcher, int proceedColor, boolean redOnTop) {
        super(redOnTop, proceedColor);
        this.proceedColor = proceedColor;
        this.redOnTop = redOnTop;
    }

    @Override
    protected void render(StoredMatrixTransformations t, BlockSignalBase.BlockEntityBase signalBlockEntity, net.minecraft.client.multiplayer.ClientLevel clientWorld, float v, int light, int occupiedAspect, boolean isBackSide) {
        if (JCMClientConfig.INSTANCE.disableRendering.value()) return;

        float y = (occupiedAspect > 0) == redOnTop ? 0.4375F : 0.0625F;

        MainRenderer.scheduleRender(ResourceLocation.fromNamespaceAndPath("mtr", "textures/block/white.png"), false, QueuedRenderLayer.LIGHT, (poseStack, vertexConsumer, offset) -> {
            t.transform(poseStack, offset);
            IDrawing.drawTexture(poseStack, vertexConsumer, -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, Direction.UP, occupiedAspect > 0 ? proceedColor : 0xFFFF0000, GraphicsHolder.getDefaultLight());
            poseStack.popPose();
        });
    }
}
