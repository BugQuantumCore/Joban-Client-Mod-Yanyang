package com.lx862.amberui.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.amberui.gui.widget.WidgetSet;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import com.lx862.amberui.gui.widget.Button;

public abstract class SavableScreen extends TitledScreen implements GuiHelper {
    protected Button saveButton;
    protected Button discardButton;
    protected boolean discardConfig = false;

    public SavableScreen(boolean animatable) {
        super(animatable);
    }

    protected void addBottomRowEntry(int x, int y, int width, int height) {
        WidgetSet bottomEntryWidget = new WidgetSet(20);

        this.saveButton = new Button(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.save"), (btn) -> {
            onClose();
        });

        this.discardButton = new Button(0, 0, 0, 20, TextUtil.translatable(TextCategory.GUI, "block_config.discard"), (btn) -> {
            discardConfig = true;
            onClose();
        });

        addRenderableWidget(saveButton);
        addRenderableWidget(discardButton);

        bottomEntryWidget.addWidget(saveButton);
        bottomEntryWidget.addWidget(discardButton);
        bottomEntryWidget.setXYSize(x, y, width, height);
        addRenderableWidget(bottomEntryWidget);
    }

    @Override
    public void onClose() {
        // Save config by default, unless explicitly requested not to
        if(!discardConfig) {
            onSave();
        } else {
            onDiscard();
        }
        super.onClose();
    }

    protected abstract void onSave();

    protected void onDiscard() {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
