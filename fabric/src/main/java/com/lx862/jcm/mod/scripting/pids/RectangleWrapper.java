package com.lx862.jcm.mod.scripting.pids;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.StoredMatrixTransformations;

import static com.lx862.jcm.mod.render.RenderHelper.*;

public class RectangleWrapper extends PIDSDrawCall<RectangleWrapper> {
    protected static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("mtr", "textures/block/white.png");
    protected int color;
    protected boolean naturalLight = false;
    protected QueuedRenderLayer renderType;

    protected RectangleWrapper() {
        super(20, 20);
        this.color = ARGB_WHITE;
        this.renderType = QueuedRenderLayer.LIGHT_2;
    }

    public static RectangleWrapper create() {
        return new RectangleWrapper();
    }

    public static RectangleWrapper create(String comment) {
        return create();
    }

    public RectangleWrapper color(int color) {
        this.color = color;
        return this;
    }

    public RectangleWrapper naturalLight() {
        this.naturalLight = true;
        return this;
    }

    public RectangleWrapper renderType(String renderType) {
        this.renderType = QueuedRenderLayer.valueOf(renderType);
        return this;
    }

    @Override
    public void validate() {
    }

    @Override
    protected void drawTransformed(StoredMatrixTransformations storedMatrixTransformations, Direction facing, int light) {
        MainRenderer.scheduleRender(WHITE_TEXTURE, false, this.renderType, (poseStack, vertexConsumer, offset) -> {
            storedMatrixTransformations.transform(poseStack, offset);
            org.mtr.client.IDrawing.drawTexture(poseStack, vertexConsumer,
                    0, (float)this.h, 0, (float)this.w, (float)this.h, 0,
                    (float)this.w, 0, 0, 0, 0, 0,
                    0, 0, 1, 1,
                    facing, ARGB_BLACK | this.color, this.naturalLight ? light : MAX_RENDER_LIGHT);
            poseStack.popPose();
        });
    }
}
