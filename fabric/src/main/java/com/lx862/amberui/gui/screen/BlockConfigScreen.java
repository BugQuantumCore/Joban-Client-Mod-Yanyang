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
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiConsumer;

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

    /**
     * Applies a mutator to the CLIENT-side block entity(ies) of the block this
     * screen configures (every member of a multi-block structure).
     *
     * <p>This is the client-visible half of MTR's
     * {@code BlockEntityPacketHandler.send} pattern: the server's
     * {@code setChanged()} never broadcasts block entity data, so a GUI save
     * that only sends the update packet leaves the editing player's renderer
     * showing the OLD values until a block update or re-login — the config
     * looks "not saved". Screens should apply the same data here, right
     * before {@code sendPacketToServer}.</p>
     */
    protected void applyToClientBlockEntities(BiConsumer<BlockState, BlockEntity> mutator) {
        final Level world = Minecraft.getInstance().level;
        if(world == null) return;
        final BlockState state = world.getBlockState(blockPos);
        if(state.getBlock() instanceof com.lx862.jcm.mod.block.base.JCMBlock jcmBlock) {
            jcmBlock.loopStructure(state, world, blockPos, mutator);
        } else {
            final BlockEntity be = world.getBlockEntity(blockPos);
            if(be != null) mutator.accept(state, be);
        }
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
