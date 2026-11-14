package com.lx862.jcm.mod.render.text;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.core.Direction;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;

@Deprecated
public class TextRenderingManager implements RenderHelper {
    public static void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, TextInfo text, double x, double y) {
        drawInternal(graphicsHolder, guiDrawing, text, null, x, y);
    }

    public static void draw(GraphicsHolder graphicsHolder, TextInfo text, Direction facing, double x, double y) {
        drawInternal(graphicsHolder, null, text, facing, x, y);
    }

    private static void drawInternal(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, TextInfo text, Direction facing, double x, double y) {
        if(text.getContent().isEmpty()) return;
        VanillaTextRenderer.draw(graphicsHolder, text, x, y);
    }
}
