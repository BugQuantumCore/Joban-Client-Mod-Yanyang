package com.lx862.jcm.mod.render.mesh;

import com.lx862.jcm.mod.util.JCMLogger;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.model.NewOptimizedModel;
import org.mtr.model.StoredVertexData;
import org.mtr.render.MainRenderer;
import org.mtr.render.QueuedRenderLayer;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.resource.RenderStage;

/**
 * Compatibility wrapper for the removed MTR 4.0
 * {@code org.mtr.resource.OptimizedModelWrapper}. In MTR 4.1 a loaded model is
 * the batched map produced by {@link NewOptimizedModelGroup#build}, rendered
 * through {@link MainRenderer#renderModel}.
 *
 * <h2>Buffered replay and the QUADS-mode root cause (2026-11-27)</h2>
 *
 * <p>Script models are TRIANGLES-mode streams (3 vertices per face, 6 with the
 * double-sided reverse copy — see {@link RawMesh#upload}). MTR's scheduled
 * render path, however, resolves every {@code QueuedRenderLayer} to a
 * QUADS-mode {@code RenderType} ({@code RenderType.entityCutout} &amp; co.
 * via {@code MoreRenderLayers}), and a {@code BufferBuilder} in QUADS mode
 * interprets its vertex stream in groups of four. A raw 3-vertices-per-face
 * triangle stream appended there is regrouped into 4-vertex chunks — the
 * first quad spans two triangles and everything after is sheared fragments,
 * which is the same corruption MTR-Yanyang 4.1.0-beta-2.yanyang.19's
 * multi-instance batching inflicted on its own OBJ rails (fixed upstream in
 * yanyang.20 by replaying OBJ meshes as degenerate quads and probing the
 * layer mode at runtime).</p>
 *
 * <p>That mode mismatch is the root cause of the "scripts load, replay,
 * queue — but nothing displays" reports on EVERY backend:</p>
 * <ul>
 *   <li><b>Plain OpenGL</b> (Sodium/Iris/vanilla): the buffered replay
 *   appended raw triangle streams into those QUADS-mode consumers — the
 *   buffered path itself was broken from the day it shipped;</li>
 *   <li><b>VulkanMod + Beryl</b>: the [wrvk] native recorder stays off under
 *   Beryl, so {@code MainRenderer.renderModel}'s GL drain draws the models —
 *   and yanyang.19's multi-instance batching (engaged from 3 instances — a
 *   6-car train queues every script model 6 times) replays the same triangle
 *   streams into the same QUADS-mode scheduled buffers.</li>
 * </ul>
 *
 * <p>The replay now emits each triangle as a <b>degenerate quad</b>
 * (v0, v1, v2, v2 — the zero-area tail triangle is discarded by the
 * rasterizer), mirroring MTR's own yanyang.20 fix, and the buffered path is
 * used on every backend except Radiance (whose captured drain tessellates
 * the model's own TRIANGLES mode and is proven correct). The vertices land
 * in the frame's {@code MultiBufferSource} exactly like every MTR-scheduled
 * quad and every vanilla entity — the path Sodium/Iris/ImmediatelyFast-style
 * stacks and VulkanMod's translated pipelines are built to handle. The GL
 * models are still built and closed exactly as before (dual-path). Pass
 * {@code -Djsblock.forceImmediateModelPath=true} to A/B test against the
 * {@code renderModel} path.</p>
 */
public final class OptimizedModelWrapper {

        public final Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models;

        /**
         * Retained per-(stage, texture) vertex lists from the upload — the
         * buffered replay's data source. {@code null} on wrappers built before
         * the retention existed (or by direct callers); those keep the legacy
         * {@link MainRenderer#renderModel} path on every backend.
         */
        private final Object2ObjectOpenHashMap<RenderStage, Object2ObjectOpenHashMap<ResourceLocation, ObjectArrayList<StoredVertexData>>> bufferedVertices;

        /** Latched decision for the session; the backend set cannot change while the client runs. */
        private static volatile Boolean cachedBufferedDecision;
        private static volatile boolean decisionLogged;

        /** Yanyang/diag (build.122+): one-shot breadcrumbs for the model draw path. */
        private static final java.util.Set<String> DIAG_PATH_LOGGED =
                        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        /** Yanyang/diag (build.122+): one-shot proof that the drain actually INVOKED a scheduled emission. */
        private static final java.util.Set<String> DIAG_DRAINED_LOGGED =
                        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

        public OptimizedModelWrapper(Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models) {
                this(models, null);
        }

        public OptimizedModelWrapper(
                        Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models,
                        Object2ObjectOpenHashMap<RenderStage, Object2ObjectOpenHashMap<ResourceLocation, ObjectArrayList<StoredVertexData>>> bufferedVertices) {
                this.models = models;
                this.bufferedVertices = bufferedVertices;
        }

        public Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> getModels() {
                return models;
        }

        /**
         * Queues this model for rendering this frame at the given transform and
         * light. Equivalent to the old
         * {@code CustomResourceLoader.OPTIMIZED_RENDERER_WRAPPER.queue}.
         */
        public void queue(StoredMatrixTransformations storedMatrixTransformations, int light) {
                if (useBufferedModelPath() && bufferedVertices != null && !bufferedVertices.isEmpty()) {
                        queueBuffered(storedMatrixTransformations, light);
                        return;
                }
                MainRenderer.renderModel(models, storedMatrixTransformations, light);
        }

        /**
         * @return true when script models should replay through the frame's
         * scheduled buffer source instead of {@code MainRenderer.renderModel}'s
         * VAO/captured drain. That is every backend EXCEPT Radiance: plain
         * OpenGL (the raw VAO draw fights Sodium/Iris-style stacks) and the
         * whole VulkanMod family — with Beryl active the [wrvk] recorder is
         * off and the GL drain's multi-instance batching (MTR yanyang.19+)
         * regroups TRIANGLES streams into QUADS-mode scheduled buffers, and
         * without Beryl the native recorder would additionally need every
         * script texture (including the dynamic GraphicsTexture canvases)
         * registered in MTR's native texture manager, which JCM does not do.
         * Radiance alone keeps the renderModel path: its captured drain
         * tessellates the model's own TRIANGLES mode, which is proven
         * correct. The decision latches per session — the installed backend
         * set cannot change while the client runs.
         */
        private static boolean useBufferedModelPath() {
                if (Boolean.getBoolean("jsblock.forceImmediateModelPath")) {
                        return false;
                }
                Boolean decision = cachedBufferedDecision;
                if (decision == null) {
                        decision = !org.mtr.client.VulkanCompat.isRadianceActive();
                        cachedBufferedDecision = decision;
                        if (!decisionLogged) {
                                decisionLogged = true;
                                JCMLogger.info("[JCM][diag] model draw path decision: {} (radiance={}, vulkanMod={})",
                                                decision ? "buffered (scheduled QUADS-safe replay)" : "immediate (renderModel)",
                                                org.mtr.client.VulkanCompat.isRadianceActive(),
                                                org.mtr.client.VulkanCompat.isVulkanModActive());
                        }
                }
                return decision;
        }

        /**
         * Buffered replay: one scheduled render per (stage, texture) bucket,
         * each emitting its retained vertices through the frame's
         * {@code VertexConsumer} against the caller's transform — the same
         * drain every MTR-scheduled quad takes (see MainRenderer's
         * CURRENT_RENDERS flush).
         *
         * <p><b>Primitive-mode contract:</b> the scheduled flush resolves the
         * bucket's {@link QueuedRenderLayer} to a QUADS-mode {@code RenderType}
         * (entityCutout &amp; co.), and a QUADS {@code BufferBuilder} groups its
         * stream in fours. The retained lists are TRIANGLES streams (3
         * vertices per face, 6 with the double-sided reverse copy), so each
         * triangle is emitted as a <b>degenerate quad</b> (v0, v1, v2, v2) —
         * the same replay MTR yanyang.20 uses for its own OBJ meshes. The
         * zero-area v0v2v2 tail triangle is discarded by the rasterizer, and
         * the geometry that lands in the buffer is bit-exact.</p>
         */
        private void queueBuffered(StoredMatrixTransformations storedMatrixTransformations, int light) {
                bufferedVertices.forEach((renderStage, textures) -> {
                        final QueuedRenderLayer queuedRenderLayer = renderStage.queuedRenderLayer;
                        final int bakedLight = renderStage.isFullBrightness ? LightTexture.FULL_BRIGHT : toPackedLight(light);
                        textures.forEach((texture, vertices) -> {
                                if (DIAG_PATH_LOGGED.add(renderStage + ":" + texture)) {
                                        JCMLogger.info("[JCM][diag] model draw path: buffered (degenerate-quad replay, doubleSided={}) — stage={}, texture={}, triangles={}({} vertices)",
                                                        com.lx862.jcm.mod.render.mesh.RawMesh.isDoubleSidedEmission(), renderStage, texture, vertices.size() / 3, vertices.size());
                                }
                                MainRenderer.scheduleRender(texture, false, queuedRenderLayer, (poseStack, vertexConsumer, offset) -> {
                                        if (DIAG_DRAINED_LOGGED.add(renderStage + ":" + texture)) {
                                                JCMLogger.info("[JCM][diag] model draw path: buffered DRAINED (degenerate-quad replay, doubleSided={}) — stage={}, texture={}, triangles={}({} vertices)",
                                                                com.lx862.jcm.mod.render.mesh.RawMesh.isDoubleSidedEmission(), renderStage, texture, vertices.size() / 3, vertices.size());
                                        }
                                        storedMatrixTransformations.transform(poseStack, offset);
                                        try {
                                                final com.mojang.blaze3d.vertex.PoseStack.Pose pose = poseStack.last();
                                                emitTriangleStreamAsDegenerateQuads(pose, vertexConsumer, vertices, bakedLight);
                                        } finally {
                                                poseStack.popPose();
                                        }
                                });
                        });
                });
        }

        /**
         * Emits a retained TRIANGLES stream into a QUADS-mode consumer as
         * degenerate quads. Public for the model-upload contract test
         * (different package): (v0, v1, v2, v2) per triangle — the
         * zero-area tail triangle is discarded by the rasterizer and the
         * visible geometry is exact.
         */
        public static void emitTriangleStreamAsDegenerateQuads(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, ObjectArrayList<StoredVertexData> vertices, int bakedLight) {
                for (int i = 0; i < vertices.size(); i++) {
                        final StoredVertexData vertex = vertices.get(i);
                        emitVertex(pose, vertexConsumer, vertex, bakedLight);
                        // Degenerate-quad completion: after the 3rd vertex of a
                        // triangle, repeat it as the 4th. The QUADS-mode builder
                        // groups in fours, so (v0, v1, v2, v2) reconstructs the
                        // exact triangle and discards the zero-area tail.
                        if (i % 3 == 2) {
                                emitVertex(pose, vertexConsumer, vertex, bakedLight);
                        }
                }
        }

        /** One vertex of the degenerate-quad replay: position/UV/colour/light/normal straight from the retained data. */
        private static void emitVertex(com.mojang.blaze3d.vertex.PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, StoredVertexData vertex, int bakedLight) {
                vertexConsumer
                                .addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                                .setColor(vertex.color().getRGB())
                                .setUv(vertex.u(), vertex.v())
                                .setOverlay(OverlayTexture.NO_OVERLAY)
                                .setLight(bakedLight)
                                .setNormal(pose, vertex.normalX(), vertex.normalY(), vertex.normalZ());
        }

        /**
         * Legacy brightness adaptation for the buffered path, replicating MTR's
         * (package-private) {@code CapturedModelLight.toPackedLight}: callers
         * pass either a raw 0-15 brightness or an already packed lightmap;
         * packed values are at least 16, so only 1-15 need expanding.
         */
        private static int toPackedLight(int brightnessOrPackedLight) {
                return brightnessOrPackedLight >= 0 && brightnessOrPackedLight <= 15
                                ? brightnessOrPackedLight << 4 | brightnessOrPackedLight << 20
                                : brightnessOrPackedLight;
        }

        /**
         * Releases the GL resources owned by this model's meshes. Idempotent per
         * mesh (see {@link NewOptimizedModel#close}). The retained vertex lists
         * need no teardown — they are plain Java data reclaimed with the
         * wrapper.
         */
        public void close() {
                models.values().forEach(list -> list.forEach(NewOptimizedModel::close));
        }
}
