package com.lx862.mtrscripting.core.util.model;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.annotation.ValueNullable;
import com.lx862.mtrscripting.core.util.DataReaderJS;
import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import com.lx862.mtrscripting.mod.MTRScriptingModClient;
import com.lx862.mtrscripting.mod.util.JSUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import org.apache.commons.io.IOUtils;
import org.mtr.libraries.it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.mtr.resource.ResourceManagerHelper;
import org.mtr.MTR;
import org.mtr.resource.CustomResourceTools;
import org.mtr.resource.ResourceProvider;

import java.util.HashMap;
import java.util.Map;

public class ModelManagerJS {
    private static final ResourceProvider RESOURCE_PROVIDER = new ResourceProvider() {
        public String get(ResourceLocation id) {
            DataReaderJS dataReaderJS = ScriptResourceUtil.read(id);
            return dataReaderJS == null ? "" : dataReaderJS.asString();
        }

        public byte[] getBytes(ResourceLocation id) {
            DataReaderJS dataReaderJS = ScriptResourceUtil.read(id);
            return dataReaderJS == null ? null : dataReaderJS.asByteArray();
        }
    };

    private record ModelKey(ResourceLocation id, boolean flipTextureV) {}
    private static final Map<ModelKey, RawModelJS> rawModelCache = new HashMap<>();
    private static final Map<RawModelJS, ModelJS> modelCache = new HashMap<>();

    private ModelManagerJS() {
    }

    @Deprecated
    public static RawModelJS loadRawModel(@ValueNullable Object resourceManager, ResourceLocation id, @ValueNullable Object atlasManager) {
        return loadModel(id, false);
    }

    @Deprecated
    public static Map<String, RawModelJS> loadPartedRawModel(@ValueNullable Object resourceManager, ResourceLocation id, @ValueNullable Object atlasManager) {
        return loadModelParts(id, false);
    }

    @Deprecated
    public static ModelJS uploadVertArrays(RawModelJS modelData) {
        return upload(modelData);
    }

    public static RawModelJS loadModel(String id) {
        return loadModel(JSUtil.parseId(id));
    }

    public static RawModelJS loadModel(ResourceLocation id) {
        return loadModel(id, true);
    }

    public static RawModelJS loadModel(String id, boolean flipTextureV) {
        return loadModel(JSUtil.parseId(id), flipTextureV);
    }

    public static RawModelJS loadModel(ResourceLocation id, boolean flipTextureV) {
        ModelKey key = new ModelKey(id, flipTextureV);
        if(rawModelCache.containsKey(key)) return rawModelCache.get(key);

        try {
            RawModelJS rawModel = new RawModelJS();
            Map<String, RawModelJS> modelDataParts = loadModelParts(id, flipTextureV);
            modelDataParts.values().forEach(rawModel::append);
            rawModelCache.put(key, rawModel);
            return rawModel;
        } catch (Exception e) {
            MTRScriptingModClient.LOGGER.error("", e);
            return null;
        }
    }

    public static Map<String, RawModelJS> loadModelParts(String modelLocation, boolean flipTextureV) {
        return loadModelParts(JSUtil.parseId(modelLocation), flipTextureV);
    }

    public static Map<String, RawModelJS> loadModelParts(ResourceLocation modelLocation, boolean flipTextureV) {
        String idStr = modelLocation.getNamespace() + ":" + modelLocation.getPath();

        final boolean isSupportedFormat = com.lx862.jcm.mod.render.mesh.ModelResourceLoader.isSupportedModelResource(idStr);
        final ResourceLocation textureId = CustomResourceTools.formatIdentifierWithDefault("", "png");

        if (isSupportedFormat) {
            Map<String, RawModelJS> rawModels = new Object2ObjectOpenHashMap<>();
            Map<String, com.lx862.jcm.mod.render.mesh.RawModel> models = com.lx862.jcm.mod.render.mesh.ModelResourceLoader.loadModel(idStr, textureId, flipTextureV, RESOURCE_PROVIDER);
            for(Map.Entry<String, com.lx862.jcm.mod.render.mesh.RawModel> modelEntry : models.entrySet()) {
                rawModels.put(modelEntry.getKey(), new RawModelJS(modelEntry.getValue()));
            }
            return rawModels;
        } else {
            throw new IllegalArgumentException("Unsupported model format " + idStr + ".");
        }
    }

    public static Map<String, RawModelJS> loadModelParts(ResourceLocation modelLocation) {
        return loadModelParts(modelLocation, true);
    }

    public static RawModelJS loadModelPart(String modelName, String modelLocation) {
        return loadModelPart(modelName, JSUtil.parseId(modelLocation));
    }

    public static RawModelJS loadModelPart(String modelName, ResourceLocation modelLocation) {
        return loadModelPart(modelName, modelLocation, true);
    }

    public static RawModelJS loadModelPart(String modelName, String modelLocation, boolean flipTextureV) {
        return loadModelPart(modelName, JSUtil.parseId(modelLocation), flipTextureV);
    }

    public static RawModelJS loadModelPart(String modelName, ResourceLocation modelLocation, boolean flipTextureV) {
        return loadModelParts(modelLocation, flipTextureV).get(modelName);
    }

    public static ModelJS upload(RawModelJS rawModel) {
        if(!RenderSystem.isOnRenderThread()) throw new IllegalStateException("Cannot invoke ModelManager.upload() off-thread! Upload during script parsing phase or use DynamicModelHolder.");

        if(modelCache.containsKey(rawModel)) return modelCache.get(rawModel);
        ModelJS uploadedModel = ModelJS.uploadRawModel(rawModel);
        modelCache.put(rawModel, uploadedModel);
        return uploadedModel;
    }

    @ApiInternal
    public static void reset() {
        modelCache.values().forEach(ModelJS::close);
        modelCache.clear();
        rawModelCache.clear();
    }
}
