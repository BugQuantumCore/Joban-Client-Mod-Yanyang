package com.lx862.jcm.mod.render;
import com.lx862.jcm.mod.render.HolderBase;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code com.lx862.jcm.mod.render.HolderBase} marker interface. Kept so legacy
 * widget wrapper signatures compile unchanged; no runtime behavior.
 */
public interface HolderBase<T> extends Comparable<HolderBase<T>> {
}
