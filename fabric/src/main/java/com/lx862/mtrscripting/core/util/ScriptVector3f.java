package com.lx862.mtrscripting.core.util;

import com.lx862.mtrscripting.core.annotation.ApiInternal;
import org.mtr.core.data.Position;
import org.mtr.core.tool.Vector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class ScriptVector3f {
    public static final ScriptVector3f ZERO = new ScriptVector3f(0, 0, 0);
    public static final ScriptVector3f XP = new ScriptVector3f(1, 0, 0);
    public static final ScriptVector3f YP = new ScriptVector3f(0, 1, 0);
    public static final ScriptVector3f ZP = new ScriptVector3f(0, 0, 1);

    private org.joml.Vector3f impl;

    @ApiInternal
    public ScriptVector3f(Vec3 impl) {
        if(impl == null) throw new IllegalArgumentException("Cannot use null as the impl!");
        this.impl = new org.joml.Vector3f((float) impl.x, (float) impl.y, (float) impl.z);
    }

    public ScriptVector3f(float x, float y, float z) {
        this(new Vec3(x, y, z));
    }

    public ScriptVector3f(Vector3f impl) {
        this.impl = new org.joml.Vector3f(impl);
    }

    public ScriptVector3f(Vector tscVector) {
        this.impl = new org.joml.Vector3f((float) tscVector.x(), (float) tscVector.y(), (float) tscVector.z());
    }

    public ScriptVector3f(Position tscPosition) {
        this.impl = new org.joml.Vector3f(tscPosition.getX(), tscPosition.getY(), tscPosition.getZ());
    }

    public ScriptVector3f(BlockPos blockPos) {
        this(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    public float x() { return impl.x; }
    public float y() { return impl.y; }
    public float z() { return impl.z; }

    public ScriptVector3f copy() {
        Vec3 newImpl = new Vec3(x(), y(), z());
        return new ScriptVector3f(newImpl);
    }

    public ScriptVector3f normalize() {
        impl = impl.normalize(new org.joml.Vector3f());
        return this;
    }

    public ScriptVector3f add(float x, float y, float z) {
        impl = impl.add(x, y, z);
        return this;
    }

    public ScriptVector3f add(ScriptVector3f other) {
        impl = impl.add(other.impl);
        return this;
    }

    public ScriptVector3f sub(ScriptVector3f other) {
        impl = impl.sub(other.impl);
        return this;
    }

    public ScriptVector3f mul(float x, float y, float z) {
        impl = impl.mul(x, y, z);
        return this;
    }

    public ScriptVector3f mul(float n) {
        impl = impl.mul(n);
        return this;
    }

    public ScriptVector3f rotX(float rad) {
        impl = impl.rotateX(rad);
        return this;
    }

    public ScriptVector3f rotY(float rad) {
        impl = impl.rotateY(rad);
        return this;
    }

    public ScriptVector3f rotZ(float rad) {
        impl = impl.rotateZ(rad);
        return this;
    }

    public ScriptVector3f cross(ScriptVector3f other) {
        impl = impl.cross(other.impl);
        return this;
    }

    public double distance(ScriptVector3f other) {
        double dx = x() - other.x();
        double dy = y() - other.y();
        double dz = z() - other.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public double distanceSq(ScriptVector3f other) {
        double dx = x() - other.x();
        double dy = y() - other.y();
        double dz = z() - other.z();
        return (dx * dx + dy * dy + dz * dz);
    }

    /**
     * Compatibility for NTE, equivalent to {@link ScriptVector3f#rawBlockPos()}
     */
    @Deprecated
    public BlockPos toBlockPos() {
        return rawBlockPos();
    }

    public BlockPos rawBlockPos() {
        return new BlockPos((int)x(), (int)y(), (int)z());
    }

    public Vec3 rawVector3d() {
        return new Vec3(impl.x, impl.y, impl.z);
    }

    public Vector3f rawVector3f() {
        return new Vector3f((float)x(), (float)y(), (float)z());
    }

    public Vector rawTscVector() {
        return new Vector(x(), y(), z());
    }

    public Position rawTscPosition() {
        return new Position((int)x(), (int)y(), (int)z());
    }

    @Override
    public int hashCode() {
        return impl.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        ScriptVector3f scriptVector3F = (ScriptVector3f) o;
        return impl.equals(scriptVector3F.impl);
    }

    @Override
    public String toString() {
        return "ScriptVector3f[" + "x=" + x() + ", y=" + y() + ", z=" + z() + "]";
    }
}
