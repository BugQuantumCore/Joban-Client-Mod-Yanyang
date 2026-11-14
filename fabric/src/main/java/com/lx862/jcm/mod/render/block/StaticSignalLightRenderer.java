package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import org.mtr.block.BlockEntityExtension;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.MTR;
import org.mtr.block.BlockSignalBase;
import org.mtr.block.IBlock;
import org.mtr.client.IDrawing;
import org.mtr.render.MoreRenderLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class StaticSignalLightRenderer<T extends BlockEntityExtension> extends JCMBlockEntityRenderer<T> {
        private final boolean drawOnTop;
        private final int color;

        public StaticSignalLightRenderer(BlockEntityRendererProvider.Context dispatcher, int color, boolean drawOnTop) {
            super(dispatcher);
            this.color = color;
            this.drawOnTop = drawOnTop;
        }

        @Override
        public final void renderCurated(T entity, GraphicsHolder graphicsHolder, Level world, BlockState state, BlockPos pos, float tickDelta, int light, int overlay) {
            final Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);
            final float angle = facing.toYRot() + (IBlock.getStatePropertySafe(state, BlockSignalBase.IS_22_5) == BlockSignalBase.EnumBooleanInverted.TRUE ? 22.5F : 0.0F) + (IBlock.getStatePropertySafe(state, BlockSignalBase.IS_45) == BlockSignalBase.EnumBooleanInverted.TRUE ? 45.0F : 0.0F);

            graphicsHolder.push();
            graphicsHolder.translate(0.5, 0, 0.5);

            graphicsHolder.push();
            graphicsHolder.rotateYDegrees(-angle);
            graphicsHolder.createVertexConsumer(MoreRenderLayers.getLight(ResourceLocation.fromNamespaceAndPath("mtr", "textures/block/white.png"), false));
            drawSignalLight(graphicsHolder, entity, tickDelta, facing);
            graphicsHolder.pop();

            graphicsHolder.pop();
        }

        private void drawSignalLight(GraphicsHolder graphicsHolder, T entity, float tickDelta, Direction facing) {
            final float y = drawOnTop ? 0.4375F : 0.0625F;
            IDrawing.drawTexture(graphicsHolder.getPoseStack(), graphicsHolder.getVertexConsumer(), -0.125F, y, -0.19375F, 0.125F, y + 0.25F, -0.19375F, facing.getOpposite(), color, RenderHelper.MAX_RENDER_LIGHT);
        }
}
