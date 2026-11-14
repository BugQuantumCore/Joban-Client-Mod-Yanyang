package com.lx862.jcm.mod.render;
import com.lx862.jcm.mod.render.TextCase;

/**
 * Compatibility shim for the removed MTR 4.0-era
 * {@code org.mtr.data.IGui$TextCase}, used by legacy text-field widgets
 * to force input case.
 */
public enum TextCase {
    DEFAULT,
    LOWER,
    UPPER;
}
