package com.lx862.jcm.mod.gui.screen;

import com.lx862.amberui.gui.widget.*;
import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.data.pids.preset.PIDSPresetBase;
import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.amberui.gui.screen.TitledScreen;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.amberui.gui.widget.Button;
import com.lx862.amberui.gui.widget.AlwaysRenderedTextField;
import com.lx862.jcm.mod.render.GuiDrawing;
import com.lx862.jcm.mod.render.TextCase;

import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.lx862.jcm.mod.render.TextCase;

public class PIDSPresetScreen extends TitledScreen implements RenderHelper, GuiHelper {
    private static final ResourceLocation PIDS_PREVIEW_BASE = ResourceLocation.parse("jsblock:textures/gui/pids_preview.png");
    private final Consumer<String> callback;
    private final String selectedPreset;
    private final String pidsType;

    public PIDSPresetScreen(BlockPos pos, String selectedPreset, Consumer<String> callback) {
        super(false);
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(pos);
        if(be != null && be instanceof PIDSBlockEntity) {
            this.pidsType = ((PIDSBlockEntity)be).getPIDSType();
        } else {
            this.pidsType = "";
        }
        this.selectedPreset = selectedPreset;
        this.callback = callback;
    }

    @Override
    protected void init() {
        super.init();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 60) * 0.76);
        int startX = (width - contentWidth) / 2;
        int searchStartY = TEXT_PADDING * 5;
        int startY = searchStartY + (TEXT_PADDING * 3);

        ListViewWidget listViewWidget = new ListViewWidget();
        addConfigEntries(listViewWidget);

        AlwaysRenderedTextField searchBox = new AlwaysRenderedTextField(startX, searchStartY, contentWidth, 22, 60, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "widget.search").getString());
        searchBox.setResponder(listViewWidget::setSearchTerm);
        searchBox.setValue(searchBox.getValue());

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addRenderableWidget(listViewWidget);
        addRenderableWidget(searchBox);
    }

    @Override
    public MutableComponent getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.title");
    }

    @Override
    public MutableComponent getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "pids_preset.subtitle", selectedPreset);
    }

    public void addConfigEntries(ListViewWidget listViewWidget) {
        listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.builtin"));
        for(PIDSPresetBase preset : PIDSManager.getBuiltInPresets()) {
            addPreset(listViewWidget, preset);
        }

        if(!PIDSManager.getCustomPresets().isEmpty()) {
            listViewWidget.addCategory(TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.category.custom"));
            for(PIDSPresetBase preset : PIDSManager.getCustomPresets()) {
                addPreset(listViewWidget, preset);
            }
        }
    }

    private void addPreset(ListViewWidget listViewWidget, PIDSPresetBase preset) {
        if(!preset.typeAllowed(pidsType)) return;

        Button selectBtn = new Button(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose"), (btn) -> {
            choose(preset.getId());
        });

        if(preset.getId().equals(selectedPreset)) {
            selectBtn.setMessage((TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.selected")));
            selectBtn.active = false;
        }

        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
        widgetSet.addWidget(new MappedWidget(selectBtn));
        widgetSet.setXYSize(0, 0, 100, 20);

        addRenderableWidget(selectBtn);
        addRenderableWidget(widgetSet);
        ContentItem contentItem = new ContentItem(TextUtil.literal(preset.getName()), new MappedWidget(widgetSet), 26);

        contentItem.setIconCallback((guiDrawing, startX, startY, width, height) -> {
            drawPIDSPreview(preset, guiDrawing, startX, startY, width, height, false);
        });
        listViewWidget.add(contentItem);
    }

    public static void drawPIDSPreview(PIDSPresetBase preset, GuiDrawing guiDrawing, int startX, int startY, int width, int height, boolean backgroundOnly) {
        final int offset = 6;

        // Background
        GuiHelper.drawTexture(guiDrawing, PIDS_PREVIEW_BASE, startX, startY, width, height);
        if(preset == null) return;

        GuiHelper.drawTexture(guiDrawing, preset.getThumbnail(), startX+0.5, startY+offset+0.5, width-1, height-offset-4);

        if(!backgroundOnly) {
            double perRow = height / 8.5;
            double rowHeight = Math.max(0.5, height / 24.0);
            for(int i = 0; i < 4; i++) {
                if(preset.isRowHidden(i)) continue;
                double curY = startY + offset + ((i+1) * perRow);
                GuiHelper.drawRectangle(guiDrawing, startX+1.5, curY, width * 0.55, rowHeight, preset.getTextColor());
                GuiHelper.drawRectangle(guiDrawing, startX + (width * 0.65), curY, rowHeight, rowHeight, preset.getTextColor());
                GuiHelper.drawRectangle(guiDrawing, startX + (width * 0.75), curY, (width * 0.2)-0.5, rowHeight, preset.getTextColor());
            }
        }
    }

    private void choose(String id) {
        callback.accept(id);
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
