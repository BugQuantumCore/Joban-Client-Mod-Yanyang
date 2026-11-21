package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.block.APGGlassDRL;
import com.lx862.jcm.mod.block.APGGlassEndDRL;
import com.lx862.jcm.mod.scripting.psd.PSDRuns;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.mtr.block.BlockPSDAPGGlassBase;
import org.mtr.block.BlockPSDTop;
import org.mtr.block.IBlock;
import org.mtr.render.RenderRouteBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Yanyang/PSD: when a script is bound to a psd_top group or a glass segment,
 * the ORIGINAL content (direction arrow / route map strip and the platform
 * color strip, plus the station-name strip MTR paints on APG glass) must no
 * longer render on <b>the group / segment the bound block belongs to</b> —
 * the stretch sitting on one wall section below (one brush-merged door run
 * or one glass segment; see {@code PSDScriptRenderer} / {@code PSDRuns},
 * which derive the group from the wall below because the band's own
 * SIDE_EXTENDED markers are only meaningful above a full-height PSD wall).
 * The script canvas spans exactly that group/segment, so every block of it
 * drops its slice, not just the bound one — while the <b>neighbouring
 * groups</b> (above other doors and glass segments) keep MTR's own arrows
 * and route maps.
 *
 * <p>The cancellation keys off the BLOCK at the entity's position, not the
 * entity type: psd_top group bindings cancel every {@code BlockPSDTop}
 * entity's pass, and glass segment bindings cancel the pass of ANY entity
 * sitting on a scripted glass block — that covers MTR's own APG glass
 * entities too (their renderer paints the same content family onto the
 * upper glass half), which the previous {@code instanceof
 * BlockPSDTop.PSDTopBlockEntity} check silently missed. The Tianjin Metro
 * mod's renderers do not go through this class at all; they are hooked by
 * {@link TianjinRenderRouteBaseMixin}.</p>
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
    private void jsblock$cancelForScriptedPsd(BlockPSDTop.BlockEntityBase entity, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, ClientLevel world, LocalPlayer player, float tickDelta, int light, int overlay, CallbackInfo ci) {
        final Level level = entity.getLevel();
        if (level != null) {
            final BlockPos pos = entity.getBlockPos();
            final BlockState state = level.getBlockState(pos);
            final Object block = state.getBlock();
            if (block instanceof BlockPSDTop) {
                final Direction facing = IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING);
                if (PSDRuns.topGroupHasScriptBinding(level, pos, facing)) {
                    ci.cancel();
                    return;
                }
            } else if (block instanceof BlockPSDAPGGlassBase) {
                final Direction facing = IBlock.getStatePropertySafe(state, com.lx862.jcm.mod.data.BlockProperties.FACING);
                if (PSDRuns.glassRunHasScriptBinding(level, pos, facing)) {
                    ci.cancel();
                    return;
                }
            }
        }

        /* Stale MTR apg_glass/apg_glass_end BEs sitting on JCM DRL glass
           blocks (old worlds, see class doc) — MTR's own route content must
           not float over the DRL's open upper half. */
        final Level level2 = entity.getLevel();
        if (level2 != null) {
            final Object block = level2.getBlockState(entity.getBlockPos()).getBlock();
            if (block instanceof APGGlassDRL || block instanceof APGGlassEndDRL) {
                ci.cancel();
            }
        }
    }
}
