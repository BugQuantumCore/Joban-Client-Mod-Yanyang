package com.lx862.jcm.mod.gui.screen;

import com.lx862.jcm.mod.block.entity.PIDSBlockEntity;
import com.lx862.jcm.mod.data.pids.PIDSManager;
import com.lx862.jcm.mod.network.block.PIDSUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.amberui.gui.screen.BlockConfigListScreen;
import com.lx862.amberui.gui.widget.ContentItem;
import com.lx862.amberui.gui.widget.HorizontalWidgetSet;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Platform;
import org.mtr.core.data.Station;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectList;
import com.lx862.amberui.gui.widget.Button;
import com.lx862.amberui.gui.widget.AlwaysRenderedTextField;
import com.lx862.amberui.gui.widget.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import com.lx862.jcm.mod.render.TextCase;
import org.mtr.MTRClient;
import org.mtr.client.MinecraftClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.lx862.jcm.mod.render.TextCase;
import org.mtr.screen.PlatformListSelectorScreen;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArraySet;

public class PIDSScreen extends BlockConfigListScreen {
    protected final EditBox[] customMessagesWidgets;
    protected final Checkbox[] rowHiddenWidgets;
    protected final Checkbox hidePlatformNumber;
    protected final Button choosePresetButton;
    protected final Button choosePlatformButton;
    protected final LongAVLTreeSet filteredPlatforms;

    protected String presetId;
    public PIDSScreen(BlockPos blockPos, String[] customMessages, boolean[] rowHidden, boolean hidePlatformNumber, String presetId) {
        super(blockPos);
        this.filteredPlatforms = new LongAVLTreeSet();
        this.customMessagesWidgets = new EditBox[customMessages.length];
        this.rowHiddenWidgets = new Checkbox[rowHidden.length];

        for(int i = 0; i < customMessages.length; i++) {
            String customMessage = customMessages[i];
            this.customMessagesWidgets[i] = new AlwaysRenderedTextField(0, 0, 120, 20, "", 100, TextCase.DEFAULT, null, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.custom_message").getString());
            this.customMessagesWidgets[i].setValue(customMessage);
        }

        for(int i = 0; i < rowHidden.length; i++) {
            boolean rowIsHidden = rowHidden[i];
            this.rowHiddenWidgets[i] = new Checkbox(0, 0, 120, 20, true, (cb) -> {});
            this.rowHiddenWidgets[i].setChecked(rowIsHidden);
            this.rowHiddenWidgets[i].setMessage((TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.row_hidden")));
        }

        this.hidePlatformNumber = new Checkbox(0, 0, 20, 20, true, (cb) -> {});
        this.hidePlatformNumber.setChecked(hidePlatformNumber);

        this.choosePresetButton = new Button(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.choose_preset"), (btn) -> {
            Minecraft.getInstance().setScreen(
                new PIDSPresetScreen(blockPos, presetId, (str) -> {
                    this.presetId = str;
                }).withPreviousScreen(this)
            );
        });

        if(Minecraft.getInstance().level != null) {
            BlockEntity be = Minecraft.getInstance().level.getBlockEntity(blockPos);
            if(be != null && be instanceof PIDSBlockEntity) {
                filteredPlatforms.addAll(((PIDSBlockEntity)be).getPlatformIds());
            }
        }

        this.choosePlatformButton = new Button(0, 0, 60, 20, TextUtil.translatable(TextCategory.GUI, "pids.listview.widget.change_platform"), (btn) -> {
            final Station station = MTRClient.findStation(blockPos);
            final ObjectArraySet<Platform> platforms;
            if(station != null) {
                platforms = station.savedRails;
            } else {
                platforms = new ObjectArraySet<>();
                MTRClient.findClosePlatform(blockPos.below(4), 5, platforms::add);
            }

            final PlatformListSelectorScreen platformListSelectorScreen = new PlatformListSelectorScreen(selectedPlatforms -> {
                filteredPlatforms.clear();
                selectedPlatforms.forEach(platform -> filteredPlatforms.add(platform.getId()));
            }, new org.mtr.screen.WindowBase(this) {});
            platformListSelectorScreen.setAvailableList(platforms);
            platforms.forEach(platform -> {
                if(filteredPlatforms.contains(platform.getId())) {
                    platformListSelectorScreen.selectData(platform);
                }
            });
            Minecraft.getInstance().setScreen(platformListSelectorScreen);
        });

        this.presetId = presetId;
    }

    @Override
    public MutableComponent getScreenTitle() {
        return Blocks.RV_PIDS.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        // Preset button
        addRenderableWidget(choosePresetButton);
        ContentItem presetEntry = new ContentItem(TextUtil.translatable(TextCategory.GUI, "pids.listview.title.pids_preset"), new MappedWidget(choosePresetButton), 26);
        presetEntry.setIconCallback((guiDrawing, startX, startY, width, height) -> {
            PIDSPresetScreen.drawPIDSPreview(PIDSManager.getPreset(presetId), guiDrawing, startX, startY, width, height, true);
        });
        listViewWidget.add(presetEntry);

        // Filter Platform button
        ObjectList<String> platformList = new ObjectArrayList<>();
        for(long filteredPlatform : filteredPlatforms) {
            Platform platform = MinecraftClientData.getInstance().platforms.stream().filter(p -> p.getId() == filteredPlatform).findFirst().orElse(null);
            if(platform == null) {
                platformList.add("?");
            } else {
                platformList.add(platform.getName());
            }
        }
        String platforms = String.join(",", platformList);
        listViewWidget.add(filteredPlatforms.isEmpty() ? TextUtil.translatable(TextCategory.GUI, "pids.listview.title.filtered_platform.nearby") : TextUtil.translatable(TextCategory.GUI, "pids.listview.title.filtered_platform", platforms), new MappedWidget(choosePlatformButton));
        addRenderableWidget(choosePlatformButton);

        for(int i = 0; i < this.customMessagesWidgets.length; i++) {
            addRenderableWidget(this.customMessagesWidgets[i]);
            addRenderableWidget(this.rowHiddenWidgets[i]);

            int w = listViewWidget.getWidth();
            this.rowHiddenWidgets[i].setWidth(95);
            this.customMessagesWidgets[i].setWidth(w - this.rowHiddenWidgets[i].getWidth() - 18);

            HorizontalWidgetSet widgetSet = new HorizontalWidgetSet();
            widgetSet.addWidget(new MappedWidget(this.customMessagesWidgets[i]));
            widgetSet.addWidget(new MappedWidget(this.rowHiddenWidgets[i]));
            widgetSet.setXYSize(listViewWidget.getX(), 20, listViewWidget.getWidth(), 20);
            listViewWidget.add(null, new MappedWidget(widgetSet));
        }

        addRenderableWidget(hidePlatformNumber);
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "pids.listview.title.hide_platform_number"), new MappedWidget(hidePlatformNumber));
    }

    @Override
    public void onSave() {
        String[] customMessages = new String[customMessagesWidgets.length];
        boolean[] rowHidden = new boolean[rowHiddenWidgets.length];

        for(int i = 0; i < customMessagesWidgets.length; i++) {
            customMessages[i] = this.customMessagesWidgets[i].getValue();
        }

        for(int i = 0; i < rowHiddenWidgets.length; i++) {
            rowHidden[i] = this.rowHiddenWidgets[i].isChecked();
        }

        Networking.sendPacketToServer(new PIDSUpdatePacket(blockPos, filteredPlatforms, customMessages, rowHidden, hidePlatformNumber.isChecked(), presetId));
    }
}
