package com.lx862.mtrscripting.core.util.model;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import com.lx862.mtrscripting.core.util.ScriptVector3f;
import com.lx862.mtrscripting.mod.util.JSUtil;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import com.lx862.jcm.mod.render.mesh.MaterialProperties;
import com.lx862.jcm.mod.render.mesh.Face;
import com.lx862.jcm.mod.render.mesh.RawMesh;
import com.lx862.jcm.mod.render.mesh.RawModel;
import com.lx862.jcm.mod.render.mesh.Vertex;

import java.util.stream.IntStream;

public class RawMeshBuilderJS {

    private final RawMesh mesh;

    private final int faceSize;
    private Vertex buildingVertex = new Vertex();

    public RawMeshBuilderJS(int faceSize, String renderType, String textureId) {
        this(faceSize, renderType, JSUtil.parseId(textureId));
    }

    public RawMeshBuilderJS(int faceSize, String renderType, ResourceLocation textureId) {
        /* A face needs at least 3 vertices: faceSize 0 divides by zero in
           endVertex(), 1-2 index out of bounds in Face.triangulate — both
           left the script erroring on every render (retry cooldown loop)
           with no hint at the actual cause. */
        if(faceSize < 3) {
            throw new IllegalArgumentException("RawMeshBuilder faceSize must be at least 3 (got " + faceSize + ")");
        }
        this.faceSize = faceSize;
        this.mesh = new RawMesh(new MaterialProperties(com.lx862.jcm.mod.render.mesh.ShaderType.fromRenderStage(RawModelJS.stringToRenderStage(renderType)), textureId, 0xFFFFFFFF));
    }

    public RawMesh getMesh() {
        return mesh;
    }

    public RawModelJS asRawModel() {
        RawModel rawModel = new RawModel();
        rawModel.append(getMesh());
        return new RawModelJS(rawModel);
    }

    public RawMeshBuilderJS reset() {
        mesh.vertices.clear();
        mesh.faces.clear();
        setNewDefaultVertex();
        return this;
    }

    public RawMeshBuilderJS vertex(ScriptVector3f position) {
        float x = position.x();
        float y = position.y();
        float z = position.z();
        buildingVertex.position = new Vector3f(x, -y, -z);
        return this;
    }

    public RawMeshBuilderJS vertex(double d, double e, double f) {
        buildingVertex.position = new Vector3f((float) d, (float) -e, (float) -f);
        return this;
    }

    public RawMeshBuilderJS normal(float f, float g, float h) {
        buildingVertex.normal = new Vector3f(f, -g, -h);
        return this;
    }

    public RawMeshBuilderJS color(int r, int g, int b, int a) {
        /* ARGB, matching every consumer: MaterialProperties.color, the
           java.awt.Color(int, true) read in RawMesh.appendTriangle and the
           0xFFxxxxxx Kd packing in ModelResourceLoader. The 4.1 port briefly
           packed RGBA, which put the red channel into the alpha slot — any
           script colour with red < 255 (e.g. black text) vanished. */
        mesh.materialProperties.setColor((a << 24) | (r << 16) | (g << 8) | b);
        return this;
    }

    public RawMeshBuilderJS uv(float f, float g) {
        buildingVertex.u = f;
        buildingVertex.v = g;
        return this;
    }

    public RawMeshBuilderJS endVertex() {
        mesh.vertices.add(buildingVertex);
        setNewDefaultVertex();
        if (mesh.vertices.size() % faceSize == 0) {
            mesh.faces.addAll(Face.triangulate(IntStream.range(mesh.vertices.size() - faceSize, mesh.vertices.size()).toArray()));
        }
        return this;
    }

    @ApiInternal
    private void setNewDefaultVertex() {
        buildingVertex = new Vertex();
        buildingVertex.position = new Vector3f(0, 0, 0);
        buildingVertex.normal = new Vector3f(0, 0, 0);
        buildingVertex.u = 0;
        buildingVertex.v = 0;
    }
}
