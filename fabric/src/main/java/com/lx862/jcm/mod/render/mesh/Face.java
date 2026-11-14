package com.lx862.jcm.mod.render.mesh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.lx862.jcm.mod.render.mesh.Face;

public final class Face {

	public final int[] vertices;

	public Face(int[] vertices) {
		this.vertices = vertices;
	}

	public Face(Face face) {
		this.vertices = Arrays.copyOf(face.vertices, face.vertices.length);
	}

	public static List<Face> triangulate(int[] vertices) {
		final List<Face> result = new ArrayList<>();
		if (vertices.length > 3) {
			for (int i = 2; i < vertices.length; i++) {
				result.add(new Face(new int[]{vertices[0], vertices[i - 1], vertices[i]}));
			}
		} else {
			result.add(new Face(new int[]{vertices[0], vertices[1], vertices[2]}));
		}
		return result;
	}

	public void flip() {
		for (int i = 0, j = vertices.length - 1; i < j; i++, j--) {
			final int temp = vertices[i];
			vertices[i] = vertices[j];
			vertices[j] = temp;
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof Face)) {
			return false;
		}
		return Arrays.equals(vertices, ((Face) object).vertices);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(vertices);
	}
}