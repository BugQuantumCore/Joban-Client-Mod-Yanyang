package com.lx862.jcm.mixin.mtrpatch;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;

/**
 * <h2>"Save and Quit to Title" instant-exit guard (build.121)</h2>
 *
 * <p>Root cause (user-verified 2026-11-22, PCL crash analyzer): Fabric fires
 * {@code ClientPlayConnectionEvents.DISCONNECT} from
 * {@code ClientConnection.channelInactive} on the <b>Netty Local Client IO</b>
 * thread (for the in-process integrated server channel, not the render
 * thread). MTR's {@code MTRClient} disconnect handler runs
 * {@code CachedFileProvider.close()} right there, which closes every cached
 * map tile's {@code VertexBuffer}. Vanilla tolerates that
 * ({@code VertexBuffer.close} defers GL work to the render thread), but
 * gpu_booster injects a {@code closeNamed} handler into {@code VertexBuffer}
 * that issues <b>direct DSA OpenGL calls</b> ({@code GL45C.glVertexArrayElementBuffer})
 * — and on a thread with no GL context LWJGL raises the native
 * {@code FATAL ERROR in native method ... No context is current} and aborts
 * the JVM: exit code 1, no crash report, no hs_err, no heapdump. All of that
 * matches the reported signature exactly.</p>
 *
 * <p>This guard defers the whole {@code close()} to the render thread when
 * called off it. The re-invocation is reflective: it re-enters the (mixed-in)
 * {@code close()} on the render thread, this handler sees the render thread
 * and passes through, and the original body — including vanilla's own and
 * gpu_booster's injected cleanup — runs where a GL context is current.
 * {@code Minecraft#execute} is the drain: the client task queue runs on the
 * render thread every frame (also on the title screen), so the deferred close
 * lands within one frame of the disconnect, long before any world rejoin.</p>
 *
 * <p>Shenandoah GC is not involved in any way — no JVM arguments change is
 * needed for this fix.</p>
 */
@Mixin(targets = "org.mtr.cache.CachedFileProvider", remap = false)
public abstract class CachedFileProviderCloseGuardMixin {

    @Inject(method = "close()V", at = @At("HEAD"), cancellable = true, remap = false)
    private void jsblock$deferCloseToRenderThread(CallbackInfo ci) {
        final Minecraft client = Minecraft.getInstance();
        if (client == null || !client.isSameThread()) {
            final Object self = this;
            final String what = "org.mtr.cache.CachedFileProvider.close() (MTR map tile vertex buffers)";
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
