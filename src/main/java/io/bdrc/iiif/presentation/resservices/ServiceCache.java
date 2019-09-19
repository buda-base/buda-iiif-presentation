package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHENAME;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.commons.jcs.access.exception.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.metrics.CacheMetrics;

public class ServiceCache {

	public static CacheAccess<String, Object> CACHE;
	public final static Logger log = LoggerFactory.getLogger(ServiceCache.class.getName());

	public static void init() {
		CACHE = JCS.getInstance(CACHENAME);
	}

	public static void put(Object res, String key) {
		try {
			log.info("PUT IN CACHE ");
			CACHE.put(key, res);
			CacheMetrics.put();
			res = null;
		} catch (CacheException e) {
			log.error("Problem putting Results -->" + res + " in the iiifpres cache, for key -->" + key + " Exception:" + e.getMessage());
		}
	}

	public static Object getObjectFromCache(String key) {
		Object obj = CACHE.get(key);
		if (obj != null) {
			CacheMetrics.found();
		} else {
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
