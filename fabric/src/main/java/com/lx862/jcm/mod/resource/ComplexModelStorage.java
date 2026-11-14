package com.lx862.jcm.mod.resource;

import com.lx862.jcm.mod.render.mesh.OptimizedModelWrapper;
import com.lx862.mtrscripting.core.util.model.ModelJS;
import com.lx862.mtrscripting.core.util.model.ModelManagerJS;
import com.lx862.mtrscripting.core.util.model.RawModelJS;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ComplexModelStorage {
    private static final Map<ResourceLocation, OptimizedModelWrapper> models = new HashMap<>();
    private static final Set<ResourceLocation> modelToLoad = new HashSet<>();

    /**
     * Only call once for each BER constructor.
     */
    public static void requireModel(ResourceLocation identifier) {
        modelToLoad.add(identifier);
    }

    public static OptimizedModelWrapper getModel(ResourceLocation identifier) {
        return models.get(identifier);
    }

    public static void beginReload() {
        modelToLoad.forEach(id -> {
            /* Yanyang: close the previous GL model before replacing it —
               put() alone leaked its buffers on every reload. */
            OptimizedModelWrapper previous = models.get(id);
            if (previous != null) {
                previous.close();
            }
            RawModelJS rawModelJS = ModelManagerJS.loadModel(id);
            if (rawModelJS == null) {
                return;
            }
            ModelJS modelJS = ModelJS.uploadRawModel(rawModelJS);
            models.put(id, modelJS.getUnderlyingModel());
        });
        /* Yanyang: the request list survives reset() otherwise — stale ids
           were re-uploaded forever. beginReload consumed them. */
        modelToLoad.clear();
    }

    public static void reset() {
        models.values().forEach(OptimizedModelWrapper::close);
        models.clear();
        modelToLoad.clear();
    }
}
