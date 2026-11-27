package com.lx862.mtrscripting.core.util.model;

import com.lx862.jcm.mod.render.mesh.MaterialProperties;
import com.lx862.jcm.mod.render.mesh.OptimizedModelWrapper;
import com.lx862.jcm.mod.render.mesh.ShaderType;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.mtr.model.NewOptimizedModel;
import org.mtr.model.StoredVertexData;
import org.mtr.resource.RenderStage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model-upload boundary contract for the MTR 4.1 batched-model pipeline.
 *
 * <p>MTR 4.1 replaced the 4.0-era {@code VertexArray}/{@code GlStateTracker}
 * machinery with {@code NewOptimizedModelGroup}/{@code NewOptimizedModel} and
 * Minecraft's own {@code VertexBuffer}; the GPU upload itself is now
 * Minecraft code, which cannot run standalone. This test therefore pins the
 * JCM side of the boundary with the real MTR classes from the pinned jar:
 * it records the vertex stream MTR's own {@code StoredVertexData.apply}
 * emits through the stubbed Minecraft vertex consumer and asserts the
 * documented contract — triangulated face-indexed emission, each triangle
 * emitted forward and reversed (the double-sided script-model emission, so a
 * quad uploads twelve vertices, not four or six), legacy MTR-model-space
 * coordinates (the builder's y/z flip is the ONLY conversion — the upload
 * emits stored coordinates verbatim, exactly like {@code ObjModelLoader};
 * the 4.1 port's extra upload-time flip buried pack-OBJ LCD frames below
 * the car), full-bright baked light, render-stage bucketing and the
 * upload/re-upload/close lifecycle.</p>
 */
public final class ModelUploadTest {
        private static int checks;

        private static void check(boolean value, String reason) {
                if (!value) throw new AssertionError(reason);
                checks++;
        }

        private static RawModelJS quad() {
                final RawMeshBuilderJS b = new RawMeshBuilderJS(4, "interior", "test:quad");
                double[][] points = {{-.8,.6,0},{-.8,-.6,0},{.8,-.6,0},{.8,.6,0}};
                for (int i = 0; i < points.length; i++) {
                        b.vertex(points[i][0], points[i][1], points[i][2]).normal(0, 1, 0)
                                        .uv(i < 2 ? 0 : 1, i == 1 || i == 2 ? 1 : 0).endVertex();
                }
                check(b.getMesh().vertices.get(0).position.y == -.6f, "production builder flips the Y axis");
                check(b.getMesh().faces.size() == 2, "production builder triangulates the quad");
                return b.asRawModel();
        }

        private static void builderSemantics() {
                final RawMeshBuilderJS b = new RawMeshBuilderJS(4, "interior", "test:vector");
                b.vertex(new ScriptVector3f(1.25f, -2.5f, 3.75f)).endVertex();
                b.vertex(1.25, -2.5, 3.75).endVertex();
                final var vectorPosition = b.getMesh().vertices.get(0).position;
                final var scalarPosition = b.getMesh().vertices.get(1).position;
                check(vectorPosition.x == 1.25f && vectorPosition.y == 2.5f
                                                && vectorPosition.z == -3.75f,
                                "vector overload retains distinct axes and flips Y/Z");
                check(vectorPosition.x == scalarPosition.x
                                                && vectorPosition.y == scalarPosition.y
                                                && vectorPosition.z == scalarPosition.z,
                                "vector and scalar vertex overloads agree");
                final RawMeshBuilderJS tri = new RawMeshBuilderJS(3, "exterior", "test:tri");
                tri.vertex(0, 0, 0).vertex(1, 0, 0).vertex(0, 1, 0);
                check(tri.getMesh().faces.isEmpty(), "faces only close at endVertex");
        }

        private static List<NewOptimizedModel> stageBucket(OptimizedModelWrapper wrapper, RenderStage stage) {
                final var models = wrapper.getModels().get(stage);
                check(models != null, "upload buckets under " + stage);
                return models;
        }

        private static void uploadContract() {
                // The stubbed Minecraft VertexBuffer captured the stream MTR's real
                // StoredVertexData.apply emitted through the real NewOptimizedModel
                // constructor's upload callback. Buffers correlate to models by
                // creation order (one buffer per bucket, deterministic list order).
                final int base = VertexBuffer.ALL.size();
                final ModelJS model = ModelJS.uploadRawModel(quad());
                final List<NewOptimizedModel> interior = stageBucket(model.getUnderlyingModel(), RenderStage.INTERIOR);
                check(interior.size() == 1, "one batched model for the quad's stage/texture bucket");
                final NewOptimizedModel uploaded = interior.get(0);
                check(uploaded.getNativeModel() == null, "headless upload selects the GL path (no native Vulkan mesh)");
                check(!uploaded.usesCapturedRendering(), "headless upload builds the vertex buffer directly");
                check(VertexBuffer.ALL.size() - base == 1, "exactly one GPU buffer per (stage, texture) bucket");
                final VertexBuffer buffer = VertexBuffer.ALL.get(base);
                check(buffer.uploads.size() == 1, "the buffer receives one mesh");
                check(buffer.uploads.get(0).mode == com.mojang.blaze3d.vertex.VertexFormat.Mode.TRIANGLES, "models build as TRIANGLES");
                final List<BufferBuilder.Vertex> stream = buffer.uploads.get(0).vertices;
                check(stream.size() == 12, "a quad uploads two triangles, each emitted forward + reversed (double-sided)");
                // Stored-space (builder-flipped) coordinates; the upload passes them
                // through verbatim. Script quad points {(-.8,.6),(-.8,-.6),(.8,-.6),(.8,.6)}
                // are stored with y negated by RawMeshBuilderJS — that is the whole
                // conversion. Face.triangulate([0,1,2,3]) = [0,1,2],[0,2,3]; the reversed
                // copies walk the same indices backwards (0,2,1 / 3,2,0).
                final float[][] expectedPositions = {
                                {-.8f,-.6f,0},{-.8f,.6f,0},{.8f,.6f,0},
                                {.8f,.6f,0},{-.8f,.6f,0},{-.8f,-.6f,0},
                                {-.8f,-.6f,0},{.8f,.6f,0},{.8f,-.6f,0},
                                {.8f,-.6f,0},{.8f,.6f,0},{-.8f,-.6f,0}};
                final float[][] expectedUv = {
                                {0,0},{0,1},{1,1},
                                {1,1},{0,1},{0,0},
                                {0,0},{1,1},{1,0},
                                {1,0},{1,1},{0,0}};
                // Builder normal (0,1,0) is stored as (0,-1,0); the reversed copies
                // negate it again for backface lighting.
                final float[][] expectedNormals = {
                                {0,-1,0},{0,-1,0},{0,-1,0},
                                {0,1,0},{0,1,0},{0,1,0},
                                {0,-1,0},{0,-1,0},{0,-1,0},
                                {0,1,0},{0,1,0},{0,1,0}};
                for (int i = 0; i < 12; i++) {
                        final BufferBuilder.Vertex v = stream.get(i);
                        check(v.x == expectedPositions[i][0] && v.y == expectedPositions[i][1] && v.z == expectedPositions[i][2],
                                        "vertex " + i + " keeps stored (builder-flipped) coordinates — the upload does not flip again");
                        check(v.u == expectedUv[i][0] && v.v == expectedUv[i][1], "vertex " + i + " uv passthrough");
                        check(v.color == 0xFFFFFFFF, "vertex " + i + " falls back to the material color");
                        check(v.nx == expectedNormals[i][0] && v.ny == expectedNormals[i][1] && v.nz == expectedNormals[i][2],
                                        "vertex " + i + " normal passthrough (negated on reversed copies)");
                        check(v.overlay == net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                                        "vertex " + i + " overlay passthrough");
                        check(v.light == 0xF000F0, "vertex " + i + " baked full-bright lightmap");
                }
                model.close();
                check(buffer.closeCount == 1, "close frees the model's buffer exactly once");
                model.close();
                check(buffer.closeCount == 1, "close is idempotent (live-model registry)");
        }

        private static void stageMapping() {
                check(ShaderType.fromRenderStage(RenderStage.ALWAYS_ON_LIGHT) == ShaderType.CUTOUT_GLOWING, "ALWAYS_ON_LIGHT maps to CUTOUT_GLOWING");
                check(ShaderType.fromRenderStage(RenderStage.LIGHT) == ShaderType.CUTOUT_BRIGHT, "LIGHT maps to CUTOUT_BRIGHT");
                check(ShaderType.fromRenderStage(RenderStage.INTERIOR) == ShaderType.CUTOUT_BRIGHT, "INTERIOR maps to CUTOUT_BRIGHT");
                check(ShaderType.fromRenderStage(RenderStage.INTERIOR_TRANSLUCENT) == ShaderType.TRANSLUCENT, "INTERIOR_TRANSLUCENT maps to TRANSLUCENT");
                check(ShaderType.fromRenderStage(RenderStage.EXTERIOR) == ShaderType.CUTOUT, "EXTERIOR maps to CUTOUT");
                for (final ShaderType type : ShaderType.values()) {
                        check(type.toRenderStage() != null, type + " maps onto a 4.1 render stage");
                }
                // Script render-type strings land in the right stage bucket end to end.
                final RawMeshBuilderJS translucent = new RawMeshBuilderJS(4, "interior_translucent", "test:tl");
                translucent.vertex(0, 0, 0).vertex(1, 0, 0).vertex(1, 1, 0).vertex(0, 1, 0);
                translucent.endVertex().endVertex().endVertex().endVertex();
                final ModelJS translucentModel = ModelJS.uploadRawModel(translucent.asRawModel());
                stageBucket(translucentModel.getUnderlyingModel(), RenderStage.INTERIOR_TRANSLUCENT);
                translucentModel.close();
                final RawMeshBuilderJS legacyAlias = new RawMeshBuilderJS(4, "exteriortranslucent", "test:xtl");
                legacyAlias.vertex(0, 0, 0).vertex(1, 0, 0).vertex(1, 1, 0).vertex(0, 1, 0);
                legacyAlias.endVertex().endVertex().endVertex().endVertex();
                final ModelJS aliasModel = ModelJS.uploadRawModel(legacyAlias.asRawModel());
                stageBucket(aliasModel.getUnderlyingModel(), RenderStage.INTERIOR_TRANSLUCENT);
                aliasModel.close();
        }

        private static void textureReplaceLifecycle() {
                final RawModelJS twoTextures = new RawModelJS();
                twoTextures.append(quadOf("test:a"));
                twoTextures.append(quadOf("test:b"));
                final int base = VertexBuffer.ALL.size();
                final ModelJS model = ModelJS.uploadRawModel(twoTextures);
                final var before = model.getUnderlyingModel().getModels();
                check(before.get(RenderStage.INTERIOR).size() == 2, "two texture buckets before the replace");
                check(VertexBuffer.ALL.size() - base == 2, "two buffers for two buckets");
                final List<VertexBuffer> previousBuffers = List.of(VertexBuffer.ALL.get(base), VertexBuffer.ALL.get(base + 1));

                model.replaceAllTexture("test:c");
                final var after = model.getUnderlyingModel().getModels();
                // After the replace both meshes share one material, and the 4.1
                // batched pipeline MERGES same-material meshes into a single bucket
                // (one model, one buffer, both quads' vertices concatenated).
                check(after.get(RenderStage.INTERIOR).size() == 1, "re-upload merges the now-identical materials into one bucket");
                check(String.valueOf(after.get(RenderStage.INTERIOR).get(0).texture).endsWith("test:c"), "replaced model draws the new texture");
                check(VertexBuffer.ALL.size() - base == 3, "re-upload creates exactly one merged buffer");
                final VertexBuffer merged = VertexBuffer.ALL.get(base + 2);
                check(merged.uploads.size() == 1 && merged.uploads.get(0).vertices.size() == 24,
                                "the merged bucket carries both quads' twelve-vertex (double-sided) streams");
                for (final VertexBuffer previous : previousBuffers) {
                        check(previous.closeCount == 1, "re-upload closes the previous generation exactly once");
                }

                // copyForMaterialChanges goes through the mixin-backed field rebind
                // (RawMeshAccessor); the copy shares vertex data but owns materials.
                final ModelJS copy = model.copyForMaterialChanges();
                final int copyBase = VertexBuffer.ALL.size();
                copy.replaceAllTexture("test:d");
                check(VertexBuffer.ALL.size() - copyBase == 1, "copy re-uploads into one merged bucket");
                check(String.valueOf(copy.getUnderlyingModel().getModels().get(RenderStage.INTERIOR).get(0).texture).endsWith("test:d"),
                                "copy rebinds materials");
                model.close();
                copy.close();
        }

        private static RawModelJS quadOf(String texture) {
                final RawMeshBuilderJS b = new RawMeshBuilderJS(4, "interior", texture);
                b.vertex(-.5, .5, 0).uv(0, 0).endVertex();
                b.vertex(-.5, -.5, 0).uv(0, 1).endVertex();
                b.vertex(.5, -.5, 0).uv(1, 1).endVertex();
                b.vertex(.5, .5, 0).uv(1, 0).endVertex();
                return b.asRawModel();
        }

        private static void miscellany() {
                check(RawModelJS.stringToRenderStage("exteriortranslucent") == RenderStage.INTERIOR_TRANSLUCENT,
                                "legacy render-type aliases still normalise");
                check(RawModelJS.stringToRenderStage("lightTranslucent".toLowerCase(Locale.ROOT)) == RenderStage.ALWAYS_ON_LIGHT,
                                "light_translucent alias still normalises");
                final MaterialProperties cutout = new MaterialProperties(ShaderType.CUTOUT, new net.minecraft.resources.ResourceLocation("test:x"), null);
                check(cutout.color == null && new MaterialProperties(ShaderType.CUTOUT, cutout.getTexture(), 0x12345678).color == 0x12345678,
                                "material color is optional and preserved");
        }

        /** Records the position and baked light of every addVertex the replay emits. */
        private static final class RecordingConsumer implements VertexConsumer {
                final List<float[]> positions = new ArrayList<>();
                final List<Integer> lights = new ArrayList<>();
                @Override public VertexConsumer addVertex(float x, float y, float z) { positions.add(new float[]{x, y, z}); return this; }
                @Override public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) { positions.add(new float[]{x, y, z}); return this; }
                @Override public VertexConsumer setLight(int packedLight) { lights.add(packedLight); return this; }
                @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
                @Override public VertexConsumer setColor(int argb) { return this; }
                @Override public VertexConsumer setUv(float u, float v) { return this; }
                @Override public VertexConsumer setUv1(int u, int v) { return this; }
                @Override public VertexConsumer setUv2(int u, int v) { return this; }
                @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }
                @Override public VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) { return this; }
                @Override public VertexConsumer setOverlay(int packedOverlay) { return this; }
        }

        private static void queueReplayContract() {
                // The buffered (scheduled) replay feeds MTR's QUADS-mode render
                // layers (entityCutout & co. via MoreRenderLayers), whose
                // BufferBuilder groups vertices in fours. A raw TRIANGLES stream
                // (3 vertices per face, 6 double-sided) would be regrouped into
                // 4-vertex chunks and mangled — the root cause of the
                // "scripts load, replay, queue but nothing displays" reports.
                // The replay must emit each triangle as a degenerate quad
                // (v0, v1, v2, v2); see OptimizedModelWrapper's javadoc.
                final var uploaded = quad().rawModel.uploadWithVertexData();
                final var interior = uploaded.bufferedVertices().get(RenderStage.INTERIOR);
                check(interior != null && interior.size() == 1, "one retained (stage, texture) bucket");
                final List<StoredVertexData> vertices = interior.values().iterator().next();
                final int triangles = vertices.size() / 3;
                check(vertices.size() % 3 == 0 && triangles == 4, "retained stream: 4 triangles (2 faces x double-sided)");
                final RecordingConsumer consumer = new RecordingConsumer();
                OptimizedModelWrapper.emitTriangleStreamAsDegenerateQuads(null, consumer, (org.mtr.libraries.it.unimi.dsi.fastutil.objects.ObjectArrayList<StoredVertexData>) vertices, 0xF000F0);
                final List<float[]> emitted = consumer.positions;
                check(emitted.size() == triangles * 4, "degenerate-quad replay emits exactly 4 vertices per triangle");
                for (int t = 0; t < triangles; t++) {
                        final float[] v0 = emitted.get(t * 4), v1 = emitted.get(t * 4 + 1), v2 = emitted.get(t * 4 + 2), v3 = emitted.get(t * 4 + 3);
                        final StoredVertexData s0 = vertices.get(t * 3), s1 = vertices.get(t * 3 + 1), s2 = vertices.get(t * 3 + 2);
                        check(v0[0] == s0.x() && v0[1] == s0.y() && v0[2] == s0.z()
                                        && v1[0] == s1.x() && v1[1] == s1.y() && v1[2] == s1.z()
                                        && v2[0] == s2.x() && v2[1] == s2.y() && v2[2] == s2.z(),
                                        "triangle " + t + ": the first 3 vertices are the retained triangle verbatim");
                        check(v3[0] == v2[0] && v3[1] == v2[1] && v3[2] == v2[2],
                                        "triangle " + t + ": the 4th vertex duplicates the 3rd (degenerate tail)");
                }
                check(consumer.lights.size() == emitted.size() && consumer.lights.stream().allMatch(l -> l == 0xF000F0),
                                "the baked light constant reaches every vertex");
        }

        public static void main(String[] args) {
                builderSemantics();
                uploadContract();
                stageMapping();
                textureReplaceLifecycle();
                queueReplayContract();
                miscellany();
                System.out.println("MTR 4.1 MODEL UPLOAD CONTRACT OK (" + checks + " checks)");
        }
}
