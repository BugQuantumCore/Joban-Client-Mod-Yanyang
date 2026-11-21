package com.lx862.mtrscripting.core.util.model;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.util.JSUtil;
import com.lx862.jcm.mod.util.JCMLogger;
import com.lx862.jcm.mod.render.mesh.OptimizedModelWrapper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.render.StoredMatrixTransformations;

/**
 * An uploaded, GPU-resident model built from a {@link RawModelJS}. Ported to
 * the MTR 4.1 batched-model pipeline: uploading goes through
 * {@link RawModel#upload()} (NewOptimizedModelGroup), rendering through
 * {@link OptimizedModelWrapper#queue} (MainRenderer.renderModel).
 *
 * <p>Because 4.1's {@code NewOptimizedModel} texture is immutable, the
 * {@code replaceTexture} family re-uploads the (mutated) raw model.</p>
 *
 * <p><b>Threading:</b> the geometry copy and material edits are pure data and safe on
 * any thread, but the GPU upload itself is render-thread confined
 * ({@code RenderSystem.assertOnRenderThread} inside the buffer build). Vehicle
 * scripts run their per-instance {@code create()} on the scripting executor
 * thread, and the stock {@code display_helper.js} calls
 * {@code copyForMaterialChanges()} + {@code replaceAllTexture()} there — so every
 * off-render-thread upload is deferred via {@link RenderSystem#recordRenderCall}
 * instead of throwing {@code Rendersystem called from wrong thread} (the error that
 * killed every JS vehicle script on 2026-11-21). A deferred model draws nothing
 * until the upload lands on the next render tick; {@link #close()} before that
 * point cancels the pending upload.</p>
 */
public class ModelJS {

        private final RawModelJS sourceModel;
        private volatile OptimizedModelWrapper model;
        private volatile boolean closed;

        /* Yanyang/diag (build for the 2026-11-21 OpenGL "scripts load but
           nothing displays" report): a handful of one-shot breadcrumbs that
           make the JS model pipeline's state visible in latest.log —
           whether the deferred upload ever lands, and whether draw() ever
           sees a non-empty wrapper. Log-only, no behaviour change. */
        private static final java.util.Set<String> DIAG_SEEN =
                        java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        private static final java.util.concurrent.atomic.AtomicInteger DIAG_SWAPS = new java.util.concurrent.atomic.AtomicInteger();

        private static String jcm$describeModels(final OptimizedModelWrapper wrapper) {
                if (wrapper == null || wrapper.models == null || wrapper.models.isEmpty()) {
                        return "EMPTY";
                }
                final StringBuilder sb = new StringBuilder();
                wrapper.models.forEach((stage, list) -> sb.append(stage).append('=').append(list.size()).append(' '));
                return sb.toString().trim();
        }

        private ModelJS(RawModelJS sourceModel, OptimizedModelWrapper model) {
                this.sourceModel = sourceModel;
                this.model = model;
        }

        @ApiInternal
        public static ModelJS uploadRawModel(RawModelJS rawModelJS) {
                return new ModelJS(rawModelJS, new OptimizedModelWrapper(rawModelJS.rawModel.upload()));
        }

        /**
         * Copies this model's geometry with independently mutable materials. Thread-safe:
         * on the render thread the copy uploads immediately; on a scripting executor
         * thread the GPU upload is deferred (see the class javadoc).
         */
        public ModelJS copyForMaterialChanges() {
                final RawModelJS copy = sourceModel.copyForMaterialChanges();
                if (RenderSystem.isOnRenderThread()) {
                        return new ModelJS(copy, new OptimizedModelWrapper(copy.rawModel.upload()));
                }
                final ModelJS modelJS = new ModelJS(copy, new OptimizedModelWrapper(new Object2ObjectOpenHashMap<>()));
                RenderSystem.recordRenderCall(modelJS::swapInUploaded);
                return modelJS;
        }

        public void replaceTexture(String oldTexture, String newTexture) {
                replaceTexture(oldTexture, JSUtil.parseId(newTexture));
        }

        public void replaceTexture(String oldTexture, ResourceLocation newTexture) {
                sourceModel.replaceTexture(oldTexture, newTexture);
                reupload();
        }

        public void replaceAllTexture(String id) {
                replaceAllTexture(JSUtil.parseId(id));
        }

        public void replaceAllTexture(ResourceLocation id) {
                sourceModel.replaceAllTexture(id);
                reupload();
        }

        @ApiInternal
        public void draw(StoredMatrixTransformations storedMatrixTransformations, int light) {
                if (DIAG_SEEN.add("draw")) {
                        JCMLogger.info("[JCM][diag] first ModelJS.draw: stages=[{}] closed={}", jcm$describeModels(model), closed);
                }
                model.queue(storedMatrixTransformations, light);
        }

        /**
         * The current GPU wrapper. Note: on a model whose upload was deferred (created
         * off the render thread), this is an empty wrapper until the deferred upload
         * lands, and {@code replaceTexture} family swaps it afterwards — prefer
         * {@link #draw} unless you cache wrappers at a point where the model is known
         * to be uploaded (e.g. ComplexModelStorage's reload-time snapshot).
         */
        @ApiInternal
        public OptimizedModelWrapper getUnderlyingModel() {
                return this.model;
        }

        /** Re-uploads the (mutated) source model into fresh GPU meshes, closing the previous ones. Thread-safe: off the render thread the swap is deferred to the next render tick. */
        private void reupload() {
                if (RenderSystem.isOnRenderThread()) {
                        swapInUploaded();
                } else {
                        RenderSystem.recordRenderCall(this::swapInUploaded);
                }
        }

        /** Render-thread side of every upload: build fresh meshes and retire the previous wrapper. Runs after {@link #close()} as a no-op. */
        private void swapInUploaded() {
                if (closed) {
                        return;
                }
                final OptimizedModelWrapper previous = this.model;
                this.model = new OptimizedModelWrapper(sourceModel.rawModel.upload());
                final int swapIndex = DIAG_SWAPS.incrementAndGet();
                if (swapIndex <= 3) {
                        JCMLogger.info("[JCM][diag] ModelJS upload #{} landed: stages=[{}] (was: [{}])",
                                        swapIndex, jcm$describeModels(this.model), jcm$describeModels(previous));
                }
                if (previous != null) {
                        previous.close();
                }
        }

        public void close() {
                // Mark first so a still-pending deferred upload (recordRenderCall) becomes a
                // no-op instead of resurrecting meshes after close; the GL release itself is
                // deferred the same way when close() is invoked from a scripting thread.
                closed = true;
                final OptimizedModelWrapper current = this.model;
                if (RenderSystem.isOnRenderThread()) {
                        if (current != null) {
                                current.close();
                        }
                } else if (current != null) {
                        RenderSystem.recordRenderCall(current::close);
                }
        }
}
