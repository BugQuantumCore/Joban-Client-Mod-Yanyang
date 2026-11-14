package com.lx862.jcm.mod.data.pids.preset.components;

import com.google.gson.JsonObject;
import com.lx862.jcm.mod.data.KVPair;
import com.lx862.jcm.mod.data.pids.preset.PIDSContext;
import com.lx862.jcm.mod.data.pids.preset.components.base.PIDSComponent;
import com.lx862.jcm.mod.data.pids.preset.components.base.TextureComponent;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;

public class CustomTextureComponent extends TextureComponent {
    protected final ResourceLocation texture;
    private final int color;
    public CustomTextureComponent(double x, double y, double width, double height, KVPair additionalParam) {
        super(x, y, width, height, additionalParam);
        this.color = additionalParam.getColor("tint", 0xFFFFFF);
        this.texture = ResourceLocation.tryParse(additionalParam.get("textureId", ""));
    }

    @Override
    public void render(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, Direction facing, PIDSContext context) {
        drawTexture(graphicsHolder, guiDrawing, facing, texture, 0, 0, width, height, color + ARGB_BLACK);
    }

    public static PIDSComponent parseComponent(double x, double y, double width, double height, JsonObject jsonObject) {
        return new CustomTextureComponent(x, y, width, height, new KVPair(jsonObject));
    }
}
