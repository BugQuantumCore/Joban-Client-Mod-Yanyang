package com.lx862.amberui.gui.screen;

import com.lx862.jcm.mod.Constants;
import com.lx862.jcm.mod.render.GraphicsHolder;

public abstract class AnimatedScreen extends ScreenBase {
    protected double linearAnimationProgress = 0;
    protected double animationProgress;
    protected boolean closing = false;
    private final boolean shouldAnimate;
    private long lastFrameMillis = -1;

    public AnimatedScreen(boolean animatable) {
        super();
        this.shouldAnimate = animatable;
        this.animationProgress = animatable ? 0 : 1;
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, tickDelta);
        /* Real elapsed time, not tickDelta: tickDelta is the partial tick
           (0..1), so the old frameDelta advanced the animation ~partialTick/20
           PER FRAME — 4x faster at 240 fps than at 60 fps. */
        final long now = net.minecraft.Util.getMillis();
        final double frameDelta = lastFrameMillis < 0 ? 0 : Math.min(0.25, (now - lastFrameMillis) / 1000.0);
        lastFrameMillis = now;
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
