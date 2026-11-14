package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;

/**
 * Wrapper around vanilla text rendering
 */
@Deprecated
public class VanillaTextRenderer implements RenderHelper {
    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        drawInternal(graphicsHolder, text, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
        int textWidth = GraphicsHolder.getTextWidth(text.toMutableText());
        double finalX = text.getTextAlignment().getX(x, textWidth);

        if(text.isForScrollingText()) {
            drawScrollingText(graphicsHolder, text, finalX, y);
        } else {
            MutableComponent finalText = text.toMutableText();
            graphicsHolder.drawText(finalText, (int)finalX, (int)y, text.getTextColor(), false, MAX_RENDER_LIGHT);
        }
    }

    // HEAVY WIP
    public static void drawScrollingText(GraphicsHolder graphicsHolder, TextInfo text, double x, double y) {
    }
}
