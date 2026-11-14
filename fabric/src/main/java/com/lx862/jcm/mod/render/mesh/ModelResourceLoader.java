package com.lx862.jcm.mod.render.mesh;

import net.minecraft.resources.ResourceLocation;
import org.mtr.libraries.de.javagl.obj.Obj;
import org.mtr.libraries.de.javagl.obj.ObjFace;
import org.mtr.libraries.de.javagl.obj.ObjGroup;
import org.mtr.libraries.de.javagl.obj.ObjReader;
import org.mtr.libraries.de.javagl.obj.ObjSplitting;
import org.mtr.libraries.de.javagl.obj.ObjUtils;
import org.mtr.libraries.de.javagl.obj.Mtl;
import org.mtr.libraries.de.javagl.obj.MtlReader;
import org.mtr.libraries.de.javagl.obj.FloatTuple;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.mtr.resource.ResourceProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.joml.Vector3f;

/**
 * Compatibility replacement for the removed MTR 4.0
 * {@code org.mtr.mod.resource.ModelResourceLoader}: parses an OBJ model into
 * raw script-mutable meshes ({@link RawModel}), mirroring MTR 4.1's
 * {@code ObjModelLoader} parsing semantics (per-group split, per-material
 * meshes, {@code flipv} material options) but producing the JCM mesh data
 * structures instead of uploading to the GPU.
 */
public final class ModelResourceLoader {

        private ModelResourceLoader() {
        }

        public static boolean isSupportedModelResource(String id) {
                final String lower = id.toLowerCase(Locale.ROOT);
                return lower.endsWith(".obj") || lower.endsWith(".zip.obj");
        }

        /**
         * @param id              model identifier ({@code namespace:path.obj})
         * @param defaultTexture  fallback texture when the material defines none
         * @param flipTextureV    invert V coordinates
         * @param resourceProvider resource access
         * @return one {@link RawModel} per OBJ {@code g} group
         */
        public static Map<String, RawModel> loadModel(String id, ResourceLocation defaultTexture, boolean flipTextureV, ResourceProvider resourceProvider) {
                final ResourceLocation modelLocation = ResourceLocation.parse(id.endsWith(".obj") ? id.substring(0, id.length() - 4) : id);
                final String objString = resourceProvider.get(withExtension(modelLocation, "obj"));
                if (objString == null) {
                        throw new IllegalArgumentException("Missing OBJ model " + id);
                }

                final Obj sourceObj;
                try {
                        sourceObj = ObjReader.read(IOUtils.toInputStream(objString, StandardCharsets.UTF_8));
                } catch (IOException e) {
                        throw new IllegalArgumentException("Failed to parse OBJ model " + id, e);
                }

                // Load MTL files referenced by the OBJ
                final Object2ObjectOpenHashMap<String, Mtl> materials = new Object2ObjectOpenHashMap<>();
                for (final String mtlFileName : sourceObj.getMtlFileNames()) {
                        try {
                                final String mtlString = resourceProvider.get(withExtension(modelLocation, mtlFileName.endsWith(".mtl") ? mtlFileName : mtlFileName + ".mtl"));
                                if (mtlString != null) {
                                        MtlReader.read(IOUtils.toInputStream(mtlString, StandardCharsets.UTF_8)).forEach(mtl -> materials.put(mtl.getName(), mtl));
                                }
                        } catch (IOException ignored) {
                                // one unreadable MTL must not take the whole model down (same tolerance as MTR's loader)
                        }
                }

                final Map<String, RawModel> result = new HashMap<>();
                final Map<String, Obj> groups = splitByGroups(sourceObj);
                groups.forEach((groupName, groupObj) -> result.put(groupName, buildModel(groupObj, materials, defaultTexture, flipTextureV)));
                return result;
        }

        private static ResourceLocation withExtension(ResourceLocation base, String extension) {
                return ResourceLocation.fromNamespaceAndPath(base.getNamespace(), base.getPath() + "." + extension);
        }

        /**
         * Mirrors {@code ObjSplitting.splitByGroups} semantics used by MTR when
         * splitting per group; falls back to one unnamed group for group-less
         * models.
         */
        private static Map<String, Obj> splitByGroups(Obj obj) {
                final Map<String, Obj> split = ObjSplitting.splitByGroups(obj);
                final Map<String, Obj> result = new HashMap<>();
                for (int groupIndex = 0; groupIndex < obj.getNumGroups(); groupIndex++) {
            final ObjGroup group = obj.getGroup(groupIndex);
                        if (group.getNumFaces() > 0) {
                                result.put(group.getName(), split.getOrDefault(group.getName(), obj));
                        }
                }
                if (result.isEmpty()) {
                        result.put("", obj);
                }
                return result;
        }

        private static RawModel buildModel(Obj groupObj, Object2ObjectOpenHashMap<String, Mtl> materials, ResourceLocation defaultTexture, boolean flipTextureV) {
                final RawModel rawModel = new RawModel();
                ObjSplitting.splitByMaterialGroups(groupObj).forEach((materialKey, materialObj) -> {
                        if (materialObj.getNumFaces() == 0) {
                                return;
                        }

                        final Object2ObjectOpenHashMap<String, String> options = splitMaterialOptions(materialKey);
                        final String materialName = options.get("");
                        final boolean newFlipTextureV = flipTextureV || "1".equals(options.getOrDefault("flipv", "0"));

                        final ResourceLocation texture;
                        final Integer color;
                        final boolean translucent;
                        final Mtl mtl = materials.get(materialKey);
                        if (mtl != null) {
                                texture = mtl.getMapKd() == null || mtl.getMapKd().isEmpty() ? defaultTexture : resolveTexture(mtl.getMapKd(), defaultTexture);
                                final FloatTuple kd = mtl.getKd();
                                final Float d = mtl.getD();
                                color = kd == null ? null : 0xFF000000 | ((int) (kd.getX() * 255) << 16) | ((int) (kd.getY() * 255) << 8) | (int) (kd.getZ() * 255);
                                translucent = d != null && d < 1;
                        } else {
                                texture = "_".equals(materialName) ? defaultTexture : resolveTexture(materialName, defaultTexture);
                                color = null;
                                translucent = false;
                        }

                        final RawMesh rawMesh = new RawMesh(new MaterialProperties(
                                        translucent ? ShaderType.TRANSLUCENT : shaderTypeFor(options.get("#")),
                                        texture,
                                        color
                        ));

                        final Obj renderable = ObjUtils.convertToRenderable(materialObj);
                        for (int i = 0; i < renderable.getNumFaces(); i++) {
                                final ObjFace face = renderable.getFace(i);
                                final int[] vertexIndices = new int[3];
                                for (int v = 0; v < 3; v++) {
                                        final FloatTuple position = renderable.getVertex(face.getVertexIndex(v));
                                        final FloatTuple texCoord = face.getTexCoordIndex(v) == -1 ? null : renderable.getTexCoord(face.getTexCoordIndex(v));
                                        final FloatTuple normal = face.getNormalIndex(v) == -1 ? null : renderable.getNormal(face.getNormalIndex(v));
                                        final Vertex vertex = new Vertex();
                                        vertex.position = new Vector3f(position.getX(), position.getY(), position.getZ());
                                        if (normal != null) {
                                                vertex.normal = new Vector3f(normal.getX(), normal.getY(), normal.getZ());
                                        }
                                        vertex.u = texCoord == null ? 0 : texCoord.getX();
                                        vertex.v = texCoord == null ? 0 : newFlipTextureV ? 1 - texCoord.getY() : texCoord.getY();
                                        if (color != null) {
                                                vertex.color = color;
                                        }
                                        // RawMesh.addVertex groups every 4 vertices into a face; OBJ faces are
                                        // triangles after convertToRenderable, so register indices directly.
                                        vertexIndices[v] = rawMesh.vertices.size();
                                        rawMesh.vertices.add(vertex);
                                }
                                rawMesh.faces.add(new Face(vertexIndices));
                        }

                        rawModel.append(rawMesh);
                });
                return rawModel;
        }

        private static ResourceLocation resolveTexture(String textureName, ResourceLocation defaultTexture) {
                if (textureName == null || textureName.isEmpty() || "default.png".equals(textureName)) {
                        return defaultTexture;
                }
                final String cleaned = textureName.replace('\\', '/');
                if (cleaned.contains(":")) {
                        return ResourceLocation.parse(cleaned.endsWith(".png") ? cleaned : cleaned + ".png");
                }
                return ResourceLocation.fromNamespaceAndPath(defaultTexture.getNamespace(), (cleaned.endsWith(".png") ? cleaned : cleaned + ".png"));
        }

        private static ShaderType shaderTypeFor(@org.jspecify.annotations.Nullable String renderStageOption) {
                if (renderStageOption == null) {
                        return ShaderType.CUTOUT;
                }
                return switch (renderStageOption.toLowerCase(Locale.ROOT)) {
                        case "always_on_light", "light" -> ShaderType.CUTOUT_GLOWING;
                        case "interior_translucent", "exterior_translucent" -> ShaderType.TRANSLUCENT;
                        case "interior" -> ShaderType.CUTOUT_BRIGHT;
                        default -> ShaderType.CUTOUT;
                };
        }

        private static Object2ObjectOpenHashMap<String, String> splitMaterialOptions(String materialGroup) {
                final Object2ObjectOpenHashMap<String, String> result = new Object2ObjectOpenHashMap<>();
                final String[] split = materialGroup.split("#");
                result.put("", split[0]);
                for (int i = 1; i < split.length; i++) {
                        final String option = split[i];
                        final int equalsIndex = option.indexOf('=');
                        if (equalsIndex >= 0) {
                                result.put(option.substring(0, equalsIndex), option.substring(equalsIndex + 1));
                        } else {
                                result.put(option, "1");
                        }
                }
                return result;
        }
}
