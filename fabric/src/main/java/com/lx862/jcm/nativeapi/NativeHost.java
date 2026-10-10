package com.lx862.jcm.nativeapi;

import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.mtrscripting.core.util.GraphicsTexture;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawMeshBuilderJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import org.mtr.mapping.holder.Identifier;
import org.mtr.mapping.mapper.OptimizedRenderer;

import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NativeHost — the JVM-side owner of every resource a C++ script asks for.
 *
 * <p>The bridge (native/jni/jni_bridge.cpp) never touches OpenGL or MTR
 * classes: while a module runs {@code mtrCreate} it only RECORDS the
 * textures and quads the script described, then calls
 * {@link #createResources} once per script instance on the render thread.
 * This class turns that batch into real objects — {@link GraphicsTexture}
 * for pixels, {@link ModelJS} for geometry — and returns the handle arrays
 * the frame records must reference.
 *
 * <p>Handles are plain ints (1-based); 0 means "nothing". They are only
 * meaningful to this host object, which is exactly what the C ABI assumes.
 *
 * <p>Both entry points are looked up by JNI signature from the bridge:
 * <pre>
 *   int[] createResources(int[] widths, int[] heights,
 *                         float[] quadVertices, float[] quadUv,
 *                         int[] quadTextureSlot, int[] quadStages)
 *   void  uploadPixels(int textureHandle, int x, int y, int w, int h, byte[] rgba)
 * </pre>
 * Renaming or re-typing either method breaks the bridge — the strings in
 * {@code jni_bridge.cpp} must be updated in lockstep.
 */
public final class NativeHost {

    /** Render stages, matching the JS slot "interior"/"exterior" wording. */
    public static final int STAGE_EXTERIOR = 0;
    public static final int STAGE_INTERIOR = 1;

    private static final NativeHost INSTANCE = new NativeHost();

    /** texture handle -> live texture. */
    private final Map<Integer, GraphicsTexture> textures = new HashMap<>();
    /** model handle -> live model (uploaded quad). */
    private final Map<Integer, ModelJS> models = new HashMap<>();
    /** texture handle -> the texture it binds (for release bookkeeping). */
    private final Map<Integer, Integer> modelTexture = new HashMap<>();

    private int nextHandle = 1;

    private NativeHost() {
    }

    public static NativeHost get() {
        return INSTANCE;
    }

    /* ------------------------------------------------------------------ */
    /* Bridge entry point 1: build everything one instance asked for       */
    /* ------------------------------------------------------------------ */

    /**
     * @param widths          texture widths  (one per create_texture call)
     * @param heights         texture heights (one per create_texture call)
     * @param quadVertices    12 floats per quad (4 vertices, model space)
     * @param quadUv          8 floats per quad (u0,v0 .. u3,v3)
     * @param quadTextureSlot which texture slot each quad samples
     * @param quadStages      render stage per quad (STAGE_*)
     * @return [texture handles..., model handles...] — model handle -1 when
     *         the quad could not be built, so the script skips the draw.
     */
    public int[] createResources(int[] widths, int[] heights,
                                 float[] quadVertices, float[] quadUv,
                                 int[] quadTextureSlot, int[] quadStages) {
        final int texCount = widths == null ? 0 : widths.length;
        final int quadCount = quadTextureSlot == null ? 0 : quadTextureSlot.length;
        final int[] out = new int[texCount + quadCount];

        /* 1) textures */
        for (int i = 0; i < texCount; i++) {
            final int w = Math.max(1, widths[i]);
            final int h = Math.max(1, heights[i]);
            try {
                final GraphicsTexture tex = new GraphicsTexture(w, h);
                final int handle = nextHandle++;
                textures.put(handle, tex);
                out[i] = handle;
            } catch (Throwable t) {
                JCMLogger.error("NativeHost: failed to create a {}x{} GraphicsTexture: {}", w, h, t.toString());
                out[i] = 0;
            }
        }

        /* 2) quad models (one shared ModelJS per uploaded mesh) */
        for (int q = 0; q < quadCount; q++) {
            final int slot = quadTextureSlot[q];
            final int texHandle = slot >= 0 && slot < texCount ? out[slot] : 0;
            if (texHandle == 0) {
                out[texCount + q] = -1;   /* no texture -> nothing to draw */
                continue;
            }
            final GraphicsTexture tex = textures.get(texHandle);
            if (tex == null) {
                out[texCount + q] = -1;
                continue;
            }
            try {
                final ModelJS model = buildQuad(quadVertices, q, quadUv, q,
                        quadStages[q], tex.identifier);
                if (model == null) {
                    out[texCount + q] = -1;
                    continue;
                }
                final int handle = nextHandle++;
                models.put(handle, model);
                modelTexture.put(handle, texHandle);
                out[texCount + q] = handle;
            } catch (Throwable t) {
                JCMLogger.error("NativeHost: failed to build a quad model: {}", t.toString());
                out[texCount + q] = -1;
            }
        }
        return out;
    }

    /**
     * Port of JCM's own DisplayHelper mesh construction: a 4-vertex quad with
     * the SAME axis convention {@link RawMeshBuilderJS#vertex} uses
     * (x, -y, -z), one shared material, texture = the GraphicsTexture's
     * dynamic identifier (so live pixel updates show up without a re-upload).
     */
    private ModelJS buildQuad(float[] verts, int vOff, float[] uv, int uvOff,
                              int stage, Identifier texture) {
        if (verts == null || verts.length < (vOff + 1) * 12) return null;
        if (uv == null || uv.length < (uvOff + 1) * 8) return null;
        if (!OptimizedRenderer.hasOptimizedRendering()) {
            /* ModelJS.draw() is a no-op without the optimized renderer; skip
               building so we do not leak a mesh for nothing. */
            return null;
        }
        final int vi = vOff * 12;
        final int ui = uvOff * 8;
        final String renderType = stage == STAGE_EXTERIOR ? "exterior" : "interior";

        final RawMeshBuilderJS builder = new RawMeshBuilderJS(4, renderType, texture);
        for (int v = 0; v < 4; v++) {
            builder.vertex(
                    verts[vi + v * 3], verts[vi + v * 3 + 1], verts[vi + v * 3 + 2]);
            /* The mesh's own normal: the quad is two-sided enough for a
               display panel, so a fixed +Z keeps the shader happy. */
            builder.normal(0, 0, 1);
            builder.uv(uv[ui + v * 2], uv[ui + v * 2 + 1]);
            builder.endVertex();
        }
        final RawModelJS raw = new RawModelJS(builder.asRawModel());
        return ModelManagerJS.upload(raw);
    }

    /* ------------------------------------------------------------------ */
    /* Bridge entry point 2: blit one dirty rect of RGBA8 pixels           */
    /* ------------------------------------------------------------------ */

    /**
     * The C++ side hands raw row-major RGBA8 (its little-endian ARGB
     * {@code uint32} storage, byte for byte). {@link GraphicsTexture} backs
     * itself with a {@code TYPE_INT_ARGB} BufferedImage whose DataBufferInt
     * is the very memory {@code upload()} reads, so writing straight into
     * that int[] is both correct and 8x cheaper than per-pixel calls.
     */
    public void uploadPixels(int textureHandle, int x, int y, int width, int height, byte[] rgba) {
        if (rgba == null) return;
        final GraphicsTexture tex = textures.get(textureHandle);
        if (tex == null) return;

        /* GraphicsTexture keeps its Graphics2D surface in a DataBufferInt. */
        if (!(tex.bufferedImage.getRaster().getDataBuffer() instanceof DataBufferInt)) {
            return;
        }
        final int[] dst = ((DataBufferInt) tex.bufferedImage.getRaster().getDataBuffer()).getData();
        final int imgW = tex.bufferedImage.getWidth();
        final int imgH = tex.bufferedImage.getHeight();

        int sx = x, sy = y, w = width, h = height;
        /* Defensive clamp: the bridge should never send out-of-range rects
           (gfx.hpp clamps mark_dirty), but a bad module must not corrupt the
           heap here. */
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

        /* GraphicsTexture.upload() reads the whole image and records a GL
           upload on the render call queue — cheap enough for a full-screen
           dirty rect, and it needs no extra GL plumbing. */
        tex.upload();
    }

    /* ------------------------------------------------------------------ */
    /* Lifetime                                                            */
    /* ------------------------------------------------------------------ */

    /** Drop every resource one script instance owned. */
    public synchronized void releaseAll(int[] handles) {
        if (handles == null) return;
        for (int handle : handles) {
            final ModelJS model = models.remove(handle);
            if (model != null) {
                modelTexture.remove(handle);
                try {
                    model.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    /** Drop every resource (script reload / world unload). */
    public synchronized void reset() {
        final List<ModelJS> toClose = new ArrayList<>(models.values());
        models.clear();
        modelTexture.clear();
        for (ModelJS model : toClose) {
            try {
                model.close();
            } catch (Throwable ignored) {
            }
        }
        final List<GraphicsTexture> texes = new ArrayList<>(textures.values());
        textures.clear();
        for (GraphicsTexture tex : texes) {
            try {
                tex.close();
            } catch (Throwable ignored) {
            }
        }
        nextHandle = 1;
    }

    /** Texture handle -> texture, for diagnostics. */
    public GraphicsTexture texture(int handle) {
        return textures.get(handle);
    }
}
