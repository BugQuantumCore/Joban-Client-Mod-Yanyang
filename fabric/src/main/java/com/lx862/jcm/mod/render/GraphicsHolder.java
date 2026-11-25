package com.lx862.jcm.mod.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import org.mtr.client.IDrawing;
import org.mtr.data.IGui;
import org.mtr.font.FontRenderHelper;
import org.mtr.font.FontRenderOptions;
import org.mtr.tool.Drawing;

import java.awt.*;
import java.util.List;
import java.util.function.Consumer;
import com.lx862.jcm.mod.render.GraphicsHolder;

/**
 * Compatibility shim for the MTR 4.0-era {@code com.lx862.jcm.mod.render.GraphicsHolder}.
 * Keeps the legacy immediate-mode call surface used throughout the JCM render
 * code, backed by the MTR 4.1 rendering stack ({@link PoseStack} +
 * {@link MultiBufferSource} + {@link Drawing}/{@link IDrawing} +
 * {@link FontRenderHelper}).
 */
public final class GraphicsHolder {

        private final PoseStack poseStack;
        private final MultiBufferSource vertexConsumerProvider;
        private final GuiGraphics guiGraphics;
        private VertexConsumer vertexConsumer;

        public GraphicsHolder(PoseStack poseStack, MultiBufferSource vertexConsumerProvider) {
                this.poseStack = poseStack;
                this.vertexConsumerProvider = vertexConsumerProvider;
                this.guiGraphics = null;
        }

        /**
         * Text-only holder: draws through {@link FontRenderHelper} with the
         * standalone render path (no buffer source). Used by the PIDS text
         * wrappers, which run inside {@code MainRenderer#scheduleTextRender}.
         */
        public GraphicsHolder(PoseStack poseStack) {
                this(poseStack, null);
        }

        /**
         * GUI-mode holder backed by a {@link GuiGraphics}; text draws go through
         * the vanilla font renderer, matching the legacy DrawContext-backed holder.
         */
        public GraphicsHolder(GuiGraphics guiGraphics) {
                this.poseStack = guiGraphics.pose();
                // GuiGraphics#bufferSource was renamed in 1.21.2; the render
                // buffer source works on every supported version (this is
                // the same source MTR's own GUI draw paths use).
                this.vertexConsumerProvider = Minecraft.getInstance().renderBuffers().bufferSource();
                this.guiGraphics = guiGraphics;
        }

        public static void createInstanceSafe(PoseStack poseStack, MultiBufferSource vertexConsumerProvider, Consumer<GraphicsHolder> consumer) {
                consumer.accept(new GraphicsHolder(poseStack, vertexConsumerProvider));
        }

        public static int getDefaultLight() {
                return IGui.DEFAULT_LIGHT;
        }

        /** The wrapped GUI-mode {@link GuiGraphics}, or null in world render mode. */
        public GuiGraphics getGuiGraphics() {
                return guiGraphics;
        }

        public PoseStack getPoseStack() {
                return poseStack;
        }

        public MultiBufferSource getVertexConsumerProvider() {
                return vertexConsumerProvider;
        }

        public VertexConsumer getVertexConsumer() {
                return vertexConsumer;
        }

        public void push() {
                poseStack.pushPose();
        }

        public void pop() {
                poseStack.popPose();
        }

        public void translate(double x, double y, double z) {
                poseStack.translate(x, y, z);
        }

        public void scale(float x, float y, float z) {
                poseStack.scale(x, y, z);
        }

        public void rotateXRadians(float angle) {
                Drawing.rotateXRadians(poseStack, angle);
        }

        public void rotateYRadians(float angle) {
                Drawing.rotateYRadians(poseStack, angle);
        }

        public void rotateZRadians(float angle) {
                Drawing.rotateZRadians(poseStack, angle);
        }

        public void rotateXDegrees(float angle) {
                Drawing.rotateXDegrees(poseStack, angle);
        }

        public void rotateYDegrees(float angle) {
                Drawing.rotateYDegrees(poseStack, angle);
        }

        public void rotateZDegrees(float angle) {
                Drawing.rotateZDegrees(poseStack, angle);
        }

        public void drawText(MutableComponent text, int x, int y, int color, boolean shadow, int light) {
                if (guiGraphics != null) {
                        /* Pass the component itself so its style (font, bold,
                           italic, ...) survives instead of being flattened by
                           text.getString() through the String overload. */
                        guiGraphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
                        return;
                }
                /* World mode: the MTR font path is string-only, so forward the
                   component's style font (defaults to minecraft:default when
                   unset) and mirror the String path option-for-option. */
                final ResourceLocation styleFont = text.getStyle().getFont();
                if (vertexConsumerProvider == null) {
                        FontRenderHelper.render(poseStack, text.getString(), FontRenderOptions.builder()
                                        .offsetX(x).offsetY(y)
                                        .font(styleFont)
                                        .color(new Color(color, true))
                                        .drawShadow(shadow)
                                        .light(light)
                                        .build());
                        return;
                }
                FontRenderHelper.render(poseStack, vertexConsumerProvider, text.getString(), FontRenderOptions.builder()
                                .offsetX(x).offsetY(y)
                                .font(styleFont)
                                .color(new Color(color, true))
                                .drawShadow(shadow)
                                .light(light)
                                .build());
        }

        public void drawText(FormattedCharSequence text, int x, int y, int color, boolean shadow, int light) {
                if (guiGraphics != null) {
                        guiGraphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
                } else {
                        /* World mode: the previous fallback drew String.valueOf(text) —
                           an object toString like
                           net.minecraft.util.FormattedCharSequence$$Lambda/0x…@1a2b —
                           instead of the text itself (KCR station name signs rendered
                           garbage). Extract the plain string; glyphs are preserved and
                           the custom MTR font path (FontRenderHelper) is untouched. */
                        drawText(toPlainString(text), x, y, color, shadow, light);
                }
        }

        /** Flattens a styled {@link FormattedCharSequence} back into its plain string. */
        private static String toPlainString(FormattedCharSequence text) {
                final StringBuilder sb = new StringBuilder();
                text.accept((index, style, codePoint) -> {
                        sb.appendCodePoint(codePoint);
                        return true;
                });
                return sb.toString();
        }

        public void drawText(String text, int x, int y, int color, boolean shadow, int light) {
                if (guiGraphics != null) {
                        guiGraphics.drawString(Minecraft.getInstance().font, text, x, y, color, shadow);
                        return;
                }
                if (vertexConsumerProvider == null) {
                        FontRenderHelper.render(poseStack, text, FontRenderOptions.builder()
                                        .offsetX(x).offsetY(y)
                                        .color(new Color(color, true))
                                        .drawShadow(shadow)
                                        .light(light)
                                        .build());
                        return;
                }
                FontRenderHelper.render(poseStack, vertexConsumerProvider, text, FontRenderOptions.builder()
                                .offsetX(x).offsetY(y)
                                .color(new Color(color, true))
                                .drawShadow(shadow)
                                .light(light)
                                .build());
        }

        public void drawCenteredText(String text, int x, int y, int color) {
                if (guiGraphics != null) {
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y, color);
                        return;
                }
                if (vertexConsumerProvider == null) {
                        FontRenderHelper.render(poseStack, text, FontRenderOptions.builder()
                                        .offsetX(x).offsetY(y)
                                        .color(new Color(color, true))
                                        .horizontalTextAlignment(FontRenderOptions.Alignment.CENTER)
                                        .build());
                        return;
                }
                FontRenderHelper.render(poseStack, vertexConsumerProvider, text, FontRenderOptions.builder()
                                .offsetX(x).offsetY(y)
                                .color(new Color(color, true))
                                .horizontalTextAlignment(FontRenderOptions.Alignment.CENTER)
                                .build());
        }

        public void drawCenteredText(MutableComponent text, int x, int y, int color) {
                if (guiGraphics != null) {
                        /* Same as drawText above: keep the component's style
                           instead of flattening it to a plain string. */
                        guiGraphics.drawCenteredString(Minecraft.getInstance().font, text, x, y, color);
                        return;
                }
                final ResourceLocation styleFont = text.getStyle().getFont();
                if (vertexConsumerProvider == null) {
                        FontRenderHelper.render(poseStack, text.getString(), FontRenderOptions.builder()
                                        .offsetX(x).offsetY(y)
                                        .font(styleFont)
                                        .color(new Color(color, true))
                                        .horizontalTextAlignment(FontRenderOptions.Alignment.CENTER)
                                        .build());
                        return;
                }
                FontRenderHelper.render(poseStack, vertexConsumerProvider, text.getString(), FontRenderOptions.builder()
                                .offsetX(x).offsetY(y)
                                .font(styleFont)
                                .color(new Color(color, true))
                                .horizontalTextAlignment(FontRenderOptions.Alignment.CENTER)
                                .build());
        }

        public static int getTextWidth(MutableComponent text) {
                return getTextWidth(text.getString());
        }

        public static int getTextWidth(FormattedCharSequence orderedText) {
                return Minecraft.getInstance().font.width(orderedText);
        }

        public static int getTextWidth(String text) {
                return Minecraft.getInstance().font.width(text);
        }

        public static List<FormattedCharSequence> wrapLines(MutableComponent text, int width) {
                return Minecraft.getInstance().font.split(text, width);
        }

        /**
         * Sets the render layer used by the immediate-mode texture/line draws on
         * this holder, mirroring the legacy semantics of
         * {@code GraphicsHolder.createVertexConsumer}.
         */
        public void createVertexConsumer(RenderType renderLayer) {
                vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);
        }

        public void drawLineInWorld(float x1, float y1, float z1, float x2, float y2, float z2, int color) {
                if (vertexConsumer != null) {
                        IDrawing.drawLineInWorld(poseStack, vertexConsumer, x1, y1, z1, x2, y2, z2, color);
                }
        }

        public void drawTextureInWorld(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction direction, int color, int light) {
                if (vertexConsumer != null) {
                        IDrawing.drawTexture(poseStack, vertexConsumer, x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4, u1, v1, u2, v2, direction, color, light);
                }
        }

        public void renderEntity(Entity entity, double x, double y, double z, float yaw, float tickDelta, int light) {
                final EntityRenderDispatcher entityRenderDispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
#if MC_VERSION == "12104"
                // 1.21.2 removed the tickDelta parameter.
                entityRenderDispatcher.render(entity, x, y, z, yaw, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), light);
#else
                entityRenderDispatcher.render(entity, x, y, z, yaw, tickDelta, poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), light);
#endif
        }
}
