package com.lx862.mtrscripting.mod.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.mapping.LoaderImplGUI;
import com.lx862.amberui.gui.widget.DoubleTextField;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import com.lx862.jcm.mod.render.GraphicsHolder;

/**
 * The DOUBLE counterpart of {@link ConfigIntegerField}: a validated
 * {@link DoubleTextField} for eyecandy custom config entries of type DOUBLE
 * (pack JSON "type": "DOUBLE" and the legacy-ANTE DoubleSlider compat path).
 * The config screen used to render NO widget at all for such entries.
 */
public class ConfigDoubleField extends DoubleTextField implements ValidatableWidget {
    private final EyecandyCustomConfig.Entry.ValidationCallback validationCallback;
    private EyecandyCustomConfig.Entry.ValidationResult result = new EyecandyCustomConfig.Entry.ValidationResult(null);
    private double originalValue = 0;

    public ConfigDoubleField(int x, int y, int width, int height, EyecandyCustomConfig.Entry.ValidationCallback validationCallback) {
        super(x, y, width, height, -Double.MAX_VALUE, Double.MAX_VALUE, 0);
        this.validationCallback = validationCallback;
        setResponder(this::updateValidation);
        updateValidation("");
    }

    public void setOriginalValue(double d) {
        this.originalValue = d;
    }

    @Override
    public void setValue(double d) {
        super.setValue(d);
        updateValidation(String.valueOf(d));
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
