package com.lx862.jcm.mixin.mtrpatch;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * Render-thread guard for {@code org.mtr.client.DynamicTextureCache#close()} —
 * the same deferral as {@link CachedFileProviderCloseGuardMixin}, for the
 * other teardown MTR's disconnect handler runs on the Netty Local Client IO
 * thread. {@code DynamicTextureCache.close()} releases every MTR dynamic
 * texture through {@code TextureManager.release()}; any third-party GL hook
 * on that path (same class as gpu_booster's VertexBuffer injection) would
 * abort the JVM with no context, with the same silent exit-code-1 signature.
 */
@Mixin(targets = "org.mtr.client.DynamicTextureCache", remap = false)
public abstract class DynamicTextureCacheCloseGuardMixin {

    @Inject(method = "close()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void jsblock$deferCloseToRenderThread(CallbackInfo ci) {
        final Minecraft client = Minecraft.getInstance();
        if (client == null || !client.isSameThread()) {
            final Object self = this;
            final String what = "org.mtr.client.DynamicTextureCache.close() (MTR dynamic textures)";
            com.lx862.jcm.mod.util.JCMLogger.warn(
                            "[JCM] Deferring {} to the render thread (called from '{}', which has no GL context).",
                            what, Thread.currentThread().getName());
            if (client != null) {
                client.execute(() -> jsblock$invokeClose(what, self));
            }
            ci.cancel();
        }
    }

    private static void jsblock$invokeClose(String what, Object target) {
        try {
            final Method close = target.getClass().getMethod("close");
            close.invoke(target);
        } catch (Throwable t) {
            com.lx862.jcm.mod.util.JCMLogger.error("[JCM] Deferred {} failed: {}", what, t.toString());
        }
    }
}
