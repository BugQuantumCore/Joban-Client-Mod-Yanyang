package com.lx862.amberui.gui.widget;

import com.lx862.amberui.gui.GuiHelper;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.GraphicsHolder;
import com.lx862.jcm.mod.render.GuiDrawing;

import java.util.Objects;

/**
 * Represent a row in {@link ListViewWidget}
 */
public class CategoryItem extends AbstractListItem {
    public final MutableComponent title;
    private final int height;

    public CategoryItem(MutableComponent title, int height) {
        this.height = height;
        this.title = title;
    }

    public CategoryItem(MutableComponent title) {
        this.height = 22;
        this.title = title;
    }

    /* */
    @Override
    public void draw(GraphicsHolder graphicsHolder, GuiDrawing guiDrawing, int entryX, int entryY, int width, int height, int mouseX, int mouseY, boolean widgetVisible, boolean rowInSight, double elapsed, float tickDelta) {
        if(rowInSight) {
            GuiHelper.drawRectangle(guiDrawing, entryX, entryY, width, this.height, 0x99999999);
            graphicsHolder.drawCenteredText(title, (entryX + width / 2), entryY - (8/2) + (this.height / 2), ARGB_WHITE);
        }
    }

    @Override
    public boolean matchQuery(String searchTerm) {
        return Objects.equals(searchTerm, "") || title.getString().contains(searchTerm);
    }

    @Override
    public void positionChanged(int entryX, int entryY) {
    }

    @Override
    public void hidden() {
    }

    @Override
    public void shown() {
    }

    @Override
    public int getHeight() {
        return 22;
    }
}
