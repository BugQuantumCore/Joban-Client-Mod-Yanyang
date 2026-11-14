package com.lx862.jcm.mod.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.mtr.tool.Drawing;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;

/**
 * Compatibility shim for the MTR 4.0-era
 * {@code com.lx862.jcm.mod.render.GuiDrawing}: immediate-mode rectangle and
 * normalized-UV texture quads drawn through the current
 * {@link GraphicsHolder} transformation, using the vanilla immediate buffer.
 * Legacy begin/draw/finish call shape preserved.
 */
public final class GuiDrawing {

        private final GraphicsHolder graphicsHolder;

        public GuiDrawing(GraphicsHolder graphicsHolder) {
                this.graphicsHolder = graphicsHolder;
        }

        public void beginDrawingRectangle() {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
        }

        public void drawRectangle(double x1, double y1, double x2, double y2, int color) {
                drawRectangle(x1, y1, 0, x2, y2, 0, color);
        }

        public void drawRectangle(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
                final PoseStack matrixStack = graphicsHolder.getPoseStack();
                RenderSystem.setShader(() -> GameRenderer.getPositionColorShader());
                final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                new Drawing(matrixStack, builder)
                                .setVertices(x1, y1, z1, x2, y1, z1, x2, y2, z2, x1, y2, z2)
                                .setColor(color)
                                .draw();
                BufferUploader.drawWithShader(builder.build());
        }

        public void finishDrawingRectangle() {
                RenderSystem.disableBlend();
        }

        public void beginDrawingTexture(ResourceLocation texture) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(() -> GameRenderer.getPositionTexShader());
                RenderSystem.setShaderTexture(0, texture);
        }

        public void drawTexture(double x1, double y1, double x2, double y2, float u1, float v1, float u2, float v2) {
                final PoseStack matrixStack = graphicsHolder.getPoseStack();
                final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                new Drawing(matrixStack, builder)
                                .setVertices(x1, y1, x2, y2)
                                .setColor(0xFFFFFFFF)
                                .setUv(u1, v1, u2, v2)
                                .draw();
                BufferUploader.drawWithShader(builder.build());
        }

        public void finishDrawingTexture() {
                RenderSystem.disableBlend();
        }
}
