package com.lx862.amberui.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.GraphicsHolder;

public abstract class AnimatedScreen extends ScreenBase {
    protected double linearAnimationProgress = 0;
    protected double animationProgress;
    protected boolean closing = false;
    private final boolean shouldAnimate;

    public AnimatedScreen(boolean animatable) {
        super();
        this.shouldAnimate = animatable;
        this.animationProgress = animatable ? 0 : 1;
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, tickDelta);
        double frameDelta = (tickDelta / Constants.MC_TICK_PER_SECOND);
        if(!shouldAnimate) {
            linearAnimationProgress = 1;
        } else {
            linearAnimationProgress = closing ? Math.max(0, linearAnimationProgress - frameDelta) : Math.min(1, linearAnimationProgress + frameDelta);

            if(linearAnimationProgress <= 0 && closing) {
                onClose();
            }
        }
        animationProgress = getEaseAnimation();
    }

    @Override
    public void onClose() {
        if(closing || !shouldAnimate) {
            super.onClose();
        } else {
            closing = true;
        }
    }

    private double getEaseAnimation() {
        double x = linearAnimationProgress;
        return 1 - Math.pow(1 - x, 5);
    }
}
