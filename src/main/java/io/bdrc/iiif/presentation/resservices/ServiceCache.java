package io.bdrc.iiif.presentation.resservices;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.metrics.CacheMetrics;

public class ServiceCache {

    public static Cache<String, Object> CACHE;
    public final static Logger log = LoggerFactory.getLogger(ServiceCache.class.getName());

    public static void init() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE = cacheManager.createCache("iiifpres", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Object.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        log.debug("Cache was initialized {}", CACHE);
    }

    public static void put(Object res, final String key) {
        try {
            log.debug("put object in cache for key: {}", key);
            CACHE.put(key, res);
            CacheMetrics.put();
            res = null;
        } catch (Exception e) {
            log.error("Problem putting Results -->" + res + " in the iiifpres cache, for key -->" + key + " Exception:" + e.getMessage());
        }
    }

    public static Object getObjectFromCache(final String key) {
        final Object obj = CACHE.get(key);
        if (obj != null) {
            log.debug("got object in cache for key: {}", key);
            CacheMetrics.found();
        } else {
            log.debug("can't get object in cache for key: {}", key);
            CacheMetrics.notFound();
        }
        return obj;
    }

    public static boolean clearCache() {
        try {
            CACHE.clear();
            log.info("The iiifpres cache has been cleared");
            return true;
        } catch (Exception e) {
            return false;
        }

    }

}
