package io.bdrc.iiif.presentation;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.maxmind.db.NodeCache;

public class JCSNodeCache implements NodeCache {

    static Cache<String, Object> cache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(Long.parseLong(SpringBootIIIFPres.getProperty("nodeCacheSize"))).build();

    @Override
    public JsonNode get(int key, Loader loader) throws IOException {
        String k = Integer.toString(key);
        JsonNode value = (JsonNode) cache.getIfPresent(k);
        if (value == null) {
            value = loader.load(key);
            cache.put(k, value);
        }
        return value;
    }

}
