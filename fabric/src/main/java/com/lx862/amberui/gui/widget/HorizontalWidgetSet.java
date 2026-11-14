package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import com.lx862.jcm.mod.render.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a set of widget within a defined area.
 * Able to tile the widgets horizontally.<br>
 * Unlike {@link WidgetSet}, this widget does not automatically distribute the widget evenly and you can control the width of each element.
 */
public class HorizontalWidgetSet extends AbstractWidget implements WidgetsWrapper, RenderHelper {
    public final int widgetXMargin;
    private final List<MappedWidget> widgets = new ArrayList<>();

    public HorizontalWidgetSet(int widgetXMargin) {
        super(0, 0, 0, 20, Component.empty());
        this.widgetXMargin = widgetXMargin;
    }
    public HorizontalWidgetSet() {
        this(5);
    }

    /**
     * Set the X, Y, Width and Height of the widget. This is required to be called as the default width is 0
     */
    public void setXYSize(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        positionWidgets();
    }

    public void addWidget(MappedWidget widget) {
        widgets.add(widget);
    }

    @Override
    public int getWidth() {
        int total = 0;
        for(MappedWidget mappedWidget : widgets) {
            total += mappedWidget.getWidth();
            total += widgetXMargin;
        }
        if(total > 1) {
            total -= widgetXMargin;
        }
        return total;
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    	final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
    
        for(MappedWidget mappedWidget : widgets) {
            mappedWidget.renderWidget(guiGraphics, mouseX, mouseY, delta);
        }
    }

    public void setVisibleMapped(boolean visible) {
        for(MappedWidget widget : widgets) {
            widget.setVisible(visible);
        }
        super.visible = visible;
    }

    public void setAllX(int newX) {
        super.setX(newX);
        positionWidgets();
    }

    public void setAllY(int newY) {
        super.setY(newY);
        positionWidgets();
    }

    private void positionWidgets() {
        int accX = 0;

        for (MappedWidget widget : widgets) {
            widget.setX(getX() + accX);
            widget.setY(getY());
            int clampedWidth = Math.min(getWidth() - accX, widget.getWidth());
            widget.setWidth(clampedWidth);
            accX += clampedWidth + widgetXMargin;
        }
    }

	@Override
	public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
	}
}
