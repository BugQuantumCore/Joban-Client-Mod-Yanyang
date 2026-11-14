package com.lx862.jcm.mixin.mtrpatch;

import com.lx862.jcm.mod.config.JCMClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.mtr.render.RenderRails;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MTR 4.1 rail render patches.
 *
 * <p>Kept: {@code disableRailRendering} (config {@code mtrPatch.disableRailRendering}).
 * Dropped in the 4.1 port (hooks into internals that no longer exist, with
 * equivalent behaviour now built into MTR 4.1's own render pipeline):</p>
 * <ul>
 *   <li>rail frustum culling / culling improvement — 4.1 culls rails through
 *       its own occlusion pipeline before the render loop;</li>
 *   <li>the mid-gradient debug stat on the rail overlay — 4.1's
 *       {@code renderRailStats} was restructured (extra PoseStack / camera
 *       parameters), and the per-rail lambda locals it captured are gone.</li>
 * </ul>
 */
@Mixin(value = RenderRails.class, remap = false)
public class RenderRailsMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void jsblock$cancelRailRendering(PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, Vec3 cameraShakeOffset, CallbackInfo ci) {
        if (JCMClientConfig.INSTANCE.mtrPatch.disableRailRendering.value()) {
            ci.cancel();
        }
    }
}
