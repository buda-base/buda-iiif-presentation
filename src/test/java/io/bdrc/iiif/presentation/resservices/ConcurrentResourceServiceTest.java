package io.bdrc.iiif.presentation.resservices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.bdrc.iiif.presentation.exceptions.BDRCAPIException;

public class ConcurrentResourceServiceTest {

    @BeforeAll
    public static void initializeCache() {
        ServiceCache.init();
    }

    @Test
    public void runtimeFailureDoesNotLeaveAnIncompleteFuture() throws Exception {
        final RuntimeException failure = new NullPointerException("malformed resource");
        final ConcurrentResourceService<String> service = new ConcurrentResourceService<String>("test:") {
            private int calls;

            @Override
            public String getFromApi(final String resId) throws BDRCAPIException {
                if (calls++ == 0) {
                    throw failure;
                }
                return "recovered";
            }
        };

        final ExecutionException exception = assertThrows(ExecutionException.class,
                () -> service.getAsync("resource").get());
        assertSame(failure, exception.getCause());

        assertFalse(service.futures.containsKey("resource"));
        assertEquals("recovered", service.getAsync("resource").get());
    }
}
