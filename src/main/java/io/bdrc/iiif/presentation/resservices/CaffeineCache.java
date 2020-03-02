package io.bdrc.iiif.presentation.resservices;

import java.util.concurrent.TimeUnit;

import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.bdrc.iiif.presentation.CaffeineStats;
import io.bdrc.iiif.presentation.SpringBootIIIFPres;
import io.bdrc.iiif.presentation.metrics.CacheMetrics;

public class CaffeineCache {

    private static Cache<String, Object> cache;
    public final static Logger log = LoggerFactory.getLogger(CaffeineCache.class.getName());

    public static void init() {
        cache = Caffeine.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).recordStats()
                .maximumSize(Long.parseLong(SpringBootIIIFPres.getProperty("mainCacheSize"))).build();
    }

    public static void put(Object res, String key) {
        try {
            log.info("PUT IN CACHE {}", res);
            cache.put(key, res);
            CacheMetrics.put();
            res = null;

        } catch (CacheException e) {
            log.error("Problem putting Results -->" + res + " in the iiifpres cache, for key -->" + key + " Exception:" + e.getMessage());
        }
    }

    public static Object getObjectFromCache(String key) {
        log.info("GETTING {} key from CACHE ", key);
        Object obj = cache.getIfPresent(key);
        if (obj != null) {
            CacheMetrics.found();
        } else {
            CacheMetrics.notFound();
        }
        return obj;
    }

    public static boolean clearCache() {
        try {
            cache.invalidateAll();
            log.info("The iiifpres cache has been cleared");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public CaffeineStats getStats() {
        return new CaffeineStats(cache);
    }

}
