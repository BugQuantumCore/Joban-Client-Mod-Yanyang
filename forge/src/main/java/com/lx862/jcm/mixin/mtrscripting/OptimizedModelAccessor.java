package com.lx862.jcm.mixin.mtrscripting;


import org.mtr.mapping.mapper.OptimizedModel;
import com.lx862.jcm.mod.render.mesh.VertexArray;
import com.lx862.jcm.mod.render.mesh.VertexAttributeMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = OptimizedModel.class, remap = false)
public interface OptimizedModelAccessor {
    @Accessor("DEFAULT_MAPPING")
    static VertexAttributeMapping getDefaultMapping() {
        throw new AssertionError();
    }

    @Accessor("uploadedParts")
    List<VertexArray> getUploadedParts();
}
