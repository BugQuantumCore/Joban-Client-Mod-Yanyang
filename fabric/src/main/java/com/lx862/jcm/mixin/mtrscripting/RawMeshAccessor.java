package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.jcm.mod.render.mesh.MaterialProperties;
import com.lx862.jcm.mod.render.mesh.Face;
import com.lx862.jcm.mod.render.mesh.RawMesh;
import com.lx862.jcm.mod.render.mesh.Vertex;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = RawMesh.class, remap = false)
public interface RawMeshAccessor {
    @Accessor("materialProperties")
    @Mutable
    void setMaterialProperties(MaterialProperties properties);

    @Accessor("vertices")
    @Mutable
    void setVertices(List<Vertex> vertices);

    @Accessor("faces")
    @Mutable
    void setFaces(List<Face> faces);
}
