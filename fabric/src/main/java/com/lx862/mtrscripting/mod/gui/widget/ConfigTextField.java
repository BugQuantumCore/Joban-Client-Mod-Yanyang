package com.lx862.mtrscripting.mod.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.mapping.LoaderImplGUI;
import com.lx862.amberui.gui.ClipStack;
import com.lx862.amberui.gui.widget.AlwaysRenderedTextField;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FormattedCharSequence;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;
import com.lx862.jcm.mod.compat.TextHelper;
import com.lx862.jcm.mod.render.TextCase;

import java.util.Objects;
import java.util.function.Consumer;
import com.lx862.jcm.mod.render.TextCase;

public class ConfigTextField extends AlwaysRenderedTextField implements ValidatableWidget {
    private final EyecandyCustomConfig.Entry.ValidationCallback validationCallback;
    private Consumer<String> changedListener = null;
    private EyecandyCustomConfig.Entry.ValidationResult result = new EyecandyCustomConfig.Entry.ValidationResult(null);
    private String originalValue = null;

    public ConfigTextField(int x, int y, int width, int height, int maxLength, TextCase textCase, String filter, String suggestion, EyecandyCustomConfig.Entry.ValidationCallback validationCallback) {
        super(x, y, width, height, maxLength, textCase, filter, suggestion);
        this.validationCallback = validationCallback;
        setResponder((newStr) -> {
            if(changedListener != null) {
                changedListener.accept(newStr);
            }
            updateValidation(newStr);
        });
        updateValidation("");
    }

    public void setOriginalValue(String str) {
        this.originalValue = str;
    }

    public void onChange(Consumer<String> callback) {
        this.changedListener = callback;
    }

    @Override
    public void setValue(String str) {
        super.setValue(str);
        updateValidation(str);
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
        ClipStack.ensureStateCorrect();
        super.renderWidget(guiGraphics, mouseX, mouseY, delta);

        String content = getValue();
        if(!this.result.success() && isHovered()) {
            #if MC_VERSION >= "11903"
                FormattedCharSequence orderedText = LoaderImplGUI.asOrderedText(this.result.validationMessage());
                LoaderImplGUI.setTooltip(Minecraft.getInstance().screen, orderedText);
            #endif
        } else if(!Objects.equals(this.originalValue, content)) {
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
