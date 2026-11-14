package com.lx862.mtrscripting.core.util.model;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.mod.util.JSUtil;
import com.lx862.jcm.mod.render.mesh.OptimizedModelWrapper;
import net.minecraft.resources.ResourceLocation;
import org.mtr.render.StoredMatrixTransformations;

/**
 * An uploaded, GPU-resident model built from a {@link RawModelJS}. Ported to
 * the MTR 4.1 batched-model pipeline: uploading goes through
 * {@link RawModel#upload()} (NewOptimizedModelGroup), rendering through
 * {@link OptimizedModelWrapper#queue} (MainRenderer.renderModel).
 *
 * <p>Because 4.1's {@code NewOptimizedModel} texture is immutable, the
 * {@code replaceTexture} family re-uploads the (mutated) raw model.</p>
 */
public class ModelJS {

        private final RawModelJS sourceModel;
        private OptimizedModelWrapper model;

        private ModelJS(RawModelJS sourceModel, OptimizedModelWrapper model) {
                this.sourceModel = sourceModel;
                this.model = model;
        }

        @ApiInternal
        public static ModelJS uploadRawModel(RawModelJS rawModelJS) {
                return new ModelJS(rawModelJS, new OptimizedModelWrapper(rawModelJS.rawModel.upload()));
        }

        public ModelJS copyForMaterialChanges() {
                final RawModelJS copy = sourceModel.copyForMaterialChanges();
                return new ModelJS(copy, new OptimizedModelWrapper(copy.rawModel.upload()));
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
                model.queue(storedMatrixTransformations, light);
        }

        @ApiInternal
        public OptimizedModelWrapper getUnderlyingModel() {
                return this.model;
        }

        /** Re-uploads the (mutated) source model into fresh GPU meshes, closing the previous ones. */
        private void reupload() {
                final OptimizedModelWrapper previous = this.model;
                this.model = new OptimizedModelWrapper(sourceModel.rawModel.upload());
                previous.close();
        }

        public void close() {
                model.close();
        }
}
