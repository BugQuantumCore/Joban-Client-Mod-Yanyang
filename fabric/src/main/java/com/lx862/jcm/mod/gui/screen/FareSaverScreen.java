package com.lx862.jcm.mod.gui.screen;

import com.lx862.jcm.mod.network.block.FareSaverUpdatePacket;
import com.lx862.jcm.mod.registry.Blocks;
import com.lx862.jcm.mod.registry.Networking;
import com.lx862.amberui.gui.screen.BlockConfigListScreen;
import com.lx862.amberui.gui.widget.AlwaysRenderedTextField;
import com.lx862.amberui.gui.widget.IntegerTextField;
import com.lx862.amberui.gui.widget.ListViewWidget;
import com.lx862.amberui.gui.widget.MappedWidget;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.MutableComponent;
import com.lx862.jcm.mod.render.TextCase;
import com.lx862.jcm.mod.render.TextCase;

public class FareSaverScreen extends BlockConfigListScreen {
    private long discount;
    private String prefix;

    public FareSaverScreen(BlockPos blockPos, String prefix, int discount) {
        super(blockPos);
        this.discount = discount;
        this.prefix = prefix;
    }

    @Override
    public MutableComponent getScreenTitle() {
        return com.lx862.jcm.mod.registry.Blocks.FARE_SAVER.get().getName();
    }

    @Override
    public void addConfigEntries(ListViewWidget listViewWidget) {
        AlwaysRenderedTextField prefixTextField = new AlwaysRenderedTextField(0, 0, 60, 20, 4, TextCase.DEFAULT, null, "$");
        prefixTextField.setResponder(newValue -> this.prefix = newValue);
        prefixTextField.setValue(this.prefix);

        IntegerTextField discountTextField = new IntegerTextField(0, 0, 60, 20, 0, 1000000, 2);
        discountTextField.onChange(newValue -> this.discount = newValue);
        discountTextField.setValue(this.discount);

        addRenderableWidget(prefixTextField);
        addRenderableWidget(discountTextField);
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "faresaver.prefix"), new MappedWidget(prefixTextField));
        listViewWidget.add(TextUtil.translatable(TextCategory.GUI, "faresaver.discount"), new MappedWidget(discountTextField));
    }

    @Override
    public void onSave() {
        Networking.sendPacketToServer(new FareSaverUpdatePacket(blockPos, this.prefix, (int)this.discount));
    }
}
