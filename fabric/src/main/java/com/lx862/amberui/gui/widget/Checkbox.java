package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.render.GraphicsHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Compatibility checkbox with the 4.0-era mapping constructor and the
 * programmatic {@code setChecked} the JCM GUI code relies on.
 *
 * <p>4.0-era JCM rendered this widget through MTR's mapping layer
 * {@code CheckboxWidgetExtension}, which is exactly the vanilla checkbox.
 * The port therefore draws the vanilla 1.21.1 GUI-atlas checkbox sprites
 * ({@code widget/checkbox[_selected][_highlighted]}) through
 * {@link GuiGraphics#blitSprite} with the same selected/focused selection
 * rule the vanilla {@code Checkbox#renderWidget} uses, so the settings
 * screen looks identical to the original mod's.</p>
 */
public class Checkbox extends AbstractWidget {

        private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
        private static final ResourceLocation CHECKBOX_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
        private static final ResourceLocation CHECKBOX_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
        private static final ResourceLocation CHECKBOX_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox");

        private boolean checked;
        private final Consumer<Checkbox> callback;

        public Checkbox(int x, int y, int width, int height, boolean checked, Consumer<Checkbox> callback) {
                super(x, y, width, height, CommonComponents.EMPTY);
                this.checked = checked;
                this.callback = callback;
        }

        public void setChecked(boolean checked) {
                this.checked = checked;
        }

        public boolean isChecked() {
                return checked;
        }

        public void setMessage(MutableComponent message) {
                super.setMessage(message);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
                super.onClick(mouseX, mouseY);
                checked = !checked;
                if (callback != null) {
                        callback.accept(this);
                }
        }

        /** Same sprite selection rule as the vanilla {@code Checkbox}. */
        private ResourceLocation sprite() {
                return checked
                                ? (isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE)
                                : (isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
                /* Statement-for-statement the vanilla 1.21.1 Checkbox#renderWidget:
                 * depth test on, white color with the widget alpha, blend on,
                 * blitSprite the 20x20 checkbox, blend off. The original JCM
                 * screen showed exactly these sprites through MTR 4.0's
                 * CheckboxWidgetExtension, so the settings screen matches the
                 * original mod again. */
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                final int size = getHeight();
#if MC_VERSION == "12104"
                /* 1.21.4: blitSprite takes the RenderType factory
                 * (RenderType::guiTextured) and an ARGB color; the vanilla
                 * Checkbox passes ARGB.white(alpha). This widget never
                 * changes its alpha, so the stock opaque white is used. */
                guiGraphics.blitSprite(net.minecraft.client.renderer.RenderType::guiTextured,
                                sprite(), getX(), getY(), size, size, net.minecraft.util.ARGB.white(1.0F));
#else
                /* 1.21.1: plain sprite blit; the vanilla Checkbox tints via
                 * setColor(1, 1, 1, alpha) - with the stock alpha of 1.0 the
                 * sprite renders untinted either way. */
                guiGraphics.blitSprite(sprite(), getX(), getY(), size, size);
#endif
                RenderSystem.disableBlend();

                if (getMessage() != null && !getMessage().getString().isEmpty()) {
                        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
                        final int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
                        graphicsHolder.drawText(getMessage().getString(), getX() + size + 4,
                                        getY() + (size - 8) / 2, color, true, GraphicsHolder.getDefaultLight());
                }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
                this.defaultButtonNarrationText(narrationElementOutput);
        }
}
