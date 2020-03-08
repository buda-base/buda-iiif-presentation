package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_BVM;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.File;
import java.io.IOException;
import java.time.Instant;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.bdrc.iiif.presentation.AppConstants;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.libraries.GitHelpers;

public class BVMService extends ConcurrentResourceService<BVM> {

	private static final Logger logger = LoggerFactory.getLogger(BVMService.class);
	public static final BVMService Instance = new BVMService();
	public final static ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

    public static final int pullEveryS = 6000; // pull every x seconds
    public static Instant lastPull = null;
    public static synchronized void pullIfNecessary() throws BDRCAPIException {
        String repoBase = System.getProperty("user.dir") + "/gitData/buda-volume-manifests/";
        Repository repo = GitHelpers.ensureGitRepo(repoBase);
        // pull if not pull has been made for x s
        final Instant now = Instant.now();
        if (lastPull == null || lastPull.isBefore(now.minusSeconds(pullEveryS))) {
            try {
                GitHelpers.pull(repo);
            } catch (GitAPIException e) {
                throw new BDRCAPIException(500, AppConstants.GENERIC_APP_ERROR_CODE, e);
            }
        }
        lastPull = now;
    }
	
	BVMService() {
		super(CACHEPREFIX_BVM);
	}

	@Override
	public final BVM getFromApi(final String imageGroupLocalName) throws BDRCAPIException {
	    final String firstTwo = ImageInfoListService.getFirstMd5Nums(imageGroupLocalName);
        String filename = System.getProperty("user.dir") + "/gitData/buda-volume-manifests/" + firstTwo + "/"
                + imageGroupLocalName + ".json";
        pullIfNecessary();
        File f = new File(filename);
        if (!f.exists()) {
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "no BVM file for " + imageGroupLocalName);
        }
        logger.debug("Git filename is {}", filename);
        try {
            return om.readValue(f, BVM.class);
        } catch (IOException e) {
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "impossible to read BVM on git: " + imageGroupLocalName, e);
        }
	}
	
	public final void putInCache(final BVM bvm, final String imageGroupLocalName) {
	    ServiceCache.put(bvm, this.cachePrefix + imageGroupLocalName);
	}

}
