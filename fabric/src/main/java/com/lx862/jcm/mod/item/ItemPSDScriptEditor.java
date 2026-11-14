package com.lx862.jcm.mod.item;

import com.lx862.jcm.mod.gui.screen.PSDScriptSelectionScreen;
import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import com.lx862.jcm.mod.scripting.psd.PSDWrapper;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.mtr.block.BlockPSDAPGBase;
import org.mtr.block.BlockPSDAPGGlassBase;
import org.mtr.block.BlockPSDTop;
import org.mtr.block.IBlock;

import java.util.List;
import net.minecraft.world.item.Item;

/**
 * The "PSD Script Editor" (屏蔽门脚本编辑器) — right-click any platform screen
 * door glass (or glass end, PSD or APG, from MTR or any addon building on
 * MTR's PSD classes) to bind a {@code psdScripts} entry of type "glass" or a
 * scripted PIDS preset onto it, and any psd_top block to bind a type "top"
 * entry or PIDS preset. Lives in the same creative tab as MTR's brush.
 *
 * <p>Yanyang.6: the editor manages the <b>whole surface the clicked block
 * belongs to</b>, not just the clicked column — the renderer draws one merged
 * canvas per glass segment / psd_top group, so a choice (including the
 * "no script" entry) binds or unbinds <b>every block of that segment or
 * group</b>: both halves of every glass column, or the group's psd_top
 * blocks. Previously the binding lived only on the clicked column's two
 * halves, and trying to unbind from any other block of the same glass left
 * the (visually merged) script running with no way to remove it.</p>
 */
public class ItemPSDScriptEditor extends Item {

    public ItemPSDScriptEditor(Item.Properties itemSettings) {
        super(itemSettings.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        final Level world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = world.getBlockState(pos);
        final Object block = state.getBlock();

        if (block instanceof BlockPSDAPGGlassBase) {
            if (world.isClientSide()) {
                final Direction facing = IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING);
                /* The segment is identified by its LOWER row — clicking
                   either half of any column resolves to the same segment. */
                BlockPos base = pos;
                final BlockState lowerState = world.getBlockState(pos.below());
                if (IBlock.getStatePropertySafe(state, IBlock.HALF) == IBlock.DoubleBlockHalf.UPPER
                        && PSDRuns.isGlass(lowerState)
                        && IBlock.getStatePropertySafe(lowerState, IBlock.HALF) == IBlock.DoubleBlockHalf.LOWER
                        && IBlock.getStatePropertySafe(lowerState, com.lx862.jcm.mod.data.BlockProperties.FACING) == facing) {
                    base = pos.below();
                }
                final PSDRuns.Run run = PSDRuns.glassRun(world, base, facing);
                final List<BlockPos> positions = PSDRuns.glassRunPositions(world, run, facing);
                Minecraft.getInstance().setScreen(new PSDScriptSelectionScreen(positions, PSDWrapper.PSDType.GLASS));
            }
            return InteractionResult.SUCCESS;
        }
        if (block instanceof BlockPSDTop) {
            if (world.isClientSide()) {
                final Direction facing = IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING);
                final PSDRuns.Run run = PSDRuns.topRun(world, pos, facing);
                final List<BlockPos> positions = PSDRuns.topRunPositions(run, facing);
                Minecraft.getInstance().setScreen(new PSDScriptSelectionScreen(positions, PSDWrapper.PSDType.TOP));
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
