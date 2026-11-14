package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.scripting.psd.PSDScriptRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.mtr.render.MainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives PSD screen-door scripts once per frame from MTR 4.1's
 * {@code MainRenderer.render(PoseStack, MultiBufferSource, Vec3)} (the
 * GraphicsHolder-based signature is gone).
 */
@Mixin(value = MainRenderer.class, remap = false)
public class MainRendererPSDMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private static void jsblock$renderPsdScripts(PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, Vec3 cameraShakeOffset, CallbackInfo ci) {
        PSDScriptRenderer.renderAll();
    }
}
