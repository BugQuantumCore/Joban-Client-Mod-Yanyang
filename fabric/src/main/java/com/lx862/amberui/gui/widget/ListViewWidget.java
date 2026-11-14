package com.lx862.amberui.gui.widget;

import com.lx862.jcm.mod.Constants;
import com.lx862.amberui.gui.ClipStack;
import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;

import java.util.ArrayList;
import java.util.List;

public class ListViewWidget extends AbstractScrollViewWidget implements RenderHelper, GuiHelper {
    public static final int ENTRY_PADDING = 5;
    private final List<AbstractListItem> displayedEntryList = new ArrayList<>();
    private final List<AbstractListItem> entryList = new ArrayList<>();
    private float elapsed;
    private String searchTerm = "";
    public ListViewWidget() {
        super(0, 0, 0, 0);
    }

    public void setXYSize(int x, int y, int width, int height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        setScroll(currentScroll);
        positionWidgets(currentScroll);
        refreshDisplayedList();
    }

    public void add(MutableComponent text, MappedWidget widget) {
        add(new ContentItem(text, widget));
    }

    public void add(AbstractListItem listItem) {
        entryList.add(listItem);
        refreshDisplayedList();
    }

    public void addCategory(MutableComponent text) {
        entryList.add(new CategoryItem(text));
        refreshDisplayedList();
    }

    public void reset() {
        this.entryList.clear();
        refreshDisplayedList();
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        refreshDisplayedList();
    }

    private void refreshDisplayedList() {
        displayedEntryList.clear();
        entryList.forEach(e -> {
            if(e.matchQuery(searchTerm)) {
                e.shown();
                displayedEntryList.add(e);
            } else {
                e.hidden();
            }
        });
        positionWidgets();

        // Update scrolling so that it will rubber-band back if scrolled area are beyond viewport
        setScroll(currentScroll);
    }

    @Override
    public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float tickDelta) {
    	final GraphicsHolder graphicsHolder = new GraphicsHolder(guiGraphics);
    
        GuiHelper.drawRectangle(new GuiDrawing(graphicsHolder), getX(), getY(), getWidth(), getHeight(), 0x4F4C4C4C);
        super.renderWidget(guiGraphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void renderContent(GraphicsHolder graphicsHolder, int mouseX, int mouseY, float tickDelta) {
        GuiDrawing guiDrawing = new GuiDrawing(graphicsHolder);
        elapsed += (tickDelta / Constants.MC_TICK_PER_SECOND);

        int incY = 0;
        for (AbstractListItem abstractListItem : displayedEntryList) {
            ClipStack.ensureStateCorrect();
            int entryX = getX();
            int entryY = getY() + incY - (int) currentScroll;
            boolean widgetVisible = false;
            boolean rowInSight = inRectangle(entryX, entryY, getX(), getY(), getWidth(), getHeight()) || inRectangle(entryX, entryY+abstractListItem.getHeight(), getX(), getY(), getWidth(), getHeight());

            if (abstractListItem instanceof ContentItem) {
                ContentItem contentItem = (ContentItem) abstractListItem;
                boolean topLeftVisible = inRectangle(contentItem.widget.getX(), contentItem.widget.getY(), getX(), getY(), getWidth(), getHeight());
                boolean bottomRightVisible = inRectangle(contentItem.widget.getX() + contentItem.widget.getWidth(), contentItem.widget.getY() + contentItem.widget.getHeight(), getX(), getY(), getWidth(), getHeight());
                widgetVisible = topLeftVisible && bottomRightVisible;
            }

            abstractListItem.draw(graphicsHolder, guiDrawing, entryX, entryY, getWidth() - getScrollbarOffset(), getHeight(), mouseX, mouseY, widgetVisible, rowInSight, elapsed, tickDelta);

            incY += abstractListItem.getHeight();
        }
    }

    @Override
    public void setScroll(double newScroll) {
        super.setScroll(newScroll);
        positionWidgets();
    }

    @Override
    protected int getContentHeight() {
        int entryHeight = 0;
        for(AbstractListItem entry : displayedEntryList) {
            entryHeight += entry.getHeight();
        }
        return entryHeight;
    }

    public void positionWidgets() {
        positionWidgets(currentScroll);
    }

    private void positionWidgets(double scroll) {
        int startX = getX();
        int startY = getY();

        int incY = 0;
        for (AbstractListItem listItem : displayedEntryList) {
            int entryY = startY + incY;
            int x = (startX + getWidth() - getScrollbarOffset()) - ENTRY_PADDING;
            listItem.positionChanged(x, (int) -scroll + entryY);
            incY += listItem.getHeight();
        }
    }

	@Override
	public void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
	}
}
