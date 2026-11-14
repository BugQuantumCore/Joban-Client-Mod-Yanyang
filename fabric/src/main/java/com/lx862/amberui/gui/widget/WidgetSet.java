package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import com.lx862.jcm.mod.render.GraphicsHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds a set of widget within a defined area.
 * Able to tile the widgets horizontally and add new rows like a grid/table.
 */
public class WidgetSet extends AbstractWidget implements WidgetsWrapper, RenderHelper {
    private final List<List<MappedWidget>> widgetRows = new ArrayList<>();
    private final int maxWidgetHeight;
    public final int widgetXMargin;

    public WidgetSet(int maxWidgetHeight, int widgetXMargin) {
        super(0, 0, 0, 0, net.minecraft.network.chat.Component.empty());
        this.maxWidgetHeight = maxWidgetHeight;
        this.widgetXMargin = widgetXMargin;
    }
    public WidgetSet(int maxWidgetHeight) {
        this(maxWidgetHeight, 10);
    }

    /**
     * Set the position and size of the widget and reposition all added widget.
     * Make sure to call this after all widget has been added.
     */
    public void setXYSize(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        positionWidgets();
    }

    public void addWidget(Button widget) {
        addWidget(new MappedWidget(widget));
    }

    public void addWidget(MappedWidget widget) {
        if(widgetRows.isEmpty()) {
            insertRow();
        }

        int lastRowIndex = widgetRows.size() - 1;
        List<MappedWidget> lastRow = widgetRows.get(lastRowIndex);
        lastRow.add(widget);
        widgetRows.set(lastRowIndex, lastRow);
    }

    public void insertRow() {
        widgetRows.add(new ArrayList<>());
    }

    public void reset() {
        this.widgetRows.clear();
    }

    private void positionWidgets() {
        int x = getX();
        int y = getY();
        int width = getWidth() + widgetXMargin;
        int widgetHeight = Math.min(maxWidgetHeight, (int)(maxWidgetHeight * ((double)getHeight() / (widgetRows.size() * maxWidgetHeight))));

        for(int i = 0; i < widgetRows.size(); i++) {
            List<MappedWidget> rowWidgets = widgetRows.get(i);

            double perWidgetWidth = ((double)width / rowWidgets.size()) - widgetXMargin;
            int rowY = y + (i * widgetHeight);

            for (int j = 0; j < rowWidgets.size(); j++) {
                MappedWidget widget = new MappedWidget(rowWidgets.get(j));
                double widgetStartX = x + (j * perWidgetWidth) + (j * widgetXMargin);

                widget.setX((int)Math.round(widgetStartX));
                widget.setY(rowY);
                widget.setWidth((int)Math.round(perWidgetWidth));
            }
        }
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    	final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
    
        for(List<MappedWidget> row : widgetRows) {
            for(MappedWidget widget : row) {
                widget.renderWidget(guiGraphics, mouseX, mouseY, delta);
            }
        }
    }

    @Override
    public void setAllX(int newX) {
        super.setX(newX);
        positionWidgets();
    }

    @Override
    public void setAllY(int newY) {
        super.setY(newY);
        positionWidgets();
    }

    public void setVisibleMapped(boolean visible) {
        for(List<MappedWidget> row : widgetRows) {
            for(MappedWidget widget : row) {
                widget.setVisible(visible);
            }
        }
        super.visible = visible;
    }

	@Override
	public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
	}
}
