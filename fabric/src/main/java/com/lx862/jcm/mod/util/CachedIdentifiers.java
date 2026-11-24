package com.lx862.jcm.mod.util;

import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for {@link ResourceLocation} instances that scripts and the native
 * record replay re-request every frame (font ids, texture ids, sound ids — a
 * small, mostly closed set).
 *
 * <p>Identifier construction parses and validates the namespace/path on
 * every call, so a per-frame {@code ResourceLocation.parse(id)} for the same string
 * is pure young-gen churn. The cache is also hit from the script-executor
 * threads (TextWrapper.font / TextureWrapper.texture run inside script
 * render functions) and cleared from the native driver on tick — hence a
 * ConcurrentHashMap instead of the "render-thread only" HashMap this class
 * used to document. When the cache overflows (a pack with pathological id
 * variety) it is dropped and rebuilt, mirroring NativeHost's glyph-mask cache
 * policy.</p>
 */
public final class CachedIdentifiers {

    private static final ConcurrentHashMap<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private static final int CAPACITY = 512;

    private CachedIdentifiers() {
    }

    public static ResourceLocation of(String id) {
        final ResourceLocation cached = CACHE.get(id);
        if (cached != null) return cached;
        final ResourceLocation created = ResourceLocation.parse(id);
        if (CACHE.size() >= CAPACITY) CACHE.clear();
        CACHE.put(id, created);
        return created;
    }

    /** Drop every cached id (resource reload / world switch). */
    public static void clear() {
        CACHE.clear();
    }
}
