package com.lx862.amberui.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.gui.screens.Screen;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.TextCase;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import net.minecraft.network.chat.Component;
import com.lx862.jcm.mod.render.TextCase;

/**
 * Text Field Widget that is specifically designed for entering number only
 */
public class DoubleTextField extends AlwaysRenderedTextField implements GuiHelper, RenderHelper {
    private final double min;
    private final double max;
    private final String prefix;
    private final double defaultValue;

    public DoubleTextField(int x, int y, int width, int height, double min, double max, double defaultValue, @Nonnull String prefix) {
        super(x, y, width, height, 16, TextCase.LOWER, null, String.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.prefix = prefix;
        this.defaultValue = defaultValue;
    }

    public DoubleTextField(int x, int y, int width, int height, double min, double max, double defaultValue, MutableComponent prefix) {
        this(x, y, width, height, min, max, defaultValue, prefix.getString());
    }

    public DoubleTextField(int x, int y, int width, int height, double min, double max, double defaultValue) {
        this(x, y, width, height, min, max, defaultValue, (String)null);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        String prevValue = getValue();
        boolean bl = super.charTyped(chr, modifiers);

        String newString = getValue();
        if(newString.equals("-")) return bl;

        try {
            double val = Double.parseDouble(newString);
            if(val < min || val > max) {
                JCMLogger.debug("DoubleTextField: Value too large or small");
                setValue(prevValue);
                return false;
            }
        } catch (Exception e) {
            setValue(prevValue);
            return false;
        }

        return bl;
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
    	final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
    
        super.renderWidget(guiGraphics, mouseX, mouseY, tickDelta);

        if(prefix != null) {
            drawPrefix(graphicsHolder);
        }

        drawUpDownButton(graphicsHolder);
    }

    protected void drawPrefix(GraphicsHolder graphicsHolder) {
        int prefixWidth = GraphicsHolder.getTextWidth(prefix);
        int prefixX = getX() - prefixWidth;
        int prefixY = getY() + (getHeight() / 2) - (9 / 2);

        graphicsHolder.drawText(prefix, prefixX, prefixY, 0xFFFFFFFF, true, MAX_RENDER_LIGHT);
    }

    protected void drawUpDownButton(GraphicsHolder graphicsHolder) {
//        MutableText upArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.increment");
//        MutableText dnArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.decrement");
//        int fontHeight = 9;
//        int startY = (height - (fontHeight * 2));
//        int upWidth = GraphicsHolder.getTextWidth(upArrow);
//        int dnWidth = GraphicsHolder.getTextWidth(dnArrow);
//        graphicsHolder.drawText(upArrow, getX2() + width - upWidth - 2, getY2() + startY, 0xFFFFFFFF, false, MAX_RENDER_LIGHT);
//        graphicsHolder.drawText(dnArrow, getX2() + width - dnWidth - 2, getY2() + startY + fontHeight, 0xFFFFFFFF, false, MAX_RENDER_LIGHT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if(visible && active && isFocused()) {
            if(scrollY > 0) {
                increment();
            } else {
                decrement();
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//        MutableText upArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.increment");
//        MutableText dnArrow = TextUtil.translatable(TextCategory.GUI, "widget.numeric_text_field.decrement");
//        int fontHeight = 9;
//        int startY = getY2() + (height - (fontHeight * 2)) / 2;
//        int upWidth = GraphicsHolder.getTextWidth(upArrow.getString());
//        int dnWidth = GraphicsHolder.getTextWidth(dnArrow.getString());
//
//        if(inRectangle(mouseX, mouseY, getX2() + width - upWidth - 2, startY, upWidth, fontHeight)) {
//            increment();
//        }
//
//        if(inRectangle(mouseX, mouseY, getX2() + width - dnWidth - 2, startY + fontHeight, dnWidth, fontHeight)) {
//            decrement();
//        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public double getNumber() {
        try {
            return Double.parseDouble(getValue());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void setValue(double value) {
        if(value < min || value > max) return;
        setValue(String.valueOf(value));
    }

    private void increment() {
        try {
            BigDecimal result = new BigDecimal(getValue()).add(new BigDecimal(getStep()));
            setValue(result.doubleValue());
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }

    private void decrement() {
        try {
            BigDecimal result = new BigDecimal(getValue()).subtract(new BigDecimal(getStep()));
            setValue(result.doubleValue());
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }

    private static String getStep() {
        if(Screen.hasShiftDown()) {
            if(Screen.hasControlDown()) {
                return "0.025";
            } else {
                return "0.05";
            }
        }
        return "0.1";
    }
}
