package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.block.APGGlassDRL;
import com.lx862.jcm.mod.block.APGGlassEndDRL;
import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.block.BlockPSDTop;
import org.mtr.block.IBlock;
import org.mtr.render.RenderRouteBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Yanyang/PSD: when a script is bound to a psd_top block, the original
 * content (direction arrow / route map strip and the platform color strip)
 * must no longer render on <b>the psd_top group the bound block belongs
 * to</b> — the stretch sitting on one wall section below (one brush-merged
 * door run or one glass segment; see {@code PSDScriptRenderer} /
 * {@code PSDRuns}, which derive the group from the wall below because the
 * band's own SIDE_EXTENDED markers are only meaningful above a full-height
 * PSD wall). The script canvas spans exactly that group, so every block of
 * the group drops its slice, not just the bound one — while the
 * <b>neighbouring groups</b> (above other doors and glass segments) keep
 * MTR's own arrows and route maps.
 *
 * <p>Also cancels for <b>stale MTR block entities on JCM DRL glass</b>:
 * worlds that placed {@code jsblock:apg_glass_drl} /
 * {@code jsblock:apg_glass_end_drl} with an older JCM — before those blocks
 * overrode {@code newBlockEntity} with their own
 * {@code jsblock:apg_glass_1} type — still carry MTR
 * {@code mtr:apg_glass} block entities in their chunk NBT. Those keep
 * rendering through MTR's RenderAPGGlass (renderer selection follows the BE
 * type), painting the arrow/route map, station-name strip and color strip at
 * the top of the upper glass half — but the DRL model has no pane there
 * (just a ~2/16 light strip), so the content floats in mid-air. No MTR
 * route content is ever wanted on a JCM DRL glass block, so the whole
 * RenderRouteBase pass is dropped whenever the block at the entity's
 * position is one of ours. (Those stale entities also can't tick — vanilla
 * logs "invalid for ticking" for them — which is harmless: the shared
 * PSD/APG block entity tick is a no-op.)</p>
 */
@Mixin(value = RenderRouteBase.class, remap = false)
public class RenderRouteBaseMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void jsblock$cancelForScriptedTop(BlockPSDTop.BlockEntityBase entity, float tickDelta, GraphicsHolder graphicsHolder, int light, int overlay, CallbackInfo ci) {
        if (entity instanceof BlockPSDTop.BlockEntity) {
            final Level world = entity.getLevel();
            final BlockPos pos = entity.getBlockPos();
            if (world != null) {
                final BlockState state = world.getBlockState(pos);
                if (state.getBlock() instanceof BlockPSDTop) {
                    final Direction facing = IBlock.getStatePropertySafe(state, BlockPSDTop.FACING);
                    if (PSDRuns.topGroupHasScriptBinding(world, pos, facing)) {
                        ci.cancel();
                        return;
                    }
                }
            }
        }

        /* Stale MTR apg_glass/apg_glass_end BEs sitting on JCM DRL glass
           blocks (old worlds, see class doc) — MTR's own route content must
           not float over the DRL's open upper half. */
        final Level world = entity.getLevel();
        if (world != null) {
            final Object block = world.getBlockState(entity.getBlockPos()).getBlock();
            if (block instanceof APGGlassDRL || block instanceof APGGlassEndDRL) {
                ci.cancel();
            }
        }
    }
}
