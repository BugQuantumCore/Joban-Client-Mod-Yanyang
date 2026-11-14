package com.lx862.amberui.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.render.GraphicsHolder;
import net.minecraft.network.chat.MutableComponent;

public abstract class TitledScreen extends AnimatedScreen {
    public static final int TEXT_PADDING = 10;
    public static final int TITLE_SCALE = 2;
    protected double elapsed = 0;

    public TitledScreen(boolean animatable) {
        super(animatable);
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        drawBackground(graphicsHolder, mouseX, mouseY, tickDelta);
        drawTitle(graphicsHolder);
        drawSubtitle(graphicsHolder);
        elapsed += tickDelta / Constants.MC_TICK_PER_SECOND;
        super.render(guiGraphics, mouseX, mouseY, tickDelta);
    }

    public void drawBackground(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        super.renderBackground(graphicsHolder.getGuiGraphics(), mouseX, mouseY, tickDelta);
    }

    private void drawTitle(GraphicsHolder graphicsHolder) {
        int titleHeight = (RenderHelper.lineHeight * TITLE_SCALE);
        MutableComponent titleText = getScreenTitle();
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, TEXT_PADDING, 0);
        graphicsHolder.translate(0, -((titleHeight + TEXT_PADDING) * (1 - animationProgress)), 0);
        graphicsHolder.scale(TITLE_SCALE, TITLE_SCALE, TITLE_SCALE);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(titleText), width / (float)TITLE_SCALE, true);
        graphicsHolder.drawCenteredText(titleText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }

    private void drawSubtitle(GraphicsHolder graphicsHolder) {
        double titleHeight = (RenderHelper.lineHeight * TITLE_SCALE);
        MutableComponent subtitleText = getScreenSubtitle();
        graphicsHolder.push();
        graphicsHolder.translate(width / 2.0, titleHeight * animationProgress, 0);
        graphicsHolder.translate(0, TEXT_PADDING * 1.5, 0);
        RenderHelper.scaleToFit(graphicsHolder, GraphicsHolder.getTextWidth(subtitleText), width, true);
        graphicsHolder.drawCenteredText(subtitleText, 0, 0, 0xFFFFFFFF);
        graphicsHolder.pop();
    }

    public abstract MutableComponent getScreenTitle();
    public abstract MutableComponent getScreenSubtitle();
}
