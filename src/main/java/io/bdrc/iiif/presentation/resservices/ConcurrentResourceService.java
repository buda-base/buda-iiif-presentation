package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.BDR;
import static io.bdrc.iiif.presentation.AppConstants.BDR_len;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class ConcurrentResourceService<T> {

	static Logger logger = LoggerFactory.getLogger(ConcurrentResourceService.class);

	String cachePrefix = "";

	Map<String, CompletableFuture<T>> futures = new ConcurrentHashMap<>();

	public ConcurrentResourceService() {
	}

	public ConcurrentResourceService(String cachePrefix) {
		this.cachePrefix = cachePrefix;
	}

	@SuppressWarnings("unchecked")
	Optional<T> getFromCache(final String resId) {
	    logger.debug("getFromCache: {}{}", cachePrefix, resId);
		return (Optional<T>) ServiceCache.getObjectFromCache(cachePrefix + resId);
	}

	void putInCache(final String resId, final T res) {
		logger.debug("putInCache: {}{}", cachePrefix, resId);
		Optional<T> wrapper;
		if (res != null)
		    wrapper = Optional.of(res);
		else
		    wrapper = Optional.empty();
		ServiceCache.put(wrapper, cachePrefix + resId);
	}

	public T getFromApi(final String resId) throws BDRCAPIException {
		return null;
	}

	String normalizeId(final String resId) {
		if (resId.startsWith(BDR))
			return "bdr:" + resId.substring(BDR_len);
		return resId;
	}

	public T getSync(String resId) throws BDRCAPIException {
		resId = normalizeId(resId);
		Optional<T> resTFromCache = getFromCache(resId);
		if (!resTFromCache.isEmpty()) {
			logger.debug("found cache for {}", resId);
			return resTFromCache.get();
		}
		T resT = getFromApi(resId);
		putInCache(resId, resT);
		return resT;
	}

	public static final BDRCAPIException notFoundEx = new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "resource not found");
	
	/*
	 * Function returning a CompletableFuture and thus allowing many different calls
	 * to iiif-presentation to try to access a non-cached WorkInfo at the same time
	 * while doing only one request.
	 * 
	 * What it prevents is the following case. We assume that we're using a naive
	 * function, not this one. let's say an API call to lds-pdi takes 100ms. Let's
	 * say we ask for all the volumes of the Kangyur at the same time to
	 * iiif-presentation (something which actually happens). This means that the
	 * following happens: - first request to iiif-presentation: no workinfo cached,
	 * making a request to lds-pdi, takes 100ms - second request to
	 * iiif-presentation (2ms after the first one): the call to lds-pdi hasn't
	 * finished yet, so there is still no cache, so making another request to
	 * lds-pdi - etc.
	 * 
	 * which can lead to a large load of requests to lds-pdi, making all the
	 * requests much longer and stressing lds-pdi.
	 * 
	 * With this function the following happens (conceptually): - first request to
	 * iiif-presentation: no workinfo cached, making adding the request to lds-pdi
	 * in the map of requests - second request to iiif-presentation: no workinfo
	 * cached, but we see that the maps of requests contains one request for this
	 * resource, so we just wait for it to return - etc.
	 * 
	 * which means just one request is made instead of 100.
	 * 
	 */
	public CompletableFuture<T> getAsync(String resId) {
		resId = normalizeId(resId);
		Optional<T> resTFromCache = getFromCache(resId);
		if (resTFromCache != null) {
		    CompletableFuture<T> resCached = new CompletableFuture<>();
		    if (!resTFromCache.isEmpty()) {
    			logger.debug("found non-empty cache for {}", resId);
    			resCached.complete(resTFromCache.get());
    		} else {
    		    logger.debug("found empty cache for {}", resId);
    		    resCached.completeExceptionally(notFoundEx);
    		}
            return resCached;
		}
		// unintuitive way to perform the (necessary) atomic operation in the list
		CompletableFuture<T> res = new CompletableFuture<>();
		CompletableFuture<T> resFromList = futures.putIfAbsent(resId, res);
		if (resFromList != null) {
			// this is the case of all the threads trying to access the resource
			// except the first one
			return resFromList;
		}
		T resT;
		try {
			resT = getFromApi(resId);
		} catch (BDRCAPIException e) {
			res.completeExceptionally(e);
			// this means that we save each failed fetch using empty Optionals in the
			// cache, so that we don't have too many API calls
			putInCache(resId, null);
			futures.remove(resId);
			return res;
		}
		putInCache(resId, resT);
		res.complete(resT);
		futures.remove(resId);
		return res;
	}

}
