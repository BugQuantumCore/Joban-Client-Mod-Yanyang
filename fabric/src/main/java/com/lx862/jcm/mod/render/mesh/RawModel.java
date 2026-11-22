package com.lx862.jcm.mod.render.mesh;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.mtr.model.NewOptimizedModel;
import org.mtr.model.NewOptimizedModelGroup;
import org.mtr.model.StoredVertexData;
import org.mtr.resource.RenderStage;

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
         * A completed upload: the GPU-resident batched model map plus the raw
         * per-(stage, texture) vertex lists that produced it.
         *
         * <p>The retained vertices exist so {@link OptimizedModelWrapper} can
         * replay the model through Minecraft's own buffer source on the plain
         * OpenGL path (see its javadoc) — the GL {@link NewOptimizedModel}
         * keeps its vertices inside an opaque VBO and offers no way to
         * re-emit them into a {@code VertexConsumer}.</p>
         */
        public record UploadedModel(
                        Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> models,
                        Object2ObjectOpenHashMap<RenderStage, Object2ObjectOpenHashMap<ResourceLocation, ObjectArrayList<StoredVertexData>>> bufferedVertices) {
        }

        /**
         * Builds this model into an MTR 4.1 batched optimized-model map,
         * keyed by render stage, ready for {@code MainRenderer.renderModel}.
         */
        public Object2ObjectOpenHashMap<RenderStage, ObjectArrayList<NewOptimizedModel>> upload() {
                return uploadWithVertexData().models();
        }

        /**
         * Same as {@link #upload()} but also returns the per-(stage, texture)
         * vertex lists, for wrappers that need a buffered fallback path.
         */
        public UploadedModel uploadWithVertexData() {
                final NewOptimizedModelGroup newOptimizedModelGroup = new NewOptimizedModelGroup();
                final Object2ObjectOpenHashMap<RenderStage, Object2ObjectOpenHashMap<ResourceLocation, ObjectArrayList<StoredVertexData>>> bufferedVertices =
                                new Object2ObjectOpenHashMap<>();
                iterateRawMeshList(rawMesh -> {
                        if (rawMesh.hasFaces()) {
                                rawMesh.upload(newOptimizedModelGroup, bufferedVertices);
                        }
                });
                return new UploadedModel(newOptimizedModelGroup.build(VertexFormat.Mode.TRIANGLES), bufferedVertices);
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
