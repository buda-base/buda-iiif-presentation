package io.bdrc.iiif.presentation.resservices;

import static io.bdrc.iiif.presentation.AppConstants.CACHEPREFIX_BVM;
import static io.bdrc.iiif.presentation.AppConstants.GENERIC_APP_ERROR_CODE;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Optional;

import io.bdrc.auth.AuthProps;
import io.bdrc.iiif.presentation.AppConstants;
import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;
import io.bdrc.iiif.presentation.resmodels.BVM;
import io.bdrc.libraries.GitHelpers;

public class BVMService extends ConcurrentResourceService<BVM> {

	private static final Logger logger = LoggerFactory.getLogger(BVMService.class);
	public static final BVMService Instance = new BVMService();
	public final static ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());
	public final static DefaultPrettyPrinter.Indenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
	public final static DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
	
	static {
	    printer.indentObjectsWith(indenter);
	    printer.indentArraysWith(indenter);
	    om.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
	    om.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

    public static final int pushEveryS = 600; // push every 600 seconds
    public static boolean pushScheduled = false;
    public static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public static synchronized void pushWhenNecessary() {
        // pull if not pull has been made for 10mn
        if (pushScheduled) return;
        pushScheduled = true;
        final String repoBase = System.getProperty("user.dir") + "/gitData/buda-volume-manifests/";
        final Repository repo = GitHelpers.ensureGitRepo(repoBase);
        Runnable task = new Runnable() {
            public void run() {
                try {
                    GitHelpers.push(repo, AuthProps.getProperty("gitRemoteUrl"), AuthProps.getProperty("gitUser"), AuthProps.getProperty("gitPass"));
                    pushScheduled = false;
                } catch (GitAPIException e) {
                    logger.error("error pushing to BVM repo", e);
                }
            }
        };
        scheduler.schedule(task, pushEveryS, TimeUnit.SECONDS);
        scheduler.shutdown();
    }
	
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
		super(CACHEPREFIX_BVM, true);
	}

	@Override
	public final BVM getFromApi(final String imageGroupLocalName) throws BDRCAPIException {
	    final String firstTwo = ImageInfoListService.getFirstMd5Nums(imageGroupLocalName);
        String filename = System.getProperty("user.dir") + "/gitData/buda-volume-manifests/" + firstTwo + "/"
                + imageGroupLocalName + ".json";
        File f = new File(filename);
        if (!f.exists()) {
            logger.debug("bvm file doesn't exist: {}", filename);
            throw new BDRCAPIException(404, GENERIC_APP_ERROR_CODE, "no BVM file for " + imageGroupLocalName);
        }
        logger.debug("Git filename is {}", filename);
        try {
            return om.readValue(f, BVM.class);
        } catch (IOException e) {
            logger.error("Error reading bvm file {}", filename, e);
            throw new BDRCAPIException(500, GENERIC_APP_ERROR_CODE, "impossible to read BVM on git: " + imageGroupLocalName, e);
        }
	}
	
	public final void putInCache(final BVM bvm, final String imageGroupLocalName) {
	    //TODO: interrupt scheduled gets?
	    ServiceCache.put(Optional.of(bvm), this.cachePrefix + imageGroupLocalName);
	}

}
