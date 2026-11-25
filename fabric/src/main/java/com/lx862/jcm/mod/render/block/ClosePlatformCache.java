package com.lx862.jcm.mod.render.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.mtr.MTRClient;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongArrayList;
import org.mtr.libraries.it.unimi.dsi.fastutil.longs.LongImmutableList;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-TTL cache for the {@code MTRClient.findClosePlatform} lookups the
 * block-entity renderers issue every frame (the underlying scan re-walks the
 * nearby platform set per call). Platform ids only change through the sync
 * channel, so a 1 second TTL is invisible to the rendered result; the level
 * identity is part of every entry so a dimension change invalidates them.
 */
final class ClosePlatformCache {

    private static final long TTL_MILLIS = 1000;
    /* Hard cap: one entry per rendered block position; cleared wholesale
       past the cap (same discipline as the other render caches). */
    private static final int MAX_ENTRIES = 512;

    /** Radius shared by every renderer call site. */
    private static final int SEARCH_RADIUS = 5;

    private static final Map<Long, CachedPlatforms> CACHE = new ConcurrentHashMap<>();

    private record CachedPlatforms(Level level, LongImmutableList platformIds, long timestamp) {
    }

    private ClosePlatformCache() {
    }

    static LongImmutableList get(Level level, BlockPos pos) {
        final long key = pos.asLong();
        final long now = System.currentTimeMillis();
        final CachedPlatforms cached = CACHE.get(key);
        if(cached != null && cached.level() == level && now - cached.timestamp() <= TTL_MILLIS) {
            return cached.platformIds();
        }

        final LongArrayList ids = new LongArrayList();
        MTRClient.findClosePlatform(pos, SEARCH_RADIUS, platform -> ids.add(platform.getId()));
        final LongImmutableList platformIds = new LongImmutableList(ids);
        if(CACHE.size() >= MAX_ENTRIES) CACHE.clear();
        CACHE.put(key, new CachedPlatforms(level, platformIds, now));
        return platformIds;
    }
}
