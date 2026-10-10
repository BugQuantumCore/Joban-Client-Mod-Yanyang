package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.util.GraphicsTexture;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawMeshBuilderJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import org.mtr.mapping.holder.Identifier;

import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.List;

/**
 * NativeHost — the JVM-side owner of every resource a C++ script asks for.
 *
 * <p>The bridge ({@code native/jni/jni_bridge.cpp}) never touches OpenGL or MTR
 * classes. While a module runs {@code mtrCreate} it only RECORDS the textures
 * and quads the script described, then calls {@link #createResources} once per
 * script instance on the render thread. This class turns that batch into real
 * objects — {@link GraphicsTexture} for pixels, {@link ModelJS} for geometry —
 * and returns the handle arrays the frame records must reference.
 *
 * <h2>Handle = SLOT, not id</h2>
 * The bridge rewrites every handle in a frame record through the array returned
 * by {@link #createResources}: <em>slot</em> i becomes {@code textureHandles[i]}
 * and quad slot q becomes {@code modelHandles[q]}. That is why the two lookup
 * methods here are plain array indexing rather than hash lookups on the hot
 * path, and why a stale instance releases by SLOT too.
 *
 * <p>Both entry points are looked up by JNI signature from the bridge — the
 * strings live in {@code jni_bridge.cpp} and must be updated in lockstep:
 * <pre>
 *   int[] createResources(int[] widths, int[] heights,
 *                         float[] quadVertices, float[] quadUv,
 *                         int[] quadTextureSlot, int[] quadStages)
 *   void  uploadPixels(int textureHandle, int x, int y, int w, int h, byte[] rgba)
 * </pre>
 */
public final class NativeHost {

    /** Render stages, matching the JS slot "interior"/"exterior" wording. */
    public static final int STAGE_EXTERIOR = 0;
    public static final int STAGE_INTERIOR = 1;

    private static final NativeHost INSTANCE = new NativeHost();

    /** One script instance's resources, addressed by slot. */
    private static final class Instance {
        GraphicsTexture[] textures = new GraphicsTexture[0];
        ModelJS[] models = new ModelJS[0];

        void close() {
            for (ModelJS model : models) {
                if (model == null) continue;
                try {
                    model.close();
                } catch (Throwable ignored) {
                }
            }
            for (GraphicsTexture tex : textures) {
                if (tex == null) continue;
                try {
                    tex.close();
                } catch (Throwable ignored) {
                }
            }
            models = new ModelJS[0];
            textures = new GraphicsTexture[0];
        }
    }

    /** instanceKey -> the resources that instance created. */
    private final java.util.Map<String, Instance> instances = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The instance currently being created. {@code createResources} is called
     * from {@code mtrRender} while {@code mtrCreate} runs for exactly one
     * instance, and the bridge serialises render calls per module, so a single
     * pending slot is enough — the driver tells us which key to file it under.
     */
    private String pendingInstanceKey;

    /** Instance whose frame is currently being applied (see {@link #setActiveInstance}). */
    private volatile String activeInstanceKey;

    private NativeHost() {
    }

    public static NativeHost get() {
        return INSTANCE;
    }

    /** Driver hook: which instance the next {@link #createResources} belongs to. */
    public void setPendingInstance(String instanceKey) {
        this.pendingInstanceKey = instanceKey;
    }

    /**
     * Driver hook: which instance the frame being applied belongs to.
     * Handle uploads only carry a per-instance SLOT, so the driver names the
     * instance before translating a frame.
     */
    public void setActiveInstance(String instanceKey) {
        this.activeInstanceKey = instanceKey;
    }

    /* ------------------------------------------------------------------ */
    /* Bridge entry point 1: build everything one instance asked for       */
    /* ------------------------------------------------------------------ */

    /**
     * @param widths          texture widths  (one per create_texture call)
     * @param heights         texture heights (one per create_texture call)
     * @param quadVertices    12 floats per quad (4 vertices, model space)
     * @param quadUv          8 floats per quad (u0,v0 .. u3,v3)
     * @param quadTextureSlot texture slot each quad samples
     * @param quadStages      render stage per quad (STAGE_*)
     * @return {@code [texture handles..., model handles...]}; a model handle of
     *         -1 means "do not draw this quad".
     */
    public int[] createResources(int[] widths, int[] heights,
                                 float[] quadVertices, float[] quadUv,
                                 int[] quadTextureSlot, int[] quadStages) {
        final int texCount = widths == null ? 0 : widths.length;
        final int quadCount = quadTextureSlot == null ? 0 : quadTextureSlot.length;
        final int[] out = new int[texCount + quadCount];

        final Instance instance = new Instance();
        instance.textures = new GraphicsTexture[texCount];
        instance.models = new ModelJS[quadCount];

        /* 1) textures */
        for (int i = 0; i < texCount; i++) {
            final int w = Math.max(1, widths[i]);
            final int h = Math.max(1, heights[i]);
            try {
                instance.textures[i] = new GraphicsTexture(w, h);
            } catch (Throwable t) {
                JCMLogger.error("NativeHost: failed to create a {}x{} GraphicsTexture: {}", w, h, t.toString());
            }
            out[i] = i;   /* SLOT index: the bridge rewrites records through this */
        }

        /* 2) quad models; several quads may share one texture */
        for (int q = 0; q < quadCount; q++) {
            out[texCount + q] = -1;
            final int slot = quadTextureSlot[q];
            if (slot < 0 || slot >= texCount) continue;
            final GraphicsTexture tex = instance.textures[slot];
            if (tex == null) continue;
            try {
                instance.models[q] = buildQuad(quadVertices, q, quadUv, q,
                        quadStages == null || q >= quadStages.length ? STAGE_INTERIOR : quadStages[q],
                        tex.identifier);
                if (instance.models[q] != null) out[texCount + q] = q;
            } catch (Throwable t) {
                JCMLogger.error("NativeHost: failed to build a quad model: {}", t.toString());
            }
        }

        /* File it under the instance the driver announced, and make sure a
           stale instance from an earlier build (car count changed, script
           reloaded) is torn down instead of leaking its textures. */
        final String key = pendingInstanceKey;
        if (key != null) {
            final Instance previous = instances.put(key, instance);
            if (previous != null) previous.close();
        }
        return out;
    }

    /**
     * Port of JCM's own DisplayHelper mesh construction: a 4-vertex quad in the
     * SAME axis convention {@link RawMeshBuilderJS#vertex} uses ({@code x, -y,
     * -z}), one shared material, and the {@link GraphicsTexture}'s dynamic
     * identifier as the texture — so live pixel updates show up without any
     * model re-upload.
     */
    private ModelJS buildQuad(float[] verts, int quadIndex, float[] uv, int uvIndex,
                              int stage, Identifier texture) {
        if (verts == null || verts.length < (quadIndex + 1) * 12) return null;
        if (uv == null || uv.length < (uvIndex + 1) * 8) return null;

        final String renderType = stage == STAGE_EXTERIOR ? "exterior" : "interior";
        final RawMeshBuilderJS builder = new RawMeshBuilderJS(4, renderType, texture);
        final int vi = quadIndex * 12;
        final int ui = uvIndex * 8;
        for (int v = 0; v < 4; v++) {
            builder.vertex(verts[vi + v * 3], verts[vi + v * 3 + 1], verts[vi + v * 3 + 2]);
            /* Display panels are thin two-sided quads; a fixed normal keeps the
               shader happy and matches what the JS DisplayHelper produced. */
            builder.normal(0, 0, 1);
            builder.uv(uv[ui + v * 2], uv[ui + v * 2 + 1]);
            builder.endVertex();
        }
        final RawModelJS raw = new RawModelJS(builder.asRawModel());
        final ModelJS model = ModelManagerJS.upload(raw);
        if (model == null) return null;
        /* The JS DisplayHelper never applies a matrix either: the slot quad is
           already authored in car space, so drawCarModel(..., null) is right. */
        return model;
    }

    /* ------------------------------------------------------------------ */
    /* Bridge entry point 2: blit one dirty rect of RGBA8 pixels            */
    /* ------------------------------------------------------------------ */

    /**
     * The C++ side hands raw row-major RGBA8 — its little-endian ARGB
     * {@code uint32} storage, byte for byte. {@link GraphicsTexture} backs
     * itself with a {@code TYPE_INT_ARGB} BufferedImage whose DataBufferInt is
     * the very memory {@code upload()} reads, so writing straight into that
     * int[] is both correct and far cheaper than per-pixel calls.
     */
    public void uploadPixels(int textureHandle, int x, int y, int width, int height, byte[] rgba) {
        if (rgba == null) return;
        final GraphicsTexture tex = texture(textureHandle, activeInstanceKey);
        if (tex == null) return;

        if (!(tex.bufferedImage.getRaster().getDataBuffer() instanceof DataBufferInt)) {
            return;
        }
        final int[] dst = ((DataBufferInt) tex.bufferedImage.getRaster().getDataBuffer()).getData();
        final int imgW = tex.bufferedImage.getWidth();
        final int imgH = tex.bufferedImage.getHeight();

        int sx = x, sy = y, w = width, h = height;
        /* Defensive clamp: gfx.hpp already clamps mark_dirty, but a bad module
           must not be able to corrupt the heap through this entry point. */
        if (sx < 0) { w += sx; sx = 0; }
        if (sy < 0) { h += sy; sy = 0; }
        if (sx + w > imgW) w = imgW - sx;
        if (sy + h > imgH) h = imgH - sy;
        if (w <= 0 || h <= 0) return;
        if (rgba.length < w * h * 4) {
            JCMLogger.error("NativeHost: uploadPixels got {} bytes for a {}x{} rect",
                    rgba.length, w, h);
            return;
        }

        int src = 0;
        for (int row = 0; row < h; row++) {
            int di = (sy + row) * imgW + sx;
            for (int col = 0; col < w; col++) {
                final int r = rgba[src] & 0xFF;
                final int g = rgba[src + 1] & 0xFF;
                final int b = rgba[src + 2] & 0xFF;
                final int a = rgba[src + 3] & 0xFF;
                dst[di++] = (a << 24) | (r << 16) | (g << 8) | b;
                src += 4;
            }
        }

        /* GraphicsTexture.upload() copies the whole image and records a GL
           upload on the render-call queue — the same thing the JS scripts do
           every frame (they call upload() with no arguments too). */
        tex.upload();
    }

    /* ------------------------------------------------------------------ */
    /* Lookups used by the draw-call replay                                */
    /* ------------------------------------------------------------------ */

    /**
     * The model behind a handle. Handles are per-instance slots, and MTR can
     * have several script instances alive at once (every vehicle), so the
     * driver always passes its instance key when it has one; the null fallback
     * scans all instances and is only used by the bridge's upload path, where
     * the handle already encodes the right instance.
     */
    public ModelJS model(int handle) {
        if (handle < 0) return null;
        for (Instance instance : instances.values()) {
            if (handle < instance.models.length && instance.models[handle] != null) {
                return instance.models[handle];
            }
        }
        return null;
    }

    /** Texture behind a handle within one instance ({@code null} key = scan). */
    public GraphicsTexture texture(int handle, String instanceKey) {
        if (handle < 0) return null;
        if (instanceKey != null) {
            final Instance instance = instances.get(instanceKey);
            if (instance == null || handle >= instance.textures.length) return null;
            return instance.textures[handle];
        }
        for (Instance instance : instances.values()) {
            if (handle < instance.textures.length && instance.textures[handle] != null) {
                return instance.textures[handle];
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------ */
    /* Lifetime                                                            */
    /* ------------------------------------------------------------------ */

    /** Drop one instance's resources (script instance dropped / state rebuilt). */
    public void releaseInstance(String instanceKey) {
        final Instance instance = instances.remove(instanceKey);
        if (instance != null) instance.close();
    }

    /** Drop every resource (script reload / world unload). */
    public void reset() {
        final List<Instance> all = new ArrayList<>(instances.values());
        instances.clear();
        pendingInstanceKey = null;
        for (Instance instance : all) instance.close();
    }
}
