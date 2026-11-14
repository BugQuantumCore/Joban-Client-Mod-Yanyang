package com.lx862.amberui.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.widget.ListViewWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.Block;

/**
 * Block Config Screen with a GUI list, override {@link BlockConfigListScreen#addConfigEntries(ListViewWidget)} to add items to the list.
 */
public abstract class BlockConfigListScreen extends BlockConfigScreen implements GuiHelper {

    public BlockConfigListScreen(BlockPos blockPos) {
        super(blockPos);
    }

    @Override
    protected void init() {
        super.init();
        int contentWidth = getContentWidth();
        int listViewHeight = Math.max(160, (int)((height - 60) * 0.75));
        int startX = (width - contentWidth) / 2;
        int startY = TEXT_PADDING * 5;
        int bottomEntryHeight = (height - startY - listViewHeight - (BOTTOM_ROW_MARGIN * 2));

        ListViewWidget listViewWidget = new ListViewWidget();
        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addConfigEntries(listViewWidget);
        addBottomRowEntry(startX, startY + listViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
        listViewWidget.positionWidgets();
        addRenderableWidget(listViewWidget);
    }

    public abstract void addConfigEntries(ListViewWidget listViewWidget);
}
