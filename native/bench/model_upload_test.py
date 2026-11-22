"""Run production ModelJS/RawMeshBuilderJS against MTR 4.1's real model classes.

MTR 4.1 moved the GPU upload into Minecraft's own VertexBuffer, so the test
pins the JCM side of the boundary instead of driving a GL context: the real
NewOptimizedModelGroup/NewOptimizedModel/StoredVertexData classes come from
--mtr-jar (the pinned MTR-Yanyang release), while the small Minecraft vertex
surface is stubbed with a recording implementation (see STUBS). RawMesh is
compiled from a generated fixture that implements the RawMeshAccessor mixin
surface (the de-finalised fields + setters the mixin weaves in production),
regenerated from the production source on every run so it cannot drift.
"""
import argparse
import os
from pathlib import Path
import platform
import subprocess
import urllib.request

STUBS = {
    'net/minecraft/resources/ResourceLocation.java': '''package net.minecraft.resources;
public record ResourceLocation(String path) {
        public String getPath() { return path; }
        public static ResourceLocation parse(String s) { return new ResourceLocation(s); }
        @Override public String toString() { return path; }
}''',
    'com/mojang/blaze3d/vertex/VertexFormat.java': '''package com.mojang.blaze3d.vertex;
public class VertexFormat {
        public enum Mode { LINES, LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, QUADS }
}''',
    'com/mojang/blaze3d/vertex/DefaultVertexFormat.java': '''package com.mojang.blaze3d.vertex;
public final class DefaultVertexFormat {
        public static final VertexFormat NEW_ENTITY = new VertexFormat();
        private DefaultVertexFormat() {}
}''',
    'com/mojang/blaze3d/vertex/VertexConsumer.java': '''package com.mojang.blaze3d.vertex;
public interface VertexConsumer {
        default VertexConsumer addVertex(float x, float y, float z) { return this; }
        /* Pose-overloads used by OptimizedModelWrapper's buffered (plain-GL)
           replay path; real MC 1.21.1/1.21.4 declare both. */
        default VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) { return this; }
        default VertexConsumer setColor(int red, int green, int blue, int alpha) { return this; }
        default VertexConsumer setColor(int argb) { return this; }
        default VertexConsumer setUv(float u, float v) { return this; }
        default VertexConsumer setUv1(int u, int v) { return this; }
        default VertexConsumer setUv2(int u, int v) { return this; }
        default VertexConsumer setNormal(float x, float y, float z) { return this; }
        default VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) { return this; }
        default VertexConsumer setOverlay(int packedOverlay) { return this; }
        default VertexConsumer setLight(int packedLight) { return this; }
        /* Real MC 1.21.1 declares the packed 11-arg addVertex as VOID — the
           descriptor must match or the jar's StoredVertexData.apply call site
           dies with NoSuchMethodError at runtime. */
        default void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
        }
}''',
    'com/mojang/blaze3d/vertex/BufferBuilder.java': '''package com.mojang.blaze3d.vertex;
import java.util.ArrayList;
import java.util.List;
public class BufferBuilder implements VertexConsumer {
        public static final class Vertex {
                public final float x, y, z, u, v, nx, ny, nz;
                public final int color, overlay, light;
                public Vertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
                        this.x = x; this.y = y; this.z = z; this.color = color; this.u = u; this.v = v;
                        this.overlay = overlay; this.light = light; this.nx = nx; this.ny = ny; this.nz = nz;
                }
        }
        public final List<Vertex> recorded = new ArrayList<>();
        public final VertexFormat.Mode mode;
        public BufferBuilder(VertexFormat.Mode mode, VertexFormat format) { this.mode = mode; }
        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
                recorded.add(new Vertex(x, y, z, color, u, v, overlay, light, nx, ny, nz));
        }
        public MeshData build() { return new MeshData(mode, recorded); }
}''',
    'com/mojang/blaze3d/vertex/MeshData.java': '''package com.mojang.blaze3d.vertex;
import java.util.List;
public final class MeshData {
        public final VertexFormat.Mode mode;
        public final List<BufferBuilder.Vertex> vertices;
        public MeshData(VertexFormat.Mode mode, List<BufferBuilder.Vertex> vertices) { this.mode = mode; this.vertices = vertices; }
}''',
    'com/mojang/blaze3d/vertex/Tesselator.java': '''package com.mojang.blaze3d.vertex;
public final class Tesselator {
        private static final Tesselator INSTANCE = new Tesselator();
        public static Tesselator getInstance() { return INSTANCE; }
        public BufferBuilder begin(VertexFormat.Mode mode, VertexFormat format) { return new BufferBuilder(mode, format); }
}''',
    'com/mojang/blaze3d/vertex/VertexBuffer.java': '''package com.mojang.blaze3d.vertex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class VertexBuffer {
        /** Every buffer ever created, in creation order (correlates to NewOptimizedModel instances). */
        public static final List<VertexBuffer> ALL = Collections.synchronizedList(new ArrayList<>());
        public enum Usage { STATIC }
        public final List<MeshData> uploads = new ArrayList<>();
        public int closeCount;
        public VertexBuffer(Usage usage) { ALL.add(this); }
        public void bind() {}
        public void upload(MeshData meshData) { uploads.add(meshData); }
        public void close() { closeCount++; }
        public static void unbind() {}
}''',
    'net/minecraft/client/renderer/texture/OverlayTexture.java': '''package net.minecraft.client.renderer.texture;
public final class OverlayTexture {
        public static final int NO_OVERLAY = 0;
        private OverlayTexture() {}
}''',
    # OptimizedModelWrapper's buffered replay bakes the per-stage lightmap
    # (full-bright stages keep the FULL_BRIGHT constant).
    'net/minecraft/client/renderer/LightTexture.java': '''package net.minecraft.client.renderer;
public final class LightTexture {
        public static final int FULL_BRIGHT = 0xF000F0;
        private LightTexture() {}
}''',
    'net/minecraft/world/phys/AABB.java': '''package net.minecraft.world.phys;
public class AABB {
        public AABB(double x1, double y1, double z1, double x2, double y2, double z2) {}
}''',
    'net/minecraft/world/phys/Vec3.java': '''package net.minecraft.world.phys;
public class Vec3 {
        public final double x, y, z;
        public Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }
}''',
    'com/mojang/blaze3d/vertex/PoseStack.java': '''package com.mojang.blaze3d.vertex;
import net.minecraft.world.phys.Vec3;
public class PoseStack {
        public static final class Pose { }
        public void pushPose() {}
        public void popPose() {}
        public void translate(double x, double y, double z) {}
        public Pose last() { return new Pose(); }
        public Pose pose() { return new Pose(); }
}''',
    # ModelJS defers its GPU uploads through RenderSystem when a scripting
    # executor thread calls copyForMaterialChanges/replaceTexture; on the
    # render thread (this headless test) the synchronous path is taken.
    'com/mojang/blaze3d/systems/RenderSystem.java': '''package com.mojang.blaze3d.systems;
public class RenderSystem {
        public static boolean isOnRenderThread() { return true; }
        public static void recordRenderCall(Runnable runnable) { runnable.run(); }
}''',
    # The remaining Minecraft surface exists purely so javac can complete the
    # real MTR classes' signatures (StoredVertexData.write, NewOptimizedModel.render,
    # MainRenderer.render); none of it is exercised by the headless test.
    'net/minecraft/client/model/geom/ModelPart.java': '''package net.minecraft.client.model.geom;
public class ModelPart { }''',
    'net/minecraft/client/renderer/RenderType.java': '''package net.minecraft.client.renderer;
public abstract class RenderType {
        public void draw(com.mojang.blaze3d.vertex.MeshData meshData) {}
}''',
    'net/minecraft/client/renderer/ShaderInstance.java': '''package net.minecraft.client.renderer;
public class ShaderInstance { }''',
    'net/minecraft/client/renderer/CompiledShaderProgram.java': '''package net.minecraft.client.renderer;
public class CompiledShaderProgram { }''',
    'net/minecraft/client/renderer/MultiBufferSource.java': '''package net.minecraft.client.renderer;
public interface MultiBufferSource { }''',
    'org/mtr/mapping/tool/DummyClass.java': '''package org.mtr.mapping.tool;
public class DummyClass { public static void logException(Exception e) {} }''',
    'com/lx862/jcm/mixin/mtrscripting/RawMeshAccessor.java': '''package com.lx862.jcm.mixin.mtrscripting;
import com.lx862.jcm.mod.render.mesh.MaterialProperties;
import com.lx862.jcm.mod.render.mesh.Face;
import com.lx862.jcm.mod.render.mesh.RawMesh;
import com.lx862.jcm.mod.render.mesh.Vertex;
import java.util.List;
/** Headless stand-in for the mixin-woven accessor; the generated RawMesh fixture implements it. */
public interface RawMeshAccessor {
        void setMaterialProperties(MaterialProperties properties);
        void setVertices(List<Vertex> vertices);
        void setFaces(List<Face> faces);
}''',
    'com/lx862/jcm/mod/util/JCMLogger.java': '''package com.lx862.jcm.mod.util;
public final class JCMLogger {
        public static void info(String s, Object... o) {}
        public static void warn(String s, Object... o) {}
        public static void error(String s, Object... o) {}
        public static void error(String s, Throwable e) {}
        public static void fatal(String s, Object... o) {}
        public static void debug(String s, Object... o) {}
}''',
    'com/lx862/mtrscripting/core/annotation/ApiInternal.java': 'package com.lx862.mtrscripting.core.annotation; public @interface ApiInternal {}',
    'com/lx862/mtrscripting/core/util/ScriptVector3f.java': '''package com.lx862.mtrscripting.core.util;
public record ScriptVector3f(float x, float y, float z) {
        public org.joml.Vector3f rawVector3f() { return new org.joml.Vector3f(x, y, z); }
}''',
    # The real one drags ScriptResourceUtil -> JCM/AWT/commons; parseId is a
    # plain passthrough onto the stubbed ResourceLocation record here.
    'com/lx862/mtrscripting/mod/util/JSUtil.java': '''package com.lx862.mtrscripting.mod.util;
public class JSUtil {
        public static net.minecraft.resources.ResourceLocation parseId(String str) { return new net.minecraft.resources.ResourceLocation(str); }
}''',
}

# Injection applied to a copy of the production RawMesh.java: the RawMeshAccessor
# mixin weaves these accessors in production (@Accessor + @Mutable on the final
# fields); the fixture de-finalises the three fields and implements the setters,
# so copyForMaterialChanges' field rebinds work verbatim in the headless build.
FIXTURE_MARKER = 'public final class RawMesh {'
FIXTURE_DECL = 'public final class RawMesh implements com.lx862.jcm.mixin.mtrscripting.RawMeshAccessor {'
FIXTURE_FIELDS = {
    'public final MaterialProperties materialProperties;': 'public MaterialProperties materialProperties;',
    'public final List<Vertex> vertices = new ArrayList<>();': 'public List<Vertex> vertices = new ArrayList<>();',
    'public final List<Face> faces = new ArrayList<>();': 'public List<Face> faces = new ArrayList<>();',
}
FIXTURE_METHODS = '''
        /* Test fixture: the RawMeshAccessor mixin weaves these in production. */
        @Override
        public void setMaterialProperties(MaterialProperties properties) { this.materialProperties = properties; }
        @Override
        public void setVertices(List<Vertex> vertices) { this.vertices = vertices; }
        @Override
        public void setFaces(List<Face> faces) { this.faces = faces; }
'''


def raw_mesh_fixture(source: str) -> str:
    if source.count(FIXTURE_MARKER) != 1:
        raise SystemExit('RawMesh.java: fixture marker not found exactly once')
    text = source.replace(FIXTURE_MARKER, FIXTURE_DECL)
    for original, de_finalised in FIXTURE_FIELDS.items():
        if text.count(original) != 1:
            raise SystemExit(f'RawMesh.java: field declaration not found exactly once: {original}')
        text = text.replace(original, de_finalised)
    tail = text.rfind('}')
    return text[:tail] + FIXTURE_METHODS + '}'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mtr-jar', required=True, type=Path)
    parser.add_argument('--work', required=True, type=Path)
    parser.add_argument('--java-home', type=Path, default=os.environ.get('JAVA_HOME'))
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[2]
    work = args.work.resolve()
    work.mkdir(parents=True, exist_ok=True)

    def fetch(url: str, name: str) -> Path:
        path = work / name
        if not path.exists():
            urllib.request.urlretrieve(url, path)
        return path

    joml = fetch('https://repo.maven.apache.org/maven2/org/joml/joml/1.10.5/joml-1.10.5.jar', 'joml-1.10.5.jar')
    annotations = fetch('https://repo.maven.apache.org/maven2/org/jetbrains/annotations/24.1.0/annotations-24.1.0.jar', 'annotations-24.1.0.jar')
    dependencies = [joml, annotations, args.mtr_jar.resolve()]

    sources = []
    for name, content in STUBS.items():
        path = work / 'src' / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding='utf-8')
        sources.append(path)

    base = repo / 'fabric/src/main/java'
    mesh = base / 'com/lx862/jcm/mod/render/mesh'
    # Generated fixture: the production RawMesh plus the mixin-woven accessors.
    fixture = work / 'src' / 'com/lx862/jcm/mod/render/mesh/RawMesh.java'
    fixture.parent.mkdir(parents=True, exist_ok=True)
    fixture.write_text(raw_mesh_fixture((mesh / 'RawMesh.java').read_text(encoding='utf-8')), encoding='utf-8')
    sources.append(fixture)
    for name in ('Face.java', 'Vertex.java', 'MaterialProperties.java', 'ShaderType.java', 'RawModel.java', 'OptimizedModelWrapper.java'):
        sources.append(mesh / name)
    model = base / 'com/lx862/mtrscripting/core/util/model'
    for name in ('ModelJS.java', 'RawModelJS.java', 'RawMeshBuilderJS.java'):
        sources.append(model / name)
    sources.append(Path(__file__).with_name('jvm') / 'ModelUploadTest.java')

    classes = work / 'classes'
    classes.mkdir(exist_ok=True)

    def java_tool(name):
        return str(Path(args.java_home) / 'bin' / (name + ('.exe' if os.name == 'nt' else ''))) if args.java_home else name

    argfile = work / 'sources.txt'
    argfile.write_text('\n'.join('"' + str(p).replace('\\', '/') + '"' for p in sources), encoding='utf-8')
    cp = os.pathsep.join(map(str, dependencies))
    subprocess.run([java_tool('javac'), '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes), '@' + str(argfile)], check=True)
    subprocess.run([java_tool('java'), '-cp', str(classes) + os.pathsep + cp,
                    'com.lx862.mtrscripting.core.util.model.ModelUploadTest'], check=True)
    print('host:', platform.system(), platform.machine())


if __name__ == '__main__':
    main()
