package com.lx862.jcm.mod.render.mesh;

import org.joml.Vector3f;

import java.util.Objects;
import com.lx862.jcm.mod.render.mesh.Vertex;

/**
 * Mutable mesh vertex used by the script model builders. Position/normal axes
 * follow the legacy MTR model convention (x, -y, -z relative to the model
 * origin); they are converted when the mesh is uploaded to the MTR 4.1
 * optimized-model format.
 */
public final class Vertex {

	public Vector3f position;
	public Vector3f normal;
	public float u, v;

	public int color;
	public int light;

	public Vertex() {
	}

	public Vertex(Vertex vertex) {
		position = vertex.position == null ? null : new Vector3f(vertex.position);
		normal = vertex.normal == null ? null : new Vector3f(vertex.normal);
		u = vertex.u;
		v = vertex.v;
		color = vertex.color;
		light = vertex.light;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		Vertex vertex = (Vertex) o;
		return Float.compare(vertex.u, u) == 0 && Float.compare(vertex.v, v) == 0 && Objects.equals(position, vertex.position) && Objects.equals(normal, vertex.normal);
	}

	@Override
	public int hashCode() {
		return Objects.hash(position, normal, u, v);
	}
}
