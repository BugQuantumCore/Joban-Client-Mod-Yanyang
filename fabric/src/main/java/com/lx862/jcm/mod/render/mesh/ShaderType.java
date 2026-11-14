package com.lx862.jcm.mod.render.mesh;

import org.mtr.resource.RenderStage;

/**
 * Material shader classification carried over from the legacy 4.0-era
 * OptimizedRenderer pipeline. The MTR 4.1 engine batches models by
 * {@link RenderStage}; this enum keeps the finer-grained script-facing
 * material names and maps each onto the closest 4.1 render stage.
 */
public enum ShaderType {

	CUTOUT,
	TRANSLUCENT,
	CUTOUT_BRIGHT,
	TRANSLUCENT_BRIGHT,
	CUTOUT_GLOWING,
	TRANSLUCENT_GLOWING;

	public final boolean isTranslucent() {
		return this == TRANSLUCENT || this == TRANSLUCENT_BRIGHT || this == TRANSLUCENT_GLOWING;
	}

	/**
	 * Closest legacy shader classification for a 4.1 render stage. Used when a
	 * script overrides a whole model's render type (RawModelJS#setAllRenderType).
	 */
	public static ShaderType fromRenderStage(RenderStage renderStage) {
		return switch (renderStage) {
			case ALWAYS_ON_LIGHT -> CUTOUT_GLOWING;
			case LIGHT, INTERIOR -> CUTOUT_BRIGHT;
			case INTERIOR_TRANSLUCENT -> TRANSLUCENT;
			default -> CUTOUT;
		};
	}

	public RenderStage toRenderStage() {
		return switch (this) {
			case TRANSLUCENT, TRANSLUCENT_BRIGHT, TRANSLUCENT_GLOWING -> RenderStage.INTERIOR_TRANSLUCENT;
			case CUTOUT_BRIGHT -> RenderStage.INTERIOR;
			case CUTOUT_GLOWING -> RenderStage.ALWAYS_ON_LIGHT;
			default -> RenderStage.EXTERIOR;
		};
	}
}
