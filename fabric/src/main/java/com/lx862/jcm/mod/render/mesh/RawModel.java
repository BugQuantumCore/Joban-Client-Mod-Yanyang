package com.lx862.jcm.mod.render.mesh;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.model.NewOptimizedModel;
import org.mtr.model.NewOptimizedModelGroup;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import com.lx862.jcm.mod.render.mesh.MaterialProperties;
import com.lx862.jcm.mod.render.mesh.RawMesh;
import com.lx862.jcm.mod.render.mesh.RawModel;

/**
 * Script-facing model: a set of meshes keyed by material. Mutations carry
 * over from the legacy 4.0-era pipeline; uploading produces the MTR 4.1
 * batched model map consumed by {@code MainRenderer.renderModel}.
 */
public final class RawModel {

	private final Map<MaterialProperties, RawMesh> materialPropertiesToRawMesh = new HashMap<>();

	/**
	 * Builds this model into an MTR 4.1 batched optimized-model map,
	 * keyed by render stage, ready for {@code MainRenderer.renderModel}.
	 */
	public Object2ObjectOpenHashMap<org.mtr.resource.RenderStage, ObjectArrayList<NewOptimizedModel>> upload() {
		final NewOptimizedModelGroup newOptimizedModelGroup = new NewOptimizedModelGroup();
		iterateRawMeshList(rawMesh -> {
			if (rawMesh.hasFaces()) {
				rawMesh.upload(newOptimizedModelGroup);
			}
		});
		return newOptimizedModelGroup.build(VertexFormat.Mode.TRIANGLES);
	}

	public void append(RawMesh nextMesh) {
		if (materialPropertiesToRawMesh.containsKey(nextMesh.materialProperties)) {
			materialPropertiesToRawMesh.get(nextMesh.materialProperties).append(nextMesh);
		} else {
			final RawMesh newMesh = new RawMesh(nextMesh.materialProperties);
			materialPropertiesToRawMesh.put(nextMesh.materialProperties, newMesh);
			newMesh.append(nextMesh);
		}
	}

	public RawMesh getRawMesh(MaterialProperties materialProperties) {
		return materialPropertiesToRawMesh.computeIfAbsent(materialProperties, RawMesh::new);
	}

	public void iterateRawMeshList(Consumer<RawMesh> consumer) {
		materialPropertiesToRawMesh.values().forEach(consumer);
	}

	public void append(Collection<RawMesh> nextMesh) {
		nextMesh.forEach(this::append);
	}

	public void append(RawModel nextModel) {
		append(nextModel.materialPropertiesToRawMesh.values());
	}

	public void applyMatrix(Matrix4f matrix4f) {
		iterateRawMeshList(rawMesh -> rawMesh.applyMatrix(matrix4f));
	}

	public void applyTranslation(float x, float y, float z) {
		iterateRawMeshList(rawMesh -> rawMesh.applyTranslation(x, y, z));
	}

	public void applyRotation(Vector3f axis, float angle) {
		iterateRawMeshList(rawMesh -> rawMesh.applyRotation(axis, angle));
	}

	public void applyScale(float x, float y, float z) {
		iterateRawMeshList(rawMesh -> rawMesh.applyScale(x, y, z));
	}

	public void applyMirror(boolean vx, boolean vy, boolean vz, boolean nx, boolean ny, boolean nz) {
		iterateRawMeshList(rawMesh -> rawMesh.applyMirror(vx, vy, vz, nx, ny, nz));
	}

	public void applyUVMirror(boolean u, boolean v) {
		iterateRawMeshList(rawMesh -> rawMesh.applyUVMirror(u, v));
	}

	public void generateNormals() {
		iterateRawMeshList(RawMesh::generateNormals);
	}

	public void distinct() {
		iterateRawMeshList(RawMesh::distinct);
	}

	public void triangulate() {
		iterateRawMeshList(RawMesh::triangulate);
	}

	public void applyShear(Vector3f dir, Vector3f shear, float ratio) {
		iterateRawMeshList(rawMesh -> rawMesh.applyShear(dir, shear, ratio));
	}
}
