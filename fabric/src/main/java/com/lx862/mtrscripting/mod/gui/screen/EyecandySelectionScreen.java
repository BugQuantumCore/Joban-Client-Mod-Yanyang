package com.lx862.mtrscripting.mod.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.render.RenderHelper;
import com.lx862.amberui.gui.screen.SavableScreen;
import com.lx862.amberui.gui.widget.ContentItem;
import com.lx862.amberui.gui.widget.HorizontalWidgetSet;
import com.lx862.amberui.gui.widget.AlwaysRenderedTextField;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.EyecandyCustomConfig;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.config.JCMBlockEyecandyExtra;
import com.lx862.mtrscripting.mod.resource.EyecandyResourceProvider;
import com.lx862.mtrscripting.mod.resource.MtrScriptingResourceManager;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import com.lx862.amberui.gui.widget.Button;
import com.lx862.jcm.mod.render.GuiDrawing;
import net.minecraft.client.gui.components.EditBox;
import com.lx862.jcm.mod.render.TextCase;
import org.mtr.block.BlockEyeCandy;
import org.mtr.client.CustomResourceLoader;
import org.mtr.resource.ObjectResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.lx862.jcm.mod.render.TextCase;

public class EyecandySelectionScreen extends SavableScreen implements RenderHelper, GuiHelper {
    private final EditBox searchBox;
    private final ListViewWidget listViewWidget;
    private final Runnable callback;
    private final BlockPos blockPos;
    private String currentObject;

    private final LongArrayList selectedIndices;
    private final Map<String, String> storedConfigEntries = new HashMap<>();

    private final ObjectImmutableList<ObjectResource> loadedObjects = CustomResourceLoader.getObjects();

    public EyecandySelectionScreen(BlockPos blockPos, LongArrayList selectedIndices, Runnable callback) {
        super(false);
        this.blockPos = blockPos;
        this.selectedIndices = selectedIndices;
        BlockEntity be = Minecraft.getInstance().level.getBlockEntity(blockPos);
        if(be != null && be instanceof BlockEyeCandy.EyeCandyBlockEntity) {
            this.currentObject = ((BlockEyeCandy.EyeCandyBlockEntity)be).getModelId();
            this.storedConfigEntries.putAll(new HashMap<>(((JCMBlockEyecandyExtra)be).jsblock$getCustomConfig()));
        } else {
            this.currentObject = "";
        }
        this.callback = callback;
        this.listViewWidget = new ListViewWidget();
        this.searchBox = new AlwaysRenderedTextField(0, 0, 0, 22, 60, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "widget.search").getString());
    }

    @Override
    protected void init() {
        super.init();
        int contentWidth = (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
        int listViewHeight = (int)((height - 90) * 0.76);
        int startX = (width - contentWidth) / 2;
        int searchStartY = TEXT_PADDING * 5;
        int startY = searchStartY + (TEXT_PADDING * 3);

        selectListener.clear();
        listViewWidget.reset();
        addConfigEntries(this.listViewWidget);
        searchBox.setX(startX);
        searchBox.setY(searchStartY);
        searchBox.setWidth(contentWidth);
        searchBox.setResponder(listViewWidget::setSearchTerm);
        searchBox.setValue(searchBox.getValue());

        listViewWidget.setXYSize(startX, startY, contentWidth, listViewHeight);
        addRenderableWidget(listViewWidget);
        addRenderableWidget(searchBox);

        addBottomRowEntry(startX, startY + listViewHeight + 6, contentWidth, height - (startY + listViewHeight));
    }

    @Override
    public MutableComponent getScreenTitle() {
        return TextUtil.translatable(TextCategory.GUI, "eyecandy.title");
    }

    @Override
    public MutableComponent getScreenSubtitle() {
        return TextUtil.translatable(TextCategory.GUI, "eyecandy.subtitle", currentObject);
    }

    public void addConfigEntries(ListViewWidget listViewWidget) {
        for(ObjectResource objectResource : loadedObjects) {
            addObjectEntry(listViewWidget, objectResource);
        }
        updateSelectedEntry();
    }

    private final List<Runnable> selectListener = new ArrayList<>();

    private void updateSelectedEntry() {
        selectListener.forEach(Runnable::run);
    }

    private void addObjectEntry(ListViewWidget listViewWidget, ObjectResource objectResource) {
        Button selectBtn = new Button(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose"), (btn) -> {
            choose(objectResource);
            updateSelectedEntry();
        });

        selectListener.add(() -> {
            if(objectResource.getId().equals(currentObject)) {
                selectBtn.setMessage((TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.selected")));
                selectBtn.active = false;
            } else {
                selectBtn.setMessage((TextUtil.translatable(TextCategory.GUI, "pids_preset.listview.widget.choose")));
                selectBtn.active = true;
            }
        });

        EyecandyResourceProvider.EyecandyScriptConfiguration scriptConfig = MtrScriptingResourceManager.eyecandy.getScriptEntry(objectResource.getId());
        boolean hasCustomConfig = scriptConfig != null && scriptConfig.eyecandyCustomConfig() != null;

        Button editBtn = new Button(0, 0, 20, 20, TextUtil.translatable(TextCategory.GUI, "eyecandy.listview.widget.edit"), (btn) -> {
            Minecraft.getInstance().setScreen(
                    new EyecandyCustomConfigScreen(
                        objectResource.getName(),
                        objectResource.getId(),
                        blockPos,
                        this.storedConfigEntries,
                        scriptConfig.eyecandyCustomConfig()
                    ).withPreviousScreen(this)
            );
        });

        HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
        if(hasCustomConfig) widgetSet.addWidget(new MappedWidget(editBtn));
        widgetSet.addWidget(new MappedWidget(selectBtn));
        widgetSet.setXYSize(0, 0, 110, 20);

        if(hasCustomConfig) addRenderableWidget(editBtn);
        addRenderableWidget(selectBtn);
        addRenderableWidget(widgetSet);
        ContentItem contentItem = new ContentItem(TextUtil.literal(objectResource.getName()), new MappedWidget(widgetSet), 26);

        contentItem.setIconCallback((guiDrawing, startX, startY, width, height) -> {
            drawIcon(objectResource.getColor(), guiDrawing, startX, startY, width, height);
        });
        listViewWidget.add(contentItem);
    }

    public static void drawIcon(int color, GuiDrawing guiDrawing, int startX, int startY, int width, int height) {
        // Background
        GuiHelper.drawRectangle(guiDrawing, startX+4, startY+4, width-8, height-8, ARGB_BLACK | color);
    }

    private void choose(ObjectResource objectResource) {
        selectedIndices.clear();
        selectedIndices.add(loadedObjects.indexOf(objectResource));
        currentObject = objectResource.getId();
        sendUpdate();
    }

    @Override
    protected void onSave() {
        sendUpdate();
    }

    private void sendUpdate() {
        this.callback.run();

        EyecandyResourceProvider.EyecandyScriptConfiguration scriptConfig = MtrScriptingResourceManager.eyecandy.getScriptEntry(currentObject);
        boolean hasCustomConfig = scriptConfig != null && scriptConfig.eyecandyCustomConfig() != null;

        if(hasCustomConfig) {
            Map<String, String> entries = new HashMap<>();
            for(EyecandyCustomConfig.Entry key : scriptConfig.eyecandyCustomConfig().entries()) {
                String value = storedConfigEntries.getOrDefault(key.id(), key.defaultValue());
                if(value != null) entries.put(key.id(), value);
            }

            EyecandyCustomConfigScreen.sendCustomConfigUpdate(currentObject, blockPos, entries);
        }
    }
}
