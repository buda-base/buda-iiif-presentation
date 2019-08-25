package io.bdrc.iiif.presentation;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resservices.ConcurrentResourceService;
import io.bdrc.iiif.presentation.resservices.ServiceCache;

public class RSMock extends ConcurrentResourceService<String> {
    
    public static final RSMock Instance = new RSMock();

    RSMock() {
        super(ServiceCache.CACHE, "mock:");
    }
    
    @Override
    final public String getFromApi(final String resId) throws BDRCAPIException {
        System.out.println("starting getFromApi");
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.out.println("finishing getFromApi");
        return "success";
    }
    
}
