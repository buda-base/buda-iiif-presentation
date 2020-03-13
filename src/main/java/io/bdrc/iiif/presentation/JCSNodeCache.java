package io.bdrc.iiif.presentation;

import java.io.IOException;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.maxmind.db.NodeCache;

public class JCSNodeCache implements NodeCache {

    public static Cache<String, JsonNode> CACHE;
    public final static Logger log = LoggerFactory.getLogger(JCSNodeCache.class.getName());

    public JCSNodeCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build();
        cacheManager.init();
        CACHE = cacheManager.createCache("geoloc", CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, JsonNode.class,
                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(500, EntryUnit.ENTRIES)));
        log.debug("Cache was initialized {}", CACHE);

    }

    @Override
    public JsonNode get(int key, Loader loader) throws IOException {
        try {
            String k = Integer.toString(key);
            JsonNode value = CACHE.get(k);
            if (value == null) {
                value = loader.load(key);
                CACHE.put(k, value);
            }
            return value;
        } catch (IOException e) {
            log.error("An issue occured while getting Json Node from cache for key " + key + " message: " + e.getMessage());
            throw e;
        }
    }

}
