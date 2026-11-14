package com.lx862.jcm.mixin.mtrscripting;

import org.mtr.mapping.mapper.OptimizedModel;
import com.lx862.jcm.mod.render.mesh.RawMesh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = OptimizedModel.ObjModel.class, remap = false)
public interface ObjModelAccessor {
    @Accessor
    List<RawMesh> getRawMeshes();
}