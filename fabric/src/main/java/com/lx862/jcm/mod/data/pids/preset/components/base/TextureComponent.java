package com.lx862.jcm.mod.data.pids.preset.components.base;

import com.lx862.jcm.mod.data.KVPair;
import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;

public abstract class TextureComponent extends PIDSComponent {
    public TextureComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height);
    }

    protected void drawTexture(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, ResourceLocation identifier, double offsetX, double offsetY, double width, double height, int color) {
        double finalX = this.x + offsetX;
        double finalY = this.y + offsetY;
        graphicsHolder.push();
        graphicsHolder.createVertexConsumer(RenderType.text(identifier));
        if(guiDrawing != null) {
            GuiHelper.drawTexture(guiDrawing, identifier, finalX, finalY, width, height);
        } else {
            RenderHelper.drawTexture(graphicsHolder, (float)(finalX), (float)(finalY), 0, (float)width, (float)height, facing, color, RenderHelper.MAX_RENDER_LIGHT);
        }
        graphicsHolder.pop();
    }
}
