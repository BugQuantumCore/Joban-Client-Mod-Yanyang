package com.lx862.jcm.mod.render.mesh;

import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.model.NewOptimizedModel;
import org.mtr.render.MainRenderer;
import org.mtr.render.StoredMatrixTransformations;
import org.mtr.resource.RenderStage;

/**
 * Compatibility wrapper for the removed MTR 4.0
 * {@code org.mtr.resource.OptimizedModelWrapper}. In MTR 4.1 a loaded model is
 * the batched map produced by {@link NewOptimizedModelGroup#build}, rendered
 * through {@link MainRenderer#renderModel}.
 */
public final class OptimizedModelWrapper {

	public final Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models;

	public OptimizedModelWrapper(Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models) {
		this.models = models;
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
		MainRenderer.renderModel(models, storedMatrixTransformations, light);
	}

	/**
	 * Releases the GL resources owned by this model's meshes. Idempotent per
	 * mesh (see {@link NewOptimizedModel#close}).
	 */
	public void close() {
		models.values().forEach(list -> list.forEach(NewOptimizedModel::close));
	}
}
