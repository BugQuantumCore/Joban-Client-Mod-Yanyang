package com.lx862.jcm.mod.gui.screen;

import com.lx862.jcm.mod.network.block.ButterflyLightUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.amberui.gui.screen.BlockConfigListScreen;
import com.lx862.amberui.gui.widget.IntegerTextField;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.MutableComponent;

public class ButterflyLightScreen extends BlockConfigListScreen {
    private long value;

    public ButterflyLightScreen(BlockPos blockPos, int startBlinkingSeconds) {
        super(blockPos);
        this.value = startBlinkingSeconds;
    }

    @Override
    public MutableComponent getScreenTitle() {
        return com.lx862.jcm.mod.registry.Blocks.BUTTERFLY_LIGHT.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        IntegerTextField startBlinkingSecTextField = new IntegerTextField(0, 0, 60, 20, 0, 100000, 10);
        startBlinkingSecTextField.setValue(value);
        startBlinkingSecTextField.onChange(newValue -> {
            this.value = newValue;
        });

        addRenderableWidget(startBlinkingSecTextField);
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "butterfly_light.countdown"), new MappedWidget(startBlinkingSecTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new ButterflyLightUpdatePacket(blockPos, (int) this.value));
    }
}
