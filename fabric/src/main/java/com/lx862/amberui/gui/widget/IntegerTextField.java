package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.TextCase;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import com.lx862.jcm.mod.render.TextCase;

/**
 * Text Field Widget that is specifically designed for entering number only
 */
public class IntegerTextField extends AlwaysRenderedTextField implements GuiHelper, RenderHelper {
    private final long min;
    private final long max;
    private final String prefix;
    private final long defaultValue;
    private Consumer<Long> changeListener;

    public IntegerTextField(int x, int y, int width, int height, long min, long max, long defaultValue, @Nonnull String prefix) {
        super(x, y, width, height, 16, TextCase.LOWER, null, String.valueOf(defaultValue));
        this.min = min;
        this.max = max;
        this.prefix = prefix;
        this.defaultValue = defaultValue;
    }

    public IntegerTextField(int x, int y, int width, int height, int min, int max, int defaultValue, MutableComponent prefix) {
        this(x, y, width, height, min, max, defaultValue, prefix.getString());
    }

    public IntegerTextField(int x, int y, int width, int height, int min, int max, int defaultValue) {
        this(x, y, width, height, min, max, defaultValue, (String)null);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        String prevValue = getValue();
        boolean bl = super.charTyped(chr, modifiers);

        String newString = getValue();
        if(newString.equals("-")) return bl;
        try {
            int val = Integer.parseInt(newString);
            if(val < min || val > max) {
                JCMLogger.debug("NumericTextField: Value too large or small");
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
            drawPrefix(new com.lx862.jcm.mod.render.GraphicsHolder(guiGraphics));
        }

        drawUpDownButton(new com.lx862.jcm.mod.render.GraphicsHolder(guiGraphics));
    }

    public void onChange(Consumer<Long> callback) {
        this.changeListener = callback;
    }

    @Override
    public void setValue(String s) {
        super.setValue(s);
        if(this.changeListener != null) this.changeListener.accept(Long.parseLong(s));
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

    public long getNumber() {
        try {
            return Integer.parseInt(getValue());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public void setValue(long value) {
        if(value < min || value > max) return;
        setValue(String.valueOf(value));
    }

    private void increment() {
        try {
            setValue(Integer.parseInt(getValue())+1);
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }

    private void decrement() {
        try {
            setValue(Integer.parseInt(getValue())-1);
        } catch (Exception e) {
            setValue(defaultValue);
        }
    }
}
