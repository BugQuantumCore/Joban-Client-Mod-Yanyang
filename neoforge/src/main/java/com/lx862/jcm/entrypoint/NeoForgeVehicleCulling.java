package com.lx862.jcm.entrypoint;

import com.lx862.jcm.mixin.mtrpatch.WorldRendererAccessor;
import com.lx862.jcm.nativeapi.NativeCullFrustum;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;

/**
 * NeoForge half of native vehicle-script frustum culling.
 *
 * <p>Installs a live frustum supplier into {@link NativeCullFrustum} - the
 * vanilla {@code LevelRenderer#cullingFrustum} field, read through the shared
 * {@code WorldRendererAccessor} mixin whenever culling asks (the shared
 * {@code com.lx862.jcm.nativeapi.VehicleCulling} only asks from inside the
 * vehicle render pass, after the field received the current frame's frustum).
 *
 * <p>Why not an event like the Fabric side ({@code FabricVehicleCulling}):
 * although NeoForge's {@code RenderLevelStageEvent} carries a frustum, the
 * vehicle pass can be driven by MTR's native (Beryl) renderer whose frame
 * boundaries do not line up with the stage set, and a single live read needs
 * none of that - it works identically no matter which pipeline drives the
 * frame. See {@link NativeCullFrustum}'s javadoc for the two publication
 * modes.
 *
 * <p>Fails open by construction: any throwable inside the supplier yields
 * {@code null}, which {@code VehicleCulling} treats as "no frustum - render
 * everything", exactly the pre-culling behavior.
 */
public final class NeoForgeVehicleCulling {
    private NeoForgeVehicleCulling() {}

    /* Render-thread confined (the supplier only runs inside the vehicle
       render pass): the raw frustum instance is stable for a whole pass, so
       the snapshot copy happens once per frame, not once per vehicle. */
    private static Frustum lastRaw;
    private static Frustum lastHolder;

    public static void init() {
        NativeCullFrustum.setLive(NeoForgeVehicleCulling::supply);
    }

    private static Frustum supply() {
        try {
            final LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
            if (levelRenderer == null) return null;
            final Frustum raw = ((WorldRendererAccessor) levelRenderer).jsblock$getFrustum();
            if (raw == null) return null;
            if (raw != lastRaw) {
                lastRaw = raw;
                /* Copy constructor: snapshot this pass's frustum so a later
                   reconfigure of the raw instance can never leak into an
                   in-flight culling query. */
                lastHolder = new Frustum(raw);
            }
            return lastHolder;
        } catch (Throwable t) {
            /* A culling aid must never break world rendering. */
            return null;
        }
    }
}
