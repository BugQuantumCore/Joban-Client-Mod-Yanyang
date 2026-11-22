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
 * <h2>Buffered fallback for the plain OpenGL path (2.3.0-1.0.2)</h2>
 *
 * <p>{@code MainRenderer.renderModel} drains GL-path models with a raw
 * {@code VertexBuffer#drawWithShader} inside {@code RenderType#
 * setupRenderState()/clearRenderState()} brackets. That immediate VAO draw is
 * the one and only render mechanism every script draw funnels through (JS
 * {@code ModelDrawCall}, native {@code NativeModelDrawCall}, dynamic models
 * and JCM complex block models all reach {@link #queue}), and it is the only
 * stage the 2026-11-21/22 "scripts load, replay, queue — but nothing
 * displays" OpenGL reports never proved wrong: under Radiance the exact same
 * models render fine, and Radiance draws them through
 * {@code NewOptimizedModel#renderCaptured} — i.e. through Minecraft's own
 * buffer source, not through a raw VAO draw.</p>
 *
 * <p>So on plain OpenGL (no VulkanMod native pipeline, no Radiance captured
 * pipeline) this wrapper now replays the retained per-(stage, texture) vertex
 * lists through {@link MainRenderer#scheduleRender} instead: the vertices are
 * appended into the frame's {@code MultiBufferSource} exactly like every
 * MTR-scheduled quad and every vanilla entity, which is the path
 * Sodium/Iris/ImmediatelyFast-style rendering stacks are built to handle. The
 * Vulkan-family backends keep {@code MainRenderer.renderModel} untouched —
 * their proven native/captured drains own the frame there.</p>
 *
 * <p>The GL models are still built and closed exactly as before (dual-path);
 * only the replay destination changes on plain GL. Pass
 * {@code -Djsblock.forceImmediateModelPath=true} to A/B test against the old
 * behaviour.</p>
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
         * buffer source instead of {@code MainRenderer.renderModel}'s raw VAO
         * drain — i.e. plain OpenGL, where no Vulkan-family backend owns the
         * frame.
         */
        private static boolean useBufferedModelPath() {
                if (Boolean.getBoolean("jsblock.forceImmediateModelPath")) {
                        return false;
                }
                Boolean decision = cachedBufferedDecision;
                if (decision == null) {
                        decision = !org.mtr.client.VulkanCompat.isVulkanModActive()
                                        && !org.mtr.client.VulkanCompat.isRadianceActive();
                        cachedBufferedDecision = decision;
                }
                return decision;
        }

        /**
         * Buffered replay: one scheduled render per (stage, texture) bucket,
         * each emitting its retained vertices through the frame's
         * {@code VertexConsumer} against the caller's transform — the same
         * drain every MTR-scheduled quad takes (see MainRenderer's
         * CURRENT_RENDERS flush).
         */
        private void queueBuffered(StoredMatrixTransformations storedMatrixTransformations, int light) {
                bufferedVertices.forEach((renderStage, textures) -> {
                        final QueuedRenderLayer queuedRenderLayer = renderStage.queuedRenderLayer;
                        final int bakedLight = renderStage.isFullBrightness ? LightTexture.FULL_BRIGHT : toPackedLight(light);
                        textures.forEach((texture, vertices) -> {
                                if (DIAG_PATH_LOGGED.add(renderStage + ":" + texture)) {
                                        JCMLogger.info("[JCM][diag] model draw path: buffered (plain GL, doubleSided={}) — stage={}, texture={}, vertices={}",
                                                        com.lx862.jcm.mod.render.mesh.RawMesh.isDoubleSidedEmission(), renderStage, texture, vertices.size());
                                }
                                MainRenderer.scheduleRender(texture, false, queuedRenderLayer, (poseStack, vertexConsumer, offset) -> {
                                        if (DIAG_DRAINED_LOGGED.add(renderStage + ":" + texture)) {
                                                JCMLogger.info("[JCM][diag] model draw path: buffered DRAINED (plain GL, doubleSided={}) — stage={}, texture={}, vertices={}",
                                                                com.lx862.jcm.mod.render.mesh.RawMesh.isDoubleSidedEmission(), renderStage, texture, vertices.size());
                                        }
                                        storedMatrixTransformations.transform(poseStack, offset);
                                        try {
                                                final com.mojang.blaze3d.vertex.PoseStack.Pose pose = poseStack.last();
                                                for (int i = 0; i < vertices.size(); i++) {
                                                        final StoredVertexData vertex = vertices.get(i);
                                                        vertexConsumer
                                                                        .addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                                                                        .setColor(vertex.color().getRGB())
                                                                        .setUv(vertex.u(), vertex.v())
                                                                        .setOverlay(OverlayTexture.NO_OVERLAY)
                                                                        .setLight(bakedLight)
                                                                        .setNormal(pose, vertex.normalX(), vertex.normalY(), vertex.normalZ());
                                                }
                                        } finally {
                                                poseStack.popPose();
                                        }
                                });
                        });
                });
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
