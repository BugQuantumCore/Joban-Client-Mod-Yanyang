package com.lx862.mtrscripting.core.util.model;

import com.lx862.jcm.mod.render.mesh.MaterialProperties;
import com.lx862.jcm.mod.render.mesh.OptimizedModelWrapper;
import com.lx862.jcm.mod.render.mesh.ShaderType;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexBuffer;
import org.mtr.model.NewOptimizedModel;
import org.mtr.resource.RenderStage;

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
 * documented contract — triangulated face-indexed emission (a quad uploads
 * six vertices, not four), verbatim script-space coordinates (the builder's
 * y/z flip and the upload's legacy-space flip cancel, matching
 * {@code ObjModelLoader}'s verbatim stored space), full-bright baked light,
 * render-stage bucketing and the upload/re-upload/close lifecycle.</p>
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
                check(stream.size() == 6, "a quad uploads six triangulated vertices, not four");
                final float[][] expectedPositions = {{-.8f,.6f,0},{-.8f,-.6f,0},{.8f,-.6f,0},{-.8f,.6f,0},{.8f,-.6f,0},{.8f,.6f,0}};
                final float[][] expectedUv = {{0,0},{0,1},{1,1},{0,0},{1,1},{1,0}};
                for (int i = 0; i < 6; i++) {
                        final BufferBuilder.Vertex v = stream.get(i);
                        check(v.x == expectedPositions[i][0] && v.y == expectedPositions[i][1] && v.z == expectedPositions[i][2],
                                        "vertex " + i + " keeps script-space coordinates (builder flip + legacy-space flip cancel)");
                        check(v.u == expectedUv[i][0] && v.v == expectedUv[i][1], "vertex " + i + " uv passthrough");
                        check(v.color == 0xFFFFFFFF, "vertex " + i + " falls back to the material color");
                        check(v.nx == 0 && v.ny == 1 && v.nz == 0, "vertex " + i + " normal passthrough");
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
                check(merged.uploads.size() == 1 && merged.uploads.get(0).vertices.size() == 12,
                                "the merged bucket carries both quads' six-vertex streams");
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

        public static void main(String[] args) {
                builderSemantics();
                uploadContract();
                stageMapping();
                textureReplaceLifecycle();
                miscellany();
                System.out.println("MTR 4.1 MODEL UPLOAD CONTRACT OK (" + checks + " checks)");
        }
}
