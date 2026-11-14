package com.lx862.mtrscripting.mod.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.mapping.LoaderImplGUI;
import com.lx862.amberui.gui.widget.IntegerTextField;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import com.lx862.jcm.mod.render.GraphicsHolder;

public class ConfigIntegerField extends IntegerTextField implements ValidatableWidget {
    private final EyecandyCustomConfig.Entry.ValidationCallback validationCallback;
    private EyecandyCustomConfig.Entry.ValidationResult result = new EyecandyCustomConfig.Entry.ValidationResult(null);
    private int originalValue = 0;

    public ConfigIntegerField(int x, int y, int width, int height, EyecandyCustomConfig.Entry.ValidationCallback validationCallback) {
        super(x, y, width, height, Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        this.validationCallback = validationCallback;
        setResponder(this::updateValidation);
        updateValidation("");
    }

    public void setOriginalValue(int i) {
        this.originalValue = i;
    }

    @Override
    public void setValue(long i) {
        super.setValue(i);
        updateValidation(String.valueOf(i));
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);

        if(!this.result.success() && isHovered()) {
            #if MC_VERSION >= "11903"
                FormattedCharSequence orderedText = LoaderImplGUI.asOrderedText(this.result.validationMessage());
                LoaderImplGUI.setTooltip(Minecraft.getInstance().screen, orderedText);
            #endif
        } else if(this.originalValue != getNumber()) {
            GuiHelper.drawBorder(graphicsHolder, getX(), getY(), getWidth(), getHeight(), 0xFFFFFF88);
        }
    }

    @Override
    public boolean validated() {
        return this.result.success();
    }

    private void updateValidation(String str) {
        EyecandyCustomConfig.Entry.ValidationResult result = validationCallback == null ? new EyecandyCustomConfig.Entry.ValidationResult(null) : validationCallback.validate(str);
        this.result = result;
        setTextColor(this.result.success() ? 0xFFFFFFFF : (0xFF000000 | 0xFF4444));
    }
}
