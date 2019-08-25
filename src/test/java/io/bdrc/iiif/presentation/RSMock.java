package io.bdrc.iiif.presentation;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resservices.ConcurrentResourceService;

public class RSMock extends ConcurrentResourceService<String> {

    public static String cachePrefix = "mock:";
    
    public static final RSMock Instance = new RSMock();
    
    RSMock() {
        super();
        super.cachePrefix = cachePrefix;
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
