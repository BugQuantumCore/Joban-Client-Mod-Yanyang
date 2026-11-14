package com.lx862.mtrscripting.mod.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.widget.Checkbox;
import com.lx862.jcm.mod.render.GraphicsHolder;

import java.util.function.Consumer;

public class ConfigCheckbox extends Checkbox {
    private boolean originalValue = false;

    public ConfigCheckbox(int x, int y, int width, int height, Consumer<Boolean> callback) {
        super(x, y, width, height, false, checkbox -> {
            if(callback != null) callback.accept(checkbox.isChecked());
        });
    }

    public void setOriginalValue(boolean bl) {
        this.originalValue = bl;
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);

        if(this.originalValue != isChecked()) {
            final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
            GuiHelper.drawBorder(graphicsHolder, getX(), getY(), getWidth()-3, getHeight()-3, 0xFFFFFF88);
        }
    }
}
