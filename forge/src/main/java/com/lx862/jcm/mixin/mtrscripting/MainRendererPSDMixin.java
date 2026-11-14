package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.scripting.psd.PSDScriptRenderer;
import net.minecraft.world.phys.Vec3;
import com.lx862.jcm.mod.render.GraphicsHolder;
import org.mtr.render.MainRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.Camera;

/**
 * Yanyang/PSD: drives the PSD script render pass at the head of MTR's frame
 * render — every draw (the pane backing included) is SCHEDULED onto MTR's
 * queued layers here and joins the queued-layer flush at the end of that
 * same render call, after the block-entity phase drew (and, for bound
 * tops, was cancelled by {@link RenderRouteBaseMixin}) and before
 * vehicles. Camera anchoring is supplied to each scheduled callback at
 * flush time (see {@code PSDScriptRenderer#drawBacking}).
 */
@Mixin(value = MainRenderer.class, remap = false)
public class MainRendererPSDMixin {
    @Inject(method = "render(Lorg/mtr/mapping/mapper/GraphicsHolder;Lorg/mtr/mapping/holder/Vector3d;)V", at = @At("HEAD"))
    private static void jsblock$renderPsdScripts(GraphicsHolder graphicsHolder, Vec3 offset, CallbackInfo ci) {
        PSDScriptRenderer.renderAll();
    }
}
