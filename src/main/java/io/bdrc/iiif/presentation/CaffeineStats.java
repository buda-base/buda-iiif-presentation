package io.bdrc.iiif.presentation;

import com.github.benmanes.caffeine.cache.Cache;

public class CaffeineStats {

    Cache<?, ?> cache;

    public CaffeineStats(Cache<?, ?> cache) {
        this.cache = cache;
    }

    /* Returns the number of times an entry has been evicted. */
    public long getEvictionCount() {
        return cache.stats().evictionCount();
    }

    /*
     * Returns the number of times Cache lookup methods have returned an uncached
     * (newly loaded) value, or null.
     */
    public long getMissCount() {
        return cache.stats().missCount();
    }

    /* Returns the ratio of cache requests which were misses. */
    public double getMissRate() {
        return cache.stats().missRate();
    }

    /* Returns the ratio of cache requests which were hits. */
    public double getHitRate() {
        return cache.stats().hitRate();
    }

    /*
     * Returns the number of times Cache lookup methods have returned a cached
     * value.
     */
    public long getHitCount() {
        return cache.stats().hitCount();
    }

    /*
     * Returns the total number of nanoseconds the cache has spent loading new
     * values.
     */
    public long getTotalLoadTime() {
        return cache.stats().totalLoadTime();
    }

}
