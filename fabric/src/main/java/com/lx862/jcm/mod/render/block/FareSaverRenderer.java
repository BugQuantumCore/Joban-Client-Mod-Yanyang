package com.lx862.jcm.mod.render.block;

import com.lx862.jcm.mod.block.entity.FareSaverBlockEntity;
import com.lx862.jcm.mod.data.BlockProperties;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.text.TextAlignment;
import com.lx862.jcm.mod.render.text.TextInfo;
import com.lx862.jcm.mod.render.text.TextRenderingManager;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.block.IBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public class FareSaverRenderer extends JCMBlockEntityRenderer<FareSaverBlockEntity> {
    public static final int TEXT_TILT_ANGLE = -10;
    public FareSaverRenderer(BlockEntityRendererProvider.Context dispatcher) {
        super(dispatcher);
    }

    @Override
    public void renderCurated(FareSaverBlockEntity be, GraphicsHolder graphicsHolder, Level world, BlockState state, BlockPos pos, float tickDelta, int light, int i1) {
        Direction facing = IBlock.getStatePropertySafe(state, BlockProperties.FACING);
        if(IBlock.getStatePropertySafe(state, BlockProperties.VERTICAL_PART_3) != IBlock.EnumThird.UPPER) return;

        MutableComponent discountText = TextUtil.literal(be.getPrefix() + be.getDiscount());

        graphicsHolder.push();
        graphicsHolder.translate(0.5, 0.5, 0.5);
        graphicsHolder.scale(0.011F, 0.011F, 0.011F);
        rotateToBlockDirection(graphicsHolder, be);
        graphicsHolder.rotateZDegrees(180);
        graphicsHolder.rotateZDegrees(TEXT_TILT_ANGLE);
        graphicsHolder.translate(5.8, 14, -9.15);

        graphicsHolder.push();
        TextInfo textInfo = new TextInfo(discountText)
                .withFont("mtr:mtr")
                .withColor(ARGB_WHITE)
                .withTextAlignment(TextAlignment.CENTER);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(textInfo.toMutableText()), 12, true, 12);
        TextRenderingManager.draw(graphicsHolder, textInfo, facing, 0, 0);
        graphicsHolder.pop();

        graphicsHolder.pop();
    }
}
