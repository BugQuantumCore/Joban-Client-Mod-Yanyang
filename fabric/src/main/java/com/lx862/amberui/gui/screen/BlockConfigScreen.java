package com.lx862.amberui.gui.screen;

import com.lx862.amberui.gui.GuiHelper;
import com.lx862.jcm.mod.util.TextCategory;
import com.lx862.jcm.mod.util.TextUtil;
import org.mtr.core.data.Station;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import org.mtr.MTRClient;
import org.mtr.data.IGui;
import net.minecraft.client.gui.screens.Screen;

/**
 * GUI Screen for configuring block settings, you should extend this class for your own block config screen
 */
public abstract class BlockConfigScreen extends SavableScreen implements GuiHelper {
    protected final BlockPos blockPos;

    public BlockConfigScreen(BlockPos blockPos) {
        super(false);
        this.blockPos = blockPos;
    }

    protected int getContentWidth() {
        return (int)Math.min((width * 0.75), MAX_CONTENT_WIDTH);
    }

    @Override
    public MutableComponent getScreenSubtitle() {
        Station atStation = MTRClient.findStation(blockPos);

        if(atStation != null) {
            return TextUtil.translatable(TextCategory.GUI,
                    "block_config.subtitle_with_station",
                    blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                    IGui.formatStationName(atStation.getName())
            );
        } else {
            return TextUtil.translatable(TextCategory.GUI,
                    "block_config.subtitle",
                    blockPos.getX(), blockPos.getY(), blockPos.getZ()
            );
        }
    }
}
