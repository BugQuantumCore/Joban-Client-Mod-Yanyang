package com.lx862.jcm.mod.gui.screen;

import com.lx862.jcm.mod.block.entity.SoundLooperBlockEntity;
import com.lx862.jcm.mod.network.block.SoundLooperUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.amberui.gui.screen.BlockConfigListScreen;
import com.lx862.amberui.gui.widget.BlockPosWidget;
import com.lx862.amberui.gui.widget.IntegerTextField;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import com.lx862.amberui.gui.widget.Button;
import com.lx862.amberui.gui.widget.AlwaysRenderedTextField;
import com.lx862.amberui.gui.widget.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import com.lx862.jcm.mod.render.TextCase;
import com.lx862.jcm.mod.render.TextCase;

public class SoundLooperScreen extends BlockConfigListScreen {
    private final Button soundCategoryButton;
    private final EditBox soundIdTextField;
    private final IntegerTextField repeatTickTextField;
    private final IntegerTextField soundVolumeTextField;
    private final Checkbox needRedstonePowerCheckbox;
    private final Checkbox limitSoundRangeCheckbox;
    private final BlockPosWidget corner1Widget;
    private final BlockPosWidget corner2Widget;
    private int soundCategory;

    public SoundLooperScreen(BlockPos blockPos, BlockPos corner1, BlockPos corner2, String soundId, int soundCategory, float volume, int repeatTick, boolean needRedstonePower, boolean limitRange) {
        super(blockPos);
        this.soundCategoryButton = new Button(0, 0, 60, 20, net.minecraft.network.chat.Component.empty(), (btn) -> {
            setSoundCategory((this.soundCategory + 1) % SoundLooperBlockEntity.SOURCE_LIST.length);
        });
        setSoundCategory(soundCategory);
        this.corner1Widget = new BlockPosWidget(0, 0, 120, 20);
        this.corner1Widget.setBlockPos(corner1);
        this.corner2Widget = new BlockPosWidget(0, 0, 120, 20);
        this.corner2Widget.setBlockPos(corner2);

        this.soundIdTextField = new AlwaysRenderedTextField(0, 0, 100, 20, 100, TextCase.LOWER, null, "mtr:ticket_barrier");
        this.soundIdTextField.setValue(soundId);
        this.soundVolumeTextField = new IntegerTextField(0, 0, 60, 20, 1, 1000, 100);
        this.soundVolumeTextField.setValue((int)(volume * 100));
        this.repeatTickTextField = new IntegerTextField(0, 0, 60, 20, 1, 99999, 20);
        this.repeatTickTextField.setValue(repeatTick);
        this.needRedstonePowerCheckbox = new Checkbox(0, 0, 20, 20, false, (checkbox) -> {});
        this.needRedstonePowerCheckbox.setChecked(needRedstonePower);
        this.limitSoundRangeCheckbox = new Checkbox(0, 0, 20, 20, false, (checkbox) -> {
            if(checkbox.isChecked()) {
                this.corner1Widget.active = true;
                this.corner2Widget.active = true;
            } else {
                this.corner1Widget.active = false;
                this.corner2Widget.active = false;
            }
        });
        this.limitSoundRangeCheckbox.setChecked(limitRange);
        this.corner1Widget.active = limitRange;
        this.corner2Widget.active = limitRange;
    }

    @Override
    public MutableComponent getScreenTitle() {
        return com.lx862.jcm.mod.registry.Blocks.SOUND_LOOPER.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        corner1Widget.addWidget(this::addRenderableWidget);
        corner2Widget.addWidget(this::addRenderableWidget);
        addRenderableWidget(soundCategoryButton);
        addRenderableWidget(soundIdTextField);
        addRenderableWidget(soundVolumeTextField);
        addRenderableWidget(repeatTickTextField);
        addRenderableWidget(needRedstonePowerCheckbox);
        addRenderableWidget(limitSoundRangeCheckbox);
        addRenderableWidget(corner1Widget);
        addRenderableWidget(corner2Widget);
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.sound_category"), new MappedWidget(soundCategoryButton));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.sound_id"), new MappedWidget(soundIdTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.sound_volume"), new MappedWidget(soundVolumeTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.repeat_tick"), new MappedWidget(repeatTickTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.need_redstone"), new MappedWidget(needRedstonePowerCheckbox));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.limit_range"), new MappedWidget(limitSoundRangeCheckbox));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.pos1"), new MappedWidget(corner1Widget));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "sound_looper.listview.title.pos2"), new MappedWidget(corner2Widget));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new SoundLooperUpdatePacket(blockPos, corner1Widget.getBlockPos(), corner2Widget.getBlockPos(), soundIdTextField.getValue(), soundCategory, (int)repeatTickTextField.getNumber(), soundVolumeTextField.getNumber() / 100f, needRedstonePowerCheckbox.isChecked(), limitSoundRangeCheckbox.isChecked()));
    }

    private void setSoundCategory(int category) {
        if(category >= SoundLooperBlockEntity.SOURCE_LIST.length) {
            this.soundCategory = 0;
        } else {
            this.soundCategory = category;
        }
        this.soundCategoryButton.setMessage((TextUtil.literal(SoundLooperBlockEntity.SOURCE_LIST[category].getName())));
    }
}
