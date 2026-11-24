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
        /* Floor the bottom row and clamp the list height: below ~222 GUI
           height pixels (small windows at high GUI scale) the old formula
           went NEGATIVE — the widget rows collapsed onto each other and the
           Save/Discard buttons were pushed off-screen, making the config
           unsaveable. */
        int bottomEntryHeight = Math.max(20, (height - startY - listViewHeight - (BOTTOM_ROW_MARGIN * 2)));
        int effectiveListViewHeight = Math.min(listViewHeight, Math.max(160, height - startY - bottomEntryHeight - (BOTTOM_ROW_MARGIN * 2)));

        ListViewWidget listViewWidget = new ListViewWidget();
        listViewWidget.setXYSize(startX, startY, contentWidth, effectiveListViewHeight);
        addConfigEntries(listViewWidget);
        addBottomRowEntry(startX, startY + effectiveListViewHeight + BOTTOM_ROW_MARGIN, contentWidth, bottomEntryHeight);
        listViewWidget.positionWidgets();
        addRenderableWidget(listViewWidget);
    }

    public abstract void addConfigEntries(ListViewWidget listViewWidget);
}
