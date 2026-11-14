package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.Consumer;

/**
 * Compatibility checkbox with the 4.0-era mapping constructor and the
 * programmatic {@code setChecked} the JCM GUI code relies on. The 4.0
 * implementation drew a sprite out of the JCM texture atlas; this port draws
 * a plain bordered box with a filled inner square when checked, which keeps
 * the widget texture-free.
 */
public class Checkbox extends AbstractWidget {

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

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
                final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
                final GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
                final int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
                final int size = getHeight();

                // Outer border
                guiDrawing.drawRectangle(getX(), getY(), getX() + size, getY() + 1, color);
                guiDrawing.drawRectangle(getX(), getY() + size - 1, getX() + size, getY() + size, color);
                guiDrawing.drawRectangle(getX(), getY(), getX() + 1, getY() + size, color);
                guiDrawing.drawRectangle(getX() + size - 1, getY(), getX() + size, getY() + size, color);

                // Inner fill when checked
                if (checked) {
                        final int inset = Math.max(2, size / 4);
                        guiDrawing.drawRectangle(getX() + inset, getY() + inset,
                                        getX() + size - inset, getY() + size - inset, color);
                }

                if (getMessage() != null && !getMessage().getString().isEmpty()) {
                        graphicsHolder.drawText(getMessage().getString(), getX() + size + 2,
                                        getY() + (size - 8) / 2, color, true, GraphicsHolder.getDefaultLight());
                }
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
                this.defaultButtonNarrationText(narrationElementOutput);
        }
}
